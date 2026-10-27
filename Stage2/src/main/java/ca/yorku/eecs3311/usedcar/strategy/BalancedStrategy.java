package ca.yorku.eecs3311.usedcar.strategy;

import ca.yorku.eecs3311.usedcar.domain.UserPreference;
import java.util.Objects;

public final class BalancedStrategy implements EvaluationStrategy {
    @Override public String buildPriorityGuidance(UserPreference preference) {
        var p = Objects.requireNonNull(preference, "Buyer preferences are required");
        String emphasis = p.reliabilityPriority() == 0 || p.fuelEconomyPriority() == 0
                ? "Do not infer a relative preference when either importance is unspecified."
                : p.reliabilityPriority() == p.fuelEconomyPriority()
                ? "No relative reliability-versus-fuel-economy preference is inferred from equal priority values."
                : p.reliabilityPriority() > p.fuelEconomyPriority()
                    ? "In trade-off explanations give reliability more emphasis than fuel economy, as explicitly rated."
                    : "In trade-off explanations give fuel economy more emphasis than reliability, while preserving risk warnings.";
        return PriorityGuidance.heading("Balanced")
                + "Balance budget, reliability, mileage, vehicle risk and ownership costs; explain trade-offs instead of optimizing price alone.\n"
                + PriorityGuidance.budget(p) + "\n" + PriorityGuidance.reliability(p) + "\n"
                + PriorityGuidance.economy(p) + "\n" + emphasis + PriorityGuidance.finish(p);
    }
}
