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
}
