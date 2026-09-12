package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

/**
 * Locks the behaviour changed by cycle 1 of the 2026-09-04 iterative review of {@code Numbers} and {@code Strings}
 * (ledger {@code scripts/cross_review/Numbers_Strings_ledger_2026-09-04.md}):
 * <ul>
 *   <li><b>C-001</b> a {@code Number} subtype whose {@code toString()} returns {@code null}, converted to a target
 *       that is reached through the source's string form ({@code AtomicLong}, {@code AtomicInteger},
 *       {@code Number}, ...), is treated as {@code Double.valueOf(x.doubleValue())} would be -- the fallback the
 *       built-in targets already applied -- instead of yielding {@code null} for a non-{@code null} source and
 *       skipping the unusable-target check.</li>
 * </ul>
 */
public class NumbersRegressionITest extends TestBase {

    /** A {@code Number} whose {@code toString()} is {@code null}, which {@link Object#toString()} permits. */
    private static final class NullTextNumber extends Number {
        private static final long serialVersionUID = 1L;

        private final double value;

        NullTextNumber(final double value) {
            this.value = value;
        }

        @Override
        public int intValue() {
            return (int) value;
        }

        @Override
        public long longValue() {
            return (long) value;
        }

        @Override
        public float floatValue() {
            return (float) value;
        }

        @Override
        public double doubleValue() {
            return value;
        }

        @Override
        public String toString() {
            return null;
        }
    }

    // ==================================================================================================
    // C-001: string-routed targets apply the doubleValue() fallback to a null toString()
    // ==================================================================================================

    @Test
    @DisplayName("C-001: a null-toString source reaching a string-routed target is answered as its doubleValue() would be, never as null")
    public void test_C001_nullToStringSourceIsConvertedAsItsDoubleValue() {
        // 42.0 rendered as a Double is "42.0", which AtomicLong's parser rejects -- exactly what
        // convert(42.0d, AtomicLong.class) does today, so the two can never disagree.
        final NumberFormatException viaNullText = assertThrows(NumberFormatException.class, () -> Numbers.convert(new NullTextNumber(42.0d), AtomicLong.class));
        final NumberFormatException viaDouble = assertThrows(NumberFormatException.class, () -> Numbers.convert(42.0d, AtomicLong.class));
        assertEquals(viaDouble.getMessage(), viaNullText.getMessage());
        assertTrue(viaNullText.getMessage().contains("AtomicLong"), viaNullText.getMessage());

        assertThrows(NumberFormatException.class, () -> Numbers.convert(new NullTextNumber(42.7d), AtomicInteger.class));
        assertThrows(NumberFormatException.class, () -> Numbers.convert(new NullTextNumber(Double.NaN), AtomicLong.class));
        assertThrows(NumberFormatException.class, () -> Numbers.convert(new NullTextNumber(Double.POSITIVE_INFINITY), AtomicLong.class));
        assertThrows(NumberFormatException.class, () -> Numbers.convert(new NullTextNumber(1e20d), AtomicLong.class));
    }

    @Test
    @DisplayName("C-001: an unusable target is still reported as unusable for a null-toString source")
    public void test_C001_unusableTargetStillThrowsForNullToStringSource() {
        final IllegalArgumentException viaNullText = assertThrows(IllegalArgumentException.class,
                () -> Numbers.convert(new NullTextNumber(42.0d), Number.class));
        final IllegalArgumentException viaInt = assertThrows(IllegalArgumentException.class, () -> Numbers.convert(42, Number.class));
        assertEquals(viaInt.getMessage(), viaNullText.getMessage());

        // A null VALUE still takes the target's default without the target being checked (unchanged contract).
        assertNull(Numbers.convert(null, Number.class));
        assertNull(Numbers.convert(null, AtomicLong.class));
    }

    @Test
    @DisplayName("C-001: every convert overload agrees, including the Type and defaultValue forms")
    public void test_C001_allOverloadsAgree() {
        final NullTextNumber source = new NullTextNumber(42.0d);
        final Type<AtomicLong> atomicLongType = CommonUtil.typeOf(AtomicLong.class);

        assertThrows(NumberFormatException.class, () -> Numbers.convert(source, atomicLongType));
        assertThrows(NumberFormatException.class, () -> Numbers.convert(source, AtomicLong.class, new AtomicLong(-1)));
        assertThrows(NumberFormatException.class, () -> Numbers.convert(source, atomicLongType, new AtomicLong(-1)));
        assertThrows(IllegalArgumentException.class, () -> Numbers.convert(source, CommonUtil.typeOf(Number.class)));

        // defaultValue applies to a null value only, as before.
        assertEquals(-1L, Numbers.convert(null, AtomicLong.class, new AtomicLong(-1)).get());
        assertEquals(-1L, Numbers.convert(null, atomicLongType, new AtomicLong(-1)).get());
    }

    @Test
    @DisplayName("C-001: the built-in targets keep their documented doubleValue() fallback for a null toString()")
    public void test_C001_builtInTargetsUnchanged() {
        final NullTextNumber source = new NullTextNumber(42.7d);

        assertEquals((byte) 42, Numbers.convert(source, Byte.class));
        assertEquals((short) 42, Numbers.convert(source, Short.class));
        assertEquals(42, Numbers.convert(source, Integer.class));
        assertEquals(42L, Numbers.convert(source, Long.class));
        assertEquals(BigInteger.valueOf(42), Numbers.convert(source, BigInteger.class));
        assertEquals(BigDecimal.valueOf(42.7d), Numbers.convert(source, BigDecimal.class));
        assertEquals(42.7d, Numbers.convert(source, Double.class));
        assertEquals(42.7f, Numbers.convert(source, Float.class));
        assertEquals(42L, Numbers.toLong(source));
        assertEquals(42, Numbers.toInt(source));

        assertThrows(ArithmeticException.class, () -> Numbers.convert(new NullTextNumber(Double.NaN), Long.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(new NullTextNumber(1e30d), Long.class));
    }

    @Test
    @DisplayName("C-001: an integral doubleValue() that the target's parser accepts converts (Integer/Long sources are the reference)")
    public void test_C001_sourceWithUsableTextStillConverts() {
        // A subtype WITH a string form is untouched: its text is what the parser reads.
        final AtomicLong fromText = Numbers.convert(new AtomicInteger(7), AtomicLong.class);
        assertNotNull(fromText);
        assertEquals(7L, fromText.get());
        assertEquals(42L, Numbers.convert(42, AtomicLong.class).get());
        assertEquals(42, Numbers.convert(42L, AtomicInteger.class).get());
    }
}
