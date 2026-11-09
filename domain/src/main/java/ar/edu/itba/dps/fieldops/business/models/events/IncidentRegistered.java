package ar.edu.itba.dps.fieldops.business.models.events;

import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;

import java.time.LocalDateTime;
import java.util.Objects;

public record IncidentRegistered(EventId id, ExpeditionId expeditionId, Incident incident) implements DomainEvent {

    public IncidentRegistered {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(expeditionId, "expeditionId is required");
        Objects.requireNonNull(incident, "incident is required");
    }

    @Override
    public LocalDateTime occurredAt() {
        return incident.occurredAt();
    }

    @Override
    public String summary() {
        return "Incident on activity %s: %s".formatted(incident.activity(), incident.description());
    }
}
