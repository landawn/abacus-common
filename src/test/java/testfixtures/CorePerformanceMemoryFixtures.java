package testfixtures;

import java.util.ArrayList;
import java.util.GregorianCalendar;

/** Distinct types keep global creator/converter registrations isolated from other tests. */
public final class CorePerformanceMemoryFixtures {
    private CorePerformanceMemoryFixtures() {
    }

    public static final class CalendarType extends GregorianCalendar {
        private static final long serialVersionUID = 1L;

        public CalendarType(final long millis) {
            setTimeInMillis(millis);
        }
    }

    public static final class Supplied {
        public final Object value;

        public Supplied(final Object value) {
            this.value = value;
        }
    }

    public static final class ReusedList extends ArrayList<Object> {
        private static final long serialVersionUID = 1L;
    }
}
