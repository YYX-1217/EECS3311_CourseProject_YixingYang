package ca.yorku.eecs3311.usedcar.ui;

import ca.yorku.eecs3311.usedcar.result.PreparationResult;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ResultFormatter {
    private ResultFormatter() { }
    private static String amount(BigDecimal value) { return value == null ? "unavailable" : value.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private static String supplied(Object value) { return value == null ? "not supplied" : value.toString(); }

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
        text.append("Unknown fields: ").append(result.missing().missingFields()).append('\n')
                .append("Data readiness is a basic field check, not a reliability judgment.\n")
                .append(result.llmStatus()).append('\n').append("Session is in memory only; closing the application discards it.\n");
        return text.toString();
    }
}
