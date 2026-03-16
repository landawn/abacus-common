package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.ShortNFunction;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings;

@Tag("2025")
public class ShortStreamTest extends TestBase {

    private ShortStream stream;
    private ShortStream stream2;
    private ShortStream emptyStream;
    private ShortStream singleElementStream;
    private ShortStream singleElementStream2;
    private ShortStream multiElementStream;
    private ShortStream multiElementStream2;
    private ShortStream multiElementStream3;

    @BeforeEach
    public void setUp() {
        stream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        stream2 = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        emptyStream = ShortStream.empty();
        singleElementStream = createShortStream((short) 5);
        singleElementStream2 = createShortStream((short) 5);
        multiElementStream = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        multiElementStream2 = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        multiElementStream3 = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
    }

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
        Stream<String> stream = ShortStream.of((short) 1, (short) 2).flatMapArrayToObj(n -> new String[] { "A" + n, "B" + n });
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
    public void testsummaryStatistics() {
        ShortSummaryStatistics stats = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).summaryStatistics();
        assertNotNull(stats);
        assertEquals(4, stats.getCount());
        assertEquals(10, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(4, stats.getMax());
        assertEquals(2.5, stats.getAverage(), 0.0001);
    }

    @Test
    public void testSummarizeEmpty() {
        ShortSummaryStatistics stats = ShortStream.empty().summaryStatistics();
        assertNotNull(stats);
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        Pair<ShortSummaryStatistics, Optional<Map<Percentage, Short>>> result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                .summaryStatisticsAndPercentiles();

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

    // ==================== debounce tests ====================

    @Test
    public void testDebounce_BasicFunctionality() {
        // Allow 3 elements per 1 second window
        short[] result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        // Only first 3 elements should pass through within the window
        assertEquals(3, result.length);
        assertEquals((short) 1, result[0]);
        assertEquals((short) 2, result[1]);
        assertEquals((short) 3, result[2]);
    }

    @Test
    public void testDebounce_AllElementsPassWhenWithinLimit() {
        // Allow 10 elements per window, but only 5 elements in stream
        short[] result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                .debounce(10, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        // All elements should pass
        assertEquals(5, result.length);
    }

    @Test
    public void testDebounce_EmptyStream() {
        short[] result = ShortStream.empty().debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertEquals(0, result.length);
    }

    @Test
    public void testDebounce_SingleElement() {
        short[] result = ShortStream.of((short) 42).debounce(1, com.landawn.abacus.util.Duration.ofMillis(100)).toArray();

        assertEquals(1, result.length);
        assertEquals((short) 42, result[0]);
    }

    @Test
    public void testDebounce_MaxWindowSizeOne() {
        // Only 1 element allowed per window
        short[] result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                .debounce(1, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals(1, result.length);
        assertEquals((short) 1, result[0]);
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveMaxWindowSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            ShortStream.of((short) 1, (short) 2, (short) 3).debounce(0, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ShortStream.of((short) 1, (short) 2, (short) 3).debounce(-1, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();
        });
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveDuration() {
        assertThrows(IllegalArgumentException.class, () -> {
            ShortStream.of((short) 1, (short) 2, (short) 3).debounce(5, com.landawn.abacus.util.Duration.ofMillis(0)).toArray();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ShortStream.of((short) 1, (short) 2, (short) 3).debounce(5, com.landawn.abacus.util.Duration.ofMillis(-100)).toArray();
        });
    }

    @Test
    public void testDebounce_WithLargeMaxWindowSize() {
        short[] input = new short[1000];
        for (int i = 0; i < 1000; i++) {
            input[i] = (short) i;
        }

        short[] result = ShortStream.of(input).debounce(500, com.landawn.abacus.util.Duration.ofSeconds(10)).toArray();

        assertEquals(500, result.length);
    }

    @Test
    public void testDebounce_PreservesOrder() {
        short[] result = ShortStream.of((short) 10, (short) 20, (short) 30, (short) 40, (short) 50)
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals((short) 10, result[0]);
        assertEquals((short) 20, result[1]);
        assertEquals((short) 30, result[2]);
    }

    @Test
    public void testDebounce_ChainedWithOtherOperations() {
        short[] result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7, (short) 8, (short) 9, (short) 10)
                .filter(n -> n % 2 == 0) // 2, 4, 6, 8, 10
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)) // 2, 4, 6
                .map(n -> (short) (n * 10)) // 20, 40, 60
                .toArray();

        assertEquals(3, result.length);
        assertEquals((short) 20, result[0]);
        assertEquals((short) 40, result[1]);
        assertEquals((short) 60, result[2]);
    }

    @Test
    public void testDebounce_WithMinMaxValues() {
        short[] result = ShortStream.of(Short.MIN_VALUE, (short) 0, Short.MAX_VALUE, (short) 1, (short) 2)
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals(3, result.length);
        assertEquals(Short.MIN_VALUE, result[0]);
        assertEquals((short) 0, result[1]);
        assertEquals(Short.MAX_VALUE, result[2]);
    }

    @Test
    public void testOfVarargs() {
        assertEquals(0, createShortStream().count());
        assertArrayEquals(new short[] { 1, 2, 3 }, createShortStream((short) 1, (short) 2, (short) 3).toArray());
    }

    @Test
    public void testOfBuffer() {
        ShortBuffer buffer = ShortBuffer.wrap(new short[] { 1, 2, 3, 4, 5 });
        buffer.position(1).limit(4);
        assertArrayEquals(new short[] { 2, 3, 4 }, createShortStream(buffer).toArray());
    }

    @Test
    public void testIterateWithPredicate() {
        short[] result = ShortStream.iterate((short) 1, i -> i < 10, i -> (short) (i * 2)).toArray();
        assertArrayEquals(new short[] { 1, 2, 4, 8 }, result);
    }

    @Test
    public void testIterateUnaryOperator() {
        short[] result = ShortStream.iterate((short) 1, i -> (short) (i + 1)).limit(5).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testConcat() {
        ShortStream s1 = createShortStream((short) 1, (short) 2);
        ShortStream s2 = createShortStream((short) 3, (short) 4);
        ShortStream s3 = createShortStream((short) 5, (short) 6);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, ShortStream.concat(s1, s2, s3).toArray());
    }

    @Test
    public void testZipWithDefault() {
        short[] a = { 1, 2 };
        short[] b = { 4, 5, 6, 7 };
        short[] result = ShortStream.zip(a, b, (short) 99, (short) 100, (x, y) -> (short) (x + y)).toArray();
        assertArrayEquals(new short[] { 5, 7, 105, 106 }, result);
    }

    @Test
    public void testScanWithInitialValue() {
        short[] result = multiElementStream.scan((short) 10, (a, b) -> (short) (a + b)).toArray();
        assertArrayEquals(new short[] { 11, 13, 16, 20, 25 }, result);
    }

    @Test
    public void testCycled() {
        short[] result = createShortStream((short) 1, (short) 2).cycled().limit(5).toArray();
        assertArrayEquals(new short[] { 1, 2, 1, 2, 1 }, result);
    }

    @Test
    public void testReduceWithIdentity() {
        short sum = multiElementStream.reduce((short) 0, (a, b) -> (short) (a + b));
        assertEquals(15, sum);
        short product = multiElementStream2.reduce((short) 1, (a, b) -> (short) (a * b));
        assertEquals(120, product);
    }

    @Test
    public void testFirst() {
        assertTrue(multiElementStream.first().isPresent());
        assertEquals((short) 1, multiElementStream2.first().get());
        assertFalse(emptyStream.first().isPresent());
    }

    @Test
    public void testLast() {
        assertTrue(multiElementStream.last().isPresent());
        assertEquals((short) 5, multiElementStream2.last().get());
        assertFalse(emptyStream.last().isPresent());
    }

    @Test
    public void testElementAt() {
        assertTrue(multiElementStream.elementAt(2).isPresent());
        assertEquals((short) 3, multiElementStream2.elementAt(2).get());
        assertFalse(multiElementStream3.elementAt(10).isPresent());
        assertFalse(emptyStream.elementAt(0).isPresent());
    }

    @Test
    public void testOnlyOne() {
        assertTrue(singleElementStream.onlyOne().isPresent());
        assertEquals((short) 5, singleElementStream2.onlyOne().get());
        assertFalse(emptyStream.onlyOne().isPresent());
        assertThrows(com.landawn.abacus.exception.TooManyElementsException.class, () -> multiElementStream.onlyOne());
    }

    @Test
    public void testReversed() {
        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 }, multiElementStream.reversed().toArray());
    }

    @Test
    public void testRotated() {
        assertArrayEquals(new short[] { 4, 5, 1, 2, 3 }, multiElementStream.rotated(2).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5, 1 }, multiElementStream2.rotated(-1).toArray());
    }

    @Test
    public void testShuffled() {
        short[] result = multiElementStream.shuffled(new Random(42)).toArray();
        short[] original = multiElementStream2.toArray();
        Arrays.sort(result);
        Arrays.sort(original);
        assertArrayEquals(original, result, "Shuffled stream should contain the same elements");
    }

    @Test
    public void testIntersection() {
        List<Short> other = List.of((short) 2, (short) 2, (short) 4, (short) 6);
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 4, (short) 4);
        short[] result = stream.intersection(other).toArray();
        assertArrayEquals(new short[] { 2, 4 }, result);
    }

    @Test
    public void testDifference() {
        List<Short> other = List.of((short) 2, (short) 4, (short) 6);
        short[] result = multiElementStream.difference(other).toArray();
        assertArrayEquals(new short[] { 1, 3, 5 }, result);
    }

    @Test
    public void testSymmetricDifference() {
        List<Short> other = List.of((short) 4, (short) 5, (short) 6, (short) 7);
        short[] result = multiElementStream.symmetricDifference(other).sorted().toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 6, 7 }, result);
    }

    @Test
    public void testJoin() {
        assertEquals("1, 2, 3, 4, 5", multiElementStream.join(", "));
        assertEquals("[1-2-3-4-5]", createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).join("-", "[", "]"));
    }

    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).toList());
        assertEquals(N.toList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).toArray());
        assertArrayEquals(new short[] { 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.toList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilterWithAction() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2, i -> {
        }).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2, i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2, i -> {
        }).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2, i -> {
        }).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2, i -> {
        }).toList());
        assertEquals(N.toList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2, i -> {
        }).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
        }).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
        }).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
                }).toList());
        assertEquals(N.toList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).count());
        assertEquals(1, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new short[] { 1, 2 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new short[] { 2 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList((short) 2), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).skip(1).toList());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).count());
        assertEquals(1, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new short[] { 1, 2 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new short[] { 2 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList((short) 2),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new short[] { 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhileWithAction() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3, i -> {
        }).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3, i -> {
        }).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3, i -> {
                }).toList());
        assertEquals(N.toList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3, i -> {
                }).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3, i -> {
                }).toList());
        assertEquals(N.toList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3, i -> {
                }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).toList());
        assertEquals(N.toList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new short[] { 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).toList());
        assertEquals(N.toList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().toArray());
        assertArrayEquals(new short[] { 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().toList());
        assertEquals(N.toList((short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().toArray());
        assertArrayEquals(new short[] { 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().toList());
        assertEquals(N.toList((short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().count());
        assertEquals(4, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 }, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().skip(1).toList());
        assertEquals(5, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().count());
        assertEquals(4, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().count());
        assertEquals(4, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().skip(1).count());
        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 }, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().toArray());
        assertArrayEquals(new short[] { 4, 3, 2, 1 }, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().skip(1).toArray());
        assertEquals(N.toList((short) 5, (short) 4, (short) 3, (short) 2, (short) 1),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().toList());
        assertEquals(N.toList((short) 4, (short) 3, (short) 2, (short) 1),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().skip(1).toList());
        assertEquals(5, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().count());
        assertEquals(4, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 },
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().toArray());
        assertArrayEquals(new short[] { 4, 3, 2, 1 },
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().skip(1).toArray());
        assertEquals(N.toList((short) 5, (short) 4, (short) 3, (short) 2, (short) 1),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().toList());
        assertEquals(N.toList((short) 4, (short) 3, (short) 2, (short) 1),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).skip(1).count());
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).toArray());
        assertArrayEquals(new short[] { 4, 6, 8, 10 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).skip(1).toArray());
        assertEquals(N.toList((short) 2, (short) 4, (short) 6, (short) 8, (short) 10),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).toList());
        assertEquals(N.toList((short) 4, (short) 6, (short) 8, (short) 10),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).skip(1).count());
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).toArray());
        assertArrayEquals(new short[] { 4, 6, 8, 10 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).skip(1).toArray());
        assertEquals(N.toList((short) 2, (short) 4, (short) 6, (short) 8, (short) 10),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).toList());
        assertEquals(N.toList((short) 4, (short) 6, (short) 8, (short) 10),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(6, ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).count());
        assertArrayEquals(new short[] { 1, 11, 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).toArray());
        assertArrayEquals(new short[] { 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).toArray());
        assertEquals(N.toList((short) 1, (short) 11, (short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).toList());
        assertEquals(N.toList((short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).toList());
        assertEquals(6, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).count());
        assertArrayEquals(new short[] { 1, 11, 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).toArray());
        assertArrayEquals(new short[] { 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).toArray());
        assertEquals(N.toList((short) 1, (short) 11, (short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).toList());
        assertEquals(N.toList((short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmap() {
        assertEquals(6, ShortStream.of((short) 1, (short) 2, (short) 3).flatmap(i -> new short[] { i, (short) (i + 10) }).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).flatmap(i -> new short[] { i, (short) (i + 10) }).skip(2).count());
        assertArrayEquals(new short[] { 1, 11, 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).flatmap(i -> new short[] { i, (short) (i + 10) }).toArray());
        assertArrayEquals(new short[] { 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).flatmap(i -> new short[] { i, (short) (i + 10) }).skip(2).toArray());
        assertEquals(N.toList((short) 1, (short) 11, (short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).flatmap(i -> new short[] { i, (short) (i + 10) }).toList());
        assertEquals(N.toList((short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).flatmap(i -> new short[] { i, (short) (i + 10) }).skip(2).toList());
        assertEquals(6, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatmap(i -> new short[] { i, (short) (i + 10) }).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatmap(i -> new short[] { i, (short) (i + 10) }).skip(2).count());
        assertArrayEquals(new short[] { 1, 11, 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatmap(i -> new short[] { i, (short) (i + 10) }).toArray());
        assertArrayEquals(new short[] { 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatmap(i -> new short[] { i, (short) (i + 10) }).skip(2).toArray());
        assertEquals(N.toList((short) 1, (short) 11, (short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatmap(i -> new short[] { i, (short) (i + 10) }).toList());
        assertEquals(N.toList((short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatmap(i -> new short[] { i, (short) (i + 10) }).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartial() {
        assertEquals(3,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .count());
        assertEquals(2,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .skip(1)
                        .count());
        assertArrayEquals(new short[] { 1, 3, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .toArray());
        assertArrayEquals(new short[] { 3, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((short) 1, (short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .toList());
        assertEquals(N.toList((short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .skip(1)
                        .toList());
        assertEquals(3,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .count());
        assertEquals(2,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .skip(1)
                        .count());
        assertArrayEquals(new short[] { 1, 3, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .toArray());
        assertArrayEquals(new short[] { 3, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((short) 1, (short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .toList());
        assertEquals(N.toList((short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterRangeMap() {
        assertEquals(3,
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7).rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).count());
        assertEquals(2,
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new short[] { 3, 9, 14 },
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7).rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 9, 14 },
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((short) 3, (short) 9, (short) 14),
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7).rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 9, (short) 14),
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toList());
        assertEquals(3,
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .map(e -> e)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .count());
        assertEquals(2,
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .map(e -> e)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new short[] { 3, 9, 14 },
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .map(e -> e)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .toArray());
        assertArrayEquals(new short[] { 9, 14 },
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .map(e -> e)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((short) 3, (short) 9, (short) 14),
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .map(e -> e)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .toList());
        assertEquals(N.toList((short) 9, (short) 14),
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .map(e -> e)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterCollapse() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 3, 5, 7 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 5, 7 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 5, (short) 7),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 5, (short) 7),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).skip(1).toList());
        assertEquals(3,
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).count());
        assertEquals(2,
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7)
                        .map(e -> e)
                        .collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new short[] { 3, 5, 7 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 5, 7 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7)
                        .map(e -> e)
                        .collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((short) 3, (short) 5, (short) 7),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 5, (short) 7),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7)
                        .map(e -> e)
                        .collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithTriPredicate() {
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).count());
        assertEquals(1,
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 3, 12 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 12 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 12),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 12),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).skip(1).toList());
        assertEquals(2,
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).map(e -> e).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).count());
        assertEquals(1,
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7)
                        .map(e -> e)
                        .collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new short[] { 3, 12 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).map(e -> e).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 12 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7)
                        .map(e -> e)
                        .collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((short) 3, (short) 12),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).map(e -> e).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 12),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7)
                        .map(e -> e)
                        .collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterScan() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInit() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 0, (a, b) -> (short) (a + b)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 0, (a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 0, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 0, (a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 0, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 0, (a, b) -> (short) (a + b)).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).count());
        assertEquals(4,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInitIncluded() {
        assertEquals(6, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 10, true, (a, b) -> (short) (a + b)).count());
        assertEquals(5,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 10, true, (a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 10, 11, 13, 16, 20, 25 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 10, true, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 11, 13, 16, 20, 25 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 10, true, (a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.toList((short) 10, (short) 11, (short) 13, (short) 16, (short) 20, (short) 25),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 10, true, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 11, (short) 13, (short) 16, (short) 20, (short) 25),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 10, true, (a, b) -> (short) (a + b)).skip(1).toList());
        assertEquals(6,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 10, true, (a, b) -> (short) (a + b)).count());
        assertEquals(5,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .scan((short) 10, true, (a, b) -> (short) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new short[] { 10, 11, 13, 16, 20, 25 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 10, true, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 11, 13, 16, 20, 25 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .scan((short) 10, true, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((short) 10, (short) 11, (short) 13, (short) 16, (short) 20, (short) 25),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 10, true, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 11, (short) 13, (short) 16, (short) 20, (short) 25),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .scan((short) 10, true, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterPrepend() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 10, 11, 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 11, 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.toList((short) 10, (short) 11, (short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).toList());
        assertEquals(N.toList((short) 11, (short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 10, 11, 1, 2, 3 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 11, 1, 2, 3 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.toList((short) 10, (short) 11, (short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).toList());
        assertEquals(N.toList((short) 11, (short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppend() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 10, 11 }, ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 2, 3, 10, 11 }, ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 10, (short) 11),
                ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 10, (short) 11),
                ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 10, 11 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 2, 3, 10, 11 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 10, (short) 11),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 10, (short) 11),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).toList());
        assertEquals(N.toList((short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 2, 3 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).toList());
        assertEquals(N.toList((short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTop() {
        assertEquals(3, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).skip(1).count());
        assertArrayEquals(new short[] { 3, 5, 4 }, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).toArray());
        assertArrayEquals(new short[] { 5, 4 }, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 5, (short) 4), ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).toList());
        assertEquals(N.toList((short) 5, (short) 4), ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).skip(1).count());
        assertArrayEquals(new short[] { 3, 5, 4 }, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).toArray());
        assertArrayEquals(new short[] { 5, 4 }, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 5, (short) 4),
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).toList());
        assertEquals(N.toList((short) 5, (short) 4), ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTopWithComparator() {
        assertEquals(3, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).skip(1).count());
        assertArrayEquals(new short[] { 3, 1, 2 },
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).toArray());
        assertArrayEquals(new short[] { 1, 2 },
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 1, (short) 2),
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).toList());
        assertEquals(N.toList((short) 1, (short) 2),
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).skip(1).count());
        assertArrayEquals(new short[] { 3, 1, 2 },
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).toArray());
        assertArrayEquals(new short[] { 1, 2 },
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 1, (short) 2),
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).toList());
        assertEquals(N.toList((short) 1, (short) 2),
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).toList());
        assertEquals(N.toList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).toList());
        assertEquals(N.toList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipWithAction() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, i -> {
        }).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, i -> {
        }).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, i -> {
        }).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, i -> {
        }).toList());
        assertEquals(N.toList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, i -> {
        }).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).toList());
        assertEquals(N.toList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterLimit() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).toArray());
        assertArrayEquals(new short[] { 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).toList());
        assertEquals(N.toList((short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).toArray());
        assertArrayEquals(new short[] { 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).toList());
        assertEquals(N.toList((short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterStep() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).toArray());
        assertArrayEquals(new short[] { 3, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).toList());
        assertEquals(N.toList((short) 3, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).toArray());
        assertArrayEquals(new short[] { 3, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).toList());
        assertEquals(N.toList((short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRateLimited() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRateLimitedWithRateLimiter() {
        RateLimiter rateLimiter = RateLimiter.create(100.0);
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(rateLimiter).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(rateLimiter).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(rateLimiter).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(rateLimiter).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(rateLimiter).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(rateLimiter).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDelay() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnEach() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).peek(i -> {
        }).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).peek(i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).peek(i -> {
        }).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).peek(i -> {
        }).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).peek(i -> {
                }).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).peek(i -> {
        }).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).peek(i -> {
        }).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).peek(i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).peek(i -> {
        }).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).peek(i -> {
        }).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).peek(i -> {
                }).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).peek(i -> {
                }).skip(1).toList());
    }

    @Test
    @DisplayName("Test of(short[], int, int) method")
    public void testOfArrayRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        ShortStream stream = createShortStream(array, 1, 4);
        assertArrayEquals(new short[] { 2, 3, 4 }, stream.toArray());
    }

    @Test
    @DisplayName("Test of(Short[]) method")
    public void testOfShortObjectArray() {
        Short[] array = { 1, 2, 3, 4, 5 };
        ShortStream stream = createShortStream(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    @DisplayName("Test flatten(short[][], boolean) method")
    public void testFlatten2DVertically() {
        short[][] array2D = { { 1, 2, 3 }, { 4, 5, 6 } };
        ShortStream stream = ShortStream.flatten(array2D, true);
        assertArrayEquals(new short[] { 1, 4, 2, 5, 3, 6 }, stream.toArray());
    }

    @Test
    @DisplayName("Test flatten(short[][], short, boolean) method with alignment")
    public void testFlatten2DWithAlignment() {
        short[][] array2D = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        ShortStream stream = ShortStream.flatten(array2D, (short) 0, false);
        assertArrayEquals(new short[] { 1, 2, 0, 3, 4, 5, 6, 0, 0 }, stream.toArray());
    }

    @Test
    @DisplayName("Test iterate() methods")
    public void testIterate() {
        final int[] counter = { 0 };
        ShortStream stream1 = ShortStream.iterate(() -> counter[0] < 5, () -> (short) (counter[0]++));
        assertArrayEquals(new short[] { 0, 1, 2, 3, 4 }, stream1.toArray());

        ShortStream stream2 = ShortStream.iterate((short) 1, () -> true, n -> (short) (n * 2)).limit(5);
        assertArrayEquals(new short[] { 1, 2, 4, 8, 16 }, stream2.toArray());

        ShortStream stream3 = ShortStream.iterate((short) 1, n -> n < 10, n -> (short) (n + 2));
        assertArrayEquals(new short[] { 1, 3, 5, 7, 9 }, stream3.toArray());

        ShortStream stream4 = ShortStream.iterate((short) 1, n -> (short) (n + 1)).limit(5);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream4.toArray());
    }

    @Test
    @DisplayName("Test filter() with action on dropped")
    public void testFilterWithAction() {
        List<Short> dropped = new ArrayList<>();
        ShortStream filtered = stream.filter(n -> n % 2 == 0, dropped::add);
        assertArrayEquals(new short[] { 2, 4 }, filtered.toArray());
        assertEquals(Arrays.asList((short) 1, (short) 3, (short) 5), dropped);
    }

    @Test
    @DisplayName("Test dropWhile() with action")
    public void testDropWhileWithAction() {
        List<Short> droppedItems = new ArrayList<>();
        ShortStream dropped = stream.dropWhile(n -> n < 4, droppedItems::add);
        assertArrayEquals(new short[] { 4, 5 }, dropped.toArray());
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), droppedItems);
    }

    @Test
    @DisplayName("Test skipUntil() method")
    public void testSkipUntil() {
        ShortStream skipped = stream.skipUntil(n -> n >= 3);
        assertArrayEquals(new short[] { 3, 4, 5 }, skipped.toArray());
    }

    @Test
    @DisplayName("Test flatmap() with array")
    public void testFlatmapArray() {
        ShortStream flattened = stream.flatmap(n -> new short[] { n, (short) (n * 10) });
        assertArrayEquals(new short[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 }, flattened.toArray());
    }

    @Test
    @DisplayName("Test flatmapToObj() with collection")
    public void testFlatmapToObjCollection() {
        Stream<String> flattened = stream.flatmapToObj(n -> Arrays.asList("A" + n, "B" + n));
        List<String> result = flattened.toList();
        assertEquals(Arrays.asList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"), result);
    }

    @Test
    @DisplayName("Test flatMapArrayToObj() with array")
    public void testFlattMapToObjArray() {
        Stream<String> flattened = stream.flatMapArrayToObj(n -> new String[] { "A" + n, "B" + n });
        List<String> result = flattened.toList();
        assertEquals(Arrays.asList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"), result);
    }

    @Test
    @DisplayName("Test sorting methods")
    public void testSorting() {
        ShortStream unsorted = createShortStream((short) 5, (short) 1, (short) 4, (short) 2, (short) 3);

        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, unsorted.sorted().toArray());

        unsorted = createShortStream((short) 5, (short) 1, (short) 4, (short) 2, (short) 3);

        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 }, createShortStream((short) 5, (short) 1, (short) 4, (short) 2, (short) 3).reverseSorted().toArray());
    }

    @Test
    @DisplayName("Test step() method")
    public void testStep() {
        assertArrayEquals(new short[] { 1, 3, 5 }, stream.step(2).toArray());
        assertArrayEquals(new short[] { 1, 4 }, stream2.step(3).toArray());
    }

    @Test
    @DisplayName("Test onEach() / peek() method")
    public void testOnEach() {
        List<Short> peeked = new ArrayList<>();
        short[] result = stream.peek(peeked::add).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), peeked);

        peeked.clear();
        result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).peek(peeked::add).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), peeked);
    }

    @Test
    @DisplayName("Test toImmutableList() method")
    public void testToImmutableList() {
        var list = stream.toImmutableList();
        assertEquals(5, list.size());
        assertThrows(UnsupportedOperationException.class, () -> list.add((short) 6));
    }

    @Test
    @DisplayName("Test toImmutableSet() method")
    public void testToImmutableSet() {
        var set = stream.toImmutableSet();
        assertEquals(5, set.size());
        assertTrue(set.contains((short) 1));
        assertThrows(UnsupportedOperationException.class, () -> set.add((short) 6));
    }

    @Test
    @DisplayName("Test toCollection() method")
    public void testToCollection() {
        LinkedList<Short> linkedList = stream.toCollection(LinkedList::new);
        assertEquals(5, linkedList.size());
        assertEquals((short) 1, linkedList.getFirst());
        assertEquals((short) 5, linkedList.getLast());
    }

    @Test
    @DisplayName("Test percentiles() method")
    public void testPercentiles() {
        Optional<Map<Percentage, Short>> percentiles = stream.percentiles();
        assertTrue(percentiles.isPresent());

        Map<Percentage, Short> map = percentiles.get();
        assertEquals((short) 3, map.get(Percentage._50));
        assertEquals((short) 5, map.get(Percentage._99_9999));
    }

    @Test
    @DisplayName("Test joinTo() method")
    public void testJoinTo() {
        Joiner joiner = Joiner.with(",", "[", "]");
        stream.joinTo(joiner);
        assertEquals("[1,2,3,4,5]", joiner.toString());
    }

    @Test
    @DisplayName("Test println() method")
    public void testPrintln() {
        assertDoesNotThrow(() -> createShortStream((short) 1, (short) 2, (short) 3).println());
    }

    @Test
    @DisplayName("Test throwIfEmpty() methods")
    public void testThrowIfEmpty() {
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream.throwIfEmpty().toArray());

        assertThrows(NoSuchElementException.class, () -> ShortStream.empty().throwIfEmpty().toArray());

        assertThrows(IllegalStateException.class, () -> ShortStream.empty().throwIfEmpty(() -> new IllegalStateException("Stream is empty")).toArray());
    }

    @Test
    @DisplayName("Test ifEmpty() method")
    public void testIfEmpty() {
        final boolean[] actionExecuted = { false };

        stream.ifEmpty(() -> actionExecuted[0] = true).toArray();
        assertFalse(actionExecuted[0]);

        ShortStream.empty().ifEmpty(() -> actionExecuted[0] = true).toArray();
        assertTrue(actionExecuted[0]);
    }

    @Test
    @DisplayName("Test applyIfNotEmpty() method")
    public void testApplyIfNotEmpty() {
        Optional<Long> count = stream.applyIfNotEmpty(s -> s.count());
        assertTrue(count.isPresent());
        assertEquals(5L, count.get());

        Optional<Long> emptyCount = ShortStream.empty().applyIfNotEmpty(s -> s.count());
        assertFalse(emptyCount.isPresent());
    }

    @Test
    @DisplayName("Test acceptIfNotEmpty() method")
    public void testAcceptIfNotEmpty() {
        final long[] count = { 0 };

        stream.acceptIfNotEmpty(s -> count[0] = s.count()).orElse(() -> count[0] = -1);
        assertEquals(5L, count[0]);

        count[0] = 0;
        ShortStream.empty().acceptIfNotEmpty(s -> count[0] = s.count()).orElse(() -> count[0] = -1);
        assertEquals(-1L, count[0]);
    }

    @Test
    @DisplayName("Test sequential() and parallel() methods")
    public void testSequentialParallel() {
        assertFalse(stream.isParallel());

        ShortStream parallelStream = stream.parallel();
        assertTrue(parallelStream.isParallel());

        ShortStream sequentialStream = parallelStream.sequential();
        assertFalse(sequentialStream.isParallel());

        ShortStream parallel2 = createShortStream((short) 1, (short) 2, (short) 3).parallel(2);
        assertTrue(parallel2.isParallel());
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
    public void testIterateWithBooleanSupplierAndInitialValue() {
        final int[] count = { 0 };
        ShortStream stream = ShortStream.iterate((short) 5, () -> count[0]++ < 3, n -> (short) (n + 1));
        assertArrayEquals(new short[] { 5, 6, 7 }, stream.toArray());
    }

    @Test
    public void testConcatArrayCollection() {
        List<short[]> arrays = Arrays.asList(new short[] { 1, 2 }, new short[] { 3, 4 }, new short[] { 5 });

        ShortStream stream = ShortStream.concat(arrays);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testConcatIterators2() {
        List<ShortIterator> iterators = Arrays.asList(ShortList.of((short) 1, (short) 2).iterator(), ShortList.of((short) 3, (short) 4).iterator());

        ShortStream stream = ShortStream.concatIterators(iterators);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
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

    @Test
    public void testMergeMultipleStreams() {
        List<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 4), createShortStream((short) 2, (short) 5),
                createShortStream((short) 3, (short) 6));

        ShortStream result = ShortStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, result.toArray());
    }

    @Test
    public void testFlattMapToObj() {
        Stream<String> stream = createShortStream((short) 1, (short) 2).flatMapArrayToObj(n -> new String[] { "A" + n, "B" + n });
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2" }, stream.toArray());
    }

    @Test
    public void testScanWithInitAndIncluded() {
        ShortStream stream1 = createShortStream((short) 1, (short) 2, (short) 3).scan((short) 10, true, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 10, 11, 13, 16 }, stream1.toArray());

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
    public void testAppendIfEmptySupplier() {
        ShortStream stream = ShortStream.empty().appendIfEmpty(() -> createShortStream((short) 10, (short) 20));
        assertArrayEquals(new short[] { 10, 20 }, stream.toArray());
    }

    @Test
    public void testDefaultIfEmpty() {
        ShortStream stream = ShortStream.empty().defaultIfEmpty(() -> createShortStream((short) 42));
        assertArrayEquals(new short[] { 42 }, stream.toArray());
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
    public void testToMultisetWithSupplier() {
        var multiset = createShortStream((short) 1, (short) 2, (short) 2, (short) 3).toMultiset(com.landawn.abacus.util.Multiset::new);
        assertEquals(1, multiset.count((short) 1));
        assertEquals(2, multiset.count((short) 2));
        assertEquals(1, multiset.count((short) 3));
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
        RateLimiter rateLimiter = RateLimiter.create(10);
        long startTime = System.currentTimeMillis();

        createShortStream((short) 1, (short) 2, (short) 3).rateLimited(rateLimiter).toArray();

        long elapsedTime = System.currentTimeMillis() - startTime;
        assertTrue(elapsedTime >= 200);
    }

    @Test
    public void testRateLimitedWithPermitsPerSecond() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        createShortStream((short) 1, (short) 2, (short) 3).rateLimited(10.0).toArray();

        long elapsedTime = System.currentTimeMillis() - startTime;
        assertTrue(elapsedTime >= 200);
    }

    @Test
    public void testDelay() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        createShortStream((short) 1, (short) 2, (short) 3).delay(Duration.ofMillis(50)).toArray();

        long elapsedTime = System.currentTimeMillis() - startTime;
        assertTrue(elapsedTime >= 100);
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
    public void testTransform() {
        IntStream result = createShortStream((short) 1, (short) 2, (short) 3).transform(s -> s.mapToInt(n -> n * 10));

        assertArrayEquals(new int[] { 10, 20, 30 }, result.toArray());
    }

    //    @Test
    //    public void testDoubleUnderscoreTransform() {
    //        IntStream result = createShortStream((short) 1, (short) 2, (short) 3).__(s -> s.mapToInt(n -> n * 10));
    //
    //        assertArrayEquals(new int[] { 10, 20, 30 }, result.toArray());
    //    }

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
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).parallel()
                .psp(s -> s.map(n -> (short) (n * 2)))
                .sequential()
                .toArray();

        Arrays.sort(result);
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
    }

    @Test
    public void testFlattenWithAlignmentVertically() {
        short[][] array = { { 1, 2 }, { 3 }, { 4, 5, 6 } };
        ShortStream stream = ShortStream.flatten(array, (short) 0, true);
        assertArrayEquals(new short[] { 1, 3, 4, 2, 0, 5, 0, 0, 6 }, stream.toArray());
    }

    @Test
    public void testOnlyOneWithMultipleElements() {
        assertThrows(TooManyElementsException.class, () -> createShortStream((short) 1, (short) 2).onlyOne());
    }

    @Test
    public void testJoinWithPrefixSuffix() {
        String result = createShortStream((short) 1, (short) 2, (short) 3).join(", ", "[", "]");
        assertEquals("[1, 2, 3]", result);
    }

    @Test
    public void testScanWithSeed() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3).scan((short) 10, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 11, 13, 16 }, stream.toArray());
    }

    @Test
    public void testFlatten() {
        short[][] array2d = { { 1, 2 }, { 3, 4 }, { 5 } };
        ShortStream stream1 = ShortStream.flatten(array2d);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream1.toArray());

        short[][][] array3d = { { { 1, 2 }, { 3 } }, { { 4, 5 } } };
        ShortStream stream2 = ShortStream.flatten(array3d);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream2.toArray());
    }

    // --- Missing dedicated test methods ---

    @Test
    public void testOfBoxedArrayWithRange() {
        Short[] boxed = { 10, 20, 30, 40, 50 };
        ShortStream stream = ShortStream.of(boxed, 1, 4);
        assertArrayEquals(new short[] { 20, 30, 40 }, stream.toArray());
    }

    @Test
    public void testOfNullShortBuffer() {
        ShortStream stream = ShortStream.of((ShortBuffer) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullShortIterator() {
        ShortStream stream = ShortStream.of((ShortIterator) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullableWithNonNull() {
        ShortStream stream = ShortStream.ofNullable((short) 42);
        assertEquals(1, stream.count());
    }

    @Test
    public void testOfNullableWithNull() {
        ShortStream stream = ShortStream.ofNullable(null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFlatMapArrayToObjDedicated() {
        ShortStream stream = createShortStream((short) 1, (short) 2);
        List<String> result = stream.flatMapArrayToObj(s -> new String[] { "x" + s, "y" + s }).toList();
        assertEquals(4, result.size());
        assertEquals("x1", result.get(0));
        assertEquals("y1", result.get(1));
    }

    @Test
    public void testFindFirstNoPredicate() {
        OptionalShort result = createShortStream((short) 7, (short) 8, (short) 9).findFirst();
        assertTrue(result.isPresent());
        assertEquals((short) 7, result.getAsShort());
    }

    @Test
    public void testFindFirstNoPredicate_EmptyStream() {
        OptionalShort result = ShortStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAnyNoPredicate() {
        OptionalShort result = createShortStream((short) 7, (short) 8).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAnyNoPredicate_EmptyStream() {
        OptionalShort result = ShortStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testEmptyStreamMin() {
        assertFalse(ShortStream.empty().min().isPresent());
    }

    @Test
    public void testEmptyStreamMax() {
        assertFalse(ShortStream.empty().max().isPresent());
    }

    @Test
    public void testEmptyStreamAverage() {
        assertFalse(ShortStream.empty().average().isPresent());
    }

    @Test
    public void testEmptyStreamSum() {
        assertEquals(0, ShortStream.empty().sum());
    }

    @Test
    public void testEmptyStreamReduce() {
        assertFalse(ShortStream.empty().reduce((a, b) -> (short) (a + b)).isPresent());
    }

    @Test
    public void testReduceWithIdentityOnEmpty() {
        short result = ShortStream.empty().reduce((short) 10, (a, b) -> (short) (a + b));
        assertEquals((short) 10, result);
    }

    @Test
    public void testEmptyStreamSummaryStatistics() {
        ShortSummaryStatistics stats = ShortStream.empty().summaryStatistics();
        assertNotNull(stats);
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testEmptyStreamSummaryStatisticsAndPercentiles() {
        Pair<ShortSummaryStatistics, Optional<Map<Percentage, Short>>> result = ShortStream.empty().summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertEquals(0, result.left().getCount());
    }

    @Test
    public void testLimitWithOffset() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(1, 3).toArray();
        assertArrayEquals(new short[] { 2, 3, 4 }, result);
    }

    @Test
    public void testRangeEmptyRange() {
        assertEquals(0, ShortStream.range((short) 5, (short) 5).count());
    }

    @Test
    public void testRangeClosedSingleElement() {
        assertArrayEquals(new short[] { 5 }, ShortStream.rangeClosed((short) 5, (short) 5).toArray());
    }

    @Test
    public void testRepeatZero() {
        ShortStream stream = ShortStream.repeat((short) 5, 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRepeatNegative() {
        assertThrows(IllegalArgumentException.class, () -> ShortStream.repeat((short) 5, -1));
    }

    @Test
    public void testDeferNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ShortStream.defer(null));
    }

    @Test
    public void testGenerateNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ShortStream.generate(null));
    }

    @Test
    public void testDefaultIfEmptyOnEmpty() {
        short[] result = ShortStream.empty().defaultIfEmpty(() -> ShortStream.of((short) 99)).toArray();
        assertArrayEquals(new short[] { 99 }, result);
    }

    @Test
    public void testDefaultIfEmptyOnNonEmpty() {
        short[] result = createShortStream((short) 1, (short) 2).defaultIfEmpty(() -> ShortStream.of((short) 99)).toArray();
        assertArrayEquals(new short[] { 1, 2 }, result);
    }

    @Test
    public void testPeekIsAliasForOnEach() {
        List<Short> peeked = new ArrayList<>();
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).peek(s -> peeked.add(s)).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
        assertEquals(3, peeked.size());
    }

    @Test
    public void testAppendIfEmptyWithSupplierOnEmpty() {
        short[] result = ShortStream.empty().appendIfEmpty(() -> ShortStream.of((short) 77)).toArray();
        assertArrayEquals(new short[] { 77 }, result);
    }

    @Test
    public void testAppendIfEmptyWithSupplierOnNonEmpty() {
        short[] result = createShortStream((short) 1).appendIfEmpty(() -> ShortStream.of((short) 77)).toArray();
        assertArrayEquals(new short[] { 1 }, result);
    }

    @Test
    public void testRateLimitedWithPermitsPerSecondDouble() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).rateLimited(1000.0).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testJoinWithDelimiterOnly() {
        String result = createShortStream((short) 1, (short) 2, (short) 3).join(", ");
        assertNotNull(result);
        assertTrue(result.contains("1"));
        assertTrue(result.contains("2"));
        assertTrue(result.contains("3"));
    }

    @Test
    public void testDelayWithJavaDuration() {
        ShortStream stream = createShortStream((short) 1, (short) 2).delay(java.time.Duration.ofMillis(1));
        assertArrayEquals(new short[] { 1, 2 }, stream.toArray());
    }

    @Test
    public void testConcatEmptyArrays() {
        ShortStream stream = ShortStream.concat(new short[0], new short[0]);
        assertEquals(0, stream.count());
    }

    @Test
    public void testZipWithThreeStreamsDedicated() {
        ShortStream a = createShortStream((short) 1, (short) 2);
        ShortStream b = ShortStream.of((short) 10, (short) 20);
        ShortStream c = ShortStream.of((short) 100);
        short[] result = a.zipWith(b, c, (x, y, z) -> (short) (x + y + z)).toArray();
        assertArrayEquals(new short[] { 111 }, result);
    }

    @Test
    public void testZipWithThreeStreamsWithDefaultsDedicated() {
        ShortStream a = createShortStream((short) 1, (short) 2);
        ShortStream b = ShortStream.of((short) 10);
        ShortStream c = ShortStream.of((short) 100);
        short[] result = a.zipWith(b, c, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z)).toArray();
        assertArrayEquals(new short[] { 111, 2 }, result);
    }

    @Test
    public void testMergeWithDedicated() {
        ShortStream a = createShortStream((short) 1, (short) 3, (short) 5);
        ShortStream b = ShortStream.of((short) 2, (short) 4, (short) 6);
        short[] result = a.mergeWith(b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testToMultisetDefault() {
        Multiset<Short> multiset = createShortStream((short) 1, (short) 2, (short) 1).toMultiset();
        assertEquals(2, multiset.get((short) 1));
        assertEquals(1, multiset.get((short) 2));
    }

    @Test
    public void testLimitWithOffset_ZeroOffset() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(0, 3).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testLimitWithOffset_OffsetBeyondSize() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).limit(10, 5).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testPrependOptional_Empty() {
        short[] result = createShortStream((short) 2, (short) 3).prepend(OptionalShort.empty()).toArray();
        assertArrayEquals(new short[] { 2, 3 }, result);
    }

    @Test
    public void testAppendStreamMethod() {
        short[] result = createShortStream((short) 1, (short) 2).append(ShortStream.of((short) 3, (short) 4)).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testAppendOptionalMethod() {
        short[] result = createShortStream((short) 1, (short) 2).append(OptionalShort.of((short) 3)).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testAppendOptional_Empty() {
        short[] result = createShortStream((short) 1, (short) 2).append(OptionalShort.empty()).toArray();
        assertArrayEquals(new short[] { 1, 2 }, result);
    }

    @Test
    public void testAppendIfEmptySupplierMethod() {
        short[] result = ShortStream.empty().appendIfEmpty(() -> ShortStream.of((short) 10, (short) 20)).toArray();
        assertArrayEquals(new short[] { 10, 20 }, result);
    }

    @Test
    public void testAppendIfEmptySupplier_NonEmpty() {
        short[] result = createShortStream((short) 1, (short) 2).appendIfEmpty(() -> ShortStream.of((short) 10, (short) 20)).toArray();
        assertArrayEquals(new short[] { 1, 2 }, result);
    }

    @Test
    public void testDefaultIfEmptyMethod() {
        short[] result = ShortStream.empty().defaultIfEmpty(() -> ShortStream.of((short) 99)).toArray();
        assertArrayEquals(new short[] { 99 }, result);
    }

    @Test
    public void testDefaultIfEmpty_NonEmpty() {
        short[] result = createShortStream((short) 1, (short) 2).defaultIfEmpty(() -> ShortStream.of((short) 99)).toArray();
        assertArrayEquals(new short[] { 1, 2 }, result);
    }

    @Test
    public void testTakeWhile_AllMatch() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).takeWhile(s -> s < 10).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testTakeWhile_NoneMatch() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).takeWhile(s -> s < 0).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testTakeWhile_Empty() {
        short[] result = ShortStream.empty().takeWhile(s -> s < 10).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testDropWhile_AllMatch() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).dropWhile(s -> s < 10).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testDropWhile_NoneMatch() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).dropWhile(s -> s > 10).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testDropWhile_Empty() {
        short[] result = ShortStream.empty().dropWhile(s -> s < 10).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMap_Empty() {
        short[] result = ShortStream.empty().map(s -> (short) (s * 2)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToInt_Empty() {
        int[] result = ShortStream.empty().mapToInt(s -> (int) s).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToObj_Empty() {
        List<String> result = ShortStream.empty().mapToObj(String::valueOf).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatMap_Empty() {
        short[] result = ShortStream.empty().flatMap(s -> ShortStream.of(s, (short) (s * 2))).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmap_Empty() {
        short[] result = ShortStream.empty().flatmap(s -> new short[] { s, (short) (s * 2) }).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatMapToInt_Empty() {
        int[] result = ShortStream.empty().flatMapToInt(s -> IntStream.of((int) s)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatMapToObj_Empty() {
        List<String> result = ShortStream.empty().flatMapToObj(s -> Stream.of(String.valueOf(s))).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapPartial_AllEmpty() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).mapPartial(s -> OptionalShort.empty()).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapPartial_AllPresent() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).mapPartial(s -> OptionalShort.of((short) (s * 10))).toArray();
        assertArrayEquals(new short[] { 10, 20, 30 }, result);
    }

    @Test
    public void testMapPartial_Empty() {
        short[] result = ShortStream.empty().mapPartial(s -> OptionalShort.of(s)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testRangeMap_SingleElement() {
        short[] result = createShortStream((short) 5).rangeMap((first, next) -> next - first == 1, (first, last) -> (short) (first + last)).toArray();
        assertArrayEquals(new short[] { 10 }, result);
    }

    @Test
    public void testRangeMap_Empty() {
        short[] result = ShortStream.empty().rangeMap((first, next) -> next - first == 1, (first, last) -> (short) (first + last)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testRangeMapToObj_Empty() {
        List<String> result = ShortStream.empty().rangeMapToObj((first, next) -> next - first == 1, (first, last) -> first + "-" + last).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollapse_Empty() {
        List<ShortList> result = ShortStream.empty().collapse((a, b) -> b - a == 1).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollapse_SingleElement() {
        List<ShortList> result = createShortStream((short) 5).collapse((a, b) -> b - a == 1).toList();
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).size());
    }

    @Test
    public void testCollapseWithMerge_Empty() {
        short[] result = ShortStream.empty().collapse((a, b) -> b - a == 1, (a, b) -> (short) (a + b)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testCollapseWithTriPredicate_Empty() {
        short[] result = ShortStream.empty().collapse((first, last, next) -> next - first < 5, (a, b) -> (short) (a + b)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testScan_Empty() {
        short[] result = ShortStream.empty().scan((a, b) -> (short) (a + b)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testScanWithInit_Empty() {
        short[] result = ShortStream.empty().scan((short) 10, (a, b) -> (short) (a + b)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testScanWithInitIncluded_Empty() {
        short[] result = ShortStream.empty().scan((short) 10, true, (a, b) -> (short) (a + b)).toArray();
        assertArrayEquals(new short[] { 10 }, result);
    }

    @Test
    public void testPrepend_Empty() {
        short[] result = ShortStream.empty().prepend((short) 1, (short) 2).toArray();
        assertArrayEquals(new short[] { 1, 2 }, result);
    }

    @Test
    public void testAppend_Empty() {
        short[] result = ShortStream.empty().append((short) 1, (short) 2).toArray();
        assertArrayEquals(new short[] { 1, 2 }, result);
    }

    @Test
    public void testDistinct_AllSame() {
        short[] result = createShortStream((short) 5, (short) 5, (short) 5).distinct().toArray();
        assertArrayEquals(new short[] { 5 }, result);
    }

    @Test
    public void testDistinct_Empty() {
        short[] result = ShortStream.empty().distinct().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testSorted_Empty() {
        short[] result = ShortStream.empty().sorted().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testSorted_AlreadySorted() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).sorted().toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testReverseSorted_Empty() {
        short[] result = ShortStream.empty().reverseSorted().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testReversed_Empty() {
        short[] result = ShortStream.empty().reversed().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testReversed_SingleElement() {
        short[] result = createShortStream((short) 5).reversed().toArray();
        assertArrayEquals(new short[] { 5 }, result);
    }

    @Test
    public void testRotated_Zero() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).rotated(0).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testRotated_Negative() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).rotated(-1).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testShuffled_Empty() {
        short[] result = ShortStream.empty().shuffled().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testShuffledWithRandom() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).shuffled(new Random(42)).toArray();
        assertEquals(5, result.length);
    }

    @Test
    public void testIntersection_Empty() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).intersection(java.util.Collections.emptyList()).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testDifference_Empty() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).difference(java.util.Collections.emptyList()).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testSymmetricDifference_Empty() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).symmetricDifference(java.util.Collections.emptyList()).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testTop_MoreThanAvailable() {
        short[] result = createShortStream((short) 1, (short) 2).top(5).toArray();
        assertEquals(2, result.length);
    }

    @Test
    public void testTop_Empty() {
        short[] result = ShortStream.empty().top(3).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testCycled_WithRoundsZero() {
        short[] result = createShortStream((short) 1, (short) 2).cycled(0).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testCycled_Empty() {
        short[] result = ShortStream.empty().cycled().limit(0).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testIndexed() {
        List<com.landawn.abacus.util.IndexedShort> result = createShortStream((short) 10, (short) 20, (short) 30).indexed().toList();
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).index());
        assertEquals(10, result.get(0).value());
        assertEquals(2, result.get(2).index());
        assertEquals(30, result.get(2).value());
    }

    @Test
    public void testIndexed_Empty() {
        List<com.landawn.abacus.util.IndexedShort> result = ShortStream.empty().indexed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEach_Empty() {
        List<Short> collected = new ArrayList<>();
        ShortStream.empty().forEach(collected::add);
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testForEachIndexed_Empty() {
        List<Short> collected = new ArrayList<>();
        ShortStream.empty().forEachIndexed((idx, val) -> collected.add(val));
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testToShortList_Empty() {
        ShortList result = ShortStream.empty().toShortList();
        assertEquals(0, result.size());
    }

    @Test
    public void testToList_Empty() {
        List<Short> result = ShortStream.empty().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToSet_Empty() {
        java.util.Set<Short> result = ShortStream.empty().toSet();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToMap_DuplicateKeys() {
        assertThrows(Exception.class, () -> createShortStream((short) 1, (short) 1).toMap(s -> s, s -> s));
    }

    @Test
    public void testToMap_Empty() {
        Map<Short, Short> result = ShortStream.empty().toMap(s -> s, s -> s);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGroupTo_Empty() {
        Map<Boolean, List<Short>> result = ShortStream.empty().boxed().groupTo(s -> s > 2);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testReduce_SingleElement() {
        OptionalShort result = createShortStream((short) 5).reduce((a, b) -> (short) (a + b));
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testCollect_Empty() {
        List<Short> result = ShortStream.empty().collect(ArrayList::new, (list, s) -> list.add(s));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollectWithCombiner_Empty() {
        List<Short> result = ShortStream.empty().collect(ArrayList::new, (list, s) -> list.add(s), (l1, l2) -> l1.addAll(l2));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFirst_NonEmpty() {
        OptionalShort result = createShortStream((short) 10, (short) 20, (short) 30).first();
        assertTrue(result.isPresent());
        assertEquals(10, result.get());
    }

    @Test
    public void testFirst_Empty() {
        OptionalShort result = ShortStream.empty().first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast_NonEmpty() {
        OptionalShort result = createShortStream((short) 10, (short) 20, (short) 30).last();
        assertTrue(result.isPresent());
        assertEquals(30, result.get());
    }

    @Test
    public void testLast_Empty() {
        OptionalShort result = ShortStream.empty().last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testElementAt_Valid() {
        OptionalShort result = createShortStream((short) 10, (short) 20, (short) 30).elementAt(1);
        assertTrue(result.isPresent());
        assertEquals(20, result.get());
    }

    @Test
    public void testElementAt_OutOfRange() {
        OptionalShort result = createShortStream((short) 10, (short) 20).elementAt(5);
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_Empty() {
        OptionalShort result = ShortStream.empty().onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testAnyMatch_Empty() {
        assertFalse(ShortStream.empty().anyMatch(s -> true));
    }

    @Test
    public void testAllMatch_Empty() {
        assertTrue(ShortStream.empty().allMatch(s -> false));
    }

    @Test
    public void testNoneMatch_True() {
        assertTrue(createShortStream((short) 1, (short) 2, (short) 3).noneMatch(s -> s > 10));
    }

    @Test
    public void testNoneMatch_False() {
        assertFalse(createShortStream((short) 1, (short) 2, (short) 3).noneMatch(s -> s > 2));
    }

    @Test
    public void testFindFirst_Empty() {
        OptionalShort result = ShortStream.empty().findFirst(s -> s > 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAny_EmptyStream() {
        OptionalShort result = ShortStream.empty().findAny(s -> s > 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLast_EmptyStream() {
        OptionalShort result = ShortStream.empty().findLast(s -> s > 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLast_NonEmpty() {
        OptionalShort result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).findLast(s -> s < 4);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    public void testMin_SingleElement() {
        OptionalShort result = createShortStream((short) 5).min();
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testMax_SingleElement() {
        OptionalShort result = createShortStream((short) 5).max();
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testKthLargest_SingleElement() {
        OptionalShort result = createShortStream((short) 5).kthLargest(1);
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testAsIntStream_Empty() {
        int[] result = ShortStream.empty().asIntStream().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testBoxed_Empty() {
        List<Short> result = ShortStream.empty().boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMergeWith_Empty() {
        short[] result = createShortStream((short) 1, (short) 3, (short) 5)
                .mergeWith(ShortStream.empty(), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new short[] { 1, 3, 5 }, result);
    }

    @Test
    public void testZipWith_DifferentLengths() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).zipWith(ShortStream.of((short) 10, (short) 20), (a, b) -> (short) (a + b))
                .toArray();
        assertArrayEquals(new short[] { 11, 22 }, result);
    }

    @Test
    public void testZipWithPadding_DifferentLengths() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3)
                .zipWith(ShortStream.of((short) 10, (short) 20), (short) 0, (short) 0, (a, b) -> (short) (a + b))
                .toArray();
        assertArrayEquals(new short[] { 11, 22, 3 }, result);
    }

    @Test
    public void testThrowIfEmpty_WithSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ShortStream.empty().throwIfEmpty(() -> new IllegalArgumentException("empty")).toArray());
    }

    @Test
    public void testThrowIfEmpty_NotEmpty() {
        assertDoesNotThrow(() -> createShortStream((short) 1).throwIfEmpty(() -> new RuntimeException("empty")).toArray());
    }

    @Test
    public void testIfEmpty_NotEmpty() {
        AtomicInteger counter = new AtomicInteger(0);
        createShortStream((short) 1, (short) 2).ifEmpty(counter::incrementAndGet).toArray();
        assertEquals(0, counter.get());
    }

    @Test
    public void testIfEmpty_Empty() {
        AtomicInteger counter = new AtomicInteger(0);
        ShortStream.empty().ifEmpty(counter::incrementAndGet).toArray();
        assertEquals(1, counter.get());
    }

    @Test
    public void testJoinWithPrefixSuffixMethod() {
        String result = createShortStream((short) 1, (short) 2, (short) 3).join(", ", "[", "]");
        assertEquals("[1, 2, 3]", result);
    }

    @Test
    public void testJoin_Empty() {
        String result = ShortStream.empty().join(", ");
        assertEquals("", result);
    }

    @Test
    public void testJoinToMethod() {
        Joiner joiner = Joiner.with(", ", "[", "]");
        createShortStream((short) 1, (short) 2, (short) 3).joinTo(joiner);
        String result = joiner.toString();
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("1"));
    }

    @Test
    public void testOnClose_Multiple() {
        AtomicInteger counter = new AtomicInteger(0);
        ShortStream stream = createShortStream((short) 1, (short) 2).onClose(counter::incrementAndGet).onClose(counter::incrementAndGet);
        stream.close();
        assertEquals(2, counter.get());
    }

    @Test
    public void testIterator_HasNextAndNext() {
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
    public void testGenerate_Null() {
        assertThrows(IllegalArgumentException.class, () -> ShortStream.generate(null));
    }

    @Test
    public void testDefer_Null() {
        assertThrows(IllegalArgumentException.class, () -> ShortStream.defer(null));
    }

    @Test
    public void testRange_Descending() {
        short[] result = ShortStream.range((short) 5, (short) 1).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testRange_Same() {
        short[] result = ShortStream.range((short) 3, (short) 3).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testRangeClosed_Same() {
        short[] result = ShortStream.rangeClosed((short) 3, (short) 3).toArray();
        assertArrayEquals(new short[] { 3 }, result);
    }

    @Test
    public void testRangeClosed_Descending() {
        short[] result = ShortStream.rangeClosed((short) 5, (short) 1).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testSummaryStatistics_SingleElement() {
        ShortSummaryStatistics stats = createShortStream((short) 42).summaryStatistics();
        assertEquals(1, stats.getCount());
        assertEquals(42, stats.getMin());
        assertEquals(42, stats.getMax());
        assertEquals(42, stats.getSum());
    }

    @Test
    public void testSummaryStatisticsAndPercentiles_Empty() {
        Pair<ShortSummaryStatistics, Optional<Map<Percentage, Short>>> result = ShortStream.empty().summaryStatisticsAndPercentiles();
        assertEquals(0, result.getLeft().getCount());
        assertFalse(result.getRight().isPresent());
    }

    @Test
    public void testPercentiles_Empty() {
        Optional<Map<Percentage, Short>> result = ShortStream.empty().percentiles();
        assertFalse(result.isPresent());
    }

    @Test
    public void testTransformMethod() {
        long count = createShortStream((short) 1, (short) 2, (short) 3).transform(s -> s.filter(v -> v > 1)).count();
        assertEquals(2, count);
    }

    @Test
    public void testApplyIfNotEmptyMethod() {
        Optional<short[]> result = createShortStream((short) 1, (short) 2, (short) 3).applyIfNotEmpty(s -> s.toArray());
        assertTrue(result.isPresent());
        assertArrayEquals(new short[] { 1, 2, 3 }, result.get());
    }

    @Test
    public void testApplyIfNotEmpty_EmptyStream() {
        Optional<short[]> result = ShortStream.empty().applyIfNotEmpty(s -> s.toArray());
        assertFalse(result.isPresent());
    }

    @Test
    public void testAcceptIfNotEmptyMethod() {
        List<Short> collected = new ArrayList<>();
        createShortStream((short) 1, (short) 2, (short) 3).acceptIfNotEmpty(s -> s.forEach(collected::add));
        assertEquals(3, collected.size());
    }

    @Test
    public void testAcceptIfNotEmpty_EmptyStream() {
        List<Short> collected = new ArrayList<>();
        ShortStream.empty().acceptIfNotEmpty(s -> s.forEach(collected::add));
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testPrintlnMethod() {
        assertDoesNotThrow(() -> createShortStream((short) 1, (short) 2, (short) 3).println());
    }

    @Test
    public void testPrintln_EmptyStream() {
        assertDoesNotThrow(() -> ShortStream.empty().println());
    }

    @Test
    public void testToMultisetWithSupplierMethod() {
        Multiset<Short> result = createShortStream((short) 1, (short) 2, (short) 2, (short) 3, (short) 3, (short) 3).toMultiset(Suppliers.ofMultiset());
        assertEquals(1, result.get((short) 1));
        assertEquals(2, result.get((short) 2));
        assertEquals(3, result.get((short) 3));
    }

    @Test
    public void testSpsMethod() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).sps(s -> s.filter(v -> v > 2)).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testSpsWithMaxThreadNumMethod() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).sps(2, s -> s.filter(v -> v > 2)).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testSpsWithExecutorMethod() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).sps(2, executor, s -> s.filter(v -> v > 2)).toArray();
            assertEquals(3, result.length);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testPspMethod2() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).psp(s -> s.filter(v -> v > 2)).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testCount_Empty() {
        assertEquals(0, ShortStream.empty().count());
    }

    @Test
    public void testSum_SingleElement() {
        assertEquals(42, createShortStream((short) 42).sum());
    }

    @Test
    public void testAverage_SingleElement() {
        OptionalDouble result = createShortStream((short) 42).average();
        assertTrue(result.isPresent());
        assertEquals(42.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testFlatMapArrayToObjMethod() {
        List<String> result = createShortStream((short) 1, (short) 2).flatMapArrayToObj(s -> new String[] { "A" + s, "B" + s }).toList();
        assertEquals(4, result.size());
        assertEquals("A1", result.get(0));
        assertEquals("B1", result.get(1));
    }

    @Test
    public void testFlatMapArrayToObj_Empty() {
        List<String> result = ShortStream.empty().flatMapArrayToObj(s -> new String[] { "A" + s }).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatmapToObjMethod() {
        List<String> result = createShortStream((short) 1, (short) 2).flatmapToObj(s -> Arrays.asList("X" + s, "Y" + s)).toList();
        assertEquals(4, result.size());
        assertEquals("X1", result.get(0));
        assertEquals("Y1", result.get(1));
    }

    @Test
    public void testStep_StepOne() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).step(1).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testStep_StepTwo() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).step(2).toArray();
        assertArrayEquals(new short[] { 1, 3, 5 }, result);
    }

    @Test
    public void testCollectWithSupplierAccumulatorMethod() {
        List<Short> result = createShortStream((short) 1, (short) 2, (short) 3).collect(ArrayList::new, (list, s) -> list.add(s));
        assertEquals(3, result.size());
    }

    @Test
    public void testToMapWithFactoryMethod() {
        LinkedHashMap<Short, Short> result = createShortStream((short) 1, (short) 2, (short) 3).toMap(s -> s, s -> (short) (s * 10), (a, b) -> a,
                LinkedHashMap::new);
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(3, result.size());
    }

    @Test
    public void testGroupToWithFactoryMethod() {
        Map<Boolean, List<Short>> result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4).boxed()
                .groupTo(s -> s > 2, Suppliers.ofLinkedHashMap());
        assertTrue(result instanceof LinkedHashMap);
        assertTrue(result.containsKey(true));
        assertTrue(result.containsKey(false));
    }

    @Test
    public void testSkipWithAction_Method() {
        List<Short> skipped = new ArrayList<>();
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, skipped::add).toArray();
        assertArrayEquals(new short[] { 3, 4, 5 }, result);
        assertEquals(2, skipped.size());
    }

    @Test
    public void testFilterWithAction_Method() {
        List<Short> dropped = new ArrayList<>();
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(s -> s > 3, dropped::add).toArray();
        assertArrayEquals(new short[] { 4, 5 }, result);
        assertEquals(3, dropped.size());
    }

    @Test
    public void testDropWhileWithAction_Method() {
        List<Short> dropped = new ArrayList<>();
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(s -> s < 3, dropped::add).toArray();
        assertArrayEquals(new short[] { 3, 4, 5 }, result);
        assertEquals(2, dropped.size());
    }

    @Test
    public void testSkipUntil_Method() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(s -> s >= 3).toArray();
        assertArrayEquals(new short[] { 3, 4, 5 }, result);
    }

    @Test
    public void testSkipUntil_NoneMatch() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).skipUntil(s -> s > 10).toArray();
        assertEquals(0, result.length);
    }

}
