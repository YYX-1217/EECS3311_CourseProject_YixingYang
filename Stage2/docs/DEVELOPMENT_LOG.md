# AI-human development log

Date: 2026-10-26 (user timezone America/New_York).

## Collaboration record

- Tool: OpenAI Codex desktop coding agent.
- Model: GPT-6-based Codex according to session identity; exact deployed model variant/build is not exposed here. Do not infer a more specific model name.
- Token usage and monetary cost: unavailable in this session; pending user account/tool reporting. No fabricated totals.
- Human instruction: inspect original Stage1 and course materials, preserve all existing files, create only a local Stage2 foundation, no Git mutations/remote operations, no LLM calls, honest feature/pattern status.
- AI contribution: local discovery/PDF-DOCX/XML inspection, traceability and discrepancy analysis, Java domain/tools/facade/command/session/CLI/Swing code, Maven metadata, offline scripts, basic JUnit/fallback/GUI checks and documentation.
- Human contribution completed: provided scope, baseline expectations and strict operational constraints. Human code review, acceptance of technical refinements and any future submission are **pending**, not assumed completed.

## Important work and decisions

1. Located the actual working project at `Desktop/eecs3311/project`, not the suggested Documents checkout. Stage2 had one instruction PDF and no implementation to overwrite.
2. Read the complete 53-page Stage1 report and compared source DOCX/task texts; inspected current class/use-case/sequence XML and images. Identified DeepSeek-vs-Gemini discrepancy, factory terminology ambiguity, incomplete-draft representation gap and clarification signature mismatch.
3. Preserved recognizable classes and Stage1 flow. Implemented only deterministic preparation with a shared command/facade, not fake AI evaluation.
4. Chose BigDecimal, immutable validated records, explicit nullable optional amounts, in-memory session, and fail-safe save-after-compute behavior. Added CostAssumptions and PreparationResult with documented purposes.
5. Used existing JDK 21 and local JUnit 4.13.2/Hamcrest. PATH java was 1.8.0_251; Maven CLI was not found. No software installed or downloaded.
6. Recorded original feature IDs, source targets and methods for all twelve features. Deferred unused strategy/repository/result classes instead of generating disconnected empty files.

## Verification actually performed

Working directory for relative commands: `C:\Users\yixin\Desktop\eecs3311\project\Stage2`.

| Command / check | Actual outcome |
|---|---|
| Existing JDK `javac -version` | `javac 21.0.12.1` |
| `& .\scripts\build.ps1` | Java 21 compilation and executable `build/used-car-assistant.jar` succeeded after sandbox-related failures were investigated |
| `& .\scripts\run.ps1 -Mode demo` | Real end-to-end demo printed 1,080 L, CAD 1,782 fuel, CAD 2,340 tax, CAD 24,922 first year; detected price 18,000 > budget 17,000 |
| `& .\scripts\verify.ps1 -Gui` | Executed outside the restrictive sandbox after tool approval: JUnit 4.13.2 reported `OK (10 tests)`; GUI smoke passed |
| `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -DependencyFree` | Windows PowerShell execution succeeded; `PASS: 10 deterministic verification groups` |
| Interactive CLI through `FoundationChecks.cliFlow` | Actual prompt-input workflow consumed all form fields, calculated results, stored vehicle, displayed profile and reported AI feature PLANNED |
| GUI smoke | Real frame launched; example button populated fields; calculate returned 24,922; negative price showed error and preserved session; changed price 16,000 recalculated 22,662; frame disposed |
| GUI rendering review | Viewed `build/verification/gui.png`, generated from this application's actual Swing panel. Fields, tabs, result pane and controls visible; result pane is scrollable |

No Maven build, live Gemini response, external API test, KUMA run, full twelve-feature acceptance test or video recording is claimed.

## Failures and corrections

