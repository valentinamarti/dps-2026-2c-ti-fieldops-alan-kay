package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReplanProposalJpaRepository extends JpaRepository<ReplanProposalEntity, String> {
    List<ReplanProposalEntity> findByExpeditionIdOrderByCreatedOrderAsc(String expeditionId);

    Optional<ReplanProposalEntity> findFirstByIncident_ExpeditionAndIncident_Number(String expedition, int number);
}
