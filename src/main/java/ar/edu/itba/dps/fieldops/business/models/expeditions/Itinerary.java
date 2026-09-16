package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class Itinerary {

    private static final Comparator<ItineraryItem> BY_START = Comparator.comparing(item -> item.getScheduledPeriod().start());

    private final List<ItineraryItem> items = new ArrayList<>();

    public List<ItineraryItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Optional<ItineraryItem> itemFor(Activity activity) {
        return items.stream().filter(item -> item.isFor(activity)).findFirst();
    }

    public boolean contains(ItineraryItem item) {
        return items.contains(item);
    }

    ItineraryItem add(Activity activity, TimePeriod scheduledPeriod) {
        requireNotScheduled(activity);
        final var item = new ItineraryItem(activity, scheduledPeriod);
        activity.getDependencies().forEach(dependency -> requireEndsBefore(dependency, item));
        items.add(item);
        items.sort(BY_START);
        return item;
    }

    private void requireNotScheduled(Activity activity) {
        if (itemFor(activity).isPresent()) {
            throw new InvalidScheduleException("%s is already scheduled".formatted(activity.getName()));
        }
    }

    private void requireEndsBefore(Activity dependency, ItineraryItem item) {
        final var dependencyItem = itemFor(dependency).orElseThrow(() -> new InvalidScheduleException(
                "%s depends on %s, which is not scheduled".formatted(item.getActivity().getName(), dependency.getName())));
        if (!dependencyItem.endsBeforeStartOf(item)) {
            throw new InvalidScheduleException(
                    "%s starts before %s ends".formatted(item.getActivity().getName(), dependency.getName()));
        }
    }
}