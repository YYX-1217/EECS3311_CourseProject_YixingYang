package ca.yorku.eecs3311.usedcar.llm;

/** Provider-independent Stage 1 boundary; no successful provider implementation yet. */
public interface LLMClient {
    String generate(String prompt);
    String status();
}
