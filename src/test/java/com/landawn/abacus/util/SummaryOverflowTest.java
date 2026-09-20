package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.landawn.abacus.TestBase;

public class SummaryOverflowTest extends TestBase {

    @Test
    void kahanCountOverflowDoesNotChangeTheSummation() {
        final KahanSummation full = new KahanSummation();
        full.combine(Long.MAX_VALUE, 0d);
        assertRejectedUnchanged(() -> snapshot(full), () -> full.add(Double.NaN));
        assertRejectedUnchanged(() -> snapshot(full), () -> full.combine(1, Double.POSITIVE_INFINITY));
        assertRejectedUnchanged(() -> snapshot(full), () -> full.combine(KahanSummation.of(-1d)));
        full.addAll(new double[0]);
        full.combine(new KahanSummation());
        assertEquals(Long.MAX_VALUE, full.count());
        assertEquals(0d, full.sum());

        final KahanSummation almostFull = new KahanSummation();
        almostFull.combine(Long.MAX_VALUE - 1, 0d);
        assertRejectedUnchanged(() -> snapshot(almostFull), () -> almostFull.addAll(new double[] { 7d, 9d }));
        almostFull.addAll(new double[] { 7d });
        assertEquals(Long.MAX_VALUE, almostFull.count());
        assertEquals(7d, almostFull.sum());
        assertThrows(IllegalArgumentException.class, () -> almostFull.addAll(null));
        assertThrows(IllegalArgumentException.class, () -> almostFull.combine(null));
        assertThrows(IllegalArgumentException.class, () -> almostFull.combine(-1, 0d));
        assertEquals(Long.MAX_VALUE, almostFull.count());
        assertEquals(7d, almostFull.sum());
    }

    @Test
    void kahanSelfCombinationAndFiniteOverflowStateArePreserved() {
        final KahanSummation doubled = KahanSummation.of(1d);
        for (int i = 0; i < 62; i++) {
            doubled.combine(doubled);
        }
        assertEquals(1L << 62, doubled.count());
        assertEquals(1d, doubled.average().get());
        assertRejectedUnchanged(() -> snapshot(doubled), () -> doubled.combine(doubled));

        final KahanSummation finite = KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE);
        finite.combine(Long.MAX_VALUE - 2, 0d);
        assertTrue(Double.isFinite(finite.average().get()));
        assertRejectedUnchanged(() -> snapshot(finite), () -> finite.add(Double.NEGATIVE_INFINITY));
        assertTrue(Double.isFinite(finite.average().get()));

