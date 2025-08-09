package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IntList;


/**
 * Unit tests for {@link IntStream}.
 * This is a sample and not an exhaustive test suite.
 */
public class IntStream200Test extends TestBase {

    @Test
    public void of_singleElement_shouldCreateStream() {
        assertEquals(1, IntStream.of(1).count());
        assertEquals(1, IntStream.of(1).first().orElseThrow());
    }

    @Test
    public void of_array_shouldCreateStream() {
        int[] source = { 1, 2, 3 };
        assertArrayEquals(source, IntStream.of(source).toArray());
    }

    @Test
    public void of_emptyArray_shouldCreateEmptyStream() {
        assertEquals(0, IntStream.of(new int[0]).count());
    }

    @Test
    public void range_shouldCreateStreamOfRange() {
        assertArrayEquals(new int[] { 0, 1, 2 }, IntStream.range(0, 3).toArray());
    }

    @Test
    public void range_emptyRange_shouldCreateEmptyStream() {
        assertEquals(0, IntStream.range(3, 3).count());
    }

    @Test
    public void rangeClosed_shouldCreateInclusiveRangeStream() {
        assertArrayEquals(new int[] { 0, 1, 2, 3 }, IntStream.rangeClosed(0, 3).toArray());
    }

    @Test
    public void empty_shouldCreateEmptyStream() {
        assertEquals(0, IntStream.empty().count());
    }

    @Test
    public void concat_shouldConcatenateStreams() {
        IntStream a = IntStream.of(1, 2);
        IntStream b = IntStream.of(3, 4);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.concat(a, b).toArray());
    }

    @Test
    public void filter_shouldFilterElements() {
        assertArrayEquals(new int[] { 2, 4 }, IntStream.of(1, 2, 3, 4, 5).filter(i -> i % 2 == 0).toArray());
    }

    @Test
    public void map_shouldTransformElements() {
        assertArrayEquals(new int[] { 2, 4, 6 }, IntStream.of(1, 2, 3).map(i -> i * 2).toArray());
    }

    @Test
    public void sum_shouldReturnSumOfElements() {
        assertEquals(6, IntStream.of(1, 2, 3).sum());
    }

    @Test
    public void sum_onEmptyStream_shouldReturnZero() {
        assertEquals(0, IntStream.empty().sum());
    }

    @Test
    public void average_shouldReturnAverageOfElements() {
        assertEquals(2.0, IntStream.of(1, 2, 3).average().orElseThrow(), 0.001);
    }

    @Test
    public void average_onEmptyStream_shouldReturnEmpty() {
        assertTrue(IntStream.empty().average().isEmpty());
    }

    @Test
    public void toIntList_shouldReturnIntList() {
        IntList expected = IntList.of(1, 2, 3);
        assertEquals(expected, IntStream.of(1, 2, 3).toIntList());
    }

    @Test
    public void findFirst_onEmptyStream_shouldReturnEmpty() {
        assertTrue(IntStream.empty().first().isEmpty());
    }

    @Test
    public void findFirst_onNonEmptyStream_shouldReturnFirstElement() {
        assertEquals(1, IntStream.of(1, 2, 3).first().orElseThrow());
    }

    @Test
    public void toArray_onEmptyStream_shouldReturnEmptyArray() {
        assertEquals(0, IntStream.empty().toArray().length);
    }

    @Test
    public void reduce_withIdentityAndAccumulator_shouldPerformReduction() {
        int result = IntStream.of(1, 2, 3).reduce(0, (a, b) -> a + b);
        assertEquals(6, result);
    }

    @Test
    public void reduce_withAccumulator_shouldPerformReduction() {
        int result = IntStream.of(1, 2, 3).reduce((a, b) -> a + b).orElseThrow();
        assertEquals(6, result);
    }

    @Test
    public void allMatch_shouldReturnTrueIfAllElementsMatch() {
        assertTrue(IntStream.of(2, 4, 6).allMatch(i -> i % 2 == 0));
    }

    @Test
    public void allMatch_shouldReturnFalseIfAnyElementDoesNotMatch() {
        assertFalse(IntStream.of(1, 2, 3).allMatch(i -> i % 2 == 0));
    }

    @Test
    public void anyMatch_shouldReturnTrueIfAnyElementMatches() {
        assertTrue(IntStream.of(1, 2, 3).anyMatch(i -> i % 2 == 0));
    }

    @Test
    public void noneMatch_shouldReturnTrueIfNoElementsMatch() {
        assertTrue(IntStream.of(1, 3, 5).noneMatch(i -> i % 2 == 0));
    }

    @Test
    public void distinct_shouldReturnStreamWithUniqueElements() {
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStream.of(1, 2, 2, 3, 1).distinct().toArray());
    }

    @Test
    public void sorted_shouldReturnSortedStream() {
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(3, 1, 4, 5, 2).sorted().toArray());
    }

    @Test
    public void reversed_shouldReturnReversedStream() {
        assertArrayEquals(new int[] { 3, 2, 1 }, IntStream.of(1, 2, 3).reversed().toArray());
    }

    @Test
    public void zip_shouldZipStreams() {
        IntStream a = IntStream.of(1, 2, 3);
        IntStream b = IntStream.of(4, 5, 6);
        IntStream zipped = IntStream.zip(a, b, (x, y) -> x + y);
        assertArrayEquals(new int[] { 5, 7, 9 }, zipped.toArray());
    }

    @Test
    public void append_shouldAppendElements() {
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.of(1, 2).append(3, 4).toArray());
    }

    @Test
    public void prepend_shouldPrependElements() {
        assertArrayEquals(new int[] { 3, 4, 1, 2 }, IntStream.of(1, 2).prepend(3, 4).toArray());
    }

    @Test
    public void generate_shouldCreateInfiniteStream() {
        assertArrayEquals(new int[] { 0, 0, 0 }, IntStream.generate(() -> 0).limit(3).toArray());
    }

    @Test
    public void iterate_shouldCreateInfiniteStream() {
        assertArrayEquals(new int[] { 0, 2, 4 }, IntStream.iterate(0, i -> i + 2).limit(3).toArray());
    }

}
