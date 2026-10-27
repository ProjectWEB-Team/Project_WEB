package vn.iotstar.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.iotstar.entity.EmailOtpChallenge;
import java.util.Optional;
import java.util.UUID;

public interface EmailOtpChallengeRepository extends JpaRepository<EmailOtpChallenge, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from EmailOtpChallenge c join fetch c.user where c.id = :id")
    Optional<EmailOtpChallenge> findByIdForUpdate(@Param("id") UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from EmailOtpChallenge c where c.user.id = :userId")
    Optional<EmailOtpChallenge> findByUserIdForUpdate(@Param("userId") Long userId);
}
