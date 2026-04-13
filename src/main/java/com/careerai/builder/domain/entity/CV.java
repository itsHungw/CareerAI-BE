package com.careerai.builder.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cvs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CV {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileUrl;

    @Column(columnDefinition = "TEXT")
    private String rawText;

    @Column(columnDefinition = "TEXT")
    private String review;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "cv", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CVSkill> skills = new ArrayList<>();

    @Column(name = "file_hash", length = 64)
    private String fileHash;  // SHA-256 hash for duplicate detection

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "last_analyzed_at")
    private LocalDateTime lastAnalyzedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
