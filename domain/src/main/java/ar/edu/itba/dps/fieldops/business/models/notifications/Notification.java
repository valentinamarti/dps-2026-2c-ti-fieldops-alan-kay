package ar.edu.itba.dps.fieldops.business.models.notifications;

import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

import java.time.LocalDateTime;
import java.util.Objects;

public record Notification(EventId event, ExpeditionId expedition, ResourceId responsible, String text,
                           LocalDateTime occurredAt) {

    public Notification {
        Objects.requireNonNull(event, "event is required");
        Objects.requireNonNull(expedition, "expedition is required");
        Objects.requireNonNull(responsible, "responsible is required");
        text = DomainArguments.requireText(text, "text");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }

    public static Notification of(DomainEvent event, ResourceId responsible) {
        return new Notification(event.id(), event.expeditionId(), responsible, event.summary(), event.occurredAt());
    }
}
