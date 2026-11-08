package ca.yorku.eecs3311.usedcar.result;
import ca.yorku.eecs3311.usedcar.domain.*;
import java.util.*;
/** Immutable snapshot; authoritative tool values remain separate from model narrative. */
public record RiskEvidence(Vehicle vehicle, UserPreference preference, MissingInformationResult missing,
        VehicleKnowledge knowledge, List<ConstraintViolation> violations, CostEstimate cost,
        String priorityGuidance, List<String> limitations, boolean constraintsAvailable, int referenceYear) {
    /** Retains the original construction contract; the agent supplies its injected clock explicitly. */
    public RiskEvidence(Vehicle vehicle,UserPreference preference,MissingInformationResult missing,VehicleKnowledge knowledge,
            List<ConstraintViolation> violations,CostEstimate cost,String priorityGuidance,List<String> limitations,boolean constraintsAvailable){
        this(vehicle,preference,missing,knowledge,violations,cost,priorityGuidance,limitations,constraintsAvailable,java.time.Year.now().getValue());
    }
    public ModelYearContext yearContext(){return new ModelYearContext(vehicle==null?null:vehicle.year(),referenceYear);}
    public RiskEvidence { violations=List.copyOf(violations);limitations=List.copyOf(limitations); }
    public List<String> requiredRisks(){
        return violations.stream().filter(v->v.severity().equals("CONFLICT"))
                .map(v->"Constraint conflict: "+v.constraint()+"; expected="+v.expected()+"; actual="+v.actual()).toList();
    }
    public List<String> requiredUncertainties(){
        var items=new ArrayList<>(limitations);
        missing.missingFields().forEach(f->items.add("Missing: "+f));missing.uncertainFields().forEach(f->items.add("Uncertain: "+f));
        violations.stream().filter(v->v.severity().equals("UNKNOWN")).forEach(v->items.add("Unknown constraint: "+v.constraint()));
        return List.copyOf(items);
    }
}
