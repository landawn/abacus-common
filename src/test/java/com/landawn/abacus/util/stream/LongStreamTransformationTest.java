package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.IndexedLong;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.u.OptionalLong;

/** LongStream transformation contracts. */
public class LongStreamTransformationTest extends LongStreamTestSupport {

    // collapse

    @Test
    public void testCollapse() {
        LongStream stream = LongStream.of(1L, 2L, 5L, 6L, 10L);
        Stream<LongList> result = stream.collapse((a, b) -> b - a <= 2);

        List<LongList> lists = result.toList();
        assertEquals(3, lists.size());
        assertArrayEquals(new long[] { 1L, 2L }, lists.get(0).trimToSize().internalArray());
        assertArrayEquals(new long[] { 5L, 6L }, lists.get(1).trimToSize().internalArray());
        assertArrayEquals(new long[] { 10L }, lists.get(2).trimToSize().internalArray());
    }

    @Test
    public void testCollapse_ListForm() {
        List<LongList> result = LongStream.of(1, 1, 2, 2, 3).collapse((a, b) -> a == b).toList();
        assertEquals(3, result.size());
        assertArrayEquals(new long[] { 1, 1 }, result.get(0).toArray());
        assertArrayEquals(new long[] { 2, 2 }, result.get(1).toArray());
        assertArrayEquals(new long[] { 3 }, result.get(2).toArray());
    }

    @Test
    public void testCollapse_WithMergeFunction2() {
        long[] result = LongStream.of(1, 1, 2, 2, 3).collapse((a, b) -> a == b, Long::sum).toArray();
        assertArrayEquals(new long[] { 2, 4, 3 }, result);
    }

    @Test
    public void testCollapse_WithTriPredicate() {
        long[] result = LongStream.of(1, 2, 3, 10, 11, 12).collapse((first, previous, current) -> current - first <= 2, Long::sum).toArray();
        assertArrayEquals(new long[] { 6, 33 }, result);
    }

    @Test
    public void testCollapseWithMerge() {
        LongStream stream = LongStream.of(1L, 2L, 5L, 6L, 10L);
        long[] result = stream.collapse((a, b) -> b - a <= 2, Long::sum).toArray();
        assertArrayEquals(new long[] { 3L, 11L, 10L }, result);
    }

    @Test
    public void testCollapseWithMergeFunction() {
        long[] result = createLongStream(1L, 2L, 2L, 3L, 3L, 3L, 4L).collapse((a, b) -> a == b, Long::sum).toArray();
        assertArrayEquals(new long[] { 1L, 4L, 9L, 4L }, result);
    }

    @Test
    public void testCollapseWithNoCollapsibleElements() {
        LongStream stream = LongStream.of(1L, 10L, 20L, 30L).collapse((prev, curr) -> curr - prev <= 2, (a, b) -> a + b);
        assertArrayEquals(new long[] { 1L, 10L, 20L, 30L }, stream.toArray());
    }

