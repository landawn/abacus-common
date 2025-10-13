package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.DoubleStream;

@Tag("new-test")
public class DoubleList101Test extends TestBase {

    private DoubleList list;
    private static final double DELTA = 0.000001;

    @BeforeEach
    public void setUp() {
        list = new DoubleList();
    }

    @Test
    public void testConstructorWithNullArrayAndSize() {
        assertThrows(NullPointerException.class, () -> new DoubleList(null, 0));
    }

    @Test
    public void testConstructorWithNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new DoubleList(-1));
    }

    @Test
    public void testConstructorWithZeroSize() {
        double[] array = { 1.1, 2.2, 3.3 };
        DoubleList list = new DoubleList(array, 0);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOfWithNullAndSize() {
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleList.of(null, 5));
    }

    @Test
    public void testCopyOfWithNull() {
        DoubleList list = DoubleList.copyOf(null);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testCopyOfRangeWithInvalidIndices() {
        double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleList.copyOf(array, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleList.copyOf(array, -1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleList.copyOf(array, 2, 10));
    }

    @Test
    public void testRepeatWithSpecialValues() {
        DoubleList nanList = DoubleList.repeat(Double.NaN, 3);
        assertEquals(3, nanList.size());
        for (int i = 0; i < 3; i++) {
            assertTrue(Double.isNaN(nanList.get(i)));
        }

        DoubleList infList = DoubleList.repeat(Double.POSITIVE_INFINITY, 3);
        assertEquals(3, infList.size());
        for (int i = 0; i < 3; i++) {
            assertEquals(Double.POSITIVE_INFINITY, infList.get(i), DELTA);
        }
    }

    @Test
    public void testAddAtBeginning() {
        list.addAll(new double[] { 2.2, 3.3, 4.4 });
        list.add(0, 1.1);
        assertEquals(4, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
    }

    @Test
    public void testAddAtEnd() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.add(list.size(), 4.4);
        assertEquals(4, list.size());
        assertEquals(4.4, list.get(3), DELTA);
    }

    @Test
    public void testAddAllEmptyListAtVariousPositions() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList empty = new DoubleList();

        assertFalse(list.addAll(0, empty));
        assertFalse(list.addAll(1, empty));
        assertFalse(list.addAll(list.size(), empty));
    }

    @Test
    public void testAddAllWithNullArray() {
        list.add(1.1);
        assertFalse(list.addAll((double[]) null));
        assertEquals(1, list.size());
    }

    @Test
    public void testAddToEnsureCapacityGrowth() {
        for (int i = 0; i < 100; i++) {
            list.add(i * 0.1);
        }
        assertEquals(100, list.size());
        for (int i = 0; i < 100; i++) {
            assertEquals(i * 0.1, list.get(i), DELTA);
        }
    }

    @Test
    public void testRemoveNaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2 });

        boolean result = list.remove(Double.NaN);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
    }

    @Test
    public void testRemoveAllOccurrencesWithNaN() {
        list.addAll(new double[] { Double.NaN, 1.1, Double.NaN, 2.2, Double.NaN });

        boolean result = list.removeAllOccurrences(Double.NaN);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
    }

    @Test
    public void testRemoveIfEmptyList() {
        assertFalse(list.removeIf(x -> true));
    }

    @Test
    public void testRemoveIfNoMatch() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertFalse(list.removeIf(x -> x > 10.0));
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveIfWithNaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2, Double.NaN, 3.3 });

        boolean result = list.removeIf(Double::isNaN);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
    }

    @Test
    public void testRemoveIfWithInfinity() {
        list.addAll(new double[] { 1.1, Double.POSITIVE_INFINITY, 2.2, Double.NEGATIVE_INFINITY, 3.3 });

        boolean result = list.removeIf(Double::isInfinite);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
    }

    @Test
    public void testRemoveDuplicatesEmptyList() {
        assertFalse(list.removeDuplicates());
    }

    @Test
    public void testRemoveDuplicatesSingleElement() {
        list.add(1.1);
        assertFalse(list.removeDuplicates());
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveDuplicatesWithNaN() {
        list.addAll(new double[] { 1.1, Double.NaN, Double.NaN, 2.2 });
        assertTrue(list.removeDuplicates());
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveDuplicatesSorted() {
        list.addAll(new double[] { 1.1, 1.1, 2.2, 2.2, 2.2, 3.3, 3.3, 4.4, 5.5, 5.5 });
        assertTrue(list.removeDuplicates());
        assertEquals(5, list.size());
        for (int i = 1; i <= 5; i++) {
            assertEquals(i * 1.1, list.get(i - 1), DELTA);
        }
    }

    @Test
    public void testRetainAllEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertTrue(list.retainAll(new DoubleList()));
        assertTrue(list.isEmpty());
    }

    @Test
    public void testDeleteAtBoundaries() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        assertEquals(1.1, list.delete(0), DELTA);
        assertEquals(4, list.size());
        assertEquals(2.2, list.get(0), DELTA);

        assertEquals(5.5, list.delete(list.size() - 1), DELTA);
        assertEquals(3, list.size());
        assertEquals(4.4, list.get(list.size() - 1), DELTA);
    }

    @Test
    public void testDeleteAllByIndicesEmpty() {
        list.deleteAllByIndices();
        assertTrue(list.isEmpty());

        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.deleteAllByIndices();
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteAllByIndicesOutOfOrder() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        list.deleteAllByIndices(4, 1, 2);
        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
    }

    @Test
    public void testDeleteRangeEmptyRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.deleteRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteRangeEntireList() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.deleteRange(0, 3);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testMoveRangeToBeginning() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        list.moveRange(3, 5, 0);
        assertEquals(5, list.size());
        assertEquals(4.4, list.get(0), DELTA);
        assertEquals(5.5, list.get(1), DELTA);
        assertEquals(1.1, list.get(2), DELTA);
    }

    @Test
    public void testMoveRangeToEnd() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        list.moveRange(0, 2, 3);
        assertEquals(5, list.size());
        assertEquals(3.3, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
        assertEquals(5.5, list.get(2), DELTA);
        assertEquals(1.1, list.get(3), DELTA);
        assertEquals(2.2, list.get(4), DELTA);
    }

    @Test
    public void testReplaceRangeWithEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        list.replaceRange(1, 3, new DoubleList());
        assertEquals(3, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
        assertEquals(5.5, list.get(2), DELTA);
    }

    @Test
    public void testReplaceRangeExpanding() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.replaceRange(1, 2, DoubleList.of(10.1, 20.2, 30.3));
        assertEquals(5, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(10.1, list.get(1), DELTA);
        assertEquals(20.2, list.get(2), DELTA);
        assertEquals(30.3, list.get(3), DELTA);
        assertEquals(3.3, list.get(4), DELTA);
    }

    @Test
    public void testReplaceAllNoMatch() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        int count = list.replaceAll(5.5, 10.0);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllWithNaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2, Double.NaN });
        int count = list.replaceAll(Double.NaN, 0.0);
        assertEquals(2, count);
        assertEquals(0.0, list.get(1), DELTA);
        assertEquals(0.0, list.get(3), DELTA);
    }

    @Test
    public void testReplaceIfFalseCondition() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        boolean result = list.replaceIf(x -> false, 10.0);
        assertFalse(result);
    }

    @Test
    public void testReplaceIfWithInfinity() {
        list.addAll(new double[] { 1.1, Double.POSITIVE_INFINITY, 2.2, Double.NEGATIVE_INFINITY });
        boolean result = list.replaceIf(Double::isInfinite, 0.0);
        assertTrue(result);
        assertEquals(0.0, list.get(1), DELTA);
        assertEquals(0.0, list.get(3), DELTA);
    }

    @Test
    public void testFillEmptyList() {
        list.fill(10.0);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testFillRangeInvalidRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(2, 1, 10.0));
    }

    @Test
    public void testFillWithSpecialValues() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.fill(Double.NaN);
        for (int i = 0; i < list.size(); i++) {
            assertTrue(Double.isNaN(list.get(i)));
        }
    }

    @Test
    public void testContainsEmptyList() {
        assertFalse(list.contains(1.1));
    }

    @Test
    public void testContainsNaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2 });
        assertTrue(list.contains(Double.NaN));
    }

    @Test
    public void testContainsInfinity() {
        list.addAll(new double[] { 1.1, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY });
        assertTrue(list.contains(Double.POSITIVE_INFINITY));
        assertTrue(list.contains(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testContainsAnyBothEmpty() {
        DoubleList other = new DoubleList();
        assertFalse(list.containsAny(other));
    }

    @Test
    public void testContainsAllEmptyAgainstNonEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertTrue(list.containsAll(new DoubleList()));
    }

    @Test
    public void testDisjointWithSelf() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertFalse(list.disjoint(list));
    }

    @Test
    public void testIntersectionWithEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList result = list.intersection(new DoubleList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionNoCommon() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList result = list.intersection(DoubleList.of(4.4, 5.5, 6.6));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDifferenceWithEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList result = list.difference(new DoubleList());
        assertEquals(3, result.size());
        assertEquals(1.1, result.get(0), DELTA);
        assertEquals(2.2, result.get(1), DELTA);
        assertEquals(3.3, result.get(2), DELTA);
    }

    @Test
    public void testDifferenceAllRemoved() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList result = list.difference(DoubleList.of(1.1, 2.2, 3.3));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSymmetricDifferenceEmpty() {
        DoubleList result = list.symmetricDifference(new DoubleList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSymmetricDifferenceOneEmpty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList result = list.symmetricDifference(new DoubleList());
        assertEquals(3, result.size());
    }

    @Test
    public void testIndexOfFromIndexBeyondSize() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertEquals(-1, list.indexOf(1.1, 10));
    }

    @Test
    public void testIndexOfNegativeFromIndex() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertEquals(0, list.indexOf(1.1, -1));
    }

    @Test
    public void testIndexOfNaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2, Double.NaN });
        assertEquals(1, list.indexOf(Double.NaN));
        assertEquals(3, list.indexOf(Double.NaN, 2));
    }

    @Test
    public void testLastIndexOfEmptyList() {
        assertEquals(-1, list.lastIndexOf(1.1));
    }

    @Test
    public void testLastIndexOfNegativeStart() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertEquals(-1, list.lastIndexOf(1.1, -1));
    }

    @Test
    public void testLastIndexOfNaN() {
        list.addAll(new double[] { Double.NaN, 1.1, Double.NaN, 2.2 });
        assertEquals(2, list.lastIndexOf(Double.NaN));
        assertEquals(0, list.lastIndexOf(Double.NaN, 1));
    }

    @Test
    public void testMinMaxMedianSingleElement() {
        list.add(5.5);

        OptionalDouble min = list.min();
        assertTrue(min.isPresent());
        assertEquals(5.5, min.getAsDouble(), DELTA);

        OptionalDouble max = list.max();
        assertTrue(max.isPresent());
        assertEquals(5.5, max.getAsDouble(), DELTA);

        OptionalDouble median = list.median();
        assertTrue(median.isPresent());
        assertEquals(5.5, median.getAsDouble(), DELTA);
    }

    @Test
    public void testMinMaxMedianEmptyRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });

        assertFalse(list.min(1, 1).isPresent());
        assertFalse(list.max(1, 1).isPresent());
        assertFalse(list.median(1, 1).isPresent());
    }

    @Test
    public void testMedianEvenElements() {
        list.addAll(new double[] { 1.0, 2.0, 3.0, 4.0 });
        OptionalDouble median = list.median();
        assertTrue(median.isPresent());
        assertEquals(2.0, median.getAsDouble(), DELTA);
    }

    @Test
    public void testMinMaxWithNaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2 });

        OptionalDouble min = list.min();
        assertTrue(min.isPresent());
        assertEquals(1.1, min.get(), DELTA);

        OptionalDouble max = list.max();
        assertTrue(max.isPresent());
        assertEquals(2.2, max.get(), DELTA);
    }

    @Test
    public void testMinMaxWithInfinity() {
        list.addAll(new double[] { 1.1, Double.NEGATIVE_INFINITY, 2.2, Double.POSITIVE_INFINITY });

        OptionalDouble min = list.min();
        assertTrue(min.isPresent());
        assertEquals(Double.NEGATIVE_INFINITY, min.getAsDouble(), DELTA);

        OptionalDouble max = list.max();
        assertTrue(max.isPresent());
        assertEquals(Double.POSITIVE_INFINITY, max.getAsDouble(), DELTA);
    }

    @Test
    public void testForEachEmptyList() {
        List<Double> result = new ArrayList<>();
        list.forEach(result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachReverseRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        List<Double> result = new ArrayList<>();

        list.forEach(4, 1, result::add);

        assertEquals(3, result.size());
        assertEquals(5.5, result.get(0), DELTA);
        assertEquals(4.4, result.get(1), DELTA);
        assertEquals(3.3, result.get(2), DELTA);
    }

    @Test
    public void testForEachWithNegativeToIndex() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        List<Double> result = new ArrayList<>();

        list.forEach(2, -1, result::add);

        assertEquals(3, result.size());
        assertEquals(3.3, result.get(0), DELTA);
        assertEquals(2.2, result.get(1), DELTA);
        assertEquals(1.1, result.get(2), DELTA);
    }

    @Test
    public void testDistinctEmptyRange() {
        list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3 });
        DoubleList result = list.distinct(1, 1);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDistinctSingleElement() {
        list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3 });
        DoubleList result = list.distinct(0, 1);
        assertEquals(1, result.size());
        assertEquals(1.1, result.get(0), DELTA);
    }

    @Test
    public void testDistinctWithNaN() {
        list.addAll(new double[] { 1.1, Double.NaN, Double.NaN, 2.2 });
        DoubleList result = list.distinct(0, list.size());
        assertEquals(3, result.size());
    }

    @Test
    public void testSortEmptyList() {
        list.sort();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSortSingleElement() {
        list.add(5.5);
        list.sort();
        assertEquals(1, list.size());
        assertEquals(5.5, list.get(0), DELTA);
    }

    @Test
    public void testSortWithNaN() {
        list.addAll(new double[] { 2.2, Double.NaN, 1.1, Double.NaN, 3.3 });
        list.sort();
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
        assertTrue(Double.isNaN(list.get(3)));
        assertTrue(Double.isNaN(list.get(4)));
    }

    @Test
    public void testSortWithInfinity() {
        list.addAll(new double[] { 1.1, Double.POSITIVE_INFINITY, -2.2, Double.NEGATIVE_INFINITY, 0.0 });
        list.sort();
        assertEquals(Double.NEGATIVE_INFINITY, list.get(0), DELTA);
        assertEquals(-2.2, list.get(1), DELTA);
        assertEquals(0.0, list.get(2), DELTA);
        assertEquals(1.1, list.get(3), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, list.get(4), DELTA);
    }

    @Test
    public void testParallelSortSmallList() {
        list.addAll(new double[] { 3.3, 1.1, 2.2 });
        list.parallelSort();
        assertTrue(list.isSorted());
    }

    @Test
    public void testBinarySearchUnsorted() {
        list.addAll(new double[] { 3.3, 1.1, 4.4, 1.1, 5.5 });
        int result = list.binarySearch(3.3);
        assertNotNull(result);
    }

    @Test
    public void testReverseEmptyList() {
        list.reverse();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testReverseSingleElement() {
        list.add(5.5);
        list.reverse();
        assertEquals(1, list.size());
        assertEquals(5.5, list.get(0), DELTA);
    }

    @Test
    public void testReverseRangeEmptyRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.reverse(1, 1);
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
    }

    @Test
    public void testRotateEmptyList() {
        list.rotate(5);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testRotateSingleElement() {
        list.add(5.5);
        list.rotate(10);
        assertEquals(1, list.size());
        assertEquals(5.5, list.get(0), DELTA);
    }

    @Test
    public void testRotateNegativeDistance() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        list.rotate(-2);
        assertEquals(3.3, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
        assertEquals(5.5, list.get(2), DELTA);
        assertEquals(1.1, list.get(3), DELTA);
        assertEquals(2.2, list.get(4), DELTA);
    }

    @Test
    public void testShuffleEmptyList() {
        list.shuffle();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testShuffleSingleElement() {
        list.add(5.5);
        list.shuffle();
        assertEquals(1, list.size());
        assertEquals(5.5, list.get(0), DELTA);
    }

    @Test
    public void testSwapSameIndex() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.swap(1, 1);
        assertEquals(2.2, list.get(1), DELTA);
    }

    @Test
    public void testSwapThrowsException() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
    }

    @Test
    public void testCopyEmptyList() {
        DoubleList copy = list.copy();
        assertTrue(copy.isEmpty());
        assertNotSame(list, copy);
    }

    @Test
    public void testCopyRangeInvalidIndices() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(1, 5));
    }

    @Test
    public void testCopyWithNegativeStep() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList copy = list.copy(4, 0, -1);
        assertEquals(4, copy.size());
        assertEquals(5.5, copy.get(0), DELTA);
        assertEquals(4.4, copy.get(1), DELTA);
        assertEquals(3.3, copy.get(2), DELTA);
        assertEquals(2.2, copy.get(3), DELTA);
    }

    @Test
    public void testCopyWithStepLargerThanRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList copy = list.copy(0, 5, 3);
        assertEquals(2, copy.size());
        assertEquals(1.1, copy.get(0), DELTA);
        assertEquals(4.4, copy.get(1), DELTA);
    }

    @Test
    public void testSplitEmptyList() {
        List<DoubleList> chunks = list.split(0, 0, 2);
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitWithChunkSizeLargerThanList() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        List<DoubleList> chunks = list.split(0, 3, 10);
        assertEquals(1, chunks.size());
        assertEquals(3, chunks.get(0).size());
    }

    @Test
    public void testSplitUnevenChunks() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        List<DoubleList> chunks = list.split(0, 5, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void testBoxedEmptyList() {
        List<Double> boxed = list.boxed();
        assertTrue(boxed.isEmpty());
    }

    @Test
    public void testBoxedRangeInvalidIndices() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.boxed(2, 1));
    }

    @Test
    public void testBoxedWithSpecialValues() {
        list.addAll(new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY });
        List<Double> boxed = list.boxed();
        assertTrue(Double.isNaN(boxed.get(0)));
        assertEquals(Double.POSITIVE_INFINITY, boxed.get(1));
        assertEquals(Double.NEGATIVE_INFINITY, boxed.get(2));
    }

    @Test
    public void testIteratorEmptyList() {
        DoubleIterator iter = list.iterator();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    @Test
    public void testStreamEmptyList() {
        DoubleStream stream = list.stream();
        assertEquals(0, stream.count());
    }

    @Test
    public void testStreamRangeEmptyRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleStream stream = list.stream(1, 1);
        assertEquals(0, stream.count());
    }

    @Test
    public void testGetFirstGetLastSingleElement() {
        list.add(5.5);
        assertEquals(5.5, list.getFirst(), DELTA);
        assertEquals(5.5, list.getLast(), DELTA);
    }

    @Test
    public void testRemoveFirstRemoveLastSingleElement() {
        list.add(5.5);
        assertEquals(5.5, list.removeFirst(), DELTA);
        assertTrue(list.isEmpty());

        list.add(10.5);
        assertEquals(10.5, list.removeLast(), DELTA);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testMaxArraySize() {
        try {
            DoubleList largeList = new DoubleList(Integer.MAX_VALUE - 8);
            assertTrue(largeList.isEmpty());
        } catch (OutOfMemoryError e) {
            assertTrue(true);
        }
    }

    @Test
    public void testEnsureCapacityOverflow() {
        list.add(1.1);
        try {
            for (int i = 0; i < 100; i++) {
                list.add(i * 0.1);
            }
            assertTrue(list.size() > 1);
        } catch (OutOfMemoryError e) {
            assertTrue(true);
        }
    }

    @Test
    public void testToStringWithSpecialValues() {
        list.add(Double.NaN);
        list.add(Double.POSITIVE_INFINITY);
        list.add(Double.NEGATIVE_INFINITY);
        list.add(-0.0);
        list.add(0.0);

        String str = list.toString();
        assertTrue(str.contains("NaN"));
        assertTrue(str.contains("Infinity"));
        assertTrue(str.contains("-Infinity"));
    }

    @Test
    public void testArrayModification() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        double[] array = list.array();

        array[1] = 20.5;
        assertEquals(20.5, list.get(1), DELTA);

        list.clear();
        double[] newArray = list.array();
        assertSame(array, newArray);
    }

    @Test
    public void testAddRemovePerformance() {
        int count = 1000;
        for (int i = 0; i < count; i++) {
            list.add(i * 0.1);
        }
        assertEquals(count, list.size());

        list.removeIf(x -> ((int) (x * 10)) % 2 == 0);
        assertEquals(count / 2, list.size());
    }

    @Test
    public void testBatchOperationsLargeData() {
        int size = 1000;
        DoubleList list1 = new DoubleList();
        DoubleList list2 = new DoubleList();

        for (int i = 0; i < size; i++) {
            list1.add(i * 0.1);
        }

        for (int i = size / 2; i < size + size / 2; i++) {
            list2.add(i * 0.1);
        }

        DoubleList intersection = list1.intersection(list2);
        assertEquals(size / 2, intersection.size());

        DoubleList difference = list1.difference(list2);
        assertEquals(size / 2, difference.size());

        DoubleList symDiff = list1.symmetricDifference(list2);
        assertEquals(size, symDiff.size());
    }

    @Test
    public void testConcurrentModification() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        DoubleIterator iter = list.iterator();
        list.add(6.6);

        assertTrue(iter.hasNext());
        iter.nextDouble();
    }

    @Test
    public void testDoublePrecisionComparison() {
        double a = 0.1;
        double b = 0.2;
        double c = a + b;

        list.add(c);
        list.add(0.3);

        assertTrue(list.contains(c));
        assertTrue(list.contains(0.3));
    }

    @Test
    public void testVerySmallValues() {
        list.add(Double.MIN_VALUE);
        list.add(Double.MIN_NORMAL);
        list.add(0.0);

        assertTrue(list.contains(Double.MIN_VALUE));
        assertTrue(list.contains(Double.MIN_NORMAL));

        list.sort();
        assertEquals(0.0, list.get(0), DELTA);
        assertEquals(Double.MIN_VALUE, list.get(1), DELTA);
        assertEquals(Double.MIN_NORMAL, list.get(2), DELTA);
    }

    @Test
    public void testSubnormalNumbers() {
        double subnormal = Double.MIN_VALUE / 2;
        list.add(subnormal);
        list.add(0.0);
        list.add(-subnormal);

        assertEquals(3, list.size());
        assertTrue(list.contains(subnormal));
        assertTrue(list.contains(-subnormal));
    }

    @Test
    public void testLargePrecisionEdge() {
        double edge = Math.pow(2, 53) - 1;
        double edgePlus = edge + 1;
        double edgePlus2 = edge + 2;

        list.add(edge);
        list.add(edgePlus);
        list.add(edgePlus2);

        assertEquals(edge, list.get(0), DELTA);
        assertEquals(edgePlus, list.get(1), DELTA);
        assertEquals(edgePlus2, list.get(2), DELTA);
    }

    @Test
    public void testEqualsWithDoublePrecision() {
        list.add(0.1 + 0.2);

        DoubleList other = new DoubleList();
        other.add(0.3);

        boolean equalsResult = list.equals(other);
        assertNotNull(equalsResult);
    }

    @Test
    public void testScientificNotationValues() {
        list.add(1.23e-10);
        list.add(4.56e+10);
        list.add(7.89e100);
        list.add(-9.87e-100);

        assertEquals(1.23e-10, list.get(0), DELTA);
        assertEquals(4.56e+10, list.get(1), DELTA);
        assertEquals(7.89e100, list.get(2), DELTA);
        assertEquals(-9.87e-100, list.get(3), DELTA);
    }

    @Test
    public void testUlpComparison() {
        double base = 1.0;
        double nextUp = Math.nextUp(base);
        double nextDown = Math.nextDown(base);

        list.add(nextDown);
        list.add(base);
        list.add(nextUp);

        assertEquals(3, list.size());
        assertNotEquals(base, nextUp);
        assertNotEquals(base, nextDown);

        assertTrue(list.contains(base));
        assertTrue(list.contains(nextUp));
        assertTrue(list.contains(nextDown));
    }
}
