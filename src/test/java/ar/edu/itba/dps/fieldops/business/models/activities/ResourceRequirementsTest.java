package ar.edu.itba.dps.fieldops.business.models.activities;

import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import org.junit.jupiter.api.Test;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceRequirementsTest {

    private static final ResourceCategory BOAT = new ResourceCategory("Boat");
    private static final ResourceCategory FUEL = new ResourceCategory("Fuel");

    @Test
    void reusableRequirementNeedsAtLeastOneUnit() {
        assertThrows(IllegalArgumentException.class, () -> new ReusableRequirement(BOAT, 0));
    }

    @Test
    void depletableRequirementNeedsAPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new DepletableRequirement(FUEL, Quantity.of("0", LITERS)));
    }
}
