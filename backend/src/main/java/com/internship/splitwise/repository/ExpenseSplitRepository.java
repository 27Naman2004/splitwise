package com.internship.splitwise.repository;

import com.internship.splitwise.model.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, UUID> {
    List<ExpenseSplit> findByExpenseId(UUID expenseId);
    List<ExpenseSplit> findByUserId(UUID userId);
}
