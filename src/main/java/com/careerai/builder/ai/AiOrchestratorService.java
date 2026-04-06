package com.careerai.builder.ai;

import com.careerai.builder.ai.config.AiProperties;
import com.careerai.builder.ai.model.CvAnalysisRequest;
import com.careerai.builder.ai.model.CvAnalysisResult;
import com.careerai.builder.ai.model.RoadmapGenerationRequest;
import com.careerai.builder.ai.model.RoadmapGenerationResult;
import com.careerai.builder.ai.provider.AiProvider;
import com.careerai.builder.dto.AiStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiOrchestratorService {

    private final List<AiProvider> providers;
    private final AiProperties aiProperties;

    public Optional<CvAnalysisResult> analyzeCv(CvAnalysisRequest request) {
        return execute(provider -> provider.analyzeCv(request));
    }

    public Optional<RoadmapGenerationResult> generateRoadmap(RoadmapGenerationRequest request) {
        return execute(provider -> provider.generateRoadmap(request));
    }

    public AiStatusResponse getStatus() {
        return AiStatusResponse.builder()
                .enabled(aiProperties.isEnabled())
                .primaryProvider(aiProperties.getProvider())
                .fallbackProvider(aiProperties.getFallbackProvider())
                .providers(providers.stream()
                        .map(provider -> AiStatusResponse.ProviderStatus.builder()
                                .name(provider.getName())
                                .configured(provider.isConfigured())
                                .build())
                        .toList())
                .build();
    }

    private <T> Optional<T> execute(Function<AiProvider, Optional<T>> action) {
        if (!aiProperties.isEnabled()) {
            log.info("AI execution skipped because ai.enabled=false");
            return Optional.empty();
        }

        Map<String, AiProvider> providerMap = providers.stream()
                .collect(Collectors.toMap(AiProvider::getName, Function.identity()));

        Optional<T> primaryResult = callProvider(providerMap.get(aiProperties.getProvider()), action, "primary");
        if (primaryResult.isPresent()) {
            return primaryResult;
        }

        if (!aiProperties.getFallbackProvider().isBlank() && !"none".equalsIgnoreCase(aiProperties.getFallbackProvider())) {
            Optional<T> fallbackResult = callProvider(providerMap.get(aiProperties.getFallbackProvider()), action, "fallback");
            if (fallbackResult.isPresent()) {
                return fallbackResult;
            }
        }

        return Optional.empty();
    }

    private <T> Optional<T> callProvider(AiProvider provider, Function<AiProvider, Optional<T>> action, String stage) {
        if (provider == null) {
            log.warn("AI {} provider was not found in the context", stage);
            return Optional.empty();
        }

        if (!provider.isConfigured()) {
            log.warn("AI {} provider '{}' is not configured", stage, provider.getName());
            return Optional.empty();
        }

        log.info("Calling AI {} provider '{}'", stage, provider.getName());
        return action.apply(provider);
    }
}
