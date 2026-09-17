package ar.edu.itba.dps.fieldops.business.interfaces.resources;

import ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;

public interface DepletableResource extends Resource, Categorized {

    MeasurementUnit unit();

    boolean hasStockFor(Quantity required);

    void consume(Quantity quantity);
}
