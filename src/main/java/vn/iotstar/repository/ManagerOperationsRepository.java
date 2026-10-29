package vn.iotstar.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.dto.operations.OperationalWorkItemResponse;

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
        return entityManager.createQuery("select count(s) from Shipment s where s.currentHub.id in :hubIds and s.onHold = false " +
                "and ((s.direction = 'FORWARD' and s.currentStatus = :forward and (s.destinationHubId is null or s.destinationHubId <> s.currentHub.id)) " +
                "or (s.direction = 'RETURN' and s.currentStatus = :returning and s.originHubId <> s.currentHub.id)) " +
                "and not exists (select r.id from ShipmentRoute r where r.shipment = s and r.currentHub.id = s.currentHub.id " +
                "and r.routeDecisionKey = concat('shipment:', cast(s.id as string), ':hub:', cast(s.currentHub.id as string), ':', s.direction))", Long.class)
                .setParameter("hubIds", hubIds).setParameter("forward", ShipmentStatus.AT_HUB)
                .setParameter("returning", ShipmentStatus.RETURN_TO_SENDER).getSingleResult();
    }

    public List<OperationalWorkItemResponse> findWorkItems(Collection<Long> hubIds) {
        if (hubIds.isEmpty()) return List.of();
        return java.util.stream.Stream.of("ROUTE", "MISSING", "HANDOVER")
                .flatMap(kind -> workItems(hubIds, kind, null).stream()).toList();
    }

    public Optional<OperationalWorkItemResponse> findWorkItem(Collection<Long> hubIds, String kind, Long id) {
        if (hubIds.isEmpty()) return Optional.empty();
        return workItems(hubIds, kind, id).stream().findFirst();
    }

    private List<OperationalWorkItemResponse> workItems(Collection<Long> hubIds, String kind, Long id) {
        String select;
        String summary;
        switch (kind) {
            case "ROUTE" -> {
                select = "select e.id, e.shipment.id, e.hub.id from RoutingExceptionRecord e where e.hub.id in :hubIds "
                        + "and e.exceptionType = 'ROUTE_LOOKUP_BLOCKED' and e.status = 'OPEN'";
                summary = "Tuyến cần chỉnh lại";
            }
            case "MISSING" -> {
                select = "select e.id, e.shipmentId, e.hubId from Incident e where e.hubId in :hubIds "
                        + "and e.type = 'MISSING_PARCEL' and e.status in ('OPEN', 'INVESTIGATING')";
                summary = "Hàng thất lạc cần điều tra";
            }
            case "HANDOVER" -> {
                select = "select e.id, null, e.fromHub.id, e.status, e.fromHub.name, e.toHub.name from Handover e where (e.fromHub.id in :hubIds or e.toHub.id in :hubIds) "
                        + "and e.status in ('PENDING_ACCEPTANCE', 'ACCEPTED', 'IN_TRANSIT')";
                summary = "Bàn giao chờ nhận";
            }
            default -> throw new IllegalArgumentException("Unknown operational work item");
        }
        var query = entityManager.createQuery(select + (id == null ? "" : " and e.id = :id") + " order by e.id", Object[].class)
                .setParameter("hubIds", hubIds).setMaxResults(id == null ? 25 : 1);
        if (id != null) query.setParameter("id", id);
        return query.getResultList().stream().map(row -> new OperationalWorkItemResponse(kind, (Long) row[0],
                (Long) row[1], (Long) row[2], "HANDOVER".equals(kind) ? handoverSummary(row) : summary)).toList();
    }

    /** Everything a supervisor needs to follow one handover: route, driver, trip, containers and their shipments. */
    public Optional<java.util.Map<String, Object>> handoverDetail(Collection<Long> hubIds, Long id) {
        var found = entityManager.createQuery("select h from Handover h join fetch h.fromHub join fetch h.toHub join fetch h.receiverDriver "
                        + "left join fetch h.transportTrip where h.id = :id and (h.fromHub.id in :hubIds or h.toHub.id in :hubIds)", vn.iotstar.entity.Handover.class)
                .setParameter("id", id).setParameter("hubIds", hubIds).getResultStream().findFirst();
        if (found.isEmpty()) return Optional.empty();
        var h = found.get();
        List<java.util.Map<String, Object>> containers = new java.util.ArrayList<>();
        for (var hc : entityManager.createQuery("select hc from HandoverContainer hc join fetch hc.container where hc.handover.id = :id order by hc.id",
                vn.iotstar.entity.HandoverContainer.class).setParameter("id", id).getResultList()) {
            var shipments = entityManager.createQuery("select ci.shipment.id, ci.shipment.trackingNumber from ContainerItem ci where ci.container.id = :cid order by ci.shipment.id", Object[].class)
                    .setParameter("cid", hc.getContainer().getId()).getResultList().stream()
                    .map(r -> java.util.Map.<String, Object>of("id", r[0], "tracking", r[1])).toList();
            containers.add(java.util.Map.of("code", hc.getContainer().getContainerCode(), "status", hc.getStatus(), "shipments", shipments));
        }
        boolean reassignable = h.isPending() && h.getTransportTrip() == null && hubIds.contains(h.getFromHub().getId());
        List<java.util.Map<String, Object>> drivers = reassignable ? activeDriversAt(h.getFromHub().getId()).stream()
                .filter(p -> !p.getUser().getId().equals(h.getReceiverDriver().getId()))
                .map(p -> java.util.Map.<String, Object>of("id", p.getUser().getId(), "name", p.getUser().getFullName())).toList() : List.of();
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("id", h.getId());
        map.put("status", h.getStatus());
        map.put("fromHub", h.getFromHub().getName());
        map.put("toHub", h.getToHub().getName());
        map.put("driver", h.getReceiverDriver().getFullName());
        map.put("trip", h.getTransportTrip() == null ? "" : h.getTransportTrip().getTripCode());
        map.put("expected", h.getExpectedContainers());
        map.put("accepted", h.getAcceptedContainers());
        map.put("containers", containers);
        map.put("reassignable", reassignable);
        map.put("drivers", drivers);
        return Optional.of(map);
    }

    public List<EmployeeProfile> activeDriversAt(Long hubId) {
        return entityManager.createQuery("select p from EmployeeProfile p join fetch p.user where p.position = 'LINEHAUL_DRIVER' "
                + "and p.status = 'ACTIVE' and p.hub.id = :hubId order by p.user.fullName", EmployeeProfile.class)
                .setParameter("hubId", hubId).getResultList();
    }

    /** Cancels a handover nobody has accepted and recreates it for another driver, keeping the same sealed containers. */
    public Long reassignDriver(Collection<Long> hubIds, Long handoverId, Long driverUserId) {
        var original = entityManager.createQuery("select h from Handover h join fetch h.fromHub join fetch h.toHub join fetch h.receiverDriver "
                        + "left join fetch h.transportTrip where h.id = :id", vn.iotstar.entity.Handover.class)
                .setParameter("id", handoverId).setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst().orElse(null);
        if (original == null || !hubIds.contains(original.getFromHub().getId())) return null;
        if (!original.isPending() || original.getTransportTrip() != null) throw new IllegalStateException("Chỉ đổi được tài xế khi chưa ai nhận bàn giao.");
        var driver = activeDriversAt(original.getFromHub().getId()).stream().filter(p -> p.getUser().getId().equals(driverUserId)).findFirst().orElse(null);
        if (driver == null) throw new IllegalArgumentException("Tài xế không thuộc bưu cục xuất.");
        var replacement = vn.iotstar.entity.Handover.pending(original.getFromHub(), original.getToHub(), original.getSenderEmployee(),
                driver.getUser(), original.getExpectedContainers(), original);
        original.cancelForCorrection();
        entityManager.flush(); // the old custody must be closed before the same containers are claimed again
        original.linkReplacement(replacement);
        entityManager.persist(replacement);
        for (var hc : entityManager.createQuery("select hc from HandoverContainer hc join fetch hc.container where hc.handover.id = :id",
                vn.iotstar.entity.HandoverContainer.class).setParameter("id", handoverId).getResultList()) {
            entityManager.persist(vn.iotstar.entity.HandoverContainer.expected(replacement, hc.getContainer()));
        }
        return replacement.getId();
    }

    /** One line that says where the handover is and who must act next. */
    private static String handoverSummary(Object[] row) {
        String route = row[4] + " → " + row[5];
        return switch (String.valueOf(row[3])) {
            case "PENDING_ACCEPTANCE" -> "Bàn giao " + route + ": chờ tài xế nhận";
            case "ACCEPTED" -> "Bàn giao " + route + ": đã nhận, chờ xuất phát";
            default -> "Bàn giao " + route + ": đang vận chuyển, chờ hub đích nhận";
        };
    }

    public long countOutboundContainers(Collection<Long> hubIds) {
        return entityManager.createQuery("select count(c) from ParcelContainer c where c.originHub.id in :hubIds and c.status = 'SEALED'", Long.class)
                .setParameter("hubIds", hubIds).getSingleResult();
    }

    public long countActiveHandovers(Collection<Long> hubIds) {
        return entityManager.createQuery("select count(h) from Handover h where (h.fromHub.id in :hubIds or h.toHub.id in :hubIds) " +
                        "and h.status in ('PENDING_ACCEPTANCE', 'ACCEPTED', 'IN_TRANSIT')", Long.class)
                .setParameter("hubIds", hubIds).getSingleResult();
    }

    public long countActiveTrips(Collection<Long> hubIds) {
        return entityManager.createQuery("select count(t) from TransportTrip t where (t.originHub.id in :hubIds or t.destinationHub.id in :hubIds) " +
                        "and t.status in ('SCHEDULED', 'DEPARTED', 'IN_TRANSIT', 'ARRIVED')", Long.class)
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
