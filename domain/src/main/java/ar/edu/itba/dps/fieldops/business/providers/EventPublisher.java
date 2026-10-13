package ar.edu.itba.dps.fieldops.business.providers;

import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;

public interface EventPublisher {
    void publish(DomainEvent event);
}
