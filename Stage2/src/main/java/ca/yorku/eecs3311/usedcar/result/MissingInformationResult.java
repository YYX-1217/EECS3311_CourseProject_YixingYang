package ca.yorku.eecs3311.usedcar.result;

import java.util.List;

public record MissingInformationResult(List<String> missingFields, List<String> uncertainFields,
                                       List<String> conflicts, boolean sufficient) {
    public MissingInformationResult {
        missingFields = List.copyOf(missingFields);
        uncertainFields = List.copyOf(uncertainFields);
        conflicts = List.copyOf(conflicts);
    }
}
