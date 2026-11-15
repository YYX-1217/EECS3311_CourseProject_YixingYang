package ca.yorku.eecs3311.usedcar.llm;

import ca.yorku.eecs3311.usedcar.*;
import ca.yorku.eecs3311.usedcar.result.*;
import ca.yorku.eecs3311.usedcar.ui.*;
import com.google.gson.*;
import java.io.*;
import java.net.http.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

/** AUD-04: actual adapter request/envelope/parser path, injected transport, fictional credentials only.
 * Contract assertions follow https://ai.google.dev/api/generate-content#GenerationConfig and
 * https://ai.google.dev/gemini-api/docs/structured-output; they do not emulate provider acceptance.
 */
public final class ChecklistGeminiChecks {
    private static final String KEY="fictional-aud04-credential",PRIVATE="fictional-private-error";
    static String body(HttpRequest request)throws IOException,InterruptedException{
        var bytes=new ByteArrayOutputStream();var done=new CountDownLatch(1);var failed=new AtomicBoolean();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>(){
            public void onSubscribe(Flow.Subscription s){s.request(Long.MAX_VALUE);}
            public void onNext(ByteBuffer b){byte[] data=new byte[b.remaining()];b.get(data);bytes.writeBytes(data);}
            public void onError(Throwable e){failed.set(true);done.countDown();}public void onComplete(){done.countDown();}
        });
        if(!done.await(2,TimeUnit.SECONDS)||failed.get())throw new IOException("Fixture serialization failed");
        return bytes.toString(StandardCharsets.UTF_8);
    }
    static String prompt(HttpRequest request)throws IOException,InterruptedException{
        return JsonParser.parseString(body(request)).getAsJsonObject().getAsJsonArray("contents").get(0).getAsJsonObject().getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString();
    }
    static String envelope(String text){return new Gson().toJson(Map.of("candidates",List.of(Map.of("finishReason","STOP","content",Map.of("parts",List.of(Map.of("text",text)))))));}
    private static void contract(JsonObject schema){
        String type=schema.get("type").getAsString();
        switch(type){
            case "object"->{
                check(schema.keySet().equals(Set.of("type","properties","required","additionalProperties"))&&!schema.get("additionalProperties").getAsBoolean(),"Closed object uses documented keywords");
                var fields=schema.getAsJsonObject("properties");var required=new HashSet<String>();schema.getAsJsonArray("required").forEach(v->required.add(v.getAsString()));
                check(required.equals(fields.keySet())&&required.size()==schema.getAsJsonArray("required").size(),"Required fields exist exactly once");fields.entrySet().forEach(e->contract(e.getValue().getAsJsonObject()));
            }
            case "array"->{check(schema.keySet().equals(Set.of("type","items","minItems"))&&schema.get("minItems").getAsInt()==1&&!schema.has("maxItems"),"Keep nonempty arrays without unrolling forty item positions in the provider grammar");contract(schema.getAsJsonObject("items"));}
            case "string"->{check(schema.keySet().equals(Set.of("type"))||schema.keySet().equals(Set.of("type","enum")),"Only documented string keywords");if(schema.has("enum"))check(schema.getAsJsonArray("enum").equals(new Gson().toJsonTree(List.of("High","Medium","Low"))),"Three string priorities preserved");}
            default->throw new AssertionError("Unexpected schema type");
        }
    }
    public static void schemaSerialization(){
        var schema=JsonParser.parseString(ChecklistSchema.json()).getAsJsonObject();contract(schema);
        check(JsonParser.parseString(schema.toString()).equals(schema),"Schema round-trip");
        var sections=schema.getAsJsonObject("properties").getAsJsonObject("sections").getAsJsonObject("properties");
        check(sections.keySet().equals(new HashSet<>(InspectionChecklist.SECTIONS)),"All six original sections");
        for(var section:sections.entrySet())check(section.getValue().getAsJsonObject().getAsJsonObject("items").getAsJsonObject("properties").keySet().equals(Set.of("id","action","priority","reason","source")),"Every item retains five required fields");
    }
    public static void workflow(){
        var requests=new ArrayList<JsonObject>();var endpoints=new ArrayList<java.net.URI>();
        var adapter=new GeminiAdapter(KEY,null,request->{
            String raw=body(request);var json=JsonParser.parseString(raw).getAsJsonObject();requests.add(json);endpoints.add(request.uri());
            check(!raw.contains(KEY)&&request.uri().getQuery()==null&&request.method().equals("POST"),"Header-only credential; POST without query");
            check(request.timeout().orElseThrow().getSeconds()==30&&request.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"),"Keep configured request timeout/content type");
            String p=prompt(request);if(p.startsWith("Create an advisory"))System.out.println("AUD-04 fictional F10 payload: prompt_utf8_bytes="+p.getBytes(StandardCharsets.UTF_8).length+", schema_utf8_bytes="+ChecklistSchema.json().getBytes(StandardCharsets.UTF_8).length+", request_utf8_bytes="+raw.getBytes(StandardCharsets.UTF_8).length);return new GeminiAdapter.Reply(200,envelope(p.startsWith("Create an advisory")?ChecklistChecks.response(p):"{}"));
        });
        adapter.generateStructured("fictional F08 contract",RiskAssessmentSchema.json());
        adapter.generateStructured("fictional F11 contract",ClarificationSchema.json(List.of(ca.yorku.eecs3311.usedcar.domain.ClarificationField.ACCIDENT)));
        var f=ReportChecks.saved(adapter);var result=f.generateCurrentChecklist();
        check(result.status().equals("AI_SUCCESS")&&f.currentChecklist().orElseThrow()==result&&requests.size()==3,"Valid F10 crosses actual adapter, parser, evidence validator and cache");
        check(new HashSet<>(endpoints).size()==1&&endpoints.get(0).toString().equals("https://generativelanguage.googleapis.com/v1beta/models/"+GeminiAdapter.DEFAULT_MODEL+":generateContent"),"Same default model/endpoint as F08/F11");
        for(var json:requests){var config=json.getAsJsonObject("generationConfig");
            check(json.keySet().equals(Set.of("contents","generationConfig"))&&config.keySet().equals(Set.of("maxOutputTokens","responseMimeType","responseJsonSchema")),"Single schema representation; no conflicting responseSchema/responseFormat");
            check(config.get("responseMimeType").getAsString().equals("application/json")&&config.get("responseJsonSchema").isJsonObject()&&config.get("maxOutputTokens").getAsInt()==4096,"Schema object, JSON MIME, existing output limit");
            var contents=json.getAsJsonArray("contents");check(contents.size()==1&&contents.get(0).getAsJsonObject().get("role").getAsString().equals("user")&&contents.get(0).getAsJsonObject().getAsJsonArray("parts").size()==1,"One user content/part");
        }
        contract(requests.get(2).getAsJsonObject("generationConfig").getAsJsonObject("responseJsonSchema"));
    }
    public static void metricReasons(){
        var llm=new ChecklistChecks.Fake();var f=ChecklistChecks.saved(llm);f.generateCurrentChecklist();
        String valid=ChecklistChecks.response(llm.prompt);
        for(String reason:List.of("Verify the claimed maintenance history with physical documentation for the 125,000-mile vehicle.","Evaluate the current physical condition of the exterior at 125,000 miles.","Check the stated 30 mpg estimate.","Review the USD 12500 price.")){
            var json=JsonParser.parseString(valid).getAsJsonObject();json.getAsJsonObject("sections").getAsJsonArray(InspectionChecklist.SECTIONS.get(0)).get(0).getAsJsonObject().addProperty("reason",reason);llm.fixed=json.toString();
            var result=f.generateCurrentChecklist();check(result.status().equals("FALLBACK")&&!ResultFormatter.format(result).contains(reason),"Fixed metric/CAD evidence must not be silently relabeled");
        }
        var json=JsonParser.parseString(valid).getAsJsonObject();json.getAsJsonObject("sections").getAsJsonArray(InspectionChecklist.SECTIONS.get(0)).get(0).getAsJsonObject().addProperty("reason","Review the supplied mileage in km and maintenance records before purchase.");llm.fixed=json.toString();
        check(f.generateCurrentChecklist().status().equals("AI_SUCCESS"),"Legitimate metric inspection guidance remains available");
    }
    public static void javaItemBound(){
        var llm=new ChecklistChecks.Fake();var f=ChecklistChecks.saved(llm);f.generateCurrentChecklist();
        var json=JsonParser.parseString(ChecklistChecks.response(llm.prompt)).getAsJsonObject();
        var items=json.getAsJsonObject("sections").getAsJsonArray(InspectionChecklist.SECTIONS.get(0));
        var template=items.get(0).getAsJsonObject().deepCopy();
        while(items.size()<40){var item=template.deepCopy();item.addProperty("id","fictional-bound-"+items.size());items.add(item);}
        var parser=new ResponseParser();check(parser.parseChecklist(json.toString()).sections().get(InspectionChecklist.SECTIONS.get(0)).size()==40,"Java parser accepts the documented forty-item boundary");
        var extra=template.deepCopy();extra.addProperty("id","fictional-bound-40");items.add(extra);
        try{parser.parseChecklist(json.toString());throw new AssertionError("Provider schema simplification must not remove the Java item bound");}catch(IllegalArgumentException expected){}
        llm.fixed=json.toString();check(f.generateCurrentChecklist().status().equals("FALLBACK"),"Oversized real-shaped output preserves deterministic fallback");
    }
    private static ChecklistEvaluation failure(GeminiAdapter.Transport transport,String diagnostic){
        var calls=new AtomicInteger();var adapter=new GeminiAdapter(KEY,null,r->{calls.incrementAndGet();return transport.send(r);});
        var f=ReportChecks.saved(adapter);var vehicle=f.currentVehicle();var prefs=f.preferences();var r=f.generateCurrentChecklist();String text=ResultFormatter.format(r);
        check(r.status().equals("FALLBACK")&&r.checklist().sections().size()==6&&!text.contains("AI_SUCCESS"),"Failure cannot fabricate AI success");
        check(r.message().contains(diagnostic),"F10 must retain sanitized provider classification "+diagnostic);
        check(!text.contains(KEY)&&!text.contains(PRIVATE)&&f.currentVehicle().equals(vehicle)&&f.preferences().equals(prefs),"No credentials/raw errors or session mutations");
        var expected=new ca.yorku.eecs3311.usedcar.tool.ChecklistPlanner().build(vehicle.orElseThrow(),f.retrieveKnowledge(),r.missing());
        check(r.checklist().equals(expected)&&f.currentChecklist().orElseThrow()==r&&calls.get()==1,"Exact deterministic fallback cached after one request");
        return r;
    }
    private static void http(int code){failure(r->new GeminiAdapter.Reply(code,PRIVATE+KEY),"[PROVIDER_HTTP_"+code+"]");}
    public static void badRequest(){http(400);}
    public static void quota(){http(429);}
    public static void server(){http(500);http(502);http(503);http(504);}
    public static void timeout(){failure(r->{throw new HttpTimeoutException(PRIVATE+KEY);},"[TIMEOUT]");}
    private static void invalid(java.util.function.Consumer<JsonObject> mutate){
        var result=failure(request->{var json=JsonParser.parseString(ChecklistChecks.response(prompt(request))).getAsJsonObject();mutate.accept(json);return new GeminiAdapter.Reply(200,envelope(json.toString()));},"structure/evidence");
        check(!result.message().contains("PROVIDER_HTTP"),"Successful HTTP with invalid output differs from provider rejection");
    }
    private static JsonObject first(JsonObject j){return j.getAsJsonObject("sections").getAsJsonArray(InspectionChecklist.SECTIONS.get(0)).get(0).getAsJsonObject();}
    public static void missingFields(){for(String key:List.of("id","action","priority","reason","source"))invalid(j->first(j).remove(key));invalid(j->j.getAsJsonObject("sections").remove(InspectionChecklist.SECTIONS.get(0)));}
    public static void wrongTypes(){for(String key:List.of("id","action","priority","reason","source"))invalid(j->first(j).addProperty(key,42));invalid(j->j.addProperty("sections",false));}
    public static void invalidIds(){invalid(j->first(j).addProperty("id","../invalid"));invalid(j->first(j).addProperty("id","invented-id"));invalid(j->{var a=j.getAsJsonObject("sections").getAsJsonArray(InspectionChecklist.SECTIONS.get(0));a.get(1).getAsJsonObject().add("id",first(j).get("id"));});}
    public static void malformed(){for(String raw:List.of("{broken","{} trailing","null","[]","{\"sections\":{},\"sections\":{}}"))failure(r->new GeminiAdapter.Reply(200,envelope(raw)),"structure/evidence");}
    public static void rejectionReasons(){
        for(var pair:Map.of("SCHEMA_COMPLEXITY","Response schema is too complex.","SCHEMA_FIELD","Invalid response_json_schema field.","RESPONSE_CONFIGURATION","Invalid response_mime_type.").entrySet())
            failure(r->new GeminiAdapter.Reply(400,new Gson().toJson(Map.of("error",Map.of("status","INVALID_ARGUMENT","message",pair.getValue()+PRIVATE+KEY)))),"["+pair.getKey()+"]");
    }
    public static void fallbackState(){
        var calls=new AtomicInteger();var f=ReportChecks.saved(new GeminiAdapter(KEY,null,r->{calls.incrementAndGet();return new GeminiAdapter.Reply(400,PRIVATE+KEY);}));
        var old=f.generateCurrentChecklist();f.completeChecklistItem(old.generationId(),"documents",true);
        check(f.currentChecklist().orElseThrow().completed().contains("documents")&&calls.get()==1,"Fallback is usable; show/mark never requests provider");
        var next=f.generateCurrentChecklist();check(next.completed().isEmpty()&&!next.generationId().equals(old.generationId())&&calls.get()==2,"Explicit regeneration resets completion once");
        var form=ReportChecks.corolla();form.put("price","14000");f.saveVehicle(PreparationForm.vehicle(f,form));check(f.currentChecklist().isEmpty(),"Vehicle edit invalidates fallback snapshot");
    }
    public static void cliDiagnostics(){
        var calls=new AtomicInteger();var f=ReportChecks.saved(new GeminiAdapter(KEY,null,r->{calls.incrementAndGet();return new GeminiAdapter.Reply(400,PRIVATE+KEY);}));
        var out=new ByteArrayOutputStream();new UsedCarCLI(f,new Scanner("checklist\nchecklist-mark documents\nchecklist-show\nquit\n"),new PrintStream(out)).run();String text=out.toString();
        check(text.contains("FALLBACK")&&text.contains("[PROVIDER_HTTP_400]")&&text.contains("[x] documents")&&!text.contains(KEY)&&!text.contains(PRIVATE)&&calls.get()==1,"CLI exposes safe classification and cached usable fallback");
    }
    public static void httpGui()throws Exception{
        var calls=new AtomicInteger();var f=ReportChecks.saved(new GeminiAdapter(KEY,null,r->{check(!javax.swing.SwingUtilities.isEventDispatchThread(),"HTTP off EDT");calls.incrementAndGet();return new GeminiAdapter.Reply(400,PRIVATE+KEY);}));
        var panel=new AtomicReference<ChecklistPanel>();var frame=new AtomicReference<javax.swing.JFrame>();
        javax.swing.SwingUtilities.invokeAndWait(()->{var p=new ChecklistPanel(f);panel.set(p);var w=new javax.swing.JFrame();frame.set(w);w.setContentPane(p);w.pack();w.setVisible(true);((javax.swing.JButton)WorkspaceGuiSmoke.named(p,"checklistGenerate")).doClick();});
        try{
            var ready=new AtomicBoolean();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
            while(!ready.get()&&System.nanoTime()<deadline){javax.swing.SwingUtilities.invokeAndWait(()->ready.set(((javax.swing.JButton)WorkspaceGuiSmoke.named(panel.get(),"checklistGenerate")).isEnabled()));Thread.sleep(20);}
            check(ready.get(),"GUI fallback finishes");
            javax.swing.SwingUtilities.invokeAndWait(()->{String text=((javax.swing.JTextArea)WorkspaceGuiSmoke.named(panel.get(),"checklistStatus")).getText();check(text.contains("FALLBACK")&&text.contains("[PROVIDER_HTTP_400]")&&!text.contains(KEY)&&!text.contains(PRIVATE),"F10 GUI preserves sanitized rejection classification");((javax.swing.JCheckBox)WorkspaceGuiSmoke.named(panel.get(),"checklistItem-documents")).doClick();((javax.swing.JButton)WorkspaceGuiSmoke.named(panel.get(),"checklistShow")).doClick();check(f.currentChecklist().orElseThrow().completed().contains("documents")&&calls.get()==1,"GUI fallback completion/cache remains usable without requests");});
            System.out.println("PASS: AUD-04 actual adapter HTTP 400 GUI diagnostic, usable deterministic checklist and local completion.");
        }finally{javax.swing.SwingUtilities.invokeAndWait(()->frame.get().dispose());}
    }
    public static void runAll(){metricReasons();javaItemBound();schemaSerialization();workflow();missingFields();wrongTypes();invalidIds();malformed();badRequest();quota();server();timeout();rejectionReasons();fallbackState();cliDiagnostics();}
}
