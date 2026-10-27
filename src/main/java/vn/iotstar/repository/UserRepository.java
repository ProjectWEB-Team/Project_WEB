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
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleSubject(String googleSubject);
    boolean existsByUsername(String username);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    @Query("select u from User u where :q = '' or lower(u.username) like lower(concat('%', :q, '%'))"
            + " or lower(u.fullName) like lower(concat('%', :q, '%')) or u.phone like concat('%', :q, '%') order by u.id desc")
    org.springframework.data.domain.Page<User> search(@Param("q") String q, org.springframework.data.domain.Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.phone = :phone")
    Optional<User> findByPhoneForUpdate(@Param("phone") String phone);
}
