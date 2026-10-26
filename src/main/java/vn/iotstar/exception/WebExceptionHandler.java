package vn.iotstar.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.validation.BindException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Safe server-rendered error views for the JSP/session route family. */
@ControllerAdvice(basePackages = "vn.iotstar.controller")
@Order(Ordered.LOWEST_PRECEDENCE)
public class WebExceptionHandler {
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(BindException exception) {
        return "error/400";
    }

    @ExceptionHandler(ShipmentAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden(ShipmentAccessDeniedException exception) {
        return "error/403";
    }

    @ExceptionHandler(HubOperationAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String hubForbidden(HubOperationAccessDeniedException exception) {
        return "error/403";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(ResourceNotFoundException exception) {
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String unexpected(Exception exception) {
        return "error/500";
    }
}
