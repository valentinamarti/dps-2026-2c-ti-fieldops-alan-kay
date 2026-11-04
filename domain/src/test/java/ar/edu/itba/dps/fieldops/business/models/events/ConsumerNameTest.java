package ar.edu.itba.dps.fieldops.business.models.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsumerNameTest {

    @Test
    void twoNamesWithTheSameValueAreTheSameName() {
        assertEquals(new ConsumerName("notifications"), new ConsumerName("notifications"));
    }

    @Test
    void rejectsABlankOrMissingValue() {
        assertThrows(IllegalArgumentException.class, () -> new ConsumerName(" "));
        assertThrows(IllegalArgumentException.class, () -> new ConsumerName(null));
    }

    @Test
    void printsOnlyItsValueSoMessagesStayReadable() {
        assertEquals("notifications", new ConsumerName("notifications").toString());
    }
}
