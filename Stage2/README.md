# AI Used-Car Evaluation Assistant — Stage 2 Foundation

## Project overview

This project is the **initial Stage 2 foundation** for the EECS3311 Fall 2026 course project. It supports a working deterministic workflow through a command-line interface (CLI) and a Swing graphical user interface (GUI): vehicle entry, session-based buyer preferences, ownership-cost estimation, data-quality checks, and buyer constraint checks. It is not a fully implemented Stage 2 application, and the Gemini HTTP adapter is implemented but live connectivity has not been verified.

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

The 126 tests cover negative and malformed inputs, required fields and year validation, exact cost calculations, optional values, unknown fuel consumption, constraint boundaries, zero values and rounding, session preservation after rejected updates, tool/LLM boundaries, and interactive CLI processing, including transmission matching/mismatching, multiple accepted types, accident tolerance, neutral preferences, unknown values, and invalid preference input.

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

Verification: 16 new JUnit groups cover prompt constraints, complete/partial extraction, missing histories, normalization, invalid numbers/JSON/types/evidence, units, uncertainty, no-auto-save, approved save, cancellation, conflicts/stale baselines, missing configuration and CLI edit/confirm. `verify.ps1 -Gui` additionally runs a fixture-backed Swing test for background analysis, responsive UI, correction, confirmation and cancellation. All 126 JUnit tests pass. The user reports a prior successful live connection test; **no live F02 extraction was performed during this increment**, so real-model extraction quality remains unverified.

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

This is a deterministic F04 increment, not complete F04 implementation. The second record is entered manually to represent structured extraction output. F02 now supplies real LLM extraction and draft review through the separate listing workflow. Broader ambiguity interpretation, automated clarification and a dedicated general conflict-resolution workflow remain planned. A source comparison concerns two descriptions of the same vehicle; it does not implement F09 comparison of competing vehicles. No risk rating or purchase recommendation is generated.

## F05 local vehicle knowledge retrieval

`knowledge` in the CLI and **View local knowledge** in the GUI follow the Stage 1 UC05/SD03 call chain: facade → agent → existing ToolManager (`vehicleKnowledge`) → VehicleKnowledgeTool → VehicleKnowledgeRepository. The result retains the Stage 1 `summary`, `commonIssues`, `inspectionNotes`, and `sourceLabel` fields. Retrieval does not modify the saved vehicle.

The UTF-8 dataset `src/main/resources/vehicle-knowledge.csv` contains three explicitly fictional instructional records: two non-overlapping year ranges for Example Motors / Sample Sedan and one for Demo Works / Practice Hatch. The existing fictional 2020 demo matches the first record. These notes are sample inputs for demonstrating retrieval, not claims about real vehicle reliability. The build packages the CSV in the JAR, so lookup does not depend on the working directory. Rebuild after editing the bundled data.

The restricted CSV schema is `make,model,yearFrom,yearTo,summary,commonIssues,inspectionNotes,sourceLabel`. The header is exact. Each nonblank record occupies one line with eight comma-separated columns; quoting, embedded commas, multiline fields, and comments are unsupported and rejected. The two list columns use `|` separators and may be empty; empty items within a nonempty list are rejected. Other columns are required. Years are inclusive integers from 1886 through 9999 with start ≤ end. Matching ignores case and surrounding whitespace. Overlapping ranges for the same normalized make/model are rejected rather than selected arbitrarily.

The repository loads lazily on first lookup, caches validated records, and exposes `load()` for explicit reloads. It also accepts a local `Path` for tests or programmatic use. Missing/unreadable files and malformed records produce explicit retrieval errors; malformed-record errors include a line number. A failed reload cannot silently serve old records. No matching make/model/year returns **No local vehicle knowledge available.** with empty issue and inspection lists. Other deterministic operations remain available if knowledge retrieval fails.

Ten F05 tests cover exact matching, case and whitespace normalization, inclusive year boundaries, no-data results, missing files, malformed CSV, tool dispatch, facade integration, and the CLI command. GUI smoke verification exercises both a matching record and no-data output.

