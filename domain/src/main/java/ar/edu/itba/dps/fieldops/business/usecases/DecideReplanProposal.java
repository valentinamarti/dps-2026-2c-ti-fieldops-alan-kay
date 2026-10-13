package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanDecision;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;
import ar.edu.itba.dps.fieldops.business.replanning.ExpeditionReplanner;

import java.util.Objects;

public class DecideReplanProposal {
    private final ReplanProposalRepository proposals;
    private final ExpeditionRepository expeditions;
    private final ExpeditionReplanner replanner;
    private final ReplanExpedition replanExpedition;
    private final ResourceCatalog catalog;

    public DecideReplanProposal(ReplanProposalRepository proposals, ExpeditionRepository expeditions, ExpeditionReplanner replanner,
                                ReplanExpedition replanExpedition, ResourceCatalog catalog) {
        this.proposals = Objects.requireNonNull(proposals, "proposals are required");
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions are required");
        this.replanner = Objects.requireNonNull(replanner, "replanner is required");
        this.replanExpedition = Objects.requireNonNull(replanExpedition, "replanExpedition is required");
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    public Expedition accept(ReplanProposalId proposalId, int optionNumber, ReplanDecisionCommand command) {
        final var proposal = Lookups.proposal(proposals, proposalId);
        proposal.requirePending();
        final var option = proposal.option(optionNumber);
        final var original = Lookups.expedition(expeditions, proposal.getOriginalExpedition());
        final var decision = decisionOf(original, command);
        final var affected = Lookups.item(original, proposal.getAffectedActivity());
        if (!replanner.canPlan(original, affected, option.reason())) {
            throw new InvalidScheduleException("the option %d of the proposal %s can no longer be planned".formatted(optionNumber, proposalId));
        }
        original.suspendToReplan();
        final var alternative = replanExpedition.replan(original, affected, option.reason());
        proposal.accept(optionNumber, decision, alternative.getId());
        proposals.findByExpedition(original.getId()).stream()
                .filter(ReplanProposal::isPending)
                .forEach(other -> other.supersedeBy(proposal.getId()));
        return alternative;
    }

    public void reject(ReplanProposalId proposalId, ReplanDecisionCommand command) {
        final var proposal = Lookups.proposal(proposals, proposalId);
        proposal.requirePending();
        proposal.reject(decisionOf(Lookups.expedition(expeditions, proposal.getOriginalExpedition()), command));
    }

    private ReplanDecision decisionOf(Expedition original, ReplanDecisionCommand command) {
        final var responsible = Lookups.staff(catalog, command.responsibleId());
        original.requireResponsible(responsible);
        return new ReplanDecision(responsible, command.justification(), command.decidedAt());
    }
}
