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
