package com.careerai.builder.repository;

import com.careerai.builder.domain.entity.CV;
import com.careerai.builder.domain.entity.CVSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CVSkillRepository extends JpaRepository<CVSkill, UUID> {
    List<CVSkill> findByCv(CV cv);
    
    void deleteByCv(CV cv); // useful for re-parsing a CV
}
