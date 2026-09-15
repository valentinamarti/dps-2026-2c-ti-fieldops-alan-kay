package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.exceptions.ResourceUnavailableException;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.ReusableResource;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Availability rules shared by every {@link ReusableResource} implementation.
 */
class ReusableResourceTest {

    private static final TimePeriod MORNING = hours(8, 12);

    private static TimePeriod hours(int fromHour, int toHour) {
        final var day = LocalDateTime.of(2026, 10, 1, 0, 0);
        return new TimePeriod(day.plusHours(fromHour), day.plusHours(toHour));
    }

    static Stream<ReusableResource> resources() {
        return Stream.of(
                new Person("p-1", "Ana", Set.of()),
                new Vehicle("v-1", "Pickup", "ABC 123", Set.of()),
                new Instrument("i-1", "Sonar", Set.of())
        );
    }

    @ParameterizedTest
    @MethodSource("resources")
    void isAvailableWhenNothingIsBooked(ReusableResource resource) {
        assertTrue(resource.isAvailableDuring(MORNING));
    }

    @ParameterizedTest
    @MethodSource("resources")
    void isNotAvailableDuringAPeriodThatOverlapsABooking(ReusableResource resource) {
        resource.reserve(MORNING);

        assertFalse(resource.isAvailableDuring(hours(11, 14)));
    }

    @ParameterizedTest
    @MethodSource("resources")
    void isAvailableRightAfterABookingEnds(ReusableResource resource) {
        resource.reserve(MORNING);

        assertTrue(resource.isAvailableDuring(hours(12, 16)));
    }

    @ParameterizedTest
    @MethodSource("resources")
    void cannotBeReservedTwiceForOverlappingPeriods(ReusableResource resource) {
        resource.reserve(MORNING);

        assertThrows(ResourceUnavailableException.class, () -> resource.reserve(hours(10, 11)));
    }
}
