package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
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
public class ParallelArrayStream100Test extends TestBase {

    private static final Integer[] TEST_INTEGER_ARRAY = new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    private static final String[] TEST_STRING_ARRAY = new String[] { "apple", "banana", "cherry", "date", "elderberry" };
    private static final int testMaxThreadNum = 4; // Use a fixed small number of threads for predictable testing  
    private Stream<Integer> stream;
    private Stream<String> stringStream;

    @BeforeEach
    public void setUp() {
        stream = Stream.of(TEST_INTEGER_ARRAY).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
        stringStream = Stream.of(TEST_STRING_ARRAY).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @AfterEach
    public void tearDown() {
    }

    @Nested
    @DisplayName("Filter Operations")
    public class FilterOperationsTest {

        @Test
        @DisplayName("filter() should filter elements based on predicate")
        public void testFilter() {
            Predicate<Integer> evenNumbers = x -> x % 2 == 0;

            List<Integer> result = stream.filter(evenNumbers).toList();

            assertTrue(N.isEqualCollection(Arrays.asList(2, 4, 6, 8, 10), result));
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

            assertTrue(N.isEqualCollection(Arrays.asList(1, 2, 3, 4), result));
        }

        @Test
        @DisplayName("dropWhile() should drop elements while predicate is true")
        public void testDropWhile() {
            Predicate<Integer> lessThan5 = x -> x < 5;

            List<Integer> result = stream.dropWhile(lessThan5).toList();

            assertTrue(N.isEqualCollection(Arrays.asList(5, 6, 7, 8, 9, 10), result));
        }
    }

    @Nested
    @DisplayName("Mapping Operations")
    public class MappingOperationsTest {

        @Test
        @DisplayName("map() should transform elements")
        public void testMap() {
            Function<Integer, String> toString = Object::toString;

            List<String> result = stream.map(toString).toList();

            assertTrue(N.isEqualCollection(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), result));
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

            List<String> result = stream.mapFirstOrElse(firstMapper, elseMapper).toList();

            assertEquals("FIRST:1", result.get(0));
            // assertEquals("OTHER:2", result.get(1));
        }

        @Test
        @DisplayName("mapLastOrElse() should map last element with last mapper")
        public void testMapLastOrElse() {
            Function<Integer, String> lastMapper = x -> "LAST:" + x;
            Function<Integer, String> elseMapper = x -> "OTHER:" + x;

            List<String> result = stream.mapLastOrElse(lastMapper, elseMapper).toList();

            // assertEquals("OTHER:1", result.get(0));
            assertEquals("LAST:10", result.get(result.size() - 1));
        }

        @Test
        @DisplayName("mapToChar() should convert to CharStream")
        public void testMapToChar() {
            ToCharFunction<Integer> toChar = x -> (char) (x + 64); // A=65, B=66, etc.

            char[] result = stream.limit(3).mapToChar(toChar).toArray();

            assertEquals(3, result.length);
            assertTrue(N.isEqualCollection(List.of('A', 'B', 'C'), N.toList(result)));
            // assertEquals('A', result[0]); // 1 + 64 = 65 = 'A'
        }

        @Test
        @DisplayName("mapToByte() should convert to ByteStream")
        public void testMapToByte() {
            ToByteFunction<Integer> toByte = Integer::byteValue;

            byte[] result = stream.limit(3).mapToByte(toByte).toArray();

            assertEquals(3, result.length);
            assertTrue(N.isEqualCollection(List.of((byte) 1, (byte) 2, (byte) 3), N.toList(result)));
            // assertEquals((byte) 1, result[0]);
        }

        @Test
        @DisplayName("mapToShort() should convert to ShortStream")
        public void testMapToShort() {
            ToShortFunction<Integer> toShort = Integer::shortValue;

            short[] result = stream.limit(3).mapToShort(toShort).toArray();

            assertEquals(3, result.length);
            assertTrue(N.isEqualCollection(List.of((short) 1, (short) 2, (short) 3), N.toList(result)));
            // assertEquals((short) 1, result[0]);
        }

        @Test
        @DisplayName("mapToInt() should convert to IntStream")
        public void testMapToInt() {
            ToIntFunction<Integer> toInt = Integer::intValue;

            int[] result = stream.limit(3).mapToInt(toInt).toArray();

            assertEquals(3, result.length);
            assertTrue(N.isEqualCollection(List.of(1, 2, 3), N.toList(result)));
            // assertEquals(1, result[0]);
        }

        @Test
        @DisplayName("mapToLong() should convert to LongStream")
        public void testMapToLong() {
            ToLongFunction<Integer> toLong = Integer::longValue;

            long[] result = stream.limit(3).mapToLong(toLong).toArray();

            assertEquals(3, result.length);
            assertTrue(N.isEqualCollection(List.of(1L, 2L, 3L), N.toList(result)));
            // assertEquals(1L, result[0]);
        }

        @Test
        @DisplayName("mapToFloat() should convert to FloatStream")
        public void testMapToFloat() {
            ToFloatFunction<Integer> toFloat = Integer::floatValue;

            float[] result = stream.limit(3).mapToFloat(toFloat).toArray();

            assertEquals(3, result.length);
            assertTrue(N.isEqualCollection(List.of(1.0f, 2.0f, 3.0f), N.toList(result)));
        }

        @Test
        @DisplayName("mapToDouble() should convert to DoubleStream")
        public void testMapToDouble() {
            ToDoubleFunction<Integer> toDouble = Integer::doubleValue;

            double[] result = stream.limit(3).mapToDouble(toDouble).toArray();

            assertEquals(3, result.length);
            assertTrue(N.isEqualCollection(List.of(1.0d, 2.0d, 3.0d), N.toList(result)));
        }
    }

