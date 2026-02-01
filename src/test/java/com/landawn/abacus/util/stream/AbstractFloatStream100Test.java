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
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.IndexedFloat;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;

@Tag("new-test")
public class AbstractFloatStream100Test extends TestBase {

    private FloatStream stream;
    private FloatStream stream2;

    @BeforeEach
    public void setUp() {
        stream = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f });
        stream2 = createFloatStream(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f });
    }

    protected FloatStream createFloatStream(float... elements) {
        return FloatStream.of(elements);
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(10);
        FloatStream rateLimitedStream = stream.rateLimited(rateLimiter);

        float[] result = rateLimitedStream.toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result);
    }

    @Test
    public void testRateLimited_NullRateLimiter() {
        assertThrows(IllegalArgumentException.class, () -> stream.rateLimited(null));
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
    public void testDelay_NullDuration() {
        assertThrows(IllegalArgumentException.class, () -> stream.delay((Duration) null));
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
        FloatStream result = stream.flatmap(x -> new float[] { x, x * 2 });
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
        Stream<String> result = stream.flattmapToObj(x -> new String[] { String.valueOf(x), String.valueOf(x * 2) });
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
    public void testStep_InvalidStep() {
        assertThrows(IllegalArgumentException.class, () -> stream.step(0));
        assertThrows(IllegalArgumentException.class, () -> stream2.step(-1));
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
}
