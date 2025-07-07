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

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.DoubleStream;

public class DoubleList100Test extends TestBase {

    private DoubleList list;
    private static final double DELTA = 0.000001;

    @BeforeEach
    public void setUp() {
        list = new DoubleList();
    }

    // Constructor Tests
    @Test
    public void testDefaultConstructor() {
        DoubleList list = new DoubleList();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testConstructorWithCapacity() {
        DoubleList list = new DoubleList(10);
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testConstructorWithArray() {
        double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        DoubleList list = new DoubleList(array);
        assertEquals(5, list.size());
        for (int i = 0; i < array.length; i++) {
            assertEquals(array[i], list.get(i), DELTA);
        }
    }

    @Test
    public void testConstructorWithArrayAndSize() {
        double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        DoubleList list = new DoubleList(array, 3);
        assertEquals(3, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
    }

    @Test
    public void testConstructorWithArrayAndSizeThrowsException() {
        double[] array = { 1.1, 2.2, 3.3 };
        assertThrows(IndexOutOfBoundsException.class, () -> new DoubleList(array, 4));
    }

    @Test
    public void testConstructorWithNullArray() {
        assertThrows(NullPointerException.class, () -> new DoubleList(null));
    }

    // Static Factory Method Tests
    @Test
    public void testOf() {
        DoubleList list = DoubleList.of(1.1, 2.2, 3.3, 4.4, 5.5);
        assertEquals(5, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(5.5, list.get(4), DELTA);
    }

    @Test
    public void testOfWithEmptyArray() {
        DoubleList list = DoubleList.of();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOfWithNull() {
        DoubleList list = DoubleList.of((double[]) null);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOfWithArrayAndSize() {
        double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        DoubleList list = DoubleList.of(array, 3);
        assertEquals(3, list.size());
    }

    @Test
    public void testCopyOf() {
        double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        DoubleList list = DoubleList.copyOf(array);
        assertEquals(5, list.size());
        // Modify original array
        array[0] = 100.0;
        // List should not be affected
        assertEquals(1.1, list.get(0), DELTA);
    }

    @Test
    public void testCopyOfRange() {
        double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        DoubleList list = DoubleList.copyOf(array, 1, 4);
        assertEquals(3, list.size());
        assertEquals(2.2, list.get(0), DELTA);
        assertEquals(4.4, list.get(2), DELTA);
    }

    @Test
    public void testRepeat() {
        DoubleList list = DoubleList.repeat(7.7, 5);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(7.7, list.get(i), DELTA);
        }
    }

    @Test
    public void testRandom() {
        DoubleList list = DoubleList.random(10);
        assertEquals(10, list.size());
        // Check that all values are between 0.0 and 1.0
        for (int i = 0; i < 10; i++) {
            double value = list.get(i);
            assertTrue(value >= 0.0 && value < 1.0);
        }
    }

    // Get and Set Tests
    @Test
    public void testGet() {
        list.add(10.5);
        list.add(20.5);
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(20.5, list.get(1), DELTA);
    }

    @Test
    public void testGetThrowsException() {
        list.add(10.5);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
    }

    @Test
    public void testSet() {
        list.add(10.5);
        list.add(20.5);
        double oldValue = list.set(1, 30.5);
        assertEquals(20.5, oldValue, DELTA);
        assertEquals(30.5, list.get(1), DELTA);
    }

    @Test
    public void testSetThrowsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(0, 10.5));
    }

    // Add Tests
    @Test
    public void testAdd() {
        list.add(10.5);
        assertEquals(1, list.size());
        assertEquals(10.5, list.get(0), DELTA);

        list.add(20.5);
        assertEquals(2, list.size());
        assertEquals(20.5, list.get(1), DELTA);
    }

    @Test
    public void testAddAtIndex() {
        list.add(10.5);
        list.add(30.5);
        list.add(1, 20.5);

        assertEquals(3, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(20.5, list.get(1), DELTA);
        assertEquals(30.5, list.get(2), DELTA);
    }

    @Test
    public void testAddAtIndexThrowsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(1, 10.5));
    }

    @Test
    public void testAddAll() {
        list.add(1.1);
        list.add(2.2);

        DoubleList other = DoubleList.of(3.3, 4.4, 5.5);
        boolean result = list.addAll(other);

        assertTrue(result);
        assertEquals(5, list.size());
        assertEquals(3.3, list.get(2), DELTA);
        assertEquals(5.5, list.get(4), DELTA);
    }

    @Test
    public void testAddAllEmpty() {
        list.add(1.1);
        DoubleList empty = new DoubleList();
        boolean result = list.addAll(empty);

        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testAddAllAtIndex() {
        list.add(1.1);
        list.add(4.4);

        DoubleList other = DoubleList.of(2.2, 3.3);
        boolean result = list.addAll(1, other);

        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
        assertEquals(4.4, list.get(3), DELTA);
    }

    @Test
    public void testAddAllArray() {
        list.add(1.1);
        double[] array = { 2.2, 3.3, 4.4 };
        boolean result = list.addAll(array);

        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(4.4, list.get(3), DELTA);
    }

    @Test
    public void testAddAllArrayAtIndex() {
        list.add(1.1);
        list.add(4.4);
        double[] array = { 2.2, 3.3 };
        boolean result = list.addAll(1, array);

        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
    }

    // Remove Tests
    @Test
    public void testRemove() {
        list.add(10.5);
        list.add(20.5);
        list.add(30.5);

        boolean result = list.remove(20.5);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(30.5, list.get(1), DELTA);
    }

    @Test
    public void testRemoveNotFound() {
        list.add(10.5);
        boolean result = list.remove(20.5);
        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveAllOccurrences() {
        list.add(10.5);
        list.add(20.5);
        list.add(10.5);
        list.add(30.5);
        list.add(10.5);

        boolean result = list.removeAllOccurrences(10.5);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(20.5, list.get(0), DELTA);
        assertEquals(30.5, list.get(1), DELTA);
    }

    @Test
    public void testRemoveAll() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList toRemove = DoubleList.of(2.2, 4.4);

        boolean result = list.removeAll(toRemove);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(3.3, list.get(1), DELTA);
        assertEquals(5.5, list.get(2), DELTA);
    }

    @Test
    public void testRemoveAllArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        double[] toRemove = { 2.2, 4.4 };

        boolean result = list.removeAll(toRemove);
        assertTrue(result);
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveIf() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        boolean result = list.removeIf(x -> x > 3.0);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
    }

    @Test
    public void testRemoveDuplicates() {
        list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3, 3.3, 3.3, 4.4 });

        boolean result = list.removeDuplicates();
        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
        assertEquals(4.4, list.get(3), DELTA);
    }

