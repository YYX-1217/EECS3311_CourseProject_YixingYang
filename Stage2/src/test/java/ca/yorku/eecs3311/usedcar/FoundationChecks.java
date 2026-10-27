package ca.yorku.eecs3311.usedcar;

import ca.yorku.eecs3311.usedcar.app.*;
import ca.yorku.eecs3311.usedcar.domain.*;
import ca.yorku.eecs3311.usedcar.tool.*;
import ca.yorku.eecs3311.usedcar.llm.GeminiAdapter;
import ca.yorku.eecs3311.usedcar.ui.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Scanner;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/** Dependency-free verification shared by the JUnit tests and offline fallback runner. */
public final class FoundationChecks {
    private FoundationChecks() { }
    public static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void equal(BigDecimal actual, String expected) {
        check(actual != null && actual.compareTo(new BigDecimal(expected)) == 0, "Expected " + expected + "; got " + actual);
    }
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid input was accepted.");
    }

    public static void numericValidation() {
        for (String field : new String[]{"price", "mileage", "budget", "maxMileage", "annualKm", "fuelPrice", "consumption", "maintenanceAllowance", "insuranceAllowance", "taxRate"}) {
            Map<String, String> values = PreparationForm.example(); values.put(field, "-1");
            rejects(() -> PreparationForm.command(ApplicationFactory.create(), values));
        }
        for (String bad : new String[]{"NaN", "Infinity", "1e309", "oops", ""}) {
            Map<String, String> values = PreparationForm.example(); values.put("price", bad);
            rejects(() -> PreparationForm.command(ApplicationFactory.create(), values));
        }
        var values = PreparationForm.example(); values.put("taxRate", "13");
        rejects(() -> PreparationForm.command(ApplicationFactory.create(), values));
    }

    public static void requiredFieldsAndYear() {
        for (String field : new String[]{"make", "model", "year", "mileage"}) {
            var values = PreparationForm.example(); values.put(field, "");
            rejects(() -> PreparationForm.command(ApplicationFactory.create(), values));
        }
        for (String year : new String[]{"1885", Integer.toString(java.time.Year.now().getValue() + 2)}) {
            var values = PreparationForm.example(); values.put("year", year);
            rejects(() -> PreparationForm.command(ApplicationFactory.create(), values));
        }
    }

    public static void exactCostCalculation() {
        var result = PreparationForm.command(ApplicationFactory.create(), PreparationForm.example()).execute();
        equal(result.cost().annualFuelUse(), "1080"); equal(result.cost().annualFuelCost(), "1782");
        equal(result.cost().purchaseTax(), "2340"); equal(result.cost().firstYearEstimate(), "24922");
        check(result.cost().unavailable().isEmpty(), "Complete supplied inputs must have a complete estimate.");
    }

    public static void omittedAssumptions() {
        for (String field : new String[]{"taxRate", "maintenanceAllowance", "insuranceAllowance"}) {
            var values = PreparationForm.example(); values.put(field, "");
            var result = PreparationForm.command(ApplicationFactory.create(), values).execute();
            check(result.cost().firstYearEstimate() == null, "Missing " + field + " must prevent a complete total.");
            equal(result.cost().annualFuelCost(), "1782");
        }
    }

    public static void unknownConsumption() {
        var values = PreparationForm.example(); values.put("consumption", "");
        var result = PreparationForm.command(ApplicationFactory.create(), values).execute();
        check(result.cost().annualFuelCost() == null && result.cost().annualFuelUse() == null, "Unknown consumption must not become zero.");
        equal(result.cost().purchaseTax(), "2340");
        check(result.cost().firstYearEstimate() == null, "Incomplete total must be unavailable.");
        check(result.missing().missingFields().contains("Fuel consumption"), "Unknown consumption must remain visible.");
    }

    public static void constraintBoundaries() {
        var values = PreparationForm.example(); values.put("budget", "18000"); values.put("maxMileage", "80000");
        check(PreparationForm.command(ApplicationFactory.create(), values).execute().violations().isEmpty(), "Equal limits must pass.");
        values.put("budget", "17999.99"); values.put("maxMileage", "79999");
        check(PreparationForm.command(ApplicationFactory.create(), values).execute().violations().size() == 2, "Both exceeded limits must be reported.");
        values.put("budget", ""); values.put("maxMileage", "");
        check(PreparationForm.command(ApplicationFactory.create(), values).execute().violations().isEmpty(), "Unspecified limits must be skipped.");
    }

    public static void zeroAndRounding() {
        var calculator = new CostCalculatorTool();
        equal(calculator.calculateAnnualFuelCost(new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("0.5")), "0.01");
        var values = PreparationForm.example();
        for (String field : new String[]{"price", "consumption", "annualKm", "fuelPrice", "taxRate", "maintenanceAllowance", "insuranceAllowance"}) values.put(field, "0");
        equal(PreparationForm.command(ApplicationFactory.create(), values).execute().cost().firstYearEstimate(), "0");
    }

    public static void sessionAndInvalidUpdate() {
        var facade = ApplicationFactory.create();
        var first = PreparationForm.command(facade, PreparationForm.example()).execute();
        var values = PreparationForm.example(); values.put("price", "16000");
        var updated = PreparationForm.command(facade, values).execute();
        check(first.vehicle().id().equals(updated.vehicle().id()), "Editing current vehicle must retain identity.");
        check(updated.violations().isEmpty(), "Updated lower price should remove budget conflict.");
        values.put("price", "-1"); rejects(() -> PreparationForm.command(facade, values).execute());
        check(facade.currentVehicle().orElseThrow().equals(updated.vehicle()), "Rejected edit must preserve saved vehicle.");
        equal(facade.preferences().maxBudget(), "17000");
    }

    public static void toolContractsAndLlmBoundary() {
        var manager = new ToolManager(); manager.registerTool("cost", new CostCalculatorTool());
        rejects(() -> manager.registerTool("cost", new CostCalculatorTool()));
        rejects(() -> manager.execute("missing", "input")); rejects(() -> manager.execute("cost", "bad input"));
        try { new GeminiAdapter().generate("Do not send anywhere"); }
        catch (UnsupportedOperationException expected) {
            check(expected.getMessage().contains("PLANNED"), "Unavailable LLM must be explicit."); return;
        }
        throw new AssertionError("Unimplemented adapter must never fake a successful response.");
    }

    private static void constraintFinding(String transmission, String accepted, String history, String tolerance,
                                          String constraint, String severity) {
        var values = PreparationForm.example();
        values.put("budget", ""); values.put("maxMileage", "");
        values.put("transmission", transmission); values.put("acceptedTransmissions", accepted);
        values.put("accident", history); values.put("accidentTolerance", tolerance);
        var facade = ApplicationFactory.create();
        var result = PreparationForm.command(facade, values).execute();
        var findings = result.violations();
        check(findings.equals(facade.checkConstraints(facade.currentVehicle().orElseThrow())), "Saved preferences must drive the same checks.");
        if (severity == null) check(findings.isEmpty(), "Unexpected constraint finding: " + findings);
        else {
            check(findings.size() == 1 && findings.getFirst().constraint().equals(constraint)
                    && findings.getFirst().severity().equals(severity), "Unexpected findings: " + findings);
            check(ResultFormatter.format(result).contains(severity + ": " + constraint), "Finding must be visible in output.");
        }
    }

    public static void transmissionConflict() { constraintFinding("manual", "auto", "", "", "Transmission", "CONFLICT"); }
    public static void transmissionMatch() { constraintFinding(" AT ", " automatic ", "", "", "Transmission", null); }
    public static void multipleTransmissions() { constraintFinding("mt", "auto, MANUAL,auto", "", "", "Transmission", null); }
    public static void neutralTransmission() { constraintFinding("manual", "", "", "", "Transmission", null); }
    public static void unknownTransmission() {
        for (String unknown : new String[]{"", "UNKNOWN", "uncertain", "unlisted gearbox"})
            constraintFinding(unknown, "auto", "", "", "Transmission", "UNKNOWN");
    }
    public static void accidentConflict() { constraintFinding("auto", "", " yes ", " no_accidents ", "Accident history", "CONFLICT"); }
    public static void acceptableHistory() {
        constraintFinding("auto", "", "NO", "NO_ACCIDENTS", "Accident history", null);
        constraintFinding("auto", "", "YES", "ANY", "Accident history", null);
    }
    public static void neutralAccidentTolerance() { constraintFinding("auto", "", "YES", "", "Accident history", null); }
    public static void unknownAccidentHistory() {
        for (String tolerance : new String[]{"NO_ACCIDENTS", "ANY"})
            for (String history : new String[]{"", "UNKNOWN", "seller says probably fine"})
                constraintFinding("auto", "", history, tolerance, "Accident history", "UNKNOWN");
    }
    public static void invalidConstraintPreferences() {
        var values = PreparationForm.example(); values.put("acceptedTransmissions", "autmatic");
        rejects(() -> PreparationForm.command(ApplicationFactory.create(), values));
        values.put("acceptedTransmissions", "auto"); values.put("accidentTolerance", "maybe");
        rejects(() -> PreparationForm.command(ApplicationFactory.create(), values));
    }

    public static void cliFlow() {
        var bytes = new ByteArrayOutputStream();
        var facade = ApplicationFactory.create();
        var values = PreparationForm.example();
        values.put("acceptedTransmissions", "manual"); values.put("accidentTolerance", "NO_ACCIDENTS"); values.put("accident", "YES");
        String script = "prepare\n" + String.join("\n", values.values()) + "\nshow\nevaluate\nquit\n";
        new UsedCarCLI(facade, new Scanner(script), new PrintStream(bytes)).run();
        String output = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
        check(output.contains("24922.00") && output.contains("CONFLICT: Maximum budget"), "Interactive CLI must calculate entered input.");
        check(output.contains("CONFLICT: Transmission") && output.contains("CONFLICT: Accident history"), "CLI must invoke the new checks.");
        check(output.contains("PLANNED"), "AI status must be explicit.");
        check(facade.currentVehicle().isPresent(), "CLI must save the vehicle through the shared facade.");
    }

    private static Map<String, String> completeVehicle() {
        var values = PreparationForm.example();
        values.put("accident", "NO"); values.put("maintenance", "DOCUMENTED");
        return values;
    }

    private static ca.yorku.eecs3311.usedcar.result.MissingInformationResult quality(Map<String, String> values) {
        return PreparationForm.command(ApplicationFactory.create(), values).execute().missing();
    }

    public static void f04UnknownAccident() {
        var values = completeVehicle(); values.put("accident", "UNKNOWN");
        var result = quality(values);
        check(result.uncertainFields().equals(java.util.List.of("Accident history"))
                && result.missingFields().isEmpty() && !result.sufficient(), "Unknown accident history is uncertain, not absent or safe.");
    }

    public static void f04UnknownMaintenance() {
        var values = completeVehicle(); values.put("maintenance", "UNKNOWN");
        var result = quality(values);
        check(result.uncertainFields().equals(java.util.List.of("Maintenance history"))
                && result.missingFields().isEmpty() && !result.sufficient(), "Unknown maintenance must prevent sufficiency.");
    }

    public static void f04CompleteData() {
        var result = quality(completeVehicle());
        check(result.missingFields().isEmpty() && result.uncertainFields().isEmpty()
                && result.conflicts().isEmpty() && result.sufficient(), "Complete essential data should be sufficient.");
        var values = completeVehicle(); values.put("accident", "YES"); values.put("maintenance", "NONE");
        check(quality(values).sufficient(), "Known adverse history is data, not missing information or a safety verdict.");
    }

    public static void f04MissingHistory() {
        var values = completeVehicle(); values.put("accident", "   "); values.put("maintenance", null);
        var result = quality(values);
        check(result.missingFields().equals(java.util.List.of("Accident history", "Maintenance history"))
                && result.uncertainFields().isEmpty() && !result.sufficient(), "Blank/null histories must remain missing.");
    }

    public static void f04OptionalData() {
        var values = completeVehicle();
        for (String key : new String[]{"trim", "transmission", "fuelType", "consumption"}) values.put(key, "");
        var result = quality(values);
        check(result.sufficient() && result.missingFields().size() == 4, "Optional omissions must be reported without blocking essential sufficiency.");
        values.put("make", "UNKNOWN");
        check(!quality(values).sufficient() && quality(values).uncertainFields().contains("Make"), "Unknown essential identity must block sufficiency.");
    }

    public static void f04MatchingSources() {
        var facade = ApplicationFactory.create();
        var manual = PreparationForm.command(facade, completeVehicle()).execute().vehicle();
        var values = completeVehicle(); values.put("make", " example motors "); values.put("transmission", "AT");
        values.put("price", "18000.00"); values.put("consumption", "7.200"); values.put("accident", "ACCIDENT_FREE");
        values.put("notes", "Different source notes are not interpreted.");
        var extracted = PreparationForm.vehicle(facade, values);
        var result = facade.checkConflict(extracted);
        check(result.conflicts().isEmpty() && result.sufficient(), "Equivalent structured values must match despite case, aliases, and decimal scale.");
        check(facade.currentVehicle().orElseThrow().equals(manual) && extracted.make().equals("example motors"), "Checking must preserve both inputs.");
    }

    public static void f04ConflictingSources() {
        var facade = ApplicationFactory.create();
        var original = PreparationForm.command(facade, completeVehicle()).execute().vehicle();
        String[][] changes = {{"year", "2021", "Year"}, {"make", "Other", "Make"}, {"model", "Other", "Model"},
                {"trim", "Other", "Trim"}, {"mileage", "90000", "Mileage"}, {"price", "19000", "Asking price"},
                {"transmission", "manual", "Transmission"}, {"fuelType", "DIESEL", "Fuel type"},
                {"accident", "YES", "Accident history"}, {"maintenance", "PARTIAL", "Maintenance history"},
                {"consumption", "8.2", "Fuel consumption"}};
        for (var change : changes) {
            var values = completeVehicle(); values.put(change[0], change[1]);
            var result = facade.checkConflict(PreparationForm.vehicle(facade, values));
            check(result.conflicts().size() == 1 && result.conflicts().getFirst().startsWith(change[2] + " | manual=")
                    && result.conflicts().getFirst().contains(" | extracted=") && !result.sufficient(), "Expected one unresolved conflict for " + change[2]);
        }
        var values = completeVehicle(); values.put("price", "19000"); values.put("mileage", "90000");
        var result = facade.checkConflict(PreparationForm.vehicle(facade, values));
        check(result.conflicts().contains("Asking price | manual=18000 | extracted=19000")
                && result.conflicts().contains("Mileage | manual=80000 | extracted=90000"), "Both values of every conflict must remain visible.");
        check(facade.currentVehicle().orElseThrow().equals(original), "Conflict detection must not overwrite saved data.");
    }

    public static void f04UnknownSources() {
        var facade = ApplicationFactory.create();
        var manual = PreparationForm.command(facade, completeVehicle()).execute().vehicle();
        var values = completeVehicle();
        values.put("accident", "seller says probably fine"); values.put("maintenance", "UNKNOWN");
        values.put("transmission", "unlisted gearbox"); values.put("fuelType", "UNKNOWN");
        var extracted = PreparationForm.vehicle(facade, values);
        var checker = new DataQualityChecker();
        for (var result : java.util.List.of(checker.checkConflict(manual, extracted), checker.checkConflict(extracted, manual)))
            check(result.conflicts().isEmpty() && result.uncertainFields().size() == 4 && !result.sufficient(), "Unknown values must not become conflicts in either direction.");
        values.put("accident", ""); values.put("maintenance", "");
        var result = facade.checkConflict(PreparationForm.vehicle(facade, values));
        check(result.missingFields().contains("Extracted: Accident history") && result.conflicts().isEmpty(), "Missing values need source labels, not false conflicts.");
    }

    public static void f04CliConflictFlow() {
        var facade = ApplicationFactory.create(); var bytes = new ByteArrayOutputStream();
        var manual = completeVehicle(); var extracted = completeVehicle(); extracted.put("price", "19000");
        String vehicleInput = PreparationForm.FIELDS.stream().filter(f -> f.section().equals("Vehicle"))
                .map(f -> extracted.get(f.key())).collect(java.util.stream.Collectors.joining("\n"));
        String script = "prepare\n" + String.join("\n", manual.values()) + "\ncheck-conflicts\n" + vehicleInput + "\nquit\n";
        new UsedCarCLI(facade, new Scanner(script), new PrintStream(bytes)).run();
        String output = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
        check(output.contains("Asking price | manual=18000 | extracted=19000") && output.contains("Sufficient essential data: false")
                && output.contains("Missing fields:") && output.contains("Uncertain fields:"), "CLI must expose the shared F04 result.");
        equal(facade.currentVehicle().orElseThrow().askingPrice(), "18000");
    }

    public static void f04ConflictWithoutSession() {
        var facade = ApplicationFactory.create();
        var candidate = PreparationForm.vehicle(facade, completeVehicle());
        try { facade.checkConflict(candidate); }
        catch (IllegalStateException expected) {
            check(facade.currentVehicle().isEmpty(), "Comparison must not create a saved record."); return;
        }
        throw new AssertionError("Comparison requires a saved manual record.");
    }

    public static void f05ExactMatch() {
        var result = new VehicleKnowledgeRepository().findByVehicle("Example Motors", "Sample Sedan", 2020);
        check(result.summary().equals("Fictional sedan scenario for retrieval practice"), "Exact lookup must retrieve the CSV record.");
        check(result.commonIssues().size() == 2 && result.inspectionNotes().size() == 2
                && result.commonIssues().getFirst().equals("Sample concern: uneven tire wear")
                && result.sourceLabel().contains("Instructional sample only"), "CSV lists and source metadata must be preserved.");
    }

    public static void f05CaseMatch() {
        var repository = new VehicleKnowledgeRepository();
        check(repository.findByVehicle("EXAMPLE MOTORS", "sample sedan", 2020)
                .equals(repository.findByVehicle("Example Motors", "Sample Sedan", 2020)), "Matching must ignore case.");
    }

    public static void f05WhitespaceMatch() {
        var repository = new VehicleKnowledgeRepository();
        check(repository.findByVehicle("  Example Motors ", " Sample Sedan  ", 2020)
                .equals(repository.findByVehicle("Example Motors", "Sample Sedan", 2020)), "Matching must ignore surrounding whitespace.");
    }

    public static void f05YearBoundaries() {
        var repository = new VehicleKnowledgeRepository();
        for (int year : new int[]{2018, 2022})
            check(repository.findByVehicle("Example Motors", "Sample Sedan", year).summary().equals("Fictional sedan scenario for retrieval practice"), "Inclusive range boundary failed.");
        check(repository.findByVehicle("Example Motors", "Sample Sedan", 2023).summary().contains("later sedan"), "Adjacent range must select the appropriate record.");
        for (int year : new int[]{2017, 2026})
            check(repository.findByVehicle("Example Motors", "Sample Sedan", year).equals(ca.yorku.eecs3311.usedcar.result.VehicleKnowledge.noData()), "Outside range must not select a nearby record.");
    }

    public static void f05NoMatch() {
        var repository = new VehicleKnowledgeRepository();
        for (String[] identity : new String[][]{{"Unknown maker", "Sample Sedan"}, {"Example Motors", "Unknown model"}}) {
            var result = repository.findByVehicle(identity[0], identity[1], 2020);
            check(result.equals(ca.yorku.eecs3311.usedcar.result.VehicleKnowledge.noData())
                    && result.commonIssues().isEmpty() && result.inspectionNotes().isEmpty(), "No match must have explicit status and no invented notes.");
            check(ResultFormatter.format(result).contains("No local vehicle knowledge available."), "No-data status must be visible.");
        }
    }

    public static void f05MissingFile() {
        var path = java.nio.file.Path.of("build", "missing-knowledge-" + java.util.UUID.randomUUID() + ".csv");
        try { new VehicleKnowledgeRepository(path).load(); }
        catch (IllegalStateException expected) {
            check(expected.getMessage().contains("Cannot load local vehicle knowledge") && expected.getMessage().contains(path.toString()), "Missing-file error must identify the source."); return;
        }
        throw new AssertionError("Missing file must not appear as an empty successful repository.");
    }

    public static void f05MalformedCsv() {
        String header = "make,model,yearFrom,yearTo,summary,commonIssues,inspectionNotes,sourceLabel\n";
        String valid = " Fixture Maker , Fixture Model ,2000,2005,Fixture summary,Concern one|Concern two,Inspect sample,Test fixture\n";
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("build", "verification"));
            var file = java.nio.file.Files.createTempFile(java.nio.file.Path.of("build", "verification"), "knowledge-", ".csv");
            try {
                java.nio.file.Files.writeString(file, header + valid);
                var repository = new VehicleKnowledgeRepository(file); repository.load();
                check(repository.findByVehicle("fixture maker", "fixture model", 2000).commonIssues().size() == 2, "Filesystem CSV and stored-name normalization must work.");
                for (String bad : new String[]{"wrong header\n", header + "too,few,columns\n",
                        header + valid.replace("2000", "oops"), header + valid.replace("2005", "1999"),
                        header + valid.replace("Fixture summary", ""), header + valid.replace("Concern one|Concern two", "One||Two"),
                        header + valid.replace("Fixture summary", "\"quoted, summary\""), header + valid + valid}) {
                    java.nio.file.Files.writeString(file, bad);
                    try { repository.load(); throw new AssertionError("Malformed CSV was accepted: " + bad); }
                    catch (IllegalStateException expected) {
                        check(expected.getMessage().contains("Malformed CSV at line"), "Malformed-file error must identify the line.");
                    }
                    try { repository.findByVehicle("fixture maker", "fixture model", 2000); throw new AssertionError("Failed reload served stale data."); }
                    catch (IllegalStateException expected) { /* Explicit failure is required. */ }
                }
            } finally { java.nio.file.Files.deleteIfExists(file); }
        } catch (java.io.IOException e) { throw new AssertionError(e); }
    }

    public static void f05ToolDispatch() {
        var manager = new ToolManager();
        manager.registerTool("vehicleKnowledge", new VehicleKnowledgeTool(new VehicleKnowledgeRepository()));
        var vehicle = PreparationForm.vehicle(ApplicationFactory.create(), PreparationForm.example());
        check(manager.execute("vehicleKnowledge", vehicle).equals(new VehicleKnowledgeRepository()
                .findByVehicle(vehicle.make(), vehicle.model(), vehicle.year())), "ToolManager must dispatch the real knowledge lookup.");
        rejects(() -> manager.execute("vehicleKnowledge", "not a vehicle"));
    }

    public static void f05FacadeIntegration() {
        var facade = ApplicationFactory.create();
        try { facade.retrieveKnowledge(); throw new AssertionError("Lookup without a saved vehicle must fail."); }
        catch (IllegalStateException expected) { check(expected.getMessage().contains("Save a vehicle"), "Missing session should be explained."); }
        var saved = PreparationForm.command(facade, PreparationForm.example()).execute().vehicle();
        check(facade.retrieveKnowledge().equals(facade.retrieveKnowledge(saved))
                && facade.retrieveKnowledge().summary().contains("Fictional sedan"), "Facade must reach the repository through the agent and tool.");
        check(facade.currentVehicle().orElseThrow().equals(saved), "Knowledge retrieval must preserve the vehicle.");
    }

    public static void f05CliKnowledge() {
        var bytes = new ByteArrayOutputStream();
        var facade = ApplicationFactory.create();
        new UsedCarCLI(facade, new Scanner("knowledge\ndemo\nknowledge\nquit\n"), new PrintStream(bytes)).run();
        String output = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
        check(output.contains("ERROR: Save a vehicle first.") && output.contains("Fictional sedan scenario")
                && output.contains("Sample concern: uneven tire wear") && output.contains("Instructional sample only"), "CLI knowledge must retrieve and display labeled CSV content.");
    }

    private static Map<String, String> completeProfile() {
        var values = new java.util.LinkedHashMap<String, String>();
        values.put("budget", "25000.50"); values.put("maxMileage", "100000");
        values.put("acceptedTransmissions", " at, CVT,auto "); values.put("accidentTolerance", " no_accidents ");
        values.put("preferredBodyType", " SUV "); values.put("reliabilityPriority", "5"); values.put("fuelEconomyPriority", "3");
        return values;
    }

    public static void f03CompleteProfile() {
        var facade = ApplicationFactory.create(); facade.savePreferences(PreparationForm.preferences(completeProfile()));
        var profile = facade.preferences();
        equal(profile.maxBudget(), "25000.50");
        check(profile.maxMileage() == 100000 && profile.acceptedTransmissions().equals(java.util.Set.of("AUTOMATIC", "CVT"))
                && profile.preferredBodyType().equals("SUV") && profile.reliabilityPriority() == 5
                && profile.fuelEconomyPriority() == 3 && profile.accidentTolerance().equals("NO_ACCIDENTS"), "All seven fields must be normalized and saved.");
        check(facade.currentVehicle().isEmpty(), "Independent profile save must not create a vehicle.");
        var values = PreparationForm.example(); values.putAll(completeProfile());
        check(PreparationForm.command(facade, values).execute().preference().equals(profile), "Combined preparation must preserve all seven submitted fields.");
    }

    public static void f03NeutralProfile() {
        var profile = PreparationForm.preferences(Map.of());
        check(profile.equals(UserPreference.limits(null, null)), "Omitted fields must stay neutral.");
        var values = completeProfile(); values.replaceAll((key, value) -> " ");
        check(PreparationForm.preferences(values).equals(profile), "Blank fields must clear to neutral.");
        values.put("budget", "0"); values.put("maxMileage", "0"); values.put("accidentTolerance", "ANY");
        var explicit = PreparationForm.preferences(values);
        check(explicit.maxBudget().signum() == 0 && explicit.maxMileage() == 0 && explicit.accidentTolerance().equals("ANY"), "Explicit zero limits and ANY must remain distinct from missing values.");
    }

    private static void rejectsProfile(String field, String... invalid) {
        for (String value : invalid) {
            var values = completeProfile(); values.put(field, value);
            rejects(() -> PreparationForm.preferences(values));
        }
    }
    public static void f03BudgetValidation() { rejectsProfile("budget", "-1", "abc", "NaN", "1e9"); }
    public static void f03MileageValidation() { rejectsProfile("maxMileage", "-1", "1.5", "oops", "999999999999"); }
    public static void f03PriorityValidation() {
        for (String field : new String[]{"reliabilityPriority", "fuelEconomyPriority"}) {
            rejectsProfile(field, "-1", "6", "2.5", "high");
            for (String value : new String[]{"0", "5"}) {
                var values = completeProfile(); values.put(field, value); PreparationForm.preferences(values);
            }
        }
    }
    public static void f03TransmissionValidation() {
        rejectsProfile("acceptedTransmissions", "automatic,spaceship", "autmatic");
        rejectsProfile("accidentTolerance", "MAYBE", "YES");
    }

    public static void f03ValidUpdate() {
        var facade = ApplicationFactory.create(); facade.savePreferences(PreparationForm.preferences(completeProfile()));
        var values = completeProfile(); values.put("budget", "20000"); values.put("preferredBodyType", "Hatchback");
        facade.savePreferences(PreparationForm.preferences(values));
        equal(facade.preferences().maxBudget(), "20000");
        check(facade.preferences().preferredBodyType().equals("Hatchback"), "Valid updates must replace saved values.");
    }

    public static void f03RejectedUpdate() {
        var facade = ApplicationFactory.create(); facade.savePreferences(PreparationForm.preferences(completeProfile()));
        var prior = facade.preferences();
        for (String[] invalid : new String[][]{{"budget", "-1"}, {"maxMileage", "-1"}, {"acceptedTransmissions", "bad"},
                {"reliabilityPriority", "6"}, {"fuelEconomyPriority", "bad"}, {"accidentTolerance", "bad"}}) {
            var values = completeProfile(); values.put(invalid[0], invalid[1]);
            rejects(() -> facade.savePreferences(PreparationForm.preferences(values)));
            check(facade.preferences().equals(prior), "Rejected update corrupted the saved profile.");
        }
    }

    public static void f03VehiclePreservation() {
        var facade = ApplicationFactory.create();
        var vehicle = PreparationForm.command(facade, PreparationForm.example()).execute().vehicle();
        facade.savePreferences(PreparationForm.preferences(completeProfile()));
        check(facade.currentVehicle().orElseThrow().equals(vehicle), "Independent preference save must retain the exact current vehicle.");
    }

    public static void f03ProfileRetrieval() {
        var facade = ApplicationFactory.create(); var profile = PreparationForm.preferences(completeProfile());
        facade.savePreferences(profile);
        check(PreparationForm.preferences(PreparationForm.preferenceValues(facade.preferences())).equals(profile), "Reloading saved fields must round-trip the profile.");
        String text = ResultFormatter.format(facade.preferences());
        check(text.contains("25000.50") && text.contains("100000") && text.contains("SUV")
                && text.contains("Reliability priority: 5") && text.contains("Fuel economy priority: 3")
                && text.contains("NO_ACCIDENTS") && text.contains("AUTOMATIC"), "Display must expose all saved preferences.");
        try { facade.preferences().acceptedTransmissions().add("MANUAL"); throw new AssertionError("Profile collection must be immutable."); }
        catch (UnsupportedOperationException expected) { /* Encapsulation protects session state. */ }
    }

    public static void f03CliProfile() {
        var facade = ApplicationFactory.create(); var bytes = new ByteArrayOutputStream();
        String valid = String.join("\n", completeProfile().values());
        var invalid = completeProfile(); invalid.put("budget", "-1");
        String script = "profile-set\n" + valid + "\nprofile-set\n" + String.join("\n", invalid.values())
                + "\nprofile-show\nprofile-set\n20000\n:keep\n:keep\n:keep\n:keep\n:keep\n:keep\nprofile-show\nprofile-set\n:cancel\nquit\n";
        new UsedCarCLI(facade, new Scanner(script), new PrintStream(bytes)).run();
        String output = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
        check(output.contains("ERROR:") && output.contains("Maximum budget (CAD): 25000.50")
                && output.contains("Maximum budget (CAD): 20000") && output.contains("Preferred body type: SUV"), "CLI must support saving, showing, rejection and correction.");
        equal(facade.preferences().maxBudget(), "20000");
        check(facade.preferences().reliabilityPriority() == 5 && facade.currentVehicle().isEmpty(), "Keep/cancel must preserve profile fields without a vehicle.");
        new UsedCarCLI(facade, new Scanner("profile-set\n100\n"), new PrintStream(bytes)).run();
        equal(facade.preferences().maxBudget(), "20000");
    }

    public static void f03F07Compatibility() {
        var facade = ApplicationFactory.create(); var values = completeVehicle(); values.put("accident", "YES");
        var vehicle = PreparationForm.command(facade, values).execute().vehicle();
        facade.savePreferences(PreparationForm.preferences(Map.of()));
        check(facade.checkConstraints(vehicle).isEmpty(), "Neutral updated preferences must skip checks.");
        var profile = completeProfile(); profile.put("budget", "17000"); profile.put("maxMileage", "70000"); profile.put("acceptedTransmissions", "MANUAL");
        facade.savePreferences(PreparationForm.preferences(profile));
        var findings = facade.checkConstraints(vehicle);
        check(findings.size() == 4 && findings.stream().allMatch(f -> f.severity().equals("CONFLICT")), "All four F07 checks must consume the newly saved profile.");
        check(facade.currentVehicle().orElseThrow().equals(vehicle), "Constraint checking must retain vehicle data.");
    }

    public static void main(String[] args) {
        numericValidation(); requiredFieldsAndYear(); exactCostCalculation(); omittedAssumptions(); unknownConsumption();
        constraintBoundaries(); zeroAndRounding(); sessionAndInvalidUpdate(); toolContractsAndLlmBoundary(); cliFlow();
        transmissionConflict(); transmissionMatch(); multipleTransmissions(); neutralTransmission(); unknownTransmission();
        accidentConflict(); acceptableHistory(); neutralAccidentTolerance(); unknownAccidentHistory(); invalidConstraintPreferences();
        f04UnknownAccident(); f04UnknownMaintenance(); f04CompleteData(); f04MissingHistory(); f04OptionalData();
        f04MatchingSources(); f04ConflictingSources(); f04UnknownSources(); f04CliConflictFlow(); f04ConflictWithoutSession();
        f05ExactMatch(); f05CaseMatch(); f05WhitespaceMatch(); f05YearBoundaries(); f05NoMatch();
        f05MissingFile(); f05MalformedCsv(); f05ToolDispatch(); f05FacadeIntegration(); f05CliKnowledge();
        f03CompleteProfile(); f03NeutralProfile(); f03BudgetValidation(); f03MileageValidation(); f03PriorityValidation();
        f03TransmissionValidation(); f03ValidUpdate(); f03RejectedUpdate(); f03VehiclePreservation();
        f03ProfileRetrieval(); f03CliProfile(); f03F07Compatibility();
        System.out.println("PASS: 52 deterministic verification groups (dependency-free runner; not JUnit/Stage 3 completion).");
    }
}
