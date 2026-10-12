package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

/**
 * The body of {@code POST /api/{staff|vehicles|instruments}/{id}/unavailabilities}: a period in which the
 * resource cannot be used, and why. Only reusable resources have one; a supply has stock, not availability.
 */
public record UnavailabilityDto(PeriodDto period, String reason) {
}
