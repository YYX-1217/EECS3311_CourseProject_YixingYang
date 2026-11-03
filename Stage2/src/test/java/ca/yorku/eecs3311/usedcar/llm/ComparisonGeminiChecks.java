package ca.yorku.eecs3311.usedcar.llm;

import ca.yorku.eecs3311.usedcar.ComparisonChecks;
import ca.yorku.eecs3311.usedcar.ui.ResultFormatter;
import com.google.gson.*;
import java.io.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

/** Exercises the production Gemini request/envelope code with injected transport, never the real endpoint. */
public final class ComparisonGeminiChecks {
    private static final String KEY="fictional-comparison-credential";
    public static void workflow() {
        var calls=new AtomicInteger();
        var adapter=new GeminiAdapter(KEY,null,request->{
            calls.incrementAndGet();var json=JsonParser.parseString(body(request)).getAsJsonObject();
            var config=json.getAsJsonObject("generationConfig");
            check(config.get("responseMimeType").getAsString().equals("application/json"),"F09 must reach structured Gemini transport");
            check(config.getAsJsonObject("responseJsonSchema").getAsJsonObject("properties").has("metricSummary"),"Comparison schema must be sent");
            String prompt=json.getAsJsonArray("contents").get(0).getAsJsonObject().getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString();
            String response=ComparisonChecks.response(prompt);
            String envelope=new Gson().toJson(Map.of("candidates",List.of(Map.of("finishReason","STOP","content",Map.of("parts",List.of(Map.of("text",response)))))));
            return new GeminiAdapter.Reply(200,envelope);
        });
        var f=ComparisonChecks.facade(adapter);ComparisonChecks.savePair(f,true);
        check(f.compareCurrentVehicles().status().equals("AI_SUCCESS")&&calls.get()==1,"Real adapter/parser path must support comparison fixture with one call");
        for(boolean timeout:new boolean[]{false,true}) {
            var failed=new GeminiAdapter(KEY,null,request->{if(timeout)throw new HttpTimeoutException("fictional detail");return new GeminiAdapter.Reply(429,KEY);});
            var facade=ComparisonChecks.facade(failed);ComparisonChecks.savePair(facade,true);var result=facade.compareCurrentVehicles();
            check(result.status().equals("AI_UNAVAILABLE")&&result.comparison().explanation().isEmpty()&&result.evidence().first().cost()!=null,"HTTP/timeout failures preserve actual comparison evidence");
            check(!ResultFormatter.format(result).contains(KEY)&&!failed.status().contains(KEY),"Transport failures must not expose credentials");
        }
    }
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
