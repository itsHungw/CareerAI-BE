package com.careerai.builder.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateRoadmapRequest {

    @Size(max = 120, message = "targetTitle must be at most 120 characters")
    private String targetTitle;
}
