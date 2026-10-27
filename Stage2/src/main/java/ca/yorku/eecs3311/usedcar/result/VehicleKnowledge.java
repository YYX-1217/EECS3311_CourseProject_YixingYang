package ca.yorku.eecs3311.usedcar.result;

import java.util.List;
import java.util.Objects;

/** Stage 1 local knowledge result; notes are evidence from the labeled dataset only. */
public record VehicleKnowledge(String summary, List<String> commonIssues,
                               List<String> inspectionNotes, String sourceLabel) {
    public VehicleKnowledge {
        Objects.requireNonNull(summary); Objects.requireNonNull(sourceLabel);
        commonIssues = List.copyOf(commonIssues);
        inspectionNotes = List.copyOf(inspectionNotes);
    }
    public static VehicleKnowledge noData() {
        return new VehicleKnowledge("No local vehicle knowledge available.", List.of(), List.of(),
                "No matching local CSV record.");
    }
}
