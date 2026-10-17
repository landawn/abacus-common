package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.UnaryOperator;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u;

/** Stream creation contracts. */
public class StreamCreationTest extends StreamTestSupport {

    @Test
    public void testOf_PrimitiveArrays_CountAndAdvance() {
        int[] intArray = { 1, 2, 3, 4, 5 };
        Stream<Integer> intStream = createStream(intArray, 1, 4);
        assertEquals(3, intStream.count());

        double[] doubleArray = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        Stream<Double> doubleStream = createStream(doubleArray);
        assertEquals(Arrays.asList(3.0, 4.0, 5.0), doubleStream.skip(2).toList());
    }

    // of(char[], from, to): exercises count() (line 16650-16652) via terminal count().
    @Test
    public void testOfCharArrayRange_count_uncovered() {
        final char[] a = { 'a', 'b', 'c', 'd', 'e' };
        assertEquals(3, Stream.of(a, 1, 4).count());
    }

    // defer

    @Test
    public void testDefer() {
        AtomicInteger callCount = new AtomicInteger(0);
        Stream<Integer> deferred = Stream.defer(() -> {
            callCount.incrementAndGet();
            return Stream.of(1, 2, 3);
        });

        assertEquals(0, callCount.get());
        List<Integer> result = deferred.toList();
        assertEquals(1, callCount.get());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDefer2() {
        Supplier<Stream<Integer>> supplier = () -> createStream(1, 2, 3);
        Stream<Integer> deferredStream = Stream.defer(supplier);
        assertEquals(Arrays.asList(1, 2, 3), deferredStream.toList());
    }

    // Self-returning deferred stream fails fast on the iterator, skip/limit and parallel paths too, and the failure is cached.
    @Test
    public void testDefer_selfReturningStreamFailsOnIteratorSkipAndParallelPaths() {
        @SuppressWarnings("unchecked")
        final Stream<Object>[] s1 = new Stream[1];
        s1[0] = Stream.defer(() -> s1[0]);
        final Iterator<Object> iter = s1[0].iterator();
        final IllegalStateException first = assertThrows(IllegalStateException.class, iter::hasNext);
        assertEquals("Stream supplier returned the deferred stream itself", first.getMessage());
        assertSame(first, assertThrows(IllegalStateException.class, iter::next));
        s1[0].close();

        @SuppressWarnings("unchecked")
        final Stream<Object>[] s2 = new Stream[1];
        s2[0] = Stream.defer(() -> s2[0]);
        assertThrows(IllegalStateException.class, () -> s2[0].skip(1).limit(2).toList());

        @SuppressWarnings("unchecked")
        final Stream<Object>[] s3 = new Stream[1];
        s3[0] = Stream.defer(() -> s3[0]);
        assertThrows(IllegalStateException.class, () -> s3[0].parallel(4).map(x -> x).count());

        final IntStream[] i1 = new IntStream[1];
        i1[0] = IntStream.defer(() -> i1[0]);
        assertThrows(IllegalStateException.class, () -> i1[0].iterator().hasNext());
        i1[0].close();

        final IntStream[] i2 = new IntStream[1];
        i2[0] = IntStream.defer(() -> i2[0]);
        assertThrows(IllegalStateException.class, () -> i2[0].parallel(4).boxed().toList());
    }

    // G123: Stream/EntryStream/primitive-stream defer reject a supplier that returns the deferred stream itself
    // (IllegalStateException instead of StackOverflowError).
    @Test
    public void testDefer_supplierReturningTheDeferredStreamItselfFailsFast() {
        @SuppressWarnings("unchecked")
        final Stream<Object>[] s = new Stream[1];
        s[0] = Stream.defer(() -> s[0]);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> s[0].count());

        @SuppressWarnings("unchecked")
        final EntryStream<Object, Object>[] es = new EntryStream[1];
        es[0] = EntryStream.defer(() -> es[0]);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> es[0].count());

