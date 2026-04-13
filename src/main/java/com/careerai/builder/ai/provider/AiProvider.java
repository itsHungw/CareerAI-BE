package com.careerai.builder.ai.provider;

import com.careerai.builder.ai.model.CvAnalysisRequest;
import com.careerai.builder.ai.model.CvAnalysisResult;
import com.careerai.builder.ai.model.RoadmapGenerationRequest;
import com.careerai.builder.ai.model.RoadmapGenerationResult;

import java.util.List;
import java.util.Optional;

public interface AiProvider {

    String getName();

    boolean isConfigured();

    Optional<CvAnalysisResult> analyzeCv(CvAnalysisRequest request);

    Optional<RoadmapGenerationResult> generateRoadmap(RoadmapGenerationRequest request);

    /**
     * Module 6 — Gap Explanation
     * Giải thích tại sao CV phù hợp/không phù hợp với JD.
     */
    Optional<String> explainJobMatch(String cvSummary, String jobDescription,
                                      List<String> matchingSkills, List<String> missingSkills);
}
