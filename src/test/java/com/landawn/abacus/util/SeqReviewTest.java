package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.LongIteratorEx;
import com.landawn.abacus.util.stream.LongStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

public class SeqReviewTest extends SeqTestSupport {
    // ===================================================================

    @Test
    public void testReview_skipZero_returnsAnEquivalentSequence() throws Exception {
        // skip(0) is a no-op on the elements, but - like every other intermediate operation, and as the class
        // javadoc has always claimed - it now derives a NEW sequence rather than handing back the receiver.
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        Seq<Integer, Exception> skipped = seq.skip(0);
        assertNotSame(seq, skipped);
        assertEquals(Arrays.asList(1, 2, 3), skipped.toList());
        // ... and consuming the derived one consumes the source.
        assertThrows(IllegalStateException.class, seq::toList);
    }

    @Test
    public void testReview_skipZero_keepsTheKnownSortOrder() throws Exception {
        // The derived no-op must carry `sorted`/`cmp` across, or min(..)/max(..) lose their shortcut.
        assertEquals(Nullable.of(1), Seq.of(3, 1, 2).sorted().skip(0).min(Comparators.<Integer> naturalOrder()));
        assertEquals(Nullable.of(3), Seq.of(3, 1, 2).sorted().skip(0).max(Comparators.<Integer> naturalOrder()));
    }

