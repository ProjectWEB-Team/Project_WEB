package vn.iotstar.dto.hub;

public record HandoverAcceptanceResponse(Long handoverId, int expectedContainers, int acceptedContainers, boolean accepted) {
    // JavaBean accessors for the configured Jasper/EL 5 runtime.
    public Long getHandoverId() { return handoverId; }
    public int getExpectedContainers() { return expectedContainers; }
    public int getAcceptedContainers() { return acceptedContainers; }
    public boolean getAccepted() { return accepted; }
}
