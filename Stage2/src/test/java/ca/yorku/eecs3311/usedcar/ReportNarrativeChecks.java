package ca.yorku.eecs3311.usedcar;
import ca.yorku.eecs3311.usedcar.llm.*;
import ca.yorku.eecs3311.usedcar.ui.*;
import ca.yorku.eecs3311.usedcar.result.*;
import com.google.gson.*;
import java.util.*;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;
/** FIN-01: fictional provider responses; never sends network requests. */
public final class ReportNarrativeChecks {
    private static ReportEvaluation result(String accident,String maintenance,String text){
        var llm=new ReportChecks.Fake(){@Override public String generateStructured(String p,String schema){
            reportCalls++;var json=JsonParser.parseString(ReportChecks.response(p)).getAsJsonObject();json.addProperty("explanation",text);return json.toString();}};
        var f=ReportChecks.saved(llm);var values=ReportChecks.corolla();values.put("accident",accident);values.put("maintenance",maintenance);f.saveVehicle(PreparationForm.vehicle(f,values));
        return f.generateCurrentReport();
    }
    private static void rejected(String accident,String maintenance,String text){
        var r=result(accident,maintenance,text);check(r.status().equals("AI_UNAVAILABLE")&&r.report()==null,"Contradictory narrative must not receive AI_SUCCESS: "+text);
        check(r.message().contains("REPORT_EVIDENCE")&&r.evidence().deterministic().cost().firstYearEstimate().intValue()==18707,"Safe diagnostic and authoritative evidence survive");
        check(!ResultFormatter.format(r).contains(text),"Rejected prose must not be displayed/exported");
    }
    private static void accepted(String accident,String maintenance,String text){check(result(accident,maintenance,text).status().equals("AI_SUCCESS"),"Valid qualified guidance remains accepted: "+text);}
    public static void auditAccident(){rejected("NO","DOCUMENTED","The seller reports prior accidents.");}
    public static void auditMaintenance(){rejected("NO","DOCUMENTED","The saved maintenance history is undocumented.");}
    public static void observedSummary(){rejected("NO","DOCUMENTED","Weigh the stated operating cost estimates against the risks of undocumented service history.");}
    public static void accidentYes(){for(String s:List.of("The seller reports no accidents.","The vehicle has never been in an accident.","No accident history is reported."))rejected("YES","DOCUMENTED",s);}
    public static void accidentNo(){for(String s:List.of("The vehicle has a history of accidents.","Prior accidents are reported.","The car was involved in a collision."))rejected("NO","DOCUMENTED",s);}
    public static void documented(){for(String s:List.of("Maintenance records are missing.","The seller has no service records.","The maintenance history is unknown.","Only partial maintenance records are available."))rejected("NO","DOCUMENTED",s);}
    public static void none(){for(String s:List.of("The seller claims maintenance records are documented.","Full maintenance records are available."))rejected("NO","NONE",s);}
    public static void partial(){accepted("NO","PARTIAL","The saved maintenance records are partial; review the remaining gaps with an independent inspector.");rejected("NO","PARTIAL","Full maintenance records are available.");rejected("NO","PARTIAL","No maintenance records are available.");}
    public static void qualified(){accepted("NO","DOCUMENTED","The seller claims maintenance records are documented, but they have not been independently verified.");accepted("NO","DOCUMENTED","The seller claims there were no accidents; independent verification is still recommended.");accepted("YES","DOCUMENTED","The seller reports prior accidents; review the supporting records with an independent inspector.");}
    public static void advice(){for(String s:List.of("Inspect accident and maintenance records before deciding.","Ask whether any prior accidents occurred and verify the seller's claims.","If maintenance records are missing, request them before purchase.","Verify whether the maintenance history is undocumented.","Do not assume complete service history."))accepted("NO","DOCUMENTED",s);}
    public static void unknown(){
        // Production sufficiency gate intentionally prevents any recommendation request for UNKNOWN.
        for(String field:List.of("accident","maintenance")){
            var llm=new ReportChecks.Fake();var f=ReportChecks.saved(llm);var values=ReportChecks.corolla();values.put(field,"UNKNOWN");f.saveVehicle(PreparationForm.vehicle(f,values));
            var r=f.generateCurrentReport();check(r.status().equals("INSUFFICIENT_INFORMATION")&&llm.reportCalls==0,"Unknown must not gain model-invented certainty");
        }
    }
    public static void unknownAssertions(){
        var f=ReportChecks.saved(new ReportChecks.Fake());var values=ReportChecks.corolla();values.put("accident","UNKNOWN");values.put("maintenance","UNKNOWN");
        var vehicle=PreparationForm.vehicle(f,values);
        ReportNarrativeValidator.validate("Accident history is unknown; maintenance history is unknown. Inspect the available evidence before deciding.",vehicle);
        for(String text:List.of("The seller reports prior accidents.","The seller reports no accidents.","Maintenance records are documented.","The maintenance history is undocumented.")){
            try{ReportNarrativeValidator.validate(text,vehicle);throw new AssertionError("UNKNOWN gained invented certainty: "+text);}catch(IllegalArgumentException expected){}
        }
    }
    public static void sections(){
        var f=ReportChecks.saved(new ReportChecks.Fake());var r=f.generateCurrentReport();var parser=new ResponseParser();
        for(String field:List.of("strengths","risks","missingInformation","constraintConflicts","inspectionPriorities","nextSteps")){
            var j=new Gson().toJsonTree(r.report()).getAsJsonObject();var a=new JsonArray();a.add("The seller reports prior accidents.");j.add(field,a);
            try{parser.validateReport(parser.parseReport(j.toString()),r.evidence());throw new AssertionError("Unsupported narrative accepted in "+field);}catch(IllegalArgumentException expected){}
        }
    }
    public static void numerical(){for(String s:List.of("Fuel consumption is 3.2 L/100 km.","Mileage is 100 km.","Asking price is CAD 100.","Annual fuel costs are CAD 1.","The vehicle is six years old."))rejected("NO","DOCUMENTED",s);ReportChecks.conflicts();}
    public static void stateExport(){
        var llm=new ReportChecks.Fake();var f=ReportChecks.saved(llm);var old=f.generateCurrentReport();
        var j=new Gson().toJsonTree(old.report()).getAsJsonObject();j.addProperty("explanation","The saved maintenance history is undocumented.");llm.fixed=j.toString();var r=f.generateCurrentReport();
        check(r.status().equals("AI_UNAVAILABLE")&&f.currentReport().orElseThrow()==r&&llm.reportCalls==2,"Rejected regeneration replaces old recommendation without automatic retry");
        try{var path=java.nio.file.Files.createTempDirectory("report-narrative-check-").resolve("report.txt");f.exportReport(r.generationId(),path);check(java.nio.file.Files.readString(path).equals(ResultFormatter.format(r))&&!java.nio.file.Files.readString(path).contains("undocumented"),"UTF-8 exports only safe failed result");}catch(java.io.IOException e){throw new AssertionError(e);}
    }
    public static void profileText(){
        String text=ResultFormatter.format(ReportChecks.saved(new ReportChecks.Fake()).preferences());check(!text.contains("remains planned")&&text.contains("not checked as an F07 body-type constraint")&&text.contains("Strategy guidance"),"F03 text reflects actual priority use without promising body-type matching");
    }
    public static void runAll(){auditAccident();auditMaintenance();observedSummary();accidentYes();accidentNo();documented();none();partial();qualified();advice();unknown();unknownAssertions();sections();numerical();stateExport();profileText();}
}