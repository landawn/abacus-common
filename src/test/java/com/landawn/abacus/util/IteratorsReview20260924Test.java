package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Tests for the Iterators findings of the 2026-09-24 review (ledger IDs C-321..C-332).
 */
public class IteratorsReview20260924Test extends TestBase {

    private static <T> List<T> drain(final Iterator<? extends T> iter) {
        final List<T> out = new ArrayList<>();

        while (iter.hasNext() && out.size() < 1000) {
            out.add(iter.next());
        }

        return out;
    }

    private static <T> List<T> take(final Iterator<? extends T> iter, final int n) {
        final List<T> out = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            out.add(iter.next());
        }

        return out;
    }

    /** A non-Collection Iterable that can be iterated only once, like a source backed by I/O. */
    private static <T> Iterable<T> onceIterable(final List<T> elements) {
        final AtomicBoolean used = new AtomicBoolean(false);

        return () -> {
            if (used.getAndSet(true)) {
                throw new IllegalStateException("iterated twice");
            }

            return elements.iterator();
        };
    }

    /** An iterator over [0, n) that counts how many times next() was called. */
    private static final class CountingIterator implements Iterator<Integer> {
        private final int n;
        private int cursor = 0;
        int nextCalls = 0;

        CountingIterator(final int n) {
            this.n = n;
        }

        @Override
        public boolean hasNext() {
            return cursor < n;
        }

        @Override
        public Integer next() {
            if (cursor >= n) {
                throw new NoSuchElementException();
            }

            nextCalls++;
            return cursor++;
        }
    }

    // ------------------------------------------------------------------ C-321

    @Test
    public void testC321_cycleCollectionStaysEndedAfterRefill_LinkedHashSet() {
        final Set<String> s = new LinkedHashSet<>(Arrays.asList("A", "B"));
        final ObjIterator<String> it = Iterators.cycle(s);

        assertEquals(Arrays.asList("A", "B"), take(it, 2));
        s.clear();
        assertFalse(it.hasNext());

        s.add("C");
        assertFalse(it.hasNext(), "an ended cycle must not come back to life when the source is refilled");
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void testC321_cycleCollectionStaysEndedAfterRefill_ConcurrentQueue() {
        final ConcurrentLinkedQueue<String> q = new ConcurrentLinkedQueue<>(Arrays.asList("A", "B"));
        final ObjIterator<String> it = Iterators.cycle(q);

        assertEquals(Arrays.asList("A", "B", "A"), take(it, 3));
        q.clear();
        // the queue's weakly consistent iterator may still hand out a pre-fetched element; drain it
        drain(it);
        assertFalse(it.hasNext());

        q.add("C");
        assertFalse(it.hasNext());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void testC321_cycleCollectionStillCyclesAndPicksUpNonEmptyChanges() {
        final List<String> src = new java.util.concurrent.CopyOnWriteArrayList<>(Arrays.asList("é", "😀"));
        final ObjIterator<String> it = Iterators.cycle(src);

        assertEquals(Arrays.asList("é", "😀", "é", "😀", "é"), take(it, 5));

        // a non-empty source keeps cycling, and a change made between rounds is picked up on the next round
        take(it, 1); // finish the round
        src.add("x");
        assertEquals(Arrays.asList("é", "😀", "x", "é"), take(it, 4));
    }

    @Test
    public void testC321_cycleNullAndEmpty() {
        assertFalse(Iterators.cycle((Iterable<String>) null).hasNext());
        assertFalse(Iterators.cycle(new ArrayList<String>()).hasNext());
        assertFalse(Iterators.cycle(Collections.<String> emptySet()).hasNext());
    }

    // ------------------------------------------------------------------ C-322

    @Test
    public void testC322_repeatElementsToSizeEmptiedSourceReportsNoNext() {
        final List<String> source = new ArrayList<>(Arrays.asList("a", "b"));
        final ObjIterator<String> iter = Iterators.repeatElementsToSize(source, 4);

        source.clear();

        assertFalse(iter.hasNext(), "an emptied source must report hasNext() == false, like a shrunk one");
        assertThrows(NoSuchElementException.class, iter::next);

        // boundary: size 1, fewer than the source size (n == 0)
        final List<String> source2 = new ArrayList<>(Arrays.asList("x", "y", "z"));
        final ObjIterator<String> iter2 = Iterators.repeatElementsToSize(source2, 1);
        source2.clear();
        assertFalse(iter2.hasNext());
    }

    @Test
    public void testC322_repeatElementsToSizeWhileLoopNeverThrowsOnEmptiedSource() {
        final List<String> source = new ArrayList<>(Arrays.asList("a", "b", "c"));
        final ObjIterator<String> iter = Iterators.repeatElementsToSize(source, 7);
        source.clear();

        assertEquals(Collections.emptyList(), drain(iter));
    }

    @Test
    public void testC322_repeatElementsToSizeEmptiedAfterHasNextFollowsIteratorContract() {
        // hasNext() now obtains the source iterator, so a later change is governed by the source's own iterator.
        final List<String> source = new ArrayList<>(Arrays.asList("a", "b"));
        final ObjIterator<String> iter = Iterators.repeatElementsToSize(source, 4);

        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext()); // repeated queries are stable
        assertEquals("a", iter.next());
        assertEquals(Arrays.asList("a", "b", "b"), drain(iter));

        // U24-05 (2026-09-25): the consequence this test is named for - a fail-fast source modified between the first
        // hasNext() (which now obtains c.iterator()) and next() raises ConcurrentModificationException; at r9620 the
        // iterator was only obtained in next(), so the grown source was silently honoured and "a" was returned.
        final List<String> grown = new ArrayList<>(Arrays.asList("a", "b"));
        final ObjIterator<String> grownIter = Iterators.repeatElementsToSize(grown, 4);
        assertTrue(grownIter.hasNext());
        grown.add("c");
        assertThrows(ConcurrentModificationException.class, grownIter::next);
    }

    @Test
    public void testC322_repeatElementsToSizeShrunkSourceEndsShortWithoutThrowing() {
        final List<String> source = new ArrayList<>(Arrays.asList("A", "B", "C"));
        final ObjIterator<String> iter = Iterators.repeatElementsToSize(source, 7);
        source.remove("C");

        assertEquals(Arrays.asList("A", "A", "A", "B", "B"), drain(iter));
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    public void testC322_repeatElementsToSizeGrownSourceStillTruncatedToSize() {
        final List<String> source = new ArrayList<>(Arrays.asList("a", "b"));
        final ObjIterator<String> iter = Iterators.repeatElementsToSize(source, 4);
        source.add("c");

        assertEquals(Arrays.asList("a", "a", "b", "b"), drain(iter));
    }

    @Test
    public void testC322_repeatElementsToSizeBoundariesAndUnicode() {
        assertEquals(Arrays.asList("α", "α", "α", "😀", "😀"),
                drain(Iterators.repeatElementsToSize(Arrays.asList("α", "😀"), 5)));
        // size smaller than the source: n == 0, only the first 'size' elements once each
        assertEquals(Arrays.asList("a", "b"), drain(Iterators.repeatElementsToSize(Arrays.asList("a", "b", "c"), 2)));
        assertEquals(Arrays.asList("a"), drain(Iterators.repeatElementsToSize(Arrays.asList("a", "b", "c"), 1)));
        assertEquals(Arrays.asList("a", "b", "c"), drain(Iterators.repeatElementsToSize(Arrays.asList("a", "b", "c"), 3)));
        assertEquals(Arrays.asList(null, null, "b", "b"), drain(Iterators.repeatElementsToSize(Arrays.asList(null, "b"), 4)));
        // size 0: empty, even for a null or empty source
        assertFalse(Iterators.repeatElementsToSize((Collection<String>) null, 0).hasNext());
        assertFalse(Iterators.repeatElementsToSize(new ArrayList<String>(), 0).hasNext());
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(new ArrayList<String>(), 1));
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize((Collection<String>) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(Arrays.asList("a"), -1));

        final ObjIterator<String> exhausted = Iterators.repeatElementsToSize(Arrays.asList("a"), 2);
        assertEquals(Arrays.asList("a", "a"), drain(exhausted));
        assertThrows(NoSuchElementException.class, exhausted::next);
    }

    // ------------------------------------------------------------------ C-323

    @Test
    public void testC323_cycleNonCollectionSnapshotStillCorrect() {
        final ObjIterator<String> it = Iterators.cycle(onceIterable(Arrays.asList("a", "😀", "c")));

        assertEquals(Arrays.asList("a", "😀", "c", "a", "😀", "c", "a"), take(it, 7));
        assertTrue(it.hasNext());

        final ObjIterator<String> single = Iterators.cycle(onceIterable(Arrays.asList("x")));
        assertEquals(Arrays.asList("x", "x", "x"), take(single, 3));

        assertFalse(Iterators.cycle(onceIterable(Collections.<String> emptyList())).hasNext());
    }

    @Test
    public void testC323_cycleRoundsNonCollectionSnapshotStillCorrect() {
        assertEquals(Arrays.asList("a", null, "c", "a", null, "c", "a", null, "c"), drain(Iterators.cycle(onceIterable(Arrays.asList("a", null, "c")), 3)));
        assertEquals(Arrays.asList("a", "b"), drain(Iterators.cycle(onceIterable(Arrays.asList("a", "b")), 1)));
        assertEquals(Arrays.asList("x", "x"), drain(Iterators.cycle(onceIterable(Arrays.asList("x")), 2)));
        assertFalse(Iterators.cycle(onceIterable(Arrays.asList("a")), 0).hasNext());
        assertFalse(Iterators.cycle(onceIterable(Collections.<String> emptyList()), 5).hasNext());

        final ObjIterator<String> it = Iterators.cycle(onceIterable(Arrays.asList("a", "b")), 2);
        assertEquals(4, drain(it).size());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    // ------------------------------------------------------------------ C-327

    @Test
    public void testC327_forEachCountZeroDoesNotConsumeOffset() throws Exception {
        final Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4).iterator();
        final AtomicBoolean done = new AtomicBoolean(false);

        Iterators.forEach(iter, 2, 0, x -> fail("nothing should be processed"), () -> done.set(true));

        assertTrue(done.get(), "onComplete still runs");
        assertEquals(Arrays.asList(1, 2, 3, 4), drain(iter));

        final Iterator<Integer> iter2 = Arrays.asList(1, 2, 3).iterator();
        Iterators.forEach(iter2, 5, 0, x -> fail("nothing should be processed"));
        assertEquals(Arrays.asList(1, 2, 3), drain(iter2));
    }

    @Test
    public void testC327_forEachCountZeroConsumptionIndependentOfProcessThreads() throws Exception {
        for (final int processThreads : new int[] { 0, 1, 2 }) {
            final CountingIterator iter = new CountingIterator(10);
            final AtomicInteger completions = new AtomicInteger();
            final Iterators.IterateOptions options = Iterators.IterateOptions.builder().offset(3).count(0).processThreads(processThreads).build();

            Iterators.forEach(iter, options, x -> fail("nothing should be processed"), completions::incrementAndGet);

            assertEquals(0, iter.nextCalls, "processThreads=" + processThreads);
            assertEquals(1, completions.get(), "processThreads=" + processThreads);
        }

        // the collection overload agrees
        final CountingIterator iter = new CountingIterator(10);
        Iterators.forEach(Arrays.<Iterator<Integer>> asList(iter), 3, 0, x -> fail("nothing should be processed"));
        assertEquals(0, iter.nextCalls);
    }

    @Test
    public void testC327_forEachNonZeroCountStillSlicesAndNullIterator() throws Exception {
        final List<Integer> out = new ArrayList<>();
        final CountingIterator iter = new CountingIterator(10);
        Iterators.forEach(iter, 3, 2, out::add);
        assertEquals(Arrays.asList(3, 4), out);
        assertEquals(5, iter.nextCalls);

        final AtomicBoolean done = new AtomicBoolean(false);
        Iterators.forEach((Iterator<Integer>) null, 0, 0, x -> fail("nothing"), () -> done.set(true));
        assertTrue(done.get());

        final List<Integer> all = new ArrayList<>();
        Iterators.forEach(Arrays.asList(1, 2).iterator(), 0, 1, all::add);
        assertEquals(Arrays.asList(1), all);
    }

    // ------------------------------------------------------------------ C-328

    /** A predicate that throws once, the first time it sees {@code poison}, and otherwise delegates. */
    private static Predicate<Integer> throwingOnceOn(final int poison, final Predicate<Integer> delegate) {
        final AtomicBoolean thrown = new AtomicBoolean(false);

        return x -> {
            if (x == poison && !thrown.getAndSet(true)) {
                throw new IllegalStateException("boom");
            }

            return delegate.test(x);
        };
    }

    @Test
    public void testC328_dropWhileDoesNotEmitUntestedElementAfterPredicateException() {
        final ObjIterator<Integer> it = Iterators.dropWhile(Arrays.asList(1, 2).iterator(), throwingOnceOn(2, x -> x < 5));

        assertThrows(IllegalStateException.class, it::hasNext);
        assertEquals(Collections.emptyList(), drain(it), "2 < 5, so 2 must be dropped, not emitted");
    }

    @Test
    public void testC328_skipUntilDoesNotEmitUntestedElementAfterPredicateException() {
        final ObjIterator<Integer> it = Iterators.skipUntil(Arrays.asList(1, 2).iterator(), throwingOnceOn(2, x -> x > 5));

        assertThrows(IllegalStateException.class, it::hasNext);
        assertEquals(Collections.emptyList(), drain(it), "2 > 5 is false, so 2 must be skipped, not emitted");
    }

    @Test
    public void testC328_iterableOverloadsAndRetryContinuesWithTheNextElement() {
        final ObjIterator<Integer> dropped = Iterators.dropWhile(Arrays.asList(1, 2, 3, 9, 1), throwingOnceOn(2, x -> x < 5));
        assertThrows(IllegalStateException.class, dropped::next);
        assertEquals(Arrays.asList(9, 1), drain(dropped));

        final ObjIterator<Integer> skipped = Iterators.skipUntil(Arrays.asList(1, 2, 3, 9, 1), throwingOnceOn(2, x -> x > 5));
        assertThrows(IllegalStateException.class, skipped::next);
        assertEquals(Arrays.asList(9, 1), drain(skipped));
    }

    @Test
    public void testC328_dropWhileSkipUntilNormalBehaviour() {
        assertEquals(Arrays.asList(4, 5, 2, 1), drain(Iterators.dropWhile(Arrays.asList(1, 2, 3, 4, 5, 2, 1).iterator(), n -> n < 4)));
        assertEquals(Arrays.asList(4, 5, 2, 1), drain(Iterators.skipUntil(Arrays.asList(1, 2, 3, 4, 5, 2, 1).iterator(), n -> n >= 4)));
        assertEquals(Collections.emptyList(), drain(Iterators.dropWhile(Arrays.asList(1, 2).iterator(), n -> true)));
        assertEquals(Collections.emptyList(), drain(Iterators.skipUntil(Arrays.asList(1, 2).iterator(), n -> false)));
        assertEquals(Arrays.asList(null, "b"), drain(Iterators.dropWhile(Arrays.asList("a", null, "b").iterator(), s -> "a".equals(s))));
        assertEquals(Arrays.asList(null, "b"), drain(Iterators.skipUntil(Arrays.asList("a", null, "b").iterator(), s -> s == null)));
        assertEquals(Arrays.asList("😀"), drain(Iterators.skipUntil(Arrays.asList("a", "😀").iterator(), s -> s.length() == 2)));
        assertFalse(Iterators.dropWhile((Iterator<String>) null, s -> true).hasNext());
        assertFalse(Iterators.skipUntil((Iterator<String>) null, s -> true).hasNext());
        assertFalse(Iterators.dropWhile(Collections.<String> emptyIterator(), s -> false).hasNext());
        assertThrows(IllegalArgumentException.class, () -> Iterators.dropWhile(Arrays.asList(1).iterator(), null));
        assertThrows(IllegalArgumentException.class, () -> Iterators.skipUntil(Arrays.asList(1).iterator(), null));

        final ObjIterator<Integer> exhausted = Iterators.dropWhile(Arrays.asList(1, 7).iterator(), n -> n < 5);
        assertEquals(Arrays.asList(7), drain(exhausted));
        assertThrows(NoSuchElementException.class, exhausted::next);
    }

    // ------------------------------------------------------------------ C-329

    @Test
    public void testC329_mergeSortedNaturalOrderBoundIsGenericComparable() throws Exception {
        for (final Class<?> sourceType : new Class<?>[] { Iterator.class, Iterable.class }) {
            final Method m = Iterators.class.getMethod("mergeSorted", sourceType, sourceType);
            final TypeVariable<Method> t = m.getTypeParameters()[0];
            final Type bound = t.getBounds()[0];

            assertInstanceOf(ParameterizedType.class, bound, "the bound must not be raw Comparable: " + m);

            final ParameterizedType pt = (ParameterizedType) bound;
            assertEquals(Comparable.class, pt.getRawType());

            final Type arg = pt.getActualTypeArguments()[0];
            assertInstanceOf(WildcardType.class, arg);
            assertEquals(t, ((WildcardType) arg).getLowerBounds()[0]);
        }

        // it still accepts ordinary Comparable element types
        assertEquals(Arrays.asList(1, 2, 3, 4), drain(Iterators.mergeSorted(Arrays.asList(1, 3).iterator(), Arrays.asList(2, 4).iterator())));
        assertEquals(Arrays.asList("a", "b", "c"), drain(Iterators.mergeSorted(Arrays.asList("a", "c"), Arrays.asList("b"))));
        assertEquals(Arrays.asList(null, 1, 2, 3), drain(Iterators.mergeSorted(Arrays.asList(null, 2), Arrays.asList(1, 3))));
    }

    // ------------------------------------------------------------------ C-331

    @Test
    public void testC331_mergeSortedNullIsEmptyAndTiesKeepSortedAFirst() {
        assertEquals(Arrays.asList(1, 2), drain(Iterators.mergeSorted((Iterator<Integer>) null, Arrays.asList(1, 2).iterator())));
        assertEquals(Arrays.asList(1, 2), drain(Iterators.mergeSorted(Arrays.asList(1, 2), (Iterable<Integer>) null)));
        assertFalse(Iterators.mergeSorted((Iterable<Integer>) null, (Iterable<Integer>) null, Comparator.naturalOrder()).hasNext());

        final String a1 = new String("k");
        final String b1 = new String("k");
        final List<String> merged = drain(Iterators.mergeSorted(Arrays.asList(a1).iterator(), Arrays.asList(b1).iterator(), Comparator.naturalOrder()));
        assertSame(a1, merged.get(0));
        assertSame(b1, merged.get(1));

        final List<String> mergedNatural = drain(Iterators.mergeSorted(Arrays.asList(a1), Arrays.asList(b1)));
        assertSame(a1, mergedNatural.get(0));
        assertSame(b1, mergedNatural.get(1));
    }
}
