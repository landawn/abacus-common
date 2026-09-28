package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.XmlSerConfig;
import com.landawn.abacus.util.function.IntFunction;

/**
 * Tests for the 2026-09-24 review of {@link N} (ledger IDs C-201 .. C-278, C-311).
 */
public class NReview20260924Test extends com.landawn.abacus.TestBase {

    // ------------------------------------------------------------------ helpers

    /** A collection whose size() claims more elements than its iterator yields. */
    private static final class LyingSizeCollection<E> extends java.util.AbstractCollection<E> {
        private final List<E> actual;
        private final int claimedSize;

        LyingSizeCollection(final int claimedSize, final List<E> actual) {
            this.actual = actual;
            this.claimedSize = claimedSize;
        }

        @Override
        public Iterator<E> iterator() {
            return actual.iterator();
        }

        @Override
        public int size() {
            return claimedSize;
        }
    }

    /** A list that supports removal but not add/set: like a Guava transform view. */
    private static final class ShrinkOnlyList<E> extends AbstractList<E> {
        private final List<E> store;

        ShrinkOnlyList(final List<E> store) {
            this.store = new ArrayList<>(store);
        }

        @Override
        public E get(final int index) {
            return store.get(index);
        }

        @Override
        public E remove(final int index) {
            return store.remove(index);
        }

        @Override
        public int size() {
            return store.size();
        }
    }

    private static final class CountingArrayList<E> extends ArrayList<E> {
        private static final long serialVersionUID = 1L;
        int setCalls;
        int removeAtCalls;
        int containsCalls;
        int iteratorCalls;

        CountingArrayList(final Collection<? extends E> c) {
            super(c);
        }

        @Override
        public E set(final int index, final E element) {
            setCalls++;
            return super.set(index, element);
        }

        @Override
        public E remove(final int index) {
            removeAtCalls++;
            return super.remove(index);
        }

        @Override
        public boolean contains(final Object o) {
            containsCalls++;
            return super.contains(o);
        }

        @Override
        public Iterator<E> iterator() {
            iteratorCalls++;
            return super.iterator();
        }
    }

    /** Equal by id only; label identifies the instance. */
    private static final class Keyed {
        final int id;
        final String label;

        Keyed(final int id, final String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public boolean equals(final Object o) {
            return o instanceof Keyed k && k.id == id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            return id + ":" + label;
        }
    }

    private static List<String> labels(final List<Keyed> list) {
        return list.stream().map(k -> k.label).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ C-201

    @Test
    public void testC201_mapToXFromShrinkingCollectionIsTruncatedNotZeroFilled() {
        final Collection<Integer> c = new LyingSizeCollection<>(4, Arrays.asList(1, 2));

        assertArrayEquals(new int[] { 10, 20 }, N.mapToInt(c, x -> x * 10));
        assertArrayEquals(new int[] { 20 }, N.mapToInt(c, 1, 4, x -> x * 10));
        assertArrayEquals(new boolean[] { true, true }, N.mapToBoolean(c, x -> x > 0));
        assertArrayEquals(new char[] { 'b', 'c' }, N.mapToChar(c, x -> (char) ('a' + x)));
        assertArrayEquals(new byte[] { 1, 2 }, N.mapToByte(c, Integer::byteValue));
        assertArrayEquals(new short[] { 1, 2 }, N.mapToShort(c, Integer::shortValue));
        assertArrayEquals(new long[] { 1L, 2L }, N.mapToLong(c, Integer::longValue));
        assertArrayEquals(new float[] { 1.5f, 3f }, N.mapToFloat(c, x -> x * 1.5f));
        assertArrayEquals(new double[] { 1.5, 3.0 }, N.mapToDouble(c, x -> x * 1.5));

        // yields fewer elements than fromIndex: empty
        assertArrayEquals(new int[0], N.mapToInt(c, 3, 4, x -> x));
        assertArrayEquals(new double[0], N.mapToDouble(c, 2, 4, x -> x));
        // yields nothing
        assertArrayEquals(new long[0], N.mapToLong(new LyingSizeCollection<Integer>(3, Collections.emptyList()), Integer::longValue));
        // fromIndex == toIndex
        assertArrayEquals(new int[0], N.mapToInt(c, 2, 2, x -> x));
        // an iterator yielding MORE than size() is cut at toIndex, as before
        assertArrayEquals(new int[] { 1, 2 }, N.mapToInt(new LyingSizeCollection<>(2, Arrays.asList(1, 2, 3)), x -> x));
        // honest non-RandomAccess input is unchanged
        assertArrayEquals(new int[] { 2, 3 }, N.mapToInt(new LinkedList<>(Arrays.asList(1, 2, 3)), 1, 3, x -> x));
    }

    // ------------------------------------------------------------------ C-202 / C-205 / C-207 (doc examples)

    @Test
    public void testC202_C205_C207_documentedExamplesHold() {
        final String[] words = { "Hi", "Hello" };
        final Set<Character> chars = N.flatMap(words, s -> s.chars().mapToObj(ch -> (char) ch).collect(Collectors.toList()),
                (IntFunction<Set<Character>>) HashSet::new);
        assertEquals(new HashSet<>(Arrays.asList('H', 'i', 'e', 'l', 'o')), chars);

        // C-205: a null mapped collection is treated as empty
        assertEquals(Arrays.asList("b"), N.flatMap(new String[] { "a", "b" }, s -> "a".equals(s) ? null : Arrays.asList(s)));

        // C-207: the new distinctBy examples actually dedupe
        assertEquals(Arrays.asList("apple", "kiwi"), new ArrayList<>(N.distinctBy(new String[] { "apple", "grape", "kiwi" }, String::length, LinkedHashSet::new)));
        assertEquals(Arrays.asList("apple", "kiwi"),
                new ArrayList<>(N.distinctBy(Arrays.asList("apple", "grape", "kiwi"), String::length, LinkedHashSet::new)));
        assertEquals(Arrays.asList("apple", "kiwi"),
                new ArrayList<>(N.distinctBy(Arrays.asList("apple", "grape", "kiwi").iterator(), String::length, LinkedHashSet::new)));
    }

    // ------------------------------------------------------------------ C-203

    @Test
    public void testC203_flatMapPresizesToTheInputSizeOnly() {
        final List<Integer> input = new ArrayList<>(Collections.nCopies(1000, 1));
        final int[] requested = { -1 };

        final List<Integer> result = N.flatMap(input, x -> Collections.<Integer> emptyList(), (IntFunction<List<Integer>>) n -> {
            requested[0] = n;
            return new ArrayList<>(n);
        });

        assertTrue(result.isEmpty());
        assertEquals(1000, requested[0]);

        final Integer[] arr = input.toArray(new Integer[0]);
        N.flatMap(arr, x -> Collections.<Integer> singletonList(x), y -> Collections.<Integer> emptyList(), n -> {
            requested[0] = n;
            return new ArrayList<Integer>(n);
        });
        assertEquals(1000, requested[0]);

        N.flatMap(arr, 0, 10, x -> Collections.<Integer> emptyList(), (IntFunction<List<Integer>>) n -> {
            requested[0] = n;
            return new ArrayList<Integer>(n);
        });
        assertEquals(10, requested[0]);
    }

    // ------------------------------------------------------------------ C-204 (+ C-214, C-224, C-229, C-256, C-267)

    @Test
    public void testC204_supplierReturningNullIsNullPointerEvenForEmptyInput() {
        final IntFunction<List<String>> nullSupplier = n -> null;
        final Supplier<Map<String, Integer>> nullMapSupplier = () -> null;

        // map / flatMap / filter / distinctBy (C-204, C-224)
        assertThrows(NullPointerException.class, () -> N.map(new String[0], s -> s, nullSupplier));
        assertThrows(NullPointerException.class, () -> N.map(new String[] { "a" }, s -> s, nullSupplier));
        assertThrows(NullPointerException.class, () -> N.filter((String[]) null, s -> true, nullSupplier));
        assertThrows(NullPointerException.class, () -> N.filter(new ArrayList<String>(), 0, 0, s -> true, nullSupplier));
        assertThrows(NullPointerException.class, () -> N.filter(Arrays.asList("x").iterator(), s -> false, nullSupplier));
        assertThrows(NullPointerException.class, () -> N.distinctBy(new String[] { "a" }, s -> s, nullSupplier));
        assertThrows(NullPointerException.class, () -> N.flatMap(Arrays.asList("a"), s -> Arrays.asList(s), (IntFunction<List<String>>) nullSupplier));

        // skipRange (C-214)
        final NullPointerException e1 = assertThrows(NullPointerException.class,
                () -> N.skipRange(Arrays.asList(1, 2, 3), 0, 1, n -> (List<Integer>) null));
        assertEquals("supplier returned null", e1.getMessage());
        assertThrows(NullPointerException.class, () -> N.skipRange(new ArrayList<Integer>(), 0, 0, n -> (List<Integer>) null));
        // index check still comes first
        assertThrows(IndexOutOfBoundsException.class, () -> N.skipRange(Arrays.asList(1), 0, 5, n -> (List<Integer>) null));

        // frequencyMap / concat (C-229)
        final NullPointerException e2 = assertThrows(NullPointerException.class, () -> N.frequencyMap(new String[0], nullMapSupplier));
        assertEquals("mapSupplier returned null", e2.getMessage());
        assertThrows(NullPointerException.class, () -> N.frequencyMap(Arrays.asList("a"), nullMapSupplier));
        assertThrows(NullPointerException.class, () -> N.frequencyMap((Iterator<String>) null, nullMapSupplier));
        assertThrows(NullPointerException.class, () -> N.concat(new ArrayList<List<String>>(), nullSupplier));
        assertThrows(NullPointerException.class, () -> N.concat(Arrays.asList(Arrays.asList("a")), nullSupplier));

        // merge / groupBy / countBy (C-256)
        assertThrows(NullPointerException.class,
                () -> N.merge(new ArrayList<List<String>>(), (a, b) -> MergeResult.TAKE_FIRST, nullSupplier));
        assertThrows(NullPointerException.class,
                () -> N.merge(Arrays.asList(Arrays.asList("a")), (a, b) -> MergeResult.TAKE_FIRST, nullSupplier));
        assertThrows(NullPointerException.class,
                () -> N.merge(Arrays.asList(Arrays.asList("a"), Arrays.asList("b")), (a, b) -> MergeResult.TAKE_FIRST, nullSupplier));
        assertThrows(NullPointerException.class,
                () -> N.merge(Arrays.asList(Arrays.asList("a"), Arrays.asList("b"), Arrays.asList("c")), (a, b) -> MergeResult.TAKE_FIRST,
                        nullSupplier));
        final NullPointerException e3 = assertThrows(NullPointerException.class,
                () -> N.groupBy((Iterable<String>) null, s -> s, () -> (Map<String, List<String>>) null));
        assertEquals("mapSupplier returned null", e3.getMessage());
        assertThrows(NullPointerException.class, () -> N.countBy(Arrays.asList("a"), s -> s, () -> (Map<String, Integer>) null));

        // flatten / flattenEachElement (C-267)
        assertThrows(NullPointerException.class, () -> N.flatten(new ArrayList<List<String>>(), nullSupplier));
        assertThrows(NullPointerException.class, () -> N.flattenEachElement(Arrays.asList("a"), () -> (List<Object>) null));
        assertThrows(NullPointerException.class, () -> N.flattenEachElement(null, () -> (List<Object>) null));

        // a non-null supplier result is still used as is
        assertEquals(new ArrayList<>(Arrays.asList("A", "B")), N.map(new String[] { "a", "b" }, String::toUpperCase, IntFunctions.ofList()));
    }

    // ------------------------------------------------------------------ C-206 / C-222 (doc) behaviour pins

    @Test
    public void testC206_C222_documentedNullAndAdjacencyRules() {
        assertArrayEquals(new int[0], N.distinct((int[]) null, 0, 0));
        assertEquals(Collections.emptyList(), N.distinct((String[]) null, 0, 0));
        assertArrayEquals(new int[] { 1, 2, 1 }, N.removeDuplicates(new int[] { 1, 2, 1 }, true));
        assertArrayEquals(new int[] { 1, 2 }, N.removeDuplicates(new int[] { 1, 2, 1 }, false));
    }

    // ------------------------------------------------------------------ C-208 / C-225

    @Test
    public void testC208_C225_primitiveKthLargestAndLowerMedianMatchSortOnLargeInputs() {
        final Random rnd = new Random(20260924);

        for (int round = 0; round < 60; round++) {
            final int len = 1 + rnd.nextInt(400);
            final int[] ints = new int[len];
            final char[] chars = new char[len];
            final byte[] bytes = new byte[len];
            final short[] shorts = new short[len];
            final long[] longs = new long[len];
            final float[] floats = new float[len];
            final double[] doubles = new double[len];

            for (int i = 0; i < len; i++) {
                ints[i] = rnd.nextInt(50) - 25;
                chars[i] = (char) rnd.nextInt(0x10000);
                bytes[i] = (byte) rnd.nextInt(256);
                shorts[i] = (short) rnd.nextInt(0x10000);
                longs[i] = rnd.nextLong() >> rnd.nextInt(60);
                final int pick = rnd.nextInt(10);
                floats[i] = pick == 0 ? Float.NaN : pick == 1 ? -0.0f : pick == 2 ? 0.0f : pick == 3 ? Float.NEGATIVE_INFINITY : rnd.nextFloat() - 0.5f;
                doubles[i] = pick == 0 ? Double.NaN : pick == 1 ? -0.0 : pick == 2 ? 0.0 : pick == 3 ? Double.POSITIVE_INFINITY : rnd.nextGaussian();
            }

            final int[] si = ints.clone();
            Arrays.sort(si);
            final char[] sc = chars.clone();
            Arrays.sort(sc);
            final byte[] sb = bytes.clone();
            Arrays.sort(sb);
            final short[] ss = shorts.clone();
            Arrays.sort(ss);
            final long[] sl = longs.clone();
            Arrays.sort(sl);
            final float[] sf = floats.clone();
            Arrays.sort(sf);
            final double[] sd = doubles.clone();
            Arrays.sort(sd);

            for (int k = 1; k <= len; k += 1 + rnd.nextInt(7)) {
                assertEquals(si[len - k], N.kthLargest(ints, k));
                assertEquals(sc[len - k], N.kthLargest(chars, k));
                assertEquals(sb[len - k], N.kthLargest(bytes, k));
                assertEquals(ss[len - k], N.kthLargest(shorts, k));
                assertEquals(sl[len - k], N.kthLargest(longs, k));
                assertEquals(Float.floatToIntBits(sf[len - k]), Float.floatToIntBits(N.kthLargest(floats, k)));
                assertEquals(Double.doubleToLongBits(sd[len - k]), Double.doubleToLongBits(N.kthLargest(doubles, k)));
            }

            final int m = (len - 1) / 2;
            assertEquals(si[m], N.lowerMedian(ints));
            assertEquals(sc[m], N.lowerMedian(chars));
            assertEquals(sb[m], N.lowerMedian(bytes));
            assertEquals(ss[m], N.lowerMedian(shorts));
            assertEquals(sl[m], N.lowerMedian(longs));
            assertEquals(Float.floatToIntBits(sf[m]), Float.floatToIntBits(N.lowerMedian(floats)));
            assertEquals(Double.doubleToLongBits(sd[m]), Double.doubleToLongBits(N.lowerMedian(doubles)));

            // a range
            if (len > 3) {
                final int from = rnd.nextInt(len / 2);
                final int to = from + 1 + rnd.nextInt(len - from);
                final int[] r = Arrays.copyOfRange(ints, from, to);
                Arrays.sort(r);
                assertEquals(r[(r.length - 1) / 2], N.lowerMedian(ints, from, to));
                assertEquals(r[0], N.kthLargest(ints, from, to, r.length));
            }
        }

        // boundaries: k out of range, empty, single element; the input is never modified
        final int[] a = { 5, 1, 4 };
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(a, 0));
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(a, 4));
        assertThrows(IllegalArgumentException.class, () -> N.lowerMedian(new int[0]));
        assertEquals(7, N.lowerMedian(new int[] { 7 }));
        final int[] big = new int[1000];
        for (int i = 0; i < big.length; i++) {
            big[i] = big.length - i;
        }
        final int[] copy = big.clone();
        assertEquals(500, N.lowerMedian(big));
        assertArrayEquals(copy, big);
    }

