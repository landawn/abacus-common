package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class IteratorsCycleTest extends IteratorsTestSupport {
    @Test
    public void testCycleVarargs() {
        ObjIterator<String> iter = Iterators.cycle("a", "b", "c");
        List<String> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "a", "b", "c", "a"), result);

        iter = Iterators.cycle();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testCycleArray() {
        ObjIterator<String> iter = Iterators.cycle();
        assertFalse(iter.hasNext());

        iter = Iterators.cycle("a", "b", "c");
        for (int i = 0; i < 10; i++) {
            assertTrue(iter.hasNext());
            assertEquals("a", iter.next());
            assertEquals("b", iter.next());
            assertEquals("c", iter.next());
        }
    }

    @Test
    public void testCycleWithRounds_OneRound() {
        ObjIterator<Integer> iter = Iterators.cycle(Arrays.asList(1, 2, 3), 1);
        assertEquals(Arrays.asList(1, 2, 3), iter.toList());
    }

    @Test
    public void testCycleIterable() {
        ObjIterator<Integer> iter = Iterators.cycle(Arrays.asList(1, 2));
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(1, 2, 1, 2, 1), result);

        iter = Iterators.cycle(Collections.emptyList());
        assertFalse(iter.hasNext());

        iter = Iterators.cycle((Iterable<Integer>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testCycleVarargs_SingleElement() {
        ObjIterator<String> iter = Iterators.cycle("x");
        for (int i = 0; i < 5; i++) {
            assertTrue(iter.hasNext());
            assertEquals("x", iter.next());
        }
    }

    @Test
    public void testCycleIterable_SingleElement() {
        ObjIterator<Integer> iter = Iterators.cycle(Arrays.asList(7));
        for (int i = 0; i < 5; i++) {
            assertTrue(iter.hasNext());
            assertEquals(7, iter.next());
        }
    }

    @Test
    public void testCycleWithRounds_EmptyCollection() {
        ObjIterator<Integer> iter = Iterators.cycle(Collections.emptyList(), 3);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testCycleWithRounds() {
        ObjIterator<String> iter = Iterators.cycle(Arrays.asList("x", "y"), 3);
        assertEquals(Arrays.asList("x", "y", "x", "y", "x", "y"), iter.toList());

        iter = Iterators.cycle(Arrays.asList("a", "b"), 0);
        assertFalse(iter.hasNext());

        iter = Iterators.cycle(null, 3);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.cycle(Arrays.asList("a"), -1));
    }

    @Test
    public void testCycleIterableWithRounds() {
        ObjIterator<String> iter = Iterators.cycle(list("a", "b"), 2L);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(Iterators.cycle(list("a", "b"), 0L).hasNext());
        assertFalse(Iterators.cycle(list(), 2L).hasNext());
        assertThrows(IllegalArgumentException.class, () -> Iterators.cycle(list("a"), -1L));

        ObjIterator<String> iterOneRound = Iterators.cycle(list("a", "b"), 1L);
        assertEquals("a", iterOneRound.next());
        assertEquals("b", iterOneRound.next());
        assertFalse(iterOneRound.hasNext());
    }

    @Test
    public void testCycleToSize() {
        ObjIterator<Integer> iter = Iterators.cycleToSize(Arrays.asList(1, 2, 3), 7);
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1), iter.toList());
    }

    @Test
    public void testCycleToSize_SizeSmallerThanCollection() {
        ObjIterator<Integer> iter = Iterators.cycleToSize(Arrays.asList(1, 2, 3, 4, 5), 3);
        assertEquals(Arrays.asList(1, 2, 3), iter.toList());
    }

    @Test
    public void testCycleToSize_LargerThanCollection() {
        ObjIterator<Integer> result = Iterators.cycleToSize(Arrays.asList(1, 2, 3), 7);
        List<Integer> list = new ArrayList<>();
        result.forEachRemaining(list::add);
        assertEquals(7, list.size());
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1), list);
    }

    @Test
    public void testCycleToSize_SmallerThanCollection() {
        ObjIterator<Integer> result = Iterators.cycleToSize(Arrays.asList(1, 2, 3), 2);
        List<Integer> list = new ArrayList<>();
        result.forEachRemaining(list::add);
        assertEquals(2, list.size());
        assertEquals(Arrays.asList(1, 2), list);
    }

    @Test
    public void testCycleToSize_Zero() {
        ObjIterator<Integer> iter = Iterators.cycleToSize(Arrays.asList(1, 2), 0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testCycleToSize_ExactMultiple() {
        ObjIterator<Integer> iter = Iterators.cycleToSize(Arrays.asList(1, 2), 4);
        assertEquals(Arrays.asList(1, 2, 1, 2), iter.toList());
    }

    @Test
    public void testCycleToSize_SingleElement() {
        ObjIterator<Integer> iter = Iterators.cycleToSize(Arrays.asList(5), 3);
        assertEquals(Arrays.asList(5, 5, 5), iter.toList());
    }

    @Test
    public void testCycleToSize_ZeroSize() {
        ObjIterator<Integer> result = Iterators.cycleToSize(Arrays.asList(1, 2, 3), 0);
        assertFalse(result.hasNext());
    }

    @Test
    public void testCycleToSize_NullCollection() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.cycleToSize(null, 5));
    }

    @Test
    public void testCycleToSize_NegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.cycleToSize(Arrays.asList(1), -1));
    }

    @Test
    public void testCycleToSize_EmptyCollection() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.cycleToSize(Collections.emptyList(), 5));
    }

    @Test
    public void testCycle_EmptyArray_ReturnsEmpty() {
        ObjIterator<Integer> r = Iterators.cycle(new Integer[0]);
        assertFalse(r.hasNext());
    }

    @Test
    public void testCycle_EmptyIterable_ReturnsEmpty() {
        ObjIterator<Integer> r = Iterators.cycle(Collections.<Integer> emptyList());
        assertFalse(r.hasNext());
    }

    @Test
    public void testCycle_BasicArray() {
        ObjIterator<Integer> r = Iterators.cycle(1, 2);
        assertEquals(Arrays.asList(1, 2, 1, 2, 1), Iterators.limit(r, 5).toList());
    }

    @Test
    public void testCycle_RoundsZero_Empty() {
        ObjIterator<Integer> r = Iterators.cycle(Arrays.asList(1, 2, 3), 0);
        assertFalse(r.hasNext());
    }

    @Test
    public void testCycle_RoundsTwo() {
        ObjIterator<Integer> r = Iterators.cycle(Arrays.asList(1, 2), 2);
        assertEquals(Arrays.asList(1, 2, 1, 2), r.toList());
    }

    /** B4: a Collection is cycled in place, so later changes to it are visible. */
    @Test
    public void testCycleIterable_collectionIsALiveViewNotASnapshot() {
        final List<String> src = new ArrayList<>(Arrays.asList("a", "b"));
        final ObjIterator<String> iter = Iterators.cycle(src);

        assertEquals(Arrays.asList("a", "b", "a", "b"), takeFrom(iter, 4));

        // set() is not a structural modification, so an ArrayList iterator keeps working and the new value shows up
        src.set(0, "MUT");
        assertEquals(Arrays.asList("MUT", "b"), takeFrom(iter, 2));

        // a weakly-consistent collection picks structural changes up on the next round
        final java.util.Queue<String> weak = new java.util.concurrent.ConcurrentLinkedQueue<>(Arrays.asList("a", "b"));
        final ObjIterator<String> weakIter = Iterators.cycle(weak);
        assertEquals(Arrays.asList("a", "b"), takeFrom(weakIter, 2));
        weak.add("c");
        assertEquals(Arrays.asList("a", "b", "c"), takeFrom(weakIter, 3));

        // a fail-fast collection reports the structural modification, per its own iterator contract
        final List<String> failFast = new ArrayList<>(Arrays.asList("a", "b"));
        final ObjIterator<String> failFastIter = Iterators.cycle(failFast);
        assertEquals(Arrays.asList("a", "b"), takeFrom(failFastIter, 2));
        failFast.add("c");
        assertThrows(java.util.ConcurrentModificationException.class, failFastIter::next);
    }

    /** B4: a Collection cycled for a fixed number of rounds is a live view too, and still stops after {@code rounds}. */
    @Test
    public void testCycleIterableRounds_collectionIsALiveView() {
        final List<String> src = new ArrayList<>(Arrays.asList("a", "b"));
        final ObjIterator<String> iter = Iterators.cycle(src, 3);

        assertEquals(Arrays.asList("a", "b"), takeFrom(iter, 2));
        src.set(1, "MUT");
        assertEquals(Arrays.asList("a", "MUT", "a", "MUT"), drainToList(iter));
        assertFalse(iter.hasNext());
    }

    /** B4: a non-Collection Iterable cannot be assumed re-iterable, so it is still snapshotted after round one. */
    @Test
    public void testCycleIterable_nonCollectionIterableIsSnapshotted() {
        final AtomicInteger iteratorCalls = new AtomicInteger();
        final Iterable<Integer> oneShot = () -> {
            if (iteratorCalls.incrementAndGet() > 1) {
                throw new IllegalStateException("iterator() must only be called once");
            }

            return Arrays.asList(1, 2, 3).iterator();
        };

        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1), takeFrom(Iterators.cycle(oneShot), 7));
        assertEquals(1, iteratorCalls.get());

        iteratorCalls.set(0);
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), drainToList(Iterators.cycle(oneShot, 2)));
        assertEquals(1, iteratorCalls.get());
    }

    /** B4: emptying the source stops the cycle cleanly instead of spinning or throwing a message-less exception. */
    @Test
    public void testCycleIterable_sourceEmptiedAfterCreation() {
        // A weakly-consistent collection lets the emptied-source guard be observed directly; a fail-fast one
        // reports the structural modification first (see testCycleIterable_collectionIsALiveViewNotASnapshot).
        final java.util.Queue<String> src = new java.util.concurrent.ConcurrentLinkedQueue<>(Arrays.asList("a", "b"));
        final ObjIterator<String> infinite = Iterators.cycle(src);
        assertEquals(Arrays.asList("a", "b"), takeFrom(infinite, 2));
        src.clear();
        assertFalse(infinite.hasNext(), "an emptied source must end the cycle rather than promise an element next() cannot supply");
        assertNotNull(assertThrows(NoSuchElementException.class, infinite::next).getMessage());

        final java.util.Queue<String> src2 = new java.util.concurrent.ConcurrentLinkedQueue<>(Arrays.asList("a", "b"));
        final ObjIterator<String> rounds = Iterators.cycle(src2, Long.MAX_VALUE);
        assertEquals(Arrays.asList("a", "b"), takeFrom(rounds, 2));
        src2.clear();
        assertFalse(rounds.hasNext(), "an emptied source must end the iteration rather than spin through the remaining rounds");
    }

    /**
     * The Iterator contract: {@code while (it.hasNext()) it.next()} must terminate rather than throw. The infinite
     * {@code cycle(Collection)} used to hard-code {@code hasNext() == true}, so a drained source reported an element
     * that {@code next()} then refused to supply.
     */
    @Test
    public void testCycleIterable_drainLoopTerminatesWhenSourceIsEmptied() {
        final java.util.Queue<String> src = new java.util.concurrent.ConcurrentLinkedQueue<>(Arrays.asList("a", "b"));
        final ObjIterator<String> infinite = Iterators.cycle(src);

        final List<String> seen = new ArrayList<>();

        while (infinite.hasNext()) {
            seen.add(infinite.next());

            if (seen.size() == 2) {
                // The first round is fully consumed; emptying the source now leaves nothing for the next one.
                src.clear();
            }
        }

        assertEquals(Arrays.asList("a", "b"), seen);
        assertFalse(infinite.hasNext());
    }

    /**
     * {@code hasNext()} is a query: how often it is called must not change what the iterator yields. The
     * {@code round} counter used to be incremented by {@code hasNext()} itself, so every call made while the
     * source was momentarily empty silently consumed one of the requested rounds.
     */
    @Test
    public void testCycleIterableRounds_hasNextDoesNotConsumeRounds() {
        assertEquals(Arrays.asList("A"), drainCycleQueriedWhileEmpty(1));
        assertEquals(Arrays.asList("A"), drainCycleQueriedWhileEmpty(3));
        assertEquals(Arrays.asList("A"), drainCycleQueriedWhileEmpty(7));
    }

    /**
     * Drains {@code cycle(source, 5)} over a one-element source that is emptied after the first element and
     * refilled after {@code queriesWhileEmpty} calls to {@code hasNext()}. A {@code LinkedHashSet} is the witness:
     * {@code ArrayList.Itr.hasNext()} is {@code cursor != size}, so a stale iterator over a cleared
     * {@code ArrayList} still answers {@code true} and the round counter is never reached.
     */
    private static List<String> drainCycleQueriedWhileEmpty(final int queriesWhileEmpty) {
        final java.util.Collection<String> src = new java.util.LinkedHashSet<>(Arrays.asList("A"));
        final ObjIterator<String> iter = Iterators.cycle(src, 5);
        final List<String> out = new ArrayList<>();

        out.add(iter.next());

        src.clear();

        for (int i = 0; i < queriesWhileEmpty; i++) {
            assertFalse(iter.hasNext(), "an emptied source has no element to offer");
        }

        src.add("B");

        while (iter.hasNext() && out.size() < 20) {
            out.add(iter.next());
        }

        return out;
    }

    /**
     * The javadoc of {@code cycle(Iterable, long)} promises that an emptied source "ends the iteration early
     * instead of walking the remaining rounds". It used to merely answer {@code false} for that one call and
     * resume later, having consumed rounds in between.
     */
    @Test
    public void testCycleIterableRounds_emptiedSourceEndsTheIterationEvenWhenRefilled() {
        final java.util.Collection<String> src = new java.util.LinkedHashSet<>(Arrays.asList("A", "B"));
        final ObjIterator<String> iter = Iterators.cycle(src, 4);

        assertEquals("A", iter.next());
        assertEquals("B", iter.next());

        src.clear();

        assertFalse(iter.hasNext(), "an emptied source must end the iteration rather than walk the remaining rounds");

        src.add("C");

        assertFalse(iter.hasNext(), "the iteration has already ended; a later refill must not resurrect it");
        assertThrows(NoSuchElementException.class, iter::next);
    }

    /** The untouched path is unchanged: repeated queries neither skip nor duplicate a round. */
    @Test
    public void testCycleIterableRounds_repeatedHasNextDoesNotSkipARound() {
        final java.util.Collection<String> src = new java.util.LinkedHashSet<>(Arrays.asList("A", "B"));
        final ObjIterator<String> iter = Iterators.cycle(src, 3);
        final List<String> out = new ArrayList<>();

        while (true) {
            final boolean available = iter.hasNext();
            assertEquals(available, iter.hasNext());
            assertEquals(available, iter.hasNext());

            if (!available) {
                break;
            }

            out.add(iter.next());

            // A latch/round regression would make hasNext() answer true forever; cap the loop so that shows up as
            // an assertion failure rather than as a hung (and eventually OOM-ing) test.
            assertTrue(out.size() <= 6, "cycle(2-element source, 3) must not yield more than 6 elements");
        }

        assertEquals(Arrays.asList("A", "B", "A", "B", "A", "B"), out);
        assertThrows(NoSuchElementException.class, iter::next);
    }
}
