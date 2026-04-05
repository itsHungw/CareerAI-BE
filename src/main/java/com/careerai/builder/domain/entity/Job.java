package com.careerai.builder.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String company;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String descriptionHtml; // For rich text web display

    private String sourceUrl; // Link to the original posting

    @CreationTimestamp
    private LocalDateTime createdAt;
}
