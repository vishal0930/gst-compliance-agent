package com.gstcompliance.controller;

import com.gstcompliance.dto.request.LoginRequest;
import com.gstcompliance.dto.response.ApiResponse;
import com.gstcompliance.model.User;
import com.gstcompliance.repository.UserRepository;
import com.gstcompliance.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<ApiResponse<User>> register(@RequestBody User user) {
        // Check if user exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email already registered", "DUPLICATE_EMAIL"));
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // ✅ Save to database!
        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("User registered successfully", savedUser));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String token = tokenProvider.generateToken(authentication);

        Map<String, Object> response = Map.of(
                "token", token,
                "expiresIn", 86400000
        );

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Unauthorized", "UNAUTHORIZED"));
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(
                ApiResponse.success("User profile retrieved", user)
        );
    }
}