package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.XmlDeserConfig;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.ToShortFunction;

public class NCoverageTest extends NTestSupport {

    @Test
    public void testMapToPrimitives_collectionAndRange() {
        final List<Integer> randomAccess = Arrays.asList(10, 20, 30, 40, 50);
        final LinkedList<Integer> sequential = new LinkedList<>(randomAccess);

        assertArrayEquals(new short[] { 10, 20, 30, 40, 50 }, N.mapToShort(randomAccess, Integer::shortValue));
        assertArrayEquals(new short[] { 20, 30, 40 }, N.mapToShort(randomAccess, 1, 4, Integer::shortValue));
        assertArrayEquals(new short[] { 20, 30, 40 }, N.mapToShort(sequential, 1, 4, Integer::shortValue));
        assertEquals(0, N.mapToShort(randomAccess, 2, 2, Integer::shortValue).length);
        assertThrows(IllegalArgumentException.class, () -> N.mapToShort(randomAccess, (ToShortFunction<Integer>) null));

        assertArrayEquals(new byte[] { 10, 20, 30, 40, 50 }, N.mapToByte(randomAccess, Integer::byteValue));
        assertArrayEquals(new byte[] { 20, 30, 40 }, N.mapToByte(randomAccess, 1, 4, Integer::byteValue));
        assertArrayEquals(new byte[] { 20, 30, 40 }, N.mapToByte(sequential, 1, 4, Integer::byteValue));
        assertEquals(0, N.mapToByte(randomAccess, 3, 3, Integer::byteValue).length);
        assertThrows(IllegalArgumentException.class, () -> N.mapToByte(randomAccess, (ToByteFunction<Integer>) null));

        assertArrayEquals(new long[] { 10L, 20L, 30L, 40L, 50L }, N.mapToLong(randomAccess, Integer::longValue));
        assertArrayEquals(new long[] { 20L, 30L, 40L }, N.mapToLong(randomAccess, 1, 4, Integer::longValue));
        assertArrayEquals(new long[] { 20L, 30L, 40L }, N.mapToLong(sequential, 1, 4, Integer::longValue));
        assertEquals(0, N.mapToLong(randomAccess, 0, 0, Integer::longValue).length);
        assertThrows(IllegalArgumentException.class, () -> N.mapToLong(randomAccess, (ToLongFunction<Integer>) null));

        assertArrayEquals(new float[] { 10f, 20f, 30f, 40f, 50f }, N.mapToFloat(randomAccess, Integer::floatValue), DELTAf);
        assertArrayEquals(new float[] { 20f, 30f, 40f }, N.mapToFloat(randomAccess, 1, 4, Integer::floatValue), DELTAf);
        assertArrayEquals(new float[] { 20f, 30f, 40f }, N.mapToFloat(sequential, 1, 4, Integer::floatValue), DELTAf);
        assertEquals(0, N.mapToFloat(randomAccess, 5, 5, Integer::floatValue).length);
        assertThrows(IllegalArgumentException.class, () -> N.mapToFloat(randomAccess, (ToFloatFunction<Integer>) null));

        assertArrayEquals(new double[] { 10d, 20d, 30d, 40d, 50d }, N.mapToDouble(randomAccess, Integer::doubleValue), DELTA);
        assertArrayEquals(new double[] { 20d, 30d, 40d }, N.mapToDouble(randomAccess, 1, 4, Integer::doubleValue), DELTA);
        assertArrayEquals(new double[] { 20d, 30d, 40d }, N.mapToDouble(sequential, 1, 4, Integer::doubleValue), DELTA);
        assertEquals(0, N.mapToDouble(randomAccess, 1, 1, Integer::doubleValue).length);
        assertThrows(IllegalArgumentException.class, () -> N.mapToDouble(randomAccess, (ToDoubleFunction<Integer>) null));

        assertArrayEquals(new int[] { 10, 20, 30, 40, 50 }, N.mapToInt(randomAccess, Integer::intValue));
        assertArrayEquals(new int[] { 20, 30, 40 }, N.mapToInt(randomAccess, 1, 4, Integer::intValue));
        assertArrayEquals(new int[] { 20, 30, 40 }, N.mapToInt(sequential, 1, 4, Integer::intValue));
        assertThrows(IllegalArgumentException.class, () -> N.mapToInt(randomAccess, (ToIntFunction<Integer>) null));

        final List<String> words = Arrays.asList("ab", "cde", "fg");
        assertArrayEquals(new char[] { 'a', 'c', 'f' }, N.mapToChar(words, s -> s.charAt(0)));
        assertArrayEquals(new char[] { 'c' }, N.mapToChar(words, 1, 2, s -> s.charAt(0)));
        assertArrayEquals(new char[] { 'c' }, N.mapToChar(new LinkedList<>(words), 1, 2, s -> s.charAt(0)));
        assertThrows(IllegalArgumentException.class, () -> N.mapToChar(words, (ToCharFunction<String>) null));
    }

