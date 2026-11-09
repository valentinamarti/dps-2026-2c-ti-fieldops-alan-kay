package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgressEntryJpaRepository extends JpaRepository<ProgressEntryEntity, Long> {
    List<ProgressEntryEntity> findByExpeditionIdOrderBySequenceAsc(String expeditionId);
}