This increment provides local deterministic retrieval only. There is no live database, fuzzy matching, dataset editor, or external API call. F08 now retrieves this knowledge automatically for risk assessment; comparison, checklists and recommendations remain planned.

## Evaluation-priority Strategy preview

The Preferences tab includes a mode selector (`auto`, `balanced`, `budget`, `reliability`) and **Preview priority guidance**. Save preference edits first: previews use the session profile, not unsaved form fields. The CLI offers the equivalent `strategy <mode>` operation and `strategy` to repeat the preview. No vehicle is required. Selection persists in the current agent context until changed or the application closes; a new context has no implicit strategy. Invalid selection preserves the previous strategy.

- **Balanced:** discusses budget, reliability, mileage, vehicle risk, and ownership-cost trade-offs. Relative reliability/fuel-economy emphasis follows their explicit nonzero priority values; an unspecified priority is not interpreted as a lower ranking.
- **Budget Focused:** discusses purchase price, the supplied budget, and ownership costs first. An absent budget remains unspecified; missing cost assumptions are not zero. Low cost never overrides risk warnings or explicit constraints.
- **Reliability Focused:** discusses reliability evidence, maintenance uncertainty, inspection concerns, and risks before cost advantages. Numeric importance remains a buyer preference, not a vehicle-condition score.

All modes retain explicit mileage, transmission, body-type and accident preferences, preserve neutral values, and prohibit invented history or risk ratings. Guidance is deterministic instruction text, not a completed AI assessment, inspection checklist, or purchase recommendation. F07 decisions are unchanged.

The implementation preserves Stage 1 `EvaluationStrategy.buildPriorityGuidance(UserPreference): String`, the three named concrete strategies, and `UsedCarAgent.setStrategy(EvaluationStrategy)`. The agent holds the interface and dispatches polymorphically. A package-private `PriorityGuidance` helper shares preference rendering and safeguards. `EvaluationStrategyFactory.createStrategy(UserPreference)` now centralizes automatic construction. The agent obtains the product and invokes its guidance through `EvaluationStrategy`; CLI and GUI use the same facade. Manual construction also resides in the factory. This preview implements the strategy-selection portion of SD03/SD06; F08 now uses this strategy-selection chain; F12 remains planned.

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

The original `assessRisk(vehicle, preference)`, `evaluateVehicle(vehicle)`, `buildRiskPrompt(vehicle, knowledge, missing, violations)` and `parseRiskAssessment(response)` contracts are retained. The current-session command uses additional envelope/context methods for costs, safe errors and freshness. D12 records these extensions and the explicit no-retry decision. Successful/current results are held only in memory and invalidated when inputs change; F09–F12 remain outside this increment.

Verification: 20 new JUnit groups plus all 106 earlier tests pass (**126 total**). Tests cover schema/category errors, no vehicle, sufficiency gate, knowledge/no-match, tool sequencing, constraints, neutral/saved preferences, automatic/manual strategy, cost reuse/invalidation, rejected/provider responses and shared CLI behavior. `verify.ps1 -Gui` runs the existing GUI/F02 checks and RiskGuiSmoke, exercising background evaluation, responsive EDT, stale-context handling, success and failure with retained evidence. **No live F08 Gemini request was performed; real-model end-to-end behavior remains unverified.** Run `evaluate` manually with local credentials only when ready to authorize the request.

## Gemini API integration status

`GeminiAdapter implements LLMClient` now uses Java 21 `HttpClient` to POST to `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`. Gson 2.13.2 serializes `contents[].parts[].text` and strictly parses the response. Authentication uses the `x-goog-api-key` header, never a URL query parameter. The first candidate's completed text parts are joined; thought parts are excluded. Empty, malformed, blocked, missing-text and incomplete responses are rejected instead of presented as successful output.

