package vn.iotstar.dto.hub;

/** Safe sorter response: only the current static next-hop decision is exposed. */
public record SortingDecisionResponse(String nextHubCode, String destinationGrouping, boolean containerReady, boolean reused) { }