- Initial compilation with extra lint options intermittently failed writing `.class` files under the Codex Windows sandbox. Basic compilation succeeded; scripts use the normal Java 21 compiler path. No source logic defect was claimed as the cause of that filesystem error.
- The first sandboxed JUnit compile produced package-resolution errors and a Java `AccessDeniedException` canonicalizing an existing VS Code JUnit jar. Running the same local verification script with approved sandbox escalation succeeded: 10 JUnit tests and the real Swing smoke test. No permissions, ACLs or system configuration were changed.
- Quoted JVM `-D` arguments in PowerShell to prevent argument parsing from splitting property names. Scripts locate their root and restore the caller's directory with try/finally.
- No failing domain assertion remained after verification. This does not establish full application correctness or complete test coverage.

## Human review items and limitations

Review all generated Java classes and tests; verify the UI manually on your display; confirm the chosen year/range/rounding policies; decide the draft/provenance representation and evaluation priority field; review factory terminology with course expectations; retain provider discrepancy in the final Stage2 report. Validate that local project files are later integrated into the correct existing repository only in a separately authorized task.

The foundation has no durable profile/session storage, full preference controls, complete data-quality/constraint rules, knowledge repository, reasoning loop, Gemini transport, final report or two-vehicle management. Adapter is foundation-only; Strategy and Factory are planned. Final Stage2 requirements remain outstanding.

## Safety record

All newly authored project code/configuration/documentation and retained build/inspection artifacts are under Stage2. Original Stage1 files, lecture documents and the existing Stage2 instruction PDF were protected by a before/after hash comparison. The final measured result is appended below after verification.

No Git command was run. No repository/branch/commit was created. No add/commit/push/pull/fetch/reset, remote GitHub access, upload, synchronization, submission, deployment, LLM request or global software/environment change occurred. No real API credential was requested, read or printed; `.env.example` contains only a placeholder.

Final integrity check: 154 protected pre-existing files checked; 0 changed, 0 missing, 0 newly added outside Stage2. `pom.xml` parsed successfully as XML.

## 2026-11-02 — Real Gemini provider adapter

Inspected Stage 1 PDF/DOCX, class/SD01/SD03/SD06 UMLet definitions and Stage 2 instructions. Implemented Java 21 HTTP POST adaptation, strict Gson JSON handling, environment-only credentials/model, safe error/status transitions and an explicit CLI connection test through existing facade/agent/LLMClient. Preserved Strategy/Factory and deterministic domain behavior. Added one pinned JSON runtime dependency and packaging support; no Maven requirement introduced.

Verification: Java 21 script build, 86 JUnit tests (14 new adapter groups), loopback HTTP and injected failure checks, deterministic packaged CLI demo and Swing smoke. An accidental change to an existing immutability test's exception type was corrected before the final successful run. No real Gemini call was made; live connectivity, account permissions and billing remain unverified. No Git mutation, commit or push was performed. Domain AI features remain incomplete.

## 2026-11-02 — F02 listing analysis and confirmation

Read actual Stage1 PDF/DOCX and SD01 UMLet contracts, F02/UC02 and Section 9 explanations, and Stage2 implementation/change requirements. Tracking status was clean before editing; existing untracked .vscode and course PDF were left alone. Implemented constrained PromptBuilder, strict ResponseParser, a partial VehicleDraft, result/baseline boundary, AnalyzeListingCommand, shared agent/facade analysis/confirmation, and F04 draft checks. Added CLI multiline review/edit/confirm/cancel and a Swing listing tab with background work and explicit approval. Gemini output allowance increased to 4096 tokens for structured JSON; no new dependency or automatic retries.

Java21 build and 102 JUnit tests pass (86 existing, 16 F02 groups); prior GUI checks and dedicated asynchronous F02 GUI smoke pass. Missing-key failures and offline fixture analysis preserve session data. No live F02 API request was made; the prior successful live connection is reported by the user. No Git state-changing commands, commits, pushes or root/Stage1/UML modifications were made. F02 software workflow is implemented, while real-model extraction quality and full Stage2 compliance are not claimed.

