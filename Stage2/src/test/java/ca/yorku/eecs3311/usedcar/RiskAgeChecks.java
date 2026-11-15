package ca.yorku.eecs3311.usedcar;

import ca.yorku.eecs3311.usedcar.agent.*;
import ca.yorku.eecs3311.usedcar.app.*;
import ca.yorku.eecs3311.usedcar.domain.*;
import ca.yorku.eecs3311.usedcar.llm.*;
import ca.yorku.eecs3311.usedcar.result.*;
import ca.yorku.eecs3311.usedcar.tool.*;
import ca.yorku.eecs3311.usedcar.ui.*;
import com.google.gson.*;
import java.time.*;
import java.util.*;
import java.io.*;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

/** Fixed-clock regression for the observed live F08 six-year claim; no network or real credentials. */
public final class RiskAgeChecks {
    public static final String BAD="Vehicle age of 6 years combined with high mileage increases potential for mechanical wear.";
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-11-07T12:00:00Z"),ZoneId.of("America/Toronto"));
    public static class Fake extends ReportChecks.Fake {
        public String field="risks",claim;
        @Override public String generateStructured(String prompt,String schema){
            String result=super.generateStructured(prompt,schema);
            if(claim==null||!prompt.startsWith("Assess advisory"))return result;
            var json=JsonParser.parseString(result).getAsJsonObject();
            if(field.equals("explanation"))json.addProperty(field,claim);else json.getAsJsonArray(field).add(claim);
            return json.toString();
        }
    }
    private static UsedCarAgent agent(LLMClient llm,Clock clock){
        var tools=new ToolManager();tools.registerTool("cost",new CostCalculatorTool());tools.registerTool("constraints",new ConstraintCheckerTool());
        tools.registerTool("vehicleKnowledge",new VehicleKnowledgeTool(new VehicleKnowledgeRepository()));return new UsedCarAgent(tools,llm,new DataQualityChecker(),clock);
    }
    public static UsedCarApplicationFacade saved(LLMClient llm){
        var f=new UsedCarApplicationFacade(agent(llm,CLOCK),new EvaluationSession());PreparationForm.command(f,ReportChecks.corolla()).execute();return f;
    }
    private static void bad(String field,String claim){
        var llm=new Fake();llm.field=field;llm.claim=claim;var f=saved(llm);var r=f.evaluateCurrentVehicle();
        check(r.status().equals("AI_UNAVAILABLE")&&r.assessment()==null&&r.message().contains("[RISK_AGE_EVIDENCE]")&&r.message().contains(field),"Unsupported age must fail in "+field);
        check(!ResultFormatter.format(r).contains(claim)&&r.evidence().cost().firstYearEstimate().intValue()==18707&&llm.riskCalls==1,"No rejected prose, invented category or hidden retries; preserve costs");
    }
    public static void arithmetic(){check(new ModelYearContext(2018,2026).difference()==8,"2018/2026 difference is eight");check(new ModelYearContext(2018,2026).description().contains("not the exact in-service age"),"Difference is not time in service");}
    public static void promptContext(){var llm=new Fake();var r=saved(llm).evaluateCurrentVehicle();check(r.evidence().referenceYear()==2026&&llm.riskCalls==1,"Reference year captured from injected clock");
        // The F08 Fake stores its last prompt only for report calls, so inspect the production builder directly.
        String prompt=new PromptBuilder().buildRiskPrompt(r.evidence());check(prompt.contains("modelYearDifference=8")&&prompt.contains("\"referenceYear\":2026")&&prompt.contains("not the exact in-service age")&&prompt.contains("Do not assert a numerical vehicle age in any section"),"Authoritative concise context and age instruction");}
    public static void risks(){bad("risks",BAD);}
    public static void explanation(){bad("explanation","This car is six years old.");}
    public static void strengths(){bad("strengths","A six-year-old vehicle is a positive factor.");}
    public static void uncertainties(){bad("uncertainties","The vehicle is 6 years old.");}
    public static void actualAge(){bad("risks","This vehicle is eight years old.");bad("explanation","The car has been in service for 8 years.");}
    public static void spelling(){for(String text:List.of("A six–year–old vehicle.","Vehicle age: 6.5 years.","Vehicle aged six years.","This car has 8 years of service."))bad("risks",text);}
    public static void wrongDifference(){bad("risks","Model-year difference: 6 years.");bad("explanation","The model year gap is six years.");}
    public static void wrongContext(){bad("explanation","Reference year 2024; model year 2018.");bad("risks","Model year 2017.");}
    public static void validGuidance(){
        for(String text:List.of("Aging and wear warrant inspection; maintenance history needs review.","Exact vehicle age is unknown; inspect age-related wear.","CAD 12500 and 125000 km; reliability priority 5/5; annual fuel 1080 L and CAD 1782.","Model-year difference: eight years; reference year 2026, model year 2018. This is not exact time in service.")){
            var llm=new Fake();llm.claim=text;for(String field:List.of("strengths","risks","uncertainties","explanation")){llm.field=field;check(saved(llm).evaluateCurrentVehicle().status().equals("AI_SUCCESS"),"Valid generic/numeric context must not be blocked: "+field);}
        }
    }
    public static void invalidContext(){
        for(var context:List.of(new ModelYearContext(null,2026),new ModelYearContext(2018,null),new ModelYearContext(0,2026),new ModelYearContext(2018,0),new ModelYearContext(3000,2026))){
            check(context.difference()==null&&context.description().contains("UNAVAILABLE"),"Missing/invalid context cannot infer age");
            var generic=new RiskAssessment("Moderate",List.of(),List.of(),List.of(),"Inspect wear and uncertain history.");RiskAgeValidator.validate(generic,context);
            try{RiskAgeValidator.validate(new RiskAssessment("Moderate",List.of(),List.of("Model-year difference: 8 years."),List.of(),"Review evidence."),context);throw new AssertionError("Unsupported difference accepted");}catch(RiskAgeValidator.Rejected expected){}
        }
        check(new ModelYearContext(2027,2026).difference()==-1,"Next model year is arithmetic, never negative actual age");
    }
    public static void noRetry(){var llm=new Fake();llm.claim=BAD;var f=saved(llm);for(int i=0;i<3;i++){check(f.evaluateCurrentVehicle().assessment()==null,"Repeated invalid output stays unavailable");check(llm.riskCalls==i+1,"One request per explicit retry");}}
    public static void replacesPriorSuccess(){var llm=new Fake();var f=saved(llm);check(f.evaluateCurrentVehicle().status().equals("AI_SUCCESS"),"Baseline valid risk");llm.claim=BAD;f.evaluateCurrentVehicle();check(f.lastRiskEvaluation().orElseThrow().assessment()==null,"Rejected new result cannot leave old success current");}
    public static void clarification(){var llm=new Fake();var f=saved(llm);f.evaluateCurrentVehicle();var c=f.revisitClarification(ClarificationField.YEAR);f.submitClarification(c.cycleId(),ClarificationField.YEAR,"2020");f.resolveClarification(c.cycleId(),ClarificationField.YEAR,true);check(f.lastRiskEvaluation().isEmpty(),"Accepted F11 year invalidates old risk");var r=f.evaluateCurrentVehicle();check(r.status().equals("AI_SUCCESS")&&r.evidence().yearContext().difference()==6,"New accepted model year recalculates F08 context");}
    public static void reportReuse(){var llm=new Fake();var f=saved(llm);var risk=f.evaluateCurrentVehicle();var report=f.generateCurrentReport();check(report.evidence().risk()==risk.assessment()&&report.evidence().referenceYear()==2026&&llm.riskCalls==1,"F12 reuses valid current F08 without another request");}
    public static void reportRejects(){var llm=new Fake();llm.claim=BAD;var f=saved(llm);f.evaluateCurrentVehicle();var r=f.generateCurrentReport();check(r.evidence().risk()==null&&r.evidence().costSummary().contains("18707.00")&&!ReportText.format(r).contains(BAD),"F12 cannot treat rejected F08 as validated risk");}
    public static void referenceYearChange(){var llm=new Fake();var f=saved(llm);var risk=f.evaluateCurrentVehicle();var next=agent(llm,Clock.fixed(Instant.parse("2027-01-02T12:00:00Z"),ZoneOffset.UTC));
        var r=next.generateRecommendation(f.currentVehicle().orElseThrow(),f.preferences(),risk.evidence().cost(),f.costAssumptions().orElseThrow(),risk,null,Set.of());
        check(r.evidence().risk()==null&&r.evidence().referenceYear()==2027,"F12 cannot reuse risk computed against an older reference year");}
    public static void clockSnapshot(){var llm=new Fake();var r=saved(llm).evaluateCurrentVehicle();check(new PromptBuilder().buildRiskPrompt(r.evidence()).contains("modelYearDifference=8")&&ResultFormatter.format(r).contains("reference year 2026"),"Prompt, validation and display reuse one year snapshot");}
    public static void cli(){var llm=new Fake();llm.claim=BAD;var out=new ByteArrayOutputStream();new UsedCarCLI(saved(llm),new Scanner("evaluate\nevaluate\nquit\n"),new PrintStream(out)).run();String text=out.toString();check(text.contains("[RISK_AGE_EVIDENCE]")&&text.contains("Model-year difference: 8")&&text.contains("18707")&&!text.contains(BAD)&&!text.contains("AI_SUCCESS")&&llm.riskCalls==2,"Actual CLI retains deterministic evidence and safe manual retry");}

