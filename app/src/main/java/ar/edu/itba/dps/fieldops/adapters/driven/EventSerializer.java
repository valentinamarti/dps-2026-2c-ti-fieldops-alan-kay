package ar.edu.itba.dps.fieldops.adapters.driven;

import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;

public class EventSerializer {
    private final JsonMapper json = JsonMapper.builder().build();

    public String typeOf(DomainEvent event) {
        Objects.requireNonNull(event, "event is required");
        return event.getClass().getName();
    }

    public String serialize(DomainEvent event) {
        Objects.requireNonNull(event, "event is required");
        return json.writeValueAsString(event);
    }

    public DomainEvent deserialize(String type, String payload) {
        Objects.requireNonNull(payload, "payload is required");
        return json.readValue(payload, eventTypeNamed(type));
    }

    private Class<? extends DomainEvent> eventTypeNamed(String type) {
        Objects.requireNonNull(type, "type is required");
        final Class<?> named;
        try {
            named = Class.forName(type);
        } catch (ClassNotFoundException missing) {
            throw new UnknownEventTypeException(type, missing);
        }
        if (!DomainEvent.class.isAssignableFrom(named)) {
            throw new UnknownEventTypeException(type);
        }
        return named.asSubclass(DomainEvent.class);
    }
}
