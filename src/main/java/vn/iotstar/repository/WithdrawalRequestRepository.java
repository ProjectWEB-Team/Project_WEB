package vn.iotstar.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.WithdrawalRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT wr FROM WithdrawalRequest wr WHERE wr.id = :id")
    Optional<WithdrawalRequest> findByIdForUpdate(@Param("id") Long id);

    Optional<WithdrawalRequest> findByUserIdAndRequestKey(Long userId, String requestKey);

    List<WithdrawalRequest> findByUserIdOrderByRequestedAtDesc(Long userId);

    List<WithdrawalRequest> findByStatusOrderByRequestedAtAsc(String status);
}

