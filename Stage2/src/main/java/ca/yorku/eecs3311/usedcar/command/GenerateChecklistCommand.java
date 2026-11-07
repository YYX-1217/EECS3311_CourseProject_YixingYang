package ca.yorku.eecs3311.usedcar.command;

import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.result.ChecklistEvaluation;

/** Generate for the saved current vehicle through the shared application boundary. */
public record GenerateChecklistCommand(UsedCarApplicationFacade facade) implements Command<ChecklistEvaluation> {
    public ChecklistEvaluation execute(){return facade.generateCurrentChecklist();}
}
