package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Comparators;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IndexedDouble;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.u.OptionalDouble;

@Tag("new-test")
public class DoubleStream104Test extends TestBase {
    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toList());
        assertEquals(N.asList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.asList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilterWithAction() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).toList());
        assertEquals(N.asList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> {
        }).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).toList());
        assertEquals(N.asList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).count());
        assertEquals(1, DoubleStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new double[] { 1, 2 }, DoubleStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toArray(), 0.0);
        assertArrayEquals(new double[] { 2 }, DoubleStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0), DoubleStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toList());
        assertEquals(N.asList(2.0), DoubleStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toList());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).count());
        assertEquals(1, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new double[] { 1, 2 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toArray(), 0.0);
        assertArrayEquals(new double[] { 2 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toList());
        assertEquals(N.asList(2.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toList());
        assertEquals(N.asList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toList());
        assertEquals(N.asList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhileWithAction() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).toList());
        assertEquals(N.asList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).toList());
        assertEquals(N.asList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).toList());
        assertEquals(N.asList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).toList());
        assertEquals(N.asList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(3, DoubleStream.of(1, 2, 2, 3, 3).distinct().count());
        assertEquals(2, DoubleStream.of(1, 2, 2, 3, 3).distinct().skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3 }, DoubleStream.of(1, 2, 2, 3, 3).distinct().toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.of(1, 2, 2, 3, 3).distinct().skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0), DoubleStream.of(1, 2, 2, 3, 3).distinct().toList());
        assertEquals(N.asList(2.0, 3.0), DoubleStream.of(1, 2, 2, 3, 3).distinct().skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 2, 3, 3).map(e -> e).distinct().count());
        assertEquals(2, DoubleStream.of(1, 2, 2, 3, 3).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3 }, DoubleStream.of(1, 2, 2, 3, 3).map(e -> e).distinct().toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.of(1, 2, 2, 3, 3).map(e -> e).distinct().skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0), DoubleStream.of(1, 2, 2, 3, 3).map(e -> e).distinct().toList());
        assertEquals(N.asList(2.0, 3.0), DoubleStream.of(1, 2, 2, 3, 3).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, DoubleStream.of(3, 1, 4, 2, 5).sorted().count());
        assertEquals(4, DoubleStream.of(3, 1, 4, 2, 5).sorted().skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(3, 1, 4, 2, 5).sorted().toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(3, 1, 4, 2, 5).sorted().skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(3, 1, 4, 2, 5).sorted().toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(3, 1, 4, 2, 5).sorted().skip(1).toList());
        assertEquals(5, DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().count());
        assertEquals(4, DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, DoubleStream.of(3, 1, 4, 2, 5).reverseSorted().count());
        assertEquals(4, DoubleStream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).count());
        assertArrayEquals(new double[] { 5, 4, 3, 2, 1 }, DoubleStream.of(3, 1, 4, 2, 5).reverseSorted().toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 3, 2, 1 }, DoubleStream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).toArray(), 0.0);
        assertEquals(N.asList(5.0, 4.0, 3.0, 2.0, 1.0), DoubleStream.of(3, 1, 4, 2, 5).reverseSorted().toList());
        assertEquals(N.asList(4.0, 3.0, 2.0, 1.0), DoubleStream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).toList());
        assertEquals(5, DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().count());
        assertEquals(4, DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new double[] { 5, 4, 3, 2, 1 }, DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 3, 2, 1 }, DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).toArray(), 0.0);
        assertEquals(N.asList(5.0, 4.0, 3.0, 2.0, 1.0), DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().toList());
        assertEquals(N.asList(4.0, 3.0, 2.0, 1.0), DoubleStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(i -> i * 2).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).count());
        assertArrayEquals(new double[] { 2, 4, 6, 8, 10 }, DoubleStream.of(1, 2, 3, 4, 5).map(i -> i * 2).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 6, 8, 10 }, DoubleStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).toArray(), 0.0);
        assertEquals(N.asList(2.0, 4.0, 6.0, 8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).map(i -> i * 2).toList());
        assertEquals(N.asList(4.0, 6.0, 8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).count());
        assertArrayEquals(new double[] { 2, 4, 6, 8, 10 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 6, 8, 10 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).toArray(), 0.0);
        assertEquals(N.asList(2.0, 4.0, 6.0, 8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).toList());
        assertEquals(N.asList(4.0, 6.0, 8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToInt() {
        assertEquals(5, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToInt(d -> (int) d).count());
        assertEquals(4, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToInt(d -> (int) d).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToInt(d -> (int) d).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToInt(d -> (int) d).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToInt(d -> (int) d).toList());
        assertEquals(N.asList(2, 3, 4, 5), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToInt(d -> (int) d).skip(1).toList());
        assertEquals(5, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToInt(d -> (int) d).count());
        assertEquals(4, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToInt(d -> (int) d).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToInt(d -> (int) d).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToInt(d -> (int) d).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToInt(d -> (int) d).toList());
        assertEquals(N.asList(2, 3, 4, 5), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToInt(d -> (int) d).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToLong() {
        assertEquals(5, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToLong(d -> (long) d).count());
        assertEquals(4, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToLong(d -> (long) d).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToLong(d -> (long) d).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToLong(d -> (long) d).skip(1).toArray());
        assertEquals(N.asList(1L, 2L, 3L, 4L, 5L), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToLong(d -> (long) d).toList());
        assertEquals(N.asList(2L, 3L, 4L, 5L), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToLong(d -> (long) d).skip(1).toList());
        assertEquals(5, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToLong(d -> (long) d).count());
        assertEquals(4, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToLong(d -> (long) d).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToLong(d -> (long) d).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToLong(d -> (long) d).skip(1).toArray());
        assertEquals(N.asList(1L, 2L, 3L, 4L, 5L), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToLong(d -> (long) d).toList());
        assertEquals(N.asList(2L, 3L, 4L, 5L), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToLong(d -> (long) d).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToFloat() {
        assertEquals(5, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToFloat(d -> (float) d).count());
        assertEquals(4, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToFloat(d -> (float) d).skip(1).count());
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToFloat(d -> (float) d).toArray(), 0.01f);
        assertArrayEquals(new float[] { 2.2f, 3.3f, 4.4f, 5.5f }, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToFloat(d -> (float) d).skip(1).toArray(), 0.01f);
        assertEquals(N.asList(1.1f, 2.2f, 3.3f, 4.4f, 5.5f), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToFloat(d -> (float) d).toList());
        assertEquals(N.asList(2.2f, 3.3f, 4.4f, 5.5f), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).mapToFloat(d -> (float) d).skip(1).toList());
        assertEquals(5, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToFloat(d -> (float) d).count());
        assertEquals(4, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToFloat(d -> (float) d).skip(1).count());
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f },
                DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToFloat(d -> (float) d).toArray(), 0.01f);
        assertArrayEquals(new float[] { 2.2f, 3.3f, 4.4f, 5.5f },
                DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToFloat(d -> (float) d).skip(1).toArray(), 0.01f);
        assertEquals(N.asList(1.1f, 2.2f, 3.3f, 4.4f, 5.5f), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToFloat(d -> (float) d).toList());
        assertEquals(N.asList(2.2f, 3.3f, 4.4f, 5.5f), DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).map(e -> e).mapToFloat(d -> (float) d).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToObj() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).mapToObj(d -> "num" + d).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).mapToObj(d -> "num" + d).skip(1).count());
        assertArrayEquals(new String[] { "num1.0", "num2.0", "num3.0", "num4.0", "num5.0" }, DoubleStream.of(1, 2, 3, 4, 5).mapToObj(d -> "num" + d).toArray());
        assertArrayEquals(new String[] { "num2.0", "num3.0", "num4.0", "num5.0" }, DoubleStream.of(1, 2, 3, 4, 5).mapToObj(d -> "num" + d).skip(1).toArray());
        assertEquals(N.asList("num1.0", "num2.0", "num3.0", "num4.0", "num5.0"), DoubleStream.of(1, 2, 3, 4, 5).mapToObj(d -> "num" + d).toList());
        assertEquals(N.asList("num2.0", "num3.0", "num4.0", "num5.0"), DoubleStream.of(1, 2, 3, 4, 5).mapToObj(d -> "num" + d).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(d -> "num" + d).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(d -> "num" + d).skip(1).count());
        assertArrayEquals(new String[] { "num1.0", "num2.0", "num3.0", "num4.0", "num5.0" },
                DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(d -> "num" + d).toArray());
        assertArrayEquals(new String[] { "num2.0", "num3.0", "num4.0", "num5.0" },
                DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(d -> "num" + d).skip(1).toArray());
        assertEquals(N.asList("num1.0", "num2.0", "num3.0", "num4.0", "num5.0"), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(d -> "num" + d).toList());
        assertEquals(N.asList("num2.0", "num3.0", "num4.0", "num5.0"), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(d -> "num" + d).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flatMap(d -> DoubleStream.of(d, d + 0.5)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flatMap(d -> DoubleStream.of(d, d + 0.5)).skip(2).count());
        assertArrayEquals(new double[] { 1, 1.5, 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).flatMap(d -> DoubleStream.of(d, d + 0.5)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).flatMap(d -> DoubleStream.of(d, d + 0.5)).skip(2).toArray(), 0.0);
        assertEquals(N.asList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).flatMap(d -> DoubleStream.of(d, d + 0.5)).toList());
        assertEquals(N.asList(2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).flatMap(d -> DoubleStream.of(d, d + 0.5)).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flatMap(d -> DoubleStream.of(d, d + 0.5)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flatMap(d -> DoubleStream.of(d, d + 0.5)).skip(2).count());
        assertArrayEquals(new double[] { 1, 1.5, 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).map(e -> e).flatMap(d -> DoubleStream.of(d, d + 0.5)).toArray(),
                0.0);
        assertArrayEquals(new double[] { 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).map(e -> e).flatMap(d -> DoubleStream.of(d, d + 0.5)).skip(2).toArray(),
                0.0);
        assertEquals(N.asList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).map(e -> e).flatMap(d -> DoubleStream.of(d, d + 0.5)).toList());
        assertEquals(N.asList(2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).map(e -> e).flatMap(d -> DoubleStream.of(d, d + 0.5)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmap() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flatmap(d -> new double[] { d, d + 0.5 }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flatmap(d -> new double[] { d, d + 0.5 }).skip(2).count());
        assertArrayEquals(new double[] { 1, 1.5, 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).flatmap(d -> new double[] { d, d + 0.5 }).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).flatmap(d -> new double[] { d, d + 0.5 }).skip(2).toArray(), 0.0);
        assertEquals(N.asList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).flatmap(d -> new double[] { d, d + 0.5 }).toList());
        assertEquals(N.asList(2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).flatmap(d -> new double[] { d, d + 0.5 }).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flatmap(d -> new double[] { d, d + 0.5 }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flatmap(d -> new double[] { d, d + 0.5 }).skip(2).count());
        assertArrayEquals(new double[] { 1, 1.5, 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).map(e -> e).flatmap(d -> new double[] { d, d + 0.5 }).toArray(),
                0.0);
        assertArrayEquals(new double[] { 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).map(e -> e).flatmap(d -> new double[] { d, d + 0.5 }).skip(2).toArray(),
                0.0);
        assertEquals(N.asList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).map(e -> e).flatmap(d -> new double[] { d, d + 0.5 }).toList());
        assertEquals(N.asList(2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).map(e -> e).flatmap(d -> new double[] { d, d + 0.5 }).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattMap() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).skip(2).count());
        assertArrayEquals(new double[] { 1, 1.5, 2, 2.5, 3, 3.5 },
                DoubleStream.of(1, 2, 3).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 2.5, 3, 3.5 },
                DoubleStream.of(1, 2, 3).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).skip(2).toArray(), 0.0);
        assertEquals(N.asList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).toList());
        assertEquals(N.asList(2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).skip(2).count());
        assertArrayEquals(new double[] { 1, 1.5, 2.0, 2.5, 3.0, 3.5 },
                DoubleStream.of(1, 2, 3).map(e -> e).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 2.5, 3, 3.5 },
                DoubleStream.of(1, 2, 3).map(e -> e).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).skip(2).toArray(), 0.0);
        assertEquals(N.asList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5),
                DoubleStream.of(1, 2, 3).map(e -> e).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).toList());
        assertEquals(N.asList(2.0, 2.5, 3.0, 3.5),
                DoubleStream.of(1, 2, 3).map(e -> e).flattMap(d -> java.util.stream.DoubleStream.of(d, d + 0.5)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMapMulti() {
        assertEquals(6, DoubleStream.of(1, 2, 3).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).skip(2).count());
        assertArrayEquals(new double[] { 1, 1.5, 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).skip(2).toArray(), 0.0);
        assertEquals(N.asList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).toList());
        assertEquals(N.asList(2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).skip(2).count());
        assertArrayEquals(new double[] { 1, 1.5, 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).map(e -> e).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 2.5, 3, 3.5 }, DoubleStream.of(1, 2, 3).map(e -> e).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).skip(2).toArray(), 0.0);
        assertEquals(N.asList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).map(e -> e).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).toList());
        assertEquals(N.asList(2.0, 2.5, 3.0, 3.5), DoubleStream.of(1, 2, 3).map(e -> e).mapMulti((d, consumer) -> {
            consumer.accept(d);
            consumer.accept(d + 0.5);
        }).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartial() {
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).count());
        assertEquals(1, DoubleStream.of(1, 2, 3, 4, 5).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).skip(1).count());
        assertArrayEquals(new double[] { 8, 10 },
                DoubleStream.of(1, 2, 3, 4, 5).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).toArray(), 0.0);
        assertArrayEquals(new double[] { 10 },
                DoubleStream.of(1, 2, 3, 4, 5).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).skip(1).toArray(), 0.0);
        assertEquals(N.asList(8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).toList());
        assertEquals(N.asList(10.0),
                DoubleStream.of(1, 2, 3, 4, 5).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).skip(1).toList());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).count());
        assertEquals(1, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).skip(1).count());
        assertArrayEquals(new double[] { 8, 10 },
                DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).toArray(), 0.0);
        assertArrayEquals(new double[] { 10 },
                DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).skip(1).toArray(), 0.0);
        assertEquals(N.asList(8.0, 10.0),
                DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).toList());
        assertEquals(N.asList(10.0),
                DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(d -> d > 3 ? OptionalDouble.of(d * 2) : OptionalDouble.empty()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartialJdk() {
        assertEquals(2,
                DoubleStream.of(1, 2, 3, 4, 5).mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty()).count());
        assertEquals(1,
                DoubleStream.of(1, 2, 3, 4, 5)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .skip(1)
                        .count());
        assertArrayEquals(new double[] { 8, 10 },
                DoubleStream.of(1, 2, 3, 4, 5).mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty()).toArray(),
                0.0);
        assertArrayEquals(new double[] { 10 },
                DoubleStream.of(1, 2, 3, 4, 5)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .skip(1)
                        .toArray(),
                0.0);
        assertEquals(N.asList(8.0, 10.0),
                DoubleStream.of(1, 2, 3, 4, 5).mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty()).toList());
        assertEquals(N.asList(10.0),
                DoubleStream.of(1, 2, 3, 4, 5)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .skip(1)
                        .toList());
        assertEquals(2,
                DoubleStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .count());
        assertEquals(1,
                DoubleStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .skip(1)
                        .count());
        assertArrayEquals(new double[] { 8, 10 },
                DoubleStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .toArray(),
                0.0);
        assertArrayEquals(new double[] { 10 },
                DoubleStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .skip(1)
                        .toArray(),
                0.0);
        assertEquals(N.asList(8.0, 10.0),
                DoubleStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .toList());
        assertEquals(N.asList(10.0),
                DoubleStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(d -> d > 3 ? java.util.OptionalDouble.of(d * 2) : java.util.OptionalDouble.empty())
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterRangeMap() {
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 3, 11, 20 }, DoubleStream.of(1, 2, 5, 6, 10).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 11, 20 }, DoubleStream.of(1, 2, 5, 6, 10).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 11.0, 20.0), DoubleStream.of(1, 2, 5, 6, 10).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).toList());
        assertEquals(N.asList(11.0, 20.0), DoubleStream.of(1, 2, 5, 6, 10).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 3, 11, 20 }, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).toArray(),
                0.0);
        assertArrayEquals(new double[] { 11, 20 },
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 11.0, 20.0), DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).toList());
        assertEquals(N.asList(11.0, 20.0), DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRangeMapToObj() {
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").skip(1).count());
        assertArrayEquals(new String[] { "range[1.0-2.0]", "range[5.0-6.0]", "range[10.0-10.0]" },
                DoubleStream.of(1, 2, 5, 6, 10).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").toArray());
        assertArrayEquals(new String[] { "range[5.0-6.0]", "range[10.0-10.0]" },
                DoubleStream.of(1, 2, 5, 6, 10).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").skip(1).toArray());
        assertEquals(N.asList("range[1.0-2.0]", "range[5.0-6.0]", "range[10.0-10.0]"),
                DoubleStream.of(1, 2, 5, 6, 10).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").toList());
        assertEquals(N.asList("range[5.0-6.0]", "range[10.0-10.0]"),
                DoubleStream.of(1, 2, 5, 6, 10).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").count());
        assertEquals(2,
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").skip(1).count());
        assertArrayEquals(new String[] { "range[1.0-2.0]", "range[5.0-6.0]", "range[10.0-10.0]" },
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").toArray());
        assertArrayEquals(new String[] { "range[5.0-6.0]", "range[10.0-10.0]" },
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").skip(1).toArray());
        assertEquals(N.asList("range[1.0-2.0]", "range[5.0-6.0]", "range[10.0-10.0]"),
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").toList());
        assertEquals(N.asList("range[5.0-6.0]", "range[10.0-10.0]"),
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).rangeMapToObj((a, b) -> b - a <= 1, (a, b) -> "range[" + a + "-" + b + "]").skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCollapse() {
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1).count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1).skip(1).count());
        Stream<DoubleList> result = DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1);
        List<DoubleList> list = result.toList();
        assertEquals(3, list.size());
        assertEquals(DoubleList.of(1, 2), list.get(0));
        assertEquals(DoubleList.of(5, 6), list.get(1));
        assertEquals(DoubleList.of(10), list.get(2));

        result = DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1).skip(1);
        list = result.toList();
        assertEquals(2, list.size());
        assertEquals(DoubleList.of(5, 6), list.get(0));
        assertEquals(DoubleList.of(10), list.get(1));

        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1).count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1).skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithMerge() {
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 3, 11, 10 }, DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 11, 10 }, DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 11.0, 10.0), DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).toList());
        assertEquals(N.asList(11.0, 10.0), DoubleStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 3, 11, 10 }, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).toArray(),
                0.0);
        assertArrayEquals(new double[] { 11, 10 },
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 11.0, 10.0), DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).toList());
        assertEquals(N.asList(11.0, 10.0), DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithTriPredicate() {
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 3, 11, 10 },
                DoubleStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 11, 10 },
                DoubleStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 11.0, 10.0), DoubleStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toList());
        assertEquals(N.asList(11.0, 10.0),
                DoubleStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 3, 11, 10 },
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 11, 10 },
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 11.0, 10.0),
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toList());
        assertEquals(N.asList(11.0, 10.0),
                DoubleStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScan() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 1, 3, 6, 10, 15 }, DoubleStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 6, 10, 15 }, DoubleStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 3.0, 6.0, 10.0, 15.0), DoubleStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toList());
        assertEquals(N.asList(3.0, 6.0, 10.0, 15.0), DoubleStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 1, 3, 6, 10, 15 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 6, 10, 15 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 3.0, 6.0, 10.0, 15.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toList());
        assertEquals(N.asList(3.0, 6.0, 10.0, 15.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInit() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 11, 13, 16, 20, 25 }, DoubleStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 13, 16, 20, 25 }, DoubleStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.asList(11.0, 13.0, 16.0, 20.0, 25.0), DoubleStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toList());
        assertEquals(N.asList(13.0, 16.0, 20.0, 25.0), DoubleStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 11, 13, 16, 20, 25 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 13, 16, 20, 25 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.asList(11.0, 13.0, 16.0, 20.0, 25.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toList());
        assertEquals(N.asList(13.0, 16.0, 20.0, 25.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInitIncluded() {
        assertEquals(6, DoubleStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 10, 11, 13, 16, 20, 25 }, DoubleStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 11, 13, 16, 20, 25 }, DoubleStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.asList(10.0, 11.0, 13.0, 16.0, 20.0, 25.0), DoubleStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(N.asList(11.0, 13.0, 16.0, 20.0, 25.0), DoubleStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 10, 11, 13, 16, 20, 25 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 11, 13, 16, 20, 25 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toArray(),
                0.0);
        assertEquals(N.asList(10.0, 11.0, 13.0, 16.0, 20.0, 25.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(N.asList(11.0, 13.0, 16.0, 20.0, 25.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToInt() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).skip(2).count());
        assertArrayEquals(new int[] { 1, 2, 2, 3, 3, 4 }, DoubleStream.of(1, 2, 3).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).toArray());
        assertArrayEquals(new int[] { 2, 3, 3, 4 }, DoubleStream.of(1, 2, 3).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).skip(2).toArray());
        assertEquals(N.asList(1, 2, 2, 3, 3, 4), DoubleStream.of(1, 2, 3).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).toList());
        assertEquals(N.asList(2, 3, 3, 4), DoubleStream.of(1, 2, 3).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).skip(2).count());
        assertArrayEquals(new int[] { 1, 2, 2, 3, 3, 4 }, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).toArray());
        assertArrayEquals(new int[] { 2, 3, 3, 4 },
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).skip(2).toArray());
        assertEquals(N.asList(1, 2, 2, 3, 3, 4), DoubleStream.of(1, 2, 3).map(e -> e).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).toList());
        assertEquals(N.asList(2, 3, 3, 4), DoubleStream.of(1, 2, 3).map(e -> e).flatMapToInt(d -> IntStream.of((int) d, (int) d + 1)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToLong() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).skip(2).count());
        assertArrayEquals(new long[] { 1, 2, 2, 3, 3, 4 }, DoubleStream.of(1, 2, 3).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).toArray());
        assertArrayEquals(new long[] { 2, 3, 3, 4 }, DoubleStream.of(1, 2, 3).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).skip(2).toArray());
        assertEquals(N.asList(1L, 2L, 2L, 3L, 3L, 4L), DoubleStream.of(1, 2, 3).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).toList());
        assertEquals(N.asList(2L, 3L, 3L, 4L), DoubleStream.of(1, 2, 3).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).skip(2).count());
        assertArrayEquals(new long[] { 1, 2, 2, 3, 3, 4 },
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).toArray());
        assertArrayEquals(new long[] { 2, 3, 3, 4 },
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).skip(2).toArray());
        assertEquals(N.asList(1L, 2L, 2L, 3L, 3L, 4L), DoubleStream.of(1, 2, 3).map(e -> e).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).toList());
        assertEquals(N.asList(2L, 3L, 3L, 4L), DoubleStream.of(1, 2, 3).map(e -> e).flatMapToLong(d -> LongStream.of((long) d, (long) d + 1)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToFloat() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).skip(2).count());
        assertArrayEquals(new float[] { 1f, 1.5f, 2f, 2.5f, 3f, 3.5f },
                DoubleStream.of(1, 2, 3).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).toArray(), 0.01f);
        assertArrayEquals(new float[] { 2f, 2.5f, 3f, 3.5f },
                DoubleStream.of(1, 2, 3).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).skip(2).toArray(), 0.01f);
        assertEquals(N.asList(1f, 1.5f, 2f, 2.5f, 3f, 3.5f),
                DoubleStream.of(1, 2, 3).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).toList());
        assertEquals(N.asList(2f, 2.5f, 3f, 3.5f), DoubleStream.of(1, 2, 3).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).skip(2).count());
        assertArrayEquals(new float[] { 1f, 1.5f, 2f, 2.5f, 3f, 3.5f },
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).toArray(), 0.01f);
        assertArrayEquals(new float[] { 2f, 2.5f, 3f, 3.5f },
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).skip(2).toArray(), 0.01f);
        assertEquals(N.asList(1f, 1.5f, 2f, 2.5f, 3f, 3.5f),
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).toList());
        assertEquals(N.asList(2f, 2.5f, 3f, 3.5f),
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToFloat(d -> FloatStream.of((float) d, (float) d + 0.5f)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToObj() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).skip(2).count());
        assertArrayEquals(new String[] { "a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).toArray());
        assertArrayEquals(new String[] { "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).skip(2).toArray());
        assertEquals(N.asList("a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).toList());
        assertEquals(N.asList("a2.0", "b2.0", "a3.0", "b3.0"), DoubleStream.of(1, 2, 3).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).skip(2).count());
        assertArrayEquals(new String[] { "a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).toArray());
        assertArrayEquals(new String[] { "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).skip(2).toArray());
        assertEquals(N.asList("a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).toList());
        assertEquals(N.asList("a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).map(e -> e).flatMapToObj(d -> Stream.of("a" + d, "b" + d)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapToObj() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).skip(2).count());
        assertArrayEquals(new String[] { "a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).toArray());
        assertArrayEquals(new String[] { "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).skip(2).toArray());
        assertEquals(N.asList("a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).toList());
        assertEquals(N.asList("a2.0", "b2.0", "a3.0", "b3.0"), DoubleStream.of(1, 2, 3).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).skip(2).count());
        assertArrayEquals(new String[] { "a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).map(e -> e).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).toArray());
        assertArrayEquals(new String[] { "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).map(e -> e).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).skip(2).toArray());
        assertEquals(N.asList("a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).map(e -> e).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).toList());
        assertEquals(N.asList("a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).map(e -> e).flatmapToObj(d -> Arrays.asList("a" + d, "b" + d)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattmapToObj() {
        assertEquals(6, DoubleStream.of(1, 2, 3).flattmapToObj(d -> new String[] { "a" + d, "b" + d }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).flattmapToObj(d -> new String[] { "a" + d, "b" + d }).skip(2).count());
        assertArrayEquals(new String[] { "a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).flattmapToObj(d -> new String[] { "a" + d, "b" + d }).toArray());
        assertArrayEquals(new String[] { "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).flattmapToObj(d -> new String[] { "a" + d, "b" + d }).skip(2).toArray());
        assertEquals(N.asList("a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).flattmapToObj(d -> new String[] { "a" + d, "b" + d }).toList());
        assertEquals(N.asList("a2.0", "b2.0", "a3.0", "b3.0"), DoubleStream.of(1, 2, 3).flattmapToObj(d -> new String[] { "a" + d, "b" + d }).skip(2).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).flattmapToObj(d -> new String[] { "a" + d, "b" + d }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3).map(e -> e).flattmapToObj(d -> new String[] { "a" + d, "b" + d }).skip(2).count());
        assertArrayEquals(new String[] { "a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).map(e -> e).flattmapToObj(d -> new String[] { "a" + d, "b" + d }).toArray());
        assertArrayEquals(new String[] { "a2.0", "b2.0", "a3.0", "b3.0" },
                DoubleStream.of(1, 2, 3).map(e -> e).flattmapToObj(d -> new String[] { "a" + d, "b" + d }).skip(2).toArray());
        assertEquals(N.asList("a1.0", "b1.0", "a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).map(e -> e).flattmapToObj(d -> new String[] { "a" + d, "b" + d }).toList());
        assertEquals(N.asList("a2.0", "b2.0", "a3.0", "b3.0"),
                DoubleStream.of(1, 2, 3).map(e -> e).flattmapToObj(d -> new String[] { "a" + d, "b" + d }).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterIntersection() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).count());
        assertArrayEquals(new double[] { 2, 3, 4 }, DoubleStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 4 }, DoubleStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(2.0, 3.0, 4.0), DoubleStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toList());
        assertEquals(N.asList(3.0, 4.0), DoubleStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).count());
        assertArrayEquals(new double[] { 2, 3, 4 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 4 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toArray(),
                0.0);
        assertEquals(N.asList(2.0, 3.0, 4.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toList());
        assertEquals(N.asList(3.0, 4.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDifference() {
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).count());
        assertEquals(1, DoubleStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).count());
        assertArrayEquals(new double[] { 1, 5 }, DoubleStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toArray(), 0.0);
        assertArrayEquals(new double[] { 5 }, DoubleStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toList());
        assertEquals(N.asList(5.0), DoubleStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toList());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).count());
        assertEquals(1, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).count());
        assertArrayEquals(new double[] { 1, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toArray(), 0.0);
        assertArrayEquals(new double[] { 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toList());
        assertEquals(N.asList(5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSymmetricDifference() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).count());
        assertArrayEquals(new double[] { 1, 5, 6 }, DoubleStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toArray(), 0.0);
        assertArrayEquals(new double[] { 5, 6 }, DoubleStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 5.0, 6.0), DoubleStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toList());
        assertEquals(N.asList(5.0, 6.0), DoubleStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).count());
        assertArrayEquals(new double[] { 1, 5, 6 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toArray(),
                0.0);
        assertArrayEquals(new double[] { 5, 6 },
                DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 5.0, 6.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).toList());
        assertEquals(N.asList(5.0, 6.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2.0, 3.0, 4.0, 6.0)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReversed() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).reversed().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).reversed().skip(1).count());
        assertArrayEquals(new double[] { 5, 4, 3, 2, 1 }, DoubleStream.of(1, 2, 3, 4, 5).reversed().toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 3, 2, 1 }, DoubleStream.of(1, 2, 3, 4, 5).reversed().skip(1).toArray(), 0.0);
        assertEquals(N.asList(5.0, 4.0, 3.0, 2.0, 1.0), DoubleStream.of(1, 2, 3, 4, 5).reversed().toList());
        assertEquals(N.asList(4.0, 3.0, 2.0, 1.0), DoubleStream.of(1, 2, 3, 4, 5).reversed().skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).count());
        assertArrayEquals(new double[] { 5, 4, 3, 2, 1 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 3, 2, 1 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toArray(), 0.0);
        assertEquals(N.asList(5.0, 4.0, 3.0, 2.0, 1.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toList());
        assertEquals(N.asList(4.0, 3.0, 2.0, 1.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTop() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).top(5, Comparators.naturalOrder()).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).top(5, Comparators.naturalOrder()).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).top(5, Comparators.naturalOrder()).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).top(5, Comparators.naturalOrder()).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).top(5, Comparators.naturalOrder()).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).top(5, Comparators.naturalOrder()).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).top(5, Comparators.naturalOrder()).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).top(5, Comparators.naturalOrder()).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).top(5, Comparators.naturalOrder()).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).top(5, Comparators.naturalOrder()).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).top(5, Comparators.naturalOrder()).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).top(5, Comparators.naturalOrder()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIfEmpty() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).ifEmpty(Fn.emptyAction()).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).ifEmpty(Fn.emptyAction()).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).ifEmpty(Fn.emptyAction()).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).ifEmpty(Fn.emptyAction()).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).ifEmpty(Fn.emptyAction()).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).ifEmpty(Fn.emptyAction()).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(Fn.emptyAction()).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(Fn.emptyAction()).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(Fn.emptyAction()).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(Fn.emptyAction()).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(Fn.emptyAction()).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(Fn.emptyAction()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> DoubleStream.empty()).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> DoubleStream.empty()).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> DoubleStream.empty()).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> DoubleStream.empty()).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> DoubleStream.empty()).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).appendIfEmpty(() -> DoubleStream.empty()).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> DoubleStream.empty()).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> DoubleStream.empty()).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> DoubleStream.empty()).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> DoubleStream.empty()).skip(1).toArray(),
                0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> DoubleStream.empty()).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(() -> DoubleStream.empty()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRotated() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).rotated(2).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).count());
        assertArrayEquals(new double[] { 4, 5, 1, 2, 3 }, DoubleStream.of(1, 2, 3, 4, 5).rotated(2).toArray(), 0.0);
        assertArrayEquals(new double[] { 5, 1, 2, 3 }, DoubleStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toArray(), 0.0);
        assertEquals(N.asList(4.0, 5.0, 1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3, 4, 5).rotated(2).toList());
        assertEquals(N.asList(5.0, 1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).count());
        assertArrayEquals(new double[] { 4, 5, 1, 2, 3 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toArray(), 0.0);
        assertArrayEquals(new double[] { 5, 1, 2, 3 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toArray(), 0.0);
        assertEquals(N.asList(4.0, 5.0, 1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toList());
        assertEquals(N.asList(5.0, 1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterShuffled() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).shuffled().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).shuffled().skip(1).count());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).shuffled().toArray().length);
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).shuffled().skip(1).toArray().length);
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).shuffled().toList().size());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).shuffled().skip(1).toList().size());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).count());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().toArray().length);
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).toArray().length);
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().toList().size());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).toList().size());
    }

    @Test
    public void testStreamCreatedAfterShuffledWithRandom() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).skip(1).count());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toArray().length);
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).skip(1).toArray().length);
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toList().size());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).skip(1).toList().size());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).skip(1).count());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).toArray().length);
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).skip(1).toArray().length);
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).toList().size());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).skip(1).toList().size());
    }

    @Test
    public void testStreamCreatedAfterCycled() {
        assertEquals(10, DoubleStream.of(1, 2, 3).cycled().limit(10).count());
        assertEquals(8, DoubleStream.of(1, 2, 3).cycled().limit(10).skip(2).count());
        assertArrayEquals(new double[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, DoubleStream.of(1, 2, 3).cycled().limit(10).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 1, 2, 3, 1, 2, 3, 1 }, DoubleStream.of(1, 2, 3).cycled().limit(10).skip(2).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0), DoubleStream.of(1, 2, 3).cycled().limit(10).toList());
        assertEquals(N.asList(3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0), DoubleStream.of(1, 2, 3).cycled().limit(10).skip(2).toList());
        assertEquals(10, DoubleStream.of(1, 2, 3).map(e -> e).cycled().limit(10).count());
        assertEquals(8, DoubleStream.of(1, 2, 3).map(e -> e).cycled().limit(10).skip(2).count());
        assertArrayEquals(new double[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, DoubleStream.of(1, 2, 3).map(e -> e).cycled().limit(10).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 1, 2, 3, 1, 2, 3, 1 }, DoubleStream.of(1, 2, 3).map(e -> e).cycled().limit(10).skip(2).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0), DoubleStream.of(1, 2, 3).map(e -> e).cycled().limit(10).toList());
        assertEquals(N.asList(3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0), DoubleStream.of(1, 2, 3).map(e -> e).cycled().limit(10).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterCycledWithRounds() {
        assertEquals(9, DoubleStream.of(1, 2, 3).cycled(3).count());
        assertEquals(7, DoubleStream.of(1, 2, 3).cycled(3).skip(2).count());
        assertArrayEquals(new double[] { 1, 2, 3, 1, 2, 3, 1, 2, 3 }, DoubleStream.of(1, 2, 3).cycled(3).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 1, 2, 3, 1, 2, 3 }, DoubleStream.of(1, 2, 3).cycled(3).skip(2).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3).cycled(3).toList());
        assertEquals(N.asList(3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3).cycled(3).skip(2).toList());
        assertEquals(9, DoubleStream.of(1, 2, 3).map(e -> e).cycled(3).count());
        assertEquals(7, DoubleStream.of(1, 2, 3).map(e -> e).cycled(3).skip(2).count());
        assertArrayEquals(new double[] { 1, 2, 3, 1, 2, 3, 1, 2, 3 }, DoubleStream.of(1, 2, 3).map(e -> e).cycled(3).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 1, 2, 3, 1, 2, 3 }, DoubleStream.of(1, 2, 3).map(e -> e).cycled(3).skip(2).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3).map(e -> e).cycled(3).toList());
        assertEquals(N.asList(3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3).map(e -> e).cycled(3).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterIndexed() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).indexed().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).indexed().skip(1).count());
        Stream<IndexedDouble> indexedStream = DoubleStream.of(1, 2, 3, 4, 5).indexed();
        List<IndexedDouble> list = indexedStream.toList();
        assertEquals(5, list.size());
        assertEquals(0, list.get(0).index());
        assertEquals(1.0, list.get(0).value(), 0.0);
        assertEquals(4, list.get(4).index());
        assertEquals(5.0, list.get(4).value(), 0.0);

        indexedStream = DoubleStream.of(1, 2, 3, 4, 5).indexed().skip(1);
        list = indexedStream.toList();
        assertEquals(4, list.size());
        assertEquals(1, list.get(0).index());
        assertEquals(2.0, list.get(0).value(), 0.0);

        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).indexed().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).indexed().skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).skip(2).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).skip(2).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).skip(2).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).skip(2).toList());
        assertEquals(N.asList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toList());
        assertEquals(N.asList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipWithAction() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).skip(2, d -> {
        }).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).skip(2, d -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).skip(2, d -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).skip(2, d -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).skip(2, d -> {
        }).toList());
        assertEquals(N.asList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).skip(2, d -> {
        }).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, d -> {
        }).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, d -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, d -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, d -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.asList(3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, d -> {
        }).toList());
        assertEquals(N.asList(4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, d -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterLimit() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).limit(3).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).limit(3).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3 }, DoubleStream.of(1, 2, 3, 4, 5).limit(3).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.of(1, 2, 3, 4, 5).limit(3).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3, 4, 5).limit(3).toList());
        assertEquals(N.asList(2.0, 3.0), DoubleStream.of(1, 2, 3, 4, 5).limit(3).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toList());
        assertEquals(N.asList(2.0, 3.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterStep() {
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5, 6).step(2).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).count());
        assertArrayEquals(new double[] { 1, 3, 5 }, DoubleStream.of(1, 2, 3, 4, 5, 6).step(2).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 5 }, DoubleStream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 3.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5, 6).step(2).toList());
        assertEquals(N.asList(3.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).count());
        assertEquals(2, DoubleStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).count());
        assertArrayEquals(new double[] { 1, 3, 5 }, DoubleStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).toArray(), 0.0);
        assertArrayEquals(new double[] { 3, 5 }, DoubleStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 3.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).toList());
        assertEquals(N.asList(3.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRateLimited() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).rateLimited(100).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).rateLimited(100).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).rateLimited(100).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).rateLimited(100).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).rateLimited(100).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).rateLimited(100).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(100).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(100).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(100).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(100).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(100).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(100).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRateLimitedWithLimiter() {
        RateLimiter limiter = RateLimiter.create(100);
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).rateLimited(limiter).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).rateLimited(RateLimiter.create(100)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).rateLimited(RateLimiter.create(100)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).rateLimited(RateLimiter.create(100)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).rateLimited(RateLimiter.create(100)).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).rateLimited(RateLimiter.create(100)).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(RateLimiter.create(100)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(RateLimiter.create(100)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(RateLimiter.create(100)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(RateLimiter.create(100)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(RateLimiter.create(100)).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).rateLimited(RateLimiter.create(100)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDelay() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).delay(Duration.ofMillis(1)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).delay(Duration.ofMillis(1)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).delay(Duration.ofMillis(1)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).delay(Duration.ofMillis(1)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).delay(Duration.ofMillis(1)).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).delay(Duration.ofMillis(1)).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).delay(Duration.ofMillis(1)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).delay(Duration.ofMillis(1)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).delay(Duration.ofMillis(1)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).delay(Duration.ofMillis(1)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).delay(Duration.ofMillis(1)).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).delay(Duration.ofMillis(1)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnEach() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).onEach(d -> {
        }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).onEach(d -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).onEach(d -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).onEach(d -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).onEach(d -> {
        }).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).onEach(d -> {
        }).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(d -> {
        }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(d -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(d -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(d -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(d -> {
        }).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(d -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPeek() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).peek(d -> {
        }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).peek(d -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).peek(d -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).peek(d -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).peek(d -> {
        }).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).peek(d -> {
        }).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).peek(d -> {
        }).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).peek(d -> {
        }).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).peek(d -> {
        }).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).peek(d -> {
        }).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).peek(d -> {
        }).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).peek(d -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependStream() {
        assertEquals(6, DoubleStream.of(4, 5, 6).prepend(DoubleStream.of(1, 2, 3)).count());
        assertEquals(5, DoubleStream.of(4, 5, 6).prepend(DoubleStream.of(1, 2, 3)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5, 6 }, DoubleStream.of(4, 5, 6).prepend(DoubleStream.of(1, 2, 3)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5, 6 }, DoubleStream.of(4, 5, 6).prepend(DoubleStream.of(1, 2, 3)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(4, 5, 6).prepend(DoubleStream.of(1, 2, 3)).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(4, 5, 6).prepend(DoubleStream.of(1, 2, 3)).skip(1).toList());
        assertEquals(6, DoubleStream.of(4, 5, 6).map(e -> e).prepend(DoubleStream.of(1, 2, 3)).count());
        assertEquals(5, DoubleStream.of(4, 5, 6).map(e -> e).prepend(DoubleStream.of(1, 2, 3)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5, 6 }, DoubleStream.of(4, 5, 6).map(e -> e).prepend(DoubleStream.of(1, 2, 3)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5, 6 }, DoubleStream.of(4, 5, 6).map(e -> e).prepend(DoubleStream.of(1, 2, 3)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(4, 5, 6).map(e -> e).prepend(DoubleStream.of(1, 2, 3)).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(4, 5, 6).map(e -> e).prepend(DoubleStream.of(1, 2, 3)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependOptional() {
        assertEquals(4, DoubleStream.of(2, 3, 4).prepend(OptionalDouble.of(1)).count());
        assertEquals(3, DoubleStream.of(2, 3, 4).prepend(OptionalDouble.of(1)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4 }, DoubleStream.of(2, 3, 4).prepend(OptionalDouble.of(1)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4 }, DoubleStream.of(2, 3, 4).prepend(OptionalDouble.of(1)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0), DoubleStream.of(2, 3, 4).prepend(OptionalDouble.of(1)).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0), DoubleStream.of(2, 3, 4).prepend(OptionalDouble.of(1)).skip(1).toList());
        assertEquals(4, DoubleStream.of(2, 3, 4).map(e -> e).prepend(OptionalDouble.of(1)).count());
        assertEquals(3, DoubleStream.of(2, 3, 4).map(e -> e).prepend(OptionalDouble.of(1)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4 }, DoubleStream.of(2, 3, 4).map(e -> e).prepend(OptionalDouble.of(1)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4 }, DoubleStream.of(2, 3, 4).map(e -> e).prepend(OptionalDouble.of(1)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0), DoubleStream.of(2, 3, 4).map(e -> e).prepend(OptionalDouble.of(1)).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0), DoubleStream.of(2, 3, 4).map(e -> e).prepend(OptionalDouble.of(1)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependArray() {
        assertEquals(6, DoubleStream.of(4, 5, 6).prepend(1, 2, 3).count());
        assertEquals(5, DoubleStream.of(4, 5, 6).prepend(1, 2, 3).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5, 6 }, DoubleStream.of(4, 5, 6).prepend(1, 2, 3).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5, 6 }, DoubleStream.of(4, 5, 6).prepend(1, 2, 3).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(4, 5, 6).prepend(1, 2, 3).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(4, 5, 6).prepend(1, 2, 3).skip(1).toList());
        assertEquals(6, DoubleStream.of(4, 5, 6).map(e -> e).prepend(1, 2, 3).count());
        assertEquals(5, DoubleStream.of(4, 5, 6).map(e -> e).prepend(1, 2, 3).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5, 6 }, DoubleStream.of(4, 5, 6).map(e -> e).prepend(1, 2, 3).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5, 6 }, DoubleStream.of(4, 5, 6).map(e -> e).prepend(1, 2, 3).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(4, 5, 6).map(e -> e).prepend(1, 2, 3).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(4, 5, 6).map(e -> e).prepend(1, 2, 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendStream() {
        assertEquals(6, DoubleStream.of(1, 2, 3).append(DoubleStream.of(4, 5, 6)).count());
        assertEquals(5, DoubleStream.of(1, 2, 3).append(DoubleStream.of(4, 5, 6)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5, 6 }, DoubleStream.of(1, 2, 3).append(DoubleStream.of(4, 5, 6)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5, 6 }, DoubleStream.of(1, 2, 3).append(DoubleStream.of(4, 5, 6)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(1, 2, 3).append(DoubleStream.of(4, 5, 6)).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(1, 2, 3).append(DoubleStream.of(4, 5, 6)).skip(1).toList());
        assertEquals(6, DoubleStream.of(1, 2, 3).map(e -> e).append(DoubleStream.of(4, 5, 6)).count());
        assertEquals(5, DoubleStream.of(1, 2, 3).map(e -> e).append(DoubleStream.of(4, 5, 6)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5, 6 }, DoubleStream.of(1, 2, 3).map(e -> e).append(DoubleStream.of(4, 5, 6)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5, 6 }, DoubleStream.of(1, 2, 3).map(e -> e).append(DoubleStream.of(4, 5, 6)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(1, 2, 3).map(e -> e).append(DoubleStream.of(4, 5, 6)).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0, 6.0), DoubleStream.of(1, 2, 3).map(e -> e).append(DoubleStream.of(4, 5, 6)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeWith() {
        assertEquals(5, DoubleStream.of(1, 3, 5).mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(4,
                DoubleStream.of(1, 3, 5).mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 },
                DoubleStream.of(1, 3, 5).mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 },
                DoubleStream.of(1, 3, 5).mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray(),
                0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0),
                DoubleStream.of(1, 3, 5).mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0),
                DoubleStream.of(1, 3, 5).mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        assertEquals(5,
                DoubleStream.of(1, 3, 5)
                        .map(e -> e)
                        .mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .count());
        assertEquals(4,
                DoubleStream.of(1, 3, 5)
                        .map(e -> e)
                        .mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 },
                DoubleStream.of(1, 3, 5)
                        .map(e -> e)
                        .mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray(),
                0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 },
                DoubleStream.of(1, 3, 5)
                        .map(e -> e)
                        .mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toArray(),
                0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0),
                DoubleStream.of(1, 3, 5)
                        .map(e -> e)
                        .mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0),
                DoubleStream.of(1, 3, 5)
                        .map(e -> e)
                        .mergeWith(DoubleStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterZipWith() {
        assertEquals(3, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 5, 7, 9 }, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 7, 9 }, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.asList(5.0, 7.0, 9.0), DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).toList());
        assertEquals(N.asList(7.0, 9.0), DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 5, 7, 9 }, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 7, 9 }, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).skip(1).toArray(),
                0.0);
        assertEquals(N.asList(5.0, 7.0, 9.0), DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).toList());
        assertEquals(N.asList(7.0, 9.0), DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithThree() {
        assertEquals(3, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).count());
        assertEquals(2, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new double[] { 12, 15, 18 },
                DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).toArray(), 0.0);
        assertArrayEquals(new double[] { 15, 18 },
                DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).skip(1).toArray(), 0.0);
        assertEquals(N.asList(12.0, 15.0, 18.0),
                DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).toList());
        assertEquals(N.asList(15.0, 18.0),
                DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).count());
        assertEquals(2,
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new double[] { 12, 15, 18 },
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).toArray(), 0.0);
        assertArrayEquals(new double[] { 15, 18 },
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).skip(1).toArray(),
                0.0);
        assertEquals(N.asList(12.0, 15.0, 18.0),
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).toList());
        assertEquals(N.asList(15.0, 18.0),
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5, 6), DoubleStream.of(7, 8, 9), (a, b, c) -> a + b + c).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithDefaults() {
        assertEquals(3, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 5, 7, 13 }, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).toArray(), 0.0);
        assertArrayEquals(new double[] { 7, 13 }, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).skip(1).toArray(), 0.0);
        assertEquals(N.asList(5.0, 7.0, 13.0), DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).toList());
        assertEquals(N.asList(7.0, 13.0), DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).count());
        assertEquals(2, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new double[] { 5, 7, 13 }, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).toArray(),
                0.0);
        assertArrayEquals(new double[] { 7, 13 }, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).skip(1).toArray(),
                0.0);
        assertEquals(N.asList(5.0, 7.0, 13.0), DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).toList());
        assertEquals(N.asList(7.0, 13.0), DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), 0, 10, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithThreeDefaults() {
        assertEquals(3, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).count());
        assertEquals(2, DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new double[] { 12, 27.0, 33.0 },
                DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).toArray(), 0.0);
        assertArrayEquals(new double[] { 27.0, 33.0 },
                DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).skip(1).toArray(), 0.0);
        assertEquals(N.asList(12.0, 27.0, 33.0),
                DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).toList());
        assertEquals(N.asList(27.0, 33.0),
                DoubleStream.of(1, 2, 3).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).skip(1).toList());
        assertEquals(3, DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).count());
        assertEquals(2,
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new double[] { 12.0, 27.0, 33.0 },
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).toArray(), 0.0);
        assertArrayEquals(new double[] { 27.0, 33.0 },
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).skip(1).toArray(),
                0.0);
        assertEquals(N.asList(12.0, 27.0, 33.0),
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).toList());
        assertEquals(N.asList(27.0, 33.0),
                DoubleStream.of(1, 2, 3).map(e -> e).zipWith(DoubleStream.of(4, 5), DoubleStream.of(7), 0, 10, 20, (a, b, c) -> a + b + c).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterBoxed() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).boxed().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).boxed().skip(1).count());
        assertArrayEquals(new Double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, DoubleStream.of(1, 2, 3, 4, 5).boxed().toArray());
        assertArrayEquals(new Double[] { 2.0, 3.0, 4.0, 5.0 }, DoubleStream.of(1, 2, 3, 4, 5).boxed().skip(1).toArray());
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).boxed().toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).boxed().skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().skip(1).count());
        assertArrayEquals(new Double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().toArray());
        assertArrayEquals(new Double[] { 2.0, 3.0, 4.0, 5.0 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().skip(1).toArray());
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().toList());
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).boxed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterToJdkStream() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).toJdkStream().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).toJdkStream().skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).toJdkStream().toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).toJdkStream().skip(1).toArray(), 0.0);
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).toJdkStream().count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).toJdkStream().skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).toJdkStream().toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3, 4, 5 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).toJdkStream().skip(1).toArray(), 0.0);
    }

    @Test
    public void testStreamCreatedAfterTransformB() {
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(d -> d * 2)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(d -> d * 2)).skip(1).count());
        assertArrayEquals(new double[] { 2, 4, 6, 8, 10 }, DoubleStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(d -> d * 2)).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 6, 8, 10 }, DoubleStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(d -> d * 2)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(2.0, 4.0, 6.0, 8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(d -> d * 2)).toList());
        assertEquals(N.asList(4.0, 6.0, 8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(d -> d * 2)).skip(1).toList());
        assertEquals(5, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(d -> d * 2)).count());
        assertEquals(4, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(d -> d * 2)).skip(1).count());
        assertArrayEquals(new double[] { 2, 4, 6, 8, 10 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(d -> d * 2)).toArray(), 0.0);
        assertArrayEquals(new double[] { 4, 6, 8, 10 }, DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(d -> d * 2)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(2.0, 4.0, 6.0, 8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(d -> d * 2)).toList());
        assertEquals(N.asList(4.0, 6.0, 8.0, 10.0), DoubleStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(d -> d * 2)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterEmpty() {
        assertEquals(0, DoubleStream.empty().count());
        assertEquals(0, DoubleStream.empty().skip(1).count());
        assertArrayEquals(new double[] {}, DoubleStream.empty().toArray(), 0.0);
        assertArrayEquals(new double[] {}, DoubleStream.empty().skip(1).toArray(), 0.0);
        assertEquals(N.asList(), DoubleStream.empty().toList());
        assertEquals(N.asList(), DoubleStream.empty().skip(1).toList());
        assertEquals(0, DoubleStream.empty().map(e -> e).count());
        assertEquals(0, DoubleStream.empty().map(e -> e).skip(1).count());
        assertArrayEquals(new double[] {}, DoubleStream.empty().map(e -> e).toArray(), 0.0);
        assertArrayEquals(new double[] {}, DoubleStream.empty().map(e -> e).skip(1).toArray(), 0.0);
        assertEquals(N.asList(), DoubleStream.empty().map(e -> e).toList());
        assertEquals(N.asList(), DoubleStream.empty().map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDefer() {
        assertEquals(3, DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).count());
        assertEquals(2, DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3 }, DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0), DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).toList());
        assertEquals(N.asList(2.0, 3.0), DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).skip(1).toList());
        assertEquals(3, DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).map(e -> e).count());
        assertEquals(2, DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).map(e -> e).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3 }, DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).map(e -> e).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).map(e -> e).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0), DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).map(e -> e).toList());
        assertEquals(N.asList(2.0, 3.0), DoubleStream.defer(() -> DoubleStream.of(1, 2, 3)).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFrom() {
        assertEquals(3, DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).count());
        assertEquals(2, DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3 }, DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0), DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).toList());
        assertEquals(N.asList(2.0, 3.0), DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).skip(1).toList());
        assertEquals(3, DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).map(e -> e).count());
        assertEquals(2, DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).map(e -> e).skip(1).count());
        assertArrayEquals(new double[] { 1, 2, 3 }, DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).map(e -> e).toArray(), 0.0);
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).map(e -> e).skip(1).toArray(), 0.0);
        assertEquals(N.asList(1.0, 2.0, 3.0), DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).map(e -> e).toList());
        assertEquals(N.asList(2.0, 3.0), DoubleStream.from(java.util.stream.DoubleStream.of(1, 2, 3)).map(e -> e).skip(1).toList());
    }
}
