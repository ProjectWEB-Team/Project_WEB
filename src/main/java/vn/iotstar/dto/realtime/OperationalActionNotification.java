package vn.iotstar.dto.realtime;

/** Internal application event; recipient authorization is added by the later realtime plan. */
public record OperationalActionNotification(String type, Long currentHubId, String diagnosticReference) { }
