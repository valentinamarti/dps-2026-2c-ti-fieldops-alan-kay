package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateReplanProposalException;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanDecision;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanOption;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalSnapshot;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalStatus;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Transactional
public class JpaReplanProposalRepository implements ReplanProposalRepository {
    private static final String CACHE_PREFIX = "proposal:";

    private final ReplanProposalJpaRepository proposals;
    private final ResourceCatalog catalog;
    private final ReplanReasonMapper reasons;
    private final LoadedAggregates loaded;

    public JpaReplanProposalRepository(ReplanProposalJpaRepository proposals, ResourceCatalog catalog, LoadedAggregates loaded) {
        this.proposals = Objects.requireNonNull(proposals, "proposals are required");
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
        this.reasons = new ReplanReasonMapper(catalog);
        this.loaded = Objects.requireNonNull(loaded, "loaded is required");
    }

    @Override
    public ReplanProposalId nextId() {
        return new ReplanProposalId(UUID.randomUUID().toString());
    }

    @Override
    public void add(ReplanProposal proposal) {
        Objects.requireNonNull(proposal, "proposal is required");
        if (proposals.existsById(proposal.getId().value())) {
            throw new DuplicateReplanProposalException(proposal.getId());
        }
        save(proposal);
    }

    @Override
    public void save(ReplanProposal proposal) {
        Objects.requireNonNull(proposal, "proposal is required");
        final var entity = proposals.findById(proposal.getId().value()).orElseGet(ReplanProposalEntity::new);
        fill(entity, proposal.snapshot());
        proposals.save(entity);
        loaded.put(CACHE_PREFIX + proposal.getId().value(), proposal);
    }

    @Override
    public Optional<ReplanProposal> findById(ReplanProposalId id) {
        return proposals.findById(id.value()).map(this::loadedFrom);
    }

    @Override
    public Optional<ReplanProposal> findByIncident(IncidentId incident) {
        return proposals.findFirstByIncident_ExpeditionAndIncident_Number(incident.expedition().value(), incident.number())
                .map(this::loadedFrom);
    }

    @Override
    public List<ReplanProposal> findByExpedition(ExpeditionId expedition) {
        return proposals.findByExpeditionIdOrderByCreatedOrderAsc(expedition.value()).stream().map(this::loadedFrom).toList();
    }

    private ReplanProposal loadedFrom(ReplanProposalEntity entity) {
        final var key = CACHE_PREFIX + entity.getId();
        final ReplanProposal alreadyLoaded = loaded.get(key);
        if (alreadyLoaded != null) {
            return alreadyLoaded;
        }
        final var proposal = toDomain(entity);
        loaded.put(key, proposal);
        return proposal;
    }

    private void fill(ReplanProposalEntity entity, ReplanProposalSnapshot snapshot) {
        entity.setId(snapshot.id().value());
        entity.setExpeditionId(snapshot.originalExpedition().value());
        entity.setStatus(snapshot.status().name());
        entity.setIncident(IncidentMapper.toColumns(snapshot.incident()));
        entity.setResponsibleId(snapshot.decision().map(decision -> decision.responsible().getId().value()).orElse(null));
        entity.setJustification(snapshot.decision().map(ReplanDecision::justification).orElse(null));
        entity.setDecidedAt(snapshot.decision().map(ReplanDecision::decidedAt).orElse(null));
        entity.setChosenOption(snapshot.chosenOption().orElse(null));
        entity.setAlternative(snapshot.alternative().map(ExpeditionId::value).orElse(null));
        entity.setSupersededBy(snapshot.supersededBy().map(ReplanProposalId::value).orElse(null));
        entity.getOptions().clear();
        snapshot.options().forEach(option -> entity.getOptions().add(new ReplanOptionRow(option.summary(), reasons.toColumns(option.reason()))));
    }

    private ReplanProposal toDomain(ReplanProposalEntity entity) {
        return ReplanProposal.restore(new ReplanProposalSnapshot(
                new ReplanProposalId(entity.getId()),
                new ExpeditionId(entity.getExpeditionId()),
                IncidentMapper.toDomain(entity.getIncident()),
                entity.getOptions().stream().map(row -> new ReplanOption(reasons.toDomain(row.getReason()), row.getSummary())).toList(),
                ReplanProposalStatus.valueOf(entity.getStatus()),
                decisionOf(entity),
                Optional.ofNullable(entity.getChosenOption()),
                Optional.ofNullable(entity.getAlternative()).map(ExpeditionId::new),
                Optional.ofNullable(entity.getSupersededBy()).map(ReplanProposalId::new)));
    }

    private Optional<ReplanDecision> decisionOf(ReplanProposalEntity entity) {
        return Optional.ofNullable(entity.getResponsibleId()).map(id -> new ReplanDecision(
                catalog.findStaff(new ResourceId(id)).orElseThrow(() -> new IllegalStateException("the person %s is not registered".formatted(id))),
                entity.getJustification(),
                entity.getDecidedAt()));
    }
}
