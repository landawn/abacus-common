package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.LongBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.IndexedLong;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalLong;

@Tag("2025")
public class LongStream2025Test extends TestBase {

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
    public void testDeferWithNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.defer(null));
    }

    @Test
    public void testFromJavaStream() {
        java.util.stream.LongStream javaStream = java.util.stream.LongStream.of(1L, 2L, 3L);
        LongStream stream = LongStream.from(javaStream);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, stream.toArray());
    }

    @Test
    public void testFromNullJavaStream() {
        LongStream stream = LongStream.from((java.util.stream.LongStream) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFromJavaStreamEmpty() {
        java.util.stream.LongStream javaStream = java.util.stream.LongStream.empty();
        LongStream stream = LongStream.from(javaStream);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullable() {
        assertEquals(1, LongStream.ofNullable(5L).count());
        assertEquals(0, LongStream.ofNullable(null).count());

        assertArrayEquals(new long[] { 100L }, LongStream.ofNullable(100L).toArray());
    }

    @Test
    public void testOfVarArgs() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongStream stream = LongStream.of(array);
        assertArrayEquals(array, stream.toArray());
    }

    @Test
    public void testOfEmptyArray() {
        LongStream stream = LongStream.of(new long[0]);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfSingleElement() {
        LongStream stream = LongStream.of(42L);
        assertArrayEquals(new long[] { 42L }, stream.toArray());
    }

    @Test
    public void testOfArrayWithRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongStream stream = LongStream.of(array, 1, 4);
        assertArrayEquals(new long[] { 2L, 3L, 4L }, stream.toArray());
    }

    @Test
    public void testOfArrayWithFullRange() {
        long[] array = { 1L, 2L, 3L };
        LongStream stream = LongStream.of(array, 0, 3);
        assertArrayEquals(array, stream.toArray());
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
    public void testOfCollection() {
        List<Long> list = Arrays.asList(1L, 2L, 3L, 4L);
        LongStream stream = LongStream.of(list);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, stream.toArray());
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
    public void testOfJavaStream() {
        java.util.stream.LongStream javaStream = java.util.stream.LongStream.of(10L, 20L, 30L);
        LongStream stream = LongStream.of(javaStream);
        assertArrayEquals(new long[] { 10L, 20L, 30L }, stream.toArray());
    }

    @Test
    public void testOfLongBuffer() {
        LongBuffer buffer = LongBuffer.wrap(new long[] { 1L, 2L, 3L, 4L });
        LongStream stream = LongStream.of(buffer);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, stream.toArray());
    }

    @Test
    public void testOfOptionalLong() {
        LongStream stream1 = LongStream.of(OptionalLong.of(5L));
        assertEquals(1, stream1.count());

        LongStream stream2 = LongStream.of(OptionalLong.empty());
        assertEquals(0, stream2.count());
    }

    @Test
    public void testOfJdkOptionalLong() {
        LongStream stream1 = LongStream.of(java.util.OptionalLong.of(10L));
        assertArrayEquals(new long[] { 10L }, stream1.toArray());

        LongStream stream2 = LongStream.of(java.util.OptionalLong.empty());
        assertEquals(0, stream2.count());
    }

    @Test
    public void testRange() {
        assertArrayEquals(new long[] { 0L, 1L, 2L, 3L, 4L }, LongStream.range(0L, 5L).toArray());
        assertArrayEquals(new long[] {}, LongStream.range(5L, 5L).toArray());
        assertArrayEquals(new long[] {}, LongStream.range(5L, 0L).toArray());
    }

    @Test
    public void testRangeLarge() {
        LongStream stream = LongStream.range(0L, 1000L);
        assertEquals(1000, stream.count());
    }

    @Test
    public void testRangeWithStep() {
        assertArrayEquals(new long[] { 0L, 2L, 4L }, LongStream.range(0L, 6L, 2L).toArray());
        assertArrayEquals(new long[] { 5L, 3L, 1L }, LongStream.range(5L, 0L, -2L).toArray());
        assertArrayEquals(new long[] { 10L, 7L, 4L, 1L }, LongStream.range(10L, 0L, -3L).toArray());
    }

    @Test
    public void testRangeWithZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.range(0L, 10L, 0L));
    }

    @Test
    public void testRangeClosed() {
        assertArrayEquals(new long[] { 0L, 1L, 2L, 3L, 4L, 5L }, LongStream.rangeClosed(0L, 5L).toArray());
        assertArrayEquals(new long[] { 5L }, LongStream.rangeClosed(5L, 5L).toArray());
        assertArrayEquals(new long[] {}, LongStream.rangeClosed(5L, 0L).toArray());
    }

    @Test
    public void testRangeClosedWithStep() {
        assertArrayEquals(new long[] { 0L, 2L, 4L, 6L }, LongStream.rangeClosed(0L, 6L, 2L).toArray());
        assertArrayEquals(new long[] { 5L, 3L, 1L }, LongStream.rangeClosed(5L, 1L, -2L).toArray());
    }

    @Test
    public void testRepeat() {
        assertArrayEquals(new long[] { 5L, 5L, 5L }, LongStream.repeat(5L, 3).toArray());
        assertArrayEquals(new long[] {}, LongStream.repeat(5L, 0).toArray());
        assertArrayEquals(new long[] { 42L }, LongStream.repeat(42L, 1).toArray());
    }

    @Test
    public void testRepeatNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.repeat(5L, -1));
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

    @Test
    public void testIterate() {
        LongStream stream = LongStream.iterate(1L, n -> n * 2).limit(5);
        assertArrayEquals(new long[] { 1L, 2L, 4L, 8L, 16L }, stream.toArray());
    }

    @Test
    public void testIterateWithPredicate() {
        LongStream stream = LongStream.iterate(1L, n -> n < 10L, n -> n + 2);
        assertArrayEquals(new long[] { 1L, 3L, 5L, 7L, 9L }, stream.toArray());
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
    public void testIterateWithNullOperator() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.iterate(1L, null));
    }

    @Test
    public void testGenerate() {
        AtomicLong counter = new AtomicLong(0);
        LongStream stream = LongStream.generate(counter::incrementAndGet).limit(5);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, stream.toArray());
    }

    @Test
    public void testGenerateWithNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.generate(null));
    }

    @Test
    public void testConcat() {
        long[] a1 = { 1L, 2L };
        long[] a2 = { 3L, 4L };
        long[] a3 = { 5L, 6L };

        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, LongStream.concat(a1, a2, a3).toArray());
    }

    @Test
    public void testConcatEmptyArrays() {
        assertArrayEquals(new long[] {}, LongStream.concat(new long[0], new long[0]).toArray());
    }

    @Test
    public void testConcatStreams() {
        LongStream s1 = LongStream.of(1L, 2L);
        LongStream s2 = LongStream.of(3L, 4L);
        LongStream s3 = LongStream.of(5L, 6L);

        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, LongStream.concat(s1, s2, s3).toArray());
    }

    @Test
    public void testConcatIterators() {
        LongIterator i1 = LongIterator.of(new long[] { 1L, 2L });
        LongIterator i2 = LongIterator.of(new long[] { 3L, 4L });

        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, LongStream.concat(i1, i2).toArray());
    }

    @Test
    public void testConcatList() {
        List<long[]> list = Arrays.asList(new long[] { 1L, 2L }, new long[] { 3L, 4L });
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, LongStream.concat(list).toArray());
    }

    @Test
    public void testConcatStreamCollection() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1L, 2L), LongStream.of(3L, 4L), LongStream.of(5L));
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, LongStream.concat(streams).toArray());
    }

    @Test
    public void testConcatIteratorsCollection() {
        Collection<LongIterator> iterators = Arrays.asList(LongIterator.of(new long[] { 1L, 2L }), LongIterator.of(new long[] { 3L, 4L }));
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, LongStream.concatIterators(iterators).toArray());
    }

    @Test
    public void testFlatten2D() {
        long[][] array = { { 1L, 2L }, { 3L, 4L }, { 5L } };
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, LongStream.flatten(array).toArray());
    }

    @Test
    public void testFlatten2DVertically() {
        long[][] array = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
        assertArrayEquals(new long[] { 1L, 4L, 2L, 5L, 3L, 6L }, LongStream.flatten(array, true).toArray());
    }

    @Test
    public void testFlatten2DHorizontally() {
        long[][] array = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, LongStream.flatten(array, false).toArray());
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
    public void testZip() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 4L, 5L, 6L };

        LongStream zipped = LongStream.zip(a, b, (x, y) -> x + y);
        assertArrayEquals(new long[] { 5L, 7L, 9L }, zipped.toArray());
    }

    @Test
    public void testZipWithDifferentLengths() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 5L, 6L };

        LongStream zipped = LongStream.zip(a, b, (x, y) -> x + y);
        assertArrayEquals(new long[] { 6L, 8L }, zipped.toArray());
    }

    @Test
    public void testZipThreeArrays() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 4L, 5L, 6L };
        long[] c = { 7L, 8L, 9L };

        LongStream zipped = LongStream.zip(a, b, c, (x, y, z) -> x + y + z);
        assertArrayEquals(new long[] { 12L, 15L, 18L }, zipped.toArray());
    }

    @Test
    public void testZipIterators() {
        LongIterator i1 = LongIterator.of(new long[] { 1L, 2L, 3L });
        LongIterator i2 = LongIterator.of(new long[] { 4L, 5L, 6L });

        LongStream zipped = LongStream.zip(i1, i2, (x, y) -> x * y);
        assertArrayEquals(new long[] { 4L, 10L, 18L }, zipped.toArray());
    }

    @Test
    public void testZipStreams() {
        LongStream s1 = LongStream.of(1L, 2L, 3L);
        LongStream s2 = LongStream.of(10L, 20L, 30L);

        LongStream zipped = LongStream.zip(s1, s2, (x, y) -> x + y);
        assertArrayEquals(new long[] { 11L, 22L, 33L }, zipped.toArray());
    }

    @Test
    public void testZipWithDefaultValues() {
        long[] a = { 1L, 2L };
        long[] b = { 3L, 4L, 5L };

        LongStream zipped = LongStream.zip(a, b, 0L, 0L, (x, y) -> x + y);
        assertArrayEquals(new long[] { 4L, 6L, 5L }, zipped.toArray());
    }

    @Test
    public void testMerge() {
        long[] a = { 1L, 3L, 5L };
        long[] b = { 2L, 4L, 6L };

        LongStream merged = LongStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, merged.toArray());
    }

    @Test
    public void testMergeIterators() {
        LongIterator i1 = LongIterator.of(new long[] { 1L, 3L, 5L });
        LongIterator i2 = LongIterator.of(new long[] { 2L, 4L, 6L });

        LongStream merged = LongStream.merge(i1, i2, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, merged.toArray());
    }

    @Test
    public void testFilter() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.filter(n -> n % 2 == 0).toArray();
        assertArrayEquals(new long[] { 2L, 4L }, result);
    }

    @Test
    public void testFilterNoneMatch() {
        LongStream stream = LongStream.of(1L, 3L, 5L);
        long[] result = stream.filter(n -> n % 2 == 0).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testFilterAllMatch() {
        LongStream stream = LongStream.of(2L, 4L, 6L);
        long[] result = stream.filter(n -> n % 2 == 0).toArray();
        assertArrayEquals(new long[] { 2L, 4L, 6L }, result);
    }

    @Test
    public void testTakeWhile() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.takeWhile(n -> n < 4L).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testTakeWhileEmpty() {
        LongStream stream = LongStream.of(5L, 6L, 7L);
        long[] result = stream.takeWhile(n -> n < 4L).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testDropWhile() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.dropWhile(n -> n < 3L).toArray();
        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
    }

    @Test
    public void testDropWhileAll() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.dropWhile(n -> n < 10L).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testMap() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.map(n -> n * 2).toArray();
        assertArrayEquals(new long[] { 2L, 4L, 6L }, result);
    }

    @Test
    public void testMapIdentity() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.map(n -> n).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testMapToInt() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        int[] result = stream.mapToInt(n -> (int) n).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapToDouble() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        double[] result = stream.mapToDouble(n -> n * 1.5).toArray();
        assertArrayEquals(new double[] { 1.5, 3.0, 4.5 }, result, 0.001);
    }

    @Test
    public void testMapToObj() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        List<String> result = stream.mapToObj(String::valueOf).toList();
        assertEquals(Arrays.asList("1", "2", "3"), result);
    }

    @Test
    public void testMapToObjComplex() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        List<String> result = stream.mapToObj(n -> "Value: " + n).toList();
        assertEquals(Arrays.asList("Value: 1", "Value: 2", "Value: 3"), result);
    }

    @Test
    public void testFlatMap() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.flatMap(n -> LongStream.of(n, n * 10)).toArray();
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L }, result);
    }

    @Test
    public void testFlatMapEmpty() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.flatMap(n -> LongStream.empty()).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testFlatmap() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.flatmap(n -> new long[] { n, n + 10 }).toArray();
        assertArrayEquals(new long[] { 1L, 11L, 2L, 12L, 3L, 13L }, result);
    }

    @Test
    public void testFlatMapToInt() {
        LongStream stream = LongStream.of(1L, 2L);
        int[] result = stream.flatMapToInt(n -> IntStream.of((int) n, (int) (n * 2))).toArray();
        assertArrayEquals(new int[] { 1, 2, 2, 4 }, result);
    }

    @Test
    public void testFlatMapToDouble() {
        LongStream stream = LongStream.of(1L, 2L);
        double[] result = stream.flatMapToDouble(n -> DoubleStream.of(n, n * 1.5)).toArray();
        assertArrayEquals(new double[] { 1.0, 1.5, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testFlatMapToObj() {
        LongStream stream = LongStream.of(1L, 2L);
        List<String> result = stream.flatMapToObj(n -> Stream.of("A" + n, "B" + n)).toList();
        assertEquals(Arrays.asList("A1", "B1", "A2", "B2"), result);
    }

    @Test
    public void testFlatmapToObj() {
        LongStream stream = LongStream.of(1L, 2L);
        List<String> result = stream.flatmapToObj(n -> Arrays.asList("X" + n, "Y" + n)).toList();
        assertEquals(Arrays.asList("X1", "Y1", "X2", "Y2"), result);
    }

    @Test
    public void testFlattmapToObj() {
        LongStream stream = LongStream.of(1L, 2L);
        List<String> result = stream.flattmapToObj(n -> new String[] { "P" + n, "Q" + n }).toList();
        assertEquals(Arrays.asList("P1", "Q1", "P2", "Q2"), result);
    }

    @Test
    public void testDistinct() {
        LongStream stream = LongStream.of(1L, 2L, 2L, 3L, 3L, 3L);
        long[] result = stream.distinct().toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testDistinctEmpty() {
        LongStream stream = LongStream.empty();
        long[] result = stream.distinct().toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testDistinctNoChange() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.distinct().toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testSorted() {
        LongStream stream = LongStream.of(3L, 1L, 4L, 1L, 5L, 9L, 2L, 6L);
        long[] result = stream.sorted().toArray();
        assertArrayEquals(new long[] { 1L, 1L, 2L, 3L, 4L, 5L, 6L, 9L }, result);
    }

    @Test
    public void testSortedAlreadySorted() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L);
        long[] result = stream.sorted().toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testReverseSorted() {
        LongStream stream = LongStream.of(3L, 1L, 4L, 2L);
        long[] result = stream.reverseSorted().toArray();
        assertArrayEquals(new long[] { 4L, 3L, 2L, 1L }, result);
    }

    @Test
    public void testReversed() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L);
        long[] result = stream.reversed().toArray();
        assertArrayEquals(new long[] { 4L, 3L, 2L, 1L }, result);
    }

    @Test
    public void testLimit() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.limit(3).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testLimitZero() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.limit(0).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testLimitMoreThanSize() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.limit(10).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testSkip() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.skip(2).toArray();
        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
    }

    @Test
    public void testSkipZero() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.skip(0).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testSkipAll() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.skip(10).toArray();
        assertArrayEquals(new long[] {}, result);
    }

    @Test
    public void testStep() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
        long[] result = stream.step(2).toArray();
        assertArrayEquals(new long[] { 1L, 3L, 5L, 7L }, result);
    }

    @Test
    public void testPeek() {
        List<Long> peeked = new ArrayList<>();
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.peek(peeked::add).toArray();

        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
        assertEquals(Arrays.asList(1L, 2L, 3L), peeked);
    }

    @Test
    public void testOnEach() {
        AtomicLong sum = new AtomicLong(0);
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.onEach(sum::addAndGet).toArray();

        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
        assertEquals(6L, sum.get());
    }

    @Test
    public void testPrepend() {
        LongStream stream = LongStream.of(3L, 4L, 5L);
        long[] result = stream.prepend(1L, 2L).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testAppend() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.append(4L, 5L).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testAppendIfEmpty() {
        LongStream stream1 = LongStream.empty();
        long[] result1 = stream1.appendIfEmpty(1L, 2L, 3L).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result1);

        LongStream stream2 = LongStream.of(10L, 20L);
        long[] result2 = stream2.appendIfEmpty(1L, 2L, 3L).toArray();
        assertArrayEquals(new long[] { 10L, 20L }, result2);
    }

    @Test
    public void testTop() {
        LongStream stream = LongStream.of(5L, 2L, 8L, 1L, 9L, 3L);
        long[] result = stream.top(3).toArray();
        assertArrayEquals(new long[] { 5L, 8L, 9L }, result);
    }

    @Test
    public void testTopWithComparator() {
        LongStream stream = LongStream.of(5L, 2L, 8L, 1L, 9L, 3L);
        long[] result = stream.top(3, Comparator.naturalOrder()).toArray();
        assertArrayEquals(new long[] { 5L, 8L, 9L }, result);
    }

    @Test
    public void testScan() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L);
        long[] result = stream.scan((a, b) -> a + b).toArray();
        assertArrayEquals(new long[] { 1L, 3L, 6L, 10L }, result);
    }

    @Test
    public void testScanWithInit() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.scan(10L, (a, b) -> a + b).toArray();
        assertArrayEquals(new long[] { 11L, 13L, 16L }, result);
    }

    @Test
    public void testScanWithInitNotIncluded() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.scan(10L, false, (a, b) -> a + b).toArray();
        assertArrayEquals(new long[] { 11L, 13L, 16L }, result);
    }

    @Test
    public void testCollapse() {
        LongStream stream = LongStream.of(1L, 2L, 5L, 6L, 10L);
        Stream<LongList> result = stream.collapse((a, b) -> b - a <= 2);

        List<LongList> lists = result.toList();
        assertEquals(3, lists.size());
        assertArrayEquals(new long[] { 1L, 2L }, lists.get(0).trimToSize().array());
        assertArrayEquals(new long[] { 5L, 6L }, lists.get(1).trimToSize().array());
        assertArrayEquals(new long[] { 10L }, lists.get(2).trimToSize().array());
    }

    @Test
    public void testCollapseWithMerge() {
        LongStream stream = LongStream.of(1L, 2L, 5L, 6L, 10L);
        long[] result = stream.collapse((a, b) -> b - a <= 2, Long::sum).toArray();
        assertArrayEquals(new long[] { 3L, 11L, 10L }, result);
    }

    @Test
    public void testRangeMap() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 10L, 11L, 20L);
        long[] result = stream.rangeMap((first, next) -> next - first <= 2, (first, last) -> first + last).toArray();
        assertArrayEquals(new long[] { 4L, 21L, 40L }, result);
    }

    @Test
    public void testRangeMapToObj() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 10L, 11L);
        List<String> result = stream.rangeMapToObj((first, next) -> next - first <= 2, (first, last) -> first + "-" + last).toList();
        assertEquals(Arrays.asList("1-3", "10-11"), result);
    }

    @Test
    public void testIndexed() {
        LongStream stream = LongStream.of(10L, 20L, 30L);
        List<IndexedLong> result = stream.indexed().toList();

        assertEquals(3, result.size());
        assertEquals(0, result.get(0).index());
        assertEquals(10L, result.get(0).value());
        assertEquals(1, result.get(1).index());
        assertEquals(20L, result.get(1).value());
    }

    @Test
    public void testCycled() {
        LongStream stream = LongStream.of(1L, 2L);
        long[] result = stream.cycled(3).limit(6).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 1L, 2L, 1L, 2L }, result);
    }

    @Test
    public void testRotated() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.rotated(2).toArray();
        assertArrayEquals(new long[] { 4L, 5L, 1L, 2L, 3L }, result);
    }

    @Test
    public void testShuffled() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.shuffled().toArray();

        assertEquals(5, result.length);
        Arrays.sort(result);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testIntersection() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        Collection<Long> set = Arrays.asList(2L, 3L, 6L);
        long[] result = stream.intersection(set).toArray();
        assertArrayEquals(new long[] { 2L, 3L }, result);
    }

    @Test
    public void testDifference() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        Collection<Long> set = Arrays.asList(2L, 3L, 6L);
        long[] result = stream.difference(set).toArray();
        assertArrayEquals(new long[] { 1L, 4L, 5L }, result);
    }

    @Test
    public void testSymmetricDifference() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        Collection<Long> set = Arrays.asList(2L, 3L, 4L);
        long[] result = stream.symmetricDifference(set).toArray();
        assertArrayEquals(new long[] { 1L, 4L }, result);
    }

    @Test
    public void testMergeWith() {
        LongStream s1 = LongStream.of(1L, 3L, 5L);
        LongStream s2 = LongStream.of(2L, 4L, 6L);

        long[] result = s1.mergeWith(s2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, result);
    }

    @Test
    public void testZipWith() {
        LongStream s1 = LongStream.of(1L, 2L, 3L);
        LongStream s2 = LongStream.of(10L, 20L, 30L);

        long[] result = s1.zipWith(s2, (a, b) -> a + b).toArray();
        assertArrayEquals(new long[] { 11L, 22L, 33L }, result);
    }

    @Test
    public void testZipWithThreeStreams() {
        LongStream s1 = LongStream.of(1L, 2L);
        LongStream s2 = LongStream.of(10L, 20L);
        LongStream s3 = LongStream.of(100L, 200L);

        long[] result = s1.zipWith(s2, s3, (a, b, c) -> a + b + c).toArray();
        assertArrayEquals(new long[] { 111L, 222L }, result);
    }

    @Test
    public void testZipWithDefaultValuesForStreams() {
        LongStream s1 = LongStream.of(1L, 2L);
        LongStream s2 = LongStream.of(10L, 20L, 30L);

        long[] result = s1.zipWith(s2, 0L, 0L, (a, b) -> a + b).toArray();
        assertArrayEquals(new long[] { 11L, 22L, 30L }, result);
    }

    @Test
    public void testAsFloatStream() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        float[] result = stream.asFloatStream().toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testAsDoubleStream() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        double[] result = stream.asDoubleStream().toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testBoxed() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        Stream<Long> boxed = stream.boxed();
        assertEquals(Arrays.asList(1L, 2L, 3L), boxed.toList());
    }

    @Test
    public void testToJdkStream() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        java.util.stream.LongStream jdkStream = stream.toJdkStream();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, jdkStream.toArray());
    }

    @Test
    public void testForEach() {
        List<Long> result = new ArrayList<>();
        LongStream stream = LongStream.of(1L, 2L, 3L);
        stream.forEach(result::add);
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testForEachIndexed() {
        List<String> result = new ArrayList<>();
        LongStream stream = LongStream.of(10L, 20L, 30L);
        stream.forEachIndexed((i, v) -> result.add(i + ":" + v));
        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), result);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(LongStream.of(1L, 2L, 3L).anyMatch(n -> n == 2L));
        assertFalse(LongStream.of(1L, 2L, 3L).anyMatch(n -> n == 5L));
        assertFalse(LongStream.empty().anyMatch(n -> true));
    }

    @Test
    public void testAllMatch() {
        assertTrue(LongStream.of(2L, 4L, 6L).allMatch(n -> n % 2 == 0));
        assertFalse(LongStream.of(1L, 2L, 3L).allMatch(n -> n % 2 == 0));
        assertTrue(LongStream.empty().allMatch(n -> false));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(LongStream.of(1L, 3L, 5L).noneMatch(n -> n % 2 == 0));
        assertFalse(LongStream.of(1L, 2L, 3L).noneMatch(n -> n % 2 == 0));
        assertTrue(LongStream.empty().noneMatch(n -> true));
    }

    @Test
    public void testFindFirst() {
        OptionalLong result = LongStream.of(1L, 2L, 3L).findFirst();
        assertTrue(result.isPresent());
        assertEquals(1L, result.get());

        assertFalse(LongStream.empty().findFirst().isPresent());
    }

    @Test
    public void testFindFirstWithPredicate() {
        OptionalLong result = LongStream.of(1L, 2L, 3L, 4L).findFirst(n -> n > 2L);
        assertTrue(result.isPresent());
        assertEquals(3L, result.get());
    }

    @Test
    public void testFindAny() {
        OptionalLong result = LongStream.of(1L, 2L, 3L).findAny();
        assertTrue(result.isPresent());
        assertTrue(result.get() >= 1L && result.get() <= 3L);
    }

    @Test
    public void testFindAnyWithPredicate() {
        OptionalLong result = LongStream.of(1L, 2L, 3L, 4L).findAny(n -> n % 2 == 0);
        assertTrue(result.isPresent());
        assertTrue(result.get() == 2L || result.get() == 4L);
    }

    @Test
    public void testFindLast() {
        OptionalLong result = LongStream.of(1L, 2L, 3L).last();
        assertTrue(result.isPresent());
        assertEquals(3L, result.get());
    }

    @Test
    public void testFindLastWithPredicate() {
        OptionalLong result = LongStream.of(1L, 2L, 3L, 4L).findLast(n -> n % 2 == 0);
        assertTrue(result.isPresent());
        assertEquals(4L, result.get());
    }

    @Test
    public void testMin() {
        OptionalLong result = LongStream.of(3L, 1L, 4L, 1L, 5L).min();
        assertTrue(result.isPresent());
        assertEquals(1L, result.get());

        assertFalse(LongStream.empty().min().isPresent());
    }

    @Test
    public void testMax() {
        OptionalLong result = LongStream.of(3L, 1L, 4L, 1L, 5L).max();
        assertTrue(result.isPresent());
        assertEquals(5L, result.get());

        assertFalse(LongStream.empty().max().isPresent());
    }

    @Test
    public void testKthLargest() {
        OptionalLong result = LongStream.of(5L, 2L, 8L, 1L, 9L, 3L).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(8L, result.get());
    }

    @Test
    public void testKthLargestOutOfBounds() {
        OptionalLong result = LongStream.of(1L, 2L, 3L).kthLargest(5);
        assertFalse(result.isPresent());
    }

    @Test
    public void testSum() {
        long result = LongStream.of(1L, 2L, 3L, 4L, 5L).sum();
        assertEquals(15L, result);

        assertEquals(0L, LongStream.empty().sum());
    }

    @Test
    public void testAverage() {
        OptionalDouble result = LongStream.of(1L, 2L, 3L, 4L, 5L).average();
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.001);

        assertFalse(LongStream.empty().average().isPresent());
    }

    @Test
    public void testSummarize() {
        LongSummaryStatistics stats = LongStream.of(1L, 2L, 3L, 4L, 5L).summarize();
        assertEquals(5, stats.getCount());
        assertEquals(1L, stats.getMin());
        assertEquals(5L, stats.getMax());
        assertEquals(15L, stats.getSum());
        assertEquals(3.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testSummarizeEmpty() {
        LongSummaryStatistics stats = LongStream.empty().summarize();
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testSummarizeAndPercentiles() {
        Pair<LongSummaryStatistics, Optional<Map<Percentage, Long>>> result = LongStream.of(1L, 2L, 3L, 4L, 5L).summarizeAndPercentiles();

        assertNotNull(result);
        assertEquals(5, result.left().getCount());
        assertTrue(result.right().isPresent());
    }

    @Test
    public void testCount() {
        assertEquals(5, LongStream.of(1L, 2L, 3L, 4L, 5L).count());
        assertEquals(0, LongStream.empty().count());
    }

    @Test
    public void testReduce() {
        long result = LongStream.of(1L, 2L, 3L, 4L).reduce(0L, Long::sum);
        assertEquals(10L, result);
    }

    @Test
    public void testReduceWithoutIdentity() {
        OptionalLong result = LongStream.of(1L, 2L, 3L, 4L).reduce(Long::sum);
        assertTrue(result.isPresent());
        assertEquals(10L, result.get());

        assertFalse(LongStream.empty().reduce(Long::sum).isPresent());
    }

    @Test
    public void testReduceMultiplication() {
        long result = LongStream.of(2L, 3L, 4L).reduce(1L, (a, b) -> a * b);
        assertEquals(24L, result);
    }

    @Test
    public void testCollect() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        List<Long> result = stream.collect(ArrayList::new, List::add, List::addAll);
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testCollectWithoutCombiner() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        List<Long> result = stream.collect(ArrayList::new, List::add);
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testToArray() {
        long[] result = LongStream.of(1L, 2L, 3L).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testToList() {
        List<Long> result = LongStream.of(1L, 2L, 3L).toList();
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testToLongList() {
        LongList result = LongStream.of(1L, 2L, 3L).toLongList();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result.trimToSize().array());
    }

    @Test
    public void testToSet() {
        Set<Long> result = LongStream.of(1L, 2L, 2L, 3L).toSet();
        assertEquals(new HashSet<>(Arrays.asList(1L, 2L, 3L)), result);
    }

    @Test
    public void testToImmutableList() {
        List<Long> result = LongStream.of(1L, 2L, 3L).toImmutableList();
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add(4L));
    }

    @Test
    public void testToImmutableSet() {
        Set<Long> result = LongStream.of(1L, 2L, 3L).toImmutableSet();
        assertEquals(3, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.add(4L));
    }

    @Test
    public void testToMultiset() {
        Multiset<Long> result = LongStream.of(1L, 2L, 2L, 3L, 3L, 3L).toMultiset();
        assertEquals(1, result.get(1L));
        assertEquals(2, result.get(2L));
        assertEquals(3, result.get(3L));
    }

    @Test
    public void testJoin() {
        String result = LongStream.of(1L, 2L, 3L).join(", ");
        assertEquals("1, 2, 3", result);
    }

    @Test
    public void testJoinWithPrefixSuffix() {
        String result = LongStream.of(1L, 2L, 3L).join(", ", "[", "]");
        assertEquals("[1, 2, 3]", result);
    }

    @Test
    public void testJoinEmpty() {
        String result = LongStream.empty().join(", ");
        assertEquals("", result);
    }

    @Test
    public void testFirst() {
        OptionalLong result = LongStream.of(1L, 2L, 3L).first();
        assertTrue(result.isPresent());
        assertEquals(1L, result.get());
    }

    @Test
    public void testLast() {
        OptionalLong result = LongStream.of(1L, 2L, 3L).last();
        assertTrue(result.isPresent());
        assertEquals(3L, result.get());
    }

    @Test
    public void testOnlyOne() {
        assertEquals(42L, LongStream.of(42L).onlyOne().get());
        assertThrows(NoSuchElementException.class, () -> LongStream.empty().onlyOne().get());
        assertThrows(TooManyElementsException.class, () -> LongStream.of(1L, 2L).onlyOne().get());
    }

    @Test
    public void testElementAt() {
        assertEquals(20L, LongStream.of(10L, 20L, 30L).elementAt(1).get());
        assertTrue(LongStream.of(1L, 2L).elementAt(5).isEmpty());
    }

    @Test
    public void testPercentiles() {
        Optional<Map<Percentage, Long>> result = LongStream.of(1L, 2L, 3L, 4L, 5L).percentiles();
        assertTrue(result.isPresent());
        assertNotNull(result.get());
    }

    @Test
    public void testIsParallel() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        assertFalse(stream.isParallel());
    }

    @Test
    public void testParallel() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        LongStream parallel = stream.parallel();
        assertTrue(parallel.isParallel());
    }

    @Test
    public void testSequential() {
        LongStream stream = LongStream.of(1L, 2L, 3L).parallel();
        assertTrue(stream.isParallel());

        LongStream sequential = stream.sequential();
        assertFalse(sequential.isParallel());
    }

    @Test
    public void testParallelWithMaxThreadNum() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        LongStream parallel = stream.parallel(2);
        assertTrue(parallel.isParallel());
        assertEquals(15L, parallel.sum());
    }

    @Test
    public void testNullHandlingInFilter() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        assertEquals(3, stream.filter(n -> n != 0).count());
    }

    @Test
    public void testLargeStream() {
        LongStream stream = LongStream.range(0L, 10000L);
        assertEquals(10000, stream.count());
    }

    @Test
    public void testLargeStreamSum() {
        long sum = LongStream.range(1L, 101L).sum();
        assertEquals(5050L, sum);
    }

    @Test
    public void testChainedOperations() {
        long result = LongStream.range(1L, 101L).filter(n -> n % 2 == 0).map(n -> n * 2).limit(10).sum();

        assertEquals(220L, result);
    }

    @Test
    public void testMultipleTerminalOpsThrowsException() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        stream.sum();

        assertThrows(IllegalStateException.class, () -> stream.count());
    }

    @Test
    public void testThrowIfEmpty() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        assertNotNull(stream.throwIfEmpty());

        assertThrows(NoSuchElementException.class, () -> LongStream.empty().throwIfEmpty().count());
    }

    @Test
    public void testThrowIfEmptyWithCustomException() {
        assertThrows(IllegalStateException.class, () -> LongStream.empty().throwIfEmpty(() -> new IllegalStateException("Stream is empty")).count());
    }

    @Test
    public void testIterator() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        LongIterator iter = stream.iterator();

        assertTrue(iter.hasNext());
        assertEquals(1L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(2L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(3L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testCloseHandler() {
        AtomicBoolean closed = new AtomicBoolean(false);
        LongStream stream = LongStream.of(1L, 2L, 3L);
        stream.onClose(() -> closed.set(true));

        stream.sum();
        stream.close();

        assertTrue(closed.get());
    }

    @Test
    public void testMultipleCloseHandlers() {
        AtomicInteger closeCount = new AtomicInteger(0);
        LongStream stream = LongStream.of(1L, 2L, 3L);
        stream.onClose(closeCount::incrementAndGet);
        stream.onClose(closeCount::incrementAndGet);

        stream.sum();
        stream.close();

        assertEquals(2, closeCount.get());
    }

    @Test
    public void testToMap() {
        Map<Long, String> map = LongStream.of(1L, 2L, 3L).toMap(n -> n, n -> "Value" + n);

        assertEquals(3, map.size());
        assertEquals("Value1", map.get(1L));
        assertEquals("Value2", map.get(2L));
        assertEquals("Value3", map.get(3L));
    }

    @Test
    public void testGroupTo() {
        Map<Boolean, List<Long>> map = LongStream.of(1L, 2L, 3L, 4L, 5L).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList());

        assertEquals(2, map.size());
        assertTrue(map.containsKey(true));
        assertTrue(map.containsKey(false));
    }

    @Test
    public void testMapMulti() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.mapMulti((value, consumer) -> {
            consumer.accept(value);
            consumer.accept(value * 10);
        }).toArray();
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L }, result);
    }

    @Test
    public void testMapPartial() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.mapPartial(n -> n % 2 == 0 ? OptionalLong.of(n * 2) : OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 4L, 8L }, result);
    }

    @Test
    public void testMapPartialJdk() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.mapPartialJdk(n -> n > 3L ? java.util.OptionalLong.of(n) : java.util.OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 4L, 5L }, result);
    }

    @Test
    public void testSkipUntil() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.skipUntil(n -> n >= 3L).toArray();
        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
    }

    @Test
    public void testFilterWithAction() {
        List<Long> dropped = new ArrayList<>();
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.filter(n -> n % 2 == 0, dropped::add).toArray();

        assertArrayEquals(new long[] { 2L, 4L }, result);
        assertEquals(Arrays.asList(1L, 3L, 5L), dropped);
    }

    @Test
    public void testSkipWithAction() {
        List<Long> skipped = new ArrayList<>();
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.skip(2, skipped::add).toArray();

        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
        assertEquals(Arrays.asList(1L, 2L), skipped);
    }

    @Test
    public void testDropWhileWithAction() {
        List<Long> dropped = new ArrayList<>();
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.dropWhile(n -> n < 3L, dropped::add).toArray();

        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
        assertEquals(Arrays.asList(1L, 2L), dropped);
    }

    @Test
    public void testPrependStream() {
        LongStream s1 = LongStream.of(1L, 2L);
        LongStream s2 = LongStream.of(3L, 4L);
        long[] result = s2.prepend(s1).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testAppendStream() {
        LongStream s1 = LongStream.of(1L, 2L);
        LongStream s2 = LongStream.of(3L, 4L);
        long[] result = s1.append(s2).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testIfEmpty() {
        AtomicBoolean called = new AtomicBoolean(false);
        LongStream.empty().ifEmpty(() -> called.set(true)).count();
        assertTrue(called.get());

        called.set(false);
        LongStream.of(1L).ifEmpty(() -> called.set(true)).count();
        assertFalse(called.get());
    }

    @Test
    public void testPrintln() {
        LongStream.of(1L, 2L, 3L).println();
    }

    @Test
    public void testMapToFloat() {
        float[] result = LongStream.of(1L, 2L, 3L).mapToFloat(n -> n * 1.5f).toArray();
        assertEquals(3, result.length);
        assertEquals(1.5f, result[0], 0.001f);
        assertEquals(3.0f, result[1], 0.001f);
        assertEquals(4.5f, result[2], 0.001f);
    }

    @Test
    public void testFlatMapToFloat() {
        float[] result = LongStream.of(1L, 2L).flatMapToFloat(n -> FloatStream.of(n, n * 2)).toArray();
        assertEquals(4, result.length);
        assertEquals(1.0f, result[0], 0.001f);
        assertEquals(2.0f, result[1], 0.001f);
        assertEquals(2.0f, result[2], 0.001f);
        assertEquals(4.0f, result[3], 0.001f);
    }

    @Test
    public void testInterval_twoArgs() {
        LongStream stream = LongStream.interval(10, 50);
        long[] first5 = stream.limit(5).toArray();
        assertEquals(5, first5.length);
    }

    @Test
    public void testInterval_threeArgs() {
        LongStream stream = LongStream.interval(10, 50, java.util.concurrent.TimeUnit.MILLISECONDS);
        long[] first3 = stream.limit(3).toArray();
        assertEquals(3, first3.length);
    }

    @Test
    public void testTransformB_singleArg() {
        long[] result = LongStream.of(1L, 2L, 3L, 4L).transformB(s -> s.map(x -> x * 2).sorted()).toArray();
        assertArrayEquals(new long[] { 2L, 4L, 6L, 8L }, result);
    }

    @Test
    public void testTransformB_withDeferred() {
        long[] result = LongStream.of(1L, 2L, 3L, 4L).transformB(s -> s.map(x -> x * 2).sorted(), false).toArray();
        assertArrayEquals(new long[] { 2L, 4L, 6L, 8L }, result);
    }

    @Test
    public void testTransform() {
        long[] result = LongStream.of(1L, 2L, 3L).transform(s -> s.map(x -> x + 10)).toArray();
        assertArrayEquals(new long[] { 11L, 12L, 13L }, result);
    }

    @Test
    public void testDelay_singleArg() {
        long startTime = System.currentTimeMillis();
        long[] result = LongStream.of(1L, 2L).delay(Duration.ofMillis(50)).toArray();
        long endTime = System.currentTimeMillis();
        assertArrayEquals(new long[] { 1L, 2L }, result);
        assertTrue(endTime - startTime >= 40);
    }

    @Test
    public void testDelay_withTimeUnit() {
        long[] result = LongStream.of(1L, 2L).delay(Duration.ofMillis(50)).toArray();
        assertArrayEquals(new long[] { 1L, 2L }, result);
    }

    @Test
    public void testRateLimited_perSecond() {
        long[] result = LongStream.of(1L, 2L, 3L).rateLimited(100).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testRateLimited_withTimeUnit() {
        long[] result = LongStream.of(1L, 2L, 3L).rateLimited(3).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testAcceptIfNotEmpty() {
        final long[] sum = { 0 };
        LongStream.of(1L, 2L, 3L).acceptIfNotEmpty(list -> {
            sum[0] = list.sum();
        });
        assertEquals(6L, sum[0]);

        sum[0] = 0;
        LongStream.empty().acceptIfNotEmpty(list -> {
            sum[0] = 999L;
        });
        assertEquals(0L, sum[0]);
    }

    @Test
    public void testApplyIfNotEmpty() {
        Long result = LongStream.of(1L, 2L, 3L).applyIfNotEmpty(list -> list.sum()).orElse(0L);
        assertNotNull(result);
        assertEquals(6L, result.longValue());

        Long emptyResult = LongStream.empty().applyIfNotEmpty(list -> 999L).orElseNull();
        assertNull(emptyResult);
    }

    @Test
    public void testSps() {
        LongStream.of(1L, 2L, 3L).sps(s -> s.map(x -> x * 2)).sps(s -> s.filter(x -> x > 2)).forEach(n -> assertTrue(n > 2 && n % 2 == 0));
    }

    @Test
    public void testOnClose_singleHandler() {
        final boolean[] closed = { false };
        LongStream stream = LongStream.of(1L, 2L, 3L).onClose(() -> closed[0] = true);
        stream.toArray();
        assertTrue(closed[0]);
    }

    @Test
    public void testOnClose_multipleHandlers() {
        final int[] closeCount = { 0 };
        LongStream stream = LongStream.of(1L, 2L, 3L).onClose(() -> closeCount[0]++).onClose(() -> closeCount[0]++);
        stream.toArray();
        assertEquals(2, closeCount[0]);
    }

    // Additional tests to improve coverage from 83.3% to 90%+

    @Test
    public void testZipWithCollectionOfStreams() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1L, 2L), LongStream.of(10L, 20L), LongStream.of(100L, 200L));
        LongStream result = LongStream.zip(streams, longs -> {
            long sum = 0;
            for (Long l : longs)
                sum += l;
            return sum;
        });
        assertArrayEquals(new long[] { 111L, 222L }, result.toArray());
    }

    @Test
    public void testZipWithCollectionAndValuesForNone() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1L, 2L, 3L), LongStream.of(10L), LongStream.of(100L, 200L));
        long[] defaults = { 0L, 0L, 0L };
        LongStream result = LongStream.zip(streams, defaults, longs -> {
            long sum = 0;
            for (Long l : longs)
                sum += l;
            return sum;
        });
        assertArrayEquals(new long[] { 111L, 202L, 3L }, result.toArray());
    }

    @Test
    public void testMergeWithCollectionOfStreams() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1L, 5L), LongStream.of(2L, 6L), LongStream.of(3L, 7L));
        LongStream result = LongStream.merge(streams, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 5L, 6L, 7L }, result.toArray());
    }

    @Test
    public void testMergeThreeArrays() {
        long[] a = { 1L, 4L, 7L };
        long[] b = { 2L, 5L, 8L };
        long[] c = { 3L, 6L, 9L };

        LongStream result = LongStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L }, result.toArray());
    }

    @Test
    public void testMergeThreeStreams() {
        LongStream s1 = LongStream.of(1L, 4L, 7L);
        LongStream s2 = LongStream.of(2L, 5L, 8L);
        LongStream s3 = LongStream.of(3L, 6L, 9L);

        LongStream result = LongStream.merge(s1, s2, s3, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L }, result.toArray());
    }

    @Test
    public void testFlattenWithAlignmentHorizontally() {
        long[][] array = { { 1L, 2L }, { 3L }, { 4L, 5L, 6L } };
        long valueForAlignment = 0L;

        LongStream stream = LongStream.flatten(array, valueForAlignment, false);
        assertArrayEquals(new long[] { 1L, 2L, 0L, 3L, 0L, 0L, 4L, 5L, 6L }, stream.toArray());
    }

    @Test
    public void testRangeClosedWithNegativeStep() {
        LongStream stream = LongStream.rangeClosed(5L, 1L, -1L);
        assertArrayEquals(new long[] { 5L, 4L, 3L, 2L, 1L }, stream.toArray());
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
    public void testParallelStreamOperations() {
        long sum = LongStream.range(1L, 100L).parallel().filter(n -> n % 2 == 0).map(n -> n * 2).sum();

        assertTrue(sum > 0);

        // Verify parallel flag is maintained through operations
        LongStream parallelStream = LongStream.of(1L, 2L, 3L).parallel().filter(n -> n > 0).map(n -> n * 2);
        assertTrue(parallelStream.isParallel());
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
    public void testCollapseWithNoCollapsibleElements() {
        LongStream stream = LongStream.of(1L, 10L, 20L, 30L).collapse((prev, curr) -> curr - prev <= 2, (a, b) -> a + b);
        assertArrayEquals(new long[] { 1L, 10L, 20L, 30L }, stream.toArray());
    }

    @Test
    public void testScanOnEmptyStream() {
        LongStream stream1 = LongStream.empty().scan((a, b) -> a + b);
        assertEquals(0, stream1.count());

        LongStream stream2 = LongStream.empty().scan(10L, (a, b) -> a + b);
        assertEquals(0, stream2.count());

        LongStream stream3 = LongStream.empty().scan(10L, true, (a, b) -> a + b);
        assertArrayEquals(new long[] { 10L }, stream3.toArray());
    }

    @Test
    public void testZipThreeArraysWithDefaults() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 10L, 20L };
        long[] c = { 100L };

        LongStream stream = LongStream.zip(a, b, c, 0L, 0L, 0L, (x, y, z) -> x + y + z);
        assertArrayEquals(new long[] { 111L, 22L, 3L, 4L }, stream.toArray());
    }

    @Test
    public void testZipThreeIteratorsWithDefaults() {
        LongIterator it1 = LongIterator.of(new long[] { 1L, 2L, 3L });
        LongIterator it2 = LongIterator.of(new long[] { 10L });
        LongIterator it3 = LongIterator.of(new long[] { 100L, 200L });

        LongStream stream = LongStream.zip(it1, it2, it3, 0L, 0L, 0L, (x, y, z) -> x + y + z);
        assertArrayEquals(new long[] { 111L, 202L, 3L }, stream.toArray());
    }

    @Test
    public void testZipThreeStreamsWithDefaults() {
        LongStream s1 = LongStream.of(1L, 2L);
        LongStream s2 = LongStream.of(10L, 20L, 30L);
        LongStream s3 = LongStream.of(100L);

        LongStream stream = s1.zipWith(s2, s3, 0L, 0L, 0L, (x, y, z) -> x + y + z);
        assertArrayEquals(new long[] { 111L, 22, 30L }, stream.toArray());
    }

    // ==================== debounce tests ====================

    @Test
    public void testDebounce_BasicFunctionality() {
        // Allow 3 elements per 1 second window
        long[] result = LongStream.of(1L, 2L, 3L, 4L, 5L)
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        // Only first 3 elements should pass through within the window
        assertEquals(3, result.length);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testDebounce_AllElementsPassWhenWithinLimit() {
        // Allow 10 elements per window, but only 5 elements in stream
        long[] result = LongStream.of(1L, 2L, 3L, 4L, 5L)
                .debounce(10, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        // All elements should pass
        assertEquals(5, result.length);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testDebounce_EmptyStream() {
        long[] result = LongStream.empty()
                .debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals(0, result.length);
    }

    @Test
    public void testDebounce_SingleElement() {
        long[] result = LongStream.of(42L)
                .debounce(1, com.landawn.abacus.util.Duration.ofMillis(100))
                .toArray();

        assertEquals(1, result.length);
        assertEquals(42L, result[0]);
    }

    @Test
    public void testDebounce_MaxWindowSizeOne() {
        // Only 1 element allowed per window
        long[] result = LongStream.of(1L, 2L, 3L, 4L, 5L)
                .debounce(1, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals(1, result.length);
        assertEquals(1L, result[0]);
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveMaxWindowSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            LongStream.of(1L, 2L, 3L).debounce(0, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            LongStream.of(1L, 2L, 3L).debounce(-1, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();
        });
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveDuration() {
        assertThrows(IllegalArgumentException.class, () -> {
            LongStream.of(1L, 2L, 3L).debounce(5, com.landawn.abacus.util.Duration.ofMillis(0)).toArray();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            LongStream.of(1L, 2L, 3L).debounce(5, com.landawn.abacus.util.Duration.ofMillis(-100)).toArray();
        });
    }

    @Test
    public void testDebounce_WithLargeMaxWindowSize() {
        long[] input = new long[1000];
        for (int i = 0; i < 1000; i++) {
            input[i] = i;
        }

        long[] result = LongStream.of(input)
                .debounce(500, com.landawn.abacus.util.Duration.ofSeconds(10))
                .toArray();

        assertEquals(500, result.length);
    }

    @Test
    public void testDebounce_PreservesOrder() {
        long[] result = LongStream.of(10L, 20L, 30L, 40L, 50L)
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertArrayEquals(new long[] { 10L, 20L, 30L }, result);
    }

    @Test
    public void testDebounce_ChainedWithOtherOperations() {
        long[] result = LongStream.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)
                .filter(n -> n % 2 == 0)  // 2, 4, 6, 8, 10
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))  // 2, 4, 6
                .map(n -> n * 10)  // 20, 40, 60
                .toArray();

        assertEquals(3, result.length);
        assertArrayEquals(new long[] { 20L, 40L, 60L }, result);
    }

    @Test
    public void testDebounce_WithRange() {
        long[] result = LongStream.range(0, 100)
                .debounce(10, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals(10, result.length);
        for (int i = 0; i < 10; i++) {
            assertEquals(i, result[i]);
        }
    }
}
