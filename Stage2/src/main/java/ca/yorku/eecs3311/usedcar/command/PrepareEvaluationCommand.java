package ca.yorku.eecs3311.usedcar.command;

import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.domain.*;
import ca.yorku.eecs3311.usedcar.result.PreparationResult;
import java.util.Objects;

/** New SD02 command; does not impersonate Stage 1 EvaluateVehicleCommand (F08). */
public record PrepareEvaluationCommand(UsedCarApplicationFacade facade, Vehicle vehicle,
                                        UserPreference preference, CostAssumptions assumptions)
        implements Command<PreparationResult> {
    public PrepareEvaluationCommand {
        Objects.requireNonNull(facade); Objects.requireNonNull(vehicle);
        Objects.requireNonNull(preference); Objects.requireNonNull(assumptions);
    }
    @Override public PreparationResult execute() { return facade.prepareEvaluation(vehicle, preference, assumptions); }
}
