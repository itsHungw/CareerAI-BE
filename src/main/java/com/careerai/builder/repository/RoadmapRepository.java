package com.careerai.builder.repository;

import com.careerai.builder.domain.entity.Roadmap;
import com.careerai.builder.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoadmapRepository extends JpaRepository<Roadmap, UUID> {
    List<Roadmap> findByUserOrderByCreatedAtDesc(User user);
    
    List<Roadmap> findByUserAndStatusOrderByCreatedAtDesc(User user, Roadmap.RoadmapStatus status);
}
