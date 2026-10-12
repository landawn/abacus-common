package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;

/** ByteStream creation contracts. */
public class ByteStreamCreationTest extends ByteStreamTestSupport {

    // defer

    @Test
    public void testDefer() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<ByteStream> supplier = () -> {
            counter.incrementAndGet();
            return ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        };

        ByteStream stream = ByteStream.defer(supplier);
        assertEquals(0, counter.get());

        byte[] result = stream.toArray();
        assertEquals(1, counter.get());
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);

        assertThrows(IllegalArgumentException.class, () -> ByteStream.defer(null));
    }

    @Test
    public void testDefer_Basic() {
        ByteStream stream = ByteStream.defer(() -> ByteStream.of((byte) 1, (byte) 2, (byte) 3));
        byte[] result = stream.toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testDefer_supplierReturningNullProducesEmptyStream() {
        final AtomicInteger supplierCalls = new AtomicInteger();
        final ByteStream deferred = ByteStream.defer(() -> {
            supplierCalls.incrementAndGet();
            return null;
        });

        assertEquals(0, deferred.count());
        assertEquals(1, supplierCalls.get());
    }

    @Test
    public void testDeferRejectsNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ByteStream.defer(null));
    }

    // empty

    @Test
    public void testEmpty() {
        ByteStream stream = ByteStream.empty();
        assertNotNull(stream);
        assertEquals(0, stream.count());

        ByteStream emptyStream = ByteStream.empty();
        assertFalse(emptyStream.findFirst().isPresent());

        ByteStream emptyStream2 = ByteStream.empty();
        assertEquals(0, emptyStream2.toArray().length);
    }

    @Test
    public void testEmptyStreamAverage() {
        assertFalse(ByteStream.empty().average().isPresent());
    }

    @Test
    public void testEmptyStreamMax() {
        assertFalse(ByteStream.empty().max().isPresent());
    }

    @Test
    public void testEmptyStreamMin() {
        assertFalse(ByteStream.empty().min().isPresent());
    }

    @Test
    public void testEmptyStreamReduce() {
        assertFalse(ByteStream.empty().reduce((a, b) -> (byte) (a + b)).isPresent());
    }

    @Test
    public void testEmptyStreamSum() {
        assertEquals(0, ByteStream.empty().sum());
    }

    @Test
    public void testEmptyStreamSummaryStatistics() {
        ByteSummaryStatistics stats = ByteStream.empty().summaryStatistics();
        assertNotNull(stats);
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testEmptyStreamSummaryStatisticsAndPercentiles() {
        Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> result = ByteStream.empty().summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertEquals(0, result.left().getCount());
    }

    // flatten

    @Test
    public void testFlatten2D() {
        byte[][] array = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        ByteStream stream = ByteStream.flatten(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());

        byte[][] emptyArray = new byte[0][];
        ByteStream emptyStream = ByteStream.flatten(emptyArray);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testFlatten2DArray() {
        byte[][] data = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        List<Byte> result = ByteStream.flatten(data).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), result);
    }

    @Test
    public void testFlatten2DArrayVertically() {
        byte[][] data = { { 1, 2, 3 }, { 4, 5, 6 } };
        List<Byte> result = ByteStream.flatten(data, true).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 4, (byte) 2, (byte) 5, (byte) 3, (byte) 6), result);

        result = ByteStream.flatten(data, false).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), result);
    }

    @Test
    public void testFlatten2DArrayWithAlignment() {
        byte[][] data = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        List<Byte> result = ByteStream.flatten(data, (byte) 0, false).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 0, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 0, (byte) 0), result);

        result = ByteStream.flatten(data, (byte) 0, true).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 6, (byte) 2, (byte) 4, (byte) 0, (byte) 0, (byte) 5, (byte) 0), result);
    }

    @Test
    public void testFlatten2DVertically() {
        byte[][] array = { { 1, 2, 3 }, { 4, 5, 6 } };
        ByteStream stream = ByteStream.flatten(array, false);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());

        byte[][] array2 = { { 1, 2, 3 }, { 4, 5, 6 } };
        ByteStream stream2 = ByteStream.flatten(array2, true);
        assertArrayEquals(new byte[] { 1, 4, 2, 5, 3, 6 }, stream2.toArray());
    }

    @Test
    public void testFlatten2DWithAlignment() {
        byte[][] array = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        ByteStream stream = ByteStream.flatten(array, (byte) 0, true);
        byte[] result = stream.toArray();
        assertEquals(9, result.length);
    }

    @Test
    public void testFlatten3D() {
        byte[][][] array = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } };
        ByteStream stream = ByteStream.flatten(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, stream.toArray());
    }

    @Test
    public void testFlatten3DArray() {
        byte[][][] data = { { { 1, 2 } }, { { 3, 4, 5 } } };
        List<Byte> result = ByteStream.flatten(data).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void testFlatten_2DArray() {
        byte[][] a = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        byte[] result = ByteStream.flatten(a).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testFlatten_3DArray() {
        byte[][][] a = { { { 1, 2 }, { 3 } }, { { 4, 5, 6 } } };
        byte[] result = ByteStream.flatten(a).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testFlatten_Vertically() {
        byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
        byte[] result = ByteStream.flatten(a, true).toArray();
        assertArrayEquals(new byte[] { 1, 4, 2, 5, 3, 6 }, result);
    }

    @Test
    public void testFlatten_WithAlignment() {
        byte[][] a = { { 1, 2, 3 }, { 4, 5 } };
        byte[] result = ByteStream.flatten(a, (byte) 0, true).toArray();
        assertArrayEquals(new byte[] { 1, 4, 2, 5, 3, 0 }, result);
    }

    @Test
    public void testFlattenArray() {
        byte[][] array = { { 1, 2 }, { 3, 4 }, { 5 } };
        ByteStream stream = ByteStream.flatten(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());

        ByteStream verticalStream = ByteStream.flatten(array, true);
        assertArrayEquals(new byte[] { 1, 3, 5, 2, 4 }, verticalStream.toArray());

        ByteStream alignedStream = ByteStream.flatten(array, (byte) 0, false);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 0 }, alignedStream.toArray());
    }

    @Test
    public void testFlattenVertically() {
        byte[][] array = { { 1, 2, 3 }, { 4, 5, 6 } };
        assertEquals(6, ByteStream.flatten(array, true).count());
        assertEquals(5, ByteStream.flatten(array, true).skip(1).count());
        assertArrayEquals(new byte[] { 1, 4, 2, 5, 3, 6 }, ByteStream.flatten(array, true).toArray());
        assertArrayEquals(new byte[] { 4, 2, 5, 3, 6 }, ByteStream.flatten(array, true).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 4, (byte) 2, (byte) 5, (byte) 3, (byte) 6), ByteStream.flatten(array, true).toList());
        assertEquals(N.toList((byte) 4, (byte) 2, (byte) 5, (byte) 3, (byte) 6), ByteStream.flatten(array, true).skip(1).toList());
        assertEquals(6, ByteStream.flatten(array, true).map(e -> e).count());
        assertEquals(5, ByteStream.flatten(array, true).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 4, 2, 5, 3, 6 }, ByteStream.flatten(array, true).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 4, 2, 5, 3, 6 }, ByteStream.flatten(array, true).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 4, (byte) 2, (byte) 5, (byte) 3, (byte) 6), ByteStream.flatten(array, true).map(e -> e).toList());
        assertEquals(N.toList((byte) 4, (byte) 2, (byte) 5, (byte) 3, (byte) 6), ByteStream.flatten(array, true).map(e -> e).skip(1).toList());
    }



    // G114-01: flatten(byte[][], true) keeps the original walk for dense input and uses a compacted row walk when
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
            final byte[][] a = new byte[rows][];

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
                    a[i] = new byte[len];

                    for (int j = 0; j < len; j++) {
                        a[i][j] = (byte) (i * 100 + j);
                    }
                }
            }

            assertFlattenVerticallyMatchesReferenceG114(a, random.nextInt(rows * 3 + 2));
        }

        // rows * longest row == 4 * elements keeps the original walk; one more empty row switches to the compacted walk
        final byte[][] boundary = { { 1, 2, 3, 4 }, null, {}, { 5, 6, 7, 8 }, null, {}, {}, {} };
        final byte[][] boundaryPlusOne = { { 1, 2, 3, 4 }, null, {}, { 5, 6, 7, 8 }, null, {}, {}, {}, {} };
        assertArrayEquals(new byte[] { 1, 5, 2, 6, 3, 7, 4, 8 }, ByteStream.flatten(boundary, true).toArray());
        assertArrayEquals(new byte[] { 1, 5, 2, 6, 3, 7, 4, 8 }, ByteStream.flatten(boundaryPlusOne, true).toArray());

        for (int skip = 0; skip <= 9; skip++) {
            assertFlattenVerticallyMatchesReferenceG114(boundary, skip);
            assertFlattenVerticallyMatchesReferenceG114(boundaryPlusOne, skip);
        }

        // sparse: rows drop out of the compacted walk at different columns
        final byte[][] sparse = { null, {}, { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, {}, {}, { 10, 11 }, {}, null, {}, {}, { 12, 13, 14, 15 }, {} };
        assertArrayEquals(new byte[] { 1, 10, 12, 2, 11, 13, 3, 14, 4, 15, 5, 6, 7, 8, 9 }, ByteStream.flatten(sparse, true).toArray());
        assertFlattenVerticallyMatchesReferenceG114(sparse, 4);

        // one long row among 999 null rows (first and last position)
        final byte[][] oneLongRow = new byte[1000][];
        oneLongRow[999] = new byte[5000];

        for (int j = 0; j < 5000; j++) {
            oneLongRow[999][j] = (byte) j;
        }

        assertArrayEquals(oneLongRow[999], ByteStream.flatten(oneLongRow, true).toArray());
        oneLongRow[0] = oneLongRow[999];
        oneLongRow[999] = null;
        assertFlattenVerticallyMatchesReferenceG114(oneLongRow, 4999);
        assertEquals(0, ByteStream.flatten(new byte[][] { null, {}, null, {} }, true).count());
    }



    // G090-03: iterator exhaustion and a strongly jagged input (one long row, many single-element and null rows)
    @Test
    public void testFlattenVertically_iteratorExhaustionAndLongRow() {
        final byte[][] a = { null, { 1 }, {}, { 2, 3, 4 }, null, { 5, 6 } };
        final ByteIterator iter = ByteStream.flatten(a, true).iterator();
        final ByteList actual = new ByteList();

        while (iter.hasNext()) {
            actual.add(iter.nextByte());
        }

        assertEquals(ByteList.of((byte) 1, (byte) 2, (byte) 5, (byte) 3, (byte) 6, (byte) 4), actual);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextByte);
        assertFalse(iter.hasNext());

        final byte[][] jagged = new byte[201][];

        for (int i = 0; i < 200; i++) {
            jagged[i] = i % 3 == 0 ? null : new byte[] { (byte) (i % 26) };
        }

        jagged[200] = new byte[5000];

        for (int i = 0; i < 5000; i++) {
            jagged[200][i] = (byte) (i % 10 + 100);
        }

        final byte[] result = ByteStream.flatten(jagged, true).toArray();
        final ByteList expected = new ByteList();

        for (int i = 0; i < 200; i++) {
            if (jagged[i] != null) {
                expected.add(jagged[i][0]);
            }
        }

        for (int i = 0; i < 5000; i++) {
            expected.add(jagged[200][i]);
        }

        assertArrayEquals(expected.toArray(), result);
        assertArrayEquals(new byte[0], ByteStream.flatten(new byte[][] { null, {}, null }, true).toArray());
    }



    // G090-03: pins the column-major order of flatten(byte[][], true) for jagged input with null/empty/short rows
    @Test
    public void testFlattenVertically_jaggedNullEmptyRowsMatchesColumnMajorReference() {
        final Random random = new Random(20260926L);

        for (int round = 0; round < 300; round++) {
            final int rows = 2 + random.nextInt(8);
            final byte[][] a = new byte[rows][];

            for (int r = 0; r < rows; r++) {
                final int kind = random.nextInt(6);
                a[r] = kind == 0 ? null : new byte[kind == 1 ? 0 : random.nextInt(7)];

                if (a[r] != null) {
                    for (int c = 0; c < a[r].length; c++) {
                        a[r][c] = (byte) random.nextInt(256);
                    }
                }
            }

            int maxLen = 0;

            for (final byte[] row : a) {
                maxLen = Math.max(maxLen, row == null ? 0 : row.length);
            }

            final ByteList expected = new ByteList();

            for (int c = 0; c < maxLen; c++) {
                for (final byte[] row : a) {
                    if (row != null && c < row.length) {
                        expected.add(row[c]);
                    }
                }
            }

            assertArrayEquals(expected.toArray(), ByteStream.flatten(a, true).toArray());
            assertEquals(expected.size(), ByteStream.flatten(a, true).count());

            if (expected.size() > 1) {
                assertArrayEquals(expected.copy(1, expected.size()).toArray(), ByteStream.flatten(a, true).skip(1).toArray());
            }
        }
    }

    @Test
    public void testFlattenWithAlignment() {
        byte[][] array = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        assertEquals(9, ByteStream.flatten(array, (byte) 0, true).count());
        assertEquals(8, ByteStream.flatten(array, (byte) 0, true).skip(1).count());
        assertArrayEquals(new byte[] { 1, 3, 6, 2, 4, 0, 0, 5, 0 }, ByteStream.flatten(array, (byte) 0, true).toArray());
        assertArrayEquals(new byte[] { 3, 6, 2, 4, 0, 0, 5, 0 }, ByteStream.flatten(array, (byte) 0, true).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 3, (byte) 6, (byte) 2, (byte) 4, (byte) 0, (byte) 0, (byte) 5, (byte) 0),
                ByteStream.flatten(array, (byte) 0, true).toList());
        assertEquals(N.toList((byte) 3, (byte) 6, (byte) 2, (byte) 4, (byte) 0, (byte) 0, (byte) 5, (byte) 0),
                ByteStream.flatten(array, (byte) 0, true).skip(1).toList());
        assertEquals(9, ByteStream.flatten(array, (byte) 0, true).map(e -> e).count());
        assertEquals(8, ByteStream.flatten(array, (byte) 0, true).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 3, 6, 2, 4, 0, 0, 5, 0 }, ByteStream.flatten(array, (byte) 0, true).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 3, 6, 2, 4, 0, 0, 5, 0 }, ByteStream.flatten(array, (byte) 0, true).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 3, (byte) 6, (byte) 2, (byte) 4, (byte) 0, (byte) 0, (byte) 5, (byte) 0),
                ByteStream.flatten(array, (byte) 0, true).map(e -> e).toList());
        assertEquals(N.toList((byte) 3, (byte) 6, (byte) 2, (byte) 4, (byte) 0, (byte) 0, (byte) 5, (byte) 0),
                ByteStream.flatten(array, (byte) 0, true).map(e -> e).skip(1).toList());
    }



    // Edge cases
    @Test
    public void testFlattenWithAlignmentHorizontally() {
        byte[][] array = { { 1, 2, 3 }, { 4, 5 }, { 6 } };
        ByteStream stream = ByteStream.flatten(array, (byte) 0, true);
        byte[] result = stream.toArray();
        assertEquals(9, result.length);
        assertEquals(1, result[0]);
        assertEquals(4, result[1]);
        assertEquals(6, result[2]);
    }

    @Test
    public void testFlattMapToObj() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<String> result = byteStream.flatMapArrayToObj(b -> new String[] { "E" + b, "F" + b }).toList();
        assertEquals(Arrays.asList("E1", "F1", "E2", "F2"), result);
    }

    @Test
    public void testFlattmapToObj() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2);
        List<String> result = stream.flatMapArrayToObj(b -> new String[] { "a" + b, "b" + b }).toList();
        assertEquals(Arrays.asList("a1", "b1", "a2", "b2"), result);
    }

    // generate

    @Test
    public void testGenerate() {
        AtomicInteger counter = new AtomicInteger(0);
        ByteStream stream = ByteStream.generate(() -> (byte) counter.getAndIncrement()).limit(3);
        assertArrayEquals(new byte[] { 0, 1, 2 }, stream.toArray());

        assertThrows(IllegalArgumentException.class, () -> ByteStream.generate(null).limit(1).count());
    }

    @Test
    public void testGenerate_Basic() {
        AtomicInteger counter = new AtomicInteger(0);
        byte[] result = ByteStream.generate(() -> (byte) counter.getAndIncrement()).limit(5).toArray();
        assertArrayEquals(new byte[] { 0, 1, 2, 3, 4 }, result);
    }

    @Test
    public void testGenerateNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ByteStream.generate(null).limit(1).count());
    }

    // iterate

    @Test
    public void testIterate() {
        assertEquals(5, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).count());
        assertEquals(4, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).toList());
        assertEquals(N.toList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).skip(1).toList());
        assertEquals(5, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).count());
        assertEquals(4, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).toList());
        assertEquals(N.toList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).skip(1).toList());
    }

    @Test
    public void testIterate_Infinite() {
        byte[] result = ByteStream.iterate((byte) 0, b -> (byte) (b + 1)).limit(5).toArray();
        assertArrayEquals(new byte[] { 0, 1, 2, 3, 4 }, result);
    }

    @Test
    public void testIterate_WithPredicate() {
        byte[] result = ByteStream.iterate((byte) 1, b -> b < 5, b -> (byte) (b + 1)).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testIterateBooleanSupplierByteSupplier() {
        AtomicReference<Byte> current = new AtomicReference<>((byte) 0);
        List<Byte> result = ByteStream.iterate(() -> current.get() < 3, () -> {
            byte val = current.get();
            current.set((byte) (val + 1));
            return val;
        }).toList();
        assertEquals(Arrays.asList((byte) 0, (byte) 1, (byte) 2), result);
    }

    @Test
    public void testIterateInfinite() {
        ByteStream stream = ByteStream.iterate((byte) 1, b -> (byte) (b + 1)).limit(5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testIterateInitBooleanSupplierByteUnaryOperator() {
        AtomicReference<Byte> currentVal = new AtomicReference<>((byte) 1);
        List<Byte> result = ByteStream.iterate((byte) 1, () -> currentVal.get() <= 5, b -> {
            currentVal.set((byte) (b + 1));
            return (byte) (b + 1);
        }).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), result);
    }

    @Test
    public void testIterateInitBytePredicateByteUnaryOperator() {
        List<Byte> result = ByteStream.iterate((byte) 1, b -> b < 5, b -> (byte) (b * 2)).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 4), result);
    }

    @Test
    public void testIterateInitByteUnaryOperator() {
        List<Byte> result = ByteStream.iterate((byte) 1, b -> (byte) (b + 1)).limit(3).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    @Test
    public void testIterateNullOperator() {
        assertThrows(IllegalArgumentException.class,
                () -> ByteStream.iterate((byte) 0, (com.landawn.abacus.util.function.ByteUnaryOperator) null).limit(2).count());
    }

    @Test
    public void testIterateWithHasNext() {
        AtomicInteger counter = new AtomicInteger(0);
        ByteStream stream = ByteStream.iterate(() -> counter.get() < 3, () -> (byte) counter.getAndIncrement());
        assertArrayEquals(new byte[] { 0, 1, 2 }, stream.toArray());
    }

    @Test
    public void testIterateWithImmediateFalseHasNext() {
        ByteStream stream = ByteStream.iterate((byte) 1, () -> false, b -> (byte) (b + 1));
        assertEquals(0, stream.count());
    }

    @Test
    public void testIterateWithInitAndHasNext() {
        ByteStream stream = ByteStream.iterate((byte) 1, () -> true, b -> (byte) (b * 2)).limit(4);
        assertArrayEquals(new byte[] { 1, 2, 4, 8 }, stream.toArray());
    }

    @Test
    public void testIterateWithInitAndPredicate() {
        ByteStream stream = ByteStream.iterate((byte) 1, b -> b < 10, b -> (byte) (b * 2));
        assertArrayEquals(new byte[] { 1, 2, 4, 8 }, stream.toArray());
    }

    @Test
    public void testFlattmapToObjArray() {
        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).count());
        assertEquals(9,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(1).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).toArray());
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(1).toArray());
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).toList());
        assertEquals(N.toList("B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(1).toList());
        assertEquals(10,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).count());
        assertEquals(9,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e })
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e })
                        .toArray());
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e })
                        .skip(1)
                        .toArray());
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).toList());
        assertEquals(N.toList("B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e })
                        .skip(1)
                        .toList());
    }

    @Test
    public void testOfArray() {
        byte[] array = { 1, 2, 3, 4, 5 };
        ByteStream stream = ByteStream.of(array);
        assertArrayEquals(array, stream.toArray());

        ByteStream emptyStream = ByteStream.of(new byte[0]);
        assertEquals(0, emptyStream.count());

        ByteStream rangeStream = ByteStream.of(array, 1, 4);
        assertArrayEquals(new byte[] { 2, 3, 4 }, rangeStream.toArray());

        ByteStream nullStream = ByteStream.of((byte[]) null);
        assertEquals(0, nullStream.count());
    }

    @Test
    public void testOfArrayWithIndices() {
        byte[] array = { 1, 2, 3, 4, 5, 6, 7 };
        assertEquals(3, ByteStream.of(array, 2, 5).count());
        assertEquals(2, ByteStream.of(array, 2, 5).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of(array, 2, 5).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of(array, 2, 5).skip(1).toArray());
        assertEquals(N.toList((byte) 3, (byte) 4, (byte) 5), ByteStream.of(array, 2, 5).toList());
        assertEquals(N.toList((byte) 4, (byte) 5), ByteStream.of(array, 2, 5).skip(1).toList());
        assertEquals(3, ByteStream.of(array, 2, 5).map(e -> e).count());
        assertEquals(2, ByteStream.of(array, 2, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of(array, 2, 5).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of(array, 2, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((byte) 3, (byte) 4, (byte) 5), ByteStream.of(array, 2, 5).map(e -> e).toList());
        assertEquals(N.toList((byte) 4, (byte) 5), ByteStream.of(array, 2, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfBoxedArrayAndCollectionUnboxNullElementsToZero() {
        assertArrayEquals(new byte[] { 1, 0, 3 }, ByteStream.of(new Byte[] { 1, null, 3 }).toArray());
        assertArrayEquals(new byte[] { 0, 3 }, ByteStream.of(new Byte[] { 1, null, 3, 4 }, 1, 3).toArray());
        assertArrayEquals(new byte[] { 1, 0, 3 }, ByteStream.of(Arrays.asList((byte) 1, null, (byte) 3)).toArray());

        assertEquals(0, ByteStream.of((Byte[]) null).count());
        assertEquals(0, ByteStream.of((Collection<Byte>) null).count());
    }

    @Test
    public void testOfBoxedBytes() {
        Byte[] data = { 1, 2, 3 };
        List<Byte> result = createByteStream(data).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    @Test
    public void testOfBoxedBytesRange() {
        Byte[] data = { 10, 20, 30, 40, 50 };
        List<Byte> result = createByteStream(data, 1, 4).toList();
        assertEquals(Arrays.asList((byte) 20, (byte) 30, (byte) 40), result);
    }

    @Test
    public void testOfByteArray() {
        Byte[] array = { 1, 2, 3, 4, 5 };
        ByteStream stream = ByteStream.of(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());

        ByteStream rangeStream = ByteStream.of(array, 1, 3);
        assertArrayEquals(new byte[] { 2, 3 }, rangeStream.toArray());

        ByteStream emptyStream = ByteStream.of(new Byte[0]);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testOfByteBuffer() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        buffer.position(1);
        buffer.limit(4);

        ByteStream stream = ByteStream.of(buffer);
        assertArrayEquals(new byte[] { 2, 3, 4 }, stream.toArray());

        ByteStream nullStream = ByteStream.of((ByteBuffer) null);
        assertEquals(0, nullStream.count());
    }

    @Test
    public void testOfByteIterator() {
        ByteIterator iter = ByteIterator.of(new byte[] { 1, 2, 3 });
        ByteStream stream = ByteStream.of(iter);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream.toArray());

        ByteStream nullStream = ByteStream.of((ByteIterator) null);
        assertEquals(0, nullStream.count());
    }

    @Test
    public void testOfByteObjectArray() {
        Byte[] array = { 1, 2, 3, 4, 5 };
        assertEquals(5, ByteStream.of(array).count());
        assertEquals(4, ByteStream.of(array).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of(array).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of(array).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(array).toList());
        assertEquals(N.toList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(array).skip(1).toList());
        assertEquals(5, ByteStream.of(array).map(e -> e).count());
        assertEquals(4, ByteStream.of(array).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of(array).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of(array).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(array).map(e -> e).toList());
        assertEquals(N.toList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfByteObjectArrayWithIndices() {
        Byte[] array = { 1, 2, 3, 4, 5, 6, 7 };
        assertEquals(3, ByteStream.of(array, 2, 5).count());
        assertEquals(2, ByteStream.of(array, 2, 5).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of(array, 2, 5).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of(array, 2, 5).skip(1).toArray());
        assertEquals(N.toList((byte) 3, (byte) 4, (byte) 5), ByteStream.of(array, 2, 5).toList());
        assertEquals(N.toList((byte) 4, (byte) 5), ByteStream.of(array, 2, 5).skip(1).toList());
        assertEquals(3, ByteStream.of(array, 2, 5).map(e -> e).count());
        assertEquals(2, ByteStream.of(array, 2, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of(array, 2, 5).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of(array, 2, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((byte) 3, (byte) 4, (byte) 5), ByteStream.of(array, 2, 5).map(e -> e).toList());
        assertEquals(N.toList((byte) 4, (byte) 5), ByteStream.of(array, 2, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfBytes() {
        List<Byte> result = createByteStream((byte) 1, (byte) 2, (byte) 3).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
        assertTrue(createByteStream().toList().isEmpty());
    }

    @Test
    public void testOfBytesRange() {
        byte[] data = { 10, 20, 30, 40, 50 };
        List<Byte> result = createByteStream(data, 1, 4).toList();
        assertEquals(Arrays.asList((byte) 20, (byte) 30, (byte) 40), result);

        assertTrue(createByteStream(data, 0, 0).toList().isEmpty());
    }

    @Test
    public void testOfCollection() {
        List<Byte> list = Arrays.asList((byte) 1, (byte) 2, (byte) 3);
        ByteStream stream = ByteStream.of(list);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream.toArray());

        List<Byte> emptyList = Arrays.asList();
        ByteStream emptyStream = ByteStream.of(emptyList);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testOfFile() throws IOException {
        File tempFile = File.createTempFile("test", ".tmp");
        tempFile.deleteOnExit();

        byte[] data = { 1, 2, 3, 4, 5 };
        IOUtil.write(data, tempFile);

        ByteStream stream = ByteStream.of(tempFile);
        assertArrayEquals(data, stream.toArray());
    }

    @Test
    public void testOfInputStream() throws IOException {
        byte[] data = { 1, 2, 3, 4, 5 };
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);

        ByteStream stream = ByteStream.of(inputStream);
        assertArrayEquals(data, stream.toArray());

        ByteArrayInputStream inputStream2 = new ByteArrayInputStream(data);
        ByteStream stream2 = ByteStream.of(inputStream2, true);
        assertArrayEquals(data, stream2.toArray());

        ByteStream nullStream = ByteStream.of((InputStream) null);
        assertEquals(0, nullStream.count());
    }

    @Test
    public void testOfIterator() {
        ByteIterator iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(5, ByteStream.of(iter).count());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(4, ByteStream.of(iter).skip(1).count());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of(iter).toArray());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of(iter).skip(1).toArray());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(iter).toList());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(iter).skip(1).toList());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(5, ByteStream.of(iter).map(e -> e).count());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(4, ByteStream.of(iter).map(e -> e).skip(1).count());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of(iter).map(e -> e).toArray());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of(iter).map(e -> e).skip(1).toArray());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(iter).map(e -> e).toList());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(iter).map(e -> e).skip(1).toList());
    }

    // ofNullable

    @Test
    public void testOfNullable() {
        ByteStream stream1 = ByteStream.ofNullable(null);
        assertEquals(0, stream1.count());

        ByteStream stream2 = ByteStream.ofNullable((byte) 5);
        assertEquals(5, stream2.findFirst().get());

        ByteStream stream3 = ByteStream.ofNullable((byte) 0);
        assertEquals(0, stream3.findFirst().get());
    }

    @Test
    public void testOfNullable_NonNull() {
        ByteStream stream = ByteStream.ofNullable((byte) 5);
        byte[] result = stream.toArray();
        assertArrayEquals(new byte[] { 5 }, result);
    }

    @Test
    public void testOfNullable_Null() {
        ByteStream stream = ByteStream.ofNullable(null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullableWithNonNull() {
        ByteStream stream = ByteStream.ofNullable((byte) 42);
        assertEquals(1, stream.count());
    }

    @Test
    public void testOfNullByteBuffer() {
        ByteStream stream = ByteStream.of((ByteBuffer) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullByteIterator() {
        ByteStream stream = ByteStream.of((ByteIterator) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullInputStream() {
        ByteStream stream = ByteStream.of((InputStream) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRandom() {
        ByteStream stream = ByteStream.random().limit(10);
        assertEquals(10, stream.count());

        ByteStream stream2 = ByteStream.random().limit(5);
        byte[] random = stream2.toArray();
        assertEquals(5, random.length);
    }

    @Test
    public void testRandom_Basic() {
        byte[] result = ByteStream.random().limit(10).toArray();
        assertEquals(10, result.length);
    }

    // range

    @Test
    public void testRange() {
        ByteStream stream = ByteStream.range((byte) 1, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.toArray());

        ByteStream emptyStream = ByteStream.range((byte) 5, (byte) 5);
        assertEquals(0, emptyStream.count());

        ByteStream negStream = ByteStream.range((byte) -3, (byte) 0);
        assertArrayEquals(new byte[] { -3, -2, -1 }, negStream.toArray());
    }

    @Test
    public void testRange_Basic() {
        byte[] result = ByteStream.range((byte) 1, (byte) 5).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testRange_EmptyRange() {
        byte[] result = ByteStream.range((byte) 5, (byte) 5).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testRangeBy() {
        List<Byte> result = ByteStream.range((byte) 1, (byte) 10, (byte) 2).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5, (byte) 7, (byte) 9), result);

        result = ByteStream.range((byte) 10, (byte) 1, (byte) -2).toList();
        assertEquals(Arrays.asList((byte) 10, (byte) 8, (byte) 6, (byte) 4, (byte) 2), result);

        assertTrue(ByteStream.range((byte) 1, (byte) 5, (byte) -1).toList().isEmpty());
    }

    @Test
    public void testRangeEmptyRange() {
        assertEquals(0, ByteStream.range((byte) 5, (byte) 5).count());
    }

    @Test
    public void testRangeWithInvalidStepDirection() {
        ByteStream stream = ByteStream.range((byte) 1, (byte) 10, (byte) -1);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRangeWithStep() {
        ByteStream stream = ByteStream.range((byte) 1, (byte) 10, (byte) 2);
        assertArrayEquals(new byte[] { 1, 3, 5, 7, 9 }, stream.toArray());

        ByteStream stream2 = ByteStream.range((byte) 1, (byte) 4, (byte) 1);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream2.toArray());
    }

    @Test
    public void testRangeMap() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11);
        assertArrayEquals(new byte[] { 1, 10 }, stream.rangeMap((first, next) -> next - first <= 2, (first, last) -> first).toArray());
    }

    @Test
    public void testRangeMapToObj() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11);
        List<String> result = stream.rangeMapToObj((first, next) -> next - first <= 2, (first, last) -> first + "-" + last).toList();
        assertEquals(2, result.size());
    }

    // repeat

    @Test
    public void testRepeat() {
        ByteStream stream = ByteStream.repeat((byte) 5, 3);
        assertArrayEquals(new byte[] { 5, 5, 5 }, stream.toArray());

        ByteStream emptyStream = ByteStream.repeat((byte) 1, 0);
        assertEquals(0, emptyStream.count());

        assertThrows(IllegalArgumentException.class, () -> ByteStream.repeat((byte) 1, -1));
    }

    @Test
    public void testRepeat_Basic() {
        byte[] result = ByteStream.repeat((byte) 7, 4).toArray();
        assertArrayEquals(new byte[] { 7, 7, 7, 7 }, result);
    }

    @Test
    public void testRepeat_Negative() {
        assertThrows(IllegalArgumentException.class, () -> ByteStream.repeat((byte) 7, -1));
    }

    @Test
    public void testRepeat_Zero() {
        byte[] result = ByteStream.repeat((byte) 7, 0).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testRepeatNegative() {
        assertThrows(IllegalArgumentException.class, () -> ByteStream.repeat((byte) 5, -1));
    }

    @Test
    public void testRepeatZero() {
        ByteStream stream = ByteStream.repeat((byte) 5, 0);
        assertEquals(0, stream.count());
    }

    // toArray



    // G090-01: empty / fully skipped / read()==0 inputs
    @Test
    public void testOfInputStream_bulkToArrayCountToList_emptyAndEdge() {
        final byte[] empty = ByteStream.of(new ByteArrayInputStream(new byte[0])).toArray();
        assertEquals(0, empty.length);
        assertEquals(0, ByteStream.of(new ByteArrayInputStream(new byte[0])).count());
        assertEquals(0, ByteStream.of(new ByteArrayInputStream(new byte[0])).toByteList().size());

        final ByteList list = ByteStream.of(new ByteArrayInputStream(new byte[0])).toByteList();
        list.add((byte) 5);
        assertEquals(ByteList.of((byte) 5), list);

        final byte[] data = g090Data(10);
        assertEquals(0, ByteStream.of(new ByteArrayInputStream(data)).skip(10).toArray().length);
        assertEquals(0, ByteStream.of(new ByteArrayInputStream(data)).skip(100).count());
        assertArrayEquals(new byte[] { data[9] }, ByteStream.of(new ByteArrayInputStream(data)).skip(9).toArray());

        final ByteList mutable = ByteStream.of(new ByteArrayInputStream(data)).toByteList();
        mutable.add((byte) 1);
        assertEquals(11, mutable.size());

        final InputStream zeroRead = new InputStream() {
            @Override
            public int read() {
                return 1;
            }

            @Override
            public int read(final byte[] b, final int off, final int len) {
                return 0;
            }
        };

        assertEquals(0, ByteStream.of(zeroRead).toArray().length);
        assertEquals(0, ByteStream.of(zeroRead).count());
    }



    // G090-01: bulk toArray/toByteList/count of of(InputStream) - same content and same read(buf) calls as element-wise iteration
    @Test
    public void testOfInputStream_bulkToArrayCountToList_multiChunk() {
        final byte[] data = g090Data(20000);

        G090ChunkedInputStream is = new G090ChunkedInputStream(data, 3000, -1);
        assertArrayEquals(data, ByteStream.of(is).toArray());
        assertEquals(8, is.readCalls);

        is = new G090ChunkedInputStream(data, 3000, -1);
        assertEquals(20000, ByteStream.of(is).count());
        assertEquals(8, is.readCalls);

        is = new G090ChunkedInputStream(data, 3000, -1);
        assertEquals(ByteList.of(data), ByteStream.of(is).toByteList());
        assertEquals(8, is.readCalls);

        // start in the middle of a buffered chunk
        is = new G090ChunkedInputStream(data, 3000, -1);
        assertArrayEquals(Arrays.copyOfRange(data, 4500, 20000), ByteStream.of(is).skip(4500).toArray());
        assertEquals(8, is.readCalls);

        is = new G090ChunkedInputStream(data, 3000, -1);
        assertEquals(20000 - 2999, ByteStream.of(is).skip(2999).count());
        assertEquals(8, is.readCalls);

        // larger than the internal 8192-byte buffer, full reads
        final byte[] big = g090Data(3 * 8192 + 17);
        assertArrayEquals(big, ByteStream.of(new ByteArrayInputStream(big)).toArray());
        assertEquals(big.length, ByteStream.of(new ByteArrayInputStream(big)).count());
        assertArrayEquals(Arrays.copyOfRange(big, 8192, big.length), ByteStream.of(new ByteArrayInputStream(big)).skip(8192).toArray());
        assertEquals(ByteList.of(Arrays.copyOfRange(big, 1, big.length)), ByteStream.of(new ByteArrayInputStream(big)).skip(1).toByteList());
    }



    // G090-01: an IOException in a later read still surfaces as UncheckedIOException from toArray/count/toByteList
    @Test
    public void testOfInputStream_bulkToArrayCountToList_readFailure() {
        final byte[] data = g090Data(10000);

        final G090ChunkedInputStream is1 = new G090ChunkedInputStream(data, 3000, 2);
        assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> ByteStream.of(is1).toArray());
        assertEquals(3, is1.readCalls);

        final G090ChunkedInputStream is2 = new G090ChunkedInputStream(data, 3000, 2);
        assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> ByteStream.of(is2).count());
        assertEquals(3, is2.readCalls);

        final G090ChunkedInputStream is3 = new G090ChunkedInputStream(data, 3000, 0);
        assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> ByteStream.of(is3).toByteList());
        assertEquals(1, is3.readCalls);
    }

}
