package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-08-31 CommonUtil/N review cycle (ledger
 * {@code scripts/cross_review/N_ledger_2026-08-31.md}).
 *
 * <ul>
 *   <li><b>C-001</b> {@code N.merge(Collection<Iterable>, ...)} used a left-nested anonymous-iterator
 *       fold and overflowed the stack at ~10,000 sources.</li>
 *   <li><b>C-002</b> {@code removeAt(List,int...)}, {@code removeRange(List,int,int)},
 *       {@code replaceRange(List,...)} and {@code removeDuplicates(Collection,boolean)} cleared the
 *       caller's collection and re-added without a rollback, so a failing re-population destroyed the
 *       caller's data.</li>
 *   <li><b>C-003</b> {@code newInstance(Class)} threw a bare NPE for a member class whose enclosing
 *       constructor chain could not be resolved, contradicting its documented
 *       {@code IllegalArgumentException}.</li>
 *   <li><b>C-004</b> {@code checkFromToIndex} rendered the exclusive upper bound with {@code ']'}.</li>
 *   <li><b>C-005</b> 12 range guards carried a dead first disjunct (behaviour-preserving cleanup).</li>
 * </ul>
 */
public class CommonUtilNMergeRollbackFixesTest extends TestBase {

    private static final BiFunction<Integer, Integer, MergeResult> ASCENDING = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

    // ---------------------------------------------------------------- helpers

    /**
     * A list whose {@code addAll} throws a bounded number of times. One failure is the interesting
     * case: a rollback, if present, still has a working {@code addAll} to restore with, so an empty
     * list afterwards proves there was no rollback rather than that the restore also failed.
     */
    private static final class FailingAddAllList<T> extends ArrayList<T> {
        private static final long serialVersionUID = 1L;
        private int failuresLeft;

        FailingAddAllList(final Collection<? extends T> initial) {
            super(initial);
        }

        public void armFailures(final int n) {
            failuresLeft = n;
        }

        @Override
        public boolean addAll(final Collection<? extends T> c) {
            if (failuresLeft-- > 0) {
                throw new IllegalStateException("boom");
            }
            return super.addAll(c);
        }
    }

    private static final class FailingAddAllDeque<T> extends ArrayDeque<T> {
        private static final long serialVersionUID = 1L;
        private int failuresLeft;

        FailingAddAllDeque(final Collection<? extends T> initial) {
            super(initial);
        }

        public void armFailures(final int n) {
            failuresLeft = n;
        }

        @Override
        public boolean addAll(final Collection<? extends T> c) {
            if (failuresLeft-- > 0) {
                throw new IllegalStateException("boom");
            }
            return super.addAll(c);
        }
    }

    /**
     * A minimal {@code AbstractList} that does <em>not</em> override {@code removeRange}, is not
     * {@code RandomAccess}, and is not a {@code LinkedList} - the shape that would have been pushed onto
     * {@code AbstractList}'s quadratic {@code listIterator().remove()} loop had {@code removeRange} been
     * routed through {@code subList().clear()} unconditionally.
     */
    private static final class ExoticList<T> extends AbstractList<T> {
        private final ArrayList<T> backing;

        ExoticList(final Collection<? extends T> initial) {
            backing = new ArrayList<>(initial);
        }

        @Override
        public T get(final int index) {
            return backing.get(index);
        }

        @Override
        public int size() {
            return backing.size();
        }

        @Override
        public T set(final int index, final T element) {
            return backing.set(index, element);
        }

        @Override
        public void add(final int index, final T element) {
            backing.add(index, element);
        }

        @Override
        public T remove(final int index) {
            return backing.remove(index);
        }
    }

    /**
     * A set that accepts the probe element but refuses to give it back - the shape that makes
     * {@code probeUnmodifiable} throw instead of answering.
     */
    private static final class UnremovableSet<T> extends LinkedHashSet<T> {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean remove(final Object o) {
            throw new UnsupportedOperationException("cannot remove");
        }
    }

    private static final class FailingAddAllSet<T> extends LinkedHashSet<T> {
        private static final long serialVersionUID = 1L;
        private int failuresLeft;

        FailingAddAllSet(final Collection<? extends T> initial) {
            super(initial);
        }

        public void armFailures(final int n) {
            failuresLeft = n;
        }

        @Override
        public boolean addAll(final Collection<? extends T> c) {
            if (failuresLeft-- > 0) {
                throw new IllegalStateException("boom");
            }
            return super.addAll(c);
        }
    }

