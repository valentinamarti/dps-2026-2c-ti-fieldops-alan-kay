package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.exceptions.InsufficientStockException;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import lombok.Getter;

import java.util.Objects;

public class Depletable implements DepletableResource {

    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private Quantity stock;

    public Depletable(String id, String name, Quantity initialStock) {
        this.id = DomainArguments.requireText(id, "id");
        this.name = DomainArguments.requireText(name, "name");
        this.stock = Objects.requireNonNull(initialStock, "initial stock is required");
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
