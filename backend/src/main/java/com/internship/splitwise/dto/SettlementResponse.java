package com.internship.splitwise.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SettlementResponse {
    private UUID id;
    private UserInfo fromUser;
    private UserInfo toUser;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime settlementDate;
    private String notes;

    @Data
    @Builder
    public static class UserInfo {
        private UUID id;
        private String name;
    }
}
