package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.u.Optional;

/** ShortStream creation contracts. */
public class ShortStreamCreationTest extends ShortStreamTestSupport {

    // defer

    @Test
    public void testDefer() {
        final AtomicInteger counter = new AtomicInteger(0);
        ShortStream stream = ShortStream.defer(() -> {
            counter.incrementAndGet();
            return ShortStream.of((short) 1, (short) 2, (short) 3);
        });

        assertEquals(0, counter.get());

        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
        assertEquals(1, counter.get());
    }

    @Test
    public void testDefer_supplierReturningNullProducesEmptyStream() {
        final AtomicInteger supplierCalls = new AtomicInteger();
        final ShortStream deferred = ShortStream.defer(() -> {
            supplierCalls.incrementAndGet();
            return null;
        });

        assertEquals(0, deferred.count());
        assertEquals(1, supplierCalls.get());
    }

    @Test
    public void testDeferRejectsNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ShortStream.defer(null));
    }

    // empty

    @Test
    public void testEmpty() {
        ShortStream stream = ShortStream.empty();
        assertEquals(0, stream.count());

        ShortStream stream2 = ShortStream.empty();
        assertArrayEquals(new short[0], stream2.toArray());
    }

    @Test
    public void testEmptyStreamAverage() {
        assertFalse(ShortStream.empty().average().isPresent());
    }

    @Test
    public void testEmptyStreamMax() {
        assertFalse(ShortStream.empty().max().isPresent());
    }

    @Test
    public void testEmptyStreamMin() {
        assertFalse(ShortStream.empty().min().isPresent());
    }

    @Test
    public void testEmptyStreamOperations() {
        ShortStream stream = ShortStream.empty().filter(n -> true).map(n -> (short) (n * 2)).sorted().distinct();

        assertEquals(0, stream.count());
    }

    @Test
    public void testEmptyStreamReduce() {
        assertFalse(ShortStream.empty().reduce((a, b) -> (short) (a + b)).isPresent());
    }

    @Test
    public void testEmptyStreamSum() {
        assertEquals(0, ShortStream.empty().sum());
    }

    @Test
    public void testEmptyStreamSummaryStatistics() {
        ShortSummaryStatistics stats = ShortStream.empty().summaryStatistics();
        assertNotNull(stats);
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testEmptyStreamSummaryStatisticsAndPercentiles() {
        Pair<ShortSummaryStatistics, Optional<Map<Percentage, Short>>> result = ShortStream.empty().summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertEquals(0, result.left().getCount());
    }

    // flatten

    @Test
    public void testFlatten() {
        short[][] array2d = { { 1, 2 }, { 3, 4 }, { 5 } };
        ShortStream stream1 = ShortStream.flatten(array2d);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream1.toArray());

        short[][][] array3d = { { { 1, 2 }, { 3 } }, { { 4, 5 } } };
        ShortStream stream2 = ShortStream.flatten(array3d);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream2.toArray());
    }

    @Test
    public void testFlatten2D() {
        short[][] array = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        ShortStream stream = ShortStream.flatten(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());

        ShortStream stream2 = ShortStream.flatten(new short[0][]);
        assertEquals(0, stream2.count());
    }

    @Test
    @DisplayName("Test flatten(short[][], boolean) method")
    public void testFlatten2DVertically() {
        short[][] array2D = { { 1, 2, 3 }, { 4, 5, 6 } };
        ShortStream stream = ShortStream.flatten(array2D, true);
        assertArrayEquals(new short[] { 1, 4, 2, 5, 3, 6 }, stream.toArray());
    }

    @Test
    @DisplayName("Test flatten(short[][], short, boolean) method with alignment")
    public void testFlatten2DWithAlignment() {
        short[][] array2D = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        ShortStream stream = ShortStream.flatten(array2D, (short) 0, false);
        assertArrayEquals(new short[] { 1, 2, 0, 3, 4, 5, 6, 0, 0 }, stream.toArray());
    }

    @Test
    public void testFlatten3D() {
        short[][][] array = { { { 1, 2 }, { 3 } }, { { 4, 5, 6 } } };
        ShortStream stream = ShortStream.flatten(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testFlattenVertically() {
        short[][] array = { { 1, 2, 3 }, { 4, 5, 6 } };

        ShortStream stream1 = ShortStream.flatten(array, false);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream1.toArray());

        ShortStream stream2 = ShortStream.flatten(array, true);
        assertArrayEquals(new short[] { 1, 4, 2, 5, 3, 6 }, stream2.toArray());
    }



    // G114-01: flatten(short[][], true) keeps the original walk for dense input and uses a compacted row walk when
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
            final short[][] a = new short[rows][];

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
                    a[i] = new short[len];

                    for (int j = 0; j < len; j++) {
                        a[i][j] = (short) (i * 100 + j);
                    }
                }
            }

            assertFlattenVerticallyMatchesReferenceG114(a, random.nextInt(rows * 3 + 2));
        }

        // rows * longest row == 4 * elements keeps the original walk; one more empty row switches to the compacted walk
        final short[][] boundary = { { 1, 2, 3, 4 }, null, {}, { 5, 6, 7, 8 }, null, {}, {}, {} };
        final short[][] boundaryPlusOne = { { 1, 2, 3, 4 }, null, {}, { 5, 6, 7, 8 }, null, {}, {}, {}, {} };
        assertArrayEquals(new short[] { 1, 5, 2, 6, 3, 7, 4, 8 }, ShortStream.flatten(boundary, true).toArray());
        assertArrayEquals(new short[] { 1, 5, 2, 6, 3, 7, 4, 8 }, ShortStream.flatten(boundaryPlusOne, true).toArray());

        for (int skip = 0; skip <= 9; skip++) {
            assertFlattenVerticallyMatchesReferenceG114(boundary, skip);
            assertFlattenVerticallyMatchesReferenceG114(boundaryPlusOne, skip);
        }

        // sparse: rows drop out of the compacted walk at different columns
        final short[][] sparse = { null, {}, { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, {}, {}, { 10, 11 }, {}, null, {}, {}, { 12, 13, 14, 15 }, {} };
        assertArrayEquals(new short[] { 1, 10, 12, 2, 11, 13, 3, 14, 4, 15, 5, 6, 7, 8, 9 }, ShortStream.flatten(sparse, true).toArray());
        assertFlattenVerticallyMatchesReferenceG114(sparse, 4);

        // one long row among 999 null rows (first and last position)
        final short[][] oneLongRow = new short[1000][];
        oneLongRow[999] = new short[5000];

        for (int j = 0; j < 5000; j++) {
            oneLongRow[999][j] = (short) j;
        }

        assertArrayEquals(oneLongRow[999], ShortStream.flatten(oneLongRow, true).toArray());
        oneLongRow[0] = oneLongRow[999];
        oneLongRow[999] = null;
        assertFlattenVerticallyMatchesReferenceG114(oneLongRow, 4999);
        assertEquals(0, ShortStream.flatten(new short[][] { null, {}, null, {} }, true).count());
    }



    // G105-01: iterator exhaustion and a strongly jagged input (one long row, many single-element and null rows)
    @Test
    public void testFlattenVertically_iteratorExhaustionAndLongRow() {
        final short[][] a = { null, { 1 }, {}, { 2, 3, 4 }, null, { 5, 6 } };
        final ShortIterator iter = ShortStream.flatten(a, true).iterator();
        final ShortList actual = new ShortList();

        while (iter.hasNext()) {
            actual.add(iter.nextShort());
        }

        assertArrayEquals(new short[] { 1, 2, 5, 3, 6, 4 }, actual.toArray());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextShort);
        assertFalse(iter.hasNext());

        final short[][] jagged = new short[201][];

        for (int i = 0; i < 200; i++) {
            jagged[i] = i % 3 == 0 ? null : new short[] { (short) (100 + i) };
        }

        jagged[200] = new short[5000];

        for (int i = 0; i < 5000; i++) {
            jagged[200][i] = (short) (i % 1000);
        }

        final short[] result = ShortStream.flatten(jagged, true).toArray();
        final ShortList expected = new ShortList();

        for (int i = 0; i < 200; i++) {
            if (jagged[i] != null) {
                expected.add(jagged[i][0]);
            }
        }

        for (int i = 0; i < 5000; i++) {
            expected.add(jagged[200][i]);
        }

        assertArrayEquals(expected.toArray(), result);
        assertArrayEquals(new short[0], ShortStream.flatten(new short[][] { null, {}, null }, true).toArray());
    }



    // G105-01: pins the column-major order of flatten(short[][], true) for jagged input with null/empty/short rows
    @Test
    public void testFlattenVertically_jaggedNullEmptyRowsMatchesColumnMajorReference() {
        final Random random = new Random(20260926L);

        for (int round = 0; round < 300; round++) {
            final int rows = 2 + random.nextInt(8);
            final short[][] a = new short[rows][];

            for (int r = 0; r < rows; r++) {
                final int kind = random.nextInt(6);
                a[r] = kind == 0 ? null : new short[kind == 1 ? 0 : random.nextInt(7)];

                if (a[r] != null) {
                    for (int c = 0; c < a[r].length; c++) {
                        a[r][c] = (short) (random.nextInt(2000) - 1000);
                    }
                }
            }

            int maxLen = 0;

            for (final short[] row : a) {
                maxLen = Math.max(maxLen, row == null ? 0 : row.length);
            }

            final ShortList expected = new ShortList();

            for (int c = 0; c < maxLen; c++) {
                for (final short[] row : a) {
                    if (row != null && c < row.length) {
                        expected.add(row[c]);
                    }
                }
            }

            assertArrayEquals(expected.toArray(), ShortStream.flatten(a, true).toArray());
            assertEquals(expected.size(), ShortStream.flatten(a, true).count());

            if (expected.size() > 1) {
                assertArrayEquals(expected.copy(1, expected.size()).toArray(), ShortStream.flatten(a, true).skip(1).toArray());
            }
        }
    }

    @Test
    public void testFlattenVerticallyWithJaggedArray() {
        short[][] array = { { 1, 2, 3 }, { 4 }, { 5, 6 } };

        ShortStream stream = ShortStream.flatten(array, true);
        assertArrayEquals(new short[] { 1, 4, 5, 2, 6, 3 }, stream.toArray());
    }

    @Test
    public void testFlattenWithAlignment() {
        short[][] array = { { 1, 2 }, { 3 }, { 4, 5, 6 } };
        short valueForAlignment = 0;

        ShortStream stream = ShortStream.flatten(array, valueForAlignment, true);
        assertArrayEquals(new short[] { 1, 3, 4, 2, 0, 5, 0, 0, 6 }, stream.toArray());
    }

    @Test
    public void testFlattenWithAlignmentHorizontally() {
        short[][] array = { { 1, 2 }, { 3 }, { 4, 5, 6 } };
        short valueForAlignment = 0;

        ShortStream stream = ShortStream.flatten(array, valueForAlignment, false);
        assertArrayEquals(new short[] { 1, 2, 0, 3, 0, 0, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testFlattenWithAlignmentVertically() {
        short[][] array = { { 1, 2 }, { 3 }, { 4, 5, 6 } };
        ShortStream stream = ShortStream.flatten(array, (short) 0, true);
        assertArrayEquals(new short[] { 1, 3, 4, 2, 0, 5, 0, 0, 6 }, stream.toArray());
    }

    @Test
    public void testFlattMapToObj() {
        Stream<String> stream = createShortStream((short) 1, (short) 2).flatMapArrayToObj(n -> new String[] { "A" + n, "B" + n });
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2" }, stream.toArray());
    }

    @Test
    public void testFlattmapToObj() {
        Stream<String> stream = ShortStream.of((short) 1, (short) 2).flatMapArrayToObj(n -> new String[] { "A" + n, "B" + n });
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2" }, stream.toArray());
    }

    @Test
    @DisplayName("Test flatMapArrayToObj() with array")
    public void testFlattMapToObjArray() {
        Stream<String> flattened = stream.flatMapArrayToObj(n -> new String[] { "A" + n, "B" + n });
        List<String> result = flattened.toList();
        assertEquals(Arrays.asList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"), result);
    }

    @Test
    public void testGenerate() {
        final short[] value = { 0 };
        ShortStream stream = ShortStream.generate(() -> value[0]++).limit(3);
        assertArrayEquals(new short[] { 0, 1, 2 }, stream.toArray());
    }

    @Test
    public void testGenerate_Null() {
        assertThrows(IllegalArgumentException.class, () -> ShortStream.generate(null).limit(1).count());
    }

    // iterate

    @Test
    @DisplayName("Test iterate() methods")
    public void testIterate() {
        final int[] counter = { 0 };
        ShortStream stream1 = ShortStream.iterate(() -> counter[0] < 5, () -> (short) (counter[0]++));
        assertArrayEquals(new short[] { 0, 1, 2, 3, 4 }, stream1.toArray());

        ShortStream stream2 = ShortStream.iterate((short) 1, () -> true, n -> (short) (n * 2)).limit(5);
        assertArrayEquals(new short[] { 1, 2, 4, 8, 16 }, stream2.toArray());

        ShortStream stream3 = ShortStream.iterate((short) 1, n -> n < 10, n -> (short) (n + 2));
        assertArrayEquals(new short[] { 1, 3, 5, 7, 9 }, stream3.toArray());

        ShortStream stream4 = ShortStream.iterate((short) 1, n -> (short) (n + 1)).limit(5);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream4.toArray());
    }

    @Test
    public void testIterateUnaryOperator() {
        short[] result = ShortStream.iterate((short) 1, i -> (short) (i + 1)).limit(5).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testIterateWithBooleanSupplierAndInitialValue() {
        final int[] count = { 0 };
        ShortStream stream = ShortStream.iterate((short) 5, () -> count[0]++ < 3, n -> (short) (n + 1));
        assertArrayEquals(new short[] { 5, 6, 7 }, stream.toArray());
    }

    @Test
    public void testIterateWithHasNext() {
        final int[] count = { 0 };
        ShortStream stream = ShortStream.iterate(() -> count[0] < 3, () -> (short) count[0]++);
        assertArrayEquals(new short[] { 0, 1, 2 }, stream.toArray());
    }

    @Test
    public void testIterateWithImmediateFalseHasNext() {
        ShortStream stream = ShortStream.iterate(() -> false, () -> (short) 1);
        assertEquals(0, stream.count());

        ShortStream stream2 = ShortStream.iterate((short) 1, () -> false, n -> (short) (n + 1));
        assertEquals(0, stream2.count());

        ShortStream stream3 = ShortStream.iterate((short) 1, n -> false, n -> (short) (n + 1));
        assertEquals(0, stream3.count());
    }

    @Test
    public void testIterateWithPredicate() {
        short[] result = ShortStream.iterate((short) 1, i -> i < 10, i -> (short) (i * 2)).toArray();
        assertArrayEquals(new short[] { 1, 2, 4, 8 }, result);
    }

    @Test
    public void testIterateWithSeed() {
        ShortStream stream = ShortStream.iterate((short) 1, n -> (short) (n * 2)).limit(4);
        assertArrayEquals(new short[] { 1, 2, 4, 8 }, stream.toArray());
    }

    @Test
    public void testIterateWithSeedAndHasNext() {
        ShortStream stream = ShortStream.iterate((short) 1, () -> true, n -> (short) (n * 2)).limit(4);
        assertArrayEquals(new short[] { 1, 2, 4, 8 }, stream.toArray());
    }

    @Test
    public void testIterateWithSeedAndPredicate() {
        ShortStream stream = ShortStream.iterate((short) 1, n -> n < 10, n -> (short) (n * 2));
        assertArrayEquals(new short[] { 1, 2, 4, 8 }, stream.toArray());
    }

    @Test
    public void testOfIterator() {
        ShortList list = new ShortList(new short[] { 1, 2, 3 });
        ShortStream stream = ShortStream.of(list.iterator());
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());

        ShortStream stream2 = ShortStream.of((ShortIterator) null);
        assertEquals(0, stream2.count());
    }

    @Test
    public void testOfArray() {
        ShortStream stream1 = ShortStream.of();
        assertEquals(0, stream1.count());

        ShortStream stream2 = ShortStream.of((short) 1);
        assertArrayEquals(new short[] { 1 }, stream2.toArray());

        ShortStream stream3 = ShortStream.of((short) 1, (short) 2, (short) 3);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream3.toArray());
    }

    @Test
    @DisplayName("Test of(short[], int, int) method")
    public void testOfArrayRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortStream stream = createShortStream(array, 1, 4);
        assertArrayEquals(new short[] { 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testOfArrayWithRange() {
        short[] array = { 1, 2, 3, 4, 5 };

        ShortStream stream1 = ShortStream.of(array, 0, 5);
        assertArrayEquals(array, stream1.toArray());

        ShortStream stream2 = ShortStream.of(array, 1, 4);
        assertArrayEquals(new short[] { 2, 3, 4 }, stream2.toArray());

        ShortStream stream3 = ShortStream.of(array, 2, 2);
        assertEquals(0, stream3.count());
    }

    @Test
    public void testOfBoxedArray() {
        Short[] boxed = { 1, 2, 3, 4, 5 };
        ShortStream stream = ShortStream.of(boxed);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream.toArray());

        ShortStream stream2 = ShortStream.of(boxed, 1, 3);
        assertArrayEquals(new short[] { 2, 3 }, stream2.toArray());
    }

    @Test
    public void testOfBoxedArrayWithRange() {
        Short[] boxed = { 10, 20, 30, 40, 50 };
        ShortStream stream = ShortStream.of(boxed, 1, 4);
        assertArrayEquals(new short[] { 20, 30, 40 }, stream.toArray());
    }

    @Test
    public void testOfBuffer() {
        ShortBuffer buffer = ShortBuffer.wrap(new short[] { 1, 2, 3, 4, 5 });
        buffer.position(1).limit(4);
        assertArrayEquals(new short[] { 2, 3, 4 }, createShortStream(buffer).toArray());
    }

    @Test
    public void testOfCollection() {
        List<Short> list = Arrays.asList((short) 1, (short) 2, (short) 3);
        ShortStream stream = ShortStream.of(list);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
    }

    // ofNullable

    @Test
    public void testOfNullable() {
        ShortStream stream1 = ShortStream.ofNullable(null);
        assertEquals(0, stream1.count());

        ShortStream stream2 = ShortStream.ofNullable((short) 42);
        assertArrayEquals(new short[] { 42 }, stream2.toArray());
    }

    @Test
    public void testOfNullableWithNonNull() {
        ShortStream stream = ShortStream.ofNullable((short) 42);
        assertEquals(1, stream.count());
    }

    @Test
    public void testOfNullableWithNull() {
        ShortStream stream = ShortStream.ofNullable(null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullShortBuffer() {
        ShortStream stream = ShortStream.of((ShortBuffer) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullShortIterator() {
        ShortStream stream = ShortStream.of((ShortIterator) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfShortArray() {
        Short[] array = { 1, 2, 3, 4 };
        ShortStream stream = createShortStream(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testOfShortArrayWithRange() {
        Short[] array = { 1, 2, 3, 4, 5 };
        ShortStream stream = createShortStream(array, 1, 4);
        assertArrayEquals(new short[] { 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testOfShortBuffer() {
        ShortBuffer buffer = ShortBuffer.wrap(new short[] { 1, 2, 3, 4, 5 });
        buffer.position(1);
        buffer.limit(4);

        ShortStream stream = ShortStream.of(buffer);
        assertArrayEquals(new short[] { 2, 3, 4 }, stream.toArray());

        ShortStream stream2 = ShortStream.of((ShortBuffer) null);
        assertEquals(0, stream2.count());
    }

    @Test
    @DisplayName("Test of(Short[]) method")
    public void testOfShortObjectArray() {
        Short[] array = { 1, 2, 3, 4, 5 };
        ShortStream stream = createShortStream(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testOfVarargs() {
        assertEquals(0, createShortStream().count());
        assertArrayEquals(new short[] { 1, 2, 3 }, createShortStream((short) 1, (short) 2, (short) 3).toArray());
    }

    @Test
    public void testRandom() {
        ShortStream stream = ShortStream.random().limit(10);
        assertEquals(10, stream.count());

        ShortStream stream2 = ShortStream.random().limit(5);
        short[] values = stream2.toArray();
        assertEquals(5, values.length);
    }

    // range

    @Test
    public void testRange() {
        ShortStream stream1 = ShortStream.range((short) 1, (short) 4);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream1.toArray());

        ShortStream stream2 = ShortStream.range((short) 5, (short) 5);
        assertEquals(0, stream2.count());

        ShortStream stream3 = ShortStream.range((short) 5, (short) 3);
        assertEquals(0, stream3.count());
    }

    @Test
    public void testRange_Descending() {
        short[] result = ShortStream.range((short) 5, (short) 1).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testRange_Same() {
        short[] result = ShortStream.range((short) 3, (short) 3).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testRangeEmptyRange() {
        assertEquals(0, ShortStream.range((short) 5, (short) 5).count());
    }

    @Test
    public void testRangeIteratorFailedNextDoesNotReduceRemainingCount() {
        final ShortIterator iter = ShortStream.range((short) 0, (short) 1).iterator();

        assertEquals((short) 0, iter.nextShort());
        assertThrows(NoSuchElementException.class, iter::nextShort);
        assertEquals(0L, iter.count());
    }

    @Test
    public void testRangeWithInvalidStepDirection() {
        // Positive step but start > end should return empty
        ShortStream stream = ShortStream.range((short) 10, (short) 1, (short) 2);
        assertEquals(0, stream.count());

        // Negative step but start < end should return empty
        ShortStream stream2 = ShortStream.range((short) 1, (short) 10, (short) -2);
        assertEquals(0, stream2.count());
    }

    @Test
    public void testRangeWithStep() {
        ShortStream stream1 = ShortStream.range((short) 0, (short) 10, (short) 3);
        assertArrayEquals(new short[] { 0, 3, 6, 9 }, stream1.toArray());

        ShortStream stream2 = ShortStream.range((short) 10, (short) 0, (short) -3);
        assertArrayEquals(new short[] { 10, 7, 4, 1 }, stream2.toArray());

        ShortStream stream3 = ShortStream.range((short) 1, (short) 5, (short) 1);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream3.toArray());
    }

    @Test
    public void testRangeWithZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> ShortStream.range((short) 0, (short) 10, (short) 0));
    }

    // rangeMap

    @Test
    public void testRangeMap() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 10, (short) 11, (short) 20)
                .rangeMap((first, next) -> next - first <= 2, (first, last) -> (short) (first + last));
        assertArrayEquals(new short[] { 4, 21, 40 }, stream.toArray());
    }

    @Test
    public void testRangeMap_Empty() {
        short[] result = ShortStream.empty().rangeMap((first, next) -> next - first == 1, (first, last) -> (short) (first + last)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testRangeMap_SingleElement() {
        short[] result = createShortStream((short) 5).rangeMap((first, next) -> next - first == 1, (first, last) -> (short) (first + last)).toArray();
        assertArrayEquals(new short[] { 10 }, result);
    }

    @Test
    public void testRangeMapToObj() {
        Stream<String> stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 10)
                .rangeMapToObj((first, next) -> next - first <= 2, (first, last) -> first + "-" + last);
        assertArrayEquals(new String[] { "1-3", "10-10" }, stream.toArray());
    }

    @Test
    public void testRangeMapToObj_Empty() {
        List<String> result = ShortStream.empty().rangeMapToObj((first, next) -> next - first == 1, (first, last) -> first + "-" + last).toList();
        assertTrue(result.isEmpty());
    }

    // repeat

    @Test
    public void testRepeat() {
        ShortStream stream1 = ShortStream.repeat((short) 5, 3);
        assertArrayEquals(new short[] { 5, 5, 5 }, stream1.toArray());

        ShortStream stream2 = ShortStream.repeat((short) 5, 0);
        assertEquals(0, stream2.count());

        ShortStream stream3 = ShortStream.repeat((short) 7, 1);
        assertArrayEquals(new short[] { 7 }, stream3.toArray());
    }

    @Test
    public void testRepeatNegative() {
        assertThrows(IllegalArgumentException.class, () -> ShortStream.repeat((short) 5, -1));
    }

    @Test
    public void testRepeatZero() {
        ShortStream stream = ShortStream.repeat((short) 5, 0);
        assertEquals(0, stream.count());
    }

}
