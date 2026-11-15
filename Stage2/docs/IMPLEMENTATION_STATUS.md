# Implementation status

Updated: 2026-11-15. These statements describe the current local working tree, including uncommitted changes. Implemented workflows are not proof of exhaustive AI quality or completion of all submission materials. The GUI has twelve independent F01–F12 workspaces; the CLI uses distinct commands through the same facade/agent/services. Save and calculate combines deterministic preparation tasks. Each workspace shows its own results.

| Feature | Implemented behavior | Evidence and remaining limits |
|---|---|---|
| F01 | Validated current-vehicle creation/editing; combined preparation save; F09 second vehicle slot | Offline verified; in-memory, no durable vehicle database |
| F02 | Listing extraction, partial draft, source checks, correction and explicit confirmation | Offline verified; previous audit validated a real draft; user reports prior live success; seller claims unverified |
| F03 | All seven preferences, independent save/load, Strategy use | Offline verified; session only; body-type matching is not implemented |
| F04 | Missing/uncertain data, sufficiency, source conflicts, F02/F11 integration and dedicated workspace | Offline verified; broader free-text interpretation is limited |
| F05 | CSV repository/tool, provenance, no-match/error handling, F08–F12 evidence use | Offline verified; instructional data, not reliability research |
| F06 | Fuel, tax, complete-input first-year cost and missing components; dedicated workspace | Offline verified; calculation action still uses combined preparation; supplied assumptions, fixed units |
| F07 | Budget, mileage, transmission and accident rules; UNKNOWN versus CONFLICT; dedicated workspace | Offline verified; no body-type rule or separate constraint editor |
| F08 | Evidence/Strategy synthesis, year/grounding checks, safe rejection, cache-aware GUI | AUD-01/02/03 offline verified; prior audit live success predates these corrections; bounded English checks are not universal fact checking |
| F09 | Symmetric A/B evidence, bounded grounded narrative/preference and transient retry | Offline verified; current live HTTP 200 / AI_SUCCESS in one attempt; previous 503/timeout not reproduced |
| F10 | Six-section typed checklist, grounded actions, completion, safe classified fallback | Offline adapter/GUI/CLI verified; six provider maxItems bounds removed; metric-unit guard; final live HTTP 200 / AI_SUCCESS |
| F11 | Bounded questions, answer/skip/unknown, keep/accept conflicts, history, F08/F10 continuation | Offline verified; current live HTTP 200 / AI_SUCCESS; prior timeout not reproduced; no cross-session restoration |
| F12 | Four-category recommendation, authoritative evidence/F08 reuse, cache invalidation, UTF-8 export | FIN-01 history contradictions rejected; current live HTTP 200 / AI_SUCCESS and matching UTF-8 export; earlier timeout cause unverified |

`PreparationResult` remains distinct from `RiskAssessment` and `EvaluationReport`. Unknown values remain unknown; AI failure preserves deterministic evidence. F10 fallback and F12 unavailable output are not fabricated AI recommendations.

## Pattern status

Strategy, Adapter, argument-based Simple Factory, Command and Facade have connected implementations. `EvaluationStrategyFactory` returns concrete policies through the `EvaluationStrategy` interface; formal GoF Factory Method inheritance is not claimed. UI code invokes shared services/commands rather than duplicating business logic. Acceptance of course submission materials remains a separate review.

Automatic mode chooses Reliability Focused when specified reliability priority exceeds specified fuel priority; otherwise Balanced. Budget Focused is a manual choice. See [design decisions](DESIGN_DECISIONS.md) and the [README](../README.md) for the documented Stage 1 refinement.

## Current targeted repair — 2026-11-15

**398 JUnit passed, 0 failed**, up from the 380-test baseline: 16 new narrative/profile groups and two checklist bounds/units groups. Java 21 build/JAR, seven Swing smoke entry points, twelve-workspace navigation, 17 launcher/credential checks, 36 actual CLI cases and 13 cross-feature assertions passed. Focused subsets passed: F10 33, F12/profile 89, F03 1, F08 age/grounding 38, F09 24, F11 60. Subset counts overlap and are not added to 398.

Exactly six authorized serial real requests were used: original F10 HTTP 400; schema-only F10 HTTP 200 with a review-discovered unit error; final F10 HTTP 200 / validated AI_SUCCESS after the unit guard; F12, F11 and F09 HTTP 200 / validated AI_SUCCESS. F12 export matched. Model/endpoint/timeouts and F09/F11 production logic were unchanged. No further requests were made. F02/F08 have earlier full-audit live evidence, not a new request in this repair.

