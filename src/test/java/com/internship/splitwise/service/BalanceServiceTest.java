package com.internship.splitwise.service;

import com.internship.splitwise.dto.*;
import com.internship.splitwise.model.*;
import com.internship.splitwise.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BalanceServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private ExpenseSplitRepository expenseSplitRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private ExchangeRateRepository exchangeRateRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private BalanceService balanceService;

    private UUID groupId;
    private UUID aishaId;
    private UUID rohanId;
    private UUID priyaId;

    private User aisha;
    private User rohan;
    private User priya;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        groupId = UUID.randomUUID();
        aishaId = UUID.randomUUID();
        rohanId = UUID.randomUUID();
        priyaId = UUID.randomUUID();

        aisha = User.builder().id(aishaId).name("Aisha").email("aisha@splitwise.local").build();
        rohan = User.builder().id(rohanId).name("Rohan").email("rohan@splitwise.local").build();
        priya = User.builder().id(priyaId).name("Priya").email("priya@splitwise.local").build();
    }

    @Test
    public void testCalculateGroupBalancesNoSettlements() {
        // Setup standard group list
        when(userRepository.findActiveMembersByGroupId(groupId)).thenReturn(Arrays.asList(aisha, rohan, priya));

        // Expense: Aisha paid ₹1200 split equally among Aisha, Rohan, Priya (₹400 each)
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .description("Rent")
                .amount(BigDecimal.valueOf(1200.00))
                .currency("INR")
                .paidBy(aisha)
                .isSettlement(false)
                .build();

        when(expenseRepository.findByGroupId(groupId)).thenReturn(Collections.singletonList(expense));

        ExpenseSplit split1 = ExpenseSplit.builder().user(aisha).calculatedAmount(BigDecimal.valueOf(400.00)).build();
        ExpenseSplit split2 = ExpenseSplit.builder().user(rohan).calculatedAmount(BigDecimal.valueOf(400.00)).build();
        ExpenseSplit split3 = ExpenseSplit.builder().user(priya).calculatedAmount(BigDecimal.valueOf(400.00)).build();

        when(expenseSplitRepository.findByExpenseId(expense.getId())).thenReturn(Arrays.asList(split1, split2, split3));
        when(settlementRepository.findByGroupId(groupId)).thenReturn(Collections.emptyList());

        BalanceResponse res = balanceService.calculateGroupBalances(groupId);

        assertNotNull(res);
        assertEquals(3, res.getBalances().size());

        // Find individual balances
        BalanceResponse.MemberBalance aishaBal = res.getBalances().stream().filter(b -> b.getUserId().equals(aishaId)).findFirst().get();
        BalanceResponse.MemberBalance rohanBal = res.getBalances().stream().filter(b -> b.getUserId().equals(rohanId)).findFirst().get();
        BalanceResponse.MemberBalance priyaBal = res.getBalances().stream().filter(b -> b.getUserId().equals(priyaId)).findFirst().get();

        assertEquals(BigDecimal.valueOf(1200.00).setScale(2), aishaBal.getTotalPaid());
        assertEquals(BigDecimal.valueOf(400.00).setScale(2), aishaBal.getTotalOwed());
        assertEquals(BigDecimal.valueOf(800.00).setScale(2), aishaBal.getNetBalance());

        assertEquals(BigDecimal.valueOf(0.00).setScale(2), rohanBal.getTotalPaid());
        assertEquals(BigDecimal.valueOf(400.00).setScale(2), rohanBal.getTotalOwed());
        assertEquals(BigDecimal.valueOf(-400.00).setScale(2), rohanBal.getNetBalance());

        assertEquals(BigDecimal.valueOf(0.00).setScale(2), priyaBal.getTotalPaid());
        assertEquals(BigDecimal.valueOf(400.00).setScale(2), priyaBal.getTotalOwed());
        assertEquals(BigDecimal.valueOf(-400.00).setScale(2), priyaBal.getNetBalance());

        // Verify sum-to-zero
        BigDecimal sum = res.getBalances().stream()
                .map(BalanceResponse.MemberBalance::getNetBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(BigDecimal.ZERO.setScale(2), sum);
    }

    @Test
    public void testCalculateGroupBalancesWithSettlements() {
        when(userRepository.findActiveMembersByGroupId(groupId)).thenReturn(Arrays.asList(aisha, rohan, priya));

        // Expense: Aisha paid ₹1200 split equally among Aisha, Rohan, Priya (₹400 each)
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .description("Rent")
                .amount(BigDecimal.valueOf(1200.00))
                .currency("INR")
                .paidBy(aisha)
                .isSettlement(false)
                .build();

        when(expenseRepository.findByGroupId(groupId)).thenReturn(Collections.singletonList(expense));

        ExpenseSplit split1 = ExpenseSplit.builder().user(aisha).calculatedAmount(BigDecimal.valueOf(400.00)).build();
        ExpenseSplit split2 = ExpenseSplit.builder().user(rohan).calculatedAmount(BigDecimal.valueOf(400.00)).build();
        ExpenseSplit split3 = ExpenseSplit.builder().user(priya).calculatedAmount(BigDecimal.valueOf(400.00)).build();

        when(expenseSplitRepository.findByExpenseId(expense.getId())).thenReturn(Arrays.asList(split1, split2, split3));

        // Dedicated Settlement: Rohan paid Aisha ₹400
        Settlement settlement = Settlement.builder()
                .id(UUID.randomUUID())
                .fromUser(rohan)
                .toUser(aisha)
                .amount(BigDecimal.valueOf(400.00))
                .currency("INR")
                .settlementDate(LocalDateTime.now())
                .build();

        when(settlementRepository.findByGroupId(groupId)).thenReturn(Collections.singletonList(settlement));

        BalanceResponse res = balanceService.calculateGroupBalances(groupId);

        BalanceResponse.MemberBalance aishaBal = res.getBalances().stream().filter(b -> b.getUserId().equals(aishaId)).findFirst().get();
        BalanceResponse.MemberBalance rohanBal = res.getBalances().stream().filter(b -> b.getUserId().equals(rohanId)).findFirst().get();
        BalanceResponse.MemberBalance priyaBal = res.getBalances().stream().filter(b -> b.getUserId().equals(priyaId)).findFirst().get();

        // Aisha totalPaid = 1200, totalOwed = 400 (expense split) + 400 (received settlement) = 800
        assertEquals(BigDecimal.valueOf(1200.00).setScale(2), aishaBal.getTotalPaid());
        assertEquals(BigDecimal.valueOf(800.00).setScale(2), aishaBal.getTotalOwed());
        assertEquals(BigDecimal.valueOf(400.00).setScale(2), aishaBal.getNetBalance());

        // Rohan totalPaid = 400 (sent settlement), totalOwed = 400 (expense split)
        assertEquals(BigDecimal.valueOf(400.00).setScale(2), rohanBal.getTotalPaid());
        assertEquals(BigDecimal.valueOf(400.00).setScale(2), rohanBal.getTotalOwed());
        assertEquals(BigDecimal.valueOf(0.00).setScale(2), rohanBal.getNetBalance());

        // Priya remains unchanged
        assertEquals(BigDecimal.valueOf(-400.00).setScale(2), priyaBal.getNetBalance());
    }

    @Test
    public void testCalculateSimplifiedDebts() {
        when(userRepository.findActiveMembersByGroupId(groupId)).thenReturn(Arrays.asList(aisha, rohan, priya));

        // Expense: Aisha paid ₹1200 split equally among Aisha, Rohan, Priya
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .description("Rent")
                .amount(BigDecimal.valueOf(1200.00))
                .currency("INR")
                .paidBy(aisha)
                .isSettlement(false)
                .build();

        when(expenseRepository.findByGroupId(groupId)).thenReturn(Collections.singletonList(expense));

        ExpenseSplit split1 = ExpenseSplit.builder().user(aisha).calculatedAmount(BigDecimal.valueOf(400.00)).build();
        ExpenseSplit split2 = ExpenseSplit.builder().user(rohan).calculatedAmount(BigDecimal.valueOf(400.00)).build();
        ExpenseSplit split3 = ExpenseSplit.builder().user(priya).calculatedAmount(BigDecimal.valueOf(400.00)).build();

        when(expenseSplitRepository.findByExpenseId(expense.getId())).thenReturn(Arrays.asList(split1, split2, split3));
        when(settlementRepository.findByGroupId(groupId)).thenReturn(Collections.emptyList());

        List<SimplifiedDebtResponse> debts = balanceService.calculateSimplifiedDebts(groupId);

        // Expect two debts: Rohan pays Aisha ₹400, Priya pays Aisha ₹400
        assertEquals(2, debts.size());
        
        SimplifiedDebtResponse debt1 = debts.stream().filter(d -> d.getFromUserName().equals("Rohan")).findFirst().get();
        assertEquals("Aisha", debt1.getToUserName());
        assertEquals(BigDecimal.valueOf(400.00).setScale(2), debt1.getAmount());

        SimplifiedDebtResponse debt2 = debts.stream().filter(d -> d.getFromUserName().equals("Priya")).findFirst().get();
        assertEquals("Aisha", debt2.getToUserName());
        assertEquals(BigDecimal.valueOf(400.00).setScale(2), debt2.getAmount());
    }

    @Test
    public void testCalculateMemberTrace() {
        when(userRepository.findById(rohanId)).thenReturn(Optional.of(rohan));

        // Expense: Aisha paid ₹1200 split equally among Aisha, Rohan, Priya (Rohan owes ₹400)
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .description("Rent")
                .amount(BigDecimal.valueOf(1200.00))
                .currency("INR")
                .paidBy(aisha)
                .isSettlement(false)
                .expenseDate(LocalDateTime.now().minusDays(1))
                .build();

        when(expenseRepository.findByGroupId(groupId)).thenReturn(Collections.singletonList(expense));

        ExpenseSplit split = ExpenseSplit.builder().user(rohan).calculatedAmount(BigDecimal.valueOf(400.00)).build();
        when(expenseSplitRepository.findByExpenseId(expense.getId())).thenReturn(Collections.singletonList(split));

        // Settlement: Rohan paid Aisha ₹400
        Settlement settlement = Settlement.builder()
                .id(UUID.randomUUID())
                .fromUser(rohan)
                .toUser(aisha)
                .amount(BigDecimal.valueOf(400.00))
                .currency("INR")
                .settlementDate(LocalDateTime.now())
                .notes("Settled rent")
                .build();

        when(settlementRepository.findByGroupId(groupId)).thenReturn(Collections.singletonList(settlement));

        MemberBalanceTraceResponse traceResponse = balanceService.calculateMemberTrace(groupId, rohanId);

        assertNotNull(traceResponse);
        assertEquals("Rohan", traceResponse.getUserName());
        assertEquals(BigDecimal.valueOf(0.00).setScale(2), traceResponse.getNetBalance());
        assertEquals(2, traceResponse.getTraces().size());

        // First trace (Expense)
        BalanceTraceEntry first = traceResponse.getTraces().get(0);
        assertEquals("Rent", first.getDescription());
        assertEquals("PARTICIPANT", first.getRole());
        assertEquals(BigDecimal.valueOf(0.00).setScale(2), first.getPaidAmount());
        assertEquals(BigDecimal.valueOf(400.00).setScale(2), first.getOwedAmount());
        assertEquals(BigDecimal.valueOf(-400.00).setScale(2), first.getNetImpact());

        // Second trace (Settlement)
        BalanceTraceEntry second = traceResponse.getTraces().get(1);
        assertEquals("Settled rent", second.getDescription());
        assertEquals("SENDER", second.getRole());
        assertEquals(BigDecimal.valueOf(400.00).setScale(2), second.getPaidAmount());
        assertEquals(BigDecimal.valueOf(0.00).setScale(2), second.getOwedAmount());
        assertEquals(BigDecimal.valueOf(400.00).setScale(2), second.getNetImpact());
    }
}
