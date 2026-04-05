package com.careerai.builder.repository;

import com.careerai.builder.domain.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    // We can add filtering by category/skills later
}
