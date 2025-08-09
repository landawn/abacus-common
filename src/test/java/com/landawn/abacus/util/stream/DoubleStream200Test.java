package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.u.OptionalDouble;


public class DoubleStream200Test extends TestBase {

    @Test
    public void testOf() {
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, DoubleStream.of(1.0, 2.0, 3.0).toArray());
        assertArrayEquals(new double[0], DoubleStream.of().toArray());
    }

    @Test
    public void testOfNullable() {
        assertEquals(0, DoubleStream.ofNullable(null).count());
        assertArrayEquals(new double[] { 1.0 }, DoubleStream.ofNullable(1.0).toArray());
    }

    @Test
    public void testEmpty() {
        assertEquals(0, DoubleStream.empty().count());
    }

    @Test
    public void testIterate() {
        assertArrayEquals(new double[] { 1.0, 2.0, 4.0, 8.0, 16.0 }, DoubleStream.iterate(1.0, d -> d * 2).limit(5).toArray());
    }

    @Test
    public void testGenerate() {
        assertEquals(5, DoubleStream.generate(() -> 1.0).limit(5).count());
        assertEquals(5.0, DoubleStream.generate(() -> 1.0).limit(5).sum(), 0.0);
    }

    @Test
    public void testConcat() {
        DoubleStream a = DoubleStream.of(1.0, 2.0);
        DoubleStream b = DoubleStream.of(3.0, 4.0);
        DoubleStream c = DoubleStream.of(5.0, 6.0);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, DoubleStream.concat(a, b, c).toArray());
    }

    @Test
    public void testFilter() {
        assertArrayEquals(new double[] { 2.0, 4.0 }, DoubleStream.of(1.0, 2.0, 3.0, 4.0).filter(d -> d % 2 == 0).toArray());
    }

    @Test
    public void testMap() {
        assertArrayEquals(new double[] { 2.0, 4.0, 6.0 }, DoubleStream.of(1.0, 2.0, 3.0).map(d -> d * 2).toArray());
    }

    @Test
    public void testFlatMap() {
        assertArrayEquals(new double[] { 1.0, -1.0, 2.0, -2.0 }, DoubleStream.of(1.0, 2.0).flatMap(d -> DoubleStream.of(d, -d)).toArray());
    }

    @Test
    public void testDistinct() {
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, DoubleStream.of(1.0, 2.0, 2.0, 3.0, 3.0, 3.0).distinct().toArray());
    }

    @Test
    public void testSorted() {
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, DoubleStream.of(3.0, 1.0, 4.0, 2.0).sorted().toArray());
    }

    @Test
    public void testPeek() {
        List<Double> peeked = new ArrayList<>();
        DoubleStream.of(1.0, 2.0).peek(peeked::add).toArray();
        assertEquals(2, peeked.size());
        assertEquals(1.0, peeked.get(0));
        assertEquals(2.0, peeked.get(1));
    }

    @Test
    public void testLimit() {
        assertArrayEquals(new double[] { 1.0, 2.0 }, DoubleStream.of(1.0, 2.0, 3.0).limit(2).toArray());
    }

    @Test
    public void testSkip() {
        assertArrayEquals(new double[] { 3.0, 4.0 }, DoubleStream.of(1.0, 2.0, 3.0, 4.0).skip(2).toArray());
    }

    @Test
    public void testForEach() {
        List<Double> list = new ArrayList<>();
        DoubleStream.of(1.0, 2.0, 3.0).forEach(list::add);
        assertEquals(3, list.size());
    }

    @Test
    public void testToArray() {
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, DoubleStream.of(1.0, 2.0, 3.0).toArray());
    }

    @Test
    public void testReduceWithIdentity() {
        double result = DoubleStream.of(1.0, 2.0, 3.0).reduce(10.0, (a, b) -> a + b);
        assertEquals(16.0, result);
    }

    @Test
    public void testReduce() {
        OptionalDouble result = DoubleStream.of(1.0, 2.0, 3.0).reduce((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(6.0, result.getAsDouble());
    }

    @Test
    public void testReduceOnEmptyStream() {
        OptionalDouble result = DoubleStream.empty().reduce((a, b) -> a + b);
        assertFalse(result.isPresent());
    }

    @Test
    public void testCollect() {
        DoubleList result = DoubleStream.of(1.0, 2.0, 3.0).collect(DoubleList::new, DoubleList::add);
        assertEquals(3, result.size());
        assertTrue(result.contains(1.0));
    }

    @Test
    public void testSum() {
        assertEquals(6.0, DoubleStream.of(1.0, 2.0, 3.0).sum());
        assertEquals(0.0, DoubleStream.empty().sum());
    }

    @Test
    public void testAverage() {
        OptionalDouble avg = DoubleStream.of(1.0, 2.0, 3.0).average();
        assertTrue(avg.isPresent());
        assertEquals(2.0, avg.getAsDouble());
    }

    @Test
    public void testAverageOnEmptyStream() {
        OptionalDouble avg = DoubleStream.empty().average();
        assertFalse(avg.isPresent());
    }

    @Test
    public void testSummarize() {
        DoubleSummaryStatistics stats = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).summarize();
        assertEquals(5, stats.getCount());
        assertEquals(15.0, stats.getSum());
        assertEquals(1.0, stats.getMin());
        assertEquals(5.0, stats.getMax());
        assertEquals(3.0, stats.getAverage());
    }

    @Test
    public void testMin() {
        OptionalDouble min = DoubleStream.of(3.0, 1.0, 2.0).min();
        assertTrue(min.isPresent());
        assertEquals(1.0, min.getAsDouble());
    }

    @Test
    public void testMax() {
        OptionalDouble max = DoubleStream.of(3.0, 1.0, 2.0).max();
        assertTrue(max.isPresent());
        assertEquals(3.0, max.getAsDouble());
    }

    @Test
    public void testCount() {
        assertEquals(3, DoubleStream.of(1.0, 2.0, 3.0).count());
    }

    @Test
    public void testAnyMatch() {
        assertTrue(DoubleStream.of(1.0, 2.0, 3.0).anyMatch(d -> d == 2.0));
        assertFalse(DoubleStream.of(1.0, 2.0, 3.0).anyMatch(d -> d == 4.0));
    }

    @Test
    public void testAllMatch() {
        assertTrue(DoubleStream.of(2.0, 4.0, 6.0).allMatch(d -> d % 2 == 0));
        assertFalse(DoubleStream.of(1.0, 2.0, 3.0).allMatch(d -> d % 2 == 0));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(DoubleStream.of(1.0, 3.0, 5.0).noneMatch(d -> d % 2 == 0));
        assertFalse(DoubleStream.of(1.0, 2.0, 3.0).noneMatch(d -> d % 2 == 0));
    }

    @Test
    public void testFindFirst() {
        OptionalDouble first = DoubleStream.of(1.0, 2.0, 3.0).first();
        assertTrue(first.isPresent());
        assertEquals(1.0, first.getAsDouble());
    }

    @Test
    public void testFindFirstOnEmptyStream() {
        OptionalDouble first = DoubleStream.empty().first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testBoxed() {
        List<Double> list = DoubleStream.of(1.0, 2.0, 3.0).boxed().toList();
        assertEquals(3, list.size());
        assertEquals(1.0, list.get(0));
    }

    @Test
    public void testIterator() {
        DoubleIterator it = DoubleStream.of(1.0, 2.0, 3.0).iterator();
        assertTrue(it.hasNext());
        assertEquals(1.0, it.nextDouble());
        assertEquals(2.0, it.nextDouble());
        assertEquals(3.0, it.nextDouble());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::nextDouble);
    }

    @Test
    public void testZipWith() {
        DoubleStream s1 = DoubleStream.of(1.0, 2.0, 3.0);
        DoubleStream s2 = DoubleStream.of(4.0, 5.0, 6.0);
        double[] expected = { 5.0, 7.0, 9.0 };
        assertArrayEquals(expected, s1.zipWith(s2, (a, b) -> a + b).toArray());
    }

    @Test
    public void testReversed() {
        assertArrayEquals(new double[] { 3.0, 2.0, 1.0 }, DoubleStream.of(1.0, 2.0, 3.0).reversed().toArray());
    }

    @Test
    public void testTop() {
        assertArrayEquals(new double[] { 4.0, 5.0 }, DoubleStream.of(1.0, 5.0, 2.0, 4.0, 3.0).top(2).toArray());
    }

    @Test
    public void testKthLargest() {
        OptionalDouble result = DoubleStream.of(1.0, 5.0, 2.0, 4.0, 3.0).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble());
    }
}
