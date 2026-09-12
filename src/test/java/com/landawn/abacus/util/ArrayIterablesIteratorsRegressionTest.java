package com.landawn.abacus.util;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the {@code Array} / {@code Iterables} / {@code Iterators} / {@code N.slice} issues
 * corrected in the 2026-09-01 review pass.
 *
 * <ul>
 *   <li><b>B1</b> - {@code Iterators.merge(Collection, ..)} and {@code Iterators.mergeIterables(Collection, ..)}
 *       decided how many sources they had from {@code size()}/{@code isEmpty()}. A collection whose {@code size()}
 *       disagrees with its iterator therefore lost sources <i>silently</i> (a reported size of 0 dropped every
 *       one) or ran the iterator off its end. Only the three-or-more path had been hardened,</li>
 *   <li><b>B3</b> - {@code Iterables.union(..).copyInto(set)} added both backing sets to the destination
 *       wholesale, so it could copy elements the view does not expose,</li>
 *   <li><b>D1</b> - {@code Iterables.difference(..).isEmpty()} answered through {@code set2.containsAll(set1)}
 *       rather than the per-element probes {@code size()}/{@code iterator()} use,</li>
 *   <li><b>D7</b> - {@code Array.concat2D}/{@code concat3D} took the result's runtime component type from
 *       {@code b} when {@code a} was merely <i>empty</i>, so the result type depended on {@code a}'s length,</li>
 *   <li><b>J1</b> - the {@code orderedPermutations} "size caveat" claimed {@code n!}; it is the count of
 *       <i>distinct</i> permutations,</li>
 *   <li><b>J2</b> - {@code Array.newInstance(Class, int...)} let an empty {@code dimensions} surface as a
 *       message-less {@code IllegalArgumentException},</li>
 *   <li><b>D4</b>/<b>D5</b> - behaviour pinned for the newly documented {@code char} step rule and for the
 *       {@code N.slice(Collection, ..)} equality asymmetry.</li>
 * </ul>
 */
public class ArrayIterablesIteratorsRegressionTest extends TestBase {

    private static final BiFunction<Integer, Integer, MergeResult> MIN_FIRST = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

    // ============================================================ helpers

    /** A {@code Collection} whose {@code size()} - and therefore the inherited {@code isEmpty()} - lies. */
    private static final class LyingSizeCollection<E> extends AbstractCollection<E> {
        private final List<E> actual;
        private final int reported;

        LyingSizeCollection(final List<E> actual, final int reported) {
            this.actual = actual;
            this.reported = reported;
        }

        @Override
        public Iterator<E> iterator() {
            return actual.iterator();
        }

        @Override
        public int size() {
            return reported;
        }
    }

    /** An iterator that records whether anything has been pulled from it. */
    private static final class CountingIterator<E> implements Iterator<E> {
        private final Iterator<E> delegate;
        int hasNextCalls;
        int nextCalls;

        CountingIterator(final Iterator<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            hasNextCalls++;
            return delegate.hasNext();
        }

        @Override
        public E next() {
            nextCalls++;
            return delegate.next();
        }
    }

    private static List<Integer> drain(final Iterator<Integer> iter) {
        final List<Integer> out = new ArrayList<>();

        while (iter.hasNext()) {
            out.add(iter.next());
        }

        return out;
    }

    /** {@code actual} single-element sources holding 0, 1, .. actual-1, so a min-first merge yields them in order. */
    private static List<Iterator<? extends Integer>> singletonSources(final int actual) {
        final List<Iterator<? extends Integer>> sources = new ArrayList<>();

        for (int i = 0; i < actual; i++) {
            sources.add(Arrays.asList(i).iterator());
        }

        return sources;
    }

    private static List<Iterable<? extends Integer>> singletonIterables(final int actual) {
        final List<Iterable<? extends Integer>> sources = new ArrayList<>();

        for (int i = 0; i < actual; i++) {
            sources.add(Arrays.asList(i));
        }

        return sources;
    }

    private static List<Integer> expectedRange(final int actual) {
        final List<Integer> expected = new ArrayList<>();

        for (int i = 0; i < actual; i++) {
            expected.add(i);
        }

        return expected;
    }

    // ============================================================ B1: merge / mergeIterables vs a lying size()

    /**
     * The full {actual, reported} matrix, covering the reported sizes 0, 1 and 2 that used to bypass the
     * iterator-driven scan entirely. Before the fix, reported 0 returned nothing at all, reported 1 or 2 kept only
     * that many sources, and an over-report threw {@code NoSuchElementException}.
     */
    @Test
    public void testMerge_sourceCountComesFromTheIteratorNotFromSize() {
        for (final int actual : new int[] { 0, 1, 2, 3, 4, 5, 8 }) {
            for (final int reported : new int[] { 0, 1, 2, 3, 4, 7, 9 }) {
                final List<Integer> expected = expectedRange(actual);

                Assertions.assertEquals(expected, drain(Iterators.merge(new LyingSizeCollection<>(singletonSources(actual), reported), MIN_FIRST)),
                        () -> "merge: actual=" + actual + " size()=" + reported);

                Assertions.assertEquals(expected, drain(Iterators.mergeIterables(new LyingSizeCollection<>(singletonIterables(actual), reported), MIN_FIRST)),
                        () -> "mergeIterables: actual=" + actual + " size()=" + reported);
            }
        }
    }

