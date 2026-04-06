package com.careerai.builder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiStatusResponse {
    private boolean enabled;
    private String primaryProvider;
    private String fallbackProvider;
    private List<ProviderStatus> providers;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderStatus {
        private String name;
        private boolean configured;
    }
}
