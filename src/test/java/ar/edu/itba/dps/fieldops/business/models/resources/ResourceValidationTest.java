package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceValidationTest {

    private static final Quantity SOME_FUEL = Quantity.of("20", LITERS);
    private static final ResourceCategory PICKUP = new ResourceCategory("Pickup");
    private static final ResourceCategory SONAR = new ResourceCategory("Sonar");
    private static final ResourceCategory FUEL = new ResourceCategory("Fuel");

    @Test
    void personRejectsABlankOrMissingIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new Person("", "Ana", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Person("p-1", " ", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Person(null, "Ana", Set.of()));
    }

    @Test
    void vehicleRejectsABlankOrMissingIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new Vehicle("", "Hilux", PICKUP, "ABC 123", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Vehicle("v-1", " ", PICKUP, "ABC 123", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Vehicle("v-1", "Hilux", PICKUP, "", Set.of()));
    }

    @Test
    void instrumentRejectsABlankOrMissingIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new Instrument("", "Sonar 3000", SONAR, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Instrument("i-1", " ", SONAR, Set.of()));
    }

    @Test
    void depletableRejectsABlankOrMissingIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new Depletable("", "Diesel", FUEL, SOME_FUEL));
        assertThrows(IllegalArgumentException.class, () -> new Depletable("d-1", " ", FUEL, SOME_FUEL));
    }

    @Test
    void depletableNeedsAnInitialStock() {
        assertThrows(NullPointerException.class, () -> new Depletable("d-1", "Diesel", FUEL, null));
    }

    @Test
    void reusableResourcesRejectANullCertificationSet() {
        assertThrows(NullPointerException.class, () -> new Person("p-1", "Ana", null));
        assertThrows(NullPointerException.class, () -> new Vehicle("v-1", "Hilux", PICKUP, "ABC 123", null));
        assertThrows(NullPointerException.class, () -> new Instrument("i-1", "Sonar 3000", SONAR, null));
    }

    @Test
    void certificationAndCategoryNeedANonBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Certification(" "));
        assertThrows(IllegalArgumentException.class, () -> new Certification(null));
        assertThrows(IllegalArgumentException.class, () -> new ResourceCategory(" "));
        assertThrows(IllegalArgumentException.class, () -> new ResourceCategory(null));
    }
}