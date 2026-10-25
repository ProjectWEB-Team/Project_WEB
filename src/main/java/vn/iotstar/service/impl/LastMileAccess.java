package vn.iotstar.service.impl;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import vn.iotstar.entity.*;
import vn.iotstar.repository.EmployeeProfileRepository;
import vn.iotstar.security.AuthenticatedActor;
import java.util.Set;

/** Server-owned active employee and physical hub checks shared by dispatch and collection. */
@Component
public class LastMileAccess {
    private final EmployeeProfileRepository profiles;
    private final vn.iotstar.repository.LastMileDeliveryRepository attempts;
    private final java.time.Clock clock;
    public LastMileAccess(EmployeeProfileRepository profiles,vn.iotstar.repository.LastMileDeliveryRepository attempts,java.time.Clock clock) {
        this.profiles = profiles;this.attempts=attempts;this.clock=clock;
    }
    public java.time.LocalDate today(){return ServerTime.now(clock).toLocalDate();}
    public boolean isForwardRetryReady(Shipment s) {
        if(!"FORWARD".equals(s.getDirection())) return true;
        return attempts.findTopByShipmentIdOrderByAttemptNumberDesc(s.getId()).map(a->{
            if(a.getAttemptDate()==null || "DELIVERED".equals(a.getResult())) return false;
            var earliest=a.getAttemptDate().plusDays(1);
            if("RESCHEDULED".equals(a.getResult())) {
                if(a.getRescheduledDate()==null) return false;
                if(a.getRescheduledDate().isAfter(earliest)) earliest=a.getRescheduledDate();
            }
            return !today().isBefore(earliest);
        }).orElse(true);
    }
    public void requireForwardRetryReady(Shipment s) {
        if(!isForwardRetryReady(s)) throw new IllegalStateException("Parcel is not yet due for another delivery attempt");
    }
    public EmployeeProfile require(UserRole role, String... positions) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(role)) throw new AccessDeniedException("Operational role required");
        EmployeeProfile p = profiles.findFirstByUserId(actor.userId())
                .filter(x -> x.isActive() && x.getUser().isEnabled() && x.getUser().getRole() == role
                        && x.getHub() != null && "ACTIVE".equals(x.getHub().getStatus()))
                .orElseThrow(() -> new AccessDeniedException("Active hub assignment required"));
        if (positions.length > 0 && !Set.of(positions).contains(p.getPosition()))
            throw new AccessDeniedException("Operational position required");
        return p;
    }
    public void requireAt(Shipment shipment, EmployeeProfile profile) {
        if (shipment.isOnHold() || shipment.getCurrentHubId() == null
                || !shipment.getCurrentHubId().equals(profile.getHubId()))
            throw new AccessDeniedException("Parcel is not in this hub's available inventory");
    }
    public EmployeeProfile shipperAt(Long userId, Long hubId) {
        return profiles.findFirstByUserId(userId).filter(p -> p.isActive() && p.getUser().isEnabled()
                && p.getUser().getRole() == UserRole.ROLE_SHIPPER && hubId.equals(p.getHubId()))
                .orElseThrow(() -> new AccessDeniedException("Shipper is not active at this hub"));
    }
}
