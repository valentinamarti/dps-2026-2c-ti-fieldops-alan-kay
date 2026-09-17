package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidTrackingException;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ActivityTracking {

    @Getter
    private final LocalDateTime startedAt;
    @Getter
    private LocalDateTime finishedAt;
    @Getter
    private String result;
    private final List<Observation> observations = new ArrayList<>();
    private final List<Incident> incidents = new ArrayList<>();

    ActivityTracking(LocalDateTime startedAt) {
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt is required");
    }

    public boolean isFinished() {
        return finishedAt != null;
    }

    public List<Observation> getObservations() {
        return Collections.unmodifiableList(observations);
    }

    public List<Incident> getIncidents() {
        return Collections.unmodifiableList(incidents);
    }

    void finish(LocalDateTime at, String result) {
        Objects.requireNonNull(at, "finishedAt is required");
        if (isFinished()) {
            throw new InvalidTrackingException("the activity was already finished");
        }
        if (at.isBefore(startedAt)) {
            throw new InvalidTrackingException("an activity cannot finish before it starts");
        }
        this.result = DomainArguments.requireText(result, "result");
        this.finishedAt = at;
    }

    void record(Observation observation) {
        observations.add(Objects.requireNonNull(observation, "observation is required"));
    }

    void record(Incident incident) {
        incidents.add(Objects.requireNonNull(incident, "incident is required"));
    }
}
