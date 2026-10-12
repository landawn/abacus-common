package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.IndexedInt;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.u.OptionalInt;

/** IntStream transformation contracts. */
public class IntStreamTransformationTest extends IntStreamTestSupport {

    // collapse

    @Test
    public void testCollapse() {
        Stream<IntList> collapsed = createIntStream(1, 1, 2, 2, 2, 3, 3).collapse((a, b) -> a == b);
        List<IntList> result = collapsed.toList();

        assertEquals(3, result.size());
        assertArrayEquals(new int[] { 1, 1 }, result.get(0).toArray());
        assertArrayEquals(new int[] { 2, 2, 2 }, result.get(1).toArray());
        assertArrayEquals(new int[] { 3, 3 }, result.get(2).toArray());
    }

    @Test
    public void testCollapse_ListForm() {
        List<IntList> result = IntStream.of(1, 1, 2, 2, 3).collapse((a, b) -> a == b).toList();
        assertEquals(3, result.size());
        assertArrayEquals(new int[] { 1, 1 }, result.get(0).toArray());
        assertArrayEquals(new int[] { 2, 2 }, result.get(1).toArray());
        assertArrayEquals(new int[] { 3 }, result.get(2).toArray());
    }

    @Test
    public void testCollapse_WithMergeFunction() {
        stream = IntStream.of(1, 1, 2, 2, 3, 3).collapse((a, b) -> a == b, (a, b) -> a + b);
        assertArrayEquals(new int[] { 2, 4, 6 }, stream.toArray());
    }

    @Test
    public void testCollapse_WithTriPredicate() {
        int[] result = IntStream.of(1, 2, 3, 10, 11, 12).collapse((first, previous, current) -> current - first <= 2, Integer::sum).toArray();
        assertArrayEquals(new int[] { 6, 33 }, result);
    }

    @Test
    public void testCollapseNonCollapsible() {
        stream = IntStream.of(1, 10, 100);
        int[] result = stream.collapse((prev, next) -> false, (a, b) -> a + b).toArray();
        assertArrayEquals(new int[] { 1, 10, 100 }, result);
    }

    @Test
    public void testCollapseWithMergeFunction() {
        IntStream stream = createIntStream(1, 1, 2, 2, 2, 3, 3).collapse((a, b) -> a == b, Integer::sum);
        assertArrayEquals(new int[] { 2, 6, 6 }, stream.toArray());
    }

    @Test
    public void testCollapseWithTriPredicate() {
        IntStream stream = createIntStream(1, 2, 3, 5, 6, 8, 9).collapse((first, prev, curr) -> curr - first <= 2, Integer::max);
        assertArrayEquals(new int[] { 3, 6, 9 }, stream.toArray());
    }

    // cycled

