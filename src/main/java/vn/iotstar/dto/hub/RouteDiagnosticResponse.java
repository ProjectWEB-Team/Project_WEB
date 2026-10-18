package vn.iotstar.dto.hub;

/** Supervisor-only configuration diagnosis; it is never returned to hub staff commands. */
public record RouteDiagnosticResponse(String currentHubCode, String destinationGrouping, String diagnosticCode) { }
