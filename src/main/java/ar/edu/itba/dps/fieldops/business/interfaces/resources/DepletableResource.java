package ar.edu.itba.dps.fieldops.business.interfaces.resources;

import ar.edu.itba.dps.fieldops.business.models.common.Quantity;

public interface DepletableResource extends Resource {

    boolean hasStockFor(Quantity required);

    void consume(Quantity quantity);
}
