package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ImmutableListIterator100Test extends TestBase {

    @Test
    public void testEmpty() {
        ImmutableListIterator<String> empty = ImmutableListIterator.empty();

        Assertions.assertFalse(empty.hasNext());
        Assertions.assertFalse(empty.hasPrevious());
        Assertions.assertEquals(0, empty.nextIndex());
        Assertions.assertEquals(-1, empty.previousIndex());

        Assertions.assertThrows(NoSuchElementException.class, () -> empty.next());
        Assertions.assertThrows(NoSuchElementException.class, () -> empty.previous());
    }

    @Test
    public void testOf_NullIterator() {
        ImmutableListIterator<String> iter = ImmutableListIterator.of((ListIterator<String>) null);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertFalse(iter.hasPrevious());
    }

    @Test
    public void testOf_AlreadyImmutable() {
        ImmutableListIterator<String> original = ImmutableListIterator.empty();
        ImmutableListIterator<String> wrapped = ImmutableListIterator.of(original);
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testOf_RegularListIterator() {
        ArrayList<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        ListIterator<String> mutableIter = list.listIterator();
        ImmutableListIterator<String> immutableIter = ImmutableListIterator.of(mutableIter);

        Assertions.assertTrue(immutableIter.hasNext());
        Assertions.assertEquals(0, immutableIter.nextIndex());
        Assertions.assertEquals("a", immutableIter.next());
        Assertions.assertEquals(1, immutableIter.nextIndex());
        Assertions.assertEquals("b", immutableIter.next());
        Assertions.assertEquals("c", immutableIter.next());
        Assertions.assertFalse(immutableIter.hasNext());

        Assertions.assertTrue(immutableIter.hasPrevious());
        Assertions.assertEquals(2, immutableIter.previousIndex());
        Assertions.assertEquals("c", immutableIter.previous());
        Assertions.assertEquals("b", immutableIter.previous());
        Assertions.assertEquals("a", immutableIter.previous());
        Assertions.assertFalse(immutableIter.hasPrevious());
    }

    @Test
    public void testOf_BidirectionalNavigation() {
        ArrayList<String> list = new ArrayList<>(Arrays.asList("1", "2", "3", "4"));
        ListIterator<String> mutableIter = list.listIterator();
        ImmutableListIterator<String> iter = ImmutableListIterator.of(mutableIter);

        Assertions.assertEquals("1", iter.next());
        Assertions.assertEquals("2", iter.next());

        Assertions.assertEquals("2", iter.previous());

        Assertions.assertEquals("2", iter.next());
        Assertions.assertEquals("3", iter.next());

        Assertions.assertEquals(3, iter.nextIndex());
        Assertions.assertEquals(2, iter.previousIndex());
    }

    @Test
    public void testOf_StartingAtPosition() {
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(10, 20, 30, 40, 50));
        ListIterator<Integer> mutableIter = list.listIterator(2);
        ImmutableListIterator<Integer> iter = ImmutableListIterator.of(mutableIter);

        Assertions.assertEquals(2, iter.nextIndex());
        Assertions.assertEquals(1, iter.previousIndex());
        Assertions.assertEquals(30, iter.next());
        Assertions.assertEquals(30, iter.previous());
        Assertions.assertEquals(20, iter.previous());
    }

    @Test
    public void testSet_ThrowsUnsupported() {
        ArrayList<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        ImmutableListIterator<String> iter = ImmutableListIterator.of(list.listIterator());

        iter.next();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> iter.set("new"));
    }

    @Test
    public void testAdd_ThrowsUnsupported() {
        ArrayList<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        ImmutableListIterator<String> iter = ImmutableListIterator.of(list.listIterator());

        Assertions.assertThrows(UnsupportedOperationException.class, () -> iter.add("new"));
    }

    @Test
    public void testRemove_ThrowsUnsupported() {
        ArrayList<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        ImmutableListIterator<String> iter = ImmutableListIterator.of(list.listIterator());

        iter.next();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> iter.remove());
    }

    @Test
    public void testIteratorWithNullElements() {
        ArrayList<String> list = new ArrayList<>(Arrays.asList("a", null, "c"));
        ImmutableListIterator<String> iter = ImmutableListIterator.of(list.listIterator());

        Assertions.assertEquals("a", iter.next());
        Assertions.assertNull(iter.next());
        Assertions.assertEquals("c", iter.next());
    }
}
