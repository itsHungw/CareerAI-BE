package com.careerai.builder.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private UserData user;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserData {
        private UUID id;
        private String email;
        private String role;
    }
}
