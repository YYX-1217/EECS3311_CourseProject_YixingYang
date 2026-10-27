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

    public static void main(String[] args) {
        numericValidation(); requiredFieldsAndYear(); exactCostCalculation(); omittedAssumptions(); unknownConsumption();
        constraintBoundaries(); zeroAndRounding(); sessionAndInvalidUpdate(); toolContractsAndLlmBoundary(); cliFlow();
        transmissionConflict(); transmissionMatch(); multipleTransmissions(); neutralTransmission(); unknownTransmission();
        accidentConflict(); acceptableHistory(); neutralAccidentTolerance(); unknownAccidentHistory(); invalidConstraintPreferences();
        System.out.println("PASS: 20 deterministic verification groups (dependency-free runner; not JUnit/Stage 3 completion).");
    }
}
