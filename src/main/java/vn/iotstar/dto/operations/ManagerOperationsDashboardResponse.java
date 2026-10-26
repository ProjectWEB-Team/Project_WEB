package vn.iotstar.dto.operations;

/** Minimal, scoped Phase-03 supervisory projection; it intentionally contains no finance or customer data. */
public record ManagerOperationsDashboardResponse(
        String scopeLabel,
        boolean systemWide,
        long inboundCount,
        long outboundCount,
        long sortingBacklogCount,
        long activeHandoverCount,
        long activeTripCount,
        long blockedRouteCount,
        long actionableIncidentCount) {

    // JavaBean accessors for the configured Jasper/EL 5 runtime.
    public String getScopeLabel() { return scopeLabel; }
    public boolean getSystemWide() { return systemWide; }
    public long getInboundCount() { return inboundCount; }
    public long getOutboundCount() { return outboundCount; }
    public long getSortingBacklogCount() { return sortingBacklogCount; }
    public long getActiveHandoverCount() { return activeHandoverCount; }
    public long getActiveTripCount() { return activeTripCount; }
    public long getBlockedRouteCount() { return blockedRouteCount; }
    public long getActionableIncidentCount() { return actionableIncidentCount; }
}
