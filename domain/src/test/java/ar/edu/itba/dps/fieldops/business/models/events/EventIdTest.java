package ar.edu.itba.dps.fieldops.business.models.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventIdTest {

    @Test
    void twoIdsWithTheSameValueAreTheSameId() {
        assertEquals(new EventId("ev-1"), new EventId("ev-1"));
    }

    @Test
    void rejectsABlankOrMissingValue() {
        assertThrows(IllegalArgumentException.class, () -> new EventId(" "));
        assertThrows(IllegalArgumentException.class, () -> new EventId(null));
    }

    @Test
    void everyGeneratedIdIsANewOne() {
        assertNotEquals(EventId.generate(), EventId.generate());
    }

    @Test
    void printsOnlyItsValueSoMessagesStayReadable() {
        assertEquals("ev-1", new EventId("ev-1").toString());
    }
}
