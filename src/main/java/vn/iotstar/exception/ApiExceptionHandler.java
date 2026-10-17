package vn.iotstar.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.iotstar.dto.error.ApiErrorResponse;

/** Safe JSON exception translation for REST controllers. */
@RestControllerAdvice(basePackages = "vn.iotstar.controller.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> invalidRequest(MethodArgumentNotValidException exception) {
        return response(HttpStatus.BAD_REQUEST, "Invalid request");
    }

    @ExceptionHandler(ShipmentAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> forbidden(ShipmentAccessDeniedException exception) {
        return response(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> forbidden(AccessDeniedException exception) {
        return response(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> unexpected(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String error) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(status.value(), error));
    }
}
