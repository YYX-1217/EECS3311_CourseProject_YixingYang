package ca.yorku.eecs3311.usedcar.command;
import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.result.ReportEvaluation;
/** Used by both GUI and CLI; generation is always explicit. */
public record GenerateReportCommand(UsedCarApplicationFacade facade) implements Command<ReportEvaluation> {
    public ReportEvaluation execute(){return facade.generateCurrentReport();}
}
