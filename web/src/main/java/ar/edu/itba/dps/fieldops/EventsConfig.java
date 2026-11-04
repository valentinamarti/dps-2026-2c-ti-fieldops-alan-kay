package ar.edu.itba.dps.fieldops;

import ar.edu.itba.dps.fieldops.adapters.driven.EventSerializer;
import ar.edu.itba.dps.fieldops.adapters.driven.OutboxEventPublisher;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.DomainEventJpaRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.EventDeliveryJpaRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.JpaEventLog;
import ar.edu.itba.dps.fieldops.business.interfaces.events.AsyncEventSubscriber;
import ar.edu.itba.dps.fieldops.business.interfaces.events.EventSubscriber;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryExpeditionEventsPort;
import ar.edu.itba.dps.fieldops.business.providers.EventLog;
import ar.edu.itba.dps.fieldops.business.providers.EventPublisher;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.usecases.QueryExpeditionEvents;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

@Configuration
public class EventsConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    EventSerializer eventSerializer() {
        return new EventSerializer();
    }

    @Bean
    EventPublisher eventPublisher(List<EventSubscriber<?>> subscribers,
                                  ObjectProvider<AsyncEventSubscriber<?>> asyncSubscribers,
                                  DomainEventJpaRepository events, EventDeliveryJpaRepository deliveries,
                                  EventSerializer serializer, Clock clock) {
        return new OutboxEventPublisher(subscribers, asyncSubscribers.stream().toList(), events, deliveries,
                serializer, clock);
    }

    @Bean
    EventLog eventLog(DomainEventJpaRepository events, EventDeliveryJpaRepository deliveries) {
        return new JpaEventLog(events, deliveries);
    }

    @Bean
    QueryExpeditionEventsPort queryExpeditionEvents(EventLog events, ExpeditionRepository expeditions,
                                                    TransactionalUseCases transactional) {
        return transactional.reading(QueryExpeditionEventsPort.class, new QueryExpeditionEvents(events, expeditions));
    }
}
