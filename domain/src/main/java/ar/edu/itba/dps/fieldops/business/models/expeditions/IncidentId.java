package ar.edu.itba.dps.fieldops.business.models.expeditions;

import java.util.Objects;

/**
 * An incident is the n-th one registered by its expedition. The expedition id is unique (D26), so the pair is too.
 */
public record IncidentId(ExpeditionId expedition, int number) {

    public IncidentId {
        Objects.requireNonNull(expedition, "expedition is required");
        if (number < 1) {
            throw new IllegalArgumentException("an incident number starts at 1");
        }
    }

    @Override
    public String toString() {
        return "%s/incidents/%d".formatted(expedition, number);
    }
}
