package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class Itinerary {

    private static final Comparator<ItineraryNode> BY_START = Comparator.comparing(node -> node.getScheduledPeriod().start());

    private final List<ItineraryNode> nodes = new ArrayList<>();

    public List<ItineraryNode> getNodes() {
        return nodes.stream().sorted(BY_START).toList();
    }

    public List<ItineraryItem> getItems() {
        return nodes.stream().flatMap(node -> node.leaves().stream()).sorted(BY_START).toList();
    }

    public Optional<ItineraryItem> itemFor(Activity activity) {
        return itemFor(activity, getItems());
    }

    public Optional<ItineraryItem> itemFor(ActivityId activityId) {
        return getItems().stream().filter(item -> item.getActivity().getId().equals(activityId)).findFirst();
    }

    public boolean contains(ItineraryItem item) {
        return getItems().contains(item);
    }

    public List<ItineraryItem> pendingItems() {
        return getItems().stream().filter(item -> !item.isFinished()).toList();
    }

    public List<SupplyDemand> supplyDemands() {
        return SupplyDemand.combine(pendingItems().stream().flatMap(item -> item.supplyDemands().stream()));
    }

    public boolean runInParallel(ItineraryItem first, ItineraryItem second) {
        return nodes.stream().anyMatch(node -> node.runsInParallel(first, second));
    }

    /**
     * The plans that rebuild this itinerary with the same blocks, in the order its nodes were scheduled.
     */
    public List<PlannedNode> planKeepingStructure(Function<ItineraryItem, Optional<PlannedNode>> itemPlans) {
        return nodes.stream().flatMap(node -> node.planKeepingStructure(itemPlans).stream()).toList();
    }

    ItineraryItem add(Activity activity, TimePeriod scheduledPeriod) {
        return add(new ItineraryItem(activity, scheduledPeriod));
    }

    <N extends ItineraryNode> N add(N node) {
        final var incoming = node.leaves();
        final var scheduled = Stream.concat(getItems().stream(), incoming.stream()).toList();
        incoming.forEach(item -> requireScheduledOnce(item, scheduled));
        incoming.stream()
                .filter(item -> !item.isFinished())
                .forEach(item -> requireDependenciesMet(item, scheduled, node));
        nodes.add(node);
        return node;
    }

    private Optional<ItineraryItem> itemFor(Activity activity, List<ItineraryItem> items) {
        return items.stream().filter(item -> item.isFor(activity)).findFirst();
    }

    private void requireScheduledOnce(ItineraryItem item, List<ItineraryItem> scheduled) {
        if (scheduled.stream().anyMatch(other -> !other.equals(item) && other.isFor(item.getActivity()))) {
            throw new InvalidScheduleException("%s is already scheduled".formatted(item.getActivity().getName()));
        }
    }

    private void requireDependenciesMet(ItineraryItem item, List<ItineraryItem> scheduled, ItineraryNode node) {
        item.getActivity().getDependencies().forEach(dependency -> {
            final var dependencyItem = itemFor(dependency, scheduled).orElseThrow(() -> new InvalidScheduleException(
                    "%s depends on %s, which is not scheduled".formatted(item.getActivity().getName(), dependency.getName())));
            requireEndsBefore(dependencyItem, item);
            requireNotInParallel(dependencyItem, item, node);
        });
    }

    private void requireEndsBefore(ItineraryItem dependencyItem, ItineraryItem item) {
        if (!dependencyItem.endsBeforeStartOf(item)) {
            throw new InvalidScheduleException("%s starts before %s ends"
                    .formatted(item.getActivity().getName(), dependencyItem.getActivity().getName()));
        }
    }

    private void requireNotInParallel(ItineraryItem dependencyItem, ItineraryItem item, ItineraryNode node) {
        if (node.runsInParallel(dependencyItem, item)) {
            throw new InvalidScheduleException("%s depends on %s, which runs in parallel with it in the same block"
                    .formatted(item.getActivity().getName(), dependencyItem.getActivity().getName()));
        }
    }
}
