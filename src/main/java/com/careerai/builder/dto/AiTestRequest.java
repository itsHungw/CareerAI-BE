package com.careerai.builder.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiTestRequest {

    @Size(max = 4000, message = "sampleText must be at most 4000 characters")
    private String sampleText;
}
