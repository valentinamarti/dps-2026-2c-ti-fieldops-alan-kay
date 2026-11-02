package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.commands.RegisterIncidentCommand;
import ar.edu.itba.dps.fieldops.business.models.events.IncidentRegistered;
import ar.edu.itba.dps.fieldops.business.models.responses.ReplanProposalView;
import ar.edu.itba.dps.fieldops.business.ports.in.RegisterIncidentPort;
import ar.edu.itba.dps.fieldops.business.providers.EventPublisher;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;

import java.util.Objects;

public class RegisterIncident implements RegisterIncidentPort {
    private final ExpeditionRepository expeditions;
    private final EventPublisher publisher;
    private final ReplanProposalRepository proposals;

    public RegisterIncident(ExpeditionRepository expeditions, EventPublisher publisher, ReplanProposalRepository proposals) {
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions are required");
        this.publisher = Objects.requireNonNull(publisher, "publisher is required");
        this.proposals = Objects.requireNonNull(proposals, "proposals are required");
    }

    @Override
    public ReplanProposalView register(RegisterIncidentCommand command) {
        final var expedition = Lookups.expedition(expeditions, command.expeditionId());
        final var incident = expedition.registerIncident(Lookups.item(expedition, command.activityId()), command.report());
        expeditions.save(expedition);
        publisher.publish(new IncidentRegistered(expedition.getId(), incident));
        return proposals.findByIncident(incident.id()).map(ReplanProposalView::of).orElseThrow(() ->
                new IllegalStateException("no replan proposal was made for the incident %s".formatted(incident.id())));
    }
}
