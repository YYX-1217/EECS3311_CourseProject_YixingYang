package ca.yorku.eecs3311.usedcar.command;
import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.result.RiskEvaluation;
/** Evaluate the current saved snapshot, preserving evidence even when AI fails. */
public record EvaluateVehicleCommand(UsedCarApplicationFacade facade) implements Command<RiskEvaluation> {
    @Override public RiskEvaluation execute(){return facade.evaluateCurrentVehicle();}
}
