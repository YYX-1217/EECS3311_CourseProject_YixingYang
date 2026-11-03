package ca.yorku.eecs3311.usedcar;

import ca.yorku.eecs3311.usedcar.agent.UsedCarAgent;
import ca.yorku.eecs3311.usedcar.app.*;
import ca.yorku.eecs3311.usedcar.command.CompareVehiclesCommand;
import ca.yorku.eecs3311.usedcar.domain.*;
import ca.yorku.eecs3311.usedcar.llm.*;
import ca.yorku.eecs3311.usedcar.result.*;
import ca.yorku.eecs3311.usedcar.tool.*;
import ca.yorku.eecs3311.usedcar.ui.*;
import com.google.gson.*;
import java.math.BigDecimal;
import java.util.*;
import java.io.*;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

/** Offline fixtures exercise the actual facade, agent, production tools, parser and CLI. */
public final class ComparisonChecks {
    public static class Fake implements LLMClient {
        public int calls; public String prompt; public String schema; public String fixed; public boolean fail;
        public String status(){return "OFFLINE FIXTURE";}
        public String generate(String p){throw new AssertionError("Comparison must request structured JSON");}
        public String generateStructured(String p,String s){
            calls++;prompt=p;schema=s;
            if(fail)throw new IllegalStateException("fictional-private-provider-detail");
            return fixed==null?response(p):fixed;
        }
    }
    public static String response(String prompt) {
        String prefix="Copy firstVehicleId, secondVehicleId and metricSummary exactly from: ";
        String line=prompt.lines().filter(x->x.startsWith(prefix)).findFirst().orElseThrow();
        var json=JsonParser.parseString(line.substring(prefix.length())).getAsJsonObject();
        json.addProperty("preferredVehicleId","");
        json.addProperty("explanation","A and B have different price, mileage and ownership-cost trade-offs. Review the displayed buyer constraints and history limitations. Instructional knowledge is not verified research; no defensible preference is established by this offline fixture.");
        return json.toString();
    }
    public static UsedCarApplicationFacade facade(LLMClient llm) { return RiskChecks.facade(llm); }
    public static Map<String,String> secondValues(){
        var v=RiskChecks.complete();v.put("model","Second Sample");v.put("price","16000");v.put("mileage","60000");v.put("consumption","6");return v;
    }
    public static void savePair(UsedCarApplicationFacade f,boolean costs){
        if(costs)PreparationForm.command(f,RiskChecks.complete()).execute();else RiskChecks.save(f);
        f.saveComparisonVehicle(PreparationForm.comparisonVehicle(f,secondValues()));
    }
    private static UsedCarApplicationFacade prepared(Fake llm){var f=facade(llm);savePair(f,true);return f;}
    private static void rejects(Runnable action){
        try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("Expected rejection");
    }
    public static void storage(){
        var f=prepared(new Fake());var a=f.currentVehicle().orElseThrow();var prefs=f.preferences();var assumptions=f.costAssumptions();var b=f.comparisonVehicle().orElseThrow();
        var v=secondValues();v.put("price","15500");f.saveComparisonVehicle(PreparationForm.comparisonVehicle(f,v));
        check(f.currentVehicle().orElseThrow().equals(a)&&f.preferences().equals(prefs)&&f.costAssumptions().equals(assumptions),"Saving/editing B must preserve A, preferences and common assumptions");
        check(f.comparisonVehicle().orElseThrow().id().equals(b.id())&&!a.id().equals(b.id()),"B retains a separate stable identity");
    }
    public static void metrics(){
        var f=prepared(new Fake());var r=f.compareCurrentVehicles();var m=r.comparison().metricSummary();
        check(m.get("Asking price (CAD)").equals("A: 18000 | B: 16000")&&m.get("Mileage (km)").equals("A: 80000 | B: 60000"),"Compare actual CAD/km values symmetrically");
        check(m.get("Price difference A-B (CAD)").equals("2000")&&m.get("Mileage difference A-B (km)").equals("20000"),"Differences must be calculated by Java");
        try{m.put("injected","bad");throw new AssertionError("Metrics must be immutable");}catch(UnsupportedOperationException expected){}
    }
    public static void constraints(){
        var llm=new Fake();var f=facade(llm);var a=RiskChecks.complete();a.put("accident","YES");f.saveVehicle(PreparationForm.vehicle(f,a));
        var b=secondValues();b.put("accident","YES");f.saveComparisonVehicle(PreparationForm.comparisonVehicle(f,b));
        f.savePreferences(new UserPreference(new BigDecimal("10000"),50000,Set.of("MANUAL"),"",5,1,"NO_ACCIDENTS"));
        var r=f.compareCurrentVehicles();
        check(r.evidence().first().violations().size()==4&&r.evidence().second().violations().size()==4,"Both vehicles must use all four actual constraint checks");
        check(llm.prompt.contains("Reliability Focused")&&llm.prompt.contains("Maximum budget"),"Saved priorities and actual constraints must reach model");
    }
    public static void costs(){
        var f=prepared(new Fake());var r=f.compareCurrentVehicles();
        check(r.evidence().first().cost().firstYearEstimate().compareTo(new BigDecimal("24922"))==0,"A must use actual production cost result");
        check(r.evidence().second().cost().firstYearEstimate().compareTo(new BigDecimal("22365"))==0,"B must use same distance, fuel price, tax and allowances with B consumption/price");
        var changed=RiskChecks.complete();changed.put("price","19000");f.saveVehicle(PreparationForm.vehicle(f,changed));
        check(f.compareCurrentVehicles().evidence().first().cost().firstYearEstimate().compareTo(new BigDecimal("26052"))==0,"Recompute A after edits, preserving common assumptions");
    }
    public static void missingCosts(){
        var f=facade(new Fake());savePair(f,false);var r=f.compareCurrentVehicles();
        check(r.evidence().assumptions()==null&&r.evidence().first().cost()==null&&r.evidence().second().cost()==null,"No assumptions must not create costs");
        check(r.comparison().metricSummary().get("First-year estimate (CAD)").equals("A: UNAVAILABLE | B: UNAVAILABLE"),"Unavailable costs must remain explicit");
        var v=RiskChecks.complete();v.put("taxRate","");PreparationForm.command(f,v).execute();r=f.compareCurrentVehicles();
        check(r.evidence().first().cost().firstYearEstimate()==null&&r.evidence().second().cost().firstYearEstimate()==null,"Optional tax missing prevents totals for BOTH vehicles");
        check(r.evidence().second().cost().annualFuelCost().compareTo(new BigDecimal("1485"))==0,"Partial known costs remain usable");
    }
    public static void knowledge(){
        var llm=new Fake();var f=prepared(llm);var r=f.compareCurrentVehicles();
        check(r.evidence().first().knowledge().summary().contains("Fictional")&&r.evidence().second().knowledge().equals(VehicleKnowledge.noData()),"Use actual CSV and explicit no-match results");
        check(llm.prompt.contains("instructional")&&llm.prompt.contains("No matching local"),"No-match and source limitations must reach model");
    }
    public static void partial(){
        var llm=new Fake();var f=prepared(llm);var b=secondValues();b.put("consumption","");f.saveComparisonVehicle(PreparationForm.comparisonVehicle(f,b));
        var r=f.compareCurrentVehicles();check(r.evidence().second().cost().annualFuelCost()==null&&r.evidence().first().cost().annualFuelCost()!=null,"Missing optional fuel consumption must remain asymmetric unknown, not zero");
        b.put("accident","UNKNOWN");f.saveComparisonVehicle(PreparationForm.comparisonVehicle(f,b));int before=llm.calls;r=f.compareCurrentVehicles();
        check(r.status().equals("INSUFFICIENT_INFORMATION")&&llm.calls==before&&r.comparison().preferredVehicleId().isEmpty()&&r.comparison().explanation().isEmpty(),"Essential history gap must prevent unsupported winner and paid call");
        check(r.comparison().metricSummary().get("Asking price (CAD)").contains("16000"),"Partial comparison retains metrics");
    }
    public static void sameVehicle(){
        var llm=new Fake();var f=prepared(llm);var a=f.currentVehicle().orElseThrow();var b=f.comparisonVehicle();
        rejects(()->f.saveComparisonVehicle(a));rejects(()->f.compareVehicles(a,a));
        check(f.comparisonVehicle().equals(b)&&llm.calls==0,"Reject self-comparison without corrupting B or invoking AI");
    }
    public static void missingVehicles(){
        var llm=new Fake();var f=facade(llm);rejects(f::compareCurrentVehicles);RiskChecks.save(f);rejects(f::compareCurrentVehicles);
        check(llm.calls==0,"Missing A/B must reject before provider call");
    }
    public static void invalidVehicle(){
        var f=prepared(new Fake());var b=f.comparisonVehicle();var a=f.currentVehicle();
        for(String key:List.of("year","mileage","price","consumption")){
            var values=secondValues();values.put(key,"-1");rejects(()->f.saveComparisonVehicle(PreparationForm.comparisonVehicle(f,values)));
        }
        for(String key:List.of("make","model")){
            var values=secondValues();values.put(key,"");rejects(()->f.saveComparisonVehicle(PreparationForm.comparisonVehicle(f,values)));
        }
        check(f.currentVehicle().equals(a)&&f.comparisonVehicle().equals(b),"Invalid B input must preserve previous records");
    }
    public static void units(){
        var f=prepared(new Fake());
        for(var entry:Map.of("mileage","10000 miles","price","USD 12000","consumption","30 mpg").entrySet()){
            var values=secondValues();values.put(entry.getKey(),entry.getValue());rejects(()->PreparationForm.comparisonVehicle(f,values));
        }
        check(ResultFormatter.format(f.compareCurrentVehicles()).contains("CAD / km / L/100 km"),"Fixed model units must be explicit; foreign unit text is rejected");
    }
    public static void order(){
        var order=new ArrayList<String>();var llm=new Fake(){public String generateStructured(String p,String s){
            check(order.equals(List.of("cost","knowledge","constraints","cost","knowledge","constraints")),"All symmetric tools must finish before one LLM request");return super.generateStructured(p,s);
        }};
        var f=instrumented(llm,order,"");savePair(f,false);
        f.estimateCost(f.currentVehicle().orElseThrow(),new CostAssumptions(new BigDecimal("15000"),new BigDecimal("1.65"),null,null,null));order.clear();
        f.compareCurrentVehicles();check(llm.calls==1,"No per-vehicle AI calls or retries");
    }
    private static UsedCarApplicationFacade instrumented(LLMClient llm,List<String> order,String failTool){
        var tools=new ToolManager();var cost=new CostCalculatorTool();var knowledge=new VehicleKnowledgeTool(new VehicleKnowledgeRepository());var constraints=new ConstraintCheckerTool();
        tools.registerTool("cost",input->{order.add("cost");if(failTool.equals("cost"))throw new IllegalStateException();return cost.execute(input);});
        tools.registerTool("vehicleKnowledge",input->{order.add("knowledge");if(failTool.equals("knowledge"))throw new IllegalStateException();return knowledge.execute(input);});
        tools.registerTool("constraints",input->{order.add("constraints");if(failTool.equals("constraints"))throw new IllegalStateException();return constraints.execute(input);});
        return new UsedCarApplicationFacade(new UsedCarAgent(tools,llm,new DataQualityChecker()),new EvaluationSession());
    }
    public static void prompt(){
        var llm=new Fake();var f=prepared(llm);f.selectStrategy("budget");f.compareCurrentVehicles();
        check(llm.prompt.contains("untrusted")&&llm.prompt.contains("Budget Focused")&&llm.prompt.contains("annualKm")&&llm.prompt.contains("24922"),"Prompt must contain real evidence, costs and guidance with source boundaries");
        var schema=JsonParser.parseString(llm.schema).getAsJsonObject();check(schema.getAsJsonObject("properties").getAsJsonObject("metricSummary").getAsJsonObject("properties").has("Price difference A-B (CAD)"),"Structured schema must constrain actual metrics");
    }
    public static void parserValid(){
        var llm=new Fake();var f=prepared(llm);var result=f.compareCurrentVehicles();var parsed=new ResponseParser().parseComparison(response(llm.prompt));
        check(result.status().equals("AI_SUCCESS")&&parsed.equals(result.comparison())&&parsed.preferredVehicleId().isEmpty(),"Valid five-field result and explicit no-preference must parse");
        var j=JsonParser.parseString(response(llm.prompt)).getAsJsonObject();j.addProperty("preferredVehicleId",f.comparisonVehicle().orElseThrow().id());
        check(new ResponseParser().validateComparison(new ResponseParser().parseComparison(j.toString()),result.evidence()).preferredVehicleId().equals(f.comparisonVehicle().orElseThrow().id()),"Supported pair ID is accepted");
    }
    public static void malformed(){for(String s:List.of("","broken","null","[]","{} trailing","```json\n{}\n```"))rejects(()->new ResponseParser().parseComparison(s));}
    public static void fields(){
        var llm=new Fake();prepared(llm).compareCurrentVehicles();var valid=JsonParser.parseString(response(llm.prompt)).getAsJsonObject();
        for(String field:List.of("firstVehicleId","secondVehicleId","metricSummary","preferredVehicleId","explanation")){
            var missing=valid.deepCopy();missing.remove(field);rejects(()->new ResponseParser().parseComparison(missing.toString()));
            var j=valid.deepCopy();j.add(field,JsonNull.INSTANCE);String text=j.toString();rejects(()->new ResponseParser().parseComparison(text));
        }
        String duplicate=response(llm.prompt).replaceFirst("\\{", "{\"explanation\":\"earlier\",");
        rejects(()->new ResponseParser().parseComparison(duplicate));
        String duplicateMetric=response(llm.prompt).replace("\"metricSummary\":{", "\"metricSummary\":{\"Year\":\"earlier\",");
        rejects(()->new ResponseParser().parseComparison(duplicateMetric));
        var j=valid.deepCopy();j.addProperty("explanation"," ");rejects(()->new ResponseParser().parseComparison(j.toString()));
        var k=valid.deepCopy();k.getAsJsonObject("metricSummary").addProperty("Year",2020);rejects(()->new ResponseParser().parseComparison(k.toString()));
    }
    public static void identifiers(){
        var llm=new Fake();var r=prepared(llm).compareCurrentVehicles();var j=JsonParser.parseString(response(llm.prompt)).getAsJsonObject();
        j.addProperty("preferredVehicleId","unrelated-id");rejects(()->new ResponseParser().parseComparison(j.toString()));
        j.addProperty("preferredVehicleId","");j.addProperty("firstVehicleId","unrelated-id");
        rejects(()->new ResponseParser().validateComparison(new ResponseParser().parseComparison(j.toString()),r.evidence()));
    }
    public static void changedMetrics(){
        var llm=new Fake();var f=prepared(llm);f.compareCurrentVehicles();var j=JsonParser.parseString(response(llm.prompt)).getAsJsonObject();
        j.getAsJsonObject("metricSummary").addProperty("Asking price (CAD)","A: 1 | B: 2");llm.fixed=j.toString();var r=f.compareCurrentVehicles();
        check(r.status().equals("AI_UNAVAILABLE")&&r.comparison().metricSummary().get("Asking price (CAD)").equals("A: 18000 | B: 16000")&&r.comparison().explanation().isEmpty(),"Reject model changes and retain authoritative Java values");
        check(r.message().contains("metricSummary"),"Safe diagnostic identifies structural/evidence problem");
    }
    public static void contradictions(){
        var llm=new Fake();var r=prepared(llm).compareCurrentVehicles();var j=JsonParser.parseString(response(llm.prompt)).getAsJsonObject();
        j.addProperty("explanation","Both vehicles meet all constraints.");rejects(()->new ResponseParser().validateComparison(new ResponseParser().parseComparison(j.toString()),r.evidence()));
    }
    public static void providerFailures(){
        var llm=new Fake();var f=prepared(llm);var a=f.currentVehicle();var b=f.comparisonVehicle();var prefs=f.preferences();var assumptions=f.costAssumptions();
        llm.fail=true;var r=f.compareCurrentVehicles();
        check(r.status().equals("AI_UNAVAILABLE")&&r.comparison().explanation().isEmpty()&&r.comparison().preferredVehicleId().isEmpty(),"Provider failure must never fabricate narrative or winner");
        check(r.evidence().first().cost()!=null&&!ResultFormatter.format(r).contains("fictional-private"),"Keep deterministic results and suppress arbitrary provider details");
        check(f.currentVehicle().equals(a)&&f.comparisonVehicle().equals(b)&&f.preferences().equals(prefs)&&f.costAssumptions().equals(assumptions),"Provider failure preserves all saved context");
        var noKey=facade(new GeminiAdapter("",null));savePair(noKey,true);check(noKey.compareCurrentVehicles().status().equals("AI_UNAVAILABLE"),"Real adapter missing-key path requires no live request");
    }
    public static void toolFailures(){
        for(String tool:List.of("knowledge","constraints","cost")){
            var llm=new Fake();var f=instrumented(llm,new ArrayList<>(),tool);savePair(f,false);
            // Cost assumptions can be saved independently of this deliberately failed tool through a valid session setup.
            if(tool.equals("cost")) {
                var session=new EvaluationSession();session.setCostAssumptions(new CostAssumptions(BigDecimal.ONE,BigDecimal.ONE,null,null,null));
                var tools=new ToolManager();tools.registerTool("cost",input->{throw new IllegalStateException();});tools.registerTool("vehicleKnowledge",new VehicleKnowledgeTool(new VehicleKnowledgeRepository()));tools.registerTool("constraints",new ConstraintCheckerTool());
                f=new UsedCarApplicationFacade(new UsedCarAgent(tools,llm,new DataQualityChecker()),session);savePair(f,false);
            }
            var r=f.compareCurrentVehicles();
            if(tool.equals("constraints"))check(r.status().equals("INSUFFICIENT_INFORMATION")&&llm.calls==0,"Mandatory rule failure must gate preference synthesis");
            else check(r.status().equals("AI_SUCCESS")&&ResultFormatter.format(r).contains("tool failed"),"Optional tool failure remains labeled while other evidence works");
        }
    }
    public static void stale(){
        var ref=new java.util.concurrent.atomic.AtomicReference<UsedCarApplicationFacade>();
        var llm=new Fake(){public String generateStructured(String p,String s){ref.get().savePreferences(UserPreference.limits(BigDecimal.ONE,null));return super.generateStructured(p,s);}};
        var f=prepared(llm);ref.set(f);var r=f.compareCurrentVehicles();check(r.status().equals("STALE")&&r.comparison().explanation().isEmpty(),"Changed context must suppress old narrative and preference");
    }
    public static String entry(Map<String,String> values){var s=new StringBuilder();for(var field:PreparationForm.FIELDS)if(field.section().equals("Vehicle"))s.append(values.getOrDefault(field.key(),"")).append("\n");return s.toString();}
    public static void cli(){
        var llm=new Fake();var f=facade(llm);PreparationForm.command(f,RiskChecks.complete()).execute();var a=f.currentVehicle();var prefs=f.preferences();
        var out=new ByteArrayOutputStream();new UsedCarCLI(f,new Scanner("comparison-set\n"+entry(secondValues())+"yes\ncomparison-show\ncompare\nquit\n"),new PrintStream(out)).run();
        check(out.toString().contains("AI_SUCCESS")&&out.toString().contains("22365")&&llm.calls==1&&f.currentVehicle().equals(a)&&f.preferences().equals(prefs),"Real CLI must save B and invoke shared facade comparison");
        var b=f.comparisonVehicle();new UsedCarCLI(f,new Scanner("comparison-set\n:cancel\nquit\n"),new PrintStream(out)).run();
        check(f.comparisonVehicle().equals(b),"CLI cancellation preserves prior B");
        var invalid=secondValues();invalid.put("price","USD 1");new UsedCarCLI(f,new Scanner("comparison-set\n"+entry(invalid)+"quit\n"),new PrintStream(out)).run();
        check(f.comparisonVehicle().equals(b)&&out.toString().contains("ERROR:"),"Invalid CLI B entry preserves previous records");
    }
    public static void runAll(){storage();metrics();constraints();costs();missingCosts();knowledge();partial();sameVehicle();missingVehicles();invalidVehicle();units();order();prompt();parserValid();malformed();fields();identifiers();changedMetrics();contradictions();providerFailures();toolFailures();stale();cli();ca.yorku.eecs3311.usedcar.llm.ComparisonGeminiChecks.workflow();}
}
