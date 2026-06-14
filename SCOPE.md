# SCOPE.md — Anomaly Log & Database Schema

## 1. Project Overview

This project is a full-stack expense management system that imports, audits, and reconciles shared expense ledgers. The CSV provided represents 6+ months of roommate expenses (Aisha, Rohan, Priya, Meera, Sam, Dev) with intentionally embedded data quality problems across 10 anomaly categories.

The application stages every CSV row before committing it to the live ledger. Every anomaly is flagged with a severity level (`CRITICAL`, `WARNING`, `INFO`), stored in the `import_issues` table, and presented in an interactive audit wizard for resolution.

---

## 2. Discovered CSV Anomalies

The following anomalies were found in `expenses_export.csv` and form the core test cases for the `ValidationService`:

### Category 1 — Duplicate Expenses
| Line | Description | Finding | Severity |
|------|-------------|---------|----------|
| 5 & 6 | "Dinner at Marina Bites" on 08-02-2026 for ₹3,200 by Dev | Exact duplicate row | CRITICAL |
| 24 & 25 | "Dinner at Thalassa" / "Thalassa dinner" on 11-03-2026 | Conflicting duplicate — different amounts (₹2,400 vs ₹2,450) and different payers (Aisha vs Rohan) | CRITICAL |

**How handled**: `ValidationService.checkDuplicates()` groups rows by `(date, description_normalized, amount)`. Exact matches are flagged CRITICAL. Near-matches (same date & description, different amount) flagged WARNING. User resolves via "Keep One / Keep Both" wizard.

### Category 2 — Entity Name Inconsistencies (Name Variants)
| Line | Raw Value | Expected | Severity |
|------|-----------|---------|----------|
| 9 | `priya` | `Priya` | WARNING |
| 11 | `Priya S` | `Priya` | WARNING |
| 27 | `rohan ` | `Rohan` | WARNING |

**How handled**: `ValidationService.checkNameConsistency()` normalizes all names to title-case and trims whitespace. Fuzzy matching (Levenshtein distance ≤ 2) detects variants. Flagged as WARNING with suggested canonical name.

### Category 3 — Missing Fields
| Line | Expense | Missing Field | Severity |
|------|---------|---------------|----------|
| 13 | "House cleaning supplies" | `paid_by` empty | CRITICAL |
| 28 | "Groceries DMart" | `currency` empty | WARNING |
| 14 | "Rohan paid Aisha back" | `split_type` empty | WARNING |

**How handled**: Missing `paid_by` → CRITICAL (cannot split without payer). Missing `currency` → WARNING, auto-assumed INR. Missing `split_type` → WARNING, settlement-type rows flagged for reclassification.

### Category 4 — Invalid & Ambiguous Dates
| Line | Raw Date | Problem | Severity |
|------|----------|---------|----------|
| 27 | `Mar-14` | Missing year, non-standard format | INFO |
| 34 | `04-05-2026` | Ambiguous — April 5 or May 4? Out of sequence with surrounding March rows | WARNING |

**How handled**: `CSVParser` attempts 3 date format patterns (`dd-MM-yyyy`, `MM-dd-yyyy`, `MMM-dd`). Ambiguous dates flagged INFO with detected format shown. Year-less dates assume current year.

### Category 5 — Currency Inconsistencies
| Lines | Currencies Found | Problem |
|-------|-----------------|---------|
| 20, 21, 23, 26 | INR and USD mixed | Goa trip expenses in both currencies — requires conversion for balance calculation |

**How handled**: `CurrencyController` maintains exchange rates. USD amounts converted to INR at configured rate (default 83.00) before being added to net balance calculations. Original currency and original amount stored for display.

### Category 6 — Numeric Precision & Negative Values
| Line | Value | Problem | Severity |
|------|-------|---------|----------|
| 10 | `899.995` | 3 decimal places — rounding ambiguity (rounds to ₹900 or ₹899.99?) | INFO |
| 26 | `-30 USD` | Negative amount — refund scenario | INFO |
| 31 | `0` | Zero amount — "Dinner order Swiggy" | WARNING |

**How handled**: Values rounded to 2 decimal places on ingest. Negative amounts treated as refunds (credited back to split participants). Zero amounts flagged WARNING — likely a data entry error.

### Category 7 — Settlements Mixed with Expenses
| Line | Entry | Problem | Severity |
|------|-------|---------|----------|
| 14 | "Rohan paid Aisha back" ₹5,000 | This is a P2P payment, not a shared expense — should not be split | WARNING |
| 38 | "Sam deposit share" ₹15,000 to Aisha | Security deposit payback, not a group expense | WARNING |

**How handled**: `ValidationService.detectSettlements()` uses keyword matching ("paid back", "deposit", "refund", "transfer") to flag settlement-type rows. User confirms reclassification in wizard → stored in `settlements` table, not `expenses`.

### Category 8 — Percentage Split Integrity
| Line | Expense | Percentages | Problem | Severity |
|------|---------|------------|---------|----------|
| 15 | "Pizza Friday" | Aisha 30% + Rohan 30% + Priya 30% + Meera 20% = 110% | Sum exceeds 100% | CRITICAL |
| 32 | "Weekend brunch" | Same breakdown = 110% | Sum exceeds 100% | CRITICAL |

