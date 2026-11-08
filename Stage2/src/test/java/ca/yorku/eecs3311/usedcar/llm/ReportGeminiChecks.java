package ca.yorku.eecs3311.usedcar.llm;

import ca.yorku.eecs3311.usedcar.ReportChecks;
import ca.yorku.eecs3311.usedcar.command.GenerateReportCommand;
import ca.yorku.eecs3311.usedcar.result.*;
import ca.yorku.eecs3311.usedcar.ui.*;
import com.google.gson.*;
import java.io.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

/** Production adapter/envelope/report path with injected transport only; no real credentials or requests. */
public final class ReportGeminiChecks {
    private static final String KEY="fictional-report-credential";
    private static final String PRIVATE="fictional-private-provider-content";
    private static String envelope(String text,String finish){
        return new Gson().toJson(Map.of("candidates",List.of(Map.of("finishReason",finish,"content",Map.of("parts",List.of(Map.of("text",text)))))));
    }
    private static String prompt(HttpRequest request) throws IOException,InterruptedException {
        return JsonParser.parseString(body(request)).getAsJsonObject().getAsJsonArray("contents").get(0).getAsJsonObject()
                .getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString();
    }
    private static ReportEvaluation fail(GeminiAdapter.Transport transport,String code){
        var calls=new AtomicInteger();
        var adapter=new GeminiAdapter(KEY,null,request->{calls.incrementAndGet();return transport.send(request);});
        var facade=ReportChecks.saved(adapter);var result=new GenerateReportCommand(facade).execute();
        check(result.status().equals("AI_UNAVAILABLE")&&result.report()==null&&result.message().contains(code),"Failure must identify stage/reason: "+code);
        check(result.evidence().deterministic().cost().firstYearEstimate().toPlainString().equals("18707.00"),"Failure preserves exact Java costs");
        String display=ResultFormatter.format(result);
        check(!display.contains(KEY)&&!display.contains(PRIVATE)&&!adapter.status().contains(KEY)&&!adapter.status().contains(PRIVATE),"Raw provider/exception data must never be displayed");
        check(facade.currentReport().orElseThrow()==result&&calls.get()==1,"Failed snapshot is cached without retries or hidden calls");
        return result;
    }
    private static void http(int status){fail(r->new GeminiAdapter.Reply(status,PRIVATE+KEY),"[PROVIDER_HTTP_"+status+"]");}
    public static void workflow(){
        var calls=new AtomicInteger();var adapter=new GeminiAdapter(KEY,"gemini-fictional-model",request->{
            calls.incrementAndGet();var json=JsonParser.parseString(body(request)).getAsJsonObject();
            var config=json.getAsJsonObject("generationConfig");
            check(request.uri().getPath().endsWith("/gemini-fictional-model:generateContent")&&request.uri().getQuery()==null,"Keep selected model and header-only authentication");
            check(request.timeout().orElseThrow().getSeconds()==30&&request.headers().firstValue("x-goog-api-key").orElseThrow().equals(KEY),"Preserve timeout/authentication");
            check(!json.toString().contains(KEY)&&config.get("responseMimeType").getAsString().equals("application/json"),"F12 request is JSON and does not contain credentials");
            var schema=config.getAsJsonObject("responseJsonSchema");
            check(schema.getAsJsonArray("required").size()==9&&schema.getAsJsonObject("properties").getAsJsonObject("category").getAsJsonArray("enum").size()==4,"Original nine fields and four categories reach adapter");
            String p=prompt(request);
            check(!p.contains("\nSchema: ")&&p.contains("Approved report sections: "),"Schema is supplied only through generationConfig; prompt retains authoritative evidence");
            return new GeminiAdapter.Reply(200,envelope(ReportChecks.response(p),"STOP"));
        });
        var facade=ReportChecks.saved(adapter);var result=new GenerateReportCommand(facade).execute();
        check(result.status().equals("AI_SUCCESS")&&result.report().costSummary().equals(result.evidence().costSummary()),"Real adapter fixture passes parser AND evidence validation");
        check(facade.currentReport().orElseThrow()==result&&calls.get()==1,"Saved report must not repeat generation");
    }
    public static void badRequest(){http(400);}
    public static void authorization(){http(401);http(403);}
    public static void quota(){http(429);}
    public static void model(){http(404);}
    public static void server(){http(500);http(503);}
    public static void timeout(){fail(r->{throw new HttpTimeoutException(PRIVATE+KEY);},"[TIMEOUT]");}
    public static void transport(){fail(r->{throw new IOException(PRIVATE+KEY);},"[TRANSPORT_ERROR]");}
    public static void interrupted(){try{fail(r->{throw new InterruptedException(PRIVATE+KEY);},"[INTERRUPTED]");check(Thread.currentThread().isInterrupted(),"Interrupt status must be restored");}finally{Thread.interrupted();}}
    public static void configuration(){
        for(String key:new String[]{"", "fictional\rkey"}){
            var result=ReportChecks.saved(new GeminiAdapter(key,null,r->{throw new AssertionError("No transport for invalid configuration");})).generateCurrentReport();
            check(result.message().contains(key.isEmpty()?"[NOT_CONFIGURED]":"[INVALID_CONFIGURATION]")&&result.report()==null,"Local configuration is separate from HTTP failure");
        }
        var result=ReportChecks.saved(new GeminiAdapter(KEY,"../invalid",r->{throw new AssertionError("Invalid model must not send");})).generateCurrentReport();
        check(result.message().contains("[INVALID_CONFIGURATION]"),"Invalid local model differs from HTTP 404");
    }
    public static void blocked(){
        fail(r->new GeminiAdapter.Reply(200,"{\"promptFeedback\":{\"blockReason\":\""+PRIVATE+"\"}}"),"[BLOCKED_RESPONSE]");
        fail(r->new GeminiAdapter.Reply(200,envelope(PRIVATE+KEY,"SAFETY")),"[BLOCKED_RESPONSE]");
    }
    public static void outputLimit(){
        var r=fail(request->new GeminiAdapter.Reply(200,envelope(ReportChecks.response(prompt(request)),"MAX_TOKENS")),"[OUTPUT_LIMIT]");
        check(r.message().contains("MAX_TOKENS")&&!r.message().contains("REPORT_STRUCTURE"),"Even parseable text is rejected when provider reports truncation");
    }
    public static void incomplete(){fail(r->new GeminiAdapter.Reply(200,envelope(PRIVATE,PRIVATE+KEY)),"[INCOMPLETE_RESPONSE]");}
    public static void invalidEnvelope(){for(String text:List.of("not-json "+PRIVATE+KEY,"{}","{\"candidates\":[]}",envelope("","STOP")))fail(r->new GeminiAdapter.Reply(200,text),"[INVALID_RESPONSE]");}
    public static void structure(){
        fail(r->new GeminiAdapter.Reply(200,envelope("not-json "+PRIVATE+KEY,"STOP")),"[REPORT_STRUCTURE]");
        fail(r->{var value=JsonParser.parseString(ReportChecks.response(prompt(r))).getAsJsonObject();value.addProperty("risks",PRIVATE+KEY);return new GeminiAdapter.Reply(200,envelope(value.toString(),"STOP"));},"[REPORT_STRUCTURE]");
    }
    public static void evidence(){
        for(String field:List.of("costSummary","strengths","risks","constraintConflicts","missingInformation","inspectionPriorities","nextSteps")){
            var result=fail(r->{var value=JsonParser.parseString(ReportChecks.response(prompt(r))).getAsJsonObject();
                if(field.equals("costSummary"))value.addProperty(field,PRIVATE+KEY);
                else {var array=new JsonArray();array.add(PRIVATE+KEY);value.add(field,array);}
                return new GeminiAdapter.Reply(200,envelope(value.toString(),"STOP"));},"[REPORT_EVIDENCE]");
            check(result.message().contains(field),"Evidence rejection identifies the affected field without its value");
        }
    }
    public static void unknownClient(){
        var client=new ReportChecks.Fake(){public String generateStructured(String p,String s){throw new IllegalStateException(PRIVATE+KEY);}};
        var result=ReportChecks.saved(client).generateCurrentReport();
        check(result.message().contains("[REQUEST_FAILURE]")&&!result.message().contains(PRIVATE)&&!result.message().contains(KEY),"Unknown clients cannot inject exception messages into UI");
    }
    public static void cliDiagnostics(){
        var calls=new AtomicInteger();var adapter=new GeminiAdapter(KEY,null,r->{calls.incrementAndGet();return new GeminiAdapter.Reply(429,PRIVATE+KEY);});
        var f=ReportChecks.saved(adapter);var output=new ByteArrayOutputStream();
        new UsedCarCLI(f,new Scanner("report\nreport-show\nquit\n"),new PrintStream(output)).run();
        String text=output.toString();check(text.contains("[PROVIDER_HTTP_429]")&&text.contains("18707.00")&&!text.contains(PRIVATE)&&!text.contains(KEY)&&calls.get()==1,"CLI shows safe diagnosis and cached evidence");
    }
    public static void localSchema(){
        var adapter=new GeminiAdapter(KEY,null,r->{throw new AssertionError("Malformed schema must never send");});
        for(String schema:new String[]{"", "null", "[]", "{broken"})try{adapter.generateStructured("fictional",schema);throw new AssertionError("Schema must fail");}
        catch(LLMFailure e){check(e.kind()==LLMFailure.Kind.INVALID_REQUEST_SCHEMA&&e.diagnostic().contains("[INVALID_REQUEST_SCHEMA]")&&e.getCause()==null,"Local schema failure is distinct and contains no raw input");}
    }
    private static void compactContract(JsonObject schema){
        var arrays=Set.of("strengths","risks","missingInformation","constraintConflicts","inspectionPriorities","nextSteps");
        var names=new HashSet<>(arrays);names.addAll(List.of("category","costSummary","explanation"));
        check(schema.keySet().equals(Set.of("type","properties","required","additionalProperties"))&&schema.get("type").getAsString().equals("object")&&!schema.get("additionalProperties").getAsBoolean(),"Compact object contract uses documented supported keywords");
        var fields=schema.getAsJsonObject("properties");var required=new HashSet<String>();schema.getAsJsonArray("required").forEach(v->required.add(v.getAsString()));
        check(fields.keySet().equals(names)&&required.equals(names)&&schema.getAsJsonArray("required").size()==9,"All nine fields remain required exactly once");
        for(String name:arrays){
            var array=fields.getAsJsonObject(name);
            check(array.keySet().equals(Set.of("type","items"))&&array.get("type").getAsString().equals("array"),"No evidence-dependent bounds, including maxItems zero: "+name);
            check(array.getAsJsonObject("items").equals(JsonParser.parseString("{\"type\":\"string\"}")),"No large dynamic evidence enums: "+name);
        }
        for(String name:List.of("costSummary","explanation"))check(fields.getAsJsonObject(name).equals(JsonParser.parseString("{\"type\":\"string\"}")),"Scalar text is not a long single-value enum");
        var category=fields.getAsJsonObject("category");
        check(category.keySet().equals(Set.of("type","enum"))&&category.get("type").getAsString().equals("string")&&category.getAsJsonArray("enum").equals(new Gson().toJsonTree(EvaluationReport.CATEGORIES)),"Keep exactly the four Stage1 categories");
    }
    public static void compactSchema(){
        var report=ReportChecks.saved(new ReportChecks.Fake()).generateCurrentReport();
        check(report.evidence().conflicts().isEmpty(),"Reproduce the original zero-conflict Corolla scenario");
        compactContract(JsonParser.parseString(ReportSchema.json(report.evidence())).getAsJsonObject());
    }
    public static void schemaIndependent(){
        var f=ReportChecks.saved(new ReportChecks.Fake());var first=f.generateCurrentReport();
        f.savePreferences(ca.yorku.eecs3311.usedcar.domain.UserPreference.limits(new java.math.BigDecimal("1"),1));var second=f.generateCurrentReport();
        check(!second.evidence().conflicts().isEmpty(),"Exercise actual buyer conflicts");
        var e=second.evidence();var large=new ReportEvidence(e.deterministic(),e.assumptions(),e.risk(),PRIVATE.repeat(500),e.inspection(),e.clarificationHistory(),e.unresolvedConflicts(),e.referenceYear());
        String schema=ReportSchema.json(first.evidence());
        check(schema.equals(ReportSchema.json(second.evidence()))&&schema.equals(ReportSchema.json(large))&&!schema.contains(PRIVATE),"Provider schema stays fixed as evidence size, values and conflict counts change");
    }
    public static void wireContract(){
        var requests=new ArrayList<JsonObject>();var endpoints=new ArrayList<java.net.URI>();
        var adapter=new GeminiAdapter(KEY,null,request->{
            var json=JsonParser.parseString(body(request)).getAsJsonObject();requests.add(json);endpoints.add(request.uri());
            check(request.method().equals("POST")&&request.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"),"Documented HTTP method/content type");
            String p=prompt(request);return new GeminiAdapter.Reply(200,envelope(p.startsWith("Generate an advisory")?ReportChecks.response(p):"{}","STOP"));
        });
        adapter.generateStructured("fictional F08 serialization comparison",RiskAssessmentSchema.json());
        adapter.generateStructured("fictional F11 serialization comparison",ClarificationSchema.json(List.of(ca.yorku.eecs3311.usedcar.domain.ClarificationField.ACCIDENT)));
        var result=ReportChecks.saved(adapter).generateCurrentReport();check(result.status().equals("AI_SUCCESS"),"Actual F12 transport/parser workflow completes offline");
        check(requests.size()==3&&new HashSet<>(endpoints).size()==1&&endpoints.get(0).getPath().endsWith("/"+GeminiAdapter.DEFAULT_MODEL+":generateContent"),"Same configured model/endpoint for F08, F11 and F12");
        for(var request:requests){
            check(request.keySet().equals(Set.of("contents","generationConfig")),"No unexpected request fields");
            var config=request.getAsJsonObject("generationConfig");
            check(config.keySet().equals(Set.of("responseMimeType","responseJsonSchema","maxOutputTokens")),"Exactly one schema representation; no conflicting responseSchema/responseFormat");
            check(config.get("responseMimeType").getAsString().equals("application/json")&&config.get("responseJsonSchema").isJsonObject()&&config.get("maxOutputTokens").getAsInt()==4096,"Schema is an object, not double-encoded text; MIME and limits unchanged");
            var content=request.getAsJsonArray("contents").get(0).getAsJsonObject();check(content.get("role").getAsString().equals("user")&&content.getAsJsonArray("parts").size()==1,"Documented contents/parts structure");
            check(!request.toString().contains(KEY),"Credential absent from JSON");
        }
        compactContract(requests.get(2).getAsJsonObject("generationConfig").getAsJsonObject("responseJsonSchema"));
    }
    public static void strictCategories(){
        fail(r->{var value=JsonParser.parseString(ReportChecks.response(prompt(r))).getAsJsonObject();value.addProperty("category","Strong Candidate");
            return new GeminiAdapter.Reply(200,envelope(value.toString(),"STOP"));},"[REPORT_EVIDENCE]");
        fail(r->{var value=JsonParser.parseString(ReportChecks.response(prompt(r))).getAsJsonObject();value.addProperty("category","Buy Now");
            return new GeminiAdapter.Reply(200,envelope(value.toString(),"STOP"));},"[REPORT_STRUCTURE]");
    }
    public static void strictBounds(){
        for(boolean duplicate:new boolean[]{false,true})fail(r->{
            var value=JsonParser.parseString(ReportChecks.response(prompt(r))).getAsJsonObject();var items=new JsonArray();
            for(int i=0;i<(duplicate?2:65);i++)items.add(duplicate?"same":"item "+i);
            value.add("strengths",items);return new GeminiAdapter.Reply(200,envelope(value.toString(),"STOP"));},"[REPORT_STRUCTURE]");
    }
    private static String serverError(String status,String message,Object details){
        return new Gson().toJson(Map.of("error",Map.of("code",400,"status",status,"message",message,"details",details)));
    }
    private static Object fieldError(String field){
        return List.of(Map.of("@type","type.googleapis.com/google.rpc.BadRequest","fieldViolations",List.of(Map.of("field",field,"description",PRIVATE+KEY))));
    }
    public static void schemaComplexityReason(){
        for(String message:List.of("Too many states for decoding constraints.","Response schema is too complex.","Schema is too large."))
            fail(r->new GeminiAdapter.Reply(400,serverError("INVALID_ARGUMENT",message+PRIVATE+KEY,List.of())),"[SCHEMA_COMPLEXITY]");
    }
    public static void schemaFieldReason(){
        for(String field:List.of("generation_config.response_json_schema.properties[private]","generationConfig.responseJsonSchema.properties.private.maxItems","generationConfig.responseSchema"))
            fail(r->new GeminiAdapter.Reply(400,serverError("INVALID_ARGUMENT",PRIVATE+KEY,fieldError(field))),"[SCHEMA_FIELD]");
        fail(r->new GeminiAdapter.Reply(400,serverError("INVALID_ARGUMENT","Invalid response_json_schema: "+PRIVATE+KEY,List.of())),"[SCHEMA_FIELD]");
    }
    public static void responseConfigReason(){
        fail(r->new GeminiAdapter.Reply(400,serverError("INVALID_ARGUMENT",PRIVATE+KEY,fieldError("generationConfig.responseMimeType"))),"[RESPONSE_CONFIGURATION]");
        fail(r->new GeminiAdapter.Reply(400,serverError("INVALID_ARGUMENT","Unsupported response_mime_type: "+PRIVATE+KEY,List.of())),"[RESPONSE_CONFIGURATION]");
    }
    public static void genericRejectionReason(){
        fail(r->new GeminiAdapter.Reply(400,serverError("INVALID_ARGUMENT",PRIVATE+KEY,fieldError("contents[0].private"))),"[INVALID_ARGUMENT]");
        fail(r->new GeminiAdapter.Reply(400,serverError("FAILED_PRECONDITION",PRIVATE+KEY,List.of())),"[FAILED_PRECONDITION]");
    }
    public static void malformedRejectionSafety(){
        for(String data:List.of("not json "+KEY+PRIVATE,"{}","null","[]",serverError(PRIVATE+KEY,"schema too complex",List.of()),"x".repeat(65537))){
            var result=fail(r->new GeminiAdapter.Reply(400,data),"[PROVIDER_HTTP_400]");
            check(!result.message().contains("[SCHEMA_COMPLEXITY]")&&!result.message().contains("[SCHEMA_FIELD]"),"Unknown malformed errors cannot manufacture a schema diagnosis");
        }
        var malformed=fail(r->new GeminiAdapter.Reply(400,"{\"error\":{\"status\":\"INVALID_ARGUMENT\",\"message\":[],\"details\":[null]}}"),"[INVALID_ARGUMENT]");
        check(!malformed.message().contains("[SCHEMA_FIELD]"),"Ignore wrong-type optional error metadata safely");
    }
    public static void runAll(){workflow();badRequest();authorization();quota();model();server();timeout();transport();interrupted();configuration();blocked();outputLimit();incomplete();invalidEnvelope();structure();evidence();unknownClient();cliDiagnostics();localSchema();compactSchema();schemaIndependent();wireContract();strictCategories();strictBounds();schemaComplexityReason();schemaFieldReason();responseConfigReason();genericRejectionReason();malformedRejectionSafety();}
    private static String body(HttpRequest request) throws IOException,InterruptedException {
        var bytes=new ByteArrayOutputStream();var done=new CountDownLatch(1);
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>(){
            public void onSubscribe(Flow.Subscription subscription){subscription.request(Long.MAX_VALUE);}
            public void onNext(ByteBuffer buffer){byte[] data=new byte[buffer.remaining()];buffer.get(data);bytes.writeBytes(data);}
            public void onError(Throwable failure){done.countDown();}
            public void onComplete(){done.countDown();}
        });
        if(!done.await(2,TimeUnit.SECONDS))throw new IOException("Fixture body timeout");
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