    @Test
    public void testRetainAll() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList toRetain = DoubleList.of(2.2, 4.4, 6.6);

        boolean result = list.retainAll(toRetain);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(2.2, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
    }

    @Test
    public void testDelete() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });

        double deleted = list.delete(1);
        assertEquals(20.5, deleted, DELTA);
        assertEquals(2, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(30.5, list.get(1), DELTA);
    }

    @Test
    public void testDeleteAllByIndices() {
        list.addAll(new double[] { 10.5, 20.5, 30.5, 40.5, 50.5 });

        list.deleteAllByIndices(1, 3);
        assertEquals(3, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(30.5, list.get(1), DELTA);
        assertEquals(50.5, list.get(2), DELTA);
    }

    @Test
    public void testDeleteRange() {
        list.addAll(new double[] { 10.5, 20.5, 30.5, 40.5, 50.5 });

        list.deleteRange(1, 3);
        assertEquals(3, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(40.5, list.get(1), DELTA);
        assertEquals(50.5, list.get(2), DELTA);
    }

    // Move and Replace Tests
    @Test
    public void testMoveRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        list.moveRange(1, 3, 3);
        assertEquals(5, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
        assertEquals(5.5, list.get(2), DELTA);
        assertEquals(2.2, list.get(3), DELTA);
        assertEquals(3.3, list.get(4), DELTA);
    }

    @Test
    public void testReplaceRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList replacement = DoubleList.of(10.1, 20.2);

        list.replaceRange(1, 3, replacement);
        assertEquals(5, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(10.1, list.get(1), DELTA);
        assertEquals(20.2, list.get(2), DELTA);
        assertEquals(4.4, list.get(3), DELTA);
        assertEquals(5.5, list.get(4), DELTA);
    }

    @Test
    public void testReplaceRangeWithArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        double[] replacement = { 10.1, 20.2 };

        list.replaceRange(1, 3, replacement);
        assertEquals(5, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(10.1, list.get(1), DELTA);
        assertEquals(20.2, list.get(2), DELTA);
    }

    @Test
    public void testReplaceAll() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 5.5 });

        int count = list.replaceAll(2.2, 20.2);
        assertEquals(2, count);
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(20.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
        assertEquals(20.2, list.get(3), DELTA);
        assertEquals(5.5, list.get(4), DELTA);
    }

    @Test
    public void testReplaceAllWithOperator() {
        list.addAll(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });

        list.replaceAll(x -> x * 2);
        assertEquals(2.0, list.get(0), DELTA);
        assertEquals(4.0, list.get(1), DELTA);
        assertEquals(10.0, list.get(4), DELTA);
    }

    @Test
    public void testReplaceIf() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        boolean result = list.replaceIf(x -> x > 3.0, 0.0);
        assertTrue(result);
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(0.0, list.get(2), DELTA);
        assertEquals(0.0, list.get(3), DELTA);
        assertEquals(0.0, list.get(4), DELTA);
    }

    // Fill Tests
    @Test
    public void testFill() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        list.fill(10.0);
        for (int i = 0; i < list.size(); i++) {
            assertEquals(10.0, list.get(i), DELTA);
        }
    }

    @Test
    public void testFillRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        list.fill(1, 4, 10.0);
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(10.0, list.get(1), DELTA);
        assertEquals(10.0, list.get(2), DELTA);
        assertEquals(10.0, list.get(3), DELTA);
        assertEquals(5.5, list.get(4), DELTA);
    }

    // Contains Tests
    @Test
    public void testContains() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        assertTrue(list.contains(3.3));
        assertFalse(list.contains(6.6));
    }

    @Test
    public void testContainsAny() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList other = DoubleList.of(6.6, 7.7, 3.3);

        assertTrue(list.containsAny(other));
    }

    @Test
    public void testContainsAnyArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        double[] other = { 6.6, 7.7, 3.3 };

        assertTrue(list.containsAny(other));
    }

    @Test
    public void testContainsAll() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList other = DoubleList.of(2.2, 4.4);

        assertTrue(list.containsAll(other));

        other = DoubleList.of(2.2, 6.6);
        assertFalse(list.containsAll(other));
    }

    @Test
    public void testDisjoint() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList other = DoubleList.of(4.4, 5.5, 6.6);

        assertTrue(list.disjoint(other));

        other = DoubleList.of(3.3, 4.4, 5.5);
        assertFalse(list.disjoint(other));
    }

    // Set Operations Tests
    @Test
    public void testIntersection() {
        list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3, 4.4 });
        DoubleList other = DoubleList.of(2.2, 3.3, 5.5, 2.2);

        DoubleList result = list.intersection(other);
        assertEquals(3, result.size());
        assertEquals(2.2, result.get(0), DELTA);
        assertEquals(2.2, result.get(1), DELTA);
        assertEquals(3.3, result.get(2), DELTA);
    }

    @Test
    public void testDifference() {
        list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3, 4.4 });
        DoubleList other = DoubleList.of(2.2, 3.3);

        DoubleList result = list.difference(other);
        assertEquals(3, result.size());
        assertEquals(1.1, result.get(0), DELTA);
        assertEquals(2.2, result.get(1), DELTA);
        assertEquals(4.4, result.get(2), DELTA);
    }

    @Test
    public void testSymmetricDifference() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList other = DoubleList.of(3.3, 4.4, 5.5);

        DoubleList result = list.symmetricDifference(other);
        assertEquals(4, result.size());
        assertTrue(result.contains(1.1));
        assertTrue(result.contains(2.2));
        assertTrue(result.contains(4.4));
        assertTrue(result.contains(5.5));
    }

    // Index and Occurrence Tests
    @Test
    public void testOccurrencesOf() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 2.2, 4.4 });

        assertEquals(3, list.occurrencesOf(2.2));
        assertEquals(1, list.occurrencesOf(1.1));
        assertEquals(0, list.occurrencesOf(5.5));
    }

    @Test
    public void testIndexOf() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 5.5 });

        assertEquals(1, list.indexOf(2.2));
        assertEquals(2, list.indexOf(3.3));
        assertEquals(-1, list.indexOf(6.6));
    }

    @Test
    public void testIndexOfWithFromIndex() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 5.5 });

        assertEquals(3, list.indexOf(2.2, 2));
        assertEquals(-1, list.indexOf(2.2, 4));
    }

    @Test
    public void testLastIndexOf() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 5.5 });

        assertEquals(3, list.lastIndexOf(2.2));
        assertEquals(0, list.lastIndexOf(1.1));
        assertEquals(-1, list.lastIndexOf(6.6));
    }

    @Test
    public void testLastIndexOfWithStart() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 5.5 });

        assertEquals(1, list.lastIndexOf(2.2, 2));
        assertEquals(-1, list.lastIndexOf(5.5, 3));
    }

    // Min/Max/Median Tests
    @Test
    public void testMin() {
        list.addAll(new double[] { 3.3, 1.1, 4.4, 1.1, 5.5 });

        OptionalDouble min = list.min();
        assertTrue(min.isPresent());
        assertEquals(1.1, min.getAsDouble(), DELTA);
    }

    @Test
    public void testMinEmpty() {
        OptionalDouble min = list.min();
        assertFalse(min.isPresent());
    }

    @Test
    public void testMinRange() {
        list.addAll(new double[] { 3.3, 1.1, 4.4, 1.1, 5.5 });

        OptionalDouble min = list.min(2, 4);
        assertTrue(min.isPresent());
        assertEquals(1.1, min.getAsDouble(), DELTA);
    }

    @Test
    public void testMax() {
        list.addAll(new double[] { 3.3, 1.1, 4.4, 1.1, 5.5 });

        OptionalDouble max = list.max();
        assertTrue(max.isPresent());
        assertEquals(5.5, max.getAsDouble(), DELTA);
    }

    @Test
    public void testMedian() {
        list.addAll(new double[] { 3.3, 1.1, 4.4, 1.1, 5.5 });

        OptionalDouble median = list.median();
        assertTrue(median.isPresent());
        assertEquals(3.3, median.getAsDouble(), DELTA);
    }

    // forEach Tests
    @Test
    public void testForEach() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        List<Double> result = new ArrayList<>();

        list.forEach(result::add);

        assertEquals(3, result.size());
        assertEquals(1.1, result.get(0), DELTA);
        assertEquals(2.2, result.get(1), DELTA);
        assertEquals(3.3, result.get(2), DELTA);
    }

    @Test
    public void testForEachRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        List<Double> result = new ArrayList<>();

        list.forEach(1, 4, result::add);

        assertEquals(3, result.size());
        assertEquals(2.2, result.get(0), DELTA);
        assertEquals(3.3, result.get(1), DELTA);
        assertEquals(4.4, result.get(2), DELTA);
    }

    // First/Last Tests
    @Test
    public void testFirst() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });

        OptionalDouble first = list.first();
        assertTrue(first.isPresent());
        assertEquals(1.1, first.getAsDouble(), DELTA);
    }

    @Test
    public void testFirstEmpty() {
        OptionalDouble first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });

        OptionalDouble last = list.last();
        assertTrue(last.isPresent());
        assertEquals(3.3, last.getAsDouble(), DELTA);
    }

    @Test
    public void testGetFirst() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertEquals(1.1, list.getFirst(), DELTA);
    }

    @Test
    public void testGetFirstThrowsException() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLast() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertEquals(3.3, list.getLast(), DELTA);
    }

    @Test
    public void testAddFirst() {
        list.addAll(new double[] { 2.2, 3.3 });
        list.addFirst(1.1);

        assertEquals(3, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
    }

    @Test
    public void testAddLast() {
        list.addAll(new double[] { 1.1, 2.2 });
        list.addLast(3.3);

        assertEquals(3, list.size());
        assertEquals(3.3, list.get(2), DELTA);
    }

    @Test
    public void testRemoveFirst() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });

        double removed = list.removeFirst();
        assertEquals(1.1, removed, DELTA);
        assertEquals(2, list.size());
        assertEquals(2.2, list.get(0), DELTA);
    }

    @Test
    public void testRemoveFirstThrowsException() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLast() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });

        double removed = list.removeLast();
        assertEquals(3.3, removed, DELTA);
        assertEquals(2, list.size());
        assertEquals(2.2, list.get(1), DELTA);
    }

    // Distinct and Duplicates Tests
    @Test
    public void testDistinct() {
        list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3, 3.3, 3.3 });

        DoubleList distinct = list.distinct(0, list.size());
        assertEquals(3, distinct.size());
        assertEquals(1.1, distinct.get(0), DELTA);
        assertEquals(2.2, distinct.get(1), DELTA);
        assertEquals(3.3, distinct.get(2), DELTA);
    }

    @Test
    public void testHasDuplicates() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertFalse(list.hasDuplicates());

        list.add(2.2);
        assertTrue(list.hasDuplicates());
    }

    // Sort Tests
    @Test
    public void testIsSorted() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        assertTrue(list.isSorted());

        list.set(2, 1.0);
        assertFalse(list.isSorted());
    }

    @Test
    public void testSort() {
        list.addAll(new double[] { 3.3, 1.1, 4.4, 1.1, 5.5 });

        list.sort();
        assertTrue(list.isSorted());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(1.1, list.get(1), DELTA);
        assertEquals(5.5, list.get(4), DELTA);
    }

    @Test
    public void testParallelSort() {
        list.addAll(new double[] { 3.3, 1.1, 4.4, 1.1, 5.5 });

        list.parallelSort();
        assertTrue(list.isSorted());
    }

    @Test
    public void testReverseSort() {
        list.addAll(new double[] { 3.3, 1.1, 4.4, 1.1, 5.5 });

        list.reverseSort();
        assertEquals(5.5, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
        assertEquals(1.1, list.get(4), DELTA);
    }

    @Test
    public void testBinarySearch() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        assertEquals(2, list.binarySearch(3.3));
        assertTrue(list.binarySearch(6.6) < 0);
    }

    @Test
    public void testBinarySearchRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        assertEquals(3, list.binarySearch(1, 5, 4.4));
        assertTrue(list.binarySearch(1, 3, 4.4) < 0);
    }

    // Reverse and Rotate Tests
    @Test
    public void testReverse() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        list.reverse();
        assertEquals(5.5, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
        assertEquals(1.1, list.get(4), DELTA);
    }

    @Test
    public void testReverseRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        list.reverse(1, 4);
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
        assertEquals(2.2, list.get(3), DELTA);
        assertEquals(5.5, list.get(4), DELTA);
    }

    @Test
    public void testRotate() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        list.rotate(2);
        assertEquals(4.4, list.get(0), DELTA);
        assertEquals(5.5, list.get(1), DELTA);
        assertEquals(1.1, list.get(2), DELTA);
    }

    @Test
    public void testShuffle() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList original = list.copy();

        list.shuffle();
        assertEquals(original.size(), list.size());

        // Check all elements are still there
        for (int i = 0; i < original.size(); i++) {
            assertTrue(list.contains(original.get(i)));
        }
    }

    @Test
    public void testShuffleWithRandom() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        Random rnd = new Random(42);

        list.shuffle(rnd);
        assertEquals(5, list.size());
    }

    @Test
    public void testSwap() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });

        list.swap(0, 2);
        assertEquals(3.3, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(1.1, list.get(2), DELTA);
    }

    // Copy Tests
    @Test
    public void testCopy() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });

        DoubleList copy = list.copy();
        assertEquals(list.size(), copy.size());

        // Modify original
        list.set(0, 10.0);
        // Copy should not be affected
        assertEquals(1.1, copy.get(0), DELTA);
    }

    @Test
    public void testCopyRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        DoubleList copy = list.copy(1, 4);
        assertEquals(3, copy.size());
        assertEquals(2.2, copy.get(0), DELTA);
        assertEquals(4.4, copy.get(2), DELTA);
    }

    @Test
    public void testCopyWithStep() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        DoubleList copy = list.copy(0, 5, 2);
        assertEquals(3, copy.size());
        assertEquals(1.1, copy.get(0), DELTA);
        assertEquals(3.3, copy.get(1), DELTA);
        assertEquals(5.5, copy.get(2), DELTA);
    }

    // Split Tests
    @Test
    public void testSplit() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5, 6.6 });

        List<DoubleList> chunks = list.split(0, 6, 2);
        assertEquals(3, chunks.size());

        assertEquals(2, chunks.get(0).size());
        assertEquals(1.1, chunks.get(0).get(0), DELTA);
        assertEquals(2.2, chunks.get(0).get(1), DELTA);

        assertEquals(2, chunks.get(2).size());
        assertEquals(5.5, chunks.get(2).get(0), DELTA);
        assertEquals(6.6, chunks.get(2).get(1), DELTA);
    }

    // Utility Tests
    @Test
    public void testTrimToSize() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.trimToSize();
        assertEquals(3, list.size());
    }

    @Test
    public void testClear() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });

        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(list.isEmpty());

        list.add(1.1);
        assertFalse(list.isEmpty());
    }

    @Test
    public void testSize() {
        assertEquals(0, list.size());

        list.add(1.1);
        assertEquals(1, list.size());

        list.add(2.2);
        assertEquals(2, list.size());
    }

    // Conversion Tests
    @Test
    public void testBoxed() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });

        List<Double> boxed = list.boxed();
        assertEquals(3, boxed.size());
        assertEquals(Double.valueOf(1.1), boxed.get(0));
        assertEquals(Double.valueOf(3.3), boxed.get(2));
    }

    @Test
    public void testBoxedRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        List<Double> boxed = list.boxed(1, 4);
        assertEquals(3, boxed.size());
        assertEquals(Double.valueOf(2.2), boxed.get(0));
        assertEquals(Double.valueOf(4.4), boxed.get(2));
    }

    @Test
    public void testToArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });

        double[] array = list.toArray();
        assertEquals(3, array.length);
        assertEquals(1.1, array[0], DELTA);
        assertEquals(3.3, array[2], DELTA);

        // Modify array should not affect list
        array[0] = 10.0;
        assertEquals(1.1, list.get(0), DELTA);
    }

    @Test
    public void testToCollection() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });

        Set<Double> set = list.toCollection(0, 3, size -> new HashSet<>());
        assertEquals(3, set.size());
        assertTrue(set.contains(1.1));
        assertTrue(set.contains(3.3));
    }

    @Test
    public void testToMultiset() {
        list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3 });

        Multiset<Double> multiset = list.toMultiset(0, 4, size -> new Multiset<>());
        assertEquals(4, multiset.size());
        assertEquals(2, multiset.count(2.2));
    }

    // Iterator and Stream Tests
    @Test
    public void testIterator() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });

        DoubleIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1.1, iter.nextDouble(), DELTA);
        assertEquals(2.2, iter.nextDouble(), DELTA);
        assertEquals(3.3, iter.nextDouble(), DELTA);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testStream() {
        list.addAll(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });

        DoubleStream stream = list.stream();
        assertNotNull(stream);

        double sum = stream.sum();
        assertEquals(15.0, sum, DELTA);
    }

    @Test
    public void testStreamRange() {
        list.addAll(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });

        DoubleStream stream = list.stream(1, 4);
        double sum = stream.sum();
        assertEquals(9.0, sum, DELTA); // 2 + 3 + 4
    }

    // Equals and HashCode Tests
    @Test
    public void testEquals() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });

        DoubleList other = DoubleList.of(1.1, 2.2, 3.3);
        assertTrue(list.equals(other));

        other.add(4.4);
        assertFalse(list.equals(other));

        assertFalse(list.equals(null));
        assertFalse(list.equals("not a list"));
        assertTrue(list.equals(list));
    }

    @Test
    public void testHashCode() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList other = DoubleList.of(1.1, 2.2, 3.3);

        assertEquals(list.hashCode(), other.hashCode());

        other.add(4.4);
        assertNotEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("[]", list.toString());

        list.add(1.1);
        list.add(2.2);
        list.add(3.3);
        String str = list.toString();
        assertTrue(str.contains("1.1"));
        assertTrue(str.contains("2.2"));
        assertTrue(str.contains("3.3"));
    }

    // Array method test
    @Test
    public void testArray() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });

        double[] array = list.array();
        assertEquals(1.1, array[0], DELTA);
        assertEquals(2.2, array[1], DELTA);
        assertEquals(3.3, array[2], DELTA);

        // Modifying the array should affect the list (since it's the internal array)
        array[0] = 10.0;
        assertEquals(10.0, list.get(0), DELTA);
    }

    // Edge case tests
    @Test
    public void testLargeList() {
        int size = 10000;
        for (int i = 0; i < size; i++) {
            list.add(i * 0.1);
        }

        assertEquals(size, list.size());
        assertEquals(0.0, list.get(0), DELTA);
        assertEquals((size - 1) * 0.1, list.get(size - 1), DELTA);
    }

    @Test
    public void testEmptyOperations() {
        // Test operations on empty list
        assertFalse(list.remove(1.1));
        assertFalse(list.removeAllOccurrences(1.1));
        assertFalse(list.removeIf(x -> true));
        assertFalse(list.removeDuplicates());
        assertFalse(list.hasDuplicates());
        assertTrue(list.isSorted());

        list.sort();
        list.reverse();
        list.shuffle();

        assertTrue(list.isEmpty());
    }

    // Double-specific edge cases
    @Test
    public void testNaNHandling() {
        list.add(1.1);
        list.add(Double.NaN);
        list.add(2.2);

        assertEquals(3, list.size());
        assertTrue(Double.isNaN(list.get(1)));

        // Test contains with NaN
        assertTrue(list.contains(Double.NaN));

        // Test indexOf with NaN
        assertEquals(1, list.indexOf(Double.NaN));
    }

    @Test
    public void testInfinityHandling() {
        list.add(Double.NEGATIVE_INFINITY);
        list.add(0.0);
        list.add(Double.POSITIVE_INFINITY);

        assertEquals(Double.NEGATIVE_INFINITY, list.get(0), DELTA);
        assertEquals(0.0, list.get(1), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, list.get(2), DELTA);

        OptionalDouble min = list.min();
        assertTrue(min.isPresent());
        assertEquals(Double.NEGATIVE_INFINITY, min.getAsDouble(), DELTA);

        OptionalDouble max = list.max();
        assertTrue(max.isPresent());
        assertEquals(Double.POSITIVE_INFINITY, max.getAsDouble(), DELTA);
    }

    @Test
    public void testZeroHandling() {
        list.add(-0.0);
        list.add(0.0);

        // Both -0.0 and 0.0 should be considered equal
        assertEquals(0.0, list.get(0), DELTA);
        assertEquals(0.0, list.get(1), DELTA);

        assertTrue(list.contains(0.0));
        assertTrue(list.contains(-0.0));
    }

    @Test
    public void testPrecisionEdgeCases() {
        double a = 0.1 + 0.2;
        double b = 0.3;

        list.add(a);
        list.add(b);

        // These might not be exactly equal due to floating point precision
        // But replaceAll uses Double.compare which handles this correctly
        int replaced = list.replaceAll(0.3, 1.0);
        assertTrue(replaced > 0);
    }

    @Test
    public void testVeryLargePreciseValues() {
        // Test with values that can be represented precisely in double
        double largeValue = 9007199254740992.0; // 2^53
        list.add(largeValue);
        list.add(largeValue + 1);

        assertEquals(largeValue, list.get(0), DELTA);
        assertEquals(largeValue + 1, list.get(1), DELTA);

        assertTrue(list.contains(largeValue));
        assertEquals(0, list.indexOf(largeValue));
    }
}
