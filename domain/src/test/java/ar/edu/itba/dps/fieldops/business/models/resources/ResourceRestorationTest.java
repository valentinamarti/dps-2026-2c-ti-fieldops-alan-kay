package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static ar.edu.itba.dps.fieldops.business.models.common.Quantity.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceRestorationTest {
    private static final ExpeditionId OWNER = new ExpeditionId("e-1");
    private static final ResourceId ID = new ResourceId("r-1");
    private static final ResourceCategory BOAT = new ResourceCategory("Boat");
    private static final CalendarSnapshot CALENDAR = new CalendarSnapshot(
            List.of(new Reservation(OWNER, hours(8, 10))),
            List.of(new Unavailability(hours(12, 14), "Medical leave")));

    private void assertKeepsItsCalendar(ScheduledResource resource) {
        assertEquals(CALENDAR, resource.calendarSnapshot());
        assertFalse(resource.isAvailableDuring(hours(9, 10)));
        assertFalse(resource.isAvailableDuring(hours(12, 13)));
        assertTrue(resource.isAvailableDuring(hours(9, 10), Set.of(OWNER)));
    }

    @Test
    void aPersonKeepsItsCalendar() {
        assertKeepsItsCalendar(Person.restored(ID, "Ada", Set.of(), CALENDAR));
    }

    @Test
    void aVehicleKeepsItsCalendar() {
        assertKeepsItsCalendar(Vehicle.restored(ID, "Zodiac", BOAT, "AA 123", Set.of(), CALENDAR));
    }

    @Test
    void anInstrumentKeepsItsCalendar() {
        assertKeepsItsCalendar(Instrument.restored(ID, "Cooler", BOAT, Set.of(), CALENDAR));
    }

    @Test
    void aSupplyKeepsItsStockAndWhatEachExpeditionReserved() {
        final var supply = Depletable.restored(ID, "Jars", BOAT, of("10", UNITS), Map.of(OWNER, of("3", UNITS)));

        assertEquals(of("10", UNITS), supply.getStock());
        assertEquals(Map.of(OWNER, of("3", UNITS)), supply.reservations());
        assertTrue(supply.hasStockFor(of("7", UNITS)));
        assertFalse(supply.hasStockFor(of("8", UNITS)));
    }
}
