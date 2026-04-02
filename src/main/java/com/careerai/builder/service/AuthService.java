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
import com.careerai.builder.dto.GoogleLoginRequest;
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

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    @Value("${spring.security.oauth2.client.registration.google.client-id:YOUR_GOOGLE_CLIENT_ID}")
    private String googleClientId;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email is already in use", HttpStatus.BAD_REQUEST);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER) // Role mặc định
                .build();

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return new AuthResponse(token, new AuthResponse.UserData(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        ));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return new AuthResponse(token, new AuthResponse.UserData(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        ));
    }

    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                
                // Check if user exists
                User user = userRepository.findByEmail(email).orElse(null);
                
                if (user == null) {
                    // Create new user for google
                    user = User.builder()
                            .email(email)
                            .passwordHash(null) // Không có mật khẩu vì login bằng Google
                            .provider(AuthProvider.GOOGLE)
                            .role(Role.USER)
                            .build();
                    userRepository.save(user);
                } else if (user.getProvider() != AuthProvider.GOOGLE) {
                    throw new ApiException("Email is mapped to a local account. Please login with password.", HttpStatus.BAD_REQUEST);
                }
                
                // Return internal JWT
                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                String internalToken = jwtUtil.generateToken(userDetails);
                
                return new AuthResponse(internalToken, new AuthResponse.UserData(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name()
                ));
            } else {
                throw new ApiException("Invalid Google ID Token", HttpStatus.UNAUTHORIZED);
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Failed to authenticate with Google: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
