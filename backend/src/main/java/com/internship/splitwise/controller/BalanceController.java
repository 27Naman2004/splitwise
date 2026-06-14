package com.internship.splitwise.controller;

import com.internship.splitwise.dto.*;
import com.internship.splitwise.model.User;
import com.internship.splitwise.service.BalanceService;
import com.internship.splitwise.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups/{groupId}")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;
    private final SettlementService settlementService;

    @GetMapping("/balances")
    public ResponseEntity<BalanceResponse> getGroupBalances(@PathVariable UUID groupId) {
        BalanceResponse response = balanceService.calculateGroupBalances(groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/simplified-debts")
    public ResponseEntity<List<SimplifiedDebtResponse>> getSimplifiedDebts(@PathVariable UUID groupId) {
        List<SimplifiedDebtResponse> response = balanceService.calculateSimplifiedDebts(groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/members/{userId}/trace")
    public ResponseEntity<MemberBalanceTraceResponse> getMemberBalanceTrace(
            @PathVariable UUID groupId,
            @PathVariable UUID userId) {
        MemberBalanceTraceResponse response = balanceService.calculateMemberTrace(groupId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/members")
    public ResponseEntity<List<UserResponse>> getGroupMembers(@PathVariable UUID groupId) {
        List<UserResponse> members = balanceService.getGroupMembersHelper(groupId).stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(members);
    }

    @PostMapping("/settlements")
    public ResponseEntity<SettlementResponse> createSettlement(
            @PathVariable UUID groupId,
            @Valid @RequestBody SettlementRequest request) {
        SettlementResponse response = settlementService.recordSettlement(groupId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/settlements")
    public ResponseEntity<List<SettlementResponse>> getSettlements(@PathVariable UUID groupId) {
        List<SettlementResponse> response = settlementService.getGroupSettlements(groupId);
        return ResponseEntity.ok(response);
    }
}
