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
    private String provider = "openai";
    private String fallbackProvider = "none";
    private int timeoutSeconds = 60;

    private OpenAiProperties openai = new OpenAiProperties();
    private GeminiProperties gemini = new GeminiProperties();

    @Getter
    @Setter
    public static class OpenAiProperties {
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-5-mini";
        private double temperature = 0.2d;
    }

    @Getter
    @Setter
    public static class GeminiProperties {
        private String apiKey = "";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        private String model = "gemini-2.5-flash";
        private double temperature = 0.2d;
    }
}
