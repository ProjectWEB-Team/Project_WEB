package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.entity.User;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from PhoneOtpChallenge c where c.user.id = :id")
    void deletePhoneChallengesOf(@Param("id") Long id);
    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from EmailOtpChallenge c where c.user.id = :id")
    void deleteEmailChallengesOf(@Param("id") Long id);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleSubject(String googleSubject);
    java.util.List<User> findByRoleAndStatusOrderByFullName(vn.iotstar.entity.UserRole role, String status);
    boolean existsByUsername(String username);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    @Query("select u from User u where :q = '' or lower(u.username) like lower(concat('%', :q, '%'))"
            + " or lower(u.fullName) like lower(concat('%', :q, '%')) or u.phone like concat('%', :q, '%') order by u.id desc")
    org.springframework.data.domain.Page<User> search(@Param("q") String q, org.springframework.data.domain.Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.phone = :phone")
    Optional<User> findByPhoneForUpdate(@Param("phone") String phone);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.email = :email")
    Optional<User> findByEmailForUpdate(@Param("email") String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
