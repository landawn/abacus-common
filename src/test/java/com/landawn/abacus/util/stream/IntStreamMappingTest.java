package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalInt;

/** IntStream mapping contracts. */
public class IntStreamMappingTest extends IntStreamTestSupport {

    // asDoubleStream

    @Test
    public void testAsDoubleStream() {
        DoubleStream doubleStream = createIntStream(1, 2, 3).asDoubleStream();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, doubleStream.toArray(), 0.001);
    }

    @Test
    public void testAsDoubleStream_BasicConversion() {
        DoubleStream result = IntStream.of(1, 2, 3).asDoubleStream();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result.toArray());
    }

    @Test
    public void testAsDoubleStream_HappyPath2() {
        double[] result = IntStream.of(1, 2, 3).asDoubleStream().toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testAsFloatStream() {
        float[] result = IntStream.of(1, 2, 3).asFloatStream().toArray();
        assertEquals(3, result.length);
        assertEquals(1.0f, result[0], 0.001f);
        assertEquals(2.0f, result[1], 0.001f);
    }

    @Test
    public void testAsFloatStream_BasicConversion() {
        FloatStream result = IntStream.of(1, 2, 3).asFloatStream();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result.toArray());
    }

    // asLongStream

    @Test
    public void testAsLongStream() {
        LongStream longStream = createIntStream(1, 2, 3).asLongStream();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, longStream.toArray());
    }

    @Test
    public void testAsLongStream_BasicConversion() {
        LongStream result = IntStream.of(1, 2, 3).asLongStream();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result.toArray());
    }

    @Test
    public void testAsLongStream_HappyPath() {
        long[] result = IntStream.of(1, 2, 3).asLongStream().toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testAsXXXStream() {
        {
            LongStream longStream = createIntStream(1, 2, 3).asLongStream();
            assertArrayEquals(new long[] { 1L, 2L, 3L }, longStream.toArray());

            FloatStream floatStream = createIntStream(1, 2, 3).asFloatStream().skip(1);
            assertArrayEquals(new float[] { 2.0f, 3.0f }, floatStream.toArray());

            DoubleStream doubleStream = createIntStream(1, 2, 3).asDoubleStream();
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, doubleStream.toArray(), 0.001);
        }
        {
            LongStream longStream = createIntStream(1, 2, 3).map(e -> e).asLongStream();
            assertArrayEquals(new long[] { 1L, 2L, 3L }, longStream.toArray());

            FloatStream floatStream = createIntStream(1, 2, 3).map(e -> e).asFloatStream().skip(1);
            assertArrayEquals(new float[] { 2.0f, 3.0f }, floatStream.toArray());

            DoubleStream doubleStream = createIntStream(1, 2, 3).map(e -> e).asDoubleStream();
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, doubleStream.toArray(), 0.001);
        }
    }

    // boxed

    @Test
    public void testBoxed() {
        Stream<Integer> boxed = createIntStream(1, 2, 3).boxed();
        assertArrayEquals(new Integer[] { 1, 2, 3 }, boxed.toArray());
    }

    @Test
    public void testBoxed_BasicBoxing() {
        Stream<Integer> result = IntStream.of(1, 2, 3, 4, 5).boxed();
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testBoxed_EmptyStream() {
        Stream<Integer> result = IntStream.empty().boxed();
        assertEquals(0, result.count());
    }

    // flatMap

    @Test
    public void testFlatMap() {
        stream = createIntStream(1, 2, 3).flatMap(n -> createIntStream(n, n * 10));
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, stream.toArray());
    }

    @Test
    public void testFlatMap_BasicFlatMap() {
        stream = IntStream.of(1, 2, 3).flatMap(x -> IntStream.of(x, x * 10));
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, stream.toArray());
    }

    @Test
    public void testFlatMap_EmptyStreams() {
        stream = IntStream.of(1, 2, 3).flatMap(x -> IntStream.empty());
        assertEquals(0, stream.count());
    }

    @Test
    public void testFlatmapCollection() {
        // multi
        assertArrayEquals(new int[] { 1, 10, 2, 20 }, IntStream.of(1, 2).flatmap(i -> Arrays.asList(i, i * 10)).toArray());

        // single
        assertArrayEquals(new int[] { 7, 9 }, IntStream.of(7).flatmap(i -> Arrays.asList(i, 9)).toArray());

        // empty stream
        assertArrayEquals(new int[] {}, IntStream.empty().flatmap(i -> Arrays.asList((Integer) i)).toArray());

        // empty collection
        assertArrayEquals(new int[] {}, IntStream.of(1, 2).flatmap(i -> java.util.Collections.<Integer> emptyList()).toArray());

        // null collection
        assertArrayEquals(new int[] {}, IntStream.of(1).flatmap(i -> (java.util.Collection<Integer>) null).toArray());

        // null elements -> 0
        assertArrayEquals(new int[] { 0, 5, 0 }, IntStream.of(1).flatmap(i -> Arrays.asList((Integer) null, 5, (Integer) null)).toArray());

        assertEquals(4, IntStream.of(1, 2).flatmap(i -> Arrays.asList((Integer) null, i)).count());
    }

    @Test
    public void testFlatmap_HappyPath() {
        int[] result = IntStream.of(1, 2, 3).flatMapArray(i -> new int[] { i, i * 10 }).toArray();
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testFlatmap_WithArrays() {
        stream = IntStream.of(1, 2, 3).flatMapArray(x -> new int[] { x, x * 10 });
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, stream.toArray());
    }

    // flatMapArrayToObj

    @Test
    public void testFlatMapArrayToObj() {
        Stream<String> result = createIntStream(1, 2, 3).flatMapArrayToObj(n -> new String[] { "A" + n, "B" + n });
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" }, result.toArray(String[]::new));
    }

    @Test
    public void testFlatMapArrayToObj_Empty() {
        Stream<String> result = IntStream.empty().flatMapArrayToObj(n -> new String[] { "A" + n });
        assertEquals(0, result.count());
    }

    @Test
    public void testFlatMapArrayToObj_HappyPath() {
        List<String> result = IntStream.of(1, 2, 3).flatMapArrayToObj(i -> new String[] { String.valueOf(i), String.valueOf(i * 10) }).toList();
        assertEquals(Arrays.asList("1", "10", "2", "20", "3", "30"), result);
    }

    @Test
    public void testFlatMapJdkStream() {
        int[] result = IntStream.of(1, 2, 3).flatMapJdkStream(i -> java.util.stream.IntStream.of(i, i * 10)).toArray();
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testFlatMapJdkStream_EmptyInput() {
        int[] result = IntStream.empty().flatMapJdkStream(i -> java.util.stream.IntStream.of(i)).toArray();
        assertArrayEquals(new int[] {}, result);
    }

    @Test
    public void testFlatMapToByte() {
        ByteStream byteStream = createIntStream(1, 2, 3).flatMapToByte(n -> ByteStream.of((byte) n, (byte) (n * 10)));
        assertArrayEquals(new byte[] { 1, 10, 2, 20, 3, 30 }, byteStream.toArray());
    }

    @Test
    public void testFlatMapToByte_BasicFlatMap() {
        ByteStream byteStream = IntStream.of(1, 2).flatMapToByte(x -> ByteStream.of((byte) x, (byte) (x + 1)));
        assertArrayEquals(new byte[] { 1, 2, 2, 3 }, byteStream.toArray());
    }

    @Test
    public void testFlatMapToChar() {
        CharStream charStream = createIntStream(65, 66, 67).flatMapToChar(n -> CharStream.of((char) n, (char) (n + 32)));
        assertArrayEquals(new char[] { 'A', 'a', 'B', 'b', 'C', 'c' }, charStream.toArray());
    }

    @Test
    public void testFlatMapToChar_BasicFlatMap() {
        CharStream charStream = IntStream.of(65, 66).flatMapToChar(x -> CharStream.of((char) x, (char) (x + 1)));
        assertArrayEquals(new char[] { 'A', 'B', 'B', 'C' }, charStream.toArray());
    }

    @Test
    public void testFlatMapToDouble_BasicFlatMap() {
        DoubleStream doubleStream = IntStream.of(1, 2).flatMapToDouble(x -> DoubleStream.of(x, x + 1));
        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 3.0 }, doubleStream.toArray());
    }

    @Test
    public void testFlatMapToFloat() {
        FloatStream floatStream = createIntStream(1, 2, 3).flatMapToFloat(n -> FloatStream.of(n * 0.1f, n * 0.2f));
        assertArrayEquals(new float[] { 0.1f, 0.2f, 0.2f, 0.4f, 0.3f, 0.6f }, floatStream.toArray(), 0.001f);
    }

    @Test
    public void testFlatMapToFloat_BasicFlatMap() {
        FloatStream floatStream = IntStream.of(1, 2).flatMapToFloat(x -> FloatStream.of(x, x + 1));
        assertArrayEquals(new float[] { 1.0f, 2.0f, 2.0f, 3.0f }, floatStream.toArray());
    }

    @Test
    public void testFlatMapToLong_BasicFlatMap() {
        LongStream longStream = IntStream.of(1, 2).flatMapToLong(x -> LongStream.of(x, x + 1));
        assertArrayEquals(new long[] { 1L, 2L, 2L, 3L }, longStream.toArray());
    }

    @Test
    public void testFlatMapToObj_BasicFlatMap() {
        Stream<String> objStream = IntStream.of(1, 2).flatMapToObj(x -> Stream.of("A" + x, "B" + x));
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2" }, objStream.toArray());
    }

    @Test
    public void testFlatmapToObj_HappyPath() {
        List<String> result = IntStream.of(1, 2).flatmapToObj(i -> Arrays.asList(String.valueOf(i), String.valueOf(i * 10))).toList();
        assertEquals(Arrays.asList("1", "10", "2", "20"), result);
    }

    @Test
    public void testFlatmapToObj_WithCollection() {
        Stream<String> objStream = IntStream.of(1, 2).flatmapToObj(x -> Arrays.asList("A" + x, "B" + x));
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2" }, objStream.toArray());
    }

    @Test
    public void testFlatMapToShort() {
        ShortStream shortStream = createIntStream(1, 2, 3).flatMapToShort(n -> ShortStream.of((short) n, (short) (n * 100)));
        assertArrayEquals(new short[] { 1, 100, 2, 200, 3, 300 }, shortStream.toArray());
    }

    @Test
    public void testFlatMapToShort_BasicFlatMap() {
        ShortStream shortStream = IntStream.of(1, 2).flatMapToShort(x -> ShortStream.of((short) x, (short) (x + 1)));
        assertArrayEquals(new short[] { 1, 2, 2, 3 }, shortStream.toArray());
    }

    // map

    @Test
    public void testMap() {
        stream = createIntStream(1, 2, 3).map(n -> n * 2);
        assertArrayEquals(new int[] { 2, 4, 6 }, stream.toArray());
    }

    @Test
    public void testMap_BasicMapping() {
        stream = IntStream.of(1, 2, 3, 4).map(x -> x * 2);
        assertArrayEquals(new int[] { 2, 4, 6, 8 }, stream.toArray());
    }

    @Test
    public void testMap_EmptyStream() {
        stream = IntStream.empty().map(x -> x * 2);
        assertEquals(0, stream.count());
    }

    @Test
    public void map_shouldTransformElements() {
        assertArrayEquals(new int[] { 2, 4, 6 }, IntStream.of(1, 2, 3).map(i -> i * 2).toArray());
    }

    // mapMulti

    @Test
    public void testMapMulti() {
        IntStream stream = createIntStream(1, 2, 3).mapMulti((value, consumer) -> {
            consumer.accept(value);
            consumer.accept(value * 10);
        });
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, stream.toArray());
    }

    @Test
    public void testMapMulti_Empty() {
        int[] result = IntStream.empty().mapMulti((value, consumer) -> consumer.accept(value)).toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testMapMulti_HappyPath() {
        int[] result = IntStream.of(1, 2, 3).mapMulti((value, consumer) -> {
            consumer.accept(value);
            consumer.accept(value * 10);
        }).toArray();
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, result);
    }

    @Test
    public void testMapPartial() {
        IntStream stream = createIntStream(1, 2, 3, 4, 5).mapPartial(n -> n % 2 == 0 ? OptionalInt.of(n * 2) : OptionalInt.empty());
        assertArrayEquals(new int[] { 4, 8 }, stream.toArray());
    }

    @Test
    public void testMapPartial_HappyPath() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalInt.of(i * 10) : OptionalInt.empty()).toArray();
        assertArrayEquals(new int[] { 20, 40 }, result);
    }

    @Test
    public void testMapPartialJdk() {
        IntStream stream = createIntStream(1, 2, 3, 4, 5).mapPartialJdk(n -> n % 2 == 0 ? java.util.OptionalInt.of(n * 2) : java.util.OptionalInt.empty());
        assertArrayEquals(new int[] { 4, 8 }, stream.toArray());
    }

    @Test
    public void testMapPartialJdk_HappyPath() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.of(i * 10) : java.util.OptionalInt.empty()).toArray();
        assertArrayEquals(new int[] { 20, 40 }, result);
    }

    @Test
    public void testMapToByte_BasicMapping() {
        ByteStream byteStream = IntStream.of(1, 2, 3).mapToByte(x -> (byte) x);
        assertArrayEquals(new byte[] { 1, 2, 3 }, byteStream.toArray());
    }

    @Test
    public void testMapToChar_BasicMapping() {
        CharStream charStream = IntStream.of(65, 66, 67).mapToChar(x -> (char) x);
        assertArrayEquals(new char[] { 'A', 'B', 'C' }, charStream.toArray());
    }

    @Test
    public void testMapToDouble() {
        DoubleStream doubleStream = createIntStream(1, 2, 3).mapToDouble(n -> n * 0.5);
        assertArrayEquals(new double[] { 0.5, 1.0, 1.5 }, doubleStream.toArray(), 0.001);
    }

    @Test
    public void testMapToDouble_BasicMapping() {
        DoubleStream doubleStream = IntStream.of(1, 2, 3).mapToDouble(x -> (double) x);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, doubleStream.toArray());
    }

    @Test
    public void testMapToFloat_BasicMapping() {
        FloatStream floatStream = IntStream.of(1, 2, 3).mapToFloat(x -> (float) x);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, floatStream.toArray());
    }

    @Test
    public void testMapToLong() {
        LongStream longStream = createIntStream(1, 2, 3).mapToLong(n -> n * 1000000000L);
        assertArrayEquals(new long[] { 1000000000L, 2000000000L, 3000000000L }, longStream.toArray());
    }

    @Test
    public void testMapToLong_BasicMapping() {
        LongStream longStream = IntStream.of(1, 2, 3).mapToLong(x -> (long) x);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, longStream.toArray());
    }

    @Test
    public void testMapToObj() {
        Stream<String> objStream = createIntStream(1, 2, 3).mapToObj(String::valueOf);
        assertArrayEquals(new String[] { "1", "2", "3" }, objStream.toArray());
    }

    @Test
    public void testMapToObj_BasicMapping() {
        Stream<String> objStream = IntStream.of(1, 2, 3).mapToObj(x -> "Value:" + x);
        assertArrayEquals(new String[] { "Value:1", "Value:2", "Value:3" }, objStream.toArray());
    }

    @Test
    public void testMapToShort_BasicMapping() {
        ShortStream shortStream = IntStream.of(1, 2, 3).mapToShort(x -> (short) x);
        assertArrayEquals(new short[] { 1, 2, 3 }, shortStream.toArray());
    }

    @Test
    public void testTransform() {
        IntStream stream = createIntStream(1, 2, 3, 4, 5);
        int sum = stream.transform(s -> s.filter(n -> n % 2 == 0)).sum();
        assertEquals(6, sum);
    }

    @Test
    public void testTransform_HappyPath() {
        int sum = IntStream.of(1, 2, 3).transform(s -> s.map(x -> x * 2)).sum();
        assertEquals(12, sum);
    }

    // transformViaJdkStream

    @Test
    public void testTransformViaJdkStream() {
        int[] result = createIntStream(3, 1, 2).transformViaJdkStream(s -> s.sorted()).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testTransformViaJdkStream_Deferred() {
        int[] result = createIntStream(3, 1, 2).transformViaJdkStream(s -> s.sorted(), true).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testTransformViaJdkStream_HappyPath2() {
        int[] result = IntStream.of(1, 2, 3).transformViaJdkStream(s -> s.map(x -> x * 2)).toArray();
        assertArrayEquals(new int[] { 2, 4, 6 }, result);
    }

    @Test
    public void testTransformViaJdkStream_NotDeferred() {
        int[] result = createIntStream(3, 1, 2).transformViaJdkStream(s -> s.map(x -> x * 2), false).toArray();
        assertArrayEquals(new int[] { 6, 2, 4 }, result);
    }

    @Test
    public void testTransformViaJdkStream_WithDeferred_False() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).transformViaJdkStream(s -> s.map(x -> x * 3), false).toArray();
        assertArrayEquals(new int[] { 3, 6, 9, 12, 15 }, result);
    }



    // transformViaJdkStream(Function, boolean) - with deferred=true path
    @Test
    public void testTransformViaJdkStream_WithDeferred_True() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).transformViaJdkStream(s -> s.filter(x -> x % 2 == 0), true).toArray();
        assertArrayEquals(new int[] { 2, 4 }, result);
    }

}