    /** The individual shapes that regressed, spelled out so a failure names the case directly. */
    @Test
    public void testMerge_theShapesThatUsedToLoseSources() {
        // size() 0 used to short-circuit through N.isEmpty(c) and drop every source.
        Assertions.assertEquals(Arrays.asList(0, 1, 2), drain(Iterators.merge(new LyingSizeCollection<>(singletonSources(3), 0), MIN_FIRST)));
        Assertions.assertEquals(Arrays.asList(0, 1, 2), drain(Iterators.mergeIterables(new LyingSizeCollection<>(singletonIterables(3), 0), MIN_FIRST)));

        // size() 1 used to keep only the first source.
        Assertions.assertEquals(Arrays.asList(0, 1), drain(Iterators.merge(new LyingSizeCollection<>(singletonSources(2), 1), MIN_FIRST)));
        Assertions.assertEquals(Arrays.asList(0, 1), drain(Iterators.mergeIterables(new LyingSizeCollection<>(singletonIterables(2), 1), MIN_FIRST)));

        // size() 2 used to keep only the first two.
        Assertions.assertEquals(Arrays.asList(0, 1, 2, 3), drain(Iterators.merge(new LyingSizeCollection<>(singletonSources(4), 2), MIN_FIRST)));
        Assertions.assertEquals(Arrays.asList(0, 1, 2, 3), drain(Iterators.mergeIterables(new LyingSizeCollection<>(singletonIterables(4), 2), MIN_FIRST)));

        // An over-reported size used to run the collection's iterator off its end.
        Assertions.assertEquals(Arrays.asList(0), drain(Iterators.merge(new LyingSizeCollection<>(singletonSources(1), 2), MIN_FIRST)));
        Assertions.assertEquals(Arrays.asList(), drain(Iterators.merge(new LyingSizeCollection<>(singletonSources(0), 2), MIN_FIRST)));
        Assertions.assertEquals(Arrays.asList(0), drain(Iterators.mergeIterables(new LyingSizeCollection<>(singletonIterables(1), 2), MIN_FIRST)));
        Assertions.assertEquals(Arrays.asList(), drain(Iterators.mergeIterables(new LyingSizeCollection<>(singletonIterables(0), 2), MIN_FIRST)));
    }

