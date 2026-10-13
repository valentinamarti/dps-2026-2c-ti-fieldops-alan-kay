package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

public record WarningAcceptanceRequest(String rule, ValidationSubjectDto subject, String responsibleId,
                                       String justification) {
}
