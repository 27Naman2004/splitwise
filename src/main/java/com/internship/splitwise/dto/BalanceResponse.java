package com.internship.splitwise.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BalanceResponse {
    private UUID groupId;
    private String baseCurrency; // e.g. "INR"
    private List<MemberBalance> balances;

    @Data
    @Builder
    public static class MemberBalance {
        private UUID userId;
        private String name;
        private String email;
        private BigDecimal netBalance; // positive means they are owed money, negative means they owe money
    }
}
