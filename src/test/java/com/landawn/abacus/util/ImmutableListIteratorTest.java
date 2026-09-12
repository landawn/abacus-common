package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ImmutableListIteratorTest extends TestBase {

    @Test
    public void testEmptySingleton() {
        ImmutableListIterator<String> empty1 = ImmutableListIterator.empty();
        ImmutableListIterator<Integer> empty2 = ImmutableListIterator.empty();
        Assertions.assertSame(empty1, empty2);
    }

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
    public void testEmptyRemoveThrows() {
        ImmutableListIterator<String> empty = ImmutableListIterator.empty();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> empty.remove());
    }

    @Test
    public void testEmptySetThrows() {
        ImmutableListIterator<String> empty = ImmutableListIterator.empty();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> empty.set("x"));
    }

    @Test
    public void testEmptyAddThrows() {
        ImmutableListIterator<String> empty = ImmutableListIterator.empty();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> empty.add("x"));
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
    public void testForEachRemaining() {
        ArrayList<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        ImmutableListIterator<String> iter = ImmutableListIterator.of(list.listIterator());

        iter.next(); // consume "a"

        ArrayList<String> remaining = new ArrayList<>();
        iter.forEachRemaining(remaining::add);

        Assertions.assertEquals(Arrays.asList("b", "c", "d"), remaining);
        Assertions.assertFalse(iter.hasNext());
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
    public void testOf_DoesNotTrustMutableSubclass() {
        ArrayList<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        ListIterator<String> delegate = list.listIterator();
        ImmutableListIterator<String> mutableSubclass = new ImmutableListIterator<>() {
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

        ImmutableListIterator<String> wrapped = ImmutableListIterator.of(mutableSubclass);
        Assertions.assertNotSame(mutableSubclass, wrapped);
        Assertions.assertEquals("a", wrapped.next());
        Assertions.assertThrows(UnsupportedOperationException.class, wrapped::remove);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> wrapped.set("x"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> wrapped.add("x"));
        Assertions.assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    public void testIteratorWithNullElements() {
        ArrayList<String> list = new ArrayList<>(Arrays.asList("a", null, "c"));
        ImmutableListIterator<String> iter = ImmutableListIterator.of(list.listIterator());

        Assertions.assertEquals("a", iter.next());
        Assertions.assertNull(iter.next());
        Assertions.assertEquals("c", iter.next());
    }

    @Test
    public void testSingleElement() {
        ArrayList<String> list = new ArrayList<>(Arrays.asList("only"));
        ImmutableListIterator<String> iter = ImmutableListIterator.of(list.listIterator());

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertFalse(iter.hasPrevious());
        Assertions.assertEquals(0, iter.nextIndex());
        Assertions.assertEquals(-1, iter.previousIndex());

        Assertions.assertEquals("only", iter.next());

        Assertions.assertFalse(iter.hasNext());
        Assertions.assertTrue(iter.hasPrevious());
        Assertions.assertEquals(1, iter.nextIndex());
        Assertions.assertEquals(0, iter.previousIndex());

        Assertions.assertEquals("only", iter.previous());

        Assertions.assertFalse(iter.hasPrevious());
        Assertions.assertTrue(iter.hasNext());
    }

    @Test
    public void testOfWithEmptyList() {
        ArrayList<String> list = new ArrayList<>();
        ImmutableListIterator<String> iter = ImmutableListIterator.of(list.listIterator());

        Assertions.assertFalse(iter.hasNext());
        Assertions.assertFalse(iter.hasPrevious());
        Assertions.assertEquals(0, iter.nextIndex());
        Assertions.assertEquals(-1, iter.previousIndex());
    }

    @Test
    public void testRemove_ThrowsUnsupported() {
        ArrayList<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        ImmutableListIterator<String> iter = ImmutableListIterator.of(list.listIterator());

        iter.next();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> iter.remove());
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
    public void testWrappedIteratorPropagatesTheBackingIteratorsOwnExhaustionException() {
        // of(ListIterator) forwards next()/previous() straight to the backing iterator, so exhaustion is NOT
        // normalised to this library's message the way ObjIterator.of / ObjListIterator.of do it. Three distinct
        // shapes therefore reach callers, and which one they see depends on the backing list. Pinned so the
        // asymmetry with the ObjIterator adapters is recorded as intended rather than re-filed as a defect.

        // (1) the shared empty() singleton has its own implementation, and DOES report the library message
        Assertions.assertEquals(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX,
                Assertions.assertThrows(NoSuchElementException.class, () -> ImmutableListIterator.empty().next()).getMessage());
        Assertions.assertEquals(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX,
                Assertions.assertThrows(NoSuchElementException.class, () -> ImmutableListIterator.empty().previous()).getMessage());

        // (2) an ArrayList list iterator reports exhaustion with no message and no cause, at both ends, and
        // traversal itself is unaffected
        final ImmutableListIterator<String> overArrayList = ImmutableListIterator.of(new ArrayList<>(Arrays.asList("a")).listIterator());
        Assertions.assertEquals("a", overArrayList.next());
        final NoSuchElementException pastArrayListEnd = Assertions.assertThrows(NoSuchElementException.class, overArrayList::next);
        Assertions.assertNull(pastArrayListEnd.getMessage());
        Assertions.assertNull(pastArrayListEnd.getCause());
        Assertions.assertEquals("a", overArrayList.previous());
        Assertions.assertNull(Assertions.assertThrows(NoSuchElementException.class, overArrayList::previous).getMessage());

        // (3) ImmutableList's own views are AbstractList-backed, so the NoSuchElementException carries the
        // internal IndexOutOfBoundsException as its cause - the message is that exception's toString, so only
        // the cause type is pinned here
        final NoSuchElementException pastEnd = Assertions.assertThrows(NoSuchElementException.class,
                () -> ImmutableList.of("a", "b").listIterator(2).next());
        Assertions.assertInstanceOf(IndexOutOfBoundsException.class, pastEnd.getCause());

        final NoSuchElementException beforeStart = Assertions.assertThrows(NoSuchElementException.class,
                () -> ImmutableList.of("a", "b").listIterator(0).previous());
        Assertions.assertInstanceOf(IndexOutOfBoundsException.class, beforeStart.getCause());

        // the reversed view wraps the forward one, so it reports the same way from the opposite end
        Assertions.assertInstanceOf(IndexOutOfBoundsException.class, Assertions
                .assertThrows(NoSuchElementException.class, () -> ImmutableList.of("a", "b").reversed().listIterator(0).previous()).getCause());
        Assertions.assertInstanceOf(IndexOutOfBoundsException.class,
                Assertions.assertThrows(NoSuchElementException.class, () -> ImmutableList.of("a", "b").reversed().listIterator(2).next()).getCause());

        // an empty ImmutableList wraps Collections' own empty list iterator: message-less and causeless
        final NoSuchElementException fromEmptyList = Assertions.assertThrows(NoSuchElementException.class,
                () -> ImmutableList.<String> empty().listIterator(0).next());
        Assertions.assertNull(fromEmptyList.getMessage());
        Assertions.assertNull(fromEmptyList.getCause());
    }

    @Test
    public void testOneImmutableListReportsExhaustionInTwoDifferentShapes() {
        // Neither traversal normalises, but they wrap different sources, so the SAME list still reports
        // exhaustion two ways: iterator() wraps the backing collection's iterator (message-less) and
        // listIterator() wraps its list iterator (IndexOutOfBoundsException as the cause). Neither is
        // ERROR_MSG_FOR_NO_SUCH_EX. Pinned so callers are never told to match on either form.
        final ImmutableList<String> list = ImmutableList.of("x");

        final ObjIterator<String> viaIterator = list.iterator();
        Assertions.assertEquals("x", viaIterator.next());
        final NoSuchElementException fromIterator = Assertions.assertThrows(NoSuchElementException.class, viaIterator::next);
        Assertions.assertNull(fromIterator.getMessage());
        Assertions.assertNull(fromIterator.getCause());

        final ImmutableListIterator<String> viaListIterator = list.listIterator();
        Assertions.assertEquals("x", viaListIterator.next());
        final NoSuchElementException fromListIterator = Assertions.assertThrows(NoSuchElementException.class, viaListIterator::next);
        Assertions.assertNotEquals(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX, fromListIterator.getMessage());
        Assertions.assertInstanceOf(IndexOutOfBoundsException.class, fromListIterator.getCause());
    }

    @Test
    public void testStaleBackingIteratorPropagatesConcurrentModificationException() {
        // of(ListIterator) does not guard with hasNext()/hasPrevious() either, so a wrap()-backed live view whose
        // backing list was structurally modified reports the backing iterator's fail-fast
        // ConcurrentModificationException rather than exhaustion - at both ends, and whether or not elements
        // remain to visit. Reachable through wrap(List), and nothing else records it.
        final ArrayList<String> emptied = new ArrayList<>(Arrays.asList("a"));
        final ImmutableListIterator<String> forward = ImmutableList.wrap(emptied).listIterator();
        emptied.remove(0);
        Assertions.assertThrows(ConcurrentModificationException.class, forward::next);

        final ArrayList<String> grown = new ArrayList<>(Arrays.asList("a"));
        final ImmutableListIterator<String> backward = ImmutableList.wrap(grown).listIterator(0);
        grown.add("b");
        Assertions.assertThrows(ConcurrentModificationException.class, backward::previous);

        final ArrayList<String> stillHasElements = new ArrayList<>(Arrays.asList("a", "b", "c"));
        final ImmutableListIterator<String> stale = ImmutableList.wrap(stillHasElements).listIterator();
        stillHasElements.remove(2);
        Assertions.assertThrows(ConcurrentModificationException.class, stale::next);
    }
}
