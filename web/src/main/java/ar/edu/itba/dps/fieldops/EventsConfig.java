package ar.edu.itba.dps.fieldops;

import ar.edu.itba.dps.fieldops.adapters.driven.EventDispatcher;
import ar.edu.itba.dps.fieldops.adapters.driven.EventSerializer;
import ar.edu.itba.dps.fieldops.adapters.driven.OutboxEventPublisher;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.DomainEventJpaRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.EventDeliveryJpaRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.JpaEventLog;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.JpaNotificationRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.JpaProgressHistory;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.NotificationJpaRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.ProgressEntryJpaRepository;
import ar.edu.itba.dps.fieldops.business.interfaces.events.AsyncEventSubscriber;
import ar.edu.itba.dps.fieldops.business.interfaces.events.EventSubscriber;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryExpeditionEventsPort;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryNotificationsPort;
import ar.edu.itba.dps.fieldops.business.providers.EventLog;
import ar.edu.itba.dps.fieldops.business.providers.EventPublisher;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.NotificationRepository;
import ar.edu.itba.dps.fieldops.business.providers.ProgressHistory;
import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;
import ar.edu.itba.dps.fieldops.business.usecases.NotifyResponsibles;
import ar.edu.itba.dps.fieldops.business.usecases.QueryExpeditionEvents;
import ar.edu.itba.dps.fieldops.business.usecases.QueryNotifications;
import ar.edu.itba.dps.fieldops.business.usecases.TrackProgress;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.util.List;

@Configuration
@EnableConfigurationProperties(EventsProperties.class)
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

    @Bean
    EventDispatcher eventDispatcher(ObjectProvider<AsyncEventSubscriber<?>> asyncSubscribers,
                                    DomainEventJpaRepository events, EventDeliveryJpaRepository deliveries,
                                    EventSerializer serializer, PlatformTransactionManager transactions,
                                    EventsProperties properties, Clock clock) {
        return new EventDispatcher(asyncSubscribers.stream().toList(), events, deliveries, serializer, transactions,
                properties.retryPolicy(), clock);
    }

    @Bean
    NotificationRepository notificationRepository(NotificationJpaRepository notifications) {
        return new JpaNotificationRepository(notifications);
    }

    @Bean
    NotifyResponsibles notifyResponsibles(ExpeditionRepository expeditions, NotificationRepository notifications) {
        return new NotifyResponsibles(expeditions, notifications);
    }

    @Bean
    QueryNotificationsPort queryNotifications(NotificationRepository notifications, ExpeditionRepository expeditions,
                                              TransactionalUseCases transactional) {
        return transactional.reading(QueryNotificationsPort.class, new QueryNotifications(notifications, expeditions));
    }

    @Bean
    ProgressHistory progressHistory(ProgressEntryJpaRepository entries) {
        return new JpaProgressHistory(entries);
    }

    @Bean
    TrackProgress trackProgress(ExpeditionRepository expeditions, ExpeditionReporter reporter, ProgressHistory history) {
        return new TrackProgress(expeditions, reporter, history);
    }
}
