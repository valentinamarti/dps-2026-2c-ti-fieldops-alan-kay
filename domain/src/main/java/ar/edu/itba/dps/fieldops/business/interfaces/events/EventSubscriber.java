package ar.edu.itba.dps.fieldops.business.interfaces.events;

/**
 * Reacts to one kind of event. The publisher hands it every event of the type it declares.
 */
public interface EventSubscriber<E extends DomainEvent> {

    Class<E> eventType();

    void on(E event);
}
