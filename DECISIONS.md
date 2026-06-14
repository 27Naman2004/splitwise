# Architectural Decisions Document (ADD)

This document lists the architectural, design, and algorithmic decisions made during the design phase of the Splitwise Clone & Anomaly Detector.

---

## 1. Backend Framework & Technology Stack
* **Decision**: Spring Boot 3.x (Java 21) + Spring Data JPA + PostgreSQL 16+.
* **Rationale**:
  * **Java 21**: Introduces Virtual Threads (Project Loom), improving parallel processing capabilities for heavy validation batch operations.
  * **Spring Boot**: Industry standard for enterprise backend applications, offering seamless dependency injection, automated REST routing, and comprehensive JPA tooling.
  * **PostgreSQL**: Robust, ACID-compliant relational database, perfect for managing structured relational ledgers, foreign keys, transaction rollbacks, and numeric precision indexes.

---

## 2. Front-End Tech Stack & Styling (Client Interface)
* **Decision**: React with TypeScript via Vite + Vanilla CSS.
* **Rationale**:
  * React + Vite offers rapid hot-reload feedback, allowing quick creation of the cleansing panels.
  * **Vanilla CSS** provides complete styling control, implementing a custom dark glassmorphic design system utilizing custom variables, CSS Modules, and Outfit sans-serif fonts.

---

## 3. Database Schema & Migration Management
* **Decision**: Relational normalization in PostgreSQL, managed by **Flyway Migrations**.
* **Rationale**:
  * A normalized design (splitting `expenses` and `expense_splits` into distinct tables) prevents update anomalies, ensures indexing efficiency, and mirrors production ledger standards.
  * **Flyway**: Integrates natively with Spring Boot. It guarantees that the database schema is built, versioned, and migrated consistently across dev, test, and production environments using plain SQL scripts.

---

## 4. Staging and Audit Architecture (Two-Phase Ingestion)
* **Decision**: Save CSV imports as staged batch transactions in `import_batches` and `anomalies` before merging into core ledgers.
* **Rationale**:
  * Directly saving unvalidated CSV data to the active `expenses` table risks corrupting ledger history (e.g. including inactive users in splits or saving double-counted entries).
  * Writing to staging batches and raising `anomalies` allows the user to inspect issues interactively and apply fixes. Only when all critical issues are resolved is the batch merged (`COMMITTED`) into the active ledger tables, maintaining strict data integrity.

---

## 5. CSV Parsing Engine
* **Decision**: Custom state-machine reader using regex.
* **Rationale**:
  * Standard CSV parsers split strings by commas, which fails on quoted formats (e.g., `"1,200"` in Line 7).
  * A custom state machine can keep track of line indices, verify columns, perform string-level diagnostics (such as detecting casing variants, trailing spaces, or invalid date codes), and save metadata directly to `anomalies` records.

---

## 6. Debt Simplification Algorithm (Greedy Flow)
* **Decision**: Min-max greedy ledger optimization.
* **Rationale**:
  * Balance equations are calculated dynamically: $Balance_i = Paid_i - Shared_i$.
  * Separating members into `Creditors` ($Bal > 0$) and `Debtors` ($Bal < 0$) and recursively settling the min of absolute values reduces transaction density.
  * This guarantees that a group of $N$ members resolves all balances in at most $N-1$ transactions, eliminating redundant mesh payments.

---

## 7. Currency Handling & Settlements
* **Decision**: Base ledger calculations are computed in INR, with conversions performed using a configurable static rate (default: `1 USD = 83 INR`).
* **Rationale**:
  * Calculating group balances requires a single base currency. Since the majority of expenses are INR, it forms the base.
  * USD items are stored with original tags, but converted to INR for ledger additions and splits.
  * P2P transactions flagged as settlements (e.g. Line 14: "Rohan paid Aisha back") are treated as balance bypasses, transferring balance directly between the two members to prevent standard split division.

---

## 8. Import Entity Renaming (ImportJob & ImportIssue)
* **Decision**: Transition from `ImportBatch`/`Anomaly` nomenclature to `ImportJob` and `ImportIssue`.
* **Rationale**:
  * Reflects standard task execution semantics where an upload is a single "Job" with a distinct validation workflow.
  * Captures the specific data engineering terminology of resolving data "Issues" rather than mathematical "Anomalies", since issues can be fixed or ignored, while anomalies might represent valid transactions.

---

## 9. Parse-Time Line Capture
* **Decision**: Custom state-machine `CSVParser` that returns fields and retains the raw `originalLine` string of each row.
* **Rationale**:
  * To show a high-fidelity audit report, the UI must display the exact string that was malformed (e.g., the row content containing `"1,200"` or `Mar-14`).
  * By preserving the raw text at the parsing stage, we can bind it directly to the `ImportIssue` record under `original_data` without needing to re-read the file during UI rendering.
