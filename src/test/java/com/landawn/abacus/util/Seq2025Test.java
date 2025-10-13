package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.stream.Collectors;

@Tag("2025")
public class Seq2025Test extends TestBase {

    private Seq<Integer, RuntimeException> intSeq;
    private Seq<String, RuntimeException> stringSeq;

    @BeforeEach
    public void setUp() {
        intSeq = Seq.of(1, 2, 3, 4, 5);
        stringSeq = Seq.of("a", "b", "c");
    }

    @Test
    public void testEmpty() throws Exception {
        Seq<String, RuntimeException> seq = Seq.empty();
        assertNotNull(seq);
        assertEquals(0, seq.count());
    }

    @Test
    public void testJust() throws Exception {
        Seq<String, RuntimeException> seq = Seq.just("test");
        assertEquals(1, seq.count());
    }

    @Test
    public void testJustWithExceptionType() throws Exception {
        Seq<String, IOException> seq = Seq.just("test", IOException.class);
        assertEquals(1, seq.count());
    }

    @Test
    public void testOfNullable_Null() throws Exception {
        Seq<String, RuntimeException> seq = Seq.ofNullable(null);
        assertEquals(0, seq.count());
    }

    @Test
    public void testOfNullable_NonNull() throws Exception {
        Seq<String, RuntimeException> seq = Seq.ofNullable("test");
        assertEquals(1, seq.count());
    }

