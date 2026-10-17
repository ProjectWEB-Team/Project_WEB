package vn.iotstar.dto.error;

/** Intentionally bounded API error response that never includes exception internals. */
public record ApiErrorResponse(int status, String error) {
}
