# Implementation status

Updated: 2026-11-07 (America/New_York; same local date as Toronto). This remains a partial implementation, not a final Stage 2 submission.

**WORKING SUBSET** means the described operation runs end to end; it does not mark the entire original feature complete. **FOUNDATION ONLY** means a boundary exists without the final behavior. **PLANNED** means no implementation claim.

| Feature | Status | Actually working | Still required |
|---|---|---|---|
| F01 | WORKING SUBSET | Manual fields, validation/normalization, create/replace current vehicle through GUI/CLI, identity retained for edit | Independent vehicle-add/edit/save controls; explicit provenance; durable vehicle storage (F09 now provides two in-memory slots) |
| F02 | IMPLEMENTED; LIVE EXTRACTION UNVERIFIED | Gemini-backed command/agent/prompt/parser/draft/F04 flow; CLI and background Swing review/edit/confirm/cancel; source retention and stale-baseline protection | Real Gemini F02 quality verification; no independent verification of seller claims |
| F03 | WORKING SUBSET | All seven validated preferences, independent CLI/GUI save/load, neutral values and in-memory storage | Local persistence and full downstream AI use |
| F04 | WORKING SUBSET | Missing/uncertain fields, conservative sufficiency and structured source conflicts through CLI/GUI | Prioritized gaps and broader uncertain-source interpretation; F02 now supports partial drafts and manual review of conflicts |
| F05 | WORKING SUBSET | Local CSV repository, source labels, normalized lookup, no-match and malformed/I/O handling | Real curated data and integration into planned report features (F09 comparison is implemented) |
| F06 | WORKING SUBSET | Fuel, optional tax and complete-input first-year estimate; exact decimal arithmetic; unknown components preserved | Independent estimate-cost command/panel; durable assumptions/results; wider formats/units (F08 now reuses matching in-memory results) |
| F07 | WORKING SUBSET | Budget, mileage, transmission and accident rules; distinct UNKNOWN and CONFLICT findings | Dedicated controls and report integration (F08/F09 integration is implemented) |
| F08 | IMPLEMENTED; LIVE UNVERIFIED | Multi-tool evidence collection, matching saved cost reuse, automatic/manual strategy, structured Gemini request, strict RiskAssessment/evidence validation, CLI and background GUI; insufficient/unavailable states preserve evidence | Live-model F08 verification; semantic claims remain advisory and require review |
| F09 | IMPLEMENTED; LIVE UNVERIFIED | Separate validated A/B records, shared cost assumptions, symmetric tools, immutable side-by-side metrics, structured Gemini narrative/preference, strict parser, CLI and background GUI, deterministic failure output | Live F09 verification; arbitrary narrative truth needs review; fixed CAD/km units and in-memory storage only |
| F10 | IMPLEMENTED; LIVE UNVERIFIED | Six grouped sections, stable typed items, priorities/reasons/source attribution, F04 + local tool orchestration, structured AI enhancement, usable deterministic fallback, GUI/CLI local completion and generation freshness | Live model verification and human acceptance; in-memory only; reasons require review; not a professional inspection |
| F11 | IMPLEMENTED; LIVE UNVERIFIED | Up to three typed F04/F02-context questions, structured AI prioritization with useful fallback, validated answers/skip/unknown, explicit keep/accept, session history, stale-result guards and actual F08/F10 resume through GUI/CLI | Live provider acceptance; in-memory only; bounded wording; partial F02 drafts require prior correction/confirmation |
| F12 | IMPLEMENTED; LIVE UNVERIFIED | Original four-category typed report, current tool evidence and validated risk reuse, Strategy/Simple Factory guidance, strict structured recommendation, session freshness, GUI/CLI and explicit create-new UTF-8 export | Live provider quality/schema acceptance; semantic prose review; final course deliverables remain separate |

