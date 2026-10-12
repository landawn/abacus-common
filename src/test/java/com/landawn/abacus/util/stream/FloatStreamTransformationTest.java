package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.IndexedFloat;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.u.OptionalFloat;

/** FloatStream transformation contracts. */
public class FloatStreamTransformationTest extends FloatStreamTestSupport {

    // collapse

    @Test
    @DisplayName("collapse() should group consecutive elements")
    public void testCollapse() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 5.0f, 6.0f, 7.0f);
        Stream<FloatList> result = stream.collapse((a, b) -> Math.abs(b - a) <= 1.5f);
        List<FloatList> groups = result.toList();
        assertTrue(groups.size() >= 2);
    }

    @Test
    public void testCollapse_Empty() {
        List<FloatList> result = FloatStream.empty().collapse((a, b) -> b - a < 2).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollapse_SingleElement() {
        List<FloatList> result = createFloatStream(5.0f).collapse((a, b) -> b - a < 2).toList();
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).size());
    }

    @Test
    @DisplayName("collapse() with no collapsible elements additional")
    public void testCollapseNoCollapsibleAdditional() {
        FloatStream stream = FloatStream.of(1.0f, 10.0f, 20.0f, 30.0f);
        float[] result = stream.collapse((a, b) -> Math.abs(b - a) <= 2.0f, Float::sum).toArray();
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("collapse() with merge function")
    public void testCollapseWithMerge() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 5.0f, 6.0f, 7.0f);
        float[] result = stream.collapse((a, b) -> Math.abs(b - a) <= 1.5f, Float::sum).toArray();
        assertTrue(result.length >= 2);
    }

    @Test
    @DisplayName("collapse() with tri-predicate and merge function")
    public void testCollapseWithTriPredicate() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 5.0f, 6.0f, 7.0f);
        float[] result = stream.collapse((first, last, next) -> Math.abs(next - first) <= 3.0f, Float::sum).toArray();
        assertTrue(result.length > 0);
    }

    // cycled

    @Test
    @DisplayName("cycled() should repeat stream indefinitely")
    public void testCycled() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 1.0f, 2.0f, 1.0f, 2.0f }, stream.cycled().limit(6).toArray());
    }

    @Test
    public void testCycled_Empty() {
        float[] result = FloatStream.empty().cycled().limit(0).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("cycled() with rounds should repeat specific times")
    public void testCycledWithRounds() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 1.0f, 2.0f, 1.0f, 2.0f }, stream.cycled(3).toArray());
    }

    @Test
    public void testCycledWithRounds_Zero() {
        float[] result = createFloatStream(1.0f, 2.0f).cycled(0).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("difference() should return elements not in other")
    public void testDifference() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        Collection<Float> other = Arrays.asList(2.0f, 3.0f, 5.0f);
        float[] result = stream.difference(other).toArray();
        Arrays.sort(result);
        assertArrayEquals(new float[] { 1.0f, 4.0f }, result);
    }

    @Test
    public void testDifference_Empty() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).difference(java.util.Collections.emptyList()).toArray();
        assertEquals(3, result.length);
    }

    // distinct

    @Test
    @DisplayName("distinct() should remove duplicates")
    public void testDistinct() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 2.0f, 3.0f, 1.0f);
        float[] result = stream.distinct().toArray();
        Arrays.sort(result);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
    }

    @Test
    public void testDistinct_AllSame() {
        float[] result = createFloatStream(5.0f, 5.0f, 5.0f).distinct().toArray();
        assertArrayEquals(new float[] { 5.0f }, result, DELTA);
    }

    @Test
    public void testDistinct_Empty() {
        float[] result = FloatStream.empty().distinct().toArray();
        assertEquals(0, result.length);
    }

    // dropWhile

    @Test
    @DisplayName("dropWhile() should drop elements while predicate is true")
    public void testDropWhile() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 2.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f, 2.0f }, stream.dropWhile(x -> x < 3.0f).toArray());
    }

    @Test
    public void testDropWhile_AllMatch() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).dropWhile(f -> f < 10.0f).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testDropWhile_NoneMatch() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).dropWhile(f -> f > 10.0f).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    @DisplayName("dropWhile() with action on dropped items")
    public void testDropWhileWithAction() {
        List<Float> dropped = new ArrayList<>();
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f }, stream.dropWhile(x -> x < 3.0f, dropped::add).toArray());
        assertEquals(Arrays.asList(1.0f, 2.0f), dropped);
    }

    @Test
    public void testDropWhileWithActionOnDroppedItem() {
        final FloatList droppedItems = new FloatList();
        FloatStream.of(1f, 2f, 3f, 4f, 1f).dropWhile(f -> f < 4f, droppedItems::add).count();

        assertArrayEquals(new float[] { 1f, 2f, 3f }, droppedItems.toArray(), DELTA);
    }

    // filter

    @Test
    @DisplayName("filter() should filter elements")
    public void testFilter() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f }, stream.filter(x -> x > 2.0f).toArray());
    }

    @Test
    public void testFilter_Empty() {
        float[] result = FloatStream.empty().filter(f -> f > 0).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("filter() with action on dropped items")
    public void testFilterWithAction() {
        List<Float> dropped = new ArrayList<>();
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f }, stream.filter(x -> x > 2.0f, dropped::add).toArray());
        assertEquals(Arrays.asList(1.0f, 2.0f), dropped);
    }

    @Test
    public void testFilterWithActionOnDroppedItem() {
        final FloatList droppedItems = new FloatList();
        FloatPredicate predicate = f -> f > 3f;
        FloatStream.of(1f, 2f, 3f, 4f, 5f).filter(predicate, droppedItems::add).count();

        assertArrayEquals(new float[] { 1f, 2f, 3f }, droppedItems.toArray(), DELTA);
    }

    @Test
    @DisplayName("indexed() should pair elements with indices")
    public void testIndexed() {
        FloatStream stream = createFloatStream(10.0f, 20.0f, 30.0f);
        List<IndexedFloat> result = stream.indexed().toList();
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).longIndex());
        assertEquals(10.0f, result.get(0).value(), 0.001);
    }

    @Test
    public void testIndexed_Empty() {
        List<IndexedFloat> result = FloatStream.empty().indexed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("intersection() should return common elements")
    public void testIntersection() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        Collection<Float> other = Arrays.asList(2.0f, 3.0f, 5.0f);
        float[] result = stream.intersection(other).toArray();
        Arrays.sort(result);
        assertArrayEquals(new float[] { 2.0f, 3.0f }, result);
    }

    @Test
    public void testIntersection_Empty() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).intersection(java.util.Collections.emptyList()).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("limit() should limit stream size")
    public void testLimit() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.limit(3).toArray());
    }

    @Test
    @DisplayName("onEach() should perform action on each element")
    public void testOnEach() {
        List<Float> collected = new ArrayList<>();
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.peek(collected::add).toArray());
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), collected);
    }

    // onlyOne

    @Test
    @DisplayName("onlyOne() should return single element")
    public void testOnlyOne() {
        FloatStream stream = createFloatStream(5.0f);
        OptionalFloat only = stream.onlyOne();
        assertTrue(only.isPresent());
        assertEquals(5.0f, only.get(), 0.001);
    }

    @Test
    public void testOnlyOne_Empty() {
        OptionalFloat result = FloatStream.empty().onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_MultipleElements() {
        assertThrows(Exception.class, () -> createFloatStream(1.0f, 2.0f).onlyOne());
    }

    @Test
    @DisplayName("onlyOne() with multiple elements should throw")
    public void testOnlyOneMultiple() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertThrows(Exception.class, stream::onlyOne);
    }

    // reversed

    @Test
    @DisplayName("reversed() should reverse order")
    public void testReversed() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        assertArrayEquals(new float[] { 4.0f, 3.0f, 2.0f, 1.0f }, stream.reversed().toArray());
    }

    @Test
    public void testReversed_Empty() {
        float[] result = FloatStream.empty().reversed().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testReversed_SingleElement() {
        float[] result = createFloatStream(5.0f).reversed().toArray();
        assertArrayEquals(new float[] { 5.0f }, result, DELTA);
    }

    @Test
    @DisplayName("reverseSorted() should sort in reverse")
    public void testReverseSorted() {
        FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f);
        assertArrayEquals(new float[] { 4.0f, 3.0f, 2.0f, 1.0f }, stream.reverseSorted().toArray());
    }

    @Test
    public void testReverseSorted_Empty() {
        float[] result = FloatStream.empty().reverseSorted().toArray();
        assertEquals(0, result.length);
    }

    // rotated

    @Test
    @DisplayName("rotated() should rotate elements")
    public void testRotated() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f, 1.0f, 2.0f }, stream.rotated(2).toArray());
    }

    @Test
    public void testRotated_Negative() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).rotated(-1).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testRotated_Zero() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).rotated(0).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    // scan

    @Test
    @DisplayName("scan() should return cumulative results")
    public void testScan() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 6.0f }, stream.scan(Float::sum).toArray());
    }

    @Test
    public void testScan_Empty() {
        float[] result = FloatStream.empty().scan((a, b) -> a + b).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("scan() on empty stream additional")
    public void testScanEmptyAdditional() {
        FloatStream emptyStream = FloatStream.empty();
        float[] result = emptyStream.scan(Float::sum).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("scan() with init should include initial value")
    public void testScanWithInit() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new float[] { 11.0f, 13.0f, 16.0f }, stream.scan(10.0f, Float::sum).toArray());
    }

    @Test
    public void testScanWithInit_Empty() {
        float[] result = FloatStream.empty().scan(10.0f, (a, b) -> a + b).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("scan() with init and initIncluded flag")
    public void testScanWithInitIncluded() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        float[] result = stream.scan(10.0f, true, Float::sum).toArray();
        assertEquals(4, result.length);
        assertEquals(10.0f, result[0], 0.001);
    }

    @Test
    public void testScanWithInitIncluded_Empty() {
        float[] result = FloatStream.empty().scan(10.0f, true, (a, b) -> a + b).toArray();
        assertArrayEquals(new float[] { 10.0f }, result, DELTA);
    }

    // shuffled

    @Test
    @DisplayName("shuffled() should shuffle elements")
    public void testShuffled() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        float[] result = stream.shuffled().toArray();
        assertEquals(4, result.length);
        Arrays.sort(result);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result);
    }

    @Test
    public void testShuffled_Empty() {
        float[] result = FloatStream.empty().shuffled().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testShuffledWithRandom() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).shuffled(new Random(42)).toArray();
        assertEquals(5, result.length);
    }

    // skip

    @Test
    @DisplayName("skip() should skip elements")
    public void testSkip() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f }, stream.skip(2).toArray());
    }

    @Test
    public void testSkipAndLimit() {
        assertArrayEquals(new float[] { 3.0f, 4.0f }, FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).skip(2).limit(2).toArray(), DELTA);
    }

    @Test
    @DisplayName("skip() with action on skipped items")
    public void testSkipWithAction() {
        List<Float> skipped = new ArrayList<>();
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f }, stream.skip(2, skipped::add).toArray());
        assertEquals(Arrays.asList(1.0f, 2.0f), skipped);
    }

    // skipAndLimit

    @Test
    public void testSkipAndLimit_OffsetBeyondSize() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).skipAndLimit(10, 5).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testSkipAndLimit_WithOffset() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).skipAndLimit(2, 2).toArray();
        assertArrayEquals(new float[] { 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    public void testSkipAndLimit_ZeroOffset() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).skipAndLimit(0, 3).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    @DisplayName("skipUntil() should skip elements until predicate is true")
    public void testSkipUntil() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f }, stream.skipUntil(x -> x >= 3.0f).toArray());
    }

    // sorted

    @Test
    @DisplayName("sorted() should sort elements")
    public void testSorted() {
        FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.sorted().toArray());
    }

    @Test
    public void testSorted_AlreadySorted() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).sorted().toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testSorted_Empty() {
        float[] result = FloatStream.empty().sorted().toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("step() should take every nth element")
    public void testStep() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 5.0f }, stream.step(2).toArray());
    }

    @Test
    public void testStep_StepOne() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).step(1).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    @DisplayName("symmetricDifference() should return elements in either but not both")
    public void testSymmetricDifference() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        Collection<Float> other = Arrays.asList(2.0f, 3.0f, 4.0f, 5.0f);
        float[] result = stream.symmetricDifference(other).toArray();
        Arrays.sort(result);
        assertArrayEquals(new float[] { 1.0f, 4.0f, 5.0f }, result);
    }

    @Test
    public void testSymmetricDifference_Empty() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).symmetricDifference(java.util.Collections.emptyList()).toArray();
        assertEquals(3, result.length);
    }

    // takeWhile

    @Test
    @DisplayName("takeWhile() should take elements while predicate is true")
    public void testTakeWhile() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 2.0f, 1.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f }, stream.takeWhile(x -> x < 3.0f).toArray());
    }

    @Test
    public void testTakeWhile_AllMatch() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).takeWhile(f -> f < 10.0f).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testTakeWhile_NoneMatch() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).takeWhile(f -> f < 0.0f).toArray();
        assertEquals(0, result.length);
    }

    // top

    @Test
    @DisplayName("top() should return top n elements")
    public void testTop() {
        FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f, 5.0f);
        float[] result = stream.top(3).toArray();
        Arrays.sort(result);
        assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f }, result);
    }

    @Test
    public void testTop_Empty() {
        float[] result = FloatStream.empty().top(3).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testTop_MoreThanAvailable() {
        float[] result = createFloatStream(1.0f, 2.0f).top(5).toArray();
        assertEquals(2, result.length);
    }

    @Test
    @DisplayName("top() with comparator should return top n elements")
    public void testTopWithComparator() {
        FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f, 5.0f);
        float[] result = stream.top(3, (a, b) -> Float.compare(b, a)).toArray();
        Arrays.sort(result);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
    }

}
