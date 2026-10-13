package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

/**
 * What someone in the field reports about a scheduled activity (D58). It has the same fields as the
 * {@link IncidentDto} that comes back, minus the id, which the expedition puts (D48).
 *
 * @param resourceId           the resource that failed, which has to be one assigned to the activity, or
 *                             {@code null} when the incident is not about a resource
 * @param expectedDelayMinutes how long the activity is expected to be delayed, or {@code null} when the
 *                             incident does not delay it. Without it there is no rescheduling to offer (D46)
 */
public record RegisterIncidentRequest(String activityId, String description, LocalDateTime occurredAt,
                                      String resourceId, Long expectedDelayMinutes) {
}
