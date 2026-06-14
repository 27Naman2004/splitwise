package com.internship.splitwise.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class ImportJobResponse {
    private UUID id;
    private UUID uploadedByUserId;
    private String fileName;
    private String status;
    private Map<String, Long> issuesCountSummary;
    private List<ImportIssueResponse> issues;
    private LocalDateTime createdAt;
}
