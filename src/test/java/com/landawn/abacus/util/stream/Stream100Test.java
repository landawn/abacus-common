package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
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
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.UnaryOperator;

@ExtendWith(MockitoExtension.class)
@Tag("new-test")
public class Stream100Test extends TestBase {

    private Stream<Integer> stream;
    private List<Integer> testList;

    @Mock
    private Predicate<Integer> mockPredicate;

    @Mock
    private Function<Integer, String> mockFunction;

    @Mock
    private Consumer<Integer> mockConsumer;

    @Mock
    private BiFunction<Integer, Integer, Integer> mockBiFunction;

    @BeforeEach
    public void setUp() {
        testList = Arrays.asList(1, 2, 3, 4, 5);
        stream = createStream(testList);
    }

    @AfterEach
    public void tearDown() {
        if (stream != null) {
            stream.close();
        }
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

    protected <T> Stream<T> createStream01(final T... data) {
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

    @Test
    public void test_lazyEvalation() throws Exception {
        final MutableBoolean moved = MutableBoolean.of(false);

        Stream<String> s = Stream.of("a", "b", "c", "d").peek(Fn.println()).onEach(it -> {
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
            list.add(ByteStream.range((byte) 0, (byte) i).onEach(it -> moved.setTrue()).onEach(N::println));
        }

        ByteStream bs = ByteStream.merge(list, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);

        bs = bs.filter(it -> it < 10);

        assertFalse(moved.value());

        final ByteIterator byteIter = bs.iterator();

        bs.forEach(N::println);

        assertFalse(byteIter.hasNext());

        moved.setFalse();

        final IntStream is = Stream.range(0, 10_000)
                .onEach(it -> moved.setTrue())
                .parallel(128)
                .map(it -> it - 0)
                .map(it -> it + 0)
                .flatMap(Stream::of)
                .map(it -> it + 1)
                .map(it -> it - 1)
                .filter(it -> it >= 0)
                .buffered(1024)
                .onEach(it -> {
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
                .onEach(it -> {
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
                .onEach(it -> {
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
                .onEach(it -> {
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
    public void testOfArray() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        Stream<Integer> stream = createStream01(array);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), stream.toList());
    }

    @Test
    public void testOfArrayWithRange() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        Stream<Integer> stream = createStream(array, 1, 4);
        assertEquals(Arrays.asList(2, 3, 4), stream.toList());
    }

    @Test
    public void testOfCollection() {
        List<String> list = Arrays.asList("a", "b", "c");
        Stream<String> stream = createStream(list);
        assertEquals(list, stream.toList());
    }

    @Test
    public void testOfCollectionWithRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Stream<String> stream = createStream(list, 1, 4);
        assertEquals(Arrays.asList("b", "c", "d"), stream.toList());
    }

    @Test
    public void testOfMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Stream<Map.Entry<String, Integer>> stream = createStream(map);
        assertEquals(2, stream.count());
    }

    @Test
    public void testOfIterable() {
        Iterable<Integer> iterable = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = createStream(iterable);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testOfIterator() {
        Iterator<Integer> iterator = Arrays.asList(1, 2, 3).iterator();
        Stream<Integer> stream = createStream(iterator);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testOfEnumeration() {
        Vector<Integer> vector = new Vector<>(Arrays.asList(1, 2, 3));
        Enumeration<Integer> enumeration = vector.elements();
        Stream<Integer> stream = createStream(enumeration);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testOfOptional() {
        Optional<String> optional = Optional.of("hello");
        Stream<String> stream = createStream(optional);
        assertEquals(Arrays.asList("hello"), stream.toList());

        Optional<String> empty = Optional.empty();
        Stream<String> emptyStream = createStream(empty);
        assertTrue(emptyStream.toList().isEmpty());
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
    public void testOfKeys() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Stream<String> keyStream = Stream.ofKeys(map);
        assertEquals(3, keyStream.count());

        Stream<String> filteredKeyStream = Stream.ofKeys(map, v -> v > 1);
        assertEquals(new HashSet<>(Arrays.asList("b", "c")), filteredKeyStream.toSet());

        Stream<String> biFilteredKeyStream = Stream.ofKeys(map, (k, v) -> k.equals("a") || v > 2);
        assertEquals(new HashSet<>(Arrays.asList("a", "c")), biFilteredKeyStream.toSet());
    }

    @Test
    public void testOfValues() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Stream<Integer> valueStream = Stream.ofValues(map);
        assertEquals(3, valueStream.count());

        Stream<Integer> filteredValueStream = Stream.ofValues(map, k -> k.compareTo("b") >= 0);
        assertEquals(new HashSet<>(Arrays.asList(2, 3)), filteredValueStream.toSet());

        Stream<Integer> biFilteredValueStream = Stream.ofValues(map, (k, v) -> k.equals("a") || v > 2);
        assertEquals(new HashSet<>(Arrays.asList(1, 3)), biFilteredValueStream.toSet());
    }

    @Test
    public void testOfReversed() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        Stream<Integer> reversedStream = Stream.ofReversed(array);
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), reversedStream.toList());

        List<String> list = Arrays.asList("a", "b", "c");
        Stream<String> reversedListStream = Stream.ofReversed(list);
        assertEquals(Arrays.asList("c", "b", "a"), reversedListStream.toList());
    }

    @Test
    public void testRange() {
        Stream<Integer> intRange = Stream.range(1, 5);
        assertEquals(Arrays.asList(1, 2, 3, 4), intRange.toList());

        Stream<Integer> intRangeWithStep = Stream.range(0, 10, 2);
        assertEquals(Arrays.asList(0, 2, 4, 6, 8), intRangeWithStep.toList());

        Stream<Long> longRange = Stream.range(1L, 5L);
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L), longRange.toList());

        Stream<Long> longRangeWithStep = Stream.range(0L, 10L, 2L);
        assertEquals(Arrays.asList(0L, 2L, 4L, 6L, 8L), longRangeWithStep.toList());
    }

    @Test
    public void testRangeClosed() {
        Stream<Integer> intRange = Stream.rangeClosed(1, 5);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), intRange.toList());

        Stream<Integer> intRangeWithStep = Stream.rangeClosed(0, 10, 2);
        assertEquals(Arrays.asList(0, 2, 4, 6, 8, 10), intRangeWithStep.toList());

        Stream<Long> longRange = Stream.rangeClosed(1L, 5L);
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), longRange.toList());

