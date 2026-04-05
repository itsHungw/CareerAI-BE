package com.careerai.builder.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; // The normalized name (e.g., "React.js")

    @Column(nullable = false)
    private String category; // e.g., "Framework", "Language", "Soft Skill"

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "skill_aliases", joinColumns = @JoinColumn(name = "skill_id"))
    @Column(name = "alias")
    @Builder.Default
    private Set<String> aliases = new HashSet<>(); // e.g., ["React", "ReactJS"] for normalization
}
