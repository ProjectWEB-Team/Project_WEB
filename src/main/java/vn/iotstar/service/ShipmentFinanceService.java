package vn.iotstar.service;

import vn.iotstar.entity.Payment;
import vn.iotstar.entity.Shipment;

import java.math.BigDecimal;
import java.util.List;

/**
 * Financial adjustments that move money into or out of the e-wallet for a
 * shipment. Every operation is reference-safe: the same payment or incident
 * can never be settled twice.
 */
public interface ShipmentFinanceService {

    /**
     * Refunds a paid payment back to the payer wallet and marks the payment as
     * refunded, as required when a prepaid shipment is cancelled.
     */
    Payment refundShipmentPayment(Long paymentId, String reason);

    /**
     * Credits the compensation the Workflow grants for a confirmed lost parcel
     * and closes the shipment with the canonical exception reason.
     */
    Shipment compensateLostShipment(Long shipmentId, Long incidentId, String reason);

    /**
     * Paid payments of cancelled shipments that still have no refund, so the
     * intake slice can settle the refund obligation it recorded.
     */
    List<Payment> listPendingRefundCandidates();

    /**
     * Total amount a lost shipment owes its sender: the declared value plus the
     * forward shipping fee when the sender already paid it.
     */
    BigDecimal calculateLostCompensation(Shipment shipment);
}