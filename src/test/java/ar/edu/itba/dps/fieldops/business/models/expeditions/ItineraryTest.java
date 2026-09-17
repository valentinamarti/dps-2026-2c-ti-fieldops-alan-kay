package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.rules.SampleCollectionRules;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItineraryTest {

    private static final Zone COAST = new Zone("z-1", "Coast", Set.of());
    private static final TimePeriod WHOLE_DAY = hours(0, 24);

    private final Itinerary itinerary = new Itinerary();

    private static TimePeriod hours(int fromHour, int toHour) {
        final var day = LocalDateTime.of(2026, 10, 1, 0, 0);
        return new TimePeriod(day.plusHours(fromHour), day.plusHours(toHour));
    }

    private static Activity sampling(String name, TimePeriod window, Activity... dependencies) {
        return new Activity("a-" + name, name, window, COAST, List.of(dependencies), new SampleCollectionRules(3));
    }

    @Test
    void keepsItsItemsOrderedByStartTime() {
        final var afternoon = itinerary.add(sampling("Afternoon", WHOLE_DAY), hours(14, 16));
        final var morning = itinerary.add(sampling("Morning", WHOLE_DAY), hours(8, 10));

        assertEquals(List.of(morning, afternoon), itinerary.getItems());
    }

    @Test
    void rejectsSchedulingTheSameActivityTwice() {
        final var survey = sampling("Survey", WHOLE_DAY);
        itinerary.add(survey, hours(8, 10));

        assertThrows(InvalidScheduleException.class, () -> itinerary.add(survey, hours(14, 16)));
    }

    @Test
    void rejectsAPeriodOutsideTheActivityWindow() {
        final var morningOnly = sampling("Survey", hours(6, 12));

        assertThrows(InvalidScheduleException.class, () -> itinerary.add(morningOnly, hours(11, 13)));
    }

    @Test
    void rejectsAnActivityWhoseDependencyIsNotScheduled() {
        final var landing = sampling("Landing", WHOLE_DAY);

        assertThrows(InvalidScheduleException.class,
                () -> itinerary.add(sampling("Survey", WHOLE_DAY, landing), hours(10, 12)));
    }

    @Test
    void rejectsAnActivityThatStartsBeforeItsDependencyEndsAndLeavesTheItineraryUntouched() {
        final var landing = sampling("Landing", WHOLE_DAY);
        itinerary.add(landing, hours(8, 11));

        assertThrows(InvalidScheduleException.class,
                () -> itinerary.add(sampling("Survey", WHOLE_DAY, landing), hours(10, 12)));
        assertEquals(1, itinerary.getItems().size());
    }

    @Test
    void acceptsAnActivityThatStartsRightWhenItsDependencyEnds() {
        final var landing = sampling("Landing", WHOLE_DAY);
        itinerary.add(landing, hours(8, 10));

        final var survey = itinerary.add(sampling("Survey", WHOLE_DAY, landing), hours(10, 12));

        assertTrue(itinerary.contains(survey));
    }

    @Test
    void itemsOverlapOnlyWhenTheirPeriodsOverlap() {
        final var morning = itinerary.add(sampling("Morning", WHOLE_DAY), hours(8, 12));
        final var noon = itinerary.add(sampling("Noon", WHOLE_DAY), hours(11, 13));
        final var afternoon = itinerary.add(sampling("Afternoon", WHOLE_DAY), hours(13, 15));

        assertTrue(morning.overlapsWith(noon));
        assertFalse(noon.overlapsWith(afternoon));
    }
}
