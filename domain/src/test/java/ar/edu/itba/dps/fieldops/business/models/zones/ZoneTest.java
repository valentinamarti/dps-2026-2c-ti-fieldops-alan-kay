package ar.edu.itba.dps.fieldops.business.models.zones;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneTest {

    private static final Permit NATIONAL_PARK = new Permit("National park access");
    private static final Permit MARINE_RESERVE = new Permit("Marine reserve access");

    private final Zone protectedCoast = new Zone("z-1", "Protected coast", Set.of(NATIONAL_PARK, MARINE_RESERVE));

    @Test
    void isAccessibleWhenAllRequiredPermitsAreGranted() {
        assertTrue(protectedCoast.isAccessibleWith(List.of(NATIONAL_PARK, MARINE_RESERVE)));
    }

    @Test
    void reportsTheRequiredPermitsThatWereNotGranted() {
        assertEquals(Set.of(MARINE_RESERVE), protectedCoast.missingPermits(List.of(NATIONAL_PARK)));
        assertFalse(protectedCoast.isAccessibleWith(List.of(NATIONAL_PARK)));
    }

    @Test
    void zoneWithoutRequirementsIsAlwaysAccessible() {
        final var openField = new Zone("z-2", "Open field", Set.of());

        assertTrue(openField.isAccessibleWith(List.of()));
    }

    @Test
    void rejectsABlankOrMissingIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new Zone("", "Coast", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Zone("z-1", " ", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Zone(null, "Coast", Set.of()));
    }

    @Test
    void rejectsANullPermitSet() {
        assertThrows(NullPointerException.class, () -> new Zone("z-1", "Coast", null));
    }

    @Test
    void permitNeedsANonBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Permit(" "));
        assertThrows(IllegalArgumentException.class, () -> new Permit(null));
    }
}
