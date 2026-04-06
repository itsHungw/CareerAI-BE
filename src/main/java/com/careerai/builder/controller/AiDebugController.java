package com.careerai.builder.controller;

import com.careerai.builder.ai.AiOrchestratorService;
import com.careerai.builder.ai.model.CvAnalysisRequest;
import com.careerai.builder.ai.model.CvAnalysisResult;
import com.careerai.builder.dto.AiStatusResponse;
import com.careerai.builder.dto.AiTestRequest;
import com.careerai.builder.dto.AiTestResponse;
import com.careerai.builder.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug/ai")
@RequiredArgsConstructor
public class AiDebugController {

    private static final String DEFAULT_SAMPLE_TEXT = """
            Candidate resume sample:
            Java, Spring Boot, PostgreSQL, REST API, Docker, Git.
            Built backend services and maintained internal systems.
            """;

    private final AiOrchestratorService aiOrchestratorService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AiStatusResponse>> getStatus() {
        return ResponseEntity.ok(ApiResponse.success("AI status retrieved", aiOrchestratorService.getStatus()));
    }

    @PostMapping("/test-cv")
    public ResponseEntity<ApiResponse<AiTestResponse>> testCvAnalysis(@Valid @RequestBody(required = false) AiTestRequest request) {
        String sampleText = request == null || request.getSampleText() == null || request.getSampleText().isBlank()
                ? DEFAULT_SAMPLE_TEXT
                : request.getSampleText();

        CvAnalysisResult result = aiOrchestratorService.analyzeCv(CvAnalysisRequest.builder()
                        .fileName("debug-sample.txt")
                        .rawText(sampleText)
                        .build())
                .orElseGet(() -> CvAnalysisResult.builder()
                        .summary("AI provider did not return a result")
                        .parsedContent("No AI output")
                        .skills(java.util.List.of())
                        .build());

        AiStatusResponse status = aiOrchestratorService.getStatus();
        AiTestResponse response = AiTestResponse.builder()
                .aiUsed(result.getSkills() != null && !result.getSkills().isEmpty())
                .provider(status.getPrimaryProvider())
                .summary(result.getSummary())
                .parsedContent(result.getParsedContent())
                .skillCount(result.getSkills() == null ? 0 : result.getSkills().size())
                .build();

        return ResponseEntity.ok(ApiResponse.success("AI CV test completed", response));
    }
}
