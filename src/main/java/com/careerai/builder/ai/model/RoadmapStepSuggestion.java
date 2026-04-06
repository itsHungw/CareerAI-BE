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
public class RoadmapStepSuggestion {
    private String title;
    private String description;
    private Integer durationDays;

    @Builder.Default
    private List<String> resources = new ArrayList<>();
}
