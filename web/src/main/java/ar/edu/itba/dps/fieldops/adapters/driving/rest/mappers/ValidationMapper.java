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

/**
 * Turns the result of the approval rules into the answer of the validation (D49). It only splits what the
 * domain already distinguishes by severity (D22): the rules, the subjects and the messages are the ones of the
 * domain, in the order the policy ran them.
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

    private static ValidationFindingDto toDto(ValidationResult finding) {
        return new ValidationFindingDto(finding.rule().value(), toDto(finding.subject()), finding.message());
    }

    private static ValidationSubjectDto toDto(ValidationSubject subject) {
        return new ValidationSubjectDto(
                subject.activity().map(ActivityId::value).orElse(null),
                subject.resource().map(ResourceId::value).orElse(null));
    }
}
