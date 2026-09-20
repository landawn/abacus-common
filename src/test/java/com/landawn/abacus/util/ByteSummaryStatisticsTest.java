package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ByteSummaryStatisticsTest extends TestBase {

    @Test
    public void testDefaultConstructor() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(0L, stats.getCount());
        assertEquals(0L, stats.getSum());
        assertEquals(Byte.MAX_VALUE, stats.getMin());
        assertEquals(Byte.MIN_VALUE, stats.getMax());
        assertEquals(0.0, stats.getAverage());
    }

    @Test
    public void testConstructorWithValues() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics(3, (byte) 10, (byte) 30, 60);
        assertEquals(3L, stats.getCount());
        assertEquals((byte) 10, stats.getMin());
        assertEquals((byte) 30, stats.getMax());
        assertEquals(60L, stats.getSum());
    }

    @Test
    public void testConstructorValidatesEmptyState() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> new ByteSummaryStatistics(0, Byte.MAX_VALUE, Byte.MIN_VALUE, 0));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new ByteSummaryStatistics(0, (byte) 0, (byte) 0, 1));
    }

    @Test
    public void testExtremeValues() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept(Byte.MIN_VALUE);
        stats.accept(Byte.MAX_VALUE);

        assertEquals(2L, stats.getCount());
        assertEquals(Byte.MIN_VALUE, stats.getMin());
        assertEquals(Byte.MAX_VALUE, stats.getMax());
    }

    @Test
    public void testParameterizedConstructor() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics(5, (byte) 10, (byte) 50, 150);
        assertEquals(5, stats.getCount());
        assertEquals((byte) 10, stats.getMin());
        assertEquals((byte) 50, stats.getMax());
        assertEquals(150L, stats.getSum());
        assertEquals(30.0, stats.getAverage());
    }

    @Test
    public void testBoundaryValues() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept(Byte.MIN_VALUE);
        stats.accept(Byte.MAX_VALUE);

        assertEquals(2, stats.getCount());
        assertEquals(Byte.MIN_VALUE, stats.getMin());
        assertEquals(Byte.MAX_VALUE, stats.getMax());
        assertEquals(-1L, stats.getSum());
        assertEquals(-0.5, stats.getAverage());
    }

    @Test
    public void testAcceptSingleValue() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 25);

        assertEquals(1L, stats.getCount());
        assertEquals(25L, stats.getSum());
        assertEquals((byte) 25, stats.getMin());
        assertEquals((byte) 25, stats.getMax());
        assertEquals(25.0, stats.getAverage());
    }

    @Test
    public void testAcceptMultipleValues() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 10);
        stats.accept((byte) 20);
        stats.accept((byte) 30);

        assertEquals(3L, stats.getCount());
        assertEquals(60L, stats.getSum());
        assertEquals((byte) 10, stats.getMin());
        assertEquals((byte) 30, stats.getMax());
        assertEquals(20.0, stats.getAverage());
    }

    @Test
    public void testAcceptNegativeValues() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) -10);
        stats.accept((byte) -20);
        stats.accept((byte) -5);

        assertEquals(3L, stats.getCount());
        assertEquals(-35L, stats.getSum());
        assertEquals((byte) -20, stats.getMin());
        assertEquals((byte) -5, stats.getMax());
    }

    @Test
    public void testAcceptZero() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 0);

        assertEquals(1L, stats.getCount());
        assertEquals(0L, stats.getSum());
        assertEquals((byte) 0, stats.getMin());
        assertEquals((byte) 0, stats.getMax());
        assertEquals(0.0, stats.getAverage());
    }

    @Test
    public void testAcceptSameValueMultipleTimes() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 5);
        stats.accept((byte) 5);
        stats.accept((byte) 5);

        assertEquals(3L, stats.getCount());
        assertEquals(15L, stats.getSum());
        assertEquals((byte) 5, stats.getMin());
        assertEquals((byte) 5, stats.getMax());
        assertEquals(5.0, stats.getAverage());
    }

    @Test
    public void testAcceptUpdatesMinAndMax() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();

        stats.accept((byte) 50);
        assertEquals((byte) 50, stats.getMin());
        assertEquals((byte) 50, stats.getMax());

        stats.accept((byte) 10);
        assertEquals((byte) 10, stats.getMin());
        assertEquals((byte) 50, stats.getMax());

        stats.accept((byte) 100);
        assertEquals((byte) 10, stats.getMin());
        assertEquals((byte) 100, stats.getMax());
    }

    @Test
    public void testCombine() {
        ByteSummaryStatistics stats1 = new ByteSummaryStatistics();
        stats1.accept((byte) 10);
        stats1.accept((byte) 20);

        ByteSummaryStatistics stats2 = new ByteSummaryStatistics();
        stats2.accept((byte) 30);
        stats2.accept((byte) 40);

        stats1.combine(stats2);

        assertEquals(4L, stats1.getCount());
        assertEquals(100L, stats1.getSum());
        assertEquals((byte) 10, stats1.getMin());
        assertEquals((byte) 40, stats1.getMax());
    }

    @Test
    public void testCombineWithEmpty() {
        ByteSummaryStatistics stats1 = new ByteSummaryStatistics();
        stats1.accept((byte) 10);
        stats1.accept((byte) 20);

        ByteSummaryStatistics stats2 = new ByteSummaryStatistics();

        stats1.combine(stats2);

        assertEquals(2, stats1.getCount());
        assertEquals((byte) 10, stats1.getMin());
        assertEquals((byte) 20, stats1.getMax());
        assertEquals(30L, stats1.getSum());
        assertEquals(15.0, stats1.getAverage());
    }

    @Test
    public void testCombineEmptyWithNonEmpty() {
        ByteSummaryStatistics stats1 = new ByteSummaryStatistics();

        ByteSummaryStatistics stats2 = new ByteSummaryStatistics();
        stats2.accept((byte) 30);
        stats2.accept((byte) 40);

        stats1.combine(stats2);

        assertEquals(2, stats1.getCount());
        assertEquals((byte) 30, stats1.getMin());
        assertEquals((byte) 40, stats1.getMax());
        assertEquals(70L, stats1.getSum());
        assertEquals(35.0, stats1.getAverage());
    }

    @Test
    public void testGetMin() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 30);
        stats.accept((byte) 10);
        stats.accept((byte) 20);

        assertEquals((byte) 10, stats.getMin());
    }

    @Test
    public void testGetMinWithNoValues() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(Byte.MAX_VALUE, stats.getMin());
    }

    @Test
    public void testGetMax() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 20);
        stats.accept((byte) 30);
        stats.accept((byte) 10);

        assertEquals((byte) 30, stats.getMax());
    }

    @Test
    public void testGetMaxWithNoValues() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(Byte.MIN_VALUE, stats.getMax());
    }

    @Test
    public void testGetCount() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(0L, stats.getCount());

        stats.accept((byte) 10);
        assertEquals(1L, stats.getCount());

        stats.accept((byte) 20);
        assertEquals(2L, stats.getCount());
    }

    @Test
    public void testGetCountEmpty() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testGetSum() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(0L, stats.getSum());

        stats.accept((byte) 10);
        assertEquals(10L, stats.getSum());

        stats.accept((byte) 20);
        assertEquals(30L, stats.getSum());
    }

    @Test
    public void testGetSumEmpty() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(0L, stats.getSum());
    }

    @Test
    public void testGetAverage() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 10);
        stats.accept((byte) 20);
        stats.accept((byte) 30);

        assertEquals(20.0, stats.getAverage());
    }

    @Test
    public void testGetAverageEmpty() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(0.0, stats.getAverage());
    }

    @Test
    public void testToString() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 10);
        stats.accept((byte) 20);

        String str = stats.toString();
        assertNotNull(str);
        assertTrue(str.contains("min="));
        assertTrue(str.contains("max="));
        assertTrue(str.contains("count="));
        assertTrue(str.contains("sum="));
        assertTrue(str.contains("average="));
    }

    // FINDING 27: toString() must render the same text on every machine. Byte/Char/Short built their text with the
    // default locale, so the average printed as "15,000000" under a comma-decimal locale and the integral
    // conversions used the locale's own zero digit, while FloatSummaryStatistics already pinned Locale.ROOT - one
    // family, two renderings.
    @Test
    public void reviewFixes20260908_toStringRendersWithLocaleRootWhateverTheDefaultLocaleIs() {
        final ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 10);
        stats.accept((byte) 20);

        final String expected = "{min=10, max=20, count=2, sum=30, average=15.000000}";
        assertEquals(expected, stats.toString());

        final java.util.Locale prev = java.util.Locale.getDefault();

        try {
            for (final String tag : new String[] { "de-DE", "fr-FR", "hi-IN-u-nu-deva", "ar-EG-u-nu-arab" }) {
                java.util.Locale.setDefault(java.util.Locale.forLanguageTag(tag));
                assertEquals(expected, stats.toString(), tag);
            }
        } finally {
            java.util.Locale.setDefault(prev);
        }
    }

    // Doc pin for the class-javadoc paragraph added on 2026-09-11: bytes are aggregated as SIGNED values, so
    // accept((byte) 200) records -56. getMin()/getMax() are bytes, and on the accept() path the average stays
    // inside -128..127 too, but getSum() is a long running total and routinely leaves that interval - the reason
    // the text does NOT claim getSum() is bounded by it. The average bound is accept()/combine()-only; the
    // four-argument constructor escapes it, which is pinned by
    // reviewFixes20260911_fourArgConstructorDoesNotCrossCheckSumSoAverageIsUnbounded below.
    @Test
    public void reviewFixes20260911_valuesAreAggregatedAsSignedBytesAndTheSumIsNotBoundedByTheByteRange() {
        final ByteSummaryStatistics signed = new ByteSummaryStatistics();
        signed.accept((byte) 200);
        signed.accept((byte) 5);
        signed.accept((byte) 128);

        assertEquals((byte) -128, signed.getMin());
        assertEquals((byte) 5, signed.getMax());
        assertEquals(-179L, signed.getSum());
        assertEquals(-59.666666666666664d, signed.getAverage());
        assertEquals("{min=-128, max=5, count=3, sum=-179, average=-59.666667}", signed.toString());

        final ByteSummaryStatistics one = new ByteSummaryStatistics();
        one.accept((byte) 200);
        assertEquals(-56L, one.getSum(), "accept((byte) 200) records -56");
        assertEquals((byte) -56, one.getMin());

        // getSum() is a long accumulator: it is NOT confined to -128..127.
        final ByteSummaryStatistics high = new ByteSummaryStatistics();
        for (int i = 0; i < 5; i++) {
            high.accept((byte) 127);
        }
        assertEquals(635L, high.getSum());
        assertEquals(127.0d, high.getAverage());

        final ByteSummaryStatistics low = new ByteSummaryStatistics();
        for (int i = 0; i < 3; i++) {
            low.accept((byte) -128);
        }
        assertEquals(-384L, low.getSum());
        assertEquals(-128.0d, low.getAverage());

        // The documented unsigned workaround: widen with b & 0xFF and accumulate elsewhere.
        final java.util.IntSummaryStatistics unsigned = new java.util.IntSummaryStatistics();
        for (final byte b : new byte[] { (byte) 200, (byte) 5, (byte) 128 }) {
            unsigned.accept(b & 0xFF);
        }
        assertEquals(5, unsigned.getMin());
        assertEquals(200, unsigned.getMax());
        assertEquals(333L, unsigned.getSum());
        assertEquals(111.0d, unsigned.getAverage());
    }

    // Doc pin for the 2026-09-11 narrowing of that same paragraph, which at first claimed getAverage() "ranges
    // over -128..127" unconditionally. The public four-argument constructor validates exactly three things -
    // count >= 0, the canonical empty state, and min <= max - and never cross-checks sum against count/min/max,
    // so a statistic built that way (or combined with one) reports an average bounded by nothing. Deliberately
    // NOT fixed in code: the obvious count * min <= sum <= count * max guard overflows, and would reject the
    // legal state asserted on the last line here.
    @Test
    public void reviewFixes20260911_fourArgConstructorDoesNotCrossCheckSumSoAverageIsUnbounded() {
        // The constructor example shape from the javadoc, with count 5 -> 1: the average escapes [min, max].
        assertEquals(150.0d, new ByteSummaryStatistics(1L, (byte) 10, (byte) 50, 150L).getAverage());
        // min == max does not save it either.
        assertEquals(999.0d, new ByteSummaryStatistics(1L, (byte) 7, (byte) 7, 999L).getAverage());
        assertEquals((double) Long.MAX_VALUE / 2, new ByteSummaryStatistics(2L, (byte) -128, (byte) 127, Long.MAX_VALUE).getAverage());

        // combine() does not restore the bound: an accept()-built receiver inherits the other side's sum.
        final ByteSummaryStatistics accepted = new ByteSummaryStatistics();
        accepted.accept((byte) 1);
        accepted.combine(new ByteSummaryStatistics(1L, (byte) 0, (byte) 0, 5000L));
        assertEquals(2L, accepted.getCount());
        assertEquals(2500.5d, accepted.getAverage());

        // The three validations that DO exist still fire, so the constructor's @throws list stays accurate.
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new ByteSummaryStatistics(-1L, (byte) 0, (byte) 0, 0L));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new ByteSummaryStatistics(0L, (byte) 0, (byte) 0, 0L));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new ByteSummaryStatistics(2L, (byte) 50, (byte) 10, 60L));

        // A legal state (Long.MAX_VALUE values in -1..10 summing to 0) that a count * min bound would wrongly
        // reject, because count * min overflows. This is why the fix is a doc narrowing and not a new check.
        assertEquals(0.0d, new ByteSummaryStatistics(Long.MAX_VALUE, (byte) -1, (byte) 10, 0L).getAverage());
    }
}