    // ------------------------------------------------------------------ C-209

    @Test
    public void testC209_minByMaxByExtractEachKeyOnce() {
        final AtomicInteger calls = new AtomicInteger();
        final java.util.function.Function<String, Integer> len = s -> {
            calls.incrementAndGet();
            return s == null ? null : s.length();
        };

        final List<String> words = Arrays.asList("ccc", "a", "bb", "dddd", "e", null);

        calls.set(0);
        assertEquals("dddd", N.maxBy(words, len));
        assertEquals(words.size(), calls.get());

        calls.set(0);
        assertEquals("a", N.minBy(words.toArray(new String[0]), len));
        assertEquals(words.size(), calls.get());

        calls.set(0);
        assertEquals("a", N.minBy(words.iterator(), len));
        assertEquals(words.size(), calls.get());

        // ties: the first one wins; a single element: no extraction; two elements: two extractions
        calls.set(0);
        assertEquals("aa", N.maxBy(new String[] { "aa", "bb" }, len));
        assertEquals(2, calls.get());
        calls.set(0);
        assertEquals("aa", N.minBy(Arrays.asList("aa"), len));
        assertEquals(0, calls.get());

        // the same instance twice (and null keys) still works
        final String s = "\uD83D\uDE80x";
        assertSame(s, N.maxBy(new String[] { s, s, "y" }, len));
        assertNull(N.minBy(new String[] { null, null }, len));

        // a non-deterministic extractor: the candidate keeps the key it won with
        final int[] counter = { 0 };
        final String winner = N.maxBy(Arrays.asList("x", "y", "z"), e -> "x".equals(e) ? 100 - counter[0]++ : 50);
        assertEquals("x", winner);
    }

    // ------------------------------------------------------------------ C-210 / C-211

    @Test
    public void testC210_documentedEarlyStopOnlyForTheSharedNullComparators() {
        // documented (doc-only fix, kept in step with Iterables.min/max): the shared singleton stops at a null ...
        final Iterator<Integer> it = Arrays.asList(1, null, 2, 3).iterator();
        assertNull(N.max(it, Comparators.<Integer> nullsLast()));
        assertTrue(it.hasNext());

        // ... an equivalent caller-built comparator drains the iterator, with the same result
        final Iterator<Integer> it2 = Arrays.asList(1, null, 2, 3).iterator();
        assertNull(N.max(it2, Comparators.nullsLast(Comparator.<Integer> naturalOrder())));
        assertFalse(it2.hasNext());
        final Iterator<Integer> it3 = Arrays.asList(1, null, 2, 3).iterator();
        assertNull(N.min(it3, Comparators.nullsFirst(Comparator.<Integer> naturalOrder())));
        assertFalse(it3.hasNext());

        // without a null the whole iterator is consumed whatever the comparator
        final Iterator<Integer> it4 = Arrays.asList(3, 1, 2).iterator();
        assertEquals(1, N.min(it4, Comparators.<Integer> naturalOrder()));
        assertFalse(it4.hasNext());
    }