    @Test
    public void testMapToPrimitives_primitiveArrayOverloads() {
        assertArrayEquals(new long[] { 1L, 2L, 3L }, N.mapToLong(new double[] { 1.9, 2.1, 3.7 }, d -> (long) d));
        assertEquals(0, N.mapToLong((double[]) null, d -> (long) d).length);
        assertThrows(IllegalArgumentException.class, () -> N.mapToLong(new double[] { 1d }, (java.util.function.DoubleToLongFunction) null));

        assertArrayEquals(new double[] { 1d, 2d, 3d }, N.mapToDouble(new long[] { 1L, 2L, 3L }, l -> (double) l), DELTA);
        assertEquals(0, N.mapToDouble((long[]) null, l -> (double) l).length);
    }

    @Test
    public void testMap_iterator() {
        assertEquals(Arrays.asList("1", "2", "3"), N.map(Arrays.asList(1, 2, 3).iterator(), String::valueOf));
        assertTrue(N.map((Iterator<Integer>) null, String::valueOf).isEmpty());
        assertEquals(new HashSet<>(Arrays.asList("1", "2")), N.map(Arrays.asList(1, 2, 1).iterator(), String::valueOf, HashSet::new));
    }

    @Test
    public void testForEachNonNull_twoAndThreeLevel() {
        final List<String> twoLevel = new ArrayList<>();
        N.forEachNonNull(Arrays.asList("ab", null, "cd"), s -> Arrays.asList(s.charAt(0), null, s.charAt(1)), (s, c) -> twoLevel.add(s + ":" + c));
        assertEquals(Arrays.asList("ab:a", "ab:b", "cd:c", "cd:d"), twoLevel);

        N.forEachNonNull((Iterable<String>) null, s -> Arrays.asList(s), (s, u) -> twoLevel.add("x"));
        N.forEachNonNull(Arrays.asList("ab"), s -> null, (s, u) -> twoLevel.add("x"));

        final List<String> tripleFromIterable = new ArrayList<>();
        N.forEachNonNull(Arrays.asList("ab", null, "cd"), s -> Arrays.asList(s.substring(0, 1), null, s.substring(1)),
                ch -> Arrays.asList(ch.toUpperCase(), null, ch.toLowerCase()), (orig, mid, leaf) -> tripleFromIterable.add(orig + ":" + mid + ":" + leaf));
        assertEquals(8, tripleFromIterable.size());
        assertTrue(tripleFromIterable.contains("ab:a:A"));
        assertTrue(tripleFromIterable.contains("cd:d:d"));

        final List<String> tripleFromIterator = new ArrayList<>();
        N.forEachNonNull(Arrays.asList("ab", null, "cd").iterator(), s -> Arrays.asList(s.substring(0, 1), null),
                ch -> Arrays.asList(ch.toUpperCase(), null, ch), (orig, mid, leaf) -> tripleFromIterator.add(orig + ":" + mid + ":" + leaf));
        assertEquals(4, tripleFromIterator.size());
        N.forEachNonNull((Iterator<String>) null, s -> Arrays.asList(s), ch -> Arrays.asList(ch), (a, b, c) -> {
        });

        final List<String> foreachTriple = new ArrayList<>();
        N.forEach(Arrays.asList("ab", "cd").iterator(), s -> Arrays.asList(s.charAt(0)), c -> Arrays.asList(c.toString()),
                (s, c, u) -> foreachTriple.add(s + ":" + c + ":" + u));
        assertEquals(2, foreachTriple.size());
        N.forEach((Iterator<String>) null, s -> Arrays.asList(s), c -> Arrays.asList(c), (a, b, c) -> {
        });
    }

