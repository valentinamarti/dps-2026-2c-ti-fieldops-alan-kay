package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

/**
 * The body of {@code POST /api/expeditions/{id}/approval}: the warnings a responsible accepts, each with its
 * justification (D57). An expedition with no warnings left to accept is approved with an empty list.
 */
public record ApprovalRequest(List<WarningAcceptanceRequest> acceptedWarnings) {
}
