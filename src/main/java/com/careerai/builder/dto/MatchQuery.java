package com.careerai.builder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query object cho RAG-based job matching.
 * User gửi cùng với CV để tìm kiếm Job phù hợp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchQuery {

    private String role;       // "backend", "frontend", "fullstack", "devops", "data", "mobile"
    private String level;      // "intern", "fresher", "junior", "mid", "senior", "lead"
    private String location;   // Optional: "Ho Chi Minh City", "Remote"
}
