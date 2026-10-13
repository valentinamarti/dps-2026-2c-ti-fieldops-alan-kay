package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalStatus;

import java.util.List;

public record ReplanProposalResponse(String id, String originalExpeditionId, ReplanProposalStatus status,
                                     IncidentDto incident, List<ReplanOptionDto> options,
                                     ReplanDecisionDto decision, String supersededByProposalId) {
}
