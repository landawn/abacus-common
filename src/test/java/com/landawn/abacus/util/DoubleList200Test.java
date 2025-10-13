package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.DoubleConsumer;
import com.landawn.abacus.util.function.DoublePredicate;
import com.landawn.abacus.util.function.DoubleUnaryOperator;

@Tag("new-test")
public class DoubleList200Test extends TestBase {

    private final double delta = 0.00001;

    @Test
    public void testConstructors() {
        DoubleList list1 = new DoubleList();
        assertEquals(0, list1.size());
        assertTrue(list1.isEmpty());

        DoubleList list2 = new DoubleList(10);
        assertEquals(0, list2.size());
        assertTrue(list2.array().length >= 10 || list2.array().length == 0);

        double[] arr = { 1.1, 2.2, 3.3 };
        DoubleList list3 = new DoubleList(arr);
        assertEquals(3, list3.size());
        assertArrayEquals(arr, list3.toArray(), delta);
        assertSame(arr, list3.array());

        DoubleList list4 = new DoubleList(arr, 2);
        assertEquals(2, list4.size());
        assertArrayEquals(new double[] { 1.1, 2.2 }, list4.toArray(), delta);
        assertThrows(IndexOutOfBoundsException.class, () -> new DoubleList(arr, 4));
    }

