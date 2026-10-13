package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

public record AcceptedWarningDto(ValidationFindingDto warning, String responsibleId, String justification) {
}
