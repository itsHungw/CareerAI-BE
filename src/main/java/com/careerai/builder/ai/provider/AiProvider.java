package com.careerai.builder.ai.provider;

import com.careerai.builder.ai.model.CvAnalysisRequest;
import com.careerai.builder.ai.model.CvAnalysisResult;
import com.careerai.builder.ai.model.RoadmapGenerationRequest;
import com.careerai.builder.ai.model.RoadmapGenerationResult;

import java.util.Optional;

public interface AiProvider {

    String getName();

    boolean isConfigured();

    Optional<CvAnalysisResult> analyzeCv(CvAnalysisRequest request);

    Optional<RoadmapGenerationResult> generateRoadmap(RoadmapGenerationRequest request);
}
