package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongNFunction;

public class LongStream104Test extends TestBase {
    @Test
    public void testFilter() {
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).count());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new long[] { 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toArray());
        assertArrayEquals(new long[] { 4, 5 }, LongStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toList());
        assertEquals(Arrays.asList(4L, 5L), LongStream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new long[] { 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toArray());
        assertArrayEquals(new long[] { 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(Arrays.asList(4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testFilterWithAction() {
        List<Long> dropped = new ArrayList<>();
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).count());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).skip(1).count());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertArrayEquals(new long[] { 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).toArray());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertArrayEquals(new long[] { 4, 5 }, LongStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertEquals(Arrays.asList(3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).toList());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertEquals(Arrays.asList(4L, 5L), LongStream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).skip(1).toList());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).count());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).count());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertArrayEquals(new long[] { 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).toArray());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertArrayEquals(new long[] { 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertEquals(Arrays.asList(3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).toList());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertEquals(Arrays.asList(4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toList());
        assertEquals(Arrays.asList(1L, 2L), dropped);
    }

    @Test
    public void testTakeWhile() {
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).count());
        assertEquals(1, LongStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new long[] { 1, 2 }, LongStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new long[] { 2 }, LongStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L), LongStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toList());
        assertEquals(Arrays.asList(2L), LongStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toList());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).count());
        assertEquals(1, LongStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new long[] { 1, 2 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new long[] { 2 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toList());
        assertEquals(Arrays.asList(2L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testDropWhile() {
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).count());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new long[] { 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new long[] { 4, 5 }, LongStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toList());
        assertEquals(Arrays.asList(4L, 5L), LongStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toList());
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).count());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new long[] { 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new long[] { 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toList());
        assertEquals(Arrays.asList(4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testDropWhileWithAction() {
        List<Long> dropped = new ArrayList<>();
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).count());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).skip(1).count());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertArrayEquals(new long[] { 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).toArray());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertArrayEquals(new long[] { 4, 5 }, LongStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertEquals(Arrays.asList(3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).toList());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertEquals(Arrays.asList(4L, 5L), LongStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).skip(1).toList());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).count());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).count());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertArrayEquals(new long[] { 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toArray());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertArrayEquals(new long[] { 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertEquals(Arrays.asList(3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toList());
        assertEquals(Arrays.asList(1L, 2L), dropped);

        dropped.clear();
        assertEquals(Arrays.asList(4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toList());
        assertEquals(Arrays.asList(1L, 2L), dropped);
    }

    @Test
    public void testSkipUntil() {
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).count());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new long[] { 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new long[] { 4, 5 }, LongStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).toList());
        assertEquals(Arrays.asList(4L, 5L), LongStream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).toList());
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).count());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new long[] { 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new long[] { 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).toList());
        assertEquals(Arrays.asList(4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toList());
    }

    @Test
    public void testDistinct() {
        assertEquals(4, LongStream.of(1, 2, 2, 3, 3, 3, 4).distinct().count());
        assertEquals(3, LongStream.of(1, 2, 2, 3, 3, 3, 4).distinct().skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4 }, LongStream.of(1, 2, 2, 3, 3, 3, 4).distinct().toArray());
        assertArrayEquals(new long[] { 2, 3, 4 }, LongStream.of(1, 2, 2, 3, 3, 3, 4).distinct().skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L), LongStream.of(1, 2, 2, 3, 3, 3, 4).distinct().toList());
        assertEquals(Arrays.asList(2L, 3L, 4L), LongStream.of(1, 2, 2, 3, 3, 3, 4).distinct().skip(1).toList());
        assertEquals(4, LongStream.of(1, 2, 2, 3, 3, 3, 4).map(e -> e).distinct().count());
        assertEquals(3, LongStream.of(1, 2, 2, 3, 3, 3, 4).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4 }, LongStream.of(1, 2, 2, 3, 3, 3, 4).map(e -> e).distinct().toArray());
        assertArrayEquals(new long[] { 2, 3, 4 }, LongStream.of(1, 2, 2, 3, 3, 3, 4).map(e -> e).distinct().skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L), LongStream.of(1, 2, 2, 3, 3, 3, 4).map(e -> e).distinct().toList());
        assertEquals(Arrays.asList(2L, 3L, 4L), LongStream.of(1, 2, 2, 3, 3, 3, 4).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testIntersection() {
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2L, 3L, 4L, 6L)).count());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).count());
        assertArrayEquals(new long[] { 2, 3, 4 }, LongStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2L, 3L, 4L, 6L)).toArray());
        assertArrayEquals(new long[] { 3, 4 }, LongStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).toArray());
        assertEquals(Arrays.asList(2L, 3L, 4L), LongStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2L, 3L, 4L, 6L)).toList());
        assertEquals(Arrays.asList(3L, 4L), LongStream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).toList());
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2L, 3L, 4L, 6L)).count());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).count());
        assertArrayEquals(new long[] { 2, 3, 4 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2L, 3L, 4L, 6L)).toArray());
        assertArrayEquals(new long[] { 3, 4 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).toArray());
        assertEquals(Arrays.asList(2L, 3L, 4L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2L, 3L, 4L, 6L)).toList());
        assertEquals(Arrays.asList(3L, 4L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).toList());
    }

    @Test
    public void testDifference() {
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2L, 3L, 4L, 6L)).count());
        assertEquals(1, LongStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).count());
        assertArrayEquals(new long[] { 1, 5 }, LongStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2L, 3L, 4L, 6L)).toArray());
        assertArrayEquals(new long[] { 5 }, LongStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 5L), LongStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2L, 3L, 4L, 6L)).toList());
        assertEquals(Arrays.asList(5L), LongStream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).toList());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2L, 3L, 4L, 6L)).count());
        assertEquals(1, LongStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).count());
        assertArrayEquals(new long[] { 1, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2L, 3L, 4L, 6L)).toArray());
        assertArrayEquals(new long[] { 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2L, 3L, 4L, 6L)).toList());
        assertEquals(Arrays.asList(5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).toList());
    }

    @Test
    public void testSymmetricDifference() {
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2L, 3L, 4L, 6L)).count());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).count());
        assertArrayEquals(new long[] { 1, 5, 6 }, LongStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2L, 3L, 4L, 6L)).toArray());
        assertArrayEquals(new long[] { 5, 6 }, LongStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 5L, 6L), LongStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2L, 3L, 4L, 6L)).toList());
        assertEquals(Arrays.asList(5L, 6L), LongStream.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).toList());
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2L, 3L, 4L, 6L)).count());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).count());
        assertArrayEquals(new long[] { 1, 5, 6 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2L, 3L, 4L, 6L)).toArray());
        assertArrayEquals(new long[] { 5, 6 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 5L, 6L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2L, 3L, 4L, 6L)).toList());
        assertEquals(Arrays.asList(5L, 6L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).symmetricDifference(Arrays.asList(2L, 3L, 4L, 6L)).skip(1).toList());
    }

    @Test
    public void testReversed() {
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).reversed().count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).reversed().skip(1).count());
        assertArrayEquals(new long[] { 5, 4, 3, 2, 1 }, LongStream.of(1, 2, 3, 4, 5).reversed().toArray());
        assertArrayEquals(new long[] { 4, 3, 2, 1 }, LongStream.of(1, 2, 3, 4, 5).reversed().skip(1).toArray());
        assertEquals(Arrays.asList(5L, 4L, 3L, 2L, 1L), LongStream.of(1, 2, 3, 4, 5).reversed().toList());
        assertEquals(Arrays.asList(4L, 3L, 2L, 1L), LongStream.of(1, 2, 3, 4, 5).reversed().skip(1).toList());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).count());
        assertArrayEquals(new long[] { 5, 4, 3, 2, 1 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toArray());
        assertArrayEquals(new long[] { 4, 3, 2, 1 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toArray());
        assertEquals(Arrays.asList(5L, 4L, 3L, 2L, 1L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toList());
        assertEquals(Arrays.asList(4L, 3L, 2L, 1L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toList());
    }

    @Test
    public void testRotated() {
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).rotated(2).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).count());
        assertArrayEquals(new long[] { 4, 5, 1, 2, 3 }, LongStream.of(1, 2, 3, 4, 5).rotated(2).toArray());
        assertArrayEquals(new long[] { 5, 1, 2, 3 }, LongStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toArray());
        assertEquals(Arrays.asList(4L, 5L, 1L, 2L, 3L), LongStream.of(1, 2, 3, 4, 5).rotated(2).toList());
        assertEquals(Arrays.asList(5L, 1L, 2L, 3L), LongStream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toList());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).count());
        assertArrayEquals(new long[] { 4, 5, 1, 2, 3 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toArray());
        assertArrayEquals(new long[] { 5, 1, 2, 3 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toArray());
        assertEquals(Arrays.asList(4L, 5L, 1L, 2L, 3L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toList());
        assertEquals(Arrays.asList(5L, 1L, 2L, 3L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toList());
    }

    @Test
    public void testShuffled() {
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).shuffled().count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).shuffled().skip(1).count());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).shuffled().toArray().length);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).shuffled().skip(1).toArray().length);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).shuffled().toList().size());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).shuffled().skip(1).toList().size());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).count());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().toArray().length);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).toArray().length);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().toList().size());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).toList().size());
    }

    @Test
    public void testShuffledWithRandom() {
        Random rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).skip(1).count());
        rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).toArray().length);
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).skip(1).toArray().length);
        rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).toList().size());
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).shuffled(rnd).skip(1).toList().size());
        rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).skip(1).count());
        rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).toArray().length);
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).skip(1).toArray().length);
        rnd = new Random(42);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).toList().size());
        rnd = new Random(42);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(rnd).skip(1).toList().size());
    }

    @Test
    public void testSorted() {
        assertEquals(5, LongStream.of(5, 3, 1, 4, 2).sorted().count());
        assertEquals(4, LongStream.of(5, 3, 1, 4, 2).sorted().skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(5, 3, 1, 4, 2).sorted().toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(5, 3, 1, 4, 2).sorted().skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(5, 3, 1, 4, 2).sorted().toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(5, 3, 1, 4, 2).sorted().skip(1).toList());
        assertEquals(5, LongStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().count());
        assertEquals(4, LongStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testReverseSorted() {
        assertEquals(5, LongStream.of(5, 3, 1, 4, 2).reverseSorted().count());
        assertEquals(4, LongStream.of(5, 3, 1, 4, 2).reverseSorted().skip(1).count());
        assertArrayEquals(new long[] { 5, 4, 3, 2, 1 }, LongStream.of(5, 3, 1, 4, 2).reverseSorted().toArray());
        assertArrayEquals(new long[] { 4, 3, 2, 1 }, LongStream.of(5, 3, 1, 4, 2).reverseSorted().skip(1).toArray());
        assertEquals(Arrays.asList(5L, 4L, 3L, 2L, 1L), LongStream.of(5, 3, 1, 4, 2).reverseSorted().toList());
        assertEquals(Arrays.asList(4L, 3L, 2L, 1L), LongStream.of(5, 3, 1, 4, 2).reverseSorted().skip(1).toList());
        assertEquals(5, LongStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().count());
        assertEquals(4, LongStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new long[] { 5, 4, 3, 2, 1 }, LongStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().toArray());
        assertArrayEquals(new long[] { 4, 3, 2, 1 }, LongStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().skip(1).toArray());
        assertEquals(Arrays.asList(5L, 4L, 3L, 2L, 1L), LongStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().toList());
        assertEquals(Arrays.asList(4L, 3L, 2L, 1L), LongStream.of(5, 3, 1, 4, 2).map(e -> e).reverseSorted().skip(1).toList());
    }

    @Test
    public void testCycled() {
        assertEquals(10, LongStream.of(1, 2, 3).cycled().limit(10).count());
        assertEquals(8, LongStream.of(1, 2, 3).cycled().limit(10).skip(2).count());
        assertArrayEquals(new long[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, LongStream.of(1, 2, 3).cycled().limit(10).toArray());
        assertArrayEquals(new long[] { 3, 1, 2, 3, 1, 2, 3, 1 }, LongStream.of(1, 2, 3).cycled().limit(10).skip(2).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 1L, 2L, 3L, 1L, 2L, 3L, 1L), LongStream.of(1, 2, 3).cycled().limit(10).toList());
        assertEquals(Arrays.asList(3L, 1L, 2L, 3L, 1L, 2L, 3L, 1L), LongStream.of(1, 2, 3).cycled().limit(10).skip(2).toList());
        assertEquals(10, LongStream.of(1, 2, 3).map(e -> e).cycled().limit(10).count());
        assertEquals(8, LongStream.of(1, 2, 3).map(e -> e).cycled().limit(10).skip(2).count());
        assertArrayEquals(new long[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, LongStream.of(1, 2, 3).map(e -> e).cycled().limit(10).toArray());
        assertArrayEquals(new long[] { 3, 1, 2, 3, 1, 2, 3, 1 }, LongStream.of(1, 2, 3).map(e -> e).cycled().limit(10).skip(2).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 1L, 2L, 3L, 1L, 2L, 3L, 1L), LongStream.of(1, 2, 3).map(e -> e).cycled().limit(10).toList());
        assertEquals(Arrays.asList(3L, 1L, 2L, 3L, 1L, 2L, 3L, 1L), LongStream.of(1, 2, 3).map(e -> e).cycled().limit(10).skip(2).toList());
    }

    @Test
    public void testCycledWithRounds() {
        assertEquals(6, LongStream.of(1, 2, 3).cycled(2).count());
        assertEquals(4, LongStream.of(1, 2, 3).cycled(2).skip(2).count());
        assertArrayEquals(new long[] { 1, 2, 3, 1, 2, 3 }, LongStream.of(1, 2, 3).cycled(2).toArray());
        assertArrayEquals(new long[] { 3, 1, 2, 3 }, LongStream.of(1, 2, 3).cycled(2).skip(2).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 1L, 2L, 3L), LongStream.of(1, 2, 3).cycled(2).toList());
        assertEquals(Arrays.asList(3L, 1L, 2L, 3L), LongStream.of(1, 2, 3).cycled(2).skip(2).toList());
        assertEquals(6, LongStream.of(1, 2, 3).map(e -> e).cycled(2).count());
        assertEquals(4, LongStream.of(1, 2, 3).map(e -> e).cycled(2).skip(2).count());
        assertArrayEquals(new long[] { 1, 2, 3, 1, 2, 3 }, LongStream.of(1, 2, 3).map(e -> e).cycled(2).toArray());
        assertArrayEquals(new long[] { 3, 1, 2, 3 }, LongStream.of(1, 2, 3).map(e -> e).cycled(2).skip(2).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 1L, 2L, 3L), LongStream.of(1, 2, 3).map(e -> e).cycled(2).toList());
        assertEquals(Arrays.asList(3L, 1L, 2L, 3L), LongStream.of(1, 2, 3).map(e -> e).cycled(2).skip(2).toList());
    }

    // Note: indexed() returns Stream<IndexedLong>, not LongStream, so we need to adjust our tests
    @Test
    public void testIndexed() {
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).indexed().count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).indexed().skip(1).count());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).indexed().toArray().length);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).indexed().skip(1).toArray().length);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).indexed().toList().size());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).indexed().skip(1).toList().size());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).indexed().count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).indexed().skip(1).count());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).indexed().toArray().length);
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).indexed().skip(1).toArray().length);
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).indexed().toList().size());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).indexed().skip(1).toList().size());
    }

    @Test
    public void testSkip() {
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).skip(2).count());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).skip(2).skip(1).count());
        assertArrayEquals(new long[] { 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).skip(2).toArray());
        assertArrayEquals(new long[] { 4, 5 }, LongStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).skip(2).toList());
        assertEquals(Arrays.asList(4L, 5L), LongStream.of(1, 2, 3, 4, 5).skip(2).skip(1).toList());
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).count());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new long[] { 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toArray());
        assertArrayEquals(new long[] { 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toList());
        assertEquals(Arrays.asList(4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toList());
    }

    @Test
    public void testSkipWithAction() {
        List<Long> skipped = new ArrayList<>();
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).skip(2, skipped::add).count());
        assertEquals(Arrays.asList(1L, 2L), skipped);

        skipped.clear();
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).skip(2, skipped::add).skip(1).count());
        assertEquals(Arrays.asList(1L, 2L), skipped);

        skipped.clear();
        assertArrayEquals(new long[] { 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).skip(2, skipped::add).toArray());
        assertEquals(Arrays.asList(1L, 2L), skipped);

        skipped.clear();
        assertArrayEquals(new long[] { 4, 5 }, LongStream.of(1, 2, 3, 4, 5).skip(2, skipped::add).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L), skipped);

        skipped.clear();
        assertEquals(Arrays.asList(3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).skip(2, skipped::add).toList());
        assertEquals(Arrays.asList(1L, 2L), skipped);

        skipped.clear();
        assertEquals(Arrays.asList(4L, 5L), LongStream.of(1, 2, 3, 4, 5).skip(2, skipped::add).skip(1).toList());
        assertEquals(Arrays.asList(1L, 2L), skipped);

        skipped.clear();
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).count());
        assertEquals(Arrays.asList(1L, 2L), skipped);

        skipped.clear();
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).skip(1).count());
        assertEquals(Arrays.asList(1L, 2L), skipped);

        skipped.clear();
        assertArrayEquals(new long[] { 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).toArray());
        assertEquals(Arrays.asList(1L, 2L), skipped);

        skipped.clear();
        assertArrayEquals(new long[] { 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L), skipped);

        skipped.clear();
        assertEquals(Arrays.asList(3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).toList());
        assertEquals(Arrays.asList(1L, 2L), skipped);

        skipped.clear();
        assertEquals(Arrays.asList(4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).skip(1).toList());
        assertEquals(Arrays.asList(1L, 2L), skipped);
    }

    @Test
    public void testLimit() {
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).limit(3).count());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).limit(3).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStream.of(1, 2, 3, 4, 5).limit(3).toArray());
        assertArrayEquals(new long[] { 2, 3 }, LongStream.of(1, 2, 3, 4, 5).limit(3).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L), LongStream.of(1, 2, 3, 4, 5).limit(3).toList());
        assertEquals(Arrays.asList(2L, 3L), LongStream.of(1, 2, 3, 4, 5).limit(3).skip(1).toList());
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).count());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toArray());
        assertArrayEquals(new long[] { 2, 3 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toList());
        assertEquals(Arrays.asList(2L, 3L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toList());
    }

    @Test
    public void testStep() {
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).step(2).count());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).step(2).skip(1).count());
        assertArrayEquals(new long[] { 1, 3, 5 }, LongStream.of(1, 2, 3, 4, 5).step(2).toArray());
        assertArrayEquals(new long[] { 3, 5 }, LongStream.of(1, 2, 3, 4, 5).step(2).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 3L, 5L), LongStream.of(1, 2, 3, 4, 5).step(2).toList());
        assertEquals(Arrays.asList(3L, 5L), LongStream.of(1, 2, 3, 4, 5).step(2).skip(1).toList());
        assertEquals(3, LongStream.of(1, 2, 3, 4, 5).map(e -> e).step(2).count());
        assertEquals(2, LongStream.of(1, 2, 3, 4, 5).map(e -> e).step(2).skip(1).count());
        assertArrayEquals(new long[] { 1, 3, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).step(2).toArray());
        assertArrayEquals(new long[] { 3, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).step(2).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 3L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).step(2).toList());
        assertEquals(Arrays.asList(3L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).step(2).skip(1).toList());
    }

    // Skipping rateLimited and delay tests as they involve timing

    @Test
    public void testOnEach() {
        List<Long> collected = new ArrayList<>();
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).onEach(collected::add).count());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).onEach(collected::add).skip(1).count());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).onEach(collected::add).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).onEach(collected::add).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).onEach(collected::add).toList());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).onEach(collected::add).skip(1).toList());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(collected::add).count());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(collected::add).skip(1).count());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(collected::add).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(collected::add).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(collected::add).toList());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).onEach(collected::add).skip(1).toList());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);
    }

    @Test
    public void testPeek() {
        List<Long> collected = new ArrayList<>();
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).peek(collected::add).count());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).peek(collected::add).skip(1).count());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).peek(collected::add).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).peek(collected::add).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).peek(collected::add).toList());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).peek(collected::add).skip(1).toList());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).peek(collected::add).count());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).peek(collected::add).skip(1).count());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).peek(collected::add).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).peek(collected::add).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).peek(collected::add).toList());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);

        collected.clear();
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).peek(collected::add).skip(1).toList());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), collected);
    }

    // Continue with more tests for prepend, append, appendIfEmpty, defaultIfEmpty, throwIfEmpty, ifEmpty, onClose
    // These would follow the same pattern but need appropriate handling for their specific behaviors

    @Test
    public void testMap() {
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e * 2).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e * 2).skip(1).count());
        assertArrayEquals(new long[] { 2, 4, 6, 8, 10 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e * 2).toArray());
        assertArrayEquals(new long[] { 4, 6, 8, 10 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e * 2).skip(1).toArray());
        assertEquals(Arrays.asList(2L, 4L, 6L, 8L, 10L), LongStream.of(1, 2, 3, 4, 5).map(e -> e * 2).toList());
        assertEquals(Arrays.asList(4L, 6L, 8L, 10L), LongStream.of(1, 2, 3, 4, 5).map(e -> e * 2).skip(1).toList());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).map(e -> e * 2).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).map(e -> e * 2).skip(1).count());
        assertArrayEquals(new long[] { 2, 4, 6, 8, 10 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).map(e -> e * 2).toArray());
        assertArrayEquals(new long[] { 4, 6, 8, 10 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).map(e -> e * 2).skip(1).toArray());
        assertEquals(Arrays.asList(2L, 4L, 6L, 8L, 10L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).map(e -> e * 2).toList());
        assertEquals(Arrays.asList(4L, 6L, 8L, 10L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).map(e -> e * 2).skip(1).toList());
    }

    @Test
    public void testMapToInt() {
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).mapToInt(e -> (int) e).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).mapToInt(e -> (int) e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).mapToInt(e -> (int) e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).mapToInt(e -> (int) e).skip(1).toArray());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), LongStream.of(1, 2, 3, 4, 5).mapToInt(e -> (int) e).toList());
        assertEquals(Arrays.asList(2, 3, 4, 5), LongStream.of(1, 2, 3, 4, 5).mapToInt(e -> (int) e).skip(1).toList());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToInt(e -> (int) e).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToInt(e -> (int) e).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToInt(e -> (int) e).toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToInt(e -> (int) e).skip(1).toArray());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToInt(e -> (int) e).toList());
        assertEquals(Arrays.asList(2, 3, 4, 5), LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToInt(e -> (int) e).skip(1).toList());
    }

    @Test
    public void testMapToFloat() {
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).mapToFloat(e -> (float) e).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).mapToFloat(e -> (float) e).skip(1).count());
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, LongStream.of(1, 2, 3, 4, 5).mapToFloat(e -> (float) e).toArray(), 0.01f);
        assertArrayEquals(new float[] { 2.0f, 3.0f, 4.0f, 5.0f }, LongStream.of(1, 2, 3, 4, 5).mapToFloat(e -> (float) e).skip(1).toArray(), 0.01f);
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f, 4.0f, 5.0f), LongStream.of(1, 2, 3, 4, 5).mapToFloat(e -> (float) e).toList());
        assertEquals(Arrays.asList(2.0f, 3.0f, 4.0f, 5.0f), LongStream.of(1, 2, 3, 4, 5).mapToFloat(e -> (float) e).skip(1).toList());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToFloat(e -> (float) e).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToFloat(e -> (float) e).skip(1).count());
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToFloat(e -> (float) e).toArray(), 0.01f);
        assertArrayEquals(new float[] { 2.0f, 3.0f, 4.0f, 5.0f }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToFloat(e -> (float) e).skip(1).toArray(), 0.01f);
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f, 4.0f, 5.0f), LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToFloat(e -> (float) e).toList());
        assertEquals(Arrays.asList(2.0f, 3.0f, 4.0f, 5.0f), LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToFloat(e -> (float) e).skip(1).toList());
    }

    @Test
    public void testMapToDouble() {
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).mapToDouble(e -> (double) e).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).mapToDouble(e -> (double) e).skip(1).count());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, LongStream.of(1, 2, 3, 4, 5).mapToDouble(e -> (double) e).toArray(), 0.01);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0, 5.0 }, LongStream.of(1, 2, 3, 4, 5).mapToDouble(e -> (double) e).skip(1).toArray(), 0.01);
        assertEquals(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0), LongStream.of(1, 2, 3, 4, 5).mapToDouble(e -> (double) e).toList());
        assertEquals(Arrays.asList(2.0, 3.0, 4.0, 5.0), LongStream.of(1, 2, 3, 4, 5).mapToDouble(e -> (double) e).skip(1).toList());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(e -> (double) e).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(e -> (double) e).skip(1).count());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(e -> (double) e).toArray(), 0.01);
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0, 5.0 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(e -> (double) e).skip(1).toArray(), 0.01);
        assertEquals(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0), LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(e -> (double) e).toList());
        assertEquals(Arrays.asList(2.0, 3.0, 4.0, 5.0), LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToDouble(e -> (double) e).skip(1).toList());
    }

    @Test
    public void testMapToObj() {
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).mapToObj(e -> "val" + e).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).mapToObj(e -> "val" + e).skip(1).count());
        assertArrayEquals(new String[] { "val1", "val2", "val3", "val4", "val5" }, LongStream.of(1, 2, 3, 4, 5).mapToObj(e -> "val" + e).toArray());
        assertArrayEquals(new String[] { "val2", "val3", "val4", "val5" }, LongStream.of(1, 2, 3, 4, 5).mapToObj(e -> "val" + e).skip(1).toArray());
        assertEquals(Arrays.asList("val1", "val2", "val3", "val4", "val5"), LongStream.of(1, 2, 3, 4, 5).mapToObj(e -> "val" + e).toList());
        assertEquals(Arrays.asList("val2", "val3", "val4", "val5"), LongStream.of(1, 2, 3, 4, 5).mapToObj(e -> "val" + e).skip(1).toList());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(e -> "val" + e).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(e -> "val" + e).skip(1).count());
        assertArrayEquals(new String[] { "val1", "val2", "val3", "val4", "val5" }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(e -> "val" + e).toArray());
        assertArrayEquals(new String[] { "val2", "val3", "val4", "val5" }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(e -> "val" + e).skip(1).toArray());
        assertEquals(Arrays.asList("val1", "val2", "val3", "val4", "val5"), LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(e -> "val" + e).toList());
        assertEquals(Arrays.asList("val2", "val3", "val4", "val5"), LongStream.of(1, 2, 3, 4, 5).map(e -> e).mapToObj(e -> "val" + e).skip(1).toList());
    }

    @Test
    public void testFlatMap() {
        assertEquals(9, LongStream.of(1, 2, 3).flatMap(e -> LongStream.of(e, e + 10, e + 20)).count());
        assertEquals(8, LongStream.of(1, 2, 3).flatMap(e -> LongStream.of(e, e + 10, e + 20)).skip(1).count());
        assertArrayEquals(new long[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 }, LongStream.of(1, 2, 3).flatMap(e -> LongStream.of(e, e + 10, e + 20)).toArray());
        assertArrayEquals(new long[] { 11, 21, 2, 12, 22, 3, 13, 23 }, LongStream.of(1, 2, 3).flatMap(e -> LongStream.of(e, e + 10, e + 20)).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L), LongStream.of(1, 2, 3).flatMap(e -> LongStream.of(e, e + 10, e + 20)).toList());
        assertEquals(Arrays.asList(11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).flatMap(e -> LongStream.of(e, e + 10, e + 20)).skip(1).toList());
        assertEquals(9, LongStream.of(1, 2, 3).map(e -> e).flatMap(e -> LongStream.of(e, e + 10, e + 20)).count());
        assertEquals(8, LongStream.of(1, 2, 3).map(e -> e).flatMap(e -> LongStream.of(e, e + 10, e + 20)).skip(1).count());
        assertArrayEquals(new long[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flatMap(e -> LongStream.of(e, e + 10, e + 20)).toArray());
        assertArrayEquals(new long[] { 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flatMap(e -> LongStream.of(e, e + 10, e + 20)).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).map(e -> e).flatMap(e -> LongStream.of(e, e + 10, e + 20)).toList());
        assertEquals(Arrays.asList(11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).map(e -> e).flatMap(e -> LongStream.of(e, e + 10, e + 20)).skip(1).toList());
    }

    @Test
    public void testFlatmapArray() {
        assertEquals(9, LongStream.of(1, 2, 3).flatmap(e -> new long[] { e, e + 10, e + 20 }).count());
        assertEquals(8, LongStream.of(1, 2, 3).flatmap(e -> new long[] { e, e + 10, e + 20 }).skip(1).count());
        assertArrayEquals(new long[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 }, LongStream.of(1, 2, 3).flatmap(e -> new long[] { e, e + 10, e + 20 }).toArray());
        assertArrayEquals(new long[] { 11, 21, 2, 12, 22, 3, 13, 23 }, LongStream.of(1, 2, 3).flatmap(e -> new long[] { e, e + 10, e + 20 }).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L), LongStream.of(1, 2, 3).flatmap(e -> new long[] { e, e + 10, e + 20 }).toList());
        assertEquals(Arrays.asList(11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).flatmap(e -> new long[] { e, e + 10, e + 20 }).skip(1).toList());
        assertEquals(9, LongStream.of(1, 2, 3).map(e -> e).flatmap(e -> new long[] { e, e + 10, e + 20 }).count());
        assertEquals(8, LongStream.of(1, 2, 3).map(e -> e).flatmap(e -> new long[] { e, e + 10, e + 20 }).skip(1).count());
        assertArrayEquals(new long[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flatmap(e -> new long[] { e, e + 10, e + 20 }).toArray());
        assertArrayEquals(new long[] { 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flatmap(e -> new long[] { e, e + 10, e + 20 }).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).map(e -> e).flatmap(e -> new long[] { e, e + 10, e + 20 }).toList());
        assertEquals(Arrays.asList(11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).map(e -> e).flatmap(e -> new long[] { e, e + 10, e + 20 }).skip(1).toList());
    }

    @Test
    public void testFlattMapJdk() {
        assertEquals(9, LongStream.of(1, 2, 3).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).count());
        assertEquals(8, LongStream.of(1, 2, 3).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).skip(1).count());
        assertArrayEquals(new long[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).toArray());
        assertArrayEquals(new long[] { 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).toList());
        assertEquals(Arrays.asList(11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).skip(1).toList());
        assertEquals(9, LongStream.of(1, 2, 3).map(e -> e).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).count());
        assertEquals(8, LongStream.of(1, 2, 3).map(e -> e).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).skip(1).count());
        assertArrayEquals(new long[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).toArray());
        assertArrayEquals(new long[] { 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).map(e -> e).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).toList());
        assertEquals(Arrays.asList(11L, 21L, 2L, 12L, 22L, 3L, 13L, 23L),
                LongStream.of(1, 2, 3).map(e -> e).flattMap(e -> java.util.stream.LongStream.of(e, e + 10, e + 20)).skip(1).toList());
    }

    @Test
    public void testFlatMapToInt() {
        assertEquals(9, LongStream.of(1, 2, 3).flatMapToInt(e -> IntStream.of((int) e, (int) (e + 10), (int) (e + 20))).count());
        assertEquals(8, LongStream.of(1, 2, 3).flatMapToInt(e -> IntStream.of((int) e, (int) (e + 10), (int) (e + 20))).skip(1).count());
        assertArrayEquals(new int[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).flatMapToInt(e -> IntStream.of((int) e, (int) (e + 10), (int) (e + 20))).toArray());
        assertArrayEquals(new int[] { 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).flatMapToInt(e -> IntStream.of((int) e, (int) (e + 10), (int) (e + 20))).skip(1).toArray());
        assertEquals(Arrays.asList(1, 11, 21, 2, 12, 22, 3, 13, 23),
                LongStream.of(1, 2, 3).flatMapToInt(e -> IntStream.of((int) e, (int) (e + 10), (int) (e + 20))).toList());
        assertEquals(Arrays.asList(11, 21, 2, 12, 22, 3, 13, 23),
                LongStream.of(1, 2, 3).flatMapToInt(e -> IntStream.of((int) e, (int) (e + 10), (int) (e + 20))).skip(1).toList());
        assertEquals(9, LongStream.of(1, 2, 3).map(e -> e).flatMapToInt(e -> IntStream.of((int) e, (int) (e + 10), (int) (e + 20))).count());
        assertEquals(8, LongStream.of(1, 2, 3).map(e -> e).flatMapToInt(e -> IntStream.of((int) e, (int) (e + 10), (int) (e + 20))).skip(1).count());
        assertArrayEquals(new int[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flatMapToInt(e -> IntStream.of((int) e, (int) (e + 10), (int) (e + 20))).toArray());
        assertArrayEquals(new int[] { 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flatMapToInt(e -> IntStream.of((int) e, (int) (e + 10), (int) (e + 20))).skip(1).toArray());
        assertEquals(Arrays.asList(1, 11, 21, 2, 12, 22, 3, 13, 23),
                LongStream.of(1, 2, 3).map(e -> e).flatMapToInt(e -> IntStream.of((int) e, (int) (e + 10), (int) (e + 20))).toList());
        assertEquals(Arrays.asList(11, 21, 2, 12, 22, 3, 13, 23),
                LongStream.of(1, 2, 3).map(e -> e).flatMapToInt(e -> IntStream.of((int) e, (int) (e + 10), (int) (e + 20))).skip(1).toList());
    }

    @Test
    public void testFlatMapToFloat() {
        assertEquals(9, LongStream.of(1, 2, 3).flatMapToFloat(e -> FloatStream.of((float) e, (float) (e + 10), (float) (e + 20))).count());
        assertEquals(8, LongStream.of(1, 2, 3).flatMapToFloat(e -> FloatStream.of((float) e, (float) (e + 10), (float) (e + 20))).skip(1).count());
        assertArrayEquals(new float[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).flatMapToFloat(e -> FloatStream.of((float) e, (float) (e + 10), (float) (e + 20))).toArray(), 0.01f);
        assertArrayEquals(new float[] { 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).flatMapToFloat(e -> FloatStream.of((float) e, (float) (e + 10), (float) (e + 20))).skip(1).toArray(), 0.01f);
        assertEquals(Arrays.asList(1.0f, 11.0f, 21.0f, 2.0f, 12.0f, 22.0f, 3.0f, 13.0f, 23.0f),
                LongStream.of(1, 2, 3).flatMapToFloat(e -> FloatStream.of((float) e, (float) (e + 10), (float) (e + 20))).toList());
        assertEquals(Arrays.asList(11.0f, 21.0f, 2.0f, 12.0f, 22.0f, 3.0f, 13.0f, 23.0f),
                LongStream.of(1, 2, 3).flatMapToFloat(e -> FloatStream.of((float) e, (float) (e + 10), (float) (e + 20))).skip(1).toList());
        assertEquals(9, LongStream.of(1, 2, 3).map(e -> e).flatMapToFloat(e -> FloatStream.of((float) e, (float) (e + 10), (float) (e + 20))).count());
        assertEquals(8, LongStream.of(1, 2, 3).map(e -> e).flatMapToFloat(e -> FloatStream.of((float) e, (float) (e + 10), (float) (e + 20))).skip(1).count());
        assertArrayEquals(new float[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flatMapToFloat(e -> FloatStream.of((float) e, (float) (e + 10), (float) (e + 20))).toArray(), 0.01f);
        assertArrayEquals(new float[] { 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flatMapToFloat(e -> FloatStream.of((float) e, (float) (e + 10), (float) (e + 20))).skip(1).toArray(), 0.01f);
        assertEquals(Arrays.asList(1.0f, 11.0f, 21.0f, 2.0f, 12.0f, 22.0f, 3.0f, 13.0f, 23.0f),
                LongStream.of(1, 2, 3).map(e -> e).flatMapToFloat(e -> FloatStream.of((float) e, (float) (e + 10), (float) (e + 20))).toList());
        assertEquals(Arrays.asList(11.0f, 21.0f, 2.0f, 12.0f, 22.0f, 3.0f, 13.0f, 23.0f),
                LongStream.of(1, 2, 3).map(e -> e).flatMapToFloat(e -> FloatStream.of((float) e, (float) (e + 10), (float) (e + 20))).skip(1).toList());
    }

    @Test
    public void testFlatMapToDouble() {
        assertEquals(9, LongStream.of(1, 2, 3).flatMapToDouble(e -> DoubleStream.of((double) e, (double) (e + 10), (double) (e + 20))).count());
        assertEquals(8, LongStream.of(1, 2, 3).flatMapToDouble(e -> DoubleStream.of((double) e, (double) (e + 10), (double) (e + 20))).skip(1).count());
        assertArrayEquals(new double[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).flatMapToDouble(e -> DoubleStream.of((double) e, (double) (e + 10), (double) (e + 20))).toArray(), 0.01);
        assertArrayEquals(new double[] { 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).flatMapToDouble(e -> DoubleStream.of((double) e, (double) (e + 10), (double) (e + 20))).skip(1).toArray(), 0.01);
        assertEquals(Arrays.asList(1.0, 11.0, 21.0, 2.0, 12.0, 22.0, 3.0, 13.0, 23.0),
                LongStream.of(1, 2, 3).flatMapToDouble(e -> DoubleStream.of((double) e, (double) (e + 10), (double) (e + 20))).toList());
        assertEquals(Arrays.asList(11.0, 21.0, 2.0, 12.0, 22.0, 3.0, 13.0, 23.0),
                LongStream.of(1, 2, 3).flatMapToDouble(e -> DoubleStream.of((double) e, (double) (e + 10), (double) (e + 20))).skip(1).toList());
        assertEquals(9, LongStream.of(1, 2, 3).map(e -> e).flatMapToDouble(e -> DoubleStream.of((double) e, (double) (e + 10), (double) (e + 20))).count());
        assertEquals(8,
                LongStream.of(1, 2, 3).map(e -> e).flatMapToDouble(e -> DoubleStream.of((double) e, (double) (e + 10), (double) (e + 20))).skip(1).count());
        assertArrayEquals(new double[] { 1, 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flatMapToDouble(e -> DoubleStream.of((double) e, (double) (e + 10), (double) (e + 20))).toArray(), 0.01);
        assertArrayEquals(new double[] { 11, 21, 2, 12, 22, 3, 13, 23 },
                LongStream.of(1, 2, 3).map(e -> e).flatMapToDouble(e -> DoubleStream.of((double) e, (double) (e + 10), (double) (e + 20))).skip(1).toArray(),
                0.01);
        assertEquals(Arrays.asList(1.0, 11.0, 21.0, 2.0, 12.0, 22.0, 3.0, 13.0, 23.0),
                LongStream.of(1, 2, 3).map(e -> e).flatMapToDouble(e -> DoubleStream.of((double) e, (double) (e + 10), (double) (e + 20))).toList());
        assertEquals(Arrays.asList(11.0, 21.0, 2.0, 12.0, 22.0, 3.0, 13.0, 23.0),
                LongStream.of(1, 2, 3).map(e -> e).flatMapToDouble(e -> DoubleStream.of((double) e, (double) (e + 10), (double) (e + 20))).skip(1).toList());
    }

    @Test
    public void testFlatMapToObj() {
        assertEquals(9, LongStream.of(1, 2, 3).flatMapToObj(e -> Stream.of("a" + e, "b" + e, "c" + e)).count());
        assertEquals(8, LongStream.of(1, 2, 3).flatMapToObj(e -> Stream.of("a" + e, "b" + e, "c" + e)).skip(1).count());
        assertArrayEquals(new String[] { "a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3" },
                LongStream.of(1, 2, 3).flatMapToObj(e -> Stream.of("a" + e, "b" + e, "c" + e)).toArray());
        assertArrayEquals(new String[] { "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3" },
                LongStream.of(1, 2, 3).flatMapToObj(e -> Stream.of("a" + e, "b" + e, "c" + e)).skip(1).toArray());
        assertEquals(Arrays.asList("a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3"),
                LongStream.of(1, 2, 3).flatMapToObj(e -> Stream.of("a" + e, "b" + e, "c" + e)).toList());
        assertEquals(Arrays.asList("b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3"),
                LongStream.of(1, 2, 3).flatMapToObj(e -> Stream.of("a" + e, "b" + e, "c" + e)).skip(1).toList());
        assertEquals(9, LongStream.of(1, 2, 3).map(e -> e).flatMapToObj(e -> Stream.of("a" + e, "b" + e, "c" + e)).count());
        assertEquals(8, LongStream.of(1, 2, 3).map(e -> e).flatMapToObj(e -> Stream.of("a" + e, "b" + e, "c" + e)).skip(1).count());
        assertArrayEquals(new String[] { "a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3" },
                LongStream.of(1, 2, 3).map(e -> e).flatMapToObj(e -> Stream.of("a" + e, "b" + e, "c" + e)).toArray());
        assertArrayEquals(new String[] { "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3" },
                LongStream.of(1, 2, 3).map(e -> e).flatMapToObj(e -> Stream.of("a" + e, "b" + e, "c" + e)).skip(1).toArray());
        assertEquals(Arrays.asList("a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3"),
                LongStream.of(1, 2, 3).map(e -> e).flatMapToObj(e -> Stream.of("a" + e, "b" + e, "c" + e)).toList());
        assertEquals(Arrays.asList("b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3"),
                LongStream.of(1, 2, 3).map(e -> e).flatMapToObj(e -> Stream.of("a" + e, "b" + e, "c" + e)).skip(1).toList());
    }

    @Test
    public void testCollapse() {
        assertEquals(3, LongStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1).count());
        assertEquals(2, LongStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1).skip(1).count());
        assertEquals(3, LongStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1).toArray().length);
        assertEquals(2, LongStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1).skip(1).toArray().length);
        assertEquals(3, LongStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1).toList().size());
        assertEquals(2, LongStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1).skip(1).toList().size());
        assertEquals(3, LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1).count());
        assertEquals(2, LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1).skip(1).count());
        assertEquals(3, LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1).toArray().length);
        assertEquals(2, LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1).skip(1).toArray().length);
        assertEquals(3, LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1).toList().size());
        assertEquals(2, LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1).skip(1).toList().size());
    }

    @Test
    public void testCollapseWithMerge() {
        assertEquals(3, LongStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).count());
        assertEquals(2, LongStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 3, 11, 10 }, LongStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 11, 10 }, LongStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 11L, 10L), LongStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(11L, 10L), LongStream.of(1, 2, 5, 6, 10).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).count());
        assertEquals(2, LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 3, 11, 10 }, LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 11, 10 }, LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 11L, 10L), LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(11L, 10L), LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testCollapseWithTriPredicate() {
        assertEquals(3, LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).count());
        assertEquals(2, LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 3, 11, 10 },
                LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 11, 10 },
                LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 11L, 10L), LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(11L, 10L),
                LongStream.of(1, 2, 5, 6, 10).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).count());
        assertEquals(2, LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 3, 11, 10 },
                LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 11, 10 },
                LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 11L, 10L),
                LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(11L, 10L),
                LongStream.of(1, 2, 5, 6, 10).map(e -> e).collapse((first, last, next) -> next - first <= 2, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testScan() {
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 1, 3, 6, 10, 15 }, LongStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 3, 6, 10, 15 }, LongStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 3L, 6L, 10L, 15L), LongStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).toList());
        assertEquals(Arrays.asList(3L, 6L, 10L, 15L), LongStream.of(1, 2, 3, 4, 5).scan((a, b) -> a + b).skip(1).toList());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 1, 3, 6, 10, 15 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 3, 6, 10, 15 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 3L, 6L, 10L, 15L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).toList());
        assertEquals(Arrays.asList(3L, 6L, 10L, 15L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan((a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testScanWithInit() {
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 11, 13, 16, 20, 25 }, LongStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 13, 16, 20, 25 }, LongStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(11L, 13L, 16L, 20L, 25L), LongStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(13L, 16L, 20L, 25L), LongStream.of(1, 2, 3, 4, 5).scan(10, (a, b) -> a + b).skip(1).toList());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 11, 13, 16, 20, 25 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 13, 16, 20, 25 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(11L, 13L, 16L, 20L, 25L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(13L, 16L, 20L, 25L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testScanWithInitIncluded() {
        assertEquals(6, LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 10, 11, 13, 16, 20, 25 }, LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 11, 13, 16, 20, 25 }, LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(10L, 11L, 13L, 16L, 20L, 25L), LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(11L, 13L, 16L, 20L, 25L), LongStream.of(1, 2, 3, 4, 5).scan(10, true, (a, b) -> a + b).skip(1).toList());
        assertEquals(6, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).count());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 10, 11, 13, 16, 20, 25 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 11, 13, 16, 20, 25 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(10L, 11L, 13L, 16L, 20L, 25L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(11L, 13L, 16L, 20L, 25L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).scan(10, true, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testPrepend() {
        assertEquals(8, LongStream.of(1, 2, 3, 4, 5).prepend(10, 20, 30).count());
        assertEquals(7, LongStream.of(1, 2, 3, 4, 5).prepend(10, 20, 30).skip(1).count());
        assertArrayEquals(new long[] { 10, 20, 30, 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).prepend(10, 20, 30).toArray());
        assertArrayEquals(new long[] { 20, 30, 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).prepend(10, 20, 30).skip(1).toArray());
        assertEquals(Arrays.asList(10L, 20L, 30L, 1L, 2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).prepend(10, 20, 30).toList());
        assertEquals(Arrays.asList(20L, 30L, 1L, 2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).prepend(10, 20, 30).skip(1).toList());
        assertEquals(8, LongStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(10, 20, 30).count());
        assertEquals(7, LongStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(10, 20, 30).skip(1).count());
        assertArrayEquals(new long[] { 10, 20, 30, 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(10, 20, 30).toArray());
        assertArrayEquals(new long[] { 20, 30, 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(10, 20, 30).skip(1).toArray());
        assertEquals(Arrays.asList(10L, 20L, 30L, 1L, 2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(10, 20, 30).toList());
        assertEquals(Arrays.asList(20L, 30L, 1L, 2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).prepend(10, 20, 30).skip(1).toList());
    }

    @Test
    public void testAppend() {
        assertEquals(8, LongStream.of(1, 2, 3, 4, 5).append(10, 20, 30).count());
        assertEquals(7, LongStream.of(1, 2, 3, 4, 5).append(10, 20, 30).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 10, 20, 30 }, LongStream.of(1, 2, 3, 4, 5).append(10, 20, 30).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 10, 20, 30 }, LongStream.of(1, 2, 3, 4, 5).append(10, 20, 30).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 10L, 20L, 30L), LongStream.of(1, 2, 3, 4, 5).append(10, 20, 30).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 10L, 20L, 30L), LongStream.of(1, 2, 3, 4, 5).append(10, 20, 30).skip(1).toList());
        assertEquals(8, LongStream.of(1, 2, 3, 4, 5).map(e -> e).append(10, 20, 30).count());
        assertEquals(7, LongStream.of(1, 2, 3, 4, 5).map(e -> e).append(10, 20, 30).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 10, 20, 30 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).append(10, 20, 30).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 10, 20, 30 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).append(10, 20, 30).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 10L, 20L, 30L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).append(10, 20, 30).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 10L, 20L, 30L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).append(10, 20, 30).skip(1).toList());
    }

    @Test
    public void testAppendIfEmpty() {
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).appendIfEmpty(10, 20, 30).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).appendIfEmpty(10, 20, 30).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).appendIfEmpty(10, 20, 30).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).appendIfEmpty(10, 20, 30).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).appendIfEmpty(10, 20, 30).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).appendIfEmpty(10, 20, 30).skip(1).toList());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(10, 20, 30).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(10, 20, 30).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(10, 20, 30).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(10, 20, 30).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(10, 20, 30).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).appendIfEmpty(10, 20, 30).skip(1).toList());

        // Test with empty stream
        assertEquals(3, LongStream.empty().appendIfEmpty(10, 20, 30).count());
        assertArrayEquals(new long[] { 10, 20, 30 }, LongStream.empty().appendIfEmpty(10, 20, 30).toArray());
        assertEquals(Arrays.asList(10L, 20L, 30L), LongStream.empty().appendIfEmpty(10, 20, 30).toList());
    }

    @Test
    public void testTop() {
        assertEquals(3, LongStream.of(5, 3, 1, 4, 2).top(3).count());
        assertEquals(2, LongStream.of(5, 3, 1, 4, 2).top(3).skip(1).count());
        assertArrayEquals(new long[] { 3, 5, 4 }, LongStream.of(5, 3, 1, 4, 2).top(3).toArray());
        assertArrayEquals(new long[] { 5, 4 }, LongStream.of(5, 3, 1, 4, 2).top(3).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 5L, 4L), LongStream.of(5, 3, 1, 4, 2).top(3).toList());
        assertEquals(Arrays.asList(5L, 4L), LongStream.of(5, 3, 1, 4, 2).top(3).skip(1).toList());
        assertEquals(3, LongStream.of(5, 3, 1, 4, 2).map(e -> e).top(3).count());
        assertEquals(2, LongStream.of(5, 3, 1, 4, 2).map(e -> e).top(3).skip(1).count());
        assertArrayEquals(new long[] { 3, 5, 4 }, LongStream.of(5, 3, 1, 4, 2).map(e -> e).top(3).toArray());
        assertArrayEquals(new long[] { 5, 4 }, LongStream.of(5, 3, 1, 4, 2).map(e -> e).top(3).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 5L, 4L), LongStream.of(5, 3, 1, 4, 2).map(e -> e).top(3).toList());
        assertEquals(Arrays.asList(5L, 4L), LongStream.of(5, 3, 1, 4, 2).map(e -> e).top(3).skip(1).toList());
    }

    @Test
    public void testTopWithComparator() {
        Comparator<Long> reverseOrder = (a, b) -> Long.compare(b, a);
        assertEquals(3, LongStream.of(5, 3, 1, 4, 2).top(3, reverseOrder).count());
        assertEquals(2, LongStream.of(5, 3, 1, 4, 2).top(3, reverseOrder).skip(1).count());
        assertArrayEquals(new long[] { 3, 1, 2 }, LongStream.of(5, 3, 1, 4, 2).top(3, reverseOrder).toArray());
        assertArrayEquals(new long[] { 1, 2 }, LongStream.of(5, 3, 1, 4, 2).top(3, reverseOrder).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 1L, 2L), LongStream.of(5, 3, 1, 4, 2).top(3, reverseOrder).toList());
        assertEquals(Arrays.asList(1L, 2L), LongStream.of(5, 3, 1, 4, 2).top(3, reverseOrder).skip(1).toList());
        assertEquals(3, LongStream.of(5, 3, 1, 4, 2).map(e -> e).top(3, reverseOrder).count());
        assertEquals(2, LongStream.of(5, 3, 1, 4, 2).map(e -> e).top(3, reverseOrder).skip(1).count());
        assertArrayEquals(new long[] { 3, 1, 2 }, LongStream.of(5, 3, 1, 4, 2).map(e -> e).top(3, reverseOrder).toArray());
        assertArrayEquals(new long[] { 1, 2 }, LongStream.of(5, 3, 1, 4, 2).map(e -> e).top(3, reverseOrder).skip(1).toArray());
        assertEquals(Arrays.asList(3L, 1L, 2L), LongStream.of(5, 3, 1, 4, 2).map(e -> e).top(3, reverseOrder).toList());
        assertEquals(Arrays.asList(1L, 2L), LongStream.of(5, 3, 1, 4, 2).map(e -> e).top(3, reverseOrder).skip(1).toList());
    }

    @Test
    public void testEmpty() {
        assertEquals(0, LongStream.empty().count());
        assertEquals(0, LongStream.empty().skip(1).count());
        assertArrayEquals(new long[] {}, LongStream.empty().toArray());
        assertArrayEquals(new long[] {}, LongStream.empty().skip(1).toArray());
        assertEquals(Arrays.asList(), LongStream.empty().toList());
        assertEquals(Arrays.asList(), LongStream.empty().skip(1).toList());
        assertEquals(0, LongStream.empty().map(e -> e).count());
        assertEquals(0, LongStream.empty().map(e -> e).skip(1).count());
        assertArrayEquals(new long[] {}, LongStream.empty().map(e -> e).toArray());
        assertArrayEquals(new long[] {}, LongStream.empty().map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(), LongStream.empty().map(e -> e).toList());
        assertEquals(Arrays.asList(), LongStream.empty().map(e -> e).skip(1).toList());
    }

    @Test
    public void testDefer() {
        assertEquals(5, LongStream.defer(() -> LongStream.of(1, 2, 3, 4, 5)).count());
        assertEquals(4, LongStream.defer(() -> LongStream.of(1, 2, 3, 4, 5)).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.defer(() -> LongStream.of(1, 2, 3, 4, 5)).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.defer(() -> LongStream.of(1, 2, 3, 4, 5)).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.defer(() -> LongStream.of(1, 2, 3, 4, 5)).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.defer(() -> LongStream.of(1, 2, 3, 4, 5)).skip(1).toList());
        assertEquals(5, LongStream.defer(() -> LongStream.of(1, 2, 3, 4, 5)).map(e -> e).count());
        assertEquals(4, LongStream.defer(() -> LongStream.of(1, 2, 3, 4, 5)).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.defer(() -> LongStream.of(1, 2, 3, 4, 5)).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.defer(() -> LongStream.of(1, 2, 3, 4, 5)).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.defer(() -> LongStream.of(1, 2, 3, 4, 5)).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.defer(() -> LongStream.of(1, 2, 3, 4, 5)).map(e -> e).skip(1).toList());
    }

    @Test
    public void testFrom() {
        java.util.stream.LongStream jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertEquals(5, LongStream.from(jdkStream).count());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertEquals(4, LongStream.from(jdkStream).skip(1).count());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.from(jdkStream).toArray());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.from(jdkStream).skip(1).toArray());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.from(jdkStream).toList());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.from(jdkStream).skip(1).toList());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertEquals(5, LongStream.from(jdkStream).map(e -> e).count());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertEquals(4, LongStream.from(jdkStream).map(e -> e).skip(1).count());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.from(jdkStream).map(e -> e).toArray());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.from(jdkStream).map(e -> e).skip(1).toArray());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.from(jdkStream).map(e -> e).toList());

        jdkStream = java.util.stream.LongStream.of(1, 2, 3, 4, 5);
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.from(jdkStream).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfNullable() {
        assertEquals(1, LongStream.ofNullable(5L).count());
        assertEquals(0, LongStream.ofNullable(5L).skip(1).count());
        assertArrayEquals(new long[] { 5 }, LongStream.ofNullable(5L).toArray());
        assertArrayEquals(new long[] {}, LongStream.ofNullable(5L).skip(1).toArray());
        assertEquals(Arrays.asList(5L), LongStream.ofNullable(5L).toList());
        assertEquals(Arrays.asList(), LongStream.ofNullable(5L).skip(1).toList());
        assertEquals(1, LongStream.ofNullable(5L).map(e -> e).count());
        assertEquals(0, LongStream.ofNullable(5L).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 5 }, LongStream.ofNullable(5L).map(e -> e).toArray());
        assertArrayEquals(new long[] {}, LongStream.ofNullable(5L).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(5L), LongStream.ofNullable(5L).map(e -> e).toList());
        assertEquals(Arrays.asList(), LongStream.ofNullable(5L).map(e -> e).skip(1).toList());

        // Test with null
        assertEquals(0, LongStream.ofNullable(null).count());
        assertArrayEquals(new long[] {}, LongStream.ofNullable(null).toArray());
        assertEquals(Arrays.asList(), LongStream.ofNullable(null).toList());
    }

    @Test
    public void testOf() {
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).skip(1).toList());
        assertEquals(5, LongStream.of(1, 2, 3, 4, 5).map(e -> e).count());
        assertEquals(4, LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(1, 2, 3, 4, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfArrayWithIndices() {
        long[] arr = { 10, 20, 30, 40, 50 };
        assertEquals(3, LongStream.of(arr, 1, 4).count());
        assertEquals(2, LongStream.of(arr, 1, 4).skip(1).count());
        assertArrayEquals(new long[] { 20, 30, 40 }, LongStream.of(arr, 1, 4).toArray());
        assertArrayEquals(new long[] { 30, 40 }, LongStream.of(arr, 1, 4).skip(1).toArray());
        assertEquals(Arrays.asList(20L, 30L, 40L), LongStream.of(arr, 1, 4).toList());
        assertEquals(Arrays.asList(30L, 40L), LongStream.of(arr, 1, 4).skip(1).toList());
        assertEquals(3, LongStream.of(arr, 1, 4).map(e -> e).count());
        assertEquals(2, LongStream.of(arr, 1, 4).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 20, 30, 40 }, LongStream.of(arr, 1, 4).map(e -> e).toArray());
        assertArrayEquals(new long[] { 30, 40 }, LongStream.of(arr, 1, 4).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(20L, 30L, 40L), LongStream.of(arr, 1, 4).map(e -> e).toList());
        assertEquals(Arrays.asList(30L, 40L), LongStream.of(arr, 1, 4).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfLongArray() {
        Long[] arr = { 1L, 2L, 3L, 4L, 5L };
        assertEquals(5, LongStream.of(arr).count());
        assertEquals(4, LongStream.of(arr).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(arr).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(arr).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(arr).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(arr).skip(1).toList());
        assertEquals(5, LongStream.of(arr).map(e -> e).count());
        assertEquals(4, LongStream.of(arr).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(arr).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(arr).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(arr).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(arr).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfLongArrayWithIndices() {
        Long[] arr = { 10L, 20L, 30L, 40L, 50L };
        assertEquals(3, LongStream.of(arr, 1, 4).count());
        assertEquals(2, LongStream.of(arr, 1, 4).skip(1).count());
        assertArrayEquals(new long[] { 20, 30, 40 }, LongStream.of(arr, 1, 4).toArray());
        assertArrayEquals(new long[] { 30, 40 }, LongStream.of(arr, 1, 4).skip(1).toArray());
        assertEquals(Arrays.asList(20L, 30L, 40L), LongStream.of(arr, 1, 4).toList());
        assertEquals(Arrays.asList(30L, 40L), LongStream.of(arr, 1, 4).skip(1).toList());
        assertEquals(3, LongStream.of(arr, 1, 4).map(e -> e).count());
        assertEquals(2, LongStream.of(arr, 1, 4).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 20, 30, 40 }, LongStream.of(arr, 1, 4).map(e -> e).toArray());
        assertArrayEquals(new long[] { 30, 40 }, LongStream.of(arr, 1, 4).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(20L, 30L, 40L), LongStream.of(arr, 1, 4).map(e -> e).toList());
        assertEquals(Arrays.asList(30L, 40L), LongStream.of(arr, 1, 4).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfCollection() {
        Collection<Long> coll = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        assertEquals(5, LongStream.of(coll).count());
        assertEquals(4, LongStream.of(coll).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(coll).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(coll).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(coll).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(coll).skip(1).toList());
        assertEquals(5, LongStream.of(coll).map(e -> e).count());
        assertEquals(4, LongStream.of(coll).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(coll).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(coll).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(coll).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(coll).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfIterator() {
        LongIterator iter = new LongIterator() {
            private long[] arr = { 1, 2, 3, 4, 5 };
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < arr.length;
            }

            @Override
            public long nextLong() {
                return arr[index++];
            }
        };

        assertEquals(5, LongStream.of(iter).count());

        // Need new iterator for each test
        iter = LongIterator.of(new long[] { 1, 2, 3, 4, 5 });
        assertEquals(4, LongStream.of(iter).skip(1).count());

        iter = LongIterator.of(new long[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(iter).toArray());

        iter = LongIterator.of(new long[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(iter).skip(1).toArray());

        iter = LongIterator.of(new long[] { 1, 2, 3, 4, 5 });
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(iter).toList());

        iter = LongIterator.of(new long[] { 1, 2, 3, 4, 5 });
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(iter).skip(1).toList());

        iter = LongIterator.of(new long[] { 1, 2, 3, 4, 5 });
        assertEquals(5, LongStream.of(iter).map(e -> e).count());

        iter = LongIterator.of(new long[] { 1, 2, 3, 4, 5 });
        assertEquals(4, LongStream.of(iter).map(e -> e).skip(1).count());

        iter = LongIterator.of(new long[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(iter).map(e -> e).toArray());

        iter = LongIterator.of(new long[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(iter).map(e -> e).skip(1).toArray());

        iter = LongIterator.of(new long[] { 1, 2, 3, 4, 5 });
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(iter).map(e -> e).toList());

        iter = LongIterator.of(new long[] { 1, 2, 3, 4, 5 });
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(iter).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfLongBuffer() {
        LongBuffer buf = LongBuffer.wrap(new long[] { 1, 2, 3, 4, 5 });
        assertEquals(5, LongStream.of(buf).count());

        buf = LongBuffer.wrap(new long[] { 1, 2, 3, 4, 5 });
        assertEquals(4, LongStream.of(buf).skip(1).count());

        buf = LongBuffer.wrap(new long[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(buf).toArray());

        buf = LongBuffer.wrap(new long[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(buf).skip(1).toArray());

        buf = LongBuffer.wrap(new long[] { 1, 2, 3, 4, 5 });
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(buf).toList());

        buf = LongBuffer.wrap(new long[] { 1, 2, 3, 4, 5 });
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(buf).skip(1).toList());

        buf = LongBuffer.wrap(new long[] { 1, 2, 3, 4, 5 });
        assertEquals(5, LongStream.of(buf).map(e -> e).count());

        buf = LongBuffer.wrap(new long[] { 1, 2, 3, 4, 5 });
        assertEquals(4, LongStream.of(buf).map(e -> e).skip(1).count());

        buf = LongBuffer.wrap(new long[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.of(buf).map(e -> e).toArray());

        buf = LongBuffer.wrap(new long[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.of(buf).map(e -> e).skip(1).toArray());

        buf = LongBuffer.wrap(new long[] { 1, 2, 3, 4, 5 });
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.of(buf).map(e -> e).toList());

        buf = LongBuffer.wrap(new long[] { 1, 2, 3, 4, 5 });
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.of(buf).map(e -> e).skip(1).toList());
    }

    @Test
    public void testOfOptionalLong() {
        assertEquals(1, LongStream.of(OptionalLong.of(5)).count());
        assertEquals(0, LongStream.of(OptionalLong.of(5)).skip(1).count());
        assertArrayEquals(new long[] { 5 }, LongStream.of(OptionalLong.of(5)).toArray());
        assertArrayEquals(new long[] {}, LongStream.of(OptionalLong.of(5)).skip(1).toArray());
        assertEquals(Arrays.asList(5L), LongStream.of(OptionalLong.of(5)).toList());
        assertEquals(Arrays.asList(), LongStream.of(OptionalLong.of(5)).skip(1).toList());
        assertEquals(1, LongStream.of(OptionalLong.of(5)).map(e -> e).count());
        assertEquals(0, LongStream.of(OptionalLong.of(5)).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 5 }, LongStream.of(OptionalLong.of(5)).map(e -> e).toArray());
        assertArrayEquals(new long[] {}, LongStream.of(OptionalLong.of(5)).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(5L), LongStream.of(OptionalLong.of(5)).map(e -> e).toList());
        assertEquals(Arrays.asList(), LongStream.of(OptionalLong.of(5)).map(e -> e).skip(1).toList());

        // Test empty optional
        assertEquals(0, LongStream.of(OptionalLong.empty()).count());
        assertArrayEquals(new long[] {}, LongStream.of(OptionalLong.empty()).toArray());
        assertEquals(Arrays.asList(), LongStream.of(OptionalLong.empty()).toList());
    }

    @Test
    public void testOfJavaOptionalLong() {
        assertEquals(1, LongStream.of(java.util.OptionalLong.of(5)).count());
        assertEquals(0, LongStream.of(java.util.OptionalLong.of(5)).skip(1).count());
        assertArrayEquals(new long[] { 5 }, LongStream.of(java.util.OptionalLong.of(5)).toArray());
        assertArrayEquals(new long[] {}, LongStream.of(java.util.OptionalLong.of(5)).skip(1).toArray());
        assertEquals(Arrays.asList(5L), LongStream.of(java.util.OptionalLong.of(5)).toList());
        assertEquals(Arrays.asList(), LongStream.of(java.util.OptionalLong.of(5)).skip(1).toList());
        assertEquals(1, LongStream.of(java.util.OptionalLong.of(5)).map(e -> e).count());
        assertEquals(0, LongStream.of(java.util.OptionalLong.of(5)).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 5 }, LongStream.of(java.util.OptionalLong.of(5)).map(e -> e).toArray());
        assertArrayEquals(new long[] {}, LongStream.of(java.util.OptionalLong.of(5)).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(5L), LongStream.of(java.util.OptionalLong.of(5)).map(e -> e).toList());
        assertEquals(Arrays.asList(), LongStream.of(java.util.OptionalLong.of(5)).map(e -> e).skip(1).toList());

        // Test empty optional
        assertEquals(0, LongStream.of(java.util.OptionalLong.empty()).count());
        assertArrayEquals(new long[] {}, LongStream.of(java.util.OptionalLong.empty()).toArray());
        assertEquals(Arrays.asList(), LongStream.of(java.util.OptionalLong.empty()).toList());
    }

    @Test
    public void testFlatten() {
        long[][] arr = { { 1, 2 }, { 3, 4 }, { 5 } };
        assertEquals(5, LongStream.flatten(arr).count());
        assertEquals(4, LongStream.flatten(arr).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.flatten(arr).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.flatten(arr).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.flatten(arr).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.flatten(arr).skip(1).toList());
        assertEquals(5, LongStream.flatten(arr).map(e -> e).count());
        assertEquals(4, LongStream.flatten(arr).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.flatten(arr).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.flatten(arr).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.flatten(arr).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.flatten(arr).map(e -> e).skip(1).toList());
    }

    @Test
    public void testFlattenVertically() {
        long[][] arr = { { 1, 2, 3 }, { 4, 5 }, { 6 } };
        assertEquals(6, LongStream.flatten(arr, true).count());
        assertEquals(5, LongStream.flatten(arr, true).skip(1).count());
        assertArrayEquals(new long[] { 1, 4, 6, 2, 5, 3 }, LongStream.flatten(arr, true).toArray());
        assertArrayEquals(new long[] { 4, 6, 2, 5, 3 }, LongStream.flatten(arr, true).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 4L, 6L, 2L, 5L, 3L), LongStream.flatten(arr, true).toList());
        assertEquals(Arrays.asList(4L, 6L, 2L, 5L, 3L), LongStream.flatten(arr, true).skip(1).toList());
        assertEquals(6, LongStream.flatten(arr, true).map(e -> e).count());
        assertEquals(5, LongStream.flatten(arr, true).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 4, 6, 2, 5, 3 }, LongStream.flatten(arr, true).map(e -> e).toArray());
        assertArrayEquals(new long[] { 4, 6, 2, 5, 3 }, LongStream.flatten(arr, true).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 4L, 6L, 2L, 5L, 3L), LongStream.flatten(arr, true).map(e -> e).toList());
        assertEquals(Arrays.asList(4L, 6L, 2L, 5L, 3L), LongStream.flatten(arr, true).map(e -> e).skip(1).toList());
    }

    @Test
    public void testFlattenWithAlignment() {
        long[][] arr = { { 1, 2, 3 }, { 4 }, { 5, 6 } };
        assertEquals(9, LongStream.flatten(arr, 0L, false).count());
        assertEquals(8, LongStream.flatten(arr, 0L, false).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 0, 0, 5, 6, 0 }, LongStream.flatten(arr, 0L, false).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 0, 0, 5, 6, 0 }, LongStream.flatten(arr, 0L, false).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 0L, 0L, 5L, 6L, 0L), LongStream.flatten(arr, 0L, false).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 0L, 0L, 5L, 6L, 0L), LongStream.flatten(arr, 0L, false).skip(1).toList());
        assertEquals(9, LongStream.flatten(arr, 0L, false).map(e -> e).count());
        assertEquals(8, LongStream.flatten(arr, 0L, false).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 0, 0, 5, 6, 0 }, LongStream.flatten(arr, 0L, false).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 0, 0, 5, 6, 0 }, LongStream.flatten(arr, 0L, false).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 0L, 0L, 5L, 6L, 0L), LongStream.flatten(arr, 0L, false).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 0L, 0L, 5L, 6L, 0L), LongStream.flatten(arr, 0L, false).map(e -> e).skip(1).toList());
    }

    @Test
    public void testFlatten3D() {
        long[][][] arr = { { { 1, 2 }, { 3 } }, { { 4 }, { 5, 6 } } };
        assertEquals(6, LongStream.flatten(arr).count());
        assertEquals(5, LongStream.flatten(arr).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, LongStream.flatten(arr).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6 }, LongStream.flatten(arr).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), LongStream.flatten(arr).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L), LongStream.flatten(arr).skip(1).toList());
        assertEquals(6, LongStream.flatten(arr).map(e -> e).count());
        assertEquals(5, LongStream.flatten(arr).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, LongStream.flatten(arr).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6 }, LongStream.flatten(arr).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), LongStream.flatten(arr).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L), LongStream.flatten(arr).map(e -> e).skip(1).toList());
    }

    @Test
    public void testRange() {
        assertEquals(5, LongStream.range(0, 5).count());
        assertEquals(4, LongStream.range(0, 5).skip(1).count());
        assertArrayEquals(new long[] { 0, 1, 2, 3, 4 }, LongStream.range(0, 5).toArray());
        assertArrayEquals(new long[] { 1, 2, 3, 4 }, LongStream.range(0, 5).skip(1).toArray());
        assertEquals(Arrays.asList(0L, 1L, 2L, 3L, 4L), LongStream.range(0, 5).toList());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L), LongStream.range(0, 5).skip(1).toList());
        assertEquals(5, LongStream.range(0, 5).map(e -> e).count());
        assertEquals(4, LongStream.range(0, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 0, 1, 2, 3, 4 }, LongStream.range(0, 5).map(e -> e).toArray());
        assertArrayEquals(new long[] { 1, 2, 3, 4 }, LongStream.range(0, 5).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(0L, 1L, 2L, 3L, 4L), LongStream.range(0, 5).map(e -> e).toList());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L), LongStream.range(0, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testRangeWithStep() {
        assertEquals(4, LongStream.range(0, 10, 3).count());
        assertEquals(3, LongStream.range(0, 10, 3).skip(1).count());
        assertArrayEquals(new long[] { 0, 3, 6, 9 }, LongStream.range(0, 10, 3).toArray());
        assertArrayEquals(new long[] { 3, 6, 9 }, LongStream.range(0, 10, 3).skip(1).toArray());
        assertEquals(Arrays.asList(0L, 3L, 6L, 9L), LongStream.range(0, 10, 3).toList());
        assertEquals(Arrays.asList(3L, 6L, 9L), LongStream.range(0, 10, 3).skip(1).toList());
        assertEquals(4, LongStream.range(0, 10, 3).map(e -> e).count());
        assertEquals(3, LongStream.range(0, 10, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 0, 3, 6, 9 }, LongStream.range(0, 10, 3).map(e -> e).toArray());
        assertArrayEquals(new long[] { 3, 6, 9 }, LongStream.range(0, 10, 3).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(0L, 3L, 6L, 9L), LongStream.range(0, 10, 3).map(e -> e).toList());
        assertEquals(Arrays.asList(3L, 6L, 9L), LongStream.range(0, 10, 3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testRangeClosed() {
        assertEquals(6, LongStream.rangeClosed(0, 5).count());
        assertEquals(5, LongStream.rangeClosed(0, 5).skip(1).count());
        assertArrayEquals(new long[] { 0, 1, 2, 3, 4, 5 }, LongStream.rangeClosed(0, 5).toArray());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.rangeClosed(0, 5).skip(1).toArray());
        assertEquals(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L), LongStream.rangeClosed(0, 5).toList());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.rangeClosed(0, 5).skip(1).toList());
        assertEquals(6, LongStream.rangeClosed(0, 5).map(e -> e).count());
        assertEquals(5, LongStream.rangeClosed(0, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 0, 1, 2, 3, 4, 5 }, LongStream.rangeClosed(0, 5).map(e -> e).toArray());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.rangeClosed(0, 5).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L), LongStream.rangeClosed(0, 5).map(e -> e).toList());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.rangeClosed(0, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testRangeClosedWithStep() {
        assertEquals(4, LongStream.rangeClosed(0, 10, 3).count());
        assertEquals(3, LongStream.rangeClosed(0, 10, 3).skip(1).count());
        assertArrayEquals(new long[] { 0, 3, 6, 9 }, LongStream.rangeClosed(0, 10, 3).toArray());
        assertArrayEquals(new long[] { 3, 6, 9 }, LongStream.rangeClosed(0, 10, 3).skip(1).toArray());
        assertEquals(Arrays.asList(0L, 3L, 6L, 9L), LongStream.rangeClosed(0, 10, 3).toList());
        assertEquals(Arrays.asList(3L, 6L, 9L), LongStream.rangeClosed(0, 10, 3).skip(1).toList());
        assertEquals(4, LongStream.rangeClosed(0, 10, 3).map(e -> e).count());
        assertEquals(3, LongStream.rangeClosed(0, 10, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 0, 3, 6, 9 }, LongStream.rangeClosed(0, 10, 3).map(e -> e).toArray());
        assertArrayEquals(new long[] { 3, 6, 9 }, LongStream.rangeClosed(0, 10, 3).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(0L, 3L, 6L, 9L), LongStream.rangeClosed(0, 10, 3).map(e -> e).toList());
        assertEquals(Arrays.asList(3L, 6L, 9L), LongStream.rangeClosed(0, 10, 3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testRepeat() {
        assertEquals(5, LongStream.repeat(7, 5).count());
        assertEquals(4, LongStream.repeat(7, 5).skip(1).count());
        assertArrayEquals(new long[] { 7, 7, 7, 7, 7 }, LongStream.repeat(7, 5).toArray());
        assertArrayEquals(new long[] { 7, 7, 7, 7 }, LongStream.repeat(7, 5).skip(1).toArray());
        assertEquals(Arrays.asList(7L, 7L, 7L, 7L, 7L), LongStream.repeat(7, 5).toList());
        assertEquals(Arrays.asList(7L, 7L, 7L, 7L), LongStream.repeat(7, 5).skip(1).toList());
        assertEquals(5, LongStream.repeat(7, 5).map(e -> e).count());
        assertEquals(4, LongStream.repeat(7, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 7, 7, 7, 7, 7 }, LongStream.repeat(7, 5).map(e -> e).toArray());
        assertArrayEquals(new long[] { 7, 7, 7, 7 }, LongStream.repeat(7, 5).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(7L, 7L, 7L, 7L, 7L), LongStream.repeat(7, 5).map(e -> e).toList());
        assertEquals(Arrays.asList(7L, 7L, 7L, 7L), LongStream.repeat(7, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testRandom() {
        assertEquals(5, LongStream.random().limit(5).count());
        assertEquals(4, LongStream.random().limit(5).skip(1).count());
        assertEquals(5, LongStream.random().limit(5).toArray().length);
        assertEquals(4, LongStream.random().limit(5).skip(1).toArray().length);
        assertEquals(5, LongStream.random().limit(5).toList().size());
        assertEquals(4, LongStream.random().limit(5).skip(1).toList().size());
        assertEquals(5, LongStream.random().map(e -> e).limit(5).count());
        assertEquals(4, LongStream.random().map(e -> e).limit(5).skip(1).count());
        assertEquals(5, LongStream.random().map(e -> e).limit(5).toArray().length);
        assertEquals(4, LongStream.random().map(e -> e).limit(5).skip(1).toArray().length);
        assertEquals(5, LongStream.random().map(e -> e).limit(5).toList().size());
        assertEquals(4, LongStream.random().map(e -> e).limit(5).skip(1).toList().size());
    }

    // Skipping interval tests as they involve timing

    @Test
    public void testIterate() {
        BooleanSupplier hasNext = new BooleanSupplier() {
            private int count = 0;

            @Override
            public boolean getAsBoolean() {
                return count++ < 5;
            }
        };
        LongSupplier next = new LongSupplier() {
            private long value = 1;

            @Override
            public long getAsLong() {
                return value++;
            }
        };

        assertEquals(5, LongStream.iterate(hasNext, next).count());

        // Need new suppliers for each test
        hasNext = new BooleanSupplier() {
            private int count = 0;

            @Override
            public boolean getAsBoolean() {
                return count++ < 5;
            }
        };
        next = new LongSupplier() {
            private long value = 1;

            @Override
            public long getAsLong() {
                return value++;
            }
        };
        assertEquals(4, LongStream.iterate(hasNext, next).skip(1).count());
    }

    @Test
    public void testIterateWithInit() {
        assertEquals(5, LongStream.iterate(1, e -> e < 6, e -> e + 1).count());
        assertEquals(4, LongStream.iterate(1, e -> e < 6, e -> e + 1).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.iterate(1, e -> e < 6, e -> e + 1).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.iterate(1, e -> e < 6, e -> e + 1).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.iterate(1, e -> e < 6, e -> e + 1).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.iterate(1, e -> e < 6, e -> e + 1).skip(1).toList());
        assertEquals(5, LongStream.iterate(1, e -> e < 6, e -> e + 1).map(e -> e).count());
        assertEquals(4, LongStream.iterate(1, e -> e < 6, e -> e + 1).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.iterate(1, e -> e < 6, e -> e + 1).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.iterate(1, e -> e < 6, e -> e + 1).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.iterate(1, e -> e < 6, e -> e + 1).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.iterate(1, e -> e < 6, e -> e + 1).map(e -> e).skip(1).toList());
    }

    @Test
    public void testIterateInfinite() {
        assertEquals(5, LongStream.iterate(1, e -> e + 1).limit(5).count());
        assertEquals(4, LongStream.iterate(1, e -> e + 1).limit(5).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.iterate(1, e -> e + 1).limit(5).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.iterate(1, e -> e + 1).limit(5).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.iterate(1, e -> e + 1).limit(5).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.iterate(1, e -> e + 1).limit(5).skip(1).toList());
        assertEquals(5, LongStream.iterate(1, e -> e + 1).map(e -> e).limit(5).count());
        assertEquals(4, LongStream.iterate(1, e -> e + 1).map(e -> e).limit(5).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.iterate(1, e -> e + 1).map(e -> e).limit(5).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.iterate(1, e -> e + 1).map(e -> e).limit(5).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), LongStream.iterate(1, e -> e + 1).map(e -> e).limit(5).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L), LongStream.iterate(1, e -> e + 1).map(e -> e).limit(5).skip(1).toList());
    }

    @Test
    public void testGenerate() {
        LongSupplier supplier = new LongSupplier() {
            private long value = 1;

            @Override
            public long getAsLong() {
                return value++;
            }
        };

        assertEquals(5, LongStream.generate(supplier).limit(5).count());

        supplier = new LongSupplier() {
            private long value = 1;

            @Override
            public long getAsLong() {
                return value++;
            }
        };
        assertEquals(4, LongStream.generate(supplier).limit(5).skip(1).count());

        supplier = new LongSupplier() {
            private long value = 1;

            @Override
            public long getAsLong() {
                return value++;
            }
        };
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.generate(supplier).limit(5).toArray());

        supplier = new LongSupplier() {
            private long value = 1;

            @Override
            public long getAsLong() {
                return value++;
            }
        };
        assertArrayEquals(new long[] { 2, 3, 4, 5 }, LongStream.generate(supplier).limit(5).skip(1).toArray());
    }

    // Continue with concat, zip, merge tests...

    @Test
    public void testConcatArrays() {
        assertEquals(9, LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).count());
        assertEquals(8, LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).skip(1).toList());
        assertEquals(9, LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).count());
        assertEquals(8, LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).skip(1).toList());
    }

    @Test
    public void testConcatIterators() {
        LongIterator iter1 = LongIterator.of(new long[] { 1, 2, 3 });
        LongIterator iter2 = LongIterator.of(new long[] { 4, 5 });
        LongIterator iter3 = LongIterator.of(new long[] { 6, 7, 8, 9 });
        assertEquals(9, LongStream.concat(iter1, iter2, iter3).count());

        iter1 = LongIterator.of(new long[] { 1, 2, 3 });
        iter2 = LongIterator.of(new long[] { 4, 5 });
        iter3 = LongIterator.of(new long[] { 6, 7, 8, 9 });
        assertEquals(8, LongStream.concat(iter1, iter2, iter3).skip(1).count());

        iter1 = LongIterator.of(new long[] { 1, 2, 3 });
        iter2 = LongIterator.of(new long[] { 4, 5 });
        iter3 = LongIterator.of(new long[] { 6, 7, 8, 9 });
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(iter1, iter2, iter3).toArray());

        iter1 = LongIterator.of(new long[] { 1, 2, 3 });
        iter2 = LongIterator.of(new long[] { 4, 5 });
        iter3 = LongIterator.of(new long[] { 6, 7, 8, 9 });
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(iter1, iter2, iter3).skip(1).toArray());
    }

    @Test
    public void testConcatStreams() {
        assertEquals(9, LongStream.concat(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9)).count());
        assertEquals(8, LongStream.concat(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9)).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                LongStream.concat(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9)).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                LongStream.concat(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9)).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                LongStream.concat(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9)).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                LongStream.concat(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9)).skip(1).toList());
        assertEquals(9, LongStream.concat(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9)).map(e -> e).count());
        assertEquals(8, LongStream.concat(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9)).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                LongStream.concat(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9)).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                LongStream.concat(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9)).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                LongStream.concat(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9)).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                LongStream.concat(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9)).map(e -> e).skip(1).toList());
    }

    @Test
    public void testConcatListOfArrays() {
        List<long[]> list = Arrays.asList(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 });
        assertEquals(9, LongStream.concat(list).count());
        assertEquals(8, LongStream.concat(list).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(list).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(list).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(list).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(list).skip(1).toList());
        assertEquals(9, LongStream.concat(list).map(e -> e).count());
        assertEquals(8, LongStream.concat(list).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(list).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(list).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(list).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(list).map(e -> e).skip(1).toList());
    }

    @Test
    public void testConcatCollectionOfStreams() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertEquals(9, LongStream.concat(streams).count());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertEquals(8, LongStream.concat(streams).skip(1).count());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(streams).toArray());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(streams).skip(1).toArray());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(streams).toList());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(streams).skip(1).toList());
    }

    @Test
    public void testConcatIterators2() {
        Collection<LongIterator> iterators = Arrays.asList(LongIterator.of(new long[] { 1, 2, 3 }), LongIterator.of(new long[] { 4, 5 }),
                LongIterator.of(new long[] { 6, 7, 8, 9 }));
        assertEquals(9, LongStream.concatIterators(iterators).count());

        iterators = Arrays.asList(LongIterator.of(new long[] { 1, 2, 3 }), LongIterator.of(new long[] { 4, 5 }), LongIterator.of(new long[] { 6, 7, 8, 9 }));
        assertEquals(8, LongStream.concatIterators(iterators).skip(1).count());

        iterators = Arrays.asList(LongIterator.of(new long[] { 1, 2, 3 }), LongIterator.of(new long[] { 4, 5 }), LongIterator.of(new long[] { 6, 7, 8, 9 }));
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concatIterators(iterators).toArray());
    }

    @Test
    public void testZipArrays() {
        assertEquals(3, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).count());
        assertEquals(2, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 11, 22, 33 }, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 22, 33 }, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(11L, 22L, 33L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(22L, 33L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).count());
        assertEquals(2, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 11, 22, 33 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).toArray());
        assertArrayEquals(new long[] { 22, 33 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(11L, 22L, 33L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).toList());
        assertEquals(Arrays.asList(22L, 33L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).skip(1).toList());
    }

    @Test
    public void testZip3Arrays() {
        assertEquals(3, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).count());
        assertEquals(2,
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new long[] { 111, 222, 333 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).toArray());
        assertArrayEquals(new long[] { 222, 333 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).skip(1).toArray());
        assertEquals(Arrays.asList(111L, 222L, 333L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).toList());
        assertEquals(Arrays.asList(222L, 333L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).skip(1).toList());
        assertEquals(3,
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .count());
        assertEquals(2,
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .skip(1)
                        .count());
        assertArrayEquals(new long[] { 111, 222, 333 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .toArray());
        assertArrayEquals(new long[] { 222, 333 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .skip(1)
                        .toArray());
        assertEquals(Arrays.asList(111L, 222L, 333L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .toList());
        assertEquals(Arrays.asList(222L, 333L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testZipIterators() {
        LongIterator iter1 = LongIterator.of(new long[] { 1, 2, 3 });
        LongIterator iter2 = LongIterator.of(new long[] { 10, 20, 30, 40 });
        assertEquals(3, LongStream.zip(iter1, iter2, (a, b) -> a + b).count());

        iter1 = LongIterator.of(new long[] { 1, 2, 3 });
        iter2 = LongIterator.of(new long[] { 10, 20, 30, 40 });
        assertEquals(2, LongStream.zip(iter1, iter2, (a, b) -> a + b).skip(1).count());

        iter1 = LongIterator.of(new long[] { 1, 2, 3 });
        iter2 = LongIterator.of(new long[] { 10, 20, 30, 40 });
        assertArrayEquals(new long[] { 11, 22, 33 }, LongStream.zip(iter1, iter2, (a, b) -> a + b).toArray());

        iter1 = LongIterator.of(new long[] { 1, 2, 3 });
        iter2 = LongIterator.of(new long[] { 10, 20, 30, 40 });
        assertArrayEquals(new long[] { 22, 33 }, LongStream.zip(iter1, iter2, (a, b) -> a + b).skip(1).toArray());
    }

    @Test
    public void testZipStreams() {
        assertEquals(3, LongStream.zip(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30, 40), (a, b) -> a + b).count());
        assertEquals(2, LongStream.zip(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30, 40), (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 11, 22, 33 }, LongStream.zip(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30, 40), (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 22, 33 }, LongStream.zip(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30, 40), (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(11L, 22L, 33L), LongStream.zip(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30, 40), (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(22L, 33L), LongStream.zip(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30, 40), (a, b) -> a + b).skip(1).toList());
        assertEquals(3, LongStream.zip(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30, 40), (a, b) -> a + b).map(e -> e).count());
        assertEquals(2, LongStream.zip(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30, 40), (a, b) -> a + b).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 11, 22, 33 },
                LongStream.zip(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30, 40), (a, b) -> a + b).map(e -> e).toArray());
        assertArrayEquals(new long[] { 22, 33 },
                LongStream.zip(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30, 40), (a, b) -> a + b).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(11L, 22L, 33L), LongStream.zip(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30, 40), (a, b) -> a + b).map(e -> e).toList());
        assertEquals(Arrays.asList(22L, 33L),
                LongStream.zip(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30, 40), (a, b) -> a + b).map(e -> e).skip(1).toList());
    }

    @Test
    public void testZipCollectionOfStreams() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30), LongStream.of(100, 200, 300));
        LongNFunction<Long> zipFunc = values -> Stream.of(values).mapToLong(v -> v).sum();

        assertEquals(3, LongStream.zip(streams, zipFunc).count());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30), LongStream.of(100, 200, 300));
        assertEquals(2, LongStream.zip(streams, zipFunc).skip(1).count());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30), LongStream.of(100, 200, 300));
        assertArrayEquals(new long[] { 111, 222, 333 }, LongStream.zip(streams, zipFunc).toArray());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30), LongStream.of(100, 200, 300));
        assertArrayEquals(new long[] { 222, 333 }, LongStream.zip(streams, zipFunc).skip(1).toArray());
    }

    @Test
    public void testZipArraysWithDefaults() {
        assertEquals(3, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).count());
        assertEquals(2, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 11, 22, 3 }, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 22, 3 }, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(11L, 22L, 3L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(22L, 3L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).count());
        assertEquals(2, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 11, 22, 3 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).toArray());
        assertArrayEquals(new long[] { 22, 3 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(11L, 22L, 3L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).toList());
        assertEquals(Arrays.asList(22L, 3L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).skip(1).toList());
    }

    @Test
    public void testMergeArrays() {
        assertEquals(5,
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(4,
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 },
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 },
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L),
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L),
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toList());
        assertEquals(5,
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .count());
        assertEquals(4,
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 },
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 },
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L),
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L),
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testMerge3Arrays() {
        assertEquals(7,
                LongStream
                        .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 },
                                (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .count());
        assertEquals(6, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .skip(1)
                .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7 },
                LongStream
                        .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 },
                                (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7 }, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .skip(1)
                .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L),
                LongStream
                        .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 },
                                (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L), LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .skip(1)
                .toList());
        assertEquals(7, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .count());
        assertEquals(6, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .skip(1)
                .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7 }, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7 }, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .skip(1)
                .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L), LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L), LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .skip(1)
                .toList());
    }

    @Test
    public void testMergeIterators() {
        LongIterator iter1 = LongIterator.of(new long[] { 1, 3, 5 });
        LongIterator iter2 = LongIterator.of(new long[] { 2, 4 });
        assertEquals(5, LongStream.merge(iter1, iter2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());

        iter1 = LongIterator.of(new long[] { 1, 3, 5 });
        iter2 = LongIterator.of(new long[] { 2, 4 });
        assertEquals(4, LongStream.merge(iter1, iter2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());

        iter1 = LongIterator.of(new long[] { 1, 3, 5 });
        iter2 = LongIterator.of(new long[] { 2, 4 });
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 },
                LongStream.merge(iter1, iter2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());

        iter1 = LongIterator.of(new long[] { 1, 3, 5 });
        iter2 = LongIterator.of(new long[] { 2, 4 });
        assertArrayEquals(new long[] { 2, 3, 4, 5 },
                LongStream.merge(iter1, iter2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray());
    }

    @Test
    public void testMergeStreams() {
        assertEquals(5,
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(4,
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 },
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 },
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L),
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L),
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toList());
        assertEquals(5,
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .count());
        assertEquals(4,
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 },
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 },
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L),
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L),
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testMergeCollectionOfStreams() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        LongBiFunction<MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        assertEquals(6, LongStream.merge(streams, selector).count());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(5, LongStream.merge(streams, selector).skip(1).count());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, LongStream.merge(streams, selector).toArray());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6 }, LongStream.merge(streams, selector).skip(1).toArray());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), LongStream.merge(streams, selector).toList());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L), LongStream.merge(streams, selector).skip(1).toList());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(6, LongStream.merge(streams, selector).map(e -> e).count());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(5, LongStream.merge(streams, selector).map(e -> e).skip(1).count());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, LongStream.merge(streams, selector).map(e -> e).toArray());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6 }, LongStream.merge(streams, selector).map(e -> e).skip(1).toArray());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), LongStream.merge(streams, selector).map(e -> e).toList());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L), LongStream.merge(streams, selector).map(e -> e).skip(1).toList());
    }
}
