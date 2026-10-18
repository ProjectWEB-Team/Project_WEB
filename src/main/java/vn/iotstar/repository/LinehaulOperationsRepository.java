package vn.iotstar.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.Handover;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.TransportTrip;

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
    public void persist(Object entity) { entityManager.persist(entity); }
}
