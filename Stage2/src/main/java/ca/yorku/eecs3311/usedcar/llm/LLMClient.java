package ca.yorku.eecs3311.usedcar.llm;

/** Provider-independent Stage 1 boundary; provider details remain inside adapters. */
public interface LLMClient {
    String generate(String prompt);
    String status();
}
