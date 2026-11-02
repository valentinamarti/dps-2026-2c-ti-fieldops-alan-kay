package ar.edu.itba.dps.fieldops.business.models.expeditions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TrackingSnapshot(LocalDateTime startedAt, Optional<LocalDateTime> finishedAt, Optional<String> result,
                               List<Observation> observations) {

    public TrackingSnapshot {
        Objects.requireNonNull(startedAt, "startedAt is required");
        Objects.requireNonNull(finishedAt, "finishedAt is required");
        Objects.requireNonNull(result, "result is required");
        observations = List.copyOf(observations);
    }
}
