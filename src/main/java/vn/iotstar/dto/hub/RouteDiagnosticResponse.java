package vn.iotstar.dto.hub;

/** Supervisor-only configuration diagnosis; it is never returned to hub staff commands. */
public record RouteDiagnosticResponse(String currentHubCode, String destinationGrouping, String diagnosticCode) {
    // JavaBean accessors for the configured Jasper/EL 5 runtime.
    public String getCurrentHubCode() { return currentHubCode; }
    public String getDestinationGrouping() { return destinationGrouping; }
    public String getDiagnosticCode() { return diagnosticCode; }
}
