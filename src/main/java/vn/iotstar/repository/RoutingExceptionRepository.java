package vn.iotstar.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.iotstar.entity.RoutingExceptionRecord;

import java.util.Optional;

/** Persistence boundary for durable, deduplicated static-routing diagnostics. */
public interface RoutingExceptionRepository extends JpaRepository<RoutingExceptionRecord, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from RoutingExceptionRecord e where e.shipment.id = :shipmentId and e.hub.id = :hubId " +
            "and e.exceptionType = 'ROUTE_LOOKUP_BLOCKED' and e.status = 'OPEN'")
    Optional<RoutingExceptionRecord> findOpenRouteLookupBlockedForUpdate(@Param("shipmentId") Long shipmentId,
                                                                           @Param("hubId") Long hubId);
}
