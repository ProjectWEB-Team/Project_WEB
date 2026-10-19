package vn.iotstar.service;

import vn.iotstar.dto.hub.DestinationReceiptCommand;
import vn.iotstar.dto.hub.DestinationReceiptResponse;
import vn.iotstar.dto.hub.DestinationReconciliationCommand;

/** Service boundary for destination physical receipt and reconciliation. */
public interface DestinationReceiptService {
    DestinationReceiptResponse receiveItem(DestinationReceiptCommand command);
    void reconcileContainer(DestinationReconciliationCommand command);
}
