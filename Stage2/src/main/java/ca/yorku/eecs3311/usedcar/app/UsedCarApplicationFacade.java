package ca.yorku.eecs3311.usedcar.app;

import ca.yorku.eecs3311.usedcar.agent.UsedCarAgent;
import ca.yorku.eecs3311.usedcar.domain.*;
import ca.yorku.eecs3311.usedcar.result.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Locale;

/** Shared application API for both interfaces. No UI code or provider protocol here. */
public final class UsedCarApplicationFacade {
    private final java.util.concurrent.atomic.AtomicLong contextVersion = new java.util.concurrent.atomic.AtomicLong();
    private final UsedCarAgent agent;
    private final EvaluationSession session;

    public UsedCarApplicationFacade(UsedCarAgent agent, EvaluationSession session) {
        this.agent = Objects.requireNonNull(agent);
        this.session = Objects.requireNonNull(session);
    }

    public Vehicle saveVehicle(Vehicle vehicle) { session.setCurrentVehicle(vehicle); contextVersion.incrementAndGet(); return vehicle; }
    public void savePreferences(UserPreference preference) { session.setPreference(preference); contextVersion.incrementAndGet(); }
    public Optional<Vehicle> currentVehicle() { return session.getCurrentVehicle(); }
    public UserPreference preferences() { return session.getPreference(); }
    public VehicleKnowledge retrieveKnowledge(Vehicle vehicle) { return agent.retrieveKnowledge(vehicle); }
    public VehicleKnowledge retrieveKnowledge() {
        return retrieveKnowledge(currentVehicle().orElseThrow(() -> new IllegalStateException("Save a vehicle first.")));
    }
    public List<String> evaluationModes() { return List.of("auto", "balanced", "budget", "reliability"); }
    public void selectStrategy(String mode) {
        if (mode != null && mode.strip().toLowerCase(Locale.ROOT).equals("auto")) agent.useAutomaticStrategy();
        else agent.selectManualStrategy(mode);
        session.clearRisk();contextVersion.incrementAndGet();
    }
    public String previewPriorityGuidance() { return agent.buildPriorityGuidance(preferences()); }
    public ListingAnalysisResult analyzeListing(String text) {
        return agent.analyzeListing(text, currentVehicle().orElse(null));
    }
    public MissingInformationResult reviewListing(ListingAnalysisResult result, Vehicle corrected) {
        if (!Objects.equals(result.baseline(), currentVehicle().orElse(null)))
            throw new IllegalStateException("Saved vehicle changed after analysis. Cancel and analyze again before replacing it.");
        return result.baseline() == null ? agent.checkMissingInformation(corrected)
                : agent.checkConflict(result.baseline(), corrected);
    }
    public Vehicle confirmListing(ListingAnalysisResult result, Vehicle corrected, boolean approved) {
        if (!approved) throw new IllegalArgumentException("Explicit user confirmation is required.");
        reviewListing(result, corrected);
        String notes = corrected.notes() + "\nUser-reviewed listing claims (not independently verified). Original extraction:\n" + result.draft().describe();
        var saved = new Vehicle(corrected.id(),corrected.year(),corrected.make(),corrected.model(),corrected.trim(),
                corrected.mileage(),corrected.askingPrice(),corrected.transmission(),corrected.fuelType(),
                corrected.accidentHistory(),corrected.maintenanceHistory(),corrected.fuelConsumption(),notes);
        return saveVehicle(saved);
    }
    public String testLlmConnection() { return agent.testLlmConnection(); }
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
    public CostEstimate estimateCost(Vehicle vehicle, CostAssumptions assumptions) {
        var result=agent.estimateCost(vehicle, assumptions);
        if(currentVehicle().filter(vehicle::equals).isPresent()){session.setCost(result);session.setCostAssumptions(assumptions);contextVersion.incrementAndGet();}
        return result;
    }
    public RiskAssessment evaluateVehicle(Vehicle vehicle) { return agent.assessRisk(vehicle,preferences()); }
    public Optional<RiskEvaluation> lastRiskEvaluation(){return session.getRisk();}
    public RiskEvaluation evaluateCurrentVehicle(){
        long version=contextVersion.get();
        var vehicle=currentVehicle().orElseThrow(()->new IllegalStateException("Save a vehicle before evaluation."));
        var profile=preferences();session.clearRisk();
        var result=agent.evaluateRisk(vehicle,profile,session.getCost().orElse(null));
        if(version!=contextVersion.get()||!currentVehicle().filter(vehicle::equals).isPresent()||!preferences().equals(profile))
            return new RiskEvaluation("STALE",null,result.evidence(),"Saved vehicle/preferences changed during evaluation. Review the evidence snapshot and evaluate again.");
        session.setRisk(result);return result;
    }

    public Vehicle saveComparisonVehicle(Vehicle vehicle) {
        if(currentVehicle().isEmpty()) throw new IllegalStateException("Save Vehicle A before entering comparison Vehicle B.");
        session.setComparisonVehicle(vehicle); contextVersion.incrementAndGet(); return vehicle;
    }
    public Optional<Vehicle> comparisonVehicle() { return session.getComparisonVehicle(); }
    public Optional<CostAssumptions> costAssumptions() { return session.getCostAssumptions(); }
    public VehicleComparison compareVehicles(Vehicle first, Vehicle second) { return compareWithEvidence(first,second).comparison(); }
    public ComparisonEvaluation compareCurrentVehicles() {
        long version=contextVersion.get();
        var first=currentVehicle().orElseThrow(()->new IllegalStateException("Save Vehicle A before comparison."));
        var second=comparisonVehicle().orElseThrow(()->new IllegalStateException("Save comparison Vehicle B before comparison."));
        var result=compareWithEvidence(first,second);
        if(version!=contextVersion.get()) return new ComparisonEvaluation("STALE",result.evidence().deterministic(),result.evidence(),
                "Saved comparison context changed during the request. This is the old deterministic snapshot; compare again. AI narrative withheld.");
        return result;
    }
    public ComparisonEvaluation compareWithEvidence(Vehicle first, Vehicle second) {
        return agent.compare(first,second,preferences(),session.getCostAssumptions().orElse(null));
    }

    public PreparationResult prepareEvaluation(Vehicle vehicle, UserPreference preference, CostAssumptions assumptions) {
        // Compute before committing session changes so a failed request preserves the previous state.
        PreparationResult result = agent.prepareEvaluation(vehicle, preference, assumptions);
        saveVehicle(vehicle);
        savePreferences(preference);
        session.setCost(result.cost());session.setCostAssumptions(assumptions);contextVersion.incrementAndGet();
        return result;
    }
}
