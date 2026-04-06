package com.careerai.builder.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RoadmapGenerationRequest {
    private final String targetTitle;
    private final String cvSummary;
    private final List<String> currentSkills;
    private final List<String> missingSkills;
}
