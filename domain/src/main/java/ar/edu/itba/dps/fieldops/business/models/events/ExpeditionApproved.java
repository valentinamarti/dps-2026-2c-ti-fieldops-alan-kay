package ar.edu.itba.dps.fieldops.business.models.events;

import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

import java.time.LocalDateTime;
import java.util.Objects;

public record ExpeditionApproved(EventId id, ExpeditionId expeditionId, LocalDateTime occurredAt) implements DomainEvent {

    public ExpeditionApproved {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(expeditionId, "expeditionId is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }

    @Override
    public String summary() {
        return "The expedition was approved";
    }
}
