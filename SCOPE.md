# Project Scope: Splitwise Clone & Anomaly Detector

## 1. Project Overview
This project is an advanced expense management system designed to import, audit, and reconcile shared expenses from group living or travel. It addresses a real-world scenario where multiple roommates (Aisha, Rohan, Priya, Meera, Sam, Dev, etc.) have recorded transactions over several months, resulting in data quality issues, inconsistent formats, overlapping dates, and currency variations (INR and USD).

The application parses the export, flags data anomalies, provides an interactive interface to clean and resolve them, calculates net balances, and executes a debt simplification algorithm to show the minimum number of transactions needed to settle all accounts.

---

## 2. Discovered CSV Anomalies

The following specific anomalies were discovered in the `Expenses Export.csv` file and form the core test cases for the validation engine:

1. **Duplicate Expenses**:
   * Exact duplication of "Dinner at Marina Bites" on `08-02-2026` for ₹3,200 paid by Dev (Lines 5 and 6).
   * Conflicting double-log for "Dinner at Thalassa" / "Thalassa dinner" on `11-03-2026` (Line 24 vs 25), with differing amounts (₹2,400 by Aisha vs ₹2,450 by Rohan).
2. **Entity Name Inconsistencies**:
   * Payer casing: `priya` (Line 9) vs `Priya`.
   * Trailing space and casing: `rohan ` (Line 27) vs `Rohan`.
   * Name variant suffix: `Priya S` (Line 11) vs `Priya`.
3. **Missing Fields**:
   * Missing payer: "House cleaning supplies" (Line 13) has `paid_by` empty.
   * Missing currency: "Groceries DMart" (Line 28) has `currency` empty.
   * Missing split type: "Rohan paid Aisha back" (Line 14) has `split_type` empty.
4. **Invalid & Ambiguous Dates**:
   * Non-standard format: "Airport cab" (Line 27) date is `Mar-14` (missing year, name format).
   * Sequence/ambiguity issue: "Deep cleaning service" (Line 34) date is `04-05-2026` but is placed between March 28 and April 1, with the note "is this April 5 or May 4? format is a mess".
5. **Currency Inconsistencies**:
   * Mixed currencies with `INR` (Indian Rupee) and `USD` (US Dollars) for Goa trip expenses (Lines 20, 21, 23, 26).
6. **Numeric Precision & Negative Values**:
   * Over-precision float: "Cylinder refill" (Line 10) amount is `899.995`.
   * Negative amount (Refund): "Parasailing refund" (Line 26) amount is `-30` (USD).
   * Zero amount: "Dinner order Swiggy" (Line 31) amount is `0`.
7. **Settlements Mixed with Expenses**:
   * Peer-to-peer payback: "Rohan paid Aisha back" (Line 14) for ₹5,000.
   * Security deposit: "Sam deposit share" (Line 38) for ₹15,000 paid to Aisha.
8. **Percentage Split Integrity**:
   * "Pizza Friday" (Line 15) and "Weekend brunch" (Line 32) split percentages sum to `110%` (`Aisha 30%; Rohan 30%; Priya 30%; Meera 20%`).
9. **Participant Inconsistencies**:
   * Inactive roommate split: "Groceries BigBasket" on `02-04-2026` (Line 36) includes Meera in the split, even though she moved out on March 29 (per Line 33 farewell dinner note).
   * Temporary participant addition: "Parasailing" (Line 23) splits with `Dev's friend Kabir` who is not a regular group member.
10. **Redundant Configurations**:
    * "Furniture for common room" (Line 42) lists `split_type` as `equal`, but includes redundant share breakdowns (`Aisha 1; Rohan 1; Priya 1; Sam 1`).

---

## 3. Functional Requirements

### 3.1. CSV Data Ingestion & Anomaly Auditing
> [!NOTE]
> **Status**: **Backend Implemented**. The `CSVParser` state-machine and `ValidationService` rules engine have been implemented.
* **File Ingest**: Drag-and-drop or select file interface for CSV uploads.
* **Format Parsing**: Robust parser capable of handling:
  * Comma-separated fields with quoted fields containing commas (e.g. `"1,200"`).
  * Missing fields (empty payer or currency).
  * Different date formats (`DD-MM-YYYY` vs `Month-DD` vs `MM-DD-YYYY`).
