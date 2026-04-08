package com.careerai.builder.service;

import com.careerai.builder.ai.AiOrchestratorService;
import com.careerai.builder.ai.model.CvAnalysisRequest;
import com.careerai.builder.ai.model.CvAnalysisResult;
import com.careerai.builder.ai.model.CvSkillSignal;
import com.careerai.builder.domain.entity.CV;
import com.careerai.builder.domain.entity.CVSkill;
import com.careerai.builder.domain.entity.Skill;
import com.careerai.builder.domain.entity.User;
import com.careerai.builder.dto.CVResponse;
import com.careerai.builder.repository.CVRepository;
import com.careerai.builder.repository.CVSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CVService {

    private static final Map<String, SkillTemplate> SKILL_LEXICON = buildSkillLexicon();

    private final AiOrchestratorService aiOrchestratorService;
    private final CVRepository cvRepository;
    private final CVSkillRepository cvSkillRepository;
    private final SkillService skillService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @CacheEvict(value = {"userLatestCv", "cvSkills", "userRoadmaps"}, key = "#user.id")
    public CVResponse uploadAndParseCV(MultipartFile file, User user) {
        try {
            Path root = Paths.get(uploadDir);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }

            // Calculate file hash for duplicate detection
            String fileHash = calculateFileHash(file.getBytes());

            // Check if same file was already analyzed
            Optional<CV> existingCv = cvRepository.findByUserOrderByCreatedAtDesc(user)
                    .stream()
                    .filter(cv -> fileHash.equals(cv.getFileHash()))
                    .findFirst();

            if (existingCv.isPresent()) {
                log.info("📁 Same file detected (hash={}). Returning cached review without AI re-analysis", fileHash.substring(0, 8));
                CV cached = existingCv.get();
                return CVResponse.builder()
                        .id(cached.getId())
                        .fileName(cached.getFileName())
                        .fileUrl(cached.getFileUrl())
                        .review(cached.getReview())
                        .build();
            }

            String originalFileName = file.getOriginalFilename() == null ? "resume.pdf" : file.getOriginalFilename();
            String uniqueFileName = UUID.randomUUID() + "_" + originalFileName;
            Path filePath = root.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), filePath);

            String extractedText = extractSearchableText(file, originalFileName);

            CV cv = CV.builder()
                    .user(user)
                    .fileName(uniqueFileName)
                    .fileUrl("/api/cv/download/" + uniqueFileName)
                    .rawText(extractedText)
                    .fileHash(fileHash)          // Store hash
                    .fileSize(file.getSize())    // Store size
                    .lastAnalyzedAt(LocalDateTime.now())
                    .build();

            CV savedCv = cvRepository.save(cv);
            CvAnalysisResult analysis = analyzeCvWithFallback(originalFileName, extractedText);

            CV enrichedCv = saveExtractedIntelligence(
                    savedCv.getId(),
                    analysis.getReview(),
                    analysis.getSummary(),
                    toExtractionResults(analysis.getSkills()));

            return CVResponse.builder()
                    .id(enrichedCv.getId())
                    .fileName(enrichedCv.getFileName())
                    .fileUrl(enrichedCv.getFileUrl())
                    .review(enrichedCv.getReview())
                    .build();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    public Resource loadCvFile(String filename) {
        try {
            Path file = Paths.get(uploadDir).resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("Could not read the file!");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @Transactional
    public CV saveRawCV(User user, String fileName, String fileUrl, String rawText) {
        CV cv = CV.builder()
                .user(user)
                .fileName(fileName)
                .fileUrl(fileUrl)
                .rawText(rawText)
                .build();
        return cvRepository.save(cv);
    }

    @Transactional
    public CV saveExtractedIntelligence(UUID cvId, String review, String summary, List<ExtractionResult> extractedSkills) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new IllegalArgumentException("CV not found with ID: " + cvId));

        cv.setReview(review);
        cv.setSummary(summary);
        cvSkillRepository.deleteByCv(cv);

        for (ExtractionResult extract : extractedSkills) {
            Skill normalizedSkill = skillService.normalizeAndHandleSkill(extract.getSkillName(), extract.getCategory());

            CVSkill cvSkill = CVSkill.builder()
                    .cv(cv)
                    .skill(normalizedSkill)
                    .confidenceScore(extract.getConfidenceScore())
                    .yearsOfExperience(extract.getYearsOfExperience())
                    .build();

            cvSkillRepository.save(cvSkill);
        }

        return cvRepository.save(cv);
    }

    private String extractSearchableText(MultipartFile file, String originalFileName) throws IOException {
        String extractedPdfText = extractPdfText(file);
        if (!extractedPdfText.isBlank()) {
            String normalizedText = normalizeExtractedText(extractedPdfText);
            return String.join(" ",
                    "file-name:", originalFileName,
                    "pdf-text:", normalizedText);
        }

        String bytePreview = new String(file.getBytes(), StandardCharsets.ISO_8859_1);
        String normalizedPreview = normalizeExtractedText(bytePreview);
        return String.join(" ",
                "file-name:", originalFileName,
                "heuristic-preview:", normalizedPreview);
    }

    private String extractPdfText(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper textStripper = new PDFTextStripper();
            return textStripper.getText(document);
        } catch (IOException ex) {
            log.warn("Failed to parse PDF content for '{}': {}", file.getOriginalFilename(), ex.getMessage());
            return "";
        }
    }

    private String normalizeExtractedText(String text) {
        String normalized = text.replaceAll("[^\\p{L}\\p{N}\\.\\+#\\-/ ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.length() > 12000) {
            return normalized.substring(0, 12000);
        }
        return normalized;
    }

    private CvAnalysisResult analyzeCvWithFallback(String originalFileName, String extractedText) {
        return aiOrchestratorService.analyzeCv(CvAnalysisRequest.builder()
                        .fileName(originalFileName)
                        .rawText(extractedText)
                        .build())
                .filter(this::hasUsableAnalysis)
                .orElseGet(() -> {
                    log.warn("⚠️ AI CV analysis returned unusable result or failed. Falling back to keyword-based analysis.");
                    List<ExtractionResult> fallbackSkills = inferSkills(extractedText);
                    log.info("✓ Fallback CV analysis generated {} skills", fallbackSkills.size());
                    return CvAnalysisResult.builder()
                            .summary(buildSummary(fallbackSkills))
                            .review(buildReview(fallbackSkills))
                            .skills(toSkillSignals(fallbackSkills))
                            .build();
                });
    }

    private boolean hasUsableAnalysis(CvAnalysisResult result) {
        return result != null
                && result.getSummary() != null
                && !result.getSummary().isBlank()
                && result.getReview() != null
                && !result.getReview().isBlank()
                && result.getSkills() != null
                && !result.getSkills().isEmpty();
    }

    private List<ExtractionResult> inferSkills(String extractedText) {
        String normalizedText = extractedText.toLowerCase();
        List<ExtractionResult> results = new ArrayList<>();

        for (Map.Entry<String, SkillTemplate> entry : SKILL_LEXICON.entrySet()) {
            if (normalizedText.contains(entry.getKey())) {
                SkillTemplate template = entry.getValue();
                results.add(new ExtractionResult(template.skillName(), template.category(), 0.72d, template.defaultYears()));
            }
        }

        if (results.isEmpty()) {
            results.add(new ExtractionResult("Communication", "Soft Skill", 0.45d, 1));
            results.add(new ExtractionResult("Problem Solving", "Soft Skill", 0.45d, 1));
            results.add(new ExtractionResult("Git", "Tooling", 0.40d, 1));
        }

        return results;
    }

    private List<ExtractionResult> toExtractionResults(List<CvSkillSignal> skills) {
        List<ExtractionResult> results = new ArrayList<>();
        for (CvSkillSignal skill : skills) {
            results.add(new ExtractionResult(
                    skill.getSkillName(),
                    skill.getCategory() == null || skill.getCategory().isBlank() ? "General" : skill.getCategory(),
                    skill.getConfidenceScore() == null ? 0.5d : skill.getConfidenceScore(),
                    skill.getYearsOfExperience() == null ? 1 : skill.getYearsOfExperience()));
        }
        return results;
    }

    private List<CvSkillSignal> toSkillSignals(List<ExtractionResult> extractedSkills) {
        List<CvSkillSignal> signals = new ArrayList<>();
        for (ExtractionResult extract : extractedSkills) {
            signals.add(CvSkillSignal.builder()
                    .skillName(extract.getSkillName())
                    .category(extract.getCategory())
                    .confidenceScore(extract.getConfidenceScore())
                    .yearsOfExperience(extract.getYearsOfExperience())
                    .build());
        }
        return signals;
    }

    private String buildReview(List<ExtractionResult> inferredSkills) {
        if (inferredSkills.isEmpty()) {
            return "## 💪 Strengths\n\nNo specific technical skills detected. Consider updating your CV with concrete technical expertise.\n\n" +
                    "## 🎯 Improvement Areas\n\n1. Add specific technologies and tools you've used\n" +
                    "2. Include quantifiable results and achievements\n" +
                    "3. Highlight relevant projects and certifications\n\n" +
                    "## 💡 Quick Tips\n\n- Be specific with skill names (e.g., 'Java 21' instead of 'programming')\n" +
                    "- Mention years of experience for each skill\n" +
                    "- Include both hard and soft skills for a complete profile\n\n" +
                    "*Note: This is a heuristic analysis. For detailed AI-powered insights, ensure your system is properly configured.*";
        }

        StringBuilder builder = new StringBuilder();

        // Group skills by category
        Map<String, List<ExtractionResult>> skillsByCategory = inferredSkills.stream()
                .collect(java.util.stream.Collectors.groupingBy(ExtractionResult::getCategory));

        // Strengths section
        builder.append("## 💪 Strengths\n\n");
        builder.append("Your CV highlights a diverse skill set across multiple areas:\n\n");
        skillsByCategory.forEach((category, skills) -> {
            builder.append("**").append(category).append(":**\n");
            skills.forEach(skill -> {
                builder.append("- ").append(skill.getSkillName());
                if (skill.getYearsOfExperience() != null && skill.getYearsOfExperience() > 0) {
                    builder.append(" (~").append(skill.getYearsOfExperience()).append(" years)");
                }
                builder.append("\n");
            });
            builder.append("\n");
        });

        // Improvement Areas section
        builder.append("## 🎯 Improvement Areas\n\n");
        builder.append("To strengthen your profile:\n\n");

        boolean hasBackend = skillsByCategory.containsKey("Backend");
        boolean hasFrontend = skillsByCategory.containsKey("Frontend");
        boolean hasDevOps = skillsByCategory.containsKey("DevOps/Infrastructure");
        boolean hasData = skillsByCategory.containsKey("Data");

        if (!hasBackend && !hasFrontend) {
            builder.append("1. **Tech Stack Definition:** Specify backend and/or frontend technologies\n");
        }
        if (!hasDevOps) {
            builder.append("2. **Platform & Deployment Skills:** Add AWS, Docker, Kubernetes, or similar\n");
        }
        if (!hasData) {
            builder.append("3. **Data & Analytics:** Consider including SQL, databases, or analytics tools\n");
        }
        builder.append("4. **Project Outcomes:** Quantify your contributions (e.g., 'Improved performance by 30%')\n");
        builder.append("5. **Soft Skills:** Professional development, leadership, or communication skills\n\n");

        // Quick Tips section
        builder.append("## 💡 Quick Tips\n\n");
        builder.append("- **Be Specific:** Use exact tool/library names (not 'databases' → 'PostgreSQL')\n");
        builder.append("- **Show Impact:** Numbers matter: years, scale, or outcomes\n");
        builder.append("- **Organized Layout:** Group similar skills together for readability\n");
        builder.append("- **Tailor to Role:** Highlight skills most relevant to your target job\n\n");

        builder.append("*Tip: This analysis is generated from keyword extraction. Upload a fresh CV or configure AI provider for more comprehensive insights.*");

        return builder.toString().trim();
    }

    private String buildSummary(List<ExtractionResult> inferredSkills) {
        String joinedSkills = inferredSkills.stream()
                .limit(4)
                .map(ExtractionResult::getSkillName)
                .reduce((left, right) -> left + ", " + right)
                .orElse("foundational professional skills");

        return "Current CV profile suggests experience around " + joinedSkills
                + ". This summary is generated from heuristic extraction and is sufficient to bootstrap roadmap generation, but it should later be replaced by full PDF parsing plus AI enrichment.";
    }

    private static Map<String, SkillTemplate> buildSkillLexicon() {
        Map<String, SkillTemplate> lexicon = new LinkedHashMap<>();
        lexicon.put("spring boot", new SkillTemplate("Spring Boot", "Backend", 1));
        lexicon.put("java", new SkillTemplate("Java", "Backend", 1));
        lexicon.put("react", new SkillTemplate("React", "Frontend", 1));
        lexicon.put("next.js", new SkillTemplate("Next.js", "Frontend", 1));
        lexicon.put("nextjs", new SkillTemplate("Next.js", "Frontend", 1));
        lexicon.put("typescript", new SkillTemplate("TypeScript", "Frontend", 1));
        lexicon.put("javascript", new SkillTemplate("JavaScript", "Frontend", 1));
        lexicon.put("node.js", new SkillTemplate("Node.js", "Backend", 1));
        lexicon.put("nodejs", new SkillTemplate("Node.js", "Backend", 1));
        lexicon.put("postgresql", new SkillTemplate("PostgreSQL", "Database", 1));
        lexicon.put("sql", new SkillTemplate("SQL", "Database", 1));
        lexicon.put("docker", new SkillTemplate("Docker", "DevOps", 1));
        lexicon.put("aws", new SkillTemplate("AWS", "Cloud", 1));
        lexicon.put("html", new SkillTemplate("HTML", "Frontend", 1));
        lexicon.put("css", new SkillTemplate("CSS", "Frontend", 1));
        lexicon.put("python", new SkillTemplate("Python", "Backend", 1));
        lexicon.put("git", new SkillTemplate("Git", "Tooling", 1));
        return lexicon;
    }

    private record SkillTemplate(String skillName, String category, Integer defaultYears) {
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ExtractionResult {
        private String skillName;
        private String category;
        private Double confidenceScore;
        private Integer yearsOfExperience;
    }

    /**
     * Cacheable method to get user's latest CV
     * Cached for 5 minutes, invalidated when user uploads new CV
     */
    @Cacheable(value = "userLatestCv", key = "#user.id")
    public CV getLatestCv(User user) {
        return cvRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * Cacheable method to get CV skills with details
     * Cached for 15 minutes, used for roadmap generation
     */
    @Cacheable(value = "cvSkills", key = "#cv.id")
    public List<CVSkill> getCvSkillsDetailed(CV cv) {
        return cvSkillRepository.findByCv(cv);
    }

    /**
     * Calculate SHA-256 hash of file bytes for duplicate detection
     */
    private String calculateFileHash(byte[] fileBytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(fileBytes);
        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte hashByte : hashBytes) {
            String hex = Integer.toHexString(0xff & hashByte);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
