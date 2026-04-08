package com.careerai.builder.dto;

import com.careerai.builder.domain.entity.Roadmap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapResponse implements Serializable {
    private UUID id;
    private String targetTitle;
    private Roadmap.RoadmapStatus status;
    private LocalDateTime createdAt;
}
