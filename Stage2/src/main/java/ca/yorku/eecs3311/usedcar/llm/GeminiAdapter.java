package ca.yorku.eecs3311.usedcar.llm;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Adapts Gemini generateContent HTTP/JSON to the Stage 1 text interface. */
public final class GeminiAdapter implements LLMClient {
    public static final String DEFAULT_MODEL = "gemini-3.1-flash-lite";
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private final String key;
    private final String model;
    private final Transport transport;
    private volatile String state = "CONFIGURED: not live-verified";
    @FunctionalInterface interface Transport {
        Reply send(HttpRequest request) throws IOException, InterruptedException;
    }
    record Reply(int status, String body) { }
    public GeminiAdapter() { this(System.getenv("GEMINI_API_KEY"), System.getenv("GEMINI_MODEL")); }
    public GeminiAdapter(String key, String model) { this(key, model, realTransport()); }
    GeminiAdapter(String key, String model, Transport transport) {
        // Preserve input so control characters cannot disappear before validation.
        this.key = key == null ? "" : key;
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.strip();
        this.transport = Objects.requireNonNull(transport);
    }
    private static Transport realTransport() {
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        return request -> {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            return new Reply(response.statusCode(), response.body());
        };
    }
    private String configurationError() {
        if (key.isBlank() || Set.of("YOUR_API_KEY_HERE", "YOUR_API_KEY", "YOUR_GEMINI_API_KEY",
                "YOUR_GEMINI_API_KEY_HERE", "REPLACE_WITH_YOUR_API_KEY", "[YOUR_API_KEY_HERE]")
                .contains(key.strip().toUpperCase(Locale.ROOT))) return "NOT CONFIGURED: set GEMINI_API_KEY locally.";
        // Treat keys as opaque credentials: no prefix, length or punctuation assumptions.
        // Reject whitespace, controls and non-ASCII values before HttpRequest can expose them in an error.
        if (key.chars().anyMatch(c -> c < 0x21 || c > 0x7e))
            return "CONFIGURATION ERROR: API key contains whitespace or unsafe header characters.";
        if (!model.matches("gemini-[a-z0-9][a-z0-9.-]*")) return "CONFIGURATION ERROR: invalid GEMINI_MODEL identifier.";
        return null;
    }
    private String safe(String text) { return key.isEmpty() ? text : text.replace(key, "[REDACTED]"); }
    @Override public String status() {
        String error = configurationError();
        return safe(error == null ? "Gemini model=" + model + "; " + state : error);
    }
    private LLMFailure failure(LLMFailure.Kind kind, int httpStatus, String reason) {
        state = "REQUEST FAILED: " + reason;
        return new LLMFailure(kind, httpStatus, safe(state)); // Never propagate provider bodies or exception causes.
    }
    @Override public String generate(String prompt) { return generateRequest(prompt, null); }
    @Override public String generateStructured(String prompt, String jsonSchema) {
        if (jsonSchema == null || jsonSchema.isBlank()) throw new LLMFailure(LLMFailure.Kind.INVALID_REQUEST_SCHEMA, 0, "JSON schema is required.");
        return generateRequest(prompt, jsonSchema);
    }
    private String generateRequest(String prompt, String jsonSchema) {
        String error = configurationError();
        if (error != null) throw new LLMFailure(error.startsWith("NOT CONFIGURED") ? LLMFailure.Kind.NOT_CONFIGURED : LLMFailure.Kind.INVALID_CONFIGURATION, 0, safe(error));
        if (prompt == null || prompt.isBlank()) throw new IllegalArgumentException("Prompt must not be blank.");
        var config = new LinkedHashMap<String,Object>(); config.put("maxOutputTokens",4096);
        if (jsonSchema != null) {
            final JsonObject schema;
            try { schema = JSON.fromJson(jsonSchema, JsonObject.class); }
            catch (RuntimeException e) { throw new LLMFailure(LLMFailure.Kind.INVALID_REQUEST_SCHEMA, 0, "Invalid structured response schema."); }
            if (schema == null) throw new LLMFailure(LLMFailure.Kind.INVALID_REQUEST_SCHEMA, 0, "JSON object schema is required.");
            config.put("responseMimeType", "application/json");
            config.put("responseJsonSchema", schema);
        }
        String body = JSON.toJson(Map.of("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", config));
        var request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"))
                .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json; charset=utf-8")
                .header("x-goog-api-key", key).POST(HttpRequest.BodyPublishers.ofString(body)).build();
        final Reply response;
        try { response = transport.send(request); }
        catch (HttpTimeoutException e) { throw failure(LLMFailure.Kind.TIMEOUT, 0, "request timed out; retry manually if appropriate."); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw failure(LLMFailure.Kind.INTERRUPTED, 0, "request interrupted."); }
        catch (IOException | RuntimeException e) { throw failure(LLMFailure.Kind.TRANSPORT_ERROR, 0, "connection or transport error; check network access."); }
        if (response.status() != 200) {
            String explanation = switch (response.status()) {
                case 401, 403 -> "authorization denied; check key permissions.";
                case 429 -> "quota or rate limit; check billing/limits before retrying.";
                case 404 -> "model unavailable; check GEMINI_MODEL and account access.";
                default -> response.status() >= 500 ? "provider server error; retry manually later." : "request rejected.";
            };
            var rejection = response.status() == 400 ? classifyRejection(response.body()) : LLMFailure.Rejection.UNSPECIFIED;
            state = "REQUEST FAILED: HTTP " + response.status() + ": " + explanation
                    + (rejection == LLMFailure.Rejection.UNSPECIFIED ? "" : " [" + rejection + "]");
            throw new LLMFailure(LLMFailure.Kind.HTTP_ERROR, response.status(), safe(state), rejection);
        }
        final String result;
        try {
            if (response.body() == null || response.body().isBlank()) throw new IllegalArgumentException();
            JsonObject root = JSON.fromJson(response.body(), JsonObject.class);
            if (root.has("promptFeedback") && root.getAsJsonObject("promptFeedback").has("blockReason"))
                throw failure(LLMFailure.Kind.BLOCKED_RESPONSE, 0, "invalid, empty, blocked or incomplete generated response (provider block).");
            JsonObject candidate = root.getAsJsonArray("candidates").get(0).getAsJsonObject();
            String finish = candidate.get("finishReason").getAsString();
            if (!"STOP".equals(finish)) {
                var kind = "MAX_TOKENS".equals(finish) ? LLMFailure.Kind.OUTPUT_LIMIT
                        : Set.of("SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "IMAGE_SAFETY").contains(finish)
                        ? LLMFailure.Kind.BLOCKED_RESPONSE : LLMFailure.Kind.INCOMPLETE_RESPONSE;
                throw failure(kind, 0, "invalid, empty, blocked or incomplete generated response (" + kind.name() + ").");
            }
            var text = new StringBuilder();
            for (JsonElement item : candidate.getAsJsonObject("content").getAsJsonArray("parts")) {
                JsonObject part = item.getAsJsonObject();
                if (part.has("thought") && part.get("thought").getAsBoolean()) continue;
                if (part.has("text")) {
                    if (!part.get("text").isJsonPrimitive() || !part.getAsJsonPrimitive("text").isString()) throw new IllegalArgumentException();
                    text.append(part.get("text").getAsString());
                }
            }
            result = text.toString();
            if (result.isBlank()) throw new IllegalArgumentException();
        } catch (LLMFailure e) { throw e; }
        catch (RuntimeException e) { throw failure(LLMFailure.Kind.INVALID_RESPONSE, 0, "invalid, empty, blocked or incomplete generated response."); }
        state = "VERIFIED: last generation succeeded in this session";
        return safe(result);
    }
    /** Read only bounded error metadata; emit fixed categories, never server text, keys or request fragments. */
    private static LLMFailure.Rejection classifyRejection(String body) {
        if (body == null || body.length() > 65536) return LLMFailure.Rejection.UNSPECIFIED;
        try {
            var error = JSON.fromJson(body,JsonObject.class).getAsJsonObject("error");
            String status = errorText(error,"status");
            if (status.equals("FAILED_PRECONDITION")) return LLMFailure.Rejection.FAILED_PRECONDITION;
            if (!status.equals("INVALID_ARGUMENT")) return LLMFailure.Rejection.UNSPECIFIED;
            String message = errorText(error,"message").toLowerCase(Locale.ROOT);
            if (message.contains("too many states") || (message.contains("schema")
                    && (message.contains("too complex") || message.contains("too large") || message.contains("complexity"))))
                return LLMFailure.Rejection.SCHEMA_COMPLEXITY;
            if (error.has("details") && error.get("details").isJsonArray()) {
                for (var detail : error.getAsJsonArray("details")) {
                    if (!detail.isJsonObject()) continue;
                    var item = detail.getAsJsonObject();
                    if (!errorText(item,"@type").equals("type.googleapis.com/google.rpc.BadRequest")) continue;
                    if (!item.has("fieldViolations") || !item.get("fieldViolations").isJsonArray()) continue;
                    for (var violation : item.getAsJsonArray("fieldViolations")) {
                        if (!violation.isJsonObject()) continue;
                        String field = errorText(violation.getAsJsonObject(),"field").replace("_","").toLowerCase(Locale.ROOT);
                        for (String schema : List.of("generationconfig.responsejsonschema","generationconfig.responseschema"))
                            if (field.equals(schema) || field.startsWith(schema+".") || field.startsWith(schema+"["))
                                return LLMFailure.Rejection.SCHEMA_FIELD;
                        if (Set.of("generationconfig.responsemimetype","generationconfig.responseformat").contains(field))
                            return LLMFailure.Rejection.RESPONSE_CONFIGURATION;
                    }
                }
            }
            if (message.contains("response_json_schema") || message.contains("responsejsonschema") || message.contains("response_schema"))
                return LLMFailure.Rejection.SCHEMA_FIELD;
            if (message.contains("response_mime_type") || message.contains("responsemimetype") || message.contains("response_format"))
                return LLMFailure.Rejection.RESPONSE_CONFIGURATION;
            return LLMFailure.Rejection.INVALID_ARGUMENT;
        } catch (RuntimeException ignored) { return LLMFailure.Rejection.UNSPECIFIED; }
    }
    private static String errorText(JsonObject object,String name) {
        var value=object.get(name);
        return value!=null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : "";
    }

}
