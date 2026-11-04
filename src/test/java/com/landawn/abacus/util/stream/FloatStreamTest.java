package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.IndexedFloat;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.u.OptionalFloat;

/** FloatStream general contracts. */
public class FloatStreamTest extends FloatStreamTestSupport {

    // append

    @Test
    public void testStreamCreatedAfterAppend() {
        assertEquals(8, FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).count());
        assertEquals(7, FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8 }, FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8 }, FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f), FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f, 7f, 8f), FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).toList());
        assertEquals(8, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).count());
        assertEquals(7, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f, 7f, 8f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendOptional() {
        OptionalFloat op = OptionalFloat.of(6);
        assertEquals(6, FloatStream.of(1, 2, 3, 4, 5).append(op).count());
        op = OptionalFloat.of(6);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).append(op).skip(1).count());
        op = OptionalFloat.of(6);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 }, FloatStream.of(1, 2, 3, 4, 5).append(op).toArray(), 0.001f);
        op = OptionalFloat.of(6);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 }, FloatStream.of(1, 2, 3, 4, 5).append(op).skip(1).toArray(), 0.001f);
        op = OptionalFloat.of(6);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.of(1, 2, 3, 4, 5).append(op).toList());
        op = OptionalFloat.of(6);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f), FloatStream.of(1, 2, 3, 4, 5).append(op).skip(1).toList());
        op = OptionalFloat.of(6);
        assertEquals(6, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).count());
        op = OptionalFloat.of(6);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).skip(1).count());
        op = OptionalFloat.of(6);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).toArray(), 0.001f);
        op = OptionalFloat.of(6);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).skip(1).toArray(), 0.001f);
        op = OptionalFloat.of(6);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).toList());
        op = OptionalFloat.of(6);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendStream() {
        FloatStream append = FloatStream.of(4, 5);
        FloatStream stream = FloatStream.of(1, 2, 3);
        assertEquals(5, stream.append(append).count());
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertEquals(4, stream.append(append).skip(1).count());
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, stream.append(append).toArray(), 0.001f);
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, stream.append(append).skip(1).toArray(), 0.001f);
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), stream.append(append).toList());
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertEquals(N.toList(2f, 3f, 4f, 5f), stream.append(append).skip(1).toList());
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertEquals(5, stream.map(e -> e).append(append).count());
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertEquals(4, stream.map(e -> e).append(append).skip(1).count());
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, stream.map(e -> e).append(append).toArray(), 0.001f);
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, stream.map(e -> e).append(append).skip(1).toArray(), 0.001f);
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), stream.map(e -> e).append(append).toList());
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertEquals(N.toList(2f, 3f, 4f, 5f), stream.map(e -> e).append(append).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAsDoubleStream() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().skip(1).count());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().toArray(), 0.001);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0, 5.0 }, FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().skip(1).toArray(), 0.001);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().skip(1).count());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().toArray(), 0.001);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0, 5.0 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().skip(1).toArray(), 0.001);
        assertEquals(N.toList(1.0, 2.0, 3.0, 4.0, 5.0), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().toList());
        assertEquals(N.toList(2.0, 3.0, 4.0, 5.0), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterBoxed() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).boxed().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).boxed().skip(1).count());
        assertArrayEquals(new Float[] { 1f, 2f, 3f, 4f, 5f }, FloatStream.of(1, 2, 3, 4, 5).boxed().toArray());
        assertArrayEquals(new Float[] { 2f, 3f, 4f, 5f }, FloatStream.of(1, 2, 3, 4, 5).boxed().skip(1).toArray());
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).boxed().toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).boxed().skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().skip(1).count());
        assertArrayEquals(new Float[] { 1f, 2f, 3f, 4f, 5f }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().toArray());
        assertArrayEquals(new Float[] { 2f, 3f, 4f, 5f }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().skip(1).toArray());
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().skip(1).toList());
    }

    // collapse

    @Test
    public void testStreamCreatedAfterCollapseToList() {
        assertEquals(3, FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1).count());
        assertEquals(2, FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1).skip(1).count());
        Object[] collapsedArray = FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1).toArray();
        assertEquals(3, collapsedArray.length);
        assertEquals(FloatList.of(1f, 2f), collapsedArray[0]);
        assertEquals(FloatList.of(5f, 6f, 7f), collapsedArray[1]);
        assertEquals(FloatList.of(10f), collapsedArray[2]);
        Object[] collapsedArraySkip = FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1).skip(1).toArray();
        assertEquals(2, collapsedArraySkip.length);
        assertEquals(FloatList.of(5f, 6f, 7f), collapsedArraySkip[0]);
        assertEquals(FloatList.of(10f), collapsedArraySkip[1]);
        List<FloatList> collapsedList = FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1).toList();
        assertEquals(3, collapsedList.size());
        assertEquals(FloatList.of(1f, 2f), collapsedList.get(0));
        assertEquals(FloatList.of(5f, 6f, 7f), collapsedList.get(1));
        assertEquals(FloatList.of(10f), collapsedList.get(2));
        List<FloatList> collapsedListSkip = FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1).skip(1).toList();
        assertEquals(2, collapsedListSkip.size());
        assertEquals(FloatList.of(5f, 6f, 7f), collapsedListSkip.get(0));
        assertEquals(FloatList.of(10f), collapsedListSkip.get(1));
        assertEquals(3, FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1).count());
        assertEquals(2, FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1).skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithMerge() {
        assertEquals(3, FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).count());
        assertEquals(2, FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 3, 18, 10 }, FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).toArray(),
                0.001f);
        assertArrayEquals(new float[] { 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 18f, 10f), FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(18f, 10f), FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).count());
        assertEquals(2, FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 3, 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 18f, 10f),
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(18f, 10f),
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithTriPredicate() {
        assertEquals(3, FloatStream.of(1, 2, 5, 6, 7, 10).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, FloatStream.of(1, 2, 5, 6, 7, 10).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 3, 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 18f, 10f), FloatStream.of(1, 2, 5, 6, 7, 10).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(18f, 10f), FloatStream.of(1, 2, 5, 6, 7, 10).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 3, 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 18f, 10f),
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.toList(18f, 10f),
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterConcatArrays() {
        float[] a1 = { 1, 2 };
        float[] a2 = { 3, 4 };
        float[] a3 = { 5 };
        assertEquals(5, FloatStream.concat(a1, a2, a3).count());
        assertEquals(4, FloatStream.concat(a1, a2, a3).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.concat(a1, a2, a3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.concat(a1, a2, a3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.concat(a1, a2, a3).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.concat(a1, a2, a3).skip(1).toList());
        assertEquals(5, FloatStream.concat(a1, a2, a3).map(e -> e).count());
        assertEquals(4, FloatStream.concat(a1, a2, a3).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.concat(a1, a2, a3).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.concat(a1, a2, a3).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.concat(a1, a2, a3).map(e -> e).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.concat(a1, a2, a3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterConcatStreams() {
        FloatStream s1 = FloatStream.of(1, 2);
        FloatStream s2 = FloatStream.of(3, 4);
        FloatStream s3 = FloatStream.of(5);
        assertEquals(5, FloatStream.concat(s1, s2, s3).count());
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertEquals(4, FloatStream.concat(s1, s2, s3).skip(1).count());
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.concat(s1, s2, s3).toArray(), 0.001f);
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.concat(s1, s2, s3).skip(1).toArray(), 0.001f);
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.concat(s1, s2, s3).toList());
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.concat(s1, s2, s3).skip(1).toList());
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertEquals(5, FloatStream.concat(s1, s2, s3).map(e -> e).count());
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertEquals(4, FloatStream.concat(s1, s2, s3).map(e -> e).skip(1).count());
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.concat(s1, s2, s3).map(e -> e).toArray(), 0.001f);
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.concat(s1, s2, s3).map(e -> e).skip(1).toArray(), 0.001f);
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.concat(s1, s2, s3).map(e -> e).toList());
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.concat(s1, s2, s3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCycled() {
        assertEquals(15, FloatStream.of(1, 2, 3, 4, 5).cycled().limit(15).count());
        assertEquals(14, FloatStream.of(1, 2, 3, 4, 5).cycled().limit(15).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).cycled().limit(15).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).cycled().limit(15).skip(1).toArray(), 0.001f);
        assertEquals(15, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(15).count());
        assertEquals(14, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(15).skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterCycledWithRounds() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).cycled(2).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).cycled(2).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).cycled(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).cycled(2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).cycled(2).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).cycled(2).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).skip(1).count());
    }

    // defaultIfEmpty

    @Test
    public void testDefaultIfEmpty() {
        float[] result = FloatStream.empty().defaultIfEmpty(() -> FloatStream.of(99.0f)).toArray();
        assertArrayEquals(new float[] { 99.0f }, result, DELTA);
    }

    @Test
    public void testDefaultIfEmpty_NonEmpty() {
        float[] result = createFloatStream(1.0f, 2.0f).defaultIfEmpty(() -> FloatStream.of(99.0f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f }, result, DELTA);
    }

    @Test
    public void testStreamCreatedAfterDefaultIfEmpty() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).toArray(),
                0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toArray(),
                0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDefer() {
        float[][] array = { { 1, 2, 3 }, { 4, 5 } };

        assertEquals(5, FloatStream.defer(() -> FloatStream.of(1, 2, 3, 4, 5)).count());
        assertEquals(4, FloatStream.defer(() -> FloatStream.of(1, 2, 3, 4, 5)).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.defer(() -> FloatStream.of(1, 2, 3, 4, 5)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.defer(() -> FloatStream.of(1, 2, 3, 4, 5)).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.defer(() -> FloatStream.of(1, 2, 3, 4, 5)).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.flatten(array).skip(1).toList());
        assertEquals(5, FloatStream.flatten(array).map(e -> e).count());
        assertEquals(4, FloatStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDelay() {
        Duration duration = Duration.ofMillis(0);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).delay(duration).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).delay(duration).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).delay(duration).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDelayJavaTime() {
        java.time.Duration duration = java.time.Duration.ofMillis(0);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).delay(duration).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).delay(duration).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).delay(duration).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDifference() {
        List<Float> otherList = Arrays.asList(3f, 4f, 5f, 6f, 7f);
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).difference(otherList).count());
        assertEquals(1, FloatStream.of(1, 2, 3, 4, 5).difference(otherList).skip(1).count());
        assertArrayEquals(new float[] { 1, 2 }, FloatStream.of(1, 2, 3, 4, 5).difference(otherList).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2 }, FloatStream.of(1, 2, 3, 4, 5).difference(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f), FloatStream.of(1, 2, 3, 4, 5).difference(otherList).toList());
        assertEquals(N.toList(2f), FloatStream.of(1, 2, 3, 4, 5).difference(otherList).skip(1).toList());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).count());
        assertEquals(1, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).skip(1).count());
        assertArrayEquals(new float[] { 1, 2 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).toList());
        assertEquals(N.toList(2f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(5, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().count());
        assertEquals(4, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().count());
        assertEquals(4, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhileWithAction() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmptySupplier() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).toArray(),
                0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toArray(),
                0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toList());

        assertEquals(3, FloatStream.empty().appendIfEmpty(() -> FloatStream.of(6, 7, 8)).count());
        assertEquals(2, FloatStream.empty().appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).count());
        assertArrayEquals(new float[] { 6, 7, 8 }, FloatStream.empty().appendIfEmpty(() -> FloatStream.of(6, 7, 8)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 8 }, FloatStream.empty().appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toArray(), 0.001f);
    }

    @Test
    public void testStreamCreatedAfterEmpty() {
        assertEquals(0, FloatStream.empty().count());
        assertEquals(0, FloatStream.empty().skip(1).count());
        assertArrayEquals(new float[] {}, FloatStream.empty().toArray(), 0.001f);
        assertArrayEquals(new float[] {}, FloatStream.empty().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(), FloatStream.empty().toList());
        assertEquals(N.toList(), FloatStream.empty().skip(1).toList());
        assertEquals(0, FloatStream.empty().map(e -> e).count());
        assertEquals(0, FloatStream.empty().map(e -> e).skip(1).count());
        assertArrayEquals(new float[] {}, FloatStream.empty().map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] {}, FloatStream.empty().map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(), FloatStream.empty().map(e -> e).toList());
        assertEquals(N.toList(), FloatStream.empty().map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilterWithAction() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).count());
        assertArrayEquals(new float[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 }, FloatStream.of(1, 2, 3, 4, 5).flatMap(f -> FloatStream.of(f, f * 10)).toArray(),
                0.001f);
        assertArrayEquals(new float[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f), FloatStream.of(1, 2, 3, 4, 5).flatMap(f -> FloatStream.of(f, f * 10)).toList());
        assertEquals(N.toList(10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f), FloatStream.of(1, 2, 3, 4, 5).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).count());
        assertArrayEquals(new float[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).toList());
        assertEquals(N.toList(10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToDouble() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).count());
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0 },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).toArray(), 0.001);
        assertArrayEquals(new double[] { 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0 },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).toArray(), 0.001);
        assertEquals(N.toList(1.0, 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).toList());
        assertEquals(N.toList(10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).count());
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).toArray(), 0.001);
        assertArrayEquals(new double[] { 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).toArray(), 0.001);
        assertEquals(N.toList(1.0, 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).toList());
        assertEquals(N.toList(10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToInt() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).toArray());
        assertArrayEquals(new int[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).toArray());
        assertEquals(N.toList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).toList());
        assertEquals(N.toList(10, 2, 20, 3, 30, 4, 40, 5, 50),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).toArray());
        assertArrayEquals(new int[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).toArray());
        assertEquals(N.toList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).toList());
        assertEquals(N.toList(10, 2, 20, 3, 30, 4, 40, 5, 50),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToLong() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).count());
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).toArray());
        assertArrayEquals(new long[] { 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).toArray());
        assertEquals(N.toList(1L, 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).toList());
        assertEquals(N.toList(10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).count());
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).toArray());
        assertArrayEquals(new long[] { 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).toArray());
        assertEquals(N.toList(1L, 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).toList());
        assertEquals(N.toList(10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToObj() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).toArray());
        assertEquals(N.toList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).toList());
        assertEquals(N.toList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).toArray());
        assertEquals(N.toList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).toList());
        assertEquals(N.toList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapToObj() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).skip(1).toArray());
        assertEquals(N.toList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).toList());
        assertEquals(N.toList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).skip(1).toArray());
        assertEquals(N.toList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).toList());
        assertEquals(N.toList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).skip(1).toList());
    }

    // flatten

    @Test
    public void testStreamCreatedAfterFlatten2D() {
        float[][] array = { { 1, 2 }, { 3, 4 }, { 5 } };
        assertEquals(5, FloatStream.flatten(array).count());
        assertEquals(4, FloatStream.flatten(array).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.flatten(array).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.flatten(array).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.flatten(array).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.flatten(array).skip(1).toList());
        assertEquals(5, FloatStream.flatten(array).map(e -> e).count());
        assertEquals(4, FloatStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatten2DVertically() {
        float[][] array = { { 1, 2, 3 }, { 4, 5, 6 } };
        assertEquals(6, FloatStream.flatten(array, true).count());
        assertEquals(5, FloatStream.flatten(array, true).skip(1).count());
        assertArrayEquals(new float[] { 1, 4, 2, 5, 3, 6 }, FloatStream.flatten(array, true).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 2, 5, 3, 6 }, FloatStream.flatten(array, true).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 4f, 2f, 5f, 3f, 6f), FloatStream.flatten(array, true).toList());
        assertEquals(N.toList(4f, 2f, 5f, 3f, 6f), FloatStream.flatten(array, true).skip(1).toList());
        assertEquals(6, FloatStream.flatten(array, true).map(e -> e).count());
        assertEquals(5, FloatStream.flatten(array, true).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 4, 2, 5, 3, 6 }, FloatStream.flatten(array, true).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 2, 5, 3, 6 }, FloatStream.flatten(array, true).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 4f, 2f, 5f, 3f, 6f), FloatStream.flatten(array, true).map(e -> e).toList());
        assertEquals(N.toList(4f, 2f, 5f, 3f, 6f), FloatStream.flatten(array, true).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatten3D() {
        float[][][] array = { { { 1, 2 }, { 3 } }, { { 4, 5 } } };
        assertEquals(5, FloatStream.flatten(array).count());
        assertEquals(4, FloatStream.flatten(array).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.flatten(array).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.flatten(array).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.flatten(array).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.flatten(array).skip(1).toList());
        assertEquals(5, FloatStream.flatten(array).map(e -> e).count());
        assertEquals(4, FloatStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterGenerate() {
        float[] value = { 0 };
        assertEquals(5, FloatStream.generate(() -> value[0]++).limit(5).count());
        value[0] = 0;
        assertEquals(4, FloatStream.generate(() -> value[0]++).limit(5).skip(1).count());
        value[0] = 0;
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4 }, FloatStream.generate(() -> value[0]++).limit(5).toArray(), 0.001f);
        value[0] = 0;
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.generate(() -> value[0]++).limit(5).skip(1).toArray(), 0.001f);
        value[0] = 0;
        assertEquals(N.toList(0f, 1f, 2f, 3f, 4f), FloatStream.generate(() -> value[0]++).limit(5).toList());
        value[0] = 0;
        assertEquals(N.toList(1f, 2f, 3f, 4f), FloatStream.generate(() -> value[0]++).limit(5).skip(1).toList());
        value[0] = 0;
        assertEquals(5, FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).count());
        value[0] = 0;
        assertEquals(4, FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).skip(1).count());
        value[0] = 0;
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4 }, FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).toArray(), 0.001f);
        value[0] = 0;
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).skip(1).toArray(), 0.001f);
        value[0] = 0;
        assertEquals(N.toList(0f, 1f, 2f, 3f, 4f), FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).toList());
        value[0] = 0;
        assertEquals(N.toList(1f, 2f, 3f, 4f), FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).skip(1).toList());
    }

    // ifEmpty

    @Test
    @DisplayName("ifEmpty() should execute action for empty stream")
    public void testIfEmpty() {
        List<String> actions = new ArrayList<>();

        FloatStream stream1 = createFloatStream(1.0f);
        stream1.ifEmpty(() -> actions.add("empty1")).toArray();

        FloatStream stream2 = FloatStream.empty();
        stream2.ifEmpty(() -> actions.add("empty2")).toArray();

        assertEquals(Arrays.asList("empty2"), actions);
    }

    @Test
    public void testIfEmpty_Empty() {
        AtomicInteger counter = new AtomicInteger(0);
        FloatStream.empty().ifEmpty(counter::incrementAndGet).toArray();
        assertEquals(1, counter.get());
    }

    @Test
    public void testIfEmpty_NotEmpty() {
        AtomicInteger counter = new AtomicInteger(0);
        createFloatStream(1.0f, 2.0f).ifEmpty(counter::incrementAndGet).toArray();
        assertEquals(0, counter.get());
    }

    @Test
    public void testStreamCreatedAfterIfEmpty() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).ifEmpty(() -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).ifEmpty(() -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).ifEmpty(() -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).ifEmpty(() -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).ifEmpty(() -> {
        }).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).ifEmpty(() -> {
        }).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIndexed() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).indexed().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).indexed().skip(1).count());
        Object[] indexedArray = FloatStream.of(1, 2, 3, 4, 5).indexed().toArray();
        assertEquals(5, indexedArray.length);
        assertEquals(0, ((IndexedFloat) indexedArray[0]).index());
        assertEquals(1f, ((IndexedFloat) indexedArray[0]).value(), 0.001f);
        assertEquals(4, ((IndexedFloat) indexedArray[4]).index());
        assertEquals(5f, ((IndexedFloat) indexedArray[4]).value(), 0.001f);
        Object[] indexedArraySkip = FloatStream.of(1, 2, 3, 4, 5).indexed().skip(1).toArray();
        assertEquals(4, indexedArraySkip.length);
        assertEquals(1, ((IndexedFloat) indexedArraySkip[0]).index());
        assertEquals(2f, ((IndexedFloat) indexedArraySkip[0]).value(), 0.001f);
        List<IndexedFloat> indexedList = FloatStream.of(1, 2, 3, 4, 5).indexed().toList();
        assertEquals(5, indexedList.size());
        assertEquals(0, indexedList.get(0).index());
        assertEquals(1f, indexedList.get(0).value(), 0.001f);
        List<IndexedFloat> indexedListSkip = FloatStream.of(1, 2, 3, 4, 5).indexed().skip(1).toList();
        assertEquals(4, indexedListSkip.size());
        assertEquals(1, indexedListSkip.get(0).index());
        assertEquals(2f, indexedListSkip.get(0).value(), 0.001f);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).indexed().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).indexed().skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterIntersection() {
        List<Float> otherList = Arrays.asList(3f, 4f, 5f, 6f, 7f);
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).skip(1).toList());
    }

    @Test
    public void testIsParallelAndSequential() {
        assertFalse(FloatStream.of(1.0f).isParallel());
        assertTrue(FloatStream.of(1.0f).parallel().isParallel());
        assertFalse(FloatStream.of(1.0f).parallel().sequential().isParallel());
    }

    @Test
    @DisplayName("isParallel() should return false for sequential")
    public void testIsParallelFalse() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertFalse(stream.isParallel());
    }

    // iterate

    @Test
    public void testStreamCreatedAfterIterateUnlimited() {
        assertEquals(10, FloatStream.iterate(1, f -> f * 2).limit(10).count());
        assertEquals(9, FloatStream.iterate(1, f -> f * 2).limit(10).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512 }, FloatStream.iterate(1, f -> f * 2).limit(10).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 4, 8, 16, 32, 64, 128, 256, 512 }, FloatStream.iterate(1, f -> f * 2).limit(10).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f, 512f), FloatStream.iterate(1, f -> f * 2).limit(10).toList());
        assertEquals(N.toList(2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f, 512f), FloatStream.iterate(1, f -> f * 2).limit(10).skip(1).toList());
        assertEquals(10, FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).count());
        assertEquals(9, FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512 }, FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 4, 8, 16, 32, 64, 128, 256, 512 }, FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).skip(1).toArray(),
                0.001f);
        assertEquals(N.toList(1f, 2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f, 512f), FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).toList());
        assertEquals(N.toList(2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f, 512f), FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIterateWithHasNext() {
        int[] counter = { 0 };
        assertEquals(5, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).count());
        counter[0] = 0;
        assertEquals(4, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).skip(1).count());
        counter[0] = 0;
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4 }, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).toArray(), 0.001f);
        counter[0] = 0;
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).skip(1).toArray(), 0.001f);
        counter[0] = 0;
        assertEquals(N.toList(0f, 1f, 2f, 3f, 4f), FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).toList());
        counter[0] = 0;
        assertEquals(N.toList(1f, 2f, 3f, 4f), FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).skip(1).toList());
        counter[0] = 0;
        assertEquals(5, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).count());
        counter[0] = 0;
        assertEquals(4, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).skip(1).count());
        counter[0] = 0;
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4 }, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).toArray(), 0.001f);
        counter[0] = 0;
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).skip(1).toArray(), 0.001f);
        counter[0] = 0;
        assertEquals(N.toList(0f, 1f, 2f, 3f, 4f), FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).toList());
        counter[0] = 0;
        assertEquals(N.toList(1f, 2f, 3f, 4f), FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIterateWithInitHasNextF() {
        int[] counter = { 0 };
        assertEquals(6, FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).count());
        counter[0] = 0;
        assertEquals(5, FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).skip(1).count());
        counter[0] = 0;
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4, 5 }, FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).toArray(), 0.001f);
        counter[0] = 0;
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).skip(1).toArray(), 0.001f);
        counter[0] = 0;
        assertEquals(N.toList(0f, 1f, 2f, 3f, 4f, 5f), FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).toList());
        counter[0] = 0;
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).skip(1).toList());
        counter[0] = 0;
        assertEquals(6, FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).map(e -> e).count());
        counter[0] = 0;
        assertEquals(5, FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).map(e -> e).skip(1).count());
        counter[0] = 0;
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4, 5 }, FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).map(e -> e).toArray(), 0.001f);
        counter[0] = 0;
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).map(e -> e).skip(1).toArray(), 0.001f);
        counter[0] = 0;
        assertEquals(N.toList(0f, 1f, 2f, 3f, 4f, 5f), FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).map(e -> e).toList());
        counter[0] = 0;
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIterateWithInitPredicateF() {
        assertEquals(5, FloatStream.iterate(0, f -> f < 5, f -> f + 1).count());
        assertEquals(4, FloatStream.iterate(0, f -> f < 5, f -> f + 1).skip(1).count());
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4 }, FloatStream.iterate(0, f -> f < 5, f -> f + 1).toArray(), 0.001f);
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.iterate(0, f -> f < 5, f -> f + 1).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(0f, 1f, 2f, 3f, 4f), FloatStream.iterate(0, f -> f < 5, f -> f + 1).toList());
        assertEquals(N.toList(1f, 2f, 3f, 4f), FloatStream.iterate(0, f -> f < 5, f -> f + 1).skip(1).toList());
        assertEquals(5, FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).count());
        assertEquals(4, FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4 }, FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(0f, 1f, 2f, 3f, 4f), FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).toList());
        assertEquals(N.toList(1f, 2f, 3f, 4f), FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).skip(1).toList());
    }

    // iterator

    @Test
    public void testBooleanSupplierIteratorsStayExhausted() {
        final AtomicInteger conditionCalls = new AtomicInteger();
        FloatIterator iter = FloatStream.iterate(() -> conditionCalls.getAndIncrement() > 0, () -> 1.0f).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextFloat);
        assertEquals(1, conditionCalls.get());

        conditionCalls.set(0);
        iter = FloatStream.iterate(1.0f, () -> conditionCalls.getAndIncrement() > 0, value -> value + 1.0f).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextFloat);
        assertEquals(1, conditionCalls.get());
    }

    @Test
    @DisplayName("iterator() should return iterator")
    public void testIterator() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        FloatIterator iter = stream.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1.0f, iter.nextFloat(), 0.001);
    }

    @Test
    public void testIterator_Empty() {
        FloatIterator iter = FloatStream.empty().iterator();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
    }

    @Test
    public void testIterator_HasNextAndNext() {
        FloatIterator iter = createFloatStream(1.0f, 2.0f, 3.0f).iterator();
        assertTrue(iter.hasNext());
        assertEquals(1.0f, iter.nextFloat(), DELTA);
        assertTrue(iter.hasNext());
        assertEquals(2.0f, iter.nextFloat(), DELTA);
        assertTrue(iter.hasNext());
        assertEquals(3.0f, iter.nextFloat(), DELTA);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testNextOnExhaustedIterator() {
        FloatIterator it = FloatStream.of(1.0f).iterator();
        it.nextFloat();
        assertThrows(NoSuchElementException.class, it::nextFloat);
    }

    @Test
    @DisplayName("Very large numbers should be handled correctly")
    public void testLargeNumbers() {
        float large = Float.MAX_VALUE / 2;
        FloatStream stream = createFloatStream(large, large);
        assertEquals(Float.MAX_VALUE, stream.sum(), Float.MAX_VALUE * 0.001);
    }

    @Test
    public void testStreamCreatedAfterLimit() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).limit(3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).limit(3).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).limit(3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).limit(3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).limit(3).toList());
        assertEquals(N.toList(2f, 3f), FloatStream.of(1, 2, 3, 4, 5).limit(3).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toList());
        assertEquals(N.toList(2f, 3f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toList());
    }

    // map

    @Test
    public void testStreamCreatedAfterFlatmap() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatMapArray(f -> new float[] { f, f * 10 }).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMapArray(f -> new float[] { f, f * 10 }).skip(1).count());
        assertArrayEquals(new float[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).flatMapArray(f -> new float[] { f, f * 10 }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).flatMapArray(f -> new float[] { f, f * 10 }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f),
                FloatStream.of(1, 2, 3, 4, 5).flatMapArray(f -> new float[] { f, f * 10 }).toList());
        assertEquals(N.toList(10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f),
                FloatStream.of(1, 2, 3, 4, 5).flatMapArray(f -> new float[] { f, f * 10 }).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArray(f -> new float[] { f, f * 10 }).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArray(f -> new float[] { f, f * 10 }).skip(1).count());
        assertArrayEquals(new float[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArray(f -> new float[] { f, f * 10 }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArray(f -> new float[] { f, f * 10 }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArray(f -> new float[] { f, f * 10 }).toList());
        assertEquals(N.toList(10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArray(f -> new float[] { f, f * 10 }).skip(1).toList());

    }

    @Test
    public void testStreamCreatedAfterFlattmapToObj() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).toArray());
        assertEquals(N.toList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).toList());
        assertEquals(N.toList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).toArray());
        assertEquals(N.toList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).toList());
        assertEquals(N.toList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapArrayToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).count());
        assertArrayEquals(new float[] { 2, 4, 6, 8, 10 }, FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 6, 8, 10 }, FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(2f, 4f, 6f, 8f, 10f), FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).toList());
        assertEquals(N.toList(4f, 6f, 8f, 10f), FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).count());
        assertArrayEquals(new float[] { 2, 4, 6, 8, 10 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 6, 8, 10 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(2f, 4f, 6f, 8f, 10f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).toList());
        assertEquals(N.toList(4f, 6f, 8f, 10f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartial() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).count());
        assertArrayEquals(new float[] { 2, 6, 10 },
                FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 6, 10 },
                FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(2f, 6f, 10f),
                FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).toList());
        assertEquals(N.toList(6f, 10f),
                FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).count());
        assertEquals(2,
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).count());
        assertArrayEquals(new float[] { 2, 6, 10 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 6, 10 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).toArray(),
                0.001f);
        assertEquals(N.toList(2f, 6f, 10f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).toList());
        assertEquals(N.toList(6f, 10f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToDouble() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).skip(1).count());
        assertArrayEquals(new double[] { 1.5, 3.0, 4.5, 6.0, 7.5 }, FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).toArray(), 0.001);
        assertArrayEquals(new double[] { 3.0, 4.5, 6.0, 7.5 }, FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).skip(1).toArray(), 0.001);
        assertEquals(N.toList(1.5, 3.0, 4.5, 6.0, 7.5), FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).toList());
        assertEquals(N.toList(3.0, 4.5, 6.0, 7.5), FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).skip(1).count());
        assertArrayEquals(new double[] { 1.5, 3.0, 4.5, 6.0, 7.5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).toArray(), 0.001);
        assertArrayEquals(new double[] { 3.0, 4.5, 6.0, 7.5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).skip(1).toArray(), 0.001);
        assertEquals(N.toList(1.5, 3.0, 4.5, 6.0, 7.5), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).toList());
        assertEquals(N.toList(3.0, 4.5, 6.0, 7.5), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToInt() {
        assertEquals(5, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).count());
        assertEquals(4, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).toList());
        assertEquals(N.toList(2, 3, 4, 5), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).skip(1).toList());
        assertEquals(5, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).count());
        assertEquals(4, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).toList());
        assertEquals(N.toList(2, 3, 4, 5), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToLong() {
        assertEquals(5, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).count());
        assertEquals(4, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).skip(1).count());
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).toArray());
        assertArrayEquals(new long[] { 2L, 3L, 4L, 5L }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).skip(1).toArray());
        assertEquals(N.toList(1L, 2L, 3L, 4L, 5L), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).toList());
        assertEquals(N.toList(2L, 3L, 4L, 5L), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).skip(1).toList());
        assertEquals(5, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).count());
        assertEquals(4, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).skip(1).count());
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).toArray());
        assertArrayEquals(new long[] { 2L, 3L, 4L, 5L }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).skip(1).toArray());
        assertEquals(N.toList(1L, 2L, 3L, 4L, 5L), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).toList());
        assertEquals(N.toList(2L, 3L, 4L, 5L), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToObj() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F2.0", "F3.0", "F4.0", "F5.0" }, FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).toArray());
        assertArrayEquals(new String[] { "F2.0", "F3.0", "F4.0", "F5.0" }, FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).skip(1).toArray());
        assertEquals(N.toList("F1.0", "F2.0", "F3.0", "F4.0", "F5.0"), FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).toList());
        assertEquals(N.toList("F2.0", "F3.0", "F4.0", "F5.0"), FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F2.0", "F3.0", "F4.0", "F5.0" }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).toArray());
        assertArrayEquals(new String[] { "F2.0", "F3.0", "F4.0", "F5.0" }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).skip(1).toArray());
        assertEquals(N.toList("F1.0", "F2.0", "F3.0", "F4.0", "F5.0"), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).toList());
        assertEquals(N.toList("F2.0", "F3.0", "F4.0", "F5.0"), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).skip(1).toList());
    }

    // merge

    @Test
    public void testStreamCreatedAfterMerge3Arrays() {
        float[] a = { 1, 4, 7 };
        float[] b = { 2, 5, 8 };
        float[] c = { 3, 6, 9 };
        assertEquals(9, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(8, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        assertEquals(9, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        assertEquals(8, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMerge3Streams() {
        FloatStream a = FloatStream.of(1, 4, 7);
        FloatStream b = FloatStream.of(2, 5, 8);
        FloatStream c = FloatStream.of(3, 6, 9);
        assertEquals(9, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertEquals(8, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), 0.001f);
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertEquals(9, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertEquals(8, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray(), 0.001f);
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeArrays() {
        float[] a = { 1, 3, 5 };
        float[] b = { 2, 4, 6 };
        assertEquals(6, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(5, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        assertEquals(6, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        assertEquals(5, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeCollectionStreams() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(9, FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(8, FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(9, FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(8, FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeIterators() {
        FloatIterator a = FloatIterator.of(new float[] { 1, 3, 5 });
        FloatIterator b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(6, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(5, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(6, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(5, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeStreams() {
        FloatStream a = FloatStream.of(1, 3, 5);
        FloatStream b = FloatStream.of(2, 4, 6);
        assertEquals(6, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), 0.001f);
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(6, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(5, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray(), 0.001f);
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeWith() {
        FloatStream a = FloatStream.of(1, 3, 5);
        FloatStream b = FloatStream.of(2, 4, 6);
        assertEquals(6, a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(5, a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 }, a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(),
                0.001f);
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 }, a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(),
                0.001f);
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f), a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(6, a.map(e -> e).mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(5, a.map(e -> e).mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 },
                a.map(e -> e).mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), 0.001f);
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 },
                a.map(e -> e).mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), a.map(e -> e).mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f),
                a.map(e -> e).mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
    }

    @Test
    @DisplayName("negative arguments should throw")
    public void testNegativeArguments() {
        assertThrows(IllegalArgumentException.class, () -> FloatStream.repeat(1.0f, -1));
        assertThrows(IllegalArgumentException.class, () -> createFloatStream(1.0f, 2.0f).limit(-1));
        assertThrows(IllegalArgumentException.class, () -> createFloatStream(1.0f, 2.0f).skip(-1));
    }

    @Test
    @DisplayName("null arguments should throw")
    public void testNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> createFloatStream(1.0f, 2.0f).map(null).count());
        assertThrows(IllegalArgumentException.class, () -> createFloatStream(1.0f, 2.0f).filter(null).count());
    }

    @Test
    public void testStreamCreatedAfterOfNullable() {
        assertEquals(1, FloatStream.ofNullable(5f).count());
        assertEquals(0, FloatStream.ofNullable(5f).skip(1).count());
        assertArrayEquals(new float[] { 5 }, FloatStream.ofNullable(5f).toArray(), 0.001f);
        assertArrayEquals(new float[] {}, FloatStream.ofNullable(5f).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f), FloatStream.ofNullable(5f).toList());
        assertEquals(N.toList(), FloatStream.ofNullable(5f).skip(1).toList());
        assertEquals(1, FloatStream.ofNullable(5f).map(e -> e).count());
        assertEquals(0, FloatStream.ofNullable(5f).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 5 }, FloatStream.ofNullable(5f).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] {}, FloatStream.ofNullable(5f).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f), FloatStream.ofNullable(5f).map(e -> e).toList());
        assertEquals(N.toList(), FloatStream.ofNullable(5f).map(e -> e).skip(1).toList());

        assertEquals(0, FloatStream.ofNullable(null).count());
        assertEquals(0, FloatStream.ofNullable(null).skip(1).count());
        assertArrayEquals(new float[] {}, FloatStream.ofNullable(null).toArray(), 0.001f);
        assertArrayEquals(new float[] {}, FloatStream.ofNullable(null).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(), FloatStream.ofNullable(null).toList());
        assertEquals(N.toList(), FloatStream.ofNullable(null).skip(1).toList());
    }

    // prepend

    @Test
    public void testStreamCreatedAfterPrepend() {
        assertEquals(8, FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).count());
        assertEquals(7, FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).skip(1).count());
        assertArrayEquals(new float[] { 0, -1, -2, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).toArray(), 0.001f);
        assertArrayEquals(new float[] { -1, -2, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(0f, -1f, -2f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).toList());
        assertEquals(N.toList(-1f, -2f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).skip(1).toList());
        assertEquals(8, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).count());
        assertEquals(7, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).skip(1).count());
        assertArrayEquals(new float[] { 0, -1, -2, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).toArray(), 0.001f);
        assertArrayEquals(new float[] { -1, -2, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(0f, -1f, -2f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).toList());
        assertEquals(N.toList(-1f, -2f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependOptional() {
        OptionalFloat op = OptionalFloat.of(1);
        assertEquals(6, FloatStream.of(2, 3, 4, 5, 6).prepend(op).count());
        op = OptionalFloat.of(1);
        assertEquals(5, FloatStream.of(2, 3, 4, 5, 6).prepend(op).skip(1).count());
        op = OptionalFloat.of(1);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 }, FloatStream.of(2, 3, 4, 5, 6).prepend(op).toArray(), 0.001f);
        op = OptionalFloat.of(1);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 }, FloatStream.of(2, 3, 4, 5, 6).prepend(op).skip(1).toArray(), 0.001f);
        op = OptionalFloat.of(1);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.of(2, 3, 4, 5, 6).prepend(op).toList());
        op = OptionalFloat.of(1);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f), FloatStream.of(2, 3, 4, 5, 6).prepend(op).skip(1).toList());
        op = OptionalFloat.of(1);
        assertEquals(6, FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).count());
        op = OptionalFloat.of(1);
        assertEquals(5, FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).skip(1).count());
        op = OptionalFloat.of(1);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 }, FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).toArray(), 0.001f);
        op = OptionalFloat.of(1);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 }, FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).skip(1).toArray(), 0.001f);
        op = OptionalFloat.of(1);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).toList());
        op = OptionalFloat.of(1);
        assertEquals(N.toList(2f, 3f, 4f, 5f, 6f), FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependStream() {
        FloatStream prepend = FloatStream.of(1, 2);
        FloatStream stream = FloatStream.of(3, 4, 5);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, stream.prepend(prepend).toArray(), 0.001f);
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, stream.prepend(prepend).skip(1).toArray(), 0.001f);
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), stream.prepend(prepend).toList());
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertEquals(N.toList(2f, 3f, 4f, 5f), stream.prepend(prepend).skip(1).toList());
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertEquals(5, stream.map(e -> e).prepend(prepend).count());
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertEquals(4, stream.map(e -> e).prepend(prepend).skip(1).count());
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, stream.map(e -> e).prepend(prepend).toArray(), 0.001f);
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, stream.map(e -> e).prepend(prepend).skip(1).toArray(), 0.001f);
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), stream.map(e -> e).prepend(prepend).toList());
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertEquals(N.toList(2f, 3f, 4f, 5f), stream.map(e -> e).prepend(prepend).skip(1).toList());
    }

    @Test
    public void testPrintln() {
        // Just verify println doesn't throw
        assertDoesNotThrow(() -> createFloatStream(1.0f, 2.0f, 3.0f).println());
    }

    @Test
    public void testPrintln_Empty() {
        assertDoesNotThrow(() -> FloatStream.empty().println());
    }

    @Test
    public void testPspMethod() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).psp(s -> s.filter(f -> f > 2.0f)).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testStreamCreatedAfterPsp() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).parallel().psp(s -> s.sorted()).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).parallel().psp(s -> s.sorted()).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).parallel().psp(s -> s.sorted()).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).parallel().psp(s -> s.sorted()).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).parallel().psp(s -> s.sorted()).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).parallel().psp(s -> s.sorted()).skip(1).toList());
        assertEquals(5, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).count());
        assertEquals(4, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRandom() {
        assertEquals(10, FloatStream.random().limit(10).count());
        assertEquals(9, FloatStream.random().limit(10).skip(1).count());
        float[] randomArray = FloatStream.random().limit(10).toArray();
        assertEquals(10, randomArray.length);
        for (float f : randomArray) {
            assertTrue(f >= 0.0f && f < 1.0f);
        }
        float[] randomArraySkip = FloatStream.random().limit(10).skip(1).toArray();
        assertEquals(9, randomArraySkip.length);
        List<Float> randomList = FloatStream.random().limit(10).toList();
        assertEquals(10, randomList.size());
        List<Float> randomListSkip = FloatStream.random().limit(10).skip(1).toList();
        assertEquals(9, randomListSkip.size());

        assertEquals(10, FloatStream.random().map(e -> e).limit(10).count());
        assertEquals(9, FloatStream.random().map(e -> e).limit(10).skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterRangeMap() {
        assertEquals(3, FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).count());
        assertEquals(2, FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).count());
        assertArrayEquals(new float[] { 2, 6, 10 },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).toArray(), 0.001f);
        assertArrayEquals(new float[] { 6, 10 },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(2f, 6f, 10f),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).toList());
        assertEquals(N.toList(6f, 10f),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).count());
        assertEquals(2,
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).count());
        assertArrayEquals(new float[] { 2, 6, 10 },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).toArray(), 0.001f);
        assertArrayEquals(new float[] { 6, 10 },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).toArray(),
                0.001f);
        assertEquals(N.toList(2f, 6f, 10f),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).toList());
        assertEquals(N.toList(6f, 10f),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRangeMapToObj() {
        assertEquals(3, FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last).count());
        assertEquals(2,
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last).skip(1).count());
        assertArrayEquals(new String[] { "1.0-2.0", "5.0-6.0", "10.0-10.0" },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last).toArray());
        assertArrayEquals(new String[] { "5.0-6.0", "10.0-10.0" },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .toArray());
        assertEquals(N.toList("1.0-2.0", "5.0-6.0", "10.0-10.0"),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last).toList());
        assertEquals(N.toList("5.0-6.0", "10.0-10.0"),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .toList());
        assertEquals(3,
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .map(e -> e)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .count());
        assertEquals(2,
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .map(e -> e)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "1.0-2.0", "5.0-6.0", "10.0-10.0" },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .map(e -> e)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .toArray());
        assertArrayEquals(new String[] { "5.0-6.0", "10.0-10.0" },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .map(e -> e)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .toArray());
        assertEquals(N.toList("1.0-2.0", "5.0-6.0", "10.0-10.0"),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .map(e -> e)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .toList());
        assertEquals(N.toList("5.0-6.0", "10.0-10.0"),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .map(e -> e)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterRateLimited() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRateLimitedWithRateLimiter() {
        RateLimiter rateLimiter = RateLimiter.create(1000.0);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).rateLimited(rateLimiter).count());
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).rateLimited(rateLimiter).skip(1).count());
        rateLimiter = RateLimiter.create(1000.0);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).rateLimited(rateLimiter).toArray(), 0.001f);
        rateLimiter = RateLimiter.create(1000.0);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).rateLimited(rateLimiter).skip(1).toArray(), 0.001f);
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).rateLimited(rateLimiter).toList());
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).rateLimited(rateLimiter).skip(1).toList());
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).count());
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).skip(1).count());
        rateLimiter = RateLimiter.create(1000.0);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).toArray(), 0.001f);
        rateLimiter = RateLimiter.create(1000.0);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).skip(1).toArray(), 0.001f);
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).toList());
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRepeat() {
        assertEquals(5, FloatStream.repeat(3.14f, 5).count());
        assertEquals(4, FloatStream.repeat(3.14f, 5).skip(1).count());
        assertArrayEquals(new float[] { 3.14f, 3.14f, 3.14f, 3.14f, 3.14f }, FloatStream.repeat(3.14f, 5).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3.14f, 3.14f, 3.14f, 3.14f }, FloatStream.repeat(3.14f, 5).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3.14f, 3.14f, 3.14f, 3.14f, 3.14f), FloatStream.repeat(3.14f, 5).toList());
        assertEquals(N.toList(3.14f, 3.14f, 3.14f, 3.14f), FloatStream.repeat(3.14f, 5).skip(1).toList());
        assertEquals(5, FloatStream.repeat(3.14f, 5).map(e -> e).count());
        assertEquals(4, FloatStream.repeat(3.14f, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 3.14f, 3.14f, 3.14f, 3.14f, 3.14f }, FloatStream.repeat(3.14f, 5).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3.14f, 3.14f, 3.14f, 3.14f }, FloatStream.repeat(3.14f, 5).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3.14f, 3.14f, 3.14f, 3.14f, 3.14f), FloatStream.repeat(3.14f, 5).map(e -> e).toList());
        assertEquals(N.toList(3.14f, 3.14f, 3.14f, 3.14f), FloatStream.repeat(3.14f, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReversed() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).reversed().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).reversed().skip(1).count());
        assertArrayEquals(new float[] { 5, 4, 3, 2, 1 }, FloatStream.of(1, 2, 3, 4, 5).reversed().toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 3, 2, 1 }, FloatStream.of(1, 2, 3, 4, 5).reversed().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f, 4f, 3f, 2f, 1f), FloatStream.of(1, 2, 3, 4, 5).reversed().toList());
        assertEquals(N.toList(4f, 3f, 2f, 1f), FloatStream.of(1, 2, 3, 4, 5).reversed().skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).count());
        assertArrayEquals(new float[] { 5, 4, 3, 2, 1 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 3, 2, 1 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f, 4f, 3f, 2f, 1f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toList());
        assertEquals(N.toList(4f, 3f, 2f, 1f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, FloatStream.of(5, 3, 1, 4, 2).reverseSorted().count());
        assertEquals(4, FloatStream.of(5, 3, 1, 4, 2).reverseSorted().skip(1).count());
        assertArrayEquals(new float[] { 5, 4, 3, 2, 1 }, FloatStream.of(5, 3, 1, 4, 2).reverseSorted().toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 3, 2, 1 }, FloatStream.of(5, 3, 1, 4, 2).reverseSorted().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f, 4f, 3f, 2f, 1f), FloatStream.of(5, 3, 1, 4, 2).reverseSorted().toList());
        assertEquals(N.toList(4f, 3f, 2f, 1f), FloatStream.of(5, 3, 1, 4, 2).reverseSorted().skip(1).toList());
        assertEquals(5, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().count());
        assertEquals(4, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new float[] { 5, 4, 3, 2, 1 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 3, 2, 1 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f, 4f, 3f, 2f, 1f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().toList());
        assertEquals(N.toList(4f, 3f, 2f, 1f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRotated() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).rotated(2).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).count());
        assertArrayEquals(new float[] { 4, 5, 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).rotated(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 5, 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(4f, 5f, 1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).rotated(2).toList());
        assertEquals(N.toList(5f, 1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).count());
        assertArrayEquals(new float[] { 4, 5, 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 5, 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(4f, 5f, 1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toList());
        assertEquals(N.toList(5f, 1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toList());
    }

    // scan

    @Test
    public void testStreamCreatedAfterScan() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 1, 3, 6, 10, 15 }, FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 6, 10, 15 }, FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 3f, 6f, 10f, 15f), FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toList());
        assertEquals(N.toList(3f, 6f, 10f, 15f), FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 1, 3, 6, 10, 15 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 6, 10, 15 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 3f, 6f, 10f, 15f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toList());
        assertEquals(N.toList(3f, 6f, 10f, 15f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInit() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toList());
        assertEquals(N.toList(13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toList());
        assertEquals(N.toList(13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInitIncluded() {
        assertEquals(6, FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 10, 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(10f, 11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(N.toList(11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toList());
        assertEquals(6, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 10, 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toArray(),
                0.001f);
        assertEquals(N.toList(10f, 11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(N.toList(11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterShuffled() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).shuffled().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).shuffled().skip(1).count());
        float[] shuffled = FloatStream.of(1, 2, 3, 4, 5).shuffled().toArray();
        assertEquals(5, shuffled.length);
        Arrays.sort(shuffled);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, shuffled, 0.001f);

        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterShuffledWithRandom() {
        Random rnd = new Random(42);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).shuffled(rnd).skip(1).count());
        rnd = new Random(42);
        float[] shuffled = FloatStream.of(1, 2, 3, 4, 5).shuffled(rnd).toArray();
        assertEquals(5, shuffled.length);

        rnd = new Random(42);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).skip(1).count());
    }

    @Test
    @DisplayName("single element stream")
    public void testSingleElement() {
        FloatStream single = createFloatStream(5.0f);
        assertEquals(1, single.count());

        single = createFloatStream(5.0f);
        assertEquals(5.0f, single.first().get(), 0.001);
    }

    @Test
    @DisplayName("Single element stream should work correctly")
    public void testSingleElementStream() {
        FloatStream single = createFloatStream(5.0f);

        assertEquals(1, single.count());

        single = createFloatStream(5.0f);
        assertEquals(5.0f, single.first().get(), 0.001);

        single = createFloatStream(5.0f);
        assertEquals(5.0f, single.min().get(), 0.001);

        single = createFloatStream(5.0f);
        assertEquals(5.0f, single.max().get(), 0.001);
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).skip(2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).skip(2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).skip(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skip(2).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipWithAction() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).skip(2, e -> {
        }).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).skip(2, e -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).skip(2, e -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).skip(2, e -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skip(2, e -> {
        }).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skip(2, e -> {
        }).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, FloatStream.of(5, 3, 1, 4, 2).sorted().count());
        assertEquals(4, FloatStream.of(5, 3, 1, 4, 2).sorted().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).sorted().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).sorted().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).sorted().toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).sorted().skip(1).toList());
        assertEquals(5, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().count());
        assertEquals(4, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    @DisplayName("special float values")
    public void testSpecialValues() {
        FloatStream stream = createFloatStream(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f);
        float[] result = stream.toArray();
        assertEquals(4, result.length);
        assertTrue(Float.isNaN(result[0]));
        assertTrue(Float.isInfinite(result[1]));
        assertTrue(Float.isInfinite(result[2]));
    }

    @Test
    public void testSpecialValues_Infinity() {
        float[] result = createFloatStream(Float.POSITIVE_INFINITY, 1.0f, Float.NEGATIVE_INFINITY).filter(f -> !Float.isInfinite(f)).toArray();
        assertArrayEquals(new float[] { 1.0f }, result, DELTA);
    }

    @Test
    public void testSpecialValues_NaN() {
        float[] result = createFloatStream(Float.NaN, 1.0f, Float.NaN).filter(f -> !Float.isNaN(f)).toArray();
        assertArrayEquals(new float[] { 1.0f }, result, DELTA);
    }

    @Test
    public void testInvalidStep() {
        assertThrows(IllegalArgumentException.class, () -> FloatStream.of(1f, 2f).step(0));
        assertThrows(IllegalArgumentException.class, () -> FloatStream.of(1f, 2f).step(-1));
    }

    @Test
    public void testStreamCreatedAfterStep() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5, 6).step(2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).count());
        assertArrayEquals(new float[] { 1, 3, 5 }, FloatStream.of(1, 2, 3, 4, 5, 6).step(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 5 }, FloatStream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 3f, 5f), FloatStream.of(1, 2, 3, 4, 5, 6).step(2).toList());
        assertEquals(N.toList(3f, 5f), FloatStream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).count());
        assertArrayEquals(new float[] { 1, 3, 5 }, FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 5 }, FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 3f, 5f), FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).toList());
        assertEquals(N.toList(3f, 5f), FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfCollection() {
        List<Float> list = Arrays.asList(1f, 2f, 3f, 4f, 5f);
        assertEquals(5, FloatStream.of(list).count());
        assertEquals(4, FloatStream.of(list).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(list).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(list).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(list).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(list).skip(1).toList());
        assertEquals(5, FloatStream.of(list).map(e -> e).count());
        assertEquals(4, FloatStream.of(list).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(list).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(list).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(list).map(e -> e).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(list).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfFloatArray() {
        assertEquals(5, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).count());
        assertEquals(4, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).skip(1).toList());
        assertEquals(5, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).count());
        assertEquals(4, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfFloatArrayWithStartEndIndex() {
        assertEquals(3, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).count());
        assertEquals(2, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).skip(1).count());
        assertArrayEquals(new float[] { 2, 3, 4 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 4 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(2f, 3f, 4f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).toList());
        assertEquals(N.toList(3f, 4f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).skip(1).toList());
        assertEquals(3, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).count());
        assertEquals(2, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 2, 3, 4 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 4 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(2f, 3f, 4f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).toList());
        assertEquals(N.toList(3f, 4f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfFloatBuffer() {
        FloatBuffer buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(5, FloatStream.of(buffer).count());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(4, FloatStream.of(buffer).skip(1).count());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(buffer).toArray(), 0.001f);
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(buffer).skip(1).toArray(), 0.001f);
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(buffer).toList());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(buffer).skip(1).toList());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(5, FloatStream.of(buffer).map(e -> e).count());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(4, FloatStream.of(buffer).map(e -> e).skip(1).count());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(buffer).map(e -> e).toArray(), 0.001f);
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(buffer).map(e -> e).skip(1).toArray(), 0.001f);
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(buffer).map(e -> e).toList());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(buffer).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfFloatIterator() {
        FloatIterator iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(5, FloatStream.of(iter).count());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(4, FloatStream.of(iter).skip(1).count());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(iter).toArray(), 0.001f);
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(iter).skip(1).toArray(), 0.001f);
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(iter).toList());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(iter).skip(1).toList());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(5, FloatStream.of(iter).map(e -> e).count());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(4, FloatStream.of(iter).map(e -> e).skip(1).count());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(iter).map(e -> e).toArray(), 0.001f);
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(iter).map(e -> e).skip(1).toArray(), 0.001f);
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(iter).map(e -> e).toList());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(iter).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfWithStartEndIndex() {
        assertEquals(3, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).count());
        assertEquals(2, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).skip(1).count());
        assertArrayEquals(new float[] { 2, 3, 4 }, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 4 }, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(2f, 3f, 4f), FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).toList());
        assertEquals(N.toList(3f, 4f), FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).skip(1).toList());
        assertEquals(3, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).count());
        assertEquals(2, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 2, 3, 4 }, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 4 }, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(2f, 3f, 4f), FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).toList());
        assertEquals(N.toList(3f, 4f), FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnEach() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSymmetricDifference() {
        List<Float> otherList = Arrays.asList(3f, 4f, 5f, 6f, 7f);
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).symmetricDifference(otherList).count());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).symmetricDifference(otherList).skip(1).count());
        assertArrayEquals(new float[] { 2, 6, 7 }, FloatStream.of(1, 2, 3, 4, 5).symmetricDifference(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 6f, 7f), FloatStream.of(1, 2, 3, 4, 5).symmetricDifference(otherList).toList());
        assertEquals(N.toList(2f, 6f, 7f), FloatStream.of(1, 2, 3, 4, 5).symmetricDifference(otherList).skip(1).toList());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).count());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 6, 7 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 6, 7 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 6f, 7f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).toList());
        assertEquals(N.toList(2f, 6f, 7f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).count());
        assertEquals(1, FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new float[] { 1, 2 }, FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2 }, FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f), FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList(2f), FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toList());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).count());
        assertEquals(1, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new float[] { 1, 2 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList(2f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toList());
    }

    // throwIfEmpty

    @Test
    public void testStreamCreatedAfterThrowIfEmpty() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterThrowIfEmptyWithSupplier() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).toArray(),
                0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).toArray(),
                0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).toList());
        assertEquals(N.toList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).toList());
    }

    @Test
    @DisplayName("throwIfEmpty() should throw for empty stream")
    public void testThrowIfEmpty() {
        FloatStream stream1 = createFloatStream(1.0f);
        assertDoesNotThrow(() -> stream1.throwIfEmpty().toArray());

        FloatStream stream2 = FloatStream.empty();
        assertThrows(RuntimeException.class, () -> stream2.throwIfEmpty().toArray());
    }

    @Test
    public void testThrowIfEmpty_NotEmpty() {
        assertDoesNotThrow(() -> createFloatStream(1.0f).throwIfEmpty(() -> new RuntimeException("empty")).toArray());
    }

    @Test
    public void testThrowIfEmpty_WithSupplier() {
        assertThrows(IllegalArgumentException.class, () -> FloatStream.empty().throwIfEmpty(() -> new IllegalArgumentException("empty")).toArray());
    }

    @Test
    public void testStreamCreatedAfterTop() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).top(3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).top(3).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).top(3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).top(3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).top(3).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).top(3).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).toList());
        assertEquals(N.toList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).skip(1).toList());
    }

    // zip

    @Test
    public void testStreamCreatedAfterZip3Arrays() {
        float[] a = { 1, 2, 3 };
        float[] b = { 4, 5, 6 };
        float[] c = { 7, 8, 9 };
        assertEquals(3, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).count());
        assertEquals(2, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).count());
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray(), 0.001f);
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toList());
        assertEquals(N.toList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).toList());
        assertEquals(3, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).count());
        assertEquals(2, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).toList());
        assertEquals(N.toList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZip3ArraysWithDefaults() {
        float[] a = { 1, 2 };
        float[] b = { 4, 5, 6 };
        float[] c = { 7 };
        assertEquals(3, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).count());
        assertEquals(2, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).count());
        assertArrayEquals(new float[] { 12, 7, 6 }, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 6 }, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(12f, 7f, 6f), FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).toList());
        assertEquals(N.toList(7f, 6f), FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).toList());
        assertEquals(3, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).count());
        assertEquals(2, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 12, 7, 6 }, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 6 }, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(12f, 7f, 6f), FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).toList());
        assertEquals(N.toList(7f, 6f), FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZip3Iterators() {
        FloatIterator a = FloatIterator.of(new float[] { 1, 2, 3 });
        FloatIterator b = FloatIterator.of(new float[] { 4, 5, 6 });
        FloatIterator c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(3, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).count());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(2, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).count());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(N.toList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toList());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(N.toList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).toList());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(3, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).count());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(2, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).count());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(N.toList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).toList());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(N.toList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZip3Streams() {
        FloatStream a = FloatStream.of(1, 2, 3);
        FloatStream b = FloatStream.of(4, 5, 6);
        FloatStream c = FloatStream.of(7, 8, 9);
        assertEquals(3, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(2, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.toList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.toList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(3, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(2, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.toList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.toList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipArrays() {
        float[] a = { 1, 2, 3 };
        float[] b = { 4, 5, 6 };
        assertEquals(3, FloatStream.zip(a, b, (x, y) -> x + y).count());
        assertEquals(2, FloatStream.zip(a, b, (x, y) -> x + y).skip(1).count());
        assertArrayEquals(new float[] { 5, 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).toList());
        assertEquals(N.toList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).skip(1).toList());
        assertEquals(3, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).count());
        assertEquals(2, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 5, 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).toList());
        assertEquals(N.toList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipArraysWithDefaults() {
        float[] a = { 1, 2 };
        float[] b = { 4, 5, 6 };
        assertEquals(3, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).count());
        assertEquals(2, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).skip(1).count());
        assertArrayEquals(new float[] { 5, 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f, 7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).toList());
        assertEquals(N.toList(7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).skip(1).toList());
        assertEquals(3, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).count());
        assertEquals(2, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 5, 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.toList(5f, 7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).toList());
        assertEquals(N.toList(7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipCollectionStreams() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(3, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).count());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(2, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).skip(1).count());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).skip(1).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(N.toList(12f, 15f, 18f), FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).toList());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(N.toList(15f, 18f), FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).skip(1).toList());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(3, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).count());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(2, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).skip(1).count());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).skip(1).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(N.toList(12f, 15f, 18f), FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).toList());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(N.toList(15f, 18f), FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipIterators() {
        FloatIterator a = FloatIterator.of(new float[] { 1, 2, 3 });
        FloatIterator b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(3, FloatStream.zip(a, b, (x, y) -> x + y).count());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(2, FloatStream.zip(a, b, (x, y) -> x + y).skip(1).count());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertArrayEquals(new float[] { 5, 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertArrayEquals(new float[] { 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.toList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).toList());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.toList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).skip(1).toList());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(3, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).count());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(2, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).count());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertArrayEquals(new float[] { 5, 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertArrayEquals(new float[] { 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.toList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).toList());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.toList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipIteratorsWithDefaults() {
        FloatIterator a = FloatIterator.of(new float[] { 1, 2 });
        FloatIterator b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(3, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).count());
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(2, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).skip(1).count());
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertArrayEquals(new float[] { 5, 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertArrayEquals(new float[] { 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.toList(5f, 7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).toList());
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.toList(7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).skip(1).toList());
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(3, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).count());
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(2, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).skip(1).count());
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertArrayEquals(new float[] { 5, 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertArrayEquals(new float[] { 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).skip(1).toArray(), 0.001f);
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.toList(5f, 7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).toList());
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.toList(7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipStreams() {
        FloatStream a = FloatStream.of(1, 2, 3);
        FloatStream b = FloatStream.of(4, 5, 6);
        assertEquals(3, FloatStream.zip(a, b, (x, y) -> x + y).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(2, FloatStream.zip(a, b, (x, y) -> x + y).skip(1).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 5, 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).skip(1).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(3, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(2, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 5, 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).toList());
    }

    // zipWith

    @Test
    public void testStreamCreatedAfterZipWith() {
        FloatStream a = FloatStream.of(1, 2, 3);
        FloatStream b = FloatStream.of(4, 5, 6);
        assertEquals(3, a.zipWith(b, (x, y) -> x + y).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(2, a.zipWith(b, (x, y) -> x + y).skip(1).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 5, 7, 9 }, a.zipWith(b, (x, y) -> x + y).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 7, 9 }, a.zipWith(b, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(5f, 7f, 9f), a.zipWith(b, (x, y) -> x + y).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(7f, 9f), a.zipWith(b, (x, y) -> x + y).skip(1).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(3, a.map(e -> e).zipWith(b, (x, y) -> x + y).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(2, a.map(e -> e).zipWith(b, (x, y) -> x + y).skip(1).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 5, 7, 9 }, a.map(e -> e).zipWith(b, (x, y) -> x + y).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 7, 9 }, a.map(e -> e).zipWith(b, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(5f, 7f, 9f), a.map(e -> e).zipWith(b, (x, y) -> x + y).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(7f, 9f), a.map(e -> e).zipWith(b, (x, y) -> x + y).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWith3() {
        FloatStream a = FloatStream.of(1, 2, 3);
        FloatStream b = FloatStream.of(4, 5, 6);
        FloatStream c = FloatStream.of(7, 8, 9);
        assertEquals(3, a.zipWith(b, c, (x, y, z) -> x + y + z).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(2, a.zipWith(b, c, (x, y, z) -> x + y + z).skip(1).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertArrayEquals(new float[] { 12, 15, 18 }, a.zipWith(b, c, (x, y, z) -> x + y + z).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertArrayEquals(new float[] { 15, 18 }, a.zipWith(b, c, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.toList(12f, 15f, 18f), a.zipWith(b, c, (x, y, z) -> x + y + z).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.toList(15f, 18f), a.zipWith(b, c, (x, y, z) -> x + y + z).skip(1).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(3, a.map(e -> e).zipWith(b, c, (x, y, z) -> x + y + z).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(2, a.map(e -> e).zipWith(b, c, (x, y, z) -> x + y + z).skip(1).count());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertArrayEquals(new float[] { 12, 15, 18 }, a.map(e -> e).zipWith(b, c, (x, y, z) -> x + y + z).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertArrayEquals(new float[] { 15, 18 }, a.map(e -> e).zipWith(b, c, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.toList(12f, 15f, 18f), a.map(e -> e).zipWith(b, c, (x, y, z) -> x + y + z).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.toList(15f, 18f), a.map(e -> e).zipWith(b, c, (x, y, z) -> x + y + z).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWith3Defaults() {
        FloatStream a = FloatStream.of(1, 2);
        FloatStream b = FloatStream.of(4, 5, 6);
        FloatStream c = FloatStream.of(7);
        assertEquals(3, a.zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).count());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertEquals(2, a.zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).count());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertArrayEquals(new float[] { 12, 7, 6 }, a.zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).toArray(), 0.001f);
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertArrayEquals(new float[] { 7, 6 }, a.zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertEquals(N.toList(12f, 7f, 6f), a.zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).toList());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertEquals(N.toList(7f, 6f), a.zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).toList());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertEquals(3, a.map(e -> e).zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).count());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertEquals(2, a.map(e -> e).zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).count());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertArrayEquals(new float[] { 12, 7, 6 }, a.map(e -> e).zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).toArray(), 0.001f);
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertArrayEquals(new float[] { 7, 6 }, a.map(e -> e).zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertEquals(N.toList(12f, 7f, 6f), a.map(e -> e).zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).toList());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertEquals(N.toList(7f, 6f), a.map(e -> e).zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithDefaults() {
        FloatStream a = FloatStream.of(1, 2);
        FloatStream b = FloatStream.of(4, 5, 6);
        assertEquals(3, a.zipWith(b, 0, 0, (x, y) -> x + y).count());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertEquals(2, a.zipWith(b, 0, 0, (x, y) -> x + y).skip(1).count());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 5, 7, 6 }, a.zipWith(b, 0, 0, (x, y) -> x + y).toArray(), 0.001f);
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 7, 6 }, a.zipWith(b, 0, 0, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(5f, 7f, 6f), a.zipWith(b, 0, 0, (x, y) -> x + y).toList());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(7f, 6f), a.zipWith(b, 0, 0, (x, y) -> x + y).skip(1).toList());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertEquals(3, a.map(e -> e).zipWith(b, 0, 0, (x, y) -> x + y).count());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertEquals(2, a.map(e -> e).zipWith(b, 0, 0, (x, y) -> x + y).skip(1).count());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 5, 7, 6 }, a.map(e -> e).zipWith(b, 0, 0, (x, y) -> x + y).toArray(), 0.001f);
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertArrayEquals(new float[] { 7, 6 }, a.map(e -> e).zipWith(b, 0, 0, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(5f, 7f, 6f), a.map(e -> e).zipWith(b, 0, 0, (x, y) -> x + y).toList());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.toList(7f, 6f), a.map(e -> e).zipWith(b, 0, 0, (x, y) -> x + y).skip(1).toList());
    }

    // ---- deep review 2026-11-02 G102 begin ----
    private static final float[] G102_SAMPLES = { 0f, -0f, 1.5f, -2.25f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.MIN_VALUE, 3e38f };

    private static float[] g102RandomArray(final Random rnd, final int maxLength) {
        final float[] a = new float[rnd.nextInt(maxLength + 1)];

        for (int i = 0; i < a.length; i++) {
            a[i] = rnd.nextInt(4) == 0 ? G102_SAMPLES[rnd.nextInt(G102_SAMPLES.length)] : rnd.nextInt(1000) / 8f;
        }

        return a;
    }

    private static float[] g102Drain(final FloatIterator iterator) {
        final FloatList list = new FloatList();

        while (iterator.hasNext()) {
            list.add(iterator.nextFloat());
        }

        return list.toArray();
    }

    private static float[] g102Suffix(final float[] a, final int from) {
        return from >= a.length ? new float[0] : Arrays.copyOfRange(a, from, a.length);
    }

    // variant 0: zip(a, b), 1: zip(a, b, c), 2: zip(a, b, -0f, NaN), 3: zip(a, b, c, -0f, NaN, 7.5f); every call is recorded
    private static FloatStream g102Zip(final int variant, final float[] a, final float[] b, final float[] c, final List<String> calls) {
        switch (variant) {
            case 0:
                return FloatStream.zip(a, b, (x, y) -> {
                    calls.add(x + "," + y);
                    return x * 3 + y;
                });
            case 1:
                return FloatStream.zip(a, b, c, (x, y, z) -> {
                    calls.add(x + "," + y + "," + z);
                    return x * 3 + y - z;
                });
            case 2:
                return FloatStream.zip(a, b, -0f, Float.NaN, (x, y) -> {
                    calls.add(x + "," + y);
                    return x * 3 + y;
                });
            default:
                return FloatStream.zip(a, b, c, -0f, Float.NaN, 7.5f, (x, y, z) -> {
                    calls.add(x + "," + y + "," + z);
                    return x * 3 + y - z;
                });
        }
    }

    private static float[] g102ZipReference(final int variant, final float[] a, final float[] b, final float[] c) {
        final int len = variant == 0 ? Math.min(a.length, b.length)
                : variant == 1 ? Math.min(a.length, Math.min(b.length, c.length))
                        : variant == 2 ? Math.max(a.length, b.length) : Math.max(a.length, Math.max(b.length, c.length));
        final float[] result = new float[len];

        for (int i = 0; i < len; i++) {
            final float x = i < a.length ? a[i] : -0f;
            final float y = i < b.length ? b[i] : Float.NaN;
            final float z = i < c.length ? c[i] : 7.5f;
            result[i] = variant == 0 || variant == 2 ? x * 3 + y : x * 3 + y - z;
        }

        return result;
    }

    // G102-01: zip(float[]...) toArray()/toFloatList()/skip(k).toArray()/iterator().toArray()|toList()/sorted() fill an exactly sized
    // array; values and the zipFunction call sequence equal the element-by-element traversal (pin: GREEN on base and fix)
    @Test
    public void testZipArrays_bulkTerminalsMatchElementwiseTraversal() {
        final Random rnd = new Random(102);

        for (int round = 0; round < 200; round++) {
            final float[] a = g102RandomArray(rnd, 12);
            final float[] b = g102RandomArray(rnd, 12);
            final float[] c = g102RandomArray(rnd, 12);

            for (int variant = 0; variant < 4; variant++) {
                final float[] expected = g102ZipReference(variant, a, b, c);
                final List<String> expectedCalls = new ArrayList<>();
                assertArrayEquals(expected, g102Drain(g102Zip(variant, a, b, c, expectedCalls).iterator()));
                assertEquals(expected.length, expectedCalls.size());

                final List<String> calls = new ArrayList<>();
                assertArrayEquals(expected, g102Zip(variant, a, b, c, calls).toArray());
                assertEquals(expectedCalls, calls);

                calls.clear();
                final FloatList list = g102Zip(variant, a, b, c, calls).toFloatList();
                assertArrayEquals(expected, list.toArray());
                assertEquals(expectedCalls, calls);
                list.add(42f);
                assertEquals(expected.length + 1, list.size());

                final int k = rnd.nextInt(expected.length + 2);
                calls.clear();
                assertArrayEquals(g102Suffix(expected, k), g102Zip(variant, a, b, c, calls).skip(k).toArray());
                assertEquals(expectedCalls, calls);

                calls.clear();
                final FloatIterator iter = g102Zip(variant, a, b, c, calls).iterator();
                final int consumed = Math.min(k, expected.length);

                for (int i = 0; i < consumed; i++) {
                    iter.nextFloat();
                }

                assertArrayEquals(g102Suffix(expected, consumed), rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                assertEquals(expectedCalls, calls);
                assertFalse(iter.hasNext());
                org.junit.jupiter.api.Assertions.assertSame(N.EMPTY_FLOAT_ARRAY, iter.toArray());
                assertEquals(0, iter.toList().size());

                calls.clear();
                final float[] sorted = expected.clone();
                Arrays.sort(sorted);
                assertArrayEquals(sorted, g102Zip(variant, a, b, c, calls).sorted().toArray());
                assertEquals(expectedCalls, calls);
            }
        }
    }

    private static boolean g102TakeFirst(final boolean stateful, final float x, final float y, final List<String> calls) {
        calls.add(x + "," + y);
        return stateful ? calls.size() % 3 != 0 : Float.compare(x, y) <= 0;
    }

    private static FloatStream g102Merge(final float[] a, final float[] b, final boolean stateful, final List<String> calls) {
        return FloatStream.merge(a, b, (x, y) -> g102TakeFirst(stateful, x, y, calls) ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
    }

    // G102-01: merge(float[], float[]) toArray()/toFloatList() fill an exactly sized array; values and the order and number of
    // nextSelector calls (a stateful selector makes the result depend on them) equal the element-by-element traversal (pin)
    @Test
    public void testMergeArrays_bulkTerminalsMatchElementwiseTraversal() {
        final Random rnd = new Random(10203);

        for (int round = 0; round < 200; round++) {
            final float[] a = g102RandomArray(rnd, 10);
            final float[] b = g102RandomArray(rnd, 10);

            if (rnd.nextBoolean()) {
                Arrays.sort(a);
                Arrays.sort(b);
            }

            final boolean stateful = rnd.nextBoolean();
            final FloatList expectedList = new FloatList();
            final List<String> expectedCalls = new ArrayList<>();
            int i = 0;
            int j = 0;

            while (i < a.length || j < b.length) {
                if (i < a.length && (j >= b.length || g102TakeFirst(stateful, a[i], b[j], expectedCalls))) {
                    expectedList.add(a[i++]);
                } else {
                    expectedList.add(b[j++]);
                }
            }

            final float[] expected = expectedList.toArray();
            final List<String> calls = new ArrayList<>();
            assertArrayEquals(expected, g102Drain(g102Merge(a, b, stateful, calls).iterator()));
            assertEquals(expectedCalls, calls);

            calls.clear();
            assertArrayEquals(expected, g102Merge(a, b, stateful, calls).toArray());
            assertEquals(expectedCalls, calls);

            calls.clear();
            final FloatList list = g102Merge(a, b, stateful, calls).toFloatList();
            assertArrayEquals(expected, list.toArray());
            assertEquals(expectedCalls, calls);
            list.add(1f);
            assertEquals(expected.length + 1, list.size());

            final int k = rnd.nextInt(expected.length + 2);
            calls.clear();
            assertArrayEquals(g102Suffix(expected, k), g102Merge(a, b, stateful, calls).skip(k).toArray());
            assertEquals(expectedCalls, calls);

            calls.clear();
            final FloatIterator iter = g102Merge(a, b, stateful, calls).iterator();
            final int consumed = Math.min(k, expected.length);

            for (int n = 0; n < consumed; n++) {
                iter.nextFloat();
            }

            assertArrayEquals(g102Suffix(expected, consumed), rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
            assertEquals(expectedCalls, calls);
            assertFalse(iter.hasNext());
            assertEquals(0, iter.toArray().length);
        }
    }

    private static Object g102Bulk(final FloatIterator iterator, final boolean useList) {
        return useList ? iterator.toList() : iterator.toArray();
    }

    // G102-01: a failing zipFunction or nextSelector inside toArray()/toList() leaves the iterator exactly where element-by-element
    // traversal leaves it (zip(a, b)/zip(a, b, c) advance before the call, the padded zips and merge do not) (pin)
    @Test
    public void testZipAndMergeArrays_failingCallbackKeepsIteratorState() {
        final float[] a = { 1, 2, 3, 4, 5 };
        final float[] b = { 10, 20, 30, 40, 50, 60 };
        final float[] c = { 100, 200, 300, 400, 500, 600, 700 };

        for (int viaList = 0; viaList < 2; viaList++) {
            final boolean useList = viaList == 1;
            final int[] calls = { 0 };

            final FloatIterator zip2 = FloatStream.zip(a, b, (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("zip2");
                }

                return x + y;
            }).iterator();
            assertEquals("zip2", assertThrows(IllegalStateException.class, () -> g102Bulk(zip2, useList)).getMessage());
            assertEquals(3, calls[0]);
            assertArrayEquals(new float[] { 44, 55 }, g102Drain(zip2));
            assertEquals(5, calls[0]);

            calls[0] = 0;
            final FloatIterator zip3 = FloatStream.zip(a, b, c, (x, y, z) -> {
                if (++calls[0] == 2) {
                    throw new IllegalStateException("zip3");
                }

                return x + y + z;
            }).iterator();
            assertEquals("zip3", assertThrows(IllegalStateException.class, () -> g102Bulk(zip3, useList)).getMessage());
            assertArrayEquals(new float[] { 333, 444, 555 }, g102Drain(zip3));
            assertEquals(5, calls[0]);

            calls[0] = 0;
            final FloatIterator pad2 = FloatStream.zip(a, b, 0.5f, 0.25f, (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("pad2");
                }

                return x + y;
            }).iterator();
            assertEquals("pad2", assertThrows(IllegalStateException.class, () -> g102Bulk(pad2, useList)).getMessage());
            assertArrayEquals(new float[] { 33, 44, 55, 60.5f }, g102Drain(pad2));
            assertEquals(7, calls[0]);

            calls[0] = 0;
            final FloatIterator pad3 = FloatStream.zip(a, b, c, 0.5f, 0.25f, 0.125f, (x, y, z) -> {
                if (++calls[0] == 6) {
                    throw new IllegalStateException("pad3");
                }

                return x + y + z;
            }).iterator();
            assertEquals("pad3", assertThrows(IllegalStateException.class, () -> g102Bulk(pad3, useList)).getMessage());
            assertArrayEquals(new float[] { 660.5f, 700.75f }, g102Drain(pad3));
            assertEquals(8, calls[0]);

            calls[0] = 0;
            final FloatIterator merge = FloatStream.merge(new float[] { 1, 3, 5 }, new float[] { 2, 4, 6 }, (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("merge");
                }

                return x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
            }).iterator();
            assertEquals("merge", assertThrows(IllegalStateException.class, () -> g102Bulk(merge, useList)).getMessage());
            assertArrayEquals(new float[] { 3, 4, 5, 6 }, g102Drain(merge));
            assertEquals(6, calls[0]);
        }
    }

    // G102-01: a zipFunction that consumes its own stream's iterator re-entrantly (once) ends toArray()/toFloatList() where the
    // hasNext()-driven loop ends, instead of failing with NoSuchElementException (pin)
    @Test
    public void testZipArrays_reentrantCallbackEndsBulkTerminalEarly() {
        final float[] a = { 1, 2, 3, 4, 5 };
        final float[] b = { 10, 20, 30, 40, 50 };
        final float[] c = { 100, 200, 300, 400, 500 };
        final Runnable[] consume = new Runnable[1];
        final boolean[] once = { true };

        FloatStream s = FloatStream.zip(a, b, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return x + y;
        });
        FloatIterator it = s.iterator();
        consume[0] = it::nextFloat;
        assertArrayEquals(new float[] { 11, 33, 44, 55 }, it.toArray());
        assertFalse(it.hasNext());

        once[0] = true;
        s = FloatStream.zip(a, b, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return x + y;
        });
        it = s.iterator();
        consume[0] = it::nextFloat;
        assertArrayEquals(new float[] { 11, 33, 44, 55 }, s.toFloatList().toArray());

        once[0] = true;
        s = FloatStream.zip(a, b, c, (x, y, z) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return x + y + z;
        });
        it = s.iterator();
        consume[0] = it::nextFloat;
        assertArrayEquals(new float[] { 111, 333, 444, 555 }, it.toArray());

        once[0] = true;
        s = FloatStream.zip(a, new float[] { 7 }, -1, -2, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return x + y;
        });
        it = s.iterator();
        consume[0] = it::nextFloat;
        assertArrayEquals(new float[] { 8, 1, 2, 3 }, it.toArray());

        once[0] = true;
        s = FloatStream.zip(a, new float[] { 7 }, new float[0], -1, -2, -3, (x, y, z) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return x + y + z;
        });
        it = s.iterator();
        consume[0] = it::nextFloat;
        assertArrayEquals(new float[] { 5, -2, -1, 0 }, it.toArray());
    }

    private static FloatStream g102Flatten(final float[][] a, final int mode, final float pad) {
        return mode == 0 ? FloatStream.flatten(a, true) : FloatStream.flatten(a, pad, mode == 1);
    }

    // G102-01: flatten(float[][], true) (rectangular walk and the sparse jagged walk) and flatten(float[][], value, vertically)
    // toArray()/toFloatList() fill an exactly sized array; pinned against plain column-major / row-major references, also after
    // skip(k) and partial iteration (pin)
    @Test
    public void testFlattenVerticalAndAligned_bulkTerminalsMatchReference() {
        final Random rnd = new Random(10202);

        for (int round = 0; round < 300; round++) {
            final int shape = rnd.nextInt(3);
            final int rows = shape == 2 ? 5 + rnd.nextInt(6) : rnd.nextInt(7);
            final float[][] a = new float[rows][];
            final int width = 1 + rnd.nextInt(6);

            for (int r = 0; r < rows; r++) {
                if (shape == 1) {
                    a[r] = new float[width];
                    Arrays.fill(a[r], r + 0.5f);
                } else if (shape == 2) {
                    // one long row among null, empty and one-element rows: rows * maxLen > 4 * n selects the jagged walk
                    a[r] = r == rows / 2 ? g102RandomArray(rnd, 40) : rnd.nextInt(3) == 0 ? null : new float[rnd.nextInt(2)];
                } else {
                    a[r] = rnd.nextInt(5) == 0 ? null : g102RandomArray(rnd, 6);
                }
            }

            int maxLen = 0;

            for (final float[] row : a) {
                maxLen = Math.max(maxLen, row == null ? 0 : row.length);
            }

            final float pad = G102_SAMPLES[rnd.nextInt(G102_SAMPLES.length)];
            final FloatList vertical = new FloatList();
            final FloatList alignedVertical = new FloatList();
            final FloatList alignedHorizontal = new FloatList();

            for (int col = 0; col < maxLen; col++) {
                for (final float[] row : a) {
                    if (row != null && col < row.length) {
                        vertical.add(row[col]);
                        alignedVertical.add(row[col]);
                    } else {
                        alignedVertical.add(pad);
                    }
                }
            }

            for (final float[] row : a) {
                for (int col = 0; col < maxLen; col++) {
                    alignedHorizontal.add(row != null && col < row.length ? row[col] : pad);
                }
            }

            for (int mode = 0; mode < 3; mode++) {
                // a single row is returned as-is (no padding) by both flatten overloads
                final float[] expected = rows == 1 ? (a[0] == null ? new float[0] : a[0].clone())
                        : (mode == 0 ? vertical : mode == 1 ? alignedVertical : alignedHorizontal).toArray();

                assertArrayEquals(expected, g102Flatten(a, mode, pad).toArray());
                assertArrayEquals(expected, g102Flatten(a, mode, pad).toFloatList().toArray());
                assertArrayEquals(expected, g102Drain(g102Flatten(a, mode, pad).iterator()));

                final int k = rnd.nextInt(expected.length + 2);
                assertArrayEquals(g102Suffix(expected, k), g102Flatten(a, mode, pad).skip(k).toArray());

                final FloatIterator iter = g102Flatten(a, mode, pad).iterator();
                final int consumed = Math.min(k, expected.length);

                for (int n = 0; n < consumed; n++) {
                    iter.nextFloat();
                }

                assertArrayEquals(g102Suffix(expected, consumed), rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                assertFalse(iter.hasNext());
                assertEquals(0, iter.toList().size());
                assertThrows(NoSuchElementException.class, iter::nextFloat);
            }
        }
    }

    // ---- deep review 2026-11-02 G102 end ----
    // ---- deep review 2026-11-02b G120 begin ----
    // G120-05 (pin): FloatStream.of(FloatBuffer) for a buffer without an accessible array (direct, read-only, sliced, view of a heap
    // ByteBuffer) reads the window [position, limit) through a dedicated iterator; toArray()/toList() fill an exact-size
    // array with the same get(i) reads. Results, laziness (live view), window snapshot and iterator states must match element-wise traversal.
    @Test
    public void testOfFloatBuffer_nonArrayBufferTerminalsMatchElementwise_r116() {
        final float[] content = { 10, Float.NaN, -0.0f, 0.0f, Float.MAX_VALUE, -1.5f, 5, 5, Float.NEGATIVE_INFINITY, -7, 2, 1 };

        for (int kind = 0; kind < 5; kind++) {
            for (int from = 0; from <= content.length; from++) {
                for (int to = from; to <= content.length; to++) {
                    final float[] expected = Arrays.copyOfRange(content, from, to);
                    final java.nio.FloatBuffer buf = g120FloatBuffer(kind, content, from, to);

                    assertFalse(buf.hasArray());
                    assertArrayEquals(expected, FloatStream.of(buf).toArray());
                    assertArrayEquals(expected, FloatStream.of(buf).toFloatList().toArray());
                    assertEquals(expected.length, FloatStream.of(buf).count());
                    assertArrayEquals(g120FloatSuffix(expected, 3), FloatStream.of(buf).skip(3).toArray());
                    assertArrayEquals(g120FloatSorted(expected), FloatStream.of(buf).sorted().toArray());

                    final com.landawn.abacus.util.FloatList list = FloatStream.of(buf).toFloatList();
                    list.add((float) 42); // a fresh, mutable list
                    assertEquals(expected.length + 1, list.size());

                    for (int consumed = 0; consumed <= expected.length; consumed++) {
                        final FloatIterator iter = FloatStream.of(buf).iterator();

                        for (int n = 0; n < consumed; n++) {
                            assertEquals(expected[n], iter.nextFloat());
                        }

                        final boolean viaArray = (consumed + from + kind) % 2 == 0;
                        final float[] rest = viaArray ? iter.toArray() : iter.toList().toArray();
                        assertArrayEquals(g120FloatSuffix(expected, consumed), rest);
                        assertFalse(iter.hasNext());
                        assertEquals(0, iter.toList().size());
                        assertThrows(NoSuchElementException.class, iter::nextFloat);

                        if (expected.length > 0) {
                            // an exhausted iterator returns the shared empty array, as the element-wise toArray() did
                            org.junit.jupiter.api.Assertions.assertSame(com.landawn.abacus.util.N.EMPTY_FLOAT_ARRAY, iter.toArray());
                        } else {
                            assertEquals(0, iter.toArray().length);
                        }
                    }

                    final FloatIteratorEx iterEx = (FloatIteratorEx) FloatStream.of(buf).iterator();
                    iterEx.advance(2);
                    assertArrayEquals(g120FloatSuffix(expected, 2), iterEx.toArray());
                }
            }
        }

        // live view of the window taken at the call: later writes are seen, later position/limit changes are not
        final java.nio.FloatBuffer direct = java.nio.ByteBuffer.allocateDirect(6 * 4).asFloatBuffer();
        direct.put(new float[] { 1, 2, 3, 4, 5, 6 });
        direct.position(1);
        direct.limit(5);

        final FloatStream stream = FloatStream.of(direct);
        direct.put(4, (float) 40);
        direct.clear();
        direct.limit(0);

        assertArrayEquals(new float[] { 2, 3, 4, 40 }, stream.toArray());
        assertEquals(0, direct.position());
        assertEquals(0, direct.limit());
    }

    private static java.nio.FloatBuffer g120FloatBuffer(final int kind, final float[] content, final int from, final int to) {
        final java.nio.FloatBuffer buf;

        if (kind == 0) {
            buf = java.nio.ByteBuffer.allocateDirect(content.length * 4).asFloatBuffer().put(content);
        } else if (kind == 1) {
            buf = java.nio.FloatBuffer.wrap(content.clone()).asReadOnlyBuffer();
        } else if (kind == 2) {
            final java.nio.FloatBuffer outer = java.nio.ByteBuffer.allocateDirect((content.length + 3) * 4).asFloatBuffer();
            outer.position(2);
            outer.put(content);
            outer.position(2);
            outer.limit(2 + content.length);
            buf = outer.slice();
        } else if (kind == 3) {
            buf = java.nio.ByteBuffer.allocateDirect(content.length * 4).asFloatBuffer().put(content).asReadOnlyBuffer();
        } else {
            buf = java.nio.ByteBuffer.allocate(content.length * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).asFloatBuffer().put(content);
        }

        buf.limit(to);
        buf.position(from);

        return buf;
    }

    private static float[] g120FloatSuffix(final float[] a, final int k) {
        return Arrays.copyOfRange(a, Math.min(k, a.length), a.length);
    }

    private static float[] g120FloatSorted(final float[] a) {
        final float[] copy = a.clone();
        Arrays.sort(copy);
        return copy;
    }

    // ---- deep review 2026-11-02b G120 end ----
    // ---- deep review 2026-11-02b G124 begin ----
    // G124: a buffer without an accessible array keeps the close-handler shape of the former
    // IntStream.range(position, limit).mapToFloat(view::get) stream: exactly one (run-once, no-op) close handler.
    @Test
    public void testOfFloatBuffer_nonArrayBufferKeepsRangeCloseHandler_r116() {
        final java.nio.FloatBuffer direct = java.nio.ByteBuffer.allocateDirect(4 * 4).asFloatBuffer();
        direct.put(new float[] { (float) 1, (float) 2, (float) 3, (float) 4 });
        direct.position(1);
        final java.nio.FloatBuffer emptyWindow = direct.duplicate();
        emptyWindow.position(4);

        for (final java.nio.FloatBuffer buf : java.util.List.of(direct, emptyWindow)) {
            // what iterator()'s "Remember to close" warning checks
            final FloatStream stream = FloatStream.of(buf);
            org.junit.jupiter.api.Assertions.assertFalse(StreamBase.isEmptyCloseHandlers(stream.closeHandlers()));
            org.junit.jupiter.api.Assertions.assertEquals(1, stream.closeHandlers().size());

            // handlers registered later run once, in order; a failing one does not stop the others
            final java.util.List<String> calls = new java.util.ArrayList<>();
            final FloatStream withHandlers = FloatStream.of(buf).onClose(() -> calls.add("a")).onClose(() -> {
                calls.add("b");
                throw new IllegalStateException("b");
            }).onClose(() -> calls.add("c"));
            org.junit.jupiter.api.Assertions.assertEquals("b",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, withHandlers::close).getMessage());
            withHandlers.close();
            org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("a", "b", "c"), calls);
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, withHandlers::count);
        }

        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[] { (float) 2, (float) 3, (float) 4 }, FloatStream.of(direct).toArray());
    }
    // ---- deep review 2026-11-02b G124 end ----

    // ---- re-review 2026-11-03 R04 begin ----
    // R04 (pin): FloatStream.of(FloatBuffer) for a buffer without an accessible array counts and skips by moving the cursor of its
    // window iterator instead of reading every skipped element. Pinned for every window, read position and skip length:
    // count(), advance(n) (negative, zero, inside, exactly to and past the end, Long.MAX_VALUE), the elements read after
    // it, the stream-level skip/step/count, the failure-atomic advance flag, and the live view of elements not yet read.
    @Test
    public void testOfFloatBuffer_nonArrayBufferCountAndAdvanceMoveCursor_r116() {
        final float[] content = { 1.5f, -0.0f, Float.NaN, 0f, -1f, 5f, 5f, 3.25f, Float.MAX_VALUE, -7f, 2f, 1f };

        for (int kind = 0; kind < 2; kind++) {
            for (int from = 0; from <= content.length; from++) {
                for (int to = from; to <= content.length; to++) {
                    final float[] expected = java.util.Arrays.copyOfRange(content, from, to);
                    final java.nio.FloatBuffer buf = r04Buffer(kind, content, from, to);
                    org.junit.jupiter.api.Assertions.assertFalse(buf.hasArray());

                    org.junit.jupiter.api.Assertions.assertEquals(expected.length, FloatStream.of(buf).count());

                    for (int k = 0; k <= expected.length + 1; k++) {
                        final int start = Math.min(k, expected.length);
                        final float[] rest = java.util.Arrays.copyOfRange(expected, start, expected.length);
                        org.junit.jupiter.api.Assertions.assertEquals(rest.length, FloatStream.of(buf).skip(k).count());
                        org.junit.jupiter.api.Assertions.assertArrayEquals(rest, FloatStream.of(buf).skip(k).toArray());
                    }

                    org.junit.jupiter.api.Assertions.assertArrayEquals(r04Step(expected, 3), FloatStream.of(buf).step(3).toArray());

                    for (int consumed = 0; consumed <= expected.length; consumed++) {
                        final int remaining = expected.length - consumed;

                        for (final long n : new long[] { -1, 0, 1, 2, remaining, remaining + 1L, Long.MAX_VALUE }) {
                            final int next = consumed + (int) Math.min(Math.max(n, 0), remaining);
                            final FloatIteratorEx iter = (FloatIteratorEx) FloatStream.of(buf).iterator();

                            if (expected.length > 0) {
                                org.junit.jupiter.api.Assertions.assertTrue(iter.supportsFailureAtomicAdvance());
                            }

                            for (int i = 0; i < consumed; i++) {
                                org.junit.jupiter.api.Assertions.assertEquals(expected[i], iter.nextFloat());
                            }

                            iter.advance(n);

                            for (int i = next; i < expected.length; i++) {
                                org.junit.jupiter.api.Assertions.assertTrue(iter.hasNext());
                                org.junit.jupiter.api.Assertions.assertEquals(expected[i], iter.nextFloat());
                            }

                            org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
                            org.junit.jupiter.api.Assertions.assertEquals(0, iter.count());
                            org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class, iter::nextFloat);

                            final FloatIteratorEx counted = (FloatIteratorEx) FloatStream.of(buf).iterator();

                            for (int i = 0; i < consumed; i++) {
                                counted.nextFloat();
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
        final java.nio.FloatBuffer live = r04Buffer(1, content, 0, 6);
        final FloatIteratorEx liveIter = (FloatIteratorEx) FloatStream.of(live).iterator();
        liveIter.advance(2);
        live.put(1, 77.5f);
        live.put(3, 88.5f);
        org.junit.jupiter.api.Assertions.assertEquals(content[2], liveIter.nextFloat());
        org.junit.jupiter.api.Assertions.assertEquals(88.5f, liveIter.nextFloat());
        org.junit.jupiter.api.Assertions.assertEquals(2, liveIter.count());
        org.junit.jupiter.api.Assertions.assertFalse(liveIter.hasNext());
    }

    private static java.nio.FloatBuffer r04Buffer(final int kind, final float[] content, final int from, final int to) {
        // kind 0: read-only view of a heap array; kind 1: writable view of direct memory. Neither exposes an array.
        final java.nio.FloatBuffer buf = kind == 0 ? java.nio.FloatBuffer.wrap(content.clone()).asReadOnlyBuffer()
                : java.nio.ByteBuffer.allocateDirect(content.length * 4).asFloatBuffer().put(content).clear();
        buf.limit(to);
        buf.position(from);
        return buf;
    }

    private static float[] r04Step(final float[] a, final int step) {
        final float[] result = new float[(a.length + step - 1) / step];

        for (int i = 0; i < result.length; i++) {
            result[i] = a[i * step];
        }

        return result;
    }
    // ---- re-review 2026-11-03 R04 end ----
}
