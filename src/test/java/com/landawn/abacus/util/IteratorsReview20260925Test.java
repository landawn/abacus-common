package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Tests for the 2026-09-25 verification findings on {@link Iterators} (U24-04, U24-05; both about ledger C-322).
 */
public class IteratorsReview20260925Test extends TestBase {

    private static <T> List<T> drain(final Iterator<? extends T> iter) {
        final List<T> out = new ArrayList<>();

        while (iter.hasNext() && out.size() < 1000) {
            out.add(iter.next());
        }

        return out;
    }

    // ------------------------------------------------------------------ U24-05: c.iterator() is obtained on the first hasNext()

    @Test
    public void testU24_05_failFastSourceGrownBetweenFirstHasNextAndNextThrowsCme() {
        final List<String> source = new ArrayList<>(Arrays.asList("a", "b"));
        final ObjIterator<String> iter = Iterators.repeatElementsToSize(source, 4);

        assertTrue(iter.hasNext()); // obtains source.iterator()
        source.add("c");

        // r9620 obtained the iterator only here and returned "a"; the source's fail-fast iterator now governs
        assertThrows(ConcurrentModificationException.class, iter::next);
    }

    @Test
    public void testU24_05_failFastSourceEmptiedAfterFirstHasNextEndsAndRefillIsCme() {
        final List<String> source = new ArrayList<>(Arrays.asList("a", "b"));
        final ObjIterator<String> iter = Iterators.repeatElementsToSize(source, 4);

        assertTrue(iter.hasNext());
        source.clear();

        // ArrayList's iterator answers hasNext() from cursor != size without a modCount check: the run simply ends
        assertFalse(iter.hasNext());
        assertEquals(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX, assertThrows(NoSuchElementException.class, iter::next).getMessage());

        // refilled: hasNext() is still answered by the iterator obtained at the first call, and next() is where fail-fast strikes
        source.addAll(Arrays.asList("x", "y"));
        assertTrue(iter.hasNext());
        assertThrows(ConcurrentModificationException.class, iter::next);
    }

    @Test
    public void testU24_05_snapshotSourceEmptiedAfterFirstHasNextIsNotSeen() {
        // a CopyOnWriteArrayList iterator is a snapshot taken when c.iterator() is called - now the first hasNext()
        final List<String> source = new CopyOnWriteArrayList<>(Arrays.asList("a", "b"));
        final ObjIterator<String> iter = Iterators.repeatElementsToSize(source, 4);

        assertTrue(iter.hasNext());
        source.clear();

        // r9620 took the (by then empty) snapshot in next() and could produce nothing; the snapshot now predates the clear
        assertEquals(Arrays.asList("a", "a", "b", "b"), drain(iter));
        assertFalse(iter.hasNext());
        assertEquals(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX, assertThrows(NoSuchElementException.class, iter::next).getMessage());
    }

    @Test
    public void testU24_05_sourceEmptiedBeforeFirstHasNextStaysEndedAfterRefill() {
        final Set<String> source = new LinkedHashSet<>(Arrays.asList("a", "b"));
        final ObjIterator<String> iter = Iterators.repeatElementsToSize(source, 4);

        source.clear();
        assertFalse(iter.hasNext()); // the iterator is obtained here, on an empty source

        source.addAll(Arrays.asList("a", "b"));
        assertFalse(iter.hasNext(), "the iterator obtained on the empty source does not see the refill");
        assertEquals(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX, assertThrows(NoSuchElementException.class, iter::next).getMessage());
    }

    // ------------------------------------------------------------------ U24-04: cycleToSize keeps its documented contract (DEFERRED)

    @Test
    public void testU24_04_cycleToSizeStillThrowsOnEmptiedSourceWhileRepeatElementsToSizeEnds() {
        // The two *ToSize siblings deliberately differ on an emptied source (cycleToSize javadoc "Live view" paragraph,
        // IteratorsTest#testToSizeIterators_sourceEmptiedAfterCreation): cycleToSize can no longer produce the requested
        // size and says so from next(); repeatElementsToSize treats it like a shrunk source and ends (C-322). Pinned so
        // that the split is visible; aligning them is an owner decision (U24-04, deferred 2026-09-25).
        final List<String> source = new ArrayList<>(Arrays.asList("a", "b"));
        final ObjIterator<String> cycle = Iterators.cycleToSize(source, 4);
        final ObjIterator<String> repeat = Iterators.repeatElementsToSize(source, 4);
        source.clear();

        assertTrue(cycle.hasNext());
        assertEquals(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX, assertThrows(NoSuchElementException.class, cycle::next).getMessage());
        assertTrue(cycle.hasNext(), "cycleToSize keeps reporting true: the shortfall is signalled by next()");

        assertFalse(repeat.hasNext());
        assertEquals(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX, assertThrows(NoSuchElementException.class, repeat::next).getMessage());

        // a shrunk-but-not-emptied source is no shortfall for cycleToSize: it simply cycles the smaller source
        final List<String> shrunk = new ArrayList<>(Arrays.asList("a", "b", "c"));
        final ObjIterator<String> cycleShrunk = Iterators.cycleToSize(shrunk, 5);
        shrunk.remove("c");
        assertEquals(Arrays.asList("a", "b", "a", "b", "a"), drain(cycleShrunk));
        assertFalse(cycleShrunk.hasNext());
    }
}
