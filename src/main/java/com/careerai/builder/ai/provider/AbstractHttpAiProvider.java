package com.careerai.builder.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public abstract class AbstractHttpAiProvider {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final int timeoutSeconds;

    protected AbstractHttpAiProvider(ObjectMapper objectMapper, int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    protected JsonNode postJson(String url, JsonNode body, HttpHeaders headers) throws IOException, InterruptedException {

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8));

        headers.forEach((headerName, values) -> values.forEach(value -> requestBuilder.header(headerName, value)));

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("AI provider request failed with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readTree(response.body());
    }

    protected <T> T readJsonPayload(String rawText, Class<T> type) throws IOException {
        return objectMapper.readValue(extractJsonObject(rawText), type);
    }

    protected String extractJsonObject(String rawText) {
        int firstBrace = rawText.indexOf('{');
        int lastBrace = rawText.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return rawText.substring(firstBrace, lastBrace + 1);
        }
        return rawText;
    }

    protected JsonNode getObjectMapperTree(Object value) {
        return objectMapper.valueToTree(value);
    }
}
