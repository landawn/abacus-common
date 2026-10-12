package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalDouble;

/** DoubleStream mapping contracts. */
public class DoubleStreamMappingTest extends DoubleStreamTestSupport {

    @Test
    public void testBoxed() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        List<Double> result = stream.boxed().toList();
        assertEquals(3, result.size());
        assertEquals(1.0, result.get(0), 0.0001);
    }

    @Test
    public void testFlatmap() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        double[] result = stream.flatMapArray(x -> new double[] { x, x * 2 }).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0 }, result, 0.0001);
    }

    @Test
    public void testFlatMap() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        double[] result = stream.flatMap(x -> DoubleStream.of(x, x * 2)).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0 }, result, 0.0001);
    }

    @Test
    public void testFlatmapCollection() {
        // multi-element
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0 }, DoubleStream.of(1.0, 2.0).flatmap(d -> Arrays.asList(d, d * 10)).toArray(), 0.0001);

        // single
        assertArrayEquals(new double[] { 7.0, 9.0 }, DoubleStream.of(7.0).flatmap(d -> Arrays.asList(d, 9.0)).toArray(), 0.0001);

        // empty stream
        assertArrayEquals(new double[] {}, DoubleStream.of(new double[] {}).flatmap(d -> Arrays.asList((Double) d)).toArray(), 0.0001);

        // empty collection
        assertArrayEquals(new double[] {}, DoubleStream.of(1.0, 2.0).flatmap(d -> java.util.Collections.<Double> emptyList()).toArray(), 0.0001);

        // null collection
        assertArrayEquals(new double[] {}, DoubleStream.of(1.0).flatmap(d -> (java.util.Collection<Double>) null).toArray(), 0.0001);

        // null elements -> 0d
        assertArrayEquals(new double[] { 0.0, 5.5, 0.0 }, DoubleStream.of(1.0).flatmap(d -> Arrays.asList((Double) null, 5.5, (Double) null)).toArray(),
                0.0001);

        // count with nulls included
        assertEquals(4, DoubleStream.of(1.0, 2.0).flatmap(d -> Arrays.asList((Double) null, d)).count());
    }

    @Test
    public void testFlatmap_HappyPath() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).flatMapArray(d -> new double[] { d, d * 10 }).toArray();
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0 }, result, 0.001);
    }

    // flatMapArrayToObj

    @Test
    public void testFlatMapArrayToObj() {
        List<String> result = createDoubleStream(1.0, 2.0, 3.0).flatMapArrayToObj(d -> new String[] { "A" + d, "B" + d }).toList();
        assertEquals(6, result.size());
        assertEquals("A1.0", result.get(0));
        assertEquals("B1.0", result.get(1));
    }

    @Test
    public void testFlatMapArrayToObj_Empty() {
        List<String> result = DoubleStream.empty().flatMapArrayToObj(d -> new String[] { "A" + d }).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatMapArrayToObj_Empty2() {
        List<String> result = DoubleStream.empty().flatMapArrayToObj(d -> new String[] { String.valueOf(d) }).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatMapArrayToObj_HappyPath2() {
        List<String> result = DoubleStream.of(1.0, 2.0, 3.0).flatMapArrayToObj(d -> new String[] { String.valueOf(d), String.valueOf(d * 10) }).toList();
        assertEquals(6, result.size());
    }

    @Test
    public void testFlatMapJdkStreamAlias() {
        assertArrayEquals(new double[] { 1, 1.5, 2, 2.5 }, DoubleStream.of(1, 2).flatMapJdkStream(e -> java.util.stream.DoubleStream.of(e, e + 0.5)).toArray());
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.of(1).flatMapJdkStream(null));
    }

    @Test
    public void testFlatMapToFloat() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        float[] result = stream.flatMapToFloat(x -> FloatStream.of((float) x, (float) x * 2)).toArray();
        assertEquals(4, result.length);
    }

    @Test
    public void testFlatMapToFloat_HappyPath2() {
        float[] result = DoubleStream.of(1.0, 2.0).flatMapToFloat(d -> FloatStream.of((float) d, (float) (d * 10))).toArray();
        assertEquals(4, result.length);
        assertEquals(1.0f, result[0], 0.001f);
        assertEquals(10.0f, result[1], 0.001f);
    }

    @Test
    public void testFlatMapToInt() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        int[] result = stream.flatMapToInt(x -> IntStream.of((int) x, (int) x * 2)).toArray();
        assertArrayEquals(new int[] { 1, 2, 2, 4 }, result);
    }

    @Test
    public void testFlatMapToInt_HappyPath() {
        int[] result = DoubleStream.of(1.0, 2.0).flatMapToInt(d -> IntStream.of((int) d, (int) (d * 10))).toArray();
        assertArrayEquals(new int[] { 1, 10, 2, 20 }, result);
    }

    @Test
    public void testFlatMapToLong() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        long[] result = stream.flatMapToLong(x -> LongStream.of((long) x, (long) x * 2)).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 2L, 4L }, result);
    }

    @Test
    public void testFlatMapToLong_HappyPath() {
        long[] result = DoubleStream.of(1.0, 2.0).flatMapToLong(d -> LongStream.of((long) d, (long) (d * 10))).toArray();
        assertArrayEquals(new long[] { 1, 10, 2, 20 }, result);
    }

    @Test
    public void testFlatMapToObj() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        List<String> result = stream.flatMapToObj(x -> Stream.of("A" + x, "B" + x)).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testFlatmapToObj() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        List<String> result = stream.flatmapToObj(x -> Arrays.asList("A" + x, "B" + x)).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testFlatmapToObj_HappyPath() {
        List<String> result = DoubleStream.of(1.0, 2.0).flatmapToObj(d -> Arrays.asList(String.valueOf(d), String.valueOf(d * 10))).toList();
        assertEquals(4, result.size());
    }

    // map

    @Test
    public void testMap() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        double[] result = stream.map(x -> x * 2.0).toArray();
        assertArrayEquals(new double[] { 2.0, 4.0, 6.0 }, result, 0.0001);
    }

    @Test
    public void testMapToChar() {
        char[] result = DoubleStream.of(65.0, 66.0, 67.0)
                .mapToObj(d -> (char) (int) d)
                .toList()
                .stream()
                .map(c -> c.toString())
                .reduce("", String::concat)
                .toCharArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testMapWithNull() {
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.of(1.0, 2.0).map(null).count());
    }

    // mapMulti

    @Test
    public void testMapMulti() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        double[] result = stream.mapMulti((x, consumer) -> {
            consumer.accept(x);
            consumer.accept(x * 2);
        }).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0 }, result, 0.0001);
    }

    @Test
    public void testMapMulti_Empty2() {
        double[] result = DoubleStream.empty().mapMulti((value, consumer) -> consumer.accept(value)).toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testMapMulti_HappyPath2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).mapMulti((value, consumer) -> {
            consumer.accept(value);
            consumer.accept(value * 10);
        }).toArray();
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0 }, result, 0.001);
    }

    @Test
    public void testMapPartial() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        double[] result = stream.mapPartial(x -> x > 2.0 ? OptionalDouble.of(x * 2) : OptionalDouble.empty()).toArray();
        assertArrayEquals(new double[] { 6.0, 8.0 }, result, 0.0001);
    }

    @Test
    public void testMapPartial_HappyPath2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).mapPartial(d -> d % 2 == 0 ? OptionalDouble.of(d * 10) : OptionalDouble.empty()).toArray();
        assertArrayEquals(new double[] { 20.0, 40.0 }, result, 0.001);
    }

    @Test
    public void testMapPartialJdk() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        double[] result = stream.mapPartialJdk(x -> x > 2.0 ? java.util.OptionalDouble.of(x * 2) : java.util.OptionalDouble.empty()).toArray();
        assertArrayEquals(new double[] { 6.0, 8.0 }, result, 0.0001);
    }

    @Test
    public void testMapPartialJdk_HappyPath2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0)
                .mapPartialJdk(d -> d % 2 == 0 ? java.util.OptionalDouble.of(d * 10) : java.util.OptionalDouble.empty())
                .toArray();
        assertArrayEquals(new double[] { 20.0, 40.0 }, result, 0.001);
    }

    @Test
    public void testMapToFloat() {
        DoubleStream stream = DoubleStream.of(1.5, 2.7, 3.9);
        float[] result = stream.mapToFloat(x -> (float) x).toArray();
        assertEquals(3, result.length);
        assertEquals(1.5f, result[0], 0.0001f);
    }

    @Test
    public void testMapToFloat_HappyPath2() {
        float[] result = DoubleStream.of(1.0, 2.0, 3.0).mapToFloat(d -> (float) d).toArray();
        assertEquals(3, result.length);
        assertEquals(1.0f, result[0], 0.001f);
    }

    @Test
    public void testMapToInt() {
        DoubleStream stream = DoubleStream.of(1.5, 2.7, 3.9);
        int[] result = stream.mapToInt(x -> (int) x).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapToLong() {
        DoubleStream stream = DoubleStream.of(1.5, 2.7, 3.9);
        long[] result = stream.mapToLong(x -> (long) x).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testMapToObj() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        List<String> result = stream.mapToObj(x -> "Value: " + x).toList();
        assertEquals(3, result.size());
        assertEquals("Value: 1.0", result.get(0));
    }

    @Test
    public void testTransform() {
        List<Integer> callOrder = new ArrayList<>();
        DoubleStream stream = createDoubleStream(1, 2, 3).onClose(() -> callOrder.add(1)).onClose(() -> callOrder.add(2)).onClose(() -> callOrder.add(3));

        DoubleStream.defer(() -> stream).close();

        Assertions.assertEquals(Arrays.asList(1, 2, 3), callOrder);
    }

    @Test
    public void testTransform_HappyPath2() {
        double sum = DoubleStream.of(1.0, 2.0, 3.0).transform(s -> s.map(x -> x * 2)).sum();
        assertEquals(12.0, sum, 0.001);
    }

    // transformViaJdkStream

    @Test
    public void testTransformViaJdkStream() {
        DoubleStream stream = DoubleStream.of(3.0, 1.0, 2.0);
        double[] result = stream.transformViaJdkStream(s -> s.sorted()).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);
    }

    @Test
    public void testTransformViaJdkStream_HappyPath2() {
        double[] result = DoubleStream.of(1.0, 2.0, 3.0).transformViaJdkStream(s -> s.map(x -> x * 2)).toArray();
        assertArrayEquals(new double[] { 2.0, 4.0, 6.0 }, result, 0.001);
    }

    @Test
    public void testTransformViaJdkStream_TwoArg() {
        double[] result = createDoubleStream(3.0, 1.0, 2.0).transformViaJdkStream(s -> s.sorted(), false).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testTransformViaJdkStream_TwoArg_Deferred() {
        double[] result = createDoubleStream(3.0, 1.0, 2.0).transformViaJdkStream(s -> s.sorted(), true).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testTransformViaJdkStreamDeferred() {
        DoubleStream stream = DoubleStream.of(3.0, 1.0, 2.0);
        double[] result = stream.transformViaJdkStream(s -> s.sorted(), true).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);
    }

}
