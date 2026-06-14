package com.internship.splitwise.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ExpenseSplitResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private BigDecimal splitValue;
    private BigDecimal calculatedAmount;
}
