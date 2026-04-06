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

import java.util.Optional;

@Component
@Slf4j
public class GeminiProvider extends AbstractHttpAiProvider implements AiProvider {

    private final AiProperties aiProperties;
    private final AiPromptFactory promptFactory;

    public GeminiProvider(ObjectMapper objectMapper, AiProperties aiProperties, AiPromptFactory promptFactory) {
        super(objectMapper, aiProperties.getTimeoutSeconds());
        this.aiProperties = aiProperties;
        this.promptFactory = promptFactory;
    }

    @Override
    public String getName() {
        return "gemini";
    }

    @Override
    public boolean isConfigured() {
        return !aiProperties.getGemini().getApiKey().isBlank();
    }

    @Override
    public Optional<CvAnalysisResult> analyzeCv(CvAnalysisRequest request) {
        try {
            log.info("Gemini CV analysis request using model '{}'", aiProperties.getGemini().getModel());
            JsonNode response = postJson(
                    buildEndpoint(),
                    buildRequestBody(promptFactory.buildCvSystemPrompt(), promptFactory.buildCvUserPrompt(request)),
                    new HttpHeaders());

            return Optional.of(readJsonPayload(extractText(response), CvAnalysisResult.class));
        } catch (Exception ex) {
            log.warn("Gemini CV analysis failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<RoadmapGenerationResult> generateRoadmap(RoadmapGenerationRequest request) {
        try {
            log.info("Gemini roadmap generation request using model '{}'", aiProperties.getGemini().getModel());
            JsonNode response = postJson(
                    buildEndpoint(),
                    buildRequestBody(promptFactory.buildRoadmapSystemPrompt(), promptFactory.buildRoadmapUserPrompt(request)),
                    new HttpHeaders());

            return Optional.of(readJsonPayload(extractText(response), RoadmapGenerationResult.class));
        } catch (Exception ex) {
            log.warn("Gemini roadmap generation failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String buildEndpoint() {
        return aiProperties.getGemini().getBaseUrl()
                + "/models/" + aiProperties.getGemini().getModel()
                + ":generateContent?key=" + aiProperties.getGemini().getApiKey();
    }

    private JsonNode buildRequestBody(String systemPrompt, String userPrompt) {
        return getObjectMapperTree(java.util.Map.of(
                "system_instruction", java.util.Map.of(
                        "parts", java.util.List.of(java.util.Map.of("text", systemPrompt))),
                "contents", java.util.List.of(
                        java.util.Map.of(
                                "role", "user",
                                "parts", java.util.List.of(java.util.Map.of("text", userPrompt)))),
                "generationConfig", java.util.Map.of(
                        "temperature", aiProperties.getGemini().getTemperature(),
                        "responseMimeType", "application/json")));
    }

    private String extractText(JsonNode response) {
        JsonNode candidates = response.path("candidates");
        for (JsonNode candidate : candidates) {
            for (JsonNode part : candidate.path("content").path("parts")) {
                JsonNode textNode = part.path("text");
                if (textNode.isTextual()) {
                    return textNode.asText();
                }
            }
        }

        throw new IllegalStateException("Gemini response did not contain text content");
    }
}
