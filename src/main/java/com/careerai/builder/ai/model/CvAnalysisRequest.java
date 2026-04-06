package com.careerai.builder.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CvAnalysisRequest {
    private final String fileName;
    private final String rawText;
}
