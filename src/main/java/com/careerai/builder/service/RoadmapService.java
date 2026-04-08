package com.careerai.builder.service;

import com.careerai.builder.ai.AiOrchestratorService;
import com.careerai.builder.ai.model.RoadmapGenerationRequest;
import com.careerai.builder.ai.model.RoadmapGenerationResult;
import com.careerai.builder.ai.model.RoadmapStepSuggestion;
import com.careerai.builder.domain.entity.CV;
import com.careerai.builder.domain.entity.CVSkill;
import com.careerai.builder.domain.entity.Roadmap;
import com.careerai.builder.domain.entity.RoadmapStep;
import com.careerai.builder.domain.entity.User;
import com.careerai.builder.exception.ApiException;
import com.careerai.builder.repository.CVRepository;
import com.careerai.builder.repository.CVSkillRepository;
import com.careerai.builder.repository.RoadmapRepository;
import com.careerai.builder.repository.RoadmapStepRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapService {

    private final AiOrchestratorService aiOrchestratorService;
    private final RoadmapRepository roadmapRepository;
    private final RoadmapStepRepository roadmapStepRepository;
    private final CVRepository cvRepository;
    private final CVSkillRepository cvSkillRepository;
    private final ObjectMapper objectMapper;

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class RoadmapPlan {
        private String targetTitle;
        private List<StepDefinition> steps;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class StepDefinition {
        private String title;
        private String description;
        private String resourcesJson;
        private Integer durationDays;
    }

    @Transactional
    public Roadmap generateRoadmap(User user, CV sourceCv, RoadmapPlan plan) {
        log.info("Generating roadmap '{}' for user: {}", plan.getTargetTitle(), user.getEmail());

        archiveActiveRoadmaps(user);

        Roadmap roadmap = Roadmap.builder()
                .user(user)
                .sourceCv(sourceCv)
                .targetTitle(plan.getTargetTitle())
                .status(Roadmap.RoadmapStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Roadmap savedRoadmap = roadmapRepository.save(roadmap);

        List<RoadmapStep> steps = new ArrayList<>();
        for (int i = 0; i < plan.getSteps().size(); i++) {
            StepDefinition stepDef = plan.getSteps().get(i);
            steps.add(RoadmapStep.builder()
                    .roadmap(savedRoadmap)
                    .title(stepDef.getTitle())
                    .description(stepDef.getDescription())
                    .resources(stepDef.getResourcesJson())
                    .orderIndex(i)
                    .durationDays(stepDef.getDurationDays())
                    .status(RoadmapStep.StepStatus.TODO)
                    .build());
        }

        roadmapStepRepository.saveAll(steps);
        return savedRoadmap;
    }

    @Transactional
    @CacheEvict(value = "userRoadmaps", key = "#user.id")
    public Roadmap generateRoadmapFromLatestCv(User user, String requestedTargetTitle) {
        CV latestCv = cvRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .findFirst()
                .orElseThrow(() -> new ApiException("Please upload a CV before generating a roadmap", HttpStatus.BAD_REQUEST));

        List<CVSkill> cvSkills = cvSkillRepository.findByCv(latestCv);
        RoadmapPlan plan = buildPlan(latestCv, cvSkills, requestedTargetTitle);
        return generateRoadmap(user, latestCv, plan);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "userRoadmaps", key = "#user.id")
    public List<Roadmap> getUserRoadmaps(User user) {
        return roadmapRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "roadmapSteps", key = "#roadmapId")
    public List<RoadmapStep> getRoadmapSteps(UUID roadmapId) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new IllegalArgumentException("Roadmap not found"));
        return roadmapStepRepository.findByRoadmapOrderByOrderIndexAsc(roadmap);
    }

    @Transactional
    public RoadmapStep updateStepStatus(UUID stepId, RoadmapStep.StepStatus newStatus) {
        RoadmapStep step = roadmapStepRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step not found"));

        UUID roadmapId = step.getRoadmap().getId();  // Extract roadmapId for cache invalidation
        step.setStatus(newStatus);

        RoadmapStep saved = roadmapStepRepository.save(step);
        invalidateRoadmapStepsCache(roadmapId);  // Invalidate cache after save

        return saved;
    }

    /**
     * Private method to invalidate roadmap steps cache
     * Called after a step status is updated
     */
    @CacheEvict(value = "roadmapSteps", key = "#roadmapId")
    private void invalidateRoadmapStepsCache(UUID roadmapId) {
        log.debug("Invalidating roadmap steps cache for roadmap: {}", roadmapId);
    }

    private void archiveActiveRoadmaps(User user) {
        List<Roadmap> activeRoadmaps = roadmapRepository.findByUserAndStatusOrderByCreatedAtDesc(user, Roadmap.RoadmapStatus.ACTIVE);
        activeRoadmaps.forEach(roadmap -> roadmap.setStatus(Roadmap.RoadmapStatus.ARCHIVED));
        if (!activeRoadmaps.isEmpty()) {
            roadmapRepository.saveAll(activeRoadmaps);
        }
    }

    private RoadmapPlan buildPlan(CV latestCv, List<CVSkill> cvSkills, String requestedTargetTitle) {
        Set<String> currentSkills = new LinkedHashSet<>();
        cvSkills.forEach(cvSkill -> currentSkills.add(cvSkill.getSkill().getName()));

        String targetTitle = resolveTargetTitle(requestedTargetTitle, currentSkills);
        List<String> targetSkills = resolveTargetSkills(targetTitle);
        List<String> missingSkills = targetSkills.stream()
                .filter(targetSkill -> currentSkills.stream().noneMatch(existing -> existing.equalsIgnoreCase(targetSkill)))
                .toList();

        List<StepDefinition> steps = new ArrayList<>();
        steps.add(new StepDefinition(
                "Audit your current profile",
                "Review the latest CV insights, identify strengths already visible in your profile, and define a concrete " + targetTitle + " outcome for the next 8 to 12 weeks.",
                resourcesJson("Review CV summary", "Pick 3 target companies", "Write a role scorecard"),
                3));

        List<String> prioritizedMissingSkills = missingSkills.isEmpty()
                ? List.of("Portfolio depth", "Interview storytelling")
                : missingSkills.stream().limit(3).toList();

        for (String missingSkill : prioritizedMissingSkills) {
            String[] skillResources = generateSkillResources(missingSkill);
            steps.add(new StepDefinition(
                    "Master " + missingSkill,
                    "Create a focused learning sprint around " + missingSkill + ". Study core concepts, build a hands-on project, and document your learning. Connect it directly to the " + targetTitle + " role requirements.",
                    resourcesJson(skillResources),
                    10));
        }

        steps.add(new StepDefinition(
                "Build portfolio project",
                "Ship a portfolio artifact that showcases your target " + targetTitle + " skills. Combine your strongest existing skills with newly learned " +
                (missingSkills.isEmpty() ? "advanced techniques" : missingSkills.get(0)) +
                " to demonstrate real capability.",
                resourcesJson("Project requirements doc", "View on GitHub", "Demo/deployed link", "README with metrics"),
                14));

        steps.add(new StepDefinition(
                "Prepare interview storytelling",
                "Turn your experience into 3-5 structured stories covering technical decisions, problem-solving, and impact. Focus on projects that align with " + targetTitle + " responsibilities.",
                resourcesJson("STAR method guide", "Record practice interview", "Key achievements list"),
                5));

        steps.add(new StepDefinition(
                "Execute job search",
                "Launch your application campaign targeting " + targetTitle + " roles. Apply to 2-3 companies weekly, track feedback, and refine your messaging based on patterns.",
                resourcesJson("Target company list", "Weekly application log", "Offer negotiation checklist"),
                7));

        if (latestCv.getSummary() != null && !latestCv.getSummary().isBlank()) {
            steps.get(0).setDescription(steps.get(0).getDescription() + "\n\nProfile Snapshot: " + latestCv.getSummary());
        }

        RoadmapPlan deterministicPlan = new RoadmapPlan(targetTitle, steps);

        return aiOrchestratorService.generateRoadmap(RoadmapGenerationRequest.builder()
                        .targetTitle(targetTitle)
                        .cvSummary(latestCv.getSummary() == null ? "" : latestCv.getSummary())
                        .currentSkills(new ArrayList<>(currentSkills))
                        .missingSkills(missingSkills)
                        .build())
                .filter(this::hasUsableAiRoadmap)
                .map(this::toRoadmapPlan)
                .orElseGet(() -> {
                    log.warn("⚠️ AI roadmap generation returned unusable result or failed. Using personalized fallback plan.");
                    log.info("✓ Fallback roadmap generated {} steps for target: {}", deterministicPlan.getSteps().size(), targetTitle);
                    return deterministicPlan;
                });
    }

    private String[] generateSkillResources(String skill) {
        String lowerSkill = skill.toLowerCase();

        if (lowerSkill.contains("react")) {
            return new String[]{"React official docs", "Build interactive components", "React Hooks deep dive", "component library project"};
        } else if (lowerSkill.contains("typescript")) {
            return new String[]{"TypeScript handbook", "Type exercises", "Real-world typing", "TS strict mode setup"};
        } else if (lowerSkill.contains("node") || lowerSkill.contains("express")) {
            return new String[]{"Node.js guide", "Build REST API", "Middleware & middleware", "Performance optimization"};
        } else if (lowerSkill.contains("aws") || lowerSkill.contains("cloud")) {
            return new String[]{"AWS Free Tier intro", "Deploy a web app", "Security best practices", "Cost optimization"};
        } else if (lowerSkill.contains("docker") || lowerSkill.contains("container")) {
            return new String[]{"Docker fundamentals", "Multi-stage builds", "Compose setup", "Kubernetes intro"};
        } else if (lowerSkill.contains("sql") || lowerSkill.contains("database")) {
            return new String[]{"SQL query optimization", "Schema design", "Index strategies", "Build sample database"};
        } else if (lowerSkill.contains("java") || lowerSkill.contains("spring")) {
            return new String[]{"Spring Boot guide", "Build microservice", "Dependency injection", "Testing patterns"};
        } else if (lowerSkill.contains("python")) {
            return new String[]{"Python fundamentals", "Build CLI tool", "Package structure", "Testing with pytest"};
        } else if (lowerSkill.contains("git") || lowerSkill.contains("devops")) {
            return new String[]{"Git workflow", "CI/CD pipeline", "Code review process", "Release management"};
        } else {
            return new String[]{"Official documentation", "Build practical project", "Real-world examples", "Contribute to open source"};
        }
    }

    private boolean hasUsableAiRoadmap(RoadmapGenerationResult result) {
        return result != null
                && result.getTargetTitle() != null
                && !result.getTargetTitle().isBlank()
                && result.getSteps() != null
                && !result.getSteps().isEmpty();
    }

    private RoadmapPlan toRoadmapPlan(RoadmapGenerationResult result) {
        List<StepDefinition> steps = result.getSteps().stream()
                .map(this::toStepDefinition)
                .toList();
        return new RoadmapPlan(result.getTargetTitle(), steps);
    }

    private StepDefinition toStepDefinition(RoadmapStepSuggestion suggestion) {
        List<String> resources = suggestion.getResources() == null ? List.of() : suggestion.getResources();
        return new StepDefinition(
                suggestion.getTitle(),
                suggestion.getDescription(),
                resourcesJson(resources.toArray(String[]::new)),
                suggestion.getDurationDays() == null ? 7 : suggestion.getDurationDays());
    }

    private String resolveTargetTitle(String requestedTargetTitle, Set<String> currentSkills) {
        if (requestedTargetTitle != null && !requestedTargetTitle.isBlank()) {
            return requestedTargetTitle.trim();
        }

        Set<String> normalizedSkills = currentSkills.stream()
                .map(skill -> skill.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());

        boolean hasFrontend = normalizedSkills.contains("react") || normalizedSkills.contains("typescript") || normalizedSkills.contains("next.js");
        boolean hasBackend = normalizedSkills.contains("java") || normalizedSkills.contains("spring boot") || normalizedSkills.contains("node.js");

        if (hasFrontend && hasBackend) {
            return "Fullstack Developer";
        }
        if (hasBackend) {
            return "Backend Developer";
        }
        if (hasFrontend) {
            return "Frontend Developer";
        }
        return "Software Engineer";
    }

    private List<String> resolveTargetSkills(String targetTitle) {
        String normalizedTarget = targetTitle.toLowerCase(Locale.ROOT);

        if (normalizedTarget.contains("front")) {
            return List.of("React", "TypeScript", "Next.js", "HTML", "CSS", "Git");
        }
        if (normalizedTarget.contains("back") || normalizedTarget.contains("java")) {
            return List.of("Java", "Spring Boot", "SQL", "PostgreSQL", "Docker", "Git");
        }
        if (normalizedTarget.contains("full")) {
            return List.of("React", "TypeScript", "Java", "Spring Boot", "SQL", "Docker");
        }
        return List.of("Problem Solving", "Communication", "Git", "SQL", "Testing", "System Design");
    }

    private String resourcesJson(String... resources) {
        try {
            return objectMapper.writeValueAsString(List.of(resources));
        } catch (JsonProcessingException e) {
            // Fallback: return empty JSON array
            return "[]";
        }
    }
}
