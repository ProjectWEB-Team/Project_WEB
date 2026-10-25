package vn.iotstar.service;

import vn.iotstar.dto.finance.ReconciliationSubmissionRequest;
import vn.iotstar.dto.finance.ShiftExpectedItemResponse;
import vn.iotstar.entity.ShipperReconciliation;
import vn.iotstar.entity.ShipperReconciliationItem;

import java.time.LocalDate;
import java.util.List;

public interface FinanceReconciliationService {

    ShipperReconciliation reconcile(ReconciliationSubmissionRequest request);

    /**
     * Pays out a reconciled COD transaction to the sender wallet, moving the
     * transaction from READY_FOR_PAYOUT to PAID_TO_SENDER.
     */
    ShipperReconciliationItem payoutReadyCod(Long codTransactionId);

    List<ShipperReconciliation> getReconciliationsByShipper(Long shipperId);

    List<ShipperReconciliationItem> getReconciliationItems(Long reconciliationId);

    /**
     * Money items the shipper is expected to hand in for the given shift, so the
     * manager reconciles against system data instead of typed-in amounts.
     */
    List<ShiftExpectedItemResponse> getShiftExpectedItems(Long shipperId, LocalDate reconciliationDate);
}

