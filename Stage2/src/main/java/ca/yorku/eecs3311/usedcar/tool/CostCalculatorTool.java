package ca.yorku.eecs3311.usedcar.tool;

import ca.yorku.eecs3311.usedcar.domain.*;
import ca.yorku.eecs3311.usedcar.result.CostEstimate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Objects;

public final class CostCalculatorTool implements Tool {
    public record Input(Vehicle vehicle, CostAssumptions assumptions) {
        public Input { Objects.requireNonNull(vehicle); Objects.requireNonNull(assumptions); }
    }

    @Override public CostEstimate execute(Object input) {
        if (!(input instanceof Input request)) throw new IllegalArgumentException("Cost tool requires CostCalculatorTool.Input.");
        Vehicle v = request.vehicle();
        CostAssumptions a = request.assumptions();
        var missing = new ArrayList<String>();
        BigDecimal litres = null;
        BigDecimal fuel = null;
        if (v.fuelConsumption() != null) {
            litres = a.annualKm().multiply(v.fuelConsumption()).movePointLeft(2);
            fuel = calculateAnnualFuelCost(a.annualKm(), v.fuelConsumption(), a.fuelPrice());
        } else missing.add("Fuel consumption: annual fuel cost and first-year total unavailable.");
        BigDecimal tax = a.taxRate() == null ? null : money(v.askingPrice().multiply(a.taxRate()));
        if (tax == null) missing.add("Purchase tax rate not supplied.");
        if (a.annualMaintenance() == null) missing.add("Annual maintenance allowance not supplied.");
        if (a.annualInsurance() == null) missing.add("Annual insurance allowance not supplied.");
        BigDecimal total = missing.isEmpty()
                ? money(v.askingPrice()).add(fuel).add(tax).add(money(a.annualMaintenance())).add(money(a.annualInsurance()))
                : null;
        return new CostEstimate(litres, fuel, tax, total, missing);
    }

    public BigDecimal calculateAnnualFuelCost(BigDecimal annualKm, BigDecimal fuelConsumption, BigDecimal fuelPrice) {
        return money(Values.nonNegative(annualKm, "Annual distance")
                .multiply(Values.nonNegative(fuelConsumption, "Fuel consumption")).movePointLeft(2)
                .multiply(Values.nonNegative(fuelPrice, "Fuel price")));
    }

    private static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
