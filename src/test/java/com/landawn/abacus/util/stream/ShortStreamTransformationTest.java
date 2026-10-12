package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.u.OptionalShort;

/** ShortStream transformation contracts. */
public class ShortStreamTransformationTest extends ShortStreamTestSupport {

    // collapse

    @Test
    public void testCollapse() {
        Stream<ShortList> stream = ShortStream.of((short) 1, (short) 2, (short) 5, (short) 6, (short) 7, (short) 10).collapse((prev, curr) -> curr - prev <= 2);

        List<ShortList> result = stream.toList();
        assertEquals(3, result.size());
        assertArrayEquals(new short[] { 1, 2 }, result.get(0).toArray());
        assertArrayEquals(new short[] { 5, 6, 7 }, result.get(1).toArray());
        assertArrayEquals(new short[] { 10 }, result.get(2).toArray());
    }

    @Test
    public void testCollapse_Empty() {
        List<ShortList> result = ShortStream.empty().collapse((a, b) -> b - a == 1).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollapse_SingleElement() {
        List<ShortList> result = createShortStream((short) 5).collapse((a, b) -> b - a == 1).toList();
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).size());
    }

    @Test
    public void testCollapseWithMerge() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 5, (short) 6, (short) 7, (short) 10)
                .collapse((prev, curr) -> curr - prev <= 2, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 3, 18, 10 }, stream.toArray());
    }

    @Test
    public void testCollapseWithMerge_Empty() {
        short[] result = ShortStream.empty().collapse((a, b) -> b - a == 1, (a, b) -> (short) (a + b)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testCollapseWithNoCollapsibleElements() {
        ShortStream stream = ShortStream.of((short) 1, (short) 10, (short) 20, (short) 30)
                .collapse((prev, curr) -> curr - prev <= 2, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 1, 10, 20, 30 }, stream.toArray());
    }

    @Test
    public void testCollapseWithTriPredicate() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 10, (short) 11)
                .collapse((first, last, next) -> next - first <= 5, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 6, 21 }, stream.toArray());
    }

    @Test
    public void testCollapseWithTriPredicate_Empty() {
        short[] result = ShortStream.empty().collapse((first, last, next) -> next - first < 5, (a, b) -> (short) (a + b)).toArray();
        assertEquals(0, result.length);
    }

    // cycled

    @Test
    public void testCycled() {
        short[] result = createShortStream((short) 1, (short) 2).cycled().limit(5).toArray();
        assertArrayEquals(new short[] { 1, 2, 1, 2, 1 }, result);
    }

    @Test
    public void testCycled_Empty() {
        short[] result = ShortStream.empty().cycled().limit(0).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testCycled_WithRoundsZero() {
        short[] result = createShortStream((short) 1, (short) 2).cycled(0).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testDifference() {
        List<Short> other = List.of((short) 2, (short) 4, (short) 6);
        short[] result = multiElementStream.difference(other).toArray();
        assertArrayEquals(new short[] { 1, 3, 5 }, result);
    }

    @Test
    public void testDifference_Empty() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).difference(java.util.Collections.emptyList()).toArray();
        assertEquals(3, result.length);
    }

    // distinct

    @Test
    public void testDistinct() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 1).distinct();
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testDistinct_AllSame() {
        short[] result = createShortStream((short) 5, (short) 5, (short) 5).distinct().toArray();
        assertArrayEquals(new short[] { 5 }, result);
    }

    @Test
    public void testDistinct_Empty() {
        short[] result = ShortStream.empty().distinct().toArray();
        assertEquals(0, result.length);
    }



    // G088-01: every distinct() call gets its own seen-set; a large random input matches the LinkedHashSet oracle
    @Test
    public void testDistinct_unsortedArray_randomMatchesOracle() {
        final Random random = new Random(20260926L);
        final short[] fullRange = new short[50_000];
        final short[] narrowRange = new short[50_000];

        for (int i = 0; i < fullRange.length; i++) {
            fullRange[i] = (short) random.nextInt();
            narrowRange[i] = (short) (random.nextInt(300) - 150);
        }

        assertArrayEquals(g088DistinctOracle(fullRange, 0, fullRange.length), ShortStream.of(fullRange).distinct().toArray());
        assertArrayEquals(g088DistinctOracle(narrowRange, 0, narrowRange.length), ShortStream.of(narrowRange).distinct().toArray());
        assertArrayEquals(g088DistinctOracle(narrowRange, 0, narrowRange.length), ShortStream.of(narrowRange).distinct().toArray());
        assertEquals(N.toList((short) 7, (short) -7), ShortStream.of((short) 7, (short) -7, (short) 7, (short) -7).distinct().toList());
        assertEquals(0, ShortStream.of(new short[0]).distinct().count());
    }



    // G088-01: unsorted ArrayShortStream.distinct keeps first occurrences in encounter order, incl. sign/word boundaries
    @Test
    public void testDistinct_unsortedArray_boundaryValues() {
        final short[] boundaryValues = { 0, -1, Short.MAX_VALUE, Short.MIN_VALUE, -1, 63, 64, -64, -65, 127, 128, -128, -129, 255, 256, Short.MIN_VALUE, 0,
                Short.MAX_VALUE, 64, 63, (short) -32767, (short) 32766, -1, 1, 1 };

        // the short input (below 256 elements) and the long input (at least 256 elements) take different tracking paths
        g088CheckDistinct(boundaryValues);

        final short[] repeated = new short[boundaryValues.length * 12];
        for (int i = 0; i < repeated.length; i++) {
            repeated[i] = boundaryValues[(i * 7) % boundaryValues.length];
        }
        g088CheckDistinct(repeated);

        final short[] sizes255And256 = new short[256];
        for (int i = 0; i < sizes255And256.length; i++) {
            sizes255And256[i] = (short) ((i % 97) * 677 - 32768);
        }
        g088CheckDistinct(Arrays.copyOf(sizes255And256, 255));
        g088CheckDistinct(sizes255And256);
    }

    // dropWhile

    @Test
    public void testDropWhile() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 2).dropWhile(n -> n < 3);
        assertArrayEquals(new short[] { 3, 4, 2 }, stream.toArray());
    }

    @Test
    public void testDropWhile_AllMatch() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).dropWhile(s -> s < 10).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testDropWhile_Empty() {
        short[] result = ShortStream.empty().dropWhile(s -> s < 10).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testDropWhile_NoneMatch() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).dropWhile(s -> s > 10).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    @DisplayName("Test dropWhile() with action")
    public void testDropWhileWithAction() {
        List<Short> droppedItems = new ArrayList<>();
        ShortStream dropped = stream.dropWhile(n -> n < 4, droppedItems::add);
        assertArrayEquals(new short[] { 4, 5 }, dropped.toArray());
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), droppedItems);
    }

    @Test
    public void testDropWhileWithAction_Method() {
        List<Short> dropped = new ArrayList<>();
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(s -> s < 3, dropped::add).toArray();
        assertArrayEquals(new short[] { 3, 4, 5 }, result);
        assertEquals(2, dropped.size());
    }

    @Test
    public void testDropWhileWithActionOnDropped() {
        List<Short> dropped = new ArrayList<>();
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).dropWhile(n -> n < 3, dropped::add);
        assertArrayEquals(new short[] { 3, 4 }, stream.toArray());
        assertEquals(Arrays.asList((short) 1, (short) 2), dropped);
    }

    // filter

    @Test
    public void testFilter() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).filter(n -> n % 2 == 0);
        assertArrayEquals(new short[] { 2, 4 }, stream.toArray());
    }

    @Test
    public void testFilterEmpty() {
        ShortStream stream = ShortStream.of((short) 1, (short) 3, (short) 5).filter(n -> n % 2 == 0);
        assertEquals(0, stream.count());
    }

    @Test
    @DisplayName("Test filter() with action on dropped")
    public void testFilterWithAction() {
        List<Short> dropped = new ArrayList<>();
        ShortStream filtered = stream.filter(n -> n % 2 == 0, dropped::add);
        assertArrayEquals(new short[] { 2, 4 }, filtered.toArray());
        assertEquals(Arrays.asList((short) 1, (short) 3, (short) 5), dropped);
    }

    @Test
    public void testFilterWithAction_Method() {
        List<Short> dropped = new ArrayList<>();
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(s -> s > 3, dropped::add).toArray();
        assertArrayEquals(new short[] { 4, 5 }, result);
        assertEquals(3, dropped.size());
    }

    @Test
    public void testFilterWithActionOnDropped() {
        List<Short> dropped = new ArrayList<>();
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).filter(n -> n % 2 == 0, dropped::add);
        assertArrayEquals(new short[] { 2, 4 }, stream.toArray());
        assertEquals(Arrays.asList((short) 1, (short) 3), dropped);
    }

    @Test
    public void testIndexed() {
        List<com.landawn.abacus.util.IndexedShort> result = createShortStream((short) 10, (short) 20, (short) 30).indexed().toList();
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).index());
        assertEquals(10, result.get(0).value());
        assertEquals(2, result.get(2).index());
        assertEquals(30, result.get(2).value());
    }

    @Test
    public void testIndexed_Empty() {
        List<com.landawn.abacus.util.IndexedShort> result = ShortStream.empty().indexed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersection() {
        List<Short> other = List.of((short) 2, (short) 2, (short) 4, (short) 6);
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 4, (short) 4);
        short[] result = stream.intersection(other).toArray();
        assertArrayEquals(new short[] { 2, 4 }, result);
    }

    @Test
    public void testIntersection_Empty() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).intersection(java.util.Collections.emptyList()).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testLimit() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).limit(2);
        assertArrayEquals(new short[] { 1, 2 }, stream.toArray());
    }

    @Test
    public void testLimitZero() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).limit(0);
        assertEquals(0, stream.count());
    }

    @Test
    @DisplayName("Test onEach() / peek() method")
    public void testOnEach() {
        List<Short> peeked = new ArrayList<>();
        short[] result = stream.peek(peeked::add).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), peeked);

        peeked.clear();
        result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).peek(peeked::add).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), peeked);
    }

    // onlyOne

    @Test
    public void testOnlyOne() {
        assertTrue(singleElementStream.onlyOne().isPresent());
        assertEquals((short) 5, singleElementStream2.onlyOne().get());
        assertFalse(emptyStream.onlyOne().isPresent());
        assertThrows(com.landawn.abacus.exception.TooManyElementsException.class, () -> multiElementStream.onlyOne());
    }

    @Test
    public void testOnlyOne_Empty() {
        OptionalShort result = ShortStream.empty().onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOneWithMultipleElements() {
        assertThrows(TooManyElementsException.class, () -> createShortStream((short) 1, (short) 2).onlyOne());
    }

    @Test
    public void testPeek() {
        List<Short> peeked = new ArrayList<>();
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).peek(peeked::add);
        stream.toArray();
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), peeked);
    }

    @Test
    public void testPeekIsAliasForOnEach() {
        List<Short> peeked = new ArrayList<>();
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).peek(s -> peeked.add(s)).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
        assertEquals(3, peeked.size());
    }

    // reversed

    @Test
    public void testReversed() {
        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 }, multiElementStream.reversed().toArray());
    }

    @Test
    public void testReversed_Empty() {
        short[] result = ShortStream.empty().reversed().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testReversed_SingleElement() {
        short[] result = createShortStream((short) 5).reversed().toArray();
        assertArrayEquals(new short[] { 5 }, result);
    }

    @Test
    public void testReverseSorted() {
        ShortStream stream = ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2).reverseSorted();
        assertArrayEquals(new short[] { 4, 3, 2, 1 }, stream.toArray());
    }

    @Test
    public void testReverseSorted_Empty() {
        short[] result = ShortStream.empty().reverseSorted().toArray();
        assertEquals(0, result.length);
    }

    // rotated

    @Test
    public void testRotated() {
        assertArrayEquals(new short[] { 4, 5, 1, 2, 3 }, multiElementStream.rotated(2).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5, 1 }, multiElementStream2.rotated(-1).toArray());
    }

    @Test
    public void testRotated_Negative() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).rotated(-1).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testRotated_Zero() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).rotated(0).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    // scan

    @Test
    public void testScan() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).scan((a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 1, 3, 6, 10 }, stream.toArray());
    }

    @Test
    public void testScan_Empty() {
        short[] result = ShortStream.empty().scan((a, b) -> (short) (a + b)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testScanOnEmptyStream() {
        ShortStream stream1 = ShortStream.empty().scan((a, b) -> (short) (a + b));
        assertEquals(0, stream1.count());

        ShortStream stream2 = ShortStream.empty().scan((short) 10, (a, b) -> (short) (a + b));
        assertEquals(0, stream2.count());

        ShortStream stream3 = ShortStream.empty().scan((short) 10, true, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 10 }, stream3.toArray());
    }

    @Test
    public void testScanWithInit() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).scan((short) 10, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 11, 13, 16 }, stream.toArray());
    }

    @Test
    public void testScanWithInit_Empty() {
        short[] result = ShortStream.empty().scan((short) 10, (a, b) -> (short) (a + b)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testScanWithInitAndIncluded() {
        ShortStream stream1 = createShortStream((short) 1, (short) 2, (short) 3).scan((short) 10, true, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 10, 11, 13, 16 }, stream1.toArray());

        ShortStream stream2 = createShortStream((short) 1, (short) 2, (short) 3).scan((short) 10, false, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 11, 13, 16 }, stream2.toArray());
    }

    @Test
    public void testScanWithInitialValue() {
        short[] result = multiElementStream.scan((short) 10, (a, b) -> (short) (a + b)).toArray();
        assertArrayEquals(new short[] { 11, 13, 16, 20, 25 }, result);
    }

    @Test
    public void testScanWithInitIncluded() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).scan((short) 10, true, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 10, 11, 13, 16 }, stream.toArray());

        ShortStream stream2 = ShortStream.of((short) 1, (short) 2, (short) 3).scan((short) 10, false, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 11, 13, 16 }, stream2.toArray());
    }

    @Test
    public void testScanWithInitIncluded_Empty() {
        short[] result = ShortStream.empty().scan((short) 10, true, (a, b) -> (short) (a + b)).toArray();
        assertArrayEquals(new short[] { 10 }, result);
    }

    @Test
    public void testScanWithSeed() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3).scan((short) 10, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 11, 13, 16 }, stream.toArray());
    }

    // shuffled

    @Test
    public void testShuffled() {
        short[] result = multiElementStream.shuffled(new Random(42)).toArray();
        short[] original = multiElementStream2.toArray();
        Arrays.sort(result);
        Arrays.sort(original);
        assertArrayEquals(original, result, "Shuffled stream should contain the same elements");
    }

    @Test
    public void testShuffled_Empty() {
        short[] result = ShortStream.empty().shuffled().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testShuffledWithRandom() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).shuffled(new Random(42)).toArray();
        assertEquals(5, result.length);
    }

    // skip

    @Test
    public void testSkip() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).skip(2);
        assertArrayEquals(new short[] { 3, 4 }, stream.toArray());
    }

    @Test
    public void testSkipAll() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2).skip(5);
        assertEquals(0, stream.count());
    }

    @Test
    public void testSkipWithAction_Method() {
        List<Short> skipped = new ArrayList<>();
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, skipped::add).toArray();
        assertArrayEquals(new short[] { 3, 4, 5 }, result);
        assertEquals(2, skipped.size());
    }

    @Test
    public void testSkipWithActionOnSkipped() {
        List<Short> skipped = new ArrayList<>();
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).skip(2, skipped::add);
        assertArrayEquals(new short[] { 3, 4 }, stream.toArray());
        assertEquals(Arrays.asList((short) 1, (short) 2), skipped);
    }

    // skipAndLimit

    @Test
    public void testSkipAndLimit() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipAndLimit(1, 3).toArray();
        assertArrayEquals(new short[] { 2, 3, 4 }, result);
    }

    @Test
    public void testSkipAndLimit_OffsetBeyondSize() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).skipAndLimit(10, 5).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testSkipAndLimit_ZeroOffset() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipAndLimit(0, 3).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    // skipUntil

    @Test
    @DisplayName("Test skipUntil() method")
    public void testSkipUntil() {
        ShortStream skipped = stream.skipUntil(n -> n >= 3);
        assertArrayEquals(new short[] { 3, 4, 5 }, skipped.toArray());
    }

    @Test
    public void testSkipUntil_Method() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(s -> s >= 3).toArray();
        assertArrayEquals(new short[] { 3, 4, 5 }, result);
    }

    @Test
    public void testSkipUntil_NoneMatch() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).skipUntil(s -> s > 10).toArray();
        assertEquals(0, result.length);
    }

    // sorted

    @Test
    public void testSorted() {
        ShortStream stream = ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2).sorted();
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testSorted_AlreadySorted() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).sorted().toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testSorted_Empty() {
        short[] result = ShortStream.empty().sorted().toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("Test sorting methods")
    public void testSorting() {
        ShortStream unsorted = createShortStream((short) 5, (short) 1, (short) 4, (short) 2, (short) 3);

        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, unsorted.sorted().toArray());

        unsorted = createShortStream((short) 5, (short) 1, (short) 4, (short) 2, (short) 3);

        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 }, createShortStream((short) 5, (short) 1, (short) 4, (short) 2, (short) 3).reverseSorted().toArray());
    }

    // step

    @Test
    @DisplayName("Test step() method")
    public void testStep() {
        assertArrayEquals(new short[] { 1, 3, 5 }, stream.step(2).toArray());
        assertArrayEquals(new short[] { 1, 4 }, stream2.step(3).toArray());
    }

    @Test
    public void testStep_StepOne() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).step(1).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testStep_StepTwo() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).step(2).toArray();
        assertArrayEquals(new short[] { 1, 3, 5 }, result);
    }

    @Test
    @DisplayName("Test step() iterator count() consumes the iterator")
    public void testStepIteratorCountConsumes() {
        // Regression: the step() iterator's count() must exhaust the iterator per the IteratorEx.count contract.
        ShortIterator iter = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).iterator();
        assertEquals(3, iter.count());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSymmetricDifference() {
        List<Short> other = List.of((short) 4, (short) 5, (short) 6, (short) 7);
        short[] result = multiElementStream.symmetricDifference(other).sorted().toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 6, 7 }, result);
    }

    @Test
    public void testSymmetricDifference_Empty() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).symmetricDifference(java.util.Collections.emptyList()).toArray();
        assertEquals(3, result.length);
    }

    // takeWhile

    @Test
    public void testTakeWhile() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 2).takeWhile(n -> n < 4);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testTakeWhile_AllMatch() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).takeWhile(s -> s < 10).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testTakeWhile_Empty() {
        short[] result = ShortStream.empty().takeWhile(s -> s < 10).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testTakeWhile_NoneMatch() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).takeWhile(s -> s < 0).toArray();
        assertEquals(0, result.length);
    }

    // top

    @Test
    public void testTop() {
        ShortStream stream = ShortStream.of((short) 5, (short) 2, (short) 8, (short) 1, (short) 9, (short) 3).top(3);
        short[] result = stream.sorted().toArray();
        assertArrayEquals(new short[] { 8, 9 }, Arrays.copyOfRange(result, Math.max(0, result.length - 2), result.length));
    }

    @Test
    public void testTop_Empty() {
        short[] result = ShortStream.empty().top(3).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testTop_MoreThanAvailable() {
        short[] result = createShortStream((short) 1, (short) 2).top(5).toArray();
        assertEquals(2, result.length);
    }

    @Test
    public void testTopWithComparator() {
        ShortStream stream = ShortStream.of((short) 5, (short) 2, (short) 8, (short) 1, (short) 9).top(3, Comparator.reverseOrder());
        short[] result = stream.toArray();
        assertEquals(3, result.length);
    }

}
