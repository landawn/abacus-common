package com.landawn.abacus.util.stream;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.IndexedFloat;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.u.OptionalFloat;

public class FloatStream104Test extends TestBase {

    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
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
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).count());
        assertEquals(1, FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new float[] { 1, 2 }, FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2 }, FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f), FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toList());
        assertEquals(N.asList(2f), FloatStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toList());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).count());
        assertEquals(1, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new float[] { 1, 2 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toList());
        assertEquals(N.asList(2f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toList());
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
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).count());
        assertArrayEquals(new float[] { 2, 4, 6, 8, 10 }, FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 6, 8, 10 }, FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(2f, 4f, 6f, 8f, 10f), FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).toList());
        assertEquals(N.asList(4f, 6f, 8f, 10f), FloatStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).count());
        assertArrayEquals(new float[] { 2, 4, 6, 8, 10 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 6, 8, 10 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(2f, 4f, 6f, 8f, 10f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).toList());
        assertEquals(N.asList(4f, 6f, 8f, 10f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrepend() {
        assertEquals(8, FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).count());
        assertEquals(7, FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).skip(1).count());
        assertArrayEquals(new float[] { 0, -1, -2, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).toArray(), 0.001f);
        assertArrayEquals(new float[] { -1, -2, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(0f, -1f, -2f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).toList());
        assertEquals(N.asList(-1f, -2f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).prepend(0, -1, -2).skip(1).toList());
        assertEquals(8, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).count());
        assertEquals(7, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).skip(1).count());
        assertArrayEquals(new float[] { 0, -1, -2, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).toArray(), 0.001f);
        assertArrayEquals(new float[] { -1, -2, 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(0f, -1f, -2f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).toList());
        assertEquals(N.asList(-1f, -2f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(0, -1, -2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppend() {
        assertEquals(8, FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).count());
        assertEquals(7, FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8 }, FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8 }, FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f), FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f, 7f, 8f), FloatStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).toList());
        assertEquals(8, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).count());
        assertEquals(7, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f, 7f, 8f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(6, 7, 8).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTop() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).top(3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).top(3).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).top(3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).top(3).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).top(3).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).top(3).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).top(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScan() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 1, 3, 6, 10, 15 }, FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 6, 10, 15 }, FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 3f, 6f, 10f, 15f), FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toList());
        assertEquals(N.asList(3f, 6f, 10f, 15f), FloatStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 1, 3, 6, 10, 15 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 6, 10, 15 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 3f, 6f, 10f, 15f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toList());
        assertEquals(N.asList(3f, 6f, 10f, 15f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInit() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toList());
        assertEquals(N.asList(13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toList());
        assertEquals(N.asList(13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInitIncluded() {
        assertEquals(6, FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 10, 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(10f, 11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(N.asList(11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toList());
        assertEquals(6, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 10, 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 11, 13, 16, 20, 25 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toArray(),
                0.001f);
        assertEquals(N.asList(10f, 11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(N.asList(11f, 13f, 16f, 20f, 25f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 2).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(5, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().count());
        assertEquals(4, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().count());
        assertEquals(4, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIntersection() {
        List<Float> otherList = Arrays.asList(3f, 4f, 5f, 6f, 7f);
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).intersection(otherList).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(otherList).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDifference() {
        List<Float> otherList = Arrays.asList(3f, 4f, 5f, 6f, 7f);
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).difference(otherList).count());
        assertEquals(1, FloatStream.of(1, 2, 3, 4, 5).difference(otherList).skip(1).count());
        assertArrayEquals(new float[] { 1, 2 }, FloatStream.of(1, 2, 3, 4, 5).difference(otherList).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2 }, FloatStream.of(1, 2, 3, 4, 5).difference(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f), FloatStream.of(1, 2, 3, 4, 5).difference(otherList).toList());
        assertEquals(N.asList(2f), FloatStream.of(1, 2, 3, 4, 5).difference(otherList).skip(1).toList());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).count());
        assertEquals(1, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).skip(1).count());
        assertArrayEquals(new float[] { 1, 2 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).toList());
        assertEquals(N.asList(2f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).difference(otherList).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSymmetricDifference() {
        List<Float> otherList = Arrays.asList(3f, 4f, 5f, 6f, 7f);
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).symmetricDifference(otherList).count());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).symmetricDifference(otherList).skip(1).count());
        assertArrayEquals(new float[] { 2, 6, 7 }, FloatStream.of(1, 2, 3, 4, 5).symmetricDifference(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 6f, 7f), FloatStream.of(1, 2, 3, 4, 5).symmetricDifference(otherList).toList());
        assertEquals(N.asList(2f, 6f, 7f), FloatStream.of(1, 2, 3, 4, 5).symmetricDifference(otherList).skip(1).toList());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).count());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 6, 7 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 6, 7 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 6f, 7f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).toList());
        assertEquals(N.asList(2f, 6f, 7f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(otherList).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReversed() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).reversed().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).reversed().skip(1).count());
        assertArrayEquals(new float[] { 5, 4, 3, 2, 1 }, FloatStream.of(1, 2, 3, 4, 5).reversed().toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 3, 2, 1 }, FloatStream.of(1, 2, 3, 4, 5).reversed().skip(1).toArray(), 0.001f);
        assertEquals(N.asList(5f, 4f, 3f, 2f, 1f), FloatStream.of(1, 2, 3, 4, 5).reversed().toList());
        assertEquals(N.asList(4f, 3f, 2f, 1f), FloatStream.of(1, 2, 3, 4, 5).reversed().skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).count());
        assertArrayEquals(new float[] { 5, 4, 3, 2, 1 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 3, 2, 1 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toArray(), 0.001f);
        assertEquals(N.asList(5f, 4f, 3f, 2f, 1f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toList());
        assertEquals(N.asList(4f, 3f, 2f, 1f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRotated() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).rotated(2).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).count());
        assertArrayEquals(new float[] { 4, 5, 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).rotated(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 5, 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(4f, 5f, 1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).rotated(2).toList());
        assertEquals(N.asList(5f, 1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).count());
        assertArrayEquals(new float[] { 4, 5, 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 5, 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(4f, 5f, 1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toList());
        assertEquals(N.asList(5f, 1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterShuffled() {
        // Since shuffled is random, we can only test the count and that all elements are present
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).shuffled().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).shuffled().skip(1).count());
        float[] shuffled = FloatStream.of(1, 2, 3, 4, 5).shuffled().toArray();
        assertEquals(5, shuffled.length);
        // Check all elements are present
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
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, FloatStream.of(5, 3, 1, 4, 2).sorted().count());
        assertEquals(4, FloatStream.of(5, 3, 1, 4, 2).sorted().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).sorted().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).sorted().skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).sorted().toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).sorted().skip(1).toList());
        assertEquals(5, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().count());
        assertEquals(4, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, FloatStream.of(5, 3, 1, 4, 2).reverseSorted().count());
        assertEquals(4, FloatStream.of(5, 3, 1, 4, 2).reverseSorted().skip(1).count());
        assertArrayEquals(new float[] { 5, 4, 3, 2, 1 }, FloatStream.of(5, 3, 1, 4, 2).reverseSorted().toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 3, 2, 1 }, FloatStream.of(5, 3, 1, 4, 2).reverseSorted().skip(1).toArray(), 0.001f);
        assertEquals(N.asList(5f, 4f, 3f, 2f, 1f), FloatStream.of(5, 3, 1, 4, 2).reverseSorted().toList());
        assertEquals(N.asList(4f, 3f, 2f, 1f), FloatStream.of(5, 3, 1, 4, 2).reverseSorted().skip(1).toList());
        assertEquals(5, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().count());
        assertEquals(4, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new float[] { 5, 4, 3, 2, 1 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 3, 2, 1 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().skip(1).toArray(), 0.001f);
        assertEquals(N.asList(5f, 4f, 3f, 2f, 1f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().toList());
        assertEquals(N.asList(4f, 3f, 2f, 1f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).cycled(2).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f, 1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).cycled(2).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).skip(2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).skip(2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).skip(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skip(2).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toList());
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
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skip(2, e -> {
        }).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).skip(2, e -> {
        }).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).toList());
        assertEquals(N.asList(4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, e -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterLimit() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).limit(3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).limit(3).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).limit(3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).limit(3).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).limit(3).toList());
        assertEquals(N.asList(2f, 3f), FloatStream.of(1, 2, 3, 4, 5).limit(3).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toList());
        assertEquals(N.asList(2f, 3f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterStep() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5, 6).step(2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).count());
        assertArrayEquals(new float[] { 1, 3, 5 }, FloatStream.of(1, 2, 3, 4, 5, 6).step(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 5 }, FloatStream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 3f, 5f), FloatStream.of(1, 2, 3, 4, 5, 6).step(2).toList());
        assertEquals(N.asList(3f, 5f), FloatStream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).count());
        assertArrayEquals(new float[] { 1, 3, 5 }, FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 5 }, FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 3f, 5f), FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).toList());
        assertEquals(N.asList(3f, 5f), FloatStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnEach() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).onEach(e -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).onEach(e -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).onEach(e -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).onEach(e -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).onEach(e -> {
        }).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).onEach(e -> {
        }).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(e -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(e -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(e -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(e -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(e -> {
        }).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(e -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPeek() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).peek(e -> {
        }).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).peek(e -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterThrowIfEmpty() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).ifEmpty(() -> {
        }).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).ifEmpty(() -> {
        }).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnClose() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).onClose(() -> {
        }).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToInt() {
        assertEquals(5, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).count());
        assertEquals(4, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).toList());
        assertEquals(N.asList(2, 3, 4, 5), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToInt(f -> (int) f).skip(1).toList());
        assertEquals(5, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).count());
        assertEquals(4, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).toList());
        assertEquals(N.asList(2, 3, 4, 5), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToInt(f -> (int) f).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToLong() {
        assertEquals(5, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).count());
        assertEquals(4, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).skip(1).count());
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).toArray());
        assertArrayEquals(new long[] { 2L, 3L, 4L, 5L }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).skip(1).toArray());
        assertEquals(N.asList(1L, 2L, 3L, 4L, 5L), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).toList());
        assertEquals(N.asList(2L, 3L, 4L, 5L), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).mapToLong(f -> (long) f).skip(1).toList());
        assertEquals(5, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).count());
        assertEquals(4, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).skip(1).count());
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).toArray());
        assertArrayEquals(new long[] { 2L, 3L, 4L, 5L }, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).skip(1).toArray());
        assertEquals(N.asList(1L, 2L, 3L, 4L, 5L), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).toList());
        assertEquals(N.asList(2L, 3L, 4L, 5L), FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f, 5.5f).map(e -> e).mapToLong(f -> (long) f).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToDouble() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).skip(1).count());
        assertArrayEquals(new double[] { 1.5, 3.0, 4.5, 6.0, 7.5 }, FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).toArray(), 0.001);
        assertArrayEquals(new double[] { 3.0, 4.5, 6.0, 7.5 }, FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).skip(1).toArray(), 0.001);
        assertEquals(N.asList(1.5, 3.0, 4.5, 6.0, 7.5), FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).toList());
        assertEquals(N.asList(3.0, 4.5, 6.0, 7.5), FloatStream.of(1, 2, 3, 4, 5).mapToDouble(f -> f * 1.5).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).skip(1).count());
        assertArrayEquals(new double[] { 1.5, 3.0, 4.5, 6.0, 7.5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).toArray(), 0.001);
        assertArrayEquals(new double[] { 3.0, 4.5, 6.0, 7.5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).skip(1).toArray(), 0.001);
        assertEquals(N.asList(1.5, 3.0, 4.5, 6.0, 7.5), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).toList());
        assertEquals(N.asList(3.0, 4.5, 6.0, 7.5), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(f -> f * 1.5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToObj() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F2.0", "F3.0", "F4.0", "F5.0" }, FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).toArray());
        assertArrayEquals(new String[] { "F2.0", "F3.0", "F4.0", "F5.0" }, FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).skip(1).toArray());
        assertEquals(N.asList("F1.0", "F2.0", "F3.0", "F4.0", "F5.0"), FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).toList());
        assertEquals(N.asList("F2.0", "F3.0", "F4.0", "F5.0"), FloatStream.of(1, 2, 3, 4, 5).mapToObj(f -> "F" + f).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F2.0", "F3.0", "F4.0", "F5.0" }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).toArray());
        assertArrayEquals(new String[] { "F2.0", "F3.0", "F4.0", "F5.0" }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).skip(1).toArray());
        assertEquals(N.asList("F1.0", "F2.0", "F3.0", "F4.0", "F5.0"), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).toList());
        assertEquals(N.asList("F2.0", "F3.0", "F4.0", "F5.0"), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(f -> "F" + f).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).count());
        assertArrayEquals(new float[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 }, FloatStream.of(1, 2, 3, 4, 5).flatMap(f -> FloatStream.of(f, f * 10)).toArray(),
                0.001f);
        assertArrayEquals(new float[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f), FloatStream.of(1, 2, 3, 4, 5).flatMap(f -> FloatStream.of(f, f * 10)).toList());
        assertEquals(N.asList(10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f), FloatStream.of(1, 2, 3, 4, 5).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).count());
        assertArrayEquals(new float[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).toList());
        assertEquals(N.asList(10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMap(f -> FloatStream.of(f, f * 10)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToLong() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).count());
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).toArray());
        assertArrayEquals(new long[] { 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).toArray());
        assertEquals(N.asList(1L, 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).toList());
        assertEquals(N.asList(10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).count());
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).toArray());
        assertArrayEquals(new long[] { 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).toArray());
        assertEquals(N.asList(1L, 10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).toList());
        assertEquals(N.asList(10L, 2L, 20L, 3L, 30L, 4L, 40L, 5L, 50L),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToLong(f -> LongStream.of((long) f, (long) (f * 10))).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToDouble() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).count());
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0 },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).toArray(), 0.001);
        assertArrayEquals(new double[] { 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0 },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).toArray(), 0.001);
        assertEquals(N.asList(1.0, 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).toList());
        assertEquals(N.asList(10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).count());
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).toArray(), 0.001);
        assertArrayEquals(new double[] { 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).toArray(), 0.001);
        assertEquals(N.asList(1.0, 10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).toList());
        assertEquals(N.asList(10.0, 2.0, 20.0, 3.0, 30.0, 4.0, 40.0, 5.0, 50.0),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToDouble(f -> DoubleStream.of(f, f * 10)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToObj() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).toArray());
        assertEquals(N.asList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).toList());
        assertEquals(N.asList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).skip(1).toArray());
        assertEquals(N.asList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToObj(f -> Stream.of("F" + f, "F" + (f * 10))).toList());
        assertEquals(N.asList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
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
        assertEquals(N.asList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).toList());
        assertEquals(N.asList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).skip(1).toArray());
        assertEquals(N.asList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).toList());
        assertEquals(N.asList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmapToObj(f -> Arrays.asList("F" + f, "F" + (f * 10))).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattmapToObj() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flattmapToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flattmapToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).flattmapToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).flattmapToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).toArray());
        assertEquals(N.asList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flattmapToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).toList());
        assertEquals(N.asList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).flattmapToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flattmapToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flattmapToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).count());
        assertArrayEquals(new String[] { "F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flattmapToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).toArray());
        assertArrayEquals(new String[] { "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0" },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flattmapToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).toArray());
        assertEquals(N.asList("F1.0", "F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flattmapToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).toList());
        assertEquals(N.asList("F10.0", "F2.0", "F20.0", "F3.0", "F30.0", "F4.0", "F40.0", "F5.0", "F50.0"),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flattmapToObj(f -> new String[] { "F" + f, "F" + (f * 10) }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartial() {
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).count());
        assertEquals(2, FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).count());
        assertArrayEquals(new float[] { 2, 6, 10 },
                FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 6, 10 },
                FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(2f, 6f, 10f),
                FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).toList());
        assertEquals(N.asList(6f, 10f),
                FloatStream.of(1, 2, 3, 4, 5).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).count());
        assertEquals(2,
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).count());
        assertArrayEquals(new float[] { 2, 6, 10 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 6, 10 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).toArray(),
                0.001f);
        assertEquals(N.asList(2f, 6f, 10f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).toList());
        assertEquals(N.asList(6f, 10f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(f -> f % 2 == 0 ? OptionalFloat.empty() : OptionalFloat.of(f * 2)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAsDoubleStream() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().skip(1).count());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().toArray(), 0.001);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0, 5.0 }, FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().skip(1).toArray(), 0.001);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), FloatStream.of(1, 2, 3, 4, 5).asDoubleStream().skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().skip(1).count());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().toArray(), 0.001);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0, 5.0 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().skip(1).toArray(), 0.001);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterBoxed() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).boxed().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).boxed().skip(1).count());
        assertArrayEquals(new Float[] { 1f, 2f, 3f, 4f, 5f }, FloatStream.of(1, 2, 3, 4, 5).boxed().toArray());
        assertArrayEquals(new Float[] { 2f, 3f, 4f, 5f }, FloatStream.of(1, 2, 3, 4, 5).boxed().skip(1).toArray());
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).boxed().toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).boxed().skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().skip(1).count());
        assertArrayEquals(new Float[] { 1f, 2f, 3f, 4f, 5f }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().toArray());
        assertArrayEquals(new Float[] { 2f, 3f, 4f, 5f }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().skip(1).toArray());
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmap() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatmap(f -> new float[] { f, f * 10 }).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatmap(f -> new float[] { f, f * 10 }).skip(1).count());
        assertArrayEquals(new float[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 }, FloatStream.of(1, 2, 3, 4, 5).flatmap(f -> new float[] { f, f * 10 }).toArray(),
                0.001f);
        assertArrayEquals(new float[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).flatmap(f -> new float[] { f, f * 10 }).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f), FloatStream.of(1, 2, 3, 4, 5).flatmap(f -> new float[] { f, f * 10 }).toList());
        assertEquals(N.asList(10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f), FloatStream.of(1, 2, 3, 4, 5).flatmap(f -> new float[] { f, f * 10 }).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmap(f -> new float[] { f, f * 10 }).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmap(f -> new float[] { f, f * 10 }).skip(1).count());
        assertArrayEquals(new float[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmap(f -> new float[] { f, f * 10 }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmap(f -> new float[] { f, f * 10 }).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmap(f -> new float[] { f, f * 10 }).toList());
        assertEquals(N.asList(10f, 2f, 20f, 3f, 30f, 4f, 40f, 5f, 50f),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatmap(f -> new float[] { f, f * 10 }).skip(1).toList());

    }

    @Test
    public void testStreamCreatedAfterFlatMapToInt() {
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).toArray());
        assertArrayEquals(new int[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).toArray());
        assertEquals(N.asList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).toList());
        assertEquals(N.asList(10, 2, 20, 3, 30, 4, 40, 5, 50),
                FloatStream.of(1, 2, 3, 4, 5).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).toList());
        assertEquals(10, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).count());
        assertEquals(9, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).toArray());
        assertArrayEquals(new int[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).toArray());
        assertEquals(N.asList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).toList());
        assertEquals(N.asList(10, 2, 20, 3, 30, 4, 40, 5, 50),
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).flatMapToInt(f -> IntStream.of((int) f, (int) (f * 10))).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterEmpty() {
        assertEquals(0, FloatStream.empty().count());
        assertEquals(0, FloatStream.empty().skip(1).count());
        assertArrayEquals(new float[] {}, FloatStream.empty().toArray(), 0.001f);
        assertArrayEquals(new float[] {}, FloatStream.empty().skip(1).toArray(), 0.001f);
        assertEquals(N.asList(), FloatStream.empty().toList());
        assertEquals(N.asList(), FloatStream.empty().skip(1).toList());
        assertEquals(0, FloatStream.empty().map(e -> e).count());
        assertEquals(0, FloatStream.empty().map(e -> e).skip(1).count());
        assertArrayEquals(new float[] {}, FloatStream.empty().map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] {}, FloatStream.empty().map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(), FloatStream.empty().map(e -> e).toList());
        assertEquals(N.asList(), FloatStream.empty().map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDefer() {
        float[][] array = { { 1, 2, 3 }, { 4, 5 } };

        assertEquals(5, FloatStream.defer(() -> FloatStream.of(1, 2, 3, 4, 5)).count());
        assertEquals(4, FloatStream.defer(() -> FloatStream.of(1, 2, 3, 4, 5)).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.defer(() -> FloatStream.of(1, 2, 3, 4, 5)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.defer(() -> FloatStream.of(1, 2, 3, 4, 5)).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.defer(() -> FloatStream.of(1, 2, 3, 4, 5)).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.flatten(array).skip(1).toList());
        assertEquals(5, FloatStream.flatten(array).map(e -> e).count());
        assertEquals(4, FloatStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRepeat() {
        assertEquals(5, FloatStream.repeat(3.14f, 5).count());
        assertEquals(4, FloatStream.repeat(3.14f, 5).skip(1).count());
        assertArrayEquals(new float[] { 3.14f, 3.14f, 3.14f, 3.14f, 3.14f }, FloatStream.repeat(3.14f, 5).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3.14f, 3.14f, 3.14f, 3.14f }, FloatStream.repeat(3.14f, 5).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3.14f, 3.14f, 3.14f, 3.14f, 3.14f), FloatStream.repeat(3.14f, 5).toList());
        assertEquals(N.asList(3.14f, 3.14f, 3.14f, 3.14f), FloatStream.repeat(3.14f, 5).skip(1).toList());
        assertEquals(5, FloatStream.repeat(3.14f, 5).map(e -> e).count());
        assertEquals(4, FloatStream.repeat(3.14f, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 3.14f, 3.14f, 3.14f, 3.14f, 3.14f }, FloatStream.repeat(3.14f, 5).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3.14f, 3.14f, 3.14f, 3.14f }, FloatStream.repeat(3.14f, 5).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3.14f, 3.14f, 3.14f, 3.14f, 3.14f), FloatStream.repeat(3.14f, 5).map(e -> e).toList());
        assertEquals(N.asList(3.14f, 3.14f, 3.14f, 3.14f), FloatStream.repeat(3.14f, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRandom() {
        assertEquals(10, FloatStream.random().limit(10).count());
        assertEquals(9, FloatStream.random().limit(10).skip(1).count());
        float[] randomArray = FloatStream.random().limit(10).toArray();
        assertEquals(10, randomArray.length);
        // Check all values are between 0 (inclusive) and 1 (exclusive)
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
        assertEquals(N.asList(0f, 1f, 2f, 3f, 4f), FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).toList());
        counter[0] = 0;
        assertEquals(N.asList(1f, 2f, 3f, 4f), FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).skip(1).toList());
        counter[0] = 0;
        assertEquals(5, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).count());
        counter[0] = 0;
        assertEquals(4, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).skip(1).count());
        counter[0] = 0;
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4 }, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).toArray(), 0.001f);
        counter[0] = 0;
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).skip(1).toArray(), 0.001f);
        counter[0] = 0;
        assertEquals(N.asList(0f, 1f, 2f, 3f, 4f), FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).toList());
        counter[0] = 0;
        assertEquals(N.asList(1f, 2f, 3f, 4f), FloatStream.iterate(() -> counter[0] < 5, () -> counter[0]++).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(0f, 1f, 2f, 3f, 4f, 5f), FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).toList());
        counter[0] = 0;
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.iterate(0, () -> counter[0] < 5, f -> {
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
        assertEquals(N.asList(0f, 1f, 2f, 3f, 4f, 5f), FloatStream.iterate(0, () -> counter[0] < 5, f -> {
            counter[0]++;
            return f + 1;
        }).map(e -> e).toList());
        counter[0] = 0;
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.iterate(0, () -> counter[0] < 5, f -> {
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
        assertEquals(N.asList(0f, 1f, 2f, 3f, 4f), FloatStream.iterate(0, f -> f < 5, f -> f + 1).toList());
        assertEquals(N.asList(1f, 2f, 3f, 4f), FloatStream.iterate(0, f -> f < 5, f -> f + 1).skip(1).toList());
        assertEquals(5, FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).count());
        assertEquals(4, FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4 }, FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(0f, 1f, 2f, 3f, 4f), FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).toList());
        assertEquals(N.asList(1f, 2f, 3f, 4f), FloatStream.iterate(0, f -> f < 5, f -> f + 1).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIterateUnlimited() {
        assertEquals(10, FloatStream.iterate(1, f -> f * 2).limit(10).count());
        assertEquals(9, FloatStream.iterate(1, f -> f * 2).limit(10).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512 }, FloatStream.iterate(1, f -> f * 2).limit(10).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 4, 8, 16, 32, 64, 128, 256, 512 }, FloatStream.iterate(1, f -> f * 2).limit(10).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f, 512f), FloatStream.iterate(1, f -> f * 2).limit(10).toList());
        assertEquals(N.asList(2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f, 512f), FloatStream.iterate(1, f -> f * 2).limit(10).skip(1).toList());
        assertEquals(10, FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).count());
        assertEquals(9, FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512 }, FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 4, 8, 16, 32, 64, 128, 256, 512 }, FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).skip(1).toArray(),
                0.001f);
        assertEquals(N.asList(1f, 2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f, 512f), FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).toList());
        assertEquals(N.asList(2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f, 512f), FloatStream.iterate(1, f -> f * 2).map(e -> e).limit(10).skip(1).toList());
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
        assertEquals(N.asList(0f, 1f, 2f, 3f, 4f), FloatStream.generate(() -> value[0]++).limit(5).toList());
        value[0] = 0;
        assertEquals(N.asList(1f, 2f, 3f, 4f), FloatStream.generate(() -> value[0]++).limit(5).skip(1).toList());
        value[0] = 0;
        assertEquals(5, FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).count());
        value[0] = 0;
        assertEquals(4, FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).skip(1).count());
        value[0] = 0;
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4 }, FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).toArray(), 0.001f);
        value[0] = 0;
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).skip(1).toArray(), 0.001f);
        value[0] = 0;
        assertEquals(N.asList(0f, 1f, 2f, 3f, 4f), FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).toList());
        value[0] = 0;
        assertEquals(N.asList(1f, 2f, 3f, 4f), FloatStream.generate(() -> value[0]++).map(e -> e).limit(5).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.concat(a1, a2, a3).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.concat(a1, a2, a3).skip(1).toList());
        assertEquals(5, FloatStream.concat(a1, a2, a3).map(e -> e).count());
        assertEquals(4, FloatStream.concat(a1, a2, a3).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.concat(a1, a2, a3).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.concat(a1, a2, a3).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.concat(a1, a2, a3).map(e -> e).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.concat(a1, a2, a3).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.concat(s1, s2, s3).toList());
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.concat(s1, s2, s3).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.concat(s1, s2, s3).map(e -> e).toList());
        s1 = FloatStream.of(1, 2);
        s2 = FloatStream.of(3, 4);
        s3 = FloatStream.of(5);
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.concat(s1, s2, s3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfNullable() {
        assertEquals(1, FloatStream.ofNullable(5f).count());
        assertEquals(0, FloatStream.ofNullable(5f).skip(1).count());
        assertArrayEquals(new float[] { 5 }, FloatStream.ofNullable(5f).toArray(), 0.001f);
        assertArrayEquals(new float[] {}, FloatStream.ofNullable(5f).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(5f), FloatStream.ofNullable(5f).toList());
        assertEquals(N.asList(), FloatStream.ofNullable(5f).skip(1).toList());
        assertEquals(1, FloatStream.ofNullable(5f).map(e -> e).count());
        assertEquals(0, FloatStream.ofNullable(5f).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 5 }, FloatStream.ofNullable(5f).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] {}, FloatStream.ofNullable(5f).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(5f), FloatStream.ofNullable(5f).map(e -> e).toList());
        assertEquals(N.asList(), FloatStream.ofNullable(5f).map(e -> e).skip(1).toList());

        assertEquals(0, FloatStream.ofNullable(null).count());
        assertEquals(0, FloatStream.ofNullable(null).skip(1).count());
        assertArrayEquals(new float[] {}, FloatStream.ofNullable(null).toArray(), 0.001f);
        assertArrayEquals(new float[] {}, FloatStream.ofNullable(null).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(), FloatStream.ofNullable(null).toList());
        assertEquals(N.asList(), FloatStream.ofNullable(null).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfWithStartEndIndex() {
        assertEquals(3, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).count());
        assertEquals(2, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).skip(1).count());
        assertArrayEquals(new float[] { 2, 3, 4 }, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 4 }, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(2f, 3f, 4f), FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).toList());
        assertEquals(N.asList(3f, 4f), FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).skip(1).toList());
        assertEquals(3, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).count());
        assertEquals(2, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 2, 3, 4 }, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 4 }, FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(2f, 3f, 4f), FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).toList());
        assertEquals(N.asList(3f, 4f), FloatStream.of(new float[] { 1, 2, 3, 4, 5 }, 1, 4).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfFloatArray() {
        assertEquals(5, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).count());
        assertEquals(4, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).skip(1).toList());
        assertEquals(5, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).count());
        assertEquals(4, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfFloatArrayWithStartEndIndex() {
        assertEquals(3, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).count());
        assertEquals(2, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).skip(1).count());
        assertArrayEquals(new float[] { 2, 3, 4 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 4 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(2f, 3f, 4f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).toList());
        assertEquals(N.asList(3f, 4f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).skip(1).toList());
        assertEquals(3, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).count());
        assertEquals(2, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 2, 3, 4 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 3, 4 }, FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(2f, 3f, 4f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).toList());
        assertEquals(N.asList(3f, 4f), FloatStream.of(new Float[] { 1f, 2f, 3f, 4f, 5f }, 1, 4).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOfCollection() {
        List<Float> list = Arrays.asList(1f, 2f, 3f, 4f, 5f);
        assertEquals(5, FloatStream.of(list).count());
        assertEquals(4, FloatStream.of(list).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(list).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(list).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(list).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(list).skip(1).toList());
        assertEquals(5, FloatStream.of(list).map(e -> e).count());
        assertEquals(4, FloatStream.of(list).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(list).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(list).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(list).map(e -> e).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(list).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(iter).toList());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(iter).skip(1).toList());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(5, FloatStream.of(iter).map(e -> e).count());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(4, FloatStream.of(iter).map(e -> e).skip(1).count());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(iter).map(e -> e).toArray(), 0.001f);
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(iter).map(e -> e).skip(1).toArray(), 0.001f);
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(iter).map(e -> e).toList());
        iter = FloatIterator.of(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(iter).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(buffer).toList());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(buffer).skip(1).toList());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(5, FloatStream.of(buffer).map(e -> e).count());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(4, FloatStream.of(buffer).map(e -> e).skip(1).count());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(buffer).map(e -> e).toArray(), 0.001f);
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(buffer).map(e -> e).skip(1).toArray(), 0.001f);
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(buffer).map(e -> e).toList());
        buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(buffer).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatten2D() {
        float[][] array = { { 1, 2 }, { 3, 4 }, { 5 } };
        assertEquals(5, FloatStream.flatten(array).count());
        assertEquals(4, FloatStream.flatten(array).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.flatten(array).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.flatten(array).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.flatten(array).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.flatten(array).skip(1).toList());
        assertEquals(5, FloatStream.flatten(array).map(e -> e).count());
        assertEquals(4, FloatStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatten2DVertically() {
        float[][] array = { { 1, 2, 3 }, { 4, 5, 6 } };
        assertEquals(6, FloatStream.flatten(array, true).count());
        assertEquals(5, FloatStream.flatten(array, true).skip(1).count());
        assertArrayEquals(new float[] { 1, 4, 2, 5, 3, 6 }, FloatStream.flatten(array, true).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 2, 5, 3, 6 }, FloatStream.flatten(array, true).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 4f, 2f, 5f, 3f, 6f), FloatStream.flatten(array, true).toList());
        assertEquals(N.asList(4f, 2f, 5f, 3f, 6f), FloatStream.flatten(array, true).skip(1).toList());
        assertEquals(6, FloatStream.flatten(array, true).map(e -> e).count());
        assertEquals(5, FloatStream.flatten(array, true).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 4, 2, 5, 3, 6 }, FloatStream.flatten(array, true).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 4, 2, 5, 3, 6 }, FloatStream.flatten(array, true).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 4f, 2f, 5f, 3f, 6f), FloatStream.flatten(array, true).map(e -> e).toList());
        assertEquals(N.asList(4f, 2f, 5f, 3f, 6f), FloatStream.flatten(array, true).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatten3D() {
        float[][][] array = { { { 1, 2 }, { 3 } }, { { 4, 5 } } };
        assertEquals(5, FloatStream.flatten(array).count());
        assertEquals(4, FloatStream.flatten(array).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.flatten(array).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.flatten(array).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.flatten(array).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.flatten(array).skip(1).toList());
        assertEquals(5, FloatStream.flatten(array).map(e -> e).count());
        assertEquals(4, FloatStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.flatten(array).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRangeMap() {
        assertEquals(3, FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).count());
        assertEquals(2, FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).count());
        assertArrayEquals(new float[] { 2, 6, 10 },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).toArray(), 0.001f);
        assertArrayEquals(new float[] { 6, 10 },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(2f, 6f, 10f),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).toList());
        assertEquals(N.asList(6f, 10f),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).count());
        assertEquals(2,
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).count());
        assertArrayEquals(new float[] { 2, 6, 10 },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).toArray(), 0.001f);
        assertArrayEquals(new float[] { 6, 10 },
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).skip(1).toArray(),
                0.001f);
        assertEquals(N.asList(2f, 6f, 10f),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> last).toList());
        assertEquals(N.asList(6f, 10f),
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
        assertEquals(N.asList("1.0-2.0", "5.0-6.0", "10.0-10.0"),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last).toList());
        assertEquals(N.asList("5.0-6.0", "10.0-10.0"),
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
        assertEquals(N.asList("1.0-2.0", "5.0-6.0", "10.0-10.0"),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .map(e -> e)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .toList());
        assertEquals(N.asList("5.0-6.0", "10.0-10.0"),
                FloatStream.of(1, 2, 2, 5, 6, 6, 6, 10)
                        .map(e -> e)
                        .rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .toList());
    }

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
        assertEquals(N.asList(3f, 18f, 10f), FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).toList());
        assertEquals(N.asList(18f, 10f), FloatStream.of(1, 2, 5, 6, 7, 10).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).count());
        assertEquals(2, FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 3, 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 18f, 10f),
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((prev, next) -> next - prev <= 1, (a, b) -> a + b).toList());
        assertEquals(N.asList(18f, 10f),
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
        assertEquals(N.asList(3f, 18f, 10f), FloatStream.of(1, 2, 5, 6, 7, 10).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.asList(18f, 10f), FloatStream.of(1, 2, 5, 6, 7, 10).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new float[] { 3, 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toArray(), 0.001f);
        assertArrayEquals(new float[] { 18, 10 },
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(3f, 18f, 10f),
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.asList(18f, 10f),
                FloatStream.of(1, 2, 5, 6, 7, 10).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f), a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f), a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f), a.map(e -> e).mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f),
                a.map(e -> e).mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
    }

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
        assertEquals(N.asList(5f, 7f, 9f), a.zipWith(b, (x, y) -> x + y).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.asList(7f, 9f), a.zipWith(b, (x, y) -> x + y).skip(1).toList());
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
        assertEquals(N.asList(5f, 7f, 9f), a.map(e -> e).zipWith(b, (x, y) -> x + y).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.asList(7f, 9f), a.map(e -> e).zipWith(b, (x, y) -> x + y).skip(1).toList());
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
        assertEquals(N.asList(12f, 15f, 18f), a.zipWith(b, c, (x, y, z) -> x + y + z).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.asList(15f, 18f), a.zipWith(b, c, (x, y, z) -> x + y + z).skip(1).toList());
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
        assertEquals(N.asList(12f, 15f, 18f), a.map(e -> e).zipWith(b, c, (x, y, z) -> x + y + z).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.asList(15f, 18f), a.map(e -> e).zipWith(b, c, (x, y, z) -> x + y + z).skip(1).toList());
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
        assertEquals(N.asList(5f, 7f, 6f), a.zipWith(b, 0, 0, (x, y) -> x + y).toList());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.asList(7f, 6f), a.zipWith(b, 0, 0, (x, y) -> x + y).skip(1).toList());
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
        assertEquals(N.asList(5f, 7f, 6f), a.map(e -> e).zipWith(b, 0, 0, (x, y) -> x + y).toList());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.asList(7f, 6f), a.map(e -> e).zipWith(b, 0, 0, (x, y) -> x + y).skip(1).toList());
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
        assertEquals(N.asList(12f, 7f, 6f), a.zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).toList());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertEquals(N.asList(7f, 6f), a.zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).toList());
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
        assertEquals(N.asList(12f, 7f, 6f), a.map(e -> e).zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).toList());
        a = FloatStream.of(1, 2);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7);
        assertEquals(N.asList(7f, 6f), a.map(e -> e).zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterThrowIfEmptyWithSupplier() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).toArray(),
                0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).toArray(),
                0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 },
                FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty")).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPsp() {
        // psp stands for "parallel-sequential-parallel"
        // This method temporarily switches to sequential for a specific operation
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).parallel().psp(s -> s.sorted()).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).parallel().psp(s -> s.sorted()).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).parallel().psp(s -> s.sorted()).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).parallel().psp(s -> s.sorted()).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).parallel().psp(s -> s.sorted()).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).parallel().psp(s -> s.sorted()).skip(1).toList());
        assertEquals(5, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).count());
        assertEquals(4, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(5, 3, 1, 4, 2).map(e -> e).parallel().psp(s -> s.sorted()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRateLimited() {
        // rateLimited method delays elements - for testing, we'll just verify the stream is returned correctly
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).rateLimited(1000.0).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(1000.0).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).rateLimited(rateLimiter).toList());
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).rateLimited(rateLimiter).skip(1).toList());
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).count());
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).skip(1).count());
        rateLimiter = RateLimiter.create(1000.0);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).toArray(), 0.001f);
        rateLimiter = RateLimiter.create(1000.0);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).skip(1).toArray(), 0.001f);
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).toList());
        rateLimiter = RateLimiter.create(1000.0);
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(rateLimiter).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDelay() {
        Duration duration = Duration.ofMillis(0); // 0 delay to make tests run fast
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).delay(duration).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).delay(duration).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).delay(duration).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDelayJavaTime() {
        java.time.Duration duration = java.time.Duration.ofMillis(0); // 0 delay to make tests run fast
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).delay(duration).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).delay(duration).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).delay(duration).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).delay(duration).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).delay(duration).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), stream.prepend(prepend).toList());
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertEquals(N.asList(2f, 3f, 4f, 5f), stream.prepend(prepend).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), stream.map(e -> e).prepend(prepend).toList());
        prepend = FloatStream.of(1, 2);
        stream = FloatStream.of(3, 4, 5);
        assertEquals(N.asList(2f, 3f, 4f, 5f), stream.map(e -> e).prepend(prepend).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.of(2, 3, 4, 5, 6).prepend(op).toList());
        op = OptionalFloat.of(1);
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f), FloatStream.of(2, 3, 4, 5, 6).prepend(op).skip(1).toList());
        op = OptionalFloat.of(1);
        assertEquals(6, FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).count());
        op = OptionalFloat.of(1);
        assertEquals(5, FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).skip(1).count());
        op = OptionalFloat.of(1);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 }, FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).toArray(), 0.001f);
        op = OptionalFloat.of(1);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 }, FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).skip(1).toArray(), 0.001f);
        op = OptionalFloat.of(1);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).toList());
        op = OptionalFloat.of(1);
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f), FloatStream.of(2, 3, 4, 5, 6).map(e -> e).prepend(op).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), stream.append(append).toList());
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertEquals(N.asList(2f, 3f, 4f, 5f), stream.append(append).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), stream.map(e -> e).append(append).toList());
        append = FloatStream.of(4, 5);
        stream = FloatStream.of(1, 2, 3);
        assertEquals(N.asList(2f, 3f, 4f, 5f), stream.map(e -> e).append(append).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.of(1, 2, 3, 4, 5).append(op).toList());
        op = OptionalFloat.of(6);
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f), FloatStream.of(1, 2, 3, 4, 5).append(op).skip(1).toList());
        op = OptionalFloat.of(6);
        assertEquals(6, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).count());
        op = OptionalFloat.of(6);
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).skip(1).count());
        op = OptionalFloat.of(6);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).toArray(), 0.001f);
        op = OptionalFloat.of(6);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).skip(1).toArray(), 0.001f);
        op = OptionalFloat.of(6);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).toList());
        op = OptionalFloat.of(6);
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).append(op).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmptySupplier() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).toArray(),
                0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toArray(),
                0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toList());

        // Test with empty stream
        assertEquals(3, FloatStream.empty().appendIfEmpty(() -> FloatStream.of(6, 7, 8)).count());
        assertEquals(2, FloatStream.empty().appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).count());
        assertArrayEquals(new float[] { 6, 7, 8 }, FloatStream.empty().appendIfEmpty(() -> FloatStream.of(6, 7, 8)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 8 }, FloatStream.empty().appendIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toArray(), 0.001f);
    }

    @Test
    public void testStreamCreatedAfterDefaultIfEmpty() {
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toList());
        assertEquals(5, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).count());
        assertEquals(4, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).toArray(),
                0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5 }, FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toArray(),
                0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f), FloatStream.of(1, 2, 3, 4, 5).map(e -> e).defaultIfEmpty(() -> FloatStream.of(6, 7, 8)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipArrays() {
        float[] a = { 1, 2, 3 };
        float[] b = { 4, 5, 6 };
        assertEquals(3, FloatStream.zip(a, b, (x, y) -> x + y).count());
        assertEquals(2, FloatStream.zip(a, b, (x, y) -> x + y).skip(1).count());
        assertArrayEquals(new float[] { 5, 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).toList());
        assertEquals(N.asList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).skip(1).toList());
        assertEquals(3, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).count());
        assertEquals(2, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 5, 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 9 }, FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).toList());
        assertEquals(N.asList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZip3Arrays() {
        float[] a = { 1, 2, 3 };
        float[] b = { 4, 5, 6 };
        float[] c = { 7, 8, 9 };
        assertEquals(3, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).count());
        assertEquals(2, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).count());
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray(), 0.001f);
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toList());
        assertEquals(N.asList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).toList());
        assertEquals(3, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).count());
        assertEquals(2, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).toList());
        assertEquals(N.asList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).toList());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.asList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).skip(1).toList());
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
        assertEquals(N.asList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).toList());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.asList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toList());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(N.asList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).toList());
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
        assertEquals(N.asList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).toList());
        a = FloatIterator.of(new float[] { 1, 2, 3 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        c = FloatIterator.of(new float[] { 7, 8, 9 });
        assertEquals(N.asList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.asList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).skip(1).toList());
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
        assertEquals(N.asList(5f, 7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        assertEquals(N.asList(7f, 9f), FloatStream.zip(a, b, (x, y) -> x + y).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.asList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).skip(1).toList());
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
        assertEquals(N.asList(12f, 15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).toList());
        a = FloatStream.of(1, 2, 3);
        b = FloatStream.of(4, 5, 6);
        c = FloatStream.of(7, 8, 9);
        assertEquals(N.asList(15f, 18f), FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(12f, 15f, 18f), FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).toList());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(N.asList(15f, 18f), FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).skip(1).toList());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(3, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).count());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(2, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).skip(1).count());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertArrayEquals(new float[] { 12, 15, 18 }, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertArrayEquals(new float[] { 15, 18 }, FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).skip(1).toArray(), 0.001f);
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(N.asList(12f, 15f, 18f), FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).toList());
        streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5, 6), FloatStream.of(7, 8, 9));
        assertEquals(N.asList(15f, 18f), FloatStream.zip(streams, arr -> arr[0] + arr[1] + arr[2]).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        assertEquals(6, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        assertEquals(5, FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 },
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        assertEquals(9, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        assertEquals(8, FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f),
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        a = FloatIterator.of(new float[] { 1, 3, 5 });
        b = FloatIterator.of(new float[] { 2, 4, 6 });
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f),
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f), FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f),
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        a = FloatStream.of(1, 3, 5);
        b = FloatStream.of(2, 4, 6);
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f),
                FloatStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        a = FloatStream.of(1, 4, 7);
        b = FloatStream.of(2, 5, 8);
        c = FloatStream.of(3, 6, 9);
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        streams = Arrays.asList(FloatStream.of(1, 4, 7), FloatStream.of(2, 5, 8), FloatStream.of(3, 6, 9));
        assertEquals(N.asList(2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
                FloatStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipArraysWithDefaults() {
        float[] a = { 1, 2 };
        float[] b = { 4, 5, 6 };
        assertEquals(3, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).count());
        assertEquals(2, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).skip(1).count());
        assertArrayEquals(new float[] { 5, 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(5f, 7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).toList());
        assertEquals(N.asList(7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).skip(1).toList());
        assertEquals(3, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).count());
        assertEquals(2, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 5, 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 6 }, FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(5f, 7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).toList());
        assertEquals(N.asList(7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(12f, 7f, 6f), FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).toList());
        assertEquals(N.asList(7f, 6f), FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).skip(1).toList());
        assertEquals(3, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).count());
        assertEquals(2, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).skip(1).count());
        assertArrayEquals(new float[] { 12, 7, 6 }, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).toArray(), 0.001f);
        assertArrayEquals(new float[] { 7, 6 }, FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).skip(1).toArray(), 0.001f);
        assertEquals(N.asList(12f, 7f, 6f), FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).toList());
        assertEquals(N.asList(7f, 6f), FloatStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(5f, 7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).toList());
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.asList(7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).skip(1).toList());
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
        assertEquals(N.asList(5f, 7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).toList());
        a = FloatIterator.of(new float[] { 1, 2 });
        b = FloatIterator.of(new float[] { 4, 5, 6 });
        assertEquals(N.asList(7f, 6f), FloatStream.zip(a, b, 0, 0, (x, y) -> x + y).map(e -> e).skip(1).toList());
    }

}
