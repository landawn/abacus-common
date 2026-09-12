package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.stream.Stream;

public class IteratorsRepeatTest extends IteratorsTestSupport {
    @Test
    public void testRepeatElement() {
        ObjIterator<String> iter = Iterators.repeat("hello", 3);
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.repeat("hello", 0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatElementLong() {
        ObjIterator<String> iter = Iterators.repeat("hello", 3L);
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertFalse(iter.hasNext());
    }

    // ===================== repeat Additional Edge Cases =====================

    @Test
    public void testRepeatInt_One() {
        ObjIterator<String> iter = Iterators.repeat("x", 1);
        assertTrue(iter.hasNext());
        assertEquals("x", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatLong_One() {
        ObjIterator<String> iter = Iterators.repeat("x", 1L);
        assertTrue(iter.hasNext());
        assertEquals("x", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatInt_NullElement() {
        ObjIterator<String> iter = Iterators.repeat(null, 2);
        assertEquals(null, iter.next());
        assertEquals(null, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_repeat() throws Exception {
        Stream.of(Iterators.repeatElements(CommonUtil.toList(1, 2, 3), 3)).println();
        Stream.of(Iterators.cycle(CommonUtil.toList(1, 2, 3), 3)).println();
        Stream.of(Iterators.repeatElementsToSize(CommonUtil.toList(1, 2, 3), 7)).println();
        Stream.of(Iterators.cycleToSize(CommonUtil.toList(1, 2, 3), 5)).println();

        assertEquals(3, CommonUtil.repeatElementsToSize(CommonUtil.toList(1, 2, 3, 4, 5, 6), 3).size());
        assertEquals(8, CommonUtil.repeatElementsToSize(CommonUtil.toList(1, 2, 3, 4, 5, 6), 8).size());

        assertEquals(3, CommonUtil.cycleToSize(CommonUtil.toList(1, 2, 3, 4, 5, 6), 3).size());
        assertEquals(8, CommonUtil.cycleToSize(CommonUtil.toList(1, 2, 3, 4, 5, 6), 8).size());
    }

    @Test
    public void testRepeatInt() {
        ObjIterator<String> iter = Iterators.repeat("a", 3);
        assertEquals(Arrays.asList("a", "a", "a"), iter.toList());

        iter = Iterators.repeat("x", 0);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("a", -1));
    }

    @Test
    public void testRepeatLong() {
        ObjIterator<String> iter = Iterators.repeat("b", 4L);
        assertEquals(Arrays.asList("b", "b", "b", "b"), iter.toList());

        iter = Iterators.repeat("y", 0L);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("a", -1L));
    }

    @Test
    public void testRepeatCollection() {
        ObjIterator<Integer> iter = Iterators.cycle(Arrays.asList(1, 2), 3);
        assertEquals(Arrays.asList(1, 2, 1, 2, 1, 2), iter.toList());

        iter = Iterators.cycle(Arrays.asList(1, 2, 3), 0);
        assertFalse(iter.hasNext());

        iter = Iterators.cycle(null, 2);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.cycle(Arrays.asList(1), -1));
    }

    @Test
    public void testRepeatCollectionToSize() {
        ObjIterator<Integer> iter = Iterators.cycleToSize(Arrays.asList(1, 2), 5);
        assertEquals(Arrays.asList(1, 2, 1, 2, 1), iter.toList());

        iter = Iterators.cycleToSize(Arrays.asList(1, 2, 3), 8);
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1, 2), iter.toList());

        iter = Iterators.cycleToSize(Arrays.asList(1, 2), 0);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.cycleToSize(null, 5));

        assertThrows(IllegalArgumentException.class, () -> Iterators.cycleToSize(Arrays.asList(1), -1));
    }

    @Test
    public void testRepeatWithInt() {
        {
            ObjIterator<String> iter = Iterators.repeat("test", 0);
            assertFalse(iter.hasNext());
        }

        {
            ObjIterator<String> iter = Iterators.repeat("hello", 3);
            assertTrue(iter.hasNext());
            assertEquals("hello", iter.next());
            assertEquals("hello", iter.next());
            assertEquals("hello", iter.next());
            assertFalse(iter.hasNext());

            assertThrows(NoSuchElementException.class, () -> iter.next());

            assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("test", -1));
        }
    }

    @Test
    public void testRepeatWithLong() {
        ObjIterator<String> iter = Iterators.repeat("test", 0L);
        assertFalse(iter.hasNext());

        iter = Iterators.repeat("hello", 3L);
        assertTrue(iter.hasNext());
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.repeat("x", 1000L);
        int count = 0;
        while (iter.hasNext()) {
            assertEquals("x", iter.next());
            count++;
        }
        assertEquals(1000, count);

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("test", -1L));
    }

