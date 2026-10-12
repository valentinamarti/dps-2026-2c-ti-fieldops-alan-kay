package ar.edu.itba.dps.fieldops.business.services;

import ar.edu.itba.dps.fieldops.business.exceptions.InsufficientStockException;
import ar.edu.itba.dps.fieldops.business.exceptions.ResourceUnavailableException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.Reservation;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.instrument;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.supply;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceReservationsTest {

    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final ResourceReservations reservations = new ResourceReservations();
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build(repository);
    private final Person worker = person("p-2");

    @Test
    void reservesEveryAssignedResourceForItsScheduledPeriodOnBehalfOfTheExpedition() {
        final var cooler = instrument("i-1", "Cooler");
        final var item = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
        expedition.assignStaff(item, worker);
        expedition.assignEquipment(item, cooler);

        reservations.reserveFor(expedition);

        assertFalse(worker.isAvailableDuring(hours(9, 11)));
        assertFalse(cooler.isAvailableDuring(hours(9, 11)));
        worker.releaseAll(expedition.getId());
        assertTrue(worker.isAvailableDuring(hours(9, 11)));
    }

    @Test
    void aResourceSharedByTwoItemsAtDifferentTimesIsReservedForBoth() {
        final var morning = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
        final var afternoon = expedition.schedule(sampling("a-2", coast, hours(0, 24)), hours(14, 16));
        expedition.assignStaff(morning, worker);
        expedition.assignStaff(afternoon, worker);

        reservations.reserveFor(expedition);

        assertFalse(worker.isAvailableDuring(hours(9, 11)));
        assertFalse(worker.isAvailableDuring(hours(14, 16)));
    }

    @Test
    void aResourceTakenByAnotherExpeditionReservesNothing() {
        final var freeWorker = person("p-3");
        final var early = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(6, 8));
        final var clashing = expedition.schedule(sampling("a-2", coast, hours(0, 24)), hours(10, 12));
        expedition.assignStaff(early, freeWorker);
        expedition.assignStaff(clashing, worker);
        worker.reserve(new Reservation(new ExpeditionId("e-2"), hours(9, 11)));

        assertThrows(ResourceUnavailableException.class, () -> reservations.reserveFor(expedition));
        assertTrue(freeWorker.isAvailableDuring(hours(6, 8)));
    }

    @Test
    void aResourceSharedByTwoOverlappingItemsOfTheSameExpeditionReservesNothing() {
        final var freeWorker = person("p-3");
        final var early = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(6, 8));
        final var first = expedition.schedule(sampling("a-2", coast, hours(0, 24)), hours(9, 11));
        final var second = expedition.schedule(sampling("a-3", coast, hours(0, 24)), hours(10, 12));
        expedition.assignStaff(early, freeWorker);
        expedition.assignStaff(first, worker);
        expedition.assignStaff(second, worker);

        assertThrows(ResourceUnavailableException.class, () -> reservations.reserveFor(expedition));
        assertTrue(freeWorker.isAvailableDuring(hours(6, 8)));
        assertTrue(worker.isAvailableDuring(hours(9, 12)));
    }

    @Test
    void checkingThatAnExpeditionCanBeReservedBooksNothing() {
        final var item = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
        expedition.assignStaff(item, worker);

        reservations.requireReservable(expedition);

        assertTrue(worker.isAvailableDuring(hours(9, 11)));
    }

    @Test
    void releasingAnExpeditionFreesEveryResourceItUsesAndKeepsWhatOthersBooked() {
        final var cooler = instrument("i-1", "Cooler");
        final var item = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
        expedition.assignStaff(item, worker);
        expedition.assignEquipment(item, cooler);
        reservations.reserveFor(expedition);
        worker.reserve(new Reservation(new ExpeditionId("e-2"), hours(14, 16)));

        reservations.releaseFor(expedition);

        assertTrue(worker.isAvailableDuring(hours(9, 11)));
        assertTrue(cooler.isAvailableDuring(hours(9, 11)));
        assertFalse(worker.isAvailableDuring(hours(14, 16)));
    }

    @Test
    void reservesTheStockTheItineraryNeedsWithoutConsumingIt() {
        final var jars = supply("d-1", "Sample container", Quantity.of("10", UNITS));
        expedition.assignSupply(expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11)), jars);

        reservations.reserveFor(expedition);

        assertTrue(jars.hasStockFor(Quantity.of("7", UNITS)));
        assertFalse(jars.hasStockFor(Quantity.of("8", UNITS)));
        assertEquals(Quantity.of("10", UNITS), jars.getStock());
    }

    @Test
    void aSupplyWithoutEnoughStockReservesNothing() {
        final var jars = supply("d-1", "Sample container", Quantity.of("2", UNITS));
        final var item = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
        expedition.assignStaff(item, worker);
        expedition.assignSupply(item, jars);

        assertThrows(InsufficientStockException.class, () -> reservations.reserveFor(expedition));
        assertTrue(worker.isAvailableDuring(hours(9, 11)));
        assertTrue(jars.hasStockFor(Quantity.of("2", UNITS)));
    }

    @Test
    void aSupplyAskedForInAnotherUnitIsLeftToTheValidation() {
        final var jars = supply("d-1", "Sample container", Quantity.of("10", LITERS));
        expedition.assignSupply(expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11)), jars);

        reservations.reserveFor(expedition);

        assertTrue(jars.hasStockFor(Quantity.of("10", LITERS)));
    }

    @Test
    void releasingAnExpeditionGivesBackTheStockItReserved() {
        final var jars = supply("d-1", "Sample container", Quantity.of("10", UNITS));
        expedition.assignSupply(expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11)), jars);
        reservations.reserveFor(expedition);

        reservations.releaseFor(expedition);

        assertTrue(jars.hasStockFor(Quantity.of("10", UNITS)));
    }
}
