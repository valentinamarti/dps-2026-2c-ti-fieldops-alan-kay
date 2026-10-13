package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

/**
 * The body of {@code POST .../activities/{activityId}/observations}: a note recorded while the activity is
 * tracked, and when it was recorded (D57). It travels with the same two fields as the domain's
 * {@code Observation}, because there is nothing else to translate.
 */
public record ObservationRequest(String note, LocalDateTime recordedAt) {
}
