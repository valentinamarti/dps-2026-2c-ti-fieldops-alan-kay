package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

public final class SequentialBlock extends ItineraryBlock {

    SequentialBlock(List<ItineraryNode> children) {
        super(children);
        if (IntStream.range(1, children().size()).anyMatch(this::startsBeforeThePreviousOneEnds)) {
            throw new InvalidScheduleException("each part of a sequential block must start once the previous one has ended");
        }
    }

    @Override
    public Duration estimatedDuration() {
        return children().stream().map(ItineraryNode::estimatedDuration).reduce(Duration.ZERO, Duration::plus);
    }

    @Override
    PlannedNode planOf(List<PlannedNode> children) {
        return PlannedNode.sequential(children);
    }

    private boolean startsBeforeThePreviousOneEnds(int position) {
        final var previous = children().get(position - 1).getScheduledPeriod();
        return children().get(position).getScheduledPeriod().start().isBefore(previous.end());
    }
}
