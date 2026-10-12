package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateResourceException;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceCatalogTest {

    private static final Certification DIVING = new Certification("Diving");
    private static final ResourceCategory BOAT = new ResourceCategory("Boat");
    private static final ResourceCategory SONAR = new ResourceCategory("Sonar");
    private static final ResourceCategory FUEL = new ResourceCategory("Fuel");
    private static final ResourceCategory WATER = new ResourceCategory("Water");
    private static final ExpeditionId ANOTHER_EXPEDITION = new ExpeditionId("e-2");
    private static final TimePeriod MORNING = hours(8, 12);

    private final ResourceCatalog catalog = new ResourceCatalog();

    private static TimePeriod hours(int fromHour, int toHour) {
        final var day = LocalDateTime.of(2026, 10, 1, 0, 0);
        return new TimePeriod(day.plusHours(fromHour), day.plusHours(toHour));
    }

    @Test
    void rejectsASecondResourceWithTheSameId() {
        catalog.addStaff(new Person(new ResourceId("r-1"), "Ana", Set.of()));

        assertThrows(DuplicateResourceException.class, () -> catalog.addStaff(new Person(new ResourceId("r-1"), "Ana", Set.of())));
    }

    @Test
    void idsAreUniqueAcrossResourceKinds() {
        catalog.addStaff(new Person(new ResourceId("r-1"), "Ana", Set.of()));

        assertThrows(DuplicateResourceException.class,
                () -> catalog.addEquipment(new Vehicle(new ResourceId("r-1"), "Albatros", BOAT, "ABC 123", Set.of())));
    }

    @Test
    void offersOnlyStaffThatIsCertifiedAndFreeDuringThePeriod() {
        final var certifiedAndFree = new Person(new ResourceId("p-1"), "Ana", Set.of(DIVING));
        final var certifiedButBusy = new Person(new ResourceId("p-2"), "Bruno", Set.of(DIVING));
        final var freeButNotCertified = new Person(new ResourceId("p-3"), "Carla", Set.of());
        certifiedButBusy.reserve(new Reservation(ANOTHER_EXPEDITION, MORNING));
        catalog.addStaff(certifiedAndFree);
        catalog.addStaff(certifiedButBusy);
        catalog.addStaff(freeButNotCertified);

        final var candidates = catalog.availableStaffFor(new StaffRequirement(1, Set.of(DIVING)), MORNING);

        assertEquals(List.of(certifiedAndFree), candidates);
    }

    @Test
    void offersOnlyEquipmentOfTheRequiredCategoryThatIsFreeDuringThePeriod() {
        final var freeBoat = new Vehicle(new ResourceId("v-1"), "Albatros", BOAT, "ABC 123", Set.of());
        final var busyBoat = new Vehicle(new ResourceId("v-2"), "Petrel", BOAT, "DEF 456", Set.of());
        final var freeSonar = new Instrument(new ResourceId("i-1"), "Sonar 3000", SONAR, Set.of());
        busyBoat.reserve(new Reservation(ANOTHER_EXPEDITION, hours(10, 11)));
        catalog.addEquipment(freeBoat);
        catalog.addEquipment(busyBoat);
        catalog.addEquipment(freeSonar);

        final var candidates = catalog.availableEquipmentFor(new ReusableRequirement(BOAT, 1), MORNING);

        assertEquals(List.of(freeBoat), candidates);
    }

    @Test
    void offersOnlySuppliesOfTheRequiredCategoryWithEnoughStock() {
        final var fullTank = new Depletable(new ResourceId("d-1"), "Diesel", FUEL, Quantity.of("50", LITERS));
        final var almostEmptyTank = new Depletable(new ResourceId("d-2"), "Diesel reserve", FUEL, Quantity.of("5", LITERS));
        final var water = new Depletable(new ResourceId("d-3"), "Drinking water", WATER, Quantity.of("100", LITERS));
        catalog.addSupply(fullTank);
        catalog.addSupply(almostEmptyTank);
        catalog.addSupply(water);

        final var candidates = catalog.suppliesCovering(new DepletableRequirement(FUEL, Quantity.of("20", LITERS)));

        assertEquals(List.of(fullTank), candidates);
    }

    @Test
    void findsEachKindOfResourceByItsId() {
        final var diver = new Person(new ResourceId("p-1"), "Ana", Set.of());
        final var boat = new Vehicle(new ResourceId("v-1"), "Albatros", BOAT, "ABC 123", Set.of());
        final var tank = new Depletable(new ResourceId("d-1"), "Diesel", FUEL, Quantity.of("50", LITERS));
        catalog.addStaff(diver);
        catalog.addEquipment(boat);
        catalog.addSupply(tank);

        assertSame(diver, catalog.findStaff(new ResourceId("p-1")).orElseThrow());
        assertSame(boat, catalog.findEquipment(new ResourceId("v-1")).orElseThrow());
        assertSame(tank, catalog.findSupply(new ResourceId("d-1")).orElseThrow());
    }

    @Test
    void findsNothingWhenTheIdBelongsToAnotherKindOfResource() {
        catalog.addStaff(new Person(new ResourceId("p-1"), "Ana", Set.of()));

        assertTrue(catalog.findEquipment(new ResourceId("p-1")).isEmpty());
        assertTrue(catalog.findSupply(new ResourceId("p-1")).isEmpty());
        assertTrue(catalog.findStaff(new ResourceId("p-404")).isEmpty());
    }

    @Test
    void listsEveryResourceItHolds() {
        final var diver = new Person(new ResourceId("p-1"), "Ana", Set.of());
        final var boat = new Vehicle(new ResourceId("v-1"), "Albatros", BOAT, "ABC 123", Set.of());
        final var tank = new Depletable(new ResourceId("d-1"), "Diesel", FUEL, Quantity.of("50", LITERS));
        catalog.addStaff(diver);
        catalog.addEquipment(boat);
        catalog.addSupply(tank);

        assertEquals(List.of(diver), catalog.allStaff());
        assertEquals(List.of(boat), catalog.allEquipment());
        assertEquals(List.of(tank), catalog.allSupplies());
    }
}
