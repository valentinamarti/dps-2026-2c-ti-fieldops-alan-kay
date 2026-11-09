package ar.edu.itba.dps.fieldops.business.models.events;

import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

import java.time.LocalDateTime;
import java.util.Objects;

public record ActivityCompleted(EventId id, ExpeditionId expeditionId, ActivityId activity,
                                LocalDateTime occurredAt, String result) implements DomainEvent {

    public ActivityCompleted {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(expeditionId, "expeditionId is required");
        Objects.requireNonNull(activity, "activity is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        result = DomainArguments.requireText(result, "result");
    }

    @Override
    public String summary() {
        return "Activity %s finished: %s".formatted(activity, result);
    }
}
