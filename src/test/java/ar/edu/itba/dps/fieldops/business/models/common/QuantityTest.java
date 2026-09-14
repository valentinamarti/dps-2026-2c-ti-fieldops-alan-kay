package ar.edu.itba.dps.fieldops.business.models.common;

import org.junit.jupiter.api.Test;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.KILOGRAMS;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantityTest {

    @Test
    void rejectsNegativeAmounts() {
        assertThrows(IllegalArgumentException.class, () -> Quantity.of("-1", LITERS));
    }

    @Test
    void quantitiesWithTheSameValueAreEqualRegardlessOfScale() {
        assertEquals(Quantity.of("2", LITERS), Quantity.of("2.00", LITERS));
    }

    @Test
    void addsQuantitiesOfTheSameUnit() {
        assertEquals(Quantity.of("3.5", LITERS), Quantity.of("2", LITERS).plus(Quantity.of("1.5", LITERS)));
    }

    @Test
    void subtractsQuantitiesOfTheSameUnit() {
        assertEquals(Quantity.of("0.5", LITERS), Quantity.of("2", LITERS).minus(Quantity.of("1.5", LITERS)));
    }

    @Test
    void cannotSubtractMoreThanAvailable() {
        assertThrows(IllegalArgumentException.class, () -> Quantity.of("1", LITERS).minus(Quantity.of("2", LITERS)));
    }

    @Test
    void cannotCombineDifferentUnits() {
        assertThrows(IllegalArgumentException.class, () -> Quantity.of("1", LITERS).plus(Quantity.of("1", KILOGRAMS)));
        assertThrows(IllegalArgumentException.class, () -> Quantity.of("1", LITERS).isEnoughFor(Quantity.of("1", KILOGRAMS)));
    }

    @Test
    void isEnoughForAnEqualOrSmallerRequirement() {
        assertTrue(Quantity.of("10", LITERS).isEnoughFor(Quantity.of("10", LITERS)));
        assertTrue(Quantity.of("10", LITERS).isEnoughFor(Quantity.of("4", LITERS)));
    }

    @Test
    void isNotEnoughForABiggerRequirement() {
        assertFalse(Quantity.of("10", LITERS).isEnoughFor(Quantity.of("10.5", LITERS)));
    }
}
