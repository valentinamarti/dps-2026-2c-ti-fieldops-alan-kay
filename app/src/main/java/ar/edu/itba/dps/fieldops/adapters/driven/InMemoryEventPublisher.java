package ar.edu.itba.dps.fieldops.adapters.driven;

import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.interfaces.events.EventSubscriber;
import ar.edu.itba.dps.fieldops.business.providers.EventPublisher;

import java.util.List;
import java.util.Objects;

/**
 * Hands each event to every subscriber of its type, in the order they were given, before returning (D45).
 * There are no retries: if a subscriber fails, the exception reaches whoever published.
 */
public class InMemoryEventPublisher implements EventPublisher {

    private final List<EventSubscriber<?>> subscribers;

    public InMemoryEventPublisher(List<EventSubscriber<?>> subscribers) {
        this.subscribers = List.copyOf(Objects.requireNonNull(subscribers, "subscribers are required"));
    }

    @Override
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "event is required");
        subscribers.forEach(subscriber -> deliver(subscriber, event));
    }

    private <E extends DomainEvent> void deliver(EventSubscriber<E> subscriber, DomainEvent event) {
        if (subscriber.eventType().isInstance(event)) {
            subscriber.on(subscriber.eventType().cast(event));
        }
    }
}
