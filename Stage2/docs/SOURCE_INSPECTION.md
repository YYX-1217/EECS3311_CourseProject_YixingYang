# Local source inspection record

Inspection date: 2026-10-26, America/New_York. Actual environment workspace: `C:\Users\yixin\Desktop\eecs3311`. Existing course project: `project/`, containing sibling Stage1 and Stage2 directories. Stage2 initially contained only `Project-Stage2-Instruction.pdf`; it was preserved. No applicable AGENTS.md was discovered in the inspected workspace/ancestor paths.

## Reports and project requirements

Source paths in the table are relative to Stage2 unless stated otherwise.

| Local source | Inspection performed |
|---|---|
| `../Stage1/EECS3311_Stage1_YixingYang.pdf` | All 53 pages' text read, including overview, all feature definitions, class contracts, patterns, UC01–UC12, SD01–SD06 descriptions, traceability and implementation explanations. Embedded p. 10 class image rendered and viewed to verify adapter-name discrepancy. |
| `../Stage1/EECS3311_Stage1_YixingYang.docx` | Entire document XML extracted (1,048 paragraphs including table cells); compared against PDF text. Two apparent paragraph differences are page-boundary headers interrupting PDF extraction, not changed requirements. |
| `../Stage1/task/EECS3311_Stage1_Task1_AI_Used_Car_Evaluation_Assistant_Gemini.docx` | Entire text extracted and compared with final report; substantive content included there. |
| `../Stage1/task/EECS3311_Stage1_Task2_AI_Used_Car_Evaluation_Assistant_Gemini.docx` | Entire text extracted/compared; section numbering differs in assembled report, class/method design retained. |
| `../Stage1/task/EECS3311_Stage1_Task3_Feature_to_Design_Traceability_AI_Used_Car_Evaluation_Assistant.docx` | Entire text extracted/compared; additional introductions and per-feature prose reviewed. Same F01–F12 / UC01–UC12 / SD01–SD06 relationships. |
| `../Stage1/task/EECS3311_Stage1_Task4_Feature_Implementation_Explanations_AI_Used_Car_Evaluation_Assistant.docx` | Entire text extracted/compared; substantive content incorporated into final report. |
| `../Stage1/Project-Instruction.pdf` | All 17 pages inspected: design traceability, AI-human records, deterministic verification versus Stage3 KUMA, final artifacts. |
| `../Stage1/stage1.pdf` | All 13 pages inspected; this is Stage1 course instructions and example project topics, not another used-car report. |
| `Project-Stage2-Instruction.pdf` | All 5 pages inspected directly: GUI/CLI, all original features, five meaningful patterns, real LLM/agent behavior, documented changes, weekly commits, report/videos, local secrets. |

No `Project-Stage2-Instruction(2).pdf` was found in the workspace. The available five-page Stage2 PDF was used, not only the pasted summary. Copies of the final PDF/DOCX under `C:\Users\yixin\Documents\EECS3311_CourseProject_YixingYang` were read for hash comparison only and match the workspace copies. No remote submitted version was inspected or asserted.

## UML sources

Read all eight current design `.uxf` files as XML, including `panel_attributes`, class members, lifeline messages, relation codes, coordinates and route points. Viewed their corresponding exported PNG diagrams. The class diagram contains 85 elements; use case 34; SD01 73; SD02 62; SD03 96; SD04 76; SD05 103; SD06 70.

