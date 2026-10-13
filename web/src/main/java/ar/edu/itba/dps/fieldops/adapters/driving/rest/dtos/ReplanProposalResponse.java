package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalStatus;

import java.util.List;

/**
 * A replan proposal as the API shows it, which is also the audit record of a replanning (D48, D58): the
 * expedition it is about, the incident that motivated it, the options offered and what was decided.
 *
 * <p>The affected activity is not a field of its own: it is {@code incident.activityId}, which is where the
 * domain keeps it too ({@code ReplanProposal.getAffectedActivity} reads it off the incident). The original
 * plan is not copied in either: it is the original expedition, read with
 * {@code GET /api/expeditions/{originalExpeditionId}} and its itinerary (D48).
 *
 * @param decision              {@code null} while the proposal is pending
 * @param supersededByProposalId the proposal whose acceptance left this one without effect, or {@code null}.
 *                              It is outside {@code decision} because nobody decided it: it is what happened
 *                              to this proposal because another one of the same expedition was accepted (D47)
 */
public record ReplanProposalResponse(String id, String originalExpeditionId, ReplanProposalStatus status,
                                     IncidentDto incident, List<ReplanOptionDto> options,
                                     ReplanDecisionDto decision, String supersededByProposalId) {
}
