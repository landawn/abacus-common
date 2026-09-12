package testfixtures.dates;

import java.util.Calendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import com.landawn.abacus.util.Dates;

/** Fresh-JVM smoke probe for a legal default TimeZone that has no faithful ZoneId. */
public final class DatesCustomDefaultSmoke {

    private DatesCustomDefaultSmoke() {
    }

    public static void main(final String[] args) throws Exception {
        final SimpleTimeZone custom = new SimpleTimeZone(0, "Unregistered/Custom-DST", Calendar.MARCH, 2, Calendar.SUNDAY, 2 * 60 * 60 * 1000,
                Calendar.NOVEMBER, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        TimeZone.setDefault(custom);

        Class.forName(Dates.class.getName(), true, Dates.class.getClassLoader());

        // Legacy functionality remains usable even though the custom default has no faithful ZoneId,
        // and its implicit fallback is the live machine default zone at call time.
        final java.util.Date summer = java.util.Date.from(java.time.Instant.parse("2026-07-01T00:00:00Z"));
        final String expectedCustom = Dates.format(summer, Dates.LOCAL_DATE_TIME_FORMAT, custom);

        if (!expectedCustom.equals(Dates.format(summer, Dates.LOCAL_DATE_TIME_FORMAT))) {
            throw new AssertionError("Implicit legacy formatting did not use the live default zone");
        }

        final TimeZone gmt9 = TimeZone.getTimeZone("GMT+09:00");
        TimeZone.setDefault(gmt9);

        final String expectedGmt9 = Dates.format(summer, Dates.LOCAL_DATE_TIME_FORMAT, gmt9);

        if (!expectedGmt9.equals(Dates.format(summer, Dates.LOCAL_DATE_TIME_FORMAT))) {
            throw new AssertionError("Implicit legacy formatting did not follow the live default-zone change");
        }

        if (expectedGmt9.equals(expectedCustom)) {
            throw new AssertionError("Probe zones are not discriminating");
        }

        // The project logging stack may own non-daemon helper threads; this probe's contract is
        // complete once the assertions above pass.
        System.exit(0);
    }
}
