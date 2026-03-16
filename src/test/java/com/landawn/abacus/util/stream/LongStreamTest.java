package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.IndexedLong;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongNFunction;
import com.landawn.abacus.util.function.LongSupplier;

@Tag("2025")
public class LongStreamTest extends TestBase {

    protected LongStream createLongStream(long... a) {
        return LongStream.of(a);
    }

    protected LongStream createLongStream(long[] a, int fromIndex, int toIndex) {
        return LongStream.of(a, fromIndex, toIndex);
    }

    protected LongStream createLongStream(Long[] a) {
        return LongStream.of(a);
    }

    protected LongStream createLongStream(Long[] a, int fromIndex, int toIndex) {
        return LongStream.of(a, fromIndex, toIndex);
    }

    protected LongStream createLongStream(Collection<Long> coll) {
        return LongStream.of(coll.toArray(new Long[coll.size()]));
    }

    protected LongStream createLongStream(LongIterator iter) {
        return iter == null ? LongStream.empty() : LongStream.of(iter.toArray());
    }

    protected LongStream createLongStream(OptionalLong op) {
        return LongStream.of(op);
    }

    protected LongStream createLongStream(LongBuffer buff) {
        return LongStream.of(buff);
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

    //    @Test
    //    public void testOfJavaStream() {
    //        java.util.stream.LongStream javaStream = java.util.stream.LongStream.of(10L, 20L, 30L);
    //        LongStream stream = LongStream.of(javaStream);
    //        assertArrayEquals(new long[] { 10L, 20L, 30L }, stream.toArray());
    //    }

    //

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
        List<String> result = stream.flatMapArrayToObj(n -> new String[] { "P" + n, "Q" + n }).toList();
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
        long[] result = stream.peek(sum::addAndGet).toArray();

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
        assertArrayEquals(new long[] { 1L, 2L }, lists.get(0).trimToSize().internalArray());
        assertArrayEquals(new long[] { 5L, 6L }, lists.get(1).trimToSize().internalArray());
        assertArrayEquals(new long[] { 10L }, lists.get(2).trimToSize().internalArray());
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
    public void testsummaryStatistics() {
        LongSummaryStatistics stats = LongStream.of(1L, 2L, 3L, 4L, 5L).summaryStatistics();
        assertEquals(5, stats.getCount());
        assertEquals(1L, stats.getMin());
        assertEquals(5L, stats.getMax());
        assertEquals(15L, stats.getSum());
        assertEquals(3.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testSummarizeEmpty() {
        LongSummaryStatistics stats = LongStream.empty().summaryStatistics();
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        Pair<LongSummaryStatistics, Optional<Map<Percentage, Long>>> result = LongStream.of(1L, 2L, 3L, 4L, 5L).summaryStatisticsAndPercentiles();

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
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result.trimToSize().internalArray());
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
        assertDoesNotThrow(() -> {
            LongStream.of(1L, 2L, 3L).println();
        });
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
        long[] result = LongStream.of(1L, 2L, 3L, 4L, 5L).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        // Only first 3 elements should pass through within the window
        assertEquals(3, result.length);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testDebounce_AllElementsPassWhenWithinLimit() {
        // Allow 10 elements per window, but only 5 elements in stream
        long[] result = LongStream.of(1L, 2L, 3L, 4L, 5L).debounce(10, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        // All elements should pass
        assertEquals(5, result.length);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testDebounce_EmptyStream() {
        long[] result = LongStream.empty().debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertEquals(0, result.length);
    }

    @Test
    public void testDebounce_SingleElement() {
        long[] result = LongStream.of(42L).debounce(1, com.landawn.abacus.util.Duration.ofMillis(100)).toArray();

        assertEquals(1, result.length);
        assertEquals(42L, result[0]);
    }

    @Test
    public void testDebounce_MaxWindowSizeOne() {
        // Only 1 element allowed per window
        long[] result = LongStream.of(1L, 2L, 3L, 4L, 5L).debounce(1, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

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

        long[] result = LongStream.of(input).debounce(500, com.landawn.abacus.util.Duration.ofSeconds(10)).toArray();

        assertEquals(500, result.length);
    }

    @Test
    public void testDebounce_PreservesOrder() {
        long[] result = LongStream.of(10L, 20L, 30L, 40L, 50L).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertArrayEquals(new long[] { 10L, 20L, 30L }, result);
    }

    @Test
    public void testDebounce_ChainedWithOtherOperations() {
        long[] result = LongStream.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)
                .filter(n -> n % 2 == 0) // 2, 4, 6, 8, 10
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)) // 2, 4, 6
                .map(n -> n * 10) // 20, 40, 60
                .toArray();

        assertEquals(3, result.length);
        assertArrayEquals(new long[] { 20L, 40L, 60L }, result);
    }

    @Test
    public void testDebounce_WithRange() {
        long[] result = LongStream.range(0, 100).debounce(10, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertEquals(10, result.length);
        for (int i = 0; i < 10; i++) {
            assertEquals(i, result[i]);
        }
    }

    @Test
    public void testOfEmpty() {
        assertEquals(0, LongStream.of().count());
    }

    @Test
    public void testOfMultipleElements() {
        assertArrayEquals(new long[] { 1L, 2L, 3L }, LongStream.of(1L, 2L, 3L).toArray());
    }

    @Test
    public void testOfArray() {
        long[] arr = { 1, 2, 3 };
        assertArrayEquals(arr, LongStream.of(arr).toArray());
    }

    @Test
    public void testConcatWithEmptyStream() {
        LongStream s1 = LongStream.of(1, 2);
        LongStream s2 = LongStream.empty();
        assertArrayEquals(new long[] { 1, 2 }, LongStream.concat(s1, s2).toArray());
    }

    @Test
    public void testZipWithDifferentLength() {
        long[] a = { 1, 2 };
        long[] b = { 4, 5, 6 };
        assertArrayEquals(new long[] { 5, 7 }, LongStream.zip(a, b, (x, y) -> x + y).toArray());
    }

    @Test
    public void testFlatten() {
        long[][] twoDimArray = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, LongStream.flatten(twoDimArray).toArray());
    }

    @Test
    public void testFromJdkStream() {
        java.util.stream.LongStream jdkStream = java.util.stream.LongStream.of(1, 2, 3);
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStream.from(jdkStream).toArray());
    }

    @Test
    public void testScanWithInitialValue() {
        assertArrayEquals(new long[] { 10, 11, 13, 16, 20 }, LongStream.of(1, 2, 3, 4).scan(10, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 11, 13, 16, 20 }, LongStream.of(1, 2, 3, 4).scan(10, (a, b) -> a + b).toArray());
    }

    @Test
    public void testReduceWithIdentity() {
        assertEquals(10, LongStream.of(1, 2, 3, 4).reduce(0, (a, b) -> a + b));
        assertEquals(0, LongStream.empty().reduce(0, (a, b) -> a + b));
    }

    @Test
    public void testParallelMapSum() {
        long expectedSum = LongStream.range(0, 100).map(i -> i * 2).sum();
        long actualSum = LongStream.range(0, 100).parallel().map(i -> i * 2).sum();
        assertEquals(expectedSum, actualSum);
    }

    @Test
    public void testOnClose() {
        AtomicLong counter = new AtomicLong(0);
        try (LongStream stream = LongStream.of(1, 2, 3).onClose(counter::incrementAndGet)) {
            stream.count();
        }
        assertEquals(1, counter.get());
    }

    @Test
    public void testExplicitClose() {
        AtomicLong counter = new AtomicLong(0);
        LongStream stream = LongStream.of(1, 2, 3).onClose(counter::incrementAndGet);
        stream.close();
        assertEquals(1, counter.get());
        stream.close();
        assertEquals(1, counter.get());
    }

    @Test
    public void testIteratorDoesNotCloseStream() {
        AtomicLong counter = new AtomicLong(0);
        LongStream stream = LongStream.of(1, 2, 3).onClose(counter::incrementAndGet);
        LongIterator iter = stream.iterator();
        while (iter.hasNext()) {
            iter.nextLong();
        }
        assertEquals(0, counter.get());
        stream.close();
        assertEquals(1, counter.get());
    }

    @Test
    public void testInvalidSkip() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.of(1, 2, 3).skip(-1));
    }

