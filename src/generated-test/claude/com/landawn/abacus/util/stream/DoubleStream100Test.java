package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.DoubleBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.DoubleBiFunction;
import com.landawn.abacus.util.function.DoubleTernaryOperator;

public class DoubleStream100Test extends TestBase {

    // This method needs to be implemented by a concrete test class to provide a DoubleStream instance.
    // For example, in ArrayDoubleStreamTest, it would return new ArrayDoubleStream(a);
    // In IteratorDoubleStreamTest, it would return new IteratorDoubleStream(DoubleIterator.of(a));
    protected DoubleStream createDoubleStream(double... a) {
        return DoubleStream.of(a);
    }

    protected DoubleStream createDoubleStream(double[] a, int fromIndex, int toIndex) {
        return DoubleStream.of(a, fromIndex, toIndex);
    }

    protected DoubleStream createDoubleStream(Double[] a) {
        return DoubleStream.of(a);
    }

    protected DoubleStream createDoubleStream(Double[] a, int fromIndex, int toIndex) {
        return DoubleStream.of(a, fromIndex, toIndex);
    }

    protected DoubleStream createDoubleStream(Collection<Double> coll) {
        return DoubleStream.of(coll.toArray(new Double[coll.size()]));
    }

    protected DoubleStream createDoubleStream(DoubleIterator iter) {
        return iter == null ? DoubleStream.empty() : DoubleStream.of(iter.toArray());
    }

    protected DoubleStream createDoubleStream(DoubleBuffer buff) {
        return DoubleStream.of(buff);
    }

    protected DoubleStream createDoubleStream(OptionalDouble op) {
        return DoubleStream.of(op);
    }

