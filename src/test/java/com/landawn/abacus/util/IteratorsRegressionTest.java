package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the {@code Iterators} behaviour corrected in the 2026-08-31 review pass.
 *
 * <ul>
 *   <li>{@code merge(Collection, BiFunction)} executes its documented left fold iteratively, so a large
 *       collection of iterators no longer overflows the call stack, while the exact sequence of
 *       {@code nextSelector} calls - which the javadoc pins down with a worked example - is unchanged,</li>
 *   <li>{@code flatMap(Iterator, Function)} releases the last mapped iterator once it is exhausted.</li>
 * </ul>
 *
 * <p>The equivalence asserted here is over <i>independently advanced</i> sources, which is what
 * {@code merge(Collection, ..)} requires. Listing the same {@code Iterator} instance twice is documented as
 * unspecified - the old nested fold tended to throw {@link NoSuchElementException} for it - so it is
 * deliberately not locked down.</p>
 */
public class IteratorsRegressionTest extends TestBase {

    private static final BiFunction<Integer, Integer, MergeResult> MIN_FIRST = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

    private static List<Iterator<Integer>> iteratorsOf(final List<List<Integer>> sources) {
        final List<Iterator<Integer>> result = new ArrayList<>(sources.size());

        for (final List<Integer> source : sources) {
            result.add(source == null ? null : new ArrayList<>(source).iterator());
        }

        return result;
    }

    /** The nested left fold that {@code merge(Collection, ..)} used to build, kept here as the oracle. */
    private static ObjIterator<Integer> nestedLeftFold(final List<Iterator<Integer>> iters, final BiFunction<Integer, Integer, MergeResult> nextSelector) {
        ObjIterator<Integer> result = Iterators.merge(iters.get(0), iters.get(1), nextSelector);

        for (int i = 2, size = iters.size(); i < size; i++) {
            result = Iterators.merge(result, iters.get(i), nextSelector);
        }

        return result;
    }

    private static List<Integer> drain(final Iterator<Integer> iter) {
        return drain(iter, 1);
    }

    /**
     * Drains with a caller-chosen {@code hasNext()}/{@code next()} interleaving: {@code mode} extra
     * {@code hasNext()} calls before each element, or {@code mode == 3} for a caller that never calls
     * {@code hasNext()} at all and stops on {@link NoSuchElementException}.
     */
    private static List<Integer> drain(final Iterator<Integer> iter, final int mode) {
        final List<Integer> out = new ArrayList<>();

        if (mode == 3) {
            try {
                for (int i = 0; i < 500; i++) {
                    out.add(iter.next());
                }
            } catch (final NoSuchElementException e) {
                // exhausted
            }

            return out;
        }

        while (true) {
            for (int k = 0; k < mode; k++) {
                if (!iter.hasNext()) {
                    return out;
                }
            }

            if (!iter.hasNext()) {
                return out;
            }

            out.add(iter.next());
        }
    }

    // ============================================================ merge(Collection): documented call sequence

