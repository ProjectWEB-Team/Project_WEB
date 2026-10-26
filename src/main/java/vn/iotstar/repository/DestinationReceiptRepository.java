package vn.iotstar.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.ContainerItem;
import vn.iotstar.entity.Handover;
import vn.iotstar.entity.HandoverContainer;

import java.util.List;
import java.util.Optional;

/** Locked persistence boundary for destination physical evidence. */
@Repository
public class DestinationReceiptRepository {
    @PersistenceContext private EntityManager entityManager;
    public Optional<Handover> findArrivedHandoverForUpdate(Long handoverId) {
        return entityManager.createQuery("select h from Handover h join fetch h.toHub left join fetch h.transportTrip t where h.id = :id", Handover.class)
                .setParameter("id", handoverId).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst();
    }
    public Optional<HandoverContainer> findHandoverContainer(Long handoverId, Long containerId) {
        return entityManager.createQuery("select hc from HandoverContainer hc join fetch hc.container c where hc.handover.id = :handoverId and c.id = :containerId", HandoverContainer.class)
                .setParameter("handoverId", handoverId).setParameter("containerId", containerId).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst();
    }
    public Optional<ContainerItem> findItemForUpdate(Long containerId, Long shipmentId) {
        return entityManager.createQuery("select ci from ContainerItem ci join fetch ci.shipment where ci.container.id = :containerId and ci.shipment.id = :shipmentId", ContainerItem.class)
                .setParameter("containerId", containerId).setParameter("shipmentId", shipmentId).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst();
    }
    public List<ContainerItem> findItemsForContainerForUpdate(Long containerId) {
        return entityManager.createQuery("select ci from ContainerItem ci join fetch ci.shipment where ci.container.id = :containerId", ContainerItem.class)
                .setParameter("containerId", containerId).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
    }
    public List<HandoverContainer> findHandoverContainers(Long handoverId) {
        return entityManager.createQuery("select hc from HandoverContainer hc join fetch hc.container where hc.handover.id = :handoverId", HandoverContainer.class)
                .setParameter("handoverId", handoverId).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
    }
}
