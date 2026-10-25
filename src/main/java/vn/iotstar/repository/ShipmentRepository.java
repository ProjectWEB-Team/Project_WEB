package vn.iotstar.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shipment s where s.id = :id")
    Optional<Shipment> findByIdForUpdate(@Param("id") Long id);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Shipment s set s.receiver = :user where s.receiver is null and s.receiverPhone in :phones")
    int linkReceiver(@Param("user") User user, @Param("phones") Collection<String> phones);
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    Page<Shipment> findBySenderIdOrderByCreatedAtDesc(Long senderId, Pageable pageable);
    Page<Shipment> findBySenderIdAndCurrentStatusOrderByCreatedAtDesc(Long senderId, ShipmentStatus status, Pageable pageable);
    Page<Shipment> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);
    List<Shipment> findByCurrentStatus(ShipmentStatus currentStatus);
    @Query("select s from Shipment s where s.pickupMethod = 'HOME_PICKUP' and s.currentStatus = :status and (s.originHubId is null or s.originHubId = :hubId) and not exists (select a.id from ShipmentAssignment a where a.shipment = s and a.assignmentType = 'PICKUP' and a.status = 'ASSIGNED') order by s.createdAt asc")
    List<Shipment> findPendingPickups(@Param("hubId") Long hubId, @Param("status") ShipmentStatus status);
    @Query("select s.id from Shipment s where s.pickupMethod = 'SELF_DROPOFF' and s.currentStatus = :status and s.waitingForHandoverAt <= :cutoff")
    List<Long> findExpiredDropoffIds(@Param("status") ShipmentStatus status, @Param("cutoff") LocalDateTime cutoff);
}
