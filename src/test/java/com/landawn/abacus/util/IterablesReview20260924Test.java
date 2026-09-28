package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;

/**
 * Tests for the Iterables findings of the 2026-09-24 review (ledger IDs C-156, C-311, C-312, C-317..C-320, C-335).
 */
public class IterablesReview20260924Test extends TestBase {

    // ---------------------------------------------------------------------------------------------
    // C-156: Iterables.Slice (N.slice over a non-List collection) must stream / forEach only its range
    // ---------------------------------------------------------------------------------------------

    private static <T> void assertSliceTraversals(final Collection<T> slice, final List<T> expected) {
        assertEquals(expected, new ArrayList<>(slice), "iterator");
        assertEquals(expected.size(), slice.size(), "size");

        final List<T> seen = new ArrayList<>();
        slice.forEach(seen::add);
        assertEquals(expected, seen, "forEach");

        assertEquals(expected, slice.stream().collect(Collectors.toList()), "stream");
        assertEquals(expected.size(), slice.stream().count(), "stream().count()");
        assertEquals(expected, slice.parallelStream().collect(Collectors.toList()), "parallelStream");
        assertEquals(expected.size(), slice.spliterator().estimateSize(), "spliterator().estimateSize()");
        assertEquals(expected.size(), slice.spliterator().getExactSizeIfKnown(), "spliterator().getExactSizeIfKnown()");
        assertTrue(slice.spliterator().hasCharacteristics(Spliterator.ORDERED));
        assertFalse(slice.spliterator().hasCharacteristics(Spliterator.SORTED));
    }

    @Test
    public void testC156_linkedHashSetSliceStreamsOnlyItsRange() {
        final Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e"));

        assertSliceTraversals(N.slice(set, 1, 3), Arrays.asList("b", "c"));
        assertSliceTraversals(N.slice(set, 0, 2), Arrays.asList("a", "b"));
        assertSliceTraversals(N.slice(set, 3, 5), Arrays.asList("d", "e"));
    }

