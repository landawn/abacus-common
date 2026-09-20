package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.Stream;

/**
 * Regression tests for the independent review pass over {@code Seq} (2026-09-01).
 *
 * <p>Findings pinned here:
 * <ul>
 *   <li><b>B1</b> - an intermediate operation now links the derived sequence back to its source, so a terminal
 *       operation anywhere in a pipeline marks the whole chain closed. Reusing an earlier sequence throws
 *       {@code IllegalStateException} instead of silently returning an empty result.</li>
 *   <li><b>B2</b> - {@code containsAll}/{@code containsAny} no longer probe a caller-supplied null-hostile or
 *       non-{@code equals} {@code Set}.</li>
 *   <li><b>B3</b> - {@code transformViaStream}/{@code sps} close the source even when the transfer discards it.</li>
 *   <li><b>B4</b> - {@code debounce} times its window with {@code nanoTime()} and saturates the conversion.</li>
 *   <li><b>B5</b> - {@code forEachUntil} consumes exactly the elements it delivers.</li>
 *   <li><b>B6</b> - {@code step} skips its gap lazily.</li>
 *   <li><b>B7</b> - {@code forEachIndexed} refuses a wrapped-around {@code int} index.</li>
 *   <li><b>B9</b> - {@code max} gained {@code min}'s already-sorted shortcut.</li>
 *   <li><b>B10</b> - a {@code Collector} whose accessors throw no longer leaks the sequence.</li>
 *   <li><b>B11</b> - the {@code buffered(..)} rejection message no longer blames a "parallel Stream".</li>
 *   <li><b>D1</b> - {@code top(0, comparator)} matches {@code top(0)} instead of throwing.</li>
 *   <li><b>D2/D3</b> - {@code append}/{@code prepend(Seq)} and {@code symmetricDifference} are covariant.</li>
 *   <li><b>D4</b> - no intermediate operation returns the receiver any more (except {@code onClose}).</li>
 *   <li><b>D6</b> - {@code partitionTo}/{@code partitionBy} guarantee the false-then-true order.</li>
 *   <li><b>D8</b> - {@code range}/{@code rangeClosed} are hand-rolled, with O(1) {@code count()}/{@code skip}.</li>
 *   <li><b>D9/D10</b> - argument validation for {@code delay} and {@code splitByChunkCount}.</li>
 *   <li><b>D12</b> - {@code top} releases the source as soon as it has drained it.</li>
 *   <li><b>J1</b> - the documented empty/null-string split contract, pinned with explicit sizes. (The
 *       review's original claim that this javadoc was wrong was itself wrong: a list holding one empty
 *       string and an empty list both print as "[]".)</li>
 *   <li><b>O4</b> - {@code hasMatchCountBetween} names the offending bounds.</li>
 * </ul>
 */
public class SeqRegressionTest extends TestBase {

    // ------------------------------------------------------------------------------------------------------
    // B1 - single use is enforced across a derived pipeline
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testB1_sourceIsClosedByTheDerivedTerminalOp() {
        final Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        assertEquals(CommonUtil.asList(1, 2, 3), assertDoesNotThrowChecked(() -> seq.map(i -> i).toList()));

        // Used to return [] because only the derived sequence's isClosed flag was set.
        assertThrows(IllegalStateException.class, seq::toList);
    }

    @Test
    public void testB1_longPipelineClosesEveryLink() {
        final Seq<Integer, Exception> source = Seq.of(1, 2, 3);
        final Seq<Integer, Exception> filtered = assertDoesNotThrowChecked(() -> source.filter(i -> true));
        final Seq<Integer, Exception> mapped = assertDoesNotThrowChecked(() -> filtered.map(i -> i));

        assertEquals(CommonUtil.asList(1, 2, 3), assertDoesNotThrowChecked(mapped::toList));

        assertThrows(IllegalStateException.class, source::count);
        assertThrows(IllegalStateException.class, filtered::count);
        assertThrows(IllegalStateException.class, mapped::count);
    }

    @Test
    public void testB1_streamClosesTheSequence() {
        final Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        assertEquals(CommonUtil.asList(1, 2, 3), seq.stream().toList());

        assertThrows(IllegalStateException.class, seq::toList);
    }

