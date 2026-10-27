package ca.yorku.eecs3311.usedcar.result;

import ca.yorku.eecs3311.usedcar.domain.*;
import java.util.List;

/** SD02 deterministic preparation only; deliberately not a RiskAssessment or EvaluationReport. */
public record PreparationResult(Vehicle vehicle, UserPreference preference, CostAssumptions assumptions,
                                CostEstimate cost, List<ConstraintViolation> violations,
                                MissingInformationResult missing, String llmStatus) {
    public PreparationResult { violations = List.copyOf(violations); }
}
