package com.internship.splitwise.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class SimplifiedDebtResponse {
    private UUID fromUserId;
    private String fromUserName;
    private UUID toUserId;
    private String toUserName;
    private BigDecimal amount;
    private String currency; // base currency of the group (e.g. INR)
}
