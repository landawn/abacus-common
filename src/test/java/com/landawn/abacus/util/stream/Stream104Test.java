package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.Optional;

public class Stream104Test extends TestBase {
    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toList());
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilterWithAction() {
        List<Integer> dropped = new ArrayList<>();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).count());
        dropped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).toList());
        dropped.clear();
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).skip(1).toList());
        dropped.clear();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).count());
        dropped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).toList());
        dropped.clear();
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2 }, Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new Integer[] { 2 }, Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.asList(1, 2), Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toList());
        assertEquals(N.asList(2), Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toList());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new Integer[] { 2 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.asList(1, 2), Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toList());
        assertEquals(N.asList(2), Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toList());
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toList());
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhileWithAction() {
        List<Integer> dropped = new ArrayList<>();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).count());
        dropped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).toList());
        dropped.clear();
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).skip(1).toList());
        dropped.clear();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).count());
        dropped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toList());
        dropped.clear();
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).toList());
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).toList());
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(4, Stream.of(1, 2, 2, 3, 3, 4).distinct().count());
        assertEquals(3, Stream.of(1, 2, 2, 3, 3, 4).distinct().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, Stream.of(1, 2, 2, 3, 3, 4).distinct().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4 }, Stream.of(1, 2, 2, 3, 3, 4).distinct().skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4), Stream.of(1, 2, 2, 3, 3, 4).distinct().toList());
        assertEquals(N.asList(2, 3, 4), Stream.of(1, 2, 2, 3, 3, 4).distinct().skip(1).toList());
        assertEquals(4, Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().count());
        assertEquals(3, Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4 }, Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4), Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().toList());
        assertEquals(N.asList(2, 3, 4), Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIntersection() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 3, 4 }, Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).toArray());
        assertArrayEquals(new Integer[] { 3, 4 }, Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).toArray());
        assertEquals(N.asList(2, 3, 4), Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).toList());
        assertEquals(N.asList(3, 4), Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 3, 4 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).toArray());
        assertArrayEquals(new Integer[] { 3, 4 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).toArray());
        assertEquals(N.asList(2, 3, 4), Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).toList());
        assertEquals(N.asList(3, 4), Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDifference() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 5 }, Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).skip(1).toArray());
        assertEquals(N.asList(1, 5), Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).toList());
        assertEquals(N.asList(5), Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).skip(1).toList());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).skip(1).toArray());
        assertEquals(N.asList(1, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).toList());
        assertEquals(N.asList(5), Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSymmetricDifference() {
        assertEquals(4, Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).count());
        assertEquals(3, Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 4, 5 }, Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).toArray());
        assertArrayEquals(new Integer[] { 2, 4, 5 }, Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).toArray());
        assertEquals(N.asList(1, 2, 4, 5), Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).toList());
        assertEquals(N.asList(2, 4, 5), Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).toList());
        assertEquals(4, Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).count());
        assertEquals(3, Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).toArray());
        assertArrayEquals(new Integer[] { 2, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).toArray());
        assertEquals(N.asList(1, 2, 4, 5), Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).toList());
        assertEquals(N.asList(2, 4, 5), Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReversed() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).reversed().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).reversed().skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reversed().toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reversed().skip(1).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reversed().toList());
        assertEquals(N.asList(4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reversed().skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toList());
        assertEquals(N.asList(4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRotated() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).rotated(2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).rotated(2).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5, 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).rotated(2).toArray());
        assertArrayEquals(new Integer[] { 5, 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toArray());
        assertEquals(N.asList(4, 5, 1, 2, 3), Stream.of(1, 2, 3, 4, 5).rotated(2).toList());
        assertEquals(N.asList(5, 1, 2, 3), Stream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5, 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toArray());
        assertArrayEquals(new Integer[] { 5, 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toArray());
        assertEquals(N.asList(4, 5, 1, 2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toList());
        assertEquals(N.asList(5, 1, 2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterShuffled() {
        // Note: shuffled() produces random order, so we can only test count and that all elements are present
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled().skip(1).count());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled().toArray().length);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled().skip(1).toArray().length);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled().toList().size());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled().skip(1).toList().size());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).count());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().toArray().length);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).toArray().length);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().toList().size());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).toList().size());
    }

    @Test
    public void testStreamCreatedAfterShuffledWithRandom() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).skip(1).count());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toArray().length);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).skip(1).toArray().length);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toList().size());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).skip(1).toList().size());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).skip(1).count());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).toArray().length);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).skip(1).toArray().length);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).toList().size());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).skip(1).toList().size());
    }

    @Test
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, Stream.of(5, 3, 1, 4, 2).sorted().count());
        assertEquals(4, Stream.of(5, 3, 1, 4, 2).sorted().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).sorted().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).sorted().skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).sorted().toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).sorted().skip(1).toList());
        assertEquals(5, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().count());
        assertEquals(4, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, Stream.of(3, 1, 4, 2, 5).reverseSorted().count());
        assertEquals(4, Stream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).reverseSorted().toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).reverseSorted().toList());
        assertEquals(N.asList(4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).toList());
        assertEquals(5, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().count());
        assertEquals(4, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().toList());
        assertEquals(N.asList(4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCycled() {
        assertEquals(15, Stream.of(1, 2, 3).cycled().limit(15).count());
        assertEquals(14, Stream.of(1, 2, 3).cycled().limit(15).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, Stream.of(1, 2, 3).cycled().limit(10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 1, 2, 3, 1, 2, 3, 1 }, Stream.of(1, 2, 3).cycled().limit(10).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 1, 2, 3), Stream.of(1, 2, 3).cycled().limit(6).toList());
        assertEquals(N.asList(2, 3, 1, 2, 3), Stream.of(1, 2, 3).cycled().limit(6).skip(1).toList());
        assertEquals(15, Stream.of(1, 2, 3).map(e -> e).cycled().limit(15).count());
        assertEquals(14, Stream.of(1, 2, 3).map(e -> e).cycled().limit(15).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, Stream.of(1, 2, 3).map(e -> e).cycled().limit(10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 1, 2, 3, 1, 2, 3, 1 }, Stream.of(1, 2, 3).map(e -> e).cycled().limit(10).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 1, 2, 3), Stream.of(1, 2, 3).map(e -> e).cycled().limit(6).toList());
        assertEquals(N.asList(2, 3, 1, 2, 3), Stream.of(1, 2, 3).map(e -> e).cycled().limit(6).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCycledWithRounds() {
        assertEquals(6, Stream.of(1, 2, 3).cycled(2).count());
        assertEquals(5, Stream.of(1, 2, 3).cycled(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 1, 2, 3 }, Stream.of(1, 2, 3).cycled(2).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 1, 2, 3 }, Stream.of(1, 2, 3).cycled(2).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 1, 2, 3), Stream.of(1, 2, 3).cycled(2).toList());
        assertEquals(N.asList(2, 3, 1, 2, 3), Stream.of(1, 2, 3).cycled(2).skip(1).toList());
        assertEquals(6, Stream.of(1, 2, 3).map(e -> e).cycled(2).count());
        assertEquals(5, Stream.of(1, 2, 3).map(e -> e).cycled(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 1, 2, 3 }, Stream.of(1, 2, 3).map(e -> e).cycled(2).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 1, 2, 3 }, Stream.of(1, 2, 3).map(e -> e).cycled(2).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 1, 2, 3), Stream.of(1, 2, 3).map(e -> e).cycled(2).toList());
        assertEquals(N.asList(2, 3, 1, 2, 3), Stream.of(1, 2, 3).map(e -> e).cycled(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).skip(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).skip(2).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).skip(2).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).skip(2).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).skip(2).toList());
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).skip(2).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toList());
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipWithAction() {
        List<Integer> skipped = new ArrayList<>();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).count());
        skipped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).skip(1).count());
        skipped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).toArray());
        skipped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).skip(1).toArray());
        skipped.clear();
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).toList());
        skipped.clear();
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).skip(1).toList());
        skipped.clear();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).count());
        skipped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).skip(1).count());
        skipped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).toArray());
        skipped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).skip(1).toArray());
        skipped.clear();
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).toList());
        skipped.clear();
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterLimit() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).limit(3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).limit(3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).limit(3).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, 2, 3, 4, 5).limit(3).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), Stream.of(1, 2, 3, 4, 5).limit(3).toList());
        assertEquals(N.asList(2, 3), Stream.of(1, 2, 3, 4, 5).limit(3).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toList());
        assertEquals(N.asList(2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterStep() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5, 6).step(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 3, 5 }, Stream.of(1, 2, 3, 4, 5, 6).step(2).toArray());
        assertArrayEquals(new Integer[] { 3, 5 }, Stream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).toArray());
        assertEquals(N.asList(1, 3, 5), Stream.of(1, 2, 3, 4, 5, 6).step(2).toList());
        assertEquals(N.asList(3, 5), Stream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 3, 5 }, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).toArray());
        assertArrayEquals(new Integer[] { 3, 5 }, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).toArray());
        assertEquals(N.asList(1, 3, 5), Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).toList());
        assertEquals(N.asList(3, 5), Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnEach() {
        List<Integer> list = new ArrayList<>();
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).onEach(list::add).count());
        list.clear();
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).onEach(list::add).skip(1).count());
        list.clear();
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).toArray());
        list.clear();
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).skip(1).toArray());
        list.clear();
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).toList());
        list.clear();
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrepend() {
        assertEquals(5, Stream.of(3, 4, 5).prepend(1, 2).count());
        assertEquals(4, Stream.of(3, 4, 5).prepend(1, 2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).prepend(1, 2).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(3, 4, 5).prepend(1, 2).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).prepend(1, 2).toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(3, 4, 5).prepend(1, 2).skip(1).toList());
        assertEquals(5, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).count());
        assertEquals(4, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependCollection() {
        assertEquals(5, Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).count());
        assertEquals(4, Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).skip(1).toList());
        assertEquals(5, Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).count());
        assertEquals(4, Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppend() {
        assertEquals(5, Stream.of(1, 2, 3).append(4, 5).count());
        assertEquals(4, Stream.of(1, 2, 3).append(4, 5).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).append(4, 5).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3).append(4, 5).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).append(4, 5).toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3).append(4, 5).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3).map(e -> e).append(4, 5).count());
        assertEquals(4, Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(4, 5).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(4, 5).toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendCollection() {
        assertEquals(5, Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).count());
        assertEquals(4, Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).count());
        assertEquals(4, Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        assertEquals(3, Stream.<Integer> empty().appendIfEmpty(1, 2, 3).count());
        assertEquals(2, Stream.<Integer> empty().appendIfEmpty(1, 2, 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.<Integer> empty().appendIfEmpty(1, 2, 3).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.<Integer> empty().appendIfEmpty(1, 2, 3).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), Stream.<Integer> empty().appendIfEmpty(1, 2, 3).toList());
        assertEquals(N.asList(2, 3), Stream.<Integer> empty().appendIfEmpty(1, 2, 3).skip(1).toList());
        assertEquals(3, Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).count());
        assertEquals(2, Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).toList());
        assertEquals(N.asList(2, 3), Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).skip(1).toList());

        // Test non-empty stream
        assertEquals(2, Stream.of(4, 5).appendIfEmpty(1, 2, 3).count());
        assertEquals(1, Stream.of(4, 5).appendIfEmpty(1, 2, 3).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(4, 5).appendIfEmpty(1, 2, 3).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.of(4, 5).appendIfEmpty(1, 2, 3).skip(1).toArray());
        assertEquals(N.asList(4, 5), Stream.of(4, 5).appendIfEmpty(1, 2, 3).toList());
        assertEquals(N.asList(5), Stream.of(4, 5).appendIfEmpty(1, 2, 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmptyCollection() {
        assertEquals(3, Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).count());
        assertEquals(2, Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).toList());
        assertEquals(N.asList(2, 3), Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).toList());
        assertEquals(3, Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).count());
        assertEquals(2, Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).toList());
        assertEquals(N.asList(2, 3), Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDefaultIfEmpty() {
        assertEquals(1, Stream.<Integer> empty().defaultIfEmpty(99).count());
        assertEquals(0, Stream.<Integer> empty().defaultIfEmpty(99).skip(1).count());
        assertArrayEquals(new Integer[] { 99 }, Stream.<Integer> empty().defaultIfEmpty(99).toArray());
        assertArrayEquals(new Integer[] {}, Stream.<Integer> empty().defaultIfEmpty(99).skip(1).toArray());
        assertEquals(N.asList(99), Stream.<Integer> empty().defaultIfEmpty(99).toList());
        assertEquals(N.asList(), Stream.<Integer> empty().defaultIfEmpty(99).skip(1).toList());
        assertEquals(1, Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).count());
        assertEquals(0, Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).skip(1).count());
        assertArrayEquals(new Integer[] { 99 }, Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).toArray());
        assertArrayEquals(new Integer[] {}, Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).skip(1).toArray());
        assertEquals(N.asList(99), Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).toList());
        assertEquals(N.asList(), Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterThrowIfEmpty() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).throwIfEmpty().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).throwIfEmpty().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).throwIfEmpty().toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterThrowIfEmptyWithSupplier() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 },
                Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIfEmpty() {
        AtomicBoolean flag = new AtomicBoolean(false);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).count());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).skip(1).count());
        assertFalse(flag.get());
        flag.set(false);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).toArray());
        assertFalse(flag.get());
        flag.set(false);
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).skip(1).toArray());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).toList());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).skip(1).toList());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).count());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).skip(1).count());
        assertFalse(flag.get());
        flag.set(false);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).toArray());
        assertFalse(flag.get());
        flag.set(false);
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).skip(1).toArray());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).toList());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).skip(1).toList());
        assertFalse(flag.get());
    }

    @Test
    public void testStreamCreatedAfterOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).count());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).skip(1).count());
        assertTrue(closed.get());
        closed.set(false);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).toArray());
        assertTrue(closed.get());
        closed.set(false);
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).skip(1).toArray());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).toList());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).skip(1).toList());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).count());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).count());
        assertTrue(closed.get());
        closed.set(false);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).toArray());
        assertTrue(closed.get());
        closed.set(false);
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).toArray());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).toList());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).toList());
        assertTrue(closed.get());
    }

    @Test
    public void testStreamCreatedAfterPeek() {
        List<Integer> list = new ArrayList<>();
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peek(list::add).count());
        list.clear();
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).peek(list::add).skip(1).count());
        list.clear();
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peek(list::add).toArray());
        list.clear();
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peek(list::add).skip(1).toArray());
        list.clear();
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peek(list::add).toList());
        list.clear();
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peek(list::add).skip(1).toList());
        list.clear();
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).count());
        list.clear();
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).skip(1).count());
        list.clear();
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).toArray());
        list.clear();
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).skip(1).toArray());
        list.clear();
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).toList());
        list.clear();
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSelect() {
        assertEquals(3, Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).count());
        assertEquals(2, Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).skip(1).count());
        assertArrayEquals(new String[] { "a", "b", "c" }, Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).toArray());
        assertArrayEquals(new String[] { "b", "c" }, Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).skip(1).toArray());
        assertEquals(N.asList("a", "b", "c"), Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).toList());
        assertEquals(N.asList("b", "c"), Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).skip(1).toList());
        assertEquals(3, Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).count());
        assertEquals(2, Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).skip(1).count());
        assertArrayEquals(new String[] { "a", "b", "c" }, Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).toArray());
        assertArrayEquals(new String[] { "b", "c" }, Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).skip(1).toArray());
        assertEquals(N.asList("a", "b", "c"), Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).toList());
        assertEquals(N.asList("b", "c"), Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPairWith() {
        assertEquals(3, Stream.of("a", "bb", "ccc").pairWith(String::length).count());
        assertEquals(2, Stream.of("a", "bb", "ccc").pairWith(String::length).skip(1).count());
        assertEquals(3, Stream.of("a", "bb", "ccc").pairWith(String::length).toArray().length);
        assertEquals(2, Stream.of("a", "bb", "ccc").pairWith(String::length).skip(1).toArray().length);
        assertEquals(3, Stream.of("a", "bb", "ccc").pairWith(String::length).toList().size());
        assertEquals(2, Stream.of("a", "bb", "ccc").pairWith(String::length).skip(1).toList().size());
        assertEquals(3, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).count());
        assertEquals(2, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).skip(1).count());
        assertEquals(3, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).toArray().length);
        assertEquals(2, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).skip(1).toArray().length);
        assertEquals(3, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).toList().size());
        assertEquals(2, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).skip(1).toList().size());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).skip(1).toArray());
        assertEquals(N.asList(2, 4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).toList());
        assertEquals(N.asList(4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).skip(1).toArray());
        assertEquals(N.asList(2, 4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).toList());
        assertEquals(N.asList(4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapIfNotNull() {
        assertEquals(3, Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).count());
        assertEquals(2, Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).skip(1).count());
        assertArrayEquals(new String[] { "A", "B", "C" }, Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).toArray());
        assertArrayEquals(new String[] { "B", "C" }, Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).skip(1).toArray());
        assertEquals(N.asList("A", "B", "C"), Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).toList());
        assertEquals(N.asList("B", "C"), Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).skip(1).toList());
        assertEquals(3, Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).count());
        assertEquals(2, Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).skip(1).count());
        assertArrayEquals(new String[] { "A", "B", "C" }, Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).toArray());
        assertArrayEquals(new String[] { "B", "C" }, Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).skip(1).toArray());
        assertEquals(N.asList("A", "B", "C"), Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).toList());
        assertEquals(N.asList("B", "C"), Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMap() {
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 7, 9 }, Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toArray());
        assertArrayEquals(new Integer[] { 5, 7, 9 },
                Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toArray());
        assertEquals(N.asList(3, 5, 7, 9), Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toList());
        assertEquals(N.asList(5, 7, 9), Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toList());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 7, 9 },
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toArray());
        assertArrayEquals(new Integer[] { 5, 7, 9 },
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toArray());
        assertEquals(N.asList(3, 5, 7, 9), Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toList());
        assertEquals(N.asList(5, 7, 9), Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMapWithIncrement() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 7, 11 }, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toArray());
        assertArrayEquals(new Integer[] { 7, 11 },
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toArray());
        assertEquals(N.asList(3, 7, 11), Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toList());
        assertEquals(N.asList(7, 11), Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 7, 11 },
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toArray());
        assertArrayEquals(new Integer[] { 7, 11 },
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toArray());
        assertEquals(N.asList(3, 7, 11), Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toList());
        assertEquals(N.asList(7, 11),
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMapWithIncrementAndIgnore() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 7 }, Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new Integer[] { 7 }, Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.asList(3, 7), Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).toList());
        assertEquals(N.asList(7), Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).skip(1).toList());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 7 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new Integer[] { 7 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.asList(3, 7), Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).toList());
        assertEquals(N.asList(7), Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMapTri() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).count());
        assertArrayEquals(new Integer[] { 6, 9, 12 },
                Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toArray());
        assertArrayEquals(new Integer[] { 9, 12 },
                Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).toArray());
        assertEquals(N.asList(6, 9, 12),
                Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toList());
        assertEquals(N.asList(9, 12),
                Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).count());
        assertEquals(2,
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).count());
        assertArrayEquals(new Integer[] { 6, 9, 12 },
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toArray());
        assertArrayEquals(new Integer[] { 9, 12 },
                Stream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList(6, 9, 12),
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toList());
        assertEquals(N.asList(9, 12),
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMapTriWithIncrement() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).count());
        assertEquals(1,
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).count());
        assertArrayEquals(new Integer[] { 6, 15 },
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toArray());
        assertArrayEquals(new Integer[] { 15 },
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).toArray());
        assertEquals(N.asList(6, 15),
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toList());
        assertEquals(N.asList(15),
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).toList());
        assertEquals(2,
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).count());
        assertEquals(1,
                Stream.of(1, 2, 3, 4, 5, 6)
                        .map(e -> e)
                        .slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c))
                        .skip(1)
                        .count());
        assertArrayEquals(new Integer[] { 6, 15 },
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toArray());
        assertArrayEquals(new Integer[] { 15 },
                Stream.of(1, 2, 3, 4, 5, 6)
                        .map(e -> e)
                        .slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList(6, 15),
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toList());
        assertEquals(N.asList(15),
                Stream.of(1, 2, 3, 4, 5, 6)
                        .map(e -> e)
                        .slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMapTriWithIncrementAndIgnore() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new Integer[] { 6, 15 }, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).toArray());
        assertArrayEquals(new Integer[] { 15 }, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).toArray());
        assertEquals(N.asList(6, 15), Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).toList());
        assertEquals(N.asList(15), Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).toList());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new Integer[] { 6, 15 }, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).toArray());
        assertArrayEquals(new Integer[] { 15 }, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).toArray());
        assertEquals(N.asList(6, 15), Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).toList());
        assertEquals(N.asList(15), Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRangeMap() {
        assertEquals(3, Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).count());
        assertEquals(2, Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).skip(1).count());
        assertArrayEquals(new String[] { "1-2", "5-6", "10-10" },
                Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).toArray());
        assertArrayEquals(new String[] { "5-6", "10-10" },
                Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).skip(1).toArray());
        assertEquals(N.asList("1-2", "5-6", "10-10"),
                Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).toList());
        assertEquals(N.asList("5-6", "10-10"),
                Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).skip(1).toList());
        assertEquals(3,
                Stream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).count());
        assertEquals(2,
                Stream.of(1, 2, 5, 6, 10)
                        .map(e -> e)
                        .rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "1-2", "5-6", "10-10" },
                Stream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).toArray());
        assertArrayEquals(new String[] { "5-6", "10-10" },
                Stream.of(1, 2, 5, 6, 10)
                        .map(e -> e)
                        .rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .toArray());
        assertEquals(N.asList("1-2", "5-6", "10-10"),
                Stream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).toList());
        assertEquals(N.asList("5-6", "10-10"),
                Stream.of(1, 2, 5, 6, 10)
                        .map(e -> e)
                        .rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterMapFirst() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).skip(1).count());
        assertArrayEquals(new Integer[] { 10, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).skip(1).toArray());
        assertEquals(N.asList(10, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).skip(1).count());
        assertArrayEquals(new Integer[] { 10, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).skip(1).toArray());
        assertEquals(N.asList(10, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapFirstOrElse() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 10, 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).toArray());
        assertEquals(N.asList(10, 4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).toList());
        assertEquals(N.asList(4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 10, 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).toArray());
        assertEquals(N.asList(10, 4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).toList());
        assertEquals(N.asList(4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapLast() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 50 }, Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 50 }, Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 50), Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).toList());
        assertEquals(N.asList(2, 3, 4, 50), Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).toList());
        assertEquals(N.asList(2, 3, 4, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapLastOrElse() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 4, 6, 8, 50 }, Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 50 }, Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).toArray());
        assertEquals(N.asList(2, 4, 6, 8, 50), Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).toList());
        assertEquals(N.asList(4, 6, 8, 50), Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 4, 6, 8, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).toArray());
        assertEquals(N.asList(2, 4, 6, 8, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).toList());
        assertEquals(N.asList(4, 6, 8, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6), Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).toList());
        assertEquals(N.asList(2, 3, 4, 5, 6), Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).skip(1).toList());
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).toList());
        assertEquals(N.asList(2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmap() {
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6), Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).toList());
        assertEquals(N.asList(2, 3, 4, 5, 6), Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).skip(1).toList());
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).toList());
        assertEquals(N.asList(2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattmap() {
        assertEquals(6, Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flattmap(Fn.identity()).count());
        assertEquals(5, Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flattmap(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flattmap(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flattmap(Fn.identity()).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6),
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flattmap(Fn.identity()).toList());
        assertEquals(N.asList(2, 3, 4, 5, 6),
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flattmap(Fn.identity()).skip(1).toList());
        assertEquals(6, Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flattmap(Fn.identity()).count());
        assertEquals(5, Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flattmap(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flattmap(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flattmap(Fn.identity()).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6),
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flattmap(Fn.identity()).toList());
        assertEquals(N.asList(2, 3, 4, 5, 6),
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flattmap(Fn.identity()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattMap() {
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6), Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).toList());
        assertEquals(N.asList(2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).skip(1).toList());
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).toList());
        assertEquals(N.asList(2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapIfNotNull() {
        assertEquals(4, Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).count());
        assertEquals(3, Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4 },
                Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4), Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).toList());
        assertEquals(N.asList(2, 3, 4), Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).skip(1).toList());
        assertEquals(4, Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).count());
        assertEquals(3, Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 },
                Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4 },
                Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4), Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).toList());
        assertEquals(N.asList(2, 3, 4), Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapIfNotNull2() {
        assertEquals(8,
                Stream.of("a", null, "b").flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2")).count());
        assertEquals(7,
                Stream.of("a", null, "b")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "A", "A2", "A1", "A12", "B", "B2", "B1", "B12" },
                Stream.of("a", null, "b")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .toArray());
        assertArrayEquals(new String[] { "A2", "A1", "A12", "B", "B2", "B1", "B12" },
                Stream.of("a", null, "b")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList("A", "A2", "A1", "A12", "B", "B2", "B1", "B12"),
                Stream.of("a", null, "b")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .toList());
        assertEquals(N.asList("A2", "A1", "A12", "B", "B2", "B1", "B12"),
                Stream.of("a", null, "b")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .toList());
        assertEquals(8,
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .count());
        assertEquals(7,
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "A", "A2", "A1", "A12", "B", "B2", "B1", "B12" },
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .toArray());
        assertArrayEquals(new String[] { "A2", "A1", "A12", "B", "B2", "B1", "B12" },
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList("A", "A2", "A1", "A12", "B", "B2", "B1", "B12"),
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .toList());
        assertEquals(N.asList("A2", "A1", "A12", "B", "B2", "B1", "B12"),
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterMapMulti() {
        assertEquals(10, Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).count());
        assertEquals(9, Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 }, Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).toArray());
        assertArrayEquals(new Integer[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 }, Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).toArray());
        assertEquals(N.asList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50), Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).toList());
        assertEquals(N.asList(10, 2, 20, 3, 30, 4, 40, 5, 50), Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).toList());
        assertEquals(10, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).count());
        assertEquals(9, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).toArray());
        assertArrayEquals(new Integer[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).toArray());
        assertEquals(N.asList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).toList());
        assertEquals(N.asList(10, 2, 20, 3, 30, 4, 40, 5, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartial() {
        assertEquals(3, Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).count());
        assertEquals(2, Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toList());
        assertEquals(N.asList(2, 3), Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).toList());
        assertEquals(3, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).count());
        assertEquals(2, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toList());
        assertEquals(N.asList(2, 3), Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartialJdk() {
        assertEquals(3, Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).count());
        assertEquals(2, Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).toList());
        assertEquals(N.asList(2, 3), Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).toList());
        assertEquals(3, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).count());
        assertEquals(2, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).toList());
        assertEquals(N.asList(2, 3), Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSortedByComparator() {
        assertEquals(5, Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).count());
        assertEquals(4, Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).skip(1).toList());
        assertEquals(5, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).count());
        assertEquals(4, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).toList());
        assertEquals(N.asList(2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSortedBy() {
        assertEquals(3, Stream.of("bb", "a", "ccc").sortedBy(String::length).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").sortedBy(String::length).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedBy(String::length).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedBy(String::length).skip(1).toArray());
        assertEquals(N.asList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").sortedBy(String::length).toList());
        assertEquals(N.asList("bb", "ccc"), Stream.of("bb", "a", "ccc").sortedBy(String::length).skip(1).toList());
        assertEquals(3, Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).skip(1).toArray());
        assertEquals(N.asList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).toList());
        assertEquals(N.asList("bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSortedByInt() {
        assertEquals(3, Stream.of("bb", "a", "ccc").sortedByInt(String::length).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").sortedByInt(String::length).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByInt(String::length).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByInt(String::length).skip(1).toArray());
        assertEquals(N.asList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByInt(String::length).toList());
        assertEquals(N.asList("bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByInt(String::length).skip(1).toList());
        assertEquals(3, Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).skip(1).toArray());
        assertEquals(N.asList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).toList());
        assertEquals(N.asList("bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSortedByLong() {
        assertEquals(3, Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).skip(1).toArray());
        assertEquals(N.asList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).toList());
        assertEquals(N.asList("bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).skip(1).toList());
        assertEquals(3, Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).skip(1).toArray());
        assertEquals(N.asList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).toList());
        assertEquals(N.asList("bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSortedByDouble() {
        assertEquals(3, Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).skip(1).toArray());
        assertEquals(N.asList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).toList());
        assertEquals(N.asList("bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).skip(1).toList());
        assertEquals(3, Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).skip(1).toArray());
        assertEquals(N.asList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).toList());
        assertEquals(N.asList("bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSortedByComparator() {
        assertEquals(5, Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).count());
        assertEquals(4, Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).toList());
        assertEquals(N.asList(4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).skip(1).toList());
        assertEquals(5, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).count());
        assertEquals(4, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).toList());
        assertEquals(N.asList(4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSortedByInt() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).skip(2).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).toList());
        assertEquals(N.asList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).skip(2).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).toList());
        assertEquals(N.asList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSortedByLong() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).skip(2).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).toList());
        assertEquals(N.asList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).skip(2).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).toList());
        assertEquals(N.asList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSortedByDouble() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).skip(2).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).toList());
        assertEquals(N.asList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).skip(2).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).toList());
        assertEquals(N.asList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSortedBy() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).skip(2).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).toList());
        assertEquals(N.asList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).skip(2).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).toList());
        assertEquals(N.asList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterTop() {
        assertEquals(3, Stream.of(5, 3, 1, 4, 2).top(3).count());
        assertEquals(2, Stream.of(5, 3, 1, 4, 2).top(3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 4 }, Stream.of(5, 3, 1, 4, 2).top(3).toArray());
        assertArrayEquals(new Integer[] { 5, 4 }, Stream.of(5, 3, 1, 4, 2).top(3).skip(1).toArray());
        assertEquals(N.asList(3, 5, 4), Stream.of(5, 3, 1, 4, 2).top(3).toList());
        assertEquals(N.asList(5, 4), Stream.of(5, 3, 1, 4, 2).top(3).skip(1).toList());
        assertEquals(3, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).count());
        assertEquals(2, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 4 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).toArray());
        assertArrayEquals(new Integer[] { 5, 4 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).skip(1).toArray());
        assertEquals(N.asList(3, 5, 4), Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).toList());
        assertEquals(N.asList(5, 4), Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTopWithComparator() {
        assertEquals(3, Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).count());
        assertEquals(2, Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 4 }, Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 5, 4 }, Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.asList(3, 5, 4), Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).toList());
        assertEquals(N.asList(5, 4), Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).skip(1).toList());
        assertEquals(3, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).count());
        assertEquals(2, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 4 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 5, 4 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.asList(3, 5, 4), Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).toList());
        assertEquals(N.asList(5, 4), Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipRange() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 4, 5 }, Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).skip(1).toArray());
        assertEquals(N.asList(1, 4, 5), Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).toList());
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).skip(1).toArray());
        assertEquals(N.asList(1, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).toList());
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipNulls() {
        assertEquals(3, Stream.of(1, null, 2, null, 3).skipNulls().count());
        assertEquals(2, Stream.of(1, null, 2, null, 3).skipNulls().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, null, 2, null, 3).skipNulls().toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, null, 2, null, 3).skipNulls().skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), Stream.of(1, null, 2, null, 3).skipNulls().toList());
        assertEquals(N.asList(2, 3), Stream.of(1, null, 2, null, 3).skipNulls().skip(1).toList());
        assertEquals(3, Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().count());
        assertEquals(2, Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().toList());
        assertEquals(N.asList(2, 3), Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipLast() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).skipLast(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).skipLast(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).skipLast(2).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, 2, 3, 4, 5).skipLast(2).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), Stream.of(1, 2, 3, 4, 5).skipLast(2).toList());
        assertEquals(N.asList(2, 3), Stream.of(1, 2, 3, 4, 5).skipLast(2).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).toList());
        assertEquals(N.asList(2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeLast() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).takeLast(2).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).takeLast(2).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).takeLast(2).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.of(1, 2, 3, 4, 5).takeLast(2).skip(1).toArray());
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).takeLast(2).toList());
        assertEquals(N.asList(5), Stream.of(1, 2, 3, 4, 5).takeLast(2).skip(1).toList());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).skip(1).toArray());
        assertEquals(N.asList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).toList());
        assertEquals(N.asList(5), Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnFirst() {
        List<Integer> firstElements = new ArrayList<>();
        Consumer<Integer> captureFirst = firstElements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterOnLast() {
        List<Integer> lastElements = new ArrayList<>();
        Consumer<Integer> captureLast = lastElements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).onLast(captureLast).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).onLast(captureLast).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onLast(captureLast).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onLast(captureLast).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).onLast(captureLast).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).onLast(captureLast).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterPeekFirst() {
        List<Integer> firstElements = new ArrayList<>();
        Consumer<Integer> captureFirst = firstElements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterPeekLast() {
        List<Integer> lastElements = new ArrayList<>();
        Consumer<Integer> captureLast = lastElements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterPeekIfWithPredicate() {
        List<Integer> evenElements = new ArrayList<>();
        Predicate<Integer> isEven = i -> i % 2 == 0;
        Consumer<Integer> captureEven = evenElements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterPeekIfWithBiPredicate() {
        List<Integer> elements = new ArrayList<>();
        BiPredicate<Integer, Long> isSecondElement = (e, count) -> count == 2L;
        Consumer<Integer> capture = elements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependVarargs() {
        assertEquals(5, Stream.of(3, 4, 5).prepend(1, 2).count());
        assertEquals(3, Stream.of(3, 4, 5).prepend(1, 2).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).prepend(1, 2).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(3, 4, 5).prepend(1, 2).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).prepend(1, 2).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(3, 4, 5).prepend(1, 2).skip(2).toList());
        assertEquals(5, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).count());
        assertEquals(3, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendVarargs() {
        assertEquals(5, Stream.of(1, 2, 3).append(4, 5).count());
        assertEquals(3, Stream.of(1, 2, 3).append(4, 5).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).append(4, 5).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3).append(4, 5).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).append(4, 5).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3).append(4, 5).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3).map(e -> e).append(4, 5).count());
        assertEquals(3, Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(4, 5).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(4, 5).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmptyVarargs() {
        // Non-empty stream should not append
        assertEquals(3, Stream.of(1, 2, 3).appendIfEmpty(4, 5).count());
        assertEquals(2, Stream.of(1, 2, 3).appendIfEmpty(4, 5).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, 2, 3).appendIfEmpty(4, 5).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, 2, 3).appendIfEmpty(4, 5).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), Stream.of(1, 2, 3).appendIfEmpty(4, 5).toList());
        assertEquals(N.asList(2, 3), Stream.of(1, 2, 3).appendIfEmpty(4, 5).skip(1).toList());

        // Empty stream should append
        assertEquals(2, Stream.<Integer> empty().appendIfEmpty(4, 5).count());
        assertEquals(1, Stream.<Integer> empty().appendIfEmpty(4, 5).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.<Integer> empty().appendIfEmpty(4, 5).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.<Integer> empty().appendIfEmpty(4, 5).skip(1).toArray());
        assertEquals(N.asList(4, 5), Stream.<Integer> empty().appendIfEmpty(4, 5).toList());
        assertEquals(N.asList(5), Stream.<Integer> empty().appendIfEmpty(4, 5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterBuffered() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).buffered().count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).buffered().skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered().toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered().skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered().toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered().skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterBufferedWithSize() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).buffered(10).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).buffered(10).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered(10).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered(10).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered(10).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered(10).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterBufferedWithQueue() {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).buffered(queue).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).buffered(queue).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered(queue).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered(queue).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered(queue).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered(queue).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).toList());
        assertEquals(N.asList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeWithCollection() {
        List<Integer> collection = Arrays.asList(2, 4, 6);
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        assertEquals(6, Stream.of(1, 3, 5).mergeWith(collection, selector).count());
        assertEquals(4, Stream.of(1, 3, 5).mergeWith(collection, selector).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 }, Stream.of(1, 3, 5).mergeWith(collection, selector).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5, 6 }, Stream.of(1, 3, 5).mergeWith(collection, selector).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6), Stream.of(1, 3, 5).mergeWith(collection, selector).toList());
        assertEquals(N.asList(3, 4, 5, 6), Stream.of(1, 3, 5).mergeWith(collection, selector).skip(2).toList());
        assertEquals(6, Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).count());
        assertEquals(4, Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 }, Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5, 6 }, Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6), Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).toList());
        assertEquals(N.asList(3, 4, 5, 6), Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeWithStream() {
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        assertEquals(6, Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).count());
        assertEquals(4, Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 }, Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5, 6 }, Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6), Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).toList());
        assertEquals(N.asList(3, 4, 5, 6), Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).skip(2).toList());
        assertEquals(6, Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).count());
        assertEquals(4, Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 }, Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5, 6 }, Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).skip(2).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6), Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).toList());
        assertEquals(N.asList(3, 4, 5, 6), Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithCollection() {
        List<String> collection = Arrays.asList("a", "b", "c");
        BiFunction<Integer, String, String> zipFunc = (i, s) -> i + s;

        assertEquals(3, Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).skip(1).count());
        assertArrayEquals(new String[] { "1a", "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).toArray());
        assertArrayEquals(new String[] { "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).skip(1).toArray());
        assertEquals(N.asList("1a", "2b", "3c"), Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).toList());
        assertEquals(N.asList("2b", "3c"), Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).skip(1).count());
        assertArrayEquals(new String[] { "1a", "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).toArray());
        assertArrayEquals(new String[] { "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).skip(1).toArray());
        assertEquals(N.asList("1a", "2b", "3c"), Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).toList());
        assertEquals(N.asList("2b", "3c"), Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithStream() {
        BiFunction<Integer, String, String> zipFunc = (i, s) -> i + s;

        assertEquals(3, Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).count());
        assertArrayEquals(new String[] { "1a", "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).toArray());
        assertArrayEquals(new String[] { "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).toArray());
        assertEquals(N.asList("1a", "2b", "3c"), Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).toList());
        assertEquals(N.asList("2b", "3c"), Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).count());
        assertArrayEquals(new String[] { "1a", "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).toArray());
        assertArrayEquals(new String[] { "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).toArray());
        assertEquals(N.asList("1a", "2b", "3c"), Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).toList());
        assertEquals(N.asList("2b", "3c"), Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).toList());
    }
}
