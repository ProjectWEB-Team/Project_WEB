package vn.iotstar.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.operations.AuditCustodyReference;
import vn.iotstar.dto.operations.OperationsAuditTimelineEntry;
import vn.iotstar.dto.operations.OperationsAuditTimelineResponse;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.HubOperationAccessDeniedException;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.repository.OperationsAuditRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.OperationsAuditService;

import java.util.List;

/** D-15 disclosure boundary: authorize the current shipment location before returning immutable operational facts. */
@Service
public class OperationsAuditServiceImpl implements OperationsAuditService {
    private final OperationsAuditRepository operationsAuditRepository;

    public OperationsAuditServiceImpl(OperationsAuditRepository operationsAuditRepository) {
        this.operationsAuditRepository = operationsAuditRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OperationsAuditTimelineResponse getTimeline(Long shipmentId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!isOperationalRole(actor)) {
            throw new HubOperationAccessDeniedException();
        }
        Shipment shipment = operationsAuditRepository.findShipmentWithCurrentHub(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment"));
        List<AuditCustodyReference> references = custodyReferencesFor(actor, shipmentId);
        if (!mayReadShipment(actor, shipment.getCurrentHub(), references)) {
            // Safe not-found behavior avoids turning this read surface into a shipment/custody oracle.
            throw new ResourceNotFoundException("Shipment");
        }
        AuditCustodyReference reference = references.isEmpty() ? new AuditCustodyReference(null, null, null, null) : references.getFirst();
        List<OperationsAuditTimelineEntry> entries = operationsAuditRepository.findEventsForShipment(shipmentId).stream()
                .map(event -> entry(event, reference)).toList();
        return new OperationsAuditTimelineResponse(shipment.getId(), entries);
    }

    private List<AuditCustodyReference> custodyReferencesFor(AuthenticatedActor actor, Long shipmentId) {
        if (actor.hasRole(UserRole.ROLE_LINEHAUL_DRIVER)) {
            return operationsAuditRepository.findActiveCustodyReferencesForDriver(shipmentId, actor.userId());
        }
        return operationsAuditRepository.findCustodyReferences(shipmentId);
    }

    private boolean mayReadShipment(AuthenticatedActor actor, Hub currentHub, List<AuditCustodyReference> references) {
        if (actor.hasRole(UserRole.ROLE_ADMIN)) return true;
        if (actor.hasRole(UserRole.ROLE_LINEHAUL_DRIVER)) return !references.isEmpty();
        if (currentHub == null) return false;
        EmployeeProfile profile = operationsAuditRepository.findActiveProfileByUserId(actor.userId()).orElse(null);
        if (profile == null) return false;
        if (actor.hasRole(UserRole.ROLE_MANAGER)) {
            return operationsAuditRepository.isHubWithinSubtree(profile.getHub().getId(), currentHub.getId());
        }
        return actor.hasRole(UserRole.ROLE_HUB_STAFF) && profile.getHub().getId().equals(currentHub.getId());
    }

    private boolean isOperationalRole(AuthenticatedActor actor) {
        return actor.hasRole(UserRole.ROLE_ADMIN) || actor.hasRole(UserRole.ROLE_MANAGER)
                || actor.hasRole(UserRole.ROLE_HUB_STAFF) || actor.hasRole(UserRole.ROLE_LINEHAUL_DRIVER);
    }

    private OperationsAuditTimelineEntry entry(ShipmentEvent event, AuditCustodyReference reference) {
        return new OperationsAuditTimelineEntry(event.getEventType(), event.getCreatedAt(),
                event.getHub() == null ? null : event.getHub().getCode(), event.getActor() == null ? "SYSTEM" : event.getActor().getRole().name(),
                reference.containerId(), reference.handoverId(), reference.tripId());
    }
}
