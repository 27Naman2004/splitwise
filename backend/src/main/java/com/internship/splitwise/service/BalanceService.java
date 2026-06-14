package com.internship.splitwise.service;

import com.internship.splitwise.dto.*;
import com.internship.splitwise.model.*;
import com.internship.splitwise.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final UserRepository userRepository;
    private final SettlementRepository settlementRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final GroupMemberRepository groupMemberRepository;

    /**
     * Helper to collect all users who have active memberships or transaction presence in a group.
     */
    public Set<User> getGroupMembersHelper(UUID groupId) {
        Set<User> members = new LinkedHashSet<>(userRepository.findActiveMembersByGroupId(groupId));

        // Fallback/Union: Collect users from expenses and splits in this group
        List<Expense> expenses = expenseRepository.findByGroupId(groupId);
        for (Expense e : expenses) {
            members.add(e.getPaidBy());
            List<ExpenseSplit> splits = expenseSplitRepository.findByExpenseId(e.getId());
            for (ExpenseSplit s : splits) {
                members.add(s.getUser());
            }
        }

        // Include from settlements
        List<Settlement> settlements = settlementRepository.findByGroupId(groupId);
        for (Settlement s : settlements) {
            members.add(s.getFromUser());
            members.add(s.getToUser());
        }

        return members;
    }

    /**
     * Converts a given amount in any currency to INR base currency dynamically.
     */
    public BigDecimal convertToInr(BigDecimal amount, String fromCurrency) {
        if (amount == null) return BigDecimal.ZERO;
        if ("INR".equalsIgnoreCase(fromCurrency)) {
            return amount.setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal rate = BigDecimal.ONE;
        if ("USD".equalsIgnoreCase(fromCurrency)) {
            rate = exchangeRateRepository.findByFromCurrencyAndToCurrency("USD", "INR")
                    .map(ExchangeRate::getRate)
                    .orElse(BigDecimal.valueOf(83.00));
        } else {
            rate = exchangeRateRepository.findByFromCurrencyAndToCurrency(fromCurrency.toUpperCase(), "INR")
                    .map(ExchangeRate::getRate)
                    .orElse(BigDecimal.ONE);
        }

        return amount.multiply(rate).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the group balances: total paid, total owed, and net balances for all members in INR.
     */
    @Transactional(readOnly = true)
    public BalanceResponse calculateGroupBalances(UUID groupId) {
        Set<User> members = getGroupMembersHelper(groupId);
        
        Map<UUID, BalanceResponse.MemberBalance> balanceMap = new HashMap<>();
        for (User u : members) {
            balanceMap.put(u.getId(), BalanceResponse.MemberBalance.builder()
                    .userId(u.getId())
                    .name(u.getName())
                    .email(u.getEmail())
                    .totalPaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .totalOwed(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .netBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        // 1. Process Standard Expenses and splits (including imported settlements)
        List<Expense> expenses = expenseRepository.findByGroupId(groupId);
        for (Expense expense : expenses) {
            BigDecimal amountInInr = convertToInr(expense.getAmount(), expense.getCurrency());
            
            // Add to payer's total paid
            BalanceResponse.MemberBalance payerBalance = balanceMap.get(expense.getPaidBy().getId());
            if (payerBalance != null) {
                payerBalance.setTotalPaid(payerBalance.getTotalPaid().add(amountInInr));
            }

            // Process split allocations
            List<ExpenseSplit> splits = expenseSplitRepository.findByExpenseId(expense.getId());
            for (ExpenseSplit split : splits) {
                BigDecimal splitAmountInInr = convertToInr(split.getCalculatedAmount(), expense.getCurrency());
                BalanceResponse.MemberBalance participantBalance = balanceMap.get(split.getUser().getId());
                if (participantBalance != null) {
                    participantBalance.setTotalOwed(participantBalance.getTotalOwed().add(splitAmountInInr));
                }
            }
        }

        // 2. Process Dedicated Settlements (from settlements table)
        List<Settlement> settlements = settlementRepository.findByGroupId(groupId);
        for (Settlement settlement : settlements) {
            BigDecimal amountInInr = convertToInr(settlement.getAmount(), settlement.getCurrency());

            // Payer (sender) of settlement
            BalanceResponse.MemberBalance senderBalance = balanceMap.get(settlement.getFromUser().getId());
            if (senderBalance != null) {
                senderBalance.setTotalPaid(senderBalance.getTotalPaid().add(amountInInr));
            }

            // Recipient (receiver) of settlement
            BalanceResponse.MemberBalance receiverBalance = balanceMap.get(settlement.getToUser().getId());
            if (receiverBalance != null) {
                receiverBalance.setTotalOwed(receiverBalance.getTotalOwed().add(amountInInr));
            }
        }

        // 3. Finalize net balances
        List<BalanceResponse.MemberBalance> balancesList = new ArrayList<>();
        for (BalanceResponse.MemberBalance mb : balanceMap.values()) {
            mb.setTotalPaid(mb.getTotalPaid().setScale(2, RoundingMode.HALF_UP));
            mb.setTotalOwed(mb.getTotalOwed().setScale(2, RoundingMode.HALF_UP));
            mb.setNetBalance(mb.getTotalPaid().subtract(mb.getTotalOwed()).setScale(2, RoundingMode.HALF_UP));
            balancesList.add(mb);
        }

        return BalanceResponse.builder()
                .groupId(groupId)
                .baseCurrency("INR")
                .balances(balancesList)
                .build();
    }

    /**
     * Executes the Min-Max Greedy algorithm to simplify roommate debts.
     */
    @Transactional(readOnly = true)
    public List<SimplifiedDebtResponse> calculateSimplifiedDebts(UUID groupId) {
        BalanceResponse balanceResponse = calculateGroupBalances(groupId);

        // Partition users into Creditors (netBalance > 0) and Debtors (netBalance < 0)
        List<BalanceResponse.MemberBalance> creditors = balanceResponse.getBalances().stream()
                .filter(b -> b.getNetBalance().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        List<BalanceResponse.MemberBalance> debtors = balanceResponse.getBalances().stream()
                .filter(b -> b.getNetBalance().compareTo(BigDecimal.ZERO) < 0)
                .collect(Collectors.toList());

        List<SimplifiedDebtResponse> simplifiedDebts = new ArrayList<>();

        // Greedy loop
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            // Sort to retrieve top creditor and top debtor
            creditors.sort((c1, c2) -> c2.getNetBalance().compareTo(c1.getNetBalance()));
            debtors.sort((d1, d2) -> d1.getNetBalance().compareTo(d2.getNetBalance())); // Most negative first

            BalanceResponse.MemberBalance topCreditor = creditors.get(0);
            BalanceResponse.MemberBalance topDebtor = debtors.get(0);

            BigDecimal creditVal = topCreditor.getNetBalance();
            BigDecimal debtVal = topDebtor.getNetBalance().abs();

            BigDecimal settleAmount = creditVal.min(debtVal);

            // Record transaction
            simplifiedDebts.add(SimplifiedDebtResponse.builder()
                    .fromUserId(topDebtor.getUserId())
                    .fromUserName(topDebtor.getName())
                    .toUserId(topCreditor.getUserId())
                    .toUserName(topCreditor.getName())
                    .amount(settleAmount.setScale(2, RoundingMode.HALF_UP))
                    .currency("INR")
                    .build());

            // Update balances
            topCreditor.setNetBalance(creditVal.subtract(settleAmount));
            topDebtor.setNetBalance(topDebtor.getNetBalance().add(settleAmount));

            // Remove settled members
            if (topCreditor.getNetBalance().compareTo(BigDecimal.ZERO) == 0) {
                creditors.remove(0);
            }
            if (topDebtor.getNetBalance().compareTo(BigDecimal.ZERO) == 0) {
                debtors.remove(0);
            }
        }

        return simplifiedDebts;
    }

    /**
     * Generates a detailed itemized audit trail explaining exactly how the user's balance was created.
     */
    @Transactional(readOnly = true)
    public MemberBalanceTraceResponse calculateMemberTrace(UUID groupId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        List<BalanceTraceEntry> traces = new ArrayList<>();

        // 1. Scan standard expenses
        List<Expense> expenses = expenseRepository.findByGroupId(groupId);
        for (Expense expense : expenses) {
            boolean isPayer = expense.getPaidBy().getId().equals(userId);
            Optional<ExpenseSplit> userSplit = expenseSplitRepository.findByExpenseId(expense.getId()).stream()
                    .filter(s -> s.getUser().getId().equals(userId))
                    .findFirst();

            boolean isParticipant = userSplit.isPresent();

            if (isPayer || isParticipant) {
                BigDecimal paidAmount = isPayer ? expense.getAmount() : BigDecimal.ZERO;
                BigDecimal owedAmount = isParticipant ? userSplit.get().getCalculatedAmount() : BigDecimal.ZERO;

                BigDecimal paidInInr = convertToInr(paidAmount, expense.getCurrency());
                BigDecimal owedInInr = convertToInr(owedAmount, expense.getCurrency());
                BigDecimal netImpact = paidInInr.subtract(owedInInr);

                String role;
                if (expense.getIsSettlement() != null && expense.getIsSettlement()) {
                    role = isPayer ? "SENDER" : "RECEIVER";
                } else {
                    role = (isPayer && isParticipant) ? "BOTH" : (isPayer ? "PAYER" : "PARTICIPANT");
                }

                traces.add(BalanceTraceEntry.builder()
                        .id(expense.getId())
                        .type(expense.getIsSettlement() != null && expense.getIsSettlement() ? "SETTLEMENT" : "EXPENSE")
                        .description(expense.getDescription())
                        .date(expense.getExpenseDate())
                        .currency(expense.getCurrency())
                        .totalAmount(expense.getAmount().setScale(2, RoundingMode.HALF_UP))
                        .role(role)
                        .paidAmount(paidAmount.setScale(2, RoundingMode.HALF_UP))
                        .owedAmount(owedAmount.setScale(2, RoundingMode.HALF_UP))
                        .netImpact(netImpact.setScale(2, RoundingMode.HALF_UP))
                        .build());
            }
        }

        // 2. Scan dedicated settlements
        List<Settlement> settlements = settlementRepository.findByGroupId(groupId);
        for (Settlement settlement : settlements) {
            boolean isSender = settlement.getFromUser().getId().equals(userId);
            boolean isReceiver = settlement.getToUser().getId().equals(userId);

            if (isSender || isReceiver) {
                BigDecimal paidAmount = isSender ? settlement.getAmount() : BigDecimal.ZERO;
                BigDecimal owedAmount = isReceiver ? settlement.getAmount() : BigDecimal.ZERO;

                BigDecimal paidInInr = convertToInr(paidAmount, settlement.getCurrency());
                BigDecimal owedInInr = convertToInr(owedAmount, settlement.getCurrency());
                BigDecimal netImpact = paidInInr.subtract(owedInInr);

                traces.add(BalanceTraceEntry.builder()
                        .id(settlement.getId())
                        .type("SETTLEMENT")
                        .description(settlement.getNotes() != null && !settlement.getNotes().isEmpty() ? 
                                settlement.getNotes() : "Direct payment")
                        .date(settlement.getSettlementDate())
                        .currency(settlement.getCurrency())
                        .totalAmount(settlement.getAmount().setScale(2, RoundingMode.HALF_UP))
                        .role(isSender ? "SENDER" : "RECEIVER")
                        .paidAmount(paidAmount.setScale(2, RoundingMode.HALF_UP))
                        .owedAmount(owedAmount.setScale(2, RoundingMode.HALF_UP))
                        .netImpact(netImpact.setScale(2, RoundingMode.HALF_UP))
                        .build());
            }
        }

        // Sort chronologically
        traces.sort(Comparator.comparing(BalanceTraceEntry::getDate));

        BigDecimal netBalance = traces.stream()
                .map(BalanceTraceEntry::getNetImpact)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return MemberBalanceTraceResponse.builder()
                .userId(userId)
                .userName(user.getName())
                .netBalance(netBalance)
                .traces(traces)
                .build();
    }
}