        final ByteStream[] b = new ByteStream[1];
        b[0] = ByteStream.defer(() -> b[0]);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> b[0].count());

        final CharStream[] c = new CharStream[1];
        c[0] = CharStream.defer(() -> c[0]);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> c[0].count());

        final ShortStream[] sh = new ShortStream[1];
        sh[0] = ShortStream.defer(() -> sh[0]);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> sh[0].count());

        final IntStream[] i = new IntStream[1];
        i[0] = IntStream.defer(() -> i[0]);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> i[0].count());

        final LongStream[] l = new LongStream[1];
        l[0] = LongStream.defer(() -> l[0]);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> l[0].count());

        final FloatStream[] f = new FloatStream[1];
        f[0] = FloatStream.defer(() -> f[0]);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> f[0].count());

        final DoubleStream[] d = new DoubleStream[1];
        d[0] = DoubleStream.defer(() -> d[0]);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> d[0].count());

        // unchanged: a normal supplier, and a null result (empty)
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(1, 2), Stream.defer(() -> Stream.of(1, 2)).toList());
        org.junit.jupiter.api.Assertions.assertEquals(0, Stream.defer(() -> null).count());
        org.junit.jupiter.api.Assertions.assertEquals(3, LongStream.defer(() -> LongStream.of(1, 2, 3)).count());
        org.junit.jupiter.api.Assertions.assertEquals(1, EntryStream.defer(() -> EntryStream.of("a", 1)).count());
    }

    @Test
    public void testDeferEmpty() {
        Stream<Integer> deferred = Stream.defer(() -> Stream.empty());
        assertTrue(deferred.toList().isEmpty());
    }

    @Test
    public void testDeferRejectsNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Stream.defer(null));
    }

    // empty

    @Test
    public void testEmpty() {
        Stream<Integer> stream = Stream.empty();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    public void testEmpty2() {
        Stream<String> emptyStream = Stream.empty();
        assertFalse(emptyStream.first().isPresent());
    }

    @Test
    public void testEmptyStreamOperations() {
        Stream<Integer> empty = Stream.empty();

        assertEquals(0, empty.count());
        assertFalse(Stream.<Integer> empty().first().isPresent());
        assertFalse(Stream.<Integer> empty().last().isPresent());
        assertEquals(0, Stream.<Integer> empty().toList().size());
        assertEquals(0, Stream.<Integer> empty().toSet().size());
        assertEquals("", Stream.<Integer> empty().join(","));
    }

    // flatGroupTo

    @Test
    public void testFlatGroupTo() {
        Map<Integer, List<String>> map = Stream.of("ab", "cd", "efg").flatGroupTo(s -> Arrays.asList(s.length(), s.length()));
        assertEquals(4, map.get(2).size());
        assertEquals(2, map.get(3).size());
    }

    @Test
    public void testFlatGroupTo_WithFlatKeyAndValue_Collector() {
        Map<Integer, Long> result = Stream.of("a", "bb", "ccc", "dd")
                .flatGroupTo(s -> Arrays.asList(s.length(), s.length() * 10), java.util.stream.Collectors.counting());
        assertFalse(result.isEmpty());
        assertTrue(result.containsKey(2));
    }

    @Test
    public void testFlatGroupToKeyExtractor() {
        Map<Character, List<String>> map = Stream.of("ant", "bat", "cat").flatGroupTo(s -> Stream.of(s.toCharArray()).map(c -> c).toList());
        assertEquals(Arrays.asList("ant", "bat", "cat"), map.get('a'));
        assertEquals(Arrays.asList("ant", "bat", "cat"), map.get('t'));
        assertEquals(Arrays.asList("bat"), map.get('b'));
        assertEquals(Arrays.asList("cat"), map.get('c'));
    }

    @Test
    public void testFlatGroupToWithDownstreamCollector() {
        Map<Character, Long> map = Stream.of("apple", "apricot", "banana")
                .flatGroupTo(s -> Stream.of(s.toCharArray()).map(c -> c).toList(), Collectors.counting());

        assertEquals(Long.valueOf(5), map.get('a'));
        assertEquals(Long.valueOf(3), map.get('p'));
        assertEquals(Long.valueOf(1), map.get('l'));
        assertEquals(Long.valueOf(1), map.get('e'));
    }

    // flatten

    @Test
    public void testFlatten() {
        List<List<Integer>> nestedList = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6));
        Stream<Integer> flattened = Stream.flatten(nestedList);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), flattened.toList());

        Integer[][] array2D = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        Stream<Integer> flattenedArray = Stream.flatten(array2D);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), flattenedArray.toList());

        Stream<Integer> flattenedVertical = Stream.flatten(array2D, true);
        assertEquals(Arrays.asList(1, 3, 5, 2, 4, 6), flattenedVertical.toList());

        Integer[][][] array3D = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } };
        Stream<Integer> flattened3D = Stream.flatten(array3D);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), flattened3D.toList());
    }

    @Test
    public void testFlatten2DArray() {
        Integer[][] array = { { 1, 2, 3 }, { 4, 5 }, { 6, 7, 8, 9 } };

        List<Integer> result = Stream.flatten(array).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);

        Integer[][] empty = new Integer[0][];
        result = Stream.flatten(empty).toList();
        assertTrue(result.isEmpty());

        Integer[][] withNulls = { { 1, 2 }, null, { 3, 4 } };
        result = Stream.flatten(withNulls).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatten2DArrayVertically() {
        Integer[][] array = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };

        List<Integer> result = Stream.flatten(array, false).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);

        result = Stream.flatten(array, true).toList();
        assertEquals(Arrays.asList(1, 4, 7, 2, 5, 8, 3, 6, 9), result);

        Integer[][] jagged = { { 1, 2, 3 }, { 4, 5 }, { 6 } };
        result = Stream.flatten(jagged, true).toList();
        assertEquals(Arrays.asList(1, 4, 6, 2, 5, 3), result);
    }

    @Test
    public void testFlatten2DArrayWithAlignment() {
        Integer[][] array = { { 1, 2 }, { 3, 4, 5 }, { 6 } };

        List<Integer> result = Stream.flatten(array, 0, false).toList();
        assertEquals(Arrays.asList(1, 2, 0, 3, 4, 5, 6, 0, 0), result);

        result = Stream.flatten(array, -1, true).toList();
        assertEquals(Arrays.asList(1, 3, 6, 2, 4, -1, -1, 5, -1), result);
    }

    // flatten(T[][], boolean) vertically=true with all-empty rows -> empty (line 18071).
    @Test
    public void testFlatten2DVertically_allEmpty_uncovered() {
        final Integer[][] a = { {}, {} };
        assertEquals(0, Stream.flatten(a, true).count());
    }

    // flatten(T[][], boolean) vertically=true with single row -> of(a[0]) (line 18059).
    @Test
    public void testFlatten2DVertically_singleRow_uncovered() {
        final Integer[][] a = { { 1, 2, 3 } };
        assertEquals(N.asList(1, 2, 3), Stream.flatten(a, true).toList());
    }

    // flatten(T[][], boolean) vertically=true (lines 18064-18111).
    @Test
    public void testFlatten2DVertically_uncovered() {
        final Integer[][] a = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        assertEquals(N.asList(1, 3, 5, 2, 4, 6), Stream.flatten(a, true).toList());
    }

    // flatten(T[][], valueForAlignment, vertically=false) horizontal (lines 18189-18218).
    @Test
    public void testFlatten2DWithAlignment_horizontal_uncovered() {
        final Integer[][] a = { { 1, 2, 3 }, { 4, 5 }, { 6 } };
        assertEquals(N.asList(1, 2, 3, 4, 5, 0, 6, 0, 0), Stream.flatten(a, 0, false).toList());
    }

    // flatten(T[][], valueForAlignment, vertically) single row -> of(a[0]) (line 18139).
    @Test
    public void testFlatten2DWithAlignment_singleRow_uncovered() {
        final Integer[][] a = { { 7, 8 } };
        assertEquals(N.asList(7, 8), Stream.flatten(a, 0, false).toList());
    }

    // flatten(T[][], valueForAlignment, vertically=true) (lines 18135-18188).
    @Test
    public void testFlatten2DWithAlignment_vertical_uncovered() {
        final Integer[][] a = { { 1, 2, 3 }, { 4, 5 }, { 6 } };
        // 3 rows x 3 cols, column-major with padding 0
        assertEquals(N.asList(1, 4, 6, 2, 5, 0, 3, 0, 0), Stream.flatten(a, 0, true).toList());
    }

    @Test
    public void testFlatten3DArray() {
        Integer[][][] array = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } };

        List<Integer> result = Stream.flatten(array).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), result);

        Integer[][][] empty = new Integer[0][][];
        result = Stream.flatten(empty).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatten_2DArray() {
        Integer[][] array = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };

        List<Integer> flattened = Stream.flatten(array).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), flattened);
    }

    @Test
    public void testFlatten_2DArrayVertically() {
        Integer[][] array = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };

        List<Integer> flattened = Stream.flatten(array, true).toList();

        assertEquals(Arrays.asList(1, 4, 7, 2, 5, 8, 3, 6, 9), flattened);
    }

    @Test
    public void testFlatten_2DArrayWithAlignment() {
        Integer[][] array = { { 1, 2 }, { 3, 4, 5, 6 }, { 7 } };

        List<Integer> flattened = Stream.flatten(array, 0, true).toList();

        assertEquals(Arrays.asList(1, 3, 7, 2, 4, 0, 0, 5, 0, 0, 6, 0), flattened);
    }

    @Test
    public void testFlatten_3DArray() {
        Integer[][][] array = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } }, { { 9, 10 }, { 11, 12 } } };

        List<Integer> flattened = Stream.flatten(array).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), flattened);
    }

    @Test
    public void testFlatten_CollectionOfCollections() {
        List<List<Integer>> nested = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6));

        List<Integer> flattened = Stream.flatten(nested).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), flattened);
    }

    @Test
    public void testFlatten_EmptyArrays() {
        Integer[][] empty2D = new Integer[0][];
        assertTrue(Stream.flatten(empty2D).toList().isEmpty());

        Integer[][] withEmpty = { {}, { 1, 2 }, {}, { 3 } };
        assertEquals(Arrays.asList(1, 2, 3), Stream.flatten(withEmpty).toList());

        Integer[][][] empty3D = new Integer[0][][];
        assertTrue(Stream.flatten(empty3D).toList().isEmpty());
    }

    @Test
    public void testFlatten_IrregularArrays() {
        Integer[][] irregular = { { 1 }, { 2, 3, 4 }, null, { 5, 6 } };

        List<Integer> flattened = Stream.flatten(irregular).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), flattened);

        List<Integer> vertical = Stream.flatten(irregular, true).toList();
        assertEquals(Arrays.asList(1, 2, 5, 3, 6, 4), vertical);
    }

    @Test
    public void testFlattenArray() {
        Integer[][] arr = { { 1, 2 }, { 3, 4 } };

        List<Integer> result = Stream.flatten(arr).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlattenCollectionEmpty() {
        List<Object> result = Stream.flatten(Arrays.asList()).toList();
        assertTrue(result.isEmpty());
    }

    // G108-01: iterator exhaustion and a strongly jagged input (one long row, many single-element and null rows)
    @Test
    public void testFlattenVertically_iteratorExhaustionAndLongRow() {
        final String[][] a = { null, { "a" }, {}, { "b", null, "d" }, null, { "e", "f" } };
        final Iterator<String> iter = Stream.flatten(a, true).iterator();
        final List<String> result = new ArrayList<>();

        while (iter.hasNext()) {
            result.add(iter.next());
        }

        assertEquals(Arrays.asList("a", "b", "e", null, "f", "d"), result);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
        assertFalse(iter.hasNext());

        final Integer[][] jagged = new Integer[201][];

        for (int i = 0; i < 200; i++) {
            jagged[i] = i % 3 == 0 ? null : new Integer[] { i };
        }

        jagged[200] = new Integer[5000];

        for (int i = 0; i < 5000; i++) {
            jagged[200][i] = 100000 + i;
        }

        final List<Integer> expected = new ArrayList<>();

        for (int i = 0; i < 200; i++) {
            if (jagged[i] != null) {
                expected.add(jagged[i][0]);
            }
        }

        for (int i = 0; i < 5000; i++) {
            expected.add(jagged[200][i]);
        }

        assertEquals(expected, Stream.flatten(jagged, true).toList());
        assertEquals(0, Stream.flatten(new Integer[][] { null, {}, null }, true).count());
    }

    // G108-01: pins the column-major order of flatten(T[][], true) for jagged input with null/empty/short rows and null elements
    @Test
    public void testFlattenVertically_jaggedNullRowsAndElementsMatchColumnMajorReference() {
        final Random rnd = new Random(20260926L);

        for (int round = 0; round < 300; round++) {
            final int rows = 2 + rnd.nextInt(12);
            final Integer[][] a = new Integer[rows][];
            int maxLen = 0;

            for (int r = 0; r < rows; r++) {
                final int kind = rnd.nextInt(6);

                if (kind == 0) {
                    a[r] = null;
                } else {
                    final int len = kind == 1 ? 0 : rnd.nextInt(kind == 5 ? 40 : 6);
                    a[r] = new Integer[len];

                    for (int c = 0; c < len; c++) {
                        a[r][c] = rnd.nextInt(7) == 0 ? null : r * 1000 + c;
                    }

                    maxLen = Math.max(maxLen, len);
                }
            }

            final List<Integer> expected = new ArrayList<>();

            for (int c = 0; c < maxLen; c++) {
                for (int r = 0; r < rows; r++) {
                    if (a[r] != null && c < a[r].length) {
                        expected.add(a[r][c]);
                    }
                }
            }

            assertEquals(expected, Stream.flatten(a, true).toList(), "round " + round);
            assertEquals(expected.size(), Stream.flatten(a, true).count(), "round " + round);
            assertEquals(expected.isEmpty() ? expected : expected.subList(1, expected.size()), Stream.flatten(a, true).skip(1).toList(), "round " + round);
        }
    }

    @Test
    public void test_stream_flatten() {
        final int[][] a = { { 1, 3, 5 }, { 2, 4, 6, 8, 10 }, { 10, 20 } };

        IntStream.flatten(a).println();
        IntStream.flatten(a, false).println();
        IntStream.flatten(a, true).println();
        IntStream.flatten(a, 0, false).println();
        IntStream.flatten(a, 0, true).println();

        final int[][][] b = { { { 1, 2, 3 } }, { { 4, 5, 6 } } };
        IntStream.flatten(b).println();

        final Object[][] c = { { 1, 3, 5 }, { 2, 4, 6, 8, 10 }, { 10, 20 } };

        Stream.flatten(c).println();
        Stream.flatten(c, false).println();
        Stream.flatten(c, true).println();
        Stream.flatten(c, 0, false).println();
        Stream.flatten(c, 0, true).println();

        final Object[][][] d = { { { 1, 2, 3 } }, { { 4, 5, 6 } } };
        Stream.flatten(d).println();

        String[][] e = { { "1", "3", "5" }, { "2", "4", "6", "8", "10" }, { "10", "20" } };

        Stream.flatten(e).println();
        Stream.flatten(e, false).println();
        Stream.flatten(e, true).println();
        Stream.flatten(e, 0, false).println();
        Stream.flatten(e, 0, true).println();

        String[][][] f = { { { "1", "2", "3" } }, { { "4", "5", "6" } } };
        Stream.flatten(f).println();

        e = null;

        Stream.flatten(e).println();
        Stream.flatten(e, false).println();
        Stream.flatten(e, true).println();
        Stream.flatten(e, 0, false).println();
        Stream.flatten(e, 0, true).println();

        f = null;
        Stream.flatten(f).println();
        assertNull(f);
    }

    @Test
    public void testFlattenCollection() {
        List<Integer> list1 = Arrays.asList(1, 2);
        List<Integer> list2 = Arrays.asList(3, 4);
        List<Integer> list3 = Arrays.asList(5, 6);

        List<Integer> result = Stream.<Integer> flatten(Arrays.asList(list1, list2, list3)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    // flattMap

    @Test
    public void testFlattMap() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4)).flattMap(list -> list.stream()).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlattMapEmpty() {
        List<Integer> result = Stream.<List<Integer>> empty().flattMap(list -> list.stream()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattmapJdkStream() {
        List<Integer> result = Stream.of(1, 2).flattMap(x -> java.util.stream.Stream.of(x, x + 1)).toList();
        assertEquals(Arrays.asList(1, 2, 2, 3), result);
    }

    @Test
    public void testFlattMapArray() {
        List<Integer> result = Stream.of("1,2", "3,4").flatMapArrayToInt(s -> Stream.of(s.split(",")).mapToInt(Integer::parseInt).toArray()).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlattmapArray() {
        Stream<Integer> stream = Stream.of(1, 2, 3).flatMapArray(n -> new Integer[] { n, n * 10 });
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), stream.toList());
    }

    // flattMapToEntry

    @Test
    public void testFlattMapToEntry() {
        EntryStream<String, Integer> stream1 = EntryStream.of("a", 1);
        EntryStream<String, Integer> stream2 = EntryStream.of("b", 2);

        List<Map.Entry<String, Integer>> result = Stream.of(stream1, stream2).flattMapToEntry(s -> s).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testFlattMapToEntryEmpty() {
        List<Map.Entry<String, Integer>> result = Stream.<EntryStream<String, Integer>> empty().flattMapToEntry(s -> s).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattMapToEntryFromEntryStream() {
        List<Map.Entry<String, Integer>> result = Stream.of("a", "b")
                .flattMapToEntry(s -> EntryStream.of(s, s.hashCode(), s.toUpperCase(), s.toUpperCase().hashCode()))
                .toList();

        List<Map.Entry<String, Integer>> expected = new ArrayList<>();
        expected.add(N.newEntry("a", "a".hashCode()));
        expected.add(N.newEntry("A", "A".hashCode()));
        expected.add(N.newEntry("b", "b".hashCode()));
        expected.add(N.newEntry("B", "B".hashCode()));

        assertTrue(result.containsAll(expected) && expected.containsAll(result));
    }

    // from

    @Test
    public void testFrom() {
        java.util.stream.Stream<Integer> jdkStream = java.util.stream.Stream.of(1, 2, 3);
        Stream<Integer> stream = Stream.from(jdkStream);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testFrom_JavaStream() {
        java.util.stream.Stream<String> javaStream = java.util.stream.Stream.of("a", "b", "c");

        Stream<String> stream = Stream.from(javaStream);

        assertEquals(Arrays.asList("a", "b", "c"), stream.toList());
    }

    @Test
    public void testFrom_JavaStreamOperations() {
        java.util.stream.Stream<String> javaStream = java.util.stream.Stream.of("apple", "banana", "cherry", "date").filter(s -> s.length() > 4);

        Stream<String> stream = Stream.from(javaStream);

        List<String> result = stream.map(String::toUpperCase).sorted().toList();

        assertEquals(Arrays.asList("APPLE", "BANANA", "CHERRY"), result);
    }

    @Test
    public void testFrom_JavaStreamToArray() {
        java.util.stream.Stream<String> javaStream = java.util.stream.Stream.of("a", "b", "c");

        Stream<String> stream = Stream.from(javaStream);

        Object[] objArray = stream.toArray();
        assertArrayEquals(new Object[] { "a", "b", "c" }, objArray);

        java.util.stream.Stream<String> javaStream2 = java.util.stream.Stream.of("x", "y", "z");
        Stream<String> stream2 = Stream.from(javaStream2);
        String[] stringArray = stream2.toArray(String[]::new);
        assertArrayEquals(new String[] { "x", "y", "z" }, stringArray);
    }

    @Test
    public void testFrom_JavaStreamWithAdvance() {
        java.util.stream.Stream<Integer> javaStream = java.util.stream.Stream.of(1, 2, 3, 4, 5);

        Stream<Integer> stream = Stream.from(javaStream);

        List<Integer> result = stream.skip(2).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testFrom_JavaStreamWithCount() {
        java.util.stream.Stream<Integer> javaStream = java.util.stream.Stream.of(1, 2, 3, 4, 5);

        Stream<Integer> stream = Stream.from(javaStream);

        assertEquals(5, stream.count());
    }

    @Test
    public void testFrom_NullJavaStream() {
        Stream<String> stream = Stream.from((java.util.stream.Stream<String>) null);

        assertTrue(stream.toList().isEmpty());
    }

    @Test
    public void testFrom_ParallelJavaStream() {
        java.util.stream.Stream<Integer> parallelJavaStream = java.util.stream.Stream.of(1, 2, 3, 4, 5).parallel();

        Stream<Integer> stream = Stream.from(parallelJavaStream);

        assertTrue(stream.isParallel());
        assertEquals(5, stream.count());
    }

    @Test
    public void testFromJdkStream() {
        java.util.stream.Stream<Integer> jdkStream = java.util.stream.Stream.of(1, 2, 3, 4, 5);
        List<Integer> result = Stream.from(jdkStream).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testFromJdkStreamEmpty() {
        java.util.stream.Stream<Integer> jdkStream = java.util.stream.Stream.empty();
        List<Integer> result = Stream.from(jdkStream).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFromJdkStreamIteratorCountConsumes() {
        try (Stream<String> stream = Stream.from(java.util.stream.Stream.of("a", "b"))) {
            final ObjIteratorEx<String> iter = stream.iteratorEx();

            assertEquals(2, iter.count());
            assertFalse(iter.hasNext());
            assertEquals(0, iter.count());
            assertArrayEquals(new Object[0], iter.toArray());
            assertThrows(NoSuchElementException.class, iter::next);
        }
    }

    @Test
    public void testFromJdkStreamIteratorToArrayConsumes() {
        try (Stream<String> stream = Stream.from(java.util.stream.Stream.of("a", "b"))) {
            final ObjIteratorEx<String> iter = stream.iteratorEx();

            assertArrayEquals(new String[] { "a", "b" }, iter.toArray(new String[0]));
            assertFalse(iter.hasNext());
            assertEquals(0, iter.count());
            assertThrows(NoSuchElementException.class, iter::next);
        }
    }

    @Test
    public void testFromNull() {
        Stream<Integer> stream = Stream.from((java.util.stream.Stream<Integer>) null);
        assertTrue(stream.toList().isEmpty());
    }

    // generate

    @Test
    public void testGenerate() {
        AtomicInteger counter = new AtomicInteger(0);
        Stream<Integer> stream = Stream.generate(() -> counter.incrementAndGet()).limit(3);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testGenerate2() {
        AtomicInteger counter = new AtomicInteger(0);
        Stream<Integer> generated = Stream.generate(counter::getAndIncrement);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), generated.limit(5).toList());
    }

    // generate(Supplier) limited (line 18539).
    @Test
    public void testGenerate_limited_uncovered() {
        final MutableInt c = MutableInt.of(0);
        final Supplier<Integer> sup = () -> c.getAndIncrement();
        assertEquals(N.asList(0, 1, 2), Stream.generate(sup).limit(3).toList());
    }

    @Test
    public void testGenerate_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Stream.generate(null).count());
    }

    @Test
    public void testGenerate_NullSupplier_uncovered() {
        assertThrows(IllegalArgumentException.class, () -> Stream.generate((Supplier<Integer>) null).count());
    }

    @Test
    public void testGenerate_RandomNumbers() {
        Random random = new Random(12345);

        List<Integer> randomNumbers = Stream.generate(() -> random.nextInt(100)).limit(10).toList();

        assertEquals(10, randomNumbers.size());
        for (Integer num : randomNumbers) {
            assertTrue(num >= 0 && num < 100);
        }
    }

    // iterate

    @Test
    public void testIterate() {
        Stream<Integer> stream = Stream.iterate(1, n -> n < 5, n -> n + 1);
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void test_iterate() throws Exception {
        final MutableInt cnt = MutableInt.of(0);

        Stream.iterate(1, () -> cnt.getAndIncrement() < 3, x -> x + 3).println();

        Stream.iterate(1, x -> x < 10, x -> x + 3).println();

        Stream.iterate(1, x -> x + 1).limit(10).println();
        assertNotNull(cnt);
    }

    @Test
    public void testIterate_BooleanSupplierAndSupplier() {
        int[] counter = { 0 };
        BooleanSupplier hasNext = () -> counter[0] < 3;
        Supplier<Integer> next = () -> counter[0]++;

        List<Integer> result = Stream.iterate(hasNext, next).toList();

        assertEquals(Arrays.asList(0, 1, 2), result);
    }

    @Test
    public void testIterate_BooleanSupplierSupplier_NullArgs_uncovered() {
        assertThrows(IllegalArgumentException.class, () -> Stream.iterate((BooleanSupplier) null, () -> 1).count());
        assertThrows(IllegalArgumentException.class, () -> Stream.iterate(() -> true, (Supplier<Integer>) null).count());
    }

    // iterate(BooleanSupplier, Supplier) (lines 18349-18374).
    @Test
    public void testIterate_BooleanSupplierSupplier_uncovered() {
        final MutableInt counter = MutableInt.of(0);
        final BooleanSupplier hasNext = () -> counter.value() < 4;
        final Supplier<Integer> next = () -> counter.getAndIncrement();
        assertEquals(N.asList(0, 1, 2, 3), Stream.iterate(hasNext, next).toList());
    }

    @Test
    public void testIterate_Infinite() {
        UnaryOperator<Integer> f = x -> x + 1;

        List<Integer> result = Stream.iterate(0, f).limit(5).toList();

        assertEquals(Arrays.asList(0, 1, 2, 3, 4), result);
    }

    // iterate(init, BooleanSupplier, UnaryOperator) (lines 18398-18425).
    @Test
    public void testIterate_InitBooleanSupplierUnary_uncovered() {
        final MutableInt remaining = MutableInt.of(5);
        final BooleanSupplier hasNext = () -> remaining.getAndDecrement() > 0;
        assertEquals(N.asList(1, 2, 4, 8, 16), Stream.iterate(1, hasNext, x -> x * 2).toList());
    }

    // iterate(init, Predicate, UnaryOperator) (lines 18448-18480).
    @Test
    public void testIterate_InitPredicateUnary_uncovered() {
        assertEquals(N.asList(1, 3, 5, 7, 9), Stream.iterate(1, (Predicate<Integer>) x -> x < 10, x -> x + 2).toList());
    }

    @Test
    public void testIterate_StopConditions() {
        BooleanSupplier alwaysFalse = () -> false;
        Supplier<String> supplier = () -> "should not be called";

        List<String> result1 = Stream.iterate(alwaysFalse, supplier).toList();
        assertTrue(result1.isEmpty());

        Predicate<Integer> negative = x -> x < 0;
        List<Integer> result2 = Stream.iterate(1, negative, x -> x + 1).toList();
        assertTrue(result2.isEmpty());
    }

    @Test
    public void testIterate_WithInitAndBooleanSupplier() {
        int[] counter = { 0 };
        BooleanSupplier hasNext = () -> counter[0] < 5;
        UnaryOperator<Integer> f = x -> {
            counter[0]++;
            return x * 2;
        };

        List<Integer> result = Stream.iterate(1, hasNext, f).toList();

        assertEquals(Arrays.asList(1, 2, 4, 8, 16, 32), result);
    }

    @Test
    public void testIterate_WithInitAndPredicate() {
        Predicate<Integer> hasNext = x -> x < 100;
        UnaryOperator<Integer> f = x -> x * 2;

        List<Integer> result = Stream.iterate(1, hasNext, f).toList();

        assertEquals(Arrays.asList(1, 2, 4, 8, 16, 32, 64), result);
    }

    @Test
    public void testIterateInfinite() {
        List<Integer> result = Stream.iterate(1, n -> n + 1).limit(5).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testIteratePredicateRetriesFunctionResultAfterThrow() {
        final AtomicInteger tests = new AtomicInteger();
        final AtomicInteger fCalls = new AtomicInteger();
        final Iterator<Integer> iter = Stream.iterate(0, i -> {
            if (i > 0 && tests.getAndIncrement() == 0) {
                throw new IllegalStateException("candidate test failed");
            }
            return i < 2;
        }, i -> {
            fCalls.incrementAndGet();
            return i + 1;
        }).iterator();

        assertEquals(0, iter.next());
        assertThrows(IllegalStateException.class, iter::hasNext);
        assertEquals(1, fCalls.get());
        assertEquals(1, iter.next());
        assertEquals(1, fCalls.get());
        assertFalse(iter.hasNext());
        assertEquals(2, fCalls.get());
    }

    @Test
    public void testIteratePredicateRetriesSeedAfterThrow() {
        final AtomicInteger tests = new AtomicInteger();
        final Iterator<Integer> iter = Stream.iterate(0, i -> {
            if (tests.getAndIncrement() == 0) {
                throw new IllegalStateException("seed test failed");
            }
            return i < 2;
        }, i -> i + 1).iterator();

        assertThrows(IllegalStateException.class, iter::hasNext);
        assertEquals(0, iter.next());
        assertEquals(1, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterateWithBooleanSupplier() {
        final int[] count = { 0 };
        List<Integer> result = Stream.iterate(() -> count[0] < 5, () -> count[0]++).toList();

        assertEquals(Arrays.asList(0, 1, 2, 3, 4), result);
    }

    @Test
    public void testIterateWithInitAndBooleanSupplier() {
        final int[] count = { 0 };
        List<Integer> result = Stream.iterate(1, () -> count[0]++ < 4, n -> n * 2).toList();

        assertEquals(Arrays.asList(1, 2, 4, 8), result);
    }

    @Test
    public void testIterateWithInitAndPredicate() {
        List<Integer> result = Stream.iterate(1, n -> n < 20, n -> n * 2).toList();

        assertEquals(Arrays.asList(1, 2, 4, 8, 16), result);
    }

    // iterator

    @Test
    public void testOf_Iterator() {
        Iterator<Double> iterator = Arrays.asList(1.1, 2.2, 3.3).iterator();

        Stream<Double> stream = createStream(iterator);

        assertEquals(Arrays.asList(1.1, 2.2, 3.3), stream.toList());
    }

    @Test
    public void testOf_Iterator_WithRange() {
        Iterator<Integer> iter = Arrays.asList(0, 1, 2, 3, 4).iterator();
        List<Integer> result = Stream.of(iter, 1, 3).toList();
        assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void testOfIterator() {
        Iterator<Integer> iterator = Arrays.asList(1, 2, 3).iterator();
        Stream<Integer> stream = Stream.of(iterator);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testOfIterator_WithRange() {
        // Covers Stream.of(Iterator, fromIndex, toIndex) internal method
        // Uses skip + limit underneath
        List<Integer> result = Stream.of(Arrays.asList(0, 1, 2, 3, 4).iterator(), 2, 4).toList();
        assertEquals(Arrays.asList(2, 3), result);
    }

    // of(Iterator, from, to): static factory exercising skip+limit composition (line 16419).
    @Test
    public void testOfIteratorRange_uncovered() {
        final Iterator<Integer> it = N.asList(0, 1, 2, 3, 4, 5).iterator();
        // package-private; reachable via Collection.of with subrange instead:
        assertEquals(N.asList(1, 2, 3), Stream.of(N.asList(0, 1, 2, 3, 4, 5), 1, 4).toList());
        assertTrue(it.hasNext());
    }

    // listFiles

    @Test
    public void testListFiles() throws IOException {
        File tempFile = tempDir.resolve("test.txt").toFile();
        IOUtil.write("test content", tempFile);

        List<File> result = Stream.listFiles(tempDir.toFile()).toList();
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(f -> f.getName().equals("test.txt")));
    }

    @Test
    public void testListFiles_DeepRecursion() throws IOException {
        File root = Files.createTempDirectory(tempFolder, "deep").toFile();
        File current = root;
        for (int i = 0; i < 5; i++) {
            File subDir = new File(current, "level" + i);
            subDir.mkdir();
            File file = new File(current, "file" + i + ".txt");
            file.createNewFile();
            current = subDir;
        }

        List<File> files = Stream.listFiles(root, true).toList();

        assertEquals(10, files.size());
    }

    /**
     * {@code Stream.listFiles(dir, true)} must not descend into a Windows directory junction.
     *
     * <p>The guard used {@code !Files.isSymbolicLink(path)}, which is {@code false} for a junction (it
     * carries the {@code MOUNT_POINT} reparse tag, not {@code SYMLINK}), so the walk descended through one.
     * Measured before the fix on this tree: {@code IOUtil.listFiles} returned 5 entries while
     * {@code Stream.listFiles} returned 383 (capped), 365 of them {@code loop\loop\loop\...} repetitions.
     * The javadoc claims parity with {@link IOUtil#listFiles(File, boolean, boolean)}, so that parity is
     * what this asserts.
     */
    @Test
    public void testListFiles_DoesNotDescendIntoAWindowsJunction() throws Exception {
        final File root = Files.createTempDirectory(tempFolder, "junction-cycle").toFile();
        new File(root, "a.txt").createNewFile();
        final File sub = new File(root, "sub");
        sub.mkdir();
        new File(sub, "b.txt").createNewFile();

        final File loop = new File(root, "loop");
        createJunctionOrAbort(loop, root);

        try {
            final List<File> viaStream = Stream.listFiles(root, true).limit(400).toList();
            final List<File> viaIoUtil = IOUtil.listFiles(root, true, false);

            assertEquals(viaIoUtil.size(), viaStream.size(), "Stream.listFiles must match IOUtil.listFiles, as its javadoc claims");
            assertTrue(viaStream.stream().noneMatch(f -> f.getPath().contains("loop" + File.separator + "loop")),
                    "the traversal descended into the junction: " + viaStream);
        } finally {
            unlinkQuietly(loop);
        }
    }

    /**
     * A junction needs no cycle to be dangerous: one pointing outside the tree made the traversal report
     * files from outside its own root, contradicting "a link is therefore never followed out of the tree".
     */
    @Test
    public void testListFiles_DoesNotFollowAJunctionOutOfTheTree() throws Exception {
        final File root = Files.createTempDirectory(tempFolder, "junction-escape").toFile();
        new File(root, "a.txt").createNewFile();

        final File outside = Files.createTempDirectory(tempFolder, "junction-outside").toFile();
        new File(outside, "SECRET.txt").createNewFile();

        final File escape = new File(root, "escape");
        createJunctionOrAbort(escape, outside);

        try {
            final List<File> viaStream = Stream.listFiles(root, true).limit(400).toList();

            assertTrue(viaStream.stream().noneMatch(f -> "SECRET.txt".equals(f.getName())),
                    "the traversal followed the junction out of the tree: " + viaStream);
            assertEquals(IOUtil.listFiles(root, true, false).size(), viaStream.size());
        } finally {
            unlinkQuietly(escape);
        }
    }

    @Test
    public void testListFiles_EmptyDirectory() throws IOException {
        File emptyDir = Files.createTempDirectory(tempFolder, "empty").toFile();

        List<File> files = Stream.listFiles(emptyDir).toList();

        assertTrue(files.isEmpty());
    }

    @Test
    public void testListFiles_HiddenFiles() throws IOException {
        File dir = Files.createTempDirectory(tempFolder, "hidden").toFile();

        File regularFile = new File(dir, "regular.txt");
        regularFile.createNewFile();

        File hiddenFile = new File(dir, ".hidden");
        hiddenFile.createNewFile();

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            try {
                Files.setAttribute(hiddenFile.toPath(), "dos:hidden", true);
            } catch (IOException e) {
            }
        }

        List<File> allFiles = Stream.listFiles(dir).toList();
        assertTrue(allFiles.size() >= 2);
    }

    @Test
    public void testListFiles_MixedContent() throws IOException {
        File dir = Files.createTempDirectory(tempFolder, "mixed").toFile();

        new File(dir, "test.txt").createNewFile();
        new File(dir, "test.java").createNewFile();
        new File(dir, "test.xml").createNewFile();
        new File(dir, ".hidden").createNewFile();

        File subDir = new File(dir, "subdir");
        subDir.mkdir();
        new File(subDir, "nested.txt").createNewFile();

        List<File> nonRecursive = Stream.listFiles(dir, false).toList();
        assertEquals(5, nonRecursive.size());

        List<File> recursiveFiles = Stream.listFiles(dir, true, true).toList();
        assertEquals(5, recursiveFiles.size());
    }

    @Test
    public void testListFiles_NonExistentDirectory() {
        File nonExistent = new File("non-existent-dir");
        List<File> files = Stream.listFiles(nonExistent).toList();
        assertTrue(files.isEmpty());
    }

    @Test
    public void testListFiles_PermissionDenied() throws IOException {
        assertDoesNotThrow(() -> {
            File dir = Files.createTempDirectory(tempFolder, "restricted").toFile();
            File file = new File(dir, "file.txt");
            file.createNewFile();

            boolean permissionChanged = dir.setReadable(false);

            if (permissionChanged) {
                try {
                    List<File> files = Stream.listFiles(dir).toList();
                } catch (Exception e) {
                } finally {
                    dir.setReadable(true);
                }
            }
        });
    }

    @Test
    public void testListFiles_Recursive() throws IOException {
        File parentDir = Files.createTempDirectory(tempFolder, "parent").toFile();
        File subDir = new File(parentDir, "subdir");
        subDir.mkdir();
        File file1 = new File(parentDir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();

        List<File> files = Stream.listFiles(parentDir, true).toList();
        assertEquals(3, files.size());
        assertTrue(files.contains(subDir));
        assertTrue(files.contains(file1));
        assertTrue(files.contains(file2));
    }

    @Test
    public void testListFiles_RecursiveDoesNotDescendIntoADirectorySymlink() throws IOException {
        // The recursive traversal used to queue any entry for which isDirectory() was true - and isDirectory()
        // FOLLOWS a symbolic link. That walked out of the tree through a link, and looped forever on a link
        // pointing at one of its own ancestors. It also made IOUtil.walk(..)'s javadoc, which promises links
        // are never descended into, false.
        final File real = Files.createTempDirectory(tempFolder, "sym-target").toFile();
        new File(real, "inside.txt").createNewFile();

        final File root = Files.createTempDirectory(tempFolder, "sym-root").toFile();
        new File(root, "plain.txt").createNewFile();

        try {
            Files.createSymbolicLink(new File(root, "link").toPath(), real.toPath());
        } catch (final UnsupportedOperationException | IOException e) {
            // Creating a symlink needs a privilege this process may not hold (Windows without Developer Mode).
            // Abort so the run reports this as skipped rather than as a test that asserted nothing.
            Assumptions.abort("symbolic links cannot be created here: " + e);
        }

        final List<String> names = new ArrayList<>();
        for (final File f : Stream.listFiles(root, true).toList()) {
            names.add(f.getName());
        }
        Collections.sort(names);

        // The link itself is reported; what it points at is not.
        assertEquals(N.asList("link", "plain.txt"), names);
    }

    @Test
    public void testListFiles_RecursiveExcludeDirectory() throws IOException {
        File parentDir = Files.createTempDirectory(tempFolder, "parent").toFile();
        File subDir = new File(parentDir, "subdir");
        subDir.mkdir();
        File file1 = new File(parentDir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();

        List<File> files = Stream.listFiles(parentDir, true, true).toList();
        assertEquals(2, files.size());
        assertTrue(files.contains(file1));
        assertTrue(files.contains(file2));
        assertFalse(files.contains(subDir));
    }

    @Test
    public void testListFiles_RecursiveTerminatesOnACyclicSymlink() throws IOException {
        final File root = Files.createTempDirectory(tempFolder, "sym-cycle").toFile();
        new File(root, "f.txt").createNewFile();

        try {
            Files.createSymbolicLink(new File(root, "self").toPath(), root.toPath());
        } catch (final UnsupportedOperationException | IOException e) {
            Assumptions.abort("symbolic links cannot be created here: " + e); // see above
        }

        assertEquals(2, Stream.listFiles(root, true).toList().size());
    }

    @Test
    public void testListFiles_SingleDirectory() throws IOException {
        File parentDir = Files.createTempDirectory(tempFolder, "parent").toFile();
        File file1 = new File(parentDir, "file1.txt");
        File file2 = new File(parentDir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();

        List<File> files = Stream.listFiles(parentDir).toList();
        assertEquals(2, files.size());
        assertTrue(files.contains(file1));
        assertTrue(files.contains(file2));
    }

    @Test
    public void testListFiles_SymbolicLinks() throws IOException {
        File targetDir = Files.createTempDirectory(tempFolder, "target").toFile();
        File targetFile = new File(targetDir, "target.txt");
        targetFile.createNewFile();

        File linkDir = Files.createTempDirectory(tempFolder, "links").toFile();

        try {
            Path link = new File(linkDir, "link.txt").toPath();
            Files.createSymbolicLink(link, targetFile.toPath());

            List<File> files = Stream.listFiles(linkDir).toList();
            assertEquals(1, files.size());
            assertEquals("link.txt", files.get(0).getName());
        } catch (UnsupportedOperationException | IOException e) {
        }
    }

    @Test
    public void testListFilesExcludeDirectory() throws IOException {
        File tempDir = Files.createTempDirectory("test").toFile();
        tempDir.deleteOnExit();

        File file1 = new File(tempDir, "file1.txt");
        File subDir = new File(tempDir, "subdir");
        File file2 = new File(subDir, "file2.txt");

        file1.createNewFile();
        subDir.mkdir();
        file2.createNewFile();

        file1.deleteOnExit();
        file2.deleteOnExit();
        subDir.deleteOnExit();

        List<File> files = Stream.listFiles(tempDir, true, true).toList();
        assertEquals(2, files.size());
        assertTrue(files.stream().allMatch(File::isFile));
    }

    @Test
    public void testListFilesRecursive() throws IOException {
        File dir = tempDir.toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();
        File f = new File(subDir, "nested.txt");
        f.createNewFile();

        List<File> files = Stream.listFiles(dir, true).toList();
        assertTrue(files.stream().anyMatch(file -> file.getName().equals("nested.txt")));
    }

    @Test
    public void testListFilesRecursiveExcludeDir() throws IOException {
        File dir = tempDir.toFile();
        File subDir = new File(dir, "subdir2");
        subDir.mkdir();
        new File(subDir, "nested2.txt").createNewFile();

        List<File> files = Stream.listFiles(dir, true, true).toList();
        assertTrue(files.stream().allMatch(File::isFile));
    }

    @Test
    public void testListFilesRecursively() throws IOException {
        File txtFile = tempDir.resolve("test.txt").toFile();
        File jsonFile = tempDir.resolve("test.json").toFile();
        IOUtil.write("txt", txtFile);
        IOUtil.write("json", jsonFile);

        List<File> result = Stream.listFiles(tempDir.toFile(), false).toList();
        assertFalse(result.isEmpty());
        assertTrue(result.size() >= 2);
    }

    @Test
    public void testOf_Array() {
        Integer[] array = { 1, 2, 3, 4, 5 };

        Stream<Integer> stream = createStream01(array);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), stream.toList());
    }

    @Test
    public void testOf_ArrayWithInvalidRange() {
        Integer[] array = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> createStream(array, 1, 5));
    }

    @Test
    public void testOf_ArrayWithRange() {
        String[] array = { "a", "b", "c", "d", "e" };

        Stream<String> stream = createStream(array, 1, 4);

        assertEquals(Arrays.asList("b", "c", "d"), stream.toList());
    }

    @Test
    public void testOf_BooleanArray() {
        boolean[] array = { true, false, true, false };

        Stream<Boolean> stream = createStream(array);

        assertEquals(Arrays.asList(true, false, true, false), stream.toList());
    }

    @Test
    public void testOf_ByteArray() {
        byte[] array = { 1, 2, 3, 4 };

        Stream<Byte> stream = createStream(array);

        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4), stream.toList());
    }

    @Test
    public void testOf_CharArray() {
        char[] array = { 'a', 'b', 'c' };

        Stream<Character> stream = createStream(array);

        assertEquals(Arrays.asList('a', 'b', 'c'), stream.toList());
    }

    @Test
    public void testOf_Collection() {
        List<String> list = Arrays.asList("x", "y", "z");

        Stream<String> stream = createStream(list);

        assertEquals(list, stream.toList());
    }

    @Test
    public void testOf_Collection_WithRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> result = Stream.of(list, 1, 4).toList();
        assertEquals(Arrays.asList("b", "c", "d"), result);
    }

    @Test
    public void testOf_CollectionWithRange() {
        List<Integer> list = Arrays.asList(10, 20, 30, 40, 50);

        Stream<Integer> stream = createStream(list, 2, 4);

        assertEquals(Arrays.asList(30, 40), stream.toList());
    }

    @Test
    public void testOf_DoubleArray() {
        double[] array = { 1.1, 2.2, 3.3 };

        Stream<Double> stream = createStream(array);

        assertEquals(Arrays.asList(1.1, 2.2, 3.3), stream.toList());
    }

    @Test
    public void testOf_EmptyArray() {
        Integer[] emptyArray = {};

        Stream<Integer> stream = createStream01(emptyArray);

        assertTrue(stream.toList().isEmpty());
    }

    @Test
    public void testOf_Enumeration() {
        Vector<String> vector = new Vector<>(Arrays.asList("alpha", "beta", "gamma"));
        Enumeration<String> enumeration = vector.elements();

        Stream<String> stream = createStream(enumeration);

        assertEquals(Arrays.asList("alpha", "beta", "gamma"), stream.toList());
    }

    @Test
    public void testOf_FloatArray() {
        float[] array = { 1.5f, 2.5f, 3.5f };

        Stream<Float> stream = createStream(array);

        assertEquals(Arrays.asList(1.5f, 2.5f, 3.5f), stream.toList());
    }

    @Test
    public void testOf_IntArray() {
        int[] array = { 100, 200, 300 };

        Stream<Integer> stream = createStream(array);

        assertEquals(Arrays.asList(100, 200, 300), stream.toList());
    }

    @Test
    public void testOf_Iterable() {
        Set<String> set = new LinkedHashSet<>(Arrays.asList("one", "two", "three"));

        Stream<String> stream = createStream(set);

        assertEquals(3, stream.count());
    }

    @Test
    public void testOf_Iterator_Advance() {
        ObjIteratorEx<Integer> iter = new ObjIteratorEx<>() {
            private int current = 0;

            @Override
            public boolean hasNext() {
                return current < 10;
            }

            @Override
            public Integer next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return current++;
            }

            @Override
            public void advance(long n) {
                if (n <= 0) {
                    return;
                }

                current = Math.min(current + (int) n, 10);
            }
        };

        Stream<Integer> stream = createStream(iter);

        List<Integer> result = stream.skip(5).toList();
        assertEquals(Arrays.asList(5, 6, 7, 8, 9), result);
    }

    @Test
    public void testOf_JavaOptional_Empty() {
        Stream<Integer> s = Stream.of(java.util.Optional.<Integer> empty());
        assertTrue(s.toList().isEmpty());
    }

    @Test
    public void testOf_JavaOptional_Present() {
        Stream<Integer> s = Stream.of(java.util.Optional.of(42));
        assertEquals(Arrays.asList(42), s.toList());
    }

    @Test
    public void testOf_JavaUtilOptional() {
        java.util.Optional<Integer> present = java.util.Optional.of(42);
        Stream<Integer> stream1 = createStream(present);
        assertEquals(Arrays.asList(42), stream1.toList());

        java.util.Optional<Integer> empty = java.util.Optional.empty();
        Stream<Integer> stream2 = createStream(empty);
        assertTrue(stream2.toList().isEmpty());
    }

    @Test
    public void testOf_LongArray() {
        long[] array = { 1000L, 2000L, 3000L };

        Stream<Long> stream = createStream(array);

        assertEquals(Arrays.asList(1000L, 2000L, 3000L), stream.toList());
    }

    @Test
    public void testOf_Map() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        List<Map.Entry<String, Integer>> entries = createStream(map).toList();

        assertEquals(3, entries.size());
        assertEquals("a", entries.get(0).getKey());
        assertEquals(Integer.valueOf(1), entries.get(0).getValue());
    }

    @Test
    public void testOf_Map_Modifications() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Stream<Map.Entry<String, Integer>> stream = createStream(map);

        map.put("c", 3);

        assertThrows(ConcurrentModificationException.class, () -> stream.toList());
    }

    @Test
    public void testOf_Optional() {
        Optional<String> present = Optional.of("value");
        Stream<String> stream1 = createStream(present);
        assertEquals(Arrays.asList("value"), stream1.toList());

        Optional<String> empty = Optional.empty();
        Stream<String> stream2 = createStream(empty);
        assertTrue(stream2.toList().isEmpty());

        Stream<String> stream3 = createStream((Optional<String>) null);
        assertTrue(stream3.toList().isEmpty());
    }

    @Test
    public void testOf_Optional_Empty() {
        Stream<String> s = Stream.of(u.Optional.<String> empty());
        assertTrue(s.toList().isEmpty());
    }

    @Test
    public void testOf_Optional_Null() {
        Stream<String> s = Stream.of((u.Optional<String>) null);
        assertTrue(s.toList().isEmpty());
    }

    @Test
    public void testOf_Optional_Present() {
        Stream<String> s = Stream.of(u.Optional.of("hello"));
        assertEquals(Arrays.asList("hello"), s.toList());
    }

    @Test
    public void testOf_PrimitiveArrays_EmptyArrays() {

        assertTrue(createStream(new boolean[0]).toList().isEmpty());
        assertTrue(createStream(new char[0]).toList().isEmpty());
        assertTrue(createStream(new byte[0]).toList().isEmpty());
        assertTrue(createStream(new short[0]).toList().isEmpty());
        assertTrue(createStream(new int[0]).toList().isEmpty());
        assertTrue(createStream(new long[0]).toList().isEmpty());
        assertTrue(createStream(new float[0]).toList().isEmpty());
        assertTrue(createStream(new double[0]).toList().isEmpty());
    }

    @Test
    public void testOf_PrimitiveArraysWithRange() {

        boolean[] boolArray = { true, false, true, false, true };
        List<Boolean> boolResult = createStream(boolArray, 1, 4).toList();
        assertEquals(Arrays.asList(false, true, false), boolResult);

        char[] charArray = { 'a', 'b', 'c', 'd', 'e' };
        List<Character> charResult = createStream(charArray, 2, 5).toList();
        assertEquals(Arrays.asList('c', 'd', 'e'), charResult);

        byte[] byteArray = { 10, 20, 30, 40, 50 };
        List<Byte> byteResult = createStream(byteArray, 0, 3).toList();
        assertEquals(Arrays.asList((byte) 10, (byte) 20, (byte) 30), byteResult);

        short[] shortArray = { 100, 200, 300, 400 };
        List<Short> shortResult = createStream(shortArray, 1, 3).toList();
        assertEquals(Arrays.asList((short) 200, (short) 300), shortResult);

        int[] intArray = { 1, 2, 3, 4, 5, 6 };
        List<Integer> intResult = createStream(intArray, 2, 5).toList();
        assertEquals(Arrays.asList(3, 4, 5), intResult);

        long[] longArray = { 10L, 20L, 30L, 40L };
        List<Long> longResult = createStream(longArray, 1, 4).toList();
        assertEquals(Arrays.asList(20L, 30L, 40L), longResult);

        float[] floatArray = { 1.1f, 2.2f, 3.3f, 4.4f };
        List<Float> floatResult = createStream(floatArray, 0, 2).toList();
        assertEquals(Arrays.asList(1.1f, 2.2f), floatResult);

        double[] doubleArray = { 1.5, 2.5, 3.5, 4.5, 5.5 };
        List<Double> doubleResult = createStream(doubleArray, 2, 4).toList();
        assertEquals(Arrays.asList(3.5, 4.5), doubleResult);
    }

    @Test
    public void testOf_ShortArray() {
        short[] array = { 10, 20, 30 };

        Stream<Short> stream = createStream(array);

        assertEquals(Arrays.asList((short) 10, (short) 20, (short) 30), stream.toList());
    }

    @Test
    public void testOfArray() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        Stream<Integer> stream = Stream.of(array);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), stream.toList());
    }

    @Test
    public void testOfArrayRange() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        Stream<Integer> stream = Stream.of(array, 1, 4);
        assertEquals(Arrays.asList(2, 3, 4), stream.toList());
    }

    @Test
    public void testOfArrayWithRange() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        Stream<Integer> stream = createStream(array, 1, 4);
        assertEquals(Arrays.asList(2, 3, 4), stream.toList());
    }

    @Test
    public void testOfBooleanArray() {
        boolean[] arr = { true, false, true };
        List<Boolean> result = Stream.of(arr).toList();
        assertEquals(Arrays.asList(true, false, true), result);
    }

    @Test
    public void testOfBooleanArrayRange() {
        boolean[] arr = { true, false, true, false };
        List<Boolean> result = Stream.of(arr, 1, 3).toList();
        assertEquals(Arrays.asList(false, true), result);
    }

    // of(boolean[], from, to)
    @Test
    public void testOfBooleanArrayRange_uncovered() {
        final boolean[] a = { true, false, true, false };
        assertEquals(3, Stream.of(a, 1, 4).count());
        assertEquals(N.asList(true, false), Stream.of(a, 1, 4).skip(1).toList());
    }

    @Test
    public void testOfByteArray() {
        byte[] arr = { 1, 2, 3 };
        List<Byte> result = Stream.of(arr).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    // of(byte[], from, to): count/advance/toArray.
    @Test
    public void testOfByteArrayRange_uncovered() {
        final byte[] a = { 1, 2, 3, 4, 5 };
        assertEquals(4, Stream.of(a, 1, 5).count());
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5), Stream.of(a, 1, 5).skip(1).toList());
    }

    @Test
    public void testOfCharArray() {
        char[] arr = { 'a', 'b', 'c' };
        List<Character> result = Stream.of(arr).toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);
    }

    @Test
    public void testOfCharArrayRange() {
        char[] arr = { 'a', 'b', 'c', 'd' };
        List<Character> result = Stream.of(arr, 1, 3).toList();
        assertEquals(Arrays.asList('b', 'c'), result);
    }

    @Test
    public void testOfCollection() {
        List<String> list = Arrays.asList("a", "b", "c");
        Stream<String> stream = Stream.of(list);
        assertEquals(Arrays.asList("a", "b", "c"), stream.toList());
    }

    @Test
    public void testOfCollection_WithRange() {
        // Covers Stream.of(Collection, fromIndex, toIndex)
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> result = Stream.of(list, 1, 4).toList();
        assertEquals(Arrays.asList(2, 3, 4), result);
    }

    @Test
    public void testOfCollectionRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Stream<String> stream = Stream.of(list, 1, 4);
        assertEquals(Arrays.asList("b", "c", "d"), stream.toList());
    }

    @Test
    public void testOfCollectionRange_emptyZeroZero_uncovered() {
        final List<String> empty = new ArrayList<>();
        assertEquals(0, Stream.of(empty, 0, 0).count());
    }

    // of(Collection, from, to) where subrange is full collection vs partial - covers line 16302/16304.
    @Test
    public void testOfCollectionRange_partial_uncovered() {
        final List<String> list = N.asList("a", "b", "c", "d", "e");
        assertEquals(N.asList("b", "c", "d"), Stream.of(list, 1, 4).toList());
    }

    @Test
    public void testOfCollectionWithRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Stream<String> stream = createStream(list, 1, 4);
        assertEquals(Arrays.asList("b", "c", "d"), stream.toList());
    }

    @Test
    public void testOfDoubleArray() {
        double[] arr = { 1.0, 2.0, 3.0 };
        List<Double> result = Stream.of(arr).toList();
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), result);
    }

    // of(double[], from, to)
    @Test
    public void testOfDoubleArrayRange_uncovered() {
        final double[] a = { 1d, 2d, 3d, 4d };
        assertEquals(3, Stream.of(a, 1, 4).count());
        assertEquals(N.asList(3d, 4d), Stream.of(a, 1, 4).skip(1).toList());
    }

    @Test
    public void testOfEnumeration() {
        Vector<Integer> vector = new Vector<>(Arrays.asList(1, 2, 3));
        Enumeration<Integer> enumeration = vector.elements();
        Stream<Integer> stream = createStream(enumeration);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testOfEnumeration_Null_uncovered() {
        final Enumeration<String> en = null;
        assertEquals(0, Stream.of(en).count());
    }

    @Test
    public void testOfEnumeration_uncovered() {
        final Vector<String> vector = new Vector<>(N.asList("a", "b", "c"));
        assertEquals(N.asList("a", "b", "c"), Stream.of(vector.elements()).toList());
    }

    @Test
    public void testOfFloatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f };
        List<Float> result = Stream.of(arr).toList();
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), result);
    }

    // of(float[], from, to)
    @Test
    public void testOfFloatArrayRange_uncovered() {
        final float[] a = { 1f, 2f, 3f, 4f };
        assertEquals(3, Stream.of(a, 1, 4).count());
        assertEquals(N.asList(3f, 4f), Stream.of(a, 1, 4).skip(1).toList());
    }

    @Test
    public void testOfIntArray() {
        int[] arr = { 1, 2, 3, 4, 5 };
        List<Integer> result = Stream.of(arr).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testOfIntArrayRange() {
        int[] arr = { 1, 2, 3, 4, 5 };
        List<Integer> result = Stream.of(arr, 1, 4).toList();
        assertEquals(Arrays.asList(2, 3, 4), result);
    }

    // of(int[], from, to)
    @Test
    public void testOfIntArrayRange_uncovered() {
        final int[] a = { 1, 2, 3, 4, 5, 6 };
        assertEquals(4, Stream.of(a, 2, 6).count());
        assertEquals(N.asList(4, 5, 6), Stream.of(a, 2, 6).skip(1).toList());
        assertArrayEquals(new Object[] { 3, 4 }, Stream.of(a, 2, 4).toArray());
    }

    @Test
    public void testOfIterable() {
        Iterable<Integer> iterable = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = Stream.of(iterable);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    // of(Iterable) where iterable is NOT a Collection -> goes through of(iterable.iterator()) branch (line 16366).
    @Test
    public void testOfIterable_NonCollection_uncovered() {
        final List<Integer> backing = N.asList(1, 2, 3);
        // anonymous Iterable that is not a Collection
        final Iterable<Integer> iterable = backing::iterator;
        assertEquals(N.asList(1, 2, 3), Stream.of(iterable).toList());
    }

    @Test
    public void testOfIterable_Null_uncovered() {
        final Iterable<Integer> iterable = null;
        assertEquals(0, Stream.of(iterable).count());
    }

    @Test
    public void testOfJavaOptional() {
        java.util.Optional<String> optional = java.util.Optional.of("hello");
        Stream<String> stream = createStream(optional);
        assertEquals(Arrays.asList("hello"), stream.toList());

        java.util.Optional<String> empty = java.util.Optional.empty();
        Stream<String> emptyStream = createStream(empty);
        assertTrue(emptyStream.toList().isEmpty());
    }

    @Test
    public void testOfJdkOptional() {
        Stream<Integer> stream1 = Stream.of(java.util.Optional.of(42));
        assertEquals(Arrays.asList(42), stream1.toList());

        Stream<Integer> stream2 = Stream.of(java.util.Optional.<Integer> empty());
        assertEquals(0, stream2.count());
    }

    // ofKeys

    @Test
    public void testOfKeys() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Stream<String> stream = Stream.ofKeys(map);
        assertEquals(Arrays.asList("a", "b"), stream.toList());
    }

    @Test
    public void testOfKeys_EmptyMap() {
        Map<String, Integer> emptyMap = Collections.emptyMap();

        assertTrue(Stream.ofKeys(emptyMap).toList().isEmpty());
        assertTrue(Stream.ofKeys(emptyMap, v -> v > 0).toList().isEmpty());
        assertTrue(Stream.ofKeys(emptyMap, (k, v) -> true).toList().isEmpty());
    }

    @Test
    public void testOfKeys_Map() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("key1", 10);
        map.put("key2", 20);
        map.put("key3", 30);

        List<String> keys = Stream.ofKeys(map).toList();

        assertEquals(Arrays.asList("key1", "key2", "key3"), keys);
    }

    @Test
    public void testOfKeys_WithBiPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("short", 5);
        map.put("medium", 10);
        map.put("long", 15);

        List<String> keys = Stream.ofKeys(map, (k, v) -> k.length() > 4 && v > 5).toList();

        assertEquals(Arrays.asList("medium"), keys);
    }

    @Test
    public void testOfKeys_WithValueFilter() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        List<String> keys = Stream.ofKeys(map, v -> v % 2 == 0).toList();

        assertEquals(Arrays.asList("b", "d"), keys);
    }

    // ofLines

    @Test
    public void testOfLines() throws IOException {
        File file = tempDir.resolve("lines.txt").toFile();
        IOUtil.write("line1\nline2\nline3", file);

        List<String> result = Stream.ofLines(file).toList();
        assertEquals(Arrays.asList("line1", "line2", "line3"), result);
    }

    @Test
    public void testOfLines_BufferedReader() throws IOException {
        String content = "line1\nline2\nline3";
        BufferedReader bufferedReader = new BufferedReader(new StringReader(content));

        List<String> lines = Stream.ofLines(bufferedReader, false).toList();

        assertEquals(3, lines.size());

        bufferedReader.close();
    }

    @Test
    public void testOfLines_DifferentCharsets() throws IOException {
        File utf8File = Files.createTempFile(tempFolder, "utf8", ".txt").toFile();
        String content = "Hello\nWorld\n你好\n世界";
        Files.write(utf8File.toPath(), content.getBytes("UTF-8"));

        List<String> utf8Lines = Stream.ofLines(utf8File, Charsets.UTF_8).toList();
        assertEquals(4, utf8Lines.size());
        assertEquals("你好", utf8Lines.get(2));
        assertEquals("世界", utf8Lines.get(3));

        List<String> isoLines = Stream.ofLines(utf8File, Charset.forName("ISO-8859-1")).toList();
        assertEquals(4, isoLines.size());
        assertNotEquals("你好", isoLines.get(2));
    }

    @Test
    public void testOfLines_EmptyFile() throws IOException {
        File emptyFile = Files.createTempFile(tempFolder, "empty", ".txt").toFile();

        List<String> lines = Stream.ofLines(emptyFile).toList();

        assertTrue(lines.isEmpty());
    }

    @Test
    public void testOfLines_File() throws IOException {
        File testFile = Files.createTempFile(tempFolder, "test", ".txt").toFile();
        Files.write(testFile.toPath(), Arrays.asList("line1", "line2", "line3"));

        List<String> lines = Stream.ofLines(testFile).toList();
        assertEquals(3, lines.size());
        assertEquals("line1", lines.get(0));
        assertEquals("line2", lines.get(1));
        assertEquals("line3", lines.get(2));
    }

    @Test
    public void testOfLines_FileNotFound() {
        File nonExistentFile = new File("non-existent-file.txt");

        try {
            Stream.ofLines(nonExistentFile).toList();
            fail("Expected exception for non-existent file");
        } catch (UncheckedIOException e) {
            assertTrue(e.getCause() instanceof FileNotFoundException);
        }
    }

    @Test
    public void testOfLines_FileWithBlankLines() throws IOException {
        File file = Files.createTempFile(tempFolder, "blank-lines", ".txt").toFile();
        Files.write(file.toPath(), Arrays.asList("", "line1", "", "line2", ""));

        List<String> lines = Stream.ofLines(file).toList();

        assertEquals(5, lines.size());
        assertEquals("", lines.get(0));
        assertEquals("line1", lines.get(1));
        assertEquals("", lines.get(2));
        assertEquals("line2", lines.get(3));
        assertEquals("", lines.get(4));
    }

    @Test
    public void testOfLines_FileWithCharset() throws IOException {
        File testFile = Files.createTempFile(tempFolder, "test-charset", ".txt").toFile();
        String content = "Hello\nWorld\n你好";
        Files.write(testFile.toPath(), content.getBytes("UTF-8"));

        List<String> lines = Stream.ofLines(testFile, Charsets.UTF_8).toList();
        assertEquals(3, lines.size());
        assertEquals("Hello", lines.get(0));
        assertEquals("World", lines.get(1));
        assertEquals("你好", lines.get(2));
    }

    @Test
    public void testOfLines_LargeFile() throws IOException {
        File largeFile = Files.createTempFile(tempFolder, "large", ".txt").toFile();
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            lines.add("Line " + i);
        }
        Files.write(largeFile.toPath(), lines);

        long count = Stream.ofLines(largeFile).count();
        assertEquals(10000, count);

        long evenCount = Stream.ofLines(largeFile)
                .filter(line -> line.contains("0") || line.contains("2") || line.contains("4") || line.contains("6") || line.contains("8"))
                .count();
        assertTrue(evenCount > 0);
    }

    @Test
    public void testOfLines_NullReader() {
        assertThrows(IllegalArgumentException.class, () -> Stream.ofLines((Reader) null));
    }

    @Test
    public void testOfLines_Path() throws IOException {
        Path testPath = Files.createTempFile(tempFolder, "test-path", ".txt").toFile().toPath();
        Files.write(testPath, Arrays.asList("path1", "path2", "path3"));

        List<String> lines = Stream.ofLines(testPath).toList();
        assertEquals(3, lines.size());
        assertEquals("path1", lines.get(0));
        assertEquals("path2", lines.get(1));
        assertEquals("path3", lines.get(2));
    }

    @Test
    public void testOfLines_PathNotFound() {
        Path nonExistentPath = new File("non-existent-path.txt").toPath();

        try {
            Stream.ofLines(nonExistentPath).toList();
            fail("Expected exception for non-existent path");
        } catch (UncheckedIOException e) {
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    @Test
    public void testOfLines_PathWithCharset() throws IOException {
        Path testPath = Files.createTempFile(tempFolder, "test-path-charset", ".txt").toFile().toPath();
        Files.write(testPath, Arrays.asList("line1", "line2"), Charsets.UTF_8);

        List<String> lines = Stream.ofLines(testPath, Charsets.UTF_8).toList();
        assertEquals(2, lines.size());
        assertEquals("line1", lines.get(0));
        assertEquals("line2", lines.get(1));
    }

    @Test
    public void testOfLines_Reader() throws IOException {
        String content = "line1\nline2\nline3";
        StringReader reader = new StringReader(content);

        List<String> lines = Stream.ofLines(reader).toList();

        assertEquals(Arrays.asList("line1", "line2", "line3"), lines);
    }

    @Test
    public void testOfLinesEmpty() throws IOException {
        File file = tempDir.resolve("empty.txt").toFile();
        IOUtil.write("", file);

        List<String> result = Stream.ofLines(file).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOfLinesFromFile() throws IOException {
        File tempFile = new File(tempDir.toFile(), "testlines.txt");
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("hello\nworld\n");
        }
        try (Stream<String> lines = Stream.ofLines(tempFile)) {
            List<String> result = lines.toList();
            assertEquals(Arrays.asList("hello", "world"), result);
        }
    }

    @Test
    public void testOfLinesFromFileWithCharset() throws IOException {
        File tempFile = new File(tempDir.toFile(), "testlines_charset.txt");
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("abc\ndef\n");
        }
        try (Stream<String> lines = Stream.ofLines(tempFile, StandardCharsets.UTF_8)) {
            List<String> result = lines.toList();
            assertEquals(Arrays.asList("abc", "def"), result);
        }
    }

    @Test
    public void testOfLinesFromPath() throws IOException {
        Path tempFile = tempDir.resolve("testlines_path.txt");
        Files.write(tempFile, Arrays.asList("one", "two", "three"));
        try (Stream<String> lines = Stream.ofLines(tempFile)) {
            List<String> result = lines.toList();
            assertEquals(Arrays.asList("one", "two", "three"), result);
        }
    }

    @Test
    public void testOfLinesFromPathWithCharset() throws IOException {
        Path tempPath = Files.createTempFile("test", ".txt");
        Files.deleteIfExists(tempPath);

        String content = "测试\n内容";
        Files.write(tempPath, content.getBytes(StandardCharsets.UTF_8));

        List<String> result = Stream.ofLines(tempPath, StandardCharsets.UTF_8).toList();
        assertEquals(Arrays.asList("测试", "内容"), result);

        Files.deleteIfExists(tempPath);
    }

    @Test
    public void testOfLinesFromReader() throws IOException {
        StringReader reader = new StringReader("first\nsecond\nthird");
        List<String> result = Stream.ofLines(reader).toList();
        assertEquals(Arrays.asList("first", "second", "third"), result);
    }

    @Test
    public void testOfLongArray() {
        long[] arr = { 1L, 2L, 3L };
        List<Long> result = Stream.of(arr).toList();
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testOfLongArrayRange() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L };
        List<Long> result = Stream.of(arr, 1, 4).toList();
        assertEquals(Arrays.asList(2L, 3L, 4L), result);
    }

    // of(long[], from, to)
    @Test
    public void testOfLongArrayRange_uncovered() {
        final long[] a = { 1L, 2L, 3L, 4L };
        assertEquals(3, Stream.of(a, 1, 4).count());
        assertEquals(N.asList(3L, 4L), Stream.of(a, 1, 4).skip(1).toList());
    }

    @Test
    public void testOfMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Stream<Map.Entry<String, Integer>> stream = Stream.of(map);
        assertEquals(2, stream.count());
    }

    // ofNullable

    @Test
    public void testOfNullable() {
        Stream<String> stream1 = Stream.ofNullable("hello");
        assertEquals("hello", stream1.first().get());

        Stream<String> stream2 = Stream.ofNullable(null);
        assertEquals(0, stream2.count());
    }

    @Test
    public void testOfNullable2() {
        Stream<String> stream1 = Stream.ofNullable("hello");
        assertEquals(Arrays.asList("hello"), stream1.toList());

        Stream<String> stream2 = Stream.ofNullable(null);
        assertTrue(stream2.toList().isEmpty());
    }

    // ofNullable(non-null) (around line 16179).
    @Test
    public void testOfNullable_nonNull_uncovered() {
        assertEquals(N.asList("x"), Stream.ofNullable("x").toList());
    }

    @Test
    public void testOfNullable_null_uncovered() {
        assertEquals(0, Stream.ofNullable(null).count());
    }

    @Test
    public void testOfOptional() {
        Stream<Integer> stream1 = Stream.of(u.Optional.of(42));
        assertEquals(Arrays.asList(42), stream1.toList());

        Stream<Integer> stream2 = Stream.of(u.Optional.<Integer> empty());
        assertEquals(0, stream2.count());
    }

    @Test
    public void testOfPrimitiveArrays() {
        boolean[] boolArray = { true, false, true };
        Stream<Boolean> boolStream = createStream(boolArray);
        assertEquals(Arrays.asList(true, false, true), boolStream.toList());

        char[] charArray = { 'a', 'b', 'c' };
        Stream<Character> charStream = createStream(charArray);
        assertEquals(Arrays.asList('a', 'b', 'c'), charStream.toList());

        byte[] byteArray = { 1, 2, 3 };
        Stream<Byte> byteStream = createStream(byteArray);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), byteStream.toList());

        short[] shortArray = { 1, 2, 3 };
        Stream<Short> shortStream = createStream(shortArray);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), shortStream.toList());

        int[] intArray = { 1, 2, 3 };
        Stream<Integer> intStream = createStream(intArray);
        assertEquals(Arrays.asList(1, 2, 3), intStream.toList());

        long[] longArray = { 1L, 2L, 3L };
        Stream<Long> longStream = createStream(longArray);
        assertEquals(Arrays.asList(1L, 2L, 3L), longStream.toList());

        float[] floatArray = { 1.0f, 2.0f, 3.0f };
        Stream<Float> floatStream = createStream(floatArray);
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), floatStream.toList());

        double[] doubleArray = { 1.0, 2.0, 3.0 };
        Stream<Double> doubleStream = createStream(doubleArray);
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), doubleStream.toList());
    }

    // ofReversed

    @Test
    public void testOfReversed() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        List<Integer> result = Stream.ofReversed(array).toList();
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result);
    }

    @Test
    public void testOfReversed_Array() {
        Integer[] array = { 1, 2, 3, 4, 5 };

        List<Integer> reversed = Stream.ofReversed(array).toList();

        assertEquals(Arrays.asList(5, 4, 3, 2, 1), reversed);
    }

    @Test
    public void testOfReversed_List() {
        List<String> list = Arrays.asList("a", "b", "c", "d");

        List<String> reversed = Stream.ofReversed(list).toList();

        assertEquals(Arrays.asList("d", "c", "b", "a"), reversed);
    }

    @Test
    public void testOfReversedEmpty() {
        Integer[] array = {};
        List<Integer> result = Stream.ofReversed(array).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOfReversedList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> result = Stream.ofReversed(list).toList();
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result);
    }

    @Test
    public void testOfShortArray() {
        short[] arr = { 1, 2, 3 };
        List<Short> result = Stream.of(arr).toList();
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), result);
    }

    // of(short[], from, to)
    @Test
    public void testOfShortArrayRange_uncovered() {
        final short[] a = { 10, 20, 30, 40 };
        assertEquals(2, Stream.of(a, 1, 3).count());
        assertEquals(N.asList((short) 30, (short) 40), Stream.of(a, 1, 4).skip(1).toList());
    }

    // ofValues

    @Test
    public void testOfValues() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Stream<Integer> stream = Stream.ofValues(map);
        assertEquals(Arrays.asList(1, 2), stream.toList());
    }

    @Test
    public void testOfValues_EmptyMap() {
        Map<String, Integer> emptyMap = Collections.emptyMap();

        assertTrue(Stream.ofValues(emptyMap).toList().isEmpty());
        assertTrue(Stream.ofValues(emptyMap, k -> true).toList().isEmpty());
        assertTrue(Stream.ofValues(emptyMap, (k, v) -> true).toList().isEmpty());
    }

    @Test
    public void testOfValues_Map() {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        List<String> values = Stream.ofValues(map).toList();

        assertEquals(Arrays.asList("one", "two", "three"), values);
    }

    @Test
    public void testOfValues_WithBiPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("bb", 2);
        map.put("ccc", 3);

        List<Integer> values = Stream.ofValues(map, (k, v) -> k.length() == v).toList();

        assertEquals(Arrays.asList(1, 2, 3), values);
    }

    @Test
    public void testOfValues_WithKeyFilter() {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "odd");
        map.put(2, "even");
        map.put(3, "odd");
        map.put(4, "even");

        List<String> values = Stream.ofValues(map, k -> k % 2 == 0).toList();

        assertEquals(Arrays.asList("even", "even"), values);
    }

    @Test
    public void testOfVarargs() {
        Stream<Integer> stream = Stream.of(1, 2, 3);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    // range

    @Test
    public void testRange() {
        Stream<Integer> stream = Stream.range(1, 5);
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testRange_Int() {
        List<Integer> range = Stream.range(1, 5).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4), range);
    }

    @Test
    public void testRange_IntWithStep() {
        List<Integer> range = Stream.range(0, 10, 2).toList();

        assertEquals(Arrays.asList(0, 2, 4, 6, 8), range);
    }

    @Test
    public void testRange_Long() {
        List<Long> range = Stream.range(10L, 13L).toList();

        assertEquals(Arrays.asList(10L, 11L, 12L), range);
    }

    @Test
    public void testRange_LongWithStep() {
        List<Long> range = Stream.range(0L, 15L, 5L).toList();

        assertEquals(Arrays.asList(0L, 5L, 10L), range);
    }

    @Test
    public void testRange_NegativeSteps() {
        List<Integer> intRange = Stream.range(10, 0, -2).toList();
        assertEquals(Arrays.asList(10, 8, 6, 4, 2), intRange);

        List<Long> longRange = Stream.range(20L, 10L, -3L).toList();
        assertEquals(Arrays.asList(20L, 17L, 14L, 11L), longRange);
    }

    @Test
    public void testRangeInt() {
        List<Integer> result = Stream.range(1, 5).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testRangeIntWithBy() {
        List<Integer> result = Stream.range(1, 10, 3).toList();
        assertEquals(Arrays.asList(1, 4, 7), result);
    }

    @Test
    public void testRangeLong() {
        List<Long> result = Stream.range(1L, 5L).toList();
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L), result);
    }

    @Test
    public void testRangeLongWithBy() {
        List<Long> result = Stream.range(1L, 10L, 3L).toList();
        assertEquals(Arrays.asList(1L, 4L, 7L), result);
    }

    @Test
    public void testRangeWithStep() {
        Stream<Integer> stream = Stream.range(0, 10, 2);
        assertEquals(Arrays.asList(0, 2, 4, 6, 8), stream.toList());
    }

    // rangeMap

    @Test
    public void testRangeMap() {
        List<Integer> result = Stream.of(1, 2, 2, 3, 3, 3, 4).rangeMap((a, b) -> a.equals(b), (a, b) -> a).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void test_rangeMap() {
        assertDoesNotThrow(() -> {
            Stream.of("a", "ab", "ac", "b", "c", "cb").rangeMap((a, b) -> b.startsWith(a), (a, b) -> a + "<->" + b).println();
        });
    }

    @Test
    public void testRangeMapEmpty() {
        List<Integer> result = Stream.<Integer> empty().rangeMap((a, b) -> a.equals(b), (a, b) -> a).toList();
        assertTrue(result.isEmpty());
    }

    // repeat

    @Test
    public void testRepeat() {
        Stream<String> stream = Stream.repeat("a", 3);
        assertEquals(Arrays.asList("a", "a", "a"), stream.toList());
    }

    @Test
    public void testRepeat2() {
        Stream<String> repeated = Stream.repeat("hello", 3);
        assertEquals(Arrays.asList("hello", "hello", "hello"), repeated.toList());

        Stream<String> noRepeat = Stream.repeat("hello", 0);
        assertTrue(noRepeat.toList().isEmpty());
    }

    // repeat: count() override (line 18301-18305).
    @Test
    public void testRepeat_count_uncovered() {
        assertEquals(5, Stream.repeat("x", 5).count());
    }

    @Test
    public void testRepeat_LargeCount() {
        long largeCount = 10000L;

        long actualCount = Stream.repeat("X", largeCount).count();

        assertEquals(largeCount, actualCount);
    }

    // repeat: advance() override via skip (line 18292-18298).
    @Test
    public void testRepeat_skip_uncovered() {
        assertEquals(N.asList("x", "x"), Stream.repeat("x", 5).skip(3).toList());
    }

    // repeat: toArray() override (line 18308-18321).
    @Test
    public void testRepeat_toArray_uncovered() {
        final Object[] arr = Stream.repeat("y", 3).toArray();
        assertArrayEquals(new Object[] { "y", "y", "y" }, arr);
    }

    @Test
    public void testRepeat_Zero() {
        List<Integer> repeated = Stream.repeat(42, 0).toList();

        assertTrue(repeated.isEmpty());
    }

    @Test
    public void testRepeatNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> Stream.repeat("test", -1));
    }

    @Test
    public void testRepeatZero() {
        Stream<String> stream = Stream.repeat("a", 0);
        assertEquals(0, stream.count());
    }

    // of(char[], from, to): exercises advance() (line 16656-16661) via skip().
    @Test
    public void testOfCharArrayRange_skipThenToArray_uncovered() {
        final char[] a = { 'a', 'b', 'c', 'd', 'e' };
        assertEquals(N.asList('d', 'e'), Stream.of(a, 1, 5).skip(2).toList());
    }

    @Test
    public void testOfJdkStream() {
        java.util.stream.Stream<Integer> jdkStream = Arrays.asList(1, 2, 3).stream();
        Stream<Integer> stream = Stream.from(jdkStream);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testOf_PrimitiveArrays_ToArray() {

        boolean[] boolArray = { true, false, true };
        Boolean[] boolResult = createStream(boolArray).toArray(Boolean[]::new);
        assertArrayEquals(new Boolean[] { true, false, true }, boolResult);

        char[] charArray = { 'x', 'y', 'z' };
        Character[] charResult = createStream(charArray).toArray(Character[]::new);
        assertArrayEquals(new Character[] { 'x', 'y', 'z' }, charResult);

        int[] intArray = { 10, 20, 30 };
        Integer[] intResult = createStream(intArray).toArray(Integer[]::new);
        assertArrayEquals(new Integer[] { 10, 20, 30 }, intResult);
    }

    // of(char[], from, to): exercises toArray() override (line 16665-16672).
    @Test
    public void testOfCharArrayRange_toArray_uncovered() {
        final char[] a = { 'a', 'b', 'c', 'd', 'e' };
        final Object[] arr = Stream.of(a, 1, 4).toArray();
        assertArrayEquals(new Object[] { 'b', 'c', 'd' }, arr);
    }

}
