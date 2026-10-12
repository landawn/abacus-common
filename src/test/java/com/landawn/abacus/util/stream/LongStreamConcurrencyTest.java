package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/** LongStream concurrency contracts. */
public class LongStreamConcurrencyTest extends LongStreamTestSupport {

    // debounce

    @Test
    public void testDebounce_coldStreamEmitsOnlyLastElement() {
        // A cold in-memory stream yields all elements with ~0 inter-arrival gap, so every element
        // except the last is superseded within the quiet window; only the final element survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new long[] { 5L },
                LongStream.of(1L, 2L, 3L, 4L, 5L).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray());
    }

    @Test
    public void testDebounce_emptyEmitsNothing() {
        org.junit.jupiter.api.Assertions.assertEquals(0, LongStream.empty().debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray().length);
    }

    @Test
    public void testDebounce_invalidDurationThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> LongStream.of(1L, 2L, 3L).debounce((com.landawn.abacus.util.Duration) null).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> LongStream.of(1L, 2L, 3L).debounce(com.landawn.abacus.util.Duration.ofMillis(0)).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> LongStream.of(1L, 2L, 3L).debounce(com.landawn.abacus.util.Duration.ofMillis(-100)).toArray());
    }

    @Test
    public void testDebounce_singleElementSurvives() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new long[] { 42L },
                LongStream.of(42L).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray());
    }

    @Test
    public void testDebounce_slowSourceAllSurviveWhenGapAtLeastDuration() {
        // delay() makes each element arrive >= 60ms after the previous; with a 20ms quiet window
        // every element clears the window and survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new long[] { 1L, 2L, 3L },
                LongStream.of(1L, 2L, 3L)
                        .delay(com.landawn.abacus.util.Duration.ofMillis(60))
                        .debounce(com.landawn.abacus.util.Duration.ofMillis(20))
                        .toArray());
    }

    @Test
    public void testDelay_singleArg() {
        long startTime = System.currentTimeMillis();
        long[] result = LongStream.of(1L, 2L).delay(Duration.ofMillis(50)).toArray();
        long endTime = System.currentTimeMillis();
        assertArrayEquals(new long[] { 1L, 2L }, result);
        assertTrue(endTime - startTime >= 40);
    }

    @Test
    public void testDelay_withTimeUnit() {
        long[] result = LongStream.of(1L, 2L).delay(Duration.ofMillis(50)).toArray();
        assertArrayEquals(new long[] { 1L, 2L }, result);
    }

    // interval

    @Test
    public void testInterval() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long[] timestamps = LongStream.interval(10).limit(3).toArray();
        long endTime = System.currentTimeMillis();

        assertEquals(3, timestamps.length);
        assertTrue(endTime - startTime >= 20);
    }

    @Test
    public void testInterval_threeArgs() {
        LongStream stream = LongStream.interval(10, 50, java.util.concurrent.TimeUnit.MILLISECONDS);
        long[] first3 = stream.limit(3).toArray();
        assertEquals(3, first3.length);
    }

    @Test
    public void testInterval_twoArgs() {
        LongStream stream = LongStream.interval(10, 50);
        long[] first5 = stream.limit(5).toArray();
        assertEquals(5, first5.length);
    }

    @Test
    public void testIntervalRejectsInvalidTiming() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.interval(-1, 1, TimeUnit.MILLISECONDS));
        assertThrows(IllegalArgumentException.class, () -> LongStream.interval(0, 0, TimeUnit.MILLISECONDS));
        assertThrows(IllegalArgumentException.class, () -> LongStream.interval(0, -1, TimeUnit.MILLISECONDS));
        assertThrows(IllegalArgumentException.class, () -> LongStream.interval(0, 1, TimeUnit.NANOSECONDS));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> LongStream.interval(0, 1, null)).getMessage().contains("unit"));
    }

    @Test
    public void testIntervalWithDelay() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long[] timestamps = LongStream.interval(50, 10).limit(3).toArray();
        long endTime = System.currentTimeMillis();

        assertEquals(3, timestamps.length);
        assertTrue(endTime - startTime >= 70);
    }

    @Test
    public void testIntervalWithTimeUnit() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long[] timestamps = LongStream.interval(50, 10, TimeUnit.MILLISECONDS).limit(3).toArray();
        long endTime = System.currentTimeMillis();

        assertEquals(3, timestamps.length);
        assertTrue(endTime - startTime >= 70);
    }

    // parallel

    @Test
    public void testParallel() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        LongStream parallel = stream.parallel();
        assertTrue(parallel.isParallel());
    }

    @Test
    public void testParallelMapSum() {
        long expectedSum = LongStream.range(0, 100).map(i -> i * 2).sum();
        long actualSum = LongStream.range(0, 100).parallel().map(i -> i * 2).sum();
        assertEquals(expectedSum, actualSum);
    }

    @Test
    public void testParallelStreamOperations() {
        long sum = LongStream.range(1L, 100L).parallel().filter(n -> n % 2 == 0).map(n -> n * 2).sum();

        assertTrue(sum > 0);

        // Verify parallel flag is maintained through operations
        LongStream parallelStream = LongStream.of(1L, 2L, 3L).parallel().filter(n -> n > 0).map(n -> n * 2);
        assertTrue(parallelStream.isParallel());
    }

    @Test
    public void testParallelWithMaxThreadNum() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        LongStream parallel = stream.parallel(2);
        assertTrue(parallel.isParallel());
        assertEquals(15L, parallel.sum());
    }

    @Test
    public void testRateLimited_perSecond() {
        long[] result = LongStream.of(1L, 2L, 3L).rateLimited(100).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testRateLimited_withTimeUnit() {
        long[] result = LongStream.of(1L, 2L, 3L).rateLimited(3).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testSequential() {
        LongStream stream = LongStream.of(1L, 2L, 3L).parallel();
        assertTrue(stream.isParallel());

        LongStream sequential = stream.sequential();
        assertFalse(sequential.isParallel());
    }

    @Test
    public void testSequential_AfterParallel() {
        long sum = LongStream.of(1, 2, 3).parallel().sequential().sum();
        assertEquals(6, sum);
    }

    @Test
    public void testSps() {
        LongStream.of(1L, 2L, 3L).sps(s -> s.map(x -> x * 2)).sps(s -> s.filter(x -> x > 2)).forEach(n -> assertTrue(n > 2 && n % 2 == 0));
    }

    @Test
    public void testSps_HappyPath() {
        long sum = LongStream.of(1, 2, 3, 4, 5).sps(s -> s.map(x -> x * 2)).sum();
        assertEquals(30, sum);
    }

}
