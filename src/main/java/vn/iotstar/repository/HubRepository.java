package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.entity.Hub;

public interface HubRepository extends JpaRepository<Hub, Long> {
}
