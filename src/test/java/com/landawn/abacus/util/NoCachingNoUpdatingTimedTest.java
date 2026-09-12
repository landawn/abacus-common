package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NoCachingNoUpdating.Timed;

public class NoCachingNoUpdatingTimedTest extends NoCachingNoUpdatingTestSupport {
    @Test
    public void testTimed_of_normal() {
        Timed<String> timed = Timed.of("value", 12345L);
        assertNotNull(timed);
        assertEquals("value", timed.value());
        assertEquals(12345L, timed.timestamp());
    }

    @Test
    public void testTimed_of_nullValue() {
        Timed<String> timed = Timed.of(null, 12345L);
        assertNotNull(timed);
        assertEquals(null, timed.value());
        assertEquals(12345L, timed.timestamp());
    }

    @Test
    public void testTimed_value() {
        Timed<String> timed = Timed.of("value", 12345L);
        assertEquals("value", timed.value());
    }

    @Test
    public void testTimed_timestamp() {
        Timed<String> timed = Timed.of("value", 12345L);
        assertEquals(12345L, timed.timestamp());
    }

    @Test
    public void testTimed_hashCode_sameObject() {
        Timed<String> timed1 = Timed.of("value", 12345L);
        Timed<String> timed2 = Timed.of("value", 12345L);
        assertEquals(timed1.hashCode(), timed2.hashCode());
    }

    @Test
    public void testTimed_hashCode_differentValue() {
        Timed<String> timed1 = Timed.of("value1", 12345L);
        Timed<String> timed2 = Timed.of("value2", 12345L);
        assertNotNull(Integer.valueOf(timed1.hashCode()));
        assertNotNull(Integer.valueOf(timed2.hashCode()));
    }

    @Test
    public void testTimed_hashCode_differentTimestamp() {
        Timed<String> timed1 = Timed.of("value", 12345L);
        Timed<String> timed2 = Timed.of("value", 67890L);
        assertNotNull(Integer.valueOf(timed1.hashCode()));
        assertNotNull(Integer.valueOf(timed2.hashCode()));
    }

    @Test
    public void testTimed_hashCode_nullValue() {
        Timed<String> timed = Timed.of(null, 12345L);
        assertNotNull(Integer.valueOf(timed.hashCode()));
    }

    @Test
    public void testTimed_equals_sameObject() {
        Timed<String> timed = Timed.of("value", 12345L);
        assertTrue(timed.equals(timed));
    }

    @Test
    public void testTimed_equals_equalObjects() {
        Timed<String> timed1 = Timed.of("value", 12345L);
        Timed<String> timed2 = Timed.of("value", 12345L);
        assertTrue(timed1.equals(timed2));
    }

    @Test
    public void testTimed_equals_differentValue() {
        Timed<String> timed1 = Timed.of("value1", 12345L);
        Timed<String> timed2 = Timed.of("value2", 12345L);
        assertFalse(timed1.equals(timed2));
    }

    @Test
    public void testTimed_equals_differentTimestamp() {
        Timed<String> timed1 = Timed.of("value", 12345L);
        Timed<String> timed2 = Timed.of("value", 67890L);
        assertFalse(timed1.equals(timed2));
    }

    @Test
    public void testTimed_equals_nullValue() {
        Timed<String> timed1 = Timed.of(null, 12345L);
        Timed<String> timed2 = Timed.of(null, 12345L);
        assertTrue(timed1.equals(timed2));
    }

    @Test
    public void testTimed_equals_oneNullValue() {
        Timed<String> timed1 = Timed.of("value", 12345L);
        Timed<String> timed2 = Timed.of(null, 12345L);
        assertFalse(timed1.equals(timed2));
    }

    @Test
    public void testTimed_equals_nullObject() {
        Timed<String> timed = Timed.of("value", 12345L);
        assertFalse(timed.equals(null));
    }

    @Test
    public void testTimed_equals_differentClass() {
        Timed<String> timed = Timed.of("value", 12345L);
        assertFalse(timed.equals("not a Timed object"));
    }

    @Test
    public void testTimed_toString() {
        Timed<String> timed = Timed.of("value", 12345L);
        String result = timed.toString();
        assertNotNull(result);
        assertTrue(result.contains("12345"));
        assertTrue(result.contains("value"));
    }

    @Test
    public void testTimed_toString_nullValue() {
        Timed<String> timed = Timed.of(null, 12345L);
        String result = timed.toString();
        assertNotNull(result);
        assertTrue(result.contains("12345"));
    }

    @Test
    public void testTimedOf() {
        long timestamp = System.currentTimeMillis();
        NoCachingNoUpdating.Timed<String> timed = NoCachingNoUpdating.Timed.of("Hello", timestamp);
        Assertions.assertNotNull(timed);
        Assertions.assertEquals("Hello", timed.value());
        Assertions.assertEquals(timestamp, timed.timestamp());
    }

    @Test
    public void testTimedValue() {
        NoCachingNoUpdating.Timed<Integer> timed = NoCachingNoUpdating.Timed.of(42, 1000L);
        Assertions.assertEquals(Integer.valueOf(42), timed.value());
    }

    @Test
    public void testTimedTimestamp() {
        long timestamp = 123456789L;
        NoCachingNoUpdating.Timed<String> timed = NoCachingNoUpdating.Timed.of("test", timestamp);
        Assertions.assertEquals(timestamp, timed.timestamp());
    }

    @Test
    public void testTimedHashCode() {
        long timestamp = 1000L;
        NoCachingNoUpdating.Timed<String> timed1 = NoCachingNoUpdating.Timed.of("test", timestamp);
        NoCachingNoUpdating.Timed<String> timed2 = NoCachingNoUpdating.Timed.of("test", timestamp);
        Assertions.assertEquals(timed1.hashCode(), timed2.hashCode());
    }

