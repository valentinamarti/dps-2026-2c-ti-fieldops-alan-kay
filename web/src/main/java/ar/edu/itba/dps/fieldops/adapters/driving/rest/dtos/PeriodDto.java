package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

/**
 * A time window as it travels over HTTP, in both directions. The domain {@code TimePeriod} never leaves the
 * hexagon: the mapper builds it, and building it is what rejects an end that is not after the start.
 */
public record PeriodDto(LocalDateTime start, LocalDateTime end) {
}
