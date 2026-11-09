package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ActivityTracking;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Observation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public record ActivityTrackingView(TrackingStatus status, Optional<LocalDateTime> startedAt,
                                   Optional<LocalDateTime> finishedAt, Optional<String> result,
                                   List<Observation> observations) {

    public static ActivityTrackingView of(ItineraryItem item) {
        return item.getTracking().map(ActivityTrackingView::of).orElseGet(ActivityTrackingView::pending);
    }

    private static ActivityTrackingView of(ActivityTracking tracking) {
        return new ActivityTrackingView(
                tracking.isFinished() ? TrackingStatus.FINISHED : TrackingStatus.IN_PROGRESS,
                Optional.of(tracking.getStartedAt()),
                Optional.ofNullable(tracking.getFinishedAt()),
                Optional.ofNullable(tracking.getResult()),
                List.copyOf(tracking.getObservations()));
    }

    private static ActivityTrackingView pending() {
        return new ActivityTrackingView(TrackingStatus.PENDING, Optional.empty(), Optional.empty(), Optional.empty(), List.of());
    }
}
