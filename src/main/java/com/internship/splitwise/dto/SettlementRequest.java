package com.internship.splitwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SettlementRequest {

    @NotNull(message = "From user ID is required")
    private UUID fromUserId;

    @NotNull(message = "To user ID is required")
    private UUID toUserId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency; // "INR" or "USD"

    @NotNull(message = "Settlement date is required")
    private LocalDateTime settlementDate;

    private String notes;
}
