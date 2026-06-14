package com.internship.splitwise.controller;

import com.internship.splitwise.dto.AnomalyResolutionRequest;
import com.internship.splitwise.dto.ImportJobResponse;
import com.internship.splitwise.model.User;
import com.internship.splitwise.repository.UserRepository;
import com.internship.splitwise.service.ImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<ImportJobResponse> uploadCSV(
            @RequestParam("file") MultipartFile file,
            @RequestParam("groupId") UUID groupId) {
        
        // Fail-safe uploader mapping for mock/development environment:
        // Use the first user in the database, or create a mock system uploader if database is empty.
        User uploader = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User mockUser = User.builder()
                    .name("Aisha")
                    .email("aisha@example.com")
                    .password("password123")
                    .createdAt(LocalDateTime.now())
                    .build();
            return userRepository.save(mockUser);
        });

        try {
            ImportJobResponse response = importService.uploadAndStage(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    groupId,
                    uploader
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload and stage CSV file: " + e.getMessage(), e);
        }
    }

    @PostMapping("/{jobId}/resolve")
    public ResponseEntity<ImportJobResponse> resolveAnomalies(
            @PathVariable UUID jobId,
            @Valid @RequestBody AnomalyResolutionRequest request) {
        ImportJobResponse response = importService.resolveAndCommit(jobId, request);
        return ResponseEntity.ok(response);
    }
}
