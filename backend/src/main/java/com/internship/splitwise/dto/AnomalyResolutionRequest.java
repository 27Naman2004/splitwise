package com.internship.splitwise.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class AnomalyResolutionRequest {

    @NotNull(message = "Resolutions list cannot be null")
    @Valid
    private List<SingleResolution> resolutions;

    @Data
    public static class SingleResolution {

        @NotNull(message = "Anomaly ID is required")
        private UUID anomalyId;

        @NotBlank(message = "Resolution action is required")
        private String action; // e.g., "NORMALIZE_PERCENTAGES", "MERGE_ENTITY", "SET_PAYER", "SET_CURRENCY", "FORCE_ACCEPT", "EXCLUDE_ROW"

        private Map<String, Object> customData; // Resolution-specific inputs (e.g., custom name mappings or corrected percentages)
    }
}
