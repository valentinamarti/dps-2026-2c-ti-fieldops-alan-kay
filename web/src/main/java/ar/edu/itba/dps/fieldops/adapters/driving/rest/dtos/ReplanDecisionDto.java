package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

public record ReplanDecisionDto(String responsibleId, String justification, LocalDateTime decidedAt,
                                Integer chosenOptionNumber, String alternativeExpeditionId) {
}
