package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.DoubleBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleFunction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalDouble;

/** DoubleStream creation contracts. */
public class DoubleStreamCreationTest extends DoubleStreamTestSupport {

    // defer

    @Test
    public void testDefer() {
        AtomicInteger counter = new AtomicInteger(0);
        DoubleStream stream = DoubleStream.defer(() -> {
            counter.incrementAndGet();
            return DoubleStream.of(1.0, 2.0, 3.0);
        });
        assertEquals(0, counter.get());
        double[] result = stream.toArray();
        assertEquals(1, counter.get());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);
    }

    @Test
    public void testDefer_supplierReturningNullProducesEmptyStream() {
        final AtomicInteger supplierCalls = new AtomicInteger();
        final DoubleStream deferred = DoubleStream.defer(() -> {
            supplierCalls.incrementAndGet();
            return null;
        });

        assertEquals(0, deferred.count());
        assertEquals(1, supplierCalls.get());
    }

    @Test
    public void testDeferRejectsNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.defer(null));
    }

    // empty

    @Test
    public void testEmpty() {
        DoubleStream stream = DoubleStream.empty();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    public void testEmptyStreamOperations() {
        DoubleStream empty = DoubleStream.empty();
        assertEquals(0, empty.count());

        DoubleStream empty2 = DoubleStream.empty();
        assertFalse(empty2.findFirst(x -> true).isPresent());

        DoubleStream empty3 = DoubleStream.empty();
        assertTrue(empty3.allMatch(x -> false));

        DoubleStream empty4 = DoubleStream.empty();
        assertFalse(empty4.anyMatch(x -> true));

        DoubleStream empty5 = DoubleStream.empty();
        assertTrue(empty5.noneMatch(x -> true));
    }

    @Test
    public void testOfOptionalDouble_Empty() {
        DoubleStream result = DoubleStream.of(OptionalDouble.empty());
        assertEquals(0, result.count());
    }

    // flatten

    @Test
    public void testFlatten2D() {
        double[][] array = { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0 } };
        DoubleStream stream = DoubleStream.flatten(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, stream.toArray(), 0.0001);
    }

    @Test
    @DisplayName("flatten() should flatten two-dimensional array")
    public void testFlatten2D_2() {
        double[][] array = { { 1.0d, 2.0d }, { 3.0d, 4.0d } };
        double[] result = DoubleStream.flatten(array, false).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d }, result);

        result = DoubleStream.flatten(array, true).toArray();
        assertArrayEquals(new double[] { 1.0d, 3.0d, 2.0d, 4.0d }, result);
    }

    @Test
    @DisplayName("flatten() should flatten two-dimensional array")
    public void testFlatten2D_3() {
        double[][] array = { { 1.0d, 2.0d }, { 3.0d, 4.0d, 5.f } };
        double[] result = DoubleStream.flatten(array, 0.0d, false).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 0.0d, 3.0d, 4.0d, 5.0d }, result);

        result = DoubleStream.flatten(array, 0.0d, true).toArray();
        assertArrayEquals(new double[] { 1.0d, 3.0d, 2.0d, 4.0d, 0.0d, 5.0d }, result);
    }

    @Test
    public void testFlatten2DVertical() {
        double[][] array = { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0 } };
        DoubleStream stream = DoubleStream.flatten(array, true);
        assertArrayEquals(new double[] { 1.0, 3.0, 5.0, 2.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testFlatten2DWithAlignment() {
        double[][] array = { { 1.0, 2.0 }, { 3.0 }, { 4.0, 5.0, 6.0 } };
        DoubleStream stream = DoubleStream.flatten(array, 0.0, false);
        assertArrayEquals(new double[] { 1.0, 2.0, 0.0, 3.0, 0.0, 0.0, 4.0, 5.0, 6.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testFlatten2DWithAlignmentVertical() {
        double[][] array = { { 1.0, 2.0 }, { 3.0 }, { 4.0, 5.0 } };
        DoubleStream stream = DoubleStream.flatten(array, 0.0, true);
        assertEquals(6, stream.count());
    }

    @Test
    public void testFlatten3D() {
        double[][][] array = { { { 1.0, 2.0 }, { 3.0 } }, { { 4.0 }, { 5.0, 6.0 } } };
        DoubleStream stream = DoubleStream.flatten(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testFlatten_3D() {
        double[][][] a = { { { 1.0, 2.0 }, { 3.0, 4.0 } }, { { 5.0, 6.0 } } };
        double[] result = DoubleStream.flatten(a).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, result, 0.001);
    }

    @Test
    public void testFlatten_WithAlignment2() {
        double[][] a = { { 1.0, 2.0, 3.0 }, { 4.0, 5.0 } };
        double[] result = DoubleStream.flatten(a, 0.0, true).toArray();
        assertArrayEquals(new double[] { 1.0, 4.0, 2.0, 5.0, 3.0, 0.0 }, result, 0.001);
    }

    @Test
    @DisplayName("flatten() with empty array should return empty stream")
    public void testFlattenEmpty() {
        double[][] array = {};
        DoubleStream stream = DoubleStream.flatten(array);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFlattenHorizontalWithPadding() {
        double[][] array = { { 1.0, 2.0 }, { 3.0 }, { 4.0, 5.0, 6.0 } };
        DoubleStream stream = DoubleStream.flatten(array, 0.0, false);
        double[] result = stream.toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 0.0, 3.0, 0.0, 0.0, 4.0, 5.0, 6.0 }, result, 0.0001);
    }



    // G114-01: flatten(double[][], true) keeps the original walk for dense input and uses a compacted row walk when
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
            final double[][] a = new double[rows][];

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
                    a[i] = new double[len];

                    for (int j = 0; j < len; j++) {
                        a[i][j] = i * 100 + j;
                    }
                }
            }

            assertFlattenVerticallyMatchesReferenceG114(a, random.nextInt(rows * 3 + 2));
        }

        // rows * longest row == 4 * elements keeps the original walk; one more empty row switches to the compacted walk
        final double[][] boundary = { { 1, 2, 3, 4 }, null, {}, { 5, 6, 7, 8 }, null, {}, {}, {} };
        final double[][] boundaryPlusOne = { { 1, 2, 3, 4 }, null, {}, { 5, 6, 7, 8 }, null, {}, {}, {}, {} };
        assertArrayEquals(new double[] { 1, 5, 2, 6, 3, 7, 4, 8 }, DoubleStream.flatten(boundary, true).toArray());
        assertArrayEquals(new double[] { 1, 5, 2, 6, 3, 7, 4, 8 }, DoubleStream.flatten(boundaryPlusOne, true).toArray());

        for (int skip = 0; skip <= 9; skip++) {
            assertFlattenVerticallyMatchesReferenceG114(boundary, skip);
            assertFlattenVerticallyMatchesReferenceG114(boundaryPlusOne, skip);
        }

        // sparse: rows drop out of the compacted walk at different columns
        final double[][] sparse = { null, {}, { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, {}, {}, { 10, 11 }, {}, null, {}, {}, { 12, 13, 14, 15 }, {} };
        assertArrayEquals(new double[] { 1, 10, 12, 2, 11, 13, 3, 14, 4, 15, 5, 6, 7, 8, 9 }, DoubleStream.flatten(sparse, true).toArray());
        assertFlattenVerticallyMatchesReferenceG114(sparse, 4);

        // one long row among 999 null rows (first and last position)
        final double[][] oneLongRow = new double[1000][];
        oneLongRow[999] = new double[5000];

        for (int j = 0; j < 5000; j++) {
            oneLongRow[999][j] = j;
        }

        assertArrayEquals(oneLongRow[999], DoubleStream.flatten(oneLongRow, true).toArray());
        oneLongRow[0] = oneLongRow[999];
        oneLongRow[999] = null;
        assertFlattenVerticallyMatchesReferenceG114(oneLongRow, 4999);
        assertEquals(0, DoubleStream.flatten(new double[][] { null, {}, null, {} }, true).count());
    }



    // G094-02: iterator exhaustion and a strongly jagged input (one long row, many single-element and null rows)
    @Test
    public void testFlattenVertically_iteratorExhaustionAndLongRow() {
        final double[][] a = { null, { 1 }, {}, { 2, 3, 4 }, null, { 5, 6 } };
        final DoubleIterator iter = DoubleStream.flatten(a, true).iterator();
        final DoubleList actual = new DoubleList();

        while (iter.hasNext()) {
            actual.add(iter.nextDouble());
        }

        assertArrayEquals(new double[] { 1, 2, 5, 3, 6, 4 }, actual.toArray());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextDouble);
        assertFalse(iter.hasNext());

        final double[][] jagged = new double[201][];

        for (int i = 0; i < 200; i++) {
            jagged[i] = i % 3 == 0 ? null : new double[] { i };
        }

        jagged[200] = new double[5000];

        for (int i = 0; i < 5000; i++) {
            jagged[200][i] = -i;
        }

        final double[] result = DoubleStream.flatten(jagged, true).toArray();
        final DoubleList expected = new DoubleList();

        for (int i = 0; i < 200; i++) {
            if (jagged[i] != null) {
                expected.add(jagged[i][0]);
            }
        }

        for (int i = 0; i < 5000; i++) {
            expected.add(jagged[200][i]);
        }

        assertArrayEquals(expected.toArray(), result);
        assertArrayEquals(new double[0], DoubleStream.flatten(new double[][] { null, {}, null }, true).toArray());
        assertArrayEquals(new double[] { 1, 3, 2, 4 }, DoubleStream.flatten(new double[][] { { 1, 2 }, { 3, 4 } }, true).toArray());
    }



    // G094-02: pins the column-major order of flatten(double[][], true) for jagged input with null/empty/short rows
    @Test
    public void testFlattenVertically_jaggedNullEmptyRowsMatchesColumnMajorReference() {
        final Random random = new Random(20260926L);

        for (int round = 0; round < 300; round++) {
            final int rows = 2 + random.nextInt(8);
            final double[][] a = new double[rows][];

            for (int r = 0; r < rows; r++) {
                final int kind = random.nextInt(6);
                a[r] = kind == 0 ? null : new double[kind == 1 ? 0 : random.nextInt(7)];

                if (a[r] != null) {
                    for (int c = 0; c < a[r].length; c++) {
                        a[r][c] = random.nextInt(1000) - 500.5;
                    }
                }
            }

            int maxLen = 0;

            for (final double[] row : a) {
                maxLen = Math.max(maxLen, row == null ? 0 : row.length);
            }

            final DoubleList expected = new DoubleList();

            for (int c = 0; c < maxLen; c++) {
                for (final double[] row : a) {
                    if (row != null && c < row.length) {
                        expected.add(row[c]);
                    }
                }
            }

            assertArrayEquals(expected.toArray(), DoubleStream.flatten(a, true).toArray());
            assertEquals(expected.size(), DoubleStream.flatten(a, true).count());

            if (expected.size() > 1) {
                assertArrayEquals(expected.copy(1, expected.size()).toArray(), DoubleStream.flatten(a, true).skip(1).toArray());
            }
        }
    }

    // flattMap

    @Test
    public void testFlattMap() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        double[] result = stream.flattMap(x -> java.util.stream.DoubleStream.of(x, x * 2)).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0 }, result, 0.0001);
    }

    @Test
    public void testFlattmap() {
        DoubleStream stream = createDoubleStream(1.0, 2.0);
        DoubleFunction<java.util.stream.DoubleStream> mapper = x -> java.util.stream.DoubleStream.of(x, x * 3);

        double[] result = stream.flattMap(mapper).toArray();
        assertArrayEquals(new double[] { 1.0, 3.0, 2.0, 6.0 }, result, 0.0001);
    }

    @Test
    public void testFlattMap_HappyPath() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).flattMap(d -> java.util.stream.DoubleStream.of(d, d * 10)).toArray();
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0 }, result, 0.001);
    }

    @Test
    public void testFlattMapToObj() {
        DoubleStream stream = createDoubleStream(1.0, 2.0);
        DoubleFunction<String[]> mapper = x -> new String[] { "X" + x, "Y" + x };

        List<String> result = stream.flatMapArrayToObj(mapper).toList();
        assertEquals(Arrays.asList("X1.0", "Y1.0", "X2.0", "Y2.0"), result);
    }

    @Test
    public void testFlattmapToObj() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        List<String> result = stream.flatMapArrayToObj(x -> new String[] { "A" + x, "B" + x }).toList();
        assertEquals(4, result.size());
    }

    // from

    @Test
    public void testFrom_JdkStream() {
        java.util.stream.DoubleStream jdkStream = java.util.stream.DoubleStream.of(1.0, 2.0, 3.0);
        DoubleStream result = DoubleStream.from(jdkStream);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result.toArray(), 0.001);
    }

    @Test
    public void testFrom_JdkStream_Empty() {
        java.util.stream.DoubleStream jdkStream = java.util.stream.DoubleStream.empty();
        DoubleStream result = DoubleStream.from(jdkStream);
        assertEquals(0, result.count());
    }

    @Test
    public void testFromJdkStream() {
        java.util.stream.DoubleStream jdkStream = java.util.stream.DoubleStream.of(1.0, 2.0, 3.0);
        DoubleStream stream = DoubleStream.from(jdkStream);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.toArray(), 0.0001);

        DoubleStream emptyStream = DoubleStream.from(null);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testFromJdkStreamIteratorCountConsumes() {
        try (DoubleStream stream = DoubleStream.from(java.util.stream.DoubleStream.of(1.0d, 2.0d))) {
            final DoubleIterator iter = stream.iterator();

            assertEquals(2, iter.count());
            assertFalse(iter.hasNext());
            assertEquals(0, iter.count());
            assertArrayEquals(new double[0], iter.toArray(), 0.0d);
            assertThrows(NoSuchElementException.class, iter::nextDouble);
        }
    }

    @Test
    public void testFromJdkStreamIteratorToArrayConsumes() {
        try (DoubleStream stream = DoubleStream.from(java.util.stream.DoubleStream.of(1.0d, 2.0d))) {
            final DoubleIterator iter = stream.iterator();

            assertArrayEquals(new double[] { 1.0d, 2.0d }, iter.toArray(), 0.0d);
            assertFalse(iter.hasNext());
            assertEquals(0, iter.count());
            assertThrows(NoSuchElementException.class, iter::nextDouble);
        }
    }

    @Test
    public void testGenerate() {
        AtomicInteger counter = new AtomicInteger(1);
        DoubleStream stream = DoubleStream.generate(() -> counter.getAndIncrement() * 1.5);
        double[] result = stream.limit(4).toArray();
        assertArrayEquals(new double[] { 1.5, 3.0, 4.5, 6.0 }, result, 0.0001);
    }

    @Test
    public void testGenerateWithNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.generate(null).limit(1).count());
    }

    // iterate

    @Test
    public void testIterate() {
        assertArrayEquals(new double[] { 1.0, 2.0, 4.0, 8.0, 16.0 }, DoubleStream.iterate(1.0, d -> d * 2).limit(5).toArray());
    }

    @Test
    public void test_iterate_02() {
        DoubleStream stream = DoubleStream.iterate(() -> true, () -> 1.0d);
        assertEquals(10, stream.limit(10).mapToFloat(e -> (float) e).count());

        stream = DoubleStream.iterate(1.0d, () -> true, it -> it);
        assertEquals(10, stream.limit(10).mapToInt(e -> (int) e).sum());

        stream = DoubleStream.iterate(1.0d, it -> true, it -> it);
        assertEquals(10, stream.limit(10).mapToInt(e -> (int) e).sum());
    }

    @Test
    public void testIterateInfinite() {
        DoubleStream stream = DoubleStream.iterate(1.0, x -> x * 2.0);
        double[] result = stream.limit(5).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 4.0, 8.0, 16.0 }, result, 0.0001);
    }

    @Test
    public void testIterateWithFalseHasNext() {
        DoubleStream stream = DoubleStream.iterate(() -> false, () -> 1.0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testIterateWithHasNext() {
        AtomicInteger counter = new AtomicInteger(0);
        DoubleStream stream = DoubleStream.iterate(() -> counter.get() < 3, () -> counter.getAndIncrement() * 2.0);
        assertArrayEquals(new double[] { 0.0, 2.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testIterateWithInitAndHasNext() {
        AtomicInteger counter = new AtomicInteger(0);
        DoubleStream stream = DoubleStream.iterate(10.0, () -> counter.getAndIncrement() < 3, x -> x + 2.0);
        double[] result = stream.toArray();
        assertEquals(3, result.length);
        assertEquals(10.0, result[0], 0.0001);
    }

    @Test
    public void testIterateWithNullHasNext() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.iterate(null, () -> 1.0).limit(1).count());
    }

    @Test
    public void testIterateWithNullNext() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.iterate(() -> true, null).limit(1).count());
    }

    @Test
    public void testIterateWithPredicate() {
        DoubleStream stream = DoubleStream.iterate(1.0, x -> x < 10.0, x -> x + 3.0);
        assertArrayEquals(new double[] { 1.0, 4.0, 7.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testOf() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.toArray(), 0.0001);

        DoubleStream emptyStream = DoubleStream.of();
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testOfArray() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        DoubleStream stream = createDoubleStream(array);
        assertArrayEquals(array, stream.toArray(), 0.0001);

        DoubleStream emptyStream = createDoubleStream(new double[0]);
        assertEquals(0, emptyStream.count());

        DoubleStream rangeStream = createDoubleStream(array, 1, 4);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, rangeStream.toArray(), 0.0001);
    }

    @Test
    public void testOfBoxedArray() {
        Double[] array = { 1.0, 2.0, 3.0 };
        DoubleStream stream = DoubleStream.of(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testOfBoxedArrayWithRange() {
        Double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        DoubleStream stream = DoubleStream.of(array, 1, 4);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testOfCollection() {
        Collection<Double> coll = Arrays.asList(1.0, 2.0, 3.0);
        DoubleStream stream = DoubleStream.of(coll);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testOfDoubleBuffer() {
        java.nio.DoubleBuffer buffer = java.nio.DoubleBuffer.wrap(new double[] { 1.0, 2.0, 3.0 });
        DoubleStream stream = DoubleStream.of(buffer);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testOfIterator() {
        DoubleIterator iter = DoubleIterator.of(new double[] { 1.0, 2.0, 3.0 });
        DoubleStream stream = DoubleStream.of(iter);
        assertEquals(3, stream.count());
    }

    @Test
    public void testOfJavaOptionalDouble() {
        java.util.OptionalDouble op = java.util.OptionalDouble.of(5.0);
        DoubleStream stream = DoubleStream.of(op);
        assertArrayEquals(new double[] { 5.0 }, stream.toArray(), 0.0001);

        java.util.OptionalDouble empty = java.util.OptionalDouble.empty();
        DoubleStream emptyStream = DoubleStream.of(empty);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testOfJdkOptionalDouble() {
        DoubleStream result = DoubleStream.of(java.util.OptionalDouble.of(42.5));
        assertArrayEquals(new double[] { 42.5 }, result.toArray(), 0.001);

        result = DoubleStream.of(java.util.OptionalDouble.empty());
        assertEquals(0, result.count());
    }

    // ofNullable

    @Test
    public void testOfNullable() {
        DoubleStream stream1 = DoubleStream.ofNullable(5.0);
        assertEquals(1, stream1.count());

        DoubleStream stream2 = DoubleStream.ofNullable(null);
        assertEquals(0, stream2.count());
    }

    @Test
    public void testOfNullable_Null() {
        DoubleStream result = DoubleStream.ofNullable(null);
        assertEquals(0, result.count());
    }

    @Test
    public void testOfNullable_Value() {
        DoubleStream result = DoubleStream.ofNullable(42.5);
        assertArrayEquals(new double[] { 42.5 }, result.toArray(), 0.001);
    }

    @Test
    public void testOfOptionalDouble() {
        OptionalDouble op = OptionalDouble.of(5.0);
        DoubleStream stream = DoubleStream.of(op);
        assertArrayEquals(new double[] { 5.0 }, stream.toArray(), 0.0001);

        OptionalDouble empty = OptionalDouble.empty();
        DoubleStream emptyStream = DoubleStream.of(empty);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testOfOptionalDouble_Present() {
        DoubleStream result = DoubleStream.of(OptionalDouble.of(42.5));
        assertArrayEquals(new double[] { 42.5 }, result.toArray(), 0.001);
    }

    @Test
    public void testOfWithRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        DoubleStream stream = DoubleStream.of(array, 1, 4);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testRandom() {
        DoubleStream stream = DoubleStream.random();
        double[] values = stream.limit(5).toArray();
        assertEquals(5, values.length);

        for (double value : values) {
            assertTrue(value >= 0.0 && value < 1.0);
        }
    }

    // rangeMap

    @Test
    public void testRangeMap() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 10.0, 11.0, 20.0);
        double[] result = stream.rangeMap((first, next) -> next - first < 2.0, (first, last) -> first + last).toArray();
        assertEquals(4, result.length);
        assertArrayEquals(new double[] { 3.0, 6.0, 21.0, 40.0 }, result, 0.0001);
    }

    @Test
    public void testRangeMap_Empty() {
        double[] result = DoubleStream.empty().rangeMap((a, b) -> a == b, Double::sum).toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testRangeMap_HappyPath2() {
        double[] result = DoubleStream.of(1.0, 1.0, 2.0, 2.0, 3.0).rangeMap((a, b) -> a == b, Double::sum).toArray();
        assertArrayEquals(new double[] { 2.0, 4.0, 6.0 }, result, 0.001);
    }

    @Test
    public void testRangeMapToObj() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 10.0, 11.0);
        List<String> result = stream.rangeMapToObj((first, next) -> next - first < 2.0, (first, last) -> first + "-" + last).toList();
        assertEquals(3, result.size());
        assertEquals(N.toList("1.0-2.0", "3.0-3.0", "10.0-11.0"), result);
    }

    @Test
    public void testRangeMapToObj_HappyPath2() {
        List<String> result = DoubleStream.of(1.0, 1.0, 2.0, 3.0, 3.0).rangeMapToObj((a, b) -> a == b, (a, b) -> a + "-" + b).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testRangeWithInvalidStepDirection() {
        // DoubleStream doesn't support range with step parameter
        // Testing iterate with false condition instead
        DoubleStream stream = DoubleStream.iterate(1.0, d -> d < 5.0 && d > 10.0, d -> d + 1.0);
        assertEquals(0, stream.count());
    }

    // repeat

    @Test
    public void testRepeat() {
        DoubleStream stream = DoubleStream.repeat(7.5, 3);
        assertArrayEquals(new double[] { 7.5, 7.5, 7.5 }, stream.toArray(), 0.0001);

        DoubleStream emptyStream = DoubleStream.repeat(7.5, 0);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testRepeat_02() {
        DoubleStream stream = DoubleStream.repeat(1.0d, 1000);
        assertEquals(1000, stream.mapToFloat(e -> (float) e).count());
    }

    @Test
    public void testRepeat_HappyPath() {
        double[] result = DoubleStream.repeat(3.14, 4).toArray();
        assertEquals(4, result.length);
        for (double d : result) {
            assertEquals(3.14, d, 0.001);
        }
    }

    @Test
    public void testRepeat_ZeroTimes() {
        double[] result = DoubleStream.repeat(3.14, 0).toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testRepeatIteratorRemainsExhaustedAfterFailedNext() {
        final DoubleIteratorEx iter = DoubleStream.repeat(1.5, 10).iteratorEx();
        iter.advance(Long.MAX_VALUE);

        assertThrows(NoSuchElementException.class, iter::nextDouble);
        assertEquals(0, iter.count());
        assertArrayEquals(new double[0], iter.toArray(), 0.0);
    }

    @Test
    public void testRepeatWithNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.repeat(1.0, -1));
    }

}
