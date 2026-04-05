package com.careerai.builder.repository;

import com.careerai.builder.domain.entity.Job;
import com.careerai.builder.domain.entity.JobRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRequirementRepository extends JpaRepository<JobRequirement, UUID> {
    List<JobRequirement> findByJob(Job job);
}
