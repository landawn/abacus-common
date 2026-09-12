package testfixtures.dates;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Subtypes for the {@code Dates} creator-registry tests that must not be built by the class's
 * constructor/clone fallback: {@link CountingCalendar} has neither a {@code (long)} nor a no-arg
 * constructor, so only a registered creator can rebuild it. They live outside
 * {@code com.landawn.abacus.*} because creators cannot be registered for that package.
 */
public final class CountingCreatorTypes {

    private CountingCreatorTypes() {
        // holder
    }

    public static final class CountingDate extends Date {
        private static final long serialVersionUID = 1L;

        public CountingDate(final long millis) {
            super(millis);
        }
    }

    public static final class CountingCalendar extends GregorianCalendar {
        private static final long serialVersionUID = 1L;

        public CountingCalendar(final long millis, final TimeZone zone) {
            super(zone);
            setTimeInMillis(millis);
        }
    }
}