* **Audit Dashboard**: Before importing, display a comprehensive audit report of all issues categorized by severity:
  * **Critical**: Unparseable rows, missing split members, invalid split details (e.g., percentages summing to 110% instead of 100%), missing payers.
  * **Warning**: Inconsistent names (e.g., "priya" vs "Priya"), currency missing (auto-assumed INR), potential duplicates (same day, same amount, or conflicting duplicate logs), split with inactive members (e.g., Meera after she moved out).
  * **Info**: Non-standard date formats (e.g., `Mar-14`), negative amounts (refunds), floating-point precision issues (3 decimal places).

### 3.2. Interactive Resolution Wizard (Data Cleansing)
Rather than failing or silently skipping bad data, the app guides the user through an interactive workflow to fix issues:
* **Entity Resolution**: Map casing and spelling variants (e.g. "rohan ", "Rohan" -> "Rohan"; "Priya S", "priya", "Priya" -> "Priya").
* **Payer Assignment**: Assign a payer to transactions missing `paid_by` (e.g. Line 13 "House cleaning supplies").
* **Currency Resolution**: Assign default currency to transactions missing `currency` (e.g. Line 28 DMart).
* **Ratio Normalization**: Recalculate or scale percentage splits that do not sum to 100% (e.g. 110% splits in lines 15 and 32), or let the user edit the weights.
* **Duplicate Resolver**: Show suspected duplicates side-by-side (e.g., Dinner at Marina Bites, Dinner at Thalassa) and let the user keep one, merge them, or approve both.
* **Settlement Identifier**: Reclassify transactions marked as expenses that are actually direct payments (e.g., "Rohan paid Aisha back") so they do not get split again.

### 3.3. Expense Management (CRUD)
* **View Transactions**: Search, sort (by date, amount, description), and filter (by payer, currency, date range) all transactions.
* **Create/Update/Delete**: Manually log new expenses or edit/delete existing ones.
* **Settle Up Creator**: Log a direct settlement between two users (e.g., "Aisha paid Priya ₹1,000") to update balances.

### 3.4. Debt Minimization & Settlements Engine
* **Net Balance Calculator**: Compute the net balance of each user in a base currency (default: INR).
* **Exchange Rate Engine**: Convert USD transactions to INR using a customizable exchange rate (default: 1 USD = 83 INR).
* **Transaction Minimizer (Greedy Flow)**: Implement a debt simplification algorithm (Splitwise algorithm) that outputs the optimal, minimized set of transfers needed to resolve all debts.
* **Interactive Settlement Checklist**: Let users mark simplified transactions as "Settled", updating balances dynamically.

### 3.5. Analytics & Visualizations
* **Group Metrics**: Total spent, average expense, distribution of spending by person.
* **Visual Charts**: Beautiful SVG-based dashboard charts (pie chart for category breakdown, line chart for spending trend over time).
* **Timeline of Members**: Show active dates for roommates (e.g., Meera active Feb-March, Sam active April onwards) to contextualize splits.

---

## 4. Non-Functional Requirements
* **Performance**: Browser-based parsing and calculation must execute in under 100ms for files under 10,000 rows.
* **Security & Privacy**: All calculations and files remain client-side; no financial data is uploaded to a remote server.
* **Usability & Design**: Sleeek, modern, responsive glassmorphic dark-theme UI. Zero generic styles; custom typography (Google Fonts Outfit) and refined hover feedback.
* **Data Persistence**: Automatic sync to browser `localStorage` so the user's progress is saved across page reloads.

---

## 5. Out of Scope (For MVP 2-Day Timeline)
* **Multi-group support**: The application assumes a single group space (roommates + trip).
* **Live Exchange Rate APIs**: Exchange rates will be configurable manually in the settings, rather than fetching live rates (avoids API keys and CORS errors).
* **User Authentication**: No login required; local-first user experience.
* **OCR Receipt Scanning**: Adding expenses is done manually or via CSV.
