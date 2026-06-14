# Build Plan: Splitwise Clone & Anomaly Detector

This document outlines the step-by-step engineering plan to build the Splitwise Clone and Anomaly Detector application within a 2-day timeline. It is divided into logical phases, each with clear objectives, file lists, and verification criteria.

---

## Project Timeline Overview

```
Phases           | Hours   | Key Focus
-----------------+---------+---------------------------------------------------
Phase 1: Init    | 0 - 4   | Project bootstrap, CSS design tokens, base routes.
Phase 2: Core    | 4 - 12  | Parser implementation, Anomaly Rules, Debt Engine.
Phase 3: Wizard  | 12 - 20 | CSV Uploader, Interactive Resolution Wizard.
Phase 4: Dash    | 20 - 30 | KPI Dashboard, Debt Graph, CRUD forms, local storage.
Phase 5: Visuals | 30 - 36 | SVG Analytics charts, timeline limits.
Phase 6: Release | 36 - 48 | Testing, final QA, documentation, Git, Vercel deploy.
```

---

## Detailed Phases

### Phase 1: Project Initialization & Configuration (Hours 0-4)
* **Goal**: Establish the base template, package structure, and custom CSS design system.
* **Tasks**:
  1. Initialize Vite project with React + TypeScript:
     `npx -y create-vite@latest ./ --template react-ts`
  2. Install dependencies (e.g. `lucide-react` for premium icons, and `vitest` for unit testing).
  3. Create global stylesheet `src/index.css` incorporating the glassmorphic dark theme variables (obsidian background `#090d16`, glowing card borders, Outfit font, clean typography).
  4. Build layout wrapper containing a responsive header, main content frame, and dark mode toggles.
* **Verification**: Run `npm run dev` to verify the blank glassmorphic template loads successfully in the browser.

### Phase 2: Core Algorithmic Engines (Hours 4-12)
* **Goal**: Implement parser, anomaly rules, and debt minimization algorithm in pure TypeScript, with 100% unit test coverage.
* **Files**:
  * `src/types.ts`: TypeScript contracts for `Expense`, `Anomaly`, `GroupMember`, `Settlement`.
  * `src/utils/csvParser.ts`: Implements row parser, quote-cleanser, date-normalizer, split percentage auditing, and duplicate detection.
  * `src/utils/debtEngine.ts`: Implements net balance calculations and the min-max greedy debt simplification solver.
  * `src/utils/csvParser.test.ts` & `src/utils/debtEngine.test.ts`: Test suites.
* **Verification**:
  * Run test suite: `npx vitest run` to ensure all edge cases (percentages summing to 110%, direct peer settlements, floats like 899.995, USD to INR conversions) parse and calculate correctly.

### Phase 3: CSV Import & Anomaly Resolution Wizard (Hours 12-20)
* **Goal**: Build the interactive portal that lets users drag-and-drop the raw export, view detected anomalies, and correct them in-place.
* **Components**:
  * `src/components/ImportWizard.tsx`: File upload drop-zone, wizard stepper.
  * `src/components/AnomalyTable.tsx`: Visual audit log categorizing issues (Critical/Warning/Info) with action buttons (e.g. "Fix Casing", "Re-scale Ratios", "Classify as Settlement").
* **Verification**:
  * Drag-and-drop the raw `Expenses Export.csv` into the UI.
  * Verify that all 10 types of discovered anomalies are listed.
  * Click "Fix Casing" and check that Rohan and priya are resolved. Click "Normalize Splits" and check that 110% ratios are re-scaled to 100%.

### Phase 4: Main Dashboard, Transactions CRUD, & Settlements (Hours 20-30)
* **Goal**: Create the primary landing interface for managing group expenses and settlements.
* **Components**:
  * `src/components/Dashboard.tsx`: KPI cards (Total Group Spend, Individual Outlays) and Net Balances.
  * `src/components/DebtSimplifier.tsx`: Renders the minimized transaction list ("Who owes whom") with "Settle Up" checkboxes.
  * `src/components/ExpenseList.tsx`: Tabular view of all active expenses. Search, filter by participant or currency, and sort features.
  * `src/components/ExpenseForm.tsx`: Modal or slide-out form to manually add or edit expenses and log peer settlements.
* **Verification**:
  * Settle an expense and check that net balances update.
  * Manually add an expense in USD and verify it automatically converts to INR in the main balance sheets based on the set exchange rate.

### Phase 5: Analytics Charts & Timeline Boundaries (Hours 30-36)
* **Goal**: Provide beautiful visual summaries of spending behaviors and prevent post-timeline violations.
* **Components**:
  * `src/components/Analytics.tsx`: Interactive SVG-based charts showing expense categories and expense ratios per person.
  * `src/components/GroupTimeline.tsx`: Sidebar widget showing member durations (e.g. Meera active Feb-Mar, Sam active Apr onwards) to highlight why certain splits exclude some individuals.
  * Settings component: Allows manual tuning of USD -> INR exchange rate.
* **Verification**:
  * Verify visual responsiveness of SVG charts.
  * Toggle exchange rate settings (e.g., from 83 to 85) and check that all dashboard metrics adjust accordingly.

### Phase 6: Production Verification & Deployment (Hours 36-48)
* **Goal**: Run final code verification, push to GitHub, and deploy the live site.
* **Tasks**:
  1. Build project: `npm run build` to verify there are no TypeScript compiler errors.
  2. Setup Git, stage files, and write comprehensive commit histories.
  3. Initialize Git repository and add remote.
  4. Create static configuration for deployment (e.g. `vercel.json` or gh-pages configuration).
  5. Generate the final documentation including `README.md` and the static `Import_Report.md`.
* **Verification**:
  * Deploy code to Vercel and check that the live URL functions perfectly on both desktop and mobile viewports.