    /** Counts how often the outer collection is traversed. */
    /**
     * A collection whose {@code size()} disagrees with what its iterator yields - a concurrently modified
     * or loosely implemented one. {@code merge} must take the iterator's word for it.
     */
    private static final class LyingSizeCollection<T> extends AbstractCollection<T> {
        private final List<T> actual;
        private final int reportedSize;

        LyingSizeCollection(final List<T> actual, final int reportedSize) {
            this.actual = actual;
            this.reportedSize = reportedSize;
        }

        @Override
        public Iterator<T> iterator() {
            return actual.iterator();
        }

        @Override
        public int size() {
            return reportedSize;
        }
    }

    private static final class TraversalCountingList<T> extends ArrayList<T> {
        private static final long serialVersionUID = 1L;
        private int traversals;

        TraversalCountingList(final Collection<? extends T> c) {
            super(c);
        }

        @Override
        public Iterator<T> iterator() {
            traversals++;
            return super.iterator();
        }
    }

    /** A non-static member class with no no-arg constructor - C-003's trigger. */
    public static class MemberWithoutNoArgCtor {
        @SuppressWarnings("unused")
        MemberWithoutNoArgCtor(final String required) {
        }
    }

    private static List<Iterable<Integer>> fourSources() {
        return Arrays.asList(Arrays.asList(1, 4, 9), Arrays.asList(2, 5), Arrays.asList(3, 6, 7), Arrays.asList(0, 8));
    }

    // ------------------------------------------------------------------ C-001

    @Test
    public void mergeDoesNotOverflowTheStackOnManySources() {
        // Pre-fix this threw StackOverflowError from ~10,000 sources: each loop iteration wrapped the
        // previous merge iterator, so hasNext()/next() recursed once per source.
        final int n = 20_000;
        final List<Iterable<Integer>> sources = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            sources.add(Collections.singletonList(i));
        }

        final List<Integer> merged = N.merge(sources, ASCENDING);

