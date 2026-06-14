# Splitwise Clone & Anomaly Detector

> **Live Demo**: [https://splitwise-swart-omega.vercel.app](https://splitwise-swart-omega.vercel.app)
> **Backend API**: [https://splitwise-2-x66d.onrender.com/api/health](https://splitwise-2-x66d.onrender.com/api/health)
> **GitHub**: [https://github.com/27Naman2004/splitwise](https://github.com/27Naman2004/splitwise)

A full-stack roommate expense management system with an intelligent CSV anomaly detection engine. It imports ledger exports, identifies 10 categories of data quality problems, guides the user through an interactive resolution wizard, calculates per-user net balances in INR (with USD conversion), and runs a greedy debt-simplification algorithm to output the minimum number of transactions needed to settle all accounts.

---

## Technical Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Spring Boot 3.3 (Java 21) |
| **Database** | PostgreSQL 16+ via Spring Data JPA / Hibernate |
| **Frontend** | React 18 + TypeScript (Vite 5) |
| **Styling** | Vanilla CSS — custom dark glassmorphic design system |
| **Auth** | Spring Security 6 + Stateless JWT (JJWT 0.12) |
| **Testing** | JUnit 5 + Mockito (12 passing unit tests) |
| **Deployment** | Docker (Render) + Vercel |

---

## Core Features

1. **CSV Staging & Anomaly Auditing** — Drag-and-drop CSV importer that parses and audits data quality issues (duplicates, wrong currencies, inactive-member splits, name variants, percentage overflows) before writing to the core ledger.
2. **Strategy Pattern Split Engine** — Extensible strategy pattern supporting `EQUAL`, `PERCENTAGE`, `SHARES`, and `UNEQUAL` split methods. New split types can be added by implementing one interface.
3. **Dynamic Exchange Rates** — USD→INR conversion via a database-backed exchange rate table (default fallback: `1 USD = 83.00 INR`).
4. **Greedy Debt Simplification** — Min-max greedy algorithm that reduces N×N mesh payments to at most N−1 transactions.
5. **Traceable Balance Ledger** — Rohan's requirement: each user can see exactly which expenses created their balance (role, split amount, paid amount, net impact per expense).
6. **Settlements Engine** — Dedicated peer-to-peer settlement ledger separate from expense splits.
7. **JWT Auth** — Stateless JWT-secured REST API with BCrypt password hashing.

---

## Project Structure

```
splitwise/
├── backend/                     ← Spring Boot (Java 21)
│   ├── src/main/java/com/internship/splitwise/
│   │   ├── config/              (SecurityConfig, CorsConfig)
│   │   ├── controller/          (Auth, Balance, Currency, Expense, Group, Import, Settlement)
│   │   ├── dto/                 (Request/Response DTOs)
│   │   ├── model/               (JPA Entities)
│   │   ├── repository/          (Spring Data interfaces)
│   │   └── service/
│   │       ├── split/           (Strategy pattern implementations)
│   │       ├── BalanceService.java
│   │       ├── CSVParser.java
│   │       ├── ImportService.java
│   │       └── ValidationService.java
│   ├── src/main/resources/application.properties
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                    ← React + TypeScript (Vite)
│   ├── src/
│   │   ├── components/
│   │   ├── context/             (AuthContext)
│   │   ├── pages/               (Dashboard, Expenses, Settlements, ImportReport, ...)
│   │   └── services/api.ts
│   └── package.json
├── render.yaml                  ← Render deployment config
├── SCOPE.md                     ← Anomaly log + DB schema
├── DECISIONS.md                 ← Decision log
├── AI_USAGE.md                  ← AI tool log
└── README.md
```

---

## Local Development Setup

### Prerequisites
- JDK 21+
- Maven 3.8+
- Node.js 18+
- PostgreSQL running locally (or use H2 in-memory for quick start)

### Backend Setup

```bash
# Option A: Use H2 in-memory (no PostgreSQL needed)
cd backend
mvn spring-boot:run
# App starts at http://localhost:8080

# Option B: Use local PostgreSQL
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/splitwise
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export JWT_SECRET=any-local-dev-secret
cd backend
mvn spring-boot:run
```

Health check: `GET http://localhost:8080/api/health` → `{"status":"UP"}`

### Frontend Setup

```bash
cd frontend
npm install
npm run dev
# Opens at http://localhost:5173
```

> In local dev the Vite proxy (`vite.config.ts`) forwards `/api/*` to `localhost:8080` automatically — no `VITE_API_BASE_URL` needed.

### Run Tests

```bash
cd backend
mvn test
# 12 tests, 0 failures
```

---

## Production Deployment

| Service | Platform | URL |
|---------|----------|-----|
| Frontend | Vercel | https://splitwise-swart-omega.vercel.app |
| Backend | Render (Docker) | https://splitwise-2-x66d.onrender.com |
| Database | Render PostgreSQL | Internal connection |

### Required Environment Variables — Render (Backend)

| Key | Value |
|-----|-------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host>/<db>` |
| `SPRING_DATASOURCE_USERNAME` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `CORS_ALLOWED_ORIGINS` | `https://splitwise-swart-omega.vercel.app` |
| `JWT_SECRET` | Any long random string |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` |

### Required Environment Variables — Vercel (Frontend)

| Key | Value |
|-----|-------|
| `VITE_API_BASE_URL` | `https://splitwise-2-x66d.onrender.com` |

---

## AI Tools Used

See [AI_USAGE.md](./AI_USAGE.md) for a detailed log of AI tools, key prompts, and three concrete cases where the AI produced incorrect output and how it was caught and corrected.
