package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Joiner;
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

@Tag("new-test")
public class ShortStream102Test extends TestBase {

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

    private short[] testArray;
    private ShortStream stream;
    private ShortStream stream2;
    private ShortStream stream3;
    private ShortStream stream4;

    @BeforeEach
    public void setUp() {
        testArray = new short[] { 1, 2, 3, 4, 5 };
        stream = createStream(testArray);
        stream2 = createStream(testArray);
        stream3 = createStream(testArray);
        stream4 = createStream(testArray);
    }

    protected ShortStream createStream(short[] array) {
        return createShortStream(array);
    }

    @Test
    @DisplayName("Test empty() method")
    public void testEmpty() {
        ShortStream emptyStream = ShortStream.empty();
        assertEquals(0, emptyStream.count());
        assertFalse(ShortStream.empty().first().isPresent());
    }

    @Test
    @DisplayName("Test defer() method")
    public void testDefer() {
        Supplier<ShortStream> supplier = () -> createShortStream((short) 1, (short) 2, (short) 3);
        ShortStream deferredStream = ShortStream.defer(supplier);
        assertArrayEquals(new short[] { 1, 2, 3 }, deferredStream.toArray());
    }

    @Test
    @DisplayName("Test ofNullable() method")
    public void testOfNullable() {
        ShortStream stream1 = ShortStream.ofNullable((short) 5);
        assertEquals(1, stream1.count());

        ShortStream stream2 = ShortStream.ofNullable(null);
        assertEquals(0, stream2.count());
    }

