package com.careerai.builder.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String industry;       // "Fintech", "E-commerce", "SaaS", "EdTech"

    private String size;           // "startup", "mid", "enterprise"

    private String website;

    @Column(columnDefinition = "TEXT")
    private String description;    // Company profile for semantic search context

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
