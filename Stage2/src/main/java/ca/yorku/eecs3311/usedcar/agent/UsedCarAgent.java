package ca.yorku.eecs3311.usedcar.agent;

import ca.yorku.eecs3311.usedcar.domain.*;
import ca.yorku.eecs3311.usedcar.llm.LLMClient;
import ca.yorku.eecs3311.usedcar.result.*;
import ca.yorku.eecs3311.usedcar.tool.*;
import java.util.List;
import java.util.Objects;

/** Initial deterministic coordination only; not yet the Stage 2 reasoning agent. */
public final class UsedCarAgent {
    private final ToolManager toolManager;
    private final LLMClient llmClient;
    private final DataQualityChecker dataQualityChecker;

    public UsedCarAgent(ToolManager tools, LLMClient llmClient, DataQualityChecker checker) {
        this.toolManager = Objects.requireNonNull(tools);
        this.llmClient = Objects.requireNonNull(llmClient);
        this.dataQualityChecker = Objects.requireNonNull(checker);
    }

    public CostEstimate estimateCost(Vehicle vehicle, CostAssumptions assumptions) {
        return (CostEstimate) toolManager.execute("cost", new CostCalculatorTool.Input(vehicle, assumptions));
    }

    public List<ConstraintViolation> checkConstraints(Vehicle vehicle, UserPreference preference) {
        Object result = toolManager.execute("constraints", new ConstraintCheckerTool.Input(vehicle, preference));
        if (!(result instanceof List<?> list)) throw new IllegalStateException("Constraint tool returned an invalid result.");
        return list.stream().map(ConstraintViolation.class::cast).toList();
    }

    public MissingInformationResult checkMissingInformation(Vehicle vehicle) { return dataQualityChecker.check(vehicle); }
    public String llmStatus() { return llmClient.status(); }

    public PreparationResult prepareEvaluation(Vehicle vehicle, UserPreference preference, CostAssumptions assumptions) {
        var missing = checkMissingInformation(vehicle);
        var cost = estimateCost(vehicle, assumptions);
        var constraints = checkConstraints(vehicle, preference);
        return new PreparationResult(vehicle, preference, assumptions, cost, constraints, missing, llmStatus());
    }
}
