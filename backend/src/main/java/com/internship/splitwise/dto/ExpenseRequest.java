package com.internship.splitwise.dto;

import com.internship.splitwise.model.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ExpenseRequest {

    @NotNull(message = "Group ID is required")
    private UUID groupId;

    @NotNull(message = "Payer User ID is required")
    private UUID paidBy;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency; // default: "INR"

    @NotNull(message = "Split type is required")
    private SplitType splitType;

    private Boolean isSettlement = false;

    @NotNull(message = "Expense date is required")
    private LocalDateTime expenseDate;

    private String notes;

    @Valid
    private List<ExpenseSplitRequest> splits;
}
