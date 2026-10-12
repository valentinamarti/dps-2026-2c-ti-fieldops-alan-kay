package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

/**
 * One thing a validation rule found, with the rule that found it, what it is about and the message of the
 * domain. The severity is not a field: a finding comes in the list of criticals or in the list of warnings.
 *
 * @param rule the id of the rule, which is the one the approval has to name to accept a warning
 */
public record ValidationFindingDto(String rule, ValidationSubjectDto subject, String message) {
}