    @Test
    public void testCycled() {
        stream = createIntStream(1, 2, 3).cycled(2);
        assertArrayEquals(new int[] { 1, 2, 3, 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testCycled_HappyPath() {
        int[] result = IntStream.of(1, 2, 3).cycled().limit(7).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 1, 2, 3, 1 }, result);
    }

    @Test
    public void testCycled_WithRounds() {
        int[] result = IntStream.of(1, 2).cycled(3).toArray();
        assertArrayEquals(new int[] { 1, 2, 1, 2, 1, 2 }, result);
    }

    @Test
    public void testDifference() {
        stream = createIntStream(1, 2, 3, 4, 5).difference(Arrays.asList(3, 4));
        assertArrayEquals(new int[] { 1, 2, 5 }, stream.toArray());
    }

    @Test
    public void testDifference_HappyPath() {
        Set<Integer> other = new HashSet<>(Arrays.asList(2, 4));
        int[] result = IntStream.of(1, 2, 3, 4, 5).difference(other).toArray();
        assertArrayEquals(new int[] { 1, 3, 5 }, result);
    }

    // distinct

    @Test
    public void testDistinct() {
        stream = createIntStream(1, 2, 2, 3, 3, 3, 4).distinct();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testDistinct_EmptyStream() {
        stream = IntStream.empty().distinct();
        assertEquals(0, stream.count());
    }

    @Test
    public void testDistinct_NoDuplicates() {
        stream = IntStream.of(1, 2, 3, 4).distinct();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testDistinct_RemovesDuplicates() {
        stream = IntStream.of(1, 2, 2, 3, 3, 3, 4).distinct();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void distinct_shouldReturnStreamWithUniqueElements() {
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStream.of(1, 2, 2, 3, 1).distinct().toArray());
    }

    // dropWhile

    @Test
    public void testDropWhile() {
        stream = createIntStream(1, 2, 3, 4, 5, 6).dropWhile(n -> n < 4);
        assertArrayEquals(new int[] { 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testDropWhile_AllElements() {
        stream = IntStream.of(5, 6, 7).dropWhile(x -> x < 5);
        assertArrayEquals(new int[] { 5, 6, 7 }, stream.toArray());
    }

    @Test
    public void testDropWhile_BasicDrop() {
        stream = IntStream.of(1, 2, 3, 4, 5).dropWhile(x -> x < 3);
        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testDropWhile_EmptyResult() {
        stream = IntStream.of(1, 2, 3).dropWhile(x -> x < 10);
        assertEquals(0, stream.count());
    }

    @Test
    public void testDropWhile_WithAction() {
        List<Integer> dropped = new ArrayList<>();
        int[] result = IntStream.of(1, 2, 3, 4, 5).dropWhile(x -> x <= 3, value -> dropped.add(value)).toArray();
        assertArrayEquals(new int[] { 4, 5 }, result);
        assertEquals(Arrays.asList(1, 2, 3), dropped);
    }

    @Test
    public void testDropWhileWithAction() {
        List<Integer> dropped = new ArrayList<>();
        IntStream stream = createIntStream(1, 2, 3, 4, 5).dropWhile(n -> n < 3, dropped::add);

        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
        assertEquals(Arrays.asList(1, 2), dropped);
    }

    // filter

    @Test
    public void testFilter() {
        stream = createIntStream(1, 2, 3, 4, 5, 6).filter(n -> n % 2 == 0);
        assertArrayEquals(new int[] { 2, 4, 6 }, stream.toArray());
    }

    @Test
    public void testFilter_AllMatch() {
        stream = IntStream.of(2, 4, 6).filter(x -> x % 2 == 0);
        assertArrayEquals(new int[] { 2, 4, 6 }, stream.toArray());
    }

    @Test
    public void testFilter_BasicFiltering() {
        stream = IntStream.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0);
        assertArrayEquals(new int[] { 2, 4 }, stream.toArray());
    }

    @Test
    public void testFilter_EmptyResult() {
        stream = IntStream.of(1, 3, 5).filter(x -> x % 2 == 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void filter_shouldFilterElements() {
        assertArrayEquals(new int[] { 2, 4 }, IntStream.of(1, 2, 3, 4, 5).filter(i -> i % 2 == 0).toArray());
    }

    @Test
    public void testFilter_WithAction() {
        List<Integer> rejected = new ArrayList<>();
        int[] result = IntStream.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0, value -> rejected.add(value)).toArray();
        assertArrayEquals(new int[] { 2, 4 }, result);
        assertEquals(Arrays.asList(1, 3, 5), rejected);
    }

    @Test
    public void testFilterWithAction() {
        List<Integer> dropped = new ArrayList<>();
        IntStream stream = createIntStream(1, 2, 3, 4, 5).filter(n -> n % 2 == 0, dropped::add);

        assertArrayEquals(new int[] { 2, 4 }, stream.toArray());
        assertEquals(Arrays.asList(1, 3, 5), dropped);
    }

    @Test
    public void testIndexed() {
        Stream<IndexedInt> indexed = createIntStream(10, 20, 30).indexed();
        List<IndexedInt> list = indexed.toList();

        assertEquals(3, list.size());
        assertEquals(10, list.get(0).value());
        assertEquals(0, list.get(0).index());
        assertEquals(20, list.get(1).value());
        assertEquals(1, list.get(1).index());
    }

    @Test
    public void testIndexed_HappyPath() {
        List<IndexedInt> result = IntStream.of(10, 20, 30).indexed().toList();
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).index());
        assertEquals(10, result.get(0).value());
        assertEquals(1, result.get(1).index());
        assertEquals(20, result.get(1).value());
        assertEquals(2, result.get(2).index());
        assertEquals(30, result.get(2).value());
    }

    @Test
    public void testIntersection() {
        stream = createIntStream(1, 2, 3, 4, 5).intersection(Arrays.asList(3, 4, 5, 6, 7));
        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testIntersection_HappyPath() {
        Set<Integer> other = new HashSet<>(Arrays.asList(2, 4, 6));
        int[] result = IntStream.of(1, 2, 3, 4, 5).intersection(other).toArray();
        assertArrayEquals(new int[] { 2, 4 }, result);
    }

    // limit

    @Test
    public void testLimit() {
        stream = createIntStream(1, 2, 3, 4, 5).limit(3);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testLimit_LimitElements() {
        stream = IntStream.of(1, 2, 3, 4, 5).limit(3);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testLimit_MoreThanAvailable() {
        stream = IntStream.of(1, 2, 3).limit(10);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testLimit_ZeroElements() {
        stream = IntStream.of(1, 2, 3).limit(0);
        assertEquals(0, stream.count());
    }

    // onlyOne

    @Test
    public void testOnlyOne() {
        OptionalInt only = createIntStream(42).onlyOne();
        assertTrue(only.isPresent());
        assertEquals(42, only.getAsInt());

        assertFalse(IntStream.empty().onlyOne().isPresent());
    }

    @Test
    public void testOnlyOne_EmptyStream2() {
        OptionalInt result = IntStream.empty().onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_SingleElement() {
        OptionalInt result = IntStream.of(42).onlyOne();
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    public void testOnlyOne_TooManyElements() {
        assertThrows(TooManyElementsException.class, () -> IntStream.of(1, 2).onlyOne());
    }

    @Test
    public void testOnlyOneWithMultipleElements() {
        assertThrows(TooManyElementsException.class, () -> createIntStream(1, 2).onlyOne());
    }

    // peek

    @Test
    public void testPeek() {
        List<Integer> sideEffect = new ArrayList<>();
        stream = createIntStream(1, 2, 3).peek(sideEffect::add);
        stream.toArray();
        assertEquals(Arrays.asList(1, 2, 3), sideEffect);
    }

    @Test
    public void testPeek_ExecutesAction() {
        AtomicInteger sum = new AtomicInteger(0);
        stream = IntStream.of(1, 2, 3, 4, 5).peek(sum::addAndGet);
        stream.toArray();
        assertEquals(15, sum.get());
    }

    @Test
    public void testPeek_HappyPath() {
        List<Integer> peeked = new ArrayList<>();
        int[] result = IntStream.of(1, 2, 3).peek(peeked::add).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
        assertEquals(Arrays.asList(1, 2, 3), peeked);
    }

    // reversed

    @Test
    public void testReversed() {
        stream = createIntStream(1, 2, 3, 4, 5).reversed();
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, stream.toArray());
    }

    @Test
    public void testReversed_BasicReverse() {
        stream = IntStream.of(1, 2, 3, 4, 5).reversed();
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, stream.toArray());
    }

    @Test
    public void testReversed_Empty() {
        int[] result = IntStream.empty().reversed().toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testReversed_EmptyStream() {
        stream = IntStream.empty().reversed();
        assertEquals(0, stream.count());
    }

    @Test
    public void testReversed_HappyPath() {
        int[] result = IntStream.of(1, 2, 3).reversed().toArray();
        assertArrayEquals(new int[] { 3, 2, 1 }, result);
    }

    @Test
    public void reversed_shouldReturnReversedStream() {
        assertArrayEquals(new int[] { 3, 2, 1 }, IntStream.of(1, 2, 3).reversed().toArray());
    }

    @Test
    public void testReversed_SingleElement() {
        stream = IntStream.of(42).reversed();
        assertArrayEquals(new int[] { 42 }, stream.toArray());
    }

    @Test
    public void testReverseSorted() {
        stream = createIntStream(3, 1, 4, 1, 5, 9, 2, 6).reverseSorted();
        assertArrayEquals(new int[] { 9, 6, 5, 4, 3, 2, 1, 1 }, stream.toArray());
    }

    @Test
    public void testReverseSorted_Empty() {
        int[] result = IntStream.empty().reverseSorted().toArray();
        assertArrayEquals(new int[0], result);
    }

    // rotated

    @Test
    public void testRotated() {
        stream = createIntStream(1, 2, 3, 4, 5).rotated(2);
        assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testRotated_HappyPath() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).rotated(2).toArray();
        assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, result);
    }

    @Test
    public void testRotated_NegativeDistance() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).rotated(-2).toArray();
        assertArrayEquals(new int[] { 3, 4, 5, 1, 2 }, result);
    }

    // scan

    @Test
    public void testScan() {
        stream = createIntStream(1, 2, 3, 4).scan(Integer::sum);
        assertArrayEquals(new int[] { 1, 3, 6, 10 }, stream.toArray());

        stream = createIntStream(1, 2, 3, 4).scan(0, Integer::sum);
        assertArrayEquals(new int[] { 1, 3, 6, 10 }, stream.toArray());
    }

    @Test
    public void testScan_BasicScan() {
        stream = IntStream.of(1, 2, 3, 4).scan((a, b) -> a + b);
        assertArrayEquals(new int[] { 1, 3, 6, 10 }, stream.toArray());
    }

    @Test
    public void testScan_WithInit() {
        stream = IntStream.of(1, 2, 3, 4).scan(10, (a, b) -> a + b);
        assertArrayEquals(new int[] { 11, 13, 16, 20 }, stream.toArray());
    }

    @Test
    public void testScan_WithInitIncluded() {
        int[] result = IntStream.of(1, 2, 3).scan(10, true, Integer::sum).toArray();
        assertArrayEquals(new int[] { 10, 11, 13, 16 }, result);
    }

    @Test
    public void testScan_WithInitNotIncluded() {
        stream = IntStream.of(1, 2, 3, 4).scan(10, false, (a, b) -> a + b);
        assertArrayEquals(new int[] { 11, 13, 16, 20 }, stream.toArray());
    }

    @Test
    public void testScanEmptyStream() {
        stream = IntStream.empty();
        int[] result = stream.scan((a, b) -> a + b).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testScanWithInit() {
        IntStream stream = createIntStream(1, 2, 3, 4).scan(10, Integer::sum);
        assertArrayEquals(new int[] { 11, 13, 16, 20 }, stream.toArray());
    }

    @Test
    public void testScanWithInitIncluded() {
        IntStream stream = createIntStream(1, 2, 3, 4).scan(10, true, Integer::sum);
        assertArrayEquals(new int[] { 10, 11, 13, 16, 20 }, stream.toArray());
    }

    // shuffled

    @Test
    public void testShuffled() {
        int[] original = { 1, 2, 3, 4, 5 };
        stream = createIntStream(original).shuffled();
        int[] shuffled = stream.toArray();

        Arrays.sort(shuffled);
        assertArrayEquals(original, shuffled);
    }

    @Test
    public void testShuffled_HappyPath() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).shuffled().toArray();
        assertEquals(5, result.length);
        Arrays.sort(result);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testShuffled_WithRandom() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toArray();
        assertEquals(5, result.length);
        Arrays.sort(result);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    // skip

    @Test
    public void testSkip() {
        stream = createIntStream(1, 2, 3, 4, 5).skip(2);
        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testSkip_MoreThanAvailable() {
        stream = IntStream.of(1, 2, 3).skip(10);
        assertEquals(0, stream.count());
    }

    @Test
    public void testSkip_SkipElements() {
        stream = IntStream.of(1, 2, 3, 4, 5).skip(2);
        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testSkip_WithAction() {
        List<Integer> skipped = new ArrayList<>();
        int[] result = IntStream.of(1, 2, 3, 4, 5).skip(2, value -> skipped.add(value)).toArray();
        assertArrayEquals(new int[] { 3, 4, 5 }, result);
        assertEquals(Arrays.asList(1, 2), skipped);
    }

    @Test
    public void testSkip_ZeroElements() {
        stream = IntStream.of(1, 2, 3).skip(0);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testSkipWithAction() {
        List<Integer> skipped = new ArrayList<>();
        IntStream stream = createIntStream(1, 2, 3, 4, 5).skip(2, skipped::add);

        assertArrayEquals(new int[] { 3, 4, 5 }, stream.toArray());
        assertEquals(Arrays.asList(1, 2), skipped);
    }

    @Test
    public void testSkipAndLimit_WithOffset() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).skipAndLimit(1, 3).toArray();
        assertArrayEquals(new int[] { 2, 3, 4 }, result);
    }

    @Test
    public void testSkipAndLimit_WithOffsetBeyondSize() {
        int[] result = IntStream.of(1, 2, 3).skipAndLimit(5, 3).toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testSkipUntil() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).skipUntil(x -> x >= 3).toArray();
        assertArrayEquals(new int[] { 3, 4, 5 }, result);
    }

    @Test
    public void testSkipUntil_NeverMatches() {
        int[] result = IntStream.of(1, 2, 3).skipUntil(x -> x > 10).toArray();
        assertArrayEquals(new int[0], result);
    }

    // sorted

    @Test
    public void testSorted() {
        stream = createIntStream(3, 1, 4, 1, 5, 9, 2, 6).sorted();
        assertArrayEquals(new int[] { 1, 1, 2, 3, 4, 5, 6, 9 }, stream.toArray());
    }

    @Test
    public void testSorted_AlreadySorted() {
        stream = IntStream.of(1, 2, 3, 4, 5).sorted();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testSorted_BasicSort() {
        stream = IntStream.of(3, 1, 4, 1, 5, 9, 2, 6).sorted();
        assertArrayEquals(new int[] { 1, 1, 2, 3, 4, 5, 6, 9 }, stream.toArray());
    }

    @Test
    public void testSorted_EmptyStream() {
        stream = IntStream.empty().sorted();
        assertEquals(0, stream.count());
    }

    @Test
    public void sorted_shouldReturnSortedStream() {
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(3, 1, 4, 5, 2).sorted().toArray());
    }

    // step

    @Test
    public void testStep() {
        stream = createIntStream(1, 2, 3, 4, 5, 6, 7, 8).step(2);
        assertArrayEquals(new int[] { 1, 3, 5, 7 }, stream.toArray());
    }

    @Test
    public void testStep_HappyPath() {
        int[] result = IntStream.of(1, 2, 3, 4, 5, 6).step(2).toArray();
        assertArrayEquals(new int[] { 1, 3, 5 }, result);
    }

    @Test
    public void testStep_StepThree() {
        int[] result = IntStream.of(1, 2, 3, 4, 5, 6, 7).step(3).toArray();
        assertArrayEquals(new int[] { 1, 4, 7 }, result);
    }

    @Test
    public void testSymmetricDifference() {
        stream = createIntStream(1, 2, 3).symmetricDifference(Arrays.asList(2, 3, 4));
        int[] result = stream.sorted().toArray();
        assertArrayEquals(new int[] { 1, 4 }, result);
    }

    @Test
    public void testSymmetricDifference_HappyPath() {
        List<Integer> other = Arrays.asList(2, 4, 6);
        int[] result = IntStream.of(1, 2, 3, 4, 5).symmetricDifference(other).sorted().toArray();
        assertArrayEquals(new int[] { 1, 3, 5, 6 }, result);
    }

    // takeWhile

    @Test
    public void testTakeWhile() {
        stream = createIntStream(1, 2, 3, 4, 5, 6).takeWhile(n -> n < 4);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testTakeWhile_AllElements() {
        stream = IntStream.of(1, 2, 3).takeWhile(x -> x < 10);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testTakeWhile_BasicTake() {
        stream = IntStream.of(1, 2, 3, 4, 5).takeWhile(x -> x < 4);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testTakeWhile_EmptyResult() {
        stream = IntStream.of(5, 6, 7).takeWhile(x -> x < 5);
        assertEquals(0, stream.count());
    }

    // top

    @Test
    public void testTop() {
        stream = createIntStream(5, 2, 8, 1, 9, 3, 7).top(3);
        int[] top3 = stream.sorted().toArray();
        assertArrayEquals(new int[] { 7, 8, 9 }, top3);
    }

    @Test
    public void testTop_TopElements() {
        stream = IntStream.of(5, 3, 8, 1, 9, 2).top(3);
        assertArrayEquals(new int[] { 5, 8, 9 }, stream.toArray());
    }

    @Test
    public void testTop_WithComparator() {
        stream = IntStream.of(5, 3, 8, 1, 9, 2).top(3, Comparator.naturalOrder());
        assertArrayEquals(new int[] { 5, 8, 9 }, stream.toArray());
    }

    @Test
    public void testTopWithComparator() {
        IntStream stream = createIntStream(5, 2, 8, 1, 9, 3, 7).top(3, (a, b) -> b - a);
        int[] bottom3 = stream.sorted().toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, bottom3);
    }

}
