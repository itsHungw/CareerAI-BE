package com.careerai.builder.controller;

import com.careerai.builder.domain.entity.Roadmap;
import com.careerai.builder.domain.entity.RoadmapStep;
import com.careerai.builder.domain.entity.User;
import com.careerai.builder.dto.ApiResponse;
import com.careerai.builder.dto.GenerateRoadmapRequest;
import com.careerai.builder.dto.RoadmapResponse;
import com.careerai.builder.dto.RoadmapStepResponse;
import com.careerai.builder.exception.ApiException;
import com.careerai.builder.repository.UserRepository;
import jakarta.validation.Valid;
import com.careerai.builder.service.RoadmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roadmaps")
@RequiredArgsConstructor
public class RoadmapController {

    private final RoadmapService roadmapService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoadmapResponse>>> getMyRoadmaps(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Roadmaps retrieved successfully", 
                roadmapService.getUserRoadmaps(user, PageRequest.of(page, size)).getRoadmaps()));
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Roadmap>> generateRoadmap(@Valid @RequestBody(required = false) GenerateRoadmapRequest request) {
        User user = getCurrentUser();
        String targetTitle = request == null ? null : request.getTargetTitle();
        Roadmap roadmap = roadmapService.generateRoadmapFromLatestCv(user, targetTitle);
        return ResponseEntity.ok(ApiResponse.success("Roadmap generated successfully", roadmap));
    }

    @GetMapping("/{id}/steps")
    public ResponseEntity<ApiResponse<List<RoadmapStepResponse>>> getRoadmapSteps(@PathVariable UUID id) {
        User user = getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Steps retrieved successfully", roadmapService.getRoadmapSteps(id, user).getSteps()));
    }

    @PatchMapping("/steps/{stepId}/status")
    public ResponseEntity<ApiResponse<RoadmapStep>> updateStepStatus(
            @PathVariable UUID stepId, 
            @RequestParam RoadmapStep.StepStatus status) {
        User user = getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Status updated", roadmapService.updateStepStatus(stepId, status, user)));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }
}
