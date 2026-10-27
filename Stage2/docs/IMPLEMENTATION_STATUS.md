# Implementation status

Baseline date: 2026-10-26 (America/New_York; same local date as Toronto). This is initial scaffolding, not a final Stage 2 submission.

**WORKING SUBSET** means the described operation runs end to end; it does not mark the entire original feature complete. **FOUNDATION ONLY** means a boundary exists without the final behavior. **PLANNED** means no implementation claim.

| Feature | Status | Actually working | Still required |
|---|---|---|---|
| F01 | WORKING SUBSET | Manual fields, validation/normalization, create/replace current vehicle through GUI/CLI, identity retained for edit | Independent vehicle-add/edit/save controls; explicit provenance; multi-vehicle storage |
| F02 | PLANNED | None | Listing extraction, schema validation, uncertainty and confirmation before save |
| F03 | WORKING SUBSET | Validated budget/mileage limits, neutral unspecified limits, session storage and CLI show | Full preference UI, priority/tolerance semantics, local persistence |
| F04 | WORKING SUBSET | Known unknown history/transmission/fuel fields listed; basic conservative sufficient flag | Incomplete drafts, prioritized gaps, uncertain source text, checkConflict and source resolution |
| F05 | PLANNED | None | Local repository, source metadata, normalized lookup, no-match and I/O failure cases |
| F06 | WORKING SUBSET | Fuel, optional tax and complete-input first-year estimate; exact decimal arithmetic; unknown components preserved | Independent estimate-cost command/panel; reusable stored assumptions/results; wider formats/units |
| F07 | WORKING SUBSET | Explicit budget/mileage rules and boundary behavior | Transmission and accident rules; unknown/not-evaluable constraint status |
| F08 | PLANNED | Supporting deterministic preparation only | Real grounded risk synthesis, evidence validation, structured RiskAssessment |
| F09 | PLANNED | None | Two candidates, symmetric evidence, deterministic comparison and AI trade-offs |
| F10 | PLANNED | None | Grounded grouped checklist, completion state, honest generic fallback |
| F11 | PLANNED | None | One-to-three questions, saved history, skip/conflict handling, resumed workflow |
| F12 | PLANNED | None | Fresh evidence aggregation, advisory report, unavailable-evidence labeling and local save |

`PreparationResult` is not an `EvaluationReport` or `RiskAssessment`. GUI/CLI have one useful preparation workflow, not access to all final major features. Unknown transmission/history rules are not falsely reported as passing: output explicitly limits current checks to budget and mileage.

## Pattern status

| Pattern | Status | Evidence |
|---|---|---|
| Facade | WORKING | UsedCarApplicationFacade hides tools/session and serves both boundaries |
| Command | WORKING SUBSET | PrepareEvaluationCommand is invoked by both GUI and CLI |
| Adapter | FOUNDATION ONLY | LLMClient and GeminiAdapter exist; generate throws an honest unavailable error |
| Strategy | PLANNED | All original strategy roles/methods documented in traceability |
| Factory Method / Factory | PLANNED | Distinction and proposed correction documented; no count claimed |

Only two useful pattern relationships are currently operational. Five completed patterns, a real LLM, meaningful AI agent behavior and all twelve features remain final-development targets.

## Verification boundary

Java 21 compilation, executable JAR/demo, 10 JUnit tests, interactive CLI processing and Swing smoke operations were run. The GUI smoke uses real Swing components and button listeners on the EDT, opens and disposes an actual frame, and renders only its own panel. It is not a feature demonstration video or an exhaustive manual desktop acceptance test.

Maven CLI was unavailable, so Maven compilation/test execution is unverified. The dependency-free runner is for environments without JUnit and is not a substitute for Stage 3 JUnit coverage, KUMA agent validation or failure analysis.

No final implementation report, videos, weekly commit history, repository upload or submission has been produced by this task.
