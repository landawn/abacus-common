package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.IndexedFloat;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;

public class FloatStream103Test extends TestBase {

    // This method needs to be implemented by a concrete test class to provide a FloatStream instance.
    // For example, in ArrayFloatStreamTest, it would return new ArrayFloatStream(a);
    // In IteratorFloatStreamTest, it would return new IteratorFloatStream(FloatIterator.of(a));
    protected FloatStream createFloatStream(float... a) {
        return FloatStream.of(a).map(e -> e + 0.0f);
    }

    protected FloatStream createFloatStream(float[] a, int fromIndex, int toIndex) {
        return FloatStream.of(a, fromIndex, toIndex).map(e -> e + 0.0f);
    }

    protected FloatStream createFloatStream(Float[] a) {
        return FloatStream.of(a).map(e -> e + 0.0f);
    }

    protected FloatStream createFloatStream(Float[] a, int fromIndex, int toIndex) {
        return FloatStream.of(a, fromIndex, toIndex).map(e -> e + 0.0f);
    }

    protected FloatStream createFloatStream(Collection<Float> coll) {
        return FloatStream.of(coll.toArray(new Float[coll.size()])).map(e -> e + 0.0f);
    }

    protected FloatStream createFloatStream(FloatIterator iter) {
        return iter == null ? FloatStream.empty() : FloatStream.of(iter.toArray()).map(e -> e + 0.0f);
    }

    protected FloatStream createFloatStream(FloatBuffer buff) {
        return FloatStream.of(buff).map(e -> e + 0.0f);
    }

    @Nested
    @DisplayName("Static Factory Methods")
    public class StaticFactoryMethods {

        @Test
        @DisplayName("empty() should return empty stream")
        public void testEmpty() {
            FloatStream stream = FloatStream.empty();
            assertEquals(0, stream.count());
        }

        @Test
        @DisplayName("of() with varargs should create stream with elements")
        public void testOfVarargs() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
        }

        @Test
        @DisplayName("of() with array should create stream")
        public void testOfArray() {
            float[] array = { 1.5f, 2.5f, 3.5f };
            FloatStream stream = createFloatStream(array);
            float[] result = stream.toArray();
            assertArrayEquals(array, result);
        }

