package ca.yorku.eecs3311.usedcar.llm;

import ca.yorku.eecs3311.usedcar.app.*;
import ca.yorku.eecs3311.usedcar.agent.UsedCarAgent;
import ca.yorku.eecs3311.usedcar.tool.*;
import ca.yorku.eecs3311.usedcar.ui.*;
import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

/** Offline fixtures only: never real Gemini evidence or credentials. */
public final class GeminiChecks {
    private static final String KEY = "offline-test-secret";
    private static final String VALID = "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"thought\":true,\"text\":\"hidden\"},{\"text\":\"Hello \"},{\"text\":\"world\"}]}}]}";
    private static GeminiAdapter fixture(int status, String body) {
        return new GeminiAdapter(KEY, null, r -> new GeminiAdapter.Reply(status, body));
    }
    private static void failure(GeminiAdapter adapter, String expected) {
        try { adapter.generate("test"); throw new AssertionError("Failure expected"); }
        catch (IllegalStateException e) {
            check(e.getMessage().contains(expected), "Wrong safe error: " + e.getMessage());
            check(!e.toString().contains(KEY) && e.getCause() == null && !adapter.status().contains(KEY), "Credentials must not escape through errors.");
        }
    }
    public static void httpRequest() {
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            var method = new AtomicReference<String>(); var body = new AtomicReference<String>(); var auth = new AtomicReference<String>();
            server.createContext("/test", exchange -> {
                method.set(exchange.getRequestMethod()); auth.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
                body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] bytes = VALID.getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(200, bytes.length);
                try (var output = exchange.getResponseBody()) { output.write(bytes); }
            }); server.start();
            URI local = URI.create("http://localhost:" + server.getAddress().getPort() + "/test");
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            var adapter = new GeminiAdapter(KEY, "gemini-test-model", request -> {
                check(request.uri().toString().equals("https://generativelanguage.googleapis.com/v1beta/models/gemini-test-model:generateContent"), "Model endpoint mismatch");
                check(request.uri().getQuery() == null && request.timeout().orElseThrow().equals(Duration.ofSeconds(30)), "Key must stay out of URL; request must be bounded");
                var forwarded = HttpRequest.newBuilder(local).timeout(Duration.ofSeconds(3))
                        .header("x-goog-api-key", request.headers().firstValue("x-goog-api-key").orElseThrow())
                        .header("Content-Type", request.headers().firstValue("Content-Type").orElseThrow())
                        .method(request.method(), request.bodyPublisher().orElseThrow()).build();
                var reply = client.send(forwarded, HttpResponse.BodyHandlers.ofString());
                return new GeminiAdapter.Reply(reply.statusCode(), reply.body());
            });
            String prompt = "Quoted \"text\"\nUnicode: 汽车";
            check(adapter.status().contains("not live-verified"), "Configuration must not imply connectivity");
            check(adapter.generate(prompt).equals("Hello world"), "Must join response text and omit thought parts");
            check(method.get().equals("POST") && auth.get().equals(KEY), "POST/header authentication mismatch");
            JsonObject json = JsonParser.parseString(body.get()).getAsJsonObject();
            check(json.getAsJsonArray("contents").get(0).getAsJsonObject().getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString().equals(prompt), "JSON escaping/structure mismatch");
            check(json.getAsJsonObject("generationConfig").get("maxOutputTokens").getAsInt() == 4096, "Output limit missing");
            check(adapter.status().contains("VERIFIED"), "Successful response must update session status");
            check(!json.getAsJsonObject("generationConfig").has("responseMimeType"), "Ordinary text generation must remain unchanged");
            adapter.generateStructured(prompt, ListingExtractionSchema.json());
            var structured=JsonParser.parseString(body.get()).getAsJsonObject().getAsJsonObject("generationConfig");
            check(structured.get("responseMimeType").getAsString().equals("application/json")
                    &&structured.getAsJsonObject("responseJsonSchema").equals(JsonParser.parseString(ListingExtractionSchema.json())),
                    "HTTP request must contain the exact structured JSON contract");
        } catch (IOException e) { throw new AssertionError(e); }
        finally { if (server != null) server.stop(0); }
    }
    public static void missingKey() {
        for (String key : new String[]{null, "", "   ", "YOUR_API_KEY_HERE", "your_api_key",
                "YOUR_GEMINI_API_KEY", "YOUR_GEMINI_API_KEY_HERE", "REPLACE_WITH_YOUR_API_KEY", "[YOUR_API_KEY_HERE]"}) {
            var adapter = new GeminiAdapter(key, null, r -> { throw new AssertionError("No request allowed"); });
            check(adapter.status().contains("NOT CONFIGURED"), "Missing/placeholder key must not appear configured");
            failure(adapter, "NOT CONFIGURED");
        }
        failure(new GeminiAdapter(KEY, "../bad", r -> { throw new AssertionError(); }), "CONFIGURATION ERROR");
        keyCompatibility();
        unsafeKeys();
    }
    private static void keyCompatibility() {
        // Fictional opaque credentials, not assertions about Google's private key encoding.
        // Dots, colons, slashes, plus and padding must not be rejected by legacy assumptions.
        for (String key : new String[]{"fictional.auth.payload.signature", "fictional:auth/token+padding==",
                "AIza_FICTIONAL-standard-0123456789", "fictional-short", "fictional." + "a".repeat(2048)}) {
            var calls = new java.util.concurrent.atomic.AtomicInteger();
            var adapter = new GeminiAdapter(key, null, request -> {
                calls.incrementAndGet();
                check(request.headers().firstValue("x-goog-api-key").orElseThrow().equals(key),
                        "Opaque credential must reach header unchanged");
                check(request.uri().getQuery() == null, "Credential must not enter URL");
                return new GeminiAdapter.Reply(200, VALID);
            });
            check(adapter.status().contains("not live-verified") && calls.get() == 0,
                    "Header-compatible keys must remain unverified until explicit generation");
            check(adapter.generate("offline fixture").equals("Hello world") && calls.get() == 1,
                    "Compatible credential must reach transport exactly once");
            check(!adapter.status().contains(key), "Status must not expose credential");
            failure(new GeminiAdapter(key, null, request -> new GeminiAdapter.Reply(401, key)), "authorization denied");
        }
    }
    private static void unsafeKeys() {
        var invalid = new java.util.ArrayList<String>();
        for (int c = 0; c <= 0x20; c++) {
            invalid.add("fictional" + (char)c + "key");
            invalid.add((char)c + "fictional-key");
            invalid.add("fictional-key" + (char)c);
        }
        invalid.add("fictional" + (char)127 + "key");
        invalid.add("fictional" + (char)133 + "key");
        invalid.add("fictional" + (char)0x200b + "key");
        for (String key : invalid) {
            var adapter = new GeminiAdapter(key, null, r -> { throw new AssertionError("Unsafe key reached transport"); });
            check(adapter.status().contains("unsafe header characters") && !adapter.status().contains(key),
                    "Unsafe key must be rejected without disclosure");
            failure(adapter, "unsafe header characters");
        }
    }
    public static void authorization() { for (int s : new int[]{401,403}) failure(fixture(s, KEY), "authorization denied"); }
    public static void quota() { failure(fixture(429, KEY), "quota or rate limit"); }
    public static void serverError() { for (int s : new int[]{500,503}) failure(fixture(s, KEY), "provider server error"); }
    public static void empty() { failure(fixture(200, ""), "invalid, empty"); }
    public static void malformed() {
        for (String body : new String[]{"not JSON " + KEY, "{candidates:[]}", "{} trailing", "null", "[]"}) failure(fixture(200,body), "invalid, empty");
    }
    public static void missingText() {
        for (String body : new String[]{"{}", "{\"candidates\":[]}", "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[]}}]}", VALID.replace("Hello ", "").replace("world", "")}) failure(fixture(200,body), "invalid, empty");
    }
    public static void blocked() {
        failure(fixture(200, VALID.replace("STOP", "SAFETY")), "invalid, empty");
        failure(fixture(200, VALID.replace("STOP", "MAX_TOKENS")), "invalid, empty");
    }
    public static void transportErrors() {
        failure(new GeminiAdapter(KEY,null,r -> {throw new HttpTimeoutException(KEY);}), "timed out");
        failure(new GeminiAdapter(KEY,null,r -> {throw new IOException(KEY);}), "connection or transport");
        failure(new GeminiAdapter(KEY,null,r -> {throw new IllegalArgumentException(KEY);}), "connection or transport");
    }
    public static void interrupted() {
        try {
            failure(new GeminiAdapter(KEY,null,r -> {throw new InterruptedException(KEY);}), "interrupted");
            check(Thread.currentThread().isInterrupted(), "Interrupt flag must be restored");
        } finally { Thread.interrupted(); }
    }
    public static void redaction() {
        var adapter = fixture(200, VALID.replace("world", KEY));
        check(adapter.generate("test").equals("Hello [REDACTED]"), "Even a successful response must not echo the credential");
        failure(fixture(302,KEY), "HTTP 302");
    }
    public static void cliIntegration() {
        var adapter = fixture(200, VALID);
        var agent = new UsedCarAgent(new ToolManager(), adapter, new DataQualityChecker());
        var facade = new UsedCarApplicationFacade(agent, new EvaluationSession());
        var original = facade.preferences(); var bytes = new ByteArrayOutputStream();
        new UsedCarCLI(facade,new Scanner("status\nllm-test\nstatus\nquit\n"),new PrintStream(bytes)).run();
        String output = bytes.toString(StandardCharsets.UTF_8);
        check(output.contains("not live-verified") && output.contains("Hello world") && output.contains("VERIFIED"), "CLI must reach real adapter boundary and show its state");
        check(facade.preferences().equals(original) && facade.currentVehicle().isEmpty() && !output.contains(KEY), "Connection test must not mutate session or expose keys");
    }
    public static void failureState() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var adapter = new GeminiAdapter(KEY,null,r -> calls.incrementAndGet() == 1 ? new GeminiAdapter.Reply(200,VALID) : new GeminiAdapter.Reply(429,KEY));
        adapter.generate("test"); failure(adapter,"429");
        check(adapter.status().contains("REQUEST FAILED") && calls.get() == 2, "Failure must replace prior success without retries");
    }
    public static void runAll() { httpRequest(); missingKey(); authorization(); quota(); serverError(); empty(); malformed(); missingText(); blocked(); transportErrors(); interrupted(); redaction(); cliIntegration(); failureState(); }
}
