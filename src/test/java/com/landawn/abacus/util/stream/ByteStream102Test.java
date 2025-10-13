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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IndexedByte;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalDouble;

@Tag("new-test")
public class ByteStream102Test extends TestBase {

    @Test
    public void testEmpty() {
        ByteStream stream = ByteStream.empty();
        assertNotNull(stream);
        assertFalse(stream.first().isPresent());
    }

    protected ByteStream createByteStream(byte... a) {
        return ByteStream.of(a).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(byte[] a, int fromIndex, int toIndex) {
        return ByteStream.of(a, fromIndex, toIndex).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(Byte[] a) {
        return ByteStream.of(a).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(Byte[] a, int fromIndex, int toIndex) {
        return ByteStream.of(a, fromIndex, toIndex).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(Collection<Byte> coll) {
        return ByteStream.of(coll.toArray(new Byte[coll.size()])).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(ByteIterator iter) {
        return iter == null ? ByteStream.empty() : ByteStream.of(iter.toArray()).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(ByteBuffer buff) {
        return ByteStream.of(buff).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(File file) {
        return ByteStream.of(file).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(InputStream is) {
        return ByteStream.of(is).map(e -> (byte) (e + 0));
    }

    protected ByteStream createByteStream(InputStream is, boolean closeInputStreamOnClose) {
        return ByteStream.of(is, closeInputStreamOnClose).map(e -> (byte) (e + 0));
    }

    @Test
    public void testDefer() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<ByteStream> supplier = () -> {
            counter.incrementAndGet();
            return createByteStream((byte) 1, (byte) 2, (byte) 3);
        };

        ByteStream stream = ByteStream.defer(supplier);
        assertEquals(0, counter.get());

        byte[] result = stream.toArray();
        assertEquals(1, counter.get());
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testOfNullable() {
        ByteStream stream1 = ByteStream.ofNullable(null);
        assertEquals(0, stream1.count());

        ByteStream stream2 = ByteStream.ofNullable((byte) 5);
        assertEquals(5, stream2.first().get());
    }

    @Test
    public void testOfArray() {
        byte[] array = { 1, 2, 3, 4, 5 };
        ByteStream stream = createByteStream(array);
        assertArrayEquals(array, stream.toArray());

        ByteStream emptyStream = createByteStream(new byte[0]);
        assertEquals(0, emptyStream.count());

        ByteStream rangeStream = createByteStream(array, 1, 4);
        assertArrayEquals(new byte[] { 2, 3, 4 }, rangeStream.toArray());
    }

    @Test
    public void testOfByteArray() {
        Byte[] array = { 1, 2, 3, 4, 5 };
        ByteStream stream = createByteStream(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());

        ByteStream rangeStream = createByteStream(array, 1, 3);
        assertArrayEquals(new byte[] { 2, 3 }, rangeStream.toArray());
    }

    @Test
    public void testOfCollection() {
        List<Byte> list = Arrays.asList((byte) 1, (byte) 2, (byte) 3);
        ByteStream stream = createByteStream(list);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testOfByteIterator() {
        ByteIterator iter = ByteIterator.of(new byte[] { 1, 2, 3 });
        ByteStream stream = createByteStream(iter);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream.toArray());

        ByteStream nullStream = createByteStream((ByteIterator) null);
        assertEquals(0, nullStream.count());
    }

    @Test
    public void testOfByteBuffer() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        buffer.position(1);
        buffer.limit(4);

        ByteStream stream = createByteStream(buffer);
        assertArrayEquals(new byte[] { 2, 3, 4 }, stream.toArray());

        ByteStream nullStream = createByteStream((ByteBuffer) null);
        assertEquals(0, nullStream.count());
    }

    @Test
    public void testOfFile() throws IOException {
        File tempFile = File.createTempFile("test", ".tmp");
        tempFile.deleteOnExit();

        byte[] data = { 1, 2, 3, 4, 5 };
        IOUtil.write(data, tempFile);

        ByteStream stream = createByteStream(tempFile);
        assertArrayEquals(data, stream.toArray());
    }

    @Test
    public void testOfInputStream() throws IOException {
        byte[] data = { 1, 2, 3, 4, 5 };
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);

        ByteStream stream = createByteStream(inputStream);
        assertArrayEquals(data, stream.toArray());

        ByteArrayInputStream inputStream2 = new ByteArrayInputStream(data);
        ByteStream stream2 = createByteStream(inputStream2, true);
        assertArrayEquals(data, stream2.toArray());
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
    public void testFlatten3DArray() {
        byte[][][] array = { { { 1, 2 }, { 3 } }, { { 4, 5, 6 } } };
        ByteStream stream = ByteStream.flatten(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testRange() {
        ByteStream stream = ByteStream.range((byte) 1, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.toArray());

        ByteStream stepStream = ByteStream.range((byte) 0, (byte) 10, (byte) 2);
        assertArrayEquals(new byte[] { 0, 2, 4, 6, 8 }, stepStream.toArray());

        ByteStream emptyStream = ByteStream.range((byte) 5, (byte) 5);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testRangeClosed() {
        ByteStream stream = ByteStream.rangeClosed((byte) 1, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());

        ByteStream stepStream = ByteStream.rangeClosed((byte) 0, (byte) 10, (byte) 3);
        assertArrayEquals(new byte[] { 0, 3, 6, 9 }, stepStream.toArray());

        ByteStream singleStream = ByteStream.rangeClosed((byte) 5, (byte) 5);
        assertArrayEquals(new byte[] { 5 }, singleStream.toArray());
    }

    @Test
    public void testRepeat() {
        ByteStream stream = ByteStream.repeat((byte) 7, 5);
        assertArrayEquals(new byte[] { 7, 7, 7, 7, 7 }, stream.toArray());

        ByteStream zeroStream = ByteStream.repeat((byte) 7, 0);
        assertEquals(0, zeroStream.count());
    }

    @Test
    public void testRandom() {
        ByteStream stream = ByteStream.random();
        byte[] array = stream.limit(10).toArray();
        assertEquals(10, array.length);
        for (byte b : array) {
            assertTrue(b >= Byte.MIN_VALUE && b <= Byte.MAX_VALUE);
        }
    }

    @Test
    public void testIterateWithHasNext() {
        AtomicInteger counter = new AtomicInteger(0);
        ByteStream stream = ByteStream.iterate(() -> counter.get() < 5, () -> (byte) counter.getAndIncrement());
        assertArrayEquals(new byte[] { 0, 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testIterateWithInitAndHasNext() {
        AtomicInteger counter = new AtomicInteger(0);
        ByteStream stream = ByteStream.iterate((byte) 10, () -> counter.getAndIncrement() < 3, b -> (byte) (b + 2));
        assertArrayEquals(new byte[] { 10, 12, 14 }, stream.toArray());
    }

    @Test
    public void testIterateWithInitAndPredicate() {
        ByteStream stream = ByteStream.iterate((byte) 1, b -> b < 10, b -> (byte) (b * 2));
        assertArrayEquals(new byte[] { 1, 2, 4, 8 }, stream.toArray());
    }

    @Test
    public void testIterateInfinite() {
        ByteStream stream = ByteStream.iterate((byte) 1, b -> (byte) (b + 1));
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.limit(5).toArray());
    }

    @Test
    public void testGenerate() {
        AtomicInteger counter = new AtomicInteger(0);
        ByteStream stream = ByteStream.generate(() -> (byte) counter.getAndIncrement());
        assertArrayEquals(new byte[] { 0, 1, 2, 3, 4 }, stream.limit(5).toArray());
    }

    @Test
    public void testConcatArrays() {
        byte[] a1 = { 1, 2 };
        byte[] a2 = { 3, 4 };
        byte[] a3 = { 5 };

        ByteStream stream = ByteStream.concat(a1, a2, a3);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());

        ByteStream emptyStream = ByteStream.concat(new byte[0][0]);
        assertEquals(0, emptyStream.count());
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
        ByteStream s1 = createByteStream((byte) 1, (byte) 2);
        ByteStream s2 = createByteStream((byte) 3, (byte) 4);
        ByteStream s3 = createByteStream((byte) 5);

        ByteStream stream = ByteStream.concat(s1, s2, s3);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());
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
    public void testZipArrays() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 4, 5, 6 };

        ByteStream stream = ByteStream.zip(a, b, (x, y) -> (byte) (x + y));
        assertArrayEquals(new byte[] { 5, 7, 9 }, stream.toArray());

        byte[] c = { 1, 2, 3, 4 };
        ByteStream stream2 = ByteStream.zip(a, c, (x, y) -> (byte) (x * y));
        assertArrayEquals(new byte[] { 1, 4, 9 }, stream2.toArray());
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
    public void testZipIterators() {
        ByteIterator a = ByteIterator.of(new byte[] { 1, 2, 3 });
        ByteIterator b = ByteIterator.of(new byte[] { 4, 5, 6 });

        ByteStream stream = ByteStream.zip(a, b, (x, y) -> (byte) (x + y));
        assertArrayEquals(new byte[] { 5, 7, 9 }, stream.toArray());
    }

    @Test
    public void testZipStreams() {
        ByteStream a = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = createByteStream((byte) 4, (byte) 5, (byte) 6);

        ByteStream stream = ByteStream.zip(a, b, (x, y) -> (byte) (x + y));
        assertArrayEquals(new byte[] { 5, 7, 9 }, stream.toArray());
    }

    @Test
    public void testZipWithDefaults() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 4, 5 };

        ByteStream stream = ByteStream.zip(a, b, (byte) 0, (byte) 10, (x, y) -> (byte) (x + y));
        assertArrayEquals(new byte[] { 5, 7, 13 }, stream.toArray());
    }

    @Test
    public void testMergeArrays() {
        byte[] a = { 1, 3, 5 };
        byte[] b = { 2, 4, 6 };

        ByteStream stream = ByteStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMergeIterators() {
        ByteIterator a = ByteIterator.of(new byte[] { 1, 3, 5 });
        ByteIterator b = ByteIterator.of(new byte[] { 2, 4, 6 });

        ByteStream stream = ByteStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMergeStreams() {
        ByteStream a = createByteStream((byte) 1, (byte) 3, (byte) 5);
        ByteStream b = createByteStream((byte) 2, (byte) 4, (byte) 6);

        ByteStream stream = ByteStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMap() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream mapped = stream.map(b -> (byte) (b * 2));
        assertArrayEquals(new byte[] { 2, 4, 6 }, mapped.toArray());
    }

    @Test
    public void testMapToInt() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        IntStream intStream = stream.mapToInt(b -> b * 10);
        assertArrayEquals(new int[] { 10, 20, 30 }, intStream.toArray());
    }

    @Test
    public void testMapToObj() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        Stream<String> stringStream = stream.mapToObj(b -> "Value: " + b);
        List<String> result = stringStream.toList();
        assertEquals(Arrays.asList("Value: 1", "Value: 2", "Value: 3"), result);
    }

    @Test
    public void testFlatMap() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream flatMapped = stream.flatMap(b -> createByteStream(b, (byte) (b * 10)));
        assertArrayEquals(new byte[] { 1, 10, 2, 20, 3, 30 }, flatMapped.toArray());
    }

    @Test
    public void testFlatmap() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream flatMapped = stream.flatmap(b -> new byte[] { b, (byte) (b * 10) });
        assertArrayEquals(new byte[] { 1, 10, 2, 20, 3, 30 }, flatMapped.toArray());
    }

    @Test
    public void testFlatMapToInt() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        IntStream intStream = stream.flatMapToInt(b -> IntStream.of(b, b * 10));
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, intStream.toArray());
    }

    @Test
    public void testFlatMapToObj() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        Stream<String> stringStream = stream.flatMapToObj(b -> Stream.of(String.valueOf(b), "x" + b));
        List<String> result = stringStream.toList();
        assertEquals(Arrays.asList("1", "x1", "2", "x2", "3", "x3"), result);
    }

    @Test
    public void testFlatmapToObj() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        Stream<String> stringStream = stream.flatmapToObj(b -> Arrays.asList(String.valueOf(b), "x" + b));
        List<String> result = stringStream.toList();
        assertEquals(Arrays.asList("1", "x1", "2", "x2", "3", "x3"), result);
    }

    @Test
    public void testFlattMapToObj() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        Stream<String> stringStream = stream.flattmapToObj(b -> new String[] { String.valueOf(b), "x" + b });
        List<String> result = stringStream.toList();
        assertEquals(Arrays.asList("1", "x1", "2", "x2", "3", "x3"), result);
    }

    @Test
    public void testMapPartial() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        ByteStream mapped = stream.mapPartial(b -> b % 2 == 0 ? OptionalByte.of((byte) (b * 10)) : OptionalByte.empty());
        assertArrayEquals(new byte[] { 20, 40 }, mapped.toArray());
    }

