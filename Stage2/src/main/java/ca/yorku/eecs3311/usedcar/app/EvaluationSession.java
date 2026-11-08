package ca.yorku.eecs3311.usedcar.app;

import ca.yorku.eecs3311.usedcar.domain.*;
import java.util.Objects;
import ca.yorku.eecs3311.usedcar.result.*;
import java.util.Optional;

/** Single-user, in-memory context. Two vehicles and common cost assumptions are retained; clarification memory is scoped to the current vehicle. */
public final class EvaluationSession {
    private Vehicle currentVehicle;
    private ChecklistEvaluation checklist;
    private long checklistRevision;
    private Vehicle comparisonVehicle;
    private CostAssumptions costAssumptions;
    private CostEstimate cost;
    private RiskEvaluation risk;
    private UserPreference preference = UserPreference.limits(null, null);

    public synchronized void setCurrentVehicle(Vehicle vehicle) { Objects.requireNonNull(vehicle); if(comparisonVehicle!=null && comparisonVehicle.id().equals(vehicle.id())) throw new IllegalArgumentException("Current and comparison vehicles must have different IDs."); if(!vehicle.equals(currentVehicle)){resetClarification();cost=null;risk=null;checklist=null;checklistRevision++;} currentVehicle=vehicle; }
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

    private ClarificationState clarification;
    private final java.util.Set<ClarificationField> asked=new java.util.LinkedHashSet<>();
    private final java.util.Set<ClarificationField> listingNeeds=new java.util.LinkedHashSet<>();
    private final java.util.List<String> clarificationHistory=new java.util.ArrayList<>();
    private long clarificationRevision;
    private boolean clarificationBusy;
    private void resetClarification(){clarification=null;asked.clear();listingNeeds.clear();clarificationHistory.clear();clarificationBusy=false;clarificationRevision++;}
    public synchronized Optional<ClarificationState> getClarification(){return Optional.ofNullable(clarification);}
    public synchronized java.util.Set<ClarificationField> askedClarifications(){return java.util.Set.copyOf(asked);}
    public synchronized java.util.Set<ClarificationField> listingClarifications(){return java.util.Set.copyOf(listingNeeds);}
    public synchronized boolean registerListingNeeds(Vehicle baseline,java.util.Set<ClarificationField> needs){
        if(baseline==null||!Objects.equals(baseline,currentVehicle)||needs.isEmpty())return false;
        listingNeeds.addAll(needs);risk=null;checklist=null;checklistRevision++;
        // An already displayed cycle stays valid; new listing gaps are offered in the next cycle.
        asked.removeAll(needs);return true;
    }
    public synchronized long beginClarification(boolean next){
        if(currentVehicle==null)throw new IllegalStateException("Save a vehicle before clarification.");
        if(clarificationBusy)throw new IllegalStateException("Clarification generation is already in progress.");
        if(next&&clarification!=null&&!clarification.settled())throw new IllegalStateException("Answer, skip or mark every question unknown and resolve conflicts before the next cycle.");
        clarificationBusy=true;return ++clarificationRevision;
    }
    public synchronized ClarificationState finishClarification(long revision,Vehicle vehicle,ClarificationBatch batch){
        if(revision!=clarificationRevision||!Objects.equals(vehicle,currentVehicle))throw new IllegalStateException("Vehicle changed during clarification; old questions discarded. Start again.");
        clarificationBusy=false;
        var responses=new java.util.LinkedHashMap<ClarificationField,ClarificationResponse>();
        for(var q:batch.questions()){asked.add(q.field());responses.put(q.field(),new ClarificationResponse("PENDING","","",""));recordClarification("Asked "+q.id()+": "+q.question());}
        clarification=new ClarificationState(java.util.UUID.randomUUID().toString(),currentVehicle,batch,responses,clarificationHistory);return clarification;
    }
    public synchronized ClarificationState requireClarification(String cycle){
        if(clarification==null||!clarification.cycleId().equals(cycle)||!clarification.vehicle().equals(currentVehicle))throw new IllegalStateException("Clarification is no longer current. Show or start clarification for the saved vehicle.");
        return clarification;
    }
    public synchronized void recordClarification(String text){clarificationHistory.add(text);}
    public synchronized ClarificationState respond(String cycle,ClarificationField field,ClarificationResponse response,Vehicle updated){
        var previous=requireClarification(cycle);
        if(!previous.responses().containsKey(field))throw new IllegalArgumentException("Field is not in the current clarification cycle.");
        if(updated!=null&&!updated.equals(currentVehicle)){
            if(!updated.id().equals(currentVehicle.id()))throw new IllegalStateException("Clarification cannot replace a different vehicle.");
            currentVehicle=updated;cost=null;risk=null;checklist=null;checklistRevision++;
        }
        var responses=new java.util.LinkedHashMap<>(previous.responses());responses.put(field,response);
        if(java.util.Set.of("ANSWERED","KEPT","ACCEPTED").contains(response.status())&&listingNeeds.remove(field)){risk=null;checklist=null;checklistRevision++;}
        recordClarification(field.key()+" -> "+response.status()+(response.answer().isBlank()?"":" | answer="+response.answer())
            +(response.existing().isBlank()?"":" | existing="+response.existing()+" | proposed="+response.proposed()));
        clarification=new ClarificationState(cycle,currentVehicle,previous.batch(),responses,clarificationHistory);return clarification;
    }
    public synchronized void recordResume(String cycle,String target){
        var s=requireClarification(cycle);recordClarification("Continued "+target+" with remaining uncertainty preserved."+(listingNeeds.isEmpty()?"":" Unresolved listing claims: "+listingNeeds+"; saved values retained, not independently verified."));
        clarification=new ClarificationState(cycle,currentVehicle,s.batch(),s.responses(),clarificationHistory);
    }
}
