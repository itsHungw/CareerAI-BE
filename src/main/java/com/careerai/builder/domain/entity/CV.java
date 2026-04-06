package com.careerai.builder.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cvs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CV {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileUrl;

    @Column(columnDefinition = "TEXT")
    private String rawText; // The full text extracted from PDF

    @Column(columnDefinition = "TEXT")
    private String review; // AI-generated professional critique and suggestions

    @Column(columnDefinition = "TEXT")
    private String summary; // 1-2 paragraph professional overview

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
