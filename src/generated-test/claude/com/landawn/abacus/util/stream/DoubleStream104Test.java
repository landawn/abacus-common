package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IndexedDouble;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.u.OptionalDouble;

public class DoubleStream104Test extends TestBase {// Test 1: filter(DoublePredicate)
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

    // Test 2: filter(DoublePredicate, DoubleConsumer)
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

    // Test 3: takeWhile(DoublePredicate)
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

    // Test 4: dropWhile(DoublePredicate)
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

    // Test 5: dropWhile(DoublePredicate, DoubleConsumer)
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

    // Test 6: skipUntil(DoublePredicate)
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

    // Test 7: distinct()
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

    // Test 8: sorted()
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

    // Test 9: reverseSorted()
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

    // Test 10: map(DoubleUnaryOperator)
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

    // Test 11: mapToInt(DoubleToIntFunction) - returns IntStream
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

    // Test 12: mapToLong(DoubleToLongFunction) - returns LongStream
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

    // Test 13: mapToFloat(DoubleToFloatFunction) - returns FloatStream
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

    // Test 14: mapToObj(DoubleFunction) - returns Stream<T>
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

    // Test 15: flatMap(DoubleFunction<? extends DoubleStream>) - returns DoubleStream
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

    // Test 16: flatmap(DoubleFunction<double[]>) - returns DoubleStream
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

    // Test 17: flattMap(DoubleFunction<? extends java.util.stream.DoubleStream>) - returns DoubleStream
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

    // Test 24: mapMulti(DoubleMapMultiConsumer) - returns DoubleStream
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

    // Test 25: mapPartial(DoubleFunction<OptionalDouble>) - returns DoubleStream
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

    // Test 26: mapPartialJdk(DoubleFunction<java.util.OptionalDouble>) - returns DoubleStream
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

    // Test 27: rangeMap(DoubleBiPredicate, DoubleBinaryOperator) - returns DoubleStream
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

    // Test 28: rangeMapToObj(DoubleBiPredicate, DoubleBiFunction) - returns Stream<T>
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

    // Test 29: collapse(DoubleBiPredicate) - returns Stream<DoubleList>
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

    // Test 30: collapse(DoubleBiPredicate, DoubleBinaryOperator) - returns DoubleStream
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

    // Test 31: collapse(DoubleTriPredicate, DoubleBinaryOperator) - returns DoubleStream
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

    // Test 32: scan(DoubleBinaryOperator) - returns DoubleStream
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

    // Test 33: scan(double, DoubleBinaryOperator) - returns DoubleStream
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

    // Test 34: scan(double, boolean, DoubleBinaryOperator) - returns DoubleStream
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

    // Test 18: flatMapToInt(DoubleFunction<? extends IntStream>) - returns IntStream
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

    // Test 19: flatMapToLong(DoubleFunction<? extends LongStream>) - returns LongStream
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

    // Test 20: flatMapToFloat(DoubleFunction<? extends FloatStream>) - returns FloatStream
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

    // Test 21: flatMapToObj(DoubleFunction<? extends Stream<? extends T>>) - returns Stream<T>
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

    // Test 22: flatmapToObj(DoubleFunction<? extends Collection<? extends T>>) - returns Stream<T>
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

    // Test 23: flattmapToObj(DoubleFunction<T[]>) - returns Stream<T>
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

    // Test 35: intersection(Collection) - returns DoubleStream
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

    // Test 36: difference(Collection) - returns DoubleStream
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

    // Test 37: symmetricDifference(Collection) - returns DoubleStream
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

    // Test 38: reversed() - returns DoubleStream
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

    // Test 39: rotated(int) - returns DoubleStream
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

    // Test 40: shuffled() - returns DoubleStream
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

    // Test 41: shuffled(Random) - returns DoubleStream
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

    // Test 42: cycled() - returns DoubleStream
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

    // Test 43: cycled(long) - returns DoubleStream
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

    // Test 44: indexed() - returns Stream<IndexedDouble>
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

    // Test 45: skip(long) - returns DoubleStream
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

    // Test 46: skip(long, DoubleConsumer) - returns DoubleStream
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

    // Test 47: limit(long) - returns DoubleStream
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

    // Test 48: step(long) - returns DoubleStream
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

    // Test 49: rateLimited(double) - returns DoubleStream
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

    // Test 50: rateLimited(RateLimiter) - returns DoubleStream
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

    // Test 51: delay(Duration) - returns DoubleStream
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

    // Test 52: onEach(DoubleConsumer) - returns DoubleStream
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

    // Test 53: peek(DoubleConsumer) - returns DoubleStream
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

    // Test 54: prepend(DoubleStream) - returns DoubleStream
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

    // Test 55: prepend(OptionalDouble) - returns DoubleStream
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

    // Test 56: prepend(double...) - returns DoubleStream
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

    // Test 57: append(DoubleStream) - returns DoubleStream
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

    // Test 68: mergeWith(DoubleStream, DoubleBiFunction) - returns DoubleStream
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

    // Test 69: zipWith(DoubleStream, DoubleBinaryOperator) - returns DoubleStream
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

    // Test 70: zipWith(DoubleStream, DoubleStream, DoubleTernaryOperator) - returns DoubleStream
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

    // Test 71: zipWith(DoubleStream, double, double, DoubleBinaryOperator) - returns DoubleStream
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

    // Test 72: zipWith(DoubleStream, DoubleStream, double, double, double, DoubleTernaryOperator) - returns DoubleStream
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

    // Test 73: boxed() - returns Stream<Double>
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

    // Test 74: toJdkStream() - returns java.util.stream.DoubleStream
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

    // Test 75: transformB(Function) - returns DoubleStream
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

    // Test 76: empty() - static method returns DoubleStream
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

    // Test 77: defer(Supplier) - static method returns DoubleStream
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

    // Test 78: from(java.util.stream.DoubleStream) - static method returns DoubleStream
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
