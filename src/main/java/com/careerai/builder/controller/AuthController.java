package com.careerai.builder.controller;

import com.careerai.builder.dto.ApiResponse;
import com.careerai.builder.dto.AuthResponse;
import com.careerai.builder.dto.LoginRequest;
import com.careerai.builder.dto.RegisterRequest;
import com.careerai.builder.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse data = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Register successful", data));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", data));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@Valid @RequestBody com.careerai.builder.dto.GoogleLoginRequest request) {
        AuthResponse data = authService.loginWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.success("Google Login successful", data));
    }
}
