package ca.yorku.eecs3311.usedcar.result;

import java.math.BigDecimal;
import java.util.List;

public record CostEstimate(BigDecimal annualFuelUse, BigDecimal annualFuelCost,
                           BigDecimal purchaseTax, BigDecimal firstYearEstimate, List<String> unavailable) {
    public CostEstimate { unavailable = List.copyOf(unavailable); }
}
