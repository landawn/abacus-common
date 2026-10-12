package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.IndexedByte;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.function.ByteBiPredicate;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteTriPredicate;
import com.landawn.abacus.util.u.OptionalByte;

/** ByteStream transformation contracts. */
public class ByteStreamTransformationTest extends ByteStreamTestSupport {

    // collapse

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
    public void testCollapseTriPredicate() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11);
        byte[] result = stream.collapse((first, last, next) -> next - first <= 3, (a, b) -> (byte) (a + b)).toArray();
        assertTrue(result.length > 0);
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
    public void testCollapseWithMerge() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 5, (byte) 6, (byte) 7, (byte) 10);
        assertArrayEquals(new byte[] { 3, 18, 10 }, stream.collapse((a, b) -> b - a <= 2, (a, b) -> (byte) (a + b)).toArray());
    }

    @Test
    public void testCollapseWithNoCollapsibleElements() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 10, (byte) 20, (byte) 30);
        byte[] result = stream.collapse((a, b) -> b - a <= 2, (a, b) -> (byte) (a + b)).toArray();
        assertTrue(result.length > 0);
    }

    @Test
    public void testCollapseWithPredicate() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 5, (byte) 6, (byte) 7, (byte) 10);
        List<ByteList> result = stream.collapse((a, b) -> b - a <= 2).toList();
        assertTrue(result.size() > 0);
    }

    @Test
    public void testCollapseWithTriPredicate() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6, (byte) 10);
        ByteTriPredicate collapsible = (first, prev, curr) -> curr - first < 3;
        ByteBinaryOperator mergeFunction = (a, b) -> (byte) (a + b);

        ByteStream collapsed = stream.collapse(collapsible, mergeFunction);
        assertArrayEquals(new byte[] { 6, 11, 10 }, collapsed.toArray());
    }

    // cycled

    @Test
    public void testCycled() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3, 1, 2, 3, 1, 2 }, stream.cycled(3).limit(8).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        assertEquals(6, stream2.cycled(3).count());
    }

    @Test
    public void testCycled_MultipleRounds() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        byte[] result = stream.cycled(3).toArray();
        assertArrayEquals(new byte[] { 1, 2, 1, 2, 1, 2 }, result);
    }

    @Test
    public void testCycled_ZeroRounds() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        byte[] result = stream.cycled(0).toArray();
        assertEquals(0, result.length);
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
    public void testDifference() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3, 4 });
        Collection<Byte> other = Arrays.asList((byte) 2, (byte) 3, (byte) 5);
        List<Byte> result = byteStream.difference(other).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 4), result);
    }

    @Test
    public void testDifference_Verify() {
        ByteStream a = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        byte[] bArr = { 2, 3, 5 };
        byte[] result = a.difference(N.toList(bArr)).toArray();
        assertArrayEquals(new byte[] { 1, 4 }, result);
    }

    // distinct

    @Test
    public void testDistinct() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 1, (byte) 4);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, stream.distinct().toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(3, stream2.distinct().count());

        ByteStream stream3 = ByteStream.of((byte) 5, (byte) 5, (byte) 5);
        assertArrayEquals(new byte[] { 5 }, stream3.distinct().toArray());
    }



    // G087-01: unsorted ArrayByteStream.distinct() keeps first occurrences in encounter order over the full byte domain
    @Test
    public void testDistinct_unsortedArrayFullDomainMatchesFirstOccurrenceOrder() {
        final byte[] edge = { 0, -128, 127, -1, 1, -128, 0, 127, -1, 64, -64, 64 };
        assertArrayEquals(new byte[] { 0, -128, 127, -1, 1, 64, -64 }, ByteStream.of(edge).distinct().toArray());
        assertArrayEquals(new byte[] { 127, -1, 1, -128, 0, 64 }, ByteStream.of(edge, 2, 10).distinct().toArray());
        assertArrayEquals(new byte[0], ByteStream.of(new byte[0]).distinct().toArray());
        assertArrayEquals(new byte[] { -7 }, ByteStream.of(new byte[] { -7 }).distinct().toArray());
        assertArrayEquals(new byte[] { -7 }, ByteStream.of(new byte[] { -7, -7, -7 }).distinct().toArray());

        final Random random = new Random(20260926L);

        for (int round = 0; round < 20; round++) {
            final byte[] data = new byte[random.nextInt(2000)];
            random.nextBytes(data);

            final java.util.LinkedHashSet<Byte> expected = new java.util.LinkedHashSet<>();

            for (final byte b : data) {
                expected.add(b);
            }

            final byte[] expectedArray = new byte[expected.size()];
            int idx = 0;

            for (final Byte b : expected) {
                expectedArray[idx++] = b;
            }

            assertArrayEquals(expectedArray, ByteStream.of(data).distinct().toArray());
            assertEquals(expected.size(), ByteStream.of(data).distinct().count());

            final byte[] parallelResult = ByteStream.of(data).parallel(4).distinct().toArray();
            Arrays.sort(parallelResult);
            final byte[] sortedExpected = expectedArray.clone();
            Arrays.sort(sortedExpected);
            assertArrayEquals(sortedExpected, parallelResult);
        }
    }



    // G087-01: the distinct filter is lazy and each distinct() call has its own seen-table
    @Test
    public void testDistinct_unsortedArrayLazyAndIndependentPerCall() {
        final byte[] data = { 3, 1, 3, 2, 1, 5 };
        final ByteIterator iter = ByteStream.of(data).distinct().iterator();
        assertTrue(iter.hasNext());
        assertEquals(3, iter.nextByte());
        assertEquals(1, iter.nextByte());
        assertEquals(2, iter.nextByte());
        assertEquals(5, iter.nextByte());
        assertFalse(iter.hasNext());

        assertArrayEquals(new byte[] { 3, 1, 2, 5 }, ByteStream.of(data).distinct().toArray());
        assertArrayEquals(new byte[] { 3, 1, 2, 5 }, ByteStream.of(data).distinct().toArray());
    }

    // dropWhile

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
    public void testDropWhileWithActionOnDroppedItem() {
        byteStream = createByteStream(TEST_ARRAY);
        List<Byte> droppedItems = new ArrayList<>();
        List<Byte> result = byteStream.dropWhile(b -> b < 3, droppedItems::add).toList();
        assertEquals(Arrays.asList((byte) 3, (byte) 4, (byte) 5), result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2), droppedItems);
    }

    // filter

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
    public void testFilterWithActionOnDroppedItem() {
        byteStream = createByteStream(TEST_ARRAY);
        List<Byte> droppedItems = new ArrayList<>();
        List<Byte> result = byteStream.filter(b -> b % 2 == 0, droppedItems::add).toList();
        assertEquals(Arrays.asList((byte) 2, (byte) 4), result);
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5), droppedItems);
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
    public void testIntersection() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3, 4 });
        Collection<Byte> other = Arrays.asList((byte) 2, (byte) 2, (byte) 3, (byte) 5);
        List<Byte> result = byteStream.intersection(other).toList();
        assertEquals(Arrays.asList((byte) 2, (byte) 2, (byte) 3), result);
    }

    @Test
    public void testIntersection_Verify() {
        ByteStream a = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        byte[] bArr = { 2, 3, 5 };
        byte[] result = a.intersection(N.toList(bArr)).toArray();
        assertArrayEquals(new byte[] { 2, 3 }, result);
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
    public void testOnEach() {
        AtomicInteger sum = new AtomicInteger(0);
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        stream.peek(b -> sum.addAndGet(b)).forEach(b -> {
        });
        assertEquals(6, sum.get());
    }

    // onlyOne

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
    public void testPeek() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        List<Byte> peeked = new ArrayList<>();
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream.peek(peeked::add).toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), peeked);
    }

    @Test
    public void testPeekIsAliasForOnEach() {
        List<Byte> peeked = new ArrayList<>();
        byte[] result = createByteStream((byte) 1, (byte) 2, (byte) 3).peek(b -> peeked.add(b)).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
        assertEquals(3, peeked.size());
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
    public void testReverseSorted() {
        ByteStream stream = ByteStream.of((byte) 3, (byte) 1, (byte) 4, (byte) 2, (byte) 5);
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, stream.reverseSorted().toArray());

        ByteStream stream2 = ByteStream.of((byte) 5, (byte) 4, (byte) 3);
        assertArrayEquals(new byte[] { 5, 4, 3 }, stream2.reverseSorted().toArray());
    }

    @Test
    public void testReverseSorted_Verify() {
        ByteStream stream = createByteStream((byte) 1, (byte) 3, (byte) 2);
        byte[] result = stream.reverseSorted().toArray();
        assertArrayEquals(new byte[] { 3, 2, 1 }, result);
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

    // scan

    @Test
    public void testScan() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        assertArrayEquals(new byte[] { 1, 3, 6, 10 }, stream.scan((a, b) -> (byte) (a + b)).toArray());

        ByteStream stream2 = ByteStream.of((byte) 5);
        assertArrayEquals(new byte[] { 5 }, stream2.scan((a, b) -> (byte) (a + b)).toArray());
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
    public void testScanOnEmptyStream() {
        ByteStream emptyStream = ByteStream.empty();
        byte[] result = emptyStream.scan((a, b) -> (byte) (a + b)).toArray();
        assertEquals(0, result.length);
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

    // shuffled

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
    public void testShuffled_WithRandom() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        byte[] result = stream.shuffled(new Random(42)).toArray();
        assertEquals(5, result.length);
        // Verify all elements are present
        Arrays.sort(result);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
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

    // skip

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
    public void testSkipWithAction() {
        byteStream = createByteStream(TEST_ARRAY);
        List<Byte> skippedItems = new ArrayList<>();
        List<Byte> result = byteStream.skip(2, skippedItems::add).toList();
        assertEquals(Arrays.asList((byte) 3, (byte) 4, (byte) 5), result);
        assertEquals(Arrays.asList((byte) 1, (byte) 2), skippedItems);
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
    public void testSkipAndLimit() {
        byte[] result = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skipAndLimit(1, 3).toArray();
        assertArrayEquals(new byte[] { 2, 3, 4 }, result);
    }

    @Test
    public void testSkipAndLimit_ZeroOffset() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        byte[] result = stream.skipAndLimit(0, 2).toArray();
        assertArrayEquals(new byte[] { 1, 2 }, result);
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
    public void testSorted() {
        ByteStream stream = ByteStream.of((byte) 3, (byte) 1, (byte) 4, (byte) 2, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.sorted().toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream2.sorted().toArray());

        ByteStream stream3 = ByteStream.of((byte) -3, (byte) 1, (byte) -1, (byte) 0);
        assertArrayEquals(new byte[] { -3, -1, 0, 1 }, stream3.sorted().toArray());
    }

    // step

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
    public void testStep_StepOne() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        byte[] result = stream.step(1).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testStep_StepTwo() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        byte[] result = stream.step(2).toArray();
        assertArrayEquals(new byte[] { 1, 3, 5 }, result);
    }

    @Test
    public void testStepIteratorCountConsumes() {
        // Regression: the step() iterator's count() must exhaust the iterator per the IteratorEx.count contract.
        ByteIterator iter = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6).step(2).iterator();
        assertEquals(3, iter.count());
        assertFalse(iter.hasNext());
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
    public void testSymmetricDifference_Verify() {
        ByteStream a = createByteStream((byte) 1, (byte) 2, (byte) 3);
        byte[] bArr = { 2, 3, 4 };
        byte[] result = a.symmetricDifference(N.toList(bArr)).toArray();
        assertArrayEquals(new byte[] { 1, 4 }, result);
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

}
