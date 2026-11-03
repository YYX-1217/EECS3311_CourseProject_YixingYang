# Implementation status

Updated: 2026-11-02 (America/New_York; same local date as Toronto). This remains a partial implementation, not a final Stage 2 submission.

**WORKING SUBSET** means the described operation runs end to end; it does not mark the entire original feature complete. **FOUNDATION ONLY** means a boundary exists without the final behavior. **PLANNED** means no implementation claim.

| Feature | Status | Actually working | Still required |
|---|---|---|---|
| F01 | WORKING SUBSET | Manual fields, validation/normalization, create/replace current vehicle through GUI/CLI, identity retained for edit | Independent vehicle-add/edit/save controls; explicit provenance; multi-vehicle storage |
| F02 | PLANNED | None | Listing extraction, schema validation, uncertainty and confirmation before save |
| F03 | WORKING SUBSET | All seven validated preferences, independent CLI/GUI save/load, neutral values and in-memory storage | Local persistence and full downstream AI use |
| F04 | WORKING SUBSET | Missing/uncertain fields, conservative sufficiency and structured source conflicts through CLI/GUI | Incomplete drafts, prioritized gaps, uncertain source text and dedicated source resolution |
| F05 | WORKING SUBSET | Local CSV repository, source labels, normalized lookup, no-match and malformed/I/O handling | Real curated data and downstream AI integration |
| F06 | WORKING SUBSET | Fuel, optional tax and complete-input first-year estimate; exact decimal arithmetic; unknown components preserved | Independent estimate-cost command/panel; reusable stored assumptions/results; wider formats/units |
| F07 | WORKING SUBSET | Budget, mileage, transmission and accident rules; distinct UNKNOWN and CONFLICT findings | Dedicated controls and downstream AI integration |
| F08 | PLANNED | Deterministic preparation and automatic/manual priority guidance only | Real grounded risk synthesis, evidence validation, structured RiskAssessment |
| F09 | PLANNED | None | Two candidates, symmetric evidence, deterministic comparison and AI trade-offs |
| F10 | PLANNED | None | Grounded grouped checklist, completion state, honest generic fallback |
| F11 | PLANNED | None | One-to-three questions, saved history, skip/conflict handling, resumed workflow |
| F12 | PLANNED | Supporting priority guidance only; no recommendation/report | Fresh evidence aggregation, advisory report, unavailable-evidence labeling and local save |

`PreparationResult` is not an `EvaluationReport` or `RiskAssessment`. GUI/CLI have one useful preparation workflow, not access to all final major features. Unknown transmission/history values remain explicitly UNKNOWN when the corresponding preference is set; strategy selection does not change F07 findings.

## Pattern status

| Pattern | Status | Evidence |
|---|---|---|
| Facade | WORKING | UsedCarApplicationFacade hides tools/session and serves both boundaries |
| Command | WORKING SUBSET | PrepareEvaluationCommand is invoked by both GUI and CLI |
| Adapter | IMPLEMENTED; LIVE UNVERIFIED | GeminiAdapter performs real HTTP/JSON adaptation; explicit CLI connection test, safe status/errors and offline HTTP verification |
| Strategy | WORKING SUBSET | Three real priority-guidance policies invoked through EvaluationStrategy by the agent |
| Simple Factory | WORKING SUBSET | EvaluationStrategyFactory creates automatic/manual policies used by CLI and GUI; not formal GoF Factory Method |

Facade, Command, Strategy and a Simple Factory now have working relationships. Formal GoF Factory Method is not implemented or claimed; the course minimum of five completed patterns has not been established. Live Gemini verification, meaningful AI agent behavior and completion of the proposed features remain development targets. The transport is implemented, but a connection test alone does not satisfy the agent-behavior requirement.

Automatic mode chooses Reliability Focused only for two specified priorities with reliability strictly higher; otherwise it chooses Balanced. Missing/zero priorities are neutral. Budget Focused remains an explicit manual choice because the profile has no budget-priority field. Each automatic preview reselects from the latest saved preferences; manual choices persist until explicitly changed. D02 in DESIGN_DECISIONS.md records the Stage 1 clarification and pattern classification.

## Verification boundary

Java 21 compilation, executable JAR/demo, 86 JUnit tests (72 previous tests plus 14 adapter tests), interactive CLI automatic selection, 10 targeted factory checks and Swing smoke operations were run. Factory tests include the 36 valid priority combinations, neutral/null input, policy behavior, manual overrides and saved-profile changes; GUI checks cover automatic/manual transitions and saved versus unsaved edits. The GUI smoke uses real Swing components and button listeners on the EDT, opens and disposes an actual frame, and renders only its own panel. It is not a feature demonstration video or an exhaustive manual desktop acceptance test.

Maven CLI was unavailable, so Maven compilation/test execution is unverified. The dependency-free runner is for environments without JUnit and is not a substitute for Stage 3 JUnit coverage, KUMA agent validation or failure analysis.

No final implementation report, videos, weekly commit history, repository upload or submission has been produced by this task.
