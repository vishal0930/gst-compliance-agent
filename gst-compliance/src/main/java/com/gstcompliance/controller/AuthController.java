package com.gstcompliance.controller;

import com.gstcompliance.dto.request.LoginRequest;
import com.gstcompliance.dto.response.ApiResponse;
import com.gstcompliance.model.User;
import com.gstcompliance.security.JwtTokenProvider;
import com.gstcompliance.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@RequestBody User user) {
        try {
            User saved = authService.register(user);
            return ResponseEntity.ok(ApiResponse.success("User registered successfully", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "DUPLICATE_EMAIL"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody LoginRequest request) {
        Authentication authentication = authService.authenticate(request.getEmail(), request.getPassword());
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

        User user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved", user));
    }
}
