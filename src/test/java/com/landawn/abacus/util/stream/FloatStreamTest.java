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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.IndexedFloat;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.function.FloatPredicate;

public class FloatStreamTest extends TestBase {

    private static final float DELTA = 0.0001f;

    protected FloatStream createFloatStream(float... a) {
        return FloatStream.of(a);
    }

    protected FloatStream createFloatStream(float[] a, int fromIndex, int toIndex) {
        return FloatStream.of(a, fromIndex, toIndex);
    }

    protected FloatStream createFloatStream(Float[] a) {
        return FloatStream.of(a);
    }

    protected FloatStream createFloatStream(Float[] a, int fromIndex, int toIndex) {
        return FloatStream.of(a, fromIndex, toIndex);
    }

    protected FloatStream createFloatStream(Collection<Float> coll) {
        return FloatStream.of(coll.toArray(new Float[coll.size()]));
    }

    protected FloatStream createFloatStream(FloatIterator iter) {
        return iter == null ? FloatStream.empty() : FloatStream.of(iter.toArray());
    }

    protected FloatStream createFloatStream(FloatBuffer buff) {
        return FloatStream.of(buff);
    }

    @Nested
    @DisplayName("Factory Methods - Static")
    public class FactoryMethods {
    }

    @Nested
    @DisplayName("Zip Operations")
    public class ZipOperations {
    }

    @Nested
    @DisplayName("Merge Operations")
    public class MergeOperations {
    }

    @Nested
    @DisplayName("Intermediate Operations - Filtering")
    public class FilteringOperations {
    }

    @Nested
    @DisplayName("Intermediate Operations - Transformation")
    public class TransformationOperations {
    }

    @Nested
    @DisplayName("Intermediate Operations - Advanced")
    public class AdvancedOperations {
    }

    @Nested
    @DisplayName("Intermediate Operations - Sorting")
    public class SortingOperations {
    }

    @Nested
    @DisplayName("Intermediate Operations - Set Operations")
    public class SetOperations {
    }

    @Nested
    @DisplayName("Intermediate Operations - Instance Zip and Merge")
    public class InstanceZipMerge {
    }

    @Nested
    @DisplayName("Intermediate Operations - Side Effects")
    public class SideEffects {
    }

    @Nested
    @DisplayName("Intermediate Operations - Conditional")
    public class ConditionalOperations {
    }

    @Nested
    @DisplayName("Terminal Operations - Aggregation")
    public class AggregationOperations {
    }

    @Nested
    @DisplayName("Terminal Operations - Reduction")
    public class ReductionOperations {
    }

    @Nested
    @DisplayName("Terminal Operations - Search")
    public class SearchOperations {
    }

    @Nested
    @DisplayName("Terminal Operations - Matching")
    public class MatchingOperations {
    }

    @Nested
    @DisplayName("Terminal Operations - Collection")
    public class CollectionOperations {
    }

    @Nested
    @DisplayName("Terminal Operations - ForEach")
    public class ForEachOperations {
    }

    @Nested
    @DisplayName("Terminal Operations - Joining")
    public class JoiningOperations {
    }

    @Nested
    @DisplayName("BaseStream - Parallelism")
    public class ParallelismOperations {
    }

    @Nested
    @DisplayName("BaseStream - Lifecycle")
    public class LifecycleOperations {
    }

    @Nested
    @DisplayName("BaseStream - Iterator")
    public class IteratorOperations {
    }

    @Nested
    @DisplayName("Edge Cases")
    public class EdgeCases {
    }

