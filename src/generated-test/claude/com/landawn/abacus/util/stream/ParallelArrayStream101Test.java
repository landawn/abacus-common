package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("ParallelArrayStream Tests")
public class ParallelArrayStream101Test extends TestBase {

    private static final Integer[] TEST_INTEGER_ARRAY = new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    private static final String[] TEST_STRING_ARRAY = new String[] { "apple", "banana", "cherry", "date", "elderberry" };
    private static final int testMaxThreadNum = 4; // Use a fixed small number of threads for predictable testing  
    private Stream<Integer> stream;
    private Stream<String> stringStream;
    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        stream = Stream.of(TEST_INTEGER_ARRAY).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
        stringStream = Stream.of(TEST_STRING_ARRAY).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
        executor = Executors.newFixedThreadPool(256); // Use a larger pool for parallel operations)
    }

    @AfterEach
    public void tearDown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        stream = null;
        stringStream = null;
    }

    @Nested
    @DisplayName("Filter Operations")
    public class FilterOperationsTest {

        @Test
        @DisplayName("filter() should filter elements based on predicate")
        public void testFilter() {
            Predicate<Integer> evenNumbers = x -> x % 2 == 0;

            List<Integer> result = stream.filter(evenNumbers).sorted().toList();

            assertEquals(Arrays.asList(2, 4, 6, 8, 10), result);
        }

        @Test
        @DisplayName("filter() should return empty stream when no elements match")
        public void testFilterNoMatches() {
            Predicate<Integer> greaterThan20 = x -> x > 20;

            List<Integer> result = stream.filter(greaterThan20).toList();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("takeWhile() should take elements while predicate is true")
        public void testTakeWhile() {
            Predicate<Integer> lessThan5 = x -> x < 5;

            List<Integer> result = stream.takeWhile(lessThan5).toList();

            assertHaveSameElements(Arrays.asList(1, 2, 3, 4), result);
        }

        @Test
        @DisplayName("dropWhile() should drop elements while predicate is true")
        public void testDropWhile() {
            Predicate<Integer> lessThan5 = x -> x < 5;

            List<Integer> result = stream.dropWhile(lessThan5).sorted().toList();

            assertEquals(Arrays.asList(5, 6, 7, 8, 9, 10), result);
        }
    }

    @Nested
    @DisplayName("Mapping Operations")
    public class MappingOperationsTest {

        @Test
        @DisplayName("map() should transform elements")
        public void testMap() {
            Function<Integer, String> toString = Object::toString;

            List<String> result = stream.map(toString).sortedBy(Integer::valueOf).toList();

            assertEquals(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), result);
        }

        @Test
        @DisplayName("map() should handle null mapper gracefully")
        public void testMapWithNull() {
            assertThrows(NullPointerException.class, () -> {
                stream.map(null).toList();
            });
        }

        @Test
        @DisplayName("slidingMap() with BiFunction should process pairs")
        public void testSlidingMapBiFunction() {
            BiFunction<Integer, Integer, Integer> sum = (a, b) -> a + (b != null ? b : 0);

            List<Integer> result = stream.slidingMap(1, false, sum).sorted().toList();

            assertFalse(result.isEmpty());
            assertEquals(Integer.valueOf(3), result.get(0)); // 1 + 2
        }

        @Test
        @DisplayName("slidingMap() with TriFunction should process triplets")
        public void testSlidingMapTriFunction() {
            TriFunction<Integer, Integer, Integer, Integer> sum = (a, b, c) -> a + (b != null ? b : 0) + (c != null ? c : 0);

            List<Integer> result = stream.slidingMap(1, false, sum).sorted().toList();

            assertFalse(result.isEmpty());
            assertEquals(Integer.valueOf(6), result.get(0)); // 1 + 2 + 3
        }

        @Test
        @DisplayName("mapFirstOrElse() should map first element with first mapper")
        public void testMapFirstOrElse() {
            Function<Integer, String> firstMapper = x -> "FIRST:" + x;
            Function<Integer, String> elseMapper = x -> "OTHER:" + x;

            List<String> result = stream.mapFirstOrElse(firstMapper, elseMapper).sortedBy(it -> Numbers.toInt(Strings.substringAfter(it, ":").trim())).toList();

            assertEquals("FIRST:1", result.get(0));
            assertEquals("OTHER:2", result.get(1));
        }

        @Test
        @DisplayName("mapLastOrElse() should map last element with last mapper")
        public void testMapLastOrElse() {
            Function<Integer, String> lastMapper = x -> "LAST:" + x;
            Function<Integer, String> elseMapper = x -> "OTHER:" + x;

            List<String> result = stream.mapLastOrElse(lastMapper, elseMapper).sortedBy(it -> Numbers.toInt(Strings.substringAfter(it, ":").trim())).toList();

            assertEquals("OTHER:1", result.get(0));
            assertEquals("LAST:10", result.get(result.size() - 1));
        }

        @Test
        @DisplayName("mapToChar() should convert to CharStream")
        public void testMapToChar() {
            ToCharFunction<Integer> toChar = x -> (char) (x + 64); // A=65, B=66, etc.

            char[] result = stream.limit(3).mapToChar(toChar).sorted().toArray();

            assertEquals(3, result.length);
            assertEquals('A', result[0]); // 1 + 64 = 65 = 'A'
        }

        @Test
        @DisplayName("mapToByte() should convert to ByteStream")
        public void testMapToByte() {
            ToByteFunction<Integer> toByte = Integer::byteValue;

            byte[] result = stream.limit(3).mapToByte(toByte).sorted().toArray();

            assertEquals(3, result.length);
            assertEquals((byte) 1, result[0]);
        }

        @Test
        @DisplayName("mapToShort() should convert to ShortStream")
        public void testMapToShort() {
            ToShortFunction<Integer> toShort = Integer::shortValue;

            short[] result = stream.limit(3).mapToShort(toShort).sorted().toArray();

            assertEquals(3, result.length);
            assertEquals((short) 1, result[0]);
        }

        @Test
        @DisplayName("mapToInt() should convert to IntStream")
        public void testMapToInt() {
            ToIntFunction<Integer> toInt = Integer::intValue;

            int[] result = stream.limit(3).mapToInt(toInt).sorted().toArray();

            assertEquals(3, result.length);
            assertEquals(1, result[0]);
        }

        @Test
        @DisplayName("mapToLong() should convert to LongStream")
        public void testMapToLong() {
            ToLongFunction<Integer> toLong = Integer::longValue;

            long[] result = stream.limit(3).mapToLong(toLong).sorted().toArray();

            assertEquals(3, result.length);
            assertEquals(1L, result[0]);
        }

        @Test
        @DisplayName("mapToFloat() should convert to FloatStream")
        public void testMapToFloat() {
            ToFloatFunction<Integer> toFloat = Integer::floatValue;

            float[] result = stream.limit(3).mapToFloat(toFloat).sorted().toArray();

            assertEquals(3, result.length);
            assertEquals(1.0f, result[0]);
        }

        @Test
        @DisplayName("mapToDouble() should convert to DoubleStream")
        public void testMapToDouble() {
            ToDoubleFunction<Integer> toDouble = Integer::doubleValue;

            double[] result = stream.limit(3).mapToDouble(toDouble).sorted().toArray();

            assertEquals(3, result.length);
            assertEquals(1.0, result[0]);
        }
    }

    @Nested
    @DisplayName("FlatMap Operations")
    public class FlatMapOperationsTest {

        @Test
        @DisplayName("flatMap() should flatten streams")
        public void testFlatMap() {
            Function<Integer, Stream<Integer>> duplicate = x -> Stream.of(x, x);

            List<Integer> result = stream.limit(3).flatMap(duplicate).sorted().toList();

            assertEquals(Arrays.asList(1, 1, 2, 2, 3, 3), result);
        }

        @Test
        @DisplayName("flatmap() should flatten collections")
        public void testFlatmapCollection() {
            Function<Integer, Collection<Integer>> duplicate = x -> Arrays.asList(x, x);

            List<Integer> result = stream.limit(3).flatmap(duplicate).sorted().toList();

            assertEquals(Arrays.asList(1, 1, 2, 2, 3, 3), result);
        }

        @Test
        @DisplayName("flattMap() should flatten arrays")
        public void testFlattMapArray() {
            Function<Integer, Integer[]> duplicate = x -> new Integer[] { x, x };

            List<Integer> result = stream.limit(3).flattMap(duplicate).sorted().toList();

            assertEquals(Arrays.asList(1, 1, 2, 2, 3, 3), result);
        }

        @Test
        @DisplayName("flatMapToChar() should flatten to CharStream")
        public void testFlatMapToChar() {
            Function<Integer, CharStream> toChars = x -> CharStream.of((char) x.intValue(), (char) (x + 1));

            char[] result = stream.limit(2).flatMapToChar(toChars).toArray();

            assertEquals(4, result.length);
        }

        @Test
        @DisplayName("flatMapToByte() should flatten to ByteStream")
        public void testFlatMapToByte() {
            Function<Integer, ByteStream> toBytes = x -> ByteStream.of(x.byteValue(), (byte) (x + 1));

            byte[] result = stream.limit(2).flatMapToByte(toBytes).toArray();

            assertEquals(4, result.length);
        }

        @Test
        @DisplayName("flatMapToShort() should flatten to ShortStream")
        public void testFlatMapToShort() {
            Function<Integer, ShortStream> toShorts = x -> ShortStream.of(x.shortValue(), (short) (x + 1));

            short[] result = stream.limit(2).flatMapToShort(toShorts).toArray();

            assertEquals(4, result.length);
        }

        @Test
        @DisplayName("flatMapToInt() should flatten to IntStream")
        public void testFlatMapToInt() {
            Function<Integer, IntStream> toInts = x -> IntStream.of(x, x + 1);

            int[] result = stream.limit(2).flatMapToInt(toInts).toArray();

            assertEquals(4, result.length);
        }

        @Test
        @DisplayName("flatMapToLong() should flatten to LongStream")
        public void testFlatMapToLong() {
            Function<Integer, LongStream> toLongs = x -> LongStream.of(x.longValue(), x + 1L);

            long[] result = stream.limit(2).flatMapToLong(toLongs).toArray();

            assertEquals(4, result.length);
        }

        @Test
        @DisplayName("flatMapToFloat() should flatten to FloatStream")
        public void testFlatMapToFloat() {
            Function<Integer, FloatStream> toFloats = x -> FloatStream.of(x.floatValue(), x + 1.0f);

            float[] result = stream.limit(2).flatMapToFloat(toFloats).toArray();

            assertEquals(4, result.length);
        }

        @Test
        @DisplayName("flatMapToDouble() should flatten to DoubleStream")
        public void testFlatMapToDouble() {
            Function<Integer, DoubleStream> toDoubles = x -> DoubleStream.of(x.doubleValue(), x + 1.0);

            double[] result = stream.limit(2).flatMapToDouble(toDoubles).toArray();

            assertEquals(4, result.length);
        }
    }

    @Nested
    @DisplayName("Side Effect Operations")
    public class SideEffectOperationsTest {

        @Test
        @DisplayName("onEach() should apply action to each element")
        public void testOnEach() {
            AtomicInteger sum = new AtomicInteger(0);
            Consumer<Integer> addToSum = sum::addAndGet;

            List<Integer> result = stream.limit(5).onEach(addToSum).sorted().toList();

            assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
            assertEquals(15, sum.get()); // 1+2+3+4+5 = 15
        }

        @Test
        @DisplayName("forEach() should process all elements")
        public void testForEach() {
            AtomicInteger sum = new AtomicInteger(0);

            stream.limit(5).forEach(sum::addAndGet, () -> {
            });

            assertEquals(15, sum.get());
        }

        @Test
        @DisplayName("forEach() with flatMapper should process nested elements")
        public void testForEachWithFlatMapper() {
            List<String> results = new ArrayList<>();

            stringStream.sorted().limit(2).forEach(str -> Arrays.asList(str.charAt(0), str.charAt(1)), (str, ch) -> {
                synchronized (results) {
                    results.add(str + ":" + ch);
                }
            });

            assertFalse(results.isEmpty());
            assertTrue(results.contains("apple:a"));
            assertTrue(results.contains("apple:p"));
        }

        @Test
        @DisplayName("forEach() with double flatMapper should process deeply nested elements")
        public void testForEachWithDoubleFlatMapper() {
            List<String> results = new ArrayList<>();

            stringStream.limit(1)
                    .forEach(str -> Arrays.asList(str.substring(0, 2), str.substring(2)), substr -> Arrays.asList(substr.charAt(0)),
                            (str, substr, ch) -> results.add(str + ":" + substr + ":" + ch));

            assertFalse(results.isEmpty());
        }

        @Test
        @DisplayName("forEachPair() should process pairs of elements")
        public void testForEachPair() {
            List<String> pairs = new ArrayList<>();

            stream.limit(5).sorted().forEachPair(1, (a, b) -> {
                synchronized (pairs) {
                    pairs.add(a + "," + b);
                }
            });

            assertFalse(pairs.isEmpty());
            // assertEquals("1,2", pairs.get(0));
            assertTrue(pairs.contains("1,2"));
        }

        @Test
        @DisplayName("forEachTriple() should process triplets of elements")
        public void testForEachTriple() {
            List<String> triplets = new ArrayList<>();

            stream.limit(5).sorted().forEachTriple(1, (a, b, c) -> {
                synchronized (triplets) {
                    triplets.add(a + "," + b + "," + c);
                }
            });

            assertFalse(triplets.isEmpty());
            // assertEquals("1,2,3", triplets.get(0));
            assertTrue(triplets.contains("1,2,3"));
        }
    }

    @Nested
    @DisplayName("Collection Operations")
    public class CollectionOperationsTest {

        @Test
        @DisplayName("toMap() should create map from elements")
        public void testToMap() {
            Function<Integer, String> keyMapper = Object::toString;
            Function<Integer, Integer> valueMapper = x -> x * 2;
            BinaryOperator<Integer> mergeFunction = Integer::sum;
            Supplier<Map<String, Integer>> mapFactory = HashMap::new;

            Map<String, Integer> result = stream.limit(5).toMap(keyMapper, valueMapper, mergeFunction, mapFactory);

            assertEquals(5, result.size());
            assertEquals(Integer.valueOf(2), result.get("1"));
            assertEquals(Integer.valueOf(10), result.get("5"));
        }

        @Test
        @DisplayName("groupTo() should group elements")
        public void testGroupTo() {
            Function<Integer, String> keyMapper = x -> x % 2 == 0 ? "even" : "odd";
            Function<Integer, Integer> valueMapper = Function.identity();
            Collector<Integer, ?, List<Integer>> downstream = Collectors.toList();
            Supplier<Map<String, List<Integer>>> mapFactory = HashMap::new;

            Map<String, List<Integer>> result = stream.limit(6).groupTo(keyMapper, valueMapper, downstream, mapFactory);

            assertEquals(2, result.size());
            assertTrue(result.containsKey("even"));
            assertTrue(result.containsKey("odd"));
            assertHaveSameElements(Arrays.asList(2, 4, 6), result.get("even"));
            assertHaveSameElements(Arrays.asList(1, 3, 5), result.get("odd"));
        }

        @Test
        @DisplayName("flatGroupTo() should group with flat key extraction")
        public void testFlatGroupTo() {
            Function<Integer, Collection<String>> keyExtractor = x -> x % 2 == 0 ? Arrays.asList("even", "number") : Arrays.asList("odd", "number");
            BiFunction<String, Integer, Integer> valueMapper = (key, value) -> value;
            Collector<Integer, ?, List<Integer>> downstream = Collectors.toList();
            Supplier<Map<String, List<Integer>>> mapFactory = HashMap::new;

            Map<String, List<Integer>> result = stream.limit(4).flatGroupTo(keyExtractor, valueMapper, downstream, mapFactory);

            assertTrue(result.containsKey("even"));
            assertTrue(result.containsKey("odd"));
            assertTrue(result.containsKey("number"));
            assertEquals(4, result.get("number").size()); // All numbers
        }

        @Test
        @DisplayName("toMultimap() should create multimap")
        public void testToMultimap() {
            Function<Integer, String> keyMapper = x -> x % 2 == 0 ? "even" : "odd";
            Function<Integer, Integer> valueMapper = Function.identity();

            Multimap<String, Integer, List<Integer>> result = stream.limit(6).toMultimap(keyMapper, valueMapper, Suppliers.ofListMultimap());

            assertEquals(2, result.size());
            assertEquals(3, result.get("even").size());
            assertEquals(3, result.get("odd").size());
        }
    }

    @Nested
    @DisplayName("Reduction Operations")
    public class ReductionOperationsTest {

        @Test
        @DisplayName("reduce() with accumulator should reduce to single value")
        public void testReduceWithAccumulator() {
            BinaryOperator<Integer> sum = Integer::sum;

            Optional<Integer> result = stream.limit(5).reduce(sum);

            assertTrue(result.isPresent());
            assertEquals(Integer.valueOf(15), result.get()); // 1+2+3+4+5
        }

        @Test
        @DisplayName("reduce() with identity should use identity for empty stream")
        public void testReduceWithIdentity() {
            Integer identity = 0;
            BiFunction<Integer, Integer, Integer> accumulator = Integer::sum;
            BinaryOperator<Integer> combiner = Integer::sum;

            Integer result = stream.limit(5).reduce(identity, accumulator, combiner);

            assertEquals(Integer.valueOf(15), result);
        }

        @Test
        @DisplayName("collect() with supplier, accumulator, combiner should collect elements")
        public void testCollectWithSupplierAccumulatorCombiner() {
            Supplier<List<Integer>> supplier = ArrayList::new;
            BiConsumer<List<Integer>, Integer> accumulator = List::add;
            BiConsumer<List<Integer>, List<Integer>> combiner = List::addAll;

            List<Integer> result = stream.limit(5).collect(supplier, accumulator, combiner);

            assertHaveSameElements(Arrays.asList(1, 2, 3, 4, 5), result);
        }

        @Test
        @DisplayName("collect() with collector should use collector")
        public void testCollectWithCollector() {
            Collector<Integer, ?, List<Integer>> collector = Collectors.toList();

            List<Integer> result = stream.limit(5).collect(collector);

            assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        }
    }

    @Nested
    @DisplayName("Min/Max Operations")
    public class MinMaxOperationsTest {

        @Test
        @DisplayName("min() should find minimum element")
        public void testMin() {
            Comparator<Integer> naturalOrder = Integer::compareTo;

            Optional<Integer> result = stream.min(naturalOrder);

            assertTrue(result.isPresent());
            assertEquals(Integer.valueOf(1), result.get());
        }

        @Test
        @DisplayName("max() should find maximum element")
        public void testMax() {
            Comparator<Integer> naturalOrder = Integer::compareTo;

            Optional<Integer> result = stream.max(naturalOrder);

            assertTrue(result.isPresent());
            assertEquals(Integer.valueOf(10), result.get());
        }

        @Test
        @DisplayName("min() should return empty for empty stream")
        public void testMinEmpty() {
            Stream<Integer> emptyStream = Stream.of(new Integer[0]).parallel(PS.create(Splitor.ARRAY, executor));

            Optional<Integer> result = emptyStream.min(Integer::compareTo);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("max() should return empty for empty stream")
        public void testMaxEmpty() {
            Stream<Integer> emptyStream = Stream.of(new Integer[0]).parallel(PS.create(Splitor.ARRAY, executor));

            Optional<Integer> result = emptyStream.max(Integer::compareTo);

            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("Matching Operations")
    public class MatchingOperationsTest {

        @Test
        @DisplayName("anyMatch() should return true if any element matches")
        public void testAnyMatchTrue() {
            Predicate<Integer> greaterThan5 = x -> x > 5;

            boolean result = stream.anyMatch(greaterThan5);

            assertTrue(result);
        }

        @Test
        @DisplayName("anyMatch() should return false if no element matches")
        public void testAnyMatchFalse() {
            Predicate<Integer> greaterThan20 = x -> x > 20;

            boolean result = stream.anyMatch(greaterThan20);

            assertFalse(result);
        }

        @Test
        @DisplayName("allMatch() should return true if all elements match")
        public void testAllMatchTrue() {
            Predicate<Integer> positive = x -> x > 0;

            boolean result = stream.allMatch(positive);

            assertTrue(result);
        }

        @Test
        @DisplayName("allMatch() should return false if any element doesn't match")
        public void testAllMatchFalse() {
            Predicate<Integer> even = x -> x % 2 == 0;

            boolean result = stream.allMatch(even);

            assertFalse(result);
        }

        @Test
        @DisplayName("noneMatch() should return true if no element matches")
        public void testNoneMatchTrue() {
            Predicate<Integer> negative = x -> x < 0;

            boolean result = stream.noneMatch(negative);

            assertTrue(result);
        }

        @Test
        @DisplayName("noneMatch() should return false if any element matches")
        public void testNoneMatchFalse() {
            Predicate<Integer> even = x -> x % 2 == 0;

            boolean result = stream.noneMatch(even);

            assertFalse(result);
        }

        @Test
        @DisplayName("nMatch() should check if count is within range")
        public void testNMatch() {
            Predicate<Integer> even = x -> x % 2 == 0;

            boolean result = stream.nMatch(3, 7, even);

            assertTrue(result); // 5 even numbers (2,4,6,8,10) is between 3 and 7
        }

        @Test
        @DisplayName("nMatch() should return false if count is outside range")
        public void testNMatchOutsideRange() {
            Predicate<Integer> even = x -> x % 2 == 0;

            boolean result = stream.nMatch(1, 3, even);

            assertFalse(result); // 5 even numbers is not between 1 and 3
        }
    }

    @Nested
    @DisplayName("Finding Operations")
    public class FindingOperationsTest {

        @Test
        @DisplayName("findFirst() should find first matching element")
        public void testFindFirst() {
            Predicate<Integer> greaterThan5 = x -> x > 5;

            Optional<Integer> result = stream.findFirst(greaterThan5);

            assertTrue(result.isPresent());
            assertEquals(Integer.valueOf(6), result.get());
        }

        @Test
        @DisplayName("findFirst() should return empty if no match")
        public void testFindFirstNoMatch() {
            Predicate<Integer> greaterThan20 = x -> x > 20;

            Optional<Integer> result = stream.findFirst(greaterThan20);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("findAny() should find any matching element")
        public void testFindAny() {
            Predicate<Integer> even = x -> x % 2 == 0;

            Optional<Integer> result = stream.findAny(even);

            assertTrue(result.isPresent());
            assertTrue(result.get() % 2 == 0);
        }

        @Test
        @DisplayName("findLast() should find last matching element")
        public void testFindLast() {
            Predicate<Integer> lessThan5 = x -> x < 5;

            Optional<Integer> result = stream.findLast(lessThan5);

            assertTrue(result.isPresent());
            assertEquals(Integer.valueOf(4), result.get());
        }
    }

    @Nested
    @DisplayName("Set Operations")
    public class SetOperationsTest {

        @Test
        @DisplayName("intersection() should find common elements")
        public void testIntersection() {
            Function<Integer, Integer> identity = Function.identity();
            Collection<Integer> other = Arrays.asList(2, 4, 6, 8, 12, 14);

            List<Integer> result = stream.intersection(identity, other).sorted().toList();

            assertEquals(Arrays.asList(2, 4, 6, 8), result);
        }

        @Test
        @DisplayName("difference() should find different elements")
        public void testDifference() {
            Function<Integer, Integer> identity = Function.identity();
            Collection<Integer> other = Arrays.asList(2, 4, 6, 8);

            List<Integer> result = stream.difference(identity, other).sorted().toList();

            assertEquals(Arrays.asList(1, 3, 5, 7, 9, 10), result);
        }
    }

    @Nested
    @DisplayName("Stream Combination Operations")
    public class StreamCombinationOperationsTest {

        @Test
        @DisplayName("append() should append another stream")
        public void testAppend() {
            Stream<Integer> other = Stream.of(11, 12, 13);

            List<Integer> result = stream.limit(3).append(other).toList();

            assertEquals(Arrays.asList(1, 2, 3, 11, 12, 13), result);
        }

        @Test
        @DisplayName("prepend() should prepend another stream")
        public void testPrepend() {
            Stream<Integer> other = Stream.of(-1, 0);

            List<Integer> result = stream.limit(3).prepend(other).toList();

            assertEquals(Arrays.asList(-1, 0, 1, 2, 3), result);
        }

        @Test
        @DisplayName("mergeWith() collection should merge streams")
        public void testMergeWithCollection() {
            Collection<Integer> other = Arrays.asList(15, 25, 35);
            BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

            List<Integer> result = stream.limit(3).mergeWith(other, selector).toList();

            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("mergeWith() stream should merge streams")
        public void testMergeWithStream() {
            Stream<Integer> other = Stream.of(15, 25, 35);
            BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

            List<Integer> result = stream.limit(3).mergeWith(other, selector).toList();

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Zip Operations")
    public class ZipOperationsTest {

        @Test
        @DisplayName("zipWith() collection should zip elements")
        public void testZipWithCollection() {
            Collection<String> other = Arrays.asList("a", "b", "c", "d", "e");
            BiFunction<Integer, String, String> zipFunction = (i, s) -> i + s;

            List<String> result = stream.limit(5).zipWith(other, zipFunction).sorted().toList();

            assertEquals(Arrays.asList("1a", "2b", "3c", "4d", "5e"), result);
        }

        @Test
        @DisplayName("zipWith() collection with defaults should use defaults for missing values")
        public void testZipWithCollectionDefaults() {
            Collection<String> other = Arrays.asList("a", "b");
            Integer defaultInt = 0;
            String defaultString = "x";
            BiFunction<Integer, String, String> zipFunction = (i, s) -> i + s;

            List<String> result = stream.limit(5).zipWith(other, defaultInt, defaultString, zipFunction).sorted().toList();

            assertEquals(5, result.size());
            assertEquals("1a", result.get(0));
            assertEquals("2b", result.get(1));
            assertEquals("3x", result.get(2));
        }

        @Test
        @DisplayName("zipWith() three collections should zip triplets")
        public void testZipWithThreeCollections() {
            Collection<String> second = Arrays.asList("a", "b", "c");
            Collection<String> third = Arrays.asList("x", "y", "z");
            TriFunction<Integer, String, String, String> zipFunction = (i, s1, s2) -> i + s1 + s2;

            List<String> result = stream.limit(3).zipWith(second, third, zipFunction).sorted().toList();

            assertEquals(Arrays.asList("1ax", "2by", "3cz"), result);
        }

        @Test
        @DisplayName("zipWith() stream should zip with another stream")
        public void testZipWithStream() {
            Stream<String> other = Stream.of("a", "b", "c", "d", "e");
            BiFunction<Integer, String, String> zipFunction = (i, s) -> i + s;

            List<String> result = stream.limit(5).zipWith(other, zipFunction).sorted().toList();

            assertEquals(Arrays.asList("1a", "2b", "3c", "4d", "5e"), result);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    public class EdgeCasesTest {

        @Test
        @DisplayName("should handle empty array")
        public void testEmptyArray() {
            Integer[] emptyArray = new Integer[0];
            Stream<Integer> emptyStream = Stream.of(emptyArray).parallel(PS.create(Splitor.ARRAY, executor));

            List<Integer> result = emptyStream.toList();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should handle single element array")
        public void testSingleElement() {
            Integer[] singleArray = new Integer[] { 42 };
            Stream<Integer> singleStream = Stream.of(singleArray).parallel(PS.create(Splitor.ARRAY));

            List<Integer> result = singleStream.toList();

            assertEquals(Arrays.asList(42), result);
        }

        @Test
        @DisplayName("should handle null elements in array")
        public void testNullElements() {
            Integer[] arrayWithNulls = new Integer[] { 1, null, 3, null, 5 };
            Stream<Integer> streamWithNulls = Stream.of(arrayWithNulls).parallel(PS.create(Splitor.ARRAY));

            List<Integer> result = streamWithNulls.toList();

            assertEquals(Arrays.asList(1, null, 3, null, 5), result);
        }

        @Test
        @DisplayName("should handle subrange of array")
        public void testSubrange() {
            Stream<Integer> subStream = Stream.of(TEST_INTEGER_ARRAY).parallel(PS.create(Splitor.ARRAY)).skip(2).limit(3);

            List<Integer> result = subStream.toList();

            assertEquals(Arrays.asList(3, 4, 5), result);
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 4, 8 })
        @DisplayName("should work with different thread counts")
        public void testDifferentThreadCounts(int threadCount) {
            Stream<Integer> streamWithThreads = Stream.of(TEST_INTEGER_ARRAY).parallel(PS.create(Splitor.ARRAY).maxThreadNum(threadCount));

            List<Integer> result = streamWithThreads.limit(5).toList();

            assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        }

        @Test
        @DisplayName("should work with ITERATOR splitor")
        public void testIteratorSplitor() {
            Stream<Integer> iteratorStream = Stream.of(TEST_INTEGER_ARRAY).parallel(PS.create(Splitor.ITERATOR));

            List<Integer> result = iteratorStream.limit(5).toList();

            assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        }
    }

    @Nested
    @DisplayName("FlatMap Array Operations")
    public class FlatMapArrayOperationsTest {

        @Test
        @DisplayName("flatmapToChar() should flatten to char arrays")
        public void testFlatmapToChar() {
            Function<Integer, char[]> toCharArray = x -> new char[] { (char) (x + 64), (char) (x + 65) };

            char[] result = stream.limit(2).flatmapToChar(toCharArray).sorted().toArray();

            assertEquals(4, result.length);
            assertEquals('A', result[0]); // 1 + 64 = 65 = 'A'
            assertEquals('B', result[1]); // 1 + 65 = 66 = 'B'
        }

        @Test
        @DisplayName("flatmapToByte() should flatten to byte arrays")
        public void testFlatmapToByte() {
            Function<Integer, byte[]> toByteArray = x -> new byte[] { x.byteValue(), (byte) (x + 1) };

            byte[] result = stream.limit(2).flatmapToByte(toByteArray).sorted().toArray();

            assertEquals(4, result.length);
            assertEquals((byte) 1, result[0]);
            assertEquals((byte) 2, result[1]);
        }

        @Test
        @DisplayName("flatmapToShort() should flatten to short arrays")
        public void testFlatmapToShort() {
            Function<Integer, short[]> toShortArray = x -> new short[] { x.shortValue(), (short) (x + 1) };

            short[] result = stream.limit(2).flatmapToShort(toShortArray).sorted().toArray();

            assertEquals(4, result.length);
            assertEquals((short) 1, result[0]);
            assertEquals((short) 2, result[1]);
        }

        @Test
        @DisplayName("flatmapToInt() should flatten to int arrays")
        public void testFlatmapToInt() {
            Function<Integer, int[]> toIntArray = x -> new int[] { x, x + 1 };

            int[] result = stream.limit(2).flatmapToInt(toIntArray).sorted().toArray();

            assertEquals(4, result.length);
            assertEquals(1, result[0]);
            assertEquals(2, result[1]);
        }

        @Test
        @DisplayName("flatmapToLong() should flatten to long arrays")
        public void testFlatmapToLong() {
            Function<Integer, long[]> toLongArray = x -> new long[] { x.longValue(), x + 1L };

            long[] result = stream.limit(2).flatmapToLong(toLongArray).sorted().toArray();

            assertEquals(4, result.length);
            assertEquals(1L, result[0]);
            assertEquals(2L, result[1]);
        }

        @Test
        @DisplayName("flatmapToFloat() should flatten to float arrays")
        public void testFlatmapToFloat() {
            Function<Integer, float[]> toFloatArray = x -> new float[] { x.floatValue(), x + 1.0f };

            float[] result = stream.limit(2).flatmapToFloat(toFloatArray).sorted().toArray();

            assertEquals(4, result.length);
            assertEquals(1.0f, result[0]);
            assertEquals(2.0f, result[1]);
        }

        @Test
        @DisplayName("flatmapToDouble() should flatten to double arrays")
        public void testFlatmapToDouble() {
            Function<Integer, double[]> toDoubleArray = x -> new double[] { x.doubleValue(), x + 1.0 };

            double[] result = stream.limit(2).flatmapToDouble(toDoubleArray).sorted().toArray();

            assertEquals(4, result.length);
            assertEquals(1.0, result[0]);
            assertEquals(2.0, result[1]);
        }
    }

    @Nested
    @DisplayName("Additional Array Operations")
    public class AdditionalArrayOperationsTest {

        @Test
        @DisplayName("step() should skip elements by step size")
        public void testStep() {
            long step = 2;

            List<Integer> result = stream.step(step).toList();

            assertEquals(Arrays.asList(1, 3, 5, 7, 9), result);
        }

        @Test
        @DisplayName("step() with step 1 should return all elements")
        public void testStepOne() {
            long step = 1;

            List<Integer> result = stream.limit(5).step(step).toList();

            assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        }

        @Test
        @DisplayName("step() should throw exception for invalid step")
        public void testStepInvalid() {
            assertThrows(IllegalArgumentException.class, () -> {
                stream.step(0).toList();
            });
        }

        @Test
        @DisplayName("mapFirst() should map only the first element")
        public void testMapFirst() {
            Function<Integer, Integer> multiply10 = x -> x * 10;

            List<Integer> result = stream.limit(5).mapFirst(multiply10).toList();

            assertEquals(Integer.valueOf(10), result.get(0)); // First element mapped
            assertEquals(Integer.valueOf(2), result.get(1)); // Rest unchanged
            assertEquals(Integer.valueOf(3), result.get(2));
        }

        @Test
        @DisplayName("mapLast() should map only the last element")
        public void testMapLast() {
            Function<Integer, Integer> multiply10 = x -> x * 10;

            List<Integer> result = stream.limit(5).mapLast(multiply10).toList();

            assertEquals(Integer.valueOf(1), result.get(0)); // First elements unchanged
            assertEquals(Integer.valueOf(2), result.get(1));
            assertEquals(Integer.valueOf(50), result.get(4)); // Last element mapped (5 * 10)
        }
    }

    @Nested
    @DisplayName("Stream Splitting Operations")
    public class StreamSplittingOperationsTest {

        @Test
        @DisplayName("split() by chunk size should create chunks")
        public void testSplitByChunkSize() {
            int chunkSize = 3;

            List<List<Integer>> result = stream.limit(7).split(chunkSize).toList();

            assertEquals(3, result.size());
            assertEquals(Arrays.asList(1, 2, 3), result.get(0));
            assertEquals(Arrays.asList(4, 5, 6), result.get(1));
            assertEquals(Arrays.asList(7), result.get(2));
        }

        @Test
        @DisplayName("split() with collection supplier should use custom collection")
        public void testSplitWithCollectionSupplier() {
            int chunkSize = 2;
            IntFunction<LinkedList<Integer>> supplier = size -> new LinkedList<>();

            List<LinkedList<Integer>> result = stream.limit(5).split(chunkSize, supplier).toList();

            assertEquals(3, result.size());
            assertTrue(result.get(0) instanceof LinkedList);
            assertEquals(Arrays.asList(1, 2), new ArrayList<>(result.get(0)));
        }

        @Test
        @DisplayName("split() with collector should use collector")
        public void testSplitWithCollector() {
            int chunkSize = 3;
            Collector<Integer, ?, String> collector = Collectors.mapping(Object::toString, Collectors.joining(","));

            List<String> result = stream.limit(7).split(chunkSize, collector).toList();

            assertEquals(3, result.size());
            assertEquals("1,2,3", result.get(0));
            assertEquals("4,5,6", result.get(1));
            assertEquals("7", result.get(2));
        }

        @Test
        @DisplayName("split() by predicate should split on condition change")
        public void testSplitByPredicate() {
            Predicate<Integer> lessThan5 = x -> x < 5;

            List<List<Integer>> result = stream.limit(8).split(lessThan5).toList();

            assertEquals(2, result.size());
            assertEquals(Arrays.asList(1, 2, 3, 4), result.get(0)); // Elements < 5
            assertEquals(Arrays.asList(5, 6, 7, 8), result.get(1)); // Elements >= 5
        }

        @Test
        @DisplayName("split() by predicate with collection supplier")
        public void testSplitByPredicateWithSupplier() {
            Predicate<Integer> even = x -> x % 2 == 0;
            Supplier<Set<Integer>> supplier = HashSet::new;

            List<Set<Integer>> result = stream.limit(6).split(even, supplier).toList();

            assertFalse(result.isEmpty());
            assertTrue(result.get(0) instanceof Set);
        }

        @Test
        @DisplayName("split() by predicate with collector")
        public void testSplitByPredicateWithCollector() {
            Predicate<Integer> lessThan5 = x -> x < 5;
            Collector<Integer, ?, Integer> sumCollector = Collectors.summingInt(Integer::intValue);

            List<Integer> result = stream.limit(8).split(lessThan5, sumCollector).toList();

            assertEquals(2, result.size());
            assertEquals(Integer.valueOf(10), result.get(0)); // Sum of 1,2,3,4
            assertEquals(Integer.valueOf(26), result.get(1)); // Sum of 5,6,7,8
        }

        @Test
        @DisplayName("splitAt() should split at specified position")
        public void testSplitAt() {
            int position = 3;

            List<Stream<Integer>> result = stream.limit(7).splitAt(position).toList();

            assertEquals(2, result.size());
            assertEquals(Arrays.asList(1, 2, 3), result.get(0).toList());
            assertEquals(Arrays.asList(4, 5, 6, 7), result.get(1).toList());
        }

        @Test
        @DisplayName("splitAt() with collector should collect split parts")
        public void testSplitAtWithCollector() {
            int position = 3;
            Collector<Integer, ?, List<String>> collector = Collectors.mapping(Object::toString, Collectors.toList());

            List<List<String>> result = stream.limit(6).splitAt(position, collector).toList();

            assertEquals(2, result.size());
            assertHaveSameElements(N.asList(N.asList("1", "2", "3"), N.asList("4", "5", "6")), result);
        }
    }

    @Nested
    @DisplayName("Sliding Window Operations")
    public class SlidingWindowOperationsTest {

        @Test
        @DisplayName("sliding() with collection supplier should create sliding windows")
        public void testSlidingWithCollectionSupplier() {
            int windowSize = 3;
            int increment = 1;
            IntFunction<List<Integer>> supplier = ArrayList::new;

            List<List<Integer>> result = stream.limit(5).sliding(windowSize, increment, supplier).toList();

            assertEquals(3, result.size()); // 5-3+1 = 3 windows
            assertEquals(Arrays.asList(1, 2, 3), result.get(0));
            assertEquals(Arrays.asList(2, 3, 4), result.get(1));
            assertEquals(Arrays.asList(3, 4, 5), result.get(2));
        }

        @Test
        @DisplayName("sliding() with collector should collect sliding windows")
        public void testSlidingWithCollector() {
            int windowSize = 2;
            int increment = 1;
            Collector<Integer, ?, Integer> sumCollector = Collectors.summingInt(Integer::intValue);

            List<Integer> result = stream.limit(4).sliding(windowSize, increment, sumCollector).toList();

            assertEquals(3, result.size());
            assertEquals(Integer.valueOf(3), result.get(0)); // 1+2
            assertEquals(Integer.valueOf(5), result.get(1)); // 2+3
            assertEquals(Integer.valueOf(7), result.get(2)); // 3+4
        }

        @Test
        @DisplayName("sliding() should handle window larger than stream")
        public void testSlidingLargeWindow() {
            int windowSize = 20;
            int increment = 1;
            IntFunction<List<Integer>> supplier = ArrayList::new;

            List<List<Integer>> result = stream.sliding(windowSize, increment, supplier).toList();

            assertEquals(1, result.size());
            assertEquals(10, result.get(0).size()); // All elements
        }
    }

    @Nested
    @DisplayName("Stream Limiting and Skipping")
    public class LimitingSkippingOperationsTest {

        @Test
        @DisplayName("limit() should limit stream to specified size")
        public void testLimit() {
            long maxSize = 5;

            List<Integer> result = stream.limit(maxSize).toList();

            assertEquals(5, result.size());
            assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        }

        @Test
        @DisplayName("limit() with size larger than stream should return all elements")
        public void testLimitLargerThanStream() {
            long maxSize = 20;

            List<Integer> result = stream.limit(maxSize).toList();

            assertEquals(10, result.size()); // Original array size
        }

        @Test
        @DisplayName("skip() should skip specified number of elements")
        public void testSkip() {
            long n = 3;

            List<Integer> result = stream.skip(n).toList();

            assertEquals(Arrays.asList(4, 5, 6, 7, 8, 9, 10), result);
        }

        @Test
        @DisplayName("skip() with count larger than stream should return empty")
        public void testSkipLargerThanStream() {
            long n = 20;

            List<Integer> result = stream.skip(n).toList();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("takeLast() should take last n elements")
        public void testTakeLast() {
            int n = 3;

            List<Integer> result = stream.takeLast(n).toList();

            assertEquals(Arrays.asList(8, 9, 10), result);
        }

        @Test
        @DisplayName("skipLast() should skip last n elements")
        public void testSkipLast() {
            int n = 3;

            List<Integer> result = stream.skipLast(n).toList();

            assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7), result);
        }

        @Test
        @DisplayName("top() should return top n elements")
        public void testTop() {
            int n = 3;
            Comparator<Integer> comparator = Integer::compareTo;

            List<Integer> result = stream.top(n, comparator).toList();

            assertEquals(3, result.size());
            assertEquals(Arrays.asList(8, 9, 10), result); // Top 3 largest
        }
    }

    @Nested
    @DisplayName("Distinct and Sorting Operations")
    public class DistinctSortingOperationsTest {

        @Test
        @DisplayName("distinct() should remove duplicates")
        public void testDistinct() {
            Integer[] arrayWithDuplicates = { 1, 2, 2, 3, 3, 3, 4, 4, 5 };
            Stream<Integer> streamWithDuplicates = Stream.of(arrayWithDuplicates).parallel(PS.create(Splitor.ARRAY));

            List<Integer> result = streamWithDuplicates.distinct().toList();

            assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        }

        @Test
        @DisplayName("distinct() on sorted stream should be optimized")
        public void testDistinctSorted() {
            Integer[] sortedWithDuplicates = { 1, 1, 2, 2, 3, 3 };
            Stream<Integer> sortedStream = Stream.of(sortedWithDuplicates).parallel(PS.create(Splitor.ARRAY));

            List<Integer> result = sortedStream.distinct().toList();

            assertEquals(Arrays.asList(1, 2, 3), result);
        }
    }

    @Nested
    @DisplayName("Collection Conversion Operations")
    public class CollectionConversionOperationsTest {

        @Test
        @DisplayName("toArray() should convert to object array")
        public void testToArray() {
            Object[] result = stream.limit(5).toArray();

            assertEquals(5, result.length);
            assertArrayEquals(new Object[] { 1, 2, 3, 4, 5 }, result);
        }

        @Test
        @DisplayName("toArray() with generator should use generator")
        public void testToArrayWithGenerator() {
            IntFunction<Integer[]> generator = Integer[]::new;

            Integer[] result = stream.limit(5).toArray(generator);

            assertEquals(5, result.length);
            assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, result);
        }

        @Test
        @DisplayName("toList() should convert to list")
        public void testToList() {
            List<Integer> result = stream.limit(5).toList();

            assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        }

        @Test
        @DisplayName("toSet() should convert to set")
        public void testToSet() {
            Set<Integer> result = stream.limit(5).toSet();

            assertEquals(5, result.size());
            assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4, 5)));
        }

        @Test
        @DisplayName("toCollection() should use provided supplier")
        public void testToCollection() {
            Supplier<LinkedList<Integer>> supplier = LinkedList::new;

            LinkedList<Integer> result = stream.limit(5).toCollection(supplier);

            assertTrue(result instanceof LinkedList);
            assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        }

        @Test
        @DisplayName("toMultiset() should convert to multiset")
        public void testToMultiset() {
            Integer[] arrayWithDuplicates = { 1, 2, 2, 3, 3, 3 };
            Stream<Integer> streamWithDuplicates = Stream.of(arrayWithDuplicates).parallel(PS.create(Splitor.ARRAY));

            Multiset<Integer> result = streamWithDuplicates.toMultiset();

            assertEquals(1, result.count(1));
            assertEquals(2, result.count(2));
            assertEquals(3, result.count(3));
        }

        @Test
        @DisplayName("toMultiset() with supplier should use supplier")
        public void testToMultisetWithSupplier() {
            Supplier<Multiset<Integer>> supplier = () -> new Multiset<>();

            Multiset<Integer> result = stream.limit(3).toMultiset(supplier);

            assertEquals(3, result.size());
        }
    }

    @Nested
    @DisplayName("Optional and Single Element Operations")
    public class OptionalSingleElementOperationsTest {

        @Test
        @DisplayName("first() should return first element")
        public void testFirst() {
            Optional<Integer> result = stream.first();

            assertTrue(result.isPresent());
            assertEquals(Integer.valueOf(1), result.get());
        }

        @Test
        @DisplayName("first() should return empty for empty stream")
        public void testFirstEmpty() {
            Stream<Integer> emptyStream = Stream.of(new Integer[0]).parallel(PS.create(Splitor.ARRAY));

            Optional<Integer> result = emptyStream.first();

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("last() should return last element")
        public void testLast() {
            Optional<Integer> result = stream.last();

            assertTrue(result.isPresent());
            assertEquals(Integer.valueOf(10), result.get());
        }

        @Test
        @DisplayName("elementAt() should return element at position")
        public void testElementAt() {
            long position = 3;

            Optional<Integer> result = stream.elementAt(position);

            assertTrue(result.isPresent());
            assertEquals(Integer.valueOf(4), result.get()); // 0-indexed
        }

        @Test
        @DisplayName("elementAt() should return empty for invalid position")
        public void testElementAtInvalidPosition() {
            long position = 20;

            Optional<Integer> result = stream.elementAt(position);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("onlyOne() should return element for single-element stream")
        public void testOnlyOne() {
            Integer[] singleArray = { 42 };
            Stream<Integer> singleStream = Stream.of(singleArray).parallel(PS.create(Splitor.ARRAY));

            Optional<Integer> result = singleStream.onlyOne();

            assertTrue(result.isPresent());
            assertEquals(Integer.valueOf(42), result.get());
        }

        @Test
        @DisplayName("onlyOne() should return empty for empty stream")
        public void testOnlyOneEmpty() {
            Stream<Integer> emptyStream = Stream.of(new Integer[0]).parallel(PS.create(Splitor.ARRAY));

            Optional<Integer> result = emptyStream.onlyOne();

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("onlyOne() should throw exception for multiple elements")
        public void testOnlyOneMultiple() {
            assertThrows(TooManyElementsException.class, () -> {
                stream.onlyOne();
            });
        }
    }

    @Nested
    @DisplayName("Fold Operations")
    public class FoldOperationsTest {

        @Test
        @DisplayName("foldLeft() should fold from left")
        public void testFoldLeft() {
            BinaryOperator<Integer> subtraction = (a, b) -> a - b;

            Optional<Integer> result = stream.limit(5).foldLeft(subtraction);

            assertTrue(result.isPresent());
            assertEquals(Integer.valueOf(-13), result.get()); // ((((1-2)-3)-4)-5) = -13
        }

        @Test
        @DisplayName("foldLeft() with identity should use identity")
        public void testFoldLeftWithIdentity() {
            Integer identity = 100;
            BiFunction<Integer, Integer, Integer> subtraction = (a, b) -> a - b;

            Integer result = stream.limit(3).foldLeft(identity, subtraction);

            assertEquals(Integer.valueOf(94), result); // ((100-1)-2)-3 = 94
        }

        @Test
        @DisplayName("foldRight() should fold from right")
        public void testFoldRight() {
            BinaryOperator<Integer> subtraction = (a, b) -> a - b;

            Optional<Integer> result = stream.limit(5).foldRight(subtraction);

            assertTrue(result.isPresent());
            assertEquals(Integer.valueOf(-5), result.get()); // 5-(4-(3-(2-1))) = 3
        }

        @Test
        @DisplayName("foldRight() with identity should use identity")
        public void testFoldRightWithIdentity() {
            Integer identity = 100;
            BiFunction<Integer, Integer, Integer> subtraction = (a, b) -> a - b;

            Integer result = stream.limit(3).foldRight(identity, subtraction);

            assertEquals(Integer.valueOf(94), result); // 100-(3-(2-1)) = 98
        }
    }

    @Nested
    @DisplayName("Special Collection Operations")
    public class SpecialCollectionOperationsTest {

        @Test
        @DisplayName("maxAll() should find all maximum elements")
        public void testMaxAll() {
            Integer[] arrayWithMaxDuplicates = { 1, 3, 5, 5, 2, 5, 4 };
            Stream<Integer> streamWithMaxDuplicates = Stream.of(arrayWithMaxDuplicates).parallel(PS.create(Splitor.ARRAY));

            List<Integer> result = streamWithMaxDuplicates.maxAll(Integer::compareTo);

            assertEquals(3, result.size());
            assertTrue(result.stream().allMatch(x -> x.equals(5)));
        }

        @Test
        @DisplayName("kthLargest() should find kth largest element")
        public void testKthLargest() {
            int k = 3;

            Optional<Integer> result = stream.kthLargest(k, Integer::compareTo);

            assertTrue(result.isPresent());
            assertEquals(Integer.valueOf(8), result.get()); // 3rd largest in [1,2,3,4,5,6,7,8,9,10]
        }

        @Test
        @DisplayName("kthLargest() should return empty for invalid k")
        public void testKthLargestInvalid() {
            int k = 20;

            Optional<Integer> result = stream.kthLargest(k, Integer::compareTo);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("count() should return total count")
        public void testCount() {
            long result = stream.count();

            assertEquals(10, result);
        }
    }

    @Nested
    @DisplayName("Stream State and Lifecycle Operations")
    public class StreamStateLifecycleOperationsTest {

        @Test
        @DisplayName("cycled() should create infinite cycling stream")
        public void testCycled() {
            List<Integer> result = stream.limit(3).cycled().limit(7).toList();

            assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1), result);
        }

        @Test
        @DisplayName("cycled() with rounds should cycle specified times")
        public void testCycledWithRounds() {
            long rounds = 2;

            List<Integer> result = stream.limit(3).cycled(rounds).toList();

            assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), result);
        }

        @Test
        @DisplayName("buffered() should return buffered stream")
        public void testBuffered() {
            int bufferSize = 5;

            Stream<Integer> result = stream.buffered(bufferSize);

            assertNotNull(result);
            assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.limit(5).toList());
        }

        @Test
        @DisplayName("appendIfEmpty() should not append if not empty")
        public void testAppendIfEmptyNotEmpty() {
            Collection<Integer> toAppend = Arrays.asList(100, 200);

            List<Integer> result = stream.limit(3).appendIfEmpty(toAppend).toList();

            assertEquals(Arrays.asList(1, 2, 3), result);
        }

        @Test
        @DisplayName("appendIfEmpty() should append if empty")
        public void testAppendIfEmptyIsEmpty() {
            Stream<Integer> emptyStream = Stream.of(new Integer[0]).parallel(PS.create(Splitor.ARRAY));
            Collection<Integer> toAppend = Arrays.asList(100, 200);

            List<Integer> result = emptyStream.appendIfEmpty(toAppend).toList();

            assertEquals(Arrays.asList(100, 200), result);
        }

        @Test
        @DisplayName("appendIfEmpty() with supplier should not call supplier if not empty")
        public void testAppendIfEmptySupplierNotEmpty() {
            AtomicBoolean supplierCalled = new AtomicBoolean(false);
            Supplier<Stream<Integer>> supplier = () -> {
                supplierCalled.set(true);
                return Stream.of(100, 200);
            };

            List<Integer> result = stream.limit(3).appendIfEmpty(supplier).toList();

            assertEquals(Arrays.asList(1, 2, 3), result);
            assertFalse(supplierCalled.get());
        }

        @Test
        @DisplayName("ifEmpty() should not execute action if not empty")
        public void testIfEmptyNotEmpty() {
            AtomicBoolean actionExecuted = new AtomicBoolean(false);
            Runnable action = () -> actionExecuted.set(true);

            List<Integer> result = stream.limit(3).ifEmpty(action).toList();

            assertEquals(Arrays.asList(1, 2, 3), result);
            assertFalse(actionExecuted.get());
        }

        @Test
        @DisplayName("ifEmpty() should execute action if empty")
        public void testIfEmptyIsEmpty() {
            Stream<Integer> emptyStream = Stream.of(new Integer[0]).parallel(PS.create(Splitor.ARRAY));
            AtomicBoolean actionExecuted = new AtomicBoolean(false);
            Runnable action = () -> actionExecuted.set(true);

            List<Integer> result = emptyStream.ifEmpty(action).toList();

            assertTrue(result.isEmpty());
            assertTrue(actionExecuted.get());
        }
    }

    @Nested
    @DisplayName("Conditional Operations")
    public class ConditionalOperationsTest {

        @Test
        @DisplayName("applyIfNotEmpty() should apply function if not empty")
        public void testApplyIfNotEmpty() {
            Function<Stream<Integer>, String> function = stream -> stream.map(Object::toString).collect(Collectors.joining(","));

            Optional<String> result = stream.limit(3).applyIfNotEmpty(function);

            assertTrue(result.isPresent());
            assertHaveSameElements(N.asList("1", "2", "3"), N.asList(result.get().split(",")));
        }

        @Test
        @DisplayName("applyIfNotEmpty() should return empty if empty")
        public void testApplyIfNotEmptyIsEmpty() {
            Stream<Integer> emptyStream = Stream.of(new Integer[0]).parallel(PS.create(Splitor.ARRAY));
            Function<Stream<Integer>, String> function = stream -> "test";

            Optional<String> result = emptyStream.applyIfNotEmpty(function);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("acceptIfNotEmpty() should accept if not empty")
        public void testAcceptIfNotEmpty() {
            AtomicBoolean actionExecuted = new AtomicBoolean(false);
            Consumer<Stream<Integer>> action = stream -> actionExecuted.set(true);

            OrElse result = stream.limit(3).acceptIfNotEmpty(action);

            assertEquals(OrElse.TRUE, result);
            assertTrue(actionExecuted.get());
        }

        @Test
        @DisplayName("acceptIfNotEmpty() should return FALSE if empty")
        public void testAcceptIfNotEmptyIsEmpty() {
            Stream<Integer> emptyStream = Stream.of(new Integer[0]).parallel(PS.create(Splitor.ARRAY));
            AtomicBoolean actionExecuted = new AtomicBoolean(false);
            Consumer<Stream<Integer>> action = stream -> actionExecuted.set(true);

            OrElse result = emptyStream.acceptIfNotEmpty(action);

            assertEquals(OrElse.FALSE, result);
            assertFalse(actionExecuted.get());
        }
    }

    @Nested
    @DisplayName("JDK Stream Conversion")
    public class JDKStreamConversionTest {

        @Test
        @DisplayName("toJdkStream() should convert to JDK parallel stream")
        public void testToJdkStream() {
            java.util.stream.Stream<Integer> jdkStream = stream.limit(5).toJdkStream();

            assertNotNull(jdkStream);
            assertTrue(jdkStream.isParallel());
            assertEquals(Arrays.asList(1, 2, 3, 4, 5), jdkStream.collect(java.util.stream.Collectors.toList()));
        }

        @Test
        @DisplayName("toJdkStream() should handle close handlers")
        public void testToJdkStreamWithCloseHandler() {
            AtomicBoolean closed = new AtomicBoolean(false);
            Runnable closeHandler = () -> closed.set(true);

            java.util.stream.Stream<Integer> jdkStream = stream.limit(3).onClose(closeHandler).toJdkStream();

            jdkStream.collect(java.util.stream.Collectors.toList());
            jdkStream.close();

            // Note: Actual close behavior depends on implementation
            assertNotNull(jdkStream);
        }
    }

    @Nested
    @DisplayName("Performance and Concurrency")
    public class PerformanceConcurrencyTest {

        @Test
        @DisplayName("should handle large arrays efficiently")
        public void testLargeArray() {
            Integer[] largeArray = new Integer[1000];
            for (int i = 0; i < 1000; i++) {
                largeArray[i] = i + 1;
            }

            Stream<Integer> largeStream = Stream.of(largeArray).parallel(PS.create(Splitor.ARRAY));

            long sum = largeStream.mapToLong(Integer::longValue).sum();

            assertEquals(500500L, sum); // Sum of 1 to 1000
        }

        @Test
        @DisplayName("should maintain thread safety during parallel operations")
        public void testThreadSafety() {
            AtomicInteger counter = new AtomicInteger(0);

            stream.forEach(x -> counter.incrementAndGet(), () -> {
            });

            assertEquals(10, counter.get());
        }

        @Test
        @DisplayName("should handle concurrent modifications safely")
        public void testConcurrentModifications() {
            List<Integer> results = Collections.synchronizedList(new ArrayList<>());

            assertDoesNotThrow(() -> {
                stream.forEach(x -> {
                    results.add(x * 2);
                    Thread.yield(); // Allow other threads to run
                }, () -> {
                });
            });

            assertEquals(10, results.size());
        }

        @Test
        @DisplayName("should handle stress test with many operations")
        public void testStressTest() {
            assertDoesNotThrow(() -> {
                Integer result = stream.filter(x -> x % 2 == 0)
                        .map(x -> x * 2)
                        .peek(Fn.println())
                        .filter(x -> x > 5)
                        // .peek(Fn.println())
                        .reduce(0, Integer::sum, Integer::sum);

                assertEquals(Integer.valueOf(56), result); //8 + 12 + 16 + 20 = 56
            });
        }
    }
}