    @Nested
    @DisplayName("FlatMap Operations")
    public class FlatMapOperationsTest {

        @Test
        @DisplayName("flatMap() should flatten streams")
        public void testFlatMap() {
            Function<Integer, Stream<Integer>> duplicate = x -> Stream.of(x, x);

            List<Integer> result = stream.limit(3).flatMap(duplicate).toList();

            assertTrue(N.isEqualCollection(Arrays.asList(1, 1, 2, 2, 3, 3), result));
        }

        @Test
        @DisplayName("flatmap() should flatten collections")
        public void testFlatmapCollection() {
            Function<Integer, Collection<Integer>> duplicate = x -> Arrays.asList(x, x);

            List<Integer> result = stream.limit(3).flatmap(duplicate).toList();

            assertTrue(N.isEqualCollection(Arrays.asList(1, 1, 2, 2, 3, 3), result));
        }

        @Test
        @DisplayName("flattmap() should flatten arrays")
        public void testFlattMapArray() {
            Function<Integer, Integer[]> duplicate = x -> new Integer[] { x, x };

            List<Integer> result = stream.limit(3).flattmap(duplicate).toList();

            assertTrue(N.isEqualCollection(Arrays.asList(1, 1, 2, 2, 3, 3), result));
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

            List<Integer> result = stream.limit(5).onEach(addToSum).toList();

            assertTrue(N.isEqualCollection(Arrays.asList(1, 2, 3, 4, 5), result));
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

            stringStream.limit(2).sorted().forEach(str -> Arrays.asList(str.charAt(0), str.charAt(1)), (str, ch) -> {
                synchronized (results) { // Ensure thread-safe addition to results
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
                synchronized (pairs) { // Ensure thread-safe addition to pairs
                    pairs.add(a + "," + b);
                }
            });

            assertFalse(pairs.isEmpty());
            assertTrue(pairs.contains("1,2"));
        }

        @Test
        @DisplayName("forEachTriple() should process triplets of elements")
        public void testForEachTriple() {
            List<String> triplets = new ArrayList<>();

            stream.limit(5).forEachTriple(1, Fn.sc(stream, (a, b, c) -> {
                triplets.add(a + "," + b + "," + c);
            }));

            assertFalse(triplets.isEmpty());
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

            Multimap<String, Integer, List<Integer>> result = stream.limit(6)
                    .toMultimap(x -> x % 2 == 0 ? "even" : "odd", Fn.identity(), Suppliers.ofListMultimap());

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
    @DisplayName("ForEach Operations")
    public class ForEachOperationsTest {

        @Test
        @DisplayName("forEach with flatMapper should apply action to each element")
        public void test_forEach() {
            {
                List<Integer> results = new ArrayList<>();

                Stream.of(TEST_INTEGER_ARRAY)
                        .parallel(PS.create(Splitor.ITERATOR))
                        .forEach(it -> N.asList(it * 2, it * 2), Fn.sc(results, (a, b) -> results.add(a)));

                assertHaveSameElements(N.repeatCollection(N.asList(TEST_INTEGER_ARRAY), 2), results);
            }
            {
                List<Integer> results = new ArrayList<>();

                Stream.of(TEST_INTEGER_ARRAY)
                        .parallel(PS.create(Splitor.ARRAY))
                        .forEach(it -> N.asList(it * 2, it * 2), Fn.sc(results, (a, b) -> results.add(a)));

                assertHaveSameElements(N.repeatCollection(N.asList(TEST_INTEGER_ARRAY), 2), results);

            }

            {
                List<Integer> results = new ArrayList<>();

                Stream.of(TEST_INTEGER_ARRAY)
                        .parallel(PS.create(Splitor.ITERATOR))
                        .forEach(it -> N.asList(it * 2, it * 2), it -> N.asList(it * 3, it * 3), Fn.sc(results, (a, b, c) -> results.add(a)));

                assertHaveSameElements(N.repeatCollection(N.asList(TEST_INTEGER_ARRAY), 4), results);
            }

            {
                List<Integer> results = new ArrayList<>();

                Stream.of(TEST_INTEGER_ARRAY)
                        .parallel(PS.create(Splitor.ARRAY))
                        .forEach(it -> N.asList(it * 2, it * 2), it -> N.asList(it * 3, it * 3), Fn.sc(results, (a, b, c) -> results.add(a)));

                assertHaveSameElements(N.repeatCollection(N.asList(TEST_INTEGER_ARRAY), 4), results);
            }
        }

        @Test
        @DisplayName("forEachPair")
        public void test_forEachPair() {
            {
                List<String> results = new ArrayList<>();

                Stream.of(TEST_INTEGER_ARRAY).limit(5).parallel(PS.create(Splitor.ITERATOR)).forEachPair(Fn.sc(results, (a, b) -> results.add(a + "->" + b)));

                assertHaveSameElements(N.asList("1->2", "2->3", "3->4", "4->5"), results);
            }
            {
                List<String> results = new ArrayList<>();

                Stream.of(TEST_INTEGER_ARRAY).limit(5).parallel(PS.create(Splitor.ARRAY)).forEachPair(Fn.sc(results, (a, b) -> results.add(a + "->" + b)));

                assertHaveSameElements(N.asList("1->2", "2->3", "3->4", "4->5"), results);
            }
            {
                List<String> results = new ArrayList<>();

                Stream.of(TEST_INTEGER_ARRAY)
                        .limit(5)
                        .parallel(PS.create(Splitor.ITERATOR))
                        .forEachPair(2, Fn.sc(results, (a, b) -> results.add(a + "->" + b)));

                assertHaveSameElements(N.asList("1->2", "3->4", "5->null"), results);
            }
        }

        @Test
        @DisplayName("forEachTriple")
        public void test_forEachTriple() {
            {
                List<String> results = new ArrayList<>();

                Stream.of(TEST_INTEGER_ARRAY)
                        .limit(5)
                        .parallel(PS.create(Splitor.ITERATOR))
                        .forEachTriple(Fn.sc(results, (a, b, c) -> results.add(a + "->" + b + "->" + c)));

                assertHaveSameElements(N.asList("1->2->3", "2->3->4", "3->4->5"), results);
            }
            {
                List<String> results = new ArrayList<>();

                Stream.of(TEST_INTEGER_ARRAY)
                        .limit(5)
                        .parallel(PS.create(Splitor.ARRAY))
                        .forEachTriple(Fn.sc(results, (a, b, c) -> results.add(a + "->" + b + "->" + c)));

                assertHaveSameElements(N.asList("1->2->3", "2->3->4", "3->4->5"), results);
            }
            {
                List<String> results = new ArrayList<>();

                Stream.of(TEST_INTEGER_ARRAY)
                        .limit(5)
                        .parallel(PS.create(Splitor.ARRAY))
                        .forEachTriple(2, Fn.sc(results, (a, b, c) -> results.add(a + "->" + b + "->" + c)));

                assertHaveSameElements(N.asList("1->2->3", "3->4->5"), results);
            }
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
            Stream<Integer> emptyStream = Stream.of(new Integer[0]).parallel(PS.create(Splitor.ARRAY));

            Optional<Integer> result = emptyStream.min(Integer::compareTo);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("max() should return empty for empty stream")
        public void testMaxEmpty() {
            Stream<Integer> emptyStream = Stream.of(new Integer[0]).parallel(PS.create(Splitor.ARRAY)); // Empty stream 

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

            List<Integer> result = stream.intersection(identity, other).toList();

            assertTrue(N.isEqualCollection(Arrays.asList(2, 4, 6, 8), result));
        }

        @Test
        @DisplayName("difference() should find different elements")
        public void testDifference() {
            Function<Integer, Integer> identity = Function.identity();
            Collection<Integer> other = Arrays.asList(2, 4, 6, 8);

            List<Integer> result = stream.difference(identity, other).toList();

            assertTrue(N.isEqualCollection(Arrays.asList(1, 3, 5, 7, 9, 10), result));
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
            {
                Collection<String> other = Arrays.asList("a", "b", "c", "d", "e");
                BiFunction<Integer, String, String> zipFunction = (i, s) -> i + s;

                List<String> result = stream.limit(5).zipWith(other, zipFunction).toList();

                assertTrue(N.isEqualCollection(Arrays.asList("1a", "2b", "3c", "4d", "5e"), result));
            }
            {
                Collection<String> other = Arrays.asList("a", "b", "c", "d", "e");
                Collection<String> other2 = Arrays.asList("a", "b", "c", "d", "e");
                TriFunction<Integer, String, String, String> zipFunction = (i, s, c) -> i + s + c;

                List<String> result = Stream.of(TEST_INTEGER_ARRAY).limit(5).zipWith(other, other2, zipFunction).toList();

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

                List<String> result = Stream.of(TEST_INTEGER_ARRAY).limit(5).zipWith(other, other2, defaultInt, defaultString, "z", zipFunction).toList();

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

                List<String> result = Stream.of(TEST_INTEGER_ARRAY).limit(5).zipWith(Stream.of(other), Stream.of(other2), zipFunction).toList();

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

                List<String> result = Stream.of(TEST_INTEGER_ARRAY)
                        .limit(5)
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
    @DisplayName("Edge Cases and Error Handling")
    public class EdgeCasesTest {

        @Test
        @DisplayName("should handle empty array")
        public void testEmptyArray() {
            Integer[] emptyArray = new Integer[0];
            Stream<Integer> emptyStream = Stream.of(emptyArray).parallel(PS.create(Splitor.ARRAY));

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
            Stream<Integer> streamWithNulls = Stream.of(arrayWithNulls);

            List<Integer> result = streamWithNulls.toList();

            assertEquals(Arrays.asList(1, null, 3, null, 5), result);
        }

        @Test
        @DisplayName("should handle subrange of array")
        public void testSubrange() {
            Stream<Integer> subStream = Stream.of(TEST_INTEGER_ARRAY, 2, 5).parallel(PS.create(Splitor.ARRAY)); // Subrange from index 2 to 5

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
    }
}
