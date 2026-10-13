package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ApprovalRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.WarningAcceptanceRequest;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.validation.RuleId;
import ar.edu.itba.dps.fieldops.business.usecases.WarningAcceptance;

import java.util.List;
import java.util.Objects;

public final class ApprovalMapper {
    private ApprovalMapper() {
    }

    public static List<WarningAcceptance> toAcceptances(ApprovalRequest request) {
        return Objects.requireNonNullElse(request.acceptedWarnings(), List.<WarningAcceptanceRequest>of()).stream()
                .map(ApprovalMapper::toAcceptance)
                .toList();
    }

    private static WarningAcceptance toAcceptance(WarningAcceptanceRequest request) {
        return new WarningAcceptance(
                new RuleId(request.rule()),
                ValidationMapper.toSubject(request.subject()),
                new ResourceId(request.responsibleId()),
                request.justification());
    }
}
