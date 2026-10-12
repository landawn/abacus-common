package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fnn;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u;

/** Stream mapping contracts. */
public class StreamMappingTest extends StreamTestSupport {



    // flatMap

    @Test
    public void testFlatMap() {
        Stream<Integer> stream = Stream.of(1, 2, 3).flatMap(n -> Stream.of(n, n * 10));
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), stream.toList());
    }

    @Test
    public void testFlatMapEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().flatMap(n -> Stream.of(n, n * 10));
        assertEquals(0, stream.count());
    }

    @Test
    public void testFlatMapReturnsEmpty() {
        Stream<Integer> stream = Stream.of(1, 2, 3).flatMap(n -> Stream.<Integer> empty());
        assertEquals(0, stream.count());
    }

    @Test
    public void testFlatmap() {
        List<Integer> result = Stream.of(1, 2).flatmap(x -> Arrays.asList(x, x * 10)).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20), result);
    }

    @Test
    public void testFlatmapCollection() {
        Stream<Integer> stream = Stream.of(1, 2, 3).flatmap(n -> Arrays.asList(n, n * 10));
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), stream.toList());
    }

    @Test
    public void testFlatMapArray() {
        List<Integer> result = Stream.of("1,2", "3,4").flatMapArray(s -> {
            String[] parts = s.split(",");
            Integer[] nums = new Integer[parts.length];
            for (int i = 0; i < parts.length; i++) {
                nums[i] = Integer.parseInt(parts[i]);
            }
            return nums;
        }).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatmapE_Collection() throws Exception {
        List<String> result = Stream.of("a", "bb").flatmapE(s -> Arrays.asList(s, s.toUpperCase())).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("A"));
        assertTrue(result.contains("bb"));
    }

    // flatmapIfNotNull

    @Test
    public void testFlatmapIfNotNull() {
        List<Integer> result = Stream.of("1", null, "2", null, "3")
                .flatmapIfNotNull(s -> Arrays.asList(Integer.parseInt(s), Integer.parseInt(s) * 10))
                .toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void testFlatmapIfNotNullAllNull() {
        List<String> result = Stream.of(null, null, null).flatmapIfNotNull(s -> Arrays.asList((String) s)).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatmapIfNotNullEmpty() {
        List<Integer> result = Stream.<String> empty().flatmapIfNotNull(s -> Arrays.asList(Integer.parseInt(s))).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatmapIfNotNullSingleMapper() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(c -> c).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatmapIfNotNullTwoMappers() {
        List<String> data = Arrays.asList("1,2", null, "3,4");
        List<Integer> result = Stream.of(data, null, Arrays.asList("5,6"))
                .flatmapIfNotNull(list -> list, (String str) -> str == null ? null : Stream.of(str.split(",")).map(Integer::parseInt).toList())
                .toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testFlatMapJdkStream() {
        List<String> result = Stream.of("a", "bb").flatMapJdkStream(s -> java.util.stream.Stream.of(s, s.toUpperCase())).toList();

        assertEquals(Arrays.asList("a", "A", "bb", "BB"), result);
    }

    @Test
    public void testFlatMapJdkStream_EmptyInput() {
        List<String> result = Stream.<String> empty().flatMapJdkStream(s -> java.util.stream.Stream.of(s)).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatmapToByte() {
        byte[] result = Stream.of(new byte[] { 1, 2 }, new byte[] { 3, 4 }).flatMapArrayToByte(b -> b).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatMapToByte() {
        byte[] result = Stream.of(new byte[] { 1, 2 }, new byte[] { 3, 4 }).flatMapToByte(b -> ByteStream.of(b)).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatMapToByteEmpty() {
        byte[] result = Stream.<byte[]> empty().flatMapToByte(b -> ByteStream.of(b)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToByteEmpty() {
        byte[] result = Stream.<byte[]> empty().flatMapArrayToByte(b -> b).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToChar() {
        char[] result = Stream.of("ab", "cd", "ef").flatMapArrayToChar(s -> s.toCharArray()).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, result);
    }

    @Test
    public void testFlatMapToChar() {
        char[] result = Stream.of("ab", "cd").flatMapToChar(s -> CharStream.of(s)).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testFlatMapToCharEmpty() {
        char[] result = Stream.<String> empty().flatMapToChar(s -> CharStream.of(s)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToCharEmpty() {
        char[] result = Stream.<String> empty().flatMapArrayToChar(s -> s.toCharArray()).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToCharFromArray() {
        char[] result = Stream.of("ab", "cd").flatMapArrayToChar(s -> s.toCharArray()).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testFlatMapToDouble() {
        DoubleStream doubleStream = Stream.of(1, 2, 3).flatMapToDouble(n -> DoubleStream.of(n, n * 1.5));
        assertArrayEquals(new double[] { 1, 1.5, 2, 3.0, 3, 4.5 }, doubleStream.toArray(), 0.001);
    }

    @Test
    public void testFlatmapToDoubleArray() {
        double[] result = Stream.of(new double[] { 1.0, 2.0 }, new double[] { 3.0, 4.0 }).flatMapArrayToDouble(arr -> arr).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testFlatmapToDoubleArrayEmpty() {
        double[] result = Stream.<double[]> empty().flatMapArrayToDouble(arr -> arr).toArray();
        assertEquals(0, result.length);
    }

    // flatMapToEntry

    @Test
    public void testFlatMapToEntry() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("c", 3);

        List<Map.Entry<String, Integer>> result = Stream.of(map1, map2).flatMapToEntry(m -> Stream.of(m.entrySet())).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testFlatmapToEntryArray() {
        List<Map.Entry<String, Integer>> result = Stream.of("ab", "cd")
                .flatMapToEntry(s -> Stream.of(new AbstractMap.SimpleEntry<>(s, s.length()), new AbstractMap.SimpleEntry<>(s.toUpperCase(), s.length())))
                .toList();
        assertEquals(4, result.size());
        assertEquals("ab", result.get(0).getKey());
        assertEquals(Integer.valueOf(2), result.get(0).getValue());
    }

    @Test
    public void testFlatmapToEntryArrayEmpty() {
        List<Map.Entry<String, Integer>> result = Stream.<String> empty().flatMapToEntry(s -> Stream.of(new AbstractMap.SimpleEntry<>(s, s.length()))).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatMapToEntryEmpty() {
        List<Map.Entry<String, Integer>> result = Stream.<Map<String, Integer>> empty().flatMapToEntry(m -> Stream.of(m.entrySet())).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatMapToEntryFromStream() {
        List<Map.Entry<String, Integer>> result = Stream.of("a:1,b:2", "c:3")
                .flatMapToEntry(str -> Stream.of(str.split(",")).map(pairStr -> N.newEntry(pairStr.split(":")[0], Integer.parseInt(pairStr.split(":")[1]))))
                .toList();
        assertEquals(Arrays.asList(N.newEntry("a", 1), N.newEntry("b", 2), N.newEntry("c", 3)), result);

    }

    @Test
    public void testFlatmapToEntry() {
        List<Map.Entry<String, Integer>> result = Stream.of("a", "b").flatmapToEntry(s -> {
            Map<String, Integer> map = new HashMap<>();
            map.put(s, s.length());
            return map;
        }).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testFlatmapToEntryFromMap() {
        Map<String, Integer> map1 = N.asMap("a", 1, "b", 2);
        Map<String, Integer> map2 = N.asMap("c", 3);
        List<Map.Entry<String, Integer>> result = Stream.of(map1, map2).flatmapToEntry(Fn.identity()).sortedByKey(Comparator.naturalOrder()).toList();
        assertEquals(Arrays.asList(N.newEntry("a", 1), N.newEntry("b", 2), N.newEntry("c", 3)), result);
    }

    @Test
    public void testFlatmapToFloat() {
        float[] result = Stream.of(new float[] { 1.0f, 2.0f }, new float[] { 3.0f, 4.0f }).flatMapArrayToFloat(f -> f).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testFlatMapToFloat() {
        float[] result = Stream.of(new float[] { 1.0f, 2.0f }, new float[] { 3.0f, 4.0f }).flatMapToFloat(f -> FloatStream.of(f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testFlatMapToFloatEmpty() {
        float[] result = Stream.<float[]> empty().flatMapToFloat(f -> FloatStream.of(f)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToFloatEmpty() {
        float[] result = Stream.<float[]> empty().flatMapArrayToFloat(f -> f).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatMapToInt() {
        IntStream intStream = Stream.of("a", "bb", "ccc").flatMapToInt(s -> IntStream.of(s.length(), s.length() * 2));
        assertArrayEquals(new int[] { 1, 2, 2, 4, 3, 6 }, intStream.toArray());
    }

    @Test
    public void testFlatmapToIntArray() {
        int[] result = Stream.of("12", "34")
                .flatMapArrayToInt(s -> new int[] { Character.getNumericValue(s.charAt(0)), Character.getNumericValue(s.charAt(1)) })
                .toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatmapToIntArrayEmpty() {
        int[] result = Stream.<String> empty().flatMapArrayToInt(s -> new int[] { 1, 2 }).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToIntFromArray() {
        int[] result = Stream.of(Pair.of(1, 2), Pair.of(3, 4)).flatMapArrayToInt(p -> new int[] { p.left(), p.right() }).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatMapToLong() {
        LongStream longStream = Stream.of(1, 2, 3).flatMapToLong(n -> LongStream.of(n, n * 10L));
        assertArrayEquals(new long[] { 1, 10, 2, 20, 3, 30 }, longStream.toArray());
    }

    @Test
    public void testFlatmapToLongArray() {
        long[] result = Stream.of(new long[] { 1L, 2L }, new long[] { 3L, 4L }).flatMapArrayToLong(arr -> arr).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testFlatmapToLongArrayEmpty() {
        long[] result = Stream.<long[]> empty().flatMapArrayToLong(arr -> arr).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatMapToPrimitiveStreams() {
        CharStream charStream = createStream01("hello", "world").flatMapToChar(s -> CharStream.of(s.toCharArray()));
        assertEquals("helloworld", new String(charStream.toArray()));

        CharStream charStream2 = createStream01("hello", "world").flatMapArrayToChar(String::toCharArray);
        assertEquals("helloworld", new String(charStream2.toArray()));

    }

    @Test
    public void testFlatmapToShort() {
        short[] result = Stream.of(new short[] { 1, 2 }, new short[] { 3, 4 }).flatMapArrayToShort(s -> s).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatMapToShort() {
        short[] result = Stream.of(new short[] { 1, 2 }, new short[] { 3, 4 }).flatMapToShort(s -> ShortStream.of(s)).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatMapToShortEmpty() {
        short[] result = Stream.<short[]> empty().flatMapToShort(s -> ShortStream.of(s)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToShortEmpty() {
        short[] result = Stream.<short[]> empty().flatMapArrayToShort(s -> s).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMap() {
        Stream<String> stream = Stream.of(1, 2, 3).map(n -> "num" + n);
        assertEquals(Arrays.asList("num1", "num2", "num3"), stream.toList());
    }

    @Test
    public void testMapEmpty() {
        Stream<String> stream = Stream.<Integer> empty().map(n -> "num" + n);
        assertEquals(0, stream.count());
    }

    @Test
    public void testMapE() throws Exception {
        List<Integer> result = Stream.of("1", "2", "3").mapE(Integer::parseInt).toList();

        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testMapE_ErrorPath() {
        assertThrows(NumberFormatException.class, () -> Stream.of("1", "x").mapE(Integer::parseInt).toList());
    }

    @Test
    public void testMapFirst() {
        Stream<Integer> stream = Stream.of(1, 2, 3).mapFirst(n -> n * 10);
        assertEquals(Arrays.asList(10, 2, 3), stream.toList());
    }

    @Test
    public void testMapFirstEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().mapFirst(n -> n * 10);
        assertEquals(0, stream.count());
    }

    @Test
    public void testMapFirstOrElse() {
        List<String> result = Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(n -> "first:" + n, n -> "else:" + n).toList();
        assertEquals(Arrays.asList("first:1", "else:2", "else:3", "else:4", "else:5"), result);
    }

    @Test
    public void testMapFirstOrElseEmpty() {
        List<String> result = Stream.<Integer> empty().mapFirstOrElse(n -> "first:" + n, n -> "else:" + n).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapIfNotNull() {
        Stream<String> stream = Stream.of(1, null, 3).mapIfNotNull(n -> "num" + n);
        assertEquals(Arrays.asList("num1", "num3"), stream.toList());
    }

    @Test
    public void testMapLast() {
        Stream<Integer> stream = Stream.of(1, 2, 3).mapLast(n -> n * 10);
        assertEquals(Arrays.asList(1, 2, 30), stream.toList());
    }

    @Test
    public void testMapLastEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().mapLast(n -> n * 10);
        assertEquals(0, stream.count());
    }

    @Test
    public void testMapLastOrElse() {
        List<String> result = Stream.of(1, 2, 3, 4, 5).mapLastOrElse(n -> "last:" + n, n -> "else:" + n).toList();
        assertEquals(Arrays.asList("else:1", "else:2", "else:3", "else:4", "last:5"), result);
    }

    @Test
    public void testMapLastOrElseEmpty() {
        List<String> result = Stream.<Integer> empty().mapLastOrElse(n -> "last:" + n, n -> "else:" + n).toList();
        assertTrue(result.isEmpty());
    }

    // mapMulti

    @Test
    public void testMapMulti() {
        List<Object> result = Stream.of(1, 2, 3).mapMulti((n, consumer) -> {
            consumer.accept(n);
            consumer.accept(n * 10);
        }).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void testMapMulti_Empty() {
        List<Object> result = Stream.<String> empty().mapMulti((s, consumer) -> {
            consumer.accept(s.toUpperCase());
        }).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapMultiEmpty() {
        List<Object> result = Stream.<Integer> empty().mapMulti((n, consumer) -> {
            consumer.accept(n);
            consumer.accept(n * 10);
        }).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapMultiToDouble() {
        double[] result = Stream.of(1, 2, 3).mapMultiToDouble((n, consumer) -> {
            consumer.accept(n.doubleValue());
            consumer.accept(n.doubleValue() * 0.5);
        }).toArray();
        assertArrayEquals(new double[] { 1.0, 0.5, 2.0, 1.0, 3.0, 1.5 }, result, 0.001);
    }

    @Test
    public void testMapMultiToInt() {
        int[] result = Stream.of("a", "bb", "ccc").mapMultiToInt((s, consumer) -> {
            for (int i = 0; i < s.length(); i++) {
                consumer.accept(s.length());
            }
        }).toArray();
        assertArrayEquals(new int[] { 1, 2, 2, 3, 3, 3 }, result);
    }

    @Test
    public void testMapMultiToLong() {
        long[] result = Stream.of(1, 2, 3).mapMultiToLong((n, consumer) -> {
            consumer.accept(n.longValue());
            consumer.accept(n.longValue() * 100L);
        }).toArray();
        assertArrayEquals(new long[] { 1L, 100L, 2L, 200L, 3L, 300L }, result);
    }

    @Test
    public void testMapPartial() {
        List<Integer> result = Stream.of("1", "abc", "2", "def", "3").mapPartial(s -> {
            try {
                return u.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return u.Optional.empty();
            }
        }).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testMapPartialEmpty() {
        List<Integer> result = Stream.<String> empty().mapPartial(s -> u.Optional.of(Integer.parseInt(s))).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapPartialJdk() {
        List<Integer> result = Stream.of("1", "abc", "2", "def", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testMapPartialToDouble() {
        double[] result = Stream.of("1.5", "abc", "2.5", "3.5").mapPartialToDouble(s -> {
            try {
                return u.OptionalDouble.of(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                return u.OptionalDouble.empty();
            }
        }).toArray();
        assertArrayEquals(new double[] { 1.5, 2.5, 3.5 }, result, 0.001);
    }

    @Test
    public void testMapPartialToDoubleJdk() {
        double[] result = Stream.of("1.5", "abc", "2.5", "3.5").mapPartialToDoubleJdk(s -> {
            try {
                return java.util.OptionalDouble.of(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                return java.util.OptionalDouble.empty();
            }
        }).toArray();
        assertArrayEquals(new double[] { 1.5, 2.5, 3.5 }, result, 0.001);
    }

    @Test
    public void testMapPartialToInt() {
        int[] result = Stream.of("1", "abc", "2", "3").mapPartialToInt(s -> {
            try {
                return u.OptionalInt.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return u.OptionalInt.empty();
            }
        }).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapPartialToIntJdk() {
        int[] result = Stream.of("1", "abc", "2", "3").mapPartialToIntJdk(s -> {
            try {
                return java.util.OptionalInt.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.OptionalInt.empty();
            }
        }).toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapPartialToLong() {
        long[] result = Stream.of("1", "abc", "2", "3").mapPartialToLong(s -> {
            try {
                return u.OptionalLong.of(Long.parseLong(s));
            } catch (NumberFormatException e) {
                return u.OptionalLong.empty();
            }
        }).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testMapPartialToLongJdk() {
        long[] result = Stream.of("1", "abc", "2", "3").mapPartialToLongJdk(s -> {
            try {
                return java.util.OptionalLong.of(Long.parseLong(s));
            } catch (NumberFormatException e) {
                return java.util.OptionalLong.empty();
            }
        }).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testMapToByte() {
        byte[] result = Stream.of(1, 2, 3).mapToByte(Integer::byteValue).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapToByteEmpty() {
        byte[] result = Stream.<Integer> empty().mapToByte(Integer::byteValue).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToChar() {
        char[] result = Stream.of("a", "b", "c").mapToChar(s -> s.charAt(0)).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testMapToCharEmpty() {
        char[] result = Stream.<String> empty().mapToChar(s -> s.charAt(0)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToDouble() {
        DoubleStream doubleStream = Stream.of(1, 2, 3).mapToDouble(n -> n * 1.5);
        assertArrayEquals(new double[] { 1.5, 3.0, 4.5 }, doubleStream.toArray(), 0.001);
    }

    // mapToEntry

    @Test
    public void testMapToEntry() {
        List<Map.Entry<String, Integer>> result = Stream.of("a", "bb", "ccc").mapToEntry(s -> s, s -> s.length()).toList();
        assertEquals(3, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals(Integer.valueOf(1), result.get(0).getValue());
        assertEquals("bb", result.get(1).getKey());
        assertEquals(Integer.valueOf(2), result.get(1).getValue());
    }

    @Test
    public void testMapToEntry2() {
        EntryStream<String, Integer> entryStream2 = stream.mapToEntry(n -> "key" + n, n -> n * 10);
        Map<String, Integer> map2 = entryStream2.toMap();
        assertEquals(5, map2.size());
        assertEquals(Integer.valueOf(30), map2.get("key3"));
    }

    @Test
    public void testMapToEntry_SingleMapper() {
        List<Map.Entry<Integer, String>> result = Stream.of("a", "bb", "ccc").mapToEntry(s -> new AbstractMap.SimpleEntry<>(s.length(), s)).toList();
        assertEquals(3, result.size());
        assertEquals(1, result.get(0).getKey().intValue());
        assertEquals("a", result.get(0).getValue());
    }

    @Test
    public void testMapToEntryEmpty() {
        List<Map.Entry<String, Integer>> result = Stream.<String> empty().mapToEntry(s -> s, s -> s.length()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapToEntryWithFunction() {
        List<Map.Entry<Integer, String>> result = Stream.of(1, 2).mapToEntry(x -> N.newEntry(x, "v" + x)).toList();
        assertEquals(2, result.size());
        assertEquals(N.newEntry(1, "v1"), result.get(0));
        assertEquals(N.newEntry(2, "v2"), result.get(1));
    }

    @Test
    public void testMapToEntryWithKeyAndValueMappers() {
        List<Map.Entry<Integer, String>> result = Stream.of(1, 2).mapToEntry(Fn.identity(), x -> "v" + x).toList();
        assertEquals(2, result.size());
        assertEquals(N.newEntry(1, "v1"), result.get(0));
        assertEquals(N.newEntry(2, "v2"), result.get(1));
    }

    @Test
    public void testMapToFloat() {
        float[] result = Stream.of(1, 2, 3).mapToFloat(Integer::floatValue).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testMapToFloatEmpty() {
        float[] result = Stream.<Integer> empty().mapToFloat(Integer::floatValue).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToInt() {
        IntStream intStream = Stream.of("a", "bb", "ccc").mapToInt(String::length);
        assertArrayEquals(new int[] { 1, 2, 3 }, intStream.toArray());
    }

    @Test
    public void testMapToLong() {
        LongStream longStream = Stream.of(1, 2, 3).mapToLong(n -> n * 10L);
        assertArrayEquals(new long[] { 10, 20, 30 }, longStream.toArray());
    }

    @Test
    public void testMapToPrimitiveStreams() {
        CharStream charStream = createStream01("a", "b", "c").mapToChar(s -> s.charAt(0));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, charStream.toArray());

        ByteStream byteStream = stream.mapToByte(n -> n.byteValue());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, byteStream.toArray());
    }

    @Test
    public void testMapToPrimitiveStreams2() {
        CharStream charStream = createStream01("a", "b", "c").mapToChar(s -> s.charAt(0));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, charStream.toArray());
        LongStream longStream = stream.mapToLong(n -> n.longValue());
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, longStream.toArray());
    }

    @Test
    public void testMapToPrimitiveStreams3() {
        CharStream charStream = createStream01("a", "b", "c").mapToChar(s -> s.charAt(0));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, charStream.toArray());
        DoubleStream doubleStream = stream.mapToDouble(n -> n.doubleValue());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, doubleStream.toArray(), 0.001);
    }

    @Test
    public void testMapToShort() {
        short[] result = Stream.of(1, 2, 3).mapToShort(Integer::shortValue).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapToShortEmpty() {
        short[] result = Stream.<Integer> empty().mapToShort(Integer::shortValue).toArray();
        assertEquals(0, result.length);
    }

    // select

    @Test
    public void testSelect() {
        List<Integer> result = Stream.of(1, "two", 3, "four", 5).select(Integer.class).toList();
        assertEquals(Arrays.asList(1, 3, 5), result);
    }

    @Test
    public void testSelect_Empty() {
        List<Integer> result = Stream.of("a", "b", "c").select(Integer.class).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSelect_Subtype() {
        List<Number> numbers = new ArrayList<>();
        numbers.add(1);
        numbers.add(2L);
        numbers.add(3.0);
        numbers.add(4);

        List<Integer> result = Stream.of(numbers).select(Integer.class).toList();
        assertEquals(Arrays.asList(1, 4), result);
    }

    @Test
    public void testTransform() {
        List<Integer> result = Stream.of(1, 2, 3).transform(s -> s.map(n -> n * 2)).toList();
        assertEquals(Arrays.asList(2, 4, 6), result);
    }

    // transformViaJdkStream

    @Test
    public void testTransformViaJdkStream() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).transformViaJdkStream(jdkStream -> jdkStream.filter(n -> n % 2 == 0)).toList();
        assertEquals(Arrays.asList(2, 4), result);
    }

    @Test
    public void testTransformViaJdkStream_Deferred() {
        // Covers the deferred=true branch in transformViaJdkStream(Function, boolean)
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).transformViaJdkStream(jdkStream -> jdkStream.filter(n -> n % 2 == 0), true).toList();
        assertEquals(Arrays.asList(2, 4), result);
    }

    @Test
    public void testTransformViaJdkStream_NotDeferred() {
        // Covers the deferred=false branch in transformViaJdkStream(Function, boolean)
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).transformViaJdkStream(jdkStream -> jdkStream.map(n -> n * 3), false).toList();
        assertEquals(Arrays.asList(3, 6, 9, 12, 15), result);
    }

    @Test
    public void testTransformViaJdkStreamEmpty() {
        List<Integer> result = Stream.<Integer> empty().transformViaJdkStream(jdkStream -> jdkStream.filter(n -> n > 0)).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTransformViaJdkStreamModeAndDeferral() {
        for (final boolean deferred : new boolean[] { false, true }) {
            final AtomicInteger transferCalls = new AtomicInteger();
            try (final Stream<Integer> result = Stream.of(1, 2).transformViaJdkStream(jdkStream -> {
                transferCalls.incrementAndGet();
                assertFalse(jdkStream.isParallel());
                return jdkStream.parallel().map(value -> value * 2);
            }, deferred)) {
                assertEquals(deferred ? 0 : 1, transferCalls.get());
                assertEquals(!deferred, result.isParallel());
                assertEquals(Arrays.asList(2, 4), result.sorted().toList());
                assertEquals(1, transferCalls.get());
            }
        }
        try (final Stream<Integer> result = Stream.of(1, 2).transformViaJdkStream(java.util.stream.Stream::parallel)) {
            assertTrue(result.isParallel());
        }
    }

    @Test
    public void testTransformViaJdkStreamWithMap() {
        List<Integer> result = Stream.of("a", "bb", "ccc").transformViaJdkStream(jdkStream -> jdkStream.map(String::length)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

}
