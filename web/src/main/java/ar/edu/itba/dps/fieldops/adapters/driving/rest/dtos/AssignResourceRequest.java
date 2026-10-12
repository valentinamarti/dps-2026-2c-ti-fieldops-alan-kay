package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

/**
 * The body of {@code POST .../staff}, {@code .../equipment} and {@code .../supplies}: the id of a resource
 * already in the catalog. The three assignments share this shape, so one request DTO covers all of them.
 */
public record AssignResourceRequest(String resourceId) {
}
