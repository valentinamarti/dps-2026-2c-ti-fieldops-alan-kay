package ar.edu.itba.dps.fieldops.business.models.common;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class DomainArguments {

    private DomainArguments() {
    }

    public static String requireText(String value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }

    public static <C extends Collection<?>> C requireNotEmpty(C values, String field) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be empty");
        }
        return values;
    }

    public static <T> Set<T> requireSet(Set<T> values, String field) {
        Objects.requireNonNull(values, field + " is required");
        return Set.copyOf(values);
    }

    public static <T> List<T> requireList(List<T> values, String field) {
        Objects.requireNonNull(values, field + " is required");
        return List.copyOf(values);
    }
}