`PreparationResult` is not an `EvaluationReport` or `RiskAssessment`. GUI/CLI expose deterministic preparation, F02 analysis, F08 risk evaluation and F09 comparison and F10 checklists plus F11 clarification/resumption, not all final major features. Unknown transmission/history values remain explicitly UNKNOWN when the corresponding preference is set; strategy selection does not change F07 findings.

## Pattern status

| Pattern | Status | Evidence |
|---|---|---|
| Facade | WORKING | UsedCarApplicationFacade hides tools/session and serves both boundaries |
| Command | WORKING SUBSET | Preparation, listing analysis risk evaluation and comparison commands are invoked by both GUI and CLI |
| Adapter | IMPLEMENTED; LIVE UNVERIFIED | GeminiAdapter performs real HTTP/JSON adaptation; explicit CLI connection test, safe status/errors and offline HTTP verification |
| Strategy | WORKING SUBSET | Three real priority-guidance policies invoked through EvaluationStrategy by the agent |
| Simple Factory | WORKING SUBSET | EvaluationStrategyFactory creates automatic/manual policies used by CLI and GUI; not formal GoF Factory Method |

Facade, Command, Strategy and a Simple Factory now have working relationships. Formal GoF Factory Method is not implemented or claimed; the course minimum of five completed patterns has not been established. F08 implements coordinated tool use and structured LLM reasoning with offline verification. Live Gemini verification and completion of the remaining proposed features remain development targets. The transport is implemented, but a connection test alone does not satisfy the agent-behavior requirement.

Automatic mode chooses Reliability Focused only for two specified priorities with reliability strictly higher; otherwise it chooses Balanced. Missing/zero priorities are neutral. Budget Focused remains an explicit manual choice because the profile has no budget-priority field. Each automatic preview reselects from the latest saved preferences; manual choices persist until explicitly changed. D02 in DESIGN_DECISIONS.md records the Stage 1 clarification and pattern classification.

## Verification boundary

Java 21 compilation, executable JAR/demo, 168 JUnit tests (150 prior tests plus 18 F10 tests), interactive CLI automatic selection, 10 targeted factory checks and Swing smoke operations were run. Factory tests include the 36 valid priority combinations, neutral/null input, policy behavior, manual overrides and saved-profile changes; GUI checks cover automatic/manual transitions and saved versus unsaved edits. The GUI smoke uses real Swing components and button listeners on the EDT, opens and disposes an actual frame, and renders only its own panel. It is not a feature demonstration video or an exhaustive manual desktop acceptance test.

Maven CLI was unavailable, so Maven compilation/test execution is unverified. The dependency-free runner is for environments without JUnit and is not a substitute for Stage 3 JUnit coverage, KUMA agent validation or failure analysis.

No final implementation report, videos, weekly commit history, repository upload or submission has been produced by this task.

F02 verification also runs ListingGuiSmoke with an injected offline LLM: background request, responsive event thread, unconfirmed draft, rejected/corrected fields, explicit approval and cancellation. No live F02 request was made. A prior live connection success is user-reported, not a new independently observed extraction result.

F02 JSON compatibility: structured output is requested through the existing LLM boundary; field-specific safe validation diagnostics replace the generic error. The supplied Corolla advertisement is covered by offline regression tests, including unknown accident history, unavailable maintenance records, optional nulls and explicit confirmation. The exact original response failure and the repaired live F02 request remain unverified.

F08 RiskGuiSmoke additionally verifies background requests, EDT responsiveness, stale-input rejection, valid structured output and provider failure without stale/fabricated AI results. RiskEvidence preserves authoritative tool values and source limitations; a missing/invalid model result leaves the assessment null. No live F08 request was performed.

