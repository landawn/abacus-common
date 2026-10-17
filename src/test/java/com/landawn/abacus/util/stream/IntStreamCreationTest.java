package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.u.OptionalInt;

/** IntStream creation contracts. */
public class IntStreamCreationTest extends IntStreamTestSupport {

    // defer

    @Test
    public void testDefer_CreatesLazyStream() {
        AtomicInteger counter = new AtomicInteger(0);
        stream = IntStream.defer(() -> {
            counter.incrementAndGet();
            return IntStream.of(1, 2, 3);
        });
        assertEquals(0, counter.get());
        stream.count();
        assertEquals(1, counter.get());
    }

    @Test
    public void testDefer_EmptySupplier() {
        IntStream deferred = IntStream.defer(IntStream::empty);
        assertEquals(0, deferred.count());
    }

    // defer(Supplier) - lazy stream creation
    @Test
    public void testDefer_LazyEvaluation() {
        AtomicInteger callCount = new AtomicInteger(0);
        // Supplier should not be called until terminal operation
        IntStream deferred = IntStream.defer(() -> {
            callCount.incrementAndGet();
            return IntStream.of(1, 2, 3);
        });
        assertEquals(0, callCount.get());
        int[] result = deferred.toArray();
        assertEquals(1, callCount.get());
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testDefer_supplierReturningNullProducesEmptyStream() {
        final AtomicInteger supplierCalls = new AtomicInteger();
        final IntStream deferred = IntStream.defer(() -> {
            supplierCalls.incrementAndGet();
            return null;
        });

        assertEquals(0, deferred.count());
        assertEquals(1, supplierCalls.get());
    }

    // empty

    @Test
    public void testEmpty_ReturnsEmptyStream() {
        stream = IntStream.empty();
        assertFalse(stream.iterator().hasNext());
        assertEquals(0, IntStream.empty().count());
    }

    @Test
    public void empty_shouldCreateEmptyStream() {
        assertEquals(0, IntStream.empty().count());
    }

    @Test
    public void testEmptyStreamOperations() {
        IntStream empty = IntStream.empty();

        assertEquals(0, empty.sum());
        assertEquals(0, IntStream.empty().count());
        assertFalse(IntStream.empty().min().isPresent());
        assertFalse(IntStream.empty().max().isPresent());
        assertFalse(IntStream.empty().average().isPresent());
        assertFalse(IntStream.empty().first().isPresent());
        assertFalse(IntStream.empty().last().isPresent());
        assertTrue(IntStream.empty().allMatch(n -> false));
        assertFalse(IntStream.empty().anyMatch(n -> true));
        assertTrue(IntStream.empty().noneMatch(n -> true));
    }

    @Test
    public void testOf_JdkOptionalInt_Empty() {
        assertEquals(0, IntStream.of(java.util.OptionalInt.empty()).count());
    }

    @Test
    public void testOf_LandawnOptionalInt_Empty() {
        assertEquals(0, IntStream.of(OptionalInt.empty()).count());
    }

    @Test
    public void testOf_WithEmptyOptionalInt() {
        stream = IntStream.of(OptionalInt.empty());
        assertEquals(0, stream.count());
    }

    // flatten

    @Test
    public void testFlatten() {
        int[][] arrays = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        IntStream stream = IntStream.flatten(arrays);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    @DisplayName("flatten() should flatten two-dimensional array")
    public void testFlatten2D_2() {
        int[][] array = { { 1, 2 }, { 3, 4 } };
        int[] result = IntStream.flatten(array, false).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);

        result = IntStream.flatten(array, true).toArray();
        assertArrayEquals(new int[] { 1, 3, 2, 4 }, result);
    }

    @Test
    @DisplayName("flatten() should flatten two-dimensional array")
    public void testFlatten2D_3() {
        int[][] array = { { 1, 2 }, { 3, 4, 5 } };
        int[] result = IntStream.flatten(array, 0, false).toArray();
        assertArrayEquals(new int[] { 1, 2, 0, 3, 4, 5 }, result);

        result = IntStream.flatten(array, 0, true).toArray();
        assertArrayEquals(new int[] { 1, 3, 2, 4, 0, 5 }, result);
    }

    @Test
    public void testFlatten_2D_EmptyArray_Vertically() {
        assertEquals(0, IntStream.flatten(new int[0][], true).count());
    }

    @Test
    public void testFlatten_2D_Horizontally() {
        int[][] arr = { { 1, 2 }, { 3, 4 } };
        int[] result = IntStream.flatten(arr, false).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    // flatten(int[][], boolean) - with vertically=true
    @Test
    public void testFlatten_2D_Vertically() {
        int[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
        int[] result = IntStream.flatten(arr, true).toArray();
        assertArrayEquals(new int[] { 1, 4, 2, 5, 3, 6 }, result);
    }

    @Test
    public void testFlatten_2D_WithPadding_EmptyArray() {
        assertEquals(0, IntStream.flatten(new int[0][], 0, true).count());
    }

    @Test
    public void testFlatten_2D_WithPadding_Horizontally() {
        int[][] arr = { { 1, 2 }, { 3, 4, 5 } };
        int[] result = IntStream.flatten(arr, -1, false).toArray();
        assertArrayEquals(new int[] { 1, 2, -1, 3, 4, 5 }, result);
    }

    // flatten(int[][], int, boolean) - with padding value
    @Test
    public void testFlatten_2D_WithPadding_Vertically() {
        int[][] arr = { { 1, 2, 3 }, { 4, 5 } };
        int[] result = IntStream.flatten(arr, 0, true).toArray();
        assertArrayEquals(new int[] { 1, 4, 2, 5, 3, 0 }, result);
    }

    @Test
    public void testFlatten_2DArray() {
        int[][] array = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        stream = IntStream.flatten(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testFlatten_2DArrayVertically() {
        int[][] array = { { 1, 2, 3 }, { 4, 5, 6 } };
        stream = IntStream.flatten(array, true);
        assertArrayEquals(new int[] { 1, 4, 2, 5, 3, 6 }, stream.toArray());
    }

    @Test
    public void testFlatten_3DArray() {
        int[][][] array = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } };
        stream = IntStream.flatten(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }, stream.toArray());
    }

    @Test
    public void testFlatten_WithAlignment() {
        int[][] a = { { 1, 2, 3 }, { 4, 5 } };
        int[] result = IntStream.flatten(a, 0, true).toArray();
        assertArrayEquals(new int[] { 1, 4, 2, 5, 3, 0 }, result);
    }

    @Test
    @DisplayName("flatten() with empty array should return empty stream")
    public void testFlattenEmpty() {
        int[][] array = {};
        IntStream stream = IntStream.flatten(array);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFlattenHorizontalWithPadding() {
        int[][] array = { { 1, 2 }, { 3 }, { 4, 5, 6 } };
        stream = IntStream.flatten(array, 0, false);
        int[] result = stream.toArray();
        assertArrayEquals(new int[] { 1, 2, 0, 3, 0, 0, 4, 5, 6 }, result);
    }

    @Test
    public void testFlattenVertically() {
        int[][] arrays = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        IntStream stream = IntStream.flatten(arrays, true);
        assertArrayEquals(new int[] { 1, 4, 7, 2, 5, 8, 3, 6, 9 }, stream.toArray());
    }

    // G097-03: flatten(int[][], true) uses a compacted row walk for sparse jagged input; pins column-major order on both walks.
    @Test
    public void testFlattenVertically_jaggedRowsPinned() {
        final int[][][] cases = { { { 1, 2, 3 }, {}, null, { 4 }, { 5, 6 } }, { {}, { 1, 2, 3, 4, 5 }, {}, null }, { null, {}, { 7 } },
                { { 1 }, { 2, 3, 4 }, null, { 5, 6 }, {}, { 8, 9, 10, 11 } }, { { 1, 2 }, { 3, 4 }, { 5, 6 } }, { { 1, 2, 3 }, { 4 } }, { { 1 }, { 2, 3 } },
                { null, null }, { {}, {} } };

        for (final int[][] a : cases) {
            final IntList expected = new IntList();
            int maxLen = 0;
            for (final int[] row : a) {
                maxLen = Math.max(maxLen, row == null ? 0 : row.length);
            }
            for (int col = 0; col < maxLen; col++) {
                for (final int[] row : a) {
                    if (row != null && col < row.length) {
                        expected.add(row[col]);
                    }
                }
            }

            assertArrayEquals(expected.toArray(), IntStream.flatten(a, true).toArray(), Arrays.deepToString(a));
            assertEquals(expected.size(), IntStream.flatten(a, true).count());
            if (expected.size() > 1) {
                assertArrayEquals(Arrays.copyOfRange(expected.toArray(), 1, expected.size()), IntStream.flatten(a, true).skip(1).toArray());
            }
        }

        final Random random = new Random(3);
        for (int round = 0; round < 200; round++) {
            // odd rounds: mostly null/empty rows plus a few long ones (the sparse walk); even rounds: dense jagged rows
            final boolean sparse = round % 2 == 1;
            final int[][] a = new int[2 + random.nextInt(sparse ? 30 : 8)][];
            int maxLen = 0;
            for (int i = 0; i < a.length; i++) {
                final int len = sparse ? (random.nextInt(6) == 0 ? random.nextInt(40) : random.nextInt(3) - 1) : random.nextInt(6) - 1;
                a[i] = len < 0 ? null : Array.range(i * 100, i * 100 + len);
                maxLen = Math.max(maxLen, Math.max(len, 0));
            }
            final IntList expected = new IntList();
            for (int col = 0; col < maxLen; col++) {
                for (final int[] row : a) {
                    if (row != null && col < row.length) {
                        expected.add(row[col]);
                    }
                }
            }
            assertArrayEquals(expected.toArray(), IntStream.flatten(a, true).toArray(), Arrays.deepToString(a));
            assertEquals(expected.size(), IntStream.flatten(a, true).count(), Arrays.deepToString(a));
            if (expected.size() > 2) {
                final IntIterator it = IntStream.flatten(a, true).skip(2).iterator();
                for (int i = 2; i < expected.size(); i++) {
                    assertTrue(it.hasNext());
                    assertEquals(expected.get(i), it.nextInt());
                }
                assertFalse(it.hasNext());
            }
        }

        // sparse: rows * longest row far exceeds the element count
        final int[][] sparse = { null, {}, { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, {}, {}, { 10, 11 }, {}, null, {}, {}, { 12, 13, 14, 15 }, {} };
        assertArrayEquals(new int[] { 1, 10, 12, 2, 11, 13, 3, 14, 4, 15, 5, 6, 7, 8, 9 }, IntStream.flatten(sparse, true).toArray());
        final int[][] oneLongRow = new int[1000][];
        oneLongRow[999] = Array.range(0, 5000);
        assertArrayEquals(Array.range(0, 5000), IntStream.flatten(oneLongRow, true).toArray());
        final IntIterator sparseIter = IntStream.flatten(new int[][] { {}, { 1, 2 }, null, {}, {}, {}, { 3 }, {}, {} }, true).iterator();
        assertEquals(1, sparseIter.nextInt());
        assertEquals(3, sparseIter.nextInt());
        assertEquals(2, sparseIter.nextInt());
        assertFalse(sparseIter.hasNext());
        assertThrows(NoSuchElementException.class, sparseIter::nextInt);

        final IntIterator iter = IntStream.flatten(new int[][] { { 1 }, {}, { 2, 3 } }, true).iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextInt());
        assertEquals(2, iter.nextInt());
        assertEquals(3, iter.nextInt());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextInt);
        assertThrows(NoSuchElementException.class, iter::nextInt);
    }

    @Test
    public void testFlattenWithPadding() {
        int[][] arrays = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        IntStream stream = IntStream.flatten(arrays, 0, false);
        assertArrayEquals(new int[] { 1, 2, 0, 3, 4, 5, 6, 0, 0 }, stream.toArray());
    }

    // flattMap

    @Test
    public void testFlattMap() {
        // flattMap uses JDK IntStream
        IntStream result = createIntStream(1, 2, 3).flattMap(n -> java.util.stream.IntStream.of(n, n * 10));
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, result.toArray());
    }

    @Test
    public void testFlattMap_Empty() {
        IntStream result = IntStream.empty().flattMap(n -> java.util.stream.IntStream.of(n, n * 10));
        assertEquals(0, result.count());
    }

    @Test
    public void testFlattMap_HappyPath() {
        int[] result = IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10)).toArray();
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    // from

