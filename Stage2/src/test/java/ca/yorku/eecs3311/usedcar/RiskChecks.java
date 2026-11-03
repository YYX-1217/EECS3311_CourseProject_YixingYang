package ca.yorku.eecs3311.usedcar;
import ca.yorku.eecs3311.usedcar.app.*;
import ca.yorku.eecs3311.usedcar.agent.UsedCarAgent;
import ca.yorku.eecs3311.usedcar.domain.*;
import ca.yorku.eecs3311.usedcar.result.*;
import ca.yorku.eecs3311.usedcar.llm.*;
import ca.yorku.eecs3311.usedcar.tool.*;
import ca.yorku.eecs3311.usedcar.ui.*;
import com.google.gson.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.io.*;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

public final class RiskChecks {
    public static List<String> required(String prompt,String prefix){
        String line=prompt.lines().filter(l->l.startsWith(prefix)).findFirst().orElseThrow();
        var list=new ArrayList<String>();for(var x:JsonParser.parseString(line.substring(line.indexOf(": ")+2)).getAsJsonArray())list.add(x.getAsString());return list;
    }
    public static String response(String prompt){return new Gson().toJson(new RiskAssessment("Moderate",List.of(),
            required(prompt,"Include each of these exact entries in risks"),required(prompt,"Include each of these exact entries in uncertainties"),
            "Advisory interpretation of the supplied evidence; review unresolved risks before proceeding."));}
    public static class Fake implements LLMClient {
        public String prompt;public int calls;public boolean fail;public String fixed;
        public String status(){return "OFFLINE FIXTURE";}
        public String generate(String p){throw new AssertionError("Risk requires structured generation");}
        public String generateStructured(String p,String schema){prompt=p;calls++;if(fail)throw new IllegalStateException("fictional-private-error");return fixed==null?response(p):fixed;}
    }
    public static UsedCarApplicationFacade facade(LLMClient llm){return new UsedCarApplicationFacade(agent(llm,new ArrayList<>(),false,false),new EvaluationSession());}
    private static UsedCarAgent agent(LLMClient llm,List<String> order,boolean knowledgeFailure,boolean constraintFailure){
        var tools=new ToolManager();var knowledge=new VehicleKnowledgeTool(new VehicleKnowledgeRepository());var constraints=new ConstraintCheckerTool();
        tools.registerTool("vehicleKnowledge",input->{order.add("knowledge");if(knowledgeFailure)throw new IllegalStateException();return knowledge.execute(input);});
        tools.registerTool("constraints",input->{order.add("constraints");if(constraintFailure)throw new IllegalStateException();return constraints.execute(input);});
        tools.registerTool("cost",new CostCalculatorTool());return new UsedCarAgent(tools,llm,new DataQualityChecker());
    }
    public static Map<String,String> complete(){var v=PreparationForm.example();v.put("accident","NO");v.put("maintenance","DOCUMENTED");return v;}
    public static void save(UsedCarApplicationFacade f){f.saveVehicle(PreparationForm.vehicle(f,complete()));}
    private static void rejects(Runnable r){try{r.run();}catch(IllegalArgumentException|IllegalStateException e){return;}throw new AssertionError("Expected rejection");}
    private static String valid(){return new Gson().toJson(new RiskAssessment("Moderate",List.of("Supplied evidence reviewed."),List.of(),List.of("Not an inspection."),"Advisory assessment."));}
    public static void parserValid(){var r=new ResponseParser().parseRiskAssessment(valid());check(r.riskLevel().equals("Moderate")&&r.strengths().size()==1,"Valid risk JSON must parse");try{r.strengths().add("bad");throw new AssertionError("Risk list must be immutable");}catch(UnsupportedOperationException expected){}}
    public static void category(){rejects(()->new ResponseParser().parseRiskAssessment(valid().replace("Moderate","Safe")));}
    public static void malformed(){for(String s:List.of("", "broken", "null", "[]", "{} trailing"))rejects(()->new ResponseParser().parseRiskAssessment(s));}
    public static void fields(){var j=JsonParser.parseString(valid()).getAsJsonObject();j.remove("risks");rejects(()->new ResponseParser().parseRiskAssessment(j.toString()));var k=JsonParser.parseString(valid()).getAsJsonObject();k.addProperty("strengths","not array");rejects(()->new ResponseParser().parseRiskAssessment(k.toString()));}
    public static void missingVehicle(){var f=facade(new Fake());rejects(f::evaluateCurrentVehicle);check(f.lastRiskEvaluation().isEmpty(),"No fabricated stored assessment");}
    public static void insufficient(){var llm=new Fake();var f=facade(llm);var v=complete();v.put("accident","");f.saveVehicle(PreparationForm.vehicle(f,v));var r=f.evaluateCurrentVehicle();check(r.assessment().riskLevel().equals("Insufficient Information")&&llm.calls==0&&r.status().equals("INSUFFICIENT_INFORMATION"),"Essential gaps must gate the API call");}
    public static void knowledge(){var llm=new Fake();var f=facade(llm);save(f);var r=f.evaluateCurrentVehicle();check(r.evidence().knowledge().summary().contains("Fictional")&&llm.prompt.contains("instructional")&&r.status().equals("AI_SUCCESS"),"Use actual labeled local knowledge");}
    public static void noKnowledge(){var llm=new Fake();var f=facade(llm);var v=complete();v.put("model","No matching model");f.saveVehicle(PreparationForm.vehicle(f,v));var r=f.evaluateCurrentVehicle();check(r.evidence().knowledge().equals(VehicleKnowledge.noData())&&r.assessment().uncertainties().stream().anyMatch(x->x.contains("No matching local")),"No-match is a limitation, not fabricated research");}
    public static void conflicts(){var llm=new Fake();var f=facade(llm);save(f);f.savePreferences(UserPreference.limits(new java.math.BigDecimal("1"),null));var r=f.evaluateCurrentVehicle();check(!r.evidence().requiredRisks().isEmpty()&&r.assessment().risks().containsAll(r.evidence().requiredRisks()),"Model must retain tool conflicts");var low=new RiskAssessment("Low",List.of(),r.assessment().risks(),r.assessment().uncertainties(),"Advisory");rejects(()->new ResponseParser().validateRiskAssessment(low,r.evidence()));}
    public static void preferences(){var llm=new Fake();var f=facade(llm);save(f);f.savePreferences(new UserPreference(null,null,Set.of(),"",5,1,"UNKNOWN"));f.selectStrategy("auto");f.evaluateCurrentVehicle();check(llm.prompt.contains("Reliability Focused"),"Auto must select using current priorities");f.savePreferences(new UserPreference(null,null,Set.of(),"",1,5,"UNKNOWN"));f.evaluateCurrentVehicle();check(llm.prompt.contains("Balanced"),"Auto must refresh after preference change");}
    public static void manual(){var llm=new Fake();var f=facade(llm);save(f);f.savePreferences(new UserPreference(null,null,Set.of(),"",5,1,"UNKNOWN"));f.selectStrategy("budget");f.evaluateCurrentVehicle();check(llm.prompt.contains("Budget Focused"),"Manual override must take priority over factory inference");}
    public static void order(){var order=new ArrayList<String>();var llm=new Fake(){@Override public String generateStructured(String p,String s){check(order.equals(List.of("knowledge","constraints")),"Tools must run before Gemini");order.add("llm");return super.generateStructured(p,s);}};var a=agent(llm,order,false,false);var f=new UsedCarApplicationFacade(a,new EvaluationSession());save(f);f.evaluateCurrentVehicle();check(order.equals(List.of("knowledge","constraints","llm")),"Actual tools must be invoked exactly once");}
    public static void prompt(){var llm=new Fake();var f=facade(llm);save(f);f.evaluateCurrentVehicle();check(llm.prompt.contains("untrusted")&&llm.prompt.contains("user-confirmed")&&llm.prompt.contains("No")&&llm.prompt.contains("Saved buyer preferences")&&llm.prompt.contains("Available Java cost result"),"Prompt must label evidence, priorities and missing cost");}
    public static void providerFailure(){var llm=new Fake();llm.fail=true;var f=facade(llm);save(f);var v=f.currentVehicle();var r=f.evaluateCurrentVehicle();check(r.assessment()==null&&r.status().equals("AI_UNAVAILABLE")&&r.evidence().knowledge()!=null&&f.currentVehicle().equals(v)&&!r.message().contains("fictional-private-error"),"Provider error must preserve evidence without exposing arbitrary exception or inventing risk");}
    public static void rejectedResponse(){var llm=new Fake();llm.fixed=valid();var f=facade(llm);save(f);var r=f.evaluateCurrentVehicle();check(r.assessment()==null&&r.message().contains("rejected"),"Omitted mandatory limitations must reject the model result");}
    public static void toolFailures(){var llm=new Fake();var f=new UsedCarApplicationFacade(agent(llm,new ArrayList<>(),true,false),new EvaluationSession());save(f);var r=f.evaluateCurrentVehicle();check(r.status().equals("AI_SUCCESS")&&r.evidence().knowledge()==null&&r.assessment().uncertainties().stream().anyMatch(x->x.contains("tool failed")),"Optional knowledge failure must be disclosed and retain remaining evidence");var second=new Fake();var g=new UsedCarApplicationFacade(agent(second,new ArrayList<>(),false,true),new EvaluationSession());save(g);check(g.evaluateCurrentVehicle().assessment().riskLevel().equals("Insufficient Information")&&second.calls==0,"Mandatory constraint failure must prevent unsupported categorization");}
    public static void costs(){var llm=new Fake();var f=facade(llm);PreparationForm.command(f,complete()).execute();var r=f.evaluateCurrentVehicle();check(r.evidence().cost().firstYearEstimate().toPlainString().equals("24922.00")&&llm.prompt.contains("24922.00"),"Existing exact cost result must be reused");var v=complete();v.put("price","19000");f.saveVehicle(PreparationForm.vehicle(f,v));check(f.evaluateCurrentVehicle().evidence().cost()==null,"Vehicle changes must invalidate stale costs");}
    public static void missingKey(){var f=facade(new GeminiAdapter("",null));save(f);var r=f.evaluateCurrentVehicle();check(r.assessment()==null&&r.evidence()!=null,"Missing key preserves local evidence without fake AI");}
    public static void neutral(){var llm=new Fake();var a=agent(llm,new ArrayList<>(),false,false);var f=facade(llm);var r=a.evaluateRisk(PreparationForm.vehicle(f,complete()),null,null);check(r.evidence().preference().equals(UserPreference.limits(null,null))&&r.evidence().priorityGuidance().contains("Balanced"),"Absent preferences must remain neutral");}
    public static void cli(){var f=facade(new Fake());save(f);var bytes=new ByteArrayOutputStream();new UsedCarCLI(f,new Scanner("evaluate\nquit\n"),new PrintStream(bytes)).run();check(bytes.toString().contains("Risk level: Moderate")&&bytes.toString().contains("DETERMINISTIC EVIDENCE SNAPSHOT"),"CLI must display structured assessment and authoritative evidence");}
    public static void runAll(){parserValid();category();malformed();fields();missingVehicle();insufficient();knowledge();noKnowledge();conflicts();preferences();manual();order();prompt();providerFailure();rejectedResponse();toolFailures();costs();missingKey();neutral();cli();}
}
