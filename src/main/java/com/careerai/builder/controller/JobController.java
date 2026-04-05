package com.careerai.builder.controller;

import com.careerai.builder.domain.entity.CV;
import com.careerai.builder.domain.entity.User;
import com.careerai.builder.dto.ApiResponse;
import com.careerai.builder.exception.ApiException;
import com.careerai.builder.repository.CVRepository;
import com.careerai.builder.repository.UserRepository;
import com.careerai.builder.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final CVRepository cvRepository;
    private final UserRepository userRepository;

    @GetMapping("/matches")
    public ResponseEntity<ApiResponse<List<com.careerai.builder.dto.JobMatchResponse>>> getMatchedJobs() {
        User user = getCurrentUser();
        
        List<CV> userCvs = cvRepository.findByUserOrderByCreatedAtDesc(user);
        if (userCvs.isEmpty()) {
            throw new ApiException("Please upload a CV first to get job matches", HttpStatus.BAD_REQUEST);
        }

        CV latestCv = userCvs.get(0);
        List<JobService.JobMatchResult> results = jobService.matchJobsForCV(latestCv);

        // Convert Results to DTOs
        List<com.careerai.builder.dto.JobMatchResponse> dtoResponse = results.stream()
                .map(res -> com.careerai.builder.dto.JobMatchResponse.builder()
                        .job(res.getJob())
                        .matchPercentage(res.getMatchPercentage())
                        .requirements(res.getRequirements())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Matched jobs retrieved", dtoResponse));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }
}
