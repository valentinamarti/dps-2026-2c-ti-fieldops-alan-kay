package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceJpaRepository extends JpaRepository<ResourceEntity, String> {
    List<ResourceEntity> findAllByKindInOrderByCreatedOrderAsc(List<String> kinds);
}
