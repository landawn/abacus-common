package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;

@Tag("new-test")
public class Iterables200Test extends TestBase {

    private <T> List<T> list(T... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    private <T> Set<T> set(T... elements) {
        return new LinkedHashSet<>(Arrays.asList(elements));
    }

    private NavigableSet<Integer> navigableSet(Integer... elements) {
        return new TreeSet<>(Arrays.asList(elements));
    }

    @Test
    public void testMinCharArray() {
        assertTrue(Iterables.min((char[]) null).isEmpty());
        assertTrue(Iterables.min(new char[0]).isEmpty());
        assertEquals('a', Iterables.min('a', 'b', 'c').get());
        assertEquals('a', Iterables.min('c', 'b', 'a').get());
        assertEquals('a', Iterables.min('a', 'a', 'a').get());
    }

    @Test
    public void testMinByteArray() {
        assertTrue(Iterables.min((byte[]) null).isEmpty());
        assertTrue(Iterables.min(new byte[0]).isEmpty());
        assertEquals((byte) 1, Iterables.min((byte) 1, (byte) 2, (byte) 3).get());
        assertEquals((byte) 1, Iterables.min((byte) 3, (byte) 2, (byte) 1).get());
    }

    @Test
    public void testMinShortArray() {
        assertTrue(Iterables.min((short[]) null).isEmpty());
        assertTrue(Iterables.min(new short[0]).isEmpty());
        assertEquals((short) 1, Iterables.min((short) 1, (short) 2, (short) 3).get());
        assertEquals((short) 1, Iterables.min((short) 3, (short) 2, (short) 1).get());
    }

    @Test
    public void testMinIntArray() {
        assertTrue(Iterables.min((int[]) null).isEmpty());
        assertTrue(Iterables.min(new int[0]).isEmpty());
        assertEquals(1, Iterables.min(1, 2, 3).get());
        assertEquals(1, Iterables.min(3, 2, 1).get());
    }

    @Test
    public void testMinLongArray() {
        assertTrue(Iterables.min((long[]) null).isEmpty());
        assertTrue(Iterables.min(new long[0]).isEmpty());
        assertEquals(1L, Iterables.min(1L, 2L, 3L).get());
        assertEquals(1L, Iterables.min(3L, 2L, 1L).get());
    }

    @Test
    public void testMinFloatArray() {
        assertTrue(Iterables.min((float[]) null).isEmpty());
        assertTrue(Iterables.min(new float[0]).isEmpty());
        assertEquals(1.0f, Iterables.min(1.0f, 2.0f, 3.0f).get());
        assertEquals(1.0f, Iterables.min(3.0f, 2.0f, 1.0f).get());
        assertEquals(1.0, Iterables.min(Float.NaN, 1.0f).get());
    }

    @Test
    public void testMinDoubleArray() {
        assertTrue(Iterables.min((double[]) null).isEmpty());
        assertTrue(Iterables.min(new double[0]).isEmpty());
        assertEquals(1.0, Iterables.min(1.0, 2.0, 3.0).get());
        assertEquals(1.0, Iterables.min(3.0, 2.0, 1.0).get());
        assertEquals(1.0, Iterables.min(Double.NaN, 1.0).get());
    }

    @Test
    public void testMinGenericArrayComparable() {
        assertTrue(Iterables.min((String[]) null).isEmpty());
        assertTrue(Iterables.min(new String[0]).isEmpty());
        assertEquals("a", Iterables.min(new String[] { "a", "b", "c" }).get());
        assertEquals("a", Iterables.min(new String[] { "c", "b", "a" }).get());
        assertEquals("a", Iterables.min(new String[] { "a", null, "c" }).get());
        assertEquals("a", Iterables.min(new String[] { null, "a", "c" }).get());
    }

    @Test
    public void testMinGenericArrayComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.min((String[]) null, reverseOrder).isEmpty());
        assertTrue(Iterables.min(new String[0], reverseOrder).isEmpty());
        assertEquals("c", Iterables.min(new String[] { "a", "b", "c" }, reverseOrder).get());
        assertEquals("a", Iterables.min(new String[] { "a", "b", "c" }, Comparator.naturalOrder()).get());
        assertEquals("a", Iterables.min(new String[] { "a", null, "c" }, Comparators.nullsLast(Comparator.<String> naturalOrder())).get());
    }

    @Test
    public void testMinIterableComparable() {
        assertTrue(Iterables.min((Iterable<String>) null).isEmpty());
        assertTrue(Iterables.min(list()).isEmpty());
        assertEquals("a", Iterables.min(list("a", "b", "c")).get());
        assertEquals("a", Iterables.min(list("c", "b", "a")).get());
        assertEquals("a", Iterables.min(list("a", null, "c")).get());
    }

    @Test
    public void testMinIterableComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.min((Iterable<String>) null, reverseOrder).isEmpty());
        assertTrue(Iterables.min(list(), reverseOrder).isEmpty());
        assertEquals("c", Iterables.min(list("a", "b", "c"), reverseOrder).get());
        assertEquals("a", Iterables.min(list("a", "b", "c"), Comparator.naturalOrder()).get());
        assertEquals("a", Iterables.min(list("a", null, "c"), Comparator.nullsLast(Comparator.naturalOrder())).get());
        assertNull(Iterables.min(list("a", null, "c"), Comparator.nullsFirst(Comparator.naturalOrder())).get());
    }

    @Test
    public void testMinIteratorComparable() {
        assertTrue(Iterables.min((Iterator<String>) null).isEmpty());
        assertTrue(Iterables.min(list(N.EMPTY_STRING_ARRAY).iterator()).isEmpty());
        assertEquals("a", Iterables.min(list("a", "b", "c").iterator()).get());
        assertEquals("a", Iterables.min(list("c", "b", "a").iterator()).get());
        assertEquals("a", Iterables.min(list("a", null, "c").iterator()).get());
    }

    @Test
    public void testMinIteratorComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.min((Iterator<String>) null, reverseOrder).isEmpty());
        assertTrue(Iterables.min(list(N.EMPTY_STRING_ARRAY).iterator(), reverseOrder).isEmpty());
        assertEquals("c", Iterables.min(list("a", "b", "c").iterator(), reverseOrder).get());
        assertEquals("a", Iterables.min(list("a", "b", "c").iterator(), Comparator.naturalOrder()).get());
        assertEquals("a", Iterables.min(list("a", null, "c").iterator(), Comparator.nullsLast(Comparator.naturalOrder())).get());
        assertNull(Iterables.min(list("a", null, "c").iterator(), Comparator.nullsFirst(Comparator.naturalOrder())).get());
    }

    @Test
    public void testMinByArray() {
        assertTrue(Iterables.minBy((String[]) null, String::length).isEmpty());
        assertTrue(Iterables.minBy(new String[0], String::length).isEmpty());
        assertEquals("a", Iterables.minBy(new String[] { "aaa", "a", "bb" }, String::length).get());
        assertEquals("bb", Iterables.minBy(new String[] { "aaa", null, "bb" }, s -> s == null ? 1000 : s.length()).get());
    }

    @Test
    public void testMinByIterable() {
        assertTrue(Iterables.minBy((List<String>) null, String::length).isEmpty());
        assertTrue(Iterables.minBy(list(), String::length).isEmpty());
        assertEquals("a", Iterables.minBy(list("aaa", "a", "bb"), String::length).get());
        assertEquals("bb", Iterables.minBy(list("aaa", null, "bb"), s -> s == null ? 1000 : s.length()).get());
    }

    @Test
    public void testMinByIterator() {
        assertTrue(Iterables.minBy((Iterator<String>) null, String::length).isEmpty());
        assertTrue(Iterables.minBy(list(N.EMPTY_STRING_ARRAY).iterator(), String::length).isEmpty());
        assertEquals("a", Iterables.minBy(list("aaa", "a", "bb").iterator(), String::length).get());
        assertEquals("bb", Iterables.minBy(list("aaa", null, "bb").iterator(), s -> s == null ? 1000 : s.length()).get());
    }

    @Test
    public void testMinIntArrayExtractor() {
        assertTrue(Iterables.minInt((String[]) null, String::length).isEmpty());
        assertTrue(Iterables.minInt(new String[0], String::length).isEmpty());
        assertEquals(1, Iterables.minInt(new String[] { "aaa", "a", "bb" }, String::length).get());
        assertEquals(3, Iterables.minInt(new String[] { "aaa" }, String::length).get());
    }

    @Test
    public void testMinIntIterableExtractor() {
        assertTrue(Iterables.minInt((List<String>) null, String::length).isEmpty());
        assertTrue(Iterables.minInt(list(), String::length).isEmpty());
        assertEquals(1, Iterables.minInt(list("aaa", "a", "bb"), String::length).get());
    }

    @Test
    public void testMinIntIteratorExtractor() {
        assertTrue(Iterables.minInt((Iterator<String>) null, String::length).isEmpty());
        assertTrue(Iterables.minInt(list(N.EMPTY_STRING_ARRAY).iterator(), String::length).isEmpty());
        assertEquals(1, Iterables.minInt(list("aaa", "a", "bb").iterator(), String::length).get());
    }

    @Test
    public void testMinLongArrayExtractor() {
        ToIntFunction<String> lengthToInt = String::length;
        ToLongFunction<String> lengthToLong = s -> (long) lengthToInt.applyAsInt(s);
        assertTrue(Iterables.minLong((String[]) null, lengthToLong).isEmpty());
        assertTrue(Iterables.minLong(new String[0], lengthToLong).isEmpty());
        assertEquals(1L, Iterables.minLong(new String[] { "aaa", "a", "bb" }, lengthToLong).get());
    }

    @Test
    public void testMinLongIterableExtractor() {
        ToLongFunction<String> lengthToLong = s -> (long) s.length();
        assertTrue(Iterables.minLong((List<String>) null, lengthToLong).isEmpty());
        assertTrue(Iterables.minLong(list(), lengthToLong).isEmpty());
        assertEquals(1L, Iterables.minLong(list("aaa", "a", "bb"), lengthToLong).get());
    }

    @Test
    public void testMinLongIteratorExtractor() {
        ToLongFunction<String> lengthToLong = s -> (long) s.length();
        assertTrue(Iterables.minLong((Iterator<String>) null, lengthToLong).isEmpty());
        assertTrue(Iterables.minLong(list(N.EMPTY_STRING_ARRAY).iterator(), lengthToLong).isEmpty());
        assertEquals(1L, Iterables.minLong(list("aaa", "a", "bb").iterator(), lengthToLong).get());
    }

    @Test
    public void testMinDoubleArrayExtractor() {
        ToDoubleFunction<String> lengthToDouble = s -> (double) s.length();
        assertTrue(Iterables.minDouble((String[]) null, lengthToDouble).isEmpty());
        assertTrue(Iterables.minDouble(new String[0], lengthToDouble).isEmpty());
        assertEquals(1.0, Iterables.minDouble(new String[] { "aaa", "a", "bb" }, lengthToDouble).get());
    }

    @Test
    public void testMinDoubleIterableExtractor() {
        ToDoubleFunction<String> lengthToDouble = s -> (double) s.length();
        assertTrue(Iterables.minDouble((List<String>) null, lengthToDouble).isEmpty());
        assertTrue(Iterables.minDouble(list(), lengthToDouble).isEmpty());
        assertEquals(1.0, Iterables.minDouble(list("aaa", "a", "bb"), lengthToDouble).get());
    }

    @Test
    public void testMinDoubleIteratorExtractor() {
        ToDoubleFunction<String> lengthToDouble = s -> (double) s.length();
        assertTrue(Iterables.minDouble((Iterator<String>) null, lengthToDouble).isEmpty());
        assertTrue(Iterables.minDouble(list(N.EMPTY_STRING_ARRAY).iterator(), lengthToDouble).isEmpty());
        assertEquals(1.0, Iterables.minDouble(list("aaa", "a", "bb").iterator(), lengthToDouble).get());
    }

    @Test
    public void testMaxCharArray() {
        assertTrue(Iterables.max((char[]) null).isEmpty());
        assertTrue(Iterables.max(new char[0]).isEmpty());
        assertEquals('c', Iterables.max('a', 'b', 'c').get());
        assertEquals('c', Iterables.max('c', 'b', 'a').get());
    }

    @Test
    public void testMaxByteArray() {
        assertTrue(Iterables.max((byte[]) null).isEmpty());
        assertTrue(Iterables.max(new byte[0]).isEmpty());
        assertEquals((byte) 3, Iterables.max((byte) 1, (byte) 2, (byte) 3).get());
    }

    @Test
    public void testMaxShortArray() {
        assertTrue(Iterables.max((short[]) null).isEmpty());
        assertTrue(Iterables.max(new short[0]).isEmpty());
        assertEquals((short) 3, Iterables.max((short) 1, (short) 2, (short) 3).get());
    }

    @Test
    public void testMaxIntArray() {
        assertTrue(Iterables.max((int[]) null).isEmpty());
        assertTrue(Iterables.max(new int[0]).isEmpty());
        assertEquals(3, Iterables.max(1, 2, 3).get());
    }

    @Test
    public void testMaxLongArray() {
        assertTrue(Iterables.max((long[]) null).isEmpty());
        assertTrue(Iterables.max(new long[0]).isEmpty());
        assertEquals(3L, Iterables.max(1L, 2L, 3L).get());
    }

    @Test
    public void testMaxFloatArray() {
        assertTrue(Iterables.max((float[]) null).isEmpty());
        assertTrue(Iterables.max(new float[0]).isEmpty());
        assertEquals(3.0f, Iterables.max(1.0f, 2.0f, 3.0f).get());
        assertEquals(1.0, Iterables.max(Float.NaN, 1.0f).get());
    }

    @Test
    public void testMaxDoubleArray() {
        assertTrue(Iterables.max((double[]) null).isEmpty());
        assertTrue(Iterables.max(new double[0]).isEmpty());
        assertEquals(3.0, Iterables.max(1.0, 2.0, 3.0).get());
        assertEquals(1.0, Iterables.max(Double.NaN, 1.0).get());
    }

    @Test
    public void testMaxGenericArrayComparable() {
        assertTrue(Iterables.max((String[]) null).isEmpty());
        assertTrue(Iterables.max(new String[0]).isEmpty());
        assertEquals("c", Iterables.max(new String[] { "a", "b", "c" }).get());
        assertEquals("c", Iterables.max(new String[] { "a", null, "c" }).get());
        assertNull(Iterables.max(new String[] { null, null }).get());
    }

    @Test
    public void testMaxGenericArrayComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.max((String[]) null, reverseOrder).isEmpty());
        assertTrue(Iterables.max(new String[0], reverseOrder).isEmpty());
        assertEquals("a", Iterables.max(new String[] { "a", "b", "c" }, reverseOrder).get());
        assertEquals("c", Iterables.max(new String[] { "a", "b", "c" }, Comparator.naturalOrder()).get());
        assertEquals("c", Iterables.max(new String[] { "a", null, "c" }, Comparator.nullsFirst(Comparator.naturalOrder())).get());
        assertNull(Iterables.max(new String[] { "a", null, "c" }, Comparator.nullsLast(Comparator.naturalOrder())).get());
    }

    @Test
    public void testMaxIterableComparable() {
        assertTrue(Iterables.max((Iterable<String>) null).isEmpty());
        assertTrue(Iterables.max(list()).isEmpty());
        assertEquals("c", Iterables.max(list("a", "b", "c")).get());
        assertEquals("c", Iterables.max(list("a", null, "c")).get());
    }

    @Test
    public void testMaxIterableComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.max((Iterable<String>) null, reverseOrder).isEmpty());
        assertTrue(Iterables.max(list(), reverseOrder).isEmpty());
        assertEquals("a", Iterables.max(list("a", "b", "c"), reverseOrder).get());
        assertEquals("c", Iterables.max(list("a", "b", "c"), Comparator.naturalOrder()).get());
        assertEquals("c", Iterables.max(list("a", null, "c"), Comparator.nullsFirst(Comparator.naturalOrder())).get());
        assertNull(Iterables.max(list("a", null, "c"), Comparator.nullsLast(Comparator.naturalOrder())).get());
    }

    @Test
    public void testMaxIteratorComparable() {
        assertTrue(Iterables.max((Iterator<String>) null).isEmpty());
        assertTrue(Iterables.max(list(N.EMPTY_STRING_ARRAY).iterator()).isEmpty());
        assertEquals("c", Iterables.max(list("a", "b", "c").iterator()).get());
        assertEquals("c", Iterables.max(list("a", null, "c").iterator()).get());
    }

    @Test
    public void testMaxIteratorComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.max((Iterator<String>) null, reverseOrder).isEmpty());
        assertTrue(Iterables.max(list(N.EMPTY_STRING_ARRAY).iterator(), reverseOrder).isEmpty());
        assertEquals("a", Iterables.max(list("a", "b", "c").iterator(), reverseOrder).get());
        assertEquals("c", Iterables.max(list("a", "b", "c").iterator(), Comparator.naturalOrder()).get());
        assertEquals("c", Iterables.max(list("a", null, "c").iterator(), Comparator.nullsFirst(Comparator.naturalOrder())).get());
        assertNull(Iterables.max(list("a", null, "c").iterator(), Comparator.nullsLast(Comparator.naturalOrder())).get());
    }

    @Test
    public void testMaxByArray() {
        assertTrue(Iterables.maxBy((String[]) null, String::length).isEmpty());
        assertTrue(Iterables.maxBy(new String[0], String::length).isEmpty());
        assertEquals("aaa", Iterables.maxBy(new String[] { "aaa", "a", "bb" }, String::length).get());
        assertEquals("aaa", Iterables.maxBy(new String[] { "aaa", null, "bb" }, s -> s == null ? -1 : s.length()).get());
    }

    @Test
    public void testMaxByIterable() {
        assertTrue(Iterables.maxBy((List<String>) null, String::length).isEmpty());
        assertTrue(Iterables.maxBy(list(), String::length).isEmpty());
        assertEquals("aaa", Iterables.maxBy(list("aaa", "a", "bb"), String::length).get());
        assertEquals("aaa", Iterables.maxBy(list("aaa", null, "bb"), s -> s == null ? -1 : s.length()).get());
    }

    @Test
    public void testMaxByIterator() {
        assertTrue(Iterables.maxBy((Iterator<String>) null, String::length).isEmpty());
        assertTrue(Iterables.maxBy(list(N.EMPTY_STRING_ARRAY).iterator(), String::length).isEmpty());
        assertEquals("aaa", Iterables.maxBy(list("aaa", "a", "bb").iterator(), String::length).get());
        assertEquals("aaa", Iterables.maxBy(list("aaa", null, "bb").iterator(), s -> s == null ? -1 : s.length()).get());
    }

    @Test
    public void testMaxIntArrayExtractor() {
        assertTrue(Iterables.maxInt((String[]) null, String::length).isEmpty());
        assertTrue(Iterables.maxInt(new String[0], String::length).isEmpty());
        assertEquals(3, Iterables.maxInt(new String[] { "aaa", "a", "bb" }, String::length).get());
    }

    @Test
    public void testMaxIntIterableExtractor() {
        assertTrue(Iterables.maxInt((List<String>) null, String::length).isEmpty());
        assertTrue(Iterables.maxInt(list(), String::length).isEmpty());
        assertEquals(3, Iterables.maxInt(list("aaa", "a", "bb"), String::length).get());
    }

    @Test
    public void testMaxIntIteratorExtractor() {
        assertTrue(Iterables.maxInt((Iterator<String>) null, String::length).isEmpty());
        assertTrue(Iterables.maxInt(list(N.EMPTY_STRING_ARRAY).iterator(), String::length).isEmpty());
        assertEquals(3, Iterables.maxInt(list("aaa", "a", "bb").iterator(), String::length).get());
    }

    @Test
    public void testMaxLongArrayExtractor() {
        ToLongFunction<String> lengthToLong = s -> (long) s.length();
        assertTrue(Iterables.maxLong((String[]) null, lengthToLong).isEmpty());
        assertTrue(Iterables.maxLong(new String[0], lengthToLong).isEmpty());
        assertEquals(3L, Iterables.maxLong(new String[] { "aaa", "a", "bb" }, lengthToLong).get());
    }

    @Test
    public void testMaxLongIterableExtractor() {
        ToLongFunction<String> lengthToLong = s -> (long) s.length();
        assertTrue(Iterables.maxLong((List<String>) null, lengthToLong).isEmpty());
        assertTrue(Iterables.maxLong(list(), lengthToLong).isEmpty());
        assertEquals(3L, Iterables.maxLong(list("aaa", "a", "bb"), lengthToLong).get());
    }

    @Test
    public void testMaxLongIteratorExtractor() {
        ToLongFunction<String> lengthToLong = s -> (long) s.length();
        assertTrue(Iterables.maxLong((Iterator<String>) null, lengthToLong).isEmpty());
        assertTrue(Iterables.maxLong(list(N.EMPTY_STRING_ARRAY).iterator(), lengthToLong).isEmpty());
        assertEquals(3L, Iterables.maxLong(list("aaa", "a", "bb").iterator(), lengthToLong).get());
    }

    @Test
    public void testMaxDoubleArrayExtractor() {
        ToDoubleFunction<String> lengthToDouble = s -> (double) s.length();
        assertTrue(Iterables.maxDouble((String[]) null, lengthToDouble).isEmpty());
        assertTrue(Iterables.maxDouble(new String[0], lengthToDouble).isEmpty());
        assertEquals(3.0, Iterables.maxDouble(new String[] { "aaa", "a", "bb" }, lengthToDouble).get());
    }

    @Test
    public void testMaxDoubleIterableExtractor() {
        ToDoubleFunction<String> lengthToDouble = s -> (double) s.length();
        assertTrue(Iterables.maxDouble((List<String>) null, lengthToDouble).isEmpty());
        assertTrue(Iterables.maxDouble(list(), lengthToDouble).isEmpty());
        assertEquals(3.0, Iterables.maxDouble(list("aaa", "a", "bb"), lengthToDouble).get());
    }

    @Test
    public void testMaxDoubleIteratorExtractor() {
        ToDoubleFunction<String> lengthToDouble = s -> (double) s.length();
        assertTrue(Iterables.maxDouble((Iterator<String>) null, lengthToDouble).isEmpty());
        assertTrue(Iterables.maxDouble(list(N.EMPTY_STRING_ARRAY).iterator(), lengthToDouble).isEmpty());
        assertEquals(3.0, Iterables.maxDouble(list("aaa", "a", "bb").iterator(), lengthToDouble).get());
    }

    @Test
    public void testMinMaxArrayComparable() {
        assertTrue(Iterables.minMax((String[]) null).isEmpty());
        assertTrue(Iterables.minMax(new String[0]).isEmpty());
        Pair<String, String> result = Iterables.minMax(new String[] { "b", "a", "c" }).get();
        assertEquals("a", result.left());
        assertEquals("c", result.right());
        result = Iterables.minMax(new String[] { "a" }).get();
        assertEquals("a", result.left());
        assertEquals("a", result.right());
        result = Iterables.minMax(new String[] { "b", null, "a", "c" }).get();
        assertEquals(null, result.left());
        assertEquals("c", result.right());
    }

    @Test
    public void testMinMaxArrayComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.minMax((String[]) null, reverseOrder).isEmpty());
        assertTrue(Iterables.minMax(new String[0], reverseOrder).isEmpty());
        Pair<String, String> result = Iterables.minMax(new String[] { "b", "a", "c" }, reverseOrder).get();
        assertEquals("c", result.left());
        assertEquals("a", result.right());
    }

    @Test
    public void testMinMaxIterableComparable() {
        assertTrue(Iterables.minMax((Iterable<String>) null).isEmpty());
        assertTrue(Iterables.minMax(list()).isEmpty());
        Pair<String, String> result = Iterables.minMax(list("b", "a", "c")).get();
        assertEquals("a", result.left());
        assertEquals("c", result.right());
    }

    @Test
    public void testMinMaxIterableComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.minMax((Iterable<String>) null, reverseOrder).isEmpty());
        assertTrue(Iterables.minMax(list(), reverseOrder).isEmpty());
        Pair<String, String> result = Iterables.minMax(list("b", "a", "c"), reverseOrder).get();
        assertEquals("c", result.left());
        assertEquals("a", result.right());
    }

    @Test
    public void testMinMaxIteratorComparable() {
        assertTrue(Iterables.minMax((Iterator<String>) null).isEmpty());
        assertTrue(Iterables.minMax(list(N.EMPTY_STRING_ARRAY).iterator()).isEmpty());
        Pair<String, String> result = Iterables.minMax(list("b", "a", "c").iterator()).get();
        assertEquals("a", result.left());
        assertEquals("c", result.right());
    }

    @Test
    public void testMinMaxIteratorComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.minMax((Iterator<String>) null, reverseOrder).isEmpty());
        assertTrue(Iterables.minMax(list(N.EMPTY_STRING_ARRAY).iterator(), reverseOrder).isEmpty());
        Pair<String, String> result = Iterables.minMax(list("b", "a", "c").iterator(), reverseOrder).get();
        assertEquals("c", result.left());
        assertEquals("a", result.right());
    }

    @Test
    public void testMedianArrayComparable() {
        assertTrue(Iterables.median((String[]) null).isEmpty());
        assertTrue(Iterables.median(new String[0]).isEmpty());
        assertEquals("b", Iterables.median(new String[] { "a", "b", "c" }).get());
        assertEquals("b", Iterables.median(new String[] { "c", "b", "a" }).get());
        assertEquals("a", Iterables.median(new String[] { "a", "b" }).get());
        assertEquals("b", Iterables.median(new String[] { "a", "b", "c", "d" }).get());
    }

    @Test
    public void testMedianArrayComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.median((String[]) null, reverseOrder).isEmpty());
        assertTrue(Iterables.median(new String[0], reverseOrder).isEmpty());
        assertEquals("b", Iterables.median(new String[] { "a", "b", "c" }, reverseOrder).get());
        assertEquals("b", Iterables.median(new String[] { "a", "b" }, reverseOrder).get());
    }

    @Test
    public void testMedianCollectionComparable() {
        assertTrue(Iterables.median((Collection<String>) null).isEmpty());
        assertTrue(Iterables.median(list()).isEmpty());
        assertEquals("b", Iterables.median(list("a", "b", "c")).get());
    }

    @Test
    public void testMedianCollectionComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.median((Collection<String>) null, reverseOrder).isEmpty());
        assertTrue(Iterables.median(list(), reverseOrder).isEmpty());
        assertEquals("b", Iterables.median(list("a", "b", "c"), reverseOrder).get());
    }

    @Test
    public void testKthLargestArrayComparable() {
        assertTrue(Iterables.kthLargest((String[]) null, 1).isEmpty());
        assertTrue(Iterables.kthLargest(new String[0], 1).isEmpty());
        assertTrue(Iterables.kthLargest(new String[] { "a", "b" }, 3).isEmpty());

        assertEquals("c", Iterables.kthLargest(new String[] { "a", "b", "c" }, 1).get());
        assertEquals("b", Iterables.kthLargest(new String[] { "a", "b", "c" }, 2).get());
        assertEquals("a", Iterables.kthLargest(new String[] { "a", "b", "c" }, 3).get());
    }

    @Test
    public void testKthLargestArrayComparator() {
        Comparator<String> reverseOrder = String.CASE_INSENSITIVE_ORDER.reversed();
        assertTrue(Iterables.kthLargest((String[]) null, 1, reverseOrder).isEmpty());
        String[] arr = { "c", "A", "b" };
        assertEquals("A", Iterables.kthLargest(arr, 1, reverseOrder).get());
        assertEquals("b", Iterables.kthLargest(arr, 2, reverseOrder).get());
        assertEquals("c", Iterables.kthLargest(arr, 3, reverseOrder).get());
    }

    @Test
    public void testKthLargestCollectionComparable() {
        assertTrue(Iterables.kthLargest((Collection<String>) null, 1).isEmpty());
        assertTrue(Iterables.kthLargest(list(), 1).isEmpty());
        List<String> l = list("a", "b", "c");
        assertEquals("c", Iterables.kthLargest(l, 1).get());
        assertEquals("b", Iterables.kthLargest(l, 2).get());
    }

    @Test
    public void testKthLargestCollectionComparator() {
        Comparator<String> reverseOrder = String.CASE_INSENSITIVE_ORDER.reversed();
        assertTrue(Iterables.kthLargest((Collection<String>) null, 1, reverseOrder).isEmpty());
        List<String> l = list("c", "A", "b");
        assertEquals("A", Iterables.kthLargest(l, 1, reverseOrder).get());
        assertEquals("b", Iterables.kthLargest(l, 2, reverseOrder).get());
    }

    @Test
    public void testSumIntIterableNumber() {
        assertTrue(Iterables.sumInt((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.sumInt(list()).isEmpty());
        assertEquals(6, Iterables.sumInt(list(1, 2, 3)).get());
        assertEquals(0, Iterables.sumInt(list(1, -1, 0)).get());
    }

    @Test
    public void testSumIntIterableExtractor() {
        ToIntFunction<String> len = String::length;
        assertTrue(Iterables.sumInt((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.sumInt(list(), len).isEmpty());
        assertEquals(6, Iterables.sumInt(list("a", "bb", "ccc"), len).get());
    }

    @Test
    public void testSumIntToLongIterableNumber() {
        assertTrue(Iterables.sumIntToLong((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.sumIntToLong(list()).isEmpty());
        assertEquals(6L, Iterables.sumIntToLong(list(1, 2, 3)).get());
        assertEquals(2L * Integer.MAX_VALUE, Iterables.sumIntToLong(list(Integer.MAX_VALUE, Integer.MAX_VALUE)).get());
    }

    @Test
    public void testSumIntToLongIterableExtractor() {
        ToIntFunction<String> len = String::length;
        assertTrue(Iterables.sumIntToLong((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.sumIntToLong(list(), len).isEmpty());
        assertEquals(6L, Iterables.sumIntToLong(list("a", "bb", "ccc"), len).get());
    }

    @Test
    public void testSumLongIterableNumber() {
        assertTrue(Iterables.sumLong((Iterable<Long>) null).isEmpty());
        assertTrue(Iterables.sumLong(list()).isEmpty());
        assertEquals(6L, Iterables.sumLong(list(1L, 2L, 3L)).get());
    }

    @Test
    public void testSumLongIterableExtractor() {
        ToLongFunction<String> len = s -> (long) s.length();
        assertTrue(Iterables.sumLong((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.sumLong(list(), len).isEmpty());
        assertEquals(6L, Iterables.sumLong(list("a", "bb", "ccc"), len).get());
    }

    @Test
    public void testSumDoubleIterableNumber() {
        assertTrue(Iterables.sumDouble((Iterable<Double>) null).isEmpty());
        assertTrue(Iterables.sumDouble(list()).isEmpty());
        assertEquals(6.0, Iterables.sumDouble(list(1.0, 2.5, 2.5)).get(), 0.001);
    }

    @Test
    public void testSumDoubleIterableExtractor() {
        ToDoubleFunction<String> len = s -> (double) s.length();
        assertTrue(Iterables.sumDouble((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.sumDouble(list(), len).isEmpty());
        assertEquals(6.0, Iterables.sumDouble(list("a", "bb", "ccc"), len).get(), 0.001);
    }

    @Test
    public void testSumBigIntegerIterable() {
        assertTrue(Iterables.sumBigInteger((Iterable<BigInteger>) null).isEmpty());
        assertTrue(Iterables.sumBigInteger(list()).isEmpty());
        assertEquals(new BigInteger("6"), Iterables.sumBigInteger(list(new BigInteger("1"), new BigInteger("2"), new BigInteger("3"))).get());
    }

    @Test
    public void testSumBigIntegerIterableExtractor() {
        Function<String, BigInteger> lenToBi = s -> BigInteger.valueOf(s.length());
        assertTrue(Iterables.sumBigInteger((Iterable<String>) null, lenToBi).isEmpty());
        assertTrue(Iterables.sumBigInteger(list(), lenToBi).isEmpty());
        assertEquals(new BigInteger("6"), Iterables.sumBigInteger(list("a", "bb", "ccc"), lenToBi).get());
    }

    @Test
    public void testSumBigDecimalIterable() {
        assertTrue(Iterables.sumBigDecimal((Iterable<BigDecimal>) null).isEmpty());
        assertTrue(Iterables.sumBigDecimal(list()).isEmpty());
        assertEquals(new BigDecimal("6.0"), Iterables.sumBigDecimal(list(new BigDecimal("1.0"), new BigDecimal("2.5"), new BigDecimal("2.5"))).get());
    }

    @Test
    public void testSumBigDecimalIterableExtractor() {
        Function<String, BigDecimal> lenToBd = s -> BigDecimal.valueOf(s.length());
        assertTrue(Iterables.sumBigDecimal((Iterable<String>) null, lenToBd).isEmpty());
        assertTrue(Iterables.sumBigDecimal(list(), lenToBd).isEmpty());
        assertEquals(new BigDecimal("6"), Iterables.sumBigDecimal(list("a", "bb", "ccc"), lenToBd).get());
    }

    @Test
    public void testAverageIntArrayNumber() {
        assertTrue(Iterables.averageInt((Integer[]) null).isEmpty());
        assertTrue(Iterables.averageInt(new Integer[0]).isEmpty());
        assertEquals(2.0, Iterables.averageInt(new Integer[] { 1, 2, 3 }).get(), 0.001);
    }

    @Test
    public void testAverageIntArrayRangeNumber() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        assertEquals(3.0, Iterables.averageInt(arr, 0, 5).get(), 0.001);
        assertEquals(2.0, Iterables.averageInt(arr, 0, 3).get(), 0.001);
        assertEquals(4.0, Iterables.averageInt(arr, 2, 5).get(), 0.001);
        assertTrue(Iterables.averageInt(arr, 1, 1).isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, 3, 2));
    }

    @Test
    public void testAverageIntArrayExtractor() {
        ToIntFunction<String> len = String::length;
        assertTrue(Iterables.averageInt((String[]) null, len).isEmpty());
        assertTrue(Iterables.averageInt(new String[0], len).isEmpty());
        assertEquals(2.0, Iterables.averageInt(new String[] { "a", "bb", "ccc" }, len).get(), 0.001);
    }

    @Test
    public void testAverageIntArrayRangeExtractor() {
        String[] arr = { "a", "bb", "ccc", "dddd", "eeeee" };
        ToIntFunction<String> len = String::length;
        assertEquals(3.0, Iterables.averageInt(arr, 0, 5, len).get(), 0.001);
        assertEquals(2.0, Iterables.averageInt(arr, 0, 3, len).get(), 0.001);
        assertTrue(Iterables.averageInt(arr, 1, 1, len).isEmpty());
    }

    @Test
    public void testAverageIntCollectionRangeNumber() {
        List<Integer> coll = list(1, 2, 3, 4, 5);
        assertEquals(3.0, Iterables.averageInt(coll, 0, 5).get(), 0.001);
        assertEquals(2.0, Iterables.averageInt(coll, 0, 3).get(), 0.001);
        assertTrue(Iterables.averageInt(coll, 1, 1).isEmpty());
    }

    @Test
    public void testAverageIntCollectionRangeExtractor() {
        List<String> coll = list("a", "bb", "ccc", "dddd", "eeeee");
        ToIntFunction<String> len = String::length;
        assertEquals(3.0, Iterables.averageInt(coll, 0, 5, len).get(), 0.001);
        assertEquals(2.0, Iterables.averageInt(coll, 0, 3, len).get(), 0.001);
    }

    @Test
    public void testAverageIntIterableNumber() {
        assertTrue(Iterables.averageInt((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.averageInt(list()).isEmpty());
        assertEquals(2.0, Iterables.averageInt(list(1, 2, 3)).get(), 0.001);
    }

    @Test
    public void testAverageIntIterableExtractor() {
        ToIntFunction<String> len = String::length;
        assertTrue(Iterables.averageInt((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.averageInt(list(), len).isEmpty());
        assertEquals(2.0, Iterables.averageInt(list("a", "bb", "ccc"), len).get(), 0.001);
    }

    @Test
    public void testAverageDoubleArrayExtractor() {
        ToDoubleFunction<String> len = s -> (double) s.length();
        assertTrue(Iterables.averageDouble((String[]) null, len).isEmpty());
        assertTrue(Iterables.averageDouble(new String[0], len).isEmpty());
        assertEquals(2.0, Iterables.averageDouble(new String[] { "a", "bb", "ccc" }, len).get(), 0.001);
    }

    @Test
    public void testAverageDoubleIterableExtractor() {
        ToDoubleFunction<String> len = s -> (double) s.length();
        assertTrue(Iterables.averageDouble((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.averageDouble(list(), len).isEmpty());
        assertEquals(2.0, Iterables.averageDouble(list("a", "bb", "ccc"), len).get(), 0.001);
        List<Double> doubles = new ArrayList<>();
        doubles.add(1.0e100);
        doubles.add(1.0);
        doubles.add(-1.0e100);
        assertEquals(0.0 / 3.0, Iterables.averageDouble(doubles, d -> d).get(), 1e-15);

        doubles = new ArrayList<>();
        doubles.add(1.0e10);
        doubles.add(1.0);
        doubles.add(-1.0e10);
        assertEquals(1.0 / 3.0, Iterables.averageDouble(doubles, d -> d).get(), 1e-15);
    }

    @Test
    public void testAverageDoubleCollectionRangeExtractor() {
        List<String> coll = list("a", "bb", "ccc", "dddd", "eeeee");
        ToDoubleFunction<String> len = s -> (double) s.length();
        assertEquals(3.0, Iterables.averageDouble(coll, 0, 5, len).get(), 0.001);
        assertEquals(2.0, Iterables.averageDouble(coll, 0, 3, len).get(), 0.001);
        assertTrue(Iterables.averageDouble(coll, 1, 1, len).isEmpty());

        LinkedList<String> linkedList = new LinkedList<>(coll);
        assertEquals(3.0, Iterables.averageDouble(linkedList, 0, 5, len).get(), 0.001);
        assertEquals(4.0, Iterables.averageDouble(linkedList, 2, 5, len).get(), 0.001);
    }

    @Test
    public void testIndexOfArray() {
        assertTrue(Iterables.indexOf((String[]) null, "a").isEmpty());
        assertTrue(Iterables.indexOf(new String[0], "a").isEmpty());
        assertEquals(0, Iterables.indexOf(new String[] { "a", "b", "a" }, "a").get());
        assertEquals(1, Iterables.indexOf(new String[] { "a", "b", "a" }, "b").get());
        assertTrue(Iterables.indexOf(new String[] { "a", "b", "a" }, "c").isEmpty());
        assertEquals(1, Iterables.indexOf(new String[] { null, "a", null }, "a").get());
        assertEquals(0, Iterables.indexOf(new String[] { null, "a", null }, null).get());
    }

    @Test
    public void testIndexOfCollection() {
        assertTrue(Iterables.indexOf((List<String>) null, "a").isEmpty());
        assertTrue(Iterables.indexOf(list(), "a").isEmpty());
        assertEquals(0, Iterables.indexOf(list("a", "b", "a"), "a").get());
        assertEquals(1, Iterables.indexOf(list("a", "b", "a"), "b").get());
        assertTrue(Iterables.indexOf(list("a", "b", "a"), "c").isEmpty());
        assertEquals(1, Iterables.indexOf(list(null, "a", null), "a").get());
        assertEquals(0, Iterables.indexOf(list(null, "a", null), null).get());
    }

    @Test
    public void testLastIndexOfArray() {
        assertTrue(Iterables.lastIndexOf((String[]) null, "a").isEmpty());
        assertTrue(Iterables.lastIndexOf(new String[0], "a").isEmpty());
        assertEquals(2, Iterables.lastIndexOf(new String[] { "a", "b", "a" }, "a").get());
        assertEquals(1, Iterables.lastIndexOf(new String[] { "a", "b", "a" }, "b").get());
        assertTrue(Iterables.lastIndexOf(new String[] { "a", "b", "a" }, "c").isEmpty());
        assertEquals(2, Iterables.lastIndexOf(new String[] { null, "a", null }, null).get());
    }

    @Test
    public void testLastIndexOfCollection() {
        assertTrue(Iterables.lastIndexOf((List<String>) null, "a").isEmpty());
        assertTrue(Iterables.lastIndexOf(list(), "a").isEmpty());
        assertEquals(2, Iterables.lastIndexOf(list("a", "b", "a"), "a").get());
        assertEquals(1, Iterables.lastIndexOf(list("a", "b", "a"), "b").get());
        assertTrue(Iterables.lastIndexOf(list("a", "b", "a"), "c").isEmpty());
        assertEquals(2, Iterables.lastIndexOf(list(null, "a", null), null).get());
    }

    @Test
    public void testFindFirstOrLastArray() {
        Predicate<String> startsWithA = s -> s != null && s.startsWith("a");
        Predicate<String> endsWithC = s -> s != null && s.endsWith("c");

        assertTrue(Iterables.findFirstOrLast((String[]) null, startsWithA, endsWithC).isEmpty());
        assertTrue(Iterables.findFirstOrLast(new String[0], startsWithA, endsWithC).isEmpty());

        assertEquals("apple", Iterables.findFirstOrLast(new String[] { "apple", "banana", "avocado" }, startsWithA, endsWithC).get());
        assertEquals("avocado", Iterables.findFirstOrLast(new String[] { "banana", "avocado", "kiwi" }, startsWithA, endsWithC).get());
        assertEquals("cherry", Iterables.findFirstOrLast(new String[] { "banana", "kiwi", "cherry" }, startsWithA, s -> s.endsWith("y")).get());
        assertTrue(Iterables.findFirstOrLast(new String[] { "banana", "kiwi" }, startsWithA, endsWithC).isEmpty());
    }

    @Test
    public void testFindFirstOrLastCollection() {
        Predicate<String> startsWithA = s -> s != null && s.startsWith("a");
        Predicate<String> endsWithC = s -> s != null && s.endsWith("c");

        assertTrue(Iterables.findFirstOrLast((List<String>) null, startsWithA, endsWithC).isEmpty());
        assertTrue(Iterables.findFirstOrLast(list(), startsWithA, endsWithC).isEmpty());

        assertEquals("apple", Iterables.findFirstOrLast(list("apple", "banana", "avocado"), startsWithA, endsWithC).get());
        assertEquals("avocado", Iterables.findFirstOrLast(list("banana", "avocado", "kiwi"), startsWithA, endsWithC).get());
    }

    @Test
    public void testFindFirstOrLastIndexArray() {
        Predicate<String> startsWithA = s -> s != null && s.startsWith("a");
        Predicate<String> endsWithC = s -> s != null && s.endsWith("c");

        assertTrue(Iterables.findFirstOrLastIndex((String[]) null, startsWithA, endsWithC).isEmpty());
        assertTrue(Iterables.findFirstOrLastIndex(new String[0], startsWithA, endsWithC).isEmpty());

        assertEquals(0, Iterables.findFirstOrLastIndex(new String[] { "apple", "banana", "avocado" }, startsWithA, endsWithC).get());
        assertEquals(1, Iterables.findFirstOrLastIndex(new String[] { "banana", "avocado", "kiwi" }, startsWithA, endsWithC).get());
    }

    @Test
    public void testFindFirstOrLastIndexCollection() {
        Predicate<String> startsWithA = s -> s != null && s.startsWith("a");
        Predicate<String> endsWithC = s -> s != null && s.endsWith("c");

        assertTrue(Iterables.findFirstOrLastIndex((List<String>) null, startsWithA, endsWithC).isEmpty());
        assertTrue(Iterables.findFirstOrLastIndex(list(), startsWithA, endsWithC).isEmpty());

        assertEquals(0, Iterables.findFirstOrLastIndex(list("apple", "banana", "avocado"), startsWithA, endsWithC).get());
        assertEquals(1, Iterables.findFirstOrLastIndex(list("banana", "avocado", "kiwi"), startsWithA, endsWithC).get());
    }

    @Test
    public void testFindFirstAndLastArray() {
        Predicate<String> isBanana = "banana"::equals;
        Pair<Nullable<String>, Nullable<String>> result = Iterables.findFirstAndLast(new String[] { "apple", "banana", "cherry", "banana", "date" }, isBanana);
        assertEquals("banana", result.left().get());
        assertEquals("banana", result.right().get());

        result = Iterables.findFirstAndLast(new String[] { "apple", "cherry", "date" }, isBanana);
        assertTrue(result.left().isEmpty());
        assertTrue(result.right().isEmpty());

        result = Iterables.findFirstAndLast(new String[0], isBanana);
        assertTrue(result.left().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testFindFirstAndLastArrayDifferentPredicates() {
        Predicate<String> startsA = s -> s.startsWith("a");
        Predicate<String> endsO = s -> s.endsWith("o");
        Pair<Nullable<String>, Nullable<String>> result = Iterables.findFirstAndLast(new String[] { "apple", "avocado", "banana", "mango", "orange" }, startsA,
                endsO);
        assertEquals("apple", result.left().get());
        assertEquals("mango", result.right().get());

        result = Iterables.findFirstAndLast(new String[] { "apple", "avocado" }, s -> s.startsWith("x"), endsO);
        assertTrue(result.left().isEmpty());
        assertEquals("avocado", result.right().get());
    }

    @Test
    public void testFindFirstAndLastCollection() {
        Predicate<String> isBanana = "banana"::equals;
        Pair<Nullable<String>, Nullable<String>> result = Iterables.findFirstAndLast(list("apple", "banana", "cherry", "banana", "date"), isBanana);
        assertEquals("banana", result.left().get());
        assertEquals("banana", result.right().get());

        result = Iterables.findFirstAndLast(list(), isBanana);
        assertTrue(result.left().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testFindFirstAndLastIndexCollection() {
        Predicate<Integer> isEven = i -> i % 2 == 0;
        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(list(1, 2, 3, 4, 2, 5), isEven);
        assertEquals(1, result.left().get());
        assertEquals(4, result.right().get());

        result = Iterables.findFirstAndLastIndex(list(1, 3, 5), isEven);
        assertTrue(result.left().isEmpty());
        assertTrue(result.right().isEmpty());

        result = Iterables.findFirstAndLastIndex(list(), isEven);
        assertTrue(result.left().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testFindFirstAndLastIndexArray() {
        Predicate<Integer> isEven = i -> i % 2 == 0;
        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(new Integer[] { 1, 2, 3, 4, 2, 5 }, isEven);
        assertEquals(1, result.left().get());
        assertEquals(4, result.right().get());
    }

    @Test
    public void testFillArraySupplier() {
        String[] arr = new String[3];
        Supplier<String> supplier = () -> "test";
        Iterables.fill(arr, supplier);
        assertArrayEquals(new String[] { "test", "test", "test" }, arr);

        Iterables.fill((String[]) null, supplier);
        Iterables.fill(new String[0], supplier);
    }

    @Test
    public void testFillArrayRangeSupplier() {
        String[] arr = { "a", "b", "c", "d" };
        Supplier<String> supplier = () -> "x";
        Iterables.fill(arr, 1, 3, supplier);
        assertArrayEquals(new String[] { "a", "x", "x", "d" }, arr);

        Iterables.fill(arr, 1, 1, supplier);
        assertArrayEquals(new String[] { "a", "x", "x", "d" }, arr);

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(arr, 0, 5, supplier));
    }

    @Test
    public void testFillListSupplier() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Supplier<String> supplier = () -> "test";
        Iterables.fill(list, supplier);
        assertEquals(list("test", "test", "test"), list);

        List<String> emptyList = new ArrayList<>();
        Iterables.fill(emptyList, supplier);
        assertTrue(emptyList.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> Iterables.fill((List<String>) null, supplier));
    }

    @Test
    public void testFillListRangeSupplier() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        Supplier<String> supplier = () -> "x";
        Iterables.fill(list, 1, 3, supplier);
        assertEquals(list("a", "x", "x", "d"), list);

        List<String> shortList = new ArrayList<>(Arrays.asList("a"));
        Iterables.fill(shortList, 0, 3, supplier);
        assertEquals(list("x", "x", "x"), shortList);

        List<String> shortList2 = new ArrayList<>(Arrays.asList("a"));
        Iterables.fill(shortList2, 2, 4, supplier);
        assertEquals(list("a", null, "x", "x"), shortList2);
        Iterables.fill(list, 0, 5, supplier);
        assertEquals(list("x", "x", "x", "x", "x"), list);

        assertThrows(IllegalArgumentException.class, () -> Iterables.fill((List<String>) null, 0, 1, supplier));
    }

    @Test
    public void testCopyListList() {
        List<String> src = list("a", "b");
        List<String> dest = new ArrayList<>(Arrays.asList("x", "y", "z"));
        Iterables.copy(src, dest);
        assertEquals(list("a", "b", "z"), dest);

        List<String> emptySrc = list();
        List<String> dest2 = new ArrayList<>(Arrays.asList("x", "y"));
        Iterables.copy(emptySrc, dest2);
        assertEquals(list("x", "y"), dest2);

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.copy(list("a", "b", "c"), list("x", "y")));
    }

    @Test
    public void testCopyListRange() {
        List<String> src = list("s1", "s2", "s3", "s4");
        List<String> dest = new ArrayList<>(Arrays.asList("d1", "d2", "d3", "d4", "d5"));

        Iterables.copy(src, 1, dest, 2, 2);
        assertEquals(list("d1", "d2", "s2", "s3", "d5"), dest);

        List<String> destNonRandom = new LinkedList<>(Arrays.asList("d1", "d2", "d3", "d4", "d5"));
        Iterables.copy(src, 1, destNonRandom, 2, 2);
        assertEquals(list("d1", "d2", "s2", "s3", "d5"), destNonRandom);

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.copy(src, 0, dest, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.copy(src, 3, dest, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.copy(src, 0, dest, 4, 2));
    }

    @Test
    public void testReverse() {
        List<String> original = list("a", "b", "c");
        List<String> reversed = Iterables.reverse(original);
        assertEquals(list("c", "b", "a"), reversed);
        assertEquals(3, reversed.size());
        assertEquals("c", reversed.get(0));

        reversed.set(0, "x");
        assertEquals(list("x", "b", "a"), reversed);
        assertEquals(list("a", "b", "x"), original);

        reversed.add("y");
        assertEquals(list("y", "a", "b", "x"), original);
        assertEquals(list("x", "b", "a", "y"), reversed);

        reversed.remove(0);
        assertEquals(list("y", "a", "b"), original);
        assertEquals(list("b", "a", "y"), reversed);

        List<String> empty = list();
        List<String> reversedEmpty = Iterables.reverse(empty);
        assertTrue(reversedEmpty.isEmpty());

        List<String> original2 = list("1", "2");
        List<String> reversed2 = Iterables.reverse(original2);
        List<String> rereversed2 = Iterables.reverse(reversed2);
        assertEquals(original2, rereversed2);
        assertSame(original2, rereversed2);
    }

    @Test
    public void testReverseListIterator() {
        List<String> original = list("a", "b", "c", "d");
        List<String> reversed = Iterables.reverse(original);
        ListIterator<String> iter = reversed.listIterator();

        assertTrue(iter.hasNext());
        assertEquals("d", iter.next());
        assertEquals(0, iter.previousIndex());
        assertEquals(1, iter.nextIndex());

        assertTrue(iter.hasNext());
        assertEquals("c", iter.next());

        assertTrue(iter.hasPrevious());
        assertEquals("c", iter.previous());

        iter.set("x");
        assertEquals(list("d", "x", "b", "a"), reversed);
        assertEquals(list("a", "b", "x", "d"), original);

        iter.add("y");
        assertEquals(list("a", "b", "x", "y", "d"), original);
        assertEquals(list("d", "y", "x", "b", "a"), reversed);

    }

    @Test
    public void testUnion() {
        Set<Integer> s1 = set(1, 2, 3);
        Set<Integer> s2 = set(3, 4, 5);
        Iterables.SetView<Integer> union = Iterables.union(s1, s2);
        assertEquals(set(1, 2, 3, 4, 5), union);
        assertEquals(5, union.size());
        assertTrue(union.contains(1));
        assertFalse(union.contains(6));

        Iterables.SetView<Integer> unionEmpty1 = Iterables.union(set(), s2);
        assertEquals(s2, unionEmpty1);

        Iterables.SetView<Integer> unionEmpty2 = Iterables.union(s1, set());
        assertEquals(s1, unionEmpty2);

        Iterables.SetView<Integer> unionBothEmpty = Iterables.union(set(), set());
        assertTrue(unionBothEmpty.isEmpty());

        Set<Integer> target = new HashSet<>();
        union.copyInto(target);
        assertEquals(set(1, 2, 3, 4, 5), target);
    }

    @Test
    public void testIntersection() {
        Set<Integer> s1 = set(1, 2, 3);
        Set<Integer> s2 = set(3, 4, 5, 2);
        Iterables.SetView<Integer> intersection = Iterables.intersection(s1, s2);
        assertEquals(set(2, 3), intersection);
        assertEquals(2, intersection.size());
        assertTrue(intersection.contains(2));
        assertFalse(intersection.contains(1));

        Iterables.SetView<Integer> intersectionEmpty1 = Iterables.intersection(set(), s2);
        assertTrue(intersectionEmpty1.isEmpty());

        Iterables.SetView<Integer> intersectionNoCommon = Iterables.intersection(s1, set(4, 5, 6));
        assertTrue(intersectionNoCommon.isEmpty());
        assertTrue(intersectionNoCommon.containsAll(set()));
        assertFalse(intersectionNoCommon.containsAll(set(1)));
    }

    @Test
    public void testDifference() {
        Set<Integer> s1 = set(1, 2, 3, 4);
        Set<Integer> s2 = set(3, 4, 5, 6);
        Iterables.SetView<Integer> difference = Iterables.difference(s1, s2);
        assertEquals(set(1, 2), difference);
        assertEquals(2, difference.size());
        assertTrue(difference.contains(1));
        assertFalse(difference.contains(3));

        Iterables.SetView<Integer> diffEmpty2 = Iterables.difference(s1, set());
        assertEquals(s1, diffEmpty2);

        Iterables.SetView<Integer> diffEmpty1 = Iterables.difference(set(), s2);
        assertTrue(diffEmpty1.isEmpty());

        Iterables.SetView<Integer> diffAllOverlap = Iterables.difference(s1, set(1, 2, 3, 4, 5));
        assertTrue(diffAllOverlap.isEmpty());
    }

    @Test
    public void testSymmetricDifference() {
        Set<Integer> s1 = set(1, 2, 3, 4);
        Set<Integer> s2 = set(3, 4, 5, 6);
        Iterables.SetView<Integer> symDiff = Iterables.symmetricDifference(s1, s2);
        assertEquals(set(1, 2, 5, 6), symDiff);
        assertEquals(4, symDiff.size());
        assertTrue(symDiff.contains(1) && symDiff.contains(5));
        assertFalse(symDiff.contains(3));

        Iterables.SetView<Integer> symDiffEmpty1 = Iterables.symmetricDifference(set(), s2);
        assertEquals(s2, symDiffEmpty1);

        Iterables.SetView<Integer> symDiffEmpty2 = Iterables.symmetricDifference(s1, set());
        assertEquals(s1, symDiffEmpty2);

        Iterables.SetView<Integer> symDiffIdentical = Iterables.symmetricDifference(s1, new HashSet<>(s1));
        assertTrue(symDiffIdentical.isEmpty());
    }

    @Test
    public void testSubSetNavigableSet() {
        NavigableSet<Integer> ns = navigableSet(1, 2, 3, 4, 5, 6, 7);

        Range<Integer> r1 = Range.closed(3, 5);
        NavigableSet<Integer> sub1 = Iterables.subSet(ns, r1);
        assertEquals(navigableSet(3, 4, 5), sub1);

        Range<Integer> r2 = Range.open(2, 6);
        NavigableSet<Integer> sub2 = Iterables.subSet(ns, r2);
        assertEquals(navigableSet(3, 4, 5), sub2);

        Range<Integer> r3 = Range.closedOpen(3, 6);
        NavigableSet<Integer> sub3 = Iterables.subSet(ns, r3);
        assertEquals(navigableSet(3, 4, 5), sub3);

        Range<Integer> r4 = Range.openClosed(2, 5);
        NavigableSet<Integer> sub4 = Iterables.subSet(ns, r4);
        assertEquals(navigableSet(3, 4, 5), sub4);

        Range<Integer> r5 = Range.closed(8, 10);
        NavigableSet<Integer> sub5 = Iterables.subSet(ns, r5);
        assertTrue(sub5.isEmpty());

        assertTrue(Iterables.subSet(navigableSet(), r1).isEmpty());

        Range<Integer> r6 = Range.closedOpen(Integer.MIN_VALUE, 0);
        assertTrue(Iterables.subSet(ns, r6).isEmpty());

        Range<Integer> r7 = Range.closed(3, Integer.MAX_VALUE);
        Range<Integer> r7_equiv = Range.closed(3, 7);
        assertEquals(navigableSet(3, 4, 5, 6, 7), Iterables.subSet(ns, r7_equiv));

        NavigableSet<Integer> reverseNs = new TreeSet<>(Comparator.naturalOrder());
        reverseNs.addAll(Arrays.asList(1, 2, 3, 4, 5));
        Range<Integer> r8 = Range.closed(2, 4);
        NavigableSet<Integer> sub8 = Iterables.subSet(reverseNs, r8);
        final Supplier<TreeSet<Integer>> treeSetSupplier = () -> new TreeSet<>(Comparator.reverseOrder());
        assertEquals(navigableSet(4, 3, 2), sub8.stream().collect(Collectors.toCollection(treeSetSupplier)));

    }

    @Test
    public void testPowerSet() {
        Set<Integer> s = set(1, 2);
        Set<Set<Integer>> ps = Iterables.powerSet(s);
        assertEquals(4, ps.size());
        assertTrue(ps.contains(set()));
        assertTrue(ps.contains(set(1)));
        assertTrue(ps.contains(set(2)));
        assertTrue(ps.contains(set(1, 2)));

        Set<Set<Integer>> psEmpty = Iterables.powerSet(set());
        assertEquals(1, psEmpty.size());
        assertTrue(psEmpty.contains(set()));

        assertThrows(NoSuchElementException.class, () -> Iterables.powerSet(set(1)).iterator().next().iterator().next());

        Set<String> sStr = set("a", "b");
        Set<Set<String>> psStr = Iterables.powerSet(sStr);
        assertTrue(psStr.contains(new HashSet<>(Arrays.asList("a"))));

    }

    @Test
    public void testRollup() {
        List<String> c = list("a", "b", "c");
        List<List<String>> rollup = Iterables.rollup(c);

        assertEquals(4, rollup.size());
        assertEquals(list(), rollup.get(0));
        assertEquals(list("a"), rollup.get(1));
        assertEquals(list("a", "b"), rollup.get(2));
        assertEquals(list("a", "b", "c"), rollup.get(3));

        List<List<Integer>> rollupEmpty = Iterables.rollup(list());
        assertEquals(1, rollupEmpty.size());
        assertEquals(list(), rollupEmpty.get(0));

        List<List<Integer>> rollupNull = Iterables.rollup(null);
        assertEquals(1, rollupNull.size());
        assertEquals(list(), rollupNull.get(0));
    }

    @Test
    public void testPermutations() {
        Collection<Integer> elements = list(1, 2);
        Collection<List<Integer>> perms = Iterables.permutations(elements);
        assertEquals(2, perms.size());
        assertTrue(perms.contains(list(1, 2)));
        assertTrue(perms.contains(list(2, 1)));

        Collection<Integer> elements2 = list(1, 2, 3);
        Collection<List<Integer>> perms2 = Iterables.permutations(elements2);
        assertEquals(6, perms2.size());
        assertTrue(perms2.contains(list(1, 2, 3)));
        assertTrue(perms2.contains(list(1, 3, 2)));

        Collection<Integer> emptyElements = list();
        Collection<List<Integer>> permsEmpty = Iterables.permutations(emptyElements);
        assertEquals(1, permsEmpty.size());
        assertTrue(permsEmpty.contains(list()));

        Collection<Integer> elementsDup = list(1, 1, 2);
        Collection<List<Integer>> permsDup = Iterables.permutations(elementsDup);
        assertEquals(6, permsDup.size());
        long count112 = permsDup.stream().filter(p -> p.equals(list(1, 1, 2))).count();
        long count121 = permsDup.stream().filter(p -> p.equals(list(1, 2, 1))).count();
        long count211 = permsDup.stream().filter(p -> p.equals(list(2, 1, 1))).count();
        assertEquals(2, count112);
        assertEquals(2, count121);
        assertEquals(2, count211);

        assertThrows(IllegalArgumentException.class, () -> Iterables.permutations(null));
    }

    @Test
    public void testOrderedPermutationsComparable() {
        Collection<Integer> elements = list(2, 1);
        Collection<List<Integer>> op = Iterables.orderedPermutations(elements);
        assertEquals(2, op.size());
        Iterator<List<Integer>> it = op.iterator();
        assertEquals(list(1, 2), it.next());
        assertEquals(list(2, 1), it.next());

        Collection<Integer> elementsDup = list(1, 2, 1);
        Collection<List<Integer>> opDup = Iterables.orderedPermutations(elementsDup);
        assertEquals(3, opDup.size());
        Iterator<List<Integer>> itDup = opDup.iterator();
        assertEquals(list(1, 1, 2), itDup.next());
        assertEquals(list(1, 2, 1), itDup.next());
        assertEquals(list(2, 1, 1), itDup.next());

        assertThrows(IllegalArgumentException.class, () -> Iterables.orderedPermutations(null));
    }

    @Test
    public void testOrderedPermutationsComparator() {
        Collection<String> elements = list("c", "A", "b");
        Comparator<String> ciComparator = String.CASE_INSENSITIVE_ORDER;
        Collection<List<String>> op = Iterables.orderedPermutations(elements, ciComparator);
        assertEquals(6, op.size());
        Iterator<List<String>> it = op.iterator();
        assertEquals(list("A", "b", "c"), it.next());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCartesianProductVarArgs() {
        List<List<Integer>> cp = Iterables.cartesianProduct(list(1, 2), list(3, 4));
        assertEquals(4, cp.size());
        assertTrue(cp.contains(list(1, 3)));
        assertTrue(cp.contains(list(1, 4)));
        assertTrue(cp.contains(list(2, 3)));
        assertTrue(cp.contains(list(2, 4)));
        assertEquals(list(1, 3), cp.get(0));
        assertEquals(list(1, 4), cp.get(1));
        assertEquals(list(2, 3), cp.get(2));
        assertEquals(list(2, 4), cp.get(3));

        List<List<Object>> cpMixed = Iterables.cartesianProduct(list(1, 2), list("a"), list(true, false));
        assertEquals(4, cpMixed.size());
        assertTrue(cpMixed.contains(list(1, "a", true)));
        assertTrue(cpMixed.contains(list(2, "a", false)));
        assertEquals(list(1, "a", true), cpMixed.get(0));

        List<List<Integer>> cpEmptyList = Iterables.cartesianProduct(list(1, 2), list());
        assertTrue(cpEmptyList.isEmpty());

        List<List<Integer>> cpNoLists = Iterables.cartesianProduct();
        assertEquals(1, cpNoLists.size());
        assertTrue(cpNoLists.get(0).isEmpty());
    }

    @Test
    public void testCartesianProductCollectionOfCollections() {
        List<Collection<Integer>> collections = new ArrayList<>();
        collections.add(list(1, 2));
        collections.add(list(3, 4));
        List<List<Integer>> cp = Iterables.cartesianProduct(collections);
        assertEquals(4, cp.size());
        assertEquals(list(1, 3), cp.get(0));

        List<Collection<?>> collectionsMixed = new ArrayList<>();
        collectionsMixed.add(list(1, 2));
        collectionsMixed.add(list("a"));
        List<List<Object>> cpMixed = Iterables.cartesianProduct(collectionsMixed);
        assertEquals(2, cpMixed.size());
        assertEquals(list(1, "a"), cpMixed.get(0));

    }
}
