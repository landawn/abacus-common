package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalLong;

/** LongStream mapping contracts. */
public class LongStreamMappingTest extends LongStreamTestSupport {

    @Test
    public void testAsDoubleStream() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        double[] result = stream.asDoubleStream().toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testAsDoubleStream_HappyPath() {
        double[] result = LongStream.of(1, 2, 3).asDoubleStream().toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testAsFloatStream() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        float[] result = stream.asFloatStream().toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testAsFloatStream_HappyPath() {
        float[] result = LongStream.of(1, 2, 3).asFloatStream().toArray();
        assertEquals(3, result.length);
        assertEquals(1.0f, result[0], 0.001f);
        assertEquals(2.0f, result[1], 0.001f);
    }

    @Test
    public void testBoxed() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        Stream<Long> boxed = stream.boxed();
        assertEquals(Arrays.asList(1L, 2L, 3L), boxed.toList());
    }

    @Test
    public void testFlatmap() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.flatMapArray(n -> new long[] { n, n + 10 }).toArray();
        assertArrayEquals(new long[] { 1L, 11L, 2L, 12L, 3L, 13L }, result);
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
    public void testFlatmapCollection() {
        // multi
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L }, LongStream.of(1L, 2L).flatmap(n -> Arrays.asList(n, n * 10L)).toArray());

        // single
        assertArrayEquals(new long[] { 7L, 9L }, LongStream.of(7L).flatmap(n -> Arrays.asList(n, 9L)).toArray());

        // empty stream
        assertArrayEquals(new long[] {}, LongStream.empty().flatmap(n -> Arrays.asList((Long) n)).toArray());

        // empty collection
        assertArrayEquals(new long[] {}, LongStream.of(1L, 2L).flatmap(n -> java.util.Collections.<Long> emptyList()).toArray());

        // null collection
        assertArrayEquals(new long[] {}, LongStream.of(1L).flatmap(n -> (java.util.Collection<Long>) null).toArray());

        // null elements -> 0L
        assertArrayEquals(new long[] { 0L, 5L, 0L }, LongStream.of(1L).flatmap(n -> Arrays.asList((Long) null, 5L, (Long) null)).toArray());

        assertEquals(4, LongStream.of(1L, 2L).flatmap(n -> Arrays.asList((Long) null, n)).count());
    }

    @Test
    public void testFlatmap_HappyPath2() {
        long[] result = LongStream.of(1, 2, 3).flatMapArray(i -> new long[] { i, i * 10 }).toArray();
        assertArrayEquals(new long[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testFlatmapArray() {
        assertEquals(9, LongStream.of(1, 2, 3).flatMapArray(e -> new long[] { e, e + 10, e + 20 }).count());
        assertEquals(8, LongStream.of(1, 2, 3).flatMapArray(e -> new long[] { e, e + 10, e + 20 }).skip(1).count());
        assertArrayEquals(new long[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 }, LongStream.of(1, 2, 3).flatMapArray(e -> new long[] { e, e + 10, e + 20 }).toArray());
        assertArrayEquals(new long[] { 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).flatMapArray(e -> new long[] { e, e + 10, e + 20 }).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).flatMapArray(e -> new long[] { e, e + 10, e + 20 }).toList());
        assertEquals(Arrays.asList(11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).flatMapArray(e -> new long[] { e, e + 10, e + 20 }).skip(1).toList());
        assertEquals(9, LongStream.of(1, 2, 3).map(e -> e).flatMapArray(e -> new long[] { e, e + 10, e + 20 }).count());
        assertEquals(8, LongStream.of(1, 2, 3).map(e -> e).flatMapArray(e -> new long[] { e, e + 10, e + 20 }).skip(1).count());
        assertArrayEquals(new long[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flatMapArray(e -> new long[] { e, e + 10, e + 20 }).toArray());
        assertArrayEquals(new long[] { 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flatMapArray(e -> new long[] { e, e + 10, e + 20 }).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).map(e -> e).flatMapArray(e -> new long[] { e, e + 10, e + 20 }).toList());
        assertEquals(Arrays.asList(11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).map(e -> e).flatMapArray(e -> new long[] { e, e + 10, e + 20 }).skip(1).toList());
    }

    // flatMapArrayToObj

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
    public void testFlatMapArrayToObj_Empty2() {
        List<String> result = LongStream.empty().flatMapArrayToObj(i -> new String[] { String.valueOf(i) }).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatMapArrayToObj_HappyPath() {
        List<String> result = LongStream.of(1, 2, 3).flatMapArrayToObj(i -> new String[] { String.valueOf(i), String.valueOf(i * 10) }).toList();
        assertEquals(Arrays.asList("1", "10", "2", "20", "3", "30"), result);
    }

    @Test
    public void testFlatMapJdkStreamAlias() {
        assertArrayEquals(new long[] { 1, 11, 2, 12 }, LongStream.of(1, 2).flatMapJdkStream(e -> java.util.stream.LongStream.of(e, e + 10)).toArray());
        assertThrows(IllegalArgumentException.class, () -> LongStream.of(1).flatMapJdkStream(null));
    }

    @Test
    public void testFlatMapToDouble() {
        LongStream stream = LongStream.of(1L, 2L);
        double[] result = stream.flatMapToDouble(n -> DoubleStream.of(n, n * 1.5)).toArray();
        assertArrayEquals(new double[] { 1.0, 1.5, 2.0, 3.0 }, result, 0.001);
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
    public void testFlatMapToFloat_HappyPath() {
        float[] result = LongStream.of(1, 2).flatMapToFloat(l -> FloatStream.of(l, l * 10)).toArray();
        assertEquals(4, result.length);
        assertEquals(1.0f, result[0], 0.001f);
        assertEquals(10.0f, result[1], 0.001f);
    }

    @Test
    public void testFlatMapToInt() {
        LongStream stream = LongStream.of(1L, 2L);
        int[] result = stream.flatMapToInt(n -> IntStream.of((int) n, (int) (n * 2))).toArray();
        assertArrayEquals(new int[] { 1, 2, 2, 4 }, result);
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
    public void testFlatmapToObj_HappyPath() {
        List<String> result = LongStream.of(1, 2).flatmapToObj(i -> Arrays.asList(String.valueOf(i), String.valueOf(i * 10))).toList();
        assertEquals(Arrays.asList("1", "10", "2", "20"), result);
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

    // mapMulti

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
    public void testMapMulti_Empty() {
        long[] result = LongStream.empty().mapMulti((value, consumer) -> consumer.accept(value)).toArray();
        assertArrayEquals(new long[0], result);
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
    public void testMapPartial() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.mapPartial(n -> n % 2 == 0 ? OptionalLong.of(n * 2) : OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 4L, 8L }, result);
    }

    @Test
    public void testMapPartial_HappyPath() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalLong.of(i * 10) : OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 20, 40 }, result);
    }

    @Test
    public void testMapPartialJdk() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L, 5L);
        long[] result = stream.mapPartialJdk(n -> n > 3L ? java.util.OptionalLong.of(n) : java.util.OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 4L, 5L }, result);
    }

    @Test
    public void testMapPartialJdk_HappyPath() {
        long[] result = LongStream.of(1, 2, 3, 4, 5)
                .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalLong.of(i * 10) : java.util.OptionalLong.empty())
                .toArray();
        assertArrayEquals(new long[] { 20, 40 }, result);
    }

    @Test
    public void testMapToDouble() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        double[] result = stream.mapToDouble(n -> n * 1.5).toArray();
        assertArrayEquals(new double[] { 1.5, 3.0, 4.5 }, result, 0.001);
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
    public void testMapToFloat_HappyPath() {
        float[] result = LongStream.of(1, 2, 3).mapToFloat(l -> (float) l).toArray();
        assertEquals(3, result.length);
        assertEquals(1.0f, result[0], 0.001f);
    }

    @Test
    public void testMapToInt() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        int[] result = stream.mapToInt(n -> (int) n).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
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
    public void testTransform() {
        long[] result = LongStream.of(1L, 2L, 3L).transform(s -> s.map(x -> x + 10)).toArray();
        assertArrayEquals(new long[] { 11L, 12L, 13L }, result);
    }

    @Test
    public void testTransform_HappyPath2() {
        long sum = LongStream.of(1, 2, 3).transform(s -> s.map(x -> x * 2)).sum();
        assertEquals(12, sum);
    }

    // transformViaJdkStream

    @Test
    public void testTransformViaJdkStream() {
        long[] result = createLongStream(1L, 2L, 3L).transformViaJdkStream(s -> s.map(n -> n * 2)).toArray();
        assertArrayEquals(new long[] { 2L, 4L, 6L }, result);
    }

    @Test
    public void testTransformViaJdkStream_HappyPath2() {
        long[] result = LongStream.of(1, 2, 3).transformViaJdkStream(s -> s.map(x -> x * 2)).toArray();
        assertArrayEquals(new long[] { 2, 4, 6 }, result);
    }

    @Test
    public void testTransformViaJdkStream_singleArg() {
        long[] result = LongStream.of(1L, 2L, 3L, 4L).transformViaJdkStream(s -> s.map(x -> x * 2).sorted()).toArray();
        assertArrayEquals(new long[] { 2L, 4L, 6L, 8L }, result);
    }

    @Test
    public void testTransformViaJdkStream_TwoArg() {
        long[] result = createLongStream(3L, 1L, 2L).transformViaJdkStream(s -> s.sorted(), false).toArray();
        assertArrayEquals(new long[] { 1, 2, 3 }, result);
    }

    @Test
    public void testTransformViaJdkStream_TwoArg_Deferred() {
        long[] result = createLongStream(3L, 1L, 2L).transformViaJdkStream(s -> s.sorted(), true).toArray();
        assertArrayEquals(new long[] { 1, 2, 3 }, result);
    }

    @Test
    public void testTransformViaJdkStream_withDeferred() {
        long[] result = LongStream.of(1L, 2L, 3L, 4L).transformViaJdkStream(s -> s.map(x -> x * 2).sorted(), false).toArray();
        assertArrayEquals(new long[] { 2L, 4L, 6L, 8L }, result);
    }

    @Test
    public void testTransformViaJdkStreamWithDeferred() {
        long[] result = createLongStream(1L, 2L, 3L).transformViaJdkStream(s -> s.map(n -> n * 2), true).toArray();
        assertArrayEquals(new long[] { 2L, 4L, 6L }, result);
    }

}
