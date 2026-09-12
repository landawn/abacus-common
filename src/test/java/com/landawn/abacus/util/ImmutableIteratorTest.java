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
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ImmutableIteratorTest extends TestBase {

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
    public void testToCollection_RejectsNullSupplierResultForEmptyIterator() {
        ImmutableIterator<String> iter = new TestImmutableIterator<>(Collections.emptyIterator());

        Assertions.assertThrows(IllegalArgumentException.class, () -> iter.toCollection(() -> null));
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
    public void testCount_Empty() {
        List<String> list = Collections.emptyList();
        ImmutableIterator<String> iter = new TestImmutableIterator<>(list.iterator());

        long count = iter.count();
        Assertions.assertEquals(0L, count);
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
    public void testImplementsImmutable() {
        List<String> list = Arrays.asList("a");
        ImmutableIterator<String> iter = new TestImmutableIterator<>(list.iterator());

        Assertions.assertFalse(iter instanceof Immutable, "An iterator advances mutable cursor state and must not advertise structural immutability");
    }

    @Test
    public void testToImmutableListAndSetOwnTheStorageTheyJustBuilt() {
        // the collected collection is created inside the call and nothing else can reach it, so the result
        // owns it; publishing it through wrap() forced ImmutableList/ImmutableSet.copyOf() to copy it again.
        // This entry is named in all four enumerations of the owning instances: ImmutableList's class
        // javadoc, ImmutableList.copyOf(Collection), ImmutableList.reversed() and ImmutableSet.copyOf.
        final ImmutableList<String> list = ObjIterator.of("a", "b").toImmutableList();
        Assertions.assertSame(list, ImmutableList.copyOf(list));
        Assertions.assertEquals(Arrays.asList("a", "b"), list);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.add("c"));

        final ImmutableSet<String> set = ObjIterator.of("a", "b", "a").toImmutableSet();
        Assertions.assertSame(set, ImmutableSet.copyOf(set));
        Assertions.assertEquals(new java.util.LinkedHashSet<>(Arrays.asList("a", "b")), set);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.add("c"));

        // the observable half of ImmutableList.reversed()'s enumeration: an owning single-element list is
        // its own reverse, so reversed() short-circuits to this same instance
        final ImmutableList<String> single = ObjIterator.of("a").toImmutableList();
        Assertions.assertSame(single, single.reversed());

        // ownership is inherited by every iterator type in this package, not just ObjIterator
        final ImmutableList<Integer> fromPrimitive = IntIterator.of(1, 2).toImmutableList();
        Assertions.assertSame(fromPrimitive, ImmutableList.copyOf(fromPrimitive));

        Assertions.assertEquals(Collections.emptyList(), ObjIterator.<String> empty().toImmutableList());
        Assertions.assertEquals(Collections.emptySet(), ObjIterator.<String> empty().toImmutableSet());
    }

    /** Memoises the storage it hands back from the overridable collectors, as any subclass legally may. */
    private static final class RetainingIterator extends ObjIterator<String> {
        static final List<String> RETAINED_LIST = new java.util.ArrayList<>();

        static final Set<String> RETAINED_SET = new java.util.LinkedHashSet<>();

        private final Iterator<String> delegate = Arrays.asList("a", "b").iterator();

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public String next() {
            return delegate.next();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <C extends java.util.Collection<String>> C toCollection(final java.util.function.Supplier<? extends C> supplier) {
            while (hasNext()) {
                RETAINED_LIST.add(next());
            }

            return (C) RETAINED_LIST;
        }

        @Override
        public Set<String> toSet() {
            while (hasNext()) {
                RETAINED_SET.add(next());
            }

            return RETAINED_SET;
        }
    }

    @Test
    public void testToImmutableListAndSetDoNotRouteThroughTheOverridableCollectors() {
        // toCollection(Supplier) and toSet() are public and non-final, and are inherited by twelve public
        // iterator classes, so a result built from their return value could be backed by storage a subclass
        // still holds - making ownsBacking=true a lie and letting copyOf() hand out a value that can change.
        // toImmutableList()/toImmutableSet() therefore drain into storage they create themselves.
        RetainingIterator.RETAINED_LIST.clear();
        RetainingIterator.RETAINED_SET.clear();

        final ImmutableList<String> list = new RetainingIterator().toImmutableList();
        final ImmutableList<String> listCopy = ImmutableList.copyOf(list);
        RetainingIterator.RETAINED_LIST.add("INJECTED");

        Assertions.assertEquals(Arrays.asList("a", "b"), listCopy, "copyOf() handed out a value that can change");
        Assertions.assertEquals(Arrays.asList("a", "b"), list, "toImmutableList() published storage the subclass retains");

        final ImmutableSet<String> set = new RetainingIterator().toImmutableSet();
        final ImmutableSet<String> setCopy = ImmutableSet.copyOf(set);
        RetainingIterator.RETAINED_SET.add("INJECTED");

        Assertions.assertEquals(new java.util.LinkedHashSet<>(Arrays.asList("a", "b")), setCopy, "copyOf() handed out a value that can change");
        Assertions.assertEquals(new java.util.LinkedHashSet<>(Arrays.asList("a", "b")), set, "toImmutableSet() published storage the subclass retains");

        // the subclass's own override is untouched - it is still what toCollection()/toSet() answer
        RetainingIterator.RETAINED_LIST.clear();
        Assertions.assertSame(RetainingIterator.RETAINED_LIST, new RetainingIterator().toCollection(java.util.ArrayList::new));
        Assertions.assertSame(RetainingIterator.RETAINED_SET, new RetainingIterator().toSet());
    }
}
