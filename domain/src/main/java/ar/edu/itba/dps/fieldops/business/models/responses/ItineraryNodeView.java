package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;
import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNodeVisitor;
import ar.edu.itba.dps.fieldops.business.models.commands.PlannedNodeCommand;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public record ItineraryNodeView(String type, TimePeriod scheduledPeriod, Duration estimatedDuration, RiskLevel estimatedRisk,
                                Optional<ScheduledActivityView> activity, List<ItineraryNodeView> children) {
    public static final String ACTIVITY = "activity";

    private static final ItineraryNodeVisitor<ItineraryNodeView> AS_VIEW = new ItineraryNodeVisitor<>() {
        @Override
        public ItineraryNodeView activity(ItineraryItem item) {
            return new ItineraryNodeView(ACTIVITY, item.getScheduledPeriod(), item.estimatedDuration(), item.estimatedRisk(),
                    Optional.of(ScheduledActivityView.of(item)), List.of());
        }

        @Override
        public ItineraryNodeView sequence(ItineraryNode block, List<ItineraryNodeView> parts) {
            return blockView(PlannedNodeCommand.SEQUENTIAL, block, parts);
        }

        @Override
        public ItineraryNodeView parallel(ItineraryNode block, List<ItineraryNodeView> branches) {
            return blockView(PlannedNodeCommand.PARALLEL, block, branches);
        }
    };

    public static ItineraryNodeView of(ItineraryNode node) {
        return node.accept(AS_VIEW);
    }

    private static ItineraryNodeView blockView(String type, ItineraryNode block, List<ItineraryNodeView> children) {
        return new ItineraryNodeView(type, block.getScheduledPeriod(), block.estimatedDuration(), block.estimatedRisk(),
                Optional.empty(), children);
    }
}
