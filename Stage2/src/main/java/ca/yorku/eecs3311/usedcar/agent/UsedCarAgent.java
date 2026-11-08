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
        return evaluateRisk(vehicle,preference,availableCost,java.util.Set.of());
    }
    public RiskEvaluation evaluateRisk(Vehicle vehicle,UserPreference preference,CostEstimate availableCost,java.util.Set<ClarificationField> listingNeeds){
        var original=collectEvidence(vehicle,preference,availableCost);
        var evidence=listingNeeds.isEmpty()?original:new RiskEvidence(vehicle,original.preference(),withListingNeeds(original.missing(),listingNeeds),original.knowledge(),original.violations(),original.cost(),original.priorityGuidance(),original.limitations(),original.constraintsAvailable());
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
    public InspectionChecklist generateChecklist(Vehicle vehicle){return generateChecklistWithEvidence(vehicle).checklist();}
    public ChecklistEvaluation generateChecklistWithEvidence(Vehicle vehicle){
        return generateChecklistWithEvidence(vehicle,java.util.Set.of());
    }
    public ChecklistEvaluation generateChecklistWithEvidence(Vehicle vehicle,java.util.Set<ClarificationField> listingNeeds){
        if(vehicle==null)throw new IllegalStateException("Save a vehicle before generating an inspection checklist.");
        var missing=withListingNeeds(checkMissingInformation(vehicle),listingNeeds);VehicleKnowledge knowledge=null;
        String knowledgeStatus="Model-specific local knowledge unavailable; general inspection guidance only.";
        try{
            knowledge=retrieveKnowledge(vehicle);
            if(knowledge!=null&&!knowledge.equals(VehicleKnowledge.noData()))knowledgeStatus="Local instructional knowledge used (not independently verified research): "+knowledge.sourceLabel();
        }catch(RuntimeException e){knowledgeStatus="Local knowledge tool failed; general inspection guidance only.";}
        var planner=new ChecklistPlanner();InspectionChecklist baseline;
        try{baseline=planner.build(vehicle,knowledge,missing);}catch(IllegalArgumentException e){knowledge=null;knowledgeStatus="Local notes could not be represented safely; general inspection guidance only.";baseline=planner.build(vehicle,null,missing);}
        InspectionChecklist result=baseline;String status="FALLBACK";
        String message="Deterministic checklist; AI unavailable or not configured. No automatic retry. This does not replace a professional pre-purchase inspection.";
        final String response;
        try{response=llmClient.generateStructured(new PromptBuilder().buildChecklistPrompt(vehicle,knowledge,missing,baseline),ChecklistSchema.json());}
        catch(RuntimeException e){return new ChecklistEvaluation(java.util.UUID.randomUUID().toString(),vehicle,result,status,message,knowledgeStatus,missing,java.util.Set.of());}
        try{
            var parser=new ResponseParser();result=parser.validateChecklist(parser.parseChecklist(response),baseline);status="AI_SUCCESS";
            message="AI-assisted prioritization and reasons for evidence-grounded inspection actions. Advisory; does not replace a professional pre-purchase inspection.";
        }catch(IllegalArgumentException e){message="Deterministic fallback: AI output failed checklist structure/evidence checks. No automatic retry. This does not replace a professional pre-purchase inspection.";}
        return new ChecklistEvaluation(java.util.UUID.randomUUID().toString(),vehicle,result,status,message,knowledgeStatus,missing,java.util.Set.of());
    }

    public ClarificationBatch generateClarificationQuestions(Vehicle vehicle,UserPreference preference,
            java.util.Set<ClarificationField> asked,java.util.Set<ClarificationField> listingNeeds){
        var missing=checkMissingInformation(vehicle);
        var targets=java.util.Arrays.stream(ClarificationField.values()).filter(f->!asked.contains(f))
            .filter(f->missing.missingFields().contains(f.label())||missing.uncertainFields().contains(f.label())||listingNeeds.contains(f)).limit(3).toList();
        if(targets.isEmpty())return new ClarificationBatch("COMPLETE","No unasked supported gaps. Skipped/unknown fields remain uncertain; explicitly revisit a field if new evidence becomes available.",List.of());
        try{
            String prompt=new PromptBuilder().buildClarificationPrompt(missing,preference,targets,listingNeeds);
            var questions=new ResponseParser().parseClarificationQuestions(llmClient.generateStructured(prompt,ClarificationSchema.json(targets)),targets);
            return new ClarificationBatch("AI_SUCCESS","AI prioritized approved, grounded questions; answers require user input and deterministic validation.",questions);
        }catch(RuntimeException e){return new ClarificationBatch("FALLBACK","Deterministic questions: AI unavailable or response rejected. No automatic retry; no vehicle values inferred.",targets.stream().map(ClarificationQuestion::fallback).toList());}
    }

    private MissingInformationResult withListingNeeds(MissingInformationResult missing,java.util.Set<ClarificationField> listingNeeds){
        if(listingNeeds.isEmpty())return missing;
        var uncertain=new java.util.LinkedHashSet<>(missing.uncertainFields());
        listingNeeds.forEach(f->{uncertain.add(f.label());uncertain.add("unresolved listing claim: "+f.label());});
        return new MissingInformationResult(missing.missingFields(),List.copyOf(uncertain),missing.conflicts(),false);
    }

    public EvaluationReport generateRecommendation(Vehicle vehicle,UserPreference preference){
        return generateRecommendation(vehicle,preference,null,null,null,null,java.util.Set.of()).requireReport();
    }
    public ReportEvaluation generateRecommendation(Vehicle vehicle,UserPreference preference,CostEstimate cost,
            CostAssumptions assumptions,RiskEvaluation savedRisk,ClarificationState clarification,java.util.Set<ClarificationField> listingNeeds){
        boolean costFailed=false;
        if(cost==null&&assumptions!=null){try{cost=estimateCost(vehicle,assumptions);}catch(RuntimeException e){costFailed=true;}}
        var original=collectEvidence(vehicle,preference,cost);
        var limits=new ArrayList<>(original.limitations());if(costFailed)limits.add("Ownership-cost tool failed; cost evidence is unavailable.");
        var missing=withListingNeeds(original.missing(),listingNeeds);
        var deterministic=new RiskEvidence(vehicle,original.preference(),missing,original.knowledge(),original.violations(),cost,original.priorityGuidance(),limits,original.constraintsAvailable());
        RiskAssessment risk=null;String riskStatus="Current AI risk assessment unavailable. Run Evaluate explicitly if needed; no automatic risk request was made.";
        if(savedRisk!=null&&savedRisk.status().equals("AI_SUCCESS")&&savedRisk.assessment()!=null&&savedRisk.evidence()!=null&&savedRisk.evidence().vehicle().equals(vehicle)&&savedRisk.evidence().preference().equals(preference)
                &&savedRisk.evidence().priorityGuidance().equals(deterministic.priorityGuidance())&&Objects.equals(savedRisk.evidence().cost(),cost)&&savedRisk.evidence().missing().equals(missing)
                &&savedRisk.evidence().violations().equals(deterministic.violations())&&Objects.equals(savedRisk.evidence().knowledge(),deterministic.knowledge())){
            risk=savedRisk.assessment();riskStatus="Current saved F08 AI risk category reused; explanatory prose is not treated as verified evidence.";
        }
        var conflicts=new ArrayList<String>();List<String> history=List.of();
        if(clarification!=null&&clarification.vehicle().equals(vehicle)){
            history=clarification.history();clarification.responses().forEach((field,response)->{if(response.status().equals("CONFLICT"))conflicts.add("Unresolved clarification conflict: "+field.label()+"; existing="+response.existing()+"; proposed="+response.proposed()+". Existing value retained pending explicit resolution.");});
        }
        InspectionChecklist inspection;
        try{inspection=new ChecklistPlanner().build(vehicle,deterministic.knowledge(),missing);}
        catch(RuntimeException e){inspection=new ChecklistPlanner().build(vehicle,null,missing);limits.add("Local inspection notes unavailable; general inspection priorities retained.");deterministic=new RiskEvidence(vehicle,original.preference(),missing,original.knowledge(),original.violations(),cost,original.priorityGuidance(),limits,original.constraintsAvailable());}
        var evidence=new ReportEvidence(deterministic,assumptions,risk,riskStatus,inspection,history,conflicts,java.time.Year.now().getValue());
        String id=java.util.UUID.randomUUID().toString();
        if(!evidence.sufficient())return new ReportEvaluation(id,"INSUFFICIENT_INFORMATION",evidence.insufficient(),evidence,"Java sufficiency gate; no AI recommendation request was made.");
        final String prompt, schema;
        try{prompt=new PromptBuilder().buildRecommendationPrompt(evidence);schema=ReportSchema.json(evidence);}
        catch(RuntimeException e){return reportFailure(id,evidence,"[REQUEST_BUILD] Local report prompt/schema construction failed; no request was made.");}
        final String response;
        try{response=llmClient.generateStructured(prompt,schema);}
        catch(LLMFailure e){return reportFailure(id,evidence,e.diagnostic());}
        catch(RuntimeException e){return reportFailure(id,evidence,"[REQUEST_FAILURE] Unclassified client/request failure; raw exception details were withheld.");}
        var parser=new ResponseParser();final EvaluationReport parsed;
        try{parsed=parser.parseReport(response);}
        catch(IllegalArgumentException e){return reportFailure(id,evidence,"[REPORT_STRUCTURE] Generated text failed report JSON/schema validation (fields, types, category, bounds or duplicates).");}
        try{var report=parser.validateReport(parsed,evidence);return new ReportEvaluation(id,"AI_SUCCESS",report,evidence,"Validated Gemini-assisted advisory interpretation. User claims and mechanical condition are not independently verified.");}
        // validateReport emits only fixed application messages, never provider text or field values.
        catch(IllegalArgumentException e){return reportFailure(id,evidence,"[REPORT_EVIDENCE] "+e.getMessage());}
    }
    private static ReportEvaluation reportFailure(String id,ReportEvidence evidence,String diagnostic){
        return new ReportEvaluation(id,"AI_UNAVAILABLE",null,evidence,"AI recommendation unavailable: "+diagnostic+" Deterministic evidence remains available; retry explicitly.");
    }

}
