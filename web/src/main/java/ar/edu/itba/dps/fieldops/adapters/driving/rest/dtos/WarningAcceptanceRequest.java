package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

/**
 * A warning a responsible accepts, with the reason why. {@code rule} and {@code subject} are the same shape
 * {@link ValidationFindingDto} already has, so this body can be built by copying a finding out of
 * {@code GET /api/expeditions/{id}/validation} and adding who accepts it and the justification (D57).
 */
public record WarningAcceptanceRequest(String rule, ValidationSubjectDto subject, String responsibleId,
                                       String justification) {
}
