package ar.edu.itba.dps.fieldops.business.models.events;

import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;

import java.util.Objects;

public record IncidentRegistered(ExpeditionId expedition, Incident incident) implements DomainEvent {

    public IncidentRegistered {
        Objects.requireNonNull(expedition, "expedition is required");
        Objects.requireNonNull(incident, "incident is required");
    }
}