    @Test
    @DisplayName("filter() should filter elements")
    public void testFilter() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f }, stream.filter(x -> x > 2.0f).toArray());
    }

    @Test
    @DisplayName("filter() with action on dropped items")
    public void testFilterWithAction() {
        List<Float> dropped = new ArrayList<>();
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f }, stream.filter(x -> x > 2.0f, dropped::add).toArray());
        assertEquals(Arrays.asList(1.0f, 2.0f), dropped);
    }

    @Test
    public void testFilterWithActionOnDroppedItem() {
        final FloatList droppedItems = new FloatList();
        FloatPredicate predicate = f -> f > 3f;
        FloatStream.of(1f, 2f, 3f, 4f, 5f).filter(predicate, droppedItems::add).count();

        assertArrayEquals(new float[] { 1f, 2f, 3f }, droppedItems.toArray(), DELTA);
    }

    @Test
    public void testSps() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).sps(s -> s.filter(f -> f > 2.0f)).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testSpsWithExecutor() {
        java.util.concurrent.Executor executor = Executors.newFixedThreadPool(2);
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).sps(2, executor, s -> s.filter(f -> f > 2.0f)).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testPspMethod() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).psp(s -> s.filter(f -> f > 2.0f)).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testTransformMethod() {
        long count = createFloatStream(1.0f, 2.0f, 3.0f).transform(s -> s.filter(f -> f > 1.0f)).count();
        assertEquals(2, count);
    }

    @Test
    public void testSpecialValues_NaN() {
        float[] result = createFloatStream(Float.NaN, 1.0f, Float.NaN).filter(f -> !Float.isNaN(f)).toArray();
        assertArrayEquals(new float[] { 1.0f }, result, DELTA);
    }

    @Test
    public void testSpsWithMaxThreadNum() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).sps(2, s -> s.filter(f -> f > 2.0f)).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testFilter_Empty() {
        float[] result = FloatStream.empty().filter(f -> f > 0).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testSpecialValues_Infinity() {
        float[] result = createFloatStream(Float.POSITIVE_INFINITY, 1.0f, Float.NEGATIVE_INFINITY).filter(f -> !Float.isInfinite(f)).toArray();
        assertArrayEquals(new float[] { 1.0f }, result, DELTA);
    }

    @Test
    @DisplayName("null arguments should throw")
    public void testNullArguments() {
        assertThrows(NullPointerException.class, () -> createFloatStream(1.0f, 2.0f).map(null).count());
        assertThrows(NullPointerException.class, () -> createFloatStream(1.0f, 2.0f).filter(null).count());
    }

    @Test
    @DisplayName("takeWhile() should take elements while predicate is true")
    public void testTakeWhile() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 2.0f, 1.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f }, stream.takeWhile(x -> x < 3.0f).toArray());
    }

    @Test
    public void testTakeWhile_AllMatch() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).takeWhile(f -> f < 10.0f).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testTakeWhile_NoneMatch() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).takeWhile(f -> f < 0.0f).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("dropWhile() should drop elements while predicate is true")
    public void testDropWhile() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 2.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f, 2.0f }, stream.dropWhile(x -> x < 3.0f).toArray());
    }

    @Test
    @DisplayName("dropWhile() with action on dropped items")
    public void testDropWhileWithAction() {
        List<Float> dropped = new ArrayList<>();
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f }, stream.dropWhile(x -> x < 3.0f, dropped::add).toArray());
        assertEquals(Arrays.asList(1.0f, 2.0f), dropped);
    }

    @Test
    public void testDropWhileWithActionOnDroppedItem() {
        final FloatList droppedItems = new FloatList();
        FloatStream.of(1f, 2f, 3f, 4f, 1f).dropWhile(f -> f < 4f, droppedItems::add).count();

        assertArrayEquals(new float[] { 1f, 2f, 3f }, droppedItems.toArray(), DELTA);
    }

    @Test
    public void testDropWhile_AllMatch() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).dropWhile(f -> f < 10.0f).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testDropWhile_NoneMatch() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).dropWhile(f -> f > 10.0f).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    @DisplayName("map() should transform elements")
    public void testMap() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new float[] { 2.0f, 4.0f, 6.0f }, stream.map(x -> x * 2).toArray());
    }

    @Test
    public void testMap_Empty() {
        float[] result = FloatStream.empty().map(f -> f * 2).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("mapToInt() should convert to IntStream")
    public void testMapToInt() {
        FloatStream stream = createFloatStream(1.5f, 2.5f, 3.5f);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.mapToInt(x -> (int) x).toArray());
    }

    @Test
    public void testMapToInt_Empty() {
        int[] result = FloatStream.empty().mapToInt(f -> (int) f).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("mapToLong() should convert to LongStream")
    public void testMapToLong() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, stream.mapToLong(x -> (long) x).toArray());
    }

    @Test
    public void testMapToLong_Empty() {
        long[] result = FloatStream.empty().mapToLong(f -> (long) f).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("mapToDouble() should convert to DoubleStream")
    public void testMapToDouble() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.mapToDouble(x -> x).toArray(), 0.001);
    }

    @Test
    @DisplayName("parallel operations should work correctly")
    public void testParallelProcessing() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        double sum = stream.parallel().mapToDouble(x -> x).sum();
        assertEquals(15.0, sum, 0.001);
    }

    @Test
    public void testMapToDouble_Empty() {
        double[] result = FloatStream.empty().mapToDouble(f -> (double) f).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("mapToObj() should convert to object stream")
    public void testMapToObj() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new String[] { "1.0", "2.0", "3.0" }, stream.mapToObj(String::valueOf).toArray(String[]::new));
    }

    @Test
    public void testMapToObj_Empty() {
        List<String> result = FloatStream.empty().mapToObj(String::valueOf).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("flatMap() should flatten streams")
    public void testFlatMap() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 2.0f, 4.0f }, stream.flatMap(x -> FloatStream.of(x, x * 2)).toArray());
    }

    @Test
    @DisplayName("flatmap() should flatten arrays")
    public void testFlatmapArray() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 2.0f, 4.0f }, stream.flatmap(x -> new float[] { x, x * 2 }).toArray());
    }

    @Test
    public void testFlatMap_Empty() {
        float[] result = FloatStream.empty().flatMap(f -> FloatStream.of(f, f * 2)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("flatMapToInt() should flatten to IntStream")
    public void testFlatMapToInt() {
        FloatStream stream = createFloatStream(1.5f, 2.5f);
        assertArrayEquals(new int[] { 1, 3, 2, 5 }, stream.flatMapToInt(f -> IntStream.of((int) f, (int) (f * 2))).toArray());
    }

    @Test
    public void testFlatMapToInt_Empty() {
        int[] result = FloatStream.empty().flatMapToInt(f -> IntStream.of((int) f)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("flatMapToLong() should flatten to LongStream")
    public void testFlatMapToLong() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new long[] { 1L, 2L, 2L, 4L }, stream.flatMapToLong(f -> LongStream.of((long) f, (long) (f * 2))).toArray());
    }

    @Test
    public void testFlatMapToLong_Empty() {
        long[] result = FloatStream.empty().flatMapToLong(f -> LongStream.of((long) f)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("flatMapToDouble() should flatten to DoubleStream")
    public void testFlatMapToDouble() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0 }, stream.flatMapToDouble(f -> DoubleStream.of(f, f * 2)).toArray(), 0.001);
    }

    @Test
    public void testFlatMapToDouble_Empty() {
        double[] result = FloatStream.empty().flatMapToDouble(f -> DoubleStream.of((double) f)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("flatMapToObj() should flatten to object stream")
    public void testFlatMapToObj() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new String[] { "1.0", "2.0", "2.0", "4.0" },
                stream.flatMapToObj(f -> Stream.of(String.valueOf(f), String.valueOf(f * 2))).toArray(String[]::new));
    }

    @Test
    @DisplayName("flatmapToObj() should flatten collection to object stream")
    public void testFlatmapToObj() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new String[] { "1.0", "2.0", "2.0", "4.0" },
                stream.flatmapToObj(f -> Arrays.asList(String.valueOf(f), String.valueOf(f * 2))).toArray(String[]::new));
    }

    @Test
    @DisplayName("flatMapArrayToObj() should flatten array to object stream")
    public void testFlattmapToObj() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new String[] { "1.0", "2.0", "2.0", "4.0" },
                stream.flatMapArrayToObj(f -> new String[] { String.valueOf(f), String.valueOf(f * 2) }).toArray(String[]::new));
    }

    @Test
    @DisplayName("flatMapArrayToObj() should flatten array to object stream")
    public void testFlattMapToObj() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        Stream<String> result = stream.flatMapArrayToObj(f -> new String[] { String.valueOf(f), String.valueOf(f * 2) });
        String[] array = result.toArray(String[]::new);
        assertArrayEquals(new String[] { "1.0", "2.0", "2.0", "4.0" }, array);
    }

    @Test
    public void testFlatMapArrayToObj_Test() {
        List<String> result = createFloatStream(1.0f, 2.0f).flatMapArrayToObj(f -> new String[] { "A" + f, "B" + f }).toList();
        assertEquals(4, result.size());
        assertTrue(result.get(0).startsWith("A"));
    }

    @Test
    public void testFlatMapArrayToObj_Empty() {
        List<String> result = FloatStream.empty().flatMapArrayToObj(f -> new String[] { "A" + f }).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapPartial_AllPresent() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).mapPartial(f -> OptionalFloat.of(f * 10)).toArray();
        assertArrayEquals(new float[] { 10.0f, 20.0f, 30.0f }, result, DELTA);
    }

    @Test
    @DisplayName("mapPartial() should filter and map partial results")
    public void testMapPartial() {
        FloatStream stream = createFloatStream(1.0f, -2.0f, 3.0f, -4.0f);
        assertArrayEquals(new float[] { 2.0f, 6.0f }, stream.mapPartial(x -> x > 0 ? OptionalFloat.of(x * 2) : OptionalFloat.empty()).toArray());
    }

    @Test
    public void testMapPartial_AllEmpty() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).mapPartial(f -> OptionalFloat.empty()).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("rangeMap() should map ranges")
    public void testRangeMap() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 6.0f, 7.0f);
        float[] result = stream.rangeMap((a, b) -> Math.abs(b - a) <= 1.5f, (start, end) -> end - start).toArray();
        assertTrue(result.length > 0);
    }

    @Test
    public void testRangeMap_SingleElement() {
        float[] result = createFloatStream(5.0f).rangeMap((first, next) -> next - first < 2, (first, last) -> first + last).toArray();
        assertArrayEquals(new float[] { 10.0f }, result, DELTA);
    }

    @Test
    public void testRangeMap_Empty() {
        float[] result = FloatStream.empty().rangeMap((first, next) -> next - first < 2, (first, last) -> first + last).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("rangeMapToObj() should map ranges to objects")
    public void testRangeMapToObj() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 5.0f, 6.0f);
        String[] result = stream.rangeMapToObj((a, b) -> Math.abs(b - a) <= 1.5f, (start, end) -> String.format("%.1f-%.1f", start, end))
                .toArray(String[]::new);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("collapse() should group consecutive elements")
    public void testCollapse() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 5.0f, 6.0f, 7.0f);
        Stream<FloatList> result = stream.collapse((a, b) -> Math.abs(b - a) <= 1.5f);
        List<FloatList> groups = result.toList();
        assertTrue(groups.size() >= 2);
    }

    @Test
    @DisplayName("collapse() with merge function")
    public void testCollapseWithMerge() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 5.0f, 6.0f, 7.0f);
        float[] result = stream.collapse((a, b) -> Math.abs(b - a) <= 1.5f, Float::sum).toArray();
        assertTrue(result.length >= 2);
    }

    @Test
    @DisplayName("collapse() with tri-predicate and merge function")
    public void testCollapseWithTriPredicate() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 5.0f, 6.0f, 7.0f);
        float[] result = stream.collapse((first, last, next) -> Math.abs(next - first) <= 3.0f, Float::sum).toArray();
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("collapse() with no collapsible elements additional")
    public void testCollapseNoCollapsibleAdditional() {
        FloatStream stream = FloatStream.of(1.0f, 10.0f, 20.0f, 30.0f);
        float[] result = stream.collapse((a, b) -> Math.abs(b - a) <= 2.0f, Float::sum).toArray();
        assertTrue(result.length > 0);
    }

    @Test
    public void testCollapse_Empty() {
        List<FloatList> result = FloatStream.empty().collapse((a, b) -> b - a < 2).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollapse_SingleElement() {
        List<FloatList> result = createFloatStream(5.0f).collapse((a, b) -> b - a < 2).toList();
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).size());
    }

    @Test
    @DisplayName("scan() should return cumulative results")
    public void testScan() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 6.0f }, stream.scan(Float::sum).toArray());
    }

    @Test
    @DisplayName("scan() with init should include initial value")
    public void testScanWithInit() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new float[] { 11.0f, 13.0f, 16.0f }, stream.scan(10.0f, Float::sum).toArray());
    }

    @Test
    @DisplayName("scan() with init and initIncluded flag")
    public void testScanWithInitIncluded() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        float[] result = stream.scan(10.0f, true, Float::sum).toArray();
        assertEquals(4, result.length);
        assertEquals(10.0f, result[0], 0.001);
    }

    @Test
    @DisplayName("scan() on empty stream additional")
    public void testScanEmptyAdditional() {
        FloatStream emptyStream = FloatStream.empty();
        float[] result = emptyStream.scan(Float::sum).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testScan_Empty() {
        float[] result = FloatStream.empty().scan((a, b) -> a + b).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testScanWithInit_Empty() {
        float[] result = FloatStream.empty().scan(10.0f, (a, b) -> a + b).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testScanWithInitIncluded_Empty() {
        float[] result = FloatStream.empty().scan(10.0f, true, (a, b) -> a + b).toArray();
        assertArrayEquals(new float[] { 10.0f }, result, DELTA);
    }

    @Test
    @DisplayName("prepend() should add elements to beginning")
    public void testPrepend() {
        FloatStream stream = createFloatStream(3.0f, 4.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.prepend(1.0f, 2.0f).toArray());
    }

    @Test
    public void testPrependAndAppend() {
        OptionalFloat opf = OptionalFloat.of(99.9f);
        FloatStream other = FloatStream.of(0.1f, 0.2f);

        float[] result = FloatStream.of(1.1f, 2.2f).prepend(opf).append(-1.1f, -2.2f).append(other).toArray();

        float[] expected = { 99.9f, 1.1f, 2.2f, -1.1f, -2.2f, 0.1f, 0.2f };
        assertArrayEquals(expected, result, DELTA);
    }

    @Test
    public void testPrependStream() {
        float[] result = createFloatStream(3.0f, 4.0f, 5.0f).prepend(FloatStream.of(1.0f, 2.0f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result, DELTA);
    }

    @Test
    public void testPrependOptional() {
        float[] result = createFloatStream(2.0f, 3.0f).prepend(OptionalFloat.of(1.0f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testPrependOptional_Empty() {
        float[] result = createFloatStream(2.0f, 3.0f).prepend(OptionalFloat.empty()).toArray();
        assertArrayEquals(new float[] { 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testPrepend_Empty() {
        float[] result = FloatStream.empty().prepend(1.0f, 2.0f).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f }, result, DELTA);
    }

    @Test
    @DisplayName("append() should add elements to end")
    public void testAppend() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.append(3.0f, 4.0f).toArray());
    }

    @Test
    public void testAppendStream() {
        float[] result = createFloatStream(1.0f, 2.0f).append(FloatStream.of(3.0f, 4.0f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    public void testAppendOptional() {
        float[] result = createFloatStream(1.0f, 2.0f).append(OptionalFloat.of(3.0f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testAppendOptional_Empty() {
        float[] result = createFloatStream(1.0f, 2.0f).append(OptionalFloat.empty()).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f }, result, DELTA);
    }

    @Test
    public void testAppend_Empty() {
        float[] result = FloatStream.empty().append(1.0f, 2.0f).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f }, result, DELTA);
    }

    @Test
    @DisplayName("appendIfEmpty() should append only if stream is empty")
    public void testAppendIfEmpty() {
        FloatStream stream1 = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f }, stream1.appendIfEmpty(99.0f).toArray());

        FloatStream stream2 = FloatStream.empty();
        assertArrayEquals(new float[] { 99.0f }, stream2.appendIfEmpty(99.0f).toArray());
    }

    @Test
    public void testAppendIfEmptySupplier() {
        float[] result = FloatStream.empty().appendIfEmpty(() -> FloatStream.of(10.0f, 20.0f)).toArray();
        assertArrayEquals(new float[] { 10.0f, 20.0f }, result, DELTA);
    }

    @Test
    public void testAppendIfEmptySupplier_NonEmpty() {
        float[] result = createFloatStream(1.0f, 2.0f).appendIfEmpty(() -> FloatStream.of(10.0f, 20.0f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f }, result, DELTA);
    }

    @Test
    @DisplayName("top() should return top n elements")
    public void testTop() {
        FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f, 5.0f);
        float[] result = stream.top(3).toArray();
        Arrays.sort(result);
        assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f }, result);
    }

    @Test
    @DisplayName("top() with comparator should return top n elements")
    public void testTopWithComparator() {
        FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f, 5.0f);
        float[] result = stream.top(3, (a, b) -> Float.compare(b, a)).toArray();
        Arrays.sort(result);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
    }

    @Test
    public void testTop_MoreThanAvailable() {
        float[] result = createFloatStream(1.0f, 2.0f).top(5).toArray();
        assertEquals(2, result.length);
    }

    @Test
    public void testTop_Empty() {
        float[] result = FloatStream.empty().top(3).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("toFloatList() should return FloatList")
    public void testToFloatList() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        FloatList result = stream.toFloatList();
        assertEquals(3, result.size());
        assertEquals(1.0f, result.get(0), 0.001);
    }

    @Test
    public void testToFloatList_Empty() {
        FloatList result = FloatStream.empty().toFloatList();
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("toMap() should create map")
    public void testToMap() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        Map<String, Float> result = stream.toMap(f -> "key" + (int) f, f -> f * 2);
        assertEquals(3, result.size());
        assertEquals(2.0f, result.get("key1"), 0.001);
    }

    @Test
    @DisplayName("toMap() with merge function")
    public void testToMapWithMerge() {
        FloatStream stream = createFloatStream(1.1f, 1.9f, 2.1f);
        Map<Integer, Float> result = stream.toMap(f -> (int) f, f -> f, Float::sum);
        assertEquals(2, result.size());
        assertEquals(3.0f, result.get(1), 0.001);
    }

    @Test
    @DisplayName("toMap() with map factory")
    public void testToMapWithFactory() {
        FloatStream stream = createFloatStream(3.0f, 1.0f, 2.0f);
        Map<String, Float> result = stream.toMap(f -> String.valueOf((int) f), f -> f, Suppliers.ofLinkedHashMap());
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testToMapWithFactoryAndMerge() {
        Map<Integer, Float> map = FloatStream.of(1.1f, 2.9f, 1.8f, 3.5f).toMap(f -> (int) Math.floor(f), f -> f, (v1, v2) -> v1 + v2, HashMap::new);

        assertEquals(1.1f + 1.8f, map.get(1), DELTA);
        assertEquals(2.9f, map.get(2), DELTA);
        assertEquals(3.5f, map.get(3), DELTA);
    }

    @Test
    public void testToMap_Test() {
        Map<String, Float> map = createFloatStream(1.0f, 2.0f, 3.0f).toMap(n -> "key" + n, n -> n * 10);
        assertEquals(3, map.size());
    }

    @Test
    public void testToMap_WithMerge_Test() {
        Map<Boolean, Float> map = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f).toMap(n -> n % 2 == 0, n -> n, Float::sum);
        assertEquals(2, map.size());
    }

    @Test
    public void testToMap_WithMapSupplier_Test() {
        Map<String, Float> map = createFloatStream(1.0f, 2.0f, 3.0f).toMap(n -> "key" + n, n -> n * 10, Suppliers.ofLinkedHashMap());
        assertEquals(3, map.size());
    }

    @Test
    public void testToMap_WithMergeAndSupplier() {
        LinkedHashMap<Boolean, Float> map = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f).toMap(n -> n % 2 == 0, n -> n, Float::sum, LinkedHashMap::new);
        assertEquals(2, map.size());
    }

    @Test
    public void testToMapWithMergeAndFactory() {
        LinkedHashMap<Integer, Float> result = createFloatStream(1.0f, 2.0f, 3.0f, 1.5f).toMap(f -> (int) (float) f, f -> f, (a, b) -> a + b,
                LinkedHashMap::new);
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(2.5f, result.get(1), DELTA);
    }

    @Test
    public void testToMap_DuplicateKeys() {
        assertThrows(Exception.class, () -> createFloatStream(1.0f, 1.0f).toMap(f -> f, f -> f));
    }

    @Test
    @DisplayName("groupTo() should group elements")
    public void testGroupTo() {
        FloatStream stream = createFloatStream(1.1f, 1.9f, 2.1f, 2.9f);
        Map<Integer, List<Float>> result = stream.groupTo(f -> (int) f, Collectors.toList());
        assertEquals(2, result.size());
        assertEquals(2, result.get(1).size());
    }

    @Test
    public void testGroupToWithFactory() {
        Map<String, FloatList> map = FloatStream.of(1f, 2f, 3f, 4f).groupTo(f -> f % 2 == 0 ? "even" : "odd", Collectors.toFloatList(), HashMap::new);

        assertTrue(map instanceof HashMap);
        assertArrayEquals(new float[] { 2f, 4f }, map.get("even").toArray(), DELTA);
        assertArrayEquals(new float[] { 1f, 3f }, map.get("odd").toArray(), DELTA);
    }

    @Test
    public void testGroupTo_Test() {
        Map<Boolean, List<Float>> grouped = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList());
        assertEquals(2, grouped.size());
    }

    @Test
    public void testGroupTo_WithMapSupplier() {
        LinkedHashMap<Boolean, List<Float>> grouped = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList(),
                LinkedHashMap::new);
        assertEquals(2, grouped.size());
    }

    @Test
    public void testGroupTo_Empty() {
        Map<String, List<Float>> result = FloatStream.empty().boxed().groupTo(f -> f > 2.0f ? "big" : "small");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("reduce() with identity should return value")
    public void testReduceWithIdentity() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertEquals(6.0f, stream.reduce(0.0f, Float::sum), 0.001);
    }

    @Test
    @DisplayName("reduce() without identity should return Optional")
    public void testReduceWithoutIdentity() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        OptionalFloat result = stream.reduce(Float::sum);
        assertTrue(result.isPresent());
        assertEquals(6.0f, result.get(), 0.001);
    }

    @Test
    public void testReduce() {
        OptionalFloat result = FloatStream.of(1.0f, 2.0f, 3.0f).reduce((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(6.0f, result.get(), DELTA);
    }

    @Test
    public void testReduce_WithIdentity_Test() {
        float result = createFloatStream(1.0f, 2.0f, 3.0f).reduce(0.0f, Float::sum);
        assertEquals(6.0f, result, DELTA);
    }

    @Test
    public void testReduce_WithoutIdentity_Test() {
        OptionalFloat result = createFloatStream(1.0f, 2.0f, 3.0f).reduce(Float::sum);
        assertTrue(result.isPresent());
        assertEquals(6.0f, result.getAsFloat(), DELTA);
    }

    @Test
    @DisplayName("reduce() of empty stream should return empty")
    public void testReduceEmpty() {
        FloatStream stream = FloatStream.empty();
        OptionalFloat result = stream.reduce(Float::sum);
        assertFalse(result.isPresent());
    }

    @Test
    public void testReduce_Empty_Test() {
        OptionalFloat result = FloatStream.empty().reduce(Float::sum);
        assertFalse(result.isPresent());
    }

    @Test
    public void testReduce_Empty() {
        OptionalFloat result = FloatStream.empty().reduce((a, b) -> a + b);
        assertFalse(result.isPresent());
    }

    @Test
    public void testReduce_SingleElement() {
        OptionalFloat result = createFloatStream(5.0f).reduce((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(5.0f, result.get(), DELTA);
    }

    @Test
    @DisplayName("collect() with supplier, accumulator, combiner")
    public void testCollect() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        StringBuilder result = stream.collect(StringBuilder::new, (sb, f) -> sb.append(f).append(","), StringBuilder::append);
        assertEquals("1.0,2.0,3.0,", result.toString());
    }

    @Test
    public void testCollectWithCombiner() {
        ArrayList<Float> result = FloatStream.of(1.1f, 2.2f, 3.3f).parallel().collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        result.sort(Comparator.naturalOrder());
        assertEquals(Arrays.asList(1.1f, 2.2f, 3.3f), result);
    }

    @Test
    public void testCollect_WithoutCombiner() {
        List<Float> result = createFloatStream(1.0f, 2.0f, 3.0f).collect(ArrayList::new, (list, f) -> list.add(f));
        assertEquals(3, result.size());
    }

    @Test
    public void testCollect_WithCombiner() {
        List<Float> result = createFloatStream(1.0f, 2.0f, 3.0f).collect(ArrayList::new, (list, f) -> list.add(f), (l1, l2) -> l1.addAll(l2));
        assertEquals(3, result.size());
    }

    @Test
    public void testCollectWithSupplierAndAccumulator() {
        List<Float> result = createFloatStream(1.0f, 2.0f, 3.0f).collect(ArrayList::new, (list, f) -> list.add(f));
        assertEquals(3, result.size());
        assertEquals(1.0f, result.get(0), DELTA);
    }

    @Test
    @DisplayName("forEach() should execute action")
    public void testForEach() {
        List<Float> collected = new ArrayList<>();
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        stream.forEach(collected::add);
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), collected);
    }

    @Test
    public void testForeach_Empty() {
        List<Float> collected = new ArrayList<>();
        FloatStream.empty().forEach(f -> collected.add(f));
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testAcceptIfNotEmpty() {
        List<Float> collected = new ArrayList<>();
        createFloatStream(1.0f, 2.0f, 3.0f).acceptIfNotEmpty(s -> s.forEach(collected::add));
        assertEquals(3, collected.size());
    }

    @Test
    public void testForEach_Empty() {
        List<Float> collected = new ArrayList<>();
        FloatStream.empty().forEach(collected::add);
        assertTrue(collected.isEmpty());
    }

    @Test
    @DisplayName("forEachIndexed() should provide index")
    public void testForEachIndexed() {
        List<String> collected = new ArrayList<>();
        FloatStream stream = createFloatStream(10.0f, 20.0f, 30.0f);
        stream.forEachIndexed((index, value) -> collected.add(index + ":" + value));
        assertEquals(Arrays.asList("0:10.0", "1:20.0", "2:30.0"), collected);
    }

    @Test
    public void testForEachIndexed_Test() {
        Map<Integer, Float> indexToValue = new HashMap<>();
        createFloatStream(10.0f, 20.0f, 30.0f).forEachIndexed((index, value) -> indexToValue.put(index, value));
        assertEquals(3, indexToValue.size());
        assertEquals(10.0f, indexToValue.get(0), DELTA);
    }

    @Test
    public void testForEachIndexed_Empty() {
        List<Float> collected = new ArrayList<>();
        FloatStream.empty().forEachIndexed((idx, val) -> collected.add(val));
        assertTrue(collected.isEmpty());
    }

    @Test
    @DisplayName("anyMatch() should return true if any matches")
    public void testAnyMatch() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertTrue(stream.anyMatch(x -> x > 2.0f));
    }

    @Test
    public void testAnyMatch_Test() {
        assertTrue(createFloatStream(1.0f, 2.0f, 3.0f).anyMatch(f -> f > 2.0f));
        assertFalse(createFloatStream(1.0f, 2.0f, 3.0f).anyMatch(f -> f > 5.0f));
    }

    @Test
    public void testAnyMatch_Empty() {
        assertFalse(FloatStream.empty().anyMatch(f -> true));
    }

    @Test
    @DisplayName("allMatch() should return true if all match")
    public void testAllMatch() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertTrue(stream.allMatch(x -> x > 0.0f));

        FloatStream stream2 = createFloatStream(1.0f, 2.0f, 3.0f);
        assertFalse(stream2.allMatch(x -> x > 2.0f));
    }

    @Test
    public void testAllMatch_Test() {
        assertTrue(createFloatStream(1.0f, 2.0f, 3.0f).allMatch(f -> f > 0.0f));
        assertFalse(createFloatStream(1.0f, 2.0f, 3.0f).allMatch(f -> f > 1.0f));
    }

    @Test
    public void testAllMatch_Empty() {
        assertTrue(FloatStream.empty().allMatch(f -> false));
    }

    @Test
    @DisplayName("noneMatch() should return true if none match")
    public void testNoneMatch() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertTrue(stream.noneMatch(x -> x > 5.0f));

        FloatStream stream2 = createFloatStream(1.0f, 2.0f, 3.0f);
        assertFalse(stream2.noneMatch(x -> x > 2.0f));
    }

    @Test
    public void testNoneMatch_Test() {
        assertTrue(createFloatStream(1.0f, 2.0f, 3.0f).noneMatch(f -> f > 5.0f));
        assertFalse(createFloatStream(1.0f, 2.0f, 3.0f).noneMatch(f -> f > 2.0f));
    }

    @Test
    public void testNoneMatch_Empty() {
        assertTrue(FloatStream.empty().noneMatch(f -> true));
    }

    @Test
    public void testNoneMatch_True() {
        assertTrue(createFloatStream(1.0f, 2.0f, 3.0f).noneMatch(f -> f > 10.0f));
    }

    @Test
    public void testNoneMatch_False() {
        assertFalse(createFloatStream(1.0f, 2.0f, 3.0f).noneMatch(f -> f > 2.0f));
    }

    @Test
    @DisplayName("findFirst() with predicate should find first matching")
    public void testFindFirstWithPredicate() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        OptionalFloat result = stream.findFirst(x -> x > 2.0f);
        assertTrue(result.isPresent());
        assertEquals(3.0f, result.get(), 0.001);
    }

    @Test
    public void testFindFirst_NoArg() {
        OptionalFloat result = createFloatStream(10.0f, 20.0f, 30.0f).findFirst();
        assertTrue(result.isPresent());
        assertEquals(10.0f, result.getAsFloat(), DELTA);
    }

    @Test
    public void testFindFirst_WithPredicate_Test() {
        OptionalFloat result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f).findFirst(f -> f > 2.0f);
        assertTrue(result.isPresent());
        assertEquals(3.0f, result.getAsFloat(), DELTA);
    }

    @Test
    public void testFindFirstLast() {
        assertEquals(1.0f, FloatStream.of(1.0f, 2.0f, 3.0f).first().get(), DELTA);
        assertEquals(3.0f, FloatStream.of(1.0f, 2.0f, 3.0f).last().get(), DELTA);
        assertFalse(FloatStream.empty().first().isPresent());
    }

    @Test
    @DisplayName("findFirst() of empty stream should return empty")
    public void testFindFirstEmpty() {
        FloatStream stream = FloatStream.empty();
        OptionalFloat first = stream.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testFindFirst_NoArg_Empty() {
        OptionalFloat result = FloatStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirst_Empty() {
        OptionalFloat result = FloatStream.empty().findFirst(f -> f > 0);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("findAny() with predicate should find any matching")
    public void testFindAnyWithPredicate() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        OptionalFloat result = stream.findAny(x -> x > 2.0f);
        assertTrue(result.isPresent());
        assertTrue(result.get() > 2.0f);
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
    public void testFindAny_NoArg() {
        OptionalFloat result = createFloatStream(10.0f, 20.0f, 30.0f).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_WithPredicate_Test() {
        OptionalFloat result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f).findAny(f -> f > 2.0f);
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_NoArg_Empty() {
        OptionalFloat result = FloatStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAny_Empty() {
        OptionalFloat result = FloatStream.empty().findAny(f -> f > 0);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("findLast() with predicate should find last matching")
    public void testFindLastWithPredicate() {
        FloatStream stream = createFloatStream(1.0f, 3.0f, 2.0f, 4.0f);
        OptionalFloat result = stream.findLast(x -> x > 2.0f);
        assertTrue(result.isPresent());
        assertEquals(4.0f, result.get(), 0.001);
    }

    @Test
    public void testFindLast_WithPredicate_Test() {
        OptionalFloat result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f).findLast(f -> f < 3.0f);
        assertTrue(result.isPresent());
        assertEquals(2.0f, result.getAsFloat(), DELTA);
    }

    @Test
    public void testFindLast() {
        OptionalFloat result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).findLast(f -> f < 4.0f);
        assertTrue(result.isPresent());
        assertEquals(3.0f, result.get(), DELTA);
    }

    @Test
    public void testFindLast_Empty() {
        OptionalFloat result = FloatStream.empty().findLast(f -> f > 0);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("min() should return minimum")
    public void testMin() {
        FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f);
        OptionalFloat min = stream.min();
        assertTrue(min.isPresent());
        assertEquals(1.0f, min.get(), 0.001);
    }

    @Test
    public void testMinMax() {
        assertTrue(FloatStream.of(3.1f, 1.2f, 5.3f).min().isPresent());
        assertEquals(1.2f, FloatStream.of(3.1f, 1.2f, 5.3f).min().get(), DELTA);
        assertTrue(FloatStream.of(3.1f, 1.2f, 5.3f).max().isPresent());
        assertEquals(5.3f, FloatStream.of(3.1f, 1.2f, 5.3f).max().get(), DELTA);
    }

    @Test
    public void testMin_Test() {
        OptionalFloat result = createFloatStream(3.0f, 1.0f, 2.0f).min();
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.getAsFloat(), DELTA);
    }

    @Test
    public void testMin_Empty() {
        OptionalFloat result = FloatStream.empty().min();
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("max() should return maximum")
    public void testMax() {
        FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f);
        OptionalFloat max = stream.max();
        assertTrue(max.isPresent());
        assertEquals(4.0f, max.get(), 0.001);
    }

    @Test
    public void testMax_Test() {
        OptionalFloat result = createFloatStream(3.0f, 1.0f, 2.0f).max();
        assertTrue(result.isPresent());
        assertEquals(3.0f, result.getAsFloat(), DELTA);
    }

    @Test
    public void testMax_Empty() {
        OptionalFloat result = FloatStream.empty().max();
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("kthLargest() should return kth largest")
    public void testKthLargest() {
        FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f, 5.0f);
        OptionalFloat kth = stream.kthLargest(2);
        assertTrue(kth.isPresent());
        assertEquals(4.0f, kth.get(), 0.001);
    }

    @Test
    public void testKthLargest_Test() {
        OptionalFloat result = createFloatStream(5.0f, 3.0f, 1.0f, 4.0f, 2.0f).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4.0f, result.getAsFloat(), DELTA);
    }

    @Test
    public void testKthLargest_SingleElement() {
        OptionalFloat result = createFloatStream(5.0f).kthLargest(1);
        assertTrue(result.isPresent());
        assertEquals(5.0f, result.get(), DELTA);
    }

    @Test
    public void testKthLargest_Empty() {
        OptionalFloat result = FloatStream.empty().kthLargest(1);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("sum() should return sum of elements")
    public void testSum() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertEquals(6.0, stream.sum(), 0.001);
    }

    @Test
    public void testSum_Test() {
        double result = createFloatStream(1.0f, 2.0f, 3.0f).sum();
        assertEquals(6.0, result, DELTA);
    }

    @Test
    public void testSum_Empty() {
        assertEquals(0.0, FloatStream.empty().sum(), DELTA);
    }

    @Test
    @DisplayName("average() should return average")
    public void testAverage() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        OptionalDouble avg = stream.average();
        assertTrue(avg.isPresent());
        assertEquals(2.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testAverage_Test() {
        OptionalDouble result = createFloatStream(1.0f, 2.0f, 3.0f).average();
        assertTrue(result.isPresent());
        assertEquals(2.0, result.getAsDouble(), DELTA);
    }

    @Test
    @DisplayName("average() of empty stream should return empty")
    public void testAverageEmpty() {
        FloatStream stream = FloatStream.empty();
        OptionalDouble average = stream.average();
        assertFalse(average.isPresent());
    }

    @Test
    public void testAverage_Empty_Test() {
        OptionalDouble result = FloatStream.empty().average();
        assertFalse(result.isPresent());
    }

    @Test
    public void testAverage_Empty() {
        OptionalDouble result = FloatStream.empty().average();
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("summaryStatistics() should return summary statistics")
    public void testsummaryStatistics() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        FloatSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(5, stats.getCount());
        assertEquals(1.0f, stats.getMin(), 0.001);
        assertEquals(5.0f, stats.getMax(), 0.001);
        assertEquals(15.0, stats.getSum(), 0.001);
        assertEquals(3.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testsummaryStatistics_Test() {
        FloatSummaryStatistics stats = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).summaryStatistics();
        assertEquals(5, stats.getCount());
        assertEquals(1.0f, stats.getMin(), DELTA);
        assertEquals(5.0f, stats.getMax(), DELTA);
    }

    @Test
    public void testSummaryStatistics_Empty() {
        FloatSummaryStatistics stats = FloatStream.empty().summaryStatistics();
        assertEquals(0, stats.getCount());
    }

    @Test
    @DisplayName("summaryStatisticsAndPercentiles() should return both")
    public void testsummaryStatisticsAndPercentiles() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        Pair<FloatSummaryStatistics, Optional<Map<Percentage, Float>>> result = stream.summaryStatisticsAndPercentiles();
        assertEquals(5, result.left().getCount());
        assertTrue(result.right().isPresent());
    }

    @Test
    public void testsummaryStatisticsAndPercentiles_Test() {
        Pair<FloatSummaryStatistics, Optional<Map<Percentage, Float>>> result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
                .summaryStatisticsAndPercentiles();
        assertEquals(5, result.left().getCount());
        assertTrue(result.right().isPresent());
    }

    @Test
    public void testSummaryStatisticsAndPercentiles_Empty() {
        Pair<FloatSummaryStatistics, Optional<Map<Percentage, Float>>> result = FloatStream.empty().summaryStatisticsAndPercentiles();
        assertEquals(0, result.getLeft().getCount());
        assertFalse(result.getRight().isPresent());
    }

    @Test
    @DisplayName("mergeWith() should merge streams")
    public void testMergeWith() {
        FloatStream stream1 = createFloatStream(1.0f, 3.0f, 5.0f);
        FloatStream stream2 = createFloatStream(2.0f, 4.0f, 6.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f },
                stream1.mergeWith(stream2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
    }

    @Test
    public void testMergeWith_Test() {
        float[] result = createFloatStream(1.0f, 3.0f, 5.0f)
                .mergeWith(FloatStream.of(2.0f, 4.0f, 6.0f), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result, DELTA);
    }

    @Test
    public void testMergeWith_Empty() {
        float[] result = createFloatStream(1.0f, 3.0f, 5.0f).mergeWith(FloatStream.empty(), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 3.0f, 5.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zipWith() two streams with binary operator")
    public void testZipWith() {
        FloatStream stream1 = createFloatStream(1.0f, 2.0f, 3.0f);
        FloatStream stream2 = createFloatStream(4.0f, 5.0f, 6.0f);
        assertArrayEquals(new float[] { 5.0f, 7.0f, 9.0f }, stream1.zipWith(stream2, Float::sum).toArray());
    }

    @Test
    @DisplayName("zipWith() three streams with ternary operator")
    public void testZipWithThreeStreams() {
        FloatStream stream1 = createFloatStream(1.0f, 2.0f);
        FloatStream stream2 = createFloatStream(3.0f, 4.0f);
        FloatStream stream3 = createFloatStream(5.0f, 6.0f);
        assertArrayEquals(new float[] { 9.0f, 12.0f }, stream1.zipWith(stream2, stream3, (a, b, c) -> a + b + c).toArray());
    }

    @Test
    @DisplayName("zipWith() with padding for two streams")
    public void testZipWithPadding() {
        FloatStream stream1 = createFloatStream(1.0f, 2.0f, 5.0f);
        FloatStream stream2 = createFloatStream(3.0f, 4.0f);
        assertArrayEquals(new float[] { 4.0f, 6.0f, 5.0f }, stream1.zipWith(stream2, 0.0f, 0.0f, Float::sum).toArray());
    }

    @Test
    @DisplayName("zipWith() with padding for three streams")
    public void testZipWithPaddingThreeStreams() {
        FloatStream stream1 = createFloatStream(1.0f);
        FloatStream stream2 = createFloatStream(3.0f, 4.0f);
        FloatStream stream3 = createFloatStream(5.0f, 6.0f, 7.0f);
        assertArrayEquals(new float[] { 9.0f, 10.0f, 7.0f }, stream1.zipWith(stream2, stream3, 0.0f, 0.0f, 0.0f, (a, b, c) -> a + b + c).toArray());
    }

    @Test
    public void testZipWithDefaults() {
        float[] a = { 1.1f, 2.2f };
        float[] b = { 10.1f, 20.2f, 30.3f };
        float[] expected = { 11.2f, 22.4f, 30.3f };
        assertArrayEquals(expected, FloatStream.zip(a, b, 0.0f, 0.0f, (x, y) -> x + y).toArray(), DELTA);
    }

    @Test
    public void testZipWith_Test() {
        float[] result = createFloatStream(1.0f, 2.0f).zipWith(FloatStream.of(10.0f, 20.0f), Float::sum).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f }, result, DELTA);
    }

    @Test
    public void testZipWith_ThreeStreams_Test() {
        float[] result = createFloatStream(1.0f, 2.0f).zipWith(FloatStream.of(10.0f, 20.0f), FloatStream.of(100.0f, 200.0f), (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new float[] { 111.0f, 222.0f }, result, DELTA);
    }

    @Test
    public void testZipWith_Padding_Test() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).zipWith(FloatStream.of(10.0f, 20.0f), 0.0f, 0.0f, Float::sum).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testZipWith_ThreeStreamsPadding_Test() {
        float[] result = createFloatStream(1.0f)
                .zipWith(FloatStream.of(10.0f, 20.0f), FloatStream.of(100.0f, 200.0f, 300.0f), 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z)
                .toArray();
        assertArrayEquals(new float[] { 111.0f, 220.0f, 300.0f }, result, DELTA);
    }

    @Test
    public void testZipWith_DifferentLengths() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).zipWith(FloatStream.of(10.0f, 20.0f), (a, b) -> a + b).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f }, result, DELTA);
    }

    @Test
    public void testZipWithPadding_DifferentLengths() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).zipWith(FloatStream.of(10.0f, 20.0f), 0.0f, 0.0f, (a, b) -> a + b).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f, 3.0f }, result, DELTA);
    }

    @Test
    @DisplayName("asDoubleStream() should convert to DoubleStream")
    public void testAsDoubleStream() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.asDoubleStream().toArray(), 0.001);
    }

    // TODO: filter(FloatPredicate) is abstract - tested via concrete implementations above
    // TODO: takeWhile(FloatPredicate) is abstract - tested via concrete implementations above
    // TODO: dropWhile(FloatPredicate) is abstract - tested via concrete implementations above
    // TODO: map(FloatUnaryOperator) is abstract - tested via concrete implementations above
    // TODO: mapToInt(FloatToIntFunction) is abstract - tested via concrete implementations above
    // TODO: mapToLong(FloatToLongFunction) is abstract - tested via concrete implementations above
    // TODO: mapToDouble(FloatToDoubleFunction) is abstract - tested via concrete implementations above
    // TODO: mapToObj(FloatFunction) is abstract - tested via concrete implementations above
    // TODO: flatMap(FloatFunction) is abstract - tested via concrete implementations above
    // TODO: flatmap(FloatFunction<float[]>) is abstract - tested via concrete implementations above
    // TODO: flatMapToInt(FloatFunction) is abstract - tested via concrete implementations above
    // TODO: flatMapToLong(FloatFunction) is abstract - tested via concrete implementations above
    // TODO: flatMapToDouble(FloatFunction) is abstract - tested via concrete implementations above
    // TODO: flatMapToObj(FloatFunction) is abstract - tested via concrete implementations above
    // TODO: flatmapToObj(FloatFunction) is abstract - tested via concrete implementations above
    // TODO: flatMapArrayToObj(FloatFunction) is abstract - tested via concrete implementations above
    // TODO: mapPartial(FloatFunction) is abstract - tested via concrete implementations above
    // TODO: rangeMap(FloatBiPredicate, FloatBinaryOperator) is abstract - tested via concrete implementations above
    // TODO: rangeMapToObj(FloatBiPredicate, FloatBiFunction) is abstract - tested via concrete implementations above
    // TODO: collapse(...) overloads are abstract - tested via concrete implementations above
    // TODO: scan(...) overloads are abstract - tested via concrete implementations above
    // TODO: prepend/append/appendIfEmpty are abstract - tested via concrete implementations above
    // TODO: top(...) overloads are abstract - tested via concrete implementations above
    // TODO: toFloatList() is abstract - tested via concrete implementations above
    // TODO: toMap/groupTo/reduce/collect overloads are abstract - tested via concrete implementations above
    // TODO: forEach/forEachIndexed are abstract - tested via concrete implementations above
    // TODO: anyMatch/allMatch/noneMatch are abstract - tested via concrete implementations above
    // TODO: findFirst/findAny/findLast with predicate are abstract - tested via concrete implementations above
    // TODO: min/max/kthLargest/sum/average/summaryStatistics are abstract - tested via concrete implementations above
    // TODO: mergeWith/zipWith overloads are abstract - tested via concrete implementations above
    // TODO: asDoubleStream/boxed are abstract - tested via concrete implementations above

    @Test
    public void testForeach() {
        List<Float> collected = new ArrayList<>();
        createFloatStream(1.0f, 2.0f, 3.0f).forEach(f -> collected.add(f));
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), collected);
    }

    @Test
    public void testAsDoubleStream_Test() {
        double[] result = createFloatStream(1.0f, 2.0f, 3.0f).asDoubleStream().toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, DELTA);
    }

    @Test
    public void testAsDoubleStream_Empty() {
        double[] result = FloatStream.empty().asDoubleStream().toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("boxed() should convert to Stream<Float>")
    public void testBoxed() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new Float[] { 1.0f, 2.0f, 3.0f }, stream.boxed().toArray(Float[]::new));
    }

    @Test
    public void testBoxed_Test() {
        List<Float> result = createFloatStream(1.0f, 2.0f, 3.0f).boxed().toList();
        assertEquals(3, result.size());
    }

    // ==================== New tests for untested methods ====================

    @Test
    public void testToCollection() {
        java.util.LinkedList<Float> list = createFloatStream(1.0f, 2.0f, 3.0f).boxed().toCollection(java.util.LinkedList::new);
        assertEquals(3, list.size());
        assertEquals(1.0f, list.get(0), DELTA);
        assertEquals(3.0f, list.get(2), DELTA);
    }

    @Test
    public void testBoxed_Empty() {
        List<Float> result = FloatStream.empty().boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("empty() should return empty stream")
    public void testEmpty() {
        FloatStream stream = FloatStream.empty();
        assertEquals(0, stream.count());
    }

    @Test
    @DisplayName("ifEmpty() should execute action for empty stream")
    public void testIfEmpty() {
        List<String> actions = new ArrayList<>();

        FloatStream stream1 = createFloatStream(1.0f);
        stream1.ifEmpty(() -> actions.add("empty1")).toArray();

        FloatStream stream2 = FloatStream.empty();
        stream2.ifEmpty(() -> actions.add("empty2")).toArray();

        assertEquals(Arrays.asList("empty2"), actions);
    }

    @Test
    @DisplayName("empty stream operations")
    public void testEmptyStream() {
        FloatStream empty = FloatStream.empty();
        assertEquals(0, empty.count());

        assertFalse(FloatStream.empty().first().isPresent());
        assertFalse(FloatStream.empty().min().isPresent());
        assertFalse(FloatStream.empty().max().isPresent());
        assertEquals(0.0, FloatStream.empty().sum(), 0.001);
        assertFalse(FloatStream.empty().average().isPresent());
    }

    @Test
    public void testDebounce_EmptyStream() {
        float[] result = FloatStream.empty().debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertEquals(0, result.length);
    }

    @Test
    public void testStreamCreatedAfterEmpty() {
        assertEquals(0, FloatStream.empty().count());
        assertEquals(0, FloatStream.empty().skip(1).count());
        assertArrayEquals(new float[] {}, FloatStream.empty().toArray(), 0.001f);
        assertArrayEquals(new float[] {}, FloatStream.empty().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(), FloatStream.empty().toList());
        assertEquals(N.toList(), FloatStream.empty().skip(1).toList());
        assertEquals(0, FloatStream.empty().map(e -> e).count());
        assertEquals(0, FloatStream.empty().map(e -> e).skip(1).count());
        assertArrayEquals(new float[] {}, FloatStream.empty().map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] {}, FloatStream.empty().map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(), FloatStream.empty().map(e -> e).toList());
        assertEquals(N.toList(), FloatStream.empty().map(e -> e).skip(1).toList());
    }

    @Test
    public void testEmpty_Test() {
        assertEquals(0, FloatStream.empty().count());
    }

    @Test
    public void testApplyIfNotEmpty_Empty() {
        Optional<float[]> result = FloatStream.empty().applyIfNotEmpty(s -> s.toArray());
        assertFalse(result.isPresent());
    }

    @Test
    public void testAcceptIfNotEmpty_Empty() {
        List<Float> collected = new ArrayList<>();
        FloatStream.empty().acceptIfNotEmpty(s -> s.forEach(collected::add));
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testDefaultIfEmpty() {
        float[] result = FloatStream.empty().defaultIfEmpty(() -> FloatStream.of(99.0f)).toArray();
        assertArrayEquals(new float[] { 99.0f }, result, DELTA);
    }

    @Test
    public void testFirst_Empty() {
        OptionalFloat result = FloatStream.empty().first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast_Empty() {
        OptionalFloat result = FloatStream.empty().last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_Empty() {
        OptionalFloat result = FloatStream.empty().onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testCycled_Empty() {
        float[] result = FloatStream.empty().cycled().limit(0).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testReversed_Empty() {
        float[] result = FloatStream.empty().reversed().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testShuffled_Empty() {
        float[] result = FloatStream.empty().shuffled().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testDistinct_Empty() {
        float[] result = FloatStream.empty().distinct().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testSorted_Empty() {
        float[] result = FloatStream.empty().sorted().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testReverseSorted_Empty() {
        float[] result = FloatStream.empty().reverseSorted().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testCount_Empty() {
        assertEquals(0, FloatStream.empty().count());
    }

    @Test
    public void testToList_Empty() {
        List<Float> result = FloatStream.empty().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToSet_Empty() {
        Set<Float> result = FloatStream.empty().toSet();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIndexed_Empty() {
        List<IndexedFloat> result = FloatStream.empty().indexed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIfEmpty_Empty() {
        AtomicInteger counter = new AtomicInteger(0);
        FloatStream.empty().ifEmpty(counter::incrementAndGet).toArray();
        assertEquals(1, counter.get());
    }

    @Test
    public void testJoin_Empty() {
        String result = FloatStream.empty().join(", ");
        assertEquals("", result);
    }

    @Test
    public void testPercentiles_Empty() {
        Optional<Map<Percentage, Float>> result = FloatStream.empty().percentiles();
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("throwIfEmpty() should throw for empty stream")
    public void testThrowIfEmpty() {
        FloatStream stream1 = createFloatStream(1.0f);
        assertDoesNotThrow(() -> stream1.throwIfEmpty().toArray());

        FloatStream stream2 = FloatStream.empty();
        assertThrows(RuntimeException.class, () -> stream2.throwIfEmpty().toArray());
    }

    @Test
    public void testPrintln_Empty() {
        assertDoesNotThrow(() -> FloatStream.empty().println());
    }

    @Test
    public void testThrowIfEmpty_WithSupplier() {
        assertThrows(IllegalArgumentException.class, () -> FloatStream.empty().throwIfEmpty(() -> new IllegalArgumentException("empty")).toArray());
    }

    @Test
    public void testIterator_Empty() {
        FloatIterator iter = FloatStream.empty().iterator();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
    }

    @Test
    @DisplayName("defer() should create deferred stream")
    public void testDefer() {
        FloatStream stream = FloatStream.defer(() -> FloatStream.of(1.0f, 2.0f, 3.0f));
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    public void testStreamCreatedAfterDefer() {
        float[][] array = { { 1, 2, 3 }, { 4, 5 } };

        assertEquals(5, FloatStream.defer(() -> FloatStream.of(1, 2, 3, 4, 5)).count());
        assertEquals(4, FloatStream.defer(() -> FloatStream.of(1, 2, 3, 4, 5)).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.defer(() -> FloatStream.of(1, 2, 3, 4, 5)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.defer(() -> FloatStream.of(1, 2, 3, 4, 5)).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.defer(() -> FloatStream.of(1, 2, 3, 4, 5)).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.flatten(array).skip(1).toList());
        assertEquals(5, FloatStream.flatten(array).map(e -> e).count());
        assertEquals(4, FloatStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testDefer_Test() {
        FloatStream result = FloatStream.defer(() -> FloatStream.of(1.0f, 2.0f, 3.0f));
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result.toArray(), DELTA);
    }

    @Test
    public void testDefer_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> FloatStream.defer(null));
    }

    @Test
    public void testDefer_NullThrows() {
        assertThrows(IllegalArgumentException.class, () -> FloatStream.defer(null));
    }

    @Test
    @DisplayName("ofNullable() with null should return empty stream")
    public void testOfNullableNull() {
        FloatStream stream = FloatStream.ofNullable(null);
        assertEquals(0, stream.count());
    }

    @Test
    @DisplayName("ofNullable() with value should return single element stream")
    public void testOfNullableValue() {
        FloatStream stream = FloatStream.ofNullable(5.0f);
        assertArrayEquals(new float[] { 5.0f }, stream.toArray());
    }

    @Test
    public void testOfNullable() {
        assertEquals(0, FloatStream.ofNullable(null).count());
        assertArrayEquals(new float[] { 9.9f }, FloatStream.ofNullable(9.9f).toArray(), DELTA);
    }

    @Test
    public void testStreamCreatedAfterOfNullable() {
        assertEquals(1, FloatStream.ofNullable(5f).count());
        assertEquals(0, FloatStream.ofNullable(5f).skip(1).count());
        assertArrayEquals(new float[] { 5 }, FloatStream.ofNullable(5f).toArray(), 0.001f);
        assertArrayEquals(new float[] {}, FloatStream.ofNullable(5f).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f), FloatStream.ofNullable(5f).toList());
        assertEquals(N.toList(), FloatStream.ofNullable(5f).skip(1).toList());
        assertEquals(1, FloatStream.ofNullable(5f).map(e -> e).count());
        assertEquals(0, FloatStream.ofNullable(5f).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 5 }, FloatStream.ofNullable(5f).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] {}, FloatStream.ofNullable(5f).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f), FloatStream.ofNullable(5f).map(e -> e).toList());
        assertEquals(N.toList(), FloatStream.ofNullable(5f).map(e -> e).skip(1).toList());

        assertEquals(0, FloatStream.ofNullable(null).count());
        assertEquals(0, FloatStream.ofNullable(null).skip(1).count());
        assertArrayEquals(new float[] {}, FloatStream.ofNullable(null).toArray(), 0.001f);
        assertArrayEquals(new float[] {}, FloatStream.ofNullable(null).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(), FloatStream.ofNullable(null).toList());
        assertEquals(N.toList(), FloatStream.ofNullable(null).skip(1).toList());
    }

    @Test
    @DisplayName("ofNullable() with value should return single element stream")
    public void testOfNullableWithValue() {
        FloatStream stream = FloatStream.ofNullable(5.0f);
        float[] result = stream.toArray();
        assertArrayEquals(new float[] { 5.0f }, result);
    }

    @Test
    public void testOfNullable_Null() {
        FloatStream result = FloatStream.ofNullable(null);
        assertEquals(0, result.count());
    }

    @Test
    public void testOfNullable_WithValue() {
        FloatStream result = FloatStream.ofNullable(42.5f);
        assertArrayEquals(new float[] { 42.5f }, result.toArray(), DELTA);
    }

    @Test
    @DisplayName("of() with varargs should create stream")
    public void testOfVarargs() {
        FloatStream stream = FloatStream.of(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    @DisplayName("of() with primitive array should create stream")
    public void testOfArray() {
        float[] array = { 1.5f, 2.5f, 3.5f };
        FloatStream stream = FloatStream.of(array);
        assertArrayEquals(array, stream.toArray());
    }

    @Test
    @DisplayName("of() with array range should create substream")
    public void testOfArrayRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        FloatStream stream = FloatStream.of(array, 1, 4);
        assertArrayEquals(new float[] { 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    @DisplayName("of() with Float array should create stream")
    public void testOfFloatArray() {
        Float[] array = { 1.0f, 2.0f, 3.0f };
        FloatStream stream = FloatStream.of(array);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    @DisplayName("of() with Float array range should create substream")
    public void testOfFloatArrayRange() {
        Float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        FloatStream stream = FloatStream.of(array, 1, 4);
        assertArrayEquals(new float[] { 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    @DisplayName("of() with collection should create stream")
    public void testOfCollection() {
        List<Float> list = Arrays.asList(1.0f, 2.0f, 3.0f);
        FloatStream stream = FloatStream.of(list);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    @DisplayName("of() with iterator should create stream")
    public void testOfIterator() {
        FloatIterator iterator = FloatIterator.of(1.0f, 2.0f, 3.0f);
        FloatStream stream = FloatStream.of(iterator);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    @DisplayName("of() with FloatBuffer should create stream")
    public void testOfFloatBuffer() {
        FloatBuffer buffer = FloatBuffer.allocate(3);
        buffer.put(1.0f).put(2.0f).put(3.0f).flip();
        FloatStream stream = FloatStream.of(buffer);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    // Parallel operations

    @Test
    @DisplayName("parallel stream operations additional")
    public void testParallelOperationsAdditional() {
        FloatStream stream = FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f).parallel();
        float[] result = stream.filter(f -> f > 3.0f).map(f -> f * 2).toArray();
        assertEquals(5, result.length);
    }

    // ==================== debounce tests ====================

    @Test
    public void testDebounce_BasicFunctionality() {
        // Allow 3 elements per 1 second window
        float[] result = FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        // Only first 3 elements should pass through within the window
        assertEquals(3, result.length);
        assertEquals(1.0f, result[0], 0.001f);
        assertEquals(2.0f, result[1], 0.001f);
        assertEquals(3.0f, result[2], 0.001f);
    }

    @Test
    public void testDebounce_AllElementsPassWhenWithinLimit() {
        // Allow 10 elements per window, but only 5 elements in stream
        float[] result = FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).debounce(10, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        // All elements should pass
        assertEquals(5, result.length);
    }

    @Test
    public void testDebounce_PreservesOrder() {
        float[] result = FloatStream.of(10.0f, 20.0f, 30.0f, 40.0f, 50.0f).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertEquals(10.0f, result[0], 0.001f);
        assertEquals(20.0f, result[1], 0.001f);
        assertEquals(30.0f, result[2], 0.001f);
    }

    @Test
    public void testDebounce_ChainedWithOtherOperations() {
        float[] result = FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f)
                .filter(n -> n % 2 == 0) // 2, 4, 6, 8, 10
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)) // 2, 4, 6
                .map(n -> n * 10) // 20, 40, 60
                .toArray();

        assertEquals(3, result.length);
        assertEquals(20.0f, result[0], 0.001f);
        assertEquals(40.0f, result[1], 0.001f);
        assertEquals(60.0f, result[2], 0.001f);
    }

    @Test
    public void testSkipAndLimit() {
        assertArrayEquals(new float[] { 3.0f, 4.0f }, FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).skip(2).limit(2).toArray(), DELTA);
    }

    @Test
    public void testParallelMapSum() {
        for (int k = 0; k < 1000; k++) {
            float[] data = new float[1000];
            for (int i = 0; i < data.length; i++) {
                data[i] = i + 1;
            }

            double expectedSum = FloatStream.of(data).map(f -> f * 2.0f).sum();
            double actualSum = FloatStream.of(data).parallel().map(f -> f * 2.0f).sum();

            assertEquals(expectedSum, actualSum, DELTA);
        }
    }

    @Test
    public void testIsParallelAndSequential() {
        assertFalse(FloatStream.of(1.0f).isParallel());
        assertTrue(FloatStream.of(1.0f).parallel().isParallel());
        assertFalse(FloatStream.of(1.0f).parallel().sequential().isParallel());
    }

    @Test
    public void testTransform() {
        FloatStream transformed = FloatStream.of(1f, 2f, 3f, 4f, 5f).transform(s -> s.step(2).map(f -> f * 2));
        assertArrayEquals(new float[] { 2f, 6f, 10f }, transformed.map(f -> f).toArray(), DELTA);
    }

    @Test
    public void testJoinTo() {
        Joiner joiner = Joiner.with(" | ");
        FloatStream.of(1.1f, 2.2f, 3.3f).joinTo(joiner);
        assertEquals("1.1 | 2.2 | 3.3", joiner.toString());
    }

    @Test
    public void testFlattMapToObjWithArray() {
        String[] result = FloatStream.of(1f, 2f).flatMapArrayToObj(f -> new String[] { "val=" + f }).toArray(String[]::new);
        assertArrayEquals(new String[] { "val=1.0", "val=2.0" }, result);
    }

    @Test
    public void testDelay() {
        long startTime = System.currentTimeMillis();
        FloatStream.of(1f, 2f, 3f).delay(com.landawn.abacus.util.Duration.ofMillis(50)).count();
        long endTime = System.currentTimeMillis();

        assertTrue((endTime - startTime) >= 100, "Stream processing should be delayed");
    }

    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilterWithAction() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).count());
        assertEquals(1, FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new float[] { 1, 2 }, FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2 }, FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f), FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList(2f), FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toList());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).count());
        assertEquals(1, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new float[] { 1, 2 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList(2f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhileWithAction() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).count());
        assertArrayEquals(new float[] { 2, 4, 6, 8, 10 }, FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 6, 8, 10 }, FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(2f, 4f, 6f, 8f, 10f), FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).toList());
        assertEquals(N.toList(4f, 6f, 8f, 10f), FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).count());
        assertArrayEquals(new float[] { 2, 4, 6, 8, 10 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 6, 8, 10 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(2f, 4f, 6f, 8f, 10f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).toList());
        assertEquals(N.toList(4f, 6f, 8f, 10f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrepend() {
        assertEquals(8, FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).count());
        assertEquals(7, FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).skip(1).count());
        assertArrayEquals(new float[] { 0, -1, -2, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).toArray(), 0.001f);
        assertArrayEquals(new float[] { -1, -2, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(0f, -1f, -2f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).toList());
        assertEquals(N.toList(-1f, -2f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).skip(1).toList());
        assertEquals(8, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).count());
        assertEquals(7, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).skip(1).count());
        assertArrayEquals(new float[] { 0, -1, -2, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).toArray(), 0.001f);
        assertArrayEquals(new float[] { -1, -2, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(0f, -1f, -2f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).toList());
        assertEquals(N.toList(-1f, -2f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppend() {
        assertEquals(8, FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).count());
        assertEquals(7, FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8 }, FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8 }, FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f), FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f, 7f, 8f), FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).toList());
        assertEquals(8, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).count());
        assertEquals(7, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f, 7f, 8f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTop() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).top(3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).top(3).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).top(3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).top(3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).top(3).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).top(3).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScan() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 1, 3, 6, 10, 15 }, FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 6, 10, 15 }, FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 3f, 6f, 10f, 15f), FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toList());
        assertEquals(N.toList(3f, 6f, 10f, 15f), FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 1, 3, 6, 10, 15 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 6, 10, 15 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 3f, 6f, 10f, 15f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toList());
        assertEquals(N.toList(3f, 6f, 10f, 15f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInit() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toList());
        assertEquals(N.toList(13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toList());
        assertEquals(N.toList(13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInitIncluded() {
        assertEquals(6, FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 10, 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(10f, 11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(N.toList(11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toList());
        assertEquals(6, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 10, 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toArray(),
                0.001f);
        assertEquals(N.toList(10f, 11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(N.toList(11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(5, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().count());
        assertEquals(4, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().count());
        assertEquals(4, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIntersection() {
        List<Float> otherList = Arrays.asList(3f, 4f, 5f, 6f, 7f);
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDifference() {
        List<Float> otherList = Arrays.asList(3f, 4f, 5f, 6f, 7f);
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).difference(otherList).count());
        assertEquals(1, FloatStream.of(1, 2, 3, 4, 5).difference(otherList).skip(1).count());
        assertArrayEquals(new float[] { 1, 2 }, FloatStream.of(1, 2, 3, 4, 5).difference(otherList).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2 }, FloatStream.of(1, 2, 3, 4, 5).difference(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f), FloatStream.of(1, 2, 3, 4, 5).difference(otherList).toList());
        assertEquals(N.toList(2f), FloatStream.of(1, 2, 3, 4, 5).difference(otherList).skip(1).toList());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).count());
        assertEquals(1, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).skip(1).count());
        assertArrayEquals(new float[] { 1, 2 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).toList());
        assertEquals(N.toList(2f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSymmetricDifference() {
        List<Float> otherList = Arrays.asList(3f, 4f, 5f, 6f, 7f);
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).symmetricDifference(otherList).count());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).symmetricDifference(otherList).skip(1).count());
        assertArrayEquals(new float[] { 2, 6, 7 }, FloatStream.of(1, 2, 3, 4, 5).symmetricDifference(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 6f, 7f), FloatStream.of(1, 2, 3, 4, 5).symmetricDifference(otherList).toList());
        assertEquals(N.toList(2f, 6f, 7f), FloatStream.of(1, 2, 3, 4, 5).symmetricDifference(otherList).skip(1).toList());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).count());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 6, 7 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 6, 7 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 6f, 7f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).toList());
        assertEquals(N.toList(2f, 6f, 7f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReversed() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).reversed().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).reversed().skip(1).count());
        assertArrayEquals(new float[] { 5, 4, 3, 2, 1 }, FloatStream.of(1, 2, 3, 4, 5).reversed().toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 3, 2, 1 }, FloatStream.of(1, 2, 3, 4, 5).reversed().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f, 4f, 3f, 2f, 1f), FloatStream.of(1, 2, 3, 4, 5).reversed().toList());
        assertEquals(N.toList(4f, 3f, 2f, 1f), FloatStream.of(1, 2, 3, 4, 5).reversed().skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).count());
        assertArrayEquals(new float[] { 5, 4, 3, 2, 1 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 3, 2, 1 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f, 4f, 3f, 2f, 1f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toList());
        assertEquals(N.toList(4f, 3f, 2f, 1f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRotated() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).rotated(2).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).count());
        assertArrayEquals(new float[] { 4, 5, 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).rotated(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 5, 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(4f, 5f, 1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).rotated(2).toList());
        assertEquals(N.toList(5f, 1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).count());
        assertArrayEquals(new float[] { 4, 5, 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 5, 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(4f, 5f, 1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toList());
        assertEquals(N.toList(5f, 1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterShuffled() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).shuffled().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).shuffled().skip(1).count());
        float[] shuffled = FloatStream.of(1, 2, 3, 4, 5).shuffled().toArray();
        assertEquals(5, shuffled.length);
        Arrays.sort(shuffled);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, shuffled, 0.001f);

        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterShuffledWithRandom() {
        Random rnd = new Random(42);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).shuffled(rnd).skip(1).count());
        rnd = new Random(42);
        float[] shuffled = FloatStream.of(1, 2, 3, 4, 5).shuffled(rnd).toArray();
        assertEquals(5, shuffled.length);

        rnd = new Random(42);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, FloatStream.of(5, 3, 1, 4, 2).sorted().count());
        assertEquals(4, FloatStream.of(5, 3, 1, 4, 2).sorted().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).sorted().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).sorted().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).sorted().toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).sorted().skip(1).toList());
        assertEquals(5, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().count());
        assertEquals(4, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, FloatStream.of(5, 3, 1, 4, 2).reverseSorted().count());
        assertEquals(4, FloatStream.of(5, 3, 1, 4, 2).reverseSorted().skip(1).count());
        assertArrayEquals(new float[] { 5, 4, 3, 2, 1 }, FloatStream.of(5, 3, 1, 4, 2).reverseSorted().toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 3, 2, 1 }, FloatStream.of(5, 3, 1, 4, 2).reverseSorted().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f, 4f, 3f, 2f, 1f), FloatStream.of(5, 3, 1, 4, 2).reverseSorted().toList());
        assertEquals(N.toList(4f, 3f, 2f, 1f), FloatStream.of(5, 3, 1, 4, 2).reverseSorted().skip(1).toList());
        assertEquals(5, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().count());
        assertEquals(4, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new float[] { 5, 4, 3, 2, 1 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 3, 2, 1 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f, 4f, 3f, 2f, 1f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().toList());
        assertEquals(N.toList(4f, 3f, 2f, 1f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCycled() {
        assertEquals(15, FloatStream.of(1, 2, 3, 4, 5).cycled().limit(15).count());
        assertEquals(14, FloatStream.of(1, 2, 3, 4, 5).cycled().limit(15).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).cycled().limit(15).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).cycled().limit(15).skip(1).toArray(), 0.001f);
        assertEquals(15, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(15).count());
        assertEquals(14, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(15).skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterCycledWithRounds() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).cycled(2).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).cycled(2).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).cycled(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).cycled(2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).cycled(2).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).cycled(2).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).skip(2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).skip(2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).skip(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skip(2).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipWithAction() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).skip(2, e -> {
        }).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).skip(2, e -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).skip(2, e -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).skip(2, e -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skip(2, e -> {
        }).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skip(2, e -> {
        }).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterLimit() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).limit(3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).limit(3).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).limit(3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).limit(3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).limit(3).toList());
        assertEquals(N.toList(2f, 3f), FloatStream.of(1, 2, 3, 4, 5).limit(3).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toList());
        assertEquals(N.toList(2f, 3f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterStep() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5, 6).step(2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).count());
        assertArrayEquals(new float[] { 1, 3, 5 }, FloatStream.of(1, 2, 3, 4, 5, 6).step(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 5 }, FloatStream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 3f, 5f), FloatStream.of(1, 2, 3, 4, 5, 6).step(2).toList());
        assertEquals(N.toList(3f, 5f), FloatStream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).count());
        assertArrayEquals(new float[] { 1, 3, 5 }, FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 5 }, FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 3f, 5f), FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).toList());
        assertEquals(N.toList(3f, 5f), FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnEach() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnClose() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToInt() {
        assertEquals(5, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).count());
        assertEquals(4, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).toList());
        assertEquals(N.toList(2, 3, 4, 5), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).skip(1).toList());
        assertEquals(5, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).count());
        assertEquals(4, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).toList());
        assertEquals(N.toList(2, 3, 4, 5), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToLong() {
        assertEquals(5, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).count());
        assertEquals(4, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).skip(1).count());
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).toArray());
        assertArrayEquals(new long[] { 2L, 3L, 4L, 5L }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).skip(1).toArray());
        assertEquals(N.toList(1L, 2L, 3L, 4L, 5L), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).toList());
        assertEquals(N.toList(2L, 3L, 4L, 5L), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).skip(1).toList());
        assertEquals(5, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).count());
        assertEquals(4, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).skip(1).count());
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).toArray());
        assertArrayEquals(new long[] { 2L, 3L, 4L, 5L }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).skip(1).toArray());
        assertEquals(N.toList(1L, 2L, 3L, 4L, 5L), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).toList());
        assertEquals(N.toList(2L, 3L, 4L, 5L), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToDouble() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).skip(1).count());
        assertArrayEquals(new double[] { 1.5, 3.0, 4.5, 6.0, 7.5 }, FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).toArray(), 0.001);
        assertArrayEquals(new double[] { 3.0, 4.5, 6.0, 7.5 }, FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).skip(1).toArray(), 0.001);
        assertEquals(N.toList(1.5, 3.0, 4.5, 6.0, 7.5), FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).toList());
        assertEquals(N.toList(3.0, 4.5, 6.0, 7.5), FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).skip(1).count());
        assertArrayEquals(new double[] { 1.5, 3.0, 4.5, 6.0, 7.5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).toArray(), 0.001);
        assertArrayEquals(new double[] { 3.0, 4.5, 6.0, 7.5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).skip(1).toArray(), 0.001);
        assertEquals(N.toList(1.5, 3.0, 4.5, 6.0, 7.5), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).toList());
        assertEquals(N.toList(3.0, 4.5, 6.0, 7.5), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToObj() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F2.0", "F3.0", "F4.0", "F5.0" }, FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).toArray());
        assertArrayEquals(new String[] { "F2.0", "F3.0", "F4.0", "F5.0" }, FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).skip(1).toArray());
        assertEquals(N.toList("F1.0", "F2.0", "F3.0", "F4.0", "F5.0"), FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).toList());
        assertEquals(N.toList("F2.0", "F3.0", "F4.0", "F5.0"), FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F2.0", "F3.0", "F4.0", "F5.0" }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).toArray());
        assertArrayEquals(new String[] { "F2.0", "F3.0", "F4.0", "F5.0" }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).skip(1).toArray());
        assertEquals(N.toList("F1.0", "F2.0", "F3.0", "F4.0", "F5.0"), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).toList());
        assertEquals(N.toList("F2.0", "F3.0", "F4.0", "F5.0"), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).count());
        assertArrayEquals(new float[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 }, FloatStream.of(1, 2, 3, 4, 5).flatMap(f -> FloatStream.of(f, f * 10)).toArray(),
                0.001f);
        assertArrayEquals(new float[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f), FloatStream.of(1, 2, 3, 4, 5).flatMap(f -> FloatStream.of(f, f * 10)).toList());
        assertEquals(N.toList(10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f), FloatStream.of(1, 2, 3, 4, 5).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).count());
        assertArrayEquals(new float[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).toList());
        assertEquals(N.toList(10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToLong() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).count());
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).toArray());
        assertArrayEquals(new long[] { 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).toArray());
        assertEquals(N.toList(1L, 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).toList());
        assertEquals(N.toList(10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).count());
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).toArray());
        assertArrayEquals(new long[] { 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).toArray());
        assertEquals(N.toList(1L, 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).toList());
        assertEquals(N.toList(10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToDouble() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).count());
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0 },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).toArray(), 0.001);
        assertArrayEquals(new double[] { 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0 },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).toArray(), 0.001);
        assertEquals(N.toList(1.0, 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).toList());
        assertEquals(N.toList(10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).count());
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).toArray(), 0.001);
        assertArrayEquals(new double[] { 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).toArray(), 0.001);
        assertEquals(N.toList(1.0, 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).toList());
        assertEquals(N.toList(10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToObj() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).toArray());
        assertEquals(N.toList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).toList());
        assertEquals(N.toList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).toArray());
        assertEquals(N.toList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).toList());
        assertEquals(N.toList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapToObj() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).skip(1).toArray());
        assertEquals(N.toList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).toList());
        assertEquals(N.toList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).skip(1).toArray());
        assertEquals(N.toList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).toList());
        assertEquals(N.toList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattmapToObj() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).toArray());
        assertEquals(N.toList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).toList());
        assertEquals(N.toList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).toArray());
        assertEquals(N.toList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).toList());
        assertEquals(N.toList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAsDoubleStream() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().skip(1).count());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().toArray(), 0.001);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0, 5.0 }, FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().skip(1).toArray(), 0.001);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().skip(1).count());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().toArray(), 0.001);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0, 5.0 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().skip(1).toArray(), 0.001);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterBoxed() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).boxed().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).boxed().skip(1).count());
        assertArrayEquals(new Float[] { 1f, 2f, 3f, 4f, 5f }, FloatStream.of(1, 2, 3, 4, 5).boxed().toArray());
        assertArrayEquals(new Float[] { 2f, 3f, 4f, 5f }, FloatStream.of(1, 2, 3, 4, 5).boxed().skip(1).toArray());
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).boxed().toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).boxed().skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().skip(1).count());
        assertArrayEquals(new Float[] { 1f, 2f, 3f, 4f, 5f }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().toArray());
        assertArrayEquals(new Float[] { 2f, 3f, 4f, 5f }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().skip(1).toArray());
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmap() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatmap(f -> new float[] { f, f * 10 }).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatmap(f -> new float[] { f, f * 10 }).skip(1).count());
        assertArrayEquals(new float[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 }, FloatStream.of(1, 2, 3, 4, 5).flatmap(f -> new float[] { f, f * 10 }).toArray(),
                0.001f);
        assertArrayEquals(new float[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).flatmap(f -> new float[] { f, f * 10 }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f), FloatStream.of(1, 2, 3, 4, 5).flatmap(f -> new float[] { f, f * 10 }).toList());
        assertEquals(N.toList(10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f), FloatStream.of(1, 2, 3, 4, 5).flatmap(f -> new float[] { f, f * 10 }).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmap(f -> new float[] { f, f * 10 }).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmap(f -> new float[] { f, f * 10 }).skip(1).count());
        assertArrayEquals(new float[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmap(f -> new float[] { f, f * 10 }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmap(f -> new float[] { f, f * 10 }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmap(f -> new float[] { f, f * 10 }).toList());
        assertEquals(N.toList(10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmap(f -> new float[] { f, f * 10 }).skip(1).toList());

    }

    @Test
    public void testStreamCreatedAfterFlatMapToInt() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).toArray());
        assertArrayEquals(new int[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).toArray());
        assertEquals(N.toList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).toList());
        assertEquals(N.toList(10, 2, 20, 3, 30, 4, 40, 5, 50),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).toArray());
        assertArrayEquals(new int[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).toArray());
        assertEquals(N.toList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).toList());
        assertEquals(N.toList(10, 2, 20, 3, 30, 4, 40, 5, 50),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterConcatStreams() {
        FloatStream s1 = FloatStream.of(1, 2);
        FloatStream s2 = FloatStream.of(3, 4);
        FloatStream s3 = FloatStream.of(5);
        assertEquals(5, FloatStream.concat(s1, s2, s3).count());
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertEquals(4, FloatStream.concat(s1, s2, s3).skip(1).count());
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.concat(s1, s2, s3).toArray(), 0.001f);
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.concat(s1, s2, s3).skip(1).toArray(), 0.001f);
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.concat(s1, s2, s3).toList());
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.concat(s1, s2, s3).skip(1).toList());
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertEquals(5, FloatStream.concat(s1, s2, s3).map(e -> e).count());
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertEquals(4, FloatStream.concat(s1, s2, s3).map(e -> e).skip(1).count());
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.concat(s1, s2, s3).map(e -> e).toArray(), 0.001f);
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.concat(s1, s2, s3).map(e -> e).skip(1).toArray(), 0.001f);
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.concat(s1, s2, s3).map(e -> e).toList());
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.concat(s1, s2, s3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfWithStartEndIndex() {
        assertEquals(3, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).count());
        assertEquals(2, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).skip(1).count());
        assertArrayEquals(new float[] { 2, 3, 4 }, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 4 }, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(2f, 3f, 4f), FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).toList());
        assertEquals(N.toList(3f, 4f), FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).skip(1).toList());
        assertEquals(3, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).count());
        assertEquals(2, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 2, 3, 4 }, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 4 }, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(2f, 3f, 4f), FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).toList());
        assertEquals(N.toList(3f, 4f), FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfFloatArray() {
        assertEquals(5, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).count());
        assertEquals(4, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).skip(1).toList());
        assertEquals(5, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).count());
        assertEquals(4, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfFloatArrayWithStartEndIndex() {
        assertEquals(3, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).count());
        assertEquals(2, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).skip(1).count());
        assertArrayEquals(new float[] { 2, 3, 4 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 4 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(2f, 3f, 4f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).toList());
        assertEquals(N.toList(3f, 4f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).skip(1).toList());
        assertEquals(3, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).count());
        assertEquals(2, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 2, 3, 4 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 4 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(2f, 3f, 4f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).toList());
        assertEquals(N.toList(3f, 4f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfCollection() {
        List<Float> list = Arrays.asList(1f, 2f, 3f, 4f, 5f);
        assertEquals(5, FloatStream.of(list).count());
        assertEquals(4, FloatStream.of(list).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(list).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(list).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(list).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(list).skip(1).toList());
        assertEquals(5, FloatStream.of(list).map(e -> e).count());
        assertEquals(4, FloatStream.of(list).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(list).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(list).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(list).map(e -> e).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(list).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfFloatIterator() {
        FloatIterator iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(5, FloatStream.of(iter).count());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(4, FloatStream.of(iter).skip(1).count());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(iter).toArray(), 0.001f);
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(iter).skip(1).toArray(), 0.001f);
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(iter).toList());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(iter).skip(1).toList());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(5, FloatStream.of(iter).map(e -> e).count());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(4, FloatStream.of(iter).map(e -> e).skip(1).count());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(iter).map(e -> e).toArray(), 0.001f);
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(iter).map(e -> e).skip(1).toArray(), 0.001f);
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(iter).map(e -> e).toList());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(iter).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfFloatBuffer() {
        FloatBuffer buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(5, FloatStream.of(buffer).count());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(4, FloatStream.of(buffer).skip(1).count());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(buffer).toArray(), 0.001f);
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(buffer).skip(1).toArray(), 0.001f);
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(buffer).toList());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(buffer).skip(1).toList());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(5, FloatStream.of(buffer).map(e -> e).count());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(4, FloatStream.of(buffer).map(e -> e).skip(1).count());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(buffer).map(e -> e).toArray(), 0.001f);
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(buffer).map(e -> e).skip(1).toArray(), 0.001f);
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(buffer).map(e -> e).toList());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(buffer).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRangeMap() {
        assertEquals(3, FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).count());
        assertEquals(2, FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).count());
        assertArrayEquals(new float[] { 2, 6, 10 },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).toArray(), 0.001f);
        assertArrayEquals(new float[] { 6, 10 },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(2f, 6f, 10f),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).toList());
        assertEquals(N.toList(6f, 10f),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).count());
        assertEquals(2,
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).count());
        assertArrayEquals(new float[] { 2, 6, 10 },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).toArray(), 0.001f);
        assertArrayEquals(new float[] { 6, 10 },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).toArray(),
                0.001f);
        assertEquals(N.toList(2f, 6f, 10f),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).toList());
        assertEquals(N.toList(6f, 10f),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRangeMapToObj() {
        assertEquals(3, FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last).count());
        assertEquals(2,
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last).skip(1).count());
        assertArrayEquals(new String[] { "1.0-2.0", "5.0-6.0", "10.0-10.0" },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last).toArray());
        assertArrayEquals(new String[] { "5.0-6.0", "10.0-10.0" },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .toArray());
        assertEquals(N.toList("1.0-2.0", "5.0-6.0", "10.0-10.0"),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last).toList());
        assertEquals(N.toList("5.0-6.0", "10.0-10.0"),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .toList());
        assertEquals(3,
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .map(e -> e)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .count());
        assertEquals(2,
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .map(e -> e)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "1.0-2.0", "5.0-6.0", "10.0-10.0" },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .map(e -> e)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .toArray());
        assertArrayEquals(new String[] { "5.0-6.0", "10.0-10.0" },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .map(e -> e)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .toArray());
        assertEquals(N.toList("1.0-2.0", "5.0-6.0", "10.0-10.0"),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .map(e -> e)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .toList());
        assertEquals(N.toList("5.0-6.0", "10.0-10.0"),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .map(e -> e)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterCollapseToList() {
        assertEquals(3, FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1).count());
        assertEquals(2, FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1).skip(1).count());
        Object[] collapsedArray = FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1).toArray();
        assertEquals(3, collapsedArray.length);
        assertEquals(FloatList.of(1f, 2f), collapsedArray[0]);
        assertEquals(FloatList.of(5f, 6f, 7f), collapsedArray[1]);
        assertEquals(FloatList.of(10f), collapsedArray[2]);
        Object[] collapsedArraySkip = FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1).skip(1).toArray();
        assertEquals(2, collapsedArraySkip.length);
        assertEquals(FloatList.of(5f, 6f, 7f), collapsedArraySkip[0]);
        assertEquals(FloatList.of(10f), collapsedArraySkip[1]);
        List<FloatList> collapsedList = FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1).toList();
        assertEquals(3, collapsedList.size());
        assertEquals(FloatList.of(1f, 2f), collapsedList.get(0));
        assertEquals(FloatList.of(5f, 6f, 7f), collapsedList.get(1));
        assertEquals(FloatList.of(10f), collapsedList.get(2));
        List<FloatList> collapsedListSkip = FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1).skip(1).toList();
        assertEquals(2, collapsedListSkip.size());
        assertEquals(FloatList.of(5f, 6f, 7f), collapsedListSkip.get(0));
        assertEquals(FloatList.of(10f), collapsedListSkip.get(1));
        assertEquals(3, FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1).count());
        assertEquals(2, FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1).skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithMerge() {
        assertEquals(3, FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).count());
        assertEquals(2, FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 3, 18, 10 }, FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).toArray(),
                0.001f);
        assertArrayEquals(new float[] { 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 18f, 10f), FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(18f, 10f), FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).count());
        assertEquals(2, FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 3, 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 18f, 10f),
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(18f, 10f),
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithTriPredicate() {
        assertEquals(3, FloatStream.of(1, 2, 5, 6, 7, 10).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, FloatStream.of(1, 2, 5, 6, 7, 10).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 3, 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 18f, 10f), FloatStream.of(1, 2, 5, 6, 7, 10).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(18f, 10f), FloatStream.of(1, 2, 5, 6, 7, 10).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 3, 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 18f, 10f),
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(18f, 10f),
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIndexed() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).indexed().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).indexed().skip(1).count());
        Object[] indexedArray = FloatStream.of(1, 2, 3, 4, 5).indexed().toArray();
        assertEquals(5, indexedArray.length);
        assertEquals(0, ((IndexedFloat) indexedArray[0]).index());
        assertEquals(1f, ((IndexedFloat) indexedArray[0]).value(), 0.001f);
        assertEquals(4, ((IndexedFloat) indexedArray[4]).index());
        assertEquals(5f, ((IndexedFloat) indexedArray[4]).value(), 0.001f);
        Object[] indexedArraySkip = FloatStream.of(1, 2, 3, 4, 5).indexed().skip(1).toArray();
        assertEquals(4, indexedArraySkip.length);
        assertEquals(1, ((IndexedFloat) indexedArraySkip[0]).index());
        assertEquals(2f, ((IndexedFloat) indexedArraySkip[0]).value(), 0.001f);
        List<IndexedFloat> indexedList = FloatStream.of(1, 2, 3, 4, 5).indexed().toList();
        assertEquals(5, indexedList.size());
        assertEquals(0, indexedList.get(0).index());
        assertEquals(1f, indexedList.get(0).value(), 0.001f);
        List<IndexedFloat> indexedListSkip = FloatStream.of(1, 2, 3, 4, 5).indexed().skip(1).toList();
        assertEquals(4, indexedListSkip.size());
        assertEquals(1, indexedListSkip.get(0).index());
        assertEquals(2f, indexedListSkip.get(0).value(), 0.001f);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).indexed().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).indexed().skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterMergeWith() {
        FloatStream a = FloatStream.of(1, 3, 5);
        FloatStream b = FloatStream.of(2, 4, 6);
        assertEquals(6, a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(5, a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 }, a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(),
                0.001f);
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 }, a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(),
                0.001f);
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f), a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(6, a.map(e -> e).mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(5, a.map(e -> e).mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 },
                a.map(e -> e).mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), 0.001f);
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 },
                a.map(e -> e).mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), a.map(e -> e).mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f),
                a.map(e -> e).mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWith() {
        FloatStream a = FloatStream.of(1, 2, 3);
        FloatStream b = FloatStream.of(4, 5, 6);
        assertEquals(3, a.zipWith(b, (x, y) -> x + y).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(2, a.zipWith(b, (x, y) -> x + y).skip(1).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 5, 7, 9 }, a.zipWith(b, (x, y) -> x + y).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 7, 9 }, a.zipWith(b, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(5f, 7f, 9f), a.zipWith(b, (x, y) -> x + y).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(7f, 9f), a.zipWith(b, (x, y) -> x + y).skip(1).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(3, a.map(e -> e).zipWith(b, (x, y) -> x + y).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(2, a.map(e -> e).zipWith(b, (x, y) -> x + y).skip(1).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 5, 7, 9 }, a.map(e -> e).zipWith(b, (x, y) -> x + y).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 7, 9 }, a.map(e -> e).zipWith(b, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(5f, 7f, 9f), a.map(e -> e).zipWith(b, (x, y) -> x + y).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(7f, 9f), a.map(e -> e).zipWith(b, (x, y) -> x + y).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWith3() {
        FloatStream a = FloatStream.of(1, 2, 3);
        FloatStream b = FloatStream.of(4, 5, 6);
        FloatStream c = FloatStream.of(7, 8, 9);
        assertEquals(3, a.zipWith(b, c, (x, y, z) -> x + y + z).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(2, a.zipWith(b, c, (x, y, z) -> x + y + z).skip(1).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertArrayEquals(new float[] { 12, 15, 18 }, a.zipWith(b, c, (x, y, z) -> x + y + z).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertArrayEquals(new float[] { 15, 18 }, a.zipWith(b, c, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.toList(12f, 15f, 18f), a.zipWith(b, c, (x, y, z) -> x + y + z).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.toList(15f, 18f), a.zipWith(b, c, (x, y, z) -> x + y + z).skip(1).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(3, a.map(e -> e).zipWith(b, c, (x, y, z) -> x + y + z).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(2, a.map(e -> e).zipWith(b, c, (x, y, z) -> x + y + z).skip(1).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertArrayEquals(new float[] { 12, 15, 18 }, a.map(e -> e).zipWith(b, c, (x, y, z) -> x + y + z).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertArrayEquals(new float[] { 15, 18 }, a.map(e -> e).zipWith(b, c, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.toList(12f, 15f, 18f), a.map(e -> e).zipWith(b, c, (x, y, z) -> x + y + z).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.toList(15f, 18f), a.map(e -> e).zipWith(b, c, (x, y, z) -> x + y + z).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithDefaults() {
        FloatStream a = FloatStream.of(1, 2);
        FloatStream b = FloatStream.of(4, 5, 6);
        assertEquals(3, a.zipWith(b, 0, 0, (x, y) -> x + y).count());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertEquals(2, a.zipWith(b, 0, 0, (x, y) -> x + y).skip(1).count());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 5, 7, 6 }, a.zipWith(b, 0, 0, (x, y) -> x + y).toArray(), 0.001f);
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 7, 6 }, a.zipWith(b, 0, 0, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(5f, 7f, 6f), a.zipWith(b, 0, 0, (x, y) -> x + y).toList());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(7f, 6f), a.zipWith(b, 0, 0, (x, y) -> x + y).skip(1).toList());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertEquals(3, a.map(e -> e).zipWith(b, 0, 0, (x, y) -> x + y).count());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertEquals(2, a.map(e -> e).zipWith(b, 0, 0, (x, y) -> x + y).skip(1).count());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 5, 7, 6 }, a.map(e -> e).zipWith(b, 0, 0, (x, y) -> x + y).toArray(), 0.001f);
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 7, 6 }, a.map(e -> e).zipWith(b, 0, 0, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(5f, 7f, 6f), a.map(e -> e).zipWith(b, 0, 0, (x, y) -> x + y).toList());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(7f, 6f), a.map(e -> e).zipWith(b, 0, 0, (x, y) -> x + y).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWith3Defaults() {
        FloatStream a = FloatStream.of(1, 2);
        FloatStream b = FloatStream.of(4, 5, 6);
        FloatStream c = FloatStream.of(7);
        assertEquals(3, a.zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).count());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertEquals(2, a.zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).count());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertArrayEquals(new float[] { 12, 7, 6 }, a.zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).toArray(), 0.001f);
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertArrayEquals(new float[] { 7, 6 }, a.zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertEquals(N.toList(12f, 7f, 6f), a.zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).toList());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertEquals(N.toList(7f, 6f), a.zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).toList());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertEquals(3, a.map(e -> e).zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).count());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertEquals(2, a.map(e -> e).zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).count());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertArrayEquals(new float[] { 12, 7, 6 }, a.map(e -> e).zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).toArray(), 0.001f);
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertArrayEquals(new float[] { 7, 6 }, a.map(e -> e).zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertEquals(N.toList(12f, 7f, 6f), a.map(e -> e).zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).toList());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertEquals(N.toList(7f, 6f), a.map(e -> e).zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPsp() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).parallel().psp(s -> s.sorted()).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).parallel().psp(s -> s.sorted()).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).parallel().psp(s -> s.sorted()).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).parallel().psp(s -> s.sorted()).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).parallel().psp(s -> s.sorted()).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).parallel().psp(s -> s.sorted()).skip(1).toList());
        assertEquals(5, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).count());
        assertEquals(4, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRateLimited() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRateLimitedWithRateLimiter() {
        RateLimiter rateLimiter = RateLimiter.create(1000.0);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).rateLimited(rateLimiter).count());
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).rateLimited(rateLimiter).skip(1).count());
        rateLimiter = RateLimiter.create(1000.0);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).rateLimited(rateLimiter).toArray(), 0.001f);
        rateLimiter = RateLimiter.create(1000.0);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).rateLimited(rateLimiter).skip(1).toArray(), 0.001f);
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).rateLimited(rateLimiter).toList());
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).rateLimited(rateLimiter).skip(1).toList());
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).count());
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).skip(1).count());
        rateLimiter = RateLimiter.create(1000.0);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).toArray(), 0.001f);
        rateLimiter = RateLimiter.create(1000.0);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).skip(1).toArray(), 0.001f);
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).toList());
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDelay() {
        Duration duration = Duration.ofMillis(0);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).delay(duration).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).delay(duration).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).delay(duration).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDelayJavaTime() {
        java.time.Duration duration = java.time.Duration.ofMillis(0);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).delay(duration).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).delay(duration).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).delay(duration).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependStream() {
        FloatStream prepend = FloatStream.of(1, 2);
        FloatStream stream = FloatStream.of(3, 4, 5);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, stream.prepend(prepend).toArray(), 0.001f);
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, stream.prepend(prepend).skip(1).toArray(), 0.001f);
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), stream.prepend(prepend).toList());
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertEquals(N.toList(2f, 3f, 4f, 5f), stream.prepend(prepend).skip(1).toList());
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertEquals(5, stream.map(e -> e).prepend(prepend).count());
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertEquals(4, stream.map(e -> e).prepend(prepend).skip(1).count());
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, stream.map(e -> e).prepend(prepend).toArray(), 0.001f);
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, stream.map(e -> e).prepend(prepend).skip(1).toArray(), 0.001f);
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), stream.map(e -> e).prepend(prepend).toList());
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertEquals(N.toList(2f, 3f, 4f, 5f), stream.map(e -> e).prepend(prepend).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependOptional() {
        OptionalFloat op = OptionalFloat.of(1);
        assertEquals(6, FloatStream.of(2, 3, 4, 5, 6).prepend(op).count());
        op = OptionalFloat.of(1);
        assertEquals(5, FloatStream.of(2, 3, 4, 5, 6).prepend(op).skip(1).count());
        op = OptionalFloat.of(1);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 }, FloatStream.of(2, 3, 4, 5, 6).prepend(op).toArray(), 0.001f);
        op = OptionalFloat.of(1);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 }, FloatStream.of(2, 3, 4, 5, 6).prepend(op).skip(1).toArray(), 0.001f);
        op = OptionalFloat.of(1);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.of(2, 3, 4, 5, 6).prepend(op).toList());
        op = OptionalFloat.of(1);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f), FloatStream.of(2, 3, 4, 5, 6).prepend(op).skip(1).toList());
        op = OptionalFloat.of(1);
        assertEquals(6, FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).count());
        op = OptionalFloat.of(1);
        assertEquals(5, FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).skip(1).count());
        op = OptionalFloat.of(1);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 }, FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).toArray(), 0.001f);
        op = OptionalFloat.of(1);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 }, FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).skip(1).toArray(), 0.001f);
        op = OptionalFloat.of(1);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).toList());
        op = OptionalFloat.of(1);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f), FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendStream() {
        FloatStream append = FloatStream.of(4, 5);
        FloatStream stream = FloatStream.of(1, 2, 3);
        assertEquals(5, stream.append(append).count());
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertEquals(4, stream.append(append).skip(1).count());
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, stream.append(append).toArray(), 0.001f);
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, stream.append(append).skip(1).toArray(), 0.001f);
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), stream.append(append).toList());
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertEquals(N.toList(2f, 3f, 4f, 5f), stream.append(append).skip(1).toList());
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertEquals(5, stream.map(e -> e).append(append).count());
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertEquals(4, stream.map(e -> e).append(append).skip(1).count());
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, stream.map(e -> e).append(append).toArray(), 0.001f);
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, stream.map(e -> e).append(append).skip(1).toArray(), 0.001f);
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), stream.map(e -> e).append(append).toList());
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertEquals(N.toList(2f, 3f, 4f, 5f), stream.map(e -> e).append(append).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendOptional() {
        OptionalFloat op = OptionalFloat.of(6);
        assertEquals(6, FloatStream.of(1, 2, 3, 4, 5).append(op).count());
        op = OptionalFloat.of(6);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).append(op).skip(1).count());
        op = OptionalFloat.of(6);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 }, FloatStream.of(1, 2, 3, 4, 5).append(op).toArray(), 0.001f);
        op = OptionalFloat.of(6);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 }, FloatStream.of(1, 2, 3, 4, 5).append(op).skip(1).toArray(), 0.001f);
        op = OptionalFloat.of(6);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.of(1, 2, 3, 4, 5).append(op).toList());
        op = OptionalFloat.of(6);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f), FloatStream.of(1, 2, 3, 4, 5).append(op).skip(1).toList());
        op = OptionalFloat.of(6);
        assertEquals(6, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).count());
        op = OptionalFloat.of(6);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).skip(1).count());
        op = OptionalFloat.of(6);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).toArray(), 0.001f);
        op = OptionalFloat.of(6);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).skip(1).toArray(), 0.001f);
        op = OptionalFloat.of(6);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).toList());
        op = OptionalFloat.of(6);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipStreams() {
        FloatStream a = FloatStream.of(1, 2, 3);
        FloatStream b = FloatStream.of(4, 5, 6);
        assertEquals(3, FloatStream.zip(a, b, (x, y) -> x + y).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(2, FloatStream.zip(a, b, (x, y) -> x + y).skip(1).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 5, 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).skip(1).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(3, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(2, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 5, 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZip3Streams() {
        FloatStream a = FloatStream.of(1, 2, 3);
        FloatStream b = FloatStream.of(4, 5, 6);
        FloatStream c = FloatStream.of(7, 8, 9);
        assertEquals(3, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(2, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.toList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.toList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(3, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(2, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.toList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.toList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipCollectionStreams() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(3, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).count());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(2, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).skip(1).count());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).skip(1).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(N.toList(12f, 15f, 18f), FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).toList());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(N.toList(15f, 18f), FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).skip(1).toList());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(3, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).count());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(2, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).skip(1).count());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).skip(1).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(N.toList(12f, 15f, 18f), FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).toList());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(N.toList(15f, 18f), FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeStreams() {
        FloatStream a = FloatStream.of(1, 3, 5);
        FloatStream b = FloatStream.of(2, 4, 6);
        assertEquals(6, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), 0.001f);
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(6, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(5, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray(), 0.001f);
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMerge3Streams() {
        FloatStream a = FloatStream.of(1, 4, 7);
        FloatStream b = FloatStream.of(2, 5, 8);
        FloatStream c = FloatStream.of(3, 6, 9);
        assertEquals(9, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertEquals(8, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), 0.001f);
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertEquals(9, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertEquals(8, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray(), 0.001f);
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeCollectionStreams() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(9, FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(8, FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(9, FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(8, FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfFloatArrayRange_SubRange() {
        FloatStream result = FloatStream.of(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 1, 3);
        assertArrayEquals(new float[] { 2.0f, 3.0f }, result.toArray(), DELTA);
    }

    @Test
    public void testOfBoxedArray() {
        FloatStream result = FloatStream.of(new Float[] { 1.0f, 2.0f, 3.0f });
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result.toArray(), DELTA);
    }

    @Test
    public void testOfBoxedArrayRange() {
        FloatStream result = FloatStream.of(new Float[] { 1.0f, 2.0f, 3.0f, 4.0f }, 0, 2);
        assertArrayEquals(new float[] { 1.0f, 2.0f }, result.toArray(), DELTA);
    }

    @Test
    public void testOfFloatBuffer_Test() {
        FloatBuffer buffer = FloatBuffer.wrap(new float[] { 1.0f, 2.0f, 3.0f });
        FloatStream result = FloatStream.of(buffer);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result.toArray(), DELTA);
    }

    @Test
    public void testDebounce_SingleElement() {
        float[] result = FloatStream.of(42.5f).debounce(1, com.landawn.abacus.util.Duration.ofMillis(100)).toArray();

        assertEquals(1, result.length);
        assertEquals(42.5f, result[0], 0.001f);
    }

    @Test
    public void testDebounce_MaxWindowSizeOne() {
        // Only 1 element allowed per window
        float[] result = FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).debounce(1, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertEquals(1, result.length);
        assertEquals(1.0f, result[0], 0.001f);
    }

    @Test
    public void testDebounce_WithLargeMaxWindowSize() {
        float[] input = new float[1000];
        for (int i = 0; i < 1000; i++) {
            input[i] = i;
        }

        float[] result = FloatStream.of(input).debounce(500, com.landawn.abacus.util.Duration.ofSeconds(10)).toArray();

        assertEquals(500, result.length);
    }

    @Test
    public void testDebounce_WithSpecialValues() {
        float[] result = FloatStream.of(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1.0f, 2.0f)
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals(3, result.length);
        assertTrue(Float.isNaN(result[0]));
        assertEquals(Float.POSITIVE_INFINITY, result[1], 0.001f);
        assertEquals(Float.NEGATIVE_INFINITY, result[2], 0.001f);
    }

    @Test
    public void testOfEmpty() {
        assertEquals(0, FloatStream.empty().count());
    }

    @Test
    public void testOfSingle() {
        assertArrayEquals(new float[] { 3.14f }, FloatStream.of(3.14f).toArray(), DELTA);
    }

    @Test
    public void testOfMultiple() {
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f }, FloatStream.of(1.1f, 2.2f, 3.3f).toArray(), DELTA);
    }

    @Test
    public void testAnyAllNoneMatch() {
        assertTrue(FloatStream.of(1.0f, 2.0f, 3.0f).anyMatch(f -> f > 2.5f));
        assertFalse(FloatStream.of(1.0f, 2.0f, 3.0f).allMatch(f -> f > 2.5f));
        assertTrue(FloatStream.of(1.0f, 2.0f, 3.0f).noneMatch(f -> f > 5.0f));
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIfEmpty() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).ifEmpty(() -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).ifEmpty(() -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).ifEmpty(() -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).ifEmpty(() -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).ifEmpty(() -> {
        }).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).ifEmpty(() -> {
        }).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartial() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).count());
        assertArrayEquals(new float[] { 2, 6, 10 },
                FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 6, 10 },
                FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(2f, 6f, 10f),
                FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).toList());
        assertEquals(N.toList(6f, 10f),
                FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).count());
        assertEquals(2,
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).count());
        assertArrayEquals(new float[] { 2, 6, 10 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 6, 10 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).toArray(),
                0.001f);
        assertEquals(N.toList(2f, 6f, 10f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).toList());
        assertEquals(N.toList(6f, 10f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmptySupplier() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).toArray(),
                0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toArray(),
                0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toList());

        assertEquals(3, FloatStream.empty().appendIfEmpty(() -> FloatStream.of(6, 7, 8)).count());
        assertEquals(2, FloatStream.empty().appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).count());
        assertArrayEquals(new float[] { 6, 7, 8 }, FloatStream.empty().appendIfEmpty(() -> FloatStream.of(6, 7, 8)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 8 }, FloatStream.empty().appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toArray(), 0.001f);
    }

    @Test
    public void testStreamCreatedAfterDefaultIfEmpty() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).toArray(),
                0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toArray(),
                0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toList());
    }

    @Test
    public void testOfFloatArray_Empty() {
        FloatStream result = FloatStream.of(new float[] {});
        assertEquals(0, result.count());
    }

    @Test
    public void testOfFloatBuffer_Empty() {
        FloatBuffer buffer = FloatBuffer.wrap(new float[] {});
        FloatStream result = FloatStream.of(buffer);
        assertEquals(0, result.count());
    }

    @Test
    public void testDefaultIfEmpty_NonEmpty() {
        float[] result = createFloatStream(1.0f, 2.0f).defaultIfEmpty(() -> FloatStream.of(99.0f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f }, result, DELTA);
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveMaxWindowSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            FloatStream.of(1.0f, 2.0f, 3.0f).debounce(0, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            FloatStream.of(1.0f, 2.0f, 3.0f).debounce(-1, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();
        });
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveDuration() {
        assertThrows(IllegalArgumentException.class, () -> {
            FloatStream.of(1.0f, 2.0f, 3.0f).debounce(5, com.landawn.abacus.util.Duration.ofMillis(0)).toArray();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            FloatStream.of(1.0f, 2.0f, 3.0f).debounce(5, com.landawn.abacus.util.Duration.ofMillis(-100)).toArray();
        });
    }

    @Test
    public void testInvalidStep() {
        assertThrows(IllegalArgumentException.class, () -> FloatStream.of(1f, 2f).step(0));
        assertThrows(IllegalArgumentException.class, () -> FloatStream.of(1f, 2f).step(-1));
    }

    @Test
    public void testNextOnExhaustedIterator() {
        FloatIterator it = FloatStream.of(1.0f).iterator();
        it.nextFloat();
        assertThrows(NoSuchElementException.class, it::nextFloat);
    }

    @Test
    public void testStreamCreatedAfterThrowIfEmpty() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterThrowIfEmptyWithSupplier() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).toArray(),
                0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).toArray(),
                0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).toList());
    }

    @Test
    @DisplayName("flatten() should flatten two-dimensional array")
    public void testFlatten2D() {
        float[][] array = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
        FloatStream stream = FloatStream.flatten(array);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    @DisplayName("flatten() with vertically flag should flatten correctly")
    public void testFlatten2DVertically() {
        float[][] array = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
        FloatStream stream = FloatStream.flatten(array, false);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());

        stream = FloatStream.flatten(array, true);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 2.0f, 4.0f }, stream.toArray());
    }

    @Test
    @DisplayName("flatten() with alignment value should pad arrays")
    public void testFlatten2DWithAlignment() {
        float[][] array = { { 1.0f, 2.0f }, { 3.0f, 4.0f, 5.0f } };
        FloatStream stream = FloatStream.flatten(array, 0.0f, false);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 0.0f, 3.0f, 4.0f, 5.0f }, stream.toArray());

        stream = FloatStream.flatten(array, 0.0f, true);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 2.0f, 4.0f, 0.0f, 5.0f }, stream.toArray());
    }

    @Test
    @DisplayName("flatten() should flatten three-dimensional array")
    public void testFlatten3D() {
        float[][][] array = { { { 1.0f, 2.0f }, { 3.0f, 4.0f } } };
        FloatStream stream = FloatStream.flatten(array);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    // Edge cases

    @Test
    @DisplayName("flatten() with alignment horizontally additional")
    public void testFlattenAlignmentAdditional() {
        float[][] array = { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f }, { 6.0f } };
        FloatStream stream = FloatStream.flatten(array, 0.0f, true);
        float[] result = stream.toArray();
        assertEquals(9, result.length);
        assertEquals(1.0f, result[0], 0.001);
        assertEquals(4.0f, result[1], 0.001);
        assertEquals(6.0f, result[2], 0.001);
    }

    @Test
    public void testStreamCreatedAfterFlatten2D() {
        float[][] array = { { 1, 2 }, { 3, 4 }, { 5 } };
        assertEquals(5, FloatStream.flatten(array).count());
        assertEquals(4, FloatStream.flatten(array).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.flatten(array).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.flatten(array).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.flatten(array).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.flatten(array).skip(1).toList());
        assertEquals(5, FloatStream.flatten(array).map(e -> e).count());
        assertEquals(4, FloatStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatten2DVertically() {
        float[][] array = { { 1, 2, 3 }, { 4, 5, 6 } };
        assertEquals(6, FloatStream.flatten(array, true).count());
        assertEquals(5, FloatStream.flatten(array, true).skip(1).count());
        assertArrayEquals(new float[] { 1, 4, 2, 5, 3, 6 }, FloatStream.flatten(array, true).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 2, 5, 3, 6 }, FloatStream.flatten(array, true).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 4f, 2f, 5f, 3f, 6f), FloatStream.flatten(array, true).toList());
        assertEquals(N.toList(4f, 2f, 5f, 3f, 6f), FloatStream.flatten(array, true).skip(1).toList());
        assertEquals(6, FloatStream.flatten(array, true).map(e -> e).count());
        assertEquals(5, FloatStream.flatten(array, true).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 4, 2, 5, 3, 6 }, FloatStream.flatten(array, true).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 2, 5, 3, 6 }, FloatStream.flatten(array, true).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 4f, 2f, 5f, 3f, 6f), FloatStream.flatten(array, true).map(e -> e).toList());
        assertEquals(N.toList(4f, 2f, 5f, 3f, 6f), FloatStream.flatten(array, true).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatten3D() {
        float[][][] array = { { { 1, 2 }, { 3 } }, { { 4, 5 } } };
        assertEquals(5, FloatStream.flatten(array).count());
        assertEquals(4, FloatStream.flatten(array).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.flatten(array).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.flatten(array).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.flatten(array).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.flatten(array).skip(1).toList());
        assertEquals(5, FloatStream.flatten(array).map(e -> e).count());
        assertEquals(4, FloatStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    @DisplayName("flatten() should flatten two-dimensional array")
    public void testFlatten2D_2() {
        float[][] array = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
        float[] result = FloatStream.flatten(array, false).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result);

        result = FloatStream.flatten(array, true).toArray();
        assertArrayEquals(new float[] { 1.0f, 3.0f, 2.0f, 4.0f }, result);
    }

    @Test
    @DisplayName("flatten() should flatten two-dimensional array")
    public void testFlatten2D_3() {
        float[][] array = { { 1.0f, 2.0f }, { 3.0f, 4.0f, 5.f } };
        float[] result = FloatStream.flatten(array, 0.0f, false).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 0.0f, 3.0f, 4.0f, 5.0f }, result);

        result = FloatStream.flatten(array, 0.0f, true).toArray();
        assertArrayEquals(new float[] { 1.0f, 3.0f, 2.0f, 4.0f, 0.0f, 5.0f }, result);
    }

    @Test
    public void testFlatten2D_Test() {
        float[][] arrays = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
        FloatStream result = FloatStream.flatten(arrays);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result.toArray(), DELTA);
    }

    @Test
    public void testFlatten2DVertically_Test() {
        float[][] arrays = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
        FloatStream result = FloatStream.flatten(arrays, true);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 2.0f, 4.0f }, result.toArray(), DELTA);
    }

    @Test
    public void testFlatten2DWithAlignment_Test() {
        float[][] arrays = { { 1.0f, 2.0f }, { 3.0f } };
        FloatStream result = FloatStream.flatten(arrays, 0.0f, true);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 2.0f, 0.0f }, result.toArray(), DELTA);
    }

    @Test
    public void testFlatten3D_Test() {
        float[][][] arrays = { { { 1.0f, 2.0f }, { 3.0f } }, { { 4.0f, 5.0f, 6.0f } } };
        FloatStream result = FloatStream.flatten(arrays);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result.toArray(), DELTA);
    }

    @Test
    @DisplayName("flatten() with empty array should return empty stream")
    public void testFlattenEmpty() {
        float[][] array = {};
        FloatStream stream = FloatStream.flatten(array);
        assertEquals(0, stream.count());
    }

    @Test
    @DisplayName("repeat() should create stream with repeated element")
    public void testRepeat() {
        FloatStream stream = FloatStream.repeat(2.5f, 3);
        assertArrayEquals(new float[] { 2.5f, 2.5f, 2.5f }, stream.toArray());
    }

    @Test
    public void testStreamCreatedAfterRepeat() {
        assertEquals(5, FloatStream.repeat(3.14f, 5).count());
        assertEquals(4, FloatStream.repeat(3.14f, 5).skip(1).count());
        assertArrayEquals(new float[] { 3.14f, 3.14f, 3.14f, 3.14f, 3.14f }, FloatStream.repeat(3.14f, 5).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3.14f, 3.14f, 3.14f, 3.14f }, FloatStream.repeat(3.14f, 5).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3.14f, 3.14f, 3.14f, 3.14f, 3.14f), FloatStream.repeat(3.14f, 5).toList());
        assertEquals(N.toList(3.14f, 3.14f, 3.14f, 3.14f), FloatStream.repeat(3.14f, 5).skip(1).toList());
        assertEquals(5, FloatStream.repeat(3.14f, 5).map(e -> e).count());
        assertEquals(4, FloatStream.repeat(3.14f, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 3.14f, 3.14f, 3.14f, 3.14f, 3.14f }, FloatStream.repeat(3.14f, 5).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3.14f, 3.14f, 3.14f, 3.14f }, FloatStream.repeat(3.14f, 5).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3.14f, 3.14f, 3.14f, 3.14f, 3.14f), FloatStream.repeat(3.14f, 5).map(e -> e).toList());
        assertEquals(N.toList(3.14f, 3.14f, 3.14f, 3.14f), FloatStream.repeat(3.14f, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testRepeat_02() {
        FloatStream stream = FloatStream.repeat(1.0f, 1000);
        assertEquals(1000, stream.mapToDouble(e -> e).count());
    }

    @Test
    public void testRepeat_Test() {
        float[] result = FloatStream.repeat(5.0f, 3).toArray();
        assertArrayEquals(new float[] { 5.0f, 5.0f, 5.0f }, result, DELTA);
    }

    @Test
    @DisplayName("repeat() with zero count should return empty stream")
    public void testRepeatZero() {
        FloatStream stream = FloatStream.repeat(1.0f, 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRepeat_WithZero() {
        FloatStream result = FloatStream.repeat(1.0f, 0);
        assertEquals(0, result.count());
    }

    @Test
    @DisplayName("negative arguments should throw")
    public void testNegativeArguments() {
        assertThrows(IllegalArgumentException.class, () -> FloatStream.repeat(1.0f, -1));
        assertThrows(IllegalArgumentException.class, () -> createFloatStream(1.0f, 2.0f).limit(-1));
        assertThrows(IllegalArgumentException.class, () -> createFloatStream(1.0f, 2.0f).skip(-1));
    }

    @Test
    public void testRepeat_Negative() {
        assertThrows(IllegalArgumentException.class, () -> FloatStream.repeat(1.0f, -1));
    }

    @Test
    public void testRepeat_NegativeThrows() {
        assertThrows(IllegalArgumentException.class, () -> FloatStream.repeat(1.0f, -1));
    }

    @Test
    @DisplayName("random() should generate random stream")
    public void testRandom() {
        FloatStream stream = FloatStream.random().limit(5);
        float[] result = stream.toArray();
        assertEquals(5, result.length);
        for (float f : result) {
            assertTrue(f >= 0.0f && f < 1.0f);
        }
    }

    @Test
    public void testStreamCreatedAfterRandom() {
        assertEquals(10, FloatStream.random().limit(10).count());
        assertEquals(9, FloatStream.random().limit(10).skip(1).count());
        float[] randomArray = FloatStream.random().limit(10).toArray();
        assertEquals(10, randomArray.length);
        for (float f : randomArray) {
            assertTrue(f >= 0.0f && f < 1.0f);
        }
        float[] randomArraySkip = FloatStream.random().limit(10).skip(1).toArray();
        assertEquals(9, randomArraySkip.length);
        List<Float> randomList = FloatStream.random().limit(10).toList();
        assertEquals(10, randomList.size());
        List<Float> randomListSkip = FloatStream.random().limit(10).skip(1).toList();
        assertEquals(9, randomListSkip.size());

        assertEquals(10, FloatStream.random().map(e -> e).limit(10).count());
        assertEquals(9, FloatStream.random().map(e -> e).limit(10).skip(1).count());
    }

    @Test
    public void testRandom_Test() {
        FloatStream result = FloatStream.random().limit(10);
        assertEquals(10, result.count());
    }

    @Test
    @DisplayName("iterate() with hasNext and next should create stream")
    public void testIterateWithHasNextNext() {
        AtomicInteger counter = new AtomicInteger(0);
        FloatStream stream = FloatStream.iterate(() -> counter.get() < 3, () -> counter.getAndIncrement() + 1.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    @DisplayName("iterate() with init, hasNext and function should create stream")
    public void testIterateWithInitHasNextFunction() {
        FloatStream stream = FloatStream.iterate(1.0f, () -> true, x -> x + 1.0f).limit(3);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    @DisplayName("iterate() with init, predicate and function should create stream")
    public void testIterateWithPredicate() {
        FloatStream stream = FloatStream.iterate(1.0f, x -> x < 5.0f, x -> x + 1.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    @DisplayName("iterate() with init and function should create infinite stream")
    public void testIterateInfinite() {
        FloatStream stream = FloatStream.iterate(1.0f, x -> x + 1.0f).limit(3);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    @DisplayName("iterate() with immediate false hasNext additional")
    public void testIterateFalseHasNextAdditional() {
        FloatStream stream = FloatStream.iterate(1.0f, () -> false, f -> f + 1.0f);
        assertEquals(0, stream.count());
    }

    @Test
    public void testIterate() {
        float[] expected = { 1.0f, 1.5f, 2.0f, 2.5f, 3.0f };
        assertArrayEquals(expected, FloatStream.iterate(1.0f, f -> f + 0.5f).limit(5).toArray(), DELTA);
    }

    @Test
    public void testStreamCreatedAfterIterateWithHasNext() {
        int[] counter = { 0 };
        assertEquals(5, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).count());
        counter[0] = 0;
        assertEquals(4, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).skip(1).count());
        counter[0] = 0;
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4 }, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).toArray(), 0.001f);
        counter[0] = 0;
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).skip(1).toArray(), 0.001f);
        counter[0] = 0;
        assertEquals(N.toList(0f, 1f, 2f, 3f, 4f), FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).toList());
        counter[0] = 0;
        assertEquals(N.toList(1f, 2f, 3f, 4f), FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).skip(1).toList());
        counter[0] = 0;
        assertEquals(5, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).count());
        counter[0] = 0;
        assertEquals(4, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).skip(1).count());
        counter[0] = 0;
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4 }, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).toArray(), 0.001f);
        counter[0] = 0;
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).skip(1).toArray(), 0.001f);
        counter[0] = 0;
        assertEquals(N.toList(0f, 1f, 2f, 3f, 4f), FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).toList());
        counter[0] = 0;
        assertEquals(N.toList(1f, 2f, 3f, 4f), FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIterateWithInitHasNextF() {
        int[] counter = { 0 };
        assertEquals(6, FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).count());
        counter[0] = 0;
        assertEquals(5, FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).skip(1).count());
        counter[0] = 0;
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4, 5 }, FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).toArray(), 0.001f);
        counter[0] = 0;
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).skip(1).toArray(), 0.001f);
        counter[0] = 0;
        assertEquals(N.toList(0f, 1f, 2f, 3f, 4f, 5f), FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).toList());
        counter[0] = 0;
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).skip(1).toList());
        counter[0] = 0;
        assertEquals(6, FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).map(e -> e).count());
        counter[0] = 0;
        assertEquals(5, FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).map(e -> e).skip(1).count());
        counter[0] = 0;
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4, 5 }, FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).map(e -> e).toArray(), 0.001f);
        counter[0] = 0;
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).map(e -> e).skip(1).toArray(), 0.001f);
        counter[0] = 0;
        assertEquals(N.toList(0f, 1f, 2f, 3f, 4f, 5f), FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).map(e -> e).toList());
        counter[0] = 0;
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIterateWithInitPredicateF() {
        assertEquals(5, FloatStream.iterate(0, f -> f < 5, f -> f + 1).count());
        assertEquals(4, FloatStream.iterate(0, f -> f < 5, f -> f + 1).skip(1).count());
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4 }, FloatStream.iterate(0, f -> f < 5, f -> f + 1).toArray(), 0.001f);
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.iterate(0, f -> f < 5, f -> f + 1).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(0f, 1f, 2f, 3f, 4f), FloatStream.iterate(0, f -> f < 5, f -> f + 1).toList());
        assertEquals(N.toList(1f, 2f, 3f, 4f), FloatStream.iterate(0, f -> f < 5, f -> f + 1).skip(1).toList());
        assertEquals(5, FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).count());
        assertEquals(4, FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4 }, FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(0f, 1f, 2f, 3f, 4f), FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).toList());
        assertEquals(N.toList(1f, 2f, 3f, 4f), FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIterateUnlimited() {
        assertEquals(10, FloatStream.iterate(1, f -> f * 2).limit(10).count());
        assertEquals(9, FloatStream.iterate(1, f -> f * 2).limit(10).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512 }, FloatStream.iterate(1, f -> f * 2).limit(10).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 4, 8, 16, 32, 64, 128, 256, 512 }, FloatStream.iterate(1, f -> f * 2).limit(10).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f, 512f), FloatStream.iterate(1, f -> f * 2).limit(10).toList());
        assertEquals(N.toList(2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f, 512f), FloatStream.iterate(1, f -> f * 2).limit(10).skip(1).toList());
        assertEquals(10, FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).count());
        assertEquals(9, FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512 }, FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 4, 8, 16, 32, 64, 128, 256, 512 }, FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).skip(1).toArray(),
                0.001f);
        assertEquals(N.toList(1f, 2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f, 512f), FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).toList());
        assertEquals(N.toList(2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f, 512f), FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).skip(1).toList());
    }

    @Test
    public void test_iterate_02() {
        FloatStream stream = FloatStream.iterate(() -> true, () -> 1.0f);
        assertEquals(10, stream.limit(10).mapToDouble(e -> e).count());

        stream = FloatStream.iterate(1.0f, () -> true, it -> it);
        assertEquals(10, stream.limit(10).mapToInt(e -> (int) e).sum());

        stream = FloatStream.iterate(1.0f, it -> true, it -> it);
        assertEquals(10, stream.limit(10).mapToInt(e -> (int) e).sum());
    }

    @Test
    public void testIterateWithHasNext_Test() {
        AtomicInteger counter = new AtomicInteger(0);
        float[] result = FloatStream.iterate(() -> counter.get() < 3, () -> counter.getAndIncrement() * 1.0f).toArray();
        assertArrayEquals(new float[] { 0.0f, 1.0f, 2.0f }, result, DELTA);
    }

    @Test
    public void testIterateWithInitHasNext_Test() {
        float[] result = FloatStream.iterate(1.0f, () -> true, f -> f + 1.0f).limit(3).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testIterateWithPredicate_Test() {
        float[] result = FloatStream.iterate(1.0f, f -> f < 4.0f, f -> f + 1.0f).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testIterateInfinite_Test() {
        float[] result = FloatStream.iterate(1.0f, f -> f + 1.0f).limit(3).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    @DisplayName("generate() should create stream with supplier")
    public void testGenerate() {
        FloatStream stream = FloatStream.generate(() -> 5.0f).limit(3);
        assertArrayEquals(new float[] { 5.0f, 5.0f, 5.0f }, stream.toArray());
    }

    @Test
    public void testStreamCreatedAfterGenerate() {
        float[] value = { 0 };
        assertEquals(5, FloatStream.generate(() -> value[0]++).limit(5).count());
        value[0] = 0;
        assertEquals(4, FloatStream.generate(() -> value[0]++).limit(5).skip(1).count());
        value[0] = 0;
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4 }, FloatStream.generate(() -> value[0]++).limit(5).toArray(), 0.001f);
        value[0] = 0;
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.generate(() -> value[0]++).limit(5).skip(1).toArray(), 0.001f);
        value[0] = 0;
        assertEquals(N.toList(0f, 1f, 2f, 3f, 4f), FloatStream.generate(() -> value[0]++).limit(5).toList());
        value[0] = 0;
        assertEquals(N.toList(1f, 2f, 3f, 4f), FloatStream.generate(() -> value[0]++).limit(5).skip(1).toList());
        value[0] = 0;
        assertEquals(5, FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).count());
        value[0] = 0;
        assertEquals(4, FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).skip(1).count());
        value[0] = 0;
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4 }, FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).toArray(), 0.001f);
        value[0] = 0;
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).skip(1).toArray(), 0.001f);
        value[0] = 0;
        assertEquals(N.toList(0f, 1f, 2f, 3f, 4f), FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).toList());
        value[0] = 0;
        assertEquals(N.toList(1f, 2f, 3f, 4f), FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).skip(1).toList());
    }

    @Test
    public void testGenerate_Test() {
        AtomicInteger counter = new AtomicInteger(0);
        float[] result = FloatStream.generate(() -> counter.getAndIncrement() * 1.0f).limit(3).toArray();
        assertArrayEquals(new float[] { 0.0f, 1.0f, 2.0f }, result, DELTA);
    }

    @Test
    public void testGenerate_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> FloatStream.generate(null));
    }

    @Test
    public void testGenerate_NullThrows() {
        assertThrows(IllegalArgumentException.class, () -> FloatStream.generate(null));
    }

    @Test
    @DisplayName("concat() with arrays should concatenate")
    public void testConcatArrays() {
        float[] a = { 1.0f, 2.0f };
        float[] b = { 3.0f, 4.0f };
        FloatStream stream = FloatStream.concat(a, b);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    @DisplayName("concat() with streams should concatenate")
    public void testConcatStreams() {
        FloatStream a = FloatStream.of(1.0f, 2.0f);
        FloatStream b = FloatStream.of(3.0f, 4.0f);
        FloatStream stream = FloatStream.concat(a, b);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    @DisplayName("concat() with list of arrays should concatenate")
    public void testConcatListOfArrays() {
        List<float[]> list = Arrays.asList(new float[] { 1.0f, 2.0f }, new float[] { 3.0f, 4.0f });
        FloatStream stream = FloatStream.concat(list);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    @DisplayName("concat() with collection of streams should concatenate")
    public void testConcatCollectionOfStreams() {
        List<FloatStream> list = Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(3.0f, 4.0f));
        FloatStream stream = FloatStream.concat(list);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    public void testConcat() {
        FloatStream s1 = FloatStream.of(1.1f, 2.2f);
        FloatStream s2 = FloatStream.of(3.3f, 4.4f);
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f, 4.4f }, FloatStream.concat(s1, s2).toArray(), DELTA);
    }

    @Test
    public void testStreamCreatedAfterConcatArrays() {
        float[] a1 = { 1, 2 };
        float[] a2 = { 3, 4 };
        float[] a3 = { 5 };
        assertEquals(5, FloatStream.concat(a1, a2, a3).count());
        assertEquals(4, FloatStream.concat(a1, a2, a3).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.concat(a1, a2, a3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.concat(a1, a2, a3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.concat(a1, a2, a3).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.concat(a1, a2, a3).skip(1).toList());
        assertEquals(5, FloatStream.concat(a1, a2, a3).map(e -> e).count());
        assertEquals(4, FloatStream.concat(a1, a2, a3).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.concat(a1, a2, a3).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.concat(a1, a2, a3).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.concat(a1, a2, a3).map(e -> e).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.concat(a1, a2, a3).map(e -> e).skip(1).toList());
    }

    @Test
    public void test_concat_02() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 4.0f, 5.0f, 6.0f };
        float[] result = FloatStream.concat(a, b).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result);

        result = FloatStream.concat(N.toList(a, b)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result);

        result = FloatStream.concatIterators(N.toList(FloatIterator.of(a), FloatIterator.of(b))).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result);
    }

    @Test
    public void testConcatArrays_Test() {
        float[] result = FloatStream.concat(new float[] { 1.0f, 2.0f }, new float[] { 3.0f, 4.0f }).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    public void testConcatStreams_Test() {
        float[] result = FloatStream.concat(FloatStream.of(1.0f, 2.0f), FloatStream.of(3.0f, 4.0f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    public void testConcatListOfArrays_Test() {
        List<float[]> arrays = Arrays.asList(new float[] { 1.0f, 2.0f }, new float[] { 3.0f, 4.0f });
        float[] result = FloatStream.concat(arrays).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    public void testConcatCollectionOfStreams_Test() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(3.0f, 4.0f));
        float[] result = FloatStream.concat(streams).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    @DisplayName("concat() with iterators should concatenate")
    public void testConcatIterators() {
        FloatIterator a = FloatIterator.of(1.0f, 2.0f);
        FloatIterator b = FloatIterator.of(3.0f, 4.0f);
        FloatStream stream = FloatStream.concat(a, b);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    @DisplayName("concatIterators() should concatenate iterators")
    public void testConcatIteratorsCollection() {
        List<FloatIterator> list = Arrays.asList(FloatIterator.of(1.0f, 2.0f), FloatIterator.of(3.0f, 4.0f));
        FloatStream stream = FloatStream.concatIterators(list);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    public void testConcatIterators_Test() {
        float[] result = FloatStream.concat(FloatIterator.of(new float[] { 1.0f, 2.0f }), FloatIterator.of(new float[] { 3.0f, 4.0f })).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    public void testConcatIteratorsCollection_Test() {
        List<FloatIterator> iterators = Arrays.asList(FloatIterator.of(new float[] { 1.0f, 2.0f }), FloatIterator.of(new float[] { 3.0f, 4.0f }));
        float[] result = FloatStream.concatIterators(iterators).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zip() two arrays with binary function")
    public void testZipTwoArrays() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 4.0f, 5.0f, 6.0f };
        FloatStream stream = FloatStream.zip(a, b, Float::sum);
        assertArrayEquals(new float[] { 5.0f, 7.0f, 9.0f }, stream.toArray());
    }

    @Test
    @DisplayName("zip() three arrays with ternary function")
    public void testZipThreeArrays() {
        float[] a = { 1.0f, 2.0f };
        float[] b = { 3.0f, 4.0f };
        float[] c = { 5.0f, 6.0f };
        FloatStream stream = FloatStream.zip(a, b, c, (x, y, z) -> x + y + z);
        assertArrayEquals(new float[] { 9.0f, 12.0f }, stream.toArray());
    }

    @Test
    @DisplayName("zip() two iterators with binary function")
    public void testZipTwoIterators() {
        FloatIterator a = FloatIterator.of(1.0f, 2.0f);
        FloatIterator b = FloatIterator.of(4.0f, 5.0f);
        FloatStream stream = FloatStream.zip(a, b, Float::sum);
        assertArrayEquals(new float[] { 5.0f, 7.0f }, stream.toArray());
    }

    @Test
    @DisplayName("zip() three iterators with ternary function")
    public void testZipThreeIterators() {
        FloatIterator a = FloatIterator.of(1.0f, 2.0f);
        FloatIterator b = FloatIterator.of(3.0f, 4.0f);
        FloatIterator c = FloatIterator.of(5.0f, 6.0f);
        FloatStream stream = FloatStream.zip(a, b, c, (x, y, z) -> x + y + z);
        assertArrayEquals(new float[] { 9.0f, 12.0f }, stream.toArray());
    }

    @Test
    @DisplayName("zip() two streams with binary function")
    public void testZipTwoStreams() {
        FloatStream a = FloatStream.of(1.0f, 2.0f);
        FloatStream b = FloatStream.of(4.0f, 5.0f);
        FloatStream stream = FloatStream.zip(a, b, Float::sum);
        assertArrayEquals(new float[] { 5.0f, 7.0f }, stream.toArray());
    }

    @Test
    @DisplayName("zip() three streams with ternary function")
    public void testZipThreeStreams() {
        FloatStream a = FloatStream.of(1.0f, 2.0f);
        FloatStream b = FloatStream.of(3.0f, 4.0f);
        FloatStream c = FloatStream.of(5.0f, 6.0f);
        FloatStream stream = FloatStream.zip(a, b, c, (x, y, z) -> x + y + z);
        assertArrayEquals(new float[] { 9.0f, 12.0f }, stream.toArray());
    }

    @Test
    @DisplayName("zip() collection of streams with N-function")
    public void testZipCollectionOfStreams() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(3.0f, 4.0f));
        FloatStream stream = FloatStream.zip(streams, floats -> floats[0] + floats[1]);
        assertArrayEquals(new float[] { 4.0f, 6.0f }, stream.toArray());
    }

    @Test
    @DisplayName("zip() with padding for two arrays")
    public void testZipTwoArraysWithPadding() {
        float[] a = { 1.0f, 2.0f };
        float[] b = { 4.0f, 5.0f, 6.0f };
        FloatStream stream = FloatStream.zip(a, b, 0.0f, 0.0f, Float::sum);
        assertArrayEquals(new float[] { 5.0f, 7.0f, 6.0f }, stream.toArray());
    }

    @Test
    @DisplayName("zip() with padding for three arrays")
    public void testZipThreeArraysWithPadding() {
        float[] a = { 1.0f };
        float[] b = { 4.0f, 5.0f };
        float[] c = { 7.0f, 8.0f, 9.0f };
        FloatStream stream = FloatStream.zip(a, b, c, 100.0f, 200.0f, 300.0f, (x, y, z) -> x + y + z);
        assertArrayEquals(new float[] { 12.0f, 113.0f, 309.0f }, stream.toArray());
    }

    @Test
    @DisplayName("zip() with padding for two iterators")
    public void testZipTwoIteratorsWithPadding() {
        FloatIterator a = FloatIterator.of(1.0f, 2.0f);
        FloatIterator b = FloatIterator.of(4.0f, 5.0f, 6.0f);
        FloatStream stream = FloatStream.zip(a, b, 0.0f, 0.0f, Float::sum);
        assertArrayEquals(new float[] { 5.0f, 7.0f, 6.0f }, stream.toArray());
    }

    @Test
    @DisplayName("zip() with padding for three iterators")
    public void testZipThreeIteratorsWithPadding() {
        FloatIterator a = FloatIterator.of(1.0f);
        FloatIterator b = FloatIterator.of(4.0f, 5.0f);
        FloatIterator c = FloatIterator.of(7.0f, 8.0f, 9.0f);
        FloatStream stream = FloatStream.zip(a, b, c, 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z);
        assertArrayEquals(new float[] { 12.0f, 13.0f, 9.0f }, stream.toArray());
    }

    @Test
    @DisplayName("zip() with padding for two streams")
    public void testZipTwoStreamsWithPadding() {
        FloatStream a = FloatStream.of(1.0f, 2.0f);
        FloatStream b = FloatStream.of(4.0f, 5.0f, 6.0f);
        FloatStream stream = FloatStream.zip(a, b, 0.0f, 0.0f, Float::sum);
        assertArrayEquals(new float[] { 5.0f, 7.0f, 6.0f }, stream.toArray());
    }

    @Test
    @DisplayName("zip() with padding for three streams")
    public void testZipThreeStreamsWithPadding() {
        FloatStream a = FloatStream.of(1.0f);
        FloatStream b = FloatStream.of(4.0f, 5.0f);
        FloatStream c = FloatStream.of(7.0f, 8.0f, 9.0f);
        FloatStream stream = FloatStream.zip(a, b, c, 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z);
        assertArrayEquals(new float[] { 12.0f, 13.0f, 9.0f }, stream.toArray());
    }

    // Additional coverage improvement tests - Collection-based factory methods

    @Test
    @DisplayName("zip() collection of streams with function")
    public void testZipCollectionStreamsAdditional() {
        Collection<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(10.0f, 20.0f), FloatStream.of(100.0f, 200.0f));
        FloatStream result = FloatStream.zip(streams, floats -> {
            float sum = 0.0f;
            for (Float f : floats)
                sum += f;
            return sum;
        });
        assertArrayEquals(new float[] { 111.0f, 222.0f }, result.toArray());
    }

    @Test
    @DisplayName("zip() collection with defaults additional")
    public void testZipCollectionDefaultsAdditional() {
        Collection<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f, 3.0f), FloatStream.of(10.0f, 20.0f));
        float[] defaults = { 0.0f, 100.0f };
        FloatStream result = FloatStream.zip(streams, defaults, floats -> {
            float sum = 0.0f;
            for (Float f : floats)
                sum += f;
            return sum;
        });
        assertEquals(3, result.count());
    }

    @Test
    @DisplayName("zip() three arrays with defaults additional")
    public void testZipThreeArraysDefaultsAdditional() {
        float[] a1 = { 1.0f, 2.0f };
        float[] a2 = { 10.0f };
        float[] a3 = { 100.0f, 200.0f, 300.0f };
        FloatStream stream = FloatStream.zip(a1, a2, a3, 0.0f, 0.0f, 0.0f, (a, b, c) -> a + b + c);
        assertEquals(3, stream.count());
    }

    @Test
    @DisplayName("zip() three iterators with defaults additional")
    public void testZipThreeIteratorsDefaultsAdditional() {
        FloatIterator iter1 = FloatIterator.of(1.0f, 2.0f);
        FloatIterator iter2 = FloatIterator.of(10.0f);
        FloatIterator iter3 = FloatIterator.of(100.0f, 200.0f, 300.0f);
        FloatStream stream = FloatStream.zip(iter1, iter2, iter3, 0.0f, 0.0f, 0.0f, (a, b, c) -> a + b + c);
        assertEquals(3, stream.count());
    }

    @Test
    @DisplayName("zip() three streams with defaults additional")
    public void testZipThreeStreamsDefaultsAdditional() {
        FloatStream s1 = FloatStream.of(1.0f, 2.0f);
        FloatStream s2 = FloatStream.of(10.0f);
        FloatStream s3 = FloatStream.of(100.0f, 200.0f, 300.0f);
        FloatStream stream = FloatStream.zip(s1, s2, s3, 0.0f, 0.0f, 0.0f, (a, b, c) -> a + b + c);
        assertEquals(3, stream.count());
    }

    @Test
    public void testZip() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 4.0f, 5.0f, 6.0f };
        assertArrayEquals(new float[] { 5.0f, 7.0f, 9.0f }, FloatStream.zip(a, b, (f1, f2) -> f1 + f2).toArray(), DELTA);
    }

    @Test
    public void testZip3() {
        float[] a = { 1f, 2f, 3f };
        float[] b = { 10f, 20f, 30f };
        float[] c = { 100f, 200f, 300f };
        float[] expected = { 111f, 222f, 333f };
        assertArrayEquals(expected, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray(), DELTA);
    }

    @Test
    public void testStreamCreatedAfterZipArrays() {
        float[] a = { 1, 2, 3 };
        float[] b = { 4, 5, 6 };
        assertEquals(3, FloatStream.zip(a, b, (x, y) -> x + y).count());
        assertEquals(2, FloatStream.zip(a, b, (x, y) -> x + y).skip(1).count());
        assertArrayEquals(new float[] { 5, 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).toList());
        assertEquals(N.toList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).skip(1).toList());
        assertEquals(3, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).count());
        assertEquals(2, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 5, 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).toList());
        assertEquals(N.toList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZip3Arrays() {
        float[] a = { 1, 2, 3 };
        float[] b = { 4, 5, 6 };
        float[] c = { 7, 8, 9 };
        assertEquals(3, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).count());
        assertEquals(2, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).count());
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray(), 0.001f);
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toList());
        assertEquals(N.toList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).toList());
        assertEquals(3, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).count());
        assertEquals(2, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).toList());
        assertEquals(N.toList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipIterators() {
        FloatIterator a = FloatIterator.of(new float[] { 1, 2, 3 });
        FloatIterator b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(3, FloatStream.zip(a, b, (x, y) -> x + y).count());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(2, FloatStream.zip(a, b, (x, y) -> x + y).skip(1).count());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertArrayEquals(new float[] { 5, 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertArrayEquals(new float[] { 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.toList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).toList());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.toList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).skip(1).toList());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(3, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).count());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(2, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).count());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertArrayEquals(new float[] { 5, 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertArrayEquals(new float[] { 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.toList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).toList());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.toList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZip3Iterators() {
        FloatIterator a = FloatIterator.of(new float[] { 1, 2, 3 });
        FloatIterator b = FloatIterator.of(new float[] { 4, 5, 6 });
        FloatIterator c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(3, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).count());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(2, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).count());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(N.toList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toList());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(N.toList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).toList());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(3, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).count());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(2, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).count());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(N.toList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).toList());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(N.toList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipArraysWithDefaults() {
        float[] a = { 1, 2 };
        float[] b = { 4, 5, 6 };
        assertEquals(3, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).count());
        assertEquals(2, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).skip(1).count());
        assertArrayEquals(new float[] { 5, 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f, 7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).toList());
        assertEquals(N.toList(7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).skip(1).toList());
        assertEquals(3, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).count());
        assertEquals(2, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 5, 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f, 7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).toList());
        assertEquals(N.toList(7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZip3ArraysWithDefaults() {
        float[] a = { 1, 2 };
        float[] b = { 4, 5, 6 };
        float[] c = { 7 };
        assertEquals(3, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).count());
        assertEquals(2, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).count());
        assertArrayEquals(new float[] { 12, 7, 6 }, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 6 }, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(12f, 7f, 6f), FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).toList());
        assertEquals(N.toList(7f, 6f), FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).toList());
        assertEquals(3, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).count());
        assertEquals(2, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 12, 7, 6 }, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 6 }, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(12f, 7f, 6f), FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).toList());
        assertEquals(N.toList(7f, 6f), FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipIteratorsWithDefaults() {
        FloatIterator a = FloatIterator.of(new float[] { 1, 2 });
        FloatIterator b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(3, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).count());
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(2, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).skip(1).count());
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertArrayEquals(new float[] { 5, 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertArrayEquals(new float[] { 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.toList(5f, 7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).toList());
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.toList(7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).skip(1).toList());
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(3, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).count());
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(2, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).skip(1).count());
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertArrayEquals(new float[] { 5, 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertArrayEquals(new float[] { 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).skip(1).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.toList(5f, 7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).toList());
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.toList(7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).skip(1).toList());
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
    public void test_zip_02() {
        float[] a = { 1.0f };
        float[] b = { 4.0f, 5.0f };
        float[] c = { 7.0f, 8.0f, 9.0f };
        float[] result = FloatStream.zip(a, b, c, 100, 200, 300, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new float[] { 12.0f, 113.0f, 309.0f }, result);
    }

    @Test
    public void testZipTwoArrays_Test() {
        float[] result = FloatStream.zip(new float[] { 1.0f, 2.0f }, new float[] { 10.0f, 20.0f }, Float::sum).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f }, result, DELTA);
    }

    @Test
    public void testZipThreeArrays_Test() {
        float[] result = FloatStream.zip(new float[] { 1.0f }, new float[] { 10.0f }, new float[] { 100.0f }, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new float[] { 111.0f }, result, DELTA);
    }

    @Test
    public void testZipTwoIterators_Test() {
        float[] result = FloatStream.zip(FloatIterator.of(new float[] { 1.0f, 2.0f }), FloatIterator.of(new float[] { 10.0f, 20.0f }), Float::sum).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f }, result, DELTA);
    }

    @Test
    public void testZipThreeIterators_Test() {
        float[] result = FloatStream
                .zip(FloatIterator.of(new float[] { 1.0f }), FloatIterator.of(new float[] { 10.0f }), FloatIterator.of(new float[] { 100.0f }),
                        (x, y, z) -> x + y + z)
                .toArray();
        assertArrayEquals(new float[] { 111.0f }, result, DELTA);
    }

    @Test
    public void testZipTwoStreams_Test() {
        float[] result = FloatStream.zip(FloatStream.of(1.0f, 2.0f), FloatStream.of(10.0f, 20.0f), Float::sum).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f }, result, DELTA);
    }

    @Test
    public void testZipThreeStreams_Test() {
        float[] result = FloatStream.zip(FloatStream.of(1.0f), FloatStream.of(10.0f), FloatStream.of(100.0f), (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new float[] { 111.0f }, result, DELTA);
    }

    @Test
    public void testZipCollectionOfStreams_Test() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(10.0f, 20.0f));
        float[] result = FloatStream.zip(streams, values -> values[0] + values[1]).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f }, result, DELTA);
    }

    @Test
    public void testZipTwoArraysWithPadding_Test() {
        float[] result = FloatStream.zip(new float[] { 1.0f, 2.0f, 3.0f }, new float[] { 10.0f, 20.0f }, 0.0f, 0.0f, Float::sum).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testZipThreeArraysWithPadding_Test() {
        float[] result = FloatStream
                .zip(new float[] { 1.0f }, new float[] { 10.0f, 20.0f }, new float[] { 100.0f, 200.0f, 300.0f }, 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z)
                .toArray();
        assertArrayEquals(new float[] { 111.0f, 220.0f, 300.0f }, result, DELTA);
    }

    @Test
    public void testZipTwoIteratorsWithPadding_Test() {
        float[] result = FloatStream
                .zip(FloatIterator.of(new float[] { 1.0f, 2.0f, 3.0f }), FloatIterator.of(new float[] { 10.0f, 20.0f }), 0.0f, 0.0f, Float::sum)
                .toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testZipThreeIteratorsWithPadding_Test() {
        float[] result = FloatStream
                .zip(FloatIterator.of(new float[] { 1.0f }), FloatIterator.of(new float[] { 10.0f, 20.0f }),
                        FloatIterator.of(new float[] { 100.0f, 200.0f, 300.0f }), 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z)
                .toArray();
        assertArrayEquals(new float[] { 111.0f, 220.0f, 300.0f }, result, DELTA);
    }

    @Test
    public void testZipTwoStreamsWithPadding_Test() {
        float[] result = FloatStream.zip(FloatStream.of(1.0f, 2.0f, 3.0f), FloatStream.of(10.0f, 20.0f), 0.0f, 0.0f, Float::sum).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testZipThreeStreamsWithPadding_Test() {
        float[] result = FloatStream
                .zip(FloatStream.of(1.0f), FloatStream.of(10.0f, 20.0f), FloatStream.of(100.0f, 200.0f, 300.0f), 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z)
                .toArray();
        assertArrayEquals(new float[] { 111.0f, 220.0f, 300.0f }, result, DELTA);
    }

    @Test
    public void testZipCollectionWithPadding_Test() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f, 3.0f), FloatStream.of(10.0f, 20.0f));
        float[] result = FloatStream.zip(streams, new float[] { 0.0f, 0.0f }, values -> values[0] + values[1]).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f, 3.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zip() with padding for collection of streams")
    public void testZipCollectionWithPadding() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(3.0f));
        float[] valuesForNone = { 0.0f, 100.0f };
        FloatStream stream = FloatStream.zip(streams, valuesForNone, floats -> floats[0] + floats[1]);
        assertArrayEquals(new float[] { 4.0f, 102.0f }, stream.toArray());
    }

    @Test
    @DisplayName("merge() two arrays")
    public void testMergeTwoArrays() {
        float[] a = { 1.0f, 3.0f, 5.0f };
        float[] b = { 2.0f, 4.0f, 6.0f };
        FloatStream stream = FloatStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
    }

    @Test
    @DisplayName("merge() three arrays")
    public void testMergeThreeArrays() {
        float[] a = { 1.0f, 4.0f };
        float[] b = { 2.0f, 5.0f };
        float[] c = { 3.0f, 6.0f };
        FloatStream stream = FloatStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
    }

    @Test
    @DisplayName("merge() two iterators")
    public void testMergeTwoIterators() {
        FloatIterator a = FloatIterator.of(1.0f, 3.0f, 5.0f);
        FloatIterator b = FloatIterator.of(2.0f, 4.0f, 6.0f);
        FloatStream stream = FloatStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
    }

    @Test
    @DisplayName("merge() three iterators")
    public void testMergeThreeIterators() {
        FloatIterator a = FloatIterator.of(1.0f, 4.0f);
        FloatIterator b = FloatIterator.of(2.0f, 5.0f);
        FloatIterator c = FloatIterator.of(3.0f, 6.0f);
        FloatStream stream = FloatStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
    }

    @Test
    @DisplayName("merge() two streams")
    public void testMergeTwoStreams() {
        FloatStream a = FloatStream.of(1.0f, 3.0f, 5.0f);
        FloatStream b = FloatStream.of(2.0f, 4.0f, 6.0f);
        FloatStream stream = FloatStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
    }

    @Test
    @DisplayName("merge() three streams")
    public void testMergeThreeStreams() {
        FloatStream a = FloatStream.of(1.0f, 4.0f);
        FloatStream b = FloatStream.of(2.0f, 5.0f);
        FloatStream c = FloatStream.of(3.0f, 6.0f);
        FloatStream stream = FloatStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
    }

    @Test
    @DisplayName("merge() collection of streams")
    public void testMergeCollectionOfStreams() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 3.0f), FloatStream.of(2.0f, 4.0f));
        FloatStream stream = FloatStream.merge(streams, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    @DisplayName("merge() collection of streams additional")
    public void testMergeCollectionAdditional() {
        Collection<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 7.0f), FloatStream.of(3.0f, 8.0f), FloatStream.of(5.0f, 9.0f));
        FloatStream result = FloatStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, result.count());
    }

    @Test
    @DisplayName("merge() three arrays additional")
    public void testMergeThreeArraysAdditional() {
        float[] a1 = { 1.0f, 10.0f };
        float[] a2 = { 4.0f, 11.0f };
        float[] a3 = { 7.0f, 12.0f };
        FloatStream stream = FloatStream.merge(a1, a2, a3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    @DisplayName("merge() three streams additional")
    public void testMergeThreeStreamsAdditional() {
        FloatStream s1 = FloatStream.of(1.0f, 15.0f);
        FloatStream s2 = FloatStream.of(8.0f, 16.0f);
        FloatStream s3 = FloatStream.of(12.0f, 17.0f);
        FloatStream stream = FloatStream.merge(s1, s2, s3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMerge() {
        float[] a = { 1.1f, 3.3f, 5.5f };
        float[] b = { 2.2f, 4.4f, 6.6f };
        float[] expected = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f, 6.6f };
        assertArrayEquals(expected, FloatStream.merge(a, b, (f1, f2) -> f1 <= f2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), DELTA);
    }

    @Test
    public void testStreamCreatedAfterMergeArrays() {
        float[] a = { 1, 3, 5 };
        float[] b = { 2, 4, 6 };
        assertEquals(6, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(5, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        assertEquals(6, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        assertEquals(5, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMerge3Arrays() {
        float[] a = { 1, 4, 7 };
        float[] b = { 2, 5, 8 };
        float[] c = { 3, 6, 9 };
        assertEquals(9, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(8, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        assertEquals(9, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        assertEquals(8, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeIterators() {
        FloatIterator a = FloatIterator.of(new float[] { 1, 3, 5 });
        FloatIterator b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(6, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(5, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(6, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(5, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void test_merge() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 4.0f, 5.0f, 6.0f };
        float[] c = { 7.0f, 8.0f, 9.0f };
        float[] result = FloatStream.merge(N.toList(FloatStream.of(a)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);

        result = FloatStream.merge(N.toList(FloatStream.of(a), FloatStream.of(b)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result);

        result = FloatStream
                .merge(N.toList(FloatStream.of(a), FloatStream.of(b), FloatStream.of(c)),
                        (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f }, result);

    }

    @Test
    public void testMergeTwoArrays_Test() {
        float[] result = FloatStream
                .merge(new float[] { 1.0f, 3.0f, 5.0f }, new float[] { 2.0f, 4.0f, 6.0f }, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result, DELTA);
    }

    @Test
    public void testMergeThreeArrays_Test() {
        float[] result = FloatStream
                .merge(new float[] { 1.0f, 4.0f }, new float[] { 2.0f, 5.0f }, new float[] { 3.0f, 6.0f },
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result, DELTA);
    }

    @Test
    public void testMergeTwoIterators_Test() {
        float[] result = FloatStream
                .merge(FloatIterator.of(new float[] { 1.0f, 3.0f }), FloatIterator.of(new float[] { 2.0f, 4.0f }),
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    public void testMergeThreeIterators_Test() {
        float[] result = FloatStream
                .merge(FloatIterator.of(new float[] { 1.0f }), FloatIterator.of(new float[] { 2.0f }), FloatIterator.of(new float[] { 3.0f }),
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testMergeTwoStreams_Test() {
        float[] result = FloatStream
                .merge(FloatStream.of(1.0f, 3.0f), FloatStream.of(2.0f, 4.0f), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    public void testMergeThreeStreams_Test() {
        float[] result = FloatStream
                .merge(FloatStream.of(1.0f), FloatStream.of(2.0f), FloatStream.of(3.0f), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testMergeCollectionOfStreams_Test() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 3.0f), FloatStream.of(2.0f, 4.0f));
        float[] result = FloatStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    public void testStreamIsClosedAfterTerminalOp() {
        FloatStream stream = FloatStream.of(1.0f, 2.0f);
        stream.count();
        assertThrows(IllegalStateException.class, stream::count);
    }

    @Test
    @DisplayName("skipUntil() should skip elements until predicate is true")
    public void testSkipUntil() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f }, stream.skipUntil(x -> x >= 3.0f).toArray());
    }

    @Test
    @DisplayName("distinct() should remove duplicates")
    public void testDistinct() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 2.0f, 3.0f, 1.0f);
        float[] result = stream.distinct().toArray();
        Arrays.sort(result);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
    }

    @Test
    @DisplayName("limit() should limit stream size")
    public void testLimit() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.limit(3).toArray());
    }

    @Test
    @DisplayName("skip() should skip elements")
    public void testSkip() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f }, stream.skip(2).toArray());
    }

    @Test
    @DisplayName("skip() with action on skipped items")
    public void testSkipWithAction() {
        List<Float> skipped = new ArrayList<>();
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f }, stream.skip(2, skipped::add).toArray());
        assertEquals(Arrays.asList(1.0f, 2.0f), skipped);
    }

    @Test
    @DisplayName("step() should take every nth element")
    public void testStep() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 5.0f }, stream.step(2).toArray());
    }

    @Test
    @DisplayName("indexed() should pair elements with indices")
    public void testIndexed() {
        FloatStream stream = createFloatStream(10.0f, 20.0f, 30.0f);
        List<IndexedFloat> result = stream.indexed().toList();
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).longIndex());
        assertEquals(10.0f, result.get(0).value(), 0.001);
    }

    @Test
    @DisplayName("cycled() should repeat stream indefinitely")
    public void testCycled() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 1.0f, 2.0f, 1.0f, 2.0f }, stream.cycled().limit(6).toArray());
    }

    @Test
    @DisplayName("cycled() with rounds should repeat specific times")
    public void testCycledWithRounds() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 1.0f, 2.0f, 1.0f, 2.0f }, stream.cycled(3).toArray());
    }

    @Test
    @DisplayName("sorted() should sort elements")
    public void testSorted() {
        FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.sorted().toArray());
    }

    @Test
    @DisplayName("reverseSorted() should sort in reverse")
    public void testReverseSorted() {
        FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f);
        assertArrayEquals(new float[] { 4.0f, 3.0f, 2.0f, 1.0f }, stream.reverseSorted().toArray());
    }

    @Test
    @DisplayName("reversed() should reverse order")
    public void testReversed() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        assertArrayEquals(new float[] { 4.0f, 3.0f, 2.0f, 1.0f }, stream.reversed().toArray());
    }

    @Test
    @DisplayName("shuffled() should shuffle elements")
    public void testShuffled() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        float[] result = stream.shuffled().toArray();
        assertEquals(4, result.length);
        Arrays.sort(result);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result);
    }

    @Test
    @DisplayName("rotated() should rotate elements")
    public void testRotated() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f, 1.0f, 2.0f }, stream.rotated(2).toArray());
    }

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
    @DisplayName("difference() should return elements not in other")
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

    @Test
    @DisplayName("onEach() should perform action on each element")
    public void testOnEach() {
        List<Float> collected = new ArrayList<>();
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.peek(collected::add).toArray());
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), collected);
    }

    @Test
    @DisplayName("count() should return element count")
    public void testCount() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertEquals(3, stream.count());
    }

    @Test
    @DisplayName("percentiles() should return percentile map")
    public void testPercentiles() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        Optional<Map<Percentage, Float>> result = stream.percentiles();
        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("first() should return first element")
    public void testFirst() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        OptionalFloat first = stream.first();
        assertTrue(first.isPresent());
        assertEquals(1.0f, first.get(), 0.001);
    }

    @Test
    @DisplayName("last() should return last element")
    public void testLast() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        OptionalFloat last = stream.last();
        assertTrue(last.isPresent());
        assertEquals(3.0f, last.get(), 0.001);
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
    @DisplayName("onlyOne() with multiple elements should throw")
    public void testOnlyOneMultiple() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertThrows(Exception.class, stream::onlyOne);
    }

    @Test
    @DisplayName("elementAt() should return element at index")
    public void testElementAt() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        OptionalFloat element = stream.elementAt(2);
        assertTrue(element.isPresent());
        assertEquals(3.0f, element.get(), 0.001);
    }

    @Test
    @DisplayName("toArray() should return array")
    public void testToArray() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
    }

    @Test
    @DisplayName("toList() should return List")
    public void testToList() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        List<Float> result = stream.toList();
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), result);
    }

    @Test
    @DisplayName("toSet() should return Set")
    public void testToSet() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 2.0f, 3.0f);
        Set<Float> result = stream.toSet();
        assertEquals(new HashSet<>(Arrays.asList(1.0f, 2.0f, 3.0f)), result);
    }

    @Test
    @DisplayName("toMultiset() should return multiset")
    public void testToMultiset() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 1.0f, 3.0f, 2.0f, 1.0f);
        Multiset<Float> result = stream.toMultiset();
        assertEquals(3, result.count(1.0f));
        assertEquals(2, result.count(2.0f));
        assertEquals(1, result.count(3.0f));
    }

    @Test
    @DisplayName("join() with delimiter")
    public void testJoin() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertEquals("1.0, 2.0, 3.0", stream.join(", "));
    }

    @Test
    @DisplayName("join() with prefix and suffix")
    public void testJoinWithPrefixSuffix() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertEquals("[1.0, 2.0, 3.0]", stream.join(", ", "[", "]"));
    }

    @Test
    @DisplayName("isParallel() should return false for sequential")
    public void testIsParallelFalse() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertFalse(stream.isParallel());
    }

    @Test
    @DisplayName("parallel() should return parallel stream")
    public void testParallel() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        FloatStream parallel = stream.parallel();
        assertTrue(parallel.isParallel());
    }

    @Test
    @DisplayName("parallel() with max thread number")
    public void testParallelWithMaxThreads() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        FloatStream parallel = stream.parallel(2);
        assertTrue(parallel.isParallel());
    }

    @Test
    @DisplayName("parallel() with executor")
    public void testParallelWithExecutor() {
        Executor executor = Executors.newFixedThreadPool(2);
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        FloatStream parallel = stream.parallel(executor);
        assertTrue(parallel.isParallel());
    }

    @Test
    @DisplayName("sequential() should return sequential stream")
    public void testSequential() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f).parallel();
        FloatStream sequential = stream.sequential();
        assertFalse(sequential.isParallel());
    }

    @Test
    @DisplayName("onClose() should register close handler")
    public void testOnClose() {
        List<String> closeActions = new ArrayList<>();
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f).onClose(() -> closeActions.add("closed"));
        stream.toArray();
        assertEquals(Arrays.asList("closed"), closeActions);
    }

    @Test
    @DisplayName("close() should close stream")
    public void testClose() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        stream.close();
    }

    @Test
    @DisplayName("closed stream should throw IllegalStateException")
    public void testClosedStream() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        stream.toArray();
        assertThrows(IllegalStateException.class, () -> stream.count());
    }

    @Test
    @DisplayName("iterator() should return iterator")
    public void testIterator() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        FloatIterator iter = stream.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1.0f, iter.nextFloat(), 0.001);
    }

    @Test
    @DisplayName("single element stream")
    public void testSingleElement() {
        FloatStream single = createFloatStream(5.0f);
        assertEquals(1, single.count());

        single = createFloatStream(5.0f);
        assertEquals(5.0f, single.first().get(), 0.001);
    }

    @Test
    @DisplayName("special float values")
    public void testSpecialValues() {
        FloatStream stream = createFloatStream(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f);
        float[] result = stream.toArray();
        assertEquals(4, result.length);
        assertTrue(Float.isNaN(result[0]));
        assertTrue(Float.isInfinite(result[1]));
        assertTrue(Float.isInfinite(result[2]));
    }

    @Test
    @DisplayName("parallel() with max thread number should configure parallelism")
    public void testParallelWithThreads() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
        FloatStream parallel = stream.parallel(2);
        assertTrue(parallel.isParallel());
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
    public void testToImmutableList() {
        com.landawn.abacus.util.ImmutableList<Float> result = createFloatStream(1.0f, 2.0f, 3.0f).toImmutableList();
        assertEquals(3, result.size());
        assertEquals(1.0f, result.get(0), DELTA);
        assertThrows(UnsupportedOperationException.class, () -> result.add(4.0f));
    }

    @Test
    public void testToImmutableSet() {
        com.landawn.abacus.util.ImmutableSet<Float> result = createFloatStream(1.0f, 2.0f, 3.0f, 2.0f).toImmutableSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1.0f));
        assertThrows(UnsupportedOperationException.class, () -> result.add(4.0f));
    }

    @Test
    public void testPrintln() {
        // Just verify println doesn't throw
        assertDoesNotThrow(() -> createFloatStream(1.0f, 2.0f, 3.0f).println());
    }

    @Test
    public void testApplyIfNotEmpty() {
        Optional<float[]> result = createFloatStream(1.0f, 2.0f, 3.0f).applyIfNotEmpty(s -> s.toArray());
        assertTrue(result.isPresent());
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result.get(), DELTA);
    }

    @Test
    public void testLimitWithOffset() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).limit(2, 2).toArray();
        assertArrayEquals(new float[] { 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    public void testLimitWithOffset_ZeroOffset() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).limit(0, 3).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testLimitWithOffset_OffsetBeyondSize() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).limit(10, 5).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testToMultisetWithSupplier() {
        Multiset<Float> result = createFloatStream(1.0f, 2.0f, 2.0f, 3.0f, 3.0f, 3.0f).toMultiset(Suppliers.ofMultiset());
        assertEquals(1, result.get(1.0f));
        assertEquals(2, result.get(2.0f));
        assertEquals(3, result.get(3.0f));
    }

    @Test
    public void testJoinWithJoiner() {
        Joiner joiner = Joiner.with(", ", "[", "]");
        createFloatStream(1.0f, 2.0f, 3.0f).joinTo(joiner);
        String result = joiner.toString();
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("1.0"));
    }

    @Test
    public void testElementAt_OutOfRange() {
        OptionalFloat result = createFloatStream(10.0f, 20.0f).elementAt(5);
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_MultipleElements() {
        assertThrows(Exception.class, () -> createFloatStream(1.0f, 2.0f).onlyOne());
    }

    @Test
    public void testStep_StepOne() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).step(1).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testCycledWithRounds_Zero() {
        float[] result = createFloatStream(1.0f, 2.0f).cycled(0).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testReversed_SingleElement() {
        float[] result = createFloatStream(5.0f).reversed().toArray();
        assertArrayEquals(new float[] { 5.0f }, result, DELTA);
    }

    @Test
    public void testRotated_Zero() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).rotated(0).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testRotated_Negative() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).rotated(-1).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testShuffledWithRandom() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).shuffled(new Random(42)).toArray();
        assertEquals(5, result.length);
    }

    @Test
    public void testIntersection_Empty() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).intersection(java.util.Collections.emptyList()).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testDifference_Empty() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).difference(java.util.Collections.emptyList()).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testSymmetricDifference_Empty() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).symmetricDifference(java.util.Collections.emptyList()).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testDistinct_AllSame() {
        float[] result = createFloatStream(5.0f, 5.0f, 5.0f).distinct().toArray();
        assertArrayEquals(new float[] { 5.0f }, result, DELTA);
    }

    @Test
    public void testSorted_AlreadySorted() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).sorted().toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testThrowIfEmpty_NotEmpty() {
        assertDoesNotThrow(() -> createFloatStream(1.0f).throwIfEmpty(() -> new RuntimeException("empty")).toArray());
    }

    @Test
    public void testIfEmpty_NotEmpty() {
        AtomicInteger counter = new AtomicInteger(0);
        createFloatStream(1.0f, 2.0f).ifEmpty(counter::incrementAndGet).toArray();
        assertEquals(0, counter.get());
    }

    @Test
    public void testJoin_WithPrefixSuffix() {
        String result = createFloatStream(1.0f, 2.0f, 3.0f).join(", ", "[", "]");
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
    }

    @Test
    public void testOnClose_Multiple() {
        AtomicInteger counter = new AtomicInteger(0);
        FloatStream stream = createFloatStream(1.0f, 2.0f).onClose(counter::incrementAndGet).onClose(counter::incrementAndGet);
        stream.close();
        assertEquals(2, counter.get());
    }

    @Test
    public void testIterator_HasNextAndNext() {
        FloatIterator iter = createFloatStream(1.0f, 2.0f, 3.0f).iterator();
        assertTrue(iter.hasNext());
        assertEquals(1.0f, iter.nextFloat(), DELTA);
        assertTrue(iter.hasNext());
        assertEquals(2.0f, iter.nextFloat(), DELTA);
        assertTrue(iter.hasNext());
        assertEquals(3.0f, iter.nextFloat(), DELTA);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testToCollection_Supplier() {
        java.util.LinkedList<Float> result = createFloatStream(1.0f, 2.0f, 3.0f).toCollection(java.util.LinkedList::new);
        assertEquals(3, result.size());
        assertEquals(Float.valueOf(1.0f), result.get(0));
        assertEquals(Float.valueOf(3.0f), result.get(2));
    }

}
