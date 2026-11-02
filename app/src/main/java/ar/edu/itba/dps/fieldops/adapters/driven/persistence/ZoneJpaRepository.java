package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZoneJpaRepository extends JpaRepository<ZoneEntity, String> {
    List<ZoneEntity> findAllByOrderByCreatedOrderAsc();
}
