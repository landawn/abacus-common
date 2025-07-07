package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.ShortNFunction;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings;


public class ShortStream101Test extends TestBase {

    // This method needs to be implemented by a concrete test class to provide a ShortStream instance.
    // For example, in ArrayShortStreamTest, it would return new ArrayShortStream(a);
    // In IteratorShortStreamTest, it would return new IteratorShortStream(ShortIterator.of(a));
    protected ShortStream createShortStream(short... a) {
        return ShortStream.of(a);
    }

    protected ShortStream createShortStream(short[] a, int fromIndex, int toIndex) {
        return ShortStream.of(a, fromIndex, toIndex);
    }

    protected ShortStream createShortStream(Short[] a) {
        return ShortStream.of(a);
    }

    protected ShortStream createShortStream(Short[] a, int fromIndex, int toIndex) {
        return ShortStream.of(a, fromIndex, toIndex);
    }

    protected ShortStream createShortStream(Collection<Short> coll) {
        return ShortStream.of(coll.toArray(new Short[coll.size()]));
    }

    protected ShortStream createShortStream(ShortIterator iter) {
        return iter == null ? ShortStream.empty() : ShortStream.of(iter.toArray());
    }

    protected ShortStream createShortStream(ShortBuffer buff) {
        return ShortStream.of(buff);
    }
    @Test
    public void testOfShortBuffer() {
        java.nio.ShortBuffer buffer = java.nio.ShortBuffer.wrap(new short[] { 1, 2, 3, 4, 5 });
        buffer.position(1);
        buffer.limit(4);

        ShortStream stream = createShortStream(buffer);
        assertArrayEquals(new short[] { 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testOfShortArray() {
        Short[] array = { 1, 2, 3, 4 };
        ShortStream stream = createShortStream(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testOfShortArrayWithRange() {
        Short[] array = { 1, 2, 3, 4, 5 };
        ShortStream stream = createShortStream(array, 1, 4);
        assertArrayEquals(new short[] { 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testIterateWithPredicate() {
        ShortStream stream = ShortStream.iterate((short) 1, n -> n < 10, n -> (short) (n * 2));
        assertArrayEquals(new short[] { 1, 2, 4, 8 }, stream.toArray());
    }

    @Test
    public void testIterateWithBooleanSupplierAndInitialValue() {
        final int[] count = { 0 };
        ShortStream stream = ShortStream.iterate((short) 5, () -> count[0]++ < 3, n -> (short) (n + 1));
        assertArrayEquals(new short[] { 5, 6, 7 }, stream.toArray());
    }

    @Test
    public void testConcatIterators() {
        ShortList list1 = ShortList.of((short) 1, (short) 2);
        ShortList list2 = ShortList.of((short) 3, (short) 4);

        ShortStream stream = ShortStream.concat(list1.iterator(), list2.iterator());
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcatArrayCollection() {
        List<short[]> arrays = Arrays.asList(new short[] { 1, 2 }, new short[] { 3, 4 }, new short[] { 5 });

        ShortStream stream = ShortStream.concat(arrays);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testConcatStreamCollection() {
        List<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 2), createShortStream((short) 3, (short) 4), createShortStream((short) 5));

        ShortStream stream = ShortStream.concat(streams);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testConcatIterators2() {
        List<ShortIterator> iterators = Arrays.asList(ShortList.of((short) 1, (short) 2).iterator(), ShortList.of((short) 3, (short) 4).iterator());

        ShortStream stream = ShortStream.concatIterators(iterators);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    // Test zip with 3 arrays/iterators/streams

    @Test
    public void testZipThreeArrays() {
        short[] a = { 1, 2 };
        short[] b = { 10, 20 };
        short[] c = { 100, 200 };

        ShortStream stream = ShortStream.zip(a, b, c, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 222 }, stream.toArray());
    }

    @Test
    public void testZipThreeIterators() {
        ShortIterator a = ShortIterator.of((short) 1, (short) 2);
        ShortIterator b = ShortIterator.of((short) 10, (short) 20);
        ShortIterator c = ShortIterator.of((short) 100, (short) 200);

        ShortStream stream = ShortStream.zip(a, b, c, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 222 }, stream.toArray());
    }

    @Test
    public void testZipThreeStreams() {
        ShortStream a = createShortStream((short) 1, (short) 2);
        ShortStream b = createShortStream((short) 10, (short) 20);
        ShortStream c = createShortStream((short) 100, (short) 200);

        ShortStream stream = ShortStream.zip(a, b, c, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 222 }, stream.toArray());
    }

    @Test
    public void testZipThreeArraysWithDefaults() {
        short[] a = { 1, 2, 3 };
        short[] b = { 10, 20 };
        short[] c = { 100 };

        ShortStream stream = ShortStream.zip(a, b, c, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 22, 3 }, stream.toArray());
    }

    @Test
    public void testZipThreeIteratorsWithDefaults() {
        ShortIterator a = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortIterator b = ShortIterator.of((short) 10, (short) 20);
        ShortIterator c = ShortIterator.of((short) 100);

        ShortStream stream = ShortStream.zip(a, b, c, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 22, 3 }, stream.toArray());
    }

    @Test
    public void testZipThreeStreamsWithDefaults() {
        ShortStream a = createShortStream((short) 1, (short) 2, (short) 3);
        ShortStream b = createShortStream((short) 10, (short) 20);
        ShortStream c = createShortStream((short) 100);

        ShortStream stream = ShortStream.zip(a, b, c, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 22, 3 }, stream.toArray());
    }

    @Test
    public void testZipMultipleStreams() {
        List<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 2), createShortStream((short) 10, (short) 20),
                createShortStream((short) 100, (short) 200));

        ShortNFunction<Short> zipFunction = args -> {
            short sum = 0;
            for (short s : args) {
                sum += s;
            }
            return sum;
        };

        ShortStream result = ShortStream.zip(streams, zipFunction);
        assertArrayEquals(new short[] { 111, 222 }, result.toArray());
    }

    @Test
    public void testZipMultipleStreamsWithDefaults() {
        List<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 2, (short) 3), createShortStream((short) 10, (short) 20),
                createShortStream((short) 100));

        short[] defaults = { 0, 0, 0 };
        ShortNFunction<Short> zipFunction = args -> {
            short sum = 0;
            for (short s : args) {
                sum += s;
            }
            return sum;
        };

        ShortStream result = ShortStream.zip(streams, defaults, zipFunction);
        assertArrayEquals(new short[] { 111, 22, 3 }, result.toArray());
    }

