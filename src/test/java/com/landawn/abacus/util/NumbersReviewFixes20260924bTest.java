package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

/**
 * Pins the behaviour changed by cycle 1 of the 2026-09-23 iterative review of {@code Dates}/{@code Numbers}
 * (ledger {@code scripts/cross_review/Dates_Numbers_ledger_2026-09-23.md}), coordinator-owned items:
 * <ul>
 *   <li><b>C-001</b> a {@code BigInteger}/{@code BigDecimal} subclass source is converted as the type it extends
 *       (exact value, same-type identity) instead of falling into the unknown-{@code Number} text recovery;</li>
 *   <li><b>C-005</b> a cause this class built itself is attached as built (the {@code ...[N chars]} marker reports
 *       the input length), and a foreign cause is bounded at the wider cause budget;</li>
 *   <li><b>C-006</b> every {@code BigInteger} magnitude overflow is reported in the class's own overflow idiom;</li>
 *   <li><b>C-008</b> {@code factorialToBigInteger} fails fast with {@code ArithmeticException} for an {@code n}
 *       whose factorial cannot exist;</li>
 *   <li><b>C-018</b> {@code decodeBigInteger} runs the shared integer-token scanner: same values, uniform causes;</li>
 *   <li><b>C-033</b> (documented, pinned here) an integral source wider than a {@code float}/{@code double}
 *       significand is rounded to nearest, never reported.</li>
 * </ul>
 */
public class NumbersReviewFixes20260924bTest extends TestBase {

    private static final BigInteger BI = new BigInteger("12345678901234567890");

    @SuppressWarnings("serial")
    private static final BigInteger BI_SUB = new BigInteger("12345678901234567890") {
        @Override
        public String toString() {
            return "12,345,678,901,234,567,890";
        }
    };

    @SuppressWarnings("serial")
    private static final BigDecimal BD_SUB = new BigDecimal("1.50") {
        @Override
        public String toString() {
            return "$1.50";
        }
    };

    @SuppressWarnings("serial")
    private static final BigDecimal BD_SUB_PLAIN = new BigDecimal("9007199254740993.75") {
    };

