package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.exceptions.InsufficientStockException;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;

import java.util.Objects;

public class Depletable implements DepletableResource {

    private final String id;
    private final String name;
    private Quantity stock;

    public Depletable(String id, String name, Quantity initialStock) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = Objects.requireNonNull(name, "name is required");
        this.stock = Objects.requireNonNull(initialStock, "initial stock is required");
    }

    @Override
    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Quantity stock() {
        return stock;
    }

    @Override
    public boolean hasStockFor(Quantity required) {
        return stock.isEnoughFor(required);
    }

    @Override
    public void consume(Quantity quantity) {
        if (!hasStockFor(quantity)) {
            throw new InsufficientStockException(id, stock, quantity);
        }
        stock = stock.minus(quantity);
    }
}
