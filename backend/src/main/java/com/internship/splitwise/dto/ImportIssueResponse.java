package com.internship.splitwise.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ImportIssueResponse {
    private UUID id;
    private UUID importJobId;
    private Integer rowNumber;
    private String anomalyType;
    private String severity;
    private String description;
    private String originalData;
    private String actionTaken;
}
