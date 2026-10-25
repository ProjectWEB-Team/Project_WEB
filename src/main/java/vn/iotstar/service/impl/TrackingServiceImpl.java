package vn.iotstar.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.tracking.IncomingShipmentResponse;
import vn.iotstar.dto.tracking.PublicTrackingResponse;
import vn.iotstar.dto.tracking.RecipientShipmentResponse;
import vn.iotstar.dto.tracking.TrackingEventResponse;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.Shipment;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.exception.ShipmentAccessDeniedException;
import vn.iotstar.repository.HubRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.TrackingService;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TrackingServiceImpl implements TrackingService {
    /** Public labels only; raw incident/finance descriptions and unknown internal events are never shown. */
    private static final Map<String, String> PUBLIC_LABELS = Map.ofEntries(
            Map.entry("MISSING_PARCEL", "Đơn hàng đang được kiểm tra hành trình"),
            Map.entry("SHIPMENT_CREATED", "Đơn hàng đã được tạo"),
            Map.entry("PAYMENT_CONFIRMED", "Đã thanh toán cước"),
            Map.entry("PICKUP_TASK_ASSIGNED", "Đã phân công shipper lấy hàng"),
            Map.entry("PICKUP_ATTEMPT_FAILED", "Lấy hàng chưa thành công, sẽ liên hệ lại"),
            Map.entry("PICKED_UP", "Shipper đã lấy hàng"),
            Map.entry("SHIPMENT_CANCELLED", "Đơn hàng đã bị hủy"),
            Map.entry("ARRIVED_AT_HUB", "Kiện hàng đã đến bưu cục"),
            Map.entry("SORTING_COMPLETED", "Kiện hàng đã được phân loại"),
            Map.entry("OUTBOUND_SCANNED", "Kiện hàng đã sẵn sàng chuyển đi"),
            Map.entry("LINEHAUL_HANDOVER_ACCEPTED", "Đã bàn giao kiện hàng để vận chuyển"),
            Map.entry("LINEHAUL_TRIP_STARTED", "Kiện hàng đang được vận chuyển"),
            Map.entry("LINEHAUL_ARRIVED", "Kiện hàng đã đến điểm trung chuyển"),
            Map.entry("RETURN_DELIVERY_ASSIGNED", "Đã phân công giao trả người gửi"),
            Map.entry("RETURN_OUT_FOR_DELIVERY", "Shipper đang giao trả người gửi"),
            Map.entry("DELIVERY_ASSIGNED", "Đã phân công shipper giao hàng"),
            Map.entry("OUT_FOR_DELIVERY", "Shipper đang giao hàng"),
            Map.entry("DELIVERED", "Giao hàng thành công"),
            Map.entry("DELIVERY_FAILED", "Giao hàng chưa thành công"),
            Map.entry("DELIVERY_ATTEMPT_FAILED", "Giao hàng chưa thành công, sẽ liên hệ lại"),
            Map.entry("DELIVERY_RESCHEDULED", "Đã hẹn lại ngày giao hàng"),
            Map.entry("RETURNED_TO_HUB", "Kiện hàng được đưa về bưu cục"),
            Map.entry("RETURN_TO_SENDER", "Kiện hàng đang được hoàn về người gửi"),
            Map.entry("RETURNED_AT_ORIGIN_HUB", "Kiện hoàn đã về bưu cục gửi"),
            Map.entry("RETURNED_TO_SENDER", "Đã hoàn hàng cho người gửi"),
            Map.entry("RETURN_DISPOSED", "Hành trình hoàn hàng đã kết thúc"),
            Map.entry("COMPENSATION_CREDITED", "Hành trình đã kết thúc do sự cố"),
            Map.entry("SHIPPING_FEE_UPDATED", "Cước vận chuyển đã được cập nhật"));
    private static final int PAGE_SIZE = 10;

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository eventRepository;
    private final HubRepository hubRepository;

    public TrackingServiceImpl(ShipmentRepository shipmentRepository, ShipmentEventRepository eventRepository,
                               HubRepository hubRepository) {
        this.shipmentRepository = shipmentRepository;
        this.eventRepository = eventRepository;
        this.hubRepository = hubRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicTrackingResponse track(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber == null ? "" : trackingNumber.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment"));
        String hubName = shipment.getCurrentHubId() == null ? null
                : hubRepository.findById(shipment.getCurrentHubId()).map(Hub::getName).orElse(null);
        return new PublicTrackingResponse(shipment.getTrackingNumber(), shipment.getCurrentStatus(), hubName, timeline(shipment));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncomingShipmentResponse> incoming(int page) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        return shipmentRepository.findByReceiverIdOrderByCreatedAtDesc(actor.userId(), PageRequest.of(Math.max(page, 0), PAGE_SIZE))
                .map(s -> new IncomingShipmentResponse(s.getId(), maskTracking(s.getTrackingNumber()), maskName(s.getSenderName()),
                        s.getCurrentStatus(), s.getCreatedAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public RecipientShipmentResponse incomingDetail(Long shipmentId, boolean unlocked) {
        Shipment s = ownIncoming(shipmentId);
        if (!unlocked) {
            return new RecipientShipmentResponse(s.getId(), false, maskTracking(s.getTrackingNumber()), s.getCurrentStatus(),
                    maskName(s.getSenderName()), maskName(s.getReceiverName()), null, null, timeline(s));
        }
        BigDecimal toPay = "RECEIVER".equals(s.getFeePayer()) ? s.getShippingFee() : BigDecimal.ZERO;
        if (s.getCodAmount() != null) toPay = toPay.add(s.getCodAmount());
        return new RecipientShipmentResponse(s.getId(), true, s.getTrackingNumber(), s.getCurrentStatus(), s.getSenderName(),
                s.getReceiverName(), s.getReceiverAddress(), toPay, timeline(s));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean unlock(Long shipmentId, String trackingNumber) {
        Shipment s = ownIncoming(shipmentId);
        byte[] expected = s.getTrackingNumber().getBytes(StandardCharsets.UTF_8);
        byte[] given = Objects.toString(trackingNumber, "").trim().toUpperCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, given);
    }

    private Shipment ownIncoming(Long shipmentId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        Shipment shipment = shipmentRepository.findById(shipmentId).orElseThrow(() -> new ResourceNotFoundException("Shipment"));
        if (shipment.getReceiver() == null || !shipment.getReceiver().getId().equals(actor.userId())) {
            throw new ShipmentAccessDeniedException();
        }
        return shipment;
    }

    private List<TrackingEventResponse> timeline(Shipment shipment) {
        return eventRepository.findByShipmentIdOrderByCreatedAtAsc(shipment.getId()).stream()
                .filter(e -> PUBLIC_LABELS.containsKey(e.getEventType()))
                .map(e -> new TrackingEventResponse(PUBLIC_LABELS.get(e.getEventType()), e.getCreatedAt()))
                .toList();
    }

    static String maskTracking(String trackingNumber) {
        return trackingNumber.length() <= 4 ? "****" : "****" + trackingNumber.substring(trackingNumber.length() - 4);
    }

    static String maskName(String name) {
        return name == null || name.isBlank() ? "" : name.strip().charAt(0) + "***";
    }
}