        @Test
        @DisplayName("of() with array range should create stream with subarray")
        public void testOfArrayRange() {
            float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
            FloatStream stream = createFloatStream(array, 1, 4);
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 2.0f, 3.0f, 4.0f }, result);
        }

        @Test
        @DisplayName("of() with Float array should create stream")
        public void testOfFloatArray() {
            Float[] array = { 1.0f, 2.0f, 3.0f };
            FloatStream stream = createFloatStream(array);
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
        }

        @Test
        @DisplayName("of() with collection should create stream")
        public void testOfCollection() {
            List<Float> list = Arrays.asList(1.0f, 2.0f, 3.0f);
            FloatStream stream = createFloatStream(list);
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
        }

        @Test
        @DisplayName("of() with iterator should create stream")
        public void testOfIterator() {
            FloatIterator iterator = FloatIterator.of(1.0f, 2.0f, 3.0f);
            FloatStream stream = createFloatStream(iterator);
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
        }

        @Test
        @DisplayName("of() with FloatBuffer should create stream")
        public void testOfFloatBuffer() {
            FloatBuffer buffer = FloatBuffer.allocate(3);
            buffer.put(1.0f).put(2.0f).put(3.0f).flip();
            FloatStream stream = createFloatStream(buffer);
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
        }

        @Test
        @DisplayName("ofNullable() with null should return empty stream")
        public void testOfNullableWithNull() {
            FloatStream stream = FloatStream.ofNullable(null);
            assertEquals(0, stream.count());
        }

        @Test
        @DisplayName("ofNullable() with value should return single element stream")
        public void testOfNullableWithValue() {
            FloatStream stream = FloatStream.ofNullable(5.0f);
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 5.0f }, result);
        }

        @Test
        @DisplayName("repeat() should create stream with repeated element")
        public void testRepeat() {
            FloatStream stream = FloatStream.repeat(2.5f, 3);
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 2.5f, 2.5f, 2.5f }, result);
        }

        @Test
        @DisplayName("repeat() with zero count should return empty stream")
        public void testRepeatZero() {
            FloatStream stream = FloatStream.repeat(1.0f, 0);
            assertEquals(0, stream.count());
        }

        @Test
        @DisplayName("iterate() with predicate should create stream")
        public void testIterateWithPredicate() {
            FloatStream stream = FloatStream.iterate(1.0f, x -> x < 5.0f, x -> x + 1.0f);
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result);
        }

        @Test
        @DisplayName("iterate() without predicate should create infinite stream")
        public void testIterateInfinite() {
            FloatStream stream = FloatStream.iterate(1.0f, x -> x + 1.0f);
            float[] result = stream.limit(3).toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
        }

        @Test
        @DisplayName("generate() should create stream with supplier")
        public void testGenerate() {
            FloatStream stream = FloatStream.generate(() -> 5.0f);
            float[] result = stream.limit(3).toArray();
            assertArrayEquals(new float[] { 5.0f, 5.0f, 5.0f }, result);
        }

        @Test
        @DisplayName("flatten() should flatten 2D array")
        public void testFlatten2D() {
            float[][] array = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            FloatStream stream = FloatStream.flatten(array);
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result);
        }

        @Test
        @DisplayName("flatten() with empty array should return empty stream")
        public void testFlattenEmpty() {
            float[][] array = {};
            FloatStream stream = FloatStream.flatten(array);
            assertEquals(0, stream.count());
        }
    }

    @Nested
    @DisplayName("Transformation Operations")
    public class TransformationOperations {

        @Test
        @DisplayName("map() should transform elements")
        public void testMap() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            float[] result = stream.map(x -> x * 2).toArray();
            assertArrayEquals(new float[] { 2.0f, 4.0f, 6.0f }, result);
        }

        @Test
        @DisplayName("mapToInt() should convert to IntStream")
        public void testMapToInt() {
            FloatStream stream = createFloatStream(1.5f, 2.5f, 3.5f);
            IntStream intStream = stream.mapToInt(x -> (int) x);
            int[] result = intStream.toArray();
            assertArrayEquals(new int[] { 1, 2, 3 }, result);
        }

        @Test
        @DisplayName("mapToLong() should convert to LongStream")
        public void testMapToLong() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            LongStream longStream = stream.mapToLong(x -> (long) x);
            long[] result = longStream.toArray();
            assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
        }

        @Test
        @DisplayName("mapToDouble() should convert to DoubleStream")
        public void testMapToDouble() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            DoubleStream doubleStream = stream.mapToDouble(x -> x);
            double[] result = doubleStream.toArray();
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
        }

        @Test
        @DisplayName("mapToObj() should convert to object stream")
        public void testMapToObj() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            Stream<String> objectStream = stream.mapToObj(String::valueOf);
            String[] result = objectStream.toArray(String[]::new);
            assertArrayEquals(new String[] { "1.0", "2.0", "3.0" }, result);
        }

        @Test
        @DisplayName("flatMap() should flatten streams")
        public void testFlatMap() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            FloatStream result = stream.flatMap(x -> createFloatStream(x, x * 2));
            float[] array = result.toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 2.0f, 4.0f }, array);
        }

        @Test
        @DisplayName("flatmap() with array should flatten arrays")
        public void testFlatmapArray() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            FloatStream result = stream.flatmap(x -> new float[] { x, x * 2 });
            float[] array = result.toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 2.0f, 4.0f }, array);
        }

        @Test
        @DisplayName("asDoubleStream() should convert to DoubleStream")
        public void testAsDoubleStream() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            DoubleStream doubleStream = stream.asDoubleStream();
            double[] result = doubleStream.toArray();
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
        }

        @Test
        @DisplayName("boxed() should convert to Stream<Float>")
        public void testBoxed() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            Stream<Float> boxedStream = stream.boxed();
            Float[] result = boxedStream.toArray(Float[]::new);
            assertArrayEquals(new Float[] { 1.0f, 2.0f, 3.0f }, result);
        }
    }

    @Nested
    @DisplayName("Filtering and Limiting Operations")
    public class FilteringOperations {

        @Test
        @DisplayName("filter() should filter elements")
        public void testFilter() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            float[] result = stream.filter(x -> x > 2.0f).toArray();
            assertArrayEquals(new float[] { 3.0f, 4.0f }, result);
        }

        @Test
        @DisplayName("distinct() should remove duplicates")
        public void testDistinct() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 2.0f, 3.0f, 1.0f);
            float[] result = stream.distinct().toArray();
            Arrays.sort(result); // Order might not be preserved
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
        }

        @Test
        @DisplayName("limit() should limit stream size")
        public void testLimit() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            float[] result = stream.limit(3).toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
        }

        @Test
        @DisplayName("skip() should skip elements")
        public void testSkip() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            float[] result = stream.skip(2).toArray();
            assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f }, result);
        }

        @Test
        @DisplayName("takeWhile() should take elements while predicate is true")
        public void testTakeWhile() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 2.0f, 1.0f);
            float[] result = stream.takeWhile(x -> x < 3.0f).toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f }, result);
        }

        @Test
        @DisplayName("dropWhile() should drop elements while predicate is true")
        public void testDropWhile() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 2.0f);
            float[] result = stream.dropWhile(x -> x < 3.0f).toArray();
            assertArrayEquals(new float[] { 3.0f, 4.0f, 2.0f }, result);
        }

        @Test
        @DisplayName("step() should take every nth element")
        public void testStep() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f);
            float[] result = stream.step(2).toArray();
            assertArrayEquals(new float[] { 1.0f, 3.0f, 5.0f }, result);
        }
    }

    @Nested
    @DisplayName("Sorting and Ordering Operations")
    public class SortingOperations {

        @Test
        @DisplayName("sorted() should sort elements")
        public void testSorted() {
            FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f);
            float[] result = stream.sorted().toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result);
        }

        @Test
        @DisplayName("reverseSorted() should sort in reverse order")
        public void testReverseSorted() {
            FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f);
            float[] result = stream.reverseSorted().toArray();
            assertArrayEquals(new float[] { 4.0f, 3.0f, 2.0f, 1.0f }, result);
        }

        @Test
        @DisplayName("reversed() should reverse order")
        public void testReversed() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            float[] result = stream.reversed().toArray();
            assertArrayEquals(new float[] { 4.0f, 3.0f, 2.0f, 1.0f }, result);
        }

        @Test
        @DisplayName("shuffled() should shuffle elements")
        public void testShuffled() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            float[] result = stream.shuffled().toArray();
            assertEquals(4, result.length);
            // Cannot test exact order due to randomness, but check all elements present
            Arrays.sort(result);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result);
        }

        @Test
        @DisplayName("rotated() should rotate elements")
        public void testRotated() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            float[] result = stream.rotated(2).toArray();
            assertArrayEquals(new float[] { 3.0f, 4.0f, 1.0f, 2.0f }, result);
        }

        @Test
        @DisplayName("top() should return top n elements")
        public void testTop() {
            FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f, 5.0f);
            float[] result = stream.top(3).toArray();
            Arrays.sort(result);
            assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f }, result);
        }
    }

    @Nested
    @DisplayName("Aggregation Operations")
    public class AggregationOperations {

        @Test
        @DisplayName("count() should return element count")
        public void testCount() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertEquals(3, stream.count());
        }

        @Test
        @DisplayName("sum() should return sum of elements")
        public void testSum() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertEquals(6.0, stream.sum(), 0.001);
        }

        @Test
        @DisplayName("average() should return average of elements")
        public void testAverage() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            OptionalDouble average = stream.average();
            assertTrue(average.isPresent());
            assertEquals(2.0, average.getAsDouble(), 0.001);
        }

        @Test
        @DisplayName("average() of empty stream should return empty")
        public void testAverageEmpty() {
            FloatStream stream = FloatStream.empty();
            OptionalDouble average = stream.average();
            assertFalse(average.isPresent());
        }

        @Test
        @DisplayName("min() should return minimum element")
        public void testMin() {
            FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f);
            OptionalFloat min = stream.min();
            assertTrue(min.isPresent());
            assertEquals(1.0f, min.get(), 0.001);
        }

        @Test
        @DisplayName("max() should return maximum element")
        public void testMax() {
            FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f);
            OptionalFloat max = stream.max();
            assertTrue(max.isPresent());
            assertEquals(4.0f, max.get(), 0.001);
        }

        @Test
        @DisplayName("kthLargest() should return kth largest element")
        public void testKthLargest() {
            FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f, 5.0f);
            OptionalFloat kth = stream.kthLargest(2);
            assertTrue(kth.isPresent());
            assertEquals(4.0f, kth.get(), 0.001);
        }

        @Test
        @DisplayName("summarize() should return summary statistics")
        public void testSummarize() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            FloatSummaryStatistics stats = stream.summarize();
            assertEquals(5, stats.getCount());
            assertEquals(1.0f, stats.getMin(), 0.001);
            assertEquals(5.0f, stats.getMax(), 0.001);
            assertEquals(15.0, stats.getSum(), 0.001);
            assertEquals(3.0, stats.getAverage(), 0.001);
        }
    }

    @Nested
    @DisplayName("Search Operations")
    public class SearchOperations {

        @Test
        @DisplayName("findFirst() should return first element")
        public void testFindFirst() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            OptionalFloat first = stream.first();
            assertTrue(first.isPresent());
            assertEquals(1.0f, first.get(), 0.001);
        }

        @Test
        @DisplayName("findFirst() of empty stream should return empty")
        public void testFindFirstEmpty() {
            FloatStream stream = FloatStream.empty();
            OptionalFloat first = stream.first();
            assertFalse(first.isPresent());
        }

        @Test
        @DisplayName("findLast() should return last element")
        public void testFindLast() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            OptionalFloat last = stream.last();
            assertTrue(last.isPresent());
            assertEquals(3.0f, last.get(), 0.001);
        }

        @Test
        @DisplayName("findAny() should return any element")
        public void testFindAny() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            OptionalFloat any = stream.first();
            assertTrue(any.isPresent());
            assertTrue(any.get() >= 1.0f && any.get() <= 3.0f);
        }

        @Test
        @DisplayName("onlyOne() should return single element")
        public void testOnlyOne() {
            FloatStream stream = createFloatStream(5.0f);
            OptionalFloat only = stream.onlyOne();
            assertTrue(only.isPresent());
            assertEquals(5.0f, only.get(), 0.001);
        }

        @Test
        @DisplayName("onlyOne() with multiple elements should throw exception")
        public void testOnlyOneMultiple() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            assertThrows(Exception.class, stream::onlyOne);
        }

        @Test
        @DisplayName("elementAt() should return element at position")
        public void testElementAt() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            OptionalFloat element = stream.elementAt(2);
            assertTrue(element.isPresent());
            assertEquals(3.0f, element.get(), 0.001);
        }
    }

    @Nested
    @DisplayName("Matching Operations")
    public class MatchingOperations {

        @Test
        @DisplayName("anyMatch() should return true if any element matches")
        public void testAnyMatch() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertTrue(stream.anyMatch(x -> x > 2.0f));
        }

        @Test
        @DisplayName("allMatch() should return true if all elements match")
        public void testAllMatch() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertTrue(stream.allMatch(x -> x > 0.0f));

            FloatStream stream2 = createFloatStream(1.0f, 2.0f, 3.0f);
            assertFalse(stream2.allMatch(x -> x > 2.0f));
        }

        @Test
        @DisplayName("noneMatch() should return true if no elements match")
        public void testNoneMatch() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertTrue(stream.noneMatch(x -> x > 5.0f));

            FloatStream stream2 = createFloatStream(1.0f, 2.0f, 3.0f);
            assertFalse(stream2.noneMatch(x -> x > 2.0f));
        }
    }

    @Nested
    @DisplayName("Collection Operations")
    public class CollectionOperations {

        @Test
        @DisplayName("toArray() should return array")
        public void testToArray() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
        }

        @Test
        @DisplayName("toList() should return list")
        public void testToList() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            List<Float> result = stream.toList();
            assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), result);
        }

        @Test
        @DisplayName("toSet() should return set")
        public void testToSet() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 2.0f, 3.0f);
            Set<Float> result = stream.toSet();
            assertEquals(new HashSet<>(Arrays.asList(1.0f, 2.0f, 3.0f)), result);
        }

        @Test
        @DisplayName("toFloatList() should return FloatList")
        public void testToFloatList() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            FloatList result = stream.toFloatList();
            assertEquals(3, result.size());
            assertEquals(1.0f, result.get(0), 0.001);
            assertEquals(2.0f, result.get(1), 0.001);
            assertEquals(3.0f, result.get(2), 0.001);
        }

        @Test
        @DisplayName("collect() with custom collector should work")
        public void testCollect() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            StringBuilder result = stream.collect(StringBuilder::new, (sb, f) -> sb.append(f).append(","), StringBuilder::append);
            assertEquals("1.0,2.0,3.0,", result.toString());
        }
    }

    @Nested
    @DisplayName("Reduction Operations")
    public class ReductionOperations {

        @Test
        @DisplayName("reduce() with identity should return reduced value")
        public void testReduceWithIdentity() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            float result = stream.reduce(0.0f, Float::sum);
            assertEquals(6.0f, result, 0.001);
        }

        @Test
        @DisplayName("reduce() without identity should return OptionalFloat")
        public void testReduceWithoutIdentity() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            OptionalFloat result = stream.reduce(Float::sum);
            assertTrue(result.isPresent());
            assertEquals(6.0f, result.get(), 0.001);
        }

        @Test
        @DisplayName("reduce() of empty stream should return empty")
        public void testReduceEmpty() {
            FloatStream stream = FloatStream.empty();
            OptionalFloat result = stream.reduce(Float::sum);
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("scan() should return cumulative results")
        public void testScan() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            float[] result = stream.scan(Float::sum).toArray();
            assertArrayEquals(new float[] { 1.0f, 3.0f, 6.0f }, result);
        }

        @Test
        @DisplayName("scan() with initial value should include initial")
        public void testScanWithInit() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            float[] result = stream.scan(10.0f, Float::sum).toArray();
            assertArrayEquals(new float[] { 11.0f, 13.0f, 16.0f }, result);
        }
    }

    @Nested
    @DisplayName("Joining Operations")
    public class JoiningOperations {

        @Test
        @DisplayName("join() should join elements with delimiter")
        public void testJoin() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            String result = stream.join(", ");
            assertEquals("1.0, 2.0, 3.0", result);
        }

        @Test
        @DisplayName("join() with prefix and suffix should format correctly")
        public void testJoinWithPrefixSuffix() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            String result = stream.join(", ", "[", "]");
            assertEquals("[1.0, 2.0, 3.0]", result);
        }
    }

    @Nested
    @DisplayName("Parallel Operations")
    public class ParallelOperations {

        @Test
        @DisplayName("parallel() should return parallel stream")
        public void testParallel() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            FloatStream parallelStream = stream.parallel();
            assertTrue(parallelStream.isParallel());
        }

        @Test
        @DisplayName("sequential() should return sequential stream")
        public void testSequential() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f).parallel();
            FloatStream sequentialStream = stream.sequential();
            assertFalse(sequentialStream.isParallel());
        }

        @Test
        @DisplayName("parallel operations should work correctly")
        public void testParallelProcessing() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            double sum = stream.parallel().mapToDouble(x -> x).sum();
            assertEquals(15.0, sum, 0.001);
        }
    }

    @Nested
    @DisplayName("Zip Operations")
    public class ZipOperations {

        @Test
        @DisplayName("zip() should combine two streams")
        public void testZip() {
            FloatStream stream1 = createFloatStream(1.0f, 2.0f, 3.0f);
            FloatStream stream2 = createFloatStream(4.0f, 5.0f, 6.0f);
            float[] result = FloatStream.zip(stream1, stream2, Float::sum).toArray();
            assertArrayEquals(new float[] { 5.0f, 7.0f, 9.0f }, result);
        }

        @Test
        @DisplayName("zip() with different lengths should stop at shorter")
        public void testZipDifferentLengths() {
            FloatStream stream1 = createFloatStream(1.0f, 2.0f);
            FloatStream stream2 = createFloatStream(4.0f, 5.0f, 6.0f);
            float[] result = FloatStream.zip(stream1, stream2, Float::sum).toArray();
            assertArrayEquals(new float[] { 5.0f, 7.0f }, result);
        }

        @Test
        @DisplayName("zip() with padding should handle different lengths")
        public void testZipWithPadding() {
            FloatStream stream1 = createFloatStream(1.0f, 2.0f);
            FloatStream stream2 = createFloatStream(4.0f, 5.0f, 6.0f);
            float[] result = FloatStream.zip(stream1, stream2, 0.0f, 0.0f, Float::sum).toArray();
            assertArrayEquals(new float[] { 5.0f, 7.0f, 6.0f }, result);
        }
    }

    @Nested
    @DisplayName("Concat Operations")
    public class ConcatOperations {

        @Test
        @DisplayName("concat() should concatenate streams")
        public void testConcat() {
            FloatStream stream1 = createFloatStream(1.0f, 2.0f);
            FloatStream stream2 = createFloatStream(3.0f, 4.0f);
            float[] result = FloatStream.concat(stream1, stream2).toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result);
        }

        @Test
        @DisplayName("prepend() should add elements to beginning")
        public void testPrepend() {
            FloatStream stream = createFloatStream(3.0f, 4.0f);
            float[] result = stream.prepend(1.0f, 2.0f).toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result);
        }

        @Test
        @DisplayName("append() should add elements to end")
        public void testAppend() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            float[] result = stream.append(3.0f, 4.0f).toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    public class ErrorHandling {

        @Test
        @DisplayName("IllegalArgumentException should be thrown for negative arguments")
        public void testNegativeArguments() {
            assertThrows(IllegalArgumentException.class, () -> FloatStream.repeat(1.0f, -1));

            assertThrows(IllegalArgumentException.class, () -> createFloatStream(1.0f, 2.0f, 3.0f).limit(-1));

            assertThrows(IllegalArgumentException.class, () -> createFloatStream(1.0f, 2.0f, 3.0f).skip(-1));
        }

        @Test
        @DisplayName("NullPointerException should be thrown for null arguments")
        public void testNullArguments() {
            assertThrows(NullPointerException.class, () -> createFloatStream(1.0f, 2.0f).map(null).count());

            assertThrows(NullPointerException.class, () -> createFloatStream(1.0f, 2.0f).filter(null).count());
        }

        @Test
        @DisplayName("IllegalStateException should be thrown on closed stream")
        public void testClosedStream() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            stream.toArray(); // This should close the stream
            assertThrows(IllegalStateException.class, () -> stream.count());
        }
    }

    @Nested
    @DisplayName("Advanced Stream Operations")
    public class AdvancedStreamOperations {

        @Test
        @DisplayName("mapPartial() should filter and map partial results")
        public void testMapPartial() {
            FloatStream stream = createFloatStream(1.0f, -2.0f, 3.0f, -4.0f);
            float[] result = stream.mapPartial(x -> x > 0 ? OptionalFloat.of(x * 2) : OptionalFloat.empty()).toArray();
            assertArrayEquals(new float[] { 2.0f, 6.0f }, result);
        }

        @Test
        @DisplayName("rangeMap() should map ranges of adjacent elements")
        public void testRangeMap() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 6.0f, 7.0f);
            float[] result = stream.rangeMap((a, b) -> Math.abs(b - a) <= 1.5f, (start, end) -> end - start).toArray();
            // Groups: [1,2,3] -> 2.0, [6,7] -> 1.0
            assertEquals(3, result.length);
        }

        @Test
        @DisplayName("rangeMapToObj() should map ranges to objects")
        public void testRangeMapToObj() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 5.0f, 6.0f);
            String[] result = stream.rangeMapToObj((a, b) -> Math.abs(b - a) <= 1.5f, (start, end) -> String.format("%.1f-%.1f", start, end))
                    .toArray(String[]::new);
            assertEquals(2, result.length);
            assertTrue(result[0].contains("1.0") && result[0].contains("2.0"));
            assertTrue(result[1].contains("5.0") && result[1].contains("6.0"));
        }

        @Test
        @DisplayName("collapse() should group consecutive elements")
        public void testCollapse() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 5.0f, 6.0f, 7.0f);
            Stream<FloatList> result = stream.collapse((a, b) -> Math.abs(b - a) <= 1.5f);
            List<FloatList> groups = result.toList();
            assertEquals(2, groups.size());
            assertEquals(2, groups.get(0).size()); // [1.0, 2.0]
            assertEquals(3, groups.get(1).size()); // [5.0, 6.0, 7.0]
        }

        @Test
        @DisplayName("collapse() with merge function should combine adjacent elements")
        public void testCollapseWithMerge() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 5.0f, 6.0f, 7.0f);
            float[] result = stream.collapse((a, b) -> Math.abs(b - a) <= 1.5f, Float::sum).toArray();
            assertEquals(2, result.length);
            assertEquals(3.0f, result[0], 0.001); // 1.0 + 2.0
            assertEquals(18.0f, result[1], 0.001); // 5.0 + 6.0 + 7.0
        }

        @Test
        @DisplayName("skipUntil() should skip elements until predicate is true")
        public void testSkipUntil() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            float[] result = stream.skipUntil(x -> x >= 3.0f).toArray();
            assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f }, result);
        }

        @Test
        @DisplayName("cycled() should repeat the stream indefinitely")
        public void testCycled() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            float[] result = stream.cycled().limit(6).toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 1.0f, 2.0f, 1.0f, 2.0f }, result);
        }

        @Test
        @DisplayName("cycled() with rounds should repeat specific number of times")
        public void testCycledWithRounds() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            float[] result = stream.cycled(3).toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 1.0f, 2.0f, 1.0f, 2.0f }, result);
        }

        @Test
        @DisplayName("indexed() should pair elements with their indices")
        public void testIndexed() {
            FloatStream stream = createFloatStream(10.0f, 20.0f, 30.0f);
            Stream<IndexedFloat> indexed = stream.indexed();
            List<IndexedFloat> result = indexed.toList();
            assertEquals(3, result.size());
            assertEquals(0, result.get(0).longIndex());
            assertEquals(10.0f, result.get(0).value(), 0.001);
            assertEquals(1, result.get(1).longIndex());
            assertEquals(20.0f, result.get(1).value(), 0.001);
        }
    }

    @Nested
    @DisplayName("Set Operations")
    public class SetOperations {

        @Test
        @DisplayName("intersection() should return common elements")
        public void testIntersection() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            Collection<Float> other = Arrays.asList(2.0f, 3.0f, 5.0f);
            float[] result = stream.intersection(other).toArray();
            Arrays.sort(result);
            assertArrayEquals(new float[] { 2.0f, 3.0f }, result);
        }

        @Test
        @DisplayName("difference() should return elements not in other collection")
        public void testDifference() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            Collection<Float> other = Arrays.asList(2.0f, 3.0f, 5.0f);
            float[] result = stream.difference(other).toArray();
            Arrays.sort(result);
            assertArrayEquals(new float[] { 1.0f, 4.0f }, result);
        }

        @Test
        @DisplayName("symmetricDifference() should return elements in either but not both")
        public void testSymmetricDifference() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            Collection<Float> other = Arrays.asList(2.0f, 3.0f, 4.0f, 5.0f);
            float[] result = stream.symmetricDifference(other).toArray();
            Arrays.sort(result);
            assertArrayEquals(new float[] { 1.0f, 4.0f, 5.0f }, result);
        }
    }

    @Nested
    @DisplayName("Mapping to Collections")
    public class MappingToCollections {

        @Test
        @DisplayName("toMap() should create map with key and value mappers")
        public void testToMap() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            Map<String, Float> result = stream.toMap(f -> "key" + (int) f, f -> f * 2);
            assertEquals(3, result.size());
            assertEquals(2.0f, result.get("key1"), 0.001);
            assertEquals(4.0f, result.get("key2"), 0.001);
            assertEquals(6.0f, result.get("key3"), 0.001);
        }

        @Test
        @DisplayName("toMap() with merge function should handle duplicate keys")
        public void testToMapWithMerge() {
            FloatStream stream = createFloatStream(1.1f, 1.9f, 2.1f);
            Map<Integer, Float> result = stream.toMap(f -> (int) f, // Keys: 1, 1, 2
                    f -> f, Float::sum // Merge duplicates
            );
            assertEquals(2, result.size());
            assertEquals(3.0f, result.get(1), 0.001); // 1.1 + 1.9
            assertEquals(2.1f, result.get(2), 0.001);
        }

        @Test
        @DisplayName("toMap() with custom map factory should use provided map type")
        public void testToMapWithFactory() {
            FloatStream stream = createFloatStream(3.0f, 1.0f, 2.0f);
            Map<String, Float> result = stream.toMap(f -> String.valueOf((int) f), f -> f, Suppliers.ofLinkedHashMap());
            assertTrue(result instanceof LinkedHashMap);
            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("groupTo() should group elements using collector")
        public void testGroupTo() {
            FloatStream stream = createFloatStream(1.1f, 1.9f, 2.1f, 2.9f);
            Map<Integer, List<Float>> result = stream.groupTo(f -> (int) f, Collectors.toList());
            assertEquals(2, result.size());
            assertEquals(2, result.get(1).size());
            assertEquals(2, result.get(2).size());
            assertTrue(result.get(1).contains(1.1f));
            assertTrue(result.get(1).contains(1.9f));
        }

        @Test
        @DisplayName("toMultiset() should return multiset with frequencies")
        public void testToMultiset() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 1.0f, 3.0f, 2.0f, 1.0f);
            Multiset<Float> result = stream.toMultiset();
            assertEquals(3, result.count(1.0f));
            assertEquals(2, result.count(2.0f));
            assertEquals(1, result.count(3.0f));
        }
    }

    @Nested
    @DisplayName("Side Effect Operations")
    public class SideEffectOperations {

        @Test
        @DisplayName("onEach() should perform action on each element")
        public void testOnEach() {
            List<Float> collected = new ArrayList<>();
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            float[] result = stream.onEach(collected::add).toArray();

            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
            assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), collected);
        }

        @Test
        @DisplayName("peek() should perform action on each element")
        public void testPeek() {
            List<Float> collected = new ArrayList<>();
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            float[] result = stream.peek(collected::add).toArray();

            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
            assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), collected);
        }

        @Test
        @DisplayName("forEach() should execute action for each element")
        public void testForEach() {
            List<Float> collected = new ArrayList<>();
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            stream.forEach(collected::add);
            assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), collected);
        }

        @Test
        @DisplayName("forEachIndexed() should provide index with each element")
        public void testForEachIndexed() {
            List<String> collected = new ArrayList<>();
            FloatStream stream = createFloatStream(10.0f, 20.0f, 30.0f);
            stream.forEachIndexed((index, value) -> collected.add(index + ":" + value));
            assertEquals(Arrays.asList("0:10.0", "1:20.0", "2:30.0"), collected);
        }
    }

    @Nested
    @DisplayName("Conditional Operations")
    public class ConditionalOperations {

        @Test
        @DisplayName("filter() with action on dropped items should execute action")
        public void testFilterWithAction() {
            List<Float> dropped = new ArrayList<>();
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            float[] result = stream.filter(x -> x > 2.0f, dropped::add).toArray();

            assertArrayEquals(new float[] { 3.0f, 4.0f }, result);
            assertEquals(Arrays.asList(1.0f, 2.0f), dropped);
        }

        @Test
        @DisplayName("dropWhile() with action should execute action on dropped items")
        public void testDropWhileWithAction() {
            List<Float> dropped = new ArrayList<>();
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 2.0f, 1.0f);
            float[] result = stream.dropWhile(x -> x < 3.0f, dropped::add).toArray();

            assertArrayEquals(new float[] { 3.0f, 2.0f, 1.0f }, result);
            assertEquals(Arrays.asList(1.0f, 2.0f), dropped);
        }

        @Test
        @DisplayName("skip() with action should execute action on skipped items")
        public void testSkipWithAction() {
            List<Float> skipped = new ArrayList<>();
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            float[] result = stream.skip(2, skipped::add).toArray();

            assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f }, result);
            assertEquals(Arrays.asList(1.0f, 2.0f), skipped);
        }

        @Test
        @DisplayName("appendIfEmpty() should append only if stream is empty")
        public void testAppendIfEmpty() {
            // Non-empty stream
            FloatStream stream1 = createFloatStream(1.0f, 2.0f);
            float[] result1 = stream1.appendIfEmpty(99.0f).toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f }, result1);

            // Empty stream
            FloatStream stream2 = FloatStream.empty();
            float[] result2 = stream2.appendIfEmpty(99.0f).toArray();
            assertArrayEquals(new float[] { 99.0f }, result2);
        }

        @Test
        @DisplayName("throwIfEmpty() should throw exception for empty stream")
        public void testThrowIfEmpty() {
            // Non-empty stream should not throw
            FloatStream stream1 = createFloatStream(1.0f);
            assertDoesNotThrow(() -> stream1.throwIfEmpty().toArray());

            // Empty stream should throw
            FloatStream stream2 = FloatStream.empty();
            assertThrows(RuntimeException.class, () -> stream2.throwIfEmpty().toArray());
        }

        @Test
        @DisplayName("ifEmpty() should execute action only for empty stream")
        public void testIfEmpty() {
            List<String> actions = new ArrayList<>();

            // Non-empty stream
            FloatStream stream1 = createFloatStream(1.0f);
            stream1.ifEmpty(() -> actions.add("empty1")).toArray();

            // Empty stream
            FloatStream stream2 = FloatStream.empty();
            stream2.ifEmpty(() -> actions.add("empty2")).toArray();

            assertEquals(Arrays.asList("empty2"), actions);
        }
    }

    @Nested
    @DisplayName("Stream Merging and Combining")
    public class StreamMerging {

        @Test
        @DisplayName("mergeWith() should merge streams using selector")
        public void testMergeWith() {
            FloatStream stream1 = createFloatStream(1.0f, 3.0f, 5.0f);
            FloatStream stream2 = createFloatStream(2.0f, 4.0f, 6.0f);
            float[] result = stream1.mergeWith(stream2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result);
        }

        @Test
        @DisplayName("zipWith() should zip with binary operator")
        public void testZipWith() {
            FloatStream stream1 = createFloatStream(1.0f, 2.0f, 3.0f);
            FloatStream stream2 = createFloatStream(4.0f, 5.0f, 6.0f);
            float[] result = stream1.zipWith(stream2, Float::sum).toArray();
            assertArrayEquals(new float[] { 5.0f, 7.0f, 9.0f }, result);
        }

        @Test
        @DisplayName("zipWith() with three streams should use ternary operator")
        public void testZipWithThreeStreams() {
            FloatStream stream1 = createFloatStream(1.0f, 2.0f);
            FloatStream stream2 = createFloatStream(3.0f, 4.0f);
            FloatStream stream3 = createFloatStream(5.0f, 6.0f);
            float[] result = stream1.zipWith(stream2, stream3, (a, b, c) -> a + b + c).toArray();
            assertArrayEquals(new float[] { 9.0f, 12.0f }, result);
        }

        @Test
        @DisplayName("zipWith() with padding should handle different lengths")
        public void testZipWithPadding() {
            FloatStream stream1 = createFloatStream(1.0f, 2.0f, 5.0f);
            FloatStream stream2 = createFloatStream(3.0f, 4.0f);
            float[] result = stream1.zipWith(stream2, 0.0f, 10.0f, Float::sum).toArray();
            assertArrayEquals(new float[] { 4.0f, 6.0f, 15.0f }, result); // 1+3, 2+4, 0+5
        }
    }

    @Nested
    @DisplayName("Statistical Operations")
    public class StatisticalOperations {

        @Test
        @DisplayName("percentiles() should return percentile map")
        public void testPercentiles() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            Optional<Map<Percentage, Float>> result = stream.percentiles();
            assertTrue(result.isPresent());
            Map<Percentage, Float> percentiles = result.get();
            assertFalse(percentiles.isEmpty());
        }

        @Test
        @DisplayName("summarizeAndPercentiles() should return both statistics and percentiles")
        public void testSummarizeAndPercentiles() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            Pair<FloatSummaryStatistics, Optional<Map<Percentage, Float>>> result = stream.summarizeAndPercentiles();

            FloatSummaryStatistics stats = result.left();
            assertEquals(5, stats.getCount());
            assertEquals(1.0f, stats.getMin(), 0.001);
            assertEquals(5.0f, stats.getMax(), 0.001);

            assertTrue(result.right().isPresent());
        }
    }

    @Nested
    @DisplayName("Stream Control and Configuration")
    public class StreamControl {

        @Test
        @DisplayName("parallel() with max thread number should configure parallelism")
        public void testParallelWithThreads() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            FloatStream parallel = stream.parallel(2);
            assertTrue(parallel.isParallel());
        }

        @Test
        @DisplayName("parallel() with executor should use custom executor")
        public void testParallelWithExecutor() {
            Executor executor = Executors.newFixedThreadPool(2);
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            FloatStream parallel = stream.parallel(executor);
            assertTrue(parallel.isParallel());
        }

        @Test
        @DisplayName("onClose() should register close handler")
        public void testOnClose() {
            List<String> closeActions = new ArrayList<>();
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f).onClose(() -> closeActions.add("closed"));

            stream.toArray(); // Should trigger close
            assertEquals(Arrays.asList("closed"), closeActions);
        }
    }

    @Nested
    @DisplayName("Search with Predicates")
    public class SearchWithPredicates {

        @Test
        @DisplayName("findFirst() with predicate should find first matching element")
        public void testFindFirstWithPredicate() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            OptionalFloat result = stream.findFirst(x -> x > 2.0f);
            assertTrue(result.isPresent());
            assertEquals(3.0f, result.get(), 0.001);
        }

        @Test
        @DisplayName("findAny() with predicate should find any matching element")
        public void testFindAnyWithPredicate() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            OptionalFloat result = stream.findAny(x -> x > 2.0f);
            assertTrue(result.isPresent());
            assertTrue(result.get() > 2.0f);
        }

        @Test
        @DisplayName("findLast() with predicate should find last matching element")
        public void testFindLastWithPredicate() {
            FloatStream stream = createFloatStream(1.0f, 3.0f, 2.0f, 4.0f);
            OptionalFloat result = stream.findLast(x -> x > 2.0f);
            assertTrue(result.isPresent());
            assertEquals(4.0f, result.get(), 0.001);
        }
    }

    @Nested
    @DisplayName("Advanced Transformations")
    public class AdvancedTransformations {

        @Test
        @DisplayName("flatMapToObj() should flatten to object stream")
        public void testFlatMapToObj() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            Stream<String> result = stream.flatMapToObj(f -> Stream.of(String.valueOf(f), String.valueOf(f * 2)));
            String[] array = result.toArray(String[]::new);
            assertArrayEquals(new String[] { "1.0", "2.0", "2.0", "4.0" }, array);
        }

        @Test
        @DisplayName("flatmapToObj() should flatten collection to object stream")
        public void testFlatmapToObj() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            Stream<String> result = stream.flatmapToObj(f -> Arrays.asList(String.valueOf(f), String.valueOf(f * 2)));
            String[] array = result.toArray(String[]::new);
            assertArrayEquals(new String[] { "1.0", "2.0", "2.0", "4.0" }, array);
        }

        @Test
        @DisplayName("flattMapToObj() should flatten array to object stream")
        public void testFlattMapToObj() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            Stream<String> result = stream.flattMapToObj(f -> new String[] { String.valueOf(f), String.valueOf(f * 2) });
            String[] array = result.toArray(String[]::new);
            assertArrayEquals(new String[] { "1.0", "2.0", "2.0", "4.0" }, array);
        }

        @Test
        @DisplayName("flatMapToInt() should flatten to IntStream")
        public void testFlatMapToInt() {
            FloatStream stream = createFloatStream(1.5f, 2.5f);
            IntStream result = stream.flatMapToInt(f -> IntStream.of((int) f, (int) (f * 2)));
            int[] array = result.toArray();
            assertArrayEquals(new int[] { 1, 3, 2, 5 }, array);
        }

        @Test
        @DisplayName("flatMapToLong() should flatten to LongStream")
        public void testFlatMapToLong() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            LongStream result = stream.flatMapToLong(f -> LongStream.of((long) f, (long) (f * 2)));
            long[] array = result.toArray();
            assertArrayEquals(new long[] { 1L, 2L, 2L, 4L }, array);
        }

        @Test
        @DisplayName("flatMapToDouble() should flatten to DoubleStream")
        public void testFlatMapToDouble() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            DoubleStream result = stream.flatMapToDouble(f -> DoubleStream.of(f, f * 2));
            double[] array = result.toArray();
            assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0 }, array, 0.001);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    public class EdgeCases {

        @Test
        @DisplayName("Empty stream operations should work correctly")
        public void testEmptyStreamOperations() {
            FloatStream empty = FloatStream.empty();

            assertEquals(0, empty.count());
            assertFalse(FloatStream.empty().first().isPresent());
            assertFalse(FloatStream.empty().min().isPresent());
            assertFalse(FloatStream.empty().max().isPresent());
            assertEquals(0.0, FloatStream.empty().sum(), 0.001);
            assertFalse(FloatStream.empty().average().isPresent());
        }

        @Test
        @DisplayName("Single element stream should work correctly")
        public void testSingleElementStream() {
            FloatStream single = createFloatStream(5.0f);

            assertEquals(1, single.count());

            single = createFloatStream(5.0f);
            assertEquals(5.0f, single.first().get(), 0.001);

            single = createFloatStream(5.0f);
            assertEquals(5.0f, single.min().get(), 0.001);

            single = createFloatStream(5.0f);
            assertEquals(5.0f, single.max().get(), 0.001);
        }

        @Test
        @DisplayName("Very large numbers should be handled correctly")
        public void testLargeNumbers() {
            float large = Float.MAX_VALUE / 2;
            FloatStream stream = createFloatStream(large, large);
            assertEquals(Float.MAX_VALUE, stream.sum(), Float.MAX_VALUE * 0.001);
        }

        @Test
        @DisplayName("Special float values should be handled correctly")
        public void testSpecialValues() {
            FloatStream stream = createFloatStream(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f);
            float[] result = stream.toArray();
            assertEquals(4, result.length);
            assertTrue(Float.isNaN(result[0]));
            assertTrue(Float.isInfinite(result[1]));
            assertTrue(Float.isInfinite(result[2]));
            assertEquals(0.0f, result[3], 0.001);
        }

        @Test
        @DisplayName("Deferred stream creation should work correctly")
        public void testDefer() {
            FloatStream deferred = FloatStream.defer(() -> createFloatStream(1.0f, 2.0f, 3.0f));
            float[] result = deferred.toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
        }

        @Test
        @DisplayName("Random stream should generate values")
        public void testRandom() {
            FloatStream randomStream = FloatStream.random();
            float[] result = randomStream.limit(5).toArray();
            assertEquals(5, result.length);
            for (float f : result) {
                assertTrue(f >= 0.0f && f < 1.0f);
            }
        }
    }
}
