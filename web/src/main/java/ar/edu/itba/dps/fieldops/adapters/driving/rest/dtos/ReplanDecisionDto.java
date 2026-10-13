package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

/**
 * What a responsible decided about a proposal: who, why, when, which option and which alternative it
 * generated (D48). A decision with no option chosen is a rejection, which changes nothing in the
 * expedition (D47).
 *
 * @param chosenOptionNumber      the number of the accepted option, or {@code null} if the proposal was rejected
 * @param alternativeExpeditionId the expedition the acceptance created as a draft, or {@code null} if rejected
 */
public record ReplanDecisionDto(String responsibleId, String justification, LocalDateTime decidedAt,
                                Integer chosenOptionNumber, String alternativeExpeditionId) {
}
