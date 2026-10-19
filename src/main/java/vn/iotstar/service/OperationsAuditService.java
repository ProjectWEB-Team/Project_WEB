package vn.iotstar.service;

import vn.iotstar.dto.operations.OperationsAuditTimelineResponse;

/** Service boundary for minimal, custody-aware Phase-03 audit disclosure. */
public interface OperationsAuditService {
    OperationsAuditTimelineResponse getTimeline(Long shipmentId);
}
