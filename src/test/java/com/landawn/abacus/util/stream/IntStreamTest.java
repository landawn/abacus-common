package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.IndexedInt;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalInt;

/** IntStream general contracts. */
public class IntStreamTest extends IntStreamTestSupport {

    @Test
    public void testStreamCreatedAfterAppend() {
        assertEquals(8, IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).count());
        assertEquals(7, IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }, IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8 }, IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8), IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6, 7, 8), IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).toList());
        assertEquals(8, IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).count());
        assertEquals(7, IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8), IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6, 7, 8), IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAsDoubleStream() {
        DoubleStream doubleStream = IntStream.of(1, 2, 3, 4, 5).asDoubleStream();
        assertEquals(5, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).asDoubleStream();
        assertEquals(4, doubleStream.skip(1).count());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).asDoubleStream();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, doubleStream.toArray(), 0.001);

        doubleStream = IntStream.of(1, 2, 3, 4, 5).asDoubleStream();
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0, 5.0 }, doubleStream.skip(1).toArray(), 0.001);

        doubleStream = IntStream.of(1, 2, 3, 4, 5).asDoubleStream();
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), doubleStream.toList());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).asDoubleStream();
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), doubleStream.skip(1).toList());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream();
        assertEquals(5, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream();
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), doubleStream.toList());
    }

    @Test
    public void testStreamCreatedAfterAsFloatStream() {
        FloatStream floatStream = IntStream.of(1, 2, 3, 4, 5).asFloatStream();
        assertEquals(5, floatStream.count());

        floatStream = IntStream.of(1, 2, 3, 4, 5).asFloatStream();
        assertEquals(4, floatStream.skip(1).count());

        floatStream = IntStream.of(1, 2, 3, 4, 5).asFloatStream();
        assertArrayEquals(new float[] { 1f, 2f, 3f, 4f, 5f }, floatStream.toArray(), 0.001f);

        floatStream = IntStream.of(1, 2, 3, 4, 5).asFloatStream();
        assertArrayEquals(new float[] { 2f, 3f, 4f, 5f }, floatStream.skip(1).toArray(), 0.001f);

        floatStream = IntStream.of(1, 2, 3, 4, 5).asFloatStream();
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), floatStream.toList());

        floatStream = IntStream.of(1, 2, 3, 4, 5).asFloatStream();
        assertEquals(N.toList(2f, 3f, 4f, 5f), floatStream.skip(1).toList());

        floatStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asFloatStream();
        assertEquals(5, floatStream.count());

        floatStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asFloatStream();
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), floatStream.toList());
    }

    @Test
    public void testStreamCreatedAfterAsLongStream() {
        LongStream longStream = IntStream.of(1, 2, 3, 4, 5).asLongStream();
        assertEquals(5, longStream.count());

        longStream = IntStream.of(1, 2, 3, 4, 5).asLongStream();
        assertEquals(4, longStream.skip(1).count());

        longStream = IntStream.of(1, 2, 3, 4, 5).asLongStream();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, longStream.toArray());

        longStream = IntStream.of(1, 2, 3, 4, 5).asLongStream();
        assertArrayEquals(new long[] { 2L, 3L, 4L, 5L }, longStream.skip(1).toArray());

        longStream = IntStream.of(1, 2, 3, 4, 5).asLongStream();
        assertEquals(N.toList(1L, 2L, 3L, 4L, 5L), longStream.toList());

        longStream = IntStream.of(1, 2, 3, 4, 5).asLongStream();
        assertEquals(N.toList(2L, 3L, 4L, 5L), longStream.skip(1).toList());

        longStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asLongStream();
        assertEquals(5, longStream.count());

        longStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asLongStream();
        assertEquals(N.toList(1L, 2L, 3L, 4L, 5L), longStream.toList());
    }

    @Test
    public void testStreamCreatedAfterBoxed() {
        Stream<Integer> boxedStream = IntStream.of(1, 2, 3, 4, 5).boxed();
        assertEquals(5, boxedStream.count());

        boxedStream = IntStream.of(1, 2, 3, 4, 5).boxed();
        assertEquals(4, boxedStream.skip(1).count());

        boxedStream = IntStream.of(1, 2, 3, 4, 5).boxed();
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, boxedStream.toArray(Integer[]::new));

        boxedStream = IntStream.of(1, 2, 3, 4, 5).boxed();
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, boxedStream.skip(1).toArray(Integer[]::new));

        boxedStream = IntStream.of(1, 2, 3, 4, 5).boxed();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), boxedStream.toList());

        boxedStream = IntStream.of(1, 2, 3, 4, 5).boxed();
        assertEquals(Arrays.asList(2, 3, 4, 5), boxedStream.skip(1).toList());

        boxedStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).boxed();
        assertEquals(5, boxedStream.count());

        boxedStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).boxed();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), boxedStream.toList());
    }

    // collapse

    @Test
    public void testStreamCreatedAfterCollapse() {
        Stream<IntList> listStream = IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1);
        assertEquals(3, listStream.count());

        listStream = IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1);
        assertEquals(2, listStream.skip(1).count());

        listStream = IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1);
        List<IntList> result = listStream.toList();
        assertEquals(3, result.size());
        assertEquals(IntList.of(1, 2, 3), result.get(0));
        assertEquals(IntList.of(5, 6), result.get(1));
        assertEquals(IntList.of(8), result.get(2));

        listStream = IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1);
        assertEquals(3, listStream.count());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithMerge() {
        assertEquals(3, IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 6, 11, 8 }, IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 8 }, IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(6, 11, 8), IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(11, 8), IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 6, 11, 8 },
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 8 },
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(6, 11, 8), IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(11, 8), IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithTriPredicate() {
        assertEquals(3, IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 6, 11, 8 }, IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 8 },
                IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(6, 11, 8), IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(11, 8), IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 6, 11, 8 },
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 8 },
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(6, 11, 8),
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(11, 8),
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
    }

    // concat

    @Test
    public void testStreamCreatedFromConcatArrays() {
        int[][] arrays = { { 1, 2 }, { 3, 4 }, { 5 } };
        assertEquals(5, IntStream.concat(arrays).count());
        assertEquals(4, IntStream.concat(arrays).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.concat(arrays).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.concat(arrays).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.concat(arrays).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.concat(arrays).skip(1).toList());
        assertEquals(5, IntStream.concat(arrays).map(e -> e).count());
        assertEquals(4, IntStream.concat(arrays).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.concat(arrays).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.concat(arrays).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.concat(arrays).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.concat(arrays).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromConcatIterators() {
        IntIterator iter1 = IntIterator.of(1, 2);
        IntIterator iter2 = IntIterator.of(3, 4);
        IntIterator iter3 = IntIterator.of(5);
        assertEquals(5, IntStream.concat(iter1, iter2, iter3).count());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertEquals(4, IntStream.concat(iter1, iter2, iter3).skip(1).count());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.concat(iter1, iter2, iter3).toArray());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.concat(iter1, iter2, iter3).skip(1).toArray());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.concat(iter1, iter2, iter3).toList());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertEquals(N.toList(2, 3, 4, 5), IntStream.concat(iter1, iter2, iter3).skip(1).toList());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertEquals(5, IntStream.concat(iter1, iter2, iter3).map(e -> e).count());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertEquals(4, IntStream.concat(iter1, iter2, iter3).map(e -> e).skip(1).count());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.concat(iter1, iter2, iter3).map(e -> e).toArray());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.concat(iter1, iter2, iter3).map(e -> e).skip(1).toArray());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.concat(iter1, iter2, iter3).map(e -> e).toList());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertEquals(N.toList(2, 3, 4, 5), IntStream.concat(iter1, iter2, iter3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromConcatStreams() {
        IntStream stream1 = IntStream.of(1, 2);
        IntStream stream2 = IntStream.of(3, 4);
        IntStream stream3 = IntStream.of(5);
        assertEquals(5, IntStream.concat(stream1, stream2, stream3).count());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertEquals(4, IntStream.concat(stream1, stream2, stream3).skip(1).count());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.concat(stream1, stream2, stream3).toArray());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.concat(stream1, stream2, stream3).skip(1).toArray());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.concat(stream1, stream2, stream3).toList());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertEquals(N.toList(2, 3, 4, 5), IntStream.concat(stream1, stream2, stream3).skip(1).toList());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertEquals(5, IntStream.concat(stream1, stream2, stream3).map(e -> e).count());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertEquals(4, IntStream.concat(stream1, stream2, stream3).map(e -> e).skip(1).count());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.concat(stream1, stream2, stream3).map(e -> e).toArray());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.concat(stream1, stream2, stream3).map(e -> e).skip(1).toArray());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.concat(stream1, stream2, stream3).map(e -> e).toList());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertEquals(N.toList(2, 3, 4, 5), IntStream.concat(stream1, stream2, stream3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCycled() {
        assertEquals(15, IntStream.of(1, 2, 3, 4, 5).cycled().limit(15).count());
        assertEquals(14, IntStream.of(1, 2, 3, 4, 5).cycled().limit(15).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).cycled().limit(8).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).cycled().limit(8).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).cycled().limit(8).toList());
        assertEquals(N.toList(2, 3, 4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).cycled().limit(8).skip(1).toList());
        assertEquals(15, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(15).count());
        assertEquals(14, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(15).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(8).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(8).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(8).toList());
        assertEquals(N.toList(2, 3, 4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(8).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCycledWithRounds() {
        assertEquals(10, IntStream.of(1, 2, 3, 4, 5).cycled(2).count());
        assertEquals(9, IntStream.of(1, 2, 3, 4, 5).cycled(2).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).cycled(2).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).cycled(2).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).cycled(2).toList());
        assertEquals(N.toList(2, 3, 4, 5, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).cycled(2).skip(1).toList());
        assertEquals(10, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).count());
        assertEquals(9, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).toList());
        assertEquals(N.toList(2, 3, 4, 5, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromDefer() {
        assertEquals(5, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).count());
        assertEquals(4, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).skip(1).toList());
        assertEquals(5, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).count());
        assertEquals(4, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDifference() {
        Collection<Integer> c = Arrays.asList(3, 4, 5, 6);
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).difference(c).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).difference(c).skip(1).count());
        assertArrayEquals(new int[] { 1, 2 }, IntStream.of(1, 2, 3, 4, 5).difference(c).toArray());
        assertArrayEquals(new int[] { 2 }, IntStream.of(1, 2, 3, 4, 5).difference(c).skip(1).toArray());
        assertEquals(N.toList(1, 2), IntStream.of(1, 2, 3, 4, 5).difference(c).toList());
        assertEquals(N.toList(2), IntStream.of(1, 2, 3, 4, 5).difference(c).skip(1).toList());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).skip(1).count());
        assertArrayEquals(new int[] { 1, 2 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).toArray());
        assertArrayEquals(new int[] { 2 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).skip(1).toArray());
        assertEquals(N.toList(1, 2), IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).toList());
        assertEquals(N.toList(2), IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(5, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().count());
        assertEquals(4, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().count());
        assertEquals(4, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).skip(1).count());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).toArray());
        assertArrayEquals(new int[] { 5 }, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).skip(1).toArray());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).toList());
        assertEquals(N.toList(5), IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).skip(1).toList());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).skip(1).count());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).toArray());
        assertArrayEquals(new int[] { 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).skip(1).toArray());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).toList());
        assertEquals(N.toList(5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhileWithAction() {
        AtomicInteger droppedCount = new AtomicInteger();
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).count());
        droppedCount.set(0);
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).skip(1).count());
        droppedCount.set(0);
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).toArray());
        droppedCount.set(0);
        assertArrayEquals(new int[] { 5 }, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).skip(1).toArray());
        droppedCount.set(0);
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).toList());
        droppedCount.set(0);
        assertEquals(N.toList(5), IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).skip(1).toList());
        droppedCount.set(0);
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).count());
        droppedCount.set(0);
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).skip(1).count());
        droppedCount.set(0);
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).toArray());
        droppedCount.set(0);
        assertArrayEquals(new int[] { 5 },
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).skip(1).toArray());
        droppedCount.set(0);
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).toList());
        droppedCount.set(0);
        assertEquals(N.toList(5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        assertEquals(3, IntStream.empty().appendIfEmpty(1, 2, 3).count());
        assertEquals(2, IntStream.empty().appendIfEmpty(1, 2, 3).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStream.empty().appendIfEmpty(1, 2, 3).toArray());
        assertArrayEquals(new int[] { 2, 3 }, IntStream.empty().appendIfEmpty(1, 2, 3).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), IntStream.empty().appendIfEmpty(1, 2, 3).toList());
        assertEquals(N.toList(2, 3), IntStream.empty().appendIfEmpty(1, 2, 3).skip(1).toList());

        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromEmpty() {
        assertEquals(0, IntStream.empty().count());
        assertEquals(0, IntStream.empty().skip(1).count());
        assertArrayEquals(new int[] {}, IntStream.empty().toArray());
        assertArrayEquals(new int[] {}, IntStream.empty().skip(1).toArray());
        assertEquals(N.toList(), IntStream.empty().toList());
        assertEquals(N.toList(), IntStream.empty().skip(1).toList());
        assertEquals(0, IntStream.empty().map(e -> e).count());
        assertEquals(0, IntStream.empty().map(e -> e).skip(1).count());
        assertArrayEquals(new int[] {}, IntStream.empty().map(e -> e).toArray());
        assertArrayEquals(new int[] {}, IntStream.empty().map(e -> e).skip(1).toArray());
        assertEquals(N.toList(), IntStream.empty().map(e -> e).toList());
        assertEquals(N.toList(), IntStream.empty().map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toList());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilterWithAction() {
        AtomicInteger droppedCount = new AtomicInteger();
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).count());
        droppedCount.set(0);
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).skip(1).count());
        droppedCount.set(0);
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).toArray());
        droppedCount.set(0);
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).skip(1).toArray());
        droppedCount.set(0);
        assertEquals(N.toList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).toList());
        droppedCount.set(0);
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).skip(1).toList());
        droppedCount.set(0);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).count());
        droppedCount.set(0);
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).skip(1).count());
        droppedCount.set(0);
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).toArray());
        droppedCount.set(0);
        assertArrayEquals(new int[] { 4, 5 },
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).skip(1).toArray());
        droppedCount.set(0);
        assertEquals(N.toList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).toList());
        droppedCount.set(0);
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).skip(1).toList());
    }

    @Test
    public void testFiniteFactoryIteratorsRemainExhaustedAfterFailedNext() {
        final IntIteratorEx[] iterators = { IntStream.range(1, 3).iteratorEx(), IntStream.range(1, 5, 2).iteratorEx(), IntStream.rangeClosed(1, 3).iteratorEx(),
                IntStream.rangeClosed(1, 5, 2).iteratorEx(), IntStream.repeat(7, 10).iteratorEx() };

        for (final IntIteratorEx iter : iterators) {
            iter.advance(Long.MAX_VALUE);
            assertThrows(NoSuchElementException.class, iter::nextInt);
            assertEquals(0, iter.count());
            assertArrayEquals(new int[0], iter.toArray());
        }
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(9, IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).count());
        assertEquals(8, IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 }, IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).toArray());
        assertEquals(N.toList(1, 10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).toList());
        assertEquals(N.toList(10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).toList());
        assertEquals(9, IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).count());
        assertEquals(8, IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).toArray());
        assertEquals(N.toList(1, 10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).toList());
        assertEquals(N.toList(10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToByte() {
        ByteStream byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(6, byteStream.count());

        byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(5, byteStream.skip(1).count());

        byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertArrayEquals(new byte[] { 1, 10, 2, 20, 3, 30 }, byteStream.toArray());

        byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertArrayEquals(new byte[] { 10, 2, 20, 3, 30 }, byteStream.skip(1).toArray());

        byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(N.toList((byte) 1, (byte) 10, (byte) 2, (byte) 20, (byte) 3, (byte) 30), byteStream.toList());

        byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(N.toList((byte) 10, (byte) 2, (byte) 20, (byte) 3, (byte) 30), byteStream.skip(1).toList());

        byteStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(6, byteStream.count());

        byteStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(N.toList((byte) 1, (byte) 10, (byte) 2, (byte) 20, (byte) 3, (byte) 30), byteStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToChar() {
        CharStream charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(6, charStream.count());

        charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(5, charStream.skip(1).count());

        charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertArrayEquals(new char[] { 'A', 'a', 'B', 'b', 'C', 'c' }, charStream.toArray());

        charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertArrayEquals(new char[] { 'a', 'B', 'b', 'C', 'c' }, charStream.skip(1).toArray());

        charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(N.toList('A', 'a', 'B', 'b', 'C', 'c'), charStream.toList());

        charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(N.toList('a', 'B', 'b', 'C', 'c'), charStream.skip(1).toList());

        charStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(6, charStream.count());

        charStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(N.toList('A', 'a', 'B', 'b', 'C', 'c'), charStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToDouble() {
        DoubleStream doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of(i, i * 10));
        assertEquals(6, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of(i, i * 10));
        assertEquals(5, doubleStream.skip(1).count());

        doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of(i, i * 10));
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0 }, doubleStream.toArray(), 0.001);

        doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of(i, i * 10));
        assertArrayEquals(new double[] { 10.0, 2.0, 20.0, 3.0, 30.0 }, doubleStream.skip(1).toArray(), 0.001);

        doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of(i, i * 10));
        assertEquals(N.toList(1.0, 10.0, 2.0, 20.0, 3.0, 30.0), doubleStream.toList());

        doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of(i, i * 10));
        assertEquals(N.toList(10.0, 2.0, 20.0, 3.0, 30.0), doubleStream.skip(1).toList());

        doubleStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToDouble(i -> DoubleStream.of(i, i * 10));
        assertEquals(6, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToDouble(i -> DoubleStream.of(i, i * 10));
        assertEquals(N.toList(1.0, 10.0, 2.0, 20.0, 3.0, 30.0), doubleStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToFloat() {
        FloatStream floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of(i, i * 10));
        assertEquals(6, floatStream.count());

        floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of(i, i * 10));
        assertEquals(5, floatStream.skip(1).count());

        floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of(i, i * 10));
        assertArrayEquals(new float[] { 1f, 10f, 2f, 20f, 3f, 30f }, floatStream.toArray(), 0.001f);

        floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of(i, i * 10));
        assertArrayEquals(new float[] { 10f, 2f, 20f, 3f, 30f }, floatStream.skip(1).toArray(), 0.001f);

        floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of(i, i * 10));
        assertEquals(N.toList(1f, 10f, 2f, 20f, 3f, 30f), floatStream.toList());

        floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of(i, i * 10));
        assertEquals(N.toList(10f, 2f, 20f, 3f, 30f), floatStream.skip(1).toList());

        floatStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToFloat(i -> FloatStream.of(i, i * 10));
        assertEquals(6, floatStream.count());

        floatStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToFloat(i -> FloatStream.of(i, i * 10));
        assertEquals(N.toList(1f, 10f, 2f, 20f, 3f, 30f), floatStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToLong() {
        LongStream longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of(i, i * 10));
        assertEquals(6, longStream.count());

        longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of(i, i * 10));
        assertEquals(5, longStream.skip(1).count());

        longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of(i, i * 10));
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L }, longStream.toArray());

        longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of(i, i * 10));
        assertArrayEquals(new long[] { 10L, 2L, 20L, 3L, 30L }, longStream.skip(1).toArray());

        longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of(i, i * 10));
        assertEquals(N.toList(1L, 10L, 2L, 20L, 3L, 30L), longStream.toList());

        longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of(i, i * 10));
        assertEquals(N.toList(10L, 2L, 20L, 3L, 30L), longStream.skip(1).toList());

        longStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToLong(i -> LongStream.of(i, i * 10));
        assertEquals(6, longStream.count());

        longStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToLong(i -> LongStream.of(i, i * 10));
        assertEquals(N.toList(1L, 10L, 2L, 20L, 3L, 30L), longStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToObj() {
        Stream<String> stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(5, stringStream.skip(1).count());

        stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" }, stringStream.toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3" }, stringStream.skip(1).toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());

        stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(N.toList("B1", "A2", "B2", "A3", "B3"), stringStream.skip(1).toList());

        stringStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapToObj() {
        Stream<String> stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(5, stringStream.skip(1).count());

        stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" }, stringStream.toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3" }, stringStream.skip(1).toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());

        stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(N.toList("B1", "A2", "B2", "A3", "B3"), stringStream.skip(1).toList());

        stringStream = IntStream.of(1, 2, 3).map(e -> e).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).map(e -> e).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToShort() {
        ShortStream shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(6, shortStream.count());

        shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(5, shortStream.skip(1).count());

        shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertArrayEquals(new short[] { 1, 10, 2, 20, 3, 30 }, shortStream.toArray());

        shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertArrayEquals(new short[] { 10, 2, 20, 3, 30 }, shortStream.skip(1).toArray());

        shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(N.toList((short) 1, (short) 10, (short) 2, (short) 20, (short) 3, (short) 30), shortStream.toList());

        shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(N.toList((short) 10, (short) 2, (short) 20, (short) 3, (short) 30), shortStream.skip(1).toList());

        shortStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(6, shortStream.count());

        shortStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(N.toList((short) 1, (short) 10, (short) 2, (short) 20, (short) 3, (short) 30), shortStream.toList());
    }

    // flatten

    @Test
    public void testStreamCreatedFromFlatten() {
        int[][] array = { { 1, 2 }, { 3, 4 }, { 5 } };
        assertEquals(5, IntStream.flatten(array).count());
        assertEquals(4, IntStream.flatten(array).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.flatten(array).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.flatten(array).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.flatten(array).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.flatten(array).skip(1).toList());
        assertEquals(5, IntStream.flatten(array).map(e -> e).count());
        assertEquals(4, IntStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.flatten(array).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.flatten(array).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.flatten(array).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromFlatten3D() {
        int[][][] array = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } };
        assertEquals(8, IntStream.flatten(array).count());
        assertEquals(7, IntStream.flatten(array).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }, IntStream.flatten(array).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8 }, IntStream.flatten(array).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8), IntStream.flatten(array).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6, 7, 8), IntStream.flatten(array).skip(1).toList());
        assertEquals(8, IntStream.flatten(array).map(e -> e).count());
        assertEquals(7, IntStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }, IntStream.flatten(array).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8 }, IntStream.flatten(array).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8), IntStream.flatten(array).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6, 7, 8), IntStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromFlattenVertically() {
        int[][] array = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        assertEquals(9, IntStream.flatten(array, true).count());
        assertEquals(8, IntStream.flatten(array, true).skip(1).count());
        assertArrayEquals(new int[] { 1, 4, 7, 2, 5, 8, 3, 6, 9 }, IntStream.flatten(array, true).toArray());
        assertArrayEquals(new int[] { 4, 7, 2, 5, 8, 3, 6, 9 }, IntStream.flatten(array, true).skip(1).toArray());
        assertEquals(N.toList(1, 4, 7, 2, 5, 8, 3, 6, 9), IntStream.flatten(array, true).toList());
        assertEquals(N.toList(4, 7, 2, 5, 8, 3, 6, 9), IntStream.flatten(array, true).skip(1).toList());
        assertEquals(9, IntStream.flatten(array, true).map(e -> e).count());
        assertEquals(8, IntStream.flatten(array, true).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 4, 7, 2, 5, 8, 3, 6, 9 }, IntStream.flatten(array, true).map(e -> e).toArray());
        assertArrayEquals(new int[] { 4, 7, 2, 5, 8, 3, 6, 9 }, IntStream.flatten(array, true).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 4, 7, 2, 5, 8, 3, 6, 9), IntStream.flatten(array, true).map(e -> e).toList());
        assertEquals(N.toList(4, 7, 2, 5, 8, 3, 6, 9), IntStream.flatten(array, true).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromFlattenWithAlignment() {
        int[][] array = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        assertEquals(9, IntStream.flatten(array, 0, true).count());
        assertEquals(8, IntStream.flatten(array, 0, true).skip(1).count());
        assertArrayEquals(new int[] { 1, 3, 6, 2, 4, 0, 0, 5, 0 }, IntStream.flatten(array, 0, true).toArray());
        assertArrayEquals(new int[] { 3, 6, 2, 4, 0, 0, 5, 0 }, IntStream.flatten(array, 0, true).skip(1).toArray());
        assertEquals(N.toList(1, 3, 6, 2, 4, 0, 0, 5, 0), IntStream.flatten(array, 0, true).toList());
        assertEquals(N.toList(3, 6, 2, 4, 0, 0, 5, 0), IntStream.flatten(array, 0, true).skip(1).toList());
        assertEquals(9, IntStream.flatten(array, 0, true).map(e -> e).count());
        assertEquals(8, IntStream.flatten(array, 0, true).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 3, 6, 2, 4, 0, 0, 5, 0 }, IntStream.flatten(array, 0, true).map(e -> e).toArray());
        assertArrayEquals(new int[] { 3, 6, 2, 4, 0, 0, 5, 0 }, IntStream.flatten(array, 0, true).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 3, 6, 2, 4, 0, 0, 5, 0), IntStream.flatten(array, 0, true).map(e -> e).toList());
        assertEquals(N.toList(3, 6, 2, 4, 0, 0, 5, 0), IntStream.flatten(array, 0, true).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattMap() {
        assertEquals(9, IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).count());
        assertEquals(8, IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).toArray());
        assertEquals(N.toList(1, 10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).toList());
        assertEquals(N.toList(10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).toList());
        assertEquals(9, IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).count());
        assertEquals(8, IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).toArray());
        assertEquals(N.toList(1, 10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).toList());
        assertEquals(N.toList(10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromJdkStream() {
        java.util.stream.IntStream jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(5, IntStream.from(jdkStream).count());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(4, IntStream.from(jdkStream).skip(1).count());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.from(jdkStream).toArray());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.from(jdkStream).skip(1).toArray());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.from(jdkStream).toList());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(N.toList(2, 3, 4, 5), IntStream.from(jdkStream).skip(1).toList());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(5, IntStream.from(jdkStream).map(e -> e).count());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(4, IntStream.from(jdkStream).map(e -> e).skip(1).count());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.from(jdkStream).map(e -> e).toArray());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.from(jdkStream).map(e -> e).skip(1).toArray());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.from(jdkStream).map(e -> e).toList());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(N.toList(2, 3, 4, 5), IntStream.from(jdkStream).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromGenerate() {
        AtomicInteger counter = new AtomicInteger(0);
        assertEquals(5, IntStream.generate(() -> counter.getAndIncrement()).limit(5).count());

        counter.set(0);
        assertEquals(4, IntStream.generate(() -> counter.getAndIncrement()).limit(5).skip(1).count());

        counter.set(0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.generate(() -> counter.getAndIncrement()).limit(5).toArray());

        counter.set(0);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.generate(() -> counter.getAndIncrement()).limit(5).skip(1).toArray());

        counter.set(0);
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.generate(() -> counter.getAndIncrement()).limit(5).toList());

        counter.set(0);
        assertEquals(N.toList(1, 2, 3, 4), IntStream.generate(() -> counter.getAndIncrement()).limit(5).skip(1).toList());

        counter.set(0);
        assertEquals(5, IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).count());

        counter.set(0);
        assertEquals(4, IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).skip(1).count());

        counter.set(0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).toArray());

        counter.set(0);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).skip(1).toArray());

        counter.set(0);
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).toList());

        counter.set(0);
        assertEquals(N.toList(1, 2, 3, 4), IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).skip(1).toList());
    }

    @Test
    public void testIfEmpty() {
        AtomicBoolean called = new AtomicBoolean(false);

        createIntStream(1, 2, 3).ifEmpty(() -> called.set(true)).count();
        assertFalse(called.get());

        IntStream.empty().ifEmpty(() -> called.set(true)).count();
        assertTrue(called.get());
    }

    @Test
    public void testStreamCreatedAfterIndexed() {
        Stream<IndexedInt> indexedStream = IntStream.of(1, 2, 3, 4, 5).indexed();
        assertEquals(5, indexedStream.count());

        indexedStream = IntStream.of(1, 2, 3, 4, 5).indexed();
        assertEquals(4, indexedStream.skip(1).count());

        indexedStream = IntStream.of(1, 2, 3, 4, 5).indexed();
        IndexedInt[] indexedArray = indexedStream.toArray(IndexedInt[]::new);
        assertEquals(5, indexedArray.length);
        assertEquals(0, indexedArray[0].index());
        assertEquals(1, indexedArray[0].value());
        assertEquals(4, indexedArray[4].index());
        assertEquals(5, indexedArray[4].value());

        indexedStream = IntStream.of(1, 2, 3, 4, 5).indexed();
        List<IndexedInt> indexedList = indexedStream.toList();
        assertEquals(5, indexedList.size());
        assertEquals(0, indexedList.get(0).index());
        assertEquals(1, indexedList.get(0).value());

        indexedStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).indexed();
        assertEquals(5, indexedStream.count());

        indexedStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).indexed();
        indexedList = indexedStream.toList();
        assertEquals(5, indexedList.size());
    }

    @Test
    public void testStreamCreatedAfterIntersection() {
        Collection<Integer> c = Arrays.asList(3, 4, 5, 6);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).intersection(c).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).intersection(c).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).intersection(c).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).intersection(c).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).intersection(c).toList());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).intersection(c).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).toList());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).skip(1).toList());
    }

    @Test
    public void testIsParallel_DefaultIsFalse() {
        stream = IntStream.of(1, 2, 3, 4, 5);
        assertFalse(stream.isParallel());
    }

    @Test
    public void testIsParallel_HappyPath() {
        assertFalse(IntStream.of(1, 2, 3).isParallel());
        assertTrue(IntStream.of(1, 2, 3).parallel().isParallel());
    }

    // iterate

    @Test
    public void testStreamCreatedFromIterate() {
        AtomicInteger counter = new AtomicInteger(0);
        assertEquals(5, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).count());

        counter.set(0);
        assertEquals(4, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).skip(1).count());

        counter.set(0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).toArray());

        counter.set(0);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).skip(1).toArray());

        counter.set(0);
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).toList());

        counter.set(0);
        assertEquals(N.toList(1, 2, 3, 4), IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).skip(1).toList());

        counter.set(0);
        assertEquals(5, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).count());

        counter.set(0);
        assertEquals(4, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).skip(1).count());

        counter.set(0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).toArray());

        counter.set(0);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).skip(1).toArray());

        counter.set(0);
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).toList());

        counter.set(0);
        assertEquals(N.toList(1, 2, 3, 4), IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromIterateInfinite() {
        assertEquals(10, IntStream.iterate(0, i -> i + 1).limit(10).count());
        assertEquals(9, IntStream.iterate(0, i -> i + 1).limit(10).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, IntStream.iterate(0, i -> i + 1).limit(10).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, IntStream.iterate(0, i -> i + 1).limit(10).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), IntStream.iterate(0, i -> i + 1).limit(10).toList());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9), IntStream.iterate(0, i -> i + 1).limit(10).skip(1).toList());
        assertEquals(10, IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).count());
        assertEquals(9, IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).toList());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9), IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromIterateWithInit() {
        AtomicInteger counter = new AtomicInteger(0);
        assertEquals(6, IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).count());

        counter.set(0);
        assertEquals(5, IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).skip(1).count());

        counter.set(0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).toArray());

        counter.set(0);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).skip(1).toArray());

        counter.set(0);
        assertEquals(N.toList(0, 1, 2, 3, 4, 5), IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).toList());

        counter.set(0);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).skip(1).toList());

        counter.set(0);
        assertEquals(6, IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).map(e -> e).count());

        counter.set(0);
        assertEquals(5, IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).map(e -> e).skip(1).count());

        counter.set(0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).map(e -> e).toArray());

        counter.set(0);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).map(e -> e).skip(1).toArray());

        counter.set(0);
        assertEquals(N.toList(0, 1, 2, 3, 4, 5), IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).map(e -> e).toList());

        counter.set(0);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromIterateWithPredicate() {
        assertEquals(5, IntStream.iterate(0, i -> i < 5, i -> i + 1).count());
        assertEquals(4, IntStream.iterate(0, i -> i < 5, i -> i + 1).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.iterate(0, i -> i < 5, i -> i + 1).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.iterate(0, i -> i < 5, i -> i + 1).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.iterate(0, i -> i < 5, i -> i + 1).toList());
        assertEquals(N.toList(1, 2, 3, 4), IntStream.iterate(0, i -> i < 5, i -> i + 1).skip(1).toList());
        assertEquals(5, IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).count());
        assertEquals(4, IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).toList());
        assertEquals(N.toList(1, 2, 3, 4), IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).skip(1).toList());
    }

    // iterator

    @Test
    public void testBooleanSupplierIteratorsStayExhausted() {
        final AtomicInteger conditionCalls = new AtomicInteger();
        IntIterator iter = IntStream.iterate(() -> conditionCalls.getAndIncrement() > 0, () -> 1).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextInt);
        assertEquals(1, conditionCalls.get());

        conditionCalls.set(0);
        iter = IntStream.iterate(1, () -> conditionCalls.getAndIncrement() > 0, value -> value + 1).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextInt);
        assertEquals(1, conditionCalls.get());
    }

    @Test
    public void testIterator_BasicIteration() {
        IntIterator iterator = IntStream.of(1, 2, 3, 4, 5).iterator();
        int count = 0;
        while (iterator.hasNext()) {
            iterator.nextInt();
            count++;
        }
        assertEquals(5, count);
    }

    @Test
    public void testIterator_EmptyStream() {
        IntIterator iterator = IntStream.empty().iterator();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testLargeStreamOperations() {
        int size = 10_000;
        IntStream stream = IntStream.range(0, size);

        assertEquals(size, stream.count());

        stream = IntStream.range(0, size);
        long expectedSum = (long) size * (size - 1) / 2;
        assertEquals(expectedSum, stream.sum());
    }

    @Test
    public void testInfiniteStreamWithLimit() {
        IntStream stream = IntStream.iterate(1, n -> n + 1).limit(10);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, stream.toArray());
    }

    @Test
    public void testNegativeLimit() {
        assertThrows(IllegalArgumentException.class, () -> createIntStream(1, 2, 3).limit(-1));
    }

    // map

    @Test
    public void testStreamCreatedAfterFlatmap() {
        assertEquals(9, IntStream.of(1, 2, 3).flatMapArray(i -> new int[] { i, i * 10, i * 100 }).count());
        assertEquals(8, IntStream.of(1, 2, 3).flatMapArray(i -> new int[] { i, i * 10, i * 100 }).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).flatMapArray(i -> new int[] { i, i * 10, i * 100 }).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).flatMapArray(i -> new int[] { i, i * 10, i * 100 }).skip(1).toArray());
        assertEquals(N.toList(1, 10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).flatMapArray(i -> new int[] { i, i * 10, i * 100 }).toList());
        assertEquals(N.toList(10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).flatMapArray(i -> new int[] { i, i * 10, i * 100 }).skip(1).toList());
        assertEquals(9, IntStream.of(1, 2, 3).map(e -> e).flatMapArray(i -> new int[] { i, i * 10, i * 100 }).count());
        assertEquals(8, IntStream.of(1, 2, 3).map(e -> e).flatMapArray(i -> new int[] { i, i * 10, i * 100 }).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flatMapArray(i -> new int[] { i, i * 10, i * 100 }).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flatMapArray(i -> new int[] { i, i * 10, i * 100 }).skip(1).toArray());
        assertEquals(N.toList(1, 10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).map(e -> e).flatMapArray(i -> new int[] { i, i * 10, i * 100 }).toList());
        assertEquals(N.toList(10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).map(e -> e).flatMapArray(i -> new int[] { i, i * 10, i * 100 }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattmapToObj() {
        Stream<String> stringStream = IntStream.of(1, 2, 3).flatMapArrayToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).flatMapArrayToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(5, stringStream.skip(1).count());

        stringStream = IntStream.of(1, 2, 3).flatMapArrayToObj(i -> new String[] { "A" + i, "B" + i });
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" }, stringStream.toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flatMapArrayToObj(i -> new String[] { "A" + i, "B" + i });
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3" }, stringStream.skip(1).toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flatMapArrayToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());

        stringStream = IntStream.of(1, 2, 3).flatMapArrayToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(N.toList("B1", "A2", "B2", "A3", "B3"), stringStream.skip(1).toList());

        stringStream = IntStream.of(1, 2, 3).map(e -> e).flatMapArrayToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).map(e -> e).flatMapArrayToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).toList());
        assertEquals(N.toList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).toList());
        assertEquals(N.toList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapMulti() {
        assertEquals(6, IntStream.of(1, 2, 3).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).count());
        assertEquals(5, IntStream.of(1, 2, 3).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, IntStream.of(1, 2, 3).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).toArray());
        assertArrayEquals(new int[] { 10, 2, 20, 3, 30 }, IntStream.of(1, 2, 3).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).skip(1).toArray());
        assertEquals(N.toList(1, 10, 2, 20, 3, 30), IntStream.of(1, 2, 3).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).toList());
        assertEquals(N.toList(10, 2, 20, 3, 30), IntStream.of(1, 2, 3).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).skip(1).toList());
        assertEquals(6, IntStream.of(1, 2, 3).map(e -> e).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).count());
        assertEquals(5, IntStream.of(1, 2, 3).map(e -> e).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, IntStream.of(1, 2, 3).map(e -> e).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).toArray());
        assertArrayEquals(new int[] { 10, 2, 20, 3, 30 }, IntStream.of(1, 2, 3).map(e -> e).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).skip(1).toArray());
        assertEquals(N.toList(1, 10, 2, 20, 3, 30), IntStream.of(1, 2, 3).map(e -> e).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).toList());
        assertEquals(N.toList(10, 2, 20, 3, 30), IntStream.of(1, 2, 3).map(e -> e).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartial() {
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).skip(1).count());
        assertArrayEquals(new int[] { 10, 30, 50 },
                IntStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).toArray());
        assertArrayEquals(new int[] { 30, 50 },
                IntStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).skip(1).toArray());
        assertEquals(N.toList(10, 30, 50), IntStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).toList());
        assertEquals(N.toList(30, 50), IntStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).skip(1).count());
        assertArrayEquals(new int[] { 10, 30, 50 },
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).toArray());
        assertArrayEquals(new int[] { 30, 50 },
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).skip(1).toArray());
        assertEquals(N.toList(10, 30, 50),
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).toList());
        assertEquals(N.toList(30, 50),
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartialJdk() {
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10)).count());
        assertEquals(2,
                IntStream.of(1, 2, 3, 4, 5).mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10)).skip(1).count());
        assertArrayEquals(new int[] { 10, 30, 50 },
                IntStream.of(1, 2, 3, 4, 5).mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10)).toArray());
        assertArrayEquals(new int[] { 30, 50 },
                IntStream.of(1, 2, 3, 4, 5)
                        .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList(10, 30, 50),
                IntStream.of(1, 2, 3, 4, 5).mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10)).toList());
        assertEquals(N.toList(30, 50),
                IntStream.of(1, 2, 3, 4, 5).mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10)).skip(1).toList());
        assertEquals(3,
                IntStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10))
                        .count());
        assertEquals(2,
                IntStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10))
                        .skip(1)
                        .count());
        assertArrayEquals(new int[] { 10, 30, 50 },
                IntStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10))
                        .toArray());
        assertArrayEquals(new int[] { 30, 50 },
                IntStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList(10, 30, 50),
                IntStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10))
                        .toList());
        assertEquals(N.toList(30, 50),
                IntStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterMapToByte() {
        ByteStream byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertEquals(5, byteStream.count());

        byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertEquals(4, byteStream.skip(1).count());

        byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, byteStream.toArray());

        byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, byteStream.skip(1).toArray());

        byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), byteStream.toList());

        byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertEquals(N.toList((byte) 2, (byte) 3, (byte) 4, (byte) 5), byteStream.skip(1).toList());

        byteStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToByte(i -> (byte) i);
        assertEquals(5, byteStream.count());

        byteStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToByte(i -> (byte) i);
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), byteStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToChar() {
        CharStream charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertEquals(5, charStream.count());

        charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertEquals(4, charStream.skip(1).count());

        charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertArrayEquals(new char[] { 'A', 'B', 'C', 'D', 'E' }, charStream.toArray());

        charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertArrayEquals(new char[] { 'B', 'C', 'D', 'E' }, charStream.skip(1).toArray());

        charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertEquals(N.toList('A', 'B', 'C', 'D', 'E'), charStream.toList());

        charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertEquals(N.toList('B', 'C', 'D', 'E'), charStream.skip(1).toList());

        charStream = IntStream.of(65, 66, 67, 68, 69).map(e -> e).mapToChar(i -> (char) i);
        assertEquals(5, charStream.count());

        charStream = IntStream.of(65, 66, 67, 68, 69).map(e -> e).mapToChar(i -> (char) i);
        assertEquals(N.toList('A', 'B', 'C', 'D', 'E'), charStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToDouble() {
        DoubleStream doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertEquals(5, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertEquals(4, doubleStream.skip(1).count());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, doubleStream.toArray(), 0.001);

        doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0, 5.0 }, doubleStream.skip(1).toArray(), 0.001);

        doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), doubleStream.toList());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), doubleStream.skip(1).toList());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(i -> (double) i);
        assertEquals(5, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(i -> (double) i);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), doubleStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToFloat() {
        FloatStream floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertEquals(5, floatStream.count());

        floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertEquals(4, floatStream.skip(1).count());

        floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertArrayEquals(new float[] { 1f, 2f, 3f, 4f, 5f }, floatStream.toArray(), 0.001f);

        floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertArrayEquals(new float[] { 2f, 3f, 4f, 5f }, floatStream.skip(1).toArray(), 0.001f);

        floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), floatStream.toList());

        floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertEquals(N.toList(2f, 3f, 4f, 5f), floatStream.skip(1).toList());

        floatStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToFloat(i -> (float) i);
        assertEquals(5, floatStream.count());

        floatStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToFloat(i -> (float) i);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), floatStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToLong() {
        LongStream longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertEquals(5, longStream.count());

        longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertEquals(4, longStream.skip(1).count());

        longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, longStream.toArray());

        longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertArrayEquals(new long[] { 2L, 3L, 4L, 5L }, longStream.skip(1).toArray());

        longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertEquals(N.toList(1L, 2L, 3L, 4L, 5L), longStream.toList());

        longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertEquals(N.toList(2L, 3L, 4L, 5L), longStream.skip(1).toList());

        longStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToLong(i -> (long) i);
        assertEquals(5, longStream.count());

        longStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToLong(i -> (long) i);
        assertEquals(N.toList(1L, 2L, 3L, 4L, 5L), longStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToObj() {
        Stream<String> stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertEquals(5, stringStream.count());

        stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertEquals(4, stringStream.skip(1).count());

        stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertArrayEquals(new String[] { "Item1", "Item2", "Item3", "Item4", "Item5" }, stringStream.toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertArrayEquals(new String[] { "Item2", "Item3", "Item4", "Item5" }, stringStream.skip(1).toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertEquals(N.toList("Item1", "Item2", "Item3", "Item4", "Item5"), stringStream.toList());

        stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertEquals(N.toList("Item2", "Item3", "Item4", "Item5"), stringStream.skip(1).toList());

        stringStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(i -> "Item" + i);
        assertEquals(5, stringStream.count());

        stringStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(i -> "Item" + i);
        assertEquals(N.toList("Item1", "Item2", "Item3", "Item4", "Item5"), stringStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToShort() {
        ShortStream shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertEquals(5, shortStream.count());

        shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertEquals(4, shortStream.skip(1).count());

        shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, shortStream.toArray());

        shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertArrayEquals(new short[] { 2, 3, 4, 5 }, shortStream.skip(1).toArray());

        shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), shortStream.toList());

        shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5), shortStream.skip(1).toList());

        shortStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToShort(i -> (short) i);
        assertEquals(5, shortStream.count());

        shortStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToShort(i -> (short) i);
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), shortStream.toList());
    }

    @Test
    public void testStreamCreatedFromMerge3Arrays() {
        int[] array1 = { 1, 4, 7 };
        int[] array2 = { 2, 5, 8 };
        int[] array3 = { 3, 6, 9 };
        assertEquals(9, IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(8, IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6, 7, 8, 9),
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        assertEquals(9, IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        assertEquals(8,
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6, 7, 8, 9),
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromMergeArrays() {
        int[] array1 = { 1, 3, 5 };
        int[] array2 = { 2, 4, 6 };
        assertEquals(6, IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(5, IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 },
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6 },
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6), IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6),
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        assertEquals(6, IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        assertEquals(5, IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 },
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6 },
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6),
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeWith() {
        IntStream streamB = IntStream.of(2, 4, 6);
        assertEquals(6, IntStream.of(1, 3, 5).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());

        streamB = IntStream.of(2, 4, 6);
        assertEquals(5, IntStream.of(1, 3, 5).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());

        streamB = IntStream.of(2, 4, 6);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 },
                IntStream.of(1, 3, 5).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());

        streamB = IntStream.of(2, 4, 6);
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6 },
                IntStream.of(1, 3, 5).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray());

        streamB = IntStream.of(2, 4, 6);
        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                IntStream.of(1, 3, 5).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        streamB = IntStream.of(2, 4, 6);
        assertEquals(N.toList(2, 3, 4, 5, 6),
                IntStream.of(1, 3, 5).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());

        streamB = IntStream.of(2, 4, 6);
        assertEquals(6, IntStream.of(1, 3, 5).map(e -> e).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());

        streamB = IntStream.of(2, 4, 6);
        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                IntStream.of(1, 3, 5).map(e -> e).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
    }

    @Test
    public void testStreamCreatedFromOfCodePoints() {
        String str = "Hello";
        assertEquals(5, IntStream.ofCodePoints(str).count());
        assertEquals(4, IntStream.ofCodePoints(str).skip(1).count());
        assertArrayEquals(new int[] { 72, 101, 108, 108, 111 }, IntStream.ofCodePoints(str).toArray());
        assertArrayEquals(new int[] { 101, 108, 108, 111 }, IntStream.ofCodePoints(str).skip(1).toArray());
        assertEquals(N.toList(72, 101, 108, 108, 111), IntStream.ofCodePoints(str).toList());
        assertEquals(N.toList(101, 108, 108, 111), IntStream.ofCodePoints(str).skip(1).toList());
        assertEquals(5, IntStream.ofCodePoints(str).map(e -> e).count());
        assertEquals(4, IntStream.ofCodePoints(str).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 72, 101, 108, 108, 111 }, IntStream.ofCodePoints(str).map(e -> e).toArray());
        assertArrayEquals(new int[] { 101, 108, 108, 111 }, IntStream.ofCodePoints(str).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(72, 101, 108, 108, 111), IntStream.ofCodePoints(str).map(e -> e).toList());
        assertEquals(N.toList(101, 108, 108, 111), IntStream.ofCodePoints(str).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfIndices() {
        assertEquals(5, IntStream.ofIndices(5).count());
        assertEquals(4, IntStream.ofIndices(5).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.ofIndices(5).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.ofIndices(5).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.ofIndices(5).toList());
        assertEquals(N.toList(1, 2, 3, 4), IntStream.ofIndices(5).skip(1).toList());
        assertEquals(5, IntStream.ofIndices(5).map(e -> e).count());
        assertEquals(4, IntStream.ofIndices(5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.ofIndices(5).map(e -> e).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.ofIndices(5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.ofIndices(5).map(e -> e).toList());
        assertEquals(N.toList(1, 2, 3, 4), IntStream.ofIndices(5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfIndicesWithStep() {
        assertEquals(4, IntStream.ofIndices(10, 3).count());
        assertEquals(3, IntStream.ofIndices(10, 3).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.ofIndices(10, 3).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.ofIndices(10, 3).skip(1).toArray());
        assertEquals(N.toList(0, 3, 6, 9), IntStream.ofIndices(10, 3).toList());
        assertEquals(N.toList(3, 6, 9), IntStream.ofIndices(10, 3).skip(1).toList());
        assertEquals(4, IntStream.ofIndices(10, 3).map(e -> e).count());
        assertEquals(3, IntStream.ofIndices(10, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.ofIndices(10, 3).map(e -> e).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.ofIndices(10, 3).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(0, 3, 6, 9), IntStream.ofIndices(10, 3).map(e -> e).toList());
        assertEquals(N.toList(3, 6, 9), IntStream.ofIndices(10, 3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfNullable() {
        assertEquals(1, IntStream.ofNullable(5).count());
        assertEquals(0, IntStream.ofNullable(5).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.ofNullable(5).toArray());
        assertArrayEquals(new int[] {}, IntStream.ofNullable(5).skip(1).toArray());
        assertEquals(N.toList(5), IntStream.ofNullable(5).toList());
        assertEquals(N.toList(), IntStream.ofNullable(5).skip(1).toList());
        assertEquals(1, IntStream.ofNullable(5).map(e -> e).count());
        assertEquals(0, IntStream.ofNullable(5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.ofNullable(5).map(e -> e).toArray());
        assertArrayEquals(new int[] {}, IntStream.ofNullable(5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(5), IntStream.ofNullable(5).map(e -> e).toList());
        assertEquals(N.toList(), IntStream.ofNullable(5).map(e -> e).skip(1).toList());

        assertEquals(0, IntStream.ofNullable(null).count());
        assertEquals(0, IntStream.ofNullable(null).skip(1).count());
        assertArrayEquals(new int[] {}, IntStream.ofNullable(null).toArray());
        assertArrayEquals(new int[] {}, IntStream.ofNullable(null).skip(1).toArray());
        assertEquals(N.toList(), IntStream.ofNullable(null).toList());
        assertEquals(N.toList(), IntStream.ofNullable(null).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrepend() {
        assertEquals(8, IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).count());
        assertEquals(7, IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).skip(1).count());
        assertArrayEquals(new int[] { -2, -1, 0, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).toArray());
        assertArrayEquals(new int[] { -1, 0, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).skip(1).toArray());
        assertEquals(N.toList(-2, -1, 0, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).toList());
        assertEquals(N.toList(-1, 0, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).skip(1).toList());
        assertEquals(8, IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).count());
        assertEquals(7, IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).skip(1).count());
        assertArrayEquals(new int[] { -2, -1, 0, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).toArray());
        assertArrayEquals(new int[] { -1, 0, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).skip(1).toArray());
        assertEquals(N.toList(-2, -1, 0, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).toList());
        assertEquals(N.toList(-1, 0, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).skip(1).toList());
    }

    @Test
    public void testPrintln() {
        assertDoesNotThrow(() -> {
            createIntStream(1, 2, 3).println();
        });
    }

    @Test
    public void testPsp_HappyPath() {
        int[] result = IntStream.of(1, 2, 3, 4, 5).psp(s -> s.filter(x -> x % 2 != 0)).toArray();
        Arrays.sort(result);
        assertArrayEquals(new int[] { 1, 3, 5 }, result);
    }

    @Test
    public void testStreamCreatedFromRandom() {
        assertEquals(10, IntStream.random().limit(10).count());
        assertEquals(9, IntStream.random().limit(10).skip(1).count());
        int[] randomArray = IntStream.random().limit(10).toArray();
        assertEquals(10, randomArray.length);
        randomArray = IntStream.random().limit(10).skip(1).toArray();
        assertEquals(9, randomArray.length);
        List<Integer> randomList = IntStream.random().limit(10).toList();
        assertEquals(10, randomList.size());
        randomList = IntStream.random().limit(10).skip(1).toList();
        assertEquals(9, randomList.size());
        assertEquals(10, IntStream.random().map(e -> e).limit(10).count());
        assertEquals(9, IntStream.random().map(e -> e).limit(10).skip(1).count());
        randomArray = IntStream.random().map(e -> e).limit(10).toArray();
        assertEquals(10, randomArray.length);
        randomArray = IntStream.random().map(e -> e).limit(10).skip(1).toArray();
        assertEquals(9, randomArray.length);
        randomList = IntStream.random().map(e -> e).limit(10).toList();
        assertEquals(10, randomList.size());
        randomList = IntStream.random().map(e -> e).limit(10).skip(1).toList();
        assertEquals(9, randomList.size());
    }

    @Test
    public void testStreamCreatedFromRandomWithRange() {
        assertEquals(10, IntStream.random(0, 100).limit(10).count());
        assertEquals(9, IntStream.random(0, 100).limit(10).skip(1).count());
        int[] randomArray = IntStream.random(0, 100).limit(10).toArray();
        assertEquals(10, randomArray.length);
        for (int i : randomArray) {
            assertTrue(i >= 0 && i < 100);
        }
        randomArray = IntStream.random(0, 100).limit(10).skip(1).toArray();
        assertEquals(9, randomArray.length);
        for (int i : randomArray) {
            assertTrue(i >= 0 && i < 100);
        }
        List<Integer> randomList = IntStream.random(0, 100).limit(10).toList();
        assertEquals(10, randomList.size());
        for (int i : randomList) {
            assertTrue(i >= 0 && i < 100);
        }
        randomList = IntStream.random(0, 100).limit(10).skip(1).toList();
        assertEquals(9, randomList.size());
        for (int i : randomList) {
            assertTrue(i >= 0 && i < 100);
        }
        assertEquals(10, IntStream.random(0, 100).map(e -> e).limit(10).count());
        assertEquals(9, IntStream.random(0, 100).map(e -> e).limit(10).skip(1).count());
        randomArray = IntStream.random(0, 100).map(e -> e).limit(10).toArray();
        assertEquals(10, randomArray.length);
        for (int i : randomArray) {
            assertTrue(i >= 0 && i < 100);
        }
        randomArray = IntStream.random(0, 100).map(e -> e).limit(10).skip(1).toArray();
        assertEquals(9, randomArray.length);
        for (int i : randomArray) {
            assertTrue(i >= 0 && i < 100);
        }
        randomList = IntStream.random(0, 100).map(e -> e).limit(10).toList();
        assertEquals(10, randomList.size());
        for (int i : randomList) {
            assertTrue(i >= 0 && i < 100);
        }
        randomList = IntStream.random(0, 100).map(e -> e).limit(10).skip(1).toList();
        assertEquals(9, randomList.size());
        for (int i : randomList) {
            assertTrue(i >= 0 && i < 100);
        }
    }

    @Test
    public void testStreamCreatedFromRange() {
        assertEquals(5, IntStream.range(0, 5).count());
        assertEquals(4, IntStream.range(0, 5).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.range(0, 5).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.range(0, 5).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.range(0, 5).toList());
        assertEquals(N.toList(1, 2, 3, 4), IntStream.range(0, 5).skip(1).toList());
        assertEquals(5, IntStream.range(0, 5).map(e -> e).count());
        assertEquals(4, IntStream.range(0, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.range(0, 5).map(e -> e).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.range(0, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(0, 1, 2, 3, 4), IntStream.range(0, 5).map(e -> e).toList());
        assertEquals(N.toList(1, 2, 3, 4), IntStream.range(0, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromRangeWithStep() {
        assertEquals(4, IntStream.range(0, 10, 3).count());
        assertEquals(3, IntStream.range(0, 10, 3).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.range(0, 10, 3).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.range(0, 10, 3).skip(1).toArray());
        assertEquals(N.toList(0, 3, 6, 9), IntStream.range(0, 10, 3).toList());
        assertEquals(N.toList(3, 6, 9), IntStream.range(0, 10, 3).skip(1).toList());
        assertEquals(4, IntStream.range(0, 10, 3).map(e -> e).count());
        assertEquals(3, IntStream.range(0, 10, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.range(0, 10, 3).map(e -> e).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.range(0, 10, 3).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(0, 3, 6, 9), IntStream.range(0, 10, 3).map(e -> e).toList());
        assertEquals(N.toList(3, 6, 9), IntStream.range(0, 10, 3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRangeMap() {
        assertEquals(3, IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).count());
        assertEquals(2, IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).count());
        assertArrayEquals(new int[] { 3, 9, 16 },
                IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).toArray());
        assertArrayEquals(new int[] { 9, 16 },
                IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).toArray());
        assertEquals(N.toList(3, 9, 16), IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).toList());
        assertEquals(N.toList(9, 16), IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).count());
        assertEquals(2, IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).count());
        assertArrayEquals(new int[] { 3, 9, 16 },
                IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).toArray());
        assertArrayEquals(new int[] { 9, 16 },
                IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).toArray());
        assertEquals(N.toList(3, 9, 16),
                IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).toList());
        assertEquals(N.toList(9, 16),
                IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRangeMapToObj() {
        Stream<String> stringStream = IntStream.of(1, 2, 4, 5, 8).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertEquals(3, stringStream.count());

        stringStream = IntStream.of(1, 2, 4, 5, 8).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertEquals(2, stringStream.skip(1).count());

        stringStream = IntStream.of(1, 2, 4, 5, 8).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertArrayEquals(new String[] { "1-2", "4-5", "8-8" }, stringStream.toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 4, 5, 8).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertArrayEquals(new String[] { "4-5", "8-8" }, stringStream.skip(1).toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 4, 5, 8).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertEquals(N.toList("1-2", "4-5", "8-8"), stringStream.toList());

        stringStream = IntStream.of(1, 2, 4, 5, 8).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertEquals(N.toList("4-5", "8-8"), stringStream.skip(1).toList());

        stringStream = IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertEquals(3, stringStream.count());

        stringStream = IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertEquals(N.toList("1-2", "4-5", "8-8"), stringStream.toList());
    }

    @Test
    public void testStreamCreatedFromRepeat() {
        assertEquals(5, IntStream.repeat(7, 5).count());
        assertEquals(4, IntStream.repeat(7, 5).skip(1).count());
        assertArrayEquals(new int[] { 7, 7, 7, 7, 7 }, IntStream.repeat(7, 5).toArray());
        assertArrayEquals(new int[] { 7, 7, 7, 7 }, IntStream.repeat(7, 5).skip(1).toArray());
        assertEquals(N.toList(7, 7, 7, 7, 7), IntStream.repeat(7, 5).toList());
        assertEquals(N.toList(7, 7, 7, 7), IntStream.repeat(7, 5).skip(1).toList());
        assertEquals(5, IntStream.repeat(7, 5).map(e -> e).count());
        assertEquals(4, IntStream.repeat(7, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 7, 7, 7, 7, 7 }, IntStream.repeat(7, 5).map(e -> e).toArray());
        assertArrayEquals(new int[] { 7, 7, 7, 7 }, IntStream.repeat(7, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(7, 7, 7, 7, 7), IntStream.repeat(7, 5).map(e -> e).toList());
        assertEquals(N.toList(7, 7, 7, 7), IntStream.repeat(7, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReversed() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).reversed().count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).reversed().skip(1).count());
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, IntStream.of(1, 2, 3, 4, 5).reversed().toArray());
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, IntStream.of(1, 2, 3, 4, 5).reversed().skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), IntStream.of(1, 2, 3, 4, 5).reversed().toList());
        assertEquals(N.toList(4, 3, 2, 1), IntStream.of(1, 2, 3, 4, 5).reversed().skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).count());
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toArray());
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toList());
        assertEquals(N.toList(4, 3, 2, 1), IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, IntStream.of(3, 1, 4, 2, 5).reverseSorted().count());
        assertEquals(4, IntStream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).count());
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, IntStream.of(3, 1, 4, 2, 5).reverseSorted().toArray());
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, IntStream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), IntStream.of(3, 1, 4, 2, 5).reverseSorted().toList());
        assertEquals(N.toList(4, 3, 2, 1), IntStream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).toList());
        assertEquals(5, IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().count());
        assertEquals(4, IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().toArray());
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().toList());
        assertEquals(N.toList(4, 3, 2, 1), IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRotated() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).rotated(2).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).count());
        assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).rotated(2).toArray());
        assertArrayEquals(new int[] { 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toArray());
        assertEquals(N.toList(4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).rotated(2).toList());
        assertEquals(N.toList(5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).count());
        assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toArray());
        assertArrayEquals(new int[] { 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toArray());
        assertEquals(N.toList(4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toList());
        assertEquals(N.toList(5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toList());
    }

    // scan

    @Test
    public void testStreamCreatedAfterScan() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 1, 3, 6, 10, 15 }, IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 3, 6, 10, 15 }, IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(1, 3, 6, 10, 15), IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toList());
        assertEquals(N.toList(3, 6, 10, 15), IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 1, 3, 6, 10, 15 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 3, 6, 10, 15 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(1, 3, 6, 10, 15), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toList());
        assertEquals(N.toList(3, 6, 10, 15), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInit() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toList());
        assertEquals(N.toList(13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toList());
        assertEquals(N.toList(13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInitIncluded() {
        assertEquals(6, IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 10, 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(10, 11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(N.toList(11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toList());
        assertEquals(6, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 10, 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(10, 11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(N.toList(11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterShuffled() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).shuffled().count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).shuffled().skip(1).count());
        int[] shuffledArray = IntStream.of(1, 2, 3, 4, 5).shuffled().toArray();
        assertEquals(5, shuffledArray.length);
        assertHaveSameElements(N.toList(shuffledArray), N.toList(1, 2, 3, 4, 5));

        List<Integer> shuffledList = IntStream.of(1, 2, 3, 4, 5).shuffled().toList();
        assertEquals(5, shuffledList.size());
        assertHaveSameElements(shuffledList, N.toList(1, 2, 3, 4, 5));

        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterShuffledWithRandom() {
        Random rnd = new Random(42);
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).shuffled(rnd).skip(1).count());
        rnd = new Random(42);
        int[] shuffledArray = IntStream.of(1, 2, 3, 4, 5).shuffled(rnd).toArray();
        assertEquals(5, shuffledArray.length);
        assertHaveSameElements(N.toList(shuffledArray), N.toList(1, 2, 3, 4, 5));

        rnd = new Random(42);
        List<Integer> shuffledList = IntStream.of(1, 2, 3, 4, 5).shuffled(rnd).toList();
        assertEquals(5, shuffledList.size());
        assertHaveSameElements(shuffledList, N.toList(1, 2, 3, 4, 5));

        rnd = new Random(42);
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).skip(1).count());
    }

    @Test
    public void testNegativeSkip() {
        assertThrows(IllegalArgumentException.class, () -> createIntStream(1, 2, 3).skip(-1));
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).skip(2).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).skip(2).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).skip(2).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).skip(2).toList());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toArray());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).skip(1).count());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).toArray());
        assertArrayEquals(new int[] { 5 }, IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).skip(1).toArray());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).toList());
        assertEquals(N.toList(5), IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).skip(1).toList());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).skip(1).count());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).toArray());
        assertArrayEquals(new int[] { 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).skip(1).toArray());
        assertEquals(N.toList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).toList());
        assertEquals(N.toList(5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, IntStream.of(3, 1, 4, 2, 5).sorted().count());
        assertEquals(4, IntStream.of(3, 1, 4, 2, 5).sorted().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).sorted().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).sorted().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(3, 1, 4, 2, 5).sorted().toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(3, 1, 4, 2, 5).sorted().skip(1).toList());
        assertEquals(5, IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().count());
        assertEquals(4, IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromSplitByChunkCount() {
        assertEquals(3, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).count());
        assertEquals(2, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).skip(1).count());
        assertArrayEquals(new int[] { 4, 3, 3 }, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).toArray());
        assertArrayEquals(new int[] { 3, 3 }, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).skip(1).toArray());
        assertEquals(N.toList(4, 3, 3), IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).toList());
        assertEquals(N.toList(3, 3), IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).skip(1).toList());
        assertEquals(3, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).count());
        assertEquals(2, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 4, 3, 3 }, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).toArray());
        assertArrayEquals(new int[] { 3, 3 }, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(4, 3, 3), IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).toList());
        assertEquals(N.toList(3, 3), IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromSplitByChunkCountWithSizeSmallerFirst() {
        assertEquals(5, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).count());
        assertEquals(4, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).skip(1).count());
        assertArrayEquals(new int[] { 1, 1, 1, 2, 2 }, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).toArray());
        assertArrayEquals(new int[] { 1, 1, 2, 2 }, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).skip(1).toArray());
        assertEquals(N.toList(1, 1, 1, 2, 2), IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).toList());
        assertEquals(N.toList(1, 1, 2, 2), IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).skip(1).toList());
        assertEquals(5, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).count());
        assertEquals(4, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 1, 1, 2, 2 }, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).toArray());
        assertArrayEquals(new int[] { 1, 1, 2, 2 }, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 1, 1, 2, 2), IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).toList());
        assertEquals(N.toList(1, 1, 2, 2), IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfArray() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfArrayWithRange() {
        int[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        assertEquals(5, IntStream.of(array, 2, 7).count());
        assertEquals(4, IntStream.of(array, 2, 7).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5, 6, 7 }, IntStream.of(array, 2, 7).toArray());
        assertArrayEquals(new int[] { 4, 5, 6, 7 }, IntStream.of(array, 2, 7).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5, 6, 7), IntStream.of(array, 2, 7).toList());
        assertEquals(N.toList(4, 5, 6, 7), IntStream.of(array, 2, 7).skip(1).toList());
        assertEquals(5, IntStream.of(array, 2, 7).map(e -> e).count());
        assertEquals(4, IntStream.of(array, 2, 7).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5, 6, 7 }, IntStream.of(array, 2, 7).map(e -> e).toArray());
        assertArrayEquals(new int[] { 4, 5, 6, 7 }, IntStream.of(array, 2, 7).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5, 6, 7), IntStream.of(array, 2, 7).map(e -> e).toList());
        assertEquals(N.toList(4, 5, 6, 7), IntStream.of(array, 2, 7).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfCollection() {
        Collection<Integer> collection = Arrays.asList(1, 2, 3, 4, 5);
        assertEquals(5, IntStream.of(collection).count());
        assertEquals(4, IntStream.of(collection).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(collection).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(collection).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(collection).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(collection).skip(1).toList());
        assertEquals(5, IntStream.of(collection).map(e -> e).count());
        assertEquals(4, IntStream.of(collection).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(collection).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(collection).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(collection).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(collection).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfIntBuffer() {
        IntBuffer buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(5, IntStream.of(buffer).count());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(4, IntStream.of(buffer).skip(1).count());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(buffer).toArray());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(buffer).skip(1).toArray());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(buffer).toList());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(buffer).skip(1).toList());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(5, IntStream.of(buffer).map(e -> e).count());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(4, IntStream.of(buffer).map(e -> e).skip(1).count());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(buffer).map(e -> e).toArray());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(buffer).map(e -> e).skip(1).toArray());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(buffer).map(e -> e).toList());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(buffer).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfIntegerArray() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        assertEquals(5, IntStream.of(array).count());
        assertEquals(4, IntStream.of(array).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(array).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(array).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(array).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(array).skip(1).toList());
        assertEquals(5, IntStream.of(array).map(e -> e).count());
        assertEquals(4, IntStream.of(array).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(array).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(array).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(array).map(e -> e).toList());
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfIntegerArrayWithRange() {
        Integer[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        assertEquals(5, IntStream.of(array, 2, 7).count());
        assertEquals(4, IntStream.of(array, 2, 7).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5, 6, 7 }, IntStream.of(array, 2, 7).toArray());
        assertArrayEquals(new int[] { 4, 5, 6, 7 }, IntStream.of(array, 2, 7).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5, 6, 7), IntStream.of(array, 2, 7).toList());
        assertEquals(N.toList(4, 5, 6, 7), IntStream.of(array, 2, 7).skip(1).toList());
        assertEquals(5, IntStream.of(array, 2, 7).map(e -> e).count());
        assertEquals(4, IntStream.of(array, 2, 7).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5, 6, 7 }, IntStream.of(array, 2, 7).map(e -> e).toArray());
        assertArrayEquals(new int[] { 4, 5, 6, 7 }, IntStream.of(array, 2, 7).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5, 6, 7), IntStream.of(array, 2, 7).map(e -> e).toList());
        assertEquals(N.toList(4, 5, 6, 7), IntStream.of(array, 2, 7).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfIntIterator() {
        IntIterator iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(5, IntStream.of(iterator).count());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(4, IntStream.of(iterator).skip(1).count());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(iterator).toArray());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(iterator).skip(1).toArray());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(iterator).toList());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(iterator).skip(1).toList());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(5, IntStream.of(iterator).map(e -> e).count());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(4, IntStream.of(iterator).map(e -> e).skip(1).count());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(iterator).map(e -> e).toArray());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(iterator).map(e -> e).skip(1).toArray());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(N.toList(1, 2, 3, 4, 5), IntStream.of(iterator).map(e -> e).toList());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(N.toList(2, 3, 4, 5), IntStream.of(iterator).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfJavaOptionalInt() {
        java.util.OptionalInt op = java.util.OptionalInt.of(5);
        assertEquals(1, IntStream.of(op).count());
        assertEquals(0, IntStream.of(op).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.of(op).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).skip(1).toArray());
        assertEquals(N.toList(5), IntStream.of(op).toList());
        assertEquals(N.toList(), IntStream.of(op).skip(1).toList());
        assertEquals(1, IntStream.of(op).map(e -> e).count());
        assertEquals(0, IntStream.of(op).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.of(op).map(e -> e).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(5), IntStream.of(op).map(e -> e).toList());
        assertEquals(N.toList(), IntStream.of(op).map(e -> e).skip(1).toList());

        op = java.util.OptionalInt.empty();
        assertEquals(0, IntStream.of(op).count());
        assertEquals(0, IntStream.of(op).skip(1).count());
        assertArrayEquals(new int[] {}, IntStream.of(op).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).skip(1).toArray());
        assertEquals(N.toList(), IntStream.of(op).toList());
        assertEquals(N.toList(), IntStream.of(op).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfOptionalInt() {
        OptionalInt op = OptionalInt.of(5);
        assertEquals(1, IntStream.of(op).count());
        assertEquals(0, IntStream.of(op).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.of(op).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).skip(1).toArray());
        assertEquals(N.toList(5), IntStream.of(op).toList());
        assertEquals(N.toList(), IntStream.of(op).skip(1).toList());
        assertEquals(1, IntStream.of(op).map(e -> e).count());
        assertEquals(0, IntStream.of(op).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.of(op).map(e -> e).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(5), IntStream.of(op).map(e -> e).toList());
        assertEquals(N.toList(), IntStream.of(op).map(e -> e).skip(1).toList());

        op = OptionalInt.empty();
        assertEquals(0, IntStream.of(op).count());
        assertEquals(0, IntStream.of(op).skip(1).count());
        assertArrayEquals(new int[] {}, IntStream.of(op).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).skip(1).toArray());
        assertEquals(N.toList(), IntStream.of(op).toList());
        assertEquals(N.toList(), IntStream.of(op).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSymmetricDifference() {
        Collection<Integer> c = Arrays.asList(3, 4, 5, 6);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 6 }, IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).toArray());
        assertArrayEquals(new int[] { 2, 6 }, IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).skip(1).toArray());
        assertEquals(N.toList(1, 2, 6), IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).toList());
        assertEquals(N.toList(2, 6), IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 6 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).toArray());
        assertArrayEquals(new int[] { 2, 6 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).skip(1).toArray());
        assertEquals(N.toList(1, 2, 6), IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).toList());
        assertEquals(N.toList(2, 6), IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).toArray());
        assertArrayEquals(new int[] { 2, 3 }, IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).toList());
        assertEquals(N.toList(2, 3), IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).toArray());
        assertArrayEquals(new int[] { 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).toList());
        assertEquals(N.toList(2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).skip(1).toList());
    }

    // throwIfEmpty

    @Test
    public void testThrowIfEmpty() {
        stream = createIntStream(1, 2, 3).throwIfEmpty();
        assertEquals(3, stream.count());
    }

    @Test
    public void testThrowIfEmpty_EmptyThrows() {
        assertThrows(NoSuchElementException.class, () -> IntStream.empty().throwIfEmpty().toArray());
    }

    @Test
    public void testThrowIfEmpty_NonEmpty() {
        int[] result = IntStream.of(1, 2, 3).throwIfEmpty().toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testThrowIfEmptyOnEmptyStream() {
        assertThrows(NoSuchElementException.class, () -> IntStream.empty().throwIfEmpty().count());
    }

    @Test
    public void testStreamCreatedAfterToJdkStream() {
        java.util.stream.IntStream jdkStream = IntStream.of(1, 2, 3, 4, 5).toJdkStream();
        assertEquals(5, jdkStream.count());

        jdkStream = IntStream.of(1, 2, 3, 4, 5).toJdkStream();
        assertEquals(4, jdkStream.skip(1).count());

        jdkStream = IntStream.of(1, 2, 3, 4, 5).toJdkStream();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, jdkStream.toArray());

        jdkStream = IntStream.of(1, 2, 3, 4, 5).toJdkStream();
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, jdkStream.skip(1).toArray());

        jdkStream = IntStream.of(1, 2, 3, 4, 5).toJdkStream();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), jdkStream.boxed().collect(java.util.stream.Collectors.toList()));

        jdkStream = IntStream.of(1, 2, 3, 4, 5).toJdkStream();
        assertEquals(Arrays.asList(2, 3, 4, 5), jdkStream.skip(1).boxed().collect(java.util.stream.Collectors.toList()));

        jdkStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).toJdkStream();
        assertEquals(5, jdkStream.count());

        jdkStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).toJdkStream();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), jdkStream.boxed().collect(java.util.stream.Collectors.toList()));
    }

    @Test
    public void testStreamCreatedAfterTop() {
        assertEquals(3, IntStream.of(3, 1, 4, 2, 5).top(3).count());
        assertEquals(2, IntStream.of(3, 1, 4, 2, 5).top(3).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).top(3).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(3, 1, 4, 2, 5).top(3).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), IntStream.of(3, 1, 4, 2, 5).top(3).toList());
        assertEquals(N.toList(4, 5), IntStream.of(3, 1, 4, 2, 5).top(3).skip(1).toList());
        assertEquals(3, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).count());
        assertEquals(2, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).toList());
        assertEquals(N.toList(4, 5), IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTopWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);
        assertEquals(3, IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).count());
        assertEquals(2, IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).skip(1).count());
        assertArrayEquals(new int[] { 3, 1, 2 }, IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).toArray());
        assertArrayEquals(new int[] { 1, 2 }, IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).skip(1).toArray());
        assertEquals(N.toList(3, 1, 2), IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).toList());
        assertEquals(N.toList(1, 2), IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).skip(1).toList());
        assertEquals(3, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).count());
        assertEquals(2, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).skip(1).count());
        assertArrayEquals(new int[] { 3, 1, 2 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).toArray());
        assertArrayEquals(new int[] { 1, 2 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).skip(1).toArray());
        assertEquals(N.toList(3, 1, 2), IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).toList());
        assertEquals(N.toList(1, 2), IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTransformViaJdkStream() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).transformViaJdkStream(s -> s.map(i -> i * 2)).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).transformViaJdkStream(s -> s.map(i -> i * 2)).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).transformViaJdkStream(s -> s.map(i -> i * 2)).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).transformViaJdkStream(s -> s.map(i -> i * 2)).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).transformViaJdkStream(s -> s.map(i -> i * 2)).toList());
        assertEquals(N.toList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).transformViaJdkStream(s -> s.map(i -> i * 2)).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformViaJdkStream(s -> s.map(i -> i * 2)).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformViaJdkStream(s -> s.map(i -> i * 2)).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformViaJdkStream(s -> s.map(i -> i * 2)).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformViaJdkStream(s -> s.map(i -> i * 2)).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformViaJdkStream(s -> s.map(i -> i * 2)).toList());
        assertEquals(N.toList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformViaJdkStream(s -> s.map(i -> i * 2)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTransformViaJdkStreamDeferred() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).transformViaJdkStream(s -> s.map(i -> i * 2), true).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).transformViaJdkStream(s -> s.map(i -> i * 2), true).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).transformViaJdkStream(s -> s.map(i -> i * 2), true).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).transformViaJdkStream(s -> s.map(i -> i * 2), true).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).transformViaJdkStream(s -> s.map(i -> i * 2), true).toList());
        assertEquals(N.toList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).transformViaJdkStream(s -> s.map(i -> i * 2), true).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformViaJdkStream(s -> s.map(i -> i * 2), true).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformViaJdkStream(s -> s.map(i -> i * 2), true).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformViaJdkStream(s -> s.map(i -> i * 2), true).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 },
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformViaJdkStream(s -> s.map(i -> i * 2), true).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformViaJdkStream(s -> s.map(i -> i * 2), true).toList());
        assertEquals(N.toList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformViaJdkStream(s -> s.map(i -> i * 2), true).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromZip3Arrays() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 10, 20, 30 };
        int[] array3 = { 100, 200, 300 };
        assertEquals(3, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).count());
        assertEquals(2, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new int[] { 111, 222, 333 }, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).toArray());
        assertArrayEquals(new int[] { 222, 333 }, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).skip(1).toArray());
        assertEquals(N.toList(111, 222, 333), IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).toList());
        assertEquals(N.toList(222, 333), IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).skip(1).toList());
        assertEquals(3, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).count());
        assertEquals(2, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 111, 222, 333 }, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).toArray());
        assertArrayEquals(new int[] { 222, 333 }, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(111, 222, 333), IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).toList());
        assertEquals(N.toList(222, 333), IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromZipArrays() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 10, 20, 30 };
        assertEquals(3, IntStream.zip(array1, array2, (a, b) -> a + b).count());
        assertEquals(2, IntStream.zip(array1, array2, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 11, 22, 33 }, IntStream.zip(array1, array2, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 22, 33 }, IntStream.zip(array1, array2, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(11, 22, 33), IntStream.zip(array1, array2, (a, b) -> a + b).toList());
        assertEquals(N.toList(22, 33), IntStream.zip(array1, array2, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).count());
        assertEquals(2, IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 11, 22, 33 }, IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).toArray());
        assertArrayEquals(new int[] { 22, 33 }, IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).skip(1).toArray());
        assertEquals(N.toList(11, 22, 33), IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).toList());
        assertEquals(N.toList(22, 33), IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).skip(1).toList());
    }

    // zipWith

    @Test
    public void testStreamCreatedAfterZipWith() {
        IntStream streamB = IntStream.of(10, 20, 30);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, (a, b) -> a + b).count());

        streamB = IntStream.of(10, 20, 30);
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, (a, b) -> a + b).skip(1).count());

        streamB = IntStream.of(10, 20, 30);
        assertArrayEquals(new int[] { 11, 22, 33 }, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, (a, b) -> a + b).toArray());

        streamB = IntStream.of(10, 20, 30);
        assertArrayEquals(new int[] { 22, 33 }, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, (a, b) -> a + b).skip(1).toArray());

        streamB = IntStream.of(10, 20, 30);
        assertEquals(N.toList(11, 22, 33), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, (a, b) -> a + b).toList());

        streamB = IntStream.of(10, 20, 30);
        assertEquals(N.toList(22, 33), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, (a, b) -> a + b).skip(1).toList());

        streamB = IntStream.of(10, 20, 30);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, (a, b) -> a + b).count());

        streamB = IntStream.of(10, 20, 30);
        assertEquals(N.toList(11, 22, 33), IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, (a, b) -> a + b).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWith3Streams() {
        IntStream streamB = IntStream.of(10, 20, 30);
        IntStream streamC = IntStream.of(100, 200, 300);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, (a, b, c) -> a + b + c).count());

        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, (a, b, c) -> a + b + c).skip(1).count());

        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertArrayEquals(new int[] { 111, 222, 333 }, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, (a, b, c) -> a + b + c).toArray());

        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertArrayEquals(new int[] { 222, 333 }, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, (a, b, c) -> a + b + c).skip(1).toArray());

        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertEquals(N.toList(111, 222, 333), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, (a, b, c) -> a + b + c).toList());

        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertEquals(N.toList(222, 333), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, (a, b, c) -> a + b + c).skip(1).toList());

        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, streamC, (a, b, c) -> a + b + c).count());

        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertEquals(N.toList(111, 222, 333), IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, streamC, (a, b, c) -> a + b + c).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWith3StreamsDefaults() {
        IntStream streamB = IntStream.of(10, 20);
        IntStream streamC = IntStream.of(100);
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).count());

        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).skip(1).count());

        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertArrayEquals(new int[] { 111, 522, 553, 554, 555 },
                IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).toArray());

        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertArrayEquals(new int[] { 522, 553, 554, 555 },
                IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).skip(1).toArray());

        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertEquals(N.toList(111, 522, 553, 554, 555), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).toList());

        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertEquals(N.toList(522, 553, 554, 555), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).skip(1).toList());

        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).count());

        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertEquals(N.toList(111, 522, 553, 554, 555),
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithDefaults() {
        IntStream streamB = IntStream.of(10, 20);
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, 0, 100, (a, b) -> a + b).count());

        streamB = IntStream.of(10, 20);
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, 0, 100, (a, b) -> a + b).skip(1).count());

        streamB = IntStream.of(10, 20);
        assertArrayEquals(new int[] { 11, 22, 103, 104, 105 }, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, 0, 100, (a, b) -> a + b).toArray());

        streamB = IntStream.of(10, 20);
        assertArrayEquals(new int[] { 22, 103, 104, 105 }, IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, 0, 100, (a, b) -> a + b).skip(1).toArray());

        streamB = IntStream.of(10, 20);
        assertEquals(N.toList(11, 22, 103, 104, 105), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, 0, 100, (a, b) -> a + b).toList());

        streamB = IntStream.of(10, 20);
        assertEquals(N.toList(22, 103, 104, 105), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, 0, 100, (a, b) -> a + b).skip(1).toList());

        streamB = IntStream.of(10, 20);
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, 0, 100, (a, b) -> a + b).count());

        streamB = IntStream.of(10, 20);
        assertEquals(N.toList(11, 22, 103, 104, 105), IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, 0, 100, (a, b) -> a + b).toList());
    }

    // ---- deep review 2026-11-02 G121 begin ----
    private static final int[] G121_SAMPLES = { 0, -1, 1, Integer.MIN_VALUE, Integer.MAX_VALUE };
    private static final int G121_PAD_A = (int) -1;
    private static final int G121_PAD_B = (int) 9;
    private static final int G121_PAD_C = (int) 7;

    private static int[] g121Of(final int... values) {
        final int[] result = new int[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = (int) values[i];
        }

        return result;
    }

    private static int[] g121RandomArray(final java.util.Random rnd, final int maxLength) {
        final int[] a = new int[rnd.nextInt(maxLength + 1)];

        for (int i = 0; i < a.length; i++) {
            a[i] = rnd.nextInt(4) == 0 ? G121_SAMPLES[rnd.nextInt(G121_SAMPLES.length)] : (int) rnd.nextInt(1000);
        }

        return a;
    }

    private static int[] g121Drain(final com.landawn.abacus.util.IntIterator iterator) {
        final com.landawn.abacus.util.IntList list = new com.landawn.abacus.util.IntList();

        while (iterator.hasNext()) {
            list.add(iterator.nextInt());
        }

        return list.toArray();
    }

    private static int[] g121Suffix(final int[] a, final int from) {
        return from >= a.length ? new int[0] : java.util.Arrays.copyOfRange(a, from, a.length);
    }

    // variant 0: zip(a, b), 1: zip(a, b, c), 2: zip(a, b, padA, padB), 3: zip(a, b, c, padA, padB, padC); every call is recorded
    private static IntStream g121Zip(final int variant, final int[] a, final int[] b, final int[] c, final java.util.List<String> calls) {
        switch (variant) {
            case 0:
                return IntStream.zip(a, b, (x, y) -> {
                    calls.add(x + "," + y);
                    return (int) (x * 3 + y);
                });
            case 1:
                return IntStream.zip(a, b, c, (x, y, z) -> {
                    calls.add(x + "," + y + "," + z);
                    return (int) (x * 3 + y - z);
                });
            case 2:
                return IntStream.zip(a, b, G121_PAD_A, G121_PAD_B, (x, y) -> {
                    calls.add(x + "," + y);
                    return (int) (x * 3 + y);
                });
            default:
                return IntStream.zip(a, b, c, G121_PAD_A, G121_PAD_B, G121_PAD_C, (x, y, z) -> {
                    calls.add(x + "," + y + "," + z);
                    return (int) (x * 3 + y - z);
                });
        }
    }

    private static int[] g121ZipReference(final int variant, final int[] a, final int[] b, final int[] c) {
        final int len = variant == 0 ? Math.min(a.length, b.length)
                : variant == 1 ? Math.min(a.length, Math.min(b.length, c.length))
                        : variant == 2 ? Math.max(a.length, b.length) : Math.max(a.length, Math.max(b.length, c.length));
        final int[] result = new int[len];

        for (int i = 0; i < len; i++) {
            final int x = i < a.length ? a[i] : G121_PAD_A;
            final int y = i < b.length ? b[i] : G121_PAD_B;
            final int z = i < c.length ? c[i] : G121_PAD_C;
            result[i] = variant == 0 || variant == 2 ? (int) (x * 3 + y) : (int) (x * 3 + y - z);
        }

        return result;
    }

    // G121-01: zip(int[]...) toArray()/toIntList()/skip(k).toArray()/iterator().toArray()|toList()/sorted() fill an exactly sized
    // array; values and the zipFunction call sequence equal the element-by-element traversal (pin: GREEN on base and fix)
    @org.junit.jupiter.api.Test
    public void testZipArrays_bulkTerminalsMatchElementwiseTraversal() {
        final java.util.Random rnd = new java.util.Random(121);

        for (int round = 0; round < 200; round++) {
            final int[] a = g121RandomArray(rnd, 12);
            final int[] b = g121RandomArray(rnd, 12);
            final int[] c = g121RandomArray(rnd, 12);

            for (int variant = 0; variant < 4; variant++) {
                final int[] expected = g121ZipReference(variant, a, b, c);
                final java.util.List<String> expectedCalls = new java.util.ArrayList<>();
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Drain(g121Zip(variant, a, b, c, expectedCalls).iterator()));
                org.junit.jupiter.api.Assertions.assertEquals(expected.length, expectedCalls.size());

                final java.util.List<String> calls = new java.util.ArrayList<>();
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Zip(variant, a, b, c, calls).toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

                calls.clear();
                final com.landawn.abacus.util.IntList list = g121Zip(variant, a, b, c, calls).toIntList();
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, list.toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
                list.add((int) 42);
                org.junit.jupiter.api.Assertions.assertEquals(expected.length + 1, list.size());

                final int k = rnd.nextInt(expected.length + 2);
                calls.clear();
                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, k), g121Zip(variant, a, b, c, calls).skip(k).toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

                calls.clear();
                final com.landawn.abacus.util.IntIterator iter = g121Zip(variant, a, b, c, calls).iterator();
                final int consumed = Math.min(k, expected.length);

                for (int i = 0; i < consumed; i++) {
                    iter.nextInt();
                }

                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, consumed),
                        rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
                org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
                org.junit.jupiter.api.Assertions.assertSame(com.landawn.abacus.util.N.EMPTY_INT_ARRAY, iter.toArray());
                org.junit.jupiter.api.Assertions.assertEquals(0, iter.toList().size());

                calls.clear();
                final int[] sorted = expected.clone();
                java.util.Arrays.sort(sorted);
                org.junit.jupiter.api.Assertions.assertArrayEquals(sorted, g121Zip(variant, a, b, c, calls).sorted().toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
            }
        }
    }

    private static boolean g121TakeFirst(final boolean stateful, final int x, final int y, final java.util.List<String> calls) {
        calls.add(x + "," + y);
        return stateful ? calls.size() % 3 != 0 : x <= y;
    }

    private static IntStream g121Merge(final int[] a, final int[] b, final boolean stateful, final java.util.List<String> calls) {
        return IntStream.merge(a, b, (x, y) -> g121TakeFirst(stateful, x, y, calls) ? com.landawn.abacus.util.MergeResult.TAKE_FIRST
                : com.landawn.abacus.util.MergeResult.TAKE_SECOND);
    }

    // G121-01: merge(int[], int[]) toArray()/toIntList() fill an exactly sized array; values and the order and number of
    // nextSelector calls (a stateful selector makes the result depend on them) equal the element-by-element traversal (pin)
    @org.junit.jupiter.api.Test
    public void testMergeArrays_bulkTerminalsMatchElementwiseTraversal() {
        final java.util.Random rnd = new java.util.Random(12103);

        for (int round = 0; round < 200; round++) {
            final int[] a = g121RandomArray(rnd, 10);
            final int[] b = g121RandomArray(rnd, 10);

            if (rnd.nextBoolean()) {
                java.util.Arrays.sort(a);
                java.util.Arrays.sort(b);
            }

            final boolean stateful = rnd.nextBoolean();
            final com.landawn.abacus.util.IntList expectedList = new com.landawn.abacus.util.IntList();
            final java.util.List<String> expectedCalls = new java.util.ArrayList<>();
            int i = 0;
            int j = 0;

            while (i < a.length || j < b.length) {
                if (i < a.length && (j >= b.length || g121TakeFirst(stateful, a[i], b[j], expectedCalls))) {
                    expectedList.add(a[i++]);
                } else {
                    expectedList.add(b[j++]);
                }
            }

            final int[] expected = expectedList.toArray();
            final java.util.List<String> calls = new java.util.ArrayList<>();
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Drain(g121Merge(a, b, stateful, calls).iterator()));
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

            calls.clear();
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Merge(a, b, stateful, calls).toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

            calls.clear();
            final com.landawn.abacus.util.IntList list = g121Merge(a, b, stateful, calls).toIntList();
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, list.toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
            list.add((int) 1);
            org.junit.jupiter.api.Assertions.assertEquals(expected.length + 1, list.size());

            final int k = rnd.nextInt(expected.length + 2);
            calls.clear();
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, k), g121Merge(a, b, stateful, calls).skip(k).toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

            calls.clear();
            final com.landawn.abacus.util.IntIterator iter = g121Merge(a, b, stateful, calls).iterator();
            final int consumed = Math.min(k, expected.length);

            for (int n = 0; n < consumed; n++) {
                iter.nextInt();
            }

            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, consumed), rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
            org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
            org.junit.jupiter.api.Assertions.assertEquals(0, iter.toArray().length);
        }
    }

    private static Object g121Bulk(final com.landawn.abacus.util.IntIterator iterator, final boolean useList) {
        return useList ? iterator.toList() : iterator.toArray();
    }

    // G121-01: a failing zipFunction or nextSelector inside toArray()/toList() leaves the iterator exactly where element-by-element
    // traversal leaves it (zip(a, b)/zip(a, b, c) advance before the call, the padded zips and merge do not) (pin)
    @org.junit.jupiter.api.Test
    public void testZipAndMergeArrays_failingCallbackKeepsIteratorState() {
        final int[] a = g121Of(1, 2, 3, 4, 5);
        final int[] b = g121Of(10, 20, 30, 40, 50, 60);
        final int[] c = g121Of(100, 200, 300, 400, 500, 600, 700);

        for (int viaList = 0; viaList < 2; viaList++) {
            final boolean useList = viaList == 1;
            final int[] calls = { 0 };

            final com.landawn.abacus.util.IntIterator zip2 = IntStream.zip(a, b, (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("zip2");
                }

                return (int) (x + y);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("zip2",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(zip2, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertEquals(3, calls[0]);
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(44, 55), g121Drain(zip2));
            org.junit.jupiter.api.Assertions.assertEquals(5, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.IntIterator zip3 = IntStream.zip(a, b, c, (x, y, z) -> {
                if (++calls[0] == 2) {
                    throw new IllegalStateException("zip3");
                }

                return (int) (x + y + z);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("zip3",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(zip3, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(333, 444, 555), g121Drain(zip3));
            org.junit.jupiter.api.Assertions.assertEquals(5, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.IntIterator pad2 = IntStream.zip(a, b, (int) -1, (int) -2, (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("pad2");
                }

                return (int) (x + y);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("pad2",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(pad2, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(33, 44, 55, 59), g121Drain(pad2));
            org.junit.jupiter.api.Assertions.assertEquals(7, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.IntIterator pad3 = IntStream.zip(a, b, c, (int) -1, (int) -2, (int) -3, (x, y, z) -> {
                if (++calls[0] == 6) {
                    throw new IllegalStateException("pad3");
                }

                return (int) (x + y + z);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("pad3",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(pad3, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(659, 697), g121Drain(pad3));
            org.junit.jupiter.api.Assertions.assertEquals(8, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.IntIterator merge = IntStream.merge(g121Of(1, 3, 5), g121Of(2, 4, 6), (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("merge");
                }

                return x <= y ? com.landawn.abacus.util.MergeResult.TAKE_FIRST : com.landawn.abacus.util.MergeResult.TAKE_SECOND;
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("merge",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(merge, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(3, 4, 5, 6), g121Drain(merge));
            org.junit.jupiter.api.Assertions.assertEquals(6, calls[0]);
        }
    }

    // G121-01: a zipFunction that consumes its own stream's iterator re-entrantly (once) ends toArray()/toIntList() where the
    // hasNext()-driven loop ends, instead of failing with NoSuchElementException (pin)
    @org.junit.jupiter.api.Test
    public void testZipArrays_reentrantCallbackEndsBulkTerminalEarly() {
        final int[] a = g121Of(1, 2, 3, 4, 5);
        final int[] b = g121Of(10, 20, 30, 40, 50);
        final int[] c = g121Of(100, 200, 300, 400, 500);
        final Runnable[] consume = new Runnable[1];
        final boolean[] once = { true };

        IntStream s = IntStream.zip(a, b, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (int) (x + y);
        });
        com.landawn.abacus.util.IntIterator it = s.iterator();
        consume[0] = it::nextInt;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(11, 33, 44, 55), it.toArray());
        org.junit.jupiter.api.Assertions.assertFalse(it.hasNext());

        once[0] = true;
        s = IntStream.zip(a, b, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (int) (x + y);
        });
        it = s.iterator();
        consume[0] = it::nextInt;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(11, 33, 44, 55), s.toIntList().toArray());

        once[0] = true;
        s = IntStream.zip(a, b, c, (x, y, z) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (int) (x + y + z);
        });
        it = s.iterator();
        consume[0] = it::nextInt;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(111, 333, 444, 555), it.toArray());

        once[0] = true;
        s = IntStream.zip(a, g121Of(7), (int) -1, (int) -2, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (int) (x + y);
        });
        it = s.iterator();
        consume[0] = it::nextInt;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(8, 1, 2, 3), it.toArray());

        once[0] = true;
        s = IntStream.zip(a, g121Of(7), new int[0], (int) -1, (int) -2, (int) -3, (x, y, z) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (int) (x + y + z);
        });
        it = s.iterator();
        consume[0] = it::nextInt;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(5, -2, -1, 0), it.toArray());
    }

    private static IntStream g121Flatten(final int[][] a, final int mode, final int pad) {
        return mode == 0 ? IntStream.flatten(a, true) : IntStream.flatten(a, pad, mode == 1);
    }

    // G121-01: flatten(int[][], true) (rectangular walk and the sparse jagged walk) and flatten(int[][], value, vertically)
    // toArray()/toIntList() fill an exactly sized array; pinned against plain column-major / row-major references, also after
    // skip(k) and partial iteration (pin)
    @org.junit.jupiter.api.Test
    public void testFlattenVerticalAndAligned_bulkTerminalsMatchReference() {
        final java.util.Random rnd = new java.util.Random(12102);

        for (int round = 0; round < 300; round++) {
            final int shape = rnd.nextInt(3);
            final int rows = shape == 2 ? 5 + rnd.nextInt(6) : rnd.nextInt(7);
            final int[][] a = new int[rows][];
            final int width = 1 + rnd.nextInt(6);

            for (int r = 0; r < rows; r++) {
                if (shape == 1) {
                    a[r] = new int[width];
                    java.util.Arrays.fill(a[r], (int) (r + 1));
                } else if (shape == 2) {
                    // one long row among null, empty and one-element rows: rows * maxLen > 4 * n selects the jagged walk
                    a[r] = r == rows / 2 ? g121RandomArray(rnd, 40) : rnd.nextInt(3) == 0 ? null : new int[rnd.nextInt(2)];
                } else {
                    a[r] = rnd.nextInt(5) == 0 ? null : g121RandomArray(rnd, 6);
                }
            }

            int maxLen = 0;

            for (final int[] row : a) {
                maxLen = Math.max(maxLen, row == null ? 0 : row.length);
            }

            final int pad = G121_SAMPLES[rnd.nextInt(G121_SAMPLES.length)];
            final com.landawn.abacus.util.IntList vertical = new com.landawn.abacus.util.IntList();
            final com.landawn.abacus.util.IntList alignedVertical = new com.landawn.abacus.util.IntList();
            final com.landawn.abacus.util.IntList alignedHorizontal = new com.landawn.abacus.util.IntList();

            for (int col = 0; col < maxLen; col++) {
                for (final int[] row : a) {
                    if (row != null && col < row.length) {
                        vertical.add(row[col]);
                        alignedVertical.add(row[col]);
                    } else {
                        alignedVertical.add(pad);
                    }
                }
            }

            for (final int[] row : a) {
                for (int col = 0; col < maxLen; col++) {
                    alignedHorizontal.add(row != null && col < row.length ? row[col] : pad);
                }
            }

            for (int mode = 0; mode < 3; mode++) {
                // a single row is returned as-is (no padding) by both flatten overloads
                final int[] expected = rows == 1 ? (a[0] == null ? new int[0] : a[0].clone())
                        : (mode == 0 ? vertical : mode == 1 ? alignedVertical : alignedHorizontal).toArray();

                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Flatten(a, mode, pad).toArray());
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Flatten(a, mode, pad).toIntList().toArray());
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Drain(g121Flatten(a, mode, pad).iterator()));

                final int k = rnd.nextInt(expected.length + 2);
                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, k), g121Flatten(a, mode, pad).skip(k).toArray());

                final com.landawn.abacus.util.IntIterator iter = g121Flatten(a, mode, pad).iterator();
                final int consumed = Math.min(k, expected.length);

                for (int n = 0; n < consumed; n++) {
                    iter.nextInt();
                }

                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, consumed),
                        rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
                org.junit.jupiter.api.Assertions.assertEquals(0, iter.toList().size());
                org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class, iter::nextInt);
            }
        }
    }

    // ---- deep review 2026-11-02 G121 end ----
    // ---- deep review 2026-11-02b G103 begin ----
    // G103-01 (pin): toIntList() of range/rangeClosed (with and without a step) and iterator().toList() take the range
    // iterators' exact-size toList(); the result must equal an arithmetic oracle for every shape, also after partial iteration.
    @Test
    public void testRangeToIntList_matchesOracleForEveryShape_r116() {
        // {kind, start, end, by}: kind 0 = range(s, e), 1 = range(s, e, by), 2 = rangeClosed(s, e), 3 = rangeClosed(s, e, by)
        final int[][] shapes = { { 0, 0, 0, 0 }, { 0, 5, 5, 0 }, { 0, 10, 5, 0 }, { 0, 0, 1, 0 }, { 0, -3, 0, 0 }, { 0, 0, 10, 0 }, { 0, 0, 11, 0 },
                { 0, -500, 500, 0 }, { 0, Integer.MAX_VALUE - 3, Integer.MAX_VALUE, 0 }, { 0, Integer.MIN_VALUE, Integer.MIN_VALUE + 3, 0 }, { 1, 0, 10, 2 },
                { 1, 10, 0, -2 }, { 1, 0, 10, -1 }, { 1, 10, 0, 1 }, { 1, 5, 5, 3 }, { 1, 0, 1, 5 }, { 1, 0, 1000, 7 }, { 1, 1000, -1000, -9 },
                { 1, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE }, { 1, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE }, { 2, 10, 5, 0 },
                { 2, 5, 6, 0 }, { 2, -1, 1, 0 }, { 2, 0, 999, 0 }, { 2, Integer.MAX_VALUE - 2, Integer.MAX_VALUE, 0 },
                { 2, Integer.MIN_VALUE, Integer.MIN_VALUE + 2, 0 }, { 3, 0, 10, 2 }, { 3, 10, 0, -2 }, { 3, 4, 4, -3 }, { 3, 0, 10, -1 }, { 3, 10, 0, 1 },
                { 3, 3, 15, 3 }, { 3, 2997, 0, -3 }, { 3, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE },
                { 3, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE } };

        for (final int[] shape : shapes) {
            final String name = java.util.Arrays.toString(shape);
            final int[] expected = g103bRangeOracle(shape);

            final IntList list = g103bRange(shape).toIntList();
            assertArrayEquals(expected, list.toArray(), name);
            assertEquals(expected.length, list.size(), name);

            // the returned list is a fresh, mutable list
            list.add(42);
            assertEquals(expected.length + 1, list.size(), name);
            assertEquals(42, list.get(expected.length), name);

            // partial iteration, then the bulk terminal continues from the cursor and exhausts the iterator
            for (int consumed = 0; consumed <= Math.min(3, expected.length); consumed++) {
                final IntIterator iter = g103bRange(shape).iterator();

                for (int i = 0; i < consumed; i++) {
                    assertEquals(expected[i], iter.nextInt(), name);
                }

                assertArrayEquals(java.util.Arrays.copyOfRange(expected, consumed, expected.length), iter.toList().toArray(), name);
                assertFalse(iter.hasNext(), name);
                assertEquals(0, iter.toList().size(), name);
                assertEquals(0, iter.toArray().length, name);
                assertEquals(0L, iter.count(), name);
                assertThrows(NoSuchElementException.class, iter::nextInt, name);
            }

            // advance(n) (skip) followed by the bulk terminals
            final int k = Math.min(2, expected.length);
            assertArrayEquals(java.util.Arrays.copyOfRange(expected, k, expected.length), g103bRange(shape).skip(k).toIntList().toArray(), name);
            assertArrayEquals(java.util.Arrays.copyOfRange(expected, k, expected.length), g103bRange(shape).skip(k).toArray(), name);
        }

        // ofIndices(n) / ofIndices(n, step) are range-backed
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.ofIndices(5).toIntList().toArray());
        assertArrayEquals(new int[] { 0, 2, 4, 6, 8 }, IntStream.ofIndices(10, 2).toIntList().toArray());
        assertArrayEquals(new int[] { 4, 3, 2, 1, 0 }, IntStream.ofIndices(5, -1).toIntList().toArray());
        assertEquals(0, IntStream.ofIndices(0).toIntList().size());
    }

    // G103-01 (pin): a range bigger than an int array can hold keeps its documented behaviour on the other terminals
    // (toArray() rejects it with IllegalStateException before reading anything; count/skip stay O(1)).
    @Test
    public void testRangeClosedFullIntRange_bulkTerminalsUnchanged_r116() {
        assertThrows(IllegalStateException.class, () -> IntStream.rangeClosed(Integer.MIN_VALUE, Integer.MAX_VALUE).toArray());
        assertThrows(IllegalStateException.class, () -> IntStream.range(Integer.MIN_VALUE, Integer.MAX_VALUE).toArray());
        assertEquals(1L << 32, IntStream.rangeClosed(Integer.MIN_VALUE, Integer.MAX_VALUE).count());
        assertArrayEquals(new int[] { Integer.MAX_VALUE - 1, Integer.MAX_VALUE },
                IntStream.rangeClosed(Integer.MIN_VALUE, Integer.MAX_VALUE).skip((1L << 32) - 2).toIntList().toArray());
        assertArrayEquals(new int[] { Integer.MAX_VALUE - 3, Integer.MAX_VALUE - 1 },
                IntStream.rangeClosed(Integer.MIN_VALUE, Integer.MAX_VALUE, 2).skip((1L << 31) - 2).toIntList().toArray());
    }

    private static IntStream g103bRange(final int[] shape) {
        switch (shape[0]) {
            case 0:
                return IntStream.range(shape[1], shape[2]);
            case 1:
                return IntStream.range(shape[1], shape[2], shape[3]);
            case 2:
                return IntStream.rangeClosed(shape[1], shape[2]);
            default:
                return IntStream.rangeClosed(shape[1], shape[2], shape[3]);
        }
    }

    private static int[] g103bRangeOracle(final int[] shape) {
        final long start = shape[1];
        final long end = shape[2];
        final long by = shape[0] == 0 || shape[0] == 2 ? 1 : shape[3];
        final boolean closed = shape[0] >= 2;
        final IntList result = new IntList();

        for (long v = start; by > 0 ? (closed ? v <= end : v < end) : (closed ? v >= end : v > end); v += by) {
            result.add((int) v);
        }

        return result.toArray();
    }

    // ---- deep review 2026-11-02b G103 end ----
    // ---- deep review 2026-11-02b G120 begin ----
    // G120-03 (pin): IntStream.of(IntBuffer) for a buffer without an accessible array (direct, read-only, sliced, view of a heap
    // ByteBuffer) reads the window [position, limit) through a dedicated iterator; toArray()/toList() fill an exact-size
    // array with the same get(i) reads. Results, laziness (live view), window snapshot and iterator states must match element-wise traversal.
    @Test
    public void testOfIntBuffer_nonArrayBufferTerminalsMatchElementwise_r116() {
        final int[] content = { 10, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, -1, 5, 5, 3, 9, -7, 2, 1 };

        for (int kind = 0; kind < 5; kind++) {
            for (int from = 0; from <= content.length; from++) {
                for (int to = from; to <= content.length; to++) {
                    final int[] expected = Arrays.copyOfRange(content, from, to);
                    final java.nio.IntBuffer buf = g120IntBuffer(kind, content, from, to);

                    assertFalse(buf.hasArray());
                    assertArrayEquals(expected, IntStream.of(buf).toArray());
                    assertArrayEquals(expected, IntStream.of(buf).toIntList().toArray());
                    assertEquals(expected.length, IntStream.of(buf).count());
                    assertArrayEquals(g120IntSuffix(expected, 3), IntStream.of(buf).skip(3).toArray());
                    assertArrayEquals(g120IntSorted(expected), IntStream.of(buf).sorted().toArray());

                    final com.landawn.abacus.util.IntList list = IntStream.of(buf).toIntList();
                    list.add((int) 42); // a fresh, mutable list
                    assertEquals(expected.length + 1, list.size());

                    for (int consumed = 0; consumed <= expected.length; consumed++) {
                        final IntIterator iter = IntStream.of(buf).iterator();

                        for (int n = 0; n < consumed; n++) {
                            assertEquals(expected[n], iter.nextInt());
                        }

                        final boolean viaArray = (consumed + from + kind) % 2 == 0;
                        final int[] rest = viaArray ? iter.toArray() : iter.toList().toArray();
                        assertArrayEquals(g120IntSuffix(expected, consumed), rest);
                        assertFalse(iter.hasNext());
                        assertEquals(0, iter.toList().size());
                        assertThrows(NoSuchElementException.class, iter::nextInt);

                        if (expected.length > 0) {
                            // an exhausted iterator returns the shared empty array, as the element-wise toArray() did
                            org.junit.jupiter.api.Assertions.assertSame(com.landawn.abacus.util.N.EMPTY_INT_ARRAY, iter.toArray());
                        } else {
                            assertEquals(0, iter.toArray().length);
                        }
                    }

                    final IntIteratorEx iterEx = (IntIteratorEx) IntStream.of(buf).iterator();
                    iterEx.advance(2);
                    assertArrayEquals(g120IntSuffix(expected, 2), iterEx.toArray());
                }
            }
        }

        // live view of the window taken at the call: later writes are seen, later position/limit changes are not
        final java.nio.IntBuffer direct = java.nio.ByteBuffer.allocateDirect(6 * 4).asIntBuffer();
        direct.put(new int[] { 1, 2, 3, 4, 5, 6 });
        direct.position(1);
        direct.limit(5);

        final IntStream stream = IntStream.of(direct);
        direct.put(4, (int) 40);
        direct.clear();
        direct.limit(0);

        assertArrayEquals(new int[] { 2, 3, 4, 40 }, stream.toArray());
        assertEquals(0, direct.position());
        assertEquals(0, direct.limit());
    }

    private static java.nio.IntBuffer g120IntBuffer(final int kind, final int[] content, final int from, final int to) {
        final java.nio.IntBuffer buf;

        if (kind == 0) {
            buf = java.nio.ByteBuffer.allocateDirect(content.length * 4).asIntBuffer().put(content);
        } else if (kind == 1) {
            buf = java.nio.IntBuffer.wrap(content.clone()).asReadOnlyBuffer();
        } else if (kind == 2) {
            final java.nio.IntBuffer outer = java.nio.ByteBuffer.allocateDirect((content.length + 3) * 4).asIntBuffer();
            outer.position(2);
            outer.put(content);
            outer.position(2);
            outer.limit(2 + content.length);
            buf = outer.slice();
        } else if (kind == 3) {
            buf = java.nio.ByteBuffer.allocateDirect(content.length * 4).asIntBuffer().put(content).asReadOnlyBuffer();
        } else {
            buf = java.nio.ByteBuffer.allocate(content.length * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(content);
        }

        buf.limit(to);
        buf.position(from);

        return buf;
    }

    private static int[] g120IntSuffix(final int[] a, final int k) {
        return Arrays.copyOfRange(a, Math.min(k, a.length), a.length);
    }

    private static int[] g120IntSorted(final int[] a) {
        final int[] copy = a.clone();
        Arrays.sort(copy);
        return copy;
    }

    // ---- deep review 2026-11-02b G120 end ----
    // ---- deep review 2026-11-02b G124 begin ----
    // G124: a buffer without an accessible array keeps the close-handler shape of the former
    // IntStream.range(position, limit).map(view::get) stream: exactly one (run-once, no-op) close handler.
    @Test
    public void testOfIntBuffer_nonArrayBufferKeepsRangeCloseHandler_r116() {
        final java.nio.IntBuffer direct = java.nio.ByteBuffer.allocateDirect(4 * 4).asIntBuffer();
        direct.put(new int[] { (int) 1, (int) 2, (int) 3, (int) 4 });
        direct.position(1);
        final java.nio.IntBuffer emptyWindow = direct.duplicate();
        emptyWindow.position(4);

        for (final java.nio.IntBuffer buf : java.util.List.of(direct, emptyWindow)) {
            // closing the JDK view closes the source stream, as for every stream that has close handlers
            final IntStream source = IntStream.of(buf);
            source.toJdkStream().close();
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, source::count);

            // what iterator()'s "Remember to close" warning checks
            final IntStream stream = IntStream.of(buf);
            org.junit.jupiter.api.Assertions.assertFalse(StreamBase.isEmptyCloseHandlers(stream.closeHandlers()));
            org.junit.jupiter.api.Assertions.assertEquals(1, stream.closeHandlers().size());

            // handlers registered later run once, in order; a failing one does not stop the others
            final java.util.List<String> calls = new java.util.ArrayList<>();
            final IntStream withHandlers = IntStream.of(buf).onClose(() -> calls.add("a")).onClose(() -> {
                calls.add("b");
                throw new IllegalStateException("b");
            }).onClose(() -> calls.add("c"));
            org.junit.jupiter.api.Assertions.assertEquals("b",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, withHandlers::close).getMessage());
            withHandlers.close();
            org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("a", "b", "c"), calls);
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, withHandlers::count);
        }

        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { (int) 2, (int) 3, (int) 4 }, IntStream.of(direct).toArray());
    }
    // ---- deep review 2026-11-02b G124 end ----

    // ---- re-review 2026-11-03 R04 begin ----
    // R04 (pin): IntStream.of(IntBuffer) for a buffer without an accessible array counts and skips by moving the cursor of its
    // window iterator instead of reading every skipped element. Pinned for every window, read position and skip length:
    // count(), advance(n) (negative, zero, inside, exactly to and past the end, Long.MAX_VALUE), the elements read after
    // it, the stream-level skip/step/count, the failure-atomic advance flag, and the live view of elements not yet read.
    @Test
    public void testOfIntBuffer_nonArrayBufferCountAndAdvanceMoveCursor_r116() {
        final int[] content = { 10, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, -1, 5, 5, 3, 9, -7, 2, 1 };

        for (int kind = 0; kind < 2; kind++) {
            for (int from = 0; from <= content.length; from++) {
                for (int to = from; to <= content.length; to++) {
                    final int[] expected = java.util.Arrays.copyOfRange(content, from, to);
                    final java.nio.IntBuffer buf = r04Buffer(kind, content, from, to);
                    org.junit.jupiter.api.Assertions.assertFalse(buf.hasArray());

                    org.junit.jupiter.api.Assertions.assertEquals(expected.length, IntStream.of(buf).count());

                    for (int k = 0; k <= expected.length + 1; k++) {
                        final int start = Math.min(k, expected.length);
                        final int[] rest = java.util.Arrays.copyOfRange(expected, start, expected.length);
                        org.junit.jupiter.api.Assertions.assertEquals(rest.length, IntStream.of(buf).skip(k).count());
                        org.junit.jupiter.api.Assertions.assertArrayEquals(rest, IntStream.of(buf).skip(k).toArray());
                    }

                    org.junit.jupiter.api.Assertions.assertArrayEquals(r04Step(expected, 3), IntStream.of(buf).step(3).toArray());

                    for (int consumed = 0; consumed <= expected.length; consumed++) {
                        final int remaining = expected.length - consumed;

                        for (final long n : new long[] { -1, 0, 1, 2, remaining, remaining + 1L, Long.MAX_VALUE }) {
                            final int next = consumed + (int) Math.min(Math.max(n, 0), remaining);
                            final IntIteratorEx iter = (IntIteratorEx) IntStream.of(buf).iterator();

                            if (expected.length > 0) {
                                org.junit.jupiter.api.Assertions.assertTrue(iter.supportsFailureAtomicAdvance());
                            }

                            for (int i = 0; i < consumed; i++) {
                                org.junit.jupiter.api.Assertions.assertEquals(expected[i], iter.nextInt());
                            }

                            iter.advance(n);

                            for (int i = next; i < expected.length; i++) {
                                org.junit.jupiter.api.Assertions.assertTrue(iter.hasNext());
                                org.junit.jupiter.api.Assertions.assertEquals(expected[i], iter.nextInt());
                            }

                            org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
                            org.junit.jupiter.api.Assertions.assertEquals(0, iter.count());
                            org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class, iter::nextInt);

                            final IntIteratorEx counted = (IntIteratorEx) IntStream.of(buf).iterator();

                            for (int i = 0; i < consumed; i++) {
                                counted.nextInt();
                            }

                            counted.advance(n);
                            org.junit.jupiter.api.Assertions.assertEquals(expected.length - next, counted.count());
                            org.junit.jupiter.api.Assertions.assertFalse(counted.hasNext());
                            counted.advance(1);
                            org.junit.jupiter.api.Assertions.assertEquals(0, counted.count());
                        }
                    }
                }
            }
        }

        // A skip moves the cursor only: an element written after the skip is read live, a skipped one is never read.
        final java.nio.IntBuffer live = r04Buffer(1, content, 0, 6);
        final IntIteratorEx liveIter = (IntIteratorEx) IntStream.of(live).iterator();
        liveIter.advance(2);
        live.put(1, 777);
        live.put(3, 888);
        org.junit.jupiter.api.Assertions.assertEquals(content[2], liveIter.nextInt());
        org.junit.jupiter.api.Assertions.assertEquals(888, liveIter.nextInt());
        org.junit.jupiter.api.Assertions.assertEquals(2, liveIter.count());
        org.junit.jupiter.api.Assertions.assertFalse(liveIter.hasNext());
    }

    private static java.nio.IntBuffer r04Buffer(final int kind, final int[] content, final int from, final int to) {
        // kind 0: read-only view of a heap array; kind 1: writable view of direct memory. Neither exposes an array.
        final java.nio.IntBuffer buf = kind == 0 ? java.nio.IntBuffer.wrap(content.clone()).asReadOnlyBuffer()
                : java.nio.ByteBuffer.allocateDirect(content.length * 4).asIntBuffer().put(content).clear();
        buf.limit(to);
        buf.position(from);
        return buf;
    }

    private static int[] r04Step(final int[] a, final int step) {
        final int[] result = new int[(a.length + step - 1) / step];

        for (int i = 0; i < result.length; i++) {
            result[i] = a[i * step];
        }

        return result;
    }
    // ---- re-review 2026-11-03 R04 end ----

    @Test
    public void testFromNullAndIteratorPaths() {
        assertEquals(0, IntStream.from(null).count());

        final IntIteratorEx it = IntStream.from(java.util.stream.IntStream.of(1, 2, 3)).iteratorEx();
        assertEquals(1, it.nextInt());
        it.advance(0);
        it.advance(1);
        assertEquals(3, it.nextInt());
        assertFalse(it.hasNext());

        final IntIteratorEx it2 = IntStream.from(java.util.stream.IntStream.of(1, 2, 3, 4)).iteratorEx();
        it2.advance(2);
        assertEquals(3, it2.nextInt());
        assertEquals(4, it2.nextInt());
    }

    @Test
    public void testOfCodePointsExhausted() {
        final IntIterator it = IntStream.ofCodePoints("a").iterator();
        assertEquals('a', it.nextInt());
        assertThrows(NoSuchElementException.class, it::nextInt);
    }

    @Test
    public void testHugeRangeToArrayRejected() {
        assertThrows(IllegalStateException.class, () -> IntStream.range(Integer.MIN_VALUE, Integer.MAX_VALUE, 2).toArray());
        assertThrows(IllegalStateException.class, () -> IntStream.rangeClosed(Integer.MIN_VALUE, Integer.MAX_VALUE, 2).toArray());
        assertThrows(IllegalStateException.class, () -> IntStream.repeat(7, 3_000_000_000L).toArray());
    }

    @Test
    public void testRangeClosedZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> IntStream.rangeClosed(1, 5, 0));
    }

    @Test
    public void testRepeatAdvanceNonPositive() {
        final IntIteratorEx it = IntStream.repeat(5, 30).iteratorEx();
        it.advance(0);
        assertEquals(30, it.count());
    }

    @Test
    public void testConcatEmptyInputs() {
        assertEquals(0, IntStream.concat(new int[0][]).count());
        assertEquals(0, IntStream.concat(new IntIterator[0]).count());
        assertEquals(0, IntStream.concat(new IntStream[0]).count());
        assertEquals(0, IntStream.concat(new ArrayList<int[]>()).count());
        assertEquals(0, IntStream.concat(Collections.<IntStream> emptyList()).count());
    }

    @Test
    public void testMergeIteratorsExhausted() {
        final IntIterator it = IntStream.merge(IntIterator.of(1), IntIterator.of(2), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .iterator();
        assertEquals(1, it.nextInt());
        assertEquals(2, it.nextInt());
        assertTrue(!it.hasNext());
        assertThrows(NoSuchElementException.class, it::nextInt);
        assertArrayEquals(new int[] { 1, 2, 3 },
                IntStream.merge(IntIterator.of(1, 3), IntIterator.of(2), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
    }
}
