package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentId;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

import java.time.Duration;
import java.util.Optional;

final class IncidentMapper {
    private IncidentMapper() {
    }

    static IncidentColumns toColumns(Incident incident) {
        return new IncidentColumns(
                incident.id().expedition().value(),
                incident.id().number(),
                incident.activity().value(),
                incident.description(),
                incident.occurredAt(),
                incident.resource().map(ResourceId::value).orElse(null),
                incident.expectedDelay().map(Duration::toMinutes).orElse(null));
    }

    static Incident toDomain(IncidentColumns columns) {
        return new Incident(
                new IncidentId(new ExpeditionId(columns.getExpedition()), columns.getNumber()),
                new ActivityId(columns.getActivity()),
                columns.getDescription(),
                columns.getOccurredAt(),
                Optional.ofNullable(columns.getResource()).map(ResourceId::new),
                Optional.ofNullable(columns.getDelayMinutes()).map(Duration::ofMinutes));
    }
}
