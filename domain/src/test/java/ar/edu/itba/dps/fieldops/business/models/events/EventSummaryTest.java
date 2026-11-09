package ar.edu.itba.dps.fieldops.business.models.events;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventSummaryTest {
    private static final EventId ID = new EventId("ev-1");
    private static final ExpeditionId EXPEDITION = new ExpeditionId("e-1");
    private static final ActivityId ACTIVITY = new ActivityId("a-08");
    private static final LocalDateTime TEN = LocalDateTime.of(2026, 10, 1, 10, 0);

    @Test
    void anApprovalSaysTheExpeditionWasApproved() {
        assertEquals("The expedition was approved", new ExpeditionApproved(ID, EXPEDITION, TEN).summary());
    }

    @Test
    void aCompletedActivitySaysWhichOneAndItsResult() {
        final var event = new ActivityCompleted(ID, EXPEDITION, ACTIVITY, TEN, "Twelve samples collected");

        assertEquals("Activity a-08 finished: Twelve samples collected", event.summary());
    }

    @Test
    void anIncidentSaysOnWhichActivityAndWhatHappened() {
        final var incident = new Incident(new IncidentId(EXPEDITION, 1), ACTIVITY, "A storm flooded the station", TEN,
                Optional.empty(), Optional.empty());

        assertEquals("Incident on activity a-08: A storm flooded the station",
                new IncidentRegistered(ID, EXPEDITION, incident).summary());
    }
}
