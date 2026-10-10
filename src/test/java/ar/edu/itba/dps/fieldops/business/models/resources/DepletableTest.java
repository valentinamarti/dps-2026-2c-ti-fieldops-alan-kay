package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.exceptions.InsufficientStockException;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import org.junit.jupiter.api.Test;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.KILOGRAMS;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepletableTest {

    private static final ResourceCategory FUEL = new ResourceCategory("Fuel");
    private static final ExpeditionId AN_EXPEDITION = new ExpeditionId("e-1");
    private static final ExpeditionId ANOTHER_EXPEDITION = new ExpeditionId("e-2");

    private static Depletable fuelWithLiters(String liters) {
        return new Depletable(new ResourceId("c-1"), "Diesel", FUEL, Quantity.of(liters, LITERS));
    }

    @Test
    void hasStockWhenTheRequirementMatchesExactly() {
        assertTrue(fuelWithLiters("20").hasStockFor(Quantity.of("20", LITERS)));
    }

    @Test
    void hasStockWhenThereIsMoreThanRequired() {
        assertTrue(fuelWithLiters("20").hasStockFor(Quantity.of("5", LITERS)));
    }

    @Test
    void lacksStockWhenThereIsLessThanRequired() {
        assertFalse(fuelWithLiters("20").hasStockFor(Quantity.of("25", LITERS)));
    }

    @Test
    void consumingReducesTheStock() {
        final var fuel = fuelWithLiters("20");

        fuel.consume(AN_EXPEDITION, Quantity.of("7.5", LITERS));

        assertEquals(Quantity.of("12.5", LITERS), fuel.getStock());
    }

    @Test
    void consumingTheWholeStockLeavesItEmpty() {
        final var fuel = fuelWithLiters("20");

        fuel.consume(AN_EXPEDITION, Quantity.of("20", LITERS));

        assertEquals(Quantity.of("0", LITERS), fuel.getStock());
    }

    @Test
    void cannotConsumeMoreThanTheStockAndTheStockStaysUntouched() {
        final var fuel = fuelWithLiters("20");

        assertThrows(InsufficientStockException.class, () -> fuel.consume(AN_EXPEDITION, Quantity.of("25", LITERS)));
        assertEquals(Quantity.of("20", LITERS), fuel.getStock());
    }

    @Test
    void cannotConsumeInADifferentUnit() {
        assertThrows(IllegalArgumentException.class, () -> fuelWithLiters("20").consume(AN_EXPEDITION, Quantity.of("1", KILOGRAMS)));
    }

    @Test
    void reservedStockIsNotAvailableToOthersButIsStillInStock() {
        final var fuel = fuelWithLiters("20");

        fuel.reserve(AN_EXPEDITION, Quantity.of("15", LITERS));

        assertTrue(fuel.hasStockFor(Quantity.of("5", LITERS)));
        assertFalse(fuel.hasStockFor(Quantity.of("6", LITERS)));
        assertEquals(Quantity.of("20", LITERS), fuel.getStock());
    }

    @Test
    void cannotReserveMoreThanIsAvailable() {
        final var fuel = fuelWithLiters("20");
        fuel.reserve(ANOTHER_EXPEDITION, Quantity.of("15", LITERS));

        assertThrows(InsufficientStockException.class, () -> fuel.reserve(AN_EXPEDITION, Quantity.of("6", LITERS)));
        assertTrue(fuel.hasStockFor(Quantity.of("5", LITERS)));
    }

    @Test
    void theReservationsOfAnExpeditionAddUp() {
        final var fuel = fuelWithLiters("20");

        fuel.reserve(AN_EXPEDITION, Quantity.of("10", LITERS));
        fuel.reserve(AN_EXPEDITION, Quantity.of("10", LITERS));

        assertFalse(fuel.hasStockFor(Quantity.of("1", LITERS)));
    }

    @Test
    void consumingUsesUpTheReservationOfTheExpedition() {
        final var fuel = fuelWithLiters("20");
        fuel.reserve(AN_EXPEDITION, Quantity.of("10", LITERS));

        fuel.consume(AN_EXPEDITION, Quantity.of("7", LITERS));

        assertEquals(Quantity.of("13", LITERS), fuel.getStock());
        assertTrue(fuel.hasStockFor(Quantity.of("10", LITERS)));
        assertFalse(fuel.hasStockFor(Quantity.of("11", LITERS)));
    }

    @Test
    void consumingMoreThanWasReservedTakesTheRestFromTheFreeStock() {
        final var fuel = fuelWithLiters("20");
        fuel.reserve(AN_EXPEDITION, Quantity.of("5", LITERS));

        fuel.consume(AN_EXPEDITION, Quantity.of("8", LITERS));

        assertEquals(Quantity.of("12", LITERS), fuel.getStock());
        assertTrue(fuel.hasStockFor(Quantity.of("12", LITERS)));
    }

    @Test
    void cannotConsumeWhatAnotherExpeditionReservedAndTheStockStaysUntouched() {
        final var fuel = fuelWithLiters("20");
        fuel.reserve(ANOTHER_EXPEDITION, Quantity.of("15", LITERS));

        assertThrows(InsufficientStockException.class, () -> fuel.consume(AN_EXPEDITION, Quantity.of("10", LITERS)));
        assertEquals(Quantity.of("20", LITERS), fuel.getStock());
    }

    @Test
    void releasingAnExpeditionGivesBackWhatItReservedAndDidNotUse() {
        final var fuel = fuelWithLiters("20");
        fuel.reserve(AN_EXPEDITION, Quantity.of("10", LITERS));
        fuel.reserve(ANOTHER_EXPEDITION, Quantity.of("5", LITERS));
        fuel.consume(AN_EXPEDITION, Quantity.of("7", LITERS));

        fuel.releaseAll(AN_EXPEDITION);

        assertTrue(fuel.hasStockFor(Quantity.of("8", LITERS)));
        assertFalse(fuel.hasStockFor(Quantity.of("9", LITERS)));
    }
}
