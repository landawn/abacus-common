package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IndexedLong;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalLong;

@Tag("new-test")
public class AbstractLongStream100Test extends TestBase {

    private LongStream stream;

    @BeforeEach
    public void setUp() {
    }

    private LongStream createLongStream(long... elements) {
        return LongStream.of(elements);
    }

    @Test
    public void testRateLimited() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        RateLimiter rateLimiter = RateLimiter.create(2.0);

        long startTime = System.currentTimeMillis();
        long[] result = stream.rateLimited(rateLimiter).toArray();
        long duration = System.currentTimeMillis() - startTime;

        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, result);
        assertTrue(duration >= 1500);

        assertThrows(IllegalArgumentException.class, () -> {
            createLongStream(new long[] { 1 }).rateLimited(null);
        });
    }

    @Test
    public void testDelay() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        Duration delay = Duration.ofMillis(100);

        long startTime = System.currentTimeMillis();
        long[] result = stream.delay(delay).toArray();
        long duration = System.currentTimeMillis() - startTime;

        assertArrayEquals(new long[] { 1, 2, 3 }, result);
        assertTrue(duration < 300);

        assertThrows(IllegalArgumentException.class, () -> {
            createLongStream(new long[] { 1 }).delay((Duration) null);
        });
    }

    @Test
    public void testSkipUntil() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        long[] result = stream.skipUntil(x -> x > 3).toArray();
        assertArrayEquals(new long[] { 4, 5 }, result);

        stream = createLongStream(new long[] { 1, 2, 3 });
        result = stream.skipUntil(x -> x > 5).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testDistinct() {
        stream = createLongStream(new long[] { 1, 2, 2, 3, 3, 3, 4, 4, 5 });
        long[] result = stream.distinct().toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, result);

        stream = createLongStream(new long[] {});
        result = stream.distinct().toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testFlatmap() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        long[] result = stream.flatmap(x -> new long[] { x, x * 2 }).toArray();
        assertArrayEquals(new long[] { 1, 2, 2, 4, 3, 6 }, result);

        stream = createLongStream(new long[] {});
        result = stream.flatmap(x -> new long[] { x, x * 2 }).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testFlattmap() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        long[] result = stream.flattMap(x -> java.util.stream.LongStream.of(x, x * 2)).toArray();
        assertArrayEquals(new long[] { 1, 2, 2, 4, 3, 6 }, result);
    }

    @Test
    public void testFlatmapToObj() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        List<String> result = stream.flatmapToObj(x -> Arrays.asList(String.valueOf(x), String.valueOf(x * 2))).toList();
        assertEquals(Arrays.asList("1", "2", "2", "4", "3", "6"), result);
    }

    @Test
    public void testFlattMapToObj() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        List<String> result = stream.flattmapToObj(x -> new String[] { String.valueOf(x), String.valueOf(x * 2) }).toList();
        assertEquals(Arrays.asList("1", "2", "2", "4", "3", "6"), result);
    }

    @Test
    public void testMapMulti() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        long[] result = stream.mapMulti((value, consumer) -> {
            consumer.accept(value);
            consumer.accept(value * 2);
        }).toArray();
        assertArrayEquals(new long[] { 1, 2, 2, 4, 3, 6 }, result);
    }

    @Test
    public void testMapPartial() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        long[] result = stream.mapPartial(x -> x % 2 == 0 ? OptionalLong.of(x * 2) : OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 4, 8 }, result);
    }

    @Test
    public void testMapPartialJdk() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        long[] result = stream.mapPartialJdk(x -> x % 2 == 0 ? java.util.OptionalLong.of(x * 2) : java.util.OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 4, 8 }, result);
    }

    @Test
    public void testRangeMap() {
        stream = createLongStream(new long[] { 1, 2, 3, 10, 11, 20, 21 });
        long[] result = stream.rangeMap((a, b) -> b - a <= 1, (first, last) -> first + last).toArray();
        assertArrayEquals(new long[] { 3, 6, 21, 41 }, result);
    }

    @Test
    public void testRangeMapToObj() {
        stream = createLongStream(new long[] { 1, 2, 3, 10, 11, 20, 21 });
        List<String> result = stream.rangeMapToObj((a, b) -> b - a <= 1, (first, last) -> first + "-" + last).toList();
        assertEquals(Arrays.asList("1-2", "3-3", "10-11", "20-21"), result);
    }

    @Test
    public void testCollapse() {
        stream = createLongStream(new long[] { 1, 2, 5, 6, 7, 10 });
        List<LongList> result = stream.collapse((a, b) -> b - a <= 1).toList();
        assertEquals(3, result.size());
        assertArrayEquals(new long[] { 1, 2 }, result.get(0).toArray());
        assertArrayEquals(new long[] { 5, 6, 7 }, result.get(1).toArray());
        assertArrayEquals(new long[] { 10 }, result.get(2).toArray());
    }

    @Test
    public void testCollapseWithMergeFunction() {
        stream = createLongStream(new long[] { 1, 2, 5, 6, 7, 10 });
        long[] result = stream.collapse((a, b) -> b - a <= 1, Long::sum).toArray();
        assertArrayEquals(new long[] { 3, 18, 10 }, result);
    }

    @Test
    public void testCollapseWithTriPredicate() {
        stream = createLongStream(new long[] { 1, 2, 5, 6, 7, 10 });
        long[] result = stream.collapse((first, prev, curr) -> curr - prev <= 1, Long::sum).toArray();
        assertArrayEquals(new long[] { 3, 18, 10 }, result);
    }

    @Test
    public void testSkipWithAction() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        List<Long> skipped = new ArrayList<>();
        long[] result = stream.skip(2, skipped::add).toArray();
        assertArrayEquals(new long[] { 3, 4, 5 }, result);
        assertEquals(Arrays.asList(1L, 2L), skipped);

        assertThrows(IllegalArgumentException.class, () -> {
            createLongStream(new long[] { 1 }).skip(-1, x -> {
            });
        });

        assertThrows(IllegalArgumentException.class, () -> {
            createLongStream(new long[] { 1 }).skip(1, null);
        });
    }

    @Test
    public void testFilterWithActionOnDropped() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        List<Long> dropped = new ArrayList<>();
        long[] result = stream.filter(x -> x % 2 == 0, dropped::add).toArray();
        assertArrayEquals(new long[] { 2, 4 }, result);
        assertEquals(Arrays.asList(1L, 3L, 5L), dropped);
    }

    @Test
    public void testDropWhileWithActionOnDropped() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        List<Long> dropped = new ArrayList<>();
        long[] result = stream.dropWhile(x -> x < 3, dropped::add).toArray();
        assertArrayEquals(new long[] { 3, 4, 5 }, result);
        assertEquals(Arrays.asList(1L, 2L), dropped);
    }

    @Test
    public void testStep() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        long[] result = stream.step(3).toArray();
        assertArrayEquals(new long[] { 1, 4, 7, 10 }, result);

        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        result = stream.step(1).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, result);

        assertThrows(IllegalArgumentException.class, () -> {
            createLongStream(new long[] { 1 }).step(0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            createLongStream(new long[] { 1 }).step(-1);
        });
    }

    @Test
    public void testScan() {
        stream = createLongStream(new long[] { 1, 2, 3, 4 });
        long[] result = stream.scan(Long::sum).toArray();
        assertArrayEquals(new long[] { 1, 3, 6, 10 }, result);

        stream = createLongStream(new long[] {});
        result = stream.scan(Long::sum).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testScanWithInit() {
        stream = createLongStream(new long[] { 1, 2, 3, 4 });
        long[] result = stream.scan(10, Long::sum).toArray();
        assertArrayEquals(new long[] { 11, 13, 16, 20 }, result);
    }

    @Test
    public void testScanWithInitIncluded() {
        stream = createLongStream(new long[] { 1, 2, 3, 4 });
        long[] result = stream.scan(10, true, Long::sum).toArray();
        assertArrayEquals(new long[] { 10, 11, 13, 16, 20 }, result);

        stream = createLongStream(new long[] { 1, 2, 3, 4 });
        result = stream.scan(10, false, Long::sum).toArray();
        assertArrayEquals(new long[] { 11, 13, 16, 20 }, result);
    }

    @Test
    public void testTop() {
        stream = createLongStream(new long[] { 5, 2, 8, 1, 9, 3, 7 });
        long[] result = stream.top(3).toArray();
        assertArrayEquals(new long[] { 7, 9, 8 }, result);

        stream = createLongStream(new long[] { 1, 2, 3 });
        result = stream.top(5).toArray();
        assertArrayEquals(new long[] { 1, 2, 3 }, result);
    }

    @Test
    public void testIntersection() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        Collection<Long> c = Arrays.asList(2L, 4L, 6L);
        long[] result = stream.intersection(c).toArray();
        assertArrayEquals(new long[] { 2, 4 }, result);

        stream = createLongStream(new long[] { 1, 2, 2, 3, 4, 4, 5 });
        c = Arrays.asList(2L, 2L, 4L, 6L);
        result = stream.intersection(c).toArray();
        assertArrayEquals(new long[] { 2, 2, 4 }, result);
    }

    @Test
    public void testDifference() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        Collection<Long> c = Arrays.asList(2L, 4L, 6L);
        long[] result = stream.difference(c).toArray();
        assertArrayEquals(new long[] { 1, 3, 5 }, result);

        stream = createLongStream(new long[] { 1, 2, 2, 3, 4, 4, 5 });
        c = Arrays.asList(2L, 4L, 6L);
        result = stream.difference(c).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testSymmetricDifference() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        Collection<Long> c = Arrays.asList(2L, 4L, 6L, 8L);
        long[] result = stream.symmetricDifference(c).toArray();
        assertArrayEquals(new long[] { 1, 3, 5, 6, 8 }, result);
    }

    @Test
    public void testReversed() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        long[] result = stream.reversed().toArray();
        assertArrayEquals(new long[] { 5, 4, 3, 2, 1 }, result);

        stream = createLongStream(new long[] {});
        result = stream.reversed().toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testRotated() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        long[] result = stream.rotated(2).toArray();
        assertArrayEquals(new long[] { 4, 5, 1, 2, 3 }, result);

        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        result = stream.rotated(-2).toArray();
        assertArrayEquals(new long[] { 3, 4, 5, 1, 2 }, result);

        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        result = stream.rotated(0).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testShuffled() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        Random rnd = new Random(42);
        long[] result = stream.shuffled(rnd).toArray();
        assertEquals(5, result.length);
        Arrays.sort(result);
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, result);

        assertThrows(IllegalArgumentException.class, () -> {
            createLongStream(new long[] { 1 }).shuffled(null);
        });
    }

    @Test
    public void testSorted() {
        stream = createLongStream(new long[] { 5, 2, 8, 1, 9, 3, 7 });
        long[] result = stream.sorted().toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 5, 7, 8, 9 }, result);

        stream = createLongStream(new long[] {});
        result = stream.sorted().toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testReverseSorted() {
        stream = createLongStream(new long[] { 5, 2, 8, 1, 9, 3, 7 });
        long[] result = stream.reverseSorted().toArray();
        assertArrayEquals(new long[] { 9, 8, 7, 5, 3, 2, 1 }, result);
    }

    @Test
    public void testCycled() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        long[] result = stream.cycled().limit(10).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, result);

        stream = createLongStream(new long[] {});
        result = stream.cycled().limit(5).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testCycledWithRounds() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        long[] result = stream.cycled(2).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 1, 2, 3 }, result);

        stream = createLongStream(new long[] { 1, 2, 3 });
        result = stream.cycled(0).toArray();
        assertArrayEquals(new long[] {}, result);

        stream = createLongStream(new long[] { 1, 2, 3 });
        result = stream.cycled(1).toArray();
        assertArrayEquals(new long[] { 1, 2, 3 }, result);

        assertThrows(IllegalArgumentException.class, () -> {
            createLongStream(new long[] { 1 }).cycled(-1);
        });
    }

    @Test
    public void testIndexed() {
        stream = createLongStream(new long[] { 10, 20, 30 });
        List<IndexedLong> result = stream.indexed().toList();
        assertEquals(3, result.size());
        assertEquals(10, result.get(0).value());
        assertEquals(0, result.get(0).index());
        assertEquals(20, result.get(1).value());
        assertEquals(1, result.get(1).index());
        assertEquals(30, result.get(2).value());
        assertEquals(2, result.get(2).index());
    }

    @Test
    public void testBoxed() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        List<Long> result = stream.boxed().toList();
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testPrependArray() {
        stream = createLongStream(new long[] { 3, 4, 5 });
        long[] result = stream.prepend(1, 2).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, result);

        stream = createLongStream(new long[] {});
        result = stream.prepend(1, 2).toArray();
        assertArrayEquals(new long[] { 1, 2 }, result);
    }

    @Test
    public void testPrependStream() {
        stream = createLongStream(new long[] { 3, 4, 5 });
        LongStream toPreppend = createLongStream(new long[] { 1, 2 });
        long[] result = stream.prepend(toPreppend).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testPrependOptional() {
        stream = createLongStream(new long[] { 2, 3 });
        long[] result = stream.prepend(OptionalLong.of(1)).toArray();
        assertArrayEquals(new long[] { 1, 2, 3 }, result);

        stream = createLongStream(new long[] { 2, 3 });
        result = stream.prepend(OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 2, 3 }, result);
    }

    @Test
    public void testAppendArray() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        long[] result = stream.append(4, 5).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, result);

        stream = createLongStream(new long[] {});
        result = stream.append(1, 2).toArray();
        assertArrayEquals(new long[] { 1, 2 }, result);
    }

    @Test
    public void testAppendStream() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        LongStream toAppend = createLongStream(new long[] { 4, 5 });
        long[] result = stream.append(toAppend).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testAppendOptional() {
        stream = createLongStream(new long[] { 1, 2 });
        long[] result = stream.append(OptionalLong.of(3)).toArray();
        assertArrayEquals(new long[] { 1, 2, 3 }, result);

        stream = createLongStream(new long[] { 1, 2 });
        result = stream.append(OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 1, 2 }, result);
    }

    @Test
    public void testAppendIfEmpty() {
        stream = createLongStream(new long[] {});
        long[] result = stream.appendIfEmpty(1, 2, 3).toArray();
        assertArrayEquals(new long[] { 1, 2, 3 }, result);

        stream = createLongStream(new long[] { 4, 5 });
        result = stream.appendIfEmpty(1, 2, 3).toArray();
        assertArrayEquals(new long[] { 4, 5 }, result);
    }

    @Test
    public void testMergeWith() {
        stream = createLongStream(new long[] { 1, 3, 5 });
        LongStream other = createLongStream(new long[] { 2, 4, 6 });
        long[] result = stream.mergeWith(other, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testZipWith() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        LongStream other = createLongStream(new long[] { 4, 5, 6 });
        long[] result = stream.zipWith(other, Long::sum).toArray();
        assertArrayEquals(new long[] { 5, 7, 9 }, result);

        stream = createLongStream(new long[] { 1, 2, 3 });
        other = createLongStream(new long[] { 4, 5 });
        result = stream.zipWith(other, Long::sum).toArray();
        assertArrayEquals(new long[] { 5, 7 }, result);
    }

    @Test
    public void testZipWithThreeStreams() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        LongStream second = createLongStream(new long[] { 4, 5, 6 });
        LongStream third = createLongStream(new long[] { 7, 8, 9 });
        long[] result = stream.zipWith(second, third, (a, b, c) -> a + b + c).toArray();
        assertArrayEquals(new long[] { 12, 15, 18 }, result);
    }

    @Test
    public void testZipWithDefaultValues() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        LongStream other = createLongStream(new long[] { 4, 5 });
        long[] result = stream.zipWith(other, 10, 20, Long::sum).toArray();
        assertArrayEquals(new long[] { 5, 7, 23 }, result);
    }

    @Test
    public void testZipWithThreeStreamsAndDefaultValues() {
        stream = createLongStream(new long[] { 1, 2 });
        LongStream second = createLongStream(new long[] { 3 });
        LongStream third = createLongStream(new long[] { 5, 6, 7 });
        long[] result = stream.zipWith(second, third, 10, 20, 30, (a, b, c) -> a + b + c).toArray();
        assertArrayEquals(new long[] { 9, 28, 37 }, result);
    }

    @Test
    public void testToMap() throws Exception {
        stream = createLongStream(new long[] { 1, 2, 3 });
        Map<String, Long> result = stream.toMap(x -> "key" + x, x -> x * 2);
        assertEquals(3, result.size());
        assertEquals(2L, result.get("key1"));
        assertEquals(4L, result.get("key2"));
        assertEquals(6L, result.get("key3"));
    }

    @Test
    public void testToMapWithSupplier() throws Exception {
        stream = createLongStream(new long[] { 1, 2, 3 });
        Map<String, Long> result = stream.toMap(x -> "key" + x, x -> x * 2, Suppliers.ofLinkedHashMap());
        assertEquals(3, result.size());
        assertEquals(2L, result.get("key1"));
        assertEquals(4L, result.get("key2"));
        assertEquals(6L, result.get("key3"));
    }

    @Test
    public void testToMapWithMergeFunction() throws Exception {
        stream = createLongStream(new long[] { 1, 2, 3, 1, 2 });
        Map<Long, Long> result = stream.toMap(x -> x, x -> x * 2, Long::sum);
        assertEquals(3, result.size());
        assertEquals(4L, result.get(1L));
        assertEquals(8L, result.get(2L));
        assertEquals(6L, result.get(3L));
    }

    @Test
    public void testToMapWithMergeFunctionAndSupplier() throws Exception {
        stream = createLongStream(new long[] { 1, 2, 3, 1, 2 });
        LinkedHashMap<Long, Long> result = stream.toMap(x -> x, x -> x * 2, Long::sum, LinkedHashMap::new);
        assertEquals(3, result.size());
        assertEquals(4L, result.get(1L));
        assertEquals(8L, result.get(2L));
        assertEquals(6L, result.get(3L));
    }

    @Test
    public void testGroupTo() throws Exception {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5, 6 });
        Map<String, List<Long>> result = stream.groupTo(x -> x % 2 == 0 ? "even" : "odd", Collectors.toList());
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1L, 3L, 5L), result.get("odd"));
        assertEquals(Arrays.asList(2L, 4L, 6L), result.get("even"));
    }

    @Test
    public void testGroupToWithSupplier() throws Exception {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5, 6 });
        LinkedHashMap<String, List<Long>> result = stream.groupTo(x -> x % 2 == 0 ? "even" : "odd", Collectors.toList(), LinkedHashMap::new);
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1L, 3L, 5L), result.get("odd"));
        assertEquals(Arrays.asList(2L, 4L, 6L), result.get("even"));
    }

    @Test
    public void testCollectWithSupplierAccumulator() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        LongList result = stream.collect(LongList::new, LongList::add);
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, result.toArray());

        stream = createLongStream(new long[] { 1, 2, 3 });
        StringBuilder sb = stream.collect(StringBuilder::new, (builder, value) -> builder.append(value).append(","));
        assertEquals("1,2,3,", sb.toString());

        Object ret = new Object();

        assertEquals(ret, createLongStream(new long[] { 1 }).collect(() -> ret, (obj, value) -> {
        }));
    }

    @Test
    public void testCollectWithSupplierAccumulatorCombiner() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });

        LongList result = stream.collect(Suppliers.ofLongList(), (c, e) -> c.add(e), (LongList a, LongList b) -> {
            a.addAll(b);
        });
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testForEachIndexed() throws Exception {
        stream = createLongStream(new long[] { 10, 20, 30 });
        List<String> result = new ArrayList<>();
        stream.forEachIndexed((idx, value) -> result.add(idx + ":" + value));
        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), result);
    }

    @Test
    public void testFirst() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        OptionalLong result = stream.first();
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsLong());

        stream = createLongStream(new long[] {});
        result = stream.first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        OptionalLong result = stream.last();
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsLong());

        stream = createLongStream(new long[] {});
        result = stream.last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne() {
        stream = createLongStream(new long[] { 42 });
        OptionalLong result = stream.onlyOne();
        assertTrue(result.isPresent());
        assertEquals(42, result.getAsLong());

        stream = createLongStream(new long[] {});
        result = stream.onlyOne();
        assertFalse(result.isPresent());

        stream = createLongStream(new long[] { 1, 2 });
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testFindAny() throws Exception {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5 });
        OptionalLong result = stream.findAny(x -> x > 3);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsLong());

        stream = createLongStream(new long[] { 1, 2, 3 });
        result = stream.findAny(x -> x > 5);
        assertFalse(result.isPresent());
    }

    @Test
    public void testPercentiles() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        Optional<Map<Percentage, Long>> result = stream.percentiles();
        assertTrue(result.isPresent());
        Map<Percentage, Long> percentiles = result.get();
        assertNotNull(percentiles);
        assertTrue(percentiles.containsKey(Percentage._50));

        stream = createLongStream(new long[] {});
        result = stream.percentiles();
        assertFalse(result.isPresent());
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        stream = createLongStream(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        Pair<LongSummaryStatistics, Optional<Map<Percentage, Long>>> result = stream.summaryStatisticsAndPercentiles();

        LongSummaryStatistics stats = result.left();
        assertEquals(10, stats.getCount());
        assertEquals(1, stats.getMin());
        assertEquals(10, stats.getMax());
        assertEquals(55, stats.getSum());

        assertTrue(result.right().isPresent());
        Map<Percentage, Long> percentiles = result.right().get();
        assertNotNull(percentiles);
        assertTrue(percentiles.containsKey(Percentage._50));

        stream = createLongStream(new long[] {});
        result = stream.summaryStatisticsAndPercentiles();
        assertEquals(0, result.left().getCount());
        assertFalse(result.right().isPresent());
    }

    @Test
    public void testJoin() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        String result = stream.join(", ", "[", "]");
        assertEquals("[1, 2, 3]", result);

        stream = createLongStream(new long[] {});
        result = stream.join(", ", "[", "]");
        assertEquals("[]", result);
    }

    @Test
    public void testJoinTo() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        Joiner joiner = Joiner.with(", ", "[", "]");
        Joiner result = stream.joinTo(joiner);
        assertEquals("[1, 2, 3]", result.toString());
    }

    @Test
    public void testIterator() {
        stream = createLongStream(new long[] { 1, 2, 3 });
        LongIterator iter = stream.iterator();

        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(3, iter.nextLong());
        assertFalse(iter.hasNext());
    }
}
