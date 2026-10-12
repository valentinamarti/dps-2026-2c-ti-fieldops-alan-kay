package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ItineraryNodeResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ItineraryResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ScheduleBlockRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ScheduledActivityResponse;
import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;
import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNodeVisitor;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.usecases.PlannedNodeCommand;
import ar.edu.itba.dps.fieldops.business.usecases.ScheduleBlockCommand;

import java.util.List;
import java.util.Objects;

/**
 * Translates between the itinerary DTOs and the domain, in both directions (D49, D54).
 *
 * <p>On the way out it is a visitor: every node of the tree calls the method of its own kind, so the mapper
 * never asks what kind of node it has in its hands and no node has to publish its children.
 *
 * <p>On the way in, the only thing it decides is whether a node of the body is a leaf, which is what bringing
 * an activity means. The names of the kinds of block come from the domain, and so does the rejection of a name
 * that is not one of them: the adapter does not validate (D49).
 */
public final class ItineraryMapper {

    private static final String ACTIVITY = "activity";

    private static final ItineraryNodeVisitor<ItineraryNodeResponse> AS_RESPONSE =
            new ItineraryNodeVisitor<ItineraryNodeResponse>() {

                @Override
                public ItineraryNodeResponse activity(ItineraryItem item) {
                    return node(ACTIVITY, item, ActivityMapper.toResponse(item), null);
                }

                @Override
                public ItineraryNodeResponse sequence(ItineraryNode block, List<ItineraryNodeResponse> parts) {
                    return node(PlannedNodeCommand.SEQUENTIAL, block, null, parts);
                }

                @Override
                public ItineraryNodeResponse parallel(ItineraryNode block, List<ItineraryNodeResponse> branches) {
                    return node(PlannedNodeCommand.PARALLEL, block, null, branches);
                }
            };

    private ItineraryMapper() {
    }

    public static ScheduleBlockCommand toCommand(String expeditionId, ScheduleBlockRequest request) {
        return new ScheduleBlockCommand(new ExpeditionId(expeditionId), toNodeCommand(request));
    }

    public static ItineraryNodeResponse toResponse(ItineraryNode node) {
        return node.accept(AS_RESPONSE);
    }

    public static ItineraryResponse toResponse(Expedition expedition) {
        return new ItineraryResponse(
                expedition.getId().value(),
                expedition.getItinerary().getNodes().stream().map(ItineraryMapper::toResponse).toList());
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

    private static ItineraryNodeResponse node(String type, ItineraryNode node, ScheduledActivityResponse activity,
                                              List<ItineraryNodeResponse> children) {
        return new ItineraryNodeResponse(type, Mappings.toDto(node.getScheduledPeriod()),
                node.estimatedDuration().toMinutes(), node.estimatedRisk(), activity, children);
    }
}