    @Test
    public void testRepeatElementNegative() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("hello", -1));
    }

    // ===================== repeatElements Dedicated Tests =====================

    @Test
    public void testRepeatElements_Dedicated() {
        ObjIterator<Integer> iter = Iterators.repeatElements(Arrays.asList(1, 2), 3);
        assertEquals(Arrays.asList(1, 1, 1, 2, 2, 2), iter.toList());
    }

    @Test
    public void testRepeatElements_OneRepeat() {
        ObjIterator<Integer> iter = Iterators.repeatElements(Arrays.asList(1, 2, 3), 1);
        assertEquals(Arrays.asList(1, 2, 3), iter.toList());
    }

    @Test
    public void testRepeatElements_Zero() {
        ObjIterator<Integer> iter = Iterators.repeatElements(Arrays.asList(1, 2), 0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatElements_NullIterable() {
        ObjIterator<Integer> iter = Iterators.repeatElements(null, 2);
        assertFalse(iter.hasNext());
    }

    // ===================== repeatElements Additional Edge Cases =====================

    @Test
    public void testRepeatElements_SingleElement() {
        ObjIterator<Integer> iter = Iterators.repeatElements(Arrays.asList(5), 3);
        assertEquals(Arrays.asList(5, 5, 5), iter.toList());
    }

    @Test
    public void testRepeatElements_EmptyCollection() {
        ObjIterator<Integer> iter = Iterators.repeatElements(Collections.emptyList(), 5);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatElements() {
        ObjIterator<Integer> iter = Iterators.repeatElements(Arrays.asList(1, 2, 3), 2);
        assertEquals(Arrays.asList(1, 1, 2, 2, 3, 3), iter.toList());

        iter = Iterators.repeatElements(Arrays.asList(1, 2), 0);
        assertFalse(iter.hasNext());

        iter = Iterators.repeatElements(null, 2);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElements(Arrays.asList(1), -1));
    }

    // ===================== repeatElementsToSize Dedicated Tests =====================

    @Test
    public void testRepeatElementsToSize_Dedicated() {
        ObjIterator<Integer> iter = Iterators.repeatElementsToSize(Arrays.asList(1, 2, 3), 7);
        List<Integer> result = iter.toList();
        assertEquals(7, result.size());
    }

    @Test
    public void testRepeatElementsToSize_SmallerThanCollection() {
        ObjIterator<Integer> iter = Iterators.repeatElementsToSize(Arrays.asList(1, 2, 3), 2);
        List<Integer> result = iter.toList();
        assertEquals(2, result.size());
    }

    // ===================== repeatElementsToSize Additional Edge Cases =====================

    @Test
    public void testRepeatElementsToSize_ExactSize() {
        ObjIterator<Integer> iter = Iterators.repeatElementsToSize(Arrays.asList(1, 2, 3), 3);
        List<Integer> result = iter.toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testRepeatElementsToSize_LargerThanCollection() {
        ObjIterator<Integer> result = Iterators.repeatElementsToSize(Arrays.asList(1, 2, 3), 7);
        List<Integer> list = new ArrayList<>();
        result.forEachRemaining(list::add);
        assertEquals(7, list.size());
    }

    @Test
    public void testRepeatElementsToSize_TruncatesToSize() {
        ObjIterator<Integer> result = Iterators.repeatElementsToSize(Arrays.asList(1, 2, 3), 2);
        List<Integer> list = new ArrayList<>();
        result.forEachRemaining(list::add);
        assertEquals(2, list.size());
    }

    @Test
    public void testRepeatElementsToSize_ZeroSize() {
        ObjIterator<Integer> result = Iterators.repeatElementsToSize(Arrays.asList(1, 2, 3), 0);
        assertFalse(result.hasNext());
    }

    @Test
    public void testRepeatElementsToSize() {
        ObjIterator<Integer> iter = Iterators.repeatElementsToSize(Arrays.asList(1, 2), 5);
        assertEquals(Arrays.asList(1, 1, 1, 2, 2), iter.toList());

        iter = Iterators.repeatElementsToSize(Arrays.asList(1, 2, 3), 7);
        assertEquals(Arrays.asList(1, 1, 1, 2, 2, 3, 3), iter.toList());

        iter = Iterators.repeatElementsToSize(Arrays.asList(1, 2), 0);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(null, 5));

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(Arrays.asList(1), -1));
    }

    @Test
    public void testRepeatElementsToSize_EmptyCollection() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(Collections.emptyList(), 5));
    }

    @Test
    public void testRepeatElementsToSize_NullThrows() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(null, 5));
    }

    // ===================== repeat / repeatElements edge =====================

    @Test
    public void testRepeat_ZeroTimes() {
        ObjIterator<String> r = Iterators.repeat("x", 0);
        assertFalse(r.hasNext());
    }

    @Test
    public void testRepeat_Negative_Throws() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("x", -1));
    }

    @Test
    public void testRepeatElementsToSize_EmptyCollection_ZeroSizeOk() {
        ObjIterator<String> r = Iterators.repeatElementsToSize(Collections.<String> emptyList(), 0);
        assertFalse(r.hasNext());
    }

    @Test
    public void testRepeatElementsToSize_EmptyCollection_NonZeroThrows() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(Collections.<String> emptyList(), 5));
    }

    /**
     * The requested total must hold even if the source collection grows between the factory call and the first
     * {@code next()}.
     *
     * <p>The per-element repeat counts are fixed from {@code c.size()} at construction while {@code c.iterator()}
     * is only taken lazily, so a grown source used to yield more than {@code size} elements - the one number the
     * method guarantees.</p>
     */
    @Test
    public void testRepeatElementsToSize_SourceGrowsBeforeFirstNext() {
        final List<String> source = new ArrayList<>(Arrays.asList("a", "b"));
        final ObjIterator<String> iter = Iterators.repeatElementsToSize(source, 4);

        source.add("c");
        source.add("d");
        source.add("e");

        final List<String> out = new ArrayList<>();

        while (iter.hasNext() && out.size() < 50) {
            out.add(iter.next());
        }

        assertEquals(4, out.size());
        assertEquals(Arrays.asList("a", "a", "b", "b"), out);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    /**
     * The inherited terminal helpers are bounded by the same guard, because they all drive
     * {@code hasNext()}/{@code next()}.
     */
    @Test
    public void testRepeatElementsToSize_SourceGrowsBeforeFirstNext_TerminalOps() {
        assertEquals(4, grownAfterCreation(4).count());
        assertEquals(4, grownAfterCreation(4).toArray().length);
        assertEquals(Arrays.asList("a", "a", "b", "b"), grownAfterCreation(4).toList());
    }

    /** A {@code repeatElementsToSize} iterator whose source gained three elements before it was first read. */
    private static ObjIterator<String> grownAfterCreation(final long size) {
        final List<String> source = new ArrayList<>(Arrays.asList("a", "b"));
        final ObjIterator<String> iter = Iterators.repeatElementsToSize(source, size);

        source.add("c");
        source.add("d");
        source.add("e");

        return iter;
    }

    /** An unmutated source is unaffected by the total-count guard. */
    @Test
    public void testRepeatElementsToSize_UnmutatedSourceIsUnchanged() {
        for (final int size : new int[] { 0, 1, 4, 5, 7, 10 }) {
            final ObjIterator<String> iter = Iterators.repeatElementsToSize(Arrays.asList("a", "b", "c"), size);
            final List<String> out = new ArrayList<>();

            while (iter.hasNext()) {
                out.add(iter.next());
            }

            assertEquals(size, out.size());
        }

        final ObjIterator<String> iter = Iterators.repeatElementsToSize(Arrays.asList("a", "b", "c"), 7);
        final List<String> out = new ArrayList<>();

        while (iter.hasNext()) {
            out.add(iter.next());
        }

        assertEquals(Arrays.asList("a", "a", "a", "b", "b", "c", "c"), out);
    }

    /**
     * A source that shrinks behaves exactly as it did before the total-count guard was added.
     *
     * <p>It returns short and silently: {@code hasNext()} consults {@code iter.hasNext()} before
     * {@code nextElement()} can raise, so requesting 8 from a source that shrank from 4 elements to 2 yields 4.
     * This test exists to pin that the guard - which only ever lowers the ceiling - did not change this path.
     * (The short return is a pre-existing characteristic of the shrink case, not something introduced here.)</p>
     */
    @Test
    public void testRepeatElementsToSize_SourceShrinksBeforeFirstNext() {
        final List<String> source = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        final ObjIterator<String> iter = Iterators.repeatElementsToSize(source, 8);

        source.remove("d");
        source.remove("c");

        final List<String> out = new ArrayList<>();

        while (iter.hasNext() && out.size() < 50) {
            out.add(iter.next());
        }

        assertEquals(Arrays.asList("a", "a", "b", "b"), out);
        assertFalse(iter.hasNext());
    }

    /**
     * Pins the corrected "Live view" paragraph: if {@code c} has been emptied before the first {@code next()},
     * {@code hasNext()} still reports {@code true} - the repeat counts say an element is due - and the first
     * {@code next()} throws {@link NoSuchElementException}.
     */
    @Test
    public void testRepeatElementsToSize_SourceEmptiedBeforeFirstNext() {
        final List<String> source = new ArrayList<>(Arrays.asList("a", "b"));
        final ObjIterator<String> iter = Iterators.repeatElementsToSize(source, 4);

        source.clear();

        assertTrue(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    /**
     * Pins the other half of the corrected "Live view" paragraph: a structural modification made <i>after</i> the
     * first {@code next()} is governed by the source collection's own iterator contract, so a fail-fast source
     * raises {@link java.util.ConcurrentModificationException}. A modification made <i>before</i> that first
     * {@code next()} raises nothing, because the iterator is created after it - see
     * {@link #testRepeatElementsToSize_SourceGrowsBeforeFirstNext()}.
     */
    @Test
    public void testRepeatElementsToSize_SourceModifiedAfterFirstNext() {
        final List<String> source = new ArrayList<>(Arrays.asList("a", "b"));
        final ObjIterator<String> iter = Iterators.repeatElementsToSize(source, 4);

        assertEquals("a", iter.next());

        source.add("c");

        // "a" is due once more and repeats without touching the source iterator; the fail-fast check fires when
        // the next distinct element is pulled.
        assertEquals("a", iter.next());
        assertThrows(java.util.ConcurrentModificationException.class, iter::next);
    }
}
