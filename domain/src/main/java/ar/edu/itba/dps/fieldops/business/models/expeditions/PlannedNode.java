package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;

import java.util.List;
import java.util.function.Function;

public final class PlannedNode {
    private final Function<Expedition, ItineraryNode> scheduling;

    private PlannedNode(Function<Expedition, ItineraryNode> scheduling) {
        this.scheduling = scheduling;
    }

    public static PlannedNode activity(Activity activity, TimePeriod scheduledPeriod) {
        return new PlannedNode(expedition -> expedition.plannedItem(activity, scheduledPeriod));
    }

    public static PlannedNode finished(ItineraryItem finished) {
        return new PlannedNode(_ -> ItineraryItem.finishedCopyOf(finished));
    }

    public static PlannedNode sequential(PlannedNode... children) {
        return sequential(List.of(children));
    }

    public static PlannedNode sequential(List<PlannedNode> children) {
        return block(children, SequentialBlock::new);
    }

    public static PlannedNode parallel(PlannedNode... branches) {
        return parallel(List.of(branches));
    }

    public static PlannedNode parallel(List<PlannedNode> branches) {
        return block(branches, ParallelBlock::new);
    }

    private static PlannedNode block(List<PlannedNode> children, Function<List<ItineraryNode>, ItineraryNode> assembler) {
        final var plans = DomainArguments.requireList(children, "children");
        return new PlannedNode(expedition -> assembler.apply(plans.stream().map(plan -> plan.nodeIn(expedition)).toList()));
    }

    ItineraryNode nodeIn(Expedition expedition) {
        return scheduling.apply(expedition);
    }
}
