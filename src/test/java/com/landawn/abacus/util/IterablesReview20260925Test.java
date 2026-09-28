package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Pins for the 2026-09-25 fixes in {@link Iterables} (review findings U02-01, U02-02/U19-01, U19-03).
 */
public class IterablesReview20260925Test extends TestBase {

    // ---------------------------------------------------------------------------------------------
    // U02-01 (L12/C-235): sumDouble shares N's overflow-safe accumulator
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testU0201_sumDoubleIsInfiniteOnlyForATrueOverflow() {
        final List<Double> a = Arrays.asList(1e308, 1e308, -1e308, -1e308, 5.0);
        final List<Double> b = Arrays.asList(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE);
        final List<Double> c = Arrays.asList(Double.MAX_VALUE, 1e292, -Double.MAX_VALUE);
        final List<Double> d = Arrays.asList(-Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);

        assertEquals(5.0, Iterables.sumDouble(a).get());
        assertEquals(Double.MAX_VALUE, Iterables.sumDouble(b).get());
        assertEquals(1e292, Iterables.sumDouble(c).get());
        assertEquals(0.0, Iterables.sumDouble(d).get());

        // the ToDoubleFunction overload (the Number overload delegates to it) and the N twin agree
        assertEquals(5.0, Iterables.sumDouble(Arrays.asList("1e308", "1e308", "-1e308", "-1e308", "5"), Double::parseDouble).get());
        for (final List<Double> list : Arrays.asList(a, b, c, d)) {
            assertEquals(N.sumDouble(list), Iterables.sumDouble(list).get(), list.toString());
            assertEquals(N.sumDouble(list, Double::doubleValue), Iterables.sumDouble(list, Double::doubleValue).get(), list.toString());
            // averageDouble is overflow-safe the same way (average * count is not promised to reproduce the sum: it can
            // itself overflow, as MAX / 3 * 3 does)
            final double expectedAverage = N.sumDouble(list) / list.size();
            assertEquals(expectedAverage, Iterables.averageDouble(list).get(), Math.abs(expectedAverage) * 1e-12, list.toString());
        }

        // a true overflow and IEEE propagation are unchanged
        assertEquals(Double.POSITIVE_INFINITY, Iterables.sumDouble(Arrays.asList(Double.MAX_VALUE, Double.MAX_VALUE)).get());
        assertEquals(Double.NEGATIVE_INFINITY, Iterables.sumDouble(Arrays.asList(-Double.MAX_VALUE, -Double.MAX_VALUE)).get());
        assertEquals(Double.POSITIVE_INFINITY, Iterables.sumDouble(Arrays.asList(Double.POSITIVE_INFINITY, 1.0)).get());
        assertTrue(Double.isNaN(Iterables.sumDouble(Arrays.asList(1e308, 1e308, Double.NaN)).get()));
        assertTrue(Double.isNaN(Iterables.sumDouble(Arrays.asList(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)).get()));
        assertEquals(Double.POSITIVE_INFINITY, Iterables.sumDouble(Arrays.asList(1e308, 1e308, Double.POSITIVE_INFINITY, -1e308)).get());

        // ordinary inputs, nulls, empty and null sources
        assertEquals(15.0, Iterables.sumDouble(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0)).get());
        assertEquals(6.0, Iterables.sumDouble(Arrays.asList("a", "bb", "ccc"), s -> (double) s.length()).get());
        assertEquals(3.0, Iterables.sumDouble(Arrays.<Number> asList(1, null, 2L)).get());
        assertFalse(Iterables.sumDouble(Collections.<Double> emptyList()).isPresent());
        assertFalse(Iterables.sumDouble((List<Double>) null).isPresent());
        assertEquals(0.0, Iterables.sumDouble(Arrays.asList(-0.0)).get());
        assertThrows(IllegalArgumentException.class, () -> Iterables.sumDouble(Arrays.asList(1.0), null));
    }

    // ---------------------------------------------------------------------------------------------
    // U02-02 / U19-01 (L12/C-311): the shared exact conversion also serves this class
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testU0202_exactConversionFastPathServesTheIterablesAggregates() {
        assertEquals(6, Iterables.sumInt(Arrays.<Number> asList(MutableInt.of(1), new AtomicInteger(2), MutableShort.of((short) 3), null)).get());
        assertEquals(4L, Iterables.sumIntToLong(Arrays.<Number> asList(MutableInt.of(1), new AtomicInteger(3))).get());
        assertEquals(2.0, Iterables.averageInt(Arrays.<Number> asList(MutableInt.of(1), new AtomicInteger(3))).get());
        assertEquals(2.0, Iterables.averageLong(Arrays.<Number> asList(MutableLong.of(1), new AtomicLong(3))).get());
        assertEquals((double) Long.MAX_VALUE, Iterables.averageLong(Arrays.<Number> asList(MutableLong.of(Long.MAX_VALUE))).get());
        assertThrows(ArithmeticException.class, () -> Iterables.sumInt(Arrays.<Number> asList(MutableLong.of(3_000_000_000L))));
        assertThrows(ArithmeticException.class, () -> Iterables.sumInt(Arrays.<Number> asList(3_000_000_000L)));
        assertThrows(ArithmeticException.class, () -> Iterables.averageInt(Arrays.<Number> asList(new AtomicLong(-3_000_000_000L))));
    }

    // ---------------------------------------------------------------------------------------------
    // U19-03 (L12/C-319): CartesianList.contains shares the guarded axis probe of indexOf/lastIndexOf
    // ---------------------------------------------------------------------------------------------

    /** A List whose iteration disagrees with its size(). */
    private static final class LyingList extends AbstractList<Object> {
        private final List<Object> yielded;
        private final int claimedSize;

        LyingList(final int claimedSize, final Object... yielded) {
            this.yielded = Arrays.asList(yielded);
            this.claimedSize = claimedSize;
        }

        @Override
        public Object get(final int index) {
            return index < yielded.size() ? yielded.get(index) : null;
        }

        @Override
        public int size() {
            return claimedSize;
        }

        @Override
        public Iterator<Object> iterator() {
            return yielded.iterator();
        }
    }

    @Test
    public void testU1903_containsGuardsAListWhoseIterationDisagreesWithItsSize() {
        final List<List<Object>> product = Iterables.<Object> cartesianProduct(Arrays.asList(1, 2), Arrays.asList("a", "b"));

        // yields more elements than size() reports: contains() used to read past the last axis (AIOOBE)
        final List<Object> tooMany = new LyingList(2, 1, "a", "b");
        assertFalse(product.contains(tooMany));
        assertEquals(-1, product.indexOf(tooMany));
        assertEquals(-1, product.lastIndexOf(tooMany));

        // yields fewer elements than size() reports: contains() used to say true
        final List<Object> tooFew = new LyingList(2, 1);
        assertFalse(product.contains(tooFew));
        assertEquals(-1, product.indexOf(tooFew));

        // honest lists, all kinds
        assertTrue(product.contains(Arrays.asList(1, "a")));
        assertTrue(product.contains(new LinkedList<>(Arrays.asList(2, "b"))));
        assertTrue(product.contains(new LyingList(2, 2, "a")));
        assertFalse(product.contains(Arrays.asList(1, "c")));
        assertFalse(product.contains(Arrays.asList("a", 1)));
        assertFalse(product.contains(Arrays.asList(1)));
        assertFalse(product.contains(Arrays.asList(1, "a", 2)));
        assertFalse(product.contains(Collections.emptyList()));
        assertFalse(product.contains("x"));
        assertFalse(product.contains(null));
        assertFalse(product.contains(Arrays.asList(1, null)));

        for (final List<Object> tuple : product) {
            assertTrue(product.contains(tuple), tuple.toString());
            assertTrue(product.contains(new LyingList(2, tuple.toArray())), tuple.toString());
        }

        // null axis elements and an empty product
        final List<List<Object>> withNull = Iterables.<Object> cartesianProduct(Arrays.asList(1, null), Arrays.asList("a"));
        assertTrue(withNull.contains(Arrays.asList(null, "a")));
        assertFalse(withNull.contains(Arrays.asList(null, null)));
        final List<List<Object>> empty = Iterables.<Object> cartesianProduct(Arrays.asList(1, 2), Collections.emptyList());
        assertFalse(empty.contains(Arrays.asList(1, "a")));
        assertFalse(empty.contains(Collections.emptyList()));
    }
}
