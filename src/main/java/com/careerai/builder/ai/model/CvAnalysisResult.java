package com.careerai.builder.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvAnalysisResult {
    private String summary;
    private String parsedContent;

    @Builder.Default
    private List<CvSkillSignal> skills = new ArrayList<>();
}