**How handled**: `PercentageSplitStrategy.validate()` sums all percentages and throws `InvalidSplitException` if not within ±0.01 of 100. Flagged CRITICAL — user must rescale (normalize to 100%) or manually edit percentages in the wizard.

### Category 9 — Participant Inconsistencies (Inactive Member)
| Line | Expense | Problem | Severity |
|------|---------|---------|----------|
| 36 | "Groceries BigBasket" 02-04-2026 | Meera in split — but farewell dinner (Line 33) on 29-03-2026 indicates she moved out | WARNING |
| 23 | "Parasailing" | Includes "Dev's friend Kabir" — temporary external participant | WARNING |

**How handled**: Membership timeline tracked via `GroupMember` entity with `joinedAt`/`leftAt` timestamps. `ValidationService.checkMembershipActive()` flags any split with a member whose `leftAt` is before the expense date.

### Category 10 — Redundant Split Configuration
| Line | Expense | Problem | Severity |
|------|---------|---------|----------|
| 42 | "Furniture for common room" | `split_type = equal` but also includes explicit share breakdown `(Aisha 1; Rohan 1; Priya 1; Sam 1)` | INFO |

**How handled**: Flagged INFO — the explicit shares are redundant when split is EQUAL. Redundant data is ignored; EQUAL strategy distributes evenly.

---

## 3. Database Schema

### Entity Relationship Overview

```
users ─────────────────┐
  │                    │
  │ (paid_by)          │ (member)
  ↓                    ↓
expenses          group_members ← groups
  │                    
  ↓                    
expense_splits (owes_user_id → users)
  
import_jobs ─────────────────→ import_issues
  │
  ↓ (on commit)
expenses + expense_splits

settlements (payer_id → users, payee_id → users)

exchange_rates
```

### Table Definitions

#### `users`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `name` | VARCHAR(100) | NOT NULL |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE |
| `password_hash` | VARCHAR(255) | NOT NULL |
| `created_at` | TIMESTAMP | NOT NULL |

#### `groups`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `name` | VARCHAR(100) | NOT NULL |
| `created_by` | UUID | FK → users.id |
| `created_at` | TIMESTAMP | NOT NULL |

#### `group_members`
| Column | Type | Constraints |
|--------|------|-------------|
| `group_id` | UUID | PK (composite), FK → groups.id |
| `user_id` | UUID | PK (composite), FK → users.id |
| `joined_at` | TIMESTAMP | NOT NULL |
| `left_at` | TIMESTAMP | nullable — NULL means still active |

#### `expenses`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `group_id` | UUID | FK → groups.id |
| `description` | VARCHAR(255) | NOT NULL |
| `amount` | DECIMAL(12,2) | NOT NULL |
| `currency` | VARCHAR(3) | NOT NULL, default 'INR' |
| `amount_inr` | DECIMAL(12,2) | NOT NULL — converted amount |
| `paid_by` | UUID | FK → users.id |
| `split_type` | ENUM | EQUAL/UNEQUAL/PERCENTAGE/SHARES |
| `expense_date` | DATE | NOT NULL |
| `created_at` | TIMESTAMP | NOT NULL |

#### `expense_splits`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `expense_id` | UUID | FK → expenses.id |
| `owes_user_id` | UUID | FK → users.id |
| `amount_owed` | DECIMAL(12,2) | NOT NULL |
| `split_value` | DECIMAL(10,4) | percentage/shares value if applicable |

#### `settlements`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `group_id` | UUID | FK → groups.id |
| `payer_id` | UUID | FK → users.id |
| `payee_id` | UUID | FK → users.id |
| `amount` | DECIMAL(12,2) | NOT NULL |
| `currency` | VARCHAR(3) | NOT NULL |
| `settled_at` | TIMESTAMP | NOT NULL |
| `note` | TEXT | nullable |

#### `import_jobs`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `group_id` | UUID | FK → groups.id |
| `uploaded_by` | UUID | FK → users.id |
| `file_name` | VARCHAR(255) | NOT NULL |
| `file_content` | TEXT | Full CSV stored for re-parse after resolution |
| `status` | ENUM | STAGED / ISSUES_FOUND / COMMITTED / FAILED |
| `row_count` | INTEGER | |
| `issue_count` | INTEGER | |
| `uploaded_at` | TIMESTAMP | NOT NULL |

#### `import_issues`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `import_job_id` | UUID | FK → import_jobs.id |
| `row_number` | INTEGER | CSV line number |
| `anomaly_type` | ENUM | DUPLICATE / MISSING_PAYER / NAME_VARIANT / INVALID_DATE / etc. |
| `severity` | ENUM | CRITICAL / WARNING / INFO |
| `description` | TEXT | Human-readable explanation |
| `original_data` | TEXT | Raw CSV row string |
| `resolution_status` | ENUM | PENDING / RESOLVED / SKIPPED |
| `resolution_note` | TEXT | nullable — what action was taken |

#### `exchange_rates`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `from_currency` | VARCHAR(3) | NOT NULL |
| `to_currency` | VARCHAR(3) | NOT NULL |
| `rate` | DECIMAL(10,4) | NOT NULL |
| `effective_date` | DATE | NOT NULL |
