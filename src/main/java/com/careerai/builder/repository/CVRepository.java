package com.careerai.builder.repository;

import com.careerai.builder.domain.entity.CV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CVRepository extends JpaRepository<CV, UUID> {
    List<CV> findByUserId(UUID userId);
}
