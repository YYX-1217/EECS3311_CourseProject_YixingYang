package ca.yorku.eecs3311.usedcar.app;

import ca.yorku.eecs3311.usedcar.agent.UsedCarAgent;
import ca.yorku.eecs3311.usedcar.domain.*;
import ca.yorku.eecs3311.usedcar.result.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;
import java.util.Locale;
import ca.yorku.eecs3311.usedcar.strategy.*;

/** Shared application API for both interfaces. No UI code or provider protocol here. */
public final class UsedCarApplicationFacade {
    // Fixed stateless instances for explicit preview selection; no automatic Factory policy.
    private static final Map<String, EvaluationStrategy> STRATEGIES = Map.of(
            "balanced", new BalancedStrategy(), "budget", new BudgetFocusedStrategy(),
            "reliability", new ReliabilityFocusedStrategy());
    private final UsedCarAgent agent;
    private final EvaluationSession session;

    public UsedCarApplicationFacade(UsedCarAgent agent, EvaluationSession session) {
        this.agent = Objects.requireNonNull(agent);
        this.session = Objects.requireNonNull(session);
    }

    public Vehicle saveVehicle(Vehicle vehicle) { session.setCurrentVehicle(vehicle); return vehicle; }
    public void savePreferences(UserPreference preference) { session.setPreference(preference); }
    public Optional<Vehicle> currentVehicle() { return session.getCurrentVehicle(); }
    public UserPreference preferences() { return session.getPreference(); }
    public VehicleKnowledge retrieveKnowledge(Vehicle vehicle) { return agent.retrieveKnowledge(vehicle); }
    public VehicleKnowledge retrieveKnowledge() {
        return retrieveKnowledge(currentVehicle().orElseThrow(() -> new IllegalStateException("Save a vehicle first.")));
    }
    public List<String> evaluationModes() { return List.of("balanced", "budget", "reliability"); }
    public void selectStrategy(String mode) {
        EvaluationStrategy selected = mode == null ? null : STRATEGIES.get(mode.strip().toLowerCase(Locale.ROOT));
        if (selected == null) throw new IllegalArgumentException("Strategy must be balanced, budget or reliability.");
        agent.setStrategy(selected);
    }
    public String previewPriorityGuidance() { return agent.buildPriorityGuidance(preferences()); }
    public String llmStatus() { return agent.llmStatus(); }
    public MissingInformationResult checkMissingInformation(Vehicle vehicle) { return agent.checkMissingInformation(vehicle); }
    public MissingInformationResult checkConflict(Vehicle extractedVehicle) {
        Vehicle manualVehicle = currentVehicle().orElseThrow(() -> new IllegalStateException("Save a manual vehicle first."));
        return agent.checkConflict(manualVehicle, extractedVehicle);
    }
    public List<ConstraintViolation> checkConstraints(Vehicle vehicle) { return agent.checkConstraints(vehicle, preferences()); }
    public CostEstimate estimateCost(Vehicle vehicle, double annualKm, double fuelPrice) {
        if (!Double.isFinite(annualKm) || !Double.isFinite(fuelPrice))
            throw new IllegalArgumentException("Annual distance and fuel price must be finite numbers.");
        return estimateCost(vehicle, new CostAssumptions(BigDecimal.valueOf(annualKm), BigDecimal.valueOf(fuelPrice), null, null, null));
    }
    public CostEstimate estimateCost(Vehicle vehicle, CostAssumptions assumptions) { return agent.estimateCost(vehicle, assumptions); }

    public PreparationResult prepareEvaluation(Vehicle vehicle, UserPreference preference, CostAssumptions assumptions) {
        // Compute before committing session changes so a failed request preserves the previous state.
        PreparationResult result = agent.prepareEvaluation(vehicle, preference, assumptions);
        saveVehicle(vehicle);
        savePreferences(preference);
        return result;
    }
}
