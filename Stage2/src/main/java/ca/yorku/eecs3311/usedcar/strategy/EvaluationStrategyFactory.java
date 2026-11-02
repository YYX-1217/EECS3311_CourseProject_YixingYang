package ca.yorku.eecs3311.usedcar.strategy;

import ca.yorku.eecs3311.usedcar.domain.UserPreference;
import java.util.Locale;

/** Stage 1 argument-based Simple Factory; not the subclass-based GoF Factory Method. */
public final class EvaluationStrategyFactory {
    /** Null or unspecified priorities are neutral. Fuel economy is not budget priority. */
    public EvaluationStrategy createStrategy(UserPreference preference) {
        return prefersReliability(preference) ? new ReliabilityFocusedStrategy() : new BalancedStrategy();
    }

    private boolean prefersReliability(UserPreference preference) {
        return preference != null && preference.fuelEconomyPriority() > 0
                && preference.reliabilityPriority() > preference.fuelEconomyPriority();
    }

    /** Explain the same selection policy without inferring any vehicle evidence. */
    public String selectionBasis(UserPreference preference) {
        int reliability = preference == null ? 0 : preference.reliabilityPriority();
        int economy = preference == null ? 0 : preference.fuelEconomyPriority();
        return "Selection mode: automatic. Basis: reliability=" + reliability + "/5, fuel economy=" + economy
                + "/5 (0 = unspecified). "
                + (prefersReliability(preference)
                    ? "Both priorities are specified and reliability is strictly higher."
                    : "No supported specialized preference: missing, tied or economy-led priorities use Balanced.")
                + " A budget limit is a constraint; fuel economy is not budget priority. Budget focus requires manual selection.\n";
    }

    /** Explicit user choice overrides inference; construction remains centralized here. */
    public EvaluationStrategy createManualStrategy(String mode) {
        if (mode == null) throw new IllegalArgumentException("Strategy must be auto, balanced, budget or reliability.");
        return switch (mode.strip().toLowerCase(Locale.ROOT)) {
            case "balanced" -> new BalancedStrategy();
            case "budget" -> new BudgetFocusedStrategy();
            case "reliability" -> new ReliabilityFocusedStrategy();
            default -> throw new IllegalArgumentException("Strategy must be auto, balanced, budget or reliability.");
        };
    }
}
