package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class DatesCalendarZoneValidationTest extends TestBase {
    @ParameterizedTest
    @ValueSource(strings = { "day", "month", "year" })
    void comparisonReadsEachCalendarZoneOnce(final String unit) {
        final CountingCalendar left = new CountingCalendar(TimeZone.getTimeZone("UTC"));
        final CountingCalendar right = new CountingCalendar(TimeZone.getTimeZone("GMT"));
        assertTrue(compare(unit, left, right));
        assertEquals(1, left.reads);
        assertEquals(1, right.reads);
    }

    @ParameterizedTest
    @ValueSource(strings = { "day", "month", "year" })
    void sameCalendarIsValidatedOnce(final String unit) {
        final CountingCalendar calendar = new CountingCalendar(TimeZone.getTimeZone("UTC"));
        assertTrue(compare(unit, calendar, calendar));
        assertEquals(1, calendar.reads);
        final Calendar unsupported = new CountingCalendar(new SimpleTimeZone(0, "Review/Unsupported") {
        });
        assertThrows(IllegalArgumentException.class, () -> compare(unit, unsupported, unsupported));
    }

    @ParameterizedTest
    @ValueSource(strings = { "day", "month", "year" })
    void sharedUnsupportedZoneIsNotReportedAsDifferentRules(final String unit) {
        final TimeZone zone = new SimpleTimeZone(0, "Review/Unsupported") {
        };
        final Calendar left = new CountingCalendar(zone);
        final Calendar right = new CountingCalendar(zone);
        final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> compare(unit, left, right));
        assertTrue(failure.getMessage().contains("unverified"), failure.getMessage());
        assertFalse(failure.getMessage().contains("different rules"), failure.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "day", "month", "year" })
    void supportedButDifferentRulesAreStillRejected(final String unit) {
        assertThrows(IllegalArgumentException.class,
                () -> compare(unit, new CountingCalendar(new SimpleTimeZone(0, "same")), new CountingCalendar(new SimpleTimeZone(3_600_000, "same"))));
    }

    private static boolean compare(final String unit, final Calendar left, final Calendar right) {
        return switch (unit) {
            case "day" -> Dates.isSameDay(left, right);
            case "month" -> Dates.isSameMonth(left, right);
            default -> Dates.isSameYear(left, right);
        };
    }

    private static class CountingCalendar extends GregorianCalendar {
        private int reads;

        CountingCalendar(final TimeZone zone) {
            super(zone);
            setTimeInMillis(0);
            reads = 0;
        }

        @Override
        public TimeZone getTimeZone() {
            reads++;
            return super.getTimeZone();
        }
    }
}
