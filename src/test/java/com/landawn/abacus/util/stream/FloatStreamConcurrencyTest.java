package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Duration;

/** FloatStream concurrency contracts. */
public class FloatStreamConcurrencyTest extends FloatStreamTestSupport {

    // debounce

    @Test
    public void testDebounce_coldStreamEmitsOnlyLastElement() {
        // A cold in-memory stream yields all elements with ~0 inter-arrival gap, so every element
        // except the last is superseded within the quiet window; only the final element survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[] { 5.0f },
                FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray(), 0.0f);
    }

    @Test
    public void testDebounce_emptyEmitsNothing() {
        org.junit.jupiter.api.Assertions.assertEquals(0, FloatStream.empty().debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray().length);
    }

    @Test
    public void testDebounce_invalidDurationThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> FloatStream.of(1.0f, 2.0f, 3.0f).debounce((com.landawn.abacus.util.Duration) null).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> FloatStream.of(1.0f, 2.0f, 3.0f).debounce(com.landawn.abacus.util.Duration.ofMillis(0)).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> FloatStream.of(1.0f, 2.0f, 3.0f).debounce(com.landawn.abacus.util.Duration.ofMillis(-100)).toArray());
    }

    @Test
    public void testDebounce_singleElementSurvives() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[] { 42.0f },
                FloatStream.of(42.0f).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray(), 0.0f);
    }

    @Test
    public void testDebounce_slowSourceAllSurviveWhenGapAtLeastDuration() {
        // delay() makes each element arrive >= 60ms after the previous; with a 20ms quiet window
        // every element clears the window and survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f },
                FloatStream.of(1.0f, 2.0f, 3.0f)
                        .delay(com.landawn.abacus.util.Duration.ofMillis(60))
                        .debounce(com.landawn.abacus.util.Duration.ofMillis(20))
                        .toArray(),
                0.0f);
    }

    @Test
    public void testDelay() {
        long startTime = System.currentTimeMillis();
        FloatStream.of(1f, 2f, 3f).delay(com.landawn.abacus.util.Duration.ofMillis(50)).count();
        long endTime = System.currentTimeMillis();

        assertTrue((endTime - startTime) >= 100, "Stream processing should be delayed");
    }

    // parallel

    @Test
    @DisplayName("parallel() should return parallel stream")
    public void testParallel() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        FloatStream parallel = stream.parallel();
        assertTrue(parallel.isParallel());
    }

    @Test
    public void testParallelMapSum() {
        for (int k = 0; k < 1000; k++) {
            float[] data = new float[1000];
            for (int i = 0; i < data.length; i++) {
                data[i] = i + 1;
            }

            double expectedSum = FloatStream.of(data).map(f -> f * 2.0f).sum();
            double actualSum = FloatStream.of(data).parallel().map(f -> f * 2.0f).sum();

            assertEquals(expectedSum, actualSum, DELTA);
        }
    }



    // Parallel operations
    @Test
    @DisplayName("parallel stream operations additional")
    public void testParallelOperationsAdditional() {
        FloatStream stream = FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f).parallel();
        float[] result = stream.filter(f -> f > 3.0f).map(f -> f * 2).toArray();
        assertEquals(5, result.length);
    }

    @Test
    @DisplayName("parallel operations should work correctly")
    public void testParallelProcessing() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        double sum = stream.parallel().mapToDouble(x -> x).sum();
        assertEquals(15.0, sum, 0.001);
    }

    @Test
    @DisplayName("parallel() with max thread number")
    public void testParallelWithMaxThreads() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        FloatStream parallel = stream.parallel(2);
        assertTrue(parallel.isParallel());
    }

    @Test
    @DisplayName("parallel() with max thread number should configure parallelism")
    public void testParallelWithThreads() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        FloatStream parallel = stream.parallel(2);
        assertTrue(parallel.isParallel());
    }

    @Test
    @DisplayName("sequential() should return sequential stream")
    public void testSequential() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f).parallel();
        FloatStream sequential = stream.sequential();
        assertFalse(sequential.isParallel());
    }

    @Test
    public void testSps() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).sps(s -> s.filter(f -> f > 2.0f)).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testSpsWithMaxThreadNum() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).sps(2, s -> s.filter(f -> f > 2.0f)).toArray();
        assertEquals(3, result.length);
    }

}