    @Test
    public void testInvalidLimit() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.of(1, 2, 3).limit(-1));
    }

    @Test
    public void testInvalidStep() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.of(1, 2, 3).step(0));
        assertThrows(IllegalArgumentException.class, () -> LongStream.of(1, 2, 3).step(-1));
    }

    @Test
    public void testNextOnExhaustedIterator() {
        LongStream s = LongStream.of(1);
        LongIterator it = s.iterator();
        it.nextLong();
        assertThrows(NoSuchElementException.class, it::nextLong);
    }

    @Test
    public void testCycledWithRounds() {
        assertArrayEquals(new long[] { 1, 2, 3, 1, 2, 3 }, LongStream.of(1, 2, 3).cycled(2).toArray());
        assertEquals(0, LongStream.of(1, 2, 3).cycled(0).count());
    }

    @Test
    public void testFilterWithActionOnDropped() {
        LongList dropped = new LongList();
        long[] result = LongStream.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0, dropped::add).toArray();
        assertArrayEquals(new long[] { 2, 4 }, result);
        assertArrayEquals(new long[] { 1, 3, 5 }, dropped.toArray());
    }

    @Test
    public void testToMapWithMerge() {
        Map<Long, Long> map = LongStream.of(1, 2, 1, 3, 2).toMap(l -> l, l -> 1L, Long::sum);
        assertEquals(2, map.get(1L));
        assertEquals(2, map.get(2L));
        assertEquals(1, map.get(3L));
    }

    @Test
    public void testPsp() {
        long[] result = LongStream.range(1, 100).parallel().psp(s -> s.filter(i -> i > 10).limit(5)).map(i -> i * 10).sorted().toArray();

        assertArrayEquals(new long[] { 110, 120, 130, 140, 150 }, result);
    }

    @Test
    public void testShuffledWithRandom() {
        Random rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).skip(1).count());
        rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).toArray().length);
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).skip(1).toArray().length);
        rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).toList().size());
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).skip(1).toList().size());
        rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).skip(1).count());
        rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).toArray().length);
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).skip(1).toArray().length);
        rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).toList().size());
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).skip(1).toList().size());
    }

    @Test
    public void testFlatmapArray() {
        assertEquals(9, LongStream.of(1, 2, 3).flatmap(e -> new long[] { e, e + 10, e + 20 }).count());
        assertEquals(8, LongStream.of(1, 2, 3).flatmap(e -> new long[] { e, e + 10, e + 20 }).skip(1).count());
        assertArrayEquals(new long[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 }, LongStream.of(1, 2, 3).flatmap(e -> new long[] { e, e + 10, e + 20 }).toArray());
        assertArrayEquals(new long[] { 11, 21, 2, 12, 22, 3, 13, 23 }, LongStream.of(1, 2, 3).flatmap(e -> new long[] { e, e + 10, e + 20 }).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L), LongStream.of(1, 2, 3).flatmap(e -> new long[] { e, e + 10, e + 20 }).toList());
        assertEquals(Arrays.asList(11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).flatmap(e -> new long[] { e, e + 10, e + 20 }).skip(1).toList());
        assertEquals(9, LongStream.of(1, 2, 3).map(e -> e).flatmap(e -> new long[] { e, e + 10, e + 20 }).count());
        assertEquals(8, LongStream.of(1, 2, 3).map(e -> e).flatmap(e -> new long[] { e, e + 10, e + 20 }).skip(1).count());
        assertArrayEquals(new long[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flatmap(e -> new long[] { e, e + 10, e + 20 }).toArray());
        assertArrayEquals(new long[] { 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flatmap(e -> new long[] { e, e + 10, e + 20 }).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).map(e -> e).flatmap(e -> new long[] { e, e + 10, e + 20 }).toList());
        assertEquals(Arrays.asList(11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).map(e -> e).flatmap(e -> new long[] { e, e + 10, e + 20 }).skip(1).toList());
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

    @Test
    public void testCollapseWithTriPredicate() {
        assertEquals(3, LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).count());
        assertEquals(2, LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 3, 11, 10 },
                LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 11, 10 },
                LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 11L, 10L), LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(11L, 10L),
                LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).count());
        assertEquals(2, LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 3, 11, 10 },
                LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 11, 10 },
                LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 11L, 10L),
                LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(11L, 10L),
                LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testScanWithInitIncluded() {
        assertEquals(6, LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 10, 11, 13, 16, 20, 25 }, LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 11, 13, 16, 20, 25 }, LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(10L, 11L, 13L, 16L, 20L, 25L), LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(11L, 13L, 16L, 20L, 25L), LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toList());
        assertEquals(6, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 10, 11, 13, 16, 20, 25 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 11, 13, 16, 20, 25 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(10L, 11L, 13L, 16L, 20L, 25L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(11L, 13L, 16L, 20L, 25L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toList());
    }

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
    public void testConcatArrays() {
        assertEquals(9, LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).count());
        assertEquals(8, LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).skip(1).toList());
        assertEquals(9, LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).count());
        assertEquals(8, LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).skip(1).toList());
    }

    @Test
    public void testConcatListOfArrays() {
        List<long[]> list = Arrays.asList(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 });
        assertEquals(9, LongStream.concat(list).count());
        assertEquals(8, LongStream.concat(list).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(list).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(list).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(list).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(list).skip(1).toList());
        assertEquals(9, LongStream.concat(list).map(e -> e).count());
        assertEquals(8, LongStream.concat(list).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(list).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(list).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(list).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(list).map(e -> e).skip(1).toList());
    }

    @Test
    public void testConcatCollectionOfStreams() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertEquals(9, LongStream.concat(streams).count());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertEquals(8, LongStream.concat(streams).skip(1).count());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(streams).toArray());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(streams).skip(1).toArray());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(streams).toList());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(streams).skip(1).toList());
    }

    @Test
    public void testConcatIterators2() {
        Collection<LongIterator> iterators = Arrays.asList(LongIterator.of(new long[] { 1, 2, 3 }), LongIterator.of(new long[] { 4, 5 }),
                LongIterator.of(new long[] { 6, 7, 8, 9 }));
        assertEquals(9, LongStream.concatIterators(iterators).count());

        iterators = Arrays.asList(LongIterator.of(new long[] { 1, 2, 3 }), LongIterator.of(new long[] { 4, 5 }), LongIterator.of(new long[] { 6, 7, 8, 9 }));
        assertEquals(8, LongStream.concatIterators(iterators).skip(1).count());

        iterators = Arrays.asList(LongIterator.of(new long[] { 1, 2, 3 }), LongIterator.of(new long[] { 4, 5 }), LongIterator.of(new long[] { 6, 7, 8, 9 }));
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concatIterators(iterators).toArray());
    }

    @Test
    public void testZipArrays() {
        assertEquals(3, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).count());
        assertEquals(2, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 11, 22, 33 }, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 22, 33 }, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(11L, 22L, 33L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(22L, 33L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).count());
        assertEquals(2, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 11, 22, 33 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).toArray());
        assertArrayEquals(new long[] { 22, 33 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(11L, 22L, 33L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).toList());
        assertEquals(Arrays.asList(22L, 33L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).skip(1).toList());
    }

    @Test
    public void testZip3Arrays() {
        assertEquals(3, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).count());
        assertEquals(2,
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new long[] { 111, 222, 333 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).toArray());
        assertArrayEquals(new long[] { 222, 333 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).skip(1).toArray());
        assertEquals(Arrays.asList(111L, 222L, 333L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).toList());
        assertEquals(Arrays.asList(222L, 333L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).skip(1).toList());
        assertEquals(3,
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .count());
        assertEquals(2,
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .skip(1)
                        .count());
        assertArrayEquals(new long[] { 111, 222, 333 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .toArray());
        assertArrayEquals(new long[] { 222, 333 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .skip(1)
                        .toArray());
        assertEquals(Arrays.asList(111L, 222L, 333L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .toList());
        assertEquals(Arrays.asList(222L, 333L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testZipCollectionOfStreams() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30), LongStream.of(100, 200, 300));
        LongNFunction<Long> zipFunc = values -> Stream.of(values).mapToLong(v -> v).sum();

        assertEquals(3, LongStream.zip(streams, zipFunc).count());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30), LongStream.of(100, 200, 300));
        assertEquals(2, LongStream.zip(streams, zipFunc).skip(1).count());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30), LongStream.of(100, 200, 300));
        assertArrayEquals(new long[] { 111, 222, 333 }, LongStream.zip(streams, zipFunc).toArray());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30), LongStream.of(100, 200, 300));
        assertArrayEquals(new long[] { 222, 333 }, LongStream.zip(streams, zipFunc).skip(1).toArray());
    }

    @Test
    public void testZipArraysWithDefaults() {
        assertEquals(3, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).count());
        assertEquals(2, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 11, 22, 3 }, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 22, 3 }, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(11L, 22L, 3L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(22L, 3L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).count());
        assertEquals(2, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 11, 22, 3 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).toArray());
        assertArrayEquals(new long[] { 22, 3 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(11L, 22L, 3L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).toList());
        assertEquals(Arrays.asList(22L, 3L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).skip(1).toList());
    }

    @Test
    public void testMergeArrays() {
        assertEquals(5,
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(4,
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 },
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 },
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L),
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L),
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toList());
        assertEquals(5,
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .count());
        assertEquals(4,
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 },
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 },
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L),
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L),
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testMerge3Arrays() {
        assertEquals(7,
                LongStream
                        .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 },
                                (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .count());
        assertEquals(6, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .skip(1)
                .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7 },
                LongStream
                        .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 },
                                (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7 }, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .skip(1)
                .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L),
                LongStream
                        .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 },
                                (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L), LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .skip(1)
                .toList());
        assertEquals(7, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .count());
        assertEquals(6, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .skip(1)
                .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7 }, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7 }, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .skip(1)
                .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L), LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L), LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .skip(1)
                .toList());
    }

    @Test
    public void testMergeStreams() {
        assertEquals(5,
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(4,
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 },
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 },
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L),
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L),
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toList());
        assertEquals(5,
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .count());
        assertEquals(4,
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 },
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 },
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L),
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L),
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testMergeCollectionOfStreams() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        LongBiFunction<MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        assertEquals(6, LongStream.merge(streams, selector).count());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(5, LongStream.merge(streams, selector).skip(1).count());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, LongStream.merge(streams, selector).toArray());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6 }, LongStream.merge(streams, selector).skip(1).toArray());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), LongStream.merge(streams, selector).toList());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L), LongStream.merge(streams, selector).skip(1).toList());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(6, LongStream.merge(streams, selector).map(e -> e).count());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(5, LongStream.merge(streams, selector).map(e -> e).skip(1).count());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, LongStream.merge(streams, selector).map(e -> e).toArray());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6 }, LongStream.merge(streams, selector).map(e -> e).skip(1).toArray());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), LongStream.merge(streams, selector).map(e -> e).toList());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L), LongStream.merge(streams, selector).map(e -> e).skip(1).toList());
    }

    @Test
    public void testFlattmap() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        long[] result = stream.flattMap(n -> java.util.stream.LongStream.of(n, n * 10)).toArray();
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L }, result);
    }

    @Test
    public void testFlattMapToObj() {
        LongStream stream = createLongStream(1L, 2L, 3L);
        List<String> result = stream.flatMapArrayToObj(n -> new String[] { String.valueOf(n), String.valueOf(n * 10) }).toList();
        assertEquals(Arrays.asList("1", "10", "2", "20", "3", "30"), result);
    }

    @Test
    public void testCollapseWithMergeFunction() {
        long[] result = createLongStream(1L, 2L, 2L, 3L, 3L, 3L, 4L).collapse((a, b) -> a == b, Long::sum).toArray();
        assertArrayEquals(new long[] { 1L, 4L, 9L, 4L }, result);
    }

    @Test
    public void testToMapWithMergeFunction() {
        Map<Long, Long> result = createLongStream(1L, 2L, 3L, 1L, 2L).toMap(n -> n, n -> n * 10, (v1, v2) -> v1 + v2);
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(20L), result.get(1L));
        assertEquals(Long.valueOf(40L), result.get(2L));
        assertEquals(Long.valueOf(30L), result.get(3L));
    }

    @Test
    public void testToMapWithSupplier() {
        TreeMap<Long, Long> result = createLongStream(3L, 1L, 2L).toMap(n -> n, n -> n * 10, TreeMap::new);
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1L, 2L, 3L), new ArrayList<>(result.keySet()));
    }

    @Test
    public void testGroupToWithSupplier() {
        TreeMap<Boolean, List<Long>> result = createLongStream(1L, 2L, 3L, 4L, 5L).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList(), TreeMap::new);
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(2L, 4L), result.get(true));
        assertEquals(Arrays.asList(1L, 3L, 5L), result.get(false));
    }

    @Test
    public void testForEachIndexedWithException() throws Exception {
        List<String> result = new ArrayList<>();
        createLongStream(10L, 20L, 30L).forEachIndexed((index, value) -> {
            result.add(index + ":" + value);
        });
        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), result);
    }

    @Test
    public void testAnyMatchWithException() throws Exception {
        assertTrue(createLongStream(1L, 2L, 3L).anyMatch(n -> n == 2L));
        assertFalse(createLongStream(1L, 2L, 3L).anyMatch(n -> n == 5L));
    }

    @Test
    public void testAllMatchWithException() throws Exception {
        assertTrue(createLongStream(2L, 4L, 6L).allMatch(n -> n % 2 == 0));
        assertFalse(createLongStream(1L, 2L, 3L).allMatch(n -> n % 2 == 0));
    }

    @Test
    public void testNoneMatchWithException() throws Exception {
        assertTrue(createLongStream(1L, 3L, 5L).noneMatch(n -> n % 2 == 0));
        assertFalse(createLongStream(1L, 2L, 3L).noneMatch(n -> n % 2 == 0));
    }

    @Test
    public void testFindFirstWithPredicateException() throws Exception {
        OptionalLong result = createLongStream(1L, 2L, 3L, 4L, 5L).findFirst(n -> n > 3);
        assertTrue(result.isPresent());
        assertEquals(4L, result.getAsLong());
    }

    @Test
    public void testFindLastWithPredicateException() throws Exception {
        OptionalLong result = createLongStream(1L, 2L, 3L, 4L, 5L).findLast(n -> n < 4);
        assertTrue(result.isPresent());
        assertEquals(3L, result.getAsLong());
    }

    @Test
    public void testZipWithStreams() {
        LongStream a = createLongStream(1L, 2L, 3L);
        LongStream b = createLongStream(4L, 5L, 6L);

        long[] result = a.zipWith(b, (x, y) -> x + y).toArray();
        assertArrayEquals(new long[] { 5L, 7L, 9L }, result);
    }

    @Test
    public void testZipWithThreeStreamsAndDefaults() {
        LongStream a = createLongStream(1L, 2L);
        LongStream b = createLongStream(3L, 4L, 5L);
        LongStream c = createLongStream(6L);

        long[] result = a.zipWith(b, c, 10L, 20L, 30L, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new long[] { 10L, 36L, 45L }, result);
    }

    @Test
    public void testTransformB() {
        long[] result = createLongStream(1L, 2L, 3L).transformB(s -> s.map(n -> n * 2)).toArray();
        assertArrayEquals(new long[] { 2L, 4L, 6L }, result);
    }

    @Test
    public void testTransformBWithDeferred() {
        long[] result = createLongStream(1L, 2L, 3L).transformB(s -> s.map(n -> n * 2), true).toArray();
        assertArrayEquals(new long[] { 2L, 4L, 6L }, result);
    }

    @Test
    public void testConcatArrayList() {
        List<long[]> arrays = Arrays.asList(new long[] { 1L, 2L }, new long[] { 3L, 4L }, new long[] { 5L, 6L });

        long[] result = LongStream.concat(arrays).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, result);
    }

    @Test
    public void testConcatIteratorCollection() {
        List<LongIterator> iterators = Arrays.asList(LongIterator.of(new long[] { 1L, 2L }), LongIterator.of(new long[] { 3L, 4L }),
                LongIterator.of(new long[] { 5L, 6L }));

        long[] result = LongStream.concatIterators(iterators).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, result);
    }

    @Test
    public void testFlattenVerticallyWithAlignment() {
        long[][] arrays = { { 1L, 2L }, { 3L }, { 4L, 5L, 6L } };
        long[] result = LongStream.flatten(arrays, 0L, true).toArray();
        assertArrayEquals(new long[] { 1L, 3L, 4L, 2L, 0L, 5L, 0L, 0L, 6L }, result);
    }

    @Test
    public void testZipMultipleStreams() {
        Collection<LongStream> streams = Arrays.asList(createLongStream(1L, 2L, 3L), createLongStream(4L, 5L, 6L), createLongStream(7L, 8L, 9L));

        LongStream result = LongStream.zip(streams, values -> Stream.of(values).reduce(0L, Long::sum));
        assertArrayEquals(new long[] { 12L, 15L, 18L }, result.toArray());
    }

    @Test
    public void testZipMultipleStreamsWithDefaults() {
        Collection<LongStream> streams = Arrays.asList(createLongStream(1L, 2L), createLongStream(3L, 4L, 5L), createLongStream(6L));

        long[] defaults = { 10L, 20L, 30L };
        LongStream result = LongStream.zip(streams, defaults, values -> Stream.of(values).reduce(0L, Long::sum));
        assertArrayEquals(new long[] { 10L, 36L, 45L }, result.toArray());
    }

    @Test
    public void testMergeMultipleStreams() {
        Collection<LongStream> streams = Arrays.asList(createLongStream(1L, 4L, 7L), createLongStream(2L, 5L, 8L), createLongStream(3L, 6L, 9L));

        LongStream result = LongStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L }, result.toArray());
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
    public void testPeekIfEmpty() {
        List<Long> result = new ArrayList<>();

        createLongStream(1L, 2L, 3L).ifEmpty(() -> result.add(999L)).forEach(result::add);
        assertEquals(Arrays.asList(1L, 2L, 3L), result);

        result.clear();
        LongStream.empty().ifEmpty(() -> result.add(999L)).forEach(result::add);
        assertEquals(Arrays.asList(999L), result);
    }

    @Test
    public void testJoinTo() {
        Joiner joiner = Joiner.with(", ", "[", "]");
        Joiner result = createLongStream(1L, 2L, 3L).joinTo(joiner);
        assertEquals("[1, 2, 3]", result.toString());
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
    public void testTopNegativeN() {
        assertThrows(IllegalArgumentException.class, () -> createLongStream(1L, 2L, 3L).top(-1));
    }

    @Test
    public void testPrependOptionalLong() {
        long[] result1 = createLongStream(2L, 3L).prepend(OptionalLong.of(1L)).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result1);

        long[] result2 = createLongStream(2L, 3L).prepend(OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 2L, 3L }, result2);
    }

    @Test
    public void testAppendOptionalLong() {
        long[] result1 = createLongStream(1L, 2L).append(OptionalLong.of(3L)).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result1);

        long[] result2 = createLongStream(1L, 2L).append(OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 1L, 2L }, result2);
    }

    @Test
    public void testCollectWithSupplierOnly() {
        LongList result = createLongStream(1L, 2L, 3L).collect(LongList::new, LongList::add);
        assertEquals(3, result.size());
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result.toArray());
    }

    @Test
    public void testIntervalWithDelay() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long[] timestamps = LongStream.interval(50, 10).limit(3).toArray();
        long endTime = System.currentTimeMillis();

        assertEquals(3, timestamps.length);
        assertTrue(endTime - startTime >= 70);
    }

    @Test
    public void testIntervalWithTimeUnit() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long[] timestamps = LongStream.interval(50, 10, TimeUnit.MILLISECONDS).limit(3).toArray();
        long endTime = System.currentTimeMillis();

        assertEquals(3, timestamps.length);
        assertTrue(endTime - startTime >= 70);
    }

    @Test
    public void testDefaultIfEmpty() {
        assertArrayEquals(new long[] { 1L, 2L, 3L }, createLongStream(1L, 2L, 3L).defaultIfEmpty(() -> createLongStream(10L, 20L)).toArray());
        assertArrayEquals(new long[] { 10L, 20L }, LongStream.empty().defaultIfEmpty(() -> createLongStream(10L, 20L)).toArray());
    }

    @Test
    public void testForeach() {
        List<Long> result = new ArrayList<>();
        createLongStream(1L, 2L, 3L).foreach(result::add);
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testClose() {
        AtomicLong closed = new AtomicLong(0);
        LongStream stream = createLongStream(1L, 2L, 3L).onClose(() -> closed.incrementAndGet());

        assertEquals(0, closed.get());
        stream.close();
        assertEquals(1, closed.get());
    }

    @Test
    public void testOnlyOneWithMultipleElements() {
        assertThrows(TooManyElementsException.class, () -> createLongStream(1L, 2L).onlyOne());
    }

    @Test
    public void testInterval() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long[] timestamps = LongStream.interval(10).limit(3).toArray();
        long endTime = System.currentTimeMillis();

        assertEquals(3, timestamps.length);
        assertTrue(endTime - startTime >= 20);
    }

    @Test
    public void testIterateWithBooleanSupplier() {
        AtomicLong counter = new AtomicLong(0);
        long[] result = LongStream.iterate(() -> counter.get() < 3, counter::incrementAndGet).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testZipWithThreeArrays() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 4L, 5L, 6L };
        long[] c = { 7L, 8L, 9L };

        long[] result = LongStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new long[] { 12L, 15L, 18L }, result);
    }

    @Test
    public void testMergeWithThreeArrays() {
        long[] a = { 1L, 4L, 7L };
        long[] b = { 2L, 5L, 8L };
        long[] c = { 3L, 6L, 9L };

        LongStream merged = LongStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L }, merged.toArray());
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
    @DisplayName("flatten() with empty array should return empty stream")
    public void testFlattenEmpty() {
        long[][] array = {};
        LongStream stream = LongStream.flatten(array);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRepeat_02() {
        LongStream stream = LongStream.repeat(1, 1000);
        assertEquals(1000, stream.mapToFloat(e -> (float) e).count());
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
    public void test_concat_02() {
        long[] a = { 1, 2, 3 };
        long[] b = { 4, 5, 6 };
        long[] result = LongStream.concat(a, b).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);

        result = LongStream.concat(N.toList(a, b)).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);

        result = LongStream.concatIterators(N.toList(LongIterator.of(a), LongIterator.of(b))).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void test_zip_02() {
        long[] a = { 1 };
        long[] b = { 4, 5 };
        long[] c = { 7, 8, 9 };
        long[] result = LongStream.zip(a, b, c, 100, 200, 300, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new long[] { 12, 113, 309 }, result);
    }

    @Test
    public void test_merge() {
        long[] a = { 1, 2, 3 };
        long[] b = { 4, 5, 6 };
        long[] c = { 7, 8, 9 };
        long[] result = LongStream.merge(N.toList(LongStream.of(a)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new long[] { 1, 2, 3 }, result);

        result = LongStream.merge(N.toList(LongStream.of(a), LongStream.of(b)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);

        result = LongStream
                .merge(N.toList(LongStream.of(a), LongStream.of(b), LongStream.of(c)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result);

    }

    // TODO: filter(LongPredicate) is abstract - tested via concrete implementations above
    // TODO: takeWhile(LongPredicate) is abstract - tested via concrete implementations above
    // TODO: dropWhile(LongPredicate) is abstract - tested via concrete implementations above
    // TODO: map(LongUnaryOperator) is abstract - tested via concrete implementations above
    // TODO: mapToInt(LongToIntFunction) is abstract - tested via concrete implementations above
    // TODO: mapToFloat(LongToFloatFunction) is abstract - tested via concrete implementations above
    // TODO: mapToDouble(LongToDoubleFunction) is abstract - tested via concrete implementations above
    // TODO: mapToObj(LongFunction) is abstract - tested via concrete implementations above
    // TODO: flatMap(LongFunction) is abstract - tested via concrete implementations above
    // TODO: flatmap(LongFunction<long[]>) is abstract - tested via concrete implementations above
    // TODO: flatMapToInt(LongFunction) is abstract - tested via concrete implementations above
    // TODO: flatMapToFloat(LongFunction) is abstract - tested via concrete implementations above
    // TODO: flatMapToDouble(LongFunction) is abstract - tested via concrete implementations above
    // TODO: flatMapToObj(LongFunction) is abstract - tested via concrete implementations above
    // TODO: flatmapToObj(LongFunction) is abstract - tested via concrete implementations above
    // TODO: mapMulti(LongMapMultiConsumer) is abstract - tested via concrete implementations above
    // TODO: mapPartial(LongFunction) is abstract - tested via concrete implementations above
    // TODO: mapPartialJdk(LongFunction) is abstract - tested via concrete implementations above
    // TODO: rangeMap(LongBiPredicate, LongBinaryOperator) is abstract - tested via concrete implementations above
    // TODO: rangeMapToObj(LongBiPredicate, LongBiFunction) is abstract - tested via concrete implementations above
    // TODO: collapse(...) overloads are abstract - tested via concrete implementations above
    // TODO: scan(...) overloads are abstract - tested via concrete implementations above
    // TODO: prepend(long...) is abstract - tested via concrete implementations above
    // TODO: append(long...) is abstract - tested via concrete implementations above
    // TODO: appendIfEmpty(long...) is abstract - tested via concrete implementations above
    // TODO: top(...) overloads are abstract - tested via concrete implementations above
    // TODO: toLongList() is abstract - tested via concrete implementations above
    // TODO: toMap(...) overloads are abstract - tested via concrete implementations above
    // TODO: groupTo(...) overloads are abstract - tested via concrete implementations above
    // TODO: reduce(...) overloads are abstract - tested via concrete implementations above
    // TODO: collect(...) overloads are abstract - tested via concrete implementations above
    // TODO: forEach(Throwables.LongConsumer) is abstract - tested via concrete implementations above
    // TODO: forEachIndexed(Throwables.IntLongConsumer) is abstract - tested via concrete implementations above
    // TODO: anyMatch/allMatch/noneMatch are abstract - tested via concrete implementations above
    // TODO: findFirst/findAny/findLast with predicate are abstract - tested via concrete implementations above
    // TODO: min/max/kthLargest/sum/average/summaryStatistics are abstract - tested via concrete implementations above
    // TODO: mergeWith/zipWith overloads are abstract - tested via concrete implementations above
    // TODO: asFloatStream/asDoubleStream/boxed/toJdkStream are abstract - tested via concrete implementations above

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
    public void testFlatMapArrayToObj() {
        List<String> result = createLongStream(1L, 2L, 3L).flatMapArrayToObj(n -> new String[] { "A" + n, "B" + n }).toList();
        assertEquals(Arrays.asList("A1", "B1", "A2", "B2", "A3", "B3"), result);
    }

    @Test
    public void testFlatMapArrayToObj_Empty() {
        List<String> result = LongStream.empty().flatMapArrayToObj(n -> new String[] { "A" + n }).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindFirst_NoArg() {
        OptionalLong result = createLongStream(10L, 20L, 30L).findFirst();
        assertTrue(result.isPresent());
        assertEquals(10L, result.getAsLong());
    }

    @Test
    public void testFindFirst_NoArg_Empty() {
        OptionalLong result = LongStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAny_NoArg() {
        OptionalLong result = createLongStream(10L, 20L, 30L).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_NoArg_Empty() {
        OptionalLong result = LongStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testTransformB_TwoArg() {
        long[] result = createLongStream(3L, 1L, 2L).transformB(s -> s.sorted(), false).toArray();
        assertArrayEquals(new long[] { 1, 2, 3 }, result);
    }

    @Test
    public void testTransformB_TwoArg_Deferred() {
        long[] result = createLongStream(3L, 1L, 2L).transformB(s -> s.sorted(), true).toArray();
        assertArrayEquals(new long[] { 1, 2, 3 }, result);
    }

    @Test
    public void testFrom_Empty() {
        java.util.stream.LongStream jdkStream = java.util.stream.LongStream.empty();
        LongStream result = LongStream.from(jdkStream);
        assertEquals(0, result.count());
    }

    @Test
    public void testConcatIteratorsCollection2() {
        List<LongIterator> iterators = Arrays.asList(LongIterator.of(new long[] { 1, 2 }), LongIterator.of(new long[] { 3, 4 }));
        LongStream result = LongStream.concatIterators(iterators);
        assertArrayEquals(new long[] { 1, 2, 3, 4 }, result.toArray());
    }

    @Test
    public void testMergeIteratorThreeWay() {
        LongIterator a = LongIterator.of(new long[] { 1, 4, 7 });
        LongIterator b = LongIterator.of(new long[] { 2, 5, 8 });
        LongIterator c = LongIterator.of(new long[] { 3, 6, 9 });
        long[] result = LongStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result);
    }

    @Test
    public void testZipIteratorsThreeWay() {
        LongIterator a = LongIterator.of(new long[] { 1, 2 });
        LongIterator b = LongIterator.of(new long[] { 10, 20 });
        LongIterator c = LongIterator.of(new long[] { 100, 200 });
        long[] result = LongStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new long[] { 111, 222 }, result);
    }

    @Test
    public void testZipIteratorsWithDefaults() {
        LongIterator a = LongIterator.of(new long[] { 1, 2, 3 });
        LongIterator b = LongIterator.of(new long[] { 10, 20 });
        long[] result = LongStream.zip(a, b, 0L, 0L, Long::sum).toArray();
        assertArrayEquals(new long[] { 11, 22, 3 }, result);
    }

    @Test
    public void testZipIteratorsThreeWayWithDefaults() {
        LongIterator a = LongIterator.of(new long[] { 1 });
        LongIterator b = LongIterator.of(new long[] { 10, 20 });
        LongIterator c = LongIterator.of(new long[] { 100, 200, 300 });
        long[] result = LongStream.zip(a, b, c, 0L, 0L, 0L, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new long[] { 111, 220, 300 }, result);
    }

    @Test
    public void testZipStreamsWithDefaults() {
        long[] result = LongStream.zip(LongStream.of(1, 2, 3), LongStream.of(10, 20), 0L, 0L, Long::sum).toArray();
        assertArrayEquals(new long[] { 11, 22, 3 }, result);
    }

    @Test
    public void testZipStreamsThreeWayWithDefaults() {
        long[] result = LongStream.zip(LongStream.of(1), LongStream.of(10, 20), LongStream.of(100, 200, 300), 0L, 0L, 0L, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new long[] { 111, 220, 300 }, result);
    }

    @Test
    public void testMergeStreamsThreeWay() {
        long[] result = LongStream
                .merge(LongStream.of(1, 4, 7), LongStream.of(2, 5, 8), LongStream.of(3, 6, 9),
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result);
    }

    @Test
    public void testZipCollectionWithDefaults() {
        List<LongStream> streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(10, 20));
        long[] result = LongStream.zip(streams, new long[] { 0, 0 }, values -> values[0] + values[1]).toArray();
        assertArrayEquals(new long[] { 11, 22, 3 }, result);
    }

    @Test
    public void testToMap_WithMerge() {
        java.util.Map<Boolean, Long> map = createLongStream(1L, 2L, 3L, 4L, 5L, 6L).toMap(n -> n % 2 == 0, n -> n, Long::sum);
        assertEquals(Long.valueOf(12), map.get(true));
        assertEquals(Long.valueOf(9), map.get(false));
    }

    @Test
    public void testToMap_WithMapSupplier() {
        TreeMap<String, Long> map = createLongStream(1L, 2L, 3L).toMap(n -> "key" + n, n -> n * 10, TreeMap::new);
        assertEquals(3, map.size());
        assertEquals("key1", map.firstKey());
    }

    @Test
    public void testGroupTo_WithMapSupplier() {
        TreeMap<Boolean, List<Long>> grouped = createLongStream(1L, 2L, 3L, 4L, 5L, 6L).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList(),
                TreeMap::new);
        assertEquals(2, grouped.size());
        assertEquals(Arrays.asList(2L, 4L, 6L), grouped.get(true));
    }

    @Test
    public void testCollect_WithoutCombiner() {
        List<Long> result = createLongStream(1L, 2L, 3L).collect(ArrayList::new, (list, n) -> list.add(n));
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
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

    // ===== New tests for untested methods =====

    @Test
    public void testConcatIterators_Collection() {
        List<LongIterator> iters = new ArrayList<>();
        iters.add(LongIterator.of(new long[] { 1L, 2L }));
        iters.add(LongIterator.of(new long[] { 3L, 4L }));
        long[] result = LongStream.concatIterators(iters).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcatIterators_EmptyCollection() {
        List<LongIterator> iters = new ArrayList<>();
        long[] result = LongStream.concatIterators(iters).toArray();
        assertArrayEquals(new long[0], result);
    }

    @Test
    public void testLimit_WithOffset() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).limit(1, 3).toArray();
        assertArrayEquals(new long[] { 2, 3, 4 }, result);
    }

    @Test
    public void testLimit_WithOffsetBeyondSize() {
        long[] result = LongStream.of(1, 2, 3).limit(5, 3).toArray();
        assertArrayEquals(new long[0], result);
    }

    @Test
    public void testFlatMapArrayToObj_HappyPath() {
        List<String> result = LongStream.of(1, 2, 3).flatMapArrayToObj(i -> new String[] { String.valueOf(i), String.valueOf(i * 10) }).toList();
        assertEquals(Arrays.asList("1", "10", "2", "20", "3", "30"), result);
    }

    @Test
    public void testFlatMapArrayToObj_Empty2() {
        List<String> result = LongStream.empty().flatMapArrayToObj(i -> new String[] { String.valueOf(i) }).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapMulti_HappyPath() {
        long[] result = LongStream.of(1, 2, 3).mapMulti((value, consumer) -> {
            consumer.accept(value);
            consumer.accept(value * 10);
        }).toArray();
        assertArrayEquals(new long[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testMapMulti_Empty() {
        long[] result = LongStream.empty().mapMulti((value, consumer) -> consumer.accept(value)).toArray();
        assertArrayEquals(new long[0], result);
    }

    @Test
    public void testMapPartial_HappyPath() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalLong.of(i * 10) : OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 20, 40 }, result);
    }

    @Test
    public void testMapPartialJdk_HappyPath() {
        long[] result = LongStream.of(1, 2, 3, 4, 5)
                .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalLong.of(i * 10) : java.util.OptionalLong.empty())
                .toArray();
        assertArrayEquals(new long[] { 20, 40 }, result);
    }

    @Test
    public void testRangeMap_HappyPath() {
        long[] result = LongStream.of(1, 1, 2, 2, 3).rangeMap((a, b) -> a == b, Long::sum).toArray();
        assertArrayEquals(new long[] { 2, 4, 6 }, result);
    }

    @Test
    public void testRangeMapToObj_HappyPath() {
        List<String> result = LongStream.of(1, 1, 2, 3, 3).rangeMapToObj((a, b) -> a == b, (a, b) -> a + "-" + b).toList();
        assertEquals(Arrays.asList("1-1", "2-2", "3-3"), result);
    }

    @Test
    public void testScan_BasicScan() {
        long[] result = LongStream.of(1, 2, 3, 4).scan(Long::sum).toArray();
        assertArrayEquals(new long[] { 1, 3, 6, 10 }, result);
    }

    @Test
    public void testScan_WithInit2() {
        long[] result = LongStream.of(1, 2, 3).scan(10L, Long::sum).toArray();
        assertArrayEquals(new long[] { 11, 13, 16 }, result);
    }

    @Test
    public void testScan_WithInitIncluded2() {
        long[] result = LongStream.of(1, 2, 3).scan(10L, true, Long::sum).toArray();
        assertArrayEquals(new long[] { 10, 11, 13, 16 }, result);
    }

    @Test
    public void testCollapse_ListForm() {
        List<LongList> result = LongStream.of(1, 1, 2, 2, 3).collapse((a, b) -> a == b).toList();
        assertEquals(3, result.size());
        assertArrayEquals(new long[] { 1, 1 }, result.get(0).toArray());
        assertArrayEquals(new long[] { 2, 2 }, result.get(1).toArray());
        assertArrayEquals(new long[] { 3 }, result.get(2).toArray());
    }

    @Test
    public void testCollapse_WithTriPredicate() {
        long[] result = LongStream.of(1, 2, 3, 10, 11, 12).collapse((first, previous, current) -> current - first <= 2, Long::sum).toArray();
        assertArrayEquals(new long[] { 6, 33 }, result);
    }

    @Test
    public void testSkipUntil_HappyPath() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).skipUntil(x -> x >= 3).toArray();
        assertArrayEquals(new long[] { 3, 4, 5 }, result);
    }

    @Test
    public void testSkipUntil_NeverMatches() {
        long[] result = LongStream.of(1, 2, 3).skipUntil(x -> x > 10).toArray();
        assertArrayEquals(new long[0], result);
    }

    @Test
    public void testStep_HappyPath() {
        long[] result = LongStream.of(1, 2, 3, 4, 5, 6).step(2).toArray();
        assertArrayEquals(new long[] { 1, 3, 5 }, result);
    }

    @Test
    public void testStep_StepThree() {
        long[] result = LongStream.of(1, 2, 3, 4, 5, 6, 7).step(3).toArray();
        assertArrayEquals(new long[] { 1, 4, 7 }, result);
    }

    @Test
    public void testCycled_HappyPath() {
        long[] result = LongStream.of(1, 2, 3).cycled().limit(7).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 1, 2, 3, 1 }, result);
    }

    @Test
    public void testCycled_WithRounds2() {
        long[] result = LongStream.of(1, 2).cycled(3).toArray();
        assertArrayEquals(new long[] { 1, 2, 1, 2, 1, 2 }, result);
    }

    @Test
    public void testReverseSorted_HappyPath() {
        long[] result = LongStream.of(3, 1, 4, 1, 5).reverseSorted().toArray();
        assertArrayEquals(new long[] { 5, 4, 3, 1, 1 }, result);
    }

    @Test
    public void testReverseSorted_Empty() {
        long[] result = LongStream.empty().reverseSorted().toArray();
        assertArrayEquals(new long[0], result);
    }

    @Test
    public void testIntersection_HappyPath() {
        Set<Long> other = new HashSet<>(Arrays.asList(2L, 4L, 6L));
        long[] result = LongStream.of(1, 2, 3, 4, 5).intersection(other).toArray();
        assertArrayEquals(new long[] { 2, 4 }, result);
    }

    @Test
    public void testDifference_HappyPath() {
        Set<Long> other = new HashSet<>(Arrays.asList(2L, 4L));
        long[] result = LongStream.of(1, 2, 3, 4, 5).difference(other).toArray();
        assertArrayEquals(new long[] { 1, 3, 5 }, result);
    }

    @Test
    public void testSymmetricDifference_HappyPath() {
        List<Long> other = Arrays.asList(2L, 4L, 6L);
        long[] result = LongStream.of(1, 2, 3, 4, 5).symmetricDifference(other).sorted().toArray();
        assertArrayEquals(new long[] { 1, 3, 5, 6 }, result);
    }

    @Test
    public void testIndexed_HappyPath() {
        List<IndexedLong> result = LongStream.of(10, 20, 30).indexed().toList();
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).index());
        assertEquals(10, result.get(0).value());
        assertEquals(1, result.get(1).index());
        assertEquals(20, result.get(1).value());
    }

    @Test
    public void testFirst_HappyPath2() {
        OptionalLong result = LongStream.of(5, 3, 1).first();
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testFirst_Empty() {
        OptionalLong result = LongStream.empty().first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast_HappyPath() {
        OptionalLong result = LongStream.of(5, 3, 1).last();
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testLast_Empty() {
        OptionalLong result = LongStream.empty().last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testJoin_WithDelimiter() {
        String result = LongStream.of(1, 2, 3).join(", ");
        assertEquals("1, 2, 3", result);
    }

    @Test
    public void testJoin_WithDelimiterAndPrefixSuffix() {
        String result = LongStream.of(1, 2, 3).join(", ", "[", "]");
        assertEquals("[1, 2, 3]", result);
    }

    @Test
    public void testToList_HappyPath() {
        List<Long> result = LongStream.of(1, 2, 3).toList();
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testToSet_HappyPath() {
        Set<Long> result = LongStream.of(1, 2, 2, 3, 3).toSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1L));
    }

    @Test
    public void testToMultiset_HappyPath() {
        Multiset<Long> result = LongStream.of(1, 2, 2, 3, 3, 3).toMultiset();
        assertEquals(1, result.get(1L));
        assertEquals(2, result.get(2L));
        assertEquals(3, result.get(3L));
    }

    @Test
    public void testToImmutableList_HappyPath() {
        com.landawn.abacus.util.ImmutableList<Long> result = LongStream.of(1, 2, 3).toImmutableList();
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(1), result.get(0));
    }

    @Test
    public void testToImmutableSet_HappyPath() {
        com.landawn.abacus.util.ImmutableSet<Long> result = LongStream.of(1, 2, 2, 3).toImmutableSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1L));
    }

    @Test
    public void testCount_HappyPath() {
        long result = LongStream.of(1, 2, 3, 4, 5).count();
        assertEquals(5, result);
    }

    @Test
    public void testCount_Empty() {
        long result = LongStream.empty().count();
        assertEquals(0, result);
    }

    @Test
    public void testSkip_WithAction2() {
        List<Long> skipped = new ArrayList<>();
        long[] result = LongStream.of(1, 2, 3, 4, 5).skip(2, value -> skipped.add(value)).toArray();
        assertArrayEquals(new long[] { 3, 4, 5 }, result);
        assertEquals(Arrays.asList(1L, 2L), skipped);
    }

    @Test
    public void testFilter_WithAction() {
        List<Long> rejected = new ArrayList<>();
        long[] result = LongStream.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0, value -> rejected.add(value)).toArray();
        assertArrayEquals(new long[] { 2, 4 }, result);
        assertEquals(Arrays.asList(1L, 3L, 5L), rejected);
    }

    @Test
    public void testDropWhile_WithAction() {
        List<Long> dropped = new ArrayList<>();
        long[] result = LongStream.of(1, 2, 3, 4, 5).dropWhile(x -> x <= 3, value -> dropped.add(value)).toArray();
        assertArrayEquals(new long[] { 4, 5 }, result);
        assertEquals(Arrays.asList(1L, 2L, 3L), dropped);
    }

    @Test
    public void testForeach_HappyPath() {
        List<Long> collected = new ArrayList<>();
        LongStream.of(1, 2, 3).foreach(collected::add);
        assertEquals(Arrays.asList(1L, 2L, 3L), collected);
    }

    @Test
    public void testAsFloatStream_HappyPath() {
        float[] result = LongStream.of(1, 2, 3).asFloatStream().toArray();
        assertEquals(3, result.length);
        assertEquals(1.0f, result[0], 0.001f);
        assertEquals(2.0f, result[1], 0.001f);
    }

    @Test
    public void testAsDoubleStream_HappyPath() {
        double[] result = LongStream.of(1, 2, 3).asDoubleStream().toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testPsp_HappyPath() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).psp(s -> s.filter(x -> x % 2 != 0)).toArray();
        Arrays.sort(result);
        assertArrayEquals(new long[] { 1, 3, 5 }, result);
    }

    @Test
    public void testSps_HappyPath() {
        long sum = LongStream.of(1, 2, 3, 4, 5).sps(s -> s.map(x -> x * 2)).sum();
        assertEquals(30, sum);
    }

    @Test
    public void testTransform_HappyPath2() {
        long sum = LongStream.of(1, 2, 3).transform(s -> s.map(x -> x * 2)).sum();
        assertEquals(12, sum);
    }

    @Test
    public void testTransformB_HappyPath2() {
        long[] result = LongStream.of(1, 2, 3).transformB(s -> s.map(x -> x * 2)).toArray();
        assertArrayEquals(new long[] { 2, 4, 6 }, result);
    }

    @Test
    public void testZipWith_CollectionOfStreams() {
        List<LongStream> streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30));
        long[] result = LongStream.zip(streams, args -> args[0] + args[1]).toArray();
        assertArrayEquals(new long[] { 11, 22, 33 }, result);
    }

    @Test
    public void testZipWith_CollectionOfStreamsWithDefaults() {
        List<LongStream> streams = Arrays.asList(LongStream.of(1, 2), LongStream.of(10, 20, 30));
        long[] result = LongStream.zip(streams, new long[] { 0, 0 }, args -> args[0] + args[1]).toArray();
        assertArrayEquals(new long[] { 11, 22, 30 }, result);
    }

    @Test
    public void testMerge_CollectionOfStreams() {
        List<LongStream> streams = Arrays.asList(LongStream.of(1, 3, 5), LongStream.of(2, 4, 6));
        long[] result = LongStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testMerge_ThreeArrays() {
        long[] result = LongStream
                .merge(new long[] { 1, 4 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testMerge_ThreeIterators() {
        long[] result = LongStream
                .merge(LongIterator.of(new long[] { 1, 4 }), LongIterator.of(new long[] { 2, 5 }), LongIterator.of(new long[] { 3, 6 }),
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testCollapse_WithMergeFunction2() {
        long[] result = LongStream.of(1, 1, 2, 2, 3).collapse((a, b) -> a == b, Long::sum).toArray();
        assertArrayEquals(new long[] { 2, 4, 3 }, result);
    }

    @Test
    public void testPercentiles_HappyPath() {
        Pair<LongSummaryStatistics, Optional<Map<Percentage, Long>>> result = LongStream.of(1, 2, 3, 4, 5).summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertEquals(5, result.left().getCount());
        assertEquals(1, result.left().getMin());
        assertEquals(5, result.left().getMax());
    }

    @Test
    public void testOnClose_HappyPath() {
        AtomicBoolean closed = new AtomicBoolean(false);
        LongStream s = LongStream.of(1, 2, 3).onClose(() -> closed.set(true));
        s.toArray();
        s.close();
        assertTrue(closed.get());
    }

    @Test
    public void testAcceptIfNotEmpty_HappyPath() {
        AtomicLong sum = new AtomicLong(0);
        LongStream.of(1, 2, 3).acceptIfNotEmpty(s -> sum.set(s.sum()));
        assertEquals(6, sum.get());
    }

    @Test
    public void testAcceptIfNotEmpty_EmptyStream() {
        AtomicBoolean called = new AtomicBoolean(false);
        LongStream.empty().acceptIfNotEmpty(s -> called.set(true));
        assertFalse(called.get());
    }

    @Test
    public void testApplyIfNotEmpty_HappyPath() {
        Optional<Long> result = LongStream.of(1, 2, 3).applyIfNotEmpty(s -> s.sum());
        assertTrue(result.isPresent());
        assertEquals(6L, result.get().longValue());
    }

    @Test
    public void testApplyIfNotEmpty_EmptyStream() {
        Optional<Long> result = LongStream.empty().applyIfNotEmpty(s -> s.sum());
        assertFalse(result.isPresent());
    }

    @Test
    public void testThrowIfEmpty_NonEmpty() {
        long[] result = LongStream.of(1, 2, 3).throwIfEmpty().toArray();
        assertArrayEquals(new long[] { 1, 2, 3 }, result);
    }

    @Test
    public void testThrowIfEmpty_EmptyThrows() {
        assertThrows(java.util.NoSuchElementException.class, () -> LongStream.empty().throwIfEmpty().toArray());
    }

    @Test
    public void testElementAt_HappyPath() {
        OptionalLong result = LongStream.of(10, 20, 30, 40).elementAt(2);
        assertTrue(result.isPresent());
        assertEquals(30, result.get());
    }

    @Test
    public void testElementAt_BeyondSize() {
        OptionalLong result = LongStream.of(10, 20).elementAt(5);
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_HappyPath2() {
        OptionalLong result = LongStream.of(42).onlyOne();
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    public void testOnlyOne_EmptyStream2() {
        OptionalLong result = LongStream.empty().onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_TooManyElements() {
        assertThrows(com.landawn.abacus.exception.TooManyElementsException.class, () -> LongStream.of(1, 2).onlyOne());
    }

    @Test
    public void testFlatten_WithAlignment() {
        long[][] a = { { 1, 2, 3 }, { 4, 5 } };
        long[] result = LongStream.flatten(a, 0L, true).toArray();
        assertArrayEquals(new long[] { 1, 4, 2, 5, 3, 0 }, result);
    }

    @Test
    public void testCollect_TwoArg() {
        List<Long> result = LongStream.of(1, 2, 3).collect(ArrayList::new, (list, val) -> list.add(val));
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testToMap_WithMergeFunction2() {
        Map<Long, Long> result = LongStream.of(1, 2, 1, 3, 2).toMap(i -> i, i -> 1L, Long::sum);
        assertEquals(3, result.size());
        assertEquals(2L, result.get(1L).longValue());
        assertEquals(2L, result.get(2L).longValue());
        assertEquals(1L, result.get(3L).longValue());
    }

    @Test
    public void testFlattMap_HappyPath() {
        long[] result = LongStream.of(1, 2, 3).flattMap(i -> java.util.stream.LongStream.of(i, i * 10)).toArray();
        assertArrayEquals(new long[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testFlatmap_HappyPath2() {
        long[] result = LongStream.of(1, 2, 3).flatmap(i -> new long[] { i, i * 10 }).toArray();
        assertArrayEquals(new long[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testFlatmapToObj_HappyPath() {
        List<String> result = LongStream.of(1, 2).flatmapToObj(i -> Arrays.asList(String.valueOf(i), String.valueOf(i * 10))).toList();
        assertEquals(Arrays.asList("1", "10", "2", "20"), result);
    }

    @Test
    public void testPeek_HappyPath() {
        List<Long> peeked = new ArrayList<>();
        long[] result = LongStream.of(1, 2, 3).peek(peeked::add).toArray();
        assertArrayEquals(new long[] { 1, 2, 3 }, result);
        assertEquals(Arrays.asList(1L, 2L, 3L), peeked);
    }

    @Test
    public void testShuffled_HappyPath2() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).shuffled().toArray();
        assertEquals(5, result.length);
        Arrays.sort(result);
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testShuffled_WithRandom2() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toArray();
        assertEquals(5, result.length);
        Arrays.sort(result);
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testRotated_HappyPath() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).rotated(2).toArray();
        assertArrayEquals(new long[] { 4, 5, 1, 2, 3 }, result);
    }

    @Test
    public void testRotated_NegativeDistance() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).rotated(-2).toArray();
        assertArrayEquals(new long[] { 3, 4, 5, 1, 2 }, result);
    }

    @Test
    public void testReversed_HappyPath() {
        long[] result = LongStream.of(1, 2, 3).reversed().toArray();
        assertArrayEquals(new long[] { 3, 2, 1 }, result);
    }

    @Test
    public void testReversed_Empty() {
        long[] result = LongStream.empty().reversed().toArray();
        assertArrayEquals(new long[0], result);
    }

    @Test
    public void testMapToFloat_HappyPath() {
        float[] result = LongStream.of(1, 2, 3).mapToFloat(l -> (float) l).toArray();
        assertEquals(3, result.length);
        assertEquals(1.0f, result[0], 0.001f);
    }

    @Test
    public void testFlatMapToFloat_HappyPath() {
        float[] result = LongStream.of(1, 2).flatMapToFloat(l -> FloatStream.of((float) l, (float) (l * 10))).toArray();
        assertEquals(4, result.length);
        assertEquals(1.0f, result[0], 0.001f);
        assertEquals(10.0f, result[1], 0.001f);
    }

    @Test
    public void testIsParallel_HappyPath2() {
        assertFalse(LongStream.of(1, 2, 3).isParallel());
        assertTrue(LongStream.of(1, 2, 3).parallel().isParallel());
    }

    @Test
    public void testSequential_AfterParallel() {
        long sum = LongStream.of(1, 2, 3).parallel().sequential().sum();
        assertEquals(6, sum);
    }

    @Test
    public void testFlatten_3D() {
        long[][][] a = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 } } };
        long[] result = LongStream.flatten(a).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testRangeWithNegativeStep() {
        long[] result = LongStream.range(5, 0, -1).toArray();
        assertArrayEquals(new long[] { 5, 4, 3, 2, 1 }, result);
    }

    @Test
    public void testRangeClosedWithNegativeStep2() {
        long[] result = LongStream.rangeClosed(5, 1, -1).toArray();
        assertArrayEquals(new long[] { 5, 4, 3, 2, 1 }, result);
    }

    @Test
    public void testFindLast_HappyPath() {
        OptionalLong result = LongStream.of(1, 2, 3, 4, 5).findLast(x -> x < 4);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    public void testFindLast_Empty() {
        OptionalLong result = LongStream.empty().findLast(x -> true);
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest_HappyPath() {
        OptionalLong result = LongStream.of(3, 1, 4, 1, 5).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4, result.get());
    }

    @Test
    public void testAverage_HappyPath() {
        OptionalDouble avg = LongStream.of(1, 2, 3, 4, 5).average();
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.get(), 0.001);
    }

    @Test
    public void testAverage_Empty() {
        OptionalDouble avg = LongStream.empty().average();
        assertFalse(avg.isPresent());
    }

}
