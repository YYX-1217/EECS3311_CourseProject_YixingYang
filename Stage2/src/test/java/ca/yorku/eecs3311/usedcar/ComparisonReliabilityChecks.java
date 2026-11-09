package ca.yorku.eecs3311.usedcar;

import ca.yorku.eecs3311.usedcar.app.*;
import ca.yorku.eecs3311.usedcar.llm.*;
import ca.yorku.eecs3311.usedcar.result.*;
import ca.yorku.eecs3311.usedcar.ui.*;
import com.google.gson.*;
import java.util.*;
import java.io.*;
import java.math.BigDecimal;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

/** Actual production tools and facade; fictional Corolla/Mazda evidence, never live credentials. */
public final class ComparisonReliabilityChecks {
    public static Map<String,String> first() {
        var v=RiskChecks.complete();
        v.putAll(Map.of("year","2018","make","Toyota","model","Corolla","trim","LE","mileage","125000","price","12500","consumption","7.2"));
        v.putAll(Map.of("budget","15000","maxMileage","150000","acceptedTransmissions","AUTOMATIC","preferredBodyType","SEDAN","reliabilityPriority","5","fuelEconomyPriority","3","accidentTolerance","","annualKm","15000","fuelPrice","1.65","taxRate","0.13"));
        v.put("maintenanceAllowance","1000");v.put("insuranceAllowance","1800");v.put("notes","");
        return v;
    }
    public static Map<String,String> second() {
        var v=first();v.putAll(Map.of("year","2017","make","Mazda","model","Mazda3","trim","GS","mileage","110000","price","13500","consumption","7.5"));return v;
    }
    public static void prepare(UsedCarApplicationFacade f) {
        PreparationForm.command(f,first()).execute();f.saveComparisonVehicle(PreparationForm.comparisonVehicle(f,second()));
    }
    public static UsedCarApplicationFacade facade(LLMClient client) {var f=ComparisonChecks.facade(client);prepare(f);return f;}
    private static JsonObject response(ComparisonChecks.Fake fake) {return JsonParser.parseString(ComparisonChecks.response(fake.prompt)).getAsJsonObject();}
    private static void preserved(ComparisonEvaluation r) {
        check(r.comparison().metricSummary().get("First-year estimate (CAD)").equals("A: 18707.00 | B: 19911.25"),"Exact common-scenario Java totals must survive");
    }
    private static void reject(java.util.function.Consumer<JsonObject> mutation) {
        var fake=new ComparisonChecks.Fake();var f=facade(fake);f.compareCurrentVehicles();var json=response(fake);mutation.accept(json);fake.fixed=json.toString();
        var r=f.compareCurrentVehicles();check(r.status().equals("AI_UNAVAILABLE")&&r.comparison().explanation().isEmpty()&&r.comparison().preferredVehicleId().isEmpty(),"Unsupported comparison must be withheld");preserved(r);
        check(!r.message().contains("PRIVATE_SENTINEL"),"Rejected text must not leak into diagnostic");
    }
    public static void scenario() {
        var fake=new ComparisonChecks.Fake();var f=facade(fake);var r=f.compareCurrentVehicles();preserved(r);
        check(r.status().equals("AI_SUCCESS")&&r.comparison().preferredVehicleId().isEmpty(),"Valid comparison must not force a winner");
        check(r.evidence().first().preference().maxBudget().compareTo(new BigDecimal("15000"))==0&&r.evidence().first().preference().reliabilityPriority()==5&&r.evidence().first().preference().fuelEconomyPriority()==3,"Exact supplied buyer profile");
        var m=r.comparison().metricSummary();check(m.get("Asking price (CAD)").equals("A: 12500 | B: 13500")&&m.get("Mileage (km)").equals("A: 125000 | B: 110000"),"Correct ordered numeric evidence");
        check(m.get("Price difference A-B (CAD)").equals("-1000")&&m.get("Mileage difference A-B (km)").equals("15000"),"Signed differences must not reverse");
        check(r.evidence().first().preference().equals(r.evidence().second().preference())&&r.evidence().first().priorityGuidance().equals(r.evidence().second().priorityGuidance()),"Symmetric buyer criteria");
        check(r.comparison().explanation().contains("Vehicle A has lower asking price by 1000 CAD")&&r.comparison().explanation().contains("Vehicle B has lower mileage by 15000 km"),"Exact asymmetric trade-offs");
    }
    public static void reversedIds(){reject(j->{String a=j.get("firstVehicleId").getAsString();j.addProperty("firstVehicleId",j.get("secondVehicleId").getAsString());j.addProperty("secondVehicleId",a);});}
    public static void priceTampering(){reject(j->j.getAsJsonObject("metricSummary").addProperty("Asking price (CAD)","A: 13500 | B: 12500"));}
    public static void mileageTampering(){reject(j->j.getAsJsonObject("metricSummary").addProperty("Mileage (km)","A: 110000 | B: 125000"));}
    public static void costTampering(){reject(j->j.getAsJsonObject("metricSummary").addProperty("First-year estimate (CAD)","A: 1 | B: 2"));}
    private static void append(JsonObject j,String claim){j.addProperty("explanation",j.get("explanation").getAsString()+"\n"+claim);}
    public static void inventedNumericProse(){reject(j->append(j,"Vehicle A costs CAD 1 and has 1 km. PRIVATE_SENTINEL"));}
    public static void reversedPriceProse(){reject(j->append(j,"Vehicle B is cheaper than A in asking price."));}
    public static void inventedKnowledge(){reject(j->append(j,"Both local records were independently verified."));var r=facade(new ComparisonChecks.Fake()).compareCurrentVehicles();check(r.comparison().explanation().contains("Vehicle A: local knowledge unavailable")&&r.comparison().explanation().contains("Vehicle B: local knowledge unavailable"),"Real CSV no-match stays unavailable for both");}
    public static void inventedVerification(){reject(j->append(j,"The seller history was independently verified; both vehicles are guaranteed safe."));}
    public static void unknownHistory(){
        var fake=new ComparisonChecks.Fake();var f=facade(fake);var b=second();b.put("maintenance","UNKNOWN");b.put("accident","UNKNOWN");f.saveComparisonVehicle(PreparationForm.comparisonVehicle(f,b));var r=f.compareCurrentVehicles();
        check(r.status().equals("INSUFFICIENT_INFORMATION")&&fake.calls==0&&r.comparison().preferredVehicleId().isEmpty()&&r.comparison().metricSummary().get("Accident history (claims)").contains("B: UNKNOWN"),"Unknown history cannot become confirmed or trigger synthesis");
    }
    private static void prefer(JsonObject j,String id) {j.addProperty("preferredVehicleId",id);j.addProperty("explanation",j.get("explanation").getAsString().replace(ComparisonNarrative.NO_PREFERENCE,ComparisonNarrative.LIMITED_PREFERENCE));}
    public static void preferredConflict(){
        var fake=new ComparisonChecks.Fake();var f=facade(fake);f.savePreferences(ca.yorku.eecs3311.usedcar.domain.UserPreference.limits(new BigDecimal("12000"),150000));f.compareCurrentVehicles();var j=response(fake);prefer(j,f.currentVehicle().orElseThrow().id());fake.fixed=j.toString();
        var r=f.compareCurrentVehicles();check(r.status().equals("AI_UNAVAILABLE"),"A measurable advantage cannot override an actual budget violation");preserved(r);
    }
    public static void unsupportedPreference(){reject(j->{prefer(j,j.get("firstVehicleId").getAsString());j.addProperty("explanation",String.join("\n",j.get("explanation").getAsString().lines().filter(x->!x.startsWith("Vehicle A has lower")).toList()));});}
    public static void supportedPreference(){
        var fake=new ComparisonChecks.Fake();var f=facade(fake);f.compareCurrentVehicles();var j=response(fake);prefer(j,f.currentVehicle().orElseThrow().id());fake.fixed=j.toString();var r=f.compareCurrentVehicles();
        check(r.status().equals("AI_SUCCESS")&&r.comparison().preferredVehicleId().equals(f.currentVehicle().orElseThrow().id())&&r.comparison().explanation().contains("not a verified reliability"),"Supported limited preference remains possible, not forced");preserved(r);
    }
    public static void reorderedStatements(){var fake=new ComparisonChecks.Fake();var f=facade(fake);f.compareCurrentVehicles();var j=response(fake);var lines=new ArrayList<>(j.get("explanation").getAsString().lines().toList());Collections.reverse(lines);j.addProperty("explanation",String.join("\n",lines));fake.fixed=j.toString();check(f.compareCurrentVehicles().status().equals("AI_SUCCESS"),"Model may order valid grounded trade-offs");}
    public static void omittedLimit(){reject(j->j.addProperty("explanation",String.join("\n",j.get("explanation").getAsString().lines().filter(x->!x.startsWith("Vehicle A: local knowledge")).toList())));}
    public static void duplicateStatement(){reject(j->append(j,ComparisonNarrative.NO_PREFERENCE));}
    public static void missingCost(){var fake=new ComparisonChecks.Fake();var f=facade(fake);var b=second();b.put("consumption","");f.saveComparisonVehicle(PreparationForm.comparisonVehicle(f,b));var r=f.compareCurrentVehicles();check(r.status().equals("AI_SUCCESS")&&r.comparison().explanation().contains("B: UNAVAILABLE")&&!r.comparison().explanation().contains("lower first-year estimate"),"Missing cost cannot be used as a numerical advantage");}
    public static void cli(){var f=facade(new ComparisonChecks.Fake());var out=new ByteArrayOutputStream();new UsedCarCLI(f,new Scanner("compare\nquit\n"),new PrintStream(out)).run();var text=out.toString();check(text.contains("AI_SUCCESS")&&text.contains("18707.00")&&text.contains("19911.25")&&text.contains("Vehicle B has lower mileage")&&text.contains("local knowledge unavailable"),"Actual CLI renders validated trade-offs and Java evidence");}
    public static void runAll(){scenario();reversedIds();priceTampering();mileageTampering();costTampering();inventedNumericProse();reversedPriceProse();inventedKnowledge();inventedVerification();unknownHistory();preferredConflict();unsupportedPreference();supportedPreference();reorderedStatements();omittedLimit();duplicateStatement();missingCost();cli();}
}