        final KahanSummation special = KahanSummation.of(-0d, 0d, Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, special.sum());
        special.add(Double.NEGATIVE_INFINITY);
        assertTrue(Double.isNaN(special.sum()));
        assertEquals(4, special.count());
    }

    @Test
    void byteCountsAndSumsAreCheckedBeforeMutation() {
        final ByteSummaryStatistics full = new ByteSummaryStatistics(Long.MAX_VALUE, (byte) 0, (byte) 0, 0);
        assertRejectedUnchanged(() -> snapshot(full), () -> full.accept((byte) 1));
        final ByteSummaryStatistics one = new ByteSummaryStatistics(1, (byte) 2, (byte) 2, 2);
        assertRejectedUnchanged(() -> snapshot(full), () -> full.combine(one));

        final ByteSummaryStatistics positive = new ByteSummaryStatistics(Long.MAX_VALUE / 2, (byte) 2, (byte) 2, Long.MAX_VALUE - 1);
        assertRejectedUnchanged(() -> snapshot(positive), () -> positive.accept((byte) 2));
        assertRejectedUnchanged(() -> snapshot(positive), () -> positive.combine(one));
        assertRejectedUnchanged(() -> snapshot(positive), () -> positive.combine(positive));
        assertEquals(2d, positive.getAverage());
        positive.accept((byte) 1);
        assertEquals(Long.MAX_VALUE, positive.getSum());

        final ByteSummaryStatistics negative = new ByteSummaryStatistics(Long.MAX_VALUE / 2 + 1, (byte) -2, (byte) -2, Long.MIN_VALUE);
        assertRejectedUnchanged(() -> snapshot(negative), () -> negative.accept((byte) -1));
        assertRejectedUnchanged(() -> snapshot(negative), () -> negative.combine(new ByteSummaryStatistics(1, (byte) -1, (byte) -1, -1)));
        full.combine(new ByteSummaryStatistics());
        assertEquals(Long.MAX_VALUE, full.getCount());
        assertThrows(IllegalArgumentException.class, () -> full.combine(null));
    }

    @Test
    void shortCountsAndSumsAreCheckedBeforeMutation() {
        final ShortSummaryStatistics full = new ShortSummaryStatistics(Long.MAX_VALUE, (short) 0, (short) 0, 0);
        assertRejectedUnchanged(() -> snapshot(full), () -> full.accept((short) 1));
        final ShortSummaryStatistics one = new ShortSummaryStatistics(1, (short) 2, (short) 2, 2);
        assertRejectedUnchanged(() -> snapshot(full), () -> full.combine(one));

        final ShortSummaryStatistics positive = new ShortSummaryStatistics(Long.MAX_VALUE / 2, (short) 2, (short) 2, Long.MAX_VALUE - 1);
        assertRejectedUnchanged(() -> snapshot(positive), () -> positive.accept((short) 2));
        assertRejectedUnchanged(() -> snapshot(positive), () -> positive.combine(one));
        assertRejectedUnchanged(() -> snapshot(positive), () -> positive.combine(positive));
        positive.accept((short) 1);
        assertEquals(Long.MAX_VALUE, positive.getSum());

        final ShortSummaryStatistics negative = new ShortSummaryStatistics(Long.MAX_VALUE / 2 + 1, (short) -2, (short) -2, Long.MIN_VALUE);
        assertRejectedUnchanged(() -> snapshot(negative), () -> negative.accept((short) -1));
        assertRejectedUnchanged(() -> snapshot(negative), () -> negative.combine(new ShortSummaryStatistics(1, (short) -1, (short) -1, -1)));
        full.combine(new ShortSummaryStatistics());
        assertEquals(Long.MAX_VALUE, full.getCount());
        assertThrows(IllegalArgumentException.class, () -> full.combine(null));
    }

    @Test
    void charCountsAndSumsAreCheckedWithoutChangingCodeUnitSemantics() {
        final CharSummaryStatistics full = new CharSummaryStatistics(Long.MAX_VALUE, (char) 0, (char) 0, 0);
        assertRejectedUnchanged(() -> snapshot(full), () -> full.accept('a'));
        final CharSummaryStatistics one = new CharSummaryStatistics(1, (char) 2, (char) 2, 2);
        assertRejectedUnchanged(() -> snapshot(full), () -> full.combine(one));

        final CharSummaryStatistics positive = new CharSummaryStatistics(Long.MAX_VALUE / 2, (char) 2, (char) 2, Long.MAX_VALUE - 1);
        assertRejectedUnchanged(() -> snapshot(positive), () -> positive.accept((char) 2));
        assertRejectedUnchanged(() -> snapshot(positive), () -> positive.combine(one));
        assertRejectedUnchanged(() -> snapshot(positive), () -> positive.combine(positive));
        positive.accept((char) 1);
        assertEquals(Long.MAX_VALUE, positive.getSum());
        full.combine(new CharSummaryStatistics());
        assertEquals(Long.MAX_VALUE, full.getCount());
        assertThrows(IllegalArgumentException.class, () -> full.combine(null));

        final CharSummaryStatistics unicode = new CharSummaryStatistics();
        unicode.accept((char) 0xd83d);
        unicode.accept((char) 0xde00);
        assertEquals(2, unicode.getCount());
        assertEquals(0xd83d + 0xde00, unicode.getSum());
        unicode.combine(unicode);
        assertEquals(4, unicode.getCount());
        assertEquals(2L * (0xd83d + 0xde00), unicode.getSum());
    }

    @Test
    void bigIntegerCountLimitsPreserveAllFields() {
        final BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics(Long.MAX_VALUE - 1, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO);
        stats.accept(BigInteger.ONE);
        assertEquals(Long.MAX_VALUE, stats.getCount());
        assertEquals(BigInteger.ONE, stats.getSum());
        assertRejectedUnchanged(() -> snapshot(stats), () -> stats.accept(BigInteger.TEN));
        assertRejectedUnchanged(() -> snapshot(stats), () -> stats.combine(new BigIntegerSummaryStatistics(1, BigInteger.TEN, BigInteger.TEN, BigInteger.TEN)));
        assertRejectedUnchanged(() -> snapshot(stats), () -> stats.combine(stats));
        stats.combine(new BigIntegerSummaryStatistics());
        assertEquals(BigInteger.ZERO, stats.getMin());
        assertEquals(BigInteger.ONE, stats.getMax());
        assertThrows(IllegalArgumentException.class, () -> stats.accept(null));
        assertThrows(IllegalArgumentException.class, () -> stats.combine(null));

        final BigIntegerSummaryStatistics self = new BigIntegerSummaryStatistics();
        self.accept(BigInteger.TEN);
        self.combine(self);
        assertEquals(2, self.getCount());
        assertEquals(BigInteger.valueOf(20), self.getSum());
    }

    @Test
    void bigDecimalCountAndRepresentationFailuresPreserveAllFields() {
        final BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics(Long.MAX_VALUE - 1, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        stats.accept(BigDecimal.ONE);
        assertEquals(Long.MAX_VALUE, stats.getCount());
        assertEquals(BigDecimal.ONE, stats.getSum());
        assertRejectedUnchanged(() -> snapshot(stats), () -> stats.accept(BigDecimal.TEN));
        assertRejectedUnchanged(() -> snapshot(stats), () -> stats.combine(new BigDecimalSummaryStatistics(1, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN)));
        assertRejectedUnchanged(() -> snapshot(stats), () -> stats.combine(stats));
        stats.combine(new BigDecimalSummaryStatistics());
        assertEquals(BigDecimal.ZERO, stats.getMin());
        assertEquals(BigDecimal.ONE, stats.getMax());
        assertThrows(IllegalArgumentException.class, () -> stats.accept(null));
        assertThrows(IllegalArgumentException.class, () -> stats.combine(null));

        final BigDecimal huge = new BigDecimal(BigInteger.ONE, Integer.MIN_VALUE);
        final BigDecimal tiny = new BigDecimal(BigInteger.ONE, Integer.MAX_VALUE);
        final BigDecimalSummaryStatistics representationLimit = new BigDecimalSummaryStatistics(1, huge, huge, huge);
        assertRejectedUnchanged(() -> snapshot(representationLimit), () -> representationLimit.accept(tiny));
        assertRejectedUnchanged(() -> snapshot(representationLimit), () -> representationLimit.combine(new BigDecimalSummaryStatistics(1, tiny, tiny, tiny)));

        final BigDecimalSummaryStatistics self = new BigDecimalSummaryStatistics();
        self.accept(new BigDecimal("1.25"));
        self.combine(self);
        assertEquals(2, self.getCount());
        assertEquals(new BigDecimal("2.50"), self.getSum());
    }

    @Test
    void floatDelegationRejectsOverflowBeforeChangingExtrema() {
        final FloatSummaryStatistics full = new FloatSummaryStatistics(Long.MAX_VALUE, 1f, 1f, Long.MAX_VALUE);
        assertRejectedUnchanged(() -> snapshot(full), () -> full.accept(Float.NaN));
        assertRejectedUnchanged(() -> snapshot(full), () -> full.combine(new FloatSummaryStatistics(1, -2f, -2f, -2d)));
        assertRejectedUnchanged(() -> snapshot(full), () -> full.combine(full));
        full.combine(new FloatSummaryStatistics());
        assertEquals(Long.MAX_VALUE, full.getCount());
        assertEquals(1f, full.getMin());
        assertEquals(1f, full.getMax());
        assertThrows(IllegalArgumentException.class, () -> full.combine(null));

        final FloatSummaryStatistics zeros = new FloatSummaryStatistics();
        zeros.accept(-0f);
        zeros.accept(0f);
        zeros.combine(zeros);
        assertEquals(4, zeros.getCount());
        assertEquals(Float.floatToRawIntBits(-0f), Float.floatToRawIntBits(zeros.getMin()));
        assertEquals(Float.floatToRawIntBits(0f), Float.floatToRawIntBits(zeros.getMax()));
        zeros.accept(Float.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, zeros.getSum());
        zeros.accept(Float.NaN);
        assertTrue(Float.isNaN(zeros.getMin()));
        assertTrue(Float.isNaN(zeros.getMax()));
        assertTrue(Double.isNaN(zeros.getSum()));
    }

    private static void assertRejectedUnchanged(final Supplier<List<?>> snapshot, final Executable operation) {
        final List<?> before = snapshot.get();
        assertThrows(ArithmeticException.class, operation);
        assertEquals(before, snapshot.get());
    }

    private static List<?> snapshot(final KahanSummation s) {
        return Arrays.asList(s.count(), Double.doubleToRawLongBits(s.sum()), s.average());
    }

    private static List<?> snapshot(final ByteSummaryStatistics s) {
        return Arrays.asList(s.getCount(), s.getSum(), s.getMin(), s.getMax());
    }

    private static List<?> snapshot(final ShortSummaryStatistics s) {
        return Arrays.asList(s.getCount(), s.getSum(), s.getMin(), s.getMax());
    }

    private static List<?> snapshot(final CharSummaryStatistics s) {
        return Arrays.asList(s.getCount(), s.getSum(), s.getMin(), s.getMax());
    }

    private static List<?> snapshot(final BigIntegerSummaryStatistics s) {
        return Arrays.asList(s.getCount(), s.getSum(), s.getMin(), s.getMax());
    }

    private static List<?> snapshot(final BigDecimalSummaryStatistics s) {
        return Arrays.asList(s.getCount(), s.getSum(), s.getMin(), s.getMax());
    }

    private static List<?> snapshot(final FloatSummaryStatistics s) {
        return Arrays.asList(s.getCount(), Double.doubleToRawLongBits(s.getSum()), Float.floatToRawIntBits(s.getMin()), Float.floatToRawIntBits(s.getMax()));
    }
}
