package com.internship.splitwise.service;

import com.internship.splitwise.dto.*;
import com.internship.splitwise.model.*;
import com.internship.splitwise.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final CSVParser csvParser;
    private final ValidationService validationService;
    private final ImportJobRepository importJobRepository;
    private final ImportIssueRepository importIssueRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    /**
     * Ingests, parses, and audits the uploaded CSV file.
     */
    @Transactional
    public ImportJobResponse uploadAndStage(InputStream fileStream, String fileName, UUID groupId, User uploader) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + groupId));

        // Read file stream to string
        String fileContent;
        try {
            fileContent = new String(fileStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file contents", e);
        }

        // 1. Create a staged ImportJob
        ImportJob job = ImportJob.builder()
                .uploadedBy(uploader)
                .group(group)
                .fileName(fileName)
                .status("PROCESSING")
                .fileContent(fileContent)
                .createdAt(LocalDateTime.now())
                .build();
        job = importJobRepository.save(job);

        try {
            // 2. Parse CSV rows
            List<CSVRowDTO> parsedRows = csvParser.parse(new java.io.ByteArrayInputStream(fileContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

            // 3. Detect anomalies
            List<ImportIssue> issues = validationService.detectAnomalies(parsedRows, job);

            // 4. Determine status based on critical anomalies
            long criticalCount = issues.stream()
                    .filter(issue -> "CRITICAL".equalsIgnoreCase(issue.getSeverity()))
                    .count();

            if (criticalCount > 0) {
                job.setStatus("PENDING_RESOLUTION");
            } else {
                // If no critical anomalies, we can write the data directly to core expenses ledger
                commitStagedRows(parsedRows, group, job);
                job.setStatus("COMPLETED");
                // Mark non-critical issues as auto-accepted or noted
                issues.forEach(issue -> issue.setActionTaken("AUTO_ACCEPTED"));
            }

            // Save issues & update job status
            importJobRepository.save(job);
            List<ImportIssue> savedIssues = importIssueRepository.saveAll(issues);

            return mapToResponse(job, savedIssues);

        } catch (Exception e) {
            job.setStatus("FAILED");
            importJobRepository.save(job);
            throw new RuntimeException("Failed to process CSV import: " + e.getMessage(), e);
        }
    }

    @Transactional
    public ImportJobResponse resolveAndCommit(UUID jobId, AnomalyResolutionRequest request) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Import job not found with ID: " + jobId));

        if (!"PENDING_RESOLUTION".equals(job.getStatus())) {
            throw new IllegalStateException("Import job is not in PENDING_RESOLUTION state");
        }

        List<ImportIssue> issues = importIssueRepository.findByImportJobId(jobId);
        Map<UUID, AnomalyResolutionRequest.SingleResolution> resolutionMap = request.getResolutions().stream()
                .collect(Collectors.toMap(AnomalyResolutionRequest.SingleResolution::getAnomalyId, r -> r));

        // Update issue actions
        for (ImportIssue issue : issues) {
            AnomalyResolutionRequest.SingleResolution res = resolutionMap.get(issue.getId());
            if (res != null) {
                issue.setActionTaken(res.getAction());
                importIssueRepository.save(issue);
            }
        }

        // Re-parse the CSV rows
        List<CSVRowDTO> parsedRows;
        try {
            parsedRows = csvParser.parse(new java.io.ByteArrayInputStream(job.getFileContent().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to re-parse CSV rows", e);
        }

        // Filter and modify rows based on resolutions
        List<CSVRowDTO> finalRows = new ArrayList<>();
        Map<Integer, List<ImportIssue>> rowIssues = issues.stream()
                .collect(Collectors.groupingBy(ImportIssue::getRowNumber));

        for (CSVRowDTO row : parsedRows) {
            List<ImportIssue> rowAnomalies = rowIssues.getOrDefault(row.getRowNumber(), Collections.emptyList());
            
            boolean exclude = false;
            String payerOverride = null;
            String currencyOverride = null;

            for (ImportIssue issue : rowAnomalies) {
                AnomalyResolutionRequest.SingleResolution res = resolutionMap.get(issue.getId());
                if (res != null) {
                    if ("EXCLUDE".equalsIgnoreCase(res.getAction())) {
                        exclude = true;
                    } else if ("RESOLVE_PAYER".equalsIgnoreCase(res.getAction()) && res.getCustomData() != null) {
                        payerOverride = (String) res.getCustomData().get("paidBy");
                    } else if ("RESOLVE_CURRENCY".equalsIgnoreCase(res.getAction()) && res.getCustomData() != null) {
                        currencyOverride = (String) res.getCustomData().get("currency");
                    } else if ("MAP_USER".equalsIgnoreCase(res.getAction()) && res.getCustomData() != null) {
                        payerOverride = (String) res.getCustomData().get("mappedName");
                    }
                }
            }

            if (exclude) {
                continue;
            }

            if (payerOverride != null && !payerOverride.trim().isEmpty()) {
                row.setPaidBy(payerOverride);
            }
            if (currencyOverride != null && !currencyOverride.trim().isEmpty()) {
                row.setCurrency(currencyOverride);
            }

            finalRows.add(row);
        }

        // Commit remaining rows
        commitStagedRows(finalRows, job.getGroup(), job);

        job.setStatus("COMPLETED");
        importJobRepository.save(job);

        return mapToResponse(job, issues);
    }

    private void commitStagedRows(List<CSVRowDTO> rows, Group group, ImportJob job) {
        for (CSVRowDTO row : rows) {
            // Check for row exclusion or skipping (can be extended during user resolution phase)
            if (row.getAmount() == null || row.getAmount().isEmpty()) {
                continue;
            }

            // Parse Payer (map/resolve names or fallback)
            User payer = resolvePayer(row.getPaidBy().trim());
            
            // Parse Amount and round
            String cleanAmount = row.getAmount().replace("\"", "").replace(",", "").trim();
            BigDecimal amount = new BigDecimal(cleanAmount).setScale(2, RoundingMode.HALF_UP);
            
            // Detect Settlement vs Expense
            boolean isSettlement = isSettlementTransaction(row);

            // Parse Date
            LocalDateTime date = parseDateSafe(row.getDate());

            Expense expense = Expense.builder()
                    .group(group)
                    .paidBy(payer)
                    .importJob(job)
                    .description(row.getDescription().trim())
                    .amount(amount)
                    .currency(isEmpty(row.getCurrency()) ? "INR" : row.getCurrency().toUpperCase().trim())
                    .splitType(parseSplitType(row.getSplitType()))
                    .isSettlement(isSettlement)
                    .expenseDate(date)
                    .notes(row.getNotes())
                    .createdAt(LocalDateTime.now())
                    .build();

            expense = expenseRepository.save(expense);

            // Save splits
            saveExpenseSplits(row, expense, amount);
        }
    }

    private User resolvePayer(String rawName) {
        // Find existing user or create temporary user
        String name = rawName.trim();
        return userRepository.findByEmail(name.toLowerCase().replace(" ", "") + "@splitwise.local")
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .name(name)
                            .email(name.toLowerCase().replace(" ", "") + "@splitwise.local")
                            .password("tempPass123!")
                            .createdAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(newUser);
                });
    }

    private boolean isSettlementTransaction(CSVRowDTO row) {
        String desc = row.getDescription().toLowerCase();
        String notes = row.getNotes() != null ? row.getNotes().toLowerCase() : "";
        return desc.contains("paid back") || desc.contains("settlement") || desc.contains("repaid") ||
                notes.contains("settlement") || notes.contains("deposit");
    }

    private LocalDateTime parseDateSafe(String rawDate) {
        DateTimeFormatter dmyFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        try {
            return LocalDate.parse(rawDate.trim(), dmyFormatter).atStartOfDay();
        } catch (Exception e) {
            // Fallback for Month-DD (Mar-14) formats
            if (rawDate.matches("^[A-Za-z]{3}-\\d{2}$")) {
                String[] parts = rawDate.split("-");
                String month = parts[0];
                int day = Integer.parseInt(parts[1]);
                int monthVal = switch (month.toLowerCase()) {
                    case "jan" -> 1; case "feb" -> 2; case "mar" -> 3; case "apr" -> 4;
                    case "may" -> 5; case "jun" -> 6; case "jul" -> 7; case "aug" -> 8;
                    case "sep" -> 9; case "oct" -> 10; case "nov" -> 11; case "dec" -> 12;
                    default -> 1;
                };
                return LocalDate.of(2026, monthVal, day).atStartOfDay();
            }
            return LocalDate.now().atStartOfDay(); // Default safe boundary fallback
        }
    }

    private SplitType parseSplitType(String rawType) {
        if (rawType == null || rawType.isEmpty()) {
            return SplitType.EQUAL;
        }
        return switch (rawType.toLowerCase().trim()) {
            case "unequal" -> SplitType.UNEQUAL;
            case "percentage" -> SplitType.PERCENTAGE;
            case "share" -> SplitType.SHARE;
            default -> SplitType.EQUAL;
        };
    }

    private void saveExpenseSplits(CSVRowDTO row, Expense expense, BigDecimal totalAmount) {
        String splitWith = row.getSplitWith();
        if (splitWith == null || splitWith.isEmpty()) return;

        String[] participants = splitWith.split(";");
        List<User> users = Arrays.stream(participants)
                .map(String::trim)
                .map(this::resolvePayer)
                .collect(Collectors.toList());

        SplitType splitType = expense.getSplitType();
        int count = users.size();

        if (splitType == SplitType.EQUAL) {
            BigDecimal share = totalAmount.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
            for (User user : users) {
                ExpenseSplit split = ExpenseSplit.builder()
                        .expense(expense)
                        .user(user)
                        .splitValue(BigDecimal.valueOf(1)) // Weight is 1 for equal
                        .calculatedAmount(share)
                        .build();
                expenseSplitRepository.save(split);
            }
        } else if (splitType == SplitType.PERCENTAGE) {
            // Parse split details (e.g., Aisha 30%; Rohan 30%; Priya 30%; Meera 20% - note this sums to 110%)
            Map<String, BigDecimal> percentages = parseSplitDetails(row.getSplitDetails());
            
            // Normalize sum if needed (e.g. 110% -> scaled to 100%)
            BigDecimal sum = percentages.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean normalize = sum.compareTo(BigDecimal.valueOf(100.00)) != 0;

            for (User user : users) {
                BigDecimal pct = percentages.getOrDefault(user.getName(), BigDecimal.ZERO);
                if (normalize && sum.compareTo(BigDecimal.ZERO) > 0) {
                    // scale pct: scaled = (pct / sum) * 100
                    pct = pct.divide(sum, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                }
                BigDecimal calculated = totalAmount.multiply(pct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                
                ExpenseSplit split = ExpenseSplit.builder()
                        .expense(expense)
                        .user(user)
                        .splitValue(pct)
                        .calculatedAmount(calculated.setScale(2, RoundingMode.HALF_UP))
                        .build();
                expenseSplitRepository.save(split);
            }
        } else if (splitType == SplitType.SHARE || splitType == SplitType.UNEQUAL) {
            // Shares split details (e.g. Aisha 1; Rohan 2; Priya 1; Dev 2)
            Map<String, BigDecimal> shares = parseSplitDetails(row.getSplitDetails());
            BigDecimal totalShares = shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            
            for (User user : users) {
                BigDecimal sh = shares.getOrDefault(user.getName(), BigDecimal.valueOf(1));
                BigDecimal calculated;
                if (totalShares.compareTo(BigDecimal.ZERO) > 0) {
                    calculated = totalAmount.multiply(sh.divide(totalShares, 4, RoundingMode.HALF_UP));
                } else {
                    calculated = totalAmount.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
                }

                ExpenseSplit split = ExpenseSplit.builder()
                        .expense(expense)
                        .user(user)
                        .splitValue(sh)
                        .calculatedAmount(calculated.setScale(2, RoundingMode.HALF_UP))
                        .build();
                expenseSplitRepository.save(split);
            }
        }
    }

    private Map<String, BigDecimal> parseSplitDetails(String details) {
        Map<String, BigDecimal> map = new HashMap<>();
        if (details == null || details.trim().isEmpty()) return map;

        // details string format is e.g. "Rohan 700; Priya 400; Meera 400" or "Aisha 30%; Rohan 30%"
        String[] parts = details.split(";");
        for (String part : parts) {
            String pTrim = part.trim();
            if (pTrim.isEmpty()) continue;
            
            // Regex to extract name and number
            Pattern pattern = Pattern.compile("^(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*%?$");
            java.util.regex.Matcher matcher = pattern.matcher(pTrim);
            if (matcher.find()) {
                String name = matcher.group(1).trim();
                BigDecimal val = new BigDecimal(matcher.group(2));
                map.put(name, val);
            }
        }
        return map;
    }

    private ImportJobResponse mapToResponse(ImportJob job, List<ImportIssue> issues) {
        List<ImportIssueResponse> issuesResponse = issues.stream()
                .map(issue -> ImportIssueResponse.builder()
                        .id(issue.getId())
                        .importJobId(job.getId())
                        .rowNumber(issue.getRowNumber())
                        .anomalyType(issue.getAnomalyType())
                        .severity(issue.getSeverity())
                        .description(issue.getDescription())
                        .originalData(issue.getOriginalData())
                        .actionTaken(issue.getActionTaken())
                        .build())
                .collect(Collectors.toList());

        Map<String, Long> countMap = issues.stream()
                .collect(Collectors.groupingBy(ImportIssue::getSeverity, Collectors.counting()));

        return ImportJobResponse.builder()
                .id(job.getId())
                .uploadedByUserId(job.getUploadedBy() != null ? job.getUploadedBy().getId() : null)
                .fileName(job.getFileName())
                .status(job.getStatus())
                .issuesCountSummary(countMap)
                .issues(issuesResponse)
                .createdAt(job.getCreatedAt())
                .build();
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
