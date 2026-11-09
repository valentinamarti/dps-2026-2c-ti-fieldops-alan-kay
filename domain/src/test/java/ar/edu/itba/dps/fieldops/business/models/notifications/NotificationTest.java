package ar.edu.itba.dps.fieldops.business.models.notifications;

import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.events.ExpeditionApproved;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationTest {
    private static final EventId EVENT = new EventId("ev-1");
    private static final ExpeditionId EXPEDITION = new ExpeditionId("e-1");
    private static final ResourceId RESPONSIBLE = new ResourceId("p-1");
    private static final LocalDateTime TEN = LocalDateTime.of(2026, 10, 1, 10, 0);

    @Test
    void takesWhatItSaysFromTheEvent() {
        final var notification = Notification.of(new ExpeditionApproved(EVENT, EXPEDITION, TEN), RESPONSIBLE);

        assertEquals(new Notification(EVENT, EXPEDITION, RESPONSIBLE, "The expedition was approved", TEN), notification);
    }

    @Test
    void needsATextToShow() {
        assertThrows(IllegalArgumentException.class, () -> new Notification(EVENT, EXPEDITION, RESPONSIBLE, " ", TEN));
    }
}
