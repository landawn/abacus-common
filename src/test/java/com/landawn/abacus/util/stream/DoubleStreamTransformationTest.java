package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.DoubleBinaryOperator;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.IndexedDouble;
import com.landawn.abacus.util.u.OptionalDouble;

/** DoubleStream transformation contracts. */
public class DoubleStreamTransformationTest extends DoubleStreamTestSupport {

    // collapse

    @Test
    public void testCollapse() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 10.0, 11.0);
        List<DoubleList> result = stream.collapse((prev, next) -> next - prev < 2.0).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testCollapse_ListForm() {
        List<DoubleList> result = DoubleStream.of(1.0, 1.0, 2.0, 2.0, 3.0).collapse((a, b) -> a == b).toList();
        assertEquals(3, result.size());
        assertArrayEquals(new double[] { 1.0, 1.0 }, result.get(0).toArray(), 0.001);
        assertArrayEquals(new double[] { 2.0, 2.0 }, result.get(1).toArray(), 0.001);
        assertArrayEquals(new double[] { 3.0 }, result.get(2).toArray(), 0.001);
    }

    @Test
    public void testCollapse_WithMergeFunction2() {
        double[] result = DoubleStream.of(1.0, 1.0, 2.0, 2.0, 3.0).collapse((a, b) -> a == b, Double::sum).toArray();
        assertArrayEquals(new double[] { 2.0, 4.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testCollapse_WithTriPredicate2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 10.0, 11.0, 12.0).collapse((first, previous, current) -> current - first <= 2, Double::sum).toArray();
        assertArrayEquals(new double[] { 6.0, 33.0 }, result, 0.001);
    }

    @Test
    public void testCollapseNonCollapsible() {
        DoubleStream stream = DoubleStream.of(1.0, 10.0, 100.0);
        double[] result = stream.collapse((prev, next) -> false, (a, b) -> a + b).toArray();
        assertArrayEquals(new double[] { 1.0, 10.0, 100.0 }, result, 0.0001);
    }

    @Test
    public void testCollapseWithMerge() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 10.0, 11.0);
        double[] result = stream.collapse((prev, next) -> next - prev < 2.0, (a, b) -> a + b).toArray();
        assertEquals(2, result.length);
    }

    @Test
    public void testCollapseWithTriPredicate() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 10.0, 11.0);
        double[] result = stream.collapse((first, last, next) -> next - first < 5.0, (a, b) -> a + b).toArray();
        assertEquals(2, result.length);
    }

    // cycled

    @Test
    public void testCycled_HappyPath() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).cycled().limit(7).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0 }, result, 0.001);
    }

    @Test
    public void testCycled_OneRound() {
        double[] result = createDoubleStream(1.0, 2.0, 3.0).cycled(1).toArray();

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0);
    }

    @Test
    public void testCycled_WithRounds() {
        double[] result = DoubleStream.of(1.0, 2.0).cycled(3).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 1.0, 2.0, 1.0, 2.0 }, result, 0.001);
    }

    @Test
    public void testDifference_HappyPath() {
        Set<Double> other = new java.util.HashSet<>(Arrays.asList(2.0, 4.0));
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).difference(other).toArray();
        assertArrayEquals(new double[] { 1.0, 3.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testDistinct() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 2.0, 3.0, 1.0);
        double[] result = stream.distinct().toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void test_distinct() {
        {
            DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);

            double[] result = stream.sorted().distinct().toArray();
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.0001);
        }
        {
            DoubleStream stream = new ArrayDoubleStream(new double[] { 1.0, 2.0, 2.0, 3.0, 4.0, 5.0, 5.0 }, true, null);

            double[] result = stream.distinct().toArray();
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.0001);
        }
        {
            DoubleStream stream = new ArrayDoubleStream(new double[] { 1.0, 2.0, 2.0, 3.0, 4.0, 5.0, 5.0 }, true, null);

            double[] result = stream.filter(e -> true).distinct().toArray();
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.0001);
        }
    }

    @Test
    public void testDropWhile() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);
        double[] result = stream.dropWhile(x -> x < 3.0).toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, result, 0.0001);

        DoubleStream stream2 = DoubleStream.of(5.0, 6.0);
        assertEquals(2, stream2.dropWhile(x -> x < 1.0).count());
    }

    @Test
    public void testDropWhile_WithAction() {
        List<Double> dropped = new ArrayList<>();
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).dropWhile(x -> x <= 3.0, value -> dropped.add(value)).toArray();
        assertArrayEquals(new double[] { 4.0, 5.0 }, result, 0.001);
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), dropped);
    }

    // filter

    @Test
    public void testFilter() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);
        double[] result = stream.filter(x -> x > 2.5).toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, result, 0.0001);

        DoubleStream emptyStream = DoubleStream.empty();
        assertEquals(0, emptyStream.filter(x -> true).count());
    }

    @Test
    public void testFilter_WithAction() {
        List<Double> rejected = new ArrayList<>();
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).filter(x -> x % 2 == 0, value -> rejected.add(value)).toArray();
        assertArrayEquals(new double[] { 2.0, 4.0 }, result, 0.001);
        assertEquals(Arrays.asList(1.0, 3.0, 5.0), rejected);
    }

    @Test
    public void testFilterWithNull() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.of(1.0, 2.0).filter(null).count());
    }

    @Test
    public void testIndexed_HappyPath() {
        List<IndexedDouble> result = DoubleStream.of(10.0, 20.0, 30.0).indexed().toList();
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).index());
        assertEquals(10.0, result.get(0).value(), 0.001);
        assertEquals(1, result.get(1).index());
        assertEquals(20.0, result.get(1).value(), 0.001);
    }

    @Test
    public void testIntersection_HappyPath() {
        Set<Double> other = new java.util.HashSet<>(Arrays.asList(2.0, 4.0, 6.0));
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).intersection(other).toArray();
        assertArrayEquals(new double[] { 2.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testLimit() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);
        double[] result = stream.limit(3).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);
    }

    // onlyOne

    @Test
    public void testOnlyOne_EmptyStream() {
        OptionalDouble result = DoubleStream.empty().onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_HappyPath() {
        OptionalDouble result = DoubleStream.of(42.0).onlyOne();
        assertTrue(result.isPresent());
        assertEquals(42.0, result.get(), 0.001);
    }

    @Test
    public void testOnlyOne_TooManyElements() {
        assertThrows(Exception.class, () -> DoubleStream.of(1.0, 2.0).onlyOne());
    }

    @Test
    public void testPeek() {
        List<Double> peeked = new ArrayList<>();
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        double[] result = stream.peek(peeked::add).toArray();
        assertEquals(3, peeked.size());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);
    }

    @Test
    public void testPeek_HappyPath2() {
        List<Double> peeked = new ArrayList<>();
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).peek(peeked::add).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), peeked);
    }

    // reversed

    @Test
    public void testReversed() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        double[] result = stream.reversed().toArray();
        assertArrayEquals(new double[] { 3.0, 2.0, 1.0 }, result, 0.0001);
    }

    @Test
    public void testReversed_Empty() {
        double[] result = DoubleStream.empty().reversed().toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testReversed_HappyPath2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).reversed().toArray();
        assertArrayEquals(new double[] { 3.0, 2.0, 1.0 }, result, 0.001);
    }

    @Test
    public void testReverseSorted_Empty() {
        double[] result = DoubleStream.empty().reverseSorted().toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testReverseSorted_HappyPath() {
        double[] result = DoubleStream.of(3.0, 1.0, 4.0, 1.0, 5.0).reverseSorted().toArray();
        assertArrayEquals(new double[] { 5.0, 4.0, 3.0, 1.0, 1.0 }, result, 0.001);
    }

    // rotated

    @Test
    public void testRotated() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        double[] result = stream.rotated(2).toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 1.0, 2.0 }, result, 0.0001);
    }

    @Test
    public void testRotated_HappyPath() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).rotated(2).toArray();
        assertArrayEquals(new double[] { 4.0, 5.0, 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testRotated_NegativeDistance() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).rotated(-2).toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0, 1.0, 2.0 }, result, 0.001);
    }

    // scan

    @Test
    public void testScan() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        double[] result = stream.scan((a, b) -> a + b).toArray();
        assertArrayEquals(new double[] { 1.0, 3.0, 6.0, 10.0 }, result, 0.0001);
    }

    @Test
    public void testScan_BasicScan2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0).scan(Double::sum).toArray();
        assertArrayEquals(new double[] { 1.0, 3.0, 6.0, 10.0 }, result, 0.001);
    }

    @Test
    public void testScan_EmptyStream2() {
        double[] result = DoubleStream.empty().scan(Double::sum).toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testScan_WithInit2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).scan(10.0, Double::sum).toArray();
        assertArrayEquals(new double[] { 11.0, 13.0, 16.0 }, result, 0.001);
    }

    @Test
    public void testScan_WithInitIncluded2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).scan(10.0, true, Double::sum).toArray();
        assertArrayEquals(new double[] { 10.0, 11.0, 13.0, 16.0 }, result, 0.001);
    }

    @Test
    public void testScanEmptyStream() {
        DoubleStream stream = DoubleStream.empty();
        double[] result = stream.scan((a, b) -> a + b).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testScanWithInit() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        double[] result = stream.scan(10.0, (a, b) -> a + b).toArray();
        assertArrayEquals(new double[] { 11.0, 13.0, 16.0 }, result, 0.0001);
    }

    @Test
    public void testScanWithInitial() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        DoubleBinaryOperator accumulator = (a, b) -> a * b;

        double[] result = stream.scan(10.0, accumulator).toArray();
        assertArrayEquals(new double[] { 10.0, 20.0, 60.0 }, result, 0.0001);
    }

    @Test
    public void testScanWithInitialIncluded() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        DoubleBinaryOperator accumulator = Double::sum;

        double[] result = stream.scan(100.0, true, accumulator).toArray();
        assertArrayEquals(new double[] { 100.0, 101.0, 103.0, 106.0 }, result, 0.0001);
    }

    @Test
    public void testScanWithInitIncluded() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        double[] result = stream.scan(10.0, true, (a, b) -> a + b).toArray();
        assertEquals(4, result.length);
        assertEquals(10.0, result[0], 0.0001);
    }

    // shuffled

    @Test
    public void testShuffled() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);
        double[] result = stream.shuffled().toArray();
        assertEquals(5, result.length);
        assertTrue(Arrays.stream(result).anyMatch(x -> Math.abs(x - 1.0) < 0.0001));
    }

    @Test
    public void testShuffled_HappyPath() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).shuffled().toArray();
        assertEquals(5, result.length);
        Arrays.sort(result);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testShuffled_WithRandom() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).shuffled(new Random(42)).toArray();
        assertEquals(5, result.length);
        Arrays.sort(result);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testSkip() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);
        double[] result = stream.skip(2).toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, result, 0.0001);
    }

    @Test
    public void testSkip_WithAction() {
        List<Double> skipped = new ArrayList<>();
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).skip(2, value -> skipped.add(value)).toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, result, 0.001);
        assertEquals(Arrays.asList(1.0, 2.0), skipped);
    }

    @Test
    public void testSkipAndLimit_WithOffset() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).skipAndLimit(1, 3).toArray();
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testSkipAndLimit_WithOffsetBeyondSize() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).skipAndLimit(5, 3).toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    // skipUntil

    @Test
    public void testSkipUntil() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);
        double[] result = stream.skipUntil(x -> x > 2.5).toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, result, 0.0001);
    }

    @Test
    public void testSkipUntil_HappyPath() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).skipUntil(x -> x >= 3.0).toArray();
        assertArrayEquals(new double[] { 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testSkipUntil_NeverMatches() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).skipUntil(x -> x > 10.0).toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testSorted() {
        DoubleStream stream = DoubleStream.of(3.0, 1.0, 4.0, 2.0);
        double[] result = stream.sorted().toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.0001);
    }

    @Test
    public void testStep_HappyPath() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).step(2).toArray();
        assertArrayEquals(new double[] { 1.0, 3.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testStep_StepThree() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0).step(3).toArray();
        assertArrayEquals(new double[] { 1.0, 4.0, 7.0 }, result, 0.001);
    }

    @Test
    public void testSymmetricDifference_HappyPath() {
        List<Double> other = Arrays.asList(2.0, 4.0, 6.0);
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).symmetricDifference(other).sorted().toArray();
        assertArrayEquals(new double[] { 1.0, 3.0, 5.0, 6.0 }, result, 0.001);
    }

    @Test
    public void testTakeWhile() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 2.0);
        double[] result = stream.takeWhile(x -> x < 4.0).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);

        DoubleStream stream2 = DoubleStream.of(1.0, 2.0, 3.0);
        assertEquals(3, stream2.takeWhile(x -> x < 10.0).count());
    }

    @Test
    public void test_takeWhile() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0, 5.0);

        double[] result = stream.takeWhile(x -> x < 4.0).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);
    }

    @Test
    public void testTop() {
        DoubleStream stream = DoubleStream.of(5.0, 1.0, 3.0, 4.0, 2.0);
        double[] result = stream.top(3).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testTopWithComparator() {
        DoubleStream stream = DoubleStream.of(5.0, 1.0, 3.0, 4.0, 2.0);
        double[] result = stream.top(3, (a, b) -> Double.compare(b, a)).toArray();
        assertEquals(3, result.length);
    }

}