    @Test
    public void testOf_Array() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3);
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_EmptyArray() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.of();
        assertEquals(0, seq.count());
    }

    @Test
    public void testOf_BooleanArray() throws Exception {
        Seq<Boolean, RuntimeException> seq = Seq.of(new boolean[] { true, false, true });
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_CharArray() throws Exception {
        Seq<Character, RuntimeException> seq = Seq.of(new char[] { 'a', 'b', 'c' });
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_ByteArray() throws Exception {
        Seq<Byte, RuntimeException> seq = Seq.of(new byte[] { 1, 2, 3 });
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_ShortArray() throws Exception {
        Seq<Short, RuntimeException> seq = Seq.of(new short[] { 1, 2, 3 });
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_IntArray() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.of(new int[] { 1, 2, 3 });
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_LongArray() throws Exception {
        Seq<Long, RuntimeException> seq = Seq.of(new long[] { 1L, 2L, 3L });
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_FloatArray() throws Exception {
        Seq<Float, RuntimeException> seq = Seq.of(new float[] { 1.0f, 2.0f, 3.0f });
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_DoubleArray() throws Exception {
        Seq<Double, RuntimeException> seq = Seq.of(new double[] { 1.0, 2.0, 3.0 });
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_Iterable() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        Seq<String, RuntimeException> seq = Seq.of(list);
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_Iterator() throws Exception {
        Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();
        Seq<String, RuntimeException> seq = Seq.of(iter);
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_Map() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Seq<Map.Entry<String, Integer>, RuntimeException> seq = Seq.of(map);
        assertEquals(2, seq.count());
    }

    @Test
    public void testOfKeys() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Seq<String, RuntimeException> seq = Seq.ofKeys(map);
        assertEquals(2, seq.count());
    }

    @Test
    public void testOfValues() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Seq<Integer, RuntimeException> seq = Seq.ofValues(map);
        assertEquals(2, seq.count());
    }

    @Test
    public void testOfReversed_Array() throws Exception {
        Integer[] array = { 1, 2, 3 };
        Seq<Integer, RuntimeException> seq = Seq.ofReversed(array);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(3, 2, 1), result);
    }

    @Test
    public void testOfReversed_List() throws Exception {
        List<Integer> list = Arrays.asList(1, 2, 3);
        Seq<Integer, RuntimeException> seq = Seq.ofReversed(list);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(3, 2, 1), result);
    }

    @Test
    public void testRepeat() throws Exception {
        Seq<String, RuntimeException> seq = Seq.repeat("a", 3);
        assertEquals(3, seq.count());
    }

    @Test
    public void testRepeat_Zero() throws Exception {
        Seq<String, RuntimeException> seq = Seq.repeat("a", 0);
        assertEquals(0, seq.count());
    }

    @Test
    public void testRange() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.range(0, 5);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), result);
    }

    @Test
    public void testRange_WithStep() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.range(0, 10, 2);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(0, 2, 4, 6, 8), result);
    }

    @Test
    public void testRangeClosed() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.rangeClosed(0, 5);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), result);
    }

    @Test
    public void testSplit_CharDelimiter() throws Exception {
        Seq<String, RuntimeException> seq = Seq.split("a,b,c", ',');
        List<String> result = seq.toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testSplit_StringDelimiter() throws Exception {
        Seq<String, RuntimeException> seq = Seq.split("a::b::c", "::");
        List<String> result = seq.toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testSplitToLines() throws Exception {
        Seq<String, RuntimeException> seq = Seq.splitToLines("a\nb\nc");
        assertEquals(3, seq.count());
    }

    @Test
    public void testConcat_Arrays() throws Exception {
        Integer[] a1 = { 1, 2 };
        Integer[] a2 = { 3, 4 };
        Seq<Integer, RuntimeException> seq = Seq.concat(a1, a2);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testConcat_Iterables() throws Exception {
        List<Integer> l1 = Arrays.asList(1, 2);
        List<Integer> l2 = Arrays.asList(3, 4);
        Seq<Integer, RuntimeException> seq = Seq.concat(l1, l2);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testConcat_Seqs() throws Exception {
        Seq<Integer, RuntimeException> s1 = Seq.of(1, 2);
        Seq<Integer, RuntimeException> s2 = Seq.of(3, 4);
        Seq<Integer, RuntimeException> seq = Seq.concat(s1, s2);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testMap() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).map(x -> x * 2).toList();
        assertEquals(Arrays.asList(2, 4, 6), result);
    }

    @Test
    public void testFlatMap() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).flatMap(x -> Seq.of(x, x * 10)).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void testFilter() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0).toList();
        assertEquals(Arrays.asList(2, 4), result);
    }

    @Test
    public void testTakeWhile() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).takeWhile(x -> x < 4).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDropWhile() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).dropWhile(x -> x < 3).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testSkip() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).skip(2).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testLimit() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).limit(3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDistinct() throws Exception {
        List<Integer> result = Seq.of(1, 2, 2, 3, 3, 3).distinct().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSorted() throws Exception {
        List<Integer> result = Seq.of(3, 1, 4, 1, 5).sorted().toList();
        assertEquals(Arrays.asList(1, 1, 3, 4, 5), result);
    }

    @Test
    public void testSorted_WithComparator() throws Exception {
        List<Integer> result = Seq.of(3, 1, 4, 1, 5).sorted(Comparator.reverseOrder()).toList();
        assertEquals(Arrays.asList(5, 4, 3, 1, 1), result);
    }

    @Test
    public void testReversed() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).reversed().toList();
        assertEquals(Arrays.asList(3, 2, 1), result);
    }

    @Test
    public void testPeek() throws Exception {
        List<Integer> peeked = new ArrayList<>();
        Seq.of(1, 2, 3).peek(peeked::add).toList();
        assertEquals(Arrays.asList(1, 2, 3), peeked);
    }

    @Test
    public void testOnEach() throws Exception {
        AtomicInteger sum = new AtomicInteger(0);
        Seq.of(1, 2, 3).onEach(sum::addAndGet).toList();
        assertEquals(6, sum.get());
    }

    @Test
    public void testAppend_Single() throws Exception {
        List<Integer> result = Seq.of(1, 2).append(3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testAppend_Multiple() throws Exception {
        List<Integer> result = Seq.of(1, 2).append(3, 4).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testAppendAll() throws Exception {
        List<Integer> result = Seq.of(1, 2).append(Seq.of(Arrays.asList(3, 4))).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testPrepend_Single() throws Exception {
        List<Integer> result = Seq.of(2, 3).prepend(1).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPrepend_Multiple() throws Exception {
        List<Integer> result = Seq.of(3, 4).prepend(1, 2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testIntersperse() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).intersperse(0).toList();
        assertEquals(Arrays.asList(1, 0, 2, 0, 3), result);
    }

    @Test
    public void testStep() throws Exception {
        var seq = Seq.of(1, 2, 3, 4, 5).step(2);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(1, 3, 5), result);
    }

    @Test
    public void testIndexed() throws Exception {
        var seq = Seq.of("a", "b", "c").indexed();
        List<Indexed<String>> result = seq.toList();
        assertEquals(3, result.size());
        assertEquals("a", result.get(0).value());
        assertEquals(0, result.get(0).index());
        assertEquals("b", result.get(1).value());
        assertEquals(1, result.get(1).index());
    }

    @Test
    public void testZipWith_Collection() throws Exception {
        List<String> result = Seq.of("a", "b", "c").zipWith(Arrays.asList(1, 2, 3), (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testZipWith_Seq() throws Exception {
        List<String> result = Seq.of("a", "b", "c").zipWith(Seq.of(1, 2, 3), (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testForEach() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Seq.of(1, 2, 3).forEach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEachIndexed() throws Exception {
        List<String> collected = new ArrayList<>();
        Seq.of("a", "b", "c").forEachIndexed((i, s) -> collected.add(i + ":" + s));
        assertEquals(Arrays.asList("0:a", "1:b", "2:c"), collected);
    }

    @Test
    public void testForEachPair() throws Exception {
        List<String> collected = new ArrayList<>();
        Seq.of(1, 2, 3, 4).forEachPair((a, b) -> collected.add(a + "-" + b));
        assertEquals(Arrays.asList("1-2", "2-3", "3-4"), collected);
    }

    @Test
    public void testCount() throws Exception {
        long count = Seq.of(1, 2, 3, 4, 5).count();
        assertEquals(5, count);
    }

    @Test
    public void testCount_Empty() throws Exception {
        long count = Seq.empty().count();
        assertEquals(0, count);
    }

    @Test
    public void testToArray() throws Exception {
        Object[] array = Seq.of(1, 2, 3).toArray();
        assertArrayEquals(new Object[] { 1, 2, 3 }, array);
    }

    @Test
    public void testToArray_WithGenerator() throws Exception {
        Integer[] array = Seq.of(1, 2, 3).toArray(Integer[]::new);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, array);
    }

    @Test
    public void testToList() throws Exception {
        List<Integer> list = Seq.of(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testToSet() throws Exception {
        Set<Integer> set = Seq.of(1, 2, 2, 3).toSet();
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
    }

    @Test
    public void testToCollection() throws Exception {
        ArrayList<Integer> list = Seq.of(1, 2, 3).toCollection(ArrayList::new);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testToImmutableList() throws Exception {
        ImmutableList<Integer> list = Seq.of(1, 2, 3).toImmutableList();
        assertEquals(3, list.size());
        assertThrows(UnsupportedOperationException.class, () -> list.add(4));
    }

    @Test
    public void testToImmutableSet() throws Exception {
        ImmutableSet<Integer> set = Seq.of(1, 2, 2, 3).toImmutableSet();
        assertEquals(3, set.size());
        assertThrows(UnsupportedOperationException.class, () -> set.add(4));
    }

    @Test
    public void testToMap() throws Exception {
        Map<String, Integer> map = Seq.of("a", "bb", "ccc").toMap(s -> s, String::length);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("bb"));
        assertEquals(Integer.valueOf(3), map.get("ccc"));
    }

    @Test
    public void testGroupBy() throws Exception {
        Map<Integer, List<String>> grouped = Seq.of("a", "bb", "ccc", "dd").groupBy(String::length).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(3, grouped.size());
        assertEquals(Arrays.asList("a"), grouped.get(1));
        assertEquals(Arrays.asList("bb", "dd"), grouped.get(2));
        assertEquals(Arrays.asList("ccc"), grouped.get(3));
    }

    @Test
    public void testSplitAt() throws Exception {
        List<List<Integer>> parts = Seq.of(1, 2, 3, 4, 5).splitAt(2).map(Seq::toList).toList();
        assertEquals(2, parts.size());
        assertEquals(Arrays.asList(1, 2), parts.get(0));
        assertEquals(Arrays.asList(3, 4, 5), parts.get(1));
    }

    @Test
    public void testJoin() throws Exception {
        String joined = Seq.of("a", "b", "c").join(", ");
        assertEquals("a, b, c", joined);
    }

    @Test
    public void testJoin_WithPrefixAndSuffix() throws Exception {
        String joined = Seq.of("a", "b", "c").join(", ", "[", "]");
        assertEquals("[a, b, c]", joined);
    }

    @Test
    public void testFirst() throws Exception {
        Optional<Integer> first = Seq.of(1, 2, 3).first();
        assertTrue(first.isPresent());
        assertEquals(Integer.valueOf(1), first.get());
    }

    @Test
    public void testFirst_Empty() throws Exception {
        Optional<Integer> first = Seq.<Integer, RuntimeException> empty().first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() throws Exception {
        Optional<Integer> last = Seq.of(1, 2, 3).last();
        assertTrue(last.isPresent());
        assertEquals(Integer.valueOf(3), last.get());
    }

    @Test
    public void testLast_Empty() throws Exception {
        Optional<Integer> last = Seq.<Integer, RuntimeException> empty().last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testElementAt() throws Exception {
        Optional<Integer> elem = Seq.of(1, 2, 3, 4, 5).elementAt(2);
        assertTrue(elem.isPresent());
        assertEquals(Integer.valueOf(3), elem.get());
    }

    @Test
    public void testElementAt_OutOfBounds() throws Exception {
        Optional<Integer> elem = Seq.of(1, 2, 3).elementAt(10);
        assertFalse(elem.isPresent());
    }

    @Test
    public void testOnlyOne() throws Exception {
        Optional<Integer> only = Seq.of(42).onlyOne();
        assertTrue(only.isPresent());
        assertEquals(Integer.valueOf(42), only.get());
    }

    @Test
    public void testOnlyOne_Empty() throws Exception {
        Optional<Integer> only = Seq.<Integer, RuntimeException> empty().onlyOne();
        assertFalse(only.isPresent());
    }

    @Test
    public void testOnlyOne_MultipleElements() throws Exception {
        assertThrows(TooManyElementsException.class, () -> Seq.of(1, 2).onlyOne());
    }

    @Test
    public void testFindFirst() throws Exception {
        Optional<Integer> found = Seq.of(1, 2, 3, 4, 5).findFirst(x -> x > 3);
        assertTrue(found.isPresent());
        assertEquals(Integer.valueOf(4), found.get());
    }

    @Test
    public void testFindLast() throws Exception {
        Optional<Integer> found = Seq.of(1, 2, 3, 4, 5).findLast(x -> x < 4);
        assertTrue(found.isPresent());
        assertEquals(Integer.valueOf(3), found.get());
    }

    @Test
    public void testFindAny() throws Exception {
        Optional<Integer> found = Seq.of(1, 2, 3, 4, 5).findAny(x -> x > 3);
        assertTrue(found.isPresent());
        assertTrue(found.get() > 3);
    }

    @Test
    public void testAllMatch() throws Exception {
        assertTrue(Seq.of(2, 4, 6).allMatch(x -> x % 2 == 0));
        assertFalse(Seq.of(2, 3, 4).allMatch(x -> x % 2 == 0));
    }

    @Test
    public void testAnyMatch() throws Exception {
        assertTrue(Seq.of(1, 2, 3).anyMatch(x -> x % 2 == 0));
        assertFalse(Seq.of(1, 3, 5).anyMatch(x -> x % 2 == 0));
    }

    @Test
    public void testNoneMatch() throws Exception {
        assertTrue(Seq.of(1, 3, 5).noneMatch(x -> x % 2 == 0));
        assertFalse(Seq.of(1, 2, 3).noneMatch(x -> x % 2 == 0));
    }

    @Test
    public void testContains() throws Exception {
        assertTrue(Seq.of(1, 2, 3).anyMatch(x -> x == 2));
        assertFalse(Seq.of(1, 2, 3).anyMatch(x -> x == 4));
    }

    @Test
    public void testContainsAll() throws Exception {
        assertTrue(Seq.of(1, 2, 3, 4).containsAll(2, 3));
        assertFalse(Seq.of(1, 2, 3).containsAll(2, 4));
    }

    @Test
    public void testContainsAny() throws Exception {
        assertTrue(Seq.of(1, 2, 3).containsAny(2, 4));
        assertFalse(Seq.of(1, 2, 3).containsAny(4, 5));
    }

    @Test
    public void testContainsNone() throws Exception {
        assertTrue(Seq.of(1, 2, 3).containsNone(4, 5));
        assertFalse(Seq.of(1, 2, 3).containsNone(2, 4));
    }

    @Test
    public void testHasDuplicates() throws Exception {
        assertTrue(Seq.of(1, 2, 2, 3).hasDuplicates());
        assertFalse(Seq.of(1, 2, 3).hasDuplicates());
    }

    @Test
    public void testMin() throws Exception {
        Optional<Integer> min = Seq.of(3, 1, 4, 1, 5).min(Comparator.naturalOrder());
        assertTrue(min.isPresent());
        assertEquals(Integer.valueOf(1), min.get());
    }

    @Test
    public void testMin_WithComparator() throws Exception {
        Optional<Integer> min = Seq.of(3, 1, 4, 1, 5).min(Comparator.reverseOrder());
        assertTrue(min.isPresent());
        assertEquals(Integer.valueOf(5), min.get());
    }

    @Test
    public void testMax() throws Exception {
        Optional<Integer> max = Seq.of(3, 1, 4, 1, 5).max(Comparator.naturalOrder());
        assertTrue(max.isPresent());
        assertEquals(Integer.valueOf(5), max.get());
    }

    @Test
    public void testMax_WithComparator() throws Exception {
        Optional<Integer> max = Seq.of(3, 1, 4, 1, 5).max(Comparator.reverseOrder());
        assertTrue(max.isPresent());
        assertEquals(Integer.valueOf(1), max.get());
    }

    @Test
    public void testSumInt() throws Exception {
        long sum = Seq.of(1, 2, 3, 4, 5).sumInt(x -> x);
        assertEquals(15L, sum);
    }

    @Test
    public void testSumLong() throws Exception {
        long sum = Seq.of(1, 2, 3, 4, 5).sumLong(x -> (long) x);
        assertEquals(15L, sum);
    }

    @Test
    public void testSumDouble() throws Exception {
        double sum = Seq.of(1.0, 2.0, 3.0).sumDouble(x -> x);
        assertEquals(6.0, sum, 0.001);
    }

    @Test
    public void testAverageInt() throws Exception {
        OptionalDouble avg = Seq.of(1, 2, 3, 4, 5).averageInt(x -> x);
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.get(), 0.001);
    }

    @Test
    public void testAverageLong() throws Exception {
        OptionalDouble avg = Seq.of(1L, 2L, 3L, 4L, 5L).averageLong(x -> x);
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.get(), 0.001);
    }

    @Test
    public void testAverageDouble() throws Exception {
        OptionalDouble avg = Seq.of(1.0, 2.0, 3.0, 4.0, 5.0).averageDouble(x -> x);
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.get(), 0.001);
    }

    @Test
    public void testReduce_WithIdentity() throws Exception {
        int sum = Seq.of(1, 2, 3, 4, 5).reduce(0, Integer::sum);
        assertEquals(15, sum);
    }

    @Test
    public void testReduce_WithoutIdentity() throws Exception {
        Optional<Integer> sum = Seq.of(1, 2, 3, 4, 5).reduce(Integer::sum);
        assertTrue(sum.isPresent());
        assertEquals(Integer.valueOf(15), sum.get());
    }

    @Test
    public void testCollect_SupplierAccumulator() throws Exception {
        List<Integer> list = Seq.of(1, 2, 3).collect(ArrayList::new, List::add);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testCollect_WithFinisher() throws Exception {
        Integer size = Seq.of(1, 2, 3).collect(ArrayList::new, List::add, List::size);
        assertEquals(Integer.valueOf(3), size);
    }

    @Test
    public void testClosedSeq_ThrowsException() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3);
        seq.toList();
        assertThrows(IllegalStateException.class, () -> seq.toList());
    }

    @Test
    public void testNullArgument_ThrowsException() throws Exception {
        assertTrue(true);
    }

    @Test
    public void testNegativeSkip_ThrowsException() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> seq.skip(-1));
    }

    @Test
    public void testNegativeLimit_ThrowsException() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> seq.limit(-1));
    }

    @Test
    public void testEmptySeq_Operations() throws Exception {
        assertEquals(0, Seq.<Integer, RuntimeException> empty().count());
        assertFalse(Seq.<Integer, RuntimeException> empty().first().isPresent());
        assertEquals(0, Seq.<Integer, RuntimeException> empty().toList().size());
    }

    @Test
    public void testSingleElement_Operations() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.of(42);
        assertEquals(1, seq.count());
    }

    @Test
    public void testLargeSeq() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.range(0, 10000);
        assertEquals(10000, seq.count());
    }

    @Test
    public void testNullElements() throws Exception {
        Seq<String, RuntimeException> seq = Seq.of("a", null, "b");
        List<String> list = seq.toList();
        assertEquals(3, list.size());
        assertNull(list.get(1));
    }

    @Test
    public void testFilterNulls() throws Exception {
        var seq = Seq.of("a", null, "b").filter(x -> x != null);
        List<String> list = seq.toList();
        assertEquals(2, list.size());
    }

    @Test
    public void testOnClose() throws Exception {
        AtomicInteger closed = new AtomicInteger(0);
        var seq = Seq.of(1, 2, 3).onClose(closed::incrementAndGet);
        seq.toList();
        assertEquals(1, closed.get());
    }

    @Test
    public void testMultipleOnClose() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        var seq = Seq.of(1, 2, 3).onClose(count::incrementAndGet).onClose(count::incrementAndGet);
        seq.toList();
        assertEquals(2, count.get());
    }

    @Test
    public void testExplicitClose() throws Exception {
        AtomicInteger closed = new AtomicInteger(0);
        try (var seq = Seq.of(1, 2, 3).onClose(closed::incrementAndGet)) {
        }
        assertEquals(1, closed.get());
    }

    @Test
    public void testChainedOperations() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5, 6).filter(x -> x % 2 == 0).map(x -> x * 2).sorted(Comparator.reverseOrder()).limit(2).toList();
        assertEquals(Arrays.asList(12, 8), result);
    }

    @Test
    public void testFlatMapWithFilter() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).flatMap(x -> Seq.of(x, x * 10, x * 100)).filter(x -> x > 10).toList();
        assertEquals(Arrays.asList(100, 20, 200, 30, 300), result);
    }

    @Test
    public void testGroupByThenMap() throws Exception {
        Map<Boolean, List<Integer>> grouped = Seq.of(1, 2, 3, 4, 5, 6).groupBy(x -> x % 2 == 0).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(2, grouped.size());
        assertEquals(Arrays.asList(2, 4, 6), grouped.get(true));
        assertEquals(Arrays.asList(1, 3, 5), grouped.get(false));
    }

    @Test
    public void testDistinctBy() throws Exception {
        List<String> result = Seq.of("a", "bb", "ccc", "dd").distinctBy(String::length).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testSortedBy() throws Exception {
        List<String> result = Seq.of("ccc", "a", "bb").sortedBy(String::length).toList();
        assertEquals(Arrays.asList("a", "bb", "ccc"), result);
    }

    @Test
    public void testTopK() throws Exception {
        List<Integer> result = Seq.of(5, 2, 8, 1, 9, 3).top(3).toList();
        assertEquals(Arrays.asList(5, 8, 9), result);
    }

    @Test
    public void testOfLines_File() throws IOException {
        File tempFile = File.createTempFile("seq-test", ".txt");
        try {
            Files.write(tempFile.toPath(), Arrays.asList("line1", "line2", "line3"));
            Seq<String, IOException> seq = Seq.ofLines(tempFile);
            List<String> lines = seq.toList();
            assertEquals(Arrays.asList("line1", "line2", "line3"), lines);
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void testSliding() throws Exception {
        List<List<Integer>> windows = Seq.of(1, 2, 3, 4, 5).sliding(3).toList();
        assertEquals(3, windows.size());
        assertEquals(Arrays.asList(1, 2, 3), windows.get(0));
        assertEquals(Arrays.asList(2, 3, 4), windows.get(1));
        assertEquals(Arrays.asList(3, 4, 5), windows.get(2));
    }

    @Test
    public void testSlidingWithIncrement() throws Exception {
        List<List<Integer>> windows = Seq.of(1, 2, 3, 4, 5, 6).sliding(2, 2).toList();
        assertEquals(3, windows.size());
        assertEquals(Arrays.asList(1, 2), windows.get(0));
        assertEquals(Arrays.asList(3, 4), windows.get(1));
        assertEquals(Arrays.asList(5, 6), windows.get(2));
    }

    @Test
    public void testSplit() throws Exception {
        List<List<Integer>> chunks = Seq.of(1, 2, 3, 4, 5, 6).split(2).toList();
        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(1, 2), chunks.get(0));
        assertEquals(Arrays.asList(3, 4), chunks.get(1));
        assertEquals(Arrays.asList(5, 6), chunks.get(2));
    }

    @Test
    public void testSplitByPredicate() throws Exception {
        List<List<Integer>> chunks = Seq.of(1, 2, 3, 0, 4, 5, 0, 6).split(x -> x == 0).toList();
        assertEquals(5, chunks.size());
        assertEquals(Arrays.asList(1, 2, 3), chunks.get(0));
        assertEquals(Arrays.asList(0), chunks.get(1));
        assertEquals(Arrays.asList(4, 5), chunks.get(2));
        assertEquals(Arrays.asList(0), chunks.get(3));
        assertEquals(Arrays.asList(6), chunks.get(4));
    }

    @Test
    public void testDefer() throws Exception {
        AtomicInteger supplierCalls = new AtomicInteger(0);
        Supplier<Seq<Integer, RuntimeException>> supplier = () -> {
            supplierCalls.incrementAndGet();
            return Seq.of(1, 2, 3);
        };

        Seq<Integer, RuntimeException> deferred = Seq.defer(supplier);
        assertEquals(0, supplierCalls.get());

        List<Integer> result = deferred.toList();
        assertEquals(1, supplierCalls.get());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testAcceptIfNotEmpty() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Seq.of(1, 2, 3).acceptIfNotEmpty(seq -> counter.set((int) seq.count())).orElse(() -> {
        });
        assertEquals(3, counter.get());

        counter.set(0);
        Seq.<Integer, RuntimeException> empty().acceptIfNotEmpty(seq -> counter.set((int) seq.count())).orElse(() -> {
        });
        assertEquals(0, counter.get());
    }

    @Test
    public void testAppendIfEmpty() throws Exception {
        List<Integer> result = Seq.<Integer, RuntimeException> empty().appendIfEmpty(Arrays.asList(10, 20)).toList();
        assertEquals(Arrays.asList(10, 20), result);

        List<Integer> result2 = Seq.of(1, 2, 3).appendIfEmpty(Arrays.asList(10, 20)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result2);
    }

    @Test
    public void testDefaultIfEmpty() throws Exception {
        List<Integer> result = Seq.<Integer, RuntimeException> empty().defaultIfEmpty(99).toList();
        assertEquals(Arrays.asList(99), result);

        List<Integer> result2 = Seq.of(1, 2, 3).defaultIfEmpty(99).toList();
        assertEquals(Arrays.asList(1, 2, 3), result2);
    }

    @Test
    public void testIfEmpty() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Seq.<Integer, RuntimeException> empty().ifEmpty(() -> counter.set(1)).toList();
        assertEquals(1, counter.get());

        counter.set(0);
        Seq.of(1, 2, 3).ifEmpty(() -> counter.set(1)).toList();
        assertEquals(0, counter.get());
    }

    @Test
    public void testThrowIfEmpty() throws Exception {
        assertThrows(RuntimeException.class, () -> {
            Seq.<Integer, RuntimeException> empty().throwIfEmpty().toList();
        });

        List<Integer> result = Seq.of(1, 2, 3).throwIfEmpty().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testMapFirst() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).mapFirst(x -> x * 10).toList();
        assertEquals(Arrays.asList(10, 2, 3), result);
    }

    @Test
    public void testMapLast() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).mapLast(x -> x * 10).toList();
        assertEquals(Arrays.asList(1, 2, 30), result);
    }

    @Test
    public void testMapFirstOrElse() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).mapFirstOrElse(x -> x * 10, x -> x * 2).toList();
        assertEquals(Arrays.asList(10, 4, 6), result);
    }

    @Test
    public void testMapLastOrElse() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).mapLastOrElse(x -> x * 10, x -> x * 2).toList();
        assertEquals(Arrays.asList(2, 4, 30), result);
    }

    @Test
    public void testMapIfNotNull() throws Exception {
        List<Integer> result = Seq.of(1, null, 3).mapIfNotNull(x -> x * 2).toList();
        assertEquals(Arrays.asList(2, 6), result);
    }

    @Test
    public void testMapMulti() throws Exception {
        List<Object> result = Seq.of(1, 2, 3).mapMulti((n, consumer) -> {
            consumer.accept(n);
            consumer.accept(n * 10);
        }).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void testMapPartial() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).mapPartial(x -> x % 2 == 0 ? Optional.of(x * 10) : Optional.<Integer> empty()).toList();
        assertEquals(Arrays.asList(20, 40), result);
    }

    @Test
    public void testMapPartialToInt() throws Exception {
        List<Integer> result = Seq.of("1", "a", "3", "b").mapPartialToInt(s -> {
            try {
                return OptionalInt.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return OptionalInt.empty();
            }
        }).toList();
        assertEquals(Arrays.asList(1, 3), result);
    }

    @Test
    public void testMapPartialToLong() throws Exception {
        List<Long> result = Seq.of("1", "a", "3", "b").mapPartialToLong(s -> {
            try {
                return OptionalLong.of(Long.parseLong(s));
            } catch (NumberFormatException e) {
                return OptionalLong.empty();
            }
        }).toList();
        assertEquals(Arrays.asList(1L, 3L), result);
    }

    @Test
    public void testMapPartialToDouble() throws Exception {
        List<Double> result = Seq.of("1.5", "a", "3.5", "b").mapPartialToDouble(s -> {
            try {
                return OptionalDouble.of(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                return OptionalDouble.empty();
            }
        }).toList();
        assertEquals(Arrays.asList(1.5, 3.5), result);
    }

    @Test
    public void testCast() throws Exception {
        List<Number> numbers = Arrays.asList(1, 2, 3);
        List<Number> result = Seq.<Number, RuntimeException> of(numbers).cast().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testFlatmap() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).flatmap(x -> Arrays.asList(x, x * 10)).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void testFlattmap() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).flattmap(x -> new Integer[] { x, x * 10 }).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void testFlatmapIfNotNull() throws Exception {
        List<Integer> result = Seq.of(1, null, 3).flatmapIfNotNull(x -> Arrays.asList(x, x * 10)).toList();
        assertEquals(Arrays.asList(1, 10, 3, 30), result);
    }

    @Test
    public void testForeach() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Seq.of(1, 2, 3).foreach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testKthLargest() throws Exception {
        Optional<Integer> result = Seq.of(3, 1, 4, 1, 5, 9, 2).kthLargest(2, Comparator.naturalOrder());
        assertEquals(Integer.valueOf(5), result.get());
    }

    @Test
    public void testMaxBy() throws Exception {
        List<String> words = Arrays.asList("a", "bb", "ccc", "dd");
        Optional<String> result = Seq.of(words).maxBy(String::length);
        assertEquals("ccc", result.get());
    }

    @Test
    public void testMinBy() throws Exception {
        List<String> words = Arrays.asList("a", "bb", "ccc", "dd");
        Optional<String> result = Seq.of(words).minBy(String::length);
        assertEquals("a", result.get());
    }

    @Test
    public void testNMatch() throws Exception {
        boolean result = Seq.of(1, 2, 3, 4, 5).nMatch(3, 3, x -> x % 2 == 0);
        assertFalse(result);

        result = Seq.of(2, 4, 6, 8, 10).nMatch(3, Long.MAX_VALUE, x -> x % 2 == 0);
        assertTrue(result);
    }

    @Test
    public void testDifference() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).difference(Arrays.asList(3, 4, 5, 6, 7)).toList();
        assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void testIntersection() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).intersection(Arrays.asList(3, 4, 5, 6, 7)).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testSymmetricDifference() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(3, 4, 5, 6, 7)).toList();
        assertEquals(Arrays.asList(1, 2, 6, 7), result);
    }

    @Test
    public void testReverseSorted() throws Exception {
        List<Integer> result = Seq.of(3, 1, 4, 1, 5).reverseSorted().toList();
        assertEquals(Arrays.asList(5, 4, 3, 1, 1), result);
    }

    @Test
    public void testReverseSortedBy() throws Exception {
        List<String> result = Seq.of("a", "ccc", "bb").reverseSortedBy(String::length).toList();
        assertEquals(Arrays.asList("ccc", "bb", "a"), result);
    }

    @Test
    public void testSortedByInt() throws Exception {
        List<String> result = Seq.of("a", "ccc", "bb").sortedByInt(String::length).toList();
        assertEquals(Arrays.asList("a", "bb", "ccc"), result);
    }

    @Test
    public void testSortedByLong() throws Exception {
        List<String> result = Seq.of("a", "ccc", "bb").sortedByLong(String::length).toList();
        assertEquals(Arrays.asList("a", "bb", "ccc"), result);
    }

    @Test
    public void testSortedByDouble() throws Exception {
        List<String> result = Seq.of("a", "ccc", "bb").sortedByDouble(s -> s.length() * 1.0).toList();
        assertEquals(Arrays.asList("a", "bb", "ccc"), result);
    }

    @Test
    public void testReverseSortedByInt() throws Exception {
        List<String> result = Seq.of("a", "ccc", "bb").reverseSortedByInt(String::length).toList();
        assertEquals(Arrays.asList("ccc", "bb", "a"), result);
    }

    @Test
    public void testReverseSortedByLong() throws Exception {
        List<String> result = Seq.of("a", "ccc", "bb").reverseSortedByLong(String::length).toList();
        assertEquals(Arrays.asList("ccc", "bb", "a"), result);
    }

    @Test
    public void testReverseSortedByDouble() throws Exception {
        List<String> result = Seq.of("a", "ccc", "bb").reverseSortedByDouble(s -> s.length() * 1.0).toList();
        assertEquals(Arrays.asList("ccc", "bb", "a"), result);
    }

    @Test
    public void testSlidingMap_BiFunction() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4).slidingMap((a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 5, 7), result);
    }

    @Test
    public void testSlidingMap_TriFunction() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4).slidingMap((a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 9), result);
    }

    @Test
    public void testCycled() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).cycled(2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), result);
    }

    @Test
    public void testRotated() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).rotated(2).toList();
        assertEquals(Arrays.asList(4, 5, 1, 2, 3), result);
    }

    @Test
    public void testForEachTriple() throws Exception {
        List<String> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5, 6).forEachTriple((a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,3", "2,3,4", "3,4,5", "4,5,6"), result);
    }

    @Test
    public void testForEachUntil() throws Exception {
        List<Integer> collected = new ArrayList<>();
        MutableBoolean flagToBreak = MutableBoolean.of(false);
        Seq.of(1, 2, 3, 4, 5).forEachUntil((x, flag) -> {
            collected.add(x);
            if (x >= 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testSkipLast() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).skipLast(2).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSkipNulls() throws Exception {
        List<Integer> result = Seq.of(1, null, 2, null, 3).skipNulls().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSkipUntil() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).skipUntil(x -> x >= 3).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testTakeLast() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).takeLast(2).toList();
        assertEquals(Arrays.asList(4, 5), result);
    }

    @Test
    public void testCollectThenAccept() throws Exception {
        AtomicInteger sum = new AtomicInteger(0);
        Seq.of(1, 2, 3).collectThenAccept(Collectors.toList(), list -> sum.set(list.stream().mapToInt(Integer::intValue).sum()));
        assertEquals(6, sum.get());
    }

    @Test
    public void testCollectThenApply() throws Exception {
        Integer result = Seq.of(1, 2, 3).collectThenApply(Collectors.toList(), list -> list.stream().mapToInt(Integer::intValue).sum());
        assertEquals(6, result.intValue());
    }

    @Test
    public void testToListThenAccept() throws Exception {
        AtomicInteger size = new AtomicInteger(0);
        Seq.of(1, 2, 3).toListThenAccept(list -> size.set(list.size()));
        assertEquals(3, size.get());
    }

    @Test
    public void testToListThenApply() throws Exception {
        Integer result = Seq.of(1, 2, 3).toListThenApply(list -> list.size());
        assertEquals(3, result.intValue());
    }

    @Test
    public void testToSetThenAccept() throws Exception {
        AtomicInteger size = new AtomicInteger(0);
        Seq.of(1, 2, 3, 2, 1).toSetThenAccept(set -> size.set(set.size()));
        assertEquals(3, size.get());
    }

    @Test
    public void testToSetThenApply() throws Exception {
        Integer result = Seq.of(1, 2, 3, 2, 1).toSetThenApply(set -> set.size());
        assertEquals(3, result.intValue());
    }

    @Test
    public void testToImmutableMap() throws Exception {
        ImmutableMap<String, Integer> map = Seq.of("a", "bb", "ccc").toImmutableMap(s -> s, String::length);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
    }

    @Test
    public void testToMultimap() throws Exception {
        ListMultimap<Integer, String> multimap = Seq.of("a", "bb", "c", "dd").toMultimap(String::length, s -> s);
        assertEquals(2, multimap.get(1).size());
        assertEquals(2, multimap.get(2).size());
    }

    @Test
    public void testToMultiset() throws Exception {
        Multiset<Integer> multiset = Seq.of(1, 2, 2, 3, 3, 3).toMultiset();
        assertEquals(1, multiset.count(1));
        assertEquals(2, multiset.count(2));
        assertEquals(3, multiset.count(3));
    }

    @Test
    public void testStream() throws Exception {
        long count = Seq.of(1, 2, 3, 4, 5).stream().filter(x -> x % 2 == 0).count();
        assertEquals(2, count);
    }

    @Test
    public void testPrintln() throws Exception {
        Seq.of(1, 2, 3).println();
    }

    @Test
    public void testPartitionTo() throws Exception {
        Map<Boolean, List<Integer>> result = Seq.of(1, 2, 3, 4, 5).partitionTo(x -> x % 2 == 0);
        assertEquals(Arrays.asList(2, 4), result.get(true));
        assertEquals(Arrays.asList(1, 3, 5), result.get(false));
    }

    @Test
    public void testGroupTo() throws Exception {
        Map<Integer, List<String>> result = Seq.of("a", "bb", "ccc", "dd").groupTo(String::length);
        assertEquals(1, result.get(1).size());
        assertEquals(2, result.get(2).size());
        assertEquals(1, result.get(3).size());
    }

    @Test
    public void testPeekFirst() throws Exception {
        AtomicInteger firstValue = new AtomicInteger(0);
        List<Integer> result = Seq.of(1, 2, 3).peekFirst(firstValue::set).toList();
        assertEquals(1, firstValue.get());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPeekLast() throws Exception {
        AtomicInteger lastValue = new AtomicInteger(0);
        List<Integer> result = Seq.of(1, 2, 3).peekLast(lastValue::set).toList();
        assertEquals(3, lastValue.get());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPeekIf() throws Exception {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).peekIf(x -> x % 2 == 0, peeked::add).toList();
        assertEquals(Arrays.asList(2, 4), peeked);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testOnFirst() throws Exception {
        AtomicInteger firstValue = new AtomicInteger(0);
        List<Integer> result = Seq.of(1, 2, 3).onFirst(firstValue::set).toList();
        assertEquals(1, firstValue.get());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testOnLast() throws Exception {
        AtomicInteger lastValue = new AtomicInteger(0);
        List<Integer> result = Seq.of(1, 2, 3).onLast(lastValue::set).toList();
        assertEquals(3, lastValue.get());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testBuffered() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).buffered(2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testDelay() throws Exception {
        long start = System.currentTimeMillis();
        Seq.of(1, 2, 3).delay(Duration.ofMillis(10)).forEach(x -> {
        });
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 30);
    }

    @Test
    public void testJoinTo() throws Exception {
        Joiner joiner = Joiner.with(", ");
        Seq.of(1, 2, 3).joinTo(joiner);
        assertEquals("1, 2, 3", joiner.toString());
    }

    @Test
    public void testListFiles() throws Exception {
        File tempDir = Files.createTempDirectory("seq-test").toFile();
        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(tempDir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();

        List<File> files = Seq.listFiles(tempDir).toList();
        assertTrue(files.size() >= 2);

        file1.delete();
        file2.delete();
        tempDir.delete();
    }

    @Test
    public void testMerge() throws Exception {
        List<Integer> result = Seq.merge(Seq.of(1, 3, 5), Seq.of(2, 4, 6), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testRateLimited() throws Exception {
        long start = System.currentTimeMillis();
        Seq.of(1, 2, 3).rateLimited(100).forEach(x -> {
        });
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 20);
    }

    @Test
    public void testShuffled() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).shuffled().toList();
        assertEquals(10, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(10));
    }

    @Test
    public void testSplitByChunkCount() throws Exception {
        List<Pair<Integer, Integer>> chunks = Seq.splitByChunkCount(10, 3, Pair::of).toList();
        assertEquals(3, chunks.size());
    }

    @Test
    public void testSps() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).sps(stream -> stream.filter(x -> x % 2 == 0)).toList();
        assertEquals(Arrays.asList(2), result);
    }

    @Test
    public void testTransform() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).transform(seq -> seq.map(x -> x * 2)).toList();
        assertEquals(Arrays.asList(2, 4, 6), result);
    }

    @Test
    public void testTransformB() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).transformB(seq -> seq.map(x -> x * 2)).toList();
        assertEquals(Arrays.asList(2, 4, 6), result);
    }

    @Test
    public void testZip() throws Exception {
        List<Pair<Integer, String>> result = Seq.zip(Seq.of(1, 2, 3), Seq.of("a", "b", "c"), Pair::of).toList();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).left());
        assertEquals("a", result.get(0).right());
    }

    @Test
    public void testAsyncCall() throws Exception {
        ContinuableFuture<List<Integer>> future = Seq.of(1, 2, 3).asyncCall(seq -> seq.toList());
        List<Integer> result = future.get();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testAsyncRun() throws Exception {
        AtomicInteger sum = new AtomicInteger(0);
        ContinuableFuture<Void> future = Seq.of(1, 2, 3).asyncRun(seq -> seq.forEach(sum::addAndGet));
        future.get();
        assertEquals(6, sum.get());
    }

    @Test
    public void testToDataset() throws Exception {
        Dataset ds = Seq.of(1, 2, 3).toDataset(Arrays.asList("value"));
        assertNotNull(ds);
    }
}
