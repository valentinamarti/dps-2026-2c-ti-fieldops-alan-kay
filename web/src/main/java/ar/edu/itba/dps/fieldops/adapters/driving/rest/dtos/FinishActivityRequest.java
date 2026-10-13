package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

/**
 * The body of {@code POST .../activities/{activityId}/completion}: when the activity actually finished and
 * what came out of it (D57). Finishing consumes the supplies it actually used (domain), so the actual result
 * is what the report shows later.
 */
public record FinishActivityRequest(LocalDateTime at, String result) {
}
