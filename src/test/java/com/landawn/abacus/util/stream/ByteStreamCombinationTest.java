package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteNFunction;
import com.landawn.abacus.util.u.OptionalByte;

/** ByteStream combination contracts. */
public class ByteStreamCombinationTest extends ByteStreamTestSupport {

    // append

    @Test
    public void testAppend() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.append((byte) 3, (byte) 4).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2 }, stream2.append().toArray());
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
    public void testAppendStream() {
        ByteStream main = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream toAppend = createByteStream((byte) 4, (byte) 5);

        ByteStream result = main.append(toAppend);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    // appendIfEmpty

    @Test
    public void testAppendIfEmpty() {
        ByteStream emptyStream = ByteStream.empty();
        assertArrayEquals(new byte[] { 1, 2 }, emptyStream.appendIfEmpty((byte) 1, (byte) 2).toArray());

        ByteStream stream = ByteStream.of((byte) 5);
        assertArrayEquals(new byte[] { 5 }, stream.appendIfEmpty((byte) 1, (byte) 2).toArray());
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
    public void testAppendIfEmptyBytes() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<Byte> result = byteStream.appendIfEmpty((byte) 10, (byte) 11).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2), result);

        byteStream = createByteStream(new byte[] {});
        result = byteStream.appendIfEmpty((byte) 10, (byte) 11).toList();
        assertEquals(Arrays.asList((byte) 10, (byte) 11), result);
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
    public void testAppendIfEmptyWithSupplierOnEmpty() {
        byte[] result = ByteStream.empty().appendIfEmpty(() -> ByteStream.of((byte) 77)).toArray();
        assertArrayEquals(new byte[] { 77 }, result);
    }

    @Test
    public void testAppendIfEmptyWithSupplierOnNonEmpty() {
        byte[] result = createByteStream((byte) 1).appendIfEmpty(() -> ByteStream.of((byte) 77)).toArray();
        assertArrayEquals(new byte[] { 1 }, result);
    }

    // concat

    @Test
    public void testConcat_EmptyArrays() {
        ByteStream stream = ByteStream.concat(new byte[0], new byte[0]);
        assertEquals(0, stream.count());
    }

    @Test
    public void testConcatArrays() {
        ByteStream stream = ByteStream.concat(new byte[] { 1, 2 }, new byte[] { 3, 4 });
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.toArray());

        ByteStream stream2 = ByteStream.concat(new byte[] { 1 }, new byte[0], new byte[] { 2 });
        assertArrayEquals(new byte[] { 1, 2 }, stream2.toArray());
    }

    // G090-02: bulk toArray/toByteList/count of concat(List<byte[]>) / concat(byte[]...) - null and empty arrays, mid-array start
    @Test
    public void testConcatArrays_bulkToArrayCountToList() {
        final byte[] a = { 1, 2 };
        final byte[] b = { 3, 4, 5 };
        final List<byte[]> list = Arrays.asList(null, a, new byte[0], null, b, null, new byte[0]);

        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.concat(list).toArray());
        assertEquals(5, ByteStream.concat(list).count());
        assertEquals(ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.concat(list).toByteList());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.concat(list).skip(1).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.concat(list).skip(3).toArray());
        assertEquals(1, ByteStream.concat(list).skip(4).count());
        assertEquals(0, ByteStream.concat(list).skip(5).toArray().length);
        assertEquals(0, ByteStream.concat(list).skip(50).count());

        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.concat(a, b).toArray());
        assertEquals(5, ByteStream.concat(a, null, b).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.concat(new byte[0], b).toArray());

        final List<byte[]> empties = Arrays.asList(null, new byte[0]);
        assertEquals(0, ByteStream.concat(empties).toArray().length);
        assertEquals(0, ByteStream.concat(empties).count());
        final ByteList emptyList = ByteStream.concat(empties).toByteList();
        emptyList.add((byte) 9);
        assertEquals(1, emptyList.size());

        // the result is a fresh array and the arrays are read when the stream is traversed (live view)
        final byte[] c = { 7, 8 };
        final ByteStream stream = ByteStream.concat(c);
        c[1] = 9;
        final byte[] result = stream.toArray();
        assertArrayEquals(new byte[] { 7, 9 }, result);
        result[0] = 0;
        assertEquals(7, c[0]);

        final byte[] big1 = g090Data(10000);
        final byte[] big2 = g090Data(7777);
        final byte[] expected = new byte[big1.length + big2.length];
        System.arraycopy(big1, 0, expected, 0, big1.length);
        System.arraycopy(big2, 0, expected, big1.length, big2.length);
        assertArrayEquals(expected, ByteStream.concat(big1, big2).toArray());
        assertArrayEquals(Arrays.copyOfRange(expected, 9999, expected.length), ByteStream.concat(big1, big2).skip(9999).toArray());
        assertEquals(expected.length, ByteStream.concat(big1, big2).count());
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
    public void testConcatCollection() {
        Collection<ByteStream> streams = Arrays.asList(createByteStream((byte) 1, (byte) 2), createByteStream((byte) 3, (byte) 4), createByteStream((byte) 5));

        ByteStream stream = ByteStream.concat(streams);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());
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
    public void testConcatCollectionOfStreams() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 2), ByteStream.of((byte) 3, (byte) 4));
        ByteStream stream = ByteStream.concat(streams);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcatCollectionSnapshotsSources() {
        List<ByteStream> sources = new ArrayList<>(Arrays.asList(ByteStream.of((byte) 1, (byte) 2), ByteStream.of((byte) 3, (byte) 4)));
        ByteStream concatenated = ByteStream.concat(sources);

        sources.clear();

        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, concatenated.toArray());

        AtomicBoolean firstClosed = new AtomicBoolean();
        AtomicBoolean secondClosed = new AtomicBoolean();
        sources.add(ByteStream.of((byte) 1).onClose(() -> firstClosed.set(true)));
        sources.add(ByteStream.of((byte) 2).onClose(() -> secondClosed.set(true)));
        concatenated = ByteStream.concat(sources);

        sources.clear();
        concatenated.close();

        assertTrue(firstClosed.get());
        assertTrue(secondClosed.get());
    }

    @Test
    public void testConcatIterators() {
        ByteIterator iter1 = ByteIterator.of(new byte[] { 1, 2 });
        ByteIterator iter2 = ByteIterator.of(new byte[] { 3, 4 });
        ByteStream stream = ByteStream.concat(iter1, iter2);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcatList() {
        List<byte[]> list = Arrays.asList(new byte[] { 1, 2 }, new byte[] { 3, 4 }, new byte[] { 5 });

        ByteStream stream = ByteStream.concat(list);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testConcatListOfArrays() {
        List<byte[]> list = Arrays.asList(new byte[] { 1, 2 }, new byte[] { 3, 4 });
        ByteStream stream = ByteStream.concat(list);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.toArray());
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
    public void testConcatNullHandling() {
        ByteStream stream = ByteStream.concat((byte[][]) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testConcatStreams() {
        ByteStream stream1 = ByteStream.of((byte) 1, (byte) 2);
        ByteStream stream2 = ByteStream.of((byte) 3, (byte) 4);
        ByteStream stream = ByteStream.concat(stream1, stream2);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcatIterators2() {
        Collection<ByteIterator> iterators = Arrays.asList(ByteIterator.of(new byte[] { 1, 2 }), ByteIterator.of(new byte[] { 3, 4 }));
        ByteStream stream = ByteStream.concatIterators(iterators);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcatIteratorsCollection() {
        Collection<ByteIterator> iterators = new ArrayList<>();
        iterators.add(ByteIterator.of((byte) 1, (byte) 2));
        iterators.add(ByteIterator.of((byte) 3, (byte) 4));
        List<Byte> result = ByteStream.concatIterators(iterators).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4), result);
    }

    // join

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
    public void testJoin_WithPrefixSuffix() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        String result = stream.join(", ", "[", "]");
        assertEquals("[1, 2, 3]", result);
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
    public void testJoinNoArgs() {
        // join(delimiter) default method
        String result = createByteStream((byte) 1, (byte) 2, (byte) 3).join(", ");
        assertNotNull(result);
        assertTrue(result.contains("1"));
        assertTrue(result.contains("2"));
        assertTrue(result.contains("3"));
    }

    @Test
    public void testJoinTo() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        Joiner joiner = Joiner.with(", ", "[", "]");
        stream.joinTo(joiner);
        assertEquals("[1, 2, 3]", joiner.toString());
    }

    @Test
    public void testJoinToJoiner() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        Joiner joiner = Joiner.with(" | ", "<", ">");
        Joiner resultJoiner = byteStream.joinTo(joiner);
        assertEquals("<1 | 2 | 3>", resultJoiner.toString());
    }

    // merge

    @Test
    public void testMergeArrays() {
        byte[] a1 = { 1, 3, 5 };
        byte[] a2 = { 2, 4, 6 };
        ByteStream stream = ByteStream.merge(a1, a2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
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
    public void testMergeByteIterators() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 5);
        ByteIterator b = ByteIterator.of((byte) 2, (byte) 6);
        ByteBiFunction<MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = ByteStream.merge(a, b, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 5, (byte) 6), result);
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
    public void testMergeCollectionOfStreams() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 4), ByteStream.of((byte) 2, (byte) 5), ByteStream.of((byte) 3, (byte) 6));
        ByteStream stream = ByteStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMergeCollectionTreatsNullStreamAsEmpty() {
        assertEquals(0, ByteStream.merge(Arrays.asList((ByteStream) null), (a, b) -> MergeResult.TAKE_FIRST).count());
    }

    @Test
    public void testMergeIterators() {
        ByteIterator iter1 = ByteIterator.of(new byte[] { 1, 3, 5 });
        ByteIterator iter2 = ByteIterator.of(new byte[] { 2, 4, 6 });
        ByteStream stream = ByteStream.merge(iter1, iter2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
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
    public void testMergeStreams() {
        ByteStream s1 = ByteStream.of((byte) 1, (byte) 3, (byte) 5);
        ByteStream s2 = ByteStream.of((byte) 2, (byte) 4, (byte) 6);
        ByteStream stream = ByteStream.merge(s1, s2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
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
    public void testMergeThreeArraysAdditional() {
        byte[] a1 = { 1, 7 };
        byte[] a2 = { 3, 8 };
        byte[] a3 = { 5, 9 };
        ByteStream stream = ByteStream.merge(a1, a2, a3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
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
    public void testMergeThreeByteIterators() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 7);
        ByteIterator b = ByteIterator.of((byte) 2, (byte) 5);
        ByteIterator c = ByteIterator.of((byte) 3, (byte) 4);
        ByteBiFunction<MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Byte> result = ByteStream.merge(a, b, c, selector).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 7), result);
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
    public void testMergeThreeIterators() {
        ByteIterator iter1 = ByteIterator.of(new byte[] { 1, 4 });
        ByteIterator iter2 = ByteIterator.of(new byte[] { 2, 5 });
        ByteIterator iter3 = ByteIterator.of(new byte[] { 3, 6 });
        ByteStream stream = ByteStream.merge(iter1, iter2, iter3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
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
    public void testMergeThreeStreamsAdditional() {
        ByteStream s1 = ByteStream.of((byte) 1, (byte) 10);
        ByteStream s2 = ByteStream.of((byte) 5, (byte) 15);
        ByteStream s3 = ByteStream.of((byte) 8, (byte) 20);
        ByteStream stream = ByteStream.merge(s1, s2, s3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMergeWithCollectionOfStreams() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 5), ByteStream.of((byte) 2, (byte) 6), ByteStream.of((byte) 3, (byte) 7));
        ByteStream result = ByteStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, result.count());
    }

    @Test
    public void testMergeWith() {
        ByteStream stream1 = ByteStream.of((byte) 1, (byte) 3, (byte) 5);
        ByteStream stream2 = ByteStream.of((byte) 2, (byte) 4, (byte) 6);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 },
                stream1.mergeWith(stream2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
    }

    @Test
    public void testMergeWith_Verify() {
        ByteStream a = createByteStream((byte) 1, (byte) 3, (byte) 5);
        ByteStream b = ByteStream.of((byte) 2, (byte) 4, (byte) 6);
        byte[] result = a.mergeWith(b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    // prepend

    @Test
    public void testPrepend() {
        ByteStream stream = ByteStream.of((byte) 3, (byte) 4);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.prepend((byte) 1, (byte) 2).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2 }, stream2.prepend().toArray());
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
    public void testPrependStream() {
        ByteStream main = createByteStream((byte) 3, (byte) 4, (byte) 5);
        ByteStream toPrepend = createByteStream((byte) 1, (byte) 2);

        ByteStream result = main.prepend(toPrepend);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    // zip

    @Test
    public void testZipArrays() {
        byte[] a1 = { 1, 2, 3 };
        byte[] a2 = { 10, 20, 30 };
        ByteStream stream = ByteStream.zip(a1, a2, (a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 11, 22, 33 }, stream.toArray());
    }

    @Test
    public void testZipArraysWithDefaults() {
        byte[] a1 = { 1, 2, 3 };
        byte[] a2 = { 10, 20 };
        ByteStream stream = ByteStream.zip(a1, a2, (byte) 0, (byte) 100, (a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 11, 22, 103 }, stream.toArray());
    }

    @Test
    public void testZipByteArrays() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 10, 20, 30, 40 };
        List<Byte> result = ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) 33), result);
    }

    @Test
    public void testZipByteArraysWithDefaults() {
        byte[] a = { 1, 2 };
        byte[] b = { 10, 20, 30 };
        List<Byte> result = ByteStream.zip(a, b, (byte) 0, (byte) 0, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) 30), result);
    }

    @Test
    public void testZipByteIterators() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator b = ByteIterator.of((byte) 10, (byte) 20, (byte) 30);
        List<Byte> result = ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22), result);
    }

    @Test
    public void testZipByteIteratorsWithDefaults() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator b = ByteIterator.of((byte) 10, (byte) 20, (byte) 30);
        List<Byte> result = ByteStream.zip(a, b, (byte) 0, (byte) 0, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) 30), result);
    }

    @Test
    public void testZipByteStreams() {
        ByteStream a = createByteStream((byte) 1, (byte) 2);
        ByteStream b = createByteStream((byte) 10, (byte) 20, (byte) 30);
        List<Byte> result = ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22), result);
    }

    @Test
    public void testZipByteStreamsWithDefaults() {
        ByteStream a = createByteStream((byte) 1, (byte) 2);
        ByteStream b = createByteStream((byte) 10, (byte) 20, (byte) 30);
        List<Byte> result = ByteStream.zip(a, b, (byte) 0, (byte) 0, (x, y) -> (byte) (x + y)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) 30), result);
    }

    @Test
    public void testZipCollectionOfStreams() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 2), ByteStream.of((byte) 10, (byte) 20));
        ByteStream stream = ByteStream.zip(streams, array -> (byte) (array[0] + array[1]));
        assertArrayEquals(new byte[] { 11, 22 }, stream.toArray());
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
    public void testZipCollectionWithDefaults() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 2, (byte) 3), ByteStream.of((byte) 10, (byte) 20));
        byte[] defaults = { 0, 0 };
        ByteStream stream = ByteStream.zip(streams, defaults, array -> (byte) (array[0] + array[1]));
        assertEquals(3, stream.count());
    }

    @Test
    public void testZipIterators() {
        ByteIterator iter1 = ByteIterator.of(new byte[] { 1, 2 });
        ByteIterator iter2 = ByteIterator.of(new byte[] { 10, 20 });
        ByteStream stream = ByteStream.zip(iter1, iter2, (a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 11, 22 }, stream.toArray());
    }

    @Test
    public void testZipIteratorsWithDefaults() {
        ByteIterator iter1 = ByteIterator.of(new byte[] { 1, 2, 3 });
        ByteIterator iter2 = ByteIterator.of(new byte[] { 10, 20 });
        ByteStream stream = ByteStream.zip(iter1, iter2, (byte) 0, (byte) 0, (a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 11, 22, 3 }, stream.toArray());
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
    public void testZipStreams() {
        ByteStream s1 = ByteStream.of((byte) 1, (byte) 2);
        ByteStream s2 = ByteStream.of((byte) 10, (byte) 20);
        ByteStream stream = ByteStream.zip(s1, s2, (a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 11, 22 }, stream.toArray());
    }

    @Test
    public void testZipStreamsWithDefaults() {
        ByteStream s1 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream s2 = ByteStream.of((byte) 10, (byte) 20);
        ByteStream stream = ByteStream.zip(s1, s2, (byte) 0, (byte) 0, (a, b) -> (byte) (a + b));
        assertArrayEquals(new byte[] { 11, 22, 3 }, stream.toArray());
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
    public void testZipThreeArraysWithDefaults() {
        byte[] a1 = { 1, 2, 3 };
        byte[] a2 = { 10, 20 };
        byte[] a3 = { 100 };
        ByteStream stream = ByteStream.zip(a1, a2, a3, (byte) 0, (byte) 0, (byte) 0, (a, b, c) -> (byte) (a + b + c));
        assertEquals(3, stream.count());
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
    public void testZipThreeByteArrays() {
        byte[] a = { 1, 2 };
        byte[] b = { 10, 20 };
        byte[] c = { 50, 60 };
        List<Byte> result = ByteStream.zip(a, b, c, (x, y, z) -> (byte) (x + y + z)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50), (byte) (2 + 20 + 60)), result);
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
    public void testZipThreeByteIterators() {
        ByteIterator a = ByteIterator.of((byte) 1);
        ByteIterator b = ByteIterator.of((byte) 10);
        ByteIterator c = ByteIterator.of((byte) 50, (byte) 60);
        List<Byte> result = ByteStream.zip(a, b, c, (x, y, z) -> (byte) (x + y + z)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50)), result);
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
    public void testZipThreeByteStreams() {
        ByteStream a = createByteStream((byte) 1);
        ByteStream b = createByteStream((byte) 10);
        ByteStream c = createByteStream((byte) 50, (byte) 60);
        List<Byte> result = ByteStream.zip(a, b, c, (x, y, z) -> (byte) (x + y + z)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50)), result);
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
    public void testZipThreeIteratorsWithDefaultsAdditional() {
        ByteIterator iter1 = ByteIterator.of(new byte[] { 1, 2 });
        ByteIterator iter2 = ByteIterator.of(new byte[] { 10 });
        ByteIterator iter3 = ByteIterator.of(new byte[] { 100, 101, 102 });
        ByteStream stream = ByteStream.zip(iter1, iter2, iter3, (byte) 0, (byte) 0, (byte) 0, (a, b, c) -> (byte) (a + b + c));
        assertEquals(3, stream.count());
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
    public void testZipThreeStreamsWithDefaultsAdditional() {
        ByteStream s1 = ByteStream.of((byte) 1, (byte) 2);
        ByteStream s2 = ByteStream.of((byte) 10);
        ByteStream s3 = ByteStream.of((byte) 100, (byte) 101, (byte) 102);
        ByteStream stream = ByteStream.zip(s1, s2, s3, (byte) 0, (byte) 0, (byte) 0, (a, b, c) -> (byte) (a + b + c));
        assertEquals(3, stream.count());
    }

    @Test
    public void testZipWithCollectionAndValuesForNone() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 2, (byte) 3), ByteStream.of((byte) 10, (byte) 20));
        byte[] defaults = { 0, 100 };
        ByteStream result = ByteStream.zip(streams, defaults, bytes -> {
            byte sum = 0;
            for (Byte b : bytes) {
                sum += b;
            }
            return sum;
        });
        assertEquals(3, result.count());
    }

    // Additional coverage improvement tests - Collection-based factory methods
    @Test
    public void testZipWithCollectionOfStreams() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 2), ByteStream.of((byte) 10, (byte) 20),
                ByteStream.of((byte) 100, (byte) 127));
        ByteStream result = ByteStream.zip(streams, bytes -> {
            byte sum = 0;
            for (Byte b : bytes) {
                sum += b;
            }
            return sum;
        });
        assertArrayEquals(new byte[] { 111, -107 }, result.toArray());
    }

    // zipWith

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
    public void testZipWith_ThreeStreams() {
        ByteStream a = createByteStream((byte) 1, (byte) 2);
        ByteStream b = ByteStream.of((byte) 3, (byte) 4);
        ByteStream c = ByteStream.of((byte) 5, (byte) 6);
        byte[] result = a.zipWith(b, c, (x, y, z) -> (byte) (x + y + z)).toArray();
        assertArrayEquals(new byte[] { 9, 12 }, result);
    }

    @Test
    public void testZipWith_TwoStreams() {
        ByteStream a = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = ByteStream.of((byte) 10, (byte) 20, (byte) 30);
        byte[] result = a.zipWith(b, (x, y) -> (byte) (x + y)).toArray();
        assertArrayEquals(new byte[] { 11, 22, 33 }, result);
    }

    @Test
    public void testZipWith_WithDefaults() {
        ByteStream a = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = ByteStream.of((byte) 10);
        byte[] result = a.zipWith(b, (byte) 0, (byte) 0, (x, y) -> (byte) (x + y)).toArray();
        assertArrayEquals(new byte[] { 11, 2, 3 }, result);
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
    public void testZipWithDefaults() {
        ByteStream stream1 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream stream2 = ByteStream.of((byte) 10, (byte) 20);
        assertArrayEquals(new byte[] { 11, 22, 3 }, stream1.zipWith(stream2, (byte) 0, (byte) 0, (a, b) -> (byte) (a + b)).toArray());
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
    public void testZipWithThreeDefaults() {
        ByteStream stream1 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream stream2 = ByteStream.of((byte) 10, (byte) 20);
        ByteStream stream3 = ByteStream.of((byte) 100);
        byte[] result = stream1.zipWith(stream2, stream3, (byte) 0, (byte) 0, (byte) 0, (a, b, c) -> (byte) (a + b + c)).toArray();
        assertEquals(3, result.length);
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
    public void testZipWithThreeStreamsDedicated() {
        ByteStream a = createByteStream((byte) 1, (byte) 2);
        ByteStream b = createByteStream((byte) 10, (byte) 20);
        ByteStream c = createByteStream((byte) 100);
        byte[] result = a.zipWith(b, c, (x, y, z) -> (byte) (x + y + z)).toArray();
        assertArrayEquals(new byte[] { 111 }, result);
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
    public void testZipWithThreeStreamsWithDefaults() {
        byteStream = createByteStream(new byte[] { 1 });
        ByteStream s2 = createByteStream(new byte[] { 10, 20 });
        ByteStream s3 = createByteStream(new byte[] { 50, 60, 70 });
        List<Byte> result = byteStream.zipWith(s2, s3, (byte) 99, (byte) 88, (byte) 77, (a, b, c) -> (byte) (a + b + c)).toList();
        assertEquals(Arrays.asList((byte) (1 + 10 + 50), (byte) (99 + 20 + 60), (byte) (99 + 88 + 70)), result);
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
    public void testZipWithTwoStreams() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteStream otherStream = createByteStream(new byte[] { 10, 20, 30, 40 });
        List<Byte> result = byteStream.zipWith(otherStream, (a, b) -> (byte) (a + b)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) 33), result);
    }

    @Test
    public void testZipWithTwoStreamsWithDefaults() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        ByteStream otherStream = createByteStream(new byte[] { 10, 20, 30 });
        List<Byte> result = byteStream.zipWith(otherStream, (byte) 99, (byte) 88, (a, b) -> (byte) (a + b)).toList();
        assertEquals(Arrays.asList((byte) 11, (byte) 22, (byte) (99 + 30)), result);
    }

}
