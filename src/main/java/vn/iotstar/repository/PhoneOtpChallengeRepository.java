package vn.iotstar.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.iotstar.entity.PhoneOtpChallenge;
import java.util.Optional;
import java.util.UUID;

public interface PhoneOtpChallengeRepository extends JpaRepository<PhoneOtpChallenge, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PhoneOtpChallenge c join fetch c.user where c.id = :id")
    Optional<PhoneOtpChallenge> findByIdForUpdate(@Param("id") UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PhoneOtpChallenge c where c.user.id = :userId")
    Optional<PhoneOtpChallenge> findByUserIdForUpdate(@Param("userId") Long userId);
}
