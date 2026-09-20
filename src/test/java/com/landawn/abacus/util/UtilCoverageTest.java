package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.stream.Stream;

public class UtilCoverageTest extends TestBase {

    @Test
    public void testCommonUtilClone_missingDims() {
        short[][] shorts = { { 1, 2 }, { 3, 4 } };
        short[][] clonedShorts = CommonUtil.clone(shorts);
        assertNotSame(shorts, clonedShorts);
        clonedShorts[0][0] = 9;
        assertEquals(1, shorts[0][0]);

        long[][] longs = { { 1L, 2L }, { 3L, 4L } };
        long[][] clonedLongs = CommonUtil.clone(longs);
        clonedLongs[0][0] = 9L;
        assertEquals(1L, longs[0][0]);

        float[][] floats = { { 1f, 2f }, { 3f, 4f } };
        float[][] clonedFloats = CommonUtil.clone(floats);
        clonedFloats[0][0] = 9f;
        assertEquals(1f, floats[0][0], 0);

        boolean[][][] bools3 = { { { true, false } }, { { false, true } } };
        boolean[][][] clonedBools3 = CommonUtil.clone(bools3);
        clonedBools3[0][0][0] = false;
        assertTrue(bools3[0][0][0]);

        byte[][][] bytes3 = { { { 1, 2 } }, { { 3, 4 } } };
        byte[][][] clonedBytes3 = CommonUtil.clone(bytes3);
        clonedBytes3[0][0][0] = 9;
        assertEquals(1, bytes3[0][0][0]);

        short[][][] shorts3 = { { { 1, 2 } }, { { 3, 4 } } };
        short[][][] clonedShorts3 = CommonUtil.clone(shorts3);
        clonedShorts3[0][0][0] = 9;
        assertEquals(1, shorts3[0][0][0]);

        long[][][] longs3 = { { { 1L, 2L } }, { { 3L, 4L } } };
        long[][][] clonedLongs3 = CommonUtil.clone(longs3);
        clonedLongs3[0][0][0] = 9L;
        assertEquals(1L, longs3[0][0][0]);

        float[][][] floats3 = { { { 1f, 2f } }, { { 3f, 4f } } };
        float[][][] clonedFloats3 = CommonUtil.clone(floats3);
        clonedFloats3[0][0][0] = 9f;
        assertEquals(1f, floats3[0][0][0], 0);
    }

    @Test
    public void testCommonUtilBinarySearchBooleanAndCompare() {
        assertEquals(-1, CommonUtil.binarySearch((boolean[]) null, true));
        assertEquals(-1, CommonUtil.binarySearch(new boolean[0], true));
        boolean[] sorted = { false, false, true, true };
        assertEquals(0, CommonUtil.binarySearch(sorted, false));
        assertTrue(CommonUtil.binarySearch(sorted, true) >= 2);
        assertEquals(-1, CommonUtil.binarySearch(new boolean[] { true, true }, false));

        assertTrue(CommonUtil.compare(new long[] { 1L, 2L }, new long[] { 1L, 3L }) < 0);
        assertEquals(0, CommonUtil.compare(new long[] { 1L, 2L }, new long[] { 1L, 2L }));
        assertTrue(CommonUtil.compare(new long[] { 1L }, new long[] { 1L, 2L }) < 0);
        assertTrue(CommonUtil.compare(new short[] { 1, 3 }, new short[] { 1, 2 }) > 0);
        assertEquals(0, CommonUtil.compare(new short[] { 1, 2 }, new short[] { 1, 2 }));
        assertTrue(CommonUtil.compare(new byte[] { 1, 2, 3 }, 0, new byte[] { 1, 9, 3 }, 0, 1) == 0);
        assertTrue(CommonUtil.compare(new byte[] { 1, 2, 3 }, 1, new byte[] { 1, 9, 3 }, 1, 1) < 0);
    }

