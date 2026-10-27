package ca.yorku.eecs3311.usedcar.strategy;

import ca.yorku.eecs3311.usedcar.domain.UserPreference;
import java.util.Objects;

public final class ReliabilityFocusedStrategy implements EvaluationStrategy {
    @Override public String buildPriorityGuidance(UserPreference preference) {
        var p = Objects.requireNonNull(preference, "Buyer preferences are required");
        return PriorityGuidance.heading("Reliability Focused")
                + "Discuss reliability evidence, maintenance uncertainty, inspection concerns and vehicle risks before cost advantages.\n"
                + PriorityGuidance.reliability(p) + "\n"
                + (p.reliabilityPriority() == 0 ? "The explicit mode orders the discussion; it does not invent a numeric reliability preference."
                    : "Use the stated importance to explain reliability trade-offs; it is not a mechanical-condition score.")
                + "\nIdentify missing service evidence and inspection needs; missing history is neither proof of safety nor proof of failure.\n"
                + PriorityGuidance.budget(p) + "\n" + PriorityGuidance.economy(p) + PriorityGuidance.finish(p);
    }
}
