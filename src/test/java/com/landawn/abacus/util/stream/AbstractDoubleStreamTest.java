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
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IndexedDouble;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;

public class AbstractDoubleStreamTest extends TestBase {

    @Test
    public void testScanInitializesOnlyAfterSuccessfulSourceRead() {
        final IllegalStateException failure = new IllegalStateException("first read failed");
        final java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger accumulatorCalls = new java.util.concurrent.atomic.AtomicInteger();

        try (DoubleStream stream = DoubleStream.of(new double[] { 1, 2, 3 }).map(value -> {
            if (attempts.getAndIncrement() == 0) {
                throw failure;
            }
            return value;
        }).scan((left, right) -> {
            accumulatorCalls.incrementAndGet();
            return left + right;
        })) {
            final com.landawn.abacus.util.DoubleIterator iter = stream.iterator();
            org.junit.jupiter.api.Assertions.assertSame(failure, org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, iter::nextDouble));
            org.junit.jupiter.api.Assertions.assertEquals(2, iter.nextDouble());
            org.junit.jupiter.api.Assertions.assertEquals(0, accumulatorCalls.get());
            org.junit.jupiter.api.Assertions.assertEquals(5, iter.nextDouble());
            org.junit.jupiter.api.Assertions.assertEquals(1, accumulatorCalls.get());
            org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
        }
    }

    private DoubleStream stream;
    private DoubleStream stream2;

    @BeforeEach
    public void setUp() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });
        stream2 = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });
    }

    private DoubleStream createDoubleStream(double... elements) {
        return DoubleStream.of(elements).map(e -> e + 0);
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(10);
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0 });

        double[] result = stream.rateLimited(rateLimiter).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testDelay() {
        stream = createDoubleStream(new double[] { 1.0, 2.0 });
        Duration delay = Duration.ofMillis(100);

        long startTime = System.currentTimeMillis();
        double[] result = stream.delay(delay).toArray();
        long endTime = System.currentTimeMillis();

        assertArrayEquals(new double[] { 1.0, 2.0 }, result, 0.001);
        assertTrue(endTime - startTime < 200);
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

        double[] result = stream.flatMapArray(d -> new double[] { d, d * 2 }).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testFlattmap() {
        stream = createDoubleStream(new double[] { 1.0, 2.0 });

        double[] result = stream.flattMap(d -> java.util.stream.DoubleStream.of(d, d * 2)).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testFlattMapToObj() {
        stream = createDoubleStream(new double[] { 1.0, 2.0 });

        List<String> result = stream.flatMapArrayToObj(d -> new String[] { String.valueOf(d), String.valueOf(d * 2) }).toList();

        assertEquals(Arrays.asList("1.0", "2.0", "2.0", "4.0"), result);
    }

    @Test
    public void testFlatmapToObj() {
        stream = createDoubleStream(new double[] { 1.0, 2.0 });

        List<String> result = stream.flatmapToObj(d -> Arrays.asList(String.valueOf(d), String.valueOf(d * 2))).toList();

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
    public void testArrayStepIteratorCountConsumes() {
        DoubleIterator iter = DoubleStream.of(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }).step(2).iterator();
        assertEquals(3, iter.count());
        assertFalse(iter.hasNext());
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
        Random rnd = new Random(12345);

        double[] result = stream.shuffled(rnd).toArray();

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
    public void testCycled_long() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        List<Double> result = stream.cycled(3).collect(ArrayList::new, (list, value) -> list.add(value));
        assertEquals(Arrays.asList(1.0, 2.0, 1.0, 2.0, 1.0, 2.0), result);
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
    public void testPrependAppend_Stream_Parallel() {
        double[] result = createDoubleStream(3.0, 4.0).parallel().prepend(DoubleStream.of(1.0, 2.0)).append(DoubleStream.of(5.0)).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
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
    public void testFirst_MultipleElements() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        OptionalDouble result = stream.first();
        assertTrue(result.isPresent());
        assertEquals(1.0, result.getAsDouble(), 0.001);
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
    public void testsummaryStatisticsAndPercentiles() {
        stream = createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });

        Pair<DoubleSummaryStatistics, Optional<Map<Percentage, Double>>> result = stream.summaryStatisticsAndPercentiles();

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
    public void testJoinToRejectsNullForEmptyStream() {
        assertThrows(IllegalArgumentException.class, () -> createDoubleStream().joinTo(null));
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

    @Test
    public void testSummaryStatisticsAndPercentiles_NaNPropagation() {
        // DoubleSummaryStatistics.accept uses Math.min/Math.max so any NaN propagates
        // into both min and max. summaryStatisticsAndPercentiles() must give the same
        // result so the two APIs do not diverge for the same data.
        Pair<DoubleSummaryStatistics, Optional<Map<Percentage, Double>>> result = createDoubleStream(new double[] { 1.0, 2.0, Double.NaN, 3.0 })
                .summaryStatisticsAndPercentiles();
        DoubleSummaryStatistics stats = result.left();
        assertEquals(4, stats.getCount());
        assertTrue(Double.isNaN(stats.getMin()), "min should be NaN when input contains NaN");
        assertTrue(Double.isNaN(stats.getMax()), "max should be NaN when input contains NaN");
    }

    @Test
    public void testReversedRotatedReverseSorted_ToArrayExhaustsIterator() {
        DoubleIteratorEx reversed = createDoubleStream(new double[] { 1, 2, 3, 4, 5 }).reversed().iteratorEx();
        assertEquals(5, reversed.toArray().length);
        assertFalse(reversed.hasNext());
        assertEquals(0, reversed.toArray().length);

        DoubleIteratorEx rotated = createDoubleStream(new double[] { 1, 2, 3, 4, 5 }).rotated(2).iteratorEx();
        rotated.nextDouble();
        assertEquals(4, rotated.toArray().length);
        assertFalse(rotated.hasNext());
        assertEquals(0, rotated.toArray().length);

        DoubleIteratorEx reverseSorted = createDoubleStream(new double[] { 3, 1, 4, 1, 5 }).reverseSorted().iteratorEx();
        reverseSorted.nextDouble();
        reverseSorted.nextDouble();
        assertEquals(3, reverseSorted.toArray().length);
        assertFalse(reverseSorted.hasNext());
        assertEquals(0, reverseSorted.toArray().length);
    }

    @Test
    public void testDebounce_emptyEmitsNothing() {
        org.junit.jupiter.api.Assertions.assertEquals(0,
                createDoubleStream(new double[] {}).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray().length);
    }

    @Test
    public void testDebounce_singleElementSurvives() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[] { 42.0 },
                createDoubleStream(new double[] { 42.0 }).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray(), 0.0);
    }

    @Test
    public void testDebounce_coldStreamEmitsOnlyLastElement() {
        // A cold in-memory stream yields all elements with ~0 inter-arrival gap, so every element
        // except the last is superseded within the quiet window; only the final element survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[] { 5.0 },
                createDoubleStream(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toArray(), 0.0);
    }

    @Test
    public void testDebounce_slowSourceAllSurviveWhenGapAtLeastDuration() {
        // delay() makes each element arrive >= 60ms after the previous; with a 20ms quiet window
        // every element clears the window and survives.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[] { 1.0, 2.0, 3.0 },
                createDoubleStream(new double[] { 1.0, 2.0, 3.0 }).delay(com.landawn.abacus.util.Duration.ofMillis(60))
                        .debounce(com.landawn.abacus.util.Duration.ofMillis(20))
                        .toArray(),
                0.0);
    }

    @Test
    public void testDebounce_invalidDurationThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> createDoubleStream(new double[] { 1.0, 2.0, 3.0 }).debounce((com.landawn.abacus.util.Duration) null).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> createDoubleStream(new double[] { 1.0, 2.0, 3.0 }).debounce(com.landawn.abacus.util.Duration.ofMillis(0)).toArray());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> createDoubleStream(new double[] { 1.0, 2.0, 3.0 }).debounce(com.landawn.abacus.util.Duration.ofMillis(-100)).toArray());
    }
    // ------------------------------------------------------------------------------------------------------
    // Stream review 2026-09-09 (pass B) - prepend/append(OptionalDouble) argument validation
    // ------------------------------------------------------------------------------------------------------

    /** Sibling of {@code AbstractIntStreamTest}'s check: the same gap existed in all seven primitive types. */
    @Test
    public void testPrependAppendOptional_nullIsRejectedAndTheStreamIsClosed() {
        final MutableBoolean closed = MutableBoolean.of(false);
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleStream.of(1d, 2d).onClose(closed::setTrue).prepend((OptionalDouble) null));
        assertTrue(closed.isTrue(), "prepend(null) must close the stream before throwing");

        closed.setFalse();
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleStream.of(1d, 2d).onClose(closed::setTrue).append((OptionalDouble) null));
        assertTrue(closed.isTrue(), "append(null) must close the stream before throwing");

        assertArrayEquals(new double[] { 9d, 1d, 2d }, DoubleStream.of(1d, 2d).prepend(OptionalDouble.of(9d)).toArray(), 0.0);
        assertArrayEquals(new double[] { 1d, 2d, 9d }, DoubleStream.of(1d, 2d).append(OptionalDouble.of(9d)).toArray(), 0.0);
    }

    // ---- perf review 2026-09-26 G085 begin ----
    // G085-01: rotated() nextX()/toArray() replaced the per-element modulo with a conditional subtraction and two bulk copies.
    @Test
    public void testRotated_nextAndToArrayMatchModuloOracle() {
        final int[] distances = { 0, 1, 2, 3, 5, 7, 8, 13, -1, -2, -3, -7, -8, -13, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE + 1 };

        for (int len = 0; len <= 8; len++) {
            final int from = 2;
            final double[] backing = new double[len + 5];

            for (int i = 0; i < backing.length; i++) {
                backing[i] = i == 1 ? Double.NaN : i == 2 ? -0.0d : i * 1.5d - 3;
            }

            for (final int distance : distances) {
                final double[] expected = new double[len];

                for (int i = 0; i < len; i++) {
                    expected[i] = backing[from + (int) Math.floorMod((long) i - distance, (long) len)];
                }

                for (int source = 0; source < 2; source++) {
                    final String msg = "len=" + len + ", distance=" + distance + ", source=" + source;
                    final double[] copy = Arrays.copyOfRange(backing, from, from + len);

                    // full toArray()
                    assertArrayEquals(expected,
                            (source == 0 ? DoubleStream.of(backing, from, from + len) : DoubleStream.of(DoubleIterator.of(copy))).rotated(distance).toArray(),
                            msg);

                    // next() for every element, then partial consumption followed by toArray() / advance() followed by toArray()
                    for (int k = 0; k <= len + 1; k++) {
                        final DoubleIteratorEx byNext = (source == 0 ? DoubleStream.of(backing, from, from + len) : DoubleStream.of(DoubleIterator.of(copy)))
                                .rotated(distance)
                                .iteratorEx();

                        for (int j = 0; j < Math.min(k, len); j++) {
                            assertEquals(expected[j], byNext.nextDouble(), msg + ", k=" + k + ", j=" + j);
                        }

                        assertArrayEquals(Arrays.copyOfRange(expected, Math.min(k, len), len), byNext.toArray(), msg + ", k=" + k);
                        org.junit.jupiter.api.Assertions.assertFalse(byNext.hasNext(), msg);

                        final DoubleIteratorEx byAdvance = (source == 0 ? DoubleStream.of(backing, from, from + len) : DoubleStream.of(DoubleIterator.of(copy)))
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
        final double[][] inputs = { {}, { 7.0 }, { 3.0, Double.NaN, -0.0, 0.0, 9.0, -1.0 },
                { 5.0, 4.0, Double.NaN, 2.0, 1.0, 0.0, -0.0, -2.0, Double.NEGATIVE_INFINITY, -4.0, Double.POSITIVE_INFINITY, 12.0 } };

        for (final double[] a : inputs) {
            final double[] expected = a.clone();
            java.util.Arrays.sort(expected);
            final double[] descending = new double[expected.length];

            for (int i = 0; i < expected.length; i++) {
                descending[i] = expected[expected.length - 1 - i];
            }

            final int k = Math.min(2, a.length);
            final double[] original = a.clone();

            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, DoubleStream.of(a).sorted().toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(original, a);
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected,
                    DoubleStream.of(com.landawn.abacus.util.DoubleIterator.of(a.clone())).sorted().toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, DoubleStream.of(a.clone()).map(x -> x).sorted().toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, DoubleStream.of(a.clone()).parallel(2).sorted().toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(descending, DoubleStream.of(a.clone()).map(x -> x).sorted().reversed().toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(java.util.Arrays.copyOfRange(expected, k, expected.length),
                    DoubleStream.of(a.clone()).sorted().skip(2).toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(java.util.Arrays.copyOf(expected, k), DoubleStream.of(a.clone()).sorted().limit(2).toArray());

            final double[] shuffledExpected = a.clone();
            com.landawn.abacus.util.N.shuffle(shuffledExpected, new Random(42));
            org.junit.jupiter.api.Assertions.assertArrayEquals(shuffledExpected, DoubleStream.of(a.clone()).shuffled(new Random(42)).toArray());
            org.junit.jupiter.api.Assertions.assertArrayEquals(shuffledExpected,
                    DoubleStream.of(com.landawn.abacus.util.DoubleIterator.of(a.clone())).shuffled(new Random(42)).toArray());
        }
    }

    // G090-01 (perf pin): toArray() on a partially consumed sorted() iterator returns exactly the remaining elements and
    // exhausts the iterator; an exhausted or empty one returns an empty array.
    @Test
    public void testSortedIterator_toArrayAfterPartialConsumption_g090b() {
        final com.landawn.abacus.util.DoubleIterator iter = DoubleStream.of(5.0, 3.0, 9.0, 1.0, 7.0).map(x -> x).sorted().iterator();
        org.junit.jupiter.api.Assertions.assertEquals(1.0, iter.nextDouble());
        org.junit.jupiter.api.Assertions.assertTrue(iter.hasNext());
        org.junit.jupiter.api.Assertions.assertEquals(3.0, iter.nextDouble());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[] { 5.0, 7.0, 9.0 }, iter.toArray());
        org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
        org.junit.jupiter.api.Assertions.assertEquals(0, iter.toArray().length);
        org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class, iter::nextDouble);

        final com.landawn.abacus.util.DoubleIterator emptyIter = DoubleStream.of(com.landawn.abacus.util.DoubleIterator.of(new double[0])).sorted().iterator();
        org.junit.jupiter.api.Assertions.assertEquals(0, emptyIter.toArray().length);
        org.junit.jupiter.api.Assertions.assertFalse(emptyIter.hasNext());
    }

    // G090-01 (perf pin): sorted() stays lazy, and a failure while loading its snapshot is rethrown as the same instance by
    // every later access (toArray() included) without reading the source again.
    @Test
    public void testSortedIterator_loadFailureRethrownByEveryAccess_g090b() {
        final int[] calls = { 0 };
        final com.landawn.abacus.util.DoubleIterator iter = DoubleStream.of(1.0, 2.0, 3.0).map(x -> {
            calls[0]++;

            if (x == 2.0) {
                throw new IllegalStateException("load failed");
            }

            return x;
        }).sorted().iterator();

        org.junit.jupiter.api.Assertions.assertEquals(0, calls[0]);

        final IllegalStateException first = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, iter::toArray);
        org.junit.jupiter.api.Assertions.assertEquals("load failed", first.getMessage());
        org.junit.jupiter.api.Assertions.assertSame(first, org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, iter::toArray));
        org.junit.jupiter.api.Assertions.assertSame(first, org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, iter::hasNext));
        org.junit.jupiter.api.Assertions.assertSame(first, org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, iter::nextDouble));
        org.junit.jupiter.api.Assertions.assertEquals(2, calls[0]);
    }

    // G090-01 (perf pin): the snapshot is loaded once on the terminal toArray() and the source's close handler runs once.
    @Test
    public void testSortedToArray_loadsOnceAndClosesSourceOnce_g090b() {
        final int[] closed = { 0 };
        final int[] calls = { 0 };
        final DoubleStream sorted = DoubleStream.of(3.0, 1.0, 2.0).map(x -> {
            calls[0]++;
            return x;
        }).onClose(() -> closed[0]++).sorted();

        org.junit.jupiter.api.Assertions.assertEquals(0, calls[0]);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, sorted.toArray());
        org.junit.jupiter.api.Assertions.assertEquals(3, calls[0]);
        org.junit.jupiter.api.Assertions.assertEquals(1, closed[0]);
    }

    // ---- deep review 2026-11-02b G090 end ----
    // ---- deep review 2026-11-02b G123 begin ----
    private static DoubleList drainG123(final DoubleIterator iter) {
        final DoubleList result = new DoubleList();

        while (iter.hasNext()) {
            result.add(iter.nextDouble());
        }

        return result;
    }

    private static String outcomeG123(final java.util.function.Supplier<DoubleList> action) {
        try {
            return "OK " + Arrays.toString(action.get().toArray());
        } catch (final RuntimeException e) {
            return "EX " + e.getClass().getName();
        }
    }

    private static DoubleIterator brokenSourceOpG123(final int op) {
        final DoubleIterator failingSource = new DoubleIterator() {
            private int cnt = 0;

            @Override
            public boolean hasNext() {
                if (cnt >= 2) {
                    throw new IllegalStateException("source failure");
                }

                return true;
            }

            @Override
            public double nextDouble() {
                return (double) cnt++;
            }
        };

        final DoubleStream stream = DoubleStream.of(failingSource);

        return (op == 0 ? stream.reversed() : op == 1 ? stream.reverseSorted() : stream.rotated(1)).iteratorEx();
    }

    // G123-01 (perf pin, port of G091-03): reversed()/rotated()/reverseSorted() iterators build toList() (used by toDoubleList())
    // from an exactly sized copy; pin it against the element-by-element walk for empty/single/partially consumed/sub-range/
    // iterator/parallel sources and rotate distances beyond the size.
    @Test
    public void testReversedRotatedReverseSorted_toDoubleList_g123() {
        assertArrayEquals(new double[] { 5.0, 4.0, 3.0, 2.0, 1.0 }, DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).reversed().toDoubleList().toArray());
        assertArrayEquals(new double[] { 4.0, 5.0, 1.0, 2.0, 3.0 }, DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).rotated(2).toDoubleList().toArray());
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0, 5.0, 1.0 }, DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).rotated(-1).toDoubleList().toArray());
        assertArrayEquals(new double[] { 3.0, 2.0, 1.0 }, DoubleStream.of(3.0, 1.0, 2.0).reverseSorted().toDoubleList().toArray());
        assertTrue(DoubleStream.empty().reversed().toDoubleList().isEmpty());
        assertTrue(DoubleStream.empty().rotated(3).toDoubleList().isEmpty());
        assertTrue(DoubleStream.empty().reverseSorted().toDoubleList().isEmpty());

        // NaN and signed zeros keep their exact bits and order.
        final double[] specials = { -1.0, -0.0, 0.0, Double.NaN };
        assertArrayEquals(new double[] { Double.NaN, 0.0, -0.0, -1.0 }, DoubleStream.of(specials).reversed().toDoubleList().toArray());
        assertArrayEquals(new double[] { Double.NaN, 0.0, -0.0, -1.0 }, DoubleStream.of(-1.0, Double.NaN, 0.0, -0.0).reverseSorted().toDoubleList().toArray());
        assertArrayEquals(new double[] { 0.0, Double.NaN, -1.0, -0.0 }, DoubleStream.of(specials).rotated(-2).toDoubleList().toArray());

        final double[] source = { 3.0, 1.0, 2.0 };
        final DoubleList reversed = DoubleStream.of(source).reversed().toDoubleList();
        reversed.set(0, 100.0);
        reversed.add(7.0);
        final DoubleList rotated = DoubleStream.of(source).rotated(1).toDoubleList();
        rotated.set(0, 100.0);
        final DoubleList reverseSorted = DoubleStream.of(source).reverseSorted().toDoubleList();
        reverseSorted.set(0, 100.0);
        assertArrayEquals(new double[] { 3.0, 1.0, 2.0 }, source);
        assertArrayEquals(new double[] { 100.0, 1.0, 3.0, 7.0 }, reversed.toArray());

        final int[] distances = { Integer.MIN_VALUE, -7, -1, 0, 1, 2, 6, Integer.MAX_VALUE };
        final List<java.util.function.UnaryOperator<DoubleStream>> ops = new ArrayList<>();
        ops.add(DoubleStream::reversed);
        ops.add(DoubleStream::reverseSorted);

        for (final int distance : distances) {
            ops.add(s -> s.rotated(distance));
        }

        final double[] pool = { Double.NaN, -0.0, 0.0, 1.5, -2.5, Double.NEGATIVE_INFINITY, 3.0 };

        for (int len = 0; len <= 6; len++) {
            final int n = len;
            final double[] padded = new double[n + 2];

            for (int i = 0; i < padded.length; i++) {
                padded[i] = pool[(i * 5) % 7];
            }

            final double[] a = Arrays.copyOfRange(padded, 1, 1 + n);
            final List<java.util.function.Supplier<DoubleStream>> sources = List.of(() -> DoubleStream.of(a.clone()), () -> DoubleStream.of(padded, 1, 1 + n),
                    () -> DoubleStream.of(DoubleList.of(a.clone()).iterator()), () -> DoubleStream.of(a.clone()).parallel(2));

            for (final java.util.function.Supplier<DoubleStream> src : sources) {
                for (final java.util.function.UnaryOperator<DoubleStream> op : ops) {
                    assertArrayEquals(drainG123(op.apply(src.get()).iteratorEx()).toArray(), op.apply(src.get()).toDoubleList().toArray());

                    for (int consumed = 0; consumed <= n + 1; consumed++) {
                        final DoubleIterator expectedIter = op.apply(src.get()).iteratorEx();
                        final DoubleIterator iter = op.apply(src.get()).iteratorEx();

                        for (int i = 0; i < consumed && iter.hasNext(); i++) {
                            assertEquals(expectedIter.nextDouble(), iter.nextDouble());
                        }

                        final DoubleList rest = iter.toList();
                        assertArrayEquals(drainG123(expectedIter).toArray(), rest.toArray());
                        assertFalse(iter.hasNext());
                        assertTrue(iter.toList().isEmpty());
                        rest.add(7.0);
                        assertEquals(7.0, rest.get(rest.size() - 1));
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
            final DoubleIterator viaToList = brokenSourceOpG123(op);
            final DoubleIterator viaWalk = brokenSourceOpG123(op);

            assertEquals("EX java.lang.IllegalStateException", outcomeG123(viaToList::toList));
            assertEquals("EX java.lang.IllegalStateException", outcomeG123(() -> drainG123(viaWalk)));

            for (int retry = 0; retry < 3; retry++) {
                assertEquals(outcomeG123(() -> drainG123(viaWalk)), outcomeG123(viaToList::toList), "op=" + op + ", retry=" + retry);
            }
        }
    }
    // ---- deep review 2026-11-02b G123 end ----

    private static DoubleStream par(final double... a) {
        return DoubleStream.of(a).parallel(2);
    }

    private static DoubleStream iterStream(final double... a) {
        return DoubleStream.of(DoubleIterator.of(a));
    }

    @Test
    public void testRateLimitedParallel() {
        assertArrayEquals(new double[] { 1, 2, 3 }, par(1, 2, 3).rateLimited(RateLimiter.create(100000)).sorted().toArray());
    }

    @Test
    public void testDelayParallel() {
        assertArrayEquals(new double[] { 1, 2 }, par(1, 2).delay(Duration.ofMillis(1)).sorted().toArray());
    }

    @Test
    public void testDebounceEmptyIteratorNextThrows() {
        final DoubleIterator iter = DoubleStream.of(DoubleIterator.empty()).debounce(Duration.ofMillis(5)).iterator();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextDouble);
    }

    @Test
    public void testDistinctFlatMapArrayFlatmapToObjOnIteratorStream() {
        assertArrayEquals(new double[] { 1, 2, 3 }, iterStream(1, 2, 1, 3, 2).distinct().toArray());
        assertArrayEquals(new double[] { 1, 1, 2, 2 }, iterStream(1, 2).flatMapArray(x -> new double[] { x, x }).toArray());
        final List<String> list = iterStream(1, 2).<String> flatmapToObj(x -> Arrays.asList("a" + x, "b" + x)).toList();
        assertEquals(Arrays.asList("a1.0", "b1.0", "a2.0", "b2.0"), list);
    }

    @Test
    public void testMapMultiMapperFailureAndLargeExpansion() {
        final DoubleIterator iter = iterStream(1, 2).mapMulti((x, consumer) -> {
            consumer.accept(x);
            if (x == 2) {
                throw new IllegalStateException("boom");
            }
        }).iterator();
        assertTrue(iter.hasNext());
        assertEquals(1.0, iter.nextDouble());
        assertThrows(IllegalStateException.class, iter::hasNext);

        final long count = iterStream(1, 2).mapMulti((x, consumer) -> {
            for (int i = 0; i < 10300; i++) {
                consumer.accept(x);
            }
        }).count();
        assertEquals(20600, count);
    }

    @Test
    public void testScanInitNotIncluded() {
        assertArrayEquals(new double[] { 11, 13 }, iterStream(1, 2).scan(10, false, Double::sum).toArray());
    }

    @Test
    public void testCycledOneRoundHasNextAfterEnd() {
        final DoubleIterator iter = iterStream(1, 2).cycled(1).iterator();
        assertEquals(1.0, iter.nextDouble());
        assertEquals(2.0, iter.nextDouble());
        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeWithParallel() {
        final double[] result = par(1, 3).mergeWith(DoubleStream.of(2, 4), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .sequential()
                .toArray();
        assertEquals(4, result.length);
        assertEquals(10.0, Arrays.stream(result).sum());
    }

    @Test
    public void testSummaryStatisticsAndPercentilesParallelAndEmpty() {
        final Pair<DoubleSummaryStatistics, Optional<Map<Percentage, Double>>> p = par(3, 1, 2).summaryStatisticsAndPercentiles();
        assertEquals(3, p.left().getCount());
        assertEquals(1.0, p.left().getMin());
        assertTrue(p.right().isPresent());

        final Pair<DoubleSummaryStatistics, Optional<Map<Percentage, Double>>> e = iterStream().summaryStatisticsAndPercentiles();
        assertEquals(0, e.left().getCount());
        assertFalse(e.right().isPresent());
    }
}
