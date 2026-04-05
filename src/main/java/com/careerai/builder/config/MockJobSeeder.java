package com.careerai.builder.config;

import com.careerai.builder.domain.entity.Job;
import com.careerai.builder.domain.entity.JobRequirement;
import com.careerai.builder.domain.entity.Skill;
import com.careerai.builder.repository.JobRepository;
import com.careerai.builder.repository.JobRequirementRepository;
import com.careerai.builder.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


import java.util.UUID; // Removed unused List


@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("!test") // Only seed if not in test profile
public class MockJobSeeder implements CommandLineRunner {

    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final JobRequirementRepository jobRequirementRepository;

    @Override
    public void run(String... args) {
        if (jobRepository.count() > 0) {
            log.info("Jobs already exists, skipping mock data seeding.");
            return;
        }

        log.info("Seeding Mock Job Data for UI testing...");

        // Create standard skills if they don't exist
        Skill java = getOrCreateSkill("Java", "Backend");
        Skill springBoot = getOrCreateSkill("Spring Boot", "Backend");
        Skill react = getOrCreateSkill("React", "Frontend");
        Skill typeScript = getOrCreateSkill("TypeScript", "Frontend");
        getOrCreateSkill("Node.js", "Backend");

        // Job 1: Java Developer
        Job javaJob = Job.builder()
                .title("Junior Java Developer")
                .company("TechGlobal")
                .location("Ho Chi Minh City, VN")
                .descriptionHtml("We are looking for a Junior Java Backend Developer with a focus on Spring Boot applications.")
                .sourceUrl("https://mock-job.com/java-1")
                .build();
        jobRepository.save(javaJob);
        createRequirement(javaJob, java, true, 1);
        createRequirement(javaJob, springBoot, true, 0);

        // Job 2: Frontend Engineer
        Job frontendJob = Job.builder()
                .title("Senior Frontend Developer (React)")
                .company("CreativeUI")
                .location("Remote")
                .descriptionHtml("Leading high-performing web application features using React and modern CSS.")
                .sourceUrl("https://mock-job.com/react-1")
                .build();
        jobRepository.save(frontendJob);
        createRequirement(frontendJob, react, true, 3);
        createRequirement(frontendJob, typeScript, false, 2);

        log.info("Finished seeding mock job data.");
    }

    private Skill getOrCreateSkill(String name, String category) {
        return skillRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> skillRepository.save(Skill.builder()
                        .name(name)
                        .category(category)
                        .build()));
    }

    private void createRequirement(Job job, Skill skill, boolean isMandatory, Integer years) {
        jobRequirementRepository.save(JobRequirement.builder()
                .job(job)
                .skill(skill)
                .isMandatory(isMandatory)
                .preferredYearsOfExperience(years)
                .build());
    }
}
