package com.careerai.builder.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "roadmaps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Roadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id")
    private CV sourceCv;

    @Column(nullable = false)
    private String targetTitle;

    @Enumerated(EnumType.STRING)
    private RoadmapStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum RoadmapStatus {
        ACTIVE, ARCHIVED
    }
}
