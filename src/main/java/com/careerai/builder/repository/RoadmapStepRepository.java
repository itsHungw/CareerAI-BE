package com.careerai.builder.repository;

import com.careerai.builder.domain.entity.Roadmap;
import com.careerai.builder.domain.entity.RoadmapStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoadmapStepRepository extends JpaRepository<RoadmapStep, UUID> {
    List<RoadmapStep> findByRoadmapOrderByOrderIndexAsc(Roadmap roadmap);
}
