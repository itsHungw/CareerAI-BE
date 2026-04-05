package com.careerai.builder.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "roadmap_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadmapStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmap_id", nullable = false)
    private Roadmap roadmap;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String resources; // JSON serialized list of learning resources

    private Integer orderIndex;

    private Integer durationDays; // Estimated time to complete in days

    @Enumerated(EnumType.STRING)
    private StepStatus status;

    public enum StepStatus {
        TODO, IN_PROGRESS, DONE
    }
}
