package vn.iotstar.dto.hub;

/** Safe sorter response: only the current static next-hop decision is exposed. */
public record SortingDecisionResponse(String nextHubCode, String destinationGrouping, boolean containerReady, boolean reused) {
    // JavaBean accessors for the configured Jasper/EL 5 runtime.
    public String getNextHubCode() { return nextHubCode; }
    public String getDestinationGrouping() { return destinationGrouping; }
    public boolean getContainerReady() { return containerReady; }
    public boolean getReused() { return reused; }
}
