package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings;

/** ShortStream concurrency contracts. */
public class ShortStreamConcurrencyTest extends ShortStreamTestSupport {

    // debounce

    @Test
    public void testDebounce_coldStreamEmitsOnlyLastElement() {
        // A cold in-memory stream yields all elements with ~0 inter-arrival gap, so every element
        // except the last is superseded within the quiet window; only the final element survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new short[] { (short) 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray());
    }

    @Test
    public void testDebounce_emptyEmitsNothing() {
        org.junit.jupiter.api.Assertions.assertEquals(0, ShortStream.empty().debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray().length);
    }

    @Test
    public void testDebounce_invalidDurationThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ShortStream.of((short) 1, (short) 2, (short) 3).debounce((com.landawn.abacus.util.Duration) null).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ShortStream.of((short) 1, (short) 2, (short) 3).debounce(com.landawn.abacus.util.Duration.ofMillis(0)).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ShortStream.of((short) 1, (short) 2, (short) 3).debounce(com.landawn.abacus.util.Duration.ofMillis(-100)).toArray());
    }

    @Test
    public void testDebounce_singleElementSurvives() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new short[] { (short) 42 },
                ShortStream.of((short) 42).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray());
    }

    @Test
    public void testDebounce_slowSourceAllSurviveWhenGapAtLeastDuration() {
        // delay() makes each element arrive >= 60ms after the previous; with a 20ms quiet window
        // every element clears the window and survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new short[] { (short) 1, (short) 2, (short) 3 },
                ShortStream.of((short) 1, (short) 2, (short) 3)
                        .delay(com.landawn.abacus.util.Duration.ofMillis(60))
                        .debounce(com.landawn.abacus.util.Duration.ofMillis(20))
                        .toArray());
    }

    @Test
    public void testDelay() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        createShortStream((short) 1, (short) 2, (short) 3).delay(Duration.ofMillis(50)).toArray();

        long elapsedTime = System.currentTimeMillis() - startTime;
        assertTrue(elapsedTime >= 100);
    }

    @Test
    public void testDelayWithJavaDuration() {
        ShortStream stream = createShortStream((short) 1, (short) 2).delay(java.time.Duration.ofMillis(1));
        assertArrayEquals(new short[] { 1, 2 }, stream.toArray());
    }

    // parallel

    @Test
    public void testParallel() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).parallel();
        assertTrue(stream.isParallel());
    }

    @Test
    public void testParallelStreamOperations() {
        long sum = ShortStream.range((short) 1, (short) 100).parallel().filter(n -> n % 2 == 0).map(n -> (short) (n * 2)).sum();

        assertTrue(sum > 0);

        // Verify parallel flag is maintained through operations
        ShortStream parallelStream = ShortStream.of((short) 1, (short) 2, (short) 3).parallel().filter(n -> n > 0).map(n -> (short) (n * 2));
        assertTrue(parallelStream.isParallel());
    }

    @Test
    public void testParallelWithMaxThreadNum() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).parallel(2).map(n -> (short) (n * 2)).toArray();

        Arrays.sort(result);
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
    }

    @Test
    public void testParallelWithParallelSettings() {
        ParallelSettings ps = ParallelSettings.builder().maxThreadNum(2).build();

        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).parallel(ps).map(n -> (short) (n * 2)).toArray();

        Arrays.sort(result);
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
    }

    // rateLimited

    @Test
    public void testRateLimited() throws InterruptedException {
        RateLimiter rateLimiter = RateLimiter.create(10);
        long startTime = System.currentTimeMillis();

        createShortStream((short) 1, (short) 2, (short) 3).rateLimited(rateLimiter).toArray();

        long elapsedTime = System.currentTimeMillis() - startTime;
        assertTrue(elapsedTime >= 200);
    }

    @Test
    public void testRateLimitedWithPermitsPerSecond() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        createShortStream((short) 1, (short) 2, (short) 3).rateLimited(10.0).toArray();

        long elapsedTime = System.currentTimeMillis() - startTime;
        assertTrue(elapsedTime >= 200);
    }

    @Test
    public void testRateLimitedWithPermitsPerSecondDouble() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).rateLimited(1000.0).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testSequential() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).parallel().sequential();
        assertFalse(stream.isParallel());
    }

    @Test
    @DisplayName("Test sequential() and parallel() methods")
    public void testSequentialParallel() {
        assertFalse(stream.isParallel());

        ShortStream parallelStream = stream.parallel();
        assertTrue(parallelStream.isParallel());

        ShortStream sequentialStream = parallelStream.sequential();
        assertFalse(sequentialStream.isParallel());

        ShortStream parallel2 = createShortStream((short) 1, (short) 2, (short) 3).parallel(2);
        assertTrue(parallel2.isParallel());
    }

    // sps

    @Test
    public void testSps() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).sps(s -> s.map(n -> (short) (n * 2))).toArray();

        Arrays.sort(result);
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
    }

    @Test
    public void testSpsMethod() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).sps(s -> s.filter(v -> v > 2)).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testSpsWithMaxThreadNum() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).sps(2, s -> s.map(n -> (short) (n * 2))).toArray();

        Arrays.sort(result);
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
    }

    @Test
    public void testSpsWithMaxThreadNumMethod() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).sps(2, s -> s.filter(v -> v > 2)).toArray();
        assertEquals(3, result.length);
    }

}
