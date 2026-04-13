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
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String company;           // Legacy field (plain text company name)

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company companyEntity;    // FK to companies table

    private String location;

    @Column(length = 50)
    private String role;              // "backend", "frontend", "fullstack", "devops", "data", "mobile"

    @Column(length = 30)
    private String level;             // "intern", "fresher", "junior", "mid", "senior", "lead"

    private String salaryRange;       // "15-25M", "negotiable", "$3000-5000"

    @Column(columnDefinition = "TEXT")
    private String descriptionHtml;   // For rich text web display

    @Column(columnDefinition = "TEXT")
    private String rawDescription;    // Plain text version of JD (for embedding)

    private String sourceUrl;         // Link to the original posting

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobRequirement> requirements = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 20, nullable = false)
    private IngestionStatus ingestionStatus = IngestionStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersistDefaults() {
        if (ingestionStatus == null) {
            ingestionStatus = IngestionStatus.PENDING;
        }
    }

    public enum IngestionStatus {
        PENDING, INGESTED, FAILED
    }
}
