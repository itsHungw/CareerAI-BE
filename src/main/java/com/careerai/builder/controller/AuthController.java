package com.careerai.builder.controller;

import com.careerai.builder.dto.ApiResponse;
import com.careerai.builder.dto.AuthResponse;
import com.careerai.builder.dto.LoginRequest;
import com.careerai.builder.dto.RegisterRequest;
import com.careerai.builder.service.AuthService;
import com.careerai.builder.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthResponse data = authService.register(request);
        cookieUtil.setRefreshTokenCookie(response, data.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Register successful", data));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse data = authService.login(request);
        cookieUtil.setRefreshTokenCookie(response, data.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Login successful", data));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@Valid @RequestBody com.careerai.builder.dto.GoogleLoginRequest request, HttpServletResponse response) {
        AuthResponse data = authService.loginWithGoogle(request);
        cookieUtil.setRefreshTokenCookie(response, data.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Google Login successful", data));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refresh(HttpServletRequest request) {
        String refreshToken = cookieUtil.extractRefreshToken(request);
        if (refreshToken == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Refresh token missing"));
        }
        
        String newAccessToken = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", newAccessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtil.extractRefreshToken(request);
        authService.logout(refreshToken);
        cookieUtil.clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }
}
