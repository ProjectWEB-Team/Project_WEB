package vn.iotstar.dto.hub;

public record MissingParcelCaseResponse(Long incidentId, Long shipmentId, Long tripId, Long hubId,
                                       String custodyReference, String resolutionOutcome) {
    // JavaBean accessors for the configured Jasper/EL 5 runtime.
    public Long getIncidentId() { return incidentId; }
    public Long getShipmentId() { return shipmentId; }
    public Long getTripId() { return tripId; }
    public Long getHubId() { return hubId; }
    public String getCustodyReference() { return custodyReference; }
    public String getResolutionOutcome() { return resolutionOutcome; }
}
