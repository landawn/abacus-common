package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.stream.FloatStream;

@Tag("new-test")
public class FloatList100Test extends TestBase {

    private FloatList list;
    private static final float DELTA = 0.0001f;

    @BeforeEach
    public void setUp() {
        list = new FloatList();
    }

    @Test
    public void testDefaultConstructor() {
        FloatList list = new FloatList();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testConstructorWithCapacity() {
        FloatList list = new FloatList(10);
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testConstructorWithArray() {
        float[] array = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f };
        FloatList list = new FloatList(array);
        assertEquals(5, list.size());
        for (int i = 0; i < array.length; i++) {
            assertEquals(array[i], list.get(i), DELTA);
        }
    }

    @Test
    public void testConstructorWithArrayAndSize() {
        float[] array = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f };
        FloatList list = new FloatList(array, 3);
        assertEquals(3, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(2.2f, list.get(1), DELTA);
        assertEquals(3.3f, list.get(2), DELTA);
    }

    @Test
    public void testConstructorWithArrayAndSizeThrowsException() {
        float[] array = { 1.1f, 2.2f, 3.3f };
        assertThrows(IndexOutOfBoundsException.class, () -> new FloatList(array, 4));
    }

    @Test
    public void testConstructorWithNullArray() {
        assertThrows(NullPointerException.class, () -> new FloatList(null));
    }

    @Test
    public void testOf() {
        FloatList list = FloatList.of(1.1f, 2.2f, 3.3f, 4.4f, 5.5f);
        assertEquals(5, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(5.5f, list.get(4), DELTA);
    }

    @Test
    public void testOfWithEmptyArray() {
        FloatList list = FloatList.of();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOfWithNull() {
        FloatList list = FloatList.of((float[]) null);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOfWithArrayAndSize() {
        float[] array = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f };
        FloatList list = FloatList.of(array, 3);
        assertEquals(3, list.size());
    }

    @Test
    public void testCopyOf() {
        float[] array = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f };
        FloatList list = FloatList.copyOf(array);
        assertEquals(5, list.size());
        array[0] = 100.0f;
        assertEquals(1.1f, list.get(0), DELTA);
    }

    @Test
    public void testCopyOfRange() {
        float[] array = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f };
        FloatList list = FloatList.copyOf(array, 1, 4);
        assertEquals(3, list.size());
        assertEquals(2.2f, list.get(0), DELTA);
        assertEquals(4.4f, list.get(2), DELTA);
    }

    @Test
    public void testRepeat() {
        FloatList list = FloatList.repeat(7.7f, 5);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(7.7f, list.get(i), DELTA);
        }
    }

    @Test
    public void testRandom() {
        FloatList list = FloatList.random(10);
        assertEquals(10, list.size());
        for (int i = 0; i < 10; i++) {
            float value = list.get(i);
            assertTrue(value >= 0.0f && value < 1.0f);
        }
    }

    @Test
    public void testGet() {
        list.add(10.5f);
        list.add(20.5f);
        assertEquals(10.5f, list.get(0), DELTA);
        assertEquals(20.5f, list.get(1), DELTA);
    }

    @Test
    public void testGetThrowsException() {
        list.add(10.5f);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
    }

    @Test
    public void testSet() {
        list.add(10.5f);
        list.add(20.5f);
        float oldValue = list.set(1, 30.5f);
        assertEquals(20.5f, oldValue, DELTA);
        assertEquals(30.5f, list.get(1), DELTA);
    }

