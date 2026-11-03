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
    private IllegalStateException failure(String reason) {
        state = "REQUEST FAILED: " + reason;
        return new IllegalStateException(safe(state)); // Never propagate provider bodies or exception causes.
    }
    @Override public String generate(String prompt) {
        String error = configurationError();
        if (error != null) throw new IllegalStateException(safe(error));
        if (prompt == null || prompt.isBlank()) throw new IllegalArgumentException("Prompt must not be blank.");
        String body = JSON.toJson(Map.of("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("maxOutputTokens", 512)));
        var request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"))
                .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json; charset=utf-8")
                .header("x-goog-api-key", key).POST(HttpRequest.BodyPublishers.ofString(body)).build();
        final Reply response;
        try { response = transport.send(request); }
        catch (HttpTimeoutException e) { throw failure("request timed out; retry manually if appropriate."); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw failure("request interrupted."); }
        catch (IOException | RuntimeException e) { throw failure("connection or transport error; check network access."); }
        if (response.status() != 200) {
            String explanation = switch (response.status()) {
                case 401, 403 -> "authorization denied; check key permissions.";
                case 429 -> "quota or rate limit; check billing/limits before retrying.";
                case 404 -> "model unavailable; check GEMINI_MODEL and account access.";
                default -> response.status() >= 500 ? "provider server error; retry manually later." : "request rejected.";
            };
            throw failure("HTTP " + response.status() + ": " + explanation);
        }
        final String result;
        try {
            if (response.body() == null || response.body().isBlank()) throw new IllegalArgumentException();
            JsonObject root = JSON.fromJson(response.body(), JsonObject.class);
            if (root.has("promptFeedback") && root.getAsJsonObject("promptFeedback").has("blockReason"))
                throw new IllegalArgumentException();
            JsonObject candidate = root.getAsJsonArray("candidates").get(0).getAsJsonObject();
            if (!"STOP".equals(candidate.get("finishReason").getAsString())) throw new IllegalArgumentException();
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
        } catch (RuntimeException e) { throw failure("invalid, empty, blocked or incomplete generated response."); }
        state = "VERIFIED: last generation succeeded in this session";
        return safe(result);
    }
}
