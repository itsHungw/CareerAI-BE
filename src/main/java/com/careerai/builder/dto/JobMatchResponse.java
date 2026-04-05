package com.careerai.builder.dto;

import com.careerai.builder.domain.entity.Job;
import com.careerai.builder.domain.entity.JobRequirement;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JobMatchResponse {
    private Job job;
    private Double matchPercentage;
    private List<JobRequirement> requirements;
}
