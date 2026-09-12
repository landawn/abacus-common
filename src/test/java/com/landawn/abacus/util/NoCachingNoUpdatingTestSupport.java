package com.landawn.abacus.util;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableDeque;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableEntry;
import com.landawn.abacus.util.NoCachingNoUpdating.Timed;

public abstract class NoCachingNoUpdatingTestSupport extends TestBase {

    protected NoCachingNoUpdating.DisposableArray<String> disposableArray;
    protected String[] sourceArray;
    protected NoCachingNoUpdating.DisposableDeque<Integer> disposableDeque;
    protected Deque<Integer> sourceDeque;
    protected NoCachingNoUpdating.DisposableEntry<String, Integer> disposableEntry;
    protected Map.Entry<String, Integer> sourceEntry;
    protected NoCachingNoUpdating.Timed<String> timed;
    protected long timestamp;
    protected String value;

    @BeforeEach
    public void setUp() {
        sourceArray = new String[] { "a", "b", "c" };
        disposableArray = NoCachingNoUpdating.DisposableArray.wrap(sourceArray);

        sourceDeque = new ArrayDeque<>(Arrays.asList(1, 2, 3));
        disposableDeque = NoCachingNoUpdating.DisposableDeque.wrap(sourceDeque);

        sourceEntry = new AbstractMap.SimpleEntry<>("key", 123);
        disposableEntry = NoCachingNoUpdating.DisposableEntry.wrap(sourceEntry);

        value = "test-value";
        timestamp = System.currentTimeMillis();
        timed = NoCachingNoUpdating.Timed.of(value, timestamp);
    }

    protected static String[] disposableArrayFixtureSource() {
        return new String[] { "a", "b", "c" };
    }

    protected static DisposableArray<String> disposableArrayFixture() {
        return DisposableArray.wrap(disposableArrayFixtureSource());
    }

    protected static DisposableDeque<Integer> disposableDequeFixture() {
        return DisposableDeque.wrap(new ArrayDeque<>(Arrays.asList(1, 2, 3)));
    }

    protected static DisposableEntry<String, Integer> disposableEntryFixture() {
        return DisposableEntry.wrap(new AbstractMap.SimpleEntry<>("key", 123));
    }

    protected static String timedFixtureValue() {
        return "test-value";
    }

    protected static long timedFixtureTimestamp() {
        return 12345L;
    }

    protected static Timed<String> timedFixture() {
        return Timed.of(timedFixtureValue(), timedFixtureTimestamp());
    }
}
