package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

/**
 * A warning that was accepted during approval, as the report shows it (D57). {@code warning} reuses
 * {@link ValidationFindingDto}, the same shape the responsible copied out of the validation to accept it in the
 * first place, so the report closes the loop the approval opened.
 */
public record AcceptedWarningDto(ValidationFindingDto warning, String responsibleId, String justification) {
}
