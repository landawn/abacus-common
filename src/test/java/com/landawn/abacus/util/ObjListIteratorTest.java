package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Stream;

public class ObjListIteratorTest extends TestBase {

    @Test
    public void testEmpty() {
        ObjListIterator<String> iter = ObjListIterator.empty();
        assertFalse(iter.hasNext());
        assertFalse(iter.hasPrevious());
        assertEquals(0, iter.nextIndex());
        assertEquals(-1, iter.previousIndex());
        assertThrows(NoSuchElementException.class, iter::next);
        assertThrows(NoSuchElementException.class, iter::previous);
        assertThrows(UnsupportedOperationException.class, () -> iter.set("x"));
        assertThrows(UnsupportedOperationException.class, () -> iter.add("x"));
        assertThrows(UnsupportedOperationException.class, iter::remove);
        assertTrue(iter.toList().isEmpty());
        assertEquals(0, iter.toArray().length);
        assertEquals(0, iter.count());
        assertTrue(iter.toSet().isEmpty());
        assertTrue(iter.toImmutableList().isEmpty());
        assertTrue(iter.toImmutableSet().isEmpty());
    }

    @Test
    public void testJust() {
        ObjListIterator<String> iter = ObjListIterator.just("Hello");
        assertTrue(iter.hasNext());
        assertFalse(iter.hasPrevious());
        assertEquals("Hello", iter.next());
        assertTrue(iter.hasPrevious());
        assertFalse(iter.hasNext());
        assertEquals("Hello", iter.previous());

        ObjListIterator<String> nil = ObjListIterator.just(null);
        assertTrue(nil.hasNext());
        assertNull(nil.next());
        assertFalse(nil.hasNext());
    }

    @Test
    public void testOf() {
        ObjListIterator<String> iter = ObjListIterator.of("one", "two", "three");
        assertTrue(iter.hasNext());
        assertEquals("one", iter.next());
        assertEquals("two", iter.next());
        assertEquals("three", iter.next());
        assertFalse(iter.hasNext());
        assertFalse(ObjListIterator.of().hasNext());
        assertFalse(ObjListIterator.of((String[]) null).hasNext());
        assertEquals(42, ObjListIterator.of(42).next());
    }

