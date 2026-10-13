package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.exceptions.ResourceUnavailableException;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.ReusableResource;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReusableResourceTest {
    private static final ExpeditionId ANOTHER_EXPEDITION = new ExpeditionId("e-2");
    private static final TimePeriod MORNING = hours(8, 12);
    private static final ResourceCategory PICKUP = new ResourceCategory("Pickup");
    private static final ResourceCategory SONAR = new ResourceCategory("Sonar");

    private static TimePeriod hours(int fromHour, int toHour) {
        final var day = LocalDateTime.of(2026, 10, 1, 0, 0);
        return new TimePeriod(day.plusHours(fromHour), day.plusHours(toHour));
    }

    static Stream<ReusableResource> resources() {
        return Stream.of(
                new Person(new ResourceId("p-1"), "Ana", Set.of()),
                new Vehicle(new ResourceId("v-1"), "Hilux", PICKUP, "ABC 123", Set.of()),
                new Instrument(new ResourceId("i-1"), "Sonar 3000", SONAR, Set.of())
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
        resource.reserve(new Reservation(ANOTHER_EXPEDITION, MORNING));

        assertFalse(resource.isAvailableDuring(hours(11, 14)));
    }

    @ParameterizedTest
    @MethodSource("resources")
    void isAvailableRightAfterABookingEnds(ReusableResource resource) {
        resource.reserve(new Reservation(ANOTHER_EXPEDITION, MORNING));

        assertTrue(resource.isAvailableDuring(hours(12, 16)));
    }

    @ParameterizedTest
    @MethodSource("resources")
    void cannotBeReservedTwiceForOverlappingPeriods(ReusableResource resource) {
        resource.reserve(new Reservation(ANOTHER_EXPEDITION, MORNING));

        assertThrows(ResourceUnavailableException.class, () -> resource.reserve(new Reservation(ANOTHER_EXPEDITION, hours(10, 11))));
    }

    @ParameterizedTest
    @MethodSource("resources")
    void isAvailableAgainOnceTheExpeditionThatBookedItReleasesIt(ReusableResource resource) {
        resource.reserve(new Reservation(ANOTHER_EXPEDITION, MORNING));

        resource.releaseAll(ANOTHER_EXPEDITION);

        assertTrue(resource.isAvailableDuring(MORNING));
    }

    @ParameterizedTest
    @MethodSource("resources")
    void isNotAvailableWhileMarkedUnavailable(ReusableResource resource) {
        resource.markUnavailable(new Unavailability(MORNING, "Maintenance"));

        assertFalse(resource.isAvailableDuring(hours(11, 14)));
        assertTrue(resource.isAvailableDuring(hours(12, 16)));
    }
}