The default is `gemini-3.1-flash-lite`, listed as stable and cost-efficient in the [official model catalog](https://ai.google.dev/gemini-api/docs/models) and [release notes](https://ai.google.dev/gemini-api/docs/changelog). Set `GEMINI_MODEL` to override it with an available Gemini model identifier. Account access, availability and quota must be verified locally. The implementation follows the [generateContent reference](https://ai.google.dev/api/generate-content) and [API-key documentation](https://ai.google.dev/gemini-api/docs/api-key). The documented generateContent endpoint is retained for this focused text adapter; no automatic model fallback or retry is performed.

Connection timeout is 10 seconds; request timeout is 30 seconds. HTTP errors distinguish authorization, quota/rate limits, unavailable models and server failures. Interrupts restore the thread interrupt flag. Diagnostics omit raw provider responses, exception causes and credentials. The exact configured key is also redacted if unexpectedly echoed in generated output. No network request occurs during startup, status queries or deterministic operations.

`status` distinguishes missing/invalid configuration, configured but not live-verified, last successful generation in this session, and last request failure. A configured key does not prove connectivity. **No real Gemini request was made during this increment; live connectivity remains unverified.** Offline fixtures are test data, not live evidence.

The explicit CLI `llm-test` command follows facade -> agent -> LLMClient -> GeminiAdapter, sends a short greeting prompt and displays the actual returned response plus model/status. It does not send or change the saved vehicle or buyer preferences. The GUI includes the F02 Listing Analyzer using a background worker; deterministic controls remain available. F02 listing extraction/review uses this integration as described above. F08 now coordinates evidence-grounded risk assessment as described above; full AI-assisted F04 and F09–F12 remain incomplete.

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
| **Implemented initial subsets** | F08 coordinated risk assessment (offline verified; live F08 unverified); F02 listing extraction with draft review and explicit confirmation (offline verified; live F02 unverified); F01 vehicle information entry and management; F03 buyer preference profile; F04 missing-information detection; F05 local CSV knowledge retrieval; F06 ownership-cost estimation; F07 buyer constraint checking. These subsets operate through shared application services and do not complete the full original feature specifications. |
| **Planned features** | F09 two-vehicle comparison; F10 inspection checklist generation; F11 clarification and follow-up management; F12 personalized recommendation and evaluation report. |
| **Operational pattern relationships** | Facade and Command have working workflows; Strategy and a Simple Factory are operational for both priority-guidance previews and F08 assessment. |
| **Implemented provider adapter; live unverified** | Adapter performs HTTP/JSON adaptation behind LLMClient; explicit CLI connection test is available. |
| **Planned design patterns** | Formal GoF Factory Method is not implemented or claimed. Strategy/Factory integration into F08 is implemented; F12 report generation remains planned. |

The application does not currently provide reliability ratings, purchase recommendations, disk persistence, or complete evaluation reports. Buyer constraint checks cover budget, mileage, accepted transmissions, and accident-history tolerance; session data remains in memory. Both the CLI `prepare` prompts and GUI Preferences tab accept comma-separated AUTOMATIC, MANUAL, or CVT preferences (existing AUTO/AT/MT aliases are normalized). Leave transmission preferences blank to skip the check. Accident tolerance accepts `NO_ACCIDENTS` or `ANY`; blank/UNKNOWN is neutral. Enter explicit accident history as YES or NO. Unknown or unrecognized vehicle values produce findings with severity `UNKNOWN` when the corresponding preference is set, distinct from `CONFLICT`. This increment implements the deterministic F07 rules in the preparation workflow; F08 reuses these findings. Dedicated constraint controls and comparison integration remain planned. This repository content is an initial foundation, not a fully implemented Stage 2 submission.

For further detail, see [Implementation Status](docs/IMPLEMENTATION_STATUS.md), [Stage 1 Traceability](docs/STAGE1_TRACEABILITY.md), [Design Decisions](docs/DESIGN_DECISIONS.md), [Development and Verification Log](docs/DEVELOPMENT_LOG.md), and [Next Steps](docs/NEXT_STEPS.md).
