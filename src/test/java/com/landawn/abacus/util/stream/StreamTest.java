package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fnn;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.PermutationIterator;
import com.landawn.abacus.util.Profiler;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ToDoubleFunction;

@Tag("old-test")
public class StreamTest extends AbstractTest {

    @Test
    public void test_exception_parallel() {
        try {
            Stream.range(0, 10).forEach(e -> {
                throw new IOException("oooh");
            });
            fail("Should throw IOException");
        } catch (IOException e) {
        }

        try {
            Stream.range(0, 10).onEachE(e -> {
                throw new IOException("oooh");
            }).forEach(Fn.println());
            fail("Should throw UncheckedIOException");
        } catch (UncheckedIOException e) {
        }

        try {
            Stream.range(0, 10).onEach(e -> {
                throw new UncheckedIOException(new IOException("oooh"));
            }).forEach(Fn.println());
            fail("Should throw UncheckedIOException");
        } catch (UncheckedIOException e) {
        }

        try {
            Stream.range(0, 10).parallel().forEach(e -> {
                throw new IOException("oooh");
            });
            fail("Should throw IOException");
        } catch (IOException e) {
        }

        try {
            Stream.range(0, 10).parallel().onEachE(e -> {
                throw new IOException("oooh");
            }).forEach(Fn.println());
            fail("Should throw UncheckedIOException");
        } catch (UncheckedIOException e) {
        }

        try {
            Stream.range(0, 10).parallel().onEach(e -> {
                throw new UncheckedIOException(new IOException("oooh"));
            }).forEach(Fn.println());
            fail("Should throw UncheckedIOException");
        } catch (UncheckedIOException e) {
        }

    }

    @Test
    public void test_ArrayDeque() {
        final ArrayDeque<Integer> queue = new ArrayDeque<>(10);

        for (int i = 0; i < 16; i++) {
            if (queue.size() > 10) {
                queue.removeFirst();
            }

            queue.add(i);
            N.println(queue);
        }
    }

    @Test
    public void test_iterate() throws Exception {
        final MutableInt cnt = MutableInt.of(0);

        Stream.iterate(1, () -> cnt.getAndIncrement() < 3, x -> x + 3).println();

        Stream.iterate(1, x -> x < 10, x -> x + 3).println();

        Stream.iterate(1, x -> x + 1).limit(10).println();

    }