    // ----------------------------------------------------------------------------------------------------
    // C-001
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c001_bigIntegerSubclassIsConvertedExactly() {
        assertEquals(BI, Numbers.convert(BI_SUB, BigInteger.class));
        assertSame(BI_SUB, Numbers.convert(BI_SUB, BigInteger.class));
        assertEquals(0, new BigDecimal(BI).compareTo(Numbers.convert(BI_SUB, BigDecimal.class)));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(BI_SUB, Long.class));
        assertEquals(1.2345678901234567E19, Numbers.convert(BI_SUB, Double.class));
    }

    @Test
    public void c001_bigDecimalSubclassIsConvertedExactlyOnEveryOverload() {
        final BigDecimal expected = new BigDecimal("1.50");

        assertSame(BD_SUB, Numbers.convert(BD_SUB, BigDecimal.class));
        assertEquals(expected, Numbers.convert(BD_SUB, BigDecimal.class)); // scale 2 preserved
        assertEquals(BigInteger.ONE, Numbers.convert(BD_SUB, BigInteger.class));
        assertEquals(1L, Numbers.convert(BD_SUB, Long.class));
        assertEquals(1.5f, Numbers.convert(BD_SUB, Float.class));

        assertSame(BD_SUB, Numbers.convert(BD_SUB, BigDecimal.class, BigDecimal.ZERO));
        assertSame(BD_SUB, Numbers.convert(BD_SUB, Type.of(BigDecimal.class)));
        assertSame(BD_SUB, Numbers.convert(BD_SUB, Type.of(BigDecimal.class), BigDecimal.ZERO));
        assertEquals(BigInteger.ONE, Numbers.convert(BD_SUB, Type.of(BigInteger.class)));
    }

    @Test
    public void c001_targetsAgreeForASubclassAboveTwoToTheFiftyThree() {
        // Without the fix the BigInteger/BigDecimal targets read doubleValue() and answered ...994.
        assertEquals(9007199254740993L, Numbers.convert(BD_SUB_PLAIN, Long.class));
        assertEquals(new BigInteger("9007199254740993"), Numbers.convert(BD_SUB_PLAIN, BigInteger.class));
        assertEquals(0, new BigDecimal("9007199254740993.75").compareTo(Numbers.convert(BD_SUB_PLAIN, BigDecimal.class)));
    }

    @Test
    public void c001_exactClassSourcesAreUnchanged() {
        assertSame(BI, Numbers.convert(BI, BigInteger.class));
        assertEquals(new BigDecimal("1.50"), Numbers.convert(new BigDecimal("1.50"), BigDecimal.class));
        assertEquals(Integer.valueOf(12), Numbers.convert(12.9d, Integer.class));
        assertNull(Numbers.convert((Number) null, BigInteger.class));
        assertEquals(BigInteger.TEN, Numbers.convert((Number) null, BigInteger.class, BigInteger.TEN));
    }

    // ----------------------------------------------------------------------------------------------------
    // C-005
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c005_ownCauseKeepsTheInputLengthMarker() {
        final NumberFormatException e = assertThrows(NumberFormatException.class, () -> Numbers.toInt("1".repeat(100) + "x"));
        assertTrue(e.getMessage().endsWith("...[101 chars] is not a valid Integer."), e.getMessage());
        assertNotNull(e.getCause());
        final String cause = e.getCause().getMessage();
        assertTrue(cause.startsWith("invalid character 'x' at index 100 of " + "1".repeat(64) + "...[101 chars]"), cause);

        final NumberFormatException huge = assertThrows(NumberFormatException.class, () -> Numbers.toByte("x".repeat(100_000)));
        assertTrue(huge.getCause().getMessage().endsWith("...[100000 chars]"), huge.getCause().getMessage());
    }

    @Test
    public void c005_ownCauseJustOverTheOldBudgetIsIntact() {
        final NumberFormatException e = assertThrows(NumberFormatException.class, () -> Numbers.toByte("x".repeat(29)));
        assertEquals("invalid character 'x' at index 0 of " + "x".repeat(29), e.getCause().getMessage());

        final NumberFormatException d = assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("1".repeat(100) + "x"));
        assertTrue(d.getCause().getMessage().endsWith("...[101 chars]"), d.getCause().getMessage());

        final NumberFormatException c = assertThrows(NumberFormatException.class, () -> Numbers.createNumber("1".repeat(100) + "x"));
        assertTrue(c.getCause().getMessage().endsWith("...[101 chars]"), c.getCause().getMessage());
    }

    @Test
    public void c005_foreignCauseStaysBoundedAndEscaped() {
        final String token = "9\n" + "9".repeat(1000) + "Z";
        final NumberFormatException e = assertThrows(NumberFormatException.class, () -> Numbers.parseFloat(token));
        final String cause = e.getCause().getMessage();
        assertTrue(cause.length() < 256, () -> "length " + cause.length());
        assertFalse(cause.contains("\n"), cause);
        assertTrue(cause.contains("\\u000A"), cause);
        // Cut once, with the marker reporting the (JDK) message's length, which covers the whole token.
        assertTrue(cause.matches("(?s).*\\.\\.\\.\\[\\d+ chars\\]$"), cause);
        assertTrue(Integer.parseInt(cause.replaceAll("(?s).*\\.\\.\\.\\[(\\d+) chars\\]$", "$1")) >= token.length(), cause);

        // A JDK cause shorter than the budget is retained as it is ("multiple points" on current JDKs).
        final NumberFormatException s = assertThrows(NumberFormatException.class, () -> Numbers.parseDouble("1.2.3"));
        assertNotNull(s.getCause().getMessage());
        assertTrue(s.getCause().getMessage().length() < 192, s.getCause().getMessage());
        assertFalse(s.getCause().getMessage().contains("chars]"), s.getCause().getMessage());
    }

    @Test
    public void c005_ownProductArrivingThroughTheTypeLayerIsNotCutAgain() {
        // The string-routed AtomicInteger target hands the source's text to the Type layer, which calls
        // Numbers.toInt; that failure comes back as the cause and must not be cut a second time.
        final NumberFormatException e = assertThrows(NumberFormatException.class,
                () -> Numbers.convert(new BigDecimal("1." + "5".repeat(200)), AtomicInteger.class));
        final String cause = e.getCause().getMessage();
        assertTrue(cause.endsWith("...[202 chars] is not a valid Integer."), cause);
    }

    // ----------------------------------------------------------------------------------------------------
    // C-006
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c006_everyBigIntegerMagnitudeOverflowUsesTheClassIdiom() {
        for (final String text : new String[] { "1e2147483648", "-1e2147483648", "5e2147483648", "1e2147483647", "1.2e2147483648" }) {
            final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.convert(new BigDecimal(text), BigInteger.class), text);
            assertTrue(e.getMessage().startsWith("BigInteger overflow: "), e.getMessage());
            assertNotNull(e.getCause(), text);
        }

        final Number unknown = new Number() {
            @Override
            public int intValue() {
                return 0;
            }

            @Override
            public long longValue() {
                return 0;
            }

            @Override
            public float floatValue() {
                return Float.POSITIVE_INFINITY;
            }

            @Override
            public double doubleValue() {
                return Double.POSITIVE_INFINITY;
            }

            @Override
            public String toString() {
                return "1e2147483648";
            }
        };
        final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.convert(unknown, BigInteger.class));
        assertTrue(e.getMessage().startsWith("BigInteger overflow: "), e.getMessage());
    }

    @Test
    public void c006_zeroAndSubOneValuesStillTruncateToZero() {
        assertEquals(BigInteger.ZERO, Numbers.convert(new BigDecimal("0e2147483648"), BigInteger.class));
        assertEquals(BigInteger.ZERO, Numbers.convert(new BigDecimal("1e-2147483647"), BigInteger.class));
        assertEquals(BigInteger.ZERO, Numbers.convert(new BigDecimal("-0.999"), BigInteger.class));
        assertEquals(BigInteger.TEN.pow(100), Numbers.convert(new BigDecimal("1e100"), BigInteger.class));
    }

    // ----------------------------------------------------------------------------------------------------
    // C-008
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c008_infeasibleFactorialFailsFastWithArithmeticException() {
        for (final int n : new int[] { 86_181_406, 100_000_000, Integer.MAX_VALUE }) {
            final long start = System.nanoTime();
            final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.factorialToBigInteger(n));
            assertTrue(e.getMessage().startsWith("factorialToBigInteger(" + n + ") overflow"), e.getMessage());
            assertTrue(System.nanoTime() - start < 1_000_000_000L, "fail-fast");
        }

        assertEquals(BigInteger.valueOf(2432902008176640000L), Numbers.factorialToBigInteger(20));
        assertEquals(new BigInteger("51090942171709440000"), Numbers.factorialToBigInteger(21));
        assertEquals(BigInteger.ONE, Numbers.factorialToBigInteger(0));
        assertThrows(IllegalArgumentException.class, () -> Numbers.factorialToBigInteger(-1));
    }

    // ----------------------------------------------------------------------------------------------------
    // C-018
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c018_decodeBigIntegerValuesAreUnchanged() {
        assertEquals(BigInteger.valueOf(255), Numbers.decodeBigInteger("0xFF"));
        assertEquals(BigInteger.valueOf(255), Numbers.decodeBigInteger("#FF"));
        assertEquals(BigInteger.valueOf(-255), Numbers.decodeBigInteger("-0XFF"));
        assertEquals(BigInteger.valueOf(63), Numbers.decodeBigInteger("077"));
        assertEquals(BigInteger.valueOf(8), Numbers.decodeBigInteger("010"));
        assertEquals(BigInteger.valueOf(-1), Numbers.decodeBigInteger("-1"));
        assertEquals(BigInteger.ONE, Numbers.decodeBigInteger("+1"));
        assertEquals(BigInteger.ZERO, Numbers.decodeBigInteger("0"));
        assertEquals(BigInteger.ZERO, Numbers.decodeBigInteger("00"));
        assertEquals(BigInteger.valueOf(Long.MIN_VALUE), Numbers.decodeBigInteger("-9223372036854775808"));
        assertEquals(BigInteger.valueOf(Long.MIN_VALUE), Numbers.decodeBigInteger("-0x8000000000000000"));
        assertEquals(BigInteger.valueOf(Long.MIN_VALUE), Numbers.decodeBigInteger("-01000000000000000000000"));
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE), Numbers.decodeBigInteger("9223372036854775807"));
        assertEquals(new BigInteger("9223372036854775808"), Numbers.decodeBigInteger("9223372036854775808"));
        assertEquals(new BigInteger("8000000000000000", 16), Numbers.decodeBigInteger("0x8000000000000000"));
        assertEquals(new BigInteger("99999999999999999999"), Numbers.decodeBigInteger("99999999999999999999"));
        assertEquals(new BigInteger("-99999999999999999999"), Numbers.decodeBigInteger("-99999999999999999999"));
        assertEquals(new BigInteger("F".repeat(40), 16), Numbers.decodeBigInteger("0x" + "F".repeat(40)));
        assertEquals(new BigInteger("F".repeat(40), 16).negate(), Numbers.decodeBigInteger("-#" + "F".repeat(40)));
        assertEquals(new BigInteger("7".repeat(40), 8), Numbers.decodeBigInteger("0" + "7".repeat(40)));
        assertEquals(new BigInteger("7".repeat(40), 8).negate(), Numbers.decodeBigInteger("-0" + "7".repeat(40)));
        assertNull(Numbers.decodeBigInteger(null));
        assertNull(Numbers.decodeBigInteger(""));
    }

    @Test
    public void c018_decodeBigIntegerRejectionsAreUnchangedAndCarryTheSharedCause() {
        for (final String text : new String[] { "123L", "0xFFL", "+-1", "--1", "0x-1", "0x", "#", " ", " 1", "1 ", "abc", "08", "1-", "١٢٣",
                "1_000", "0x" + "F".repeat(40) + "L", "1\n2" }) {
            final NumberFormatException e = assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger(text), text);
            assertTrue(e.getMessage().endsWith(" is not a valid BigInteger."), e.getMessage());
            assertNotNull(e.getCause(), text);
        }

        assertEquals("invalid character 'L' at index 4 of 0xFFL",
                assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("0xFFL")).getCause().getMessage());
        assertEquals("no digits in integer token 0x",
                assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("0x")).getCause().getMessage());
        assertEquals("invalid character '-' at index 1 of +-1",
                assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("+-1")).getCause().getMessage());
        assertEquals("invalid character '8' at index 1 of 08",
                assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("08")).getCause().getMessage());
        // The same token gives the same cause on the sibling decoder.
        assertEquals(assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("+-1")).getCause().getMessage(),
                assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("+-1")).getCause().getMessage());
    }

    // ----------------------------------------------------------------------------------------------------
    // C-077 (cycle 2): the overflow fallback reuses the scan's sign/radix/digit start
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c077_decodeBigIntegerOverflowFallbackUsesTheScannedShape() {
        assertEquals(new BigInteger("99999999999999999999"), Numbers.decodeBigInteger("+99999999999999999999"));
        assertEquals(new BigInteger("-99999999999999999999"), Numbers.decodeBigInteger("-99999999999999999999"));
        assertEquals(new BigInteger("8000000000000000", 16), Numbers.decodeBigInteger("0x8000000000000000"));
        assertEquals(new BigInteger("-8000000000000001", 16), Numbers.decodeBigInteger("-0x8000000000000001"));
        assertEquals(new BigInteger("FFFFFFFFFFFFFFFFFF", 16), Numbers.decodeBigInteger("#FFFFFFFFFFFFFFFFFF"));
        assertEquals(new BigInteger("-FFFFFFFFFFFFFFFFFF", 16), Numbers.decodeBigInteger("-#FFFFFFFFFFFFFFFFFF"));
        assertEquals(new BigInteger("1000000000000000000000", 8), Numbers.decodeBigInteger("01000000000000000000000"));
        assertEquals(new BigInteger("-1000000000000000000001", 8), Numbers.decodeBigInteger("-01000000000000000000001"));
        assertEquals(new BigInteger("7".repeat(40), 8).negate(), Numbers.decodeBigInteger("-0" + "7".repeat(40)));
        assertEquals(BigInteger.valueOf(Long.MIN_VALUE), Numbers.decodeBigInteger("-0x0000000000000008000000000000000"));
        assertEquals(BigInteger.ONE, Numbers.decodeBigInteger("0000000000000000000000000000001"));
        // Malformed and in-range tokens are unaffected by the fallback.
        assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("0x" + "F".repeat(40) + "L"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("-0" + "7".repeat(40) + "8"));
        assertEquals(BigInteger.valueOf(255), Numbers.decodeBigInteger("-#-FF".replace("-#-", "#")));
    }

    // ----------------------------------------------------------------------------------------------------
    // C-033
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c033_integralSourceWiderThanTheSignificandRoundsToNearest() {
        assertEquals(1.6777216E7f, Numbers.convert(16777217, Float.class));
        assertEquals(1.6777216E7f, Numbers.convert(16777217L, Float.class));
        assertEquals(9.223372036854776E18, Numbers.convert(Long.MAX_VALUE, Double.class));
        assertEquals(9.007199254740992E15, Numbers.convert(9007199254740993L, Double.class));
    }
}
