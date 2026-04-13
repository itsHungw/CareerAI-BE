package com.careerai.builder.dto;

import com.careerai.builder.domain.entity.Job;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO cho kết quả RAG-based job matching.
 * Bao gồm thông tin Job + match score từ vector similarity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchedJobResponse {

    private UUID jobId;
    private String title;
    private String company;
    private String location;
    private String role;
    private String level;
    private String salaryRange;
    private String sourceUrl;
    private Double matchScore;       // 0-100, from vector similarity
    private Integer chunkMatches;    // Số chunks matched (confidence indicator)

    /**
     * Factory method to build response from Job entity + search results.
     */
    public static MatchedJobResponse fromJob(Job job, double matchScore, int chunkMatches) {
        return MatchedJobResponse.builder()
                .jobId(job.getId())
                .title(job.getTitle())
                .company(job.getCompany())
                .location(job.getLocation())
                .role(job.getRole())
                .level(job.getLevel())
                .salaryRange(job.getSalaryRange())
                .sourceUrl(job.getSourceUrl())
                .matchScore(Math.round(matchScore * 100.0) / 100.0) // round to 2 decimals
                .chunkMatches(chunkMatches)
                .build();
    }
}
