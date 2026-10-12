package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.RateLimiter;

/** IntStream concurrency contracts. */
public class IntStreamConcurrencyTest extends IntStreamTestSupport {

    // debounce

    @Test
    public void testDebounce_coldStreamEmitsOnlyLastElement() {
        // A cold in-memory stream yields all elements with ~0 inter-arrival gap, so every element
        // except the last is superseded within the quiet window; only the final element survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 5 },
                IntStream.of(1, 2, 3, 4, 5).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 5 },
                IntStream.of(1, 2, 3, 4, 5).parallel().debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray());
    }

    @Test
    public void testDebounce_emptyEmitsNothing() {
        org.junit.jupiter.api.Assertions.assertEquals(0, IntStream.empty().debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray().length);
        org.junit.jupiter.api.Assertions.assertEquals(0, IntStream.empty().parallel().debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray().length);
    }

    @Test
    public void testDebounce_invalidDurationThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> IntStream.of(1, 2, 3).debounce((com.landawn.abacus.util.Duration) null).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> IntStream.of(1, 2, 3).debounce(com.landawn.abacus.util.Duration.ofMillis(0)).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> IntStream.of(1, 2, 3).debounce(com.landawn.abacus.util.Duration.ofMillis(-100)).toArray());
    }

    @Test
    public void testDebounce_singleElementSurvives() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 42 },
                IntStream.of(42).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray());
    }

    @Test
    public void testDebounce_slowSourceAllSurviveWhenGapAtLeastDuration() {
        // delay() makes each element arrive >= 60ms after the previous; with a 20ms quiet window
        // every element clears the window and survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 1, 2, 3 },
                IntStream.of(1, 2, 3).delay(com.landawn.abacus.util.Duration.ofMillis(60)).debounce(com.landawn.abacus.util.Duration.ofMillis(20)).toArray());
    }

    @Test
    public void testDelay() {
        Duration delay = Duration.ofMillis(100);
        long startTime = System.currentTimeMillis();

        createIntStream(1, 2, 3).delay(delay).count();

        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration >= 200, "Delay should be applied");
    }

    // parallel

    @Test
    public void testParallel() {
        stream = IntStream.range(0, 1000).parallel();
        assertTrue(stream.isParallel());

        int sum = stream.sum();
        assertEquals(499500, sum);
    }

    @Test
    public void testParallel_ReturnsParallelStream() {
        stream = IntStream.of(1, 2, 3, 4, 5).parallel();
        assertNotNull(stream);
        assertTrue(stream.isParallel());
    }

    @Test
    public void testParallelFilterMap() {
        stream = IntStream.of(1, 2, 3, 4, 5, 6, 7, 8).parallel().filter(x -> x % 2 == 0).map(x -> x * 2);
        int[] result = stream.toArray();
        assertEquals(4, result.length);
    }

    @Test
    public void testParallelWithMaxThreadNum() {
        IntStream stream = IntStream.range(0, 1000).parallel(4);
        assertTrue(stream.isParallel());
        assertEquals(499500, stream.sum());
    }

    @Test
    public void testRateLimited() throws InterruptedException {
        RateLimiter rateLimiter = RateLimiter.create(5);
        long startTime = System.currentTimeMillis();

        createIntStream(1, 2, 3).rateLimited(rateLimiter).count();

        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration >= 400, "Rate limiting should introduce delay");
    }

    // sequential

    @Test
    public void testSequential() {
        stream = IntStream.range(0, 100).parallel().sequential();
        assertFalse(stream.isParallel());
    }

    @Test
    public void testSequential_AfterParallel() {
        int sum = IntStream.of(1, 2, 3).parallel().sequential().sum();
        assertEquals(6, sum);
    }

    @Test
    public void testSequential_ReturnsSequentialStream() {
        stream = IntStream.of(1, 2, 3, 4, 5).sequential();
        assertNotNull(stream);
        assertFalse(stream.isParallel());
    }

    @Test
    public void testSps() {
        IntStream stream = IntStream.range(0, 100);
        IntStream result = stream.sps(s -> s.filter(n -> n % 2 == 0));
        assertFalse(result.isParallel());
        assertEquals(50, result.count());
    }

    @Test
    public void testSps_HappyPath() {
        int sum = IntStream.of(1, 2, 3, 4, 5).sps(s -> s.map(x -> x * 2)).sum();
        assertEquals(30, sum);
    }

}
