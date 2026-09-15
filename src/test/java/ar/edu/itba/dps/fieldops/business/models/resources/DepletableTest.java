package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.exceptions.InsufficientStockException;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import org.junit.jupiter.api.Test;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.KILOGRAMS;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepletableTest {

    private static Depletable fuelWithLiters(String liters) {
        return new Depletable("c-1", "Fuel", Quantity.of(liters, LITERS));
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

        fuel.consume(Quantity.of("7.5", LITERS));

        assertEquals(Quantity.of("12.5", LITERS), fuel.getStock());
    }

    @Test
    void consumingTheWholeStockLeavesItEmpty() {
        final var fuel = fuelWithLiters("20");

        fuel.consume(Quantity.of("20", LITERS));

        assertEquals(Quantity.of("0", LITERS), fuel.getStock());
    }

    @Test
    void cannotConsumeMoreThanTheStockAndTheStockStaysUntouched() {
        final var fuel = fuelWithLiters("20");

        assertThrows(InsufficientStockException.class, () -> fuel.consume(Quantity.of("25", LITERS)));
        assertEquals(Quantity.of("20", LITERS), fuel.getStock());
    }

    @Test
    void cannotConsumeInADifferentUnit() {
        assertThrows(IllegalArgumentException.class, () -> fuelWithLiters("20").consume(Quantity.of("1", KILOGRAMS)));
    }
}
