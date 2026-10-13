package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

public record ValidationFindingDto(String rule, ValidationSubjectDto subject, String message) {
}
