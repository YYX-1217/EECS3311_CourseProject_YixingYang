package ca.yorku.eecs3311.usedcar;

import ca.yorku.eecs3311.usedcar.domain.*;
import ca.yorku.eecs3311.usedcar.llm.*;
import ca.yorku.eecs3311.usedcar.result.*;
import ca.yorku.eecs3311.usedcar.ui.*;
import java.util.*;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

/** AUD-02: real F08 parse/validate/cache path, fictional provider, fixed 2026 context. */
public final class RiskGroundingChecks {
    public static final String OBSERVED="The vehicle has an 8-year model-year difference, placing it in a category where mechanical wear is statistically more likely";
    private static void rejected(String field,String claim){
        var llm=new RiskAgeChecks.Fake();llm.field=field;llm.claim=claim;
        var r=RiskAgeChecks.saved(llm).evaluateCurrentVehicle();
        check(r.status().equals("AI_UNAVAILABLE")&&r.assessment()==null,"Unsupported assertion must be rejected in "+field);
        check(r.message().contains("[RISK_GROUNDING]")&&r.message().contains(field),"Sanitized field-specific diagnostic");
        check(!ResultFormatter.format(r).contains(claim)&&r.evidence().cost().firstYearEstimate().intValue()==18707&&llm.riskCalls==1,"Withhold prose/category, retain Java costs, no automatic retry");
    }
    private static void accepted(String field,String claim){
        var llm=new RiskAgeChecks.Fake();llm.field=field;llm.claim=claim;
        var r=RiskAgeChecks.saved(llm).evaluateCurrentVehicle();
        check(r.status().equals("AI_SUCCESS")&&ResultFormatter.format(r).contains(claim),"Valid guidance must be retained verbatim in "+field+": "+claim);
    }
    public static void observed(){
        var llm=new RiskAgeChecks.Fake();var f=RiskAgeChecks.saved(llm);var valid=f.evaluateCurrentVehicle();
        check(valid.status().equals("AI_SUCCESS")&&valid.evidence().yearContext().equals(new ModelYearContext(2018,2026))&&valid.evidence().knowledge().equals(VehicleKnowledge.noData()),"Otherwise valid reproduction with no Corolla statistics");
        rejected("risks",OBSERVED);
    }
    public static void numeric(){for(String text:List.of("This vehicle has an 80% probability of transmission failure.","The failure rate is 25 percent.","Reliability is rated at 98.5%.","There is a 70% chance of breakdown.","The failure probability is 0.8.","The probability of transmission failure is 0.8."))rejected("explanation",text);}
    public static void mechanical(){for(String text:List.of("This Corolla has documented transmission failures.","The model has a known engine defect.","The engine will fail soon.","An imminent mechanical breakdown is expected."))rejected("risks",text);}
    public static void fakeSource(){for(String text:List.of("The local CSV confirms this model has reliable brakes.","According to the CSV record, transmission failure is common.","The local knowledge shows an 80% failure probability."))rejected("strengths",text);}
    public static void benign(){for(String text:List.of("The vehicle's mechanical condition has not been independently inspected.","Higher mileage warrants careful inspection.","The model-year difference is 8 years, which is not the exact time in service.","CAD 12500, 125000 km, 13% purchase tax; inspect the transmission before purchase.","Wear is a risk to review with an inspector, not a measured failure prediction.","Purchase tax is 13%, not a failure probability."))accepted("risks",text);}
    public static void denials(){for(String text:List.of("The available information does not establish a statistical failure probability.","No statistical evidence is available to establish that mechanical wear is statistically more likely.","There is no evidence of documented model-specific failures.","Failure probability is unknown.","Statistical risk is unknown.","The available data cannot establish an 80% failure probability."))accepted("uncertainties",text);
        rejected("uncertainties","There is no statistical source, but mechanical failure is statistically more likely.");
        rejected("uncertainties","No statistical source is available; the failure rate is 80%.");
    }
    public static void fields(){for(String field:List.of("strengths","risks","uncertainties","explanation")){rejected(field,OBSERVED);accepted(field,"Review saved evidence and obtain an independent inspection.");}}
    public static void localAttribution(){
        var evidence=RiskAgeChecks.saved(new RiskAgeChecks.Fake()).evaluateCurrentVehicle().evidence();
        var knowledge=new VehicleKnowledge("Fictional retrieval example",List.of("Sample concern: documented transmission failures"),List.of("Inspect the transmission"),"Instructional fictional CSV");
        var sourced=new RiskEvidence(evidence.vehicle(),evidence.preference(),evidence.missing(),knowledge,evidence.violations(),evidence.cost(),evidence.priorityGuidance(),evidence.limitations(),true,2026);
        String quote="Local knowledge (unverified; source: Instructional fictional CSV): Sample concern: documented transmission failures";
        var base=RiskAgeChecks.saved(new RiskAgeChecks.Fake()).evaluateCurrentVehicle().assessment();
        var risks=new ArrayList<>(base.risks());risks.add(quote);
        var parser=new ResponseParser();var assessment=new RiskAssessment(base.riskLevel(),base.strengths(),risks,base.uncertainties(),base.explanation());
        check(parser.validateRiskAssessment(assessment,sourced)==assessment,"Exact source attribution remains explicitly unverified");
        try{parser.validateRiskAssessment(assessment,evidence);throw new AssertionError("Missing CSV source accepted");}catch(IllegalArgumentException expected){}
        risks.set(risks.size()-1,quote+"; the engine will fail soon.");
        try{parser.validateRiskAssessment(new RiskAssessment(base.riskLevel(),base.strengths(),risks,base.uncertainties(),base.explanation()),sourced);throw new AssertionError("True quote cannot support appended invented diagnosis");}catch(IllegalArgumentException expected){}
    }
    public static void userAttribution(){
        var llm=new RiskAgeChecks.Fake();var f=RiskAgeChecks.saved(llm);var form=ReportChecks.corolla();form.put("notes","The engine will fail soon.");f.saveVehicle(PreparationForm.vehicle(f,form));
        llm.claim="User-provided unverified claim: The engine will fail soon.";
        check(f.evaluateCurrentVehicle().status().equals("AI_SUCCESS"),"Exact seller claim may be quoted without endorsing it");
        llm.claim="User-provided unverified claim: The transmission will fail soon.";
        check(f.evaluateCurrentVehicle().assessment()==null,"Cannot fabricate even an unverified user claim");
    }
    public static void noReuse(){
        var llm=new RiskAgeChecks.Fake();var f=RiskAgeChecks.saved(llm);check(f.evaluateCurrentVehicle().status().equals("AI_SUCCESS"),"Initial success");
        llm.claim=OBSERVED;var r=f.evaluateCurrentVehicle();var report=f.generateCurrentReport();
        check(r.assessment()==null&&f.lastRiskEvaluation().orElseThrow().assessment()==null&&report.evidence().risk()==null&&llm.riskCalls==2,"Rejected F08 cannot remain current or feed F12; no hidden retry");
    }
    public static void runAll(){observed();numeric();mechanical();fakeSource();benign();denials();fields();localAttribution();userAttribution();noReuse();}
}