    @Test
    public void testRangeMap() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3, (byte) 3);
        ByteStream mapped = stream.rangeMap((a, b) -> a == b, (a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 2, 4, 6 }, mapped.toArray());
    }

    @Test
    public void testRangeMapToObj() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3, (byte) 3);
        Stream<String> mapped = stream.rangeMapToObj((a, b) -> a == b, (a, b) -> a + "-" + b);
        List<String> result = mapped.toList();
        assertEquals(Arrays.asList("1-1", "2-2", "3-3"), result);
    }

    @Test
    public void testCollapse() {
        ByteStream stream = createByteStream((byte) 1, (byte) 1, (byte) 2, (byte) 2, (byte) 2, (byte) 3);
        Stream<ByteList> collapsed = stream.collapse((a, b) -> a == b);
        List<ByteList> result = collapsed.toList();
        assertEquals(3, result.size());
        assertArrayEquals(new byte[] { 1, 1 }, result.get(0).trimToSize().array());
        assertArrayEquals(new byte[] { 2, 2, 2 }, result.get(1).trimToSize().array());
        assertArrayEquals(new byte[] { 3 }, result.get(2).trimToSize().array());
    }

    @Test
    public void testCollapseWithMerge() {
        ByteStream stream = createByteStream((byte) 1, (byte) 1, (byte) 2, (byte) 2, (byte) 2, (byte) 3);
        ByteStream collapsed = stream.collapse((a, b) -> a == b, (a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 2, 6, 3 }, collapsed.toArray());
    }

    @Test
    public void testScan() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        ByteStream scanned = stream.scan((a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 1, 3, 6, 10 }, scanned.toArray());
    }

    @Test
    public void testScanWithInit() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream scanned = stream.scan((byte) 10, (a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 11, 13, 16 }, scanned.toArray());
    }

    @Test
    public void testScanWithInitIncluded() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream scanned = stream.scan((byte) 10, true, (a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 10, 11, 13, 16 }, scanned.toArray());
    }

    @Test
    public void testPrepend() {
        ByteStream stream = createByteStream((byte) 3, (byte) 4, (byte) 5);
        ByteStream prepended = stream.prepend((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, prepended.toArray());
    }

    @Test
    public void testAppend() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream appended = stream.append((byte) 4, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, appended.toArray());
    }

    @Test
    public void testAppendIfEmpty() {
        ByteStream empty = ByteStream.empty();
        ByteStream appended = empty.appendIfEmpty((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2 }, appended.toArray());

        ByteStream nonEmpty = createByteStream((byte) 3);
        ByteStream notAppended = nonEmpty.appendIfEmpty((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 3 }, notAppended.toArray());
    }

    @Test
    public void testToByteList() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteList list = stream.toByteList();
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void testToMap() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        Map<String, Integer> map = stream.toMap(b -> "key" + b, b -> (int) b);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(1), map.get("key1"));
        assertEquals(Integer.valueOf(2), map.get("key2"));
        assertEquals(Integer.valueOf(3), map.get("key3"));
    }

    @Test
    public void testGroupTo() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        Map<String, List<Byte>> grouped = stream.groupTo(b -> b % 2 == 0 ? "even" : "odd", Collectors.toList());
        assertEquals(2, grouped.size());
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5), grouped.get("odd"));
        assertEquals(Arrays.asList((byte) 2, (byte) 4), grouped.get("even"));
    }

    @Test
    public void testReduce() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        byte result = stream.reduce((byte) 0, (a, b) -> (byte) (a + b));
        assertEquals(10, result);
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
    public void testCollect() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        List<Byte> list = stream.collect(ArrayList::new, (l, b) -> l.add(b), (l1, l2) -> l1.addAll(l2));
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), list);
    }

    @Test
    public void testForEach() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        List<Byte> result = new ArrayList<>();
        stream.forEach(result::add);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    @Test
    public void testForEachIndexed() {
        ByteStream stream = createByteStream((byte) 10, (byte) 20, (byte) 30);
        List<String> result = new ArrayList<>();
        stream.forEachIndexed((index, value) -> result.add(index + ":" + value));
        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), result);
    }

    @Test
    public void testAnyMatch() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        assertTrue(stream.anyMatch(b -> b > 2));

        ByteStream stream2 = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        assertFalse(stream2.anyMatch(b -> b > 10));
    }

    @Test
    public void testAllMatch() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        assertTrue(stream.allMatch(b -> b > 0));

        ByteStream stream2 = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        assertFalse(stream2.allMatch(b -> b > 2));
    }

    @Test
    public void testNoneMatch() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        assertTrue(stream.noneMatch(b -> b > 10));

        ByteStream stream2 = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        assertFalse(stream2.noneMatch(b -> b > 2));
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
    public void testFindAny() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        OptionalByte result = stream.findAny(b -> b > 2);
        assertTrue(result.isPresent());
        assertTrue(result.get() > 2);
    }

    @Test
    public void testMin() {
        ByteStream stream = createByteStream((byte) 3, (byte) 1, (byte) 4, (byte) 2);
        OptionalByte min = stream.min();
        assertTrue(min.isPresent());
        assertEquals(1, min.get());

        ByteStream empty = ByteStream.empty();
        OptionalByte emptyMin = empty.min();
        assertFalse(emptyMin.isPresent());
    }

    @Test
    public void testMax() {
        ByteStream stream = createByteStream((byte) 3, (byte) 1, (byte) 4, (byte) 2);
        OptionalByte max = stream.max();
        assertTrue(max.isPresent());
        assertEquals(4, max.get());

        ByteStream empty = ByteStream.empty();
        OptionalByte emptyMax = empty.max();
        assertFalse(emptyMax.isPresent());
    }

    @Test
    public void testKthLargest() {
        ByteStream stream = createByteStream((byte) 3, (byte) 1, (byte) 4, (byte) 2, (byte) 5);
        OptionalByte second = stream.kthLargest(2);
        assertTrue(second.isPresent());
        assertEquals(4, second.get());

        ByteStream stream2 = createByteStream((byte) 1, (byte) 2, (byte) 3);
        OptionalByte tooLarge = stream2.kthLargest(5);
        assertFalse(tooLarge.isPresent());
    }

    @Test
    public void testSum() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        assertEquals(10, stream.sum());

        ByteStream empty = ByteStream.empty();
        assertEquals(0, empty.sum());
    }

    @Test
    public void testAverage() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        OptionalDouble avg = stream.average();
        assertTrue(avg.isPresent());
        assertEquals(2.5, avg.get(), 0.001);

        ByteStream empty = ByteStream.empty();
        OptionalDouble emptyAvg = empty.average();
        assertFalse(emptyAvg.isPresent());
    }

    @Test
    public void testSummarize() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        ByteSummaryStatistics stats = stream.summarize();
        assertEquals(4, stats.getCount());
        assertEquals(10, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(4, stats.getMax());
        assertEquals(2.5, stats.getAverage(), 0.001);
    }

    @Test
    public void testSummarizeAndPercentiles() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> result = stream.summarizeAndPercentiles();

        ByteSummaryStatistics stats = result.left();
        assertEquals(5, stats.getCount());
        assertEquals(15, stats.getSum());

        assertTrue(result.right().isPresent());
        Map<Percentage, Byte> percentiles = result.right().get();
        assertNotNull(percentiles);
    }

    @Test
    public void testMergeWith() {
        ByteStream a = createByteStream((byte) 1, (byte) 3, (byte) 5);
        ByteStream b = createByteStream((byte) 2, (byte) 4, (byte) 6);

        ByteStream merged = a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, merged.toArray());
    }

    @Test
    public void testZipWith() {
        ByteStream a = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = createByteStream((byte) 4, (byte) 5, (byte) 6);

        ByteStream zipped = a.zipWith(b, (x, y) -> (byte) (x + y));
        assertArrayEquals(new byte[] { 5, 7, 9 }, zipped.toArray());
    }

    @Test
    public void testAsIntStream() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        IntStream intStream = stream.asIntStream();
        assertArrayEquals(new int[] { 1, 2, 3 }, intStream.toArray());
    }

    @Test
    public void testBoxed() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        Stream<Byte> boxed = stream.boxed();
        List<Byte> result = boxed.toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    @Test
    public void testFilter() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream filtered = stream.filter(b -> b % 2 == 0);
        assertArrayEquals(new byte[] { 2, 4 }, filtered.toArray());
    }

    @Test
    public void testTakeWhile() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 1);
        ByteStream taken = stream.takeWhile(b -> b < 4);
        assertArrayEquals(new byte[] { 1, 2, 3 }, taken.toArray());
    }

    @Test
    public void testDropWhile() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 1);
        ByteStream dropped = stream.dropWhile(b -> b < 4);
        assertArrayEquals(new byte[] { 4, 1 }, dropped.toArray());
    }

    @Test
    public void testSkipUntil() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 1);
        ByteStream skipped = stream.skipUntil(b -> b >= 4);
        assertArrayEquals(new byte[] { 4, 1 }, skipped.toArray());
    }

    @Test
    public void testDistinct() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 1, (byte) 3);
        ByteStream distinct = stream.distinct();
        assertArrayEquals(new byte[] { 1, 2, 3 }, distinct.toArray());
    }

    @Test
    public void testSorted() {
        ByteStream stream = createByteStream((byte) 3, (byte) 1, (byte) 4, (byte) 2);
        ByteStream sorted = stream.sorted();
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, sorted.toArray());
    }

    @Test
    public void testReverseSorted() {
        ByteStream stream = createByteStream((byte) 3, (byte) 1, (byte) 4, (byte) 2);
        ByteStream sorted = stream.reverseSorted();
        assertArrayEquals(new byte[] { 4, 3, 2, 1 }, sorted.toArray());
    }

    @Test
    public void testSkip() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream skipped = stream.skip(2);
        assertArrayEquals(new byte[] { 3, 4, 5 }, skipped.toArray());
    }

    @Test
    public void testLimit() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream limited = stream.limit(3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, limited.toArray());
    }

    @Test
    public void testStep() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6);
        ByteStream stepped = stream.step(2);
        assertArrayEquals(new byte[] { 1, 3, 5 }, stepped.toArray());
    }

    @Test
    public void testReversed() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        ByteStream reversed = stream.reversed();
        assertArrayEquals(new byte[] { 4, 3, 2, 1 }, reversed.toArray());
    }

    @Test
    public void testRotated() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream rotated = stream.rotated(2);
        assertArrayEquals(new byte[] { 4, 5, 1, 2, 3 }, rotated.toArray());
    }

    @Test
    public void testShuffled() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        byte[] shuffled = stream.shuffled().toArray();
        assertEquals(5, shuffled.length);
        Set<Byte> set = new HashSet<>();
        for (byte b : shuffled) {
            set.add(b);
        }
        assertEquals(5, set.size());
    }

    @Test
    public void testCycled() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream cycled = stream.cycled();
        assertArrayEquals(new byte[] { 1, 2, 3, 1, 2, 3, 1 }, cycled.limit(7).toArray());
    }

    @Test
    public void testCycledWithRounds() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream cycled = stream.cycled(2);
        assertArrayEquals(new byte[] { 1, 2, 3, 1, 2, 3 }, cycled.toArray());
    }

    @Test
    public void testIntersection() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        List<Byte> list = Arrays.asList((byte) 2, (byte) 3, (byte) 5);
        ByteStream intersection = stream.intersection(list);
        assertArrayEquals(new byte[] { 2, 3 }, intersection.toArray());
    }

    @Test
    public void testDifference() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        List<Byte> list = Arrays.asList((byte) 2, (byte) 3, (byte) 5);
        ByteStream difference = stream.difference(list);
        assertArrayEquals(new byte[] { 1, 4 }, difference.toArray());
    }

    @Test
    public void testSymmetricDifference() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        List<Byte> list = Arrays.asList((byte) 2, (byte) 3, (byte) 5);
        ByteStream symDiff = stream.symmetricDifference(list);
        assertArrayEquals(new byte[] { 1, 4, 5 }, symDiff.toArray());
    }

    @Test
    public void testIndexed() {
        ByteStream stream = createByteStream((byte) 10, (byte) 20, (byte) 30);
        Stream<IndexedByte> indexed = stream.indexed();
        List<IndexedByte> result = indexed.toList();

        assertEquals(3, result.size());
        assertEquals(0, result.get(0).index());
        assertEquals(10, result.get(0).value());
        assertEquals(1, result.get(1).index());
        assertEquals(20, result.get(1).value());
        assertEquals(2, result.get(2).index());
        assertEquals(30, result.get(2).value());
    }

    @Test
    public void testPeek() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        List<Byte> peeked = new ArrayList<>();
        byte[] result = stream.peek(peeked::add).toArray();

        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), peeked);
    }

    @Test
    public void testOnEach() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        List<Byte> onEach = new ArrayList<>();
        byte[] result = stream.onEach(onEach::add).toArray();

        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), onEach);
    }

    @Test
    public void testJoin() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        String joined = stream.join(", ");
        assertEquals("1, 2, 3", joined);

        ByteStream stream2 = createByteStream((byte) 1, (byte) 2, (byte) 3);
        String joined2 = stream2.join(", ", "[", "]");
        assertEquals("[1, 2, 3]", joined2);
    }

    @Test
    public void testJoinTo() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        Joiner joiner = Joiner.with(", ", "[", "]");
        stream.joinTo(joiner);
        assertEquals("[1, 2, 3]", joiner.toString());
    }

    @Test
    public void testCount() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertEquals(5, stream.count());

        ByteStream empty = ByteStream.empty();
        assertEquals(0, empty.count());
    }

    @Test
    public void testToArray() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        byte[] array = stream.toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, array);
    }

    @Test
    public void testToList() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        List<Byte> list = stream.toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), list);
    }

    @Test
    public void testToSet() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 2, (byte) 3);
        Set<Byte> set = stream.toSet();
        assertEquals(3, set.size());
        assertTrue(set.contains((byte) 1));
        assertTrue(set.contains((byte) 2));
        assertTrue(set.contains((byte) 3));
    }

    @Test
    public void testToMultiset() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 2, (byte) 3);
        Multiset<Byte> multiset = stream.toMultiset();
        assertEquals(4, multiset.size());
        assertEquals(1, multiset.getCount((byte) 1));
        assertEquals(2, multiset.getCount((byte) 2));
        assertEquals(1, multiset.getCount((byte) 3));
    }

    @Test
    public void testFindFirst() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        OptionalByte first = stream.first();
        assertTrue(first.isPresent());
        assertEquals(1, first.get());

        ByteStream empty = ByteStream.empty();
        OptionalByte emptyFirst = empty.first();
        assertFalse(emptyFirst.isPresent());
    }

    @Test
    public void testFindLast() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        OptionalByte last = stream.last();
        assertTrue(last.isPresent());
        assertEquals(3, last.get());

        ByteStream empty = ByteStream.empty();
        OptionalByte emptyLast = empty.last();
        assertFalse(emptyLast.isPresent());
    }

    @Test
    public void testOnlyOne() {
        ByteStream single = createByteStream((byte) 42);
        OptionalByte result = single.onlyOne();
        assertTrue(result.isPresent());
        assertEquals(42, result.get());

        ByteStream empty = ByteStream.empty();
        OptionalByte emptyResult = empty.onlyOne();
        assertFalse(emptyResult.isPresent());

        ByteStream multiple = createByteStream((byte) 1, (byte) 2);
        assertThrows(TooManyElementsException.class, () -> multiple.onlyOne());
    }

    @Test
    public void testPercentiles() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        Optional<Map<Percentage, Byte>> percentiles = stream.percentiles();
        assertTrue(percentiles.isPresent());
        assertNotNull(percentiles.get());

        ByteStream empty = ByteStream.empty();
        Optional<Map<Percentage, Byte>> emptyPercentiles = empty.percentiles();
        assertFalse(emptyPercentiles.isPresent());
    }

    @Test
    public void testThrowIfEmpty() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream.throwIfEmpty().toArray());

        ByteStream empty = ByteStream.empty();
        assertThrows(NoSuchElementException.class, () -> empty.throwIfEmpty().toArray());
    }

    @Test
    public void testIterator() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
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

    @Test
    public void testSequential() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream parallel = stream.parallel();
        assertTrue(parallel.isParallel());

        ByteStream sequential = parallel.sequential();
        assertFalse(sequential.isParallel());
    }
}
