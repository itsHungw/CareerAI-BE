package com.careerai.builder.service;

import com.careerai.builder.domain.entity.Skill;
import com.careerai.builder.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillService {

    private final SkillRepository skillRepository;

    /**
     * Normalizes a raw skill string extracted from a CV or Job Description.
     * Uses the database alias lookup to find existing exact or synonymous skills.
     * If it doesn't exist, it creates a new normalized skill entry.
     *
     * @param rawSkillName e.g., "ReactJS", "Node.js", "java"
     * @param category     e.g., "Framework", "Language"
     * @return the normalized Skill entity
     */
    @Transactional
    public Skill normalizeAndHandleSkill(String rawSkillName, String category) {
        String trimmedSkill = rawSkillName.trim();
        
        // Lookup using the custom alias query
        Optional<Skill> existingSkill = skillRepository.findByNameOrAlias(trimmedSkill);

        if (existingSkill.isPresent()) {
            return existingSkill.get();
        }

        // If not found, we create a new "Normalized" standard base on the input
        log.info("Creating new normalized skill for: {}", trimmedSkill);
        Skill newSkill = Skill.builder()
                .name(trimmedSkill) // Assuming the first time we see it, it acts as the primary name
                .category(category)
                .build();
        newSkill.getAliases().add(trimmedSkill.toLowerCase()); // Add self to aliases for indexing
        
        return skillRepository.save(newSkill);
    }
    
    /**
     * Adds an alias to an existing skill if they are deemed synonymous.
     */
    @Transactional
    public void addAliasToSkill(Skill skill, String alias) {
        skill.getAliases().add(alias.trim().toLowerCase());
        skillRepository.save(skill);
    }
}
