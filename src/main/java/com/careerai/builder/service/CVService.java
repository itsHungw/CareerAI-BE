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

    private final AiOrchestratorService aiOrchestratorService;
    private final CVRepository cvRepository;
    private final CVSkillRepository cvSkillRepository;
    private final SkillService skillService;
    private final PdfParsingService pdfParsingService;
    private final InferenceService inferenceService;

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

            String extractedText = pdfParsingService.extractSearchableText(file, originalFileName);

            CV cv = CV.builder()
                    .user(user)
                    .fileName(uniqueFileName)
                    .fileUrl("/api/cv/download/" + uniqueFileName)
                    .rawText(extractedText)
                    .fileHash(fileHash)
                    .fileSize(file.getSize())
                    .lastAnalyzedAt(LocalDateTime.now())
                    .build();

            CV savedCv = cvRepository.save(cv);
            CvAnalysisResult analysis = analyzeCvWithFallback(originalFileName, extractedText);

            CV enrichedCv = saveExtractedIntelligence(
                    savedCv.getId(),
                    analysis.getReview(),
                    analysis.getSummary(),
                    toInclusionResults(analysis.getSkills()));

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
    public CV saveExtractedIntelligence(UUID cvId, String review, String summary, List<InferenceService.ExtractionResult> extractedSkills) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new IllegalArgumentException("CV not found with ID: " + cvId));

        cv.setReview(review);
        cv.setSummary(summary);
        cvSkillRepository.deleteByCv(cv);

        for (InferenceService.ExtractionResult extract : extractedSkills) {
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
    
    private CvAnalysisResult analyzeCvWithFallback(String originalFileName, String extractedText) {
        return aiOrchestratorService.analyzeCv(CvAnalysisRequest.builder()
                        .fileName(originalFileName)
                        .rawText(extractedText)
                        .build())
                .filter(this::hasUsableAnalysis)
                .orElseGet(() -> {
                    log.warn("⚠️ AI CV analysis returned unusable result or failed. Falling back to keyword-based analysis.");
                    List<InferenceService.ExtractionResult> fallbackSkills = inferenceService.inferSkills(extractedText);
                    log.info("✓ Fallback CV analysis generated {} skills", fallbackSkills.size());
                    return CvAnalysisResult.builder()
                            .summary(inferenceService.buildHeuristicSummary(fallbackSkills))
                            .review(inferenceService.buildHeuristicReview(fallbackSkills))
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

    private List<InferenceService.ExtractionResult> toInclusionResults(List<CvSkillSignal> skills) {
        List<InferenceService.ExtractionResult> results = new ArrayList<>();
        for (CvSkillSignal skill : skills) {
            results.add(new InferenceService.ExtractionResult(
                    skill.getSkillName(),
                    skill.getCategory() == null || skill.getCategory().isBlank() ? "General" : skill.getCategory(),
                    skill.getConfidenceScore() == null ? 0.5d : skill.getConfidenceScore(),
                    skill.getYearsOfExperience() == null ? 1 : skill.getYearsOfExperience()));
        }
        return results;
    }

    private List<CvSkillSignal> toSkillSignals(List<InferenceService.ExtractionResult> extractedSkills) {
        List<CvSkillSignal> signals = new ArrayList<>();
        for (InferenceService.ExtractionResult extract : extractedSkills) {
            signals.add(CvSkillSignal.builder()
                    .skillName(extract.getSkillName())
                    .category(extract.getCategory())
                    .confidenceScore(extract.getConfidenceScore())
                    .yearsOfExperience(extract.getYearsOfExperience())
                    .build());
        }
        return signals;
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
