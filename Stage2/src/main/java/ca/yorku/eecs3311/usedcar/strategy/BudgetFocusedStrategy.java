package ca.yorku.eecs3311.usedcar.strategy;

import ca.yorku.eecs3311.usedcar.domain.UserPreference;
import java.util.Objects;

public final class BudgetFocusedStrategy implements EvaluationStrategy {
    @Override public String buildPriorityGuidance(UserPreference preference) {
        var p = Objects.requireNonNull(preference, "Buyer preferences are required");
        return PriorityGuidance.heading("Budget Focused")
                + "Discuss purchase price and ownership costs first, separating upfront affordability from recurring costs.\n"
                + PriorityGuidance.budget(p) + "\n"
                + (p.maxBudget() == null ? "Request a budget before judging affordability; cost focus does not supply a missing limit."
                    : "Show any purchase-budget excess explicitly; do not silently reinterpret the purchase budget as a total-cost budget.")
                + "\nUse only supplied cost assumptions; missing maintenance, insurance or fuel inputs cannot be treated as zero.\n"
                + PriorityGuidance.economy(p) + "\n" + PriorityGuidance.reliability(p) + PriorityGuidance.finish(p);
    }
}
