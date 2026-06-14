# Splitwise Clone & Anomaly Detector

An advanced roommate expense management system designed to import, audit, and reconcile shared expenses. It parses CSV exports, identifies data quality anomalies (duplicates, wrong currencies, split imbalances), guides the user through an interactive resolution flow, calculates balances, and runs a debt-simplification algorithm.

---

## Technical Stack

*   **Backend Framework**: Spring Boot 3.3.0 (Java 21)
*   **Database**: PostgreSQL 16+ / Hibernate JPA
*   **Frontend**: React + TypeScript (Vite)
*   **Styling**: Vanilla CSS (Custom dark glassmorphic design system)
*   **Testing**: JUnit 5 + Mockito

---

## Core Features

1.  **Staging & Anomaly Auditing**: Drag-and-drop CSV importer that parses and audits data quality issues before writing to the core ledger.
2.  **Split Strategies Engine**: Extensible strategy pattern implementations supporting `Equal`, `Percentage`, `Shares`, and `Unequal` split methods.
3.  **Dynamic Exchange Rates**: dynamic conversion of USD expenses to INR using a database-backed exchange rate table or default `83.00` fallback.
4.  **Debt Simplification (Greedy Flow)**: Min-max greedy ledger solver that reduces transaction density to at most $N-1$ payments.
5.  **Itemized Auditable Ledger**: Rohan's requirement: user balance traces showing the exact role, split amount, paid amount, and net impact of each transaction.
6.  **Dedicated Settlements**: Separate peer-to-peer settlements ledger to log paybacks directly.

---

## Local Development Setup

### 1. Prerequisites
*   JDK 21+
*   Maven 3.8+
*   Node.js 18+
*   PostgreSQL running locally

### 2. Backend Server Setup
1.  Configure PostgreSQL connection properties in `src/main/resources/application.properties` or set environment variables:
    ```bash
    export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/splitwise
    export SPRING_DATASOURCE_USERNAME=postgres
    export SPRING_DATASOURCE_PASSWORD=postgres
    ```
2.  Run the backend:
    ```bash
    mvn spring-boot:run
    ```
3.  Exposed endpoints:
    *   Health check: `GET http://localhost:8080/api/health`
    *   API namespace: `/api/**`

### 3. Frontend Client Setup
1.  Navigate to the `frontend/` directory:
    ```bash
    cd frontend
    ```
2.  Install dependencies:
    ```bash
    npm install
    ```
3.  Start the development server:
    ```bash
    npm run dev
    ```
4.  Open the web app in your browser at `http://localhost:5173`.

---

## Testing

Run all unit tests:
```bash
mvn test
```

---

## Production Deployment

For step-by-step instructions on deploying the PostgreSQL database, Render backend, and Vercel frontend, please refer to:
*   [DEPLOYMENT.md](file:///e:/Splitwise/DEPLOYMENT.md)
