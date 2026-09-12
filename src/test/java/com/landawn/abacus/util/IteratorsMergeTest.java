package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.BiFunction;

public class IteratorsMergeTest extends IteratorsTestSupport {
    @Test
    public void testMergeIterators() {
        Iterator<Integer> a = list(1, 3, 5).iterator();
        Iterator<Integer> b = list(2, 4, 6).iterator();
        BiFunction<Integer, Integer, MergeResult> selector = (i1, i2) -> (i1 <= i2) ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        ObjIterator<Integer> merged = Iterators.merge(a, b, selector);

        assertEquals(1, merged.next());
        assertEquals(2, merged.next());
        assertEquals(3, merged.next());
        assertEquals(4, merged.next());
        assertEquals(5, merged.next());
        assertEquals(6, merged.next());
        assertFalse(merged.hasNext());

        a = list(1, 3).iterator();
        b = list(new Integer[0]).iterator();
        merged = Iterators.merge(a, b, selector);
        assertEquals(1, merged.next());
        assertEquals(3, merged.next());
        assertFalse(merged.hasNext());
    }

    @Test
    public void testMerge() {
        Iterator<Integer> iter1 = Arrays.asList(1, 3, 5).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4, 6).iterator();

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ObjIterator<Integer> result = Iterators.merge(iter1, iter2, selector);

