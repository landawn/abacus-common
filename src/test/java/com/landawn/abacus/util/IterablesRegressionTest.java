package com.landawn.abacus.util;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;

/**
 * Regression tests for the {@code Iterables} behaviour corrected in the 2026-08-31 review pass.
 *
 * <ul>
 *   <li>{@code intersection(...).isEmpty()} probes {@code set2} from {@code set1} - the direction
 *       {@code iterator()}/{@code size()} use - instead of delegating to {@link Collections#disjoint},
 *       which iterates the wrong set,</li>
 *   <li>{@code symmetricDifference(...).isEmpty()} tests both directions instead of the one-directional
 *       {@code set1.equals(set2)},</li>
 *   <li>{@code min}/{@code max(Iterator, Comparator)} short-circuit on a {@code null} <i>first</i> element too,
 *       matching {@code N.min}/{@code N.max}.</li>
 * </ul>
 */
public class IterablesRegressionTest extends TestBase {

    // ============================================================ SetView.isEmpty() vs size()/iterator()

    /** Counts how often the view iterates this set and how often it probes it with {@code contains}. */
    private static final class CountingSet<E> extends AbstractSet<E> {
        private final Set<E> delegate;
        int iterated;
        int probed;

        CountingSet(final Set<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Iterator<E> iterator() {
            final Iterator<E> it = delegate.iterator();

            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public E next() {
                    iterated++;
                    return it.next();
                }
            };
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean contains(final Object o) {
            probed++;
            return delegate.contains(o);
        }
    }

    private static <T> void assertViewSelfConsistent(final Iterables.SetView<T> view) {
        final List<T> byIteration = new ArrayList<>();
        view.iterator().forEachRemaining(byIteration::add);

        Assertions.assertEquals(byIteration.size(), view.size(), "size() must agree with iterator()");
        Assertions.assertEquals(byIteration.isEmpty(), view.isEmpty(), "isEmpty() must agree with iterator()");
        Assertions.assertEquals(view.size() == 0, view.isEmpty(), "isEmpty() must agree with size()");
    }

    @Test
    public void testIntersection_isEmpty_probesSet2FromSet1_notTheOtherWayAround() {
        final CountingSet<Integer> small = new CountingSet<>(new LinkedHashSet<>(Arrays.asList(1, 2, 3)));
        final CountingSet<Integer> big = new CountingSet<>(new LinkedHashSet<>(Arrays.asList(10, 11, 12, 13, 14, 15)));

        Assertions.assertTrue(Iterables.intersection(small, big).isEmpty());

        // The view is documented to iterate set1 and probe set2 ("pass the smaller set first"). Collections
        // .disjoint did the opposite, which made isEmpty() cost O(set2) on exactly the shape the doc recommends.
        Assertions.assertEquals(3, small.iterated);
        Assertions.assertEquals(0, small.probed);
        Assertions.assertEquals(0, big.iterated);
        Assertions.assertEquals(3, big.probed);
    }

    @Test
    public void testIntersection_isEmpty_stopsAtTheFirstCommonElement() {
        final CountingSet<Integer> set1 = new CountingSet<>(new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4, 5)));
        final CountingSet<Integer> set2 = new CountingSet<>(new LinkedHashSet<>(Arrays.asList(1)));

