package vn.iotstar.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.delivery.*;
import vn.iotstar.dto.firstmile.ShipperOptionResponse;
import vn.iotstar.dto.realtime.ShipmentCreatedEventResponse;
import vn.iotstar.entity.*;
import vn.iotstar.repository.*;
import vn.iotstar.realtime.ShipmentEventPublisher;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class LastMileAssignmentService {
    private final LastMileAccess access;
    private final ShipmentRepository shipments;
    private final ShipmentAssignmentRepository assignments;
    private final ShipmentEventRepository events;
    private final EmployeeProfileRepository profiles;
    private final ShipmentEventPublisher publisher;
    public LastMileAssignmentService(LastMileAccess access, ShipmentRepository shipments,
            ShipmentAssignmentRepository assignments, ShipmentEventRepository events,
            EmployeeProfileRepository profiles, ShipmentEventPublisher publisher) {
        this.access=access; this.shipments=shipments; this.assignments=assignments;
        this.events=events; this.profiles=profiles; this.publisher=publisher;
    }
    @Transactional(readOnly=true)
    public List<DeliveryAssignmentCandidate> candidates() {
        var p=coordinator();
        return shipments.findAssignableLastMile(p.getHubId(), PageRequest.of(0,100)).stream()
                .filter(access::isForwardRetryReady)
                .map(s -> new DeliveryAssignmentCandidate(s.getId(),s.getTrackingNumber(),s.getDirection())).toList();
    }
    @Transactional(readOnly=true)
    public List<ShipperOptionResponse> shippers() {
        var p=coordinator();
        return profiles.findByHubIdAndStatusAndUserRole(p.getHubId(),"ACTIVE",UserRole.ROLE_SHIPPER).stream()
                .filter(x->x.getUser().isEnabled()).map(x->new ShipperOptionResponse(x.getUser().getId(),x.getUser().getFullName())).toList();
    }
    public void assign(DeliveryAssignmentRequest request) {
        var p=coordinator();
        Shipment s=shipments.findByIdForUpdate(request.shipmentId()).orElseThrow(()->new AccessDeniedException("Parcel unavailable"));
        access.requireAt(s,p);
        boolean forward="FORWARD".equals(s.getDirection()) && s.getCurrentStatus()==ShipmentStatus.AT_HUB
                && p.getHubId().equals(s.getDestinationHubId());
        boolean returning="RETURN".equals(s.getDirection()) && s.getCurrentStatus()==ShipmentStatus.RETURN_TO_SENDER
                && p.getHubId().equals(s.getOriginHubId()) && s.getReturnArrivedAtOriginAt()!=null;
        if ((!forward && !returning) || shipments.hasActiveContainerCustody(s.getId())) throw new AccessDeniedException("Parcel is not ready for local delivery");
        if(forward) access.requireForwardRetryReady(s);
        var shipper=access.shipperAt(request.shipperId(),p.getHubId());
        if (assignments.existsByShipmentIdAndStatusIn(s.getId(),List.of("ASSIGNED","ACCEPTED","IN_PROGRESS")))
            throw new IllegalStateException("Parcel already has an active task");
        assignments.save(ShipmentAssignment.create(s,shipper.getUser(),returning?"RETURN_DELIVERY":"DELIVERY"));
        String type=returning?"RETURN_DELIVERY_ASSIGNED":"DELIVERY_ASSIGNED";
        events.save(ShipmentEvent.of(s,p.getUser(),type,"Delivery task assigned"));
        publisher.publishToUserAfterCommit(shipper.getUser().getUsername(),
                new ShipmentCreatedEventResponse(s.getTrackingNumber(),s.getCurrentStatus(),type,LocalDateTime.now()));
    }
    private EmployeeProfile coordinator() { return access.require(UserRole.ROLE_HUB_STAFF,"COORDINATOR","GENERAL_STAFF"); }
}
