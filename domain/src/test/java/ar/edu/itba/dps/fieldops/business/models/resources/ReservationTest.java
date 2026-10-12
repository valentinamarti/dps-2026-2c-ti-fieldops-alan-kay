package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationTest {

    private static final TimePeriod MORNING = new TimePeriod(
            LocalDateTime.of(2026, 10, 1, 8, 0), LocalDateTime.of(2026, 10, 1, 12, 0));

    @Test
    void aReservationAlwaysKnowsWhichExpeditionMadeIt() {
        assertThrows(NullPointerException.class, () -> new Reservation(null, MORNING));
    }

    @Test
    void aReservationNeedsAPeriod() {
        assertThrows(NullPointerException.class, () -> new Reservation(new ExpeditionId("e-1"), null));
    }
}
