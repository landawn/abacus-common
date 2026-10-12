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

/** CharStream concurrency contracts. */
public class CharStreamConcurrencyTest extends CharStreamTestSupport {

    // debounce

    @Test
    public void testDebounce_coldStreamEmitsOnlyLastElement() {
        // A cold in-memory stream yields all elements with ~0 inter-arrival gap, so every element
        // except the last is superseded within the quiet window; only the final element survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new char[] { (char) 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray());
    }

    @Test
    public void testDebounce_emptyEmitsNothing() {
        org.junit.jupiter.api.Assertions.assertEquals(0, CharStream.empty().debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray().length);
    }

    @Test
    public void testDebounce_invalidDurationThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> CharStream.of((char) 1, (char) 2, (char) 3).debounce((com.landawn.abacus.util.Duration) null).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> CharStream.of((char) 1, (char) 2, (char) 3).debounce(com.landawn.abacus.util.Duration.ofMillis(0)).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> CharStream.of((char) 1, (char) 2, (char) 3).debounce(com.landawn.abacus.util.Duration.ofMillis(-100)).toArray());
    }

    @Test
    public void testDebounce_singleElementSurvives() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new char[] { (char) 42 },
                CharStream.of((char) 42).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray());
    }

    @Test
    public void testDebounce_slowSourceAllSurviveWhenGapAtLeastDuration() {
        // delay() makes each element arrive >= 60ms after the previous; with a 20ms quiet window
        // every element clears the window and survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new char[] { (char) 1, (char) 2, (char) 3 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .delay(com.landawn.abacus.util.Duration.ofMillis(60))
                        .debounce(com.landawn.abacus.util.Duration.ofMillis(20))
                        .toArray());
    }

    // delay

    @Test
    public void testDelay() {
        Duration delay = Duration.ofMillis(10);
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        CharStream result = stream.delay(delay);
        assertNotNull(result);
    }

    @Test
    public void testDelay_NullDuration() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        assertThrows(IllegalArgumentException.class, () -> stream.delay((Duration) null));
    }

    @Test
    public void testDelay_WithDuration() {
        CharStream stream = createCharStream('a', 'b');
        char[] result = stream.delay(Duration.ofMillis(1)).toArray();
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testDelayWithJavaDuration() {
        CharStream stream = createCharStream('a', 'b').delay(java.time.Duration.ofMillis(1));
        assertArrayEquals(new char[] { 'a', 'b' }, stream.toArray());
    }

    // parallel

    @Test
    public void testParallel() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharStream result = stream.parallel();
        assertNotNull(result);
    }

    @Test
    public void testParallel_WithMaxThreadNum() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharStream result = stream.parallel(2);
        assertNotNull(result);
        assertTrue(result.isParallel());
    }



    // Parallel operations
    @Test
    public void testParallelStreamOperationsForCoverage() {
        CharStream stream = CharStream.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h').parallel();
        char[] result = stream.filter(c -> c > 'c').map(c -> (char) (c + 1)).toArray();
        assertEquals(5, result.length);
    }

    // rateLimited

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(10);
        CharStream stream = createCharStream('a', 'b', 'c');
        CharStream result = stream.rateLimited(rateLimiter);
        assertNotNull(result);
    }

    @Test
    public void testRateLimited_NullRateLimiter() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertThrows(IllegalArgumentException.class, () -> stream.rateLimited(null));
    }

    @Test
    public void testRateLimited_WithRateLimiter() {
        CharStream stream = createCharStream('a', 'b', 'c');
        RateLimiter rateLimiter = RateLimiter.create(1000);
        char[] result = stream.rateLimited(rateLimiter).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testRateLimitedWithPermitsPerSecondDouble() {
        char[] result = createCharStream('a', 'b', 'c').rateLimited(1000.0).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testSequential() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharStream result = stream.sequential();
        assertNotNull(result);
        assertFalse(result.isParallel());
    }

    @Test
    public void testSps_WithMaxThreadNum() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        char[] result = stream.sps(2, s -> s.filter(c -> c > 'b')).toArray();
        assertHaveSameElements(new char[] { 'c', 'd' }, result);
    }

}