    @Test
    public void testC211_validationOrderAndMessagesAreConsistent() {
        final IllegalArgumentException a = assertThrows(IllegalArgumentException.class,
                () -> N.min(Collections.<Integer> emptyIterator(), (Comparator<Integer>) null));
        final IllegalArgumentException b = assertThrows(IllegalArgumentException.class,
                () -> N.min(Collections.<Integer> emptyList(), (Comparator<Integer>) null));
        assertEquals(b.getMessage(), a.getMessage());
        assertTrue(a.getMessage().contains("comparator"), a.getMessage());

        assertThrows(IllegalArgumentException.class, () -> N.max(Collections.<Integer> emptyIterator(), (Comparator<Integer>) null)).getMessage()
                .contains("comparator");

        final String emptyIterable = "The specified Collection/Iterable/Iterator cannot be null or empty";
        assertEquals(emptyIterable, assertThrows(IllegalArgumentException.class, () -> N.max(Collections.<Integer> emptyList())).getMessage());
        assertEquals(emptyIterable, assertThrows(IllegalArgumentException.class, () -> N.min(new ArrayList<Integer>())).getMessage());
        assertEquals(emptyIterable, assertThrows(IllegalArgumentException.class, () -> N.maxBy(new ArrayList<String>(), String::length)).getMessage());

        assertEquals("The specified iterable cannot be null or empty",
                assertThrows(IllegalArgumentException.class, () -> N.minMax(Collections.<Integer> emptyList())).getMessage());
        assertEquals("The specified iterable cannot be null or empty",
                assertThrows(IllegalArgumentException.class, () -> N.minMax((List<Integer>) null)).getMessage());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> N.minMax(Collections.<Integer> emptyIterator(), (Comparator<Integer>) null))
                .getMessage()
                .contains("comparator"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> N.minMax(Collections.<Integer> emptyList(), (Comparator<Integer>) null))
                .getMessage()
                .contains("comparator"));

        // unchanged results
        assertEquals(Pair.of(1, 3), N.minMax(Arrays.asList(2, null, 1, 3)));
        assertEquals(3, N.max(Arrays.asList(1, 3, 2), Comparator.naturalOrder()));
    }

    // ------------------------------------------------------------------ C-215

    @Test
    public void testC215_removeDuplicatesOnListsUsesOneRemovalPass() {
        final List<Integer> sortedValues = new ArrayList<>();
        for (int i = 0; i < 20000; i++) {
            sortedValues.add(i / 2);
        }

        final CountingArrayList<Integer> sorted = new CountingArrayList<>(sortedValues);
        assertTrue(N.removeDuplicates(sorted, true));
        assertEquals(10000, sorted.size());
        assertEquals(0, sorted.removeAtCalls, "one removeIf pass, not one remove(int) per duplicate");
        for (int i = 0; i < 10000; i++) {
            assertEquals(i, sorted.get(i));
        }

        final CountingArrayList<Integer> unsorted = new CountingArrayList<>(Arrays.asList(3, 1, 3, 2, 1));
        assertTrue(N.removeDuplicates(unsorted, false));
        assertEquals(Arrays.asList(3, 1, 2), unsorted);

        // adjacent-only semantics for isSorted == true
        final List<Integer> notSorted = new ArrayList<>(Arrays.asList(1, 1, 2, 1));
        assertTrue(N.removeDuplicates(notSorted, true));
        assertEquals(Arrays.asList(1, 2, 1), notSorted);

        // null runs and deep array equality
        final List<Object> nulls = new ArrayList<>(Arrays.asList(null, null, 1, 1));
        assertTrue(N.removeDuplicates(nulls, true));
        assertEquals(Arrays.asList(null, 1), nulls);
        final List<Object> arrays = new ArrayList<>(Arrays.asList(new int[] { 1 }, new int[] { 1 }, new int[] { 2 }));
        assertTrue(N.removeDuplicates(arrays, true));
        assertEquals(2, arrays.size());
        final List<Object> arrays2 = new ArrayList<>(Arrays.asList(new int[] { 1 }, new int[] { 2 }, new int[] { 1 }));
        assertTrue(N.removeDuplicates(arrays2, false));
        assertEquals(2, arrays2.size());

        // two elements: equal and not equal
        final List<String> two = new ArrayList<>(Arrays.asList("\uD83D\uDE80", "\uD83D\uDE80"));
        assertTrue(N.removeDuplicates(two, false));
        assertEquals(Arrays.asList("\uD83D\uDE80"), two);
        assertFalse(N.removeDuplicates(new ArrayList<>(Arrays.asList("a", "b")), true));

        // COW, LinkedList, subList
        final CopyOnWriteArrayList<Integer> cow = new CopyOnWriteArrayList<>(Arrays.asList(1, 1, 2));
        assertTrue(N.removeDuplicates(cow, true));
        assertEquals(Arrays.asList(1, 2), cow);
        final CopyOnWriteArrayList<Integer> cow2 = new CopyOnWriteArrayList<>(Arrays.asList(1, 1));
        assertTrue(N.removeDuplicates(cow2, false));
        assertEquals(Arrays.asList(1), cow2);
        final LinkedList<Integer> linked = new LinkedList<>(Arrays.asList(1, 1, 2, 2, 3));
        assertTrue(N.removeDuplicates(linked, true));
        assertEquals(Arrays.asList(1, 2, 3), linked);
        final List<Integer> backing = new ArrayList<>(Arrays.asList(9, 1, 1, 2, 9));
        assertTrue(N.removeDuplicates(backing.subList(1, 4), true));
        assertEquals(Arrays.asList(9, 1, 2, 9), backing);

        // a list that does not support removal fails on the first removal, unchanged
        final List<Integer> fixed = Arrays.asList(1, 1, 2);
        assertThrows(UnsupportedOperationException.class, () -> N.removeDuplicates(fixed, true));
        assertEquals(Arrays.asList(1, 1, 2), fixed);
        assertThrows(UnsupportedOperationException.class, () -> N.removeDuplicates(fixed, false));
        assertEquals(Arrays.asList(1, 1, 2), fixed);

        // a remove-only list (transform view) is not emptied by the unsorted path
        final List<Integer> store = new ArrayList<>(Arrays.asList(1, 2, 1, 3));
        final List<Integer> view = Lists.transform(store, x -> x);
        assertTrue(N.removeDuplicates(view, false));
        assertEquals(Arrays.asList(1, 2, 3), store);
    }

    @Test
    public void testC215_removeDuplicatesOnMultiset() {
        final Multiset<String> two = Multiset.of("a", "a");
        assertTrue(N.removeDuplicates(two, true));
        assertEquals(1, two.getCount("a"));
        assertEquals(1, two.size());

        final Multiset<String> twoUnsorted = Multiset.of("a", "a");
        assertTrue(N.removeDuplicates(twoUnsorted, false));
        assertEquals(1, twoUnsorted.size());

        final Multiset<String> three = Multiset.of("a", "a", "b");
        assertTrue(N.removeDuplicates(three, true));
        assertEquals(1, three.getCount("a"));
        assertEquals(1, three.getCount("b"));

        final Multiset<String> threeUnsorted = Multiset.of("a", "a", "b");
        assertTrue(N.removeDuplicates(threeUnsorted, false));
        assertEquals(2, threeUnsorted.size());

        assertFalse(N.removeDuplicates(Multiset.of("a", "b"), true));
    }

    // ------------------------------------------------------------------ C-216

    @Test
    public void testC216_removeAllOccurrencesSkipsTheViewSafeTwoPassRoute() {
        final CountingArrayList<String> list = new CountingArrayList<>(Arrays.asList("a", "b", "a", null, "c"));
        assertTrue(N.removeAllOccurrences(list, "a"));
        assertEquals(Arrays.asList("b", null, "c"), list);
        assertEquals(0, list.iteratorCalls, "no separate scan before removeIf");

        assertTrue(N.removeAllOccurrences(list, (String) null));
        assertEquals(Arrays.asList("b", "c"), list);
        assertFalse(N.removeAllOccurrences(list, "zz"));

        final TreeSet<String> ci = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ci.addAll(Arrays.asList("A", "b"));
        assertTrue(N.removeAllOccurrences(ci, "a"));
        assertEquals(Collections.singleton("b"), ci);

        final Multiset<String> ms = Multiset.of("x", "x", "y");
        assertTrue(N.removeAllOccurrences(ms, "x"));
        assertEquals(0, ms.getCount("x"));
        assertEquals(1, ms.getCount("y"));
    }

    // ------------------------------------------------------------------ C-217

    @Test
    public void testC217_moveRangeOnlyTouchesTheAffectedWindow() {
        final List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            values.add(i);
        }

        final CountingArrayList<Integer> list = new CountingArrayList<>(values);
        assertTrue(N.moveRange(list, 500, 501, 501));
        assertTrue(list.setCalls <= 2, "set() calls: " + list.setCalls);
        assertEquals(501, list.get(500));
        assertEquals(500, list.get(501));

        // differential against the array overload
        final Random rnd = new Random(7);
        for (int round = 0; round < 2000; round++) {
            final int n = rnd.nextInt(12);
            final Integer[] arr = new Integer[n];
            for (int i = 0; i < n; i++) {
                arr[i] = i;
            }
            final int from = n == 0 ? 0 : rnd.nextInt(n + 1);
            final int to = from + (n - from == 0 ? 0 : rnd.nextInt(n - from + 1));
            final int newPos = rnd.nextInt(n - (to - from) + 1);
            final List<Integer> al = new ArrayList<>(Arrays.asList(arr));
            final List<Integer> ll = new LinkedList<>(Arrays.asList(arr));
            final boolean changed = N.moveRange(al, from, to, newPos);
            N.moveRange(ll, from, to, newPos);
            N.moveRange(arr, from, to, newPos);
            assertEquals(Arrays.asList(arr), al);
            assertEquals(Arrays.asList(arr), ll);
            assertEquals(from != to && from != newPos, changed);
        }

        // unsupported set: only when something moves
        assertThrows(UnsupportedOperationException.class, () -> N.moveRange(Collections.unmodifiableList(new ArrayList<>(values)), 0, 1, 1));
        assertFalse(N.moveRange(Collections.unmodifiableList(new ArrayList<>(values)), 0, 1, 0));
    }

    // ------------------------------------------------------------------ C-218

    @Test
    public void testC218_byteCharShortDistinctAndContainsDuplicates() {
        final byte[] bytes = new byte[5000];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i * 7);
        }
        final byte[] distinctBytes = N.distinct(bytes);
        assertEquals(256, distinctBytes.length);
        assertEquals(new LinkedHashSet<>(Arrays.asList(ArrayUtils_box(bytes))), new LinkedHashSet<>(Arrays.asList(ArrayUtils_box(distinctBytes))));
        assertArrayEquals(new byte[] { 5, -2, 8 }, N.distinct(new byte[] { 5, -2, 5, 8, -2 }));
        assertArrayEquals(new byte[] { -128, 127 }, N.removeDuplicates(new byte[] { -128, 127, -128 }, false));
        assertTrue(N.containsDuplicates(bytes));
        assertTrue(N.containsDuplicates(new byte[257], false));
        assertFalse(N.containsDuplicates(new byte[] { 1, 2, 3, 4 }, false));
        assertTrue(N.containsDuplicates(new byte[] { 1, 2, 3, 1 }, false));

        // char, including surrogate halves and the extremes
        final char[] chars = { '\uD83D', 'a', '\uDE80', '\uD83D', '\uFFFF', '\u0000', 'a', '\u0000' };
        assertArrayEquals(new char[] { '\uD83D', 'a', '\uDE80', '\uFFFF', '\u0000' }, N.distinct(chars));
        assertTrue(N.containsDuplicates(chars, false));
        assertFalse(N.containsDuplicates(new char[] { 'a', '\uFFFF', '\u0000', 'b' }, false));
        assertArrayEquals(new char[] { 'b', 'c' }, N.removeDuplicates(new char[] { 'a', 'b', 'c', 'b' }, 1, 4, false));

        // short, including negative values and a full-range array
        assertArrayEquals(new short[] { -1, 32767, -32768 }, N.distinct(new short[] { -1, 32767, -1, -32768, 32767 }));
        final short[] all = new short[65536];
        for (int i = 0; i < all.length; i++) {
            all[i] = (short) i;
        }
        assertFalse(N.containsDuplicates(all, false));
        assertEquals(65536, N.distinct(all).length);
        assertTrue(N.containsDuplicates(new short[65537], false));

        // untouched input and fresh result
        final short[] noDup = { 3, 1, 2 };
        final short[] res = N.distinct(noDup);
        assertArrayEquals(noDup, res);
        assertFalse(res == noDup);
        assertArrayEquals(new short[0], N.distinct((short[]) null));
    }

    private static Byte[] ArrayUtils_box(final byte[] a) {
        final Byte[] r = new Byte[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = a[i];
        }
        return r;
    }

    // ------------------------------------------------------------------ C-220

    @Test
    public void testC220_internalBooleanRemoveDuplicatesIsNotDeprecated() throws Exception {
        assertFalse(N.class.getDeclaredMethod("removeDuplicates", boolean[].class, int.class, int.class).isAnnotationPresent(Deprecated.class));
        assertArrayEquals(new boolean[] { true, false }, N.distinct(new boolean[] { true, true, false, true }, 0, 4));
    }

    // ------------------------------------------------------------------ C-221 (rename only) behaviour unchanged

    @Test
    public void testC221_skipRangeOnCollectionsUnchanged() {
        assertEquals(Arrays.asList("alpha", "delta"), N.skipRange(Arrays.asList("alpha", "beta", "gamma", "delta"), 1, 3));
        assertEquals(new ArrayList<>(Arrays.asList(4, 5)), N.skipRange(new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4, 5)), 0, 3));
        assertEquals(Arrays.asList(), N.skipRange((Collection<Integer>) null, 0, 0));
    }

    // ------------------------------------------------------------------ C-228 / C-262

    @Test
    public void testC228_containsFamilyAnswersFalseForProbesTheCollectionRejects() {
        assertFalse(N.contains(Set.of("a"), null));
        assertFalse(N.contains(List.of("a"), null));
        assertFalse(N.contains((Iterable<?>) Set.of("a"), null));
        assertFalse(N.contains(new TreeSet<>(List.of("a")), (Object) 1));
        assertTrue(N.contains(Set.of("a"), "a"));

        assertFalse(N.containsAll(List.of("a"), Arrays.asList((Object) null)));
        assertFalse(N.containsAll(Set.of("a"), "a", null));
        assertFalse(N.containsAll((Iterable<?>) List.of("a"), Arrays.asList((Object) null)));
        assertTrue(N.containsAll(Set.of("a", "b"), "a", "b"));

        assertFalse(N.containsAny((Iterable<?>) Arrays.asList("x", null), Set.of("a")));
        assertTrue(N.containsAny((Iterable<?>) Arrays.asList(null, "a"), Set.of("a")));
        assertFalse(N.containsAny(Arrays.asList("x", null).iterator(), Set.of("a")));
        assertFalse(N.containsAny(Arrays.asList("x", null), Set.of("a")));
        assertFalse(N.containsAny(new TreeSet<>(List.of("a")), (Object) null, 1));
        assertTrue(N.containsNone(new TreeSet<>(List.of("a")), (Object) null));
        assertTrue(N.containsNone((Iterable<?>) Arrays.asList("x", null), Set.of("a")));
        assertFalse(N.containsNone(Arrays.asList("\uD83D\uDE80", null), Set.of("\uD83D\uDE80")));
    }

    @Test
    public void testC262_excludeAllTreatsARejectedProbeAsNotExcluded() {
        assertEquals(Arrays.asList(null, "c"), N.excludeAll(Arrays.asList("a", null, "c"), Set.of("a", "b")));
        assertEquals(Arrays.asList(null, "c"), N.excludeAll(Arrays.asList("a", null, "c"), Set.of("a")));
        assertEquals(new LinkedHashSet<>(Arrays.asList(null, "c")), N.excludeAllToSet(Arrays.asList("a", null, "c"), Set.of("a", "b")));
        assertEquals(Arrays.asList("x"), N.excludeAll(Arrays.<Object> asList(1, "x"), new TreeSet<>(List.of(1, 2))));
        // the Set's own rule still decides
        final TreeSet<String> ci = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ci.add("A");
        assertEquals(Arrays.asList("b"), N.excludeAll(Arrays.asList("a", "b"), ci));
    }

    // ------------------------------------------------------------------ C-229 / C-230

    @Test
    public void testC230_frequencyMapAccumulatesThroughTheSuppliedMap() {
        final Supplier<Map<String, Integer>> ci = () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final Map<String, Integer> expected = new TreeMap<>();
        expected.put("a", 2);
        expected.put("b", 1);

        assertEquals(expected, N.frequencyMap(Arrays.asList("a", "A", "b"), ci));
        assertEquals(expected, N.frequencyMap(Arrays.asList("a", "A", "b").iterator(), ci));
        assertEquals(expected, N.frequencyMap(new String[] { "a", "A", "b" }, ci));
        assertEquals(N.countBy(Arrays.asList("a", "A", "b"), Fn.identity(), () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)),
                N.frequencyMap(Arrays.asList("a", "A", "b"), ci));

        // Unicode: case folding across the multiset entries
        final Map<String, Integer> u = N.frequencyMap(Arrays.asList("\u00C9", "\u00E9", "\uD83D\uDE80", "\uD83D\uDE80"), ci);
        assertEquals(2, u.get("\u00E9").intValue());
        assertEquals(2, u.get("\uD83D\uDE80").intValue());

        // a finer equivalence (identity) still differs from the array overload: equal instances are merged first
        final String x1 = new String("x");
        final String x2 = new String("x");
        final Supplier<Map<String, Integer>> identity = IdentityHashMap::new;
        assertEquals(1, N.frequencyMap(Arrays.asList(x1, x2), identity).size());
        assertEquals(2, N.frequencyMap(new String[] { x1, x2 }, identity).size());

        // null elements with a HashMap
        final Map<String, Integer> withNull = N.frequencyMap(Arrays.asList(null, "a", null), HashMap::new);
        assertEquals(2, withNull.get(null).intValue());
        // existing entries of the supplied map are added to
        final Map<String, Integer> pre = N.frequencyMap(Arrays.asList("a"), () -> new HashMap<>(Map.of("a", 5)));
        assertEquals(6, pre.get("a").intValue());
    }

    // ------------------------------------------------------------------ C-231

    @Test
    public void testC231_containsAnyOnTwoListsHashesTheProbedList() {
        final List<Integer> left = new ArrayList<>();
        final List<Integer> right = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            left.add(i); // the larger side is the probed one
        }
        for (int i = 0; i < 200; i++) {
            right.add(1000 + i);
        }

        final CountingArrayList<Integer> probed = new CountingArrayList<>(left);
        assertFalse(N.containsAny(probed, right));
        assertEquals(0, probed.containsCalls, "the probed list is hashed once, not scanned per element");
        assertTrue(N.containsNone(probed, right));

        right.set(150, 42);
        assertTrue(N.containsAny(probed, right));
        assertTrue(N.containsAny(probed, right.toArray()));
        assertFalse(N.containsNone(probed, right));

        // nulls and small inputs
        assertTrue(N.containsAny(Arrays.asList(1, null), Arrays.asList(null, 2)));
        final List<Integer> withNull = new ArrayList<>(left);
        withNull.add(null);
        final List<Integer> otherWithNull = new ArrayList<>(right);
        otherWithNull.set(150, null);
        assertTrue(N.containsAny(withNull, otherWithNull));
        // a Set still decides with its own rule
        final TreeSet<String> ci = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ci.add("A");
        assertTrue(N.containsAny(Arrays.asList("a"), ci));
    }

    // ------------------------------------------------------------------ C-234

    @Test
    public void testC234_splitIteratorMessageMatchesItsSiblings() {
        final String expected = assertThrows(IllegalArgumentException.class, () -> N.split(Arrays.asList(1), 0)).getMessage();
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> N.split(Arrays.asList(1).iterator(), 0)).getMessage());
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3)), N.toList(N.split(Arrays.asList(1, 2, 3).iterator(), 2)));
    }

    // ------------------------------------------------------------------ C-235

    @Test
    public void testC235_doubleSumIsInfiniteOnlyForATrueOverflow() {
        assertEquals(5.0, N.sum(1e308, 1e308, -1e308, -1e308, 5));
        assertEquals(0.0, N.sum(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE));
        assertEquals(Double.MAX_VALUE, N.sum(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE));
        assertEquals(Double.MAX_VALUE, N.sum(new double[] { 1, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, 1 }, 1, 4));
        assertEquals(Double.POSITIVE_INFINITY, N.sum(Double.MAX_VALUE, Double.MAX_VALUE));
        assertEquals(Double.NEGATIVE_INFINITY, N.sum(-Double.MAX_VALUE, -Double.MAX_VALUE, 1));
        assertTrue(Double.isNaN(N.sum(1e308, 1e308, Double.NaN)));
        assertEquals(Double.POSITIVE_INFINITY, N.sum(1e308, 1e308, Double.POSITIVE_INFINITY, -1e308));
        assertEquals(6.0, N.sum(1.0, 2.0, 3.0));
        assertEquals(0.0, N.sum(new double[0]));

        assertEquals(5.0, N.sumDouble(Arrays.asList(1e308, 1e308, -1e308, -1e308, 5.0)));
        assertEquals(5.0, N.sumDouble(new Double[] { 1e308, 1e308, -1e308, -1e308, 5.0 }));
        assertEquals(5.0, N.sumDouble(new LinkedList<>(Arrays.asList(1e308, 1e308, -1e308, -1e308, 5.0)), 0, 5));
        assertEquals(5.0, N.sumDouble(Arrays.asList("a", "b", "c", "d", "e"), s -> "e".equals(s) ? 5.0 : ("a".equals(s) || "b".equals(s) ? 1e308 : -1e308)));
        assertEquals(N.average(1e308, 1e308, -1e308, -1e308, 5) * 5, N.sum(1e308, 1e308, -1e308, -1e308, 5), 1e-9);
    }

    // ------------------------------------------------------------------ C-237

    /** Equal by id, but with identity hashCode: breaks the equals/hashCode contract. */
    private static final class Sloppy {
        final int id;

        Sloppy(final int id) {
            this.id = id;
        }

        @Override
        public boolean equals(final Object o) {
            return o instanceof Sloppy s && s.id == id;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }

    @Test
    public void testC237_retainAllHasNoSizeSeam() {
        final Sloppy b1 = new Sloppy(1);
        final Sloppy b1Twin = new Sloppy(1);

        final List<Object> small = new ArrayList<>(Arrays.asList(b1, "x"));
        N.retainAll(small, Arrays.asList(b1Twin, "x"));

        final List<Object> bigArg = new ArrayList<>(Arrays.asList(b1Twin, "x"));
        for (int i = 0; i < 10; i++) {
            bigArg.add("pad" + i);
        }
        final List<Object> big = new ArrayList<>(Arrays.asList(b1, "x"));
        N.retainAll(big, bigArg);

        assertEquals(big, small, "the same membership answer at argument size 2 and 12");

        // well-behaved elements unchanged; the argument's rule decides for a Set argument
        final List<Integer> nums = new ArrayList<>(Arrays.asList(1, 2, 3));
        assertTrue(N.retainAll(nums, Arrays.asList(3)));
        assertEquals(Arrays.asList(3), nums);
        final TreeSet<String> ci = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ci.add("A");
        final List<String> subject = new ArrayList<>(Arrays.asList("a", "b"));
        assertTrue(N.retainAll(subject, ci));
        assertEquals(Arrays.asList("a"), subject);
    }

    // ------------------------------------------------------------------ C-241

    /** An equals() that casts blindly. */
    private static final class SloppyEquals {
        @Override
        public boolean equals(final Object o) {
            return ((SloppyEquals) o) == this;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    @Test
    public void testC241_duplicateChecksNeverPassTheNullSentinelToUserEquals() {
        final SloppyEquals s = new SloppyEquals();
        assertFalse(N.containsDuplicates(new Object[] { s, null }));
        assertFalse(N.containsDuplicates(new Object[] { null, s }, true));
        assertFalse(N.containsDuplicates(Arrays.asList(s, null)));
        assertTrue(N.containsDuplicates(new Object[] { null, null }));

        // a Wrapper of an array and the raw array are different values
        final Object wrapped = Wrapper.of(new int[] { 1 });
        final Object raw = new int[] { 1 };
        assertFalse(N.containsDuplicates(new Object[] { wrapped, raw }));
        assertEquals(2, N.distinct(new Object[] { wrapped, raw }).size());
        // two raw arrays with equal content still are
        assertTrue(N.containsDuplicates(new Object[] { new int[] { 1 }, new int[] { 1 } }));
        assertEquals(1, N.distinct(new Object[] { new String[] { "\uD83D\uDE80" }, new String[] { "\uD83D\uDE80" } }).size());
    }

    // ------------------------------------------------------------------ C-242

    @Test
    public void testC242_formatXmlPreservesStructure() {
        final String repeated = N.formatXml("<root><e>1</e><e>2</e><e>3</e></root>");
        assertEquals("<root>\n    <e>1</e>\n    <e>2</e>\n    <e>3</e>\n</root>", repeated);

        assertEquals("<root>\n    <a>\n        <b>1</b>\n        <c>2</c>\n    </a>\n</root>", N.formatXml("<root><a><b>1</b><c>2</c></a></root>"));
        assertEquals("<Person id=\"7\" q=\"&quot;&lt;&amp;\">\n    <name>A</name>\n</Person>",
                N.formatXml("<Person id='7' q='&quot;&lt;&amp;'><name>A</name></Person>"));
        assertEquals("<root>\u9ec4\uD83D\uDE00</root>", N.formatXml("<root>\u9ec4\uD83D\uDE00</root>"));
        assertEquals("<root><![CDATA[<x>&]]></root>", N.formatXml("<root><![CDATA[<x>&]]></root>"));
        assertEquals("<root>t1<a>1</a>t2</root>", N.formatXml("<root>t1<a>1</a>t2</root>"));
        assertEquals("<root>\n    <!-- c -->\n    <a>1</a>\n</root>", N.formatXml("<root><!-- c --><a>1</a></root>"));
        assertEquals("<r xmlns:p=\"urn:x\">\n    <p:a p:k=\"v\">&amp;&lt;</p:a>\n</r>",
                N.formatXml("<?xml version=\"1.0\"?>\n<r xmlns:p=\"urn:x\"><p:a p:k=\"v\">&amp;&lt;</p:a></r>"));
        assertEquals("<r>\n    <s xml:space=\"preserve\">  x  </s>\n</r>", N.formatXml("<r><s xml:space=\"preserve\">  x  </s></r>"));
        assertEquals("<root>\n    <a/>\n    <b>x</b>\n</root>", N.formatXml("<root><a/><b>x</b></root>"));
        assertEquals("<r>\n\t<a>1</a>\n</r>", N.formatXml("<r><a>1</a></r>", XmlSerConfig.create().setIndentation("\t")));
        assertEquals("<r>\n  <a>1</a>\n</r>", N.formatXml("<r><a>1</a></r>", XmlSerConfig.create().setIndentation("  ")));

        // idempotent on its own output; null/blank -> ""
        assertEquals(repeated, N.formatXml(repeated));
        assertEquals("", N.formatXml(null));
        assertEquals("", N.formatXml("   "));
        assertEquals("", N.formatXml(null, (XmlSerConfig) null));

        // malformed input and DOCTYPE/XXE are rejected
        assertThrows(ParsingException.class, () -> N.formatXml("<a><b></a>"));
        assertThrows(ParsingException.class, () -> N.formatXml("<!DOCTYPE r [<!ENTITY x SYSTEM \"file:///c:/windows/win.ini\">]><r>&x;</r>"));
    }

    // ------------------------------------------------------------------ C-245

    @Test
    public void testC245_formatJsonKeepsOrderAndAcceptsScalars() {
        final String formatted = N.formatJson("{\"z\":1,\"a\":2,\"m\":{\"y\":1,\"b\":2},\"l\":[{\"q\":1,\"c\":2}]}");
        assertTrue(formatted.indexOf("\"z\"") < formatted.indexOf("\"a\"") && formatted.indexOf("\"a\"") < formatted.indexOf("\"m\""), formatted);
        assertTrue(formatted.indexOf("\"y\"") < formatted.indexOf("\"b\""), formatted);
        assertTrue(formatted.indexOf("\"q\"") < formatted.indexOf("\"c\""), formatted);
        assertTrue(formatted.contains("\n"), formatted);

        assertEquals("\"abc\"", N.formatJson("\"abc\""));
        assertEquals("42", N.formatJson(" 42 "));
        assertEquals("null", N.formatJson("null"));
        assertEquals("true", N.formatJson("true"));
        assertEquals("\"\uD83D\uDE80\"", N.formatJson("\"\uD83D\uDE80\""));
        assertEquals("", N.formatJson("   "));
        assertEquals("", N.formatJson(null));
        assertEquals("", N.formatJson(""));
        assertTrue(N.formatJson(" {\"b\":1} ").contains("\"b\""));
        assertThrows(ParsingException.class, () -> N.formatJson("{bad"));

        final String custom = N.formatJson("{\"z\":1,\"a\":2}", com.landawn.abacus.parser.JsonSerConfig.create().setIndentation("\t"));
        assertTrue(custom.indexOf("\"z\"") < custom.indexOf("\"a\""), custom);
    }

    // ------------------------------------------------------------------ C-248

    @Test
    public void testC248_removeAllFromAViewThatRematerialisesElements() {
        final List<Integer> backing = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        final List<Long> view = Lists.transform(backing, i -> i * 1000L);
        assertTrue(N.removeAll(view, new HashSet<>(Arrays.asList(2000L, 3000L))));
        assertEquals(Arrays.asList(1, 4), backing);

        final List<Integer> backing2 = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        final Collection<Long> view2 = Collections2.transform(backing2, i -> i * 1000L);
        assertTrue(N.removeAll(view2, new TreeSet<>(Arrays.asList(2000L, 3000L))));
        assertEquals(Arrays.asList(1, 4), backing2);

        final List<Integer> backing3 = new ArrayList<>(Arrays.asList(1, 2, 3));
        final Set<Long> identity = Collections.newSetFromMap(new IdentityHashMap<>());
        identity.add(9_999_999L);
        assertFalse(N.removeAll(Lists.transform(backing3, i -> i * 1000L), identity));
        assertEquals(Arrays.asList(1, 2, 3), backing3);

        // regression guards: Multiset receiver (its removeIf visits distinct elements)
        final Multiset<String> ms = Multiset.of("a", "a", "b");
        assertTrue(N.removeAll(ms, new HashSet<>(Arrays.asList("b"))));
        assertEquals(2, ms.getCount("a"));
        assertEquals(0, ms.getCount("b"));
        final Multiset<String> ms2 = Multiset.of("a", "a", "b", "c");
        assertTrue(N.removeAll(ms2, ms2.elementSet()));
        assertTrue(ms2.isEmpty());

        // identity-sibling rule and nulls
        final String s1 = new String("s");
        final String s2 = new String("s");
        final List<String> siblings = new ArrayList<>(Arrays.asList(s1, s2));
        final Set<String> onlyS1 = Collections.newSetFromMap(new IdentityHashMap<>());
        onlyS1.add(s1);
        assertTrue(N.removeAll(siblings, onlyS1));
        assertEquals(1, siblings.size());
        assertSame(s2, siblings.get(0));
        final List<String> withNulls = new ArrayList<>(Arrays.asList(null, "a", null));
        assertTrue(N.removeAll(withNulls, new HashSet<>(Collections.singleton((String) null))));
        assertEquals(Arrays.asList("a"), withNulls);
    }

    // ------------------------------------------------------------------ C-249 / C-251

    @Test
    public void testC249_removeAtFromARemoveOnlyListKeepsTheSurvivors() {
        final List<Integer> backing = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        assertTrue(N.removeAt(Lists.transform(backing, i -> "v" + i), 0, 2));
        assertEquals(Arrays.asList(2, 4), backing);

        final ShrinkOnlyList<Integer> shrink = new ShrinkOnlyList<>(Arrays.asList(10, 20, 30, 40, 50));
        assertTrue(N.removeAt(shrink, 3, 1, 3));
        assertEquals(Arrays.asList(10, 30, 50), shrink.store);

        // first / last / all indexes, nulls, subList, synchronizedList, LinkedList
        final List<String> list = new ArrayList<>(Arrays.asList(null, "b", null, "\uD83D\uDE80", "e"));
        assertTrue(N.removeAt(list, 4, 0));
        assertEquals(Arrays.asList("b", null, "\uD83D\uDE80"), list);
        final List<Integer> all = new ArrayList<>(Arrays.asList(1, 2, 3));
        assertTrue(N.removeAt(all, 0, 1, 2));
        assertTrue(all.isEmpty());
        final List<Integer> outer = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5));
        assertTrue(N.removeAt(outer.subList(1, 5), 0, 3));
        assertEquals(Arrays.asList(0, 2, 3, 5), outer);
        final List<Integer> sync = Collections.synchronizedList(new ArrayList<>(Arrays.asList(1, 2, 3, 4)));
        assertTrue(N.removeAt(sync, 1, 2));
        assertEquals(Arrays.asList(1, 4), sync);
        final LinkedList<Integer> linked = new LinkedList<>(Arrays.asList(1, 2, 3, 4));
        assertTrue(N.removeAt(linked, 3, 0, 0));
        assertEquals(Arrays.asList(2, 3), linked);

        // a list without removal: UOE, unchanged
        final List<Integer> fixed = Arrays.asList(1, 2, 3);
        assertThrows(UnsupportedOperationException.class, () -> N.removeAt(fixed, 0, 2));
        assertEquals(Arrays.asList(1, 2, 3), fixed);
    }

    @Test
    public void testC251_multiIndexOutOfBoundsMessageNamesTheIndexAndLength() {
        assertEquals("Index 5 out of bounds for length 3", assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(new int[] { 1, 2, 3 }, 0, 5)).getMessage());
        assertEquals("Index -1 out of bounds for length 3",
                assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(new char[] { 'a', 'b', 'c' }, 1, -1)).getMessage());
        assertEquals("Index 3 out of bounds for length 3",
                assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(new String[] { "a", "b", "c" }, 0, 3)).getMessage());
        assertEquals("Index 9 out of bounds for length 2",
                assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(new Integer[] { 1, 2 }, 9, 0)).getMessage());
        assertEquals("Index 4 out of bounds for length 2",
                assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(new ArrayList<>(Arrays.asList(1, 2)), 4, 1)).getMessage());
        assertEquals("Index 1 out of bounds for length 0", assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt((int[]) null, 0, 1)).getMessage());
    }

    // ------------------------------------------------------------------ C-277

    @Test
    public void testC277_removeRangeOnARemoveOnlyListKeepsTheSurvivors() {
        final List<Integer> backing = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        assertTrue(N.removeRange(Lists.transform(backing, i -> "v" + i), 1, 6)); // span > 3: the non-subList branch
        assertEquals(Arrays.asList(1, 7), backing);

        final ShrinkOnlyList<String> shrink = new ShrinkOnlyList<>(Arrays.asList("a", null, "\uD83D\uDE80", "d", "e", "f"));
        assertTrue(N.removeRange(shrink, 0, 4));
        assertEquals(Arrays.asList("e", "f"), shrink.store);

        // whole list, tail, head; empty range; a list without removal fails unchanged
        final List<Integer> all = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        assertTrue(N.removeRange(all, 0, 5));
        assertTrue(all.isEmpty());
        final List<Integer> tail = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));
        assertTrue(N.removeRange(tail, 2, 6));
        assertEquals(Arrays.asList(1, 2), tail);
        assertFalse(N.removeRange(tail, 1, 1));
        final List<Integer> fixed = Arrays.asList(1, 2, 3, 4, 5, 6);
        assertThrows(UnsupportedOperationException.class, () -> N.removeRange(fixed, 0, 5));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), fixed);
        final CopyOnWriteArrayList<Integer> cow = new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));
        assertTrue(N.removeRange(cow, 1, 5));
        assertEquals(Arrays.asList(1, 6), cow);
    }

    // ------------------------------------------------------------------ C-250

    @Test
    public void testC250_addAllOfNothingNeverTouchesTheReceiver() {
        final List<Integer> ro = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(1)));
        assertFalse(N.addAll(ro, (Iterable<Integer>) new ArrayList<Integer>()));
        assertFalse(N.addAll(ro, Collections.<Integer> emptyIterator()));
        assertFalse(N.addAll(List.<Integer> of(), (Iterable<Integer>) Collections.<Integer> emptyList()));
        assertFalse(N.addAll(ro, (Iterable<Integer>) () -> Collections.<Integer> emptyIterator()));
        assertThrows(UnsupportedOperationException.class, () -> N.addAll(ro, (Iterable<Integer>) Arrays.asList(2)));

        final List<Integer> ok = new ArrayList<>();
        assertTrue(N.addAll(ok, Arrays.asList(1, 2).iterator()));
        assertEquals(Arrays.asList(1, 2), ok);
    }

    // ------------------------------------------------------------------ C-254

    @Test
    public void testC254_groupByWithCollectorRejectsANonEmptySuppliedMap() {
        final Map<Character, Long> prePopulated = new HashMap<>();
        prePopulated.put('z', 5L);

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> N.groupBy(Arrays.asList("apple", "apricot", "banana"), s -> s.charAt(0), Collectors.counting(), () -> prePopulated));
        assertTrue(e.getMessage().contains("empty map"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> N.groupBy(new ArrayList<String>(), s -> s.charAt(0), Collectors.counting(), () -> prePopulated));
        assertThrows(IllegalArgumentException.class,
                () -> N.groupBy(Arrays.asList("x").iterator(), s -> s.charAt(0), Collectors.joining(), () -> new HashMap<>(Map.of('q', "q"))));

        final Map<Character, Long> fresh = N.groupBy(Arrays.asList("apple", "apricot", "banana"), s -> s.charAt(0), Collectors.counting(), TreeMap::new);
        assertEquals(Map.of('a', 2L, 'b', 1L), fresh);
        assertEquals(Collections.emptyMap(), N.groupBy((Iterable<String>) null, s -> s.charAt(0), Collectors.counting(), HashMap::new));
    }

    // ------------------------------------------------------------------ C-255

    @Test
    public void testC255_zipWithDefaultsFollowsTheIteratorsNotSize() {
        final ConcurrentLinkedQueue<Integer> q = new ConcurrentLinkedQueue<>(Arrays.asList(1, 2, 3, 4));
        final List<String> zipped = N.zip(q, Arrays.asList(10, 20, 30, 40), -1, -1, (x, y) -> {
            q.remove(4);
            return x + ":" + y;
        });
        assertEquals(Arrays.asList("1:10", "2:20", "3:30", "-1:40"), zipped);

        final ConcurrentLinkedQueue<Integer> q3 = new ConcurrentLinkedQueue<>(Arrays.asList(1, 2, 3));
        final List<String> zipped3 = N.zip(q3, Arrays.asList(10, 20, 30), Arrays.asList("a", "b", "c"), -1, -1, "?", (x, y, z) -> {
            q3.remove(3);
            return x + ":" + y + z;
        });
        assertEquals(Arrays.asList("1:10a", "2:20b", "-1:30c"), zipped3);

        // null / empty / plain Iterable inputs
        assertEquals(Arrays.asList("?=30", "?=25"), N.zip(N.<String> emptyList(), Arrays.asList(30, 25), "?", 0, (n, age) -> n + "=" + age));
        assertEquals(Arrays.asList("A=0"), N.zip(Arrays.asList("A"), (Iterable<Integer>) null, "?", 0, (n, age) -> n + "=" + age));
        final Iterable<Integer> plain = () -> Arrays.asList(7, 8).iterator();
        assertEquals(Arrays.asList("x7", "?8"), N.zip(Arrays.asList("x"), plain, "?", 0, (n, v) -> n + v));
        assertEquals(Collections.emptyList(), N.zip((Iterable<String>) null, (Iterable<Integer>) null, "?", 0, (n, v) -> n + v));
        assertEquals(Arrays.asList("x1?", "?2?"), N.zip(Arrays.asList("x"), Arrays.asList(1, 2), (Iterable<String>) null, "?", 0, "?", (a, b, c) -> a + b + c));
    }

    // ------------------------------------------------------------------ C-259

    @Test
    public void testC259_zipAndFromJsonValidateUnderTheirOwnParameterNames() {
        final Integer[] a = { 1, 2 };
        final Integer[] b = { 3, 4 };
        final IllegalArgumentException prim = assertThrows(IllegalArgumentException.class, () -> N.zip(a, b, (x, y) -> x + y, int.class));
        assertTrue(prim.getMessage().contains("targetElementType"), prim.getMessage());
        assertThrows(IllegalArgumentException.class, () -> N.zip(a, b, 0, 0, (x, y) -> x + y, int.class));
        assertThrows(IllegalArgumentException.class, () -> N.zip(a, b, a, (x, y, z) -> x + y + z, int.class));
        assertThrows(IllegalArgumentException.class, () -> N.zip(a, b, a, 0, 0, 0, (x, y, z) -> x + y + z, int.class));
        assertArrayEquals(new Integer[] { 4, 6 }, N.zip(a, b, (x, y) -> x + y, Integer.class));

        final IllegalArgumentException fj = assertThrows(IllegalArgumentException.class, () -> N.fromJson("{}", (Class<Object>) null));
        assertTrue(fj.getMessage().contains("targetType"), fj.getMessage());
        assertThrows(IllegalArgumentException.class, () -> N.fromJson("{}", (com.landawn.abacus.parser.JsonDeserConfig) null, (Class<Object>) null));
        assertThrows(IllegalArgumentException.class, () -> N.fromJson(new java.io.File("x.json"), (Class<Object>) null));
    }

    // ------------------------------------------------------------------ C-263

    @Test
    public void testC263_symmetricDifferenceKeepsTheSameRepresentativesAsDifference() {
        final List<Keyed> a = Arrays.asList(new Keyed(1, "a"));
        final List<Keyed> b = Arrays.asList(new Keyed(1, "b-first"), new Keyed(2, "b"), new Keyed(1, "b-last"));

        final List<Keyed> viaDifference = new ArrayList<>(N.difference(a, b));
        viaDifference.addAll(N.difference(b, a));
        assertEquals(labels(viaDifference), labels(N.symmetricDifference(a, b)));
        assertEquals(Arrays.asList("b", "b-last"), labels(N.symmetricDifference(a, b)));
        assertEquals(Arrays.asList("b", "b-last"), labels(N.symmetricDifference(a.toArray(new Keyed[0]), b.toArray(new Keyed[0]))));

        // counts and a-side order unchanged
        assertEquals(Arrays.asList("B", "C", "D", "E"), N.symmetricDifference(Arrays.asList("A", "B", "B", "C", "D"), Arrays.asList("B", "E", "A")));
        assertEquals(Arrays.asList("B", "C", "D", "E"), N.symmetricDifference(new String[] { "A", "B", "B", "C", "D" }, new String[] { "B", "E", "A" }));
        assertEquals(Arrays.asList(null, "x"), N.symmetricDifference(Arrays.asList(null, null, "y"), Arrays.asList("y", null, "x")));
        assertEquals(Arrays.asList("\uD83D\uDE80"), N.symmetricDifference(Collections.<String> emptyList(), Arrays.asList("\uD83D\uDE80")));
    }

    // ------------------------------------------------------------------ C-264 / C-265

    @Test
    public void testC264_arrayReplaceAllCallsOldValsEqualsLikeTheListOverload() {
        final Object matchesEverything = new Object() {
            @Override
            public boolean equals(final Object o) {
                return true;
            }

            @Override
            public int hashCode() {
                return 0;
            }
        };

        final String[] arr = { "p", "q" };
        final List<String> list = new ArrayList<>(Arrays.asList("p", "q"));
        assertEquals(N.replaceAll(list, matchesEverything, "X"), N.replaceAll(arr, matchesEverything, "X"));
        assertArrayEquals(new String[] { "X", "X" }, arr);

        final String[] withNull = { null, "a", null };
        assertEquals(2, N.replaceAll(withNull, null, "n"));
        assertArrayEquals(new String[] { "n", "a", "n" }, withNull);
    }

    @Test
    public void testC265_randomAccessListsAreRewrittenByTheirOwnReplaceAll() {
        final CountingArrayList<Integer> list = new CountingArrayList<>(Arrays.asList(1, 2, 3, 2));
        assertEquals(2, N.replaceIf(list, x -> x == 2, 9));
        assertEquals(Arrays.asList(1, 9, 3, 9), list);
        assertEquals(0, list.setCalls);

        assertEquals(2, N.replaceAll(list, 9, 7));
        assertEquals(Arrays.asList(1, 7, 3, 7), list);
        assertEquals(0, list.setCalls);

        N.replaceAll(list, x -> x * 10);
        assertEquals(Arrays.asList(10, 70, 30, 70), list);
        assertEquals(0, list.setCalls);

        // the predicate is evaluated once per element, in order; earlier replacements stay applied on failure
        final List<String> visited = new ArrayList<>();
        final List<String> words = new ArrayList<>(Arrays.asList("x", "\u4e2d", "stop", "last"));
        final IllegalStateException failure = new IllegalStateException("predicate");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> N.replaceIf(words, w -> {
            visited.add(w);
            if ("stop".equals(w)) {
                throw failure;
            }
            return true;
        }, "\uD83D\uDE00")));
        assertEquals(Arrays.asList("x", "\u4e2d", "stop"), visited);
        assertEquals(Arrays.asList("\uD83D\uDE00", "\uD83D\uDE00", "stop", "last"), words);

        // UOE only if something matches; CopyOnWriteArrayList; null values
        final List<Integer> ro = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(1, 2)));
        assertEquals(0, N.replaceIf(ro, x -> false, 5));
        assertEquals(0, N.replaceAll(ro, 3, 5));
        assertThrows(UnsupportedOperationException.class, () -> N.replaceIf(ro, x -> x == 2, 5));
        final CopyOnWriteArrayList<String> cow = new CopyOnWriteArrayList<>(Arrays.asList("a", null, "a"));
        assertEquals(1, N.replaceAll(cow, null, "n"));
        assertEquals(2, N.replaceAll(cow, "a", "b"));
        assertEquals(Arrays.asList("b", "n", "b"), cow);
    }

    // ------------------------------------------------------------------ C-266

    @Test
    public void testC266_pathIsALeafForFlattenEachElement() {
        final Path path = Path.of("dir", "file.txt");
        assertEquals(Arrays.asList("x", path, "y"), N.flattenEachElement(Arrays.asList("x", path, Arrays.asList("y"))));
        assertEquals(Arrays.asList(Path.of("a"), Path.of("b")), N.flattenEachElement(Path.of("a", "b")));
    }

    // ------------------------------------------------------------------ C-271 / C-272 / C-276

    @Test
    public void testC271_anErrorFromAPoolThreadCommandIsRethrownUnchanged() {
        final AssertionError boom = new AssertionError("boom");
        final Throwables.Runnable<Exception> ok = () -> {
        };
        final Throwables.Runnable<Exception> err = () -> {
            throw boom;
        };
        final java.util.concurrent.Callable<Integer> cok = () -> 1;
        final java.util.concurrent.Callable<Integer> cerr = () -> {
            throw boom;
        };

        assertSame(boom, assertThrows(AssertionError.class, () -> N.runInParallel(ok, err)));
        assertSame(boom, assertThrows(AssertionError.class, () -> N.runInParallel(ok, ok, err)));
        assertSame(boom, assertThrows(AssertionError.class, () -> N.runInParallel(ok, ok, ok, err)));
        assertSame(boom, assertThrows(AssertionError.class, () -> N.runInParallel(ok, ok, ok, ok, err)));
        assertSame(boom, assertThrows(AssertionError.class, () -> N.runInParallel(Arrays.asList(ok, err))));
        assertSame(boom, assertThrows(AssertionError.class, () -> N.callInParallel(cok, cerr)));
        assertSame(boom, assertThrows(AssertionError.class, () -> N.callInParallel(cok, cok, cerr)));
        assertSame(boom, assertThrows(AssertionError.class, () -> N.callInParallel(cok, cok, cok, cerr)));
        assertSame(boom, assertThrows(AssertionError.class, () -> N.callInParallel(cok, cok, cok, cok, cerr)));
        assertSame(boom, assertThrows(AssertionError.class, () -> N.callInParallel(Arrays.asList(cok, cerr))));
        // the first command (calling thread) was already raw
        assertSame(boom, assertThrows(AssertionError.class, () -> N.runInParallel(err, ok)));

        final ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            assertSame(boom, assertThrows(AssertionError.class, () -> N.runInParallel(Arrays.asList(ok, err), pool)));
            assertSame(boom, assertThrows(AssertionError.class, () -> N.callInParallel(Arrays.asList(cok, cerr), pool)));
            assertSame(boom, assertThrows(AssertionError.class, () -> N.runAsync(Arrays.asList(err), pool).hasNext()));
            assertSame(boom, assertThrows(AssertionError.class, () -> N.callAsync(Arrays.asList(cerr), pool).hasNext()));
        } finally {
            pool.shutdownNow();
        }

        final Executor direct = Runnable::run;
        assertSame(boom, assertThrows(AssertionError.class, () -> N.runAsync(Arrays.asList(err), direct).hasNext()));
        assertSame(boom, assertThrows(AssertionError.class, () -> N.callAsync(Arrays.asList(cerr), direct).hasNext()));

        // checked exceptions are still wrapped
        final IOException io = new IOException("io");
        final RuntimeException wrapped = assertThrows(RuntimeException.class, () -> N.runInParallel(ok, () -> {
            throw io;
        }));
        assertSame(io, wrapped.getCause());
        final RuntimeException wrapped2 = assertThrows(RuntimeException.class, () -> N.callAsync(Arrays.<java.util.concurrent.Callable<Integer>> asList(() -> {
            throw io;
        }), direct).hasNext());
        assertSame(io, wrapped2.getCause());
    }

    @Test
    public void testC272_C276_nullCommandsAreRejectedBeforeAnythingRuns() {
        final AtomicInteger ran = new AtomicInteger();
        final Throwables.Runnable<Exception> ok = ran::incrementAndGet;
        final java.util.concurrent.Callable<Integer> cok = ran::incrementAndGet;
        final Executor direct = Runnable::run;

        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> N.runAsync(Arrays.asList(ok, null), direct));
        assertEquals("The command at index 1 in 'commands' cannot be null", e1.getMessage());
        assertThrows(IllegalArgumentException.class, () -> N.runAsync(Arrays.asList(ok, null)));
        assertThrows(IllegalArgumentException.class, () -> N.callAsync(Arrays.asList(cok, null), direct));
        assertThrows(IllegalArgumentException.class, () -> N.callAsync(Arrays.asList(null, cok)));

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> N.runInParallel(Arrays.asList(ok, ok, null), direct));
        assertEquals("The command at index 2 in 'commands' cannot be null", e2.getMessage());
        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> N.callInParallel(Arrays.asList(null, cok), direct));
        assertEquals("The command at index 0 in 'commands' cannot be null", e3.getMessage());
        assertEquals(0, ran.get(), "nothing may run before the null element is reported");

        // valid input still works
        assertEquals(Arrays.asList(1, 2), N.callInParallel(Arrays.asList(() -> 1, () -> 2), direct));
    }

    // ------------------------------------------------------------------ C-278

    @Test
    public void testC278_minusOneMinusOneIsAnEmptyReverseRange() {
        final List<Object> seen = new ArrayList<>();
        N.forEach(new String[0], -1, -1, seen::add);
        N.forEach(new String[] { "a", "b" }, -1, -1, seen::add);
        N.forEach((String[]) null, -1, -1, seen::add);
        N.forEach(new ArrayList<String>(), -1, -1, seen::add);
        N.forEach(Arrays.asList("a"), -1, -1, seen::add);
        N.forEachIndexed(new String[0], -1, -1, (i, e) -> seen.add(e));
        N.forEachIndexed(new LinkedList<String>(), -1, -1, (i, e) -> seen.add(e));
        assertTrue(seen.isEmpty());

        // the reverse idiom still covers everything
        N.forEach(new String[] { "a", "b", "\uD83D\uDE80" }, 2, -1, seen::add);
        assertEquals(Arrays.asList("\uD83D\uDE80", "b", "a"), seen);
        // other bad ranges still throw; the action is still validated
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(new String[] { "a" }, -2, -1, e -> {
        }));
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(new String[] { "a" }, -1, 0, e -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> N.forEach(new String[0], -1, -1, (Throwables.Consumer<String, RuntimeException>) null));
    }

    // ------------------------------------------------------------------ C-311

    @Test
    public void testC311_numberTypedIntAndLongAggregatesConvertExactly() {
        assertThrows(ArithmeticException.class, () -> N.sumInt(new Long[] { 3_000_000_000L }));
        assertThrows(ArithmeticException.class, () -> N.sumInt(new Long[] { 1L, 4294967297L }, 0, 2));
        assertThrows(ArithmeticException.class, () -> N.sumInt(Arrays.<Number> asList(BigInteger.ONE.shiftLeft(64).add(BigInteger.ONE)), 0, 1));
        assertThrows(ArithmeticException.class, () -> N.sumInt(Arrays.<Number> asList(3e9)));
        assertThrows(ArithmeticException.class, () -> N.sumIntToLong(Arrays.<Number> asList(Double.NaN)));
        assertThrows(ArithmeticException.class, () -> N.averageInt(new Double[] { Double.POSITIVE_INFINITY }));
        assertThrows(ArithmeticException.class, () -> N.averageInt(new Long[] { 1L, 3_000_000_000L }, 0, 2));
        assertThrows(ArithmeticException.class, () -> N.averageInt(Arrays.<Number> asList(4294967297L), 0, 1));
        assertThrows(ArithmeticException.class, () -> N.averageInt(Arrays.<Number> asList(4294967297L)));
        assertThrows(ArithmeticException.class, () -> N.averageLong(new Number[] { new BigDecimal("1e19") }));
        assertThrows(ArithmeticException.class, () -> N.averageLong(new Number[] { 1, Double.NaN }, 0, 2));
        assertThrows(ArithmeticException.class, () -> N.averageLong(Arrays.<Number> asList(Double.NEGATIVE_INFINITY), 0, 1));
        assertThrows(ArithmeticException.class, () -> N.averageLong(Arrays.<Number> asList(BigInteger.ONE.shiftLeft(64))));

        // fractions truncate, null counts as 0, in-range values unchanged
        assertEquals(1, N.sumInt(new Double[] { 1.7 }));
        assertEquals(3, N.sumInt(Arrays.<Number> asList(1, null, 2L, (short) 0, 0.9)));
        assertEquals(3L, N.sumIntToLong(Arrays.<Number> asList(1, null, 2L)));
        assertEquals(1.0, N.averageInt(new Integer[] { 1, null, 2 }));
        assertEquals(1.0, N.averageLong(Arrays.<Number> asList(1.7, null, 2L)));
        assertEquals((double) Long.MAX_VALUE, N.averageLong(new Long[] { Long.MAX_VALUE }));
        // sumLong keeps its documented longValue() conversion
        assertEquals(-1L, N.sumLong(new Number[] { new BigInteger("18446744073709551615") }));
    }
}
