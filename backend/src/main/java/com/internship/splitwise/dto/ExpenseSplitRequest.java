package com.internship.splitwise.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ExpenseSplitRequest {

    @NotNull(message = "User ID for the split is required")
    private UUID userId;

    @NotNull(message = "Split value is required")
    @Positive(message = "Split value must be greater than zero")
    private BigDecimal splitValue; // Can be a percentage (e.g. 30.00), shares (e.g. 2.00) or absolute amount (e.g. 500.00)
}
