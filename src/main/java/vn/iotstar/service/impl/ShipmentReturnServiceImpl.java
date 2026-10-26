package vn.iotstar.service.impl;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.config.LogisticsProperties;
import vn.iotstar.entity.*;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.repository.*;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.ShipmentReturnService;
import java.time.*;
import java.util.List;

/** Immutable physical-origin arrival starts grace; only a scoped manager decides disposal. */
@Service
@Transactional
public class ShipmentReturnServiceImpl implements ShipmentReturnService {
    public static final String EVENT_RETURNED_AT_ORIGIN_HUB="RETURNED_AT_ORIGIN_HUB";
    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final UserRepository userRepository;
    private final LogisticsProperties logisticsProperties;
    private final ShipmentAssignmentRepository assignments;
    private final ManagerOperationsRepository managers;
    private final LastMileAccess access;
    private final Clock clock;
    public ShipmentReturnServiceImpl(ShipmentRepository shipmentRepository,ShipmentEventRepository shipmentEventRepository,
            UserRepository userRepository,LogisticsProperties logisticsProperties,ShipmentAssignmentRepository assignments,
            ManagerOperationsRepository managers,LastMileAccess access,Clock clock) {
        this.shipmentRepository=shipmentRepository;this.shipmentEventRepository=shipmentEventRepository;
        this.userRepository=userRepository;this.logisticsProperties=logisticsProperties;this.assignments=assignments;
        this.managers=managers;this.access=access;this.clock=clock;
    }
    @Override public Shipment markReturnArrivedAtOriginHub(Long shipmentId) {
        var actor=AuthenticatedActor.requireCurrent();var s=locked(shipmentId);
        if(actor.hasRole(UserRole.ROLE_HUB_STAFF)) access.requireAt(s,access.require(UserRole.ROLE_HUB_STAFF,"INBOUND","GENERAL_STAFF"));
        else requireManagerScope(s);
        if(!atOrigin(s) || s.getReturnArrivedAtOriginAt()==null) throw new IllegalStateException("Actual origin receipt is required");
        if(!shipmentEventRepository.existsByShipmentIdAndEventType(s.getId(),EVENT_RETURNED_AT_ORIGIN_HUB))
            shipmentEventRepository.save(ShipmentEvent.of(s,userRepository.getReferenceById(actor.userId()),EVENT_RETURNED_AT_ORIGIN_HUB,"Returned parcel received at origin"));
        return s;
    }
    @Override @Transactional(readOnly=true)
    public List<Shipment> findOverdueReturnedShipments(LocalDateTime referenceTime) {
        return shipmentRepository.findByCurrentStatus(ShipmentStatus.RETURN_TO_SENDER).stream()
                .filter(s->eligible(s,referenceTime)).toList();
    }
    @Transactional(readOnly=true)
    public List<ReturnDisposalCandidate> disposalCandidates() {
        var ids=managerScope();
        return findOverdueReturnedShipments(ServerTime.now(clock)).stream().filter(s->ids.contains(s.getOriginHubId()))
                .limit(100).map(s->new ReturnDisposalCandidate(s.getId(),s.getTrackingNumber(),s.getReturnArrivedAtOriginAt())).toList();
    }
    @Override public Shipment disposeOverdueReturn(Long shipmentId,String reason) {
        var s=locked(shipmentId); requireManagerScope(s);
        if(s.getCurrentStatus()==ShipmentStatus.CLOSED_EXCEPTION && "DISPOSED".equals(s.getClosureReason())) return s;
        if(!eligible(s,ServerTime.now(clock))) throw new IllegalStateException("Return is not overdue available origin inventory");
        if(reason==null || reason.isBlank() || reason.length()>255) throw new IllegalArgumentException("A bounded disposal reason is required");
        s.transitionTo(ShipmentStatus.CLOSED_EXCEPTION);s.setClosureReason("DISPOSED");
        shipmentEventRepository.save(ShipmentEvent.of(s,userRepository.getReferenceById(AuthenticatedActor.requireCurrent().userId()),"RETURN_DISPOSED",reason));
        return s;
    }
    private boolean eligible(Shipment s,LocalDateTime now) {
        return atOrigin(s) && !s.isOnHold() && s.getReturnArrivedAtOriginAt()!=null
                && !s.getReturnArrivedAtOriginAt().plus(gracePeriod()).isAfter(now)
                && !shipmentRepository.hasActiveContainerCustody(s.getId())
                && !assignments.existsByShipmentIdAndStatusIn(s.getId(),List.of("ASSIGNED","ACCEPTED","IN_PROGRESS"));
    }
    private boolean atOrigin(Shipment s) {
        return "RETURN".equals(s.getDirection()) && s.getCurrentStatus()==ShipmentStatus.RETURN_TO_SENDER
                && s.getOriginHubId()!=null && s.getOriginHubId().equals(s.getCurrentHubId());
    }
    private void requireManagerScope(Shipment s) {
        if(s.getOriginHubId()==null || !managerScope().contains(s.getOriginHubId())) throw new AccessDeniedException("Return is outside manager scope");
    }
    private List<Long> managerScope() {
        var actor=AuthenticatedActor.requireCurrent();
        if(actor.hasRole(UserRole.ROLE_ADMIN)) return managers.findAllActiveHubIds();
        if(!actor.hasRole(UserRole.ROLE_MANAGER)) throw new AccessDeniedException("Manager required");
        var profile=managers.findActiveProfileByUserId(actor.userId()).orElseThrow(()->new AccessDeniedException("Active manager hub required"));
        return managers.findHubSubtreeIds(profile.getHub().getId());
    }
    private Shipment locked(Long id){return shipmentRepository.findByIdForUpdate(id).orElseThrow(()->new ResourceNotFoundException("Shipment"));}
    private Duration gracePeriod(){
        int minutes=logisticsProperties.getTimeout().getDisposedGraceMinutes();
        int days=logisticsProperties.getTimeout().getDisposedGraceDays();
        if(minutes<0 || days<=0) throw new IllegalStateException("Invalid return grace policy");
        return minutes>0?Duration.ofMinutes(minutes):Duration.ofDays(days);
    }
    public record ReturnDisposalCandidate(Long shipmentId,String trackingNumber,LocalDateTime arrivedAt) { }
}