    @Test
    public void testFactoryMethods() {
        DoubleList list1 = DoubleList.of(1.1, 2.2, 3.3);
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3 }, list1.toArray(), delta);

        DoubleList emptyList = DoubleList.of();
        assertTrue(emptyList.isEmpty());

        double[] arr = { 1.1, 2.2, 3.3 };
        DoubleList list2 = DoubleList.copyOf(arr);
        assertArrayEquals(arr, list2.toArray(), delta);
        arr[0] = 9.9;
        assertEquals(1.1, list2.get(0), delta);

        DoubleList list3 = DoubleList.copyOf(arr, 1, 3);
        assertArrayEquals(new double[] { 2.2, 3.3 }, list3.toArray(), delta);

        DoubleList list4 = DoubleList.repeat(5.5, 3);
        assertArrayEquals(new double[] { 5.5, 5.5, 5.5 }, list4.toArray(), delta);

        DoubleList list5 = DoubleList.random(10);
        assertEquals(10, list5.size());
    }

    @Test
    public void testGetAndSet() {
        DoubleList list = DoubleList.of(1.1, 2.2, 3.3);
        assertEquals(2.2, list.get(1), delta);
        double oldValue = list.set(1, 9.9);
        assertEquals(2.2, oldValue, delta);
        assertEquals(9.9, list.get(1), delta);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(3, 10.0));
    }

    @Test
    public void testAddAndAddAtIndex() {
        DoubleList list = new DoubleList();
        list.add(1.1);
        list.add(3.3);
        list.add(1, 2.2);
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3 }, list.toArray(), delta);
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(4, 4.4));
    }

    @Test
    public void testAddAll() {
        DoubleList list = DoubleList.of(1.1, 2.2);
        assertTrue(list.addAll(DoubleList.of(3.3, 4.4)));
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3, 4.4 }, list.toArray(), delta);
        assertFalse(list.addAll(new DoubleList()));
    }

    @Test
    public void testRemoveAndRemoveAllOccurrences() {
        DoubleList list = DoubleList.of(1.1, 2.2, 3.3, 2.2, 4.4);
        assertTrue(list.remove(2.2));
        assertArrayEquals(new double[] { 1.1, 3.3, 2.2, 4.4 }, list.toArray(), delta);
        assertFalse(list.remove(9.9));

        assertTrue(list.removeAllOccurrences(2.2));
        assertArrayEquals(new double[] { 1.1, 3.3, 4.4 }, list.toArray(), delta);
    }

    @Test
    public void testRemoveWithNaN() {
        DoubleList list = DoubleList.of(1.1, Double.NaN, 3.3, Double.NaN);
        assertTrue(list.remove(Double.NaN));
        assertArrayEquals(new double[] { 1.1, 3.3, Double.NaN }, list.toArray(), delta);
        assertTrue(list.removeAllOccurrences(Double.NaN));
        assertArrayEquals(new double[] { 1.1, 3.3 }, list.toArray(), delta);
    }

    @Test
    public void testRemoveIf() {
        DoubleList list = DoubleList.of(1.1, 2.2, 3.3, 4.4, 5.5);
        DoublePredicate predicate = d -> d > 3.0;
        assertTrue(list.removeIf(predicate));
        assertArrayEquals(new double[] { 1.1, 2.2 }, list.toArray(), delta);
        assertFalse(list.removeIf(predicate));
    }

    @Test
    public void testRemoveDuplicates() {
        DoubleList list = DoubleList.of(1.1, 2.2, 1.1, 3.3, 2.2);
        assertTrue(list.removeDuplicates());
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3 }, list.toArray(), delta);
        assertFalse(DoubleList.of(1.0, 2.0, 3.0).removeDuplicates());
    }

    @Test
    public void testRetainAll() {
        DoubleList list = DoubleList.of(1.1, 2.2, 3.3, 2.2, 4.4);
        assertTrue(list.retainAll(DoubleList.of(2.2, 4.4, 5.5)));
        assertArrayEquals(new double[] { 2.2, 2.2, 4.4 }, list.toArray(), delta);
    }

    @Test
    public void testDeleteAndRange() {
        DoubleList list = DoubleList.of(1.1, 2.2, 3.3, 4.4, 5.5);
        double deleted = list.delete(2);
        assertEquals(3.3, deleted, delta);
        assertArrayEquals(new double[] { 1.1, 2.2, 4.4, 5.5 }, list.toArray(), delta);

        list.deleteRange(1, 3);
        assertArrayEquals(new double[] { 1.1, 5.5 }, list.toArray(), delta);
    }

    @Test
    public void testReplaceMethods() {
        DoubleList list1 = DoubleList.of(1.1, 2.2, 1.1, 3.3);
        assertEquals(2, list1.replaceAll(1.1, 9.9));
        assertArrayEquals(new double[] { 9.9, 2.2, 9.9, 3.3 }, list1.toArray(), delta);

        DoubleList list2 = DoubleList.of(1.0, 2.0, 3.0);
        DoubleUnaryOperator operator = d -> d * 2.0;
        list2.replaceAll(operator);
        assertArrayEquals(new double[] { 2.0, 4.0, 6.0 }, list2.toArray(), delta);

        DoubleList list3 = DoubleList.of(1.0, 2.0, 3.0, 4.0);
        assertTrue(list3.replaceIf(d -> d > 2.5, 5.5));
        assertArrayEquals(new double[] { 1.0, 2.0, 5.5, 5.5 }, list3.toArray(), delta);
    }

    @Test
    public void testContainsMethods() {
        DoubleList list = DoubleList.of(1.1, 2.2, 3.3, Double.NaN);
        assertTrue(list.contains(2.2));
        assertTrue(list.contains(Double.NaN));
        assertFalse(list.contains(9.9));

        assertTrue(list.containsAll(DoubleList.of(1.1, 3.3, Double.NaN)));
        assertFalse(list.containsAll(DoubleList.of(1.1, 4.4)));

        assertTrue(list.containsAny(new double[] { 4.4, 5.5, 2.2 }));
        assertFalse(list.containsAny(new double[] { 4.4, 5.5, 6.6 }));
    }

    @Test
    public void testSetLogicMethods() {
        DoubleList listA = DoubleList.of(1.1, 2.2, 2.2, 3.3);
        DoubleList listB = DoubleList.of(2.2, 3.3, 4.4);

        assertFalse(listA.disjoint(listB));
        assertTrue(listA.disjoint(DoubleList.of(5.5, 6.6)));

        DoubleList intersection = listA.intersection(listB);
        intersection.sort();
        assertArrayEquals(new double[] { 2.2, 3.3 }, intersection.toArray(), delta);

        DoubleList difference = listA.difference(listB);
        assertArrayEquals(new double[] { 1.1, 2.2 }, difference.toArray(), delta);

        DoubleList symmDiff = listA.symmetricDifference(listB);
        symmDiff.sort();
        assertArrayEquals(new double[] { 1.1, 2.2, 4.4 }, symmDiff.toArray(), delta);
    }

    @Test
    public void testIndexOfAndLastIndexOf() {
        DoubleList list = DoubleList.of(1.1, 2.2, 1.1, 3.3, 2.2);
        assertEquals(0, list.indexOf(1.1));
        assertEquals(2, list.lastIndexOf(1.1));
        assertEquals(4, list.lastIndexOf(2.2));
        assertEquals(4, list.indexOf(2.2, 2));
        assertEquals(-1, list.indexOf(9.9));
    }

    @Test
    public void testAggregateMethods() {
        DoubleList list = DoubleList.of(3.3, 1.1, 4.4, 1.1, 5.5, 9.9, 2.2);
        DoubleList emptyList = new DoubleList();

        assertEquals(1.1, list.min().getAsDouble(), delta);
        assertEquals(9.9, list.max().getAsDouble(), delta);
        assertEquals(3.3, list.median().getAsDouble(), delta);

        assertTrue(emptyList.min().isEmpty());
        assertTrue(emptyList.max().isEmpty());
        assertTrue(emptyList.median().isEmpty());
    }

    @Test
    public void testSortAndSearch() {
        DoubleList list = DoubleList.of(3.3, 1.1, 4.4, 2.2, 5.5);
        list.sort();
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 }, list.toArray(), delta);
        assertTrue(list.isSorted());

        assertEquals(2, list.binarySearch(3.3));
        assertTrue(list.binarySearch(3.0) < 0);

        list.reverseSort();
        assertArrayEquals(new double[] { 5.5, 4.4, 3.3, 2.2, 1.1 }, list.toArray(), delta);
    }

    @Test
    public void testManipulationMethods() {
        DoubleList list = DoubleList.of(1.1, 2.2, 3.3, 4.4, 5.5);

        list.reverse();
        assertArrayEquals(new double[] { 5.5, 4.4, 3.3, 2.2, 1.1 }, list.toArray(), delta);

        list.rotate(2);
        assertArrayEquals(new double[] { 2.2, 1.1, 5.5, 4.4, 3.3 }, list.toArray(), delta);

        DoubleList sorted = DoubleList.of(1, 2, 3, 4, 5, 6, 7, 8);
        DoubleList shuffled = sorted.copy();
        shuffled.shuffle(new Random(123));
        assertFalse(Arrays.equals(sorted.toArray(), shuffled.toArray()));

        list.swap(0, 4);
        assertArrayEquals(new double[] { 3.3, 1.1, 5.5, 4.4, 2.2 }, list.toArray(), delta);
    }

    @Test
    public void testViewAndCopyMethods() {
        DoubleList list = DoubleList.of(1.1, 2.2, 3.3, 4.4, 5.5);

        DoubleList copy = list.copy();
        assertNotSame(list, copy);
        assertArrayEquals(list.toArray(), copy.toArray(), delta);

        double[] arr = list.toArray();
        assertNotSame(list.array(), arr);

        List<Double> boxedList = list.boxed();
        assertEquals(5, boxedList.size());
        assertEquals(1.1, boxedList.get(0), delta);

        List<DoubleList> chunks = list.split(2);
        assertEquals(3, chunks.size());
        assertArrayEquals(new double[] { 1.1, 2.2 }, chunks.get(0).toArray(), delta);
        assertArrayEquals(new double[] { 5.5 }, chunks.get(2).toArray(), delta);
    }

    @Test
    public void testToOtherCollections() {
        DoubleList list = DoubleList.of(1.1, 2.2, 3.3);

        ArrayList<Double> arrayList = list.toCollection(ArrayList::new);
        assertEquals(3, arrayList.size());
        assertEquals(1.1, arrayList.get(0), delta);

        DoubleList listWithDups = DoubleList.of(1.1, 2.2, 1.1);
        Multiset<Double> multiset = listWithDups.toMultiset();
        assertEquals(2, multiset.count(1.1));
    }

    @Test
    public void testDequeMethods() {
        DoubleList list = DoubleList.of(2.2, 3.3);
        list.addFirst(1.1);
        list.addLast(4.4);
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3, 4.4 }, list.toArray(), delta);

        assertEquals(1.1, list.getFirst(), delta);
        assertEquals(4.4, list.getLast(), delta);

        assertEquals(1.1, list.removeFirst(), delta);
        assertEquals(4.4, list.removeLast(), delta);

        assertArrayEquals(new double[] { 2.2, 3.3 }, list.toArray(), delta);

        DoubleList emptyList = new DoubleList();
        assertThrows(NoSuchElementException.class, emptyList::getFirst);
        assertThrows(NoSuchElementException.class, emptyList::getLast);
    }

    @Test
    public void testObjectMethods() {
        DoubleList list1 = DoubleList.of(1.1, 2.2);
        DoubleList list2 = DoubleList.of(1.1, 2.2);
        DoubleList list3 = DoubleList.of(2.2, 1.1);

        assertEquals(list1, list2);
        assertNotEquals(list1, list3);
        assertNotEquals(null, list1);
        assertNotEquals(list1, new Object());

        assertEquals(list1.hashCode(), list2.hashCode());

        assertEquals("[1.1, 2.2]", list1.toString());
        assertEquals("[]", new DoubleList().toString());
    }

    @Test
    public void testStream() {
        DoubleList list = DoubleList.of(1.1, 2.2, 3.3, 4.4);
        double sum = list.stream().sum();
        assertEquals(11.0, sum, delta);

        long count = list.stream(1, 3).count();
        assertEquals(2, count);
    }

    @Test
    public void testForEach() {
        DoubleList list = DoubleList.of(1.1, 2.2, 3.3);
        final double[] sum = { 0.0 };
        DoubleConsumer consumer = d -> sum[0] += d;
        list.forEach(consumer);
        assertEquals(6.6, sum[0], delta);
    }
}
