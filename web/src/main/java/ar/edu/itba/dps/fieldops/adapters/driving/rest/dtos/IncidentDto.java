package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

public record IncidentDto(String id, String activityId, String description, LocalDateTime occurredAt,
                          String resourceId, Long expectedDelayMinutes) {
}
