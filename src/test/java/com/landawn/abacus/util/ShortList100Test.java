package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalShort;

@Tag("new-test")
public class ShortList100Test extends TestBase {

    private ShortList list;

    @BeforeEach
    public void setUp() {
        list = new ShortList();
    }

    @Test
    public void testConstructors() {
        ShortList list1 = new ShortList();
        assertEquals(0, list1.size());

        ShortList list2 = new ShortList(10);
        assertEquals(0, list2.size());

        short[] arr = { 1, 2, 3 };
        ShortList list3 = new ShortList(arr);
        assertEquals(3, list3.size());
        assertEquals((short) 1, list3.get(0));
        assertEquals((short) 2, list3.get(1));
        assertEquals((short) 3, list3.get(2));

        short[] arr2 = { 1, 2, 3, 4, 5 };
        ShortList list4 = new ShortList(arr2, 3);
        assertEquals(3, list4.size());
        assertEquals((short) 1, list4.get(0));
        assertEquals((short) 2, list4.get(1));
        assertEquals((short) 3, list4.get(2));

        assertThrows(IndexOutOfBoundsException.class, () -> new ShortList(arr2, 10));

        assertThrows(NullPointerException.class, () -> new ShortList(null));
        assertThrows(NullPointerException.class, () -> new ShortList(null, 0));
    }

    @Test
    public void testStaticFactoryMethods() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
        assertEquals(3, list1.size());
        assertEquals((short) 1, list1.get(0));
        assertEquals((short) 2, list1.get(1));
        assertEquals((short) 3, list1.get(2));

        ShortList list2 = ShortList.of((short[]) null);
        assertEquals(0, list2.size());

        short[] arr = { 1, 2, 3, 4 };
        ShortList list3 = ShortList.of(arr, 2);
        assertEquals(2, list3.size());
        assertEquals((short) 1, list3.get(0));
        assertEquals((short) 2, list3.get(1));

        ShortList list4 = ShortList.copyOf(arr);
        assertEquals(4, list4.size());
        arr[0] = 10;
        assertEquals((short) 1, list4.get(0));

        ShortList list5 = ShortList.copyOf(arr, 1, 3);
        assertEquals(2, list5.size());
        assertEquals((short) 2, list5.get(0));
        assertEquals((short) 3, list5.get(1));

        ShortList list6 = ShortList.repeat((short) 5, 4);
        assertEquals(4, list6.size());
        for (int i = 0; i < 4; i++) {
            assertEquals((short) 5, list6.get(i));
        }

