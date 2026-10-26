package vn.iotstar.service;

import vn.iotstar.dto.operations.ManagerOperationsDashboardResponse;

/** Service boundary for supervisor-only, hub-scoped operational aggregates. */
public interface ManagerOperationsService {
    ManagerOperationsDashboardResponse getDashboard();
    java.util.List<vn.iotstar.dto.operations.OperationalWorkItemResponse> getWorkItems();
    vn.iotstar.dto.operations.OperationalWorkItemResponse getWorkItem(String kind, Long id);
}