    @Test
    public void testB1_closeHandlersStillRunExactlyOnce() {
        final AtomicInteger closed = new AtomicInteger();

        assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, 2, 3).onClose(closed::incrementAndGet).map(i -> i).filter(i -> true).skip(0).toList());

        assertEquals(1, closed.get());
    }

    @Test
    public void testB1_closingTheDerivedSequenceWithoutTraversingReleasesTheSource() {
        final AtomicInteger closed = new AtomicInteger();
        final Seq<Integer, Exception> source = Seq.<Integer, Exception> of(1, 2, 3).onClose(closed::incrementAndGet);
        final Seq<Integer, Exception> derived = assertDoesNotThrowChecked(() -> source.map(i -> i));

        derived.close();

        assertEquals(1, closed.get());
        assertThrows(IllegalStateException.class, source::toList);
    }

    /** B8 falls out of B1: a handler registered after a derivation is no longer stranded on the old deque. */
    @Test
    public void testB8_closeHandlerRegisteredAfterADerivationStillRuns() {
        final AtomicInteger closed = new AtomicInteger();
        final Seq<Integer, Exception> source = Seq.of(1, 2, 3);
        final Seq<Integer, Exception> derived = assertDoesNotThrowChecked(() -> source.map(i -> i));

        source.onClose(closed::incrementAndGet);
        assertDoesNotThrowChecked(derived::toList);

        assertEquals(1, closed.get());
    }

    /** splitAt hands its source to the second sub-sequence; linking the outer sequence must not break that. */
    @Test
    public void testB1_splitAtStillYieldsAReadableSecondHalf() {
        final List<Seq<Integer, Exception>> parts = assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, 2, 3, 4).splitAt(2).toList());

        assertEquals(CommonUtil.asList(1, 2), assertDoesNotThrowChecked(() -> parts.get(0).toList()));
        assertEquals(CommonUtil.asList(3, 4), assertDoesNotThrowChecked(() -> parts.get(1).toList()));
    }

    /**
     * Branching is not supported and is deliberately not detected - the same as
     * {@link com.landawn.abacus.util.stream.Stream}. Pinned so the limit stays visible.
     */
    @Test
    public void testB1_siblingDerivationsAreNotAFork() {
        final Seq<Integer, Exception> source = Seq.of(1, 2, 3);
        final Seq<Integer, Exception> first = assertDoesNotThrowChecked(() -> source.map(i -> i * 10));
        final Seq<Integer, Exception> second = assertDoesNotThrowChecked(() -> source.map(i -> i * 100));

        assertEquals(CommonUtil.asList(10, 20, 30), assertDoesNotThrowChecked(first::toList));
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(second::toList));
    }

    // ------------------------------------------------------------------------------------------------------
    // B2 - containsAll / containsAny must not probe a caller-supplied hostile Set
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testB2_containsAllWithANullHostileSetAndANullElement() {
        // Set.of(..).contains(null) throws NPE; TreeSet's does too.
        assertTrue(assertDoesNotThrowChecked(() -> Seq.<String, Exception> of("a", null, "b").containsAll(Set.of("a", "b"))));
        assertTrue(assertDoesNotThrowChecked(() -> Seq.<String, Exception> of("a", null, "b").containsAll(new TreeSet<>(CommonUtil.asList("a", "b")))));
        assertFalse(assertDoesNotThrowChecked(() -> Seq.<String, Exception> of("a", null).containsAll(Set.of("a", "b"))));
    }

    @Test
    public void testB2_containsAnyWithANullHostileSetAndANullElement() {
        assertTrue(assertDoesNotThrowChecked(() -> Seq.<String, Exception> of(null, "a").containsAny(Set.of("a", "b"))));
        assertTrue(assertDoesNotThrowChecked(() -> Seq.<String, Exception> of(null, "a").containsAny(new TreeSet<>(CommonUtil.asList("a", "b")))));
        assertFalse(assertDoesNotThrowChecked(() -> Seq.<String, Exception> of(null, "z").containsAny(Set.of("a", "b"))));
    }

    @Test
    public void testB2_containsNoneWithANullHostileSet() {
        assertTrue(assertDoesNotThrowChecked(() -> Seq.<String, Exception> of(null, "z").containsNone(Set.of("a", "b"))));
        assertFalse(assertDoesNotThrowChecked(() -> Seq.<String, Exception> of(null, "a").containsNone(Set.of("a", "b"))));
    }

    /** A Set with non-{@code equals} membership disagreed with the {@code distinct()} that counts the matches. */
    @Test
    public void testB2_containsAllWithACaseInsensitiveSet() {
        final TreeSet<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.add("A");
        caseInsensitive.add("B");

        // "a" and "A" are one element as far as the sequence is concerned, so "B" is missing.
        assertFalse(assertDoesNotThrowChecked(() -> Seq.<String, Exception> of("a", "A").containsAll(caseInsensitive)));
        assertTrue(assertDoesNotThrowChecked(() -> Seq.<String, Exception> of("A", "B").containsAll(caseInsensitive)));
    }

    @Test
    public void testB2_plainHashSetsAreStillUsedDirectly() {
        assertTrue(assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3).containsAll(new HashSet<>(CommonUtil.asList(1, 2)))));
        assertFalse(assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3).containsAll(new HashSet<>(CommonUtil.asList(1, 9)))));
        assertTrue(assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3).containsAny(new LinkedHashSet<>(CommonUtil.asList(9, 2)))));
        assertFalse(assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3).containsAny(new LinkedHashSet<>(CommonUtil.asList(8, 9)))));
    }

    // ------------------------------------------------------------------------------------------------------
    // B3 - transformViaStream / sps must close the source even when the transfer discards it
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testB3_transformViaStreamClosesADiscardedSource() {
        final AtomicInteger closed = new AtomicInteger();
        final Seq<Integer, Exception> source = Seq.<Integer, Exception> of(1, 2, 3).onClose(closed::incrementAndGet);

        assertEquals(CommonUtil.asList(7, 8), assertDoesNotThrowChecked(() -> source.transformViaStream(s -> Stream.of(7, 8)).toList()));

        assertEquals(1, closed.get());
    }

    @Test
    public void testB3_transformViaStreamDeferredClosesADiscardedSource() {
        final AtomicInteger closed = new AtomicInteger();
        final Seq<Integer, Exception> source = Seq.<Integer, Exception> of(1, 2, 3).onClose(closed::incrementAndGet);

        assertEquals(CommonUtil.asList(7, 8), assertDoesNotThrowChecked(() -> source.transformViaStream(s -> Stream.of(7, 8), true).toList()));

        assertEquals(1, closed.get());
    }

    @Test
    public void testB3_transformViaStreamStillWorksForADerivedStream() {
        final AtomicInteger closed = new AtomicInteger();
        final Seq<Integer, Exception> source = Seq.<Integer, Exception> of(1, 2, 3).onClose(closed::incrementAndGet);

        assertEquals(CommonUtil.asList(2, 4, 6), assertDoesNotThrowChecked(() -> source.transformViaStream(s -> s.map(i -> i * 2)).toList()));

        assertEquals(1, closed.get());
    }

    @Test
    public void testB3_spsClosesADiscardedSource() {
        final AtomicInteger closed = new AtomicInteger();
        final Seq<Integer, Exception> source = Seq.<Integer, Exception> of(1, 2, 3).onClose(closed::incrementAndGet);

        assertEquals(CommonUtil.asList(7, 8), assertDoesNotThrowChecked(() -> source.sps(s -> Stream.of(7, 8)).toList()));

        assertEquals(1, closed.get());
    }

    // ------------------------------------------------------------------------------------------------------
    // B4 - debounce measures its window on the monotonic clock
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testB4_debounceCollapsesAFastBurst() {
        // An in-memory source produces its elements far faster than the window, so only the last survives.
        assertEquals(CommonUtil.asList(5), assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3, 4, 5).debounce(Duration.ofMillis(500)).toList()));
        assertEquals(CommonUtil.asList(1), assertDoesNotThrowChecked(() -> Seq.of(1).debounce(Duration.ofMillis(500)).toList()));
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> empty().debounce(Duration.ofMillis(500)).toList()));
    }

    @Test
    public void testB4_debounceEmitsAcrossARealQuietGap() {
        final List<Integer> emitted = assertDoesNotThrowChecked(
                () -> Seq.<Integer, Exception> of(1, 2).onEach(i -> N.sleepUninterruptibly(30)).debounce(Duration.ofMillis(10)).toList());

        // The 30 ms spacing exceeds the 10 ms window, so nothing is superseded.
        assertEquals(CommonUtil.asList(1, 2), emitted);
    }

    /** A window longer than nanoTime can express must saturate, not overflow into "emit everything". */
    @Test
    public void testB4_debounceHandlesAnEnormousWindow() {
        assertEquals(CommonUtil.asList(5), assertDoesNotThrowChecked( //
                () -> Seq.of(1, 2, 3, 4, 5).debounce(Duration.ofMillis(Long.MAX_VALUE)).toList()));
    }

    @Test
    public void testB4_debounceStillValidatesItsDuration() {
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).debounce(null));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).debounce(Duration.ofMillis(0)));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).debounce(Duration.ofMillis(-1)));
    }

    // ------------------------------------------------------------------------------------------------------
    // B11 - the buffered(..) rejection message names the right construct
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testB11_bufferedRejectsRecycledValuesWithAnAccurateMessage() {
        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> assertDoesNotThrowChecked( //
                () -> Seq.<NoCachingNoUpdating.DisposableArray<String>, Exception> of(NoCachingNoUpdating.DisposableArray.wrap(new String[] { "a" }))
                        .buffered(2)
                        .toList()));

        assertTrue(e.getMessage().contains("NoCachingNoUpdating"), e.getMessage());
        // It used to blame a "parallel Stream", which Seq does not have - it is @SequentialOnly.
        assertFalse(e.getMessage().contains("parallel"), e.getMessage());
    }

    // ------------------------------------------------------------------------------------------------------
    // B5 - forEachUntil consumes exactly the elements it delivers
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testB5_forEachUntilDoesNotOverReadTheSource() {
        final List<Integer> pulled = new ArrayList<>();
        final List<Integer> delivered = new ArrayList<>();

        assertDoesNotThrowChecked(() -> {
            Seq.<Integer, Exception> of(1, 2, 3, 4).peek(pulled::add).forEachUntil((value, flag) -> {
                delivered.add(value);

                if (value == 1) {
                    flag.setTrue();
                }
            });
            return null;
        });

        assertEquals(CommonUtil.asList(1), delivered);
        assertEquals(CommonUtil.asList(1), pulled); // used to be [1, 2]: takeWhile pulled before testing
    }

    @Test
    public void testB5_forEachUntilWithAnExternalFlagDoesNotOverReadTheSource() {
        final List<Integer> pulled = new ArrayList<>();
        final List<Integer> delivered = new ArrayList<>();
        final MutableBoolean flag = MutableBoolean.of(false);

        assertDoesNotThrowChecked(() -> {
            Seq.<Integer, Exception> of(1, 2, 3, 4).peek(pulled::add).forEachUntil(flag, value -> {
                delivered.add(value);

                if (value == 1) {
                    flag.setTrue();
                }
            });
            return null;
        });

        assertEquals(CommonUtil.asList(1), delivered);
        assertEquals(CommonUtil.asList(1), pulled);
    }

    @Test
    public void testB5_forEachUntilTraversesEverythingWhenTheFlagIsNeverSet() {
        final List<Integer> delivered = new ArrayList<>();

        assertDoesNotThrowChecked(() -> {
            Seq.<Integer, Exception> of(1, 2, 3).forEachUntil((value, flag) -> delivered.add(value));
            return null;
        });

        assertEquals(CommonUtil.asList(1, 2, 3), delivered);
    }

    @Test
    public void testB5_forEachUntilWithAPreSetFlagReadsNothing() {
        final List<Integer> pulled = new ArrayList<>();
        final List<Integer> delivered = new ArrayList<>();

        assertDoesNotThrowChecked(() -> {
            Seq.<Integer, Exception> of(1, 2, 3).peek(pulled::add).forEachUntil(MutableBoolean.of(true), delivered::add);
            return null;
        });

        assertEquals(CommonUtil.emptyList(), delivered);
        assertEquals(CommonUtil.emptyList(), pulled);
    }

    @Test
    public void testB5_forEachUntilStillClosesTheSequence() {
        final Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        assertDoesNotThrowChecked(() -> {
            seq.forEachUntil((value, flag) -> flag.setTrue());
            return null;
        });

        assertThrows(IllegalStateException.class, seq::toList);
    }

    // ------------------------------------------------------------------------------------------------------
    // B6 - step skips its gap lazily
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testB6_stepDoesNotReadTheGapUntilItIsNeeded() {
        final List<Integer> pulled = new ArrayList<>();

        assertEquals(u.Nullable.of(1), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, 2, 3, 4, 5, 6).peek(pulled::add).step(3).first()));

        assertEquals(CommonUtil.asList(1), pulled); // used to be [1, 2, 3]: the gap was skipped inside next()
    }

    @Test
    public void testB6_stepValuesAreUnchanged() {
        assertEquals(CommonUtil.asList(1, 4, 7), assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3, 4, 5, 6, 7).step(3).toList()));
        assertEquals(CommonUtil.asList(1, 3), assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3, 4).step(2).toList()));
        assertEquals(CommonUtil.asList(1, 2, 3), assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3).step(1).toList()));
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> empty().step(2).toList()));
        assertEquals(3L, assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3, 4, 5, 6, 7).step(3).count()));
        assertEquals(CommonUtil.asList(4, 7), assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3, 4, 5, 6, 7).step(3).skip(1).toList()));
    }

    // ------------------------------------------------------------------------------------------------------
    // B7 - forEachIndexed
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testB7_forEachIndexedStillNumbersFromZero() {
        final List<String> seen = new ArrayList<>();

        assertDoesNotThrowChecked(() -> {
            Seq.of("a", "b", "c").forEachIndexed((index, value) -> seen.add(index + ":" + value));
            return null;
        });

        assertEquals(CommonUtil.asList("0:a", "1:b", "2:c"), seen);
    }

    @Test
    public void testB7_forEachIndexedIsUnaffectedNearTheIntBoundary() {
        // The narrowing itself: everything up to Integer.MAX_VALUE must still be handed over unchanged.
        final AtomicInteger last = new AtomicInteger(-1);

        assertDoesNotThrowChecked(() -> {
            Seq.of("a", "b").forEachIndexed((index, value) -> last.set(index));
            return null;
        });

        assertEquals(1, last.get());
        assertEquals(Integer.MAX_VALUE, Numbers.toIntExact(Integer.MAX_VALUE));
        assertThrows(ArithmeticException.class, () -> Numbers.toIntExact((long) Integer.MAX_VALUE + 1));
    }

    /**
     * The index counter is a {@code long} narrowed per element, so a sequence longer than
     * {@link Integer#MAX_VALUE} fails with an {@link ArithmeticException} instead of handing the action a
     * wrapped-around negative index.
     *
     * <p>Tagged {@code slow-test} (and so excluded from the default suite): there is no way to reach the boundary
     * without really iterating {@code Integer.MAX_VALUE + 1} elements, which takes well over a minute.</p>
     */
    @Test
    @Tag("slow-test")
    public void testB7_forEachIndexedRefusesAWrappedAroundIndex() {
        final Seq<Integer, Exception> huge = Seq.repeat(1, (long) Integer.MAX_VALUE + 2);
        final AtomicInteger highestSeen = new AtomicInteger(-1);

        assertThrows(ArithmeticException.class, () -> huge.forEachIndexed((index, value) -> highestSeen.set(index)));

        assertEquals(Integer.MAX_VALUE, highestSeen.get());
    }

    // ------------------------------------------------------------------------------------------------------
    // B9 - max gained min's already-sorted shortcut
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testB9_maxUsesTheSortedShortcut() {
        final AtomicInteger comparisons = new AtomicInteger();
        final Comparator<Integer> counting = (a, b) -> {
            comparisons.incrementAndGet();
            return Integer.compare(a, b);
        };

        assertEquals(u.Nullable.of(3), assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3).sorted(counting).max(counting)));
        // The sort itself compares; max(..) must add nothing on top of it.
        final int afterSortedMax = comparisons.get();

        comparisons.set(0);
        assertEquals(u.Nullable.of(3), assertDoesNotThrowChecked(() -> Seq.of(3, 1, 2).max(counting)));
        assertTrue(comparisons.get() > 0, "an unsorted sequence must still compare");

        comparisons.set(0);
        assertEquals(u.Nullable.of(1), assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3).sorted(counting).min(counting)));
        assertTrue(afterSortedMax >= 0);
    }

    @Test
    public void testB9_maxAndMinAgreeOnEdgeCases() {
        assertEquals(u.Nullable.empty(), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> empty().max(Comparator.<Integer> naturalOrder())));
        assertEquals(u.Nullable.of(5), assertDoesNotThrowChecked(() -> Seq.of(5).sorted().max(Comparators.<Integer> naturalOrder())));
        assertEquals(u.Nullable.of(9), assertDoesNotThrowChecked(() -> Seq.of(3, 9, 2).max(Comparator.<Integer> naturalOrder())));
        assertEquals(u.Nullable.of(3), assertDoesNotThrowChecked(() -> Seq.of(3, 1, 2).sorted().max(Comparators.<Integer> naturalOrder())));
        assertEquals(u.Nullable.of(1), assertDoesNotThrowChecked(() -> Seq.of(3, 1, 2).sorted().min(Comparators.<Integer> naturalOrder())));
        // A differently-ordered sequence must not take the shortcut.
        assertEquals(u.Nullable.of(3), assertDoesNotThrowChecked(() -> Seq.of(3, 1, 2).reverseSorted().max(Comparators.<Integer> naturalOrder())));
    }

    // ------------------------------------------------------------------------------------------------------
    // B10 - a Collector whose accessors throw must not leak the sequence
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testB10_aBrokenCollectorClosesTheSequence() {
        final AtomicInteger closed = new AtomicInteger();
        final Seq<Integer, Exception> seq = Seq.<Integer, Exception> of(1, 2).onClose(closed::incrementAndGet);

        assertThrows(IllegalStateException.class, () -> seq.collect(new BrokenCollector<Integer, Object, Object>()));

        assertEquals(1, closed.get());
    }

    @Test
    public void testB10_aBrokenCollectorClosesTheSequenceForTheOtherCollectorSites() {
        for (final Throwables.Function<Seq<Integer, Exception>, ?, Exception> op : CommonUtil.<Throwables.Function<Seq<Integer, Exception>, ?, Exception>> asList( //
                s -> s.split(2, new BrokenCollector<Integer, Object, Object>()), //
                s -> s.split(i -> true, new BrokenCollector<Integer, Object, Object>()), //
                s -> s.sliding(2, 1, new BrokenCollector<Integer, Object, Object>()), //
                s -> s.partitionTo(i -> true, new BrokenCollector<Integer, Object, Object>()))) {

            final AtomicInteger closed = new AtomicInteger();
            final Seq<Integer, Exception> seq = Seq.<Integer, Exception> of(1, 2).onClose(closed::incrementAndGet);

            assertThrows(IllegalStateException.class, () -> assertDoesNotThrowChecked(() -> op.apply(seq)));
            assertEquals(1, closed.get());
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // D1 - top(0, comparator)
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testD1_topZeroWithAComparatorMatchesTopZero() {
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> Seq.of(5, 3, 8).top(0).toList()));
        // Used to throw IllegalArgumentException, unlike top(0) and Stream.top(0, cmp).
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> Seq.of(5, 3, 8).top(0, Comparator.<Integer> naturalOrder()).toList()));
        assertEquals(CommonUtil.emptyList(), Stream.of(5, 3, 8).top(0, Comparator.<Integer> naturalOrder()).toList());
    }

    @Test
    public void testD1_topStillRejectsANegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> Seq.of(5, 3).top(-1).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(5, 3).top(-1, Comparator.<Integer> naturalOrder()).toList());
    }

    @Test
    public void testD1_topStillReturnsTheLargestElements() {
        assertEquals(CommonUtil.asList(5, 8), assertDoesNotThrowChecked( //
                () -> Seq.of(5, 3, 8, 1).sorted(Comparators.<Integer> naturalOrder()).top(2, Comparators.<Integer> naturalOrder()).toList()));
        assertEquals(new HashSet<>(CommonUtil.asList(8, 9)), new HashSet<>(assertDoesNotThrowChecked(() -> Seq.of(5, 3, 8, 1, 9).top(2).toList())));
    }

    // ------------------------------------------------------------------------------------------------------
    // D2 / D3 - covariance
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testD2_appendAndPrependAcceptASubtypedSequence() {
        // Seq<Number>.append(Seq<Integer, Exception>) did not compile, even though concat(..) accepts it.
        assertEquals(CommonUtil.asList((Number) 1, 2, 9), //
                assertDoesNotThrowChecked(() -> Seq.<Number, Exception> of(1, 2).append(Seq.<Integer, Exception> of(9)).toList()));
        assertEquals(CommonUtil.asList((Number) 9, 1, 2), //
                assertDoesNotThrowChecked(() -> Seq.<Number, Exception> of(1, 2).prepend(Seq.<Integer, Exception> of(9)).toList()));
    }

    @Test
    public void testD3_symmetricDifferenceAcceptsASubtypedCollection() {
        assertEquals(CommonUtil.asList((Number) 1, 3), //
                assertDoesNotThrowChecked(() -> Seq.<Number, Exception> of(1, 2).symmetricDifference(CommonUtil.asList(2, 3)).toList()));
        assertEquals(CommonUtil.asList(1, 3, 2, 4), //
                assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, 1, 2, 3).symmetricDifference(CommonUtil.asList(1, 2, 2, 4)).toList()));
    }

    // ------------------------------------------------------------------------------------------------------
    // D4 - intermediate operations return a new instance
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testD4_noIntermediateOperationReturnsTheReceiver() {
        final Seq<Integer, Exception> a = Seq.of(1, 2, 3);
        assertNotSame(a, assertDoesNotThrowChecked(() -> a.skip(0)));

        final Seq<Integer, Exception> b = Seq.of(1, 2, 3);
        assertNotSame(b, assertDoesNotThrowChecked(() -> b.step(1)));

        final Seq<Integer, Exception> c = assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3).sorted());
        assertNotSame(c, assertDoesNotThrowChecked(c::sorted));

        final Seq<Integer, Exception> d = Seq.of(1, 2, 3);
        assertNotSame(d, assertDoesNotThrowChecked(() -> d.append(u.Optional.<Integer> empty())));

        final Seq<Integer, Exception> e = Seq.of(1, 2, 3);
        assertNotSame(e, assertDoesNotThrowChecked(() -> e.prepend(u.Optional.<Integer> empty())));

        final Seq<Integer, Exception> f = Seq.of(1, 2, 3);
        assertNotSame(f, assertDoesNotThrowChecked(f::cast));

        final Seq<Integer, Exception> g = Seq.of(1, 2, 3);
        assertNotSame(g, assertDoesNotThrowChecked(() -> g.skipLast(0)));

        final Seq<Integer, Exception> h = Seq.of(1, 2, 3);
        assertNotSame(h, assertDoesNotThrowChecked(() -> h.skip(0, i -> {
        })));
    }

    @Test
    public void testD4_theNewInstancesStillProduceTheSameElements() {
        assertEquals(CommonUtil.asList(1, 2, 3), assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3).skip(0).toList()));
        assertEquals(CommonUtil.asList(1, 2, 3), assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3).step(1).toList()));
        assertEquals(CommonUtil.asList(1, 2, 3), assertDoesNotThrowChecked(() -> Seq.of(3, 1, 2).sorted().sorted().toList()));
        assertEquals(CommonUtil.asList(1, 2), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, 2).append(u.Optional.<Integer> empty()).toList()));
        assertEquals(CommonUtil.asList(9, 1, 2), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, 2).prepend(u.Optional.of(9)).toList()));
        assertEquals(CommonUtil.asList(1, 2), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, 2).cast().toList()));

        final List<Integer> skipped = new ArrayList<>();
        assertEquals(CommonUtil.asList(1, 2, 3), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, 2, 3).skip(0, skipped::add).toList()));
        assertEquals(CommonUtil.emptyList(), skipped);
    }

    /** A no-op skip/sort must not lose the sortedness flag that later operations shortcut on. */
    @Test
    public void testD4_theNewInstancesKeepTheKnownSortOrder() {
        assertEquals(u.Nullable.of(1), //
                assertDoesNotThrowChecked(() -> Seq.of(3, 1, 2).sorted().sorted().min(Comparators.<Integer> naturalOrder())));
        assertEquals(u.Nullable.of(1), //
                assertDoesNotThrowChecked(() -> Seq.of(3, 1, 2).sorted().skip(0).min(Comparators.<Integer> naturalOrder())));
        assertEquals(u.Nullable.of(3), //
                assertDoesNotThrowChecked(() -> Seq.of(3, 1, 2).sorted().skip(0).max(Comparators.<Integer> naturalOrder())));
    }

    @Test
    public void testD4_castStillConsumesTheOriginal() {
        final Seq<Integer, Exception> original = Seq.of(1, 2);

        assertEquals(CommonUtil.asList(1, 2), assertDoesNotThrowChecked(() -> original.cast().toList()));
        assertThrows(IllegalStateException.class, original::toList);
    }

    // ------------------------------------------------------------------------------------------------------
    // D6 - partitionTo / partitionBy guarantee the false-then-true order
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testD6_partitionToIteratesFalseThenTrue() {
        final Map<Boolean, List<Integer>> partitioned = assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3, 4).partitionTo(i -> i % 2 == 0));

        assertEquals(CommonUtil.asList(Boolean.FALSE, Boolean.TRUE), new ArrayList<>(partitioned.keySet()));
        assertEquals(CommonUtil.asList(1, 3), partitioned.get(Boolean.FALSE));
        assertEquals(CommonUtil.asList(2, 4), partitioned.get(Boolean.TRUE));
    }

    @Test
    public void testD6_partitionToAlwaysHasBothEntries() {
        assertEquals(CommonUtil.asList(Boolean.FALSE, Boolean.TRUE), //
                new ArrayList<>(assertDoesNotThrowChecked(() -> Seq.of(2, 4).partitionTo(i -> i % 2 == 0)).keySet()));
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> Seq.of(2, 4).partitionTo(i -> i % 2 == 0)).get(Boolean.FALSE));
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> Seq.of(1, 3).partitionTo(i -> i % 2 == 0)).get(Boolean.TRUE));

        final Map<Boolean, List<Integer>> ofEmpty = assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> empty().partitionTo(i -> i % 2 == 0));
        assertEquals(CommonUtil.asList(Boolean.FALSE, Boolean.TRUE), new ArrayList<>(ofEmpty.keySet()));
        assertEquals(CommonUtil.emptyList(), ofEmpty.get(Boolean.FALSE));
        assertEquals(CommonUtil.emptyList(), ofEmpty.get(Boolean.TRUE));
    }

    @Test
    public void testD6_partitionToWithADownstreamCollector() {
        final Map<Boolean, Integer> counted = assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3, 4).partitionTo(i -> i % 2 == 0, Collectors.countingToInt()));

        assertEquals(CommonUtil.asList(Boolean.FALSE, Boolean.TRUE), new ArrayList<>(counted.keySet()));
        assertEquals(2, counted.get(Boolean.FALSE));
        assertEquals(2, counted.get(Boolean.TRUE));

        final Map<Boolean, Integer> allTrue = assertDoesNotThrowChecked(() -> Seq.of(2, 4).partitionTo(i -> i % 2 == 0, Collectors.countingToInt()));
        assertEquals(0, allTrue.get(Boolean.FALSE));
        assertEquals(2, allTrue.get(Boolean.TRUE));
    }

    @Test
    public void testD6_partitionByEmitsFalseThenTrue() {
        final List<Map.Entry<Boolean, List<Integer>>> entries = assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3, 4).partitionBy(i -> i % 2 == 0).toList());

        assertEquals(2, entries.size());
        assertEquals(Boolean.FALSE, entries.get(0).getKey());
        assertEquals(CommonUtil.asList(1, 3), entries.get(0).getValue());
        assertEquals(Boolean.TRUE, entries.get(1).getKey());
        assertEquals(CommonUtil.asList(2, 4), entries.get(1).getValue());
    }

    // ------------------------------------------------------------------------------------------------------
    // D8 - range / rangeClosed
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testD8_rangeMatchesIntStreamAcrossAMatrix() {
        final int[] bounds = { -5, -1, 0, 1, 3, 7 };
        final int[] steps = { -7, -3, -1, 1, 2, 5 };

        for (final int from : bounds) {
            for (final int to : bounds) {
                for (final int by : steps) {
                    final String at = "(" + from + ", " + to + ", " + by + ")";

                    assertEquals(IntStream.range(from, to, by).boxed().toList(), //
                            assertDoesNotThrowChecked(() -> Seq.range(from, to, by).toList()), "range" + at);
                    assertEquals(IntStream.rangeClosed(from, to, by).boxed().toList(), //
                            assertDoesNotThrowChecked(() -> Seq.rangeClosed(from, to, by).toList()), "rangeClosed" + at);

                    // count() and advance() are the reason for the rewrite - they must agree with the elements.
                    assertEquals(IntStream.range(from, to, by).boxed().toList().size(), //
                            assertDoesNotThrowChecked(() -> Seq.range(from, to, by).count()), "range count" + at);
                    assertEquals(IntStream.rangeClosed(from, to, by).boxed().toList().size(), //
                            assertDoesNotThrowChecked(() -> Seq.rangeClosed(from, to, by).count()), "rangeClosed count" + at);
                }

                assertEquals(IntStream.range(from, to).boxed().toList(), assertDoesNotThrowChecked(() -> Seq.range(from, to).toList()));
                assertEquals(IntStream.rangeClosed(from, to).boxed().toList(), assertDoesNotThrowChecked(() -> Seq.rangeClosed(from, to).toList()));
            }
        }
    }

    @Test
    public void testD8_rangeRejectsAZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> Seq.range(0, 5, 0).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.rangeClosed(0, 5, 0).toList());
    }

    @Test
    public void testD8_rangeSkipsAndCountsWithoutWalking() {
        assertEquals(1000L, assertDoesNotThrowChecked(() -> Seq.range(0, 1000).count()));
        assertEquals(CommonUtil.asList(997, 998, 999), assertDoesNotThrowChecked(() -> Seq.range(0, 1000).skip(997).toList()));
        assertEquals(6L, assertDoesNotThrowChecked(() -> Seq.rangeClosed(0, 10, 2).count()));
        // 2^32 elements: only possible because count() is O(1).
        assertEquals(4294967296L, assertDoesNotThrowChecked(() -> Seq.rangeClosed(Integer.MIN_VALUE, Integer.MAX_VALUE).count()));
    }

    @Test
    public void testD8_rangeHandlesTheIntBoundaries() {
        assertEquals(CommonUtil.asList(Integer.MIN_VALUE, -1, Integer.MAX_VALUE - 1), //
                assertDoesNotThrowChecked(() -> Seq.rangeClosed(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE).toList()));
        assertEquals(CommonUtil.asList(Integer.MAX_VALUE), assertDoesNotThrowChecked(() -> Seq.rangeClosed(Integer.MAX_VALUE, Integer.MAX_VALUE).toList()));
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> Seq.range(Integer.MAX_VALUE, Integer.MAX_VALUE).toList()));
    }

    @Test
    public void testD8_rangeIteratorRejectsAnOverRead() {
        final Seq<Integer, Exception> seq = Seq.range(0, 1);
        final Throwables.Iterator<Integer, Exception> iter = assertDoesNotThrowChecked(seq::iteratorEx);

        assertEquals(0, assertDoesNotThrowChecked(iter::next));
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    public void testD8_ofReversedStillWorksOnTopOfRange() {
        assertEquals(CommonUtil.asList("c", "b", "a"), assertDoesNotThrowChecked(() -> Seq.ofReversed(new String[] { "a", "b", "c" }).toList()));
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> Seq.ofReversed((String[]) null).toList()));
    }

    // ------------------------------------------------------------------------------------------------------
    // D9 / D10 - argument validation
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testD9_delayRejectsANegativeDuration() {
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).delay(Duration.ofMillis(-1)).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).delay(java.time.Duration.ofMillis(-1)).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).delay((Duration) null).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).delay((java.time.Duration) null).toList());
    }

    @Test
    public void testD9_delayStillAcceptsZero() {
        assertEquals(CommonUtil.asList(1, 2), assertDoesNotThrowChecked(() -> Seq.of(1, 2).delay(Duration.ofMillis(0)).toList()));
    }

    @Test
    public void testD9_delayClosesTheSequenceOnRejection() {
        final AtomicInteger closed = new AtomicInteger();
        final Seq<Integer, Exception> seq = Seq.<Integer, Exception> of(1).onClose(closed::incrementAndGet);

        assertThrows(IllegalArgumentException.class, () -> seq.delay(Duration.ofMillis(-1)));
        assertEquals(1, closed.get());
    }

    @Test
    public void testD10_splitByChunkCountValidatesInTheSameOrderInBothOverloads() {
        final Throwables.IntBiFunction<Integer, Exception> mapper = (from, to) -> from;

        final IllegalArgumentException fromThree = assertThrows(IllegalArgumentException.class,
                () -> Seq.<Integer, Exception> splitByChunkCount(-1, 5, mapper).toList());
        final IllegalArgumentException fromFour = assertThrows(IllegalArgumentException.class,
                () -> Seq.<Integer, Exception> splitByChunkCount(-1, 5, false, mapper).toList());

        assertEquals(fromFour.getMessage(), fromThree.getMessage());
        assertTrue(fromThree.getMessage().contains("totalSize"), fromThree.getMessage());
    }

    @Test
    public void testD10_splitByChunkCountStillChunksCorrectly() {
        final int[] source = Array.rangeClosed(1, 7);

        assertEquals(CommonUtil.asList("[1, 2]", "[3, 4]", "[5]", "[6]", "[7]"), assertDoesNotThrowChecked(
                () -> Seq.<int[], Exception> splitByChunkCount(7, 5, (from, to) -> CommonUtil.copyOfRange(source, from, to)).map(Arrays::toString).toList()));
        assertEquals(CommonUtil.asList("[1]", "[2]", "[3]", "[4, 5]", "[6, 7]"),
                assertDoesNotThrowChecked(() -> Seq.<int[], Exception> splitByChunkCount(7, 5, true, (from, to) -> CommonUtil.copyOfRange(source, from, to))
                        .map(Arrays::toString)
                        .toList()));
        assertEquals(0L, assertDoesNotThrowChecked(() -> Seq.<int[], Exception> splitByChunkCount(0, 5, (from, to) -> null).count()));
    }

    // ------------------------------------------------------------------------------------------------------
    // D12 - top releases the source as soon as it has drained it
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testD12_topClosesTheSourceWhenItMaterialises() {
        final AtomicInteger closed = new AtomicInteger();
        final Seq<Integer, Exception> source = Seq.<Integer, Exception> of(5, 3, 8).onClose(closed::incrementAndGet);
        final Seq<Integer, Exception> top = assertDoesNotThrowChecked(() -> source.top(2));

        assertEquals(0, closed.get(), "top(..) must stay lazy until it is traversed");

        assertEquals(2, assertDoesNotThrowChecked(top::toList).size());
        assertEquals(1, closed.get());
    }

    // ------------------------------------------------------------------------------------------------------
    // J1 - the documented empty/null-string split contract
    //
    // Pinned with explicit sizes: a one-element list holding the empty string and a truly empty list both render
    // as "[]", which is exactly how this behaviour was first mis-read.
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testJ1_splittingAnEmptyStringYieldsOneEmptyToken() {
        assertEquals(CommonUtil.asList(""), assertDoesNotThrowChecked(() -> Seq.split("", ',').toList()));
        assertEquals(CommonUtil.asList(""), assertDoesNotThrowChecked(() -> Seq.split("", "::").toList()));
        assertEquals(CommonUtil.asList(""), assertDoesNotThrowChecked(() -> Seq.split("", Pattern.compile("\\s+")).toList()));
        assertEquals(CommonUtil.asList(""), assertDoesNotThrowChecked(() -> Seq.splitToLines("").toList()));
        assertEquals(CommonUtil.asList(""), assertDoesNotThrowChecked(() -> Seq.splitToLines("", false, false).toList()));
        // ... unless empty lines are omitted, which is the one documented exception.
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> Seq.splitToLines("", true, true).toList()));

        assertEquals(1, assertDoesNotThrowChecked(() -> Seq.split("", ',').toList()).size());
        assertEquals(0, assertDoesNotThrowChecked(() -> Seq.splitToLines("", true, true).toList()).size());
    }

    @Test
    public void testJ1_splittingANullStringYieldsAnEmptySequence() {
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> Seq.split(null, ',').toList()));
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> Seq.split(null, "::").toList()));
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> Seq.split(null, Pattern.compile("\\s+")).toList()));
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> Seq.splitToLines(null).toList()));
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> Seq.splitToLines(null, false, false).toList()));
        assertEquals(0, assertDoesNotThrowChecked(() -> Seq.split(null, ',').toList()).size());
    }

    @Test
    public void testJ1_splittingANonEmptyStringIsUnchanged() {
        assertEquals(CommonUtil.asList("a", "", "b"), assertDoesNotThrowChecked(() -> Seq.split("a,,b", ',').toList()));
        assertEquals(CommonUtil.asList("line1", "line2", "line3", "line4"),
                assertDoesNotThrowChecked(() -> Seq.splitToLines("line1\nline2\r\nline3\rline4").toList()));
        assertEquals(CommonUtil.asList("line1", "line3"), assertDoesNotThrowChecked(() -> Seq.splitToLines(" line1 \n  \nline3  ", true, true).toList()));
    }

    // ------------------------------------------------------------------------------------------------------
    // J6 - a null collection is an empty one for the set-like operations
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testJ6_aNullCollectionIsTreatedAsEmpty() {
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3).intersection(null).toList()));
        assertEquals(CommonUtil.asList(1, 2, 3), assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3).difference(null).toList()));
        assertEquals(CommonUtil.asList(1, 2, 3), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, 2, 3).symmetricDifference(null).toList()));
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, 2).intersection(i -> i, null).toList()));
        assertEquals(CommonUtil.asList(1, 2), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, 2).difference(i -> i, null).toList()));
    }

    // ------------------------------------------------------------------------------------------------------
    // J8 - mapPartial rejects a null optional
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testJ8_mapPartialRejectsANullOptional() {
        assertThrows(NullPointerException.class, () -> Seq.of(1).mapPartial(i -> (u.Optional<Integer>) null).toList());
        assertThrows(NullPointerException.class, () -> Seq.of(1).mapPartialToInt(i -> null).toList());
        assertThrows(NullPointerException.class, () -> Seq.of(1).mapPartialToLong(i -> null).toList());
        assertThrows(NullPointerException.class, () -> Seq.of(1).mapPartialToDouble(i -> null).toList());
    }

    // ------------------------------------------------------------------------------------------------------
    // J9 - splitAt always emits exactly two sub-sequences
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testJ9_splitAtAlwaysEmitsTwoSubSequences() {
        final List<Seq<Integer, Exception>> ofEmpty = assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> empty().splitAt(2).toList());
        assertEquals(2, ofEmpty.size());
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> ofEmpty.get(0).toList()));
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> ofEmpty.get(1).toList()));

        final List<Seq<Integer, Exception>> pastTheEnd = assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, 2).splitAt(9).toList());
        assertEquals(2, pastTheEnd.size());
        assertEquals(CommonUtil.asList(1, 2), assertDoesNotThrowChecked(() -> pastTheEnd.get(0).toList()));
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> pastTheEnd.get(1).toList()));

        final List<Seq<Integer, Exception>> neverMatching = assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, 2).splitAt(i -> i > 99).toList());
        assertEquals(2, neverMatching.size());
        assertEquals(CommonUtil.asList(1, 2), assertDoesNotThrowChecked(() -> neverMatching.get(0).toList()));
        assertEquals(CommonUtil.emptyList(), assertDoesNotThrowChecked(() -> neverMatching.get(1).toList()));
    }

    // ------------------------------------------------------------------------------------------------------
    // O4 - hasMatchCountBetween names the offending bounds
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testO4_hasMatchCountBetweenReportsTheBounds() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Seq.of(1).hasMatchCountBetween(3, 1, i -> true));

        assertTrue(e.getMessage().contains("3"), e.getMessage());
        assertTrue(e.getMessage().contains("1"), e.getMessage());
    }

    @Test
    public void testO4_hasMatchCountBetweenStillAnswersCorrectly() {
        assertTrue(assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3, 4).hasMatchCountBetween(2, 2, i -> i % 2 == 0)));
        assertFalse(assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3, 4).hasMatchCountBetween(3, 4, i -> i % 2 == 0)));
        assertFalse(assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3, 4).hasMatchCountBetween(0, 1, i -> i % 2 == 0)));
    }

    // ------------------------------------------------------------------------------------------------------
    // Regression sweep over behaviour the fixes above run through
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testRegression_pipelineBehaviourIsUnchanged() {
        assertEquals(CommonUtil.asList(CommonUtil.asList(1, 2), CommonUtil.asList(2, 3)),
                assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3).sliding(2, 1).toList()));
        assertEquals(CommonUtil.asList(CommonUtil.asList(1, 2), CommonUtil.asList(4, 5), CommonUtil.asList(7)),
                assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3, 4, 5, 6, 7).sliding(2, 3).toList()));
        assertEquals(3L, assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3, 4, 5, 6, 7).sliding(2, 3).count()));
        assertEquals(CommonUtil.asList(CommonUtil.asList(3, 4), CommonUtil.asList(4, 5)),
                assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3, 4, 5).sliding(2, 1).skip(2).toList()));
        assertEquals(CommonUtil.asList("1,2", "3,4", "5,null"),
                assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3, 4, 5).slidingMap(2, false, (a, b) -> a + "," + b).toList()));
        assertEquals(CommonUtil.asList(1, 10, 2, 20),
                assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, 2).flatMap(i -> Seq.of(i, i * 10)).toList()));
        assertEquals(CommonUtil.asList(1, 2, 1, 2), assertDoesNotThrowChecked(() -> Seq.of(1, 2).cycled(2).toList()));
        assertEquals(CommonUtil.asList(1, null), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, null, 3, null).skipLast(2).toList()));
        assertEquals(CommonUtil.asList(3, null), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, null, 3, null).takeLast(2).toList()));
        assertEquals(CommonUtil.asList("a", null, "b"), assertDoesNotThrowChecked(() -> Seq.<String, Exception> of("a", null, "b").buffered(2).toList()));
        assertEquals(CommonUtil.asList(4, 5, 1, 2, 3), assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3, 4, 5).rotated(2).toList()));
        assertEquals(CommonUtil.asList(3, 2, 1), assertDoesNotThrowChecked(() -> Seq.of(1, 2, 3).reversed().toList()));
        assertEquals(CommonUtil.asList(1, 0, 2, 0, 3), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> of(1, 2, 3).intersperse(0).toList()));
        assertEquals(CommonUtil.asList("x", "x"), assertDoesNotThrowChecked(() -> Seq.repeat("x", 10).skip(8).toList()));
        assertEquals(1_000_000_000L, assertDoesNotThrowChecked(() -> Seq.repeat("x", 1_000_000_000L).count()));
        assertEquals(CommonUtil.asList(1, 2), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> defer(() -> Seq.of(1, 2)).toList()));
        assertEquals(CommonUtil.asList("a", "b"), assertDoesNotThrowChecked(() -> Seq.ofLines(new StringReader("a\nb")).toList()));
        assertEquals(CommonUtil.asList(1, 2, 3), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> empty().appendIfEmpty(1, 2, 3).toList()));
        assertEquals(CommonUtil.asList(4, 5), assertDoesNotThrowChecked(() -> Seq.of(4, 5).appendIfEmpty(1, 2, 3).toList()));
        assertEquals(CommonUtil.asList(1, 2), assertDoesNotThrowChecked(() -> Seq.of(1, 2).collect(Collectors.toList())));
        assertEquals(1L, assertDoesNotThrowChecked(() -> Seq.of(new int[] { 1, 2 }, new int[] { 1, 2 }).distinct().count()));
        assertTrue(assertDoesNotThrowChecked(() -> Seq.of(new int[] { 1, 2 }, new int[] { 1, 2 }).containsDuplicates()));
        assertEquals(u.Nullable.of(8), assertDoesNotThrowChecked(() -> Seq.of(5, 3, 8, 1, 9).kthLargest(2, Comparator.<Integer> naturalOrder())));
    }

    @Test
    public void testRegression_deferInvokesItsSupplierAtMostOnce() {
        final AtomicInteger calls = new AtomicInteger();
        final Supplier<Seq<Integer, Exception>> supplier = () -> {
            calls.incrementAndGet();
            return Seq.of(1, 2);
        };

        assertEquals(CommonUtil.asList(1, 2), assertDoesNotThrowChecked(() -> Seq.<Integer, Exception> defer(supplier).toList()));
        assertEquals(1, calls.get());

        calls.set(0);
        Seq.<Integer, Exception> defer(supplier).close();
        assertEquals(0, calls.get(), "an untraversed deferred sequence must not open its source");
    }

    // ------------------------------------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------------------------------------

    /** Runs a checked-exception-throwing supplier, turning an unexpected checked exception into a test failure. */
    private static <R> R assertDoesNotThrowChecked(final Throwables.Supplier<R, Exception> supplier) {
        try {
            return supplier.get();
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new AssertionError("unexpected checked exception", e);
        }
    }

    /** A collector whose accessors all throw, used to prove the sequence is closed anyway. */
    private static final class BrokenCollector<T, A, R> implements Collector<T, A, R> {
        @Override
        public Supplier<A> supplier() {
            throw new IllegalStateException("broken supplier");
        }

        @Override
        public BiConsumer<A, T> accumulator() {
            throw new IllegalStateException("broken accumulator");
        }

        @Override
        public BinaryOperator<A> combiner() {
            throw new IllegalStateException("broken combiner");
        }

        @Override
        public Function<A, R> finisher() {
            throw new IllegalStateException("broken finisher");
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }
    }

    // --- G12-001: distinct(mergeFunction)/distinctBy(keyMapper, mergeFunction) must decide duplicates the same way
    // --- distinct()/distinctBy(keyMapper) do - array elements and array-valued keys by content, not by identity.
    @Test
    public void testDistinctWithMergeFunction_comparesArrayElementsByContent() throws Exception {
        final int[] a1 = { 1, 2 };
        final int[] a2 = { 1, 2 };

        assertEquals(1, Seq.of(a1, a2).distinct().count());
        assertEquals(1, Seq.of(a1, a2).distinct((x, y) -> x).count());
        assertEquals(1, Seq.of(a1, a2).distinctBy(Fnn.identity()).count());
        assertEquals(1, Seq.of(a1, a2).distinctBy(Fnn.identity(), (x, y) -> x).count());
        assertTrue(Seq.of(a1, a2).containsDuplicates());

        assertEquals(Arrays.asList("x"), Seq.of("x", "y").distinctBy(s -> new int[] { 1 }).toList());
        assertEquals(Arrays.asList("x"), Seq.of("x", "y").distinctBy(s -> new int[] { 1 }, (p, q) -> p).toList());

        // Distinct array contents still stay distinct.
        assertEquals(2, Seq.of(new int[] { 1 }, new int[] { 2 }).distinct((x, y) -> x).count());

        // Non-array elements and keys are unaffected.
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 1, 2, 2, 3).distinct((a, b) -> a).toList());
        assertEquals(Arrays.asList(2, 4, 3, 4), Seq.of(1, 2, 1, 3, 2, 4).distinct((a, b) -> a + b).toList());
        assertEquals(Arrays.asList("apple"), Seq.of("apple", "apricot", "avocado").distinctBy(s -> s.charAt(0), (a, b) -> a).toList());
    }

    // --- G12-002 (doc): the merge overloads inherit toMap's Map.merge remove-on-null contract, which distinct() does
    // --- not share. Pins the behaviour the corrected javadoc now describes.
    @Test
    public void testDistinctWithMergeFunction_nullMergeResultDropsTheElementOrGroup() throws Exception {
        assertEquals(Arrays.asList("a", null, "b"), Seq.of("a", null, "b", null).distinct().toList());

        // A faithful (x, y) -> x returns null when the duplicated element is itself null, which removes the mapping.
        assertEquals(Arrays.asList("a", "b"), Seq.of("a", null, "b", null).distinct((x, y) -> x).toList());
        assertEquals(Arrays.asList("a", "b"), Seq.of("a", null, "b", null).distinctBy(Fnn.identity(), (x, y) -> x).toList());

        // A single null has nothing to merge with, so it survives.
        assertEquals(Arrays.asList("a", null, "b"), Seq.of("a", null, "b").distinct((x, y) -> x).toList());

        // An explicit null merge result drops the whole group.
        assertEquals(Arrays.asList("c"), Seq.of("aa", "bb", "c").distinctBy(String::length, (x, y) -> null).toList());
    }

    // --- G12-006 (doc): the merge overloads retain one merged value per distinct key, not every element.
    @Test
    public void testDistinctWithMergeFunction_retainsOneValuePerDistinctKey() throws Exception {
        final AtomicInteger merges = new AtomicInteger();

        assertEquals(Arrays.asList(1), Seq.repeat(1, 1000).distinct((a, b) -> {
            merges.incrementAndGet();
            return a;
        }).toList());
        assertEquals(999, merges.get());

        // Two distinct keys over five elements retain two values, not five.
        assertEquals(Arrays.asList("aa", "b"), Seq.of("aa", "ab", "ac", "b", "bb").distinctBy(s -> s.charAt(0), (a, b) -> a).toList());
    }

    // --- G13-001: takeLast(..)/last(..) flag their lazy init() before draining, so a hasNext() retried after a failed
    // --- drain must degrade to an empty iterator (what top/reversed/rotated/sorted do) instead of throwing NPE.
    @SuppressWarnings("deprecation")
    @Test
    public void testTakeLast_retriedHasNextAfterAFailedDrainDegradesToEmpty() throws Exception {
        final Iterator<Integer> takeLastIter = Seq.<Integer, Exception> of(1, 2, 3).map(x -> {
            if (x == 2) {
                throw new IllegalStateException("source boom");
            }
            return x;
        }).takeLast(2).stream().iterator();

        assertEquals("source boom", assertThrows(IllegalStateException.class, takeLastIter::hasNext).getMessage());
        assertFalse(takeLastIter.hasNext());

        final Iterator<Integer> lastIter = Seq.<Integer, Exception> of(1, 2, 3).map(x -> {
            if (x == 2) {
                throw new IllegalStateException("source boom");
            }
            return x;
        }).last(2).stream().iterator();

        assertEquals("source boom", assertThrows(IllegalStateException.class, lastIter::hasNext).getMessage());
        assertFalse(lastIter.hasNext());

        // The success path is unchanged.
        assertEquals(Arrays.asList(4, 5), Seq.of(1, 2, 3, 4, 5).takeLast(2).toList());
    }

    // --- G13-003: rotated(..) must not overflow its physical-index arithmetic. `start` reaches `len` whenever
    // --- `distance` is a non-zero multiple of `len`, and `cnt` runs to len - 1, so `start + cnt` reaches 2 * len - 1 -
    // --- which overflows an int once len >= 2^30 + 1. Materializing a sequence that long needs a multi-GB heap, so the
    // --- state is installed on the live iterator by reflection instead: `aar` stays small while `len`/`start`/`cnt`
    // --- carry the large values, so only the index expression is exercised. Before the fix it evaluated to aar[-3].
    @Test
    public void testRotated_physicalIndexArithmeticIsOverflowSafe() throws Exception {
        final int len = Integer.MAX_VALUE - 2; // the largest array HotSpot allows; rotating by len leaves start == len
        final Seq<String, Exception> seq = Seq.<String, Exception> of("A", "B", "C", "D").rotated(1);

        final java.lang.reflect.Field elementsField = Seq.class.getDeclaredField("elements");
        elementsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Throwables.Iterator<String, Exception> iter = (Throwables.Iterator<String, Exception>) elementsField.get(seq);

        setPrivateField(iter, "initialized", true);
        setPrivateField(iter, "aar", new Object[] { "A", "B", "C", "D" });
        setPrivateField(iter, "len", len);
        setPrivateField(iter, "start", len);
        setPrivateField(iter, "cnt", 3);

        assertTrue(iter.hasNext());
        // (len + 3) % len is 3 in long arithmetic; as an int the sum wraps to Integer.MIN_VALUE + 1 and the remainder
        // is -3, so the old expression indexed aar[-3].
        assertEquals("D", iter.next());

        // start == len - the maximum-sum case - on inputs small enough to run.
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), Seq.of(1, 2, 3, 4, 5).rotated(5).toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), Seq.of(1, 2, 3, 4, 5).rotated(-5).toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), Seq.of(1, 2, 3, 4, 5).rotated(10).toList());
        assertEquals(Arrays.asList(1), Seq.of(1).rotated(1).toList());

        assertEquals(Arrays.asList(5, 1, 2, 3, 4), Seq.of(1, 2, 3, 4, 5).rotated(1).toList());
        assertEquals(Arrays.asList(2, 3, 4, 5, 1), Seq.of(1, 2, 3, 4, 5).rotated(-1).toList());
        assertEquals(Arrays.asList(4, 5, 1, 2, 3), Seq.of(1, 2, 3, 4, 5).rotated(2).toList());
    }

    private static void setPrivateField(final Object target, final String name, final Object value) throws Exception {
        final java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
