package com.careerai.builder.service;

import com.careerai.builder.domain.entity.*;
import com.careerai.builder.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final JobRequirementRepository jobRequirementRepository;
    private final CVSkillRepository cvSkillRepository;

    /**
     * Matching logic:
     * Calculates a match score for a job based on the skills found in a CV.
     * Score = (Number of mandatory skills matched) / (Total mandatory skills)
     */
    @Transactional(readOnly = true)
    public List<JobMatchResult> matchJobsForCV(CV cv) {
        List<CVSkill> userSkills = cvSkillRepository.findByCv(cv);
        Map<String, Skill> userSkillMap = userSkills.stream()
                .collect(Collectors.toMap(cs -> cs.getSkill().getName().toLowerCase(), CVSkill::getSkill));

        List<Job> allJobs = jobRepository.findAllWithRequirementsAndSkills();

        return allJobs.stream()
                .map(job -> {
                List<JobRequirement> requirements = job.getRequirements();

                    long mandatoryCount = requirements.stream().filter(JobRequirement::getIsMandatory).count();
                    long matchCount = requirements.stream()
                            .filter(req -> userSkillMap.containsKey(req.getSkill().getName().toLowerCase()))
                            .count();
                    
                    double matchPercentage = mandatoryCount > 0 ? (double) matchCount / mandatoryCount * 100 : 0;

                    return new JobMatchResult(job, matchPercentage, requirements);
                })
                .sorted(Comparator.comparing(JobMatchResult::getMatchPercentage).reversed())
                .collect(Collectors.toList());
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class JobMatchResult {
        private Job job;
        private Double matchPercentage;
        private List<JobRequirement> requirements;
    }

    @Transactional
    public Job createJob(Job job, List<JobRequirement> requirements) {
        Job savedJob = jobRepository.save(job);
        requirements.forEach(req -> {
            req.setJob(savedJob);
            jobRequirementRepository.save(req);
        });
        return savedJob;
    }
}
