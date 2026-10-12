package ar.edu.itba.dps.fieldops.business.models.activities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivityIdTest {

    @Test
    void twoIdsWithTheSameValueAreTheSameId() {
        assertEquals(new ActivityId("a-1"), new ActivityId("a-1"));
    }

    @Test
    void rejectsABlankOrMissingValue() {
        assertThrows(IllegalArgumentException.class, () -> new ActivityId(" "));
        assertThrows(IllegalArgumentException.class, () -> new ActivityId(null));
    }

    @Test
    void printsOnlyItsValueSoMessagesStayReadable() {
        assertEquals("a-1", new ActivityId("a-1").toString());
    }
}
