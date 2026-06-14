package com.internship.splitwise.repository;

import com.internship.splitwise.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByGroupId(UUID groupId);

    @Query("SELECT e FROM Expense e WHERE e.group.id = :groupId AND e.isSettlement = false")
    List<Expense> findGroupExpensesOnly(@Param("groupId") UUID groupId);
}
