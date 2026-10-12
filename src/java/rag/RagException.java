package rag;
/** Error codes are safe to store/log. Never store provider body, document content or key. */
public final class RagException extends Exception {
    public enum Code { CONFIGURATION, RATE_LIMIT, TIMEOUT, PROVIDER, INVALID_VECTOR, INVALID_DOCUMENT, STALE_VERSION, DATABASE, BUDGET_EXCEEDED, EXECUTION_BLOCKED }
    public final Code code;
    public RagException(Code code) { super(code.name()); this.code=code; }
}
