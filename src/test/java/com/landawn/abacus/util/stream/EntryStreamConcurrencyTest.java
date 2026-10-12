package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.RateLimiter;

/** EntryStream concurrency contracts. */
public class EntryStreamConcurrencyTest extends EntryStreamTestSupport {

    // debounce

    @Test
    public void testDebounce_coldStreamEmitsOnlyLastEntry() {
        java.util.Map<String, Integer> m = new java.util.LinkedHashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        m.put("c", 3);
        List<Entry<String, Integer>> result = EntryStream.of(m).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toList();
        assertEquals(1, result.size());
        assertEquals("c", result.get(0).getKey());
    }

    @Test
    public void testDebounce_emptyEmitsNothing() {
        assertTrue(EntryStream.of(new java.util.LinkedHashMap<String, Integer>()).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toList().isEmpty());
    }

    @Test
    public void testDebounce_invalidDurationThrows() {
        java.util.Map<String, Integer> m = new java.util.LinkedHashMap<>();
        m.put("a", 1);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> EntryStream.of(m).debounce((com.landawn.abacus.util.Duration) null).toList());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> EntryStream.of(m).debounce(com.landawn.abacus.util.Duration.ofMillis(0)).toList());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> EntryStream.of(m).debounce(com.landawn.abacus.util.Duration.ofMillis(-100)).toList());
    }

    @Test
    public void testDebounce_singleEntrySurvives() {
        java.util.Map<String, Integer> m = new java.util.LinkedHashMap<>();
        m.put("a", 1);
        List<Entry<String, Integer>> result = EntryStream.of(m).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toList();
        assertEquals(1, result.size());
        assertEquals("a", result.get(0).getKey());
    }

    @Test
    public void testDebounce_slowSourceAllEntriesSurviveWhenGapAtLeastDuration() {
        java.util.Map<String, Integer> m = new java.util.LinkedHashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        m.put("c", 3);
        List<Entry<String, Integer>> result = EntryStream.of(m)
                .delay(com.landawn.abacus.util.Duration.ofMillis(60))
                .debounce(com.landawn.abacus.util.Duration.ofMillis(20))
                .toList();
        List<String> keys = new java.util.ArrayList<>();
        for (Entry<String, Integer> e : result) {
            keys.add(e.getKey());
        }
        assertEquals(Arrays.asList("a", "b", "c"), keys);
    }

    // delay

    @Test
    public void testDelay() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2).delay(com.landawn.abacus.util.Duration.ofMillis(1)).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDelay_JavaDuration() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2).delay(Duration.ofMillis(1)).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDelayAndRateLimited() {
        long startTime = System.currentTimeMillis();
        List<Entry<String, Integer>> delayed = EntryStream.of("a", 1, "b", 2).delay(Duration.ofMillis(10)).toList();
        long duration = System.currentTimeMillis() - startTime;

        assertEquals(2, delayed.size());
        assertTrue(duration >= 10);

        RateLimiter rateLimiter = RateLimiter.create(100);
        List<Entry<String, Integer>> limited = EntryStream.of(testMap).rateLimited(rateLimiter).toList();

        assertEquals(5, limited.size());
    }

    // parallel

    @Test
    public void testParallel() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap).parallel();
        assertTrue(stream.isParallel());
        assertEquals(5, stream.count());
    }

    @Test
    public void testParallel_WithMaxThreadNum() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap).parallel(2);
        assertTrue(stream.isParallel());
        assertEquals(5, stream.count());
    }

    @Test
    public void testParallelOperations() {
        Map<String, Integer> largeMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            largeMap.put("key" + i, i);
        }

        EntryStream<String, Integer> parallel = EntryStream.of(largeMap).parallel();

        long count = parallel.filter((k, v) -> v % 2 == 0).count();
        assertEquals(500, count);

        EntryStream<String, Integer> parallel2 = EntryStream.of(largeMap).parallel();
        int sum = parallel2.entries().mapToInt(Map.Entry::getValue).sum();
        assertEquals(499500, sum);
    }

    @Test
    public void testParallelPrefixOperationsRetainLaterMatchingEntries() {
        final List<String> expectedKeys = Arrays.asList("c", "d", "e");
        final List<String> droppedKeys = new ArrayList<>();

        assertEquals(expectedKeys,
                EntryStream.of("a", 1, "b", 2, "c", 3, "d", 1, "e", 2)
                        .parallel(3)
                        .dropWhile(e -> e.getValue() < 3, e -> droppedKeys.add(e.getKey()))
                        .keys()
                        .sorted()
                        .toList());
        assertEquals(Arrays.asList("a", "b"), droppedKeys);

        assertEquals(expectedKeys, EntryStream.of("a", 1, "b", 2, "c", 3, "d", 1, "e", 2).parallel(3).dropWhile((k, v) -> v < 3).keys().sorted().toList());
        assertEquals(expectedKeys, EntryStream.of("a", 1, "b", 2, "c", 3, "d", 1, "e", 2).parallel(3).skipUntil((k, v) -> v >= 3).keys().sorted().toList());
    }

    @Test
    public void testParallelStreamOperations() {
        Map<String, Integer> result = EntryStream.of(testMap).parallel().filter(e -> e.getValue() > 2).mapValue(v -> v * 2).sequential().toMap();

        assertEquals(3, result.size());
        assertEquals(6, result.get("three"));
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(100);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).rateLimited(rateLimiter).toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testRateLimited_Double() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2).rateLimited(100.0).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testSequential() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap).parallel();
        assertTrue(stream.isParallel());
        stream = stream.sequential();
        assertFalse(stream.isParallel());
    }

    @Test
    public void testSps() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).sps(s -> s.filter(e -> e.getValue() > 2)).toList();
        assertEquals(3, result.size());
    }

}
