package ca.yorku.eecs3311.usedcar.result;
import ca.yorku.eecs3311.usedcar.domain.*;
/** Baseline is captured before analysis so delayed confirmation cannot overwrite newer edits. */
public record ListingAnalysisResult(VehicleDraft draft, MissingInformationResult quality, Vehicle baseline) { }
