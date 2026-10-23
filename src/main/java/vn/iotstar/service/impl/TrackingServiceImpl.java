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
    /** Public timeline allowlist; any event type not listed here (incidents, finance, notes) is never shown. */
    private static final Map<String, String> PUBLIC_LABELS = Map.of(
            "SHIPMENT_CREATED", "Đơn hàng đã được tạo",
            "PAYMENT_CONFIRMED", "Đã thanh toán cước",
            "PICKUP_TASK_ASSIGNED", "Đã phân công shipper lấy hàng",
            "PICKUP_ATTEMPT_FAILED", "Lấy hàng chưa thành công, sẽ liên hệ lại",
            "PICKED_UP", "Shipper đã lấy hàng",
            "SHIPMENT_CANCELLED", "Đơn hàng đã bị hủy");
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
