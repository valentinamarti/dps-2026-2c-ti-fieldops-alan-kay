package ar.edu.itba.dps.fieldops.business.models.common;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimePeriodTest {

    private static TimePeriod hours(int fromHour, int toHour) {
        final var day = LocalDateTime.of(2026, 10, 1, 0, 0);
        return new TimePeriod(day.plusHours(fromHour), day.plusHours(toHour));
    }

    @Test
    void rejectsPeriodThatEndsBeforeItStarts() {
        assertThrows(IllegalArgumentException.class, () -> hours(10, 8));
    }

    @Test
    void rejectsPeriodWithNoDuration() {
        assertThrows(IllegalArgumentException.class, () -> hours(10, 10));
    }

    @Test
    void identicalPeriodsOverlap() {
        assertTrue(hours(8, 12).overlapsWith(hours(8, 12)));
    }

    @Test
    void periodContainingAnotherOverlapsWithIt() {
        assertTrue(hours(8, 18).overlapsWith(hours(10, 12)));
        assertTrue(hours(10, 12).overlapsWith(hours(8, 18)));
    }

    @Test
    void partiallySharedPeriodsOverlap() {
        assertTrue(hours(8, 12).overlapsWith(hours(11, 14)));
    }

    @Test
    void disjointPeriodsDoNotOverlap() {
        assertFalse(hours(8, 10).overlapsWith(hours(14, 16)));
    }

    @Test
    void periodsThatOnlyTouchAtTheBorderDoNotOverlap() {
        assertFalse(hours(8, 12).overlapsWith(hours(12, 16)));
    }

    @Test
    void containsAPeriodThatFitsInsideIt() {
        assertTrue(hours(8, 18).contains(hours(10, 12)));
        assertTrue(hours(8, 18).contains(hours(8, 18)));
    }

    @Test
    void doesNotContainAPeriodThatGoesBeyondIt() {
        assertFalse(hours(8, 12).contains(hours(11, 14)));
        assertFalse(hours(10, 12).contains(hours(8, 18)));
    }

    @Test
    void includesItsStartButNotItsEnd() {
        final var period = hours(8, 12);

        assertTrue(period.includes(period.start()));
        assertFalse(period.includes(period.end()));
    }

    @Test
    void durationIsTheTimeBetweenStartAndEnd() {
        assertEquals(Duration.ofHours(4), hours(8, 12).duration());
    }
}
