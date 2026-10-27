package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.entity.Hub;

public interface HubRepository extends JpaRepository<Hub, Long> {
    java.util.List<Hub> findByStatusOrderByCode(String status);
    boolean existsByCodeIgnoreCase(String code);
    java.util.List<Hub> findByStatusAndTypeAndProvinceAndDistrict(String status, String type, String province, String district);
}
