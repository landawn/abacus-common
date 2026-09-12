package testfixtures.dates;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/** Test-only date/calendar subtypes used to exercise the public creator registry. */
public final class CreatorTypes {

    private CreatorTypes() {
        // No instances.
    }

    public static class CustomDate extends Date {
        private static final long serialVersionUID = 1L;

        public CustomDate(final long millis) {
            super(millis);
        }
    }

    public static final class WrongMillisDate extends Date {
        private static final long serialVersionUID = 1L;

        public WrongMillisDate(final long millis) {
            super(millis);
        }
    }

    public static final class NoisyTimestamp extends Timestamp {
        private static final long serialVersionUID = 1L;

        public NoisyTimestamp(final long millis) {
            super(millis);
        }
    }

    public static class ParentCalendar extends GregorianCalendar {
        private static final long serialVersionUID = 1L;

        public ParentCalendar(final long millis) {
            setTimeInMillis(millis);
        }
    }

    public static final class ChildCalendar extends ParentCalendar {
        private static final long serialVersionUID = 1L;

        public ChildCalendar(final long millis) {
            super(millis);
        }
    }

    public static final class AliasingCalendar extends GregorianCalendar {
        private static final long serialVersionUID = 1L;

        public AliasingCalendar(final long millis) {
            setTimeInMillis(millis);
        }

        public static AliasingCalendar fromTemplate(final long millis, final Calendar template) {
            template.setTimeInMillis(Long.MIN_VALUE);
            return new AliasingCalendar(millis);
        }
    }
}
