package vn.iotstar.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import vn.iotstar.dto.operations.AuditCustodyReference;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;

import java.util.List;
import java.util.Optional;

/** Repository-only, minimal Phase-03 audit reads. Controllers never receive this type. */
@Repository
public class OperationsAuditRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Shipment> findShipmentWithCurrentHub(Long shipmentId) {
        return entityManager.createQuery("select s from Shipment s left join fetch s.currentHub where s.id = :shipmentId", Shipment.class)
                .setParameter("shipmentId", shipmentId).getResultStream().findFirst();
    }

    public Optional<EmployeeProfile> findActiveProfileByUserId(Long userId) {
        return entityManager.createQuery("select p from EmployeeProfile p join fetch p.hub where p.user.id = :userId and p.status = 'ACTIVE'", EmployeeProfile.class)
                .setParameter("userId", userId).getResultStream().findFirst();
    }

    public boolean isHubWithinSubtree(Long rootHubId, Long targetHubId) {
        Number result = (Number) entityManager.createNativeQuery("WITH hub_scope AS (" +
                        " SELECT id, CAST('/' + CAST(id AS varchar(20)) + '/' AS varchar(max)) AS path FROM hubs WHERE id = :rootHubId" +
                        " UNION ALL SELECT h.id, CAST(hs.path + CAST(h.id AS varchar(20)) + '/' AS varchar(max))" +
                        " FROM hubs h JOIN hub_scope hs ON h.parent_hub_id = hs.id" +
                        " WHERE CHARINDEX('/' + CAST(h.id AS varchar(20)) + '/', hs.path) = 0" +
                        ") SELECT CASE WHEN EXISTS (SELECT 1 FROM hub_scope WHERE id = :targetHubId) THEN 1 ELSE 0 END OPTION (MAXRECURSION 100)")
                .setParameter("rootHubId", rootHubId).setParameter("targetHubId", targetHubId).getSingleResult();
        return result.intValue() == 1;
    }

    public List<ShipmentEvent> findEventsForShipment(Long shipmentId) {
        return entityManager.createQuery("select e from ShipmentEvent e join fetch e.actor left join fetch e.hub where e.shipment.id = :shipmentId order by e.createdAt asc, e.id asc", ShipmentEvent.class)
                .setParameter("shipmentId", shipmentId).getResultList();
    }

    public List<AuditCustodyReference> findCustodyReferences(Long shipmentId) {
        return entityManager.createQuery("select new vn.iotstar.dto.operations.AuditCustodyReference(c.id, h.id, t.id, d.id) " +
                        "from ContainerItem ci join ci.container c left join HandoverContainer hc on hc.container = c " +
                        "left join hc.handover h left join h.transportTrip t left join h.receiverDriver d where ci.shipment.id = :shipmentId", AuditCustodyReference.class)
                .setParameter("shipmentId", shipmentId).getResultList();
    }
}
