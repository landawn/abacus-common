package com.landawn.abacus.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ImmutableIterator100Test extends TestBase {

    private static class TestImmutableIterator<T> extends ImmutableIterator<T> {
        private final Iterator<T> delegate;

        TestImmutableIterator(Iterator<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public T next() {
            return delegate.next();
        }
    }

    @Test
    public void testRemove_ThrowsUnsupported() {
        List<String> list = Arrays.asList("a", "b", "c");
        ImmutableIterator<String> iter = new TestImmutableIterator<>(list.iterator());

        iter.next();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> iter.remove());
    }

    @Test
    public void testToSet() {
        List<String> list = Arrays.asList("a", "b", "c", "b");
        ImmutableIterator<String> iter = new TestImmutableIterator<>(list.iterator());

        Set<String> set = iter.toSet();
        Assertions.assertEquals(3, set.size());
        Assertions.assertTrue(set.contains("a"));
        Assertions.assertTrue(set.contains("b"));
        Assertions.assertTrue(set.contains("c"));

        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToCollection() {
        List<String> list = Arrays.asList("x", "y", "z");
        ImmutableIterator<String> iter = new TestImmutableIterator<>(list.iterator());

        LinkedList<String> result = iter.toCollection(LinkedList::new);
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("x", result.get(0));
        Assertions.assertEquals("y", result.get(1));
        Assertions.assertEquals("z", result.get(2));
    }

    @Test
    public void testToCollection_WithCustomSupplier() {
        List<Integer> list = Arrays.asList(3, 1, 4, 1, 5);
        ImmutableIterator<Integer> iter = new TestImmutableIterator<>(list.iterator());

        TreeSet<Integer> sorted = iter.toCollection(TreeSet::new);
        Assertions.assertEquals(4, sorted.size());
        Iterator<Integer> sortedIter = sorted.iterator();
        Assertions.assertEquals(1, sortedIter.next());
        Assertions.assertEquals(3, sortedIter.next());
        Assertions.assertEquals(4, sortedIter.next());
        Assertions.assertEquals(5, sortedIter.next());
    }

    @Test
    public void testToImmutableList() {
        List<String> list = Arrays.asList("one", "two", "three");
        ImmutableIterator<String> iter = new TestImmutableIterator<>(list.iterator());

        ImmutableList<String> immutableList = iter.toImmutableList();
        Assertions.assertEquals(3, immutableList.size());
        Assertions.assertEquals("one", immutableList.get(0));
        Assertions.assertEquals("two", immutableList.get(1));
        Assertions.assertEquals("three", immutableList.get(2));

        Assertions.assertThrows(UnsupportedOperationException.class, () -> immutableList.add("four"));
    }

    @Test
    public void testToImmutableSet() {
        List<Integer> list = Arrays.asList(1, 2, 3, 2, 1);
        ImmutableIterator<Integer> iter = new TestImmutableIterator<>(list.iterator());

        ImmutableSet<Integer> immutableSet = iter.toImmutableSet();
        Assertions.assertEquals(3, immutableSet.size());
        Assertions.assertTrue(immutableSet.contains(1));
        Assertions.assertTrue(immutableSet.contains(2));
        Assertions.assertTrue(immutableSet.contains(3));

        Assertions.assertThrows(UnsupportedOperationException.class, () -> immutableSet.add(4));
    }

    @Test
    public void testCount() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        ImmutableIterator<String> iter = new TestImmutableIterator<>(list.iterator());

        long count = iter.count();
        Assertions.assertEquals(5L, count);

        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testCount_Empty() {
        List<String> list = Collections.emptyList();
        ImmutableIterator<String> iter = new TestImmutableIterator<>(list.iterator());

        long count = iter.count();
        Assertions.assertEquals(0L, count);
    }

    @Test
    public void testCount_PartiallyConsumed() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        ImmutableIterator<String> iter = new TestImmutableIterator<>(list.iterator());

        iter.next();
        iter.next();

        long count = iter.count();
        Assertions.assertEquals(3L, count);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testIteratorBehavior() {
        List<String> list = Arrays.asList("first", "second", "third");
        ImmutableIterator<String> iter = new TestImmutableIterator<>(list.iterator());

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("first", iter.next());

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("second", iter.next());

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("third", iter.next());

        Assertions.assertFalse(iter.hasNext());

        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testWithNullElements() {
        List<String> list = Arrays.asList("a", null, "c");
        ImmutableIterator<String> iter = new TestImmutableIterator<>(list.iterator());

        ImmutableList<String> result = iter.toImmutableList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertNull(result.get(1));
        Assertions.assertEquals("c", result.get(2));
    }

    @Test
    public void testImplementsImmutable() {
        List<String> list = Arrays.asList("a");
        ImmutableIterator<String> iter = new TestImmutableIterator<>(list.iterator());

        Assertions.assertTrue(iter instanceof Immutable);
    }
}
