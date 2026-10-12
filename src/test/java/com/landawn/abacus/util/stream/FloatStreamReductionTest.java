package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;

/** FloatStream reduction contracts. */
public class FloatStreamReductionTest extends FloatStreamTestSupport {

    @Test
    public void testAcceptIfNotEmpty() {
        List<Float> collected = new ArrayList<>();
        createFloatStream(1.0f, 2.0f, 3.0f).acceptIfNotEmpty(s -> s.forEach(collected::add));
        assertEquals(3, collected.size());
    }

    @Test
    public void testAcceptIfNotEmpty_Empty() {
        List<Float> collected = new ArrayList<>();
        FloatStream.empty().acceptIfNotEmpty(s -> s.forEach(collected::add));
        assertTrue(collected.isEmpty());
    }

    // allMatch

    @Test
    @DisplayName("allMatch() should return true if all match")
    public void testAllMatch() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertTrue(stream.allMatch(x -> x > 0.0f));

        FloatStream stream2 = createFloatStream(1.0f, 2.0f, 3.0f);
        assertFalse(stream2.allMatch(x -> x > 2.0f));
    }

    @Test
    public void testAllMatch_Empty() {
        assertTrue(FloatStream.empty().allMatch(f -> false));
    }

    @Test
    public void testAllMatch_Test() {
        assertTrue(createFloatStream(1.0f, 2.0f, 3.0f).allMatch(f -> f > 0.0f));
        assertFalse(createFloatStream(1.0f, 2.0f, 3.0f).allMatch(f -> f > 1.0f));
    }

    // anyMatch

    @Test
    @DisplayName("anyMatch() should return true if any matches")
    public void testAnyMatch() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertTrue(stream.anyMatch(x -> x > 2.0f));
    }

    @Test
    public void testAnyMatch_Empty() {
        assertFalse(FloatStream.empty().anyMatch(f -> true));
    }

    @Test
    public void testAnyMatch_Test() {
        assertTrue(createFloatStream(1.0f, 2.0f, 3.0f).anyMatch(f -> f > 2.0f));
        assertFalse(createFloatStream(1.0f, 2.0f, 3.0f).anyMatch(f -> f > 5.0f));
    }

    @Test
    public void testApplyIfNotEmpty() {
        Optional<float[]> result = createFloatStream(1.0f, 2.0f, 3.0f).applyIfNotEmpty(s -> s.toArray());
        assertTrue(result.isPresent());
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result.get(), DELTA);
    }

    @Test
    public void testApplyIfNotEmpty_Empty() {
        Optional<float[]> result = FloatStream.empty().applyIfNotEmpty(s -> s.toArray());
        assertFalse(result.isPresent());
    }

    // average

    @Test
    @DisplayName("average() should return average")
    public void testAverage() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        OptionalDouble avg = stream.average();
        assertTrue(avg.isPresent());
        assertEquals(2.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testAverage_Empty_Test() {
        OptionalDouble result = FloatStream.empty().average();
        assertFalse(result.isPresent());
    }

    @Test
    public void testAverage_Test() {
        OptionalDouble result = createFloatStream(1.0f, 2.0f, 3.0f).average();
        assertTrue(result.isPresent());
        assertEquals(2.0, result.getAsDouble(), DELTA);
    }

    @Test
    @DisplayName("average() of empty stream should return empty")
    public void testAverageEmpty() {
        FloatStream stream = FloatStream.empty();
        OptionalDouble average = stream.average();
        assertFalse(average.isPresent());
    }

    @Test
    @DisplayName("count() should return element count")
    public void testCount() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertEquals(3, stream.count());
    }

    @Test
    public void testCount_Empty() {
        assertEquals(0, FloatStream.empty().count());
    }

    @Test
    @DisplayName("elementAt() should return element at index")
    public void testElementAt() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        OptionalFloat element = stream.elementAt(2);
        assertTrue(element.isPresent());
        assertEquals(3.0f, element.get(), 0.001);
    }

    @Test
    public void testElementAt_OutOfRange() {
        OptionalFloat result = createFloatStream(10.0f, 20.0f).elementAt(5);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("findFirst() of empty stream should return empty")
    public void testFindFirstEmpty() {
        FloatStream stream = FloatStream.empty();
        OptionalFloat first = stream.first();
        assertFalse(first.isPresent());
    }

    @Test
    @DisplayName("findAny() should return any element")
    public void testFindAny() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        OptionalFloat any = stream.first();
        assertTrue(any.isPresent());
        assertTrue(any.get() >= 1.0f && any.get() <= 3.0f);
    }

    // findAny

    @Test
    public void testFindAny_Empty() {
        OptionalFloat result = FloatStream.empty().findAny(f -> f > 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAny_NoArg() {
        OptionalFloat result = createFloatStream(10.0f, 20.0f, 30.0f).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_NoArg_Empty() {
        OptionalFloat result = FloatStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAny_WithPredicate_Test() {
        OptionalFloat result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f).findAny(f -> f > 2.0f);
        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("findAny() with predicate should find any matching")
    public void testFindAnyWithPredicate() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        OptionalFloat result = stream.findAny(x -> x > 2.0f);
        assertTrue(result.isPresent());
        assertTrue(result.get() > 2.0f);
    }

    // findFirst

    @Test
    public void testFindFirst_Empty() {
        OptionalFloat result = FloatStream.empty().findFirst(f -> f > 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirst_NoArg() {
        OptionalFloat result = createFloatStream(10.0f, 20.0f, 30.0f).findFirst();
        assertTrue(result.isPresent());
        assertEquals(10.0f, result.getAsFloat(), DELTA);
    }

    @Test
    public void testFindFirst_NoArg_Empty() {
        OptionalFloat result = FloatStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirst_WithPredicate_Test() {
        OptionalFloat result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f).findFirst(f -> f > 2.0f);
        assertTrue(result.isPresent());
        assertEquals(3.0f, result.getAsFloat(), DELTA);
    }

    @Test
    @DisplayName("findFirst() with predicate should find first matching")
    public void testFindFirstWithPredicate() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        OptionalFloat result = stream.findFirst(x -> x > 2.0f);
        assertTrue(result.isPresent());
        assertEquals(3.0f, result.get(), 0.001);
    }

    // findLast

    @Test
    public void testFindLast() {
        OptionalFloat result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).findLast(f -> f < 4.0f);
        assertTrue(result.isPresent());
        assertEquals(3.0f, result.get(), DELTA);
    }

    @Test
    public void testFindLast_Empty() {
        OptionalFloat result = FloatStream.empty().findLast(f -> f > 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLast_WithPredicate_Test() {
        OptionalFloat result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f).findLast(f -> f < 3.0f);
        assertTrue(result.isPresent());
        assertEquals(2.0f, result.getAsFloat(), DELTA);
    }

    @Test
    @DisplayName("findLast() with predicate should find last matching")
    public void testFindLastWithPredicate() {
        FloatStream stream = createFloatStream(1.0f, 3.0f, 2.0f, 4.0f);
        OptionalFloat result = stream.findLast(x -> x > 2.0f);
        assertTrue(result.isPresent());
        assertEquals(4.0f, result.get(), 0.001);
    }

    // first

    @Test
    public void testFindFirstLast() {
        assertEquals(1.0f, FloatStream.of(1.0f, 2.0f, 3.0f).first().get(), DELTA);
        assertEquals(3.0f, FloatStream.of(1.0f, 2.0f, 3.0f).last().get(), DELTA);
        assertFalse(FloatStream.empty().first().isPresent());
    }

    @Test
    @DisplayName("first() should return first element")
    public void testFirst() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        OptionalFloat first = stream.first();
        assertTrue(first.isPresent());
        assertEquals(1.0f, first.get(), 0.001);
    }

    @Test
    public void testFirst_Empty() {
        OptionalFloat result = FloatStream.empty().first();
        assertFalse(result.isPresent());
    }

    // forEach

    @Test
    @DisplayName("forEach() should execute action")
    public void testForEach() {
        List<Float> collected = new ArrayList<>();
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        stream.forEach(collected::add);
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), collected);
    }

    @Test
    public void testForeach() {
        List<Float> collected = new ArrayList<>();
        createFloatStream(1.0f, 2.0f, 3.0f).forEach(f -> collected.add(f));
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), collected);
    }

    @Test
    public void testForeach_Empty() {
        List<Float> collected = new ArrayList<>();
        FloatStream.empty().forEach(f -> collected.add(f));
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testForEach_Empty() {
        List<Float> collected = new ArrayList<>();
        FloatStream.empty().forEach(collected::add);
        assertTrue(collected.isEmpty());
    }

    // forEachIndexed

    @Test
    @DisplayName("forEachIndexed() should provide index")
    public void testForEachIndexed() {
        List<String> collected = new ArrayList<>();
        FloatStream stream = createFloatStream(10.0f, 20.0f, 30.0f);
        stream.forEachIndexed((index, value) -> collected.add(index + ":" + value));
        assertEquals(Arrays.asList("0:10.0", "1:20.0", "2:30.0"), collected);
    }

    @Test
    public void testForEachIndexed_Empty() {
        List<Float> collected = new ArrayList<>();
        FloatStream.empty().forEachIndexed((idx, val) -> collected.add(val));
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testForEachIndexed_Test() {
        Map<Integer, Float> indexToValue = new HashMap<>();
        createFloatStream(10.0f, 20.0f, 30.0f).forEachIndexed((index, value) -> indexToValue.put(index, value));
        assertEquals(3, indexToValue.size());
        assertEquals(10.0f, indexToValue.get(0), DELTA);
    }

    // kthLargest

    @Test
    @DisplayName("kthLargest() should return kth largest")
    public void testKthLargest() {
        FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f, 5.0f);
        OptionalFloat kth = stream.kthLargest(2);
        assertTrue(kth.isPresent());
        assertEquals(4.0f, kth.get(), 0.001);
    }

    @Test
    public void testKthLargest_Empty() {
        OptionalFloat result = FloatStream.empty().kthLargest(1);
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest_SingleElement() {
        OptionalFloat result = createFloatStream(5.0f).kthLargest(1);
        assertTrue(result.isPresent());
        assertEquals(5.0f, result.get(), DELTA);
    }

    @Test
    public void testKthLargest_Test() {
        OptionalFloat result = createFloatStream(5.0f, 3.0f, 1.0f, 4.0f, 2.0f).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4.0f, result.getAsFloat(), DELTA);
    }

    @Test
    @DisplayName("last() should return last element")
    public void testLast() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        OptionalFloat last = stream.last();
        assertTrue(last.isPresent());
        assertEquals(3.0f, last.get(), 0.001);
    }

    @Test
    public void testLast_Empty() {
        OptionalFloat result = FloatStream.empty().last();
        assertFalse(result.isPresent());
    }

    // max

    @Test
    @DisplayName("max() should return maximum")
    public void testMax() {
        FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f);
        OptionalFloat max = stream.max();
        assertTrue(max.isPresent());
        assertEquals(4.0f, max.get(), 0.001);
    }

    @Test
    public void testMax_Empty() {
        OptionalFloat result = FloatStream.empty().max();
        assertFalse(result.isPresent());
    }

    @Test
    public void testMax_Test() {
        OptionalFloat result = createFloatStream(3.0f, 1.0f, 2.0f).max();
        assertTrue(result.isPresent());
        assertEquals(3.0f, result.getAsFloat(), DELTA);
    }

    // min

    @Test
    @DisplayName("min() should return minimum")
    public void testMin() {
        FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f);
        OptionalFloat min = stream.min();
        assertTrue(min.isPresent());
        assertEquals(1.0f, min.get(), 0.001);
    }

    @Test
    public void testMin_Empty() {
        OptionalFloat result = FloatStream.empty().min();
        assertFalse(result.isPresent());
    }

    @Test
    public void testMin_Test() {
        OptionalFloat result = createFloatStream(3.0f, 1.0f, 2.0f).min();
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.getAsFloat(), DELTA);
    }

    @Test
    public void testMinMax() {
        assertTrue(FloatStream.of(3.1f, 1.2f, 5.3f).min().isPresent());
        assertEquals(1.2f, FloatStream.of(3.1f, 1.2f, 5.3f).min().get(), DELTA);
        assertTrue(FloatStream.of(3.1f, 1.2f, 5.3f).max().isPresent());
        assertEquals(5.3f, FloatStream.of(3.1f, 1.2f, 5.3f).max().get(), DELTA);
    }

    // noneMatch

    @Test
    public void testAnyAllNoneMatch() {
        assertTrue(FloatStream.of(1.0f, 2.0f, 3.0f).anyMatch(f -> f > 2.5f));
        assertFalse(FloatStream.of(1.0f, 2.0f, 3.0f).allMatch(f -> f > 2.5f));
        assertTrue(FloatStream.of(1.0f, 2.0f, 3.0f).noneMatch(f -> f > 5.0f));
    }

    @Test
    @DisplayName("noneMatch() should return true if none match")
    public void testNoneMatch() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertTrue(stream.noneMatch(x -> x > 5.0f));

        FloatStream stream2 = createFloatStream(1.0f, 2.0f, 3.0f);
        assertFalse(stream2.noneMatch(x -> x > 2.0f));
    }

    @Test
    public void testNoneMatch_Empty() {
        assertTrue(FloatStream.empty().noneMatch(f -> true));
    }

    @Test
    public void testNoneMatch_False() {
        assertFalse(createFloatStream(1.0f, 2.0f, 3.0f).noneMatch(f -> f > 2.0f));
    }

    @Test
    public void testNoneMatch_Test() {
        assertTrue(createFloatStream(1.0f, 2.0f, 3.0f).noneMatch(f -> f > 5.0f));
        assertFalse(createFloatStream(1.0f, 2.0f, 3.0f).noneMatch(f -> f > 2.0f));
    }

    @Test
    public void testNoneMatch_True() {
        assertTrue(createFloatStream(1.0f, 2.0f, 3.0f).noneMatch(f -> f > 10.0f));
    }

    @Test
    @DisplayName("percentiles() should return percentile map")
    public void testPercentiles() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        Optional<Map<Percentage, Float>> result = stream.percentiles();
        assertTrue(result.isPresent());
    }

    @Test
    public void testPercentiles_Empty() {
        Optional<Map<Percentage, Float>> result = FloatStream.empty().percentiles();
        assertFalse(result.isPresent());
    }

    // reduce

    @Test
    public void testReduce() {
        OptionalFloat result = FloatStream.of(1.0f, 2.0f, 3.0f).reduce((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(6.0f, result.get(), DELTA);
    }

    @Test
    public void testReduce_Empty() {
        OptionalFloat result = FloatStream.empty().reduce((a, b) -> a + b);
        assertFalse(result.isPresent());
    }

    @Test
    public void testReduce_Empty_Test() {
        OptionalFloat result = FloatStream.empty().reduce(Float::sum);
        assertFalse(result.isPresent());
    }

    @Test
    public void testReduce_SingleElement() {
        OptionalFloat result = createFloatStream(5.0f).reduce((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(5.0f, result.get(), DELTA);
    }

    @Test
    public void testReduce_WithIdentity_Test() {
        float result = createFloatStream(1.0f, 2.0f, 3.0f).reduce(0.0f, Float::sum);
        assertEquals(6.0f, result, DELTA);
    }

    @Test
    public void testReduce_WithoutIdentity_Test() {
        OptionalFloat result = createFloatStream(1.0f, 2.0f, 3.0f).reduce(Float::sum);
        assertTrue(result.isPresent());
        assertEquals(6.0f, result.getAsFloat(), DELTA);
    }

    @Test
    @DisplayName("reduce() of empty stream should return empty")
    public void testReduceEmpty() {
        FloatStream stream = FloatStream.empty();
        OptionalFloat result = stream.reduce(Float::sum);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("reduce() with identity should return value")
    public void testReduceWithIdentity() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertEquals(6.0f, stream.reduce(0.0f, Float::sum), 0.001);
    }

    @Test
    @DisplayName("reduce() without identity should return Optional")
    public void testReduceWithoutIdentity() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        OptionalFloat result = stream.reduce(Float::sum);
        assertTrue(result.isPresent());
        assertEquals(6.0f, result.get(), 0.001);
    }

    // sum

    @Test
    @DisplayName("sum() should return sum of elements")
    public void testSum() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertEquals(6.0, stream.sum(), 0.001);
    }

    @Test
    public void testSum_Empty() {
        assertEquals(0.0, FloatStream.empty().sum(), DELTA);
    }

    @Test
    public void testSum_Test() {
        double result = createFloatStream(1.0f, 2.0f, 3.0f).sum();
        assertEquals(6.0, result, DELTA);
    }

    // summaryStatistics

    @Test
    @DisplayName("summaryStatistics() should return summary statistics")
    public void testsummaryStatistics() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        FloatSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(5, stats.getCount());
        assertEquals(1.0f, stats.getMin(), 0.001);
        assertEquals(5.0f, stats.getMax(), 0.001);
        assertEquals(15.0, stats.getSum(), 0.001);
        assertEquals(3.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testSummaryStatistics_Empty() {
        FloatSummaryStatistics stats = FloatStream.empty().summaryStatistics();
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testsummaryStatistics_Test() {
        FloatSummaryStatistics stats = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).summaryStatistics();
        assertEquals(5, stats.getCount());
        assertEquals(1.0f, stats.getMin(), DELTA);
        assertEquals(5.0f, stats.getMax(), DELTA);
    }

    // summaryStatisticsAndPercentiles

    @Test
    @DisplayName("summaryStatisticsAndPercentiles() should return both")
    public void testsummaryStatisticsAndPercentiles() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        Pair<FloatSummaryStatistics, Optional<Map<Percentage, Float>>> result = stream.summaryStatisticsAndPercentiles();
        assertEquals(5, result.left().getCount());
        assertTrue(result.right().isPresent());
    }

    @Test
    public void testSummaryStatisticsAndPercentiles_Empty() {
        Pair<FloatSummaryStatistics, Optional<Map<Percentage, Float>>> result = FloatStream.empty().summaryStatisticsAndPercentiles();
        assertEquals(0, result.getLeft().getCount());
        assertFalse(result.getRight().isPresent());
    }

    @Test
    public void testsummaryStatisticsAndPercentiles_Test() {
        Pair<FloatSummaryStatistics, Optional<Map<Percentage, Float>>> result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
                .summaryStatisticsAndPercentiles();
        assertEquals(5, result.left().getCount());
        assertTrue(result.right().isPresent());
    }

}
