package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.AcceptReplanOptionRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.IncidentDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterIncidentRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ReplanDecisionDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ReplanDecisionRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ReplanOptionDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ReplanProposalResponse;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanDecision;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanOption;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterIncidentCommand;
import ar.edu.itba.dps.fieldops.business.usecases.ReplanDecisionCommand;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public final class ReplanningMapper {
    private ReplanningMapper() {
    }

    public static RegisterIncidentCommand toCommand(String expeditionId, RegisterIncidentRequest request) {
        return new RegisterIncidentCommand(new ExpeditionId(expeditionId), new ActivityId(request.activityId()),
                toReport(request));
    }

    public static ReplanDecisionCommand toCommand(AcceptReplanOptionRequest request) {
        return new ReplanDecisionCommand(toResponsible(request.responsibleId()), request.justification(),
                request.decidedAt());
    }

    public static ReplanDecisionCommand toCommand(ReplanDecisionRequest request) {
        return new ReplanDecisionCommand(toResponsible(request.responsibleId()), request.justification(),
                request.decidedAt());
    }

    public static ReplanProposalResponse toResponse(ReplanProposal proposal) {
        return new ReplanProposalResponse(
                proposal.getId().value(),
                proposal.getOriginalExpedition().value(),
                proposal.getStatus(),
                toDto(proposal.getIncident()),
                toDtos(proposal.getOptions()),
                proposal.getDecision().map(decision -> toDto(proposal, decision)).orElse(null),
                proposal.getSupersededBy().map(ReplanProposalId::value).orElse(null));
    }

    static IncidentDto toDto(Incident incident) {
        return new IncidentDto(
                incident.id().toString(),
                incident.activity().value(),
                incident.description(),
                incident.occurredAt(),
                incident.resource().map(ResourceId::value).orElse(null),
                incident.expectedDelay().map(Duration::toMinutes).orElse(null));
    }

    private static List<ReplanOptionDto> toDtos(List<ReplanOption> options) {
        return IntStream.rangeClosed(1, options.size())
                .mapToObj(number -> new ReplanOptionDto(number, options.get(number - 1).summary()))
                .toList();
    }

    private static ReplanDecisionDto toDto(ReplanProposal proposal, ReplanDecision decision) {
        return new ReplanDecisionDto(
                decision.responsible().getId().value(),
                decision.justification(),
                decision.decidedAt(),
                proposal.getChosenOptionNumber().orElse(null),
                proposal.getAlternative().map(ExpeditionId::value).orElse(null));
    }

    private static ResourceId toResponsible(String responsibleId) {
        return responsibleId == null ? null : new ResourceId(responsibleId);
    }

    private static IncidentReport toReport(RegisterIncidentRequest request) {
        final var reported = IncidentReport.of(request.description(), request.occurredAt());
        final var affecting = Optional.ofNullable(request.resourceId())
                .map(resource -> reported.affecting(new ResourceId(resource)))
                .orElse(reported);
        return Optional.ofNullable(request.expectedDelayMinutes())
                .map(minutes -> affecting.delayingBy(Duration.ofMinutes(minutes)))
                .orElse(affecting);
    }
}
