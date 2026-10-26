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

    List<vn.iotstar.dto.finance.ReconciliationSummaryResponse> getReconciliationsByShipper(Long shipperId);

    List<vn.iotstar.dto.finance.ReconciliationItemResponse> getReconciliationItems(Long reconciliationId);

    List<ShiftExpectedItemResponse> getReadyPayouts(Long shipperId);

    /**
     * Money items the shipper is expected to hand in for the given shift, so the
     * manager reconciles against system data instead of typed-in amounts.
     */
    List<ShiftExpectedItemResponse> getShiftExpectedItems(Long shipperId, LocalDate reconciliationDate);
    List<ShiftExpectedItemResponse> getShiftExpectedItems(Long shipperId, LocalDate reconciliationDate, String collectionMethod);
}

