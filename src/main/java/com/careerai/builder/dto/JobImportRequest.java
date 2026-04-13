package com.careerai.builder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobImportRequest {

    @NotBlank(message = "Job title is required")
    private String title;

    @NotBlank(message = "Company name is required")
    private String company;

    private String industry;       // Company industry (e.g., "Fintech", "SaaS")

    private String location;       // e.g., "Ho Chi Minh City", "Remote"

    @NotBlank(message = "Role is required (e.g., backend, frontend)")
    private String role;           // "backend", "frontend", "fullstack", "devops", "data"

    @NotBlank(message = "Level is required (e.g., junior, mid, senior)")
    private String level;          // "intern", "fresher", "junior", "mid", "senior", "lead"

    private String salaryRange;    // "15-25M", "negotiable"

    @NotBlank(message = "Job description is required")
    private String description;    // Plain text JD content

    private String sourceUrl;      // Original job posting URL
}
