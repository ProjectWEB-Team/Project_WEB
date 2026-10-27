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

    private static final String AT_MY_HUB = " and h.toHub.id in (select p.hub.id from EmployeeProfile p where p.user.username = :u and p.status = 'ACTIVE')"
            + " and h.status = 'IN_TRANSIT'";
    /** Picker rows for the inbound page, scoped to the staff member's own destination hub: {handoverId, fromHubCode}. */
    public List<Object[]> inboundHandoverPicks(String username) {
        return entityManager.createQuery("select h.id, h.fromHub.code from Handover h where 1 = 1" + AT_MY_HUB + " order by h.id desc", Object[].class)
                .setParameter("u", username).setMaxResults(50).getResultList();
    }
    /** {handoverId, containerId, containerCode}. */
    public List<Object[]> inboundContainerPicks(String username) {
        return entityManager.createQuery("select hc.handover.id, hc.container.id, hc.container.containerCode from HandoverContainer hc join hc.handover h where 1 = 1" + AT_MY_HUB + " order by h.id desc, hc.container.id", Object[].class)
                .setParameter("u", username).setMaxResults(200).getResultList();
    }
    /** {containerId, shipmentId, trackingNumber}. */
    public List<Object[]> inboundShipmentPicks(String username) {
        return entityManager.createQuery("select ci.container.id, ci.shipment.id, ci.shipment.trackingNumber from ContainerItem ci, HandoverContainer hc join hc.handover h where hc.container = ci.container" + AT_MY_HUB + " order by ci.container.id, ci.shipment.id", Object[].class)
                .setParameter("u", username).setMaxResults(500).getResultList();
    }
}
