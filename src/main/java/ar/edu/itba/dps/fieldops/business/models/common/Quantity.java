package ar.edu.itba.dps.fieldops.business.models.common;

import java.math.BigDecimal;
import java.util.Objects;

public record Quantity(BigDecimal amount, MeasurementUnit unit) {

    public Quantity {
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(unit, "unit is required");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        amount = amount.stripTrailingZeros();
    }

    public static Quantity of(String amount, MeasurementUnit unit) {
        return new Quantity(new BigDecimal(amount), unit);
    }

    public Quantity plus(Quantity other) {
        requireSameUnitAs(other);
        return new Quantity(amount.add(other.amount), unit);
    }

    public Quantity minus(Quantity other) {
        requireSameUnitAs(other);
        return new Quantity(amount.subtract(other.amount), unit);
    }

    public boolean isEnoughFor(Quantity required) {
        requireSameUnitAs(required);
        return amount.compareTo(required.amount) >= 0;
    }

    private void requireSameUnitAs(Quantity other) {
        if (unit != other.unit) {
            throw new IllegalArgumentException("Cannot combine %s with %s".formatted(unit, other.unit));
        }
    }
}
