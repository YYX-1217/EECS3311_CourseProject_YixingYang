# AI Used-Car Evaluation Assistant — Stage 2 Foundation

## Project overview

This project is the **initial Stage 2 foundation** for the EECS3311 Fall 2026 course project. It supports a working deterministic workflow through a command-line interface (CLI) and a Swing graphical user interface (GUI): vehicle entry, session-based buyer preferences, ownership-cost estimation, data-quality checks, and buyer constraint checks. F02 listing analysis, F08 risk assessment, F09 two-vehicle comparison, F10 inspection checklists, F11 clarification/follow-up and F12 personalized recommendation reports also have working shared GUI/CLI and Gemini integration paths verified with offline fixtures. It is not a fully implemented Stage 2 application; real-model F09 behavior remains unverified.

The `Stage2` directory is part of the existing course-project repository. The original `Project-Stage2-Instruction.pdf` is retained. The foundation was developed without modifying the Stage 1 materials, repository-root README, or Git configuration. No particular checkout location or personal Windows directory is required.

## Prerequisites

- Windows PowerShell 5.1 or PowerShell 7 and a **Java 21 JDK**, rather than only a JRE.
- A Windows desktop display for the GUI.
- No Maven installation or API key is required for deterministic use. The first script build downloads Gson 2.13.2 from Maven Central; subsequent builds use its checksum-verified cache in build/dependencies. Runtime JSON classes are packaged in the executable JAR.
- The verification script prefers JUnit 4.13.2 and Hamcrest from an existing VS Code Java extension installation. If those libraries are unavailable, it explicitly uses the dependency-free verification runner.

The scripts check the current process's `STAGE2_JAVA_HOME`, then `JAVA_HOME`, `javac` on `PATH`, and common JDK installation directories to locate Java 21. They do not modify system-wide environment settings. During the initial scaffolding verification, the default `java` on `PATH` was Java 8; the scripts successfully located an installed Adoptium Java 21 JDK instead.

## Project directory structure

```text
Stage2/
  pom.xml, README.md, .gitignore, .env.example
  Project-Stage2-Instruction.pdf          Original course instruction file
  src/main/java/ca/yorku/eecs3311/usedcar/
    app/ agent/ command/ domain/ llm/ result/ strategy/ tool/ ui/
  src/main/resources/example.properties
  src/test/java/ca/yorku/eecs3311/usedcar/
  scripts/common.ps1, build.ps1, run.ps1, verify.ps1
  docs/                                 Architecture, traceability, status, logs, and next steps
  build/                                Ignored build, verification, and inspection artifacts
```

The application entry point is `ca.yorku.eecs3311.usedcar.app.Main`. It accepts `--demo`, `--cli`, `--gui`, and `--help`.

## Build instructions

Open the repository's `Stage2` folder in VS Code and open a terminal in that folder. **All commands below assume that the current working directory is `Stage2`.** If VS Code is opened at the repository root, navigate into its `Stage2` subdirectory before running them.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

The script compiles the application with Java 21 and creates `build/used-car-assistant.jar`. `-ExecutionPolicy Bypass` applies only to the newly launched PowerShell process; it does not change the machine's execution policy. In a terminal that already permits script execution, the equivalent command is:

```powershell
& .\scripts\build.ps1
```

The scripts resolve their own Stage2 directory and do not depend on a fixed drive letter or checkout path. In PowerShell 7, they can be invoked directly as shown above; the longer commands explicitly launch Windows PowerShell.

After changing source files, run `build.ps1` again. `run.ps1` builds automatically only when the JAR is absent; it does not detect source changes.

If the scripts cannot locate an existing Java 21 JDK, the following commands prompt for its installation directory and set the override only for the current terminal session:

```powershell
$env:STAGE2_JAVA_HOME = Read-Host 'Enter the installation directory of your Java 21 JDK'
& .\scripts\build.ps1
```

## CLI demo and interactive mode

Run the deterministic demonstration with explicitly labeled fictional input:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Mode demo
```

Start the interactive CLI:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Mode cli
```

Within the interactive CLI, use the following commands:

| Command | Behavior |
|---|---|
| `analyze-listing` | Paste seller text, terminate with `:end`, review extracted claims, then edit/confirm/cancel. Only a final `yes` saves the record. |
| `evaluate` | Assess the saved vehicle using current preferences, local tools, available matching cost results and strategy guidance; explicitly invokes Gemini only when essential evidence is sufficient. |
| `prepare` | Prompt for vehicle information, buyer limits, and cost assumptions; replace the current vehicle after successful preparation. |
| `check-conflicts` | Enter a second structured record for the saved vehicle; display source differences without saving or merging values. No listing extraction is performed. |
| `:cancel` | Cancel data entry at a `prepare` or `check-conflicts` field prompt. |
| `demo` | Execute the deterministic workflow with fictional example input. |
| `checklist` | Generate a new current-vehicle checklist; may send one Gemini request, with deterministic fallback. Resets completion for the new generation. |
| `checklist-show` | Redisplay the current checklist and completion state locally, without another AI request. |
| `checklist-mark <id>`, `checklist-unmark <id>` | Mark/unmark a displayed item ID locally; no AI call. |
| `comparison-set` | Enter/edit separate Vehicle B using existing validation; `:keep` retains a B field, `:cancel` cancels, and final `yes` saves without replacing A or preferences. |
| `comparison-show` | Display saved A/B IDs, records and shared cost assumptions. |
| `compare` | Compare saved A and B using symmetric tools; send at most one Gemini request when essential evidence is sufficient. |
| `show` | Display the saved current vehicle and buyer preferences. |
| `profile-set` | Enter or update all seven buyer preferences independently of vehicle entry. Blank clears a field to neutral; `:keep` retains its saved value and `:cancel` cancels the update. |
| `profile-show` | Display the saved buyer preference profile. |
| `knowledge` | Retrieve local CSV knowledge for the currently saved vehicle; report explicitly when no matching record exists. |
| `strategy auto` | Select automatically from saved buyer preferences and preview the selected policy and selection basis; subsequent previews reselect using current saved preferences. |
| `strategy balanced`, `strategy budget`, `strategy reliability` | Select and preview an evaluation-priority policy using saved preferences. |
| `strategy` | Preview the active mode with the latest saved preferences; automatic mode reselects its policy. Requires explicit selection first. |
| `llm-test` | Explicitly send one harmless connection-test prompt; display genuine returned text and updated provider status. May consume API quota. |
| `status` | Display the current LLM integration status. |
| `help` | List available commands. |
| `quit` | Exit the application. |

All session information is held in memory and is discarded when the application closes.

After building, the JAR can also be launched directly with **Java 21**:

```powershell
java -jar .\build\used-car-assistant.jar --demo
```

This direct command uses the `java` executable on `PATH`. Use the launch script if `PATH` selects an older Java version.

## GUI launch and use

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Mode gui
```

Select **Load fictional example**, review the **Vehicle**, **Preferences**, and **Cost assumptions** tabs, and then select **Save and calculate**. Alternatively, enter your own data. The results area supports scrolling and displays validation errors. After changing input, select **Save and calculate** again to obtain updated results.

To check source differences, first save the manual vehicle with **Save and calculate**. Enter the second structured record in the **Vehicle** tab, then select **Check against saved vehicle**. This action uses only the Vehicle tab and leaves the saved record unchanged. Do not select Save and calculate before comparing, because that replaces the baseline. Review both reported values and explicitly edit/save your chosen record; automatic resolution is not implemented.

Select **View local knowledge** to retrieve notes for the saved vehicle in the existing results area. Unsaved form edits are not used; save the vehicle first. Both matching results and the explicit no-data result are displayed.

The GUI and CLI use the same underlying application workflow. Neither interface currently provides all planned Stage 2 features.

## Example output and calculation conventions

The example input is stored in `src/main/resources/example.properties`. Its vehicle make and model are fictional. The tax rate, fuel price, insurance allowance, and maintenance allowance are demonstration assumptions, not current market quotations or applicable tax rules.

- Annual fuel use: 15,000 × 7.2 ÷ 100 = **1,080 L**.
- Annual fuel cost: 1,080 × 1.65 = **CAD 1,782.00**.
- Purchase tax: 18,000 × 0.13 = **CAD 2,340.00**.
- Simple first-year estimate: 18,000 + 2,340 + 1,782 + 1,000 + 1,800 = **CAD 24,922.00**.
- The asking price of CAD 18,000 exceeds the buyer's CAD 17,000 budget, producing an explicit budget conflict.

Calculations use `BigDecimal`. Monetary components are rounded to cents using `HALF_UP`, and the first-year estimate sums the rounded components. If fuel consumption is unknown, fuel results are unavailable. If any tax, maintenance, or insurance assumption is missing, a complete first-year total is not produced. An explicit zero is distinct from a blank field. Financing, depreciation, registration, and other unlisted costs are excluded.

## JUnit and local verification

Run the verification script from Stage2:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1
```

When the expected JUnit 4.13.2 and Hamcrest libraries are available in the installed VS Code Java extension, the script runs `FoundationTest` through JUnit. Otherwise, it reports that it is using the dependency-free runner; that fallback is not presented as a JUnit execution.

In a terminal that permits script execution, the available verification modes are:

```powershell
& .\scripts\verify.ps1                  # Prefer JUnit; explicitly fall back if unavailable
& .\scripts\verify.ps1 -DependencyFree  # Force dependency-free verification
& .\scripts\verify.ps1 -Gui             # Also open a real GUI, test its buttons, and close it
```

The 274 tests cover negative and malformed inputs, required fields and year validation, exact cost calculations, optional values, unknown fuel consumption, constraint boundaries, zero values and rounding, session preservation after rejected updates, tool/LLM boundaries, and interactive CLI processing, including transmission matching/mismatching, multiple accepted types, accident tolerance, neutral preferences, unknown values, and invalid preference input.

Ten F04 tests additionally cover missing versus unknown histories, complete and insufficient essential data, optional omissions, matching and conflicting sources, unknown values without false conflicts, session preservation, and the interactive conflict-check command.

The GUI smoke check verifies window launch, calculation, error display, recalculation, and source conflict display without overwriting the saved vehicle. It saves an image of the application's panel to `build/verification/gui.png`. These checks do not constitute complete Stage 3 testing, and KUMA validation has not been performed.

## Optional Maven workflow

`pom.xml` specifies Java 21, JUnit 4.13.2, and an executable JAR. Maven CLI was unavailable during initial scaffolding verification, so the Maven build was not verified at that stage. If Maven is already installed, configure it to use Java 21 and keep its dependency cache within Stage2:

```powershell
mvn "-Dmaven.repo.local=$PWD\build\maven-repository" test package
java -jar .\target\used-car-assistant-0.1.0-SNAPSHOT.jar --demo
```

An initial Maven build may download the declared plugins and test dependencies. No such downloads or Maven installation were performed during initial scaffolding. Maven's `test` phase runs `FoundationTest`; the GUI smoke check is an explicit local verification step and is not automatically included in the Maven test phase.

## F02 listing analysis — UC02 / SD01

F02 now runs through both interfaces: `AnalyzeListingCommand` -> facade -> agent -> `PromptBuilder` -> existing `LLMClient`/`GeminiAdapter` -> `ResponseParser` -> `DataQualityChecker`. It returns an unconfirmed `VehicleDraft` with original source text, field quotations, units, missing/uncertain values and conflicts. The session is unchanged until explicit user confirmation of a valid corrected Vehicle.

**CLI:** run `analyze-listing`, paste up to 16000 characters, and enter `:end` on a separate line. `:cancel` or end-of-input cancels before analysis. Submitting the listing makes one real Gemini request when configured. Review all extracted claims and data-quality output. Enter `edit` to correct fields (`:keep` retains a draft value; blank clears it; `:cancel` cancels), `confirm` to review without editing, or `cancel` to discard the draft. The final preview shows corrected values and conflicts with the saved record. Only `yes` at the explicit replacement prompt saves. Any other answer leaves the draft unsaved; input errors allow correction without another API call. To retain a manual value in a conflict, enter that value during editing or cancel to retain the entire saved record.

**GUI:** open **Listing Analyzer**, paste text and click **Analyze Listing**. The network operation runs off the Swing event thread. Results and editable vehicle fields appear in that tab alongside the original listing. Review/correct them, click **Confirm and Save**, then approve the final values/conflicts dialog. Declining leaves the draft unsaved. **Cancel** discards the draft or pending result without changing vehicle/preferences; cancellation cannot guarantee reversal of a request already received by the provider or its quota usage. No request runs on GUI startup. If the saved vehicle changes while analysis/review is underway, confirmation is rejected; reanalyze against the new baseline.