    @Test
    public void testEmpty() {
        DoubleStream stream = DoubleStream.empty();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    public void testDefer() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<DoubleStream> supplier = () -> {
            counter.incrementAndGet();
            return createDoubleStream(1.0, 2.0, 3.0);
        };

        DoubleStream stream = DoubleStream.defer(supplier);
        assertEquals(0, counter.get()); // Supplier not called yet

        double[] result = stream.toArray();
        assertEquals(1, counter.get()); // Supplier called once
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);
    }

    @Test
    public void testFromJdkStream() {
        java.util.stream.DoubleStream jdkStream = java.util.stream.DoubleStream.of(1.0, 2.0, 3.0);
        DoubleStream stream = DoubleStream.from(jdkStream);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.toArray(), 0.0001);

        // Test with null
        DoubleStream emptyStream = DoubleStream.from(null);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testOfNullable() {
        DoubleStream stream1 = DoubleStream.ofNullable(5.0);
        assertEquals(1, stream1.count());

        DoubleStream stream2 = DoubleStream.ofNullable(null);
        assertEquals(0, stream2.count());
    }

    @Test
    public void testOfArray() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        DoubleStream stream = createDoubleStream(array);
        assertArrayEquals(array, stream.toArray(), 0.0001);

        // Test empty array
        DoubleStream emptyStream = createDoubleStream(new double[0]);
        assertEquals(0, emptyStream.count());

        // Test with range
        DoubleStream rangeStream = createDoubleStream(array, 1, 4);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, rangeStream.toArray(), 0.0001);
    }

    @Test
    public void testOfBoxedArray() {
        Double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        DoubleStream stream = createDoubleStream(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, stream.toArray(), 0.0001);

        // Test with range
        DoubleStream rangeStream = createDoubleStream(array, 1, 4);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, rangeStream.toArray(), 0.0001);
    }

    @Test
    public void testOfCollection() {
        List<Double> list = Arrays.asList(1.0, 2.0, 3.0);
        DoubleStream stream = createDoubleStream(list);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testOfIterator() {
        DoubleIterator iter = DoubleIterator.of(new double[] { 1.0, 2.0, 3.0 });
        DoubleStream stream = createDoubleStream(iter);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.toArray(), 0.0001);

        // Test with null
        DoubleStream emptyStream = createDoubleStream((DoubleIterator) null);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testOfDoubleBuffer() {
        DoubleBuffer buffer = DoubleBuffer.wrap(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 });
        buffer.position(1);
        buffer.limit(4);

        DoubleStream stream = createDoubleStream(buffer);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, stream.toArray(), 0.0001);

        // Test with null
        DoubleStream emptyStream = createDoubleStream((DoubleBuffer) null);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testOfOptionalDouble() {
        OptionalDouble op = OptionalDouble.of(5.0);
        DoubleStream stream = createDoubleStream(op);
        assertArrayEquals(new double[] { 5.0 }, stream.toArray(), 0.0001);

        OptionalDouble empty = OptionalDouble.empty();
        DoubleStream emptyStream = createDoubleStream(empty);
        assertEquals(0, emptyStream.count());

        // Test with null
        DoubleStream nullStream = createDoubleStream((OptionalDouble) null);
        assertEquals(0, nullStream.count());
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
    public void testFlatten2D() {
        double[][] array = { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0 } };
        DoubleStream stream = DoubleStream.flatten(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, stream.toArray(), 0.0001);

        // Test vertical flatten
        DoubleStream verticalStream = DoubleStream.flatten(array, true);
        assertArrayEquals(new double[] { 1.0, 3.0, 5.0, 2.0, 4.0 }, verticalStream.toArray(), 0.0001);

        // Test with alignment value
        double[][] jaggedArray = { { 1.0, 2.0 }, { 3.0 }, { 4.0, 5.0, 6.0 } };
        DoubleStream alignedStream = DoubleStream.flatten(jaggedArray, 0.0, false);
        assertArrayEquals(new double[] { 1.0, 2.0, 0.0, 3.0, 0.0, 0.0, 4.0, 5.0, 6.0 }, alignedStream.toArray(), 0.0001);
    }

    @Test
    public void testFlatten3D() {
        double[][][] array = { { { 1.0, 2.0 }, { 3.0 } }, { { 4.0 }, { 5.0, 6.0 } } };
        DoubleStream stream = DoubleStream.flatten(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testRepeat() {
        DoubleStream stream = DoubleStream.repeat(7.5, 3);
        assertArrayEquals(new double[] { 7.5, 7.5, 7.5 }, stream.toArray(), 0.0001);

        // Test with 0 repetitions
        DoubleStream emptyStream = DoubleStream.repeat(7.5, 0);
        assertEquals(0, emptyStream.count());

        // Test with large repetitions
        DoubleStream largeStream = DoubleStream.repeat(1.0, 100);
        assertEquals(100, largeStream.count());
    }

    @Test
    public void testRandom() {
        DoubleStream stream = DoubleStream.random();
        double[] values = stream.limit(5).toArray();
        assertEquals(5, values.length);

        // All values should be between 0.0 and 1.0
        for (double value : values) {
            assertTrue(value >= 0.0 && value < 1.0);
        }
    }

    @Test
    public void testIterateWithHasNext() {
        AtomicInteger counter = new AtomicInteger(0);
        BooleanSupplier hasNext = () -> counter.get() < 3;
        DoubleSupplier next = () -> counter.getAndIncrement() * 2.0;

        DoubleStream stream = DoubleStream.iterate(hasNext, next);
        assertArrayEquals(new double[] { 0.0, 2.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testIterateWithInitAndHasNext() {
        AtomicInteger counter = new AtomicInteger(0);
        BooleanSupplier hasNext = () -> counter.getAndIncrement() < 3;
        DoubleUnaryOperator f = x -> x + 2.0;

        DoubleStream stream = DoubleStream.iterate(10.0, hasNext, f);
        double[] result = stream.toArray();
        assertEquals(3, result.length);
        assertEquals(10.0, result[0], 0.0001);
    }

    @Test
    public void testIterateWithPredicate() {
        DoublePredicate hasNext = x -> x < 10.0;
        DoubleUnaryOperator f = x -> x + 3.0;

        DoubleStream stream = DoubleStream.iterate(1.0, hasNext, f);
        assertArrayEquals(new double[] { 1.0, 4.0, 7.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testIterateInfinite() {
        DoubleUnaryOperator f = x -> x * 2.0;
        DoubleStream stream = DoubleStream.iterate(1.0, f);
        double[] result = stream.limit(5).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 4.0, 8.0, 16.0 }, result, 0.0001);
    }

    @Test
    public void testGenerate() {
        AtomicInteger counter = new AtomicInteger(1);
        DoubleSupplier s = () -> counter.getAndIncrement() * 1.5;

        DoubleStream stream = DoubleStream.generate(s);
        double[] result = stream.limit(4).toArray();
        assertArrayEquals(new double[] { 1.5, 3.0, 4.5, 6.0 }, result, 0.0001);
    }

    @Test
    public void testConcatArrays() {
        double[] a1 = { 1.0, 2.0 };
        double[] a2 = { 3.0, 4.0 };
        double[] a3 = { 5.0 };

        DoubleStream stream = DoubleStream.concat(a1, a2, a3);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, stream.toArray(), 0.0001);

        // Test empty
        DoubleStream emptyStream = DoubleStream.concat(new double[0][0]);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testConcatIterators() {
        DoubleIterator iter1 = DoubleIterator.of(new double[] { 1.0, 2.0 });
        DoubleIterator iter2 = DoubleIterator.of(new double[] { 3.0, 4.0 });

        DoubleStream stream = DoubleStream.concat(iter1, iter2);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testConcatStreams() {
        DoubleStream s1 = createDoubleStream(1.0, 2.0);
        DoubleStream s2 = createDoubleStream(3.0, 4.0);
        DoubleStream s3 = createDoubleStream(5.0);

        DoubleStream stream = DoubleStream.concat(s1, s2, s3);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testConcatListOfArrays() {
        List<double[]> list = Arrays.asList(new double[] { 1.0, 2.0 }, new double[] { 3.0, 4.0 }, new double[] { 5.0 });

        DoubleStream stream = DoubleStream.concat(list);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testConcatCollectionOfStreams() {
        List<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 2.0), createDoubleStream(3.0, 4.0), createDoubleStream(5.0));

        DoubleStream stream = DoubleStream.concat(streams);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipArrays() {
        double[] a = { 1.0, 2.0, 3.0 };
        double[] b = { 4.0, 5.0, 6.0 };
        DoubleBinaryOperator zipFunction = (x, y) -> x + y;

        DoubleStream stream = DoubleStream.zip(a, b, zipFunction);
        assertArrayEquals(new double[] { 5.0, 7.0, 9.0 }, stream.toArray(), 0.0001);

        // Test with different lengths
        double[] c = { 1.0, 2.0 };
        double[] d = { 3.0, 4.0, 5.0 };
        DoubleStream stream2 = DoubleStream.zip(c, d, zipFunction);
        assertArrayEquals(new double[] { 4.0, 6.0 }, stream2.toArray(), 0.0001);
    }

    @Test
    public void testZipThreeArrays() {
        double[] a = { 1.0, 2.0 };
        double[] b = { 3.0, 4.0 };
        double[] c = { 5.0, 6.0 };
        DoubleTernaryOperator zipFunction = (x, y, z) -> x + y + z;

        DoubleStream stream = DoubleStream.zip(a, b, c, zipFunction);
        assertArrayEquals(new double[] { 9.0, 12.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipIterators() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0, 2.0, 3.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 4.0, 5.0, 6.0 });
        DoubleBinaryOperator zipFunction = (x, y) -> x * y;

        DoubleStream stream = DoubleStream.zip(a, b, zipFunction);
        assertArrayEquals(new double[] { 4.0, 10.0, 18.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipStreams() {
        DoubleStream a = createDoubleStream(1.0, 2.0, 3.0);
        DoubleStream b = createDoubleStream(4.0, 5.0, 6.0);
        DoubleBinaryOperator zipFunction = (x, y) -> x - y;

        DoubleStream stream = DoubleStream.zip(a, b, zipFunction);
        assertArrayEquals(new double[] { -3.0, -3.0, -3.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipWithDefaults() {
        double[] a = { 1.0, 2.0 };
        double[] b = { 3.0, 4.0, 5.0 };
        DoubleBinaryOperator zipFunction = (x, y) -> x + y;

        DoubleStream stream = DoubleStream.zip(a, b, 10.0, 20.0, zipFunction);
        assertArrayEquals(new double[] { 4.0, 6.0, 15.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testMergeArrays() {
        double[] a = { 1.0, 3.0, 5.0 };
        double[] b = { 2.0, 4.0, 6.0 };
        DoubleBiFunction<MergeResult> nextSelector = (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        DoubleStream stream = DoubleStream.merge(a, b, nextSelector);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testMergeIterators() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0, 3.0, 5.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 2.0, 4.0, 6.0 });
        DoubleBiFunction<MergeResult> nextSelector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        DoubleStream stream = DoubleStream.merge(a, b, nextSelector);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testMergeStreams() {
        DoubleStream a = createDoubleStream(1.0, 3.0, 5.0);
        DoubleStream b = createDoubleStream(2.0, 4.0, 6.0);
        DoubleBiFunction<MergeResult> nextSelector = (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        DoubleStream stream = DoubleStream.merge(a, b, nextSelector);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testMergeCollectionOfStreams() {
        List<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 4.0, 7.0), createDoubleStream(2.0, 5.0, 8.0), createDoubleStream(3.0, 6.0, 9.0));
        DoubleBiFunction<MergeResult> nextSelector = (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        DoubleStream stream = DoubleStream.merge(streams, nextSelector);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testDeferWithNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.defer(null));
    }

    @Test
    public void testIterateWithNullHasNext() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.iterate(null, () -> 1.0));
    }

    @Test
    public void testIterateWithNullNext() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.iterate(() -> true, null));
    }

    @Test
    public void testGenerateWithNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.generate(null));
    }

    @Test
    public void testRepeatWithNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.repeat(1.0, -1));
    }

    @Test
    @DisplayName("flatten() should flatten 2D array")
    public void testFlatten2D_2() {
        double[][] array = { { 1.0d, 2.0d }, { 3.0d, 4.0d } };
        double[] result = DoubleStream.flatten(array, false).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d }, result);

        result = DoubleStream.flatten(array, true).toArray();
        assertArrayEquals(new double[] { 1.0d, 3.0d, 2.0d, 4.0d }, result);
    }

    @Test
    @DisplayName("flatten() should flatten 2D array")
    public void testFlatten2D_3() {
        double[][] array = { { 1.0d, 2.0d }, { 3.0d, 4.0d, 5.f } };
        double[] result = DoubleStream.flatten(array, 0.0d, false).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 0.0d, 3.0d, 4.0d, 5.0d }, result);

        result = DoubleStream.flatten(array, 0.0d, true).toArray();
        assertArrayEquals(new double[] { 1.0d, 3.0d, 2.0d, 4.0d, 0.0d, 5.0d }, result);
    }

    @Test
    @DisplayName("flatten() with empty array should return empty stream")
    public void testFlattenEmpty() {
        double[][] array = {};
        DoubleStream stream = DoubleStream.flatten(array);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRepeat_02() {
        DoubleStream stream = DoubleStream.repeat(1.0d, 1000);
        assertEquals(1000, stream.mapToFloat(e -> (float) e).count());
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
    public void test_concat_02() {
        double[] a = { 1.0d, 2.0d, 3.0d };
        double[] b = { 4.0d, 5.0d, 6.0d };
        double[] result = DoubleStream.concat(a, b).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d }, result);

        result = DoubleStream.concat(N.asList(a, b)).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d }, result);

        result = DoubleStream.concatIterators(N.asList(DoubleIterator.of(a), DoubleIterator.of(b))).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d }, result);
    }

    @Test
    public void test_zip_02() {
        double[] a = { 1.0d };
        double[] b = { 4.0d, 5.0d };
        double[] c = { 7.0d, 8.0d, 9.0d };
        double[] result = DoubleStream.zip(a, b, c, 100, 200, 300, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new double[] { 12.0d, 113.0d, 309.0d }, result);
    }

    @Test
    public void test_merge() {
        double[] a = { 1.0d, 2.0d, 3.0d };
        double[] b = { 4.0d, 5.0d, 6.0d };
        double[] c = { 7.0d, 8.0d, 9.0d };
        double[] result = DoubleStream.merge(N.asList(DoubleStream.of(a)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d }, result);

        result = DoubleStream.merge(N.asList(DoubleStream.of(a), DoubleStream.of(b)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d }, result);

        result = DoubleStream.merge(N.asList(DoubleStream.of(a), DoubleStream.of(b), DoubleStream.of(c)),
                        (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d, 7.0d, 8.0d, 9.0d }, result);

    }

}
