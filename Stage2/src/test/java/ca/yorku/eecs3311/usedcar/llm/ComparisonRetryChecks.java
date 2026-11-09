package ca.yorku.eecs3311.usedcar.llm;

import ca.yorku.eecs3311.usedcar.*;
import ca.yorku.eecs3311.usedcar.ui.ResultFormatter;
import com.google.gson.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.net.http.HttpTimeoutException;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

/** Retry classification is exercised through the actual adapter, never a paid endpoint. */
public final class ComparisonRetryChecks {
    private static final String KEY="fictional-f09-offline-key";
    private static GeminiAdapter.Reply success(java.net.http.HttpRequest request) throws java.io.IOException,InterruptedException {
        var body=JsonParser.parseString(ComparisonGeminiChecks.body(request)).getAsJsonObject();
        String prompt=body.getAsJsonArray("contents").get(0).getAsJsonObject().getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString();
        return new GeminiAdapter.Reply(200,new Gson().toJson(Map.of("candidates",List.of(Map.of("finishReason","STOP","content",Map.of("parts",List.of(Map.of("text",ComparisonChecks.response(prompt)))))))));
    }
    public static LLMClient guiClient(AtomicInteger calls,CountDownLatch first) {
        return new GeminiAdapter(KEY,null,request->{check(!javax.swing.SwingUtilities.isEventDispatchThread(),"F09 retry must stay off EDT");if(calls.incrementAndGet()==1){first.countDown();return new GeminiAdapter.Reply(503,KEY);}return success(request);});
    }
    public static void recovery(){for(int code:new int[]{502,503,504}){
        var calls=new AtomicInteger();var bodies=new ArrayList<String>();
        var client=new GeminiAdapter(KEY,null,request->{bodies.add(ComparisonGeminiChecks.body(request));return calls.incrementAndGet()==1?new GeminiAdapter.Reply(code,KEY):success(request);});
        var r=ComparisonReliabilityChecks.facade(client).compareCurrentVehicles();
        check(r.status().equals("AI_SUCCESS")&&calls.get()==2&&bodies.get(0).equals(bodies.get(1))&&r.message().contains("attempts: 2"),"One bounded retry must reuse exactly the same evidence/schema request");
    }}
    public static void exhausted(){var calls=new AtomicInteger();var client=new GeminiAdapter(KEY,null,r->{calls.incrementAndGet();return new GeminiAdapter.Reply(503,KEY);});var result=ComparisonReliabilityChecks.facade(client).compareCurrentVehicles();var text=ResultFormatter.format(result);check(calls.get()==2&&result.status().equals("AI_UNAVAILABLE")&&text.contains("PROVIDER_HTTP_503")&&text.contains("18707.00")&&text.contains("19911.25")&&!text.contains(KEY)&&result.comparison().explanation().isEmpty(),"503 exhaustion preserves evidence and sanitized diagnosis without loops");}
    public static void noRetryPermanent(){for(int code:new int[]{400,401,403,404,429,500}){var calls=new AtomicInteger();var client=new GeminiAdapter(KEY,null,r->{calls.incrementAndGet();return new GeminiAdapter.Reply(code,KEY);});var result=ComparisonReliabilityChecks.facade(client).compareCurrentVehicles();check(calls.get()==1&&result.message().contains("PROVIDER_HTTP_"+code),"Only the explicit transient status allowlist may retry");}}
    public static void deferredHeader(){var calls=new AtomicInteger();var client=new GeminiAdapter(KEY,null,r->{calls.incrementAndGet();return new GeminiAdapter.Reply(503,KEY,true);});var result=ComparisonReliabilityChecks.facade(client).compareCurrentVehicles();check(calls.get()==1&&result.message().contains("automatic retry deferred"),"Retry-After must not be overridden by a short automatic wait");}
    public static void deferredMetadata(){var calls=new AtomicInteger();var client=new GeminiAdapter(KEY,null,r->{calls.incrementAndGet();return new GeminiAdapter.Reply(503,"{\"error\":{\"details\":[{\"@type\":\"type.googleapis.com/google.rpc.RetryInfo\",\"retryDelay\":\"60s\"}]}}");});var result=ComparisonReliabilityChecks.facade(client).compareCurrentVehicles();check(calls.get()==1&&result.message().contains("automatic retry deferred"),"Provider retry metadata must defer without displaying raw values");}
    public static void noRetryTimeout(){var calls=new AtomicInteger();var client=new GeminiAdapter(KEY,null,r->{calls.incrementAndGet();throw new HttpTimeoutException(KEY);});var result=ComparisonReliabilityChecks.facade(client).compareCurrentVehicles();check(calls.get()==1&&result.message().contains("TIMEOUT")&&!result.message().contains(KEY),"Ambiguous timeout is not automatically billed again");}
    public static void interruptedDelay() throws Exception {
        var calls=new AtomicInteger();var first=new CountDownLatch(1);var interrupted=new AtomicBoolean();var result=new AtomicReference<ca.yorku.eecs3311.usedcar.result.ComparisonEvaluation>();
        var f=ComparisonReliabilityChecks.facade(guiClient(calls,first));var t=new Thread(()->{result.set(f.compareCurrentVehicles());interrupted.set(Thread.currentThread().isInterrupted());});t.start();
        try{check(first.await(3,TimeUnit.SECONDS),"First request did not start");t.interrupt();t.join(2000);check(!t.isAlive()&&calls.get()==1&&interrupted.get()&&result.get().message().contains("INTERRUPTED"),"Interrupted delay must stop promptly, preserve flag and avoid second call");}finally{t.interrupt();t.join(2000);}
    }
    public static void preInterrupted() throws Exception {var calls=new AtomicInteger();var f=ComparisonReliabilityChecks.facade(guiClient(calls,new CountDownLatch(1)));var result=new AtomicReference<String>();var t=new Thread(()->{Thread.currentThread().interrupt();result.set(f.compareCurrentVehicles().message());});t.start();t.join(2000);check(calls.get()==0&&result.get().contains("INTERRUPTED"),"Cancelled work must not begin a request");}
    public static void invalidAfterRetry(){var calls=new AtomicInteger();var client=new GeminiAdapter(KEY,null,r->{if(calls.incrementAndGet()==1)return new GeminiAdapter.Reply(503,"");return new GeminiAdapter.Reply(200,"{}");});var result=ComparisonReliabilityChecks.facade(client).compareCurrentVehicles();check(calls.get()==2&&result.status().equals("AI_UNAVAILABLE")&&result.message().contains("INVALID_RESPONSE"),"Invalid second response cannot trigger a third attempt or fake success");}
    public static void otherFeaturesNoRetry(){var calls=new AtomicInteger();var client=new GeminiAdapter(KEY,null,r->{calls.incrementAndGet();return new GeminiAdapter.Reply(503,"");});var f=ComparisonReliabilityChecks.facade(client);check(f.evaluateCurrentVehicle().status().equals("AI_UNAVAILABLE")&&calls.get()==1,"F08 request policy must remain unchanged");}
    public static void runAll(){recovery();exhausted();noRetryPermanent();deferredHeader();deferredMetadata();noRetryTimeout();invalidAfterRetry();otherFeaturesNoRetry();try{interruptedDelay();preInterrupted();}catch(Exception e){throw new AssertionError(e);}}
}
