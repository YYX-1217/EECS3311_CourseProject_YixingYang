package ca.yorku.eecs3311.usedcar.domain;

import java.math.BigDecimal;

/** User-supplied CAD assumptions. Null optional amounts mean unknown, not zero. */
public record CostAssumptions(BigDecimal annualKm, BigDecimal fuelPrice, BigDecimal taxRate,
                              BigDecimal annualMaintenance, BigDecimal annualInsurance) {
    public CostAssumptions {
        annualKm = Values.nonNegative(annualKm, "Annual distance");
        fuelPrice = Values.nonNegative(fuelPrice, "Fuel price");
        taxRate = Values.optionalNonNegative(taxRate, "Tax rate");
        if (taxRate != null && taxRate.compareTo(BigDecimal.ONE) > 0)
            throw new IllegalArgumentException("Tax rate must be a fraction from 0 to 1 (for example 0.13).");
        annualMaintenance = Values.optionalNonNegative(annualMaintenance, "Maintenance allowance");
        annualInsurance = Values.optionalNonNegative(annualInsurance, "Insurance allowance");
    }
}
