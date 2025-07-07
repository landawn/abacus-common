package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.IndexedDouble;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;


public class AbstractDoubleStream100Test extends TestBase {

    private DoubleStream stream;
    private DoubleStream stream2;

    @BeforeEach
    public void setUp() {
        // Initialize with default test data
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });
        stream2 = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });
    }

    private DoubleStream createDoubleStream(double... elements) {
        return DoubleStream.of(elements);
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(10);
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        double[] result = stream.rateLimited(rateLimiter).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testRateLimitedWithNullRateLimiter() {
        assertThrows(IllegalArgumentException.class, () -> stream.rateLimited(null));
    }

    @Test
    public void testDelay() {
        stream = createDoubleStream(new double[] { 1.0, 2.0 });
        Duration delay = Duration.ofMillis(10);

        long startTime = System.currentTimeMillis();
        double[] result = stream.delay(delay).toArray();
        long endTime = System.currentTimeMillis();

        assertArrayEquals(new double[] { 1.0, 2.0 }, result, 0.001);
        assertTrue(endTime - startTime >= 20); // At least 2 delays
    }

    @Test
    public void testDelayWithNullDuration() {
        assertThrows(IllegalArgumentException.class, () -> stream.delay((Duration) null));
    }

    @Test
    public void testSkipUntil() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });

        double[] result = stream.skipUntil(d -> d > 3.0).toArray();

        assertArrayEquals(new double[] { 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testDistinct() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 2.0, 3.0, 3.0, 3.0 });

        double[] result = stream.distinct().toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testFlatmapWithArray() {
        stream = createDoubleStream(new double[] { 1.0, 2.0 });

        double[] result = stream.flatmap(d -> new double[] { d, d * 2 }).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testFlattmap() {
        stream = createDoubleStream(new double[] { 1.0, 2.0 });

        double[] result = stream.flattmap(d -> java.util.stream.DoubleStream.of(d, d * 2)).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testFlatmapToObj() {
        stream = createDoubleStream(new double[] { 1.0, 2.0 });

        List<String> result = stream.flatmapToObj(d -> Arrays.asList(String.valueOf(d), String.valueOf(d * 2))).toList();

        assertEquals(Arrays.asList("1.0", "2.0", "2.0", "4.0"), result);
    }

    @Test
    public void testFlattMapToObj() {
        stream = createDoubleStream(new double[] { 1.0, 2.0 });

        List<String> result = stream.flattMapToObj(d -> new String[] { String.valueOf(d), String.valueOf(d * 2) }).toList();

        assertEquals(Arrays.asList("1.0", "2.0", "2.0", "4.0"), result);
    }

    @Test
    public void testMapMulti() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        double[] result = stream.mapMulti((value, consumer) -> {
            consumer.accept(value);
            consumer.accept(value * 2);
        }).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0, 3.0, 6.0 }, result, 0.001);
    }

    @Test
    public void testMapPartial() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0 });

        double[] result = stream.mapPartial(d -> d % 2 == 0 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).toArray();

        assertArrayEquals(new double[] { 4.0, 8.0 }, result, 0.001);
    }

    @Test
    public void testMapPartialJdk() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0 });

        double[] result = stream.mapPartialJdk(d -> d % 2 == 0 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty()).toArray();

        assertArrayEquals(new double[] { 4.0, 8.0 }, result, 0.001);
    }

    @Test
    public void testRangeMap() {
        stream = createDoubleStream(new double[] { 1.0, 1.5, 2.0, 3.0, 3.5 });

        double[] result = stream.rangeMap((left, right) -> Math.abs(left - right) < 1.0, (left, right) -> left + right).toArray();

        assertArrayEquals(new double[] { 2.5, 4.0, 6.5 }, result, 0.001);
    }

    @Test
    public void testRangeMapToObj() {
        stream = createDoubleStream(new double[] { 1.0, 1.5, 2.0, 3.0, 3.5 });

        List<String> result = stream.rangeMapToObj((left, right) -> Math.abs(left - right) < 1.0, (left, right) -> left + "-" + right).toList();

        assertEquals(Arrays.asList("1.0-1.5", "2.0-2.0", "3.0-3.5"), result);
    }

    @Test
    public void testCollapseWithBiPredicate() {
        stream = createDoubleStream(new double[] { 1.0, 1.1, 1.2, 2.0, 2.1, 3.0 });

        List<DoubleList> result = stream.collapse((a, b) -> Math.abs(a - b) < 0.5).toList();

        assertEquals(3, result.size());
        assertArrayEquals(new double[] { 1.0, 1.1, 1.2 }, result.get(0).toArray(), 0.001);
        assertArrayEquals(new double[] { 2.0, 2.1 }, result.get(1).toArray(), 0.001);
        assertArrayEquals(new double[] { 3.0 }, result.get(2).toArray(), 0.001);
    }

    @Test
    public void testCollapseWithBiPredicateAndMergeFunction() {
        stream = createDoubleStream(new double[] { 1.0, 1.1, 1.2, 2.0, 2.1, 3.0 });

        double[] result = stream.collapse((a, b) -> Math.abs(a - b) < 0.5, (a, b) -> a + b).toArray();

        assertArrayEquals(new double[] { 3.3, 4.1, 3.0 }, result, 0.001);
    }

    @Test
    public void testCollapseWithTriPredicate() {
        stream = createDoubleStream(new double[] { 1.0, 1.5, 2.0, 3.0, 3.5, 4.0 });

        double[] result = stream.collapse((first, prev, curr) -> curr - first < 2.0, (a, b) -> a + b).toArray();

        assertArrayEquals(new double[] { 4.5, 10.5 }, result, 0.001);
    }

    @Test
    public void testSkipWithAction() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });
        List<Double> skipped = new ArrayList<>();

        double[] result = stream.skip(2, skipped::add).toArray();

        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, result, 0.001);
        assertEquals(Arrays.asList(1.0, 2.0), skipped);
    }

    @Test
    public void testFilterWithAction() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });
        List<Double> dropped = new ArrayList<>();

        double[] result = stream.filter(d -> d % 2 == 0, dropped::add).toArray();

        assertArrayEquals(new double[] { 2.0, 4.0 }, result, 0.001);
        assertEquals(Arrays.asList(1.0, 3.0, 5.0), dropped);
    }

    @Test
    public void testDropWhileWithAction() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });
        List<Double> dropped = new ArrayList<>();

        double[] result = stream.dropWhile(d -> d < 3.0, dropped::add).toArray();

        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, result, 0.001);
        assertEquals(Arrays.asList(1.0, 2.0), dropped);
    }

    @Test
    public void testStep() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 });

        double[] result = stream.step(2).toArray();

        assertArrayEquals(new double[] { 1.0, 3.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testStepWithInvalidStep() {
        assertThrows(IllegalArgumentException.class, () -> stream.step(0));
        assertThrows(IllegalArgumentException.class, () -> stream2.step(-1));
    }

    @Test
    public void testScan() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0 });

        double[] result = stream.scan((a, b) -> a + b).toArray();

        assertArrayEquals(new double[] { 1.0, 3.0, 6.0, 10.0 }, result, 0.001);
    }

    @Test
    public void testScanWithInit() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0 });

        double[] result = stream.scan(10.0, (a, b) -> a + b).toArray();

        assertArrayEquals(new double[] { 11.0, 13.0, 16.0, 20.0 }, result, 0.001);
    }

    @Test
    public void testScanWithInitIncluded() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0 });

        double[] result = stream.scan(10.0, true, (a, b) -> a + b).toArray();

        assertArrayEquals(new double[] { 10.0, 11.0, 13.0, 16.0, 20.0 }, result, 0.001);
    }

    @Test
    public void testIntersection() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });

        double[] result = stream.intersection(Arrays.asList(2.0, 3.0, 6.0)).toArray();

        assertArrayEquals(new double[] { 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testDifference() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });

        double[] result = stream.difference(Arrays.asList(2.0, 3.0, 6.0)).toArray();

        assertArrayEquals(new double[] { 1.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testSymmetricDifference() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });

        double[] result = stream.symmetricDifference(Arrays.asList(2.0, 3.0, 6.0, 7.0)).toArray();

        // Elements in stream but not in collection: 1.0, 4.0, 5.0
        // Elements in collection but not in stream: 6.0, 7.0
        double[] expected = { 1.0, 4.0, 5.0, 6.0, 7.0 };
        Arrays.sort(result);
        Arrays.sort(expected);
        assertArrayEquals(expected, result, 0.001);
    }

    @Test
    public void testReversed() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });

        double[] result = stream.reversed().toArray();

        assertArrayEquals(new double[] { 5.0, 4.0, 3.0, 2.0, 1.0 }, result, 0.001);
    }

    @Test
    public void testRotated() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });

        double[] result = stream.rotated(2).toArray();

        assertArrayEquals(new double[] { 4.0, 5.0, 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testRotatedNegative() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });

        double[] result = stream.rotated(-2).toArray();

        assertArrayEquals(new double[] { 3.0, 4.0, 5.0, 1.0, 2.0 }, result, 0.001);
    }

    @Test
    public void testShuffled() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });
        Random rnd = new Random(12345); // Fixed seed for deterministic test

        double[] result = stream.shuffled(rnd).toArray();

        // Verify all elements are present
        Arrays.sort(result);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testShuffledWithNullRandom() {
        assertThrows(IllegalArgumentException.class, () -> stream.shuffled(null));
    }

    @Test
    public void testSorted() {
        stream = createDoubleStream(new double[] { 5.0, 3.0, 1.0, 4.0, 2.0 });

        double[] result = stream.sorted().toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testReverseSorted() {
        stream = createDoubleStream(new double[] { 2.0, 5.0, 1.0, 4.0, 3.0 });

        double[] result = stream.reverseSorted().toArray();

        assertArrayEquals(new double[] { 5.0, 4.0, 3.0, 2.0, 1.0 }, result, 0.001);
    }

    @Test
    public void testCycled() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        double[] result = stream.cycled().limit(10).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0 }, result, 0.001);
    }

    @Test
    public void testCycledWithRounds() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        double[] result = stream.cycled(2).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testCycledWithZeroRounds() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        double[] result = stream.cycled(0).toArray();

        assertArrayEquals(new double[] {}, result, 0.001);
    }

    @Test
    public void testIndexed() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        List<IndexedDouble> result = stream.indexed().toList();

        assertEquals(3, result.size());
        assertEquals(1.0, result.get(0).value(), 0.001);
        assertEquals(0, result.get(0).index());
        assertEquals(2.0, result.get(1).value(), 0.001);
        assertEquals(1, result.get(1).index());
        assertEquals(3.0, result.get(2).value(), 0.001);
        assertEquals(2, result.get(2).index());
    }

    @Test
    public void testBoxed() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        List<Double> result = stream.boxed().toList();

        assertEquals(Arrays.asList(1.0, 2.0, 3.0), result);
    }

    @Test
    public void testPrependArray() {
        stream = createDoubleStream(new double[] { 3.0, 4.0, 5.0 });

        double[] result = stream.prepend(1.0, 2.0).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testPrependStream() {
        stream = createDoubleStream(new double[] { 3.0, 4.0, 5.0 });
        DoubleStream prepend = createDoubleStream(new double[] { 1.0, 2.0 });

        double[] result = stream.prepend(prepend).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testPrependOptional() {
        stream = createDoubleStream(new double[] { 2.0, 3.0 });

        double[] result1 = stream.prepend(OptionalDouble.of(1.0)).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result1, 0.001);

        stream = createDoubleStream(new double[] { 2.0, 3.0 });
        double[] result2 = stream.prepend(OptionalDouble.empty()).toArray();
        assertArrayEquals(new double[] { 2.0, 3.0 }, result2, 0.001);
    }

    @Test
    public void testAppendArray() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        double[] result = stream.append(4.0, 5.0).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testAppendStream() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        DoubleStream append = createDoubleStream(new double[] { 4.0, 5.0 });

        double[] result = stream.append(append).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testAppendOptional() {
        stream = createDoubleStream(new double[] { 1.0, 2.0 });

        double[] result1 = stream.append(OptionalDouble.of(3.0)).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result1, 0.001);

        stream = createDoubleStream(new double[] { 1.0, 2.0 });
        double[] result2 = stream.append(OptionalDouble.empty()).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0 }, result2, 0.001);
    }

    @Test
    public void testAppendIfEmpty() {
        stream = createDoubleStream(new double[] {});

        double[] result = stream.appendIfEmpty(1.0, 2.0, 3.0).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testAppendIfEmptyNotEmpty() {
        stream = createDoubleStream(new double[] { 1.0, 2.0 });

        double[] result = stream.appendIfEmpty(3.0, 4.0).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0 }, result, 0.001);
    }

    @Test
    public void testMergeWith() {
        stream = createDoubleStream(new double[] { 1.0, 3.0, 5.0 });
        DoubleStream other = createDoubleStream(new double[] { 2.0, 4.0, 6.0 });

        double[] result = stream.mergeWith(other, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, result, 0.001);
    }

    @Test
    public void testZipWith() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        DoubleStream other = createDoubleStream(new double[] { 4.0, 5.0, 6.0 });

        double[] result = stream.zipWith(other, (a, b) -> a + b).toArray();

        assertArrayEquals(new double[] { 5.0, 7.0, 9.0 }, result, 0.001);
    }

    @Test
    public void testZipWithThreeStreams() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        DoubleStream second = createDoubleStream(new double[] { 4.0, 5.0, 6.0 });
        DoubleStream third = createDoubleStream(new double[] { 7.0, 8.0, 9.0 });

        double[] result = stream.zipWith(second, third, (a, b, c) -> a + b + c).toArray();

        assertArrayEquals(new double[] { 12.0, 15.0, 18.0 }, result, 0.001);
    }

    @Test
    public void testZipWithDefaultValues() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        DoubleStream other = createDoubleStream(new double[] { 4.0, 5.0 });

        double[] result = stream.zipWith(other, 0.0, 10.0, (a, b) -> a + b).toArray();

        assertArrayEquals(new double[] { 5.0, 7.0, 13.0 }, result, 0.001);
    }

    @Test
    public void testZipWithThreeStreamsDefaultValues() {
        stream = createDoubleStream(new double[] { 1.0, 2.0 });
        DoubleStream second = createDoubleStream(new double[] { 3.0 });
        DoubleStream third = createDoubleStream(new double[] { 5.0, 6.0, 7.0 });

        double[] result = stream.zipWith(second, third, 0.0, 0.0, 0.0, (a, b, c) -> a + b + c).toArray();

        assertArrayEquals(new double[] { 9.0, 8.0, 7.0 }, result, 0.001);
    }

    @Test
    public void testTop() {
        stream = createDoubleStream(new double[] { 5.0, 2.0, 8.0, 1.0, 9.0, 3.0 });

        double[] result = stream.top(3).toArray();

        Arrays.sort(result);
        assertArrayEquals(new double[] { 5.0, 8.0, 9.0 }, result, 0.001);
    }

    @Test
    public void testToMap() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        Map<String, Double> result = stream.toMap(d -> "key" + (int) d, d -> d * 2);

        assertEquals(3, result.size());
        assertEquals(2.0, result.get("key1"), 0.001);
        assertEquals(4.0, result.get("key2"), 0.001);
        assertEquals(6.0, result.get("key3"), 0.001);
    }

    @Test
    public void testToMapWithSupplier() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        Map<String, Double> result = stream.toMap(d -> "key" + (int) d, d -> d * 2, Suppliers.ofLinkedHashMap());

        assertEquals(3, result.size());
        assertEquals(2.0, result.get("key1"), 0.001);
        assertEquals(4.0, result.get("key2"), 0.001);
        assertEquals(6.0, result.get("key3"), 0.001);
    }

    @Test
    public void testToMapWithMergeFunction() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 1.0, 3.0 });

        Map<String, Double> result = stream.toMap(d -> "key" + (int) d, d -> d, (v1, v2) -> v1 + v2);

        assertEquals(3, result.size());
        assertEquals(2.0, result.get("key1"), 0.001);
        assertEquals(2.0, result.get("key2"), 0.001);
        assertEquals(3.0, result.get("key3"), 0.001);
    }

    @Test
    public void testGroupTo() {
        stream = createDoubleStream(new double[] { 1.1, 2.1, 1.2, 3.1, 2.2 });

        Map<Integer, List<Double>> result = stream.groupTo(d -> (int) d, Collectors.toList());

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1.1, 1.2), result.get(1));
        assertEquals(Arrays.asList(2.1, 2.2), result.get(2));
        assertEquals(Arrays.asList(3.1), result.get(3));
    }

    @Test
    public void testForEachIndexed() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        List<String> result = new ArrayList<>();

        stream.forEachIndexed((index, value) -> result.add(index + ":" + value));

        assertEquals(Arrays.asList("0:1.0", "1:2.0", "2:3.0"), result);
    }

    @Test
    public void testSum() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });

        double result = stream.sum();

        assertEquals(15.0, result, 0.001);
    }

    @Test
    public void testSumEmpty() {
        stream = createDoubleStream(new double[] {});

        double result = stream.sum();

        assertEquals(0.0, result, 0.001);
    }

    @Test
    public void testAverage() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });

        OptionalDouble result = stream.average();

        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageEmpty() {
        stream = createDoubleStream(new double[] {});

        OptionalDouble result = stream.average();

        assertFalse(result.isPresent());
    }

    @Test
    public void testFirst() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        OptionalDouble result = stream.first();

        assertTrue(result.isPresent());
        assertEquals(1.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testFirstEmpty() {
        stream = createDoubleStream(new double[] {});

        OptionalDouble result = stream.first();

        assertFalse(result.isPresent());
    }

    @Test
    public void testLast() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        OptionalDouble result = stream.last();

        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testLastEmpty() {
        stream = createDoubleStream(new double[] {});

        OptionalDouble result = stream.last();

        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne() {
        stream = createDoubleStream(new double[] { 42.0 });

        OptionalDouble result = stream.onlyOne();

        assertTrue(result.isPresent());
        assertEquals(42.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testOnlyOneEmpty() {
        stream = createDoubleStream(new double[] {});

        OptionalDouble result = stream.onlyOne();

        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOneTooMany() {
        stream = createDoubleStream(new double[] { 1.0, 2.0 });

        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testFindAny() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });

        OptionalDouble result = stream.findAny(d -> d > 3.0);

        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testFindAnyNotFound() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        OptionalDouble result = stream.findAny(d -> d > 10.0);

        assertFalse(result.isPresent());
    }

    @Test
    public void testPercentiles() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 });

        Optional<Map<Percentage, Double>> result = stream.percentiles();

        assertTrue(result.isPresent());
        Map<Percentage, Double> percentiles = result.get();

        // Check some common percentiles
        assertTrue(percentiles.containsKey(Percentage._20));
        assertTrue(percentiles.containsKey(Percentage._50));
        assertTrue(percentiles.containsKey(Percentage._70));
    }

    @Test
    public void testPercentilesEmpty() {
        stream = createDoubleStream(new double[] {});

        Optional<Map<Percentage, Double>> result = stream.percentiles();

        assertFalse(result.isPresent());
    }

    @Test
    public void testSummarizeAndPercentiles() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });

        Pair<DoubleSummaryStatistics, Optional<Map<Percentage, Double>>> result = stream.summarizeAndPercentiles();

        DoubleSummaryStatistics stats = result.left();
        assertEquals(5, stats.getCount());
        assertEquals(1.0, stats.getMin(), 0.001);
        assertEquals(5.0, stats.getMax(), 0.001);
        assertEquals(15.0, stats.getSum(), 0.001);

        assertTrue(result.right().isPresent());
    }

    @Test
    public void testJoin() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        String result = stream.join(", ", "[", "]");

        assertEquals("[1.0, 2.0, 3.0]", result);
    }

    @Test
    public void testJoinTo() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });
        Joiner joiner = Joiner.with(", ");

        Joiner result = stream.joinTo(joiner);

        assertEquals("1.0, 2.0, 3.0", result.toString());
    }

    @Test
    public void testCollect() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        List<Double> result = stream.collect(ArrayList::new, (list, value) -> list.add(value));

        assertEquals(Arrays.asList(1.0, 2.0, 3.0), result);
    }

    @Test
    public void testIterator() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        DoubleIterator iter = stream.iterator();

        assertTrue(iter.hasNext());
        assertEquals(1.0, iter.nextDouble(), 0.001);
        assertTrue(iter.hasNext());
        assertEquals(2.0, iter.nextDouble(), 0.001);
        assertTrue(iter.hasNext());
        assertEquals(3.0, iter.nextDouble(), 0.001);
        assertFalse(iter.hasNext());
    }
}
