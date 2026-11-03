package ca.yorku.eecs3311.usedcar.ui;

import ca.yorku.eecs3311.usedcar.result.PreparationResult;
import ca.yorku.eecs3311.usedcar.result.MissingInformationResult;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ResultFormatter {
    private ResultFormatter() { }
    private static String amount(BigDecimal value) { return value == null ? "unavailable" : value.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private static String supplied(Object value) { return value == null ? "not supplied" : value.toString(); }

    public static String format(MissingInformationResult result) {
        return "Missing fields: " + result.missingFields() + "\n"
                + "Uncertain fields: " + result.uncertainFields() + "\n"
                + "Source conflicts: " + result.conflicts() + "\n"
                + "Sufficient essential data: " + result.sufficient() + "\n"
                + "Data sufficiency is not a safety or reliability judgment. No source values are merged.\n";
    }

    public static String format(ca.yorku.eecs3311.usedcar.result.VehicleKnowledge result) {
        return "LOCAL VEHICLE KNOWLEDGE\n" + result.summary() + "\n"
                + "Common issues: " + result.commonIssues() + "\n"
                + "Inspection notes: " + result.inspectionNotes() + "\n"
                + "Source: " + result.sourceLabel() + "\n"
                + "Retrieved notes only; no AI risk assessment or purchase recommendation.\n";
    }

    public static String format(ca.yorku.eecs3311.usedcar.domain.UserPreference preference) {
        return "SAVED BUYER PREFERENCES\n"
                + "Maximum budget (CAD): " + supplied(preference.maxBudget()) + "\n"
                + "Maximum mileage (km): " + supplied(preference.maxMileage()) + "\n"
                + "Accepted transmissions: " + new java.util.TreeSet<>(preference.acceptedTransmissions()) + " (empty = neutral)\n"
                + "Preferred body type: " + (preference.preferredBodyType().isEmpty() ? "not supplied" : preference.preferredBodyType()) + "\n"
                + "Reliability priority: " + preference.reliabilityPriority() + "\n"
                + "Fuel economy priority: " + preference.fuelEconomyPriority() + " (priorities: 0 = neutral; 1-5 increasing importance)\n"
                + "Accident tolerance: " + preference.accidentTolerance() + " (UNKNOWN = neutral)\n"
                + "Body type and priorities are stored; scoring and recommendation use remains planned.\n";
    }

    public static String format(PreparationResult result) {
        var v = result.vehicle(); var a = result.assumptions(); var c = result.cost();
        var text = new StringBuilder("DETERMINISTIC PREPARATION ONLY - no AI risk assessment or purchase recommendation\n");
        text.append(v.year()).append(' ').append(v.make()).append(' ').append(v.model())
                .append(" | ").append(v.mileage()).append(" km | CAD ").append(amount(v.askingPrice())).append('\n')
                .append("Transmission: ").append(v.transmission()).append(" | Fuel type: ").append(v.fuelType()).append('\n')
                .append("Accident history: ").append(v.accidentHistory()).append(" | Maintenance history: ").append(v.maintenanceHistory()).append('\n')
                .append("Notes: ").append(v.notes()).append('\n')
                .append("Saved buyer limits: budget=").append(supplied(result.preference().maxBudget()))
                .append(" CAD; mileage=").append(supplied(result.preference().maxMileage())).append(" km\n")
                .append("Accepted transmissions: ").append(result.preference().acceptedTransmissions())
                .append(" (empty = neutral); accident tolerance: ").append(result.preference().accidentTolerance()).append("\n\n")
                .append("Explicit assumptions: ").append(a.annualKm()).append(" km/year; ").append(supplied(v.fuelConsumption()))
                .append(" L/100 km; CAD ").append(a.fuelPrice()).append("/L\n")
                .append("Tax fraction: ").append(supplied(a.taxRate())).append("; maintenance CAD ").append(supplied(a.annualMaintenance()))
                .append("; insurance CAD ").append(supplied(a.annualInsurance())).append('\n')
                .append("Annual fuel use (L): ").append(amount(c.annualFuelUse())).append('\n')
                .append("Annual fuel cost (CAD): ").append(amount(c.annualFuelCost())).append('\n')
                .append("Purchase tax (CAD): ").append(amount(c.purchaseTax())).append('\n')
                .append("First-year estimate (CAD): ").append(amount(c.firstYearEstimate())).append('\n')
                .append("Formula: price + tax + annual fuel + maintenance + insurance. Excludes financing, depreciation, registration and other unlisted costs.\n");
        c.unavailable().forEach(item -> text.append("UNAVAILABLE: ").append(item).append('\n'));
        text.append("\nChecks cover budget, mileage, accepted transmissions and accident tolerance. UNKNOWN means not evaluable, not a confirmed conflict or pass.\n");
        if (result.violations().isEmpty()) text.append("No conflicts or unresolved checks among the supplied constraints; unspecified preferences are skipped.\n");
        result.violations().forEach(item -> text.append(item.severity()).append(": ").append(item.constraint())
                .append(" | expected=").append(item.expected()).append(" | actual=").append(item.actual()).append('\n'));
        text.append(format(result.missing()))
                .append(result.llmStatus()).append('\n').append("Session is in memory only; closing the application discards it.\n");
        return text.toString();
    }
    public static String format(ca.yorku.eecs3311.usedcar.result.RiskEvaluation evaluation) {
        var out=new StringBuilder("F08 RISK ASSESSMENT - ").append(evaluation.status()).append("\n").append(evaluation.message()).append("\n");
        var risk=evaluation.assessment();
        if(risk!=null)out.append("Risk level: ").append(risk.riskLevel()).append("\nStrengths: ").append(risk.strengths())
                .append("\nRisks: ").append(risk.risks()).append("\nUncertainties: ").append(risk.uncertainties()).append("\nExplanation: ").append(risk.explanation()).append("\n");
        else out.append("No validated AI risk category is available.\n");
        var e=evaluation.evidence();
        out.append("\nDETERMINISTIC EVIDENCE SNAPSHOT (not overwritten by AI)\n")
                .append("Vehicle: ").append(e.vehicle()).append("\nSaved preferences: ").append(e.preference()).append("\n")
                .append(format(e.missing())).append("\nConstraints: ").append(e.violations()).append("\nConstraint tool available: ").append(e.constraintsAvailable())
                .append("\nLocal knowledge: ").append(e.knowledge()==null?"UNAVAILABLE":e.knowledge())
                .append("\nCost estimate: ").append(e.cost()==null?"UNAVAILABLE":e.cost())
                .append("\nPriority guidance: ").append(e.priorityGuidance()).append("\nLimitations: ").append(e.limitations());
        return out.toString();
    }
    public static String format(ca.yorku.eecs3311.usedcar.result.ComparisonEvaluation evaluation) {
        var result=evaluation.comparison(); var evidence=evaluation.evidence();
        var out=new StringBuilder("F09 TWO-VEHICLE COMPARISON - ").append(evaluation.status()).append("\n").append(evaluation.message()).append("\n");
        out.append("Vehicle A ID: ").append(result.firstVehicleId()).append("\nVehicle B ID: ").append(result.secondVehicleId())
                .append("\nDETERMINISTIC SIDE-BY-SIDE METRICS (CAD / km / L/100 km)\n");
        result.metricSummary().forEach((key,value)->out.append(key).append(": ").append(value).append("\n"));
        out.append("Common cost assumptions: ").append(evidence.assumptions()==null?"UNAVAILABLE":evidence.assumptions()).append("\n")
                .append("Costs exclude financing, depreciation, registration and other unlisted expenses. Equal allowances are scenario assumptions.\n");
        int i=0;
        for(var side:java.util.List.of(evidence.first(),evidence.second())) {
            out.append("\nVehicle ").append(i++==0?"A":"B").append(" EVIDENCE\n")
                    .append("Buyer preferences: ").append(side.preference()).append("\n").append(format(side.missing()))
                    .append("Constraints: ").append(side.constraintsAvailable()?side.violations():"UNAVAILABLE").append("\n")
                    .append("Local knowledge: ").append(side.knowledge()==null?"UNAVAILABLE":side.knowledge()).append("\n")
                    .append("Cost details: ").append(side.cost()==null?"UNAVAILABLE":side.cost()).append("\n")
                    .append("Uncertainties/source limits: ").append(side.requiredUncertainties()).append("\n");
        }
        if(result.explanation().isBlank()) out.append("\nAI narrative: UNAVAILABLE. No preferred vehicle identified.\n");
        else out.append("\nAI trade-off explanation (advisory): ").append(result.explanation())
                .append("\nPreferred vehicle ID: ").append(result.preferredVehicleId().isEmpty()?"NONE - no defensible preference":result.preferredVehicleId()).append("\n");
        return out.toString();
    }
}
