package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
public class DurationPartDocsTest {
    @Test
    void negativeSubsecondBoundariesFollowTruncationRatherThanJdkNormalization() {
        assertEquals(0, Duration.ofMillis(-59999).toMinutesPart());
        assertEquals(-1, java.time.Duration.ofMillis(-59999).toMinutesPart());
        assertEquals(0, Duration.ofMillis(-3599999).toHoursPart());
        assertEquals(-1, java.time.Duration.ofMillis(-3599999).toHoursPart());
        for (final long millis : new long[] { Long.MIN_VALUE, -86400001, -86400000, -3600001, -3600000, -3599999, -60001, -60000, -59999, -1, 0, 1, 59999,
                60000, 3600000, 86400000, Long.MAX_VALUE }) {
            assertEquals((int) ((millis / 3600000) % 24), Duration.ofMillis(millis).toHoursPart());
            assertEquals((int) ((millis / 60000) % 60), Duration.ofMillis(millis).toMinutesPart());
        }
    }
}
