package ar.edu.itba.dps.fieldops;

import ar.edu.itba.dps.fieldops.adapters.driven.InMemoryEventPublisher;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.JpaReplanProposalRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.LoadedAggregates;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.ReplanProposalJpaRepository;
import ar.edu.itba.dps.fieldops.business.interfaces.events.EventSubscriber;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.ports.in.DecideReplanProposalPort;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryReplanProposalsPort;
import ar.edu.itba.dps.fieldops.business.ports.in.RegisterIncidentPort;
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

@Configuration
public class ReplanningConfig {
    @Bean
    ReplanProposalRepository replanProposalRepository(ReplanProposalJpaRepository proposals, ResourceCatalog catalog,
                                                      LoadedAggregates loaded) {
        return new JpaReplanProposalRepository(proposals, catalog, loaded);
    }

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
    RegisterIncidentPort registerIncident(ExpeditionRepository expeditions, EventPublisher publisher, ReplanProposalRepository proposals, TransactionalUseCases transactional) {
        return transactional.writing(RegisterIncidentPort.class, new RegisterIncident(expeditions, publisher, proposals));
    }

    @Bean
    DecideReplanProposalPort decideReplanProposal(ReplanProposalRepository proposals, ExpeditionRepository expeditions,
                                              ExpeditionReplanner replanner, ReplanExpedition replanExpedition,
                                              ResourceCatalog catalog, TransactionalUseCases transactional) {
        return transactional.writing(DecideReplanProposalPort.class, new DecideReplanProposal(proposals, expeditions, replanner, replanExpedition, catalog));
    }

    @Bean
    QueryReplanProposalsPort queryReplanProposals(ReplanProposalRepository proposals, ExpeditionRepository expeditions, TransactionalUseCases transactional) {
        return transactional.reading(QueryReplanProposalsPort.class, new QueryReplanProposals(proposals, expeditions));
    }
}
