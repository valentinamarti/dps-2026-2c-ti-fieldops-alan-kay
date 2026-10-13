package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ValidationFindingDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ValidationResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ValidationSubjectDto;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.validation.ApprovalResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;

import java.util.List;
import java.util.Optional;

/**
 * Turns the result of the approval rules into the answer of the validation (D49). It only splits what the
 * domain already distinguishes by severity (D22): the rules, the subjects and the messages are the ones of the
 * domain, in the order the policy ran them.
 *
 * <p>{@code toDto(ValidationResult)} and {@code toSubject} are package-private and not private: the approval
 * (D57) and the report reuse them, the former to accept a warning by copying a finding it already has and the
 * latter to show one back, with exactly the same translation the validation uses (D56's precedent for sharing
 * a mapper's private pieces).
 */
public final class ValidationMapper {

    private ValidationMapper() {
    }

    public static ValidationResponse toResponse(ApprovalResult result) {
        return new ValidationResponse(result.canBeApproved(), toDtos(result.criticals()), toDtos(result.warnings()));
    }

    private static List<ValidationFindingDto> toDtos(List<ValidationResult> findings) {
        return findings.stream().map(ValidationMapper::toDto).toList();
    }

    static ValidationFindingDto toDto(ValidationResult finding) {
        return new ValidationFindingDto(finding.rule().value(), toDto(finding.subject()), finding.message());
    }

    private static ValidationSubjectDto toDto(ValidationSubject subject) {
        return new ValidationSubjectDto(
                subject.activity().map(ActivityId::value).orElse(null),
                subject.resource().map(ResourceId::value).orElse(null));
    }

    /**
     * The inverse of {@link #toDto(ValidationSubject)}, for the approval (D57): a warning is accepted by
     * sending back the same subject {@code GET .../validation} showed for it.
     */
    static ValidationSubject toSubject(ValidationSubjectDto dto) {
        return new ValidationSubject(
                Optional.ofNullable(dto.activityId()).map(ActivityId::new),
                Optional.ofNullable(dto.resourceId()).map(ResourceId::new));
    }
}