    @Test
    public void testForEachPairAndTriple() {
        final List<String> pairs = new ArrayList<>();
        N.forEachPair(Arrays.asList("a", "b", "c").iterator(), (x, y) -> pairs.add(x + y));
        assertEquals(Arrays.asList("ab", "bc"), pairs);

        final List<String> triples = new ArrayList<>();
        N.forEachTriple(Arrays.asList("a", "b", "c", "d"), (x, y, z) -> triples.add("" + x + y + z));
        assertEquals(Arrays.asList("abc", "bcd"), triples);

        final List<String> triplesIter = new ArrayList<>();
        N.forEachTriple(Arrays.asList("a", "b", "c", "d").iterator(), (x, y, z) -> triplesIter.add("" + x + y + z));
        assertEquals(Arrays.asList("abc", "bcd"), triplesIter);

        final List<String> stepped = new ArrayList<>();
        N.forEachTriple(Arrays.asList("a", "b", "c", "d", "e").iterator(), 2, (x, y, z) -> stepped.add("" + x + y + z));
        assertTrue(stepped.size() >= 2);
    }

    @Test
    public void testMinMaxOrDefaultIfEmpty() {
        final List<String> words = Arrays.asList("aa", "b", "ccc");
        assertEquals(1, N.minIntOrDefaultIfEmpty(words, String::length, -1));
        assertEquals(1, N.minIntOrDefaultIfEmpty(words.iterator(), String::length, -1));
        assertEquals(-1, N.minIntOrDefaultIfEmpty((Iterator<String>) null, String::length, -1));
        assertEquals(-1, N.minIntOrDefaultIfEmpty(N.<String> emptyList(), String::length, -1));

        assertEquals(3, N.maxIntOrDefaultIfEmpty(words, String::length, -1));
        assertEquals(3, N.maxIntOrDefaultIfEmpty(words.iterator(), String::length, -1));
        assertEquals(-1, N.maxIntOrDefaultIfEmpty((Iterator<String>) null, String::length, -1));
        assertEquals(-1, N.maxIntOrDefaultIfEmpty(N.<String> emptyList(), String::length, -1));

        assertEquals(1L, N.minLongOrDefaultIfEmpty(words.iterator(), s -> (long) s.length(), -1L));
        assertEquals(-1L, N.minLongOrDefaultIfEmpty((Iterator<String>) null, s -> (long) s.length(), -1L));
        assertEquals(3L, N.maxLongOrDefaultIfEmpty(words.iterator(), s -> (long) s.length(), -1L));
        assertEquals(-1L, N.maxLongOrDefaultIfEmpty((Iterator<String>) null, s -> (long) s.length(), -1L));

        assertEquals(1d, N.minDoubleOrDefaultIfEmpty(words, s -> (double) s.length(), -1d), DELTA);
        assertEquals(1d, N.minDoubleOrDefaultIfEmpty(words.iterator(), s -> (double) s.length(), -1d), DELTA);
        assertEquals(-1d, N.minDoubleOrDefaultIfEmpty((Iterator<String>) null, s -> (double) s.length(), -1d), DELTA);
        assertEquals(3d, N.maxDoubleOrDefaultIfEmpty(words, s -> (double) s.length(), -1d), DELTA);
        assertEquals(3d, N.maxDoubleOrDefaultIfEmpty(words.iterator(), s -> (double) s.length(), -1d), DELTA);
        assertEquals(-1d, N.maxDoubleOrDefaultIfEmpty((Iterator<String>) null, s -> (double) s.length(), -1d), DELTA);
        assertEquals(-1d, N.maxDoubleOrDefaultIfEmpty(N.<String> emptyList(), s -> (double) s.length(), -1d), DELTA);
    }