## 2026-11-02 — F08 multi-tool risk assessment

Read actual Stage1 F08/UC08/SD03, relevant class/pattern/traceability/implementation definitions, local original Project Instructions and Stage2 instructions. Preflight found no uncommitted tracked work; only existing ignored-for-task VS Code configuration and course PDF remained untracked. Implemented the Stage1 risk result and original parser/prompt signatures, coordinated knowledge/constraint tools, matching cost reuse, automatic/manual strategy and one structured Gemini request. Added evidence-preserving result states, cache invalidation, CLI evaluate and a background Swing action.

Verification: Java21 compilation/JAR succeeded; 126 JUnit tests passed (106 previous plus 20 F08 groups). GUI regressions and dedicated F02/F08 background checks passed, including stale-context and failure evidence behavior. Corrected two test-code issues found during verification (immutability exception expectation and a shadowed counter). No actual Gemini request was made. No Git state change, commit, push, source-history alteration or Stage1/UML/root edit occurred. Semantic truth of arbitrary AI prose and live F08 behavior remain unverified; no guarantees or final submission claim.

## 2026-11-02 — F09 two-vehicle comparison

Preflight: read-only status/history confirmed committed F08 (`4e9d0e3`), F02 (`3851728`) and Gemini (`b45b679`); only known VS Code configuration and course PDF remained untracked. Inspected original Stage1 PDF/DOCX, class and SD04 UMLet, relevant F09/UC09 contracts and implementation explanations, Stage2 instructions and original Project Instructions.

Implemented separate session Vehicle B, shared production validation, common saved cost assumptions, symmetric actual tools, immutable VehicleComparison/evidence/status results, constrained prompt/schema and strict parser. Added CLI B entry/review/confirmation/cancellation and compare, plus a small Swing comparison panel with background synthesis and stale-result rejection. Retained original Stage1 result/method contracts and documented additive context/status APIs in D13. No duplicate services or provider integration were introduced.

Verification: Java21 compilation and executable JAR succeeded; 150 JUnit tests executed, 150 passed, 0 failed (126 prior + 24 F09). Actual adapter serialization/envelope parsing, HTTP failure and timeout use injected transport with fictional credentials. Existing GUI/F02/F08 smoke tests and new ComparisonGuiSmoke passed. A test compilation issue caused by reassigning a lambda-captured local variable was corrected before the successful run. Duplicate JSON property rejection was added during integrity review. No live Gemini request, credential inspection, Git mutation, commit, push, upload or original Stage1/UML/root edit occurred. F09 real-model explanation quality remains unverified; F10–F12 and final submission artifacts are outside this increment.

## 2026-11-07 — F10 inspection checklist and local completion

Read-only preflight verified committed F09 (de96d28) and earlier F08/F02/Gemini work. Only pre-existing .vscode and the Stage2 professor PDF were untracked. Inspected the actual Stage1 PDF/DOCX, F10/UC10/SD05 UMLet, class/mapping contracts, professor Stage2 instructions, current Java architecture and existing GUI/CLI/test scripts. F10 was not implemented.

Implemented typed immutable grouped items, deterministic evidence-based planning/fallback, actual checker/knowledge/LLM orchestration, structured schema and strict parser, current-generation session storage and local completion. Added CLI generation/show/mark/unmark and an integrated Swing checklist panel with background generation and stale-result protection. D14 records typed-item and bounded-synthesis design clarifications. The original result-returning methods remain available.

Verification: Java21 compilation and executable JAR succeeded; 168 JUnit tests executed, 168 passed, 0 failed (150 prior + 18 F10). Existing GUI/F02/F08/F09 and new F10 Swing smoke checks passed with offline fixtures. A shadowed counter in new test code caused an initial test-compilation error and was corrected. Visual inspection of the application's rendered checklist led to a small scroll-position/long-text usability correction. No real Gemini request or credential inspection occurred. No Git mutation, commit, push, history change, upload, original Stage1/UML/root edit, final report or video was performed. Manual real-model quality and human GUI acceptance remain unverified.