- `1_AI_Used_Car_Class_Diagram_FIXED`: commands realize Command; concrete tools realize Tool; three strategies realize EvaluationStrategy; provider realizes LLMClient. Facade has 1:1 agent and session associations; session links to 0..2 vehicles and one preference. Knowledge tool links to repository. Agent depends on tools, quality checker, LLM, strategy, prompts/parser; factory depends on strategy products. Provider label is **DeepSeekAdapter**, contrary to written/sequence design.
- `2_AI_Used_Car_Use_Case_Diagram`: twelve use cases, User and External LLM Service; local knowledge repository is internal rather than an external actor.
- `3_SD01_Listing_Analysis_and_Vehicle_Record_Creation_FIXED`: listing → command → facade → agent → prompt → Gemini → parser/quality check → user confirmation → session. Parsing and conflict alternatives explicit.
- `4_SD02_Prepare_Vehicle_Evaluation_FIXED`: save preferences, quality checks, cost tool and constraint tool; missing optional costs omitted, unknown constraint data not passed. This is the main runnable foundation slice.
- `5_SD03_Knowledge_Retrieval_and_AI_Risk_Assessment_FIXED`: knowledge and constraints before strategy-guided Gemini synthesis; no-data, insufficient evidence, API failure and one controlled parsing retry.
- `6_SD04_Two_Vehicle_Comparison_FIXED`: loop gathers evidence for both candidates; fixed metrics remain available when LLM narrative fails.
- `7_SD05_Inspection_Checklist_and_Clarification_Loop_FIXED`: checklist and question generation, then bounded answer loop; explicit skip/conflict/fallback paths. Single-argument conflict-resolution clarification message conflicts with facade's field/value signature.
- `8_SD06_Personalized_Recommendation_and_Final_Report_FIXED`: prior structured evidence, preference/strategy, constrained Gemini request, parsed report; evidence unavailable and insufficient alternatives. Evidence storage/freshness is not fully specified.

The 19 other `.uxf` files under `Stage1/Umlet/palettes` were inventoried and parsed successfully; they are bundled UMLet examples/templates, not application architecture. Forty PNGs under `Stage1/tmp` were inventoried and viewed in contact sheets, including historical exports/previews and UML lecture slides. Those drafts do not override the current eight design files. Existing diagram-building/fix scripts were discovered as historical support, not executed or treated as instructions.

## Course material consulted

The local lecture PDFs were text-extracted successfully. Relevant sections actually reviewed include:

| PDF at workspace root | Reviewed topics/pages |
|---|---|
| `EECS3311-1- OOP Design.pdf` | Polymorphism p.36, generics p.59 and overview |
| `EECS3311-3-UML.pdf` | Perspectives p.14; relations pp.19,21–23; multiplicity p.25; existing slide previews pp.15–28 |
| `EECS3311-4-UML-II.pdf` | Sequence messages/lifelines pp.9–10 |
| `EECS3311-5-SOLID.pdf` | Principles pp.3–4, dependency inversion p.22 |
| `EECS3311-6-Design Pattern-Creational.pdf` | Factory construction distinction pp.20–21; pattern classification p.12 |
| `EECS3311-07-Structural Patterns.pdf` | Adapter pp.4,6; Facade pp.5,66 |
| `EECS3311-08-Behavioral Patterns.pdf` | Strategy pp.53–55; Command p.64 |
| `EECS3311-10-Software Architecture.pdf` | Maintainability/layering p.10, architecture styles p.31, MVC context p.64 |
| `EECS3311-2-Junit.pdf` | Framework, cases and assertions pp.1–3,9 |
| `EECS3311-11-TDD.pdf` | Test-driven development p.3 |
| `EECS3311-12-InputSpace.pdf` | Boundary-value tests pp.42,45 |
| `EECS3311-13-Write-Good-Test Cases.pdf` | Independent, focused and exception tests p.47 |
| `EECS3311-14-Test Automation.pdf` | Repeatable automated suites pp.1–3 |

Syllabus, administration, software-process-pattern and Java SDK pattern PDFs were also discovered and text-extracted, but are not claimed to have been fully studied. Lecture references inform the architecture and basic verification; this task does not claim exhaustive course compliance.

## Inspection artifacts and integrity

Text extracts, image previews, parsed XML inventory and the pre-change SHA-256 baseline live only in ignored `Stage2/build/inspection/`. Original source files were opened read-only. A final baseline comparison checks all pre-existing workspace files outside Stage2 plus the pre-existing Stage2 instruction PDF; see DEVELOPMENT_LOG and `build/inspection/integrity-result.json` for the actual outcome.