    /** AUD-01: same accepted response, with only one contradictory risk statement added. */
    public static void aud01PrefixRejected(){
        var llm=new Fake();var f=saved(llm);
        check(f.evaluateCurrentVehicle().status().equals("AI_SUCCESS"),"Unmodified fixed-2026/2018 fixture must be valid");
        llm.claim="The vehicle has a 6-year model-year difference.";
        var r=f.evaluateCurrentVehicle();
        check(r.evidence().yearContext().equals(new ModelYearContext(2018,2026)),"Reproduction must use exactly 2026 minus 2018");
        check(r.status().equals("AI_UNAVAILABLE")&&r.assessment()==null
                &&r.message().contains("[RISK_AGE_EVIDENCE]")&&llm.riskCalls==2,
                "AUD-01: contradictory 6-year prefix must be rejected without an extra request");
        for(String claim:List.of("6-year model-year difference","a 6-year model-year difference",
                "six-year model-year difference","A six–year model–year difference.",
                "A 6.5-year model-year difference."))bad("risks",claim);
    }
    public static void aud01PrefixAccepted(){
        for(String claim:List.of("The vehicle has an 8-year model-year difference.",
                "An eight-year model-year difference.","8 year model year gap","An 8–year model–year difference.")){
            var llm=new Fake();llm.claim=claim;
            for(String field:List.of("strengths","risks","uncertainties","explanation")){
                llm.field=field;var r=saved(llm).evaluateCurrentVehicle();
                check(r.status().equals("AI_SUCCESS")&&ResultFormatter.format(r).contains(claim),
                        "Correct explicit model-year difference must remain unchanged in "+field);
            }
        }
    }
    public static void aud01SuffixForms(){
        for(String claim:List.of("model-year difference of 6 years","model year difference is 6 years",
                "model-year difference of six years"))bad("risks",claim);
        for(String claim:List.of("model-year difference of 8 years","model year difference is 8 years",
                "model-year difference of eight years")){
            var llm=new Fake();llm.claim=claim;check(saved(llm).evaluateCurrentVehicle().status().equals("AI_SUCCESS"),"Existing correct suffix form preserved");
        }
    }
    public static void aud01UnrelatedNumbers(){
        for(String claim:List.of("Mileage 125000 km; price CAD 12500; annual distance 15000 km.",
                "For a hypothetical 6-year ownership period, review future costs separately.",
                "A maintenance record dated 2020 does not establish the vehicle's first registration date.",
                "For comparison, another vehicle has a 6-year model-year difference; this vehicle has an 8-year model-year difference.",
                "The other vehicle has a six-year model-year difference. This vehicle has an eight-year model-year difference.",
                "Vehicle B has a 6-year model-year difference; the evaluated vehicle has an 8-year model-year difference.")){
            var llm=new Fake();llm.claim=claim;
            check(saved(llm).evaluateCurrentVehicle().status().equals("AI_SUCCESS"),"Unrelated numbers or clearly identified other-vehicle comparison must not be rejected");
        }
        bad("risks","Another vehicle has a 6-year model-year difference; this vehicle has a 7-year model-year difference.");
        bad("risks","Another vehicle has a 6-year model-year difference, but this vehicle has a 7-year model-year difference.");
    }
    public static void aud01ExactAgeRejected(){
        bad("risks","This vehicle is 8 years old.");
        bad("explanation","The vehicle has been in service for eight years.");
    }
    public static void aud01SuppliedContext(){
        for(var context:List.of(new ModelYearContext(2020,2029),new ModelYearContext(2010,2031),
                new ModelYearContext(2027,2026))){
            String valid="The vehicle has a "+context.difference()+"-year model-year difference.";
            var assessment=new RiskAssessment("Moderate",List.of(),List.of(valid),List.of(),"Review the supplied evidence.");
            RiskAgeValidator.validate(assessment,context);
            try{
                RiskAgeValidator.validate(new RiskAssessment("Moderate",List.of(),List.of("The vehicle has an 8-year model-year difference."),List.of(),"Review evidence."),context);
                throw new AssertionError("Difference must come from supplied Java context");
            }catch(RiskAgeValidator.Rejected expected){}
        }
        RiskAgeValidator.validate(new RiskAssessment("Moderate",List.of(),List.of("A twenty-one-year model-year difference."),List.of(),"Review evidence."),new ModelYearContext(2010,2031));
        try{
            RiskAgeValidator.validate(new RiskAssessment("Moderate",List.of(),List.of("An 8-year model-year difference."),List.of(),"Review evidence."),new ModelYearContext(null,2026));
            throw new AssertionError("Missing context cannot validate a numerical difference");
        }catch(RiskAgeValidator.Rejected expected){}
    }
    public static void aud01EveryNarrativeField(){
        for(String field:List.of("strengths","risks","uncertainties","explanation"))
            bad(field,"The vehicle has a 6-year model-year difference.");
    }
    public static void aud01RejectedRiskNotReused(){
        var llm=new Fake();var f=saved(llm);
        check(f.evaluateCurrentVehicle().status().equals("AI_SUCCESS"),"Initial valid assessment");
        llm.claim="The vehicle has a 6-year model-year difference.";
        var r=f.evaluateCurrentVehicle();var report=f.generateCurrentReport();
        check(r.assessment()==null&&f.lastRiskEvaluation().orElseThrow().assessment()==null
                &&report.evidence().risk()==null&&llm.riskCalls==2,"Rejected response supplies no validated risk to cache/F12 and triggers no retry");
        check(r.evidence().cost().firstYearEstimate().intValue()==18707
                &&!ResultFormatter.format(r).contains(llm.claim)&&!ReportText.format(report).contains(llm.claim),
                "Preserve deterministic costs and withhold rejected text without rewriting it");
    }

    public static void runAll(){aud01PrefixRejected();aud01PrefixAccepted();aud01SuffixForms();aud01UnrelatedNumbers();aud01ExactAgeRejected();aud01SuppliedContext();aud01EveryNarrativeField();aud01RejectedRiskNotReused();arithmetic();promptContext();risks();explanation();strengths();uncertainties();actualAge();spelling();wrongDifference();wrongContext();validGuidance();invalidContext();noRetry();replacesPriorSuccess();clarification();reportReuse();reportRejects();referenceYearChange();clockSnapshot();cli();}
}
