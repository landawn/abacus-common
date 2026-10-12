package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;

/** AbstractStream mapping contracts. */
public class AbstractStreamMappingTest extends AbstractStreamTestSupport {

    @Test
    public void test_flatmap() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4)).flatmap(Fn.identity()).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatmap() {
        {
            stream = createStream(1, 2, 3);
            Stream<Integer> result = stream.flatmap(i -> Arrays.asList(i, i * 10));
            assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
        {
            stream = createStream(1, 2, 3);
            Stream<Integer> result = stream.parallel().flatmap(i -> Arrays.asList(i, i * 10));
            assertHaveSameElements(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
        {
            stream = createStream(1, 2, 3).map(e -> e);
            Stream<Integer> result = stream.flatmap(i -> Arrays.asList(i, i * 10));
            assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
        {
            stream = createStream(1, 2, 3).map(e -> e);
            Stream<Integer> result = stream.parallel().flatmap(i -> Arrays.asList(i, i * 10));
            assertHaveSameElements(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
    }

    @Test
    public void test_flatMapArrayToByte() {
        byte[] bytes1 = { 1, 2 };
        byte[] bytes2 = { 3, 4 };
        long count = Stream.of(bytes1, bytes2).flatMapArrayToByte(b -> b).count();
        assertEquals(4L, count);
    }

    @Test
    public void test_flatMapArrayToChar() {
        long count = Stream.of("a", "bc").flatMapArrayToChar(s -> s.toCharArray()).count();
        assertEquals(3L, count);
    }

    @Test
    public void test_flatMapArrayToDouble() {
        double[] doubles1 = { 1.0, 2.0 };
        double[] doubles2 = { 3.0, 4.0 };
        long count = Stream.of(doubles1, doubles2).flatMapArrayToDouble(d -> d).count();
        assertEquals(4L, count);
    }

    @Test
    public void test_flatMapArrayToFloat() {
        float[] floats1 = { 1.0f, 2.0f };
        float[] floats2 = { 3.0f, 4.0f };
        long count = Stream.of(floats1, floats2).flatMapArrayToFloat(f -> f).count();
        assertEquals(4L, count);
    }

    @Test
    public void test_flatMapArrayToInt() {
        int[] ints1 = { 1, 2 };
        int[] ints2 = { 3, 4 };
        long count = Stream.of(ints1, ints2).flatMapArrayToInt(i -> i).count();
        assertEquals(4L, count);
    }

    @Test
    public void test_flatMapArrayToLong() {
        long[] longs1 = { 1L, 2L };
        long[] longs2 = { 3L, 4L };
        long count = Stream.of(longs1, longs2).flatMapArrayToLong(l -> l).count();
        assertEquals(4L, count);
    }

    @Test
    public void test_flatMapArrayToShort() {
        short[] shorts1 = { 1, 2 };
        short[] shorts2 = { 3, 4 };
        long count = Stream.of(shorts1, shorts2).flatMapArrayToShort(s -> s).count();
        assertEquals(4L, count);
    }

    // flatmapIfNotNull

    @Test
    public void testFlatmapIfNotNull() {
        objectStream = createStream("a", null, "b");
        Stream<Character> result = objectStream.flatmapIfNotNull(obj -> obj == null ? null : Arrays.asList(((String) obj).charAt(0)));
        assertEquals(Arrays.asList('a', 'b'), result.toList());
    }

    @Test
    public void test_flatmapIfNotNull_doubleMapper() {
        List<List<List<Integer>>> source = Arrays.asList(Arrays.asList(Arrays.asList(1), null, Arrays.asList(2)), null, Arrays.asList(Arrays.asList(3, 4)));
        List<Integer> result = Stream.of(source).flatmapIfNotNull(Fn.identity(), Fn.identity()).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void test_flatmapIfNotNull_singleMapper() {
        List<List<Integer>> source = Arrays.asList(Arrays.asList(1, 2), null, Arrays.asList(3, 4));
        List<Integer> result = Stream.of(source).flatmapIfNotNull(Fn.identity()).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatmapIfNotNullWithTwoMappers() {
        objectStream = createStream("ab", null, "cd");
        Stream<Character> result = objectStream.flatmapIfNotNull(obj -> obj == null ? null : Arrays.asList(obj.toString()),
                str -> str == null ? null : Arrays.asList(str.charAt(0), str.charAt(1)));
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result.toList());
    }

    @Test
    public void testFlatmapToByte() {
        stringStream = createStream("ab", "cd");
        ByteStream result = stringStream.flatMapArrayToByte(String::getBytes);
        byte[] expected = "abcd".getBytes();
        assertArrayEquals(expected, result.toArray());
    }

    @Test
    public void test_flatmapToByte() {
        stream = createStream(1, 2);
        ByteStream result = stream.flatmapToByte(i -> Arrays.asList(i.byteValue(), (byte) (i * 10)));
        assertArrayEquals(new byte[] { 1, 10, 2, 20 }, result.toArray());
    }

    @Test
    public void testFlatmapToChar() {
        stringStream = createStream("ab", "cd");
        CharStream result = stringStream.flatMapArrayToChar(String::toCharArray);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result.toArray());
    }

    @Test
    public void test_flatmapToChar() {
        stringStream = createStream("ab", "cd");
        CharStream result = stringStream.flatmapToChar(s -> Arrays.asList(s.charAt(0), s.charAt(1)));
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result.toArray());
    }

    @Test
    public void testFlatmapToDouble() {
        stream = createStream(1, 2);
        DoubleStream result = stream.flatMapArrayToDouble(i -> new double[] { i, i * 10.0 });
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0 }, result.toArray(), 0.01);
    }

    @Test
    public void test_flatmapToDouble() {
        stream = createStream(1, 2);
        DoubleStream result = stream.flatmapToDouble(i -> Arrays.asList(i.doubleValue(), i * 10.0));
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0 }, result.toArray(), 0.01);
    }

    @Test
    public void testFlatMapToEntry() {
        stream = createStream(1, 2);
        EntryStream<Integer, String> result = stream
                .flatMapToEntry(i -> Stream.of(new AbstractMap.SimpleEntry<>(i, "a"), new AbstractMap.SimpleEntry<>(i, "b")));
        assertEquals(4, result.toList().size());
    }

    @Test
    public void test_flatMapToEntry_streamMapper() {
        Map<Integer, String> map1 = Map.of(1, "a");
        Map<Integer, String> map2 = Map.of(2, "b");
        List<Map.Entry<Integer, String>> result = Stream.of(map1, map2)
                .flatMapToEntry(m -> Stream.of(m.entrySet()))
                .map(e -> Pair.of(e.getKey(), e.getValue()))
                .toList();
        assertTrue(result.contains(Pair.of(1, "a")));
        assertTrue(result.contains(Pair.of(2, "b")));
    }

    @Test
    public void testFlatmapToEntry() {
        stream = createStream(1, 2);
        EntryStream<String, Integer> result = stream.flatmapToEntry(i -> {
            Map<String, Integer> map = new HashMap<>();
            map.put("a" + i, i);
            map.put("b" + i, i * 10);
            return map;
        });
        Map<String, Integer> map = result.toMap();
        assertEquals(1, (int) map.get("a1"));
        assertEquals(10, (int) map.get("b1"));
    }

    @Test
    public void test_flatmapToEntry_mapMapper() {
        Map<Integer, String> map1 = Map.of(1, "a");
        Map<Integer, String> map2 = Map.of(2, "b");
        List<Map.Entry<Integer, String>> result = Stream.of(map1, map2).flatmapToEntry(m -> m).map(e -> Pair.of(e.getKey(), e.getValue())).toList();
        assertTrue(result.contains(Pair.of(1, "a")));
        assertTrue(result.contains(Pair.of(2, "b")));
    }

    @Test
    public void testFlatmapToFloat() {
        stream = createStream(1, 2);
        FloatStream result = stream.flatMapArrayToFloat(i -> new float[] { i, i * 10f });
        assertArrayEquals(new float[] { 1f, 10f, 2f, 20f }, result.toArray(), 0.01f);
    }

    @Test
    public void test_flatmapToFloat() {
        stream = createStream(1, 2);
        FloatStream result = stream.flatmapToFloat(i -> Arrays.asList(i.floatValue(), i * 10f));
        assertArrayEquals(new float[] { 1f, 10f, 2f, 20f }, result.toArray(), 0.01f);
    }

    @Test
    public void testFlatmapToInt() {
        stream = createStream(1, 2);
        IntStream result = stream.flatMapArrayToInt(i -> new int[] { i, i * 10 });
        assertArrayEquals(new int[] { 1, 10, 2, 20 }, result.toArray());
    }

    @Test
    public void test_flatmapToInt() {
        stream = createStream(1, 2);
        IntStream result = stream.flatmapToInt(i -> Arrays.asList(i, i * 10));
        assertArrayEquals(new int[] { 1, 10, 2, 20 }, result.toArray());
    }

    @Test
    public void testFlatmapToLong() {
        stream = createStream(1, 2);
        LongStream result = stream.flatMapArrayToLong(i -> new long[] { i, i * 10L });
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L }, result.toArray());
    }

    @Test
    public void test_flatmapToLong() {
        stream = createStream(1, 2);
        LongStream result = stream.flatmapToLong(i -> Arrays.asList(i.longValue(), i * 10L));
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L }, result.toArray());
    }

    @Test
    public void testFlatmapToShort() {
        stream = createStream(1, 2);
        ShortStream result = stream.flatMapArrayToShort(i -> new short[] { i.byteValue(), (short) (i * 10) });
        assertArrayEquals(new short[] { 1, 10, 2, 20 }, result.toArray());
    }

    @Test
    public void test_flatmapToShort() {
        stream = createStream(1, 2);
        ShortStream result = stream.flatmapToShort(i -> Arrays.asList(i.shortValue(), (short) (i * 10)));
        assertArrayEquals(new short[] { 1, 10, 2, 20 }, result.toArray());
    }



    // G086-01: mapMulti iterator protocol - repeated hasNext(), next() without hasNext(), null elements, NoSuchElementException at the end
    @Test
    public void testMapMulti_iteratorProtocol() {
        final ObjIterator<String> iter = Stream.of("a", "", "bc").<String> mapMulti((s, consumer) -> {
            for (int i = 0; i < s.length(); i++) {
                consumer.accept(i == 0 ? null : String.valueOf(s.charAt(i)));
            }
        }).iterator();

        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        org.junit.jupiter.api.Assertions.assertNull(iter.next());
        org.junit.jupiter.api.Assertions.assertNull(iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(java.util.NoSuchElementException.class, iter::next);
    }

    @Test
    public void test_mapIfNotNull() {
        List<String> source = Arrays.asList("a", null, "b");
        List<String> result = Stream.of(source).mapIfNotNull(String::toUpperCase).toList();
        assertEquals(Arrays.asList("A", "B"), result);
    }

    @Test
    public void testMapIfNotNull() {
        objectStream = createStream("a", null, "b", null, "c");
        Stream<String> result = objectStream.mapIfNotNull(obj -> obj == null ? null : obj.toString().toUpperCase());
        assertEquals(Arrays.asList("A", "B", "C"), result.toList());
    }

    @Test
    public void test_mapMulti() {
        List<String> result = Stream.of(1, 2).<String> mapMulti((i, consumer) -> {
            consumer.accept("v" + i);
            consumer.accept("v" + (i * 2));
        }).toList();
        assertEquals(Arrays.asList("v1", "v2", "v2", "v4"), result);
    }

    @Test
    public void testMapMulti() {
        stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        });
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
    }



    // G086-01, revised 2026-10-07: elements a mapper emitted before it failed are discarded with that call, as the
    // primitive mapMulti operations do; iteration continues with the next source element.
    @Test
    public void testMapMulti_elementsEmittedBeforeMapperFailureAreDiscarded() {
        final ObjIterator<Integer> iter = Stream.of(1, 2, 3).<Integer> mapMulti((n, consumer) -> {
            consumer.accept(n);

            if (n == 2) {
                throw new IllegalStateException("boom");
            }

            consumer.accept(-n);
        }).iterator();

        assertEquals(1, iter.next());
        assertEquals(-1, iter.next());
        assertThrows(IllegalStateException.class, iter::hasNext);
        assertTrue(iter.hasNext());
        assertEquals(3, iter.next());
        assertEquals(-3, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMapMulti_emitsNullElement() {
        // A mapper emitting a null element must be handled by the sequential path (regression: the
        // old ArrayDeque buffer threw NPE on offer(null); parallel path and JDK mapMulti allow null).
        List<String> result = Stream.of(1, 2).<String> mapMulti((i, consumer) -> {
            consumer.accept(null);
            consumer.accept("v" + i);
        }).toList();
        assertEquals(Arrays.asList(null, "v1", null, "v2"), result);
    }



    // G086-01: mapMulti stays lazy - the source is read only until the buffer holds an element
    @Test
    public void testMapMulti_lazySourceConsumption() {
        final List<Integer> pulled = new ArrayList<>();
        final List<Integer> result = Stream.of(0, 0, 2, 0, 3, 4, 5).peek(pulled::add).<Integer> mapMulti((n, consumer) -> {
            for (int i = 0; i < n; i++) {
                consumer.accept(n);
            }
        }).limit(3).toList();

        assertEquals(Arrays.asList(2, 2, 3), result);
        assertEquals(Arrays.asList(0, 0, 2, 0, 3), pulled);
    }



    // G086-01: mapMulti (sequential buffer) - variable fan-out (0, 1, many), order kept, buffer drained and reused across bursts
    @Test
    public void testMapMulti_variableFanOutAndLargeBurst() {
        final int[] fanOuts = { 0, 1, 3, 0, 2, 1000, 0, 1, 5 };
        final List<Integer> result = Stream.of(0, 1, 3, 0, 2, 1000, 0, 1, 5).<Integer> mapMulti((n, consumer) -> {
            for (int i = 0; i < n; i++) {
                consumer.accept(n * 10000 + i);
            }
        }).toList();

        final List<Integer> expected = new ArrayList<>();

        for (final int n : fanOuts) {
            for (int i = 0; i < n; i++) {
                expected.add(n * 10000 + i);
            }
        }

        assertEquals(expected, result);
    }

    @Test
    public void test_mapMultiToDouble() {
        List<Double> result = Stream.of("1.1", "2.2").mapMultiToDouble((s, consumer) -> consumer.accept(Double.parseDouble(s))).boxed().toList();
        assertEquals(Arrays.asList(1.1, 2.2), result);
    }

    @Test
    public void testMapMultiToDouble() {
        stream = createStream(1, 2, 3);
        DoubleStream result = stream.mapMultiToDouble((i, consumer) -> {
            consumer.accept(i.doubleValue());
            consumer.accept(i * 10.0);
        });
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0 }, result.toArray(), 0.01);
    }

    @Test
    public void test_mapMultiToInt() {
        List<Integer> result = Stream.of("1", "2").mapMultiToInt((s, consumer) -> consumer.accept(Integer.parseInt(s))).boxed().toList();
        assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void testMapMultiToInt() {
        stream = createStream(1, 2, 3);
        IntStream result = stream.mapMultiToInt((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        });
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, result.toArray());
    }

    @Test
    public void test_mapMultiToLong() {
        List<Long> result = Stream.of("1", "2").mapMultiToLong((s, consumer) -> consumer.accept(Long.parseLong(s))).boxed().toList();
        assertEquals(Arrays.asList(1L, 2L), result);
    }

    @Test
    public void testMapMultiToLong() {
        stream = createStream(1, 2, 3);
        LongStream result = stream.mapMultiToLong((i, consumer) -> {
            consumer.accept(i.longValue());
            consumer.accept(i * 10L);
        });
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L }, result.toArray());
    }

    @Test
    public void test_mapPartial() {
        List<String> result = Stream.of(1, 2, 3).mapPartial(i -> i % 2 == 0 ? Optional.of("even") : Optional.empty()).toList();
        assertEquals(List.of("even"), result);
    }

    @Test
    public void testMapPartial() {
        stream = createStream(1, 2, 3, 4);
        Stream<Integer> result = stream.mapPartial(i -> i % 2 == 0 ? Optional.of(i * 10) : Optional.empty());
        assertEquals(Arrays.asList(20, 40), result.toList());
    }

    @Test
    public void testMapPartialJdk() {
        stream = createStream(1, 2, 3, 4);
        Stream<Integer> result = stream.mapPartialJdk(i -> i % 2 == 0 ? java.util.Optional.of(i * 10) : java.util.Optional.empty());
        assertEquals(Arrays.asList(20, 40), result.toList());
    }

    @Test
    public void testMapPartialToDouble() {
        stream = createStream(1, 2, 3, 4);
        DoubleStream result = stream.mapPartialToDouble(i -> i % 2 == 0 ? OptionalDouble.of(i * 10.0) : OptionalDouble.empty());
        assertArrayEquals(new double[] { 20.0, 40.0 }, result.toArray(), 0.01);
    }

    @Test
    public void test_mapPartialToDouble() {
        List<Double> result = Stream.of("1.1", "a", "3.3").mapPartialToDouble(s -> {
            try {
                return OptionalDouble.of(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                return OptionalDouble.empty();
            }
        }).boxed().toList();
        assertEquals(Arrays.asList(1.1, 3.3), result);
    }

    @Test
    public void testMapPartialToDoubleJdk() {
        stream = createStream(1, 2, 3, 4);
        DoubleStream result = stream.mapPartialToDoubleJdk(i -> i % 2 == 0 ? java.util.OptionalDouble.of(i * 10.0) : java.util.OptionalDouble.empty());
        assertArrayEquals(new double[] { 20.0, 40.0 }, result.toArray(), 0.01);
    }

    @Test
    public void testMapPartialToInt() {
        stream = createStream(1, 2, 3, 4);
        IntStream result = stream.mapPartialToInt(i -> i % 2 == 0 ? OptionalInt.of(i * 10) : OptionalInt.empty());
        assertArrayEquals(new int[] { 20, 40 }, result.toArray());
    }

    @Test
    public void test_mapPartialToInt() {
        List<Integer> result = Stream.of("1", "a", "3").mapPartialToInt(s -> {
            try {
                return OptionalInt.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return OptionalInt.empty();
            }
        }).boxed().toList();
        assertEquals(Arrays.asList(1, 3), result);
    }

    @Test
    public void testMapPartialToIntJdk() {
        stream = createStream(1, 2, 3, 4);
        IntStream result = stream.mapPartialToIntJdk(i -> i % 2 == 0 ? java.util.OptionalInt.of(i * 10) : java.util.OptionalInt.empty());
        assertArrayEquals(new int[] { 20, 40 }, result.toArray());
    }

    @Test
    public void testMapPartialToLong() {
        stream = createStream(1, 2, 3, 4);
        LongStream result = stream.mapPartialToLong(i -> i % 2 == 0 ? OptionalLong.of(i * 10L) : OptionalLong.empty());
        assertArrayEquals(new long[] { 20L, 40L }, result.toArray());
    }

    @Test
    public void test_mapPartialToLong() {
        List<Long> result = Stream.of("1", "a", "3").mapPartialToLong(s -> {
            try {
                return OptionalLong.of(Long.parseLong(s));
            } catch (NumberFormatException e) {
                return OptionalLong.empty();
            }
        }).boxed().toList();
        assertEquals(Arrays.asList(1L, 3L), result);
    }

    @Test
    public void testMapPartialToLongJdk() {
        stream = createStream(1, 2, 3, 4);
        LongStream result = stream.mapPartialToLongJdk(i -> i % 2 == 0 ? java.util.OptionalLong.of(i * 10L) : java.util.OptionalLong.empty());
        assertArrayEquals(new long[] { 20L, 40L }, result.toArray());
    }

    // mapToEntry

    @Test
    public void testMapToEntry() {
        stream = createStream(1, 2, 3);
        EntryStream<Integer, String> result = stream.mapToEntry(i -> new AbstractMap.SimpleEntry<>(i, "val" + i));
        Map<Integer, String> map = result.toMap();
        assertEquals("val1", map.get(1));
        assertEquals("val2", map.get(2));
    }

    @Test
    public void test_mapToEntry_withKeyAndValueMappers() {
        List<Map.Entry<Integer, String>> result = Stream.of(1, 2).mapToEntry(i -> i, i -> "v" + i).map(e -> Pair.of(e.getKey(), e.getValue())).toList();
        assertEquals(Arrays.asList(Pair.of(1, "v1"), Pair.of(2, "v2")), result);
    }

    @Test
    public void test_mapToEntry_withMapper() {
        List<Map.Entry<Integer, String>> result = Stream.of(1, 2).mapToEntry(i -> Pair.of(i, "v" + i)).toList();
        assertEquals(Arrays.asList(Pair.of(1, "v1"), Pair.of(2, "v2")), result);
    }

    @Test
    public void testMapToEntryWithKeyValueMappers() {
        stream = createStream(1, 2, 3);
        EntryStream<Integer, String> result = stream.mapToEntry(i -> i * 10, i -> "val" + i);
        Map<Integer, String> map = result.toMap();
        assertEquals("val1", map.get(10));
        assertEquals("val2", map.get(20));
    }

    // select

    @Test
    public void test_select() {
        List<Object> source = Arrays.asList(1, "two", 3.0, 4, "five");
        List<Integer> result = Stream.of(source).select(Integer.class).toList();
        assertEquals(Arrays.asList(1, 4), result);
    }

    @Test
    public void testSelect() {
        objectStream = createStream("string", 123, 45.6, "another");
        Stream<String> selected = objectStream.select(String.class);
        List<String> result = selected.toList();
        assertEquals(Arrays.asList("string", "another"), result);
    }

    @Test
    public void testSelect_NoMatch() {
        Stream<Object> s = Stream.of(1, 2, 3);
        java.util.List<String> result = s.select(String.class).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSelect_Parallel() {
        List<Object> mixed = Arrays.asList(1, "hello", 2, "world", 3.0);
        List<Integer> result = Stream.of(mixed).parallel().select(Integer.class).sorted().toList();
        assertEquals(Arrays.asList(1, 2), result);
    }

}
