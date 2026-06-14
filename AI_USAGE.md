# AI_USAGE.md — AI Tools, Prompts & Failure Cases

## 1. AI Tools Used

| Tool | Version | Purpose |
|------|---------|---------|
| **Google Gemini (Antigravity IDE)** | Gemini 2.5 Pro | Primary pair programmer — architecture, code generation, debugging |
| **GitHub Copilot** | GPT-4o | Inline autocomplete for boilerplate (DTOs, repository methods) |

---

## 2. How AI Was Used (Key Prompts)

### 2.1 CSV Anomaly Analysis
**Prompt given to AI**:
> "Read this CSV file line by line. Identify every data quality problem you find — wrong formats, duplicates, missing fields, inconsistencies. Categorize them by severity."

**What AI produced**: A structured list of 10 anomaly categories with line numbers, descriptions, and suggested severities (CRITICAL/WARNING/INFO). This became the foundation of `SCOPE.md` and the `ValidationService` rule engine.

### 2.2 Architecture Design
**Prompt given to AI**:
> "Design a Spring Boot backend for a Splitwise clone. It needs to support CSV import with anomaly staging, a strategy pattern for split calculations, balance tracing per user, and debt simplification. Use Java 21, PostgreSQL, Spring Security JWT."

**What AI produced**: The full entity relationship design, package structure, and the two-phase import flow (`STAGED → ISSUES_FOUND → COMMITTED`).

### 2.3 Split Strategy Pattern
**Prompt given to AI**:
> "Implement the Strategy Pattern for expense splitting. Support Equal, Unequal, Percentage, and Shares. Make it easy to add a new type in under 10 minutes. Include a factory."

**What AI produced**: `SplitStrategy` interface, 4 implementations, and `SplitStrategyFactory` with a `Map<SplitType, SplitStrategy>` registry pattern.

### 2.4 Greedy Debt Simplification
**Prompt given to AI**:
> "Implement the Splitwise debt simplification algorithm. Given N users with net balances, find the minimum number of transactions to settle all debts. Explain the algorithm step by step."

**What AI produced**: The min-max greedy approach in `BalanceService.simplifyDebts()` — separate creditors and debtors into two priority queues, always settle the smaller of the max-creditor and max-debtor amounts.

### 2.5 Deployment Setup
**Prompt given to AI**:
> "Deploy this project. Backend on Render, database on Render PostgreSQL, frontend on Vercel. Generate all environment variables and a Dockerfile."

**What AI produced**: `render.yaml`, `backend/Dockerfile` (multi-stage Maven build), and the full environment variable mapping.

---

## 3. Three Concrete Cases Where AI Was Wrong

### ❌ Case 1: AI Generated Wrong Runtime for Render

**What AI did wrong**:
When generating `render.yaml`, the AI set `runtime: java` — assuming Render's free tier supported Java as a native runtime. It does not. Render's free tier only supports: Docker, Node, Python 3, Go, Ruby, Rust, Elixir.

**How I caught it**:
The first deploy attempt on Render failed with the error:
```
bash: line 1: mvn: command not found
```
Looking at the Render dashboard, the Language dropdown only showed the 7 options above — Java was absent.

**What I changed**:
- Added a `backend/Dockerfile` with a two-stage Maven build:
  ```dockerfile
  FROM maven:3.9.6-eclipse-temurin-21 AS builder
  RUN mvn clean package -DskipTests
  FROM eclipse-temurin:21-jre-alpine
  COPY --from=builder /app/target/splitwise-0.0.1-SNAPSHOT.jar app.jar
  ```
- Updated `render.yaml` to `runtime: docker`
- Changed Render service language from "Java" (non-existent) to "Docker"

---

### ❌ Case 2: AI Used Raw `axios` in AuthContext Instead of Configured API Instance

**What AI did wrong**:
When generating `AuthContext.tsx`, the AI imported raw `axios` and called `axios.post('/api/auth/login', ...)` directly, bypassing the configured `api` instance in `src/services/api.ts`.

The `api.ts` instance was the one configured with:
```typescript
baseURL: import.meta.env.VITE_API_BASE_URL || ''
```

**How I caught it**:
After deploying to Vercel, the login and registration calls failed in production with network errors, even though all other API calls (expenses, balances) worked correctly. Inspecting the network tab showed `/api/auth/login` was being called relative to `vercel.app` instead of the Render backend URL.

**What I changed**:
```typescript
// WRONG (AI generated)
import axios from 'axios';
const response = await axios.post('/api/auth/login', { email, password });

// FIXED (human corrected)
import api from '../services/api';
const response = await api.post('/api/auth/login', { email, password });
```

---

### ❌ Case 3: AI Generated Lambda Scope Error in GroupController

**What AI did wrong**:
In `GroupController.java`, the AI generated code that tried to use a non-final variable inside a lambda:

```java
// WRONG (AI generated)
Group savedGroup = groupRepository.save(group);
savedGroup.getMembers().forEach(m -> {
    // savedGroup used inside lambda - but AI tried to reassign it above
    m.setGroup(savedGroup);
});
```

The actual code the AI produced had the variable being modified inside the lambda, which Java's compiler rejects with:
> `Variable used in lambda expression should be final or effectively final`

**How I caught it**:
The Maven build failed during `mvn compile` with this exact compiler error:
```
[ERROR] GroupController.java: Variable 'savedGroup' is accessed from within inner class,
needs to be declared final or effectively final
```

**What I changed**:
Introduced an explicitly `final` intermediate variable so the lambda could capture it:
```java
// FIXED (human corrected)
final Group savedGroup = groupRepository.save(group);
savedGroup.getMembers().forEach(m -> m.setGroup(savedGroup));
```

---

## 4. Overall Assessment

**AI was very useful for**:
- Generating boilerplate (entities, DTOs, repository interfaces)
- Designing algorithms (debt simplification, strategy pattern)
- Writing documentation structure (SCOPE.md, DECISIONS.md outlines)
- Debugging error messages quickly

**AI required human correction for**:
- Platform-specific deployment details (Render runtime support)
- Cross-file consistency (using the right axios instance across all files)
- Java compiler edge cases (lambda variable capture rules)
- Business logic validation (ensuring percentage splits ≠ 110% was a human-caught requirement from the CSV analysis, not AI-initiated)

**Lesson learned**: AI is effective as a "fast first draft" tool, but every output needs to be tested, compiled, and validated against real platform constraints before being committed.
