package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.exceptions.InsufficientStockException;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Depletable implements DepletableResource {

    @Getter
    private final ResourceId id;
    @Getter
    private final String name;
    @Getter
    private final ResourceCategory category;
    @Getter
    private Quantity stock;
    private final Map<ExpeditionId, Quantity> reserved = new HashMap<>();

    public Depletable(ResourceId id, String name, ResourceCategory category, Quantity initialStock) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = DomainArguments.requireText(name, "name");
        this.category = Objects.requireNonNull(category, "category is required");
        this.stock = Objects.requireNonNull(initialStock, "initial stock is required");
    }

    @Override
    public boolean belongsTo(ResourceCategory category) {
        return this.category.equals(category);
    }

    @Override
    public MeasurementUnit unit() {
        return stock.unit();
    }

    @Override
    public boolean hasStockFor(Quantity required) {
        return hasStockFor(required, Set.of());
    }

    @Override
    public boolean hasStockFor(Quantity required, Set<ExpeditionId> ignoredOwners) {
        return availableStockIgnoring(ignoredOwners).isEnoughFor(required);
    }

    @Override
    public void reserve(ExpeditionId owner, Quantity quantity) {
        Objects.requireNonNull(owner, "owner is required");
        if (!hasStockFor(quantity)) {
            throw new InsufficientStockException(id, availableStock(), quantity);
        }
        reserved.merge(owner, quantity, Quantity::plus);
    }

    @Override
    public boolean canConsume(ExpeditionId owner, Quantity quantity) {
        return usableBy(owner).isEnoughFor(quantity);
    }

    @Override
    public void consume(ExpeditionId owner, Quantity quantity) {
        Objects.requireNonNull(owner, "owner is required");
        if (!canConsume(owner, quantity)) {
            throw new InsufficientStockException(id, usableBy(owner), quantity);
        }
        final var fromReservation = reservedBy(owner).isEnoughFor(quantity) ? quantity : reservedBy(owner);
        reserved.computeIfPresent(owner, (_, reservation) -> reservation.minus(fromReservation));
        stock = stock.minus(quantity);
    }

    @Override
    public void releaseAll(ExpeditionId owner) {
        reserved.remove(Objects.requireNonNull(owner, "owner is required"));
    }

    private Quantity availableStock() {
        return availableStockIgnoring(Set.of());
    }

    private Quantity availableStockIgnoring(Set<ExpeditionId> ignoredOwners) {
        return reserved.entrySet().stream()
                .filter(reservation -> !ignoredOwners.contains(reservation.getKey()))
                .map(Map.Entry::getValue)
                .reduce(stock, Quantity::minus);
    }

    private Quantity usableBy(ExpeditionId owner) {
        return availableStock().plus(reservedBy(owner));
    }

    private Quantity reservedBy(ExpeditionId owner) {
        return reserved.getOrDefault(owner, Quantity.of("0", unit()));
    }
}
