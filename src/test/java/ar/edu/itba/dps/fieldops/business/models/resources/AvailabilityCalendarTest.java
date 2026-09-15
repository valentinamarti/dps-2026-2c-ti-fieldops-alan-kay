package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.exceptions.ResourceUnavailableException;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvailabilityCalendarTest {

    private final AvailabilityCalendar calendar = new AvailabilityCalendar();

    private static TimePeriod hours(int fromHour, int toHour) {
        final var day = LocalDateTime.of(2026, 10, 1, 0, 0);
        return new TimePeriod(day.plusHours(fromHour), day.plusHours(toHour));
    }

    @Test
    void anEmptyCalendarIsFreeForAnyPeriod() {
        assertTrue(calendar.isFreeDuring(hours(8, 12)));
    }

    @Test
    void aBookedPeriodIsNoLongerFree() {
        calendar.book(hours(8, 12));

        assertFalse(calendar.isFreeDuring(hours(11, 14)));
    }

    @Test
    void periodsThatOnlyTouchAtTheBorderDoNotCollide() {
        calendar.book(hours(8, 12));

        assertTrue(calendar.isFreeDuring(hours(12, 16)));
    }

    @Test
    void severalNonOverlappingPeriodsCanBeBooked() {
        calendar.book(hours(8, 10));
        calendar.book(hours(14, 16));

        assertTrue(calendar.isFreeDuring(hours(10, 14)));
        assertFalse(calendar.isFreeDuring(hours(9, 15)));
    }

    @Test
    void bookingAnOverlappingPeriodIsRejectedAndLeavesTheCalendarUntouched() {
        calendar.book(hours(8, 12));

        assertThrows(ResourceUnavailableException.class, () -> calendar.book(hours(10, 11)));
        assertTrue(calendar.isFreeDuring(hours(12, 16)));
        assertFalse(calendar.isFreeDuring(hours(8, 12)));
    }
}