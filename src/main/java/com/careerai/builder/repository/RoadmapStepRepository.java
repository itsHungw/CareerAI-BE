package com.careerai.builder.repository;

import com.careerai.builder.domain.entity.Roadmap;
import com.careerai.builder.domain.entity.RoadmapStep;
import com.careerai.builder.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoadmapStepRepository extends JpaRepository<RoadmapStep, UUID> {
    List<RoadmapStep> findByRoadmapOrderByOrderIndexAsc(Roadmap roadmap);

    Optional<RoadmapStep> findByIdAndRoadmapUser(UUID id, User user);
}
