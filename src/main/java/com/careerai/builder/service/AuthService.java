package com.careerai.builder.service;

import com.careerai.builder.config.JwtUtil;
import com.careerai.builder.domain.entity.Role;
import com.careerai.builder.domain.entity.User;
import com.careerai.builder.dto.AuthResponse;
import com.careerai.builder.dto.LoginRequest;
import com.careerai.builder.dto.RegisterRequest;
import com.careerai.builder.exception.ApiException;
import com.careerai.builder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.careerai.builder.domain.entity.AuthProvider;
import com.careerai.builder.domain.entity.RefreshToken;
import com.careerai.builder.dto.GoogleLoginRequest;
import com.careerai.builder.repository.RefreshTokenRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    @Value("${spring.security.oauth2.client.registration.google.client-id:YOUR_GOOGLE_CLIENT_ID}")
    private String googleClientId;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email is already in use", HttpStatus.BAD_REQUEST);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = createRefreshToken(user).getToken();

        return new AuthResponse(accessToken, refreshToken, new AuthResponse.UserData(
                user.getId(),
                user.getEmail(),
                user.getRole().name()));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = createRefreshToken(user).getToken();

        return new AuthResponse(accessToken, refreshToken, new AuthResponse.UserData(
                user.getId(),
                user.getEmail(),
                user.getRole().name()));
    }

    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                throw new ApiException("Invalid Google ID Token", HttpStatus.UNAUTHORIZED);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();

            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setProvider(AuthProvider.GOOGLE);
                user.setPasswordHash(null);
                user.setRole(Role.USER);
                userRepository.save(user);
            } else if (user.getProvider() != AuthProvider.GOOGLE) {
                throw new ApiException(
                        "Email is mapped to a local account. Please login with password.",
                        HttpStatus.BAD_REQUEST);
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            String accessToken = jwtUtil.generateToken(userDetails);
            String refreshToken = createRefreshToken(user).getToken();

            return new AuthResponse(accessToken, refreshToken, new AuthResponse.UserData(
                    user.getId(),
                    user.getEmail(),
                    user.getRole().name()));

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Failed to authenticate with Google: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String refreshAccessToken(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                    return jwtUtil.generateToken(userDetails);
                })
                .orElseThrow(() -> new ApiException("Refresh token is not in database!", HttpStatus.FORBIDDEN));
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenRepository.findByToken(refreshToken)
                    .ifPresent(refreshTokenRepository::delete);
        }
    }

    private RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(java.time.Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new ApiException("Refresh token was expired. Please make a new signin request", HttpStatus.FORBIDDEN);
        }
        return token;
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .expiryDate(java.time.Instant.now().plusMillis(refreshExpiration))
                .token(java.util.UUID.randomUUID().toString())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }
}
