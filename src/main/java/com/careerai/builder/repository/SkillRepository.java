package com.careerai.builder.repository;

import com.careerai.builder.domain.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {
    
    Optional<Skill> findByNameIgnoreCase(String name);

    // Custom query to find a skill by name or by its aliases
    @Query("SELECT s FROM Skill s LEFT JOIN s.aliases a WHERE LOWER(s.name) = LOWER(:searchTerm) OR LOWER(a) = LOWER(:searchTerm)")
    Optional<Skill> findByNameOrAlias(@Param("searchTerm") String searchTerm);
}
