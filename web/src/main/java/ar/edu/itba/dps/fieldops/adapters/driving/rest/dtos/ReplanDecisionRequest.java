package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

public record ReplanDecisionRequest(String responsibleId, String justification, LocalDateTime decidedAt) {
}
