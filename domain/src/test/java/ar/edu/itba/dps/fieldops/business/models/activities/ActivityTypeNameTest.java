package ar.edu.itba.dps.fieldops.business.models.activities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivityTypeNameTest {

    @Test
    void spellingVariantsOfTheSameNameAreTheSameType() {
        final var name = new ActivityTypeName("drone survey");

        assertEquals(name, new ActivityTypeName("Drone survey"));
        assertEquals(name, new ActivityTypeName("DRONE SURVEY"));
        assertEquals(name, new ActivityTypeName("  Drone Survey "));
        assertEquals(name.hashCode(), new ActivityTypeName("Drone Survey").hashCode());
    }

    @Test
    void keepsTheNameStrippedAndInLowerCase() {
        assertEquals("drone survey", new ActivityTypeName("  Drone Survey ").value());
        assertEquals("drone survey", new ActivityTypeName("  Drone Survey ").toString());
    }

    @Test
    void differentNamesAreDifferentTypes() {
        assertNotEquals(new ActivityTypeName("diving"), new ActivityTypeName("dive"));
    }

    @Test
    void rejectsABlankOrNullName() {
        assertThrows(IllegalArgumentException.class, () -> new ActivityTypeName(" "));
        assertThrows(IllegalArgumentException.class, () -> new ActivityTypeName(null));
    }
}
