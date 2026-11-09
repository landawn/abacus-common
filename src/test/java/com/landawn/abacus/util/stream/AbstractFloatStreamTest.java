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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.IndexedFloat;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;

public class AbstractFloatStreamTest extends TestBase {

    @Test
    public void testScanInitializesOnlyAfterSuccessfulSourceRead() {
        final IllegalStateException failure = new IllegalStateException("first read failed");
        final java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger accumulatorCalls = new java.util.concurrent.atomic.AtomicInteger();

        try (FloatStream stream = FloatStream.of(new float[] { 1, 2, 3 }).map(value -> {
            if (attempts.getAndIncrement() == 0) {
                throw failure;
            }
            return value;
        }).scan((left, right) -> {
            accumulatorCalls.incrementAndGet();
            return left + right;
        })) {
            final com.landawn.abacus.util.FloatIterator iter = stream.iterator();
            org.junit.jupiter.api.Assertions.assertSame(failure, org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, iter::nextFloat));
            org.junit.jupiter.api.Assertions.assertEquals(2, iter.nextFloat());
            org.junit.jupiter.api.Assertions.assertEquals(0, accumulatorCalls.get());
            org.junit.jupiter.api.Assertions.assertEquals(5, iter.nextFloat());
            org.junit.jupiter.api.Assertions.assertEquals(1, accumulatorCalls.get());
            org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
        }
    }

    private FloatStream stream;
    private FloatStream stream2;

    @BeforeEach
    public void setUp() {
        stream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f });
        stream2 = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f });
    }

    protected FloatStream createFloatStream(float... elements) {
        return FloatStream.of(elements).map(e -> e + 0);
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(10);
        FloatStream rateLimitedStream = stream.rateLimited(rateLimiter);

        float[] result = rateLimitedStream.toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result);
    }

    @Test
    public void testDelay() {
        Duration delay = Duration.ofMillis(10);
        FloatStream delayedStream = stream.delay(delay);

        long startTime = System.currentTimeMillis();
        float[] result = delayedStream.toArray();
        long endTime = System.currentTimeMillis();

        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result);
        assertTrue(endTime - startTime >= 40);
    }

    @Test
    public void testSkipUntil() {
        FloatStream result = stream.skipUntil(x -> x > 3.0f);
        assertArrayEquals(new float[] { 4.0f, 5.0f }, result.toArray());
    }

    @Test
    public void testSkipUntil_NoMatch() {
        FloatStream result = stream.skipUntil(x -> x > 10.0f);
        assertArrayEquals(new float[] {}, result.toArray());
    }

    @Test
    public void testDistinct() {
        FloatStream streamWithDuplicates = createFloatStream(new float[] { 1.0f, 2.0f, 2.0f, 3.0f, 3.0f, 3.0f });
        float[] result = streamWithDuplicates.distinct().toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
    }

    @Test
    public void testFlatmap() {
        FloatStream result = stream.flatMapArray(x -> new float[] { x, x * 2 });
        assertArrayEquals(new float[] { 1.0f, 2.0f, 2.0f, 4.0f, 3.0f, 6.0f, 4.0f, 8.0f, 5.0f, 10.0f }, result.toArray());
    }

    @Test
    public void testFlatmapToObj() {
        Stream<String> result = stream.flatmapToObj(x -> Arrays.asList(String.valueOf(x), String.valueOf(x * 2)));
        List<String> list = result.toList();
        assertEquals(Arrays.asList("1.0", "2.0", "2.0", "4.0", "3.0", "6.0", "4.0", "8.0", "5.0", "10.0"), list);
    }

    @Test
    public void testFlattMapToObj() {
        Stream<String> result = stream.flatMapArrayToObj(x -> new String[] { String.valueOf(x), String.valueOf(x * 2) });
        List<String> list = result.toList();
        assertEquals(Arrays.asList("1.0", "2.0", "2.0", "4.0", "3.0", "6.0", "4.0", "8.0", "5.0", "10.0"), list);
    }

    @Test
    public void testMapPartial() {
        FloatStream result = stream.mapPartial(x -> x > 2.0f ? OptionalFloat.of(x * 2) : OptionalFloat.empty());
        assertArrayEquals(new float[] { 6.0f, 8.0f, 10.0f }, result.toArray());
    }

    @Test
    public void testRangeMap() {
        FloatStream result = stream.rangeMap((a, b) -> Math.abs(a - b) <= 1.0f, (a, b) -> a + b);
        assertArrayEquals(new float[] { 3.0f, 7.0f, 10.0f }, result.toArray());
    }

    @Test
    public void testRangeMapToObj() {
        Stream<String> result = stream.rangeMapToObj((a, b) -> Math.abs(a - b) <= 1.0f, (a, b) -> a + "-" + b);
        assertEquals(Arrays.asList("1.0-2.0", "3.0-4.0", "5.0-5.0"), result.toList());
    }

    @Test
    public void testCollapse() {
        Stream<FloatList> result = stream.collapse((a, b) -> Math.abs(a - b) <= 1.0f);
        List<FloatList> lists = result.toList();
        assertEquals(1, lists.size());
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, lists.get(0).toArray());
    }

    @Test
    public void testCollapse_WithMergeFunction() {
        FloatStream result = stream.collapse((a, b) -> Math.abs(a - b) <= 1.0f, (a, b) -> a + b);
        assertArrayEquals(new float[] { 15.0f }, result.toArray());
    }

    @Test
    public void testCollapse_WithTriPredicate() {
        FloatStream result = stream.collapse((first, prev, curr) -> curr - first <= 2.0f, (a, b) -> a + b);
        assertArrayEquals(new float[] { 6.0f, 9.0f }, result.toArray());
    }

    @Test
    public void testSkipWithAction() {
        List<Float> skipped = new ArrayList<>();
        FloatStream result = stream.skip(2, skipped::add);
        assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f }, result.toArray());
        assertEquals(Arrays.asList(1.0f, 2.0f), skipped);
    }

    @Test
    public void testFilterWithAction() {
        List<Float> dropped = new ArrayList<>();
        FloatStream result = stream.filter(x -> x > 2.0f, dropped::add);
        assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f }, result.toArray());
        assertEquals(Arrays.asList(1.0f, 2.0f), dropped);
    }

    @Test
    public void testDropWhileWithAction() {
        List<Float> dropped = new ArrayList<>();
        FloatStream result = stream.dropWhile(x -> x < 3.0f, dropped::add);
        assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f }, result.toArray());
        assertEquals(Arrays.asList(1.0f, 2.0f), dropped);
    }

    @Test
    public void testStep() {
        FloatStream result = stream.step(2);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 5.0f }, result.toArray());
    }

    @Test
    public void testArrayStepIteratorCountConsumes() {
        FloatIterator iter = FloatStream.of(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }).step(2).iterator();
        assertEquals(3, iter.count());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testScan() {
        FloatStream result = stream.scan((a, b) -> a + b);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 6.0f, 10.0f, 15.0f }, result.toArray());
    }

    @Test
    public void testScan_WithInit() {
        FloatStream result = stream.scan(10.0f, (a, b) -> a + b);
        assertArrayEquals(new float[] { 11.0f, 13.0f, 16.0f, 20.0f, 25.0f }, result.toArray());
    }

    @Test
    public void testScan_WithInitIncluded() {
        FloatStream result = stream.scan(10.0f, true, (a, b) -> a + b);
        assertArrayEquals(new float[] { 10.0f, 11.0f, 13.0f, 16.0f, 20.0f, 25.0f }, result.toArray());
    }

    @Test
    public void testTop() {
        FloatStream unsorted = createFloatStream(new float[] { 5.0f, 2.0f, 8.0f, 1.0f, 9.0f });
        float[] result = unsorted.top(3).toArray();
        Arrays.sort(result);
        assertArrayEquals(new float[] { 5.0f, 8.0f, 9.0f }, result);
    }

    @Test
    public void testIntersection() {
        Collection<Float> c = Arrays.asList(2.0f, 3.0f, 6.0f);
        FloatStream result = stream.intersection(c);
        assertArrayEquals(new float[] { 2.0f, 3.0f }, result.toArray());
    }

    @Test
    public void testDifference() {
        Collection<Float> c = Arrays.asList(2.0f, 3.0f, 6.0f);
        FloatStream result = stream.difference(c);
        assertArrayEquals(new float[] { 1.0f, 4.0f, 5.0f }, result.toArray());
    }

    @Test
    public void testSymmetricDifference() {
        Collection<Float> c = Arrays.asList(2.0f, 3.0f, 6.0f);
        FloatStream result = stream.symmetricDifference(c);
        float[] arr = result.toArray();
        Arrays.sort(arr);
        assertArrayEquals(new float[] { 1.0f, 4.0f, 5.0f, 6.0f }, arr);
    }

    @Test
    public void testReversed() {
        FloatStream result = stream.reversed();
        assertArrayEquals(new float[] { 5.0f, 4.0f, 3.0f, 2.0f, 1.0f }, result.toArray());
    }

    @Test
    public void testRotated() {
        FloatStream result = stream.rotated(2);
        assertArrayEquals(new float[] { 4.0f, 5.0f, 1.0f, 2.0f, 3.0f }, result.toArray());
    }

    @Test
    public void testRotated_Negative() {
        FloatStream result = stream.rotated(-2);
        assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f, 1.0f, 2.0f }, result.toArray());
    }

    @Test
    public void testShuffled() {
        Random rnd = new Random(42);
        FloatStream result = stream.shuffled(rnd);
        float[] arr = result.toArray();
        assertEquals(5, arr.length);
        Arrays.sort(arr);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, arr);
    }

    @Test
    public void testSorted() {
        FloatStream unsorted = createFloatStream(new float[] { 5.0f, 2.0f, 8.0f, 1.0f, 9.0f });
        float[] result = unsorted.sorted().toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 5.0f, 8.0f, 9.0f }, result);
    }

    @Test
    public void testReverseSorted() {
        FloatStream unsorted = createFloatStream(new float[] { 5.0f, 2.0f, 8.0f, 1.0f, 9.0f });
        float[] result = unsorted.reverseSorted().toArray();
        assertArrayEquals(new float[] { 9.0f, 8.0f, 5.0f, 2.0f, 1.0f }, result);
    }

    @Test
    public void testCycled() {
        FloatStream result = stream.cycled().limit(12);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 1.0f, 2.0f }, result.toArray());
    }

    @Test
    public void testCycled_WithRounds() {
        FloatStream result = stream.cycled(2);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result.toArray());
    }

    @Test
    public void testCycled_long() {
        stream = FloatStream.of(1.0f, 2.0f);
        List<Float> result = stream.cycled(3).collect(ArrayList::new, (list, value) -> list.add(value));
        assertEquals(Arrays.asList(1.0f, 2.0f, 1.0f, 2.0f, 1.0f, 2.0f), result);
    }

    @Test
    public void testCycled_ZeroRounds() {
        FloatStream result = stream.cycled(0);
        assertArrayEquals(new float[] {}, result.toArray());
    }

    @Test
    public void testIndexed() {
        Stream<IndexedFloat> result = stream.indexed();
        List<IndexedFloat> list = result.toList();
        assertEquals(5, list.size());
        assertEquals(1.0f, list.get(0).value());
        assertEquals(0, list.get(0).index());
        assertEquals(5.0f, list.get(4).value());
        assertEquals(4, list.get(4).index());
    }

    @Test
    public void testBoxed() {
        Stream<Float> result = stream.boxed();
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f, 4.0f, 5.0f), result.toList());
    }

    @Test
    public void testPrependAppend_Stream_Parallel() {
        float[] result = createFloatStream(3.0f, 4.0f).parallel().prepend(FloatStream.of(1.0f, 2.0f)).append(FloatStream.of(5.0f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result);
    }

    @Test
    public void testPrepend_Array() {
        FloatStream result = stream.prepend(0.0f, -1.0f);
        assertArrayEquals(new float[] { 0.0f, -1.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result.toArray());
    }

    @Test
    public void testPrepend_Stream() {
        FloatStream toAdd = createFloatStream(new float[] { 0.0f, -1.0f });
        FloatStream result = stream.prepend(toAdd);
        assertArrayEquals(new float[] { 0.0f, -1.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result.toArray());
    }

    @Test
    public void testPrepend_Optional() {
        FloatStream result1 = stream.prepend(OptionalFloat.of(0.0f));
        assertArrayEquals(new float[] { 0.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result1.toArray());

        FloatStream stream2 = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f });
        FloatStream result2 = stream2.prepend(OptionalFloat.empty());
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result2.toArray());
    }

    @Test
    public void testAppend_Array() {
        FloatStream result = stream.append(6.0f, 7.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f }, result.toArray());
    }

    @Test
    public void testAppend_Stream() {
        FloatStream toAdd = createFloatStream(new float[] { 6.0f, 7.0f });
        FloatStream result = stream.append(toAdd);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f }, result.toArray());
    }

    @Test
    public void testAppend_Optional() {
        FloatStream result1 = stream.append(OptionalFloat.of(6.0f));
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result1.toArray());

        FloatStream stream2 = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f });
        FloatStream result2 = stream2.append(OptionalFloat.empty());
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result2.toArray());
    }

    @Test
    public void testAppendIfEmpty() {
        FloatStream result = stream.appendIfEmpty(10.0f, 20.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result.toArray());

        FloatStream empty = createFloatStream(new float[] {});
        FloatStream result2 = empty.appendIfEmpty(10.0f, 20.0f);
        assertArrayEquals(new float[] { 10.0f, 20.0f }, result2.toArray());
    }

    @Test
    public void testMergeWith() {
        FloatStream other = createFloatStream(new float[] { 1.5f, 2.5f, 3.5f });
        FloatStream result = stream.mergeWith(other, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new float[] { 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 5.0f }, result.toArray());
    }

    @Test
    public void testZipWith_BinaryOperator() {
        FloatStream other = createFloatStream(new float[] { 10.0f, 20.0f, 30.0f });
        FloatStream result = stream.zipWith(other, (a, b) -> a + b);
        assertArrayEquals(new float[] { 11.0f, 22.0f, 33.0f }, result.toArray());
    }

    @Test
    public void testZipWith_TernaryOperator() {
        FloatStream b = createFloatStream(new float[] { 10.0f, 20.0f, 30.0f });
        FloatStream c = createFloatStream(new float[] { 100.0f, 200.0f, 300.0f });
        FloatStream result = stream.zipWith(b, c, (x, y, z) -> x + y + z);
        assertArrayEquals(new float[] { 111.0f, 222.0f, 333.0f }, result.toArray());
    }

    @Test
    public void testZipWith_WithDefaults() {
        FloatStream other = createFloatStream(new float[] { 10.0f, 20.0f });
        FloatStream result = stream.zipWith(other, 0.0f, 0.0f, (a, b) -> a + b);
        assertArrayEquals(new float[] { 11.0f, 22.0f, 3.0f, 4.0f, 5.0f }, result.toArray());
    }

    @Test
    public void testZipWith_TernaryWithDefaults() {
        FloatStream b = createFloatStream(new float[] { 10.0f, 20.0f });
        FloatStream c = createFloatStream(new float[] { 100.0f });
        FloatStream result = stream.zipWith(b, c, 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z);
        assertArrayEquals(new float[] { 111.0f, 22.0f, 3.0f, 4.0f, 5.0f }, result.toArray());
    }

    @Test
    public void testToMap() {
        Map<String, Float> result = stream.toMap(x -> "key" + x, x -> x * 2);
        assertEquals(5, result.size());
        assertEquals(2.0f, result.get("key1.0"));
        assertEquals(10.0f, result.get("key5.0"));
    }

    @Test
    public void testToMap_WithMapFactory() {
        Map<String, Float> result = stream.toMap(x -> "key" + x, x -> x * 2, Suppliers.ofLinkedHashMap());
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(5, result.size());
    }

    @Test
    public void testToMap_WithMergeFunction() {
        FloatStream duplicates = createFloatStream(new float[] { 1.0f, 1.0f, 2.0f, 2.0f });
        Map<String, Float> result = duplicates.toMap(x -> "key" + (int) x, x -> x, (a, b) -> a + b);
        assertEquals(2, result.size());
        assertEquals(2.0f, result.get("key1"));
        assertEquals(4.0f, result.get("key2"));
    }

    @Test
    public void testGroupTo() {
        Map<Integer, List<Float>> result = stream.groupTo(x -> (int) x % 2, Collectors.toList());
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(2.0f, 4.0f), result.get(0));
        assertEquals(Arrays.asList(1.0f, 3.0f, 5.0f), result.get(1));
    }

    @Test
    public void testForEachIndexed() {
        List<String> result = new ArrayList<>();
        stream.forEachIndexed((index, value) -> result.add(index + ":" + value));
        assertEquals(Arrays.asList("0:1.0", "1:2.0", "2:3.0", "3:4.0", "4:5.0"), result);
    }

    @Test
    public void testSum() {
        assertEquals(15.0, stream.sum(), 0.001);
    }

    @Test
    public void testSum_Empty() {
        FloatStream empty = createFloatStream(new float[] {});
        assertEquals(0.0, empty.sum(), 0.001);
    }

    @Test
    public void testAverage() {
        OptionalDouble result = stream.average();
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverage_Empty() {
        FloatStream empty = createFloatStream(new float[] {});
        OptionalDouble result = empty.average();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFirst() {
        OptionalFloat result = stream.first();
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.getAsFloat());
    }

    @Test
    public void testFirst_Empty() {
        FloatStream empty = createFloatStream(new float[] {});
        OptionalFloat result = empty.first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFirst_MultipleElements() {
        stream = FloatStream.of(1.0f, 2.0f, 3.0f);
        OptionalFloat result = stream.first();
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.getAsFloat());
    }

    @Test
    public void testLast() {
        OptionalFloat result = stream.last();
        assertTrue(result.isPresent());
        assertEquals(5.0f, result.getAsFloat());
    }

    @Test
    public void testLast_Empty() {
        FloatStream empty = createFloatStream(new float[] {});
        OptionalFloat result = empty.last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne() {
        FloatStream single = createFloatStream(new float[] { 42.0f });
        OptionalFloat result = single.onlyOne();
        assertTrue(result.isPresent());
        assertEquals(42.0f, result.getAsFloat());
    }

    @Test
    public void testOnlyOne_Empty() {
        FloatStream empty = createFloatStream(new float[] {});
        OptionalFloat result = empty.onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_Multiple() {
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testFindAny() {
        OptionalFloat result = stream.findAny(x -> x > 3.0f);
        assertTrue(result.isPresent());
        assertEquals(4.0f, result.getAsFloat());
    }

    @Test
    public void testFindAny_NoMatch() {
        OptionalFloat result = stream.findAny(x -> x > 10.0f);
        assertFalse(result.isPresent());
    }

    @Test
    public void testPercentiles() {
        Optional<Map<Percentage, Float>> result = stream.percentiles();
        assertTrue(result.isPresent());
        Map<Percentage, Float> percentiles = result.get();
        assertNotNull(percentiles);
        assertTrue(percentiles.containsKey(Percentage._50));
    }

    @Test
    public void testPercentiles_Empty() {
        FloatStream empty = createFloatStream(new float[] {});
        Optional<Map<Percentage, Float>> result = empty.percentiles();
        assertFalse(result.isPresent());
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        Pair<FloatSummaryStatistics, Optional<Map<Percentage, Float>>> result = stream.summaryStatisticsAndPercentiles();

        FloatSummaryStatistics stats = result.left();
        assertEquals(5, stats.getCount());
        assertEquals(1.0f, stats.getMin());
        assertEquals(5.0f, stats.getMax());
        assertEquals(15.0, stats.getSum(), 0.001);

        assertTrue(result.right().isPresent());
    }

    @Test
    public void testJoin() {
        String result = stream.join(", ", "[", "]");
        assertEquals("[1.0, 2.0, 3.0, 4.0, 5.0]", result);
    }

    @Test
    public void testJoinTo() {
        Joiner joiner = Joiner.with(", ");
        stream.joinTo(joiner);
        assertEquals("1.0, 2.0, 3.0, 4.0, 5.0", joiner.toString());
    }

    @Test
    public void testJoinToRejectsNullForEmptyStream() {
        assertThrows(IllegalArgumentException.class, () -> createFloatStream().joinTo(null));
    }

    @Test
    public void testCollect() {
        List<Float> result = stream.collect(ArrayList::new, (list, val) -> list.add(val));
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f, 4.0f, 5.0f), result);
    }

    @Test
    public void testIterator() {
        FloatIterator iter = stream.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1.0f, iter.nextFloat());
        assertTrue(iter.hasNext());
        assertEquals(2.0f, iter.nextFloat());
    }

    @Test
    public void testSummaryStatisticsAndPercentiles_NaNPropagation() {
        // FloatSummaryStatistics.accept uses Math.min/Math.max so any NaN propagates
        // into both min and max. summaryStatisticsAndPercentiles() must give the same
        // result so the two APIs do not diverge for the same data.
        Pair<FloatSummaryStatistics, Optional<Map<Percentage, Float>>> result = createFloatStream(new float[] { 1.0f, 2.0f, Float.NaN, 3.0f })
                .summaryStatisticsAndPercentiles();
        FloatSummaryStatistics stats = result.left();
        assertEquals(4, stats.getCount());
        assertTrue(Float.isNaN(stats.getMin()), "min should be NaN when input contains NaN");
        assertTrue(Float.isNaN(stats.getMax()), "max should be NaN when input contains NaN");
    }

    @Test
    public void testReversedRotatedReverseSorted_ToArrayExhaustsIterator() {
        FloatIteratorEx reversed = createFloatStream(new float[] { 1, 2, 3, 4, 5 }).reversed().iteratorEx();
        assertEquals(5, reversed.toArray().length);
        assertFalse(reversed.hasNext());
        assertEquals(0, reversed.toArray().length);

        FloatIteratorEx rotated = createFloatStream(new float[] { 1, 2, 3, 4, 5 }).rotated(2).iteratorEx();
        rotated.nextFloat();
        assertEquals(4, rotated.toArray().length);
        assertFalse(rotated.hasNext());
        assertEquals(0, rotated.toArray().length);

        FloatIteratorEx reverseSorted = createFloatStream(new float[] { 3, 1, 4, 1, 5 }).reverseSorted().iteratorEx();
        reverseSorted.nextFloat();
        reverseSorted.nextFloat();
        assertEquals(3, reverseSorted.toArray().length);
        assertFalse(reverseSorted.hasNext());
        assertEquals(0, reverseSorted.toArray().length);
    }

    @Test
    public void testDebounce_emptyEmitsNothing() {
        org.junit.jupiter.api.Assertions.assertEquals(0,
                createFloatStream(new float[] {}).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray().length);
    }

    @Test
    public void testDebounce_singleElementSurvives() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[] { 42.0f },
                createFloatStream(new float[] { 42.0f }).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray(), 0.0f);
    }

    @Test
    public void testDebounce_coldStreamEmitsOnlyLastElement() {
        // A cold in-memory stream yields all elements with ~0 inter-arrival gap, so every element
        // except the last is superseded within the quiet window; only the final element survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[] { 5.0f },
                createFloatStream(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray(), 0.0f);
    }

    @Test
    public void testDebounce_slowSourceAllSurviveWhenGapAtLeastDuration() {
        // delay() makes each element arrive >= 60ms after the previous; with a 20ms quiet window
        // every element clears the window and survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f },
                createFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).delay(com.landawn.abacus.util.Duration.ofMillis(60))
                        .debounce(com.landawn.abacus.util.Duration.ofMillis(20))
                        .toArray(),
                0.0f);
    }

    @Test
    public void testDebounce_invalidDurationThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> createFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).debounce((com.landawn.abacus.util.Duration) null).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> createFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).debounce(com.landawn.abacus.util.Duration.ofMillis(0)).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> createFloatStream(new float[] { 1.0f, 2.0f, 3.0f }).debounce(com.landawn.abacus.util.Duration.ofMillis(-100)).toArray());
    }

    // ---- perf review 2026-09-26 G085 begin ----
    // G085-01: rotated() nextX()/toArray() replaced the per-element modulo with a conditional subtraction and two bulk copies.
    @Test
    public void testRotated_nextAndToArrayMatchModuloOracle() {
        final int[] distances = { 0, 1, 2, 3, 5, 7, 8, 13, -1, -2, -3, -7, -8, -13, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE + 1 };

        for (int len = 0; len <= 8; len++) {
            final int from = 2;
            final float[] backing = new float[len + 5];

            for (int i = 0; i < backing.length; i++) {
                backing[i] = i == 1 ? Float.NaN : i == 2 ? -0.0f : i * 1.5f - 3;
            }

            for (final int distance : distances) {
                final float[] expected = new float[len];

                for (int i = 0; i < len; i++) {
                    expected[i] = backing[from + (int) Math.floorMod((long) i - distance, (long) len)];
                }

                for (int source = 0; source < 2; source++) {
                    final String msg = "len=" + len + ", distance=" + distance + ", source=" + source;
                    final float[] copy = Arrays.copyOfRange(backing, from, from + len);

                    // full toArray()
                    assertArrayEquals(expected,
                            (source == 0 ? FloatStream.of(backing, from, from + len) : FloatStream.of(FloatIterator.of(copy))).rotated(distance).toArray(),
                            msg);

                    // next() for every element, then partial consumption followed by toArray() / advance() followed by toArray()
                    for (int k = 0; k <= len + 1; k++) {
                        final FloatIteratorEx byNext = (source == 0 ? FloatStream.of(backing, from, from + len) : FloatStream.of(FloatIterator.of(copy)))
                                .rotated(distance)
                                .iteratorEx();

                        for (int j = 0; j < Math.min(k, len); j++) {
                            assertEquals(expected[j], byNext.nextFloat(), msg + ", k=" + k + ", j=" + j);
                        }

                        assertArrayEquals(Arrays.copyOfRange(expected, Math.min(k, len), len), byNext.toArray(), msg + ", k=" + k);
                        org.junit.jupiter.api.Assertions.assertFalse(byNext.hasNext(), msg);

                        final FloatIteratorEx byAdvance = (source == 0 ? FloatStream.of(backing, from, from + len) : FloatStream.of(FloatIterator.of(copy)))
                                .rotated(distance)
                                .iteratorEx();
                        byAdvance.advance(k);
                        assertArrayEquals(Arrays.copyOfRange(expected, Math.min(k, len), len), byAdvance.toArray(), msg + ", advance=" + k);
                    }
                }
            }
        }
    }
    // ---- perf review 2026-09-26 G085 end ----

    // ---- deep review 2026-11-02b G090 begin ----
    // G090-01 (perf pin): sorted()/shuffled(Random) results read through toArray() - empty, single and several elements;
    // array, iterator, mapped and parallel sources; and the reversed()/skip()/limit() stages that read the snapshot again.
    @Test
    public void testSortedShuffledToArray_sourcesAndSizes_g090b() {
        final float[][] inputs = { {}, { 7.0f }, { 3.0f, Float.NaN, -0.0f, 0.0f, 9.0f, -1.0f },
                { 5.0f, 4.0f, Float.NaN, 2.0f, 1.0f, 0.0f, -0.0f, -2.0f, Float.NEGATIVE_INFINITY, -4.0f, Float.POSITIVE_INFINITY, 12.0f } };

        for (final float[] a : inputs) {
            final float[] expected = a.clone();
            java.util.Arrays.sort(expected);
            final float[] descending = new float[expected.length];

            for (int i = 0; i < expected.length; i++) {
                descending[i] = expected[expected.length - 1 - i];
            }

            final int k = Math.min(2, a.length);
            final float[] original = a.clone();

            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, FloatStream.of(a).sorted().toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(original, a);
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected,
                    FloatStream.of(com.landawn.abacus.util.FloatIterator.of(a.clone())).sorted().toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, FloatStream.of(a.clone()).map(x -> x).sorted().toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, FloatStream.of(a.clone()).parallel(2).sorted().toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(descending, FloatStream.of(a.clone()).map(x -> x).sorted().reversed().toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(java.util.Arrays.copyOfRange(expected, k, expected.length),
                    FloatStream.of(a.clone()).sorted().skip(2).toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(java.util.Arrays.copyOf(expected, k), FloatStream.of(a.clone()).sorted().limit(2).toArray());

            final float[] shuffledExpected = a.clone();
            com.landawn.abacus.util.N.shuffle(shuffledExpected, new Random(42));
            org.junit.jupiter.api.Assertions.assertArrayEquals(shuffledExpected, FloatStream.of(a.clone()).shuffled(new Random(42)).toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(shuffledExpected,
                    FloatStream.of(com.landawn.abacus.util.FloatIterator.of(a.clone())).shuffled(new Random(42)).toArray());
        }
    }

    // G090-01 (perf pin): toArray() on a partially consumed sorted() iterator returns exactly the remaining elements and
    // exhausts the iterator; an exhausted or empty one returns an empty array.
    @Test
    public void testSortedIterator_toArrayAfterPartialConsumption_g090b() {
        final com.landawn.abacus.util.FloatIterator iter = FloatStream.of(5.0f, 3.0f, 9.0f, 1.0f, 7.0f).map(x -> x).sorted().iterator();
        org.junit.jupiter.api.Assertions.assertEquals(1.0f, iter.nextFloat());
        org.junit.jupiter.api.Assertions.assertTrue(iter.hasNext());
        org.junit.jupiter.api.Assertions.assertEquals(3.0f, iter.nextFloat());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[] { 5.0f, 7.0f, 9.0f }, iter.toArray());
        org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
        org.junit.jupiter.api.Assertions.assertEquals(0, iter.toArray().length);
        org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class, iter::nextFloat);

        final com.landawn.abacus.util.FloatIterator emptyIter = FloatStream.of(com.landawn.abacus.util.FloatIterator.of(new float[0])).sorted().iterator();
        org.junit.jupiter.api.Assertions.assertEquals(0, emptyIter.toArray().length);
        org.junit.jupiter.api.Assertions.assertFalse(emptyIter.hasNext());
    }

    // G090-01 (perf pin): sorted() stays lazy, and a failure while loading its snapshot is rethrown as the same instance by
    // every later access (toArray() included) without reading the source again.
    @Test
    public void testSortedIterator_loadFailureRethrownByEveryAccess_g090b() {
        final int[] calls = { 0 };
        final com.landawn.abacus.util.FloatIterator iter = FloatStream.of(1.0f, 2.0f, 3.0f).map(x -> {
            calls[0]++;

            if (x == 2.0f) {
                throw new IllegalStateException("load failed");
            }

            return x;
        }).sorted().iterator();

        org.junit.jupiter.api.Assertions.assertEquals(0, calls[0]);

        final IllegalStateException first = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, iter::toArray);
        org.junit.jupiter.api.Assertions.assertEquals("load failed", first.getMessage());
        org.junit.jupiter.api.Assertions.assertSame(first, org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, iter::toArray));
        org.junit.jupiter.api.Assertions.assertSame(first, org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, iter::hasNext));
        org.junit.jupiter.api.Assertions.assertSame(first, org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, iter::nextFloat));
        org.junit.jupiter.api.Assertions.assertEquals(2, calls[0]);
    }

    // G090-01 (perf pin): the snapshot is loaded once on the terminal toArray() and the source's close handler runs once.
    @Test
    public void testSortedToArray_loadsOnceAndClosesSourceOnce_g090b() {
        final int[] closed = { 0 };
        final int[] calls = { 0 };
        final FloatStream sorted = FloatStream.of(3.0f, 1.0f, 2.0f).map(x -> {
            calls[0]++;
            return x;
        }).onClose(() -> closed[0]++).sorted();

        org.junit.jupiter.api.Assertions.assertEquals(0, calls[0]);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, sorted.toArray());
        org.junit.jupiter.api.Assertions.assertEquals(3, calls[0]);
        org.junit.jupiter.api.Assertions.assertEquals(1, closed[0]);
    }

    // ---- deep review 2026-11-02b G090 end ----
    // ---- deep review 2026-11-02b G123 begin ----
    private static FloatList drainG123(final FloatIterator iter) {
        final FloatList result = new FloatList();

        while (iter.hasNext()) {
            result.add(iter.nextFloat());
        }

        return result;
    }

    private static String outcomeG123(final java.util.function.Supplier<FloatList> action) {
        try {
            return "OK " + Arrays.toString(action.get().toArray());
        } catch (final RuntimeException e) {
            return "EX " + e.getClass().getName();
        }
    }

    private static FloatIterator brokenSourceOpG123(final int op) {
        final FloatIterator failingSource = new FloatIterator() {
            private int cnt = 0;

            @Override
            public boolean hasNext() {
                if (cnt >= 2) {
                    throw new IllegalStateException("source failure");
                }

                return true;
            }

            @Override
            public float nextFloat() {
                return (float) cnt++;
            }
        };

        final FloatStream stream = FloatStream.of(failingSource);

        return (op == 0 ? stream.reversed() : op == 1 ? stream.reverseSorted() : stream.rotated(1)).iteratorEx();
    }

    // G123-01 (perf pin, port of G091-03): reversed()/rotated()/reverseSorted() iterators build toList() (used by toFloatList())
    // from an exactly sized copy; pin it against the element-by-element walk for empty/single/partially consumed/sub-range/
    // iterator/parallel sources and rotate distances beyond the size.
    @Test
    public void testReversedRotatedReverseSorted_toFloatList_g123() {
        assertArrayEquals(new float[] { 5f, 4f, 3f, 2f, 1f }, FloatStream.of(1f, 2f, 3f, 4f, 5f).reversed().toFloatList().toArray());
        assertArrayEquals(new float[] { 4f, 5f, 1f, 2f, 3f }, FloatStream.of(1f, 2f, 3f, 4f, 5f).rotated(2).toFloatList().toArray());
        assertArrayEquals(new float[] { 2f, 3f, 4f, 5f, 1f }, FloatStream.of(1f, 2f, 3f, 4f, 5f).rotated(-1).toFloatList().toArray());
        assertArrayEquals(new float[] { 3f, 2f, 1f }, FloatStream.of(3f, 1f, 2f).reverseSorted().toFloatList().toArray());
        assertTrue(FloatStream.empty().reversed().toFloatList().isEmpty());
        assertTrue(FloatStream.empty().rotated(3).toFloatList().isEmpty());
        assertTrue(FloatStream.empty().reverseSorted().toFloatList().isEmpty());

        // NaN and signed zeros keep their exact bits and order.
        final float[] specials = { -1f, -0f, 0f, Float.NaN };
        assertArrayEquals(new float[] { Float.NaN, 0f, -0f, -1f }, FloatStream.of(specials).reversed().toFloatList().toArray());
        assertArrayEquals(new float[] { Float.NaN, 0f, -0f, -1f }, FloatStream.of(-1f, Float.NaN, 0f, -0f).reverseSorted().toFloatList().toArray());
        assertArrayEquals(new float[] { 0f, Float.NaN, -1f, -0f }, FloatStream.of(specials).rotated(-2).toFloatList().toArray());

        final float[] source = { 3f, 1f, 2f };
        final FloatList reversed = FloatStream.of(source).reversed().toFloatList();
        reversed.set(0, 100f);
        reversed.add(7f);
        final FloatList rotated = FloatStream.of(source).rotated(1).toFloatList();
        rotated.set(0, 100f);
        final FloatList reverseSorted = FloatStream.of(source).reverseSorted().toFloatList();
        reverseSorted.set(0, 100f);
        assertArrayEquals(new float[] { 3f, 1f, 2f }, source);
        assertArrayEquals(new float[] { 100f, 1f, 3f, 7f }, reversed.toArray());

        final int[] distances = { Integer.MIN_VALUE, -7, -1, 0, 1, 2, 6, Integer.MAX_VALUE };
        final List<java.util.function.UnaryOperator<FloatStream>> ops = new ArrayList<>();
        ops.add(FloatStream::reversed);
        ops.add(FloatStream::reverseSorted);

        for (final int distance : distances) {
            ops.add(s -> s.rotated(distance));
        }

        final float[] pool = { Float.NaN, -0.0f, 0.0f, 1.5f, -2.5f, Float.NEGATIVE_INFINITY, 3.0f };

        for (int len = 0; len <= 6; len++) {
            final int n = len;
            final float[] padded = new float[n + 2];

            for (int i = 0; i < padded.length; i++) {
                padded[i] = pool[(i * 5) % 7];
            }

            final float[] a = Arrays.copyOfRange(padded, 1, 1 + n);
            final List<java.util.function.Supplier<FloatStream>> sources = List.of(() -> FloatStream.of(a.clone()), () -> FloatStream.of(padded, 1, 1 + n),
                    () -> FloatStream.of(FloatList.of(a.clone()).iterator()), () -> FloatStream.of(a.clone()).parallel(2));

            for (final java.util.function.Supplier<FloatStream> src : sources) {
                for (final java.util.function.UnaryOperator<FloatStream> op : ops) {
                    assertArrayEquals(drainG123(op.apply(src.get()).iteratorEx()).toArray(), op.apply(src.get()).toFloatList().toArray());

                    for (int consumed = 0; consumed <= n + 1; consumed++) {
                        final FloatIterator expectedIter = op.apply(src.get()).iteratorEx();
                        final FloatIterator iter = op.apply(src.get()).iteratorEx();

                        for (int i = 0; i < consumed && iter.hasNext(); i++) {
                            assertEquals(expectedIter.nextFloat(), iter.nextFloat());
                        }

                        final FloatList rest = iter.toList();
                        assertArrayEquals(drainG123(expectedIter).toArray(), rest.toArray());
                        assertFalse(iter.hasNext());
                        assertTrue(iter.toList().isEmpty());
                        rest.add(7f);
                        assertEquals(7f, rest.get(rest.size() - 1));
                    }
                }
            }
        }
    }

    // G123-01 (perf pin): after a failed materialization, toList() must keep behaving like the element-by-element walk on every
    // retry (reversed() keeps the inherited walk while its backing array is still null).
    @Test
    public void testReversedRotatedReverseSorted_toListRetryAfterFailure_g123() {
        for (int op = 0; op < 3; op++) {
            final FloatIterator viaToList = brokenSourceOpG123(op);
            final FloatIterator viaWalk = brokenSourceOpG123(op);

            assertEquals("EX java.lang.IllegalStateException", outcomeG123(viaToList::toList));
            assertEquals("EX java.lang.IllegalStateException", outcomeG123(() -> drainG123(viaWalk)));

            for (int retry = 0; retry < 3; retry++) {
                assertEquals(outcomeG123(() -> drainG123(viaWalk)), outcomeG123(viaToList::toList), "op=" + op + ", retry=" + retry);
            }
        }
    }
    // ---- deep review 2026-11-02b G123 end ----

    private static FloatStream par(final float... a) {
        return FloatStream.of(a).parallel(2);
    }

    @Test
    public void testRateLimitedParallel() {
        final float[] result = par(1f, 2f, 3f).rateLimited(RateLimiter.create(100000)).sorted().toArray();
        assertArrayEquals(new float[] { 1f, 2f, 3f }, result);
    }

    @Test
    public void testDelayParallel() {
        final float[] result = par(1f, 2f).delay(java.time.Duration.ofMillis(1)).sorted().toArray();
        assertArrayEquals(new float[] { 1f, 2f }, result);
    }

    @Test
    public void testDebounceEmptyIteratorNextThrows() {
        final FloatIterator iter = FloatStream.of(FloatIterator.empty()).debounce(com.landawn.abacus.util.Duration.ofMillis(5)).iterator();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextFloat);
    }

    @Test
    public void testScanInitNotIncluded() {
        assertArrayEquals(new float[] { 11f, 13f }, FloatStream.of(1f, 2f).scan(10f, false, (a, b) -> a + b).toArray());
    }

    @Test
    public void testCycledRoundsExhaustedHasNextStaysFalse() {
        final FloatIterator iter = FloatStream.of(1f, 2f).cycled(2).iterator();
        int count = 0;
        while (iter.hasNext()) {
            iter.nextFloat();
            count++;
        }
        assertEquals(4, count);
        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeWithParallel() {
        final float[] merged = par(1f, 3f, 5f).mergeWith(FloatStream.of(2f, 4f), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .sequential()
                .toArray();
        assertEquals(5, merged.length);
        final float[] sorted = merged.clone();
        N.sort(sorted);
        assertArrayEquals(new float[] { 1f, 2f, 3f, 4f, 5f }, sorted);
    }

    @Test
    public void testPercentilesParallel() {
        final Optional<Map<Percentage, Float>> p = par(5f, 1f, 3f, 2f, 4f).percentiles();
        assertTrue(p.isPresent());
        assertFalse(p.get().isEmpty());

        final Pair<FloatSummaryStatistics, Optional<Map<Percentage, Float>>> both = par(5f, 1f, 3f).summaryStatisticsAndPercentiles();
        assertEquals(3, both.left().getCount());
        assertEquals(1f, both.left().getMin(), 0f);
        assertEquals(5f, both.left().getMax(), 0f);
        assertTrue(both.right().isPresent());
    }
}