    @Test
    public void testC156_treeSetSliceStreamsOnlyItsRange() {
        final TreeSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));
        final Collection<Integer> slice = N.slice(set, 3, 5);

        assertSliceTraversals(slice, Arrays.asList(4, 5));
        assertEquals(9, slice.stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    public void testC156_arrayDequeSliceStreamsOnlyItsRange() {
        final ArrayDeque<Integer> deque = new ArrayDeque<>(Arrays.asList(1, 2, 3, 4, 5));

        assertSliceTraversals(N.slice(deque, 0, 2), Arrays.asList(1, 2));
        assertSliceTraversals(N.slice(deque, 1, 4), Arrays.asList(2, 3, 4));
    }

    @Test
    public void testC156_emptyAndFullRange() {
        final Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e"));

        assertSliceTraversals(N.slice(set, 2, 2), Collections.<String> emptyList());
        assertSliceTraversals(N.slice(set, 0, 0), Collections.<String> emptyList());
        assertSliceTraversals(N.slice(set, 5, 5), Collections.<String> emptyList());
        assertSliceTraversals(N.slice(set, 0, 5), Arrays.asList("a", "b", "c", "d", "e"));
    }

    @Test
    public void testC156_unicodeAndNullElements() {
        final Set<String> set = new LinkedHashSet<>(Arrays.asList("été", null, "中文", "😀", "z"));

        assertSliceTraversals(N.slice(set, 1, 4), Arrays.asList(null, "中文", "😀"));
    }

    @Test
    public void testC156_forEachNullActionThrowsNpe() {
        final Collection<String> slice = N.slice(new LinkedHashSet<>(Arrays.asList("a", "b", "c")), 1, 2);

        assertThrows(NullPointerException.class, () -> slice.forEach(null));
    }

    @Test
    public void testC156_backingSetShrinksAfterSliceWasCreated() {
        final Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e"));
        final Collection<String> slice = N.slice(set, 1, 4);
        final Spliterator<String> earlySpliterator = slice.spliterator(); // late-binding: created before the change

        set.remove("a");

        final List<String> viaIterator = new ArrayList<>(slice);
        assertSliceTraversals(slice, viaIterator);

        final List<String> viaEarlySpliterator = new ArrayList<>();
        earlySpliterator.forEachRemaining(viaEarlySpliterator::add);
        assertEquals(viaIterator, viaEarlySpliterator);

        set.clear();
        assertSliceTraversals(slice, Collections.<String> emptyList());
    }

    // ---------------------------------------------------------------------------------------------
    // C-311: Number-typed sumInt/sumIntToLong/averageInt/averageLong convert each element exactly
    // ---------------------------------------------------------------------------------------------

    private static final BigInteger TWO_POW_64_PLUS_1 = BigInteger.ONE.shiftLeft(64).add(BigInteger.ONE);

    /** Runs every Number-typed int overload (Iterable, Iterable->long, T[], T[] range, Collection range, Iterable average). */
    private static void assertAllIntShapesThrow(final Number... values) {
        final List<Number> list = Arrays.asList(values);
        final String msg = list.toString();

        assertThrows(ArithmeticException.class, () -> Iterables.sumInt(list), msg);
        assertThrows(ArithmeticException.class, () -> Iterables.sumIntToLong(list), msg);
        assertThrows(ArithmeticException.class, () -> Iterables.averageInt(values), msg);
        assertThrows(ArithmeticException.class, () -> Iterables.averageInt(values, 0, values.length), msg);
        assertThrows(ArithmeticException.class, () -> Iterables.averageInt(list, 0, values.length), msg);
        assertThrows(ArithmeticException.class, () -> Iterables.averageInt(new LinkedList<>(list), 0, values.length), msg);
        assertThrows(ArithmeticException.class, () -> Iterables.averageInt(list), msg);
    }

    private static void assertAllLongShapesThrow(final Number... values) {
        final List<Number> list = Arrays.asList(values);
        final String msg = list.toString();

        assertThrows(ArithmeticException.class, () -> Iterables.averageLong(values), msg);
        assertThrows(ArithmeticException.class, () -> Iterables.averageLong(values, 0, values.length), msg);
        assertThrows(ArithmeticException.class, () -> Iterables.averageLong(list, 0, values.length), msg);
        assertThrows(ArithmeticException.class, () -> Iterables.averageLong(new LinkedList<>(list), 0, values.length), msg);
        assertThrows(ArithmeticException.class, () -> Iterables.averageLong(list), msg);
    }

    @Test
    public void testC311_outOfIntRangeElementThrowsInsteadOfWrapping() {
        assertAllIntShapesThrow(3_000_000_000L);
        assertAllIntShapesThrow(4294967297L, 1L);
        assertAllIntShapesThrow(1, -3_000_000_000L);
        assertAllIntShapesThrow(BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE));
        assertAllIntShapesThrow(new BigDecimal("1e19"));
        assertAllIntShapesThrow(3e9);
        assertAllIntShapesThrow(2147483648.0f);
        assertAllIntShapesThrow(Double.NaN, 4.0);
        assertAllIntShapesThrow(Double.POSITIVE_INFINITY);
        assertAllIntShapesThrow(Float.NEGATIVE_INFINITY);
        assertAllIntShapesThrow(new java.util.concurrent.atomic.AtomicLong(3_000_000_000L));
    }

    @Test
    public void testC311_outOfLongRangeElementThrowsInsteadOfWrapping() {
        assertAllLongShapesThrow(TWO_POW_64_PLUS_1);
        assertAllLongShapesThrow(1L, TWO_POW_64_PLUS_1.negate());
        assertAllLongShapesThrow(new BigDecimal("1e19"));
        assertAllLongShapesThrow(1e19);
        assertAllLongShapesThrow(Double.NaN, 4.0);
        assertAllLongShapesThrow(Double.NEGATIVE_INFINITY);
    }

    @Test
    public void testC311_fractionsStillTruncateAndNullCountsAsZero() {
        final List<Double> decimals = Arrays.asList(1.7, 2.3, 3.9);

        assertEquals(6, Iterables.sumInt(decimals).getAsInt());
        assertEquals(6L, Iterables.sumIntToLong(decimals).getAsLong());
        assertEquals(2.0, Iterables.averageInt(decimals).getAsDouble());
        assertEquals(2.0, Iterables.averageLong(decimals).getAsDouble());
        assertEquals(2.0, Iterables.averageInt(decimals.toArray(new Double[0])).getAsDouble());
        assertEquals(2.0, Iterables.averageLong(decimals, 0, 3).getAsDouble());
        assertEquals(-1, Iterables.sumInt(Arrays.asList(-1.9)).getAsInt());

        final List<Integer> withNull = Arrays.asList(null, 2);
        assertEquals(2, Iterables.sumInt(withNull).getAsInt());
        assertEquals(2L, Iterables.sumIntToLong(withNull).getAsLong());
        assertEquals(1.0, Iterables.averageInt(withNull).getAsDouble());
        assertEquals(1.0, Iterables.averageLong(withNull).getAsDouble());
        assertEquals(1.0, Iterables.averageInt(new Integer[] { null, 2 }, 0, 2).getAsDouble());
        assertEquals(1.0, Iterables.averageLong(new Long[] { null, 2L }).getAsDouble());
        assertEquals(1.0, Iterables.averageLong(new LinkedList<>(withNull), 0, 2).getAsDouble());
    }

    @Test
    public void testC311_boundariesAndMixedInRangeNumbersUnchanged() {
        assertEquals(Integer.MAX_VALUE, Iterables.sumInt(Arrays.asList(2147483647.9)).getAsInt());
        assertEquals(Integer.MIN_VALUE, Iterables.sumInt(Arrays.asList(-2147483648.9)).getAsInt());
        assertEquals(Integer.MAX_VALUE, Iterables.sumInt(Arrays.asList(Long.valueOf(Integer.MAX_VALUE))).getAsInt());
        assertEquals((double) Long.MAX_VALUE, Iterables.averageLong(Arrays.asList(BigInteger.valueOf(Long.MAX_VALUE))).getAsDouble());
        assertEquals((double) Long.MIN_VALUE, Iterables.averageLong(new Number[] { new BigDecimal(Long.MIN_VALUE) }).getAsDouble());

        // sumIntToLong's total may exceed int (only each ELEMENT must fit)
        assertEquals(2L * Integer.MAX_VALUE, Iterables.sumIntToLong(Arrays.asList(Integer.MAX_VALUE, (long) Integer.MAX_VALUE)).getAsLong());
        // the sum overflow check of sumInt is unchanged
        assertThrows(ArithmeticException.class, () -> Iterables.sumInt(Arrays.asList(Integer.MAX_VALUE, 1)));

        final List<Number> mixed = Arrays.asList((byte) 1, (short) 2, 3, 4L, 5.5f, 6.5, BigInteger.valueOf(7), new BigDecimal("8.9"),
                new java.util.concurrent.atomic.AtomicInteger(9), new java.util.concurrent.atomic.AtomicLong(10));
        assertEquals(55, Iterables.sumInt(mixed).getAsInt());
        assertEquals(55L, Iterables.sumIntToLong(mixed).getAsLong());
        assertEquals(5.5, Iterables.averageInt(mixed).getAsDouble());
        assertEquals(5.5, Iterables.averageLong(mixed).getAsDouble());
        assertEquals(5.5, Iterables.averageInt(mixed, 0, 10).getAsDouble());
        assertEquals(5.5, Iterables.averageLong(mixed.toArray(new Number[0]), 0, 10).getAsDouble());

        // empty / null inputs are unchanged
        assertFalse(Iterables.sumInt((Iterable<Number>) null).isPresent());
        assertFalse(Iterables.averageLong(Collections.<Number> emptyList()).isPresent());
        assertFalse(Iterables.averageInt(new Number[0]).isPresent());
        assertFalse(Iterables.averageLong((Number[]) null, 0, 0).isPresent());
    }

    @Test
    public void testC311_sumLongStillWrapsItsTotalButFnNumToIntIsUnchanged() {
        // not part of the fix: sumLong keeps longValue() per element and a wrapping total (documented)
        assertEquals(Long.MIN_VALUE, Iterables.sumLong(Arrays.asList(Long.MAX_VALUE, 1L)).getAsLong());
        // the public Fn.numToInt() contract (intValue()) is untouched
        assertEquals((int) 3_000_000_000L, Fn.<Long> numToInt().applyAsInt(3_000_000_000L));
    }

    // ---------------------------------------------------------------------------------------------
    // C-312: minBy / maxBy apply keyExtractor once per element
    // ---------------------------------------------------------------------------------------------

    private static final class CountingKey implements Function<String, Integer> {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public Integer apply(final String s) {
            calls.incrementAndGet();
            return s == null ? null : s.length();
        }
    }

    @Test
    public void testC312_minByAndMaxByCallKeyExtractorOncePerElement() {
        final List<String> list = Arrays.asList("apple", "fig", "banana", "kiwi", "cherry", "plum", "date", "pear", "melon", "watermelon");
        final String[] array = list.toArray(new String[0]);

        CountingKey key = new CountingKey();
        assertEquals("fig", Iterables.minBy(list, key).get());
        assertEquals(10, key.calls.get());

        key = new CountingKey();
        assertEquals("fig", Iterables.minBy(array, key).get());
        assertEquals(10, key.calls.get());

        key = new CountingKey();
        assertEquals("fig", Iterables.minBy(list.iterator(), key).get());
        assertEquals(10, key.calls.get());

        key = new CountingKey();
        assertEquals("watermelon", Iterables.maxBy(list, key).get());
        assertEquals(10, key.calls.get());

        key = new CountingKey();
        assertEquals("watermelon", Iterables.maxBy(array, key).get());
        assertEquals(10, key.calls.get());

        key = new CountingKey();
        assertEquals("watermelon", Iterables.maxBy(list.iterator(), key).get());
        assertEquals(10, key.calls.get());
    }

    @Test
    public void testC312_singleElementNeverCallsKeyExtractor() {
        final CountingKey key = new CountingKey();

        assertEquals("only", Iterables.minBy(new String[] { "only" }, key).get());
        assertEquals("only", Iterables.maxBy(Collections.singletonList("only"), key).get());
        assertEquals(0, key.calls.get());

        // a lone null element is returned without being handed to the extractor
        final Nullable<String> lone = Iterables.minBy(new String[] { null }, String::length);
        assertTrue(lone.isPresent());
        assertEquals(null, lone.orElseNull());
    }

    @Test
    public void testC312_nullAndEmptyInputs() {
        final CountingKey key = new CountingKey();

        assertFalse(Iterables.minBy((String[]) null, key).isPresent());
        assertFalse(Iterables.minBy(new String[0], key).isPresent());
        assertFalse(Iterables.minBy((Iterable<String>) null, key).isPresent());
        assertFalse(Iterables.minBy(Collections.<String> emptyList(), key).isPresent());
        assertFalse(Iterables.minBy((Iterator<String>) null, key).isPresent());
        assertFalse(Iterables.maxBy((String[]) null, key).isPresent());
        assertFalse(Iterables.maxBy(Collections.<String> emptyList(), key).isPresent());
        assertFalse(Iterables.maxBy(Collections.<String> emptyIterator(), key).isPresent());
        assertEquals(0, key.calls.get());

        assertThrows(IllegalArgumentException.class, () -> Iterables.minBy(Arrays.asList("a"), null));
        assertThrows(IllegalArgumentException.class, () -> Iterables.maxBy(new String[] { "a" }, null));
        assertThrows(IllegalArgumentException.class, () -> Iterables.maxBy((Iterator<String>) null, null));
    }

    @Test
    public void testC312_nullKeysAreMaxForMinByAndMinForMaxBy() {
        final Function<String, Integer> keyOrNull = s -> s.startsWith("n") ? null : s.length();
        final List<String> list = Arrays.asList("nothing", "abc", "nada", "z", "no");

        assertEquals("z", Iterables.minBy(list, keyOrNull).get());
        assertEquals("abc", Iterables.maxBy(list, keyOrNull).get());

        // all keys null: the first element wins in both directions
        final List<String> allNull = Arrays.asList("n1", "n2", "n3");
        assertEquals("n1", Iterables.minBy(allNull, keyOrNull).get());
        assertEquals("n1", Iterables.maxBy(allNull, keyOrNull).get());
        assertEquals("n1", Iterables.minBy(allNull.iterator(), keyOrNull).get());
        assertEquals("n1", Iterables.maxBy(allNull.toArray(new String[0]), keyOrNull).get());

        // a null element is passed to the extractor as-is
        assertThrows(NullPointerException.class, () -> Iterables.minBy(Arrays.asList("a", null), String::length));
    }

    @Test
    public void testC312_tiesKeepTheFirstElement() {
        final String first = new String("ab");
        final String second = new String("ab");
        final String third = new String("cd");
        final List<String> list = Arrays.asList(first, second, third);

        assertSame(first, Iterables.minBy(list, String::length).get());
        assertSame(first, Iterables.maxBy(list, String::length).get());
        assertSame(first, Iterables.minBy(list.toArray(new String[0]), String::length).get());
        assertSame(first, Iterables.maxBy(list.iterator(), String::length).get());
    }

    @Test
    public void testC312_unicodeKeys() {
        final List<String> list = Arrays.asList("中", "é", "a", "😀");

        assertEquals("a", Iterables.minBy(list, Function.identity()).get());
        assertEquals("😀", Iterables.maxBy(list, Function.identity()).get());
        assertEquals(N.min(list), Iterables.minBy(list, Function.identity()).get());
    }

    @Test
    public void testC312_stableKeyPerElementForStatefulExtractor() {
        // A key function whose answer changes between calls: every element is keyed exactly once, so the result
        // follows those single keys. Call order matches the old comparator's (the second element is keyed before the
        // first), so the keys are b=1, a=2, c=3, d=4 and the minimum is "b". Re-keying the candidate on every
        // comparison used to give "d" after 6 calls.
        final AtomicInteger counter = new AtomicInteger();
        final Function<String, Integer> drifting = s -> counter.incrementAndGet();

        assertEquals("b", Iterables.minBy(Arrays.asList("a", "b", "c", "d"), drifting).get());
        assertEquals(4, counter.get());
    }

    // ---------------------------------------------------------------------------------------------
    // C-317: union / symmetricDifference copyInto(set2) must not fail with ConcurrentModificationException
    // ---------------------------------------------------------------------------------------------

    private static List<Supplier<Set<Integer>>> setFactories() {
        return Arrays.asList(HashSet::new, LinkedHashSet::new, TreeSet::new);
    }

    @Test
    public void testC317_unionCopyIntoSecondBackingSet() {
        for (final Supplier<Set<Integer>> f : setFactories()) {
            final Set<Integer> a = f.get();
            a.addAll(Arrays.asList(1, 2, 3));
            final Set<Integer> b = f.get();
            b.addAll(Arrays.asList(3, 4, 5));

            assertSame(b, Iterables.union(a, b).copyInto(b));
            assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5)), b);
        }
    }

    @Test
    public void testC317_unionAddAllIntoSecondBackingSet() {
        for (final Supplier<Set<Integer>> f : setFactories()) {
            final Set<Integer> a = f.get();
            a.addAll(Arrays.asList(1, 2, 3));
            final Set<Integer> b = f.get();
            b.addAll(Arrays.asList(3, 4, 5));

            b.addAll(Iterables.union(a, b));
            assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5)), b);
        }
    }

    @Test
    public void testC317_symmetricDifferenceCopyIntoSecondBackingSet() {
        for (final Supplier<Set<Integer>> f : setFactories()) {
            final Set<Integer> a = f.get();
            a.addAll(Arrays.asList(1, 2, 3));
            final Set<Integer> b = f.get();
            b.addAll(Arrays.asList(3, 4, 5));

            assertSame(b, Iterables.symmetricDifference(a, b).copyInto(b));
            assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5)), b);
        }
    }

    @Test
    public void testC317_copyIntoFirstBackingSetStillWorks() {
        final Set<Integer> a = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        final Set<Integer> b = new LinkedHashSet<>(Arrays.asList(3, 4, 5));

        Iterables.union(a, b).copyInto(a);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), new ArrayList<>(a));

        final Set<Integer> c = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        final Set<Integer> d = new LinkedHashSet<>(Arrays.asList(3, 4, 5));
        Iterables.symmetricDifference(c, d).copyInto(c);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), new ArrayList<>(c));
    }

    @Test
    public void testC317_secondSetIteratorNotCreatedWhenIterationStopsInFirstPhase() {
        final AtomicInteger iteratorCalls = new AtomicInteger();
        final Set<String> set2 = new LinkedHashSet<>(Arrays.asList("中", "x")) {
            @Override
            public Iterator<String> iterator() {
                iteratorCalls.incrementAndGet();
                return super.iterator();
            }
        };
        final Set<String> set1 = new LinkedHashSet<>(Arrays.asList("é", "x"));

        assertEquals("é", Iterables.union(set1, set2).iterator().next());
        assertEquals("é", Iterables.symmetricDifference(set1, set2).iterator().next());
        assertEquals(0, iteratorCalls.get());

        // the full iteration is unchanged
        assertEquals(Arrays.asList("é", "x", "中"), new ArrayList<>(Iterables.union(set1, set2)));
        assertEquals(Arrays.asList("é", "中"), new ArrayList<>(Iterables.symmetricDifference(set1, set2)));
    }

    @Test
    public void testC317_emptyAndNullShapes() {
        final Set<Integer> a = new LinkedHashSet<>(Arrays.asList(1, 2));
        final Set<Integer> empty = new LinkedHashSet<>();

        assertEquals(Arrays.asList(1, 2), new ArrayList<>(Iterables.union(a, empty).copyInto(empty)));
        assertEquals(Arrays.asList(1, 2), new ArrayList<>(Iterables.union(a, null)));
        assertEquals(Arrays.asList(1, 2), new ArrayList<>(Iterables.symmetricDifference(null, a)));

        final Set<Integer> empty2 = new LinkedHashSet<>();
        assertEquals(Arrays.asList(1, 2), new ArrayList<>(Iterables.symmetricDifference(a, empty2).copyInto(empty2)));

        final Iterator<Integer> it = Iterables.union(new HashSet<Integer>(), new HashSet<Integer>()).iterator();
        assertFalse(it.hasNext());
        assertThrows(java.util.NoSuchElementException.class, it::next);
    }

    // ---------------------------------------------------------------------------------------------
    // C-318: fill(List, from, to, Supplier) must not use per-index set(i, ..) on a LinkedList when extending
    // ---------------------------------------------------------------------------------------------

    private static final class CountingLinkedList<E> extends LinkedList<E> {
        private static final long serialVersionUID = 1L;
        int indexedSets = 0;

        CountingLinkedList(final Collection<? extends E> c) {
            super(c);
        }

        @Override
        public E set(final int index, final E element) {
            indexedSets++;
            return super.set(index, element);
        }
    }

    @Test
    public void testC318_extendingFillOfLinkedListAvoidsIndexedSet() {
        final int n = 1000;
        final CountingLinkedList<Integer> list = new CountingLinkedList<>(Collections.nCopies(n, 0));
        final AtomicInteger supplied = new AtomicInteger();

        Iterables.fill(list, 0, n + 1, () -> supplied.incrementAndGet());

        assertEquals(0, list.indexedSets);
        assertEquals(n + 1, list.size());
        assertEquals(n + 1, supplied.get());

        for (int i = 0; i < n + 1; i++) {
            assertEquals(i + 1, list.get(i));
        }
    }

    @Test
    public void testC318_extendingFillResultsMatchReference() {
        for (final int size : new int[] { 0, 1, 5, 24, 25, 26, 60 }) {
            for (final int from : new int[] { 0, 1, 3, 30 }) {
                for (final int extra : new int[] { 0, 1, 7, 40 }) {
                    final int to = Math.max(from, size) + extra;

                    if (from > to) {
                        continue;
                    }

                    final List<String> expected = new ArrayList<>(Collections.nCopies(size, "o"));
                    final AtomicInteger c1 = new AtomicInteger();

                    for (int i = expected.size(); i < from; i++) {
                        expected.add(null);
                    }

                    for (int i = from; i < to; i++) {
                        final String v = "é" + c1.getAndIncrement();

                        if (i < expected.size()) {
                            expected.set(i, v);
                        } else {
                            expected.add(v);
                        }
                    }

                    for (final Supplier<List<String>> f : Arrays.<Supplier<List<String>>> asList(ArrayList::new, LinkedList::new)) {
                        final List<String> actual = f.get();
                        actual.addAll(Collections.nCopies(size, "o"));
                        final AtomicInteger c2 = new AtomicInteger();

                        Iterables.fill(actual, from, to, () -> "é" + c2.getAndIncrement());

                        assertEquals(expected, actual, "size=" + size + " from=" + from + " to=" + to + " " + actual.getClass());
                    }
                }
            }
        }
    }

    @Test
    public void testC318_fixedSizeListFilledInPlaceThenUoeOnExtension() {
        final List<String> fixed = Arrays.asList("a", "b", "c");

        assertThrows(UnsupportedOperationException.class, () -> Iterables.fill(fixed, 1, 4, () -> "x"));
        assertEquals(Arrays.asList("a", "x", "x"), fixed);

        assertThrows(IllegalArgumentException.class, () -> Iterables.fill((List<String>) null, 0, 1, () -> "x"));
        assertThrows(IllegalArgumentException.class, () -> Iterables.fill(new LinkedList<String>(), 0, 1, null));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(new LinkedList<String>(), 2, 1, () -> "x"));
    }

    // ---------------------------------------------------------------------------------------------
    // C-319: CartesianList indexOf / lastIndexOf probe the axes instead of walking the product
    // ---------------------------------------------------------------------------------------------

    private static final class CountingEq {
        static final AtomicInteger EQUALS_CALLS = new AtomicInteger();
        final String v;

        CountingEq(final String v) {
            this.v = v;
        }

        @Override
        public boolean equals(final Object o) {
            EQUALS_CALLS.incrementAndGet();
            return o instanceof CountingEq && ((CountingEq) o).v.equals(v);
        }

        @Override
        public int hashCode() {
            return v.hashCode();
        }

        @Override
        public String toString() {
            return v;
        }
    }

    @Test
    public void testC319_indexOfDoesNotWalkTheProduct() {
        final List<CountingEq> axis0 = new ArrayList<>();
        final List<CountingEq> axis1 = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            axis0.add(new CountingEq("a" + i));
            axis1.add(new CountingEq("b" + i));
        }

        final List<List<CountingEq>> product = Iterables.cartesianProduct(axis0, axis1);
        final List<CountingEq> last = Arrays.asList(new CountingEq("a99"), new CountingEq("b99"));

        CountingEq.EQUALS_CALLS.set(0);
        assertEquals(9999, product.indexOf(last));
        assertTrue(CountingEq.EQUALS_CALLS.get() <= 200, "equals calls: " + CountingEq.EQUALS_CALLS.get());

        CountingEq.EQUALS_CALLS.set(0);
        assertEquals(0, product.lastIndexOf(Arrays.asList(new CountingEq("a0"), new CountingEq("b0"))));
        assertTrue(CountingEq.EQUALS_CALLS.get() <= 200, "equals calls: " + CountingEq.EQUALS_CALLS.get());

        CountingEq.EQUALS_CALLS.set(0);
        assertEquals(-1, product.indexOf(Arrays.asList(new CountingEq("zz"), new CountingEq("b0"))));
        assertTrue(CountingEq.EQUALS_CALLS.get() <= 200, "equals calls: " + CountingEq.EQUALS_CALLS.get());
    }

    private static int referenceIndexOf(final List<List<Object>> product, final Object o, final boolean last) {
        int found = -1;

        for (int i = 0, n = product.size(); i < n; i++) {
            if (o != null && o.equals(product.get(i))) {
                found = i;

                if (!last) {
                    break;
                }
            }
        }

        return found;
    }

    @Test
    public void testC319_indexOfAndLastIndexOfMatchReference() {
        final List<Object> ax0 = Arrays.asList(1, 2, 1, null);
        final List<Object> ax1 = Arrays.asList("中", "b", "中");
        final List<Object> ax2 = Arrays.asList(true, false);
        final List<List<Object>> product = Iterables.<Object> cartesianProduct(ax0, ax1, ax2);

        final List<Object> probes = new ArrayList<>();
        probes.add(null);
        probes.add("not a list");
        probes.add(Arrays.asList(1, "中"));
        probes.add(Arrays.asList(1, "中", true, 1));
        probes.add(Arrays.asList(3, "中", true));
        probes.add(new LinkedList<>(Arrays.asList(null, "b", false)));

        for (final Object a : ax0) {
            for (final Object b : ax1) {
                for (final Object c : ax2) {
                    probes.add(Arrays.asList(a, b, c));
                }
            }
        }

        for (final Object p : probes) {
            assertEquals(referenceIndexOf(product, p, false), product.indexOf(p), "indexOf " + p);
            assertEquals(referenceIndexOf(product, p, true), product.lastIndexOf(p), "lastIndexOf " + p);
            assertEquals(product.indexOf(p) >= 0, product.contains(p), "contains " + p);
        }

        assertEquals(0, product.indexOf(Arrays.asList(1, "中", true)));
        // last 1 on axis 0 is position 2, last "中" on axis 1 is position 2, false is position 1: 2*6 + 2*2 + 1
        assertEquals(17, product.lastIndexOf(Arrays.asList(1, "中", false)));
        assertEquals(24, product.size());
    }

    @Test
    public void testC319_emptyAndDegenerateProducts() {
        final List<List<Object>> withEmptyAxis = Iterables.<Object> cartesianProduct(Arrays.asList(1, 2), Collections.emptyList());
        assertEquals(0, withEmptyAxis.size());
        assertEquals(-1, withEmptyAxis.indexOf(Arrays.asList(1, 2)));
        assertEquals(-1, withEmptyAxis.lastIndexOf(Arrays.asList(1, null)));

        final List<List<Object>> noAxes = Iterables.<Object> cartesianProduct(Collections.<Collection<Object>> emptyList());
        assertEquals(1, noAxes.size());
        assertEquals(0, noAxes.indexOf(Collections.emptyList()));
        assertEquals(0, noAxes.lastIndexOf(new ArrayList<>()));
        assertEquals(-1, noAxes.indexOf(Arrays.asList(1)));

        final List<List<Object>> single = Iterables.<Object> cartesianProduct(Arrays.asList("x", "y", "x"));
        assertEquals(0, single.indexOf(Arrays.asList("x")));
        assertEquals(2, single.lastIndexOf(Arrays.asList("x")));
        assertEquals(1, single.indexOf(Arrays.asList("y")));
    }

    // ---------------------------------------------------------------------------------------------
    // C-320: SetView.containsAll forwards to the backing set, so intersection's override is reachable
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testC320_intersectionContainsAllUsesBackingSets() {
        final AtomicInteger containsAllCalls = new AtomicInteger();
        final Set<String> set1 = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "中")) {
            @Override
            public boolean containsAll(final Collection<?> c) {
                containsAllCalls.incrementAndGet();
                return super.containsAll(c);
            }
        };
        final Set<String> set2 = new LinkedHashSet<>(Arrays.asList("b", "c", "d", "中"));
        final Iterables.SetView<String> view = Iterables.intersection(set1, set2);

        assertTrue(view.containsAll(Arrays.asList("b", "中")));
        assertEquals(1, containsAllCalls.get());
        assertFalse(view.containsAll(Arrays.asList("b", "a")));
        assertFalse(view.containsAll(Arrays.asList("d")));
        assertTrue(view.containsAll(Collections.emptyList()));
        assertThrows(NullPointerException.class, () -> view.containsAll(null));
    }

    @Test
    public void testC320_containsAllMatchesContainsForEveryView() {
        final Set<Integer> s1 = new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4));
        final Set<Integer> s2 = new LinkedHashSet<>(Arrays.asList(3, 4, 5, 6));
        final List<Iterables.SetView<Integer>> views = Arrays.asList(Iterables.union(s1, s2), Iterables.intersection(s1, s2), Iterables.difference(s1, s2),
                Iterables.symmetricDifference(s1, s2), Iterables.union(null, s2), Iterables.intersection(null, s2), Iterables.difference(s1, null),
                Iterables.symmetricDifference(s1, null));
        final List<List<Integer>> probes = Arrays.asList(Collections.<Integer> emptyList(), Arrays.asList(1), Arrays.asList(3, 4), Arrays.asList(5, 6),
                Arrays.asList(1, 6), Arrays.asList(1, 2, 3, 4, 5, 6), Arrays.asList(7), Arrays.asList((Integer) null));

        for (final Iterables.SetView<Integer> v : views) {
            for (final List<Integer> p : probes) {
                boolean expected = true;

                for (final Integer e : p) {
                    expected &= v.contains(e);
                }

                assertEquals(expected, v.containsAll(p), v + " containsAll " + p);
            }

            assertTrue(v.containsAll(v));
            assertEquals(new HashSet<>(v), v);
            assertEquals(v, new HashSet<>(v));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // C-335 (doc-only): pins for the documented copyInto(List, int, List, int, int) edges
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testC335_nullListWithPositiveLengthThrowsIoobe() {
        final List<String> list = new ArrayList<>(Arrays.asList("a", "b"));

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.copyInto(null, 0, list, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.copyInto(list, 0, null, 0, 1));
        Iterables.copyInto(null, 0, null, 0, 0);
        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    public void testC335_sameListObjectOverlapIsSafeButSharedViewsAreNot() {
        final List<Integer> same = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5));
        Iterables.copyInto(same, 0, same, 1, 5);
        assertEquals(Arrays.asList(0, 0, 1, 2, 3, 4), same);

        final List<Integer> base = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5));
        Iterables.copyInto(base.subList(0, 5), 0, base.subList(1, 6), 0, 5);
        assertEquals(Arrays.asList(0, 0, 0, 0, 0, 0), base); // documented: overlap between distinct views is not detected
    }
}