    // Test merge with 3 arrays/iterators/streams

    @Test
    public void testMergeThreeArrays() {
        short[] a = { 1, 5 };
        short[] b = { 2, 6 };
        short[] c = { 3, 7 };

        ShortStream stream = ShortStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 5, 6, 7 }, stream.toArray());
    }

    @Test
    public void testMergeThreeIterators() {
        ShortIterator a = ShortIterator.of((short) 1, (short) 5);
        ShortIterator b = ShortIterator.of((short) 2, (short) 6);
        ShortIterator c = ShortIterator.of((short) 3, (short) 7);

        ShortStream stream = ShortStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 5, 6, 7 }, stream.toArray());
    }

    @Test
    public void testMergeThreeStreams() {
        ShortStream a = createShortStream((short) 1, (short) 5);
        ShortStream b = createShortStream((short) 2, (short) 6);
        ShortStream c = createShortStream((short) 3, (short) 7);

        ShortStream stream = ShortStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 5, 6, 7 }, stream.toArray());
    }

    @Test
    public void testMergeMultipleStreams() {
        List<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 4), createShortStream((short) 2, (short) 5),
                createShortStream((short) 3, (short) 6));

        ShortStream result = ShortStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, result.toArray());
    }

    // Test additional instance methods

    @Test
    public void testFlatMapToObj() {
        Stream<String> stream = createShortStream((short) 1, (short) 2).flatMapToObj(n -> Stream.of("A" + n, "B" + n));
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2" }, stream.toArray());
    }

    @Test
    public void testFlatmapToObj() {
        Stream<String> stream = createShortStream((short) 1, (short) 2).flatmapToObj(n -> Arrays.asList("A" + n, "B" + n));
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2" }, stream.toArray());
    }

    @Test
    public void testFlattMapToObj() {
        Stream<String> stream = createShortStream((short) 1, (short) 2).flattMapToObj(n -> new String[] { "A" + n, "B" + n });
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2" }, stream.toArray());
    }

    @Test
    public void testFlatMapToInt() {
        IntStream stream = createShortStream((short) 1, (short) 2).flatMapToInt(n -> IntStream.of(n * 10, n * 20));
        assertArrayEquals(new int[] { 10, 20, 20, 40 }, stream.toArray());
    }

    @Test
    public void testFlatmap() {
        ShortStream stream = createShortStream((short) 1, (short) 2).flatmap(n -> new short[] { n, (short) (n + 10) });
        assertArrayEquals(new short[] { 1, 11, 2, 12 }, stream.toArray());
    }

    @Test
    public void testRangeMapToObj() {
        Stream<String> stream = createShortStream((short) 1, (short) 2, (short) 5, (short) 6).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> a + "-" + b);
        assertArrayEquals(new String[] { "1-2", "5-6" }, stream.toArray());
    }

    @Test
    public void testCollapseWithTriPredicate() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 7, (short) 8, (short) 9)
                .collapse((first, prev, curr) -> curr - first <= 2, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 6, 24 }, stream.toArray());
    }

    @Test
    public void testScanWithInitAndIncluded() {
        // Test with init included
        ShortStream stream1 = createShortStream((short) 1, (short) 2, (short) 3).scan((short) 10, true, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 10, 11, 13, 16 }, stream1.toArray());

        // Test with init not included
        ShortStream stream2 = createShortStream((short) 1, (short) 2, (short) 3).scan((short) 10, false, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 11, 13, 16 }, stream2.toArray());
    }

    @Test
    public void testPrependStream() {
        ShortStream stream = createShortStream((short) 3, (short) 4).prepend(ShortStream.of((short) 1, (short) 2));
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testPrependOptional() {
        ShortStream stream1 = createShortStream((short) 2, (short) 3).prepend(OptionalShort.of((short) 1));
        assertArrayEquals(new short[] { 1, 2, 3 }, stream1.toArray());

        ShortStream stream2 = createShortStream((short) 2, (short) 3).prepend(OptionalShort.empty());
        assertArrayEquals(new short[] { 2, 3 }, stream2.toArray());
    }

    @Test
    public void testAppendStream() {
        ShortStream stream = createShortStream((short) 1, (short) 2).append(ShortStream.of((short) 3, (short) 4));
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testAppendOptional() {
        ShortStream stream1 = createShortStream((short) 1, (short) 2).append(OptionalShort.of((short) 3));
        assertArrayEquals(new short[] { 1, 2, 3 }, stream1.toArray());

        ShortStream stream2 = createShortStream((short) 1, (short) 2).append(OptionalShort.empty());
        assertArrayEquals(new short[] { 1, 2 }, stream2.toArray());
    }

    @Test
    public void testAppendIfEmpty() {
        // Test with non-empty stream
        ShortStream stream1 = createShortStream((short) 1, (short) 2).appendIfEmpty((short) 10, (short) 20);
        assertArrayEquals(new short[] { 1, 2 }, stream1.toArray());

        // Test with empty stream
        ShortStream stream2 = ShortStream.empty().appendIfEmpty((short) 10, (short) 20);
        assertArrayEquals(new short[] { 10, 20 }, stream2.toArray());
    }

    @Test
    public void testAppendIfEmptySupplier() {
        // Test with empty stream
        ShortStream stream = ShortStream.empty().appendIfEmpty(() -> createShortStream((short) 10, (short) 20));
        assertArrayEquals(new short[] { 10, 20 }, stream.toArray());
    }

    @Test
    public void testDefaultIfEmpty() {
        ShortStream stream = ShortStream.empty().defaultIfEmpty(() -> createShortStream((short) 42));
        assertArrayEquals(new short[] { 42 }, stream.toArray());
    }

    @Test
    public void testTopWithComparator() {
        ShortStream stream = createShortStream((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).top(3, Comparator.reverseOrder());
        short[] result = stream.toArray();
        Arrays.sort(result);
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testToShortList() {
        ShortList list = createShortStream((short) 1, (short) 2, (short) 3).toShortList();
        assertEquals(3, list.size());
        assertArrayEquals(new short[] { 1, 2, 3 }, list.toArray());
    }

    @Test
    public void testToMapWithMergeFunction() {
        Map<String, Integer> map = createShortStream((short) 1, (short) 2, (short) 1).toMap(n -> "key" + n, n -> (int) n, (v1, v2) -> v1 + v2);

        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(2), map.get("key1"));
        assertEquals(Integer.valueOf(2), map.get("key2"));
    }

    @Test
    public void testToMapWithSupplier() {
        Map<String, Short> map = createShortStream((short) 1, (short) 2, (short) 3).toMap(n -> "key" + n, n -> n, Suppliers.ofMap());

        assertTrue(map instanceof HashMap);
        assertEquals(3, map.size());
    }

    @Test
    public void testGroupTo() {
        Map<String, List<Short>> map = createShortStream((short) 1, (short) 2, (short) 3, (short) 4)
                .groupTo(n -> n % 2 == 0 ? "even" : "odd", java.util.stream.Collectors.toList());

        assertEquals(2, map.size());
        assertEquals(Arrays.asList((short) 1, (short) 3), map.get("odd"));
        assertEquals(Arrays.asList((short) 2, (short) 4), map.get("even"));
    }

    @Test
    public void testCollectWithSupplierAndAccumulator() {
        ShortList list = createShortStream((short) 1, (short) 2, (short) 3).collect(ShortList::new, ShortList::add);
        assertArrayEquals(new short[] { 1, 2, 3 }, list.toArray());
    }

    @Test
    public void testCollectWithSupplierAccumulatorCombiner() {
        List<Short> list = createShortStream((short) 1, (short) 2, (short) 3).collect(ArrayList::new, List::add, List::addAll);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), list);
    }

    @Test
    public void testForEachIndexed() {
        Map<Integer, Short> indexMap = new HashMap<>();
        createShortStream((short) 10, (short) 20, (short) 30).forEachIndexed((index, value) -> indexMap.put(index, value));

        assertEquals(3, indexMap.size());
        assertEquals(Short.valueOf((short) 10), indexMap.get(0));
        assertEquals(Short.valueOf((short) 20), indexMap.get(1));
        assertEquals(Short.valueOf((short) 30), indexMap.get(2));
    }

    @Test
    public void testFindFirstWithPredicate() {
        OptionalShort result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).findFirst(n -> n > 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    public void testFindAnyWithPredicate() {
        OptionalShort result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).findAny(n -> n > 2);
        assertTrue(result.isPresent());
        assertTrue(result.get() == 3 || result.get() == 4);
    }

    @Test
    public void testFindLastWithPredicate() {
        OptionalShort result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).findLast(n -> n < 3);
        assertTrue(result.isPresent());
        assertEquals(2, result.get());
    }

    @Test
    public void testMergeWith() {
        ShortStream a = createShortStream((short) 1, (short) 3);
        ShortStream b = createShortStream((short) 2, (short) 4);

        ShortStream merged = a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, merged.toArray());
    }

    @Test
    public void testZipWith() {
        ShortStream a = createShortStream((short) 1, (short) 2, (short) 3);
        ShortStream b = createShortStream((short) 10, (short) 20, (short) 30);

        ShortStream zipped = a.zipWith(b, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22, 33 }, zipped.toArray());
    }

    @Test
    public void testZipWithThree() {
        ShortStream a = createShortStream((short) 1, (short) 2);
        ShortStream b = createShortStream((short) 10, (short) 20);
        ShortStream c = createShortStream((short) 100, (short) 200);

        ShortStream zipped = a.zipWith(b, c, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 222 }, zipped.toArray());
    }

    @Test
    public void testZipWithDefault() {
        ShortStream a = createShortStream((short) 1, (short) 2, (short) 3);
        ShortStream b = createShortStream((short) 10, (short) 20);

        ShortStream zipped = a.zipWith(b, (short) 0, (short) 100, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22, 103 }, zipped.toArray());
    }

    @Test
    public void testZipWithThreeDefaults() {
        ShortStream a = createShortStream((short) 1, (short) 2, (short) 3);
        ShortStream b = createShortStream((short) 10, (short) 20);
        ShortStream c = createShortStream((short) 100);

        ShortStream zipped = a.zipWith(b, c, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 22, 3 }, zipped.toArray());
    }

    @Test
    public void testToImmutableList() {
        ImmutableList<Short> list = createShortStream((short) 1, (short) 2, (short) 3).toImmutableList();
        assertEquals(3, list.size());
        assertEquals(Short.valueOf((short) 1), list.get(0));
        assertEquals(Short.valueOf((short) 2), list.get(1));
        assertEquals(Short.valueOf((short) 3), list.get(2));
    }

    @Test
    public void testToImmutableSet() {
        ImmutableSet<Short> set = createShortStream((short) 1, (short) 2, (short) 2, (short) 3).toImmutableSet();
        assertEquals(3, set.size());
        assertTrue(set.contains((short) 1));
        assertTrue(set.contains((short) 2));
        assertTrue(set.contains((short) 3));
    }

    @Test
    public void testToCollection() {
        List<Short> list = createShortStream((short) 1, (short) 2, (short) 3).toCollection(ArrayList::new);
        assertTrue(list instanceof ArrayList);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), list);
    }

    @Test
    public void testToMultisetWithSupplier() {
        var multiset = createShortStream((short) 1, (short) 2, (short) 2, (short) 3).toMultiset(com.landawn.abacus.util.Multiset::new);
        assertEquals(1, multiset.count((short) 1));
        assertEquals(2, multiset.count((short) 2));
        assertEquals(1, multiset.count((short) 3));
    }

    @Test
    public void testJoinTo() {
        Joiner joiner = Joiner.with(", ", "[", "]");
        Joiner result = createShortStream((short) 1, (short) 2, (short) 3).joinTo(joiner);
        assertEquals("[1, 2, 3]", result.toString());
    }

    @Test
    public void testPrintln() {
        // This test just ensures println() doesn't throw an exception
        // The actual output goes to System.out
        createShortStream((short) 1, (short) 2, (short) 3).println();
    }

    @Test
    public void testSkipUntil() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(n -> n > 2);
        assertArrayEquals(new short[] { 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testFilterWithActionOnDropped() {
        List<Short> dropped = new ArrayList<>();
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).filter(n -> n % 2 == 0, dropped::add);
        assertArrayEquals(new short[] { 2, 4 }, stream.toArray());
        assertEquals(Arrays.asList((short) 1, (short) 3), dropped);
    }

    @Test
    public void testDropWhileWithActionOnDropped() {
        List<Short> dropped = new ArrayList<>();
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).dropWhile(n -> n < 3, dropped::add);
        assertArrayEquals(new short[] { 3, 4 }, stream.toArray());
        assertEquals(Arrays.asList((short) 1, (short) 2), dropped);
    }

    @Test
    public void testSkipWithActionOnSkipped() {
        List<Short> skipped = new ArrayList<>();
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).skip(2, skipped::add);
        assertArrayEquals(new short[] { 3, 4 }, stream.toArray());
        assertEquals(Arrays.asList((short) 1, (short) 2), skipped);
    }

    @Test
    public void testRateLimited() throws InterruptedException {
        RateLimiter rateLimiter = RateLimiter.create(10); // 10 permits per second
        long startTime = System.currentTimeMillis();

        createShortStream((short) 1, (short) 2, (short) 3).rateLimited(rateLimiter).toArray();

        long elapsedTime = System.currentTimeMillis() - startTime;
        // Should take at least 200ms for 3 elements with 10 permits/second
        assertTrue(elapsedTime >= 200);
    }

    @Test
    public void testRateLimitedWithPermitsPerSecond() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        createShortStream((short) 1, (short) 2, (short) 3)
                .rateLimited(10.0) // 10 permits per second
                .toArray();

        long elapsedTime = System.currentTimeMillis() - startTime;
        // Should take at least 200ms for 3 elements with 10 permits/second
        assertTrue(elapsedTime >= 200);
    }

    @Test
    public void testDelay() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        createShortStream((short) 1, (short) 2, (short) 3).delay(Duration.ofMillis(50)).toArray();

        long elapsedTime = System.currentTimeMillis() - startTime;
        // Should take at least 100ms for 3 elements with 50ms delay each
        assertTrue(elapsedTime >= 100);
    }

    @Test
    public void testOnEach() {
        List<Short> collected = new ArrayList<>();
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).onEach(collected::add).toArray();

        assertArrayEquals(new short[] { 1, 2, 3 }, result);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), collected);
    }

    @Test
    public void testThrowIfEmptyWithSupplier() {
        try {
            ShortStream.empty().throwIfEmpty(() -> new IllegalStateException("Stream is empty")).toArray();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Stream is empty", e.getMessage());
        }
    }

    @Test
    public void testIfEmpty() {
        final boolean[] actionExecuted = { false };

        ShortStream.empty().ifEmpty(() -> actionExecuted[0] = true).toArray();

        assertTrue(actionExecuted[0]);
    }

    @Test
    public void testTransform() {
        IntStream result = createShortStream((short) 1, (short) 2, (short) 3).transform(s -> s.mapToInt(n -> n * 10));

        assertArrayEquals(new int[] { 10, 20, 30 }, result.toArray());
    }

    @Test
    public void testDoubleUnderscoreTransform() {
        IntStream result = createShortStream((short) 1, (short) 2, (short) 3).__(s -> s.mapToInt(n -> n * 10));

        assertArrayEquals(new int[] { 10, 20, 30 }, result.toArray());
    }

    @Test
    public void testApplyIfNotEmpty() {
        Optional<Integer> result1 = createShortStream((short) 1, (short) 2, (short) 3).applyIfNotEmpty(s -> s.sum());
        assertTrue(result1.isPresent());
        assertEquals(6, result1.get().intValue());

        Optional<Integer> result2 = ShortStream.empty().applyIfNotEmpty(s -> s.sum());
        assertFalse(result2.isPresent());
    }

    @Test
    public void testAcceptIfNotEmpty() {
        final boolean[] actionExecuted = { false };

        OrElse orElse = createShortStream((short) 1, (short) 2, (short) 3).acceptIfNotEmpty(s -> {
            actionExecuted[0] = true;
            assertEquals(3, s.count());
        });

        assertTrue(actionExecuted[0]);

        // Test with empty stream
        actionExecuted[0] = false;
        ShortStream.empty().acceptIfNotEmpty(s -> actionExecuted[0] = true).orElse(() -> actionExecuted[0] = true);

        assertTrue(actionExecuted[0]);
    }

    @Test
    public void testParallelWithMaxThreadNum() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).parallel(2).map(n -> (short) (n * 2)).toArray();

        Arrays.sort(result);
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
    }

    @Test
    public void testParallelWithExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).parallel(executor).map(n -> (short) (n * 2)).toArray();

            Arrays.sort(result);
            assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testParallelWithMaxThreadNumAndExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).parallel(2, executor).map(n -> (short) (n * 2)).toArray();

            Arrays.sort(result);
            assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testParallelWithParallelSettings() {
        ParallelSettings ps = new ParallelSettings();
        ps.maxThreadNum(2);

        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).parallel(ps).map(n -> (short) (n * 2)).toArray();

        Arrays.sort(result);
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
    }

    @Test
    public void testSps() {
        // Sequential-Parallel-Sequential
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).sps(s -> s.map(n -> (short) (n * 2))).toArray();

        Arrays.sort(result);
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
    }

    @Test
    public void testSpsWithMaxThreadNum() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).sps(2, s -> s.map(n -> (short) (n * 2))).toArray();

        Arrays.sort(result);
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
    }

    @Test
    public void testSpsWithMaxThreadNumAndExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).sps(2, executor, s -> s.map(n -> (short) (n * 2)))
                    .toArray();

            Arrays.sort(result);
            assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testPsp() {
        // Parallel-Sequential-Parallel
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                .parallel()
                .psp(s -> s.map(n -> (short) (n * 2)))
                .sequential()
                .toArray();

        Arrays.sort(result);
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
    }

    @Test
    public void testIsParallel() {
        assertFalse(ShortStream.of((short) 1, (short) 2).isParallel());
        assertTrue(ShortStream.of((short) 1, (short) 2).parallel().isParallel());
    }

    @Test
    public void testSequential() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3).parallel().sequential();

        assertFalse(stream.isParallel());
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testIterator() {
        ShortIterator iter = createShortStream((short) 1, (short) 2, (short) 3).iterator();

        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextShort());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.nextShort());
        assertTrue(iter.hasNext());
        assertEquals(3, iter.nextShort());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFlattenWithAlignment() {
        short[][] array = { { 1, 2 }, { 3 }, { 4, 5, 6 } };
        ShortStream stream = ShortStream.flatten(array, (short) 0, false);
        assertArrayEquals(new short[] { 1, 2, 0, 3, 0, 0, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testFlattenWithAlignmentVertically() {
        short[][] array = { { 1, 2 }, { 3 }, { 4, 5, 6 } };
        ShortStream stream = ShortStream.flatten(array, (short) 0, true);
        assertArrayEquals(new short[] { 1, 3, 4, 2, 0, 5, 0, 0, 6 }, stream.toArray());
    }
}
