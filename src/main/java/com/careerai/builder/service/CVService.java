package com.careerai.builder.service;

import com.careerai.builder.domain.entity.CV;
import com.careerai.builder.domain.entity.CVSkill;
import com.careerai.builder.domain.entity.Skill;
import com.careerai.builder.domain.entity.User;
import com.careerai.builder.dto.CVResponse;
import com.careerai.builder.repository.CVRepository;
import com.careerai.builder.repository.CVSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CVService {

    private static final Map<String, SkillTemplate> SKILL_LEXICON = buildSkillLexicon();

    private final CVRepository cvRepository;
    private final CVSkillRepository cvSkillRepository;
    private final SkillService skillService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public CVResponse uploadAndParseCV(MultipartFile file, User user) {
        try {
            Path root = Paths.get(uploadDir);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
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
                    .build();

            CV savedCv = cvRepository.save(cv);
            List<ExtractionResult> inferredSkills = inferSkills(extractedText);
            String parsedContent = buildParsedContent(inferredSkills);
            String summary = buildSummary(inferredSkills);

            CV enrichedCv = saveExtractedIntelligence(savedCv.getId(), parsedContent, summary, inferredSkills);

            return CVResponse.builder()
                    .id(enrichedCv.getId())
                    .fileName(enrichedCv.getFileName())
                    .fileUrl(enrichedCv.getFileUrl())
                    .parsedContent(enrichedCv.getParsedContent())
                    .build();
        } catch (IOException e) {
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
    public CV saveExtractedIntelligence(UUID cvId, String parsedContent, String summary, List<ExtractionResult> extractedSkills) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new IllegalArgumentException("CV not found with ID: " + cvId));

        cv.setParsedContent(parsedContent);
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
        String bytePreview = new String(file.getBytes(), StandardCharsets.ISO_8859_1);
        String normalizedPreview = bytePreview.replaceAll("[^\\p{L}\\p{N}\\.\\+#\\-/ ]", " ");
        if (normalizedPreview.length() > 4000) {
            normalizedPreview = normalizedPreview.substring(0, 4000);
        }

        return String.join(" ",
                "file-name:", originalFileName,
                "heuristic-preview:", normalizedPreview);
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

    private String buildParsedContent(List<ExtractionResult> inferredSkills) {
        StringBuilder builder = new StringBuilder("Detected strengths:\n");
        for (ExtractionResult skill : inferredSkills) {
            builder.append("- ")
                    .append(skill.getSkillName())
                    .append(" (")
                    .append(skill.getCategory())
                    .append(")\n");
        }
        builder.append("\nParsing mode: heuristic fallback based on searchable text patterns. Replace with full PDF/AI parsing in the next iteration.");
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
}