## F11 clarification and follow-up increment — 2026-11-07

Inspected original Stage2 instructions, Stage1 F11/UC11/SD05 report and class/sequence artifacts, existing F02/F04/F08/F09/F10 implementation, tests/scripts and read-only Git state. Confirmed F10 HEAD 9bcd8e6 and an actual 168/168 JUnit baseline before edits. Existing untracked .vscode/ and Stage2/Project-Stage2-Instruction.pdf were left untouched.

Implemented typed questions/schema, constrained Agent/PromptBuilder/ResponseParser orchestration through existing LLMClient/GeminiAdapter, deterministic fallback, validated field updates, explicit known-value conflict decisions, in-memory response/history/revision handling and F02 ambiguity retention. Real F08/F10 continuation uses fresh vehicle evidence; accepted edits invalidate dependent caches. Added integrated asynchronous ClarificationPanel and CLI commands; text answers preserve case. D15 documents the typed-signature and bounded-wording refinements. No new dependencies or transport changes.

Verification commands from repository root: `powershell -NoProfile -ExecutionPolicy Bypass -File .\Stage2\scripts\build.ps1` and `powershell -NoProfile -ExecutionPolicy Bypass -File .\Stage2\scripts\verify.ps1 -Gui`, with an explicitly empty process GEMINI_API_KEY. Final Java21 build/executable JAR succeeded; 204 JUnit tests passed, 0 failed (168 prior + 36 F11). All existing and F11 actual-frame Swing smoke entry points passed. Two initial new-test failures were caused by fixtures that respectively lacked an intended price conflict and included additional unaddressed conflicts; fixtures were corrected to test the intended behaviors, without disabling assertions. Visual inspection of the rendered application confirmed usable F11 controls/status/history. The packaged Java CLI ran the complete fictional Corolla prepare/clarify/answer/unknown/skip/reopen/resume/keep/accept/checklist scenario with no errors. Test-generated CLI output and GUI render are ignored build artifacts only.

Final review also found that verbose F02 ambiguity labels could exceed the existing F10 item-ID length limit; labels now retain the canonical field and a concise unresolved-claim marker. A regression covers all supported listing ambiguity targets reaching F10.

No real Gemini call, credential inspection, Git mutation, commit, push, Stage1/UML/root edit, final report, presentation or video. Live F11 model/schema acceptance and human manual acceptance remain unverified. Session data is not persistent; F12 remains planned.

## F12 personalized recommendation/report increment — 2026-11-07

Preflight read-only Git inspection confirmed HEAD a1bc28b (committed F11), with no tracked unfinished F11 changes. Existing untracked .vscode/, Stage2/Project-Stage2-Instruction.pdf and root file `tatus --short` were preserved. No remote verification or Git mutation occurred. Read Stage1 F12/UC12/SD06 PDF pages, original SD06 image/XML and relevant class UMLet; inspected the professor's complete Stage2 instruction PDF and current code. Baseline Java compilation succeeded, but initial JAR packaging was blocked by the user's running application. Compiled baseline JUnit directly against the current classes: 204 passed. The user subsequently closed the application and normal build/verification scripts succeeded; no forced process termination or isolated checkout was needed.

Implemented original advisory categories and sections, explicit constraints, current evidence/risk checks, existing tool and Strategy coordination, structured Adapter generation and validation, memory/freshness, real GUI/CLI and opt-in UTF-8 export. Exact supplied costs and source limitations are immutable; unknown histories and F11 conflicts remain uncertain. Bounded evidence sections plus guarded free-form explanation mitigate unsupported assertions; F08 age prose is not forwarded. See D16 for refinements and limits.