        ShortList list7 = ShortList.random(10);
        assertEquals(10, list7.size());
        for (int i = 0; i < 10; i++) {
            short value = list7.get(i);
            assertTrue(value >= Short.MIN_VALUE && value <= Short.MAX_VALUE);
        }
    }

    @Test
    public void testRangeFactoryMethods() {
        ShortList list1 = ShortList.range((short) 0, (short) 5);
        assertEquals(5, list1.size());
        for (int i = 0; i < 5; i++) {
            assertEquals((short) i, list1.get(i));
        }

        ShortList list2 = ShortList.range((short) 0, (short) 10, (short) 2);
        assertEquals(5, list2.size());
        assertEquals((short) 0, list2.get(0));
        assertEquals((short) 2, list2.get(1));
        assertEquals((short) 4, list2.get(2));
        assertEquals((short) 6, list2.get(3));
        assertEquals((short) 8, list2.get(4));

        ShortList list3 = ShortList.rangeClosed((short) 1, (short) 5);
        assertEquals(5, list3.size());
        for (int i = 0; i < 5; i++) {
            assertEquals((short) (i + 1), list3.get(i));
        }

        ShortList list4 = ShortList.rangeClosed((short) 1, (short) 9, (short) 2);
        assertEquals(5, list4.size());
        assertEquals((short) 1, list4.get(0));
        assertEquals((short) 3, list4.get(1));
        assertEquals((short) 5, list4.get(2));
        assertEquals((short) 7, list4.get(3));
        assertEquals((short) 9, list4.get(4));

        ShortList list5 = ShortList.range((short) -5, (short) 0);
        assertEquals(5, list5.size());
        for (int i = 0; i < 5; i++) {
            assertEquals((short) (-5 + i), list5.get(i));
        }

        ShortList list6 = ShortList.range((short) 1000, (short) 1005);
        assertEquals(5, list6.size());
        assertEquals((short) 1000, list6.get(0));
        assertEquals((short) 1004, list6.get(4));
    }

    @Test
    public void testGetAndSet() {
        list.add((short) 10);
        list.add((short) 20);
        list.add((short) 30);

        assertEquals((short) 10, list.get(0));
        assertEquals((short) 20, list.get(1));
        assertEquals((short) 30, list.get(2));

        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));

        short oldValue = list.set(1, (short) 25);
        assertEquals((short) 20, oldValue);
        assertEquals((short) 25, list.get(1));

        assertThrows(IndexOutOfBoundsException.class, () -> list.set(3, (short) 40));
    }

    @Test
    public void testAdd() {
        list.add((short) 1);
        assertEquals(1, list.size());
        assertEquals((short) 1, list.get(0));

        list.add((short) 2);
        assertEquals(2, list.size());
        assertEquals((short) 2, list.get(1));

        list.add(1, (short) 15);
        assertEquals(3, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 15, list.get(1));
        assertEquals((short) 2, list.get(2));

        list.add(0, (short) 0);
        assertEquals(4, list.size());
        assertEquals((short) 0, list.get(0));

        list.add(list.size(), (short) 100);
        assertEquals(5, list.size());
        assertEquals((short) 100, list.get(4));

        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, (short) 5));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(6, (short) 5));
    }

    @Test
    public void testAddAll() {
        ShortList other = ShortList.of((short) 1, (short) 2, (short) 3);
        assertTrue(list.addAll(other));
        assertEquals(3, list.size());

        assertFalse(list.addAll(new ShortList()));
        assertEquals(3, list.size());

        ShortList other2 = ShortList.of((short) 10, (short) 20);
        assertTrue(list.addAll(1, other2));
        assertEquals(5, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 10, list.get(1));
        assertEquals((short) 20, list.get(2));
        assertEquals((short) 2, list.get(3));
        assertEquals((short) 3, list.get(4));

        short[] arr = { 50, 60 };
        assertTrue(list.addAll(arr));
        assertEquals(7, list.size());

        short[] arr2 = { -1, -2 };
        assertTrue(list.addAll(0, arr2));
        assertEquals(9, list.size());
        assertEquals((short) -1, list.get(0));
        assertEquals((short) -2, list.get(1));
    }

    @Test
    public void testRemove() {
        list.add((short) 10);
        list.add((short) 20);
        list.add((short) 10);
        list.add((short) 30);

        assertTrue(list.remove((short) 20));
        assertEquals(3, list.size());
        assertEquals((short) 10, list.get(0));
        assertEquals((short) 10, list.get(1));
        assertEquals((short) 30, list.get(2));

        assertFalse(list.remove((short) 40));
        assertEquals(3, list.size());

        assertTrue(list.removeAllOccurrences((short) 10));
        assertEquals(1, list.size());
        assertEquals((short) 30, list.get(0));

        assertFalse(list.removeAllOccurrences((short) 10));
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveIf() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 4);

        assertTrue(list.removeIf(s -> s % 2 == 0));
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 3, list.get(1));

        assertFalse(list.removeIf(s -> s > 10));
        assertEquals(2, list.size());
    }

    @Test
    public void testRemoveDuplicates() {
        list.add((short) 1);
        assertFalse(list.removeDuplicates());
        assertEquals(1, list.size());

        list.clear();
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 1);
        list.add((short) 3);
        list.add((short) 2);
        assertTrue(list.removeDuplicates());
        assertEquals(3, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 3, list.get(2));

        list.clear();
        list.add((short) 1);
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 2);
        list.add((short) 3);
        assertTrue(list.removeDuplicates());
        assertEquals(3, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 3, list.get(2));
    }

    @Test
    public void testRemoveAllAndRetainAll() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 2);

        ShortList toRemove = ShortList.of((short) 2);
        assertTrue(list.removeAll(toRemove));
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 3, list.get(1));

        list.clear();
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        assertTrue(list.removeAll(new short[] { 1, 3 }));
        assertEquals(1, list.size());
        assertEquals((short) 2, list.get(0));

        list.clear();
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        ShortList toRetain = ShortList.of((short) 1, (short) 3);
        assertTrue(list.retainAll(toRetain));
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 3, list.get(1));

        list.clear();
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        assertTrue(list.retainAll(new short[] { 2 }));
        assertEquals(1, list.size());
        assertEquals((short) 2, list.get(0));
    }

    @Test
    public void testDelete() {
        list.add((short) 10);
        list.add((short) 20);
        list.add((short) 30);

        assertEquals((short) 20, list.delete(1));
        assertEquals(2, list.size());
        assertEquals((short) 10, list.get(0));
        assertEquals((short) 30, list.get(1));

        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(-1));
    }

    @Test
    public void testDeleteAllByIndices() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 4);
        list.add((short) 5);

        list.deleteAllByIndices(1, 3);
        assertEquals(3, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 3, list.get(1));
        assertEquals((short) 5, list.get(2));

        list.deleteAllByIndices();
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteRange() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 4);
        list.add((short) 5);

        list.deleteRange(1, 4);
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 5, list.get(1));

        list.deleteRange(1, 1);
        assertEquals(2, list.size());

        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteRange(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteRange(-1, 1));
    }

    @Test
    public void testMoveRange() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 4);
        list.add((short) 5);

        list.moveRange(1, 3, 0);
        assertEquals(5, list.size());
        assertEquals((short) 2, list.get(0));
        assertEquals((short) 3, list.get(1));
        assertEquals((short) 1, list.get(2));
        assertEquals((short) 4, list.get(3));
        assertEquals((short) 5, list.get(4));
    }

    @Test
    public void testReplaceRange() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 4);

        ShortList replacement = ShortList.of((short) 10, (short) 20, (short) 30);
        list.replaceRange(1, 3, replacement);
        assertEquals(5, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 10, list.get(1));
        assertEquals((short) 20, list.get(2));
        assertEquals((short) 30, list.get(3));
        assertEquals((short) 4, list.get(4));

        list.clear();
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.replaceRange(1, 2, new short[] { 100, 101 });
        assertEquals(4, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 100, list.get(1));
        assertEquals((short) 101, list.get(2));
        assertEquals((short) 3, list.get(3));

        list.replaceRange(1, 3, new short[0]);
        assertEquals(2, list.size());
    }

    @Test
    public void testReplaceAll() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 1);
        list.add((short) 3);

        assertEquals(2, list.replaceAll((short) 1, (short) 10));
        assertEquals(4, list.size());
        assertEquals((short) 10, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 10, list.get(2));
        assertEquals((short) 3, list.get(3));

        list.replaceAll(s -> (short) (s * 2));
        assertEquals((short) 20, list.get(0));
        assertEquals((short) 4, list.get(1));
        assertEquals((short) 20, list.get(2));
        assertEquals((short) 6, list.get(3));

        assertTrue(list.replaceIf(s -> s > 10, (short) 0));
        assertEquals((short) 0, list.get(0));
        assertEquals((short) 4, list.get(1));
        assertEquals((short) 0, list.get(2));
        assertEquals((short) 6, list.get(3));

        assertFalse(list.replaceIf(s -> s > 100, (short) 1));
    }

    @Test
    public void testFill() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);

        list.fill((short) 5);
        assertEquals(3, list.size());
        for (int i = 0; i < 3; i++) {
            assertEquals((short) 5, list.get(i));
        }

        list.fill(1, 3, (short) 10);
        assertEquals((short) 5, list.get(0));
        assertEquals((short) 10, list.get(1));
        assertEquals((short) 10, list.get(2));
    }

    @Test
    public void testContains() {
        list.add((short) 10);
        list.add((short) 20);

        assertTrue(list.contains((short) 10));
        assertTrue(list.contains((short) 20));
        assertFalse(list.contains((short) 30));

        list.clear();
        assertFalse(list.contains((short) 10));
    }

    @Test
    public void testContainsAnyAndAll() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);

        assertTrue(list.containsAny(ShortList.of((short) 1)));
        assertTrue(list.containsAny(ShortList.of((short) 4, (short) 3)));
        assertFalse(list.containsAny(ShortList.of((short) 4, (short) 5)));
        assertFalse(list.containsAny(new ShortList()));

        assertTrue(list.containsAny(new short[] { 1 }));
        assertTrue(list.containsAny(new short[] { 4, 2 }));
        assertFalse(list.containsAny(new short[] { 4, 5 }));

        assertTrue(list.containsAll(ShortList.of((short) 1, (short) 2)));
        assertTrue(list.containsAll(ShortList.of((short) 3)));
        assertFalse(list.containsAll(ShortList.of((short) 1, (short) 4)));
        assertTrue(list.containsAll(new ShortList()));

        assertTrue(list.containsAll(new short[] { 1, 2 }));
        assertFalse(list.containsAll(new short[] { 1, 4 }));
        assertTrue(list.containsAll(new short[0]));

        list.clear();
        assertFalse(list.containsAny(ShortList.of((short) 1)));
        assertFalse(list.containsAll(ShortList.of((short) 1)));
        assertTrue(list.containsAll(new ShortList()));
    }

    @Test
    public void testDisjoint() {
        list.add((short) 1);
        list.add((short) 2);

        assertFalse(list.disjoint(ShortList.of((short) 1)));
        assertFalse(list.disjoint(ShortList.of((short) 2, (short) 3)));
        assertTrue(list.disjoint(ShortList.of((short) 3, (short) 4)));
        assertTrue(list.disjoint(new ShortList()));

        assertFalse(list.disjoint(new short[] { 1 }));
        assertTrue(list.disjoint(new short[] { 3, 4 }));
        assertTrue(list.disjoint(new short[0]));

        list.clear();
        assertTrue(list.disjoint(ShortList.of((short) 1)));
        assertTrue(list.disjoint(new short[] { 2 }));
    }

    @Test
    public void testIntersection() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 2);

        ShortList other = ShortList.of((short) 2, (short) 3, (short) 4, (short) 2);
        ShortList result = list.intersection(other);
        assertEquals(3, result.size());
        assertEquals((short) 2, result.get(0));
        assertEquals((short) 3, result.get(1));
        assertEquals((short) 2, result.get(2));

        result = list.intersection(new short[] { 1, 1, 4 });
        assertEquals(1, result.size());
        assertEquals((short) 1, result.get(0));

        result = list.intersection(new ShortList());
        assertEquals(0, result.size());
    }

    @Test
    public void testDifference() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 2);

        ShortList other = ShortList.of((short) 2, (short) 2);
        ShortList result = list.difference(other);
        assertEquals(2, result.size());
        assertEquals((short) 1, result.get(0));
        assertEquals((short) 3, result.get(1));

        result = list.difference(new short[] { 1, 3 });
        assertEquals(2, result.size());
        assertEquals((short) 2, result.get(0));
        assertEquals((short) 2, result.get(1));

        result = list.difference(new ShortList());
        assertEquals(4, result.size());
    }

    @Test
    public void testSymmetricDifference() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);

        ShortList other = ShortList.of((short) 2, (short) 3, (short) 4);
        ShortList result = list.symmetricDifference(other);
        assertEquals(2, result.size());
        assertEquals((short) 1, result.get(0));
        assertEquals((short) 4, result.get(1));

        result = list.symmetricDifference(new short[] { 1, 2, 3, 4, 5 });
        assertEquals(2, result.size());
        assertEquals((short) 4, result.get(0));
        assertEquals((short) 5, result.get(1));

        result = list.symmetricDifference(new ShortList());
        assertEquals(3, result.size());
    }

    @Test
    public void testOccurrencesOf() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 1);
        list.add((short) 3);
        list.add((short) 1);

        assertEquals(3, list.occurrencesOf((short) 1));
        assertEquals(1, list.occurrencesOf((short) 2));
        assertEquals(1, list.occurrencesOf((short) 3));
        assertEquals(0, list.occurrencesOf((short) 4));

        list.clear();
        assertEquals(0, list.occurrencesOf((short) 1));
    }

    @Test
    public void testIndexOf() {
        list.add((short) 10);
        list.add((short) 20);
        list.add((short) 30);
        list.add((short) 20);

        assertEquals(0, list.indexOf((short) 10));
        assertEquals(1, list.indexOf((short) 20));
        assertEquals(2, list.indexOf((short) 30));
        assertEquals(-1, list.indexOf((short) 40));

        assertEquals(3, list.indexOf((short) 20, 2));
        assertEquals(-1, list.indexOf((short) 10, 1));
        assertEquals(-1, list.indexOf((short) 20, 4));

        list.clear();
        assertEquals(-1, list.indexOf((short) 10));
    }

    @Test
    public void testLastIndexOf() {
        list.add((short) 10);
        list.add((short) 20);
        list.add((short) 30);
        list.add((short) 20);

        assertEquals(0, list.lastIndexOf((short) 10));
        assertEquals(3, list.lastIndexOf((short) 20));
        assertEquals(2, list.lastIndexOf((short) 30));
        assertEquals(-1, list.lastIndexOf((short) 40));

        assertEquals(1, list.lastIndexOf((short) 20, 2));
        assertEquals(-1, list.lastIndexOf((short) 10, -1));
        assertEquals(3, list.lastIndexOf((short) 20, 10));

        list.clear();
        assertEquals(-1, list.lastIndexOf((short) 10));
    }

    @Test
    public void testMinMaxMedian() {
        assertFalse(list.min().isPresent());
        assertFalse(list.max().isPresent());
        assertFalse(list.median().isPresent());

        list.add((short) 5);
        assertEquals(OptionalShort.of((short) 5), list.min());
        assertEquals(OptionalShort.of((short) 5), list.max());
        assertEquals(OptionalShort.of((short) 5), list.median());

        list.clear();
        list.add((short) 3);
        list.add((short) 1);
        list.add((short) 4);
        list.add((short) 1);
        list.add((short) 5);

        assertEquals(OptionalShort.of((short) 1), list.min());
        assertEquals(OptionalShort.of((short) 5), list.max());
        assertEquals(OptionalShort.of((short) 3), list.median());

        assertEquals(OptionalShort.of((short) 1), list.min(1, 4));
        assertEquals(OptionalShort.of((short) 4), list.max(1, 4));
        assertEquals(OptionalShort.of((short) 1), list.median(1, 4));

        assertFalse(list.min(2, 2).isPresent());
        assertFalse(list.max(2, 2).isPresent());
        assertFalse(list.median(2, 2).isPresent());
    }

    @Test
    public void testForEach() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);

        List<Short> collected = new ArrayList<>();
        list.forEach(s -> collected.add(s));
        assertEquals(3, collected.size());
        assertEquals(Short.valueOf((short) 1), collected.get(0));
        assertEquals(Short.valueOf((short) 2), collected.get(1));
        assertEquals(Short.valueOf((short) 3), collected.get(2));

        collected.clear();
        list.forEach(1, 3, s -> collected.add(s));
        assertEquals(2, collected.size());
        assertEquals(Short.valueOf((short) 2), collected.get(0));
        assertEquals(Short.valueOf((short) 3), collected.get(1));

        collected.clear();
        list.forEach(2, 0, s -> collected.add(s));
        assertEquals(2, collected.size());
        assertEquals(Short.valueOf((short) 3), collected.get(0));
        assertEquals(Short.valueOf((short) 2), collected.get(1));
    }

    @Test
    public void testFirstAndLast() {
        OptionalShort first = list.first();
        OptionalShort last = list.last();
        assertFalse(first.isPresent());
        assertFalse(last.isPresent());

        list.add((short) 10);
        list.add((short) 20);
        list.add((short) 30);

        first = list.first();
        last = list.last();
        assertTrue(first.isPresent());
        assertTrue(last.isPresent());
        assertEquals((short) 10, first.get());
        assertEquals((short) 30, last.get());
    }

    @Test
    public void testDistinct() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 1);
        list.add((short) 3);
        list.add((short) 2);

        ShortList distinct = list.distinct(0, list.size());
        assertEquals(3, distinct.size());
        assertTrue(distinct.contains((short) 1));
        assertTrue(distinct.contains((short) 2));
        assertTrue(distinct.contains((short) 3));

        distinct = list.distinct(1, 4);
        assertEquals(3, distinct.size());
    }

    @Test
    public void testHasDuplicates() {
        assertFalse(list.hasDuplicates());

        list.add((short) 1);
        assertFalse(list.hasDuplicates());

        list.add((short) 2);
        assertFalse(list.hasDuplicates());

        list.add((short) 1);
        assertTrue(list.hasDuplicates());
    }

    @Test
    public void testIsSorted() {
        assertTrue(list.isSorted());

        list.add((short) 1);
        assertTrue(list.isSorted());

        list.add((short) 2);
        list.add((short) 3);
        assertTrue(list.isSorted());

        list.add((short) 1);
        assertFalse(list.isSorted());
    }

    @Test
    public void testSort() {
        list.add((short) 3);
        list.add((short) 1);
        list.add((short) 4);
        list.add((short) 1);
        list.add((short) 5);

        list.sort();
        assertEquals(5, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 1, list.get(1));
        assertEquals((short) 3, list.get(2));
        assertEquals((short) 4, list.get(3));
        assertEquals((short) 5, list.get(4));
        assertTrue(list.isSorted());

        list.clear();
        list.sort();
        assertEquals(0, list.size());

        list.add((short) 1);
        list.sort();
        assertEquals(1, list.size());
        assertEquals((short) 1, list.get(0));
    }

    @Test
    public void testParallelSort() {
        for (int i = 1000; i > 0; i--) {
            list.add((short) i);
        }

        list.parallelSort();
        assertTrue(list.isSorted());

        for (int i = 1; i < Math.min(10, list.size()); i++) {
            assertTrue(list.get(i - 1) <= list.get(i));
        }
    }

    @Test
    public void testReverseSort() {
        list.add((short) 3);
        list.add((short) 1);
        list.add((short) 4);
        list.add((short) 1);

        list.reverseSort();
        assertEquals(4, list.size());
        assertEquals((short) 4, list.get(0));
        assertEquals((short) 3, list.get(1));
        assertEquals((short) 1, list.get(2));
        assertEquals((short) 1, list.get(3));
    }

    @Test
    public void testBinarySearch() {
        list.add((short) 1);
        list.add((short) 3);
        list.add((short) 5);
        list.add((short) 7);
        list.add((short) 9);

        assertTrue(list.isSorted());

        assertEquals(0, list.binarySearch((short) 1));
        assertEquals(2, list.binarySearch((short) 5));
        assertEquals(4, list.binarySearch((short) 9));
        assertTrue(list.binarySearch((short) 2) < 0);
        assertTrue(list.binarySearch((short) 10) < 0);

        assertEquals(1, list.binarySearch(1, 4, (short) 3));
        assertEquals(3, list.binarySearch(2, 5, (short) 7));
        assertTrue(list.binarySearch(0, 3, (short) 7) < 0);
    }

    @Test
    public void testReverse() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 4);

        list.reverse();
        assertEquals(4, list.size());
        assertEquals((short) 4, list.get(0));
        assertEquals((short) 3, list.get(1));
        assertEquals((short) 2, list.get(2));
        assertEquals((short) 1, list.get(3));

        list.reverse(1, 3);
        assertEquals((short) 4, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 3, list.get(2));
        assertEquals((short) 1, list.get(3));
    }

    @Test
    public void testRotate() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 4);

        list.rotate(1);
        assertEquals((short) 4, list.get(0));
        assertEquals((short) 1, list.get(1));
        assertEquals((short) 2, list.get(2));
        assertEquals((short) 3, list.get(3));

        list.rotate(-2);
        assertEquals((short) 2, list.get(0));
        assertEquals((short) 3, list.get(1));
        assertEquals((short) 4, list.get(2));
        assertEquals((short) 1, list.get(3));
    }

    @Test
    public void testShuffle() {
        for (int i = 0; i < 20; i++) {
            list.add((short) i);
        }

        ShortList original = list.copy();
        list.shuffle();
        assertEquals(original.size(), list.size());

        for (int i = 0; i < original.size(); i++) {
            assertTrue(list.contains(original.get(i)));
        }

        list.shuffle(new Random(42));
        assertEquals(original.size(), list.size());
    }

    @Test
    public void testSwap() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);

        list.swap(0, 2);
        assertEquals((short) 3, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 1, list.get(2));

        list.swap(0, 1);
        assertEquals((short) 2, list.get(0));
        assertEquals((short) 3, list.get(1));

        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
    }

    @Test
    public void testCopy() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);

        ShortList copy = list.copy();
        assertEquals(list.size(), copy.size());
        for (int i = 0; i < list.size(); i++) {
            assertEquals(list.get(i), copy.get(i));
        }

        copy.set(0, (short) 10);
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 10, copy.get(0));

        ShortList partialCopy = list.copy(1, 3);
        assertEquals(2, partialCopy.size());
        assertEquals((short) 2, partialCopy.get(0));
        assertEquals((short) 3, partialCopy.get(1));

        list.clear();
        for (int i = 0; i < 10; i++) {
            list.add((short) i);
        }
        ShortList steppedCopy = list.copy(0, 10, 2);
        assertEquals(5, steppedCopy.size());
        assertEquals((short) 0, steppedCopy.get(0));
        assertEquals((short) 2, steppedCopy.get(1));
        assertEquals((short) 4, steppedCopy.get(2));
        assertEquals((short) 6, steppedCopy.get(3));
        assertEquals((short) 8, steppedCopy.get(4));
    }

    @Test
    public void testSplit() {
        for (int i = 0; i < 7; i++) {
            list.add((short) i);
        }

        List<ShortList> splits = list.split(0, list.size(), 3);
        assertEquals(3, splits.size());
        assertEquals(3, splits.get(0).size());
        assertEquals(3, splits.get(1).size());
        assertEquals(1, splits.get(2).size());

        assertEquals((short) 0, splits.get(0).get(0));
        assertEquals((short) 1, splits.get(0).get(1));
        assertEquals((short) 2, splits.get(0).get(2));
    }

    @Test
    public void testTrimToSize() {
        list.add((short) 1);
        list.add((short) 2);

        ShortList trimmed = list.trimToSize();
        assertSame(list, trimmed);
        assertEquals(2, list.size());
    }

    @Test
    public void testClear() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);

        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(list.isEmpty());

        list.add((short) 1);
        assertFalse(list.isEmpty());

        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSize() {
        assertEquals(0, list.size());

        list.add((short) 1);
        assertEquals(1, list.size());

        list.add((short) 2);
        assertEquals(2, list.size());

        list.remove((short) 1);
        assertEquals(1, list.size());
    }

    @Test
    public void testBoxed() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);

        List<Short> boxed = list.boxed();
        assertEquals(3, boxed.size());
        assertEquals(Short.valueOf((short) 1), boxed.get(0));
        assertEquals(Short.valueOf((short) 2), boxed.get(1));
        assertEquals(Short.valueOf((short) 3), boxed.get(2));

        List<Short> partialBoxed = list.boxed(1, 3);
        assertEquals(2, partialBoxed.size());
        assertEquals(Short.valueOf((short) 2), partialBoxed.get(0));
        assertEquals(Short.valueOf((short) 3), partialBoxed.get(1));
    }

    @Test
    public void testToArray() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);

        short[] array = list.toArray();
        assertEquals(3, array.length);
        assertEquals((short) 1, array[0]);
        assertEquals((short) 2, array[1]);
        assertEquals((short) 3, array[2]);

        array[0] = 10;
        assertEquals((short) 1, list.get(0));
    }

    @Test
    public void testToIntList() {
        list.add((short) 1);
        list.add((short) -1);
        list.add((short) 32767);
        list.add((short) -32768);

        IntList intList = list.toIntList();
        assertEquals(4, intList.size());
        assertEquals(1, intList.get(0));
        assertEquals(-1, intList.get(1));
        assertEquals(32767, intList.get(2));
        assertEquals(-32768, intList.get(3));
    }

    @Test
    public void testToCollection() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);

        List<Short> collection = list.toCollection(0, list.size(), ArrayList::new);
        assertEquals(3, collection.size());
        assertEquals(Short.valueOf((short) 1), collection.get(0));
        assertEquals(Short.valueOf((short) 2), collection.get(1));
        assertEquals(Short.valueOf((short) 3), collection.get(2));
    }

    @Test
    public void testToMultiset() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 1);
        list.add((short) 3);
        list.add((short) 1);

        Multiset<Short> multiset = list.toMultiset(0, list.size(), Multiset::new);
        assertEquals(3, multiset.count(Short.valueOf((short) 1)));
        assertEquals(1, multiset.count(Short.valueOf((short) 2)));
        assertEquals(1, multiset.count(Short.valueOf((short) 3)));
    }

    @Test
    public void testIterator() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);

        ShortIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals((short) 1, iter.nextShort());
        assertTrue(iter.hasNext());
        assertEquals((short) 2, iter.nextShort());
        assertTrue(iter.hasNext());
        assertEquals((short) 3, iter.nextShort());
        assertFalse(iter.hasNext());

        ShortList emptyList = new ShortList();
        ShortIterator emptyIter = emptyList.iterator();
        assertFalse(emptyIter.hasNext());
    }

    @Test
    public void testStream() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);

        short[] streamResult = list.stream().toArray();
        assertEquals(3, streamResult.length);
        assertEquals((short) 1, streamResult[0]);
        assertEquals((short) 2, streamResult[1]);
        assertEquals((short) 3, streamResult[2]);

        short[] partialStreamResult = list.stream(1, 3).toArray();
        assertEquals(2, partialStreamResult.length);
        assertEquals((short) 2, partialStreamResult[0]);
        assertEquals((short) 3, partialStreamResult[1]);
    }

    @Test
    public void testGetFirstAndGetLast() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
        assertThrows(NoSuchElementException.class, () -> list.getLast());

        list.add((short) 10);
        list.add((short) 20);
        list.add((short) 30);

        assertEquals((short) 10, list.getFirst());
        assertEquals((short) 30, list.getLast());
    }

    @Test
    public void testAddFirstAndAddLast() {
        list.add((short) 10);

        list.addFirst((short) 5);
        assertEquals(2, list.size());
        assertEquals((short) 5, list.get(0));
        assertEquals((short) 10, list.get(1));

        list.addLast((short) 15);
        assertEquals(3, list.size());
        assertEquals((short) 5, list.get(0));
        assertEquals((short) 10, list.get(1));
        assertEquals((short) 15, list.get(2));
    }

    @Test
    public void testRemoveFirstAndRemoveLast() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
        assertThrows(NoSuchElementException.class, () -> list.removeLast());

        list.add((short) 10);
        list.add((short) 20);
        list.add((short) 30);

        assertEquals((short) 10, list.removeFirst());
        assertEquals(2, list.size());
        assertEquals((short) 20, list.get(0));
        assertEquals((short) 30, list.get(1));

        assertEquals((short) 30, list.removeLast());
        assertEquals(1, list.size());
        assertEquals((short) 20, list.get(0));
    }

    @Test
    public void testHashCode() {
        list.add((short) 1);
        list.add((short) 2);

        ShortList other = new ShortList();
        other.add((short) 1);
        other.add((short) 2);

        assertEquals(list.hashCode(), other.hashCode());

        other.add((short) 3);
        assertNotEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testEquals() {
        list.add((short) 1);
        list.add((short) 2);

        assertEquals(list, list);

        ShortList other = new ShortList();
        other.add((short) 1);
        other.add((short) 2);
        assertEquals(list, other);

        other.add((short) 3);
        assertNotEquals(list, other);

        ShortList different = new ShortList();
        different.add((short) 2);
        different.add((short) 1);
        assertNotEquals(list, different);

        assertNotEquals(list, null);

        assertNotEquals(list, "not a list");
    }

    @Test
    public void testToString() {
        assertEquals("[]", list.toString());

        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        assertEquals("[1, 2, 3]", list.toString());
    }

    @Test
    public void testArray() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);

        short[] array = list.array();
        assertEquals((short) 1, array[0]);
        assertEquals((short) 2, array[1]);
        assertEquals((short) 3, array[2]);

        array[0] = 10;
        assertEquals((short) 10, list.get(0));
    }
}
