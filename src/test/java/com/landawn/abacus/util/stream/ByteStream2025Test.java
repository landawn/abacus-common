package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IndexedByte;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalDouble;

@Tag("2025")
public class ByteStream2025Test extends TestBase {

    @Test
    public void testEmpty() {
        ByteStream stream = ByteStream.empty();
        assertNotNull(stream);
        assertEquals(0, stream.count());

        ByteStream emptyStream = ByteStream.empty();
        assertFalse(emptyStream.findFirst().isPresent());

        ByteStream emptyStream2 = ByteStream.empty();
        assertEquals(0, emptyStream2.toArray().length);
    }

    @Test
    public void testDefer() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<ByteStream> supplier = () -> {
            counter.incrementAndGet();
            return ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        };

        ByteStream stream = ByteStream.defer(supplier);
        assertEquals(0, counter.get());

        byte[] result = stream.toArray();
        assertEquals(1, counter.get());
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);

        assertThrows(IllegalArgumentException.class, () -> ByteStream.defer(null));
    }

    @Test
    public void testOfNullable() {
        ByteStream stream1 = ByteStream.ofNullable(null);
        assertEquals(0, stream1.count());

        ByteStream stream2 = ByteStream.ofNullable((byte) 5);
        assertEquals(5, stream2.findFirst().get());

        ByteStream stream3 = ByteStream.ofNullable((byte) 0);
        assertEquals(0, stream3.findFirst().get());
    }

    @Test
    public void testOfArray() {
        byte[] array = { 1, 2, 3, 4, 5 };
        ByteStream stream = ByteStream.of(array);
        assertArrayEquals(array, stream.toArray());

        ByteStream emptyStream = ByteStream.of(new byte[0]);
        assertEquals(0, emptyStream.count());

        ByteStream rangeStream = ByteStream.of(array, 1, 4);
        assertArrayEquals(new byte[] { 2, 3, 4 }, rangeStream.toArray());

        ByteStream nullStream = ByteStream.of((byte[]) null);
        assertEquals(0, nullStream.count());
    }

    @Test
    public void testOfByteArray() {
        Byte[] array = { 1, 2, 3, 4, 5 };
        ByteStream stream = ByteStream.of(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());

        ByteStream rangeStream = ByteStream.of(array, 1, 3);
        assertArrayEquals(new byte[] { 2, 3 }, rangeStream.toArray());

        ByteStream emptyStream = ByteStream.of(new Byte[0]);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testOfCollection() {
        List<Byte> list = Arrays.asList((byte) 1, (byte) 2, (byte) 3);
        ByteStream stream = ByteStream.of(list);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream.toArray());

        List<Byte> emptyList = Arrays.asList();
        ByteStream emptyStream = ByteStream.of(emptyList);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testOfByteIterator() {
        ByteIterator iter = ByteIterator.of(new byte[] { 1, 2, 3 });
        ByteStream stream = ByteStream.of(iter);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream.toArray());

        ByteStream nullStream = ByteStream.of((ByteIterator) null);
        assertEquals(0, nullStream.count());
    }

    @Test
    public void testOfByteBuffer() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        buffer.position(1);
        buffer.limit(4);

        ByteStream stream = ByteStream.of(buffer);
        assertArrayEquals(new byte[] { 2, 3, 4 }, stream.toArray());

        ByteStream nullStream = ByteStream.of((ByteBuffer) null);
        assertEquals(0, nullStream.count());
    }

    @Test
    public void testOfFile() throws IOException {
        File tempFile = File.createTempFile("test", ".tmp");
        tempFile.deleteOnExit();

        byte[] data = { 1, 2, 3, 4, 5 };
        IOUtil.write(data, tempFile);

        ByteStream stream = ByteStream.of(tempFile);
        assertArrayEquals(data, stream.toArray());
    }

    @Test
    public void testOfInputStream() throws IOException {
        byte[] data = { 1, 2, 3, 4, 5 };
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);

        ByteStream stream = ByteStream.of(inputStream);
        assertArrayEquals(data, stream.toArray());

        ByteArrayInputStream inputStream2 = new ByteArrayInputStream(data);
        ByteStream stream2 = ByteStream.of(inputStream2, true);
        assertArrayEquals(data, stream2.toArray());

        ByteStream nullStream = ByteStream.of((InputStream) null);
        assertEquals(0, nullStream.count());
    }

    @Test
    public void testFilter() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertArrayEquals(new byte[] { 3, 4, 5 }, stream.filter(b -> b > 2).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(3, stream2.filter(b -> b > 0).count());

        ByteStream stream3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(0, stream3.filter(b -> b > 10).count());

        ByteStream emptyStream = ByteStream.empty();
        assertEquals(0, emptyStream.filter(b -> b > 0).count());
    }

    @Test
    public void testTakeWhile() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2 }, stream.takeWhile(b -> b < 3).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(3, stream2.takeWhile(b -> b < 10).count());

        ByteStream stream3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(0, stream3.takeWhile(b -> b < 0).count());
    }

    @Test
    public void testDropWhile() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertArrayEquals(new byte[] { 3, 4, 5 }, stream.dropWhile(b -> b < 3).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(0, stream2.dropWhile(b -> b < 10).count());

        ByteStream stream3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(3, stream3.dropWhile(b -> b < 0).count());
    }

    @Test
    public void testMap() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 2, 4, 6 }, stream.map(b -> (byte) (b * 2)).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream2.map(b -> b).toArray());

        ByteStream emptyStream = ByteStream.empty();
        assertEquals(0, emptyStream.map(b -> (byte) (b * 2)).count());
    }

    @Test
    public void testMapToInt() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new int[] { 10, 20, 30 }, stream.mapToInt(b -> b * 10).toArray());

        ByteStream stream2 = ByteStream.of((byte) 5, (byte) 10, (byte) 15);
        assertArrayEquals(new int[] { 5, 10, 15 }, stream2.mapToInt(b -> b).toArray());
    }

    @Test
    public void testMapToObj() {
        ByteStream stream = ByteStream.of((byte) 65, (byte) 66, (byte) 67);
        List<String> result = stream.mapToObj(b -> String.valueOf((char) b)).toList();
        assertEquals(Arrays.asList("A", "B", "C"), result);

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        List<Integer> intList = stream2.mapToObj(b -> (int) b).toList();
        assertEquals(Arrays.asList(1, 2, 3), intList);
    }

    @Test
    public void testFlatMap() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 10, 2, 20 }, stream.flatMap(b -> ByteStream.of(b, (byte) (b * 10))).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2 }, stream2.flatMap(b -> ByteStream.of(b)).toArray());
    }

    @Test
    public void testFlatmap() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 10, 2, 20 }, stream.flatmap(b -> new byte[] { b, (byte) (b * 10) }).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        assertEquals(2, stream2.flatmap(b -> new byte[] { b }).count());
    }

    @Test
    public void testFlatMapToInt() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2);
        assertArrayEquals(new int[] { 1, 10, 2, 20 }, stream.flatMapToInt(b -> IntStream.of(b, b * 10)).toArray());

        ByteStream stream2 = ByteStream.of((byte) 3, (byte) 4);
        assertEquals(2, stream2.flatMapToInt(b -> IntStream.of(b)).count());
    }

    @Test
    public void testFlatMapToObj() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2);
        List<String> result = stream.flatMapToObj(b -> Stream.of("byte:" + b, "doubled:" + (b * 2))).toList();
        assertEquals(Arrays.asList("byte:1", "doubled:2", "byte:2", "doubled:4"), result);
    }

    @Test
    public void testFlatmapToObj() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2);
        List<Integer> result = stream.flatmapToObj(b -> Arrays.asList((int) b, b * 10)).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20), result);
    }

    @Test
    public void testFlattmapToObj() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2);
        List<String> result = stream.flattmapToObj(b -> new String[] { "a" + b, "b" + b }).toList();
        assertEquals(Arrays.asList("a1", "b1", "a2", "b2"), result);
    }

    @Test
    public void testMapPartial() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertArrayEquals(new byte[] { 2, 4 }, stream.mapPartial(b -> b % 2 == 0 ? OptionalByte.of(b) : OptionalByte.empty()).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 3, (byte) 5);
        assertEquals(0, stream2.mapPartial(b -> OptionalByte.empty()).count());
    }

    @Test
    public void testRangeMap() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11);
        assertArrayEquals(new byte[] { 1, 10 }, stream.rangeMap((first, next) -> next - first <= 2, (first, last) -> first).toArray());
    }

    @Test
    public void testRangeMapToObj() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11);
        List<String> result = stream.rangeMapToObj((first, next) -> next - first <= 2, (first, last) -> first + "-" + last).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testCollapseWithPredicate() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 5, (byte) 6, (byte) 7, (byte) 10);
        List<ByteList> result = stream.collapse((a, b) -> b - a <= 2).toList();
        assertTrue(result.size() > 0);
    }

    @Test
    public void testCollapseWithMerge() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 5, (byte) 6, (byte) 7, (byte) 10);
        assertArrayEquals(new byte[] { 3, 18, 10 }, stream.collapse((a, b) -> b - a <= 2, (a, b) -> (byte) (a + b)).toArray());
    }

    @Test
    public void testCollapseTriPredicate() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11);
        byte[] result = stream.collapse((first, last, next) -> next - first <= 3, (a, b) -> (byte) (a + b)).toArray();
        assertTrue(result.length > 0);
    }

    @Test
    public void testScan() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        assertArrayEquals(new byte[] { 1, 3, 6, 10 }, stream.scan((a, b) -> (byte) (a + b)).toArray());

        ByteStream stream2 = ByteStream.of((byte) 5);
        assertArrayEquals(new byte[] { 5 }, stream2.scan((a, b) -> (byte) (a + b)).toArray());
    }

    @Test
    public void testScanWithInit() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        assertArrayEquals(new byte[] { 11, 13, 16, 20 }, stream.scan((byte) 10, (a, b) -> (byte) (a + b)).toArray());

        ByteStream emptyStream = ByteStream.empty();
        assertEquals(0, emptyStream.scan((byte) 10, (a, b) -> (byte) (a + b)).count());
    }

    @Test
    public void testScanWithInitIncluded() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 10, 11, 13, 16 }, stream.scan((byte) 10, true, (a, b) -> (byte) (a + b)).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 11, 13, 16 }, stream2.scan((byte) 10, false, (a, b) -> (byte) (a + b)).toArray());
    }

    @Test
    public void testPrepend() {
        ByteStream stream = ByteStream.of((byte) 3, (byte) 4);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.prepend((byte) 1, (byte) 2).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2 }, stream2.prepend().toArray());
    }

    @Test
    public void testAppend() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.append((byte) 3, (byte) 4).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2 }, stream2.append().toArray());
    }

    @Test
    public void testAppendIfEmpty() {
        ByteStream emptyStream = ByteStream.empty();
        assertArrayEquals(new byte[] { 1, 2 }, emptyStream.appendIfEmpty((byte) 1, (byte) 2).toArray());

        ByteStream stream = ByteStream.of((byte) 5);
        assertArrayEquals(new byte[] { 5 }, stream.appendIfEmpty((byte) 1, (byte) 2).toArray());
    }

    @Test
    public void testToByteList() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list = stream.toByteList();
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void testToMap() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        Map<Integer, String> map = stream.toMap(b -> (int) b, b -> "value" + b);
        assertEquals(3, map.size());
        assertEquals("value1", map.get(1));
        assertEquals("value2", map.get(2));
        assertEquals("value3", map.get(3));
    }

    @Test
    public void testToMapWithFactory() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        Map<Integer, String> map = stream.toMap(b -> (int) b, b -> "val" + b, Suppliers.ofMap());
        assertEquals(3, map.size());
        assertTrue(map instanceof HashMap);
    }

    @Test
    public void testToMapWithMerge() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 1);
        Map<Integer, String> map = stream.toMap(b -> (int) b, b -> "v" + b, (a, b) -> a + "," + b);
        assertEquals(2, map.size());
        assertEquals("v1,v1", map.get(1));
    }

    @Test
    public void testToMapWithMergeAndFactory() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 1);
        Map<Integer, String> map = stream.toMap(b -> (int) b, b -> "x" + b, (a, b) -> a + b, HashMap::new);
        assertEquals(2, map.size());
    }

    @Test
    public void testGroupTo() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        Map<Boolean, List<Byte>> map = stream.groupTo(b -> b % 2 == 0, java.util.stream.Collectors.toList());
        assertEquals(2, map.size());
        assertTrue(map.containsKey(true));
        assertTrue(map.containsKey(false));
    }

    @Test
    public void testGroupToWithFactory() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        Map<Integer, List<Byte>> map = stream.groupTo(b -> (int) b, java.util.stream.Collectors.toList(), HashMap::new);
        assertEquals(3, map.size());
    }

    @Test
    public void testReduce() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        byte sum = stream.reduce((byte) 0, (a, b) -> (byte) (a + b));
        assertEquals(10, sum);

        ByteStream stream2 = ByteStream.of((byte) 5, (byte) 10);
        assertEquals(115, stream2.reduce((byte) 100, (a, b) -> (byte) (a + b)));
    }

    @Test
    public void testReduceOptional() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        OptionalByte result = stream.reduce((a, b) -> (byte) (a + b));
        assertTrue(result.isPresent());
        assertEquals(6, result.get());

        ByteStream emptyStream = ByteStream.empty();
        OptionalByte emptyResult = emptyStream.reduce((a, b) -> (byte) (a + b));
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testCollect() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list = stream.collect(ByteList::new, ByteList::add, ByteList::addAll);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
    }

    @Test
    public void testCollectWithoutCombiner() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list = stream.collect(ByteList::new, ByteList::add);
        assertEquals(3, list.size());
    }

    @Test
    public void testForEach() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        List<Byte> collected = new ArrayList<>();
        stream.forEach(collected::add);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), collected);
    }

    @Test
    public void testForEachIndexed() {
        ByteStream stream = ByteStream.of((byte) 10, (byte) 20, (byte) 30);
        List<String> collected = new ArrayList<>();
        stream.forEachIndexed((idx, b) -> collected.add(idx + ":" + b));
        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), collected);
    }

    @Test
    public void testAnyMatch() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(stream.anyMatch(b -> b > 2));

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(stream2.anyMatch(b -> b > 10));

        ByteStream emptyStream = ByteStream.empty();
        assertFalse(emptyStream.anyMatch(b -> b > 0));
    }

    @Test
    public void testAllMatch() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(stream.allMatch(b -> b > 0));

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(stream2.allMatch(b -> b > 2));

        ByteStream emptyStream = ByteStream.empty();
        assertTrue(emptyStream.allMatch(b -> b > 0));
    }

    @Test
    public void testNoneMatch() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(stream.noneMatch(b -> b > 10));

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(stream2.noneMatch(b -> b > 2));

        ByteStream emptyStream = ByteStream.empty();
        assertTrue(emptyStream.noneMatch(b -> b > 0));
    }

    @Test
    public void testFindFirst() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        OptionalByte result = stream.findFirst(b -> b > 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        OptionalByte noMatch = stream2.findFirst(b -> b > 10);
        assertFalse(noMatch.isPresent());
    }

    @Test
    public void testFindAny() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        OptionalByte result = stream.findAny(b -> b > 2);
        assertTrue(result.isPresent());
        assertTrue(result.get() >= 3);

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        OptionalByte noMatch = stream2.findAny(b -> b > 10);
        assertFalse(noMatch.isPresent());
    }

    @Test
    public void testFindLast() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        OptionalByte result = stream.findLast(b -> b > 2);
        assertTrue(result.isPresent());
        assertEquals(5, result.get());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        OptionalByte noMatch = stream2.findLast(b -> b > 10);
        assertFalse(noMatch.isPresent());
    }

    @Test
    public void testMin() {
        ByteStream stream = ByteStream.of((byte) 3, (byte) 1, (byte) 4, (byte) 2);
        OptionalByte min = stream.min();
        assertTrue(min.isPresent());
        assertEquals(1, min.get());

        ByteStream emptyStream = ByteStream.empty();
        assertFalse(emptyStream.min().isPresent());
    }

    @Test
    public void testMax() {
        ByteStream stream = ByteStream.of((byte) 3, (byte) 1, (byte) 4, (byte) 2);
        OptionalByte max = stream.max();
        assertTrue(max.isPresent());
        assertEquals(4, max.get());

        ByteStream emptyStream = ByteStream.empty();
        assertFalse(emptyStream.max().isPresent());
    }

    @Test
    public void testKthLargest() {
        ByteStream stream = ByteStream.of((byte) 3, (byte) 1, (byte) 4, (byte) 2, (byte) 5);
        OptionalByte result = stream.kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4, result.get());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        assertFalse(stream2.kthLargest(5).isPresent());
    }

    @Test
    public void testSum() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        int sum = stream.sum();
        assertEquals(10, sum);

        ByteStream emptyStream = ByteStream.empty();
        assertEquals(0, emptyStream.sum());
    }

    @Test
    public void testAverage() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        OptionalDouble avg = stream.average();
        assertTrue(avg.isPresent());
        assertEquals(2.5, avg.getAsDouble(), 0.001);

        ByteStream emptyStream = ByteStream.empty();
        assertFalse(emptyStream.average().isPresent());
    }

    @Test
    public void testSummarize() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteSummaryStatistics stats = stream.summarize();
        assertNotNull(stats);
        assertEquals(5, stats.getCount());
        assertEquals(15, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(3.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testSummarizeAndPercentiles() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> result = stream.summarizeAndPercentiles();
        assertNotNull(result);
        assertNotNull(result.left());
        assertEquals(5, result.left().getCount());
        assertTrue(result.right().isPresent());
    }

    @Test
    public void testMergeWith() {
        ByteStream stream1 = ByteStream.of((byte) 1, (byte) 3, (byte) 5);
        ByteStream stream2 = ByteStream.of((byte) 2, (byte) 4, (byte) 6);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 },
                stream1.mergeWith(stream2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
    }

    @Test
    public void testZipWith() {
        ByteStream stream1 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream stream2 = ByteStream.of((byte) 10, (byte) 20, (byte) 30);
        assertArrayEquals(new byte[] { 11, 22, 33 }, stream1.zipWith(stream2, (a, b) -> (byte) (a + b)).toArray());

        ByteStream s1 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream s2 = ByteStream.of((byte) 10, (byte) 20);
        assertEquals(2, s1.zipWith(s2, (a, b) -> (byte) (a + b)).count());
    }

    @Test
    public void testZipWithDefaults() {
        ByteStream stream1 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream stream2 = ByteStream.of((byte) 10, (byte) 20);
        assertArrayEquals(new byte[] { 11, 22, 3 }, stream1.zipWith(stream2, (byte) 0, (byte) 0, (a, b) -> (byte) (a + b)).toArray());
    }

    @Test
    public void testZipWithThreeDefaults() {
        ByteStream stream1 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream stream2 = ByteStream.of((byte) 10, (byte) 20);
        ByteStream stream3 = ByteStream.of((byte) 100);
        byte[] result = stream1.zipWith(stream2, stream3, (byte) 0, (byte) 0, (byte) 0, (a, b, c) -> (byte) (a + b + c)).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testAsIntStream() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.asIntStream().toArray());

        ByteStream stream2 = ByteStream.of((byte) -1, (byte) 0, (byte) 127);
        assertArrayEquals(new int[] { -1, 0, 127 }, stream2.asIntStream().toArray());
    }

    @Test
    public void testBoxed() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        List<Byte> boxed = stream.boxed().toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), boxed);
    }

    @Test
    public void testRange() {
        ByteStream stream = ByteStream.range((byte) 1, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.toArray());

        ByteStream emptyStream = ByteStream.range((byte) 5, (byte) 5);
        assertEquals(0, emptyStream.count());

        ByteStream negStream = ByteStream.range((byte) -3, (byte) 0);
        assertArrayEquals(new byte[] { -3, -2, -1 }, negStream.toArray());
    }

    @Test
    public void testRangeWithStep() {
        ByteStream stream = ByteStream.range((byte) 1, (byte) 10, (byte) 2);
        assertArrayEquals(new byte[] { 1, 3, 5, 7, 9 }, stream.toArray());

        ByteStream stream2 = ByteStream.range((byte) 1, (byte) 4, (byte) 1);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream2.toArray());
    }

    @Test
    public void testRangeClosed() {
        ByteStream stream = ByteStream.rangeClosed((byte) 1, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());

        ByteStream singleStream = ByteStream.rangeClosed((byte) 5, (byte) 5);
        assertArrayEquals(new byte[] { 5 }, singleStream.toArray());
    }

    @Test
    public void testRangeClosedWithStep() {
        ByteStream stream = ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 2);
        assertArrayEquals(new byte[] { 1, 3, 5, 7, 9 }, stream.toArray());

        ByteStream stream2 = ByteStream.rangeClosed((byte) 0, (byte) 10, (byte) 5);
        assertArrayEquals(new byte[] { 0, 5, 10 }, stream2.toArray());
    }

    @Test
    public void testRepeat() {
        ByteStream stream = ByteStream.repeat((byte) 5, 3);
        assertArrayEquals(new byte[] { 5, 5, 5 }, stream.toArray());

        ByteStream emptyStream = ByteStream.repeat((byte) 1, 0);
        assertEquals(0, emptyStream.count());

        assertThrows(IllegalArgumentException.class, () -> ByteStream.repeat((byte) 1, -1));
    }

    @Test
    public void testRandom() {
        ByteStream stream = ByteStream.random().limit(10);
        assertEquals(10, stream.count());

        ByteStream stream2 = ByteStream.random().limit(5);
        byte[] random = stream2.toArray();
        assertEquals(5, random.length);
    }

    @Test
    public void testIterateWithHasNext() {
        AtomicInteger counter = new AtomicInteger(0);
        ByteStream stream = ByteStream.iterate(() -> counter.get() < 3, () -> (byte) counter.getAndIncrement());
        assertArrayEquals(new byte[] { 0, 1, 2 }, stream.toArray());
    }

    @Test
    public void testIterateWithInitAndHasNext() {
        ByteStream stream = ByteStream.iterate((byte) 1, () -> true, b -> (byte) (b * 2)).limit(4);
        assertArrayEquals(new byte[] { 1, 2, 4, 8 }, stream.toArray());
    }

    @Test
    public void testIterateWithInitAndPredicate() {
        ByteStream stream = ByteStream.iterate((byte) 1, b -> b < 10, b -> (byte) (b * 2));
        assertArrayEquals(new byte[] { 1, 2, 4, 8 }, stream.toArray());
    }

    @Test
    public void testIterateInfinite() {
        ByteStream stream = ByteStream.iterate((byte) 1, b -> (byte) (b + 1)).limit(5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testGenerate() {
        AtomicInteger counter = new AtomicInteger(0);
        ByteStream stream = ByteStream.generate(() -> (byte) counter.getAndIncrement()).limit(3);
        assertArrayEquals(new byte[] { 0, 1, 2 }, stream.toArray());

        assertThrows(IllegalArgumentException.class, () -> ByteStream.generate(null));
    }

    @Test
    public void testConcatArrays() {
        ByteStream stream = ByteStream.concat(new byte[] { 1, 2 }, new byte[] { 3, 4 });
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.toArray());

        ByteStream stream2 = ByteStream.concat(new byte[] { 1 }, new byte[0], new byte[] { 2 });
        assertArrayEquals(new byte[] { 1, 2 }, stream2.toArray());
    }

    @Test
    public void testConcatIterators() {
        ByteIterator iter1 = ByteIterator.of(new byte[] { 1, 2 });
        ByteIterator iter2 = ByteIterator.of(new byte[] { 3, 4 });
        ByteStream stream = ByteStream.concat(iter1, iter2);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcatStreams() {
        ByteStream stream1 = ByteStream.of((byte) 1, (byte) 2);
        ByteStream stream2 = ByteStream.of((byte) 3, (byte) 4);
        ByteStream stream = ByteStream.concat(stream1, stream2);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcatListOfArrays() {
        List<byte[]> list = Arrays.asList(new byte[] { 1, 2 }, new byte[] { 3, 4 });
        ByteStream stream = ByteStream.concat(list);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcatCollectionOfStreams() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 2), ByteStream.of((byte) 3, (byte) 4));
        ByteStream stream = ByteStream.concat(streams);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcatIterators2() {
        Collection<ByteIterator> iterators = Arrays.asList(ByteIterator.of(new byte[] { 1, 2 }), ByteIterator.of(new byte[] { 3, 4 }));
        ByteStream stream = ByteStream.concatIterators(iterators);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testZipArrays() {
        byte[] a1 = { 1, 2, 3 };
        byte[] a2 = { 10, 20, 30 };
        ByteStream stream = ByteStream.zip(a1, a2, (a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 11, 22, 33 }, stream.toArray());
    }

    @Test
    public void testZipIterators() {
        ByteIterator iter1 = ByteIterator.of(new byte[] { 1, 2 });
        ByteIterator iter2 = ByteIterator.of(new byte[] { 10, 20 });
        ByteStream stream = ByteStream.zip(iter1, iter2, (a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 11, 22 }, stream.toArray());
    }

    @Test
    public void testZipStreams() {
        ByteStream s1 = ByteStream.of((byte) 1, (byte) 2);
        ByteStream s2 = ByteStream.of((byte) 10, (byte) 20);
        ByteStream stream = ByteStream.zip(s1, s2, (a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 11, 22 }, stream.toArray());
    }

    @Test
    public void testZipCollectionOfStreams() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 2), ByteStream.of((byte) 10, (byte) 20));
        ByteStream stream = ByteStream.zip(streams, array -> (byte) (array[0] + array[1]));
        assertArrayEquals(new byte[] { 11, 22 }, stream.toArray());
    }

    @Test
    public void testZipArraysWithDefaults() {
        byte[] a1 = { 1, 2, 3 };
        byte[] a2 = { 10, 20 };
        ByteStream stream = ByteStream.zip(a1, a2, (byte) 0, (byte) 100, (a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 11, 22, 103 }, stream.toArray());
    }

    @Test
    public void testZipThreeArraysWithDefaults() {
        byte[] a1 = { 1, 2, 3 };
        byte[] a2 = { 10, 20 };
        byte[] a3 = { 100 };
        ByteStream stream = ByteStream.zip(a1, a2, a3, (byte) 0, (byte) 0, (byte) 0, (a, b, c) -> (byte) (a + b + c));
        assertEquals(3, stream.count());
    }

    @Test
    public void testZipIteratorsWithDefaults() {
        ByteIterator iter1 = ByteIterator.of(new byte[] { 1, 2, 3 });
        ByteIterator iter2 = ByteIterator.of(new byte[] { 10, 20 });
        ByteStream stream = ByteStream.zip(iter1, iter2, (byte) 0, (byte) 0, (a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 11, 22, 3 }, stream.toArray());
    }

    @Test
    public void testZipStreamsWithDefaults() {
        ByteStream s1 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream s2 = ByteStream.of((byte) 10, (byte) 20);
        ByteStream stream = ByteStream.zip(s1, s2, (byte) 0, (byte) 0, (a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 11, 22, 3 }, stream.toArray());
    }

    @Test
    public void testZipThreeStreamsWithDefaults() {
        ByteStream s1 = ByteStream.of((byte) 1, (byte) 2);
        ByteStream s2 = ByteStream.of((byte) 10);
        ByteStream s3 = ByteStream.of((byte) 100, (byte) 200, (byte) 300);
        ByteStream stream = ByteStream.zip(s1, s2, s3, (byte) 0, (byte) 0, (byte) 0, (a, b, c) -> (byte) (a + b + c));
        assertEquals(3, stream.count());
    }

    @Test
    public void testZipCollectionWithDefaults() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 2, (byte) 3), ByteStream.of((byte) 10, (byte) 20));
        byte[] defaults = { 0, 0 };
        ByteStream stream = ByteStream.zip(streams, defaults, array -> (byte) (array[0] + array[1]));
        assertEquals(3, stream.count());
    }

    @Test
    public void testMergeArrays() {
        byte[] a1 = { 1, 3, 5 };
        byte[] a2 = { 2, 4, 6 };
        ByteStream stream = ByteStream.merge(a1, a2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMergeThreeArrays() {
        byte[] a1 = { 1, 4 };
        byte[] a2 = { 2, 5 };
        byte[] a3 = { 3, 6 };
        ByteStream stream = ByteStream.merge(a1, a2, a3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMergeIterators() {
        ByteIterator iter1 = ByteIterator.of(new byte[] { 1, 3, 5 });
        ByteIterator iter2 = ByteIterator.of(new byte[] { 2, 4, 6 });
        ByteStream stream = ByteStream.merge(iter1, iter2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMergeThreeIterators() {
        ByteIterator iter1 = ByteIterator.of(new byte[] { 1, 4 });
        ByteIterator iter2 = ByteIterator.of(new byte[] { 2, 5 });
        ByteIterator iter3 = ByteIterator.of(new byte[] { 3, 6 });
        ByteStream stream = ByteStream.merge(iter1, iter2, iter3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMergeStreams() {
        ByteStream s1 = ByteStream.of((byte) 1, (byte) 3, (byte) 5);
        ByteStream s2 = ByteStream.of((byte) 2, (byte) 4, (byte) 6);
        ByteStream stream = ByteStream.merge(s1, s2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMergeThreeStreams() {
        ByteStream s1 = ByteStream.of((byte) 1, (byte) 4);
        ByteStream s2 = ByteStream.of((byte) 2, (byte) 5);
        ByteStream s3 = ByteStream.of((byte) 3, (byte) 6);
        ByteStream stream = ByteStream.merge(s1, s2, s3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMergeCollectionOfStreams() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 4), ByteStream.of((byte) 2, (byte) 5), ByteStream.of((byte) 3, (byte) 6));
        ByteStream stream = ByteStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testFlatten2D() {
        byte[][] array = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        ByteStream stream = ByteStream.flatten(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());

        byte[][] emptyArray = new byte[0][];
        ByteStream emptyStream = ByteStream.flatten(emptyArray);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testFlatten2DVertically() {
        byte[][] array = { { 1, 2, 3 }, { 4, 5, 6 } };
        ByteStream stream = ByteStream.flatten(array, false);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());

        byte[][] array2 = { { 1, 2, 3 }, { 4, 5, 6 } };
        ByteStream stream2 = ByteStream.flatten(array2, true);
        assertArrayEquals(new byte[] { 1, 4, 2, 5, 3, 6 }, stream2.toArray());
    }

    @Test
    public void testFlatten2DWithAlignment() {
        byte[][] array = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        ByteStream stream = ByteStream.flatten(array, (byte) 0, true);
        byte[] result = stream.toArray();
        assertEquals(9, result.length);
    }

    @Test
    public void testFlatten3D() {
        byte[][][] array = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } };
        ByteStream stream = ByteStream.flatten(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, stream.toArray());
    }

    @Test
    public void testSkip() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertArrayEquals(new byte[] { 3, 4, 5 }, stream.skip(2).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        assertEquals(0, stream2.skip(5).count());

        ByteStream stream3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(3, stream3.skip(0).count());
    }

    @Test
    public void testLimit() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream.limit(3).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        assertEquals(2, stream2.limit(5).count());

        ByteStream stream3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(0, stream3.limit(0).count());
    }

    @Test
    public void testDistinct() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 1, (byte) 4);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.distinct().toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(3, stream2.distinct().count());

        ByteStream stream3 = ByteStream.of((byte) 5, (byte) 5, (byte) 5);
        assertArrayEquals(new byte[] { 5 }, stream3.distinct().toArray());
    }

    @Test
    public void testSorted() {
        ByteStream stream = ByteStream.of((byte) 3, (byte) 1, (byte) 4, (byte) 2, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.sorted().toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream2.sorted().toArray());

        ByteStream stream3 = ByteStream.of((byte) -3, (byte) 1, (byte) -1, (byte) 0);
        assertArrayEquals(new byte[] { -3, -1, 0, 1 }, stream3.sorted().toArray());
    }

    @Test
    public void testReversed() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, stream.reversed().toArray());

        ByteStream stream2 = ByteStream.of((byte) 1);
        assertArrayEquals(new byte[] { 1 }, stream2.reversed().toArray());

        ByteStream emptyStream = ByteStream.empty();
        assertEquals(0, emptyStream.reversed().count());
    }

    @Test
    public void testShuffled() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        byte[] shuffled = stream.shuffled().toArray();
        assertEquals(5, shuffled.length);
        ByteList original = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        for (byte b : shuffled) {
            assertTrue(original.contains(b));
        }
    }

    @Test
    public void testIndexed() {
        ByteStream stream = ByteStream.of((byte) 10, (byte) 20, (byte) 30);
        List<IndexedByte> indexed = stream.indexed().toList();
        assertEquals(3, indexed.size());
        assertEquals(0, indexed.get(0).index());
        assertEquals(10, indexed.get(0).value());
        assertEquals(1, indexed.get(1).index());
        assertEquals(20, indexed.get(1).value());
        assertEquals(2, indexed.get(2).index());
        assertEquals(30, indexed.get(2).value());
    }

    @Test
    public void testPeek() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        List<Byte> peeked = new ArrayList<>();
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream.peek(peeked::add).toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), peeked);
    }

    @Test
    public void testSkipUntil() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertArrayEquals(new byte[] { 3, 4, 5 }, stream.skipUntil(b -> b >= 3).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(0, stream2.skipUntil(b -> b > 10).count());

        ByteStream stream3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(3, stream3.skipUntil(b -> b > 0).count());
    }

    @Test
    public void testToList() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        List<Byte> list = stream.toList();
        assertEquals(3, list.size());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), list);

        ByteStream emptyStream = ByteStream.empty();
        assertEquals(0, emptyStream.toList().size());
    }

    @Test
    public void testToSet() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 1);
        Set<Byte> set = stream.toSet();
        assertEquals(3, set.size());
        assertTrue(set.contains((byte) 1));
        assertTrue(set.contains((byte) 2));
        assertTrue(set.contains((byte) 3));

        ByteStream stream2 = ByteStream.of((byte) 5, (byte) 5, (byte) 5);
        assertEquals(1, stream2.toSet().size());
    }

    @Test
    public void testFirst() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(1, stream.first().get());

        ByteStream emptyStream = ByteStream.empty();
        assertFalse(emptyStream.first().isPresent());
    }

    @Test
    public void testLast() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(3, stream.last().get());

        ByteStream emptyStream = ByteStream.empty();
        assertFalse(emptyStream.last().isPresent());

        ByteStream stream2 = ByteStream.of((byte) 42);
        assertEquals(42, stream2.last().get());
    }

    @Test
    public void testOnlyOne() {
        ByteStream stream = ByteStream.of((byte) 42);
        assertEquals(42, stream.onlyOne().get());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        assertThrows(TooManyElementsException.class, () -> stream2.onlyOne());

        ByteStream emptyStream = ByteStream.empty();
        assertFalse(emptyStream.onlyOne().isPresent());
    }

    @Test
    public void testCount() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertEquals(5, stream.count());

        ByteStream emptyStream = ByteStream.empty();
        assertEquals(0, emptyStream.count());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertEquals(3, stream2.filter(b -> b > 2).count());
    }

    @Test
    public void testIterator() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteIterator iter = stream.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextByte());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.nextByte());
        assertTrue(iter.hasNext());
        assertEquals(3, iter.nextByte());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOnEach() {
        AtomicInteger sum = new AtomicInteger(0);
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        stream.onEach(b -> sum.addAndGet(b)).forEach(b -> {
        });
        assertEquals(6, sum.get());
    }

    @Test
    public void testStep() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6);
        assertArrayEquals(new byte[] { 1, 3, 5 }, stream.step(2).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream2.step(1).toArray());

        ByteStream stream3 = ByteStream.of((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1 }, stream3.step(5).toArray());
    }

    @Test
    public void testRotated() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertArrayEquals(new byte[] { 4, 5, 1, 2, 3 }, stream.rotated(2).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertArrayEquals(new byte[] { 3, 4, 5, 1, 2 }, stream2.rotated(-2).toArray());

        ByteStream stream3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream3.rotated(0).toArray());
    }

    @Test
    public void testCycled() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3, 1, 2, 3, 1, 2 }, stream.cycled(3).limit(8).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        assertEquals(6, stream2.cycled(3).count());
    }

    @Test
    public void testJoin() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        String joined = stream.join(", ");
        assertEquals("1, 2, 3", joined);

        ByteStream stream2 = ByteStream.of((byte) 42);
        assertEquals("42", stream2.join(", "));

        ByteStream emptyStream = ByteStream.empty();
        assertEquals("", emptyStream.join(", "));
    }

    @Test
    public void testPercentiles() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10);
        Map<Percentage, Byte> percentiles = stream.percentiles().get();
        assertNotNull(percentiles);
        assertTrue(percentiles.size() > 0);
    }

    @Test
    public void testThrowIfEmpty() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream.throwIfEmpty().toArray());

        ByteStream emptyStream = ByteStream.empty();
        assertThrows(NoSuchElementException.class, () -> emptyStream.throwIfEmpty().toArray());
    }

    @Test
    public void testReverseSorted() {
        ByteStream stream = ByteStream.of((byte) 3, (byte) 1, (byte) 4, (byte) 2, (byte) 5);
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, stream.reverseSorted().toArray());

        ByteStream stream2 = ByteStream.of((byte) 5, (byte) 4, (byte) 3);
        assertArrayEquals(new byte[] { 5, 4, 3 }, stream2.reverseSorted().toArray());
    }

    @Test
    public void testToMultiset() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3, (byte) 3);
        Multiset<Byte> multiset = stream.toMultiset();
        assertEquals(1, multiset.get((byte) 1));
        assertEquals(2, multiset.get((byte) 2));
        assertEquals(3, multiset.get((byte) 3));
    }

    // Additional coverage improvement tests - Collection-based factory methods

    @Test
    public void testZipWithCollectionOfStreams() {
        Collection<ByteStream> streams = Arrays.asList(
                ByteStream.of((byte) 1, (byte) 2),
                ByteStream.of((byte) 10, (byte) 20),
                ByteStream.of((byte) 100, (byte) 127)
        );
        ByteStream result = ByteStream.zip(streams, bytes -> {
            byte sum = 0;
            for (Byte b : bytes) sum += b;
            return sum;
        });
        assertArrayEquals(new byte[] { 111, -107 }, result.toArray());
    }

    @Test
    public void testZipWithCollectionAndValuesForNone() {
        Collection<ByteStream> streams = Arrays.asList(
                ByteStream.of((byte) 1, (byte) 2, (byte) 3),
                ByteStream.of((byte) 10, (byte) 20)
        );
        byte[] defaults = { 0, 100 };
        ByteStream result = ByteStream.zip(streams, defaults, bytes -> {
            byte sum = 0;
            for (Byte b : bytes) sum += b;
            return sum;
        });
        assertEquals(3, result.count());
    }

    @Test
    public void testMergeWithCollectionOfStreams() {
        Collection<ByteStream> streams = Arrays.asList(
                ByteStream.of((byte) 1, (byte) 5),
                ByteStream.of((byte) 2, (byte) 6),
                ByteStream.of((byte) 3, (byte) 7)
        );
        ByteStream result = ByteStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, result.count());
    }

    @Test
    public void testMergeThreeArraysAdditional() {
        byte[] a1 = { 1, 7 };
        byte[] a2 = { 3, 8 };
        byte[] a3 = { 5, 9 };
        ByteStream stream = ByteStream.merge(a1, a2, a3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMergeThreeStreamsAdditional() {
        ByteStream s1 = ByteStream.of((byte) 1, (byte) 10);
        ByteStream s2 = ByteStream.of((byte) 5, (byte) 15);
        ByteStream s3 = ByteStream.of((byte) 8, (byte) 20);
        ByteStream stream = ByteStream.merge(s1, s2, s3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    // Edge cases

    @Test
    public void testFlattenWithAlignmentHorizontally() {
        byte[][] array = { { 1, 2, 3 }, { 4, 5 }, { 6 } };
        ByteStream stream = ByteStream.flatten(array, (byte) 0, true);
        byte[] result = stream.toArray();
        assertEquals(9, result.length);
        assertEquals(1, result[0]);
        assertEquals(4, result[1]);
        assertEquals(6, result[2]);
    }

    @Test
    public void testRangeClosedWithNegativeStep() {
        ByteStream stream = ByteStream.rangeClosed((byte) 10, (byte) 1, (byte) -2);
        assertArrayEquals(new byte[] { 10, 8, 6, 4, 2 }, stream.toArray());
    }

    @Test
    public void testRangeWithInvalidStepDirection() {
        ByteStream stream = ByteStream.range((byte) 1, (byte) 10, (byte) -1);
        assertEquals(0, stream.count());
    }

    // Parallel operations

    @Test
    public void testParallelStreamOperations() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8).parallel();
        byte[] result = stream.filter(b -> b % 2 == 0).map(b -> (byte) (b * 2)).toArray();
        assertEquals(4, result.length);
    }

    @Test
    public void testIterateWithImmediateFalseHasNext() {
        ByteStream stream = ByteStream.iterate((byte) 1, () -> false, b -> (byte) (b + 1));
        assertEquals(0, stream.count());
    }

    @Test
    public void testCollapseWithNoCollapsibleElements() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 10, (byte) 20, (byte) 30);
        byte[] result = stream.collapse((a, b) -> b - a <= 2, (a, b) -> (byte) (a + b)).toArray();
        assertTrue(result.length > 0);
    }

    @Test
    public void testScanOnEmptyStream() {
        ByteStream emptyStream = ByteStream.empty();
        byte[] result = emptyStream.scan((a, b) -> (byte) (a + b)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testZipThreeArraysWithDefaultsAdditional() {
        byte[] a1 = { 1, 2 };
        byte[] a2 = { 10 };
        byte[] a3 = { 100, 101, 102 };
        ByteStream stream = ByteStream.zip(a1, a2, a3, (byte) 0, (byte) 0, (byte) 0, (a, b, c) -> (byte) (a + b + c));
        assertEquals(3, stream.count());
    }

    @Test
    public void testZipThreeIteratorsWithDefaultsAdditional() {
        ByteIterator iter1 = ByteIterator.of(new byte[] { 1, 2 });
        ByteIterator iter2 = ByteIterator.of(new byte[] { 10 });
        ByteIterator iter3 = ByteIterator.of(new byte[] { 100, 101, 102 });
        ByteStream stream = ByteStream.zip(iter1, iter2, iter3, (byte) 0, (byte) 0, (byte) 0, (a, b, c) -> (byte) (a + b + c));
        assertEquals(3, stream.count());
    }

    @Test
    public void testZipThreeStreamsWithDefaultsAdditional() {
        ByteStream s1 = ByteStream.of((byte) 1, (byte) 2);
        ByteStream s2 = ByteStream.of((byte) 10);
        ByteStream s3 = ByteStream.of((byte) 100, (byte) 101, (byte) 102);
        ByteStream stream = ByteStream.zip(s1, s2, s3, (byte) 0, (byte) 0, (byte) 0, (a, b, c) -> (byte) (a + b + c));
        assertEquals(3, stream.count());
    }
}
