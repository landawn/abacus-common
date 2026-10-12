package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Duration;

/** DoubleStream concurrency contracts. */
public class DoubleStreamConcurrencyTest extends DoubleStreamTestSupport {

    // debounce

    @Test
    public void testDebounce_coldStreamEmitsOnlyLastElement() {
        // A cold in-memory stream yields all elements with ~0 inter-arrival gap, so every element
        // except the last is superseded within the quiet window; only the final element survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[] { 5.0 },
                DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray(), 0.0);
    }

    @Test
    public void testDebounce_emptyEmitsNothing() {
        org.junit.jupiter.api.Assertions.assertEquals(0, DoubleStream.empty().debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray().length);
    }

    @Test
    public void testDebounce_invalidDurationThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> DoubleStream.of(1.0, 2.0, 3.0).debounce((com.landawn.abacus.util.Duration) null).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> DoubleStream.of(1.0, 2.0, 3.0).debounce(com.landawn.abacus.util.Duration.ofMillis(0)).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> DoubleStream.of(1.0, 2.0, 3.0).debounce(com.landawn.abacus.util.Duration.ofMillis(-100)).toArray());
    }

    @Test
    public void testDebounce_singleElementSurvives() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[] { 42.0 },
                DoubleStream.of(42.0).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray(), 0.0);
    }

    @Test
    public void testDebounce_slowSourceAllSurviveWhenGapAtLeastDuration() {
        // delay() makes each element arrive >= 60ms after the previous; with a 20ms quiet window
        // every element clears the window and survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[] { 1.0, 2.0, 3.0 },
                DoubleStream.of(1.0, 2.0, 3.0)
                        .delay(com.landawn.abacus.util.Duration.ofMillis(60))
                        .debounce(com.landawn.abacus.util.Duration.ofMillis(20))
                        .toArray(),
                0.0);
    }

    @Test
    public void testParallel() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        DoubleStream parallel = stream.parallel();
        assertNotNull(parallel);
    }

    @Test
    public void testParallelFilterMap() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0);
        double[] result = stream.parallel().filter(x -> x % 2 == 0).map(x -> x * 2).toArray();
        assertEquals(4, result.length);
    }

    @Test
    public void testSequential() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        DoubleStream sequential = stream.sequential();
        assertNotNull(sequential);
    }

    @Test
    public void testSequential_AfterParallel() {
        double sum = DoubleStream.of(1.0, 2.0, 3.0).parallel().sequential().sum();
        assertEquals(6.0, sum, 0.001);
    }

    @Test
    public void testSps_HappyPath() {
        double sum = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).sps(s -> s.map(x -> x * 2)).sum();
        assertEquals(30.0, sum, 0.001);
    }

}
