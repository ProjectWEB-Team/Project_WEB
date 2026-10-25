package vn.iotstar.dto.operations;

/** Safe action reference; customer addresses, incident notes and financial data are never projected. */
public record OperationalWorkItemResponse(String kind, Long id, Long shipmentId, Long hubId, String summary) {
    // JavaBean accessors for the configured Jasper/EL 5 runtime.
    public String getKind() { return kind; }
    public Long getId() { return id; }
    public Long getShipmentId() { return shipmentId; }
    public Long getHubId() { return hubId; }
    public String getSummary() { return summary; }
}
