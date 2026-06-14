package com.internship.splitwise.service;

import com.internship.splitwise.dto.CSVRowDTO;
import com.internship.splitwise.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ValidationService {

    private static final List<String> CANONICAL_MEMBERS = List.of("Aisha", "Rohan", "Priya", "Meera", "Sam", "Dev");
    private static final LocalDate MEERA_EXIT_DATE = LocalDate.of(2026, 3, 29);
    private static final LocalDate SAM_ENTRY_DATE = LocalDate.of(2026, 4, 8);

    /**
     * Runs anomaly detection on a list of raw parsed CSV lines.
     */
    public List<ImportIssue> detectAnomalies(List<CSVRowDTO> rows, ImportJob job) {
        List<ImportIssue> issues = new ArrayList<>();
        Set<String> uniqueTrans = new HashSet<>();

        for (CSVRowDTO row : rows) {
            // 1. Check for missing critical fields
            checkMissingFields(row, issues, job);

            // 2. Validate Amount
            checkAmountAnomalies(row, issues, job);

            // 3. Validate Date
            LocalDate parsedDate = checkDateAnomalies(row, issues, job);

            // 4. Validate Payer Entity Name Normalization
            checkPayerAnomalies(row, issues, job);

            // 5. Validate Split ratios and Inactive Members
            checkSplitAnomalies(row, parsedDate, issues, job);

            // 6. Identify Peer settlements
            checkSettlementAnomalies(row, issues, job);

            // 7. Check for duplicate logs (same date, payer, amount, split list)
            checkDuplicateAnomalies(row, uniqueTrans, issues, job);
        }

        return issues;
    }

    private void checkMissingFields(CSVRowDTO row, List<ImportIssue> issues, ImportJob job) {
        if (isEmpty(row.getDescription())) {
            issues.add(createIssue(job, row, "CRITICAL", "MISSING_FIELD", "Missing transaction description"));
        }
        if (isEmpty(row.getPaidBy())) {
            issues.add(createIssue(job, row, "CRITICAL", "MISSING_FIELD", "Missing payer (paid_by) field"));
        }
        if (isEmpty(row.getAmount())) {
            issues.add(createIssue(job, row, "CRITICAL", "MISSING_FIELD", "Missing amount field"));
        }
        if (isEmpty(row.getCurrency())) {
            issues.add(createIssue(job, row, "WARNING", "MISSING_FIELD", "Missing currency, default to INR will be applied"));
        }
        if (isEmpty(row.getSplitType())) {
            issues.add(createIssue(job, row, "CRITICAL", "MISSING_FIELD", "Missing split_type field"));
        }
        if (isEmpty(row.getSplitWith())) {
            issues.add(createIssue(job, row, "CRITICAL", "MISSING_FIELD", "Missing split_with field"));
        }
    }

    private void checkAmountAnomalies(CSVRowDTO row, List<ImportIssue> issues, ImportJob job) {
        if (isEmpty(row.getAmount())) return;
        
        // Clean quoted amounts (e.g. "1,200" -> 1200)
        String rawAmount = row.getAmount().replace("\"", "").replace(",", "");
        try {
            BigDecimal val = new BigDecimal(rawAmount);
            
            if (val.compareTo(BigDecimal.ZERO) == 0) {
                issues.add(createIssue(job, row, "WARNING", "ZERO_AMOUNT", "Zero value transaction logged"));
            } else if (val.compareTo(BigDecimal.ZERO) < 0) {
                issues.add(createIssue(job, row, "INFO", "NEGATIVE_AMOUNT", "Refund transaction detected (negative amount)"));
            }
            
            // Check decimal precision (e.g. 899.995 has 3 decimal places)
            int scale = val.scale();
            if (scale > 2) {
                issues.add(createIssue(job, row, "INFO", "PRECISION_OVERFLOW", 
                        "Amount has " + scale + " decimal places, will be rounded to 2 decimal places"));
            }
        } catch (NumberFormatException e) {
            issues.add(createIssue(job, row, "CRITICAL", "MALFORMED_RECORD", "Unable to parse amount value: " + row.getAmount()));
        }
    }

    private LocalDate checkDateAnomalies(CSVRowDTO row, List<ImportIssue> issues, ImportJob job) {
        if (isEmpty(row.getDate())) return null;
        
        String rawDate = row.getDate();
        
        // Try parsing DD-MM-YYYY (standard format)
        DateTimeFormatter dmyFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        try {
            return LocalDate.parse(rawDate, dmyFormatter);
        } catch (DateTimeParseException e) {
            // Check for non-standard Month-DD (e.g. Mar-14)
            if (rawDate.matches("^[A-Za-z]{3}-\\d{2}$")) {
                issues.add(createIssue(job, row, "WARNING", "INVALID_DATE", 
                        "Non-standard date format 'Month-Day' (e.g. Mar-14), year will be inferred as 2026"));
                try {
                    String[] parts = rawDate.split("-");
                    String month = parts[0];
                    int day = Integer.parseInt(parts[1]);
                    // Map abbreviation to digit
                    int monthVal = switch (month.toLowerCase()) {
                        case "jan" -> 1; case "feb" -> 2; case "mar" -> 3; case "apr" -> 4;
                        case "may" -> 5; case "jun" -> 6; case "jul" -> 7; case "aug" -> 8;
                        case "sep" -> 9; case "oct" -> 10; case "nov" -> 11; case "dec" -> 12;
                        default -> throw new IllegalArgumentException("Unknown month: " + month);
                    };
                    return LocalDate.of(2026, monthVal, day);
                } catch (Exception ex) {
                    issues.add(createIssue(job, row, "CRITICAL", "INVALID_DATE", "Failed to parse date string: " + rawDate));
                }
            } else {
                issues.add(createIssue(job, row, "CRITICAL", "INVALID_DATE", "Failed to parse date string: " + rawDate));
            }
        }
        return null;
    }

    private void checkPayerAnomalies(CSVRowDTO row, List<ImportIssue> issues, ImportJob job) {
        if (isEmpty(row.getPaidBy())) return;
        
        String payer = row.getPaidBy().trim();
        
        // Check spelling against canonical list
        boolean matched = false;
        for (String canonical : CANONICAL_MEMBERS) {
            if (canonical.equalsIgnoreCase(payer)) {
                matched = true;
                if (!canonical.equals(payer)) {
                    issues.add(createIssue(job, row, "WARNING", "ENTITY_NAME_INCONSISTENCY", 
                            "Payer casing/spacing variant detected: '" + payer + "' (matches canonical '" + canonical + "')"));
                }
                break;
            }
        }
        
        if (!matched) {
            // Check for common suffixes like Priya S
            Optional<String> matchedBase = CANONICAL_MEMBERS.stream()
                    .filter(c -> payer.toLowerCase().startsWith(c.toLowerCase()))
                    .findFirst();
            
            if (matchedBase.isPresent()) {
                issues.add(createIssue(job, row, "WARNING", "ENTITY_NAME_INCONSISTENCY", 
                        "Payer name matches canonical member '" + matchedBase.get() + "' with a suffix/modifier: '" + payer + "'"));
            } else {
                issues.add(createIssue(job, row, "WARNING", "ENTITY_NAME_INCONSISTENCY", 
                        "Payer '" + payer + "' is not in the canonical roommate member list. Will register as a temporary group member."));
            }
        }
    }

    private void checkSplitAnomalies(CSVRowDTO row, LocalDate parsedDate, List<ImportIssue> issues, ImportJob job) {
        if (isEmpty(row.getSplitType())) return;
        
        String splitType = row.getSplitType().trim().toLowerCase();
        
        // 1. Check percentage total
        if ("percentage".equals(splitType) && !isEmpty(row.getSplitDetails())) {
            double totalPct = 0;
            Pattern p = Pattern.compile("(\\d+(\\.\\d+)?)\\s*%");
            Matcher m = p.matcher(row.getSplitDetails());
            while (m.find()) {
                totalPct += Double.parseDouble(m.group(1));
            }
            if (Math.abs(totalPct - 100.0) > 0.01) {
                issues.add(createIssue(job, row, "CRITICAL", "SPLIT_PERCENTAGE_IMBALANCE", 
                        "Split percentages sum to " + totalPct + "%, expected 100%"));
            }
        }
        
        // 2. Check redundant equal splits details
        if ("equal".equals(splitType) && !isEmpty(row.getSplitDetails())) {
            issues.add(createIssue(job, row, "INFO", "REDUNDANT_CONFIGURATION", 
                    "Split type is equal, but redundant shares/breakdown details are listed: " + row.getSplitDetails()));
        }
        
        // 3. Check split list active member violations
        if (!isEmpty(row.getSplitWith())) {
            String[] participants = row.getSplitWith().split(";");
            for (String participant : participants) {
                String pTrim = participant.trim();
                
                // Meera inactive check
                if ("Meera".equalsIgnoreCase(pTrim) && parsedDate != null && parsedDate.isAfter(MEERA_EXIT_DATE)) {
                    issues.add(createIssue(job, row, "WARNING", "INACTIVE_MEMBER_SPLIT", 
                            "Transaction splits with Meera who moved out on " + MEERA_EXIT_DATE.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
                }
                
                // Sam active check
                if ("Sam".equalsIgnoreCase(pTrim) && parsedDate != null && parsedDate.isBefore(SAM_ENTRY_DATE)) {
                    issues.add(createIssue(job, row, "WARNING", "INACTIVE_MEMBER_SPLIT", 
                            "Transaction splits with Sam who only moved in on " + SAM_ENTRY_DATE.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
                }
            }
        }
    }

    private void checkSettlementAnomalies(CSVRowDTO row, List<ImportIssue> issues, ImportJob job) {
        String desc = row.getDescription().toLowerCase();
        String notes = row.getNotes().toLowerCase();
        
        if (desc.contains("paid back") || desc.contains("settlement") || desc.contains("repaid") ||
                notes.contains("settlement") || notes.contains("deposit")) {
            issues.add(createIssue(job, row, "INFO", "PEER_SETTLEMENT", 
                    "Direct balance transfer or settlement detected, not a shared group expense."));
        }
    }

    private void checkDuplicateAnomalies(CSVRowDTO row, Set<String> uniqueTrans, List<ImportIssue> issues, ImportJob job) {
        // Build composite key: Date + Payer + Amount + Description (slugged) + split_with
        String cleanAmount = row.getAmount().replace("\"", "").replace(",", "").trim();
        String key = String.format("%s|%s|%s|%s|%s",
                row.getDate().trim(),
                row.getPaidBy().trim().toLowerCase(),
                cleanAmount,
                row.getDescription().trim().toLowerCase().replaceAll("[^a-z0-9]", ""),
                row.getSplitWith().trim()
        );
        
        if (uniqueTrans.contains(key)) {
            issues.add(createIssue(job, row, "WARNING", "DUPLICATE_EXPENSE", 
                    "Suspected duplicate expense logged on same date with identical amount, payer, and description."));
        } else {
            uniqueTrans.add(key);
        }
    }

    private ImportIssue createIssue(ImportJob job, CSVRowDTO row, String severity, String type, String desc) {
        return ImportIssue.builder()
                .importJob(job)
                .rowNumber(row.getRowNumber())
                .anomalyType(type)
                .severity(severity)
                .description(desc)
                .originalData(row.getOriginalLine())
                .actionTaken("NONE")
                .build();
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
