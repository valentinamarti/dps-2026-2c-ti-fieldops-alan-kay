package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

/**
 * Rejecting a proposal: who decides, why and when. The three are the same ones an acceptance sends
 * ({@link AcceptReplanOptionRequest}), because rejecting is just as much a decision that stays audited (D47).
 *
 * <p>Who decides travels in the body and not in a credential, which is why a person who is not a responsible
 * of the expedition is a 422 and not a 403 (D49).
 */
public record ReplanDecisionRequest(String responsibleId, String justification, LocalDateTime decidedAt) {
}
