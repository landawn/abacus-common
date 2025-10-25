package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalShort;

@Tag("2025")
public class ShortStream2025Test extends TestBase {

    @Test
    public void testEmpty() {
        ShortStream stream = ShortStream.empty();
        assertEquals(0, stream.count());

        ShortStream stream2 = ShortStream.empty();
        assertArrayEquals(new short[0], stream2.toArray());
    }

    @Test
    public void testDefer() {
        final AtomicInteger counter = new AtomicInteger(0);
        ShortStream stream = ShortStream.defer(() -> {
            counter.incrementAndGet();
            return ShortStream.of((short) 1, (short) 2, (short) 3);
        });

        assertEquals(0, counter.get());

        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
        assertEquals(1, counter.get());
    }

    @Test
    public void testOfNullable() {
        ShortStream stream1 = ShortStream.ofNullable(null);
        assertEquals(0, stream1.count());

        ShortStream stream2 = ShortStream.ofNullable((short) 42);
        assertArrayEquals(new short[] { 42 }, stream2.toArray());
    }

    @Test
    public void testOfArray() {
        ShortStream stream1 = ShortStream.of();
        assertEquals(0, stream1.count());

        ShortStream stream2 = ShortStream.of((short) 1);
        assertArrayEquals(new short[] { 1 }, stream2.toArray());

        ShortStream stream3 = ShortStream.of((short) 1, (short) 2, (short) 3);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream3.toArray());
    }

    @Test
    public void testOfArrayWithRange() {
        short[] array = { 1, 2, 3, 4, 5 };

        ShortStream stream1 = ShortStream.of(array, 0, 5);
        assertArrayEquals(array, stream1.toArray());

        ShortStream stream2 = ShortStream.of(array, 1, 4);
        assertArrayEquals(new short[] { 2, 3, 4 }, stream2.toArray());

        ShortStream stream3 = ShortStream.of(array, 2, 2);
        assertEquals(0, stream3.count());
    }

    @Test
    public void testOfBoxedArray() {
        Short[] boxed = { 1, 2, 3, 4, 5 };
        ShortStream stream = ShortStream.of(boxed);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream.toArray());

        ShortStream stream2 = ShortStream.of(boxed, 1, 3);
        assertArrayEquals(new short[] { 2, 3 }, stream2.toArray());
    }

    @Test
    public void testOfCollection() {
        List<Short> list = Arrays.asList((short) 1, (short) 2, (short) 3);
        ShortStream stream = ShortStream.of(list);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testOfIterator() {
        ShortList list = new ShortList(new short[] { 1, 2, 3 });
        ShortStream stream = ShortStream.of(list.iterator());
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());

        ShortStream stream2 = ShortStream.of((ShortIterator) null);
        assertEquals(0, stream2.count());
    }

    @Test
    public void testOfShortBuffer() {
        ShortBuffer buffer = ShortBuffer.wrap(new short[] { 1, 2, 3, 4, 5 });
        buffer.position(1);
        buffer.limit(4);

        ShortStream stream = ShortStream.of(buffer);
        assertArrayEquals(new short[] { 2, 3, 4 }, stream.toArray());

        ShortStream stream2 = ShortStream.of((ShortBuffer) null);
        assertEquals(0, stream2.count());
    }

    @Test
    public void testFlatten2D() {
        short[][] array = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        ShortStream stream = ShortStream.flatten(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());

        ShortStream stream2 = ShortStream.flatten(new short[0][]);
        assertEquals(0, stream2.count());
    }

    @Test
    public void testFlattenVertically() {
        short[][] array = { { 1, 2, 3 }, { 4, 5, 6 } };

        ShortStream stream1 = ShortStream.flatten(array, false);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream1.toArray());

        ShortStream stream2 = ShortStream.flatten(array, true);
        assertArrayEquals(new short[] { 1, 4, 2, 5, 3, 6 }, stream2.toArray());
    }

    @Test
    public void testFlattenVerticallyWithJaggedArray() {
        short[][] array = { { 1, 2, 3 }, { 4 }, { 5, 6 } };

        ShortStream stream = ShortStream.flatten(array, true);
        assertArrayEquals(new short[] { 1, 4, 5, 2, 6, 3 }, stream.toArray());
    }

    @Test
    public void testFlattenWithAlignment() {
        short[][] array = { { 1, 2 }, { 3 }, { 4, 5, 6 } };
        short valueForAlignment = 0;

        ShortStream stream = ShortStream.flatten(array, valueForAlignment, true);
        assertArrayEquals(new short[] { 1, 3, 4, 2, 0, 5, 0, 0, 6 }, stream.toArray());
    }

    @Test
    public void testFlatten3D() {
        short[][][] array = { { { 1, 2 }, { 3 } }, { { 4, 5, 6 } } };
        ShortStream stream = ShortStream.flatten(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testRange() {
        ShortStream stream1 = ShortStream.range((short) 1, (short) 4);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream1.toArray());

        ShortStream stream2 = ShortStream.range((short) 5, (short) 5);
        assertEquals(0, stream2.count());

        ShortStream stream3 = ShortStream.range((short) 5, (short) 3);
        assertEquals(0, stream3.count());
    }

    @Test
    public void testRangeWithStep() {
        ShortStream stream1 = ShortStream.range((short) 0, (short) 10, (short) 3);
        assertArrayEquals(new short[] { 0, 3, 6, 9 }, stream1.toArray());

        ShortStream stream2 = ShortStream.range((short) 10, (short) 0, (short) -3);
        assertArrayEquals(new short[] { 10, 7, 4, 1 }, stream2.toArray());

        ShortStream stream3 = ShortStream.range((short) 1, (short) 5, (short) 1);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream3.toArray());
    }

    @Test
    public void testRangeWithZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> ShortStream.range((short) 0, (short) 10, (short) 0));
    }

    @Test
    public void testRangeClosed() {
        ShortStream stream1 = ShortStream.rangeClosed((short) 1, (short) 3);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream1.toArray());

        ShortStream stream2 = ShortStream.rangeClosed((short) 5, (short) 5);
        assertArrayEquals(new short[] { 5 }, stream2.toArray());
    }

    @Test
    public void testRangeClosedWithStep() {
        ShortStream stream1 = ShortStream.rangeClosed((short) 0, (short) 10, (short) 3);
        assertArrayEquals(new short[] { 0, 3, 6, 9 }, stream1.toArray());

        ShortStream stream2 = ShortStream.rangeClosed((short) 10, (short) 0, (short) -3);
        assertArrayEquals(new short[] { 10, 7, 4, 1 }, stream2.toArray());
    }

    @Test
    public void testRepeat() {
        ShortStream stream1 = ShortStream.repeat((short) 5, 3);
        assertArrayEquals(new short[] { 5, 5, 5 }, stream1.toArray());

        ShortStream stream2 = ShortStream.repeat((short) 5, 0);
        assertEquals(0, stream2.count());

        ShortStream stream3 = ShortStream.repeat((short) 7, 1);
        assertArrayEquals(new short[] { 7 }, stream3.toArray());
    }

    @Test
    public void testRandom() {
        ShortStream stream = ShortStream.random().limit(10);
        assertEquals(10, stream.count());

        ShortStream stream2 = ShortStream.random().limit(5);
        short[] values = stream2.toArray();
        assertEquals(5, values.length);
    }

    @Test
    public void testIterateWithHasNext() {
        final int[] count = { 0 };
        ShortStream stream = ShortStream.iterate(() -> count[0] < 3, () -> (short) count[0]++);
        assertArrayEquals(new short[] { 0, 1, 2 }, stream.toArray());
    }

    @Test
    public void testIterateWithSeedAndHasNext() {
        ShortStream stream = ShortStream.iterate((short) 1, () -> true, n -> (short) (n * 2)).limit(4);
        assertArrayEquals(new short[] { 1, 2, 4, 8 }, stream.toArray());
    }

    @Test
    public void testIterateWithSeedAndPredicate() {
        ShortStream stream = ShortStream.iterate((short) 1, n -> n < 10, n -> (short) (n * 2));
        assertArrayEquals(new short[] { 1, 2, 4, 8 }, stream.toArray());
    }

    @Test
    public void testIterateWithSeed() {
        ShortStream stream = ShortStream.iterate((short) 1, n -> (short) (n * 2)).limit(4);
        assertArrayEquals(new short[] { 1, 2, 4, 8 }, stream.toArray());
    }

    @Test
    public void testGenerate() {
        final short[] value = { 0 };
        ShortStream stream = ShortStream.generate(() -> value[0]++).limit(3);
        assertArrayEquals(new short[] { 0, 1, 2 }, stream.toArray());
    }

    @Test
    public void testConcatArrays() {
        short[] a1 = { 1, 2 };
        short[] a2 = { 3, 4 };
        ShortStream stream = ShortStream.concat(a1, a2);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());

        short[] a3 = { 5, 6 };
        ShortStream stream2 = ShortStream.concat(a1, a2, a3);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream2.toArray());
    }

    @Test
    public void testConcatIterators() {
        ShortIterator it1 = ShortIterator.of((short) 1, (short) 2);
        ShortIterator it2 = ShortIterator.of((short) 3, (short) 4);

        ShortStream stream = ShortStream.concat(it1, it2);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcatStreams() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2);
        ShortStream s2 = ShortStream.of((short) 3, (short) 4);
        ShortStream stream = ShortStream.concat(s1, s2);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcatList() {
        List<short[]> list = Arrays.asList(new short[] { 1, 2 }, new short[] { 3, 4 }, new short[] { 5 });

        ShortStream stream = ShortStream.concat(list);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testConcatStreamCollection() {
        Collection<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 2), ShortStream.of((short) 3, (short) 4), ShortStream.of((short) 5));

        ShortStream stream = ShortStream.concat(streams);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testConcatIteratorCollection() {
        Collection<ShortIterator> iterators = Arrays.asList(ShortIterator.of((short) 1, (short) 2), ShortIterator.of((short) 3, (short) 4));

        ShortStream stream = ShortStream.concatIterators(iterators);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testZip() {
        short[] a = { 1, 2, 3 };
        short[] b = { 10, 20, 30 };

        ShortStream stream = ShortStream.zip(a, b, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22, 33 }, stream.toArray());
    }

    @Test
    public void testZipWithDifferentLengths() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 10, 20 };

        ShortStream stream = ShortStream.zip(a, b, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22 }, stream.toArray());
    }

    @Test
    public void testZipThreeArrays() {
        short[] a = { 1, 2, 3 };
        short[] b = { 10, 20, 30 };
        short[] c = { 100, 200, 300 };

        ShortStream stream = ShortStream.zip(a, b, c, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 222, 333 }, stream.toArray());
    }

    @Test
    public void testZipIterators() {
        ShortIterator it1 = ShortIterator.of((short) 1, (short) 2);
        ShortIterator it2 = ShortIterator.of((short) 10, (short) 20);

        ShortStream stream = ShortStream.zip(it1, it2, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22 }, stream.toArray());
    }

    @Test
    public void testZipThreeIterators() {
        ShortIterator it1 = ShortIterator.of((short) 1, (short) 2);
        ShortIterator it2 = ShortIterator.of((short) 10, (short) 20);
        ShortIterator it3 = ShortIterator.of((short) 100, (short) 200);

        ShortStream stream = ShortStream.zip(it1, it2, it3, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 222 }, stream.toArray());
    }

    @Test
    public void testZipStreams() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2);
        ShortStream s2 = ShortStream.of((short) 10, (short) 20);

        ShortStream stream = ShortStream.zip(s1, s2, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22 }, stream.toArray());
    }

    @Test
    public void testZipThreeStreams() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2);
        ShortStream s2 = ShortStream.of((short) 10, (short) 20);
        ShortStream s3 = ShortStream.of((short) 100, (short) 200);

        ShortStream stream = ShortStream.zip(s1, s2, s3, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 222 }, stream.toArray());
    }

    @Test
    public void testZipThreeWithDefaults() {
        short[] a = { 1, 2, 3 };
        short[] b = { 10, 20 };
        short[] c = { 100 };

        ShortStream stream = ShortStream.zip(a, b, c, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 22, 3 }, stream.toArray());
    }

    @Test
    public void testZipIteratorsWithDefaults() {
        ShortIterator it1 = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortIterator it2 = ShortIterator.of((short) 10);

        ShortStream stream = ShortStream.zip(it1, it2, (short) 0, (short) 0, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 2, 3 }, stream.toArray());
    }

    @Test
    public void testZipStreamsWithDefaults() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2);
        ShortStream s2 = ShortStream.of((short) 10, (short) 20, (short) 30);

        ShortStream stream = ShortStream.zip(s1, s2, (short) 0, (short) 0, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22, 30 }, stream.toArray());
    }

    @Test
    public void testMerge() {
        short[] a = { 1, 3, 5 };
        short[] b = { 2, 4, 6 };

        ShortStream stream = ShortStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMergeThreeArrays() {
        short[] a = { 1, 4, 7 };
        short[] b = { 2, 5, 8 };
        short[] c = { 3, 6, 9 };

        ShortStream stream = ShortStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, stream.toArray());
    }

    @Test
    public void testMergeIterators() {
        ShortIterator it1 = ShortIterator.of((short) 1, (short) 3, (short) 5);
        ShortIterator it2 = ShortIterator.of((short) 2, (short) 4, (short) 6);

        ShortStream stream = ShortStream.merge(it1, it2, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMergeStreams() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 3, (short) 5);
        ShortStream s2 = ShortStream.of((short) 2, (short) 4, (short) 6);

        ShortStream stream = ShortStream.merge(s1, s2, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testFilter() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).filter(n -> n % 2 == 0);
        assertArrayEquals(new short[] { 2, 4 }, stream.toArray());
    }

    @Test
    public void testFilterEmpty() {
        ShortStream stream = ShortStream.of((short) 1, (short) 3, (short) 5).filter(n -> n % 2 == 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testTakeWhile() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 2).takeWhile(n -> n < 4);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testDropWhile() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 2).dropWhile(n -> n < 3);
        assertArrayEquals(new short[] { 3, 4, 2 }, stream.toArray());
    }

    @Test
    public void testMap() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).map(n -> (short) (n * 2));
        assertArrayEquals(new short[] { 2, 4, 6 }, stream.toArray());
    }

    @Test
    public void testMapToInt() {
        IntStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).mapToInt(n -> n * 10);
        assertArrayEquals(new int[] { 10, 20, 30 }, stream.toArray());
    }

    @Test
    public void testMapToObj() {
        Stream<String> stream = ShortStream.of((short) 1, (short) 2, (short) 3).mapToObj(n -> "S" + n);
        assertArrayEquals(new String[] { "S1", "S2", "S3" }, stream.toArray());
    }

    @Test
    public void testFlatMap() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2).flatMap(n -> ShortStream.of(n, (short) (n + 10)));
        assertArrayEquals(new short[] { 1, 11, 2, 12 }, stream.toArray());
    }

    @Test
    public void testFlatmap() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2).flatmap(n -> new short[] { n, (short) (n + 10) });
        assertArrayEquals(new short[] { 1, 11, 2, 12 }, stream.toArray());
    }

    @Test
    public void testFlatMapToInt() {
        IntStream stream = ShortStream.of((short) 1, (short) 2).flatMapToInt(n -> IntStream.of(n, n + 10));
        assertArrayEquals(new int[] { 1, 11, 2, 12 }, stream.toArray());
    }

    @Test
    public void testFlatMapToObj() {
        Stream<String> stream = ShortStream.of((short) 1, (short) 2).flatMapToObj(n -> Stream.of("A" + n, "B" + n));
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2" }, stream.toArray());
    }

    @Test
    public void testFlatmapToObj() {
        Stream<String> stream = ShortStream.of((short) 1, (short) 2).flatmapToObj(n -> Arrays.asList("A" + n, "B" + n));
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2" }, stream.toArray());
    }

    @Test
    public void testFlattmapToObj() {
        Stream<String> stream = ShortStream.of((short) 1, (short) 2).flattmapToObj(n -> new String[] { "A" + n, "B" + n });
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2" }, stream.toArray());
    }

    @Test
    public void testMapPartial() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4)
                .mapPartial(n -> n % 2 == 0 ? OptionalShort.of((short) (n * 10)) : OptionalShort.empty());
        assertArrayEquals(new short[] { 20, 40 }, stream.toArray());
    }

    @Test
    public void testRangeMap() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 10, (short) 11, (short) 20)
                .rangeMap((first, next) -> next - first <= 2, (first, last) -> (short) (first + last));
        assertArrayEquals(new short[] { 4, 21, 40 }, stream.toArray());
    }

    @Test
    public void testRangeMapToObj() {
        Stream<String> stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 10)
                .rangeMapToObj((first, next) -> next - first <= 2, (first, last) -> first + "-" + last);
        assertArrayEquals(new String[] { "1-3", "10-10" }, stream.toArray());
    }

    @Test
    public void testCollapse() {
        Stream<ShortList> stream = ShortStream.of((short) 1, (short) 2, (short) 5, (short) 6, (short) 7, (short) 10).collapse((prev, curr) -> curr - prev <= 2);

        List<ShortList> result = stream.toList();
        assertEquals(3, result.size());
        assertArrayEquals(new short[] { 1, 2 }, result.get(0).toArray());
        assertArrayEquals(new short[] { 5, 6, 7 }, result.get(1).toArray());
        assertArrayEquals(new short[] { 10 }, result.get(2).toArray());
    }

    @Test
    public void testCollapseWithMerge() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 5, (short) 6, (short) 7, (short) 10)
                .collapse((prev, curr) -> curr - prev <= 2, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 3, 18, 10 }, stream.toArray());
    }

    @Test
    public void testCollapseWithTriPredicate() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 10, (short) 11)
                .collapse((first, last, next) -> next - first <= 5, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 6, 21 }, stream.toArray());
    }

    @Test
    public void testScan() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).scan((a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 1, 3, 6, 10 }, stream.toArray());
    }

    @Test
    public void testScanWithInit() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).scan((short) 10, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 11, 13, 16 }, stream.toArray());
    }

    @Test
    public void testScanWithInitIncluded() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).scan((short) 10, true, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 10, 11, 13, 16 }, stream.toArray());

        ShortStream stream2 = ShortStream.of((short) 1, (short) 2, (short) 3).scan((short) 10, false, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 11, 13, 16 }, stream2.toArray());
    }

    @Test
    public void testPrepend() {
        ShortStream stream = ShortStream.of((short) 3, (short) 4).prepend((short) 1, (short) 2);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testAppend() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2).append((short) 3, (short) 4);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testAppendIfEmpty() {
        ShortStream stream1 = ShortStream.of((short) 1, (short) 2).appendIfEmpty((short) 3, (short) 4);
        assertArrayEquals(new short[] { 1, 2 }, stream1.toArray());

        ShortStream stream2 = ShortStream.empty().appendIfEmpty((short) 3, (short) 4);
        assertArrayEquals(new short[] { 3, 4 }, stream2.toArray());
    }

    @Test
    public void testTop() {
        ShortStream stream = ShortStream.of((short) 5, (short) 2, (short) 8, (short) 1, (short) 9, (short) 3).top(3);
        short[] result = stream.sorted().toArray();
        assertArrayEquals(new short[] { 8, 9 }, Arrays.copyOfRange(result, Math.max(0, result.length - 2), result.length));
    }

    @Test
    public void testTopWithComparator() {
        ShortStream stream = ShortStream.of((short) 5, (short) 2, (short) 8, (short) 1, (short) 9).top(3, Comparator.reverseOrder());
        short[] result = stream.toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testDistinct() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 1).distinct();
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testSorted() {
        ShortStream stream = ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2).sorted();
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testReverseSorted() {
        ShortStream stream = ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2).reverseSorted();
        assertArrayEquals(new short[] { 4, 3, 2, 1 }, stream.toArray());
    }

    @Test
    public void testPeek() {
        List<Short> peeked = new ArrayList<>();
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).peek(peeked::add);
        stream.toArray();
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), peeked);
    }

    @Test
    public void testLimit() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).limit(2);
        assertArrayEquals(new short[] { 1, 2 }, stream.toArray());
    }

    @Test
    public void testLimitZero() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).limit(0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testSkip() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).skip(2);
        assertArrayEquals(new short[] { 3, 4 }, stream.toArray());
    }

    @Test
    public void testSkipAll() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2).skip(5);
        assertEquals(0, stream.count());
    }

    @Test
    public void testForEach() {
        List<Short> collected = new ArrayList<>();
        ShortStream.of((short) 1, (short) 2, (short) 3).forEach(collected::add);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), collected);
    }

    @Test
    public void testForEachIndexed() {
        Map<Integer, Short> collected = new HashMap<>();
        ShortStream.of((short) 10, (short) 20, (short) 30).forEachIndexed(collected::put);
        assertEquals((short) 10, collected.get(0).shortValue());
        assertEquals((short) 20, collected.get(1).shortValue());
        assertEquals((short) 30, collected.get(2).shortValue());
    }

    @Test
    public void testToArray() {
        short[] array = ShortStream.of((short) 1, (short) 2, (short) 3).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, array);
    }

    @Test
    public void testToShortList() {
        ShortList list = ShortStream.of((short) 1, (short) 2, (short) 3).toShortList();
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void testToList() {
        List<Short> list = ShortStream.of((short) 1, (short) 2, (short) 3).toList();
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), list);
    }

    @Test
    public void testToSet() {
        var set = ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3).toSet();
        assertEquals(3, set.size());
        assertTrue(set.contains((short) 1));
        assertTrue(set.contains((short) 2));
        assertTrue(set.contains((short) 3));
    }

    @Test
    public void testToMultiset() {
        Multiset<Short> multiset = ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3).toMultiset();
        assertEquals(1, multiset.getCount((short) 1));
        assertEquals(2, multiset.getCount((short) 2));
        assertEquals(1, multiset.getCount((short) 3));
    }

    @Test
    public void testToMap() {
        Map<Short, String> map = ShortStream.of((short) 1, (short) 2, (short) 3).toMap(n -> n, n -> "V" + n);
        assertEquals("V1", map.get((short) 1));
        assertEquals("V2", map.get((short) 2));
        assertEquals("V3", map.get((short) 3));
    }

    @Test
    public void testToMapWithFactory() {
        Map<Short, String> map = ShortStream.of((short) 1, (short) 2, (short) 3).toMap(n -> n, n -> "V" + n, Suppliers.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertEquals("V1", map.get((short) 1));
    }

    @Test
    public void testToMapWithMerge() {
        Map<Short, String> map = ShortStream.of((short) 1, (short) 2, (short) 1).toMap(n -> n, n -> "V" + n, (a, b) -> a + "," + b);
        assertEquals("V1,V1", map.get((short) 1));
        assertEquals("V2", map.get((short) 2));
    }

    @Test
    public void testToMapWithMergeAndFactory() {
        Map<Short, String> map = ShortStream.of((short) 1, (short) 2, (short) 1)
                .toMap(n -> n, n -> "V" + n, (a, b) -> a + "," + b, Suppliers.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertEquals("V1,V1", map.get((short) 1));
    }

    @Test
    public void testGroupTo() {
        Map<Boolean, List<Short>> map = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).groupTo(n -> n % 2 == 0, Collectors.toList());
        assertEquals(Arrays.asList((short) 2, (short) 4), map.get(true));
        assertEquals(Arrays.asList((short) 1, (short) 3), map.get(false));
    }

    @Test
    public void testGroupToWithFactory() {
        Map<Boolean, List<Short>> map = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4)
                .groupTo(n -> n % 2 == 0, Collectors.toList(), LinkedHashMap::new);
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testReduce() {
        short result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).reduce((short) 0, (a, b) -> (short) (a + b));
        assertEquals(10, result);
    }

    @Test
    public void testReduceOptional() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).reduce((a, b) -> (short) (a + b));
        assertTrue(result.isPresent());
        assertEquals(10, result.get());
    }

    @Test
    public void testReduceOptionalEmpty() {
        OptionalShort result = ShortStream.empty().reduce((a, b) -> (short) (a + b));
        assertFalse(result.isPresent());
    }

    @Test
    public void testCollect() {
        ShortList result = ShortStream.of((short) 1, (short) 2, (short) 3).collect(ShortList::new, ShortList::add, ShortList::addAll);
        assertArrayEquals(new short[] { 1, 2, 3 }, result.toArray());
    }

    @Test
    public void testCollectWithoutCombiner() {
        ShortList result = ShortStream.of((short) 1, (short) 2, (short) 3).collect(ShortList::new, ShortList::add);
        assertArrayEquals(new short[] { 1, 2, 3 }, result.toArray());
    }

    @Test
    public void testSum() {
        int sum = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).sum();
        assertEquals(10, sum);
    }

    @Test
    public void testSumEmpty() {
        int sum = ShortStream.empty().sum();
        assertEquals(0, sum);
    }

    @Test
    public void testAverage() {
        OptionalDouble avg = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).average();
        assertTrue(avg.isPresent());
        assertEquals(2.5, avg.get(), 0.0001);
    }

    @Test
    public void testAverageEmpty() {
        OptionalDouble avg = ShortStream.empty().average();
        assertFalse(avg.isPresent());
    }

    @Test
    public void testMin() {
        OptionalShort min = ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2).min();
        assertTrue(min.isPresent());
        assertEquals(1, min.get());
    }

    @Test
    public void testMinEmpty() {
        OptionalShort min = ShortStream.empty().min();
        assertFalse(min.isPresent());
    }

    @Test
    public void testMax() {
        OptionalShort max = ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2).max();
        assertTrue(max.isPresent());
        assertEquals(4, max.get());
    }

    @Test
    public void testMaxEmpty() {
        OptionalShort max = ShortStream.empty().max();
        assertFalse(max.isPresent());
    }

    @Test
    public void testKthLargest() {
        OptionalShort result = ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4, result.get());
    }

    @Test
    public void testKthLargestEmpty() {
        OptionalShort result = ShortStream.empty().kthLargest(1);
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargestOutOfRange() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2).kthLargest(5);
        assertFalse(result.isPresent());
    }

    @Test
    public void testCount() {
        long count = ShortStream.of((short) 1, (short) 2, (short) 3).count();
        assertEquals(3, count);
    }

    @Test
    public void testCountEmpty() {
        long count = ShortStream.empty().count();
        assertEquals(0, count);
    }

    @Test
    public void testAnyMatch() {
        boolean result = ShortStream.of((short) 1, (short) 2, (short) 3).anyMatch(n -> n > 2);
        assertTrue(result);
    }

    @Test
    public void testAnyMatchNone() {
        boolean result = ShortStream.of((short) 1, (short) 2, (short) 3).anyMatch(n -> n > 5);
        assertFalse(result);
    }

    @Test
    public void testAnyMatchEmpty() {
        boolean result = ShortStream.empty().anyMatch(n -> true);
        assertFalse(result);
    }

    @Test
    public void testAllMatch() {
        boolean result = ShortStream.of((short) 1, (short) 2, (short) 3).allMatch(n -> n > 0);
        assertTrue(result);
    }

    @Test
    public void testAllMatchFalse() {
        boolean result = ShortStream.of((short) 1, (short) 2, (short) 3).allMatch(n -> n > 2);
        assertFalse(result);
    }

    @Test
    public void testAllMatchEmpty() {
        boolean result = ShortStream.empty().allMatch(n -> false);
        assertTrue(result);
    }

    @Test
    public void testNoneMatch() {
        boolean result = ShortStream.of((short) 1, (short) 2, (short) 3).noneMatch(n -> n > 5);
        assertTrue(result);
    }

    @Test
    public void testNoneMatchFalse() {
        boolean result = ShortStream.of((short) 1, (short) 2, (short) 3).noneMatch(n -> n > 2);
        assertFalse(result);
    }

    @Test
    public void testNoneMatchEmpty() {
        boolean result = ShortStream.empty().noneMatch(n -> true);
        assertTrue(result);
    }

    @Test
    public void testFindFirst() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2, (short) 3).findFirst();
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testFindFirstEmpty() {
        OptionalShort result = ShortStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirstWithPredicate() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).findFirst(n -> n > 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    public void testFindFirstWithPredicateNone() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2, (short) 3).findFirst(n -> n > 5);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAny() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2, (short) 3).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAnyEmpty() {
        OptionalShort result = ShortStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAnyWithPredicate() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).findAny(n -> n > 2);
        assertTrue(result.isPresent());
        assertTrue(result.get() > 2);
    }

    @Test
    public void testFindLast() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2, (short) 3).last();
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    public void testFindLastEmpty() {
        OptionalShort result = ShortStream.empty().last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLastWithPredicate() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 2).findLast(n -> n == 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.get());
    }

    @Test
    public void testSummarize() {
        ShortSummaryStatistics stats = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).summarize();
        assertNotNull(stats);
        assertEquals(4, stats.getCount());
        assertEquals(10, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(4, stats.getMax());
        assertEquals(2.5, stats.getAverage(), 0.0001);
    }

    @Test
    public void testSummarizeEmpty() {
        ShortSummaryStatistics stats = ShortStream.empty().summarize();
        assertNotNull(stats);
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testSummarizeAndPercentiles() {
        Pair<ShortSummaryStatistics, Optional<Map<Percentage, Short>>> result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                .summarizeAndPercentiles();

        assertNotNull(result.left());
        assertEquals(5, result.left().getCount());
        assertTrue(result.right().isPresent());
    }

    @Test
    public void testMergeWith() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 3, (short) 5);
        ShortStream s2 = ShortStream.of((short) 2, (short) 4, (short) 6);

        ShortStream stream = s1.mergeWith(s2, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testZipWith() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2, (short) 3);
        ShortStream s2 = ShortStream.of((short) 10, (short) 20, (short) 30);

        ShortStream stream = s1.zipWith(s2, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22, 33 }, stream.toArray());
    }

    @Test
    public void testZipWithThree() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2);
        ShortStream s2 = ShortStream.of((short) 10, (short) 20);
        ShortStream s3 = ShortStream.of((short) 100, (short) 200);

        ShortStream stream = s1.zipWith(s2, s3, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 222 }, stream.toArray());
    }

    @Test
    public void testZipWithDefaults() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2, (short) 3);
        ShortStream s2 = ShortStream.of((short) 10, (short) 20);

        ShortStream stream = s1.zipWith(s2, (short) 0, (short) 0, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22, 3 }, stream.toArray());
    }

    @Test
    public void testZipWithThreeDefaults() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2);
        ShortStream s2 = ShortStream.of((short) 10);
        ShortStream s3 = ShortStream.of((short) 100, (short) 200, (short) 300);

        ShortStream stream = s1.zipWith(s2, s3, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 202, 300 }, stream.toArray());
    }

    @Test
    public void testAsIntStream() {
        IntStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).asIntStream();
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testBoxed() {
        Stream<Short> stream = ShortStream.of((short) 1, (short) 2, (short) 3).boxed();
        assertArrayEquals(new Short[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testIterator() {
        ShortIterator iterator = ShortStream.of((short) 1, (short) 2, (short) 3).iterator();
        assertTrue(iterator.hasNext());
        assertEquals(1, iterator.nextShort());
        assertTrue(iterator.hasNext());
        assertEquals(2, iterator.nextShort());
        assertTrue(iterator.hasNext());
        assertEquals(3, iterator.nextShort());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testIteratorEmpty() {
        ShortIterator iterator = ShortStream.empty().iterator();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testIteratorThrowsNoSuchElement() {
        ShortIterator iterator = ShortStream.of((short) 1).iterator();
        iterator.nextShort();
        assertThrows(NoSuchElementException.class, iterator::nextShort);
    }

    @Test
    public void testSequential() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).parallel().sequential();
        assertFalse(stream.isParallel());
    }

    @Test
    public void testParallel() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).parallel();
        assertTrue(stream.isParallel());
    }

    @Test
    public void testIsParallel() {
        ShortStream stream1 = ShortStream.of((short) 1, (short) 2, (short) 3);
        assertFalse(stream1.isParallel());

        ShortStream stream2 = ShortStream.of((short) 1, (short) 2, (short) 3).parallel();
        assertTrue(stream2.isParallel());
    }

    @Test
    public void testOnClose() {
        final AtomicInteger closeCount = new AtomicInteger(0);
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).onClose(closeCount::incrementAndGet);

        assertEquals(0, closeCount.get());
        stream.close();
        assertEquals(1, closeCount.get());
    }

    @Test
    public void testOnCloseMultiple() {
        final AtomicInteger closeCount = new AtomicInteger(0);
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).onClose(closeCount::incrementAndGet).onClose(closeCount::incrementAndGet);

        stream.close();
        assertEquals(2, closeCount.get());
    }

    @Test
    public void testClose() {
        final AtomicInteger closeCount = new AtomicInteger(0);
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).onClose(closeCount::incrementAndGet);

        stream.close();
        assertEquals(1, closeCount.get());

        stream.close();
        assertEquals(1, closeCount.get());
    }

    @Test
    public void testStreamAutoClosedAfterTerminalOp() {
        final AtomicInteger closeCount = new AtomicInteger(0);
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).onClose(closeCount::incrementAndGet);

        stream.count();
        assertEquals(1, closeCount.get());
    }

    @Test
    public void testComplexPipeline() {
        short[] result = ShortStream.range((short) 1, (short) 10).filter(n -> n % 2 == 0).map(n -> (short) (n * 2)).sorted().limit(3).toArray();

        assertArrayEquals(new short[] { 4, 8, 12 }, result);
    }

    @Test
    public void testChainedOperations() {
        long count = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                .filter(n -> n > 2)
                .map(n -> (short) (n * 10))
                .peek(n -> assertTrue(n >= 30))
                .count();

        assertEquals(3, count);
    }

    @Test
    public void testEmptyStreamOperations() {
        ShortStream stream = ShortStream.empty().filter(n -> true).map(n -> (short) (n * 2)).sorted().distinct();

        assertEquals(0, stream.count());
    }

    // Additional tests to improve coverage from 81.9% to 90%+

    @Test
    public void testZipWithCollectionOfStreams() {
        Collection<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 2), ShortStream.of((short) 10, (short) 20),
                ShortStream.of((short) 100, (short) 200));
        ShortStream result = ShortStream.zip(streams, shorts -> {
            short sum = 0;
            for (Short s : shorts)
                sum += s;
            return sum;
        });
        assertArrayEquals(new short[] { 111, 222 }, result.toArray());
    }

    @Test
    public void testZipWithCollectionAndValuesForNone() {
        Collection<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 2, (short) 3), ShortStream.of((short) 10),
                ShortStream.of((short) 100, (short) 200));
        short[] defaults = { 0, 0, 0 };
        ShortStream result = ShortStream.zip(streams, defaults, shorts -> {
            short sum = 0;
            for (Short s : shorts)
                sum += s;
            return sum;
        });
        assertArrayEquals(new short[] { 111, 202, 3 }, result.toArray());
    }

    @Test
    public void testMergeWithCollectionOfStreams() {
        Collection<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 5), ShortStream.of((short) 2, (short) 6),
                ShortStream.of((short) 3, (short) 7));
        ShortStream result = ShortStream.merge(streams, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 5, 6, 7 }, result.toArray());
    }

    @Test
    public void testMergeThreeIterators() {
        ShortIterator it1 = ShortIterator.of((short) 1, (short) 4);
        ShortIterator it2 = ShortIterator.of((short) 2, (short) 5);
        ShortIterator it3 = ShortIterator.of((short) 3, (short) 6);

        ShortStream result = ShortStream.merge(it1, it2, it3, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, result.toArray());
    }

    @Test
    public void testMergeThreeStreams() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 4, (short) 7);
        ShortStream s2 = ShortStream.of((short) 2, (short) 5, (short) 8);
        ShortStream s3 = ShortStream.of((short) 3, (short) 6, (short) 9);

        ShortStream result = ShortStream.merge(s1, s2, s3, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result.toArray());
    }

    @Test
    public void testFlattenWithAlignmentHorizontally() {
        short[][] array = { { 1, 2 }, { 3 }, { 4, 5, 6 } };
        short valueForAlignment = 0;

        ShortStream stream = ShortStream.flatten(array, valueForAlignment, false);
        assertArrayEquals(new short[] { 1, 2, 0, 3, 0, 0, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testRangeClosedWithNegativeStep() {
        ShortStream stream = ShortStream.rangeClosed((short) 5, (short) 1, (short) -1);
        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 }, stream.toArray());
    }

    @Test
    public void testRangeWithInvalidStepDirection() {
        // Positive step but start > end should return empty
        ShortStream stream = ShortStream.range((short) 10, (short) 1, (short) 2);
        assertEquals(0, stream.count());

        // Negative step but start < end should return empty
        ShortStream stream2 = ShortStream.range((short) 1, (short) 10, (short) -2);
        assertEquals(0, stream2.count());
    }

    @Test
    public void testParallelStreamOperations() {
        long sum = ShortStream.range((short) 1, (short) 100).parallel().filter(n -> n % 2 == 0).map(n -> (short) (n * 2)).sum();

        assertTrue(sum > 0);

        // Verify parallel flag is maintained through operations
        ShortStream parallelStream = ShortStream.of((short) 1, (short) 2, (short) 3).parallel().filter(n -> n > 0).map(n -> (short) (n * 2));
        assertTrue(parallelStream.isParallel());
    }

    @Test
    public void testIterateWithImmediateFalseHasNext() {
        ShortStream stream = ShortStream.iterate(() -> false, () -> (short) 1);
        assertEquals(0, stream.count());

        ShortStream stream2 = ShortStream.iterate((short) 1, () -> false, n -> (short) (n + 1));
        assertEquals(0, stream2.count());

        ShortStream stream3 = ShortStream.iterate((short) 1, n -> false, n -> (short) (n + 1));
        assertEquals(0, stream3.count());
    }

    @Test
    public void testCollapseWithNoCollapsibleElements() {
        ShortStream stream = ShortStream.of((short) 1, (short) 10, (short) 20, (short) 30)
                .collapse((prev, curr) -> curr - prev <= 2, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 1, 10, 20, 30 }, stream.toArray());
    }

    @Test
    public void testScanOnEmptyStream() {
        ShortStream stream1 = ShortStream.empty().scan((a, b) -> (short) (a + b));
        assertEquals(0, stream1.count());

        ShortStream stream2 = ShortStream.empty().scan((short) 10, (a, b) -> (short) (a + b));
        assertEquals(0, stream2.count());

        ShortStream stream3 = ShortStream.empty().scan((short) 10, true, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 10 }, stream3.toArray());
    }

    @Test
    public void testZipThreeArraysWithDefaults() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 10, 20 };
        short[] c = { 100 };

        ShortStream stream = ShortStream.zip(a, b, c, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 22, 3, 4 }, stream.toArray());
    }

    @Test
    public void testZipThreeIteratorsWithDefaults() {
        ShortIterator it1 = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortIterator it2 = ShortIterator.of((short) 10);
        ShortIterator it3 = ShortIterator.of((short) 100, (short) 200);

        ShortStream stream = ShortStream.zip(it1, it2, it3, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 202, 3 }, stream.toArray());
    }

    @Test
    public void testZipThreeStreamsWithDefaults() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2);
        ShortStream s2 = ShortStream.of((short) 10, (short) 20, (short) 30);
        ShortStream s3 = ShortStream.of((short) 100);

        ShortStream stream = s1.zipWith(s2, s3, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 22, 30 }, stream.toArray());
    }
}