    @Test
    public void testCollapseWithTriPredicate() {
        assertEquals(3, LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).count());
        assertEquals(2, LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 3, 11, 10 },
                LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 11, 10 },
                LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 11L, 10L), LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(11L, 10L),
                LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).count());
        assertEquals(2, LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 3, 11, 10 },
                LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 11, 10 },
                LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 11L, 10L),
                LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(11L, 10L),
                LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toList());
    }

    // cycled

    @Test
    public void testCycled() {
        LongStream stream = LongStream.of(1L, 2L);
        long[] result = stream.cycled(3).limit(6).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 1L, 2L, 1L, 2L }, result);
    }

    @Test
    public void testCycled_HappyPath() {
        long[] result = LongStream.of(1, 2, 3).cycled().limit(7).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 1, 2, 3, 1 }, result);
    }

    @Test
    public void testCycled_WithRounds2() {
        long[] result = LongStream.of(1, 2).cycled(3).toArray();
        assertArrayEquals(new long[] { 1, 2, 1, 2, 1, 2 }, result);
    }

    @Test
    public void testCycledWithRounds() {
        assertArrayEquals(new long[] { 1, 2, 3, 1, 2, 3 }, LongStream.of(1, 2, 3).cycled(2).toArray());
        assertEquals(0, LongStream.of(1, 2, 3).cycled(0).count());
    }

    @Test
    public void testDifference() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        Collection<Long> set = Arrays.asList(2L, 3L, 6L);
        long[] result = stream.difference(set).toArray();
        assertArrayEquals(new long[] { 1L, 4L, 5L }, result);
    }

    @Test
    public void testDifference_HappyPath() {
        Set<Long> other = new HashSet<>(Arrays.asList(2L, 4L));
        long[] result = LongStream.of(1, 2, 3, 4, 5).difference(other).toArray();
        assertArrayEquals(new long[] { 1, 3, 5 }, result);
    }

    // distinct

    @Test
    public void testDistinct() {
        LongStream stream = LongStream.of(1L, 2L, 2L, 3L, 3L, 3L);
        long[] result = stream.distinct().toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testDistinctEmpty() {
        LongStream stream = LongStream.empty();
        long[] result = stream.distinct().toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testDistinctNoChange() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.distinct().toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    // dropWhile

    @Test
    public void testDropWhile() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.dropWhile(n -> n < 3L).toArray();
        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
    }

    @Test
    public void testDropWhile_WithAction() {
        List<Long> dropped = new ArrayList<>();
        long[] result = LongStream.of(1, 2, 3, 4, 5).dropWhile(x -> x <= 3, value -> dropped.add(value)).toArray();
        assertArrayEquals(new long[] { 4, 5 }, result);
        assertEquals(Arrays.asList(1L, 2L, 3L), dropped);
    }

    @Test
    public void testDropWhileAll() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.dropWhile(n -> n < 10L).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testDropWhileWithAction() {
        List<Long> dropped = new ArrayList<>();
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.dropWhile(n -> n < 3L, dropped::add).toArray();

        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
        assertEquals(Arrays.asList(1L, 2L), dropped);
    }

    @Test
    public void testPeekIfEmpty() {
        List<Long> result = new ArrayList<>();

        createLongStream(1L, 2L, 3L).ifEmpty(() -> result.add(999L)).forEach(result::add);
        assertEquals(Arrays.asList(1L, 2L, 3L), result);

        result.clear();
        LongStream.empty().ifEmpty(() -> result.add(999L)).forEach(result::add);
        assertEquals(Arrays.asList(999L), result);
    }

    // filter

    @Test
    public void testFilter() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.filter(n -> n % 2 == 0).toArray();
        assertArrayEquals(new long[] { 2L, 4L }, result);
    }

    @Test
    public void testFilter_WithAction() {
        List<Long> rejected = new ArrayList<>();
        long[] result = LongStream.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0, value -> rejected.add(value)).toArray();
        assertArrayEquals(new long[] { 2, 4 }, result);
        assertEquals(Arrays.asList(1L, 3L, 5L), rejected);
    }

    @Test
    public void testFilterAllMatch() {
        LongStream stream = LongStream.of(2L, 4L, 6L);
        long[] result = stream.filter(n -> n % 2 == 0).toArray();
        assertArrayEquals(new long[] { 2L, 4L, 6L }, result);
    }

    @Test
    public void testFilterNoneMatch() {
        LongStream stream = LongStream.of(1L, 3L, 5L);
        long[] result = stream.filter(n -> n % 2 == 0).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testFilterWithAction() {
        List<Long> dropped = new ArrayList<>();
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.filter(n -> n % 2 == 0, dropped::add).toArray();

        assertArrayEquals(new long[] { 2L, 4L }, result);
        assertEquals(Arrays.asList(1L, 3L, 5L), dropped);
    }

    @Test
    public void testFilterWithActionOnDropped() {
        LongList dropped = new LongList();
        long[] result = LongStream.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0, dropped::add).toArray();
        assertArrayEquals(new long[] { 2, 4 }, result);
        assertArrayEquals(new long[] { 1, 3, 5 }, dropped.toArray());
    }

    @Test
    public void testIndexed() {
        LongStream stream = LongStream.of(10L, 20L, 30L);
        List<IndexedLong> result = stream.indexed().toList();

        assertEquals(3, result.size());
        assertEquals(0, result.get(0).index());
        assertEquals(10L, result.get(0).value());
        assertEquals(1, result.get(1).index());
        assertEquals(20L, result.get(1).value());
    }

    @Test
    public void testIndexed_HappyPath() {
        List<IndexedLong> result = LongStream.of(10, 20, 30).indexed().toList();
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).index());
        assertEquals(10, result.get(0).value());
        assertEquals(1, result.get(1).index());
        assertEquals(20, result.get(1).value());
    }

    @Test
    public void testIntersection() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        Collection<Long> set = Arrays.asList(2L, 3L, 6L);
        long[] result = stream.intersection(set).toArray();
        assertArrayEquals(new long[] { 2L, 3L }, result);
    }

    @Test
    public void testIntersection_HappyPath() {
        Set<Long> other = new HashSet<>(Arrays.asList(2L, 4L, 6L));
        long[] result = LongStream.of(1, 2, 3, 4, 5).intersection(other).toArray();
        assertArrayEquals(new long[] { 2, 4 }, result);
    }

    // limit

    @Test
    public void testLimit() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.limit(3).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testLimitMoreThanSize() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.limit(10).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testLimitZero() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.limit(0).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testOnEach() {
        AtomicLong sum = new AtomicLong(0);
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.peek(sum::addAndGet).toArray();

        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
        assertEquals(6L, sum.get());
    }

    // onlyOne

    @Test
    public void testOnlyOne() {
        assertEquals(42L, LongStream.of(42L).onlyOne().get());
        assertThrows(NoSuchElementException.class, () -> LongStream.empty().onlyOne().get());
        assertThrows(TooManyElementsException.class, () -> LongStream.of(1L, 2L).onlyOne().get());
    }

    @Test
    public void testOnlyOne_EmptyStream2() {
        OptionalLong result = LongStream.empty().onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_HappyPath2() {
        OptionalLong result = LongStream.of(42).onlyOne();
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    public void testOnlyOne_TooManyElements() {
        assertThrows(com.landawn.abacus.exception.TooManyElementsException.class, () -> LongStream.of(1, 2).onlyOne());
    }

    @Test
    public void testOnlyOneWithMultipleElements() {
        assertThrows(TooManyElementsException.class, () -> createLongStream(1L, 2L).onlyOne());
    }

    @Test
    public void testPeek() {
        List<Long> peeked = new ArrayList<>();
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.peek(peeked::add).toArray();

        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
        assertEquals(Arrays.asList(1L, 2L, 3L), peeked);
    }

    @Test
    public void testPeek_HappyPath() {
        List<Long> peeked = new ArrayList<>();
        long[] result = LongStream.of(1, 2, 3).peek(peeked::add).toArray();
        assertArrayEquals(new long[] { 1, 2, 3 }, result);
        assertEquals(Arrays.asList(1L, 2L, 3L), peeked);
    }

    // reversed

    @Test
    public void testReversed() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L);
        long[] result = stream.reversed().toArray();
        assertArrayEquals(new long[] { 4L, 3L, 2L, 1L }, result);
    }

    @Test
    public void testReversed_Empty() {
        long[] result = LongStream.empty().reversed().toArray();
        assertArrayEquals(new long[0], result);
    }

    @Test
    public void testReversed_HappyPath() {
        long[] result = LongStream.of(1, 2, 3).reversed().toArray();
        assertArrayEquals(new long[] { 3, 2, 1 }, result);
    }

    // reverseSorted

    @Test
    public void testReverseSorted() {
        LongStream stream = LongStream.of(3L, 1L, 4L, 2L);
        long[] result = stream.reverseSorted().toArray();
        assertArrayEquals(new long[] { 4L, 3L, 2L, 1L }, result);
    }

    @Test
    public void testReverseSorted_Empty() {
        long[] result = LongStream.empty().reverseSorted().toArray();
        assertArrayEquals(new long[0], result);
    }

    @Test
    public void testReverseSorted_HappyPath() {
        long[] result = LongStream.of(3, 1, 4, 1, 5).reverseSorted().toArray();
        assertArrayEquals(new long[] { 5, 4, 3, 1, 1 }, result);
    }

    // rotated

    @Test
    public void testRotated() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.rotated(2).toArray();
        assertArrayEquals(new long[] { 4L, 5L, 1L, 2L, 3L }, result);
    }

    @Test
    public void testRotated_HappyPath() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).rotated(2).toArray();
        assertArrayEquals(new long[] { 4, 5, 1, 2, 3 }, result);
    }

    @Test
    public void testRotated_NegativeDistance() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).rotated(-2).toArray();
        assertArrayEquals(new long[] { 3, 4, 5, 1, 2 }, result);
    }

    // scan

    @Test
    public void testScan() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L);
        long[] result = stream.scan((a, b) -> a + b).toArray();
        assertArrayEquals(new long[] { 1L, 3L, 6L, 10L }, result);
    }

    @Test
    public void testScan_BasicScan() {
        long[] result = LongStream.of(1, 2, 3, 4).scan(Long::sum).toArray();
        assertArrayEquals(new long[] { 1, 3, 6, 10 }, result);
    }

    @Test
    public void testScan_WithInit2() {
        long[] result = LongStream.of(1, 2, 3).scan(10L, Long::sum).toArray();
        assertArrayEquals(new long[] { 11, 13, 16 }, result);
    }

    @Test
    public void testScan_WithInitIncluded2() {
        long[] result = LongStream.of(1, 2, 3).scan(10L, true, Long::sum).toArray();
        assertArrayEquals(new long[] { 10, 11, 13, 16 }, result);
    }

    @Test
    public void testScanOnEmptyStream() {
        LongStream stream1 = LongStream.empty().scan((a, b) -> a + b);
        assertEquals(0, stream1.count());

        LongStream stream2 = LongStream.empty().scan(10L, (a, b) -> a + b);
        assertEquals(0, stream2.count());

        LongStream stream3 = LongStream.empty().scan(10L, true, (a, b) -> a + b);
        assertArrayEquals(new long[] { 10L }, stream3.toArray());
    }

    @Test
    public void testScanWithInit() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.scan(10L, (a, b) -> a + b).toArray();
        assertArrayEquals(new long[] { 11L, 13L, 16L }, result);
    }

    @Test
    public void testScanWithInitialValue() {
        assertArrayEquals(new long[] { 10, 11, 13, 16, 20 }, LongStream.of(1, 2, 3, 4).scan(10, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 11, 13, 16, 20 }, LongStream.of(1, 2, 3, 4).scan(10, (a, b) -> a + b).toArray());
    }

    @Test
    public void testScanWithInitIncluded() {
        assertEquals(6, LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 10, 11, 13, 16, 20, 25 }, LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 11, 13, 16, 20, 25 }, LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(10L, 11L, 13L, 16L, 20L, 25L), LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(11L, 13L, 16L, 20L, 25L), LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toList());
        assertEquals(6, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 10, 11, 13, 16, 20, 25 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 11, 13, 16, 20, 25 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(10L, 11L, 13L, 16L, 20L, 25L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(11L, 13L, 16L, 20L, 25L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testScanWithInitNotIncluded() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.scan(10L, false, (a, b) -> a + b).toArray();
        assertArrayEquals(new long[] { 11L, 13L, 16L }, result);
    }

    // shuffled

    @Test
    public void testShuffled() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.shuffled().toArray();

        assertEquals(5, result.length);
        Arrays.sort(result);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testShuffled_HappyPath2() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).shuffled().toArray();
        assertEquals(5, result.length);
        Arrays.sort(result);
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testShuffled_WithRandom2() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toArray();
        assertEquals(5, result.length);
        Arrays.sort(result);
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testShuffledWithRandom() {
        Random rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).skip(1).count());
        rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).toArray().length);
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).skip(1).toArray().length);
        rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).toList().size());
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).skip(1).toList().size());
        rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).skip(1).count());
        rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).toArray().length);
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).skip(1).toArray().length);
        rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).toList().size());
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).skip(1).toList().size());
    }

    // skip

    @Test
    public void testSkip() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.skip(2).toArray();
        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
    }

    @Test
    public void testSkip_WithAction2() {
        List<Long> skipped = new ArrayList<>();
        long[] result = LongStream.of(1, 2, 3, 4, 5).skip(2, value -> skipped.add(value)).toArray();
        assertArrayEquals(new long[] { 3, 4, 5 }, result);
        assertEquals(Arrays.asList(1L, 2L), skipped);
    }

    @Test
    public void testSkipAll() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.skip(10).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testSkipWithAction() {
        List<Long> skipped = new ArrayList<>();
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.skip(2, skipped::add).toArray();

        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
        assertEquals(Arrays.asList(1L, 2L), skipped);
    }

    @Test
    public void testSkipZero() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.skip(0).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testSkipAndLimit_WithOffset() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).skipAndLimit(1, 3).toArray();
        assertArrayEquals(new long[] { 2, 3, 4 }, result);
    }

    @Test
    public void testSkipAndLimit_WithOffsetBeyondSize() {
        long[] result = LongStream.of(1, 2, 3).skipAndLimit(5, 3).toArray();
        assertArrayEquals(new long[0], result);
    }

    // skipUntil

    @Test
    public void testSkipUntil() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.skipUntil(n -> n >= 3L).toArray();
        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
    }

    @Test
    public void testSkipUntil_HappyPath() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).skipUntil(x -> x >= 3).toArray();
        assertArrayEquals(new long[] { 3, 4, 5 }, result);
    }

    @Test
    public void testSkipUntil_NeverMatches() {
        long[] result = LongStream.of(1, 2, 3).skipUntil(x -> x > 10).toArray();
        assertArrayEquals(new long[0], result);
    }

    @Test
    public void testSorted() {
        LongStream stream = LongStream.of(3L, 1L, 4L, 1L, 5L, 9L, 2L, 6L);
        long[] result = stream.sorted().toArray();
        assertArrayEquals(new long[] { 1L, 1L, 2L, 3L, 4L, 5L, 6L, 9L }, result);
    }

    @Test
    public void testSortedAlreadySorted() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L);
        long[] result = stream.sorted().toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    // step

    @Test
    public void testStep() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
        long[] result = stream.step(2).toArray();
        assertArrayEquals(new long[] { 1L, 3L, 5L, 7L }, result);
    }

    @Test
    public void testStep_HappyPath() {
        long[] result = LongStream.of(1, 2, 3, 4, 5, 6).step(2).toArray();
        assertArrayEquals(new long[] { 1, 3, 5 }, result);
    }

    @Test
    public void testStep_StepThree() {
        long[] result = LongStream.of(1, 2, 3, 4, 5, 6, 7).step(3).toArray();
        assertArrayEquals(new long[] { 1, 4, 7 }, result);
    }

    @Test
    public void testSymmetricDifference() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        Collection<Long> set = Arrays.asList(2L, 3L, 4L);
        long[] result = stream.symmetricDifference(set).toArray();
        assertArrayEquals(new long[] { 1L, 4L }, result);
    }

    @Test
    public void testSymmetricDifference_HappyPath() {
        List<Long> other = Arrays.asList(2L, 4L, 6L);
        long[] result = LongStream.of(1, 2, 3, 4, 5).symmetricDifference(other).sorted().toArray();
        assertArrayEquals(new long[] { 1, 3, 5, 6 }, result);
    }

    @Test
    public void testTakeWhile() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.takeWhile(n -> n < 4L).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testTakeWhileEmpty() {
        LongStream stream = LongStream.of(5L, 6L, 7L);
        long[] result = stream.takeWhile(n -> n < 4L).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    // top

    @Test
    public void testTop() {
        LongStream stream = LongStream.of(5L, 2L, 8L, 1L, 9L, 3L);
        long[] result = stream.top(3).toArray();
        assertArrayEquals(new long[] { 5L, 8L, 9L }, result);
    }

    @Test
    public void testTopNegativeN() {
        assertThrows(IllegalArgumentException.class, () -> createLongStream(1L, 2L, 3L).top(-1));
    }

    @Test
    public void testTopWithComparator() {
        LongStream stream = LongStream.of(5L, 2L, 8L, 1L, 9L, 3L);
        long[] result = stream.top(3, Comparator.naturalOrder()).toArray();
        assertArrayEquals(new long[] { 5L, 8L, 9L }, result);
    }

}
