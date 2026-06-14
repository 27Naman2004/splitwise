package com.internship.splitwise.controller;

import com.internship.splitwise.dto.ExpenseRequest;
import com.internship.splitwise.dto.ExpenseResponse;
import com.internship.splitwise.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping("/expenses")
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse response = expenseService.createExpense(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/expenses/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable UUID id,
            @Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse response = expenseService.updateExpense(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable UUID id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/expenses/{id}")
    public ResponseEntity<ExpenseResponse> getExpense(@PathVariable UUID id) {
        ExpenseResponse response = expenseService.getExpense(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/groups/{groupId}/expenses")
    public ResponseEntity<List<ExpenseResponse>> getGroupExpenses(@PathVariable UUID groupId) {
        List<ExpenseResponse> response = expenseService.getGroupExpenses(groupId);
        return ResponseEntity.ok(response);
    }
}
