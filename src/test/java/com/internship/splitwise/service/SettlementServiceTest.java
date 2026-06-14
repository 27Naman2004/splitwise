package com.internship.splitwise.service;

import com.internship.splitwise.dto.SettlementRequest;
import com.internship.splitwise.dto.SettlementResponse;
import com.internship.splitwise.model.Group;
import com.internship.splitwise.model.Settlement;
import com.internship.splitwise.model.User;
import com.internship.splitwise.repository.GroupRepository;
import com.internship.splitwise.repository.SettlementRepository;
import com.internship.splitwise.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private SettlementService settlementService;

    private UUID groupId;
    private UUID user1Id;
    private UUID user2Id;
    private Group group;
    private User user1;
    private User user2;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        groupId = UUID.randomUUID();
        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();

        group = Group.builder().id(groupId).name("Roommates").build();
        user1 = User.builder().id(user1Id).name("Rohan").email("rohan@splitwise.local").build();
        user2 = User.builder().id(user2Id).name("Aisha").email("aisha@splitwise.local").build();
    }

    @Test
    public void testRecordSettlementSuccess() {
        SettlementRequest req = new SettlementRequest();
        req.setFromUserId(user1Id);
        req.setToUserId(user2Id);
        req.setAmount(BigDecimal.valueOf(500.00));
        req.setCurrency("INR");
        req.setSettlementDate(LocalDateTime.now());
        req.setNotes("Cash payoff");

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2Id)).thenReturn(Optional.of(user2));

        Settlement saved = Settlement.builder()
                .id(UUID.randomUUID())
                .group(group)
                .fromUser(user1)
                .toUser(user2)
                .amount(BigDecimal.valueOf(500.00))
                .currency("INR")
                .settlementDate(req.getSettlementDate())
                .notes("Cash payoff")
                .build();

        when(settlementRepository.save(any(Settlement.class))).thenReturn(saved);

        SettlementResponse res = settlementService.recordSettlement(groupId, req);

        assertNotNull(res);
        assertEquals(saved.getId(), res.getId());
        assertEquals("Rohan", res.getFromUser().getName());
        assertEquals("Aisha", res.getToUser().getName());
        assertEquals(BigDecimal.valueOf(500.00), res.getAmount());
        verify(settlementRepository, times(1)).save(any(Settlement.class));
    }

    @Test
    public void testRecordSettlementSameUserThrowsException() {
        SettlementRequest req = new SettlementRequest();
        req.setFromUserId(user1Id);
        req.setToUserId(user1Id);
        req.setAmount(BigDecimal.valueOf(500.00));
        req.setCurrency("INR");
        req.setSettlementDate(LocalDateTime.now());

        assertThrows(IllegalArgumentException.class, () -> {
            settlementService.recordSettlement(groupId, req);
        });

        verify(settlementRepository, never()).save(any(Settlement.class));
    }
}
