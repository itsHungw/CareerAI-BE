package com.careerai.builder.repository;

import com.careerai.builder.domain.entity.Job;
import com.careerai.builder.domain.entity.JobRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRequirementRepository extends JpaRepository<JobRequirement, UUID> {
    List<JobRequirement> findByJob(Job job);

    @Query("SELECT jr FROM JobRequirement jr JOIN FETCH jr.skill WHERE jr.job = :job")
    List<JobRequirement> findByJobWithSkills(@Param("job") Job job);
}