    @Test
    public void testReview_skipNegative_throws() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).skip(-1));
    }

    @Test
    public void testReview_limitZero_returnsEmpty() throws Exception {
        assertEquals(Collections.emptyList(), Seq.of(1, 2, 3).limit(0).toList());
    }

    @Test
    public void testReview_limitLargerThanSize() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).limit(100).toList());
    }

    @Test
    public void testReview_limitNegative_throws() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).limit(-5));
    }

    @Test
    public void testReview_distinctWithNullElements() throws Exception {
        // distinct uses hashKey() which maps null to a sentinel, so multiple nulls collapse to one.
        List<Integer> result = Seq.of(1, null, 2, null, 1, 3).distinct().toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(null));
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
    }

    @Test
    public void testReview_distinctByWithNullKey() throws Exception {
        List<String> result = Seq.of("a", "bb", "c", "dd").distinctBy(s -> s.length() == 1 ? null : s.length()).toList();
        // first len=1 ('a') with null-key kept, first len=2 ('bb') kept; later duplicates dropped
        assertEquals(Arrays.asList("a", "bb"), result);
    }

    @Test
    public void testReview_toMap_duplicateKey_throws() throws Exception {
        // Default Seq.toMap uses throwingMerger and throws on duplicate keys.
        assertThrows(IllegalStateException.class,
                () -> Seq.of("a", "b", "a").toMap(java.util.function.Function.identity()::apply, java.util.function.Function.identity()::apply));
    }

    @Test
    public void testReview_toMap_duplicateKey_withMergeFunction() throws Exception {
        Map<Object, Integer> result = Seq.of("a", "b", "a").toMap(java.util.function.Function.identity()::apply, s -> 1, Integer::sum);
        assertEquals(Integer.valueOf(2), result.get("a"));
        assertEquals(Integer.valueOf(1), result.get("b"));
    }

    @Test
    public void testReview_reduceEmpty_returnsEmptyOptional() throws Exception {
        assertFalse(Seq.<Integer, Exception> empty().reduce(Integer::sum).isPresent());
    }

    @Test
    public void testReview_reduceWithIdentity_emptySeqReturnsIdentity() throws Exception {
        assertEquals(Integer.valueOf(42), Seq.<Integer, Exception> empty().reduce(42, Integer::sum));
    }

    @Test
    public void testReview_minMaxEmpty_returnsEmpty() throws Exception {
        assertFalse(Seq.<Integer, Exception> empty().min(Comparator.naturalOrder()).isPresent());
        assertFalse(Seq.<Integer, Exception> empty().max(Comparator.naturalOrder()).isPresent());
    }

    @Test
    public void testReview_minNullComparator_throws() throws Exception {
        // min() requires a comparator; a null one now surfaces as an NPE when it is invoked.
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).min(null));
    }

    @Test
    public void testReview_maxNullComparator_throws() {
        assertThrows(IllegalArgumentException.class, () -> Seq.of(3, 1, 2).max(null));
    }

    @Test
    public void testReview_allMatchEmpty_isVacuouslyTrue() throws Exception {
        assertTrue(Seq.<Integer, Exception> empty().allMatch(n -> false));
    }

    @Test
    public void testReview_anyMatchEmpty_isFalse() throws Exception {
        assertFalse(Seq.<Integer, Exception> empty().anyMatch(n -> true));
    }

    @Test
    public void testReview_noneMatchEmpty_isTrue() throws Exception {
        assertTrue(Seq.<Integer, Exception> empty().noneMatch(n -> true));
    }

    @Test
    public void testReview_sumInt_promotesToLong_noOverflow() throws Exception {
        // Two MAX_VALUE ints would overflow as int, but sumInt returns long.
        long total = Seq.of(Integer.MAX_VALUE, Integer.MAX_VALUE).sumInt(Integer::intValue);
        assertEquals(2L * Integer.MAX_VALUE, total);
    }

    @Test
    public void testReview_sumLong_canSilentlyOverflow_documented() throws Exception {
        // sumLong does NOT detect overflow - documents current behaviour so a regression is detected if changed.
        long total = Seq.of(Long.MAX_VALUE, 1L).sumLong(Long::longValue);
        assertEquals(Long.MIN_VALUE, total); // wrap-around
    }

    @Test
    public void testReview_averageInt_emptyReturnsEmpty() throws Exception {
        assertFalse(Seq.<Integer, Exception> empty().averageInt(Integer::intValue).isPresent());
    }

    @Test
    public void testReview_count_onEmpty() throws Exception {
        assertEquals(0L, Seq.<Integer, Exception> empty().count());
    }

    @Test
    public void testReview_peek_doesNotChangeContent() throws Exception {
        AtomicInteger seen = new AtomicInteger();
        List<Integer> r = Seq.of(1, 2, 3).peek(n -> seen.incrementAndGet()).toList();
        assertEquals(Arrays.asList(1, 2, 3), r);
        assertEquals(3, seen.get());
    }

    @Test
    public void testReview_close_isIdempotent() {
        AtomicInteger callCount = new AtomicInteger();
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).onClose(callCount::incrementAndGet);
        seq.close();
        seq.close();
        seq.close();
        assertEquals(1, callCount.get());
    }

    @Test
    public void testReview_terminalOpAfterClose_throws() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        seq.close();
        assertThrows(IllegalStateException.class, seq::toList);
    }

    @Test
    public void testReview_reuseConsumedSeq_throws() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        seq.toList();
        // Once a terminal op has run, the seq is closed; further terminal ops must reject.
        assertThrows(IllegalStateException.class, seq::toList);
    }

    @Test
    public void testReview_zipWith_stopsAtShortest() throws Exception {
        List<String> r = Seq.of(1, 2, 3, 4, 5).zipWith(Arrays.asList("a", "b", "c"), (i, s) -> i + s).toList();
        assertEquals(Arrays.asList("1a", "2b", "3c"), r);
    }

    @Test
    public void testReview_zipWithDefaults_padsToLongest() throws Exception {
        List<String> r = Seq.of(1, 2).zipWith(Arrays.asList("a", "b", "c"), 0, "?", (i, s) -> i + s).toList();
        assertEquals(Arrays.asList("1a", "2b", "0c"), r);
    }

    @Test
    public void testReview_cycledZeroRounds_isEmpty() throws Exception {
        assertEquals(Collections.emptyList(), Seq.of(1, 2, 3).cycled(0).toList());
    }

    @Test
    public void testReview_cycledTwoRounds() throws Exception {
        assertEquals(Arrays.asList(1, 2, 1, 2), Seq.of(1, 2).cycled(2).toList());
    }

    @Test
    public void testReview_cycledOnEmptySource_terminates() throws Exception {
        // Cycling an empty source should not loop forever.
        assertEquals(Collections.emptyList(), Seq.<Integer, Exception> empty().cycled().limit(10).toList());
    }

    @Test
    public void testReview_repeatZero_isEmpty() throws Exception {
        assertEquals(Collections.emptyList(), Seq.repeat("x", 0).toList());
    }

    @Test
    public void testReview_repeatNegative_throws() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.repeat("x", -1));
    }

    @Test
    public void testReview_ofNullArray_isEmpty() throws Exception {
        Integer[] a = null;
        assertEquals(Collections.emptyList(), Seq.of(a).toList());
    }

    @Test
    public void testReview_ofNullable_null() throws Exception {
        assertEquals(Collections.emptyList(), Seq.ofNullable(null).toList());
    }

    @Test
    public void testReview_ofNullable_present() throws Exception {
        assertEquals(Collections.singletonList("x"), Seq.ofNullable("x").toList());
    }

    @Test
    public void testReview_groupBy_nullKeyAllowed() throws Exception {
        // Default groupBy uses HashMap and must allow null keys.
        Map<Integer, List<String>> result = Seq.of("a", "bb", "c", "dd")
                .groupBy(s -> s.length() == 1 ? null : s.length())
                .toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(Arrays.asList("a", "c"), result.get(null));
        assertEquals(Arrays.asList("bb", "dd"), result.get(2));
    }

    @Test
    public void testReview_filterIsLazy_predicateNotCalledUntilTerminal() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);
        Seq<Integer, Exception> chain = Seq.of(1, 2, 3).filter(n -> {
            called.set(true);
            return true;
        });
        assertFalse(called.get(), "predicate should not run before terminal op");
        chain.toList();
        assertTrue(called.get());
    }

    @Test
    public void testReview_mapIsLazy() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);
        Seq<Integer, Exception> chain = Seq.of(1, 2, 3).map(n -> {
            called.set(true);
            return n;
        });
        assertFalse(called.get());
        chain.count();
        assertTrue(called.get());
    }

    @Test
    public void testReview_takeLastZero_isEmpty() throws Exception {
        assertEquals(Collections.emptyList(), Seq.of(1, 2, 3).takeLast(0).toList());
    }

    @Test
    public void testReview_skipLastLargerThanSize_isEmpty() throws Exception {
        assertEquals(Collections.emptyList(), Seq.of(1, 2, 3).skipLast(10).toList());
    }

    @Test
    public void testReview_distinctOnInfinite_terminatesWithLimit() throws Exception {
        // An eventual repeat scheme - because Seq is sequential, distinct is implemented as a stateful filter and
        // works correctly when downstream limit() short-circuits.
        List<Integer> r = Seq.of(1, 2, 3).cycled().distinct().limit(3).toList();
        assertEquals(Arrays.asList(1, 2, 3), r);
    }

    @Test
    public void testReview_forEachPropagatesException() {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        Exception thrown = assertThrows(Exception.class, () -> seq.forEach(n -> {
            if (n == 2) {
                throw new IllegalStateException("boom");
            }
        }));
        assertTrue(thrown instanceof IllegalStateException);
    }

    @Test
    public void testReview_forEachClosesEvenOnException() {
        AtomicBoolean closed = new AtomicBoolean();
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).onClose(() -> closed.set(true));
        try {
            seq.forEach(n -> {
                throw new RuntimeException("x");
            });
        } catch (Exception ignore) {
            // expected
        }
        assertTrue(closed.get());
    }

    @Test
    public void testReview_topZero_returnsEmpty() throws Exception {
        // Bug fix: top(int n) advertises "must not be negative" so n=0 should be allowed and yield an empty seq.
        // Previously top(0) delegated to top(0, comparator) which used checkArgPositive and threw.
        assertEquals(Collections.emptyList(), Seq.of(3, 1, 4, 1, 5, 9, 2, 6).top(0).toList());
    }

    @Test
    public void testReview_kthLargestWithNullAwareComparator() throws Exception {
        Nullable<Integer> kth = Seq.of(3, null, 1, 2).kthLargest(2, Comparator.nullsFirst(Integer::compareTo));

        assertTrue(kth.isPresent());
        assertEquals(Integer.valueOf(2), kth.get());
    }

    @Test
    public void reviewFixes20260906_zipAndMergeCloseEveryInputWhenConstructionFails() {
        // zipWith's @param b says "Will be closed along with this Seq", and the class javadoc says a rejected
        // null function closes the sequence before the exception propagates. Only `this` was ever closed: the
        // other operands were left open forever. Seq.concat(Collection) never had this hole.
        final java.util.concurrent.atomic.AtomicInteger closedA = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger closedB = new java.util.concurrent.atomic.AtomicInteger();

        final Seq<Integer, Exception> a = Seq.<Integer, Exception> of(1, 2).onClose(closedA::incrementAndGet);
        final Seq<Integer, Exception> b = Seq.<Integer, Exception> of(3, 4).onClose(closedB::incrementAndGet);

        assertThrows(IllegalArgumentException.class, () -> a.zipWith(b, null));
        assertEquals(1, closedA.get());
        assertEquals(1, closedB.get(), "the other sequence must be closed too");

        final java.util.concurrent.atomic.AtomicInteger closedC = new java.util.concurrent.atomic.AtomicInteger();
        final Seq<Integer, Exception> c = Seq.<Integer, Exception> of(5).onClose(closedC::incrementAndGet);
        assertThrows(IllegalArgumentException.class, () -> Seq.<Integer, Exception> of(1).mergeWith(c, null));
        assertEquals(1, closedC.get());

        // Static factories: an already-closed first operand must not strand the second.
        final java.util.concurrent.atomic.AtomicInteger closedOpen = new java.util.concurrent.atomic.AtomicInteger();
        final Seq<Integer, Exception> alreadyClosed = Seq.of(1, 2);
        alreadyClosed.close();
        final Seq<Integer, Exception> open = Seq.<Integer, Exception> of(10, 20).onClose(closedOpen::incrementAndGet);

        assertThrows(IllegalStateException.class, () -> Seq.<Integer, Integer, Integer, Exception> zip(alreadyClosed, open, Integer::sum));
        assertEquals(1, closedOpen.get());

        final java.util.concurrent.atomic.AtomicInteger closedOpen2 = new java.util.concurrent.atomic.AtomicInteger();
        final Seq<Integer, Exception> open2 = Seq.<Integer, Exception> of(10).onClose(closedOpen2::incrementAndGet);
        assertThrows(IllegalArgumentException.class, () -> Seq.<Integer, Integer, Integer, Exception> zip(Seq.of(1), open2, null));
        assertEquals(1, closedOpen2.get());

        // Three-operand form.
        final java.util.concurrent.atomic.AtomicInteger closed3 = new java.util.concurrent.atomic.AtomicInteger();
        final Seq<Integer, Exception> third = Seq.<Integer, Exception> of(9).onClose(closed3::incrementAndGet);
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).zipWith(Seq.<Integer, Exception> of(2), third, null));
        assertEquals(1, closed3.get());
    }

    @Test
    public void reviewFixes20260906_zipAndMergeStillWorkNormally() throws Exception {
        assertEquals(CommonUtil.asList(4, 6), Seq.<Integer, Exception> of(1, 2).zipWith(Seq.<Integer, Exception> of(3, 4), Integer::sum).toList());
        assertEquals(CommonUtil.asList(1, 2, 3, 4),
                Seq.<Integer, Exception> of(1, 3)
                        .mergeWith(Seq.<Integer, Exception> of(2, 4), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());

        final java.util.concurrent.atomic.AtomicInteger closedA = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger closedB = new java.util.concurrent.atomic.AtomicInteger();
        Seq.<Integer, Exception> of(1)
                .onClose(closedA::incrementAndGet)
                .zipWith(Seq.<Integer, Exception> of(2).onClose(closedB::incrementAndGet), Integer::sum)
                .toList();
        assertEquals(1, closedA.get());
        assertEquals(1, closedB.get());
    }

    @Test
    public void reviewFixes20260906_containsAllUsesOneEqualityRule() throws Exception {
        // Membership used equals/hashCode (identity for arrays) while the match count used distinct(), which
        // normalises an array through Wrapper (content). Two content-equal arrays therefore collapsed in the
        // count and a sequence that literally contained both answered false.
        final int[] a1 = { 1 };
        final int[] a2 = { 1 };

        assertTrue(Seq.<int[], Exception> of(a1, a2).containsAll(java.util.Arrays.asList(a1, a2)));
        assertTrue(Seq.<int[], Exception> of(a1, a2).containsAll(a1, a2));
        assertTrue(Seq.<int[], Exception> of(a1, a2).containsAll(java.util.Arrays.asList(a1)));
        assertFalse(Seq.<int[], Exception> of(a1).containsAll(java.util.Arrays.asList(a1, new int[] { 2 })));

        // Ordinary elements are unchanged, including duplicates and a missing element.
        assertTrue(Seq.<Integer, Exception> of(1, 2, 3).containsAll(java.util.Arrays.asList(2, 3)));
        assertTrue(Seq.<Integer, Exception> of(1, 2, 3).containsAll(2, 3));
        assertTrue(Seq.<Integer, Exception> of(1, 2, 3).containsAll(2, 2));
        assertFalse(Seq.<Integer, Exception> of(1, 2, 3).containsAll(2, 9));
        assertTrue(Seq.<Integer, Exception> of(1, 2, 3).containsAll(java.util.Collections.<Integer> emptyList()));
        assertTrue(Seq.<Integer, Exception> of(1, 2, 3).containsAll(1, 2, 3));
        assertFalse(Seq.<Integer, Exception> of(1, 2).containsAll(1, 2, 3));

        // A null element must not blow up the membership probe.
        assertTrue(Seq.<String, Exception> of("a", null).containsAll(java.util.Arrays.asList("a", null)));
        assertFalse(Seq.<String, Exception> of("a").containsAll(java.util.Arrays.asList("a", null)));

        // The documented case-insensitive-TreeSet divergence keeps answering false.
        final java.util.Set<String> ci = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ci.add("A");
        ci.add("B");
        assertFalse(Seq.<String, Exception> of("a", "A").containsAll(ci));
    }

    @Test
    public void reviewFixes20260906_slidingOverloadsAgreeOnTheFirstInvalidArgument() throws Exception {
        // The 2-arg overloads validated the function before delegating, while the delegate validates
        // windowSize/increment first - so the same logical call named a different offending argument depending
        // on which overload you used. splitByChunkCount(int, int, ..) already followed the delegate's order.
        final String two = assertThrows(IllegalArgumentException.class,
                () -> Seq.<Integer, Exception> of(1, 2, 3).sliding(0, (java.util.function.IntFunction<java.util.List<Integer>>) null)).getMessage();
        final String three = assertThrows(IllegalArgumentException.class,
                () -> Seq.<Integer, Exception> of(1, 2, 3).sliding(0, 1, (java.util.function.IntFunction<java.util.List<Integer>>) null)).getMessage();
        assertEquals(three, two);

        final String twoMap = assertThrows(IllegalArgumentException.class,
                () -> Seq.<Integer, Exception> of(1, 2, 3).slidingMap(0, (Throwables.BiFunction<Integer, Integer, Integer, Exception>) null)).getMessage();
        final String threeMap = assertThrows(IllegalArgumentException.class,
                () -> Seq.<Integer, Exception> of(1, 2, 3).slidingMap(0, false, (Throwables.BiFunction<Integer, Integer, Integer, Exception>) null))
                        .getMessage();
        assertEquals(threeMap, twoMap);

        // A null function with a VALID window is still rejected, by the delegate.
        assertThrows(IllegalArgumentException.class,
                () -> Seq.<Integer, Exception> of(1, 2, 3).sliding(2, (java.util.function.IntFunction<java.util.List<Integer>>) null));

        // And the normal path is untouched.
        assertEquals(CommonUtil.asList(CommonUtil.asList(1, 2), CommonUtil.asList(2, 3)), Seq.<Integer, Exception> of(1, 2, 3).sliding(2).toList());
    }

    @Test
    public void reviewFixes20260906_takeWhileConsumesTheFirstNonMatchingElement() throws Exception {
        // Documented (F5): takeWhile reads an element from the source BEFORE testing it, so the first
        // non-matching element is consumed and then discarded. Every sibling with that property already said
        // so; takeWhile's javadoc only promised "short-circuiting".
        final List<Integer> pulled = new ArrayList<>();
        assertEquals(CommonUtil.asList(1, 2), Seq.<Integer, Exception> of(1, 2, 3, 4).peek(pulled::add).takeWhile(n -> n < 3).toList());
        assertEquals(CommonUtil.asList(1, 2, 3), pulled, "the failing element 3 is pulled from the source, then dropped");

        // ... and it really is gone from a shared source: [3] is NOT left in the iterator.
        final Iterator<Integer> src = new ArrayList<>(CommonUtil.asList(1, 2, 3, 4)).iterator();
        assertEquals(CommonUtil.asList(1, 2), Seq.<Integer, Exception> of(src).takeWhile(n -> n < 3).toList());
        final List<Integer> remaining = new ArrayList<>();
        while (src.hasNext()) {
            remaining.add(src.next());
        }
        assertEquals(CommonUtil.asList(4), remaining);

        // Control - when every element matches, nothing extra is read and nothing is dropped.
        final List<Integer> pulledAll = new ArrayList<>();
        assertEquals(CommonUtil.asList(1, 2, 3, 4), Seq.<Integer, Exception> of(1, 2, 3, 4).peek(pulledAll::add).takeWhile(n -> n < 9).toList());
        assertEquals(CommonUtil.asList(1, 2, 3, 4), pulledAll);

        // Control - nothing after the failing element is read either.
        final List<Integer> pulledEarly = new ArrayList<>();
        assertEquals(CommonUtil.asList(), Seq.<Integer, Exception> of(1, 2, 3, 4).peek(pulledEarly::add).takeWhile(n -> n > 9).toList());
        assertEquals(CommonUtil.asList(1), pulledEarly);
    }

    @Test
    public void reviewFixes20260906_minByMaxByApplyTheKeyMapperToNullElements() throws Exception {
        // Documented (F6): "nulls last"/"nulls first" orders a null KEY, not a null ELEMENT - the key mapper
        // still sees the null. The @throws clause used to say the NPE "can only happen when every element of
        // the sequence is null", which is only true for a null-tolerant key mapper.
        assertThrows(NullPointerException.class, () -> Seq.<String, Exception> of("bb", null, "a").minBy(String::length));
        assertThrows(NullPointerException.class, () -> Seq.<String, Exception> of("bb", null, "a").maxBy(String::length));

        // The sequence is still closed on that failure.
        final AtomicBoolean closed = new AtomicBoolean();
        assertThrows(NullPointerException.class, () -> Seq.<String, Exception> of("bb", null, "a").onClose(() -> closed.set(true)).minBy(String::length));
        assertTrue(closed.get());

        // Control - with a null-tolerant key mapper the documented ordering holds: a null KEY sorts last for
        // minBy and first for maxBy, so a null element is never the answer while a non-null one exists.
        final Function<String, Comparable> len = s -> s == null ? null : s.length();
        assertEquals("a", Seq.<String, Exception> of("bb", null, "a").minBy(len).get());
        assertEquals("bb", Seq.<String, Exception> of("bb", null, "a").maxBy(len).get());

        // Control - all-null selects a null winner, reported as a PRESENT Nullable holding null; empty is empty.
        assertTrue(Seq.<String, Exception> of((String) null, null).minBy(len).isNull());
        assertTrue(Seq.<String, Exception> of((String) null, null).maxBy(len).isNull());
        assertFalse(Seq.<String, Exception> empty().minBy(len).isPresent());
        assertFalse(Seq.<String, Exception> empty().maxBy(len).isPresent());

        // Control - no nulls at all, the ordinary case, is unchanged.
        assertEquals("a", Seq.<String, Exception> of("bb", "a", "ccc").minBy(String::length).get());
        assertEquals("ccc", Seq.<String, Exception> of("bb", "a", "ccc").maxBy(String::length).get());
    }

    // G03-78: merge(Seq, Seq, Seq, BiFunction) was the one factory that rejected the selector before building a
    // close handler, so a null selector - or an already-closed operand - stranded the inputs the failure never
    // reached. Its five siblings (the four zip shapes and the two-input merge) all close every operand.
    @Test
    public void fixG03_threeSeqMergeClosesEveryInputWhenConstructionFails() {
        final AtomicInteger closedA = new AtomicInteger();
        final AtomicInteger closedB = new AtomicInteger();
        final AtomicInteger closedC = new AtomicInteger();

        final Seq<Integer, Exception> a = Seq.<Integer, Exception> of(1, 4).onClose(closedA::incrementAndGet);
        final Seq<Integer, Exception> b = Seq.<Integer, Exception> of(2, 5).onClose(closedB::incrementAndGet);
        final Seq<Integer, Exception> c = Seq.<Integer, Exception> of(3, 6).onClose(closedC::incrementAndGet);

        assertThrows(IllegalArgumentException.class, () -> Seq.<Integer, Exception> merge(a, b, c, null));
        assertEquals(1, closedA.get());
        assertEquals(1, closedB.get());
        assertEquals(1, closedC.get(), "the third sequence must be closed too");

        // A failure inside the nested construction (an already-closed first operand) must not strand the third
        // sequence either - the inner two-input merge only ever reaches a and b.
        final AtomicInteger closedOpenB = new AtomicInteger();
        final AtomicInteger closedOpenC = new AtomicInteger();
        final Seq<Integer, Exception> alreadyClosed = Seq.of(1);
        alreadyClosed.close();
        final Seq<Integer, Exception> openB = Seq.<Integer, Exception> of(2).onClose(closedOpenB::incrementAndGet);
        final Seq<Integer, Exception> openC = Seq.<Integer, Exception> of(3).onClose(closedOpenC::incrementAndGet);

        assertThrows(IllegalStateException.class,
                () -> Seq.<Integer, Exception> merge(alreadyClosed, openB, openC, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND));
        assertEquals(1, closedOpenB.get());
        assertEquals(1, closedOpenC.get(), "the third sequence must be closed too");
    }

    // G03-78 control: the successful path is unchanged - the merge still produces the merged order and closes
    // each of the three inputs exactly once when the returned sequence is consumed.
    @Test
    public void fixG03_threeSeqMergeStillMergesAndClosesEachInputOnce() throws Exception {
        final AtomicInteger closedA = new AtomicInteger();
        final AtomicInteger closedB = new AtomicInteger();
        final AtomicInteger closedC = new AtomicInteger();

        final Seq<Integer, Exception> a = Seq.<Integer, Exception> of(1, 4).onClose(closedA::incrementAndGet);
        final Seq<Integer, Exception> b = Seq.<Integer, Exception> of(2, 5).onClose(closedB::incrementAndGet);
        final Seq<Integer, Exception> c = Seq.<Integer, Exception> of(3, 6).onClose(closedC::incrementAndGet);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6),
                Seq.<Integer, Exception> merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(1, closedA.get());
        assertEquals(1, closedB.get());
        assertEquals(1, closedC.get());
    }

    // Seq.join(delimiter, prefix, suffix) builds a reuse-mode Joiner, which borrows a StringBuilder from
    // Objectory and only returns it on close(). A throwing traversal skipped that close, so every failed
    // join permanently drained one builder from the pool. Stream's join was given try-with-resources in
    // r9520; this is the same fix on the Seq side.
    @Test
    public void seqJoinReturnsPooledBufferWhenTraversalFails() throws Exception {
        // Warm the pool so there is a builder to watch being taken and returned.
        final Joiner warm = Joiner.with(",", "[", "]").reuseBuffer();
        warm.append("x");
        warm.toString();

        final int before = pooledStringBuilderCount();

        for (int i = 0; i < 5; i++) {
            assertThrows(IllegalStateException.class, () -> Seq.<Integer, Exception> of(1, 2, 3).map(v -> {
                if (v == 2) {
                    throw new IllegalStateException("boom");
                }
                return v;
            }).join(",", "[", "]"));
        }

        assertTrue(pooledStringBuilderCount() >= before,
                "the reuse-mode buffer must be returned to the pool; was " + before + " -> " + pooledStringBuilderCount());

        // The successful path is unchanged.
        assertEquals("[1,2,3]", Seq.<Integer, Exception> of(1, 2, 3).join(",", "[", "]"));
    }

    private static int pooledStringBuilderCount() throws Exception {
        final java.lang.reflect.Field f = Class.forName("com.landawn.abacus.util.Objectory").getDeclaredField("stringBuilderPool");
        f.setAccessible(true);
        return ((java.util.Queue<?>) f.get(null)).size();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testSplitStreamBridgeAdvancesTheFullOverflowingDistance(final boolean collector) throws Exception {
        final AdvancingLongIterator source = new AdvancingLongIterator(Long.MIN_VALUE, Long.MAX_VALUE);
        try (Stream<List<Long>> chunks = groupedLongs(source, true, 2, 2, collector).stream()) {
            assertEquals(List.of(List.of(Long.MIN_VALUE, Long.MIN_VALUE + 1), List.of(Long.MAX_VALUE - 1, Long.MAX_VALUE)),
                    chunks.step(Long.MAX_VALUE).limit(2).toList());
        }

        try (Stream<List<Long>> chunks = groupedLongs(new AdvancingLongIterator(Long.MIN_VALUE, Long.MAX_VALUE), true, 2, 2, collector).stream()) {
            final ObjIteratorEx<List<Long>> iter = (ObjIteratorEx<List<Long>>) chunks.iterator();
            iter.advance(Long.MAX_VALUE);
            assertEquals(List.of(Long.MAX_VALUE - 1, Long.MAX_VALUE), iter.next());
            assertFalse(iter.hasNext());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testSlidingStreamBridgeAdvancesOverflowingGappedAndBufferedWindows(final boolean collector) throws Exception {
        for (int windowSize = 1; windowSize <= 4; windowSize++) {
            final List<Long> first = new ArrayList<>();
            for (int i = 0; i < windowSize; i++) {
                first.add(Long.MIN_VALUE + i);
            }
            final List<Long> last = windowSize == 1 ? List.of(Long.MAX_VALUE - 1) : List.of(Long.MAX_VALUE - 1, Long.MAX_VALUE);
            try (Stream<List<Long>> windows = groupedLongs(new AdvancingLongIterator(Long.MIN_VALUE, Long.MAX_VALUE), false, windowSize, 2, collector)
                    .stream()) {
                // A size-four trailing window would contain only previously consumed elements.
                assertEquals(windowSize == 4 ? List.of(first) : List.of(first, last), windows.step(Long.MAX_VALUE).limit(2).toList());
            }

            for (int prefixMode = 0; prefixMode < 3; prefixMode++) {
                try (Stream<List<Long>> windows = groupedLongs(new AdvancingLongIterator(Long.MIN_VALUE, Long.MAX_VALUE), false, windowSize, 2, collector)
                        .stream()) {
                    final ObjIteratorEx<List<Long>> iter = (ObjIteratorEx<List<Long>>) windows.iterator();
                    if (prefixMode > 0) {
                        assertEquals(first, iter.next());
                    }
                    if (prefixMode == 2) {
                        assertTrue(iter.hasNext());
                    }
                    iter.advance(Long.MAX_VALUE - (prefixMode == 0 ? 0 : 1));
                    if (windowSize != 4) {
                        assertEquals(last, iter.next());
                    }
                    assertFalse(iter.hasNext());
                }
            }
        }
    }

    @Test
    public void testOverflowingGroupedAdvanceStopsAtFiniteSourceExhaustion() throws Exception {
        for (final boolean split : new boolean[] { false, true }) {
            for (final boolean collector : new boolean[] { false, true }) {
                final AdvancingLongIterator source = new AdvancingLongIterator(0, 3);
                try (Stream<List<Long>> groups = groupedLongs(source, split, Integer.MAX_VALUE, Integer.MAX_VALUE, collector).stream()) {
                    final ObjIteratorEx<List<Long>> iter = (ObjIteratorEx<List<Long>>) groups.iterator();
                    iter.advance(0);
                    iter.advance(-1);
                    assertEquals(0, source.advanceCalls);
                    iter.advance(Long.MAX_VALUE);
                    assertEquals(1, source.advanceCalls);
                    assertFalse(iter.hasNext());
                }
            }
        }
    }

    @Test
    public void testOverflowingGroupedAdvancePreservesCheckedFailuresAndCapability() throws Exception {
        for (final boolean split : new boolean[] { false, true }) {
            for (final boolean collector : new boolean[] { false, true }) {
                for (final boolean failOnHasNext : new boolean[] { false, true }) {
                    for (final boolean bridge : new boolean[] { false, true }) {
                        final AdvancingLongIterator source = new AdvancingLongIterator(Long.MIN_VALUE, Long.MAX_VALUE);
                        source.failOnHasNext = failOnHasNext;
                        source.failOnAdvance = failOnHasNext ? 0 : 2;
                        try (Seq<List<Long>, IOException> groups = groupedLongs(source, split, split ? 2 : 3, 2, collector)) {
                            // One source batch can succeed before a later batch or exhaustion check fails.
                            assertTrue(source.supportsFailureAtomicAdvance());
                            assertFalse(groups.iteratorEx().supportsFailureAtomicAdvance());
                            if (bridge) {
                                try (Stream<List<Long>> stream = groups.stream()) {
                                    final ObjIteratorEx<List<Long>> iter = (ObjIteratorEx<List<Long>>) stream.iterator();
                                    assertSame(source.failure, assertThrows(RuntimeException.class, () -> iter.advance(Long.MAX_VALUE)).getCause());
                                    assertEquals(split ? List.of(-2L, -1L) : List.of(-2L, -1L, 0L), iter.next());
                                }
                            } else {
                                final Throwables.Iterator<List<Long>, IOException> iter = groups.iteratorEx();
                                assertSame(source.failure, assertThrows(IOException.class, () -> iter.advance(Long.MAX_VALUE)));
                                assertEquals(split ? List.of(-2L, -1L) : List.of(-2L, -1L, 0L), iter.next());
                            }
                        }
                    }
                }
            }
        }
    }

    private static Seq<List<Long>, IOException> groupedLongs(final AdvancingLongIterator source, final boolean split, final int size, final int increment,
            final boolean collector) {
        final Seq<Long, IOException> seq = Seq.of(source);
        if (split) {
            return collector ? seq.split(size, Collectors.toList()) : seq.split(size, ArrayList::new);
        }
        return collector ? seq.sliding(size, increment, Collectors.toList()) : seq.sliding(size, increment, ArrayList::new);
    }

    private static final class AdvancingLongIterator extends Throwables.Iterator<Long, IOException> {
        private final LongStream stream;
        private final LongIteratorEx iterator;
        private final IOException failure = new IOException("bulk advance failed");
        private int advanceCalls;
        private int reads;
        private int failOnAdvance;
        private boolean failOnHasNext;
        private boolean failed;

        private AdvancingLongIterator(final long first, final long last) {
            stream = LongStream.rangeClosed(first, last);
            iterator = (LongIteratorEx) stream.iterator();
        }

        @Override
        boolean supportsFailureAtomicAdvance() {
            return true;
        }

        @Override
        public boolean hasNext() throws IOException {
            if (failOnHasNext && advanceCalls > 0 && !failed) {
                failed = true;
                throw failure;
            }
            return iterator.hasNext();
        }

        @Override
        public Long next() {
            if (++reads > 12) {
                throw new AssertionError("The virtual source must retain bulk advancement");
            }
            return iterator.nextLong();
        }

        @Override
        public void advance(final long n) throws IOException {
            if (++advanceCalls == failOnAdvance) {
                throw failure;
            }
            iterator.advance(n);
        }

        @Override
        protected void closeResourceInternal() {
            stream.close();
        }
    }

}
