package ar.edu.itba.dps.fieldops;

import ar.edu.itba.dps.fieldops.adapters.driven.InMemoryEventPublisher;
import ar.edu.itba.dps.fieldops.adapters.driven.InMemoryReplanProposalRepository;
import ar.edu.itba.dps.fieldops.business.interfaces.events.EventSubscriber;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.EventPublisher;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;
import ar.edu.itba.dps.fieldops.business.replanning.CancellationOptions;
import ar.edu.itba.dps.fieldops.business.replanning.ExpeditionReplanner;
import ar.edu.itba.dps.fieldops.business.replanning.RescheduleOptions;
import ar.edu.itba.dps.fieldops.business.replanning.ResourceReplacementOptions;
import ar.edu.itba.dps.fieldops.business.usecases.DecideReplanProposal;
import ar.edu.itba.dps.fieldops.business.usecases.ProposeReplan;
import ar.edu.itba.dps.fieldops.business.usecases.QueryReplanProposals;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterIncident;
import ar.edu.itba.dps.fieldops.business.usecases.ReplanExpedition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Wiring of F3: incidents, the event publisher and its subscribers, and replan proposals (D45).
 */
@Configuration
public class ReplanningConfig {

    @Bean
    ReplanProposalRepository replanProposalRepository() {
        return new InMemoryReplanProposalRepository();
    }

    /**
     * The options are offered in this order: reschedule, replace a resource, cancel.
     */
    @Bean
    ProposeReplan proposeReplan(ExpeditionRepository expeditions, ReplanProposalRepository proposals,
                                ExpeditionReplanner replanner, ResourceCatalog catalog) {
        return new ProposeReplan(expeditions, proposals, replanner,
                List.of(new RescheduleOptions(), new ResourceReplacementOptions(catalog), new CancellationOptions()));
    }

    @Bean
    EventPublisher eventPublisher(List<EventSubscriber<?>> subscribers) {
        return new InMemoryEventPublisher(subscribers);
    }

    @Bean
    RegisterIncident registerIncident(ExpeditionRepository expeditions, EventPublisher publisher, ReplanProposalRepository proposals) {
        return new RegisterIncident(expeditions, publisher, proposals);
    }

    @Bean
    DecideReplanProposal decideReplanProposal(ReplanProposalRepository proposals, ExpeditionRepository expeditions,
                                              ExpeditionReplanner replanner, ReplanExpedition replanExpedition,
                                              ResourceCatalog catalog) {
        return new DecideReplanProposal(proposals, expeditions, replanner, replanExpedition, catalog);
    }

    @Bean
    QueryReplanProposals queryReplanProposals(ReplanProposalRepository proposals, ExpeditionRepository expeditions) {
        return new QueryReplanProposals(proposals, expeditions);
    }
}
