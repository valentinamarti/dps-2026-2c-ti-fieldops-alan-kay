package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.common.Quantity;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String consumableId, Quantity stock, Quantity requested) {
        super("Consumable %s has %s but %s were requested".formatted(consumableId, stock, requested));
    }

    public InsufficientStockException(String consumableId, Quantity requested) {
        super("Consumable %s does not have %s available".formatted(consumableId, requested));
    }
}
