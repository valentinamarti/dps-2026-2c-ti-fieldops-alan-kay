package ar.edu.itba.dps.fieldops.business.interfaces.resources;

import ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

public interface DepletableResource extends Resource, Categorized {

    MeasurementUnit unit();

    boolean hasStockFor(Quantity required);

    void reserve(ExpeditionId owner, Quantity quantity);

    boolean canConsume(ExpeditionId owner, Quantity quantity);

    void consume(ExpeditionId owner, Quantity quantity);

    void releaseAll(ExpeditionId owner);
}
