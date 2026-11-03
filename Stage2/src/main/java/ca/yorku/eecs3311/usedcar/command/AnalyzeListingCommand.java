package ca.yorku.eecs3311.usedcar.command;
import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.result.ListingAnalysisResult;
public record AnalyzeListingCommand(UsedCarApplicationFacade facade, String text) implements Command<ListingAnalysisResult> {
    @Override public ListingAnalysisResult execute() { return facade.analyzeListing(text); }
}
