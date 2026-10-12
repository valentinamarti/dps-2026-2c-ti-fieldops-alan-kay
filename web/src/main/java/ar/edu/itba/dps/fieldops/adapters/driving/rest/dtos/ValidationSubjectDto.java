package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

/**
 * What a validation finding is about: an activity, a resource, both, or neither when it is about the whole
 * expedition. The part that does not take part is {@code null}, because the rule and the subject together are
 * what identifies a finding (D32) and what the approval has to name in order to accept a warning.
 */
public record ValidationSubjectDto(String activityId, String resourceId) {
}
