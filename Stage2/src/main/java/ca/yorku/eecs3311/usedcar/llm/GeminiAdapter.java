package ca.yorku.eecs3311.usedcar.llm;

/** Architectural foundation only. No credentials are read and no network code exists. */
public final class GeminiAdapter implements LLMClient {
    @Override public String status() {
        return "PLANNED: Gemini integration is not implemented/configured; deterministic operations need no API key.";
    }

    @Override public String generate(String prompt) {
        throw new UnsupportedOperationException(status());
    }
}
