package com.careerai.builder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Kết quả phân tích Gap Skill giữa CV và một Job cụ thể.
 * Module 5 — Gap Skill Analyzer output.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapAnalysisResult {

    private UUID jobId;
    private String jobTitle;
    private String company;
    private Double matchScore;             // 0-100%

    private List<String> matchingSkills;   // Skills có trong cả CV và JD
    private List<String> missingSkills;    // Skills có trong JD nhưng thiếu trong CV
    private List<String> extraSkills;      // Skills có trong CV nhưng không yêu cầu trong JD

    private String aiExplanation;          // AI-generated reasoning (Module 6)
}
