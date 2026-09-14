package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.common.Quantity;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String consumableId, Quantity stock, Quantity requested) {
        super("Consumable %s has %s %s but %s %s were requested".formatted(
                consumableId, stock.amount(), stock.unit(), requested.amount(), requested.unit()));
    }
}
