package com.careerai.builder.service;

import com.careerai.builder.ai.AiOrchestratorService;
import com.careerai.builder.domain.entity.CV;
import com.careerai.builder.domain.entity.CVSkill;
import com.careerai.builder.domain.entity.Job;
import com.careerai.builder.dto.GapAnalysisResult;
import com.careerai.builder.repository.CVSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Module 5 — Gap Skill Analyzer
 *
 * So sánh CV Skills vs JD Required Skills → tìm skills phù hợp, thiếu, và dư.
 * Chạy ON-DEMAND khi user click vào một Job cụ thể (không chạy lúc list).
 *
 * Module 6 — Recommendation Engine (AI Explanation)
 *
 * Kết hợp LLM để giải thích kết quả gap analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GapAnalysisService {

    private final CVSkillRepository cvSkillRepository;
    private final AiOrchestratorService aiOrchestratorService;

    // Skill lexicon phổ biến để extract từ JD text (rule-based)
    private static final List<String> KNOWN_SKILLS = List.of(
            "Java", "Spring Boot", "Spring Security", "Spring Data JPA", "Hibernate",
            "Python", "Django", "FastAPI", "Flask",
            "JavaScript", "TypeScript", "React", "Next.js", "Vue.js", "Angular", "Node.js",
            "Express.js", "NestJS",
            "HTML", "CSS", "Tailwind CSS", "SASS",
            "SQL", "PostgreSQL", "MySQL", "MongoDB", "Redis",
            "Docker", "Kubernetes", "Terraform", "Ansible",
            "AWS", "GCP", "Azure", "Vercel",
            "Git", "GitHub Actions", "Jenkins", "CI/CD",
            "Kafka", "RabbitMQ",
            "GraphQL", "REST API", "gRPC",
            "Linux", "Bash",
            "Swift", "SwiftUI", "Kotlin", "React Native", "Flutter",
            "PyTorch", "TensorFlow", "Machine Learning", "Deep Learning",
            "Spark", "Airflow", "dbt", "Snowflake", "BigQuery",
            "Prometheus", "Grafana", "ELK",
            "Selenium", "Playwright", "Cypress", "Jest",
            "Figma", "Storybook",
            "System Design", "Microservices", "Event Sourcing",
            "Agile", "Scrum"
    );

    /**
     * Rule-based gap analysis (nhanh, không tốn API).
     */
    @Transactional(readOnly = true)
    public GapAnalysisResult analyzeGap(CV cv, Job job) {
        log.info("📊 Gap Analysis — CV: {} vs Job: '{}'", cv.getId(), job.getTitle());

        // 1. Lấy skills từ CV (đã được extract trước đó bởi CVService)
        List<CVSkill> cvSkills = cvSkillRepository.findByCv(cv);
        Set<String> cvSkillNames = cvSkills.stream()
                .map(s -> s.getSkill().getName().toLowerCase().trim())
                .collect(Collectors.toSet());

        // 2. Extract required skills từ JD text (rule-based)
        String jdText = job.getRawDescription() != null ? job.getRawDescription()
                : (job.getDescriptionHtml() != null ? job.getDescriptionHtml().replaceAll("<[^>]*>", " ") : "");

        Set<String> jdSkills = extractSkillsFromText(jdText);

        // 3. Phân loại: matching / missing / extra
        List<String> matchingSkills = jdSkills.stream()
                .filter(jdSkill -> cvSkillNames.contains(jdSkill.toLowerCase()))
                .sorted()
                .toList();

        List<String> missingSkills = jdSkills.stream()
                .filter(jdSkill -> !cvSkillNames.contains(jdSkill.toLowerCase()))
                .sorted()
                .toList();

        List<String> extraSkills = cvSkillNames.stream()
                .filter(cvSkill -> jdSkills.stream().noneMatch(jd -> jd.equalsIgnoreCase(cvSkill)))
                .map(this::capitalizeFirst)
                .sorted()
                .limit(10) // Giới hạn 10 extra skills để không quá dài
                .toList();

        // 4. Tính match score
        double matchScore = jdSkills.isEmpty() ? 0
                : (double) matchingSkills.size() / jdSkills.size() * 100;

        log.info("✅ Gap Analysis hoàn tất — Match: {}/{} skills ({}%)",
                matchingSkills.size(), jdSkills.size(), String.format("%.0f", matchScore));

        return GapAnalysisResult.builder()
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .company(job.getCompany())
                .matchScore(Math.round(matchScore * 100.0) / 100.0)
                .matchingSkills(matchingSkills)
                .missingSkills(missingSkills)
                .extraSkills(extraSkills)
                .build();
    }

    /**
     * Gap analysis + AI explanation (Module 5 + Module 6 kết hợp).
     * Gọi LLM để giải thích kết quả và gợi ý cải thiện.
     */
    @Transactional(readOnly = true)
    public GapAnalysisResult analyzeGapWithExplanation(CV cv, Job job) {
        GapAnalysisResult result = analyzeGap(cv, job);

        // Gọi AI để giải thích (nếu AI enabled)
        try {
            String cvSummary = cv.getSummary() != null ? cv.getSummary() : "No summary available";
            String jdText = job.getRawDescription() != null ? job.getRawDescription() : job.getTitle();

            Optional<String> explanation = aiOrchestratorService.explainJobMatch(
                    cvSummary, jdText, result.getMatchingSkills(), result.getMissingSkills());

            explanation.ifPresent(result::setAiExplanation);
        } catch (Exception e) {
            log.warn("⚠️ AI explanation failed, returning result without explanation: {}", e.getMessage());
            result.setAiExplanation(buildFallbackExplanation(result));
        }

        // Nếu AI không trả về gì, dùng fallback
        if (result.getAiExplanation() == null || result.getAiExplanation().isBlank()) {
            result.setAiExplanation(buildFallbackExplanation(result));
        }

        return result;
    }

    /**
     * Extract skills từ text bằng keyword matching (rule-based).
     * Nhanh và chính xác cho các skill phổ biến.
     */
    private Set<String> extractSkillsFromText(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        String normalizedText = text.toLowerCase();
        Set<String> found = new LinkedHashSet<>();

        for (String skill : KNOWN_SKILLS) {
            // Tìm exact match (case-insensitive) để tránh false positive
            // VD: "Java" không match "JavaScript"
            String skillLower = skill.toLowerCase();
            int idx = normalizedText.indexOf(skillLower);

            while (idx >= 0) {
                // Kiểm tra boundary: ký tự trước và sau phải không phải letter
                boolean validStart = idx == 0 || !Character.isLetterOrDigit(normalizedText.charAt(idx - 1));
                boolean validEnd = (idx + skillLower.length() >= normalizedText.length())
                        || !Character.isLetterOrDigit(normalizedText.charAt(idx + skillLower.length()));

                if (validStart && validEnd) {
                    found.add(skill);
                    break;
                }

                idx = normalizedText.indexOf(skillLower, idx + 1);
            }
        }

        return found;
    }

    /**
     * Fallback explanation khi AI không available.
     */
    private String buildFallbackExplanation(GapAnalysisResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("## Career Fit Analysis\n\n");

        if (result.getMatchScore() >= 70) {
            sb.append("**Strong Match** — Your profile aligns well with this position. ");
            sb.append("You already possess ").append(result.getMatchingSkills().size())
                    .append(" of the required skills.\n\n");
        } else if (result.getMatchScore() >= 40) {
            sb.append("**Moderate Match** — You have a solid foundation, but there are some skill gaps to address. ");
            sb.append("You match ").append(result.getMatchingSkills().size())
                    .append(" skills and need to develop ").append(result.getMissingSkills().size())
                    .append(" more.\n\n");
        } else {
            sb.append("**Growth Opportunity** — This role requires significant skill development. ");
            sb.append("Consider this as a target position and build a roadmap to close the gaps.\n\n");
        }

        if (!result.getMatchingSkills().isEmpty()) {
            sb.append("### ✅ Your Strengths\n");
            result.getMatchingSkills().forEach(s -> sb.append("- ").append(s).append("\n"));
            sb.append("\n");
        }

        if (!result.getMissingSkills().isEmpty()) {
            sb.append("### ❌ Skills to Develop\n");
            result.getMissingSkills().forEach(s -> sb.append("- ").append(s).append("\n"));
            sb.append("\n");
            sb.append("💡 *Consider generating a learning roadmap to close these gaps.*\n");
        }

        return sb.toString().trim();
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
