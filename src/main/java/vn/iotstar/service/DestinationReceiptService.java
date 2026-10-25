package vn.iotstar.service;

import vn.iotstar.dto.hub.DestinationReceiptCommand;
import vn.iotstar.dto.hub.DestinationReceiptResponse;
import vn.iotstar.dto.hub.DestinationReconciliationCommand;

/** Service boundary for destination physical receipt and reconciliation. */
public interface DestinationReceiptService {
    DestinationReceiptResponse receiveItem(DestinationReceiptCommand command);
    void reconcileContainer(DestinationReconciliationCommand command);
    void resolveMissing(vn.iotstar.dto.hub.MissingParcelResolutionCommand command);
    java.util.List<vn.iotstar.dto.hub.MissingParcelCaseResponse> missingCases();
    vn.iotstar.dto.hub.MissingParcelCaseResponse missingCase(Long incidentId);
}
