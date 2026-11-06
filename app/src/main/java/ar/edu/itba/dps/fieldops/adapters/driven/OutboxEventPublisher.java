package ar.edu.itba.dps.fieldops.adapters.driven;

import ar.edu.itba.dps.fieldops.adapters.driven.persistence.DomainEventEntity;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.DomainEventJpaRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.EventDeliveryEntity;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.EventDeliveryJpaRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.EventDeliveryKey;
import ar.edu.itba.dps.fieldops.business.interfaces.events.AsyncEventSubscriber;
import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.interfaces.events.EventSubscriber;
import ar.edu.itba.dps.fieldops.business.models.events.ConsumerName;
import ar.edu.itba.dps.fieldops.business.models.events.DeliveryStatus;
import ar.edu.itba.dps.fieldops.business.providers.EventPublisher;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class OutboxEventPublisher implements EventPublisher {
    private final List<EventSubscriber<?>> subscribers;
    private final List<AsyncEventSubscriber<?>> asyncSubscribers;
    private final DomainEventJpaRepository events;
    private final EventDeliveryJpaRepository deliveries;
    private final EventSerializer serializer;
    private final Clock clock;

    public OutboxEventPublisher(List<EventSubscriber<?>> subscribers, List<AsyncEventSubscriber<?>> asyncSubscribers,
                                DomainEventJpaRepository events, EventDeliveryJpaRepository deliveries,
                                EventSerializer serializer, Clock clock) {
        this.subscribers = List.copyOf(Objects.requireNonNull(subscribers, "subscribers are required"));
        this.asyncSubscribers = List.copyOf(Objects.requireNonNull(asyncSubscribers, "async subscribers are required"));
        this.events = Objects.requireNonNull(events, "events are required");
        this.deliveries = Objects.requireNonNull(deliveries, "deliveries are required");
        this.serializer = Objects.requireNonNull(serializer, "serializer is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "event is required");
        subscribers.forEach(subscriber -> deliver(subscriber, event));
        final var recorded = recorded(event);
        asyncSubscribers.stream()
                .filter(subscriber -> subscriber.eventType().isInstance(event))
                .forEach(subscriber -> leavePendingFor(recorded, subscriber.consumerName()));
    }

    private <E extends DomainEvent> void deliver(EventSubscriber<E> subscriber, DomainEvent event) {
        if (subscriber.eventType().isInstance(event)) {
            subscriber.on(subscriber.eventType().cast(event));
        }
    }

    private DomainEventEntity recorded(DomainEvent event) {
        final var entity = new DomainEventEntity();
        entity.setEventId(event.id().value());
        entity.setExpeditionId(event.expeditionId().value());
        entity.setEventType(serializer.typeOf(event));
        entity.setOccurredAt(event.occurredAt());
        entity.setRecordedAt(LocalDateTime.now(clock));
        entity.setPayload(serializer.serialize(event));
        return events.save(entity);
    }

    private void leavePendingFor(DomainEventEntity recorded, ConsumerName consumer) {
        final var delivery = new EventDeliveryEntity();
        delivery.setId(new EventDeliveryKey(recorded.getSequence(), consumer.value()));
        delivery.setStatus(DeliveryStatus.PENDING.name());
        delivery.setAttempts(0);
        delivery.setNextAttemptAt(recorded.getRecordedAt());
        deliveries.save(delivery);
    }
}
