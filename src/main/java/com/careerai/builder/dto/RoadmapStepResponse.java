package com.careerai.builder.dto;

import com.careerai.builder.domain.entity.RoadmapStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapStepResponse implements Serializable {
    private UUID id;
    private String title;
    private String description;
    private String resources;
    private Integer orderIndex;
    private Integer durationDays;
    private RoadmapStep.StepStatus status;
}
