package vn.iotstar.service.auth;

/** Contains only the existing private notification payload, never addresses or payment details. */
public record AccountEmailNotification(String username, String subject, String message) { }
