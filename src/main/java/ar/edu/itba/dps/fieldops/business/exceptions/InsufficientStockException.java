package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(ResourceId consumableId, Quantity stock, Quantity requested) {
        super("Consumable %s has %s but %s were requested".formatted(consumableId, stock, requested));
    }

    public InsufficientStockException(ResourceId consumableId, Quantity requested) {
        super("Consumable %s does not have %s available".formatted(consumableId, requested));
    }
}