    @Test
    public void testRunInParallel_threeAndFour() {
        final AtomicInteger a = new AtomicInteger();
        final AtomicInteger b = new AtomicInteger();
        final AtomicInteger c = new AtomicInteger();
        final AtomicInteger d = new AtomicInteger();

        N.runInParallel(a::incrementAndGet, b::incrementAndGet, c::incrementAndGet);
        assertEquals(1, a.get());
        assertEquals(1, b.get());
        assertEquals(1, c.get());

        N.runInParallel(a::incrementAndGet, b::incrementAndGet, c::incrementAndGet, d::incrementAndGet);
        assertEquals(1, d.get());
        assertEquals(2, a.get());

        assertThrows(IllegalArgumentException.class, () -> N.runInParallel(() -> {
        }, () -> {
        }, null));
        assertThrows(IllegalArgumentException.class, () -> N.runInParallel(() -> {
        }, () -> {
        }, () -> {
        }, null));

        assertThrows(RuntimeException.class, () -> N.runInParallel(() -> {
            throw new RuntimeException("fail-3");
        }, () -> Thread.sleep(20), () -> Thread.sleep(20)));

        assertThrows(RuntimeException.class, () -> N.runInParallel(() -> {
            throw new RuntimeException("fail-4");
        }, () -> Thread.sleep(20), () -> Thread.sleep(20), () -> Thread.sleep(20)));
    }

    @Test
    public void testCallInParallel_threeAndFourFailure() {
        Tuple3<String, Integer, Boolean> three = N.callInParallel(() -> "a", () -> 1, () -> true);
        assertEquals("a", three._1);
        assertEquals(1, three._2);
        assertEquals(true, three._3);

        Tuple4<String, Integer, Boolean, Double> four = N.callInParallel(() -> "a", () -> 1, () -> true, () -> 2.0);
        assertEquals(2.0, four._4, DELTA);

        assertThrows(RuntimeException.class, () -> N.callInParallel(() -> {
            throw new RuntimeException("c3");
        }, () -> 1, () -> true));
        assertThrows(RuntimeException.class, () -> N.callInParallel(() -> {
            throw new RuntimeException("c4");
        }, () -> 1, () -> true, () -> 2.0));
    }

    @Test
    public void testAsyncExecute_collectionAndDelay() throws Exception {
        final Collection<Callable<Integer>> callables = new LinkedHashSet<>();
        callables.add(() -> 1);
        callables.add(() -> 2);
        final List<Integer> results = new ArrayList<>();
        for (ContinuableFuture<Integer> future : N.asyncExecute(callables, executorService)) {
            results.add(future.get());
        }
        assertEquals(2, results.size());
        assertTrue(results.containsAll(Arrays.asList(1, 2)));

        assertEquals(0, N.asyncExecute((Collection<Callable<Integer>>) null, executorService).size());
        assertThrows(IllegalArgumentException.class, () -> N.asyncExecute(callables, (java.util.concurrent.Executor) null));

        assertEquals("delayed", N.asyncExecute(() -> "delayed", 20L).get());
    }

