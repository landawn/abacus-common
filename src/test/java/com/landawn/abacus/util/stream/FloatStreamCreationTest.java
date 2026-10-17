package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.N;

/** FloatStream creation contracts. */
public class FloatStreamCreationTest extends FloatStreamTestSupport {

    // defer

    @Test
    @DisplayName("defer() should create deferred stream")
    public void testDefer() {
        FloatStream stream = FloatStream.defer(() -> FloatStream.of(1.0f, 2.0f, 3.0f));
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    public void testDefer_supplierReturningNullProducesEmptyStream() {
        final AtomicInteger supplierCalls = new AtomicInteger();
        final FloatStream deferred = FloatStream.defer(() -> {
            supplierCalls.incrementAndGet();
            return null;
        });

        assertEquals(0, deferred.count());
        assertEquals(1, supplierCalls.get());
    }

    @Test
    public void testDefer_Test() {
        FloatStream result = FloatStream.defer(() -> FloatStream.of(1.0f, 2.0f, 3.0f));
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result.toArray(), DELTA);
    }

    @Test
    public void testDeferRejectsNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> FloatStream.defer(null));
    }

    @Test
    @DisplayName("empty() should return empty stream")
    public void testEmpty() {
        FloatStream stream = FloatStream.empty();
        assertEquals(0, stream.count());
    }

    @Test
    @DisplayName("empty stream operations")
    public void testEmptyStream() {
        FloatStream empty = FloatStream.empty();
        assertEquals(0, empty.count());

        assertFalse(FloatStream.empty().first().isPresent());
        assertFalse(FloatStream.empty().min().isPresent());
        assertFalse(FloatStream.empty().max().isPresent());
        assertEquals(0.0, FloatStream.empty().sum(), 0.001);
        assertFalse(FloatStream.empty().average().isPresent());
    }

    // flatten

    @Test
    @DisplayName("flatten() should flatten two-dimensional array")
    public void testFlatten2D() {
        float[][] array = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
        FloatStream stream = FloatStream.flatten(array);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    @DisplayName("flatten() should flatten two-dimensional array")
    public void testFlatten2D_2() {
        float[][] array = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
        float[] result = FloatStream.flatten(array, false).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result);

        result = FloatStream.flatten(array, true).toArray();
        assertArrayEquals(new float[] { 1.0f, 3.0f, 2.0f, 4.0f }, result);
    }

    @Test
    @DisplayName("flatten() should flatten two-dimensional array")
    public void testFlatten2D_3() {
        float[][] array = { { 1.0f, 2.0f }, { 3.0f, 4.0f, 5.f } };
        float[] result = FloatStream.flatten(array, 0.0f, false).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 0.0f, 3.0f, 4.0f, 5.0f }, result);

        result = FloatStream.flatten(array, 0.0f, true).toArray();
        assertArrayEquals(new float[] { 1.0f, 3.0f, 2.0f, 4.0f, 0.0f, 5.0f }, result);
    }

    @Test
    public void testFlatten2D_Test() {
        float[][] arrays = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
        FloatStream result = FloatStream.flatten(arrays);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result.toArray(), DELTA);
    }

    @Test
    @DisplayName("flatten() with vertically flag should flatten correctly")
    public void testFlatten2DVertically() {
        float[][] array = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
        FloatStream stream = FloatStream.flatten(array, false);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());

        stream = FloatStream.flatten(array, true);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 2.0f, 4.0f }, stream.toArray());
    }

    @Test
    public void testFlatten2DVertically_Test() {
        float[][] arrays = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
        FloatStream result = FloatStream.flatten(arrays, true);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 2.0f, 4.0f }, result.toArray(), DELTA);
    }

    @Test
    @DisplayName("flatten() with alignment value should pad arrays")
    public void testFlatten2DWithAlignment() {
        float[][] array = { { 1.0f, 2.0f }, { 3.0f, 4.0f, 5.0f } };
        FloatStream stream = FloatStream.flatten(array, 0.0f, false);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 0.0f, 3.0f, 4.0f, 5.0f }, stream.toArray());

        stream = FloatStream.flatten(array, 0.0f, true);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 2.0f, 4.0f, 0.0f, 5.0f }, stream.toArray());
    }

    @Test
    public void testFlatten2DWithAlignment_Test() {
        float[][] arrays = { { 1.0f, 2.0f }, { 3.0f } };
        FloatStream result = FloatStream.flatten(arrays, 0.0f, true);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 2.0f, 0.0f }, result.toArray(), DELTA);
    }

    @Test
    @DisplayName("flatten() should flatten three-dimensional array")
    public void testFlatten3D() {
        float[][][] array = { { { 1.0f, 2.0f }, { 3.0f, 4.0f } } };
        FloatStream stream = FloatStream.flatten(array);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    public void testFlatten3D_Test() {
        float[][][] arrays = { { { 1.0f, 2.0f }, { 3.0f } }, { { 4.0f, 5.0f, 6.0f } } };
        FloatStream result = FloatStream.flatten(arrays);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result.toArray(), DELTA);
    }

    // Edge cases
    @Test
    @DisplayName("flatten() with alignment horizontally additional")
    public void testFlattenAlignmentAdditional() {
        float[][] array = { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f }, { 6.0f } };
        FloatStream stream = FloatStream.flatten(array, 0.0f, true);
        float[] result = stream.toArray();
        assertEquals(9, result.length);
        assertEquals(1.0f, result[0], 0.001);
        assertEquals(4.0f, result[1], 0.001);
        assertEquals(6.0f, result[2], 0.001);
    }

    @Test
    @DisplayName("flatten() with empty array should return empty stream")
    public void testFlattenEmpty() {
        float[][] array = {};
        FloatStream stream = FloatStream.flatten(array);
        assertEquals(0, stream.count());
    }

    // G114-01: flatten(float[][], true) keeps the original walk for dense input and uses a compacted row walk when
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
            final float[][] a = new float[rows][];

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
                    a[i] = new float[len];

                    for (int j = 0; j < len; j++) {
                        a[i][j] = i * 100 + j;
                    }
                }
            }

            assertFlattenVerticallyMatchesReferenceG114(a, random.nextInt(rows * 3 + 2));
        }

        // rows * longest row == 4 * elements keeps the original walk; one more empty row switches to the compacted walk
        final float[][] boundary = { { 1, 2, 3, 4 }, null, {}, { 5, 6, 7, 8 }, null, {}, {}, {} };
        final float[][] boundaryPlusOne = { { 1, 2, 3, 4 }, null, {}, { 5, 6, 7, 8 }, null, {}, {}, {}, {} };
        assertArrayEquals(new float[] { 1, 5, 2, 6, 3, 7, 4, 8 }, FloatStream.flatten(boundary, true).toArray());
        assertArrayEquals(new float[] { 1, 5, 2, 6, 3, 7, 4, 8 }, FloatStream.flatten(boundaryPlusOne, true).toArray());

        for (int skip = 0; skip <= 9; skip++) {
            assertFlattenVerticallyMatchesReferenceG114(boundary, skip);
            assertFlattenVerticallyMatchesReferenceG114(boundaryPlusOne, skip);
        }

        // sparse: rows drop out of the compacted walk at different columns
        final float[][] sparse = { null, {}, { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, {}, {}, { 10, 11 }, {}, null, {}, {}, { 12, 13, 14, 15 }, {} };
        assertArrayEquals(new float[] { 1, 10, 12, 2, 11, 13, 3, 14, 4, 15, 5, 6, 7, 8, 9 }, FloatStream.flatten(sparse, true).toArray());
        assertFlattenVerticallyMatchesReferenceG114(sparse, 4);

        // one long row among 999 null rows (first and last position)
        final float[][] oneLongRow = new float[1000][];
        oneLongRow[999] = new float[5000];

        for (int j = 0; j < 5000; j++) {
            oneLongRow[999][j] = j;
        }

        assertArrayEquals(oneLongRow[999], FloatStream.flatten(oneLongRow, true).toArray());
        oneLongRow[0] = oneLongRow[999];
        oneLongRow[999] = null;
        assertFlattenVerticallyMatchesReferenceG114(oneLongRow, 4999);
        assertEquals(0, FloatStream.flatten(new float[][] { null, {}, null, {} }, true).count());
    }

    // G096-01: iterator of flatten(a, true) throws NoSuchElementException once exhausted
    @Test
    public void testFlattenVertically_iteratorExhaustion() {
        final FloatIterator iter = FloatStream.flatten(new float[][] { { 1, 2, 3 }, null, { 4 } }, true).iterator();
        assertEquals(1f, iter.nextFloat());
        assertEquals(4f, iter.nextFloat());
        assertEquals(2f, iter.nextFloat());
        assertEquals(3f, iter.nextFloat());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextFloat);
        assertThrows(NoSuchElementException.class, iter::nextFloat);
    }

    // G096-01: flatten(a, true) on jagged arrays with null/empty rows keeps the column-major order
    @Test
    public void testFlattenVertically_jaggedNullAndEmptyRows() {
        final float[][] array = { null, { 1, 2, 3, 4 }, {}, { 5 }, null, { 6, 7 }, { 8, 9, 10, 11, 12 }, {} };
        final float[] expected = { 1, 5, 6, 8, 2, 7, 9, 3, 10, 4, 11, 12 };
        assertArrayEquals(expected, flattenVerticallyReference(array));
        assertArrayEquals(expected, FloatStream.flatten(array, true).toArray());
        assertEquals(12, FloatStream.flatten(array, true).count());
        assertArrayEquals(new float[] { 9, 3, 10, 4, 11, 12 }, FloatStream.flatten(array, true).skip(6).toArray());
        assertEquals(N.toList(1f, 5f, 6f, 8f, 2f, 7f, 9f, 3f, 10f, 4f, 11f, 12f), FloatStream.flatten(array, true).toList());

        // only the last row is long, only the first row is long
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 }, FloatStream.flatten(new float[][] { { 1 }, { 2 }, { 3, 4, 5, 6 } }, true).toArray());
        assertArrayEquals(new float[] { 1, 5, 6, 2, 3, 4 }, FloatStream.flatten(new float[][] { { 1, 2, 3, 4 }, { 5 }, { 6 } }, true).toArray());
        // all rows null or empty
        assertEquals(0, FloatStream.flatten(new float[][] { null, {}, null }, true).count());
        // NaN / -0.0 are passed through bit for bit
        final float[] special = FloatStream.flatten(new float[][] { { Float.NaN, 1 }, { -0.0f } }, true).toArray();
        assertEquals(3, special.length);
        assertTrue(Float.isNaN(special[0]));
        assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(special[1]));
        assertEquals(1f, special[2]);
    }

    // G096-01: flatten(a, true) matches a naive column-major reference on random jagged arrays
    @Test
    public void testFlattenVertically_randomJaggedMatchesReference() {
        final Random random = new Random(96);

        for (int round = 0; round < 300; round++) {
            final float[][] array = new float[2 + random.nextInt(8)][];

            for (int i = 0; i < array.length; i++) {
                final int len = random.nextInt(10) - 1;

                if (len >= 0) {
                    array[i] = new float[len];

                    for (int j = 0; j < len; j++) {
                        array[i][j] = random.nextInt(100);
                    }
                }
            }

            final float[] expected = flattenVerticallyReference(array);
            assertArrayEquals(expected, FloatStream.flatten(array, true).toArray());
            assertEquals(expected.length, FloatStream.flatten(array, true).count());

            final int skip = random.nextInt(expected.length + 2);
            assertArrayEquals(Arrays.copyOfRange(expected, Math.min(skip, expected.length), expected.length),
                    FloatStream.flatten(array, true).skip(skip).toArray());
        }
    }

    @Test
    @DisplayName("flatMapArrayToObj() should flatten array to object stream")
    public void testFlattMapToObj() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        Stream<String> result = stream.flatMapArrayToObj(f -> new String[] { String.valueOf(f), String.valueOf(f * 2) });
        String[] array = result.toArray(String[]::new);
        assertArrayEquals(new String[] { "1.0", "2.0", "2.0", "4.0" }, array);
    }

    @Test
    @DisplayName("flatMapArrayToObj() should flatten array to object stream")
    public void testFlattmapToObj() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new String[] { "1.0", "2.0", "2.0", "4.0" },
                stream.flatMapArrayToObj(f -> new String[] { String.valueOf(f), String.valueOf(f * 2) }).toArray(String[]::new));
    }

    @Test
    public void testFlattMapToObjWithArray() {
        String[] result = FloatStream.of(1f, 2f).flatMapArrayToObj(f -> new String[] { "val=" + f }).toArray(String[]::new);
        assertArrayEquals(new String[] { "val=1.0", "val=2.0" }, result);
    }

    // generate

    @Test
    @DisplayName("generate() should create stream with supplier")
    public void testGenerate() {
        FloatStream stream = FloatStream.generate(() -> 5.0f).limit(3);
        assertArrayEquals(new float[] { 5.0f, 5.0f, 5.0f }, stream.toArray());
    }

    @Test
    public void testGenerate_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> FloatStream.generate(null).limit(1).count());
    }

    @Test
    public void testGenerate_Test() {
        AtomicInteger counter = new AtomicInteger(0);
        float[] result = FloatStream.generate(() -> counter.getAndIncrement() * 1.0f).limit(3).toArray();
        assertArrayEquals(new float[] { 0.0f, 1.0f, 2.0f }, result, DELTA);
    }

    // iterate

    @Test
    public void testIterate() {
        float[] expected = { 1.0f, 1.5f, 2.0f, 2.5f, 3.0f };
        assertArrayEquals(expected, FloatStream.iterate(1.0f, f -> f + 0.5f).limit(5).toArray(), DELTA);
    }

    @Test
    public void test_iterate_02() {
        FloatStream stream = FloatStream.iterate(() -> true, () -> 1.0f);
        assertEquals(10, stream.limit(10).mapToDouble(e -> e).count());

        stream = FloatStream.iterate(1.0f, () -> true, it -> it);
        assertEquals(10, stream.limit(10).mapToInt(e -> (int) e).sum());

        stream = FloatStream.iterate(1.0f, it -> true, it -> it);
        assertEquals(10, stream.limit(10).mapToInt(e -> (int) e).sum());
    }

    @Test
    @DisplayName("iterate() with immediate false hasNext additional")
    public void testIterateFalseHasNextAdditional() {
        FloatStream stream = FloatStream.iterate(1.0f, () -> false, f -> f + 1.0f);
        assertEquals(0, stream.count());
    }

    @Test
    @DisplayName("iterate() with init and function should create infinite stream")
    public void testIterateInfinite() {
        FloatStream stream = FloatStream.iterate(1.0f, x -> x + 1.0f).limit(3);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    public void testIterateInfinite_Test() {
        float[] result = FloatStream.iterate(1.0f, f -> f + 1.0f).limit(3).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testIterateWithHasNext_Test() {
        AtomicInteger counter = new AtomicInteger(0);
        float[] result = FloatStream.iterate(() -> counter.get() < 3, () -> counter.getAndIncrement() * 1.0f).toArray();
        assertArrayEquals(new float[] { 0.0f, 1.0f, 2.0f }, result, DELTA);
    }

    @Test
    @DisplayName("iterate() with hasNext and next should create stream")
    public void testIterateWithHasNextNext() {
        AtomicInteger counter = new AtomicInteger(0);
        FloatStream stream = FloatStream.iterate(() -> counter.get() < 3, () -> counter.getAndIncrement() + 1.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    public void testIterateWithInitHasNext_Test() {
        float[] result = FloatStream.iterate(1.0f, () -> true, f -> f + 1.0f).limit(3).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    @DisplayName("iterate() with init, hasNext and function should create stream")
    public void testIterateWithInitHasNextFunction() {
        FloatStream stream = FloatStream.iterate(1.0f, () -> true, x -> x + 1.0f).limit(3);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    @DisplayName("iterate() with init, predicate and function should create stream")
    public void testIterateWithPredicate() {
        FloatStream stream = FloatStream.iterate(1.0f, x -> x < 5.0f, x -> x + 1.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    public void testIterateWithPredicate_Test() {
        float[] result = FloatStream.iterate(1.0f, f -> f < 4.0f, f -> f + 1.0f).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    @DisplayName("of() with primitive array should create stream")
    public void testOfArray() {
        float[] array = { 1.5f, 2.5f, 3.5f };
        FloatStream stream = FloatStream.of(array);
        assertArrayEquals(array, stream.toArray());
    }

    @Test
    @DisplayName("of() with array range should create substream")
    public void testOfArrayRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        FloatStream stream = FloatStream.of(array, 1, 4);
        assertArrayEquals(new float[] { 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    public void testOfBoxedArray() {
        FloatStream result = FloatStream.of(new Float[] { 1.0f, 2.0f, 3.0f });
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result.toArray(), DELTA);
    }

    @Test
    public void testOfBoxedArrayRange() {
        FloatStream result = FloatStream.of(new Float[] { 1.0f, 2.0f, 3.0f, 4.0f }, 0, 2);
        assertArrayEquals(new float[] { 1.0f, 2.0f }, result.toArray(), DELTA);
    }

    @Test
    @DisplayName("of() with collection should create stream")
    public void testOfCollection() {
        List<Float> list = Arrays.asList(1.0f, 2.0f, 3.0f);
        FloatStream stream = FloatStream.of(list);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    @DisplayName("of() with Float array should create stream")
    public void testOfFloatArray() {
        Float[] array = { 1.0f, 2.0f, 3.0f };
        FloatStream stream = FloatStream.of(array);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    public void testOfFloatArray_Empty() {
        FloatStream result = FloatStream.of(new float[] {});
        assertEquals(0, result.count());
    }

    @Test
    @DisplayName("of() with Float array range should create substream")
    public void testOfFloatArrayRange() {
        Float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        FloatStream stream = FloatStream.of(array, 1, 4);
        assertArrayEquals(new float[] { 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    public void testOfFloatArrayRange_SubRange() {
        FloatStream result = FloatStream.of(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 1, 3);
        assertArrayEquals(new float[] { 2.0f, 3.0f }, result.toArray(), DELTA);
    }

    @Test
    @DisplayName("of() with FloatBuffer should create stream")
    public void testOfFloatBuffer() {
        FloatBuffer buffer = FloatBuffer.allocate(3);
        buffer.put(1.0f).put(2.0f).put(3.0f).flip();
        FloatStream stream = FloatStream.of(buffer);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    public void testOfFloatBuffer_Empty() {
        FloatBuffer buffer = FloatBuffer.wrap(new float[] {});
        FloatStream result = FloatStream.of(buffer);
        assertEquals(0, result.count());
    }

    @Test
    public void testOfFloatBuffer_Test() {
        FloatBuffer buffer = FloatBuffer.wrap(new float[] { 1.0f, 2.0f, 3.0f });
        FloatStream result = FloatStream.of(buffer);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result.toArray(), DELTA);
    }

    @Test
    @DisplayName("of() with iterator should create stream")
    public void testOfIterator() {
        FloatIterator iterator = FloatIterator.of(1.0f, 2.0f, 3.0f);
        FloatStream stream = FloatStream.of(iterator);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    public void testOfMultiple() {
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f }, FloatStream.of(1.1f, 2.2f, 3.3f).toArray(), DELTA);
    }

    // ofNullable

    @Test
    public void testOfNullable() {
        assertEquals(0, FloatStream.ofNullable(null).count());
        assertArrayEquals(new float[] { 9.9f }, FloatStream.ofNullable(9.9f).toArray(), DELTA);
    }

    @Test
    public void testOfNullable_Null() {
        FloatStream result = FloatStream.ofNullable(null);
        assertEquals(0, result.count());
    }

    @Test
    public void testOfNullable_WithValue() {
        FloatStream result = FloatStream.ofNullable(42.5f);
        assertArrayEquals(new float[] { 42.5f }, result.toArray(), DELTA);
    }

    @Test
    @DisplayName("ofNullable() with null should return empty stream")
    public void testOfNullableNull() {
        FloatStream stream = FloatStream.ofNullable(null);
        assertEquals(0, stream.count());
    }

    @Test
    @DisplayName("ofNullable() with value should return single element stream")
    public void testOfNullableValue() {
        FloatStream stream = FloatStream.ofNullable(5.0f);
        assertArrayEquals(new float[] { 5.0f }, stream.toArray());
    }

    @Test
    @DisplayName("ofNullable() with value should return single element stream")
    public void testOfNullableWithValue() {
        FloatStream stream = FloatStream.ofNullable(5.0f);
        float[] result = stream.toArray();
        assertArrayEquals(new float[] { 5.0f }, result);
    }

    @Test
    public void testOfSingle() {
        assertArrayEquals(new float[] { 3.14f }, FloatStream.of(3.14f).toArray(), DELTA);
    }

    @Test
    @DisplayName("of() with varargs should create stream")
    public void testOfVarargs() {
        FloatStream stream = FloatStream.of(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    @DisplayName("random() should generate random stream")
    public void testRandom() {
        FloatStream stream = FloatStream.random().limit(5);
        float[] result = stream.toArray();
        assertEquals(5, result.length);
        for (float f : result) {
            assertTrue(f >= 0.0f && f < 1.0f);
        }
    }

    @Test
    public void testRandom_Test() {
        FloatStream result = FloatStream.random().limit(10);
        assertEquals(10, result.count());
    }

    // rangeMap

    @Test
    @DisplayName("rangeMap() should map ranges")
    public void testRangeMap() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 6.0f, 7.0f);
        float[] result = stream.rangeMap((a, b) -> Math.abs(b - a) <= 1.5f, (start, end) -> end - start).toArray();
        assertTrue(result.length > 0);
    }

    @Test
    public void testRangeMap_Empty() {
        float[] result = FloatStream.empty().rangeMap((first, next) -> next - first < 2, (first, last) -> first + last).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testRangeMap_SingleElement() {
        float[] result = createFloatStream(5.0f).rangeMap((first, next) -> next - first < 2, (first, last) -> first + last).toArray();
        assertArrayEquals(new float[] { 10.0f }, result, DELTA);
    }

    @Test
    @DisplayName("rangeMapToObj() should map ranges to objects")
    public void testRangeMapToObj() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 5.0f, 6.0f);
        String[] result = stream.rangeMapToObj((a, b) -> Math.abs(b - a) <= 1.5f, (start, end) -> String.format("%.1f-%.1f", start, end))
                .toArray(String[]::new);
        assertTrue(result.length > 0);
    }

    // repeat

    @Test
    @DisplayName("repeat() should create stream with repeated element")
    public void testRepeat() {
        FloatStream stream = FloatStream.repeat(2.5f, 3);
        assertArrayEquals(new float[] { 2.5f, 2.5f, 2.5f }, stream.toArray());
    }

    @Test
    public void testRepeat_02() {
        FloatStream stream = FloatStream.repeat(1.0f, 1000);
        assertEquals(1000, stream.mapToDouble(e -> e).count());
    }

    @Test
    public void testRepeat_NegativeThrows() {
        assertThrows(IllegalArgumentException.class, () -> FloatStream.repeat(1.0f, -1));
    }

    @Test
    public void testRepeat_Test() {
        float[] result = FloatStream.repeat(5.0f, 3).toArray();
        assertArrayEquals(new float[] { 5.0f, 5.0f, 5.0f }, result, DELTA);
    }

    @Test
    public void testRepeat_WithZero() {
        FloatStream result = FloatStream.repeat(1.0f, 0);
        assertEquals(0, result.count());
    }

    @Test
    public void testRepeatIteratorRemainsExhaustedAfterFailedNext() {
        final FloatIteratorEx iter = FloatStream.repeat(1.5f, 10).iteratorEx();
        iter.advance(Long.MAX_VALUE);

        assertThrows(NoSuchElementException.class, iter::nextFloat);
        assertEquals(0, iter.count());
        assertArrayEquals(new float[0], iter.toArray(), 0.0f);
    }

    @Test
    @DisplayName("repeat() with zero count should return empty stream")
    public void testRepeatZero() {
        FloatStream stream = FloatStream.repeat(1.0f, 0);
        assertEquals(0, stream.count());
    }

}
