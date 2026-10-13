package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

/**
 * An incident registered against a scheduled activity, as the report shows it (D57). It is the first time an
 * incident travels over HTTP; F3 reuses this same DTO for its own endpoints.
 *
 * @param id                 the domain's {@code IncidentId}, which already reads as a path ("e-1/incidents/1")
 * @param resourceId         the resource the incident is about, or {@code null} when it is not about one
 * @param expectedDelayMinutes the expected delay in minutes, or {@code null} when the incident does not delay
 *                             anything, so the unit stays in the name of the field like every other duration
 */
public record IncidentDto(String id, String activityId, String description, LocalDateTime occurredAt,
                          String resourceId, Long expectedDelayMinutes) {
}
