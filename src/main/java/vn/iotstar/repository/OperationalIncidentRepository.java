package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.entity.Incident;

public interface OperationalIncidentRepository extends JpaRepository<Incident, Long> { }
