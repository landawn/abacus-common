package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalFloat;

/** FloatStream mapping contracts. */
public class FloatStreamMappingTest extends FloatStreamTestSupport {

    // asDoubleStream

    @Test
    @DisplayName("asDoubleStream() should convert to DoubleStream")
    public void testAsDoubleStream() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.asDoubleStream().toArray(), 0.001);
    }

    @Test
    public void testAsDoubleStream_Empty() {
        double[] result = FloatStream.empty().asDoubleStream().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testAsDoubleStream_Test() {
        double[] result = createFloatStream(1.0f, 2.0f, 3.0f).asDoubleStream().toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, DELTA);
    }

    // boxed

    @Test
    @DisplayName("boxed() should convert to Stream<Float>")
    public void testBoxed() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new Float[] { 1.0f, 2.0f, 3.0f }, stream.boxed().toArray(Float[]::new));
    }

    @Test
    public void testBoxed_Empty() {
        List<Float> result = FloatStream.empty().boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testBoxed_Test() {
        List<Float> result = createFloatStream(1.0f, 2.0f, 3.0f).boxed().toList();
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("flatMap() should flatten streams")
    public void testFlatMap() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 2.0f, 4.0f }, stream.flatMap(x -> FloatStream.of(x, x * 2)).toArray());
    }

    @Test
    public void testFlatMap_Empty() {
        float[] result = FloatStream.empty().flatMap(f -> FloatStream.of(f, f * 2)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapCollection() {
        // multi
        assertArrayEquals(new float[] { 1f, 10f, 2f, 20f }, createFloatStream(1f, 2f).flatmap(x -> Arrays.asList(x, x * 10f)).toArray(), 0.0001f);

        // single
        assertArrayEquals(new float[] { 7f, 9f }, createFloatStream(7f).flatmap(x -> Arrays.asList(x, 9f)).toArray(), 0.0001f);

        // empty stream
        assertArrayEquals(new float[] {}, createFloatStream(new float[] {}).flatmap(x -> Arrays.asList((Float) x)).toArray(), 0.0001f);

        // empty collection
        assertArrayEquals(new float[] {}, createFloatStream(1f, 2f).flatmap(x -> java.util.Collections.<Float> emptyList()).toArray(), 0.0001f);

        // null collection
        assertArrayEquals(new float[] {}, createFloatStream(1f).flatmap(x -> (java.util.Collection<Float>) null).toArray(), 0.0001f);

        // null elements -> 0f
        assertArrayEquals(new float[] { 0f, 5.5f, 0f }, createFloatStream(1f).flatmap(x -> Arrays.asList((Float) null, 5.5f, (Float) null)).toArray(), 0.0001f);

        assertEquals(4, createFloatStream(1f, 2f).flatmap(x -> Arrays.asList((Float) null, x)).count());
    }

    @Test
    @DisplayName("flatMapArray() should flatten arrays")
    public void testFlatmapArray() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 2.0f, 4.0f }, stream.flatMapArray(x -> new float[] { x, x * 2 }).toArray());
    }

    @Test
    public void testFlatMapArrayToObj_Empty() {
        List<String> result = FloatStream.empty().flatMapArrayToObj(f -> new String[] { "A" + f }).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatMapArrayToObj_Test() {
        List<String> result = createFloatStream(1.0f, 2.0f).flatMapArrayToObj(f -> new String[] { "A" + f, "B" + f }).toList();
        assertEquals(4, result.size());
        assertTrue(result.get(0).startsWith("A"));
    }

    @Test
    @DisplayName("flatMapToDouble() should flatten to DoubleStream")
    public void testFlatMapToDouble() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0 }, stream.flatMapToDouble(f -> DoubleStream.of(f, f * 2)).toArray(), 0.001);
    }

    @Test
    public void testFlatMapToDouble_Empty() {
        double[] result = FloatStream.empty().flatMapToDouble(f -> DoubleStream.of((double) f)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("flatMapToInt() should flatten to IntStream")
    public void testFlatMapToInt() {
        FloatStream stream = createFloatStream(1.5f, 2.5f);
        assertArrayEquals(new int[] { 1, 3, 2, 5 }, stream.flatMapToInt(f -> IntStream.of((int) f, (int) (f * 2))).toArray());
    }

    @Test
    public void testFlatMapToInt_Empty() {
        int[] result = FloatStream.empty().flatMapToInt(f -> IntStream.of((int) f)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("flatMapToLong() should flatten to LongStream")
    public void testFlatMapToLong() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new long[] { 1L, 2L, 2L, 4L }, stream.flatMapToLong(f -> LongStream.of((long) f, (long) (f * 2))).toArray());
    }

    @Test
    public void testFlatMapToLong_Empty() {
        long[] result = FloatStream.empty().flatMapToLong(f -> LongStream.of((long) f)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("flatMapToObj() should flatten to object stream")
    public void testFlatMapToObj() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new String[] { "1.0", "2.0", "2.0", "4.0" },
                stream.flatMapToObj(f -> Stream.of(String.valueOf(f), String.valueOf(f * 2))).toArray(String[]::new));
    }

    @Test
    @DisplayName("flatmapToObj() should flatten collection to object stream")
    public void testFlatmapToObj() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new String[] { "1.0", "2.0", "2.0", "4.0" },
                stream.flatmapToObj(f -> Arrays.asList(String.valueOf(f), String.valueOf(f * 2))).toArray(String[]::new));
    }

    @Test
    @DisplayName("map() should transform elements")
    public void testMap() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new float[] { 2.0f, 4.0f, 6.0f }, stream.map(x -> x * 2).toArray());
    }

    @Test
    public void testMap_Empty() {
        float[] result = FloatStream.empty().map(f -> f * 2).toArray();
        assertEquals(0, result.length);
    }

    // mapPartial

    @Test
    @DisplayName("mapPartial() should filter and map partial results")
    public void testMapPartial() {
        FloatStream stream = createFloatStream(1.0f, -2.0f, 3.0f, -4.0f);
        assertArrayEquals(new float[] { 2.0f, 6.0f }, stream.mapPartial(x -> x > 0 ? OptionalFloat.of(x * 2) : OptionalFloat.empty()).toArray());
    }

    @Test
    public void testMapPartial_AllEmpty() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).mapPartial(f -> OptionalFloat.empty()).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapPartial_AllPresent() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).mapPartial(f -> OptionalFloat.of(f * 10)).toArray();
        assertArrayEquals(new float[] { 10.0f, 20.0f, 30.0f }, result, DELTA);
    }

    @Test
    @DisplayName("mapToDouble() should convert to DoubleStream")
    public void testMapToDouble() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.mapToDouble(x -> x).toArray(), 0.001);
    }

    @Test
    public void testMapToDouble_Empty() {
        double[] result = FloatStream.empty().mapToDouble(f -> (double) f).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("mapToInt() should convert to IntStream")
    public void testMapToInt() {
        FloatStream stream = createFloatStream(1.5f, 2.5f, 3.5f);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.mapToInt(x -> (int) x).toArray());
    }

    @Test
    public void testMapToInt_Empty() {
        int[] result = FloatStream.empty().mapToInt(f -> (int) f).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("mapToLong() should convert to LongStream")
    public void testMapToLong() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, stream.mapToLong(x -> (long) x).toArray());
    }

    @Test
    public void testMapToLong_Empty() {
        long[] result = FloatStream.empty().mapToLong(f -> (long) f).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("mapToObj() should convert to object stream")
    public void testMapToObj() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new String[] { "1.0", "2.0", "3.0" }, stream.mapToObj(String::valueOf).toArray(String[]::new));
    }

    @Test
    public void testMapToObj_Empty() {
        List<String> result = FloatStream.empty().mapToObj(String::valueOf).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTransform() {
        FloatStream transformed = FloatStream.of(1f, 2f, 3f, 4f, 5f).transform(s -> s.step(2).map(f -> f * 2));
        assertArrayEquals(new float[] { 2f, 6f, 10f }, transformed.map(f -> f).toArray(), DELTA);
    }

    @Test
    public void testTransformMethod() {
        long count = createFloatStream(1.0f, 2.0f, 3.0f).transform(s -> s.filter(f -> f > 1.0f)).count();
        assertEquals(2, count);
    }

}
