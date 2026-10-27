package vn.iotstar.controller;

import org.springframework.security.access.AccessDeniedException;

/** Business-rule messages are safe to show; anything else (database or framework text) gets a generic message. */
final class FlashMessages {
    static final String GENERIC = "Không thể thực hiện thao tác. Vui lòng thử lại.";

    private FlashMessages() { }

    private static final java.util.Map<String, String> VI = load();

    static String of(Exception exception) {
        return exception instanceof IllegalArgumentException || exception instanceof IllegalStateException
                || exception instanceof AccessDeniedException ? text(exception) : GENERIC;
    }

    /** The exception message in Vietnamese when known (exact text, or a known prefix ending in ": " plus its detail). */
    static String text(Exception exception) {
        String message = exception.getMessage();
        if (message == null) return GENERIC;
        String exact = VI.get(message);
        if (exact != null) return exact;
        return VI.entrySet().stream().filter(e -> e.getKey().endsWith(": ") && message.startsWith(e.getKey()))
                .findFirst().map(e -> e.getValue().stripTrailing() + " " + message.substring(e.getKey().length())).orElse(message);
    }

    private static java.util.Map<String, String> load() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        try (var in = FlashMessages.class.getResourceAsStream("/error-messages-vi.tsv")) {
            if (in == null) return map;
            new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).lines()
                    .map(line -> line.split("	", 2)).filter(parts -> parts.length == 2).forEach(parts -> map.put(parts[0], parts[1]));
        } catch (java.io.IOException ignored) { }
        return map;
    }
}
