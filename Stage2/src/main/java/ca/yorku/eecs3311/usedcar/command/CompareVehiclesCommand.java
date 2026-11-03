package ca.yorku.eecs3311.usedcar.command;

import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.domain.Vehicle;
import ca.yorku.eecs3311.usedcar.result.ComparisonEvaluation;

/** Original two-record command, with a current-session convenience constructor and evidence-preserving result. */
public record CompareVehiclesCommand(UsedCarApplicationFacade facade, Vehicle first, Vehicle second)
        implements Command<ComparisonEvaluation> {
    public CompareVehiclesCommand(UsedCarApplicationFacade facade) { this(facade,null,null); }
    public ComparisonEvaluation execute() {
        return first==null && second==null ? facade.compareCurrentVehicles() : facade.compareWithEvidence(first,second);
    }
}
