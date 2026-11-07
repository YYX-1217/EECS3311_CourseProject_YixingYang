package ca.yorku.eecs3311.usedcar;

import ca.yorku.eecs3311.usedcar.app.*;
import ca.yorku.eecs3311.usedcar.agent.UsedCarAgent;
import ca.yorku.eecs3311.usedcar.domain.*;
import ca.yorku.eecs3311.usedcar.llm.*;
import ca.yorku.eecs3311.usedcar.result.*;
import ca.yorku.eecs3311.usedcar.tool.*;
import ca.yorku.eecs3311.usedcar.ui.*;
import com.google.gson.*;
import java.util.*;
import java.io.*;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

public final class ChecklistChecks {
    public static class Fake implements LLMClient {
        public int calls;public String prompt,schema,fixed;public boolean fail;
        public String status(){return "OFFLINE FIXTURE";}
        public String generate(String p){throw new AssertionError("F10 requires structured generation");}
        public String generateStructured(String p,String s){calls++;prompt=p;schema=s;if(fail)throw new IllegalStateException("fictional-private-error");return fixed==null?response(p):fixed;}
    }
    public static String response(String prompt){
        String prefix="Required checklist: ";String line=prompt.lines().filter(x->x.startsWith(prefix)).findFirst().orElseThrow();
        var root=JsonParser.parseString(line.substring(prefix.length())).getAsJsonObject();
        root.getAsJsonObject("sections").entrySet().forEach(section->{for(var item:section.getValue().getAsJsonArray())item.getAsJsonObject().addProperty("reason","Review the supplied evidence and unresolved questions before proceeding; this is advisory inspection planning.");});
        return root.toString();
    }
    public static UsedCarApplicationFacade facade(LLMClient llm){return RiskChecks.facade(llm);}
    public static UsedCarApplicationFacade saved(Fake llm){var f=facade(llm);RiskChecks.save(f);return f;}
    private static void rejects(Runnable action){try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("Expected rejection");}
    private static InspectionChecklistItem item(ChecklistEvaluation r,String id){return r.checklist().items().stream().filter(i->i.id().equals(id)).findFirst().orElseThrow();}
    public static void valid(){
        var llm=new Fake();var f=saved(llm);var r=f.generateCurrentChecklist();
        check(r.status().equals("AI_SUCCESS")&&r.checklist().sections().size()==6&&llm.calls==1,"Valid F10 must produce six sections via one structured request");
        check(new ResponseParser().parseChecklist(response(llm.prompt)).equals(r.checklist()),"Parse actual structured item response");
        check(llm.prompt.contains("untrusted")&&llm.prompt.contains("instructional")&&llm.schema.contains("priority"),"Evidence trust boundaries and item schema must reach LLM");
        try{r.checklist().sections().clear();throw new AssertionError("Map must be immutable");}catch(UnsupportedOperationException expected){}
        try{r.checklist().sections().values().iterator().next().clear();throw new AssertionError("Items must be immutable");}catch(UnsupportedOperationException expected){}
    }
    public static void missingHistory(){
        var llm=new Fake();llm.fail=true;var f=facade(llm);f.saveVehicle(PreparationForm.vehicle(f,PreparationForm.example()));var r=f.generateCurrentChecklist();
        check(item(r,"maintenance-records").priority().equals("High")&&item(r,"maintenance-records").reason().contains("unknown"),"Unknown maintenance requires records, not a complete-history claim");
        check(item(r,"accident-disclosures").priority().equals("High")&&item(r,"accident-disclosures").reason().contains("unknown"),"Unknown accidents require disclosures and evidence");
        check(r.vehicle().accidentHistory().isEmpty()&&r.vehicle().maintenanceHistory().isEmpty(),"Checklist must not fill in history");
    }
    public static void knowledge(){
        var r=saved(new Fake()).generateCurrentChecklist();
        check(item(r,"local-note-1").action().contains("Review the supplied service log")&&item(r,"local-note-1").source().contains("Instructional sample"),"Actual local inspection note and attribution must be preserved");
        check(r.knowledgeStatus().contains("not independently verified"),"Teaching notes must not be represented as reliability research");
    }
    public static void noKnowledge(){
        var f=saved(new Fake());var v=RiskChecks.complete();v.put("model","No matching model");f.saveVehicle(PreparationForm.vehicle(f,v));var r=f.generateCurrentChecklist();
        check(r.status().equals("AI_SUCCESS")&&r.knowledgeStatus().contains("general inspection guidance only")&&r.checklist().items().stream().noneMatch(i->i.id().startsWith("local-note")),"Missing knowledge must retain usable generic guidance without fabricated model-specific notes");
    }
    public static void transmissions(){
        var f=saved(new Fake());
        for(var entry:Map.of("AUTOMATIC","automatic engagement","MANUAL","clutch","CVT","CVT engagement","","Confirm the transmission").entrySet()){
            var v=RiskChecks.complete();v.put("transmission",entry.getKey());f.saveVehicle(PreparationForm.vehicle(f,v));
            check(item(f.generateCurrentChecklist(),"transmission-check").action().contains(entry.getValue()),"Guidance must follow actual known transmission");
        }
    }
    public static void malformed(){
        var llm=new Fake();var f=saved(llm);
        for(String text:List.of("","broken","{}","null","[]","```json\n{}\n```","{\"sections\":{},\"sections\":{}}")){
            llm.fixed=text;var r=f.generateCurrentChecklist();check(r.status().equals("FALLBACK")&&r.checklist().sections().size()==6,"Malformed/empty AI must yield practical six-section fallback");
        }
    }
    public static void fields(){
        var llm=new Fake();var f=saved(llm);f.generateCurrentChecklist();String valid=response(llm.prompt);
        for(String mutation:List.of("section","empty","extra","priority","type","duplicate")){
            var j=JsonParser.parseString(valid).getAsJsonObject();var sections=j.getAsJsonObject("sections");var items=sections.getAsJsonArray(InspectionChecklist.SECTIONS.get(0));var first=items.get(0).getAsJsonObject();
            switch(mutation){
                case "section"->sections.remove(InspectionChecklist.SECTIONS.get(1));
                case "empty"->sections.add(InspectionChecklist.SECTIONS.get(1),new JsonArray());
                case "extra"->first.addProperty("completed",true);
                case "priority"->first.addProperty("priority","Urgent");
                case "type"->first.addProperty("reason",false);
                case "duplicate"->items.get(1).getAsJsonObject().addProperty("id",first.get("id").getAsString());
            }
            rejects(()->new ResponseParser().parseChecklist(j.toString()));
        }
    }
    public static void grounding(){
        var llm=new Fake();var f=saved(llm);f.generateCurrentChecklist();String valid=response(llm.prompt);
        for(String field:List.of("action","source","priority","id")){
            var j=JsonParser.parseString(valid).getAsJsonObject();var i=j.getAsJsonObject("sections").getAsJsonArray(InspectionChecklist.SECTIONS.get(0)).get(0).getAsJsonObject();
            i.addProperty(field,field.equals("priority")?"Low":field.equals("id")?"invented-id":"Fabricated content");llm.fixed=j.toString();
            check(f.generateCurrentChecklist().status().equals("FALLBACK"),"Reject changed grounded action/source/ID or downgraded priority");
        }
    }
    public static void claims(){
        var llm=new Fake();var f=facade(llm);f.saveVehicle(PreparationForm.vehicle(f,PreparationForm.example()));f.generateCurrentChecklist();String valid=response(llm.prompt);
        for(String claim:List.of("This vehicle is accident-free.","It has complete service history.","It is mechanically sound.","This model is defective.")){
            var j=JsonParser.parseString(valid).getAsJsonObject();j.getAsJsonObject("sections").getAsJsonArray(InspectionChecklist.SECTIONS.get(0)).get(0).getAsJsonObject().addProperty("reason",claim);llm.fixed=j.toString();
            var r=f.generateCurrentChecklist();check(r.status().equals("FALLBACK")&&!ResultFormatter.format(r).contains(claim),"Unsupported history/condition claims must not reach displayed model output");
        }
    }
    public static void providerFailure(){
        var llm=new Fake();llm.fail=true;var f=saved(llm);var v=f.currentVehicle();var r=f.generateCurrentChecklist();
        check(r.status().equals("FALLBACK")&&r.checklist().items().size()>=9&&f.currentVehicle().equals(v)&&!ResultFormatter.format(r).contains("fictional-private-error"),"Provider failure retains useful guidance and protects session/private diagnostics");
        var missing=facade(new GeminiAdapter("",null));RiskChecks.save(missing);check(missing.generateCurrentChecklist().status().equals("FALLBACK"),"Real adapter missing-key path must return fallback without network");
    }
    public static void missingVehicle(){var llm=new Fake();var f=facade(llm);rejects(f::generateCurrentChecklist);check(llm.calls==0&&f.currentChecklist().isEmpty(),"No vehicle must not create sample or request AI");}
    public static void completion(){
        var llm=new Fake();var f=saved(llm);var r=f.generateCurrentChecklist();var updated=f.completeChecklistItem(r.generationId(),"documents",true);
        check(updated.completed().contains("documents")&&r.completed().isEmpty(),"Completion must create independent immutable snapshot");
        f.completeChecklistItem(r.generationId(),"documents",false);check(f.currentChecklist().orElseThrow().completed().isEmpty()&&llm.calls==1,"Unmark/show must not call LLM");
        rejects(()->f.completeChecklistItem(r.generationId(),"unknown-id",true));
    }
    public static void isolation(){
        var llm=new Fake();var a=saved(llm);var b=saved(llm);var ar=a.generateCurrentChecklist();var br=b.generateCurrentChecklist();a.completeChecklistItem(ar.generationId(),"documents",true);
        check(b.currentChecklist().orElseThrow().completed().isEmpty()&&!ar.generationId().equals(br.generationId()),"Sessions must not share completion");
        rejects(()->b.completeChecklistItem(ar.generationId(),"documents",true));
    }
    public static void invalidation(){
        var f=saved(new Fake());var r=f.generateCurrentChecklist();var v=RiskChecks.complete();v.put("mileage","12345");f.saveVehicle(PreparationForm.vehicle(f,v));
        check(f.currentChecklist().isEmpty(),"Changed vehicle must invalidate checklist even when ID stays the same");rejects(()->f.completeChecklistItem(r.generationId(),"documents",true));
    }
    public static void regeneration(){
        var llm=new Fake();var f=saved(llm);var old=f.generateCurrentChecklist();f.completeChecklistItem(old.generationId(),"documents",true);var next=f.generateCurrentChecklist();
        check(next.completed().isEmpty()&&!next.generationId().equals(old.generationId())&&llm.calls==2,"Explicit regeneration resets completion with new generation identity");
        rejects(()->f.completeChecklistItem(old.generationId(),"documents",true));
    }
    public static void tools(){
        var toolOrder=new ArrayList<String>();var knowledge=new VehicleKnowledgeTool(new VehicleKnowledgeRepository());var tools=new ToolManager();
        tools.registerTool("vehicleKnowledge",input->{toolOrder.add("knowledge");return knowledge.execute(input);});
        var llm=new Fake(){public String generateStructured(String p,String s){check(toolOrder.equals(List.of("knowledge"))&&p.contains("missingFields"),"F04 and actual knowledge retrieval must precede LLM");return super.generateStructured(p,s);}};
        var f=new UsedCarApplicationFacade(new UsedCarAgent(tools,llm,new DataQualityChecker()),new EvaluationSession());RiskChecks.save(f);check(f.generateCurrentChecklist().status().equals("AI_SUCCESS"),"F10 needs existing knowledge tool, not unrelated risk/cost operations");
        var failedTools=new ToolManager();failedTools.registerTool("vehicleKnowledge",input->{throw new IllegalStateException("private");});
        var fallback=new UsedCarApplicationFacade(new UsedCarAgent(failedTools,new Fake(),new DataQualityChecker()),new EvaluationSession());RiskChecks.save(fallback);
        check(fallback.generateCurrentChecklist().knowledgeStatus().contains("tool failed"),"Knowledge failure must be labeled and allow general checklist");
    }
    public static void stale(){
        var ref=new java.util.concurrent.atomic.AtomicReference<UsedCarApplicationFacade>();
        var llm=new Fake(){public String generateStructured(String p,String s){var v=RiskChecks.complete();v.put("price","19000");ref.get().saveVehicle(PreparationForm.vehicle(ref.get(),v));return super.generateStructured(p,s);}};
        var f=saved(llm);ref.set(f);rejects(f::generateCurrentChecklist);check(f.currentChecklist().isEmpty(),"Delayed result must not be assigned to changed vehicle");
    }
    public static void cli(){
        var llm=new Fake();var f=saved(llm);var out=new ByteArrayOutputStream();
        new UsedCarCLI(f,new Scanner("checklist\nchecklist-mark documents\nchecklist-show\nchecklist-unmark documents\nchecklist-show\nquit\n"),new PrintStream(out)).run();
        check(llm.calls==1&&out.toString().contains("[x] documents")&&out.toString().contains("AI_SUCCESS")&&f.currentChecklist().orElseThrow().completed().isEmpty(),"CLI must share generation/cache/completion without repeated AI calls");
    }
    public static void runAll(){valid();missingHistory();knowledge();noKnowledge();transmissions();malformed();fields();grounding();claims();providerFailure();missingVehicle();completion();isolation();invalidation();regeneration();tools();stale();cli();}
}