    @Test
    public void testCommonUtilToCollectionAndLinkedHashMap() {
        assertEquals(Arrays.asList(1, 2, 3), CommonUtil.toCollection(Arrays.asList(1, 2, 3), ArrayList::new));
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), CommonUtil.toCollection(Arrays.asList(1, 2, 1), HashSet::new));
        assertTrue(CommonUtil.toCollection((Iterable<Integer>) null, ArrayList::new).isEmpty());

        Map<String, Integer> map = CommonUtil.toLinkedHashMap("a", 1, "b", 2);
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testArrayRepeatObjectAndRangeOverflow() {
        Integer[] repeated = Array.repeat(Integer.valueOf(7), 3);
        assertArrayEquals(new Integer[] { 7, 7, 7 }, repeated);
        assertEquals(0, Array.repeat(Integer.valueOf(7), 0).length);
        assertThrows(IllegalArgumentException.class, () -> Array.repeat(Integer.valueOf(7), -1));
        assertThrows(IllegalArgumentException.class, () -> Array.repeat((Integer) null, 2));

        assertEquals(0, Array.range(5L, 5L, 1L).length);
        assertArrayEquals(new long[] { 1L, 3L, 5L }, Array.range(1L, 6L, 2L));
        assertThrows(IllegalArgumentException.class, () -> Array.range(0L, Long.MAX_VALUE, 1L));
        assertArrayEquals(new long[] { 1L, 2L, 3L }, Array.rangeClosed(1L, 3L, 1L));
        assertEquals(0, Array.rangeClosed(5L, 4L, 1L).length);
    }

    @Test
    public void testArrayTranspose_emptyAndSingle() {
        assertEquals(0, Array.transpose(new long[0][]).length);
        assertEquals(0, Array.transpose(new double[0][]).length);
        assertEquals(0, Array.transpose(new char[0][]).length);
        assertEquals(0, Array.transpose(new byte[0][]).length);
        assertEquals(0, Array.transpose(new boolean[0][]).length);
        assertEquals(0, Array.transpose(new Object[0][]).length);
        assertArrayEquals(new int[][] { { 1 }, { 2 } }, Array.transpose(new int[][] { { 1, 2 } }));
        assertArrayEquals(new String[][] { { "a" }, { "b" } }, Array.transpose(new String[][] { { "a", "b" } }));
    }

    @Test
    public void testIndexOfSubArray() {
        assertFalse(Index.ofSubArray(new short[] { 1, 2, 3, 4 }, 0, new short[] { 9 }, 0, 1).isPresent());
        assertEquals(1, Index.ofSubArray(new short[] { 1, 2, 3, 4 }, 0, new short[] { 2, 3 }, 0, 2).getAsInt());
        assertEquals(1, Index.ofSubArray(new byte[] { 1, 2, 3, 4 }, 0, new byte[] { 2, 3 }, 0, 2).getAsInt());
        assertEquals(2, Index.ofSubArray(new double[] { 1, 2, 3, 4 }, 0, new double[] { 3, 4 }, 0, 2).getAsInt());
        assertEquals(1, Index.ofSubArray(new float[] { 1, 2, 3, 4 }, 0, new float[] { 2, 3 }, 0, 2).getAsInt());
        assertEquals(1, Index.ofSubArray(new Object[] { "a", "b", "c" }, 0, new Object[] { "b", "c" }, 0, 2).getAsInt());
        assertFalse(Index.ofSubArray(new Object[] { "a" }, 0, new Object[] { "z" }, 0, 1).isPresent());

        assertEquals(3, Index.lastOfSubArray(new short[] { 1, 2, 3, 2, 3 }, 5, new short[] { 2, 3 }, 0, 2).getAsInt());
        assertEquals(3, Index.lastOfSubArray(new byte[] { 1, 2, 3, 2, 3 }, 5, new byte[] { 2, 3 }, 0, 2).getAsInt());
        assertEquals(2, Index.lastOfSubArray(new float[] { 1, 2, 3, 4 }, 4, new float[] { 3, 4 }, 0, 2).getAsInt());
        assertFalse(Index.lastOfSubArray(new boolean[] { true, false }, 2, new boolean[] { true, true }, 0, 2).isPresent());
        assertEquals(2, Index.lastOfSubArray(new boolean[] { true, false, true, false }, 4, new boolean[] { true, false }, 0, 2).getAsInt());
    }

    @Test
    public void testMedianOfPrimitiveRanges() {
        assertEquals((short) 2, Median.of(new short[] { 1, 2, 3, 4, 5 }, 0, 3).left());
        assertEquals((byte) 2, Median.of(new byte[] { 1, 2, 3, 4, 5 }, 0, 3).left());
        assertEquals(2L, Median.of(new long[] { 1, 2, 3, 4, 5 }, 0, 3).left());
        assertEquals(2f, Median.of(new float[] { 1, 2, 3, 4, 5 }, 0, 3).left(), 0.0001f);
        assertEquals('b', Median.of(new char[] { 'a', 'b', 'c', 'd' }, 0, 3).left());
        Pair<Short, OptionalShort> even = Median.of(new short[] { 1, 2, 3, 4 }, 0, 4);
        assertEquals((short) 2, even.left());
        assertTrue(even.right().isPresent());
    }

    @Test
    public void testNumbersClampAndLogs() {
        assertEquals(2f, Numbers.clamp(1f, 2f, 4f), 0);
        assertEquals(4f, Numbers.clamp(9f, 2f, 4f), 0);
        assertEquals(3f, Numbers.clamp(3f, 2f, 4f), 0);
        assertThrows(IllegalArgumentException.class, () -> Numbers.clamp(1f, 5f, 2f));

        assertEquals(3, Numbers.log2(8.0, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.log2(9.0, RoundingMode.FLOOR));
        assertEquals(4, Numbers.log2(9.0, RoundingMode.CEILING));
        assertEquals(3, Numbers.log2(10.0, RoundingMode.DOWN));
        assertEquals(4, Numbers.log2(10.0, RoundingMode.UP));
        assertEquals(3, Numbers.log2(11.0, RoundingMode.HALF_DOWN));
        assertEquals(4, Numbers.log2(12.0, RoundingMode.HALF_UP));
        assertEquals(4, Numbers.log2(12.0, RoundingMode.HALF_EVEN));

        assertEquals(2, Numbers.log10(100L, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.log10(101L, RoundingMode.FLOOR));
        assertEquals(3, Numbers.log10(101L, RoundingMode.CEILING));
        assertEquals(2, Numbers.ceilingPowerOfTwo(2));
        assertEquals(16, Numbers.ceilingPowerOfTwo(9));
        assertEquals(0, Numbers.fuzzyCompare(1.0f, 1.0f, 0.001f));
        assertTrue(Numbers.fuzzyCompare(1.0f, 2.0f, 0.001f) < 0);
        assertEquals(0, Numbers.fuzzyCompare(1.0f, 1.0001f, 0.01f));
    }

    @Test
    public void testSeqCoverage() throws Exception {
        final List<Integer> seen = new ArrayList<>();
        Seq.of(3, 1, 2).foreach(seen::add);
        assertEquals(Arrays.asList(3, 1, 2), seen);

        assertEquals(Arrays.asList(3, 2, 1), Seq.of(1, 2, 3).reverseSorted().toList());
        assertEquals(Arrays.asList(3, 2, 1), Seq.of(1, 2, 3).reverseSorted(Comparator.naturalOrder()).toList());
        assertEquals(Arrays.asList("ccc", "bb", "a"), Seq.of("a", "bb", "ccc").reverseSortedByInt(String::length).toList());
        assertEquals(Arrays.asList("ccc", "bb", "a"), Seq.of("a", "bb", "ccc").reverseSortedByLong(s -> (long) s.length()).toList());
        assertEquals(Arrays.asList("ccc", "bb", "a"), Seq.of("a", "bb", "ccc").reverseSortedByDouble(s -> (double) s.length()).toList());

        assertTrue(Seq.of(1, 2, 3, 4).percentiles(Comparator.naturalOrder()).isPresent());
        assertTrue(Seq.<Integer, RuntimeException> empty().percentiles(Comparator.naturalOrder()).isEmpty());

        assertHaveSameElements(Arrays.asList(1, 4, 9), Seq.of(1, 2, 3).sps(s -> s.map(i -> i * i)).toList());
        assertHaveSameElements(Arrays.asList(2, 4, 6), Seq.of(1, 2, 3).sps(2, s -> s.map(i -> i * 2)).toList());
        final ExecutorService exec = Executors.newFixedThreadPool(2);
        try {
            assertHaveSameElements(Arrays.asList(3, 6, 9), Seq.of(1, 2, 3).sps(2, exec, s -> s.map(i -> i * 3)).toList());
        } finally {
            exec.shutdown();
        }

        assertEquals("abc", Seq.of("a", "b", "c").collect(StringBuilder::new, StringBuilder::append, StringBuilder::toString));
        final Map<String, Integer> counts = new HashMap<>();
        Seq.of("a", "b", "a").countBy(s -> s, HashMap::new).forEach(e -> counts.put(e.getKey(), e.getValue()));
        assertEquals(2, counts.get("a"));
        assertEquals(1, counts.get("b"));
        Seq.of("x").println();
        assertEquals(3, Seq.of(1, 2, 3).kthLargest(1, Comparator.naturalOrder()).get());
        assertEquals(2, Seq.of(1, 2, 3).kthLargest(2, Comparator.naturalOrder()).get());
        assertEquals(1, Seq.of(1, 2, 3).kthLargest(3, Comparator.naturalOrder()).get());
    }

    @Test
    public void testOptionalCoverage() throws Exception {
        assertEquals(0, OptionalShort.of((short) 3).compareTo(OptionalShort.of((short) 3)));
        assertTrue(OptionalShort.of((short) 3).compareTo(OptionalShort.empty()) > 0);
        assertTrue(OptionalShort.empty().compareTo(OptionalShort.of((short) 1)) < 0);
        assertEquals(List.of((short) 3), OptionalShort.of((short) 3).toList());
        assertEquals((short) 9, OptionalShort.empty().or(() -> OptionalShort.of((short) 9)).get());
        assertEquals(OptionalShort.of((short) 1).hashCode(), OptionalShort.of((short) 1).hashCode());

        assertEquals(0, OptionalLong.of(3L).compareTo(OptionalLong.of(3L)));
        assertTrue(OptionalLong.of(3L).compareTo(OptionalLong.empty()) > 0);
        assertEquals(Set.of(3L), OptionalLong.of(3L).toSet());
        assertEquals(OptionalLong.of(1L).hashCode(), OptionalLong.of(1L).hashCode());

        assertEquals(OptionalChar.of('A').hashCode(), OptionalChar.of('A').hashCode());
        assertEquals(OptionalChar.of('A'), OptionalChar.of('A'));
        assertNotEquals(OptionalChar.of('A'), OptionalChar.of('B'));
        assertNotEquals(OptionalChar.of('A'), OptionalChar.empty());
        assertNotEquals(OptionalChar.of('A'), "A");

        assertEquals(OptionalByte.of((byte) 1).hashCode(), OptionalByte.of((byte) 1).hashCode());
        assertEquals(OptionalByte.of((byte) 1), OptionalByte.of((byte) 1));
        assertNotEquals(OptionalByte.of((byte) 1), OptionalByte.of((byte) 2));
        assertNotEquals(OptionalByte.of((byte) 1), OptionalByte.empty());
        assertTrue(OptionalByte.of((byte) 1).toImmutableSet().contains((byte) 1));

        final AtomicBoolean emptyRan = new AtomicBoolean();
        OptionalFloat.empty().ifPresentOrElse(v -> {
        }, () -> emptyRan.set(true));
        assertTrue(emptyRan.get());
        OptionalFloat.of(1.5f).ifPresentOrElse(v -> emptyRan.set(false), () -> {
        });
        assertFalse(emptyRan.get());
        OptionalFloat.of(1.5f).ifPresent(v -> emptyRan.set(true));
        assertTrue(emptyRan.get());
        assertEquals(List.of(1.5f), OptionalFloat.of(1.5f).toList());
        assertEquals(Set.of(1.5f), OptionalFloat.of(1.5f).toSet());

        assertEquals(List.of(1.5d), OptionalDouble.of(1.5d).toList());
        assertEquals(Set.of(1.5d), OptionalDouble.of(1.5d).toSet());
        assertEquals(1.5d, OptionalDouble.of(1.5d).orElseThrow("missing"));
        assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().orElseThrow("missing"));
        assertEquals((byte) 7, OptionalByte.of((byte) 7).orElseThrow("missing"));
        assertThrows(NoSuchElementException.class, () -> OptionalByte.empty().orElseThrow("missing"));

        assertTrue(OptionalInt.of(4).filter(i -> i > 0).isPresent());
        assertFalse(OptionalInt.of(4).filter(i -> i < 0).isPresent());
        assertFalse(OptionalInt.empty().filter(i -> true).isPresent());

        assertTrue(OptionalBoolean.of(true).flatMap(v -> OptionalBoolean.of(!v)).isPresent());
        assertFalse(OptionalBoolean.empty().flatMap(v -> OptionalBoolean.of(true)).isPresent());
        assertEquals(List.of(true), OptionalBoolean.of(true).toList());
        assertEquals(Set.of(true), OptionalBoolean.of(true).toSet());
        assertEquals(1, OptionalBoolean.of(true).stream().count());
    }

    @Test
    public void testListsPredicatesOperatorsIf() throws Exception {
        ByteList bytes = ByteList.of((byte) 1, (byte) 2, (byte) 1);
        bytes.replaceAll((byte) 1, (byte) 9);
        assertArrayEquals(new byte[] { 9, 2, 9 }, bytes.toArray());

        BooleanList bools = BooleanList.of(true, false, true, true);
        assertTrue(bools.removeDuplicates());
        assertEquals(2, bools.size());
        BooleanList range = BooleanList.of(true, false, true);
        range.replaceRange(1, 2, BooleanList.of(true, true));
        assertTrue(range.size() >= 3);
        final List<Boolean> walked = new ArrayList<>();
        BooleanList.of(true, false, true).forEach(1, 3, walked::add);
        assertEquals(Arrays.asList(false, true), walked);

        FloatList floats = FloatList.of(1f, 2f, 2f, 3f);
        assertTrue(floats.removeDuplicates());
        final List<Float> fSeen = new ArrayList<>();
        FloatList.of(1f, 2f, 3f).forEach(0, 2, fSeen::add);
        assertEquals(2, fSeen.size());

        ShortList shorts = ShortList.of((short) 1, (short) 1, (short) 2);
        assertTrue(shorts.removeDuplicates());

        final List<Double> dSeen = new ArrayList<>();
        DoubleList.of(1d, 2d, 3d).forEach(1, 3, dSeen::add);
        assertEquals(2, dSeen.size());

        final java.util.function.Predicate<String> distinct = Predicates.distinct();
        assertTrue(distinct.test("a"));
        assertFalse(distinct.test("a"));
        final java.util.function.Predicate<String> distinctBy = Predicates.distinctBy(String::length);
        assertTrue(distinctBy.test("ab"));
        assertFalse(distinctBy.test("cd"));
        assertTrue(Predicates.concurrentDistinct().test("x"));
        assertTrue(Predicates.concurrentDistinctBy(String::length).test("yz"));
        final java.util.function.Predicate<String> skipRepeats = Predicates.skipRepeats();
        assertTrue(skipRepeats.test("a"));
        assertFalse(skipRepeats.test("a"));
        assertTrue(skipRepeats.test("b"));

        Set<Integer> left = new HashSet<>(Arrays.asList(1, 2));
        BinaryOperators.<Integer, Set<Integer>> ofRemoveAll().apply(left, Set.of(2));
        assertEquals(Set.of(1), left);
        Map<String, Integer> m = new HashMap<>();
        m.put("a", 1);
        BinaryOperators.<String, Integer, Map<String, Integer>> ofPutAll().apply(m, Map.of("b", 2));
        assertEquals(2, m.size());
        BinaryOperators.ofAppend().apply(new StringBuilder("a"), new StringBuilder("b"));

        List<String> list = new ArrayList<>();
        BiFunctions.<String, List<String>> ofAdd().apply(list, "a");
        assertEquals(List.of("a"), list);
        BiFunctions.<String, List<String>> ofAddAll().apply(list, List.of("b"));
        assertEquals(List.of("a", "b"), list);
        BiFunctions.<String, List<String>> ofRemove().apply(list, "a");
        assertEquals(List.of("b"), list);

        If.notEmpty(new short[] { 1 }).then(() -> {
        });
        If.notEmpty(new long[] { 1L }).then(() -> {
        });
        If.notEmpty(new float[] { 1f }).then(() -> {
        });
        If.notEmpty(new double[] { 1d }).then(() -> {
        });
        If.notEmpty(new char[] { 'a' }).then(() -> {
        });
        If.notEmpty(new byte[] { 1 }).then(() -> {
        });
        If.notEmpty(new boolean[] { true }).then(() -> {
        });
        If.notEmpty(new Object[] { "a" }).then(() -> {
        });
        If.notEmpty(Map.of("a", 1)).then(() -> {
        });
        If.notEmpty(new int[] { 1 }).then(() -> {
        });

        assertEquals(2, Stream.zip(List.of("a", "b"), List.of(1, 2), List.of(true, false), (s, i, b) -> s + i + b).toList().size());
    }

    @Test
    public void testCommonUtilEqualsFloatDoubleRangeWithDelta() {
        float[] fa = { 1f, 2f, 3f, 9f };
        float[] fb = { 0f, 1.01f, 2.01f, 3.5f };
        assertTrue(CommonUtil.equals(fa, 0, fa, 0, 3, 0.0f));
        assertTrue(CommonUtil.equals(fa, 0, fb, 1, 2, 0.02f));
        assertFalse(CommonUtil.equals(fa, 0, fb, 1, 3, 0.02f));
        assertTrue(CommonUtil.equals(fa, 1, fa, 1, 0, 0.1f));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(fa, 0, fb, 0, -1, 0.1f));

        double[] da = { 1d, 2d, 3d, 9d };
        double[] db = { 0d, 1.01d, 2.01d, 3.5d };
        assertTrue(CommonUtil.equals(da, 0, da, 0, 3, 0.0d));
        assertTrue(CommonUtil.equals(da, 0, db, 1, 2, 0.02d));
        assertFalse(CommonUtil.equals(da, 0, db, 1, 3, 0.02d));
        assertTrue(CommonUtil.equals(da, 2, da, 2, 0, 0.1d));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(da, 0, db, 0, -1, 0.1d));
    }

    @Test
    public void testCommonUtilGetElementLongIndex() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("b", CommonUtil.getElement(list, 1L));
        assertEquals("c", CommonUtil.getElement(new LinkedHashSet<>(list), 2L));
        assertEquals("a", CommonUtil.getElement((Iterable<String>) list::iterator, 0L));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.getElement(list, -1L));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.getElement(list, 9L));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.getElement((Iterable<String>) null, 0L));
    }

    @Test
    public void testCommonUtilFindLastIterator() {
        List<String> list = Arrays.asList("a", "bb", "ccc", "dd");
        assertEquals("dd", CommonUtil.findLast(list.iterator(), s -> s.length() == 2).get());
        assertFalse(CommonUtil.findLast(list.iterator(), s -> s.length() > 9).isPresent());
        assertFalse(CommonUtil.findLast((Iterator<String>) null, s -> true).isPresent());
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.findLast(list.iterator(), null));

        List<String> withNulls = Arrays.asList(null, "a", null, "b", "c", null);
        assertEquals("c", CommonUtil.findLastNonNull(withNulls.iterator(), s -> s.length() == 1).get());
        assertFalse(CommonUtil.findLastNonNull(withNulls.iterator(), s -> s.equals("z")).isPresent());
        assertFalse(CommonUtil.findLastNonNull((Iterator<String>) null, s -> true).isPresent());
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.findLastNonNull(withNulls.iterator(), null));

        Iterable<String> notACollection = list::iterator;
        assertEquals("ccc", CommonUtil.findLast(notACollection, s -> s.startsWith("c")).get());
        assertEquals("c", CommonUtil.findLastNonNull(withNulls::iterator, s -> s.startsWith("c")).orElse(null));
        assertEquals("dd", CommonUtil.findLast(new java.util.LinkedList<>(list), s -> s.length() == 2).get());
    }

    @Test
    public void testCommonUtilAsMapSevenAndEightPairs() {
        Map<String, Integer> seven = CommonUtil.asMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
        assertEquals(7, seven.size());
        assertEquals(7, seven.get("g"));
        Map<String, Integer> eight = CommonUtil.asMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8);
        assertEquals(8, eight.size());
        assertEquals(8, eight.get("h"));
    }

    @Test
    public void testCommonUtilConvertStructured() {
        assertEquals(Integer.valueOf(123), CommonUtil.convert("123", Integer.class));
        assertEquals(0, CommonUtil.convert(null, int.class));
        assertEquals(Boolean.TRUE, CommonUtil.convert(2, Boolean.class));
        assertEquals(Boolean.FALSE, CommonUtil.convert(0, Boolean.class));
        assertArrayEquals(new Integer[] { 1, 2 }, CommonUtil.convert(Arrays.asList("1", "2"), Integer[].class));
        @SuppressWarnings("unchecked")
        final com.landawn.abacus.type.Type<Map<Integer, Integer>> mapType = (com.landawn.abacus.type.Type<Map<Integer, Integer>>) (com.landawn.abacus.type.Type<?>) N
                .typeOf("Map<Integer, Integer>");
        Map<Integer, Integer> converted = CommonUtil.convert(Map.of("1", "2"), mapType);
        assertEquals(2, converted.get(1));
        List<Integer> ints = CommonUtil.convert(new int[] { 4, 5 }, (Class<List<Integer>>) (Class<?>) List.class);
        assertEquals(Arrays.asList(4, 5), ints);
    }

    @Test
    public void testImmutableMapOfTenPairsAndBiMapForcePutAll() {
        ImmutableMap<String, Integer> ten = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9, "j", 10);
        assertEquals(10, ten.size());
        assertEquals(10, ten.get("j"));

        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);
        biMap.forcePutAll(Map.of("two", 1, "three", 3));
        assertEquals(Integer.valueOf(1), biMap.get("two"));
        assertEquals("two", biMap.getByValue(1));
        assertEquals(Integer.valueOf(3), biMap.get("three"));
        assertThrows(IllegalArgumentException.class, () -> biMap.forcePutAll(null));
    }

    @Test
    public void testCharacterWriterWriteCharacterArray() throws Exception {
        try (BufferedJsonWriter writer = new BufferedJsonWriter()) {
            writer.writeCharacter("a\"b\\c".toCharArray());
            writer.writeCharacter(new char[0]);
            writer.writeCharacter("plain".toCharArray());
            assertTrue(writer.toString().contains("plain"));
        }
    }

    @Test
    public void testRangeSpanAndIntFunctionsOfCollection() {
        assertEquals(Range.closed(1, 5), Range.closed(1, 3).span(Range.closed(2, 5)));
        assertEquals(Range.closed(1, 3), Range.closed(1, 3).span(Range.open(2, 2)));
        assertEquals(Range.closed(4, 6), Range.open(5, 5).span(Range.closed(4, 6)));
        Range<Integer> emptyA = Range.open(1, 1);
        Range<Integer> emptyB = Range.open(2, 2);
        assertTrue(emptyA.span(emptyB).isEmpty());

        assertTrue(IntFunctions.<String> ofCollection(LinkedList.class).apply(2) instanceof LinkedList);
        assertTrue(IntFunctions.<String> ofCollection(LinkedHashSet.class).apply(2) instanceof LinkedHashSet);
        assertTrue(IntFunctions.<String> ofCollection(java.util.TreeSet.class).apply(2) instanceof java.util.TreeSet);
        assertTrue(IntFunctions.<String> ofCollection(java.util.concurrent.ConcurrentSkipListSet.class)
                .apply(2) instanceof java.util.concurrent.ConcurrentSkipListSet);
        assertTrue(IntFunctions.<String> ofCollection(java.util.PriorityQueue.class).apply(2) instanceof java.util.PriorityQueue);
        assertThrows(IllegalArgumentException.class, () -> IntFunctions.ofCollection(java.util.AbstractSequentialList.class));
    }

    @Test
    public void testOptionalOrElseThrowThreeParamsAndFilter() throws Exception {
        assertEquals((short) 7, OptionalShort.of((short) 7).orElseThrow("missing %s %s %s", 1, 2, 3));
        assertThrows(NoSuchElementException.class, () -> OptionalShort.empty().orElseThrow("missing %s %s %s", 1, 2, 3));
        assertEquals(7L, OptionalLong.of(7L).orElseThrow("missing %s %s %s", 1, 2, 3));
        assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().orElseThrow("missing %s %s %s", 1, 2, 3));
        assertEquals(1.5d, OptionalDouble.of(1.5d).orElseThrow("missing %s %s %s", 1, 2, 3));
        assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().orElseThrow("missing %s %s %s", 1, 2, 3));
        assertEquals(1.5f, OptionalFloat.of(1.5f).orElseThrow("missing %s %s %s", 1, 2, 3));
        assertThrows(NoSuchElementException.class, () -> OptionalFloat.empty().orElseThrow("missing %s %s %s", 1, 2, 3));
        assertEquals((byte) 7, OptionalByte.of((byte) 7).orElseThrow("missing %s %s %s", 1, 2, 3));
        assertThrows(NoSuchElementException.class, () -> OptionalByte.empty().orElseThrow("missing %s %s %s", 1, 2, 3));

        assertTrue(OptionalBoolean.of(true).filter(v -> v).isPresent());
        assertFalse(OptionalBoolean.of(true).filter(v -> !v).isPresent());
        assertFalse(OptionalBoolean.empty().filter(v -> true).isPresent());

        final AtomicBoolean emptyRan = new AtomicBoolean();
        OptionalDouble.empty().ifPresentOrElse(v -> {
        }, () -> emptyRan.set(true));
        assertTrue(emptyRan.get());
        OptionalDouble.of(1.5d).ifPresentOrElse(v -> emptyRan.set(false), () -> {
        });
        assertFalse(emptyRan.get());
        OptionalBoolean.empty().ifPresentOrElse(v -> {
        }, () -> emptyRan.set(true));
        assertTrue(emptyRan.get());
        OptionalBoolean.of(true).ifPresentOrElse(v -> emptyRan.set(false), () -> {
        });
        assertFalse(emptyRan.get());
    }

    @Test
    public void testSeqKthLargestRemainingBranches() throws Exception {
        assertTrue(Seq.<Integer, RuntimeException> empty().kthLargest(1, Comparator.naturalOrder()).isEmpty());
        assertTrue(Seq.of(1, 2).kthLargest(5, Comparator.naturalOrder()).isEmpty());
        assertEquals(1, Seq.of(1, 2, 3).sorted().kthLargest(3, Comparator.naturalOrder()).get());
        assertEquals(3, Seq.of(1, 2, 3).sorted().kthLargest(1, Comparator.naturalOrder()).get());
        assertEquals(9, Seq.of(1, 9, 3, 7).kthLargest(1, Comparator.naturalOrder()).get());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).kthLargest(0, Comparator.naturalOrder()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).kthLargest(1, null));
    }

    @Test
    public void testArrayRangeLongOverflowAndNegativeStep() {
        assertArrayEquals(new long[] { 10L, 8L, 6L }, Array.range(10L, 5L, -2L));
        assertEquals(0, Array.range(5L, 10L, -1L).length);
        assertThrows(IllegalArgumentException.class, () -> Array.range(0L, 10L, 0L));
        assertThrows(IllegalArgumentException.class, () -> Array.range(Long.MIN_VALUE, Long.MAX_VALUE, 1L));
    }

    @Test
    public void testNumbersLog10BigInteger() {
        assertEquals(2, Numbers.log10(BigInteger.valueOf(100), RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.log10(BigInteger.valueOf(101), RoundingMode.FLOOR));
        assertEquals(3, Numbers.log10(BigInteger.valueOf(101), RoundingMode.CEILING));
        assertEquals(3, Numbers.log10(BigInteger.valueOf(1000), RoundingMode.UNNECESSARY));
        BigInteger huge = BigInteger.TEN.pow(40);
        assertEquals(40, Numbers.log10(huge, RoundingMode.UNNECESSARY));
        assertEquals(40, Numbers.log10(huge.add(BigInteger.ONE), RoundingMode.FLOOR));
        assertEquals(41, Numbers.log10(huge.add(BigInteger.ONE), RoundingMode.CEILING));
        assertEquals(40, Numbers.log10(huge.add(BigInteger.ONE), RoundingMode.DOWN));
        assertEquals(41, Numbers.log10(huge.add(BigInteger.ONE), RoundingMode.UP));
        assertEquals(40, Numbers.log10(huge.add(BigInteger.ONE), RoundingMode.HALF_UP));
    }

    @Test
    public void testDatesTwoDigitYearWithZone() {
        java.util.Date parsed = Dates.parseToJUDate("75-06-15 PST", "yy-MM-dd z", TimeZone.getTimeZone("America/Los_Angeles"), java.util.Locale.US);
        assertTrue(parsed.getTime() != 0);
        java.util.Date parsedY = Dates.parseToJUDate("75-06-15 PST", "y-MM-dd z", TimeZone.getTimeZone("America/Los_Angeles"), java.util.Locale.US);
        assertTrue(parsedY.getTime() != 0);
    }

    @Test
    public void testIoUtilCopyDirectoryWithSymbolicLink() throws Exception {
        java.nio.file.Path srcDir = java.nio.file.Files.createTempDirectory("cov-src");
        java.nio.file.Path destParent = java.nio.file.Files.createTempDirectory("cov-dest");
        try {
            java.nio.file.Path target = srcDir.resolve("target.txt");
            java.nio.file.Files.writeString(target, "hello");
            java.nio.file.Path link = srcDir.resolve("link.txt");
            try {
                java.nio.file.Files.createSymbolicLink(link, target.getFileName());
            } catch (Exception e) {
                org.junit.jupiter.api.Assumptions.assumeTrue(false, "symbolic links not permitted: " + e.getMessage());
            }
            File dest = IOUtil.copyToDirectory(srcDir.toFile(), destParent.toFile());
            assertTrue(dest.exists());
            assertTrue(java.nio.file.Files.exists(dest.toPath().resolve("link.txt")) || java.nio.file.Files.isSymbolicLink(dest.toPath().resolve("link.txt")));
        } finally {
            IOUtil.deleteRecursivelyIfExists(srcDir.toFile());
            IOUtil.deleteRecursivelyIfExists(destParent.toFile());
        }
    }
}
