package vn.iotstar.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.Handover;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.TransportTrip;
import vn.iotstar.entity.Vehicle;
import vn.iotstar.entity.HandoverContainer;
import vn.iotstar.entity.ContainerItem;

import java.util.List;
import java.util.Optional;

/** Locked persistence gateway for driver-scoped linehaul operations. */
@Repository
public class LinehaulOperationsRepository {
    @PersistenceContext private EntityManager entityManager;
    public Optional<Handover> findHandoverForUpdate(Long id) {
        return entityManager.createQuery("select h from Handover h join fetch h.fromHub join fetch h.toHub join fetch h.receiverDriver left join fetch h.transportTrip where h.id = :id", Handover.class)
                .setParameter("id", id).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst();
    }
    public Optional<TransportTrip> findTripForUpdate(Long id) {
        return entityManager.createQuery("select t from TransportTrip t join fetch t.originHub join fetch t.destinationHub join fetch t.driver join fetch t.handover where t.id = :id", TransportTrip.class)
                .setParameter("id", id).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst();
    }
    public List<Shipment> findShipmentsForHandover(Long handoverId) {
        return entityManager.createQuery("select ci.shipment from ContainerItem ci join HandoverContainer hc on hc.container.id = ci.container.id where hc.handover.id = :handoverId", Shipment.class)
                .setParameter("handoverId", handoverId).getResultList();
    }
    public Optional<Vehicle> findVehicleForUpdate(Long id) {
        return Optional.ofNullable(entityManager.find(Vehicle.class, id, LockModeType.PESSIMISTIC_WRITE));
    }
    public List<Vehicle> findAvailableVehicles() {
        return entityManager.createQuery("select v from Vehicle v where v.status = 'AVAILABLE' order by v.plateNumber", Vehicle.class)
                .setMaxResults(100).getResultList();
    }
    public List<HandoverContainer> findContainersForUpdate(Long handoverId) {
        return entityManager.createQuery("select hc from HandoverContainer hc join fetch hc.container where hc.handover.id = :id order by hc.container.id", HandoverContainer.class)
                .setParameter("id", handoverId).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
    }
    public List<ContainerItem> findManifestItemsForUpdate(Long handoverId) {
        return entityManager.createQuery("select ci from ContainerItem ci join fetch ci.shipment s join HandoverContainer hc on hc.container = ci.container where hc.handover.id = :id order by s.id", ContainerItem.class)
                .setParameter("id", handoverId).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
    }
    /** Picker rows for the driver page. */
    public List<Object[]> driverHandoverPicks(String username) {
        return entityManager.createQuery("select h.id, h.fromHub.code, h.toHub.code from Handover h where h.receiverDriver.username = :u and h.status in ('PENDING_ACCEPTANCE','ACCEPTED') order by h.id desc", Object[].class)
                .setParameter("u", username).setMaxResults(50).getResultList();
    }
    public List<Object[]> driverContainerPicks(String username) {
        return entityManager.createQuery("select hc.handover.id, hc.container.id, hc.container.containerCode from HandoverContainer hc where hc.handover.receiverDriver.username = :u and hc.handover.status = 'PENDING_ACCEPTANCE' and hc.status = 'EXPECTED' order by hc.handover.id desc, hc.container.id", Object[].class)
                .setParameter("u", username).setMaxResults(200).getResultList();
    }
    public List<Object[]> driverTripPicks(String username) {
        return entityManager.createQuery("select t.id, t.tripCode, t.status, t.destinationHub.code from TransportTrip t where t.driver.username = :u and t.status in ('SCHEDULED','IN_TRANSIT','ARRIVED') order by t.id desc", Object[].class)
                .setParameter("u", username).setMaxResults(50).getResultList();
    }
    public void persist(Object entity) { entityManager.persist(entity); }
}