    @Test
    public void testZip_threeIterablesWithDefaults_allCollectionCombinations() {
        final List<String> aCol = Arrays.asList("a", "b");
        final List<Integer> bCol = Arrays.asList(1, 2, 3);
        final List<Boolean> cCol = Arrays.asList(true);
        final CustomIterable<String> aIt = new CustomIterable<>(aCol);
        final CustomIterable<Integer> bIt = new CustomIterable<>(bCol);
        final CustomIterable<Boolean> cIt = new CustomIterable<>(cCol);

        assertEquals(Arrays.asList("a1true", "b2false", "x3false"), N.zip(aCol, bCol, cCol, "x", 0, false, (s, i, bool) -> s + i + bool));
        assertEquals(Arrays.asList("a1true", "b2false", "x3false"), N.zip(aCol, bCol, cIt, "x", 0, false, (s, i, bool) -> s + i + bool));
        assertEquals(Arrays.asList("a1true", "b2false", "x3false"), N.zip(aCol, bIt, cCol, "x", 0, false, (s, i, bool) -> s + i + bool));
        assertEquals(Arrays.asList("a1true", "b2false", "x3false"), N.zip(aCol, bIt, cIt, "x", 0, false, (s, i, bool) -> s + i + bool));
        assertEquals(Arrays.asList("a1true", "b2false", "x3false"), N.zip(aIt, bCol, cCol, "x", 0, false, (s, i, bool) -> s + i + bool));
        assertEquals(Arrays.asList("a1true", "b2false", "x3false"), N.zip(aIt, bCol, cIt, "x", 0, false, (s, i, bool) -> s + i + bool));
        assertEquals(Arrays.asList("a1true", "b2false", "x3false"), N.zip(aIt, bIt, cCol, "x", 0, false, (s, i, bool) -> s + i + bool));
        assertEquals(Arrays.asList("a1true", "b2false", "x3false"), N.zip(aIt, bIt, cIt, "x", 0, false, (s, i, bool) -> s + i + bool));

        assertTrue(N.zip((Iterable<String>) null, (Iterable<Integer>) null, (Iterable<Boolean>) null, "x", 0, false, (s, i, bool) -> s + i + bool).isEmpty());
        assertEquals(3, N.zip((List<String>) null, bCol, (List<Boolean>) null, "x", 0, false, (s, i, bool) -> s + i + bool).size());
        assertThrows(IllegalArgumentException.class, () -> N.zip(aCol, bCol, cCol, "x", 0, false, null));
    }

    @Test
    public void testFromJsonAndXml_readerOverloads() {
        assertEquals(Integer.valueOf(1), N.fromJson(new StringReader("1"), Integer.class));
        assertEquals(Integer.valueOf(2), N.fromJson(new StringReader("2"), N.typeOf(Integer.class)));
        assertEquals(Integer.valueOf(3), N.fromJson(new StringReader("3"), JsonDeserConfig.create(), Integer.class));

        @SuppressWarnings("unchecked")
        final Type<List<Integer>> listType = (Type<List<Integer>>) (Type<?>) N.typeOf("List<Integer>");
        assertEquals(Arrays.asList(1, 2, 3), N.fromJson(new StringReader("[1,2,3]"), JsonDeserConfig.create(), listType));

        final String xml = "<map><e><k>a</k><v>1</v></e></map>";
        assertTrue(N.fromXml(new StringReader(xml), Map.class) instanceof Map);
        assertTrue(N.fromXml(new StringReader(xml), N.typeOf(Map.class)) instanceof Map);
        assertTrue(N.fromXml(new StringReader(xml), XmlDeserConfig.create(), N.typeOf(Map.class)) instanceof Map);
    }

