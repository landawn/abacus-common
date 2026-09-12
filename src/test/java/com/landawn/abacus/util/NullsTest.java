package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class NullsTest extends AbstractTest {

    @Test
    public void testFirstNonNullFixedArity() {
        assertEquals("a", Nulls.firstNonNull("a", "b"));
        assertEquals("b", Nulls.firstNonNull(null, "b"));
        assertEquals("a", Nulls.firstNonNull("a", null));
        assertNull(Nulls.firstNonNull(null, null));

        assertEquals("a", Nulls.firstNonNull("a", "b", "c"));
        assertEquals("b", Nulls.firstNonNull(null, "b", "c"));
        assertEquals("c", Nulls.firstNonNull(null, null, "c"));
        assertNull(Nulls.firstNonNull(null, null, null));
    }

    @Test
    public void testFirstNonNullArray() {
        assertEquals("found", Nulls.firstNonNull(null, null, "found", "second"));
        assertNull(Nulls.firstNonNull((String[]) null));
        assertNull(Nulls.firstNonNull(new String[0]));
        assertNull(Nulls.firstNonNull(new String[] { null, null }));
    }

    @Test
    public void testFirstNonNullIterableAndIterator() {
        final List<String> values = Arrays.asList(null, null, "found", "second");
        assertEquals("found", Nulls.firstNonNull(values));
        assertEquals("found", Nulls.firstNonNull(values.iterator()));

        assertNull(Nulls.firstNonNull(Collections.<String> emptyList()));
        assertNull(Nulls.firstNonNull(Collections.<String> emptyIterator()));
        assertNull(Nulls.firstNonNull((Iterable<String>) null));
        assertNull(Nulls.firstNonNull((Iterator<String>) null));
    }

    @Test
    public void testLastNonNullFixedArity() {
        assertEquals("b", Nulls.lastNonNull("a", "b"));
        assertEquals("a", Nulls.lastNonNull("a", null));
        assertEquals("b", Nulls.lastNonNull(null, "b"));
        assertNull(Nulls.lastNonNull(null, null));

        assertEquals("c", Nulls.lastNonNull("a", "b", "c"));
        assertEquals("b", Nulls.lastNonNull("a", "b", null));
        assertEquals("a", Nulls.lastNonNull("a", null, null));
        assertNull(Nulls.lastNonNull(null, null, null));
    }

    @Test
    public void testLastNonNullArray() {
        assertEquals("second", Nulls.lastNonNull("first", "second", null, null));
        assertNull(Nulls.lastNonNull((String[]) null));
        assertNull(Nulls.lastNonNull(new String[0]));
        assertNull(Nulls.lastNonNull(new String[] { null, null }));
    }

    @Test
    public void testLastNonNullIterableAndIterator() {
        final List<String> values = Arrays.asList("first", "second", null, null);
        assertEquals("second", Nulls.lastNonNull(values));
        assertEquals("second", Nulls.lastNonNull(values.iterator()));

        assertNull(Nulls.lastNonNull(Collections.<String> emptyList()));
        assertNull(Nulls.lastNonNull(Collections.<String> emptyIterator()));
        assertNull(Nulls.lastNonNull((Iterable<String>) null));
        assertNull(Nulls.lastNonNull((Iterator<String>) null));
    }

    @Test
    public void testSingleUseIterables() {
        final Iterable<String> first = java.util.stream.Stream.of(null, "a")::iterator;
        assertEquals("a", Nulls.firstNonNull(first));

        final Iterable<String> last = java.util.stream.Stream.of("b", null)::iterator;
        assertEquals("b", Nulls.lastNonNull(last));
    }

    @Test
    public void testReturnsSameObjectInstance() {
        final Object first = new Object();
        final Object second = new Object();

        assertSame(first, Nulls.firstNonNull(null, first, second));
        assertSame(second, Nulls.lastNonNull(first, null, second));
    }

    @Test
    public void testFirstElementNonCollectionIterable() {
        // An empty non-Collection Iterable must return null (documented), not throw NoSuchElementException.
        final Iterable<String> emptyIterable = () -> Collections.<String> emptyIterator();
        assertNull(Nulls.firstElement(emptyIterable));

        // A non-empty non-Collection Iterable returns its first element.
        final Iterable<String> nonEmptyIterable = () -> Arrays.asList("x", "y").iterator();
        assertEquals("x", Nulls.firstElement(nonEmptyIterable));

        // Collection / list fast-paths still behave.
        assertNull(Nulls.firstElement(Collections.<String> emptyList()));
        assertEquals("a", Nulls.firstElement(Arrays.asList("a", "b")));
        assertNull(Nulls.firstElement((Iterable<String>) null));
    }

    @Test
    public void testLastElementArray() {
        assertEquals("b", Nulls.lastElement(new String[] { "a", "b" }));
        assertNull(Nulls.lastElement(new String[] { "a", null }));
        assertNull(Nulls.lastElement(new String[0]));
        assertNull(Nulls.lastElement((String[]) null));
    }

    @Test
    public void testLastElementIterable() {
        assertEquals("b", Nulls.lastElement(Arrays.asList("a", "b")));
        assertNull(Nulls.lastElement(Arrays.asList("a", null)));

        final Iterable<String> nonRandomAccess = () -> Arrays.asList("x", "y").iterator();
        assertEquals("y", Nulls.lastElement(nonRandomAccess));
        assertNull(Nulls.lastElement(Collections.<String> emptyList()));
        assertNull(Nulls.lastElement((Iterable<String>) null));
    }

    @Test
    public void testLastElementConsumesIterator() {
        final Iterator<String> iter = Arrays.asList("a", "b", null).iterator();
        assertNull(Nulls.lastElement(iter));
        assertFalse(iter.hasNext());

        assertNull(Nulls.lastElement(Collections.<String> emptyIterator()));
        assertNull(Nulls.lastElement((Iterator<String>) null));
    }
    @Test
    public void testLastElementIterableUsesDescendingIterator() {
        assertEquals("c", Nulls.lastElement(new LinkedList<>(Arrays.asList("a", "b", "c"))));
        assertNull(Nulls.lastElement(new LinkedList<>(Arrays.asList("a", "b", null))));
        assertNull(Nulls.lastElement(new LinkedList<String>()));

        assertEquals("z", Nulls.lastElement(new ArrayDeque<>(Arrays.asList("x", "y", "z"))));
        assertNull(Nulls.lastElement(new ArrayDeque<String>()));

        assertEquals("c", Nulls.lastElement(new TreeSet<>(Arrays.asList("b", "a", "c"))));
        assertNull(Nulls.lastElement(new TreeSet<String>()));

        // Proof that the descending path is taken rather than a forward drain: this source refuses
        // forward traversal, so reaching lastElement(c.iterator()) would fail the test.
        assertEquals("c", Nulls.lastElement(new DescendingOnlyIterable<>("a", "b", "c")));
        assertNull(Nulls.lastElement(new DescendingOnlyIterable<>("a", "b", (String) null)));
        assertNull(Nulls.lastElement(new DescendingOnlyIterable<String>()));
    }

    /**
     * An {@code Iterable} that can only be read backwards: {@link #iterator()} always throws, while
     * {@code descendingIterator()} walks the elements from last to first.
     */
    public static final class DescendingOnlyIterable<T> implements Iterable<T> {

        private final List<T> data;

        @SafeVarargs
        public DescendingOnlyIterable(final T... elements) {
            data = Arrays.asList(elements);
        }

        @Override
        public Iterator<T> iterator() {
            throw new UnsupportedOperationException("forward traversal is not expected here");
        }

        public Iterator<T> descendingIterator() {
            final ListIterator<T> iter = data.listIterator(data.size());

            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return iter.hasPrevious();
                }

                @Override
                public T next() {
                    return iter.previous();
                }
            };
        }
    }
}
