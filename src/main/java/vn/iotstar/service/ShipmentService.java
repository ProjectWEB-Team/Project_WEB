package vn.iotstar.service;

import vn.iotstar.dto.shipment.CreateShipmentRequest;
import vn.iotstar.dto.shipment.QuoteResponse;
import vn.iotstar.dto.shipment.ShipmentDetailResponse;
import org.springframework.data.domain.Page;
import vn.iotstar.dto.shipment.ShipmentSummaryResponse;
import vn.iotstar.entity.ShipmentStatus;
import java.math.BigDecimal;

public interface ShipmentService {
    /** @throws IllegalArgumentException with a user-safe message when the request cannot be priced or accepted */
    QuoteResponse quote(CreateShipmentRequest request);
    Long create(CreateShipmentRequest request);
    /** Reprice the reviewed draft and reject a changed fee before persistence. */
    Long create(CreateShipmentRequest request, BigDecimal acceptedFee);
    ShipmentDetailResponse getDetail(Long shipmentId);
    /** Demo online-payment confirmation; idempotent for the owner's own prepaid shipment. */
    void confirmPayment(Long shipmentId);
    /** Amount the owner must pay online for a shipment still waiting for payment (VNPay). */
    BigDecimal onlinePaymentAmount(Long shipmentId);
    /** Applies a signed gateway result; the caller has already verified the signature. Idempotent. */
    vn.iotstar.dto.shipment.GatewayPaymentResult confirmGatewayPayment(Long shipmentId, String gatewayRef, BigDecimal paidAmount);
    /** The current sender's own shipments, newest first; status null means all. */
    Page<ShipmentSummaryResponse> listMine(ShipmentStatus status, int page);
    /** Sender cancellation, allowed only in CREATED or WAITING_FOR_HANDOVER. */
    void cancel(Long shipmentId);
}
