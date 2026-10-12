package ar.edu.itba.dps.fieldops.business.providers;

import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;

/**
 * Output port to announce what happened. Delivery is synchronous: when {@code publish} returns, every subscriber
 * has already reacted (D45).
 */
public interface EventPublisher {

    void publish(DomainEvent event);
}
