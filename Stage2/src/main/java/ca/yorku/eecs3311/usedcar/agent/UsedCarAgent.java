package ca.yorku.eecs3311.usedcar.agent;

import ca.yorku.eecs3311.usedcar.domain.*;
import ca.yorku.eecs3311.usedcar.llm.*;
import ca.yorku.eecs3311.usedcar.result.*;
import ca.yorku.eecs3311.usedcar.tool.*;
import ca.yorku.eecs3311.usedcar.strategy.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/** Coordinates deterministic evidence, buyer strategies and bounded structured LLM workflows. */
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

    public RiskAssessment assessRisk(Vehicle vehicle, UserPreference preference) {
        return evaluateRisk(vehicle,preference,null).requireAssessment();
    }
    public RiskEvaluation evaluateRisk(Vehicle vehicle, UserPreference preference, CostEstimate availableCost) {
        var evidence=collectEvidence(vehicle,preference,availableCost);
        var missing=evidence.missing(); boolean constraintsAvailable=evidence.constraintsAvailable();
        if(!missing.sufficient()||!constraintsAvailable){
            var result=new RiskAssessment("Insufficient Information",List.of(),evidence.requiredRisks(),evidence.requiredUncertainties(),
                    "Essential information or mandatory constraint checks are unavailable. Supply missing data and retry; no AI request was made.");
            return new RiskEvaluation("INSUFFICIENT_INFORMATION",result,evidence,"Deterministic sufficiency gate; not an AI-generated assessment.");
        }
        try{
            var parser=new ResponseParser();String prompt=new PromptBuilder().buildRiskPrompt(evidence);
            var assessment=parser.validateRiskAssessment(parser.parseRiskAssessment(llmClient.generateStructured(prompt,RiskAssessmentSchema.json())),evidence);
            return new RiskEvaluation("AI_SUCCESS",assessment,evidence,"AI interpretation of the displayed evidence; advisory only.");
        }catch(IllegalArgumentException e){return new RiskEvaluation("AI_UNAVAILABLE",null,evidence,"Risk response rejected by structural/evidence validation. Deterministic evidence remains available; retry explicitly.");}
        catch(RuntimeException e){return new RiskEvaluation("AI_UNAVAILABLE",null,evidence,"AI request failed or is not configured. Check status/configuration; deterministic evidence remains available. No automatic retry.");}
    }

    private RiskEvidence collectEvidence(Vehicle vehicle, UserPreference preference, CostEstimate availableCost) {
        Objects.requireNonNull(vehicle,"Vehicle is required");
        UserPreference profile=preference==null?UserPreference.limits(null,null):preference;
        var missing=checkMissingInformation(vehicle);var limits=new ArrayList<String>();
        limits.add("Vehicle values and seller statements are user-provided claims, not verified mechanical inspections.");
        limits.add("This assessment is advisory and does not guarantee safety or reliability.");
        if(preference==null)limits.add("Buyer preferences were absent; neutral preferences were used.");
        VehicleKnowledge knowledge=null;
        try{knowledge=retrieveKnowledge(vehicle);}catch(RuntimeException e){limits.add("Local knowledge tool failed; knowledge is unavailable.");}
        if(knowledge==null)limits.add("No local knowledge evidence is available.");
        else if(knowledge.equals(VehicleKnowledge.noData()))limits.add("No matching local vehicle knowledge; model-specific reliability is unknown.");
        else limits.add("Local knowledge is instructional material, not independently verified reliability research. Source: "+knowledge.sourceLabel());
        List<ConstraintViolation> violations=List.of();boolean constraintsAvailable=true;
        try{violations=checkConstraints(vehicle,profile);}catch(RuntimeException e){constraintsAvailable=false;limits.add("Constraint tool failed; buyer compatibility could not be checked.");}
        if(availableCost==null)limits.add("Ownership-cost estimate unavailable; no cost assumptions were invented.");
        else availableCost.unavailable().forEach(x->limits.add("Cost limitation: "+x));
        final String guidance;
        if(strategy==null&&!automaticStrategy)guidance=strategyFactory.selectionBasis(profile)+strategyFactory.createStrategy(profile).buildPriorityGuidance(profile);
        else guidance=buildPriorityGuidance(profile);
        return new RiskEvidence(vehicle,profile,missing,knowledge,violations,availableCost,guidance,limits,constraintsAvailable);
    }

    public VehicleComparison compare(Vehicle first, Vehicle second, UserPreference preference) {
        return compare(first,second,preference,null).comparison();
    }
    public ComparisonEvaluation compare(Vehicle first, Vehicle second, UserPreference preference, CostAssumptions assumptions) {
        if(first==null || second==null) throw new IllegalStateException("Save Vehicle A and comparison Vehicle B before comparing.");
        if(first.id().equals(second.id())) throw new IllegalArgumentException("Select two distinct vehicles; a vehicle cannot be compared with itself.");
        var a=comparisonEvidence(first,preference,assumptions);
        var b=comparisonEvidence(second,preference,assumptions);
        var evidence=new ComparisonEvidence(a,b,assumptions);
        if(!evidence.sufficient()) return new ComparisonEvaluation("INSUFFICIENT_INFORMATION",evidence.deterministic(),evidence,
                "Essential data or mandatory checks are unavailable. No AI request was made; no preferred vehicle is identified.");
        final String response;
        try { response=llmClient.generateStructured(new PromptBuilder().buildComparisonPrompt(evidence),ComparisonSchema.json(evidence)); }
        catch(RuntimeException e) { return new ComparisonEvaluation("AI_UNAVAILABLE",evidence.deterministic(),evidence,
                "AI narrative unavailable: request failed or is not configured. Check status and retry explicitly; deterministic evidence is retained."); }
        try {
            var parser=new ResponseParser();
            var result=parser.validateComparison(parser.parseComparison(response),evidence);
            return new ComparisonEvaluation("AI_SUCCESS",result,evidence,"AI trade-off interpretation; advisory only, not a verified inspection or safety guarantee.");
        } catch(IllegalArgumentException e) {
            return new ComparisonEvaluation("AI_UNAVAILABLE",evidence.deterministic(),evidence,"AI narrative rejected: "+e.getMessage());
        }
    }
    private RiskEvidence comparisonEvidence(Vehicle vehicle, UserPreference preference, CostAssumptions assumptions) {
        CostEstimate cost=null; boolean costFailed=false;
        if(assumptions!=null) {
            try { cost=estimateCost(vehicle,assumptions); } catch(RuntimeException e) { costFailed=true; }
        }
        var evidence=collectEvidence(vehicle,preference,cost);
        var limits=new ArrayList<>(evidence.limitations());
        if(assumptions==null) limits.add("Common cost assumptions unavailable: annual km, CAD/L fuel price and optional tax/maintenance/insurance inputs have not been supplied.");
        else limits.add("Costs use the same supplied scenario for both vehicles; equal maintenance/insurance allowances are assumptions, not vehicle-specific quotations.");
        if(costFailed) limits.add("Cost tool failed for this vehicle; costs are unavailable.");
        return new RiskEvidence(vehicle,evidence.preference(),evidence.missing(),evidence.knowledge(),evidence.violations(),cost,
                evidence.priorityGuidance(),limits,evidence.constraintsAvailable());
    }

    public PreparationResult prepareEvaluation(Vehicle vehicle, UserPreference preference, CostAssumptions assumptions) {
        var missing = checkMissingInformation(vehicle);
        var cost = estimateCost(vehicle, assumptions);
        var constraints = checkConstraints(vehicle, preference);
        return new PreparationResult(vehicle, preference, assumptions, cost, constraints, missing, llmStatus());
    }
}
