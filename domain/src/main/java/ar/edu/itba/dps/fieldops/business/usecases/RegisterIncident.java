package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.events.IncidentRegistered;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.providers.EventPublisher;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;

import java.util.Objects;

/**
 * Registers an incident in its expedition and announces it. Delivery is synchronous, so by the time the event is
 * published the proposal already exists, and this use case returns it (D45).
 */
public class RegisterIncident {

    private final ExpeditionRepository expeditions;
    private final EventPublisher publisher;
    private final ReplanProposalRepository proposals;

    public RegisterIncident(ExpeditionRepository expeditions, EventPublisher publisher, ReplanProposalRepository proposals) {
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions are required");
        this.publisher = Objects.requireNonNull(publisher, "publisher is required");
        this.proposals = Objects.requireNonNull(proposals, "proposals are required");
    }

    public ReplanProposal register(RegisterIncidentCommand command) {
        final var expedition = Lookups.expedition(expeditions, command.expeditionId());
        final var incident = expedition.registerIncident(Lookups.item(expedition, command.activityId()), command.report());
        publisher.publish(new IncidentRegistered(expedition.getId(), incident));
        return proposals.findByIncident(incident.id()).orElseThrow(() ->
                new IllegalStateException("no replan proposal was made for the incident %s".formatted(incident.id())));
    }
}
