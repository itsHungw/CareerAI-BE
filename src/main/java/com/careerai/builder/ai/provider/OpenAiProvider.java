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
public class OpenAiProvider extends AbstractHttpAiProvider implements AiProvider {

    private final AiProperties aiProperties;
    private final AiPromptFactory promptFactory;

    public OpenAiProvider(ObjectMapper objectMapper, AiProperties aiProperties, AiPromptFactory promptFactory) {
        super(objectMapper, aiProperties.getTimeoutSeconds());
        this.aiProperties = aiProperties;
        this.promptFactory = promptFactory;
    }

    @Override
    public String getName() {
        return "openai";
    }

    @Override
    public boolean isConfigured() {
        return !aiProperties.getOpenai().getApiKey().isBlank();
    }

    @Override
    public Optional<CvAnalysisResult> analyzeCv(CvAnalysisRequest request) {
        try {
            log.info("OpenAI CV analysis request using model '{}'", aiProperties.getOpenai().getModel());
            JsonNode response = postJson(
                    aiProperties.getOpenai().getBaseUrl() + "/responses",
                    buildRequestBody(promptFactory.buildCvSystemPrompt(), promptFactory.buildCvUserPrompt(request)),
                    authHeaders(aiProperties.getOpenai().getApiKey()));

            return Optional.of(readJsonPayload(extractText(response), CvAnalysisResult.class));
        } catch (Exception ex) {
            log.warn("OpenAI CV analysis failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<RoadmapGenerationResult> generateRoadmap(RoadmapGenerationRequest request) {
        try {
            log.info("OpenAI roadmap generation request using model '{}'", aiProperties.getOpenai().getModel());
            JsonNode response = postJson(
                    aiProperties.getOpenai().getBaseUrl() + "/responses",
                    buildRequestBody(promptFactory.buildRoadmapSystemPrompt(), promptFactory.buildRoadmapUserPrompt(request)),
                    authHeaders(aiProperties.getOpenai().getApiKey()));

            return Optional.of(readJsonPayload(extractText(response), RoadmapGenerationResult.class));
        } catch (Exception ex) {
            log.warn("OpenAI roadmap generation failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private JsonNode buildRequestBody(String systemPrompt, String userPrompt) {
        return getObjectMapperTree(java.util.Map.of(
                "model", aiProperties.getOpenai().getModel(),
                "input", java.util.List.of(
                        java.util.Map.of(
                                "role", "system",
                                "content", java.util.List.of(java.util.Map.of("type", "input_text", "text", systemPrompt))),
                        java.util.Map.of(
                                "role", "user",
                                "content", java.util.List.of(java.util.Map.of("type", "input_text", "text", userPrompt)))),
                "text", java.util.Map.of("format", java.util.Map.of("type", "json_object"))));
    }

    private HttpHeaders authHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        return headers;
    }

    private String extractText(JsonNode response) {
        JsonNode outputText = response.path("output_text");
        if (outputText.isTextual()) {
            return outputText.asText();
        }

        JsonNode output = response.path("output");
        for (JsonNode outputItem : output) {
            for (JsonNode contentItem : outputItem.path("content")) {
                JsonNode textNode = contentItem.path("text");
                if (textNode.isTextual()) {
                    return textNode.asText();
                }
            }
        }

        throw new IllegalStateException("OpenAI response did not contain output text");
    }
}