    /** Multi-element sources, so the fold itself - not just the source count - is exercised under a lying size(). */
    @Test
    public void testMerge_lyingSizeStillMergesInOrder() {
        for (final int reported : new int[] { 0, 1, 2, 3, 99 }) {
            final List<Iterator<? extends Integer>> sources = new ArrayList<>();
            sources.add(Arrays.asList(1, 4, 7).iterator());
            sources.add(Arrays.asList(2, 5, 8).iterator());
            sources.add(Arrays.asList(3, 6, 9).iterator());

            Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), drain(Iterators.merge(new LyingSizeCollection<>(sources, reported), MIN_FIRST)),
                    () -> "size()=" + reported);
        }
    }

    /** A wildly over-reported size() must not be used as a capacity hint. */
    @Test
    public void testMerge_bogusHugeSizeDoesNotDriveAnAllocation() {
        Assertions.assertEquals(Arrays.asList(0, 1, 2), drain(Iterators.merge(new LyingSizeCollection<>(singletonSources(3), Integer.MAX_VALUE), MIN_FIRST)));
        Assertions.assertEquals(Arrays.asList(0, 1, 2),
                drain(Iterators.mergeIterables(new LyingSizeCollection<>(singletonIterables(3), Integer.MAX_VALUE), MIN_FIRST)));
    }

    /** Null members stay "treated as empty" at every observed source count, including the 1- and 2-source shapes. */
    @Test
    public void testMerge_nullMembersAreTreatedAsEmptyAtEverySourceCount() {
        for (final int reported : new int[] { 0, 1, 2, 3 }) {
            final List<Iterator<? extends Integer>> withNulls = new ArrayList<>();
            withNulls.add(null);
            withNulls.add(Arrays.asList(1, 3).iterator());
            withNulls.add(null);
            withNulls.add(Arrays.asList(2, 4).iterator());

            Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), drain(Iterators.merge(new LyingSizeCollection<>(withNulls, reported), MIN_FIRST)),
                    () -> "merge size()=" + reported);

            final List<Iterable<? extends Integer>> iterablesWithNulls = new ArrayList<>();
            iterablesWithNulls.add(null);
            iterablesWithNulls.add(Arrays.asList(1, 3));
            iterablesWithNulls.add(null);
            iterablesWithNulls.add(Arrays.asList(2, 4));

            Assertions.assertEquals(Arrays.asList(1, 2, 3, 4),
                    drain(Iterators.mergeIterables(new LyingSizeCollection<>(iterablesWithNulls, reported), MIN_FIRST)),
                    () -> "mergeIterables size()=" + reported);
        }

        Assertions.assertEquals(Arrays.asList(), drain(Iterators.merge(Collections.singletonList(null), MIN_FIRST)));
        Assertions.assertEquals(Arrays.asList(), drain(Iterators.mergeIterables(Collections.singletonList(null), MIN_FIRST)));
    }

    /** The honest-size behaviour, and the null/empty collection, are unchanged. */
    @Test
    public void testMerge_honestSizesAreUnchanged() {
        Assertions.assertFalse(Iterators.merge((Collection<Iterator<Integer>>) null, MIN_FIRST).hasNext());
        Assertions.assertFalse(Iterators.merge(new ArrayList<Iterator<Integer>>(), MIN_FIRST).hasNext());
        Assertions.assertFalse(Iterators.mergeIterables(null, MIN_FIRST).hasNext());
        Assertions.assertFalse(Iterators.mergeIterables(new ArrayList<Iterable<Integer>>(), MIN_FIRST).hasNext());

        Assertions.assertEquals(Arrays.asList(1, 2), drain(Iterators.merge(Arrays.asList(Arrays.asList(1, 2).iterator()), MIN_FIRST)));
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4),
                drain(Iterators.merge(Arrays.asList(Arrays.asList(1, 3).iterator(), Arrays.asList(2, 4).iterator()), MIN_FIRST)));
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), drain(
                Iterators.merge(Arrays.asList(Arrays.asList(1, 4).iterator(), Arrays.asList(2, 5).iterator(), Arrays.asList(3, 6).iterator()), MIN_FIRST)));

        Assertions.assertEquals(Arrays.asList(1, 2), drain(Iterators.mergeIterables(Arrays.asList(Arrays.asList(1, 2)), MIN_FIRST)));
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), drain(Iterators.mergeIterables(Arrays.asList(Arrays.asList(1, 3), Arrays.asList(2, 4)), MIN_FIRST)));
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6),
                drain(Iterators.mergeIterables(Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5), Arrays.asList(3, 6)), MIN_FIRST)));
    }

    /**
     * Collecting the source references up front must not pull any element: only the source count is decided
     * eagerly, the elements are still produced on demand.
     */
    @Test
    public void testMerge_scanningTheCollectionDoesNotConsumeAnySource() {
        for (final int count : new int[] { 1, 2, 3, 5 }) {
            final List<CountingIterator<Integer>> counters = new ArrayList<>();
            final List<Iterator<? extends Integer>> sources = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                final CountingIterator<Integer> counting = new CountingIterator<>(Arrays.asList(i).iterator());
                counters.add(counting);
                sources.add(counting);
            }

            final ObjIterator<Integer> merged = Iterators.merge(sources, MIN_FIRST);

            for (final CountingIterator<Integer> counting : counters) {
                Assertions.assertEquals(0, counting.hasNextCalls, () -> "hasNext() before first read, count=" + count);
                Assertions.assertEquals(0, counting.nextCalls, () -> "next() before first read, count=" + count);
            }

            Assertions.assertEquals(expectedRange(count), drain(merged));
        }
    }

    /**
     * {@code mergeIterables} obtains each source's {@code iterator()} eagerly - the documented behaviour of every
     * {@code Iterable}-accepting method here - but still must not pull an element before the result is read.
     */
    @Test
    public void testMergeIterables_obtainsIteratorsEagerlyButPullsNoElement() {
        final int[] iteratorCalls = { 0 };
        final List<CountingIterator<Integer>> counters = new ArrayList<>();
        final List<Iterable<? extends Integer>> sources = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            final int value = i;
            sources.add(() -> {
                iteratorCalls[0]++;
                final CountingIterator<Integer> counting = new CountingIterator<>(Arrays.asList(value).iterator());
                counters.add(counting);
                return counting;
            });
        }

        final ObjIterator<Integer> merged = Iterators.mergeIterables(sources, MIN_FIRST);

        Assertions.assertEquals(4, iteratorCalls[0], "iterator() is obtained eagerly, once per source");

        for (final CountingIterator<Integer> counting : counters) {
            Assertions.assertEquals(0, counting.hasNextCalls);
            Assertions.assertEquals(0, counting.nextCalls);
        }

        Assertions.assertEquals(Arrays.asList(0, 1, 2, 3), drain(merged));
        Assertions.assertEquals(4, iteratorCalls[0], "..and never re-obtained");
    }

    @Test
    public void testMerge_nullSelectorStillRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.merge(Arrays.asList(Arrays.asList(1).iterator()), null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.merge((Collection<Iterator<Integer>>) null, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.mergeIterables(Arrays.asList(Arrays.asList(1)), null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.mergeIterables(null, null));
    }

    // ============================================================ B3: union(..).copyInto

    /**
     * Both backing sets use the same (equals-based) equivalence relation, so this stays inside {@code union}'s
     * defined contract. The view holds one element; the removed override used to put two into the destination.
     */
    @Test
    public void testUnionCopyInto_copiesExactlyWhatTheViewExposes() {
        final String s1 = new String("dup"); // NOSONAR - a distinct instance is the point
        final String s2 = new String("dup"); // NOSONAR

        Assertions.assertEquals(s1, s2);
        Assertions.assertNotSame(s1, s2);

        final Set<String> set1 = new HashSet<>(Collections.singletonList(s1));
        final Set<String> set2 = new HashSet<>(Collections.singletonList(s2));
        final Iterables.SetView<String> view = Iterables.union(set1, set2);

        Assertions.assertEquals(1, view.size());
        Assertions.assertEquals(1, new ArrayList<>(view).size());

        final Set<String> identityDest = Collections.newSetFromMap(new IdentityHashMap<>());
        Assertions.assertEquals(1, view.copyInto(identityDest).size());

        // ..and it agrees with draining the view by hand.
        final Set<String> byHand = Collections.newSetFromMap(new IdentityHashMap<>());
        byHand.addAll(view);
        Assertions.assertEquals(byHand.size(), identityDest.size());
    }

    @Test
    public void testUnionCopyInto_ordinaryUseIsUnchanged() {
        final Set<Integer> set1 = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        final Set<Integer> set2 = new LinkedHashSet<>(Arrays.asList(3, 4, 5));

        Assertions.assertEquals(new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4, 5)), Iterables.union(set1, set2).copyInto(new LinkedHashSet<Integer>()));
        // iteration order: all of set1, then set2's new elements
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), new ArrayList<>(Iterables.union(set1, set2).copyInto(new LinkedHashSet<Integer>())));

        // null backing sets are still accepted
        Assertions.assertEquals(new LinkedHashSet<>(Arrays.asList(1, 2, 3)), Iterables.union(set1, null).copyInto(new LinkedHashSet<Integer>()));
        Assertions.assertEquals(new LinkedHashSet<>(Arrays.asList(3, 4, 5)), Iterables.union(null, set2).copyInto(new LinkedHashSet<Integer>()));
        Assertions.assertEquals(new LinkedHashSet<>(), Iterables.union((Set<Integer>) null, (Set<Integer>) null).copyInto(new LinkedHashSet<Integer>()));

        // the destination is returned, and a null destination is still rejected
        final Set<Integer> dest = new LinkedHashSet<>();
        Assertions.assertSame(dest, Iterables.union(set1, set2).copyInto(dest));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iterables.union(set1, set2).copyInto(null));
    }

    /**
     * The other half of {@code copyInto}'s contract: a destination whose equivalence relation is <i>coarser</i>
     * than the view's keeps fewer elements than the view exposes - never more.
     */
    @Test
    public void testUnionCopyInto_aCoarserDestinationMayKeepFewerButNeverMore() {
        final Set<String> set1 = new LinkedHashSet<>(Arrays.asList("a", "B"));
        final Set<String> set2 = new LinkedHashSet<>(Arrays.asList("A", "c"));
        final Iterables.SetView<String> view = Iterables.union(set1, set2);

        // The view exposes 4 elements: "a", "B" from set1 and "A", "c" from set2 (neither is in set1 by equals).
        Assertions.assertEquals(Arrays.asList("a", "B", "A", "c"), new ArrayList<>(view));
        Assertions.assertEquals(4, view.size());

        // A case-insensitive destination collapses "a"/"A" - fewer, and never more than the view exposes.
        final Set<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Assertions.assertEquals(3, view.copyInto(caseInsensitive).size());
        Assertions.assertTrue(view.copyInto(new TreeSet<>(String.CASE_INSENSITIVE_ORDER)).size() <= view.size());

        // An exact-equality destination keeps all of them, in iteration order.
        Assertions.assertEquals(Arrays.asList("a", "B", "A", "c"), new ArrayList<>(view.copyInto(new LinkedHashSet<String>())));
    }

    /** copyInto must see later changes to the backing sets - the view stays live. */
    @Test
    public void testUnionCopyInto_readsThroughToTheBackingSets() {
        final Set<Integer> set1 = new LinkedHashSet<>(Arrays.asList(1));
        final Set<Integer> set2 = new LinkedHashSet<>();
        final Iterables.SetView<Integer> view = Iterables.union(set1, set2);

        set2.add(9);

        Assertions.assertEquals(new LinkedHashSet<>(Arrays.asList(1, 9)), view.copyInto(new LinkedHashSet<Integer>()));
    }

    // ============================================================ D1: difference(..).isEmpty()

    @Test
    public void testDifferenceIsEmpty_agreesWithSizeAndIterator() {
        final List<Set<Integer>> candidates = Arrays.asList(new LinkedHashSet<>(), new LinkedHashSet<>(Arrays.asList(1)),
                new LinkedHashSet<>(Arrays.asList(1, 2, 3)), new LinkedHashSet<>(Arrays.asList(3, 4)), new HashSet<>(Arrays.asList(1, 2, 3, 4, 5)));

        for (final Set<Integer> set1 : candidates) {
            for (final Set<Integer> set2 : candidates) {
                final Iterables.SetView<Integer> view = Iterables.difference(set1, set2);
                final boolean empty = view.isEmpty();

                Assertions.assertEquals(view.size() == 0, empty, () -> "size(): " + set1 + " \\ " + set2);
                Assertions.assertEquals(!view.iterator().hasNext(), empty, () -> "iterator(): " + set1 + " \\ " + set2);
            }
        }
    }

    /**
     * isEmpty() must probe set2 the same way size()/iterator() do, so a probe that throws throws from all three
     * alike - the guarantee the method's javadoc gives - and identically for all three set views.
     */
    @Test
    public void testDifferenceIsEmpty_probeFailurePropagatesFromAllThree() {
        // TreeSet.contains(null) throws NPE, so probing the null member of set1 must fail the same way everywhere.
        final Set<String> nullHostile = new TreeSet<>(Arrays.asList("zzz"));

        // (a) the null is probed first, so nothing can be settled before reaching it
        final Iterables.SetView<String> nullFirst = Iterables.difference(new LinkedHashSet<>(Arrays.asList(null, "a")), nullHostile);
        Assertions.assertThrows(NullPointerException.class, nullFirst::isEmpty);
        Assertions.assertThrows(NullPointerException.class, nullFirst::size);
        Assertions.assertThrows(NullPointerException.class, () -> drainToList(nullFirst.iterator()));

        // (b) the null comes second, and set2 covers the element before it, so isEmpty() cannot settle early either
        final Iterables.SetView<String> covering = Iterables.difference(new LinkedHashSet<>(Arrays.asList("a", null)), new TreeSet<>(Arrays.asList("a")));
        Assertions.assertThrows(NullPointerException.class, covering::isEmpty);
        Assertions.assertThrows(NullPointerException.class, covering::size);
        Assertions.assertThrows(NullPointerException.class, () -> drainToList(covering.iterator()));

        // ..and the same for the two sibling views, which use the identical probe loop
        final Iterables.SetView<String> intersection = Iterables.intersection(new LinkedHashSet<>(Arrays.asList(null, "a")), nullHostile);
        Assertions.assertThrows(NullPointerException.class, intersection::isEmpty);
        Assertions.assertThrows(NullPointerException.class, intersection::size);
        Assertions.assertThrows(NullPointerException.class, () -> drainToList(intersection.iterator()));

        final Iterables.SetView<String> symmetric = Iterables.symmetricDifference(new LinkedHashSet<>(Arrays.asList(null, "a")), nullHostile);
        Assertions.assertThrows(NullPointerException.class, symmetric::isEmpty);
        Assertions.assertThrows(NullPointerException.class, symmetric::size);
        Assertions.assertThrows(NullPointerException.class, () -> drainToList(symmetric.iterator()));
    }

    /**
     * A {@code Set} may override {@code containsAll} with something that is not an element-by-element
     * {@code contains} walk - {@code EnumSet} does exactly that - so answering {@code isEmpty()} through
     * {@code set2.containsAll(set1)} was not structurally the same question {@code size()}/{@code iterator()} ask.
     * With such a set the old form disagreed with its own view; the explicit probe loop cannot.
     */
    @Test
    public void testDifferenceIsEmpty_doesNotDelegateToAnOverriddenContainsAll() {
        final Set<String> set1 = new LinkedHashSet<>(Arrays.asList("a", "b"));
        final Set<String> lyingContainsAll = new LinkedHashSet<>(Arrays.asList("b")) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean containsAll(final Collection<?> c) {
                return true; // claims to contain everything, while contains(..) stays honest
            }
        };

        final Iterables.SetView<String> view = Iterables.difference(set1, lyingContainsAll);

        // contains(..) - the probe iterator()/size() use - says "a" is missing, so the difference is NOT empty.
        Assertions.assertEquals(Arrays.asList("a"), new ArrayList<>(view));
        Assertions.assertEquals(1, view.size());
        Assertions.assertFalse(view.isEmpty());
    }

    /**
     * All three answers short-circuit as soon as they are settled, so an element beyond that point is never
     * probed by any of them - {@code isEmpty()} included. This is not an inconsistency between them: it is the
     * same rule, reached at different points because they answer different questions.
     */
    @Test
    public void testDifferenceIsEmpty_shortCircuitsBeforeAnUnreachedProbe() {
        final Set<String> withTrailingNull = new LinkedHashSet<>(Arrays.asList("a", null));
        final Set<String> nullHostile = new TreeSet<>(Arrays.asList("zzz"));
        final Iterables.SetView<String> view = Iterables.difference(withTrailingNull, nullHostile);

        // "a" is absent from set2, so the difference is settled as non-empty before the null is ever probed.
        Assertions.assertFalse(view.isEmpty());
        // size() and iterator() must visit every element, so they do reach it.
        Assertions.assertThrows(NullPointerException.class, view::size);
        Assertions.assertThrows(NullPointerException.class, () -> drainToList(view.iterator()));
    }

    private static <T> List<T> drainToList(final Iterator<T> iter) {
        final List<T> out = new ArrayList<>();

        while (iter.hasNext()) {
            out.add(iter.next());
        }

        return out;
    }

    @Test
    public void testDifferenceIsEmpty_ordinaryResultsAreUnchanged() {
        final Set<String> set1 = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        final Set<String> set2 = new LinkedHashSet<>(Arrays.asList("b", "d"));

        Assertions.assertFalse(Iterables.difference(set1, set2).isEmpty());
        Assertions.assertEquals(Arrays.asList("a", "c"), new ArrayList<>(Iterables.difference(set1, set2)));
        Assertions.assertTrue(Iterables.difference(set1, set1).isEmpty());
        Assertions.assertTrue(Iterables.difference(new LinkedHashSet<String>(), set2).isEmpty());
        Assertions.assertTrue(Iterables.difference((Set<String>) null, set2).isEmpty());
        Assertions.assertFalse(Iterables.difference(set1, null).isEmpty());
        // a superset on the right empties the difference
        Assertions.assertTrue(Iterables.difference(set1, new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"))).isEmpty());
    }

    // ============================================================ D7: concat2D / concat3D component type

    @Test
    public void testConcat2D_componentTypeIsAlwaysAsWhateverItsLength() {
        final Number[][] nums = { { 2.5 }, { 3 } };

        // a is empty but non-null: it still dictates the result type, so a Number[] row cannot be stored.
        Assertions.assertThrows(ArrayStoreException.class, () -> Array.concat2D(new Integer[0][], nums));
        // ..exactly as it already did for a non-empty a.
        Assertions.assertThrows(ArrayStoreException.class, () -> Array.concat2D(new Integer[][] { { 1 } }, nums));

        // Homogeneous calls keep a's type for every length of a.
        Assertions.assertEquals(String[].class, Array.concat2D(new String[0][], new String[][] { { "x" } }).getClass().getComponentType());
        Assertions.assertEquals(String[].class, Array.concat2D(new String[][] { { "a" } }, new String[][] { { "x" } }).getClass().getComponentType());
        Assertions.assertEquals(String[].class, Array.concat2D(new String[0][], new String[0][]).getClass().getComponentType());
        Assertions.assertEquals(String[].class, Array.concat2D(new String[0][], (String[][]) null).getClass().getComponentType());

        // ..and fall back to b's only when a is null.
        Assertions.assertEquals(Number[].class, Array.concat2D(null, nums).getClass().getComponentType());
        Assertions.assertEquals(String[].class, Array.concat2D(null, new String[0][]).getClass().getComponentType());
    }

    @Test
    public void testConcat3D_componentTypeIsAlwaysAsWhateverItsLength() {
        final Number[][][] nums = { { { 2.5 } } };

        Assertions.assertThrows(ArrayStoreException.class, () -> Array.concat3D(new Integer[0][][], nums));
        Assertions.assertThrows(ArrayStoreException.class, () -> Array.concat3D(new Integer[][][] { { { 1 } } }, nums));

        Assertions.assertEquals(String[][].class, Array.concat3D(new String[0][][], new String[][][] { { { "x" } } }).getClass().getComponentType());
        Assertions.assertEquals(String[][].class,
                Array.concat3D(new String[][][] { { { "a" } } }, new String[][][] { { { "x" } } }).getClass().getComponentType());
        Assertions.assertEquals(String[][].class, Array.concat3D(new String[0][][], (String[][][]) null).getClass().getComponentType());
        Assertions.assertEquals(Number[][].class, Array.concat3D(null, nums).getClass().getComponentType());
    }

    /** Every array dimension is copied on every path; only the elements are shared. */
    @Test
    public void testConcat2D_copiesEveryDimensionOnEveryPath() {
        final String x = "x";
        final String[][] b = { { x }, null, { "y", "z" } };

        final String[][] fromEmptyA = Array.concat2D(new String[0][], b);
        Assertions.assertNotSame(b, fromEmptyA);
        Assertions.assertNotSame(b[0], fromEmptyA[0]);
        Assertions.assertSame(x, fromEmptyA[0][0]);
        Assertions.assertNull(fromEmptyA[1]);
        Assertions.assertArrayEquals(b[2], fromEmptyA[2]);
        Assertions.assertNotSame(b[2], fromEmptyA[2]);

        final String[][] fromEmptyB = Array.concat2D(b, new String[0][]);
        Assertions.assertNotSame(b, fromEmptyB);
        Assertions.assertNotSame(b[0], fromEmptyB[0]);
        Assertions.assertSame(x, fromEmptyB[0][0]);
        Assertions.assertNull(fromEmptyB[1]);

        final String[][] fromNullB = Array.concat2D(b, null);
        Assertions.assertNotSame(b[0], fromNullB[0]);
        Assertions.assertNull(fromNullB[1]);

        // mutating the result must not reach back into the input
        fromEmptyA[0][0] = "changed";
        Assertions.assertSame(x, b[0][0]);
    }

    @Test
    public void testConcat3D_copiesEveryDimensionOnEveryPath() {
        final String[][][] b = { { { "x" } }, null, { null, { "y" } } };

        final String[][][] fromEmptyA = Array.concat3D(new String[0][][], b);
        Assertions.assertNotSame(b, fromEmptyA);
        Assertions.assertNotSame(b[0], fromEmptyA[0]);
        Assertions.assertNotSame(b[0][0], fromEmptyA[0][0]);
        Assertions.assertNull(fromEmptyA[1]);
        Assertions.assertNull(fromEmptyA[2][0]);
        Assertions.assertArrayEquals(b[2][1], fromEmptyA[2][1]);

        final String[][][] fromEmptyB = Array.concat3D(b, new String[0][][]);
        Assertions.assertNotSame(b[0], fromEmptyB[0]);
        Assertions.assertNull(fromEmptyB[1]);
    }

    /** Contents are unchanged across the whole null/empty/non-empty matrix. */
    @Test
    public void testConcat2D_contentsUnchanged() {
        Assertions.assertNull(Array.concat2D((String[][]) null, null));
        Assertions.assertArrayEquals(new String[0][], Array.concat2D(new String[0][], null));
        Assertions.assertArrayEquals(new String[0][], Array.concat2D(null, new String[0][]));
        Assertions.assertArrayEquals(new String[][] { { "a", "b", "d" }, { "c", "e", "f" } },
                Array.concat2D(new String[][] { { "a", "b" }, { "c" } }, new String[][] { { "d" }, { "e", "f" } }));
        // different lengths: the longer side supplies the surplus rows
        Assertions.assertArrayEquals(new Integer[][] { { 1, 2, 4 }, { 3 } }, Array.concat2D(new Integer[][] { { 1, 2 }, { 3 } }, new Integer[][] { { 4 } }));
        Assertions.assertArrayEquals(new Integer[][] { { 1, 2 }, { 4 } }, Array.concat2D(new Integer[][] { { 1, 2 } }, new Integer[][] { {}, { 4 } }));
        // a null row on both sides at the same index stays null
        Assertions.assertArrayEquals(new String[][] { null }, Array.concat2D(new String[][] { null }, new String[][] { null }));
        // ..but is filled in from whichever side has one
        Assertions.assertArrayEquals(new String[][] { { "q" } }, Array.concat2D(new String[][] { null }, new String[][] { { "q" } }));
    }

    @Test
    public void testConcat3D_contentsUnchanged() {
        Assertions.assertNull(Array.concat3D((String[][][]) null, null));
        Assertions.assertArrayEquals(new String[0][][], Array.concat3D(new String[0][][], null));
        Assertions.assertArrayEquals(new String[][][] { { { "a", "b", "d" } }, { { "c", "e", "f" } } },
                Array.concat3D(new String[][][] { { { "a", "b" } }, { { "c" } } }, new String[][][] { { { "d" } }, { { "e", "f" } } }));
        Assertions.assertArrayEquals(new String[][][] { null }, Array.concat3D(new String[][][] { null }, new String[][][] { null }));
        Assertions.assertArrayEquals(new Integer[][][] { { { 1, 2, 4 } }, { { 3 } } },
                Array.concat3D(new Integer[][][] { { { 1, 2 } }, { { 3 } } }, new Integer[][][] { { { 4 } } }));
    }

    // ============================================================ J1: orderedPermutations size()

    @Test
    public void testOrderedPermutations_sizeIsTheDistinctCountNotFactorial() {
        final Collection<List<Integer>> withDuplicates = Iterables.orderedPermutations(Arrays.asList(1, 2, 2, 1), Comparator.naturalOrder());

        // 4!/(2!*2!) == 6, not 4! == 24
        Assertions.assertEquals(6, withDuplicates.size());
        Assertions.assertEquals(6, drainToList(withDuplicates.iterator()).size());
        Assertions.assertEquals(6, new ArrayList<>(withDuplicates).size());

        Assertions.assertEquals(3, Iterables.orderedPermutations(Arrays.asList(1, 1, 2)).size()); // 3!/2!
        Assertions.assertEquals(1, Iterables.orderedPermutations(new ArrayList<Integer>()).size());

        // Saturation is driven by the distinct count, not by n: 20 mutually equal elements report 1.
        final List<Integer> allEqual = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            allEqual.add(1);
        }

        Assertions.assertEquals(1, Iterables.orderedPermutations(allEqual).size());
        Assertions.assertEquals(1, Iterables.orderedPermutations(allEqual, Comparator.naturalOrder()).size());

        // ..while 13 distinct elements do saturate.
        final List<Integer> distinct13 = new ArrayList<>();

        for (int i = 0; i < 13; i++) {
            distinct13.add(i);
        }

        Assertions.assertEquals(Integer.MAX_VALUE, Iterables.orderedPermutations(distinct13).size());
    }

    /** permutations(..) really does report n!, so its own (unchanged) caveat stays correct. */
    @Test
    public void testPermutations_sizeIsFactorial() {
        Assertions.assertEquals(24, Iterables.permutations(Arrays.asList(1, 2, 2, 1)).size());
        Assertions.assertEquals(24, new ArrayList<>(Iterables.permutations(Arrays.asList(1, 2, 2, 1))).size());
        Assertions.assertEquals(6, Iterables.permutations(Arrays.asList(1, 2, 3)).size());
        Assertions.assertEquals(1, Iterables.permutations(new ArrayList<Integer>()).size());
    }

    // ============================================================ J2: Array.newInstance(Class, int...)

    @Test
    public void testNewInstanceWithDimensions_emptyDimensionsIsRejectedWithAMessage() {
        final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> Array.newInstance(int.class, new int[0]));
        Assertions.assertNotNull(e.getMessage());
        Assertions.assertTrue(e.getMessage().contains("dimensions"), e.getMessage());

        // a null componentType is still named, and a null dimensions array still raises NPE as documented
        final IllegalArgumentException nullType = Assertions.assertThrows(IllegalArgumentException.class, () -> Array.newInstance(null, 1));
        Assertions.assertTrue(nullType.getMessage().contains("componentType"), nullType.getMessage());
        Assertions.assertThrows(IllegalArgumentException.class, () -> Array.newInstance(int.class, (int[]) null));
    }

    /** The other two conditions the {@code @throws} tags now name explicitly. */
    @Test
    public void testNewInstance_voidComponentTypeAndTooManyDimensionsAreRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Array.newInstance(void.class, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Array.newInstance(void.class, 5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Array.newInstance(void.class, new int[] { 1 }));

        // 255 dimensions is the JVM limit; 256 is one too many.
        Assertions.assertNotNull(Array.newInstance(int.class, new int[255]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Array.newInstance(int.class, new int[256]));
    }

    @Test
    public void testNewInstanceWithDimensions_ordinaryUseIsUnchanged() {
        final int[][] twoD = Array.newInstance(int.class, 2, 3);
        Assertions.assertEquals(2, twoD.length);
        Assertions.assertEquals(3, twoD[0].length);

        final String[][][] threeD = Array.newInstance(String.class, 1, 2, 3);
        Assertions.assertEquals(1, threeD.length);
        Assertions.assertEquals(2, threeD[0].length);
        Assertions.assertEquals(3, threeD[0][1].length);

        // the single-dimension varargs call is still just an array of that length
        final int[] oneD = Array.newInstance(int.class, 4);
        Assertions.assertEquals(4, oneD.length);

        Assertions.assertThrows(NegativeArraySizeException.class, () -> Array.newInstance(int.class, 2, -1));
    }

    // ============================================================ C-017: the reflective accessors surface the JDK's exceptions

    /**
     * {@code Array}'s reflective element accessors add no checks of their own, so a bad index yields the JDK's
     * <i>message-less</i> {@code ArrayIndexOutOfBoundsException} rather than the helpful message plain array
     * access produces. Now documented on the class; pinned here so it stays a deliberate choice.
     */
    @Test
    public void testReflectiveAccessors_surfaceTheJdkExceptionsVerbatim() {
        final int[] one = { 1 };

        for (final org.junit.jupiter.api.function.Executable bad : new org.junit.jupiter.api.function.Executable[] { //
                () -> Array.get(one, 5), () -> Array.getInt(one, 5), () -> Array.getLong(one, 5), //
                () -> Array.getDouble(one, 5), () -> Array.set(one, 5, 1), () -> Array.setInt(one, 5, 1) }) {
            final ArrayIndexOutOfBoundsException e = Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, bad);
            Assertions.assertNull(e.getMessage(), "the reflective path carries no message - same as java.lang.reflect.Array");
        }

        // ..which is exactly what the JDK method it delegates to does
        Assertions.assertNull(Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> java.lang.reflect.Array.get(one, 5)).getMessage());
        // ..whereas plain indexing is helpful
        Assertions.assertNotNull(Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            final int ignored = one[5];
        }).getMessage());

        // a null array is likewise the JDK's bare NullPointerException
        Assertions.assertThrows(NullPointerException.class, () -> Array.get(null, 0));
        Assertions.assertThrows(NullPointerException.class, () -> Array.getInt(null, 0));
        Assertions.assertThrows(NullPointerException.class, () -> Array.set(null, 0, 1));

        // a non-array argument is an IllegalArgumentException, as documented
        Assertions.assertThrows(IllegalArgumentException.class, () -> Array.getLength("not an array"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Array.get("not an array", 0));
        // ..and so is a get* whose primitive widening does not apply
        Assertions.assertThrows(IllegalArgumentException.class, () -> Array.getInt(new Integer[] { 1 }, 0));
    }

    // ============================================================ D4: the char step rule that is now documented

    @Test
    public void testCharRange_stepMagnitudeBeyondTheCharDomain() {
        // A non-empty, direction-consistent range with distinct endpoints yields exactly one element..
        Assertions.assertArrayEquals(new char[] { 'a' }, Array.range('a', 'e', 70000));
        Assertions.assertArrayEquals(new char[] { 'a' }, Array.rangeClosed('a', 'e', 70000));
        Assertions.assertArrayEquals(new char[] { 'e' }, Array.range('e', 'a', -70000));
        Assertions.assertArrayEquals(new char[] { 'e' }, Array.rangeClosed('e', 'a', -70000));

        // ..while an equal-endpoint or direction-inconsistent range keeps its usual (empty / singleton) answer.
        Assertions.assertArrayEquals(new char[0], Array.range('a', 'a', 70000));
        Assertions.assertArrayEquals(new char[] { 'a' }, Array.rangeClosed('a', 'a', 70000));
        Assertions.assertArrayEquals(new char[0], Array.range('e', 'a', 70000));
        Assertions.assertArrayEquals(new char[0], Array.rangeClosed('e', 'a', 70000));
        Assertions.assertArrayEquals(new char[0], Array.range('a', 'e', -70000));
        Assertions.assertArrayEquals(new char[0], Array.rangeClosed('a', 'e', -70000));

        // a negative step is why this overload takes an int at all
        Assertions.assertArrayEquals(new char[] { 'e', 'd', 'c', 'b' }, Array.range('e', 'a', -1));
        Assertions.assertArrayEquals(new char[] { 'e', 'd', 'c', 'b', 'a' }, Array.rangeClosed('e', 'a', -1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Array.range('a', 'e', 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed('a', 'e', 0));
    }

    // ============================================================ C-013: NaN under a natural ordering

    /**
     * The rule the class javadoc and the three natural-ordering {@code minMax} overloads now state: a natural
     * ordering puts {@code NaN} above everything, so it comes back as the maximum and never as the minimum -
     * the opposite of the primitive {@code min(double...)}, which propagates it.
     */
    @Test
    public void testNaN_naturalOrderingPutsItAtTheTopNotTheBottom() {
        final Double[] withNaN = { 1.0, Double.NaN, 3.0 };

        Assertions.assertEquals(Pair.of(1.0, Double.NaN), Iterables.minMax(withNaN).get());
        Assertions.assertEquals(Pair.of(1.0, Double.NaN), Iterables.minMax(Arrays.asList(withNaN)).get());
        Assertions.assertEquals(Pair.of(1.0, Double.NaN), Iterables.minMax(Arrays.asList(withNaN).iterator()).get());
        Assertions.assertEquals(1.0, Iterables.min(withNaN).get());
        Assertions.assertEquals(Double.NaN, Iterables.max(withNaN).get());

        // NaN outranks even POSITIVE_INFINITY under the natural ordering
        Assertions.assertEquals(Pair.of(1.0, Double.NaN), Iterables.minMax(new Double[] { 1.0, Double.POSITIVE_INFINITY, Double.NaN }).get());

        final Float[] withNaNf = { 1f, Float.NaN, 3f };
        Assertions.assertEquals(Pair.of(1f, Float.NaN), Iterables.minMax(withNaNf).get());

        // ..whereas the primitive overloads propagate NaN through Math.min/Math.max
        Assertions.assertTrue(Double.isNaN(Iterables.min(new double[] { 1.0, Double.NaN, 3.0 }).get()));
        Assertions.assertTrue(Double.isNaN(Iterables.max(new double[] { 1.0, Double.NaN, 3.0 }).get()));

        // ..and the extractor family agrees with the natural ordering, not with the primitive one
        Assertions.assertEquals(1.0, Iterables.minDouble(withNaN, x -> x).get());
        Assertions.assertTrue(Double.isNaN(Iterables.maxDouble(withNaN, x -> x).get()));

        // a caller-supplied comparator decides for itself
        Assertions.assertEquals(Double.NaN, Iterables.min(withNaN, Comparator.reverseOrder()).get());
    }

    // ============================================================ C-014: signed zero in sumDouble

    /** A sum of nothing but {@code -0.0} returns {@code +0.0} - now documented, and matching the JDK. */
    @Test
    public void testSumDouble_signedZeroMatchesTheJdk() {
        final double sum = Iterables.sumDouble(Arrays.asList(-0.0, -0.0)).get();

        Assertions.assertEquals(0.0, sum);
        Assertions.assertEquals(Double.doubleToRawLongBits(+0.0), Double.doubleToRawLongBits(sum), "must be +0.0, not -0.0");
        // ..which is what java.util.stream.DoubleStream.sum() does too, so this is not a divergence
        Assertions.assertEquals(Double.doubleToRawLongBits(java.util.stream.DoubleStream.of(-0.0, -0.0).sum()), Double.doubleToRawLongBits(sum));
        // ..even though plain IEEE addition would keep the sign
        Assertions.assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(-0.0 + -0.0));

        // the extractor overload agrees, and a single -0.0 with a positive value is unremarkable
        Assertions.assertEquals(Double.doubleToRawLongBits(+0.0), Double.doubleToRawLongBits(Iterables.sumDouble(Arrays.asList(-0.0, -0.0), x -> x).get()));
        Assertions.assertEquals(1.0, Iterables.sumDouble(Arrays.asList(-0.0, 1.0)).get());

        // the other IEEE corners the same paragraph documents are unchanged
        Assertions.assertTrue(Double.isNaN(Iterables.sumDouble(Arrays.asList(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)).get()));
        Assertions.assertTrue(Double.isNaN(Iterables.sumDouble(Arrays.asList(1.0, Double.NaN)).get()));
    }

    // ============================================================ C-015: fill(List, ..) and UnsupportedOperationException

    /** Both {@code fill(List, ..)} overloads now document the {@code UnsupportedOperationException} they can raise. */
    @Test
    public void testFillList_unsupportedOperationIsDocumentedAndReal() {
        // an unmodifiable list rejects set(..)
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> Iterables.fill(Collections.unmodifiableList(new ArrayList<>(Arrays.asList("a"))), () -> "x"));
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> Iterables.fill(Collections.unmodifiableList(new ArrayList<>(Arrays.asList("a"))), 0, 1, () -> "x"));

        // a fixed-size list can be filled in place..
        final List<String> fixed = Arrays.asList("a", "b");
        Iterables.fill(fixed, () -> "x");
        Assertions.assertEquals(Arrays.asList("x", "x"), fixed);

        // ..but not extended - and the javadoc's "may have been partially filled" is literal: the in-range slots
        // are written before add(..) throws.
        final List<String> tooShort = Arrays.asList("a", "b");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> Iterables.fill(tooShort, 0, 4, () -> "x"));
        Assertions.assertEquals(Arrays.asList("x", "x"), tooShort);

        // a growable list is still extended, as documented
        final List<String> growable = new ArrayList<>(Arrays.asList("a", "b"));
        Iterables.fill(growable, 0, 4, () -> "x");
        Assertions.assertEquals(Arrays.asList("x", "x", "x", "x"), growable);

        // validation still precedes any mutation
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iterables.fill((List<String>) null, () -> "x"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iterables.fill(new ArrayList<String>(), (java.util.function.Supplier<String>) null));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(new ArrayList<>(Arrays.asList("a")), 2, 1, () -> "x"));
    }

    // ============================================================ D5: N.slice equality asymmetry (documented)

    @Test
    public void testSliceEquality_listSlicesCompareByValueCollectionSlicesByIdentity() {
        final List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Assertions.assertEquals(CommonUtil.slice(list, 0, 2), CommonUtil.slice(list, 0, 2));

        final Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        final Collection<String> s1 = CommonUtil.slice(set, 1, 3);
        final Collection<String> s2 = CommonUtil.slice(set, 1, 3);

        Assertions.assertNotEquals(s1, s2);
        Assertions.assertEquals(s1, s1);
        // ..but the contents are what they should be, and toString shows the slice, not the whole source
        Assertions.assertEquals(Arrays.asList("b", "c"), new ArrayList<>(s1));
        Assertions.assertEquals("[b, c]", s1.toString());

        // an empty source takes the ImmutableList path, so it does compare by value
        Assertions.assertEquals(CommonUtil.slice(new LinkedHashSet<String>(), 0, 0), CommonUtil.slice(new LinkedHashSet<String>(), 0, 0));
    }
}
