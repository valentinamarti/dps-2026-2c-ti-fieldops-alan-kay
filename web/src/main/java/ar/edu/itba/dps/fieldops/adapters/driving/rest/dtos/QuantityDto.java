package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit;

/**
 * An amount with its unit, in both directions. The amount travels as text, like {@code Quantity.of} takes it:
 * the domain keeps it as an exact decimal with the trailing zeros stripped, so a stock of ten units reads back
 * as {@code "10"} and never as {@code 1E+1}. A number in the request is accepted too.
 */
public record QuantityDto(String amount, MeasurementUnit unit) {
}
