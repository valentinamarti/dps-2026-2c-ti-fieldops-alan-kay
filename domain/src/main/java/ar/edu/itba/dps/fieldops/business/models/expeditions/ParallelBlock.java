package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;
import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNodeVisitor;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

public final class ParallelBlock extends ItineraryBlock {

    ParallelBlock(List<ItineraryNode> branches) {
        super(branches);
    }

    @Override
    public Duration estimatedDuration() {
        return children().stream().map(ItineraryNode::estimatedDuration).max(Comparator.naturalOrder()).orElseThrow();
    }

    @Override
    public boolean runsInParallel(ItineraryItem first, ItineraryItem second) {
        return areInDifferentBranches(first, second) || super.runsInParallel(first, second);
    }

    @Override
    public <T> T accept(ItineraryNodeVisitor<T> visitor) {
        return visitor.parallel(this, acceptedChildren(visitor));
    }

    @Override
    PlannedNode planOf(List<PlannedNode> branches) {
        return PlannedNode.parallel(branches);
    }

    private boolean areInDifferentBranches(ItineraryItem first, ItineraryItem second) {
        return children().stream().anyMatch(branch -> branch.leaves().contains(first) && !branch.leaves().contains(second))
                && children().stream().anyMatch(branch -> branch.leaves().contains(second));
    }
}
