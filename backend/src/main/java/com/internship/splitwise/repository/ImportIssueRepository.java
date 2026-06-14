package com.internship.splitwise.repository;

import com.internship.splitwise.model.ImportIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImportIssueRepository extends JpaRepository<ImportIssue, UUID> {
    List<ImportIssue> findByImportJobId(UUID importJobId);

    @Query("SELECT COUNT(i) FROM ImportIssue i WHERE i.importJob.id = :jobId AND i.actionTaken = 'NONE' AND i.severity = 'CRITICAL'")
    long countUnresolvedCriticalIssues(@Param("jobId") UUID jobId);
}
