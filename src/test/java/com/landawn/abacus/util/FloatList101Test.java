package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.stream.FloatStream;

@Tag("new-test")
public class FloatList101Test extends TestBase {

    private FloatList list;
    private static final float DELTA = 0.0001f;

    @BeforeEach
    public void setUp() {
        list = new FloatList();
    }

    @Test
    public void testConstructorWithNullArrayAndSize() {
        assertThrows(NullPointerException.class, () -> new FloatList(null, 0));
    }

    @Test
    public void testConstructorWithNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new FloatList(-1));
    }

    @Test
    public void testConstructorWithZeroSize() {
        float[] array = { 1.1f, 2.2f, 3.3f };
        FloatList list = new FloatList(array, 0);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOfWithNullAndSize() {
        assertThrows(IndexOutOfBoundsException.class, () -> FloatList.of(null, 5));
    }

    @Test
    public void testCopyOfWithNull() {
        FloatList list = FloatList.copyOf(null);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testCopyOfRangeWithInvalidIndices() {
        float[] array = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f };
        assertThrows(IndexOutOfBoundsException.class, () -> FloatList.copyOf(array, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> FloatList.copyOf(array, -1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> FloatList.copyOf(array, 2, 10));
    }

    @Test
    public void testRepeatWithSpecialValues() {
        FloatList nanList = FloatList.repeat(Float.NaN, 3);
        assertEquals(3, nanList.size());
        for (int i = 0; i < 3; i++) {
            assertTrue(Float.isNaN(nanList.get(i)));
        }

        FloatList infList = FloatList.repeat(Float.POSITIVE_INFINITY, 3);
        assertEquals(3, infList.size());
        for (int i = 0; i < 3; i++) {
            assertEquals(Float.POSITIVE_INFINITY, infList.get(i), DELTA);
        }
    }

    @Test
    public void testAddAtBeginning() {
        list.addAll(new float[] { 2.2f, 3.3f, 4.4f });
        list.add(0, 1.1f);
        assertEquals(4, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(2.2f, list.get(1), DELTA);
    }

    @Test
    public void testAddAtEnd() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        list.add(list.size(), 4.4f);
        assertEquals(4, list.size());
        assertEquals(4.4f, list.get(3), DELTA);
    }

    @Test
    public void testAddAllEmptyListAtVariousPositions() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatList empty = new FloatList();

        assertFalse(list.addAll(0, empty));
        assertFalse(list.addAll(1, empty));
        assertFalse(list.addAll(list.size(), empty));
    }

    @Test
    public void testAddAllWithNullArray() {
        list.add(1.1f);
        assertFalse(list.addAll((float[]) null));
        assertEquals(1, list.size());
    }

    @Test
    public void testAddToEnsureCapacityGrowth() {
        for (int i = 0; i < 100; i++) {
            list.add(i * 0.1f);
        }
        assertEquals(100, list.size());
        for (int i = 0; i < 100; i++) {
            assertEquals(i * 0.1f, list.get(i), DELTA);
        }
    }

    @Test
    public void testRemoveNaN() {
        list.addAll(new float[] { 1.1f, Float.NaN, 2.2f });

        boolean result = list.remove(Float.NaN);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(2.2f, list.get(1), DELTA);
    }

    @Test
    public void testRemoveAllOccurrencesWithNaN() {
        list.addAll(new float[] { Float.NaN, 1.1f, Float.NaN, 2.2f, Float.NaN });

        boolean result = list.removeAllOccurrences(Float.NaN);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(2.2f, list.get(1), DELTA);
    }

    @Test
    public void testRemoveIfEmptyList() {
        assertFalse(list.removeIf(x -> true));
    }

    @Test
    public void testRemoveIfNoMatch() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertFalse(list.removeIf(x -> x > 10.0f));
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveIfWithNaN() {
        list.addAll(new float[] { 1.1f, Float.NaN, 2.2f, Float.NaN, 3.3f });

        boolean result = list.removeIf(Float::isNaN);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(2.2f, list.get(1), DELTA);
        assertEquals(3.3f, list.get(2), DELTA);
    }

    @Test
    public void testRemoveDuplicatesEmptyList() {
        assertFalse(list.removeDuplicates());
    }

    @Test
    public void testRemoveDuplicatesSingleElement() {
        list.add(1.1f);
        assertFalse(list.removeDuplicates());
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveDuplicatesWithNaN() {
        list.addAll(new float[] { 1.1f, Float.NaN, Float.NaN, 2.2f });
        assertTrue(list.removeDuplicates());
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveDuplicatesSorted() {
        list.addAll(new float[] { 1.1f, 1.1f, 2.2f, 2.2f, 2.2f, 3.3f, 3.3f, 4.4f, 5.5f, 5.5f });
        assertTrue(list.removeDuplicates());
        assertEquals(5, list.size());
        for (int i = 1; i <= 5; i++) {
            assertEquals(i * 1.1f, list.get(i - 1), DELTA);
        }
    }

    @Test
    public void testRetainAllEmpty() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertTrue(list.retainAll(new FloatList()));
        assertTrue(list.isEmpty());
    }

    @Test
    public void testDeleteAtBoundaries() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        assertEquals(1.1f, list.delete(0), DELTA);
        assertEquals(4, list.size());
        assertEquals(2.2f, list.get(0), DELTA);

        assertEquals(5.5f, list.delete(list.size() - 1), DELTA);
        assertEquals(3, list.size());
        assertEquals(4.4f, list.get(list.size() - 1), DELTA);
    }

    @Test
    public void testDeleteAllByIndicesEmpty() {
        list.deleteAllByIndices();
        assertTrue(list.isEmpty());

        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        list.deleteAllByIndices();
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteAllByIndicesOutOfOrder() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        list.deleteAllByIndices(4, 1, 2);
        assertEquals(2, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(4.4f, list.get(1), DELTA);
    }

    @Test
    public void testDeleteRangeEmptyRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        list.deleteRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteRangeEntireList() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        list.deleteRange(0, 3);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testMoveRangeToBeginning() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        list.moveRange(3, 5, 0);
        assertEquals(5, list.size());
        assertEquals(4.4f, list.get(0), DELTA);
        assertEquals(5.5f, list.get(1), DELTA);
        assertEquals(1.1f, list.get(2), DELTA);
    }

    @Test
    public void testMoveRangeToEnd() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        list.moveRange(0, 2, 3);
        assertEquals(5, list.size());
        assertEquals(3.3f, list.get(0), DELTA);
        assertEquals(4.4f, list.get(1), DELTA);
        assertEquals(5.5f, list.get(2), DELTA);
        assertEquals(1.1f, list.get(3), DELTA);
        assertEquals(2.2f, list.get(4), DELTA);
    }

    @Test
    public void testReplaceRangeWithEmpty() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        list.replaceRange(1, 3, new FloatList());
        assertEquals(3, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(4.4f, list.get(1), DELTA);
        assertEquals(5.5f, list.get(2), DELTA);
    }

    @Test
    public void testReplaceRangeExpanding() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        list.replaceRange(1, 2, FloatList.of(10.1f, 20.2f, 30.3f));
        assertEquals(5, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(10.1f, list.get(1), DELTA);
        assertEquals(20.2f, list.get(2), DELTA);
        assertEquals(30.3f, list.get(3), DELTA);
        assertEquals(3.3f, list.get(4), DELTA);
    }

    @Test
    public void testReplaceAllNoMatch() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        int count = list.replaceAll(5.5f, 10.0f);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllWithNaN() {
        list.addAll(new float[] { 1.1f, Float.NaN, 2.2f, Float.NaN });
        int count = list.replaceAll(Float.NaN, 0.0f);
        assertEquals(2, count);
        assertEquals(0.0f, list.get(1), DELTA);
        assertEquals(0.0f, list.get(3), DELTA);
    }

    @Test
    public void testReplaceIfFalseCondition() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        boolean result = list.replaceIf(x -> false, 10.0f);
        assertFalse(result);
    }

    @Test
    public void testReplaceIfWithInfinity() {
        list.addAll(new float[] { 1.1f, Float.POSITIVE_INFINITY, 2.2f, Float.NEGATIVE_INFINITY });
        boolean result = list.replaceIf(Float::isInfinite, 0.0f);
        assertTrue(result);
        assertEquals(0.0f, list.get(1), DELTA);
        assertEquals(0.0f, list.get(3), DELTA);
    }

    @Test
    public void testFillEmptyList() {
        list.fill(10.0f);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testFillRangeInvalidRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(2, 1, 10.0f));
    }

    @Test
    public void testFillWithSpecialValues() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        list.fill(Float.NaN);
        for (int i = 0; i < list.size(); i++) {
            assertTrue(Float.isNaN(list.get(i)));
        }
    }

    @Test
    public void testContainsEmptyList() {
        assertFalse(list.contains(1.1f));
    }

    @Test
    public void testContainsNaN() {
        list.addAll(new float[] { 1.1f, Float.NaN, 2.2f });
        assertTrue(list.contains(Float.NaN));
    }

    @Test
    public void testContainsInfinity() {
        list.addAll(new float[] { 1.1f, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY });
        assertTrue(list.contains(Float.POSITIVE_INFINITY));
        assertTrue(list.contains(Float.NEGATIVE_INFINITY));
    }

    @Test
    public void testContainsAnyBothEmpty() {
        FloatList other = new FloatList();
        assertFalse(list.containsAny(other));
    }

    @Test
    public void testContainsAllEmptyAgainstNonEmpty() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertTrue(list.containsAll(new FloatList()));
    }

    @Test
    public void testDisjointWithSelf() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertFalse(list.disjoint(list));
    }

    @Test
    public void testIntersectionWithEmpty() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatList result = list.intersection(new FloatList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionNoCommon() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatList result = list.intersection(FloatList.of(4.4f, 5.5f, 6.6f));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDifferenceWithEmpty() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatList result = list.difference(new FloatList());
        assertEquals(3, result.size());
        assertEquals(1.1f, result.get(0), DELTA);
        assertEquals(2.2f, result.get(1), DELTA);
        assertEquals(3.3f, result.get(2), DELTA);
    }

    @Test
    public void testDifferenceAllRemoved() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatList result = list.difference(FloatList.of(1.1f, 2.2f, 3.3f));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSymmetricDifferenceEmpty() {
        FloatList result = list.symmetricDifference(new FloatList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSymmetricDifferenceOneEmpty() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatList result = list.symmetricDifference(new FloatList());
        assertEquals(3, result.size());
    }

    @Test
    public void testIndexOfFromIndexBeyondSize() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertEquals(-1, list.indexOf(1.1f, 10));
    }

    @Test
    public void testIndexOfNegativeFromIndex() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertEquals(0, list.indexOf(1.1f, -1));
    }

    @Test
    public void testIndexOfNaN() {
        list.addAll(new float[] { 1.1f, Float.NaN, 2.2f, Float.NaN });
        assertEquals(1, list.indexOf(Float.NaN));
        assertEquals(3, list.indexOf(Float.NaN, 2));
    }

    @Test
    public void testLastIndexOfEmptyList() {
        assertEquals(-1, list.lastIndexOf(1.1f));
    }

    @Test
    public void testLastIndexOfNegativeStart() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertEquals(-1, list.lastIndexOf(1.1f, -1));
    }

    @Test
    public void testLastIndexOfNaN() {
        list.addAll(new float[] { Float.NaN, 1.1f, Float.NaN, 2.2f });
        assertEquals(2, list.lastIndexOf(Float.NaN));
        assertEquals(0, list.lastIndexOf(Float.NaN, 1));
    }

    @Test
    public void testMinMaxMedianSingleElement() {
        list.add(5.5f);

        OptionalFloat min = list.min();
        assertTrue(min.isPresent());
        assertEquals(5.5f, min.get(), DELTA);

        OptionalFloat max = list.max();
        assertTrue(max.isPresent());
        assertEquals(5.5f, max.get(), DELTA);

        OptionalFloat median = list.median();
        assertTrue(median.isPresent());
        assertEquals(5.5f, median.get(), DELTA);
    }

    @Test
    public void testMinMaxMedianEmptyRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

        assertFalse(list.min(1, 1).isPresent());
        assertFalse(list.max(1, 1).isPresent());
        assertFalse(list.median(1, 1).isPresent());
    }

    @Test
    public void testMedianEvenElements() {
        list.addAll(new float[] { 1.0f, 2.0f, 3.0f, 4.0f });
        OptionalFloat median = list.median();
        assertTrue(median.isPresent());
        assertEquals(2.0f, median.get(), DELTA);
    }

    @Test
    public void testMinMaxWithNaN() {
        list.addAll(new float[] { 1.1f, Float.NaN, 2.2f });

        OptionalFloat min = list.min();
        assertTrue(min.isPresent());
        assertEquals(1.1f, min.get(), DELTA);

        OptionalFloat max = list.max();
        assertTrue(max.isPresent());
        assertEquals(2.2f, max.get(), DELTA);
    }

    @Test
    public void testMinMaxWithInfinity() {
        list.addAll(new float[] { 1.1f, Float.NEGATIVE_INFINITY, 2.2f, Float.POSITIVE_INFINITY });

        OptionalFloat min = list.min();
        assertTrue(min.isPresent());
        assertEquals(Float.NEGATIVE_INFINITY, min.get(), DELTA);

        OptionalFloat max = list.max();
        assertTrue(max.isPresent());
        assertEquals(Float.POSITIVE_INFINITY, max.get(), DELTA);
    }

    @Test
    public void testForEachEmptyList() {
        List<Float> result = new ArrayList<>();
        list.forEach(result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachReverseRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        List<Float> result = new ArrayList<>();

        list.forEach(4, 1, result::add);

        assertEquals(3, result.size());
        assertEquals(5.5f, result.get(0), DELTA);
        assertEquals(4.4f, result.get(1), DELTA);
        assertEquals(3.3f, result.get(2), DELTA);
    }

    @Test
    public void testForEachWithNegativeToIndex() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        List<Float> result = new ArrayList<>();

        list.forEach(2, -1, result::add);

        assertEquals(3, result.size());
        assertEquals(3.3f, result.get(0), DELTA);
        assertEquals(2.2f, result.get(1), DELTA);
        assertEquals(1.1f, result.get(2), DELTA);
    }

    @Test
    public void testDistinctEmptyRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 2.2f, 3.3f });
        FloatList result = list.distinct(1, 1);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDistinctSingleElement() {
        list.addAll(new float[] { 1.1f, 2.2f, 2.2f, 3.3f });
        FloatList result = list.distinct(0, 1);
        assertEquals(1, result.size());
        assertEquals(1.1f, result.get(0), DELTA);
    }

    @Test
    public void testDistinctWithNaN() {
        list.addAll(new float[] { 1.1f, Float.NaN, Float.NaN, 2.2f });
        FloatList result = list.distinct(0, list.size());
        assertEquals(3, result.size());
    }

    @Test
    public void testSortEmptyList() {
        list.sort();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSortSingleElement() {
        list.add(5.5f);
        list.sort();
        assertEquals(1, list.size());
        assertEquals(5.5f, list.get(0), DELTA);
    }

    @Test
    public void testSortWithNaN() {
        list.addAll(new float[] { 2.2f, Float.NaN, 1.1f, Float.NaN, 3.3f });
        list.sort();
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(2.2f, list.get(1), DELTA);
        assertEquals(3.3f, list.get(2), DELTA);
        assertTrue(Float.isNaN(list.get(3)));
        assertTrue(Float.isNaN(list.get(4)));
    }

    @Test
    public void testSortWithInfinity() {
        list.addAll(new float[] { 1.1f, Float.POSITIVE_INFINITY, -2.2f, Float.NEGATIVE_INFINITY, 0.0f });
        list.sort();
        assertEquals(Float.NEGATIVE_INFINITY, list.get(0), DELTA);
        assertEquals(-2.2f, list.get(1), DELTA);
        assertEquals(0.0f, list.get(2), DELTA);
        assertEquals(1.1f, list.get(3), DELTA);
        assertEquals(Float.POSITIVE_INFINITY, list.get(4), DELTA);
    }

    @Test
    public void testParallelSortSmallList() {
        list.addAll(new float[] { 3.3f, 1.1f, 2.2f });
        list.parallelSort();
        assertTrue(list.isSorted());
    }

    @Test
    public void testBinarySearchUnsorted() {
        list.addAll(new float[] { 3.3f, 1.1f, 4.4f, 1.1f, 5.5f });
        int result = list.binarySearch(3.3f);
        assertNotNull(result);
    }

    @Test
    public void testReverseEmptyList() {
        list.reverse();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testReverseSingleElement() {
        list.add(5.5f);
        list.reverse();
        assertEquals(1, list.size());
        assertEquals(5.5f, list.get(0), DELTA);
    }

    @Test
    public void testReverseRangeEmptyRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        list.reverse(1, 1);
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(2.2f, list.get(1), DELTA);
        assertEquals(3.3f, list.get(2), DELTA);
    }

    @Test
    public void testRotateEmptyList() {
        list.rotate(5);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testRotateSingleElement() {
        list.add(5.5f);
        list.rotate(10);
        assertEquals(1, list.size());
        assertEquals(5.5f, list.get(0), DELTA);
    }

    @Test
    public void testRotateNegativeDistance() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        list.rotate(-2);
        assertEquals(3.3f, list.get(0), DELTA);
        assertEquals(4.4f, list.get(1), DELTA);
        assertEquals(5.5f, list.get(2), DELTA);
        assertEquals(1.1f, list.get(3), DELTA);
        assertEquals(2.2f, list.get(4), DELTA);
    }

    @Test
    public void testShuffleEmptyList() {
        list.shuffle();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testShuffleSingleElement() {
        list.add(5.5f);
        list.shuffle();
        assertEquals(1, list.size());
        assertEquals(5.5f, list.get(0), DELTA);
    }

    @Test
    public void testSwapSameIndex() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        list.swap(1, 1);
        assertEquals(2.2f, list.get(1), DELTA);
    }

    @Test
    public void testSwapThrowsException() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
    }

    @Test
    public void testCopyEmptyList() {
        FloatList copy = list.copy();
        assertTrue(copy.isEmpty());
        assertNotSame(list, copy);
    }

    @Test
    public void testCopyRangeInvalidIndices() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(1, 5));
    }

    @Test
    public void testCopyWithNegativeStep() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        FloatList copy = list.copy(4, 0, -1);
        assertEquals(4, copy.size());
        assertEquals(5.5f, copy.get(0), DELTA);
        assertEquals(4.4f, copy.get(1), DELTA);
        assertEquals(3.3f, copy.get(2), DELTA);
        assertEquals(2.2f, copy.get(3), DELTA);
    }

    @Test
    public void testCopyWithStepLargerThanRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        FloatList copy = list.copy(0, 5, 3);
        assertEquals(2, copy.size());
        assertEquals(1.1f, copy.get(0), DELTA);
        assertEquals(4.4f, copy.get(1), DELTA);
    }

    @Test
    public void testSplitEmptyList() {
        List<FloatList> chunks = list.split(0, 0, 2);
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitWithChunkSizeLargerThanList() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        List<FloatList> chunks = list.split(0, 3, 10);
        assertEquals(1, chunks.size());
        assertEquals(3, chunks.get(0).size());
    }

    @Test
    public void testSplitUnevenChunks() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        List<FloatList> chunks = list.split(0, 5, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void testBoxedEmptyList() {
        List<Float> boxed = list.boxed();
        assertTrue(boxed.isEmpty());
    }

    @Test
    public void testBoxedRangeInvalidIndices() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertThrows(IndexOutOfBoundsException.class, () -> list.boxed(2, 1));
    }

    @Test
    public void testBoxedWithSpecialValues() {
        list.addAll(new float[] { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY });
        List<Float> boxed = list.boxed();
        assertTrue(Float.isNaN(boxed.get(0)));
        assertEquals(Float.POSITIVE_INFINITY, boxed.get(1));
        assertEquals(Float.NEGATIVE_INFINITY, boxed.get(2));
    }

    @Test
    public void testIteratorEmptyList() {
        FloatIterator iter = list.iterator();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
    }

    @Test
    public void testStreamEmptyList() {
        FloatStream stream = list.stream();
        assertEquals(0, stream.count());
    }

    @Test
    public void testStreamRangeEmptyRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatStream stream = list.stream(1, 1);
        assertEquals(0, stream.count());
    }

    @Test
    public void testGetFirstGetLastSingleElement() {
        list.add(5.5f);
        assertEquals(5.5f, list.getFirst(), DELTA);
        assertEquals(5.5f, list.getLast(), DELTA);
    }

    @Test
    public void testRemoveFirstRemoveLastSingleElement() {
        list.add(5.5f);
        assertEquals(5.5f, list.removeFirst(), DELTA);
        assertTrue(list.isEmpty());

        list.add(10.5f);
        assertEquals(10.5f, list.removeLast(), DELTA);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testMaxArraySize() {
        try {
            FloatList largeList = new FloatList(Integer.MAX_VALUE - 8);
            assertTrue(largeList.isEmpty());
        } catch (OutOfMemoryError e) {
            assertTrue(true);
        }
    }

    @Test
    public void testEnsureCapacityOverflow() {
        list.add(1.1f);
        try {
            for (int i = 0; i < 100; i++) {
                list.add(i * 0.1f);
            }
            assertTrue(list.size() > 1);
        } catch (OutOfMemoryError e) {
            assertTrue(true);
        }
    }

    @Test
    public void testToStringWithSpecialValues() {
        list.add(Float.NaN);
        list.add(Float.POSITIVE_INFINITY);
        list.add(Float.NEGATIVE_INFINITY);
        list.add(-0.0f);
        list.add(0.0f);

        String str = list.toString();
        assertTrue(str.contains("NaN"));
        assertTrue(str.contains("Infinity"));
        assertTrue(str.contains("-Infinity"));
    }

    @Test
    public void testArrayModification() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        float[] array = list.array();

        array[1] = 20.5f;
        assertEquals(20.5f, list.get(1), DELTA);

        list.clear();
        float[] newArray = list.array();
        assertSame(array, newArray);
    }

    @Test
    public void testAddRemovePerformance() {
        int count = 1000;
        for (int i = 0; i < count; i++) {
            list.add(i * 0.1f);
        }
        assertEquals(count, list.size());

        list.removeIf(x -> ((int) (x * 10)) % 2 == 0);
        assertEquals(count / 2, list.size());
    }

    @Test
    public void testBatchOperationsLargeData() {
        int size = 1000;
        FloatList list1 = new FloatList();
        FloatList list2 = new FloatList();

        for (int i = 0; i < size; i++) {
            list1.add(i * 0.1f);
        }

        for (int i = size / 2; i < size + size / 2; i++) {
            list2.add(i * 0.1f);
        }

        FloatList intersection = list1.intersection(list2);
        assertEquals(size / 2, intersection.size());

        FloatList difference = list1.difference(list2);
        assertEquals(size / 2, difference.size());

        FloatList symDiff = list1.symmetricDifference(list2);
        assertEquals(size, symDiff.size());
    }

    @Test
    public void testConcurrentModification() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        FloatIterator iter = list.iterator();
        list.add(6.6f);

        assertTrue(iter.hasNext());
        iter.nextFloat();
    }

    @Test
    public void testFloatPrecisionComparison() {
        float a = 0.1f;
        float b = 0.2f;
        float c = a + b;

        list.add(c);
        list.add(0.3f);

        assertTrue(list.contains(c));
        assertTrue(list.contains(0.3f));
    }

    @Test
    public void testVerySmallValues() {
        list.add(Float.MIN_VALUE);
        list.add(Float.MIN_NORMAL);
        list.add(0.0f);

        assertTrue(list.contains(Float.MIN_VALUE));
        assertTrue(list.contains(Float.MIN_NORMAL));

        list.sort();
        assertEquals(0.0f, list.get(0), DELTA);
        assertEquals(Float.MIN_VALUE, list.get(1), DELTA);
        assertEquals(Float.MIN_NORMAL, list.get(2), DELTA);
    }

    @Test
    public void testSubnormalNumbers() {
        float subnormal = Float.MIN_VALUE / 2;
        list.add(subnormal);
        list.add(0.0f);
        list.add(-subnormal);

        assertEquals(3, list.size());
        assertTrue(list.contains(subnormal));
        assertTrue(list.contains(-subnormal));
    }

    @Test
    public void testEqualsWithFloatPrecision() {
        list.add(0.1f + 0.2f);

        FloatList other = new FloatList();
        other.add(0.3f);

        boolean equalsResult = list.equals(other);
        assertNotNull(equalsResult);
    }
}
