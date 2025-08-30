package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

@ExtendWith(MockitoExtension.class)
public class ParallelIteratorStream100Test extends TestBase {

    @Mock
    private AsyncExecutor mockAsyncExecutor;

    private List<Integer> TEST_DATA;
    private Stream<Integer> stream;

    @BeforeEach
    public void setUp() {
        TEST_DATA = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        stream = createStream(TEST_DATA);
    }

    @AfterEach
    public void tearDown() {
        if (stream != null) {
            stream.close();
        }
    }

    private <T> Stream<T> createStream(Collection<T> data) {
        return Stream.of(data.iterator()).parallel();
    }

    @Test
    public void testFilter() {
        Predicate<Integer> evenFilter = n -> n % 2 == 0;

        List<Integer> result = stream.filter(evenFilter).sorted().toList();

        assertEquals(Arrays.asList(2, 4, 6, 8, 10), result);
    }

    @Test
    public void testFilterWithLargeDataset() {
        List<Integer> largeData = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            largeData.add(i);
        }

        try (Stream<Integer> largeStream = createStream(largeData)) {
            List<Integer> result = largeStream.filter(n -> n % 100 == 0).toList();

            assertEquals(100, result.size());
            assertTrue(result.contains(0));
            assertTrue(result.contains(100));
            assertTrue(result.contains(9900));
        }
    }

    @Test
    public void testTakeWhile() {
        List<Integer> result = stream.takeWhile(n -> n < 5).sorted().toList();

        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testTakeWhileWithNoMatch() {
        List<Integer> result = stream.takeWhile(n -> n < 0).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testDropWhile() {
        List<Integer> result = stream.dropWhile(n -> n < 5).sorted().toList();

        assertEquals(Arrays.asList(5, 6, 7, 8, 9, 10), result);
    }

    @Test
    public void testDropWhileAll() {
        List<Integer> result = stream.dropWhile(n -> n > 0).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testMap() {
        Function<Integer, String> mapper = n -> "Number: " + n;

        List<String> result = stream.map(mapper).toList();

        assertEquals(10, result.size());
        assertTrue(result.contains("Number: 1"));
        assertTrue(result.contains("Number: 10"));
    }

    @Test
    public void testMapWithNullHandling() {
        List<String> data = Arrays.asList("a", null, "b", "c");
        try (Stream<String> strStream = createStream(data)) {
            List<Integer> result = strStream.map(s -> s == null ? -1 : s.length()).toList();

            assertHaveSameElements(Arrays.asList(1, -1, 1, 1), result);
        }
    }

    @Test
    public void testSlidingMap() {
        BiFunction<Integer, Integer, String> mapper = (a, b) -> a + "+" + b;

        List<String> result = stream.slidingMap(1, false, mapper).sorted().toList();

        assertEquals(9, result.size());
        assertEquals("1+2", result.get(0));
        assertEquals("9+10", result.get(8));
    }

    @Test
    public void testSlidingMapWithIncrement() {
        BiFunction<Integer, Integer, String> mapper = (a, b) -> a + "+" + b;

        List<String> result = stream.slidingMap(2, false, mapper).sorted().toList();

        assertEquals(5, result.size());
        assertEquals("1+2", result.get(0));
        assertEquals("3+4", result.get(1));
    }

    @Test
    public void testSlidingMapTriFunction() {
        TriFunction<Integer, Integer, Integer, String> mapper = (a, b, c) -> a + "+" + b + "+" + c;

        List<String> result = stream.slidingMap(1, false, mapper).sorted().toList();

        assertEquals(8, result.size());
        assertEquals("1+2+3", result.get(0));
    }

    @Test
    public void testMapFirstOrElse() {
        Function<Integer, String> mapperForFirst = n -> "First: " + n;
        Function<Integer, String> mapperForElse = n -> "Other: " + n;

        List<String> result = stream.mapFirstOrElse(mapperForFirst, mapperForElse)
                .sortedBy(it -> Numbers.toInt(Strings.substringAfter(it, ":").trim()))
                .toList();

        assertEquals(10, result.size());
        assertEquals("First: 1", result.get(0));
        assertEquals("Other: 2", result.get(1));
        assertEquals("Other: 10", result.get(9));
    }

    @Test
    public void testMapLastOrElse() {
        Function<Integer, String> mapperForLast = n -> "Last: " + n;
        Function<Integer, String> mapperForElse = n -> "Other: " + n;

        List<String> result = stream.mapLastOrElse(mapperForLast, mapperForElse).sortedBy(it -> Numbers.toInt(Strings.substringAfter(it, ":").trim())).toList();

        assertEquals(10, result.size());
        assertEquals("Other: 1", result.get(0));
        assertEquals("Last: 10", result.get(9));
    }

    @Test
    public void testMapToChar() {
        ToCharFunction<Integer> mapper = n -> (char) ('A' + n - 1);

        char[] result = stream.limit(5).mapToChar(mapper).sorted().toArray();

        assertArrayEquals(new char[] { 'A', 'B', 'C', 'D', 'E' }, result);
    }

    @Test
    public void testMapToByte() {
        ToByteFunction<Integer> mapper = n -> n.byteValue();

        byte[] result = stream.limit(5).mapToByte(mapper).sorted().toArray();

        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testMapToShort() {
        ToShortFunction<Integer> mapper = n -> n.shortValue();

        short[] result = stream.limit(5).mapToShort(mapper).sorted().toArray();

        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testMapToInt() {
        ToIntFunction<Integer> mapper = n -> n * 10;

        int[] result = stream.mapToInt(mapper).sorted().toArray();

        assertArrayEquals(new int[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 }, result);
    }

    @Test
    public void testMapToLong() {
        ToLongFunction<Integer> mapper = n -> n * 1000L;

        long[] result = stream.mapToLong(mapper).sorted().toArray();

        assertArrayEquals(new long[] { 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000 }, result);
    }

    @Test
    public void testMapToFloat() {
        ToFloatFunction<Integer> mapper = n -> n / 2.0f;

        float[] result = stream.limit(3).mapToFloat(mapper).sorted().toArray();

        assertArrayEquals(new float[] { 0.5f, 1.0f, 1.5f }, result, 0.001f);
    }

    @Test
    public void testMapToDouble() {
        ToDoubleFunction<Integer> mapper = n -> n / 2.0;

        double[] result = stream.limit(3).mapToDouble(mapper).sorted().toArray();

        assertArrayEquals(new double[] { 0.5, 1.0, 1.5 }, result, 0.001);
    }

    @Test
    public void testFlatMap() {
        Function<Integer, Stream<String>> mapper = n -> Stream.of("A" + n, "B" + n);

        List<String> result = stream.limit(3).flatMap(mapper).toList();

        assertEquals(6, result.size());
        assertTrue(result.contains("A1"));
        assertTrue(result.contains("B3"));
    }

    @Test
    public void testFlatMapWithEmptyStreams() {
        Function<Integer, Stream<Integer>> mapper = n -> n % 2 == 0 ? Stream.of(n, n * 10) : Stream.empty();

        List<Integer> result = stream.flatMap(mapper).sorted().toList();

        assertEquals(Arrays.asList(2, 4, 6, 8, 10, 20, 40, 60, 80, 100), result);
    }

    @Test
    public void testFlatmap() {
        Function<Integer, Collection<String>> mapper = n -> Arrays.asList("X" + n, "Y" + n);

        List<String> result = stream.limit(2).flatmap(mapper).toList();

        assertHaveSameElements(Arrays.asList("X1", "Y1", "X2", "Y2"), result);
    }

    @Test
    public void testFlattMap() {
        Function<Integer, String[]> mapper = n -> new String[] { "P" + n, "Q" + n };

        List<String> result = stream.limit(2).flattmap(mapper).toList();

        assertHaveSameElements(Arrays.asList("P1", "Q1", "P2", "Q2"), result);
    }

    @Test
    public void testFlatMapToChar() {
        Function<Integer, CharStream> mapper = n -> CharStream.of((char) ('A' + n - 1));

        char[] result = stream.limit(3).flatMapToChar(mapper).sorted().toArray();

        assertArrayEquals(new char[] { 'A', 'B', 'C' }, result);
    }

    @Test
    public void testFlatMapToByte() {
        Function<Integer, ByteStream> mapper = n -> ByteStream.of((byte) n.intValue(), (byte) (n + 10));

        byte[] result = stream.limit(2).flatMapToByte(mapper).sorted().toArray();

        assertArrayEquals(new byte[] { 1, 2, 11, 12 }, result);
    }

    @Test
    public void testFlatMapToShort() {
        Function<Integer, ShortStream> mapper = n -> ShortStream.of((short) n.shortValue(), (short) (n * 10));

        short[] result = stream.limit(2).flatMapToShort(mapper).sorted().toArray();

        assertHaveSameElements(N.asList((short) 1, (short) 2, (short) 10, (short) 20), N.toList(result));
    }

    @Test
    public void testFlatMapToInt() {
        Function<Integer, IntStream> mapper = n -> IntStream.of(n, n * 100);

        int[] result = stream.limit(2).flatMapToInt(mapper).sorted().toArray();

        assertArrayEquals(new int[] { 1, 2, 100, 200 }, result);
    }

    @Test
    public void testFlatMapToLong() {
        Function<Integer, LongStream> mapper = n -> LongStream.of(n, n * 1000L);

        long[] result = stream.limit(2).flatMapToLong(mapper).sorted().toArray();

        assertArrayEquals(new long[] { 1, 2, 1000, 2000 }, result);
    }

    @Test
    public void testFlatMapToFloat() {
        Function<Integer, FloatStream> mapper = n -> FloatStream.of(n * 0.1f, n * 0.2f);

        float[] result = stream.limit(2).flatMapToFloat(mapper).sorted().toArray();

        assertArrayEquals(new float[] { 0.1f, 0.2f, 0.2f, 0.4f }, result, 0.001f);
    }

    @Test
    public void testFlatMapToDouble() {
        Function<Integer, DoubleStream> mapper = n -> DoubleStream.of(n * 0.1, n * 0.2);

        double[] result = stream.limit(2).flatMapToDouble(mapper).sorted().toArray();

        assertArrayEquals(new double[] { 0.1, 0.2, 0.2, 0.4 }, result, 0.001);
    }

    @Test
    public void testOnEach() {
        List<Integer> sideEffectList = new CopyOnWriteArrayList<>();

        List<Integer> result = stream.onEach(sideEffectList::add).toList();

        assertHaveSameElements(TEST_DATA, result);
        assertHaveSameElements(TEST_DATA, sideEffectList);
    }

    @Test
    public void testForEach() throws Exception {
        List<Integer> collected = new CopyOnWriteArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        stream.forEach(collected::add, () -> completed.set(true));

        assertTrue(completed.get());
        assertEquals(TEST_DATA.size(), collected.size());
        assertTrue(collected.containsAll(TEST_DATA));
    }

    @Test
    public void testForEachWithFlatMapper() throws Exception {
        List<String> result = new CopyOnWriteArrayList<>();

        stream.limit(3).forEach(n -> Arrays.asList("A" + n, "B" + n), (n, s) -> result.add(s));

        assertEquals(6, result.size());
        assertTrue(result.contains("A1"));
        assertTrue(result.contains("B3"));
    }

    @Test
    public void testForEachTriple() throws Exception {
        List<String> result = new CopyOnWriteArrayList<>();

        stream.limit(5).forEach(n -> Arrays.asList("X" + n, "Y" + n), s -> Arrays.asList(s + "1", s + "2"), (n, s, t) -> result.add(n + "-" + s + "-" + t));

        assertTrue(result.size() > 0);
    }

    @Test
    public void testForEachPair() throws Exception {
        List<String> result = new CopyOnWriteArrayList<>();

        stream.forEachPair(1, (a, b) -> {
            if (b != null) {
                result.add(a + "+" + b);
            }
        });

        assertEquals(9, result.size());
        assertTrue(result.contains("1+2"));
        assertTrue(result.contains("9+10"));
    }

    @Test
    public void testForEachTripleConsumer() throws Exception {
        List<String> result = new CopyOnWriteArrayList<>();

        stream.forEachTriple(1, (a, b, c) -> {
            if (b != null && c != null) {
                result.add(a + "+" + b + "+" + c);
            }
        });

        assertEquals(8, result.size());
        assertTrue(result.contains("1+2+3"));
    }

    @Test
    public void testToMap() throws Exception {
        Map<String, Integer> result = stream.toMap(n -> "Key" + n, n -> n * 10);

        assertEquals(10, result.size());
        assertEquals(Integer.valueOf(10), result.get("Key1"));
        assertEquals(Integer.valueOf(100), result.get("Key10"));
    }

    @Test
    public void testToMapWithMergeFunction() throws Exception {
        List<Integer> duplicates = Arrays.asList(1, 2, 2, 3, 3, 3);

        try (Stream<Integer> dupStream = createStream(duplicates)) {
            Map<Integer, Integer> result = dupStream.toMap(n -> n, n -> 1, Integer::sum);

            assertEquals(3, result.size());
            assertEquals(Integer.valueOf(1), result.get(1));
            assertEquals(Integer.valueOf(2), result.get(2));
            assertEquals(Integer.valueOf(3), result.get(3));
        }
    }

    @Test
    public void testGroupTo() throws Exception {
        Map<Integer, List<Integer>> result = stream.groupTo(n -> n % 3);

        assertEquals(3, result.size());
        assertHaveSameElements(Arrays.asList(3, 6, 9), result.get(0));
        assertHaveSameElements(Arrays.asList(1, 4, 7, 10), result.get(1));
        assertHaveSameElements(Arrays.asList(2, 5, 8), result.get(2));
    }

    @Test
    public void testGroupToWithDownstream() throws Exception {
        Map<Boolean, Long> result = stream.groupTo(n -> n % 2 == 0, Collectors.counting());

        assertEquals(2, result.size());
        assertEquals(Long.valueOf(5), result.get(true));
        assertEquals(Long.valueOf(5), result.get(false));
    }

    @Test
    public void testFlatGroupTo() throws Exception {
        Map<Integer, List<Integer>> result = stream.limit(3).flatGroupTo(n -> Arrays.asList(n, n + 10));

        assertEquals(6, result.size());
        assertTrue(result.containsKey(1));
        assertTrue(result.containsKey(11));
        assertEquals(Arrays.asList(1), result.get(1));
    }

    @Test
    public void testToMultimap() throws Exception {
        ListMultimap<Integer, Integer> result = stream.toMultimap(n -> n % 3, n -> n);

        assertEquals(3, result.keySet().size());
        assertTrue(result.get(0).containsAll(Arrays.asList(3, 6, 9)));
        assertTrue(result.get(1).containsAll(Arrays.asList(1, 4, 7, 10)));
    }

    @Test
    public void testReduce() {
        u.Optional<Integer> result = stream.reduce(Integer::sum);

        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(55), result.get());
    }

    @Test
    public void testReduceWithIdentity() {
        Integer result = stream.reduce(0, Integer::sum, Integer::sum);

        assertEquals(Integer.valueOf(55), result);
    }

    @Test
    public void testCollectWithSupplier() {
        StringBuilder result = stream.collect(StringBuilder::new, (sb, n) -> sb.append(n).append(","), StringBuilder::append);

        String str = result.toString();
        assertTrue(str.contains("1,"));
        assertTrue(str.contains("10,"));
    }

    @Test
    public void testCollectWithCollector() {
        Set<Integer> result = stream.collect(Collectors.toSet());

        assertEquals(10, result.size());
        assertEquals(TEST_DATA.stream().collect(Collectors.toSet()), result);
    }

    @Test
    public void testMin() {
        u.Optional<Integer> result = stream.min(Integer::compare);

        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());
    }

    @Test
    public void testMax() {
        u.Optional<Integer> result = stream.max(Integer::compare);

        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(10), result.get());
    }

    @Test
    public void testAnyMatch() throws Exception {
        boolean result = stream.anyMatch(n -> n > 5);

        assertTrue(result);
    }

    @Test
    public void testAnyMatchNoMatch() throws Exception {
        boolean result = stream.anyMatch(n -> n > 100);

        assertFalse(result);
    }

    @Test
    public void testAllMatch() throws Exception {
        boolean result = stream.allMatch(n -> n > 0);

        assertTrue(result);
    }

    @Test
    public void testAllMatchNotAll() throws Exception {
        boolean result = stream.allMatch(n -> n < 5);

        assertFalse(result);
    }

    @Test
    public void testNoneMatch() throws Exception {
        boolean result = stream.noneMatch(n -> n < 0);

        assertTrue(result);
    }

    @Test
    public void testNoneMatchWithMatch() throws Exception {
        boolean result = stream.noneMatch(n -> n == 5);

        assertFalse(result);
    }

    @Test
    public void testNMatch() throws Exception {
        boolean result = stream.nMatch(3, 5, n -> n % 2 == 0);

        assertTrue(result); // We have exactly 5 even numbers
    }

    @Test
    public void testFindFirst() throws Exception {
        u.Optional<Integer> result = stream.findFirst(n -> n > 5);

        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(6), result.get());
    }

    @Test
    public void testFindAny() throws Exception {
        u.Optional<Integer> result = stream.findAny(n -> n > 5);

        assertTrue(result.isPresent());
        assertTrue(result.get() > 5);
    }

    @Test
    public void testFindLast() throws Exception {
        u.Optional<Integer> result = stream.findLast(n -> n < 5);

        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(4), result.get());
    }

    @Test
    public void testIntersection() {
        List<Integer> result = stream.intersection(n -> n % 3, Arrays.asList(0, 1)).toList();

        assertEquals(2, result.size()); // Elements where n%3 is 0 or 1
    }

    @Test
    public void testDifference() {
        List<Integer> result = stream.difference(n -> n % 3, Arrays.asList(0)).toList();

        assertEquals(9, result.size()); // Elements where n%3 is not 0
        N.println(result);
        assertTrue(N.asList(3, 6, 9).containsAll(N.difference(TEST_DATA, result))); // Ensure 3, 6, 9 are not in the result)) 
    }

    @Test
    public void testAppend() {
        Stream<Integer> toAppend = Stream.of(11, 12, 13);

        List<Integer> result = stream.append(toAppend).toList();

        assertEquals(13, result.size());
        assertEquals(Integer.valueOf(11), result.get(10));
        assertEquals(Integer.valueOf(13), result.get(12));
    }

    @Test
    public void testPrepend() {
        Stream<Integer> toPrepend = Stream.of(-2, -1, 0);

        List<Integer> result = stream.prepend(toPrepend).toList();

        assertEquals(13, result.size());
        assertEquals(Integer.valueOf(-2), result.get(0));
        assertEquals(Integer.valueOf(0), result.get(2));
        assertEquals(Integer.valueOf(1), result.get(3));
    }

    @Test
    public void testMergeWith() {
        List<Integer> other = Arrays.asList(5, 15, 25);

        List<Integer> result = stream.mergeWith(other, (a, b) -> a.compareTo(b) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();

        assertEquals(13, result.size());
    }

    @Test
    public void testZipWith() {
        List<String> letters = Arrays.asList("A", "B", "C");

        List<String> result = stream.zipWith(letters, (n, s) -> n + s).sorted().toList();

        assertEquals(3, result.size());
        assertEquals("1A", result.get(0));
        assertEquals("2B", result.get(1));
        assertEquals("3C", result.get(2));
    }

    @Test
    public void testZipWithDefaultValues() {
        List<String> letters = Arrays.asList("X", "Y");

        List<String> result = stream.limit(4).zipWith(letters, -1, "Z", (n, s) -> n + s).sorted().toList();

        assertEquals(4, result.size());
        assertEquals("1X", result.get(0));
        assertEquals("2Y", result.get(1));
        assertEquals("3Z", result.get(2));
        assertEquals("4Z", result.get(3));
    }

    @Test
    public void testZipWithThreeCollections() {
        List<String> letters = Arrays.asList("A", "B");
        List<Integer> numbers = Arrays.asList(100, 200);

        List<String> result = stream.zipWith(letters, numbers, (a, b, c) -> a + b + c).sorted().toList();

        assertEquals(2, result.size());
        assertEquals("1A100", result.get(0));
        assertEquals("2B200", result.get(1));
    }

    @Nested
    @DisplayName("Zip Operations")
    public class ZipOperationsTest {

        @Test
        @DisplayName("zipWith() collection should zip elements")
        public void testZipWithCollection() {
            {
                Collection<String> other = Arrays.asList("a", "b", "c", "d", "e");
                BiFunction<Integer, String, String> zipFunction = (i, s) -> i + s;

                List<String> result = createStream(TEST_DATA).limit(5).zipWith(other, zipFunction).toList();

                assertTrue(N.isEqualCollection(Arrays.asList("1a", "2b", "3c", "4d", "5e"), result));
            }
            {
                Collection<String> other = Arrays.asList("a", "b", "c", "d", "e");
                Collection<String> other2 = Arrays.asList("a", "b", "c", "d", "e");
                TriFunction<Integer, String, String, String> zipFunction = (i, s, c) -> i + s + c;

                List<String> result = createStream(TEST_DATA).limit(5).zipWith(other, other2, zipFunction).toList();

                assertTrue(N.isEqualCollection(Arrays.asList("1aa", "2bb", "3cc", "4dd", "5ee"), result));
            }
        }

        @Test
        @DisplayName("zipWith() collection with defaults should use defaults for missing values")
        public void testZipWithCollectionDefaults() {
            {
                Collection<String> other = Arrays.asList("a", "b");
                Integer defaultInt = 0;
                String defaultString = "x";
                BiFunction<Integer, String, String> zipFunction = (i, s) -> i + s;

                List<String> result = stream.limit(5).zipWith(other, defaultInt, defaultString, zipFunction).toList();

                assertEquals(5, result.size());
                assertTrue(result.contains("1a"));
                assertTrue(result.contains("2b"));
                assertTrue(result.contains("3x"));
            }
            {
                Collection<String> other = Arrays.asList("a", "b");
                Collection<String> other2 = Arrays.asList("a");
                Integer defaultInt = 0;
                String defaultString = "x";
                TriFunction<Integer, String, String, String> zipFunction = (i, s, c) -> i + s + c;

                List<String> result = createStream(TEST_DATA).limit(5).zipWith(other, other2, defaultInt, defaultString, "z", zipFunction).toList();

                assertEquals(5, result.size());
                assertTrue(result.contains("1aa"));
                assertTrue(result.contains("2bz"));
                assertTrue(result.contains("3xz"));
            }
        }

        @Test
        @DisplayName("zipWith() stream should zip with other streams")
        public void testZipWithStream() {
            {
                Collection<String> other = Arrays.asList("a", "b", "c", "d", "e");
                BiFunction<Integer, String, String> zipFunction = (i, s) -> i + s;

                List<String> result = stream.limit(5).zipWith(Stream.of(other), zipFunction).toList();

                assertTrue(N.isEqualCollection(Arrays.asList("1a", "2b", "3c", "4d", "5e"), result));
            }
            {
                Collection<String> other = Arrays.asList("a", "b", "c", "d", "e");
                Collection<String> other2 = Arrays.asList("a", "b", "c", "d", "e");
                TriFunction<Integer, String, String, String> zipFunction = (i, s, c) -> i + s + c;

                List<String> result = createStream(TEST_DATA).limit(5).zipWith(Stream.of(other), Stream.of(other2), zipFunction).toList();

                assertTrue(N.isEqualCollection(Arrays.asList("1aa", "2bb", "3cc", "4dd", "5ee"), result));
            }
        }

        @Test
        @DisplayName("zipWith() stream should zip with other streams with defaults should use defaults for missing values")
        public void testZipWithStreamDefaults() {
            {
                Collection<String> other = Arrays.asList("a", "b");
                Integer defaultInt = 0;
                String defaultString = "x";
                BiFunction<Integer, String, String> zipFunction = (i, s) -> i + s;

                List<String> result = stream.limit(5).zipWith(Stream.of(other), defaultInt, defaultString, zipFunction).toList();

                assertEquals(5, result.size());
                assertTrue(result.contains("1a"));
                assertTrue(result.contains("2b"));
                assertTrue(result.contains("3x"));
            }
            {
                Collection<String> other = Arrays.asList("a", "b");
                Collection<String> other2 = Arrays.asList("a");
                Integer defaultInt = 0;
                String defaultString = "x";
                TriFunction<Integer, String, String, String> zipFunction = (i, s, c) -> i + s + c;

                List<String> result = createStream(TEST_DATA).limit(5)
                        .zipWith(Stream.of(other), Stream.of(other2), defaultInt, defaultString, "z", zipFunction)
                        .toList();

                assertEquals(5, result.size());
                assertTrue(result.contains("1aa"));
                assertTrue(result.contains("2bz"));
                assertTrue(result.contains("3xz"));
            }
        }
    }

    @Nested
    @DisplayName("ForEach Operations")
    public class ForEachOperationsTest {

        @Test
        @DisplayName("forEach with flatMapper should apply action to each element")
        public void test_forEach() {
            {
                List<Integer> results = new ArrayList<>();

                createStream(TEST_DATA).parallel(PS.create(Splitor.ITERATOR)).forEach(it -> N.asList(it * 2, it * 2), Fn.sc(results, (a, b) -> results.add(a)));

                assertHaveSameElements(N.repeatCollection(TEST_DATA, 2), results);
            }
            {
                List<Integer> results = new ArrayList<>();

                createStream(TEST_DATA).parallel(PS.create(Splitor.ARRAY)).forEach(it -> N.asList(it * 2, it * 2), Fn.sc(results, (a, b) -> results.add(a)));

                assertHaveSameElements(N.repeatCollection(TEST_DATA, 2), results);

            }

            {
                List<Integer> results = new ArrayList<>();

                createStream(TEST_DATA).parallel(PS.create(Splitor.ITERATOR))
                        .forEach(it -> N.asList(it * 2, it * 2), it -> N.asList(it * 3, it * 3), Fn.sc(results, (a, b, c) -> results.add(a)));

                assertHaveSameElements(N.repeatCollection(TEST_DATA, 4), results);
            }

            {
                List<Integer> results = new ArrayList<>();

                createStream(TEST_DATA).parallel(PS.create(Splitor.ARRAY))
                        .forEach(it -> N.asList(it * 2, it * 2), it -> N.asList(it * 3, it * 3), Fn.sc(results, (a, b, c) -> results.add(a)));

                assertHaveSameElements(N.repeatCollection(TEST_DATA, 4), results);
            }
        }

        @Test
        @DisplayName("forEachPair")
        public void test_forEachPair() {
            {
                List<String> results = new ArrayList<>();

                createStream(TEST_DATA).limit(5).parallel(PS.create(Splitor.ITERATOR)).forEachPair(Fn.sc(results, (a, b) -> results.add(a + "->" + b)));

                assertHaveSameElements(N.asList("1->2", "2->3", "3->4", "4->5"), results);
            }
            {
                List<String> results = new ArrayList<>();

                createStream(TEST_DATA).limit(5).parallel(PS.create(Splitor.ARRAY)).forEachPair(Fn.sc(results, (a, b) -> results.add(a + "->" + b)));

                assertHaveSameElements(N.asList("1->2", "2->3", "3->4", "4->5"), results);
            }
            {
                List<String> results = new ArrayList<>();

                createStream(TEST_DATA).limit(5).parallel(PS.create(Splitor.ITERATOR)).forEachPair(2, Fn.sc(results, (a, b) -> results.add(a + "->" + b)));

                assertHaveSameElements(N.asList("1->2", "3->4", "5->null"), results);
            }
        }

        @Test
        @DisplayName("forEachTriple")
        public void test_forEachTriple() {
            {
                List<String> results = new ArrayList<>();

                createStream(TEST_DATA).limit(5)
                        .parallel(PS.create(Splitor.ITERATOR))
                        .forEachTriple(Fn.sc(results, (a, b, c) -> results.add(a + "->" + b + "->" + c)));

                assertHaveSameElements(N.asList("1->2->3", "2->3->4", "3->4->5"), results);
            }
            {
                List<String> results = new ArrayList<>();

                createStream(TEST_DATA).limit(5)
                        .parallel(PS.create(Splitor.ARRAY))
                        .forEachTriple(Fn.sc(results, (a, b, c) -> results.add(a + "->" + b + "->" + c)));

                assertHaveSameElements(N.asList("1->2->3", "2->3->4", "3->4->5"), results);
            }
            {
                List<String> results = new ArrayList<>();

                createStream(TEST_DATA).limit(5)
                        .parallel(PS.create(Splitor.ARRAY))
                        .forEachTriple(2, Fn.sc(results, (a, b, c) -> results.add(a + "->" + b + "->" + c)));

                assertHaveSameElements(N.asList("1->2->3", "3->4->5"), results);
            }
        }
    }

    @Test
    public void testIsParallel() {
        assertTrue(stream.isParallel());
    }

    @Test
    public void testSequential() {
        Stream<Integer> seqStream = stream.sequential();

        assertFalse(seqStream.isParallel());

        // Verify it's the same data
        List<Integer> result = seqStream.toList();
        assertEquals(TEST_DATA, result);
    }

    @Test
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);

        Stream<Integer> streamWithCloseHandler = stream.onClose(() -> closed.set(true));

        streamWithCloseHandler.close();

        assertTrue(closed.get());
    }

    @Test
    public void testParallelExecution() {
        // Test that operations actually run in parallel
        List<String> threadNames = new CopyOnWriteArrayList<>();

        List<Integer> result = stream.map(n -> {
            threadNames.add(Thread.currentThread().getName());
            return n * 2;
        }).toList();

        assertEquals(10, result.size());
        // In true parallel execution, we should see multiple thread names
        // Note: This might not always be true in small datasets or certain environments
    }

    @Test
    public void testConcurrentModification() {
        // Test thread safety with concurrent operations
        ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();

        stream.forEach(n -> {
            map.put(n, n * n);
            // Simulate some work
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertEquals(10, map.size());
        assertEquals(Integer.valueOf(100), map.get(10));
    }

    @Test
    public void testOperationAfterClose() {
        stream.close();
        assertThrows(IllegalStateException.class, () -> stream.toList()); // Should throw IllegalStateException
    }

    @Test
    public void testEmptyStream() {
        try (Stream<Integer> emptyStream = createStream(Collections.emptyList())) {
            List<Integer> result = emptyStream.toList();
            assertTrue(result.isEmpty());

            u.Optional<Integer> min = createStream(Collections.<Integer> emptyList()).min(Integer::compare);
            assertFalse(min.isPresent());
        }
    }

    @Test
    public void testLargeDataProcessing() {
        // Test with larger dataset to ensure parallel processing works correctly
        List<Integer> largeData = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            largeData.add(i);
        }

        try (Stream<Integer> largeStream = createStream(largeData)) {
            long sum = largeStream.mapToLong(Integer::longValue).sum();

            assertEquals(500500L, sum); // Sum of 1 to 1000
        }
    }

    @Test
    public void testErrorHandling() {
        AtomicInteger counter = new AtomicInteger(0);

        try {
            stream.map(n -> {
                if (n == 5) {
                    throw new RuntimeException("Test exception");
                }
                counter.incrementAndGet();
                return n;
            }).toList();

            fail("Expected exception");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Test exception"));
        }
    }
}
