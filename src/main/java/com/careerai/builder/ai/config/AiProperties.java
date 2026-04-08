package com.careerai.builder.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai")
@Getter
@Setter
public class AiProperties {

    private boolean enabled = false;
    private String provider = "fpt";
    private String fallbackProvider = "none";
    private int timeoutSeconds = 60;

    private FptProperties fpt = new FptProperties();

    @Getter
    @Setter
    public static class FptProperties {
        private String apiKey = "";
        private String baseUrl = "https://mkp-api.fptcloud.com";
        private String model = "Qwen2.5-7B-instruct";
        private String embeddingModel = "Vietnamese_Embedding";
        private double temperature = 0.2d;
    }
}
