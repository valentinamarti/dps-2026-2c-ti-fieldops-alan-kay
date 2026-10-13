package ar.edu.itba.dps.fieldops.business.interfaces.events;

public interface EventSubscriber<E extends DomainEvent> {
    Class<E> eventType();

    void on(E event);
}
