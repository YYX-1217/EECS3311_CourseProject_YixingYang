package ca.yorku.eecs3311.usedcar.llm;

import ca.yorku.eecs3311.usedcar.ClarificationChecks;
import ca.yorku.eecs3311.usedcar.result.*;
import ca.yorku.eecs3311.usedcar.ui.*;
import ca.yorku.eecs3311.usedcar.domain.ClarificationField;
import com.google.gson.*;
import java.io.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static ca.yorku.eecs3311.usedcar.domain.ClarificationField.*;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

/** Actual request serialization, adapter and F11 facade; injected HTTP transport, fictional credentials only. */
public final class ClarificationGeminiChecks {
    private static final String KEY = "fictional-f11-test-credential";
    private static final String PRIVATE = "fictional-private-provider-data";
    private static final List<ClarificationField> TARGETS = List.of(ACCIDENT, MAINTENANCE, CONSUMPTION);
    private static JsonObject response() {
        var questions = new JsonArray();
        // Independent provider fixture: alternate approved wording, reversed order, mixed-case target IDs.
        for (var field : List.of(CONSUMPTION, MAINTENANCE, ACCIDENT)) {
            var q = new JsonObject(); q.addProperty("field", field.key().toUpperCase(Locale.ROOT));
            q.addProperty("question", ClarificationQuestion.wordings(field).get(1));
            q.addProperty("priority", field == CONSUMPTION ? "Medium" : "High");
            q.addProperty("reason", ClarificationQuestion.REASON); questions.add(q);
        }
        var value = new JsonObject(); value.add("questions", questions); return value;
    }
    private static String envelope(String text, String finish) {
        return new Gson().toJson(Map.of("candidates", List.of(Map.of("finishReason", finish,
                "content", Map.of("parts", List.of(Map.of("text", text)))))));
    }
    private static GeminiAdapter.Reply ok(String text) { return new GeminiAdapter.Reply(200, envelope(text, "STOP")); }
    private static ClarificationBatch fail(GeminiAdapter.Transport transport, String code) {
        var calls = new AtomicInteger();
        var adapter = new GeminiAdapter(KEY, null, r -> { calls.incrementAndGet(); return transport.send(r); });
        var facade = ClarificationChecks.saved(adapter);
        var before = facade.currentVehicle(); var prefs = facade.preferences();
        var state = facade.startClarification(false);
        check(state.batch().status().equals("FALLBACK") && state.batch().message().contains(code), "Expected safe F11 failure " + code);
        check(state.batch().questions().stream().map(ClarificationQuestion::field).toList().equals(TARGETS), "Keep all three deterministic gaps");
        check(before.equals(facade.currentVehicle()) && prefs.equals(facade.preferences()), "Failed AI cannot mutate vehicle/preferences");
        check(calls.get() == 1 && facade.startClarification(false).cycleId().equals(state.cycleId()) && calls.get() == 1,
                "No automatic retry; cached cycle is reused");
        String display = ResultFormatter.format(state);
        check(!display.contains(KEY) && !display.contains(PRIVATE), "Never display raw provider data or credentials");
        check(display.contains("Deterministic questions remain available") && display.contains("new application session"), "Actionable recovery guidance");
        return state.batch();
    }
    public static void valid() {
        var calls = new AtomicInteger();
        var facade = ClarificationChecks.saved(new GeminiAdapter(KEY, null, r -> { calls.incrementAndGet(); return ok(response().toString()); }));
        var before = facade.currentVehicle(); var state = facade.startClarification(false);
        check(state.batch().status().equals("AI_SUCCESS") && calls.get() == 1 && before.equals(facade.currentVehicle()), "Real adapter fixture reaches validated F11 success without saving answers");
        check(state.batch().questions().get(2).field() == CONSUMPTION, "History priorities sort before fuel consumption");
        check(state.batch().questions().stream().allMatch(q -> q.question().startsWith("Can you confirm")), "Success retains actual validated provider selections, not fallback text");
    }
    public static void wireContract() {
        for (int count = 1; count <= 3; count++) {
            var targets = TARGETS.subList(0, count);
            String schema = ClarificationSchema.json(targets);
            var adapter = new GeminiAdapter(KEY, null, request -> {
                var root = JsonParser.parseString(body(request)).getAsJsonObject();
                var config = root.getAsJsonObject("generationConfig");
                check(request.uri().toString().equals("https://generativelanguage.googleapis.com/v1beta/models/" + GeminiAdapter.DEFAULT_MODEL + ":generateContent"), "Keep model and endpoint");
                check(request.method().equals("POST") && request.timeout().orElseThrow().getSeconds() == 30, "Keep POST and bounded timeout");
                check(config.get("responseMimeType").getAsString().equals("application/json") && !config.has("responseSchema"), "Use JSON Schema configuration exclusively");
                check(config.get("responseJsonSchema").isJsonObject() && config.get("responseJsonSchema").equals(JsonParser.parseString(schema)), "Serialize schema as object, not escaped JSON string");
                check(config.get("maxOutputTokens").getAsInt() == 4096, "Existing output bound preserved");
                supported(config.getAsJsonObject("responseJsonSchema"));
                return ok("{}");
            });
            adapter.generateStructured("Generate clarification questions for fictional evidence only.", schema);
        }
    }
    // Check the documented subset used here, independently of response parsing.
    private static void supported(JsonObject schema) {
        check(Set.of("type", "properties", "required", "additionalProperties", "items", "minItems", "maxItems", "enum", "anyOf").containsAll(schema.keySet()), "Only documented JSON Schema keywords");
        if (schema.has("properties")) schema.getAsJsonObject("properties").entrySet().forEach(e -> supported(e.getValue().getAsJsonObject()));
        if (schema.has("items")) supported(schema.getAsJsonObject("items"));
        if (schema.has("anyOf")) schema.getAsJsonArray("anyOf").forEach(v -> supported(v.getAsJsonObject()));
    }
    private static JsonObject array(List<ClarificationField> targets) {
        return JsonParser.parseString(ClarificationSchema.json(targets)).getAsJsonObject().getAsJsonObject("properties").getAsJsonObject("questions");
    }
    public static void boundCounts() {
        for (int count = 1; count <= 3; count++) {
            var schema = array(TARGETS.subList(0, count));
            check(schema.get("minItems").getAsInt() == count && schema.get("maxItems").getAsInt() == count,
                    "Provider contract must require exactly the count required by Java, not a fixed 1-3 range");
        }
        var missing = response(); missing.getAsJsonArray("questions").remove(0);
        fail(r -> ok(missing.toString()), "[RESPONSE_SCHEMA_ERROR]");
    }
    public static void fieldPairing() {
        var item = array(TARGETS).getAsJsonObject("items");
        check(item.has("anyOf"), "Independent enums incorrectly allow an accident ID with a consumption question");
        var alternatives = item.getAsJsonArray("anyOf");
        check(alternatives.size() == 3, "Exactly one bounded alternative per requested target");
        for (int i = 0; i < 3; i++) {
            var fields = alternatives.get(i).getAsJsonObject().getAsJsonObject("properties");
            check(fields.getAsJsonObject("field").getAsJsonArray("enum").equals(new Gson().toJsonTree(List.of(TARGETS.get(i).key()))), "Branch binds one target ID");
            check(fields.getAsJsonObject("question").getAsJsonArray("enum").equals(new Gson().toJsonTree(ClarificationQuestion.wordings(TARGETS.get(i)))), "No question from another field can match the branch");
        }
        var wrong = response(); wrong.getAsJsonArray("questions").get(2).getAsJsonObject().addProperty("question", ClarificationQuestion.wordings(CONSUMPTION).get(0));
        fail(r -> ok(wrong.toString()), "questions[2].question");
    }
    public static void priorities() {
        var branches = array(TARGETS).getAsJsonObject("items").getAsJsonArray("anyOf");
        for (int i : List.of(0, 1)) check(branches.get(i).getAsJsonObject().getAsJsonObject("properties").getAsJsonObject("priority").getAsJsonArray("enum").equals(new Gson().toJsonTree(List.of("High"))), "Schema cannot allow low-priority accident/maintenance questions");
        var wrong = response(); wrong.getAsJsonArray("questions").get(1).getAsJsonObject().addProperty("priority", "Low");
        fail(r -> ok(wrong.toString()), "questions[1].priority");
    }
    private static void http(int status) { fail(r -> new GeminiAdapter.Reply(status, PRIVATE + KEY), "[PROVIDER_HTTP_" + status + "]"); }
    public static void badRequest() {
        http(400);
        var body = new Gson().toJson(Map.of("error", Map.of("status", "INVALID_ARGUMENT", "message", "Response schema is too complex. " + PRIVATE + KEY)));
        fail(r -> new GeminiAdapter.Reply(400, body), "[SCHEMA_COMPLEXITY]");
    }
    public static void authorization() { http(401); http(403); }
    public static void model() { http(404); }
    public static void quota() { http(429); }
    public static void server() { for (int status : List.of(500, 502, 503, 504)) http(status); }
    public static void timeout() { fail(r -> { throw new HttpTimeoutException(PRIVATE + KEY); }, "[TIMEOUT]"); }
    public static void transport() { fail(r -> { throw new IOException(PRIVATE + KEY); }, "[TRANSPORT_ERROR]"); }
    public static void interrupted() {
        try {
            fail(r -> { throw new InterruptedException(PRIVATE + KEY); }, "[INTERRUPTED]");
            check(Thread.currentThread().isInterrupted(), "Preserve thread interruption");
        } finally { Thread.interrupted(); }
    }
    public static void configuration() {
        for (String key : List.of("", "fictional\runsafe")) {
            var f = ClarificationChecks.saved(new GeminiAdapter(key, null, r -> { throw new AssertionError("No transport for invalid configuration"); }));
            check(f.startClarification(false).batch().message().contains(key.isEmpty() ? "[NOT_CONFIGURED]" : "[INVALID_CONFIGURATION]"), "Configuration is not misreported as HTTP failure");
        }
    }
    public static void providerEnvelope() {
        for (String raw : List.of("{}", "not-json " + PRIVATE + KEY, "{\"candidates\":[]}"))
            fail(r -> new GeminiAdapter.Reply(200, raw), "[INVALID_RESPONSE]");
        fail(r -> new GeminiAdapter.Reply(200, envelope(PRIVATE + KEY, "MAX_TOKENS")), "[OUTPUT_LIMIT]");
        fail(r -> new GeminiAdapter.Reply(200, envelope(PRIVATE + KEY, "SAFETY")), "[BLOCKED_RESPONSE]");
    }
    public static void malformed() {
        for (String raw : List.of("{", "not-json " + PRIVATE + KEY, "```json\n" + response() + "\n```", response() + " trailing"))
            fail(r -> ok(raw), "[RESPONSE_PARSE_ERROR]");
    }
    public static void structure() {
        for (String raw : List.of("[]", "null", "{}", "{\"questions\":{}}", "{\"questions\":[]}", "{\"questions\":[],\"questions\":[]}"))
            fail(r -> ok(raw), "[RESPONSE_SCHEMA_ERROR]");
        for (String name : List.of("field", "question", "priority", "reason")) {
            var missing = response(); missing.getAsJsonArray("questions").get(0).getAsJsonObject().remove(name);
            fail(r -> ok(missing.toString()), "[RESPONSE_SCHEMA_ERROR]");
            for (JsonElement wrong : List.of(new JsonPrimitive(7), new JsonPrimitive(true), JsonNull.INSTANCE, new JsonArray(), new JsonObject())) {
                var value = response(); value.getAsJsonArray("questions").get(0).getAsJsonObject().add(name, wrong);
                fail(r -> ok(value.toString()), "[RESPONSE_SCHEMA_ERROR]");
            }
        }
    }
    public static void targets() {
        for (String target : List.of("price", "MAINTENANCE", "apiKey" + PRIVATE + KEY)) {
            var value = response(); value.getAsJsonArray("questions").get(2).getAsJsonObject().addProperty("field", target);
            fail(r -> ok(value.toString()), "[CLARIFICATION_VALIDATION_ERROR]");
        }
        var value = response(); value.getAsJsonArray("questions").get(0).getAsJsonObject().addProperty("id", PRIVATE);
        fail(r -> ok(value.toString()), "[RESPONSE_SCHEMA_ERROR]");
    }
    public static void wording() {
        for (String name : List.of("question", "reason", "priority")) {
            var value = response(); value.getAsJsonArray("questions").get(0).getAsJsonObject().addProperty(name, "The vehicle has no accidents " + PRIVATE + KEY + "?");
            fail(r -> ok(value.toString()), "[CLARIFICATION_VALIDATION_ERROR]");
        }
    }
    public static void unknownFailure() {
        var fake = new ClarificationChecks.Fake() { public String generateStructured(String p, String s) { throw new IllegalStateException(PRIVATE + KEY); } };
        var batch = ClarificationChecks.saved(fake).startClarification(false).batch();
        check(batch.message().contains("[UNKNOWN_FAILURE]") && !batch.message().contains(PRIVATE) && !batch.message().contains(KEY), "Unclassified exception text cannot leak into UI");
    }
    public static void cachedState() {
        var f = ClarificationChecks.saved(new GeminiAdapter(KEY, null, r -> { throw new HttpTimeoutException(PRIVATE); }));
        var s = f.startClarification(false); String id = f.currentVehicle().orElseThrow().id();
        f.submitClarification(s.cycleId(), ACCIDENT, "NO"); f.skipClarification(s.cycleId(), MAINTENANCE, true); f.skipClarification(s.cycleId(), CONSUMPTION, false);
        var current = f.startClarification(false);
        check(current.cycleId().equals(s.cycleId()) && current.history().stream().anyMatch(h -> h.contains("maintenance -> UNKNOWN")), "Fallback retains answered/skipped/unknown history");
        check(f.currentVehicle().orElseThrow().id().equals(id) && f.currentVehicle().orElseThrow().maintenanceHistory().equals("UNKNOWN"), "Identity and unknown evidence preserved");
    }
    public static void continuation() {
        var fake = new ClarificationChecks.Fake() { public String generateStructured(String p, String s) {
            if (p.startsWith("Generate clarification")) return response().toString(); return super.generateStructured(p, s);
        } };
        var f = ClarificationChecks.saved(fake); var s = f.startClarification(false);
        f.submitClarification(s.cycleId(), ACCIDENT, "NO"); f.submitClarification(s.cycleId(), MAINTENANCE, "DOCUMENTED"); f.submitClarification(s.cycleId(), CONSUMPTION, "7.2");
        var risk = (RiskEvaluation) f.continueClarification(s.cycleId(), false);
        check(risk.status().equals("AI_SUCCESS") && risk.evidence().cost().annualFuelCost().toPlainString().equals("1782.00") && risk.evidence().cost().firstYearEstimate().toPlainString().equals("18707.00"), "Corolla answers reach F08 and exact costs");
        var checklist = (ChecklistEvaluation) f.continueClarification(s.cycleId(), true);
        check(checklist.vehicle().maintenanceHistory().equals("DOCUMENTED") && checklist.vehicle().fuelConsumption().toPlainString().equals("7.2"), "F10 receives the same updated vehicle");
    }
    public static void cliDiagnostics() {
        var calls = new AtomicInteger();
        var f = ClarificationChecks.saved(new GeminiAdapter(KEY, null, r -> { calls.incrementAndGet(); return new GeminiAdapter.Reply(503, PRIVATE + KEY); }));
        var bytes = new ByteArrayOutputStream();
        new UsedCarCLI(f, new Scanner("clarify\nclarify-show\nclarify\nquit\n"), new PrintStream(bytes)).run();
        String text = bytes.toString();
        check(text.contains("[PROVIDER_HTTP_503]") && text.contains("FALLBACK") && !text.contains(PRIVATE) && !text.contains(KEY) && calls.get() == 1, "CLI exposes sanitized category and preserves cached/no-retry behavior");
    }
    public static void schemaInput() {
        for (var targets : List.of(List.<ClarificationField>of(), List.of(ACCIDENT, ACCIDENT), List.of(ACCIDENT, MAINTENANCE, CONSUMPTION, PRICE))) {
            try { ClarificationSchema.json(targets); throw new AssertionError("Invalid schema targets must fail locally"); }
            catch (IllegalArgumentException expected) { }
        }
    }
    public static void runAll() {
        valid(); wireContract(); boundCounts(); fieldPairing(); priorities(); badRequest(); authorization(); model(); quota(); server();
        timeout(); transport(); interrupted(); configuration(); providerEnvelope(); malformed(); structure(); targets(); wording();
        unknownFailure(); cachedState(); continuation(); cliDiagnostics(); schemaInput();
    }
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("contract")) { boundCounts(); fieldPairing(); priorities(); }
        else { ClarificationChecks.runAll(); runAll(); }
        System.out.println("PASS: focused F11 offline checks.");
    }
    private static String body(HttpRequest request) throws IOException, InterruptedException {
        var bytes = new ByteArrayOutputStream(); var done = new CountDownLatch(1);
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer buffer) { byte[] data = new byte[buffer.remaining()]; buffer.get(data); bytes.writeBytes(data); }
            public void onError(Throwable failure) { done.countDown(); }
            public void onComplete() { done.countDown(); }
        });
        if (!done.await(2, TimeUnit.SECONDS)) throw new IOException("Fixture body timeout");
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
