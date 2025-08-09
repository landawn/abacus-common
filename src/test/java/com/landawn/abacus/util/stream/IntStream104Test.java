package com.landawn.abacus.util.stream;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IndexedInt;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalInt;

public class IntStream104Test extends TestBase {
    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toList());
        assertEquals(N.asList(4, 5), IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.asList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
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
        assertEquals(N.asList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).toList());
        droppedCount.set(0);
        assertEquals(N.asList(4, 5), IntStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).skip(1).toList());
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
        assertEquals(N.asList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).toList());
        droppedCount.set(0);
        assertEquals(N.asList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, i -> droppedCount.incrementAndGet()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).toArray());
        assertArrayEquals(new int[] { 2, 3 }, IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).toList());
        assertEquals(N.asList(2, 3), IntStream.of(1, 2, 3, 4, 5).takeWhile(i -> i <= 3).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).toArray());
        assertArrayEquals(new int[] { 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).toList());
        assertEquals(N.asList(2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i <= 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).skip(1).count());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).toArray());
        assertArrayEquals(new int[] { 5 }, IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).skip(1).toArray());
        assertEquals(N.asList(4, 5), IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).toList());
        assertEquals(N.asList(5), IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3).skip(1).toList());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).skip(1).count());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).toArray());
        assertArrayEquals(new int[] { 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).skip(1).toArray());
        assertEquals(N.asList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).toList());
        assertEquals(N.asList(5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3).skip(1).toList());
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
        assertEquals(N.asList(4, 5), IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).toList());
        droppedCount.set(0);
        assertEquals(N.asList(5), IntStream.of(1, 2, 3, 4, 5).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).skip(1).toList());
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
        assertEquals(N.asList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).toList());
        droppedCount.set(0);
        assertEquals(N.asList(5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i <= 3, i -> droppedCount.incrementAndGet()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).skip(1).count());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).toArray());
        assertArrayEquals(new int[] { 5 }, IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).skip(1).toArray());
        assertEquals(N.asList(4, 5), IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).toList());
        assertEquals(N.asList(5), IntStream.of(1, 2, 3, 4, 5).skipUntil(i -> i > 3).skip(1).toList());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).skip(1).count());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).toArray());
        assertArrayEquals(new int[] { 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).skip(1).toArray());
        assertEquals(N.asList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).toList());
        assertEquals(N.asList(5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i > 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(5, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().count());
        assertEquals(4, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).distinct().skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().count());
        assertEquals(4, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.of(1, 2, 2, 3, 3, 3, 4, 5, 5).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIntersection() {
        Collection<Integer> c = Arrays.asList(3, 4, 5, 6);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).intersection(c).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).intersection(c).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).intersection(c).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).intersection(c).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).intersection(c).toList());
        assertEquals(N.asList(4, 5), IntStream.of(1, 2, 3, 4, 5).intersection(c).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).toList());
        assertEquals(N.asList(4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(c).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDifference() {
        Collection<Integer> c = Arrays.asList(3, 4, 5, 6);
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).difference(c).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).difference(c).skip(1).count());
        assertArrayEquals(new int[] { 1, 2 }, IntStream.of(1, 2, 3, 4, 5).difference(c).toArray());
        assertArrayEquals(new int[] { 2 }, IntStream.of(1, 2, 3, 4, 5).difference(c).skip(1).toArray());
        assertEquals(N.asList(1, 2), IntStream.of(1, 2, 3, 4, 5).difference(c).toList());
        assertEquals(N.asList(2), IntStream.of(1, 2, 3, 4, 5).difference(c).skip(1).toList());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).count());
        assertEquals(1, IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).skip(1).count());
        assertArrayEquals(new int[] { 1, 2 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).toArray());
        assertArrayEquals(new int[] { 2 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).skip(1).toArray());
        assertEquals(N.asList(1, 2), IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).toList());
        assertEquals(N.asList(2), IntStream.of(1, 2, 3, 4, 5).map(e -> e).difference(c).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSymmetricDifference() {
        Collection<Integer> c = Arrays.asList(3, 4, 5, 6);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 6 }, IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).toArray());
        assertArrayEquals(new int[] { 2, 6 }, IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).skip(1).toArray());
        assertEquals(N.asList(1, 2, 6), IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).toList());
        assertEquals(N.asList(2, 6), IntStream.of(1, 2, 3, 4, 5).symmetricDifference(c).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 6 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).toArray());
        assertArrayEquals(new int[] { 2, 6 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).skip(1).toArray());
        assertEquals(N.asList(1, 2, 6), IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).toList());
        assertEquals(N.asList(2, 6), IntStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(c).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReversed() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).reversed().count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).reversed().skip(1).count());
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, IntStream.of(1, 2, 3, 4, 5).reversed().toArray());
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, IntStream.of(1, 2, 3, 4, 5).reversed().skip(1).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), IntStream.of(1, 2, 3, 4, 5).reversed().toList());
        assertEquals(N.asList(4, 3, 2, 1), IntStream.of(1, 2, 3, 4, 5).reversed().skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).count());
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toArray());
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toList());
        assertEquals(N.asList(4, 3, 2, 1), IntStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRotated() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).rotated(2).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).count());
        assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).rotated(2).toArray());
        assertArrayEquals(new int[] { 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toArray());
        assertEquals(N.asList(4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).rotated(2).toList());
        assertEquals(N.asList(5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).count());
        assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toArray());
        assertArrayEquals(new int[] { 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toArray());
        assertEquals(N.asList(4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toList());
        assertEquals(N.asList(5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterShuffled() {
        // Testing count and skip operations (shuffle doesn't affect count)
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).shuffled().count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).shuffled().skip(1).count());
        // Testing toArray and toList (elements should be same, just order different)
        int[] shuffledArray = IntStream.of(1, 2, 3, 4, 5).shuffled().toArray();
        assertEquals(5, shuffledArray.length);
        assertHaveSameElements(N.toList(shuffledArray), N.asList(1, 2, 3, 4, 5));

        List<Integer> shuffledList = IntStream.of(1, 2, 3, 4, 5).shuffled().toList();
        assertEquals(5, shuffledList.size());
        assertHaveSameElements(shuffledList, N.asList(1, 2, 3, 4, 5));

        // Testing with map
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterShuffledWithRandom() {
        Random rnd = new Random(42);
        // Testing count and skip operations (shuffle doesn't affect count)
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).shuffled(rnd).skip(1).count());
        // Testing toArray and toList (elements should be same, just order different)
        rnd = new Random(42);
        int[] shuffledArray = IntStream.of(1, 2, 3, 4, 5).shuffled(rnd).toArray();
        assertEquals(5, shuffledArray.length);
        assertHaveSameElements(N.toList(shuffledArray), N.asList(1, 2, 3, 4, 5));

        rnd = new Random(42);
        List<Integer> shuffledList = IntStream.of(1, 2, 3, 4, 5).shuffled(rnd).toList();
        assertEquals(5, shuffledList.size());
        assertHaveSameElements(shuffledList, N.asList(1, 2, 3, 4, 5));

        // Testing with map
        rnd = new Random(42);
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).skip(1).count());
    }

    @Test
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, IntStream.of(3, 1, 4, 2, 5).sorted().count());
        assertEquals(4, IntStream.of(3, 1, 4, 2, 5).sorted().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).sorted().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).sorted().skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.of(3, 1, 4, 2, 5).sorted().toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.of(3, 1, 4, 2, 5).sorted().skip(1).toList());
        assertEquals(5, IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().count());
        assertEquals(4, IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.of(3, 1, 4, 2, 5).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, IntStream.of(3, 1, 4, 2, 5).reverseSorted().count());
        assertEquals(4, IntStream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).count());
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, IntStream.of(3, 1, 4, 2, 5).reverseSorted().toArray());
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, IntStream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), IntStream.of(3, 1, 4, 2, 5).reverseSorted().toList());
        assertEquals(N.asList(4, 3, 2, 1), IntStream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).toList());
        assertEquals(5, IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().count());
        assertEquals(4, IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().toArray());
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).toArray());
        assertEquals(N.asList(5, 4, 3, 2, 1), IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().toList());
        assertEquals(N.asList(4, 3, 2, 1), IntStream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCycled() {
        assertEquals(15, IntStream.of(1, 2, 3, 4, 5).cycled().limit(15).count());
        assertEquals(14, IntStream.of(1, 2, 3, 4, 5).cycled().limit(15).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).cycled().limit(8).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).cycled().limit(8).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).cycled().limit(8).toList());
        assertEquals(N.asList(2, 3, 4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).cycled().limit(8).skip(1).toList());
        assertEquals(15, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(15).count());
        assertEquals(14, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(15).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(8).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 1, 2, 3 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(8).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(8).toList());
        assertEquals(N.asList(2, 3, 4, 5, 1, 2, 3), IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled().limit(8).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCycledWithRounds() {
        assertEquals(10, IntStream.of(1, 2, 3, 4, 5).cycled(2).count());
        assertEquals(9, IntStream.of(1, 2, 3, 4, 5).cycled(2).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).cycled(2).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).cycled(2).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).cycled(2).toList());
        assertEquals(N.asList(2, 3, 4, 5, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).cycled(2).skip(1).toList());
        assertEquals(10, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).count());
        assertEquals(9, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).toList());
        assertEquals(N.asList(2, 3, 4, 5, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).cycled(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIndexed() {
        // indexed() returns Stream<IndexedInt>, not IntStream, so we need to test differently
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

        // Test with map
        indexedStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).indexed();
        assertEquals(5, indexedStream.count());

        indexedStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).indexed();
        indexedList = indexedStream.toList();
        assertEquals(5, indexedList.size());
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).skip(2).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).skip(2).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).skip(2).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5), IntStream.of(1, 2, 3, 4, 5).skip(2).toList());
        assertEquals(N.asList(4, 5), IntStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toArray());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).toArray());
        assertEquals(N.asList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).toList());
        assertEquals(N.asList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(i -> i * 2).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).toArray());
        assertEquals(N.asList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).toList());
        assertEquals(N.asList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).map(i -> i * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToChar() {
        // mapToChar returns CharStream, so we need to test CharStream operations
        CharStream charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertEquals(5, charStream.count());

        charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertEquals(4, charStream.skip(1).count());

        charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertArrayEquals(new char[] { 'A', 'B', 'C', 'D', 'E' }, charStream.toArray());

        charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertArrayEquals(new char[] { 'B', 'C', 'D', 'E' }, charStream.skip(1).toArray());

        charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertEquals(N.asList('A', 'B', 'C', 'D', 'E'), charStream.toList());

        charStream = IntStream.of(65, 66, 67, 68, 69).mapToChar(i -> (char) i);
        assertEquals(N.asList('B', 'C', 'D', 'E'), charStream.skip(1).toList());

        // Test with map
        charStream = IntStream.of(65, 66, 67, 68, 69).map(e -> e).mapToChar(i -> (char) i);
        assertEquals(5, charStream.count());

        charStream = IntStream.of(65, 66, 67, 68, 69).map(e -> e).mapToChar(i -> (char) i);
        assertEquals(N.asList('A', 'B', 'C', 'D', 'E'), charStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToByte() {
        // mapToByte returns ByteStream, so we need to test ByteStream operations
        ByteStream byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertEquals(5, byteStream.count());

        byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertEquals(4, byteStream.skip(1).count());

        byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, byteStream.toArray());

        byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, byteStream.skip(1).toArray());

        byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), byteStream.toList());

        byteStream = IntStream.of(1, 2, 3, 4, 5).mapToByte(i -> (byte) i);
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), byteStream.skip(1).toList());

        // Test with map
        byteStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToByte(i -> (byte) i);
        assertEquals(5, byteStream.count());

        byteStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToByte(i -> (byte) i);
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), byteStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToShort() {
        // mapToShort returns ShortStream, so we need to test ShortStream operations
        ShortStream shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertEquals(5, shortStream.count());

        shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertEquals(4, shortStream.skip(1).count());

        shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, shortStream.toArray());

        shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertArrayEquals(new short[] { 2, 3, 4, 5 }, shortStream.skip(1).toArray());

        shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertEquals(N.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), shortStream.toList());

        shortStream = IntStream.of(1, 2, 3, 4, 5).mapToShort(i -> (short) i);
        assertEquals(N.asList((short) 2, (short) 3, (short) 4, (short) 5), shortStream.skip(1).toList());

        // Test with map
        shortStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToShort(i -> (short) i);
        assertEquals(5, shortStream.count());

        shortStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToShort(i -> (short) i);
        assertEquals(N.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), shortStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToLong() {
        // mapToLong returns LongStream, so we need to test LongStream operations
        LongStream longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertEquals(5, longStream.count());

        longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertEquals(4, longStream.skip(1).count());

        longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, longStream.toArray());

        longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertArrayEquals(new long[] { 2L, 3L, 4L, 5L }, longStream.skip(1).toArray());

        longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertEquals(N.asList(1L, 2L, 3L, 4L, 5L), longStream.toList());

        longStream = IntStream.of(1, 2, 3, 4, 5).mapToLong(i -> (long) i);
        assertEquals(N.asList(2L, 3L, 4L, 5L), longStream.skip(1).toList());

        // Test with map
        longStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToLong(i -> (long) i);
        assertEquals(5, longStream.count());

        longStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToLong(i -> (long) i);
        assertEquals(N.asList(1L, 2L, 3L, 4L, 5L), longStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToFloat() {
        // mapToFloat returns FloatStream, so we need to test FloatStream operations
        FloatStream floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertEquals(5, floatStream.count());

        floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertEquals(4, floatStream.skip(1).count());

        floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertArrayEquals(new float[] { 1f, 2f, 3f, 4f, 5f }, floatStream.toArray(), 0.001f);

        floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertArrayEquals(new float[] { 2f, 3f, 4f, 5f }, floatStream.skip(1).toArray(), 0.001f);

        floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), floatStream.toList());

        floatStream = IntStream.of(1, 2, 3, 4, 5).mapToFloat(i -> (float) i);
        assertEquals(N.asList(2f, 3f, 4f, 5f), floatStream.skip(1).toList());

        // Test with map
        floatStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToFloat(i -> (float) i);
        assertEquals(5, floatStream.count());

        floatStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToFloat(i -> (float) i);
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), floatStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToDouble() {
        // mapToDouble returns DoubleStream, so we need to test DoubleStream operations
        DoubleStream doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertEquals(5, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertEquals(4, doubleStream.skip(1).count());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, doubleStream.toArray(), 0.001);

        doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0, 5.0 }, doubleStream.skip(1).toArray(), 0.001);

        doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), doubleStream.toList());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).mapToDouble(i -> (double) i);
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), doubleStream.skip(1).toList());

        // Test with map
        doubleStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(i -> (double) i);
        assertEquals(5, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(i -> (double) i);
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), doubleStream.toList());
    }

    @Test
    public void testStreamCreatedAfterMapToObj() {
        // mapToObj returns Stream<T>, so we need to test Stream operations
        Stream<String> stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertEquals(5, stringStream.count());

        stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertEquals(4, stringStream.skip(1).count());

        stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertArrayEquals(new String[] { "Item1", "Item2", "Item3", "Item4", "Item5" }, stringStream.toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertArrayEquals(new String[] { "Item2", "Item3", "Item4", "Item5" }, stringStream.skip(1).toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertEquals(N.asList("Item1", "Item2", "Item3", "Item4", "Item5"), stringStream.toList());

        stringStream = IntStream.of(1, 2, 3, 4, 5).mapToObj(i -> "Item" + i);
        assertEquals(N.asList("Item2", "Item3", "Item4", "Item5"), stringStream.skip(1).toList());

        // Test with map
        stringStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(i -> "Item" + i);
        assertEquals(5, stringStream.count());

        stringStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(i -> "Item" + i);
        assertEquals(N.asList("Item1", "Item2", "Item3", "Item4", "Item5"), stringStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(9, IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).count());
        assertEquals(8, IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 }, IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).toArray());
        assertEquals(N.asList(1, 10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).toList());
        assertEquals(N.asList(10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).toList());
        assertEquals(9, IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).count());
        assertEquals(8, IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).toArray());
        assertEquals(N.asList(1, 10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).toList());
        assertEquals(N.asList(10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).map(e -> e).flatMap(i -> IntStream.of(i, i * 10, i * 100)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmap() {
        assertEquals(9, IntStream.of(1, 2, 3).flatmap(i -> new int[] { i, i * 10, i * 100 }).count());
        assertEquals(8, IntStream.of(1, 2, 3).flatmap(i -> new int[] { i, i * 10, i * 100 }).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 }, IntStream.of(1, 2, 3).flatmap(i -> new int[] { i, i * 10, i * 100 }).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).flatmap(i -> new int[] { i, i * 10, i * 100 }).skip(1).toArray());
        assertEquals(N.asList(1, 10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).flatmap(i -> new int[] { i, i * 10, i * 100 }).toList());
        assertEquals(N.asList(10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).flatmap(i -> new int[] { i, i * 10, i * 100 }).skip(1).toList());
        assertEquals(9, IntStream.of(1, 2, 3).map(e -> e).flatmap(i -> new int[] { i, i * 10, i * 100 }).count());
        assertEquals(8, IntStream.of(1, 2, 3).map(e -> e).flatmap(i -> new int[] { i, i * 10, i * 100 }).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flatmap(i -> new int[] { i, i * 10, i * 100 }).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flatmap(i -> new int[] { i, i * 10, i * 100 }).skip(1).toArray());
        assertEquals(N.asList(1, 10, 100, 2, 20, 200, 3, 30, 300), IntStream.of(1, 2, 3).map(e -> e).flatmap(i -> new int[] { i, i * 10, i * 100 }).toList());
        assertEquals(N.asList(10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).map(e -> e).flatmap(i -> new int[] { i, i * 10, i * 100 }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattMap() {
        assertEquals(9, IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).count());
        assertEquals(8, IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).toArray());
        assertEquals(N.asList(1, 10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).toList());
        assertEquals(N.asList(10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).toList());
        assertEquals(9, IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).count());
        assertEquals(8, IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).toArray());
        assertArrayEquals(new int[] { 10, 100, 2, 20, 200, 3, 30, 300 },
                IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).toArray());
        assertEquals(N.asList(1, 10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).toList());
        assertEquals(N.asList(10, 100, 2, 20, 200, 3, 30, 300),
                IntStream.of(1, 2, 3).map(e -> e).flattMap(i -> java.util.stream.IntStream.of(i, i * 10, i * 100)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToChar() {
        // flatMapToChar returns CharStream
        CharStream charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(6, charStream.count());

        charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(5, charStream.skip(1).count());

        charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertArrayEquals(new char[] { 'A', 'a', 'B', 'b', 'C', 'c' }, charStream.toArray());

        charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertArrayEquals(new char[] { 'a', 'B', 'b', 'C', 'c' }, charStream.skip(1).toArray());

        charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(N.asList('A', 'a', 'B', 'b', 'C', 'c'), charStream.toList());

        charStream = IntStream.of(1, 2, 3).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(N.asList('a', 'B', 'b', 'C', 'c'), charStream.skip(1).toList());

        // Test with map
        charStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(6, charStream.count());

        charStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToChar(i -> CharStream.of((char) ('A' + i - 1), (char) ('a' + i - 1)));
        assertEquals(N.asList('A', 'a', 'B', 'b', 'C', 'c'), charStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToByte() {
        // flatMapToByte returns ByteStream
        ByteStream byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(6, byteStream.count());

        byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(5, byteStream.skip(1).count());

        byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertArrayEquals(new byte[] { 1, 10, 2, 20, 3, 30 }, byteStream.toArray());

        byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertArrayEquals(new byte[] { 10, 2, 20, 3, 30 }, byteStream.skip(1).toArray());

        byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(N.asList((byte) 1, (byte) 10, (byte) 2, (byte) 20, (byte) 3, (byte) 30), byteStream.toList());

        byteStream = IntStream.of(1, 2, 3).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(N.asList((byte) 10, (byte) 2, (byte) 20, (byte) 3, (byte) 30), byteStream.skip(1).toList());

        // Test with map
        byteStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(6, byteStream.count());

        byteStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToByte(i -> ByteStream.of((byte) i, (byte) (i * 10)));
        assertEquals(N.asList((byte) 1, (byte) 10, (byte) 2, (byte) 20, (byte) 3, (byte) 30), byteStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToShort() {
        // flatMapToShort returns ShortStream
        ShortStream shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(6, shortStream.count());

        shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(5, shortStream.skip(1).count());

        shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertArrayEquals(new short[] { 1, 10, 2, 20, 3, 30 }, shortStream.toArray());

        shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertArrayEquals(new short[] { 10, 2, 20, 3, 30 }, shortStream.skip(1).toArray());

        shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(N.asList((short) 1, (short) 10, (short) 2, (short) 20, (short) 3, (short) 30), shortStream.toList());

        shortStream = IntStream.of(1, 2, 3).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(N.asList((short) 10, (short) 2, (short) 20, (short) 3, (short) 30), shortStream.skip(1).toList());

        // Test with map
        shortStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(6, shortStream.count());

        shortStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToShort(i -> ShortStream.of((short) i, (short) (i * 10)));
        assertEquals(N.asList((short) 1, (short) 10, (short) 2, (short) 20, (short) 3, (short) 30), shortStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToLong() {
        // flatMapToLong returns LongStream
        LongStream longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of((long) i, (long) (i * 10)));
        assertEquals(6, longStream.count());

        longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of((long) i, (long) (i * 10)));
        assertEquals(5, longStream.skip(1).count());

        longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of((long) i, (long) (i * 10)));
        assertArrayEquals(new long[] { 1L, 10L, 2L, 20L, 3L, 30L }, longStream.toArray());

        longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of((long) i, (long) (i * 10)));
        assertArrayEquals(new long[] { 10L, 2L, 20L, 3L, 30L }, longStream.skip(1).toArray());

        longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of((long) i, (long) (i * 10)));
        assertEquals(N.asList(1L, 10L, 2L, 20L, 3L, 30L), longStream.toList());

        longStream = IntStream.of(1, 2, 3).flatMapToLong(i -> LongStream.of((long) i, (long) (i * 10)));
        assertEquals(N.asList(10L, 2L, 20L, 3L, 30L), longStream.skip(1).toList());

        // Test with map
        longStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToLong(i -> LongStream.of((long) i, (long) (i * 10)));
        assertEquals(6, longStream.count());

        longStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToLong(i -> LongStream.of((long) i, (long) (i * 10)));
        assertEquals(N.asList(1L, 10L, 2L, 20L, 3L, 30L), longStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToFloat() {
        // flatMapToFloat returns FloatStream
        FloatStream floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of((float) i, (float) (i * 10)));
        assertEquals(6, floatStream.count());

        floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of((float) i, (float) (i * 10)));
        assertEquals(5, floatStream.skip(1).count());

        floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of((float) i, (float) (i * 10)));
        assertArrayEquals(new float[] { 1f, 10f, 2f, 20f, 3f, 30f }, floatStream.toArray(), 0.001f);

        floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of((float) i, (float) (i * 10)));
        assertArrayEquals(new float[] { 10f, 2f, 20f, 3f, 30f }, floatStream.skip(1).toArray(), 0.001f);

        floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of((float) i, (float) (i * 10)));
        assertEquals(N.asList(1f, 10f, 2f, 20f, 3f, 30f), floatStream.toList());

        floatStream = IntStream.of(1, 2, 3).flatMapToFloat(i -> FloatStream.of((float) i, (float) (i * 10)));
        assertEquals(N.asList(10f, 2f, 20f, 3f, 30f), floatStream.skip(1).toList());

        // Test with map
        floatStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToFloat(i -> FloatStream.of((float) i, (float) (i * 10)));
        assertEquals(6, floatStream.count());

        floatStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToFloat(i -> FloatStream.of((float) i, (float) (i * 10)));
        assertEquals(N.asList(1f, 10f, 2f, 20f, 3f, 30f), floatStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToDouble() {
        // flatMapToDouble returns DoubleStream
        DoubleStream doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of((double) i, (double) (i * 10)));
        assertEquals(6, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of((double) i, (double) (i * 10)));
        assertEquals(5, doubleStream.skip(1).count());

        doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of((double) i, (double) (i * 10)));
        assertArrayEquals(new double[] { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0 }, doubleStream.toArray(), 0.001);

        doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of((double) i, (double) (i * 10)));
        assertArrayEquals(new double[] { 10.0, 2.0, 20.0, 3.0, 30.0 }, doubleStream.skip(1).toArray(), 0.001);

        doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of((double) i, (double) (i * 10)));
        assertEquals(N.asList(1.0, 10.0, 2.0, 20.0, 3.0, 30.0), doubleStream.toList());

        doubleStream = IntStream.of(1, 2, 3).flatMapToDouble(i -> DoubleStream.of((double) i, (double) (i * 10)));
        assertEquals(N.asList(10.0, 2.0, 20.0, 3.0, 30.0), doubleStream.skip(1).toList());

        // Test with map
        doubleStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToDouble(i -> DoubleStream.of((double) i, (double) (i * 10)));
        assertEquals(6, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToDouble(i -> DoubleStream.of((double) i, (double) (i * 10)));
        assertEquals(N.asList(1.0, 10.0, 2.0, 20.0, 3.0, 30.0), doubleStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToObj() {
        // flatMapToObj returns Stream<T>
        Stream<String> stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(5, stringStream.skip(1).count());

        stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" }, stringStream.toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3" }, stringStream.skip(1).toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());

        stringStream = IntStream.of(1, 2, 3).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(N.asList("B1", "A2", "B2", "A3", "B3"), stringStream.skip(1).toList());

        // Test with map
        stringStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).map(e -> e).flatMapToObj(i -> Stream.of("A" + i, "B" + i));
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapToObj() {
        // flatmapToObj returns Stream<T>
        Stream<String> stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(5, stringStream.skip(1).count());

        stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" }, stringStream.toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3" }, stringStream.skip(1).toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());

        stringStream = IntStream.of(1, 2, 3).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(N.asList("B1", "A2", "B2", "A3", "B3"), stringStream.skip(1).toList());

        // Test with map
        stringStream = IntStream.of(1, 2, 3).map(e -> e).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).map(e -> e).flatmapToObj(i -> Arrays.asList("A" + i, "B" + i));
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());
    }

    @Test
    public void testStreamCreatedAfterFlattmapToObj() {
        // flattmapToObj returns Stream<T>
        Stream<String> stringStream = IntStream.of(1, 2, 3).flattmapToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).flattmapToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(5, stringStream.skip(1).count());

        stringStream = IntStream.of(1, 2, 3).flattmapToObj(i -> new String[] { "A" + i, "B" + i });
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" }, stringStream.toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flattmapToObj(i -> new String[] { "A" + i, "B" + i });
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3" }, stringStream.skip(1).toArray(String[]::new));

        stringStream = IntStream.of(1, 2, 3).flattmapToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());

        stringStream = IntStream.of(1, 2, 3).flattmapToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(N.asList("B1", "A2", "B2", "A3", "B3"), stringStream.skip(1).toList());

        // Test with map
        stringStream = IntStream.of(1, 2, 3).map(e -> e).flattmapToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(6, stringStream.count());

        stringStream = IntStream.of(1, 2, 3).map(e -> e).flattmapToObj(i -> new String[] { "A" + i, "B" + i });
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3"), stringStream.toList());
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
        assertEquals(N.asList(1, 10, 2, 20, 3, 30), IntStream.of(1, 2, 3).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).toList());
        assertEquals(N.asList(10, 2, 20, 3, 30), IntStream.of(1, 2, 3).mapMulti((i, consumer) -> {
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
        assertEquals(N.asList(1, 10, 2, 20, 3, 30), IntStream.of(1, 2, 3).map(e -> e).mapMulti((i, consumer) -> {
            consumer.accept(i);
            consumer.accept(i * 10);
        }).toList());
        assertEquals(N.asList(10, 2, 20, 3, 30), IntStream.of(1, 2, 3).map(e -> e).mapMulti((i, consumer) -> {
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
        assertEquals(N.asList(10, 30, 50), IntStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).toList());
        assertEquals(N.asList(30, 50), IntStream.of(1, 2, 3, 4, 5).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).count());
        assertEquals(2, IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).skip(1).count());
        assertArrayEquals(new int[] { 10, 30, 50 },
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).toArray());
        assertArrayEquals(new int[] { 30, 50 },
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).skip(1).toArray());
        assertEquals(N.asList(10, 30, 50),
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).mapPartial(i -> i % 2 == 0 ? OptionalInt.empty() : OptionalInt.of(i * 10)).toList());
        assertEquals(N.asList(30, 50),
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
        assertEquals(N.asList(10, 30, 50),
                IntStream.of(1, 2, 3, 4, 5).mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10)).toList());
        assertEquals(N.asList(30, 50),
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
        assertEquals(N.asList(10, 30, 50),
                IntStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10))
                        .toList());
        assertEquals(N.asList(30, 50),
                IntStream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .mapPartialJdk(i -> i % 2 == 0 ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(i * 10))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterRangeMap() {
        assertEquals(3, IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).count());
        assertEquals(2, IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).count());
        assertArrayEquals(new int[] { 3, 9, 16 },
                IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).toArray());
        assertArrayEquals(new int[] { 9, 16 },
                IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).toArray());
        assertEquals(N.asList(3, 9, 16), IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).toList());
        assertEquals(N.asList(9, 16), IntStream.of(1, 2, 4, 5, 8).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).count());
        assertEquals(2, IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).count());
        assertArrayEquals(new int[] { 3, 9, 16 },
                IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).toArray());
        assertArrayEquals(new int[] { 9, 16 },
                IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).skip(1).toArray());
        assertEquals(N.asList(3, 9, 16),
                IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMap((first, next) -> next - first <= 1, (first, last) -> first + last).toList());
        assertEquals(N.asList(9, 16),
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
        assertEquals(N.asList("1-2", "4-5", "8-8"), stringStream.toList());

        stringStream = IntStream.of(1, 2, 4, 5, 8).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertEquals(N.asList("4-5", "8-8"), stringStream.skip(1).toList());

        // Test with map
        stringStream = IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertEquals(3, stringStream.count());

        stringStream = IntStream.of(1, 2, 4, 5, 8).map(e -> e).rangeMapToObj((first, next) -> next - first <= 1, (first, last) -> first + "-" + last);
        assertEquals(N.asList("1-2", "4-5", "8-8"), stringStream.toList());
    }

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

        // Test with map
        listStream = IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1);
        assertEquals(3, listStream.count());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithMerge() {
        assertEquals(3, IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 6, 11, 8 }, IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 8 }, IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.asList(6, 11, 8), IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.asList(11, 8), IntStream.of(1, 2, 3, 5, 6, 8).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 6, 11, 8 },
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 8 },
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.asList(6, 11, 8), IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.asList(11, 8), IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithTriPredicate() {
        assertEquals(3, IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 6, 11, 8 }, IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 8 },
                IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.asList(6, 11, 8), IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.asList(11, 8), IntStream.of(1, 2, 3, 5, 6, 8).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).count());
        assertEquals(2, IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 6, 11, 8 },
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 8 },
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.asList(6, 11, 8),
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).toList());
        assertEquals(N.asList(11, 8),
                IntStream.of(1, 2, 3, 5, 6, 8).map(e -> e).collapse((first, last, next) -> next - last <= 1, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScan() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 1, 3, 6, 10, 15 }, IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 3, 6, 10, 15 }, IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toArray());
        assertEquals(N.asList(1, 3, 6, 10, 15), IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toList());
        assertEquals(N.asList(3, 6, 10, 15), IntStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 1, 3, 6, 10, 15 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 3, 6, 10, 15 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toArray());
        assertEquals(N.asList(1, 3, 6, 10, 15), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toList());
        assertEquals(N.asList(3, 6, 10, 15), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInit() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.asList(11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toList());
        assertEquals(N.asList(13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.asList(11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toList());
        assertEquals(N.asList(13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInitIncluded() {
        assertEquals(6, IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 10, 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.asList(10, 11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(N.asList(11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toList());
        assertEquals(6, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 10, 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 11, 13, 16, 20, 25 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.asList(10, 11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(N.asList(11, 13, 16, 20, 25), IntStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrepend() {
        assertEquals(8, IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).count());
        assertEquals(7, IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).skip(1).count());
        assertArrayEquals(new int[] { -2, -1, 0, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).toArray());
        assertArrayEquals(new int[] { -1, 0, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).skip(1).toArray());
        assertEquals(N.asList(-2, -1, 0, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).toList());
        assertEquals(N.asList(-1, 0, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).prepend(-2, -1, 0).skip(1).toList());
        assertEquals(8, IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).count());
        assertEquals(7, IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).skip(1).count());
        assertArrayEquals(new int[] { -2, -1, 0, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).toArray());
        assertArrayEquals(new int[] { -1, 0, 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).skip(1).toArray());
        assertEquals(N.asList(-2, -1, 0, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).toList());
        assertEquals(N.asList(-1, 0, 1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(-2, -1, 0).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppend() {
        assertEquals(8, IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).count());
        assertEquals(7, IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }, IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8 }, IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 8), IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).toList());
        assertEquals(N.asList(2, 3, 4, 5, 6, 7, 8), IntStream.of(1, 2, 3, 4, 5).append(6, 7, 8).skip(1).toList());
        assertEquals(8, IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).count());
        assertEquals(7, IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 8), IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).toList());
        assertEquals(N.asList(2, 3, 4, 5, 6, 7, 8), IntStream.of(1, 2, 3, 4, 5).map(e -> e).append(6, 7, 8).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        // Test with empty stream
        assertEquals(3, IntStream.empty().appendIfEmpty(1, 2, 3).count());
        assertEquals(2, IntStream.empty().appendIfEmpty(1, 2, 3).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStream.empty().appendIfEmpty(1, 2, 3).toArray());
        assertArrayEquals(new int[] { 2, 3 }, IntStream.empty().appendIfEmpty(1, 2, 3).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3), IntStream.empty().appendIfEmpty(1, 2, 3).toList());
        assertEquals(N.asList(2, 3), IntStream.empty().appendIfEmpty(1, 2, 3).skip(1).toList());

        // Test with non-empty stream
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).appendIfEmpty(6, 7, 8).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTop() {
        assertEquals(3, IntStream.of(3, 1, 4, 2, 5).top(3).count());
        assertEquals(2, IntStream.of(3, 1, 4, 2, 5).top(3).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).top(3).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(3, 1, 4, 2, 5).top(3).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5), IntStream.of(3, 1, 4, 2, 5).top(3).toList());
        assertEquals(N.asList(4, 5), IntStream.of(3, 1, 4, 2, 5).top(3).skip(1).toList());
        assertEquals(3, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).count());
        assertEquals(2, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).toArray());
        assertArrayEquals(new int[] { 4, 5 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5), IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).toList());
        assertEquals(N.asList(4, 5), IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTopWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);
        assertEquals(3, IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).count());
        assertEquals(2, IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).skip(1).count());
        assertArrayEquals(new int[] { 3, 1, 2 }, IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).toArray());
        assertArrayEquals(new int[] { 1, 2 }, IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).skip(1).toArray());
        assertEquals(N.asList(3, 1, 2), IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).toList());
        assertEquals(N.asList(1, 2), IntStream.of(3, 1, 4, 2, 5).top(3, reverseComparator).skip(1).toList());
        assertEquals(3, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).count());
        assertEquals(2, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).skip(1).count());
        assertArrayEquals(new int[] { 3, 1, 2 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).toArray());
        assertArrayEquals(new int[] { 1, 2 }, IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).skip(1).toArray());
        assertEquals(N.asList(3, 1, 2), IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).toList());
        assertEquals(N.asList(1, 2), IntStream.of(3, 1, 4, 2, 5).map(e -> e).top(3, reverseComparator).skip(1).toList());
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
        assertEquals(N.asList(1, 2, 3, 4, 5, 6),
                IntStream.of(1, 3, 5).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        streamB = IntStream.of(2, 4, 6);
        assertEquals(N.asList(2, 3, 4, 5, 6),
                IntStream.of(1, 3, 5).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());

        // Test with map
        streamB = IntStream.of(2, 4, 6);
        assertEquals(6, IntStream.of(1, 3, 5).map(e -> e).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());

        streamB = IntStream.of(2, 4, 6);
        assertEquals(N.asList(1, 2, 3, 4, 5, 6),
                IntStream.of(1, 3, 5).map(e -> e).mergeWith(streamB, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
    }

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
        assertEquals(N.asList(11, 22, 33), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, (a, b) -> a + b).toList());

        streamB = IntStream.of(10, 20, 30);
        assertEquals(N.asList(22, 33), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, (a, b) -> a + b).skip(1).toList());

        // Test with map
        streamB = IntStream.of(10, 20, 30);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, (a, b) -> a + b).count());

        streamB = IntStream.of(10, 20, 30);
        assertEquals(N.asList(11, 22, 33), IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, (a, b) -> a + b).toList());
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
        assertEquals(N.asList(111, 222, 333), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, (a, b, c) -> a + b + c).toList());

        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertEquals(N.asList(222, 333), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, (a, b, c) -> a + b + c).skip(1).toList());

        // Test with map
        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertEquals(3, IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, streamC, (a, b, c) -> a + b + c).count());

        streamB = IntStream.of(10, 20, 30);
        streamC = IntStream.of(100, 200, 300);
        assertEquals(N.asList(111, 222, 333), IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, streamC, (a, b, c) -> a + b + c).toList());
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
        assertEquals(N.asList(11, 22, 103, 104, 105), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, 0, 100, (a, b) -> a + b).toList());

        streamB = IntStream.of(10, 20);
        assertEquals(N.asList(22, 103, 104, 105), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, 0, 100, (a, b) -> a + b).skip(1).toList());

        // Test with map
        streamB = IntStream.of(10, 20);
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, 0, 100, (a, b) -> a + b).count());

        streamB = IntStream.of(10, 20);
        assertEquals(N.asList(11, 22, 103, 104, 105), IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, 0, 100, (a, b) -> a + b).toList());
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
        assertEquals(N.asList(111, 522, 553, 554, 555), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).toList());

        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertEquals(N.asList(522, 553, 554, 555), IntStream.of(1, 2, 3, 4, 5).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).skip(1).toList());

        // Test with map
        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).count());

        streamB = IntStream.of(10, 20);
        streamC = IntStream.of(100);
        assertEquals(N.asList(111, 522, 553, 554, 555),
                IntStream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(streamB, streamC, 0, 50, 500, (a, b, c) -> a + b + c).toList());
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
        assertEquals(N.asList(1L, 2L, 3L, 4L, 5L), longStream.toList());

        longStream = IntStream.of(1, 2, 3, 4, 5).asLongStream();
        assertEquals(N.asList(2L, 3L, 4L, 5L), longStream.skip(1).toList());

        // Test with map
        longStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asLongStream();
        assertEquals(5, longStream.count());

        longStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asLongStream();
        assertEquals(N.asList(1L, 2L, 3L, 4L, 5L), longStream.toList());
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
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), floatStream.toList());

        floatStream = IntStream.of(1, 2, 3, 4, 5).asFloatStream();
        assertEquals(N.asList(2f, 3f, 4f, 5f), floatStream.skip(1).toList());

        // Test with map
        floatStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asFloatStream();
        assertEquals(5, floatStream.count());

        floatStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asFloatStream();
        assertEquals(N.asList(1f, 2f, 3f, 4f, 5f), floatStream.toList());
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
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), doubleStream.toList());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).asDoubleStream();
        assertEquals(N.asList(2.0, 3.0, 4.0, 5.0), doubleStream.skip(1).toList());

        // Test with map
        doubleStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream();
        assertEquals(5, doubleStream.count());

        doubleStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).asDoubleStream();
        assertEquals(N.asList(1.0, 2.0, 3.0, 4.0, 5.0), doubleStream.toList());
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

        // Test with map
        boxedStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).boxed();
        assertEquals(5, boxedStream.count());

        boxedStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).boxed();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), boxedStream.toList());
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

        // Test with map
        jdkStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).toJdkStream();
        assertEquals(5, jdkStream.count());

        jdkStream = IntStream.of(1, 2, 3, 4, 5).map(e -> e).toJdkStream();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), jdkStream.boxed().collect(java.util.stream.Collectors.toList()));
    }

    @Test
    public void testStreamCreatedAfterTransformB() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2)).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2)).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2)).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2)).skip(1).toArray());
        assertEquals(N.asList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2)).toList());
        assertEquals(N.asList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2)).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2)).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2)).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2)).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2)).skip(1).toArray());
        assertEquals(N.asList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2)).toList());
        assertEquals(N.asList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTransformBDeferred() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2), true).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2), true).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2), true).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2), true).skip(1).toArray());
        assertEquals(N.asList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2), true).toList());
        assertEquals(N.asList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).transformB(s -> s.map(i -> i * 2), true).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2), true).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2), true).skip(1).count());
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2), true).toArray());
        assertArrayEquals(new int[] { 4, 6, 8, 10 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2), true).skip(1).toArray());
        assertEquals(N.asList(2, 4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2), true).toList());
        assertEquals(N.asList(4, 6, 8, 10), IntStream.of(1, 2, 3, 4, 5).map(e -> e).transformB(s -> s.map(i -> i * 2), true).skip(1).toList());
    }

    // Tests for static methods

    @Test
    public void testStreamCreatedFromEmpty() {
        assertEquals(0, IntStream.empty().count());
        assertEquals(0, IntStream.empty().skip(1).count());
        assertArrayEquals(new int[] {}, IntStream.empty().toArray());
        assertArrayEquals(new int[] {}, IntStream.empty().skip(1).toArray());
        assertEquals(N.asList(), IntStream.empty().toList());
        assertEquals(N.asList(), IntStream.empty().skip(1).toList());
        assertEquals(0, IntStream.empty().map(e -> e).count());
        assertEquals(0, IntStream.empty().map(e -> e).skip(1).count());
        assertArrayEquals(new int[] {}, IntStream.empty().map(e -> e).toArray());
        assertArrayEquals(new int[] {}, IntStream.empty().map(e -> e).skip(1).toArray());
        assertEquals(N.asList(), IntStream.empty().map(e -> e).toList());
        assertEquals(N.asList(), IntStream.empty().map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromDefer() {
        assertEquals(5, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).count());
        assertEquals(4, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).skip(1).toList());
        assertEquals(5, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).count());
        assertEquals(4, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.defer(() -> IntStream.of(1, 2, 3, 4, 5)).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.from(jdkStream).toList());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(N.asList(2, 3, 4, 5), IntStream.from(jdkStream).skip(1).toList());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(5, IntStream.from(jdkStream).map(e -> e).count());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(4, IntStream.from(jdkStream).map(e -> e).skip(1).count());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.from(jdkStream).map(e -> e).toArray());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.from(jdkStream).map(e -> e).skip(1).toArray());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.from(jdkStream).map(e -> e).toList());

        jdkStream = java.util.stream.IntStream.of(1, 2, 3, 4, 5);
        assertEquals(N.asList(2, 3, 4, 5), IntStream.from(jdkStream).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfNullable() {
        assertEquals(1, IntStream.ofNullable(5).count());
        assertEquals(0, IntStream.ofNullable(5).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.ofNullable(5).toArray());
        assertArrayEquals(new int[] {}, IntStream.ofNullable(5).skip(1).toArray());
        assertEquals(N.asList(5), IntStream.ofNullable(5).toList());
        assertEquals(N.asList(), IntStream.ofNullable(5).skip(1).toList());
        assertEquals(1, IntStream.ofNullable(5).map(e -> e).count());
        assertEquals(0, IntStream.ofNullable(5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.ofNullable(5).map(e -> e).toArray());
        assertArrayEquals(new int[] {}, IntStream.ofNullable(5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(5), IntStream.ofNullable(5).map(e -> e).toList());
        assertEquals(N.asList(), IntStream.ofNullable(5).map(e -> e).skip(1).toList());

        // Test with null
        assertEquals(0, IntStream.ofNullable(null).count());
        assertEquals(0, IntStream.ofNullable(null).skip(1).count());
        assertArrayEquals(new int[] {}, IntStream.ofNullable(null).toArray());
        assertArrayEquals(new int[] {}, IntStream.ofNullable(null).skip(1).toArray());
        assertEquals(N.asList(), IntStream.ofNullable(null).toList());
        assertEquals(N.asList(), IntStream.ofNullable(null).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfArray() {
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).skip(1).toList());
        assertEquals(5, IntStream.of(1, 2, 3, 4, 5).map(e -> e).count());
        assertEquals(4, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.of(1, 2, 3, 4, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfArrayWithRange() {
        int[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        assertEquals(5, IntStream.of(array, 2, 7).count());
        assertEquals(4, IntStream.of(array, 2, 7).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5, 6, 7 }, IntStream.of(array, 2, 7).toArray());
        assertArrayEquals(new int[] { 4, 5, 6, 7 }, IntStream.of(array, 2, 7).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5, 6, 7), IntStream.of(array, 2, 7).toList());
        assertEquals(N.asList(4, 5, 6, 7), IntStream.of(array, 2, 7).skip(1).toList());
        assertEquals(5, IntStream.of(array, 2, 7).map(e -> e).count());
        assertEquals(4, IntStream.of(array, 2, 7).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5, 6, 7 }, IntStream.of(array, 2, 7).map(e -> e).toArray());
        assertArrayEquals(new int[] { 4, 5, 6, 7 }, IntStream.of(array, 2, 7).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5, 6, 7), IntStream.of(array, 2, 7).map(e -> e).toList());
        assertEquals(N.asList(4, 5, 6, 7), IntStream.of(array, 2, 7).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfIntegerArray() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        assertEquals(5, IntStream.of(array).count());
        assertEquals(4, IntStream.of(array).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(array).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(array).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.of(array).toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.of(array).skip(1).toList());
        assertEquals(5, IntStream.of(array).map(e -> e).count());
        assertEquals(4, IntStream.of(array).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(array).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(array).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.of(array).map(e -> e).toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.of(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfIntegerArrayWithRange() {
        Integer[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        assertEquals(5, IntStream.of(array, 2, 7).count());
        assertEquals(4, IntStream.of(array, 2, 7).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5, 6, 7 }, IntStream.of(array, 2, 7).toArray());
        assertArrayEquals(new int[] { 4, 5, 6, 7 }, IntStream.of(array, 2, 7).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5, 6, 7), IntStream.of(array, 2, 7).toList());
        assertEquals(N.asList(4, 5, 6, 7), IntStream.of(array, 2, 7).skip(1).toList());
        assertEquals(5, IntStream.of(array, 2, 7).map(e -> e).count());
        assertEquals(4, IntStream.of(array, 2, 7).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 3, 4, 5, 6, 7 }, IntStream.of(array, 2, 7).map(e -> e).toArray());
        assertArrayEquals(new int[] { 4, 5, 6, 7 }, IntStream.of(array, 2, 7).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(3, 4, 5, 6, 7), IntStream.of(array, 2, 7).map(e -> e).toList());
        assertEquals(N.asList(4, 5, 6, 7), IntStream.of(array, 2, 7).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfCollection() {
        Collection<Integer> collection = Arrays.asList(1, 2, 3, 4, 5);
        assertEquals(5, IntStream.of(collection).count());
        assertEquals(4, IntStream.of(collection).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(collection).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(collection).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.of(collection).toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.of(collection).skip(1).toList());
        assertEquals(5, IntStream.of(collection).map(e -> e).count());
        assertEquals(4, IntStream.of(collection).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(collection).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(collection).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.of(collection).map(e -> e).toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.of(collection).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.of(iterator).toList());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(N.asList(2, 3, 4, 5), IntStream.of(iterator).skip(1).toList());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(5, IntStream.of(iterator).map(e -> e).count());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(4, IntStream.of(iterator).map(e -> e).skip(1).count());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(iterator).map(e -> e).toArray());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(iterator).map(e -> e).skip(1).toArray());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.of(iterator).map(e -> e).toList());

        iterator = IntIterator.of(1, 2, 3, 4, 5);
        assertEquals(N.asList(2, 3, 4, 5), IntStream.of(iterator).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfOptionalInt() {
        OptionalInt op = OptionalInt.of(5);
        assertEquals(1, IntStream.of(op).count());
        assertEquals(0, IntStream.of(op).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.of(op).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).skip(1).toArray());
        assertEquals(N.asList(5), IntStream.of(op).toList());
        assertEquals(N.asList(), IntStream.of(op).skip(1).toList());
        assertEquals(1, IntStream.of(op).map(e -> e).count());
        assertEquals(0, IntStream.of(op).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.of(op).map(e -> e).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(5), IntStream.of(op).map(e -> e).toList());
        assertEquals(N.asList(), IntStream.of(op).map(e -> e).skip(1).toList());

        // Test with empty optional
        op = OptionalInt.empty();
        assertEquals(0, IntStream.of(op).count());
        assertEquals(0, IntStream.of(op).skip(1).count());
        assertArrayEquals(new int[] {}, IntStream.of(op).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).skip(1).toArray());
        assertEquals(N.asList(), IntStream.of(op).toList());
        assertEquals(N.asList(), IntStream.of(op).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfJavaOptionalInt() {
        java.util.OptionalInt op = java.util.OptionalInt.of(5);
        assertEquals(1, IntStream.of(op).count());
        assertEquals(0, IntStream.of(op).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.of(op).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).skip(1).toArray());
        assertEquals(N.asList(5), IntStream.of(op).toList());
        assertEquals(N.asList(), IntStream.of(op).skip(1).toList());
        assertEquals(1, IntStream.of(op).map(e -> e).count());
        assertEquals(0, IntStream.of(op).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 5 }, IntStream.of(op).map(e -> e).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(5), IntStream.of(op).map(e -> e).toList());
        assertEquals(N.asList(), IntStream.of(op).map(e -> e).skip(1).toList());

        // Test with empty optional
        op = java.util.OptionalInt.empty();
        assertEquals(0, IntStream.of(op).count());
        assertEquals(0, IntStream.of(op).skip(1).count());
        assertArrayEquals(new int[] {}, IntStream.of(op).toArray());
        assertArrayEquals(new int[] {}, IntStream.of(op).skip(1).toArray());
        assertEquals(N.asList(), IntStream.of(op).toList());
        assertEquals(N.asList(), IntStream.of(op).skip(1).toList());
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
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.of(buffer).toList());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList(2, 3, 4, 5), IntStream.of(buffer).skip(1).toList());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(5, IntStream.of(buffer).map(e -> e).count());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(4, IntStream.of(buffer).map(e -> e).skip(1).count());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.of(buffer).map(e -> e).toArray());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.of(buffer).map(e -> e).skip(1).toArray());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.of(buffer).map(e -> e).toList());

        buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList(2, 3, 4, 5), IntStream.of(buffer).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfCodePoints() {
        String str = "Hello";
        assertEquals(5, IntStream.ofCodePoints(str).count());
        assertEquals(4, IntStream.ofCodePoints(str).skip(1).count());
        assertArrayEquals(new int[] { 72, 101, 108, 108, 111 }, IntStream.ofCodePoints(str).toArray());
        assertArrayEquals(new int[] { 101, 108, 108, 111 }, IntStream.ofCodePoints(str).skip(1).toArray());
        assertEquals(N.asList(72, 101, 108, 108, 111), IntStream.ofCodePoints(str).toList());
        assertEquals(N.asList(101, 108, 108, 111), IntStream.ofCodePoints(str).skip(1).toList());
        assertEquals(5, IntStream.ofCodePoints(str).map(e -> e).count());
        assertEquals(4, IntStream.ofCodePoints(str).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 72, 101, 108, 108, 111 }, IntStream.ofCodePoints(str).map(e -> e).toArray());
        assertArrayEquals(new int[] { 101, 108, 108, 111 }, IntStream.ofCodePoints(str).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(72, 101, 108, 108, 111), IntStream.ofCodePoints(str).map(e -> e).toList());
        assertEquals(N.asList(101, 108, 108, 111), IntStream.ofCodePoints(str).map(e -> e).skip(1).toList());
    }

    // Tests for remaining static methods (splitByChunkCount through merge)

    @Test
    public void testStreamCreatedFromSplitByChunkCount() {
        assertEquals(3, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).count());
        assertEquals(2, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).skip(1).count());
        assertArrayEquals(new int[] { 4, 3, 3 }, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).toArray());
        assertArrayEquals(new int[] { 3, 3 }, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).skip(1).toArray());
        assertEquals(N.asList(4, 3, 3), IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).toList());
        assertEquals(N.asList(3, 3), IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).skip(1).toList());
        assertEquals(3, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).count());
        assertEquals(2, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 4, 3, 3 }, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).toArray());
        assertArrayEquals(new int[] { 3, 3 }, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(4, 3, 3), IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).toList());
        assertEquals(N.asList(3, 3), IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromSplitByChunkCountWithSizeSmallerFirst() {
        assertEquals(5, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).count());
        assertEquals(4, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).skip(1).count());
        assertArrayEquals(new int[] { 1, 1, 1, 2, 2 }, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).toArray());
        assertArrayEquals(new int[] { 1, 1, 2, 2 }, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).skip(1).toArray());
        assertEquals(N.asList(1, 1, 1, 2, 2), IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).toList());
        assertEquals(N.asList(1, 1, 2, 2), IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).skip(1).toList());
        assertEquals(5, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).count());
        assertEquals(4, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 1, 1, 2, 2 }, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).toArray());
        assertArrayEquals(new int[] { 1, 1, 2, 2 }, IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(1, 1, 1, 2, 2), IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).toList());
        assertEquals(N.asList(1, 1, 2, 2), IntStream.splitByChunkCount(7, 5, true, (from, to) -> to - from).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromFlatten() {
        int[][] array = { { 1, 2 }, { 3, 4 }, { 5 } };
        assertEquals(5, IntStream.flatten(array).count());
        assertEquals(4, IntStream.flatten(array).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.flatten(array).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.flatten(array).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.flatten(array).toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.flatten(array).skip(1).toList());
        assertEquals(5, IntStream.flatten(array).map(e -> e).count());
        assertEquals(4, IntStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.flatten(array).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.flatten(array).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.flatten(array).map(e -> e).toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromFlattenVertically() {
        int[][] array = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        assertEquals(9, IntStream.flatten(array, true).count());
        assertEquals(8, IntStream.flatten(array, true).skip(1).count());
        assertArrayEquals(new int[] { 1, 4, 7, 2, 5, 8, 3, 6, 9 }, IntStream.flatten(array, true).toArray());
        assertArrayEquals(new int[] { 4, 7, 2, 5, 8, 3, 6, 9 }, IntStream.flatten(array, true).skip(1).toArray());
        assertEquals(N.asList(1, 4, 7, 2, 5, 8, 3, 6, 9), IntStream.flatten(array, true).toList());
        assertEquals(N.asList(4, 7, 2, 5, 8, 3, 6, 9), IntStream.flatten(array, true).skip(1).toList());
        assertEquals(9, IntStream.flatten(array, true).map(e -> e).count());
        assertEquals(8, IntStream.flatten(array, true).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 4, 7, 2, 5, 8, 3, 6, 9 }, IntStream.flatten(array, true).map(e -> e).toArray());
        assertArrayEquals(new int[] { 4, 7, 2, 5, 8, 3, 6, 9 }, IntStream.flatten(array, true).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(1, 4, 7, 2, 5, 8, 3, 6, 9), IntStream.flatten(array, true).map(e -> e).toList());
        assertEquals(N.asList(4, 7, 2, 5, 8, 3, 6, 9), IntStream.flatten(array, true).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromFlattenWithAlignment() {
        int[][] array = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        assertEquals(9, IntStream.flatten(array, 0, true).count());
        assertEquals(8, IntStream.flatten(array, 0, true).skip(1).count());
        assertArrayEquals(new int[] { 1, 3, 6, 2, 4, 0, 0, 5, 0 }, IntStream.flatten(array, 0, true).toArray());
        assertArrayEquals(new int[] { 3, 6, 2, 4, 0, 0, 5, 0 }, IntStream.flatten(array, 0, true).skip(1).toArray());
        assertEquals(N.asList(1, 3, 6, 2, 4, 0, 0, 5, 0), IntStream.flatten(array, 0, true).toList());
        assertEquals(N.asList(3, 6, 2, 4, 0, 0, 5, 0), IntStream.flatten(array, 0, true).skip(1).toList());
        assertEquals(9, IntStream.flatten(array, 0, true).map(e -> e).count());
        assertEquals(8, IntStream.flatten(array, 0, true).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 3, 6, 2, 4, 0, 0, 5, 0 }, IntStream.flatten(array, 0, true).map(e -> e).toArray());
        assertArrayEquals(new int[] { 3, 6, 2, 4, 0, 0, 5, 0 }, IntStream.flatten(array, 0, true).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(1, 3, 6, 2, 4, 0, 0, 5, 0), IntStream.flatten(array, 0, true).map(e -> e).toList());
        assertEquals(N.asList(3, 6, 2, 4, 0, 0, 5, 0), IntStream.flatten(array, 0, true).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromFlatten3D() {
        int[][][] array = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } };
        assertEquals(8, IntStream.flatten(array).count());
        assertEquals(7, IntStream.flatten(array).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }, IntStream.flatten(array).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8 }, IntStream.flatten(array).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 8), IntStream.flatten(array).toList());
        assertEquals(N.asList(2, 3, 4, 5, 6, 7, 8), IntStream.flatten(array).skip(1).toList());
        assertEquals(8, IntStream.flatten(array).map(e -> e).count());
        assertEquals(7, IntStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }, IntStream.flatten(array).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8 }, IntStream.flatten(array).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 8), IntStream.flatten(array).map(e -> e).toList());
        assertEquals(N.asList(2, 3, 4, 5, 6, 7, 8), IntStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromRange() {
        assertEquals(5, IntStream.range(0, 5).count());
        assertEquals(4, IntStream.range(0, 5).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.range(0, 5).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.range(0, 5).skip(1).toArray());
        assertEquals(N.asList(0, 1, 2, 3, 4), IntStream.range(0, 5).toList());
        assertEquals(N.asList(1, 2, 3, 4), IntStream.range(0, 5).skip(1).toList());
        assertEquals(5, IntStream.range(0, 5).map(e -> e).count());
        assertEquals(4, IntStream.range(0, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.range(0, 5).map(e -> e).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.range(0, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(0, 1, 2, 3, 4), IntStream.range(0, 5).map(e -> e).toList());
        assertEquals(N.asList(1, 2, 3, 4), IntStream.range(0, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromRangeWithStep() {
        assertEquals(4, IntStream.range(0, 10, 3).count());
        assertEquals(3, IntStream.range(0, 10, 3).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.range(0, 10, 3).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.range(0, 10, 3).skip(1).toArray());
        assertEquals(N.asList(0, 3, 6, 9), IntStream.range(0, 10, 3).toList());
        assertEquals(N.asList(3, 6, 9), IntStream.range(0, 10, 3).skip(1).toList());
        assertEquals(4, IntStream.range(0, 10, 3).map(e -> e).count());
        assertEquals(3, IntStream.range(0, 10, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.range(0, 10, 3).map(e -> e).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.range(0, 10, 3).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(0, 3, 6, 9), IntStream.range(0, 10, 3).map(e -> e).toList());
        assertEquals(N.asList(3, 6, 9), IntStream.range(0, 10, 3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromRangeClosed() {
        assertEquals(6, IntStream.rangeClosed(0, 5).count());
        assertEquals(5, IntStream.rangeClosed(0, 5).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, IntStream.rangeClosed(0, 5).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.rangeClosed(0, 5).skip(1).toArray());
        assertEquals(N.asList(0, 1, 2, 3, 4, 5), IntStream.rangeClosed(0, 5).toList());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.rangeClosed(0, 5).skip(1).toList());
        assertEquals(6, IntStream.rangeClosed(0, 5).map(e -> e).count());
        assertEquals(5, IntStream.rangeClosed(0, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, IntStream.rangeClosed(0, 5).map(e -> e).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.rangeClosed(0, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(0, 1, 2, 3, 4, 5), IntStream.rangeClosed(0, 5).map(e -> e).toList());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.rangeClosed(0, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromRangeClosedWithStep() {
        assertEquals(4, IntStream.rangeClosed(0, 9, 3).count());
        assertEquals(3, IntStream.rangeClosed(0, 9, 3).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.rangeClosed(0, 9, 3).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.rangeClosed(0, 9, 3).skip(1).toArray());
        assertEquals(N.asList(0, 3, 6, 9), IntStream.rangeClosed(0, 9, 3).toList());
        assertEquals(N.asList(3, 6, 9), IntStream.rangeClosed(0, 9, 3).skip(1).toList());
        assertEquals(4, IntStream.rangeClosed(0, 9, 3).map(e -> e).count());
        assertEquals(3, IntStream.rangeClosed(0, 9, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.rangeClosed(0, 9, 3).map(e -> e).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.rangeClosed(0, 9, 3).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(0, 3, 6, 9), IntStream.rangeClosed(0, 9, 3).map(e -> e).toList());
        assertEquals(N.asList(3, 6, 9), IntStream.rangeClosed(0, 9, 3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromRepeat() {
        assertEquals(5, IntStream.repeat(7, 5).count());
        assertEquals(4, IntStream.repeat(7, 5).skip(1).count());
        assertArrayEquals(new int[] { 7, 7, 7, 7, 7 }, IntStream.repeat(7, 5).toArray());
        assertArrayEquals(new int[] { 7, 7, 7, 7 }, IntStream.repeat(7, 5).skip(1).toArray());
        assertEquals(N.asList(7, 7, 7, 7, 7), IntStream.repeat(7, 5).toList());
        assertEquals(N.asList(7, 7, 7, 7), IntStream.repeat(7, 5).skip(1).toList());
        assertEquals(5, IntStream.repeat(7, 5).map(e -> e).count());
        assertEquals(4, IntStream.repeat(7, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 7, 7, 7, 7, 7 }, IntStream.repeat(7, 5).map(e -> e).toArray());
        assertArrayEquals(new int[] { 7, 7, 7, 7 }, IntStream.repeat(7, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(7, 7, 7, 7, 7), IntStream.repeat(7, 5).map(e -> e).toList());
        assertEquals(N.asList(7, 7, 7, 7), IntStream.repeat(7, 5).map(e -> e).skip(1).toList());
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
    public void testStreamCreatedFromOfIndices() {
        assertEquals(5, IntStream.ofIndices(5).count());
        assertEquals(4, IntStream.ofIndices(5).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.ofIndices(5).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.ofIndices(5).skip(1).toArray());
        assertEquals(N.asList(0, 1, 2, 3, 4), IntStream.ofIndices(5).toList());
        assertEquals(N.asList(1, 2, 3, 4), IntStream.ofIndices(5).skip(1).toList());
        assertEquals(5, IntStream.ofIndices(5).map(e -> e).count());
        assertEquals(4, IntStream.ofIndices(5).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.ofIndices(5).map(e -> e).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.ofIndices(5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(0, 1, 2, 3, 4), IntStream.ofIndices(5).map(e -> e).toList());
        assertEquals(N.asList(1, 2, 3, 4), IntStream.ofIndices(5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromOfIndicesWithStep() {
        assertEquals(4, IntStream.ofIndices(10, 3).count());
        assertEquals(3, IntStream.ofIndices(10, 3).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.ofIndices(10, 3).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.ofIndices(10, 3).skip(1).toArray());
        assertEquals(N.asList(0, 3, 6, 9), IntStream.ofIndices(10, 3).toList());
        assertEquals(N.asList(3, 6, 9), IntStream.ofIndices(10, 3).skip(1).toList());
        assertEquals(4, IntStream.ofIndices(10, 3).map(e -> e).count());
        assertEquals(3, IntStream.ofIndices(10, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.ofIndices(10, 3).map(e -> e).toArray());
        assertArrayEquals(new int[] { 3, 6, 9 }, IntStream.ofIndices(10, 3).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(0, 3, 6, 9), IntStream.ofIndices(10, 3).map(e -> e).toList());
        assertEquals(N.asList(3, 6, 9), IntStream.ofIndices(10, 3).map(e -> e).skip(1).toList());
    }

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
        assertEquals(N.asList(0, 1, 2, 3, 4), IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).toList());

        counter.set(0);
        assertEquals(N.asList(1, 2, 3, 4), IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).skip(1).toList());

        counter.set(0);
        assertEquals(5, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).count());

        counter.set(0);
        assertEquals(4, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).skip(1).count());

        counter.set(0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).toArray());

        counter.set(0);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).skip(1).toArray());

        counter.set(0);
        assertEquals(N.asList(0, 1, 2, 3, 4), IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).toList());

        counter.set(0);
        assertEquals(N.asList(1, 2, 3, 4), IntStream.iterate(() -> counter.get() < 5, () -> counter.getAndIncrement()).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(0, 1, 2, 3, 4, 5), IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).toList());

        counter.set(0);
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.iterate(0, () -> counter.get() < 5, i -> {
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
        assertEquals(N.asList(0, 1, 2, 3, 4, 5), IntStream.iterate(0, () -> counter.get() < 5, i -> {
            counter.incrementAndGet();
            return i + 1;
        }).map(e -> e).toList());

        counter.set(0);
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.iterate(0, () -> counter.get() < 5, i -> {
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
        assertEquals(N.asList(0, 1, 2, 3, 4), IntStream.iterate(0, i -> i < 5, i -> i + 1).toList());
        assertEquals(N.asList(1, 2, 3, 4), IntStream.iterate(0, i -> i < 5, i -> i + 1).skip(1).toList());
        assertEquals(5, IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).count());
        assertEquals(4, IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(0, 1, 2, 3, 4), IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).toList());
        assertEquals(N.asList(1, 2, 3, 4), IntStream.iterate(0, i -> i < 5, i -> i + 1).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromIterateInfinite() {
        assertEquals(10, IntStream.iterate(0, i -> i + 1).limit(10).count());
        assertEquals(9, IntStream.iterate(0, i -> i + 1).limit(10).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, IntStream.iterate(0, i -> i + 1).limit(10).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, IntStream.iterate(0, i -> i + 1).limit(10).skip(1).toArray());
        assertEquals(N.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), IntStream.iterate(0, i -> i + 1).limit(10).toList());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), IntStream.iterate(0, i -> i + 1).limit(10).skip(1).toList());
        assertEquals(10, IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).count());
        assertEquals(9, IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).skip(1).count());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).toArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).skip(1).toArray());
        assertEquals(N.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).toList());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), IntStream.iterate(0, i -> i + 1).map(e -> e).limit(10).skip(1).toList());
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
        assertEquals(N.asList(0, 1, 2, 3, 4), IntStream.generate(() -> counter.getAndIncrement()).limit(5).toList());

        counter.set(0);
        assertEquals(N.asList(1, 2, 3, 4), IntStream.generate(() -> counter.getAndIncrement()).limit(5).skip(1).toList());

        counter.set(0);
        assertEquals(5, IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).count());

        counter.set(0);
        assertEquals(4, IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).skip(1).count());

        counter.set(0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).toArray());

        counter.set(0);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).skip(1).toArray());

        counter.set(0);
        assertEquals(N.asList(0, 1, 2, 3, 4), IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).toList());

        counter.set(0);
        assertEquals(N.asList(1, 2, 3, 4), IntStream.generate(() -> counter.getAndIncrement()).map(e -> e).limit(5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromConcatArrays() {
        int[][] arrays = { { 1, 2 }, { 3, 4 }, { 5 } };
        assertEquals(5, IntStream.concat(arrays).count());
        assertEquals(4, IntStream.concat(arrays).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.concat(arrays).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.concat(arrays).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.concat(arrays).toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.concat(arrays).skip(1).toList());
        assertEquals(5, IntStream.concat(arrays).map(e -> e).count());
        assertEquals(4, IntStream.concat(arrays).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.concat(arrays).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, IntStream.concat(arrays).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.concat(arrays).map(e -> e).toList());
        assertEquals(N.asList(2, 3, 4, 5), IntStream.concat(arrays).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.concat(iter1, iter2, iter3).toList());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertEquals(N.asList(2, 3, 4, 5), IntStream.concat(iter1, iter2, iter3).skip(1).toList());

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
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.concat(iter1, iter2, iter3).map(e -> e).toList());

        iter1 = IntIterator.of(1, 2);
        iter2 = IntIterator.of(3, 4);
        iter3 = IntIterator.of(5);
        assertEquals(N.asList(2, 3, 4, 5), IntStream.concat(iter1, iter2, iter3).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.concat(stream1, stream2, stream3).toList());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertEquals(N.asList(2, 3, 4, 5), IntStream.concat(stream1, stream2, stream3).skip(1).toList());

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
        assertEquals(N.asList(1, 2, 3, 4, 5), IntStream.concat(stream1, stream2, stream3).map(e -> e).toList());

        stream1 = IntStream.of(1, 2);
        stream2 = IntStream.of(3, 4);
        stream3 = IntStream.of(5);
        assertEquals(N.asList(2, 3, 4, 5), IntStream.concat(stream1, stream2, stream3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedFromZipArrays() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 10, 20, 30 };
        assertEquals(3, IntStream.zip(array1, array2, (a, b) -> a + b).count());
        assertEquals(2, IntStream.zip(array1, array2, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new int[] { 11, 22, 33 }, IntStream.zip(array1, array2, (a, b) -> a + b).toArray());
        assertArrayEquals(new int[] { 22, 33 }, IntStream.zip(array1, array2, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.asList(11, 22, 33), IntStream.zip(array1, array2, (a, b) -> a + b).toList());
        assertEquals(N.asList(22, 33), IntStream.zip(array1, array2, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).count());
        assertEquals(2, IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 11, 22, 33 }, IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).toArray());
        assertArrayEquals(new int[] { 22, 33 }, IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(11, 22, 33), IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).toList());
        assertEquals(N.asList(22, 33), IntStream.zip(array1, array2, (a, b) -> a + b).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(111, 222, 333), IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).toList());
        assertEquals(N.asList(222, 333), IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).skip(1).toList());
        assertEquals(3, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).count());
        assertEquals(2, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 111, 222, 333 }, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).toArray());
        assertArrayEquals(new int[] { 222, 333 }, IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(111, 222, 333), IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).toList());
        assertEquals(N.asList(222, 333), IntStream.zip(array1, array2, array3, (a, b, c) -> a + b + c).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(1, 2, 3, 4, 5, 6), IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.asList(2, 3, 4, 5, 6),
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        assertEquals(6, IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        assertEquals(5, IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 },
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6 },
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6),
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        assertEquals(N.asList(2, 3, 4, 5, 6),
                IntStream.merge(array1, array2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
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
        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.asList(2, 3, 4, 5, 6, 7, 8, 9),
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        assertEquals(9, IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        assertEquals(8,
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        assertEquals(N.asList(2, 3, 4, 5, 6, 7, 8, 9),
                IntStream.merge(array1, array2, array3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }
}
