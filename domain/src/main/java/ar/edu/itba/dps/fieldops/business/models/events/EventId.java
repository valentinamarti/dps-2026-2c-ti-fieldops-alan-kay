package ar.edu.itba.dps.fieldops.business.models.events;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

import java.util.UUID;

public record EventId(String value) {

    public EventId {
        value = DomainArguments.requireText(value, "id");
    }

    public static EventId generate() {
        return new EventId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
