package com.internship.splitwise.service;

import com.internship.splitwise.dto.*;
import com.internship.splitwise.model.*;
import com.internship.splitwise.repository.*;
import com.internship.splitwise.service.split.SplitStrategy;
import com.internship.splitwise.service.split.SplitStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final SplitStrategyFactory splitStrategyFactory;

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + request.getGroupId()));
        User payer = userRepository.findById(request.getPaidBy())
                .orElseThrow(() -> new IllegalArgumentException("Payer not found with ID: " + request.getPaidBy()));

        // Resolve Strategy and Calculate Splits
        SplitStrategy strategy = splitStrategyFactory.getStrategy(request.getSplitType());
        Map<UUID, BigDecimal> calculatedSplits = strategy.calculate(request.getAmount(), request.getSplits());

        // Create and save Expense
        Expense expense = Expense.builder()
                .group(group)
                .paidBy(payer)
                .description(request.getDescription())
                .amount(request.getAmount())
                .currency(request.getCurrency() == null ? "INR" : request.getCurrency())
                .splitType(request.getSplitType())
                .isSettlement(request.getIsSettlement() != null ? request.getIsSettlement() : false)
                .expenseDate(request.getExpenseDate())
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .build();

        expense = expenseRepository.save(expense);

        // Save Splits
        List<ExpenseSplit> splitsList = new ArrayList<>();
        for (ExpenseSplitRequest splitReq : request.getSplits()) {
            User participant = userRepository.findById(splitReq.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Participant not found with ID: " + splitReq.getUserId()));
            BigDecimal calculatedAmount = calculatedSplits.get(splitReq.getUserId());
            if (calculatedAmount == null) {
                throw new IllegalStateException("Split calculation missing for user ID: " + splitReq.getUserId());
            }

            ExpenseSplit split = ExpenseSplit.builder()
                    .expense(expense)
                    .user(participant)
                    .splitValue(splitReq.getSplitValue())
                    .calculatedAmount(calculatedAmount)
                    .build();
            splitsList.add(expenseSplitRepository.save(split));
        }

        return mapToResponse(expense, splitsList);
    }

    @Transactional
    public ExpenseResponse updateExpense(UUID id, ExpenseRequest request) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found with ID: " + id));
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + request.getGroupId()));
        User payer = userRepository.findById(request.getPaidBy())
                .orElseThrow(() -> new IllegalArgumentException("Payer not found with ID: " + request.getPaidBy()));

        // Delete existing splits
        List<ExpenseSplit> oldSplits = expenseSplitRepository.findByExpenseId(id);
        expenseSplitRepository.deleteAll(oldSplits);

        // Resolve Strategy and Recalculate
        SplitStrategy strategy = splitStrategyFactory.getStrategy(request.getSplitType());
        Map<UUID, BigDecimal> calculatedSplits = strategy.calculate(request.getAmount(), request.getSplits());

        // Update properties
        expense.setGroup(group);
        expense.setPaidBy(payer);
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setCurrency(request.getCurrency() == null ? "INR" : request.getCurrency());
        expense.setSplitType(request.getSplitType());
        expense.setIsSettlement(request.getIsSettlement() != null ? request.getIsSettlement() : false);
        expense.setExpenseDate(request.getExpenseDate());
        expense.setNotes(request.getNotes());

        expense = expenseRepository.save(expense);

        // Re-save Splits
        List<ExpenseSplit> splitsList = new ArrayList<>();
        for (ExpenseSplitRequest splitReq : request.getSplits()) {
            User participant = userRepository.findById(splitReq.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Participant not found with ID: " + splitReq.getUserId()));
            BigDecimal calculatedAmount = calculatedSplits.get(splitReq.getUserId());
            if (calculatedAmount == null) {
                throw new IllegalStateException("Split calculation missing for user ID: " + splitReq.getUserId());
            }

            ExpenseSplit split = ExpenseSplit.builder()
                    .expense(expense)
                    .user(participant)
                    .splitValue(splitReq.getSplitValue())
                    .calculatedAmount(calculatedAmount)
                    .build();
            splitsList.add(expenseSplitRepository.save(split));
        }

        return mapToResponse(expense, splitsList);
    }

    @Transactional
    public void deleteExpense(UUID id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found with ID: " + id));
        List<ExpenseSplit> splits = expenseSplitRepository.findByExpenseId(id);
        expenseSplitRepository.deleteAll(splits);
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(UUID id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found with ID: " + id));
        List<ExpenseSplit> splits = expenseSplitRepository.findByExpenseId(id);
        return mapToResponse(expense, splits);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getGroupExpenses(UUID groupId) {
        List<Expense> expenses = expenseRepository.findByGroupId(groupId);
        return expenses.stream()
                .map(e -> {
                    List<ExpenseSplit> splits = expenseSplitRepository.findByExpenseId(e.getId());
                    return mapToResponse(e, splits);
                })
                .collect(Collectors.toList());
    }

    private ExpenseResponse mapToResponse(Expense expense, List<ExpenseSplit> splits) {
        UserResponse paidByResponse = UserResponse.builder()
                .id(expense.getPaidBy().getId())
                .name(expense.getPaidBy().getName())
                .email(expense.getPaidBy().getEmail())
                .createdAt(expense.getPaidBy().getCreatedAt())
                .build();

        List<ExpenseSplitResponse> splitResponses = splits.stream()
                .map(s -> ExpenseSplitResponse.builder()
                        .id(s.getId())
                        .userId(s.getUser().getId())
                        .userName(s.getUser().getName())
                        .splitValue(s.getSplitValue())
                        .calculatedAmount(s.getCalculatedAmount())
                        .build())
                .collect(Collectors.toList());

        return ExpenseResponse.builder()
                .id(expense.getId())
                .groupId(expense.getGroup().getId())
                .paidBy(paidByResponse)
                .importBatchId(expense.getImportJob() != null ? expense.getImportJob().getId() : null)
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .currency(expense.getCurrency())
                .splitType(expense.getSplitType())
                .isSettlement(expense.getIsSettlement())
                .expenseDate(expense.getExpenseDate())
                .notes(expense.getNotes())
                .splits(splitResponses)
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
