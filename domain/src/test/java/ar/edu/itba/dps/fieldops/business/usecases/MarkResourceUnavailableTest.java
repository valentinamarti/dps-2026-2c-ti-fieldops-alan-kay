package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;
import org.junit.jupiter.api.Test;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.instrument;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.supply;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.vehicle;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static ar.edu.itba.dps.fieldops.business.models.common.Quantity.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkResourceUnavailableTest {
    private final ResourceCatalog catalog = new ResourceCatalog();
    private final MarkResourceUnavailable markUnavailable = new MarkResourceUnavailable(catalog);
    private final Unavailability sickLeave = new Unavailability(hours(8, 12), "Sick leave");

    @Test
    void leavesAPersonUnavailableForThePeriodThatWasMarked() {
        final var ada = person("p-1");
        catalog.addStaff(ada);

        markUnavailable.markStaffUnavailable(new ResourceId("p-1"), sickLeave);

        assertFalse(ada.isAvailableDuring(hours(8, 12)));
        assertTrue(ada.isAvailableDuring(hours(12, 18)));
    }

    @Test
    void leavesAVehicleUnavailableForThePeriodThatWasMarked() {
        final var zodiac = vehicle("v-1", "Boat");
        catalog.addEquipment(zodiac);

        markUnavailable.markVehicleUnavailable(new ResourceId("v-1"), new Unavailability(hours(8, 12), "Maintenance"));

        assertFalse(zodiac.isAvailableDuring(hours(10, 14)));
    }

    @Test
    void leavesAnInstrumentUnavailableForThePeriodThatWasMarked() {
        final var cooler = instrument("i-1", "Cooler");
        catalog.addEquipment(cooler);

        markUnavailable.markInstrumentUnavailable(new ResourceId("i-1"), new Unavailability(hours(8, 12), "Calibration"));

        assertFalse(cooler.isAvailableDuring(hours(10, 14)));
    }

    @Test
    void failsWhenNothingIsRegisteredUnderThatId() {
        assertEquals("There is no person with id p-404", assertThrows(NotFoundException.class,
                () -> markUnavailable.markStaffUnavailable(new ResourceId("p-404"), sickLeave)).getMessage());
    }

    @Test
    void failsWhenTheIdBelongsToAnotherKindOfResource() {
        catalog.addEquipment(vehicle("v-1", "Boat"));

        assertEquals("There is no instrument with id v-1", assertThrows(NotFoundException.class,
                () -> markUnavailable.markInstrumentUnavailable(new ResourceId("v-1"), sickLeave)).getMessage());
    }

    @Test
    void doesNotReachTheSuppliesOfTheCatalog() {
        catalog.addSupply(supply("s-1", "Sample container", of("10", UNITS)));

        assertThrows(NotFoundException.class,
                () -> markUnavailable.markStaffUnavailable(new ResourceId("s-1"), sickLeave));
        assertThrows(NotFoundException.class,
                () -> markUnavailable.markVehicleUnavailable(new ResourceId("s-1"), sickLeave));
    }
}