    @Test
    public void testOf_Range() {
        Integer[] numbers = { 1, 2, 3, 4, 5 };
        assertEquals(List.of(2, 3, 4), ObjListIterator.of(numbers, 1, 4).toList());
        assertEquals(List.of(1, 2, 3), ObjListIterator.of(numbers, 0, 3).toList());
        assertFalse(ObjListIterator.of(numbers, 1, 1).hasNext());
        assertThrows(IndexOutOfBoundsException.class, () -> ObjListIterator.of(numbers, 1, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> ObjListIterator.of(numbers, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> ObjListIterator.of(numbers, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> ObjListIterator.of((Integer[]) null, 0, 1));
    }

    @Test
    public void testOfList() {
        assertEquals(List.of("a", "b", "c"), ObjListIterator.of(Arrays.asList("a", "b", "c")).toList());
        assertFalse(ObjListIterator.of((List<String>) null).hasNext());
        assertFalse(ObjListIterator.of(new ArrayList<String>()).hasNext());

        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b"));
        iter.next();
        assertThrows(UnsupportedOperationException.class, () -> iter.set("x"));
        assertThrows(UnsupportedOperationException.class, () -> iter.add("x"));
        assertThrows(UnsupportedOperationException.class, iter::remove);
    }

    @Test
    public void testOfListIterator() {
        List<String> list = Arrays.asList("x", "y", "z");
        assertEquals(List.of("x", "y", "z"), ObjListIterator.of(list.listIterator()).toList());
        assertFalse(ObjListIterator.of((ListIterator<String>) null).hasNext());

        ObjListIterator<String> original = ObjListIterator.of(Arrays.asList("a", "b"));
        ObjListIterator<String> wrapped = ObjListIterator.of(original);
        assertNotSame(original, wrapped);
        assertEquals(List.of("a", "b"), wrapped.toList());

        List<String> source = new ArrayList<>(Arrays.asList("a", "b"));
        ListIterator<String> delegate = source.listIterator();
        ObjListIterator<String> mutable = new ObjListIterator<>() {
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public String next() {
                return delegate.next();
            }

            @Override
            public boolean hasPrevious() {
                return delegate.hasPrevious();
            }

            @Override
            public String previous() {
                return delegate.previous();
            }

            @Override
            public int nextIndex() {
                return delegate.nextIndex();
            }

            @Override
            public int previousIndex() {
                return delegate.previousIndex();
            }

            @Override
            public void remove() {
                delegate.remove();
            }

            @Override
            public void set(final String value) {
                delegate.set(value);
            }

            @Override
            public void add(final String value) {
                delegate.add(value);
            }
        };

        ObjListIterator<String> blocked = ObjListIterator.of(mutable);
        assertEquals("a", blocked.next());
        assertThrows(UnsupportedOperationException.class, blocked::remove);
        assertThrows(UnsupportedOperationException.class, () -> blocked.set("changed"));
        assertThrows(UnsupportedOperationException.class, () -> blocked.add("added"));
        assertEquals(Arrays.asList("a", "b"), source);

        ObjListIterator<String> fromListIterator = ObjListIterator.of(list.listIterator());
        fromListIterator.next();
        assertThrows(UnsupportedOperationException.class, () -> fromListIterator.set("x"));
        assertThrows(UnsupportedOperationException.class, () -> fromListIterator.add("x"));
    }

    @Test
    public void testBidirectional() {
        ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c", "d"));
        assertEquals(0, iter.nextIndex());
        assertEquals(-1, iter.previousIndex());
        iter.next();
        iter.next();
        assertEquals(2, iter.nextIndex());
        assertEquals(1, iter.previousIndex());
        assertEquals("b", iter.previous());
        assertEquals("a", iter.previous());
        assertFalse(iter.hasPrevious());
    }

    @Test
    public void testSkip() {
        assertEquals(List.of(3, 4, 5), ObjListIterator.of(Arrays.asList(1, 2, 3, 4, 5)).skip(2).toList());
        assertFalse(ObjListIterator.of(Arrays.asList(1, 2, 3)).skip(5).hasNext());
        ObjListIterator<Integer> original = ObjListIterator.of(Arrays.asList(1, 2, 3));
        assertSame(original, original.skip(0));
        assertThrows(IllegalArgumentException.class, () -> ObjListIterator.of(Arrays.asList(1, 2, 3)).skip(-1));
        assertThrows(NoSuchElementException.class, () -> ObjListIterator.of(Arrays.asList(1, 2)).skip(5).next());

        ObjListIterator<Integer> skipped = ObjListIterator.of(Arrays.asList(1, 2, 3, 4, 5)).skip(2);
        assertEquals(2, skipped.nextIndex());
        assertEquals(1, skipped.previousIndex());
        assertTrue(skipped.hasNext());
        assertTrue(skipped.hasPrevious());
        assertEquals(2, skipped.previous());
        assertEquals(2, skipped.next());
        assertEquals(3, skipped.next());
        assertEquals(4, skipped.next());
        assertEquals(4, skipped.previous());
        assertEquals(3, skipped.previous());
        skipped.next();
        assertThrows(UnsupportedOperationException.class, () -> skipped.set(99));
        assertThrows(UnsupportedOperationException.class, () -> skipped.add(99));
    }

    @Test
    public void testLimit() {
        assertEquals(List.of("a", "b", "c"), ObjListIterator.of(Arrays.asList("a", "b", "c", "d", "e")).limit(3).toList());
        assertEquals(List.of("a", "b"), ObjListIterator.of(Arrays.asList("a", "b")).limit(5).toList());
        assertFalse(ObjListIterator.of(Arrays.asList("a", "b", "c")).limit(0).hasNext());
        assertThrows(IllegalArgumentException.class, () -> ObjListIterator.of(Arrays.asList("a", "b")).limit(-1));
        ObjListIterator<String> exhausted = ObjListIterator.of(Arrays.asList("a", "b", "c")).limit(1);
        exhausted.next();
        assertThrows(NoSuchElementException.class, exhausted::next);

        ObjListIterator<String> limited = ObjListIterator.of(Arrays.asList("a", "b", "c", "d")).limit(3);
        assertEquals(0, limited.nextIndex());
        assertEquals(-1, limited.previousIndex());
        assertEquals("a", limited.next());
        assertTrue(limited.hasPrevious());
        assertEquals(1, limited.nextIndex());
        assertEquals(0, limited.previousIndex());
        assertEquals("b", limited.next());
        assertEquals("c", limited.next());
        assertFalse(limited.hasNext());
        assertEquals("c", limited.previous());
        assertTrue(limited.hasNext());
        assertEquals("c", limited.next());
        assertFalse(limited.hasNext());
        assertThrows(UnsupportedOperationException.class, () -> limited.set("x"));
        assertThrows(UnsupportedOperationException.class, () -> limited.add("x"));

        assertEquals(List.of(2, 3), ObjListIterator.of(Arrays.asList(1, 2, 3, 4, 5)).skip(1).limit(2).toList());
    }

    @Test
    public void testFirstNonNull() {
        assertEquals("found", ObjListIterator.of(Arrays.asList(null, null, "found", "next")).firstNonNull().get());
        assertEquals("a", ObjListIterator.of(Arrays.asList("a", "b")).firstNonNull().get());
        assertFalse(ObjListIterator.of(Arrays.asList(null, null, null)).firstNonNull().isPresent());
        assertFalse(ObjListIterator.<String> empty().firstNonNull().isPresent());
    }

    @Test
    public void testToArrayAndToList() {
        assertArrayEquals(new Object[] { "a", "b", "c" }, ObjListIterator.of(Arrays.asList("a", "b", "c")).toArray());
        assertArrayEquals(new String[] { "a", "b", "c" }, ObjListIterator.of(Arrays.asList("a", "b", "c")).toArray(new String[0]));
        String[] larger = ObjListIterator.of(Arrays.asList("a", "b")).toArray(new String[5]);
        assertEquals(5, larger.length);
        assertEquals("a", larger[0]);
        assertEquals("b", larger[1]);

        List<Integer> list = ObjListIterator.of(Arrays.asList(1, 2, 3)).toList();
        assertEquals(List.of(1, 2, 3), list);
        List<String> mutable = ObjListIterator.of(Arrays.asList("a")).toList();
        mutable.add("b");
        assertEquals(2, mutable.size());

        Set<String> set = ObjListIterator.of(Arrays.asList("a", "b", "a", "c")).toSet();
        assertEquals(3, set.size());
        assertTrue(set.containsAll(List.of("a", "b", "c")));
        LinkedList<String> linked = ObjListIterator.of(Arrays.asList("a", "b", "c")).toCollection(LinkedList::new);
        assertEquals("a", linked.getFirst());
        assertEquals("c", linked.getLast());
        assertEquals(3, ObjListIterator.of(Arrays.asList(1, 2, 2, 3)).toCollection(HashSet::new).size());
        assertEquals(List.of("a", "b", "c"), ObjListIterator.of(Arrays.asList("a", "b", "c")).toImmutableList());
        assertEquals(2, ObjListIterator.of(Arrays.asList("a", "b", "a")).toImmutableSet().size());

        ObjListIterator<String> counted = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        assertEquals(3, counted.count());
        assertFalse(counted.hasNext());
        ObjListIterator<String> partial = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        partial.next();
        assertEquals(2, partial.count());
    }

    @Test
    public void testStream() {
        Stream<String> stream = ObjListIterator.of(Arrays.asList("a", "b", "c")).stream();
        assertNotNull(stream);
        assertEquals(3, stream.count());
        assertEquals(2, ObjListIterator.of(Arrays.asList(1, 2, 3, 4, 5)).stream().filter(x -> x > 3).count());
        assertEquals(0, ObjListIterator.empty().stream().count());
    }

    @Test
    public void testForeachRemaining() throws Exception {
        List<String> result = new ArrayList<>();
        ObjListIterator.of(Arrays.asList("a", "b", "c")).foreachRemaining(result::add);
        assertEquals(List.of("a", "b", "c"), result);

        ObjListIterator<String> partial = ObjListIterator.of(Arrays.asList("a", "b", "c"));
        partial.next();
        List<String> remaining = new ArrayList<>();
        partial.foreachRemaining(remaining::add);
        assertEquals(List.of("b", "c"), remaining);

        List<String> empty = new ArrayList<>();
        ObjListIterator.<String> empty().foreachRemaining(empty::add);
        assertTrue(empty.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> ObjListIterator.of(Arrays.asList("a")).foreachRemaining(null));
    }

    @Test
    public void testForeachIndexed() throws Exception {
        List<String> fromStart = new ArrayList<>();
        ObjListIterator.of(Arrays.asList("x", "y")).foreachIndexed((index, value) -> fromStart.add(index + "=" + value));
        assertEquals(List.of("0=x", "1=y"), fromStart);

        ObjListIterator<String> partial = ObjListIterator.of(Arrays.asList("a", "b", "c", "d", "e"));
        partial.next();
        partial.next();
        List<Integer> indices = new ArrayList<>();
        List<String> values = new ArrayList<>();
        partial.foreachIndexed((index, value) -> {
            indices.add(index);
            values.add(value);
        });
        assertEquals(List.of(2, 3, 4), indices);
        assertEquals(List.of("c", "d", "e"), values);

        List<String> empty = new ArrayList<>();
        ObjListIterator.<String> empty().foreachIndexed((index, value) -> empty.add(index + ": " + value));
        assertTrue(empty.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> ObjListIterator.of(Arrays.asList("a")).foreachIndexed(null));
    }

    @Test
    public void testOfListIteratorPropagatesTheBackingListIteratorsExhaustionException() {
        // of(ListIterator) delegates next()/previous() straight to the wrapped list iterator, so exhaustion is
        // NOT normalised to ERROR_MSG_FOR_NO_SUCH_EX at either end. Arrays.asList is AbstractList-backed, so its
        // NoSuchElementException carries the internal IndexOutOfBoundsException as the cause; the message is that
        // exception's toString (it embeds the length), so only the cause type is pinned.
        final ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b").listIterator());

        final NoSuchElementException beforeStart = assertThrows(NoSuchElementException.class, iter::previous);
        assertNotEquals(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX, beforeStart.getMessage());
        assertInstanceOf(IndexOutOfBoundsException.class, beforeStart.getCause());

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());

        final NoSuchElementException pastEnd = assertThrows(NoSuchElementException.class, iter::next);
        assertNotEquals(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX, pastEnd.getMessage());
        assertInstanceOf(IndexOutOfBoundsException.class, pastEnd.getCause());

        // an ArrayList list iterator reports the same exhaustion with no message and no cause at all
        final ObjListIterator<String> overArrayList = ObjListIterator.of(new ArrayList<>(Arrays.asList("a")).listIterator());
        assertEquals("a", overArrayList.next());
        final NoSuchElementException bare = assertThrows(NoSuchElementException.class, overArrayList::next);
        assertNull(bare.getMessage());
        assertNull(bare.getCause());

        // contrast: skip()/limit() build their OWN list iterators, so those still report the standard message -
        // see testSkipAndLimitReportPreviousExhaustionWithStandardMessage
    }

    @Test
    public void testOfListIteratorLetsAForeignExhaustionExceptionEscape() {
        // A source whose own exhaustion exception is not even a NoSuchElementException: the wrapper does not
        // translate it, so the caller sees the source's exception. Pinned because the opposite (normalising it
        // to NoSuchElementException with ERROR_MSG_FOR_NO_SUCH_EX) was tried and deliberately reverted.
        final ListIterator<String> hostile = new ListIterator<>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public String next() {
                throw new IllegalStateException("exhausted");
            }

            @Override
            public boolean hasPrevious() {
                return false;
            }

            @Override
            public String previous() {
                throw new IllegalStateException("exhausted");
            }

            @Override
            public int nextIndex() {
                return 0;
            }

            @Override
            public int previousIndex() {
                return -1;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(final String e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(final String e) {
                throw new UnsupportedOperationException();
            }
        };

        final ObjListIterator<String> iter = ObjListIterator.of(hostile);

        assertEquals("exhausted", assertThrows(IllegalStateException.class, iter::next).getMessage());
        assertEquals("exhausted", assertThrows(IllegalStateException.class, iter::previous).getMessage());
    }

    @Test
    public void testSkipAndLimitReportPreviousExhaustionWithStandardMessage() {
        final ObjListIterator<String> skipped = ObjListIterator.of(Arrays.asList("a", "b", "c")).skip(1);
        assertEquals("a", skipped.previous());
        assertEquals(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX, assertThrows(NoSuchElementException.class, skipped::previous).getMessage());

        final ObjListIterator<String> limited = ObjListIterator.of(Arrays.asList("a", "b", "c")).limit(2);
        assertEquals(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX, assertThrows(NoSuchElementException.class, limited::previous).getMessage());
        assertEquals("a", limited.next());
        assertEquals("a", limited.previous());
    }
}