F10's controlled schema-only change removed six `maxItems: 40` values while retaining all essential fields and Java's 40-item and exact-plan checks. Provider grammar complexity is supported by official documentation, but the server returned only generic INVALID_ARGUMENT, so its precise internal rule remains unknown. The actual wrong-unit reasons are covered by a narrow metric/CAD prompt and Java guard.

FIN-01's exact contradictions and observed summary failed three pre-fix regression tests. The new targeted history validator checks the free explanation against saved claims; other report fields remain constrained to authoritative evidence. Unknown-history gates, legitimate verification/advice, category restrictions, numeric/age/cost/constraint checks, cache replacement and safe UTF-8 export are preserved. These checks are not a universal language fact checker. FIN-02 shared GUI/CLI text now accurately describes Strategy use and absent F07 body-type matching.

The original root Stage 1 PDF was already deleted before this task. Its exact HEAD blob is recoverable, but deletion intent is uncertain; the working-tree deletion is left unchanged. Source DOCX/UML were not modified. Final report/UML/videos, AI usage records and submission evidence still require separate completion/review.

## Historical AUD-04–06 follow-up

At that checkpoint, baseline: 364 JUnit tests. Observed result: **380 passed, 0 failed** (13 new F10 adapter groups and 3 F12 timeout/payload/export groups), Java 21 compilation and JAR packaging passed. All existing Swing smoke entry points, the new HTTP 400/timeout GUI cases, twelve-workspace navigation and 17 Windows launcher/credential checks passed. Focused JUnit runs passed: F10 31, F12 73, F08/AUD-01/AUD-02 58, F11 60. These focused counts are subsets of the 380 total, not additional tests. AUD-03 GUI scenarios also passed. Tests use fictional credentials and injected transport; no real credentials were read and no live Gemini calls were made.

- **AUD-04: improved, not live-verified.** Request serialization/schema tests pass. The actual F10 fixture is 10,105 UTF-8 bytes (prompt 6,744; schema 2,481). Keywords/types and exclusive schema/MIME configuration match the documented contract; model-specific complexity limits cannot be certified offline. The only production change preserves `LLMFailure.diagnostic()` in F10 fallback. The earlier HTTP 400 body/reason was not retained; schema complexity remains a hypothesis. ChecklistSchema, prompt, adapter, model and timeouts are unchanged.
- **AUD-05: application defect not reproduced.** The real audit timeout had no response. Mocked timeout handling, export and explicit recovery pass. A fictional F11-update/F08/F12 replay uses a 6,219-byte prompt, 751-byte schema, 7,305-byte request and existing 30-second request timeout. No F12 production logic, schema, timeout or retry behavior changed. Network/provider latency remains unverified.
- **AUD-06: current documentation synchronized.** Twelve-workspace navigation, shared operations, current verification counts and live-versus-offline evidence are distinguished. Historical milestone counts below are retained as history, not current totals.

For official schema references and the exact one-request F10/F12 manual retest procedure, see the [README F10](../README.md#f10-vehicle-specific-inspection-checklist--uc10--sd05) and [README F12](../README.md#f12-personalized-purchase-recommendation-and-evaluation-report). These results do not certify the final report, updated submission UML, demonstration videos, weekly GitHub activity, Stage 3 KUMA validation or grading acceptance.

## Historical verification milestones

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


## F08 model-year consistency correction — 2026-11-07

Confirmed the observed six-year Risks statement passes the committed parser but fails the corrected Java validation. F08 now carries a Clock-derived reference-year snapshot, authoritative model-year difference and explicit unknown service age. Targeted checks cover strengths, risks, uncertainties and explanation, with safe unavailable fallback and explicit retry. Generic guidance/unrelated numbers remain usable. F11 invalidation and F12 reuse of valid matching-year risk pass regression tests; the F12 compact schema and GeminiAdapter are untouched.

Java21 build/JAR succeeds; 294 JUnit tests pass, zero failures (274 baseline plus 20), with all seven GUI entry points including bad-age rejection/recovery and CLI fallback verification. No real API request was made. The rules do not verify arbitrary prose, all age paraphrases or non-English claims; live acceptance remains independent. Existing Simple Factory/final-deliverable limitations are unchanged.
