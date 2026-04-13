package com.careerai.builder.ai.provider;

import com.careerai.builder.ai.config.AiProperties;
import com.careerai.builder.ai.model.CvAnalysisRequest;
import com.careerai.builder.ai.model.CvAnalysisResult;
import com.careerai.builder.ai.model.RoadmapGenerationRequest;
import com.careerai.builder.ai.model.RoadmapGenerationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class FptProvider extends AbstractHttpAiProvider implements AiProvider {

    private final AiProperties aiProperties;
    private final AiPromptFactory promptFactory;

    public FptProvider(ObjectMapper objectMapper, AiProperties aiProperties, AiPromptFactory promptFactory) {
        super(objectMapper, aiProperties.getTimeoutSeconds());
        this.aiProperties = aiProperties;
        this.promptFactory = promptFactory;
    }

    @Override
    public String getName() {
        return "fpt";
    }

    @Override
    public boolean isConfigured() {
        return !aiProperties.getFpt().getApiKey().isBlank();
    }

    @Override
    public Optional<CvAnalysisResult> analyzeCv(CvAnalysisRequest request) {
        try {
            log.info("FPT AI CV analysis request using model '{}'", aiProperties.getFpt().getModel());

            JsonNode requestBody = buildRequestBody(promptFactory.buildCvSystemPrompt(), promptFactory.buildCvUserPrompt(request));
            log.debug("FPT CV analysis request body: {}", requestBody);

            JsonNode response = postJson(
                    aiProperties.getFpt().getBaseUrl() + "/chat/completions",
                    requestBody,
                    authHeaders(aiProperties.getFpt().getApiKey()));

            log.debug("FPT CV analysis raw response: {}", response);

            String extractedText = extractText(response);
            log.debug("FPT CV analysis extracted text: {}", extractedText);

            CvAnalysisResult result = readJsonPayload(extractedText, CvAnalysisResult.class);
            validateCvReviewFormat(result.getReview());
            log.info("✓ FPT AI CV analysis succeeded with {} skills extracted",
                    result.getSkills() != null ? result.getSkills().size() : 0);

            return Optional.of(result);
        } catch (Exception ex) {
            log.warn("✗ FPT AI CV analysis failed: {}", ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    @Override
    public Optional<RoadmapGenerationResult> generateRoadmap(RoadmapGenerationRequest request) {
        try {
            log.info("FPT AI roadmap generation request using model '{}'", aiProperties.getFpt().getModel());

            JsonNode requestBody = buildRequestBody(promptFactory.buildRoadmapSystemPrompt(), promptFactory.buildRoadmapUserPrompt(request));
            log.debug("FPT roadmap generation request body: {}", requestBody);

            JsonNode response = postJson(
                    aiProperties.getFpt().getBaseUrl() + "/chat/completions",
                    requestBody,
                    authHeaders(aiProperties.getFpt().getApiKey()));

            log.debug("FPT roadmap generation raw response: {}", response);

            String extractedText = extractText(response);
            log.debug("FPT roadmap generation extracted text: {}", extractedText);

            RoadmapGenerationResult result = readJsonPayload(extractedText, RoadmapGenerationResult.class);
            log.info("✓ FPT AI roadmap generation succeeded with {} steps generated",
                    result.getSteps() != null ? result.getSteps().size() : 0);

            return Optional.of(result);
        } catch (Exception ex) {
            log.warn("✗ FPT AI roadmap generation failed: {}", ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> explainJobMatch(String cvSummary, String jobDescription,
                                             List<String> matchingSkills, List<String> missingSkills) {
        try {
            log.info("FPT AI gap explanation request using model '{}'", aiProperties.getFpt().getModel());

            // Gap explanation trả về Markdown, không phải JSON → dùng text mode
            JsonNode requestBody = buildTextRequestBody(
                    promptFactory.buildGapExplanationSystemPrompt(),
                    promptFactory.buildGapExplanationUserPrompt(cvSummary, jobDescription, matchingSkills, missingSkills));

            JsonNode response = postJson(
                    aiProperties.getFpt().getBaseUrl() + "/chat/completions",
                    requestBody,
                    authHeaders(aiProperties.getFpt().getApiKey()));

            String explanation = extractText(response);
            log.info("✓ FPT AI gap explanation succeeded ({} chars)", explanation.length());
            return Optional.of(explanation);
        } catch (Exception ex) {
            log.warn("✗ FPT AI gap explanation failed: {}", ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    private JsonNode buildRequestBody(String systemPrompt, String userPrompt) {
        return getObjectMapperTree(Map.of(
                "model", aiProperties.getFpt().getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)),
                "temperature", aiProperties.getFpt().getTemperature(),
                "response_format", Map.of("type", "json_object")));
    }

    /**
     * Build request body for text/markdown responses (no JSON format constraint).
     */
    private JsonNode buildTextRequestBody(String systemPrompt, String userPrompt) {
        return getObjectMapperTree(Map.of(
                "model", aiProperties.getFpt().getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)),
                "temperature", aiProperties.getFpt().getTemperature()));
    }

    private HttpHeaders authHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        headers.add("api-key", apiKey);
        return headers;
    }

    private String extractText(JsonNode response) {
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        if (content.isTextual()) {
            return content.asText();
        }
        throw new IllegalStateException("FPT AI response did not contain message content");
    }
}
