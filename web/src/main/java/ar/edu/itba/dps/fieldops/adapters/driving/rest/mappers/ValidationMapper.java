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

public final class ValidationMapper {
    private ValidationMapper() {
    }

    public static ValidationResponse toResponse(ApprovalResult result) {
        return new ValidationResponse(result.canBeApproved(), toDtos(result.criticals()), toDtos(result.warnings()));
    }

    private static List<ValidationFindingDto> toDtos(List<ValidationResult> findings) {
        return findings.stream().map(ValidationMapper::toDto).toList();
    }

    public static ValidationFindingDto toDto(ValidationResult finding) {
        return new ValidationFindingDto(finding.rule().value(), toDto(finding.subject()), finding.message());
    }

    private static ValidationSubjectDto toDto(ValidationSubject subject) {
        return new ValidationSubjectDto(
                subject.activity().map(ActivityId::value).orElse(null),
                subject.resource().map(ResourceId::value).orElse(null));
    }

    static ValidationSubject toSubject(ValidationSubjectDto dto) {
        return new ValidationSubject(
                Optional.ofNullable(dto.activityId()).map(ActivityId::new),
                Optional.ofNullable(dto.resourceId()).map(ResourceId::new));
    }
}
