package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;

import com.landawn.abacus.TestBase;

/** Shared date-resolution fixtures with restoration of the default time zone. */
public abstract class DatesResolutionTestSupport extends TestBase {

    protected static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    protected static final TimeZone NY = TimeZone.getTimeZone("America/New_York");

    protected static final TimeZone KOLKATA = TimeZone.getTimeZone("Asia/Kolkata");

    protected static final Locale THAI = Locale.forLanguageTag("th-TH");

    protected static final Locale JAPANESE_IMPERIAL = Locale.forLanguageTag("ja-JP-u-ca-japanese");

    protected TimeZone defaultZone;

    @BeforeEach
    public void saveDefaultZone() {
        defaultZone = TimeZone.getDefault();
    }

    @AfterEach
    public void restoreDefaultZone() {
        TimeZone.setDefault(defaultZone);
    }

    protected static String iso(final java.util.Date date) {
        return Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, UTC);
    }

    protected static String local(final java.util.Date date, final TimeZone zone) {
        return Dates.format(date, Dates.LOCAL_DATE_TIME_FORMAT, zone);
    }

    protected static java.util.Date at(final String localDateTime, final TimeZone zone) {
        return Dates.parseToJUDate(localDateTime, Dates.LOCAL_DATE_TIME_FORMAT, zone);
    }

    // ----------------------------------------------------------------------------------------------------
    // C-003
    // ----------------------------------------------------------------------------------------------------

    protected static final long OVERLAP_STANDARD = 1699165800000L;

    // 2023-11-05T01:30-05:00[America/New_York]
    protected static final long OVERLAP_DAYLIGHT = 1699162200000L;

    protected static void assertAmbiguous(final org.junit.jupiter.api.function.Executable call) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
        assertTrue(e.getMessage().contains("Ambiguous"), e.getMessage());
    }

    // ----------------------------------------------------------------------------------------------------
    // C-118: only a plain fixed SimpleTimeZone is verifiable; other known-ID defaults use registered rules.
    // ----------------------------------------------------------------------------------------------------

    @SuppressWarnings("serial")
    protected static TimeZone fixedOffsetSubclass(final int rawOffsetMillis, final String id) {
        final TimeZone zone = new TimeZone() {
            @Override
            public int getOffset(final int era, final int year, final int month, final int day, final int dayOfWeek, final int milliseconds) {
                return rawOffsetMillis;
            }

            @Override
            public int getOffset(final long date) {
                return rawOffsetMillis;
            }

            @Override
            public void setRawOffset(final int offsetMillis) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getRawOffset() {
                return rawOffsetMillis;
            }

            @Override
            public boolean useDaylightTime() {
                return false;
            }

            @Override
            public boolean inDaylightTime(final java.util.Date date) {
                return false;
            }
        };
        zone.setID(id);
        return zone;
    }

    protected static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    protected static int count(final String text, final String token) {
        int count = 0;

        for (int i = text.indexOf(token); i >= 0; i = text.indexOf(token, i + token.length())) {
            count++;
        }

        return count;
    }

    protected static String messageOf(final Executable call) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
        assertNotNull(e.getMessage());
        return e.getMessage();
    }

    // =============================================================================================
    // Cycle 3
    // =============================================================================================

    protected static String overflowOf(final Executable call) {
        final ArithmeticException e = assertThrows(ArithmeticException.class, call);
        assertNotNull(e.getMessage());
        return e.getMessage();
    }
}
