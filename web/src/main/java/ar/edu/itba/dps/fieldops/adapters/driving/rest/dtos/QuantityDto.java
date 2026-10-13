package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit;

public record QuantityDto(String amount, MeasurementUnit unit) {
}