Verification from repository root: `powershell -NoProfile -ExecutionPolicy Bypass -File .\Stage2\scripts\build.ps1`; `powershell -NoProfile -ExecutionPolicy Bypass -File .\Stage2\scripts\verify.ps1 -Gui`, with an explicitly empty GEMINI_API_KEY in the test process. Final Java21 JAR build succeeded; 245 JUnit tests passed, 0 failed (204 prior + 41 F12). All seven GUI smoke entry points passed. New test code initially required an effectively-final lambda-variable correction. A legacy CLI assertion checked PLANNED in help instead of actual behavior; it now verifies the real F08 insufficient-information status and non-AI label, retaining the original safety intent without relying on obsolete help text. No tests were disabled.

Final review added coverage for explicitly stale prior risk, current F08 insufficiency and combined real F04/F02/F11/cost uncertainty exceeding forty entries; F12 uses a bounded sixty-four-item parser while prior workflows keep their original forty-item cap. GUI visual review led to a buyer-facing category/summary at the top and readable preferences/cost assumptions. The packaged CLI verified fictional Corolla values (1080 L / CAD 1782 fuel / CAD 1625 tax / CAD 18707 total), AI-unavailable evidence, cached report/export, accepted F11 price/consumption recalculation, stale report rejection, budget conflict and unknown-maintenance sufficiency gate. Only the deliberately stale report-show command produced its expected error. Fictional smoke exports/output/images are ignored build artifacts.

No real Gemini request, credential inspection, external report transmission, automatic private report export, Stage1/UML/root modification, commit/push, final implementation report or demonstration recording. Live F12 quality/schema acceptance, broad semantic truth checking, the separate F08 age-prose issue, strict Factory Method compliance and final submission artifacts remain outside the completed verification boundary.


## F12 real-failure diagnostic correction — 2026-11-07

Inspected GenerateReportCommand, Facade/Session, Agent, PromptBuilder, ReportSchema, LLMClient/GeminiAdapter, ResponseParser, EvaluationReport/ReportEvaluation and ReportPanel, comparing F08/F11 structured request patterns. The user's exact generic message originates in the request catch; report parse/evidence rejection used a different message. No HTTP status or finish reason from the real failures was available. No unsupported-schema, model or quota diagnosis was invented. The documented schema keywords do not alone prove provider compatibility for this exact dynamic schema.

Reproduced the lost-error-category defect using injected transport failures in the production adapter-to-F12 path. Added LLMFailure (provider-independent typed metadata) and safe fixed diagnostic descriptions. HTTP codes, configuration, timeout/transport/interruption, blocked/incomplete/MAX_TOKENS/unusable envelopes are distinguished. F12 separately handles local prompt construction, provider call, report structure and evidence validation. Evidence messages name the failing field using application-controlled names. Raw provider bodies/exception causes/credentials are never propagated. Existing strict success predicates and request settings remain intact; no speculative schema simplification or output-limit increase.

The first normal verification attempt compiled Java but could not replace the JAR because the user was running it. Recompiled and ran the baseline tests directly: 245 passed. The user then closed the program. Ran from repository root with empty test-process GEMINI_API_KEY and GEMINI_MODEL: `powershell -NoProfile -ExecutionPolicy Bypass -File .\Stage2\scripts\verify.ps1 -Gui`. Normal Java 21 compilation/JAR packaging succeeded; 264 tests passed, zero failed. All seven actual-frame GUI entry points passed; the F12 GUI assertion additionally verifies safe diagnostic display and absence of raw private exception text.

Nineteen new JUnit groups cover the production F12 command/facade/adapter request and successful envelope/validated report, HTTP 400/401/403/404/429/500/503, timeout/network/interruption, missing/unsafe configuration, blocked/truncated/non-final/malformed envelopes, malformed/wrong-type report JSON, per-field evidence tampering, unknown-client error privacy, CLI cached diagnostics and invalid local schema rejection. All credentials and provider responses are fictional fixtures; no real endpoint is contacted. No Git mutation, unrelated untracked-file change, Stage1/UML/root edit or live-success claim. The next diagnostic step is one explicit user-run F12 attempt and its sanitized diagnostic code, as documented in README.


