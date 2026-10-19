package vn.iotstar.repository;

import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.iotstar.entity.ContainerItem;
import vn.iotstar.entity.Handover;
import vn.iotstar.entity.HandoverContainer;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.ParcelContainer;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentRoute;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

/** Repository boundary for custody aggregates; controllers never access it. */
@Repository
public class ContainerHandoverRepository {
    @PersistenceContext private EntityManager entityManager;
    public Optional<ParcelContainer> findContainerForUpdate(Long id) {
        return entityManager.createQuery("select c from ParcelContainer c join fetch c.originHub join fetch c.destinationHub where c.id = :id", ParcelContainer.class)
                .setParameter("id", id).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst();
    }
    public Optional<Shipment> findShipmentForUpdate(Long id) {
        return entityManager.createQuery("select s from Shipment s join fetch s.currentHub where s.id = :id", Shipment.class)
                .setParameter("id", id).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst();
    }
    public Optional<ShipmentRoute> findPendingRouteForShipmentAtHub(Long shipmentId, Long currentHubId) {
        return entityManager.createQuery("select r from ShipmentRoute r join fetch r.hub where r.shipment.id = :shipmentId " +
                        "and r.currentHub.id = :currentHubId and r.status = 'PENDING' order by r.id desc", ShipmentRoute.class)
                .setParameter("shipmentId", shipmentId).setParameter("currentHubId", currentHubId)
                .setMaxResults(1).getResultStream().findFirst();
    }
    public Optional<Hub> findHubById(Long id) { return Optional.ofNullable(entityManager.find(Hub.class, id)); }
    public Optional<Handover> findHandoverForUpdate(Long id) {
        return entityManager.createQuery("select h from Handover h join fetch h.fromHub join fetch h.toHub join fetch h.receiverDriver where h.id = :id", Handover.class)
                .setParameter("id", id).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst();
    }
    public Optional<HandoverContainer> findHandoverContainer(Long handoverId, Long containerId) {
        return entityManager.createQuery("select hc from HandoverContainer hc join fetch hc.container where hc.handover.id = :handoverId and hc.container.id = :containerId", HandoverContainer.class)
                .setParameter("handoverId", handoverId).setParameter("containerId", containerId).getResultStream().findFirst();
    }
    public List<HandoverContainer> findActiveCustodyForContainer(Long containerId) {
        return entityManager.createQuery("select hc from HandoverContainer hc join fetch hc.handover h where hc.container.id = :id and h.status in ('PENDING_ACCEPTANCE','ACCEPTED')", HandoverContainer.class).setParameter("id", containerId).getResultList();
    }
    public boolean containerAlreadyContains(Long containerId, Long shipmentId) {
        return !entityManager.createQuery("select ci from ContainerItem ci where ci.container.id = :containerId and ci.shipment.id = :shipmentId", ContainerItem.class)
                .setParameter("containerId", containerId).setParameter("shipmentId", shipmentId).setMaxResults(1).getResultList().isEmpty();
    }
    public <T> T save(T entity) { return entityManager.merge(entity); }
    public void persist(Object entity) { entityManager.persist(entity); }
}