F09 verification: ComparisonChecks covers both vehicle slots, identical buyer rules, recomputed shared-scenario costs, optional/essential gaps, known/no-match CSV evidence, no self-comparison, invalid/cancelled input, fixed units, strict JSON/IDs/metrics, provider fallback and CLI. ComparisonGeminiChecks uses the actual GeminiAdapter with injected offline transport for structured success, HTTP 429 and timeout. ComparisonGuiSmoke launches the actual GUI and checks B edit/cancel, nonblocking work, stale context and failure recovery. All 150 JUnit tests and all four GUI smoke programs passed; no live F09 request was performed. D13 documents the original contracts and additive evidence/status context.

F10 verification (2026-11-07): 168 JUnit tests passed, 0 failed. ChecklistChecks covers actual tool/LLM sequencing, known/unknown history and transmission, attributed/no-match knowledge, strict structure/evidence checks, misleading history reasons, missing-key/provider fallback, immutable local completion, isolated sessions, vehicle/regeneration invalidation, stale requests and CLI. ChecklistGuiSmoke launches the integrated Swing GUI and exercises grouped items, background generation, check/uncheck/reload, stale rejection and fallback. No live F10 request, final report or video was generated. D14 documents typed-item refinement and bounded AI enhancement.

F11 verification (2026-11-07): actual baseline 168/168 JUnit; final 204/204 (36 new), 0 failures. Existing and F11 Swing smoke tests pass, including fictional Corolla entry, background requests, local responses, history, conflict decisions and real F08/F10 continuation. Packaged CLI fallback acceptance passes. No credentials inspected or live Gemini requests made. At that point F12 was still planned; its implementation is documented below. See README F11 and D15 for exact scope/limits.

F12 verification (2026-11-07): baseline 204 JUnit passed; final 245 passed, 0 failed (41 new). Java21 build/JAR, all seven Swing smoke entry points and packaged CLI Corolla/budget-conflict/unknown-history scenarios passed. F08 risk is reused only against matching evidence; absent/stale risk is explicit, not silently regenerated. Report export is opt-in UTF-8 and cannot overwrite. Existing body-type verification and F08 age-prose limitations are disclosed. No live Gemini request, final submission report/video, Git mutation, or Stage1/UML/root edit occurred. User-reported live F11 success is distinct from unverified F12 live behavior.


## F12 failure classification correction — 2026-11-07

The user reported two real F12 AI_UNAVAILABLE results. The reproduced implementation defect was loss of diagnostic information: the request-stage catch converted configuration, HTTP, transport and provider-envelope errors into one generic sentence. The underlying cause of the user's two real requests remains unverified; there is no captured status/finish reason from those requests.

The existing GeminiAdapter now supplies typed, sanitized failure metadata to F12. Request construction, provider failures, report structure and report evidence failures are distinguished; evidence failures identify a field/rule without rejected values. The same cached diagnostic appears in GUI/CLI/export. At that diagnostic-only checkpoint, schema, model, token limit, request count, evidence validation and Stage1 architecture remained unchanged. Java 21 build succeeds; 264 JUnit tests pass (245 baseline plus 19 new adapter-to-report groups), with all seven Swing verification entry points passing. No real Gemini request was made and no live success is claimed.


## F12 HTTP 400 request-compatibility correction — 2026-11-07

User-confirmed HTTP 400 narrows the failure to request rejection, not local report parsing. The exact historical server reason is unavailable. ReportSchema now uses a small, evidence-independent structural schema (nine required fields/four categories) rather than long evidence enums and dynamic maxItems; prompt duplication is removed. Exact evidence and category validation remains mandatory in Java. GeminiAdapter retains model, endpoint, structured MIME/schema configuration and limits, with bounded, allowlisted provider error classification for future manual diagnostics. See D17 and README for confirmed observations versus the complexity hypothesis.

Java21 normal JAR build and all 274 JUnit tests pass (264 baseline plus ten), alongside all seven GUI verification entry points. Offline transport tests exercise actual F12 serialization and both accepted/rejected structured reports. No real Gemini request, live F12 AI_SUCCESS, Git mutation or Stage1 modification is claimed.