    @Test
    public void testForEachIndexed_collectionAndArrayRange() {
        final List<String> seen = new ArrayList<>();
        N.forEachIndexed(Arrays.asList("a", "b", "c", "d"), 1, 3, (i, v) -> seen.add(i + v));
        assertEquals(Arrays.asList("1b", "2c"), seen);

        seen.clear();
        N.forEachIndexed(new LinkedList<>(Arrays.asList("a", "b", "c", "d")), 1, 3, (i, v) -> seen.add(i + v));
        assertEquals(Arrays.asList("1b", "2c"), seen);

        seen.clear();
        N.forEachIndexed(new String[] { "a", "b", "c", "d" }, 1, 3, (i, v) -> seen.add(i + v));
        assertEquals(Arrays.asList("1b", "2c"), seen);

        N.forEachIndexed(Arrays.asList("a", "b"), 0, 0, (i, v) -> seen.add("x"));
        assertEquals(2, seen.size());
    }

    @Test
    public void testForEach_collectionRange() {
        final List<String> seen = new ArrayList<>();
        N.forEach(Arrays.asList("a", "b", "c", "d"), 1, 3, seen::add);
        assertEquals(Arrays.asList("b", "c"), seen);

        seen.clear();
        N.forEach(new LinkedList<>(Arrays.asList("a", "b", "c", "d")), 1, 3, seen::add);
        assertEquals(Arrays.asList("b", "c"), seen);
    }

    @Test
    public void testMax_collectionRange() {
        assertEquals(4, N.max(Arrays.asList(1, 4, 2, 3), 0, 4, Integer::compare).intValue());
        assertEquals(3, N.max(Arrays.asList(1, 4, 2, 3), 2, 4, Integer::compare).intValue());
        assertEquals(4, N.max(new LinkedList<>(Arrays.asList(1, 4, 2, 3)), 0, 4, Integer::compare).intValue());
    }

    @Test
    public void testCallAsync_coversTakeCompletion() throws Exception {
        final ExecutorService exec = Executors.newFixedThreadPool(2);
        try {
            final List<Integer> values = N.callAsync(Arrays.<Callable<Integer>> asList(() -> {
                N.sleepUninterruptibly(30);
                return 1;
            }, () -> 2), exec).toList();
            assertEquals(2, values.size());
            assertTrue(values.containsAll(Arrays.asList(1, 2)));
        } finally {
            exec.shutdown();
        }
    }

