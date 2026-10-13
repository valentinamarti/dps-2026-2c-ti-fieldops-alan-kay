package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

/**
 * Accepting one option of a proposal: which one, and the same decision a rejection carries (D47). It repeats
 * the three fields of {@link ReplanDecisionRequest} instead of nesting them, so each body is flat and has
 * exactly the fields its operation uses: a rejection has no option to choose (D58).
 *
 * @param optionNumber the number of the option, from 1, as {@code GET /api/replan-proposals/{id}} lists them.
 *                     It is a primitive because the domain takes it as one: a number that is missing or out of
 *                     range is the domain's own "there is no replan option n of the proposal" (404)
 */
public record AcceptReplanOptionRequest(int optionNumber, String responsibleId, String justification,
                                        LocalDateTime decidedAt) {
}
