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

    public Vehicle saveVehicle(Vehicle vehicle) { synchronized(session){session.setCurrentVehicle(vehicle); contextVersion.incrementAndGet(); return vehicle;} }
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
        var result=agent.analyzeListing(text, currentVehicle().orElse(null));
        var needs=new java.util.LinkedHashSet<ClarificationField>();
        for(var field:ClarificationField.values())if(result.quality().uncertainFields().contains(field.label())||result.quality().conflicts().stream().anyMatch(c->c.startsWith(field.label()+" |")))needs.add(field);
        synchronized(session){if(session.registerListingNeeds(result.baseline(),needs))contextVersion.incrementAndGet();}
        return result;
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
    public RiskEvaluation evaluateCurrentVehicle(){return evaluateCurrentVehicle(null);}
    private RiskEvaluation evaluateCurrentVehicle(Vehicle expected){
        final long version;final Vehicle vehicle;final UserPreference profile;final java.util.Set<ClarificationField> listingNeeds;
        synchronized(session){
            version=contextVersion.get();vehicle=currentVehicle().orElseThrow(()->new IllegalStateException("Save a vehicle before evaluation."));
            if(expected!=null&&!expected.equals(vehicle))throw new IllegalStateException("Vehicle changed before resuming evaluation.");
            profile=preferences();listingNeeds=session.listingClarifications();session.clearRisk();
        }
        var result=agent.evaluateRisk(vehicle,profile,session.getCost().orElse(null),listingNeeds);
        synchronized(session){
            if(version!=contextVersion.get()||!currentVehicle().filter(vehicle::equals).isPresent()||!preferences().equals(profile))
                return new RiskEvaluation("STALE",null,result.evidence(),"Saved vehicle/preferences changed during evaluation. Review the evidence snapshot and evaluate again.");
            session.setRisk(result);return result;
        }
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
    public InspectionChecklist generateChecklist(Vehicle vehicle){return agent.generateChecklist(vehicle);}
    public ChecklistEvaluation generateCurrentChecklist(){return generateCurrentChecklist(null);}
    private ChecklistEvaluation generateCurrentChecklist(Vehicle expected){
        final Vehicle vehicle;final long revision;final java.util.Set<ClarificationField> listingNeeds;
        synchronized(session){
            vehicle=currentVehicle().orElseThrow(()->new IllegalStateException("Save a vehicle before generating an inspection checklist."));
            if(expected!=null&&!expected.equals(vehicle))throw new IllegalStateException("Vehicle changed before resuming checklist.");
            revision=session.beginChecklist(vehicle);listingNeeds=session.listingClarifications();
        }
        var result=agent.generateChecklistWithEvidence(vehicle,listingNeeds);session.saveChecklist(revision,result);return result;
    }
    public Optional<ChecklistEvaluation> currentChecklist(){return session.getChecklist();}
    public ChecklistEvaluation completeChecklistItem(String generationId,String itemId,boolean done){return session.completeChecklistItem(generationId,itemId,done);}

    public Optional<ClarificationState> currentClarification(){return session.getClarification();}
    public ClarificationState startClarification(boolean next){
        final Vehicle vehicle;final long revision;final UserPreference profile;
        final java.util.Set<ClarificationField> asked,listing;
        synchronized(session){
            if(!next&&session.getClarification().isPresent())return session.getClarification().get();
            revision=session.beginClarification(next);vehicle=currentVehicle().orElseThrow();profile=preferences();
            asked=session.askedClarifications();listing=session.listingClarifications();
        }
        var batch=agent.generateClarificationQuestions(vehicle,profile,asked,listing);
        return session.finishClarification(revision,vehicle,batch);
    }
    public ClarificationState revisitClarification(ClarificationField field){
        synchronized(session){
            long revision=session.beginClarification(true);var vehicle=currentVehicle().orElseThrow();
            return session.finishClarification(revision,vehicle,new ClarificationBatch("USER_REQUESTED","Explicit local correction/revisit. Known values require conflict resolution.",List.of(ClarificationQuestion.fallback(field))));
        }
    }
    private ClarificationResponse pendingResponse(ClarificationState state,ClarificationField field){
        var response=state.responses().get(field);
        if(response==null)throw new IllegalArgumentException("Field is not in this cycle.");
        if(!response.status().equals("PENDING"))throw new IllegalStateException("Question already answered or pending conflict resolution. Resolve the conflict or explicitly revisit after finishing the cycle.");
        return response;
    }
    public ClarificationState submitClarification(String cycle,ClarificationField field,String answer){
        synchronized(session){
            var state=session.requireClarification(cycle);pendingResponse(state,field);
            var candidate=field.apply(state.vehicle(),answer);
            boolean conflict=!agent.checkConflict(state.vehicle(),candidate).conflicts().isEmpty();
            var response=new ClarificationResponse(conflict?"CONFLICT":"ANSWERED",field.read(candidate),field.read(state.vehicle()),field.read(candidate));
            var result=session.respond(cycle,field,response,conflict?null:candidate);
            if(!conflict)contextVersion.incrementAndGet();return result;
        }
    }
    public ClarificationState skipClarification(String cycle,ClarificationField field,boolean unknown){
        synchronized(session){var state=session.requireClarification(cycle);pendingResponse(state,field);
            return session.respond(cycle,field,new ClarificationResponse(unknown?"UNKNOWN":"SKIPPED","","",""),null);}
    }
    public ClarificationState resolveClarification(String cycle,ClarificationField field,boolean accept){
        synchronized(session){var state=session.requireClarification(cycle);var previous=state.responses().get(field);
            if(previous==null||!previous.status().equals("CONFLICT"))throw new IllegalStateException("No pending conflict for this field.");
            var candidate=accept?field.apply(state.vehicle(),previous.proposed()):null;
            var result=session.respond(cycle,field,new ClarificationResponse(accept?"ACCEPTED":"KEPT",previous.answer(),previous.existing(),previous.proposed()),candidate);
            contextVersion.incrementAndGet();return result;}
    }
    public Object continueClarification(String cycle,boolean checklist){
        final Vehicle vehicle;
        synchronized(session){var state=session.requireClarification(cycle);
            if(!state.settled())throw new IllegalStateException("Answer, skip or mark all questions unknown and resolve conflicts before continuing.");
            vehicle=state.vehicle();
            if(!checklist&&session.getCostAssumptions().isPresent())estimateCost(vehicle,session.getCostAssumptions().get());
        }
        // Existing F08/F10 entry points retain their own late-result guards; expected snapshot guards the start too.
        Object result=checklist?generateCurrentChecklist(vehicle):evaluateCurrentVehicle(vehicle);
        synchronized(session){session.requireClarification(cycle);session.recordResume(cycle,checklist?"F10 checklist":"F08 risk evaluation");}
        return result;
    }
}
