package com.internship.splitwise.controller;

import com.internship.splitwise.dto.*;
import com.internship.splitwise.model.User;
import com.internship.splitwise.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody UserRegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isEmpty()) {
            User newUser = User.builder()
                    .name(request.getName().trim())
                    .email(email)
                    .password(request.getPassword()) // Storing plaintext password for mock testing simplicity
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(newUser);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // Fail-safe for development: auto-create the user if they don't exist
                    String name = email.split("@")[0];
                    name = name.substring(0, 1).toUpperCase() + name.substring(1);
                    User newUser = User.builder()
                            .name(name)
                            .email(email)
                            .password(request.getPassword())
                            .createdAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(newUser);
                });

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();

        LoginResponse response = LoginResponse.builder()
                .token("mock-jwt-token-for-development")
                .tokenType("Bearer")
                .user(userResponse)
                .build();

        return ResponseEntity.ok(response);
    }
}