    @Test
    public void test_asyncExecute() throws Exception {
        try {
            N.asyncExecute(() -> Stream.of(1, 2, 3).forEach(Fnn.throwIOException("oooh"))).get();

            fail("Should throw ExecutionException");
        } catch (final ExecutionException e) {
            e.printStackTrace();
        }

        try {
            Stream.repeat("a", 100).parallel(10).forEach(Fnn.throwIOException("oooh"));

            fail("Should throw IOException");
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_rangeMap() {
        Stream.of("a", "ab", "ac", "b", "c", "cb").rangeMap((a, b) -> b.startsWith(a), (a, b) -> a + "<->" + b).println();
    }

    @Test
    public void test_sliding_3() {

        N.println(Strings.repeat("=", 80));

        IntStream.range(0, 10).boxed().slide(3, 2).skip(0).println();
        IntStream.range(0, 10).boxed().slide(3, 2).skip(1).println();
        IntStream.range(0, 10).boxed().slide(3, 2).skip(2).println();
        IntStream.range(0, 10).boxed().slide(3, 2).skip(7).println();
        IntStream.range(0, 10).boxed().slide(3, 2).skip(8).println();
        IntStream.range(0, 10).boxed().slide(3, 2).skip(9).println();
        IntStream.range(0, 10).boxed().slide(3, 2).skip(10).println();
        IntStream.range(0, 10).boxed().slide(3, 2).skip(11).println();

        N.println(Strings.repeat("=", 80));

        IntStream.range(0, 10).boxed().slide(3, 3).skip(0).println();
        IntStream.range(0, 10).boxed().slide(3, 3).skip(1).println();
        IntStream.range(0, 10).boxed().slide(3, 3).skip(2).println();
        IntStream.range(0, 10).boxed().slide(3, 3).skip(7).println();
        IntStream.range(0, 10).boxed().slide(3, 3).skip(8).println();
        IntStream.range(0, 10).boxed().slide(3, 3).skip(9).println();
        IntStream.range(0, 10).boxed().slide(3, 3).skip(10).println();
        IntStream.range(0, 10).boxed().slide(3, 3).skip(11).println();

        N.println(Strings.repeat("=", 80));

        IntStream.range(0, 10).boxed().slide(3, 4).skip(0).println();
        IntStream.range(0, 10).boxed().slide(3, 4).skip(1).println();
        IntStream.range(0, 10).boxed().slide(3, 4).skip(2).println();
        IntStream.range(0, 10).boxed().slide(3, 4).skip(7).println();
        IntStream.range(0, 10).boxed().slide(3, 4).skip(8).println();
        IntStream.range(0, 10).boxed().slide(3, 4).skip(9).println();
        IntStream.range(0, 10).boxed().slide(3, 4).skip(10).println();
        IntStream.range(0, 10).boxed().slide(3, 4).skip(11).println();
    }

    @Test
    public void test_sliding_4() {

        N.println(Strings.repeat("=", 80));

        IntStream.range(0, 10).boxed().slide(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(0).println();
        IntStream.range(0, 10).boxed().slide(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(1).println();
        IntStream.range(0, 10).boxed().slide(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(2).println();
        IntStream.range(0, 10).boxed().slide(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(7).println();
        IntStream.range(0, 10).boxed().slide(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(8).println();
        IntStream.range(0, 10).boxed().slide(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(9).println();
        IntStream.range(0, 10).boxed().slide(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(10).println();
        IntStream.range(0, 10).boxed().slide(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(11).println();

        N.println(Strings.repeat("=", 80));

        IntStream.range(0, 10).boxed().slide(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(0).println();
        IntStream.range(0, 10).boxed().slide(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(1).println();
        IntStream.range(0, 10).boxed().slide(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(2).println();
        IntStream.range(0, 10).boxed().slide(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(7).println();
        IntStream.range(0, 10).boxed().slide(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(8).println();
        IntStream.range(0, 10).boxed().slide(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(9).println();
        IntStream.range(0, 10).boxed().slide(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(10).println();
        IntStream.range(0, 10).boxed().slide(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(11).println();

        N.println(Strings.repeat("=", 80));

        IntStream.range(0, 10).boxed().slide(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(0).println();
        IntStream.range(0, 10).boxed().slide(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(1).println();
        IntStream.range(0, 10).boxed().slide(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(2).println();
        IntStream.range(0, 10).boxed().slide(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(7).println();
        IntStream.range(0, 10).boxed().slide(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(8).println();
        IntStream.range(0, 10).boxed().slide(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(9).println();
        IntStream.range(0, 10).boxed().slide(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(10).println();
        IntStream.range(0, 10).boxed().slide(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(11).println();
    }

    @Test
    public void test_average() {
        final ToDoubleFunction<String> mapper = String::hashCode;

        Stream.of("a", "b", "c", "a", "d").collect(Collectors.averagingDoubleOrElseThrow(mapper));
    }

    @Test
    public void test_stream_flatten() {
        final int[][] a = { { 1, 3, 5 }, { 2, 4, 6, 8, 10 }, { 10, 20 } };

        IntStream.flatten(a).println();
        IntStream.flatten(a, false).println();
        IntStream.flatten(a, true).println();
        IntStream.flatten(a, 0, false).println();
        IntStream.flatten(a, 0, true).println();

        final int[][][] b = { { { 1, 2, 3 } }, { { 4, 5, 6 } } };
        IntStream.flatten(b).println();

        final Object[][] c = { { 1, 3, 5 }, { 2, 4, 6, 8, 10 }, { 10, 20 } };

        Stream.flatten(c).println();
        Stream.flatten(c, false).println();
        Stream.flatten(c, true).println();
        Stream.flatten(c, 0, false).println();
        Stream.flatten(c, 0, true).println();

        final Object[][][] d = { { { 1, 2, 3 } }, { { 4, 5, 6 } } };
        Stream.flatten(d).println();

        String[][] e = { { "1", "3", "5" }, { "2", "4", "6", "8", "10" }, { "10", "20" } };

        Stream.flatten(e).println();
        Stream.flatten(e, false).println();
        Stream.flatten(e, true).println();
        Stream.flatten(e, 0, false).println();
        Stream.flatten(e, 0, true).println();

        String[][][] f = { { { "1", "2", "3" } }, { { "4", "5", "6" } } };
        Stream.flatten(f).println();

        e = null;

        Stream.flatten(e).println();
        Stream.flatten(e, false).println();
        Stream.flatten(e, true).println();
        Stream.flatten(e, 0, false).println();
        Stream.flatten(e, 0, true).println();

        f = null;
        Stream.flatten(f).println();
    }

    @Test
    public void test_skipLast() {
        N.println(Strings.repeat("=", 80));

        IntStream.range(0, 20).boxed().skipLast(0).println();
        IntStream.range(0, 20).boxed().skipLast(10).println();
        IntStream.range(0, 20).boxed().skipLast(19).println();
        IntStream.range(0, 20).boxed().skipLast(20).println();
        IntStream.range(0, 20).boxed().skipLast(21).println();

        N.println(Strings.repeat("=", 80));

        IntStream.range(0, 20).boxed().parallel().skipLast(0).println();
        IntStream.range(0, 20).boxed().parallel().skipLast(10).println();
        IntStream.range(0, 20).boxed().parallel().skipLast(19).println();
        IntStream.range(0, 20).boxed().parallel().skipLast(20).println();
        IntStream.range(0, 20).boxed().parallel().skipLast(21).println();

        N.println(Strings.repeat("=", 80));

    }

    @Test
    public void test_step() {
        IntStream.range(0, 13).step(2).println();
        IntStream.range(0, 13).step(1).println();
        IntStream.range(0, 13).skip(2).limit(7).step(2).println();
        IntStream.range(0, 13).skip(1).limit(7).step(1).println();
    }

    @Test
    public void test_splitAt() {
        for (int j = 0; j < 10; j++) {
            final int[] a = IntList.random(10_000).array();
            int n = a.length / 2;

            for (int i = 0, len = a.length; i < len; i++) {
                if (a[i] == a[n]) {
                    n = i;
                    break;
                }
            }

            final int tmp = a[n];

            Object[] r1 = Stream.of(a).splitAt(n).toArray();
            Object[] r2 = Stream.of(a).parallel().splitAt(value -> value == tmp).toArray();

            assertTrue(N.equals(((Stream) r1[0]).toArray(), ((Stream) r2[0]).toArray()));
            assertTrue(N.equals(((Stream) r1[1]).toArray(), ((Stream) r2[1]).toArray()));

            r1 = IntStream.of(a).boxed().splitAt(n).toArray();
            r2 = IntStream.of(a).boxed().parallel().splitAt(value -> value.intValue() == tmp).toArray();

            assertTrue(N.equals(((Stream<Integer>) r1[0]).toArray(), ((Stream<Integer>) r2[0]).toArray()));

            final Integer[] a1 = ((Stream<Integer>) r1[1]).toArray(value -> new Integer[value]);

            final Integer[] a2 = ((Stream<Integer>) r2[1]).toArray(value -> new Integer[value]);

            N.println("=================");
            N.println(a);
            N.println(tmp);

            N.println("=================");
            N.println(a1);
            N.println(a2);

            assertTrue(N.equals(a1, a2));
        }
    }

    @Test
    public void test_perf() {
        final String[] strs = new String[10_000];
        N.fill(strs, Strings.uuid());

        final int m = 500;
        final Function<String, Long> mapper = str -> {
            long result = 0;
            for (int i = 0; i < m; i++) {
                result += N.sum(str.toCharArray()) + 1;
            }
            return result;
        };

        long tmp = 0;

        for (final String str : strs) {
            tmp += mapper.apply(str);
        }

        final long sum = tmp;

        Profiler.run(1, 10, 1, "For loop", () -> {
            long result = 0;

            for (final String str : strs) {
                result += mapper.apply(str);
            }

            assertEquals(sum, result);
        }).printResult();

        Profiler.run(1, 10, 1, "Abacus sequential", () -> assertEquals(sum, Stream.of(strs).map(mapper).mapToLong(t -> t).sum())).printResult();

        Profiler.run(1, 10, 1, "Abacus parallel", () -> assertEquals(sum, Stream.of(strs).parallel().map(mapper).mapToLong(t -> t).sum())).printResult();
    }

    @Test
    public void test_04() {
        Iterator<List<String>> iter = PermutationIterator.of(N.asList("a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.of(N.asList("a", "b"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.of(N.asList("a", "a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.of(N.asList("a", "b", "c"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.of(N.asList("a", "b", "a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.of(new ArrayList<String>());
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        iter = PermutationIterator.ordered(N.asList("a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.asList("a", "b"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.asList("a", "a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.asList("a", "b", "c"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.asList("a", "b", "a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(new ArrayList<String>());
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        iter = PermutationIterator.of(N.asList("1", "2", "2", "1"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.asList("a", "b", "c"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.asList("1", "2", "2", "1"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.asList("a", "b", "a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        final Consumer<? super List<Character>> consumer = N::println;

        CharStream.rangeClosed('a', 'e').boxed().permutations().forEach(consumer);
    }

    @Test
    public void test_03() {
        assertEquals(Character.MAX_VALUE, CharStream.range(Character.MIN_VALUE, Character.MAX_VALUE).count());
        assertEquals(Character.MAX_VALUE + 1, CharStream.rangeClosed(Character.MIN_VALUE, Character.MAX_VALUE).count());

        assertEquals(Byte.MAX_VALUE * 2 + 1, ByteStream.range(Byte.MIN_VALUE, Byte.MAX_VALUE).count());
        assertEquals(Byte.MAX_VALUE * 2 + 2, ByteStream.rangeClosed(Byte.MIN_VALUE, Byte.MAX_VALUE).count());

        assertEquals(Short.MAX_VALUE * 2 + 1, ShortStream.range(Short.MIN_VALUE, Short.MAX_VALUE).count());
        assertEquals(Short.MAX_VALUE * 2 + 2, ShortStream.rangeClosed(Short.MIN_VALUE, Short.MAX_VALUE).count());

        assertEquals(Character.MAX_VALUE, Array.range(Character.MIN_VALUE, Character.MAX_VALUE).length);
        assertEquals(Character.MAX_VALUE + 1, Array.rangeClosed(Character.MIN_VALUE, Character.MAX_VALUE).length);

        assertEquals(Byte.MAX_VALUE * 2 + 1, Array.range(Byte.MIN_VALUE, Byte.MAX_VALUE).length);
        assertEquals(Byte.MAX_VALUE * 2 + 2, Array.rangeClosed(Byte.MIN_VALUE, Byte.MAX_VALUE).length);

        assertEquals(Short.MAX_VALUE * 2 + 1, Array.range(Short.MIN_VALUE, Short.MAX_VALUE).length);
        assertEquals(Short.MAX_VALUE * 2 + 2, Array.rangeClosed(Short.MIN_VALUE, Short.MAX_VALUE).length);
    }

    @Test
    public void test_02() {
        N.println(0 - Long.MIN_VALUE);
        N.println(-1 - Long.MIN_VALUE);
        N.println(Long.MAX_VALUE);
        N.println(Long.MAX_VALUE * 1D + 10);
        N.println(Double.MAX_VALUE - Long.MAX_VALUE);
        assertFalse((double) Long.MAX_VALUE + 2 - Long.MAX_VALUE > 0);

        assertEquals(Integer.MAX_VALUE, ((int) (Integer.MAX_VALUE + 0.9999999999999999999D)));
        assertEquals(Long.MAX_VALUE, ((long) (Long.MAX_VALUE + 0.999999999999999D)));

        N.println(Integer.MAX_VALUE - Integer.MIN_VALUE);
        N.println(Integer.MAX_VALUE * 1L - Integer.MIN_VALUE);

        double d = 100;
        for (int i = 0; i < 100; i++) {
            N.println(d--);
        }

        N.println(Long.MAX_VALUE - Long.MIN_VALUE);
        N.println(Long.MAX_VALUE);
        N.println(Long.MIN_VALUE);
        assertEquals(Long.MAX_VALUE, (long) ((Long.MAX_VALUE * 1D - Long.MIN_VALUE) + Long.MIN_VALUE));
        N.println(String.format("%s", Long.MAX_VALUE * 1D - Long.MIN_VALUE));
        assertTrue(N.equals(new int[] { 1, 2, 3, 4, 5 }, IntStream.range(1, 6).toArray()));
        assertTrue(N.equals(new int[] { 1, 2, 3, 4, 5 }, IntStream.rangeClosed(1, 5).toArray()));
        assertTrue(N.equals(new int[] { 1, 3, 5 }, IntStream.range(1, 6, 2).toArray()));
        assertTrue(N.equals(new int[] { 1, 3, 5 }, IntStream.rangeClosed(1, 5, 2).toArray()));
        assertTrue(N.equals(new int[] { 1, 1, 1 }, IntStream.repeat(1, 3).toArray()));

        assertTrue(N.equals(Integer.MAX_VALUE, IntStream.range(0, Integer.MAX_VALUE).count()));
        assertTrue(N.equals(Integer.MAX_VALUE + 1L, IntStream.rangeClosed(0, Integer.MAX_VALUE).count()));
        assertTrue(N.equals(Integer.MAX_VALUE, IntStream.repeat(0, Integer.MAX_VALUE).count()));
    }

}
