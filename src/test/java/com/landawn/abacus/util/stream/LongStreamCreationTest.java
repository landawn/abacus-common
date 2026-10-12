package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.LongSupplier;
import com.landawn.abacus.util.u.OptionalLong;

/** LongStream creation contracts. */
public class LongStreamCreationTest extends LongStreamTestSupport {

    // defer

    @Test
    public void testDefer() {
        AtomicLong counter = new AtomicLong(0);
        Supplier<LongStream> supplier = () -> {
            counter.incrementAndGet();
            return LongStream.of(1L, 2L, 3L);
        };

        LongStream stream = LongStream.defer(supplier);
        assertEquals(0, counter.get());

        long[] result = stream.toArray();
        assertEquals(1, counter.get());
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testDefer_supplierReturningNullProducesEmptyStream() {
        final AtomicInteger supplierCalls = new AtomicInteger();
        final LongStream deferred = LongStream.defer(() -> {
            supplierCalls.incrementAndGet();
            return null;
        });

        assertEquals(0, deferred.count());
        assertEquals(1, supplierCalls.get());
    }

    @Test
    public void testDeferRejectsNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.defer(null));
    }

    @Test
    public void testEmpty() {
        LongStream stream = LongStream.empty();
        assertFalse(stream.iterator().hasNext());
        assertEquals(0, stream.count());
    }

    @Test
    public void testEmptyStreamOperations() {
        LongStream stream1 = LongStream.empty();
        assertFalse(stream1.findFirst().isPresent());

        LongStream stream2 = LongStream.empty();
        assertFalse(stream2.min().isPresent());

        LongStream stream3 = LongStream.empty();
        assertFalse(stream3.max().isPresent());

        LongStream stream4 = LongStream.empty();
        assertEquals(0L, stream4.sum());
    }

    // flatten

    @Test
    public void testFlatten() {
        long[][] twoDimArray = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, LongStream.flatten(twoDimArray).toArray());
    }

    @Test
    public void testFlatten2D() {
        long[][] array = { { 1L, 2L }, { 3L, 4L }, { 5L } };
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, LongStream.flatten(array).toArray());
    }

    @Test
    @DisplayName("flatten() should flatten two-dimensional array")
    public void testFlatten2D_2() {
        long[][] array = { { 1, 2 }, { 3, 4 } };
        long[] result = LongStream.flatten(array, false).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4 }, result);

        result = LongStream.flatten(array, true).toArray();
        assertArrayEquals(new long[] { 1, 3, 2, 4 }, result);
    }

    @Test
    @DisplayName("flatten() should flatten two-dimensional array")
    public void testFlatten2D_3() {
        long[][] array = { { 1, 2 }, { 3, 4, 5 } };
        long[] result = LongStream.flatten(array, 0, false).toArray();
        assertArrayEquals(new long[] { 1, 2, 0, 3, 4, 5 }, result);

        result = LongStream.flatten(array, 0, true).toArray();
        assertArrayEquals(new long[] { 1, 3, 2, 4, 0, 5 }, result);
    }

    @Test
    public void testFlatten2D_Empty() {
        assertEquals(0L, LongStream.flatten(new long[0][], true).count());
        assertEquals(0L, LongStream.flatten(new long[0][], false).count());
    }

    @Test
    public void testFlatten2D_Horizontally() {
        long[][] a = { { 1L, 2L }, { 3L, 4L } };
        long[] result = LongStream.flatten(a, false).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testFlatten2D_SingleRow() {
        long[][] a = { { 7L, 8L, 9L } };
        long[] result = LongStream.flatten(a, true).toArray();
        assertArrayEquals(new long[] { 7L, 8L, 9L }, result);
    }

    @Test
    public void testFlatten2D_Vertically() {
        // flatten(long[][], boolean) - vertically
        long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
        long[] result = LongStream.flatten(a, true).toArray();
        // vertical: col-by-col => 1,4,2,5,3,6
        assertArrayEquals(new long[] { 1L, 4L, 2L, 5L, 3L, 6L }, result);
    }

    @Test
    public void testFlatten2D_WithAlignment_PadsShortRows() {
        // flatten(long[][], long, boolean) - horizontally with padding
        long[][] a = { { 1L, 2L, 3L }, { 4L, 5L } };
        long[] result = LongStream.flatten(a, 0L, false).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 0L }, result);
    }

    @Test
    public void testFlatten2D_WithAlignment_Vertically() {
        long[][] a = { { 1L, 2L }, { 3L, 4L, 5L } };
        long[] result = LongStream.flatten(a, -1L, true).toArray();
        // col 0: 1,3; col 1: 2,4; col 2: -1,5
        assertArrayEquals(new long[] { 1L, 3L, 2L, 4L, -1L, 5L }, result);
    }

    @Test
    public void testFlatten2DHorizontally() {
        long[][] array = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, LongStream.flatten(array, false).toArray());
    }

    @Test
    public void testFlatten2DVertically() {
        long[][] array = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
        assertArrayEquals(new long[] { 1L, 4L, 2L, 5L, 3L, 6L }, LongStream.flatten(array, true).toArray());
    }

    @Test
    public void testFlatten2DWithAlignment() {
        long[][] array = { { 1L, 2L }, { 3L, 4L, 5L } };
        assertArrayEquals(new long[] { 1L, 3L, 2L, 4L, 0L, 5L }, LongStream.flatten(array, 0L, true).toArray());
    }

    @Test
    public void testFlatten3D() {
        long[][][] array = { { { 1L, 2L }, { 3L, 4L } }, { { 5L, 6L }, { 7L, 8L } } };
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L }, LongStream.flatten(array).toArray());
    }

    @Test
    public void testFlatten3D_Basic() {
        long[][][] a = { { { 1L, 2L }, { 3L, 4L } }, { { 5L, 6L } } };
        long[] result = LongStream.flatten(a).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, result);
    }

    @Test
    public void testFlatten3D_Empty() {
        assertEquals(0L, LongStream.flatten(new long[0][][]).count());
    }

    @Test
    public void testFlatten_3D() {
        long[][][] a = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 } } };
        long[] result = LongStream.flatten(a).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testFlatten_WithAlignment() {
        long[][] a = { { 1, 2, 3 }, { 4, 5 } };
        long[] result = LongStream.flatten(a, 0L, true).toArray();
        assertArrayEquals(new long[] { 1, 4, 2, 5, 3, 0 }, result);
    }

    @Test
    @DisplayName("flatten() with empty array should return empty stream")
    public void testFlattenEmpty() {
        long[][] array = {};
        LongStream stream = LongStream.flatten(array);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFlattenVertically() {
        long[][] arr = { { 1, 2, 3 }, { 4, 5 }, { 6 } };
        assertEquals(6, LongStream.flatten(arr, true).count());
        assertEquals(5, LongStream.flatten(arr, true).skip(1).count());
        assertArrayEquals(new long[] { 1, 4, 6, 2, 5, 3 }, LongStream.flatten(arr, true).toArray());
        assertArrayEquals(new long[] { 4, 6, 2, 5, 3 }, LongStream.flatten(arr, true).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 4L, 6L, 2L, 5L, 3L), LongStream.flatten(arr, true).toList());
        assertEquals(Arrays.asList(4L, 6L, 2L, 5L, 3L), LongStream.flatten(arr, true).skip(1).toList());
        assertEquals(6, LongStream.flatten(arr, true).map(e -> e).count());
        assertEquals(5, LongStream.flatten(arr, true).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 4, 6, 2, 5, 3 }, LongStream.flatten(arr, true).map(e -> e).toArray());
        assertArrayEquals(new long[] { 4, 6, 2, 5, 3 }, LongStream.flatten(arr, true).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 4L, 6L, 2L, 5L, 3L), LongStream.flatten(arr, true).map(e -> e).toList());
        assertEquals(Arrays.asList(4L, 6L, 2L, 5L, 3L), LongStream.flatten(arr, true).map(e -> e).skip(1).toList());
    }



    // G114-01: flatten(long[][], true) keeps the original walk for dense input and uses a compacted row walk when
    // rows * longest row > 4 * elements; pins order, count, skip and the iterator (incl. NoSuchElementException) on both walks.
    @Test
    public void testFlattenVertically_denseAndSparseWalksMatchColumnMajorReference() {
        final Random random = new Random(114);

        for (int round = 0; round < 400; round++) {
            // kind 0: dense jagged, 1: mostly null/empty rows plus a few long ones, 2: rectangular, 3: one long row among empty/null rows
            final int kind = round % 4;
            final int rows = 2 + random.nextInt(kind == 1 || kind == 3 ? 40 : 8);
            final int width = 1 + random.nextInt(6);
            final int longRow = random.nextInt(rows);
            final long[][] a = new long[rows][];

            for (int i = 0; i < rows; i++) {
                final int len;

                if (kind == 0) {
                    len = random.nextInt(7) - 1;
                } else if (kind == 1) {
                    len = random.nextInt(6) == 0 ? random.nextInt(40) : random.nextInt(3) - 1;
                } else if (kind == 2) {
                    len = width;
                } else {
                    len = i == longRow ? 1 + random.nextInt(60) : random.nextInt(2) - 1;
                }

                if (len >= 0) {
                    a[i] = new long[len];

                    for (int j = 0; j < len; j++) {
                        a[i][j] = i * 100 + j;
                    }
                }
            }

            assertFlattenVerticallyMatchesReferenceG114(a, random.nextInt(rows * 3 + 2));
        }

        // rows * longest row == 4 * elements keeps the original walk; one more empty row switches to the compacted walk
        final long[][] boundary = { { 1, 2, 3, 4 }, null, {}, { 5, 6, 7, 8 }, null, {}, {}, {} };
        final long[][] boundaryPlusOne = { { 1, 2, 3, 4 }, null, {}, { 5, 6, 7, 8 }, null, {}, {}, {}, {} };
        assertArrayEquals(new long[] { 1, 5, 2, 6, 3, 7, 4, 8 }, LongStream.flatten(boundary, true).toArray());
        assertArrayEquals(new long[] { 1, 5, 2, 6, 3, 7, 4, 8 }, LongStream.flatten(boundaryPlusOne, true).toArray());

        for (int skip = 0; skip <= 9; skip++) {
            assertFlattenVerticallyMatchesReferenceG114(boundary, skip);
            assertFlattenVerticallyMatchesReferenceG114(boundaryPlusOne, skip);
        }

        // sparse: rows drop out of the compacted walk at different columns
        final long[][] sparse = { null, {}, { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, {}, {}, { 10, 11 }, {}, null, {}, {}, { 12, 13, 14, 15 }, {} };
        assertArrayEquals(new long[] { 1, 10, 12, 2, 11, 13, 3, 14, 4, 15, 5, 6, 7, 8, 9 }, LongStream.flatten(sparse, true).toArray());
        assertFlattenVerticallyMatchesReferenceG114(sparse, 4);

        // one long row among 999 null rows (first and last position)
        final long[][] oneLongRow = new long[1000][];
        oneLongRow[999] = new long[5000];

        for (int j = 0; j < 5000; j++) {
            oneLongRow[999][j] = j;
        }

        assertArrayEquals(oneLongRow[999], LongStream.flatten(oneLongRow, true).toArray());
        oneLongRow[0] = oneLongRow[999];
        oneLongRow[999] = null;
        assertFlattenVerticallyMatchesReferenceG114(oneLongRow, 4999);
        assertEquals(0, LongStream.flatten(new long[][] { null, {}, null, {} }, true).count());
    }



    // G100-01: iterator exhaustion and a strongly jagged input (one long row, many single-element and null rows)
    @Test
    public void testFlattenVertically_iteratorExhaustionAndLongRow() {
        final long[][] a = { null, { 1L }, {}, { 2L, 3L, 4L }, null, { 5L, 6L } };
        final LongIterator iter = LongStream.flatten(a, true).iterator();
        final LongList actual = new LongList();

        while (iter.hasNext()) {
            actual.add(iter.nextLong());
        }

        assertArrayEquals(new long[] { 1L, 2L, 5L, 3L, 6L, 4L }, actual.toArray());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextLong);
        assertFalse(iter.hasNext());

        final long[][] jagged = new long[201][];

        for (int i = 0; i < 200; i++) {
            jagged[i] = i % 3 == 0 ? null : new long[] { 1000L + i };
        }

        jagged[200] = new long[5000];

        for (int i = 0; i < 5000; i++) {
            jagged[200][i] = -i;
        }

        final long[] result = LongStream.flatten(jagged, true).toArray();
        final LongList expected = new LongList();

        for (int i = 0; i < 200; i++) {
            if (jagged[i] != null) {
                expected.add(jagged[i][0]);
            }
        }

        for (int i = 0; i < 5000; i++) {
            expected.add(jagged[200][i]);
        }

        assertArrayEquals(expected.toArray(), result);
        assertArrayEquals(new long[0], LongStream.flatten(new long[][] { null, {}, null }, true).toArray());
    }



    // G100-01: pins the column-major order of flatten(long[][], true) for jagged input with null/empty/short rows
    @Test
    public void testFlattenVertically_jaggedNullEmptyRowsMatchesColumnMajorReference() {
        final Random random = new Random(20260926L);

        for (int round = 0; round < 300; round++) {
            final int rows = 2 + random.nextInt(8);
            final long[][] a = new long[rows][];

            for (int r = 0; r < rows; r++) {
                final int kind = random.nextInt(6);
                a[r] = kind == 0 ? null : new long[kind == 1 ? 0 : random.nextInt(7)];

                if (a[r] != null) {
                    for (int c = 0; c < a[r].length; c++) {
                        a[r][c] = random.nextLong();
                    }
                }
            }

            int maxLen = 0;

            for (final long[] row : a) {
                maxLen = Math.max(maxLen, row == null ? 0 : row.length);
            }

            final LongList expected = new LongList();

            for (int c = 0; c < maxLen; c++) {
                for (final long[] row : a) {
                    if (row != null && c < row.length) {
                        expected.add(row[c]);
                    }
                }
            }

            assertArrayEquals(expected.toArray(), LongStream.flatten(a, true).toArray());
            assertEquals(expected.size(), LongStream.flatten(a, true).count());

            if (expected.size() > 1) {
                assertArrayEquals(expected.copy(1, expected.size()).toArray(), LongStream.flatten(a, true).skip(1).toArray());
            }
        }
    }

    @Test
    public void testFlattenVerticallyWithAlignment() {
        long[][] arrays = { { 1L, 2L }, { 3L }, { 4L, 5L, 6L } };
        long[] result = LongStream.flatten(arrays, 0L, true).toArray();
        assertArrayEquals(new long[] { 1L, 3L, 4L, 2L, 0L, 5L, 0L, 0L, 6L }, result);
    }

    @Test
    public void testFlattenWithAlignment() {
        long[][] arr = { { 1, 2, 3 }, { 4 }, { 5, 6 } };
        assertEquals(9, LongStream.flatten(arr, 0L, false).count());
        assertEquals(8, LongStream.flatten(arr, 0L, false).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 0, 0, 5, 6, 0 }, LongStream.flatten(arr, 0L, false).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 0, 0, 5, 6, 0 }, LongStream.flatten(arr, 0L, false).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 0L, 0L, 5L, 6L, 0L), LongStream.flatten(arr, 0L, false).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 0L, 0L, 5L, 6L, 0L), LongStream.flatten(arr, 0L, false).skip(1).toList());
        assertEquals(9, LongStream.flatten(arr, 0L, false).map(e -> e).count());
        assertEquals(8, LongStream.flatten(arr, 0L, false).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 0, 0, 5, 6, 0 }, LongStream.flatten(arr, 0L, false).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 0, 0, 5, 6, 0 }, LongStream.flatten(arr, 0L, false).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 0L, 0L, 5L, 6L, 0L), LongStream.flatten(arr, 0L, false).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 0L, 0L, 5L, 6L, 0L), LongStream.flatten(arr, 0L, false).map(e -> e).skip(1).toList());
    }

    @Test
    public void testFlattenWithAlignmentHorizontally() {
        long[][] array = { { 1L, 2L }, { 3L }, { 4L, 5L, 6L } };
        long valueForAlignment = 0L;

        LongStream stream = LongStream.flatten(array, valueForAlignment, false);
        assertArrayEquals(new long[] { 1L, 2L, 0L, 3L, 0L, 0L, 4L, 5L, 6L }, stream.toArray());
    }

    // flattMap

    @Test
    public void testFlattmap() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        long[] result = stream.flattMap(n -> java.util.stream.LongStream.of(n, n * 10)).toArray();
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L }, result);
    }

    @Test
    public void testFlattMap() {
        // flattMap uses JDK LongStream
        long[] result = createLongStream(1L, 2L, 3L).flattMap(n -> java.util.stream.LongStream.of(n, n * 10)).toArray();
        assertArrayEquals(new long[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testFlattMap_Empty() {
        long[] result = LongStream.empty().flattMap(n -> java.util.stream.LongStream.of(n, n * 10)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattMap_HappyPath() {
        long[] result = LongStream.of(1, 2, 3).flattMap(i -> java.util.stream.LongStream.of(i, i * 10)).toArray();
        assertArrayEquals(new long[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testFlattMapJdk() {
        assertEquals(9, LongStream.of(1, 2, 3).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).count());
        assertEquals(8, LongStream.of(1, 2, 3).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).skip(1).count());
        assertArrayEquals(new long[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).toArray());
        assertArrayEquals(new long[] { 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).toList());
        assertEquals(Arrays.asList(11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).skip(1).toList());
        assertEquals(9, LongStream.of(1, 2, 3).map(e -> e).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).count());
        assertEquals(8, LongStream.of(1, 2, 3).map(e -> e).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).skip(1).count());
        assertArrayEquals(new long[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).toArray());
        assertArrayEquals(new long[] { 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).map(e -> e).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).toList());
        assertEquals(Arrays.asList(11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).map(e -> e).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).skip(1).toList());
    }



    // Covers flattmap (throws UnsupportedOperationException)
    @Test
    public void testFlattmap_ThrowsUnsupported() {
        assertThrows(UnsupportedOperationException.class, () -> LongStream.of(1L, 2L).flattmap(n -> java.util.Arrays.asList(n, n * 2)));
    }

    @Test
    public void testFlattMapToObj() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        List<String> result = stream.flatMapArrayToObj(n -> new String[] { String.valueOf(n), String.valueOf(n * 10) }).toList();
        assertEquals(Arrays.asList("1", "10", "2", "20", "3", "30"), result);
    }

    @Test
    public void testFlattmapToObj() {
        LongStream stream = LongStream.of(1L, 2L);
        List<String> result = stream.flatMapArrayToObj(n -> new String[] { "P" + n, "Q" + n }).toList();
        assertEquals(Arrays.asList("P1", "Q1", "P2", "Q2"), result);
    }

    // from

    @Test
    public void testFrom() {
        java.util.stream.LongStream jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertEquals(5, LongStream.from(jdkStream).count());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertEquals(4, LongStream.from(jdkStream).skip(1).count());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.from(jdkStream).toArray());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.from(jdkStream).skip(1).toArray());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.from(jdkStream).toList());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.from(jdkStream).skip(1).toList());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertEquals(5, LongStream.from(jdkStream).map(e -> e).count());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertEquals(4, LongStream.from(jdkStream).map(e -> e).skip(1).count());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.from(jdkStream).map(e -> e).toArray());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.from(jdkStream).map(e -> e).skip(1).toArray());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.from(jdkStream).map(e -> e).toList());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.from(jdkStream).map(e -> e).skip(1).toList());
    }

    @Test
    public void testFrom_Empty() {
        java.util.stream.LongStream jdkStream = java.util.stream.LongStream.empty();
        LongStream result = LongStream.from(jdkStream);
        assertEquals(0, result.count());
    }

    @Test
    public void testFromJavaStream() {
        java.util.stream.LongStream javaStream = java.util.stream.LongStream.of(1L, 2L, 3L);
        LongStream stream = LongStream.from(javaStream);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, stream.toArray());
    }

    @Test
    public void testFromJavaStreamEmpty() {
        java.util.stream.LongStream javaStream = java.util.stream.LongStream.empty();
        LongStream stream = LongStream.from(javaStream);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFromJdkStream() {
        java.util.stream.LongStream jdkStream = java.util.stream.LongStream.of(1, 2, 3);
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStream.from(jdkStream).toArray());
    }

    @Test
    public void testFromJdkStreamIteratorCountConsumes() {
        try (LongStream stream = LongStream.from(java.util.stream.LongStream.of(1L, 2L))) {
            final LongIterator iter = stream.iterator();

            assertEquals(2, iter.count());
            assertFalse(iter.hasNext());
            assertEquals(0, iter.count());
            assertArrayEquals(new long[0], iter.toArray());
            assertThrows(NoSuchElementException.class, iter::nextLong);
        }
    }

    @Test
    public void testFromJdkStreamIteratorToArrayConsumes() {
        try (LongStream stream = LongStream.from(java.util.stream.LongStream.of(1L, 2L))) {
            final LongIterator iter = stream.iterator();

            assertArrayEquals(new long[] { 1L, 2L }, iter.toArray());
            assertFalse(iter.hasNext());
            assertEquals(0, iter.count());
            assertThrows(NoSuchElementException.class, iter::nextLong);
        }
    }

    @Test
    public void testFromNullJavaStream() {
        LongStream stream = LongStream.from((java.util.stream.LongStream) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testGenerate() {
        AtomicLong counter = new AtomicLong(0);
        LongStream stream = LongStream.generate(counter::incrementAndGet).limit(5);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, stream.toArray());
    }

    @Test
    public void testGenerateWithNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.generate(null).limit(1).count());
    }

    // iterate

    @Test
    public void testIterate() {
        LongStream stream = LongStream.iterate(1L, n -> n * 2).limit(5);
        assertArrayEquals(new long[] { 1L, 2L, 4L, 8L, 16L }, stream.toArray());
    }

    @Test
    public void test_iterate_02() {
        LongStream stream = LongStream.iterate(() -> true, () -> 1);
        assertEquals(10, stream.limit(10).mapToFloat(e -> (float) e).count());

        stream = LongStream.iterate(1, () -> true, it -> it);
        assertEquals(10, stream.limit(10).mapToInt(e -> (int) e).sum());

        stream = LongStream.iterate(1, it -> true, it -> it);
        assertEquals(10, stream.limit(10).mapToInt(e -> (int) e).sum());
    }

    @Test
    public void testIterateInfinite() {
        assertEquals(5, LongStream.iterate(1, e -> e + 1).limit(5).count());
        assertEquals(4, LongStream.iterate(1, e -> e + 1).limit(5).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.iterate(1, e -> e + 1).limit(5).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.iterate(1, e -> e + 1).limit(5).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.iterate(1, e -> e + 1).limit(5).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.iterate(1, e -> e + 1).limit(5).skip(1).toList());
        assertEquals(5, LongStream.iterate(1, e -> e + 1).map(e -> e).limit(5).count());
        assertEquals(4, LongStream.iterate(1, e -> e + 1).map(e -> e).limit(5).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.iterate(1, e -> e + 1).map(e -> e).limit(5).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.iterate(1, e -> e + 1).map(e -> e).limit(5).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.iterate(1, e -> e + 1).map(e -> e).limit(5).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.iterate(1, e -> e + 1).map(e -> e).limit(5).skip(1).toList());
    }

    @Test
    public void testIterateWithBooleanSupplier() {
        AtomicLong counter = new AtomicLong(0);
        long[] result = LongStream.iterate(() -> counter.get() < 3, counter::incrementAndGet).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testIterateWithBooleanSupplierAndLongSupplier() {
        AtomicInteger count = new AtomicInteger(0);
        LongStream stream = LongStream.iterate(() -> count.get() < 5, () -> count.getAndIncrement());
        assertArrayEquals(new long[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testIterateWithBooleanSupplierAndUnaryOperator() {
        LongStream stream = LongStream.iterate(2L, () -> true, n -> n * 2).limit(4);
        assertArrayEquals(new long[] { 2L, 4L, 8L, 16L }, stream.toArray());
    }

    @Test
    public void testIterateWithBooleanSupplierNoHasNext() {
        LongSupplier next = new LongSupplier() {
            private long value = 1L;

            @Override
            public long getAsLong() {
                long result = value;
                value *= 2;
                return result;
            }
        };

        long[] result = LongStream.iterate(() -> false, next).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testIterateWithImmediateFalseHasNext() {
        LongStream stream = LongStream.iterate(() -> false, () -> 1L);
        assertEquals(0, stream.count());

        LongStream stream2 = LongStream.iterate(1L, () -> false, n -> n + 1);
        assertEquals(0, stream2.count());

        LongStream stream3 = LongStream.iterate(1L, n -> false, n -> n + 1);
        assertEquals(0, stream3.count());
    }

    @Test
    public void testIterateWithInit() {
        assertEquals(5, LongStream.iterate(1, e -> e < 6, e -> e + 1).count());
        assertEquals(4, LongStream.iterate(1, e -> e < 6, e -> e + 1).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.iterate(1, e -> e < 6, e -> e + 1).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.iterate(1, e -> e < 6, e -> e + 1).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.iterate(1, e -> e < 6, e -> e + 1).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.iterate(1, e -> e < 6, e -> e + 1).skip(1).toList());
        assertEquals(5, LongStream.iterate(1, e -> e < 6, e -> e + 1).map(e -> e).count());
        assertEquals(4, LongStream.iterate(1, e -> e < 6, e -> e + 1).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.iterate(1, e -> e < 6, e -> e + 1).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.iterate(1, e -> e < 6, e -> e + 1).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.iterate(1, e -> e < 6, e -> e + 1).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.iterate(1, e -> e < 6, e -> e + 1).map(e -> e).skip(1).toList());
    }

    @Test
    public void testIterateWithInitAndHasNext() {
        BooleanSupplier hasNext = new BooleanSupplier() {
            private int count = 0;

            @Override
            public boolean getAsBoolean() {
                return count++ < 3;
            }
        };

        long[] result = LongStream.iterate(10L, hasNext, n -> n + 5).toArray();
        assertArrayEquals(new long[] { 10L, 15L, 20L }, result);
    }

    @Test
    public void testIterateWithNullOperator() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.iterate(1L, null).limit(2).count());
    }

    @Test
    public void testIterateWithPredicate() {
        LongStream stream = LongStream.iterate(1L, n -> n < 10L, n -> n + 2);
        assertArrayEquals(new long[] { 1L, 3L, 5L, 7L, 9L }, stream.toArray());
    }

    @Test
    public void testOf() {
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).skip(1).toList());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfArray() {
        long[] arr = { 1, 2, 3 };
        assertArrayEquals(arr, LongStream.of(arr).toArray());
    }

    @Test
    public void testOfArrayWithFullRange() {
        long[] array = { 1L, 2L, 3L };
        LongStream stream = LongStream.of(array, 0, 3);
        assertArrayEquals(array, stream.toArray());
    }

    @Test
    public void testOfArrayWithIndices() {
        long[] arr = { 10, 20, 30, 40, 50 };
        assertEquals(3, LongStream.of(arr, 1, 4).count());
        assertEquals(2, LongStream.of(arr, 1, 4).skip(1).count());
        assertArrayEquals(new long[] { 20, 30, 40 }, LongStream.of(arr, 1, 4).toArray());
        assertArrayEquals(new long[] { 30, 40 }, LongStream.of(arr, 1, 4).skip(1).toArray());
        assertEquals(Arrays.asList(20L, 30L, 40L), LongStream.of(arr, 1, 4).toList());
        assertEquals(Arrays.asList(30L, 40L), LongStream.of(arr, 1, 4).skip(1).toList());
        assertEquals(3, LongStream.of(arr, 1, 4).map(e -> e).count());
        assertEquals(2, LongStream.of(arr, 1, 4).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 20, 30, 40 }, LongStream.of(arr, 1, 4).map(e -> e).toArray());
        assertArrayEquals(new long[] { 30, 40 }, LongStream.of(arr, 1, 4).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(20L, 30L, 40L), LongStream.of(arr, 1, 4).map(e -> e).toList());
        assertEquals(Arrays.asList(30L, 40L), LongStream.of(arr, 1, 4).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfArrayWithRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongStream stream = LongStream.of(array, 1, 4);
        assertArrayEquals(new long[] { 2L, 3L, 4L }, stream.toArray());
    }

    @Test
    public void testOfBoxedNullElementsUnboxToZero() {
        // Matches ToLongFunction.UNBOX semantics: null -> 0L (no NullPointerException).
        assertArrayEquals(new long[] { 1L, 0L, 3L }, LongStream.of(new Long[] { 1L, null, 3L }).toArray());
        assertArrayEquals(new long[] { 0L, 2L }, LongStream.of(new Long[] { 9L, null, 2L }, 1, 3).toArray());
        assertArrayEquals(new long[] { 1L, 0L }, LongStream.of(Arrays.asList(1L, (Long) null)).toArray());
    }

    @Test
    public void testOfCollection() {
        List<Long> list = Arrays.asList(1L, 2L, 3L, 4L);
        LongStream stream = LongStream.of(list);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, stream.toArray());
    }

    @Test
    public void testOfEmpty() {
        assertEquals(0, LongStream.of().count());
    }

    @Test
    public void testOfEmptyArray() {
        LongStream stream = LongStream.of(new long[0]);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfEmptyCollection() {
        List<Long> list = new ArrayList<>();
        LongStream stream = LongStream.of(list);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfIterator() {
        LongIterator iter = LongIterator.of(new long[] { 1L, 2L, 3L });
        LongStream stream = LongStream.of(iter);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, stream.toArray());
    }

    @Test
    public void testOfJavaOptionalLong() {
        assertEquals(1, LongStream.of(java.util.OptionalLong.of(5)).count());
        assertEquals(0, LongStream.of(java.util.OptionalLong.of(5)).skip(1).count());
        assertArrayEquals(new long[] { 5 }, LongStream.of(java.util.OptionalLong.of(5)).toArray());
        assertArrayEquals(new long[] {}, LongStream.of(java.util.OptionalLong.of(5)).skip(1).toArray());
        assertEquals(Arrays.asList(5L), LongStream.of(java.util.OptionalLong.of(5)).toList());
        assertEquals(Arrays.asList(), LongStream.of(java.util.OptionalLong.of(5)).skip(1).toList());
        assertEquals(1, LongStream.of(java.util.OptionalLong.of(5)).map(e -> e).count());
        assertEquals(0, LongStream.of(java.util.OptionalLong.of(5)).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 5 }, LongStream.of(java.util.OptionalLong.of(5)).map(e -> e).toArray());
        assertArrayEquals(new long[] {}, LongStream.of(java.util.OptionalLong.of(5)).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(5L), LongStream.of(java.util.OptionalLong.of(5)).map(e -> e).toList());
        assertEquals(Arrays.asList(), LongStream.of(java.util.OptionalLong.of(5)).map(e -> e).skip(1).toList());

        assertEquals(0, LongStream.of(java.util.OptionalLong.empty()).count());
        assertArrayEquals(new long[] {}, LongStream.of(java.util.OptionalLong.empty()).toArray());
        assertEquals(Arrays.asList(), LongStream.of(java.util.OptionalLong.empty()).toList());
    }

    @Test
    public void testOfJdkOptionalLong() {
        LongStream stream1 = LongStream.of(java.util.OptionalLong.of(10L));
        assertArrayEquals(new long[] { 10L }, stream1.toArray());

        LongStream stream2 = LongStream.of(java.util.OptionalLong.empty());
        assertEquals(0, stream2.count());
    }

    @Test
    public void testOfLongArray() {
        Long[] arr = { 1L, 2L, 3L, 4L, 5L };
        assertEquals(5, LongStream.of(arr).count());
        assertEquals(4, LongStream.of(arr).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(arr).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(arr).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(arr).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(arr).skip(1).toList());
        assertEquals(5, LongStream.of(arr).map(e -> e).count());
        assertEquals(4, LongStream.of(arr).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(arr).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(arr).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(arr).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(arr).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfLongArrayWithIndices() {
        Long[] arr = { 10L, 20L, 30L, 40L, 50L };
        assertEquals(3, LongStream.of(arr, 1, 4).count());
        assertEquals(2, LongStream.of(arr, 1, 4).skip(1).count());
        assertArrayEquals(new long[] { 20, 30, 40 }, LongStream.of(arr, 1, 4).toArray());
        assertArrayEquals(new long[] { 30, 40 }, LongStream.of(arr, 1, 4).skip(1).toArray());
        assertEquals(Arrays.asList(20L, 30L, 40L), LongStream.of(arr, 1, 4).toList());
        assertEquals(Arrays.asList(30L, 40L), LongStream.of(arr, 1, 4).skip(1).toList());
        assertEquals(3, LongStream.of(arr, 1, 4).map(e -> e).count());
        assertEquals(2, LongStream.of(arr, 1, 4).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 20, 30, 40 }, LongStream.of(arr, 1, 4).map(e -> e).toArray());
        assertArrayEquals(new long[] { 30, 40 }, LongStream.of(arr, 1, 4).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(20L, 30L, 40L), LongStream.of(arr, 1, 4).map(e -> e).toList());
        assertEquals(Arrays.asList(30L, 40L), LongStream.of(arr, 1, 4).map(e -> e).skip(1).toList());
    }



    //
    @Test
    public void testOfLongBuffer() {
        LongBuffer buffer = LongBuffer.wrap(new long[] { 1L, 2L, 3L, 4L });
        LongStream stream = LongStream.of(buffer);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, stream.toArray());
    }

    @Test
    public void testOfLongBuffer_Basic() {
        java.nio.LongBuffer buf = java.nio.LongBuffer.wrap(new long[] { 10L, 20L, 30L });
        long[] result = LongStream.of(buf).toArray();
        assertArrayEquals(new long[] { 10L, 20L, 30L }, result);
    }

    @Test
    public void testOfLongBuffer_Null() {
        assertEquals(0L, LongStream.of((java.nio.LongBuffer) null).count());
    }

    @Test
    public void testOfLongIterator_Basic() {
        LongIterator iter = LongIterator.of(new long[] { 5L, 6L, 7L });
        long[] result = LongStream.of(iter).toArray();
        assertArrayEquals(new long[] { 5L, 6L, 7L }, result);
    }

    @Test
    public void testOfLongIterator_Null() {
        assertEquals(0L, LongStream.of((LongIterator) null).count());
    }

    @Test
    public void testOfLongObjectArray() {
        Long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongStream stream = LongStream.of(array);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, stream.toArray());
    }

    @Test
    public void testOfLongObjectArrayWithRange() {
        Long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongStream stream = LongStream.of(array, 1, 3);
        assertArrayEquals(new long[] { 2L, 3L }, stream.toArray());
    }

    @Test
    public void testOfMultipleElements() {
        assertArrayEquals(new long[] { 1L, 2L, 3L }, LongStream.of(1L, 2L, 3L).toArray());
    }

    // ofNullable

    @Test
    public void testOfNullable() {
        assertEquals(1, LongStream.ofNullable(5L).count());
        assertEquals(0, LongStream.ofNullable(null).count());

        assertArrayEquals(new long[] { 100L }, LongStream.ofNullable(100L).toArray());
    }

    @Test
    public void testOfNullable_WithNull() {
        LongStream result = LongStream.ofNullable(null);
        assertEquals(0, result.count());
    }

    @Test
    public void testOfNullable_WithValue() {
        LongStream result = LongStream.ofNullable(42L);
        assertArrayEquals(new long[] { 42 }, result.toArray());
    }

    @Test
    public void testOfOptionalLong() {
        LongStream stream1 = LongStream.of(OptionalLong.of(5L));
        assertEquals(1, stream1.count());

        LongStream stream2 = LongStream.of(OptionalLong.empty());
        assertEquals(0, stream2.count());
    }

    @Test
    public void testOfSingleElement() {
        LongStream stream = LongStream.of(42L);
        assertArrayEquals(new long[] { 42L }, stream.toArray());
    }

    @Test
    public void testOfVarArgs() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongStream stream = LongStream.of(array);
        assertArrayEquals(array, stream.toArray());
    }

    @Test
    public void testRandom() {
        long[] randoms = LongStream.random().limit(10).toArray();
        assertEquals(10, randoms.length);

        Set<Long> uniqueValues = new HashSet<>();
        for (long val : randoms) {
            uniqueValues.add(val);
        }
        assertTrue(uniqueValues.size() > 1);
    }

    // range

    @Test
    public void testRange() {
        assertArrayEquals(new long[] { 0L, 1L, 2L, 3L, 4L }, LongStream.range(0L, 5L).toArray());
        assertArrayEquals(new long[] {}, LongStream.range(5L, 5L).toArray());
        assertArrayEquals(new long[] {}, LongStream.range(5L, 0L).toArray());
    }



    // Covers count() in range(long, long) inner LongIteratorEx
    @Test
    public void testRange_Count() {
        assertEquals(10L, LongStream.range(0L, 10L).count());
        assertEquals(0L, LongStream.range(5L, 5L).count());
    }

    @Test
    public void testRange_Empty_WhenStartGeEnd() {
        assertEquals(0L, LongStream.range(5L, 5L).count());
        assertEquals(0L, LongStream.range(6L, 5L).count());
    }



    // Covers advance() in range(long, long) inner LongIteratorEx
    @Test
    public void testRange_Skip() {
        long[] result = LongStream.range(0L, 10L).skip(5).toArray();
        assertArrayEquals(new long[] { 5L, 6L, 7L, 8L, 9L }, result);

        long[] empty = LongStream.range(0L, 5L).skip(10).toArray();
        assertEquals(0, empty.length);
    }

    @Test
    public void testRange_ToArray_Direct() {
        // exercises toArray() on range iterator
        long[] result = LongStream.range(3L, 8L).toArray();
        assertArrayEquals(new long[] { 3L, 4L, 5L, 6L, 7L }, result);
    }

    @Test
    public void testRange_WithStep_Count() {
        // range(long, long, long) - positive step
        long[] result = LongStream.range(0L, 10L, 2L).toArray();
        assertArrayEquals(new long[] { 0L, 2L, 4L, 6L, 8L }, result);
    }

    @Test
    public void testRange_WithStep_EmptyWhenWrongDirection() {
        // step direction does not match start/end: empty
        assertEquals(0L, LongStream.range(0L, 10L, -1L).count());
        assertEquals(0L, LongStream.range(10L, 0L, 1L).count());
    }

    @Test
    public void testRange_WithStep_NegativeStep() {
        // range(long, long, long) - negative step
        long[] result = LongStream.range(10L, 0L, -2L).toArray();
        assertArrayEquals(new long[] { 10L, 8L, 6L, 4L, 2L }, result);
    }



    // Covers advance() in range(long, long, long) inner LongIteratorEx
    @Test
    public void testRange_WithStep_Skip() {
        // skip() calls advance() on the iterator
        long[] result = LongStream.range(0L, 20L, 2L).skip(3).toArray();
        assertArrayEquals(new long[] { 6L, 8L, 10L, 12L, 14L, 16L, 18L }, result);

        // skip more than count
        long[] empty = LongStream.range(0L, 10L, 2L).skip(100).toArray();
        assertEquals(0, empty.length);
    }

    @Test
    public void testRange_WithStep_SkipAdvance() {
        // exercises advance() in range(long, long, long) inner iterator
        long[] result = LongStream.range(0L, 20L, 2L).skip(3).toArray();
        assertArrayEquals(new long[] { 6L, 8L, 10L, 12L, 14L, 16L, 18L }, result);
    }

    @Test
    public void testRange_WithStep_SkipAll() {
        long[] result = LongStream.range(0L, 10L, 2L).skip(100).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testRange_WithStep_ZeroThrows() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.range(0L, 10L, 0L));
    }

    @Test
    public void testRangeDescendingHugeSpanStaysOnStepGrid() {
        // Regression: the overflow-split path used `m + m % by` for descending ranges (negative m),
        // which is not a multiple of `by`, so the concatenated sub-ranges drifted off the
        // arithmetic-progression grid and emitted wrong elements.
        final long by = -(1L << 62);
        final long[] expected = { Long.MAX_VALUE, 4611686018427387903L, -1L, -4611686018427387905L };

        assertArrayEquals(expected, LongStream.range(Long.MAX_VALUE, Long.MIN_VALUE, by).toArray());
        assertArrayEquals(expected, LongStream.rangeClosed(Long.MAX_VALUE, Long.MIN_VALUE, by).toArray());

        // Ascending mirror case (was already correct; guards against symmetric regressions).
        final long[] expectedAsc = { Long.MIN_VALUE, -4611686018427387904L, 0L, 4611686018427387904L };
        assertArrayEquals(expectedAsc, LongStream.range(Long.MIN_VALUE, Long.MAX_VALUE, 1L << 62).toArray());
        assertArrayEquals(expectedAsc, LongStream.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE, 1L << 62).toArray());
    }

    @Test
    public void testRangeIteratorFailedNextDoesNotReduceRemainingCount() {
        final LongIterator iter = LongStream.range(0L, 1L).iterator();

        assertEquals(0L, iter.nextLong());
        assertThrows(NoSuchElementException.class, iter::nextLong);
        assertEquals(0L, iter.count());
    }

    @Test
    public void testRangeLarge() {
        LongStream stream = LongStream.range(0L, 1000L);
        assertEquals(1000, stream.count());
    }

    @Test
    public void testRangeLargeNumbers() {
        long start = Long.MAX_VALUE - 5;
        long end = Long.MAX_VALUE;
        long[] result = LongStream.range(start, end).toArray();
        assertEquals(5, result.length);
        assertEquals(start, result[0]);
        assertEquals(end - 1, result[4]);
    }

    @Test
    public void testRangeWithInvalidStepDirection() {
        // Positive step but start > end should return empty
        LongStream stream = LongStream.range(10L, 1L, 2L);
        assertEquals(0, stream.count());

        // Negative step but start < end should return empty
        LongStream stream2 = LongStream.range(1L, 10L, -2L);
        assertEquals(0, stream2.count());
    }

    @Test
    public void testRangeWithLongMinStepDoesNotRecurseForever() {
        assertArrayEquals(new long[] { 0L }, LongStream.range(0L, Long.MIN_VALUE, Long.MIN_VALUE).toArray());
        assertArrayEquals(new long[] { 0L, Long.MIN_VALUE }, LongStream.rangeClosed(0L, Long.MIN_VALUE, Long.MIN_VALUE).toArray());
    }

    @Test
    public void testRangeWithNegativeStep() {
        long[] result = LongStream.range(5, 0, -1).toArray();
        assertArrayEquals(new long[] { 5, 4, 3, 2, 1 }, result);
    }

    @Test
    public void testRangeWithStep() {
        assertArrayEquals(new long[] { 0L, 2L, 4L }, LongStream.range(0L, 6L, 2L).toArray());
        assertArrayEquals(new long[] { 5L, 3L, 1L }, LongStream.range(5L, 0L, -2L).toArray());
        assertArrayEquals(new long[] { 10L, 7L, 4L, 1L }, LongStream.range(10L, 0L, -3L).toArray());
    }

    @Test
    public void testRangeMap() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 10L, 11L, 20L);
        long[] result = stream.rangeMap((first, next) -> next - first <= 2, (first, last) -> first + last).toArray();
        assertArrayEquals(new long[] { 4L, 21L, 40L }, result);
    }

    @Test
    public void testRangeMap_HappyPath() {
        long[] result = LongStream.of(1, 1, 2, 2, 3).rangeMap((a, b) -> a == b, Long::sum).toArray();
        assertArrayEquals(new long[] { 2, 4, 6 }, result);
    }

    @Test
    public void testRangeMapToObj() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 10L, 11L);
        List<String> result = stream.rangeMapToObj((first, next) -> next - first <= 2, (first, last) -> first + "-" + last).toList();
        assertEquals(Arrays.asList("1-3", "10-11"), result);
    }

    @Test
    public void testRangeMapToObj_HappyPath() {
        List<String> result = LongStream.of(1, 1, 2, 3, 3).rangeMapToObj((a, b) -> a == b, (a, b) -> a + "-" + b).toList();
        assertEquals(Arrays.asList("1-1", "2-2", "3-3"), result);
    }

    // repeat

    @Test
    public void testRepeat() {
        assertArrayEquals(new long[] { 5L, 5L, 5L }, LongStream.repeat(5L, 3).toArray());
        assertArrayEquals(new long[] {}, LongStream.repeat(5L, 0).toArray());
        assertArrayEquals(new long[] { 42L }, LongStream.repeat(42L, 1).toArray());
    }

    @Test
    public void testRepeat_02() {
        LongStream stream = LongStream.repeat(1, 1000);
        assertEquals(1000, stream.mapToFloat(e -> (float) e).count());
    }

    @Test
    public void testRepeatNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.repeat(5L, -1));
    }

}
