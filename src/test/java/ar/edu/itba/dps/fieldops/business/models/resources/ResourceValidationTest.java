package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceValidationTest {

    private static final Quantity SOME_FUEL = Quantity.of("20", LITERS);

    @Test
    void personRejectsABlankOrMissingIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new Person("", "Ana", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Person("p-1", " ", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Person(null, "Ana", Set.of()));
    }

    @Test
    void vehicleRejectsABlankOrMissingIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new Vehicle("", "Pickup", "ABC 123", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Vehicle("v-1", " ", "ABC 123", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Vehicle("v-1", "Pickup", "", Set.of()));
    }

    @Test
    void instrumentRejectsABlankOrMissingIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new Instrument("", "Sonar", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Instrument("i-1", " ", Set.of()));
    }

    @Test
    void depletableRejectsABlankOrMissingIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new Depletable("", "Fuel", SOME_FUEL));
        assertThrows(IllegalArgumentException.class, () -> new Depletable("d-1", " ", SOME_FUEL));
    }

    @Test
    void depletableNeedsAnInitialStock() {
        assertThrows(NullPointerException.class, () -> new Depletable("d-1", "Fuel", null));
    }

    @Test
    void reusableResourcesRejectANullCertificationSet() {
        assertThrows(NullPointerException.class, () -> new Person("p-1", "Ana", null));
        assertThrows(NullPointerException.class, () -> new Vehicle("v-1", "Pickup", "ABC 123", null));
        assertThrows(NullPointerException.class, () -> new Instrument("i-1", "Sonar", null));
    }

    @Test
    void certificationAndCategoryNeedANonBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Certification(" "));
        assertThrows(IllegalArgumentException.class, () -> new Certification(null));
        assertThrows(IllegalArgumentException.class, () -> new ResourceCategory(" "));
        assertThrows(IllegalArgumentException.class, () -> new ResourceCategory(null));
    }
}