package vn.iotstar.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.shipment.CreateShipmentRequest;
import vn.iotstar.dto.shipment.ShipmentDetailResponse;
import vn.iotstar.dto.shipment.ShipmentEventResponse;
import vn.iotstar.dto.realtime.ShipmentCreatedEventResponse;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.User;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.exception.ShipmentAccessDeniedException;
import vn.iotstar.realtime.ShipmentEventPublisher;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.ShipmentService;

import java.util.List;
import java.util.UUID;

/** Service boundary for the Phase-01 tracer's transaction and ownership policy. */
@Service
public class ShipmentServiceImpl implements ShipmentService {
    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final UserRepository userRepository;
    private final ShipmentEventPublisher shipmentEventPublisher;

    public ShipmentServiceImpl(ShipmentRepository shipmentRepository, ShipmentEventRepository shipmentEventRepository,
                               UserRepository userRepository, ShipmentEventPublisher shipmentEventPublisher) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.userRepository = userRepository;
        this.shipmentEventPublisher = shipmentEventPublisher;
    }

    @Override
    @Transactional
    public Long create(CreateShipmentRequest request) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        User sender = userRepository.getReferenceById(actor.userId());
        Shipment shipment = Shipment.create(newTrackingNumber(), sender, request.getSenderName(), request.getSenderPhone(),
                request.getSenderAddress(), request.getReceiverName(), request.getReceiverPhone(), request.getReceiverAddress());
        Shipment savedShipment = shipmentRepository.save(shipment);
        ShipmentEvent savedEvent = shipmentEventRepository.save(ShipmentEvent.shipmentCreated(savedShipment, sender));
        shipmentEventPublisher.publishAfterCommit(actor, new ShipmentCreatedEventResponse(savedShipment.getTrackingNumber(),
                savedShipment.getCurrentStatus(), savedEvent.getEventType(), savedEvent.getCreatedAt()));
        return savedShipment.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentDetailResponse getDetail(Long shipmentId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment"));
        if (!shipment.getSender().getId().equals(actor.userId())) {
            throw new ShipmentAccessDeniedException();
        }
        List<ShipmentEventResponse> events = shipmentEventRepository.findByShipmentIdOrderByCreatedAtAsc(shipmentId).stream()
                .map(event -> new ShipmentEventResponse(event.getEventType(), event.getDescription(), event.getCreatedAt()))
                .toList();
        return new ShipmentDetailResponse(shipment.getId(), shipment.getTrackingNumber(), shipment.getCurrentStatus(),
                shipment.getSenderName(), shipment.getSenderPhone(), shipment.getSenderAddress(), shipment.getReceiverName(),
                shipment.getReceiverPhone(), shipment.getReceiverAddress(), shipment.getCreatedAt(), events);
    }

    private String newTrackingNumber() {
        return "UTX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
