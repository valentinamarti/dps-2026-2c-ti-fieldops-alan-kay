package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

/**
 * A type of activity as the API publishes it: the name to ask for it by and the parameters it needs. It is
 * what tells a client what it can send in the body of {@code POST /api/expeditions/{id}/activities}.
 *
 * <p>The name comes normalized, because the domain compares it that way (D34, D44); the names of the
 * parameters do not, because they are identifiers and travel exactly as they have to be sent.
 */
public record ActivityTypeResponse(String name, List<String> parameterNames) {
}
