package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings;

/** ByteStream concurrency contracts. */
public class ByteStreamConcurrencyTest extends ByteStreamTestSupport {

    // debounce

    @Test
    public void testDebounce_coldStreamEmitsOnlyLastElement() {
        // A cold in-memory stream yields all elements with ~0 inter-arrival gap, so every element
        // except the last is superseded within the quiet window; only the final element survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { (byte) 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray());
    }

    @Test
    public void testDebounce_emptyEmitsNothing() {
        org.junit.jupiter.api.Assertions.assertEquals(0, ByteStream.empty().debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray().length);
    }

    @Test
    public void testDebounce_invalidDurationThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ByteStream.of((byte) 1, (byte) 2, (byte) 3).debounce((com.landawn.abacus.util.Duration) null).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ByteStream.of((byte) 1, (byte) 2, (byte) 3).debounce(com.landawn.abacus.util.Duration.ofMillis(0)).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ByteStream.of((byte) 1, (byte) 2, (byte) 3).debounce(com.landawn.abacus.util.Duration.ofMillis(-100)).toArray());
    }

    @Test
    public void testDebounce_singleElementSurvives() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { (byte) 42 },
                ByteStream.of((byte) 42).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray());
    }

    @Test
    public void testDebounce_slowSourceAllSurviveWhenGapAtLeastDuration() {
        // delay() makes each element arrive >= 60ms after the previous; with a 20ms quiet window
        // every element clears the window and survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { (byte) 1, (byte) 2, (byte) 3 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .delay(com.landawn.abacus.util.Duration.ofMillis(60))
                        .debounce(com.landawn.abacus.util.Duration.ofMillis(20))
                        .toArray());
    }

    // delay

    @Test
    public void testDelay() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        Duration delay = Duration.ofMillis(50);
        long startTime = System.currentTimeMillis();
        List<Byte> result = byteStream.delay(delay).toList();
        long endTime = System.currentTimeMillis();
        assertEquals(3, result.size());
        assertTrue((endTime - startTime) >= 2 * 50, "Delay was not applied or was too short");
    }

    @Test
    public void testDelay_WithDuration() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        byte[] result = stream.delay(Duration.ofMillis(1)).toArray();
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testDelayWithJavaDuration() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2).delay(java.time.Duration.ofMillis(1));
        assertArrayEquals(new byte[] { 1, 2 }, stream.toArray());
    }

    // parallel

    @Test
    public void testParallel() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertFalse(stream.isParallel());

        ByteStream parallel = stream.parallel();
        assertTrue(parallel.isParallel());

        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, parallel.sorted().toArray());
    }

    @Test
    public void testParallel_WithMaxThreadNum() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream result = stream.parallel(2);
        assertNotNull(result);
        assertTrue(result.isParallel());
    }

    @Test
    public void testParallelMaxThreadNum() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteStream parallelStream = byteStream.parallel(2);
        assertTrue(parallelStream.isParallel());
    }

    // Parallel operations
    @Test
    public void testParallelStreamOperations() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8).parallel();
        byte[] result = stream.filter(b -> b % 2 == 0).map(b -> (byte) (b * 2)).toArray();
        assertEquals(4, result.length);
    }

    @Test
    public void testParallelWithMaxThreadNum() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream parallel = stream.parallel(2);

        assertTrue(parallel.isParallel());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, parallel.toArray());
    }

    @Test
    public void testParallelSettings() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteStream.ParallelSettings settings = ParallelSettings.builder().maxThreadNum(2).build();
        ByteStream parallelStream = byteStream.parallel(settings);
        assertTrue(parallelStream.isParallel());
    }

    // rateLimited

    @Test
    public void testRateLimited() {
        byteStream = createByteStream(TEST_ARRAY);
        long startTime = System.currentTimeMillis();
        List<Byte> result = byteStream.rateLimited(2.0).toList();
        long endTime = System.currentTimeMillis();
        assertEquals(5, result.size());
        assertTrue((endTime - startTime) >= 2000, "Rate limit was not applied or was too fast");
    }

    @Test
    public void testRateLimited_WithRateLimiter() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        RateLimiter rateLimiter = RateLimiter.create(1000);
        byte[] result = stream.rateLimited(rateLimiter).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testRateLimitedWithPermitsPerSecond() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);

        long startTime = System.currentTimeMillis();
        byte[] result = stream.rateLimited(100).toArray();
        long duration = System.currentTimeMillis() - startTime;

        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
        assertTrue(duration >= 0);
    }

    @Test
    public void testRateLimitedWithPermitsPerSecondDouble() {
        byte[] result = createByteStream((byte) 1, (byte) 2, (byte) 3).rateLimited(1000.0).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testRateLimitedWithRateLimiter() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        RateLimiter limiter = RateLimiter.create(1.0);
        long startTime = System.currentTimeMillis();
        List<Byte> result = byteStream.rateLimited(limiter).toList();
        long endTime = System.currentTimeMillis();
        assertEquals(3, result.size());
        assertTrue((endTime - startTime) >= 2000, "Rate limiter not working as expected");
    }

    @Test
    public void testSequential() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 }).parallel();
        assertTrue(byteStream.isParallel());
        assertFalse(byteStream.sequential().isParallel());
    }

    // sps

    @Test
    public void testSps() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);

        ByteStream result = stream.sps(s -> s.map(b -> (byte) (b * 2)));

        assertFalse(result.isParallel());
        assertArrayEquals(new byte[] { 2, 4, 6, 8, 10 }, result.sorted().toArray());
    }

    @Test
    public void testSps_WithMaxThreadNum() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        byte[] result = stream.sps(2, s -> s.filter(b -> b > 2)).toArray();
        assertHaveSameElements(new byte[] { 3, 4 }, result);
    }

    @Test
    public void testSpsFunction() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Integer> result = byteStream.sps(s -> s.mapToInt(b -> b * 10)).toList();
        assertHaveSameElements(Arrays.asList(10, 20, 30), result);
        assertFalse(byteStream.isParallel());
    }

    @Test
    public void testSpsIntFunction() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Integer> result = byteStream.sps(2, s -> s.mapToInt(b -> b * 10)).toList();
        assertHaveSameElements(Arrays.asList(10, 20, 30), result);
        assertFalse(byteStream.isParallel());
    }

    @Test
    public void testSpsWithMaxThreadNum() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);

        ByteStream result = stream.sps(2, s -> s.map(b -> (byte) (b * 2)));

        assertFalse(result.isParallel());
        assertArrayEquals(new byte[] { 2, 4, 6, 8, 10 }, result.sorted().toArray());
    }

}
