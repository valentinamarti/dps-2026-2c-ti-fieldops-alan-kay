package ar.edu.itba.dps.fieldops.business.models.report;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.events.ActivityCompleted;
import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

import java.time.LocalDateTime;
import java.util.Objects;

public record ProgressEntry(EventId event, ExpeditionId expedition, ActivityId activity, LocalDateTime occurredAt,
                            int finishedActivities, int scheduledActivities) {

    public ProgressEntry {
        Objects.requireNonNull(event, "event is required");
        Objects.requireNonNull(expedition, "expedition is required");
        Objects.requireNonNull(activity, "activity is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }

    public static ProgressEntry of(ActivityCompleted event, ExpeditionReport report) {
        return new ProgressEntry(event.id(), event.expeditionId(), event.activity(), event.occurredAt(),
                report.finishedActivities(), report.scheduledActivities());
    }
}
