package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
    public List<ItineraryItem> itemsAfter(ItineraryItem item) {
        return IntStream.range(0, children().size())
                .filter(position -> children().get(position).leaves().contains(item))
                .boxed()
                .findFirst()
                .map(position -> Stream.concat(
                        children().get(position).itemsAfter(item).stream(),
                        children().subList(position + 1, children().size()).stream().flatMap(child -> child.leaves().stream())).toList())
                .orElse(List.of());
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
