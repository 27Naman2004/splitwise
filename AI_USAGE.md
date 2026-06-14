# AI Usage Log

This document records the collaboration between the human developer and the AI assistant (Google DeepMind's Antigravity) in designing, planning, and building the Splitwise Clone & Anomaly Detector.

---

## 1. Collaboration Summary

* **AI Role**: Senior Software Architect and Pair Programmer.
* **Human Role**: Product Owner, Code Reviewer, and Deployment Engineer.
* **Timeline**: 2-day rapid development.
* **Primary Scope of AI Contribution**:
  1. Data Auditing & Anomaly Cataloging (Parsing CSV and identifying structural flaws).
  2. Software Architecture design (Drafting `BUILD_PLAN.md`, `SCOPE.md`, and `DECISIONS.md`).
  3. Designing the core algorithms (Debt Simplification min-max greedy algorithm, parser mapping).
  4. Building UI layouts (Glassmorphic dark UI patterns and SVG charts).

---

## 2. Iterative Prompting & Plan Refinements

1. **Initial Assessment**: The AI scanned the workspace files and discovered the raw `Expenses Export.csv` in the system downloads folder, copying it to the workspace for inspection.
2. **Data Engineer Analysis**: The AI reviewed the CSV file line-by-line and flagged 10 discrete categories of anomalies, creating the `CSV_ANALYSIS.md` document to catalog them.
3. **Architecture Definition**: The AI proposed a React + TypeScript + Vanilla CSS client-only structure, which was later expanded to an enterprise Spring Boot + PostgreSQL REST API backend to ensure robustness and relational security.
4. **Interactive Cleansing Concept**: The AI suggested transforming a simple validation page into an interactive wizard where users can resolve anomalies inline prior to committing data.
5. **Backend Project Generation**: The AI generated a production-grade Spring Boot 3 Maven structure including Java 21 entities, repository interfaces, and validations.
6. **CSV Import Module Implementation**: The AI implemented the character-by-character `CSVParser` state-machine, `ValidationService` rules, and `ImportService` orchestrator, enabling validation and staging of records.

---

## 3. Code & Asset Attribution

All files generated or edited during this project have been developed in pair programming sessions.
* **Core Code Modules (AI Generated, Human Reviewed)**:
  * [pom.xml](file:///e:/Splitwise/pom.xml): Maven build configuration.
  * [ImportJob.java](file:///e:/Splitwise/src/main/java/com/internship/splitwise/model/ImportJob.java) & [ImportIssue.java](file:///e:/Splitwise/src/main/java/com/internship/splitwise/model/ImportIssue.java): Staging ledger entities.
  * [CSVParser.java](file:///e:/Splitwise/src/main/java/com/internship/splitwise/service/CSVParser.java): State-machine line parser.
  * [ValidationService.java](file:///e:/Splitwise/src/main/java/com/internship/splitwise/service/ValidationService.java): Validation engine running rules against raw rows.
  * [ImportService.java](file:///e:/Splitwise/src/main/java/com/internship/splitwise/service/ImportService.java): Coordinates file reads, staging audits, and commits.
  * Repositories & DTOs: Schema interfaces and payload definitions.
* **Documentation Assets (AI Generated)**:
  * `BUILD_PLAN.md`: Implementation track.
  * `SCOPE.md`: Functional boundaries and requirements.
  * `DECISIONS.md`: Design patterns and algorithm choices.
  * `CSV_ANALYSIS.md`: Data quality audit report.
  * `BACKEND_DESIGN.md`: Spring Boot API and schema specs.
