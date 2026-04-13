package com.careerai.builder.controller;

import com.careerai.builder.domain.entity.CV;
import com.careerai.builder.domain.entity.Job;
import com.careerai.builder.domain.entity.User;
import com.careerai.builder.dto.*;
import com.careerai.builder.exception.ApiException;
import com.careerai.builder.repository.CVRepository;
import com.careerai.builder.repository.JobRepository;
import com.careerai.builder.repository.UserRepository;
import com.careerai.builder.service.GapAnalysisService;
import com.careerai.builder.service.JobService;
import com.careerai.builder.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final JobService jobService;
    private final VectorSearchService vectorSearchService;
    private final GapAnalysisService gapAnalysisService;
    private final CVRepository cvRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    /**
     * Legacy endpoint: Skill-based matching (giữ lại cho backward compatibility).
     * Sử dụng logic so khớp kỹ năng thủ công từ JobService.
     */
    @GetMapping("/matches")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<JobMatchResponse>>> getMatchedJobs() {
        User user = getCurrentUser();

        List<CV> userCvs = cvRepository.findByUserOrderByCreatedAtDesc(user);
        if (userCvs.isEmpty()) {
            throw new ApiException("Please upload a CV first to get job matches", HttpStatus.BAD_REQUEST);
        }

        CV latestCv = userCvs.get(0);
        List<JobService.JobMatchResult> results = jobService.matchJobsForCV(latestCv);

        List<JobMatchResponse> dtoResponse = results.stream()
                .map(res -> JobMatchResponse.builder()
                        .job(res.getJob())
                        .matchPercentage(res.getMatchPercentage())
                        .requirements(res.getRequirements())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Matched jobs retrieved", dtoResponse));
    }

    /**
     * NEW — RAG-based Job Matching (Module 3 + 4).
     *
     * Flow: CV text → Vector Search (pgvector) → Metadata Filter → Ranked Jobs
     *
     * POST /api/jobs/match
     * Body: { "role": "backend", "level": "junior", "location": "HCM" }
     */
    @PostMapping("/match")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<MatchedJobResponse>>> matchJobsRag(
            @RequestBody(required = false) MatchQuery query) {

        User user = getCurrentUser();

        // Lấy CV mới nhất
        CV latestCv = cvRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        "Please upload a CV first to get AI-powered job matches", HttpStatus.BAD_REQUEST));

        if (latestCv.getRawText() == null || latestCv.getRawText().isBlank()) {
            throw new ApiException(
                    "Your CV could not be parsed. Please re-upload a readable PDF.", HttpStatus.BAD_REQUEST);
        }

        // Default empty query nếu null
        if (query == null) {
            query = new MatchQuery();
        }

        log.info("🔍 RAG Job Match — user: {}, role: {}, level: {}",
                user.getEmail(), query.getRole(), query.getLevel());

        List<MatchedJobResponse> matches = vectorSearchService.searchAndMatch(
                latestCv.getRawText(), query);

        return ResponseEntity.ok(ApiResponse.success(
                "AI-powered job matches retrieved (" + matches.size() + " results)", matches));
    }

    /**
     * NEW — Gap Analysis for a specific Job (Module 5).
     *
     * Chỉ chạy khi user click vào 1 Job cụ thể.
     * So sánh CV Skills vs JD Required Skills → trả về matching + missing skills.
     *
     * GET /api/jobs/{id}/analysis
     */
    @GetMapping("/{id}/analysis")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<GapAnalysisResult>> getJobAnalysis(@PathVariable UUID id) {

        User user = getCurrentUser();

        // Lấy CV mới nhất
        CV latestCv = cvRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        "Please upload a CV first", HttpStatus.BAD_REQUEST));

        // Lấy Job
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ApiException("Job not found", HttpStatus.NOT_FOUND));

        log.info("📊 Gap Analysis — user: {}, job: '{}'", user.getEmail(), job.getTitle());

        GapAnalysisResult result = gapAnalysisService.analyzeGapWithExplanation(latestCv, job);

        return ResponseEntity.ok(ApiResponse.success("Gap analysis completed", result));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }
}
