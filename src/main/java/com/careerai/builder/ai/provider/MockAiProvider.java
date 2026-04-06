package com.careerai.builder.ai.provider;

import com.careerai.builder.ai.model.CvAnalysisRequest;
import com.careerai.builder.ai.model.CvAnalysisResult;
import com.careerai.builder.ai.model.CvSkillSignal;
import com.careerai.builder.ai.model.RoadmapGenerationRequest;
import com.careerai.builder.ai.model.RoadmapGenerationResult;
import com.careerai.builder.ai.model.RoadmapStepSuggestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class MockAiProvider implements AiProvider {

    private static final Map<String, CvSkillSignal> SKILL_LEXICON = buildSkillLexicon();

    @Override
    public String getName() {
        return "mock";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public Optional<CvAnalysisResult> analyzeCv(CvAnalysisRequest request) {
        String normalizedText = request.getRawText().toLowerCase();
        List<CvSkillSignal> signals = new ArrayList<>();

        for (Map.Entry<String, CvSkillSignal> entry : SKILL_LEXICON.entrySet()) {
            if (normalizedText.contains(entry.getKey())) {
                signals.add(entry.getValue());
            }
        }

        if (signals.isEmpty()) {
            signals.add(CvSkillSignal.builder().skillName("Communication").category("Soft Skill").confidenceScore(0.45d).yearsOfExperience(1).build());
            signals.add(CvSkillSignal.builder().skillName("Problem Solving").category("Soft Skill").confidenceScore(0.45d).yearsOfExperience(1).build());
        }
        log.info("Mock AI: Analyzing CV for '{}'", request.getFileName());

        return Optional.of(CvAnalysisResult.builder()
                .summary("Mock CV summary for " + request.getFileName() + ". AI identified several foundational professional skills.")
                .review("### Mock Review & Critique\n\nBased on a simulated analysis of your CV, here is some feedback:\n\n**Strengths:**\n- Good mix of technical and soft skills.\n- Clear document naming convention.\n\n**Areas for Improvement:**\n- Consider a more technical deep-dive in your project descriptions.\n- Ensure each skill has a corresponding impact metric.\n\n**Action Items:**\n1. Add specific numbers to your achievements.\n2. Verify the consistency of your formatting across sections.")
                .skills(signals)
                .build());
    }

    @Override
    public Optional<RoadmapGenerationResult> generateRoadmap(RoadmapGenerationRequest request) {
        List<RoadmapStepSuggestion> steps = new ArrayList<>();
        steps.add(RoadmapStepSuggestion.builder()
                .title("Clarify the target role")
                .description("Review the desired role, compare it against current evidence in the CV, and define a focused execution goal for the next learning cycle.")
                .durationDays(3)
                .resources(List.of("Role scorecard", "Target company list"))
                .build());

        for (String missingSkill : request.getMissingSkills().stream().limit(3).toList()) {
            steps.add(RoadmapStepSuggestion.builder()
                    .title("Build depth in " + missingSkill)
                    .description("Create a dedicated sprint for " + missingSkill + " and tie it to a small proof-of-work artifact.")
                    .durationDays(10)
                    .resources(List.of("Official docs for " + missingSkill, "One guided project"))
                    .build());
        }

        steps.add(RoadmapStepSuggestion.builder()
                .title("Ship a portfolio proof")
                .description("Convert current skills plus the new gap-closure work into a project artifact that is relevant to the target role.")
                .durationDays(14)
                .resources(List.of("Project README", "Demo video", "Impact notes"))
                .build());

        return Optional.of(RoadmapGenerationResult.builder()
                .targetTitle(request.getTargetTitle())
                .steps(steps)
                .build());
    }

    private static Map<String, CvSkillSignal> buildSkillLexicon() {
        Map<String, CvSkillSignal> lexicon = new LinkedHashMap<>();
        lexicon.put("java", CvSkillSignal.builder().skillName("Java").category("Backend").confidenceScore(0.72d).yearsOfExperience(1).build());
        lexicon.put("spring boot", CvSkillSignal.builder().skillName("Spring Boot").category("Backend").confidenceScore(0.72d).yearsOfExperience(1).build());
        lexicon.put("react", CvSkillSignal.builder().skillName("React").category("Frontend").confidenceScore(0.72d).yearsOfExperience(1).build());
        lexicon.put("typescript", CvSkillSignal.builder().skillName("TypeScript").category("Frontend").confidenceScore(0.72d).yearsOfExperience(1).build());
        lexicon.put("next.js", CvSkillSignal.builder().skillName("Next.js").category("Frontend").confidenceScore(0.72d).yearsOfExperience(1).build());
        lexicon.put("node.js", CvSkillSignal.builder().skillName("Node.js").category("Backend").confidenceScore(0.72d).yearsOfExperience(1).build());
        lexicon.put("sql", CvSkillSignal.builder().skillName("SQL").category("Database").confidenceScore(0.72d).yearsOfExperience(1).build());
        lexicon.put("docker", CvSkillSignal.builder().skillName("Docker").category("DevOps").confidenceScore(0.72d).yearsOfExperience(1).build());
        lexicon.put("aws", CvSkillSignal.builder().skillName("AWS").category("Cloud").confidenceScore(0.72d).yearsOfExperience(1).build());
        return lexicon;
    }
}
