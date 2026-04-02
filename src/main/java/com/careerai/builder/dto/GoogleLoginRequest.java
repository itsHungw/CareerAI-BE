package com.careerai.builder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleLoginRequest {
    @NotBlank(message = "Google ID Token is required")
    private String idToken;
}