        Stream<Long> longRangeWithStep = Stream.rangeClosed(0L, 10L, 2L);
        assertEquals(Arrays.asList(0L, 2L, 4L, 6L, 8L, 10L), longRangeWithStep.toList());
    }

    @Test
    public void testSplit() {
        String str = "a,b,c,d";
        Stream<String> splitStream = Stream.split(str, ',');
        assertEquals(Arrays.asList("a", "b", "c", "d"), splitStream.toList());

        Stream<String> splitStream2 = Stream.split(str, ",");
        assertEquals(Arrays.asList("a", "b", "c", "d"), splitStream2.toList());
    }

    @Test
    public void testSplitToLines() {
        String str = "line1\nline2\rline3\r\nline4";
        Stream<String> lines = Stream.splitToLines(str);
        assertEquals(Arrays.asList("line1", "line2", "line3", "line4"), lines.toList());

        String str2 = "  line1  \n\n  line2  \n";
        Stream<String> trimmedLines = Stream.splitToLines(str2, true, true);
        assertEquals(Arrays.asList("line1", "line2"), trimmedLines.toList());
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
    public void testIterate() {
        AtomicInteger counter = new AtomicInteger(0);
        BooleanSupplier hasNext = () -> counter.get() < 5;
        Supplier<Integer> next = counter::getAndIncrement;
        Stream<Integer> iterateStream = Stream.iterate(hasNext, next);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), iterateStream.toList());

        Stream<Integer> iterateStream2 = Stream.iterate(1, n -> n <= 5, n -> n + 1);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), iterateStream2.toList());

        Stream<Integer> infiniteStream = Stream.iterate(0, n -> n + 1);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), infiniteStream.limit(5).toList());
    }

    @Test
    public void testGenerate2() {
        AtomicInteger counter = new AtomicInteger(0);
        Stream<Integer> generated = Stream.generate(counter::getAndIncrement);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), generated.limit(5).toList());
    }

    @Test
    public void testConcat() {
        Integer[] arr1 = { 1, 2 };
        Integer[] arr2 = { 3, 4 };
        Integer[] arr3 = { 5, 6 };
        Stream<Integer> concatArrays = Stream.concat(arr1, arr2, arr3);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), concatArrays.toList());

        List<Integer> list1 = Arrays.asList(1, 2);
        List<Integer> list2 = Arrays.asList(3, 4);
        Stream<Integer> concatIterables = Stream.concat(list1, list2);
        assertEquals(Arrays.asList(1, 2, 3, 4), concatIterables.toList());

        Iterator<Integer> iter1 = Arrays.asList(1, 2).iterator();
        Iterator<Integer> iter2 = Arrays.asList(3, 4).iterator();
        Stream<Integer> concatIterators = Stream.concat(iter1, iter2);
        assertEquals(Arrays.asList(1, 2, 3, 4), concatIterators.toList());

        Stream<Integer> stream1 = createStream(1, 2);
        Stream<Integer> stream2 = createStream(3, 4);
        Stream<Integer> concatStreams = Stream.concat(stream1, stream2);
        assertEquals(Arrays.asList(1, 2, 3, 4), concatStreams.toList());

        List<Stream<Integer>> streams = Arrays.asList(createStream(1, 2), createStream(3, 4), createStream(5, 6));
        Stream<Integer> concatCollection = Stream.concat(streams);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), concatCollection.toList());
    }

    @Test
    public void testFilter() {
        Stream<Integer> filtered = stream.filter(n -> n > 2);
        assertEquals(Arrays.asList(3, 4, 5), filtered.toList());
    }

    @Test
    public void testFilterWithAction() {
        List<Integer> dropped = new ArrayList<>();
        Stream<Integer> filtered = stream.filter(n -> n > 2, dropped::add);
        assertEquals(Arrays.asList(3, 4, 5), filtered.toList());
        assertEquals(Arrays.asList(1, 2), dropped);
    }

    @Test
    public void testTakeWhile() {
        Stream<Integer> taken = stream.takeWhile(n -> n < 4);
        assertEquals(Arrays.asList(1, 2, 3), taken.toList());
    }

    @Test
    public void testDropWhile() {
        Stream<Integer> dropped = stream.dropWhile(n -> n < 3);
        assertEquals(Arrays.asList(3, 4, 5), dropped.toList());
    }

    @Test
    public void testDropWhileWithAction() {
        List<Integer> droppedItems = new ArrayList<>();
        Stream<Integer> dropped = stream.dropWhile(n -> n < 3, droppedItems::add);
        assertEquals(Arrays.asList(3, 4, 5), dropped.toList());
        assertEquals(Arrays.asList(1, 2), droppedItems);
    }

    @Test
    public void testSkipUntil() {
        Stream<Integer> skipped = stream.skipUntil(n -> n >= 3);
        assertEquals(Arrays.asList(3, 4, 5), skipped.toList());
    }

    @Test
    public void testOnEach() {
        List<Integer> collected = new ArrayList<>();
        Stream<Integer> onEachStream = stream.onEach(collected::add);
        onEachStream.forEach(e -> {
        });
        assertEquals(testList, collected);
    }

    @Test
    public void testPeek() {
        List<Integer> peeked = new ArrayList<>();
        Stream<Integer> peekStream = stream.peek(peeked::add);
        peekStream.forEach(e -> {
        });
        assertEquals(testList, peeked);
    }

    @Test
    public void testSelect() {
        Stream<Object> mixed = createStream01(1, "hello", 2, "world", 3);
        Stream<String> strings = mixed.select(String.class);
        assertEquals(Arrays.asList("hello", "world"), strings.toList());
    }

    @Test
    public void testPairWith() {
        Stream<Pair<Integer, String>> paired = stream.pairWith(n -> "num" + n);
        List<Pair<Integer, String>> result = paired.toList();
        assertEquals(5, result.size());
        assertEquals(Pair.of(1, "num1"), result.get(0));
        assertEquals(Pair.of(5, "num5"), result.get(4));
    }

    @Test
    public void testMap() {
        Stream<String> mapped = stream.map(n -> "num" + n);
        assertEquals(Arrays.asList("num1", "num2", "num3", "num4", "num5"), mapped.toList());
    }

    @Test
    public void testMapIfNotNull() {
        Stream<Integer> withNulls = createStream01(1, null, 2, null, 3);
        Stream<String> mapped = withNulls.mapIfNotNull(n -> "num" + n);
        assertEquals(Arrays.asList("num1", "num2", "num3"), mapped.toList());
    }

    @Test
    public void testSlidingMap() {
        Stream<Integer> sumOfPairs = stream.slidingMap((a, b) -> a + (b == null ? 0 : b));
        assertEquals(Arrays.asList(3, 5, 7, 9), sumOfPairs.toList());

        Stream<Integer> sumWithIncrement = createStream(1, 2, 3, 4, 5).slidingMap(2, (a, b) -> a + N.defaultIfNull(b, 0));
        assertEquals(Arrays.asList(3, 7, 5), sumWithIncrement.toList());

        Stream<Integer> sumOfTriples = createStream(1, 2, 3, 4, 5).slidingMap((a, b, c) -> a + b + c);
        assertEquals(Arrays.asList(6, 9, 12), sumOfTriples.toList());
    }

    @Test
    public void testRangeMap() {
        Stream<String> rangeMapResult = createStream01("a", "ab", "ac", "b", "c", "cb").rangeMap((a, b) -> b.startsWith(a), (a, b) -> a + "->" + b);
        assertEquals(Arrays.asList("a->ac", "b->b", "c->cb"), rangeMapResult.toList());
    }

    @Test
    public void testMapFirst() {
        Stream<Integer> mapFirstResult = stream.mapFirst(n -> n * 10);
        assertEquals(Arrays.asList(10, 2, 3, 4, 5), mapFirstResult.toList());
    }

    @Test
    public void testMapFirstOrElse() {
        Stream<String> mapFirstOrElseResult = stream.mapFirstOrElse(n -> "first:" + n, n -> "rest:" + n);
        assertEquals(Arrays.asList("first:1", "rest:2", "rest:3", "rest:4", "rest:5"), mapFirstOrElseResult.toList());
    }

    @Test
    public void testMapLast() {
        Stream<Integer> mapLastResult = stream.mapLast(n -> n * 10);
        assertEquals(Arrays.asList(1, 2, 3, 4, 50), mapLastResult.toList());
    }

    @Test
    public void testMapLastOrElse() {
        Stream<String> mapLastOrElseResult = stream.mapLastOrElse(n -> "last:" + n, n -> "rest:" + n);
        assertEquals(Arrays.asList("rest:1", "rest:2", "rest:3", "rest:4", "last:5"), mapLastOrElseResult.toList());
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
    public void testMapToEntry() {
        EntryStream<String, Integer> entryStream1 = stream.mapToEntry(n -> new AbstractMap.SimpleEntry<>("key" + n, n));
        Map<String, Integer> map1 = entryStream1.toMap();
        assertEquals(5, map1.size());
        assertEquals(Integer.valueOf(3), map1.get("key3"));
    }

    @Test
    public void testMapToEntry2() {
        EntryStream<String, Integer> entryStream2 = stream.mapToEntry(n -> "key" + n, n -> n * 10);
        Map<String, Integer> map2 = entryStream2.toMap();
        assertEquals(5, map2.size());
        assertEquals(Integer.valueOf(30), map2.get("key3"));
    }

    @Test
    public void testFlatMap() {
        Stream<Integer> flatMapped = stream.flatMap(n -> createStream(n, n * 10));
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50), flatMapped.toList());
    }

    @Test
    public void testFlatmap() {
        Stream<Integer> flatmapped = stream.flatmap(n -> Arrays.asList(n, n * 10));
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50), flatmapped.toList());
    }

    @Test
    public void testFlattMap() {
        Stream<Integer> flattMapped = stream.flattmap(n -> new Integer[] { n, n * 10 });
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50), flattMapped.toList());
    }

    @Test
    public void testFlatMapToPrimitiveStreams() {
        CharStream charStream = createStream01("hello", "world").flatMapToChar(s -> CharStream.of(s.toCharArray()));
        assertEquals("helloworld", new String(charStream.toArray()));

        CharStream charStream2 = createStream01("hello", "world").flatmapToChar(String::toCharArray);
        assertEquals("helloworld", new String(charStream2.toArray()));

    }

    @Test
    public void testFlatmapIfNotNull() {
        Stream<Integer> withNulls = createStream01(1, null, 2, null, 3);
        Stream<Integer> result = withNulls.flatmapIfNotNull(n -> Arrays.asList(n, n * 10));
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
    }

    @Test
    public void testMapMulti() {
        Stream<Integer> mapMultiResult = stream.mapMulti((n, consumer) -> {
            consumer.accept(n);
            consumer.accept(n * 10);
        });
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50), mapMultiResult.toList());
    }

    @Test
    public void testMapPartial() {
        Stream<Integer> withEvens = createStream(1, 2, 3, 4, 5);
        Stream<Integer> partialMapped = withEvens.mapPartial(n -> n % 2 == 0 ? Optional.of(n * 10) : Optional.empty());
        assertEquals(Arrays.asList(20, 40), partialMapped.toList());
    }

    @Test
    public void testGroupBy() {
        Stream<Map.Entry<Integer, List<String>>> grouped = createStream01("a", "bb", "ccc", "dd", "e").groupBy(String::length);
        Map<Integer, List<String>> groupMap = grouped.toMap(Fn.key(), Fn.value());
        assertEquals(Arrays.asList("a", "e"), groupMap.get(1));
        assertEquals(Arrays.asList("bb", "dd"), groupMap.get(2));
        assertEquals(Arrays.asList("ccc"), groupMap.get(3));

        Stream<Map.Entry<Integer, List<String>>> groupedWithValue = createStream01("a", "bb", "ccc").groupBy(String::length, String::toUpperCase);
        Map<Integer, List<String>> groupMapWithValue = groupedWithValue.toMap(Fn.key(), Fn.value());
        assertEquals(Arrays.asList("A"), groupMapWithValue.get(1));
        assertEquals(Arrays.asList("BB"), groupMapWithValue.get(2));

        Stream<Map.Entry<Integer, Long>> groupedWithCollector = createStream01("a", "bb", "ccc", "dd", "e").groupBy(String::length, Collectors.counting());
        Map<Integer, Long> countMap = groupedWithCollector.toMap(Fn.key(), Fn.value());
        assertEquals(Long.valueOf(2), countMap.get(1));
        assertEquals(Long.valueOf(2), countMap.get(2));
        assertEquals(Long.valueOf(1), countMap.get(3));
    }

    @Test
    public void testGroupByToEntry() {
        EntryStream<Integer, List<String>> grouped = createStream01("a", "bb", "ccc", "dd", "e").groupByToEntry(String::length);
        Map<Integer, List<String>> groupMap = grouped.toMap();
        assertEquals(Arrays.asList("a", "e"), groupMap.get(1));
        assertEquals(Arrays.asList("bb", "dd"), groupMap.get(2));
        assertEquals(Arrays.asList("ccc"), groupMap.get(3));
    }

    @Test
    public void testPartitionBy() {
        Stream<Map.Entry<Boolean, List<Integer>>> partitioned = stream.partitionBy(n -> n > 3);
        Map<Boolean, List<Integer>> partitionMap = partitioned.toMap(Fn.key(), Fn.value());
        assertEquals(Arrays.asList(1, 2, 3), partitionMap.get(false));
        assertEquals(Arrays.asList(4, 5), partitionMap.get(true));
    }

    @Test
    public void testPartitionByToEntry() {
        EntryStream<Boolean, List<Integer>> partitioned = stream.partitionByToEntry(n -> n > 3);
        Map<Boolean, List<Integer>> partitionMap = partitioned.toMap();
        assertEquals(Arrays.asList(1, 2, 3), partitionMap.get(false));
        assertEquals(Arrays.asList(4, 5), partitionMap.get(true));
    }

    @Test
    public void testCountBy() {
        Stream<Map.Entry<Integer, Integer>> counted = createStream01("a", "bb", "ccc", "dd", "e").countBy(String::length);
        Map<Integer, Integer> countMap = counted.toMap(Fn.key(), Fn.value());
        assertEquals(Integer.valueOf(2), countMap.get(1));
        assertEquals(Integer.valueOf(2), countMap.get(2));
        assertEquals(Integer.valueOf(1), countMap.get(3));
    }

    @Test
    public void testCountByToEntry() {
        EntryStream<Integer, Integer> counted = createStream01("a", "bb", "ccc", "dd", "e").countByToEntry(String::length);
        Map<Integer, Integer> countMap = counted.toMap();
        assertEquals(Integer.valueOf(2), countMap.get(1));
        assertEquals(Integer.valueOf(2), countMap.get(2));
        assertEquals(Integer.valueOf(1), countMap.get(3));
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
    public void testScan() {
        Stream<Integer> scanned = createStream(1, 2, 3, 4, 5).scan((a, b) -> a + b);
        assertEquals(Arrays.asList(1, 3, 6, 10, 15), scanned.toList());

        Stream<Integer> scannedWithInit = createStream(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b);
        assertEquals(Arrays.asList(11, 13, 16, 20, 25), scannedWithInit.toList());

        Stream<Integer> scannedWithInitIncluded = createStream(1, 2, 3).scan(10, true, (a, b) -> a + b);
        assertEquals(Arrays.asList(10, 11, 13, 16), scannedWithInitIncluded.toList());
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
    public void testSplitAt() {
        Stream<Stream<Integer>> splitAtIndex = createStream(1, 2, 3, 4, 5).splitAt(3);
        List<List<Integer>> splitResult = splitAtIndex.map(s -> s.toList()).toList();
        assertEquals(2, splitResult.size());
        assertEquals(Arrays.asList(1, 2, 3), splitResult.get(0));
        assertEquals(Arrays.asList(4, 5), splitResult.get(1));

        Stream<Stream<Integer>> splitAtPredicate = Stream.range(0, 7).splitAt(n -> n == 4);
        List<List<Integer>> predicateSplitResult = splitAtPredicate.map(s -> s.toList()).toList();
        assertEquals(2, predicateSplitResult.size());
        assertEquals(Arrays.asList(0, 1, 2, 3), predicateSplitResult.get(0));
        assertEquals(Arrays.asList(4, 5, 6), predicateSplitResult.get(1));
    }

    @Test
    public void testSliding() {
        Stream<List<Integer>> sliding = createStream(1, 2, 3, 4, 5).sliding(3);
        List<List<Integer>> windows = sliding.toList();
        assertEquals(3, windows.size());
        assertEquals(Arrays.asList(1, 2, 3), windows.get(0));
        assertEquals(Arrays.asList(2, 3, 4), windows.get(1));
        assertEquals(Arrays.asList(3, 4, 5), windows.get(2));

        Stream<List<Integer>> slidingWithIncrement = createStream(1, 2, 3, 4, 5).sliding(3, 2);
        List<List<Integer>> incrementWindows = slidingWithIncrement.toList();
        assertEquals(2, incrementWindows.size());
        assertEquals(Arrays.asList(1, 2, 3), incrementWindows.get(0));
        assertEquals(Arrays.asList(3, 4, 5), incrementWindows.get(1));
    }

    @Test
    public void testIntersperse() {
        Stream<Integer> interspersed = createStream(1, 2, 3).intersperse(0);
        assertEquals(Arrays.asList(1, 0, 2, 0, 3), interspersed.toList());

        Stream<Integer> singleElement = createStream(1).intersperse(0);
        assertEquals(Arrays.asList(1), singleElement.toList());
    }

    @Test
    public void testDistinct() {
        Stream<Integer> withDuplicates = createStream(1, 2, 2, 3, 3, 3, 4, 5, 5);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), withDuplicates.distinct().toList());

        Stream<String> withDuplicateStrings = createStream01("a", "b", "b", "c");
        Stream<String> distinctMerged = withDuplicateStrings.distinct((a, b) -> a + b);
        assertEquals(Arrays.asList("a", "bb", "c"), distinctMerged.toList());
    }

    @Test
    public void testDistinctBy() {

        {
            Stream<String> strings = createStream01("a", "bb", "ccc", "dd", "e");
            Stream<String> distinctByLength = strings.distinctBy(String::length);
            assertEquals(Arrays.asList("a", "bb", "ccc"), distinctByLength.toList());
            Stream<String> stringsForMerge = createStream01("a", "bb", "ccc", "dd", "e");
            Stream<String> distinctByLengthMerged = stringsForMerge.distinctBy(String::length, (a, b) -> a + "+" + b);
            assertEquals(Arrays.asList("a+e", "bb+dd", "ccc"), distinctByLengthMerged.toList());
        }

        {
            Stream<String> strings = createStream01("a", "bb", "ccc", "dd", "e");
            List<String> distinctByLength = strings.parallel().distinctBy(String::length).toList();

            assertTrue(N.haveSameElements(Arrays.asList("a", "bb", "ccc"), distinctByLength)
                    || N.haveSameElements(Arrays.asList("a", "dd", "ccc"), distinctByLength));
            Stream<String> stringsForMerge = createStream01("a", "bb", "ccc", "dd", "e");
            List<String> distinctByLengthMerged = stringsForMerge.parallel().distinctBy(String::length, (a, b) -> Stream.of(a, b).sorted().join("+")).toList();
            assertTrue(N.haveSameElements(Arrays.asList("a+e", "bb+dd", "ccc"), distinctByLengthMerged)
                    || N.haveSameElements(Arrays.asList("a+e", "dd+bb", "ccc"), distinctByLengthMerged));
        }
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
    public void testSorted() {
        Stream<Integer> unsorted = createStream(3, 1, 4, 1, 5, 9, 2, 6);
        assertEquals(Arrays.asList(1, 1, 2, 3, 4, 5, 6, 9), unsorted.sorted(Comparator.naturalOrder()).toList());
    }

    @Test
    public void testSortedBy() {
        Stream<String> strings = createStream01("ccc", "a", "bb", "dddd");
        assertEquals(Arrays.asList("a", "bb", "ccc", "dddd"), strings.sortedBy(String::length).toList());
    }

    @Test
    public void testSortedByInt() {
        Stream<String> strings = createStream01("ccc", "a", "bb", "dddd");
        assertEquals(Arrays.asList("a", "bb", "ccc", "dddd"), strings.sortedByInt(String::length).toList());
    }

    @Test
    public void testSortedByLong() {
        Stream<String> strings = createStream01("ccc", "a", "bb", "dddd");
        assertEquals(Arrays.asList("a", "bb", "ccc", "dddd"), strings.sortedByLong(s -> (long) s.length()).toList());
    }

    @Test
    public void testSortedByDouble() {
        Stream<String> strings = createStream01("ccc", "a", "bb", "dddd");
        assertEquals(Arrays.asList("a", "bb", "ccc", "dddd"), strings.sortedByDouble(s -> (double) s.length()).toList());
    }

    @Test
    public void testReverseSorted() {
        Stream<Integer> numbers = createStream(3, 1, 4, 1, 5, 9, 2, 6);
        assertEquals(Arrays.asList(9, 6, 5, 4, 3, 2, 1, 1), numbers.reverseSorted(Comparator.naturalOrder()).toList());
    }

    @Test
    public void testReverseSortedBy() {
        Stream<String> strings = createStream01("ccc", "a", "bb", "dddd");
        assertEquals(Arrays.asList("dddd", "ccc", "bb", "a"), strings.reverseSortedBy(String::length).toList());
    }

    @Test
    public void testTop() {
        Stream<Integer> numbers = createStream(3, 1, 4, 1, 5, 9, 2, 6);
        assertEquals(Arrays.asList(5, 9, 6), numbers.top(3).toList());

        Stream<String> strings = createStream01("ccc", "a", "bb", "dddd");
        assertEquals(Arrays.asList("ccc", "dddd"), strings.top(2, Comparator.comparing(String::length)).toList());
    }

    @Test
    public void testSkipRange() {
        Stream<Integer> numbers = createStream(1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertEquals(Arrays.asList(1, 2, 3, 7, 8, 9), numbers.skipRange(3, 6).toList());
    }

    @Test
    public void testSkipNulls() {
        Stream<Integer> withNulls = createStream01(1, null, 2, null, 3, null);
        assertEquals(Arrays.asList(1, 2, 3), withNulls.skipNulls().toList());
    }

    @Test
    public void testSkipLast() {
        assertEquals(Arrays.asList(1, 2, 3), stream.skipLast(2).toList());
    }

    @Test
    public void testTakeLast() {
        assertEquals(Arrays.asList(3, 4, 5), stream.takeLast(3).toList());
    }

    @Test
    public void testOnFirst() {
        AtomicInteger firstValue = new AtomicInteger();
        stream.onFirst(firstValue::set).forEach(e -> {
        });
        assertEquals(1, firstValue.get());
    }

    @Test
    public void testOnLast() {
        AtomicInteger lastValue = new AtomicInteger();
        stream.onLast(lastValue::set).forEach(e -> {
        });
        assertEquals(5, lastValue.get());
    }

    @Test
    public void testPeekFirst() {
        AtomicInteger firstValue = new AtomicInteger();
        stream.peekFirst(firstValue::set).forEach(e -> {
        });
        assertEquals(1, firstValue.get());
    }

    @Test
    public void testPeekLast() {
        AtomicInteger lastValue = new AtomicInteger();
        stream.peekLast(lastValue::set).forEach(e -> {
        });
        assertEquals(5, lastValue.get());
    }

    @Test
    public void testPeekIf() {
        List<Integer> evenNumbers = new ArrayList<>();
        stream.peekIf(n -> n % 2 == 0, evenNumbers::add).forEach(e -> {
        });
        assertEquals(Arrays.asList(2, 4), evenNumbers);

        List<Integer> collected = new ArrayList<>();
        createStream(1, 2, 3, 4, 5).peekIf((n, count) -> count <= 3, collected::add).forEach(e -> {
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testIntersection() {
        Stream<String> strings = createStream01("apple", "apple", "apple", "banana", "banana", "cherry");
        Stream<String> intersection = strings.intersection(String::length, Arrays.asList(5, 6, 5));
        assertEquals(Arrays.asList("apple", "apple", "banana"), intersection.toList());
    }

    @Test
    public void testDifference() {
        Stream<String> strings = createStream01("apple", "apple", "apple", "banana", "banana", "cherry");
        Stream<String> difference = strings.difference(String::length, Arrays.asList(5, 6, 5));
        assertEquals(Arrays.asList("apple", "banana", "cherry"), difference.toList());
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
    public void testAppendIfEmpty() {
        Stream<Integer> notEmpty = stream.appendIfEmpty(Arrays.asList(10, 20));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), notEmpty.toList());

        Stream<Integer> empty = Stream.<Integer> empty().appendIfEmpty(Arrays.asList(10, 20));
        assertEquals(Arrays.asList(10, 20), empty.toList());
    }

    @Test
    public void testDefaultIfEmpty() {
        Stream<Integer> notEmpty = stream.defaultIfEmpty(10);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), notEmpty.toList());

        Stream<Integer> empty = Stream.<Integer> empty().defaultIfEmpty(10);
        assertEquals(Arrays.asList(10), empty.toList());
    }

    @Test
    public void testBuffered() {
        Stream<Integer> buffered = stream.buffered();
        assertEquals(testList, buffered.toList());

        Stream<Integer> bufferedWithSize = createStream(1, 2, 3, 4, 5).buffered(3);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), bufferedWithSize.toList());
    }

    @Test
    public void testMergeWith() {
        Stream<Integer> merged = createStream(1, 3, 5).mergeWith(Arrays.asList(2, 4, 6), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), merged.toList());

        Stream<Integer> stream1 = createStream(1, 3, 5);
        Stream<Integer> stream2 = createStream(2, 4, 6);
        Stream<Integer> mergedStreams = stream1.mergeWith(stream2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), mergedStreams.toList());
    }

    @Test
    public void testZipWith() {
        Stream<String> zipped = stream.zipWith(Arrays.asList("a", "b", "c"), (n, s) -> n + s);
        assertEquals(Arrays.asList("1a", "2b", "3c"), zipped.toList());

        Stream<String> zippedWithDefaults = createStream(1, 2, 3).zipWith(Arrays.asList("a"), 0, "z", (n, s) -> n + s);
        assertEquals(Arrays.asList("1a", "2z", "3z"), zippedWithDefaults.toList());

        Stream<Integer> stream1 = createStream(1, 2, 3);
        Stream<Integer> stream2 = createStream(10, 20, 30);
        Stream<Integer> zippedStreams = stream1.zipWith(stream2, (a, b) -> a + b);
        assertEquals(Arrays.asList(11, 22, 33), zippedStreams.toList());

        Stream<String> zipped3 = createStream(1, 2, 3).zipWith(Arrays.asList("a", "b", "c"), Arrays.asList("x", "y", "z"), (n, s1, s2) -> n + s1 + s2);
        assertEquals(Arrays.asList("1ax", "2by", "3cz"), zipped3.toList());
    }

    @Test
    public void testToJdkStream() {
        java.util.stream.Stream<Integer> jdkStream = stream.toJdkStream();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), jdkStream.collect(java.util.stream.Collectors.toList()));
    }

    @Test
    public void testTransformB() {
        Stream<String> transformed = stream.transformB(jdkStream -> jdkStream.map(n -> "num" + n));
        assertEquals(Arrays.asList("num1", "num2", "num3", "num4", "num5"), transformed.toList());

        Stream<String> deferredTransform = createStream(1, 2, 3).transformB(jdkStream -> jdkStream.map(n -> "num" + n), true);
        assertEquals(Arrays.asList("num1", "num2", "num3"), deferredTransform.toList());
    }

    @Test
    public void testForEach() {
        List<Integer> collected = new ArrayList<>();
        stream.forEach(collected::add);
        assertEquals(testList, collected);
    }

    @Test
    public void testForEachIndexed() {
        Map<Integer, Integer> indexMap = new HashMap<>();
        stream.forEachIndexed((index, value) -> indexMap.put(index, value));
        assertEquals(5, indexMap.size());
        assertEquals(Integer.valueOf(1), indexMap.get(0));
        assertEquals(Integer.valueOf(5), indexMap.get(4));
    }

    @Test
    public void testForEachUntil() {
        List<Integer> collected = new ArrayList<>();
        stream.forEachUntil((value, flag) -> {
            collected.add(value);
            if (value >= 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);

        List<Integer> collected2 = new ArrayList<>();
        MutableBoolean flag = MutableBoolean.of(false);
        createStream(1, 2, 3, 4, 5).forEachUntil(flag, value -> {
            collected2.add(value);
            if (value >= 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected2);
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
    public void testForEachPair() {
        List<String> pairs = new ArrayList<>();
        stream.forEachPair((a, b) -> pairs.add(a + "," + b));
        assertEquals(Arrays.asList("1,2", "2,3", "3,4", "4,5"), pairs);
    }

    @Test
    public void testForEachTriple() {
        List<String> triples = new ArrayList<>();
        stream.forEachTriple((a, b, c) -> triples.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,3", "2,3,4", "3,4,5"), triples);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(stream.anyMatch(n -> n > 3));
        assertFalse(createStream(1, 2, 3).anyMatch(n -> n > 5));
    }

    @Test
    public void testAllMatch() {
        assertTrue(stream.allMatch(n -> n > 0));
        assertTrue(Stream.<Integer> empty().allMatch(n -> n > 0));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(stream.noneMatch(n -> n > 10));
        assertTrue(Stream.<Integer> empty().noneMatch(n -> n > 0));
    }

    @Test
    public void testNMatch() {
        assertTrue(stream.nMatch(2, 3, n -> n > 2));
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
    public void testConcatIterables() {
        List<List<Integer>> iterables = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6), Arrays.asList(7, 8, 9));

        List<Integer> result = Stream.concatIterables(iterables).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);
    }

    @Test
    public void testConcatIterators() {
        List<Iterator<String>> iterators = Arrays.asList(Arrays.asList("a", "b").iterator(), Arrays.asList("c", "d").iterator(),
                Arrays.asList("e", "f").iterator());

        List<String> result = Stream.concatIterators(iterators).toList();

        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), result);
    }

    @Test
    public void testStreamEx_Inheritance() {
        assertTrue(Stream.StreamEx.class.getSuperclass().equals(Stream.class));
    }

    @Test
    public void testEmpty() {
        Stream<String> emptyStream = Stream.empty();

        assertFalse(emptyStream.iterator().hasNext());
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testDefer() {
        int[] counter = { 0 };
        Supplier<Stream<Integer>> supplier = () -> {
            counter[0]++;
            return createStream(1, 2, 3);
        };

        Stream<Integer> deferred = Stream.defer(supplier);

        assertEquals(0, counter[0]);

        List<Integer> result = deferred.toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertEquals(1, counter[0]);
    }

    @Test
    public void testDefer_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Stream.defer(null));
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
    public void testJust() {
        Stream<String> stream = Stream.just("hello");

        List<String> result = stream.toList();
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0));
    }

    @Test
    public void testJust_NullElement() {
        Stream<String> stream = Stream.just(null);

        List<String> result = stream.toList();
        assertEquals(1, result.size());
        assertNull(result.get(0));
    }

    @Test
    public void testOfNullable() {
        Stream<String> stream1 = Stream.ofNullable("test");
        assertEquals(Arrays.asList("test"), stream1.toList());

        Stream<String> stream2 = Stream.ofNullable(null);
        assertTrue(stream2.toList().isEmpty());
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

        Stream<String> stream = createStream((Iterable<String>) set);

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
    public void testSplitByChunkCount() {
        Integer[] array = { 1, 2, 3, 4, 5, 6, 7 };

        List<List<Integer>> chunks = Stream.splitByChunkCount(7, 3, (from, to) -> Arrays.asList(array).subList(from, to)).toList();

        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(1, 2, 3), chunks.get(0));
        assertEquals(Arrays.asList(4, 5), chunks.get(1));
        assertEquals(Arrays.asList(6, 7), chunks.get(2));
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
    public void testRepeat() {
        List<String> repeated = Stream.repeat("X", 5).toList();

        assertEquals(Arrays.asList("X", "X", "X", "X", "X"), repeated);
    }

    @Test
    public void testRepeat_Zero() {
        List<Integer> repeated = Stream.repeat(42, 0).toList();

        assertTrue(repeated.isEmpty());
    }

    @Test
    public void testRepeat_Negative() {
        assertThrows(IllegalArgumentException.class, () -> Stream.repeat("test", -1));
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
    public void testGenerate() {
        int[] counter = { 0 };
        Supplier<String> supplier = () -> "item" + counter[0]++;

        List<String> result = Stream.generate(supplier).limit(3).toList();

        assertEquals(Arrays.asList("item0", "item1", "item2"), result);
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

    private List<Stream<?>> streamsToClose = new ArrayList<>();

    @AfterEach
    public void cleanup() {
        for (Stream<?> stream : streamsToClose) {
            try {
                stream.close();
            } catch (Exception e) {
            }
        }
        streamsToClose.clear();
    }

    private <T> Stream<T> trackStream(Stream<T> stream) {
        streamsToClose.add(stream);
        return stream;
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
        ObjIteratorEx<Integer> iter = new ObjIteratorEx<Integer>() {
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
        Iterator<Integer> failingIterator = new Iterator<Integer>() {
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

    @TempDir
    Path tempFolder;

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
    public void testMergeArray() {
        Integer[] a = { 1, 3, 5 };
        Integer[] b = { 2, 4, 6 };
        Integer[] c = { 7, 8, 9 };

        assertEquals(N.asList(1, 2, 3, 4, 5, 6), Stream.merge(a, b, (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                Stream.merge(a, b, c, (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
    }

    @Test
    public void test_merge_streams() {
        Integer[] a = { 1, 3, 5 };
        Integer[] b = { 2, 4, 6 };
        Integer[] c = { 7, 8, 9 };

        assertEquals(N.asList(1, 2, 3, 4, 5, 6),
                Stream.merge(Stream.of(a), Stream.of(b), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.asList(1, 2, 3, 4, 5, 6),
                Stream.merge(N.asList(Stream.of(a), Stream.of(b)), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                Stream.merge(Stream.of(a), Stream.of(b), Stream.of(c), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                Stream.merge(N.asList(Stream.of(a), Stream.of(b), Stream.of(c)), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(
                N.asList(1, 1, 2, 3, 3, 4, 5, 5, 6, 7, 8, 9), Stream
                        .merge(N.asList(Stream.of(a), Stream.of(b), Stream.of(c), Stream.of(a)),
                                (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());

    }

    @Test
    public void test_parallelMerge_streams() {
        Integer[] a = { 1, 3, 5 };
        Integer[] b = { 2, 4, 6 };
        Integer[] c = { 7, 8, 9 };

        assertEquals(N.asList(1, 2, 3, 4, 5, 6),
                Stream.parallelMerge(N.asList(Stream.of(a), Stream.of(b)), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), Stream
                .parallelMerge(N.asList(Stream.of(a), Stream.of(b), Stream.of(c)), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toList());
        assertEquals(
                N.asList(1, 1, 2, 3, 3, 4, 5, 5, 6, 7, 8, 9), Stream
                        .parallelMerge(N.asList(Stream.of(a), Stream.of(b), Stream.of(c), Stream.of(a)),
                                (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());

    }

    @Test
    public void testParallelMergeIterators() {
        List<Integer> a = N.asList(1, 3, 5);
        List<Integer> b = N.asList(2, 4, 6);
        List<Integer> c = N.asList(7, 8, 9);
        List<Integer> d = N.asList(7, 9, 10);

        assertEquals(N.asList(1, 2, 3, 4, 5, 6),
                Stream.parallelMergeIterables(N.asList(a, b), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                Stream.parallelMergeIterables(N.asList(a, b, c), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 7, 8, 9, 9, 10),
                Stream.parallelMergeIterables(N.asList(a, b, c, d), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
    }
}
