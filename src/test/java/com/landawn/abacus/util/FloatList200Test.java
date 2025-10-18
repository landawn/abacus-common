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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.FloatConsumer;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.FloatUnaryOperator;

@Tag("new-test")
public class FloatList200Test extends TestBase {

    private final float delta = 0.0001f;

    @Test
    public void testConstructors() {
        FloatList list1 = new FloatList();
        assertEquals(0, list1.size());
        assertTrue(list1.isEmpty());

        FloatList list2 = new FloatList(10);
        assertEquals(0, list2.size());
        assertTrue(list2.array().length >= 10 || list2.array().length == 0);

        float[] arr = { 1.1f, 2.2f, 3.3f };
        FloatList list3 = new FloatList(arr);
        assertEquals(3, list3.size());
        assertArrayEquals(arr, list3.toArray(), delta);
        assertSame(arr, list3.array());

        FloatList list4 = new FloatList(arr, 2);
        assertEquals(2, list4.size());
        assertArrayEquals(new float[] { 1.1f, 2.2f }, list4.toArray(), delta);
        assertThrows(IndexOutOfBoundsException.class, () -> new FloatList(arr, 4));
    }

    @Test
    public void testFactoryMethods() {
        FloatList list1 = FloatList.of(1.1f, 2.2f, 3.3f);
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f }, list1.toArray(), delta);

        FloatList emptyList = FloatList.of();
        assertTrue(emptyList.isEmpty());

        float[] arr = { 1.1f, 2.2f, 3.3f };
        FloatList list2 = FloatList.copyOf(arr);
        assertArrayEquals(arr, list2.toArray(), delta);
        arr[0] = 9.9f;
        assertEquals(1.1f, list2.get(0), delta);

        FloatList list3 = FloatList.copyOf(arr, 1, 3);
        assertArrayEquals(new float[] { 2.2f, 3.3f }, list3.toArray(), delta);

        FloatList list4 = FloatList.repeat(5.5f, 3);
        assertArrayEquals(new float[] { 5.5f, 5.5f, 5.5f }, list4.toArray(), delta);

        FloatList list5 = FloatList.random(10);
        assertEquals(10, list5.size());
    }

    @Test
    public void testGetAndSet() {
        FloatList list = FloatList.of(1.1f, 2.2f, 3.3f);
        assertEquals(2.2f, list.get(1), delta);
        float oldValue = list.set(1, 9.9f);
        assertEquals(2.2f, oldValue, delta);
        assertEquals(9.9f, list.get(1), delta);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
    }

    @Test
    public void testAddAndAddAtIndex() {
        FloatList list = new FloatList();
        list.add(1.1f);
        list.add(3.3f);
        list.add(1, 2.2f);
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f }, list.toArray(), delta);
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(4, 4.4f));
    }

    @Test
    public void testAddAll() {
        FloatList list = FloatList.of(1.1f, 2.2f);
        assertTrue(list.addAll(FloatList.of(3.3f, 4.4f)));
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f, 4.4f }, list.toArray(), delta);
        assertFalse(list.addAll(new FloatList()));

        FloatList list2 = FloatList.of(1.1f, 2.2f);
        assertTrue(list2.addAll(new float[] { 3.3f, 4.4f }));
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f, 4.4f }, list2.toArray(), delta);
    }

    @Test
    public void testRemoveAndRemoveAllOccurrences() {
        FloatList list = FloatList.of(1.1f, 2.2f, 3.3f, 2.2f, 4.4f);
        assertTrue(list.remove(2.2f));
        assertArrayEquals(new float[] { 1.1f, 3.3f, 2.2f, 4.4f }, list.toArray(), delta);
        assertFalse(list.remove(9.9f));

        assertTrue(list.removeAllOccurrences(2.2f));
        assertArrayEquals(new float[] { 1.1f, 3.3f, 4.4f }, list.toArray(), delta);
    }

    @Test
    public void testRemoveIf() {
        FloatList list = FloatList.of(1.1f, 2.2f, 3.3f, 4.4f, 5.5f);
        FloatPredicate predicate = f -> f > 3.0f;
        assertTrue(list.removeIf(predicate));
        assertArrayEquals(new float[] { 1.1f, 2.2f }, list.toArray(), delta);
        assertFalse(list.removeIf(predicate));
    }

    @Test
    public void testRemoveDuplicates() {
        FloatList list = FloatList.of(1.1f, 2.2f, 1.1f, 3.3f, 2.2f);
        assertTrue(list.removeDuplicates());
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f }, list.toArray(), delta);
        assertFalse(FloatList.of(1f, 2f, 3f).removeDuplicates());
    }

    @Test
    public void testRetainAll() {
        FloatList list = FloatList.of(1.1f, 2.2f, 3.3f, 2.2f, 4.4f);
        assertTrue(list.retainAll(FloatList.of(2.2f, 4.4f, 5.5f)));
        assertArrayEquals(new float[] { 2.2f, 2.2f, 4.4f }, list.toArray(), delta);
    }

    @Test
    public void testDeleteAndRange() {
        FloatList list = FloatList.of(1.1f, 2.2f, 3.3f, 4.4f, 5.5f);
        float deleted = list.delete(2);
        assertEquals(3.3f, deleted, delta);
        assertArrayEquals(new float[] { 1.1f, 2.2f, 4.4f, 5.5f }, list.toArray(), delta);

        list.deleteRange(1, 3);
        assertArrayEquals(new float[] { 1.1f, 5.5f }, list.toArray(), delta);
    }

    @Test
    public void testReplaceMethods() {
        FloatList list1 = FloatList.of(1.1f, 2.2f, 1.1f, 3.3f);
        assertEquals(2, list1.replaceAll(1.1f, 9.9f));
        assertArrayEquals(new float[] { 9.9f, 2.2f, 9.9f, 3.3f }, list1.toArray(), delta);

        FloatList list2 = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatUnaryOperator operator = f -> f * 2.0f;
        list2.replaceAll(operator);
        assertArrayEquals(new float[] { 2.0f, 4.0f, 6.0f }, list2.toArray(), delta);

        FloatList list3 = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
        assertTrue(list3.replaceIf(f -> f > 2.5f, 5.5f));
        assertArrayEquals(new float[] { 1.0f, 2.0f, 5.5f, 5.5f }, list3.toArray(), delta);
    }

    @Test
    public void testContainsMethods() {
        FloatList list = FloatList.of(1.1f, 2.2f, 3.3f);
        assertTrue(list.contains(2.2f));
        assertFalse(list.contains(9.9f));

        assertTrue(list.containsAll(FloatList.of(1.1f, 3.3f)));
        assertFalse(list.containsAll(FloatList.of(1.1f, 4.4f)));

        assertTrue(list.containsAny(new float[] { 4.4f, 5.5f, 2.2f }));
        assertFalse(list.containsAny(new float[] { 4.4f, 5.5f, 6.6f }));
    }

    @Test
    public void testSetLogicMethods() {
        FloatList listA = FloatList.of(1.1f, 2.2f, 2.2f, 3.3f);
        FloatList listB = FloatList.of(2.2f, 3.3f, 4.4f);

        assertFalse(listA.disjoint(listB));
        assertTrue(listA.disjoint(FloatList.of(5.5f, 6.6f)));

        FloatList intersection = listA.intersection(listB);
        intersection.sort();
        assertArrayEquals(new float[] { 2.2f, 3.3f }, intersection.toArray(), delta);

        FloatList difference = listA.difference(listB);
        assertArrayEquals(new float[] { 1.1f, 2.2f }, difference.toArray(), delta);

        FloatList symmDiff = listA.symmetricDifference(listB);
        symmDiff.sort();
        assertArrayEquals(new float[] { 1.1f, 2.2f, 4.4f }, symmDiff.toArray(), delta);
    }

    @Test
    public void testIndexOfAndLastIndexOf() {
        FloatList list = FloatList.of(1.1f, 2.2f, 1.1f, 3.3f, 2.2f);
        assertEquals(0, list.indexOf(1.1f));
        assertEquals(2, list.lastIndexOf(1.1f));
        assertEquals(4, list.lastIndexOf(2.2f));
        assertEquals(4, list.indexOf(2.2f, 2));
        assertEquals(-1, list.indexOf(9.9f));
    }

    @Test
    public void testAggregateMethods() {
        FloatList list = FloatList.of(3.3f, 1.1f, 4.4f, 1.1f, 5.5f, 9.9f, 2.2f);
        FloatList emptyList = new FloatList();

        assertEquals(1.1f, list.min().get(), delta);
        assertEquals(9.9f, list.max().get(), delta);
        assertEquals(3.3f, list.median().get(), delta);

        assertTrue(emptyList.min().isEmpty());
        assertTrue(emptyList.max().isEmpty());
        assertTrue(emptyList.median().isEmpty());
    }

    @Test
    public void testSortAndSearch() {
        FloatList list = FloatList.of(3.3f, 1.1f, 4.4f, 2.2f, 5.5f);
        list.sort();
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f }, list.toArray(), delta);
        assertTrue(list.isSorted());

        assertEquals(2, list.binarySearch(3.3f));
        assertTrue(list.binarySearch(3.0f) < 0);

        list.reverseSort();
        assertArrayEquals(new float[] { 5.5f, 4.4f, 3.3f, 2.2f, 1.1f }, list.toArray(), delta);
    }

    @Test
    public void testManipulationMethods() {
        FloatList list = FloatList.of(1.1f, 2.2f, 3.3f, 4.4f, 5.5f);

        list.reverse();
        assertArrayEquals(new float[] { 5.5f, 4.4f, 3.3f, 2.2f, 1.1f }, list.toArray(), delta);

        list.rotate(2);
        assertArrayEquals(new float[] { 2.2f, 1.1f, 5.5f, 4.4f, 3.3f }, list.toArray(), delta);

        FloatList sorted = FloatList.of(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f);
        FloatList shuffled = sorted.copy();
        shuffled.shuffle(new Random(123));
        assertFalse(Arrays.equals(sorted.toArray(), shuffled.toArray()));

        list.swap(0, 4);
        assertArrayEquals(new float[] { 3.3f, 1.1f, 5.5f, 4.4f, 2.2f }, list.toArray(), delta);
    }

    @Test
    public void testViewAndCopyMethods() {
        FloatList list = FloatList.of(1.1f, 2.2f, 3.3f, 4.4f, 5.5f);

        FloatList copy = list.copy();
        assertNotSame(list, copy);
        assertArrayEquals(list.toArray(), copy.toArray(), delta);

        float[] arr = list.toArray();
        assertNotSame(list.array(), arr);

        List<Float> boxedList = list.boxed();
        assertEquals(5, boxedList.size());
        assertEquals(1.1f, boxedList.get(0), delta);

        List<FloatList> chunks = list.split(2);
        assertEquals(3, chunks.size());
        assertArrayEquals(new float[] { 1.1f, 2.2f }, chunks.get(0).toArray(), delta);
        assertArrayEquals(new float[] { 5.5f }, chunks.get(2).toArray(), delta);
    }

    @Test
    public void testToOtherCollections() {
        FloatList list = FloatList.of(1.1f, 2.2f, 3.3f);

        DoubleList doubleList = list.toDoubleList();
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3 }, doubleList.toArray(), delta);

        ArrayList<Float> arrayList = list.toCollection(ArrayList::new);
        assertEquals(3, arrayList.size());
        assertEquals(1.1f, arrayList.get(0), delta);

        FloatList listWithDups = FloatList.of(1.1f, 2.2f, 1.1f);
        Multiset<Float> multiset = listWithDups.toMultiset();
        assertEquals(2, multiset.count(1.1f));
    }

    @Test
    public void testDequeMethods() {
        FloatList list = FloatList.of(2.2f, 3.3f);
        list.addFirst(1.1f);
        list.addLast(4.4f);
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f, 4.4f }, list.toArray(), delta);

        assertEquals(1.1f, list.getFirst(), delta);
        assertEquals(4.4f, list.getLast(), delta);

        assertEquals(1.1f, list.removeFirst(), delta);
        assertEquals(4.4f, list.removeLast(), delta);

        assertArrayEquals(new float[] { 2.2f, 3.3f }, list.toArray(), delta);

        FloatList emptyList = new FloatList();
        assertThrows(NoSuchElementException.class, emptyList::getFirst);
        assertThrows(NoSuchElementException.class, emptyList::getLast);
    }

    @Test
    public void testObjectMethods() {
        FloatList list1 = FloatList.of(1.1f, 2.2f);
        FloatList list2 = FloatList.of(1.1f, 2.2f);
        FloatList list3 = FloatList.of(2.2f, 1.1f);

        assertEquals(list1, list2);
        assertNotEquals(list1, list3);
        assertNotEquals(null, list1);
        assertNotEquals(list1, new Object());

        assertEquals(list1.hashCode(), list2.hashCode());
        assertNotEquals(list1.hashCode(), list3.hashCode());

        assertEquals("[1.1, 2.2]", list1.toString());
        assertEquals("[]", new FloatList().toString());
    }

    @Test
    public void testStream() {
        FloatList list = FloatList.of(1.1f, 2.2f, 3.3f, 4.4f);
        double sum = list.stream().sum();
        assertEquals(11.0f, sum, delta);

        long count = list.stream(1, 3).count();
        assertEquals(2, count);
    }

    @Test
    public void testForEach() {
        FloatList list = FloatList.of(1.1f, 2.2f, 3.3f);
        final float[] sum = { 0.0f };
        FloatConsumer consumer = f -> sum[0] += f;
        list.forEach(consumer);
        assertEquals(6.6f, sum[0], delta);
    }
}