    @Test
    public void testSetThrowsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(0, 10.5f));
    }

    @Test
    public void testAdd() {
        list.add(10.5f);
        assertEquals(1, list.size());
        assertEquals(10.5f, list.get(0), DELTA);

        list.add(20.5f);
        assertEquals(2, list.size());
        assertEquals(20.5f, list.get(1), DELTA);
    }

    @Test
    public void testAddAtIndex() {
        list.add(10.5f);
        list.add(30.5f);
        list.add(1, 20.5f);

        assertEquals(3, list.size());
        assertEquals(10.5f, list.get(0), DELTA);
        assertEquals(20.5f, list.get(1), DELTA);
        assertEquals(30.5f, list.get(2), DELTA);
    }

    @Test
    public void testAddAtIndexThrowsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(1, 10.5f));
    }

    @Test
    public void testAddAll() {
        list.add(1.1f);
        list.add(2.2f);

        FloatList other = FloatList.of(3.3f, 4.4f, 5.5f);
        boolean result = list.addAll(other);

        assertTrue(result);
        assertEquals(5, list.size());
        assertEquals(3.3f, list.get(2), DELTA);
        assertEquals(5.5f, list.get(4), DELTA);
    }

    @Test
    public void testAddAllEmpty() {
        list.add(1.1f);
        FloatList empty = new FloatList();
        boolean result = list.addAll(empty);

        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testAddAllAtIndex() {
        list.add(1.1f);
        list.add(4.4f);

        FloatList other = FloatList.of(2.2f, 3.3f);
        boolean result = list.addAll(1, other);

        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(2.2f, list.get(1), DELTA);
        assertEquals(3.3f, list.get(2), DELTA);
        assertEquals(4.4f, list.get(3), DELTA);
    }

    @Test
    public void testAddAllArray() {
        list.add(1.1f);
        float[] array = { 2.2f, 3.3f, 4.4f };
        boolean result = list.addAll(array);

        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(4.4f, list.get(3), DELTA);
    }

    @Test
    public void testAddAllArrayAtIndex() {
        list.add(1.1f);
        list.add(4.4f);
        float[] array = { 2.2f, 3.3f };
        boolean result = list.addAll(1, array);

        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(2.2f, list.get(1), DELTA);
        assertEquals(3.3f, list.get(2), DELTA);
    }

    @Test
    public void testRemove() {
        list.add(10.5f);
        list.add(20.5f);
        list.add(30.5f);

        boolean result = list.remove(20.5f);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(10.5f, list.get(0), DELTA);
        assertEquals(30.5f, list.get(1), DELTA);
    }

    @Test
    public void testRemoveNotFound() {
        list.add(10.5f);
        boolean result = list.remove(20.5f);
        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveAllOccurrences() {
        list.add(10.5f);
        list.add(20.5f);
        list.add(10.5f);
        list.add(30.5f);
        list.add(10.5f);

        boolean result = list.removeAllOccurrences(10.5f);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(20.5f, list.get(0), DELTA);
        assertEquals(30.5f, list.get(1), DELTA);
    }

    @Test
    public void testRemoveAll() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        FloatList toRemove = FloatList.of(2.2f, 4.4f);

        boolean result = list.removeAll(toRemove);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(3.3f, list.get(1), DELTA);
        assertEquals(5.5f, list.get(2), DELTA);
    }

    @Test
    public void testRemoveAllArray() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        float[] toRemove = { 2.2f, 4.4f };

        boolean result = list.removeAll(toRemove);
        assertTrue(result);
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveIf() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        boolean result = list.removeIf(x -> x > 3.0f);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(2.2f, list.get(1), DELTA);
    }

    @Test
    public void testRemoveDuplicates() {
        list.addAll(new float[] { 1.1f, 2.2f, 2.2f, 3.3f, 3.3f, 3.3f, 4.4f });

        boolean result = list.removeDuplicates();
        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(2.2f, list.get(1), DELTA);
        assertEquals(3.3f, list.get(2), DELTA);
        assertEquals(4.4f, list.get(3), DELTA);
    }

    @Test
    public void testRetainAll() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        FloatList toRetain = FloatList.of(2.2f, 4.4f, 6.6f);

        boolean result = list.retainAll(toRetain);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(2.2f, list.get(0), DELTA);
        assertEquals(4.4f, list.get(1), DELTA);
    }

    @Test
    public void testDelete() {
        list.addAll(new float[] { 10.5f, 20.5f, 30.5f });

        float deleted = list.delete(1);
        assertEquals(20.5f, deleted, DELTA);
        assertEquals(2, list.size());
        assertEquals(10.5f, list.get(0), DELTA);
        assertEquals(30.5f, list.get(1), DELTA);
    }

    @Test
    public void testDeleteAllByIndices() {
        list.addAll(new float[] { 10.5f, 20.5f, 30.5f, 40.5f, 50.5f });

        list.deleteAllByIndices(1, 3);
        assertEquals(3, list.size());
        assertEquals(10.5f, list.get(0), DELTA);
        assertEquals(30.5f, list.get(1), DELTA);
        assertEquals(50.5f, list.get(2), DELTA);
    }

    @Test
    public void testDeleteRange() {
        list.addAll(new float[] { 10.5f, 20.5f, 30.5f, 40.5f, 50.5f });

        list.deleteRange(1, 3);
        assertEquals(3, list.size());
        assertEquals(10.5f, list.get(0), DELTA);
        assertEquals(40.5f, list.get(1), DELTA);
        assertEquals(50.5f, list.get(2), DELTA);
    }

    @Test
    public void testMoveRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        list.moveRange(1, 3, 3);
        assertEquals(5, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(4.4f, list.get(1), DELTA);
        assertEquals(5.5f, list.get(2), DELTA);
        assertEquals(2.2f, list.get(3), DELTA);
        assertEquals(3.3f, list.get(4), DELTA);
    }

    @Test
    public void testReplaceRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        FloatList replacement = FloatList.of(10.1f, 20.2f);

        list.replaceRange(1, 3, replacement);
        assertEquals(5, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(10.1f, list.get(1), DELTA);
        assertEquals(20.2f, list.get(2), DELTA);
        assertEquals(4.4f, list.get(3), DELTA);
        assertEquals(5.5f, list.get(4), DELTA);
    }

    @Test
    public void testReplaceRangeWithArray() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        float[] replacement = { 10.1f, 20.2f };

        list.replaceRange(1, 3, replacement);
        assertEquals(5, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(10.1f, list.get(1), DELTA);
        assertEquals(20.2f, list.get(2), DELTA);
    }

    @Test
    public void testReplaceAll() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 2.2f, 5.5f });

        int count = list.replaceAll(2.2f, 20.2f);
        assertEquals(2, count);
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(20.2f, list.get(1), DELTA);
        assertEquals(3.3f, list.get(2), DELTA);
        assertEquals(20.2f, list.get(3), DELTA);
        assertEquals(5.5f, list.get(4), DELTA);
    }

    @Test
    public void testReplaceAllWithOperator() {
        list.addAll(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f });

        list.replaceAll(x -> x * 2);
        assertEquals(2.0f, list.get(0), DELTA);
        assertEquals(4.0f, list.get(1), DELTA);
        assertEquals(10.0f, list.get(4), DELTA);
    }

    @Test
    public void testReplaceIf() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        boolean result = list.replaceIf(x -> x > 3.0f, 0.0f);
        assertTrue(result);
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(2.2f, list.get(1), DELTA);
        assertEquals(0.0f, list.get(2), DELTA);
        assertEquals(0.0f, list.get(3), DELTA);
        assertEquals(0.0f, list.get(4), DELTA);
    }

    @Test
    public void testFill() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        list.fill(10.0f);
        for (int i = 0; i < list.size(); i++) {
            assertEquals(10.0f, list.get(i), DELTA);
        }
    }

    @Test
    public void testFillRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        list.fill(1, 4, 10.0f);
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(10.0f, list.get(1), DELTA);
        assertEquals(10.0f, list.get(2), DELTA);
        assertEquals(10.0f, list.get(3), DELTA);
        assertEquals(5.5f, list.get(4), DELTA);
    }

    @Test
    public void testContains() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        assertTrue(list.contains(3.3f));
        assertFalse(list.contains(6.6f));
    }

    @Test
    public void testContainsAny() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        FloatList other = FloatList.of(6.6f, 7.7f, 3.3f);

        assertTrue(list.containsAny(other));
    }

    @Test
    public void testContainsAnyArray() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        float[] other = { 6.6f, 7.7f, 3.3f };

        assertTrue(list.containsAny(other));
    }

    @Test
    public void testContainsAll() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        FloatList other = FloatList.of(2.2f, 4.4f);

        assertTrue(list.containsAll(other));

        other = FloatList.of(2.2f, 6.6f);
        assertFalse(list.containsAll(other));
    }

    @Test
    public void testDisjoint() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatList other = FloatList.of(4.4f, 5.5f, 6.6f);

        assertTrue(list.disjoint(other));

        other = FloatList.of(3.3f, 4.4f, 5.5f);
        assertFalse(list.disjoint(other));
    }

    @Test
    public void testIntersection() {
        list.addAll(new float[] { 1.1f, 2.2f, 2.2f, 3.3f, 4.4f });
        FloatList other = FloatList.of(2.2f, 3.3f, 5.5f, 2.2f);

        FloatList result = list.intersection(other);
        assertEquals(3, result.size());
        assertEquals(2.2f, result.get(0), DELTA);
        assertEquals(2.2f, result.get(1), DELTA);
        assertEquals(3.3f, result.get(2), DELTA);
    }

    @Test
    public void testDifference() {
        list.addAll(new float[] { 1.1f, 2.2f, 2.2f, 3.3f, 4.4f });
        FloatList other = FloatList.of(2.2f, 3.3f);

        FloatList result = list.difference(other);
        assertEquals(3, result.size());
        assertEquals(1.1f, result.get(0), DELTA);
        assertEquals(2.2f, result.get(1), DELTA);
        assertEquals(4.4f, result.get(2), DELTA);
    }

    @Test
    public void testSymmetricDifference() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatList other = FloatList.of(3.3f, 4.4f, 5.5f);

        FloatList result = list.symmetricDifference(other);
        assertEquals(4, result.size());
        assertTrue(result.contains(1.1f));
        assertTrue(result.contains(2.2f));
        assertTrue(result.contains(4.4f));
        assertTrue(result.contains(5.5f));
    }

    @Test
    public void testOccurrencesOf() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 2.2f, 2.2f, 4.4f });

        assertEquals(3, list.occurrencesOf(2.2f));
        assertEquals(1, list.occurrencesOf(1.1f));
        assertEquals(0, list.occurrencesOf(5.5f));
    }

    @Test
    public void testIndexOf() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 2.2f, 5.5f });

        assertEquals(1, list.indexOf(2.2f));
        assertEquals(2, list.indexOf(3.3f));
        assertEquals(-1, list.indexOf(6.6f));
    }

    @Test
    public void testIndexOfWithFromIndex() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 2.2f, 5.5f });

        assertEquals(3, list.indexOf(2.2f, 2));
        assertEquals(-1, list.indexOf(2.2f, 4));
    }

    @Test
    public void testLastIndexOf() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 2.2f, 5.5f });

        assertEquals(3, list.lastIndexOf(2.2f));
        assertEquals(0, list.lastIndexOf(1.1f));
        assertEquals(-1, list.lastIndexOf(6.6f));
    }

    @Test
    public void testLastIndexOfWithStart() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 2.2f, 5.5f });

        assertEquals(1, list.lastIndexOf(2.2f, 2));
        assertEquals(-1, list.lastIndexOf(5.5f, 3));
    }

    @Test
    public void testMin() {
        list.addAll(new float[] { 3.3f, 1.1f, 4.4f, 1.1f, 5.5f });

        OptionalFloat min = list.min();
        assertTrue(min.isPresent());
        assertEquals(1.1f, min.get(), DELTA);
    }

    @Test
    public void testMinEmpty() {
        OptionalFloat min = list.min();
        assertFalse(min.isPresent());
    }

    @Test
    public void testMinRange() {
        list.addAll(new float[] { 3.3f, 1.1f, 4.4f, 1.1f, 5.5f });

        OptionalFloat min = list.min(2, 4);
        assertTrue(min.isPresent());
        assertEquals(1.1f, min.get(), DELTA);
    }

    @Test
    public void testMax() {
        list.addAll(new float[] { 3.3f, 1.1f, 4.4f, 1.1f, 5.5f });

        OptionalFloat max = list.max();
        assertTrue(max.isPresent());
        assertEquals(5.5f, max.get(), DELTA);
    }

    @Test
    public void testMedian() {
        list.addAll(new float[] { 3.3f, 1.1f, 4.4f, 1.1f, 5.5f });

        OptionalFloat median = list.median();
        assertTrue(median.isPresent());
        assertEquals(3.3f, median.get(), DELTA);
    }

    @Test
    public void testForEach() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        List<Float> result = new ArrayList<>();

        list.forEach(result::add);

        assertEquals(3, result.size());
        assertEquals(1.1f, result.get(0), DELTA);
        assertEquals(2.2f, result.get(1), DELTA);
        assertEquals(3.3f, result.get(2), DELTA);
    }

    @Test
    public void testForEachRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        List<Float> result = new ArrayList<>();

        list.forEach(1, 4, result::add);

        assertEquals(3, result.size());
        assertEquals(2.2f, result.get(0), DELTA);
        assertEquals(3.3f, result.get(1), DELTA);
        assertEquals(4.4f, result.get(2), DELTA);
    }

    @Test
    public void testFirst() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

        OptionalFloat first = list.first();
        assertTrue(first.isPresent());
        assertEquals(1.1f, first.get(), DELTA);
    }

    @Test
    public void testFirstEmpty() {
        OptionalFloat first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

        OptionalFloat last = list.last();
        assertTrue(last.isPresent());
        assertEquals(3.3f, last.get(), DELTA);
    }

    @Test
    public void testGetFirst() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertEquals(1.1f, list.getFirst(), DELTA);
    }

    @Test
    public void testGetFirstThrowsException() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLast() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertEquals(3.3f, list.getLast(), DELTA);
    }

    @Test
    public void testAddFirst() {
        list.addAll(new float[] { 2.2f, 3.3f });
        list.addFirst(1.1f);

        assertEquals(3, list.size());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(2.2f, list.get(1), DELTA);
    }

    @Test
    public void testAddLast() {
        list.addAll(new float[] { 1.1f, 2.2f });
        list.addLast(3.3f);

        assertEquals(3, list.size());
        assertEquals(3.3f, list.get(2), DELTA);
    }

    @Test
    public void testRemoveFirst() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

        float removed = list.removeFirst();
        assertEquals(1.1f, removed, DELTA);
        assertEquals(2, list.size());
        assertEquals(2.2f, list.get(0), DELTA);
    }

    @Test
    public void testRemoveFirstThrowsException() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLast() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

        float removed = list.removeLast();
        assertEquals(3.3f, removed, DELTA);
        assertEquals(2, list.size());
        assertEquals(2.2f, list.get(1), DELTA);
    }

    @Test
    public void testDistinct() {
        list.addAll(new float[] { 1.1f, 2.2f, 2.2f, 3.3f, 3.3f, 3.3f });

        FloatList distinct = list.distinct(0, list.size());
        assertEquals(3, distinct.size());
        assertEquals(1.1f, distinct.get(0), DELTA);
        assertEquals(2.2f, distinct.get(1), DELTA);
        assertEquals(3.3f, distinct.get(2), DELTA);
    }

    @Test
    public void testHasDuplicates() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertFalse(list.hasDuplicates());

        list.add(2.2f);
        assertTrue(list.hasDuplicates());
    }

    @Test
    public void testIsSorted() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        assertTrue(list.isSorted());

        list.set(2, 1.0f);
        assertFalse(list.isSorted());
    }

    @Test
    public void testSort() {
        list.addAll(new float[] { 3.3f, 1.1f, 4.4f, 1.1f, 5.5f });

        list.sort();
        assertTrue(list.isSorted());
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(1.1f, list.get(1), DELTA);
        assertEquals(5.5f, list.get(4), DELTA);
    }

    @Test
    public void testParallelSort() {
        list.addAll(new float[] { 3.3f, 1.1f, 4.4f, 1.1f, 5.5f });

        list.parallelSort();
        assertTrue(list.isSorted());
    }

    @Test
    public void testReverseSort() {
        list.addAll(new float[] { 3.3f, 1.1f, 4.4f, 1.1f, 5.5f });

        list.reverseSort();
        assertEquals(5.5f, list.get(0), DELTA);
        assertEquals(4.4f, list.get(1), DELTA);
        assertEquals(1.1f, list.get(4), DELTA);
    }

    @Test
    public void testBinarySearch() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        assertEquals(2, list.binarySearch(3.3f));
        assertTrue(list.binarySearch(6.6f) < 0);
    }

    @Test
    public void testBinarySearchRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        assertEquals(3, list.binarySearch(1, 5, 4.4f));
        assertTrue(list.binarySearch(1, 3, 4.4f) < 0);
    }

    @Test
    public void testReverse() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        list.reverse();
        assertEquals(5.5f, list.get(0), DELTA);
        assertEquals(4.4f, list.get(1), DELTA);
        assertEquals(1.1f, list.get(4), DELTA);
    }

    @Test
    public void testReverseRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        list.reverse(1, 4);
        assertEquals(1.1f, list.get(0), DELTA);
        assertEquals(4.4f, list.get(1), DELTA);
        assertEquals(3.3f, list.get(2), DELTA);
        assertEquals(2.2f, list.get(3), DELTA);
        assertEquals(5.5f, list.get(4), DELTA);
    }

    @Test
    public void testRotate() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        list.rotate(2);
        assertEquals(4.4f, list.get(0), DELTA);
        assertEquals(5.5f, list.get(1), DELTA);
        assertEquals(1.1f, list.get(2), DELTA);
    }

    @Test
    public void testShuffle() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        FloatList original = list.copy();

        list.shuffle();
        assertEquals(original.size(), list.size());

        for (int i = 0; i < original.size(); i++) {
            assertTrue(list.contains(original.get(i)));
        }
    }

    @Test
    public void testShuffleWithRandom() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        Random rnd = new Random(42);

        list.shuffle(rnd);
        assertEquals(5, list.size());
    }

    @Test
    public void testSwap() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

        list.swap(0, 2);
        assertEquals(3.3f, list.get(0), DELTA);
        assertEquals(2.2f, list.get(1), DELTA);
        assertEquals(1.1f, list.get(2), DELTA);
    }

    @Test
    public void testCopy() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

        FloatList copy = list.copy();
        assertEquals(list.size(), copy.size());

        list.set(0, 10.0f);
        assertEquals(1.1f, copy.get(0), DELTA);
    }

    @Test
    public void testCopyRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        FloatList copy = list.copy(1, 4);
        assertEquals(3, copy.size());
        assertEquals(2.2f, copy.get(0), DELTA);
        assertEquals(4.4f, copy.get(2), DELTA);
    }

    @Test
    public void testCopyWithStep() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        FloatList copy = list.copy(0, 5, 2);
        assertEquals(3, copy.size());
        assertEquals(1.1f, copy.get(0), DELTA);
        assertEquals(3.3f, copy.get(1), DELTA);
        assertEquals(5.5f, copy.get(2), DELTA);
    }

    @Test
    public void testSplit() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f, 6.6f });

        List<FloatList> chunks = list.split(0, 6, 2);
        assertEquals(3, chunks.size());

        assertEquals(2, chunks.get(0).size());
        assertEquals(1.1f, chunks.get(0).get(0), DELTA);
        assertEquals(2.2f, chunks.get(0).get(1), DELTA);

        assertEquals(2, chunks.get(2).size());
        assertEquals(5.5f, chunks.get(2).get(0), DELTA);
        assertEquals(6.6f, chunks.get(2).get(1), DELTA);
    }

    @Test
    public void testTrimToSize() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        list.trimToSize();
        assertEquals(3, list.size());
    }

    @Test
    public void testClear() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(list.isEmpty());

        list.add(1.1f);
        assertFalse(list.isEmpty());
    }

    @Test
    public void testSize() {
        assertEquals(0, list.size());

        list.add(1.1f);
        assertEquals(1, list.size());

        list.add(2.2f);
        assertEquals(2, list.size());
    }

    @Test
    public void testBoxed() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

        List<Float> boxed = list.boxed();
        assertEquals(3, boxed.size());
        assertEquals(Float.valueOf(1.1f), boxed.get(0));
        assertEquals(Float.valueOf(3.3f), boxed.get(2));
    }

    @Test
    public void testBoxedRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        List<Float> boxed = list.boxed(1, 4);
        assertEquals(3, boxed.size());
        assertEquals(Float.valueOf(2.2f), boxed.get(0));
        assertEquals(Float.valueOf(4.4f), boxed.get(2));
    }

    @Test
    public void testToArray() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

        float[] array = list.toArray();
        assertEquals(3, array.length);
        assertEquals(1.1f, array[0], DELTA);
        assertEquals(3.3f, array[2], DELTA);

        array[0] = 10.0f;
        assertEquals(1.1f, list.get(0), DELTA);
    }

    @Test
    public void testToDoubleList() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

        DoubleList doubleList = list.toDoubleList();
        assertEquals(3, doubleList.size());
        assertEquals(1.1, doubleList.get(0), 0.0001);
        assertEquals(3.3, doubleList.get(2), 0.0001);
    }

    @Test
    public void testToCollection() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

        Set<Float> set = list.toCollection(0, 3, size -> new HashSet<>());
        assertEquals(3, set.size());
        assertTrue(set.contains(1.1f));
        assertTrue(set.contains(3.3f));
    }

    @Test
    public void testToMultiset() {
        list.addAll(new float[] { 1.1f, 2.2f, 2.2f, 3.3f });

        Multiset<Float> multiset = list.toMultiset(0, 4, size -> new Multiset<>());
        assertEquals(4, multiset.size());
        assertEquals(2, multiset.count(2.2f));
    }

    @Test
    public void testIterator() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

        FloatIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1.1f, iter.nextFloat(), DELTA);
        assertEquals(2.2f, iter.nextFloat(), DELTA);
        assertEquals(3.3f, iter.nextFloat(), DELTA);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testStream() {
        list.addAll(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f });

        FloatStream stream = list.stream();
        assertNotNull(stream);

        double sum = stream.sum();
        assertEquals(15.0, sum, 0.0001);
    }

    @Test
    public void testStreamRange() {
        list.addAll(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f });

        FloatStream stream = list.stream(1, 4);
        double sum = stream.sum();
        assertEquals(9.0, sum, 0.0001);
    }

    @Test
    public void testEquals() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

        FloatList other = FloatList.of(1.1f, 2.2f, 3.3f);
        assertTrue(list.equals(other));

        other.add(4.4f);
        assertFalse(list.equals(other));

        assertFalse(list.equals(null));
        assertFalse(list.equals("not a list"));
        assertTrue(list.equals(list));
    }

    @Test
    public void testHashCode() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatList other = FloatList.of(1.1f, 2.2f, 3.3f);

        assertEquals(list.hashCode(), other.hashCode());

        other.add(4.4f);
        assertNotEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("[]", list.toString());

        list.add(1.1f);
        list.add(2.2f);
        list.add(3.3f);
        String str = list.toString();
        assertTrue(str.contains("1.1"));
        assertTrue(str.contains("2.2"));
        assertTrue(str.contains("3.3"));
    }

    @Test
    public void testArray() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

        float[] array = list.array();
        assertEquals(1.1f, array[0], DELTA);
        assertEquals(2.2f, array[1], DELTA);
        assertEquals(3.3f, array[2], DELTA);

        array[0] = 10.0f;
        assertEquals(10.0f, list.get(0), DELTA);
    }

    @Test
    public void testLargeList() {
        int size = 10000;
        for (int i = 0; i < size; i++) {
            list.add(i * 0.1f);
        }

        assertEquals(size, list.size());
        assertEquals(0.0f, list.get(0), DELTA);
        assertEquals((size - 1) * 0.1f, list.get(size - 1), DELTA);
    }

    @Test
    public void testEmptyOperations() {
        assertFalse(list.remove(1.1f));
        assertFalse(list.removeAllOccurrences(1.1f));
        assertFalse(list.removeIf(x -> true));
        assertFalse(list.removeDuplicates());
        assertFalse(list.hasDuplicates());
        assertTrue(list.isSorted());

        list.sort();
        list.reverse();
        list.shuffle();

        assertTrue(list.isEmpty());
    }

    @Test
    public void testNaNHandling() {
        list.add(1.1f);
        list.add(Float.NaN);
        list.add(2.2f);

        assertEquals(3, list.size());
        assertTrue(Float.isNaN(list.get(1)));

        assertTrue(list.contains(Float.NaN));

        assertEquals(1, list.indexOf(Float.NaN));
    }

    @Test
    public void testInfinityHandling() {
        list.add(Float.NEGATIVE_INFINITY);
        list.add(0.0f);
        list.add(Float.POSITIVE_INFINITY);

        assertEquals(Float.NEGATIVE_INFINITY, list.get(0), DELTA);
        assertEquals(0.0f, list.get(1), DELTA);
        assertEquals(Float.POSITIVE_INFINITY, list.get(2), DELTA);

        OptionalFloat min = list.min();
        assertTrue(min.isPresent());
        assertEquals(Float.NEGATIVE_INFINITY, min.get(), DELTA);

        OptionalFloat max = list.max();
        assertTrue(max.isPresent());
        assertEquals(Float.POSITIVE_INFINITY, max.get(), DELTA);
    }

    @Test
    public void testZeroHandling() {
        list.add(-0.0f);
        list.add(0.0f);

        assertEquals(0.0f, list.get(0), DELTA);
        assertEquals(0.0f, list.get(1), DELTA);

        assertTrue(list.contains(0.0f));
        assertTrue(list.contains(-0.0f));
    }

    @Test
    public void testPrecisionEdgeCases() {
        float a = 0.1f + 0.2f;
        float b = 0.3f;

        list.add(a);
        list.add(b);

        int replaced = list.replaceAll(0.3f, 1.0f);
        assertTrue(replaced > 0);
    }
}
