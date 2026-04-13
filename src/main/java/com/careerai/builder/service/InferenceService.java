package com.careerai.builder.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service xử lý các logic Heuristic/Inference (suy luận) dựa trên văn bản.
 * Chứa bộ từ điển kỹ năng (Skill Lexicon) và logic sinh review mặc định.
 */
@Service
@Slf4j
public class InferenceService {

    private static final Map<String, SkillTemplate> SKILL_LEXICON = buildSkillLexicon();

    /**
     * Suy luận kỹ năng dựa trên từ khóa (Fallback khi AI không khả dụng).
     */
    public List<ExtractionResult> inferSkills(String extractedText) {
        String normalizedText = extractedText.toLowerCase();
        List<ExtractionResult> results = new ArrayList<>();

        for (Map.Entry<String, SkillTemplate> entry : SKILL_LEXICON.entrySet()) {
            if (normalizedText.contains(entry.getKey())) {
                SkillTemplate template = entry.getValue();
                results.add(new ExtractionResult(template.skillName(), template.category(), 0.72d, template.defaultYears()));
            }
        }

        // Default soft skills if nothing else found
        if (results.isEmpty()) {
            results.add(new ExtractionResult("Communication", "Soft Skill", 0.45d, 1));
            results.add(new ExtractionResult("Problem Solving", "Soft Skill", 0.45d, 1));
            results.add(new ExtractionResult("Git", "Tooling", 0.40d, 1));
        }

        return results;
    }

    /**
     * Tự động xây dựng lời nhận xét dựa trên bộ kỹ năng tìm thấy.
     */
    public String buildHeuristicReview(List<ExtractionResult> inferredSkills) {
        if (inferredSkills.isEmpty()) {
            return "## 💪 Strengths\n\nNo specific technical skills detected.\n\n" +
                    "## 🎯 Improvement Areas\n\n1. Add specific technologies\n\n*Heuristic Analysis*";
        }

        StringBuilder builder = new StringBuilder();
        Map<String, List<ExtractionResult>> skillsByCategory = inferredSkills.stream()
                .collect(java.util.stream.Collectors.groupingBy(ExtractionResult::getCategory));

        builder.append("## 💪 Strengths\n\n");
        skillsByCategory.forEach((category, skills) -> {
            builder.append("**").append(category).append(":** ");
            builder.append(String.join(", ", skills.stream().map(ExtractionResult::getSkillName).toList()));
            builder.append("\n\n");
        });

        builder.append("## 🎯 Improvement Areas\n\n");
        builder.append("- Consider quantifying your impact.\n- Add missing cloud or devops technologies.\n\n");
        builder.append("*Note: This is a keyword-based analysis. Use AI for deeper insights.*");

        return builder.toString();
    }

    public String buildHeuristicSummary(List<ExtractionResult> inferredSkills) {
        String joinedSkills = inferredSkills.stream()
                .limit(4)
                .map(ExtractionResult::getSkillName)
                .reduce((left, right) -> left + ", " + right)
                .orElse("general professional skills");

        return "Profile focuses on " + joinedSkills + ". (Generated via keyword inference)";
    }

    private static Map<String, SkillTemplate> buildSkillLexicon() {
        Map<String, SkillTemplate> lexicon = new LinkedHashMap<>();
        lexicon.put("spring boot", new SkillTemplate("Spring Boot", "Backend", 1));
        lexicon.put("java", new SkillTemplate("Java", "Backend", 1));
        lexicon.put("react", new SkillTemplate("React", "Frontend", 1));
        lexicon.put("next.js", new SkillTemplate("Next.js", "Frontend", 1));
        lexicon.put("typescript", new SkillTemplate("TypeScript", "Frontend", 1));
        lexicon.put("javascript", new SkillTemplate("JavaScript", "Frontend", 1));
        lexicon.put("node.js", new SkillTemplate("Node.js", "Backend", 1));
        lexicon.put("postgresql", new SkillTemplate("PostgreSQL", "Database", 1));
        lexicon.put("sql", new SkillTemplate("SQL", "Database", 1));
        lexicon.put("docker", new SkillTemplate("Docker", "DevOps", 1));
        lexicon.put("aws", new SkillTemplate("AWS", "Cloud", 1));
        lexicon.put("git", new SkillTemplate("Git", "Tooling", 1));
        return lexicon;
    }

    private record SkillTemplate(String skillName, String category, Integer defaultYears) {}

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ExtractionResult {
        private String skillName;
        private String category;
        private Double confidenceScore;
        private Integer yearsOfExperience;
    }
}