    @Test
    public void testSumToBigInteger_range() {
        assertEquals(BigInteger.ZERO, N.sumToBigInteger((long[]) null, 0, 0));
        assertEquals(BigInteger.ZERO, N.sumToBigInteger(new long[] { 1L, 2L }, 1, 1));
        assertEquals(BigInteger.valueOf(6), N.sumToBigInteger(new long[] { 1L, 2L, 3L }, 0, 3));
        assertEquals(BigInteger.valueOf(5), N.sumToBigInteger(new long[] { 1L, 2L, 3L }, 1, 3));
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(Long.MAX_VALUE)),
                N.sumToBigInteger(new long[] { Long.MAX_VALUE, Long.MAX_VALUE }, 0, 2));
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(Long.MAX_VALUE)), N.sumToBigInteger(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(BigInteger.ZERO, N.sumToBigInteger());
        assertThrows(IndexOutOfBoundsException.class, () -> N.sumToBigInteger(new long[] { 1L }, 0, 2));
    }

    @Test
    public void testRemoveDuplicates_stringRange() {
        assertEquals(0, N.removeDuplicates((String[]) null, 0, 0, true).length);
        assertEquals(0, N.removeDuplicates(new String[] { "a", "b" }, 1, 1, false).length);
        assertArrayEquals(new String[] { "b" }, N.removeDuplicates(new String[] { "a", "b", "c" }, 1, 2, false));
        assertArrayEquals(new String[] { "a", "b" }, N.removeDuplicates(new String[] { "a", "a", "b", "b" }, 0, 4, true));
        assertArrayEquals(new String[] { "a", "b" }, N.removeDuplicates(new String[] { "a", "b", "a" }, 0, 3, false));
        assertArrayEquals(new String[] { "a", "b", "c" }, N.removeDuplicates(new String[] { "a", "b", "c" }, 0, 3, false));
        assertArrayEquals(new String[] { "b", "c" }, N.removeDuplicates(new String[] { "a", "b", "b", "c" }, 1, 4, true));
        assertArrayEquals(new String[] { "x", "y" }, N.removeDuplicates(new String[] { "x", "y", "x" }, false));
        assertArrayEquals(new String[] { "x", "y" }, N.removeDuplicates(new String[] { "x", "x", "y" }));
    }

    @Test
    public void testZip_twoIterablesWithDefaults() {
        final List<String> aCol = Arrays.asList("a", "b");
        final List<Integer> bCol = Arrays.asList(1, 2, 3);
        final CustomIterable<String> aIt = new CustomIterable<>(aCol);
        final CustomIterable<Integer> bIt = new CustomIterable<>(bCol);

        assertEquals(Arrays.asList("a1", "b2", "x3"), N.zip(aCol, bCol, "x", 0, (s, i) -> s + i));
        assertEquals(Arrays.asList("a1", "b2", "x3"), N.zip(aCol, bIt, "x", 0, (s, i) -> s + i));
        assertEquals(Arrays.asList("a1", "b2", "x3"), N.zip(aIt, bCol, "x", 0, (s, i) -> s + i));
        assertEquals(Arrays.asList("a1", "b2", "x3"), N.zip(aIt, bIt, "x", 0, (s, i) -> s + i));
        assertEquals(Arrays.asList("x1", "x2", "x3"), N.zip((Iterable<String>) null, bCol, "x", 0, (s, i) -> s + i));
        assertEquals(Arrays.asList("a0", "b0"), N.zip(aCol, (Iterable<Integer>) null, "x", 0, (s, i) -> s + i));
        assertTrue(N.zip((Iterable<String>) null, (Iterable<Integer>) null, "x", 0, (s, i) -> s + i).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.zip(aCol, bCol, "x", 0, null));
    }

    @Test
    public void testUnzip_iteratorOverloads() {
        final List<String> pairs = Arrays.asList("a=1", "b=2");
        final Pair<List<String>, List<Integer>> unzipped = N.unzip(pairs.iterator(), (s, out) -> {
            final String[] parts = s.split("=");
            out.set(parts[0], Integer.valueOf(parts[1]));
        });
        assertEquals(Arrays.asList("a", "b"), unzipped.left());
        assertEquals(Arrays.asList(1, 2), unzipped.right());

        final Pair<List<String>, List<Integer>> withSupplier = N.unzip(pairs.iterator(), (s, out) -> {
            final String[] parts = s.split("=");
            out.set(parts[0], Integer.valueOf(parts[1]));
        }, (IntFunction<List<?>>) ArrayList::new);
        assertEquals(Arrays.asList("a", "b"), withSupplier.left());
        assertEquals(Arrays.asList(1, 2), withSupplier.right());

        final List<String> rows = Arrays.asList("a-1-x", "b-2-y");
        final Triple<List<String>, List<String>, List<String>> triple = N.unzip3(rows.iterator(), (s, out) -> {
            final String[] parts = s.split("-");
            out.set(parts[0], parts[1], parts[2]);
        });
        assertEquals(Arrays.asList("a", "b"), triple.left());
        assertEquals(Arrays.asList("1", "2"), triple.middle());
        assertEquals(Arrays.asList("x", "y"), triple.right());

        final Triple<List<String>, List<String>, List<String>> tripleSupplied = N.unzip3(rows.iterator(), (s, out) -> {
            final String[] parts = s.split("-");
            out.set(parts[0], parts[1], parts[2]);
        }, (IntFunction<List<?>>) ArrayList::new);
        assertEquals(Arrays.asList("a", "b"), tripleSupplied.left());
        assertThrows(IllegalArgumentException.class, () -> N.unzip(pairs.iterator(), null));
        assertThrows(IllegalArgumentException.class, () -> N.unzip3(rows.iterator(), null));
    }
}