## F12 persistent HTTP 400 request correction — 2026-11-07

The user now reports F12 PROVIDER_HTTP_400; F08/F11 have succeeded with the same configuration. Re-inspected actual request creation and compared ReportSchema, F08/F11 schemas, PromptBuilder, agent and strict parser. Read-only Git status confirmed the earlier uncommitted F12 work and unrelated untracked files remain present. Initial normal build compiled Java but encountered the running JAR lock; baseline tests compiled/run directly passed 264/264. The user closed the application before final normal packaging.

A fictional-only Java probe measured old F12 schema 3458 characters, 29 enum entries, six maxItems constraints including one zero; F08 423 characters/four enum entries, F11 963/thirteen for three clarification fields. F12 prompt contained the same schema again. No credentials, environment values or private requests were inspected. Official Google API documentation confirms supported schema keywords and warns about combined constraint/enum complexity; no blanket maxItems-zero prohibition was assumed.

Implemented a fixed-shape F12 structural schema with nine required fields and four advisory categories, typed text/arrays and no evidence-dependent enums/bounds. Approved evidence is still in the prompt and exact evidence validation is unchanged. Removed duplicate schema text and clarified that the response must be a report matching the supplied schema. New schema measures 751 characters/four enum entries/no maxItems. No model switch, endpoint change, token-limit change, disabling of structured output or automatic retry. Added safe classification of bounded HTTP400 error metadata into fixed categories; unknown errors remain generic and no server strings/private field paths are displayed.

Ten new JUnit groups verify compact schema, evidence-size independence, actual serialized F08/F11/F12 adapter contract, preserved category/size/duplicate validation, and schema/response-config/generic/malformed server error privacy. Prior per-field evidence-tampering tests remain in the full suite. Normal Java21 build and `powershell -NoProfile -ExecutionPolicy Bypass -File .\Stage2\scripts\verify.ps1 -Gui` pass: 274 tests, zero failures, all seven GUI entry points. Tests use an explicitly empty process key and injected fictional transport. The old schema's combined constraints are a documented compatibility risk and working cause hypothesis; a fake provider is not proof of Google's historical rejection rule. Real Gemini acceptance remains for the user's explicit manual retest. No Git mutations or unrelated file edits.


## F08 age-evidence consistency correction — 2026-11-07

Read-only preflight: HEAD d67c52e (F12); tracked tree clean. Untracked .vscode/, Stage2/Project-Stage2-Instruction.pdf and root `tatus --short` were preserved. No remote verification or Git mutation. Read the Stage2 instruction PDF, original Stage1 F08/UC08/SD03/mapping/explanation and viewed original SD03; inspected production F08/F11/F12 flow and tests. Normal Java21 baseline build and JUnit succeeded: 274 passed.

Root cause: existing structural/category and required-evidence checks contained no model-year/age validation; risks/uncertainties were excluded from the older combined narrative phrase checks. Added a shared ModelYearContext, injectable Clock and captured referenceYear; all copies retain the snapshot. Added targeted all-section RiskAgeValidator checks and safe failure handling without echoing rejected content. F12 now shares the year description and requires matching year context for prior-risk reuse; schema/adapter/provider remain unchanged. No automatic retry or replacement of AI numbers.

Added 20 fixed-clock JUnit groups for arithmetic/labeling/prompt context, all four bad-age locations, correct-number-but-unsupported actual age, common spellings, inconsistent difference/year, allowed guidance/unrelated numbers, invalid context, controlled retry, old-success replacement, F11 accepted year updates, F12 valid/rejected/old-year reuse, snapshot consistency and CLI output. Strengthened the existing F12 age-prose test to require F08 rejection rather than accept the bad risk category. Extended actual RiskGuiSmoke with the bad Risks claim and successful explicit recovery. No tests disabled or assertions relaxed.