**Structured output and diagnostics:** F02 now requests `application/json` with `responseJsonSchema` through a provider-independent `LLMClient.generateStructured(prompt, schema)` extension. Ordinary `generate(prompt)` and `llm-test` remain text operations. `ListingExtractionSchema` defines nullable properties for the prompt, Gemini request and parser key checks, following the [documented generateContent structured-output capability](https://ai.google.dev/gemini-api/docs/generate-content/structured-output). Configured models must support this capability; there is no automatic paid retry or fallback. Schema-constrained output still requires Java validation and user review. Optional data is represented by explicit null, not omitted properties or invented defaults.

Validation errors now identify a trusted field path and reason, such as `root.fuelType: required property missing`, `mileage: expected JSON number or null`, or `evidence.price: non-null extracted value requires a source quotation`. Errors never echo raw model values, source quotations, arbitrary property names or parser exception details. Numeric values exclude grouping separators, while evidence preserves the source wording and punctuation exactly. Unknown accident history remains null/uncertain; a statement that no maintenance records are available means NONE, never DOCUMENTED.

The reported Corolla live-test failure could not be attributed to a specific response field because the original raw response was unavailable and the old diagnostic discarded that detail. Confirmed defects were prompt-only JSON enforcement and undifferentiated validation errors. Offline regressions now cover this advertisement, nullable optional fields, missing evidence, wrong types, exact quote checks and the structured HTTP payload. **No new live F02 request was made to verify this fix.**

**Extraction contract:** strict JSON object containing all twelve vehicle fields (`year`, `make`, `model`, `trim`, `mileage`, `price`, `transmission`, `fuelType`, `accident`, `maintenance`, `consumption`, `notes`), plus `mileageUnit`, `currency`, `consumptionUnit`, `uncertainFields`, and `evidence`. Year/mileage are integers, price/consumption are nonnegative numbers, and other values/units are strings; absent values must be JSON null. Every vehicle field has an evidence entry with an exact source quotation or null. Non-null values require source evidence. `uncertainFields` lists ambiguous/contradictory vehicle fields. Missing properties, extra properties, wrong types, invalid numeric ranges, arbitrary prose wrappers and quotations not present in the source are rejected. A single complete Markdown JSON code fence is tolerated, but its content undergoes the same strict checks. Numeric precision is limited to 18 significant digits and scale magnitude 6; confirmed form values also obey existing form/domain limits.

The prompt treats advertisements as untrusted data and forbids following embedded instructions or inventing information. Deterministic validation checks schema and source quotations; it cannot prove that an LLM interpreted the quotation correctly or that seller claims are true. Human review is mandatory. Ambiguous fields and quantities without explicit compatible units are not prefilled into the save form. Only km, CAD and L/100km quantities can prefill canonical numeric fields; miles, USD, mpg or unknown units remain visible in the draft and require explicit user correction. A bare dollar sign does not imply CAD. No currency/unit conversion is performed. Missing required values cannot be saved until supplied; optional unknown histories remain missing/uncertain. F04 reuses its existing known/unknown semantics and comparison rules, including both manual and extracted values for comparable conflicts.

The confirmed vehicle retains source text, extraction claims and uncertainty in its notes, labeled as original unverified extraction; corrected current fields take precedence. No preference, cost formula or F07 rule changes. Drafts and saved vehicles remain in memory. There is no site scraping, file-import command, VIN lookup, independent seller verification, risk rating or recommendation. Malformed model output/API failure produces an error and requires an explicit user retry; no fake fallback and no automatic paid retry occurs. The provider output cap is now 4096 tokens to accommodate structured fields and quotations; truncated responses remain errors.

Verification: 16 new JUnit groups cover prompt constraints, complete/partial extraction, missing histories, normalization, invalid numbers/JSON/types/evidence, units, uncertainty, no-auto-save, approved save, cancellation, conflicts/stale baselines, missing configuration and CLI edit/confirm. `verify.ps1 -Gui` additionally runs a fixture-backed Swing test for background analysis, responsive UI, correction, confirmation and cancellation. The current suite contains 274 passing JUnit tests, including F09/F10/F11/F12 regression groups. The user reports a prior successful live connection test; **no live F02 extraction was performed during this increment**, so real-model extraction quality remains unverified.

## F03 buyer preference management

The CLI `profile-set` / `profile-show` commands and the GUI **Preferences** tab support all seven Stage 1 fields: maximum budget, maximum mileage, accepted transmissions, preferred body type, reliability priority, fuel-economy priority, and accident tolerance. **Save preferences** validates and saves only preference fields, even if no vehicle exists or vehicle/cost inputs are incomplete. **Load saved preferences** reloads and displays the saved profile, discarding unsaved preference edits. Existing profiles populate the GUI when it opens.

Both interfaces use the shared form parser, validated immutable `UserPreference`, and existing facade/session operations (`savePreferences`, `setPreference`, and `getPreference`), following UC03 and SD02. Parsing and validation complete before saving. Rejected input, cancellation, or CLI end-of-input preserves the valid profile; users can correct the fields and retry. Saving preferences leaves the current vehicle unchanged. Subsequent facade constraint checks use the updated profile and the existing F07 rules.

Blank budget/mileage limits remain unspecified; explicit zero is a real limit. Negative values, malformed numbers, unsupported transmissions, and unsupported accident tolerances are rejected using existing rules. Transmission aliases and accident-tolerance conventions are unchanged. Preferred body type is optional free text with surrounding whitespace removed; no body-style taxonomy was specified in Stage 1. Stage 1 specifies integer priorities without a numeric range, so this increment preserves the existing convention: **0 = neutral; 1–5 = increasing importance**. Blank priority input maps to zero; fractions and values outside 0–5 are rejected. No preference is inferred from vehicle data.

The combined `prepare` / **Save and calculate** workflow still explicitly submits and saves the preference fields together with vehicle and cost input; it now includes all seven fields. The fictional example loads sample preferences into the form, and executing `demo` saves those sample values. Use the independent profile operations when only preferences should change.

Profiles remain in memory for the current session. Body type and priorities are stored and displayed; they do not introduce new F07 rules, scoring, or AI recommendations. The Strategy/Factory preview described below now also uses the saved profile. Twelve additional automated tests cover complete/neutral profiles, validation, updates, preservation, retrieval, CLI behavior, and F07 compatibility. GUI smoke verification also exercises independent save, rejection, reload, and vehicle preservation.

## F04 deterministic data-quality checks

`DataQualityChecker.check(vehicle)` runs through the existing agent/facade preparation workflow before cost and constraint checks, following UC04 and SD02. It returns the existing `MissingInformationResult` fields: `missingFields`, `uncertainFields`, `conflicts`, and `sufficient`. Both interfaces display all four fields.

- Blank or null optional vehicle text is **missing**; explicit `UNKNOWN` (also `UNCERTAIN`, `UNSURE`, `N/A`, `NOT KNOWN`, or `NOT PROVIDED`) is **uncertain**. Null fuel consumption is missing, never zero.
- Accident history uses `YES` or `NO`. Existing aliases `ACCIDENT_REPORTED`/`HAS_ACCIDENTS` and `NONE`/`NO_ACCIDENTS`/`ACCIDENT_FREE` are accepted. Maintenance history uses `DOCUMENTED`, `PARTIAL`, or `NONE`. Other history text remains uncertain; seller prose is not interpreted. These values are user assertions, not independently verified evidence.
- Essential fields for this increment are year, make, model, mileage, asking price, accident history, and maintenance history. Missing or uncertain essential information makes `sufficient` false. Known adverse history still counts as available data; sufficiency does not imply safety or suitability. Trim, transmission, fuel type, and fuel consumption are reported but do not determine essential-data sufficiency. Missing fuel consumption still prevents fuel-cost calculation.
- The existing `Vehicle` constructor rejects absent make/model, invalid year, absent/negative price, and negative mileage; the form also rejects missing year/mileage. Incomplete core-field drafts therefore remain unsupported. Explicit unknown make/model labels are reported as uncertain rather than accepted as sufficient identity data.
- `checkConflict(manualVehicle, extractedVehicle)` implements the Stage 1 structured-source comparison. It compares year, make, model, trim, mileage, asking price, transmission, fuel type, accident history, maintenance history, and fuel consumption. Text comparison ignores case and surrounding whitespace; existing transmission and accident aliases are normalized, and decimal scale does not create conflicts. IDs and free-text notes are not compared. Unsupported transmissions remain uncertain.
- Only two known comparable values can conflict. Each conflict retains the field and both supplied values. Missing/uncertain findings identify their source. Combined sufficiency requires both records to have sufficient essential data and no unresolved source conflicts. Neither source is merged or overwritten. The facade compares the saved manual record with the submitted second record; CLI and GUI share this operation.

This is a deterministic F04 increment, not complete F04 implementation. The second record is entered manually to represent structured extraction output. F02 now supplies real LLM extraction and draft review through the separate listing workflow. F11 now provides prioritized clarification and explicit conflict resolution for supported saved-vehicle fields, including F02 ambiguity/conflict targets. Broader free-text ambiguity interpretation remains outside that bounded workflow. A source comparison concerns two descriptions of the same vehicle; it does not implement F09 comparison of competing vehicles. No risk rating or purchase recommendation is generated.

## F05 local vehicle knowledge retrieval

`knowledge` in the CLI and **View local knowledge** in the GUI follow the Stage 1 UC05/SD03 call chain: facade → agent → existing ToolManager (`vehicleKnowledge`) → VehicleKnowledgeTool → VehicleKnowledgeRepository. The result retains the Stage 1 `summary`, `commonIssues`, `inspectionNotes`, and `sourceLabel` fields. Retrieval does not modify the saved vehicle.

The UTF-8 dataset `src/main/resources/vehicle-knowledge.csv` contains three explicitly fictional instructional records: two non-overlapping year ranges for Example Motors / Sample Sedan and one for Demo Works / Practice Hatch. The existing fictional 2020 demo matches the first record. These notes are sample inputs for demonstrating retrieval, not claims about real vehicle reliability. The build packages the CSV in the JAR, so lookup does not depend on the working directory. Rebuild after editing the bundled data.

The restricted CSV schema is `make,model,yearFrom,yearTo,summary,commonIssues,inspectionNotes,sourceLabel`. The header is exact. Each nonblank record occupies one line with eight comma-separated columns; quoting, embedded commas, multiline fields, and comments are unsupported and rejected. The two list columns use `|` separators and may be empty; empty items within a nonempty list are rejected. Other columns are required. Years are inclusive integers from 1886 through 9999 with start ≤ end. Matching ignores case and surrounding whitespace. Overlapping ranges for the same normalized make/model are rejected rather than selected arbitrarily.

The repository loads lazily on first lookup, caches validated records, and exposes `load()` for explicit reloads. It also accepts a local `Path` for tests or programmatic use. Missing/unreadable files and malformed records produce explicit retrieval errors; malformed-record errors include a line number. A failed reload cannot silently serve old records. No matching make/model/year returns **No local vehicle knowledge available.** with empty issue and inspection lists. Other deterministic operations remain available if knowledge retrieval fails.

Ten F05 tests cover exact matching, case and whitespace normalization, inclusive year boundaries, no-data results, missing files, malformed CSV, tool dispatch, facade integration, and the CLI command. GUI smoke verification exercises both a matching record and no-data output.

This increment provides local deterministic retrieval only. There is no live database, fuzzy matching, dataset editor, or external API call. F08 and F09 retrieve this knowledge automatically for assessment/comparison; F10 now provides inspection checklists; recommendations remain planned.

## Evaluation-priority Strategy preview

The Preferences tab includes a mode selector (`auto`, `balanced`, `budget`, `reliability`) and **Preview priority guidance**. Save preference edits first: previews use the session profile, not unsaved form fields. The CLI offers the equivalent `strategy <mode>` operation and `strategy` to repeat the preview. No vehicle is required. Selection persists in the current agent context until changed or the application closes; a new context has no implicit strategy. Invalid selection preserves the previous strategy.

- **Balanced:** discusses budget, reliability, mileage, vehicle risk, and ownership-cost trade-offs. Relative reliability/fuel-economy emphasis follows their explicit nonzero priority values; an unspecified priority is not interpreted as a lower ranking.
- **Budget Focused:** discusses purchase price, the supplied budget, and ownership costs first. An absent budget remains unspecified; missing cost assumptions are not zero. Low cost never overrides risk warnings or explicit constraints.
- **Reliability Focused:** discusses reliability evidence, maintenance uncertainty, inspection concerns, and risks before cost advantages. Numeric importance remains a buyer preference, not a vehicle-condition score.

All modes retain explicit mileage, transmission, body-type and accident preferences, preserve neutral values, and prohibit invented history or risk ratings. Guidance is deterministic instruction text, not a completed AI assessment, inspection checklist, or purchase recommendation. F07 decisions are unchanged.

The implementation preserves Stage 1 `EvaluationStrategy.buildPriorityGuidance(UserPreference): String`, the three named concrete strategies, and `UsedCarAgent.setStrategy(EvaluationStrategy)`. The agent holds the interface and dispatches polymorphically. A package-private `PriorityGuidance` helper shares preference rendering and safeguards. `EvaluationStrategyFactory.createStrategy(UserPreference)` now centralizes automatic construction. The agent obtains the product and invokes its guidance through `EvaluationStrategy`; CLI and GUI use the same facade. Manual construction also resides in the factory. This preview implements the strategy-selection portion of SD03/SD06; F08 now uses this strategy-selection chain; F12 is implemented below with live verification outstanding.

### Automatic selection and manual override

Run `strategy auto` in the interactive CLI, or choose `auto` in Preferences and select **Preview priority guidance**. Output identifies automatic mode, both priority values, the selection basis, the selected strategy, and its real guidance. Save preferences before previewing. `strategy` repeats the active mode. The factory is called again on every automatic preview, so saving a changed profile updates the next selection without changing the saved vehicle.

| Saved preference basis | Automatic strategy |
|---|---|
| Both priorities are 1–5 and reliability is strictly greater than fuel economy | Reliability Focused |
| Null profile, either priority is 0 (unspecified), equal priorities, or fuel economy is greater | Balanced |

The factory treats a null profile as neutral for selection; the automatic agent path supplies a neutral profile to the existing non-null Strategy interface. Other optional profile fields retain their existing semantics. In particular, an absent budget is unspecified and a zero budget remains an explicit constraint.

The profile has no budget-importance field. A maximum budget constrains affordability, and fuel economy can matter for reasons other than purchase price or total cost. Neither is sufficient to infer budget-first intent, even in combination. Consequently automatic mode does not select Budget Focused in this version. Use `strategy budget` (or GUI `budget`) to express that explicit choice; manual `balanced` and `reliability` also remain available. A manual choice persists across profile updates until another mode is explicitly selected. Selecting `auto` resumes inference. Invalid mode input leaves the active mode unchanged.

This is a **Simple Factory**, consistent with the single factory class and argument-based selection in the Stage 1 UML and the creational lecture's argument-based example. It is not the formal GoF Factory Method, which delegates construction to creator subclasses. No artificial creator hierarchy or completed formal Factory Method is claimed. See D02 in [Design Decisions](docs/DESIGN_DECISIONS.md) for the source gap, conservative rule, and impact on implemented F08 and planned F12 integration.

Ten additional factory JUnit tests cover null/neutral/tied profiles, all 36 priority combinations, actual reliability guidance, unsupported budget inference, manual products, agent interface dispatch, override transitions, saved-profile updates, invalid selection preservation, and CLI integration. GUI smoke checks exercise automatic selection, unsaved versus saved edits, shared facade output, and manual override. Targeted checks can also be run after verification using the Java 21 executable (this command is a dependency-free targeted check, not a separate JUnit run):

```powershell
& .\scripts\verify.ps1 -Gui
. .\scripts\common.ps1
& (Join-Path (Find-Stage2Jdk) 'bin\java.exe') -cp 'build\classes;build\test-classes' ca.yorku.eecs3311.usedcar.FactoryChecks
```

Ten additional JUnit tests verify policy differences beyond labels, preference-sensitive behavior, neutrality, safety constraints, injected agent dispatch, runtime replacement, session preservation, F07 compatibility and CLI operation. Swing smoke verification covers all modes and fresh guidance after saving preferences.

## F08 AI-assisted risk assessment — UC08 / SD03

**CLI:** save/confirm a vehicle using `prepare` or F02, save buyer preferences, optionally choose `strategy auto`, `strategy budget`, `strategy balanced` or `strategy reliability`, then run `evaluate`. **GUI:** save a vehicle and preferences, then open **Risk Assessment** and click **Evaluate Vehicle**. Unsaved form edits are not evaluated. The GUI runs the workflow in a SwingWorker and restores the button after success/failure. Both boundaries invoke `EvaluateVehicleCommand` and the same facade/agent. No request occurs on startup. A missing saved vehicle produces an actionable error.

The agent runs F04 data-quality checks, calls `vehicleKnowledge` and `constraints` through the existing ToolManager, adds a previously calculated F06 cost result only when it still belongs to the saved vehicle, and obtains buyer guidance through the existing Strategy interface. Automatic mode uses EvaluationStrategyFactory; explicit manual choices override it. If no strategy was selected, F08 uses factory inference for that assessment without changing the existing explicit-preview selection behavior. Missing preferences at the agent boundary use neutral values; UI sessions already have a neutral default profile. Preparing or explicitly calculating costs stores the real result; changing the vehicle invalidates that cache. No missing assumptions or cost components are invented.

`RiskAssessment` retains Stage1's five fields: `riskLevel`, `strengths`, `risks`, `uncertainties`, `explanation`. Categories are **Low**, **Moderate**, **High**, and **Insufficient Information**. These are advisory assessments, not safety guarantees, reliability scores or purchase recommendations. The separate `RiskEvaluation` envelope contains an immutable `RiskEvidence` snapshot, status and diagnostic, allowing both interfaces to show authoritative tool results even if no AI assessment exists.

- **INSUFFICIENT_INFORMATION:** F04 essential evidence is missing/uncertain, or the mandatory constraint tool failed. Java returns a clearly labeled Insufficient Information result without calling Gemini. The fictional demonstration may take this path; provide actual history information rather than inventing it to obtain an AI category.
- **AI_SUCCESS:** one structured Gemini request was parsed and passed the implemented evidence checks. This is interpretation of supplied evidence, not proof of its truth.
- **AI_UNAVAILABLE:** configuration/provider failure or rejected output. No risk category is fabricated; collected data-quality, knowledge, constraint and cost evidence remains visible. Check `status`, correct configuration/input, then retry explicitly. No automatic paid retries occur.
- **STALE:** saved vehicle, preferences, costs or selected strategy changed during the request. The old evidence snapshot remains visible but its AI category is withheld and not stored as the current assessment. Evaluate again against current data.

Missing local knowledge is explicitly labeled; optional knowledge-tool failure does not erase other evidence. Instructional CSV notes are always labeled as such, not real-world reliability research. Confirmed structured inputs and seller notes are still unverified user/seller claims. Gemini receives those sources as data with instructions to ignore embedded commands. Constraint conflicts must appear verbatim in the response risks, and missing information, tool failures, unavailable costs and source limitations must appear verbatim in uncertainties. Java rejects altered/omitted required entries, unsupported categories/types and Low with unresolved buyer constraint conflicts. Additional conservative phrase checks reject obvious accident/maintenance/constraint contradictions. Costs and violations are displayed separately from AI prose and cannot be replaced by model fields. These checks cannot establish the semantic truth of all generated prose; users must review the assessment against the evidence.

The original `assessRisk(vehicle, preference)`, `evaluateVehicle(vehicle)`, `buildRiskPrompt(vehicle, knowledge, missing, violations)` and `parseRiskAssessment(response)` contracts are retained. The current-session command uses additional envelope/context methods for costs, safe errors and freshness. D12 records these extensions and the explicit no-retry decision. Successful/current results are held only in memory and invalidated when inputs change; F09 is documented below; F10 is implemented below; F11 is implemented below; F12 is implemented below with live verification outstanding.

Verification: 20 new JUnit groups plus all 106 earlier tests pass (**126 at the F08 increment; 150 after F09**). Tests cover schema/category errors, no vehicle, sufficiency gate, knowledge/no-match, tool sequencing, constraints, neutral/saved preferences, automatic/manual strategy, cost reuse/invalidation, rejected/provider responses and shared CLI behavior. `verify.ps1 -Gui` runs the existing GUI/F02 checks and RiskGuiSmoke, exercising background evaluation, responsive EDT, stale-context handling, success and failure with retained evidence. **No live F08 Gemini request was performed; real-model end-to-end behavior remains unverified.** Run `evaluate` manually with local credentials only when ready to authorize the request.

## F09 two-vehicle comparison — UC09 / SD04

Save Vehicle A through `prepare` (or confirmed F02 analysis), then use the following interactive CLI commands:

```text
comparison-set
comparison-show
compare
```

`comparison-set` asks only for B's vehicle fields in the same order and units as the existing Vehicle form. It does not ask you to re-enter A, preferences or costs. Enter `:keep` to retain a saved B field, leave an optional field blank to clear it, or enter `:cancel` to abort. After validation, type `yes` to save B; other input cancels. Invalid input or cancellation preserves both previous records. Editing B retains its own ID; comparison rejects equal A/B IDs. `comparison-show` displays the saved pair and common cost assumptions. `compare` resolves these two slots rather than requiring ID arguments; the original two-Vehicle facade/agent contracts remain available.

In the GUI, save A through the existing workflow, open **Comparison**, enter B, and click **Save Vehicle B** followed by **Compare saved vehicles**. The tab shows saved A/B summaries, a separate B form and comparison output. **Cancel edits / reload saved B** discards unsaved B edits. **Refresh saved summaries** updates the displayed selection. Unsaved form changes are never compared. The request runs in a SwingWorker; the button recovers after success or failure. A change to saved vehicles, preferences, costs or strategy during the request yields `STALE`, retains the old deterministic snapshot and withholds the old AI narrative. Compare again for current results.

The agent uses the existing F04 checks and ToolManager operations for **both** vehicles: CostCalculatorTool when common assumptions exist, VehicleKnowledgeTool and ConstraintCheckerTool. It applies the same saved preferences and priority guidance. No per-vehicle AI risk request is required. The comparison includes year/make/model, price and mileage (with signed A-minus-B differences), transmission, fuel type, history claims, fuel consumption, cost components, buyer conflicts, missing information and knowledge availability. Java computes the numbers; Gemini supplies only the advisory trade-off interpretation and an optional supported preference.

Successful preparation or explicit estimation for current A now retains its `CostAssumptions` in EvaluationSession. F09 recomputes both estimates using the same annual kilometres, CAD/L fuel price, tax fraction, maintenance allowance and insurance allowance. Vehicle changes retain this shared scenario but invalidate A's old cost result. Equal allowances are user assumptions, not vehicle-specific quotations. Missing assumptions/components remain unavailable, never zero or invented. No old A estimate is compared with a newly calculated B estimate. The existing model supports **CAD, km and L/100 km only**: incompatible unit/currency text is rejected by the form; no conversion occurs. Users must supply correctly labeled values; the application cannot infer the true unit of an incorrectly labeled bare number.

`VehicleComparison` preserves the five Stage1 fields: `firstVehicleId`, `secondVehicleId`, `metricSummary`, `preferredVehicleId`, and `explanation`. Empty preferred ID means no defensible preference. An unavailable AI narrative is represented by an empty explanation and an explicit UI status, not fabricated model text. `ComparisonEvidence` reuses existing immutable evidence snapshots; `ComparisonEvaluation` supplies status and safe diagnostics while keeping the original result usable.

- `AI_SUCCESS`: one structured response passed schema, ID, exact-metric and implemented contradiction checks. A blank preferred ID is valid even on success.
- `INSUFFICIENT_INFORMATION`: either vehicle lacks essential F04 evidence, or a mandatory constraint tool failed. Deterministic metrics remain visible; no Gemini call and no preferred vehicle.
- `AI_UNAVAILABLE`: missing credentials, HTTP/timeout failure, malformed JSON, wrong types/IDs, altered metrics or a detected contradiction. Both records, preferences and available evidence remain intact. Check `status`, correct the issue and retry explicitly.
- `STALE`: the saved context changed during the operation. The displayed metrics are labeled as an old snapshot; AI narrative and preference are withheld.

Missing local knowledge or optional cost-tool failure is labeled and does not erase other evidence. Instructional CSV data is not independently verified research. The parser rejects missing/extra/duplicate properties, unsupported preferred IDs, modified deterministic metrics and selected mechanically detectable narrative contradictions. These checks do not establish the truth of arbitrary model prose or seller claims. Review the separate evidence before acting; the comparison is not an inspection, safety guarantee or complete purchase report.

**Verification:** Java 21 build/JAR and **150 JUnit tests passed, 0 failed** (126 earlier + 24 F09 groups). ComparisonChecks exercises production tools, symmetric costs/constraints, session preservation, partial/missing inputs, unit-text rejection, schema/ID/metric validation, provider failure and actual CLI commands. ComparisonGeminiChecks verifies real adapter request serialization, response-envelope parsing, HTTP failure and timeout with injected transport. ComparisonGuiSmoke exercises B entry/edit/cancel, background execution, responsiveness, stale-result handling and safe failure. Existing GUI/F02/F08 checks also pass. All fixtures are offline; no real API key or Gemini request is used.

**Manual live F09 retry:** follow the hidden-key setup under API key security below and launch CLI from Stage2 with `& .\scripts\run.ps1 -Mode cli`. Save real A using `prepare` or confirm an F02 draft; keep genuinely unknown history unknown. Save B with `comparison-set`, inspect `comparison-show`, then enter `status` and `compare` when ready to authorize one live request. `llm-test` is optional and is not an F09 test. With insufficient essential history, `compare` deliberately makes no request. **No live F09 success is claimed.** F10 is implemented below; F11 is implemented below; F12 is implemented below with live verification outstanding.

## F10 vehicle-specific inspection checklist — UC10 / SD05

Save or confirm a vehicle first. In CLI, `checklist` explicitly generates a new checklist, `checklist-show` redisplays the saved result, and `checklist-mark documents` / `checklist-unmark documents` change the displayed `documents` item locally. Substitute any displayed item ID. Only generation may call Gemini; redisplay and completion never do. Explicit regeneration resets completion. Updating/replacing the current vehicle invalidates the checklist, even if its ID is retained. Completion is progress tracking, not verification of vehicle condition. Data is in memory only.

In GUI, open **Inspection Checklist**, select **Generate Inspection Checklist**, then scroll through the six grouped sections and use item checkboxes. **Show saved checklist** reloads local state without requesting AI. The result displays its vehicle, AI/fallback status, knowledge availability, priorities, reasons and sources. Generation runs in a SwingWorker and restores the button after completion/failure. Delayed results for changed vehicles or superseded generations are rejected; old controls cannot alter a new generation's completion state. Long item text has its own scroll view.

The existing agent first runs DataQualityChecker and retrieves VehicleKnowledge through ToolManager. ChecklistPlanner derives actual inspection actions from missing/uncertain fields, the supplied transmission/history and attributed local inspection notes. Six sections are always present: Documents and vehicle history; Exterior inspection; Interior inspection; Engine and drivetrain inspection; Road test; Questions to ask the seller. Unknown accident/maintenance history creates high-priority evidence requests, never positive history claims. No local match means explicitly general guidance. Instructional CSV notes remain attributed teaching material.

The existing PromptBuilder/LLMClient/GeminiAdapter/ResponseParser chain provides AI-assisted prioritization and brief reasons. Model output must preserve required IDs, actions, sections and sources and cannot lower minimum priorities. This intentionally bounds model freedom: it may reorder items, raise priorities and explain relevance, but cannot add unsupported mechanical claims or discard necessary questions. Structural validation rejects empty/missing/extra sections, duplicate IDs/properties, wrong fields/types/priorities and altered evidence. Selected obvious unsupported claims in model reasons are rejected; arbitrary prose cannot be proven true. User review and a professional pre-purchase inspection remain necessary.

`AI_SUCCESS` identifies validated AI-assisted priorities/reasons. `FALLBACK` identifies a practical non-AI checklist after missing credentials, request failure, timeout, malformed/unsupported output or failed evidence validation. Fallback retains generic inspection actions, missing-information questions, applicable transmission guidance and available attributed notes. Knowledge failure/no-match is disclosed separately from AI status. There are no automatic retries. F11 below adds the clarification loop; F12 reports are implemented below.

### Reproducible fictional demonstration

From the repository's Stage2 directory, use a fresh terminal without credentials for a predictable offline fallback:

```powershell
$env:GEMINI_API_KEY = ''
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Gui
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Mode cli
```

Enter these commands in the interactive application:

```text
demo
checklist
checklist-mark documents
checklist-show
checklist-unmark documents
checklist-show
quit
```

`demo` saves the existing fictional **2020 Example Motors Sample Sedan**, 80,000 km, CAD 18,000, automatic transmission, with unknown accident/maintenance history and explicit example cost assumptions. Expect `FALLBACK`, six nonempty sections, High-priority records/disclosure requests, automatic-transmission guidance and attributed instructional notes. `documents` changes from `[ ]` to `[x]` and back; show/mark/unmark make no AI requests.

For GUI, launch with `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Mode gui`. Select **Load fictional example**, **Save and calculate**, **Inspection Checklist**, **Generate Inspection Checklist**. Check/uncheck `documents`, then select **Show saved checklist** to confirm retention. Editing/saving the vehicle requires a newly generated checklist. To demonstrate no matching local knowledge, change the model to `Unlisted Fictional Model`, save, and generate again; expect a clearly general checklist without local-note items.

Offline verification: **168 JUnit tests passed, 0 failed** (150 pre-existing + 18 F10 groups). ChecklistGuiSmoke launches the actual GUI and exercises asynchronous generation, missing vehicle, stale-result rejection, check/uncheck/redisplay and fallback. Existing F02/F08/F09 GUI checks also pass. This is automated Swing smoke verification, not a human acceptance session or a demonstration video. No live F10 request was made. To test live separately, configure a local key using the hidden-input instructions below, restart the application, save a vehicle and explicitly generate the checklist; do not infer live success from fixtures.


## F11 clarification and follow-up management

**F11 → UC11 → SD05:** GUI/CLI → `UsedCarApplicationFacade.startClarification` → `UsedCarAgent.generateClarificationQuestions` → existing `DataQualityChecker` → `PromptBuilder.buildClarificationPrompt` → `LLMClient.generateStructured` / `GeminiAdapter` → `ResponseParser.parseClarificationQuestions` → typed `ClarificationState` in `EvaluationSession`. User responses return through `submitClarification`, skip/unknown and explicit resolution operations. `continueClarification` executes the existing F08 risk or F10 checklist workflow using the updated vehicle. Facade and Adapter retain their roles; resumption reuses the existing Strategy/Factory and tool coordination. No new pattern is claimed merely for a data record.

Java selects up to three actual F04 missing/uncertain targets, prioritizing accident and maintenance history. Pending F02 listing ambiguities/conflicts against the saved vehicle are also eligible; extracting a listing never saves its values automatically. Gemini selects between approved field-specific question phrasings, ranks/orders questions and returns a bounded JSON object (`questions`: `field`, `question`, `priority`, `reason`). It receives only relevant field labels, ambiguity targets and saved preferences, not raw notes, vehicle IDs or credentials. Every returned target must be in the selected typed allowlist; structural validation rejects duplicate properties/targets, extra fields, wrong types, excess questions and unapproved factual prose. Accident/maintenance priorities cannot be downgraded. The fixed evidence-review explanation and two phrasing choices deliberately constrain model creativity to prevent fabricated claims. Invalid output, missing configuration, provider failure or timeout yields useful **FALLBACK** questions; **AI_SUCCESS** means validated provider output, not verified vehicle facts. There are no automatic retries.

Answers use existing Vehicle validation plus field-specific input rules. Mileage is a nonnegative whole number; price is nonnegative CAD; consumption is nonnegative L/100 km. Numeric answers contain no grouping commas, unit symbols or currency symbols. Accident history accepts `YES`/`NO`; maintenance accepts `DOCUMENTED`/`PARTIAL`/`NONE`; transmission accepts `AUTOMATIC`/`MANUAL`/`CVT` (AUTO/AT/MT aliases normalize); fuel type accepts `GASOLINE`, `DIESEL`, `HYBRID`, `ELECTRIC`, `PLUGIN_HYBRID`, `OTHER`. Unknown is a separate action. Invalid answers leave both vehicle and response state unchanged. Known-value differences use the existing F04 conflict check: both existing/proposed values are displayed, and nothing changes before **keep** or **accept**. Skip/unknown never erases known data or interprets unknown history as accident-free/complete records.

The session retains asked targets, responses, conflict decisions and history. Reopening a cycle is local and does not regenerate/charge again. Finish each pending question by answering, skipping or marking unknown, and resolve conflicts before Next, Revisit or Continue. Next asks only unasked gaps; skipped/unknown targets do not repeat automatically. Explicit Revisit permits a correction or a new answer when evidence becomes available. Another vehicle or an external edit to the current vehicle resets clarification memory; internal F11 edits preserve it. Generation revisions and cycle tokens reject old results/answers, including after switching away and back. Preferences, Vehicle B and common cost assumptions are preserved.

Changed values invalidate cached cost, risk and checklist results/completion. Unresolved listing claims also invalidate risk/checklist results and remain uncertainty after skip/unknown. F08 conservatively gates unresolved listing claims instead of synthesizing a confident assessment. Continue Risk recalculates costs from saved assumptions if present, then calls real F08; insufficient essential information retains its safe `INSUFFICIENT_INFORMATION` gate. Continue Checklist calls real F10 with fresh evidence. Neither button fabricates completion or automatically starts another clarification cycle. A new explicit Continue can make another downstream request when its evidence permits.

### GUI workflow

From the Stage2 directory:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Mode gui
```

1. Enter and save a vehicle using the existing Vehicle/Preferences/Cost assumptions tabs and **Save and calculate** (or explicitly confirm an F02 draft). Unsaved form edits are not used by F11.
2. Open **Clarification**, select **Start / Show**. Generation runs in a SwingWorker; status shows AI_SUCCESS or FALLBACK and one to three targets/questions.
3. Select a target, enter an answer and select **Answer**, or select **Skip**/**Unknown**. **Refresh status** shows the retained cycle/history without a request.
4. If `CONFLICT` appears, review existing/proposed values, then choose **Keep existing** or **Accept new**. Until that choice, the saved vehicle is unchanged.
5. Finish the cycle. Use **Next gaps** for remaining targets, or choose a field in the correction selector and **Revisit field** for an explicit local correction.
6. Select **Continue Risk Evaluation** or **Continue Checklist**. Actual downstream results appear in the panel. Vehicle form values refresh after F11 actions; old displayed calculation output is cleared so it cannot be mistaken for fresh results.

### CLI syntax

Launch interactively from Stage2 using `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Mode cli`.

```text
clarify
clarify-show
clarify-answer <field> <value>
clarify-skip <field>
clarify-unknown <field>
clarify-resolve <field> keep|accept
clarify-next
clarify-field <field>
clarify-continue [risk|checklist]
```

Supported field IDs: `accident`, `maintenance`, `mileage`, `price`, `transmission`, `consumption`, `fuelType`, `year`, `make`, `model`, `trim`. Field IDs are case-insensitive; text answers retain their case. `clarify` reuses a cached cycle; `clarify-next` explicitly requests remaining gaps; `clarify-field` is an explicit local revisit and never calls Gemini. `clarify-continue` defaults to risk. Use `show` for the current Vehicle, and `clarify-show` for response/history state. No vehicle yields a helpful error.

### Reproducible fictional Corolla acceptance scenario

For an offline run, open a new PowerShell terminal in Stage2. The following complete example uses the actual executable CLI, saves the fictional 2018 Toyota Corolla LE via `prepare`, then exercises fallback, answer/unknown/skip, cached reopening, real F08 continuation, conflict keep/accept and F10 continuation. Empty strings provide intentionally unspecified values; annual 15,000 km and CAD 1.60/L are explicit fictional cost assumptions. This clears only the new terminal's process key and cannot make a live Gemini call.

```powershell
$env:GEMINI_API_KEY = ''
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
. .\scripts\common.ps1
$f11Jdk = Find-Stage2Jdk
@(
    'prepare'
    '2018'; 'Toyota'; 'Corolla'; 'LE'; '125000'; '12500'
    'AUTOMATIC'; 'GASOLINE'; 'UNKNOWN'; 'UNKNOWN'; ''; ''
    ''; ''; ''; ''; ''; ''; ''
    '15000'; '1.60'; ''; ''; ''
    'clarify'
    'clarify-answer accident NO'
    'clarify-unknown maintenance'
    'clarify-skip consumption'
    'clarify-show'
    'clarify'
    'clarify-continue risk'
    'clarify-field mileage'
    'clarify-answer mileage 130000'
    'clarify-resolve mileage keep'
    'clarify-field mileage'
    'clarify-answer mileage 130000'
    'clarify-resolve mileage accept'
    'clarify-continue checklist'
    'show'
    'quit'
) | & (Join-Path $f11Jdk 'bin\java.exe') -jar .\build\used-car-assistant.jar --cli
```

Piped automation invokes Java directly because PowerShell `run.ps1` does not forward piped lines to its child process. For interactive testing, use `run.ps1` normally. Expected: accident becomes NO, maintenance remains UNKNOWN; the first resume reports insufficient information; proposed 130000 km initially preserves 125000, **keep** retains it, and the second explicit **accept** changes it. Final checklist uses 130000 km and unknown maintenance. History records these actions. GUI acceptance can use the same vehicle values, leave preferences neutral, enter 15000/1.60 in Cost assumptions and follow the steps above.

To test real F11 later, use the hidden-key setup below in a fresh terminal, launch the app, save a vehicle with actual gaps and explicitly run `clarify` or select **Start / Show**. Do not paste the key into an answer or notes. `clarify-show` and reopening are cached; a new session or explicit next unasked cycle is needed for another question-generation request. Continue may request F08/F10 separately. `llm-test` checks connectivity only and does not establish F11 end-to-end success. **No live F11 success is claimed.**

### Verification and limits

Run `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Gui` from Stage2. The observed baseline was 168 passing JUnit tests; F11 adds 36 tests, for **204 passed, 0 failed**. All six GUI smoke entry points, including the actual integrated F11 Swing frame with fictional Corolla input, pass offline. The packaged CLI scenario above also passes. Tests use provider doubles or an explicitly empty-key adapter; no real Gemini request was made.

Memory/history is in-process only. Question wording is deliberately bounded, not a general chatbot. Saved Vehicle requires core numeric fields; incomplete F02 drafts must be corrected and explicitly confirmed in the existing listing workflow before F11, rather than inventing zero mileage/price. F11 can explicitly revisit these known fields. Changed external vehicle records reset history; no cross-session restoration or automatic provenance verification exists. The model's selection quality and live schema acceptance remain unverified. Existing advisory/knowledge limitations remain; F12 is implemented below; the full Stage2 submission is not claimed complete.


## F12 personalized purchase recommendation and evaluation report

F12 implements **F12 → UC12 → SD06** through the existing Java architecture. It produces the original advisory categories **Strong Candidate**, **Consider with Caution**, **High Risk**, and **Insufficient Information**, with strengths, risks, missing information, explicit buyer conflicts, ownership costs, inspection priorities, next steps and explanation. This is the application's buyer-facing report, not the professor's `stage2-report` submission document.

### Evidence and agent workflow

GUI/CLI → `GenerateReportCommand` → `UsedCarApplicationFacade.generateCurrentReport` → session snapshot → `UsedCarAgent.generateRecommendation` → current F04/constraint/local-knowledge tools and available costs/risk → existing Strategy/Simple Factory → `PromptBuilder.buildRecommendationPrompt` → `LLMClient.generateStructured` / unchanged `GeminiAdapter` → `ResponseParser.parseReport` and evidence validation → session storage → shared `ReportText` output. Original `generateReport(Vehicle)`, `generateRecommendation(Vehicle, UserPreference)` and four-argument recommendation-prompt contracts remain available; current-session overloads carry the complete evidence and failure status.

Java evidence is authoritative. Costs already valid for the current vehicle are reused; missing cached costs are calculated only when saved assumptions exist. F04, constraint and local-knowledge checks are refreshed using existing tools. A saved F08 assessment is reused only when its vehicle, preferences, strategy guidance, cost, missing information, constraints and knowledge still match. Otherwise it is explicitly unavailable. F12 does not silently call F08 or F10 AI generation. It uses the existing deterministic inspection planner, ignoring completion checkboxes as proof of condition. One explicit report generation makes **at most one recommendation request**, with no automatic retry.

The report displays **A. Deterministic evidence** and **B. Gemini-assisted interpretation** separately. Gemini selects supported strengths, orders the supplied evidence/inspection/next-step sections, and synthesizes the advisory category and explanation. Required risks, uncertainties, conflicts, cost summary and inspection actions must match Java's supplied values; the model cannot remove or rewrite them. Exact schema/type/category validation rejects missing/extra/duplicate fields and unsupported additions. A favorable `Strong Candidate` requires sufficient evidence, no constraint conflicts or unknown constraint checks, a matching saved Low risk category and a complete cost estimate; it is never assigned just because a car is within budget.

Essential missing information, unresolved F11 conflicts/listing claims, unavailable mandatory constraint checks, or a current F08 Insufficient Information assessment produce **INSUFFICIENT_INFORMATION** without a recommendation request. Configuration/transport/output validation failures produce **AI_UNAVAILABLE**, retain all deterministic evidence and do not fabricate a category or narrative. **AI_SUCCESS** means the response passed application validation, not that the vehicle's history or condition was independently verified.

The model-year difference is calculated by Java as `reference year - model year` and labeled as different from exact in-service age. F12 does not forward or display arbitrary F08 explanatory prose. Numerical/age assertions and selected unsupported condition/certification claims in the new explanation are rejected. Supported evidence sections are exact; validation cannot prove every possible statement in free-form explanatory prose true. Human review remains necessary. The guards are conservative and may also reject cautious wording containing restricted terms; retry is always explicit.

### Diagnosing F12 AI_UNAVAILABLE safely

The earlier generic "configuration or request failure" message discarded the request-stage failure category. The user has now reproduced **HTTP 400** after diagnostic improvements, while reporting successful F08/F11 requests with the same configuration. HTTP rejection is confirmed by the user; the server's detailed reason was discarded by the previous adapter and cannot be recovered from that status alone. Live F12 success remains unverified.

F12 now preserves sanitized typed failure metadata from the existing GeminiAdapter, while unknown exception text remains hidden. The same message is displayed in GUI, CLI, cached review and explicit export. No request bodies, provider error bodies, credentials or raw exception causes are logged or displayed.

| Diagnostic | Meaning / next check |
|---|---|
| `NOT_CONFIGURED`, `INVALID_CONFIGURATION` | Check the environment of the terminal that launches the application; restart after correcting it. Do not share keys. |
| `INVALID_REQUEST_SCHEMA`, `REQUEST_BUILD` | Local request/schema construction failure before transport; report the code for a code-level investigation. |
| `PROVIDER_HTTP_400` | HTTP request rejected. A second allowlisted code may identify the provider's reason: `SCHEMA_COMPLEXITY`, `SCHEMA_FIELD`, `RESPONSE_CONFIGURATION`, `INVALID_ARGUMENT` or `FAILED_PRECONDITION`. No raw server message or private field value is displayed. |
| `PROVIDER_HTTP_401` / `403` | Authorization denied; check account/key permissions locally. |
| `PROVIDER_HTTP_404` | Check model identifier and account access. |
| `PROVIDER_HTTP_429` | Check quota/rate limits before explicitly retrying. |
| `PROVIDER_HTTP_5xx` | Provider server error. |
| `TIMEOUT`, `TRANSPORT_ERROR`, `INTERRUPTED` | Timeout, network failure or interrupted request, respectively. |
| `OUTPUT_LIMIT` | Provider returned `MAX_TOKENS`; incomplete output is rejected even if its text happens to parse. |
| `BLOCKED_RESPONSE`, `INCOMPLETE_RESPONSE`, `INVALID_RESPONSE` | Blocked/non-final output or an unusable provider envelope, before report JSON parsing. |
| `REPORT_STRUCTURE` | Generated report JSON failed required fields/types/category/bounds/duplicate checks. |
| `REPORT_EVIDENCE` | Parsed report contradicted authoritative evidence; the diagnostic names the relevant field/rule without displaying the rejected value. |
| `REQUEST_FAILURE` | An unclassified client error; its raw message is deliberately withheld. |

Retest: close the old application, build, launch from the terminal with the locally configured key, save the fictional Corolla/profile/cost assumptions below, then select **Recommendation / Report → Generate / Regenerate Report** once. Inspect F12's own status and diagnostic line. **Show saved report** does not retry. If it fails, share only the diagnostic code and its sanitized sentence, not a full private report, environment dump or HTTP log. A previously successful F08/F11 request does not establish F12 success. Inspect account configuration locally for configuration/HTTP codes; output-limit, schema and report-validation codes identify the next focused engineering investigation.

The compatibility correction changes the F12 request itself: its provider schema now follows F08's compact structural pattern, with nine required fields, string arrays and only the four advisory categories as an enum. It removes long evidence-dependent enums, the single-cost-string enum and all six dynamic array bounds (including `maxItems: 0` for no buyer conflicts). Evidence remains in the prompt; Java still enforces exact costs, required evidence, constraints, supported strengths, category safety, array/string limits and duplicates. No evidence is silently repaired or dropped. The schema is sent only in `generationConfig.responseJsonSchema`, no longer duplicated in the prompt. The prompt explicitly requests a report matching that schema.

For the fictional Corolla, measured schema size falls from 3458 to 751 characters, enum entries from 29 to 4, and array bounds from 6 to 0. F08 is 423 characters and F11 is 963 for the measured three-question fixture. All three use the same adapter, model selection, endpoint, JSON serializer, `application/json` MIME setting, 4096-token limit and timeout. No conflicting `responseSchema`/`responseFormat` field is sent. These are offline serialization observations, not a replay of private live requests.

The [Gemini API structured-output documentation](https://ai.google.dev/gemini-api/docs/structured-output) warns that schema complexity can cause rejection. Google's [structured-output guidance](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/multimodal/control-generated-output) recommends reducing enum/constraint complexity after a valid schema receives HTTP 400. This justifies the small structural contract, but does not prove the precise server rule that rejected the old request. `enum` and `maxItems` are supported features in general; zero-length bounds are not claimed universally invalid. Excessive combined constraints are the working hypothesis pending live retest.

The adapter classifies only bounded error JSON into fixed labels using recognized status/field metadata and message patterns. Unknown, malformed or unrecognized errors retain a generic status; provider text is never echoed or logged. If the new request fails, share only its diagnostic codes and sanitized sentence. Successful offline fixtures verify serialization, envelope extraction and unchanged Java validation, not live provider acceptance.

### Report memory and export

Reports are in-memory snapshots for the saved current vehicle. Vehicle changes, relevant preferences/strategy/cost/risk changes, and F11 history/response changes invalidate the report. Generation revisions reject late results, including when inputs change and later change back. Reopening **Show saved report** or `report-show` never calls Gemini. An updated calendar reference year invalidates the saved report as well.

Export uses the same displayed report text in UTF-8. It requires a user-chosen new `.txt` path in an existing writable folder; existing files (including links) are never overwritten. A stale report cannot be exported as current. Export contains vehicle/preferences/history data: select a private location deliberately. Nothing is automatically exported into the repository, and there is no report import/session restoration feature.

### GUI manual verification

In a terminal whose current directory is Stage2:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Mode gui
```

Close any previously running instance before rebuilding; Windows can lock the executable JAR.

1. Enter **Vehicle**: 2018, Toyota, Corolla, LE, mileage 125000, price 12500, AUTOMATIC, GASOLINE, accident NO, maintenance DOCUMENTED, consumption 7.2; leave notes blank.
2. Enter **Preferences**: budget 15000, maximum mileage 150000, accepted transmission AUTOMATIC, accident tolerance blank, body type SEDAN, reliability 5, fuel economy 3.
3. Enter **Cost assumptions**: annual km 15000, fuel price 1.65, tax fraction 0.13, maintenance allowance 1000, insurance allowance 1800. Select **Save and calculate**.
4. Check **1080 L**, **CAD 1782 fuel**, **CAD 1625 tax**, and **CAD 18707 first-year estimate**. These are fictional supplied assumptions, not insurance quotations or market valuation.
5. Open **Recommendation / Report** → **Generate / Regenerate Report**. The category/summary appears first, followed by evidence and interpretation. No matching local Corolla knowledge is disclosed, as are unverified seller history and the unverified body-type preference.
6. Select **Show saved report** to reuse the snapshot. Select **Export UTF-8 text...**, choose a new `.txt` filename outside the repository if it contains private data, and save. Choosing an existing filename must fail without overwriting it.
7. Change and save a preference; the report becomes unavailable until regenerated. Or use **Clarification → Revisit field → MILEAGE**, answer 130000, then **Accept new**; the old report is invalidated and a new report must show 130000.

For an offline test, open a new PowerShell terminal, set `$env:GEMINI_API_KEY = ''`, then launch the GUI. Complete input gives **AI_UNAVAILABLE** with exact deterministic results. Change maintenance to UNKNOWN and save: report generation must give **INSUFFICIENT_INFORMATION**. Set budget to 10000: buyer-budget conflict must remain visible, even when Gemini is unavailable. No positive recommendation is invented.

For a live test, use the hidden-key setup under API key security, launch a fresh application, save the same fictional input, and explicitly run **Risk Assessment → Evaluate Vehicle**, followed by **Generate / Regenerate Report**. These are separate authorized requests. A valid current risk may be reused; if the F08 result is unavailable, the report labels that limitation. Verify F12's own **AI_SUCCESS**, preserved costs, source limitations and all sections. A previous `llm-test` or F11 AI_SUCCESS is not proof of F12 success. **No real Gemini request was made during this increment.**

### CLI commands and complete offline example

```text
report                         Generate/regenerate for current saved data; at most one AI request
report-show                    View the saved current report; no AI request
report-save <new-file.txt>      Export UTF-8 text; path may contain spaces; never overwrite
```

Launch interactively with `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Mode cli`. Use `prepare` and enter the values above in prompt order. For a complete copyable offline exercise from Stage2:

```powershell
$env:GEMINI_API_KEY = ''
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
. .\scripts\common.ps1
$f12Jdk = Find-Stage2Jdk
$f12Export = Join-Path $env:TEMP ('Corolla-F12-' + [guid]::NewGuid().ToString('N') + '.txt')
@(
    'prepare'
    '2018'; 'Toyota'; 'Corolla'; 'LE'; '125000'; '12500'
    'AUTOMATIC'; 'GASOLINE'; 'NO'; 'DOCUMENTED'; '7.2'; ''
    '15000'; '150000'; 'AUTOMATIC'; ''; 'SEDAN'; '5'; '3'
    '15000'; '1.65'; '0.13'; '1000'; '1800'
    'report'
    'report-show'
    "report-save $f12Export"
    'clarify-field consumption'
    'clarify-answer consumption 8'
    'clarify-resolve consumption accept'
    'report-show'
    'report'
    'quit'
) | & (Join-Path $f12Jdk 'bin\java.exe') -jar .\build\used-car-assistant.jar --cli
Write-Host "First report exported to: $f12Export"
```

The first report preserves CAD 18707. The intentional second `report-show` after accepting new consumption reports **No current report**; this proves invalidation. Regeneration uses 1200 L, CAD 1980 fuel and CAD 18905 first-year estimate. The existing export remains the earlier snapshot. Direct Java invocation is used for piped input because `run.ps1` does not forward the PowerShell pipeline to the child process; interactive `run.ps1` remains supported.

For live CLI acceptance, configure the key locally as described below, then use `prepare`, optionally `strategy auto`, explicitly `evaluate`, `report`, and `report-show`. `report-save C:\path\you\choose\new-report.txt` accepts the entire remainder as the filename, optionally surrounded by quotes. After an F11 accepted change, `clarify-continue risk` recalculates costs and explicitly runs F08; then `report` can reuse that fresh risk. Repeating `report` is an explicit retry/regeneration, not a cached read.

### Verification and remaining limits

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Gui
```

Observed F11 baseline: **204 JUnit passed**. Initial F12 suite: **245 passed, 0 failed** (41 new tests). The subsequent F12 diagnostic correction adds 19 adapter-to-report regression groups: **264 passed, 0 failed**. The request-compatibility correction adds ten contract/strict-validation/server-error regression groups: **274 passed, 0 failed**. All seven actual-frame Swing smoke entry points pass, including the report tab, asynchronous generation, stale-result rejection, cached view, UTF-8/no-overwrite export, F11 updates and unavailable/insufficient states. The packaged CLI passes the fictional Corolla, budget-conflict and unknown-maintenance scenarios with exact recalculation and export. Provider doubles and explicitly unconfigured adapters were used; live F12 quality/schema acceptance remains unverified.

The existing F08 free-form explanation can still contain unsupported age assertions; that separate validation defect was not broadly refactored here. F12 isolates it and rejects age assertions in its own explanation. The existing factory is an argument-based **Simple Factory**, not strict GoF Factory Method. Reports remain advisory, local knowledge is instructional, body-type matching is not implemented by the existing constraint checker, units remain CAD/km/L-per-100-km, and sessions are not restored from exports. The final course implementation report, updated submission diagrams, demonstration videos, GitHub review and grading acceptance remain separate work.

## Gemini API integration status

`GeminiAdapter implements LLMClient` now uses Java 21 `HttpClient` to POST to `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`. Gson 2.13.2 serializes `contents[].parts[].text` and strictly parses the response. Authentication uses the `x-goog-api-key` header, never a URL query parameter. The first candidate's completed text parts are joined; thought parts are excluded. Empty, malformed, blocked, missing-text and incomplete responses are rejected instead of presented as successful output.

The default is `gemini-3.1-flash-lite`, listed as stable and cost-efficient in the [official model catalog](https://ai.google.dev/gemini-api/docs/models) and [release notes](https://ai.google.dev/gemini-api/docs/changelog). Set `GEMINI_MODEL` to override it with an available Gemini model identifier. Account access, availability and quota must be verified locally. The implementation follows the [generateContent reference](https://ai.google.dev/api/generate-content) and [API-key documentation](https://ai.google.dev/gemini-api/docs/api-key). The documented generateContent endpoint is retained for this focused text adapter; no automatic model fallback or retry is performed.

Connection timeout is 10 seconds; request timeout is 30 seconds. HTTP errors distinguish authorization, quota/rate limits, unavailable models and server failures. Interrupts restore the thread interrupt flag. Diagnostics omit raw provider responses, exception causes and credentials. The exact configured key is also redacted if unexpectedly echoed in generated output. No network request occurs during startup, status queries or deterministic operations.

`status` distinguishes missing/invalid configuration, configured but not live-verified, last successful generation in this session, and last request failure. A configured key does not prove connectivity. **No real Gemini request was made during this increment; live connectivity remains unverified.** Offline fixtures are test data, not live evidence.

The explicit CLI `llm-test` command follows facade -> agent -> LLMClient -> GeminiAdapter, sends a short greeting prompt and displays the actual returned response plus model/status. It does not send or change the saved vehicle or buyer preferences. The GUI includes the F02 Listing Analyzer using a background worker; deterministic controls remain available. F02 listing extraction/review uses this integration as described above. F08 now coordinates evidence-grounded risk assessment as described above; F09 now adds symmetric comparison; F10 adds inspection planning and local completion; F11 adds bounded clarification and real resumption; F12 now adds validated recommendation reports; broader AI-assisted F04 interpretation remains limited.

## API key security and optional live test

From the Stage2 directory, build first. Enter a key only into the local hidden PowerShell prompt, never into chat, code or a command-history literal:

```powershell
& .\scripts\build.ps1
$geminiSecret = Read-Host 'Gemini API key (hidden)' -AsSecureString
$geminiPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($geminiSecret)
try {
    $env:GEMINI_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($geminiPointer)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($geminiPointer)
    $geminiSecret.Dispose()
}
$env:GEMINI_MODEL = 'gemini-3.1-flash-lite'
try {
    & .\scripts\run.ps1 -Mode cli
} finally {
    Remove-Item Env:GEMINI_API_KEY -ErrorAction SilentlyContinue
}
```

In the CLI, enter `status`, then `llm-test` only when ready to authorize one real request, then `status` and `quit`. A successful test prints genuine generated text and the configured model. A failure prints a safe diagnostic; check account permissions, quota/billing or model access as appropriate before retrying manually. This operation can consume quota and incur provider charges. Without a key, the test explains missing configuration while other operations remain available.

The application reads process environment variables only; `.env` is not loaded. `.env.example` contains placeholders and variable names, not working credentials. `.gitignore` continues to exclude `.env`, `.env.*` and `config.local.json`, while retaining `.env.example`. Never include keys in source, logs, screenshots, reports or Git. Do not enable HTTP header debugging with real keys. Environment variables exist in process memory; close the terminal after use if appropriate.

Fourteen adapter test groups exercise loopback HTTP requests, JSON escaping, endpoint selection, actual parser behavior, configuration, HTTP failures, malformed/empty/blocked output, timeout/connection/interruption handling, secret redaction, state transitions and shared CLI integration. They require no real key and consume no Gemini quota. The existing verification command runs these alongside all prior regression tests.

## Implementation status and known limitations

| Status | Features or components |
|---|---|
| **Implemented initial subsets** | F11 clarification with session memory, explicit conflict resolution and real F08/F10 continuation (offline verified; live F11 unverified); F10 inspection checklists with local completion and deterministic fallback (offline verified; live F10 unverified); F09 two-vehicle comparison (offline verified; live F09 unverified); F08 coordinated risk assessment (offline verified; live F08 unverified); F02 listing extraction with draft review and explicit confirmation (offline verified; live F02 unverified); F01 vehicle information entry and management; F03 buyer preference profile; F04 missing-information detection; F05 local CSV knowledge retrieval; F06 ownership-cost estimation; F07 buyer constraint checking. These subsets operate through shared application services and do not complete the full original feature specifications. |
| **F12 implemented; live unverified** | Personalized recommendation with the original four categories, deterministic evidence, cached session view and explicit UTF-8 export. Real-provider quality and final course deliverables still require separate acceptance. |
| **Operational pattern relationships** | Facade and Command have working workflows; Strategy and a Simple Factory are operational for both priority-guidance previews and F08 assessment. |
| **Implemented provider adapter; live unverified** | Adapter performs HTTP/JSON adaptation behind LLMClient; explicit CLI connection test is available. |
| **Planned design patterns** | Formal GoF Factory Method is not implemented or claimed. Strategy/Factory integration into F08 is implemented; F12 now uses the same Strategy/Simple Factory roles; no strict GoF Factory Method is claimed. |

The application provides advisory F12 recommendations and explicit text export, but does not provide verified reliability ratings, automatic durable session storage, or the professor's final implementation report. Buyer constraint checks cover budget, mileage, accepted transmissions, and accident-history tolerance; session data remains in memory. Both the CLI `prepare` prompts and GUI Preferences tab accept comma-separated AUTOMATIC, MANUAL, or CVT preferences (existing AUTO/AT/MT aliases are normalized). Leave transmission preferences blank to skip the check. Accident tolerance accepts `NO_ACCIDENTS` or `ANY`; blank/UNKNOWN is neutral. Enter explicit accident history as YES or NO. Unknown or unrecognized vehicle values produce findings with severity `UNKNOWN` when the corresponding preference is set, distinct from `CONFLICT`. This increment implements the deterministic F07 rules in the preparation workflow; F08 and F09 reuse these findings. Dedicated constraint controls remain planned. This repository content is an initial foundation, not a fully implemented Stage 2 submission.

For further detail, see [Implementation Status](docs/IMPLEMENTATION_STATUS.md), [Stage 1 Traceability](docs/STAGE1_TRACEABILITY.md), [Design Decisions](docs/DESIGN_DECISIONS.md), [Development and Verification Log](docs/DEVELOPMENT_LOG.md), and [Next Steps](docs/NEXT_STEPS.md).
