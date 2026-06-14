package com.internship.splitwise.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MemberBalanceTraceResponse {
    private UUID userId;
    private String userName;
    private BigDecimal netBalance;
    private List<BalanceTraceEntry> traces;
}
