package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

import java.time.LocalDateTime;
import java.util.Objects;

public record Observation(String note, LocalDateTime recordedAt) {

    public Observation {
        note = DomainArguments.requireText(note, "note");
        Objects.requireNonNull(recordedAt, "recordedAt is required");
    }
}
