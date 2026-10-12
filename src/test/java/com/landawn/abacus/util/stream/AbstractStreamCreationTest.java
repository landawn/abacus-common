package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Pair;

/** AbstractStream creation contracts. */
public class AbstractStreamCreationTest extends AbstractStreamTestSupport {

    // flatGroupTo

    @Test
    public void testFlatGroupTo() {
        stream = createStream(1, 2, 3);
        Map<String, List<Integer>> result = stream.flatGroupTo(i -> Arrays.asList("a", "b"));
        assertEquals(Arrays.asList(1, 2, 3), result.get("a"));
        assertEquals(Arrays.asList(1, 2, 3), result.get("b"));
    }

    @Test
    public void testFlatGroupTo_WithDownstreamAndMapFactory() {
        // flatGroupTo(flatKeyExtractor, downstream, mapFactory) - uncovered overload
        java.util.TreeMap<String, Long> result = Stream.of(1, 2, 3, 4)
                .flatGroupTo(i -> java.util.Arrays.asList("a", "b"), Collectors.counting(), java.util.TreeMap::new);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(4L, result.get("a").longValue());
        assertEquals(4L, result.get("b").longValue());
    }

    @Test
    public void testFlatGroupTo_WithValueMapperAndDownstream() {
        // flatGroupTo(flatKeyExtractor, valueMapper, downstream)
        java.util.Map<String, Long> result = Stream.of(1, 2, 3).flatGroupTo(i -> java.util.Arrays.asList("odd", "all"), (k, v) -> v, Collectors.counting());
        assertNotNull(result);
        assertEquals(3L, result.get("all").longValue());
    }

    @Test
    public void testFlatGroupTo_WithValueMapperDownstreamAndMapFactory() {
        // flatGroupTo(flatKeyExtractor, valueMapper, downstream, mapFactory) - full overload
        java.util.TreeMap<String, Long> result = Stream.of(1, 2, 3)
                .flatGroupTo(i -> java.util.Arrays.asList("key" + i), (k, v) -> v, Collectors.counting(), java.util.TreeMap::new);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(1L, result.get("key1").longValue());
    }

    @Test
    public void testFlatGroupToRejectsNullDownstreamBeforeInvokingMapFactory() {
        for (final boolean empty : new boolean[] { true, false }) {
            final AtomicInteger factoryCalls = new AtomicInteger();
            try (final Stream<Integer> stream = empty ? Stream.empty() : Stream.of(1)) {
                assertThrows(IllegalArgumentException.class, () -> stream.flatGroupTo(value -> List.of(value), (key, value) -> value, null, () -> {
                    factoryCalls.incrementAndGet();
                    return new HashMap<>();
                }));
                assertEquals(0, factoryCalls.get());
            }
        }
    }

    @Test
    public void testFlatGroupToWithCollector() {
        stream = createStream(1, 2, 3);
        Map<String, Long> result = stream.flatGroupTo(i -> Arrays.asList("a", "b"), Collectors.counting());
        assertEquals(3L, result.get("a"));
        assertEquals(3L, result.get("b"));
    }

    @Test
    public void testFlatGroupToWithValueMapper() {
        stream = createStream(1, 2);
        Map<String, List<String>> result = stream.flatGroupTo(i -> Arrays.asList("a", "b"), (key, val) -> key + val);
        assertEquals(Arrays.asList("a1", "a2"), result.get("a"));
        assertEquals(Arrays.asList("b1", "b2"), result.get("b"));
    }

    @Test
    public void testFlattmap() {
        stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.flattMap(i -> java.util.stream.Stream.of(i, i * 10));
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
    }

    @Test
    public void test_flattmap_jdkStream() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4)).flattMap(Collection::stream).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlattMapToEntry() {
        stream = createStream(1, 2);
        EntryStream<Integer, String> result = stream.flattMapToEntry(i -> EntryStream.of(i, "val" + i));
        Map<Integer, String> map = result.toMap();
        assertEquals("val1", map.get(1));
    }

    @Test
    public void test_flattMapToEntry_entryStreamMapper() {
        Map<Integer, String> map1 = Map.of(1, "a");
        Map<Integer, String> map2 = Map.of(2, "b");
        List<Map.Entry<Integer, String>> result = Stream.of(map1, map2)
                .flattMapToEntry(m -> Stream.of(m).mapToEntry(Fn.identity()))
                .map(e -> Pair.of(e.getKey(), e.getValue()))
                .toList();
        assertTrue(result.contains(Pair.of(1, "a")));
        assertTrue(result.contains(Pair.of(2, "b")));
    }

    @Test
    public void testFlattMap() {
        {
            stream = createStream(1, 2, 3);
            Stream<Integer> result = stream.flatMapArray(i -> new Integer[] { i, i * 10 });
            assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
        {
            stream = createStream(1, 2, 3);
            Stream<Integer> result = stream.parallel().flatMapArray(i -> new Integer[] { i, i * 10 });
            assertHaveSameElements(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
        {
            stream = createStream(1, 2, 3).map(e -> e);
            Stream<Integer> result = stream.flatMapArray(i -> new Integer[] { i, i * 10 });
            assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
        {
            stream = createStream(1, 2, 3).map(e -> e);
            Stream<Integer> result = stream.parallel().flatMapArray(i -> new Integer[] { i, i * 10 });
            assertHaveSameElements(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
    }

    @Test
    public void test_flattMap_array() {
        List<Integer> result = Stream.of("1,2", "3,4").flatMapArray(s -> s.split(",")).map(i -> Numbers.toInt(i)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void test_rangeMap() {
        List<String> result = Stream.of("a", "ab", "ac", "b", "c", "cb").rangeMap((a, b) -> b.startsWith(a), (a, b) -> a + "->" + b).toList();
        assertEquals(Arrays.asList("a->ac", "b->b", "c->cb"), result);
    }

    @Test
    public void testRangeMap() {
        stream = createStream(1, 2, 2, 3, 3, 3, 4);
        Stream<String> result = stream.rangeMap((left, right) -> left.equals(right), (left, right) -> left + "-" + right);
        assertEquals(Arrays.asList("1-1", "2-2", "3-3", "4-4"), result.toList());
    }

}
