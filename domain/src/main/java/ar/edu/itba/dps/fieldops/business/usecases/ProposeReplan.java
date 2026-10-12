package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.events.EventSubscriber;
import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanOptionGenerator;
import ar.edu.itba.dps.fieldops.business.models.events.IncidentRegistered;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;
import ar.edu.itba.dps.fieldops.business.replanning.ExpeditionReplanner;

import java.util.List;
import java.util.Objects;

/**
 * Reacts to a registered incident by storing a proposal with every option the generators offer and the replanner
 * can actually build. A proposal with no options is still stored: it records that nothing could be proposed (D46).
 */
public class ProposeReplan implements EventSubscriber<IncidentRegistered> {

    private final ExpeditionRepository expeditions;
    private final ReplanProposalRepository proposals;
    private final ExpeditionReplanner replanner;
    private final List<ReplanOptionGenerator> generators;

    public ProposeReplan(ExpeditionRepository expeditions, ReplanProposalRepository proposals, ExpeditionReplanner replanner,
                         List<ReplanOptionGenerator> generators) {
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions are required");
        this.proposals = Objects.requireNonNull(proposals, "proposals are required");
        this.replanner = Objects.requireNonNull(replanner, "replanner is required");
        this.generators = List.copyOf(Objects.requireNonNull(generators, "generators are required"));
    }

    @Override
    public Class<IncidentRegistered> eventType() {
        return IncidentRegistered.class;
    }

    @Override
    public void on(IncidentRegistered event) {
        final var expedition = Lookups.expedition(expeditions, event.expedition());
        final var incident = event.incident();
        final var affected = Lookups.item(expedition, incident.activity());
        final var options = generators.stream()
                .flatMap(generator -> generator.optionsFor(expedition, affected, incident).stream())
                .filter(option -> replanner.canPlan(expedition, affected, option.reason()))
                .toList();
        proposals.add(new ReplanProposal(proposals.nextId(), expedition.getId(), incident, options));
    }
}
