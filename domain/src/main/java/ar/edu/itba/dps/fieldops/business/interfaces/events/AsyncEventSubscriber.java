package ar.edu.itba.dps.fieldops.business.interfaces.events;

import ar.edu.itba.dps.fieldops.business.models.events.ConsumerName;

public interface AsyncEventSubscriber<E extends DomainEvent> {
    Class<E> eventType();

    ConsumerName consumerName();

    void on(E event);
}
