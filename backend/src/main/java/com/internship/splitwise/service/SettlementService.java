package com.internship.splitwise.service;

import com.internship.splitwise.dto.SettlementRequest;
import com.internship.splitwise.dto.SettlementResponse;
import com.internship.splitwise.model.Group;
import com.internship.splitwise.model.Settlement;
import com.internship.splitwise.model.User;
import com.internship.splitwise.repository.GroupRepository;
import com.internship.splitwise.repository.SettlementRepository;
import com.internship.splitwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Transactional
    public SettlementResponse recordSettlement(UUID groupId, SettlementRequest request) {
        if (request.getFromUserId().equals(request.getToUserId())) {
            throw new IllegalArgumentException("Debtor and Creditor cannot be the same user.");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + groupId));

        User fromUser = userRepository.findById(request.getFromUserId())
                .orElseThrow(() -> new IllegalArgumentException("Payer user not found with ID: " + request.getFromUserId()));

        User toUser = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient user not found with ID: " + request.getToUserId()));

        Settlement settlement = Settlement.builder()
                .group(group)
                .fromUser(fromUser)
                .toUser(toUser)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .settlementDate(request.getSettlementDate())
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .build();

        settlement = settlementRepository.save(settlement);
        return mapToResponse(settlement);
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> getGroupSettlements(UUID groupId) {
        List<Settlement> settlements = settlementRepository.findByGroupId(groupId);
        return settlements.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private SettlementResponse mapToResponse(Settlement settlement) {
        return SettlementResponse.builder()
                .id(settlement.getId())
                .fromUser(SettlementResponse.UserInfo.builder()
                        .id(settlement.getFromUser().getId())
                        .name(settlement.getFromUser().getName())
                        .build())
                .toUser(SettlementResponse.UserInfo.builder()
                        .id(settlement.getToUser().getId())
                        .name(settlement.getToUser().getName())
                        .build())
                .amount(settlement.getAmount())
                .currency(settlement.getCurrency())
                .settlementDate(settlement.getSettlementDate())
                .notes(settlement.getNotes())
                .build();
    }
}
