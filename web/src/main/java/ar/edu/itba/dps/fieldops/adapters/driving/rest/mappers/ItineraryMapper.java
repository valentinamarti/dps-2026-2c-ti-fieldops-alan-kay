package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ItineraryNodeResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ItineraryResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ScheduleBlockRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ScheduledActivityResponse;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.commands.PlannedNodeCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.ScheduleBlockCommand;
import ar.edu.itba.dps.fieldops.business.models.responses.ItineraryNodeView;
import ar.edu.itba.dps.fieldops.business.models.responses.ItineraryView;

import java.util.List;
import java.util.Objects;

public final class ItineraryMapper {
    private ItineraryMapper() {
    }

    public static ScheduleBlockCommand toCommand(String expeditionId, ScheduleBlockRequest request) {
        return new ScheduleBlockCommand(new ExpeditionId(expeditionId), toNodeCommand(request));
    }

    public static ItineraryNodeResponse toResponse(ItineraryNodeView node) {
        return new ItineraryNodeResponse(
                node.type(),
                Mappings.toDto(node.scheduledPeriod()),
                node.estimatedDuration().toMinutes(),
                node.estimatedRisk(),
                node.activity().map(ActivityMapper::toResponse).orElse(null),
                node.children().isEmpty() ? null : node.children().stream().map(ItineraryMapper::toResponse).toList());
    }

    public static ItineraryResponse toResponse(ItineraryView itinerary) {
        return new ItineraryResponse(
                itinerary.expeditionId().value(),
                itinerary.nodes().stream().map(ItineraryMapper::toResponse).toList());
    }

    private static PlannedNodeCommand toNodeCommand(ScheduleBlockRequest node) {
        return node.activity() == null
                ? PlannedNodeCommand.block(node.type(), childrenOf(node))
                : PlannedNodeCommand.activity(ActivityMapper.toPlannedActivity(node.activity()));
    }

    private static List<PlannedNodeCommand> childrenOf(ScheduleBlockRequest node) {
        return Objects.requireNonNullElse(node.children(), List.<ScheduleBlockRequest>of()).stream()
                .map(ItineraryMapper::toNodeCommand)
                .toList();
    }
}
