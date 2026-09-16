package ar.edu.itba.dps.fieldops.business.models.activities;

import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceRequirementsTest {

    private static final ResourceCategory BOAT = new ResourceCategory("Boat");
    private static final ResourceCategory SONAR = new ResourceCategory("Sonar");
    private static final ResourceCategory FUEL = new ResourceCategory("Fuel");
    private static final ResourceCategory WATER = new ResourceCategory("Water");

    private static final DepletableRequirement TWENTY_LITERS_OF_FUEL = new DepletableRequirement(FUEL, Quantity.of("20", LITERS));

    @Test
    void reusableRequirementNeedsAtLeastOneUnit() {
        assertThrows(IllegalArgumentException.class, () -> new ReusableRequirement(BOAT, 0));
    }

    @Test
    void depletableRequirementNeedsAPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new DepletableRequirement(FUEL, Quantity.of("0", LITERS)));
    }

    @Test
    void reusableRequirementAcceptsEquipmentOfItsCategory() {
        final var boat = new Vehicle("v-1", "Albatros", BOAT, "ABC 123", Set.of());

        assertTrue(new ReusableRequirement(BOAT, 1).accepts(boat));
    }

    @Test
    void reusableRequirementRejectsEquipmentOfAnotherCategory() {
        final var sonar = new Instrument("i-1", "Sonar 3000", SONAR, Set.of());

        assertFalse(new ReusableRequirement(BOAT, 1).accepts(sonar));
    }

    @Test
    void depletableRequirementIsCoveredByASupplyOfItsCategoryWithEnoughStock() {
        final var diesel = new Depletable("d-1", "Diesel", FUEL, Quantity.of("50", LITERS));

        assertTrue(TWENTY_LITERS_OF_FUEL.isCoveredBy(diesel));
    }

    @Test
    void depletableRequirementIsNotCoveredWhenTheStockFallsShort() {
        final var almostEmpty = new Depletable("d-1", "Diesel", FUEL, Quantity.of("5", LITERS));

        assertFalse(TWENTY_LITERS_OF_FUEL.isCoveredBy(almostEmpty));
    }

    @Test
    void depletableRequirementIsNotCoveredByASupplyOfAnotherCategory() {
        final var water = new Depletable("d-2", "Drinking water", WATER, Quantity.of("100", LITERS));

        assertFalse(TWENTY_LITERS_OF_FUEL.isCoveredBy(water));
    }
}
