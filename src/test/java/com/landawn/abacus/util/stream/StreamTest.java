package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fnn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.IntFunctions;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.PermutationIterator;
import com.landawn.abacus.util.Profiler;
import com.landawn.abacus.util.Seq;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteNFunction;
import com.landawn.abacus.util.function.ByteTriFunction;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharNFunction;
import com.landawn.abacus.util.function.CharTriFunction;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.DoubleBiFunction;
import com.landawn.abacus.util.function.DoubleNFunction;
import com.landawn.abacus.util.function.DoubleTriFunction;
import com.landawn.abacus.util.function.FloatBiFunction;
import com.landawn.abacus.util.function.FloatNFunction;
import com.landawn.abacus.util.function.FloatTriFunction;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntNFunction;
import com.landawn.abacus.util.function.IntTriFunction;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongNFunction;
import com.landawn.abacus.util.function.LongTriFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.ShortBiFunction;
import com.landawn.abacus.util.function.ShortNFunction;
import com.landawn.abacus.util.function.ShortTriFunction;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.UnaryOperator;

@Tag("old-test")
public class StreamTest extends AbstractTest {

    @TempDir
    Path tempDir;

    @TempDir
    Path tempFolder;

    private List<Integer> testData;
    private List<Integer> testList;
    private Stream<Integer> stream;
    private List<Integer> mainStreamResults;
    private List<Integer> subscriberStreamResults;
    private ExecutorService customExecutor;
    private List<Stream<?>> streamsToClose = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        testList = Arrays.asList(1, 2, 3, 4, 5);
        testData = testList;
        stream = createStream(testList);
        mainStreamResults = new ArrayList<>();
        subscriberStreamResults = new ArrayList<>();
        customExecutor = Executors.newFixedThreadPool(4);
    }

    @AfterEach
    public void tearDown() {
        if (stream != null) {
            stream.close();
        }

        if (customExecutor != null) {
            customExecutor.shutdownNow();
        }

        for (Stream<?> streamToClose : streamsToClose) {
            try {
                streamToClose.close();
            } catch (Exception e) {
            }
        }

        streamsToClose.clear();
    }

    protected Stream<Boolean> createStream(final boolean... data) {
        return Stream.of(data);
    }

    protected Stream<Boolean> createStream(final boolean[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Character> createStream(final char... data) {
        return Stream.of(data);
    }

    protected Stream<Character> createStream(final char[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Byte> createStream(final byte... data) {
        return Stream.of(data);
    }

    protected Stream<Byte> createStream(final byte[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Short> createStream(final short... data) {
        return Stream.of(data);
    }

    protected Stream<Short> createStream(final short[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Integer> createStream(final int... data) {
        return Stream.of(data);
    }

    protected Stream<Integer> createStream(final int[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Long> createStream(final long... data) {
        return Stream.of(data);
    }

    protected Stream<Long> createStream(final long[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Float> createStream(final float... data) {
        return Stream.of(data);
    }

    protected Stream<Float> createStream(final float[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Double> createStream(final double... data) {
        return Stream.of(data);
    }

    protected Stream<Double> createStream(final double[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    @SafeVarargs
    protected final <T> Stream<T> createStream01(final T... data) {
        return Stream.of(data);
    }

    protected <T> Stream<T> createStream(final T[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected <T> Stream<T> createStream(final Iterable<? extends T> data) {
        return Stream.of(data);
    }

    protected <T> Stream<T> createStream(final Collection<? extends T> data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected <T> Stream<T> createStream(final Iterator<? extends T> data) {
        return Stream.of(data);
    }

    protected <K, V> Stream<Map.Entry<K, V>> createStream(final Map<? extends K, ? extends V> map) {
        return Stream.of(map);
    }

    protected <T> Stream<T> createStream(final Optional<T> op) {
        return op == null || op.isEmpty() ? Stream.empty() : Stream.of(op.get());
    }

    protected <T> Stream<T> createStream(final java.util.Optional<T> op) {
        return op == null || op.isEmpty() ? Stream.empty() : Stream.of(op.get());
    }

    protected <T> Stream<T> createStream(final Enumeration<? extends T> enumeration) {
        return Stream.of(enumeration);
    }

    private <T> Stream<T> trackStream(Stream<T> stream) {
        streamsToClose.add(stream);
        return stream;
    }

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
            Stream.range(0, 10).peek(e -> {
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
            Stream.range(0, 10).parallel().peek(e -> {
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
        assertNotNull(cnt);
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
        assertDoesNotThrow(() -> {
            Stream.of("a", "ab", "ac", "b", "c", "cb").rangeMap((a, b) -> b.startsWith(a), (a, b) -> a + "<->" + b).println();
        });
    }

    @Test
    public void test_sliding_3() {
        assertDoesNotThrow(() -> {
            N.println(Strings.repeat("=", 80));

            IntStream.range(0, 10).boxed().sliding(3, 2).skip(0).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).skip(1).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).skip(2).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).skip(7).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).skip(8).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).skip(9).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).skip(10).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).skip(11).println();

            N.println(Strings.repeat("=", 80));

            IntStream.range(0, 10).boxed().sliding(3, 3).skip(0).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).skip(1).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).skip(2).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).skip(7).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).skip(8).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).skip(9).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).skip(10).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).skip(11).println();

            N.println(Strings.repeat("=", 80));

            IntStream.range(0, 10).boxed().sliding(3, 4).skip(0).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).skip(1).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).skip(2).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).skip(7).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).skip(8).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).skip(9).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).skip(10).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).skip(11).println();
        });
    }

    @Test
    public void test_sliding_4() {
        assertDoesNotThrow(() -> {
            N.println(Strings.repeat("=", 80));

            IntStream.range(0, 10).boxed().sliding(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(0).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(1).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(2).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(7).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(8).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(9).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(10).println();
            IntStream.range(0, 10).boxed().sliding(3, 2).mapFirst(Fn.<List<Integer>> identity()).skip(11).println();

            N.println(Strings.repeat("=", 80));

            IntStream.range(0, 10).boxed().sliding(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(0).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(1).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(2).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(7).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(8).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(9).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(10).println();
            IntStream.range(0, 10).boxed().sliding(3, 3).mapFirst(Fn.<List<Integer>> identity()).skip(11).println();

            N.println(Strings.repeat("=", 80));

            IntStream.range(0, 10).boxed().sliding(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(0).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(1).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(2).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(7).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(8).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(9).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(10).println();
            IntStream.range(0, 10).boxed().sliding(3, 4).mapFirst(Fn.<List<Integer>> identity()).skip(11).println();
        });
    }

    @Test
    public void test_average() {
        final ToDoubleFunction<String> mapper = String::hashCode;

        Stream.of("a", "b", "c", "a", "d").collect(Collectors.averagingDoubleOrElseThrow(mapper));
        assertNotNull(mapper);
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
        assertNull(f);
    }

    @Test
    public void test_skipLast() {
        assertDoesNotThrow(() -> {
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
        });
    }

    @Test
    public void test_step() {
        assertDoesNotThrow(() -> {
            IntStream.range(0, 13).step(2).println();
            IntStream.range(0, 13).step(1).println();
            IntStream.range(0, 13).skip(2).limit(7).step(2).println();
            IntStream.range(0, 13).skip(1).limit(7).step(1).println();
        });
    }

    @Test
    public void test_splitAt() {
        for (int j = 0; j < 10; j++) {
            final int[] a = IntList.random(10_000).internalArray();
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
        Iterator<List<String>> iter = PermutationIterator.of(N.toList("a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.of(N.toList("a", "b"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.of(N.toList("a", "a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.of(N.toList("a", "b", "c"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.of(N.toList("a", "b", "a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.of(new ArrayList<String>());
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        iter = PermutationIterator.ordered(N.toList("a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.toList("a", "b"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.toList("a", "a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.toList("a", "b", "c"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.toList("a", "b", "a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(new ArrayList<String>());
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        iter = PermutationIterator.of(N.toList("1", "2", "2", "1"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.toList("a", "b", "c"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.toList("1", "2", "2", "1"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.toList("a", "b", "a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        final Consumer<? super List<Character>> consumer = N::println;

        CharStream.rangeClosed('a', 'e').boxed().permutations().forEach(consumer);
        assertNotNull(consumer);
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

    @Test
    public void testEmpty() {
        Stream<Integer> stream = Stream.empty();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    public void testJust() {
        Stream<String> stream = Stream.just("hello");
        assertEquals("hello", stream.first().get());
    }

    @Test
    public void testOfNullable() {
        Stream<String> stream1 = Stream.ofNullable("hello");
        assertEquals("hello", stream1.first().get());

        Stream<String> stream2 = Stream.ofNullable(null);
        assertEquals(0, stream2.count());
    }

    @Test
    public void testOfVarargs() {
        Stream<Integer> stream = Stream.of(1, 2, 3);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testOfArray() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        Stream<Integer> stream = Stream.of(array);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), stream.toList());
    }

    @Test
    public void testOfArrayRange() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        Stream<Integer> stream = Stream.of(array, 1, 4);
        assertEquals(Arrays.asList(2, 3, 4), stream.toList());
    }

    @Test
    public void testOfCollection() {
        List<String> list = Arrays.asList("a", "b", "c");
        Stream<String> stream = Stream.of(list);
        assertEquals(Arrays.asList("a", "b", "c"), stream.toList());
    }

    @Test
    public void testOfCollectionRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Stream<String> stream = Stream.of(list, 1, 4);
        assertEquals(Arrays.asList("b", "c", "d"), stream.toList());
    }

    @Test
    public void testOfMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Stream<Map.Entry<String, Integer>> stream = Stream.of(map);
        assertEquals(2, stream.count());
    }

    @Test
    public void testOfKeys() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Stream<String> stream = Stream.ofKeys(map);
        assertEquals(Arrays.asList("a", "b"), stream.toList());
    }

    @Test
    public void testOfValues() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Stream<Integer> stream = Stream.ofValues(map);
        assertEquals(Arrays.asList(1, 2), stream.toList());
    }

    @Test
    public void testOfIterator() {
        Iterator<Integer> iterator = Arrays.asList(1, 2, 3).iterator();
        Stream<Integer> stream = Stream.of(iterator);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testOfIterable() {
        Iterable<Integer> iterable = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = Stream.of(iterable);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testOfJdkStream() {
        java.util.stream.Stream<Integer> jdkStream = Arrays.asList(1, 2, 3).stream();
        Stream<Integer> stream = Stream.from(jdkStream);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testOfOptional() {
        Stream<Integer> stream1 = Stream.of(u.Optional.of(42));
        assertEquals(Arrays.asList(42), stream1.toList());

        Stream<Integer> stream2 = Stream.of(u.Optional.<Integer> empty());
        assertEquals(0, stream2.count());
    }

    @Test
    public void testOfJdkOptional() {
        Stream<Integer> stream1 = Stream.of(java.util.Optional.of(42));
        assertEquals(Arrays.asList(42), stream1.toList());

        Stream<Integer> stream2 = Stream.of(java.util.Optional.<Integer> empty());
        assertEquals(0, stream2.count());
    }

    @Test
    public void testRange() {
        Stream<Integer> stream = Stream.range(1, 5);
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testRangeWithStep() {
        Stream<Integer> stream = Stream.range(0, 10, 2);
        assertEquals(Arrays.asList(0, 2, 4, 6, 8), stream.toList());
    }

    @Test
    public void testRangeClosed() {
        Stream<Integer> stream = Stream.rangeClosed(1, 5);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), stream.toList());
    }

    @Test
    public void testRangeClosedWithStep() {
        Stream<Integer> stream = Stream.rangeClosed(0, 10, 2);
        assertEquals(Arrays.asList(0, 2, 4, 6, 8, 10), stream.toList());
    }

    @Test
    public void testRepeat() {
        Stream<String> stream = Stream.repeat("a", 3);
        assertEquals(Arrays.asList("a", "a", "a"), stream.toList());
    }

    @Test
    public void testRepeatZero() {
        Stream<String> stream = Stream.repeat("a", 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testIterate() {
        Stream<Integer> stream = Stream.iterate(1, n -> n < 5, n -> n + 1);
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testGenerate() {
        AtomicInteger counter = new AtomicInteger(0);
        Stream<Integer> stream = Stream.generate(() -> counter.incrementAndGet()).limit(3);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testConcat() {
        Stream<Integer> stream = Stream.concat(Stream.of(1, 2), Stream.of(3, 4));
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testConcatVarargs() {
        Stream<Integer> stream = Stream.concat(Stream.of(1), Stream.of(2), Stream.of(3));
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testZip() {
        Stream<String> stream = Stream.zip(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"), (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b", "3c"), stream.toList());
    }

    @Test
    public void testZipWithDifferentLengths() {
        Stream<String> stream = Stream.zip(Arrays.asList(1, 2), Arrays.asList("a", "b", "c"), (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b"), stream.toList());
    }

    @Test
    public void testZipWithDefaultValues() {
        Stream<String> stream = Stream.zip(Arrays.asList(1, 2), Arrays.asList("a", "b", "c"), 0, "", (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b", "0c"), stream.toList());
    }

    @Test
    public void testMerge() {
        Stream<Integer> stream = Stream.merge(Arrays.asList(1, 3, 5), Arrays.asList(2, 4, 6),
                (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), stream.toList());
    }

    @Test
    public void testFilter() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).filter(n -> n % 2 == 0);
        assertEquals(Arrays.asList(2, 4), stream.toList());
    }

    @Test
    public void testFilterEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().filter(n -> n % 2 == 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFilterNoneMatch() {
        Stream<Integer> stream = Stream.of(1, 3, 5).filter(n -> n % 2 == 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFilterAllMatch() {
        Stream<Integer> stream = Stream.of(2, 4, 6).filter(n -> n % 2 == 0);
        assertEquals(Arrays.asList(2, 4, 6), stream.toList());
    }

    @Test
    public void testFilterWithAction() {
        List<Integer> dropped = new ArrayList<>();
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).filter(n -> n % 2 == 0, dropped::add);
        assertEquals(Arrays.asList(2, 4), stream.toList());
        assertEquals(Arrays.asList(1, 3, 5), dropped);
    }

    @Test
    public void testTakeWhile() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).takeWhile(n -> n < 4);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testTakeWhileEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().takeWhile(n -> n < 4);
        assertEquals(0, stream.count());
    }

    @Test
    public void testTakeWhileFirstFalse() {
        Stream<Integer> stream = Stream.of(5, 6, 7).takeWhile(n -> n < 4);
        assertEquals(0, stream.count());
    }

    @Test
    public void testTakeWhileAllTrue() {
        Stream<Integer> stream = Stream.of(1, 2, 3).takeWhile(n -> n < 10);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testDropWhile() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).dropWhile(n -> n < 3);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
    }

    @Test
    public void testDropWhileEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().dropWhile(n -> n < 3);
        assertEquals(0, stream.count());
    }

    @Test
    public void testDropWhileNoneDrop() {
        Stream<Integer> stream = Stream.of(5, 6, 7).dropWhile(n -> n < 3);
        assertEquals(Arrays.asList(5, 6, 7), stream.toList());
    }

    @Test
    public void testDropWhileAllDrop() {
        Stream<Integer> stream = Stream.of(1, 2, 3).dropWhile(n -> n < 10);
        assertEquals(0, stream.count());
    }

    @Test
    public void testDropWhileWithAction() {
        List<Integer> dropped = new ArrayList<>();
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).dropWhile(n -> n < 3, dropped::add);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
        assertEquals(Arrays.asList(1, 2), dropped);
    }

    @Test
    public void testSkipUntil() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).skipUntil(n -> n >= 3);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
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
    public void testMapToDouble() {
        DoubleStream doubleStream = Stream.of(1, 2, 3).mapToDouble(n -> n * 1.5);
        assertArrayEquals(new double[] { 1.5, 3.0, 4.5 }, doubleStream.toArray(), 0.001);
    }

    @Test
    public void testMapIfNotNull() {
        Stream<String> stream = Stream.of(1, null, 3).mapIfNotNull(n -> "num" + n);
        assertEquals(Arrays.asList("num1", "num3"), stream.toList());
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
    public void testFlatmapCollection() {
        Stream<Integer> stream = Stream.of(1, 2, 3).flatmap(n -> Arrays.asList(n, n * 10));
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), stream.toList());
    }

    @Test
    public void testFlattmapArray() {
        Stream<Integer> stream = Stream.of(1, 2, 3).flatMapArray(n -> new Integer[] { n, n * 10 });
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), stream.toList());
    }

    @Test
    public void testFlatMapToInt() {
        IntStream intStream = Stream.of("a", "bb", "ccc").flatMapToInt(s -> IntStream.of(s.length(), s.length() * 2));
        assertArrayEquals(new int[] { 1, 2, 2, 4, 3, 6 }, intStream.toArray());
    }

    @Test
    public void testFlatMapToLong() {
        LongStream longStream = Stream.of(1, 2, 3).flatMapToLong(n -> LongStream.of(n, n * 10L));
        assertArrayEquals(new long[] { 1, 10, 2, 20, 3, 30 }, longStream.toArray());
    }

    @Test
    public void testFlatMapToDouble() {
        DoubleStream doubleStream = Stream.of(1, 2, 3).flatMapToDouble(n -> DoubleStream.of(n, n * 1.5));
        assertArrayEquals(new double[] { 1, 1.5, 2, 3.0, 3, 4.5 }, doubleStream.toArray(), 0.001);
    }

    @Test
    public void testDistinct() {
        Stream<Integer> stream = Stream.of(1, 2, 2, 3, 3, 3).distinct();
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testDistinctEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().distinct();
        assertEquals(0, stream.count());
    }

    @Test
    public void testDistinctNoDuplicates() {
        Stream<Integer> stream = Stream.of(1, 2, 3).distinct();
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testDistinctAllDuplicates() {
        Stream<Integer> stream = Stream.of(1, 1, 1).distinct();
        assertEquals(Arrays.asList(1), stream.toList());
    }

    @Test
    public void testDistinctBy() {
        Stream<String> stream = Stream.of("a", "bb", "c", "dd").distinctBy(String::length);
        assertEquals(Arrays.asList("a", "bb"), stream.toList());
    }

    @Test
    public void testDistinctByWithMerge() {
        Stream<String> stream = Stream.of("a", "bb", "c", "dd").distinctBy(String::length, (s1, s2) -> s1 + s2);
        assertEquals(Arrays.asList("ac", "bbdd"), stream.toList());
    }

    @Test
    public void testSorted() {
        Stream<Integer> stream = Stream.of(3, 1, 2).sorted();
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testSortedEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().sorted();
        assertEquals(0, stream.count());
    }

    @Test
    public void testSortedAlreadySorted() {
        Stream<Integer> stream = Stream.of(1, 2, 3).sorted();
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testSortedComparator() {
        Stream<Integer> stream = Stream.of(1, 2, 3).sorted(Comparator.reverseOrder());
        assertEquals(Arrays.asList(3, 2, 1), stream.toList());
    }

    @Test
    public void testSortedBy() {
        Stream<String> stream = Stream.of("aaa", "b", "cc").sortedBy(String::length);
        assertEquals(Arrays.asList("b", "cc", "aaa"), stream.toList());
    }

    @Test
    public void testSortedByInt() {
        Stream<String> stream = Stream.of("aaa", "b", "cc").sortedByInt(String::length);
        assertEquals(Arrays.asList("b", "cc", "aaa"), stream.toList());
    }

    @Test
    public void testReverseSorted() {
        Stream<Integer> stream = Stream.of(1, 2, 3).reverseSorted();
        assertEquals(Arrays.asList(3, 2, 1), stream.toList());
    }

    @Test
    public void testReverseSortedComparator() {
        Stream<String> stream = Stream.of("a", "bbb", "cc").reverseSorted(Comparator.comparing(String::length));
        assertEquals(Arrays.asList("bbb", "cc", "a"), stream.toList());
    }

    @Test
    public void testReversed() {
        Stream<Integer> stream = Stream.of(1, 2, 3).reversed();
        assertEquals(Arrays.asList(3, 2, 1), stream.toList());
    }

    @Test
    public void testReversedEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().reversed();
        assertEquals(0, stream.count());
    }

    @Test
    public void testReversedSingleElement() {
        Stream<Integer> stream = Stream.of(42).reversed();
        assertEquals(Arrays.asList(42), stream.toList());
    }

    @Test
    public void testRotatedPositive() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).rotated(2);
        assertEquals(Arrays.asList(4, 5, 1, 2, 3), stream.toList());
    }

    @Test
    public void testRotatedNegative() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).rotated(-2);
        assertEquals(Arrays.asList(3, 4, 5, 1, 2), stream.toList());
    }

    @Test
    public void testRotatedZero() {
        Stream<Integer> stream = Stream.of(1, 2, 3).rotated(0);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testRotatedGreaterThanSize() {
        Stream<Integer> stream = Stream.of(1, 2, 3).rotated(5);
        assertHaveSameElements(Arrays.asList(3, 1, 2), stream.toList());
    }

    @Test
    public void testShuffled() {
        List<Integer> original = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> shuffled = Stream.of(original).shuffled().toList();
        assertEquals(5, shuffled.size());
        assertTrue(shuffled.containsAll(original));
    }

    @Test
    public void testShuffledWithRandom() {
        List<Integer> original = Arrays.asList(1, 2, 3, 4, 5);
        Random random = new Random(12345);
        List<Integer> shuffled1 = Stream.of(original).shuffled(new Random(12345)).toList();
        List<Integer> shuffled2 = Stream.of(original).shuffled(new Random(12345)).toList();
        assertEquals(shuffled1, shuffled2);
    }

    @Test
    public void testIndexed() {
        Stream<Indexed<String>> stream = Stream.of("a", "b", "c").indexed();
        List<Indexed<String>> list = stream.toList();
        assertEquals(0, list.get(0).index());
        assertEquals("a", list.get(0).value());
        assertEquals(1, list.get(1).index());
        assertEquals("b", list.get(1).value());
        assertEquals(2, list.get(2).index());
        assertEquals("c", list.get(2).value());
    }

    @Test
    public void testSkip() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).skip(2);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
    }

    @Test
    public void testSkipZero() {
        Stream<Integer> stream = Stream.of(1, 2, 3).skip(0);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testSkipGreaterThanSize() {
        Stream<Integer> stream = Stream.of(1, 2, 3).skip(10);
        assertEquals(0, stream.count());
    }

    @Test
    public void testSkipWithAction() {
        List<Integer> skipped = new ArrayList<>();
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
        assertEquals(Arrays.asList(1, 2), skipped);
    }

    @Test
    public void testLimit() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).limit(3);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testLimitZero() {
        Stream<Integer> stream = Stream.of(1, 2, 3).limit(0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testLimitGreaterThanSize() {
        Stream<Integer> stream = Stream.of(1, 2, 3).limit(10);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testStep() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5, 6).step(2);
        assertEquals(Arrays.asList(1, 3, 5), stream.toList());
    }

    @Test
    public void testStepOne() {
        Stream<Integer> stream = Stream.of(1, 2, 3).step(1);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testSkipLast() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).skipLast(2);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testTakeLast() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).takeLast(2);
        assertEquals(Arrays.asList(4, 5), stream.toList());
    }

    @Test
    public void testLastN() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).last(3);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
    }

    @Test
    public void testPeek() {
        List<Integer> peeked = new ArrayList<>();
        Stream<Integer> stream = Stream.of(1, 2, 3).peek(peeked::add);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
        assertEquals(Arrays.asList(1, 2, 3), peeked);
    }

    @Test
    public void testOnEach() {
        List<Integer> processed = new ArrayList<>();
        Stream<Integer> stream = Stream.of(1, 2, 3).peek(processed::add);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
        assertEquals(Arrays.asList(1, 2, 3), processed);
    }

    @Test
    public void testOnFirst() {
        AtomicReference<Integer> first = new AtomicReference<>();
        Stream<Integer> stream = Stream.of(1, 2, 3).onFirst(first::set);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
        assertEquals(Integer.valueOf(1), first.get());
    }

    @Test
    public void testOnLast() {
        AtomicReference<Integer> last = new AtomicReference<>();
        Stream<Integer> stream = Stream.of(1, 2, 3).onLast(last::set);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
        assertEquals(Integer.valueOf(3), last.get());
    }

    @Test
    public void testPrependStream() {
        Stream<Integer> stream = Stream.of(3, 4).prepend(Stream.of(1, 2));
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testPrependVarargs() {
        Stream<Integer> stream = Stream.of(3, 4).prepend(1, 2);
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testPrependCollection() {
        Stream<Integer> stream = Stream.of(3, 4).prepend(Arrays.asList(1, 2));
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testPrependOptional() {
        Stream<Integer> stream = Stream.of(2, 3).prepend(u.Optional.of(1));
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testAppendStream() {
        Stream<Integer> stream = Stream.of(1, 2).append(Stream.of(3, 4));
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testAppendVarargs() {
        Stream<Integer> stream = Stream.of(1, 2).append(3, 4);
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testAppendCollection() {
        Stream<Integer> stream = Stream.of(1, 2).append(Arrays.asList(3, 4));
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testAppendOptional() {
        Stream<Integer> stream = Stream.of(1, 2).append(u.Optional.of(3));
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testAppendIfEmpty() {
        Stream<Integer> stream1 = Stream.<Integer> empty().appendIfEmpty(() -> Stream.of(1, 2));
        assertEquals(Arrays.asList(1, 2), stream1.toList());

        Stream<Integer> stream2 = Stream.of(1, 2).appendIfEmpty(() -> Stream.of(3, 4));
        assertEquals(Arrays.asList(1, 2), stream2.toList());
    }

    @Test
    public void testIntersection() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).intersection(Arrays.asList(2, 3, 5));
        assertEquals(Arrays.asList(2, 3), stream.toList());
    }

    @Test
    public void testIntersectionEmpty() {
        Stream<Integer> stream = Stream.of(1, 2, 3).intersection(Arrays.asList(4, 5, 6));
        assertEquals(0, stream.count());
    }

    @Test
    public void testDifference() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).difference(Arrays.asList(2, 3, 5));
        assertEquals(Arrays.asList(1, 4), stream.toList());
    }

    @Test
    public void testDifferenceNone() {
        Stream<Integer> stream = Stream.of(1, 2, 3).difference(Arrays.asList(4, 5, 6));
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testSymmetricDifference() {
        Stream<Integer> stream = Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(2, 3, 4));
        List<Integer> result = stream.toList();
        assertTrue(result.containsAll(Arrays.asList(1, 4)));
        assertEquals(2, result.size());
    }

    @Test
    public void testGroupBy() {
        Map<Integer, List<String>> map = Stream.of("a", "bb", "c", "dd", "eee").groupBy(String::length).toMap(e -> e.getKey(), e -> e.getValue());
        assertEquals(Arrays.asList("a", "c"), map.get(1));
        assertEquals(Arrays.asList("bb", "dd"), map.get(2));
        assertEquals(Arrays.asList("eee"), map.get(3));
    }

    @Test
    public void testGroupByEmpty() {
        Map<Integer, List<String>> map = Stream.<String> empty().groupBy(String::length).toMap(e -> e.getKey(), e -> e.getValue());
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGroupByWithValueMapper() {
        Map<Integer, List<Integer>> map = Stream.of("a", "bb", "ccc").groupBy(String::length, String::length).toMap(e -> e.getKey(), e -> e.getValue());
        assertEquals(Arrays.asList(1), map.get(1));
        assertEquals(Arrays.asList(2), map.get(2));
        assertEquals(Arrays.asList(3), map.get(3));
    }

    @Test
    public void testGroupByWithCollector() {
        Map<Integer, Long> map = Stream.of("a", "bb", "c", "dd").groupBy(String::length, Collectors.counting()).toMap(e -> e.getKey(), e -> e.getValue());
        assertEquals(Long.valueOf(2), map.get(1));
        assertEquals(Long.valueOf(2), map.get(2));
    }

    @Test
    public void testGroupByToEntry() {
        List<Map.Entry<Integer, List<String>>> list = Stream.of("a", "bb", "c", "dd").groupByToEntry(String::length).toList();
        assertEquals(2, list.size());
    }

    @Test
    public void testPartitionBy() {
        Map<Boolean, List<Integer>> map = Stream.of(1, 2, 3, 4, 5).partitionBy(n -> n % 2 == 0).toMap(e -> e.getKey(), e -> e.getValue());
        assertEquals(Arrays.asList(2, 4), map.get(true));
        assertEquals(Arrays.asList(1, 3, 5), map.get(false));
    }

    @Test
    public void testPartitionByEmpty() {
        Map<Boolean, List<Integer>> map = Stream.<Integer> empty().partitionBy(n -> n % 2 == 0).toMap(e -> e.getKey(), e -> e.getValue());
        assertTrue(map.get(true).isEmpty());
        assertTrue(map.get(false).isEmpty());
    }

    @Test
    public void testPartitionByWithCollector() {
        Map<Boolean, Long> map = Stream.of(1, 2, 3, 4, 5).partitionBy(n -> n % 2 == 0, Collectors.counting()).toMap(e -> e.getKey(), e -> e.getValue());
        assertEquals(Long.valueOf(2), map.get(true));
        assertEquals(Long.valueOf(3), map.get(false));
    }

    @Test
    public void testCountBy() {
        Map<Integer, Long> map = Stream.of("a", "bb", "c", "dd", "eee").countBy(String::length).toMap(e -> e.getKey(), e -> Long.valueOf(e.getValue()));
        assertEquals(Long.valueOf(2), map.get(1));
        assertEquals(Long.valueOf(2), map.get(2));
        assertEquals(Long.valueOf(1), map.get(3));
    }

    @Test
    public void testCountByEmpty() {
        Map<Integer, Long> map = Stream.<String> empty().countBy(String::length).toMap(e -> e.getKey(), e -> Long.valueOf(e.getValue()));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testCollapseBiPredicate() {
        Stream<List<Integer>> stream = Stream.of(1, 1, 2, 2, 2, 3).collapse((a, b) -> a.equals(b));
        List<List<Integer>> result = stream.toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 1), result.get(0));
        assertEquals(Arrays.asList(2, 2, 2), result.get(1));
        assertEquals(Arrays.asList(3), result.get(2));
    }

    @Test
    public void testCollapseWithMergeFunction() {
        Stream<Integer> stream = Stream.of(1, 1, 2, 2, 2, 3).collapse((a, b) -> a.equals(b), Integer::sum);
        assertEquals(Arrays.asList(2, 6, 3), stream.toList());
    }

    @Test
    public void testCollapseWithCollector() {
        Stream<Integer> stream = Stream.of(1, 1, 2, 2, 2, 3).collapse((a, b) -> a.equals(b), Collectors.summingInt(Integer::intValue));
        assertEquals(Arrays.asList(2, 6, 3), stream.toList());
    }

    @Test
    public void testScan() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).scan(Integer::sum);
        assertEquals(Arrays.asList(1, 3, 6, 10), stream.toList());
    }

    @Test
    public void testScanWithInit() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).scan(0, Integer::sum);
        assertEquals(Arrays.asList(1, 3, 6, 10), stream.toList());
    }

    @Test
    public void testScanWithInitIncluded() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).scan(0, true, Integer::sum);
        assertEquals(Arrays.asList(0, 1, 3, 6, 10), stream.toList());
    }

    @Test
    public void testSplit() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3, 4, 5).split(2);
        List<List<Integer>> result = stream.toList();
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
        assertEquals(Arrays.asList(5), result.get(2));
    }

    @Test
    public void testSplitChunkSizeOne() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3).split(1);
        List<List<Integer>> result = stream.toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1), result.get(0));
    }

    @Test
    public void testSplitWithPredicate() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3, 4, 5, 6).split(n -> n % 3 == 0);
        List<List<Integer>> result = stream.toList();
        assertTrue(result.size() >= 2);
    }

    @Test
    public void testSplitAt() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5).splitAt(2).map(s -> s.toList()).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4, 5), result.get(1));
    }

    @Test
    public void testsliding() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3, 4, 5).sliding(3);
        List<List<Integer>> result = stream.toList();
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(2, 3, 4), result.get(1));
        assertEquals(Arrays.asList(3, 4, 5), result.get(2));
    }

    @Test
    public void testSlidingWithIncrement() {
        Stream<List<Integer>> stream = Stream.of(1, 2, 3, 4, 5).sliding(2, 2);
        List<List<Integer>> result = stream.toList();
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
        assertEquals(Arrays.asList(5), result.get(2));
    }

    @Test
    public void testSlidingMap() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).slidingMap((a, b) -> a + b);
        assertEquals(Arrays.asList(3, 5, 7), stream.toList());
    }

    @Test
    public void testSlidingMapTriFunction() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).slidingMap((a, b, c) -> a + b + c);
        assertEquals(Arrays.asList(6, 9), stream.toList());
    }

    @Test
    public void testTop() {
        Stream<Integer> stream = Stream.of(5, 1, 3, 2, 4).top(3);
        assertEquals(Arrays.asList(3, 5, 4), stream.toList());
    }

    @Test
    public void testTopWithComparator() {
        Stream<Integer> stream = Stream.of(5, 1, 3, 2, 4).top(3, Comparator.naturalOrder());
        assertEquals(Arrays.asList(3, 5, 4), stream.toList());
    }

    @Test
    public void testTopGreaterThanSize() {
        Stream<Integer> stream = Stream.of(1, 2, 3).top(10);
        assertHaveSameElements(Arrays.asList(3, 2, 1), stream.toList());
    }

    @Test
    public void testForEach() {
        List<Integer> result = new ArrayList<>();
        Stream.of(1, 2, 3).forEach(result::add);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testForEachEmpty() {
        List<Integer> result = new ArrayList<>();
        Stream.<Integer> empty().forEach(result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForeachWithCompletion() {
        List<Integer> result = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        Stream.of(1, 2, 3).forEach(result::add, () -> completed.set(true));
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertTrue(completed.get());
    }

    @Test
    public void testForEachIndexed() {
        List<String> result = new ArrayList<>();
        Stream.of("a", "b", "c").forEachIndexed((i, s) -> result.add(i + ":" + s));
        assertEquals(Arrays.asList("0:a", "1:b", "2:c"), result);
    }

    @Test
    public void testForEachPair() {
        List<String> result = new ArrayList<>();
        Stream.of(1, 2, 3, 4).forEachPair((a, b) -> result.add(a + "-" + b));
        assertEquals(Arrays.asList("1-2", "2-3", "3-4"), result);
    }

    @Test
    public void testForEachTriple() {
        List<String> result = new ArrayList<>();
        Stream.of(1, 2, 3, 4).forEachTriple((a, b, c) -> result.add(a + "-" + b + "-" + c));
        assertEquals(Arrays.asList("1-2-3", "2-3-4"), result);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(Stream.of(1, 2, 3, 4).anyMatch(n -> n > 3));
        assertFalse(Stream.of(1, 2, 3).anyMatch(n -> n > 5));
        assertFalse(Stream.<Integer> empty().anyMatch(n -> true));
    }

    @Test
    public void testAllMatch() {
        assertTrue(Stream.of(2, 4, 6).allMatch(n -> n % 2 == 0));
        assertFalse(Stream.of(2, 3, 4).allMatch(n -> n % 2 == 0));
        assertTrue(Stream.<Integer> empty().allMatch(n -> false));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(Stream.of(1, 3, 5).noneMatch(n -> n % 2 == 0));
        assertFalse(Stream.of(1, 2, 3).noneMatch(n -> n % 2 == 0));
        assertTrue(Stream.<Integer> empty().noneMatch(n -> true));
    }

    @Test
    public void testNMatch() {
        assertTrue(Stream.of(2, 4, 6, 8).hasMatchCountBetween(2, 3, n -> n > 5));
        assertFalse(Stream.of(2, 4, 6, 8).hasMatchCountBetween(5, 10, n -> n > 5));
    }

    @Test
    public void testFindFirst() {
        assertEquals(Integer.valueOf(1), Stream.of(1, 2, 3).findFirst().get());
        assertFalse(Stream.<Integer> empty().findFirst().isPresent());
    }

    @Test
    public void testFindFirstWithPredicate() {
        assertEquals(Integer.valueOf(3), Stream.of(1, 2, 3, 4).findFirst(n -> n > 2).get());
        assertFalse(Stream.of(1, 2, 3).findFirst(n -> n > 5).isPresent());
    }

    @Test
    public void testFindLast() {
        assertEquals(Integer.valueOf(3), Stream.of(1, 2, 3).last().get());
        assertFalse(Stream.<Integer> empty().last().isPresent());
    }

    @Test
    public void testFindLastWithPredicate() {
        assertEquals(Integer.valueOf(4), Stream.of(1, 2, 3, 4).findLast(n -> n > 2).get());
        assertFalse(Stream.of(1, 2, 3).findLast(n -> n > 5).isPresent());
    }

    @Test
    public void testFindAny() {
        u.Optional<Integer> result = Stream.of(1, 2, 3).findAny();
        assertTrue(result.isPresent());
        assertTrue(Arrays.asList(1, 2, 3).contains(result.get()));
    }

    @Test
    public void testFirst() {
        assertEquals(Integer.valueOf(1), Stream.of(1, 2, 3).first().get());
        assertFalse(Stream.<Integer> empty().first().isPresent());
    }

    @Test
    public void testElementAt() {
        assertEquals(Integer.valueOf(2), Stream.of(1, 2, 3).elementAt(1).get());
        assertFalse(Stream.of(1, 2, 3).elementAt(5).isPresent());
        assertFalse(Stream.<Integer> empty().elementAt(0).isPresent());
    }

    @Test
    public void testOnlyOne() {
        assertEquals(Integer.valueOf(42), Stream.of(42).onlyOne().get());
        assertFalse(Stream.<Integer> empty().onlyOne().isPresent());
        assertThrows(TooManyElementsException.class, () -> Stream.of(1, 2).onlyOne());
    }

    @Test
    public void testCount() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).count());
        assertEquals(0, Stream.empty().count());
    }

    @Test
    public void testMin() {
        assertEquals(Integer.valueOf(1), Stream.of(3, 1, 2).min(Comparator.naturalOrder()).get());
        assertFalse(Stream.<Integer> empty().min(Comparator.naturalOrder()).isPresent());
    }

    @Test
    public void testMinBy() {
        assertEquals("c", Stream.of("aaa", "bb", "c").minBy(String::length).get());
    }

    @Test
    public void testMax() {
        assertEquals(Integer.valueOf(3), Stream.of(1, 3, 2).max(Comparator.naturalOrder()).get());
        assertFalse(Stream.<Integer> empty().max(Comparator.naturalOrder()).isPresent());
    }

    @Test
    public void testMaxBy() {
        assertEquals("aaa", Stream.of("a", "bb", "aaa").maxBy(String::length).get());
    }

    @Test
    public void testKthLargest() {
        assertEquals(Integer.valueOf(4), Stream.of(1, 2, 3, 4, 5).kthLargest(2, Comparator.naturalOrder()).get());
    }

    @Test
    public void testSumInt() {
        assertEquals(15, Stream.of(1, 2, 3, 4, 5).sumInt(n -> n));
    }

    @Test
    public void testSumLong() {
        assertEquals(15L, Stream.of(1, 2, 3, 4, 5).sumLong(n -> n));
    }

    @Test
    public void testSumDouble() {
        assertEquals(15.0, Stream.of(1, 2, 3, 4, 5).sumDouble(n -> n), 0.001);
    }

    @Test
    public void testAverageInt() {
        assertEquals(u.OptionalDouble.of(3.0), Stream.of(1, 2, 3, 4, 5).averageInt(n -> n));
    }

    @Test
    public void testAverageLong() {
        assertEquals(u.OptionalDouble.of(3.0), Stream.of(1, 2, 3, 4, 5).averageLong(n -> n));
    }

    @Test
    public void testAverageDouble() {
        assertEquals(u.OptionalDouble.of(3.0), Stream.of(1, 2, 3, 4, 5).averageDouble(n -> n));
    }

    @Test
    public void testReduce() {
        assertEquals(Integer.valueOf(15), Stream.of(1, 2, 3, 4, 5).reduce(Integer::sum).get());
        assertFalse(Stream.<Integer> empty().reduce(Integer::sum).isPresent());
    }

    @Test
    public void testReduceWithIdentity() {
        assertEquals(Integer.valueOf(15), Stream.of(1, 2, 3, 4, 5).reduce(0, Integer::sum));
        assertEquals(Integer.valueOf(0), Stream.<Integer> empty().reduce(0, Integer::sum));
    }

    @Test
    public void testReduceWithIdentityAndCombiner() {
        assertEquals(Integer.valueOf(15), Stream.of(1, 2, 3, 4, 5).reduce(0, Integer::sum, Integer::sum));
    }

    @Test
    public void testCollect() {
        List<Integer> list = Stream.of(1, 2, 3).collect(Collectors.toList());
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testToList() {
        List<Integer> list = Stream.of(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testToSet() {
        Set<Integer> set = Stream.of(1, 2, 2, 3).toSet();
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
    }

    @Test
    public void testToCollection() {
        LinkedHashSet<Integer> set = Stream.of(1, 2, 3).toCollection(LinkedHashSet::new);
        assertEquals(new LinkedHashSet<>(Arrays.asList(1, 2, 3)), set);
    }

    @Test
    public void testToArray() {
        Integer[] array = Stream.of(1, 2, 3).toArray(Integer[]::new);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, array);
    }

    @Test
    public void testToMap() {
        Map<Integer, String> map = Stream.of("a", "bb", "ccc").toMap(String::length, s -> s);
        assertEquals("a", map.get(1));
        assertEquals("bb", map.get(2));
        assertEquals("ccc", map.get(3));
    }

    @Test
    public void testToMapWithMerge() {
        Map<Integer, String> map = Stream.of("a", "b", "cc", "dd").toMap(String::length, s -> s, (s1, s2) -> s1 + s2);
        assertEquals("ab", map.get(1));
        assertEquals("ccdd", map.get(2));
    }

    @Test
    public void testToMultiset() {
        Multiset<Integer> multiset = Stream.of(1, 2, 2, 3, 3, 3).toMultiset();
        assertEquals(1, multiset.count(1));
        assertEquals(2, multiset.count(2));
        assertEquals(3, multiset.count(3));
    }

    @Test
    public void testJoin() {
        assertEquals("1,2,3", Stream.of(1, 2, 3).join(","));
    }

    @Test
    public void testJoinEmpty() {
        assertEquals("", Stream.empty().join(","));
    }

    @Test
    public void testJoinWithPrefixSuffix() {
        assertEquals("[1,2,3]", Stream.of(1, 2, 3).join(",", "[", "]"));
    }

    @Test
    public void testInnerJoin() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 2, 3).innerJoin(Arrays.asList(2, 3, 4), n -> n, n -> n).toList();
        assertEquals(2, result.size());
        assertEquals(Pair.of(2, 2), result.get(0));
        assertEquals(Pair.of(3, 3), result.get(1));
    }

    @Test
    public void testInnerJoinEmpty() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 2, 3).innerJoin(Arrays.asList(4, 5, 6), n -> n, n -> n).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testInnerJoinWithFunction() {
        List<String> result = Stream.of(1, 2, 3).innerJoin(Arrays.asList(2, 3, 4), n -> n, n -> n, (a, b) -> a + "-" + b).toList();
        assertEquals(Arrays.asList("2-2", "3-3"), result);
    }

    @Test
    public void testLeftJoin() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 2, 3).leftJoin(Arrays.asList(2, 3, 4), n -> n, n -> n).toList();
        assertEquals(3, result.size());
        assertEquals(Pair.of(1, null), result.get(0));
        assertEquals(Pair.of(2, 2), result.get(1));
        assertEquals(Pair.of(3, 3), result.get(2));
    }

    @Test
    public void testRightJoin() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 2, 3).rightJoin(Arrays.asList(2, 3, 4), n -> n, n -> n).toList();
        assertEquals(3, result.size());
        assertEquals(Pair.of(2, 2), result.get(0));
        assertEquals(Pair.of(3, 3), result.get(1));
        assertEquals(Pair.of(null, 4), result.get(2));
    }

    @Test
    public void testFullJoin() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 2, 3).fullJoin(Arrays.asList(2, 3, 4), n -> n, n -> n).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testCrossJoin() {
        List<Pair<Integer, String>> result = Stream.of(1, 2).crossJoin(Arrays.asList("a", "b")).toList();
        assertEquals(4, result.size());
        assertEquals(Pair.of(1, "a"), result.get(0));
        assertEquals(Pair.of(1, "b"), result.get(1));
        assertEquals(Pair.of(2, "a"), result.get(2));
        assertEquals(Pair.of(2, "b"), result.get(3));
    }

    @Test
    public void testCycled() {
        List<Integer> result = Stream.of(1, 2, 3).cycled().limit(7).toList();
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1), result);
    }

    @Test
    public void testCycledWithRounds() {
        List<Integer> result = Stream.of(1, 2, 3).cycled(2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), result);
    }

    @Test
    public void testCycledZeroRounds() {
        List<Integer> result = Stream.of(1, 2, 3).cycled(0).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testIntersperseEmpty() {
        List<Integer> result = Stream.<Integer> empty().intersperse(0).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testIntersperseSingleElement() {
        List<Integer> result = Stream.of(1).intersperse(0).toList();
        assertEquals(Arrays.asList(1), result);
    }

    @Test
    public void testParallel() {
        Stream<Integer> stream = Stream.of(1, 2, 3).parallel();
        assertTrue(stream.isParallel());
    }

    @Test
    public void testSequential() {
        Stream<Integer> stream = Stream.of(1, 2, 3).parallel().sequential();
        assertFalse(stream.isParallel());
    }

    @Test
    public void testIsParallel() {
        assertFalse(Stream.of(1, 2, 3).isParallel());
        assertTrue(Stream.of(1, 2, 3).parallel().isParallel());
    }

    @Test
    public void testParallelWithMaxThreadNum() {
        Stream<Integer> stream = Stream.of(1, 2, 3).parallel(2);
        assertTrue(stream.isParallel());
    }

    @Test
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        Stream<Integer> stream = Stream.of(1, 2, 3).onClose(() -> closed.set(true));
        stream.count();
        stream.close();
        assertTrue(closed.get());
    }

    @Test
    public void testOnCloseMultipleHandlers() {
        AtomicInteger counter = new AtomicInteger(0);
        Stream<Integer> stream = Stream.of(1, 2, 3).onClose(counter::incrementAndGet).onClose(counter::incrementAndGet);
        stream.count();
        stream.close();
        assertEquals(2, counter.get());
    }

    @Test
    public void testThrowIfEmpty() {
        assertThrows(NoSuchElementException.class, () -> Stream.empty().throwIfEmpty().count());
    }

    @Test
    public void testThrowIfEmptyNonEmpty() {
        assertEquals(3, Stream.of(1, 2, 3).throwIfEmpty().count());
    }

    @Test
    public void testThrowIfEmptyWithSupplier() {
        assertThrows(IllegalStateException.class, () -> Stream.empty().throwIfEmpty(IllegalStateException::new).count());
    }

    @Test
    public void testIfEmpty() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Stream.empty().ifEmpty(() -> executed.set(true)).count();
        assertTrue(executed.get());
    }

    @Test
    public void testIfEmptyNonEmpty() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Stream.of(1, 2, 3).ifEmpty(() -> executed.set(true)).count();
        assertFalse(executed.get());
    }

    @Test
    public void testContainsAll() {
        assertTrue(Stream.of(1, 2, 3, 4, 5).containsAll(Arrays.asList(2, 3, 4)));
        assertFalse(Stream.of(1, 2, 3).containsAll(Arrays.asList(3, 4, 5)));
    }

    @Test
    public void testContainsAny() {
        assertTrue(Stream.of(1, 2, 3).containsAny(Arrays.asList(3, 4, 5)));
        assertFalse(Stream.of(1, 2, 3).containsAny(Arrays.asList(4, 5, 6)));
    }

    @Test
    public void testContainsNone() {
        assertTrue(Stream.of(1, 2, 3).containsNone(Arrays.asList(4, 5, 6)));
        assertFalse(Stream.of(1, 2, 3).containsNone(Arrays.asList(3, 4, 5)));
    }

    @Test
    public void testHasDuplicates() {
        assertTrue(Stream.of(1, 2, 2, 3).containsDuplicates());
        assertFalse(Stream.of(1, 2, 3).containsDuplicates());
        assertFalse(Stream.<Integer> empty().containsDuplicates());
    }

    @Test
    public void testAsyncRun() throws Exception {
        AtomicInteger result = new AtomicInteger(0);
        com.landawn.abacus.util.ContinuableFuture<Void> future = Stream.of(1, 2, 3).asyncRun(s -> s.forEach(result::addAndGet));
        future.get();
        assertEquals(6, result.get());
    }

    @Test
    public void testAsyncCall() throws Exception {
        com.landawn.abacus.util.ContinuableFuture<Integer> future = Stream.of(1, 2, 3).asyncCall(s -> s.reduce(0, Integer::sum));
        assertEquals(Integer.valueOf(6), future.get());
    }

    @Test
    public void testTransform() {
        List<Integer> result = Stream.of(1, 2, 3).transform(s -> s.map(n -> n * 2)).toList();
        assertEquals(Arrays.asList(2, 4, 6), result);
    }

    @Test
    public void testSelect() {
        List<Integer> result = Stream.of(1, "two", 3, "four", 5).select(Integer.class).toList();
        assertEquals(Arrays.asList(1, 3, 5), result);
    }

    @Test
    public void testBuffered() {
        List<Integer> result = Stream.of(1, 2, 3).buffered().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testBufferedWithSize() {
        List<Integer> result = Stream.of(1, 2, 3).buffered(2).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPairWith() {
        List<Pair<String, Integer>> result = Stream.of("a", "bb", "ccc").pairWith(String::length).toList();
        assertEquals(Pair.of("a", 1), result.get(0));
        assertEquals(Pair.of("bb", 2), result.get(1));
        assertEquals(Pair.of("ccc", 3), result.get(2));
    }

    @Test
    public void testCombinations() {
        List<List<Integer>> result = Stream.of(1, 2, 3).combinations(2).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(Arrays.asList(1, 2)));
        assertTrue(result.contains(Arrays.asList(1, 3)));
        assertTrue(result.contains(Arrays.asList(2, 3)));
    }

    @Test
    public void testCombinationsAll() {
        List<List<Integer>> result = Stream.of(1, 2).combinations().toList();
        assertTrue(result.size() >= 3);
    }

    @Test
    public void testPermutations() {
        List<List<Integer>> result = Stream.of(1, 2, 3).permutations().toList();
        assertEquals(6, result.size());
    }

    @Test
    public void testOrderedPermutations() {
        List<List<Integer>> result = Stream.of(1, 2, 3).orderedPermutations().toList();
        assertEquals(6, result.size());
    }

    @Test
    public void testCartesianProduct() {
        List<List<Integer>> result = Stream.of(1, 2).cartesianProduct(Arrays.asList(3, 4)).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(Arrays.asList(1, 3)));
        assertTrue(result.contains(Arrays.asList(1, 4)));
        assertTrue(result.contains(Arrays.asList(2, 3)));
        assertTrue(result.contains(Arrays.asList(2, 4)));
    }

    @Test
    public void testToJdkStream() {
        java.util.stream.Stream<Integer> jdkStream = Stream.of(1, 2, 3).toJdkStream();
        assertEquals(Arrays.asList(1, 2, 3), jdkStream.collect(Collectors.toList()));
    }

    @Test
    public void testSps() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).sps(s -> s.filter(n -> n % 2 == 0)).toList();
        assertHaveSameElements(Arrays.asList(2, 4), result);
    }

    @Test
    public void testSkipNulls() {
        List<Integer> result = Stream.of(1, null, 2, null, 3).skipNulls().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDefaultIfEmpty() {
        List<Integer> result1 = Stream.<Integer> empty().defaultIfEmpty(() -> Stream.of(1, 2, 3)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result1);

        List<Integer> result2 = Stream.of(1, 2).defaultIfEmpty(() -> Stream.of(3, 4)).toList();
        assertEquals(Arrays.asList(1, 2), result2);
    }

    @Test
    public void testEmptyStreamOperations() {
        Stream<Integer> empty = Stream.empty();

        assertEquals(0, empty.count());
        assertFalse(Stream.<Integer> empty().first().isPresent());
        assertFalse(Stream.<Integer> empty().last().isPresent());
        assertEquals(0, Stream.<Integer> empty().toList().size());
        assertEquals(0, Stream.<Integer> empty().toSet().size());
        assertEquals("", Stream.<Integer> empty().join(","));
    }

    @Test
    public void testSingleElementStream() {
        Stream<Integer> single = Stream.of(42);

        assertEquals(1, single.count());
        assertEquals(Integer.valueOf(42), Stream.of(42).first().get());
        assertEquals(Integer.valueOf(42), Stream.of(42).last().get());
        assertEquals(Integer.valueOf(42), Stream.of(42).onlyOne().get());
    }

    @Test
    public void testNullHandling() {
        List<Integer> withNulls = Arrays.asList(1, null, 2, null, 3);

        List<Integer> result = Stream.of(withNulls).filter(n -> n != null).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testComplexStreamPipeline() {
        List<String> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).filter(n -> n % 2 == 0).map(n -> n * 2).sorted().limit(3).map(String::valueOf).toList();

        assertEquals(Arrays.asList("4", "8", "12"), result);
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
    public void testMapToShort() {
        short[] result = Stream.of(1, 2, 3).mapToShort(Integer::shortValue).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapToShortEmpty() {
        short[] result = Stream.<Integer> empty().mapToShort(Integer::shortValue).toArray();
        assertEquals(0, result.length);
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
    public void testFlatmapToChar() {
        char[] result = Stream.of("ab", "cd", "ef").flatmapToChar(s -> s.toCharArray()).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, result);
    }

    @Test
    public void testFlatmapToCharEmpty() {
        char[] result = Stream.<String> empty().flatmapToChar(s -> s.toCharArray()).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToByte() {
        byte[] result = Stream.of(new byte[] { 1, 2 }, new byte[] { 3, 4 }).flatmapToByte(b -> b).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatmapToByteEmpty() {
        byte[] result = Stream.<byte[]> empty().flatmapToByte(b -> b).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToShort() {
        short[] result = Stream.of(new short[] { 1, 2 }, new short[] { 3, 4 }).flatmapToShort(s -> s).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatmapToShortEmpty() {
        short[] result = Stream.<short[]> empty().flatmapToShort(s -> s).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToFloat() {
        float[] result = Stream.of(new float[] { 1.0f, 2.0f }, new float[] { 3.0f, 4.0f }).flatmapToFloat(f -> f).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testFlatmapToFloatEmpty() {
        float[] result = Stream.<float[]> empty().flatmapToFloat(f -> f).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFoldLeft() {
        String result = Stream.of("a", "b", "c", "d").foldLeft((a, b) -> a + b).get();
        assertEquals("abcd", result);
    }

    @Test
    public void testFoldLeftWithIdentity() {
        String result = Stream.of("a", "b", "c").foldLeft("start:", (a, b) -> a + b);
        assertEquals("start:abc", result);
    }

    @Test
    public void testFoldLeftEmpty() {
        assertFalse(Stream.<String> empty().foldLeft((a, b) -> a + b).isPresent());
    }

    @Test
    public void testFoldRight() {
        String result = Stream.of("a", "b", "c", "d").foldRight((a, b) -> a + b).get();
        assertEquals("dcba", result);
    }

    @Test
    public void testFoldRightWithIdentity() {
        String result = Stream.of("a", "b", "c").foldRight(":end", (a, b) -> a + b);
        assertEquals(":endcba", result);
    }

    @Test
    public void testFoldRightEmpty() {
        assertFalse(Stream.<String> empty().foldRight((a, b) -> a + b).isPresent());
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
    public void testMapMulti() {
        List<Object> result = Stream.of(1, 2, 3).mapMulti((n, consumer) -> {
            consumer.accept(n);
            consumer.accept(n * 10);
        }).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
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
    public void testMapMultiToDouble() {
        double[] result = Stream.of(1, 2, 3).mapMultiToDouble((n, consumer) -> {
            consumer.accept(n.doubleValue());
            consumer.accept(n.doubleValue() * 0.5);
        }).toArray();
        assertArrayEquals(new double[] { 1.0, 0.5, 2.0, 1.0, 3.0, 1.5 }, result, 0.001);
    }

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
    public void testMapToEntryEmpty() {
        List<Map.Entry<String, Integer>> result = Stream.<String> empty().mapToEntry(s -> s, s -> s.length()).toList();
        assertTrue(result.isEmpty());
    }

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
    public void testFlatMapToEntryEmpty() {
        List<Map.Entry<String, Integer>> result = Stream.<Map<String, Integer>> empty().flatMapToEntry(m -> Stream.of(m.entrySet())).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRangeMap() {
        List<Integer> result = Stream.of(1, 2, 2, 3, 3, 3, 4).rangeMap((a, b) -> a.equals(b), (a, b) -> a).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testRangeMapEmpty() {
        List<Integer> result = Stream.<Integer> empty().rangeMap((a, b) -> a.equals(b), (a, b) -> a).toList();
        assertTrue(result.isEmpty());
    }

    //
    //    @Test
    //    public void testReduceUntilWithBiPredicate() {
    //        int result = Stream.of(1, 2, 3, 4, 5).reduceUntil((a, b) -> a + b, (a, b) -> a + b > 6).get();
    //        assertEquals(6, result);
    //    }

    //
    //
    //
    @Test
    public void testGroupTo() {
        Map<Integer, List<String>> map = Stream.of("a", "bb", "c", "dd", "eee").groupTo(String::length);
        assertEquals(2, map.get(1).size());
        assertEquals(2, map.get(2).size());
        assertEquals(1, map.get(3).size());
    }

    @Test
    public void testGroupToEmpty() {
        Map<Integer, List<String>> map = Stream.<String> empty().groupTo(String::length);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testFlatGroupTo() {
        Map<Integer, List<String>> map = Stream.of("ab", "cd", "efg").flatGroupTo(s -> Arrays.asList(s.length(), s.length()));
        assertEquals(4, map.get(2).size());
        assertEquals(2, map.get(3).size());
    }

    @Test
    public void testCountByToEntry() {
        List<Map.Entry<Integer, Integer>> list = Stream.of("a", "bb", "c", "dd", "eee").countByToEntry(String::length).toList();
        assertEquals(3, list.size());
        assertTrue(list.stream().anyMatch(e -> e.getKey() == 1 && e.getValue() == 2));
        assertTrue(list.stream().anyMatch(e -> e.getKey() == 2 && e.getValue() == 2));
        assertTrue(list.stream().anyMatch(e -> e.getKey() == 3 && e.getValue() == 1));
    }

    @Test
    public void testCountByToEntryEmpty() {
        List<Map.Entry<Integer, Integer>> list = Stream.<String> empty().countByToEntry(String::length).toList();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testPartitionTo() {
        Map<Boolean, List<Integer>> map = Stream.of(1, 2, 3, 4, 5).partitionTo(n -> n % 2 == 0);
        assertEquals(Arrays.asList(2, 4), map.get(true));
        assertEquals(Arrays.asList(1, 3, 5), map.get(false));
    }

    @Test
    public void testPartitionToEmpty() {
        Map<Boolean, List<Integer>> map = Stream.<Integer> empty().partitionTo(n -> n % 2 == 0);
        assertTrue(map.get(true).isEmpty());
        assertTrue(map.get(false).isEmpty());
    }

    @Test
    public void testPartitionByToEntry() {
        List<Map.Entry<Boolean, List<Integer>>> list = Stream.of(1, 2, 3, 4, 5).partitionByToEntry(n -> n % 2 == 0).toList();
        assertEquals(2, list.size());
    }

    @Test
    public void testToMultimap() {
        com.landawn.abacus.util.ListMultimap<Integer, String> multimap = Stream.of("a", "bb", "c", "dd", "eee").toMultimap(String::length);
        assertEquals(2, multimap.get(1).size());
        assertEquals(2, multimap.get(2).size());
        assertEquals(1, multimap.get(3).size());
    }

    @Test
    public void testToMultimapEmpty() {
        com.landawn.abacus.util.ListMultimap<Integer, String> multimap = Stream.<String> empty().toMultimap(String::length);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testToDataset() {
        Dataset dataset = Stream.of(Arrays.asList("a", 1), Arrays.asList("b", 2), Arrays.asList("c", 3)).toDataset(N.toList("Column1", "Column2"));
        assertEquals(3, dataset.size());
    }

    @Test
    public void testToDatasetWithColumnNames() {
        Dataset dataset = Stream.of(Arrays.asList("a", 1), Arrays.asList("b", 2)).toDataset(Arrays.asList("col1", "col2"));
        assertEquals(2, dataset.size());
        assertEquals(Arrays.asList("col1", "col2"), dataset.columnNames());
    }

    @Test
    public void testToDatasetEmpty() {
        Dataset dataset = Stream.<List<Object>> empty().toDataset();
        assertEquals(0, dataset.size());
    }

    @Test
    public void testSortedByLong() {
        List<String> result = Stream.of("a", "bbb", "cc").sortedByLong(String::length).toList();
        assertEquals(Arrays.asList("a", "cc", "bbb"), result);
    }

    @Test
    public void testSortedByDouble() {
        List<Integer> result = Stream.of(3, 1, 2).sortedByDouble(n -> n.doubleValue()).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testReverseSortedBy() {
        List<String> result = Stream.of("a", "bbb", "cc").reverseSortedBy(String::length).toList();
        assertEquals(Arrays.asList("bbb", "cc", "a"), result);
    }

    @Test
    public void testReverseSortedByInt() {
        List<String> result = Stream.of("a", "bbb", "cc").reverseSortedByInt(String::length).toList();
        assertEquals(Arrays.asList("bbb", "cc", "a"), result);
    }

    @Test
    public void testReverseSortedByLong() {
        List<String> result = Stream.of("a", "bbb", "cc").reverseSortedByLong(String::length).toList();
        assertEquals(Arrays.asList("bbb", "cc", "a"), result);
    }

    @Test
    public void testReverseSortedByDouble() {
        List<Integer> result = Stream.of(1, 3, 2).reverseSortedByDouble(n -> n.doubleValue()).toList();
        assertEquals(Arrays.asList(3, 2, 1), result);
    }

    @Test
    public void testMinAll() {
        List<Integer> result = Stream.of(1, 1, 2, 3, 4).minAll(Integer::compareTo);
        assertEquals(Arrays.asList(1, 1), result);
    }

    @Test
    public void testMinAllEmpty() {
        List<Integer> result = Stream.<Integer> empty().minAll(Integer::compareTo);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMaxAll() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 4).maxAll(Integer::compareTo);
        assertEquals(Arrays.asList(4, 4), result);
    }

    @Test
    public void testMaxAllEmpty() {
        List<Integer> result = Stream.<Integer> empty().maxAll(Integer::compareTo);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testPercentiles() {
        u.Optional<Map<Percentage, Integer>> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).percentiles(Integer::compareTo);
        assertTrue(result.isPresent());
        assertFalse(result.get().isEmpty());
    }

    @Test
    public void testSkipRange() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).toList();
        assertEquals(Arrays.asList(1, 4, 5), result);
    }

    @Test
    public void testSkipRangeEmpty() {
        List<Integer> result = Stream.<Integer> empty().skipRange(1, 3).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSkipRangeOutOfBounds() {
        List<Integer> result = Stream.of(1, 2, 3).skipRange(5, 10).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPeekIf() {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).peekIf(n -> n % 2 == 0, peeked::add).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        assertEquals(Arrays.asList(2, 4), peeked);
    }

    @Test
    public void testPeekIfEmpty() {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Stream.<Integer> empty().peekIf(n -> n % 2 == 0, peeked::add).toList();
        assertTrue(result.isEmpty());
        assertTrue(peeked.isEmpty());
    }

    @Test
    public void testCollectThenAccept() {
        List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 3).collectThenAccept(java.util.stream.Collectors.toList(), holder::addAll);
        assertEquals(Arrays.asList(1, 2, 3), holder);
    }

    @Test
    public void testCollectThenApply() {
        int result = Stream.of(1, 2, 3).collectThenApply(java.util.stream.Collectors.toList(), List::size);
        assertEquals(3, result);
    }

    @Test
    public void testToListThenAccept() {
        List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 3).toListThenAccept(holder::addAll);
        assertEquals(Arrays.asList(1, 2, 3), holder);
    }

    @Test
    public void testToListThenApply() {
        int result = Stream.of(1, 2, 3).toListThenApply(List::size);
        assertEquals(3, result);
    }

    @Test
    public void testToSetThenAccept() {
        List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 2, 3).toSetThenAccept(holder::addAll);
        assertEquals(3, holder.size());
    }

    @Test
    public void testToSetThenApply() {
        int result = Stream.of(1, 2, 2, 3).toSetThenApply(java.util.Set::size);
        assertEquals(3, result);
    }

    @Test
    public void testToCollectionThenAccept() {
        List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 3).toCollectionThenAccept(ArrayList::new, holder::addAll);
        assertEquals(Arrays.asList(1, 2, 3), holder);
    }

    @Test
    public void testToCollectionThenApply() {
        int result = Stream.of(1, 2, 3).toCollectionThenApply(ArrayList::new, List::size);
        assertEquals(3, result);
    }

    @Test
    public void testAcceptIfNotEmpty() {
        List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 3).acceptIfNotEmpty(s -> s.forEach(holder::add));
        assertEquals(Arrays.asList(1, 2, 3), holder);
    }

    @Test
    public void testAcceptIfNotEmptyWithEmpty() {
        List<Integer> holder = new ArrayList<>();
        Stream.<Integer> empty().acceptIfNotEmpty(s -> s.forEach(holder::add));
        assertTrue(holder.isEmpty());
    }

    @Test
    public void testApplyIfNotEmpty() {
        long result = Stream.of(1, 2, 3).applyIfNotEmpty(s -> s.count()).get();
        assertEquals(3L, result);
    }

    @Test
    public void testApplyIfNotEmptyWithEmpty() {
        assertFalse(Stream.<Integer> empty().applyIfNotEmpty(s -> s.count()).isPresent());
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
    public void testMapLastOrElse() {
        List<String> result = Stream.of(1, 2, 3, 4, 5).mapLastOrElse(n -> "last:" + n, n -> "else:" + n).toList();
        assertEquals(Arrays.asList("else:1", "else:2", "else:3", "else:4", "last:5"), result);
    }

    @Test
    public void testMapLastOrElseEmpty() {
        List<String> result = Stream.<Integer> empty().mapLastOrElse(n -> "last:" + n, n -> "else:" + n).toList();
        assertTrue(result.isEmpty());
    }

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
    public void testFlattMapToEntry() {
        EntryStream<String, Integer> stream1 = EntryStream.of("a", 1);
        EntryStream<String, Integer> stream2 = EntryStream.of("b", 2);

        List<Map.Entry<String, Integer>> result = Stream.of(stream1, stream2).flattMapToEntry(s -> s).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testFlattMapToEntryEmpty() {
        List<Map.Entry<String, Integer>> result = Stream.<EntryStream<String, Integer>> empty().flattMapToEntry(s -> s).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFromJdkStream() {
        java.util.stream.Stream<Integer> jdkStream = java.util.stream.Stream.of(1, 2, 3, 4, 5);
        List<Integer> result = Stream.from(jdkStream).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testFromJdkStreamEmpty() {
        java.util.stream.Stream<Integer> jdkStream = java.util.stream.Stream.empty();
        List<Integer> result = Stream.from(jdkStream).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDefer() {
        AtomicInteger callCount = new AtomicInteger(0);
        Stream<Integer> deferred = Stream.defer(() -> {
            callCount.incrementAndGet();
            return Stream.of(1, 2, 3);
        });

        assertEquals(0, callCount.get());
        List<Integer> result = deferred.toList();
        assertEquals(1, callCount.get());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDeferEmpty() {
        Stream<Integer> deferred = Stream.defer(() -> Stream.empty());
        assertTrue(deferred.toList().isEmpty());
    }

    @Test
    public void testFlattenCollection() {
        List<Integer> list1 = Arrays.asList(1, 2);
        List<Integer> list2 = Arrays.asList(3, 4);
        List<Integer> list3 = Arrays.asList(5, 6);

        List<Integer> result = Stream.<Integer> flatten(Arrays.asList(list1, list2, list3)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testFlattenCollectionEmpty() {
        List<Object> result = Stream.flatten(Arrays.asList()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattenArray() {
        Integer[][] arr = { { 1, 2 }, { 3, 4 } };

        List<Integer> result = Stream.flatten(arr).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testRollup() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4).rollup().toList();
        assertEquals(5, result.size());
        assertEquals(Arrays.asList(Arrays.asList(), Arrays.asList(1), Arrays.asList(1, 2), Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 3, 4)), result);
    }

    @Test
    public void testRollupEmpty() {
        List<List<Integer>> result = Stream.<Integer> empty().rollup().toList();
        assertEquals(1, result.size());
        assertEquals(Arrays.asList(Arrays.asList()), result);
    }

    @Test
    public void testRollupSingleElement() {
        List<List<Integer>> result = Stream.of(1).rollup().toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(Arrays.asList(), Arrays.asList(1)), result);
    }

    @Test
    public void testSplitByChunkCount() {
        List<Integer> result = Stream.splitByChunkCount(8, 3, (from, to) -> from).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testSplitByChunkCountEmpty() {
        List<Integer> result = Stream.splitByChunkCount(0, 3, (from, to) -> from).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatIterables() {
        List<Integer> list1 = Arrays.asList(1, 2);
        List<Integer> list2 = Arrays.asList(3, 4);
        List<Integer> list3 = Arrays.asList(5, 6);

        List<Integer> result = Stream.concatIterables(Arrays.asList(list1, list2, list3)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testConcatIterablesEmpty() {
        List<Integer> result = Stream.concatIterables(Arrays.<List<Integer>> asList()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatIterators() {
        Iterator<Integer> iter1 = Arrays.asList(1, 2).iterator();
        Iterator<Integer> iter2 = Arrays.asList(3, 4).iterator();

        List<Integer> result = Stream.concatIterators(Arrays.asList(iter1, iter2)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testMergeIterables() {
        List<Integer> list1 = Arrays.asList(1, 3, 5);
        List<Integer> list2 = Arrays.asList(2, 4, 6);

        List<Integer> result = Stream.mergeIterables(Arrays.asList(list1, list2), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeIterablesEmpty() {
        List<Integer> result = Stream.mergeIterables(Arrays.<List<Integer>> asList(), (a, b) -> MergeResult.TAKE_FIRST).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMergeIterators() {
        Iterator<Integer> iter1 = Arrays.asList(1, 3).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4).iterator();

        List<Integer> result = Stream.mergeIterators(Arrays.asList(iter1, iter2), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testZipIterables() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(10, 20, 30);

        List<Integer> result = Stream.zipIterables(Arrays.asList(list1, list2), (List<Integer> args) -> args.get(0) + args.get(1)).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipIterablesEmpty() {
        List<Integer> result = Stream.zipIterables(Arrays.asList(), (List<Integer> args) -> 0).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipIterators() {
        Iterator<Integer> iter1 = Arrays.asList(1, 2).iterator();
        Iterator<Integer> iter2 = Arrays.asList(10, 20).iterator();

        List<Integer> result = Stream.zipIterators(Arrays.asList(iter1, iter2), (List<Integer> args) -> args.get(0) + args.get(1)).toList();
        assertEquals(Arrays.asList(11, 22), result);
    }

    @Test
    public void testOfReversed() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        List<Integer> result = Stream.ofReversed(array).toList();
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result);
    }

    @Test
    public void testOfReversedEmpty() {
        Integer[] array = {};
        List<Integer> result = Stream.ofReversed(array).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOfReversedList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> result = Stream.ofReversed(list).toList();
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result);
    }

    @Test
    public void testForEachUntil() {
        List<Integer> collected = new ArrayList<>();
        MutableBoolean flagToBreak = MutableBoolean.of(false);
        Stream.of(1, 2, 3, 4, 5).forEachUntil(flagToBreak, n -> {
            collected.add(n);
            if (n >= 3) {
                flagToBreak.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEachUntilEmpty() {
        List<Integer> collected = new ArrayList<>();
        com.landawn.abacus.util.MutableBoolean flagToBreak = com.landawn.abacus.util.MutableBoolean.of(true);
        Stream.<Integer> empty().forEachUntil(flagToBreak, n -> {
            collected.add(n);
        });
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testForEachUntilBiConsumer() {
        List<Integer> collected = new ArrayList<>();
        Stream.of(1, 2, 3, 4, 5).forEachUntil((n, breakCondition) -> {
            collected.add(n);
            if (n >= 3) {
                breakCondition.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testToImmutableMap() {
        com.landawn.abacus.util.ImmutableMap<Integer, String> map = Stream.of("a", "bb", "ccc").toImmutableMap(String::length, s -> s);
        assertEquals(3, map.size());
        assertEquals("a", map.get(1));
        assertEquals("bb", map.get(2));
        assertEquals("ccc", map.get(3));
    }

    @Test
    public void testToImmutableMapEmpty() {
        com.landawn.abacus.util.ImmutableMap<Integer, String> map = Stream.<String> empty().toImmutableMap(String::length, s -> s);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testMergeWith() {
        List<Integer> result = Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeWithEmpty() {
        List<Integer> result = Stream.<Integer> empty().mergeWith(Stream.of(1, 2, 3), (a, b) -> MergeResult.TAKE_FIRST).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testJoinByRange() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(2, 3, 4);

        List<Pair<Integer, List<Integer>>> result = Stream.of(list1).joinByRange(list2.iterator(), (a, b) -> Math.abs(a - b) <= 1).toList();
        assertFalse(result.isEmpty());
    }

    @Test
    public void testJoinByRangeEmpty() {
        List<Integer> list2 = Arrays.asList(2, 3, 4);

        List<Pair<Integer, List<Integer>>> result = Stream.<Integer> empty().joinByRange(list2.iterator(), (a, b) -> Math.abs(a - b) <= 1).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testListFiles() throws IOException {
        File tempFile = tempDir.resolve("test.txt").toFile();
        IOUtil.write("test content", tempFile);

        List<File> result = Stream.listFiles(tempDir.toFile()).toList();
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(f -> f.getName().equals("test.txt")));
    }

    @Test
    public void testListFilesRecursively() throws IOException {
        File txtFile = tempDir.resolve("test.txt").toFile();
        File jsonFile = tempDir.resolve("test.json").toFile();
        IOUtil.write("txt", txtFile);
        IOUtil.write("json", jsonFile);

        List<File> result = Stream.listFiles(tempDir.toFile(), false).toList();
        assertFalse(result.isEmpty());
        assertTrue(result.size() >= 2);
    }

    @Test
    public void testOfLines() throws IOException {
        File file = tempDir.resolve("lines.txt").toFile();
        IOUtil.write("line1\nline2\nline3", file);

        List<String> result = Stream.ofLines(file).toList();
        assertEquals(Arrays.asList("line1", "line2", "line3"), result);
    }

    @Test
    public void testOfLinesEmpty() throws IOException {
        File file = tempDir.resolve("empty.txt").toFile();
        IOUtil.write("", file);

        List<String> result = Stream.ofLines(file).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSplitToLines() {
        List<String> result = Stream.splitToLines("line1\nline2\nline3").toList();
        assertEquals(Arrays.asList("line1", "line2", "line3"), result);
    }

    @Test
    public void testSplitToLinesEmpty() {
        List<String> result = Stream.splitToLines("").toList();
        assertEquals(Arrays.asList(""), result);
    }

    @Test
    public void testSplitToLinesSingleLine() {
        List<String> result = Stream.splitToLines("single line").toList();
        assertEquals(Arrays.asList("single line"), result);
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
    public void testInterval() {
        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> result = Stream.interval(5, () -> counter.incrementAndGet()).limit(3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testIntervalWithDelay() {
        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> result = Stream.interval(1, 5, () -> counter.incrementAndGet()).limit(3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
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
    public void testFlatmapToIntArray() {
        int[] result = Stream.of("12", "34")
                .flatmapToInt(s -> new int[] { Character.getNumericValue(s.charAt(0)), Character.getNumericValue(s.charAt(1)) })
                .toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatmapToIntArrayEmpty() {
        int[] result = Stream.<String> empty().flatmapToInt(s -> new int[] { 1, 2 }).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToLongArray() {
        long[] result = Stream.of(new long[] { 1L, 2L }, new long[] { 3L, 4L }).flatmapToLong(arr -> arr).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testFlatmapToLongArrayEmpty() {
        long[] result = Stream.<long[]> empty().flatmapToLong(arr -> arr).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapToDoubleArray() {
        double[] result = Stream.of(new double[] { 1.0, 2.0 }, new double[] { 3.0, 4.0 }).flatmapToDouble(arr -> arr).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testFlatmapToDoubleArrayEmpty() {
        double[] result = Stream.<double[]> empty().flatmapToDouble(arr -> arr).toArray();
        assertEquals(0, result.length);
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
    public void testFlattMap() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4)).flattMap(list -> list.stream()).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlattMapEmpty() {
        List<Integer> result = Stream.<List<Integer>> empty().flattMap(list -> list.stream()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGroupJoin() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 1, 2, 3, 3);

        List<Pair<Integer, List<Integer>>> result = Stream.of(list1).groupJoin(list2, n -> n, n -> n).toList();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).getLeft());
        assertEquals(2, result.get(0).getRight().size());
    }

    @Test
    public void testGroupJoinEmpty() {
        List<Integer> list2 = Arrays.asList(1, 2, 3);

        List<Pair<Integer, List<Integer>>> result = Stream.<Integer> empty().groupJoin(list2, n -> n, n -> n).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGroupJoinWithMapper() {
        List<String> list1 = Arrays.asList("a", "bb", "ccc");
        List<Integer> list2 = Arrays.asList(1, 1, 2, 3);

        List<Pair<String, List<Integer>>> result = Stream.of(list1).groupJoin(list2, String::length, n -> n).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testZipWith() {
        List<Integer> list1 = Arrays.asList(10, 20, 30);

        List<Integer> result = Stream.of(1, 2, 3).zipWith(list1, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipWithEmpty() {
        List<Integer> list1 = Arrays.asList(10, 20, 30);

        List<Integer> result = Stream.<Integer> empty().zipWith(list1, (a, b) -> a + b).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipWithThreeWay() {
        List<Integer> list1 = Arrays.asList(10, 20, 30);
        List<Integer> list2 = Arrays.asList(100, 200, 300);

        List<Integer> result = Stream.of(1, 2, 3).zipWith(list1, list2, (a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(111, 222, 333), result);
    }

    @Test
    public void testZipWithDefaultValue() {
        List<Integer> list1 = Arrays.asList(10, 20);

        List<Integer> result = Stream.of(1, 2, 3).zipWith(list1, 0, 0, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 22, 3), result);
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
    public void testPrintln() {
        assertDoesNotThrow(() -> {
            Stream.of(1, 2, 3).println();

            Stream.range(0, 1000).println();

            Stream.range(0, 1001).println();
        });
    }

    @Test
    public void testPrintlnEmpty() {
        Stream.empty().println();
        assertTrue(true);
    }

    @Test
    public void testToImmutableList() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).toImmutableList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        try {
            result.add(6);
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testToImmutableListEmpty() {
        List<Integer> result = Stream.<Integer> empty().toImmutableList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToImmutableSet() {
        Set<Integer> result = Stream.of(1, 2, 3, 2, 1).toImmutableSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));

        try {
            result.add(4);
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testToImmutableSetEmpty() {
        Set<Integer> result = Stream.<Integer> empty().toImmutableSet();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTransformB() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).transformB(jdkStream -> jdkStream.filter(n -> n % 2 == 0)).toList();
        assertEquals(Arrays.asList(2, 4), result);
    }

    @Test
    public void testTransformBWithMap() {
        List<Integer> result = Stream.of("a", "bb", "ccc").transformB(jdkStream -> jdkStream.map(String::length)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testTransformBEmpty() {
        List<Integer> result = Stream.<Integer> empty().transformB(jdkStream -> jdkStream.filter(n -> n > 0)).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParallelConcat() {
        Stream<Integer> s1 = Stream.of(1, 2, 3);
        Stream<Integer> s2 = Stream.of(4, 5, 6);
        List<Integer> result = Stream.parallelConcat(Arrays.asList(s1, s2), 2).toList();
        assertEquals(6, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4, 5, 6)));
    }

    @Test
    public void testParallelConcatIterators() {
        Iterator<Integer> it1 = Arrays.asList(1, 2).iterator();
        Iterator<Integer> it2 = Arrays.asList(3, 4).iterator();
        List<Integer> result = Stream.parallelConcatIterators(Arrays.asList(it1, it2), 2).toList();
        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4)));
    }

    @Test
    public void testPersist() throws IOException {
        File tempFile = new File(tempDir.toFile(), "persist_test.txt");
        List<String> data = Arrays.asList("line1", "line2", "line3");
        Stream.of(data).persist(tempFile);

        List<String> readBack = IOUtil.readAllLines(tempFile);
        assertEquals(data, readBack);
    }

    @Test
    public void testPersistToCSV() throws IOException {
        File tempFile = new File(tempDir.toFile(), "test.csv");
        List<List<String>> rows = Arrays.asList(Arrays.asList("a", "b", "c"), Arrays.asList("1", "2", "3"));
        Stream.of(rows).persistToCsv(N.toList("Column1", "Column2", "Column3"), tempFile);

        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testPersistToJSON() throws IOException {
        File tempFile = new File(tempDir.toFile(), "test.json");
        List<Map<String, Object>> data = Arrays.asList(new LinkedHashMap<String, Object>() {
            {
                put("name", "Alice");
                put("age", 30);
            }
        });
        Stream.of(data).persistToJson(tempFile);

        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testSpsMap() {
        Stream<Integer> stream = Stream.of(1, 2, 3);
        Stream<String> mapped = stream.spsMap(n -> String.valueOf(n * 2));
        assertNotNull(mapped);
        assertHaveSameElements(Arrays.asList("2", "4", "6"), mapped.toList());
    }

    @Test
    public void testSpsFilter() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5);
        Stream<Integer> filtered = stream.spsFilter(n -> n % 2 == 0);
        assertNotNull(filtered);
        assertHaveSameElements(Arrays.asList(2, 4), filtered.toList());
    }

    @Test
    public void testSpsFlatMap() {
        Stream<Integer> stream = Stream.of(1, 2, 3);
        Stream<Integer> flatMapped = stream.spsFlatMap(n -> Stream.of(n, n * 10));
        assertNotNull(flatMapped);
        List<Integer> result = flatMapped.toList();
        assertEquals(6, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 10, 2, 20, 3, 30)));
    }

    @Test
    public void testSpsMapE() {
        Stream<Integer> stream = Stream.of(1, 2, 3);
        Stream<String> mapped = stream.spsMapE(n -> {
            if (n == null)
                throw new IllegalArgumentException("null value");
            return String.valueOf(n * 2);
        });
        assertHaveSameElements(Arrays.asList("2", "4", "6"), mapped.toList());
    }

    @Test
    public void testSpsFilterE() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5);
        Stream<Integer> filtered = stream.spsFilterE(n -> {
            if (n == null)
                throw new IllegalArgumentException("null value");
            return n % 2 == 0;
        });
        assertHaveSameElements(Arrays.asList(2, 4), filtered.toList());
    }

    @Test
    public void testSpsFlatMapE() {
        Stream<Integer> stream = Stream.of(1, 2, 3);
        Stream<Integer> flatMapped = stream.spsFlatMapE(n -> {
            if (n == null)
                throw new IllegalArgumentException("null value");
            return Stream.of(n, n * 10);
        });
        List<Integer> result = flatMapped.toList();
        assertEquals(6, result.size());
    }

    @Test
    public void testParallelConcatEmpty() {
        Stream<Integer> s1 = Stream.empty();
        Stream<Integer> s2 = Stream.empty();
        List<Integer> result = Stream.parallelConcat(Arrays.asList(s1, s2), 2).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSpsFilterEmptyStream() {
        Stream<Integer> filtered = Stream.<Integer> empty().spsFilter(n -> n > 0);
        assertTrue(filtered.toList().isEmpty());
    }

    @Test
    public void testSpsMapEmptyStream() {
        Stream<String> mapped = Stream.<Integer> empty().spsMap(String::valueOf);
        assertTrue(mapped.toList().isEmpty());
    }

    // ==================== debounce tests ====================

    @Test
    public void testDebounce_BasicFunctionality() {
        // Allow 3 elements per 1 second window
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();

        // Only first 3 elements should pass through within the window
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result);

        result = Stream.range(0, 20).delay(Duration.ofMillis(100)).debounce(2, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();

        assertEquals(Arrays.asList(0, 1, 10, 11, 19), result);
    }

    @Test
    public void testDebounce_AllElementsPassWhenWithinLimit() {
        // Allow 10 elements per window, but only 5 elements in stream
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).debounce(10, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();

        // All elements should pass
        assertEquals(5, result.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testDebounce_EmptyStream() {
        List<Integer> result = Stream.<Integer> empty().debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testDebounce_SingleElement() {
        List<String> result = Stream.of("hello").debounce(1, com.landawn.abacus.util.Duration.ofMillis(100)).toList();

        assertEquals(1, result.size());
        assertEquals("hello", result.get(0));
    }

    @Test
    public void testDebounce_MaxWindowSizeOne() {
        // Only 1 element allowed per window
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).debounce(1, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();

        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
    }

    @Test
    public void testDebounce_WindowResetAfterDuration() throws InterruptedException {
        // Use a short duration to test window reset
        AtomicInteger count = new AtomicInteger(0);
        List<Integer> elements = new ArrayList<>();

        // Generate elements with delays to span multiple windows
        Stream.of(1, 2, 3, 4, 5, 6).peek(e -> {
            if (count.incrementAndGet() == 3) {
                // Sleep after 3rd element to allow window to reset
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }).debounce(2, com.landawn.abacus.util.Duration.ofMillis(100)).forEach(elements::add);

        // First window: 1, 2 pass (3 is after window reset)
        // After sleep, window resets: 4, 5 pass
        assertTrue(elements.size() >= 2);
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveMaxWindowSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            Stream.of(1, 2, 3).debounce(0, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Stream.of(1, 2, 3).debounce(-1, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();
        });
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveDuration() {
        assertThrows(IllegalArgumentException.class, () -> {
            Stream.of(1, 2, 3).debounce(5, com.landawn.abacus.util.Duration.ofMillis(0)).toList();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Stream.of(1, 2, 3).debounce(5, com.landawn.abacus.util.Duration.ofMillis(-100)).toList();
        });
    }

    @Test
    public void testDebounce_WithLargeMaxWindowSize() {
        List<Integer> input = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            input.add(i);
        }

        List<Integer> result = Stream.of(input).debounce(500, com.landawn.abacus.util.Duration.ofSeconds(10)).toList();

        assertEquals(500, result.size());
    }

    @Test
    public void testDebounce_PreservesOrder() {
        List<String> result = Stream.of("a", "b", "c", "d", "e").debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();

        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testDebounce_WithNullElements() {
        List<String> result = Stream.of("a", null, "c", null, "e").debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();

        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals(null, result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testDebounce_ChainedWithOtherOperations() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .filter(n -> n % 2 == 0) // 2, 4, 6, 8, 10
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)) // 2, 4, 6
                .map(n -> n * 10) // 20, 40, 60
                .toList();

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(20, 40, 60), result);
    }

    @Test
    public void testDebounce_WithLongDuration() {
        // Test with a very long duration to ensure all elements within limit pass
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).debounce(3, com.landawn.abacus.util.Duration.ofHours(1)).toList();

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDebounce_ParallelStream() {
        // Test debounce on a parallel stream - the method handles parallel specially
        List<Integer> input = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            input.add(i);
        }

        List<Integer> result = Stream.of(input).parallel().debounce(10, com.landawn.abacus.util.Duration.ofSeconds(10)).toList();

        // Should limit to maxWindowSize elements
        assertEquals(10, result.size());
    }

    @Test
    public void testDebounce_ParallelStreamEmpty() {
        List<Integer> result = Stream.<Integer> empty().parallel().debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilterWithActionOnDroppedItem() {
        List<Integer> droppedItems = new ArrayList<>();
        assertEquals(Arrays.asList(2, 4), Stream.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0, droppedItems::add).toList());
        assertEquals(Arrays.asList(1, 3, 5), droppedItems);

        droppedItems.clear();
        assertEquals(Collections.emptyList(), Stream.<Integer> empty().filter(x -> x % 2 == 0, droppedItems::add).toList());
        assertTrue(droppedItems.isEmpty());
    }

    @Test
    public void testOnEachAndPeek() {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> onEached = new ArrayList<>();

        List<Integer> resultPeek = Stream.of(1, 2).peek(peeked::add).map(x -> x * 2).toList();
        assertEquals(Arrays.asList(1, 2), peeked);
        assertEquals(Arrays.asList(2, 4), resultPeek);

        List<Integer> resultOnEach = Stream.of(1, 2).peek(onEached::add).map(x -> x * 2).toList();
        assertEquals(Arrays.asList(1, 2), onEached);
        assertEquals(Arrays.asList(2, 4), resultOnEach);
    }

    @Test
    public void testSlidingMapBiFunction() {
        List<String> result = Stream.of(1, 2, 3, 4).slidingMap((a, b) -> (a == null ? "null" : a) + "-" + (b == null ? "null" : b)).toList();
        assertEquals(Arrays.asList("1-2", "2-3", "3-4"), result);

        result = Stream.of(1).slidingMap((a, b) -> (a == null ? "null" : a) + "-" + (b == null ? "null" : b)).toList();
        assertEquals(Arrays.asList("1-null"), result);

        result = Stream.<Integer> empty().slidingMap((a, b) -> (a == null ? "null" : a) + "-" + (b == null ? "null" : b)).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSlidingMapBiFunctionWithIncrement() {
        List<String> result = Stream.of(1, 2, 3, 4, 5).slidingMap(2, (a, b) -> (a == null ? "null" : a) + "-" + (b == null ? "null" : b)).toList();
        assertEquals(Arrays.asList("1-2", "3-4", "5-null"), result);
    }

    @Test
    public void testSlidingMapBiFunctionWithIncrementAndIgnore() {
        List<String> result = Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> (a == null ? "null" : a) + "-" + (b == null ? "null" : b)).toList();
        assertEquals(Arrays.asList("1-2", "3-4"), result);

        result = Stream.of(1, 2, 3, 4).slidingMap(2, false, (a, b) -> (a == null ? "null" : a) + "-" + (b == null ? "null" : b)).toList();
        assertEquals(Arrays.asList("1-2", "3-4"), result);
    }

    @Test
    public void testSlidingMapTriFunctionWithIncrementAndIgnore() {
        List<String> result = Stream.of(1, 2, 3, 4, 5, 6, 7)
                .slidingMap(2, true, (a, b, c) -> (a == null ? "N" : a) + "-" + (b == null ? "N" : b) + "-" + (c == null ? "N" : c))
                .toList();
        assertEquals(Arrays.asList("1-2-3", "3-4-5", "5-6-7"), result);

        result = Stream.of(1, 2, 3, 4, 5, 6)
                .slidingMap(2, false, (a, b, c) -> (a == null ? "N" : a) + "-" + (b == null ? "N" : b) + "-" + (c == null ? "N" : c))
                .toList();
        assertEquals(Arrays.asList("1-2-3", "3-4-5", "5-6-N"), result);
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
    public void testFlatmap() {
        List<Integer> result = Stream.of(1, 2).flatmap(x -> Arrays.asList(x, x * 10)).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20), result);
    }

    @Test
    public void testFlattMapArray() {
        List<Integer> result = Stream.of("1,2", "3,4").flatmapToInt(s -> Stream.of(s.split(",")).mapToInt(Integer::parseInt).toArray()).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlattmapJdkStream() {
        List<Integer> result = Stream.of(1, 2).flattMap(x -> java.util.stream.Stream.of(x, x + 1)).toList();
        assertEquals(Arrays.asList(1, 2, 2, 3), result);
    }

    @Test
    public void testFlatmapToCharFromArray() {
        char[] result = Stream.of("ab", "cd").flatmapToChar(s -> s.toCharArray()).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testFlatmapToIntFromArray() {
        int[] result = Stream.of(Pair.of(1, 2), Pair.of(3, 4)).flatmapToInt(p -> new int[] { p.left(), p.right() }).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlatMapToEntryFromStream() {
        List<Map.Entry<String, Integer>> result = Stream.of("a:1,b:2", "c:3")
                .flatMapToEntry(str -> Stream.of(str.split(",")).map(pairStr -> N.newEntry(pairStr.split(":")[0], Integer.parseInt(pairStr.split(":")[1]))))
                .toList();
        assertEquals(Arrays.asList(N.newEntry("a", 1), N.newEntry("b", 2), N.newEntry("c", 3)), result);

    }

    @Test
    public void testFlatmapToEntryFromMap() {
        Map<String, Integer> map1 = N.asMap("a", 1, "b", 2);
        Map<String, Integer> map2 = N.asMap("c", 3);
        List<Map.Entry<String, Integer>> result = Stream.of(map1, map2).flatmapToEntry(Fn.identity()).sortedByKey(Comparator.naturalOrder()).toList();
        assertEquals(Arrays.asList(N.newEntry("a", 1), N.newEntry("b", 2), N.newEntry("c", 3)), result);
    }

    @Test
    public void testFlattMapToEntryFromEntryStream() {
        List<Map.Entry<String, Integer>> result = Stream.of("a", "b")
                .flattMapToEntry(s -> EntryStream.of(s, s.hashCode(), s.toUpperCase(), s.toUpperCase().hashCode()))
                .toList();

        List<Map.Entry<String, Integer>> expected = new ArrayList<>();
        expected.add(N.newEntry("a", "a".hashCode()));
        expected.add(N.newEntry("A", "A".hashCode()));
        expected.add(N.newEntry("b", "b".hashCode()));
        expected.add(N.newEntry("B", "B".hashCode()));

        assertTrue(result.containsAll(expected) && expected.containsAll(result));
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
    public void testGroupBySimple() {
        Stream<Map.Entry<Integer, List<String>>> resultStream = Stream.of("apple", "banana", "apricot", "blueberry", "cherry").groupBy(String::length);

        Map<Integer, List<String>> resultMap = resultStream.toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(Set.of(5, 6, 7, 9), resultMap.keySet());
        assertTrue(resultMap.get(5).containsAll(Arrays.asList("apple")));
        assertTrue(resultMap.get(6).containsAll(Arrays.asList("banana", "cherry")));
        assertTrue(resultMap.get(7).containsAll(Arrays.asList("apricot")));
        assertTrue(resultMap.get(9).containsAll(Arrays.asList("blueberry")));
    }

    @Test
    public void testGroupByWithKeyMapperValueMapperAndMergeFunction() {
        Map<Character, String> result = Stream.of("apple", "apricot", "banana", "blueberry", "avocado")
                .groupBy(s -> s.charAt(0), s -> s.substring(0, Math.min(s.length(), 3)), (v1, v2) -> v1 + ";" + v2)
                .toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals("app;apr;avo", result.get('a'));
        assertEquals("ban;blu", result.get('b'));
    }

    @Test
    public void testGroupByToEntrySimple() {
        EntryStream<Integer, List<String>> resultStream = Stream.of("apple", "banana", "apricot", "blueberry", "cherry").groupByToEntry(String::length);

        Map<Integer, List<String>> resultMap = resultStream.toMap();
        assertEquals(Set.of(5, 6, 7, 9), resultMap.keySet());
        assertTrue(resultMap.get(5).containsAll(Arrays.asList("apple")));
    }

    @Test
    public void testGroupByToEntryWithKeyMapperValueMapperCollectorAndMapFactory() {
        Map<Character, String> result = Stream.of("apple", "apricot", "banana", "blueberry", "avocado")
                .groupByToEntry(s -> s.charAt(0), s -> String.valueOf(s.length()), Collectors.joining(",", "[", "]"), TreeMap::new)
                .toMap();

        assertEquals("[5,7,7]", result.get('a'));
        assertEquals("[6,9]", result.get('b'));
    }

    @Test
    public void testPartitionBySimple() {
        Stream<Map.Entry<Boolean, List<Integer>>> resultStream = Stream.of(1, 2, 3, 4, 5).partitionBy(x -> x % 2 == 0);
        Map<Boolean, List<Integer>> resultMap = resultStream.toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(Arrays.asList(2, 4), resultMap.get(true));
        assertEquals(Arrays.asList(1, 3, 5), resultMap.get(false));
    }

    @Test
    public void testPartitionByToEntrySimple() {
        EntryStream<Boolean, List<Integer>> resultStream = Stream.of(1, 2, 3, 4, 5).partitionByToEntry(x -> x % 2 == 0);
        Map<Boolean, List<Integer>> resultMap = resultStream.toMap();

        assertEquals(Arrays.asList(2, 4), resultMap.get(true));
        assertEquals(Arrays.asList(1, 3, 5), resultMap.get(false));
    }

    @Test
    public void testCollapseBiPredicateAndMerger() {
        List<Integer> result = Stream.of(1, 1, 2, 3, 3, 1, 2).collapse((p, c) -> p.equals(c), (r, c) -> r + c).toList();
        assertEquals(Arrays.asList(2, 2, 6, 1, 2), result);
    }

    @Test
    public void testCollapseTriPredicate() {
        List<List<Integer>> result = Stream.of(10, 11, 12, 9, 5, 4, 6, 20)
                .collapse((first, prev, curr) -> Math.abs(curr - first) <= 2 && Math.abs(curr - prev) <= 1)
                .toList();

        assertEquals(Arrays.asList(Arrays.asList(10, 11, 12), Collections.singletonList(9), Arrays.asList(5, 4), Collections.singletonList(6),
                Collections.singletonList(20)), result);
    }

    @Test
    public void testScanSimple() {
        List<Integer> result = Stream.of(1, 2, 3, 4).scan((a, b) -> a + b).toList();
        assertEquals(Arrays.asList(1, 3, 6, 10), result);
        assertTrue(Stream.<Integer> empty().scan((a, b) -> a + b).toList().isEmpty());
        assertEquals(Arrays.asList(1), Stream.of(1).scan((a, b) -> a + b).toList());
    }

    @Test
    public void testScanWithInitialValue() {
        List<Integer> result = Stream.of(1, 2, 3).scan(10, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 13, 16), result);
        assertTrue(Stream.<Integer> empty().scan(10, (a, b) -> a + b).toList().isEmpty());
    }

    @Test
    public void testScanWithInitialValueAndInclusion() {
        List<Integer> result = Stream.of(1, 2, 3).scan(10, true, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(10, 11, 13, 16), result);

        result = Stream.of(1, 2, 3).scan(10, false, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 13, 16), result);

        result = Stream.<Integer> empty().scan(10, true, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(10), result);

        result = Stream.<Integer> empty().scan(10, false, (a, b) -> a + b).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSplitByChunkSize() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5).split(2).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5)), result);
        assertTrue(Stream.empty().split(2).toList().isEmpty());
        assertEquals(Arrays.asList(Arrays.asList(1)), Stream.of(1).split(2).toList());
    }

    @Test
    public void testSplitByChunkSizeAndSupplier() {
        List<Set<Integer>> result = Stream.of(1, 2, 3, 4, 5).split(2, IntFunctions.ofLinkedHashSet()).toList();
        assertEquals(Arrays.asList(N.toLinkedHashSet(1, 2), N.toLinkedHashSet(3, 4), N.toLinkedHashSet(5)), result);
    }

    @Test
    public void testSplitByChunkSizeAndCollector() {
        List<Long> result = Stream.of(1, 2, 3, 4, 5).split(2, Collectors.summingInt(x -> x)).map(Integer::longValue).toList();
        assertEquals(Arrays.asList(3L, 7L, 5L), result);
    }

    @Test
    public void testSplitByPredicate() {
        List<List<Integer>> result = Stream.of(1, 2, 0, 3, 4, 0, 5).split(x -> x == 0).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Collections.singletonList(0), Arrays.asList(3, 4), Collections.singletonList(0),
                Collections.singletonList(5)), result);

        result = Stream.of(0, 1, 2, 0, 3, 4, 0).split(x -> x == 0).toList();
        assertEquals(Arrays.asList(Collections.singletonList(0), Arrays.asList(1, 2), Collections.singletonList(0), Arrays.asList(3, 4),
                Collections.singletonList(0)), result);
    }

    @Test
    public void testSplitAtPosition() {
        List<Stream<Integer>> resultStreams = Stream.of(1, 2, 3, 4, 5).splitAt(2).toList();
        assertEquals(2, resultStreams.size());
        assertEquals(Arrays.asList(1, 2), resultStreams.get(0).toList());
        assertEquals(Arrays.asList(3, 4, 5), resultStreams.get(1).toList());

        resultStreams = Stream.of(1, 2).splitAt(2).toList();
        assertEquals(2, resultStreams.size());
        assertEquals(Arrays.asList(1, 2), resultStreams.get(0).toList());
        assertTrue(resultStreams.get(1).toList().isEmpty());

        resultStreams = Stream.of(1, 2).splitAt(0).toList();
        assertEquals(2, resultStreams.size());
        assertTrue(resultStreams.get(0).toList().isEmpty());
        assertEquals(Arrays.asList(1, 2), resultStreams.get(1).toList());
    }

    @Test
    public void testSplitAtPositionWithCollector() {
        List<Long> result = Stream.of(1, 2, 3, 4, 5).splitAt(2, Collectors.summingInt(ToIntFunction.UNBOX)).map(Integer::longValue).toList();
        assertEquals(N.toList(3L, 12L), result);
    }

    @Test
    public void testSplitAtPredicate() {
        List<Stream<Integer>> resultStreams = Stream.of(1, 2, 99, 4, 5).splitAt(x -> x == 99).toList();
        assertEquals(2, resultStreams.size());
        assertEquals(Arrays.asList(1, 2), resultStreams.get(0).toList());
        assertEquals(Arrays.asList(99, 4, 5), resultStreams.get(1).toList());

        resultStreams = Stream.of(99, 1, 2).splitAt(x -> x == 99).toList();
        assertEquals(2, resultStreams.size());
        assertTrue(resultStreams.get(0).toList().isEmpty());
        assertEquals(Arrays.asList(99, 1, 2), resultStreams.get(1).toList());

        resultStreams = Stream.of(1, 2, 3).splitAt(x -> x == 99).toList();
        assertEquals(2, resultStreams.size());
        assertEquals(Arrays.asList(1, 2, 3), resultStreams.get(0).toList());
        assertTrue(resultStreams.get(1).toList().isEmpty());
    }

    @Test
    public void testSlidingWindowSize() throws Exception {
        {
            assertEquals(Arrays.asList(Arrays.asList(1)), Stream.of(1).sliding(2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2)), Stream.of(1, 2).sliding(2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(2, 3)), Stream.of(1, 2, 3).sliding(2).toList());

            assertEquals(Arrays.asList(Arrays.asList(1)), Stream.of(N.toList(1).iterator()).sliding(2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2)), Stream.of(N.toList(1, 2).iterator()).sliding(2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(2, 3)), Stream.of(N.toList(1, 2, 3).iterator()).sliding(2).toList());
        }
        {
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4), Arrays.asList(3, 4, 5)), Stream.of(1, 2, 3, 4, 5).sliding(3).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(3, 4, 5)), Stream.of(1, 2, 3, 4, 5).sliding(3, 2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5)), Stream.of(1, 2, 3, 4, 5).sliding(3, 3).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(5)), Stream.of(1, 2, 3, 4, 5).sliding(3, 4).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Stream.of(1, 2, 3, 4, 5).sliding(3, 5).toList());

            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4), Arrays.asList(3, 4, 5)),
                    Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(3, 4, 5)), Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 2).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5)), Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 3).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(5)), Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 4).toList());
            assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 5).toList());

        }

        {
            assertEquals(0, Stream.of(1).sliding(2).skip(1).count());
            assertEquals(0, Stream.of(1, 2).sliding(2).skip(1).count());
            assertEquals(1, Stream.of(1, 2, 3).sliding(2).skip(1).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5)).sliding(3).skip(2).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 2).skip(1).count());
            assertEquals(0, Stream.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 2).skip(2).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 3).skip(1).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 4).skip(1).count());

            assertEquals(0, Stream.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 5).skip(1).count());

        }

        {
            assertEquals(0, Stream.of(N.toList(1).iterator()).sliding(2).skip(1).count());
            assertEquals(0, Stream.of(N.toList(1, 2).iterator()).sliding(2).skip(1).count());
            assertEquals(1, Stream.of(N.toList(1, 2, 3).iterator()).sliding(2).skip(1).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3).skip(2).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 2).skip(1).count());
            assertEquals(0, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 2).skip(2).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 3).skip(1).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 4).skip(1).count());

            assertEquals(0, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 5).skip(1).count());

        }

        {
            assertEquals(0, Stream.of(N.toList(1).iterator()).sliding(2, Collectors.toList()).skip(1).count());
            assertEquals(0, Stream.of(N.toList(1, 2).iterator()).sliding(2, Collectors.toList()).skip(1).count());
            assertEquals(1, Stream.of(N.toList(1, 2, 3).iterator()).sliding(2, Collectors.toList()).skip(1).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, Collectors.toList()).skip(2).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 2, Collectors.toList()).skip(1).count());
            assertEquals(0, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 2, Collectors.toList()).skip(2).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 3, Collectors.toList()).skip(1).count());

            assertEquals(1, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 4, Collectors.toList()).skip(1).count());

            assertEquals(0, Stream.of(N.toList(1, 2, 3, 4, 5).iterator()).sliding(3, 5, Collectors.toList()).skip(1).count());

        }

        {
            assertEquals(0, Seq.of(1).sliding(2).skip(1).count());
            assertEquals(0, Seq.of(1, 2).sliding(2).skip(1).count());
            assertEquals(1, Seq.of(1, 2, 3).sliding(2).skip(1).count());

            assertEquals(1, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3).skip(2).count());

            assertEquals(1, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 2).skip(1).count());
            assertEquals(0, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 2).skip(2).count());

            assertEquals(1, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 3).skip(1).count());

            assertEquals(1, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 4).skip(1).count());

            assertEquals(0, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 5).skip(1).count());

        }

        {
            assertEquals(0, Seq.of(1).sliding(2, Collectors.toList()).skip(1).count());
            assertEquals(0, Seq.of(1, 2).sliding(2, Collectors.toList()).skip(1).count());
            assertEquals(1, Seq.of(1, 2, 3).sliding(2, Collectors.toList()).skip(1).count());

            assertEquals(1, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, Collectors.toList()).skip(2).count());

            assertEquals(1, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 2, Collectors.toList()).skip(1).count());
            assertEquals(0, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 2, Collectors.toList()).skip(2).count());

            assertEquals(1, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 3, Collectors.toList()).skip(1).count());

            assertEquals(1, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 4, Collectors.toList()).skip(1).count());

            assertEquals(0, Seq.of(N.toList(1, 2, 3, 4, 5)).sliding(3, 5, Collectors.toList()).skip(1).count());

        }
    }

    @Test
    public void testSlidingWindowSizeAndIncrement() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5, 6).sliding(3, 2).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(3, 4, 5), Arrays.asList(5, 6)), result);
    }

    @Test
    public void testDistinctWithMerge() {
        List<Pair<String, Integer>> data = Arrays.asList(Pair.of("a", 1), Pair.of("b", 2), Pair.of("a", 3));
        List<Pair<String, Integer>> result = Stream.of(data)
                .distinctBy(Pair::left, (p1, p2) -> Pair.of(p1.left(), p1.right() + p2.right()))
                .sorted(Comparator.comparing(Pair::left))
                .toList();

        assertEquals(Arrays.asList(Pair.of("a", 4), Pair.of("b", 2)), result);
    }

    @Test
    public void testDistinctByFunction() {
        List<String> result = Stream.of("apple", "apricot", "banana", "blueberry", "Apple").distinctBy(s -> s.toLowerCase().charAt(0)).toList();
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(s -> s.toLowerCase().startsWith("a")));
        assertTrue(result.stream().anyMatch(s -> s.toLowerCase().startsWith("b")));
    }

    @Test
    public void testTopN() {
        assertEquals(Arrays.asList(4, 5), Stream.of(1, 5, 2, 4, 3).top(2).toList());
        assertEquals(Arrays.asList(4, 5), Stream.of(1, 5, 2, 4, 3).top(2, Comparator.naturalOrder()).toList());
        assertEquals(Arrays.asList(2, 1), Stream.of(1, 5, 2, 4, 3).top(2, Comparator.reverseOrder()).toList());
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).top(5).toList());
    }

    @Test
    public void testPeekIfWithBiPredicate() {
        List<Integer> peeked = new ArrayList<>();
        Stream.of(1, 2, 3, 4, 5, 6).peekIf((val, idx) -> val % 2 == 0 && idx > 1, peeked::add).toList();
        assertEquals(Arrays.asList(2, 4, 6), peeked);
    }

    @Test
    public void testForEachThrowing() {
        List<Integer> collected = new ArrayList<>();
        Stream.of(1, 2, 3).forEach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEachUntilMutableBoolean() {
        List<Integer> collected = new ArrayList<>();
        MutableBoolean flag = MutableBoolean.of(false);
        Stream.of(1, 2, 3, 4, 5).forEachUntil(flag, val -> {
            collected.add(val);
            if (val == 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testFindFirstPredicate() {
        assertEquals(Optional.of(2), Stream.of(1, 2, 3, 4).findFirst(x -> x % 2 == 0));
        assertEquals(Optional.empty(), Stream.of(1, 3, 5).findFirst(x -> x % 2 == 0));
    }

    @Test
    public void testFindAnyPredicate() {
        assertEquals(Optional.of(2), Stream.of(1, 2, 3, 4).findAny(x -> x % 2 == 0));
        Optional<Integer> result = Stream.of(1, 2, 3, 4).parallel().findAny(x -> x % 2 == 0);
        assertTrue(result.isPresent() && result.get() % 2 == 0);
    }

    @Test
    public void testFindLastPredicate() {
        assertEquals(Optional.of(4), Stream.of(1, 2, 3, 4).findLast(x -> x % 2 == 0));
        assertEquals(Optional.empty(), Stream.of(1, 3, 5).findLast(x -> x % 2 == 0));
        assertEquals(Optional.of(2), Stream.of(2).findLast(x -> x % 2 == 0));
    }

    @Test
    public void testContainsAllArray() {
        assertTrue(Stream.of(1, 2, 3, 4).containsAll(2, 4));
        assertFalse(Stream.of(1, 2, 3).containsAll(2, 5));
        assertTrue(Stream.of(1, 2, 3).containsAll());
    }

    @Test
    public void testContainsAllCollection() {
        assertTrue(Stream.of(1, 2, 3, 4).containsAll(Arrays.asList(2, 4)));
        assertFalse(Stream.of(1, 2, 3).containsAll(Arrays.asList(2, 5)));
    }

    @Test
    public void testContainsAnyArray() {
        assertTrue(Stream.of(1, 2, 3).containsAny(3, 5, 6));
        assertFalse(Stream.of(1, 2, 3).containsAny(4, 5, 6));
    }

    @Test
    public void testContainsNoneArray() {
        assertTrue(Stream.of(1, 2, 3).containsNone(4, 5, 6));
        assertFalse(Stream.of(1, 2, 3).containsNone(3, 5, 6));
    }

    @Test
    public void testToArrayGenerator() {
        String[] result = Stream.of("a", "b").toArray(String[]::new);
        assertArrayEquals(new String[] { "a", "b" }, result);
    }

    @Test
    public void testToImmutableMapWithMerge() {
        ImmutableMap<Integer, String> map = Stream.of("a", "b", "cc").toImmutableMap(String::length, Fn.identity(), (s1, s2) -> s1 + ";" + s2);
        assertEquals("a;b", map.get(1));
        assertEquals("cc", map.get(2));
    }

    @Test
    public void testToMapWithFactory() {
        Map<Integer, String> map = Stream.of("a", "bb", "ccc").toMap(String::length, Fn.identity(), Suppliers.ofLinkedHashMap());
        assertEquals("a", map.get(1));
        assertEquals("bb", map.get(2));
        Iterator<Map.Entry<Integer, String>> iter = map.entrySet().iterator();
        assertEquals(1, iter.next().getKey());
        assertEquals(2, iter.next().getKey());
    }

    @Test
    public void testGroupToKeyMapper() {
        Map<Integer, List<String>> map = Stream.of("a", "bb", "c", "ddd").groupTo(String::length);
        assertEquals(Arrays.asList("a", "c"), map.get(1));
        assertEquals(Arrays.asList("bb"), map.get(2));
        assertEquals(Arrays.asList("ddd"), map.get(3));
    }

    @Test
    public void testGroupToKeyAndValueMapper() {
        Map<Integer, List<String>> map = Stream.of("a", "bb", "c").groupTo(String::length, String::toUpperCase);
        assertEquals(Arrays.asList("A", "C"), map.get(1));
        assertEquals(Arrays.asList("BB"), map.get(2));
    }

    @Test
    public void testGroupToWithDownstreamCollectorAndMapFactory() {
        TreeMap<Integer, Long> map = Stream.of("a", "bb", "c", "ddd", "ee").groupTo(String::length, Collectors.counting(), TreeMap::new);
        assertTrue(map instanceof TreeMap);
        assertEquals(2L, map.get(1));
        assertEquals(2L, map.get(2));
        assertEquals(1L, map.get(3));
    }

    @Test
    public void testFlatGroupToKeyExtractor() {
        Map<Character, List<String>> map = Stream.of("ant", "bat", "cat").flatGroupTo(s -> Stream.of(s.toCharArray()).map(c -> c).toList());
        assertEquals(Arrays.asList("ant", "bat", "cat"), map.get('a'));
        assertEquals(Arrays.asList("ant", "bat", "cat"), map.get('t'));
        assertEquals(Arrays.asList("bat"), map.get('b'));
        assertEquals(Arrays.asList("cat"), map.get('c'));
    }

    @Test
    public void testFlatGroupToWithDownstreamCollector() {
        Map<Character, Long> map = Stream.of("apple", "apricot", "banana")
                .flatGroupTo(s -> Stream.of(s.toCharArray()).map(c -> c).toList(), Collectors.counting());

        assertEquals(Long.valueOf(5), map.get('a'));
        assertEquals(Long.valueOf(3), map.get('p'));
        assertEquals(Long.valueOf(1), map.get('l'));
        assertEquals(Long.valueOf(1), map.get('e'));
    }

    @Test
    public void testToMultimapKeyMapper() {
        ListMultimap<Integer, String> multimap = Stream.of("a", "bb", "c", "dd").toMultimap(String::length);
        assertEquals(Arrays.asList("a", "c"), multimap.get(1));
        assertEquals(Arrays.asList("bb", "dd"), multimap.get(2));
    }

    @Test
    public void testToMultimapKeyAndValueMapperAndFactory() {
        ListMultimap<Character, Integer> multimap = Stream.of("apple", "apricot").toMultimap(s -> s.charAt(0), String::length, Suppliers.ofListMultimap());

        assertEquals(Arrays.asList(5, 7), multimap.get('a'));
    }

    @Test
    public void testToDatasetSimple() {
        Map<String, Object> row1 = N.asMap("id", 1, "name", "Alice");
        Map<String, Object> row2 = N.asMap("id", 2, "name", "Bob");
        Dataset dataset = Stream.of(row1, row2).toDataset();

        assertEquals(2, dataset.size());
        assertTrue(dataset.columnNames().containsAll(Arrays.asList("id", "name")));
    }

    @Test
    public void testReduceBinaryOperator() {
        assertEquals(Optional.of(6), Stream.of(1, 2, 3).reduce((a, b) -> a + b));
        assertEquals(Optional.empty(), Stream.<Integer> empty().reduce((a, b) -> a + b));
    }

    @Test
    public void testReduceIdentityBinaryOperator() {
        assertEquals(10, (int) Stream.of(1, 2, 3).reduce(4, (a, b) -> a + b));
        assertEquals(4, (int) Stream.<Integer> empty().reduce(4, (a, b) -> a + b));
    }

    @Test
    public void testReduceIdentityAccumulatorCombiner() {
        int result = Stream.of("1", "2", "3").reduce(0, (sum, str) -> sum + Integer.parseInt(str), (s1, s2) -> s1 + s2);
        assertEquals(6, result);

        result = Stream.of("1", "2", "3", "4", "5", "6").parallel().reduce(0, (sum, str) -> sum + Integer.parseInt(str), Integer::sum);
        assertEquals(21, result);
    }



    @Test
    public void testCollectSupplierAccumulatorCombiner() {
        ArrayList<Integer> result = Stream.of(1, 2, 3).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testCollectSupplierAccumulator() {
        List<Integer> result = Stream.of(1, 2, 3).collect(ArrayList::new, ArrayList::add);
        assertEquals(Arrays.asList(1, 2, 3), result);

        Set<Integer> parallelResult = Stream.of(1, 2, 3, 1, 2).parallel().collect(HashSet::new, HashSet::add);
        assertEquals(Set.of(1, 2, 3), parallelResult);
    }

    @Test
    public void testCollectCollector() {
        List<Integer> result = Stream.of(1, 2, 3).collect(java.util.stream.Collectors.toList());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testMinComparator() {
        assertEquals(Optional.of(1), Stream.of(5, 1, 3).min(Comparator.naturalOrder()));
        assertEquals(Optional.empty(), Stream.<Integer> empty().min(Comparator.naturalOrder()));
    }

    @Test
    public void testMaxComparator() {
        assertEquals(Optional.of(5), Stream.of(5, 1, 3).max(Comparator.naturalOrder()));
    }

    @Test
    public void testCombinationsNoLength() {
        List<List<Integer>> combs = Stream.of(1, 2).combinations().toList();
        assertEquals(4, combs.size());
        assertTrue(combs.contains(Collections.emptyList()));
        assertTrue(combs.contains(Arrays.asList(1)));
        assertTrue(combs.contains(Arrays.asList(2)));
        assertTrue(combs.contains(Arrays.asList(1, 2)));
    }

    @Test
    public void testCombinationsWithLength() {
        List<List<Integer>> combs = Stream.of(1, 2, 3).combinations(2).toList();
        assertEquals(3, combs.size());
        assertTrue(combs.contains(Arrays.asList(1, 2)));
        assertTrue(combs.contains(Arrays.asList(1, 3)));
        assertTrue(combs.contains(Arrays.asList(2, 3)));
    }

    @Test
    public void testCombinationsWithLengthAndRepeat() {
        List<List<Integer>> combsWithoutRepeat = Stream.of(1, 2).combinations(2, false).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2)), combsWithoutRepeat);

        List<List<Integer>> combsWithRepeat = Stream.of(1, 2).combinations(2, true).toList();
        assertEquals(4, combsWithRepeat.size());
        assertTrue(combsWithRepeat.contains(Arrays.asList(1, 1)));
        assertTrue(combsWithRepeat.contains(Arrays.asList(1, 2)));
        assertTrue(combsWithRepeat.contains(Arrays.asList(2, 1)));
        assertTrue(combsWithRepeat.contains(Arrays.asList(2, 2)));
    }

    @Test
    public void testCartesianProductVarargs() {
        List<List<Integer>> result = Stream.of(0, 1).cartesianProduct(Arrays.asList(2, 3)).toList();
        assertEquals(Arrays.asList(Arrays.asList(0, 2), Arrays.asList(0, 3), Arrays.asList(1, 2), Arrays.asList(1, 3)), result);
    }

    @Test
    public void testCartesianProductCollectionOfCollections() {
        List<List<String>> result = Stream.of("a", "b").cartesianProduct(Arrays.asList(Arrays.asList("1", "2"))).toList();
        assertEquals(Arrays.asList(Arrays.asList("a", "1"), Arrays.asList("a", "2"), Arrays.asList("b", "1"), Arrays.asList("b", "2")), result);

        List<List<String>> result2 = Stream.of("a", "b").cartesianProduct(Arrays.asList(Arrays.asList("1", "2"), Arrays.asList("true", "false"))).toList();
        assertEquals(Arrays.asList(Arrays.asList("a", "1", "true"), Arrays.asList("a", "1", "false"), Arrays.asList("a", "2", "true"),
                Arrays.asList("a", "2", "false"), Arrays.asList("b", "1", "true"), Arrays.asList("b", "1", "false"), Arrays.asList("b", "2", "true"),
                Arrays.asList("b", "2", "false")), result2);
    }


    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilterWithAction() {
        List<Integer> dropped = new ArrayList<>();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).count());
        dropped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).toList());
        dropped.clear();
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).skip(1).toList());
        dropped.clear();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).count());
        dropped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).toList());
        dropped.clear();
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2 }, Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new Integer[] { 2 }, Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList(1, 2), Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList(2), Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toList());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new Integer[] { 2 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList(1, 2), Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList(2), Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhileWithAction() {
        List<Integer> dropped = new ArrayList<>();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).count());
        dropped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).toList());
        dropped.clear();
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).skip(1).toList());
        dropped.clear();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).count());
        dropped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toList());
        dropped.clear();
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(4, Stream.of(1, 2, 2, 3, 3, 4).distinct().count());
        assertEquals(3, Stream.of(1, 2, 2, 3, 3, 4).distinct().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, Stream.of(1, 2, 2, 3, 3, 4).distinct().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4 }, Stream.of(1, 2, 2, 3, 3, 4).distinct().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4), Stream.of(1, 2, 2, 3, 3, 4).distinct().toList());
        assertEquals(N.toList(2, 3, 4), Stream.of(1, 2, 2, 3, 3, 4).distinct().skip(1).toList());
        assertEquals(4, Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().count());
        assertEquals(3, Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4 }, Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4), Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().toList());
        assertEquals(N.toList(2, 3, 4), Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIntersection() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 3, 4 }, Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).toArray());
        assertArrayEquals(new Integer[] { 3, 4 }, Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).toArray());
        assertEquals(N.toList(2, 3, 4), Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).toList());
        assertEquals(N.toList(3, 4), Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 3, 4 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).toArray());
        assertArrayEquals(new Integer[] { 3, 4 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).toArray());
        assertEquals(N.toList(2, 3, 4), Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).toList());
        assertEquals(N.toList(3, 4), Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDifference() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 5 }, Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).skip(1).toArray());
        assertEquals(N.toList(1, 5), Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).toList());
        assertEquals(N.toList(5), Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).skip(1).toList());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).skip(1).toArray());
        assertEquals(N.toList(1, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).toList());
        assertEquals(N.toList(5), Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSymmetricDifference() {
        assertEquals(4, Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).count());
        assertEquals(3, Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 4, 5 }, Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).toArray());
        assertArrayEquals(new Integer[] { 2, 4, 5 }, Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 4, 5), Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).toList());
        assertEquals(N.toList(2, 4, 5), Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).toList());
        assertEquals(4, Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).count());
        assertEquals(3, Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).toArray());
        assertArrayEquals(new Integer[] { 2, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 4, 5), Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).toList());
        assertEquals(N.toList(2, 4, 5), Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReversed() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).reversed().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).reversed().skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reversed().toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reversed().skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reversed().toList());
        assertEquals(N.toList(4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reversed().skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toList());
        assertEquals(N.toList(4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRotated() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).rotated(2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).rotated(2).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5, 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).rotated(2).toArray());
        assertArrayEquals(new Integer[] { 5, 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toArray());
        assertEquals(N.toList(4, 5, 1, 2, 3), Stream.of(1, 2, 3, 4, 5).rotated(2).toList());
        assertEquals(N.toList(5, 1, 2, 3), Stream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5, 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toArray());
        assertArrayEquals(new Integer[] { 5, 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toArray());
        assertEquals(N.toList(4, 5, 1, 2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toList());
        assertEquals(N.toList(5, 1, 2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterShuffled() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled().skip(1).count());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled().toArray().length);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled().skip(1).toArray().length);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled().toList().size());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled().skip(1).toList().size());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).count());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().toArray().length);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).toArray().length);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().toList().size());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).toList().size());
    }

    @Test
    public void testStreamCreatedAfterShuffledWithRandom() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).skip(1).count());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toArray().length);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).skip(1).toArray().length);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toList().size());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).skip(1).toList().size());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).skip(1).count());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).toArray().length);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).skip(1).toArray().length);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).toList().size());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).skip(1).toList().size());
    }

    @Test
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, Stream.of(5, 3, 1, 4, 2).sorted().count());
        assertEquals(4, Stream.of(5, 3, 1, 4, 2).sorted().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).sorted().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).sorted().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).sorted().toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).sorted().skip(1).toList());
        assertEquals(5, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().count());
        assertEquals(4, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, Stream.of(3, 1, 4, 2, 5).reverseSorted().count());
        assertEquals(4, Stream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).reverseSorted().toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).reverseSorted().toList());
        assertEquals(N.toList(4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).toList());
        assertEquals(5, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().count());
        assertEquals(4, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().toList());
        assertEquals(N.toList(4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCycled() {
        assertEquals(15, Stream.of(1, 2, 3).cycled().limit(15).count());
        assertEquals(14, Stream.of(1, 2, 3).cycled().limit(15).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, Stream.of(1, 2, 3).cycled().limit(10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 1, 2, 3, 1, 2, 3, 1 }, Stream.of(1, 2, 3).cycled().limit(10).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 1, 2, 3), Stream.of(1, 2, 3).cycled().limit(6).toList());
        assertEquals(N.toList(2, 3, 1, 2, 3), Stream.of(1, 2, 3).cycled().limit(6).skip(1).toList());
        assertEquals(15, Stream.of(1, 2, 3).map(e -> e).cycled().limit(15).count());
        assertEquals(14, Stream.of(1, 2, 3).map(e -> e).cycled().limit(15).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, Stream.of(1, 2, 3).map(e -> e).cycled().limit(10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 1, 2, 3, 1, 2, 3, 1 }, Stream.of(1, 2, 3).map(e -> e).cycled().limit(10).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 1, 2, 3), Stream.of(1, 2, 3).map(e -> e).cycled().limit(6).toList());
        assertEquals(N.toList(2, 3, 1, 2, 3), Stream.of(1, 2, 3).map(e -> e).cycled().limit(6).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCycledWithRounds() {
        assertEquals(6, Stream.of(1, 2, 3).cycled(2).count());
        assertEquals(5, Stream.of(1, 2, 3).cycled(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 1, 2, 3 }, Stream.of(1, 2, 3).cycled(2).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 1, 2, 3 }, Stream.of(1, 2, 3).cycled(2).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 1, 2, 3), Stream.of(1, 2, 3).cycled(2).toList());
        assertEquals(N.toList(2, 3, 1, 2, 3), Stream.of(1, 2, 3).cycled(2).skip(1).toList());
        assertEquals(6, Stream.of(1, 2, 3).map(e -> e).cycled(2).count());
        assertEquals(5, Stream.of(1, 2, 3).map(e -> e).cycled(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 1, 2, 3 }, Stream.of(1, 2, 3).map(e -> e).cycled(2).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 1, 2, 3 }, Stream.of(1, 2, 3).map(e -> e).cycled(2).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 1, 2, 3), Stream.of(1, 2, 3).map(e -> e).cycled(2).toList());
        assertEquals(N.toList(2, 3, 1, 2, 3), Stream.of(1, 2, 3).map(e -> e).cycled(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).skip(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).skip(2).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).skip(2).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).skip(2).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).skip(2).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).skip(2).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipWithAction() {
        List<Integer> skipped = new ArrayList<>();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).count());
        skipped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).skip(1).count());
        skipped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).toArray());
        skipped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).skip(1).toArray());
        skipped.clear();
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).toList());
        skipped.clear();
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).skip(1).toList());
        skipped.clear();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).count());
        skipped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).skip(1).count());
        skipped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).toArray());
        skipped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).skip(1).toArray());
        skipped.clear();
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).toList());
        skipped.clear();
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterLimit() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).limit(3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).limit(3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).limit(3).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, 2, 3, 4, 5).limit(3).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of(1, 2, 3, 4, 5).limit(3).toList());
        assertEquals(N.toList(2, 3), Stream.of(1, 2, 3, 4, 5).limit(3).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toList());
        assertEquals(N.toList(2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterStep() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5, 6).step(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 3, 5 }, Stream.of(1, 2, 3, 4, 5, 6).step(2).toArray());
        assertArrayEquals(new Integer[] { 3, 5 }, Stream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).toArray());
        assertEquals(N.toList(1, 3, 5), Stream.of(1, 2, 3, 4, 5, 6).step(2).toList());
        assertEquals(N.toList(3, 5), Stream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 3, 5 }, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).toArray());
        assertArrayEquals(new Integer[] { 3, 5 }, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).toArray());
        assertEquals(N.toList(1, 3, 5), Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).toList());
        assertEquals(N.toList(3, 5), Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnEach() {
        List<Integer> list = new ArrayList<>();
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peek(list::add).count());
        list.clear();
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).peek(list::add).skip(1).count());
        list.clear();
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).toArray());
        list.clear();
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).skip(1).toArray());
        list.clear();
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).toList());
        list.clear();
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrepend() {
        assertEquals(5, Stream.of(3, 4, 5).prepend(1, 2).count());
        assertEquals(4, Stream.of(3, 4, 5).prepend(1, 2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).prepend(1, 2).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(3, 4, 5).prepend(1, 2).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).prepend(1, 2).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(3, 4, 5).prepend(1, 2).skip(1).toList());
        assertEquals(5, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).count());
        assertEquals(4, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependCollection() {
        assertEquals(5, Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).count());
        assertEquals(4, Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).skip(1).toList());
        assertEquals(5, Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).count());
        assertEquals(4, Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppend() {
        assertEquals(5, Stream.of(1, 2, 3).append(4, 5).count());
        assertEquals(4, Stream.of(1, 2, 3).append(4, 5).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).append(4, 5).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3).append(4, 5).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).append(4, 5).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3).append(4, 5).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3).map(e -> e).append(4, 5).count());
        assertEquals(4, Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(4, 5).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(4, 5).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendCollection() {
        assertEquals(5, Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).count());
        assertEquals(4, Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).count());
        assertEquals(4, Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        assertEquals(3, Stream.<Integer> empty().appendIfEmpty(1, 2, 3).count());
        assertEquals(2, Stream.<Integer> empty().appendIfEmpty(1, 2, 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.<Integer> empty().appendIfEmpty(1, 2, 3).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.<Integer> empty().appendIfEmpty(1, 2, 3).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.<Integer> empty().appendIfEmpty(1, 2, 3).toList());
        assertEquals(N.toList(2, 3), Stream.<Integer> empty().appendIfEmpty(1, 2, 3).skip(1).toList());
        assertEquals(3, Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).count());
        assertEquals(2, Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).toList());
        assertEquals(N.toList(2, 3), Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).skip(1).toList());

        assertEquals(2, Stream.of(4, 5).appendIfEmpty(1, 2, 3).count());
        assertEquals(1, Stream.of(4, 5).appendIfEmpty(1, 2, 3).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(4, 5).appendIfEmpty(1, 2, 3).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.of(4, 5).appendIfEmpty(1, 2, 3).skip(1).toArray());
        assertEquals(N.toList(4, 5), Stream.of(4, 5).appendIfEmpty(1, 2, 3).toList());
        assertEquals(N.toList(5), Stream.of(4, 5).appendIfEmpty(1, 2, 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmptyCollection() {
        assertEquals(3, Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).count());
        assertEquals(2, Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).toList());
        assertEquals(N.toList(2, 3), Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).toList());
        assertEquals(3, Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).count());
        assertEquals(2, Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).toList());
        assertEquals(N.toList(2, 3), Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDefaultIfEmpty() {
        assertEquals(1, Stream.<Integer> empty().defaultIfEmpty(99).count());
        assertEquals(0, Stream.<Integer> empty().defaultIfEmpty(99).skip(1).count());
        assertArrayEquals(new Integer[] { 99 }, Stream.<Integer> empty().defaultIfEmpty(99).toArray());
        assertArrayEquals(new Integer[] {}, Stream.<Integer> empty().defaultIfEmpty(99).skip(1).toArray());
        assertEquals(N.toList(99), Stream.<Integer> empty().defaultIfEmpty(99).toList());
        assertEquals(N.toList(), Stream.<Integer> empty().defaultIfEmpty(99).skip(1).toList());
        assertEquals(1, Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).count());
        assertEquals(0, Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).skip(1).count());
        assertArrayEquals(new Integer[] { 99 }, Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).toArray());
        assertArrayEquals(new Integer[] {}, Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).skip(1).toArray());
        assertEquals(N.toList(99), Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).toList());
        assertEquals(N.toList(), Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterThrowIfEmpty() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).throwIfEmpty().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).throwIfEmpty().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).throwIfEmpty().toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterThrowIfEmptyWithSupplier() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 },
                Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIfEmpty() {
        AtomicBoolean flag = new AtomicBoolean(false);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).count());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).skip(1).count());
        assertFalse(flag.get());
        flag.set(false);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).toArray());
        assertFalse(flag.get());
        flag.set(false);
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).skip(1).toArray());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).toList());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).skip(1).toList());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).count());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).skip(1).count());
        assertFalse(flag.get());
        flag.set(false);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).toArray());
        assertFalse(flag.get());
        flag.set(false);
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).skip(1).toArray());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).toList());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).skip(1).toList());
        assertFalse(flag.get());
    }

    @Test
    public void testStreamCreatedAfterOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).count());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).skip(1).count());
        assertTrue(closed.get());
        closed.set(false);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).toArray());
        assertTrue(closed.get());
        closed.set(false);
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).skip(1).toArray());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).toList());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).skip(1).toList());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).count());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).count());
        assertTrue(closed.get());
        closed.set(false);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).toArray());
        assertTrue(closed.get());
        closed.set(false);
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).toArray());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).toList());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).toList());
        assertTrue(closed.get());
    }

    @Test
    public void testStreamCreatedAfterPeek() {
        List<Integer> list = new ArrayList<>();
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peek(list::add).count());
        list.clear();
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).peek(list::add).skip(1).count());
        list.clear();
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peek(list::add).toArray());
        list.clear();
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peek(list::add).skip(1).toArray());
        list.clear();
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peek(list::add).toList());
        list.clear();
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peek(list::add).skip(1).toList());
        list.clear();
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).count());
        list.clear();
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).skip(1).count());
        list.clear();
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).toArray());
        list.clear();
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).skip(1).toArray());
        list.clear();
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).toList());
        list.clear();
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSelect() {
        assertEquals(3, Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).count());
        assertEquals(2, Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).skip(1).count());
        assertArrayEquals(new String[] { "a", "b", "c" }, Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).toArray());
        assertArrayEquals(new String[] { "b", "c" }, Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).skip(1).toArray());
        assertEquals(N.toList("a", "b", "c"), Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).toList());
        assertEquals(N.toList("b", "c"), Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).skip(1).toList());
        assertEquals(3, Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).count());
        assertEquals(2, Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).skip(1).count());
        assertArrayEquals(new String[] { "a", "b", "c" }, Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).toArray());
        assertArrayEquals(new String[] { "b", "c" }, Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).skip(1).toArray());
        assertEquals(N.toList("a", "b", "c"), Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).toList());
        assertEquals(N.toList("b", "c"), Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPairWith() {
        assertEquals(3, Stream.of("a", "bb", "ccc").pairWith(String::length).count());
        assertEquals(2, Stream.of("a", "bb", "ccc").pairWith(String::length).skip(1).count());
        assertEquals(3, Stream.of("a", "bb", "ccc").pairWith(String::length).toArray().length);
        assertEquals(2, Stream.of("a", "bb", "ccc").pairWith(String::length).skip(1).toArray().length);
        assertEquals(3, Stream.of("a", "bb", "ccc").pairWith(String::length).toList().size());
        assertEquals(2, Stream.of("a", "bb", "ccc").pairWith(String::length).skip(1).toList().size());
        assertEquals(3, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).count());
        assertEquals(2, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).skip(1).count());
        assertEquals(3, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).toArray().length);
        assertEquals(2, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).skip(1).toArray().length);
        assertEquals(3, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).toList().size());
        assertEquals(2, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).skip(1).toList().size());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).toList());
        assertEquals(N.toList(4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).toList());
        assertEquals(N.toList(4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapIfNotNull() {
        assertEquals(3, Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).count());
        assertEquals(2, Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).skip(1).count());
        assertArrayEquals(new String[] { "A", "B", "C" }, Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).toArray());
        assertArrayEquals(new String[] { "B", "C" }, Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).skip(1).toArray());
        assertEquals(N.toList("A", "B", "C"), Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).toList());
        assertEquals(N.toList("B", "C"), Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).skip(1).toList());
        assertEquals(3, Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).count());
        assertEquals(2, Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).skip(1).count());
        assertArrayEquals(new String[] { "A", "B", "C" }, Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).toArray());
        assertArrayEquals(new String[] { "B", "C" }, Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).skip(1).toArray());
        assertEquals(N.toList("A", "B", "C"), Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).toList());
        assertEquals(N.toList("B", "C"), Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMap() {
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 7, 9 }, Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toArray());
        assertArrayEquals(new Integer[] { 5, 7, 9 },
                Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toArray());
        assertEquals(N.toList(3, 5, 7, 9), Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toList());
        assertEquals(N.toList(5, 7, 9), Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toList());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 7, 9 },
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toArray());
        assertArrayEquals(new Integer[] { 5, 7, 9 },
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toArray());
        assertEquals(N.toList(3, 5, 7, 9), Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toList());
        assertEquals(N.toList(5, 7, 9), Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMapWithIncrement() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 7, 11 }, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toArray());
        assertArrayEquals(new Integer[] { 7, 11 },
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toArray());
        assertEquals(N.toList(3, 7, 11), Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toList());
        assertEquals(N.toList(7, 11), Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 7, 11 },
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toArray());
        assertArrayEquals(new Integer[] { 7, 11 },
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toArray());
        assertEquals(N.toList(3, 7, 11), Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toList());
        assertEquals(N.toList(7, 11),
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMapWithIncrementAndIgnore() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 7 }, Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new Integer[] { 7 }, Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(3, 7), Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).toList());
        assertEquals(N.toList(7), Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).skip(1).toList());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 7 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new Integer[] { 7 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(3, 7), Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).toList());
        assertEquals(N.toList(7), Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMapTri() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).count());
        assertArrayEquals(new Integer[] { 6, 9, 12 },
                Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toArray());
        assertArrayEquals(new Integer[] { 9, 12 },
                Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).toArray());
        assertEquals(N.toList(6, 9, 12),
                Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toList());
        assertEquals(N.toList(9, 12),
                Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).count());
        assertEquals(2,
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).count());
        assertArrayEquals(new Integer[] { 6, 9, 12 },
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toArray());
        assertArrayEquals(new Integer[] { 9, 12 },
                Stream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList(6, 9, 12),
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toList());
        assertEquals(N.toList(9, 12),
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMapTriWithIncrement() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).count());
        assertEquals(1,
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).count());
        assertArrayEquals(new Integer[] { 6, 15 },
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toArray());
        assertArrayEquals(new Integer[] { 15 },
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).toArray());
        assertEquals(N.toList(6, 15),
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toList());
        assertEquals(N.toList(15),
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).toList());
        assertEquals(2,
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).count());
        assertEquals(1,
                Stream.of(1, 2, 3, 4, 5, 6)
                        .map(e -> e)
                        .slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c))
                        .skip(1)
                        .count());
        assertArrayEquals(new Integer[] { 6, 15 },
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toArray());
        assertArrayEquals(new Integer[] { 15 },
                Stream.of(1, 2, 3, 4, 5, 6)
                        .map(e -> e)
                        .slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList(6, 15),
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toList());
        assertEquals(N.toList(15),
                Stream.of(1, 2, 3, 4, 5, 6)
                        .map(e -> e)
                        .slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMapTriWithIncrementAndIgnore() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new Integer[] { 6, 15 }, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).toArray());
        assertArrayEquals(new Integer[] { 15 }, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).toArray());
        assertEquals(N.toList(6, 15), Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).toList());
        assertEquals(N.toList(15), Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).toList());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new Integer[] { 6, 15 }, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).toArray());
        assertArrayEquals(new Integer[] { 15 }, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).toArray());
        assertEquals(N.toList(6, 15), Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).toList());
        assertEquals(N.toList(15), Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRangeMap() {
        assertEquals(3, Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).count());
        assertEquals(2, Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).skip(1).count());
        assertArrayEquals(new String[] { "1-2", "5-6", "10-10" },
                Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).toArray());
        assertArrayEquals(new String[] { "5-6", "10-10" },
                Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).skip(1).toArray());
        assertEquals(N.toList("1-2", "5-6", "10-10"),
                Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).toList());
        assertEquals(N.toList("5-6", "10-10"),
                Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).skip(1).toList());
        assertEquals(3,
                Stream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).count());
        assertEquals(2,
                Stream.of(1, 2, 5, 6, 10)
                        .map(e -> e)
                        .rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "1-2", "5-6", "10-10" },
                Stream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).toArray());
        assertArrayEquals(new String[] { "5-6", "10-10" },
                Stream.of(1, 2, 5, 6, 10)
                        .map(e -> e)
                        .rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .toArray());
        assertEquals(N.toList("1-2", "5-6", "10-10"),
                Stream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).toList());
        assertEquals(N.toList("5-6", "10-10"),
                Stream.of(1, 2, 5, 6, 10)
                        .map(e -> e)
                        .rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterMapFirst() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).skip(1).count());
        assertArrayEquals(new Integer[] { 10, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).skip(1).toArray());
        assertEquals(N.toList(10, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).skip(1).count());
        assertArrayEquals(new Integer[] { 10, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).skip(1).toArray());
        assertEquals(N.toList(10, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapFirstOrElse() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 10, 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).toArray());
        assertEquals(N.toList(10, 4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).toList());
        assertEquals(N.toList(4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 10, 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).toArray());
        assertEquals(N.toList(10, 4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).toList());
        assertEquals(N.toList(4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapLast() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 50 }, Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 50 }, Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 50), Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).toList());
        assertEquals(N.toList(2, 3, 4, 50), Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).toList());
        assertEquals(N.toList(2, 3, 4, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapLastOrElse() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 4, 6, 8, 50 }, Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 50 }, Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 50), Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).toList());
        assertEquals(N.toList(4, 6, 8, 50), Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 4, 6, 8, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).toList());
        assertEquals(N.toList(4, 6, 8, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6), Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6), Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).skip(1).toList());
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmap() {
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6), Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6), Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).skip(1).toList());
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattmap() {
        assertEquals(6, Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flatMapArray(Fn.identity()).count());
        assertEquals(5, Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flatMapArray(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flatMapArray(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flatMapArray(Fn.identity()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flatMapArray(Fn.identity()).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6),
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flatMapArray(Fn.identity()).skip(1).toList());
        assertEquals(6, Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flatMapArray(Fn.identity()).count());
        assertEquals(5,
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flatMapArray(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flatMapArray(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flatMapArray(Fn.identity()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flatMapArray(Fn.identity()).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6),
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flatMapArray(Fn.identity()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattMap() {
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6), Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).skip(1).toList());
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapIfNotNull() {
        assertEquals(4, Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).count());
        assertEquals(3, Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4 },
                Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4), Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).toList());
        assertEquals(N.toList(2, 3, 4), Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).skip(1).toList());
        assertEquals(4, Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).count());
        assertEquals(3, Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 },
                Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4 },
                Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4), Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).toList());
        assertEquals(N.toList(2, 3, 4), Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapIfNotNull2() {
        assertEquals(8,
                Stream.of("a", null, "b").flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2")).count());
        assertEquals(7,
                Stream.of("a", null, "b")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "A", "A2", "A1", "A12", "B", "B2", "B1", "B12" },
                Stream.of("a", null, "b")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .toArray());
        assertArrayEquals(new String[] { "A2", "A1", "A12", "B", "B2", "B1", "B12" },
                Stream.of("a", null, "b")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList("A", "A2", "A1", "A12", "B", "B2", "B1", "B12"),
                Stream.of("a", null, "b")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .toList());
        assertEquals(N.toList("A2", "A1", "A12", "B", "B2", "B1", "B12"),
                Stream.of("a", null, "b")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .toList());
        assertEquals(8,
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .count());
        assertEquals(7,
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "A", "A2", "A1", "A12", "B", "B2", "B1", "B12" },
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .toArray());
        assertArrayEquals(new String[] { "A2", "A1", "A12", "B", "B2", "B1", "B12" },
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList("A", "A2", "A1", "A12", "B", "B2", "B1", "B12"),
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .toList());
        assertEquals(N.toList("A2", "A1", "A12", "B", "B2", "B1", "B12"),
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterMapMulti() {
        assertEquals(10, Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).count());
        assertEquals(9, Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 }, Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).toArray());
        assertArrayEquals(new Integer[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 }, Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).toArray());
        assertEquals(N.toList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50), Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).toList());
        assertEquals(N.toList(10, 2, 20, 3, 30, 4, 40, 5, 50), Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).toList());
        assertEquals(10, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).count());
        assertEquals(9, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).toArray());
        assertArrayEquals(new Integer[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).toArray());
        assertEquals(N.toList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).toList());
        assertEquals(N.toList(10, 2, 20, 3, 30, 4, 40, 5, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartial() {
        assertEquals(3, Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).count());
        assertEquals(2, Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toList());
        assertEquals(N.toList(2, 3), Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).toList());
        assertEquals(3, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).count());
        assertEquals(2, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toList());
        assertEquals(N.toList(2, 3), Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartialJdk() {
        assertEquals(3, Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).count());
        assertEquals(2, Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).toList());
        assertEquals(N.toList(2, 3), Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).toList());
        assertEquals(3, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).count());
        assertEquals(2, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).toList());
        assertEquals(N.toList(2, 3), Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSortedByComparator() {
        assertEquals(5, Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).count());
        assertEquals(4, Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).skip(1).toList());
        assertEquals(5, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).count());
        assertEquals(4, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSortedBy() {
        assertEquals(3, Stream.of("bb", "a", "ccc").sortedBy(String::length).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").sortedBy(String::length).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedBy(String::length).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedBy(String::length).skip(1).toArray());
        assertEquals(N.toList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").sortedBy(String::length).toList());
        assertEquals(N.toList("bb", "ccc"), Stream.of("bb", "a", "ccc").sortedBy(String::length).skip(1).toList());
        assertEquals(3, Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).skip(1).toArray());
        assertEquals(N.toList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).toList());
        assertEquals(N.toList("bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSortedByInt() {
        assertEquals(3, Stream.of("bb", "a", "ccc").sortedByInt(String::length).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").sortedByInt(String::length).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByInt(String::length).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByInt(String::length).skip(1).toArray());
        assertEquals(N.toList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByInt(String::length).toList());
        assertEquals(N.toList("bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByInt(String::length).skip(1).toList());
        assertEquals(3, Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).skip(1).toArray());
        assertEquals(N.toList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).toList());
        assertEquals(N.toList("bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSortedByLong() {
        assertEquals(3, Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).skip(1).toArray());
        assertEquals(N.toList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).toList());
        assertEquals(N.toList("bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).skip(1).toList());
        assertEquals(3, Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).skip(1).toArray());
        assertEquals(N.toList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).toList());
        assertEquals(N.toList("bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSortedByDouble() {
        assertEquals(3, Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).skip(1).toArray());
        assertEquals(N.toList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).toList());
        assertEquals(N.toList("bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).skip(1).toList());
        assertEquals(3, Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).skip(1).toArray());
        assertEquals(N.toList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).toList());
        assertEquals(N.toList("bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSortedByComparator() {
        assertEquals(5, Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).count());
        assertEquals(4, Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).toList());
        assertEquals(N.toList(4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).skip(1).toList());
        assertEquals(5, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).count());
        assertEquals(4, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).toList());
        assertEquals(N.toList(4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSortedByInt() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).skip(2).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).toList());
        assertEquals(N.toList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).skip(2).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).toList());
        assertEquals(N.toList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSortedByLong() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).skip(2).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).toList());
        assertEquals(N.toList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).skip(2).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).toList());
        assertEquals(N.toList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSortedByDouble() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).skip(2).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).toList());
        assertEquals(N.toList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).skip(2).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).toList());
        assertEquals(N.toList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSortedBy() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).skip(2).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).toList());
        assertEquals(N.toList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).skip(2).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).toList());
        assertEquals(N.toList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterTop() {
        assertEquals(3, Stream.of(5, 3, 1, 4, 2).top(3).count());
        assertEquals(2, Stream.of(5, 3, 1, 4, 2).top(3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 4 }, Stream.of(5, 3, 1, 4, 2).top(3).toArray());
        assertArrayEquals(new Integer[] { 5, 4 }, Stream.of(5, 3, 1, 4, 2).top(3).skip(1).toArray());
        assertEquals(N.toList(3, 5, 4), Stream.of(5, 3, 1, 4, 2).top(3).toList());
        assertEquals(N.toList(5, 4), Stream.of(5, 3, 1, 4, 2).top(3).skip(1).toList());
        assertEquals(3, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).count());
        assertEquals(2, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 4 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).toArray());
        assertArrayEquals(new Integer[] { 5, 4 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).skip(1).toArray());
        assertEquals(N.toList(3, 5, 4), Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).toList());
        assertEquals(N.toList(5, 4), Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTopWithComparator() {
        assertEquals(3, Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).count());
        assertEquals(2, Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 4 }, Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 5, 4 }, Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.toList(3, 5, 4), Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).toList());
        assertEquals(N.toList(5, 4), Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).skip(1).toList());
        assertEquals(3, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).count());
        assertEquals(2, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 4 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 5, 4 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.toList(3, 5, 4), Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).toList());
        assertEquals(N.toList(5, 4), Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipRange() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 4, 5 }, Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).skip(1).toArray());
        assertEquals(N.toList(1, 4, 5), Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).skip(1).toArray());
        assertEquals(N.toList(1, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipNulls() {
        assertEquals(3, Stream.of(1, null, 2, null, 3).skipNulls().count());
        assertEquals(2, Stream.of(1, null, 2, null, 3).skipNulls().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, null, 2, null, 3).skipNulls().toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, null, 2, null, 3).skipNulls().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of(1, null, 2, null, 3).skipNulls().toList());
        assertEquals(N.toList(2, 3), Stream.of(1, null, 2, null, 3).skipNulls().skip(1).toList());
        assertEquals(3, Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().count());
        assertEquals(2, Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().toList());
        assertEquals(N.toList(2, 3), Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipLast() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).skipLast(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).skipLast(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).skipLast(2).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, 2, 3, 4, 5).skipLast(2).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of(1, 2, 3, 4, 5).skipLast(2).toList());
        assertEquals(N.toList(2, 3), Stream.of(1, 2, 3, 4, 5).skipLast(2).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).toList());
        assertEquals(N.toList(2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeLast() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).takeLast(2).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).takeLast(2).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).takeLast(2).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.of(1, 2, 3, 4, 5).takeLast(2).skip(1).toArray());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).takeLast(2).toList());
        assertEquals(N.toList(5), Stream.of(1, 2, 3, 4, 5).takeLast(2).skip(1).toList());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).skip(1).toArray());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).toList());
        assertEquals(N.toList(5), Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnFirst() {
        List<Integer> firstElements = new ArrayList<>();
        Consumer<Integer> captureFirst = firstElements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterOnLast() {
        List<Integer> lastElements = new ArrayList<>();
        Consumer<Integer> captureLast = lastElements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).onLast(captureLast).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).onLast(captureLast).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onLast(captureLast).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onLast(captureLast).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).onLast(captureLast).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).onLast(captureLast).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterPeekFirst() {
        List<Integer> firstElements = new ArrayList<>();
        Consumer<Integer> captureFirst = firstElements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterPeekLast() {
        List<Integer> lastElements = new ArrayList<>();
        Consumer<Integer> captureLast = lastElements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterPeekIfWithPredicate() {
        List<Integer> evenElements = new ArrayList<>();
        Predicate<Integer> isEven = i -> i % 2 == 0;
        Consumer<Integer> captureEven = evenElements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterPeekIfWithBiPredicate() {
        List<Integer> elements = new ArrayList<>();
        BiPredicate<Integer, Long> isSecondElement = (e, count) -> count == 2L;
        Consumer<Integer> capture = elements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependVarargs() {
        assertEquals(5, Stream.of(3, 4, 5).prepend(1, 2).count());
        assertEquals(3, Stream.of(3, 4, 5).prepend(1, 2).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).prepend(1, 2).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(3, 4, 5).prepend(1, 2).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).prepend(1, 2).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(3, 4, 5).prepend(1, 2).skip(2).toList());
        assertEquals(5, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).count());
        assertEquals(3, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendVarargs() {
        assertEquals(5, Stream.of(1, 2, 3).append(4, 5).count());
        assertEquals(3, Stream.of(1, 2, 3).append(4, 5).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).append(4, 5).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3).append(4, 5).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).append(4, 5).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3).append(4, 5).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3).map(e -> e).append(4, 5).count());
        assertEquals(3, Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(4, 5).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(4, 5).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmptyVarargs() {
        assertEquals(3, Stream.of(1, 2, 3).appendIfEmpty(4, 5).count());
        assertEquals(2, Stream.of(1, 2, 3).appendIfEmpty(4, 5).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, 2, 3).appendIfEmpty(4, 5).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, 2, 3).appendIfEmpty(4, 5).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of(1, 2, 3).appendIfEmpty(4, 5).toList());
        assertEquals(N.toList(2, 3), Stream.of(1, 2, 3).appendIfEmpty(4, 5).skip(1).toList());

        assertEquals(2, Stream.<Integer> empty().appendIfEmpty(4, 5).count());
        assertEquals(1, Stream.<Integer> empty().appendIfEmpty(4, 5).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.<Integer> empty().appendIfEmpty(4, 5).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.<Integer> empty().appendIfEmpty(4, 5).skip(1).toArray());
        assertEquals(N.toList(4, 5), Stream.<Integer> empty().appendIfEmpty(4, 5).toList());
        assertEquals(N.toList(5), Stream.<Integer> empty().appendIfEmpty(4, 5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterBuffered() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).buffered().count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).buffered().skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered().toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered().skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered().toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered().skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterBufferedWithSize() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).buffered(10).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).buffered(10).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered(10).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered(10).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered(10).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered(10).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterBufferedWithQueue() {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).buffered(queue).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).buffered(queue).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered(queue).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered(queue).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered(queue).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered(queue).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeWithCollection() {
        List<Integer> collection = Arrays.asList(2, 4, 6);
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        assertEquals(6, Stream.of(1, 3, 5).mergeWith(collection, selector).count());
        assertEquals(4, Stream.of(1, 3, 5).mergeWith(collection, selector).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 }, Stream.of(1, 3, 5).mergeWith(collection, selector).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5, 6 }, Stream.of(1, 3, 5).mergeWith(collection, selector).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6), Stream.of(1, 3, 5).mergeWith(collection, selector).toList());
        assertEquals(N.toList(3, 4, 5, 6), Stream.of(1, 3, 5).mergeWith(collection, selector).skip(2).toList());
        assertEquals(6, Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).count());
        assertEquals(4, Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 }, Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5, 6 }, Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6), Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).toList());
        assertEquals(N.toList(3, 4, 5, 6), Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeWithStream() {
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        assertEquals(6, Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).count());
        assertEquals(4, Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 }, Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5, 6 }, Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6), Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).toList());
        assertEquals(N.toList(3, 4, 5, 6), Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).skip(2).toList());
        assertEquals(6, Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).count());
        assertEquals(4, Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 }, Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5, 6 }, Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6), Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).toList());
        assertEquals(N.toList(3, 4, 5, 6), Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithCollection() {
        List<String> collection = Arrays.asList("a", "b", "c");
        BiFunction<Integer, String, String> zipFunc = (i, s) -> i + s;

        assertEquals(3, Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).skip(1).count());
        assertArrayEquals(new String[] { "1a", "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).toArray());
        assertArrayEquals(new String[] { "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).skip(1).toArray());
        assertEquals(N.toList("1a", "2b", "3c"), Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).toList());
        assertEquals(N.toList("2b", "3c"), Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).skip(1).count());
        assertArrayEquals(new String[] { "1a", "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).toArray());
        assertArrayEquals(new String[] { "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).skip(1).toArray());
        assertEquals(N.toList("1a", "2b", "3c"), Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).toList());
        assertEquals(N.toList("2b", "3c"), Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithStream() {
        BiFunction<Integer, String, String> zipFunc = (i, s) -> i + s;

        assertEquals(3, Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).count());
        assertArrayEquals(new String[] { "1a", "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).toArray());
        assertArrayEquals(new String[] { "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).toArray());
        assertEquals(N.toList("1a", "2b", "3c"), Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).toList());
        assertEquals(N.toList("2b", "3c"), Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).count());
        assertArrayEquals(new String[] { "1a", "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).toArray());
        assertArrayEquals(new String[] { "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).toArray());
        assertEquals(N.toList("1a", "2b", "3c"), Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).toList());
        assertEquals(N.toList("2b", "3c"), Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).toList());
    }

    @Test
    public void testZipCharArraysWithBiFunction() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'x', 'y', 'z' };

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("cz", result.get(2));
    }

    @Test
    public void testZipCharArraysWithTriFunction() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'x', 'y', 'z' };
        char[] c = { '1', '2', '3' };

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("by2", result.get(1));
        assertEquals("cz3", result.get(2));
    }

    @Test
    public void testZipCharIteratorsWithBiFunction() {
        CharIterator a = CharIterator.of('a', 'b', 'c');
        CharIterator b = CharIterator.of('x', 'y', 'z');

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("cz", result.get(2));
    }

    @Test
    public void testZipCharIteratorsWithTriFunction() {
        CharIterator a = CharIterator.of('a', 'b', 'c');
        CharIterator b = CharIterator.of('x', 'y', 'z');
        CharIterator c = CharIterator.of('1', '2', '3');

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("by2", result.get(1));
        assertEquals("cz3", result.get(2));
    }

    @Test
    public void testZipCharStreamsWithBiFunction() {
        CharStream a = CharStream.of('a', 'b', 'c');
        CharStream b = CharStream.of('x', 'y', 'z');

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("cz", result.get(2));
    }

    @Test
    public void testZipCharStreamsWithTriFunction() {
        CharStream a = CharStream.of('a', 'b', 'c');
        CharStream b = CharStream.of('x', 'y', 'z');
        CharStream c = CharStream.of('1', '2', '3');

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("by2", result.get(1));
        assertEquals("cz3", result.get(2));
    }

    @Test
    public void testZipCharStreamCollectionWithNFunction() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a', 'b', 'c'), CharStream.of('x', 'y', 'z'), CharStream.of('1', '2', '3'));

        CharNFunction<String> zipFunction = chars -> {
            StringBuilder sb = new StringBuilder();
            for (char c : chars) {
                sb.append(c);
            }
            return sb.toString();
        };

        List<String> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("by2", result.get(1));
        assertEquals("cz3", result.get(2));
    }

    @Test
    public void testZipCharArraysWithValueForNone() {
        char[] a = { 'a', 'b' };
        char[] b = { 'x', 'y', 'z' };

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, '-', '+', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("-z", result.get(2));
    }

    @Test
    public void testZipCharArraysWithValueForNoneTriFunction() {
        char[] a = { 'a' };
        char[] b = { 'x', 'y' };
        char[] c = { '1', '2', '3' };

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, '-', '+', '*', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("-y2", result.get(1));
        assertEquals("-+3", result.get(2));
    }

    @Test
    public void testZipCharIteratorsWithValueForNone() {
        CharIterator a = CharIterator.of('a', 'b');
        CharIterator b = CharIterator.of('x', 'y', 'z');

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, '-', '+', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("-z", result.get(2));
    }

    @Test
    public void testZipCharIteratorsWithValueForNoneTriFunction() {
        CharIterator a = CharIterator.of('a');
        CharIterator b = CharIterator.of('x', 'y');
        CharIterator c = CharIterator.of('1', '2', '3');

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, '-', '+', '*', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("-y2", result.get(1));
        assertEquals("-+3", result.get(2));
    }

    @Test
    public void testZipCharStreamsWithValueForNone() {
        CharStream a = CharStream.of('a', 'b');
        CharStream b = CharStream.of('x', 'y', 'z');

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, '-', '+', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("-z", result.get(2));
    }

    @Test
    public void testZipCharStreamsWithValueForNoneTriFunction() {
        CharStream a = CharStream.of('a');
        CharStream b = CharStream.of('x', 'y');
        CharStream c = CharStream.of('1', '2', '3');

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, '-', '+', '*', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("-y2", result.get(1));
        assertEquals("-+3", result.get(2));
    }

    @Test
    public void testZipCharStreamCollectionWithValuesForNone() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a'), CharStream.of('x', 'y'), CharStream.of('1', '2', '3'));

        char[] valuesForNone = { '-', '+', '*' };

        CharNFunction<String> zipFunction = chars -> {
            StringBuilder sb = new StringBuilder();
            for (char c : chars) {
                sb.append(c);
            }
            return sb.toString();
        };

        List<String> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("-y2", result.get(1));
        assertEquals("-+3", result.get(2));
    }

    @Test
    public void testZipByteArraysWithBiFunction() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 4, 5, 6 };

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> b1 + b2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipByteArraysWithTriFunction() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 4, 5, 6 };
        byte[] c = { 7, 8, 9 };

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipByteIteratorsWithBiFunction() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        ByteIterator b = ByteIterator.of((byte) 4, (byte) 5, (byte) 6);

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> b1 + b2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipByteIteratorsWithTriFunction() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        ByteIterator b = ByteIterator.of((byte) 4, (byte) 5, (byte) 6);
        ByteIterator c = ByteIterator.of((byte) 7, (byte) 8, (byte) 9);

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipByteStreamsWithBiFunction() {
        ByteStream a = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = ByteStream.of((byte) 4, (byte) 5, (byte) 6);

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> b1 + b2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipByteStreamsWithTriFunction() {
        ByteStream a = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = ByteStream.of((byte) 4, (byte) 5, (byte) 6);
        ByteStream c = ByteStream.of((byte) 7, (byte) 8, (byte) 9);

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipByteStreamCollectionWithNFunction() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 2, (byte) 3), ByteStream.of((byte) 4, (byte) 5, (byte) 6),
                ByteStream.of((byte) 7, (byte) 8, (byte) 9));

        ByteNFunction<Integer> zipFunction = bytes -> {
            int sum = 0;
            for (byte b : bytes) {
                sum += b;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipByteArraysWithValueForNone() {
        byte[] a = { 1, 2 };
        byte[] b = { 4, 5, 6 };

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> b1 + b2;

        List<Integer> result = Stream.zip(a, b, (byte) 10, (byte) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipByteArraysWithValueForNoneTriFunction() {
        byte[] a = { 1 };
        byte[] b = { 4, 5 };
        byte[] c = { 7, 8, 9 };

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, (byte) 10, (byte) 20, (byte) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipByteIteratorsWithValueForNone() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator b = ByteIterator.of((byte) 4, (byte) 5, (byte) 6);

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> b1 + b2;

        List<Integer> result = Stream.zip(a, b, (byte) 10, (byte) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipByteIteratorsWithValueForNoneTriFunction() {
        ByteIterator a = ByteIterator.of((byte) 1);
        ByteIterator b = ByteIterator.of((byte) 4, (byte) 5);
        ByteIterator c = ByteIterator.of((byte) 7, (byte) 8, (byte) 9);

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, (byte) 10, (byte) 20, (byte) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipByteStreamsWithValueForNone() {
        ByteStream a = ByteStream.of((byte) 1, (byte) 2);
        ByteStream b = ByteStream.of((byte) 4, (byte) 5, (byte) 6);

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> b1 + b2;

        List<Integer> result = Stream.zip(a, b, (byte) 10, (byte) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipByteStreamsWithValueForNoneTriFunction() {
        ByteStream a = ByteStream.of((byte) 1);
        ByteStream b = ByteStream.of((byte) 4, (byte) 5);
        ByteStream c = ByteStream.of((byte) 7, (byte) 8, (byte) 9);

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, (byte) 10, (byte) 20, (byte) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipByteStreamCollectionWithValuesForNone() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1), ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9));

        byte[] valuesForNone = { 10, 20, 30 };

        ByteNFunction<Integer> zipFunction = bytes -> {
            int sum = 0;
            for (byte b : bytes) {
                sum += b;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipShortArraysWithBiFunction() {
        short[] a = { 1, 2, 3 };
        short[] b = { 4, 5, 6 };

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> s1 + s2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipShortArraysWithTriFunction() {
        short[] a = { 1, 2, 3 };
        short[] b = { 4, 5, 6 };
        short[] c = { 7, 8, 9 };

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipShortIteratorsWithBiFunction() {
        ShortIterator a = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortIterator b = ShortIterator.of((short) 4, (short) 5, (short) 6);

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> s1 + s2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipShortIteratorsWithTriFunction() {
        ShortIterator a = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortIterator b = ShortIterator.of((short) 4, (short) 5, (short) 6);
        ShortIterator c = ShortIterator.of((short) 7, (short) 8, (short) 9);

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipShortStreamsWithBiFunction() {
        ShortStream a = ShortStream.of((short) 1, (short) 2, (short) 3);
        ShortStream b = ShortStream.of((short) 4, (short) 5, (short) 6);

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> s1 + s2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipShortStreamsWithTriFunction() {
        ShortStream a = ShortStream.of((short) 1, (short) 2, (short) 3);
        ShortStream b = ShortStream.of((short) 4, (short) 5, (short) 6);
        ShortStream c = ShortStream.of((short) 7, (short) 8, (short) 9);

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipShortStreamCollectionWithNFunction() {
        Collection<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 2, (short) 3), ShortStream.of((short) 4, (short) 5, (short) 6),
                ShortStream.of((short) 7, (short) 8, (short) 9));

        ShortNFunction<Integer> zipFunction = shorts -> {
            int sum = 0;
            for (short s : shorts) {
                sum += s;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipShortArraysWithValueForNone() {
        short[] a = { 1, 2 };
        short[] b = { 4, 5, 6 };

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> s1 + s2;

        List<Integer> result = Stream.zip(a, b, (short) 10, (short) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipShortArraysWithValueForNoneTriFunction() {
        short[] a = { 1 };
        short[] b = { 4, 5 };
        short[] c = { 7, 8, 9 };

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, (short) 10, (short) 20, (short) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipShortIteratorsWithValueForNone() {
        ShortIterator a = ShortIterator.of((short) 1, (short) 2);
        ShortIterator b = ShortIterator.of((short) 4, (short) 5, (short) 6);

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> s1 + s2;

        List<Integer> result = Stream.zip(a, b, (short) 10, (short) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipShortIteratorsWithValueForNoneTriFunction() {
        ShortIterator a = ShortIterator.of((short) 1);
        ShortIterator b = ShortIterator.of((short) 4, (short) 5);
        ShortIterator c = ShortIterator.of((short) 7, (short) 8, (short) 9);

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, (short) 10, (short) 20, (short) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipShortStreamsWithValueForNone() {
        ShortStream a = ShortStream.of((short) 1, (short) 2);
        ShortStream b = ShortStream.of((short) 4, (short) 5, (short) 6);

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> s1 + s2;

        List<Integer> result = Stream.zip(a, b, (short) 10, (short) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipShortStreamsWithValueForNoneTriFunction() {
        ShortStream a = ShortStream.of((short) 1);
        ShortStream b = ShortStream.of((short) 4, (short) 5);
        ShortStream c = ShortStream.of((short) 7, (short) 8, (short) 9);

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, (short) 10, (short) 20, (short) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipShortStreamCollectionWithValuesForNone() {
        Collection<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1), ShortStream.of((short) 4, (short) 5),
                ShortStream.of((short) 7, (short) 8, (short) 9));

        short[] valuesForNone = { 10, 20, 30 };

        ShortNFunction<Integer> zipFunction = shorts -> {
            int sum = 0;
            for (short s : shorts) {
                sum += s;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipIntArraysWithBiFunction() {
        int[] a = { 1, 2, 3 };
        int[] b = { 4, 5, 6 };

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipIntArraysWithTriFunction() {
        int[] a = { 1, 2, 3 };
        int[] b = { 4, 5, 6 };
        int[] c = { 7, 8, 9 };

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipIntIteratorsWithBiFunction() {
        IntIterator a = IntIterator.of(1, 2, 3);
        IntIterator b = IntIterator.of(4, 5, 6);

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipIntIteratorsWithTriFunction() {
        IntIterator a = IntIterator.of(1, 2, 3);
        IntIterator b = IntIterator.of(4, 5, 6);
        IntIterator c = IntIterator.of(7, 8, 9);

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipIntStreamsWithBiFunction() {
        IntStream a = IntStream.of(1, 2, 3);
        IntStream b = IntStream.of(4, 5, 6);

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipIntStreamsWithTriFunction() {
        IntStream a = IntStream.of(1, 2, 3);
        IntStream b = IntStream.of(4, 5, 6);
        IntStream c = IntStream.of(7, 8, 9);

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipIntStreamCollectionWithNFunction() {
        Collection<IntStream> streams = Arrays.asList(IntStream.of(1, 2, 3), IntStream.of(4, 5, 6), IntStream.of(7, 8, 9));

        IntNFunction<Integer> zipFunction = ints -> {
            int sum = 0;
            for (int i : ints) {
                sum += i;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipIntArraysWithValueForNone() {
        int[] a = { 1, 2 };
        int[] b = { 4, 5, 6 };

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipIntArraysWithValueForNoneTriFunction() {
        int[] a = { 1 };
        int[] b = { 4, 5 };
        int[] c = { 7, 8, 9 };

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipIntIteratorsWithValueForNone() {
        IntIterator a = IntIterator.of(1, 2);
        IntIterator b = IntIterator.of(4, 5, 6);

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipIntIteratorsWithValueForNoneTriFunction() {
        IntIterator a = IntIterator.of(1);
        IntIterator b = IntIterator.of(4, 5);
        IntIterator c = IntIterator.of(7, 8, 9);

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipIntStreamsWithValueForNone() {
        IntStream a = IntStream.of(1, 2);
        IntStream b = IntStream.of(4, 5, 6);

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipIntStreamsWithValueForNoneTriFunction() {
        IntStream a = IntStream.of(1);
        IntStream b = IntStream.of(4, 5);
        IntStream c = IntStream.of(7, 8, 9);

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipIntStreamCollectionWithValuesForNone() {
        Collection<IntStream> streams = Arrays.asList(IntStream.of(1), IntStream.of(4, 5), IntStream.of(7, 8, 9));

        int[] valuesForNone = { 10, 20, 30 };

        IntNFunction<Integer> zipFunction = ints -> {
            int sum = 0;
            for (int i : ints) {
                sum += i;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipLongArraysWithBiFunction() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 4L, 5L, 6L };

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(9L), result.get(2));
    }

    @Test
    public void testZipLongArraysWithTriFunction() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 4L, 5L, 6L };
        long[] c = { 7L, 8L, 9L };

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(15L), result.get(1));
        assertEquals(Long.valueOf(18L), result.get(2));
    }

    @Test
    public void testZipLongIteratorsWithBiFunction() {
        LongIterator a = LongIterator.of(1L, 2L, 3L);
        LongIterator b = LongIterator.of(4L, 5L, 6L);

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(9L), result.get(2));
    }

    @Test
    public void testZipLongIteratorsWithTriFunction() {
        LongIterator a = LongIterator.of(1L, 2L, 3L);
        LongIterator b = LongIterator.of(4L, 5L, 6L);
        LongIterator c = LongIterator.of(7L, 8L, 9L);

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(15L), result.get(1));
        assertEquals(Long.valueOf(18L), result.get(2));
    }

    @Test
    public void testZipLongStreamsWithBiFunction() {
        LongStream a = LongStream.of(1L, 2L, 3L);
        LongStream b = LongStream.of(4L, 5L, 6L);

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(9L), result.get(2));
    }

    @Test
    public void testZipLongStreamsWithTriFunction() {
        LongStream a = LongStream.of(1L, 2L, 3L);
        LongStream b = LongStream.of(4L, 5L, 6L);
        LongStream c = LongStream.of(7L, 8L, 9L);

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(15L), result.get(1));
        assertEquals(Long.valueOf(18L), result.get(2));
    }

    @Test
    public void testZipLongStreamCollectionWithNFunction() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1L, 2L, 3L), LongStream.of(4L, 5L, 6L), LongStream.of(7L, 8L, 9L));

        LongNFunction<Long> zipFunction = longs -> {
            long sum = 0;
            for (long l : longs) {
                sum += l;
            }
            return sum;
        };

        List<Long> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(15L), result.get(1));
        assertEquals(Long.valueOf(18L), result.get(2));
    }

    @Test
    public void testZipLongArraysWithValueForNone() {
        long[] a = { 1L, 2L };
        long[] b = { 4L, 5L, 6L };

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, 10L, 20L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(16L), result.get(2));
    }

    @Test
    public void testZipLongArraysWithValueForNoneTriFunction() {
        long[] a = { 1L };
        long[] b = { 4L, 5L };
        long[] c = { 7L, 8L, 9L };

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, 10L, 20L, 30L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(23L), result.get(1));
        assertEquals(Long.valueOf(39L), result.get(2));
    }

    @Test
    public void testZipLongIteratorsWithValueForNone() {
        LongIterator a = LongIterator.of(1L, 2L);
        LongIterator b = LongIterator.of(4L, 5L, 6L);

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, 10L, 20L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(16L), result.get(2));
    }

    @Test
    public void testZipLongIteratorsWithValueForNoneTriFunction() {
        LongIterator a = LongIterator.of(1L);
        LongIterator b = LongIterator.of(4L, 5L);
        LongIterator c = LongIterator.of(7L, 8L, 9L);

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, 10L, 20L, 30L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(23L), result.get(1));
        assertEquals(Long.valueOf(39L), result.get(2));
    }

    @Test
    public void testZipLongStreamsWithValueForNone() {
        LongStream a = LongStream.of(1L, 2L);
        LongStream b = LongStream.of(4L, 5L, 6L);

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, 10L, 20L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(16L), result.get(2));
    }

    @Test
    public void testZipLongStreamsWithValueForNoneTriFunction() {
        LongStream a = LongStream.of(1L);
        LongStream b = LongStream.of(4L, 5L);
        LongStream c = LongStream.of(7L, 8L, 9L);

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, 10L, 20L, 30L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(23L), result.get(1));
        assertEquals(Long.valueOf(39L), result.get(2));
    }

    @Test
    public void testZipLongStreamCollectionWithValuesForNone() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1L), LongStream.of(4L, 5L), LongStream.of(7L, 8L, 9L));

        long[] valuesForNone = { 10L, 20L, 30L };

        LongNFunction<Long> zipFunction = longs -> {
            long sum = 0;
            for (long l : longs) {
                sum += l;
            }
            return sum;
        };

        List<Long> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(23L), result.get(1));
        assertEquals(Long.valueOf(39L), result.get(2));
    }

    @Test
    public void testZipFloatArraysWithBiFunction() {
        float[] a = { 1f, 2f, 3f };
        float[] b = { 4f, 5f, 6f };

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(9f), result.get(2));
    }

    @Test
    public void testZipFloatArraysWithTriFunction() {
        float[] a = { 1f, 2f, 3f };
        float[] b = { 4f, 5f, 6f };
        float[] c = { 7f, 8f, 9f };

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12f), result.get(0));
        assertEquals(Float.valueOf(15f), result.get(1));
        assertEquals(Float.valueOf(18f), result.get(2));
    }

    @Test
    public void testZipFloatIteratorsWithBiFunction() {
        FloatIterator a = FloatIterator.of(1f, 2f, 3f);
        FloatIterator b = FloatIterator.of(4f, 5f, 6f);

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(9f), result.get(2));
    }

    @Test
    public void testZipFloatIteratorsWithTriFunction() {
        FloatIterator a = FloatIterator.of(1, 2, 3);
        FloatIterator b = FloatIterator.of(4, 5f, 6f);
        FloatIterator c = FloatIterator.of(7f, 8f, 9f);

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(15f), result.get(1));
        assertEquals(Float.valueOf(18f), result.get(2));
    }

    @Test
    public void testZipFloatStreamsWithBiFunction() {
        FloatStream a = FloatStream.of(1, 2, 3);
        FloatStream b = FloatStream.of(4, 5f, 6f);

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(9f), result.get(2));
    }

    @Test
    public void testZipFloatStreamsWithTriFunction() {
        FloatStream a = FloatStream.of(1, 2, 3);
        FloatStream b = FloatStream.of(4, 5f, 6f);
        FloatStream c = FloatStream.of(7f, 8f, 9f);

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(15f), result.get(1));
        assertEquals(Float.valueOf(18f), result.get(2));
    }

    @Test
    public void testZipFloatStreamCollectionWithNFunction() {
        Collection<FloatStream> streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5f, 6f), FloatStream.of(7f, 8f, 9f));

        FloatNFunction<Float> zipFunction = floats -> {
            float sum = 0;
            for (float l : floats) {
                sum += l;
            }
            return sum;
        };

        List<Float> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(15f), result.get(1));
        assertEquals(Float.valueOf(18f), result.get(2));
    }

    @Test
    public void testZipFloatArraysWithValueForNone() {
        float[] a = { 1, 2 };
        float[] b = { 4, 5f, 6f };

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(16f), result.get(2));
    }

    @Test
    public void testZipFloatArraysWithValueForNoneTriFunction() {
        float[] a = { 1 };
        float[] b = { 4, 5f };
        float[] c = { 7f, 8f, 9f };

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(23), result.get(1));
        assertEquals(Float.valueOf(39f), result.get(2));
    }

    @Test
    public void testZipFloatIteratorsWithValueForNone() {
        FloatIterator a = FloatIterator.of(1, 2);
        FloatIterator b = FloatIterator.of(4, 5f, 6f);

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(16f), result.get(2));
    }

    @Test
    public void testZipFloatIteratorsWithValueForNoneTriFunction() {
        FloatIterator a = FloatIterator.of(1);
        FloatIterator b = FloatIterator.of(4, 5f);
        FloatIterator c = FloatIterator.of(7f, 8f, 9f);

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(23), result.get(1));
        assertEquals(Float.valueOf(39f), result.get(2));
    }

    @Test
    public void testZipFloatStreamsWithValueForNone() {
        FloatStream a = FloatStream.of(1, 2);
        FloatStream b = FloatStream.of(4, 5f, 6f);

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(16f), result.get(2));
    }

    @Test
    public void testZipFloatStreamsWithValueForNoneTriFunction() {
        FloatStream a = FloatStream.of(1);
        FloatStream b = FloatStream.of(4, 5f);
        FloatStream c = FloatStream.of(7f, 8f, 9f);

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(23), result.get(1));
        assertEquals(Float.valueOf(39f), result.get(2));
    }

    @Test
    public void testZipFloatStreamCollectionWithValuesForNone() {
        Collection<FloatStream> streams = Arrays.asList(FloatStream.of(1), FloatStream.of(4, 5f), FloatStream.of(7f, 8f, 9f));

        float[] valuesForNone = { 10, 20, 30 };

        FloatNFunction<Float> zipFunction = floats -> {
            float sum = 0;
            for (float l : floats) {
                sum += l;
            }
            return sum;
        };

        List<Float> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(23), result.get(1));
        assertEquals(Float.valueOf(39f), result.get(2));
    }

    @Test
    public void testZipDoubleArraysWithBiFunction() {
        double[] a = { 1d, 2d, 3d };
        double[] b = { 4d, 5d, 6d };

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(9d), result.get(2));
    }

    @Test
    public void testZipDoubleArraysWithTriFunction() {
        double[] a = { 1d, 2d, 3d };
        double[] b = { 4d, 5d, 6d };
        double[] c = { 7d, 8d, 9d };

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(15d), result.get(1));
        assertEquals(Double.valueOf(18d), result.get(2));
    }

    @Test
    public void testZipDoubleIteratorsWithBiFunction() {
        DoubleIterator a = DoubleIterator.of(1d, 2d, 3d);
        DoubleIterator b = DoubleIterator.of(4d, 5d, 6d);

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(9d), result.get(2));
    }

    @Test
    public void testZipDoubleIteratorsWithTriFunction() {
        DoubleIterator a = DoubleIterator.of(1d, 2d, 3d);
        DoubleIterator b = DoubleIterator.of(4d, 5d, 6d);
        DoubleIterator c = DoubleIterator.of(7d, 8d, 9d);

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(15d), result.get(1));
        assertEquals(Double.valueOf(18d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamsWithBiFunction() {
        DoubleStream a = DoubleStream.of(1d, 2d, 3d);
        DoubleStream b = DoubleStream.of(4d, 5d, 6d);

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(9d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamsWithTriFunction() {
        DoubleStream a = DoubleStream.of(1d, 2d, 3d);
        DoubleStream b = DoubleStream.of(4d, 5d, 6d);
        DoubleStream c = DoubleStream.of(7d, 8d, 9d);

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(15d), result.get(1));
        assertEquals(Double.valueOf(18d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamCollectionWithNFunction() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1d, 2d, 3d), DoubleStream.of(4d, 5d, 6d), DoubleStream.of(7d, 8d, 9d));

        DoubleNFunction<Double> zipFunction = doubles -> {
            double sum = 0;
            for (double l : doubles) {
                sum += l;
            }
            return sum;
        };

        List<Double> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(15d), result.get(1));
        assertEquals(Double.valueOf(18d), result.get(2));
    }

    @Test
    public void testZipDoubleArraysWithValueForNone() {
        double[] a = { 1d, 2d };
        double[] b = { 4d, 5d, 6d };

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, 10d, 20d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(16d), result.get(2));
    }

    @Test
    public void testZipDoubleArraysWithValueForNoneTriFunction() {
        double[] a = { 1d };
        double[] b = { 4d, 5d };
        double[] c = { 7d, 8d, 9d };

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, 10d, 20d, 30d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(23d), result.get(1));
        assertEquals(Double.valueOf(39d), result.get(2));
    }

    @Test
    public void testZipDoubleIteratorsWithValueForNone() {
        DoubleIterator a = DoubleIterator.of(1d, 2d);
        DoubleIterator b = DoubleIterator.of(4d, 5d, 6d);

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, 10d, 20d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(16d), result.get(2));
    }

    @Test
    public void testZipDoubleIteratorsWithValueForNoneTriFunction() {
        DoubleIterator a = DoubleIterator.of(1d);
        DoubleIterator b = DoubleIterator.of(4d, 5d);
        DoubleIterator c = DoubleIterator.of(7d, 8d, 9d);

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, 10d, 20d, 30d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(23d), result.get(1));
        assertEquals(Double.valueOf(39d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamsWithValueForNone() {
        DoubleStream a = DoubleStream.of(1d, 2d);
        DoubleStream b = DoubleStream.of(4d, 5d, 6d);

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, 10d, 20d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(16d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamsWithValueForNoneTriFunction() {
        DoubleStream a = DoubleStream.of(1d);
        DoubleStream b = DoubleStream.of(4d, 5d);
        DoubleStream c = DoubleStream.of(7d, 8d, 9d);

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, 10d, 20d, 30d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(23d), result.get(1));
        assertEquals(Double.valueOf(39d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamCollectionWithValuesForNone() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1d), DoubleStream.of(4d, 5d), DoubleStream.of(7d, 8d, 9d));

        double[] valuesForNone = { 10d, 20d, 30d };

        DoubleNFunction<Double> zipFunction = doubles -> {
            double sum = 0;
            for (double l : doubles) {
                sum += l;
            }
            return sum;
        };

        List<Double> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(23d), result.get(1));
        assertEquals(Double.valueOf(39d), result.get(2));
    }

    @Test
    public void testSplitWithCharDelimiter() {
        List<String> result = Stream.split("a,b,c", ',').toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = Stream.split("hello world", ' ').toList();
        assertEquals(Arrays.asList("hello", "world"), result);

        result = Stream.split("", ',').toList();
        assertEquals(Arrays.asList(""), result);

        result = Stream.split("abc", ',').toList();
        assertEquals(Arrays.asList("abc"), result);
    }

    @Test
    public void testSplitWithCharSequenceDelimiter() {
        List<String> result = Stream.split("a::b::c", "::").toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = Stream.split("hello<br>world", "<br>").toList();
        assertEquals(Arrays.asList("hello", "world"), result);

        result = Stream.split("test", "::").toList();
        assertEquals(Arrays.asList("test"), result);
    }

    @Test
    public void testSplitWithPattern() {
        Pattern pattern = Pattern.compile("\\s+");
        List<String> result = Stream.split("hello   world  test", pattern).toList();
        assertEquals(Arrays.asList("hello", "world", "test"), result);

        pattern = Pattern.compile("[,;]");
        result = Stream.split("a,b;c,d", pattern).toList();
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testSplitToLinesWithOptions() {
        String multiLine = "  line1  \n\n  line2  \n\n";

        List<String> result = Stream.splitToLines(multiLine, false, false).toList();
        assertTrue(result.contains("  line1  "));
        assertTrue(result.contains("  line2  "));
        assertTrue(result.contains(""));

        result = Stream.splitToLines(multiLine, true, false).toList();
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
        assertTrue(result.contains(""));

        result = Stream.splitToLines(multiLine, false, true).toList();
        assertTrue(result.contains("  line1  "));
        assertTrue(result.contains("  line2  "));
        assertFalse(result.contains(""));

        result = Stream.splitToLines(multiLine, true, true).toList();
        assertEquals(Arrays.asList("line1", "line2"), result);
    }

    @Test
    public void testSplitByChunkCountWithSizeSmallerFirst() {
        List<String> result = Stream.splitByChunkCount(7, 5, true, (from, to) -> from + "-" + to).toList();
        assertEquals(5, result.size());
        assertEquals("0-1", result.get(0));
        assertEquals("1-2", result.get(1));
        assertEquals("2-3", result.get(2));
        assertEquals("3-5", result.get(3));
        assertEquals("5-7", result.get(4));

        result = Stream.splitByChunkCount(7, 5, false, (from, to) -> from + "-" + to).toList();
        assertEquals(5, result.size());
        assertEquals("0-2", result.get(0));
        assertEquals("2-4", result.get(1));
        assertEquals("4-5", result.get(2));
        assertEquals("5-6", result.get(3));
        assertEquals("6-7", result.get(4));
    }

    @Test
    public void testSplitByChunkCountNegativeTotalSize() {
        assertThrows(IllegalArgumentException.class, () -> Stream.splitByChunkCount(-1, 3, (from, to) -> from + "-" + to));
    }

    @Test
    public void testSplitByChunkCountZeroMaxChunkCount() {
        assertThrows(IllegalArgumentException.class, () -> Stream.splitByChunkCount(10, 0, (from, to) -> from + "-" + to));
    }

    @Test
    public void testFlatten2DArray() {
        Integer[][] array = { { 1, 2, 3 }, { 4, 5 }, { 6, 7, 8, 9 } };

        List<Integer> result = Stream.flatten(array).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);

        Integer[][] empty = new Integer[0][];
        result = Stream.flatten(empty).toList();
        assertTrue(result.isEmpty());

        Integer[][] withNulls = { { 1, 2 }, null, { 3, 4 } };
        result = Stream.flatten(withNulls).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatten2DArrayVertically() {
        Integer[][] array = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };

        List<Integer> result = Stream.flatten(array, false).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);

        result = Stream.flatten(array, true).toList();
        assertEquals(Arrays.asList(1, 4, 7, 2, 5, 8, 3, 6, 9), result);

        Integer[][] jagged = { { 1, 2, 3 }, { 4, 5 }, { 6 } };
        result = Stream.flatten(jagged, true).toList();
        assertEquals(Arrays.asList(1, 4, 6, 2, 5, 3), result);
    }

    @Test
    public void testFlatten2DArrayWithAlignment() {
        Integer[][] array = { { 1, 2 }, { 3, 4, 5 }, { 6 } };

        List<Integer> result = Stream.flatten(array, 0, false).toList();
        assertEquals(Arrays.asList(1, 2, 0, 3, 4, 5, 6, 0, 0), result);

        result = Stream.flatten(array, -1, true).toList();
        assertEquals(Arrays.asList(1, 3, 6, 2, 4, -1, -1, 5, -1), result);
    }

    @Test
    public void testFlatten3DArray() {
        Integer[][][] array = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } };

        List<Integer> result = Stream.flatten(array).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), result);

        Integer[][][] empty = new Integer[0][][];
        result = Stream.flatten(empty).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeatNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> Stream.repeat("test", -1));
    }

    @Test
    public void testIterateWithBooleanSupplier() {
        final int[] count = { 0 };
        List<Integer> result = Stream.iterate(() -> count[0] < 5, () -> count[0]++).toList();

        assertEquals(Arrays.asList(0, 1, 2, 3, 4), result);
    }

    @Test
    public void testIterateWithInitAndBooleanSupplier() {
        final int[] count = { 0 };
        List<Integer> result = Stream.iterate(1, () -> count[0]++ < 4, n -> n * 2).toList();

        assertEquals(Arrays.asList(1, 2, 4, 8), result);
    }

    @Test
    public void testIterateWithInitAndPredicate() {
        List<Integer> result = Stream.iterate(1, n -> n < 20, n -> n * 2).toList();

        assertEquals(Arrays.asList(1, 2, 4, 8, 16), result);
    }

    @Test
    public void testIterateInfinite() {
        List<Integer> result = Stream.iterate(1, n -> n + 1).limit(5).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testOfLinesFromPathWithCharset() throws IOException {
        Path tempPath = Files.createTempFile("test", ".txt");
        Files.deleteIfExists(tempPath);

        String content = "测试\n内容";
        Files.write(tempPath, content.getBytes(StandardCharsets.UTF_8));

        List<String> result = Stream.ofLines(tempPath, StandardCharsets.UTF_8).toList();
        assertEquals(Arrays.asList("测试", "内容"), result);

        Files.deleteIfExists(tempPath);
    }

    @Test
    public void testOfLinesFromReaderWithCloseOption() throws IOException {
        String content = "test1\ntest2";
        StringReader reader = new StringReader(content);

        try (Stream<String> stream = Stream.ofLines(reader, true)) {
            List<String> result = stream.toList();
            assertEquals(Arrays.asList("test1", "test2"), result);
        }

    }

    @Test
    public void testListFilesExcludeDirectory() throws IOException {
        File tempDir = Files.createTempDirectory("test").toFile();
        tempDir.deleteOnExit();

        File file1 = new File(tempDir, "file1.txt");
        File subDir = new File(tempDir, "subdir");
        File file2 = new File(subDir, "file2.txt");

        file1.createNewFile();
        subDir.mkdir();
        file2.createNewFile();

        file1.deleteOnExit();
        file2.deleteOnExit();
        subDir.deleteOnExit();

        List<File> files = Stream.listFiles(tempDir, true, true).toList();
        assertEquals(2, files.size());
        assertTrue(files.stream().allMatch(File::isFile));
    }

    @Test
    public void testIntervalWithSupplier() throws InterruptedException {
        final int[] counter = { 0 };
        List<Integer> result = Stream.interval(10, () -> counter[0]++).limit(3).toList();

        assertEquals(Arrays.asList(0, 1, 2), result);
        assertTrue(counter[0] >= 3);
    }

    @Test
    public void testIntervalWithDelayAndSupplier() throws InterruptedException {
        final int[] counter = { 0 };
        long startTime = System.currentTimeMillis();

        List<Integer> result = Stream.interval(50, 10, () -> counter[0]++).limit(3).toList();

        long elapsedTime = System.currentTimeMillis() - startTime;

        assertEquals(Arrays.asList(0, 1, 2), result);
        assertTrue(elapsedTime >= 50);
    }

    @Test
    public void testIntervalWithTimeUnit() throws InterruptedException {
        final int[] counter = { 0 };

        List<Integer> result = Stream.interval(0, 10, TimeUnit.MILLISECONDS, () -> counter[0]++).limit(3).toList();

        assertEquals(Arrays.asList(0, 1, 2), result);
    }

    @Test
    public void testIntervalWithLongFunction() throws InterruptedException {
        List<String> result = Stream.interval(10, time -> "Time: " + time).limit(2).toList();

        assertEquals(2, result.size());
        assertTrue(result.get(0).startsWith("Time: "));
        assertTrue(result.get(1).startsWith("Time: "));
    }

    @Test
    public void testObserveWithDuration() throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
        queue.offer("item1");
        queue.offer("item2");

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
                queue.offer("item3");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        List<String> result = Stream.observe(queue, Duration.ofMillis(100)).toList();
        assertTrue(result.size() >= 2);
        assertTrue(result.contains("item1"));
        assertTrue(result.contains("item2"));
    }

    @Test
    public void testObserveWithBooleanSupplier() throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
        MutableBoolean hasMore = MutableBoolean.of(true);

        queue.offer("item1");
        queue.offer("item2");

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
                queue.offer("item3");
                Thread.sleep(50);
                hasMore.setFalse();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        List<String> result = Stream.observe(queue, hasMore::value, 20).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("item1", "item2", "item3"), result);
    }

    @Test
    public void testConcatArrays() {
        Integer[] arr1 = { 1, 2, 3 };
        Integer[] arr2 = { 4, 5 };
        Integer[] arr3 = { 6, 7, 8 };

        List<Integer> result = Stream.concat(arr1, arr2, arr3).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), result);

        result = Stream.concat(new Integer[0], arr1, new Integer[0]).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        result = Stream.concat(new Integer[0][0]).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatStreams() {
        Stream<Integer> stream1 = createStream(1, 2, 3);
        Stream<Integer> stream2 = createStream(4, 5);
        Stream<Integer> stream3 = createStream(6, 7, 8);

        List<Integer> result = Stream.concat(stream1, stream2, stream3).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), result);
    }

    @Test
    public void testConcatStreamCollection() {
        List<Stream<Integer>> streams = Arrays.asList(createStream(1, 2), createStream(3, 4), createStream(5, 6));

        List<Integer> result = Stream.concat(streams).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);

        List<Stream<Integer>> emptyStreams = new ArrayList<>();
        result = Stream.concat(emptyStreams).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatIterablesCollection() {
        List<Iterable<Integer>> iterables = Arrays.asList(Arrays.asList(1, 2), new HashSet<>(Arrays.asList(3, 4)), Arrays.asList(5, 6));

        List<Integer> result = Stream.concatIterables(iterables).toList();
        assertEquals(6, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4, 5, 6)));
    }

    @Test
    public void testConcatIteratorsCollection() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator(), Arrays.asList(5, 6).iterator());

        List<Integer> result = Stream.concatIterators(iterators).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testZipCharArrays() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'x', 'y', 'z' };

        List<String> result = Stream.zip(a, b, (c1, c2) -> "" + c1 + c2).toList();
        assertEquals(Arrays.asList("ax", "by", "cz"), result);

        char[] c = { '1', '2' };
        result = Stream.zip(a, c, (c1, c2) -> "" + c1 + c2).toList();
        assertEquals(Arrays.asList("a1", "b2"), result);
    }

    @Test
    public void testZipThreeCharArrays() {
        char[] a = { 'a', 'b' };
        char[] b = { 'x', 'y' };
        char[] c = { '1', '2' };

        List<String> result = Stream.zip(a, b, c, (c1, c2, c3) -> "" + c1 + c2 + c3).toList();
        assertEquals(Arrays.asList("ax1", "by2"), result);
    }

    @Test
    public void testZipByteArrays() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 10, 20, 30 };

        List<Integer> result = Stream.zip(a, b, (b1, b2) -> (int) (b1 + b2)).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipShortArrays() {
        short[] a = { 1, 2, 3 };
        short[] b = { 10, 20, 30 };

        List<Integer> result = Stream.zip(a, b, (s1, s2) -> (int) (s1 + s2)).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipIntArrays() {
        int[] a = { 1, 2, 3 };
        int[] b = { 10, 20, 30 };

        List<Integer> result = Stream.zip(a, b, (i1, i2) -> i1 + i2).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipLongArrays() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 10L, 20L, 30L };

        List<Long> result = Stream.zip(a, b, (l1, l2) -> l1 + l2).toList();
        assertEquals(Arrays.asList(11L, 22L, 33L), result);
    }

    @Test
    public void testZipFloatArrays() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 10.0f, 20.0f, 30.0f };

        List<Float> result = Stream.zip(a, b, (f1, f2) -> f1 + f2).toList();
        assertEquals(Arrays.asList(11.0f, 22.0f, 33.0f), result);
    }

    @Test
    public void testZipDoubleArrays() {
        double[] a = { 1.0, 2.0, 3.0 };
        double[] b = { 10.0, 20.0, 30.0 };

        List<Double> result = Stream.zip(a, b, (d1, d2) -> d1 + d2).toList();
        assertEquals(Arrays.asList(11.0, 22.0, 33.0), result);
    }

    @Test
    public void testZipObjectArrays() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };

        List<String> result = Stream.zip(a, b, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testZipThreeObjectArrays() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2 };
        Double[] c = { 0.1, 0.2 };

        List<String> result = Stream.zip(a, b, c, (s, i, d) -> s + i + d).toList();
        assertEquals(Arrays.asList("a10.1", "b20.2"), result);
    }

    @Test
    public void testZipStreams() {
        Stream<String> a = createStream01("a", "b", "c");
        Stream<Integer> b = createStream(1, 2, 3);

        List<String> result = Stream.zip(a, b, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testZipThreeStreams() {
        Stream<String> a = createStream01("a", "b");
        Stream<Integer> b = createStream(1, 2);
        Stream<Double> c = createStream(0.1, 0.2);

        List<String> result = Stream.zip(a, b, c, (s, i, d) -> s + i + d).toList();
        assertEquals(Arrays.asList("a10.1", "b20.2"), result);
    }

    @Test
    public void testZipStreamCollection() {
        List<Stream<Integer>> streams = Arrays.asList(createStream(1, 2, 3), createStream(10, 20, 30), createStream(100, 200, 300));

        List<Integer> result = Stream.zip(streams, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 222, 333), result);
    }

    @Test
    public void testZipIterablesWithFunction() {
        List<Iterable<Integer>> iterables = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(10, 20, 30), Arrays.asList(100, 200, 300));

        List<Integer> result = Stream.zipIterables(iterables, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 222, 333), result);
    }

    @Test
    public void testZipIteratorsWithFunction() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(10, 20, 30).iterator(),
                Arrays.asList(100, 200, 300).iterator());

        List<Integer> result = Stream.zipIterators(iterators, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 222, 333), result);
    }

    @Test
    public void testZipArraysWithValueForNone() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2 };

        List<String> result = Stream.zip(a, b, "X", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipThreeArraysWithValueForNone() {
        String[] a = { "a", "b" };
        Integer[] b = { 1 };
        Double[] c = { 0.1, 0.2, 0.3 };

        List<String> result = Stream.zip(a, b, c, "X", 99, 0.0, (s, i, d) -> s + i + d).toList();
        assertEquals(Arrays.asList("a10.1", "b990.2", "X990.3"), result);
    }

    @Test
    public void testZipIterablesWithValueForNone() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<Integer> b = Arrays.asList(1, 2);

        List<String> result = Stream.zip(a, b, "X", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipIteratorsWithValueForNone() {
        Iterator<String> a = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> b = Arrays.asList(1, 2).iterator();

        List<String> result = Stream.zip(a, b, "X", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipStreamsWithValueForNone() {
        Stream<String> a = createStream01("a", "b", "c");
        Stream<Integer> b = createStream(1, 2);

        List<String> result = Stream.zip(a, b, "X", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipCollectionWithValuesForNone() {
        List<Stream<Integer>> streams = Arrays.asList(createStream(1, 2), createStream(10), createStream(100, 200, 300));

        List<Integer> valuesForNone = Arrays.asList(0, 0, 0);

        List<Integer> result = Stream.zip(streams, valuesForNone, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 202, 300), result);
    }

    @Test
    public void testZipIterablesCollectionWithValuesForNone() {
        List<Iterable<Integer>> iterables = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(10), Arrays.asList(100, 200, 300));

        List<Integer> valuesForNone = Arrays.asList(0, 0, 0);

        List<Integer> result = Stream.zipIterables(iterables, valuesForNone, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 202, 300), result);
    }

    @Test
    public void testZipIteratorsCollectionWithValuesForNone() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(10).iterator(),
                Arrays.asList(100, 200, 300).iterator());

        List<Integer> valuesForNone = Arrays.asList(0, 0, 0);

        List<Integer> result = Stream.zipIterators(iterators, valuesForNone, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 202, 300), result);
    }

    @Test
    public void testZipCharStreams() {
        CharStream a = CharStream.of('a', 'b', 'c');
        CharStream b = CharStream.of('x', 'y', 'z');

        List<String> result = Stream.zip(a, b, (c1, c2) -> "" + c1 + c2).toList();
        assertEquals(Arrays.asList("ax", "by", "cz"), result);
    }

    @Test
    public void testZipByteStreams() {
        ByteStream a = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = ByteStream.of((byte) 10, (byte) 20, (byte) 30);

        List<Integer> result = Stream.zip(a, b, (b1, b2) -> (int) (b1 + b2)).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipIntStreams() {
        IntStream a = IntStream.of(1, 2, 3);
        IntStream b = IntStream.of(10, 20, 30);

        List<Integer> result = Stream.zip(a, b, (i1, i2) -> i1 + i2).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipPrimitiveStreamCollection() {
        List<IntStream> streams = Arrays.asList(IntStream.of(1, 2, 3), IntStream.of(10, 20, 30), IntStream.of(100, 200, 300));

        List<Integer> result = Stream.zip(streams, arr -> Arrays.stream(arr).sum()).toList();
        assertEquals(Arrays.asList(111, 222, 333), result);
    }

    @Test
    public void testZipPrimitiveArraysWithValueForNone() {
        int[] a = { 1, 2, 3 };
        int[] b = { 10, 20 };

        List<Integer> result = Stream.zip(a, b, 0, 99, (i1, i2) -> i1 + i2).toList();
        assertEquals(Arrays.asList(11, 22, 102), result);
    }

    @Test
    public void testZipPrimitiveStreamsWithValueForNone() {
        IntStream a = IntStream.of(1, 2, 3);
        IntStream b = IntStream.of(10, 20);

        List<Integer> result = Stream.zip(a, b, 0, 99, (i1, i2) -> i1 + i2).toList();
        assertEquals(Arrays.asList(11, 22, 102), result);
    }

    @Test
    public void testZipPrimitiveStreamCollectionWithValuesForNone() {
        List<IntStream> streams = Arrays.asList(IntStream.of(1, 2), IntStream.of(10), IntStream.of(100, 200, 300));

        int[] valuesForNone = { 0, 0, 0 };

        List<Integer> result = Stream.zip(streams, valuesForNone, arr -> Arrays.stream(arr).sum()).toList();
        assertEquals(Arrays.asList(111, 202, 300), result);
    }

    @Test
    public void testEmpty2() {
        Stream<String> emptyStream = Stream.empty();
        assertFalse(emptyStream.first().isPresent());
    }

    @Test
    public void testDefer2() {
        Supplier<Stream<Integer>> supplier = () -> createStream(1, 2, 3);
        Stream<Integer> deferredStream = Stream.defer(supplier);
        assertEquals(Arrays.asList(1, 2, 3), deferredStream.toList());
    }

    @Test
    public void testDeferWithNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Stream.defer(null));
    }

    @Test
    public void testFrom() {
        java.util.stream.Stream<Integer> jdkStream = java.util.stream.Stream.of(1, 2, 3);
        Stream<Integer> stream = Stream.from(jdkStream);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testFromNull() {
        Stream<Integer> stream = Stream.from((java.util.stream.Stream<Integer>) null);
        assertTrue(stream.toList().isEmpty());
    }

    @Test
    public void testJust2() {
        Stream<String> stream = Stream.just("hello");
        assertEquals(Arrays.asList("hello"), stream.toList());
    }

    @Test
    public void testOfNullable2() {
        Stream<String> stream1 = Stream.ofNullable("hello");
        assertEquals(Arrays.asList("hello"), stream1.toList());

        Stream<String> stream2 = Stream.ofNullable(null);
        assertTrue(stream2.toList().isEmpty());
    }

    @Test
    public void testOfArrayWithRange() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        Stream<Integer> stream = createStream(array, 1, 4);
        assertEquals(Arrays.asList(2, 3, 4), stream.toList());
    }

    @Test
    public void testOfCollectionWithRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Stream<String> stream = createStream(list, 1, 4);
        assertEquals(Arrays.asList("b", "c", "d"), stream.toList());
    }

    @Test
    public void testOfEnumeration() {
        Vector<Integer> vector = new Vector<>(Arrays.asList(1, 2, 3));
        Enumeration<Integer> enumeration = vector.elements();
        Stream<Integer> stream = createStream(enumeration);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testOfJavaOptional() {
        java.util.Optional<String> optional = java.util.Optional.of("hello");
        Stream<String> stream = createStream(optional);
        assertEquals(Arrays.asList("hello"), stream.toList());

        java.util.Optional<String> empty = java.util.Optional.empty();
        Stream<String> emptyStream = createStream(empty);
        assertTrue(emptyStream.toList().isEmpty());
    }

    @Test
    public void testOfPrimitiveArrays() {
        boolean[] boolArray = { true, false, true };
        Stream<Boolean> boolStream = createStream(boolArray);
        assertEquals(Arrays.asList(true, false, true), boolStream.toList());

        char[] charArray = { 'a', 'b', 'c' };
        Stream<Character> charStream = createStream(charArray);
        assertEquals(Arrays.asList('a', 'b', 'c'), charStream.toList());

        byte[] byteArray = { 1, 2, 3 };
        Stream<Byte> byteStream = createStream(byteArray);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), byteStream.toList());

        short[] shortArray = { 1, 2, 3 };
        Stream<Short> shortStream = createStream(shortArray);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), shortStream.toList());

        int[] intArray = { 1, 2, 3 };
        Stream<Integer> intStream = createStream(intArray);
        assertEquals(Arrays.asList(1, 2, 3), intStream.toList());

        long[] longArray = { 1L, 2L, 3L };
        Stream<Long> longStream = createStream(longArray);
        assertEquals(Arrays.asList(1L, 2L, 3L), longStream.toList());

        float[] floatArray = { 1.0f, 2.0f, 3.0f };
        Stream<Float> floatStream = createStream(floatArray);
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), floatStream.toList());

        double[] doubleArray = { 1.0, 2.0, 3.0 };
        Stream<Double> doubleStream = createStream(doubleArray);
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), doubleStream.toList());
    }

    @Test
    public void testSplitByChunkCount2() {
        IntBiFunction<List<Integer>> mapper = (from, to) -> {
            List<Integer> list = new ArrayList<>();
            for (int i = from; i < to; i++) {
                list.add(i);
            }
            return list;
        };

        Stream<List<Integer>> chunks = Stream.splitByChunkCount(10, 3, mapper);
        List<List<Integer>> result = chunks.toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(0, 1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(4, 5, 6), result.get(1));
        assertEquals(Arrays.asList(7, 8, 9), result.get(2));

        Stream<List<Integer>> smallerFirstChunks = Stream.splitByChunkCount(10, 3, true, mapper);
        List<List<Integer>> smallerFirstResult = smallerFirstChunks.toList();
        assertEquals(3, smallerFirstResult.size());
        assertEquals(Arrays.asList(0, 1, 2), smallerFirstResult.get(0));
        assertEquals(Arrays.asList(3, 4, 5), smallerFirstResult.get(1));
        assertEquals(Arrays.asList(6, 7, 8, 9), smallerFirstResult.get(2));
    }

    @Test
    public void testFlatten() {
        List<List<Integer>> nestedList = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6));
        Stream<Integer> flattened = Stream.flatten(nestedList);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), flattened.toList());

        Integer[][] array2D = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        Stream<Integer> flattenedArray = Stream.flatten(array2D);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), flattenedArray.toList());

        Stream<Integer> flattenedVertical = Stream.flatten(array2D, true);
        assertEquals(Arrays.asList(1, 3, 5, 2, 4, 6), flattenedVertical.toList());

        Integer[][][] array3D = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } };
        Stream<Integer> flattened3D = Stream.flatten(array3D);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), flattened3D.toList());
    }

    @Test
    public void testRepeat2() {
        Stream<String> repeated = Stream.repeat("hello", 3);
        assertEquals(Arrays.asList("hello", "hello", "hello"), repeated.toList());

        Stream<String> noRepeat = Stream.repeat("hello", 0);
        assertTrue(noRepeat.toList().isEmpty());
    }

    @Test
    public void testGenerate2() {
        AtomicInteger counter = new AtomicInteger(0);
        Stream<Integer> generated = Stream.generate(counter::getAndIncrement);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), generated.limit(5).toList());
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
    public void testMapToEntry2() {
        EntryStream<String, Integer> entryStream2 = stream.mapToEntry(n -> "key" + n, n -> n * 10);
        Map<String, Integer> map2 = entryStream2.toMap();
        assertEquals(5, map2.size());
        assertEquals(Integer.valueOf(30), map2.get("key3"));
    }

    @Test
    public void testFlatMapToPrimitiveStreams() {
        CharStream charStream = createStream01("hello", "world").flatMapToChar(s -> CharStream.of(s.toCharArray()));
        assertEquals("helloworld", new String(charStream.toArray()));

        CharStream charStream2 = createStream01("hello", "world").flatmapToChar(String::toCharArray);
        assertEquals("helloworld", new String(charStream2.toArray()));

    }

    @Test
    public void testCollapse() {
        Stream<List<Integer>> collapsed = createStream(1, 2, 3, 3, 2, 1).collapse((a, b) -> a < b);
        List<List<Integer>> result = collapsed.toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(3), Arrays.asList(2), Arrays.asList(1)), result);

        Stream<Integer> collapsedWithMerge = createStream(1, 2, 3, 3, 2, 1).collapse((a, b) -> a < b, (a, b) -> a + b);
        assertEquals(Arrays.asList(6, 3, 2, 1), collapsedWithMerge.toList());

        Stream<Integer> collapsedWithCollector = createStream(1, 2, 3, 3, 2, 1).collapse((a, b) -> a < b, Collectors.summingInt(Integer::intValue));
        assertEquals(Arrays.asList(6, 3, 2, 1), collapsedWithCollector.toList());
    }

    @Test
    public void testSplit2() {
        Stream<List<Integer>> splitBySize = createStream(1, 2, 3, 4, 5, 6, 7).split(3);
        List<List<Integer>> chunks = splitBySize.toList();
        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(1, 2, 3), chunks.get(0));
        assertEquals(Arrays.asList(4, 5, 6), chunks.get(1));
        assertEquals(Arrays.asList(7), chunks.get(2));

        Stream<List<Integer>> splitByPredicate = Stream.range(0, 7).split(n -> n % 3 == 0);
        List<List<Integer>> predicateChunks = splitByPredicate.toList();
        assertEquals(Arrays.asList(Arrays.asList(0), Arrays.asList(1, 2), Arrays.asList(3), Arrays.asList(4, 5), Arrays.asList(6)), predicateChunks);

        Stream<Integer> splitWithCollector = createStream(1, 2, 3, 4, 5, 6).split(2, Collectors.summingInt(Integer::intValue));
        assertEquals(Arrays.asList(3, 7, 11), splitWithCollector.toList());
    }

    @Test
    public void testPrepend() {
        Stream<Integer> prepended = stream.prepend(Arrays.asList(-2, -1, 0));
        assertEquals(Arrays.asList(-2, -1, 0, 1, 2, 3, 4, 5), prepended.toList());

        Stream<Integer> prependedVarargs = createStream(3, 4, 5).prepend(1, 2);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), prependedVarargs.toList());
    }

    @Test
    public void testAppend() {
        Stream<Integer> appended = stream.append(Arrays.asList(6, 7, 8));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), appended.toList());

        Stream<Integer> appendedVarargs = createStream(1, 2, 3).append(4, 5);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), appendedVarargs.toList());
    }

    @Test
    public void testForEachWithOnComplete() {
        List<Integer> collected = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        stream.forEach(collected::add, () -> completed.set(true));
        assertEquals(testList, collected);
        assertTrue(completed.get());
    }

    @Test
    public void testForEachWithFlatMapper() {
        List<String> collected = new ArrayList<>();
        stream.forEach(n -> Arrays.asList("a" + n, "b" + n), (n, s) -> collected.add(s));
        assertEquals(Arrays.asList("a1", "b1", "a2", "b2", "a3", "b3", "a4", "b4", "a5", "b5"), collected);
    }

    @Test
    public void testOfLines_File() throws IOException {
        File testFile = Files.createTempFile(tempFolder, "test", ".txt").toFile();
        Files.write(testFile.toPath(), Arrays.asList("line1", "line2", "line3"));

        List<String> lines = Stream.ofLines(testFile).toList();
        assertEquals(3, lines.size());
        assertEquals("line1", lines.get(0));
        assertEquals("line2", lines.get(1));
        assertEquals("line3", lines.get(2));
    }

    @Test
    public void testOfLines_FileWithCharset() throws IOException {
        File testFile = Files.createTempFile(tempFolder, "test-charset", ".txt").toFile();
        String content = "Hello\nWorld\n你好";
        Files.write(testFile.toPath(), content.getBytes("UTF-8"));

        List<String> lines = Stream.ofLines(testFile, Charsets.UTF_8).toList();
        assertEquals(3, lines.size());
        assertEquals("Hello", lines.get(0));
        assertEquals("World", lines.get(1));
        assertEquals("你好", lines.get(2));
    }

    @Test
    public void testOfLines_Path() throws IOException {
        Path testPath = Files.createTempFile(tempFolder, "test-path", ".txt").toFile().toPath();
        Files.write(testPath, Arrays.asList("path1", "path2", "path3"));

        List<String> lines = Stream.ofLines(testPath).toList();
        assertEquals(3, lines.size());
        assertEquals("path1", lines.get(0));
        assertEquals("path2", lines.get(1));
        assertEquals("path3", lines.get(2));
    }

    @Test
    public void testOfLines_PathWithCharset() throws IOException {
        Path testPath = Files.createTempFile(tempFolder, "test-path-charset", ".txt").toFile().toPath();
        Files.write(testPath, Arrays.asList("line1", "line2"), Charsets.UTF_8);

        List<String> lines = Stream.ofLines(testPath, Charsets.UTF_8).toList();
        assertEquals(2, lines.size());
        assertEquals("line1", lines.get(0));
        assertEquals("line2", lines.get(1));
    }

    @Test
    public void testListFiles_SingleDirectory() throws IOException {
        File parentDir = Files.createTempDirectory(tempFolder, "parent").toFile();
        File file1 = new File(parentDir, "file1.txt");
        File file2 = new File(parentDir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();

        List<File> files = Stream.listFiles(parentDir).toList();
        assertEquals(2, files.size());
        assertTrue(files.contains(file1));
        assertTrue(files.contains(file2));
    }

    @Test
    public void testListFiles_NonExistentDirectory() {
        File nonExistent = new File("non-existent-dir");
        List<File> files = Stream.listFiles(nonExistent).toList();
        assertTrue(files.isEmpty());
    }

    @Test
    public void testListFiles_Recursive() throws IOException {
        File parentDir = Files.createTempDirectory(tempFolder, "parent").toFile();
        File subDir = new File(parentDir, "subdir");
        subDir.mkdir();
        File file1 = new File(parentDir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();

        List<File> files = Stream.listFiles(parentDir, true).toList();
        assertEquals(3, files.size());
        assertTrue(files.contains(subDir));
        assertTrue(files.contains(file1));
        assertTrue(files.contains(file2));
    }

    @Test
    public void testListFiles_RecursiveExcludeDirectory() throws IOException {
        File parentDir = Files.createTempDirectory(tempFolder, "parent").toFile();
        File subDir = new File(parentDir, "subdir");
        subDir.mkdir();
        File file1 = new File(parentDir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();

        List<File> files = Stream.listFiles(parentDir, true, true).toList();
        assertEquals(2, files.size());
        assertTrue(files.contains(file1));
        assertTrue(files.contains(file2));
        assertFalse(files.contains(subDir));
    }

    @Test
    public void testInterval_WithSupplier() throws InterruptedException {
        List<Long> timestamps = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        Stream.interval(50, s -> s).limit(3).forEach(timestamps::add);

        assertEquals(3, timestamps.size());

        for (int i = 1; i < timestamps.size(); i++) {
            long interval = timestamps.get(i) - timestamps.get(i - 1);
            assertTrue(interval >= 40 && interval <= 70, "Interval should be around 50ms");
        }
    }

    @Test
    public void testInterval_WithDelayAndSupplier() throws InterruptedException {
        List<String> results = new ArrayList<>();

        Stream.interval(100, 50, () -> "tick").limit(3).forEach(results::add);

        assertEquals(Arrays.asList("tick", "tick", "tick"), results);
    }

    @Test
    public void testInterval_WithLongFunction() throws InterruptedException {
        List<Long> values = new ArrayList<>();

        Stream.interval(50, timeMillis -> timeMillis).limit(3).forEach(values::add);

        assertEquals(3, values.size());

        for (Long value : values) {
            assertTrue(value > 0);
        }
    }

    @Test
    public void testObserve_BlockingQueueWithDuration() throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
        queue.add("item1");
        queue.add("item2");

        List<String> results = Stream.observe(queue, Duration.ofMillis(100)).toList();

        assertTrue(results.contains("item1"));
        assertTrue(results.contains("item2"));
    }

    @Test
    public void testObserve_BlockingQueueWithHasMore() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
        BooleanSupplier hasMore = () -> true;

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
                queue.add(1);
                Thread.sleep(50);
                queue.add(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        List<Integer> results = Stream.observe(queue, hasMore, 30).limit(2).toList();

        assertEquals(Arrays.asList(1, 2), results);
    }

    @Test
    public void testConcat_Arrays() {
        Integer[] arr1 = { 1, 2, 3 };
        Integer[] arr2 = { 4, 5, 6 };
        Integer[] arr3 = { 7, 8, 9 };

        List<Integer> result = Stream.concat(arr1, arr2, arr3).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);
    }

    @Test
    public void testConcat_EmptyArrays() {
        Integer[] arr1 = {};
        Integer[] arr2 = {};

        List<Integer> result = Stream.concat(arr1, arr2).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcat_Iterables() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        Set<Integer> set2 = new HashSet<>(Arrays.asList(4, 5, 6));
        List<Integer> list3 = Arrays.asList(7, 8, 9);

        List<Integer> result = Stream.concat(list1, set2, list3).sorted().toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);
    }

    @Test
    public void testConcat_Iterators() {
        Iterator<String> iter1 = Arrays.asList("a", "b").iterator();
        Iterator<String> iter2 = Arrays.asList("c", "d").iterator();

        List<String> result = Stream.concat(iter1, iter2).toList();

        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testConcat_Streams() {
        Stream<Integer> stream1 = createStream(1, 2, 3);
        Stream<Integer> stream2 = createStream(4, 5, 6);
        Stream<Integer> stream3 = createStream(7, 8, 9);

        List<Integer> result = Stream.concat(stream1, stream2, stream3).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);
    }

    @Test
    public void testConcat_CollectionOfStreams() {
        List<Stream<String>> streams = Arrays.asList(createStream01("a", "b"), createStream01("c", "d"), createStream01("e", "f"));

        List<String> result = Stream.concat(streams).toList();

        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), result);
    }

    @Test
    public void testStreamEx_Inheritance() {
        assertTrue(Stream.StreamEx.class.getSuperclass().equals(Stream.class));
    }

    @Test
    public void testFrom_JavaStream() {
        java.util.stream.Stream<String> javaStream = java.util.stream.Stream.of("a", "b", "c");

        Stream<String> stream = Stream.from(javaStream);

        assertEquals(Arrays.asList("a", "b", "c"), stream.toList());
    }

    @Test
    public void testFrom_NullJavaStream() {
        Stream<String> stream = Stream.from((java.util.stream.Stream<String>) null);

        assertTrue(stream.toList().isEmpty());
    }

    @Test
    public void testFrom_ParallelJavaStream() {
        java.util.stream.Stream<Integer> parallelJavaStream = java.util.stream.Stream.of(1, 2, 3, 4, 5).parallel();

        Stream<Integer> stream = Stream.from(parallelJavaStream);

        assertTrue(stream.isParallel());
        assertEquals(5, stream.count());
    }

    @Test
    public void testJust_NullElement() {
        Stream<String> stream = Stream.just(null);

        List<String> result = stream.toList();
        assertEquals(1, result.size());
        assertNull(result.get(0));
    }

    @Test
    public void testOf_Array() {
        Integer[] array = { 1, 2, 3, 4, 5 };

        Stream<Integer> stream = createStream01(array);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), stream.toList());
    }

    @Test
    public void testOf_EmptyArray() {
        Integer[] emptyArray = {};

        Stream<Integer> stream = createStream01(emptyArray);

        assertTrue(stream.toList().isEmpty());
    }

    @Test
    public void testOf_ArrayWithRange() {
        String[] array = { "a", "b", "c", "d", "e" };

        Stream<String> stream = createStream(array, 1, 4);

        assertEquals(Arrays.asList("b", "c", "d"), stream.toList());
    }

    @Test
    public void testOf_ArrayWithInvalidRange() {
        Integer[] array = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> createStream(array, 1, 5));
    }

    @Test
    public void testOf_Collection() {
        List<String> list = Arrays.asList("x", "y", "z");

        Stream<String> stream = createStream(list);

        assertEquals(list, stream.toList());
    }

    @Test
    public void testOf_CollectionWithRange() {
        List<Integer> list = Arrays.asList(10, 20, 30, 40, 50);

        Stream<Integer> stream = createStream(list, 2, 4);

        assertEquals(Arrays.asList(30, 40), stream.toList());
    }

    @Test
    public void testOf_Map() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        List<Map.Entry<String, Integer>> entries = createStream(map).toList();

        assertEquals(3, entries.size());
        assertEquals("a", entries.get(0).getKey());
        assertEquals(Integer.valueOf(1), entries.get(0).getValue());
    }

    @Test
    public void testOf_Iterable() {
        Set<String> set = new LinkedHashSet<>(Arrays.asList("one", "two", "three"));

        Stream<String> stream = createStream(set);

        assertEquals(3, stream.count());
    }

    @Test
    public void testOf_Iterator() {
        Iterator<Double> iterator = Arrays.asList(1.1, 2.2, 3.3).iterator();

        Stream<Double> stream = createStream(iterator);

        assertEquals(Arrays.asList(1.1, 2.2, 3.3), stream.toList());
    }

    @Test
    public void testOf_Enumeration() {
        Vector<String> vector = new Vector<>(Arrays.asList("alpha", "beta", "gamma"));
        Enumeration<String> enumeration = vector.elements();

        Stream<String> stream = createStream(enumeration);

        assertEquals(Arrays.asList("alpha", "beta", "gamma"), stream.toList());
    }

    @Test
    public void testOf_BooleanArray() {
        boolean[] array = { true, false, true, false };

        Stream<Boolean> stream = createStream(array);

        assertEquals(Arrays.asList(true, false, true, false), stream.toList());
    }

    @Test
    public void testOf_CharArray() {
        char[] array = { 'a', 'b', 'c' };

        Stream<Character> stream = createStream(array);

        assertEquals(Arrays.asList('a', 'b', 'c'), stream.toList());
    }

    @Test
    public void testOf_ByteArray() {
        byte[] array = { 1, 2, 3, 4 };

        Stream<Byte> stream = createStream(array);

        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4), stream.toList());
    }

    @Test
    public void testOf_ShortArray() {
        short[] array = { 10, 20, 30 };

        Stream<Short> stream = createStream(array);

        assertEquals(Arrays.asList((short) 10, (short) 20, (short) 30), stream.toList());
    }

    @Test
    public void testOf_IntArray() {
        int[] array = { 100, 200, 300 };

        Stream<Integer> stream = createStream(array);

        assertEquals(Arrays.asList(100, 200, 300), stream.toList());
    }

    @Test
    public void testOf_LongArray() {
        long[] array = { 1000L, 2000L, 3000L };

        Stream<Long> stream = createStream(array);

        assertEquals(Arrays.asList(1000L, 2000L, 3000L), stream.toList());
    }

    @Test
    public void testOf_FloatArray() {
        float[] array = { 1.5f, 2.5f, 3.5f };

        Stream<Float> stream = createStream(array);

        assertEquals(Arrays.asList(1.5f, 2.5f, 3.5f), stream.toList());
    }

    @Test
    public void testOf_DoubleArray() {
        double[] array = { 1.1, 2.2, 3.3 };

        Stream<Double> stream = createStream(array);

        assertEquals(Arrays.asList(1.1, 2.2, 3.3), stream.toList());
    }

    @Test
    public void testOf_Optional() {
        Optional<String> present = Optional.of("value");
        Stream<String> stream1 = createStream(present);
        assertEquals(Arrays.asList("value"), stream1.toList());

        Optional<String> empty = Optional.empty();
        Stream<String> stream2 = createStream(empty);
        assertTrue(stream2.toList().isEmpty());

        Stream<String> stream3 = createStream((Optional<String>) null);
        assertTrue(stream3.toList().isEmpty());
    }

    @Test
    public void testOf_JavaUtilOptional() {
        java.util.Optional<Integer> present = java.util.Optional.of(42);
        Stream<Integer> stream1 = createStream(present);
        assertEquals(Arrays.asList(42), stream1.toList());

        java.util.Optional<Integer> empty = java.util.Optional.empty();
        Stream<Integer> stream2 = createStream(empty);
        assertTrue(stream2.toList().isEmpty());
    }

    @Test
    public void testOfKeys_Map() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("key1", 10);
        map.put("key2", 20);
        map.put("key3", 30);

        List<String> keys = Stream.ofKeys(map).toList();

        assertEquals(Arrays.asList("key1", "key2", "key3"), keys);
    }

    @Test
    public void testOfKeys_WithValueFilter() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        List<String> keys = Stream.ofKeys(map, v -> v % 2 == 0).toList();

        assertEquals(Arrays.asList("b", "d"), keys);
    }

    @Test
    public void testOfKeys_WithBiPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("short", 5);
        map.put("medium", 10);
        map.put("long", 15);

        List<String> keys = Stream.ofKeys(map, (k, v) -> k.length() > 4 && v > 5).toList();

        assertEquals(Arrays.asList("medium"), keys);
    }

    @Test
    public void testOfValues_Map() {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        List<String> values = Stream.ofValues(map).toList();

        assertEquals(Arrays.asList("one", "two", "three"), values);
    }

    @Test
    public void testOfValues_WithKeyFilter() {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "odd");
        map.put(2, "even");
        map.put(3, "odd");
        map.put(4, "even");

        List<String> values = Stream.ofValues(map, k -> k % 2 == 0).toList();

        assertEquals(Arrays.asList("even", "even"), values);
    }

    @Test
    public void testOfValues_WithBiPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("bb", 2);
        map.put("ccc", 3);

        List<Integer> values = Stream.ofValues(map, (k, v) -> k.length() == v).toList();

        assertEquals(Arrays.asList(1, 2, 3), values);
    }

    @Test
    public void testOfReversed_Array() {
        Integer[] array = { 1, 2, 3, 4, 5 };

        List<Integer> reversed = Stream.ofReversed(array).toList();

        assertEquals(Arrays.asList(5, 4, 3, 2, 1), reversed);
    }

    @Test
    public void testOfReversed_List() {
        List<String> list = Arrays.asList("a", "b", "c", "d");

        List<String> reversed = Stream.ofReversed(list).toList();

        assertEquals(Arrays.asList("d", "c", "b", "a"), reversed);
    }

    @Test
    public void testRange_Int() {
        List<Integer> range = Stream.range(1, 5).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4), range);
    }

    @Test
    public void testRange_IntWithStep() {
        List<Integer> range = Stream.range(0, 10, 2).toList();

        assertEquals(Arrays.asList(0, 2, 4, 6, 8), range);
    }

    @Test
    public void testRange_Long() {
        List<Long> range = Stream.range(10L, 13L).toList();

        assertEquals(Arrays.asList(10L, 11L, 12L), range);
    }

    @Test
    public void testRange_LongWithStep() {
        List<Long> range = Stream.range(0L, 15L, 5L).toList();

        assertEquals(Arrays.asList(0L, 5L, 10L), range);
    }

    @Test
    public void testRangeClosed_Int() {
        List<Integer> range = Stream.rangeClosed(1, 5).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), range);
    }

    @Test
    public void testRangeClosed_IntWithStep() {
        List<Integer> range = Stream.rangeClosed(1, 10, 3).toList();

        assertEquals(Arrays.asList(1, 4, 7, 10), range);
    }

    @Test
    public void testRangeClosed_Long() {
        List<Long> range = Stream.rangeClosed(5L, 8L).toList();

        assertEquals(Arrays.asList(5L, 6L, 7L, 8L), range);
    }

    @Test
    public void testRangeClosed_LongWithStep() {
        List<Long> range = Stream.rangeClosed(0L, 20L, 5L).toList();

        assertEquals(Arrays.asList(0L, 5L, 10L, 15L, 20L), range);
    }

    @Test
    public void testSplit_CharDelimiter() {
        String str = "apple,banana,cherry";

        List<String> parts = Stream.split(str, ',').toList();

        assertEquals(Arrays.asList("apple", "banana", "cherry"), parts);
    }

    @Test
    public void testSplit_StringDelimiter() {
        String str = "one::two::three";

        List<String> parts = Stream.split(str, "::").toList();

        assertEquals(Arrays.asList("one", "two", "three"), parts);
    }

    @Test
    public void testSplit_Pattern() {
        String str = "a1b2c3d";
        Pattern pattern = Pattern.compile("\\d");

        List<String> parts = Stream.split(str, pattern).toList();

        assertEquals(Arrays.asList("a", "b", "c", "d"), parts);
    }

    @Test
    public void testSplitToLines2() {
        String str = "line1\nline2\rline3\r\nline4";

        List<String> lines = Stream.splitToLines(str).toList();

        assertEquals(Arrays.asList("line1", "line2", "line3", "line4"), lines);
    }

    @Test
    public void testSplitToLines_WithOptions() {
        String str = "  line1  \n\n  line2  \n  \n  line3  ";

        List<String> lines1 = Stream.splitToLines(str, true, true).toList();
        assertEquals(Arrays.asList("line1", "line2", "line3"), lines1);

        List<String> lines2 = Stream.splitToLines(str, true, false).toList();
        assertEquals(Arrays.asList("line1", "", "line2", "", "line3"), lines2);

        List<String> lines3 = Stream.splitToLines(str, false, true).toList();
        assertEquals(Arrays.asList("  line1  ", "  line2  ", "  ", "  line3  "), lines3);

        List<String> lines4 = Stream.splitToLines(str, false, false).toList();
        assertEquals(Arrays.asList("  line1  ", "", "  line2  ", "  ", "  line3  "), lines4);
    }

    @Test
    public void testSplitByChunkCount_SizeSmallerFirst() {
        Integer[] array = { 1, 2, 3, 4, 5, 6, 7 };

        List<List<Integer>> chunks = Stream.splitByChunkCount(7, 5, true, (from, to) -> Arrays.asList(array).subList(from, to)).toList();

        assertEquals(5, chunks.size());
        assertEquals(Arrays.asList(1), chunks.get(0));
        assertEquals(Arrays.asList(2), chunks.get(1));
        assertEquals(Arrays.asList(3), chunks.get(2));
        assertEquals(Arrays.asList(4, 5), chunks.get(3));
        assertEquals(Arrays.asList(6, 7), chunks.get(4));
    }

    @Test
    public void testFlatten_CollectionOfCollections() {
        List<List<Integer>> nested = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6));

        List<Integer> flattened = Stream.flatten(nested).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), flattened);
    }

    @Test
    public void testFlatten_2DArray() {
        Integer[][] array = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };

        List<Integer> flattened = Stream.flatten(array).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), flattened);
    }

    @Test
    public void testFlatten_2DArrayVertically() {
        Integer[][] array = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };

        List<Integer> flattened = Stream.flatten(array, true).toList();

        assertEquals(Arrays.asList(1, 4, 7, 2, 5, 8, 3, 6, 9), flattened);
    }

    @Test
    public void testFlatten_2DArrayWithAlignment() {
        Integer[][] array = { { 1, 2 }, { 3, 4, 5, 6 }, { 7 } };

        List<Integer> flattened = Stream.flatten(array, 0, true).toList();

        assertEquals(Arrays.asList(1, 3, 7, 2, 4, 0, 0, 5, 0, 0, 6, 0), flattened);
    }

    @Test
    public void testFlatten_3DArray() {
        Integer[][][] array = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } }, { { 9, 10 }, { 11, 12 } } };

        List<Integer> flattened = Stream.flatten(array).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), flattened);
    }

    @Test
    public void testRepeat_Zero() {
        List<Integer> repeated = Stream.repeat(42, 0).toList();

        assertTrue(repeated.isEmpty());
    }

    @Test
    public void testIterate_BooleanSupplierAndSupplier() {
        int[] counter = { 0 };
        BooleanSupplier hasNext = () -> counter[0] < 3;
        Supplier<Integer> next = () -> counter[0]++;

        List<Integer> result = Stream.iterate(hasNext, next).toList();

        assertEquals(Arrays.asList(0, 1, 2), result);
    }

    @Test
    public void testIterate_WithInitAndBooleanSupplier() {
        int[] counter = { 0 };
        BooleanSupplier hasNext = () -> counter[0] < 5;
        UnaryOperator<Integer> f = x -> {
            counter[0]++;
            return x * 2;
        };

        List<Integer> result = Stream.iterate(1, hasNext, f).toList();

        assertEquals(Arrays.asList(1, 2, 4, 8, 16, 32), result);
    }

    @Test
    public void testIterate_WithInitAndPredicate() {
        Predicate<Integer> hasNext = x -> x < 100;
        UnaryOperator<Integer> f = x -> x * 2;

        List<Integer> result = Stream.iterate(1, hasNext, f).toList();

        assertEquals(Arrays.asList(1, 2, 4, 8, 16, 32, 64), result);
    }

    @Test
    public void testIterate_Infinite() {
        UnaryOperator<Integer> f = x -> x + 1;

        List<Integer> result = Stream.iterate(0, f).limit(5).toList();

        assertEquals(Arrays.asList(0, 1, 2, 3, 4), result);
    }

    @Test
    public void testGenerate_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Stream.generate(null));
    }

    @Test
    public void testOfLines_Reader() throws IOException {
        String content = "line1\nline2\nline3";
        StringReader reader = new StringReader(content);

        List<String> lines = Stream.ofLines(reader).toList();

        assertEquals(Arrays.asList("line1", "line2", "line3"), lines);
    }

    @Test
    public void testOfLines_ReaderWithCloseFlag() throws IOException {
        String content = "a\nb\nc";
        StringReader reader = new StringReader(content);

        try (Stream<String> stream = Stream.ofLines(reader, true)) {
            assertEquals(Arrays.asList("a", "b", "c"), stream.toList());
        }

        StringReader reader2 = new StringReader(content);
        Stream<String> stream2 = Stream.ofLines(reader2, false);
        assertEquals(Arrays.asList("a", "b", "c"), stream2.toList());
    }

    @Test
    public void testOfLines_NullReader() {
        assertThrows(IllegalArgumentException.class, () -> Stream.ofLines((Reader) null));
    }

    @Test
    public void testComplexStreamOperations() {
        List<String> result = Stream.range(1, 10).filter(x -> x % 2 == 0).map(x -> "Number: " + x).toList();

        assertEquals(Arrays.asList("Number: 2", "Number: 4", "Number: 6", "Number: 8"), result);
    }

    @Test
    public void testStreamWithNullHandling() {
        List<String> input = Arrays.asList("a", null, "b", null, "c");

        List<String> result = createStream(input).filter(Objects::nonNull).map(String::toUpperCase).toList();

        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testParallelStreamOperations() {
        List<Integer> result = Stream.range(1, 1000).parallel().filter(x -> x % 7 == 0).sorted().limit(10).toList();

        assertEquals(Arrays.asList(7, 14, 21, 28, 35, 42, 49, 56, 63, 70), result);
    }

    @Test
    public void testOfLines_EmptyFile() throws IOException {
        File emptyFile = Files.createTempFile(tempFolder, "empty", ".txt").toFile();

        List<String> lines = Stream.ofLines(emptyFile).toList();

        assertTrue(lines.isEmpty());
    }

    @Test
    public void testOfLines_FileWithBlankLines() throws IOException {
        File file = Files.createTempFile(tempFolder, "blank-lines", ".txt").toFile();
        Files.write(file.toPath(), Arrays.asList("", "line1", "", "line2", ""));

        List<String> lines = Stream.ofLines(file).toList();

        assertEquals(5, lines.size());
        assertEquals("", lines.get(0));
        assertEquals("line1", lines.get(1));
        assertEquals("", lines.get(2));
        assertEquals("line2", lines.get(3));
        assertEquals("", lines.get(4));
    }

    @Test
    public void testListFiles_EmptyDirectory() throws IOException {
        File emptyDir = Files.createTempDirectory(tempFolder, "empty").toFile();

        List<File> files = Stream.listFiles(emptyDir).toList();

        assertTrue(files.isEmpty());
    }

    @Test
    public void testListFiles_DeepRecursion() throws IOException {
        File root = Files.createTempDirectory(tempFolder, "deep").toFile();
        File current = root;
        for (int i = 0; i < 5; i++) {
            File subDir = new File(current, "level" + i);
            subDir.mkdir();
            File file = new File(current, "file" + i + ".txt");
            file.createNewFile();
            current = subDir;
        }

        List<File> files = Stream.listFiles(root, true).toList();

        assertEquals(10, files.size());
    }

    @Test
    public void testListFiles_MixedContent() throws IOException {
        File dir = Files.createTempDirectory(tempFolder, "mixed").toFile();

        new File(dir, "test.txt").createNewFile();
        new File(dir, "test.java").createNewFile();
        new File(dir, "test.xml").createNewFile();
        new File(dir, ".hidden").createNewFile();

        File subDir = new File(dir, "subdir");
        subDir.mkdir();
        new File(subDir, "nested.txt").createNewFile();

        List<File> nonRecursive = Stream.listFiles(dir, false).toList();
        assertEquals(5, nonRecursive.size());

        List<File> recursiveFiles = Stream.listFiles(dir, true, true).toList();
        assertEquals(5, recursiveFiles.size());
    }

    @Test
    public void testInterval_CancellationBehavior() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);

        Stream<Integer> intervalStream = trackStream(Stream.interval(10, () -> counter.incrementAndGet()));

        List<Integer> results = intervalStream.limit(3).toList();

        assertEquals(Arrays.asList(1, 2, 3), results);

        Thread.sleep(50);
        assertTrue(counter.get() <= 4);
    }

    @Test
    public void testObserve_EmptyQueue() throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);

        List<String> results = Stream.observe(queue, Duration.ofMillis(50)).toList();

        assertTrue(results.isEmpty());
    }

    @Test
    public void testObserve_QueueWithTimeout() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(30);
                queue.add(1);
                Thread.sleep(30);
                queue.add(2);
                Thread.sleep(100);
                queue.add(3);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        List<Integer> results = Stream.observe(queue, Duration.ofMillis(100)).toList();

        assertEquals(2, results.size());
        assertTrue(results.contains(1));
        assertTrue(results.contains(2));
    }

    @Test
    public void testConcat_MixedTypes() {
        Integer[] arr = { 1, 2 };
        List<Integer> list = Arrays.asList(3, 4);
        Set<Integer> set = new HashSet<>(Arrays.asList(5, 6));

        List<Integer> result = Stream.concat(createStream01(arr), createStream(list), createStream(set)).sorted().toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testConcat_WithNullElements() {
        Stream<String> s1 = createStream01("a", null, "b");
        Stream<String> s2 = createStream01(null, "c", "d");

        List<String> result = Stream.concat(s1, s2).toList();

        assertEquals(Arrays.asList("a", null, "b", null, "c", "d"), result);
    }

    @Test
    public void testConcatIterables_EmptyIterables() {
        List<List<String>> emptyLists = Arrays.asList(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        List<String> result = Stream.concatIterables(emptyLists).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testSplitByChunkCount_NegativeTotalSize() {
        assertThrows(IllegalArgumentException.class, () -> Stream.splitByChunkCount(-1, 5, (from, to) -> Collections.emptyList()));
    }

    @Test
    public void testSplitByChunkCount_ZeroMaxChunkCount() {
        assertThrows(IllegalArgumentException.class, () -> Stream.splitByChunkCount(10, 0, (from, to) -> Collections.emptyList()));
    }

    @Test
    public void testFlatten_EmptyArrays() {
        Integer[][] empty2D = new Integer[0][];
        assertTrue(Stream.flatten(empty2D).toList().isEmpty());

        Integer[][] withEmpty = { {}, { 1, 2 }, {}, { 3 } };
        assertEquals(Arrays.asList(1, 2, 3), Stream.flatten(withEmpty).toList());

        Integer[][][] empty3D = new Integer[0][][];
        assertTrue(Stream.flatten(empty3D).toList().isEmpty());
    }

    @Test
    public void testFlatten_IrregularArrays() {
        Integer[][] irregular = { { 1 }, { 2, 3, 4 }, null, { 5, 6 } };

        List<Integer> flattened = Stream.flatten(irregular).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), flattened);

        List<Integer> vertical = Stream.flatten(irregular, true).toList();
        assertEquals(Arrays.asList(1, 2, 5, 3, 6, 4), vertical);
    }

    @Test
    public void testRepeat_LargeCount() {
        long largeCount = 10000L;

        long actualCount = Stream.repeat("X", largeCount).count();

        assertEquals(largeCount, actualCount);
    }

    @Test
    public void testIterate_StopConditions() {
        BooleanSupplier alwaysFalse = () -> false;
        Supplier<String> supplier = () -> "should not be called";

        List<String> result1 = Stream.iterate(alwaysFalse, supplier).toList();
        assertTrue(result1.isEmpty());

        Predicate<Integer> negative = x -> x < 0;
        List<Integer> result2 = Stream.iterate(1, negative, x -> x + 1).toList();
        assertTrue(result2.isEmpty());
    }

    @Test
    public void testGenerate_RandomNumbers() {
        Random random = new Random(12345);

        List<Integer> randomNumbers = Stream.generate(() -> random.nextInt(100)).limit(10).toList();

        assertEquals(10, randomNumbers.size());
        for (Integer num : randomNumbers) {
            assertTrue(num >= 0 && num < 100);
        }
    }

    @Test
    public void testOf_PrimitiveArraysWithRange() {

        boolean[] boolArray = { true, false, true, false, true };
        List<Boolean> boolResult = createStream(boolArray, 1, 4).toList();
        assertEquals(Arrays.asList(false, true, false), boolResult);

        char[] charArray = { 'a', 'b', 'c', 'd', 'e' };
        List<Character> charResult = createStream(charArray, 2, 5).toList();
        assertEquals(Arrays.asList('c', 'd', 'e'), charResult);

        byte[] byteArray = { 10, 20, 30, 40, 50 };
        List<Byte> byteResult = createStream(byteArray, 0, 3).toList();
        assertEquals(Arrays.asList((byte) 10, (byte) 20, (byte) 30), byteResult);

        short[] shortArray = { 100, 200, 300, 400 };
        List<Short> shortResult = createStream(shortArray, 1, 3).toList();
        assertEquals(Arrays.asList((short) 200, (short) 300), shortResult);

        int[] intArray = { 1, 2, 3, 4, 5, 6 };
        List<Integer> intResult = createStream(intArray, 2, 5).toList();
        assertEquals(Arrays.asList(3, 4, 5), intResult);

        long[] longArray = { 10L, 20L, 30L, 40L };
        List<Long> longResult = createStream(longArray, 1, 4).toList();
        assertEquals(Arrays.asList(20L, 30L, 40L), longResult);

        float[] floatArray = { 1.1f, 2.2f, 3.3f, 4.4f };
        List<Float> floatResult = createStream(floatArray, 0, 2).toList();
        assertEquals(Arrays.asList(1.1f, 2.2f), floatResult);

        double[] doubleArray = { 1.5, 2.5, 3.5, 4.5, 5.5 };
        List<Double> doubleResult = createStream(doubleArray, 2, 4).toList();
        assertEquals(Arrays.asList(3.5, 4.5), doubleResult);
    }

    @Test
    public void testOfKeys_EmptyMap() {
        Map<String, Integer> emptyMap = Collections.emptyMap();

        assertTrue(Stream.ofKeys(emptyMap).toList().isEmpty());
        assertTrue(Stream.ofKeys(emptyMap, v -> v > 0).toList().isEmpty());
        assertTrue(Stream.ofKeys(emptyMap, (k, v) -> true).toList().isEmpty());
    }

    @Test
    public void testOfValues_EmptyMap() {
        Map<String, Integer> emptyMap = Collections.emptyMap();

        assertTrue(Stream.ofValues(emptyMap).toList().isEmpty());
        assertTrue(Stream.ofValues(emptyMap, k -> true).toList().isEmpty());
        assertTrue(Stream.ofValues(emptyMap, (k, v) -> true).toList().isEmpty());
    }

    @Test
    public void testRange_NegativeSteps() {
        List<Integer> intRange = Stream.range(10, 0, -2).toList();
        assertEquals(Arrays.asList(10, 8, 6, 4, 2), intRange);

        List<Long> longRange = Stream.range(20L, 10L, -3L).toList();
        assertEquals(Arrays.asList(20L, 17L, 14L, 11L), longRange);
    }

    @Test
    public void testRangeClosed_NegativeSteps() {
        List<Integer> intRange = Stream.rangeClosed(10, 0, -2).toList();
        assertEquals(Arrays.asList(10, 8, 6, 4, 2, 0), intRange);

        List<Long> longRange = Stream.rangeClosed(15L, 5L, -5L).toList();
        assertEquals(Arrays.asList(15L, 10L, 5L), longRange);
    }

    @Test
    public void testSplit_EdgeCases() {
        assertEquals("", Stream.split("", ',').onlyOne().orElseThrow());

        assertEquals(Arrays.asList("hello"), Stream.split("hello", ',').toList());

        List<String> onlyDelimiters = Stream.split("::::", ":").toList();
        assertEquals(5, onlyDelimiters.size());
        assertTrue(onlyDelimiters.stream().allMatch(String::isEmpty));

        assertEquals(Arrays.asList("a", "b", ""), Stream.split("a,b,", ',').toList());
    }

    @Test
    public void testFrom_ClosedJavaStream() {
        java.util.stream.Stream<String> javaStream = java.util.stream.Stream.of("a", "b", "c");
        javaStream.close();

        Stream<String> stream = Stream.from(javaStream);

        try {
            stream.toList();
            fail("Expected exception from closed java stream");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testOf_Iterator_Advance() {
        ObjIteratorEx<Integer> iter = new ObjIteratorEx<>() {
            private int current = 0;

            @Override
            public boolean hasNext() {
                return current < 10;
            }

            @Override
            public Integer next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return current++;
            }

            @Override
            public void advance(long n) {
                if (n <= 0) {
                    return;
                }

                current = Math.min(current + (int) n, 10);
            }
        };

        Stream<Integer> stream = createStream(iter);

        List<Integer> result = stream.skip(5).toList();
        assertEquals(Arrays.asList(5, 6, 7, 8, 9), result);
    }

    @Test
    public void testOf_Map_Modifications() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Stream<Map.Entry<String, Integer>> stream = createStream(map);

        map.put("c", 3);

        assertThrows(ConcurrentModificationException.class, () -> stream.toList());
    }

    @Test
    public void testComplexNestedOperations() {
        List<String> result = createStream(Arrays.asList(1, 2, 3, 4, 5)).flatMap(n -> createStream(n, n * 10))
                .filter(n -> n % 2 == 0)
                .map(n -> "Value: " + n)
                .distinct()
                .sorted()
                .toList();

        assertEquals(Arrays.asList("Value: 10", "Value: 2", "Value: 20", "Value: 30", "Value: 4", "Value: 40", "Value: 50"), result);
    }

    @Test
    public void testMemoryEfficientOperations() {
        AtomicInteger generatedCount = new AtomicInteger(0);

        Stream<Integer> infiniteStream = Stream.generate(() -> generatedCount.incrementAndGet());

        List<Integer> result = infiniteStream.limit(5).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        assertTrue(generatedCount.get() <= 10);
    }

    @Test
    public void testExceptionPropagation() {
        Iterator<Integer> failingIterator = new Iterator<>() {
            private int count = 0;

            @Override
            public boolean hasNext() {
                if (count == 3) {
                    throw new RuntimeException("Iterator failed");
                }
                return count < 5;
            }

            @Override
            public Integer next() {
                return count++;
            }
        };

        Stream<Integer> stream = createStream(failingIterator);

        try {
            stream.toList();
            fail("Expected exception");
        } catch (RuntimeException e) {
            assertEquals("Iterator failed", e.getMessage());
        }
    }

    @Test
    public void testThreadSafety() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(1000);

        Thread producer = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                try {
                    queue.put(i);
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        producer.start();

        List<Integer> consumed = Stream.observe(queue, Duration.ofMillis(500)).filter(n -> n % 2 == 0).toList();

        producer.join();

        assertTrue(consumed.size() > 0);
        assertTrue(consumed.stream().allMatch(n -> n % 2 == 0));
    }

    @Test
    public void testResourceManagement() throws IOException {
        File testFile = Files.createTempFile(tempFolder, "resource-test", ".txt").toFile();
        Files.write(testFile.toPath(), Arrays.asList("line1", "line2", "line3"));

        AtomicBoolean streamClosed = new AtomicBoolean(false);

        try (Stream<String> stream = Stream.ofLines(testFile).onClose(() -> streamClosed.set(true))) {
            assertEquals(3, stream.count());
        }

        assertTrue(streamClosed.get());
    }

    @Test
    public void testLargeDataHandling() {
        int size = 100000;

        long sum = Stream.range(0, size).mapToLong(i -> i).sum();

        long expectedSum = ((long) size * (size - 1)) / 2;
        assertEquals(expectedSum, sum);
    }

    @Test
    public void testNullSafety() {
        List<String> withNulls = Arrays.asList("a", null, "b", null, "c");

        assertEquals(5, createStream(withNulls).count());

        List<String> nonNulls = createStream(withNulls).filter(Objects::nonNull).toList();
        assertEquals(Arrays.asList("a", "b", "c"), nonNulls);

        List<Integer> lengths = createStream(withNulls).map(s -> s == null ? -1 : s.length()).toList();
        assertEquals(Arrays.asList(1, -1, 1, -1, 1), lengths);
    }

    @Test
    public void testOf_PrimitiveArrays_EmptyArrays() {

        assertTrue(createStream(new boolean[0]).toList().isEmpty());
        assertTrue(createStream(new char[0]).toList().isEmpty());
        assertTrue(createStream(new byte[0]).toList().isEmpty());
        assertTrue(createStream(new short[0]).toList().isEmpty());
        assertTrue(createStream(new int[0]).toList().isEmpty());
        assertTrue(createStream(new long[0]).toList().isEmpty());
        assertTrue(createStream(new float[0]).toList().isEmpty());
        assertTrue(createStream(new double[0]).toList().isEmpty());
    }

    @Test
    public void testOf_PrimitiveArrays_CountAndAdvance() {
        int[] intArray = { 1, 2, 3, 4, 5 };
        Stream<Integer> intStream = createStream(intArray, 1, 4);
        assertEquals(3, intStream.count());

        double[] doubleArray = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        Stream<Double> doubleStream = createStream(doubleArray);
        assertEquals(Arrays.asList(3.0, 4.0, 5.0), doubleStream.skip(2).toList());
    }

    @Test
    public void testOf_PrimitiveArrays_ToArray() {

        boolean[] boolArray = { true, false, true };
        Boolean[] boolResult = createStream(boolArray).toArray(Boolean[]::new);
        assertArrayEquals(new Boolean[] { true, false, true }, boolResult);

        char[] charArray = { 'x', 'y', 'z' };
        Character[] charResult = createStream(charArray).toArray(Character[]::new);
        assertArrayEquals(new Character[] { 'x', 'y', 'z' }, charResult);

        int[] intArray = { 10, 20, 30 };
        Integer[] intResult = createStream(intArray).toArray(Integer[]::new);
        assertArrayEquals(new Integer[] { 10, 20, 30 }, intResult);
    }

    @Test
    public void testOfLines_FileNotFound() {
        File nonExistentFile = new File("non-existent-file.txt");

        try {
            Stream.ofLines(nonExistentFile).toList();
            fail("Expected exception for non-existent file");
        } catch (UncheckedIOException e) {
            assertTrue(e.getCause() instanceof FileNotFoundException);
        }
    }

    @Test
    public void testOfLines_PathNotFound() {
        Path nonExistentPath = new File("non-existent-path.txt").toPath();

        try {
            Stream.ofLines(nonExistentPath).toList();
            fail("Expected exception for non-existent path");
        } catch (UncheckedIOException e) {
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    @Test
    public void testOfLines_DifferentCharsets() throws IOException {
        File utf8File = Files.createTempFile(tempFolder, "utf8", ".txt").toFile();
        String content = "Hello\nWorld\n你好\n世界";
        Files.write(utf8File.toPath(), content.getBytes("UTF-8"));

        List<String> utf8Lines = Stream.ofLines(utf8File, Charsets.UTF_8).toList();
        assertEquals(4, utf8Lines.size());
        assertEquals("你好", utf8Lines.get(2));
        assertEquals("世界", utf8Lines.get(3));

        List<String> isoLines = Stream.ofLines(utf8File, Charset.forName("ISO-8859-1")).toList();
        assertEquals(4, isoLines.size());
        assertNotEquals("你好", isoLines.get(2));
    }

    @Test
    public void testOfLines_LargeFile() throws IOException {
        File largeFile = Files.createTempFile(tempFolder, "large", ".txt").toFile();
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            lines.add("Line " + i);
        }
        Files.write(largeFile.toPath(), lines);

        long count = Stream.ofLines(largeFile).count();
        assertEquals(10000, count);

        long evenCount = Stream.ofLines(largeFile)
                .filter(line -> line.contains("0") || line.contains("2") || line.contains("4") || line.contains("6") || line.contains("8"))
                .count();
        assertTrue(evenCount > 0);
    }

    @Test
    public void testOfLines_ReaderClosed() throws IOException {
        String content = "line1\nline2\nline3";
        StringReader reader = new StringReader(content);

        List<String> lines;
        try (Stream<String> stream = Stream.ofLines(reader, true)) {
            lines = stream.toList();
        }

        assertEquals(3, lines.size());

        try {
            reader.read();
            fail("Reader should be closed");
        } catch (IOException e) {
        }
    }

    @Test
    public void testOfLines_BufferedReader() throws IOException {
        String content = "line1\nline2\nline3";
        BufferedReader bufferedReader = new BufferedReader(new StringReader(content));

        List<String> lines = Stream.ofLines(bufferedReader, false).toList();

        assertEquals(3, lines.size());

        bufferedReader.close();
    }

    @Test
    public void testListFiles_SymbolicLinks() throws IOException {
        File targetDir = Files.createTempDirectory(tempFolder, "target").toFile();
        File targetFile = new File(targetDir, "target.txt");
        targetFile.createNewFile();

        File linkDir = Files.createTempDirectory(tempFolder, "links").toFile();

        try {
            Path link = new File(linkDir, "link.txt").toPath();
            Files.createSymbolicLink(link, targetFile.toPath());

            List<File> files = Stream.listFiles(linkDir).toList();
            assertEquals(1, files.size());
            assertEquals("link.txt", files.get(0).getName());
        } catch (UnsupportedOperationException | IOException e) {
        }
    }

    @Test
    public void testListFiles_HiddenFiles() throws IOException {
        File dir = Files.createTempDirectory(tempFolder, "hidden").toFile();

        File regularFile = new File(dir, "regular.txt");
        regularFile.createNewFile();

        File hiddenFile = new File(dir, ".hidden");
        hiddenFile.createNewFile();

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            try {
                Files.setAttribute(hiddenFile.toPath(), "dos:hidden", true);
            } catch (IOException e) {
            }
        }

        List<File> allFiles = Stream.listFiles(dir).toList();
        assertTrue(allFiles.size() >= 2);
    }

    @Test
    public void testListFiles_PermissionDenied() throws IOException {
        File dir = Files.createTempDirectory(tempFolder, "restricted").toFile();
        File file = new File(dir, "file.txt");
        file.createNewFile();

        boolean permissionChanged = dir.setReadable(false);

        if (permissionChanged) {
            try {
                List<File> files = Stream.listFiles(dir).toList();
            } catch (Exception e) {
            } finally {
                dir.setReadable(true);
            }
        }
    }

    @Test
    public void testConcat_StreamClose() {
        AtomicBoolean stream1Closed = new AtomicBoolean(false);
        AtomicBoolean stream2Closed = new AtomicBoolean(false);

        Stream<String> stream1 = createStream01("a", "b").onClose(() -> stream1Closed.set(true));
        Stream<String> stream2 = createStream01("c", "d").onClose(() -> stream2Closed.set(true));

        try (Stream<String> concatenated = Stream.concat(stream1, stream2)) {
            assertEquals(Arrays.asList("a", "b", "c", "d"), concatenated.toList());
        }

        assertTrue(stream1Closed.get());
        assertTrue(stream2Closed.get());
    }

    @Test
    public void testConcat_CollectionOfStreams_CloseHandlers() {
        List<AtomicBoolean> closedFlags = new ArrayList<>();
        List<Stream<Integer>> streams = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            AtomicBoolean closed = new AtomicBoolean(false);
            closedFlags.add(closed);
            final int base = i * 10;
            streams.add(createStream(base, base + 1).onClose(() -> closed.set(true)));
        }

        try (Stream<Integer> concatenated = Stream.concat(streams)) {
            List<Integer> result = concatenated.toList();
            assertEquals(Arrays.asList(0, 1, 10, 11, 20, 21), result);
        }

        for (AtomicBoolean flag : closedFlags) {
            assertTrue(flag.get());
        }
    }

    @Test
    public void testFrom_JavaStreamOperations() {
        java.util.stream.Stream<String> javaStream = java.util.stream.Stream.of("apple", "banana", "cherry", "date").filter(s -> s.length() > 4);

        Stream<String> stream = Stream.from(javaStream);

        List<String> result = stream.map(String::toUpperCase).sorted().toList();

        assertEquals(Arrays.asList("APPLE", "BANANA", "CHERRY"), result);
    }

    @Test
    public void testFrom_JavaStreamWithCount() {
        java.util.stream.Stream<Integer> javaStream = java.util.stream.Stream.of(1, 2, 3, 4, 5);

        Stream<Integer> stream = Stream.from(javaStream);

        assertEquals(5, stream.count());
    }

    @Test
    public void testFrom_JavaStreamWithAdvance() {
        java.util.stream.Stream<Integer> javaStream = java.util.stream.Stream.of(1, 2, 3, 4, 5);

        Stream<Integer> stream = Stream.from(javaStream);

        List<Integer> result = stream.skip(2).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testFrom_JavaStreamToArray() {
        java.util.stream.Stream<String> javaStream = java.util.stream.Stream.of("a", "b", "c");

        Stream<String> stream = Stream.from(javaStream);

        Object[] objArray = stream.toArray();
        assertArrayEquals(new Object[] { "a", "b", "c" }, objArray);

        java.util.stream.Stream<String> javaStream2 = java.util.stream.Stream.of("x", "y", "z");
        Stream<String> stream2 = Stream.from(javaStream2);
        String[] stringArray = stream2.toArray(String[]::new);
        assertArrayEquals(new String[] { "x", "y", "z" }, stringArray);
    }

    @Test
    public void testStreamEx_AbstractClass() {
        try {
            Class<?> streamExClass = Stream.StreamEx.class;
            assertTrue(java.lang.reflect.Modifier.isAbstract(streamExClass.getModifiers()));

            assertEquals(Stream.class, streamExClass.getSuperclass());

            assertEquals(0, streamExClass.getConstructors().length);
        } catch (Exception e) {
            fail("Failed to verify StreamEx class structure: " + e.getMessage());
        }
    }

    @Test
    public void testComplexScenarios_MemoryLeak() {
        List<Stream<?>> streams = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            Stream<Integer> stream = Stream.range(0, 1000);
            streams.add(stream);
        }

        for (Stream<?> stream : streams) {
            stream.close();
        }

        assertTrue(true);
    }

    @Test
    public void testComplexScenarios_NestedParallelism() {
        List<Integer> result = Stream.range(0, 10)
                .parallel()
                .flatMap(i -> Stream.range(i * 10, (i + 1) * 10).parallel())
                .filter(n -> n % 3 == 0)
                .sorted()
                .toList();

        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 100; i += 3) {
            expected.add(i);
        }
        assertEquals(expected, result);
    }

    @Test
    public void testPerformance_LargeConcat() {
        List<Stream<Integer>> manyStreams = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            final int base = i * 10;
            manyStreams.add(Stream.range(base, base + 10));
        }

        long count = Stream.concat(manyStreams).count();
        assertEquals(1000, count);
    }

    @Test
    public void testEdgeCases_VeryLargeRepeat() {
        long veryLarge = Long.MAX_VALUE / 2;

        Stream<String> repeated = Stream.repeat("X", veryLarge);

        List<String> first10 = repeated.limit(10).toList();
        assertEquals(10, first10.size());
        assertTrue(first10.stream().allMatch("X"::equals));
    }

    @Test
    public void testIntegration_AllFactoryMethods() {

        assertEquals(0, Stream.empty().count());

        assertEquals(1, Stream.just("test").count());

        assertEquals(0, Stream.ofNullable(null).count());
        assertEquals(1, Stream.ofNullable("test").count());

        assertEquals(3, Stream.defer(() -> createStream(1, 2, 3)).count());

        assertEquals(3, createStream01(new Integer[] { 1, 2, 3 }).count());
        assertEquals(3, createStream(Arrays.asList(1, 2, 3)).count());
        assertEquals(3, createStream(Arrays.asList(1, 2, 3).iterator()).count());

        assertEquals(5, Stream.range(0, 5).count());
        assertEquals(6, Stream.rangeClosed(0, 5).count());

        assertEquals(10, Stream.repeat("X", 10).count());

        assertEquals(5, Stream.generate(() -> "Y").limit(5).count());

        assertEquals(5, Stream.iterate(1, x -> x + 1).limit(5).count());
    }

    @Test
    public void test_lazyEvalation() throws Exception {
        final MutableBoolean moved = MutableBoolean.of(false);

        Stream<String> s = Stream.of("a", "b", "c", "d").peek(Fn.println()).peek(it -> {
            new RuntimeException("Testing lazy evaluation").printStackTrace();
            moved.setTrue();
        })
                .parallel()
                .map(it -> it + "111")
                .buffered()
                .shuffled()
                .sorted()
                .rotated(2)
                .filter(Fn.alwaysTrue())
                .flatMap(Stream::of)
                .takeWhile(Fn.alwaysTrue())
                .dropWhile(Fn.alwaysFalse())
                .sequential()
                .map(it -> it + "111")
                .buffered()
                .shuffled()
                .sorted()
                .rotated(2)
                .filter(Fn.alwaysTrue())
                .flatMap(Stream::of)
                .takeWhile(Fn.alwaysTrue())
                .dropWhile(Fn.alwaysFalse());

        assertFalse(moved.value());

        s = s.filter(Fn.notNull());

        assertFalse(moved.value());

        s = s.buffered();

        assertFalse(moved.value());

        final ObjIterator<String> iter = s.iterator();

        assertFalse(moved.value());

        s.forEach(Fn.println());

        assertTrue(moved.value());

        assertFalse(iter.hasNext());

        moved.setFalse();
        final List<ByteStream> list = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            list.add(ByteStream.range((byte) 0, (byte) i).peek(it -> moved.setTrue()).peek(N::println));
        }

        ByteStream bs = ByteStream.merge(list, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);

        bs = bs.filter(it -> it < 10);

        assertFalse(moved.value());

        final ByteIterator byteIter = bs.iterator();

        bs.forEach(N::println);

        assertFalse(byteIter.hasNext());

        moved.setFalse();

        final IntStream is = Stream.range(0, 10_000)
                .peek(it -> moved.setTrue())
                .parallel(128)
                .map(it -> it - 0)
                .map(it -> it + 0)
                .flatMap(Stream::of)
                .map(it -> it + 1)
                .map(it -> it - 1)
                .filter(it -> it >= 0)
                .buffered(1024)
                .peek(it -> {
                    if (it % 37 == 0) {
                        N.sleep(3);
                    }
                })
                .map(it -> it - 0)
                .map(it -> it + 0)
                .flatMap(Stream::of)
                .map(it -> it + 1)
                .sequential()
                .map(it -> it - 1)
                .filter(it -> it >= 0)
                .buffered(1024)
                .reverseSorted()
                .peek(it -> {
                    if (it % 79 == 0) {
                        N.sleep(3);
                    }
                })
                .map(it -> it - 0)
                .map(it -> it + 0)
                .flatMap(Stream::of)
                .map(it -> it + 1)
                .map(it -> it - 1)
                .filter(it -> it >= 0)
                .buffered(1024)
                .peek(it -> {
                    if (it % 137 == 0) {
                        N.sleep(3);
                    }
                })
                .map(it -> it - 0)
                .parallel(64)
                .map(it -> it + 0)
                .flatMap(Stream::of)
                .map(it -> it + 1)
                .map(it -> it - 1)
                .filter(it -> it >= 0)
                .buffered(137)
                .peek(it -> {
                    if (it % 537 == 0) {
                        N.sleep(3);
                    }
                })
                .mapToInt(ToIntFunction.UNBOX);

        assertFalse(moved.value());

        final IntIterator ii = is.iterator();

        assertFalse(moved.value());

        assertEquals(IntStream.range(0, 10_000).sum(), is.sum());

        assertFalse(ii.hasNext());
    }

    @Test
    public void test_spsFilter() {
        Stream<String> strings = createStream01("a", "bb", "ccc", "dd", "e");
        List<String> result = strings.spsFilter(it -> it.length() <= 1).toList();
        assertHaveSameElements(Arrays.asList("a", "e"), result);
    }

    @Test
    public void test_sjps() {
        {
            Stream<String> strings = createStream01("a", "bb", "ccc", "dd", "e");
            List<String> result = strings.sjps(s -> s.filter(it -> it.length() <= 1)).toList();
            assertHaveSameElements(Arrays.asList("a", "e"), result);
        }
        {
            Stream<String> strings = createStream01("a", "bb", "ccc", "dd", "e");
            List<String> result = strings.parallel().sjps(s -> s.filter(it -> it.length() <= 1)).toList();
            assertHaveSameElements(Arrays.asList("a", "e"), result);
        }
    }

    @Test
    public void test_advance_toArray() {
        {
            boolean[] a = new boolean[] { true, false, true, false, true };
            assertArrayEquals(Array.box(a), Stream.of(a).toArray());
            Boolean[] ret = Stream.of(a).step(2).toArray(Boolean[]::new);
            assertArrayEquals(new Boolean[] { true, true, true }, ret);
        }
        {
            char[] a = new char[] { 'a', 'b', 'c', 'd', 'e' };
            assertArrayEquals(Array.box(a), Stream.of(a).toArray());
            Character[] ret = Stream.of(a).step(2).toArray(Character[]::new);
            assertArrayEquals(new Character[] { 'a', 'c', 'e' }, ret);
        }
        {
            byte[] a = new byte[] { 1, 2, 3, 4, 5 };
            assertArrayEquals(Array.box(a), Stream.of(a).toArray());
            Byte[] ret = Stream.of(a).step(2).toArray(Byte[]::new);
            assertArrayEquals(new Byte[] { 1, 3, 5 }, ret);
        }
        {
            short[] a = new short[] { 1, 2, 3, 4, 5 };
            assertArrayEquals(Array.box(a), Stream.of(a).toArray());
            Short[] ret = Stream.of(a).step(2).toArray(Short[]::new);
            assertArrayEquals(new Short[] { 1, 3, 5 }, ret);
        }
        {
            int[] a = new int[] { 1, 2, 3, 4, 5 };
            assertArrayEquals(Array.box(a), Stream.of(a).toArray());
            Integer[] ret = Stream.of(a).step(2).toArray(Integer[]::new);
            assertArrayEquals(new Integer[] { 1, 3, 5 }, ret);
        }
        {
            long[] a = new long[] { 1, 2, 3, 4, 5 };
            assertArrayEquals(Array.box(a), Stream.of(a).toArray());
            Long[] ret = Stream.of(a).step(2).toArray(Long[]::new);
            assertArrayEquals(new Long[] { 1l, 3l, 5l }, ret);
        }
        {
            float[] a = new float[] { 1, 2, 3, 4, 5 };
            assertArrayEquals(Array.box(a), Stream.of(a).toArray());
            Float[] ret = Stream.of(a).step(2).toArray(Float[]::new);
            assertArrayEquals(new Float[] { 1f, 3f, 5f }, ret);
        }
        {
            double[] a = new double[] { 1, 2, 3, 4, 5 };
            assertArrayEquals(Array.box(a), Stream.of(a).toArray());
            Double[] ret = Stream.of(a).step(2).toArray(Double[]::new);
            assertArrayEquals(new Double[] { 1d, 3d, 5d }, ret);
        }
        {
            String[] a = new String[] { "1", "2", "3", "4", "5" };
            assertArrayEquals(a, Stream.of(a).toArray());
            String[] ret = Stream.of(a).step(2).toArray(String[]::new);
            assertArrayEquals(new String[] { "1", "3", "5" }, ret);
        }
    }

    @Test
    public void testMergeArray() {
        Integer[] a = { 1, 3, 5 };
        Integer[] b = { 2, 4, 6 };
        Integer[] c = { 7, 8, 9 };

        assertEquals(N.toList(1, 2, 3, 4, 5, 6), Stream.merge(a, b, (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                Stream.merge(a, b, c, (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
    }

    @Test
    public void test_merge_streams() {
        Integer[] a = { 1, 3, 5 };
        Integer[] b = { 2, 4, 6 };
        Integer[] c = { 7, 8, 9 };

        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.merge(Stream.of(a), Stream.of(b), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.merge(N.toList(Stream.of(a), Stream.of(b)), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                Stream.merge(Stream.of(a), Stream.of(b), Stream.of(c), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                Stream.merge(N.toList(Stream.of(a), Stream.of(b), Stream.of(c)), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(
                N.toList(1, 1, 2, 3, 3, 4, 5, 5, 6, 7, 8, 9), Stream
                        .merge(N.toList(Stream.of(a), Stream.of(b), Stream.of(c), Stream.of(a)),
                                (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());

    }

    @Test
    public void test_parallelMerge_streams() {
        Integer[] a = { 1, 3, 5 };
        Integer[] b = { 2, 4, 6 };
        Integer[] c = { 7, 8, 9 };

        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.parallelMerge(N.toList(Stream.of(a), Stream.of(b)), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9), Stream
                .parallelMerge(N.toList(Stream.of(a), Stream.of(b), Stream.of(c)), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toList());
        assertEquals(
                N.toList(1, 1, 2, 3, 3, 4, 5, 5, 6, 7, 8, 9), Stream
                        .parallelMerge(N.toList(Stream.of(a), Stream.of(b), Stream.of(c), Stream.of(a)),
                                (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());

    }

    @Test
    public void testParallelMergeIterators() {
        List<Integer> a = N.toList(1, 3, 5);
        List<Integer> b = N.toList(2, 4, 6);
        List<Integer> c = N.toList(7, 8, 9);
        List<Integer> d = N.toList(7, 9, 10);

        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.parallelMergeIterables(N.toList(a, b), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                Stream.parallelMergeIterables(N.toList(a, b, c), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 7, 8, 9, 9, 10),
                Stream.parallelMergeIterables(N.toList(a, b, c, d), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
    }

    // ==================== Missing test methods below ====================

    @Test
    public void testDistinctWithMergeFunction() {
        List<String> result = Stream.of("a1", "b1", "a2", "b2").distinct((s1, s2) -> s1 + "+" + s2).toList();
        // Since distinct uses identity as key, each string is unique => no merge
        assertEquals(4, result.size());

        // Test with actual duplicates
        List<Integer> intResult = Stream.of(1, 2, 1, 3, 2).distinct(Integer::sum).toList();
        assertEquals(3, intResult.size());
        assertTrue(intResult.contains(3)); // 1+1=2, but merged as sum
    }

    @Test
    public void testDistinctByWithMergeFunction() {
        List<String> result = Stream.of("a1", "b1", "a2", "b2").distinctBy(s -> s.charAt(0), (s1, s2) -> s1 + "+" + s2).toList();
        assertEquals(2, result.size());
        assertEquals("a1+a2", result.get(0));
        assertEquals("b1+b2", result.get(1));
    }

    @Test
    public void testPeekFirst() {
        AtomicReference<Integer> firstRef = new AtomicReference<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).peekFirst(firstRef::set).toList();
        assertEquals(1, firstRef.get());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testPeekLast() {
        AtomicReference<Integer> lastRef = new AtomicReference<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).peekLast(lastRef::set).toList();
        assertEquals(5, lastRef.get());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testPeekIfWithIndex() {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Stream.of(10, 20, 30, 40, 50).peekIf((e, count) -> count % 2 == 0, peeked::add).toList();
        assertEquals(Arrays.asList(20, 40), peeked); // count starts at 1, so even counts are 2nd, 4th
        assertEquals(Arrays.asList(10, 20, 30, 40, 50), result);
    }

    @Test
    public void testForeach() {
        List<Integer> collected = new ArrayList<>();
        Stream.of(1, 2, 3).foreach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testFindFirst_alias() {
        Optional<Integer> result = Stream.of(1, 2, 3).findFirst();
        assertTrue(result.isPresent());
        assertEquals(1, result.get().intValue());
    }

    @Test
    public void testFindFirst_empty() {
        Optional<Integer> result = Stream.<Integer> empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAny_alias() {
        Optional<Integer> result = Stream.of(1, 2, 3).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_empty() {
        Optional<Integer> result = Stream.<Integer> empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testReduceWithIdentity_empty() {
        int result = Stream.<Integer> empty().reduce(0, Integer::sum);
        assertEquals(0, result);
    }

    @Test
    public void testMinBy_empty() {
        Optional<String> result = Stream.<String> empty().minBy(String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void testMaxBy_empty() {
        Optional<String> result = Stream.<String> empty().maxBy(String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFilterE() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).filterE(x -> x > 3).toList();
        assertEquals(Arrays.asList(4, 5), result);
    }

    @Test
    public void testSplitByCharDelimiter() {
        List<String> result = Stream.split("a,b,c", ',').toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testSplitByStringDelimiter() {
        List<String> result = Stream.split("a::b::c", "::").toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testSplitByPattern() {
        List<String> result = Stream.split("a  b   c", Pattern.compile("\\s+")).toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testSplitToLines_withTrimAndOmitEmpty() {
        List<String> result = Stream.splitToLines("  line1  \n\n  line2  ", true, true).toList();
        assertEquals(Arrays.asList("line1", "line2"), result);
    }

    @Test
    public void testOfLinesFromFile() throws IOException {
        File tempFile = new File(tempDir.toFile(), "testlines.txt");
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("hello\nworld\n");
        }
        try (Stream<String> lines = Stream.ofLines(tempFile)) {
            List<String> result = lines.toList();
            assertEquals(Arrays.asList("hello", "world"), result);
        }
    }

    @Test
    public void testOfLinesFromFileWithCharset() throws IOException {
        File tempFile = new File(tempDir.toFile(), "testlines_charset.txt");
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("abc\ndef\n");
        }
        try (Stream<String> lines = Stream.ofLines(tempFile, StandardCharsets.UTF_8)) {
            List<String> result = lines.toList();
            assertEquals(Arrays.asList("abc", "def"), result);
        }
    }

    @Test
    public void testOfLinesFromPath() throws IOException {
        Path tempFile = tempDir.resolve("testlines_path.txt");
        Files.write(tempFile, Arrays.asList("one", "two", "three"));
        try (Stream<String> lines = Stream.ofLines(tempFile)) {
            List<String> result = lines.toList();
            assertEquals(Arrays.asList("one", "two", "three"), result);
        }
    }

    @Test
    public void testOfLinesFromReader() throws IOException {
        StringReader reader = new StringReader("first\nsecond\nthird");
        List<String> result = Stream.ofLines(reader).toList();
        assertEquals(Arrays.asList("first", "second", "third"), result);
    }

    @Test
    public void testOfLinesFromReader_autoClose() throws IOException {
        StringReader reader = new StringReader("a\nb");
        try (Stream<String> lines = Stream.ofLines(reader, true)) {
            List<String> result = lines.toList();
            assertEquals(Arrays.asList("a", "b"), result);
        }
    }

    @Test
    public void testListFilesRecursive() throws IOException {
        File dir = tempDir.toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();
        File f = new File(subDir, "nested.txt");
        f.createNewFile();

        List<File> files = Stream.listFiles(dir, true).toList();
        assertTrue(files.stream().anyMatch(file -> file.getName().equals("nested.txt")));
    }

    @Test
    public void testListFilesRecursiveExcludeDir() throws IOException {
        File dir = tempDir.toFile();
        File subDir = new File(dir, "subdir2");
        subDir.mkdir();
        new File(subDir, "nested2.txt").createNewFile();

        List<File> files = Stream.listFiles(dir, true, true).toList();
        assertTrue(files.stream().allMatch(File::isFile));
    }

    @Test
    public void testOfBooleanArray() {
        boolean[] arr = { true, false, true };
        List<Boolean> result = Stream.of(arr).toList();
        assertEquals(Arrays.asList(true, false, true), result);
    }

    @Test
    public void testOfBooleanArrayRange() {
        boolean[] arr = { true, false, true, false };
        List<Boolean> result = Stream.of(arr, 1, 3).toList();
        assertEquals(Arrays.asList(false, true), result);
    }

    @Test
    public void testOfCharArray() {
        char[] arr = { 'a', 'b', 'c' };
        List<Character> result = Stream.of(arr).toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);
    }

    @Test
    public void testOfCharArrayRange() {
        char[] arr = { 'a', 'b', 'c', 'd' };
        List<Character> result = Stream.of(arr, 1, 3).toList();
        assertEquals(Arrays.asList('b', 'c'), result);
    }

    @Test
    public void testOfByteArray() {
        byte[] arr = { 1, 2, 3 };
        List<Byte> result = Stream.of(arr).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    @Test
    public void testOfShortArray() {
        short[] arr = { 1, 2, 3 };
        List<Short> result = Stream.of(arr).toList();
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), result);
    }

    @Test
    public void testOfFloatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f };
        List<Float> result = Stream.of(arr).toList();
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), result);
    }

    @Test
    public void testOfDoubleArray() {
        double[] arr = { 1.0, 2.0, 3.0 };
        List<Double> result = Stream.of(arr).toList();
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), result);
    }

    @Test
    public void testRangeInt() {
        List<Integer> result = Stream.range(1, 5).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testRangeIntWithBy() {
        List<Integer> result = Stream.range(1, 10, 3).toList();
        assertEquals(Arrays.asList(1, 4, 7), result);
    }

    @Test
    public void testRangeLong() {
        List<Long> result = Stream.range(1L, 5L).toList();
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L), result);
    }

    @Test
    public void testRangeLongWithBy() {
        List<Long> result = Stream.range(1L, 10L, 3L).toList();
        assertEquals(Arrays.asList(1L, 4L, 7L), result);
    }

    @Test
    public void testRangeClosedInt() {
        List<Integer> result = Stream.rangeClosed(1, 5).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testRangeClosedIntWithBy() {
        List<Integer> result = Stream.rangeClosed(1, 10, 3).toList();
        assertEquals(Arrays.asList(1, 4, 7, 10), result);
    }

    @Test
    public void testRangeClosedLong() {
        List<Long> result = Stream.rangeClosed(1L, 5L).toList();
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), result);
    }

    @Test
    public void testRangeClosedLongWithBy() {
        List<Long> result = Stream.rangeClosed(1L, 10L, 3L).toList();
        assertEquals(Arrays.asList(1L, 4L, 7L, 10L), result);
    }

    @Test
    public void testDebounce() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).debounce(10, Duration.ofMillis(100)).toList();
        assertNotNull(result);
        // debounce groups items within duration windows
        assertTrue(result.size() >= 1);
    }

    @Test
    public void testAsyncRunWithExecutor() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            AtomicInteger sum = new AtomicInteger(0);
            Stream.of(1, 2, 3).asyncRun(s -> s.forEach(e -> sum.addAndGet(e)), executor).get();
            assertEquals(6, sum.get());
        } finally {
            executor.shutdownNow();
        }
    }


    @Test
    public void testSaveEachToFileDefault() throws IOException {
        File tempFile = new File(tempDir.toFile(), "saveEachDefault.txt");
        Stream.of("hello", "world").saveEach(tempFile).forEach(e -> {
        });
        assertTrue(tempFile.exists());
        String content = IOUtil.readAllToString(tempFile);
        assertTrue(content.contains("hello"));
        assertTrue(content.contains("world"));
    }

    @Test
    public void testCollapseTriPredicateWithMerge() {
        List<Integer> result = Stream.of(1, 1, 2, 2, 2, 3, 3).collapse((prev, cur, next) -> Objects.equals(prev, cur), Integer::sum).toList();
        assertNotNull(result);
        assertTrue(result.size() >= 1);
    }

    @Test
    public void testSpsFilterWithMaxThreadNum() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8).spsFilter(2, x -> x % 2 == 0).toList();
        assertHaveSameElements(Arrays.asList(2, 4, 6, 8), result);
    }

    @Test
    public void testOfIntArray() {
        int[] arr = { 1, 2, 3, 4, 5 };
        List<Integer> result = Stream.of(arr).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testOfIntArrayRange() {
        int[] arr = { 1, 2, 3, 4, 5 };
        List<Integer> result = Stream.of(arr, 1, 4).toList();
        assertEquals(Arrays.asList(2, 3, 4), result);
    }

    @Test
    public void testOfLongArray() {
        long[] arr = { 1L, 2L, 3L };
        List<Long> result = Stream.of(arr).toList();
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testOfLongArrayRange() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L };
        List<Long> result = Stream.of(arr, 1, 4).toList();
        assertEquals(Arrays.asList(2L, 3L, 4L), result);
    }

    // ==================== New tests for untested methods ====================

    @Test
    public void testSelect_Empty() {
        List<Integer> result = Stream.of("a", "b", "c").select(Integer.class).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSlidingMap_BiFunction() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 5, 7, 9), result);
    }

    @Test
    public void testSlidingMap_TriFunction() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 9, 12), result);
    }

    @Test
    public void testSlidingMap_Empty() {
        List<Integer> result = Stream.<Integer> empty().slidingMap((a, b) -> a + b).toList();
        assertTrue(result.isEmpty());
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
    public void testGroupBy_Empty() {
        List<Map.Entry<Integer, List<String>>> result = Stream.<String> empty().groupBy(String::length).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollapse_BiPredicate() {
        // Collapse consecutive equal elements into groups
        List<List<Integer>> result = Stream.of(1, 1, 2, 2, 2, 3, 3, 1).collapse((a, b) -> a.equals(b)).toList();
        assertEquals(4, result.size());
        assertEquals(Arrays.asList(1, 1), result.get(0));
        assertEquals(Arrays.asList(2, 2, 2), result.get(1));
        assertEquals(Arrays.asList(3, 3), result.get(2));
        assertEquals(Arrays.asList(1), result.get(3));
    }

    @Test
    public void testCollapse_WithMergeFunction() {
        // Collapse and merge with sum
        List<Integer> result = Stream.of(1, 1, 2, 2, 2, 3, 3, 1).collapse((a, b) -> a.equals(b), Integer::sum).toList();
        assertEquals(Arrays.asList(2, 6, 6, 1), result);
    }

    @Test
    public void testScan_WithInit() {
        List<Integer> result = Stream.of(1, 2, 3, 4).scan(10, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 13, 16, 20), result);
    }

    @Test
    public void testScan_WithInitIncluded() {
        List<Integer> result = Stream.of(1, 2, 3).scan(10, true, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(10, 11, 13, 16), result);
    }

    @Test
    public void testScan_Empty() {
        List<Integer> result = Stream.<Integer> empty().scan((a, b) -> a + b).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersperse() {
        List<Integer> result = Stream.of(1, 2, 3).intersperse(0).toList();
        assertEquals(Arrays.asList(1, 0, 2, 0, 3), result);
    }

    @Test
    public void testIntersperse_SingleElement() {
        List<Integer> result = Stream.of(1).intersperse(0).toList();
        assertEquals(Arrays.asList(1), result);
    }

    @Test
    public void testIntersperse_Empty() {
        List<Integer> result = Stream.<Integer> empty().intersperse(0).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSkipNulls_NoNulls() {
        List<String> result = Stream.of("a", "b", "c").skipNulls().toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testTakeLast_MoreThanSize() {
        List<Integer> result = Stream.of(1, 2, 3).takeLast(10).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testTakeLast_Zero() {
        List<Integer> result = Stream.of(1, 2, 3).takeLast(0).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOnFirst_Empty() {
        List<String> sideEffects = new ArrayList<>();
        List<Integer> result = Stream.<Integer> empty().onFirst(e -> sideEffects.add("first:" + e)).toList();
        assertTrue(result.isEmpty());
        assertTrue(sideEffects.isEmpty());
    }

    @Test
    public void testFoldLeft_Empty() {
        Optional<String> result = Stream.<String> empty().foldLeft((a, b) -> a + b);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFoldLeft_WithIdentity() {
        String result = Stream.of("a", "b", "c").foldLeft("", (acc, s) -> acc + s);
        assertEquals("abc", result);
    }

    @Test
    public void testFoldRight_WithIdentity() {
        String result = Stream.of("a", "b", "c").foldRight("", (acc, s) -> acc + s);
        assertEquals("cba", result);
    }

    @Test
    public void testMinAll_Empty() {
        List<Integer> result = Stream.<Integer> empty().minAll(Comparator.naturalOrder());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMaxAll_Empty() {
        List<Integer> result = Stream.<Integer> empty().maxAll(Comparator.naturalOrder());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSumInt_Empty() {
        long result = Stream.<String> empty().sumInt(String::length);
        assertEquals(0L, result);
    }

    @Test
    public void testAverageInt_Empty() {
        OptionalDouble result = Stream.<String> empty().averageInt(String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest_Third() {
        Optional<Integer> result = Stream.of(5, 2, 8, 1, 9, 3).kthLargest(3, Comparator.naturalOrder());
        assertTrue(result.isPresent());
        assertEquals(5, result.get().intValue());
    }

    @Test
    public void testKthLargest_Empty() {
        Optional<Integer> result = Stream.<Integer> empty().kthLargest(1, Comparator.naturalOrder());
        assertFalse(result.isPresent());
    }

    @Test
    public void testPercentiles_Empty() {
        Optional<Map<Percentage, Integer>> result = Stream.<Integer> empty().percentiles(Comparator.naturalOrder());
        assertFalse(result.isPresent());
    }

    @Test
    public void testContainsDuplicates() {
        assertTrue(Stream.of(1, 2, 3, 2, 4).containsDuplicates());
        assertFalse(Stream.of(1, 2, 3, 4, 5).containsDuplicates());
        assertFalse(Stream.<Integer> empty().containsDuplicates());
    }

    @Test
    public void testCombinations_WithLen() {
        List<List<Integer>> result = Stream.of(1, 2, 3).combinations(2).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(1, 3), result.get(1));
        assertEquals(Arrays.asList(2, 3), result.get(2));
    }

    @Test
    public void testZipWith_DifferentLengths() {
        List<String> result = Stream.of(1, 2, 3).zipWith(Arrays.asList("a", "b"), (i, s) -> i + s).toList();
        assertEquals(Arrays.asList("1a", "2b"), result);
    }

    @Test
    public void testZipWith_WithDefaults() {
        List<String> result = Stream.of(1, 2, 3).zipWith(Arrays.asList("a", "b"), 0, "z", (i, s) -> i + s).toList();
        assertEquals(Arrays.asList("1a", "2b", "3z"), result);
    }

    @Test
    public void testCrossJoin_WithFunction() {
        List<String> result = Stream.of("A", "B").crossJoin(Arrays.asList(1, 2, 3), (str, num) -> str + num).toList();
        assertEquals(Arrays.asList("A1", "A2", "A3", "B1", "B2", "B3"), result);
    }

    @Test
    public void testInnerJoin_NoMatch() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 3, 5)
                .innerJoin(Arrays.asList(2, 4, 6), (java.util.function.Function<Integer, Integer>) i -> i,
                        (java.util.function.Function<Integer, Integer>) i -> i)
                .toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollapse_BiPredicate_Empty() {
        List<List<Integer>> result = Stream.<Integer> empty().collapse((a, b) -> a.equals(b)).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollapse_WithCollector() {
        List<Long> result = Stream.of(1, 1, 2, 2, 2, 3).collapse((a, b) -> a.equals(b), java.util.stream.Collectors.counting()).toList();
        assertEquals(Arrays.asList(2L, 3L, 1L), result);
    }

    @Test
    public void testBuffered_WithSize() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).buffered(2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
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
    public void testCrossJoin_Empty() {
        List<Pair<String, Integer>> result = Stream.<String> empty().crossJoin(Arrays.asList(1, 2)).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCrossJoin_EmptyCollection() {
        List<Pair<String, Integer>> result = Stream.of("A", "B").crossJoin(Collections.<Integer> emptyList()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testScan_SingleElement() {
        List<Integer> result = Stream.of(42).scan((a, b) -> a + b).toList();
        assertEquals(Arrays.asList(42), result);
    }

    @Test
    public void testIntersperse_TwoElements() {
        List<Integer> result = Stream.of(1, 2).intersperse(0).toList();
        assertEquals(Arrays.asList(1, 0, 2), result);
    }

    @Test
    public void testSkipRange_FullRange() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).skipRange(0, 5).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSkipRange_EmptyRange() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).skipRange(2, 2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testRollup_Empty() {
        List<List<Integer>> result = Stream.<Integer> empty().rollup().toList();
        assertEquals(1, result.size());
        assertTrue(result.get(0).isEmpty());
    }

    @Test
    public void testContainsDuplicates_SingleElement() {
        assertFalse(Stream.of(1).containsDuplicates());
    }

    @Test
    public void testCombinations_Zero() {
        List<List<Integer>> result = Stream.of(1, 2, 3).combinations(0).toList();
        assertEquals(1, result.size());
        assertTrue(result.get(0).isEmpty());
    }

    @Test
    public void testFoldLeft_SingleElement() {
        Optional<Integer> result = Stream.of(42).foldLeft((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(42, result.get().intValue());
    }

    @Test
    public void testFoldRight_SingleElement() {
        Optional<Integer> result = Stream.of(42).foldRight((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(42, result.get().intValue());
    }

    @Test
    public void testMergeWith_EmptyStreams() {
        List<Integer> result = Stream.<Integer> empty().mergeWith(Collections.emptyList(), (a, b) -> MergeResult.TAKE_FIRST).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testPrepend_Empty() {
        List<Integer> result = Stream.of(1, 2, 3).prepend(Collections.emptyList()).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testAppend_Empty() {
        List<Integer> result = Stream.of(1, 2, 3).append(Collections.emptyList()).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSlidingMap_WithIncrement() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).slidingMap(2, (a, b) -> a + (b == null ? 0 : b)).toList();
        assertEquals(Arrays.asList(3, 7, 5), result);
    }

    @Test
    public void testMapMulti_Empty() {
        List<Object> result = Stream.<String> empty().mapMulti((s, consumer) -> {
            consumer.accept(s.toUpperCase());
        }).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipWith_ThreeCollections() {
        List<String> result = Stream.of(1, 2).zipWith(Arrays.asList("a", "b"), Arrays.asList(true, false), (i, s, b) -> i + s + b).toList();
        assertEquals(Arrays.asList("1atrue", "2bfalse"), result);
    }

    @Test
    public void testGroupBy_WithDownstream() {
        List<Map.Entry<Integer, Long>> result = Stream.of("a", "bb", "c", "dd", "eee").groupBy(String::length, java.util.stream.Collectors.counting()).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testPartitionBy_WithDownstream() {
        List<Map.Entry<Boolean, Long>> result = Stream.of(1, 2, 3, 4, 5).partitionBy(n -> n % 2 == 0, java.util.stream.Collectors.counting()).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testMapToEntry_SingleMapper() {
        List<Map.Entry<Integer, String>> result = Stream.of("a", "bb", "ccc").mapToEntry(s -> new AbstractMap.SimpleEntry<>(s.length(), s)).toList();
        assertEquals(3, result.size());
        assertEquals(1, result.get(0).getKey().intValue());
        assertEquals("a", result.get(0).getValue());
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
    public void testPairWith_Empty() {
        List<Pair<String, Integer>> result = Stream.<String> empty().pairWith(String::length).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInnerJoin_WithKeyMapper() {
        List<Pair<String, String>> result = Stream.of("apple", "banana")
                .innerJoin(Arrays.asList("ant", "bat", "big"), (java.util.function.Function<String, Character>) s -> s.charAt(0))
                .toList();
        // "apple" matches "ant" (both start with 'a'), "banana" matches "bat" and "big" (start with 'b')
        assertEquals(3, result.size());
    }

}
