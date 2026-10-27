# Design decisions and source discrepancies

The Stage 1 sources were inspected before implementation. Changes below are explicit local foundation decisions, not edits to Stage 1 or assertions about a remotely submitted version. The Documents-folder report copies match the workspace PDF/DOCX by SHA-256, but no remote repository was consulted.

## D01 — Provider name conflict

**Original:** the actual class diagram XML and PNG name `DeepSeekAdapter`; the embedded class diagram on report p. 10 also shows that name. The report prose (pp. 3, 13, 16 and feature explanations), task DOCX documents and SD01/03/04/05/06 use `GeminiAdapter` and Gemini API.

**Decision:** retain `GeminiAdapter implements LLMClient` in Stage2, as explicitly required by the user and consistently specified by the written report and sequence diagrams. This is an acknowledged internal contradiction in the local final report, not a silently resolved naming change. Do not create a second DeepSeek adapter. No provider implementation or network call is included. Update a future Stage2 UML copy; leave originals untouched.

## D02 — Factory terminology and selection policy

**Original:** pp. 16–17 call the design “Factory Method / Factory Pattern”; the diagram lists one `EvaluationStrategyFactory.createStrategy(preference)` and three strategy products, with no creator subclass hierarchy. The creational lecture p. 20 describes subclass-controlled construction, while p. 21 also presents argument-based factory selection.

**Current:** no strategy factory is implemented or counted. `ApplicationFactory.create()` is dependency wiring only. No claim of a formal GoF Factory Method is made.

**Proposed correction for later implementation:** preserve `EvaluationStrategyFactory` as an abstract creator with the public `createStrategy(preference)` entry point and a protected factory method returning `EvaluationStrategy`. Concrete balanced/budget/reliability creators override product creation. Shared creator behavior should validate the requested priority and create the policy used by the agent. Select/inject a creator once at the composition/application boundary. This adds creator types and must be reflected in the Stage2 UML and report. Avoid adding hierarchy merely to obtain a pattern count; review the course interpretation with the instructor if the simpler existing factory is retained.

**Unresolved source gap:** `UserPreference` has reliability/fuel-economy priorities but no explicit budget-focus or evaluation-mode attribute. Do not infer budget priority merely because a buyer entered a maximum budget. Before implementing strategies, document an explicit priority-mode field or a justified selection rule; retain the three original strategy names.

## D03 — Strict records versus incomplete listing drafts

**Original:** Vehicle uses primitive year/mileage/price fields, but F02/F04 require missing/uncertain extracted numeric values. `parseVehicle()` returns Vehicle before user confirmation.

**Current:** the manual confirmed Vehicle requires valid year/make/model/mileage/price. Optional text histories become UNKNOWN; optional consumption stays null. No default numeric zero is used for absent listing data.

**Planned refinement:** introduce a distinct `VehicleDraft` with nullable values and provenance, or carefully refine Vehicle numeric types, before F02. Update parse/confirmation contracts and document the change. Do not force incomplete AI output into a valid confirmed record.

## D04 — Mutation, decimal values and cost inputs

**Original:** Vehicle exposes `updateField(String,Object): void`; money/consumption use double/Double; facade `estimateCost(vehicle,annualKm,fuelPrice)` omits optional cost assumptions mentioned by F06.

**Implemented:** immutable Java records are validated at construction. Editing creates a replacement preserving the current vehicle ID and commits only after successful preparation. Money and decimal inputs use BigDecimal to make cent rounding explicit. `CostAssumptions` carries annual distance, fuel price, optional fractional tax, maintenance and insurance. The original double-based facade overload remains as a validated convenience method; a new overload accepts full assumptions. `calculateAnnualFuelCost` uses BigDecimal parameters/return. `CostEstimate` fuel values can be null to represent unavailable results, with reasons.

**Impact:** F01/F06 method/type refinements are intentional; update Stage2 UML. No unsafe stringly typed mutation, guessed allowances or NaN/infinite arithmetic enters saved state. Tax is a supplied fraction 0–1, not a jurisdiction lookup. Manual year range is 1886 through next calendar year; priorities reserve 0–5 with 0 neutral. These ranges are implementation decisions because Stage1 provides no exact values.

## D05 — Command and coordination scope

**Original:** the four concrete commands represent AI listing/evaluation/comparison/checklist operations; SD02 shows direct facade-to-checker/tool calls even though the class diagram stores only agent/session fields on the facade.

**Implemented:** add `PrepareEvaluationCommand` for the combined deterministic preparation path and typed `Command<R>`. Keep the original four command names reserved for their actual AI/hybrid features. Facade delegates tool coordination through its existing agent collaborator; it does not acquire duplicate tool infrastructure. Added `PreparationResult` is a snapshot of deterministic inputs/results, not a renamed final report.

**Impact:** useful Command and Facade relationships now work in both interfaces. This is a documented SD02 routing refinement; no completed meaningful AI behavior is claimed.

## D06 — Session and feature granularity

**Original:** report describes local preference storage, up to two vehicles, clarification memory and saving reports; the session class does not specify report/evidence cache fields. SD06 says prior evidence is available without defining freshness/versioning.

**Current:** one current vehicle and one neutral-or-user-supplied profile live in process memory. F01/F03/F06/F07 are presented as a combined prepare operation; dedicated commands and persistence remain planned. F04 is a basic known-unknown checker; its empty uncertainty/conflict collections are not proof of full analysis.

**Future:** add explicit persistence and evidence version/invalidation design before final reporting. Provide storage/read methods rather than direct private-field access implied by the SD06 note. Expose full buyer preferences before claiming F03 complete. Budget/mileage only are checked now; transmission/history checks must return unknown/not-evaluable when evidence is absent.

## D07 — Clarification sequence signature

SD05 XML includes a conflict-resolution message `submitClarification(resolvedValue : String)` while the facade contract is `submitClarification(field : String, value : String)`. Preserve the field/value contract in the future implementation; the single-argument diagram message is insufficient to identify the field. SD05 positions question generation before the bounded answer loop, consistent with report p. 52: generate once, then process at most three questions. No clarification implementation is claimed now.

## D08 — Technology, dependencies and unavailable AI

Java 21, Maven conventions and Swing fit the single-user local design and avoid UI dependencies. Maven CLI is not installed/discoverable through PATH; offline PowerShell scripts compile/package with the existing JDK. JUnit 4.13.2 was already available in VS Code's Java extension and matches the lecture style; no libraries were downloaded. A dependency-free runner reuses the same checks when those optional jars are absent.

Only essential operational classes were created. Repository, strategies, prompts/parsers, four AI commands and final result types are documented rather than filled with empty methods. GeminiAdapter is the one deliberately unavailable provider boundary: it throws a clear PLANNED exception, never returns a fake model answer, and reads no credentials.

`.env.example` describes future configuration; the current application does not load it. `.gitignore` excludes local secrets and all build outputs. Scripts resolve their own Stage2 root and never run Git operations. The workspace project is not assumed to be the existing Documents-folder Git checkout; no repository was created or copied.

## D09 — Tool ownership notation

The class diagram's ToolManager-to-Tool relation uses `lt=<<<-` (filled diamond) with 1 to 0..* multiplicity, while report p. 15 describes aggregation. The current registration API stores references to injected tools and does not enforce exclusive lifecycle ownership. Treat this implementation as a managed collection/association; clarify aggregation versus composition in the future Stage2 diagram instead of claiming that Java references alone enforce UML composition.
