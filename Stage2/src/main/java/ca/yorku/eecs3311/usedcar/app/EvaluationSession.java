package ca.yorku.eecs3311.usedcar.app;

import ca.yorku.eecs3311.usedcar.domain.*;
import java.util.Objects;
import ca.yorku.eecs3311.usedcar.result.*;
import java.util.Optional;

/** Single-user, in-memory context. Two vehicles and common cost assumptions are retained; clarification memory remains planned. */
public final class EvaluationSession {
    private Vehicle currentVehicle;
    private ChecklistEvaluation checklist;
    private long checklistRevision;
    private Vehicle comparisonVehicle;
    private CostAssumptions costAssumptions;
    private CostEstimate cost;
    private RiskEvaluation risk;
    private UserPreference preference = UserPreference.limits(null, null);

    public synchronized void setCurrentVehicle(Vehicle vehicle) { Objects.requireNonNull(vehicle); if(comparisonVehicle!=null && comparisonVehicle.id().equals(vehicle.id())) throw new IllegalArgumentException("Current and comparison vehicles must have different IDs."); if(!vehicle.equals(currentVehicle)){cost=null;risk=null;checklist=null;checklistRevision++;} currentVehicle=vehicle; }
    public synchronized Optional<Vehicle> getCurrentVehicle() { return Optional.ofNullable(currentVehicle); }
    public void setPreference(UserPreference value) { preference = Objects.requireNonNull(value); risk=null; }
    public Optional<CostEstimate> getCost(){return Optional.ofNullable(cost);}
    public void setCost(CostEstimate value){cost=value;risk=null;}
    public void setRisk(RiskEvaluation value){risk=value;}
    public Optional<RiskEvaluation> getRisk(){return Optional.ofNullable(risk);}
    public void clearRisk(){risk=null;}
    public void setComparisonVehicle(Vehicle vehicle) {
        Objects.requireNonNull(vehicle);
        if(currentVehicle!=null && currentVehicle.id().equals(vehicle.id())) throw new IllegalArgumentException("A vehicle cannot be compared with itself.");
        comparisonVehicle=vehicle;
    }
    public Optional<Vehicle> getComparisonVehicle() { return Optional.ofNullable(comparisonVehicle); }
    public void setCostAssumptions(CostAssumptions value) { costAssumptions=Objects.requireNonNull(value); }
    public Optional<CostAssumptions> getCostAssumptions() { return Optional.ofNullable(costAssumptions); }
    public UserPreference getPreference() { return preference; }
    public synchronized long beginChecklist(Vehicle vehicle){
        if(!Objects.equals(currentVehicle,vehicle))throw new IllegalStateException("Saved vehicle changed; generate the checklist again.");
        checklist=null;return ++checklistRevision;
    }
    public synchronized void saveChecklist(long revision,ChecklistEvaluation result){
        if(revision!=checklistRevision||!Objects.equals(currentVehicle,result.vehicle()))throw new IllegalStateException("Vehicle or checklist changed during generation; old checklist discarded. Generate again.");
        checklist=result;
    }
    public synchronized Optional<ChecklistEvaluation> getChecklist(){return Optional.ofNullable(checklist);}
    public synchronized ChecklistEvaluation completeChecklistItem(String generationId,String itemId,boolean done){
        if(checklist==null||!checklist.generationId().equals(generationId))throw new IllegalStateException("Checklist is no longer current; reload or generate for the saved vehicle.");
        checklist=checklist.withCompletion(itemId,done);return checklist;
    }
}
