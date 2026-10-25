package vn.iotstar.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.ShipmentStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Read-only SQL Server projections. Scope is selected by the service from a server-derived profile. */
@Repository
public class ManagerOperationsRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public Optional<EmployeeProfile> findActiveProfileByUserId(Long userId) {
        return entityManager.createQuery("select p from EmployeeProfile p join fetch p.hub where p.user.id = :userId and p.status = 'ACTIVE'", EmployeeProfile.class)
                .setParameter("userId", userId).getResultStream().findFirst();
    }

    /** SQL Server recursive CTE with a path guard so corrupt cycles cannot broaden a manager's scope. */
    public List<Long> findHubSubtreeIds(Long rootHubId) {
        @SuppressWarnings("unchecked")
        List<Number> rows = entityManager.createNativeQuery("WITH hub_scope AS (" +
                        " SELECT id, CAST('/' + CAST(id AS varchar(20)) + '/' AS varchar(max)) AS path FROM hubs WHERE id = :rootHubId" +
                        " UNION ALL" +
                        " SELECT h.id, CAST(hs.path + CAST(h.id AS varchar(20)) + '/' AS varchar(max))" +
                        " FROM hubs h JOIN hub_scope hs ON h.parent_hub_id = hs.id" +
                        " WHERE CHARINDEX('/' + CAST(h.id AS varchar(20)) + '/', hs.path) = 0" +
                        ") SELECT id FROM hub_scope OPTION (MAXRECURSION 100)")
                .setParameter("rootHubId", rootHubId).getResultList();
        return rows.stream().map(Number::longValue).toList();
    }

    public List<Long> findAllActiveHubIds() {
        @SuppressWarnings("unchecked")
        List<Number> rows = entityManager.createNativeQuery("SELECT id FROM hubs WHERE status = 'ACTIVE'").getResultList();
        return rows.stream().map(Number::longValue).toList();
    }

    public long countInboundShipments(Collection<Long> hubIds) {
        return countShipments(hubIds, "s.currentStatus = :status", ShipmentStatus.AT_HUB);
    }

    public long countSortingBacklog(Collection<Long> hubIds) {
        return entityManager.createQuery("select count(s) from Shipment s where s.currentHub.id in :hubIds and s.currentStatus = :status " +
                        "and not exists (select r.id from ShipmentRoute r where r.shipment.id = s.id)", Long.class)
                .setParameter("hubIds", hubIds).setParameter("status", ShipmentStatus.AT_HUB).getSingleResult();
    }

    public long countOutboundContainers(Collection<Long> hubIds) {
        return entityManager.createQuery("select count(c) from ParcelContainer c where c.originHub.id in :hubIds and c.status = 'SEALED'", Long.class)
                .setParameter("hubIds", hubIds).getSingleResult();
    }

    public long countActiveHandovers(Collection<Long> hubIds) {
        return entityManager.createQuery("select count(h) from Handover h where (h.fromHub.id in :hubIds or h.toHub.id in :hubIds) " +
                        "and h.status in ('PENDING_ACCEPTANCE', 'ACCEPTED')", Long.class)
                .setParameter("hubIds", hubIds).getSingleResult();
    }

    public long countActiveTrips(Collection<Long> hubIds) {
        return entityManager.createQuery("select count(t) from TransportTrip t where (t.originHub.id in :hubIds or t.destinationHub.id in :hubIds) " +
                        "and t.status in ('ACCEPTED', 'STARTED', 'ARRIVED')", Long.class)
                .setParameter("hubIds", hubIds).getSingleResult();
    }

    /** Counts unresolved durable routing exceptions, never transient or historical events. */
    public long countBlockedRoutes(Collection<Long> hubIds) {
        return entityManager.createQuery("select count(e) from RoutingExceptionRecord e where e.hub.id in :hubIds " +
                        "and e.exceptionType = 'ROUTE_LOOKUP_BLOCKED' and e.status = 'OPEN'", Long.class)
                .setParameter("hubIds", hubIds).getSingleResult();
    }

    public long countActionableIncidents(Collection<Long> hubIds) {
        return entityManager.createQuery("select count(i) from Incident i where i.hubId in :hubIds " +
                        "and i.type in ('TRIP_DELAY', 'MISSING_PARCEL') and i.status in ('OPEN', 'INVESTIGATING')", Long.class)
                .setParameter("hubIds", hubIds).getSingleResult();
    }

    private long countShipments(Collection<Long> hubIds, String condition, ShipmentStatus status) {
        return entityManager.createQuery("select count(s) from Shipment s where s.currentHub.id in :hubIds and " + condition, Long.class)
                .setParameter("hubIds", hubIds).setParameter("status", status).getSingleResult();
    }
}
