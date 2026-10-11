package ar.edu.itba.dps.fieldops.business.models.resources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceCategoryTest {

    @Test
    void spellingVariantsOfTheSameNameAreTheSameCategory() {
        final var category = new ResourceCategory("boat");

        assertEquals(category, new ResourceCategory("Boat"));
        assertEquals(category, new ResourceCategory("BOAT"));
        assertEquals(category, new ResourceCategory("boat "));
        assertEquals(category, new ResourceCategory("  Boat"));
        assertEquals(category.hashCode(), new ResourceCategory("Boat ").hashCode());
    }

    @Test
    void keepsTheNameStrippedAndInLowerCase() {
        assertEquals("sample container", new ResourceCategory("  Sample Container ").name());
    }

    @Test
    void differentNamesAreDifferentCategories() {
        assertNotEquals(new ResourceCategory("boat"), new ResourceCategory("bote"));
    }

    @Test
    void rejectsABlankOrNullName() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceCategory(" "));
        assertThrows(IllegalArgumentException.class, () -> new ResourceCategory(null));
    }
}
