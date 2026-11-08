package ca.yorku.eecs3311.usedcar.llm;

/** Provider-independent failure metadata. Diagnostics never include raw exception/provider text. */
public final class LLMFailure extends IllegalStateException {
    public enum Kind {
        NOT_CONFIGURED, INVALID_CONFIGURATION, INVALID_REQUEST_SCHEMA, HTTP_ERROR, TIMEOUT, INTERRUPTED,
        TRANSPORT_ERROR, BLOCKED_RESPONSE, OUTPUT_LIMIT, INCOMPLETE_RESPONSE, INVALID_RESPONSE
    }
    /** Allowlisted summaries of provider error metadata; never raw server messages or field values. */
    public enum Rejection { UNSPECIFIED, SCHEMA_COMPLEXITY, SCHEMA_FIELD, RESPONSE_CONFIGURATION, INVALID_ARGUMENT, FAILED_PRECONDITION }
    private final Rejection rejection;
    private final Kind kind;
    private final int httpStatus;
    // Only adapters in this package supply the already-sanitized legacy exception message.
    LLMFailure(Kind kind, int httpStatus, String safeMessage) {
        this(kind,httpStatus,safeMessage,Rejection.UNSPECIFIED);
    }
    LLMFailure(Kind kind, int httpStatus, String safeMessage, Rejection rejection) {
        super(safeMessage);
        this.rejection = java.util.Objects.requireNonNull(rejection);
        this.kind = java.util.Objects.requireNonNull(kind);
        this.httpStatus = httpStatus;
    }
    public Rejection rejection() { return rejection; }
    public Kind kind() { return kind; }
    public int httpStatus() { return httpStatus; }
    public String diagnostic() {
        if (kind == Kind.HTTP_ERROR) {
            String reason = switch (httpStatus) {
                case 400 -> "Request rejected; request/schema compatibility must be checked. This does not prove a schema defect.";
                case 401, 403 -> "Authorization denied; check local key permissions.";
                case 404 -> "Model unavailable; check model identifier and account access.";
                case 429 -> "Quota or rate limit; check account limits before retrying.";
                default -> httpStatus >= 500 ? "Provider server error." : "Provider rejected the request.";
            };
            if (httpStatus == 400 && rejection != Rejection.UNSPECIFIED) {
                reason = "[" + rejection + "] " + switch (rejection) {
                    case SCHEMA_COMPLEXITY -> "Provider reports excessive schema/constraint complexity.";
                    case SCHEMA_FIELD -> "Provider identifies a response schema field as invalid.";
                    case RESPONSE_CONFIGURATION -> "Provider identifies structured-response configuration as invalid.";
                    case INVALID_ARGUMENT -> "Provider reports INVALID_ARGUMENT; no more specific safe reason was recognized.";
                    case FAILED_PRECONDITION -> "Provider reports FAILED_PRECONDITION; check account/service prerequisites locally.";
                    default -> throw new IllegalStateException("Unexpected rejection category.");
                };
            }
            return "[PROVIDER_HTTP_" + httpStatus + "] " + reason;
        }
        String reason = switch (kind) {
            case NOT_CONFIGURED -> "Set GEMINI_API_KEY in the launching terminal and restart the application.";
            case INVALID_CONFIGURATION -> "Local key header or model configuration is invalid; check CLI status without sharing credentials.";
            case INVALID_REQUEST_SCHEMA -> "Local structured-response schema is not a JSON object; no provider request was made.";
            case TIMEOUT -> "Provider request timed out.";
            case INTERRUPTED -> "Provider request was interrupted.";
            case TRANSPORT_ERROR -> "Network or transport failure before a usable HTTP response.";
            case BLOCKED_RESPONSE -> "Provider blocked the prompt or generated response.";
            case OUTPUT_LIMIT -> "Provider finishReason was MAX_TOKENS; incomplete JSON was rejected.";
            case INCOMPLETE_RESPONSE -> "Provider did not finish normally; incomplete output was rejected.";
            case INVALID_RESPONSE -> "Provider envelope was malformed, empty or missing usable generated text.";
            default -> throw new IllegalStateException("Unexpected failure kind.");
        };
        return "[" + kind + "] " + reason;
    }
}