    @Test
    public void testTimedEquals() {
        long timestamp = 1000L;
        NoCachingNoUpdating.Timed<String> timed1 = NoCachingNoUpdating.Timed.of("test", timestamp);
        NoCachingNoUpdating.Timed<String> timed2 = NoCachingNoUpdating.Timed.of("test", timestamp);
        NoCachingNoUpdating.Timed<String> timed3 = NoCachingNoUpdating.Timed.of("other", timestamp);
        NoCachingNoUpdating.Timed<String> timed4 = NoCachingNoUpdating.Timed.of("test", 2000L);

        Assertions.assertEquals(timed1, timed1);
        Assertions.assertEquals(timed1, timed2);
        Assertions.assertNotEquals(timed1, timed3);
        Assertions.assertNotEquals(timed1, timed4);
        Assertions.assertNotEquals(timed1, null);
        Assertions.assertNotEquals(timed1, "string");
    }

    @Test
    public void testTimedEqualsWithNull() {
        NoCachingNoUpdating.Timed<String> timed1 = NoCachingNoUpdating.Timed.of(null, 1000L);
        NoCachingNoUpdating.Timed<String> timed2 = NoCachingNoUpdating.Timed.of(null, 1000L);
        NoCachingNoUpdating.Timed<String> timed3 = NoCachingNoUpdating.Timed.of("test", 1000L);

        Assertions.assertEquals(timed1, timed2);
        Assertions.assertNotEquals(timed1, timed3);
    }

    @Test
    public void testTimedToString() {
        NoCachingNoUpdating.Timed<String> timed = NoCachingNoUpdating.Timed.of("value", 12345L);
        String str = timed.toString();
        Assertions.assertEquals("12345: value", str);
    }

    @Test
    public void testTimedToStringWithNull() {
        NoCachingNoUpdating.Timed<String> timed = NoCachingNoUpdating.Timed.of(null, 12345L);
        String str = timed.toString();
        Assertions.assertEquals("12345: null", str);
    }

    @Test
    public void testTimed_hashCodeWithNullValue() {
        Timed<String> timed1 = Timed.of(null, 1000L);
        Timed<String> timed2 = Timed.of(null, 1000L);
        assertEquals(timed1.hashCode(), timed2.hashCode());

        Timed<String> timed3 = Timed.of("value", 1000L);
        assertNotEquals(timed1.hashCode(), timed3.hashCode());
    }

    @Test
    public void testTimed_equalsEdgeCases() {
        Timed<String> timed = Timed.of("value", 1000L);

        assertTrue(timed.equals(timed));

        assertFalse(timed.equals(null));

        assertFalse(timed.equals("not a timed"));
        assertFalse(timed.equals(new Object()));

        Timed<String> different1 = Timed.of("value", 2000L);
        assertFalse(timed.equals(different1));

        Timed<String> different2 = Timed.of("other", 1000L);
        assertFalse(timed.equals(different2));

        Timed<String> different3 = Timed.of("other", 2000L);
        assertFalse(timed.equals(different3));
    }

    @Test
    public void testTimed_of() {
        long timestamp = System.currentTimeMillis();
        Timed<String> timed = Timed.of("value", timestamp);

        assertEquals("value", timed.value());
        assertEquals(timestamp, timed.timestamp());
    }

    @Test
    public void testTimed_hashCode() {
        long timestamp = 1000L;
        Timed<String> timed1 = Timed.of("value", timestamp);
        Timed<String> timed2 = Timed.of("value", timestamp);

        assertEquals(timed1.hashCode(), timed2.hashCode());
    }

    @Test
    public void testTimed_equals() {
        long timestamp = 1000L;
        Timed<String> timed1 = Timed.of("value", timestamp);
        Timed<String> timed2 = Timed.of("value", timestamp);
        Timed<String> timed3 = Timed.of("different", timestamp);
        Timed<String> timed4 = Timed.of("value", 2000L);

        assertTrue(timed1.equals(timed1));
        assertTrue(timed1.equals(timed2));
        assertFalse(timed1.equals(timed3));
        assertFalse(timed1.equals(timed4));
        assertFalse(timed1.equals(null));
        assertFalse(timed1.equals("not a timed"));
    }

    @Test
    public void testTimed_withNullValue() {
        Timed<String> timed = Timed.of(null, 1000L);
        assertNull(timed.value());
        assertEquals(1000L, timed.timestamp());

        Timed<String> timed2 = Timed.of(null, 1000L);
        assertTrue(timed.equals(timed2));
    }

    @Test
    public void testTimedCopySnapshotsReusableStateAndArrayHashMatchesEquals() {
        class MutableTimed<T> extends Timed<T> {
            MutableTimed(final T value, final long timestamp) {
                super(value, timestamp);
            }

            void update(final T value, final long timestamp) {
                set(value, timestamp);
            }
        }

        StringBuilder mutableValue = new StringBuilder("value");
        MutableTimed<StringBuilder> reusable = new MutableTimed<>(mutableValue, 1L);
        Timed<StringBuilder> snapshot = reusable.copy();
        reusable.update(new StringBuilder("next"), 2L);
        assertNotSame(reusable, snapshot);
        assertSame(mutableValue, snapshot.value());
        assertEquals(1L, snapshot.timestamp());

        int[] values = { 1, 2, 3 };
        Timed<int[]> first = Timed.of(values, 10L);
        Timed<int[]> second = Timed.of(new int[] { 1, 2, 3 }, 10L);
        assertNotEquals(first, second);
        Timed<int[]> sameRef = Timed.of(values, 10L);
        assertEquals(first, sameRef);
        assertEquals(first.hashCode(), sameRef.hashCode());
    }
}
