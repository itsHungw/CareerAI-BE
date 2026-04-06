package com.careerai.builder.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvSkillSignal {
    private String skillName;
    private String category;
    private Double confidenceScore;
    private Integer yearsOfExperience;
}
