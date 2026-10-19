package vn.iotstar.dto.realtime;

/** Safe, minimal operational notification delivered to a server-authorized private user queue. */
public record OperationalActionEventResponse(String type, String reference) { }