    @Test
    public void testFrom() {
        java.util.stream.IntStream jdkStream = java.util.stream.IntStream.of(1, 2, 3);
        IntStream result = IntStream.from(jdkStream);
        assertArrayEquals(new int[] { 1, 2, 3 }, result.toArray());
    }

    @Test
    public void testFrom_Empty() {
        java.util.stream.IntStream jdkStream = java.util.stream.IntStream.empty();
        IntStream result = IntStream.from(jdkStream);
        assertEquals(0, result.count());
    }

    @Test
    public void testFrom_JdkStream() {
        java.util.stream.IntStream jdkStream = java.util.stream.IntStream.of(1, 2, 3);
        stream = IntStream.from(jdkStream);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testFromJavaStream() {
        java.util.stream.IntStream javaStream = java.util.stream.IntStream.of(1, 2, 3);
        stream = IntStream.from(javaStream);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testFromJdkStreamIteratorCountConsumes() {
        try (IntStream stream = IntStream.from(java.util.stream.IntStream.of(1, 2))) {
            final IntIterator iter = stream.iterator();

            assertEquals(2, iter.count());
            assertFalse(iter.hasNext());
            assertEquals(0, iter.count());
            assertArrayEquals(new int[0], iter.toArray());
            assertThrows(NoSuchElementException.class, iter::nextInt);
        }
    }

    @Test
    public void testFromJdkStreamIteratorToArrayConsumes() {
        try (IntStream stream = IntStream.from(java.util.stream.IntStream.of(1, 2))) {
            final IntIterator iter = stream.iterator();

            assertArrayEquals(new int[] { 1, 2 }, iter.toArray());
            assertFalse(iter.hasNext());
            assertEquals(0, iter.count());
            assertThrows(NoSuchElementException.class, iter::nextInt);
        }
    }

    @Test
    public void testFromIterator() {
        IntIterator iter = new IntIteratorEx() {
            private int n = 0;

            @Override
            public boolean hasNext() {
                return n < 5;
            }

            @Override
            public int nextInt() {
                return n++;
            }
        };

        IntStream stream = createIntStream(iter);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    // generate

    @Test
    public void testGenerate() {
        AtomicInteger counter = new AtomicInteger(0);
        stream = IntStream.generate(counter::getAndIncrement).limit(5);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testGenerate_LimitedElements() {
        AtomicInteger counter = new AtomicInteger(0);
        stream = IntStream.generate(() -> counter.incrementAndGet()).limit(5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void generate_shouldCreateInfiniteStream() {
        assertArrayEquals(new int[] { 0, 0, 0 }, IntStream.generate(() -> 0).limit(3).toArray());
    }

    // iterate

    @Test
    public void testIterate() {
        stream = IntStream.iterate(1, n -> n * 2).limit(5);
        assertArrayEquals(new int[] { 1, 2, 4, 8, 16 }, stream.toArray());

        stream = IntStream.iterate(1, n -> n < 10, n -> n + 2);
        assertArrayEquals(new int[] { 1, 3, 5, 7, 9 }, stream.toArray());
    }

    @Test
    public void test_iterate_02() {
        IntStream stream = IntStream.iterate(() -> true, () -> 1);
        assertEquals(10, stream.limit(10).mapToFloat(e -> (float) e).count());

        stream = IntStream.iterate(1, () -> true, it -> it);
        assertEquals(10, stream.limit(10).mapToLong(e -> e).sum());

        stream = IntStream.iterate(1, it -> true, it -> it);
        assertEquals(10, stream.limit(10).mapToLong(e -> e).sum());
    }

    // iterate(BooleanSupplier, IntSupplier) - BooleanSupplier-gated stream
    @Test
    public void testIterate_BooleanSupplierAndIntSupplier() {
        AtomicInteger counter = new AtomicInteger(10);
        int[] result = IntStream.iterate(() -> counter.get() > 7, () -> counter.getAndDecrement()).toArray();
        assertArrayEquals(new int[] { 10, 9, 8 }, result);
    }

    @Test
    public void testIterate_BooleanSupplierFalseImmediately() {
        int[] result = IntStream.iterate(() -> false, () -> 42).toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testIterate_InfiniteNegative() {
        int[] result = IntStream.iterate(10, x -> x - 2).limit(5).toArray();
        assertArrayEquals(new int[] { 10, 8, 6, 4, 2 }, result);
    }

    // iterate(int, IntUnaryOperator) - infinite stream
    @Test
    public void testIterate_InfiniteWithLimit() {
        int[] result = IntStream.iterate(0, x -> x + 3).limit(4).toArray();
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, result);
    }

    @Test
    public void iterate_shouldCreateInfiniteStream() {
        assertArrayEquals(new int[] { 0, 2, 4 }, IntStream.iterate(0, i -> i + 2).limit(3).toArray());
    }

    @Test
    public void testIterate_WithBooleanSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        stream = IntStream.iterate(() -> counter.get() < 5, () -> counter.incrementAndGet());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    // iterate(int, BooleanSupplier, IntUnaryOperator) - finite stream with BooleanSupplier
    @Test
    public void testIterate_WithBooleanSupplierAndInit() {
        AtomicInteger calls = new AtomicInteger(0);
        int[] result = IntStream.iterate(1, () -> calls.incrementAndGet() <= 3, x -> x + 10).toArray();
        assertArrayEquals(new int[] { 1, 11, 21 }, result);
    }

    @Test
    public void testIterate_WithBooleanSupplierAndUnaryOperator() {
        AtomicInteger counter = new AtomicInteger(0);
        stream = IntStream.iterate(1, () -> counter.incrementAndGet() < 5, x -> x * 2);
        assertEquals(4, stream.count());
    }

    @Test
    public void testIterate_WithInitAndUnaryOperator() {
        stream = IntStream.iterate(1, x -> x * 2).limit(5);
        assertArrayEquals(new int[] { 1, 2, 4, 8, 16 }, stream.toArray());
    }

    @Test
    public void testIterate_WithIntPredicate_FalseFromStart() {
        int[] result = IntStream.iterate(100, x -> x < 10, x -> x + 1).toArray();
        assertArrayEquals(new int[0], result);
    }

    // iterate(int, IntPredicate, IntUnaryOperator) - finite stream with predicate
    @Test
    public void testIterate_WithIntPredicate_HappyPath() {
        int[] result = IntStream.iterate(1, x -> x <= 16, x -> x * 2).toArray();
        assertArrayEquals(new int[] { 1, 2, 4, 8, 16 }, result);
    }

    @Test
    public void testIterate_WithPredicate() {
        stream = IntStream.iterate(1, x -> x < 100, x -> x * 2);
        assertArrayEquals(new int[] { 1, 2, 4, 8, 16, 32, 64 }, stream.toArray());
    }

    @Test
    public void testIteratePredicateRetriesFunctionResultAfterThrow() {
        final AtomicInteger tests = new AtomicInteger();
        final AtomicInteger fCalls = new AtomicInteger();
        final IntIterator iter = IntStream.iterate(0, i -> {
            if (i > 0 && tests.getAndIncrement() == 0) {
                throw new IllegalStateException("candidate test failed");
            }
            return i < 2;
        }, i -> {
            fCalls.incrementAndGet();
            return i + 1;
        }).iterator();

        assertEquals(0, iter.nextInt());
        assertThrows(IllegalStateException.class, iter::hasNext);
        assertEquals(1, fCalls.get());
        assertEquals(1, iter.nextInt());
        assertEquals(1, fCalls.get());
        assertFalse(iter.hasNext());
        assertEquals(2, fCalls.get());
    }

    @Test
    public void testIteratePredicateRetriesSeedAfterThrow() {
        final AtomicInteger tests = new AtomicInteger();
        final IntIterator iter = IntStream.iterate(0, i -> {
            if (tests.getAndIncrement() == 0) {
                throw new IllegalStateException("seed test failed");
            }
            return i < 2;
        }, i -> i + 1).iterator();

        assertThrows(IllegalStateException.class, iter::hasNext);
        assertEquals(0, iter.nextInt());
        assertEquals(1, iter.nextInt());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterateWithBooleanSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        IntStream stream = IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement());

        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testIterateWithFalseHasNext() {
        stream = IntStream.iterate(() -> false, () -> 1);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOf() {
        stream = createIntStream(5);
        assertArrayEquals(new int[] { 5 }, stream.toArray());

        stream = createIntStream(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());

        stream = createIntStream(new int[] {});
        assertEquals(0, stream.count());
    }

    @Test
    public void of_array_shouldCreateStream() {
        int[] source = { 1, 2, 3 };
        assertArrayEquals(source, IntStream.of(source).toArray());
    }

    @Test
    public void testOf_ArrayWithFromTo_EmptyRange() {
        int[] src = { 1, 2, 3 };
        assertEquals(0, IntStream.of(src, 2, 2).count());
    }

    @Test
    public void testOf_ArrayWithFromTo_FullRange() {
        int[] src = { 1, 2, 3 };
        int[] result = IntStream.of(src, 0, 3).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    // of(int[], int, int) - array range factory
    @Test
    public void testOf_ArrayWithFromTo_HappyPath() {
        int[] src = { 10, 20, 30, 40, 50 };
        int[] result = IntStream.of(src, 1, 4).toArray();
        assertArrayEquals(new int[] { 20, 30, 40 }, result);
    }

    @Test
    public void of_emptyArray_shouldCreateEmptyStream() {
        assertEquals(0, IntStream.of(new int[0]).count());
    }

    @Test
    public void testOf_JdkOptionalInt_Null() {
        assertEquals(0, IntStream.of((java.util.OptionalInt) null).count());
    }

    // of(java.util.OptionalInt) - JDK OptionalInt
    @Test
    public void testOf_JdkOptionalInt_Present() {
        int[] result = IntStream.of(java.util.OptionalInt.of(55)).toArray();
        assertArrayEquals(new int[] { 55 }, result);
    }

    @Test
    public void testOf_LandawnOptionalInt_Null() {
        assertEquals(0, IntStream.of((OptionalInt) null).count());
    }

    // of(u.OptionalInt) - Landawn's OptionalInt
    @Test
    public void testOf_LandawnOptionalInt_Present() {
        int[] result = IntStream.of(OptionalInt.of(99)).toArray();
        assertArrayEquals(new int[] { 99 }, result);
    }

    @Test
    public void of_singleElement_shouldCreateStream() {
        assertEquals(1, IntStream.of(1).count());
        assertEquals(1, IntStream.of(1).first().orElseThrow());
    }

    @Test
    public void testOf_WithArray() {
        stream = IntStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testOf_WithArrayRange() {
        stream = IntStream.of(new int[] { 1, 2, 3, 4, 5 }, 1, 4);
        assertArrayEquals(new int[] { 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testOf_WithCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4);
        stream = IntStream.of(list);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testOf_WithEmptyArray() {
        stream = IntStream.of(new int[0]);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOf_WithIntegerArray() {
        stream = IntStream.of(new Integer[] { 1, 2, 3 });
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testOf_WithIntegerArrayRange() {
        stream = IntStream.of(new Integer[] { 1, 2, 3, 4, 5 }, 1, 4);
        assertArrayEquals(new int[] { 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testOf_WithIntIterator() {
        IntIterator iterator = IntIterator.of(1, 2, 3);
        stream = IntStream.of(iterator);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testOf_WithJdkOptionalInt() {
        stream = IntStream.of(java.util.OptionalInt.of(42));
        assertArrayEquals(new int[] { 42 }, stream.toArray());
    }

    //
    @Test
    public void testOf_WithOptionalInt() {
        stream = IntStream.of(OptionalInt.of(42));
        assertArrayEquals(new int[] { 42 }, stream.toArray());
    }

    // ofCodePoints

    @Test
    public void testOfCodePoints() {
        IntStream stream = IntStream.ofCodePoints("Hello");
        assertArrayEquals(new int[] { 72, 101, 108, 108, 111 }, stream.toArray());
    }

    @Test
    public void testOfCodePoints_EmptyString() {
        int[] result = IntStream.ofCodePoints("").toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testOfCodePoints_HappyPath() {
        int[] result = IntStream.ofCodePoints("ABC").toArray();
        assertArrayEquals(new int[] { 65, 66, 67 }, result);
    }

    @Test
    public void testOfCodePoints_Null() {
        int[] result = IntStream.ofCodePoints(null).toArray();
        assertArrayEquals(new int[0], result);
    }

    // ofIndices

    @Test
    public void testOfIndices_BasicIndices() {
        stream = IntStream.ofIndices(5);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testOfIndices_RangeWithStep_NegativeStep() {
        int[] result = IntStream.ofIndices(4, -1).toArray();
        assertArrayEquals(new int[] { 3, 2, 1, 0 }, result);
    }

    @Test
    public void testOfIndices_RangeWithStep_StepOne() {
        int[] result = IntStream.ofIndices(4, 1).toArray();
        assertArrayEquals(new int[] { 0, 1, 2, 3 }, result);
    }

    // ofIndices(int, int) - range-based with step > 1
    @Test
    public void testOfIndices_RangeWithStep_StepTwo() {
        int[] result = IntStream.ofIndices(6, 2).toArray();
        assertArrayEquals(new int[] { 0, 2, 4 }, result);
    }

    @Test
    public void testOfIndices_RangeWithStep_Zero() {
        assertThrows(IllegalArgumentException.class, () -> IntStream.ofIndices(5, 0));
    }

    @Test
    public void testOfIndices_SourceWithFromIndex_NullSource() {
        int[] result = IntStream.ofIndices((int[]) null, 0, 1, (arr, fromIdx) -> fromIdx < 3 ? fromIdx : -1).toArray();
        assertArrayEquals(new int[0], result);
    }

    // ofIndices(Object, int, int, ObjIntFunction) - source with fromIndex and increment
    @Test
    public void testOfIndices_SourceWithFromIndexAndIncrement() {
        int[] source = { 1, 2, 3, 1, 5, 1 };
        int[] result = IntStream.ofIndices(source, 1, 1, (arr, fromIdx) -> {
            for (int i = fromIdx; i < arr.length; i++) {
                if (arr[i] == 1) {
                    return i;
                }
            }
            return -1;
        }).toArray();
        assertArrayEquals(new int[] { 3, 5 }, result);
    }

    @Test
    public void testOfIndices_WithNegativeStep() {
        int[] result = IntStream.ofIndices(5, -1).toArray();
        assertArrayEquals(new int[] { 4, 3, 2, 1, 0 }, result);
    }

    @Test
    public void testOfIndices_WithSource() {
        String source = "hello";
        stream = IntStream.ofIndices(source, (s, i) -> i < s.length() ? i : null);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testOfIndices_WithSourceAndIncrement() {
        String source = "aXaXaX";
        int[] indices = IntStream.ofIndices(source, 0, 2, (s, fromIndex) -> {
            int idx = s.indexOf('a', fromIndex);
            return idx >= 0 ? idx : null;
        }).toArray();
        assertArrayEquals(new int[] { 0, 2, 4 }, indices);
    }

    @Test
    public void testOfIndices_WithStep() {
        stream = IntStream.ofIndices(10, 2);
        assertArrayEquals(new int[] { 0, 2, 4, 6, 8 }, stream.toArray());
    }

    @Test
    public void testOfIndicesRetriesFailedInitialIndexLookup() {
        for (boolean firstPullIsNext : new boolean[] { false, true }) {
            for (int increment : new int[] { -1, 1 }) {
                AtomicInteger attempts = new AtomicInteger();
                List<Integer> requestedIndices = new ArrayList<>();
                int start = increment < 0 ? 2 : 0;

                try (IntStream indices = IntStream.ofIndices("abc", start, increment, (source, fromIndex) -> {
                    requestedIndices.add(fromIndex);
                    if (attempts.getAndIncrement() == 0) {
                        throw new IllegalStateException("initial lookup");
                    }
                    return fromIndex;
                })) {
                    IntIterator iterator = indices.iterator();
                    if (firstPullIsNext) {
                        assertThrows(IllegalStateException.class, iterator::nextInt);
                    } else {
                        assertThrows(IllegalStateException.class, iterator::hasNext);
                    }

                    assertTrue(iterator.hasNext());
                    assertTrue(iterator.hasNext());
                    assertArrayEquals(increment < 0 ? new int[] { 2, 1, 0 } : new int[] { 0, 1, 2 }, iterator.toArray());
                    assertEquals(increment < 0 ? Arrays.asList(2, 2, 1, 0) : Arrays.asList(0, 0, 1, 2), requestedIndices);
                    assertFalse(iterator.hasNext());
                    assertThrows(NoSuchElementException.class, iterator::nextInt);
                }
            }
        }
    }

    @Test
    public void testOfIndicesTreatsNullIndexAsEnd() {
        assertArrayEquals(new int[] { 0, 1 }, IntStream.ofIndices(new int[] { 9, 8, 7 }, (a, i) -> i < 2 ? i : null).toArray());
    }

    @Test
    public void testOfIntBuffer() {
        IntBuffer buffer = IntBuffer.wrap(new int[] { 1, 2, 3 });
        IntStream result = IntStream.of(buffer);
        assertArrayEquals(new int[] { 1, 2, 3 }, result.toArray());
    }

    @Test
    public void testOfIntBuffer_Empty() {
        IntBuffer buffer = IntBuffer.wrap(new int[] {});
        IntStream result = IntStream.of(buffer);
        assertEquals(0, result.count());
    }

    // ofNullable

    @Test
    public void testOfNullable() {
        stream = IntStream.ofNullable(null);
        assertEquals(0, stream.count());

        stream = IntStream.ofNullable(42);
        assertArrayEquals(new int[] { 42 }, stream.toArray());
    }

    // ofNullable(Integer) - both null and non-null paths
    @Test
    public void testOfNullable_NonNull() {
        int[] result = IntStream.ofNullable(Integer.valueOf(77)).toArray();
        assertArrayEquals(new int[] { 77 }, result);
    }

    @Test
    public void testOfNullable_NullInteger() {
        assertEquals(0, IntStream.ofNullable((Integer) null).count());
    }

    @Test
    public void testOfNullable_WithNull() {
        stream = IntStream.ofNullable(null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullable_WithValue() {
        stream = IntStream.ofNullable(42);
        assertArrayEquals(new int[] { 42 }, stream.toArray());
    }

    @Test
    public void testOfWithRange() {
        int[] arr = { 1, 2, 3, 4, 5 };
        stream = createIntStream(arr, 1, 4);
        assertArrayEquals(new int[] { 2, 3, 4 }, stream.toArray());
    }

    // random

    @Test
    public void testRandom_Basic() {
        stream = IntStream.random().limit(10);
        assertEquals(10, stream.count());
    }

    @Test
    public void testRandom_InRange() {
        int[] result = IntStream.random(5, 100).limit(10).toArray();
        assertEquals(10, result.length);
        for (int v : result) {
            assertTrue(v >= 5 && v < 100);
        }
    }

    // G115-01: random() streams are lazy and look up ThreadLocalRandom.current() per draw, so a stream created on one
    // thread can be consumed on another (and several such streams concurrently).
    @Test
    public void testRandom_primitiveStreamsConsumedOnOtherThreads() throws Exception {
        final IntStream created = IntStream.random(-3, 3).limit(500);
        final ExecutorService executor = Executors.newFixedThreadPool(4);

        try {
            final java.util.concurrent.Future<int[]> handedOver = executor.submit(() -> created.toArray());
            final int[] fromOtherThread = handedOver.get();
            assertEquals(500, fromOtherThread.length);
            for (final int value : fromOtherThread) {
                assertTrue(value >= -3 && value < 3);
            }

            final List<java.util.concurrent.Future<Boolean>> futures = new ArrayList<>();

            for (int t = 0; t < 4; t++) {
                futures.add(executor.submit(() -> {
                    final int[] ints = IntStream.random().limit(1000).toArray();
                    assertTrue(IntList.of(ints).min().orElseThrow() < 0 && IntList.of(ints).max().orElseThrow() > 0);
                    assertEquals(1000L, LongStream.random().limit(1000).distinct().count());
                    assertTrue(DoubleStream.random().limit(1000).allMatch(d -> d >= 0d && d < 1d));
                    assertTrue(FloatStream.random().limit(1000).allMatch(f -> f >= 0f && f < 1f));
                    assertTrue(ByteStream.random().limit(1000).distinct().count() > 100);
                    assertTrue(ShortStream.random().limit(1000).distinct().count() > 500);
                    assertTrue(CharStream.random().limit(1000).distinct().count() > 500);
                    assertTrue(CharStream.random('a', 'c').limit(1000).allMatch(c -> c == 'a' || c == 'b'));
                    assertEquals(2, CharStream.random(new char[] { 'x', 'y' }).limit(1000).distinct().count());
                    return true;
                }));
            }

            for (final java.util.concurrent.Future<Boolean> future : futures) {
                assertTrue(future.get());
            }
        } finally {
            executor.shutdown();
        }

        // Validation is unchanged: still eager, before any value is drawn.
        assertThrows(IllegalArgumentException.class, () -> IntStream.random(5, 5));
        assertThrows(IllegalArgumentException.class, () -> CharStream.random('z', 'a'));
        assertEquals(0, CharStream.random(new char[0]).count());
    }

    @Test
    public void testRandom_WithRange() {
        stream = IntStream.random(1, 10).limit(100);
        int[] result = stream.toArray();
        for (int val : result) {
            assertTrue(val >= 1 && val < 10);
        }
    }

    @Test
    public void testRandom_WithRange_AllInRange() {
        int[] values = IntStream.random(10, 20).limit(100).toArray();
        assertEquals(100, values.length);
        for (int v : values) {
            assertTrue(v >= 10 && v < 20, "Value " + v + " should be in range [10, 20)");
        }
    }

    // range

    @Test
    public void testRange() {
        stream = IntStream.range(1, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());

        stream = IntStream.range(5, 5);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRange_BasicRange() {
        stream = IntStream.range(0, 5);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testRange_EmptyRange() {
        stream = IntStream.range(5, 5);
        assertEquals(0, stream.count());
    }

    @Test
    public void range_emptyRange_shouldCreateEmptyStream() {
        assertEquals(0, IntStream.range(3, 3).count());
    }

    @Test
    public void testRange_NegativeToPositive() {
        int[] result = IntStream.range(-2, 3).toArray();
        assertArrayEquals(new int[] { -2, -1, 0, 1, 2 }, result);
    }

    @Test
    public void range_shouldCreateStreamOfRange() {
        assertArrayEquals(new int[] { 0, 1, 2 }, IntStream.range(0, 3).toArray());
    }

    @Test
    public void testRange_WithNegativeStep() {
        stream = IntStream.range(10, 0, -2);
        assertArrayEquals(new int[] { 10, 8, 6, 4, 2 }, stream.toArray());
    }

    // range(int, int) with advance() exercised via skip/limit
    @Test
    public void testRange_WithSkip() {
        int[] result = IntStream.range(0, 10).skip(3).toArray();
        assertArrayEquals(new int[] { 3, 4, 5, 6, 7, 8, 9 }, result);
    }

    @Test
    public void testRange_WithStep() {
        stream = IntStream.range(0, 10, 2);
        assertArrayEquals(new int[] { 0, 2, 4, 6, 8 }, stream.toArray());
    }

    @Test
    public void testRangeWithInvalidStepDirection() {
        stream = IntStream.range(1, 5, -1);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRangeWithStep() {
        stream = IntStream.range(0, 10, 2);
        assertArrayEquals(new int[] { 0, 2, 4, 6, 8 }, stream.toArray());

        stream = IntStream.range(10, 0, -2);
        assertArrayEquals(new int[] { 10, 8, 6, 4, 2 }, stream.toArray());
    }

    @Test
    public void testRangeWithZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> IntStream.range(0, 10, 0));
    }

    // rangeMap

    @Test
    public void testRangeMap() {
        IntStream stream = createIntStream(1, 1, 2, 2, 2, 3).rangeMap((a, b) -> a == b, (first, last) -> first * 10 + last);
        assertArrayEquals(new int[] { 11, 22, 33 }, stream.toArray());
    }

    @Test
    public void testRangeMap_AllSame() {
        int[] result = IntStream.of(5, 5, 5).rangeMap((a, b) -> a == b, Integer::sum).toArray();
        assertArrayEquals(new int[] { 10 }, result);
    }

    @Test
    public void testRangeMap_HappyPath() {
        int[] result = IntStream.of(1, 1, 2, 2, 3).rangeMap((a, b) -> a == b, Integer::sum).toArray();
        assertArrayEquals(new int[] { 2, 4, 6 }, result);
    }

    @Test
    public void testRangeMapToObj() {
        Stream<String> stream = createIntStream(1, 1, 2, 2, 2, 3).rangeMapToObj((a, b) -> a == b, (first, last) -> first + "-" + last);
        assertArrayEquals(new String[] { "1-1", "2-2", "3-3" }, stream.toArray());
    }

    @Test
    public void testRangeMapToObj_HappyPath() {
        List<String> result = IntStream.of(1, 1, 2, 3, 3).rangeMapToObj((a, b) -> a == b, (a, b) -> a + "-" + b).toList();
        assertEquals(Arrays.asList("1-1", "2-2", "3-3"), result);
    }

    // repeat

    @Test
    public void testRepeat() {
        stream = IntStream.repeat(7, 3);
        assertArrayEquals(new int[] { 7, 7, 7 }, stream.toArray());

        stream = IntStream.repeat(7, 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRepeat_02() {
        IntStream stream = IntStream.repeat(1, 1000);
        assertEquals(1000, stream.mapToFloat(e -> (float) e).count());
    }

    @Test
    public void testRepeat_BasicRepeat() {
        stream = IntStream.repeat(5, 3);
        assertArrayEquals(new int[] { 5, 5, 5 }, stream.toArray());
    }

    @Test
    public void testRepeat_ExactlyTen() {
        int[] result = IntStream.repeat(3, 10).toArray();
        assertEquals(10, result.length);
        for (int v : result) {
            assertEquals(3, v);
        }
    }

    // Tests targeting uncovered code paths identified by coverage report

    // repeat(int, long) with large n uses IteratorIntStream path (n >= 10)
    @Test
    public void testRepeat_LargeCount() {
        int[] result = IntStream.repeat(7, 15).toArray();
        assertEquals(15, result.length);
        for (int v : result) {
            assertEquals(7, v);
        }
    }

    @Test
    public void testRepeat_ZeroTimes() {
        stream = IntStream.repeat(5, 0);
        assertEquals(0, stream.count());
    }

}