    @Test
    @DisplayName("Test of(short...) method")
    public void testOfVarargs() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 3);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());

        ShortStream emptyStream = createShortStream();
        assertEquals(0, emptyStream.count());
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
    @DisplayName("Test of(Collection<Short>) method")
    public void testOfCollection() {
        List<Short> list = Arrays.asList((short) 1, (short) 2, (short) 3);
        ShortStream stream = createShortStream(list);
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    @DisplayName("Test of(ShortIterator) method")
    public void testOfIterator() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortStream stream = createShortStream(list.iterator());
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    @DisplayName("Test of(ShortBuffer) method")
    public void testOfShortBuffer() {
        ShortBuffer buffer = ShortBuffer.wrap(new short[] { 1, 2, 3, 4, 5 });
        buffer.position(1);
        buffer.limit(4);
        ShortStream stream = createShortStream(buffer);
        assertArrayEquals(new short[] { 2, 3, 4 }, stream.toArray());
    }

    @Test
    @DisplayName("Test flatten(short[][]) method")
    public void testFlatten2D() {
        short[][] array2D = { { 1, 2 }, { 3, 4 }, { 5 } };
        ShortStream stream = ShortStream.flatten(array2D);
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
    @DisplayName("Test flatten(short[][][]) method")
    public void testFlatten3D() {
        short[][][] array3D = { { { 1, 2 }, { 3 } }, { { 4, 5, 6 } } };
        ShortStream stream = ShortStream.flatten(array3D);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    @DisplayName("Test range(short, short) method")
    public void testRange() {
        ShortStream stream = ShortStream.range((short) 1, (short) 5);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());

        ShortStream emptyStream = ShortStream.range((short) 5, (short) 5);
        assertEquals(0, emptyStream.count());
    }

    @Test
    @DisplayName("Test range(short, short, short) method")
    public void testRangeWithStep() {
        ShortStream stream = ShortStream.range((short) 1, (short) 10, (short) 3);
        assertArrayEquals(new short[] { 1, 4, 7 }, stream.toArray());

        ShortStream negativeStep = ShortStream.range((short) 10, (short) 1, (short) -3);
        assertArrayEquals(new short[] { 10, 7, 4 }, negativeStep.toArray());
    }

    @Test
    @DisplayName("Test rangeClosed(short, short) method")
    public void testRangeClosed() {
        ShortStream stream = ShortStream.rangeClosed((short) 1, (short) 5);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    @DisplayName("Test rangeClosed(short, short, short) method")
    public void testRangeClosedWithStep() {
        ShortStream stream = ShortStream.rangeClosed((short) 1, (short) 9, (short) 3);
        assertArrayEquals(new short[] { 1, 4, 7 }, stream.toArray());
    }

    @Test
    @DisplayName("Test repeat(short, long) method")
    public void testRepeat() {
        ShortStream stream = ShortStream.repeat((short) 7, 3);
        assertArrayEquals(new short[] { 7, 7, 7 }, stream.toArray());

        ShortStream emptyStream = ShortStream.repeat((short) 7, 0);
        assertEquals(0, emptyStream.count());
    }

    @Test
    @DisplayName("Test random() method")
    public void testRandom() {
        ShortStream stream = ShortStream.random().limit(100);
        short[] array = stream.toArray();
        assertEquals(100, array.length);
        for (short value : array) {
            assertTrue(value >= Short.MIN_VALUE && value <= Short.MAX_VALUE);
        }
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
    @DisplayName("Test generate() method")
    public void testGenerate() {
        final short[] value = { 0 };
        ShortStream stream = ShortStream.generate(() -> value[0]++).limit(5);
        assertArrayEquals(new short[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    @DisplayName("Test concat() methods")
    public void testConcat() {
        ShortStream stream1 = ShortStream.concat(new short[] { 1, 2 }, new short[] { 3, 4 }, new short[] { 5 });
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream1.toArray());

        ShortIterator iter1 = ShortList.of((short) 1, (short) 2).iterator();
        ShortIterator iter2 = ShortList.of((short) 3, (short) 4).iterator();
        ShortStream stream2 = ShortStream.concat(iter1, iter2);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream2.toArray());

        ShortStream s1 = createShortStream((short) 1, (short) 2);
        ShortStream s2 = createShortStream((short) 3, (short) 4);
        ShortStream s3 = createShortStream((short) 5);
        ShortStream stream3 = ShortStream.concat(s1, s2, s3);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream3.toArray());

        List<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 2), createShortStream((short) 3, (short) 4));
        ShortStream stream4 = ShortStream.concat(streams);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream4.toArray());
    }

    @Test
    @DisplayName("Test zip() methods")
    public void testZip() {
        short[] a1 = { 1, 2, 3 };
        short[] a2 = { 4, 5, 6 };
        ShortStream stream1 = ShortStream.zip(a1, a2, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 5, 7, 9 }, stream1.toArray());

        short[] a3 = { 7, 8, 9 };
        ShortStream stream2 = ShortStream.zip(a1, a2, a3, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 12, 15, 18 }, stream2.toArray());

        short[] b1 = { 1, 2, 3, 4 };
        short[] b2 = { 5, 6 };
        ShortStream stream3 = ShortStream.zip(b1, b2, (short) 0, (short) 10, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 6, 8, 13, 14 }, stream3.toArray());
    }

    @Test
    @DisplayName("Test merge() methods")
    public void testMerge() {
        short[] a1 = { 1, 3, 5 };
        short[] a2 = { 2, 4, 6 };
        ShortStream stream = ShortStream.merge(a1, a2, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    @DisplayName("Test filter() method")
    public void testFilter() {
        ShortStream filtered = stream.filter(n -> n % 2 == 0);
        assertArrayEquals(new short[] { 2, 4 }, filtered.toArray());
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
    @DisplayName("Test takeWhile() method")
    public void testTakeWhile() {
        ShortStream taken = stream.takeWhile(n -> n < 4);
        assertArrayEquals(new short[] { 1, 2, 3 }, taken.toArray());
    }

    @Test
    @DisplayName("Test dropWhile() method")
    public void testDropWhile() {
        ShortStream dropped = stream.dropWhile(n -> n < 4);
        assertArrayEquals(new short[] { 4, 5 }, dropped.toArray());
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
    @DisplayName("Test distinct() method")
    public void testDistinct() {
        ShortStream stream = createShortStream((short) 1, (short) 2, (short) 2, (short) 3, (short) 1);
        ShortStream distinct = stream.distinct();
        assertArrayEquals(new short[] { 1, 2, 3 }, distinct.toArray());
    }

    @Test
    @DisplayName("Test map() method")
    public void testMap() {
        ShortStream mapped = stream.map(n -> (short) (n * 2));
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, mapped.toArray());
    }

    @Test
    @DisplayName("Test mapToInt() method")
    public void testMapToInt() {
        IntStream intStream = stream.mapToInt(n -> n * 10);
        assertArrayEquals(new int[] { 10, 20, 30, 40, 50 }, intStream.toArray());
    }

    @Test
    @DisplayName("Test mapToObj() method")
    public void testMapToObj() {
        Stream<String> objStream = stream.mapToObj(n -> "Value: " + n);
        List<String> result = objStream.toList();
        assertEquals(Arrays.asList("Value: 1", "Value: 2", "Value: 3", "Value: 4", "Value: 5"), result);
    }

    @Test
    @DisplayName("Test flatMap() method")
    public void testFlatMap() {
        ShortStream flattened = stream.flatMap(n -> createShortStream(n, (short) (n * 10)));
        assertArrayEquals(new short[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 }, flattened.toArray());
    }

    @Test
    @DisplayName("Test flatmap() with array")
    public void testFlatmapArray() {
        ShortStream flattened = stream.flatmap(n -> new short[] { n, (short) (n * 10) });
        assertArrayEquals(new short[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 }, flattened.toArray());
    }

    @Test
    @DisplayName("Test flatMapToInt() method")
    public void testFlatMapToInt() {
        IntStream flattened = stream.flatMapToInt(n -> IntStream.of(n, n * 10));
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 }, flattened.toArray());
    }

    @Test
    @DisplayName("Test flatMapToObj() method")
    public void testFlatMapToObj() {
        Stream<String> flattened = stream.flatMapToObj(n -> Stream.of("A" + n, "B" + n));
        List<String> result = flattened.toList();
        assertEquals(Arrays.asList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"), result);
    }

    @Test
    @DisplayName("Test flatmapToObj() with collection")
    public void testFlatmapToObjCollection() {
        Stream<String> flattened = stream.flatmapToObj(n -> Arrays.asList("A" + n, "B" + n));
        List<String> result = flattened.toList();
        assertEquals(Arrays.asList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"), result);
    }

    @Test
    @DisplayName("Test flattmapToObj() with array")
    public void testFlattMapToObjArray() {
        Stream<String> flattened = stream.flattmapToObj(n -> new String[] { "A" + n, "B" + n });
        List<String> result = flattened.toList();
        assertEquals(Arrays.asList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"), result);
    }

    @Test
    @DisplayName("Test mapPartial() method")
    public void testMapPartial() {
        ShortStream mapped = stream.mapPartial(n -> n % 2 == 0 ? OptionalShort.of((short) (n * 2)) : OptionalShort.empty());
        assertArrayEquals(new short[] { 4, 8 }, mapped.toArray());
    }

    @Test
    @DisplayName("Test rangeMap() method")
    public void testRangeMap() {
        ShortStream ranged = createShortStream((short) 1, (short) 2, (short) 5, (short) 6, (short) 10).rangeMap((a, b) -> Math.abs(a - b) <= 1,
                (a, b) -> (short) ((a + b) / 2));
        assertArrayEquals(new short[] { 1, 5, 10 }, ranged.toArray());
    }

    @Test
    @DisplayName("Test rangeMapToObj() method")
    public void testRangeMapToObj() {
        Stream<String> ranged = createShortStream((short) 1, (short) 2, (short) 5, (short) 6, (short) 10).rangeMapToObj((a, b) -> Math.abs(a - b) <= 1,
                (a, b) -> a + "-" + b);
        assertEquals(Arrays.asList("1-2", "5-6", "10-10"), ranged.toList());
    }

    @Test
    @DisplayName("Test collapse() methods")
    public void testCollapse() {
        Stream<ShortList> collapsed1 = createShortStream((short) 1, (short) 2, (short) 5, (short) 6, (short) 10).collapse((a, b) -> Math.abs(a - b) <= 1);
        List<ShortList> result1 = collapsed1.toList();
        assertEquals(3, result1.size());
        assertArrayEquals(new short[] { 1, 2 }, result1.get(0).toArray());
        assertArrayEquals(new short[] { 5, 6 }, result1.get(1).toArray());
        assertArrayEquals(new short[] { 10 }, result1.get(2).toArray());

        ShortStream collapsed2 = createShortStream((short) 1, (short) 2, (short) 5, (short) 6, (short) 10).collapse((a, b) -> Math.abs(a - b) <= 1,
                (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 3, 11, 10 }, collapsed2.toArray());

        ShortStream collapsed3 = createShortStream((short) 1, (short) 2, (short) 3, (short) 10, (short) 11).collapse((first, last, next) -> next - first <= 3,
                (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 6, 21 }, collapsed3.toArray());
    }

    @Test
    @DisplayName("Test scan() methods")
    public void testScan() {
        ShortStream scanned1 = stream.scan((a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 1, 3, 6, 10, 15 }, scanned1.toArray());

        ShortStream scanned2 = stream2.scan((short) 10, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 11, 13, 16, 20, 25 }, scanned2.toArray());

        ShortStream scanned3 = stream3.scan((short) 10, true, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 10, 11, 13, 16, 20, 25 }, scanned3.toArray());
    }

    @Test
    @DisplayName("Test prepend() methods")
    public void testPrepend() {
        ShortStream prepended1 = stream.prepend((short) 10, (short) 20);
        assertArrayEquals(new short[] { 10, 20, 1, 2, 3, 4, 5 }, prepended1.toArray());

        ShortStream prepended2 = stream2.prepend(ShortStream.of((short) 10, (short) 20));
        assertArrayEquals(new short[] { 10, 20, 1, 2, 3, 4, 5 }, prepended2.toArray());

        ShortStream prepended3 = stream3.prepend(OptionalShort.of((short) 10));
        assertArrayEquals(new short[] { 10, 1, 2, 3, 4, 5 }, prepended3.toArray());

        ShortStream prepended4 = stream4.prepend(OptionalShort.empty());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, prepended4.toArray());
    }

    @Test
    @DisplayName("Test append() methods")
    public void testAppend() {
        ShortStream appended1 = stream.append((short) 10, (short) 20);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 10, 20 }, appended1.toArray());

        ShortStream appended2 = stream2.append(ShortStream.of((short) 10, (short) 20));
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 10, 20 }, appended2.toArray());

        ShortStream appended3 = stream3.append(OptionalShort.of((short) 10));
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 10 }, appended3.toArray());
    }

    @Test
    @DisplayName("Test appendIfEmpty() methods")
    public void testAppendIfEmpty() {
        ShortStream emptyStream = ShortStream.empty();
        ShortStream nonEmptyStream = createShortStream((short) 1, (short) 2);

        assertArrayEquals(new short[] { 10, 20 }, emptyStream.appendIfEmpty((short) 10, (short) 20).toArray());
        assertArrayEquals(new short[] { 1, 2 }, nonEmptyStream.appendIfEmpty((short) 10, (short) 20).toArray());

        assertArrayEquals(new short[] { 10, 20 }, ShortStream.empty().defaultIfEmpty(() -> createShortStream((short) 10, (short) 20)).toArray());
    }

    @Test
    @DisplayName("Test top() methods")
    public void testTop() {
        ShortStream unsorted = createShortStream((short) 5, (short) 1, (short) 4, (short) 2, (short) 3);

        assertArrayEquals(new short[] { 3, 4, 5 }, unsorted.top(3).sorted().toArray());

        unsorted = createShortStream((short) 5, (short) 1, (short) 4, (short) 2, (short) 3);

        assertArrayEquals(new short[] { 1, 2, 3 }, unsorted.top(3, Comparator.reverseOrder()).sorted().toArray());
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
    @DisplayName("Test reversed() method")
    public void testReversed() {
        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 }, stream.reversed().toArray());
    }

    @Test
    @DisplayName("Test rotated() method")
    public void testRotated() {
        assertArrayEquals(new short[] { 4, 5, 1, 2, 3 }, stream.rotated(2).toArray());
        assertArrayEquals(new short[] { 3, 4, 5, 1, 2 }, stream2.rotated(-2).toArray());
    }

    @Test
    @DisplayName("Test shuffled() methods")
    public void testShuffled() {
        short[] shuffled1 = stream.shuffled().toArray();
        assertEquals(5, shuffled1.length);
        Set<Short> set1 = new HashSet<>();
        for (short s : shuffled1) {
            set1.add(s);
        }
        assertEquals(Set.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), set1);

        Random random = new Random(42);
        short[] shuffled2 = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).shuffled(random).toArray();
        assertEquals(5, shuffled2.length);
    }

    @Test
    @DisplayName("Test cycled() methods")
    public void testCycled() {
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 1, 2 }, stream.cycled().limit(7).toArray());

        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 },
                createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).cycled(2).toArray());
    }

    @Test
    @DisplayName("Test indexed() method")
    public void testIndexed() {
        List<String> indexed = stream.indexed().map(idx -> idx.index() + ":" + idx.value()).toList();
        assertEquals(Arrays.asList("0:1", "1:2", "2:3", "3:4", "4:5"), indexed);
    }

    @Test
    @DisplayName("Test skip() methods")
    public void testSkip() {
        assertArrayEquals(new short[] { 3, 4, 5 }, stream.skip(2).toArray());

        List<Short> skipped = new ArrayList<>();
        assertArrayEquals(new short[] { 3, 4, 5 }, createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, skipped::add).toArray());
        assertEquals(Arrays.asList((short) 1, (short) 2), skipped);
    }

    @Test
    @DisplayName("Test limit() method")
    public void testLimit() {
        assertArrayEquals(new short[] { 1, 2, 3 }, stream.limit(3).toArray());
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
        short[] result = stream.onEach(peeked::add).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), peeked);

        peeked.clear();
        result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).peek(peeked::add).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), peeked);
    }

    @Test
    @DisplayName("Test intersection() method")
    public void testIntersection() {
        Collection<Short> other = Arrays.asList((short) 3, (short) 4, (short) 5, (short) 6, (short) 7);
        assertArrayEquals(new short[] { 3, 4, 5 }, stream.intersection(other).toArray());
    }

    @Test
    @DisplayName("Test difference() method")
    public void testDifference() {
        Collection<Short> other = Arrays.asList((short) 3, (short) 4, (short) 5, (short) 6, (short) 7);
        assertArrayEquals(new short[] { 1, 2 }, stream.difference(other).toArray());
    }

    @Test
    @DisplayName("Test symmetricDifference() method")
    public void testSymmetricDifference() {
        Collection<Short> other = Arrays.asList((short) 3, (short) 4, (short) 5, (short) 6, (short) 7);
        short[] result = stream.symmetricDifference(other).toArray();
        Set<Short> resultSet = new HashSet<>();
        for (short s : result) {
            resultSet.add(s);
        }
        assertEquals(Set.of((short) 1, (short) 2, (short) 6, (short) 7), resultSet);
    }

    @Test
    @DisplayName("Test asIntStream() method")
    public void testAsIntStream() {
        IntStream intStream = stream.asIntStream();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, intStream.toArray());
    }

    @Test
    @DisplayName("Test boxed() method")
    public void testBoxed() {
        Stream<Short> boxed = stream.boxed();
        List<Short> result = boxed.toList();
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), result);
    }

    @Test
    @DisplayName("Test toArray() method")
    public void testToArray() {
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    @DisplayName("Test toShortList() method")
    public void testToShortList() {
        ShortList list = stream.toShortList();
        assertEquals(5, list.size());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, list.toArray());
    }

    @Test
    @DisplayName("Test toList() method")
    public void testToList() {
        List<Short> list = stream.toList();
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), list);
    }

    @Test
    @DisplayName("Test toSet() method")
    public void testToSet() {
        Set<Short> set = stream.toSet();
        assertEquals(Set.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), set);
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
    @DisplayName("Test toMultiset() methods")
    public void testToMultiset() {
        ShortStream streamWithDuplicates = createShortStream((short) 1, (short) 2, (short) 2, (short) 3, (short) 3, (short) 3);

        Multiset<Short> multiset1 = streamWithDuplicates.toMultiset();
        assertEquals(1, multiset1.occurrencesOf((short) 1));
        assertEquals(2, multiset1.occurrencesOf((short) 2));
        assertEquals(3, multiset1.occurrencesOf((short) 3));

        Multiset<Short> multiset2 = createShortStream((short) 1, (short) 2, (short) 2, (short) 3, (short) 3, (short) 3).toMultiset(Multiset::new);
        assertEquals(1, multiset2.occurrencesOf((short) 1));
    }

    @Test
    @DisplayName("Test toMap() methods")
    public void testToMap() {
        Map<Short, String> map1 = stream.toMap(n -> n, n -> "Value" + n);
        assertEquals(5, map1.size());
        assertEquals("Value1", map1.get((short) 1));

        Map<Short, String> map2 = stream2.toMap(n -> n, n -> "Value" + n, Suppliers.ofLinkedHashMap());
        assertEquals(5, map2.size());

        ShortStream duplicateStream = createShortStream((short) 1, (short) 1, (short) 2, (short) 2);
        Map<Short, String> map3 = duplicateStream.toMap(n -> n, n -> "Value" + n, (v1, v2) -> v1 + "," + v2);
        assertEquals("Value1,Value1", map3.get((short) 1));

        LinkedHashMap<Short, String> map4 = createShortStream((short) 1, (short) 1, (short) 2, (short) 2).toMap(n -> n, n -> "Value" + n,
                (v1, v2) -> v1 + "," + v2, LinkedHashMap::new);
        assertEquals("Value1,Value1", map4.get((short) 1));
    }

    @Test
    @DisplayName("Test groupTo() methods")
    public void testGroupTo() {
        ShortStream streamForGrouping = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6);

        Map<Boolean, List<Short>> grouped1 = streamForGrouping.groupTo(n -> n % 2 == 0, Collectors.toList());
        assertEquals(Arrays.asList((short) 2, (short) 4, (short) 6), grouped1.get(true));
        assertEquals(Arrays.asList((short) 1, (short) 3, (short) 5), grouped1.get(false));

        LinkedHashMap<Boolean, Long> grouped2 = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).groupTo(n -> n % 2 == 0,
                Collectors.counting(), LinkedHashMap::new);
        assertEquals(3L, grouped2.get(true));
        assertEquals(3L, grouped2.get(false));
    }

    @Test
    @DisplayName("Test reduce() methods")
    public void testReduce() {
        short sum = stream.reduce((short) 0, (a, b) -> (short) (a + b));
        assertEquals(15, sum);

        OptionalShort optSum = stream2.reduce((a, b) -> (short) (a + b));
        assertTrue(optSum.isPresent());
        assertEquals(15, optSum.get());

        OptionalShort emptyResult = ShortStream.empty().reduce((a, b) -> (short) (a + b));
        assertFalse(emptyResult.isPresent());
    }

    @Test
    @DisplayName("Test collect() methods")
    public void testCollect() {
        ArrayList<Short> collected1 = stream.collect(ArrayList::new, (list, n) -> list.add(n), ArrayList::addAll);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), collected1);

        ArrayList<Short> collected2 = stream2.collect(ArrayList::new, (list, n) -> list.add(n));
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), collected2);
    }

    @Test
    @DisplayName("Test forEach() method")
    public void testForEach() {
        List<Short> collected = new ArrayList<>();
        stream.forEach(collected::add);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), collected);
    }

    @Test
    @DisplayName("Test forEachIndexed() method")
    public void testForEachIndexed() {
        Map<Integer, Short> indexMap = new HashMap<>();
        stream.forEachIndexed((index, value) -> indexMap.put(index, value));
        assertEquals(5, indexMap.size());
        assertEquals((short) 1, indexMap.get(0));
        assertEquals((short) 5, indexMap.get(4));
    }

    @Test
    @DisplayName("Test anyMatch() method")
    public void testAnyMatch() {
        assertTrue(stream.anyMatch(n -> n > 3));
        assertFalse(ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).anyMatch(n -> n > 10));
    }

    @Test
    @DisplayName("Test allMatch() method")
    public void testAllMatch() {
        assertTrue(stream.allMatch(n -> n > 0));
        assertFalse(ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).allMatch(n -> n > 3));
    }

    @Test
    @DisplayName("Test noneMatch() method")
    public void testNoneMatch() {
        assertTrue(stream.noneMatch(n -> n > 10));
        assertFalse(ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).noneMatch(n -> n > 3));
    }

    @Test
    @DisplayName("Test findFirst() method")
    public void testFindFirst() {
        OptionalShort first = stream.findFirst(n -> n > 3);
        assertTrue(first.isPresent());
        assertEquals(4, first.get());

        OptionalShort notFound = createShortStream((short) 1, (short) 2, (short) 3).findFirst(n -> n > 10);
        assertFalse(notFound.isPresent());
    }

    @Test
    @DisplayName("Test findAny() method")
    public void testFindAny() {
        OptionalShort any = stream.findAny(n -> n > 3);
        assertTrue(any.isPresent());
        assertTrue(any.get() > 3);
    }

    @Test
    @DisplayName("Test findLast() method")
    public void testFindLast() {
        OptionalShort last = stream.findLast(n -> n < 4);
        assertTrue(last.isPresent());
        assertEquals(3, last.get());
    }

    @Test
    @DisplayName("Test min() method")
    public void testMin() {
        OptionalShort min = stream.min();
        assertTrue(min.isPresent());
        assertEquals(1, min.get());

        OptionalShort emptyMin = ShortStream.empty().min();
        assertFalse(emptyMin.isPresent());
    }

    @Test
    @DisplayName("Test max() method")
    public void testMax() {
        OptionalShort max = stream.max();
        assertTrue(max.isPresent());
        assertEquals(5, max.get());

        OptionalShort emptyMax = ShortStream.empty().max();
        assertFalse(emptyMax.isPresent());
    }

    @Test
    @DisplayName("Test kthLargest() method")
    public void testKthLargest() {
        OptionalShort kth = stream.kthLargest(2);
        assertTrue(kth.isPresent());
        assertEquals(4, kth.get());

        OptionalShort notFound = stream2.kthLargest(10);
        assertFalse(notFound.isPresent());
    }

    @Test
    @DisplayName("Test sum() method")
    public void testSum() {
        int sum = stream.sum();
        assertEquals(15, sum);

        assertEquals(0, ShortStream.empty().sum());
    }

    @Test
    @DisplayName("Test average() method")
    public void testAverage() {
        OptionalDouble avg = stream.average();
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.getAsDouble(), 0.001);

        OptionalDouble emptyAvg = ShortStream.empty().average();
        assertFalse(emptyAvg.isPresent());
    }

    @Test
    @DisplayName("Test summaryStatistics() method")
    public void testsummaryStatistics() {
        ShortSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(5, stats.getCount());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(15, stats.getSum());
        assertEquals(3.0, stats.getAverage(), 0.001);
    }

    @Test
    @DisplayName("Test summaryStatisticsAndPercentiles() method")
    public void testsummaryStatisticsAndPercentiles() {
        Pair<ShortSummaryStatistics, Optional<Map<Percentage, Short>>> result = stream.summaryStatisticsAndPercentiles();

        ShortSummaryStatistics stats = result.left();
        assertEquals(5, stats.getCount());
        assertEquals(15, stats.getSum());

        assertTrue(result.right().isPresent());
        Map<Percentage, Short> percentiles = result.right().get();
        assertNotNull(percentiles.get(Percentage._50));
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
    @DisplayName("Test count() method")
    public void testCount() {
        assertEquals(5, stream.count());
        assertEquals(0, ShortStream.empty().count());
    }

    @Test
    @DisplayName("Test first() method")
    public void testFirst() {
        OptionalShort first = stream.first();
        assertTrue(first.isPresent());
        assertEquals(1, first.get());

        OptionalShort emptyFirst = ShortStream.empty().first();
        assertFalse(emptyFirst.isPresent());
    }

    @Test
    @DisplayName("Test last() method")
    public void testLast() {
        OptionalShort last = stream.last();
        assertTrue(last.isPresent());
        assertEquals(5, last.get());

        OptionalShort emptyLast = ShortStream.empty().last();
        assertFalse(emptyLast.isPresent());
    }

    @Test
    @DisplayName("Test elementAt() method")
    public void testElementAt() {
        OptionalShort element = stream.elementAt(2);
        assertTrue(element.isPresent());
        assertEquals(3, element.get());
    }

    @Test
    @DisplayName("Test onlyOne() method")
    public void testOnlyOne() {
        OptionalShort single = createShortStream((short) 42).onlyOne();
        assertTrue(single.isPresent());
        assertEquals(42, single.get());

        OptionalShort empty = ShortStream.empty().onlyOne();
        assertFalse(empty.isPresent());

        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    @DisplayName("Test join() methods")
    public void testJoin() {
        assertEquals("1,2,3,4,5", stream.join(","));

        assertEquals("[1,2,3,4,5]", createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).join(",", "[", "]"));
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
    @DisplayName("Test mergeWith() method")
    public void testMergeWith() {
        ShortStream s1 = createShortStream((short) 1, (short) 3, (short) 5);
        ShortStream s2 = createShortStream((short) 2, (short) 4, (short) 6);

        short[] merged = s1.mergeWith(s2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, merged);
    }

    @Test
    @DisplayName("Test zipWith() methods")
    public void testZipWith() {
        ShortStream s1 = createShortStream((short) 1, (short) 2, (short) 3);
        ShortStream s2 = createShortStream((short) 4, (short) 5, (short) 6);

        short[] zipped1 = s1.zipWith(s2, (a, b) -> (short) (a + b)).toArray();
        assertArrayEquals(new short[] { 5, 7, 9 }, zipped1);

        ShortStream s3 = createShortStream((short) 7, (short) 8, (short) 9);
        short[] zipped2 = createShortStream((short) 1, (short) 2, (short) 3)
                .zipWith(ShortStream.of((short) 4, (short) 5, (short) 6), createShortStream((short) 7, (short) 8, (short) 9), (a, b, c) -> (short) (a + b + c))
                .toArray();
        assertArrayEquals(new short[] { 12, 15, 18 }, zipped2);

        ShortStream s4 = createShortStream((short) 1, (short) 2, (short) 3, (short) 4);
        ShortStream s5 = createShortStream((short) 5, (short) 6);
        short[] zipped3 = s4.zipWith(s5, (short) 0, (short) 10, (a, b) -> (short) (a + b)).toArray();
        assertArrayEquals(new short[] { 6, 8, 13, 14 }, zipped3);
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
    @DisplayName("Test isParallel() method")
    public void testIsParallel() {
        assertFalse(stream.isParallel());
        assertTrue(stream.parallel().isParallel());
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
    @DisplayName("Test onClose() method")
    public void testOnClose() {
        final boolean[] closed = { false };

        ShortStream streamWithCloseHandler = stream.onClose(() -> closed[0] = true);
        streamWithCloseHandler.toArray();
        assertTrue(closed[0]);
    }

    @Test
    @DisplayName("Test close() method")
    public void testClose() {
        final boolean[] closed = { false };

        ShortStream streamWithCloseHandler = stream.onClose(() -> closed[0] = true);
        streamWithCloseHandler.close();
        assertTrue(closed[0]);
    }

    @Test
    @DisplayName("Test iterator() method")
    public void testIterator() {
        ShortIterator iter = stream.iterator();
        List<Short> collected = new ArrayList<>();
        while (iter.hasNext()) {
            collected.add(iter.nextShort());
        }
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), collected);
    }
}
