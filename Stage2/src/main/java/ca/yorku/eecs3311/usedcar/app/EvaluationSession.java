package ca.yorku.eecs3311.usedcar.app;

import ca.yorku.eecs3311.usedcar.domain.*;
import java.util.Objects;
import ca.yorku.eecs3311.usedcar.result.*;
import java.util.Optional;

/** Single-user, in-memory context. Comparison and clarification memory remain planned. */
public final class EvaluationSession {
    private Vehicle currentVehicle;
    private CostEstimate cost;
    private RiskEvaluation risk;
    private UserPreference preference = UserPreference.limits(null, null);

    public void setCurrentVehicle(Vehicle vehicle) { Objects.requireNonNull(vehicle); if(!vehicle.equals(currentVehicle)){cost=null;risk=null;} currentVehicle=vehicle; }
    public Optional<Vehicle> getCurrentVehicle() { return Optional.ofNullable(currentVehicle); }
    public void setPreference(UserPreference value) { preference = Objects.requireNonNull(value); risk=null; }
    public Optional<CostEstimate> getCost(){return Optional.ofNullable(cost);}
    public void setCost(CostEstimate value){cost=value;risk=null;}
    public void setRisk(RiskEvaluation value){risk=value;}
    public Optional<RiskEvaluation> getRisk(){return Optional.ofNullable(risk);}
    public void clearRisk(){risk=null;}
    public UserPreference getPreference() { return preference; }
}
