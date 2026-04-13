package com.careerai.builder.repository;

import com.careerai.builder.domain.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    @Query("SELECT DISTINCT j FROM Job j LEFT JOIN FETCH j.requirements r LEFT JOIN FETCH r.skill")
    List<Job> findAllWithRequirementsAndSkills();

    List<Job> findByRoleIgnoreCase(String role);

    List<Job> findByLevelIgnoreCase(String level);

    List<Job> findByRoleIgnoreCaseAndLevelIgnoreCase(String role, String level);

    List<Job> findByIngestionStatus(Job.IngestionStatus status);

    @Query("SELECT j FROM Job j WHERE j.id IN :ids")
    List<Job> findByIdIn(@Param("ids") List<UUID> ids);

    // We can add filtering by category/skills later
}
