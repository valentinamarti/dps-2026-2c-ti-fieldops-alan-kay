package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UnavailabilityTest {

    private static final TimePeriod MORNING = new TimePeriod(
            LocalDateTime.of(2026, 10, 1, 8, 0), LocalDateTime.of(2026, 10, 1, 12, 0));

    @Test
    void anUnavailabilityNeedsAPeriod() {
        assertThrows(NullPointerException.class, () -> new Unavailability(null, "Maintenance"));
    }

    @Test
    void anUnavailabilityNeedsAReason() {
        assertThrows(IllegalArgumentException.class, () -> new Unavailability(MORNING, " "));
    }
}
