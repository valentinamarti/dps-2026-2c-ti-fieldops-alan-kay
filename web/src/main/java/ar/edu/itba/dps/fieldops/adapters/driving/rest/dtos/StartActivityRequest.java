package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

/**
 * The body of {@code POST .../activities/{activityId}/start}: when the activity actually began (D57).
 */
public record StartActivityRequest(LocalDateTime at) {
}
