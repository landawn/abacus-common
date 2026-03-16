package com.landawn.abacus.util.stream;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IndexedByte;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteBiPredicate;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteNFunction;
import com.landawn.abacus.util.function.ByteTriPredicate;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;

@Tag("2025")
public class ByteStreamTest extends TestBase {

    private static final byte[] TEST_ARRAY = { 1, 2, 3, 4, 5 };

    private ByteStream byteStream;

    private ByteStream createByteStream(final byte... elements) {
        return ByteStream.of(elements);
    }

    private ByteStream createByteStream(final byte[] elements, final int fromIndex, final int toIndex) {
        return ByteStream.of(elements, fromIndex, toIndex);
    }

    private ByteStream createByteStream(final Byte[] elements) {
        return ByteStream.of(elements);
    }

    private ByteStream createByteStream(final Byte[] elements, final int fromIndex, final int toIndex) {
        return ByteStream.of(elements, fromIndex, toIndex);
    }

    private ByteStream createByteStream(final Collection<Byte> coll) {
        return ByteStream.of(coll.toArray(new Byte[coll.size()]));
    }

    private ByteStream createByteStream(final ByteIterator iter) {
        return iter == null ? ByteStream.empty() : ByteStream.of(iter.toArray());
    }

    private ByteStream createByteStream(final ByteBuffer buff) {
        return ByteStream.of(buff);
    }

    private ByteStream createByteStream(final File file) {
        return ByteStream.of(file);
    }

    private ByteStream createByteStream(final InputStream is) {
        return ByteStream.of(is);
    }

