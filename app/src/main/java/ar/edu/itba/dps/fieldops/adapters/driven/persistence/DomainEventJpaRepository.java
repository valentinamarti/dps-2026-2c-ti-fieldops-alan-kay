package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DomainEventJpaRepository extends JpaRepository<DomainEventEntity, Long> {
    List<DomainEventEntity> findByExpeditionIdOrderBySequenceAsc(String expeditionId);
}