    @Test
    public void testMerge_reproducesTheSelectorCallSequenceInTheJavadoc() {
        final StringBuilder calls = new StringBuilder();
        final BiFunction<Integer, Integer, MergeResult> recording = (a, b) -> {
            calls.append('(').append(a).append(',').append(b).append(") ");
            return MIN_FIRST.apply(a, b);
        };

        final List<Iterator<Integer>> iters = iteratorsOf(Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5), Arrays.asList(3, 6)));

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), drain(Iterators.merge(iters, recording)));
        Assertions.assertEquals("(1,2) (1,3) (4,2) (2,3) (4,5) (4,3) (4,6) (5,6)", calls.toString().trim());
    }

    // ============================================================ merge(Collection): equivalence with the fold

    /**
     * Runs the same sources through the new implementation and through the nested fold, comparing both the
     * elements produced and every {@code nextSelector} call. A selector that answers from a call counter makes
     * any divergence in the call sequence show up in the output as well.
     */
    private static void assertMatchesNestedFold(final List<List<Integer>> sources, final MergeResult[] pattern) {
        // Every hasNext()/next() interleaving, because the new implementation answers "does this node have a
        // next element?" from a scratch array refreshed on each query rather than by recursing on demand.
        for (int mode = 0; mode <= 3; mode++) {
            final int drainMode = mode;

            final List<String> actualCalls = new ArrayList<>();
            final int[] actualIdx = { 0 };
            final List<Integer> actual = drain(Iterators.merge(iteratorsOf(sources), (a, b) -> {
                actualCalls.add(a + "," + b);
                return pattern[actualIdx[0]++ % pattern.length];
            }), drainMode);

            final List<String> expectedCalls = new ArrayList<>();
            final int[] expectedIdx = { 0 };
            final List<Integer> expected = drain(nestedLeftFold(iteratorsOf(sources), (a, b) -> {
                expectedCalls.add(a + "," + b);
                return pattern[expectedIdx[0]++ % pattern.length];
            }), drainMode);

            Assertions.assertEquals(expected, actual, () -> "elements differ for " + sources + " (drain mode " + drainMode + ")");
            Assertions.assertEquals(expectedCalls, actualCalls, () -> "selector call sequence differs for " + sources + " (drain mode " + drainMode + ")");
        }
    }

    @Test
    public void testMerge_matchesTheNestedFoldOnHandPickedShapes() {
        final MergeResult[] minFirstLike = { MergeResult.TAKE_FIRST, MergeResult.TAKE_SECOND };

        assertMatchesNestedFold(Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5), Arrays.asList(3, 6)), minFirstLike);
        assertMatchesNestedFold(Arrays.asList(Arrays.asList(), Arrays.asList(1), Arrays.asList(2, 3)), minFirstLike);
        assertMatchesNestedFold(Arrays.asList(Arrays.asList(1), Arrays.asList(), Arrays.asList()), minFirstLike);
        assertMatchesNestedFold(Arrays.asList(Arrays.asList(), Arrays.asList(), Arrays.asList()), minFirstLike);
        assertMatchesNestedFold(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(), Arrays.asList(4), Arrays.asList(), Arrays.asList(5, 6)), minFirstLike);
        assertMatchesNestedFold(Arrays.asList(Arrays.asList(1, 1, 1), Arrays.asList(1, 1), Arrays.asList(1)), minFirstLike);
    }

    @Test
    public void testMerge_matchesTheNestedFoldForArbitraryAndStatefulSelectors() {
        // Balanced merging would have produced the same multiset but a different order for selectors like
        // these, which is why the fold - not just the result - has to be preserved.
        final MergeResult[][] patterns = { { MergeResult.TAKE_FIRST }, { MergeResult.TAKE_SECOND },
                { MergeResult.TAKE_SECOND, MergeResult.TAKE_FIRST, MergeResult.TAKE_FIRST },
                { MergeResult.TAKE_FIRST, MergeResult.TAKE_FIRST, MergeResult.TAKE_SECOND, MergeResult.TAKE_SECOND } };

        final Random rnd = new Random(20260831L);

        for (int round = 0; round < 400; round++) {
            final int sourceCount = 3 + rnd.nextInt(6);
            final List<List<Integer>> sources = new ArrayList<>(sourceCount);

            for (int i = 0; i < sourceCount; i++) {
                final int len = rnd.nextInt(5);
                final List<Integer> source = new ArrayList<>(len);

                for (int j = 0; j < len; j++) {
                    source.add(rnd.nextInt(10));
                }

                sources.add(source);
            }

            assertMatchesNestedFold(sources, patterns[rnd.nextInt(patterns.length)]);
        }
    }

    @Test
    public void testMerge_matchesTheNestedFoldWithNullElementsInTheSources() {
        final List<Iterator<Integer>> withNullValues = Arrays.asList(Arrays.asList(1, null).iterator(), Arrays.asList((Integer) null).iterator(),
                Arrays.asList(2).iterator());
        final BiFunction<Integer, Integer, MergeResult> nullFriendly = (a, b) -> a == null ? MergeResult.TAKE_FIRST
                : b == null ? MergeResult.TAKE_SECOND : MIN_FIRST.apply(a, b);

        final List<Integer> actual = drain(Iterators.merge(withNullValues, nullFriendly));
        final List<Integer> expected = drain(nestedLeftFold(
                Arrays.asList(Arrays.asList(1, null).iterator(), Arrays.asList((Integer) null).iterator(), Arrays.asList(2).iterator()), nullFriendly));

        Assertions.assertEquals(expected, actual);
    }

    // ============================================================ merge(Collection): stack safety and edges

    @Test
    public void testMerge_doesNotOverflowTheStackForManyIterators() {
        final int count = 20_000;
        final List<Iterator<Integer>> iters = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            iters.add(Arrays.asList(i).iterator());
        }

        final List<Integer> merged = drain(Iterators.merge(iters, MIN_FIRST));

        Assertions.assertEquals(count, merged.size());
        Assertions.assertEquals(CommonUtil.toList(Array.range(0, count)), merged);
    }

    @Test
    public void testMerge_treatsANullIteratorAsEmptyAtEverySize() {
        final BiFunction<Integer, Integer, MergeResult> sel = MIN_FIRST;

        Assertions.assertEquals(Arrays.asList(), drain(Iterators.merge(Arrays.<Iterator<Integer>> asList((Iterator<Integer>) null), sel)));
        Assertions.assertEquals(Arrays.asList(), drain(Iterators.merge(Arrays.asList(null, null), sel)));
        Assertions.assertEquals(Arrays.asList(), drain(Iterators.merge(Arrays.asList(null, null, null), sel)));
        Assertions.assertEquals(Arrays.asList(1, 2, 3),
                drain(Iterators.merge(Arrays.asList(null, Arrays.asList(1, 3).iterator(), null, Arrays.asList(2).iterator()), sel)));
    }

    /** A {@code Collection} whose {@code size()} deliberately disagrees with what its iterator yields. */
    private static final class LyingSizeCollection extends java.util.AbstractCollection<Iterator<Integer>> {
        private final List<Iterator<Integer>> actual;
        private final int reported;

        LyingSizeCollection(final List<Iterator<Integer>> actual, final int reported) {
            this.actual = actual;
            this.reported = reported;
        }

        @Override
        public Iterator<Iterator<Integer>> iterator() {
            return actual.iterator();
        }

        @Override
        public int size() {
            return reported;
        }
    }

    /**
     * The nested fold this replaced looped on {@code hasNext()}, so it coped with a {@code size()} that
     * disagreed with the iterator - which a concurrently modified collection can report. Driving the source
     * scan from {@code size()} instead would drop the surplus sources or read past the end of a short one.
     */
    @Test
    public void testMerge_doesNotTrustACollectionSizeThatDisagreesWithItsIterator() {
        for (final int[] shape : new int[][] { { 5, 5 }, { 5, 8 }, { 5, 3 }, { 4, 6 }, { 6, 4 }, { 3, 9 }, { 8, 3 } }) {
            final int actual = shape[0];
            final int reported = shape[1];
            final List<Iterator<Integer>> sources = new ArrayList<>();
            final List<Integer> expected = new ArrayList<>();

            for (int i = 0; i < actual; i++) {
                sources.add(Arrays.asList(i).iterator());
                expected.add(i);
            }

            Assertions.assertEquals(expected, drain(Iterators.merge(new LyingSizeCollection(sources, reported), MIN_FIRST)),
                    () -> "actual=" + actual + " size()=" + reported);
        }
    }

    @Test
    public void testMerge_overReportedSizeStillFallsBackToTheSmallCollectionShapes() {
        // size() claims three or more, the iterator yields fewer: the result must match what the size()-based
        // dispatch would have produced for that real count.
        Assertions.assertEquals(Arrays.asList(), drain(Iterators.merge(new LyingSizeCollection(new ArrayList<>(), 4), MIN_FIRST)));
        Assertions.assertEquals(Arrays.asList(1, 2),
                drain(Iterators.merge(new LyingSizeCollection(new ArrayList<>(Arrays.asList(Arrays.asList(1, 2).iterator())), 5), MIN_FIRST)));
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), drain(Iterators
                .merge(new LyingSizeCollection(new ArrayList<>(Arrays.asList(Arrays.asList(1, 3).iterator(), Arrays.asList(2, 4).iterator())), 7), MIN_FIRST)));
    }

    @Test
    public void testMerge_hasNextIsIdempotentAndNextThrowsWhenExhausted() {
        final ObjIterator<Integer> merged = Iterators.merge(iteratorsOf(Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3))), MIN_FIRST);

        Assertions.assertTrue(merged.hasNext());
        Assertions.assertTrue(merged.hasNext());
        Assertions.assertEquals(Arrays.asList(1, 2, 3), drain(merged));
        Assertions.assertFalse(merged.hasNext());
        Assertions.assertFalse(merged.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, merged::next);
    }

    @Test
    public void testMerge_nextWithoutCallingHasNext() {
        final ObjIterator<Integer> merged = Iterators.merge(iteratorsOf(Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5), Arrays.asList(3, 6))),
                MIN_FIRST);
        final List<Integer> out = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            out.add(merged.next());
        }

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), out);
        Assertions.assertThrows(NoSuchElementException.class, merged::next);
    }

    @Test
    public void testMerge_sizesZeroOneAndTwoAreUnchanged() {
        Assertions.assertFalse(Iterators.merge(new ArrayList<Iterator<Integer>>(), MIN_FIRST).hasNext());
        Assertions.assertEquals(Arrays.asList(1, 2), drain(Iterators.merge(Arrays.asList(Arrays.asList(1, 2).iterator()), MIN_FIRST)));
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4),
                drain(Iterators.merge(Arrays.asList(Arrays.asList(1, 3).iterator(), Arrays.asList(2, 4).iterator()), MIN_FIRST)));
    }

    @Test
    public void testMergeIterables_usesTheSameFold() {
        final List<Iterable<Integer>> iterables = Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5), Arrays.asList(3, 6));

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), drain(Iterators.mergeIterables(iterables, MIN_FIRST)));
    }

    @Test
    public void testMerge_isLazyPerElement() {
        // The fold must not drain its sources up front: only what has been asked for may be consumed.
        final int[] pulled = { 0 };
        final Iterator<Integer> counting = new ObjIterator<>() {
            private int next = 0;

            @Override
            public boolean hasNext() {
                return next < 100;
            }

            @Override
            public Integer next() {
                pulled[0]++;
                return next++;
            }
        };

        final ObjIterator<Integer> merged = Iterators.merge(Arrays.asList(counting, Arrays.asList(1000).iterator(), Arrays.asList(2000).iterator()), MIN_FIRST);

        Assertions.assertEquals(0, merged.next());
        Assertions.assertTrue(pulled[0] <= 2, "pulled " + pulled[0] + " elements to produce one");
    }

    // ============================================================ flatMap exhaustion

    @Test
    public void testFlatMap_afterExhaustionHasNextStaysFalseAndNextThrows() {
        final ObjIterator<Integer> flat = Iterators.flatMap(Arrays.asList(1, 2).iterator(), i -> Arrays.asList(i, i * 10));

        Assertions.assertEquals(Arrays.asList(1, 10, 2, 20), drain(flat));
        Assertions.assertFalse(flat.hasNext());
        Assertions.assertFalse(flat.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, flat::next);
    }

    @Test
    public void testFlatMap_doesNotReinvokeTheMapperAfterExhaustion() {
        final int[] calls = { 0 };
        final ObjIterator<Integer> flat = Iterators.flatMap(Arrays.asList(1, 2).iterator(), i -> {
            calls[0]++;
            return Arrays.asList(i);
        });

        drain(flat);
        final int afterDrain = calls[0];
        flat.hasNext();
        flat.hasNext();

        Assertions.assertEquals(2, afterDrain);
        Assertions.assertEquals(afterDrain, calls[0]);
    }

    @Test
    public void testFlatMap_skipsEmptyAndNullMappedIterables() {
        Assertions.assertEquals(Arrays.asList(1, 3),
                drain(Iterators.flatMap(Arrays.asList(1, 2, 3).iterator(), i -> i == 2 ? Arrays.asList() : Arrays.asList(i))));
        Assertions.assertEquals(Arrays.asList(1, 3), drain(Iterators.flatMap(Arrays.asList(1, 2, 3).iterator(), i -> i == 2 ? null : Arrays.asList(i))));
        Assertions.assertEquals(Arrays.asList(), drain(Iterators.flatMap(Arrays.asList(1, 2).iterator(), i -> null)));
    }
}
