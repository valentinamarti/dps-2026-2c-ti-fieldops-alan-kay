package ar.edu.itba.dps.fieldops.business.models.expeditions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpeditionIdTest {

    @Test
    void twoIdsWithTheSameValueAreTheSameId() {
        assertEquals(new ExpeditionId("e-1"), new ExpeditionId("e-1"));
    }

    @Test
    void rejectsABlankOrMissingValue() {
        assertThrows(IllegalArgumentException.class, () -> new ExpeditionId(" "));
        assertThrows(IllegalArgumentException.class, () -> new ExpeditionId(null));
    }

    @Test
    void printsOnlyItsValueSoMessagesStayReadable() {
        assertEquals("e-1", new ExpeditionId("e-1").toString());
    }
}
