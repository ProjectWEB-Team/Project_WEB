package vn.iotstar.controller;

import org.springframework.security.access.AccessDeniedException;

/** Business-rule messages are safe to show; anything else (database or framework text) gets a generic message. */
final class FlashMessages {
    static final String GENERIC = "Không thể thực hiện thao tác. Vui lòng thử lại.";

    private FlashMessages() { }

    static String of(Exception exception) {
        return exception instanceof IllegalArgumentException || exception instanceof IllegalStateException
                || exception instanceof AccessDeniedException ? exception.getMessage() : GENERIC;
    }
}
