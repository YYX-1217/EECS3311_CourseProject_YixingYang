package ca.yorku.eecs3311.usedcar.agent;

import ca.yorku.eecs3311.usedcar.domain.*;
import ca.yorku.eecs3311.usedcar.llm.*;
import ca.yorku.eecs3311.usedcar.result.*;
import ca.yorku.eecs3311.usedcar.tool.*;
import ca.yorku.eecs3311.usedcar.strategy.*;
import java.util.List;
import java.util.Objects;

/** Initial deterministic coordination only; not yet the Stage 2 reasoning agent. */
public final class UsedCarAgent {
    private EvaluationStrategy strategy;
    private final EvaluationStrategyFactory strategyFactory = new EvaluationStrategyFactory();
    private boolean automaticStrategy;
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
    public MissingInformationResult checkConflict(Vehicle manualVehicle, Vehicle extractedVehicle) {
        return dataQualityChecker.checkConflict(manualVehicle, extractedVehicle);
    }
    public VehicleKnowledge retrieveKnowledge(Vehicle vehicle) {
        return (VehicleKnowledge) toolManager.execute("vehicleKnowledge", vehicle);
    }
    public void setStrategy(EvaluationStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "Evaluation strategy is required");
        automaticStrategy = false;
    }
    public void useAutomaticStrategy() { automaticStrategy = true; }
    public void selectManualStrategy(String mode) { setStrategy(strategyFactory.createManualStrategy(mode)); }
    public String buildPriorityGuidance(UserPreference preference) {
        if (automaticStrategy) {
            UserPreference current = preference == null ? UserPreference.limits(null, null) : preference;
            strategy = strategyFactory.createStrategy(current);
            return strategyFactory.selectionBasis(current) + strategy.buildPriorityGuidance(current);
        }
        if (strategy == null) throw new IllegalStateException("Select a strategy first: auto, balanced, budget or reliability.");
        return strategy.buildPriorityGuidance(Objects.requireNonNull(preference));
    }
    public ListingAnalysisResult analyzeListing(String text, Vehicle baseline) {
        String prompt = new PromptBuilder().buildListingExtractionPrompt(text);
        VehicleDraft draft = new ResponseParser().parseVehicle(llmClient.generateStructured(prompt, ListingExtractionSchema.json()), text);
        return new ListingAnalysisResult(draft, dataQualityChecker.check(draft, baseline), baseline);
    }
    public String testLlmConnection() { return llmClient.generate("Reply with one short greeting sentence. This is a connection test."); }
    public String llmStatus() { return llmClient.status(); }

    public PreparationResult prepareEvaluation(Vehicle vehicle, UserPreference preference, CostAssumptions assumptions) {
        var missing = checkMissingInformation(vehicle);
        var cost = estimateCost(vehicle, assumptions);
        var constraints = checkConstraints(vehicle, preference);
        return new PreparationResult(vehicle, preference, assumptions, cost, constraints, missing, llmStatus());
    }
}
