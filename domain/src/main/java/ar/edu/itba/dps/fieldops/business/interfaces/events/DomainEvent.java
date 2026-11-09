package ar.edu.itba.dps.fieldops.business.interfaces.events;

import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

import java.time.LocalDateTime;

public interface DomainEvent {
    EventId id();

    ExpeditionId expeditionId();

    LocalDateTime occurredAt();

    String summary();
}
