package ar.edu.itba.dps.fieldops.adapters.driven;

import ar.edu.itba.dps.fieldops.adapters.driven.persistence.DomainEventJpaRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.EventDeliveryEntity;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.EventDeliveryJpaRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.EventDeliveryKey;
import ar.edu.itba.dps.fieldops.business.interfaces.events.AsyncEventSubscriber;
import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.models.events.ConsumerName;
import ar.edu.itba.dps.fieldops.business.models.events.DeliveryStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class EventDispatcher {
    private static final int MAX_ERROR_LENGTH = 1000;

    private final Map<ConsumerName, AsyncEventSubscriber<?>> subscribers;
    private final DomainEventJpaRepository events;
    private final EventDeliveryJpaRepository deliveries;
    private final EventSerializer serializer;
    private final TransactionTemplate transactions;
    private final RetryPolicy retries;
    private final Clock clock;

    public EventDispatcher(List<AsyncEventSubscriber<?>> subscribers, DomainEventJpaRepository events,
                           EventDeliveryJpaRepository deliveries, EventSerializer serializer,
                           PlatformTransactionManager transactions, RetryPolicy retries, Clock clock) {
        this.subscribers = Objects.requireNonNull(subscribers, "subscribers are required").stream()
                .collect(Collectors.toUnmodifiableMap(AsyncEventSubscriber::consumerName, subscriber -> subscriber));
        this.events = Objects.requireNonNull(events, "events are required");
        this.deliveries = Objects.requireNonNull(deliveries, "deliveries are required");
        this.serializer = Objects.requireNonNull(serializer, "serializer is required");
        this.transactions = new TransactionTemplate(Objects.requireNonNull(transactions, "transactions are required"));
        this.retries = Objects.requireNonNull(retries, "retries are required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public void dispatchPending() {
        final var now = LocalDateTime.now(clock);
        for (var ready = deliveries.findReadyToDeliver(now); !ready.isEmpty(); ready = deliveries.findReadyToDeliver(now)) {
            ready.forEach(this::attempt);
        }
    }

    private void attempt(EventDeliveryKey key) {
        try {
            transactions.executeWithoutResult(_ -> pending(key).ifPresent(this::deliver));
        } catch (RuntimeException failure) {
            transactions.executeWithoutResult(_ -> pending(key).ifPresent(delivery -> recordFailure(delivery, failure)));
        }
    }

    private Optional<EventDeliveryEntity> pending(EventDeliveryKey key) {
        return deliveries.findById(key).filter(delivery -> DeliveryStatus.PENDING.name().equals(delivery.getStatus()));
    }

    private void deliver(EventDeliveryEntity delivery) {
        final var consumer = delivery.getId().getConsumer();
        final var subscriber = subscribers.get(new ConsumerName(consumer));
        if (subscriber == null) {
            giveUp(delivery, "There is no async subscriber named %s, so nobody can receive this event".formatted(consumer));
            return;
        }
        final DomainEvent event;
        try {
            event = recordedEvent(delivery);
        } catch (UnknownEventTypeException unreadable) {
            giveUp(delivery, describe(unreadable));
            return;
        }
        handOver(subscriber, event);
        delivery.setAttempts(delivery.getAttempts() + 1);
        delivery.setStatus(DeliveryStatus.DELIVERED.name());
        delivery.setDeliveredAt(LocalDateTime.now(clock));
        deliveries.save(delivery);
    }

    private DomainEvent recordedEvent(EventDeliveryEntity delivery) {
        final var recorded = events.findById(delivery.getId().getEventSequence()).orElseThrow();
        return serializer.deserialize(recorded.getEventType(), recorded.getPayload());
    }

    private static <E extends DomainEvent> void handOver(AsyncEventSubscriber<E> subscriber, DomainEvent event) {
        subscriber.on(subscriber.eventType().cast(event));
    }

    private void recordFailure(EventDeliveryEntity delivery, RuntimeException failure) {
        final var attempts = delivery.getAttempts() + 1;
        delivery.setAttempts(attempts);
        delivery.setLastError(describe(failure));
        if (retries.exhaustedAfter(attempts)) {
            delivery.setStatus(DeliveryStatus.FAILED.name());
        } else {
            delivery.setNextAttemptAt(LocalDateTime.now(clock).plus(retries.backoffAfter(attempts)));
        }
        deliveries.save(delivery);
    }

    private void giveUp(EventDeliveryEntity delivery, String reason) {
        delivery.setAttempts(delivery.getAttempts() + 1);
        delivery.setLastError(reason);
        delivery.setStatus(DeliveryStatus.FAILED.name());
        deliveries.save(delivery);
    }

    private static String describe(RuntimeException failure) {
        final var message = failure.getMessage();
        final var text = message == null || message.isBlank() ? failure.getClass().getName() : message;
        return text.codePointCount(0, text.length()) <= MAX_ERROR_LENGTH
                ? text
                : text.substring(0, text.offsetByCodePoints(0, MAX_ERROR_LENGTH));
    }
}
