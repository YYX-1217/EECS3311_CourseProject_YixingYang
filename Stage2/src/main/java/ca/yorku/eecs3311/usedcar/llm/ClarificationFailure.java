package ca.yorku.eecs3311.usedcar.llm;

/** F11 diagnostics contain only fixed rules and locally constructed paths, never response values. */
public final class ClarificationFailure extends IllegalArgumentException {
    public enum Kind { RESPONSE_PARSE_ERROR, RESPONSE_SCHEMA_ERROR, CLARIFICATION_VALIDATION_ERROR }
    private final Kind kind;
    ClarificationFailure(Kind kind, String path, String rule) {
        super("[" + kind + "] " + path + ": " + rule);
        this.kind = kind;
    }
    public Kind kind() { return kind; }
    public String diagnostic() { return getMessage(); }
}
