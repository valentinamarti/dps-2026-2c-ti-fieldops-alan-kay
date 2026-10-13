package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

final class OptionSummaries {
    private OptionSummaries() {
    }

    static String names(List<ItineraryItem> items) {
        return items.stream().map(item -> item.getActivity().getName()).collect(Collectors.joining(", "));
    }

    static String withOthers(String summary, String link, List<ItineraryItem> others) {
        return others.isEmpty() ? summary : "%s %s %s".formatted(summary, link, names(others));
    }

    static String duration(Duration duration) {
        final var minutes = duration.toMinutes();
        return minutes % 60 == 0 ? "%d h".formatted(minutes / 60) : "%d min".formatted(minutes);
    }
}