        Assertions.assertFalse(Iterables.intersection(set1, set2).isEmpty());
        Assertions.assertEquals(1, set1.iterated);
    }

    @Test
    public void testIntersection_isEmptyAgreesWithSizeForMismatchedEquivalenceRelations() {
        final TreeSet<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.add("a");
        final Set<String> exact = new HashSet<>(Arrays.asList("A"));

        // Collections.disjoint probed caseInsensitive with "A" and answered "not disjoint", while size() and
        // iterator() probed exact with "a" and found nothing: isEmpty() was false for a view of size 0.
        final Iterables.SetView<String> view = Iterables.intersection(caseInsensitive, exact);
        Assertions.assertEquals(0, view.size());
        Assertions.assertTrue(view.isEmpty());
        assertViewSelfConsistent(view);

        final Iterables.SetView<String> reversed = Iterables.intersection(exact, caseInsensitive);
        Assertions.assertEquals(1, reversed.size());
        Assertions.assertFalse(reversed.isEmpty());
        assertViewSelfConsistent(reversed);
    }

    @Test
    public void testIntersection_isEmptyAgreesWithSizeForIdentityBackedSets() {
        final Set<String> identity = Collections.newSetFromMap(new IdentityHashMap<>());
        identity.add(new String("x"));
        final Set<String> equality = new HashSet<>(Arrays.asList(new String("x")));

        assertViewSelfConsistent(Iterables.intersection(identity, equality));
        assertViewSelfConsistent(Iterables.intersection(equality, identity));
    }

    @Test
    public void testIntersection_isEmptyOnOrdinaryInputs() {
        final Set<Integer> a = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        final Set<Integer> b = new LinkedHashSet<>(Arrays.asList(3, 4));

        Assertions.assertFalse(Iterables.intersection(a, b).isEmpty());
        Assertions.assertTrue(Iterables.intersection(a, new LinkedHashSet<>()).isEmpty());
        Assertions.assertTrue(Iterables.intersection(new LinkedHashSet<Integer>(), b).isEmpty());
        Assertions.assertTrue(Iterables.intersection((Set<Integer>) null, b).isEmpty());
        Assertions.assertTrue(Iterables.intersection(a, (Set<Integer>) null).isEmpty());
        assertViewSelfConsistent(Iterables.intersection(a, b));
    }

    @Test
    public void testIntersection_isEmptyIsLive() {
        final Set<Integer> a = new LinkedHashSet<>(Arrays.asList(1, 2));
        final Set<Integer> b = new LinkedHashSet<>();
        final Iterables.SetView<Integer> view = Iterables.intersection(a, b);

        Assertions.assertTrue(view.isEmpty());
        b.add(2);
        Assertions.assertFalse(view.isEmpty());
        assertViewSelfConsistent(view);
    }

    @Test
    public void testSymmetricDifference_isEmptyAgreesWithSizeForMismatchedEquivalenceRelations() {
        final TreeSet<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.add("a");
        final Set<String> exact = new HashSet<>(Arrays.asList("A"));

        // set1.equals(set2) was true here (equal sizes, and caseInsensitive.contains("A")), yet the view
        // still yields "a" because exact.contains("a") is false.
        final Iterables.SetView<String> view = Iterables.symmetricDifference(caseInsensitive, exact);
        Assertions.assertEquals(1, view.size());
        Assertions.assertFalse(view.isEmpty());
        assertViewSelfConsistent(view);
    }

    @Test
    public void testSymmetricDifference_isEmptyOnOrdinaryInputs() {
        final Set<Integer> a = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        final Set<Integer> b = new LinkedHashSet<>(Arrays.asList(1, 2, 3));

        Assertions.assertTrue(Iterables.symmetricDifference(a, b).isEmpty());
        Assertions.assertTrue(Iterables.symmetricDifference(new LinkedHashSet<Integer>(), new LinkedHashSet<Integer>()).isEmpty());
        Assertions.assertFalse(Iterables.symmetricDifference(a, new LinkedHashSet<>(Arrays.asList(1, 2))).isEmpty());
        Assertions.assertFalse(Iterables.symmetricDifference(new LinkedHashSet<>(Arrays.asList(1, 2)), a).isEmpty());
        assertViewSelfConsistent(Iterables.symmetricDifference(a, b));
        assertViewSelfConsistent(Iterables.symmetricDifference(a, new LinkedHashSet<>(Arrays.asList(3, 4))));
    }

    @Test
    public void testSymmetricDifference_isEmptyWithNullArguments() {
        final Set<Integer> a = new LinkedHashSet<>(Arrays.asList(1, 2));

        Assertions.assertFalse(Iterables.symmetricDifference(a, (Set<Integer>) null).isEmpty());
        Assertions.assertFalse(Iterables.symmetricDifference((Set<Integer>) null, a).isEmpty());
        Assertions.assertTrue(Iterables.symmetricDifference((Set<Integer>) null, (Set<Integer>) null).isEmpty());
    }

    /**
     * The probing views reach {@code isEmpty()}, {@code size()} and {@code iterator()} through the same
     * {@code contains} calls, so a set that rejects an element it is asked about - a {@code TreeSet} asked
     * about {@code null} - must fail the same way in all three. Before the fix {@code isEmpty()} answered
     * {@code false} for a view whose {@code size()} and {@code iterator()} both threw; {@code difference}
     * already behaved consistently, which is why it is included here as the control.
     */
    @Test
    public void testProbingViews_failTheSameWayInIsEmptySizeAndIterator() {
        final Set<String> nullFirst = new LinkedHashSet<>(Arrays.asList(null, "a"));
        final Set<String> rejectsNull = new TreeSet<>(Arrays.asList("a", "b"));

        for (final Iterables.SetView<String> view : Arrays.asList(Iterables.intersection(nullFirst, rejectsNull), Iterables.difference(nullFirst, rejectsNull),
                Iterables.symmetricDifference(nullFirst, rejectsNull))) {
            Assertions.assertThrows(NullPointerException.class, view::size);
            Assertions.assertThrows(NullPointerException.class, () -> view.iterator().forEachRemaining(e -> {
            }));
            Assertions.assertThrows(NullPointerException.class, view::isEmpty, "isEmpty() must not give a lucky answer for a view that cannot be read");
        }
    }

    @Test
    public void testProbingViews_doNotThrowWhenTheProbeNeverReachesTheRejectedElement() {
        // set1 yields "a" first and set2 contains it, so intersection settles before it ever probes with null.
        final Set<String> nullLast = new LinkedHashSet<>(Arrays.asList("a", null));
        final Set<String> rejectsNull = new TreeSet<>(Arrays.asList("a", "b"));

        Assertions.assertFalse(Iterables.intersection(nullLast, rejectsNull).isEmpty());

        // union never probes at all, so it is unaffected either way.
        final Iterables.SetView<String> union = Iterables.union(nullLast, rejectsNull);
        Assertions.assertFalse(union.isEmpty());
        Assertions.assertEquals(3, union.size());
    }

    @Test
    public void testUnionAndDifference_isEmptyStillAgreeWithSize() {
        final Set<Integer> a = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        final Set<Integer> b = new LinkedHashSet<>(Arrays.asList(3, 4));

        assertViewSelfConsistent(Iterables.union(a, b));
        assertViewSelfConsistent(Iterables.difference(a, b));
        assertViewSelfConsistent(Iterables.difference(a, a));
    }

    // ============================================================ min/max(Iterator, Comparator) short-circuit

    @Test
    public void testMin_shortCircuitsOnANullFirstElement() {
        final List<String> values = Arrays.asList(null, "b", "c", "d");
        final Iterator<String> iter = values.iterator();

        Assertions.assertEquals(Nullable.of(null), Iterables.min(iter, Comparators.<String> naturalOrder()));

        // Previously the null first element was taken as the initial candidate and never re-examined, so the
        // whole iterator was drained. N.min stops after reading one more element; so does this now.
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("c", iter.next());
    }

    @Test
    public void testMin_shortCircuitsOnALaterNullElement() {
        final Iterator<String> iter = Arrays.asList("b", null, "c", "d").iterator();

        Assertions.assertEquals(Nullable.of(null), Iterables.min(iter, Comparators.<String> naturalOrder()));
        Assertions.assertEquals("c", iter.next());
    }

    @Test
    public void testMax_shortCircuitsOnANullFirstElement() {
        final Iterator<String> iter = Arrays.asList(null, "b", "c", "d").iterator();

        Assertions.assertEquals(Nullable.of(null), Iterables.max(iter, Comparators.<String> nullsLast()));
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("c", iter.next());
    }

    @Test
    public void testMax_shortCircuitsOnALaterNullElement() {
        final Iterator<String> iter = Arrays.asList("b", null, "c", "d").iterator();

        Assertions.assertEquals(Nullable.of(null), Iterables.max(iter, Comparators.<String> nullsLast()));
        Assertions.assertEquals("c", iter.next());
    }

    @Test
    public void testMinMax_resultsAreUnchangedByTheShortCircuitMove() {
        Assertions.assertEquals(Nullable.of("a"), Iterables.min(Arrays.asList("b", "a", "c").iterator(), Comparators.<String> naturalOrder()));
        Assertions.assertEquals(Nullable.of("c"), Iterables.max(Arrays.asList("b", "a", "c").iterator(), Comparators.<String> nullsLast()));
        Assertions.assertEquals(Nullable.of("b"), Iterables.min(Arrays.asList("b", null, "c").iterator()));
        Assertions.assertEquals(Nullable.of("c"), Iterables.max(Arrays.asList("b", null, "c").iterator()));
        Assertions.assertEquals(Nullable.<String> empty(), Iterables.min((Iterator<String>) null, Comparators.<String> naturalOrder()));
        Assertions.assertEquals(Nullable.<String> empty(), Iterables.max((Iterator<String>) null, Comparators.<String> nullsLast()));
    }

    @Test
    public void testMinMax_allNullElements() {
        Assertions.assertEquals(Nullable.of(null), Iterables.min(Arrays.asList((String) null, null).iterator(), Comparators.<String> naturalOrder()));
        Assertions.assertEquals(Nullable.of(null), Iterables.max(Arrays.asList((String) null, null).iterator(), Comparators.<String> nullsLast()));
        Assertions.assertEquals(Nullable.of(null), Iterables.min(Arrays.asList((String) null).iterator(), Comparators.<String> naturalOrder()));
    }

    // ============================================================ sum* vs average* presence (documented)

    /**
     * The {@code sum*} javadoc added by this pass states the distinction explicitly, so lock the two concrete
     * values it names: {@code sum} is present whenever the source had elements ({@code null} contributes the
     * additive identity), while the big-number {@code average} is present only when a non-{@code null} value
     * existed. They answer different questions, and the docs must not drift from that.
     */
    @Test
    public void testSumAndAverageReportPresenceDifferentlyForAnAllNullSource() {
        final List<java.math.BigInteger> allNullInts = Arrays.asList((java.math.BigInteger) null, null);
        final List<java.math.BigDecimal> allNullDecimals = Arrays.asList((java.math.BigDecimal) null, null);

        Assertions.assertEquals(java.math.BigInteger.ZERO, Iterables.sumBigInteger(allNullInts).orElseNull());
        Assertions.assertEquals(java.math.BigDecimal.ZERO, Iterables.sumBigDecimal(allNullDecimals).orElseNull());
        Assertions.assertTrue(Iterables.averageBigInteger(allNullInts).isEmpty());
        Assertions.assertTrue(Iterables.averageBigDecimal(allNullDecimals).isEmpty());

        // ... and an actually empty source is absent for both.
        Assertions.assertTrue(Iterables.sumBigInteger(new ArrayList<>()).isEmpty());
        Assertions.assertTrue(Iterables.sumBigDecimal(new ArrayList<>()).isEmpty());
        Assertions.assertTrue(Iterables.averageBigInteger(new ArrayList<>()).isEmpty());
        Assertions.assertTrue(Iterables.averageBigDecimal(new ArrayList<>()).isEmpty());

        // The extractor overloads behave identically.
        Assertions.assertEquals(java.math.BigInteger.ZERO, Iterables.sumBigInteger(Arrays.asList("x", "y"), s -> null).orElseNull());
        Assertions.assertTrue(Iterables.averageBigInteger(Arrays.asList("x", "y"), s -> null).isEmpty());
    }

    // ============================================================ min/max vs N

    /** Counts elements actually pulled, so "how far did it consume?" is comparable between the two APIs. */
    private static final class CountingIterator implements Iterator<String> {
        private final Iterator<String> delegate;
        int pulled;

        CountingIterator(final List<String> values) {
            delegate = values.iterator();
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public String next() {
            pulled++;
            return delegate.next();
        }
    }

    /**
     * The point of the fix is that {@code Iterables.min}/{@code max} mirror {@code N.min}/{@code N.max}, so state
     * that directly: same result <i>and</i> same amount of the iterator consumed, over every small input of
     * {@code a}/{@code b}/{@code c}/{@code null} against the comparators that do and do not short-circuit.
     */
    @Test
    public void testMinMax_agreeWithNOnBothValueAndConsumption() {
        final String[] alphabet = { "a", "b", "c", null };
        @SuppressWarnings("unchecked")
        final Comparator<String>[] comparators = new Comparator[] { Comparators.<String> naturalOrder(), Comparators.<String> nullsFirst(),
                Comparators.<String> nullsLast(), Comparators.<String> reverseOrder(), Comparators.nullsFirst(Comparator.<String> naturalOrder()),
                Comparators.nullsLast(Comparator.<String> naturalOrder()) };
        final Random rnd = new Random(20260831L);

        for (int round = 0; round < 3000; round++) {
            final int len = 1 + rnd.nextInt(6);
            final List<String> data = new ArrayList<>(len);

            for (int i = 0; i < len; i++) {
                data.add(alphabet[rnd.nextInt(alphabet.length)]);
            }

            final Comparator<String> cmp = comparators[rnd.nextInt(comparators.length)];

            final CountingIterator minMine = new CountingIterator(data);
            final CountingIterator minTheirs = new CountingIterator(data);
            Assertions.assertEquals(N.min(minTheirs, cmp), Iterables.min(minMine, cmp).orElseNull(), () -> "min value for " + data);
            Assertions.assertEquals(minTheirs.pulled, minMine.pulled, () -> "min consumed a different number of elements for " + data);

            final CountingIterator maxMine = new CountingIterator(data);
            final CountingIterator maxTheirs = new CountingIterator(data);
            Assertions.assertEquals(N.max(maxTheirs, cmp), Iterables.max(maxMine, cmp).orElseNull(), () -> "max value for " + data);
            Assertions.assertEquals(maxTheirs.pulled, maxMine.pulled, () -> "max consumed a different number of elements for " + data);
        }
    }

    @Test
    public void testMinMax_otherComparatorsStillDrainTheIterator() {
        // The short-circuit is keyed on one shared comparator instance, not on comparator semantics: an
        // equivalent nulls-first comparator built by the caller must still produce the right answer.
        final Iterator<String> iter = Arrays.asList("b", null, "c").iterator();
        final java.util.Comparator<String> equivalent = Comparators.nullsFirst(java.util.Comparator.<String> naturalOrder());

        Assertions.assertEquals(Nullable.of(null), Iterables.min(iter, equivalent));
        Assertions.assertFalse(iter.hasNext());
    }
}
