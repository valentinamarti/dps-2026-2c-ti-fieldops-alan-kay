package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

public record RegisterIncidentRequest(String activityId, String description, LocalDateTime occurredAt,
                                      String resourceId, Long expectedDelayMinutes) {
}
