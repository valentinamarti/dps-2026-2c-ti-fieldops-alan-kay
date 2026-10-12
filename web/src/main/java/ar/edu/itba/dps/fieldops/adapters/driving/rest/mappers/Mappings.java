package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.PeriodDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.QuantityDto;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.Certifications;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The translations every mapper of this package shares: the value objects that travel as text and the lists
 * that come out sorted. It is here and not duplicated in each mapper because they are the same translation,
 * not a coincidence.
 *
 * <p>None of them validates. A value that is missing travels as {@code null} to the constructor of the value
 * object, which is what rejects it with its own message (D49).
 */
final class Mappings {

    private Mappings() {
    }

    static TimePeriod toPeriod(PeriodDto period) {
        return period == null ? null : new TimePeriod(period.start(), period.end());
    }

    static PeriodDto toDto(TimePeriod period) {
        return new PeriodDto(period.start(), period.end());
    }

    static Quantity toQuantity(QuantityDto quantity) {
        return quantity == null ? null : new Quantity(new BigDecimal(quantity.amount()), quantity.unit());
    }

    static QuantityDto toDto(Quantity quantity) {
        return new QuantityDto(quantity.amount().toPlainString(), quantity.unit());
    }

    static Set<Certification> toCertifications(Set<String> names) {
        return mapped(names, Certification::new);
    }

    static List<String> certificationNames(Certifications certifications) {
        return sorted(certifications.values().stream().map(Certification::name));
    }

    static <T> Set<T> mapped(Set<String> values, Function<String, T> asValueObject) {
        return Objects.requireNonNullElse(values, Set.<String>of()).stream()
                .map(asValueObject)
                .collect(Collectors.toUnmodifiableSet());
    }

    static List<String> sorted(Stream<String> values) {
        return values.sorted().toList();
    }
}