        assertEquals(n, merged.size());
        assertEquals(0, merged.get(0).intValue());
        assertEquals(n - 1, merged.get(n - 1).intValue());
        assertTrue(CommonUtil.isSorted(merged));
    }

    @Test
    public void mergeTakesTheSourceCountFromTheIteratorNotFromSize() {
        // C-014. Iterators.merge/mergeIterables - the direct siblings on this same input shape - were
        // hardened against a lying size(); merge(Collection<Iterable>, ..) has to agree with them.
        final List<Iterable<Integer>> four = fourSources();

        // under-reported: isEmpty()/size()==1/size()==2 used to drop every source past the reported count
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), N.merge(new LyingSizeCollection<>(four, 0), ASCENDING));
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), N.merge(new LyingSizeCollection<>(four, 1), ASCENDING));
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), N.merge(new LyingSizeCollection<>(four, 2), ASCENDING));

        // over-reported: iter.next() used to run off the end of the two-source branch
        final List<Iterable<Integer>> one = Arrays.asList(Arrays.asList(3, 1));
        assertEquals(Arrays.asList(3, 1), N.merge(new LyingSizeCollection<>(one, 2), ASCENDING));
        assertEquals(Arrays.asList(3, 1), N.merge(new LyingSizeCollection<>(one, 7), ASCENDING));

        final List<Iterable<Integer>> none = Collections.emptyList();
        assertEquals(new ArrayList<>(), N.merge(new LyingSizeCollection<>(none, 5), ASCENDING));

        // and the capacity hint no longer comes from size(), so a bogus huge one allocates nothing
        final int[] asked = { -1 };
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), N.merge(new LyingSizeCollection<>(four, Integer.MAX_VALUE), ASCENDING, size -> {
            asked[0] = size;
            return new ArrayList<>(size);
        }));
        assertEquals(10, asked[0]);
    }

    @Test
    public void mergeAgreesWithIteratorsMergeIterablesElementForElementAndCallForCall() {
        // The hybrid claims the two folds are interchangeable. Iterators.mergeIterables always runs the
        // iterative fold, so agreeing with it - on the elements AND on the exact nextSelector call
        // sequence - is exactly the property that makes the internal switch unobservable. A selector that
        // ignores its arguments is used because a total order would hide any difference in fold shape.
        for (int n = 3; n <= 12; n++) {
            final List<Iterable<Integer>> sources = new ArrayList<>(n);

            for (int i = 0; i < n; i++) {
                sources.add(Arrays.asList(i, 100 + i, 200 + i));
            }

            for (final MergeResult fixed : new MergeResult[] { MergeResult.TAKE_FIRST, MergeResult.TAKE_SECOND }) {
                final StringBuilder mine = new StringBuilder();
                final List<Integer> mineOut = N.merge(sources, (x, y) -> {
                    mine.append(x).append(':').append(y).append(' ');
                    return fixed;
                });

                final StringBuilder theirs = new StringBuilder();
                final List<Integer> theirsOut = new ArrayList<>();
                Iterators.mergeIterables(sources, (Integer x, Integer y) -> {
                    theirs.append(x).append(':').append(y).append(' ');
                    return fixed;
                }).forEachRemaining(theirsOut::add);

                assertEquals(theirsOut, mineOut, "elements, n=" + n + " " + fixed);
                assertEquals(theirs.toString(), mine.toString(), "selector calls, n=" + n + " " + fixed);
            }
        }
    }

    @Test
    public void mergeAgreesAcrossTheNestedAndIterativePaths() {
        // The fix keeps the fast nested fold for small source counts and switches to an iterative one above
        // an internal threshold. Straddling that threshold must be unobservable. 999..1_002 brackets the
        // current threshold; if it ever moves, this still compares source counts one apart, which is the
        // property that matters.
        for (final int n : new int[] { 999, 1_000, 1_001, 1_002 }) {
            final List<Iterable<Integer>> sources = new ArrayList<>(n);

            for (int i = 0; i < n; i++) {
                sources.add(Arrays.asList(i, n + i));
            }

            final int[] selectorCalls = { 0 };
            final List<Integer> merged = N.merge(sources, (x, y) -> {
                selectorCalls[0]++;
                return x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
            });

            // an independent re-computation of the same result, path-agnostic
            final List<Integer> expected = new ArrayList<>();

            for (int i = 0; i < 2 * n; i++) {
                expected.add(i);
            }

            assertEquals(expected, merged, "n=" + n);
            assertTrue(selectorCalls[0] > 0, "n=" + n);
        }
    }

    @Test
    public void mergeKeepsTheSelectorSequenceWhenPaddedPastTheThreshold() {
        // The same four-source shape as the oracle test, padded with empty members so the *collected* source
        // count stays 4 while the outer collection crosses the threshold - proving the switch is driven by
        // real sources and that skipped members cost nothing.
        final List<Iterable<Integer>> padded = new ArrayList<>(fourSources());

        for (int i = 0; i < 2_000; i++) {
            padded.add(Collections.emptyList());
        }

        final StringBuilder calls = new StringBuilder();
        final List<Integer> merged = N.merge(padded, (x, y) -> {
            calls.append('(').append(x).append(',').append(y).append(')');
            return x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        });

        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), merged);
        assertEquals("(1,2)(1,3)(1,0)(1,8)(4,2)(2,3)(2,8)(4,5)(4,3)(3,8)(4,6)(4,8)(9,5)(5,6)(5,8)(9,6)(6,8)(9,7)(7,8)(9,8)", calls.toString());
    }

    @Test
    public void mergeKeepsExactElementOrderForThreeOrMoreSources() {
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), N.merge(fourSources(), ASCENDING));
    }

    @Test
    public void mergeKeepsTheExactNextSelectorCallSequence() {
        // Captured from the pre-fix build. The left fold is documented behaviour, so the fix must not
        // merely produce the same elements - it must ask the selector the same questions in the same
        // order, or a non-total-order selector would silently change meaning.
        final StringBuilder calls = new StringBuilder();

        N.merge(fourSources(), (x, y) -> {
            calls.append('(').append(x).append(',').append(y).append(')');
            return x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        });

        assertEquals("(1,2)(1,3)(1,0)(1,8)(4,2)(2,3)(2,8)(4,5)(4,3)(3,8)(4,6)(4,8)(9,5)(5,6)(5,8)(9,6)(6,8)(9,7)(7,8)(9,8)", calls.toString());
    }

    @Test
    public void mergeKeepsBehaviourForANonTotalOrderSelector() {
        // The shape of the fold is most visible when the selector ignores its arguments; this input
        // distinguishes a left fold from an n-way merge.
        final StringBuilder calls = new StringBuilder();

        final List<Integer> merged = N.merge(fourSources(), (x, y) -> {
            calls.append('(').append(x).append(',').append(y).append(')');
            return MergeResult.TAKE_SECOND;
        });

        assertEquals(Arrays.asList(0, 8, 3, 6, 7, 2, 5, 1, 4, 9), merged);
        assertEquals("(1,2)(2,3)(3,0)(3,8)(2,6)(2,7)(1,5)", calls.toString());
    }

    @Test
    public void mergeTraversesTheOuterCollectionExactlyOnce() {
        // A second pass would change observable behaviour for a one-shot outer collection.
        final TraversalCountingList<Iterable<Integer>> sources = new TraversalCountingList<>(fourSources());

        N.merge(sources, ASCENDING);

        assertEquals(1, sources.traversals);
    }

    @Test
    public void mergeSkipsNullAndEmptyMembers() {
        final List<Iterable<Integer>> sources = new ArrayList<>();
        sources.add(Arrays.asList(1, 5));
        sources.add(null);
        sources.add(Collections.emptyList());
        sources.add(Arrays.asList(2, 3));

        assertEquals(Arrays.asList(1, 2, 3, 5), N.merge(sources, ASCENDING));
    }

    @Test
    public void mergeBoundaryArities() {
        assertEquals(new ArrayList<>(), N.merge(new ArrayList<Iterable<Integer>>(), ASCENDING));
        assertEquals(Arrays.asList(3, 1), N.merge(Arrays.<Iterable<Integer>> asList(Arrays.asList(3, 1)), ASCENDING));
        assertEquals(Arrays.asList(1, 2, 3, 4), N.merge(Arrays.<Iterable<Integer>> asList(Arrays.asList(1, 4), Arrays.asList(2, 3)), ASCENDING));
        // all members null/empty
        final List<Iterable<Integer>> blank = new ArrayList<>();
        blank.add(null);
        blank.add(Collections.emptyList());
        blank.add(null);
        assertEquals(new ArrayList<>(), N.merge(blank, ASCENDING));

        // three outer members but only ONE survives the null/empty skip: the fold runs with a single
        // source, so nextSelector must never be consulted and the elements must pass through untouched
        final List<Iterable<Integer>> onlyOneReal = new ArrayList<>();
        onlyOneReal.add(null);
        onlyOneReal.add(Arrays.asList(3, 1, 2));
        onlyOneReal.add(Collections.emptyList());
        assertEquals(Arrays.asList(3, 1, 2), N.merge(onlyOneReal, (x, y) -> {
            throw new AssertionError("nextSelector must not be called for a single source");
        }));
    }

    @Test
    public void mergeAsksTheSupplierForTheCombinedSize() {
        final int[] asked = { -1 };

        N.merge(Arrays.<Iterable<Integer>> asList(Arrays.asList(1, 2), Arrays.asList(3), Arrays.asList(4, 5, 6)), ASCENDING, size -> {
            asked[0] = size;
            return new ArrayList<>(size);
        });

        assertEquals(6, asked[0]);
    }

    @Test
    public void mergeSupportsUnicodeElements() {
        // surrogate pairs must round-trip untouched through the fold
        final String rocket = "🚀";
        final String crab = "🦀";
        final List<Iterable<String>> sources = Arrays.asList(Arrays.asList("a", rocket), Arrays.asList("b"), Arrays.asList(crab));

        final List<String> merged = N.merge(sources, (x, y) -> x.compareTo(y) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);

        assertEquals(4, merged.size());
        assertTrue(merged.contains(rocket));
        assertTrue(merged.contains(crab));
    }

    // ------------------------------------------------------------------ C-002

    @Test
    public void removeAtKeepsTheListIntactWhenRepopulationFails() {
        final FailingAddAllList<String> list = new FailingAddAllList<>(Arrays.asList("a", "b", "c", "d", "e"));
        list.armFailures(1);

        assertThrows(IllegalStateException.class, () -> N.removeAt(list, 0, 2));
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), new ArrayList<>(list));
    }

    @Test
    public void removeRangeKeepsTheListIntactWhenRepopulationFails() {
        // span > 3 and not a LinkedList, so this takes the clear+rebuild branch. Pre-fix the failing addAll
        // left the caller holding an EMPTY list; the removal must now either happen or not happen at all.
        final FailingAddAllList<String> list = new FailingAddAllList<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g"));
        list.armFailures(1);

        assertThrows(IllegalStateException.class, () -> N.removeRange(list, 1, 6));
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f", "g"), new ArrayList<>(list));
    }

    @Test
    public void removeRangeStillUsesItsSubListGatedPaths() {
        // The gate is deliberately unchanged: a short span and a LinkedList go through subList().clear(),
        // which never calls addAll, so the armed failure must never fire on those paths.
        final FailingAddAllList<String> shortSpan = new FailingAddAllList<>(Arrays.asList("a", "b", "c", "d", "e"));
        shortSpan.armFailures(1);

        assertTrue(N.removeRange(shortSpan, 1, 4));
        assertEquals(Arrays.asList("a", "e"), new ArrayList<>(shortSpan));

        final LinkedList<String> linked = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g"));

        assertTrue(N.removeRange(linked, 1, 6));
        assertEquals(Arrays.asList("a", "g"), new ArrayList<>(linked));
    }

    @Test
    public void removeRangeStillWorksOnANonRandomAccessListWithoutRemoveRange() {
        // ExoticList does not override removeRange and is not RandomAccess. Routing every List through
        // subList().clear() would have dropped it onto AbstractList's quadratic remove loop; it stays on the
        // array-rebuild path and must produce exactly the same content.
        final ExoticList<String> list = new ExoticList<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g"));

        assertTrue(N.removeRange(list, 1, 6));
        assertEquals(Arrays.asList("a", "g"), new ArrayList<>(list));
        assertFalse(N.removeRange(list, 1, 1));
        assertEquals(Arrays.asList("a", "g"), new ArrayList<>(list));
    }

    @Test
    public void removeRangeBoundaries() {
        assertFalse(N.removeRange(new ArrayList<>(Arrays.asList("a", "b")), 1, 1));

        final List<String> whole = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        assertTrue(N.removeRange(whole, 0, 5));
        assertTrue(whole.isEmpty());

        final List<String> head = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        assertTrue(N.removeRange(head, 0, 4));
        assertEquals(Arrays.asList("e"), head);

        final List<String> tail = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        assertTrue(N.removeRange(tail, 1, 5));
        assertEquals(Arrays.asList("a"), tail);

        final List<String> withNulls = new ArrayList<>(Arrays.asList("a", null, null, null, null, "z"));
        assertTrue(N.removeRange(withNulls, 1, 5));
        assertEquals(Arrays.asList("a", "z"), withNulls);
    }

    @Test
    public void replaceRangeKeepsTheListIntactWhenRepopulationFails() {
        final FailingAddAllList<String> list = new FailingAddAllList<>(Arrays.asList("a", "b", "c", "d", "e"));
        list.armFailures(1);

        assertThrows(IllegalStateException.class, () -> N.replaceRange(list, 1, 3, Arrays.asList("X", "Y", "Z")));
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), new ArrayList<>(list));
    }

    @Test
    public void replaceRangeKeepsTheTailWhenTheReplacementFails() {
        // The pre-fix shape removed [fromIndex, size) first and only then appended the replacement and the
        // saved tail, so a failure in between left a truncated list AND discarded the tail entirely.
        final FailingAddAllList<String> list = new FailingAddAllList<>(Arrays.asList("a", "b", "c", "d", "e"));
        list.armFailures(1);

        assertThrows(IllegalStateException.class, () -> N.replaceRange(list, 1, 2, Arrays.asList("X", "Y", "Z")));
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), new ArrayList<>(list));
    }

    @Test
    public void replaceRangeWithAnEqualSizedReplacementStillWorksOnAFixedSizeList() {
        // The equal-size path writes through set()/ListIterator.set() and must keep working on a list that
        // supports set but not add/remove - it never reaches the clear()+addAll() rebuild.
        final List<String> fixed = Arrays.asList("a", "b", "c", "d");

        assertTrue(N.replaceRange(fixed, 1, 3, Arrays.asList("X", "Y")));
        assertEquals(Arrays.asList("a", "X", "Y", "d"), fixed);

        // ... and a size-changing replacement on the same list is still rejected rather than half-applied
        final List<String> fixed2 = Arrays.asList("a", "b", "c", "d");
        assertThrows(UnsupportedOperationException.class, () -> N.replaceRange(fixed2, 1, 3, Arrays.asList("X")));
        assertEquals(Arrays.asList("a", "b", "c", "d"), fixed2);
    }

    @Test
    public void replaceRangeBoundaries() {
        final List<String> grow = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertTrue(N.replaceRange(grow, 1, 2, Arrays.asList("X", "Y", "Z")));
        assertEquals(Arrays.asList("a", "X", "Y", "Z", "c"), grow);

        final List<String> shrink = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        assertTrue(N.replaceRange(shrink, 1, 4, Arrays.asList("X")));
        assertEquals(Arrays.asList("a", "X", "e"), shrink);

        final List<String> insert = new ArrayList<>(Arrays.asList("a", "b"));
        assertTrue(N.replaceRange(insert, 2, 2, Arrays.asList("X", "Y")));
        assertEquals(Arrays.asList("a", "b", "X", "Y"), insert);

        final List<String> whole = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertTrue(N.replaceRange(whole, 0, 3, Arrays.asList("X")));
        assertEquals(Arrays.asList("X"), whole);

        // an empty replacement degenerates to removeRange; an empty range with it is a no-op
        final List<String> emptyRepl = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e", "f"));
        assertTrue(N.replaceRange(emptyRepl, 1, 5, Collections.<String> emptyList()));
        assertEquals(Arrays.asList("a", "f"), emptyRepl);
        assertFalse(N.replaceRange(emptyRepl, 1, 1, Collections.<String> emptyList()));

        // nulls and non-BMP text survive the rebuild
        final String rocket = "\uD83D\uDE80";
        final String crab = "\uD83E\uDD80";
        final List<String> unicode = new ArrayList<>(Arrays.asList("a", null, "c"));
        assertTrue(N.replaceRange(unicode, 1, 2, Arrays.asList(rocket, null, crab)));
        assertEquals(Arrays.asList("a", rocket, null, crab, "c"), unicode);
    }

    @Test
    public void removeDuplicatesKeepsTheCollectionIntactWhenRepopulationFails() {
        final FailingAddAllList<String> list = new FailingAddAllList<>(Arrays.asList("a", "b", "a", "c", "b"));
        list.armFailures(1);

        assertThrows(IllegalStateException.class, () -> N.removeDuplicates(list));
        assertEquals(Arrays.asList("a", "b", "a", "c", "b"), new ArrayList<>(list));
    }

    @Test
    public void removeDuplicatesRollsBackANonListCollection() {
        // A Deque is the reachable non-List, non-Set case: it also goes through the clear()+addAll() rebuild.
        final FailingAddAllDeque<String> deque = new FailingAddAllDeque<>(Arrays.asList("a", "b", "a", "c", "b"));
        deque.armFailures(1);

        assertThrows(IllegalStateException.class, () -> N.removeDuplicates(deque));
        assertEquals(Arrays.asList("a", "b", "a", "c", "b"), new ArrayList<>(deque));

        // and it still de-duplicates, in encounter order, when nothing fails
        final ArrayDeque<String> ok = new ArrayDeque<>(Arrays.asList("a", "b", "a", "c", "b"));
        assertTrue(N.removeDuplicates(ok));
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(ok));

        // no duplicates -> untouched, and no rebuild at all
        final ArrayDeque<String> distinct = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
        assertFalse(N.removeDuplicates(distinct));
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(distinct));
    }

    // ------------------------------------------------------------------ C-016

    @Test
    public void probeUnmodifiableThrowsWhenItCannotUndoItsOwnProbe() {
        // C-016. The @return clause reads as "anything unexpected just means false", but a container that
        // ACCEPTS the probe and then refuses to give it back leaves the method unable to answer: it throws
        // IllegalStateException wrapping the rollback failure, and the collection keeps the probe element.
        // That is now documented on both overloads; this pins it.
        final UnremovableSet<Object> set = new UnremovableSet<>();
        set.add("a");

        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CommonUtil.probeUnmodifiable(set));

        assertNotNull(ex.getCause());
        assertEquals(UnsupportedOperationException.class, ex.getCause().getClass());
        // the probe element really is still in there - the reason the method cannot just answer "false"
        assertEquals(2, set.size());
    }

    @Test
    public void reverseRotateAndShuffleStillRollBack() {
        // the pre-existing rollback (replaceElements) must keep working
        for (final String op : new String[] { "reverse", "rotate", "shuffle" }) {
            final FailingAddAllSet<String> set = new FailingAddAllSet<>(Arrays.asList("a", "b", "c"));
            set.armFailures(1);

            assertThrows(IllegalStateException.class, () -> {
                switch (op) {
                    case "reverse" -> CommonUtil.reverse(set);
                    case "rotate" -> CommonUtil.rotate(set, 1);
                    default -> CommonUtil.shuffle(set);
                }
            }, op);

            assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(set), op);
        }
    }

    @Test
    public void destructiveHelpersStillBehaveOnTheHappyPath() {
        final List<String> a = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        assertTrue(N.removeAt(a, 1, 3));
        assertEquals(Arrays.asList("A", "C", "E"), a);

        final List<String> b = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E", "F", "G"));
        assertTrue(N.removeRange(b, 1, 6));
        assertEquals(Arrays.asList("A", "G"), b);

        final List<String> c = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        assertTrue(N.replaceRange(c, 1, 3, Arrays.asList("x", "y")));
        assertEquals(Arrays.asList("a", "x", "y", "d", "e"), c);

        final List<Integer> d = new ArrayList<>(Arrays.asList(5, 2, 8, 2, 5));
        assertTrue(N.removeDuplicates(d));
        assertEquals(Arrays.asList(5, 2, 8), d);
    }

    @Test
    public void removeRangeBoundariesAndUnsupportedLists() {
        final List<String> empty = new ArrayList<>();
        assertFalse(N.removeRange(empty, 0, 0));
        assertEquals(new ArrayList<>(), empty);

        final List<String> one = new ArrayList<>(Arrays.asList("only"));
        assertTrue(N.removeRange(one, 0, 1));
        assertTrue(one.isEmpty());

        final List<String> whole = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        assertTrue(N.removeRange(whole, 0, 5));
        assertTrue(whole.isEmpty());

        final List<String> linked = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));
        assertTrue(N.removeRange(linked, 1, 4));
        assertEquals(Arrays.asList("a", "e"), linked);

        // a fixed-size list must fail without losing its content
        final List<String> fixed = Arrays.asList("a", "b", "c", "d", "e");
        assertThrows(UnsupportedOperationException.class, () -> N.removeRange(fixed, 1, 4));
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), fixed);
    }

    @Test
    public void removeDuplicatesHandlesUnicodeAndEmptyInput() {
        final String rocket = "🚀";
        final List<String> list = new ArrayList<>(Arrays.asList(rocket, "a", rocket, "é", "é"));
        assertTrue(N.removeDuplicates(list));
        assertEquals(Arrays.asList(rocket, "a", "é"), list);

        final List<String> none = new ArrayList<>();
        assertFalse(N.removeDuplicates(none));

        final List<String> single = new ArrayList<>(Arrays.asList("x"));
        assertFalse(N.removeDuplicates(single));
        assertEquals(Arrays.asList("x"), single);
    }

    // ------------------------------------------------------------------ C-003

    @Test
    public void newInstanceReportsAMissingConstructorAsIllegalArgument() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(MemberWithoutNoArgCtor.class));
        assertNotNull(e.getMessage());
        assertTrue(e.getMessage().contains("constructor"), e.getMessage());
    }

    @Test
    public void newInstanceStillWorksForOrdinaryClasses() {
        assertNotNull(CommonUtil.newInstance(ArrayList.class));
        assertNotNull(CommonUtil.newInstance(java.util.HashMap.class));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(Runnable.class));
    }

    // ------------------------------------------------------------------ C-004

    @Test
    public void checkFromToIndexRendersAnExclusiveUpperBound() {
        final IndexOutOfBoundsException e = assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(2, 6, 5));
        assertTrue(e.getMessage().contains("[2, 6)"), e.getMessage());
    }

    // ------------------------------------------------------------------ C-005

    @Test
    public void rangeGuardsStillShortCircuitOnEmptyInputAndEmptyRanges() {
        final List<String> empty = new ArrayList<>();
        assertEquals(new ArrayList<>(), N.map(empty, 0, 0, String::length));
        assertEquals(0, N.mapToInt(empty, 0, 0, String::length).length);
        assertEquals(new ArrayList<>(), N.distinct(empty, 0, 0));

        final List<String> three = Arrays.asList("a", "bb", "ccc");
        assertEquals(new ArrayList<>(), N.map(three, 1, 1, String::length));
        assertEquals(0, N.mapToInt(three, 2, 2, String::length).length);
        assertEquals(Arrays.asList(2, 3), N.map(three, 1, 3, String::length));
        assertEquals(new ArrayList<>(), N.distinct(three, 1, 1));
    }
}