        assertEquals(Integer.valueOf(1), result.next());
        assertEquals(Integer.valueOf(2), result.next());
        assertEquals(Integer.valueOf(3), result.next());
        assertEquals(Integer.valueOf(4), result.next());
        assertEquals(Integer.valueOf(5), result.next());
        assertEquals(Integer.valueOf(6), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeIterables_Dedicated() {
        List<Integer> a = Arrays.asList(1, 3, 5);
        List<Integer> b = Arrays.asList(2, 4, 6);
        ObjIterator<Integer> iter = Iterators.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), iter.toList());
    }

    @Test
    public void testMergeCollection_TwoIterators() {
        List<Integer> list1 = Arrays.asList(1, 3, 5);
        List<Integer> list2 = Arrays.asList(2, 4, 6);
        List<Iterator<? extends Integer>> iterators = Arrays.asList(list1.iterator(), list2.iterator());

        ObjIterator<Integer> result = Iterators.merge(iterators, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        List<Integer> merged = new ArrayList<>();
        result.forEachRemaining(merged::add);
        assertEquals(6, merged.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), merged);
    }

    @Test
    public void testMergeCollection_ThreeIterators() {
        List<Integer> list1 = Arrays.asList(1, 4);
        List<Integer> list2 = Arrays.asList(2, 5);
        List<Integer> list3 = Arrays.asList(3, 6);
        List<Iterator<? extends Integer>> iterators = Arrays.asList(list1.iterator(), list2.iterator(), list3.iterator());

        ObjIterator<Integer> result = Iterators.merge(iterators, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        List<Integer> merged = new ArrayList<>();
        result.forEachRemaining(merged::add);
        assertEquals(6, merged.size());
    }

    @Test
    public void testMergeTwoIterables() {
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ObjIterator<Integer> result = Iterators.merge(Arrays.asList(1, 3, 5), Arrays.asList(2, 4, 6), selector);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result.toList());

        result = Iterators.merge((Iterable<Integer>) null, Arrays.asList(1), selector);
        assertEquals(Arrays.asList(1), result.toList());
    }

    @Test
    public void testMergeCollectionOfIterators() {
        List<Iterator<Integer>> iterators = list(list(1, 5).iterator(), list(2, 4).iterator(), list(3, 6).iterator());
        BiFunction<Integer, Integer, MergeResult> selector = (i1, i2) -> (i1 <= i2) ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        ObjIterator<Integer> merged = Iterators.merge(iterators, selector);
        List<Integer> result = new ArrayList<>();
        merged.forEachRemaining(result::add);
        assertEquals(list(1, 2, 3, 4, 5, 6), result);

        assertFalse(Iterators.merge((Collection<Iterator<Integer>>) null, selector).hasNext());
    }

    @Test
    public void testMerge_BothNull() {
        java.util.function.BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> MergeResult.TAKE_FIRST;
        ObjIterator<Integer> result = Iterators.merge((Iterator<Integer>) null, (Iterator<Integer>) null, selector);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMerge_FirstEmpty() {
        java.util.function.BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> MergeResult.TAKE_FIRST;
        ObjIterator<Integer> result = Iterators.merge(Collections.<Integer> emptyIterator(), Arrays.asList(1, 2).iterator(), selector);
        assertEquals(Arrays.asList(1, 2), result.toList());
    }

    @Test
    public void testMerge_SecondEmpty() {
        java.util.function.BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> MergeResult.TAKE_FIRST;
        ObjIterator<Integer> result = Iterators.merge(Arrays.asList(1, 2).iterator(), Collections.<Integer> emptyIterator(), selector);
        assertEquals(Arrays.asList(1, 2), result.toList());
    }

    @Test
    public void testMergeCollection_Empty() {
        ObjIterator<Integer> result = Iterators.merge(Collections.emptyList(), (a, b) -> MergeResult.TAKE_FIRST);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeCollection_SingleIterator() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        List<Iterator<? extends Integer>> iterators = Collections.singletonList(list.iterator());

        ObjIterator<Integer> result = Iterators.merge(iterators, (a, b) -> MergeResult.TAKE_FIRST);
        List<Integer> merged = new ArrayList<>();
        result.forEachRemaining(merged::add);
        assertEquals(Arrays.asList(1, 2, 3), merged);
    }

    @Test
    public void testMergeTwoIterators() {
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ObjIterator<Integer> result = Iterators.merge(Arrays.asList(1, 3, 5).iterator(), Arrays.asList(2, 4, 6).iterator(), selector);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result.toList());

        result = Iterators.merge((Iterator<Integer>) null, Arrays.asList(1).iterator(), selector);
        assertEquals(Arrays.asList(1), result.toList());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Iterators.merge(Arrays.asList(1).iterator(), Arrays.asList(2).iterator(), null));
    }

    @Test
    public void testMergeCollection() {
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        Collection<Iterator<Integer>> collection = Arrays.asList(Arrays.asList(1, 4).iterator(), Arrays.asList(2, 5).iterator(),
                Arrays.asList(3, 6).iterator());

        ObjIterator<Integer> result = Iterators.merge(collection, selector);
        assertNotNull(result);

        result = Iterators.merge((Collection<Iterator<Integer>>) null, selector);
        assertFalse(result.hasNext());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.merge(Arrays.asList(Arrays.asList(1).iterator()), null));
    }

    @Test
    public void testMergeIterablesCollection_Dedicated() {
        List<Iterable<Integer>> iterables = Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5), Arrays.asList(3, 6));
        ObjIterator<Integer> iter = Iterators.mergeIterables(iterables, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        List<Integer> result = iter.toList();
        assertEquals(6, result.size());
    }

    @Test
    public void testMergeIterablesCollection() {
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        Collection<Iterable<Integer>> collection = Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5));

        ObjIterator<Integer> result = Iterators.mergeIterables(collection, selector);
        assertNotNull(result);

        result = Iterators.mergeIterables(null, selector);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeIterablesEdgeCases() {
        List<Iterable<Integer>> empty = new ArrayList<>();
        ObjIterator<Integer> result = Iterators.mergeIterables(empty, (a, b) -> MergeResult.TAKE_FIRST);
        assertFalse(result.hasNext());

        List<Iterable<Integer>> single = Arrays.asList(Arrays.asList(1, 2, 3));
        result = Iterators.mergeIterables(single, (a, b) -> MergeResult.TAKE_FIRST);
        assertEquals(Integer.valueOf(1), result.next());
        assertEquals(Integer.valueOf(2), result.next());
        assertEquals(Integer.valueOf(3), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeIterables_BothNull() {
        java.util.function.BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> MergeResult.TAKE_FIRST;
        ObjIterator<Integer> result = Iterators.merge((Iterable<Integer>) null, (Iterable<Integer>) null, selector);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeIterables() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.mergeIterables(Arrays.asList(Arrays.asList(1)), null));

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        Collection<Iterable<Integer>> coll = Collections.emptyList();
        ObjIterator<Integer> iter = Iterators.mergeIterables(coll, selector);
        assertFalse(iter.hasNext());

        List<Iterable<Integer>> iterables = new ArrayList<>();
        iterables.add(Arrays.asList(1, 4));
        iterables.add(Arrays.asList(2, 5));
        iterables.add(Arrays.asList(3, 6));

        iter = Iterators.mergeIterables(iterables, selector);
        for (int i = 1; i <= 6; i++) {
            assertEquals(i, iter.next());
        }
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeSortedIterablesWithComparator() {
        Comparator<Integer> cmp = Integer::compareTo;

        ObjIterator<Integer> result = Iterators.mergeSorted(Arrays.asList(1, 3), Arrays.asList(2, 4), cmp);
        assertEquals(Arrays.asList(1, 2, 3, 4), result.toList());
    }

    @Test
    public void testMergeSortedIteratorsComparable() {
        Iterator<Integer> a = list(1, 3, 5, 8).iterator();
        Iterator<Integer> b = list(2, 3, 6, 7).iterator();
        ObjIterator<Integer> merged = Iterators.mergeSorted(a, b);

        assertEquals(1, merged.next());
        assertEquals(2, merged.next());
        assertEquals(3, merged.next());
        assertEquals(3, merged.next());
        assertEquals(5, merged.next());
        assertEquals(6, merged.next());
        assertEquals(7, merged.next());
        assertEquals(8, merged.next());
        assertFalse(merged.hasNext());
    }

    @Test
    public void testMergeSortedIteratorsComparator() {
        Iterator<Integer> a = list(5, 3, 1).iterator();
        Iterator<Integer> b = list(6, 4, 2).iterator();
        ObjIterator<Integer> merged = Iterators.mergeSorted(a, b, Comparator.reverseOrder());

        assertEquals(6, merged.next());
        assertEquals(5, merged.next());
        assertEquals(4, merged.next());
        assertEquals(3, merged.next());
        assertEquals(2, merged.next());
        assertEquals(1, merged.next());
        assertFalse(merged.hasNext());
    }

    @Test
    public void testMergeSorted() {
        Iterator<Integer> iter1 = Arrays.asList(1, 3, 5).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4, 6).iterator();

        ObjIterator<Integer> result = Iterators.mergeSorted(iter1, iter2);

        assertEquals(Integer.valueOf(1), result.next());
        assertEquals(Integer.valueOf(2), result.next());
        assertEquals(Integer.valueOf(3), result.next());
        assertEquals(Integer.valueOf(4), result.next());
        assertEquals(Integer.valueOf(5), result.next());
        assertEquals(Integer.valueOf(6), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeSortedIterables() {
        Iterable<Integer> iter1 = Arrays.asList(1, 3, 5);
        Iterable<Integer> iter2 = Arrays.asList(2, 4, 6);

        ObjIterator<Integer> result = Iterators.mergeSorted(iter1, iter2);

        assertEquals(Integer.valueOf(1), result.next());
        assertEquals(Integer.valueOf(2), result.next());
        assertEquals(Integer.valueOf(3), result.next());
        assertEquals(Integer.valueOf(4), result.next());
        assertEquals(Integer.valueOf(5), result.next());
        assertEquals(Integer.valueOf(6), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeSortedIterables_Dedicated() {
        ObjIterator<Integer> iter = Iterators.mergeSorted(Arrays.asList(1, 3, 5), Arrays.asList(2, 4, 6));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), iter.toList());
    }

    @Test
    public void testMergeSortedIterablesWithComparator_Dedicated() {
        ObjIterator<Integer> iter = Iterators.mergeSorted(Arrays.asList(5, 3, 1), Arrays.asList(6, 4, 2), Comparator.reverseOrder());
        assertEquals(Arrays.asList(6, 5, 4, 3, 2, 1), iter.toList());
    }

    @Test
    public void testMergeSorted_DuplicateElements() {
        ObjIterator<Integer> result = Iterators.mergeSorted(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(1, 2, 3).iterator());
        assertEquals(Arrays.asList(1, 1, 2, 2, 3, 3), result.toList());
    }

    @Test
    public void testMergeSortedComparable() {
        ObjIterator<Integer> result = Iterators.mergeSorted(Arrays.asList(1, 3, 5).iterator(), Arrays.asList(2, 4, 6).iterator());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result.toList());

        result = Iterators.mergeSorted(Arrays.asList(1).iterator(), (Iterator<Integer>) null);
        assertEquals(Arrays.asList(1), result.toList());
    }

    @Test
    public void testMergeSortedWithComparator() {
        Comparator<Integer> cmp = Integer::compareTo;

        ObjIterator<Integer> result = Iterators.mergeSorted(Arrays.asList(1, 3, 5).iterator(), Arrays.asList(2, 4, 6).iterator(), cmp);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result.toList());

        result = Iterators.mergeSorted((Iterator<Integer>) null, Arrays.asList(1).iterator(), cmp);
        assertEquals(Arrays.asList(1), result.toList());
    }

    @Test
    public void testMergeSortedIterablesComparable() {
        ObjIterator<String> result = Iterators.mergeSorted(Arrays.asList("a", "c", "e"), Arrays.asList("b", "d"));
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result.toList());

        result = Iterators.mergeSorted(Arrays.asList("x"), (Iterable<String>) null);
        assertEquals(Arrays.asList("x"), result.toList());
    }

    @Test
    public void testMergeSorted_BothNull() {
        ObjIterator<Integer> result = Iterators.mergeSorted((Iterator<Integer>) null, (Iterator<Integer>) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeSorted_BothEmpty() {
        ObjIterator<Integer> result = Iterators.mergeSorted(Collections.<Integer> emptyIterator(), Collections.<Integer> emptyIterator());
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeSortedIterables_BothNull() {
        ObjIterator<Integer> result = Iterators.mergeSorted((Iterable<Integer>) null, (Iterable<Integer>) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeSortedIteratorsWithComparator() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Iterators.mergeSorted(Arrays.asList(1).iterator(), Arrays.asList(2).iterator(), null));

        Comparator<String> cmp = (a, b) -> a.compareTo(b);
        Iterator<String> iter1 = Arrays.asList("a", "c", "e").iterator();
        Iterator<String> iter2 = Arrays.asList("b", "d", "f").iterator();

        ObjIterator<String> iter = Iterators.mergeSorted(iter1, iter2, cmp);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertEquals("e", iter.next());
        assertEquals("f", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeSortedWithComparator_NullComparator() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Iterators.mergeSorted(Arrays.asList(1).iterator(), Arrays.asList(2).iterator(), null));
    }

    @Test
    public void testMergeSorted_Equals_PrefersFirst() {
        Iterator<Integer> a = Arrays.asList(1, 3, 5).iterator();
        Iterator<Integer> b = Arrays.asList(3, 4, 6).iterator();
        // With minFirst comparator (natural), equal elements: TAKE_FIRST first.
        ObjIterator<Integer> r = Iterators.mergeSorted(a, b);
        assertEquals(Arrays.asList(1, 3, 3, 4, 5, 6), r.toList());
    }

    /** B3: the same for {@code merge}, whose "both sides exhausted" branch had no guard. */
    @Test
    public void testMerge_nextPastEndThrowsNoSuchElement() {
        final java.util.function.BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        final ObjIterator<Integer> iter = Iterators.merge(Arrays.asList(1).iterator(), Arrays.asList(2).iterator(), selector);
        assertEquals(Arrays.asList(1, 2), drainToList(iter));
        assertNotNull(assertThrows(NoSuchElementException.class, iter::next).getMessage());

        assertNotNull(assertThrows(NoSuchElementException.class,
                () -> Iterators.merge(Collections.<Integer> emptyIterator(), Collections.<Integer> emptyIterator(), selector).next()).getMessage());
    }

    /** J3: the no-comparator {@code mergeSorted} uses the library's null-friendly natural order (nulls first). */
    @Test
    public void testMergeSortedNaturalOrder_ordersNullsFirstInsteadOfThrowing() {
        assertEquals(Arrays.asList(null, 1, 2, 3), drainToList(Iterators.mergeSorted(Arrays.asList(null, 2).iterator(), Arrays.asList(1, 3).iterator())));

        // The Iterable overload agrees with the Iterator one.
        assertEquals(Arrays.asList(null, 1, 2, 3), drainToList(Iterators.mergeSorted(Arrays.asList(null, 2), Arrays.asList(1, 3))));
    }
}
