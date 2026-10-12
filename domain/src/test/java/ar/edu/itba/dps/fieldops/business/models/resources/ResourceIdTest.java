package ar.edu.itba.dps.fieldops.business.models.resources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceIdTest {

    @Test
    void twoIdsWithTheSameValueAreTheSameId() {
        assertEquals(new ResourceId("p-1"), new ResourceId("p-1"));
    }

    @Test
    void rejectsABlankOrMissingValue() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceId(" "));
        assertThrows(IllegalArgumentException.class, () -> new ResourceId(null));
    }

    @Test
    void printsOnlyItsValueSoMessagesStayReadable() {
        assertEquals("p-1", new ResourceId("p-1").toString());
    }
}
