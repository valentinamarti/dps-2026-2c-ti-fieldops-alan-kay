package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.nightSampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.activity;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.parallel;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.sequential;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduledPeriodEstimateTest {
    private static final Zone COAST = zone("z-1");

    private final Expedition expedition = expeditionIn(COAST, person("p-1")).build(new SequentialExpeditionRepository());

    static Stream<Arguments> activitiesEstimatedInTwoHours() {
        return Stream.of(
                Arguments.of("day activity", sampling("a-1", COAST, hours(0, 24))),
                Arguments.of("night activity", nightSampling("a-1", COAST, hours(0, 24))));
    }

    static Stream<Arguments> periodsAndWhetherTheyFit() {
        return activitiesEstimatedInTwoHours().flatMap(activity -> Stream.of(
                Arguments.of(activity.get()[0], activity.get()[1], hours(9, 10), false),
                Arguments.of(activity.get()[0], activity.get()[1], hours(9, 11), true),
                Arguments.of(activity.get()[0], activity.get()[1], hours(9, 12), true)));
    }

    @ParameterizedTest(name = "{0} in {2} fits: {3}")
    @MethodSource("periodsAndWhetherTheyFit")
    void anActivityCannotBeScheduledForLessThanItsEstimate(String kind, Activity activity, TimePeriod period, boolean fits) {
        if (fits) {
            assertDoesNotThrow(() -> expedition.schedule(activity, period));
        } else {
            assertThrows(InvalidScheduleException.class, () -> expedition.schedule(activity, period));
            assertTrue(expedition.getItinerary().getItems().isEmpty());
        }
    }

    @Test
    void aSecondActivityWithTheSamePersonCannotHideAnOverlapBehindAShortPeriod() {
        final var collection = sampling("a-1", COAST, hours(8, 18));

        assertThrows(InvalidScheduleException.class, () -> expedition.schedule(collection, hours(9, 10)));
    }

    @Test
    void aBlockIsRejectedWholeWhenOneOfItsActivitiesIsShorterThanItsEstimate() {
        final var first = sampling("a-1", COAST, hours(8, 18));
        final var second = sampling("a-2", COAST, hours(8, 18));

        assertThrows(InvalidScheduleException.class, () -> expedition.schedule(
                sequential(activity(first, hours(9, 11)), activity(second, hours(11, 12)))));
        assertThrows(InvalidScheduleException.class, () -> expedition.schedule(
                parallel(activity(first, hours(9, 11)), activity(second, hours(9, 10)))));
        assertTrue(expedition.getItinerary().getItems().isEmpty());
    }

    @Test
    void theScheduledPeriodOfABlockNeverLastsLessThanItsEstimate() {
        final var first = sampling("a-1", COAST, hours(8, 18));
        final var second = sampling("a-2", COAST, hours(8, 18));
        final var third = sampling("a-3", COAST, hours(8, 18));

        final var block = expedition.schedule(sequential(
                activity(first, hours(8, 10)),
                parallel(activity(second, hours(10, 12)), activity(third, hours(10, 13)))));

        assertFalse(block.getScheduledPeriod().duration().compareTo(block.estimatedDuration()) < 0);
    }
}
