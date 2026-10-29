package vn.iotstar.service;

import vn.iotstar.dto.operations.ManagerOperationsDashboardResponse;

/** Service boundary for supervisor-only, hub-scoped operational aggregates. */
public interface ManagerOperationsService {
    ManagerOperationsDashboardResponse getDashboard();
    java.util.List<vn.iotstar.dto.operations.OperationalWorkItemResponse> getWorkItems();
    vn.iotstar.dto.operations.OperationalWorkItemResponse getWorkItem(String kind, Long id);
    /** Route, driver, trip, containers and shipments of one handover in the supervisor's scope. */
    java.util.Map<String, Object> getHandoverDetail(Long id);
    /** Cancels a not-yet-accepted handover and recreates it for another driver; returns the new handover id. */
    Long reassignHandoverDriver(Long handoverId, Long driverUserId);
}