    private ByteStream createByteStream(final InputStream is, final boolean closeInputStreamOnClose) {
        return ByteStream.of(is, closeInputStreamOnClose);
    }

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
        List<String> result = stream.flatMapArrayToObj(b -> new String[] { "a" + b, "b" + b }).toList();
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
    public void testsummaryStatistics() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteSummaryStatistics stats = stream.summaryStatistics();
        assertNotNull(stats);
        assertEquals(5, stats.getCount());
        assertEquals(15, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(3.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> result = stream.summaryStatisticsAndPercentiles();
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
        stream.peek(b -> sum.addAndGet(b)).forEach(b -> {
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
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 2), ByteStream.of((byte) 10, (byte) 20),
                ByteStream.of((byte) 100, (byte) 127));
        ByteStream result = ByteStream.zip(streams, bytes -> {
            byte sum = 0;
            for (Byte b : bytes)
                sum += b;
            return sum;
        });
        assertArrayEquals(new byte[] { 111, -107 }, result.toArray());
    }

    @Test
    public void testZipWithCollectionAndValuesForNone() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 2, (byte) 3), ByteStream.of((byte) 10, (byte) 20));
        byte[] defaults = { 0, 100 };
        ByteStream result = ByteStream.zip(streams, defaults, bytes -> {
            byte sum = 0;
            for (Byte b : bytes)
                sum += b;
            return sum;
        });
        assertEquals(3, result.count());
    }

    @Test
    public void testMergeWithCollectionOfStreams() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 5), ByteStream.of((byte) 2, (byte) 6), ByteStream.of((byte) 3, (byte) 7));
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

    // ==================== debounce tests ====================

    @Test
    public void testDebounce_BasicFunctionality() {
        // Allow 3 elements per 1 second window
        byte[] result = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        // Only first 3 elements should pass through within the window
        assertEquals(3, result.length);
        assertEquals((byte) 1, result[0]);
        assertEquals((byte) 2, result[1]);
        assertEquals((byte) 3, result[2]);
    }

    @Test
    public void testDebounce_AllElementsPassWhenWithinLimit() {
        // Allow 10 elements per window, but only 5 elements in stream
        byte[] result = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).debounce(10, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        // All elements should pass
        assertEquals(5, result.length);
    }

    @Test
    public void testDebounce_EmptyStream() {
        byte[] result = ByteStream.empty().debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertEquals(0, result.length);
    }

    @Test
    public void testDebounce_SingleElement() {
        byte[] result = ByteStream.of((byte) 42).debounce(1, com.landawn.abacus.util.Duration.ofMillis(100)).toArray();

        assertEquals(1, result.length);
        assertEquals((byte) 42, result[0]);
    }

    @Test
    public void testDebounce_MaxWindowSizeOne() {
        // Only 1 element allowed per window
        byte[] result = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).debounce(1, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertEquals(1, result.length);
        assertEquals((byte) 1, result[0]);
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveMaxWindowSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            ByteStream.of((byte) 1, (byte) 2, (byte) 3).debounce(0, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ByteStream.of((byte) 1, (byte) 2, (byte) 3).debounce(-1, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();
        });
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveDuration() {
        assertThrows(IllegalArgumentException.class, () -> {
            ByteStream.of((byte) 1, (byte) 2, (byte) 3).debounce(5, com.landawn.abacus.util.Duration.ofMillis(0)).toArray();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ByteStream.of((byte) 1, (byte) 2, (byte) 3).debounce(5, com.landawn.abacus.util.Duration.ofMillis(-100)).toArray();
        });
    }

    @Test
    public void testDebounce_WithLargeMaxWindowSize() {
        byte[] input = new byte[100];
        for (int i = 0; i < 100; i++) {
            input[i] = (byte) i;
        }

        byte[] result = ByteStream.of(input).debounce(50, com.landawn.abacus.util.Duration.ofSeconds(10)).toArray();

        assertEquals(50, result.length);
    }

    @Test
    public void testDebounce_PreservesOrder() {
        byte[] result = ByteStream.of((byte) 10, (byte) 20, (byte) 30, (byte) 40, (byte) 50)
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals((byte) 10, result[0]);
        assertEquals((byte) 20, result[1]);
        assertEquals((byte) 30, result[2]);
    }

    @Test
    public void testDebounce_ChainedWithOtherOperations() {
        byte[] result = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10)
                .filter(n -> n % 2 == 0) // 2, 4, 6, 8, 10
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)) // 2, 4, 6
                .map(n -> (byte) (n * 10)) // 20, 40, 60
                .toArray();

        assertEquals(3, result.length);
        assertEquals((byte) 20, result[0]);
        assertEquals((byte) 40, result[1]);
        assertEquals((byte) 60, result[2]);
    }

    @Test
    public void testDebounce_WithMinMaxValues() {
        byte[] result = ByteStream.of(Byte.MIN_VALUE, (byte) 0, Byte.MAX_VALUE, (byte) 1, (byte) 2)
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals(3, result.length);
        assertEquals(Byte.MIN_VALUE, result[0]);
        assertEquals((byte) 0, result[1]);
        assertEquals(Byte.MAX_VALUE, result[2]);
    }

    @Test
    public void testFlatmapByteArray() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<Byte> result = byteStream.flatmap(b -> new byte[] { b, (byte) (b * 10) }).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 10, (byte) 2, (byte) 20), result);
    }

    @Test
    public void testFlatmapToObjCollection() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<String> result = byteStream.flatmapToObj(b -> Arrays.asList("C" + b, "D" + b)).toList();
        assertEquals(Arrays.asList("C1", "D1", "C2", "D2"), result);
    }

    @Test
    public void testFlattMapToObj() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<String> result = byteStream.flatMapArrayToObj(b -> new String[] { "E" + b, "F" + b }).toList();
        assertEquals(Arrays.asList("E1", "F1", "E2", "F2"), result);
    }

    @Test
    public void testCollapseBiPredicate() {
        byteStream = createByteStream(new byte[] { 1, 2, 5, 6, 7, 10 });
        ByteBiPredicate collapsible = (a, b) -> Math.abs(a - b) < 3;
        List<ByteList> result = byteStream.collapse(collapsible).toList();
        assertEquals(Arrays.asList(ByteList.of((byte) 1, (byte) 2), ByteList.of((byte) 5, (byte) 6, (byte) 7), ByteList.of((byte) 10)), result);
    }

    @Test
    public void testCollapseBiPredicateMergeFunction() {
        byteStream = createByteStream(new byte[] { 1, 2, 5, 6, 7, 10 });
        ByteBiPredicate collapsible = (a, b) -> Math.abs(a - b) < 3;
        ByteBinaryOperator mergeFunction = (a, b) -> (byte) (a + b);
        List<Byte> result = byteStream.collapse(collapsible, mergeFunction).toList();
        assertEquals(Arrays.asList((byte) 3, (byte) 18, (byte) 10), result);
    }

    @Test
    public void testCollapseTriPredicateMergeFunction() {
        byteStream = createByteStream(new byte[] { 1, 3, 6, 10, 15, 16 });
        ByteTriPredicate collapsible = (first, last, next) -> next >= last;
        ByteBinaryOperator mergeFunction = (a, b) -> (byte) (a + b);
        List<Byte> result = byteStream.collapse(collapsible, mergeFunction).toList();
        assertEquals(Arrays.asList((byte) 51), result);
    }

    @Test
    public void testScanAccumulator() {
        byteStream = createByteStream(new byte[] { 1, 2, 3, 4 });
        List<Byte> result = byteStream.scan((a, b) -> (byte) (a + b)).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 6, (byte) 10), result);
    }

    @Test
    public void testScanInitAccumulator() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Byte> result = byteStream.scan((byte) 10, (a, b) -> (byte) (a + b)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 13, (byte) 16), result);
    }

    @Test
    public void testScanInitIncludedAccumulator() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Byte> result = byteStream.scan((byte) 10, true, (a, b) -> (byte) (a + b)).toList();
        assertEquals(Arrays.asList((byte) 10, (byte) 11, (byte) 13, (byte) 16), result);
    }

    @Test
    public void testPrependBytes() {
        byteStream = createByteStream(new byte[] { 4, 5 });
        List<Byte> result = byteStream.prepend((byte) 1, (byte) 2, (byte) 3).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void testPrependByteStream() {
        byteStream = createByteStream(new byte[] { 4, 5 });
        ByteStream prefixStream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        List<Byte> result = byteStream.prepend(prefixStream).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void testPrependOptionalByte() {
        byteStream = createByteStream(new byte[] { 2, 3 });
        List<Byte> result = byteStream.prepend(OptionalByte.of((byte) 1)).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);

        byteStream = createByteStream(new byte[] { 2, 3 });
        result = byteStream.prepend(OptionalByte.empty()).toList();
        assertEquals(Arrays.asList((byte) 2, (byte) 3), result);
    }

    @Test
    public void testAppendBytes() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<Byte> result = byteStream.append((byte) 3, (byte) 4, (byte) 5).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void testAppendByteStream() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        ByteStream suffixStream = createByteStream((byte) 3, (byte) 4, (byte) 5);
        List<Byte> result = byteStream.append(suffixStream).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void testAppendOptionalByte() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<Byte> result = byteStream.append(OptionalByte.of((byte) 3)).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);

        byteStream = createByteStream(new byte[] { 1, 2 });
        result = byteStream.append(OptionalByte.empty()).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2), result);
    }

    @Test
    public void testAppendIfEmptyBytes() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<Byte> result = byteStream.appendIfEmpty((byte) 10, (byte) 11).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2), result);

        byteStream = createByteStream(new byte[] {});
        result = byteStream.appendIfEmpty((byte) 10, (byte) 11).toList();
        assertEquals(Arrays.asList((byte) 10, (byte) 11), result);
    }

    @Test
    public void testToMapKeyValue() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        Map<String, Integer> result = byteStream.toMap(b -> "K" + b, b -> b * 10);
        assertEquals(3, result.size());
        assertEquals(10, (int) result.get("K1"));
        assertEquals(20, (int) result.get("K2"));
        assertEquals(30, (int) result.get("K3"));
    }

    @Test
    public void testToMapKeyValueMapFactory() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        Map<String, Integer> result = byteStream.toMap(b -> "K" + b, b -> b * 10, () -> N.newHashMap());
        assertEquals(3, result.size());
        assertEquals(10, (int) result.get("K1"));
    }

    @Test
    public void testToMapKeyValueMergeFunction() {
        byteStream = createByteStream(new byte[] { 1, 2, 1 });
        Map<String, Integer> result = byteStream.toMap(b -> "K" + b, b -> b * 10, (v1, v2) -> v1 + v2);
        assertEquals(2, result.size());
        assertEquals(20, (int) result.get("K1"));
        assertEquals(20, (int) result.get("K2"));
    }

    @Test
    public void testToMapKeyValueMergeFunctionMapFactory() {
        byteStream = createByteStream(new byte[] { 1, 2, 1 });
        Map<String, Integer> result = byteStream.toMap(b -> "K" + b, b -> b * 10, (v1, v2) -> v1 + v2, () -> N.newLinkedHashMap());
        assertEquals(2, result.size());
        assertEquals(20, (int) result.get("K1"));
        assertEquals(20, (int) result.get("K2"));
    }

    @Test
    public void testGroupToCollector() {
        byteStream = createByteStream(TEST_ARRAY);
        Map<String, List<Byte>> result = byteStream.groupTo(b -> (b % 2 == 0) ? "Even" : "Odd", Collectors.mapping(b -> b, Collectors.toList()));
        assertEquals(2, result.size());
        assertEquals(Arrays.asList((byte) 2, (byte) 4), result.get("Even"));
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5), result.get("Odd"));
    }

    @Test
    public void testGroupToCollectorMapFactory() {
        byteStream = createByteStream(TEST_ARRAY);
        Map<String, Long> result = byteStream.groupTo(b -> (b % 2 == 0) ? "Even" : "Odd", Collectors.counting(), Suppliers.ofTreeMap());
        assertEquals(2, result.size());
        assertEquals(Long.valueOf(2), result.get("Even"));
        assertEquals(Long.valueOf(3), result.get("Odd"));
    }

    @Test
    public void testReduceIdentity() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        byte result = byteStream.reduce((byte) 10, (a, b) -> (byte) (a + b));
        assertEquals((byte) 16, result);
    }

    @Test
    public void testCollectFull() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        StringBuilder sb = byteStream.collect(StringBuilder::new, (s, b) -> s.append(b), StringBuilder::append);
        assertEquals("123", sb.toString());
    }

    @Test
    public void testCollectSimplified() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteList list = byteStream.collect(ByteList::new, ByteList::add);
        assertEquals(ByteList.of((byte) 1, (byte) 2, (byte) 3), list);
    }

    @Test
    public void testZipWithTwoStreams() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteStream otherStream = createByteStream(new byte[] { 10, 20, 30, 40 });
        List<Byte> result = byteStream.zipWith(otherStream, (a, b) -> (byte) (a + b)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) 33), result);
    }

    @Test
    public void testZipWithThreeStreams() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        ByteStream s2 = createByteStream(new byte[] { 10, 20 });
        ByteStream s3 = createByteStream(new byte[] { 50, 60 });
        List<Byte> result = byteStream.zipWith(s2, s3, (a, b, c) -> (byte) (a + b + c)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50), (byte) (2 + 20 + 60)), result);
    }

    @Test
    public void testZipWithTwoStreamsWithDefaults() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        ByteStream otherStream = createByteStream(new byte[] { 10, 20, 30 });
        List<Byte> result = byteStream.zipWith(otherStream, (byte) 99, (byte) 88, (a, b) -> (byte) (a + b)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) (99 + 30)), result);
    }

    @Test
    public void testZipWithThreeStreamsWithDefaults() {
        byteStream = createByteStream(new byte[] { 1 });
        ByteStream s2 = createByteStream(new byte[] { 10, 20 });
        ByteStream s3 = createByteStream(new byte[] { 50, 60, 70 });
        List<Byte> result = byteStream.zipWith(s2, s3, (byte) 99, (byte) 88, (byte) 77, (a, b, c) -> (byte) (a + b + c)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50), (byte) (99 + 20 + 60), (byte) (99 + 88 + 70)), result);
    }

    @Test
    public void testOfBytes() {
        List<Byte> result = createByteStream((byte) 1, (byte) 2, (byte) 3).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
        assertTrue(createByteStream().toList().isEmpty());
    }

    @Test
    public void testOfBytesRange() {
        byte[] data = { 10, 20, 30, 40, 50 };
        List<Byte> result = createByteStream(data, 1, 4).toList();
        assertEquals(Arrays.asList((byte) 20, (byte) 30, (byte) 40), result);

        assertTrue(createByteStream(data, 0, 0).toList().isEmpty());
    }

    @Test
    public void testOfBoxedBytes() {
        Byte[] data = { 1, 2, 3 };
        List<Byte> result = createByteStream(data).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    @Test
    public void testOfBoxedBytesRange() {
        Byte[] data = { 10, 20, 30, 40, 50 };
        List<Byte> result = createByteStream(data, 1, 4).toList();
        assertEquals(Arrays.asList((byte) 20, (byte) 30, (byte) 40), result);
    }

    @Test
    public void testFlatten2DArray() {
        byte[][] data = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        List<Byte> result = ByteStream.flatten(data).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), result);
    }

    @Test
    public void testFlatten2DArrayVertically() {
        byte[][] data = { { 1, 2, 3 }, { 4, 5, 6 } };
        List<Byte> result = ByteStream.flatten(data, true).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 4, (byte) 2, (byte) 5, (byte) 3, (byte) 6), result);

        result = ByteStream.flatten(data, false).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), result);
    }

    @Test
    public void testFlatten2DArrayWithAlignment() {
        byte[][] data = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        List<Byte> result = ByteStream.flatten(data, (byte) 0, false).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 0, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 0, (byte) 0), result);

        result = ByteStream.flatten(data, (byte) 0, true).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 6, (byte) 2, (byte) 4, (byte) 0, (byte) 0, (byte) 5, (byte) 0), result);
    }

    @Test
    public void testFlatten3DArray() {
        byte[][][] data = { { { 1, 2 } }, { { 3, 4, 5 } } };
        List<Byte> result = ByteStream.flatten(data).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void testRangeBy() {
        List<Byte> result = ByteStream.range((byte) 1, (byte) 10, (byte) 2).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5, (byte) 7, (byte) 9), result);

        result = ByteStream.range((byte) 10, (byte) 1, (byte) -2).toList();
        assertEquals(Arrays.asList((byte) 10, (byte) 8, (byte) 6, (byte) 4, (byte) 2), result);

        assertTrue(ByteStream.range((byte) 1, (byte) 5, (byte) -1).toList().isEmpty());
    }

    @Test
    public void testRangeClosedBy() {
        List<Byte> result = ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 2).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5, (byte) 7, (byte) 9), result);

        result = ByteStream.rangeClosed((byte) 1, (byte) 9, (byte) 2).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5, (byte) 7, (byte) 9), result);

        result = ByteStream.rangeClosed((byte) 10, (byte) 1, (byte) -2).toList();
        assertEquals(Arrays.asList((byte) 10, (byte) 8, (byte) 6, (byte) 4, (byte) 2), result);
    }

    @Test
    public void testIterateBooleanSupplierByteSupplier() {
        AtomicReference<Byte> current = new AtomicReference<>((byte) 0);
        List<Byte> result = ByteStream.iterate(() -> current.get() < 3, () -> {
            byte val = current.get();
            current.set((byte) (val + 1));
            return val;
        }).toList();
        assertEquals(Arrays.asList((byte) 0, (byte) 1, (byte) 2), result);
    }

    @Test
    public void testIterateInitBooleanSupplierByteUnaryOperator() {
        AtomicReference<Byte> currentVal = new AtomicReference<>((byte) 1);
        List<Byte> result = ByteStream.iterate((byte) 1, () -> currentVal.get() <= 5, b -> {
            currentVal.set((byte) (b + 1));
            return (byte) (b + 1);
        }).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), result);
    }

    @Test
    public void testIterateInitBytePredicateByteUnaryOperator() {
        List<Byte> result = ByteStream.iterate((byte) 1, b -> b < 5, b -> (byte) (b * 2)).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 4), result);
    }

    @Test
    public void testIterateInitByteUnaryOperator() {
        List<Byte> result = ByteStream.iterate((byte) 1, b -> (byte) (b + 1)).limit(3).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    @Test
    public void testConcatByteArrays() {
        List<Byte> result = ByteStream.concat(new byte[] { 1, 2 }, new byte[] { 3, 4, 5 }).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void testConcatByteIterators() {
        ByteIterator iter1 = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator iter2 = ByteIterator.of((byte) 3, (byte) 4);
        List<Byte> result = ByteStream.concat(iter1, iter2).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4), result);
    }

    @Test
    public void testConcatByteStreams() {
        ByteStream s1 = createByteStream((byte) 1, (byte) 2);
        ByteStream s2 = createByteStream((byte) 3, (byte) 4);
        List<Byte> result = ByteStream.concat(s1, s2).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4), result);
    }

    @Test
    public void testConcatListOfByteArrays() {
        List<byte[]> listOfArrays = new ArrayList<>();
        listOfArrays.add(new byte[] { 1, 2 });
        listOfArrays.add(new byte[] { 3, 4, 5 });
        List<Byte> result = ByteStream.concat(listOfArrays).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), result);
    }

    @Test
    public void testConcatCollectionOfByteStreams() {
        Collection<ByteStream> streams = new ArrayList<>();
        streams.add(createByteStream((byte) 1, (byte) 2));
        streams.add(createByteStream((byte) 3, (byte) 4));
        List<Byte> result = ByteStream.concat(streams).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4), result);
    }

    @Test
    public void testConcatIteratorsCollection() {
        Collection<ByteIterator> iterators = new ArrayList<>();
        iterators.add(ByteIterator.of((byte) 1, (byte) 2));
        iterators.add(ByteIterator.of((byte) 3, (byte) 4));
        List<Byte> result = ByteStream.concatIterators(iterators).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4), result);
    }

    @Test
    public void testZipByteArrays() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 10, 20, 30, 40 };
        List<Byte> result = ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) 33), result);
    }

    @Test
    public void testZipThreeByteArrays() {
        byte[] a = { 1, 2 };
        byte[] b = { 10, 20 };
        byte[] c = { 50, 60 };
        List<Byte> result = ByteStream.zip(a, b, c, (x, y, z) -> (byte) (x + y + z)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50), (byte) (2 + 20 + 60)), result);
    }

    @Test
    public void testZipByteIterators() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator b = ByteIterator.of((byte) 10, (byte) 20, (byte) 30);
        List<Byte> result = ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22), result);
    }

    @Test
    public void testZipThreeByteIterators() {
        ByteIterator a = ByteIterator.of((byte) 1);
        ByteIterator b = ByteIterator.of((byte) 10);
        ByteIterator c = ByteIterator.of((byte) 50, (byte) 60);
        List<Byte> result = ByteStream.zip(a, b, c, (x, y, z) -> (byte) (x + y + z)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50)), result);
    }

    @Test
    public void testZipByteStreams() {
        ByteStream a = createByteStream((byte) 1, (byte) 2);
        ByteStream b = createByteStream((byte) 10, (byte) 20, (byte) 30);
        List<Byte> result = ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22), result);
    }

    @Test
    public void testZipThreeByteStreams() {
        ByteStream a = createByteStream((byte) 1);
        ByteStream b = createByteStream((byte) 10);
        ByteStream c = createByteStream((byte) 50, (byte) 60);
        List<Byte> result = ByteStream.zip(a, b, c, (x, y, z) -> (byte) (x + y + z)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50)), result);
    }

    @Test
    public void testZipByteArraysWithDefaults() {
        byte[] a = { 1, 2 };
        byte[] b = { 10, 20, 30 };
        List<Byte> result = ByteStream.zip(a, b, (byte) 0, (byte) 0, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) 30), result);
    }

    @Test
    public void testZipThreeByteArraysWithDefaults() {
        byte[] a = { 1 };
        byte[] b = { 10, 20 };
        byte[] c = { 50, 60, 70 };
        List<Byte> result = ByteStream.zip(a, b, c, (byte) 0, (byte) 0, (byte) 0, (x, y, z) -> (byte) (x + y + z)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50), (byte) (0 + 20 + 60), (byte) (0 + 0 + 70)), result);
    }

    @Test
    public void testZipByteIteratorsWithDefaults() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator b = ByteIterator.of((byte) 10, (byte) 20, (byte) 30);
        List<Byte> result = ByteStream.zip(a, b, (byte) 0, (byte) 0, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) 30), result);
    }

    @Test
    public void testZipThreeByteIteratorsWithDefaults() {
        ByteIterator a = ByteIterator.of((byte) 1);
        ByteIterator b = ByteIterator.of((byte) 10, (byte) 20);
        ByteIterator c = ByteIterator.of((byte) 50, (byte) 60, (byte) 300);
        List<Byte> result = ByteStream.zip(a, b, c, (byte) 0, (byte) 0, (byte) 0, (x, y, z) -> (byte) (x + y + z)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50), (byte) (0 + 20 + 60), (byte) (0 + 0 + 300)), result);
    }

    @Test
    public void testZipByteStreamsWithDefaults() {
        ByteStream a = createByteStream((byte) 1, (byte) 2);
        ByteStream b = createByteStream((byte) 10, (byte) 20, (byte) 30);
        List<Byte> result = ByteStream.zip(a, b, (byte) 0, (byte) 0, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) 30), result);
    }

    @Test
    public void testZipThreeByteStreamsWithDefaults() {
        ByteStream a = createByteStream((byte) 1);
        ByteStream b = createByteStream((byte) 10, (byte) 20);
        ByteStream c = createByteStream((byte) 50, (byte) 60, (byte) 300);
        List<Byte> result = ByteStream.zip(a, b, c, (byte) 0, (byte) 0, (byte) 0, (x, y, z) -> (byte) (x + y + z)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50), (byte) (0 + 20 + 60), (byte) (0 + 0 + 300)), result);
    }

    @Test
    public void testZipCollectionOfStreamsWithDefaults() {
        Collection<ByteStream> streams = Arrays.asList(createByteStream((byte) 1), createByteStream((byte) 10, (byte) 20));
        byte[] defaults = { 0, 0 };
        ByteNFunction<Byte> zipFunction = bytes -> (byte) (bytes[0] + bytes[1]);
        List<Byte> result = ByteStream.zip(streams, defaults, zipFunction).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 20), result);
    }

    @Test
    public void testMergeByteArrays() {
        byte[] a = { 1, 5, 10 };
        byte[] b = { 2, 4, 11 };
        ByteBiFunction<MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = ByteStream.merge(a, b, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 4, (byte) 5, (byte) 10, (byte) 11), result);
    }

    @Test
    public void testMergeThreeByteArrays() {
        byte[] a = { 1, 7 };
        byte[] b = { 2, 5 };
        byte[] c = { 3, 4 };
        ByteBiFunction<MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = ByteStream.merge(a, b, c, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 7), result);
    }

    @Test
    public void testMergeByteIterators() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 5);
        ByteIterator b = ByteIterator.of((byte) 2, (byte) 6);
        ByteBiFunction<MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = ByteStream.merge(a, b, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 5, (byte) 6), result);
    }

    @Test
    public void testMergeThreeByteIterators() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 7);
        ByteIterator b = ByteIterator.of((byte) 2, (byte) 5);
        ByteIterator c = ByteIterator.of((byte) 3, (byte) 4);
        ByteBiFunction<MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = ByteStream.merge(a, b, c, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 7), result);
    }

    @Test
    public void testMergeByteStreams() {
        ByteStream a = createByteStream((byte) 1, (byte) 5);
        ByteStream b = createByteStream((byte) 2, (byte) 6);
        ByteBiFunction<MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = ByteStream.merge(a, b, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 5, (byte) 6), result);
    }

    @Test
    public void testMergeThreeByteStreams() {
        ByteStream a = createByteStream((byte) 1, (byte) 7);
        ByteStream b = createByteStream((byte) 2, (byte) 5);
        ByteStream c = createByteStream((byte) 3, (byte) 4);
        ByteBiFunction<MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = ByteStream.merge(a, b, c, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 7), result);
    }

    @Test
    public void testMergeCollectionOfByteStreams() {
        Collection<ByteStream> streams = new ArrayList<>();
        streams.add(createByteStream((byte) 1, (byte) 8));
        streams.add(createByteStream((byte) 2, (byte) 7));
        streams.add(createByteStream((byte) 3, (byte) 6));
        streams.add(createByteStream((byte) 4, (byte) 5));
        ByteBiFunction<MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = ByteStream.merge(streams, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8), result);
    }

    @Test
    public void testFilterWithActionOnDroppedItem() {
        byteStream = createByteStream(TEST_ARRAY);
        List<Byte> droppedItems = new ArrayList<>();
        List<Byte> result = byteStream.filter(b -> b % 2 == 0, droppedItems::add).toList();
        assertEquals(Arrays.asList((byte) 2, (byte) 4), result);
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5), droppedItems);
    }

    @Test
    public void testDropWhileWithActionOnDroppedItem() {
        byteStream = createByteStream(TEST_ARRAY);
        List<Byte> droppedItems = new ArrayList<>();
        List<Byte> result = byteStream.dropWhile(b -> b < 3, droppedItems::add).toList();
        assertEquals(Arrays.asList((byte) 3, (byte) 4, (byte) 5), result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2), droppedItems);
    }

    @Test
    public void testIntersection() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3, 4 });
        Collection<Byte> other = Arrays.asList((byte) 2, (byte) 2, (byte) 3, (byte) 5);
        List<Byte> result = byteStream.intersection(other).toList();
        assertEquals(Arrays.asList((byte) 2, (byte) 2, (byte) 3), result);
    }

    @Test
    public void testDifference() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3, 4 });
        Collection<Byte> other = Arrays.asList((byte) 2, (byte) 3, (byte) 5);
        List<Byte> result = byteStream.difference(other).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 4), result);
    }

    @Test
    public void testSymmetricDifference() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3, 4 });
        Collection<Byte> other = Arrays.asList((byte) 2, (byte) 3, (byte) 3, (byte) 5);
        List<Byte> result = byteStream.symmetricDifference(other).toList();
        List<Byte> expected = Arrays.asList((byte) 1, (byte) 2, (byte) 4, (byte) 3, (byte) 5);
        assertEquals(new HashSet<>(expected), new HashSet<>(result));
    }

    @Test
    public void testShuffledWithRandom() {
        byteStream = createByteStream(TEST_ARRAY);
        Random rnd = new Random(123);
        List<Byte> shuffled1 = byteStream.shuffled(rnd).toList();

        byteStream = createByteStream(TEST_ARRAY);
        rnd = new Random(123);
        List<Byte> shuffled2 = byteStream.shuffled(rnd).toList();
        assertEquals(shuffled1, shuffled2);
    }

    @Test
    public void testCycledWithRounds() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<Byte> result = byteStream.cycled(3).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 1, (byte) 2, (byte) 1, (byte) 2), result);

        assertTrue(createByteStream(new byte[] {}).cycled(3).toList().isEmpty());
        assertTrue(createByteStream(new byte[] { 1, 2 }).cycled(0).toList().isEmpty());
    }

    @Test
    public void testSkipWithAction() {
        byteStream = createByteStream(TEST_ARRAY);
        List<Byte> skippedItems = new ArrayList<>();
        List<Byte> result = byteStream.skip(2, skippedItems::add).toList();
        assertEquals(Arrays.asList((byte) 3, (byte) 4, (byte) 5), result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2), skippedItems);
    }

    @Test
    public void testRateLimited() {
        byteStream = createByteStream(TEST_ARRAY);
        long startTime = System.currentTimeMillis();
        List<Byte> result = byteStream.rateLimited(2.0).toList();
        long endTime = System.currentTimeMillis();
        assertEquals(5, result.size());
        assertTrue((endTime - startTime) >= 2000, "Rate limit was not applied or was too fast");
    }

    @Test
    public void testRateLimitedWithRateLimiter() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        RateLimiter limiter = RateLimiter.create(1.0);
        long startTime = System.currentTimeMillis();
        List<Byte> result = byteStream.rateLimited(limiter).toList();
        long endTime = System.currentTimeMillis();
        assertEquals(3, result.size());
        assertTrue((endTime - startTime) >= 2000, "Rate limiter not working as expected");
    }

    @Test
    public void testDelay() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        Duration delay = Duration.ofMillis(50);
        long startTime = System.currentTimeMillis();
        List<Byte> result = byteStream.delay(delay).toList();
        long endTime = System.currentTimeMillis();
        assertEquals(3, result.size());
        assertTrue((endTime - startTime) >= 2 * 50, "Delay was not applied or was too short");
    }

    @Test
    public void testThrowIfEmptyWithCustomException() {
        byteStream = createByteStream(new byte[] {});
        RuntimeException customException = new RuntimeException("Stream is empty!");
        try {
            byteStream.throwIfEmpty(() -> customException).toList();
            fail("Should throw custom exception");
        } catch (RuntimeException e) {
            assertSame(customException, e);
        }
    }

    @Test
    public void testIfEmpty() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        byteStream = createByteStream(new byte[] { 1, 2 });
        byteStream.ifEmpty(() -> actionExecuted.set(true)).toList();
        assertFalse(actionExecuted.get());

        actionExecuted.set(false);
        byteStream = createByteStream(new byte[] {});
        byteStream.ifEmpty(() -> actionExecuted.set(true)).toList();
        assertTrue(actionExecuted.get());
    }

    @Test
    public void testJoinDelimiter() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        String result = byteStream.join("-");
        assertEquals("1-2-3", result);

        result = createByteStream(new byte[] { 1 }).join("-");
        assertEquals("1", result);

        result = createByteStream(new byte[] {}).join("-");
        assertEquals("", result);
    }

    @Test
    public void testJoinDelimiterPrefixSuffix() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        String result = byteStream.join("-", "[", "]");
        assertEquals("[1-2-3]", result);

        result = createByteStream(new byte[] { 1 }).join("-", "[", "]");
        assertEquals("[1]", result);

        result = createByteStream(new byte[] {}).join("-", "[", "]");
        assertEquals("[]", result);
    }

    @Test
    public void testJoinToJoiner() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        Joiner joiner = Joiner.with(" | ", "<", ">");
        Joiner resultJoiner = byteStream.joinTo(joiner);
        assertEquals("<1 | 2 | 3>", resultJoiner.toString());
    }

    @Test
    public void testElementAt() {
        byteStream = createByteStream(new byte[] { 10, 20, 30 });
        OptionalByte result = byteStream.elementAt(1);
        assertTrue(result.isPresent());
        assertEquals((byte) 20, result.get());

        result = createByteStream(new byte[] { 10, 20, 30 }).elementAt(10);
        assertFalse(result.isPresent());
    }

    @Test
    public void testToArray() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        byte[] result = byteStream.toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);

        result = createByteStream(new byte[] {}).toArray();
        assertArrayEquals(new byte[] {}, result);
    }

    @Test
    public void testToImmutableList() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Byte> result = byteStream.toImmutableList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
        try {
            result.add((byte) 4);
            fail("Should be immutable");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testToImmutableSet() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3 });
        Set<Byte> result = byteStream.toImmutableSet();
        assertEquals(new HashSet<>(Arrays.asList((byte) 1, (byte) 2, (byte) 3)), result);
        try {
            result.add((byte) 4);
            fail("Should be immutable");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testToCollection() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ArrayList<Byte> result = byteStream.toCollection(ArrayList::new);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    @Test
    public void testToMultisetWithSupplier() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3, 3, 3 });
        Multiset<Byte> result = byteStream.toMultiset(Suppliers.ofMultiset());
        assertEquals(1, result.getCount((byte) 1));
        assertEquals(2, result.getCount((byte) 2));
        assertEquals(3, result.getCount((byte) 3));
    }

    @Test
    public void testPrintln() {
        assertDoesNotThrow(() -> {
            byteStream = createByteStream(new byte[] { 1, 2, 3 });
            byteStream.println();
        });
    }

    @Test
    public void testIsParallel() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        assertFalse(byteStream.isParallel());
        assertTrue(byteStream.parallel().isParallel());
    }

    @Test
    public void testSequential() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 }).parallel();
        assertTrue(byteStream.isParallel());
        assertFalse(byteStream.sequential().isParallel());
    }

    @Test
    public void testParallelMaxThreadNum() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteStream parallelStream = byteStream.parallel(2);
        assertTrue(parallelStream.isParallel());
    }

    @Test
    public void testParallelExecutor() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteStream parallelStream = byteStream.parallel(Runnable::run);
        assertTrue(parallelStream.isParallel());
    }

    @Test
    public void testParallelMaxThreadNumExecutor() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteStream parallelStream = byteStream.parallel(2, Runnable::run);
        assertTrue(parallelStream.isParallel());
    }

    @Test
    public void testParallelSettings() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteStream.ParallelSettings settings = PS.create(2);
        ByteStream parallelStream = byteStream.parallel(settings);
        assertTrue(parallelStream.isParallel());
    }

    @Test
    public void testSpsFunction() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Integer> result = byteStream.sps(s -> s.mapToInt(b -> b * 10)).toList();
        assertHaveSameElements(Arrays.asList(10, 20, 30), result);
        assertFalse(byteStream.isParallel());
    }

    @Test
    public void testSpsIntFunction() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Integer> result = byteStream.sps(2, s -> s.mapToInt(b -> b * 10)).toList();
        assertHaveSameElements(Arrays.asList(10, 20, 30), result);
        assertFalse(byteStream.isParallel());
    }

    @Test
    public void testSpsIntExecutorFunction() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Integer> result = byteStream.sps(2, Runnable::run, s -> s.mapToInt(b -> b * 10)).toList();
        assertEquals(Arrays.asList(10, 20, 30), result);
        assertFalse(byteStream.isParallel());
    }

    @Test
    public void testPspFunction() {
        byteStream = createByteStream(new byte[] { 3, 1, 2 }).parallel();
        List<Byte> result = byteStream.psp(s -> s.sorted()).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
        assertTrue(byteStream.isParallel());
    }

    @Test
    public void testTransform() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<String> result = byteStream.transform(s -> s.mapToObj(b -> "Item" + b)).toList();
        assertEquals(Arrays.asList("Item1", "Item2", "Item3"), result);
    }

    @Test
    public void testApplyIfNotEmpty() {
        Optional<Long> count = createByteStream(new byte[] { 1, 2, 3 }).applyIfNotEmpty(s -> s.count());
        assertTrue(count.isPresent());
        assertEquals(3L, count.get().longValue());

        count = createByteStream(new byte[] {}).applyIfNotEmpty(s -> s.count());
        assertFalse(count.isPresent());
    }

    @Test
    public void testAcceptIfNotEmpty() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        createByteStream(new byte[] { 1, 2 }).acceptIfNotEmpty(s -> actionExecuted.set(true)).orElse(() -> fail("Else should not be called"));
        assertTrue(actionExecuted.get());

        actionExecuted.set(false);
        createByteStream(new byte[] {}).acceptIfNotEmpty(s -> fail("Action should not be called")).orElse(() -> actionExecuted.set(true));
        assertTrue(actionExecuted.get());
    }

    @Test
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        byteStream = createByteStream(new byte[] { 1, 2, 3 }).onClose(() -> closed.set(true));
        byteStream.toList();
        assertTrue(closed.get());

        closed.set(false);
        byteStream = createByteStream(new byte[] { 1, 2, 3 }).onClose(() -> closed.set(true));
        byteStream.close();
        assertTrue(closed.get());
    }

    @Test
    public void testCloseIdempotent() {
        AtomicBoolean closed = new AtomicBoolean(false);
        byteStream = createByteStream(new byte[] { 1, 2, 3 }).onClose(() -> closed.set(true));
        byteStream.close();
        assertTrue(closed.get());
        closed.set(false);
        byteStream.close();
        assertFalse(closed.get());
    }

    @Test
    public void testFilterWithAction() {
        List<Byte> dropped = new ArrayList<>();
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2, dropped::add).count());

        dropped.clear();
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2, dropped::add).skip(1).count());

        dropped.clear();
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2, dropped::add).toArray());

        dropped.clear();
        assertArrayEquals(new byte[] { 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2, dropped::add).skip(1).toArray());

        dropped.clear();
        assertEquals(N.toList((byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2, dropped::add).toList());

        dropped.clear();
        assertEquals(N.toList((byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2, dropped::add).skip(1).toList());

        dropped.clear();
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2, dropped::add).count());

        dropped.clear();
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).count());

        dropped.clear();
        assertArrayEquals(new byte[] { 3, 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2, dropped::add).toArray());

        dropped.clear();
        assertArrayEquals(new byte[] { 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toArray());

        dropped.clear();
        assertEquals(N.toList((byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2, dropped::add).toList());

        dropped.clear();
        assertEquals(N.toList((byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toList());
    }

    @Test
    public void testDropWhileWithAction() {
        List<Byte> dropped = new ArrayList<>();
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3, dropped::add).count());

        dropped.clear();
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3, dropped::add).skip(1).count());

        dropped.clear();
        assertArrayEquals(new byte[] { 3, 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3, dropped::add).toArray());

        dropped.clear();
        assertArrayEquals(new byte[] { 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());

        dropped.clear();
        assertEquals(N.toList((byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3, dropped::add).toList());

        dropped.clear();
        assertEquals(N.toList((byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3, dropped::add).skip(1).toList());

        dropped.clear();
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).count());

        dropped.clear();
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).count());

        dropped.clear();
        assertArrayEquals(new byte[] { 3, 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toArray());

        dropped.clear();
        assertArrayEquals(new byte[] { 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());

        dropped.clear();
        assertEquals(N.toList((byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toList());

        dropped.clear();
        assertEquals(N.toList((byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toList());
    }

    @Test
    public void testFlatmapArray() {
        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmap(e -> new byte[] { e, (byte) (e * 2) }).count());
        assertEquals(9, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmap(e -> new byte[] { e, (byte) (e * 2) }).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 2, 4, 3, 6, 4, 8, 5, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmap(e -> new byte[] { e, (byte) (e * 2) }).toArray());
        assertArrayEquals(new byte[] { 2, 2, 4, 3, 6, 4, 8, 5, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmap(e -> new byte[] { e, (byte) (e * 2) }).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 2, (byte) 4, (byte) 3, (byte) 6, (byte) 4, (byte) 8, (byte) 5, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmap(e -> new byte[] { e, (byte) (e * 2) }).toList());
        assertEquals(N.toList((byte) 2, (byte) 2, (byte) 4, (byte) 3, (byte) 6, (byte) 4, (byte) 8, (byte) 5, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmap(e -> new byte[] { e, (byte) (e * 2) }).skip(1).toList());

        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatmap(e -> new byte[] { e, (byte) (e * 2) }).count());
        assertEquals(9,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatmap(e -> new byte[] { e, (byte) (e * 2) }).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 2, 4, 3, 6, 4, 8, 5, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatmap(e -> new byte[] { e, (byte) (e * 2) }).toArray());
        assertArrayEquals(new byte[] { 2, 2, 4, 3, 6, 4, 8, 5, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatmap(e -> new byte[] { e, (byte) (e * 2) }).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 2, (byte) 4, (byte) 3, (byte) 6, (byte) 4, (byte) 8, (byte) 5, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatmap(e -> new byte[] { e, (byte) (e * 2) }).toList());
        assertEquals(N.toList((byte) 2, (byte) 2, (byte) 4, (byte) 3, (byte) 6, (byte) 4, (byte) 8, (byte) 5, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatmap(e -> new byte[] { e, (byte) (e * 2) }).skip(1).toList());
    }

    @Test
    public void testFlattmapToObjArray() {
        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).count());
        assertEquals(9,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(1).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).toArray());
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(1).toArray());
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).toList());
        assertEquals(N.toList("B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(1).toList());
        assertEquals(10,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).count());
        assertEquals(9,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e })
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e })
                        .toArray());
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e })
                        .skip(1)
                        .toArray());
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).toList());
        assertEquals(N.toList("B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e })
                        .skip(1)
                        .toList());
    }

    @Test
    public void testCollapse() {
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).collapse((a, b) -> Math.abs(b - a) <= 1).count());
        assertEquals(1, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).collapse((a, b) -> Math.abs(b - a) <= 1).skip(1).count());

        Object[] arr = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).collapse((a, b) -> Math.abs(b - a) <= 1).toArray();
        assertEquals(2, arr.length);

        Object[] arr2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).collapse((a, b) -> Math.abs(b - a) <= 1).skip(1).toArray();
        assertEquals(1, arr2.length);

        List<ByteList> list = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).collapse((a, b) -> Math.abs(b - a) <= 1).toList();
        assertEquals(2, list.size());

        List<ByteList> list2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).collapse((a, b) -> Math.abs(b - a) <= 1).skip(1).toList();
        assertEquals(1, list2.size());

        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).map(e -> e).collapse((a, b) -> Math.abs(b - a) <= 1).count());
        assertEquals(1, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).map(e -> e).collapse((a, b) -> Math.abs(b - a) <= 1).skip(1).count());

        Object[] arr3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).map(e -> e).collapse((a, b) -> Math.abs(b - a) <= 1).toArray();
        assertEquals(2, arr3.length);

        Object[] arr4 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).map(e -> e).collapse((a, b) -> Math.abs(b - a) <= 1).skip(1).toArray();
        assertEquals(1, arr4.length);

        List<ByteList> list3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).map(e -> e).collapse((a, b) -> Math.abs(b - a) <= 1).toList();
        assertEquals(2, list3.size());

        List<ByteList> list4 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6)
                .map(e -> e)
                .collapse((a, b) -> Math.abs(b - a) <= 1)
                .skip(1)
                .toList();
        assertEquals(1, list4.size());
    }

    @Test
    public void testZipWithDefault() {
        assertEquals(3,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y)).count());
        assertEquals(2,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 5, 7, 13 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .toArray());
        assertArrayEquals(new byte[] { 7, 13 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((byte) 5, (byte) 7, (byte) 13),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y)).toList());
        assertEquals(N.toList((byte) 7, (byte) 13),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .skip(1)
                        .toList());
        assertEquals(3,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .count());
        assertEquals(2,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 5, 7, 13 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .toArray());
        assertArrayEquals(new byte[] { 7, 13 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((byte) 5, (byte) 7, (byte) 13),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .toList());
        assertEquals(N.toList((byte) 7, (byte) 13),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testZipWithThreeStreamsDefault() {
        assertEquals(4,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .count());
        assertEquals(3,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 12, 15, 22, 20 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .toArray());
        assertArrayEquals(new byte[] { 15, 22, 20 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((byte) 12, (byte) 15, (byte) 22, (byte) 20),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .toList());
        assertEquals(N.toList((byte) 15, (byte) 22, (byte) 20),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .toList());
        assertEquals(4,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .count());
        assertEquals(3,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 12, 15, 22, 20 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .toArray());
        assertArrayEquals(new byte[] { 15, 22, 20 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((byte) 12, (byte) 15, (byte) 22, (byte) 20),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .toList());
        assertEquals(N.toList((byte) 15, (byte) 22, (byte) 20),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testOfArrayWithIndices() {
        byte[] array = { 1, 2, 3, 4, 5, 6, 7 };
        assertEquals(3, ByteStream.of(array, 2, 5).count());
        assertEquals(2, ByteStream.of(array, 2, 5).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of(array, 2, 5).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of(array, 2, 5).skip(1).toArray());
        assertEquals(N.toList((byte) 3, (byte) 4, (byte) 5), ByteStream.of(array, 2, 5).toList());
        assertEquals(N.toList((byte) 4, (byte) 5), ByteStream.of(array, 2, 5).skip(1).toList());
        assertEquals(3, ByteStream.of(array, 2, 5).map(e -> e).count());
        assertEquals(2, ByteStream.of(array, 2, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of(array, 2, 5).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of(array, 2, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((byte) 3, (byte) 4, (byte) 5), ByteStream.of(array, 2, 5).map(e -> e).toList());
        assertEquals(N.toList((byte) 4, (byte) 5), ByteStream.of(array, 2, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfByteObjectArray() {
        Byte[] array = { 1, 2, 3, 4, 5 };
        assertEquals(5, ByteStream.of(array).count());
        assertEquals(4, ByteStream.of(array).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of(array).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of(array).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(array).toList());
        assertEquals(N.toList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(array).skip(1).toList());
        assertEquals(5, ByteStream.of(array).map(e -> e).count());
        assertEquals(4, ByteStream.of(array).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of(array).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of(array).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(array).map(e -> e).toList());
        assertEquals(N.toList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfByteObjectArrayWithIndices() {
        Byte[] array = { 1, 2, 3, 4, 5, 6, 7 };
        assertEquals(3, ByteStream.of(array, 2, 5).count());
        assertEquals(2, ByteStream.of(array, 2, 5).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of(array, 2, 5).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of(array, 2, 5).skip(1).toArray());
        assertEquals(N.toList((byte) 3, (byte) 4, (byte) 5), ByteStream.of(array, 2, 5).toList());
        assertEquals(N.toList((byte) 4, (byte) 5), ByteStream.of(array, 2, 5).skip(1).toList());
        assertEquals(3, ByteStream.of(array, 2, 5).map(e -> e).count());
        assertEquals(2, ByteStream.of(array, 2, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of(array, 2, 5).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of(array, 2, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((byte) 3, (byte) 4, (byte) 5), ByteStream.of(array, 2, 5).map(e -> e).toList());
        assertEquals(N.toList((byte) 4, (byte) 5), ByteStream.of(array, 2, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfIterator() {
        ByteIterator iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(5, ByteStream.of(iter).count());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(4, ByteStream.of(iter).skip(1).count());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of(iter).toArray());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of(iter).skip(1).toArray());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(iter).toList());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(iter).skip(1).toList());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(5, ByteStream.of(iter).map(e -> e).count());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(4, ByteStream.of(iter).map(e -> e).skip(1).count());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of(iter).map(e -> e).toArray());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of(iter).map(e -> e).skip(1).toArray());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(iter).map(e -> e).toList());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(iter).map(e -> e).skip(1).toList());
    }

    @Test
    public void testFlattenVertically() {
        byte[][] array = { { 1, 2, 3 }, { 4, 5, 6 } };
        assertEquals(6, ByteStream.flatten(array, true).count());
        assertEquals(5, ByteStream.flatten(array, true).skip(1).count());
        assertArrayEquals(new byte[] { 1, 4, 2, 5, 3, 6 }, ByteStream.flatten(array, true).toArray());
        assertArrayEquals(new byte[] { 4, 2, 5, 3, 6 }, ByteStream.flatten(array, true).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 4, (byte) 2, (byte) 5, (byte) 3, (byte) 6), ByteStream.flatten(array, true).toList());
        assertEquals(N.toList((byte) 4, (byte) 2, (byte) 5, (byte) 3, (byte) 6), ByteStream.flatten(array, true).skip(1).toList());
        assertEquals(6, ByteStream.flatten(array, true).map(e -> e).count());
        assertEquals(5, ByteStream.flatten(array, true).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 4, 2, 5, 3, 6 }, ByteStream.flatten(array, true).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 4, 2, 5, 3, 6 }, ByteStream.flatten(array, true).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 4, (byte) 2, (byte) 5, (byte) 3, (byte) 6), ByteStream.flatten(array, true).map(e -> e).toList());
        assertEquals(N.toList((byte) 4, (byte) 2, (byte) 5, (byte) 3, (byte) 6), ByteStream.flatten(array, true).map(e -> e).skip(1).toList());
    }

    @Test
    public void testFlattenWithAlignment() {
        byte[][] array = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        assertEquals(9, ByteStream.flatten(array, (byte) 0, true).count());
        assertEquals(8, ByteStream.flatten(array, (byte) 0, true).skip(1).count());
        assertArrayEquals(new byte[] { 1, 3, 6, 2, 4, 0, 0, 5, 0 }, ByteStream.flatten(array, (byte) 0, true).toArray());
        assertArrayEquals(new byte[] { 3, 6, 2, 4, 0, 0, 5, 0 }, ByteStream.flatten(array, (byte) 0, true).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 3, (byte) 6, (byte) 2, (byte) 4, (byte) 0, (byte) 0, (byte) 5, (byte) 0),
                ByteStream.flatten(array, (byte) 0, true).toList());
        assertEquals(N.toList((byte) 3, (byte) 6, (byte) 2, (byte) 4, (byte) 0, (byte) 0, (byte) 5, (byte) 0),
                ByteStream.flatten(array, (byte) 0, true).skip(1).toList());
        assertEquals(9, ByteStream.flatten(array, (byte) 0, true).map(e -> e).count());
        assertEquals(8, ByteStream.flatten(array, (byte) 0, true).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 3, 6, 2, 4, 0, 0, 5, 0 }, ByteStream.flatten(array, (byte) 0, true).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 3, 6, 2, 4, 0, 0, 5, 0 }, ByteStream.flatten(array, (byte) 0, true).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 3, (byte) 6, (byte) 2, (byte) 4, (byte) 0, (byte) 0, (byte) 5, (byte) 0),
                ByteStream.flatten(array, (byte) 0, true).map(e -> e).toList());
        assertEquals(N.toList((byte) 3, (byte) 6, (byte) 2, (byte) 4, (byte) 0, (byte) 0, (byte) 5, (byte) 0),
                ByteStream.flatten(array, (byte) 0, true).map(e -> e).skip(1).toList());
    }

    @Test
    public void testIterate() {
        assertEquals(5, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).count());
        assertEquals(4, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).toList());
        assertEquals(N.toList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).skip(1).toList());
        assertEquals(5, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).count());
        assertEquals(4, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).toList());
        assertEquals(N.toList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).skip(1).toList());
    }

    @Test
    public void testSkipWithActionOnSkippedItem() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        List<Byte> skipped = new ArrayList<>();

        ByteStream result = stream.skip(3, skipped::add);
        assertArrayEquals(new byte[] { 4, 5 }, result.toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), skipped);
    }

    @Test
    public void testCollapseWithTriPredicate() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6, (byte) 10);
        ByteTriPredicate collapsible = (first, prev, curr) -> curr - first < 3;
        ByteBinaryOperator mergeFunction = (a, b) -> (byte) (a + b);

        ByteStream collapsed = stream.collapse(collapsible, mergeFunction);
        assertArrayEquals(new byte[] { 6, 11, 10 }, collapsed.toArray());
    }

    @Test
    public void testPrependStream() {
        ByteStream main = createByteStream((byte) 3, (byte) 4, (byte) 5);
        ByteStream toPrepend = createByteStream((byte) 1, (byte) 2);

        ByteStream result = main.prepend(toPrepend);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testAppendStream() {
        ByteStream main = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream toAppend = createByteStream((byte) 4, (byte) 5);

        ByteStream result = main.append(toAppend);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testAppendIfEmptySupplier() {
        ByteStream empty = ByteStream.empty();
        ByteStream result1 = empty.appendIfEmpty(() -> createByteStream((byte) 1, (byte) 2));
        assertArrayEquals(new byte[] { 1, 2 }, result1.toArray());

        ByteStream nonEmpty = createByteStream((byte) 3);
        ByteStream result2 = nonEmpty.appendIfEmpty(() -> createByteStream((byte) 1, (byte) 2));
        assertArrayEquals(new byte[] { 3 }, result2.toArray());
    }

    @Test
    public void testDefaultIfEmpty() {
        ByteStream empty = ByteStream.empty();
        ByteStream result1 = empty.defaultIfEmpty(() -> createByteStream((byte) 1, (byte) 2));
        assertArrayEquals(new byte[] { 1, 2 }, result1.toArray());

        ByteStream nonEmpty = createByteStream((byte) 3);
        ByteStream result2 = nonEmpty.defaultIfEmpty(() -> createByteStream((byte) 1, (byte) 2));
        assertArrayEquals(new byte[] { 3 }, result2.toArray());
    }

    @Test
    public void testThrowIfEmptyWithSupplier() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2 }, stream.throwIfEmpty(() -> new RuntimeException("Should not throw")).toArray());

        ByteStream empty = ByteStream.empty();
        assertThrows(IllegalStateException.class, () -> empty.throwIfEmpty(() -> new IllegalStateException("Empty stream")).toArray());
    }

    @Test
    public void testRateLimitedWithPermitsPerSecond() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);

        long startTime = System.currentTimeMillis();
        byte[] result = stream.rateLimited(100).toArray();
        long duration = System.currentTimeMillis() - startTime;

        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
        assertTrue(duration >= 0);
    }

    @Test
    public void testToMapWithMergeFunction() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 1, (byte) 3);

        var map = stream.toMap(b -> b % 2 == 0 ? "even" : "odd", b -> (int) b, (v1, v2) -> v1 + v2);

        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(5), map.get("odd"));
        assertEquals(Integer.valueOf(2), map.get("even"));
    }

    @Test
    public void testToMapWithSupplier() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);

        var map = stream.toMap(b -> "key" + b, b -> (int) b, Suppliers.ofLinkedHashMap());

        assertEquals(3, map.size());
        assertTrue(map instanceof java.util.LinkedHashMap);
    }

    @Test
    public void testToMapWithMergeFunctionAndSupplier() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 1, (byte) 3);

        var map = stream.toMap(b -> b % 2 == 0 ? "even" : "odd", b -> (int) b, (v1, v2) -> v1 + v2, java.util.TreeMap::new);

        assertEquals(2, map.size());
        assertTrue(map instanceof java.util.TreeMap);
        assertEquals(Integer.valueOf(5), map.get("odd"));
        assertEquals(Integer.valueOf(2), map.get("even"));
    }

    @Test
    public void testGroupToWithSupplier() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);

        var grouped = stream.groupTo(b -> b % 2 == 0 ? "even" : "odd", Collectors.counting(), java.util.TreeMap::new);

        assertTrue(grouped instanceof java.util.TreeMap);
        assertEquals(2, grouped.size());
        assertEquals(Long.valueOf(3), grouped.get("odd"));
        assertEquals(Long.valueOf(2), grouped.get("even"));
    }

    @Test
    public void testCollectWithSupplierOnly() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);

        ByteList list = stream.collect(ByteList::new, ByteList::add);

        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void testZipWithDefaults2Streams() {
        ByteStream a = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = createByteStream((byte) 4, (byte) 5);

        ByteStream result = a.zipWith(b, (byte) 10, (byte) 20, (x, y) -> (byte) (x + y));
        assertArrayEquals(new byte[] { 5, 7, 23 }, result.toArray());
    }

    @Test
    public void testZipWithDefaults3Streams() {
        ByteStream a = createByteStream((byte) 1, (byte) 2);
        ByteStream b = createByteStream((byte) 3);
        ByteStream c = createByteStream((byte) 5, (byte) 6, (byte) 7);

        ByteStream result = a.zipWith(b, c, (byte) 10, (byte) 20, (byte) 30, (x, y, z) -> (byte) (x + y + z));
        assertArrayEquals(new byte[] { 9, 28, 37 }, result.toArray());
    }

    @Test
    public void testZipMultipleStreams() {
        List<ByteStream> streams = Arrays.asList(createByteStream((byte) 1, (byte) 2), createByteStream((byte) 3, (byte) 4),
                createByteStream((byte) 5, (byte) 6));

        ByteNFunction<Byte> zipFunction = values -> {
            byte sum = 0;
            for (byte b : values) {
                sum += b;
            }
            return sum;
        };

        ByteStream result = ByteStream.zip(streams, zipFunction);
        assertArrayEquals(new byte[] { 9, 12 }, result.toArray());
    }

    @Test
    public void testZipMultipleStreamsWithDefaults() {
        List<ByteStream> streams = Arrays.asList(createByteStream((byte) 1, (byte) 2, (byte) 3), createByteStream((byte) 4),
                createByteStream((byte) 5, (byte) 6));

        byte[] defaults = { 10, 20, 30 };

        ByteNFunction<Byte> zipFunction = values -> {
            byte sum = 0;
            for (byte b : values) {
                sum += b;
            }
            return sum;
        };

        ByteStream result = ByteStream.zip(streams, defaults, zipFunction);
        assertArrayEquals(new byte[] { 10, 28, 53 }, result.toArray());
    }

    @Test
    public void testMergeMultipleStreams() {
        List<ByteStream> streams = Arrays.asList(createByteStream((byte) 1, (byte) 5), createByteStream((byte) 2, (byte) 6),
                createByteStream((byte) 3, (byte) 7), createByteStream((byte) 4, (byte) 8));

        ByteBiFunction<MergeResult> selector = (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ByteStream result = ByteStream.merge(streams, selector);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, result.toArray());
    }

    @Test
    public void testFindAnyDefault() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        OptionalByte result = stream.first();

        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testParallelWithMaxThreadNum() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream parallel = stream.parallel(2);

        assertTrue(parallel.isParallel());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, parallel.toArray());
    }

    @Test
    public void testParallelWithExecutor() {
        Executor executor = Executors.newFixedThreadPool(2);
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream parallel = stream.parallel(executor);

        assertTrue(parallel.isParallel());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, parallel.toArray());

        ((java.util.concurrent.ExecutorService) executor).shutdown();
    }

    @Test
    public void testParallelWithMaxThreadNumAndExecutor() {
        Executor executor = Executors.newFixedThreadPool(3);
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream parallel = stream.parallel(2, executor);

        assertTrue(parallel.isParallel());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, parallel.toArray());

        ((java.util.concurrent.ExecutorService) executor).shutdown();
    }

    @Test
    public void testSps() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);

        ByteStream result = stream.sps(s -> s.map(b -> (byte) (b * 2)));

        assertFalse(result.isParallel());
        assertArrayEquals(new byte[] { 2, 4, 6, 8, 10 }, result.sorted().toArray());
    }

    @Test
    public void testSpsWithMaxThreadNum() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);

        ByteStream result = stream.sps(2, s -> s.map(b -> (byte) (b * 2)));

        assertFalse(result.isParallel());
        assertArrayEquals(new byte[] { 2, 4, 6, 8, 10 }, result.sorted().toArray());
    }

    @Test
    public void testPsp() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);

        ByteStream result = stream.parallel().psp(s -> s.filter(b -> b % 2 == 0));

        assertTrue(result.isParallel());
        assertArrayEquals(new byte[] { 2, 4 }, result.sorted().toArray());
    }

    //    @Test
    //    public void testDoubleUnderscore() {
    //        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
    //
    //        IntStream intStream = stream.__(s -> s.mapToInt(b -> b * 10));
    //
    //        assertArrayEquals(new int[] { 10, 20, 30 }, intStream.toArray());
    //    }

    @Test
    public void testWithCloseHandlerInConstructor() {
        AtomicInteger closeCount = new AtomicInteger(0);
        List<Runnable> closeHandlers = Arrays.asList(() -> closeCount.incrementAndGet(), () -> closeCount.addAndGet(10));

        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3).onClose(closeHandlers.get(0)).onClose(closeHandlers.get(1));

        stream.close();
        assertEquals(11, closeCount.get());
    }

    @Test
    public void testFlattenArray() {
        byte[][] array = { { 1, 2 }, { 3, 4 }, { 5 } };
        ByteStream stream = ByteStream.flatten(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());

        ByteStream verticalStream = ByteStream.flatten(array, true);
        assertArrayEquals(new byte[] { 1, 3, 5, 2, 4 }, verticalStream.toArray());

        ByteStream alignedStream = ByteStream.flatten(array, (byte) 0, false);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 0 }, alignedStream.toArray());
    }

    @Test
    public void testConcatList() {
        List<byte[]> list = Arrays.asList(new byte[] { 1, 2 }, new byte[] { 3, 4 }, new byte[] { 5 });

        ByteStream stream = ByteStream.concat(list);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testConcatCollection() {
        Collection<ByteStream> streams = Arrays.asList(createByteStream((byte) 1, (byte) 2), createByteStream((byte) 3, (byte) 4), createByteStream((byte) 5));

        ByteStream stream = ByteStream.concat(streams);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testZipThreeArrays() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 4, 5, 6 };
        byte[] c = { 7, 8, 9 };

        ByteStream stream = ByteStream.zip(a, b, c, (x, y, z) -> (byte) (x + y + z));
        assertArrayEquals(new byte[] { 12, 15, 18 }, stream.toArray());
    }

    @Test
    public void testReduceWithoutIdentity() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        OptionalByte result = stream.reduce((a, b) -> (byte) (a + b));
        assertTrue(result.isPresent());
        assertEquals(10, result.get());

        ByteStream empty = ByteStream.empty();
        OptionalByte emptyResult = empty.reduce((a, b) -> (byte) (a + b));
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindFirstWithPredicate() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        OptionalByte result = stream.findFirst(b -> b > 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());

        ByteStream stream2 = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        OptionalByte notFound = stream2.findFirst(b -> b > 10);
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testJoinTo() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        Joiner joiner = Joiner.with(", ", "[", "]");
        stream.joinTo(joiner);
        assertEquals("[1, 2, 3]", joiner.toString());
    }

    @Test
    public void testClose() {
        AtomicInteger closed = new AtomicInteger(0);
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3).onClose(() -> closed.incrementAndGet());

        assertEquals(0, closed.get());
        stream.close();
        assertEquals(1, closed.get());

        stream.close();
        assertEquals(1, closed.get());
    }

    @Test
    public void testParallel() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertFalse(stream.isParallel());

        ByteStream parallel = stream.parallel();
        assertTrue(parallel.isParallel());

        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, parallel.sorted().toArray());
    }

    // --- Missing dedicated test methods ---

    @Test
    public void testOfInputStreamWithCloseFlag() throws IOException {
        byte[] data = { 10, 20, 30 };
        InputStream is = new ByteArrayInputStream(data);
        ByteStream stream = ByteStream.of(is, true);
        assertArrayEquals(data, stream.toArray());
        stream.close();
    }

    @Test
    public void testOfInputStreamWithCloseFlagFalse() throws IOException {
        byte[] data = { 1, 2, 3 };
        InputStream is = new ByteArrayInputStream(data);
        ByteStream stream = ByteStream.of(is, false);
        assertArrayEquals(data, stream.toArray());
        stream.close();
    }

    @Test
    public void testOfNullInputStream() {
        ByteStream stream = ByteStream.of((InputStream) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullByteBuffer() {
        ByteStream stream = ByteStream.of((ByteBuffer) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullByteIterator() {
        ByteStream stream = ByteStream.of((ByteIterator) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullableWithNonNull() {
        ByteStream stream = ByteStream.ofNullable((byte) 42);
        assertEquals(1, stream.count());
    }

    @Test
    public void testOfNullableWithNull() {
        ByteStream stream = ByteStream.ofNullable(null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFlatMapArrayToObjDedicated() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        List<String> result = stream.flatMapArrayToObj(b -> new String[] { "x" + b, "y" + b }).toList();
        assertEquals(4, result.size());
        assertEquals("x1", result.get(0));
        assertEquals("y1", result.get(1));
        assertEquals("x2", result.get(2));
        assertEquals("y2", result.get(3));
    }

    @Test
    public void testFindFirstNoPredicate() {
        OptionalByte result = createByteStream((byte) 7, (byte) 8, (byte) 9).findFirst();
        assertTrue(result.isPresent());
        assertEquals((byte) 7, result.getAsByte());
    }

    @Test
    public void testFindFirstNoPredicate_EmptyStream() {
        OptionalByte result = ByteStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAnyNoPredicate() {
        OptionalByte result = createByteStream((byte) 7, (byte) 8).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAnyNoPredicate_EmptyStream() {
        OptionalByte result = ByteStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testRepeatZero() {
        ByteStream stream = ByteStream.repeat((byte) 5, 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRepeatNegative() {
        assertThrows(IllegalArgumentException.class, () -> ByteStream.repeat((byte) 5, -1));
    }

    @Test
    public void testDeferNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ByteStream.defer(null));
    }

    @Test
    public void testGenerateNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ByteStream.generate(null));
    }

    @Test
    public void testIterateNullOperator() {
        assertThrows(IllegalArgumentException.class, () -> ByteStream.iterate((byte) 0, (com.landawn.abacus.util.function.ByteUnaryOperator) null));
    }

    @Test
    public void testEmptyStreamMin() {
        assertFalse(ByteStream.empty().min().isPresent());
    }

    @Test
    public void testEmptyStreamMax() {
        assertFalse(ByteStream.empty().max().isPresent());
    }

    @Test
    public void testEmptyStreamAverage() {
        assertFalse(ByteStream.empty().average().isPresent());
    }

    @Test
    public void testEmptyStreamSum() {
        assertEquals(0, ByteStream.empty().sum());
    }

    @Test
    public void testEmptyStreamReduce() {
        assertFalse(ByteStream.empty().reduce((a, b) -> (byte) (a + b)).isPresent());
    }

    @Test
    public void testReduceWithIdentityOnEmpty() {
        byte result = ByteStream.empty().reduce((byte) 10, (a, b) -> (byte) (a + b));
        assertEquals((byte) 10, result);
    }

    @Test
    public void testEmptyStreamSummaryStatistics() {
        ByteSummaryStatistics stats = ByteStream.empty().summaryStatistics();
        assertNotNull(stats);
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testEmptyStreamSummaryStatisticsAndPercentiles() {
        Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> result = ByteStream.empty().summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertEquals(0, result.left().getCount());
    }

    @Test
    public void testLimitWithOffset() {
        byte[] result = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).limit(1, 3).toArray();
        assertArrayEquals(new byte[] { 2, 3, 4 }, result);
    }

    @Test
    public void testRangeEmptyRange() {
        assertEquals(0, ByteStream.range((byte) 5, (byte) 5).count());
    }

    @Test
    public void testRangeClosedSingleElement() {
        assertArrayEquals(new byte[] { 5 }, ByteStream.rangeClosed((byte) 5, (byte) 5).toArray());
    }

    @Test
    public void testDelayWithJavaDuration() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2).delay(java.time.Duration.ofMillis(1));
        assertArrayEquals(new byte[] { 1, 2 }, stream.toArray());
    }

    @Test
    public void testPeekIsAliasForOnEach() {
        List<Byte> peeked = new ArrayList<>();
        byte[] result = createByteStream((byte) 1, (byte) 2, (byte) 3).peek(b -> peeked.add(b)).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
        assertEquals(3, peeked.size());
    }

    @Test
    public void testDefaultIfEmptyOnNonEmpty() {
        byte[] result = createByteStream((byte) 1, (byte) 2).defaultIfEmpty(() -> ByteStream.of((byte) 99)).toArray();
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testDefaultIfEmptyOnEmpty() {
        byte[] result = ByteStream.empty().defaultIfEmpty(() -> ByteStream.of((byte) 99)).toArray();
        assertArrayEquals(new byte[] { 99 }, result);
    }

    @Test
    public void testAppendIfEmptyWithSupplierOnEmpty() {
        byte[] result = ByteStream.empty().appendIfEmpty(() -> ByteStream.of((byte) 77)).toArray();
        assertArrayEquals(new byte[] { 77 }, result);
    }

    @Test
    public void testAppendIfEmptyWithSupplierOnNonEmpty() {
        byte[] result = createByteStream((byte) 1).appendIfEmpty(() -> ByteStream.of((byte) 77)).toArray();
        assertArrayEquals(new byte[] { 1 }, result);
    }

    @Test
    public void testRateLimitedWithPermitsPerSecondDouble() {
        byte[] result = createByteStream((byte) 1, (byte) 2, (byte) 3).rateLimited(1000.0).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testJoinNoArgs() {
        // join(delimiter) default method
        String result = createByteStream((byte) 1, (byte) 2, (byte) 3).join(", ");
        assertNotNull(result);
        assertTrue(result.contains("1"));
        assertTrue(result.contains("2"));
        assertTrue(result.contains("3"));
    }

    @Test
    public void testToMultisetWithDefaultSupplier() {
        Multiset<Byte> multiset = createByteStream((byte) 1, (byte) 2, (byte) 1).toMultiset();
        assertEquals(2, multiset.get((byte) 1));
        assertEquals(1, multiset.get((byte) 2));
    }

    @Test
    public void testConcatEmptyArrays() {
        ByteStream stream = ByteStream.concat(new byte[0], new byte[0]);
        assertEquals(0, stream.count());
    }

    @Test
    public void testConcatNullHandling() {
        ByteStream stream = ByteStream.concat((byte[][]) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testZipWithThreeStreamsDedicated() {
        ByteStream a = createByteStream((byte) 1, (byte) 2);
        ByteStream b = createByteStream((byte) 10, (byte) 20);
        ByteStream c = createByteStream((byte) 100);
        byte[] result = a.zipWith(b, c, (x, y, z) -> (byte) (x + y + z)).toArray();
        assertArrayEquals(new byte[] { 111 }, result);
    }

    @Test
    public void testZipWithThreeStreamsWithDefaultsDedicated() {
        ByteStream a = createByteStream((byte) 1, (byte) 2);
        ByteStream b = createByteStream((byte) 10);
        ByteStream c = createByteStream((byte) 100);
        byte[] result = a.zipWith(b, c, (byte) 0, (byte) 0, (byte) 0, (x, y, z) -> (byte) (x + y + z)).toArray();
        assertArrayEquals(new byte[] { 111, 2 }, result);
    }

    @Test
    public void testMergeWithDedicated() {
        ByteStream a = createByteStream((byte) 1, (byte) 3, (byte) 5);
        ByteStream b = ByteStream.of((byte) 2, (byte) 4, (byte) 6);
        byte[] result = a.mergeWith(b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    // ========== New tests for previously untested methods ==========

    @Test
    public void testElementAt_FirstElement() {
        ByteStream stream = createByteStream((byte) 10, (byte) 20, (byte) 30);
        OptionalByte result = stream.elementAt(0);
        assertTrue(result.isPresent());
        assertEquals((byte) 10, result.get());
    }

    @Test
    public void testElementAt_OutOfBounds() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        OptionalByte result = stream.elementAt(5);
        assertFalse(result.isPresent());
    }

    @Test
    public void testElementAt_NegativePosition() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        assertThrows(IllegalArgumentException.class, () -> stream.elementAt(-1));
    }

    @Test
    public void testIfEmpty_NonEmpty() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        AtomicBoolean called = new AtomicBoolean(false);
        byte[] result = stream.ifEmpty(() -> called.set(true)).toArray();
        assertFalse(called.get());
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testIfEmpty_Empty() {
        ByteStream stream = ByteStream.empty();
        AtomicBoolean called = new AtomicBoolean(false);
        ByteStream result = stream.ifEmpty(() -> called.set(true));
        result.count(); // terminal op to trigger
        assertTrue(called.get());
    }

    @Test
    public void testPrintln_EmptyStream() {
        assertDoesNotThrow(() -> ByteStream.empty().println());
    }

    @Test
    public void testThrowIfEmpty_NonEmpty() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        byte[] result = stream.throwIfEmpty().toArray();
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testThrowIfEmpty_Empty() {
        ByteStream stream = ByteStream.empty();
        assertThrows(NoSuchElementException.class, () -> stream.throwIfEmpty().toArray());
    }

    @Test
    public void testSps_WithMaxThreadNum() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        byte[] result = stream.sps(2, s -> s.filter(b -> b > 2)).toArray();
        assertHaveSameElements(new byte[] { 3, 4 }, result);
    }

    @Test
    public void testParallel_WithMaxThreadNum() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream result = stream.parallel(2);
        assertNotNull(result);
        assertTrue(result.isParallel());
    }

    @Test
    public void testParallel_WithExecutor() {
        Executor executor = Executors.newFixedThreadPool(2);
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream result = stream.parallel(executor);
        assertNotNull(result);
        assertTrue(result.isParallel());
    }

    @Test
    public void testParallel_WithMaxThreadNumAndExecutor() {
        Executor executor = Executors.newFixedThreadPool(2);
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream result = stream.parallel(2, executor);
        assertNotNull(result);
        assertTrue(result.isParallel());
    }

    @Test
    public void testLimitWithOffset_ZeroOffset() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        byte[] result = stream.limit(0, 2).toArray();
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testReverseSorted_Verify() {
        ByteStream stream = createByteStream((byte) 1, (byte) 3, (byte) 2);
        byte[] result = stream.reverseSorted().toArray();
        assertArrayEquals(new byte[] { 3, 2, 1 }, result);
    }

    @Test
    public void testShuffled_WithRandom() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        byte[] result = stream.shuffled(new Random(42)).toArray();
        assertEquals(5, result.length);
        // Verify all elements are present
        Arrays.sort(result);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testCycled_ZeroRounds() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        byte[] result = stream.cycled(0).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testCycled_MultipleRounds() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        byte[] result = stream.cycled(3).toArray();
        assertArrayEquals(new byte[] { 1, 2, 1, 2, 1, 2 }, result);
    }

    @Test
    public void testStep_StepTwo() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        byte[] result = stream.step(2).toArray();
        assertArrayEquals(new byte[] { 1, 3, 5 }, result);
    }

    @Test
    public void testStep_StepOne() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        byte[] result = stream.step(1).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testSkip_WithAction() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        List<Byte> skipped = new ArrayList<>();
        byte[] result = stream.skip(2, b -> skipped.add(b)).toArray();
        assertArrayEquals(new byte[] { 3, 4, 5 }, result);
        assertEquals(2, skipped.size());
        assertEquals(Byte.valueOf((byte) 1), skipped.get(0));
        assertEquals(Byte.valueOf((byte) 2), skipped.get(1));
    }

    @Test
    public void testDelay_WithDuration() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        byte[] result = stream.delay(Duration.ofMillis(1)).toArray();
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testDefaultIfEmpty_OnEmpty() {
        ByteStream stream = ByteStream.empty();
        byte[] result = stream.defaultIfEmpty(() -> ByteStream.of((byte) 99)).toArray();
        assertArrayEquals(new byte[] { 99 }, result);
    }

    @Test
    public void testDefaultIfEmpty_OnNonEmpty() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        byte[] result = stream.defaultIfEmpty(() -> ByteStream.of((byte) 99)).toArray();
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testAppendIfEmpty_WithSupplier() {
        ByteStream stream = ByteStream.empty();
        byte[] result = stream.appendIfEmpty(() -> ByteStream.of((byte) 10, (byte) 20)).toArray();
        assertArrayEquals(new byte[] { 10, 20 }, result);
    }

    @Test
    public void testAppendIfEmpty_WithSupplier_NonEmpty() {
        ByteStream stream = createByteStream((byte) 1);
        byte[] result = stream.appendIfEmpty(() -> ByteStream.of((byte) 10, (byte) 20)).toArray();
        assertArrayEquals(new byte[] { 1 }, result);
    }

    @Test
    public void testFindFirst_NoPredicate() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        OptionalByte result = stream.findFirst();
        assertTrue(result.isPresent());
        assertEquals((byte) 1, result.get());
    }

    @Test
    public void testFindFirst_NoPredicate_Empty() {
        OptionalByte result = ByteStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAny_NoPredicate() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        OptionalByte result = stream.findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_NoPredicate_Empty() {
        OptionalByte result = ByteStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLast_WithPredicate() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        OptionalByte result = stream.findLast(b -> b < 3);
        assertTrue(result.isPresent());
        assertEquals((byte) 2, result.get());
    }

    @Test
    public void testFindLast_NoMatch() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        OptionalByte result = stream.findLast(b -> b > 100);
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest_First() {
        ByteStream stream = createByteStream((byte) 1, (byte) 3, (byte) 5, (byte) 2, (byte) 4);
        OptionalByte result = stream.kthLargest(1);
        assertTrue(result.isPresent());
        assertEquals((byte) 5, result.get());
    }

    @Test
    public void testKthLargest_Second() {
        ByteStream stream = createByteStream((byte) 1, (byte) 3, (byte) 5, (byte) 2, (byte) 4);
        OptionalByte result = stream.kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals((byte) 4, result.get());
    }

    @Test
    public void testSummaryStatistics_Verify() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(3, stats.getCount());
        assertEquals(1, stats.getMin());
        assertEquals(3, stats.getMax());
    }

    @Test
    public void testSummaryStatisticsAndPercentiles_Verify() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> result = stream.summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertEquals(5, result.left().getCount());
    }

    @Test
    public void testAsIntStream_Verify() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        int[] result = stream.asIntStream().toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testBoxed_Verify() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        List<Byte> result = stream.boxed().toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    @Test
    public void testMergeWith_Verify() {
        ByteStream a = createByteStream((byte) 1, (byte) 3, (byte) 5);
        ByteStream b = ByteStream.of((byte) 2, (byte) 4, (byte) 6);
        byte[] result = a.mergeWith(b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testZipWith_TwoStreams() {
        ByteStream a = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = ByteStream.of((byte) 10, (byte) 20, (byte) 30);
        byte[] result = a.zipWith(b, (x, y) -> (byte) (x + y)).toArray();
        assertArrayEquals(new byte[] { 11, 22, 33 }, result);
    }

    @Test
    public void testZipWith_ThreeStreams() {
        ByteStream a = createByteStream((byte) 1, (byte) 2);
        ByteStream b = ByteStream.of((byte) 3, (byte) 4);
        ByteStream c = ByteStream.of((byte) 5, (byte) 6);
        byte[] result = a.zipWith(b, c, (x, y, z) -> (byte) (x + y + z)).toArray();
        assertArrayEquals(new byte[] { 9, 12 }, result);
    }

    @Test
    public void testZipWith_WithDefaults() {
        ByteStream a = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = ByteStream.of((byte) 10);
        byte[] result = a.zipWith(b, (byte) 0, (byte) 0, (x, y) -> (byte) (x + y)).toArray();
        assertArrayEquals(new byte[] { 11, 2, 3 }, result);
    }

    @Test
    public void testOnClose_MultipleHandlers() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        List<String> actions = new ArrayList<>();
        stream.onClose(() -> actions.add("first")).onClose(() -> actions.add("second"));
        stream.close();
        assertEquals(2, actions.size());
        assertEquals("first", actions.get(0));
        assertEquals("second", actions.get(1));
    }

    @Test
    public void testStreamReuseThrowsException() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        stream.toArray(); // consume
        assertThrows(IllegalStateException.class, () -> stream.toArray());
    }

    @Test
    public void testForEachIndexed_Verify() {
        ByteStream stream = createByteStream((byte) 10, (byte) 20, (byte) 30);
        List<String> collected = new ArrayList<>();
        stream.forEachIndexed((idx, b) -> collected.add(idx + ":" + b));
        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), collected);
    }

    @Test
    public void testMin_EmptyStream() {
        OptionalByte result = ByteStream.empty().min();
        assertFalse(result.isPresent());
    }

    @Test
    public void testMax_EmptyStream() {
        OptionalByte result = ByteStream.empty().max();
        assertFalse(result.isPresent());
    }

    @Test
    public void testSum_EmptyStream() {
        int result = ByteStream.empty().sum();
        assertEquals(0, result);
    }

    @Test
    public void testAverage_EmptyStream() {
        OptionalDouble result = ByteStream.empty().average();
        assertFalse(result.isPresent());
    }

    @Test
    public void testRateLimited_WithRateLimiter() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        RateLimiter rateLimiter = RateLimiter.create(1000);
        byte[] result = stream.rateLimited(rateLimiter).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testFlatten_2DArray() {
        byte[][] a = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        byte[] result = ByteStream.flatten(a).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testFlatten_3DArray() {
        byte[][][] a = { { { 1, 2 }, { 3 } }, { { 4, 5, 6 } } };
        byte[] result = ByteStream.flatten(a).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testFlatten_Vertically() {
        byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
        byte[] result = ByteStream.flatten(a, true).toArray();
        assertArrayEquals(new byte[] { 1, 4, 2, 5, 3, 6 }, result);
    }

    @Test
    public void testFlatten_WithAlignment() {
        byte[][] a = { { 1, 2, 3 }, { 4, 5 } };
        byte[] result = ByteStream.flatten(a, (byte) 0, true).toArray();
        assertArrayEquals(new byte[] { 1, 4, 2, 5, 3, 0 }, result);
    }

    @Test
    public void testRange_Basic() {
        byte[] result = ByteStream.range((byte) 1, (byte) 5).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testRange_EmptyRange() {
        byte[] result = ByteStream.range((byte) 5, (byte) 5).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testRangeClosed_Basic() {
        byte[] result = ByteStream.rangeClosed((byte) 1, (byte) 5).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testRangeClosed_SingleElement() {
        byte[] result = ByteStream.rangeClosed((byte) 3, (byte) 3).toArray();
        assertArrayEquals(new byte[] { 3 }, result);
    }

    @Test
    public void testRepeat_Basic() {
        byte[] result = ByteStream.repeat((byte) 7, 4).toArray();
        assertArrayEquals(new byte[] { 7, 7, 7, 7 }, result);
    }

    @Test
    public void testRepeat_Zero() {
        byte[] result = ByteStream.repeat((byte) 7, 0).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testRepeat_Negative() {
        assertThrows(IllegalArgumentException.class, () -> ByteStream.repeat((byte) 7, -1));
    }

    @Test
    public void testRandom_Basic() {
        byte[] result = ByteStream.random().limit(10).toArray();
        assertEquals(10, result.length);
    }

    @Test
    public void testGenerate_Basic() {
        AtomicInteger counter = new AtomicInteger(0);
        byte[] result = ByteStream.generate(() -> (byte) counter.getAndIncrement()).limit(5).toArray();
        assertArrayEquals(new byte[] { 0, 1, 2, 3, 4 }, result);
    }

    @Test
    public void testIterate_WithPredicate() {
        byte[] result = ByteStream.iterate((byte) 1, b -> b < 5, b -> (byte) (b + 1)).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testIterate_Infinite() {
        byte[] result = ByteStream.iterate((byte) 0, b -> (byte) (b + 1)).limit(5).toArray();
        assertArrayEquals(new byte[] { 0, 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcat_EmptyArrays() {
        ByteStream stream = ByteStream.concat(new byte[0], new byte[0]);
        assertEquals(0, stream.count());
    }

    @Test
    public void testIntersection_Verify() {
        ByteStream a = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        byte[] bArr = { 2, 3, 5 };
        byte[] result = a.intersection(N.toList(bArr)).toArray();
        assertArrayEquals(new byte[] { 2, 3 }, result);
    }

    @Test
    public void testDifference_Verify() {
        ByteStream a = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        byte[] bArr = { 2, 3, 5 };
        byte[] result = a.difference(N.toList(bArr)).toArray();
        assertArrayEquals(new byte[] { 1, 4 }, result);
    }

    @Test
    public void testSymmetricDifference_Verify() {
        ByteStream a = createByteStream((byte) 1, (byte) 2, (byte) 3);
        byte[] bArr = { 2, 3, 4 };
        byte[] result = a.symmetricDifference(N.toList(bArr)).toArray();
        assertArrayEquals(new byte[] { 1, 4 }, result);
    }

    @Test
    public void testOfNullable_NonNull() {
        ByteStream stream = ByteStream.ofNullable((byte) 5);
        byte[] result = stream.toArray();
        assertArrayEquals(new byte[] { 5 }, result);
    }

    @Test
    public void testOfNullable_Null() {
        ByteStream stream = ByteStream.ofNullable(null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testDefer_Basic() {
        ByteStream stream = ByteStream.defer(() -> ByteStream.of((byte) 1, (byte) 2, (byte) 3));
        byte[] result = stream.toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testDefer_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ByteStream.defer(null));
    }

    @Test
    public void testFirst_EmptyStream() {
        OptionalByte result = ByteStream.empty().first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast_EmptyStream() {
        OptionalByte result = ByteStream.empty().last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_EmptyStream() {
        OptionalByte result = ByteStream.empty().onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_MultipleElements() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testCount_EmptyStream() {
        assertEquals(0, ByteStream.empty().count());
    }

    @Test
    public void testJoin_WithPrefixSuffix() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        String result = stream.join(", ", "[", "]");
        assertEquals("[1, 2, 3]", result);
    }

}
