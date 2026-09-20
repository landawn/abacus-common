package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ImmutableNavigableSetTest extends TestBase {

    @Test
    public void testEmpty() {
        ImmutableNavigableSet<String> emptySet = ImmutableNavigableSet.empty();
        Assertions.assertTrue(emptySet.isEmpty());
        Assertions.assertEquals(0, emptySet.size());
        Assertions.assertNull(emptySet.lower("any"));
        Assertions.assertNull(emptySet.higher("any"));
    }

    @Test
    public void testOf_TwoElements() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(2, 1);
        Assertions.assertEquals(2, set.size());
        Assertions.assertEquals(1, set.first());
        Assertions.assertEquals(2, set.last());
    }

    @Test
    public void testOf_ThreeElements() {
        ImmutableNavigableSet<String> set = ImmutableNavigableSet.of("beta", "alpha", "gamma");
        Assertions.assertEquals(3, set.size());
        Iterator<String> iter = set.iterator();
        Assertions.assertEquals("alpha", iter.next());
        Assertions.assertEquals("beta", iter.next());
        Assertions.assertEquals("gamma", iter.next());
    }

    @Test
    public void testOf_FourElements() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(4, 2, 3, 1);
        Assertions.assertEquals(4, set.size());
        Assertions.assertEquals(1, set.first());
        Assertions.assertEquals(4, set.last());
    }

    @Test
    public void testOf_FiveElements() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(5, 3, 1, 4, 2);
        Assertions.assertEquals(5, set.size());
        Assertions.assertTrue(set.contains(3));
    }

    @Test
    public void testOf_SixElements() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(6, 5, 4, 3, 2, 1);
        Assertions.assertEquals(6, set.size());
        Assertions.assertEquals(1, set.first());
        Assertions.assertEquals(6, set.last());
    }

    @Test
    public void testOf_SevenElements() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(7, 6, 5, 4, 3, 2, 1);
        Assertions.assertEquals(7, set.size());
    }

    @Test
    public void testOf_EightElements() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(8, 7, 6, 5, 4, 3, 2, 1);
        Assertions.assertEquals(8, set.size());
    }

    @Test
    public void testOf_NineElements() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(9, 8, 7, 6, 5, 4, 3, 2, 1);
        Assertions.assertEquals(9, set.size());
    }

    @Test
    public void testOf_TenElements() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1);
        Assertions.assertEquals(10, set.size());
        Assertions.assertEquals(1, set.first());
        Assertions.assertEquals(10, set.last());
    }

    @Test
    public void testNavigationWithStrings() {
        ImmutableNavigableSet<String> set = ImmutableNavigableSet.of("apple", "banana", "cherry", "date", "elderberry");

        Assertions.assertEquals("banana", set.higher("apple"));
        Assertions.assertEquals("cherry", set.ceiling("cherry"));
        Assertions.assertEquals("banana", set.floor("banana"));
        Assertions.assertEquals("apple", set.lower("banana"));

        Assertions.assertNull(set.lower("apple"));
        Assertions.assertNull(set.higher("elderberry"));
    }

    @Test
    public void testOf_SingleElement() {
        ImmutableNavigableSet<String> set = ImmutableNavigableSet.of("single");
        Assertions.assertEquals(1, set.size());
        Assertions.assertTrue(set.contains("single"));
    }

    @Test
    public void testMutationMethods_ThrowUnsupported() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.add(4));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.remove(2));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.clear());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.pollFirst());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.pollLast());
    }

    @Test
    public void testWithCustomComparator() {
        NavigableSet<String> source = new TreeSet<>(Comparator.reverseOrder());
        source.addAll(Arrays.asList("a", "b", "c"));

        ImmutableNavigableSet<String> set = ImmutableNavigableSet.copyOf(source);
        Iterator<String> iter = set.iterator();
        Assertions.assertEquals("c", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("a", iter.next());
    }

    @Test
    public void testCopyOf_EmptySortedSetRetainsComparator() {
        Comparator<String> comparator = Comparator.reverseOrder();
        SortedSet<String> source = new TreeSet<>(comparator);

        Assertions.assertSame(comparator, ImmutableNavigableSet.copyOf(source).comparator());
    }

    @Test
    public void testCopyOf_Array() {
        String[] values = { "c", "a", "b", "a" };
        ImmutableNavigableSet<String> set = ImmutableNavigableSet.copyOf(values);

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(set));
        values[0] = "z";
        Assertions.assertFalse(set.contains("z"));
        Assertions.assertSame(ImmutableNavigableSet.empty(), ImmutableNavigableSet.copyOf(new String[0]));
        Assertions.assertSame(ImmutableNavigableSet.empty(), ImmutableNavigableSet.copyOf((String[]) null));
    }

    @Test
    public void testCopyOf() {
        List<String> list = Arrays.asList("charlie", "alpha", "beta");
        ImmutableNavigableSet<String> set = ImmutableNavigableSet.copyOf(list);

        Assertions.assertEquals(3, set.size());
        Iterator<String> iter = set.iterator();
        Assertions.assertEquals("alpha", iter.next());
        Assertions.assertEquals("beta", iter.next());
        Assertions.assertEquals("charlie", iter.next());
    }

    @Test
    public void testCopyOf_AlreadyImmutable() {
        ImmutableNavigableSet<String> original = ImmutableNavigableSet.of("a", "b");
        ImmutableNavigableSet<String> copy = ImmutableNavigableSet.copyOf(original);
        Assertions.assertSame(original, copy);
    }

    @Test
    public void testCopyOf_Empty() {
        ImmutableNavigableSet<String> set = ImmutableNavigableSet.copyOf(new ArrayList<>());
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testCopyOf_Null() {
        ImmutableNavigableSet<String> set = ImmutableNavigableSet.copyOf((Collection<String>) null);
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testWrap() {
        NavigableSet<String> mutable = new TreeSet<>();
        mutable.add("b");
        mutable.add("a");

        ImmutableNavigableSet<String> wrapped = ImmutableNavigableSet.wrap(mutable);
        Assertions.assertEquals(2, wrapped.size());

        mutable.add("c");
        Assertions.assertEquals(3, wrapped.size());
        Assertions.assertTrue(wrapped.contains("c"));
    }

    @Test
    public void testWrap_AlreadyImmutable() {
        ImmutableNavigableSet<String> original = ImmutableNavigableSet.of("a");
        ImmutableNavigableSet<String> wrapped = ImmutableNavigableSet.wrap(original);
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testWrap_Null() {
        ImmutableNavigableSet<String> wrapped = ImmutableNavigableSet.wrap(null);
        Assertions.assertTrue(wrapped.isEmpty());
    }

    @Test
    public void testWrap_SortedSet_Deprecated() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            ImmutableNavigableSet.wrap((SortedSet<String>) new TreeSet<String>());
        });
    }

    @Test
    public void testLower() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 3, 5, 7, 9);

        Assertions.assertNull(set.lower(1));
        Assertions.assertEquals(1, set.lower(2));
        Assertions.assertEquals(1, set.lower(3));
        Assertions.assertEquals(3, set.lower(4));
        Assertions.assertEquals(3, set.lower(5));
        Assertions.assertEquals(5, set.lower(6));
        Assertions.assertEquals(9, set.lower(10));
    }

    @Test
    public void testFloor() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 3, 5, 7, 9);

        Assertions.assertNull(set.floor(0));
        Assertions.assertEquals(1, set.floor(1));
        Assertions.assertEquals(1, set.floor(2));
        Assertions.assertEquals(3, set.floor(3));
        Assertions.assertEquals(3, set.floor(4));
        Assertions.assertEquals(5, set.floor(5));
        Assertions.assertEquals(9, set.floor(10));
    }

    @Test
    public void testCeiling() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 3, 5, 7, 9);

        Assertions.assertEquals(1, set.ceiling(0));
        Assertions.assertEquals(1, set.ceiling(1));
        Assertions.assertEquals(3, set.ceiling(2));
        Assertions.assertEquals(3, set.ceiling(3));
        Assertions.assertEquals(5, set.ceiling(4));
        Assertions.assertEquals(5, set.ceiling(5));
        Assertions.assertNull(set.ceiling(10));
    }

    @Test
    public void testHigher() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 3, 5, 7, 9);

        Assertions.assertEquals(1, set.higher(0));
        Assertions.assertEquals(3, set.higher(1));
        Assertions.assertEquals(3, set.higher(2));
        Assertions.assertEquals(5, set.higher(3));
        Assertions.assertEquals(5, set.higher(4));
        Assertions.assertEquals(7, set.higher(5));
        Assertions.assertNull(set.higher(9));
        Assertions.assertNull(set.higher(10));
    }

    @Test
    public void testPollFirst_ThrowsUnsupported() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.pollFirst());
    }

    @Test
    public void testPollLast_ThrowsUnsupported() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.pollLast());
    }

    @Test
    public void testDescendingSet() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 3, 5, 7, 9);
        ImmutableNavigableSet<Integer> descending = set.descendingSet();

        Assertions.assertEquals(5, descending.size());
        Iterator<Integer> iter = descending.iterator();
        Assertions.assertEquals(9, iter.next());
        Assertions.assertEquals(7, iter.next());
        Assertions.assertEquals(5, iter.next());
        Assertions.assertEquals(3, iter.next());
        Assertions.assertEquals(1, iter.next());
    }

    @Test
    public void testDescendingIterator() {
        ImmutableNavigableSet<String> set = ImmutableNavigableSet.of("a", "b", "c");
        ObjIterator<String> iter = set.descendingIterator();

        Assertions.assertEquals("c", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testDescendingIteratorDoesNotExposeMutableObjIterator() {
        TreeSet<String> backing = new TreeSet<>(Arrays.asList("a", "b", "c")) {
            @Override
            public ObjIterator<String> descendingIterator() {
                Iterator<String> delegate = super.descendingIterator();

                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        return delegate.hasNext();
                    }

                    @Override
                    public String next() {
                        return delegate.next();
                    }

                    @Override
                    public void remove() {
                        delegate.remove();
                    }
                };
            }
        };

        ObjIterator<String> iter = ImmutableNavigableSet.wrap(backing).descendingIterator();
        Assertions.assertEquals("c", iter.next());
        Assertions.assertThrows(UnsupportedOperationException.class, iter::remove);
        Assertions.assertEquals(new TreeSet<>(Arrays.asList("a", "b", "c")), backing);
    }

    @Test
    public void testDescendingIteratorPropagatesTheBackingSetsExhaustionException() {
        // descendingIterator() goes through ObjIterator.of(..), which does NOT normalise exhaustion, so the
        // backing TreeSet's own message-less NoSuchElementException reaches the caller rather than this
        // library's standard message. Pinned because normalising it was tried and deliberately reverted.
        ObjIterator<String> iter = ImmutableNavigableSet.of("a", "b").descendingIterator();
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertFalse(iter.hasNext());

        java.util.NoSuchElementException ex = Assertions.assertThrows(java.util.NoSuchElementException.class, iter::next);
        Assertions.assertNotEquals(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX, ex.getMessage());
        Assertions.assertNull(ex.getMessage());
    }

    @Test
    public void testSubSet_Inclusive() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
        ImmutableNavigableSet<Integer> sub = set.subSet(2, true, 4, false);

        Assertions.assertEquals(2, sub.size());
        Assertions.assertTrue(sub.contains(2));
        Assertions.assertTrue(sub.contains(3));
        Assertions.assertFalse(sub.contains(4));
    }

    @Test
    public void testSubSet_Exclusive() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
        ImmutableNavigableSet<Integer> sub = set.subSet(2, false, 4, false);

        Assertions.assertEquals(1, sub.size());
        Assertions.assertTrue(sub.contains(3));
    }

    @Test
    public void testSubSet_BothInclusive() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
        ImmutableNavigableSet<Integer> sub = set.subSet(2, true, 4, true);

        Assertions.assertEquals(3, sub.size());
        Assertions.assertTrue(sub.contains(2));
        Assertions.assertTrue(sub.contains(3));
        Assertions.assertTrue(sub.contains(4));
    }

    @Test
    public void testHeadSet_Inclusive() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
        ImmutableNavigableSet<Integer> head = set.headSet(3, true);

        Assertions.assertEquals(3, head.size());
        Assertions.assertTrue(head.contains(1));
        Assertions.assertTrue(head.contains(2));
        Assertions.assertTrue(head.contains(3));
        Assertions.assertFalse(head.contains(4));
    }

    @Test
    public void testHeadSet_Exclusive() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
        ImmutableNavigableSet<Integer> head = set.headSet(3, false);

        Assertions.assertEquals(2, head.size());
        Assertions.assertTrue(head.contains(1));
        Assertions.assertTrue(head.contains(2));
        Assertions.assertFalse(head.contains(3));
    }

    @Test
    public void testTailSet_Inclusive() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
        ImmutableNavigableSet<Integer> tail = set.tailSet(3, true);

        Assertions.assertEquals(3, tail.size());
        Assertions.assertTrue(tail.contains(3));
        Assertions.assertTrue(tail.contains(4));
        Assertions.assertTrue(tail.contains(5));
        Assertions.assertFalse(tail.contains(2));
    }

    @Test
    public void testTailSet_Exclusive() {
        ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
        ImmutableNavigableSet<Integer> tail = set.tailSet(3, false);

        Assertions.assertEquals(2, tail.size());
        Assertions.assertTrue(tail.contains(4));
        Assertions.assertTrue(tail.contains(5));
        Assertions.assertFalse(tail.contains(3));
    }

    /** A key/element type that is deliberately NOT Comparable. */
    private static final class NotComparableNS {
        private final String s;

        NotComparableNS(final String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return s;
        }
    }

    @Test
    public void testOf_nonComparableElementSelectsInheritedUnsortedFactory() {
        // of(...) requires E extends Comparable. A non-Comparable element is not applicable to
        // ImmutableNavigableSet.of, so the call binds to ImmutableSet.of and returns an unsorted set.
        final ImmutableSet<NotComparableNS> one = ImmutableNavigableSet.of(new NotComparableNS("z"));
        final ImmutableSet<NotComparableNS> two = ImmutableNavigableSet.of(new NotComparableNS("z"), new NotComparableNS("a"));
        Assertions.assertFalse(one instanceof SortedSet);
        Assertions.assertFalse(two instanceof SortedSet);
        Assertions.assertEquals(1, one.size());
        Assertions.assertEquals(2, two.size());
    }

    @Test
    public void testCopyOfPreservesComparatorForNonComparableElements() {
        final Comparator<NotComparableNS> comparator = Comparator.comparing(element -> element.s);
        final SortedSet<NotComparableNS> source = new TreeSet<>(comparator);
        final NotComparableNS first = new NotComparableNS("a");
        final NotComparableNS last = new NotComparableNS("z");
        source.add(last);
        source.add(first);

        final ImmutableNavigableSet<NotComparableNS> copy = ImmutableNavigableSet.copyOf(source);
        Assertions.assertSame(comparator, copy.comparator());
        Assertions.assertEquals(Arrays.asList(first, last), new ArrayList<>(copy));
        Assertions.assertSame(last, copy.ceiling(new NotComparableNS("m")));
        source.clear();
        Assertions.assertEquals(2, copy.size());
        Assertions.assertSame(comparator, ImmutableNavigableSet.copyOf(source).comparator());
        Assertions.assertThrows(ClassCastException.class, () -> ImmutableNavigableSet.copyOf(Arrays.asList(first, last)));
    }

    @Test
    public void testOf_isStillSortedForComparableElements() {
        final ImmutableNavigableSet<String> s = ImmutableNavigableSet.of("c", "a", "b");

        Assertions.assertEquals(java.util.Arrays.asList("a", "b", "c"), new java.util.ArrayList<>(s));
        Assertions.assertEquals("a", s.first());
        Assertions.assertEquals("c", s.last());
        Assertions.assertNull(s.comparator());
        // a null element still fails as NullPointerException (from TreeSet's natural ordering), not CCE
        Assertions.assertThrows(NullPointerException.class, () -> ImmutableNavigableSet.of((String) null));
    }

    @Test
    public void testBuilderIsBlocked() {
        // ImmutableSet.builder() is inherited through this class's name and would silently build an
        // unsorted ImmutableSet in insertion order.
        Assertions.assertThrows(UnsupportedOperationException.class, ImmutableNavigableSet::builder);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> ImmutableNavigableSet.builder(new java.util.TreeSet<String>()));
    }

    @Test
    public void testCopyOf_copiesAWrappedView() {
        final java.util.TreeSet<String> live = new java.util.TreeSet<>(java.util.List.of("a"));
        final ImmutableNavigableSet<String> view = ImmutableNavigableSet.wrap(live);
        final ImmutableNavigableSet<String> copy = ImmutableNavigableSet.copyOf(view);

        Assertions.assertNotSame(view, copy);

        live.add("b");

        Assertions.assertEquals(2, view.size());
        Assertions.assertEquals(1, copy.size());
        Assertions.assertFalse(copy.contains("b"));
    }

    @Test
    public void testRangeViewsInheritOwnership() {
        final ImmutableNavigableSet<String> owned = ImmutableNavigableSet.of("a", "b", "c");
        final ImmutableNavigableSet<String> ownedHead = owned.headSet("c", false);
        Assertions.assertSame(ownedHead, ImmutableNavigableSet.copyOf(ownedHead));

        final java.util.TreeSet<String> live = new java.util.TreeSet<>(java.util.List.of("a", "c"));
        final ImmutableNavigableSet<String> viewHead = ImmutableNavigableSet.wrap(live).headSet("d", false);
        final ImmutableNavigableSet<String> copy = ImmutableNavigableSet.copyOf(viewHead);
        Assertions.assertNotSame(viewHead, copy);

        live.add("b");
        Assertions.assertEquals(3, viewHead.size());
        Assertions.assertEquals(2, copy.size());

        Assertions.assertThrows(UnsupportedOperationException.class, () -> viewHead.add("z"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> ownedHead.add("z"));
    }

    @Test
    public void testDescendingSetTraversesInDescendingOrder() {
        // ImmutableCollection.spliterator()/forEach() delegate to the backing collection. For a descending
        // set the backing collection IS the descending view, so every traversal must agree.
        final ImmutableNavigableSet<String> descending = ImmutableNavigableSet.of("a", "b", "c").descendingSet();
        final java.util.List<String> expected = java.util.Arrays.asList("c", "b", "a");

        Assertions.assertEquals(expected, new java.util.ArrayList<>(descending));
        Assertions.assertEquals(expected, descending.stream().toList());
        Assertions.assertEquals(expected, descending.parallelStream().toList());
        Assertions.assertEquals(expected, java.util.Arrays.asList(descending.toArray()));

        final java.util.List<String> seen = new java.util.ArrayList<>();
        descending.forEach(seen::add);
        Assertions.assertEquals(expected, seen);

        final java.util.List<String> spliterated = new java.util.ArrayList<>();
        descending.spliterator().forEachRemaining(spliterated::add);
        Assertions.assertEquals(expected, spliterated);

        Assertions.assertEquals("c", descending.first());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> descending.add("z"));
    }

    @Test
    public void reversed_isTheNarrowedDescendingSetView() {
        final ImmutableNavigableSet<String> set = ImmutableNavigableSet.of("a", "b", "c");

        // covariant re-override of ImmutableSortedSet.reversed(); its behaviour is that of descendingSet()
        final ImmutableNavigableSet<String> reversed = set.reversed();
        Assertions.assertEquals("[c, b, a]", reversed.toString());
        Assertions.assertEquals(set.descendingSet().toString(), reversed.toString());
        Assertions.assertTrue(reversed instanceof Immutable);
        Assertions.assertEquals("c", reversed.first());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> reversed.add("z"));
    }

    @Test
    public void copyOf_returnsEveryDerivedViewOfAnOwningSourceUnchanged() {
        // pins the copyOf(Collection) memory note: every derived view of an owning source owns its backing
        // storage too, so copyOf hands it straight back and the whole parent stays reachable.
        final ImmutableNavigableSet<Integer> owning = ImmutableNavigableSet.of(1, 2, 3);

        final ImmutableNavigableSet<Integer> descending = owning.descendingSet();
        Assertions.assertSame(descending, ImmutableNavigableSet.copyOf(descending));
        Assertions.assertSame(descending, ImmutableSortedSet.copyOf(descending));

        // the remaining four views the same paragraph enumerates - subSet/headSet/tailSet/reversed
        final ImmutableNavigableSet<Integer> sub = owning.subSet(1, 3);
        Assertions.assertSame(sub, ImmutableNavigableSet.copyOf(sub));

        final ImmutableNavigableSet<Integer> head = owning.headSet(3);
        Assertions.assertSame(head, ImmutableNavigableSet.copyOf(head));

        final ImmutableNavigableSet<Integer> tail = owning.tailSet(2);
        Assertions.assertSame(tail, ImmutableNavigableSet.copyOf(tail));

        final ImmutableNavigableSet<Integer> reversed = owning.reversed();
        Assertions.assertSame(reversed, ImmutableNavigableSet.copyOf(reversed));
        Assertions.assertSame(reversed, ImmutableSortedSet.copyOf(reversed));

        // and the inclusive navigable forms
        final ImmutableNavigableSet<Integer> subInclusive = owning.subSet(1, true, 3, true);
        Assertions.assertSame(subInclusive, ImmutableNavigableSet.copyOf(subInclusive));

        final ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "a", 2, "b", 3, "c");

        // a key-set view is handed back too, and it retains the parent MAP, its values included
        final ImmutableNavigableSet<Integer> keys = map.navigableKeySet();
        Assertions.assertSame(keys, ImmutableNavigableSet.copyOf(keys));
        Assertions.assertSame(keys, ImmutableSortedSet.copyOf(keys));

        final ImmutableNavigableSet<Integer> descendingKeys = map.descendingKeySet();
        Assertions.assertSame(descendingKeys, ImmutableNavigableSet.copyOf(descendingKeys));
    }
}
