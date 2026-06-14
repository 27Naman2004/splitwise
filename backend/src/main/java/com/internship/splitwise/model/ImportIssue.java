package com.internship.splitwise.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "import_issues")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_job_id", nullable = false)
    private ImportJob importJob;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "anomaly_type", nullable = false)
    private String anomalyType; // e.g., "DUPLICATE_EXPENSE", "MISSING_FIELD", etc.

    @Column(nullable = false)
    private String severity; // "CRITICAL", "WARNING", "INFO"

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "original_data", nullable = false, columnDefinition = "TEXT")
    private String originalData; // Raw CSV row as a string or JSON representation

    @Column(name = "action_taken", nullable = false)
    @Builder.Default
    private String actionTaken = "NONE"; // e.g., "NONE", "MAPPED", "NORMALIZED", "RESOLVED_PAYER", "RESOLVED_CURRENCY", "EXCLUDED"
}