Offline before/after probe in ignored build/verification/f08-age-before-after used `git show HEAD:.../ResponseParser.java` (read-only) and compiled that single committed parser separately. The same fictional response through the agent produced: committed-parser AI_SUCCESS / unsupported claim accepted=true / cost=18707.00; corrected-parser AI_UNAVAILABLE / accepted=false / cost=18707.00. This demonstrates the semantic defect independently of provider nondeterminism.

Final verification from repository root with test-process GEMINI_API_KEY/GEMINI_MODEL empty: `powershell -NoProfile -ExecutionPolicy Bypass -File .\Stage2\scripts\verify.ps1 -Gui`. Java21 compilation/JAR packaging and 294 JUnit tests pass, zero failures; all seven GUI smoke entry points pass, including the new rejection/recovery path. Automated CLI tests verify exact evidence, safe reason and one call per explicit evaluate. No real Gemini request, credential inspection, Stage1/UML/root modification, commit or push. Natural-language coverage is targeted, not complete; no universal or live-success claim.


## Secure automatic Gemini enrollment and GUI cleanup — 2026-11-07

Read-only preflight: HEAD 0b25169 (F08 evidence correction), tracked tree clean. Preserved untracked .vscode/, Stage2/Project-Stage2-Instruction.pdf and root `tatus --short`. Inspected the launcher/build/verification scripts, entry point, GUI disposal, factory/adapter configuration and documentation. The Java factory already reads process variables and the GUI already displays configured-but-not-live-verified status; neither required modification. Baseline Java21 build and all 294 JUnit tests passed.

Added Windows DPAPI enrollment/removal/helpers, automatic launcher loading and synchronous GUI wait/finally cleanup. Explicit environment values and GEMINI_MODEL take precedence/persist unchanged; no implicit enrollment occurs. Saved ciphertext lives outside the repository with owner/SYSTEM-only ACLs. Added ignore rules as defense in depth. README covers initial setup, replace/delete confirmation, fallback, process/memory limits and the distinction between local configuration and live authentication. Its offline GUI example now invokes Java directly, because an empty environment followed by run.ps1 would otherwise reload enrolled credentials.

Added LauncherProbe and 17 offline PowerShell integration checks for ciphertext/location/ACLs, decryption and child inheritance, conversion memory release, missing/corrupt/unreadable fallback, explicit override, atomic replacement, failed enrollment preservation, deletion, normal and abnormal child exit, failed launch, demo/CLI compatibility, initial GUI status and actual Swing/Java/launcher termination. Each test uses a unique temporary store and random fictional credentials, with fixed output markers; the real user credential store is never read or changed. Tests exposed and corrected a Windows PowerShell Set-Acl replacement privilege issue using the filesystem ACL API. verify.ps1 runs these checks in a separate process, preserving the caller's environment.

Final command from Stage2: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Gui` (invoked with empty test-process key/model). Java21 compilation and normal JAR packaging succeeded; JUnit 4.13.2: 294 passed, zero failures; all seven Swing smoke entry points passed; Windows PowerShell 5.1: 17 launcher checks passed, zero failures. Additional `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-launcher.ps1 -SkipBuild` on PowerShell 7.6.9: 15 checks passed, zero failures (the two actual-window checks require -Gui). The tested Java GUI process and its waiting PowerShell launcher exited after CloseMainWindow; a post-finally marker confirmed the temporary environment was cleared. Failure/abnormal-exit tests also confirmed cleanup. This does not guarantee finally after a forced OS-level launcher kill or physical erasure of managed memory.

No real API calls, actual-user enrollment, credential inspection, application-source changes, Stage1/UML/root changes or Git mutations. Live Gemini authentication and feature success remain for explicit user acceptance. This increment has implementation and integration-test substance; course grading/meaningful-commit acceptance is not asserted.
