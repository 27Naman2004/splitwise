package com.internship.splitwise.dto;

import com.internship.splitwise.model.SplitType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ExpenseResponse {
    private UUID id;
    private UUID groupId;
    private UserResponse paidBy;
    private UUID importBatchId;
    private String description;
    private BigDecimal amount;
    private String currency;
    private SplitType splitType;
    private Boolean isSettlement;
    private LocalDateTime expenseDate;
    private String notes;
    private List<ExpenseSplitResponse> splits;
    private LocalDateTime createdAt;
}
