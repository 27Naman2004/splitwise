package com.internship.splitwise.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BalanceTraceEntry {
    private UUID id;
    private String type; // "EXPENSE" or "SETTLEMENT"
    private String description;
    private LocalDateTime date;
    private String currency;
    private BigDecimal totalAmount;
    private String role; // "PAYER", "PARTICIPANT", "BOTH", "SENDER", "RECEIVER"
    private BigDecimal paidAmount;
    private BigDecimal owedAmount;
    private BigDecimal netImpact; // In base currency (INR)
}
