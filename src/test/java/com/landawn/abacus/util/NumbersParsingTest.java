/*
 * Copyright (C) 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NumbersParsingTest extends NumbersTestSupport {

    private static void sweepScannerAgreement(final String alphabet, final int maxLen) {
        final char[] buf = new char[maxLen];

        for (int len = 1; len <= maxLen; len++) {
            int total = 1;

            for (int i = 0; i < len; i++) {
                total *= alphabet.length();
            }

            for (int code = 0; code < total; code++) {
                int c = code;

                for (int i = 0; i < len; i++) {
                    buf[i] = alphabet.charAt(c % alphabet.length());
                    c /= alphabet.length();
                }

                exerciseScanners(new String(buf, 0, len));
            }
        }
    }

    /** Runs every entry point that carries an {@code AssertionError} guard. Only that error escapes. */
    private static void exerciseScanners(final String token) {
        try {
            Numbers.toByte(token);
        } catch (final NumberFormatException | ArithmeticException expected) {
            // Either outcome is fine; only an AssertionError is a failure.
        }

        try {
            Numbers.toShort(token);
        } catch (final NumberFormatException | ArithmeticException expected) {
            // Either outcome is fine.
        }

        try {
            Numbers.toInt(token);
        } catch (final NumberFormatException | ArithmeticException expected) {
            // Either outcome is fine.
        }

        try {
            Numbers.toLong(token);
        } catch (final NumberFormatException | ArithmeticException expected) {
            // Either outcome is fine.
        }

        try {
            Numbers.decodeInteger(token);
        } catch (final NumberFormatException expected) {
            // Either outcome is fine.
        }

        try {
            Numbers.decodeLong(token);
        } catch (final NumberFormatException expected) {
            // Either outcome is fine.
        }
    }

    private static void assertCreated(final Number expected, final String token) {
        final Number actual = Numbers.createNumber(token);
        assertEquals(expected.getClass(), actual.getClass(), token);
        assertEquals(expected, actual, token); // Float/Double equals distinguishes -0.0 from 0.0; BigDecimal equals includes scale
        assertTrue(Numbers.isCreatable(token), token);
        assertEquals(expected, Numbers.tryCreateNumber(token).get(), token);
    }


    /**
     * Independent spelling of the sign + radix-prefix grammar: an optional sign, then {@code 0x}/{@code 0X}/{@code #}
     * for radix 16, a leading zero followed by another character for radix 8 when {@code octal} is set, ASCII
     * digits of that radix, and, when {@code stripLongSuffix} is set, an optional trailing {@code L}/{@code l}.
     * Returns {@code null} for a malformed token.
     */
    private static BigInteger referenceIntegerToken(final String s, final boolean octal, final boolean stripLongSuffix) {
        int end = s.length();

        if (stripLongSuffix && end > 1 && (s.charAt(end - 1) == 'L' || s.charAt(end - 1) == 'l')) {
            end--;
        }

        if (end == 0) {
            return null;
        }

        int pos = 0;
        boolean negate = false;

        if (s.charAt(0) == '-') {
            negate = true;
            pos++;
        } else if (s.charAt(0) == '+') {
            pos++;
        }

        int radix = 10;

        if (pos + 1 < end && s.charAt(pos) == '0' && (s.charAt(pos + 1) == 'x' || s.charAt(pos + 1) == 'X')) {
            radix = 16;
            pos += 2;
        } else if (pos < end && s.charAt(pos) == '#') {
            radix = 16;
            pos++;
        } else if (octal && pos + 1 < end && s.charAt(pos) == '0') {
            radix = 8;
            pos++;
        }

        if (pos >= end) {
            return null;
        }

        for (int i = pos; i < end; i++) {
            final char ch = s.charAt(i);

            if (ch > 127 || Character.digit(ch, radix) < 0) {
                return null;
            }
        }

        final BigInteger magnitude = new BigInteger(s.substring(pos, end), radix);
        return negate ? magnitude.negate() : magnitude;
    }

    private static void assertScannersAgree(final String token) {
        final BigInteger decode = referenceIntegerToken(token, true, false); // decodeInteger, decodeBigInteger
        final BigInteger decodeWithSuffix = referenceIntegerToken(token, true, true); // decodeLong
        final BigInteger decimalFirst = referenceIntegerToken(token, false, true); // toInt, toLong, tryParseInt, tryParseLong

        if (decode == null) {
            assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger(token), token);
            assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger(token), token);
        } else {
            assertEquals(decode, Numbers.decodeBigInteger(token), token);

            if (decode.bitLength() > 31) {
                assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger(token), token);
            } else {
                assertEquals(decode.intValue(), Numbers.decodeInteger(token).intValue(), token);
            }
        }

        if (decodeWithSuffix == null || decodeWithSuffix.bitLength() > 63) {
            assertThrows(NumberFormatException.class, () -> Numbers.decodeLong(token), token);
        } else {
            assertEquals(decodeWithSuffix.longValue(), Numbers.decodeLong(token).longValue(), token);
        }

        if (decimalFirst == null) {
            assertThrows(NumberFormatException.class, () -> Numbers.toInt(token), token);
            assertThrows(NumberFormatException.class, () -> Numbers.toLong(token), token);
            assertFalse(Numbers.tryParseInt(token).isPresent(), token);
            assertFalse(Numbers.tryParseLong(token).isPresent(), token);
        } else {
            if (decimalFirst.bitLength() > 31) {
                assertThrows(ArithmeticException.class, () -> Numbers.toInt(token), token);
                assertFalse(Numbers.tryParseInt(token).isPresent(), token);
            } else {
                assertEquals(decimalFirst.intValue(), Numbers.toInt(token), token);
                assertEquals(decimalFirst.intValue(), Numbers.tryParseInt(token).getAsInt(), token);
            }

            if (decimalFirst.bitLength() > 63) {
                assertThrows(ArithmeticException.class, () -> Numbers.toLong(token), token);
                assertFalse(Numbers.tryParseLong(token).isPresent(), token);
            } else {
                assertEquals(decimalFirst.longValue(), Numbers.toLong(token), token);
                assertEquals(decimalFirst.longValue(), Numbers.tryParseLong(token).getAsLong(), token);
            }
        }
    }

    // createNumber

    @Test
    public void testCreateNumber() {
        assertEquals(Integer.valueOf(123), Numbers.createNumber("123"));
        assertEquals(Long.valueOf(12345678901L), Numbers.createNumber("12345678901L"));
        assertEquals(Float.valueOf(1.23f), Numbers.createNumber("1.23f"));
        assertEquals(Double.valueOf(1.23d), Numbers.createNumber("1.23d"));
        assertEquals(Double.valueOf(1.23e4), Numbers.createNumber("1.23e4"));
        assertEquals(Integer.valueOf(0xFF), Numbers.createNumber("0xFF"));
        assertEquals(Integer.valueOf(077), Numbers.createNumber("077"));
        assertEquals(new BigInteger("1234567890123456789012345"), Numbers.createNumber("1234567890123456789012345"));
        assertNull(Numbers.createNumber(null));
        assertNull(Numbers.createNumber(""));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("abc"));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("-"));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("1.2.3"));
    }

    @Test
    public void testCreateNumber_FailureCause() {
        assertEquals("invalid character 'a' at index 0 of abc", causeMessageOfCreateNumber("abc"));
        assertEquals("invalid character '.' at index 3 of 1.2.3", causeMessageOfCreateNumber("1.2.3"));
        assertEquals("invalid character '9' at index 1 of 09", causeMessageOfCreateNumber("09"));
        assertEquals("invalid character 'L' at index 4 of 0xFFL", causeMessageOfCreateNumber("0xFFL"));
        assertEquals("no digits in number token 1e", causeMessageOfCreateNumber("1e"));
        assertEquals("no digits in number token 0x", causeMessageOfCreateNumber("0x"));
        assertEquals("effective BigDecimal scale is out of range in 1e2147483649", causeMessageOfCreateNumber("1e2147483649"));
        assertEquals("invalid character 'N' at index 0 of NaN", causeMessageOfCreateNumber("NaN"));
        assertEquals("invalid character '\\u0007' at index 1 of 1\\u0007", causeMessageOfCreateNumber("1\u0007"));
        final NumberFormatException huge = assertThrows(NumberFormatException.class, () -> Numbers.createNumber("1" + "z".repeat(500_000)));
        assertTrue(huge.getMessage().length() < 160, huge.getMessage());
        assertNotNull(huge.getCause());
    }

    @Test
    public void testCreateNumber_FloatingType() {
        assertTrue(Numbers.createNumber("1.0000000000000000000000001") instanceof Double);
        assertEquals(1.0, Numbers.createNumber("1.0000000000000000000000001").doubleValue(), 0.0);
        assertEquals(Double.class, Numbers.createNumber("1e-50f").getClass());
        assertEquals(Double.class, Numbers.createNumber("1e40f").getClass());

        final Number typed = Numbers.createNumber("-1e-400D");
        assertTrue(typed instanceof BigDecimal);
        assertEquals(0, ((BigDecimal) typed).compareTo(new BigDecimal("-1e-400")));
        assertEquals(Double.valueOf(-1e-60), Numbers.createNumber("-1e-60F"));
        final Number untyped = Numbers.createNumber("1e-400");
        assertTrue(untyped instanceof BigDecimal);
        assertEquals(Double.valueOf(0.0d), Numbers.createNumber("0.0"));
        assertEquals(Float.valueOf(0), Numbers.createNumber("0F"));
        assertEquals(Double.valueOf(0), Numbers.createNumber("0D"));
        assertEquals(Float.valueOf(0.0f), Numbers.createNumber("0.0e-2000F"));
    }

    @Test
    public void testCreateNumber_Hex() {
        assertEquals(Integer.valueOf(255), Numbers.createNumber("0xFF"));
        assertEquals(Integer.valueOf(255), Numbers.createNumber("+0XFF"));
        assertEquals(Integer.valueOf(255), Numbers.createNumber("#FF"));
        assertEquals(Integer.valueOf(-255), Numbers.createNumber("-#FF"));
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), Numbers.createNumber("0x7FFFFFFF"));
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), Numbers.createNumber("-0X80000000"));
        assertEquals(Long.valueOf(2147483648L), Numbers.createNumber("0x80000000"));
        assertEquals(new BigInteger("8000000000000000", 16), Numbers.createNumber("#8000000000000000"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.createNumber("-0x8000000000000000"));
        assertEquals(Integer.decode("+0xF"), Numbers.createNumber("+0xF"));

        assertEquals(Long.valueOf(255L), Numbers.decodeLong("0xFFL"));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("0xFFL"));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("#FFL"));
        assertFalse(Numbers.isCreatable("0xFFL"));
        assertEquals(255L, Numbers.toLong("0xFFL"));
    }

    @Test
    public void testCreateNumber_LongTokenValue() {
        final String padded = "0".repeat(400) + "1.5";
        assertEquals(Double.class, Numbers.createNumber(padded).getClass());
        assertEquals(Double.valueOf(1.5d), Numbers.createNumber(padded));
        assertEquals(BigDecimal.class, Numbers.createNumber("1.5e400").getClass());
        assertEquals(BigDecimal.class, Numbers.createNumber("1.5e-400").getClass());
        assertEquals(Double.valueOf(0.0d), Numbers.createNumber("0" + "0".repeat(400) + ".0"));

        final String overLimit = "1." + "0".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH - 2) + "1";
        assertFalse(Numbers.isParsable(overLimit));
        assertThrows(NumberFormatException.class, () -> Numbers.parseDouble(overLimit));
        assertNotNull(Numbers.createNumber(overLimit));
        assertNotNull(Numbers.parseBigDecimal(overLimit));
    }

    @Test
    public void testCreateNumber_TypeByRange() {
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), Numbers.createNumber("2147483647"));
        assertEquals(Long.valueOf(2147483648L), Numbers.createNumber("2147483648"));
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), Numbers.createNumber("-2147483648"));
        assertEquals(Long.valueOf(Long.MAX_VALUE), Numbers.createNumber("9223372036854775807"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.createNumber("-9223372036854775808"));
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), Numbers.createNumber("9223372036854775808"));
        assertEquals(Integer.valueOf(83), Numbers.createNumber("0123"));
        assertEquals(Integer.valueOf(123), Numbers.createNumber("+123"));
        assertEquals(Long.class, Numbers.createNumber("7L").getClass());
        assertEquals(Integer.class, Numbers.createNumber("7").getClass());
        assertEquals(Long.valueOf(123L), Numbers.createNumber("123L"));
        assertEquals(Long.valueOf(83L), Numbers.createNumber("0123L"));
        assertEquals(Long.valueOf(-7L), Numbers.createNumber("-7l"));
        assertEquals(Long.valueOf(7L), Numbers.createNumber("+7L"));
        assertEquals(new BigInteger("9999999999999999999999"), Numbers.createNumber("9999999999999999999999L"));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("09L"));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("1.5L"));

        final Random random = new Random(0xC0FFEE_1234L);
        for (int i = 0; i < 2048; i++) {
            final long value = random.nextLong();
            final BigInteger magnitude = BigInteger.valueOf(value);
            final Number boxed = (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) ? Integer.valueOf((int) value) : Long.valueOf(value);
            assertEquals(boxed, Numbers.createNumber(magnitude.toString(10)), "decimal " + value);
            assertEquals(boxed, Numbers.createNumber(signedRadixToken(magnitude, 16, "0x", (i & 1) == 0)), "hex " + value);
            assertEquals(boxed, Numbers.createNumber(signedRadixToken(magnitude, 8, "0", (i & 1) == 0)), "octal " + value);
        }
    }

    @Test
    public void test_createNumber_typeIsUnaffectedByLengthOrSuffix() {
        // The claim the sentence is actually about: a token sitting either side of
        // MAX_FLOATING_POINT_TOKEN_LENGTH gets the same type with and without a trailing 'd' -- and, since
        // 2026-09-02, the same type on both sides too, because the create family no longer looks at length.
        final String justUnder = "1." + "0".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH - 2);
        final String justOver = "1." + "0".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH - 1);
        assertEquals(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH, justUnder.length());
        assertEquals(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH + 1, justOver.length());

        assertInstanceOf(Double.class, Numbers.createNumber(justUnder));
        assertInstanceOf(Double.class, Numbers.createNumber(justUnder + "d"));
        assertInstanceOf(Double.class, Numbers.createNumber(justOver));
        assertInstanceOf(Double.class, Numbers.createNumber(justOver + "d"));
    }

    // J1: a trailing type suffix changes the GRAMMAR, not just the width. The old sentence claimed
    // "createNumber(s) and createNumber(s + \"d\") agree on the result type for every s".
    @Test
    public void test_createNumber_typeSuffixAgreesOnlyForAnUnsuffixedFloatingToken() {
        // The claim as corrected: it holds for a non-hexadecimal token carrying a decimal point or an
        // exponent and no type suffix of its own, because the length routing measures the payload
        // without the suffix.
        for (final String s : new String[] { "1.5", "1e5", "1E5", ".5", "1.", "0.9", "01e1", "1.5e3", "-1.5", "+1.5", "0.0", "-0.0",
                "1.0000000000000000000000001", "1e400", "-1e400", "1e-400", "0e999999999" }) {
            assertEquals(Numbers.createNumber(s).getClass(), Numbers.createNumber(s + "d").getClass(), s);
        }
    }

    // D2: the create-family type-suffix detection uses an ASCII digit test, like the rest of the class.
    @Test
    public void test_createNumber_typeSuffixDetectionIsAsciiOnly() {
        // A non-ASCII digit is rejected by the grammar, so it can never reach the suffix test.
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("12٣")); // Arabic-Indic 3
        assertFalse(Numbers.isCreatable("12٣"));

        // and the ASCII cases the test actually governs still route correctly
        assertInstanceOf(Integer.class, Numbers.createNumber("123"));
        assertInstanceOf(Double.class, Numbers.createNumber("123."));
        assertInstanceOf(Double.class, Numbers.createNumber("1.5"));
        assertInstanceOf(Float.class, Numbers.createNumber("1.5f"));
        assertInstanceOf(Double.class, Numbers.createNumber("1.5d"));
        assertInstanceOf(Long.class, Numbers.createNumber("123L"));
    }

    @Test
    public void test_createNumber_typeSuffixIsAHexDigitOnTheHexadecimalPath() {
        // d/D and f/F are hexadecimal digits, so on the hex path the "suffix" is consumed as magnitude.
        assertEquals(16, Numbers.createNumber("0x10").intValue());
        assertEquals(269, Numbers.createNumber("0x10d").intValue()); // 0x10d, not 0x10 as a double
        assertEquals(16, Numbers.createNumber("#10").intValue());
        assertEquals(269, Numbers.createNumber("#10d").intValue());
        assertEquals(255, Numbers.createNumber("0xFF").intValue());
        assertEquals(4093, Numbers.createNumber("0xFFd").intValue());

        // ... while an 'l'/'L' suffix is NOT a hex digit and is still rejected on that path.
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("0xFFL"));
    }

    @Test
    public void test_createNumber_typeSuffixMovesAnIntegralTokenOntoTheDecimalPath() {
        // An integral token: the suffix changes both the type and, for a leading-zero token, the radix.
        assertInstanceOf(Integer.class, Numbers.createNumber("123"));
        assertInstanceOf(Double.class, Numbers.createNumber("123d"));
        assertEquals(123, Numbers.createNumber("123").intValue());
        assertEquals(123.0d, Numbers.createNumber("123d"));

        // "010" is octal 8 unsuffixed, but the suffix selects the decimal path, so it becomes 10.0.
        assertEquals(8, Numbers.createNumber("010").intValue());
        assertEquals(10.0d, Numbers.createNumber("010d"));
        assertEquals(83, Numbers.createNumber("0123").intValue());
        assertEquals(123.0d, Numbers.createNumber("0123d"));
    }

    // J1/J2/J3 - behaviour pinned by the corrected Javadoc.
    @Test
    @DisplayName("J1: createNumber picks the type from the value, never from the length of the literal")
    public void test_J1_createNumberTypeFollowsTheValueNotTheLiteralLength() {
        // The Javadoc used to claim this returned a BigDecimal via a "longer than 308 characters" shortcut.
        // That shortcut no longer exists: the value is an ordinary double, so the result is a Double.
        assertEquals(Double.valueOf(1.0d), Numbers.createNumber("1" + "0".repeat(310) + "e-310"));
        assertEquals(Double.valueOf(1.5d), Numbers.createNumber("0".repeat(400) + "1.5"));
        assertEquals(Double.valueOf(1.0d), Numbers.createNumber("1." + "0".repeat(400) + "5"));

        // A suffix never changes the chosen type for the same numeric payload.
        for (final String literal : new String[] { "1" + "0".repeat(310) + "e-310", "0".repeat(400) + "1.5", "1." + "0".repeat(400) + "5" }) {
            assertSame(Numbers.createNumber(literal).getClass(), Numbers.createNumber(literal + "d").getClass(), literal);
        }

        // A value that genuinely does not fit a double still escalates, at any length.
        assertEquals(BigDecimal.class, Numbers.createNumber("1e400").getClass());
        assertEquals(BigDecimal.class, Numbers.createNumber("0." + "0".repeat(400) + "1").getClass());

        // The create family applies no length limit at all (2026-09-02): a payload past
        // MAX_FLOATING_POINT_TOKEN_LENGTH is parsed like any other and the value selects the type.
        final String overLongPayload = "1." + "3".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH);
        assertEquals(Double.valueOf(1.3333333333333333d), Numbers.createNumber(overLongPayload));
        assertEquals(Double.class, Numbers.createNumber("1." + "3".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH - 2)).getClass());
    }

    @Test
    public void c022_createNumberZeroSignificandDetection() {
        // Float results
        assertCreated(Float.valueOf(0.0f), "0.0e-99999999999f");
        assertCreated(Float.valueOf(0.0f), "0e400f");
        assertCreated(Float.valueOf(-0.0f), "-0.0f");
        assertCreated(Float.valueOf(0.5f), ".5f");
        assertCreated(Float.valueOf(1.0f), "1.f");
        assertCreated(Float.valueOf(1.0f), "1.0f");
        assertCreated(Float.valueOf(1.0f), "1.0F");
        assertCreated(Float.valueOf(1e-40f), "1e-40f"); // subnormal float, not zero

        // Float underflow/overflow with a non-zero significand falls through to Double
        assertCreated(Double.valueOf(1.0E-50), "1e-50f");
        assertCreated(Double.valueOf(-1.0E-50), "-1e-50f");
        assertCreated(Double.valueOf(1.0E40), "1e40f");

        // Double results
        assertCreated(Double.valueOf(123.0), "123d");
        assertCreated(Double.valueOf(1.0), "1.0D");
        assertCreated(Double.valueOf(100000.0), "1e5D");
        assertCreated(Double.valueOf(0.0), "0.000");
        assertCreated(Double.valueOf(-0.0), "-0.0");
        assertCreated(Double.valueOf(-0.0), "-0e5");
        assertCreated(Double.valueOf(-0.0), "-.0e-3");
        assertCreated(Double.valueOf(0.0), "00.0e1");
        assertCreated(Double.valueOf(0.0), "0.0");
        assertCreated(Double.valueOf(0.0), ".0");
        assertCreated(Double.valueOf(0.0), "0.");
        assertCreated(Double.valueOf(0.0), "0E0");
        assertCreated(Double.valueOf(0.0), "0e-2147483647");
        assertCreated(Double.valueOf(0.0), "0e2147483648");
        assertCreated(Double.valueOf(-0.0), "-0e-99999999999");
        assertCreated(Double.valueOf(1500.0), "1.5e3");
        assertCreated(Double.valueOf(100000.0), "1E5");
        assertCreated(Double.valueOf(4.9E-324), "4.9e-324");
        assertCreated(Double.valueOf(4.9E-324), "3e-324"); // rounds up to Double.MIN_VALUE, not zero
        assertCreated(Double.valueOf(1.7976931348623157E308), "1.7976931348623157e308");

        // Double underflow/overflow with a non-zero significand becomes BigDecimal
        assertCreated(new BigDecimal("1e400"), "1e400f");
        assertCreated(new BigDecimal("1e400"), "1e400d");
        assertCreated(new BigDecimal("1e-400"), "1e-400d");
        assertCreated(new BigDecimal("1e309"), "1e309");
        assertCreated(new BigDecimal("1e-330"), "1e-330");
        assertCreated(new BigDecimal("1e-324"), "1e-324");
        assertCreated(new BigDecimal("0.0000000001e-400"), "0.0000000001e-400");

        // integral tokens are untouched, including hexadecimal ones that happen to contain an 'E'
        assertCreated(Integer.valueOf(123), "123");
        assertCreated(Long.valueOf(123), "123L");
        assertCreated(Long.valueOf(83), "0123L");
        assertCreated(Long.valueOf(1234567890123L), "1234567890123");
        assertCreated(new BigInteger("12345678901234567890"), "12345678901234567890");
        assertCreated(Integer.valueOf(0x1E5), "0x1E5");
        assertCreated(Integer.valueOf(-0x1e5), "-0x1e5");

        // a non-zero significand with an unrepresentable scale is still rejected
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("1e-99999999999"));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("1e99999999999"));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("1e"));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("1e+"));
        assertTrue(Numbers.tryCreateNumber("1e99999999999").isEmpty());
        assertEquals(Double.valueOf(0.0), Numbers.tryCreateNumber("0e99999999999").get());
    }

    @Test
    public void c116_invalidCharacterCauseNamesTheWholeCodePoint() {
        NumberFormatException e = assertThrows(NumberFormatException.class, () -> Numbers.createNumber("\uD83D\uDE001"));
        assertEquals("\uD83D\uDE001 is not a valid Number.", e.getMessage());
        assertEquals("invalid character '\uD83D\uDE00' at index 0 of \uD83D\uDE001", e.getCause().getMessage());

        e = assertThrows(NumberFormatException.class, () -> Numbers.toInt("\uD83D\uDE001"));
        assertEquals("\uD83D\uDE001 is not a valid Integer.", e.getMessage());
        assertEquals("invalid character '\uD83D\uDE00' at index 0 of \uD83D\uDE001", e.getCause().getMessage());

        e = assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\uD83D\uDE00"));
        assertEquals("invalid character '\uD83D\uDE00' at index 1 of 1\uD83D\uDE00", e.getCause().getMessage());
        assertEquals("invalid character '\uD83D\uDE00' at index 0 of \uD83D\uDE00",
                assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("\uD83D\uDE00")).getCause().getMessage());
        assertEquals("invalid character '\uD83D\uDE00' at index 2 of 12\uD83D\uDE00",
                assertThrows(NumberFormatException.class, () -> Numbers.createNumber("12\uD83D\uDE00")).getCause().getMessage());

        // a lone surrogate is still escaped
        e = assertThrows(NumberFormatException.class, () -> Numbers.createNumber("\uD83D1"));
        assertEquals("\\uD83D1 is not a valid Number.", e.getMessage());
        assertEquals("invalid character '\\uD83D' at index 0 of \\uD83D1", e.getCause().getMessage());
        e = assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\uDE00"));
        assertEquals("invalid character '\\uDE00' at index 1 of 1\\uDE00", e.getCause().getMessage());

        // an ordinary invalid character is unchanged
        assertEquals("invalid character 'x' at index 2 of 12x", assertThrows(NumberFormatException.class, () -> Numbers.toByte("12x")).getCause().getMessage());
        assertEquals("invalid character 'x' at index 2 of 12x",
                assertThrows(NumberFormatException.class, () -> Numbers.createNumber("12x")).getCause().getMessage());
    }

    // D1: the create family has no length limit; the value alone selects the type
    @Test
    @DisplayName("D1: a literal equal to 1.0 is a Double at 4096, 4097 and 40000 characters alike")
    public void test_D1_lengthNeverFlipsTheType() {
        for (final int zeros : new int[] { 10, 4093, 4094, 4095, 4096, 5000, 40_000 }) {
            final String s = "1." + "0".repeat(zeros);
            assertEquals(Double.valueOf(1.0d), Numbers.createNumber(s), "zeros=" + zeros);
            assertEquals(Double.valueOf(1.0d), Numbers.createNumber(s + "d"), "zeros=" + zeros);
            assertEquals(Double.valueOf(1.0d), Numbers.createNumber(s + "D"), "zeros=" + zeros);
            assertEquals(Float.valueOf(1.0f), Numbers.createNumber(s + "f"), "zeros=" + zeros);
            assertEquals(Float.valueOf(1.0f), Numbers.createNumber(s + "F"), "zeros=" + zeros);
            assertEquals(Double.valueOf(-1.0d), Numbers.createNumber("-" + s), "zeros=" + zeros);
            assertEquals(Double.valueOf(1.0d), Numbers.tryCreateNumber(s).orElseThrow(), "zeros=" + zeros);
            assertTrue(Numbers.isCreatable(s), "zeros=" + zeros);
        }

        // The digits past double precision are folded into the rounding, exactly as for a short token.
        final String third = "1." + "3".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH + 100);
        assertEquals(Double.valueOf(1.3333333333333333d), Numbers.createNumber(third));
        assertEquals(Float.valueOf(1.3333334f), Numbers.createNumber(third + "f"));
        assertEquals(Double.valueOf(Double.parseDouble("1." + "3".repeat(50))), Numbers.createNumber(third));
    }

    // J1 / J2: createNumber selects the type from the value alone, at any length. (The payload-length
    // routing these tests once pinned was removed on 2026-09-02: it flipped the type of a literal equal
    // to 1.0 at an arbitrary character count, and the JDK parser is linear in the token length.)
    @Test
    @DisplayName("J1: the value selects the type on both sides of MAX_FLOATING_POINT_TOKEN_LENGTH")
    public void test_J1_lengthNeverSelectsTheType() {
        // The value here is exactly 1.0 at every length, so the type must not change with the length.
        assertSame(Double.class, Numbers.createNumber("1." + "0".repeat(4094)).getClass());
        assertSame(Double.class, Numbers.createNumber("1." + "0".repeat(4095)).getClass());
        assertEquals(Double.valueOf(1.0d), Numbers.createNumber("1." + "0".repeat(4095)));

        // Nor do leading zeros or the exponent.
        assertEquals(Double.valueOf(1.0d), Numbers.createNumber("1" + "0".repeat(310) + "e-310"));
        assertSame(Double.class, Numbers.createNumber("0".repeat(400) + "1.5").getClass());
    }

    @Test
    @DisplayName("D1: a lexical zero of any length is the correctly signed binary zero, with or without a suffix")
    public void test_D1_overLongZeroKeepsSignAndType() {
        final String zero = "0." + "0".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH + 10);
        assertEquals(Double.valueOf(0.0d), Numbers.createNumber(zero));
        assertEquals(Double.valueOf(-0.0d), Numbers.createNumber("-" + zero));
        assertEquals(Float.valueOf(0.0f), Numbers.createNumber(zero + "f"));
        assertEquals(Float.valueOf(-0.0f), Numbers.createNumber("-" + zero + "f"));
        assertEquals(Double.valueOf(-0.0d), Numbers.createNumber("-" + zero + "d"));

        // A zero significand with an exponent too large for any BigDecimal is still a plain zero.
        final String hugeExponentZero = "0e" + "9".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH + 10);
        assertTrue(Numbers.isCreatable(hugeExponentZero));
        assertEquals(Double.valueOf(0.0d), Numbers.createNumber(hugeExponentZero));
        assertEquals(Double.valueOf(-0.0d), Numbers.createNumber("-" + hugeExponentZero));
        assertEquals(Float.valueOf(-0.0f), Numbers.createNumber("-" + hugeExponentZero + "f"));
        assertEquals(Double.valueOf(0.0d), Numbers.tryCreateNumber(hugeExponentZero).orElseThrow());
    }

    @Test
    @DisplayName("D1: createNumber(s) and createNumber(s + 'd') agree on the type for every floating shape, at every length")
    public void test_D1_suffixNeverChangesTheTypeOfAFloatingToken() {
        final List<String> shapes = new ArrayList<>();

        for (final int n : new int[] { 5, 308, 309, 4095, 4096, 4097, 8000 }) {
            shapes.add("1" + "0".repeat(n - 1) + "e-400");
            shapes.add("1" + "0".repeat(n - 1) + "e-1");
            shapes.add("1." + "0".repeat(n - 2));
            shapes.add("0." + "0".repeat(n - 2) + "1");
            shapes.add("0".repeat(n) + "1.5");
            shapes.add("1" + "0".repeat(n - 1) + ".5");
        }

        for (final String s : shapes) {
            assertSame(Numbers.createNumber(s).getClass(), Numbers.createNumber(s + "d").getClass(), "len=" + s.length() + " head=" + s.substring(0, 5));
            assertEquals(Numbers.createNumber(s), Numbers.tryCreateNumber(s).orElseThrow());
        }
    }

    @Test
    @DisplayName("J2: an f/d suffix is honoured at any length")
    public void test_J2_typeSuffixIsHonouredAtAnyLength() {
        assertSame(Float.class, Numbers.createNumber("1." + "0".repeat(4094) + "f").getClass());
        assertSame(Float.class, Numbers.createNumber("1." + "0".repeat(4095) + "f").getClass());
        assertSame(Double.class, Numbers.createNumber("1." + "0".repeat(4094) + "d").getClass());
        assertSame(Double.class, Numbers.createNumber("1." + "0".repeat(4095) + "d").getClass());

        // A lexical zero is still built as the correctly signed binary zero, suffix and all.
        assertEquals(Float.valueOf(0.0f), Numbers.createNumber("0." + "0".repeat(4095) + "f"));
        assertEquals(Float.valueOf(-0.0f), Numbers.createNumber("-0." + "0".repeat(4095) + "f"));

        // createNumber(s) and createNumber(s + "d") still agree on the result type at every length.
        for (final int zeros : new int[] { 10, 4093, 4094, 4095, 4096, 5000 }) {
            final String s = "1." + "0".repeat(zeros);
            assertSame(Numbers.createNumber(s).getClass(), Numbers.createNumber(s + "d").getClass(), "zeros=" + zeros);
        }
    }

    @Test
    @DisplayName("D1: over-long tokens still escalate to BigDecimal when, and only when, the value requires it")
    public void test_D1_valueStillSelectsBigDecimal() {
        final int limit = Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH;

        // Underflow to zero with non-zero digits present.
        final String tiny = "0." + "0".repeat(limit + 5) + "1";
        assertInstanceOf(BigDecimal.class, Numbers.createNumber(tiny));
        assertEquals(0, new BigDecimal(tiny).compareTo((BigDecimal) Numbers.createNumber(tiny)));
        assertInstanceOf(BigDecimal.class, Numbers.createNumber(tiny + "f"));
        assertInstanceOf(BigDecimal.class, Numbers.createNumber(tiny + "d"));

        // Overflow to infinity.
        final String huge = "1" + "0".repeat(limit + 5) + ".5";
        assertInstanceOf(BigDecimal.class, Numbers.createNumber(huge));
        assertEquals(0, new BigDecimal(huge).compareTo((BigDecimal) Numbers.createNumber(huge)));
        assertInstanceOf(BigDecimal.class, Numbers.createNumber(huge + "d"));

        // A value that fits a double but not a float honours the f suffix by escalating one step.
        final String floatOverflow = "1" + "0".repeat(limit + 5) + "e-" + (limit + 5 - 39);
        assertEquals(1e39d, Numbers.createNumber(floatOverflow + "f").doubleValue(), 0.0d);
        assertInstanceOf(Double.class, Numbers.createNumber(floatOverflow + "f"));

        // An integral token of any length is a BigInteger.
        final String integral = "7" + "0".repeat(limit + 5);
        assertEquals(new BigInteger(integral), Numbers.createNumber(integral));
    }

    // decodeBigInteger

    @Test
    public void testDecodeBigInteger() {
        assertEquals(new BigInteger("12345678901234567890"), Numbers.decodeBigInteger("12345678901234567890"));
        assertEquals(new BigInteger("FF", 16), Numbers.decodeBigInteger("0xFF"));
        assertEquals(new BigInteger("77", 8), Numbers.decodeBigInteger("077"));
        assertEquals(BigInteger.valueOf(-1), Numbers.decodeBigInteger("-1"));
        assertNull(Numbers.decodeBigInteger(null));
        assertNull(Numbers.decodeBigInteger(""));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("abc"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("--1"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("+-1"));
    }

    // C-077 (cycle 2): the overflow fallback reuses the scan's sign/radix/digit start
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

    @Test
    public void c018_decodeBigIntegerRejectionsAreUnchangedAndCarryTheSharedCause() {
        for (final String text : new String[] { "123L", "0xFFL", "+-1", "--1", "0x-1", "0x", "#", " ", " 1", "1 ", "abc", "08", "1-", "١٢٣", "1_000",
                "0x" + "F".repeat(40) + "L", "1\n2" }) {
            final NumberFormatException e = assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger(text), text);
            assertTrue(e.getMessage().endsWith(" is not a valid BigInteger."), e.getMessage());
            assertNotNull(e.getCause(), text);
        }

        assertEquals("invalid character 'L' at index 4 of 0xFFL",
                assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("0xFFL")).getCause().getMessage());
        assertEquals("no digits in integer token 0x", assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("0x")).getCause().getMessage());
        assertEquals("invalid character '-' at index 1 of +-1",
                assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("+-1")).getCause().getMessage());
        assertEquals("invalid character '8' at index 1 of 08",
                assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("08")).getCause().getMessage());
        // The same token gives the same cause on the sibling decoder.
        assertEquals(assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("+-1")).getCause().getMessage(),
                assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("+-1")).getCause().getMessage());
    }

    // C-018
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

    // decodeInteger

    @Test
    public void testDecodeInteger() {
        assertEquals(Integer.valueOf(123), Numbers.decodeInteger("123"));
        assertEquals(Integer.valueOf(255), Numbers.decodeInteger("0xFF"));
        assertEquals(Integer.valueOf(8), Numbers.decodeInteger("010"));
        assertNull(Numbers.decodeInteger(null));
        assertNull(Numbers.decodeInteger(""));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("abc"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("08"));
        assertEquals("abc is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("abc")).getMessage());
        for (final String token : new String[] { "0", "00", "+00", "010", "+010", "-010", "0x7fffffff", "-0x80000000", "#7f" }) {
            assertEquals(Integer.decode(token), Numbers.decodeInteger(token), token);
        }
    }

    @Test
    public void c034_decodeOutOfRangeIsNumberFormatException() {
        final NumberFormatException intNfe = assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("2147483648"));
        assertEquals("2147483648 is not a valid Integer.", intNfe.getMessage());
        assertEquals("Integer value is out of range", intNfe.getCause().getMessage());
        assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("-2147483649"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("0x80000000"));

        final NumberFormatException longNfe = assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("9223372036854775808"));
        assertEquals("9223372036854775808 is not a valid Long.", longNfe.getMessage());
        assertEquals("Long value is out of range", longNfe.getCause().getMessage());
        assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("0x8000000000000000"));

        // the to* family reports the same values as ArithmeticException; decodeBigInteger has no range
        assertThrows(ArithmeticException.class, () -> Numbers.toInt("2147483648"));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong("9223372036854775808"));
        assertEquals(new BigInteger("9223372036854775808"), Numbers.decodeBigInteger("9223372036854775808"));
    }

    @Test
    public void testNullableCreationTreatsOnlyEmptyAsNull() {
        assertNull(Numbers.decodeInteger(null));
        assertNull(Numbers.parseFloat(""));
        assertNull(Numbers.createNumber(""));
        for (final String whitespace : new String[] { " ", "\n" }) {
            assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger(whitespace));
            assertThrows(NumberFormatException.class, () -> Numbers.parseFloat(whitespace));
            assertThrows(NumberFormatException.class, () -> Numbers.createNumber(whitespace));
            assertTrue(Numbers.tryParseInt(whitespace).isEmpty());
            assertTrue(Numbers.tryCreateNumber(whitespace).isEmpty());
        }
    }

    // decodeLong

    @Test
    public void testDecodeLong() {
        assertEquals(Long.valueOf(123L), Numbers.decodeLong("123"));
        assertEquals(Long.valueOf(255L), Numbers.decodeLong("0xFFL"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.decodeLong("-9223372036854775808"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.decodeLong("-0x8000000000000000"));
        assertNull(Numbers.decodeLong(null));
        assertNull(Numbers.decodeLong(""));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("9223372036854775808"));
        assertEquals("abc is not a valid Long.", assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("abc")).getMessage());
        for (final String token : new String[] { "0", "010", "0x7fffffffffffffff", "-0x8000000000000000" }) {
            assertEquals(Long.decode(token), Numbers.decodeLong(token), token);
        }
    }

    // C-004: decodeLong's cross-scanner invariant is checked without relying on -ea
    @Test
    public void test_decodeLong_stillHandlesTheLongMinValueTokenAndMalformedInput() {
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.decodeLong("-9223372036854775808"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.decodeLong("-0x8000000000000000"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.decodeLong("-01000000000000000000000"));
        assertEquals(Long.valueOf(255L), Numbers.decodeLong("0xFFL"));

        assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("9223372036854775808"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("-9223372036854775809"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("12x3"));
        assertBoundedAndEscaped(assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("9".repeat(100_000))));
    }

    // D3 - the invariant behind the AssertionError guards.
    @Test
    @DisplayName("D3: the two integer scanners agree, so no input reaches an AssertionError guard")
    public void test_D3_scannerGuardsAreUnreachableForEveryToken() {
        // parseLongInRange and decodeInteger now fail loudly if the range-limited scan and the diagnostic
        // scan ever disagree, matching decodeLong. Sweep the grammar to show that they do not: any
        // AssertionError escaping here would be that disagreement.
        sweepScannerAgreement("0123456789.eE+-xX#lLfFdD", 3);
        sweepScannerAgreement("0123456789+-xX#lL", 4);

        // Plus the boundary tokens most likely to expose a sentinel collision.
        final String[] boundaries = { "-9223372036854775808", "9223372036854775807", "-9223372036854775809", "9223372036854775808", "-2147483648", "2147483647",
                "-2147483649", "2147483648", "0x8000000000000000", "-0x8000000000000000", "#8000000000000000", "-#8000000000000000", "-01000000000000000000000",
                "-9223372036854775808L", "0000000000000009223372036854775807", "-0000000000000009223372036854775808", "0x7FFFFFFFFFFFFFFF", "-0x1" };

        for (final String token : boundaries) {
            exerciseScanners(token);
        }

        // The one documented sentinel collision still round-trips.
        assertEquals(Long.MIN_VALUE, Numbers.toLong("-9223372036854775808"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.decodeLong("-9223372036854775808"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Numbers.decodeLong("-0x8000000000000000"));
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), Numbers.decodeInteger("-2147483648"));
        assertEquals(Integer.MIN_VALUE, Numbers.toInt("-2147483648"));
    }

    // extractFirstDouble

    @Test
    public void testExtractFirstDouble() {
        assertEquals(3.14, Numbers.extractFirstDouble("pi is 3.14").getAsDouble(), 0.001);
        assertEquals(123.456, Numbers.extractFirstDouble("abc123.456def").orElseThrow(), DELTA);
        assertEquals(-78.9, Numbers.extractFirstDouble("xyz-78.9abc").orElseThrow(), DELTA);
        assertEquals(-2.5, Numbers.extractFirstDouble("temp is -2.5 degrees").getAsDouble(), 0.001);
        assertTrue(Numbers.extractFirstDouble("noNumber").isEmpty());
        assertTrue(Numbers.extractFirstDouble("").isEmpty());
        assertTrue(Numbers.extractFirstDouble(null).isEmpty());
        assertTrue(Numbers.extractFirstDouble("null").isEmpty());

        assertEquals(0.5, Numbers.extractFirstDouble(".5").orElseThrow(), DELTA);
        assertEquals(-0.5, Numbers.extractFirstDouble("-.5").orElseThrow(), DELTA);
        assertEquals(-0.5, Numbers.extractFirstDouble("offset=-.5").orElseThrow(), DELTA);
        assertEquals(0.5, Numbers.extractFirstDouble("+.5").orElseThrow(), DELTA);
        assertEquals(1.0, Numbers.extractFirstDouble("1.").orElseThrow(), DELTA);
    }

    @Test
    public void testExtractFirstDouble_Scientific() {
        assertEquals(1.23e10, Numbers.extractFirstDouble("1.23e10", true).getAsDouble(), 1.0);
        assertEquals(1.23, Numbers.extractFirstDouble("1.23e10", false).getAsDouble(), 0.001);
        assertEquals(1.23e-4, Numbers.extractFirstDouble("value is 1.23e-4", true).getAsDouble(), 0.00001);
        assertEquals(1.23, Numbers.extractFirstDouble("value is 1.23e-4", false).getAsDouble(), 0.001);
        assertEquals(-3.14e-2, Numbers.extractFirstDouble("temp -3.14e-2 celsius", true).getAsDouble(), DELTA);
        assertEquals(-5.67e-3, Numbers.extractFirstDouble("result: -5.67e-3", true).orElseThrow(), DELTA);
        assertEquals(50.0, Numbers.extractFirstDouble(".5e2", true).orElseThrow(), DELTA);
        assertEquals(-50.0, Numbers.extractFirstDouble("-.5e2", true).orElseThrow(), DELTA);
        assertEquals(0.5, Numbers.extractFirstDouble(".5e2", false).orElseThrow(), DELTA);
        assertFalse(Numbers.extractFirstDouble(null, true).isPresent());
        assertFalse(Numbers.extractFirstDouble("", true).isPresent());
        assertFalse(Numbers.extractFirstDouble("no number here", true).isPresent());
    }

    @Test
    @DisplayName("J3: the token length limit bounds the matched token, not the argument it was found in")
    public void test_J3_extractFirstDoubleLimitsTheTokenNotTheArgument() {
        final String haystack = "x".repeat(50_000) + "1.5" + "y".repeat(50_000);
        assertEquals(100_003, haystack.length());
        assertEquals(u.OptionalDouble.of(1.5d), Numbers.extractFirstDouble(haystack));
        assertEquals(1.5d, Numbers.extractFirstDoubleOrElse(haystack, 0d));

        // Whereas the bounded parsers do measure the raw argument.
        assertFalse(Numbers.isParsable(" ".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH) + "1.5"));
        assertThrows(NumberFormatException.class, () -> Numbers.toDouble(" ".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH) + "1.5"));
    }

    // D1 - the extract family saturates where the integer finders throw.
    @Test
    @DisplayName("D1: extractFirstDouble saturates to Infinity where extractFirstInt throws")
    public void test_D1_extractFirstDoubleSaturatesInsteadOfThrowing() {
        final String tooBig = "9".repeat(400);

        assertEquals(u.OptionalDouble.of(Double.POSITIVE_INFINITY), Numbers.extractFirstDouble(tooBig));
        assertEquals(u.OptionalDouble.of(Double.POSITIVE_INFINITY), Numbers.extractFirstDouble(tooBig, true));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.extractFirstDoubleOrElse(tooBig, 0d));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.extractFirstDoubleOrElse(tooBig, 0d, true));
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.extractFirstDoubleOrElse("x=-" + tooBig, 0d));

        // A token that underflows becomes a signed zero rather than an absence.
        assertEquals(0d, Numbers.extractFirstDoubleOrElse("0." + "0".repeat(400) + "1", 7d));
        assertEquals("-0.0", Double.toString(Numbers.extractFirstDoubleOrElse("-1e-400", -7d, true)));

        // The integer finders keep the opposite policy: out of range is an error, not a value.
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstInt(tooBig));
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstLong(tooBig));
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstIntOrElse(tooBig, 0));
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstLongOrElse(tooBig, 0L));

        // An over-long token is still an error for the double family, which is the case that does throw.
        final String overLong = "1".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH + 1);
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstDouble(overLong));
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstDoubleOrElse(overLong, 0d));
    }

    @Test
    public void c149_overLengthTokenMessageIsIdenticalOnTheWholeStringAndSlicePaths() {
        final int limit = Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH;
        final String token = "1".repeat(limit + 1);
        final NumberFormatException whole = assertThrows(NumberFormatException.class, () -> Numbers.toDouble(token));
        final NumberFormatException sliced = assertThrows(NumberFormatException.class, () -> Numbers.extractFirstDouble("id=" + token + ";"));
        final NumberFormatException slicedOrElse = assertThrows(NumberFormatException.class, () -> Numbers.extractFirstDoubleOrElse("id=" + token + ";", -1.0));

        assertEquals("1".repeat(64) + "...[" + (limit + 1) + " chars] is not a valid Double.", whole.getMessage());
        assertEquals(whole.getMessage(), sliced.getMessage());
        assertEquals(whole.getMessage(), slicedOrElse.getMessage());
        assertEquals("Double input length " + (limit + 1) + " exceeds limit " + limit, whole.getCause().getMessage());
        assertEquals(whole.getCause().getMessage(), sliced.getCause().getMessage());
        assertEquals(whole.getCause().getMessage(), slicedOrElse.getCause().getMessage());

        // the slice previews the token only, never its surroundings
        final String signed = "-1." + "3".repeat(5000);
        assertEquals("-1." + "3".repeat(61) + "...[5003 chars] is not a valid Double.",
                assertThrows(NumberFormatException.class, () -> Numbers.extractFirstDouble("x " + signed + " tail")).getMessage());

        // a token at the limit parses on both paths
        final String atLimit = "1".repeat(limit);
        assertEquals(Numbers.toDouble(atLimit), Numbers.extractFirstDouble("id=" + atLimit).getAsDouble(), 0.0);
    }

    @Test
    public void testExtractFirstDoubleOrElse() {
        assertEquals(3.14, Numbers.extractFirstDoubleOrElse("pi is 3.14", 1.0), 0.001);
        assertEquals(123.45, Numbers.extractFirstDoubleOrElse("abc 123.45 def", 9.9), DELTA);
        assertEquals(9.9, Numbers.extractFirstDoubleOrElse("abc def", 9.9), DELTA);
        assertEquals(1.0, Numbers.extractFirstDoubleOrElse("noNumber", 1.0), 0.001);
        assertEquals(1.0, Numbers.extractFirstDoubleOrElse("", 1.0), 0.001);
        assertEquals(1.0, Numbers.extractFirstDoubleOrElse(null, 1.0), 0.001);
        assertEquals(0.5, Numbers.extractFirstDoubleOrElse(".5", 9.9), DELTA);
        assertEquals(-0.5, Numbers.extractFirstDoubleOrElse("x=-.5", 9.9), DELTA);

        assertEquals(1.5e10, Numbers.extractFirstDoubleOrElse("number 1.5e10 here", 0.0, true), 0.001);
        assertEquals(1.23e10, Numbers.extractFirstDoubleOrElse("val 1.23e10 end", 0.0, true), DELTA);
        assertEquals(1.23, Numbers.extractFirstDoubleOrElse("val 1.23e10 end", 0.0, false), DELTA);
        assertEquals(1.2E-5, Numbers.extractFirstDoubleOrElse("num 1.2E-5 text", 0.0, true), DELTA);
        assertEquals(-99.0, Numbers.extractFirstDoubleOrElse("no numbers", -99.0, true), DELTA);
        assertEquals(-99.0, Numbers.extractFirstDoubleOrElse(null, -99.0, true), DELTA);
        assertEquals(50.0, Numbers.extractFirstDoubleOrElse(".5e2", 9.9, true), DELTA);
        assertEquals(-50.0, Numbers.extractFirstDoubleOrElse("x=-.5e2", 9.9, true), DELTA);
    }

    // extractFirstInt

    @Test
    public void testExtractFirstInt() {
        assertEquals(123, Numbers.extractFirstInt("abc123def").getAsInt());
        assertEquals(123, Numbers.extractFirstInt("abc 123 def 456").getAsInt());
        assertEquals(-456, Numbers.extractFirstInt("test-456xyz").getAsInt());
        assertEquals(-42, Numbers.extractFirstInt("val=-42 end").getAsInt());
        assertTrue(Numbers.extractFirstInt("noNumber").isEmpty());
        assertTrue(Numbers.extractFirstInt("").isEmpty());
        assertTrue(Numbers.extractFirstInt(null).isEmpty());
    }

    // D1: the extractFirst* family scans a PLAIN decimal token. RegExUtil.INTEGER_FINDER only ever yields
    // an optional sign plus ASCII digits, so no radix prefix can reach the scanner.
    @Test
    public void test_extractFirstInt_neverInterpretsARadixPrefix() {
        // The finder stops at 'x', so the match is the leading "0" -- the extract family is decimal-only,
        // unlike the decimal-first to* parsers, which do honour a 0x/# prefix.
        assertEquals(0, Numbers.extractFirstInt("0x1F").getAsInt());
        assertEquals(31, Numbers.toInt("0x1F"));
        assertEquals(0, Numbers.extractFirstLong("0x1F").getAsLong());
        assertEquals(31L, Numbers.toLong("0x1F"));

        // a '#' prefix is not matched at all: there is no digit before it
        assertEquals(255, Numbers.extractFirstIntOrElse("#FF", 255));
        assertEquals(255, Numbers.toInt("#FF"));

        // a leading zero is decimal padding here, exactly as in the to* family (never octal)
        assertEquals(10, Numbers.extractFirstInt("010").getAsInt());
        assertEquals(8, Numbers.extractFirstInt("08").getAsInt());
        assertEquals(10L, Numbers.extractFirstLong("010").getAsLong());

        // the OrElse variants publish the same examples in their Javadoc, so execute those too
        assertEquals(0, Numbers.extractFirstIntOrElse("0x1F", -1));
        assertEquals(10, Numbers.extractFirstIntOrElse("010", -1));
        assertEquals(0L, Numbers.extractFirstLongOrElse("0x1F", -1L));
        assertEquals(10L, Numbers.extractFirstLongOrElse("010", -1L));
    }

    @Test
    public void testExtractFirstIntLong_Overflow() {
        final NumberFormatException intEx = assertThrows(NumberFormatException.class, () -> Numbers.extractFirstInt("id=99999999999"));
        assertEquals("integer token '99999999999' does not fit in int", intEx.getMessage());
        assertNotNull(intEx.getCause());
        assertTrue(intEx.getCause() instanceof NumberFormatException);
        assertEquals("integer token '99999999999' does not fit in int",
                assertThrows(NumberFormatException.class, () -> Numbers.extractFirstIntOrElse("id=99999999999", 0)).getMessage());

        final NumberFormatException longEx = assertThrows(NumberFormatException.class, () -> Numbers.extractFirstLong("id=99999999999999999999"));
        assertEquals("integer token '99999999999999999999' does not fit in long", longEx.getMessage());
        assertNotNull(longEx.getCause());
        assertEquals("integer token '99999999999999999999' does not fit in long",
                assertThrows(NumberFormatException.class, () -> Numbers.extractFirstLongOrElse("id=99999999999999999999", 0L)).getMessage());

        final String hugeInput = "id=" + "9".repeat(5000);
        final NumberFormatException hugeInt = assertThrows(NumberFormatException.class, () -> Numbers.extractFirstInt(hugeInput));
        assertTrue(hugeInt.getMessage().length() < 256, hugeInt.getMessage());
        assertTrue(hugeInt.getMessage().contains("5000 chars"));
        assertNotNull(hugeInt.getCause());
        assertTrue(hugeInt.getCause().getMessage() == null || hugeInt.getCause().getMessage().length() < 256);

        final NumberFormatException hugeLong = assertThrows(NumberFormatException.class, () -> Numbers.extractFirstLong(hugeInput));
        assertTrue(hugeLong.getMessage().length() < 256, hugeLong.getMessage());
        assertTrue(hugeLong.getMessage().contains("5000 chars"));

        final StringBuilder sb = new StringBuilder("x");
        for (int i = 0; i < 400; i++) {
            sb.append('9');
        }
        assertEquals(Double.POSITIVE_INFINITY, Numbers.extractFirstDouble(sb.toString()).getAsDouble(), 0.0);
    }

    // D5 (by design, not a fix): the extract* family reports an out-of-range token as
    // NumberFormatException where the to* family reports ArithmeticException. Locked so the divergence
    // cannot drift silently.
    @Test
    public void test_outOfRangeExceptionTypeDivergesBetweenTheExtractAndToFamilies() {
        // extract*: NumberFormatException, and NOT ArithmeticException
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstInt("id=99999999999"));
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstIntOrElse("id=99999999999", 0));
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstLong("id=9999999999999999999"));
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstLongOrElse("id=9999999999999999999", 0L));

        // to*: ArithmeticException for the very same magnitude
        assertThrows(ArithmeticException.class, () -> Numbers.toInt("99999999999"));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong("9999999999999999999"));

        // and the two are genuinely different types, not one extending the other
        assertFalse(ArithmeticException.class.isAssignableFrom(NumberFormatException.class));
        assertFalse(NumberFormatException.class.isAssignableFrom(ArithmeticException.class));
    }

    @Test
    public void testExtractFirstIntOrElse() {
        assertEquals(123, Numbers.extractFirstIntOrElse("abc123def", 99));
        assertEquals(99, Numbers.extractFirstIntOrElse("abcdef", 99));
        assertEquals(99, Numbers.extractFirstIntOrElse("", 99));
        assertEquals(99, Numbers.extractFirstIntOrElse(null, 99));
    }

    @Test
    public void testExtractFirstLong() {
        assertEquals(1234567890L, Numbers.extractFirstLong("abc 1234567890 def").getAsLong());
        assertEquals(9223372036854775807L, Numbers.extractFirstLong("value9223372036854775807end").getAsLong());
        assertEquals(-987L, Numbers.extractFirstLong("word -987 test").getAsLong());
        assertEquals(-100000000000L, Numbers.extractFirstLong("val=-100000000000").getAsLong());
        assertTrue(Numbers.extractFirstLong("noNumber").isEmpty());
        assertTrue(Numbers.extractFirstLong("").isEmpty());
        assertTrue(Numbers.extractFirstLong(null).isEmpty());
    }

    @Test
    public void test_extractFirstLong_stillResolvesTheLongMinValueToken() {
        // Long.MIN_VALUE collides with the scanner's INVALID sentinel and is disambiguated lexically; that
        // path takes the same grammar argument, so it has to keep working after the D1 change.
        assertEquals(Long.MIN_VALUE, Numbers.extractFirstLong("x=-9223372036854775808").getAsLong());
        assertEquals(Long.MIN_VALUE, Numbers.extractFirstLongOrElse("-9223372036854775808", 0L));
        assertEquals(Long.MAX_VALUE, Numbers.extractFirstLong("9223372036854775807").getAsLong());
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstLong("9223372036854775808"));
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstLong("-9223372036854775809"));
    }

    @Test
    public void testExtractFirstLongOrElse() {
        assertEquals(123L, Numbers.extractFirstLongOrElse("abc123def", 99L));
        assertEquals(999L, Numbers.extractFirstLongOrElse("abc def", 999L));
        assertEquals(99L, Numbers.extractFirstLongOrElse("", 99L));
        assertEquals(99L, Numbers.extractFirstLongOrElse(null, 99L));
    }

    @Test
    @DisplayName("B1: the lexical recovery accepts exactly the BigDecimal grammar, restricted to ASCII digits")
    public void test_B1_grammarMatchesBigDecimalExceptForNonAsciiDigits() {
        // Accepted by both: the BigDecimal target reproduces the text exactly, so it came from the text and
        // not from doubleValue(). Compared as BigDecimal rather than BigInteger because a legal token such as
        // "0e999999999" has a billion-digit integer form.
        for (final String text : new String[] { "1", "+1", "-1", "1.", "+1.", ".5", "1.5", "1.e5", ".5e5", "1e5", "1E5", "1e+5", "1e-5", "010", "00",
                "1e0000000005", "1e999999999", "0e999999999", "1e2147483648" }) {
            assertEquals(new BigDecimal(text), Numbers.convert(n(text), BigDecimal.class), text);
        }

        // Rejected by both: the sentinel doubleValue() is used instead.
        final BigDecimal sentinel = BigDecimal.valueOf(SENTINEL);

        for (final String text : new String[] { ".", "1e", "1e+", "e5", "", " 1", "1 ", "1_0", "1.2.3", "--1", "1-", "1+1", "1e5e5", "1.5f", "NaN", "Infinity",
                "0x10", "#10", "1L", "١٢٣", "1۲1" }) {
            assertEquals(sentinel, Numbers.convert(n(text), BigDecimal.class), text);
            assertEquals((long) SENTINEL, Numbers.toLong(n(text)), text);
        }

        // An exponent BigDecimal itself refuses is rejected here too, so the two stay in step.
        for (final String text : new String[] { "1e-2147483648", "1e-2147483649", "0e-2147483648", "1e9999999999", "1e10000000000",
                "1e99999999999999999999" }) {
            assertThrows(NumberFormatException.class, () -> new BigDecimal(text), text);
            assertEquals(sentinel, Numbers.convert(n(text), BigDecimal.class), text);
            assertEquals((long) SENTINEL, Numbers.toLong(n(text)), text);
        }
    }

    // isCreatable

    @Test
    public void testIsCreatable() {
        assertTrue(Numbers.isCreatable("123"));
        assertTrue(Numbers.isCreatable("-123"));
        assertTrue(Numbers.isCreatable("+123"));
        assertTrue(Numbers.isCreatable("12.3"));
        assertTrue(Numbers.isCreatable("1.23e5"));
        assertTrue(Numbers.isCreatable("1.5e-3"));
        assertTrue(Numbers.isCreatable("123L"));
        assertTrue(Numbers.isCreatable("1.0f"));
        assertTrue(Numbers.isCreatable("1.0d"));
        assertTrue(Numbers.isCreatable("0x1A"));
        assertTrue(Numbers.isCreatable("0x00FF"));
        assertTrue(Numbers.isCreatable("-0xABCDEF"));
        assertTrue(Numbers.isCreatable("077"));
        assertTrue(Numbers.isCreatable("0.9"));
        assertTrue(Numbers.isCreatable("#FF"));
        assertTrue(Numbers.isCreatable("-#FF"));
        assertTrue(Numbers.isCreatable("+#1a2b"));

        assertFalse(Numbers.isCreatable(null));
        assertFalse(Numbers.isCreatable(""));
        assertFalse(Numbers.isCreatable("   "));
        assertFalse(Numbers.isCreatable("abc"));
        assertFalse(Numbers.isCreatable("123.45.67"));
        assertFalse(Numbers.isCreatable("123e"));
        assertFalse(Numbers.isCreatable("++123"));
        assertFalse(Numbers.isCreatable("+"));
        assertFalse(Numbers.isCreatable("0x"));
        assertFalse(Numbers.isCreatable("0xG"));
        assertFalse(Numbers.isCreatable("08"));
        assertFalse(Numbers.isCreatable("09"));
        assertFalse(Numbers.isCreatable("0789"));
        assertFalse(Numbers.isCreatable("#"));
        assertFalse(Numbers.isCreatable("#FG"));
        assertFalse(Numbers.isCreatable(" "));
        assertFalse(Numbers.isCreatable("  123  "));
    }

    @Test
    public void testIsCreatable_doesNotMaterializeLargeInteger() {
        assertTimeout(Duration.ofSeconds(2), () -> assertTrue(Numbers.isCreatable("9".repeat(1_000_000))));
    }

    @Test
    public void testIsCreatable_ExponentScale() {
        assertTrue(Numbers.isCreatable("1e2147483648"));
        assertTrue(Numbers.isCreatable("1.0e2147483649D"));
        final Number exponentOnlyBoundary = Numbers.createNumber("1e2147483648");
        assertEquals(BigDecimal.class, exponentOnlyBoundary.getClass());
        assertEquals(Integer.MIN_VALUE, ((BigDecimal) exponentOnlyBoundary).scale());

        assertFalse(Numbers.isCreatable("1e2147483649"));
        assertFalse(Numbers.isCreatable("1e-2147483648"));
        assertTrue(Numbers.isCreatable("1e2147483647"));
        assertTrue(Numbers.isCreatable("1e-2147483647"));
        assertFalse(Numbers.isCreatable("1.0e-2147483647"));
        assertTrue(Numbers.isCreatable("1.0e-2147483646"));
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("1e2147483649"));
    }

    @Test
    public void testIsCreatable_matchesCreateNumber() {
        final String[] valid = { "0", "-123", "+.5", "123.", "0xFF", "-0x80000000", "#fffffffffffffffffff", "010", "01f", "09f", "0123L", "01e1", "1.5e3D",
                "1e2147483648", "0e999999999999999999999", "#FF", "+#1a2b" };
        for (final String value : valid) {
            assertNotNull(Numbers.createNumber(value), value);
            assertTrue(Numbers.isCreatable(value), value);
        }
        assertEquals(Float.valueOf(1.0f), Numbers.createNumber("01f"));
        assertEquals(Long.valueOf(83L), Numbers.createNumber("0123L"));
        assertEquals(Double.valueOf(10.0d), Numbers.createNumber("01e1"));

        final String[] invalid = { " ", "\n", "abc", "09", "0789", "0x", "0xFFL", "#", "1e", "1.2.3", "NaN", "Infinity", "0x1.0p2", "1e2147483649",
                "1.0e-2147483647", "١٢٣" };
        for (final String value : invalid) {
            assertThrows(NumberFormatException.class, () -> Numbers.createNumber(value), value);
            assertFalse(Numbers.isCreatable(value), value);
        }
        assertNull(Numbers.createNumber(null));
        assertNull(Numbers.createNumber(""));
    }

    @Test
    public void testIsCreatable_ZeroSignificand() {
        assertTrue(Numbers.isCreatable("0e9223372036854775808"));
        assertEquals(0L, Double.doubleToRawLongBits(Numbers.createNumber("0e9223372036854775808").doubleValue()));
        assertTrue(Numbers.isCreatable("-0.000e-999999999999999999999D"));
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(Numbers.createNumber("-0.000e-999999999999999999999D").doubleValue()));
        assertFalse(Numbers.isCreatable("1e9223372036854775808"));
        assertFalse(Numbers.isCreatable("0e"));
        assertFalse(Numbers.isCreatable("0e+"));
    }

    // isParsable

    @Test
    public void testIsParsable() {
        assertTrue(Numbers.isParsable("123"));
        assertTrue(Numbers.isParsable("-123"));
        assertTrue(Numbers.isParsable("123.45"));
        assertTrue(Numbers.isParsable(".5"));
        assertTrue(Numbers.isParsable("0.5"));
        assertTrue(Numbers.isParsable("1.2e3"));
        assertTrue(Numbers.isParsable("123."));
        assertTrue(Numbers.isParsable("0."));
        assertTrue(Numbers.isParsable("1.5f"));
        assertTrue(Numbers.isParsable("NaN"));
        assertTrue(Numbers.isParsable("Infinity"));
        assertTrue(Numbers.isParsable("0x1.0p2"));
        assertTrue(Numbers.isParsable(" 123.45 "));

        assertFalse(Numbers.isParsable("0xFF"));
        assertFalse(Numbers.isParsable("123L"));
        assertFalse(Numbers.isParsable("abc"));
        assertFalse(Numbers.isParsable(""));
        assertFalse(Numbers.isParsable(null));
        assertFalse(Numbers.isParsable("1.2.3"));
        assertFalse(Numbers.isParsable("+"));
        assertFalse(Numbers.isParsable("-"));
        assertFalse(Numbers.isParsable("."));
        assertFalse(Numbers.isParsable("   "));
    }

    @Test
    public void testIsParsable_AsciiDigitsOnly() {
        assertTrue(Numbers.isParsable("123"));
        assertTrue(Numbers.isParsable("123.45"));
        assertFalse(Numbers.isParsable("١٢٣"));
        assertFalse(Numbers.isParsable("١.٥"));
        assertFalse(Numbers.isParsable("१२३"));
        assertFalse(Numbers.isParsable("１２３"));
        assertThrows(NumberFormatException.class, () -> Float.parseFloat("١٢٣"));

        for (final String value : new String[] { "1e39", "1e-50", "3.4028236e38f", "0x1.fffffep127", "NaN", "-Infinity" }) {
            assertDoesNotThrow(() -> Numbers.parseFloat(value), value);
            assertTrue(Numbers.isParsable(value), value);
        }
        for (final String value : new String[] { "123", "+.5", "123.", "1e309", "1e2147483648" }) {
            assertDoesNotThrow(() -> Numbers.parseBigDecimal(value), value);
            assertTrue(Numbers.isParsable(value), value);
        }
    }

    @Test
    public void testIsParsable_matchesParseUnion() {
        assertTrue(Numbers.isParsable("1.5f"));
        assertNotNull(Numbers.parseFloat("1.5f"));
        assertNotNull(Numbers.parseDouble("1.5f"));
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("1.5f"));

        assertTrue(Numbers.isParsable("NaN"));
        assertTrue(Numbers.parseFloat("NaN").isNaN());
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("NaN"));

        assertTrue(Numbers.isParsable("0x1.0p2"));
        assertEquals(4.0f, Numbers.parseFloat("0x1.0p2"));
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("0x1.0p2"));

        assertTrue(Numbers.isParsable("Infinity"));
        assertEquals(Float.POSITIVE_INFINITY, Numbers.parseFloat("Infinity"));
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("Infinity"));

        assertTrue(Numbers.isParsable(" 123.45 "));
        assertEquals(Float.valueOf(123.45f), Numbers.parseFloat(" 123.45 "));
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal(" 123.45 "));

        assertTrue(Numbers.isParsable("1e2147483649"));
        assertTrue(Numbers.parseFloat("1e2147483649").isInfinite());
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("1e2147483649"));

        assertFalse(Numbers.isParsable("0xFF"));
        assertThrows(NumberFormatException.class, () -> Numbers.parseFloat("0xFF"));
        assertFalse(Numbers.isParsable("123L"));
        assertThrows(NumberFormatException.class, () -> Numbers.parseFloat("123L"));

        assertNull(Numbers.parseFloat(null));
        assertNull(Numbers.parseDouble(null));
        assertNull(Numbers.parseBigDecimal(null));
        assertThrows(NumberFormatException.class, () -> Numbers.parseFloat("   "));
    }

    @Test
    public void testFloatingPointTokenLengthLimit() {
        final String atLimit = "0".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH);
        final String overLimit = atLimit + "0";
        assertTrue(Numbers.isParsable(atLimit));
        assertEquals(0.0d, Numbers.toDouble(atLimit), 0.0d);
        assertFalse(Numbers.isParsable(overLimit));
        assertTrue(Numbers.tryParseDouble(overLimit).isEmpty());
        final NumberFormatException nfe = assertThrows(NumberFormatException.class, () -> Numbers.parseDouble(overLimit));
        assertTrue(nfe.getMessage().length() < 128, nfe.getMessage());
        assertNotNull(nfe.getCause());
        assertTrue(nfe.getCause().getMessage().contains("exceeds limit"));
    }

    @Test
    @DisplayName("D1: the bounded parsers keep their limit; only the create family ignores it")
    public void test_D1_boundedParsersStillRejectOverLongInput() {
        final String overLimit = "1." + "0".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH - 1);
        assertEquals(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH + 1, overLimit.length());

        assertFalse(Numbers.isParsable(overLimit));
        assertThrows(NumberFormatException.class, () -> Numbers.toFloat(overLimit));
        assertThrows(NumberFormatException.class, () -> Numbers.toDouble(overLimit));
        assertThrows(NumberFormatException.class, () -> Numbers.parseFloat(overLimit));
        assertThrows(NumberFormatException.class, () -> Numbers.parseDouble(overLimit));
        assertTrue(Numbers.tryParseFloat(overLimit).isEmpty());
        assertTrue(Numbers.tryParseDouble(overLimit).isEmpty());
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstDouble("x=" + overLimit));

        assertEquals(Double.valueOf(1.0d), Numbers.createNumber(overLimit));
        assertEquals(Double.valueOf(1.0d), Numbers.tryCreateNumber(overLimit).orElseThrow());
        assertEquals(0, BigDecimal.ONE.compareTo(Numbers.parseBigDecimal(overLimit)));
    }

    @Test
    public void testParseOrElseAgreesWithTryParseAndToNumber() {
        final List<String> inputs = new ArrayList<>(List.of("0", "-0", "+0", "1", "-1", "007", "+42", "-128", "127", "128", "-32768", "32767", "65535",
                "2147483647", "-2147483648", "2147483648", "-2147483649", "9223372036854775807", "-9223372036854775808", "9223372036854775808",
                "-9223372036854775809", "0x7fffffff", "0x80000000", "-0x80000000", "#FF", "-#ff", "0X1F", "0x", "#", "0xG", "1L", "1l", "1LL", "L", "12F",
                "12D", "1.5", "1e5", " 1", "1 ", "+", "-", "--1", "+-1", "00000000000000000000000000001", "\u0661"));
        final Random random = new Random(20261008);
        final String alphabet = "0123456789000999+-xX#LlFfDdaE. ";

        for (int i = 0; i < 20_000; i++) {
            final StringBuilder sb = new StringBuilder();

            for (int j = 1 + random.nextInt(random.nextInt(4) == 0 ? 24 : 8); j > 0; j--) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }

            inputs.add(sb.toString());
        }

        for (final String text : inputs) {
            final u.OptionalInt asInt = Numbers.tryParseInt(text);
            final u.OptionalLong asLong = Numbers.tryParseLong(text);

            for (final int defaultValue : new int[] { -1, 0, Integer.MIN_VALUE }) {
                assertEquals(asInt.orElse(defaultValue), Numbers.parseIntOrElse(text, defaultValue), text);
                assertEquals(asLong.orElse(defaultValue), Numbers.parseLongOrElse(text, defaultValue), text);
            }

            if (asInt.isPresent()) {
                assertEquals(Numbers.toInt(text), asInt.getAsInt(), text);
            } else if (!text.isEmpty()) {
                assertThrows(RuntimeException.class, () -> Numbers.toInt(text), text);
            }

            if (asLong.isPresent()) {
                assertEquals(Numbers.toLong(text), asLong.getAsLong(), text);
            } else if (!text.isEmpty()) {
                assertThrows(RuntimeException.class, () -> Numbers.toLong(text), text);
            }
        }
    }

    // parseBigDecimal

    // D4: the character table still works from its new declaration site
    @Test
    @DisplayName("D4: moving the alphanumerics table above the methods changed nothing")
    public void test_D4_characterTableStillFilters() {
        assertEquals(new BigDecimal("1.5"), Numbers.parseBigDecimal("1.5"));
        assertEquals(BigInteger.valueOf(255), Numbers.decodeBigInteger("0xFF"));
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("1۲1"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeBigInteger("1۲1"));
        assertNull(Numbers.parseBigDecimal(""));
        assertNotNull(Numbers.parseBigDecimal("1e2147483648"));
    }

    @Test
    public void testParseBigDecimal() {
        assertEquals(new BigDecimal("123.456789"), Numbers.parseBigDecimal("123.456789"));
        assertEquals(BigDecimal.ZERO, Numbers.parseBigDecimal("0"));
        assertNull(Numbers.parseBigDecimal(null));
        assertNull(Numbers.parseBigDecimal(""));
        assertEquals(0, Numbers.parseBigDecimal("123.").scale());
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("abc"));
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("١٢٣"));
        assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("0x1.0p2"));
        assertEquals("1.2.3 is not a valid BigDecimal.", assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("1.2.3")).getMessage());
        final String overLimit = "1" + "0".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH);
        assertEquals(overLimit.length(), Numbers.parseBigDecimal(overLimit).precision());
    }

    @Test
    public void c061_parseBigDecimalDanglingExponentCauseHasMessage() {
        for (final String token : new String[] { "1e", "1e+", "1e-", "1E", "0.5e+", "1.e" }) {
            final NumberFormatException nfe = assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal(token), token);
            assertEquals(token + " is not a valid BigDecimal.", nfe.getMessage(), token);
            assertNotNull(nfe.getCause(), token);
            assertInstanceOf(NumberFormatException.class, nfe.getCause(), token);
            assertEquals("no exponent digits in " + token, nfe.getCause().getMessage(), token);

            for (Throwable t = nfe; t != null; t = t.getCause()) {
                assertNotNull(t.getMessage(), token + " -> " + t.getClass().getName());
            }
        }

        // a JDK cause that does carry a message is kept as is
        final NumberFormatException dot = assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("."));
        assertEquals(". is not a valid BigDecimal.", dot.getMessage());
        assertEquals("No digits found.", dot.getCause().getMessage());
        final NumberFormatException twoPoints = assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("1.2.3"));
        assertEquals("Character array contains more than one decimal point.", twoPoints.getCause().getMessage());

        // the quick pre-check path attaches no cause at all
        final NumberFormatException abc = assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("abc"));
        assertEquals("abc is not a valid BigDecimal.", abc.getMessage());
        assertNull(abc.getCause());

        assertEquals(new BigDecimal("1e5"), Numbers.parseBigDecimal("1e5"));
        assertEquals(new BigDecimal("1e+5"), Numbers.parseBigDecimal("1e+5"));
    }

    @Test
    public void testParseDouble() {
        assertEquals(Double.valueOf(1.5d), Numbers.parseDouble("1.5"));
        assertEquals(Double.valueOf(-1.5), Numbers.parseDouble("-1.5"));
        assertNull(Numbers.parseDouble(null));
        assertNull(Numbers.parseDouble(""));
        assertThrows(NumberFormatException.class, () -> Numbers.parseDouble("abc"));
        assertEquals("abc is not a valid Double.", assertThrows(NumberFormatException.class, () -> Numbers.parseDouble("abc")).getMessage());
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
    public void testParseFloat() {
        assertEquals(Float.valueOf(1.5f), Numbers.parseFloat("1.5"));
        assertEquals(Float.valueOf(0.0f), Numbers.parseFloat("0"));
        assertNull(Numbers.parseFloat(null));
        assertNull(Numbers.parseFloat(""));
        assertTrue(Numbers.parseFloat("NaN").isNaN());
        assertThrows(NumberFormatException.class, () -> Numbers.parseFloat("abc"));
        assertEquals("abc is not a valid Float.", assertThrows(NumberFormatException.class, () -> Numbers.parseFloat("abc")).getMessage());
        assertEquals("\"   \" is not a valid Float.", assertThrows(NumberFormatException.class, () -> Numbers.parseFloat("   ")).getMessage());
    }

    @Test
    public void testParseIntOrElse() {
        assertEquals(123, Numbers.parseIntOrElse("123", -1));
        assertEquals(10, Numbers.parseIntOrElse("010", -1));
        assertEquals(16, Numbers.parseIntOrElse("0x10", -1));
        assertEquals(123, Numbers.parseIntOrElse("123L", -1));
        assertEquals(Integer.MIN_VALUE, Numbers.parseIntOrElse("-2147483648", -1));
        assertEquals(-1, Numbers.parseIntOrElse("2147483648", -1));
        assertEquals(-1, Numbers.parseIntOrElse("12F", -1));
        assertEquals(-1, Numbers.parseIntOrElse("abc", -1));
        assertEquals(-1, Numbers.parseIntOrElse("", -1));
        assertEquals(-1, Numbers.parseIntOrElse(null, -1));
        assertEquals(-1, Numbers.parseIntOrElse("\u0661\u0662\u0663", -1));
        assertEquals(7, Numbers.parseIntOrElse(" ", 7));
    }

    @Test
    public void testParseLongOrElse() {
        assertEquals(123L, Numbers.parseLongOrElse("123", -1L));
        assertEquals(255L, Numbers.parseLongOrElse("0xFFL", -1L));
        assertEquals(Long.MIN_VALUE, Numbers.parseLongOrElse("-9223372036854775808", -1L));
        assertEquals(Long.MIN_VALUE, Numbers.parseLongOrElse("-0x8000000000000000", -1L));
        assertEquals(-1L, Numbers.parseLongOrElse("9223372036854775808", -1L));
        assertEquals(-1L, Numbers.parseLongOrElse("12D", -1L));
        assertEquals(-1L, Numbers.parseLongOrElse("abc", -1L));
        assertEquals(-1L, Numbers.parseLongOrElse("", -1L));
        assertEquals(-1L, Numbers.parseLongOrElse(null, -1L));
    }

    @Test
    public void testScannerFastPaths_RandomizedRoundTrips() {
        final Random random = new Random(0x5EED_C0DE_1234_5678L);
        for (int i = 0; i < 4096; i++) {
            final int intValue = random.nextInt();
            final BigInteger expectedInt = BigInteger.valueOf(intValue);
            final boolean explicitPlus = (i & 1) == 0;
            final String hexPrefix = (i % 3 == 0) ? "0x" : (i % 3 == 1 ? "0X" : "#");
            final String intDecimal = signedRadixToken(expectedInt, 10, "", explicitPlus);
            final String intHex = signedRadixToken(expectedInt, 16, hexPrefix, explicitPlus);
            assertEquals(intValue, Numbers.toInt(intDecimal), intDecimal);
            assertEquals(Integer.valueOf(intValue), Numbers.decodeInteger(intHex), intHex);

            final long longValue = random.nextLong();
            final BigInteger expectedLong = BigInteger.valueOf(longValue);
            final String longDecimal = signedRadixToken(expectedLong, 10, "", explicitPlus);
            assertEquals(longValue, Numbers.toLong(longDecimal), longDecimal);
            assertEquals(Long.valueOf(longValue), Numbers.decodeLong(longDecimal + "L"), longDecimal);
        }
    }

    @Test
    public void c151_everyScannerAgreesWithTheReferenceGrammarOverTheSignPrefixMatrix() {
        final String[] signs = { "", "+", "-" };
        final String[] prefixes = { "", "0x", "0X", "#", "0", "00", "0x0", "-", "+" };
        final String[] zeros = { "", "0", "000" };
        final String[] magnitudes = { "", "1", "7", "8", "9", "a", "f", "F", "g", "x", "10", "77", "80", "ff", "127", "128", "255", "32767", "32768", "7fff",
                "8000", "2147483647", "2147483648", "7fffffff", "80000000", "17777777777", "20000000000", "9223372036854775807", "9223372036854775808",
                "9223372036854775809", "7fffffffffffffff", "8000000000000000", "8000000000000001", "777777777777777777777", "1000000000000000000000",
                "1000000000000000000001", "99999999999999999999", "1-", "1+1" };
        final String[] suffixes = { "", "L", "l", "x", " " };
        int checked = 0;

        for (final String sign : signs) {
            for (final String prefix : prefixes) {
                for (final String zero : zeros) {
                    for (final String magnitude : magnitudes) {
                        for (final String suffix : suffixes) {
                            final String token = sign + prefix + zero + magnitude + suffix;

                            if (!token.isEmpty()) {
                                assertScannersAgree(token);
                                checked++;
                            }
                        }
                    }
                }
            }
        }

        assertTrue(checked > 15_000, "tokens checked: " + checked);

        // the one lexical collision, Long.MIN_VALUE, in every radix and spelling
        assertEquals(Long.MIN_VALUE, Numbers.toLong("-9223372036854775808"));
        assertEquals(Long.MIN_VALUE, Numbers.toLong("-0x8000000000000000"));
        assertEquals(Long.MIN_VALUE, Numbers.toLong("-#0000008000000000000000L"));
        assertEquals(Long.MIN_VALUE, Numbers.decodeLong("-01000000000000000000000"));
        assertEquals(BigInteger.valueOf(Long.MIN_VALUE), Numbers.decodeBigInteger("-0X8000000000000000"));
        assertEquals(Long.MIN_VALUE, Numbers.tryParseLong("-9223372036854775808").getAsLong());
        assertThrows(ArithmeticException.class, () -> Numbers.toLong("-01000000000000000000000")); // decimal-first: -10^21
        assertThrows(ArithmeticException.class, () -> Numbers.toLong("+9223372036854775808"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("9223372036854775808"));
    }

    @Test
    public void testTryCreateNumber() {
        final String[] valid = { "0", "-123", "+.5", "123.", "0xFF", "-0x80000000", "#fffffffffffffffffff", "010", "01f", "09f", "0123L", "01e1", "1.5e3D",
                "1e2147483648", "0e999999999999999999999" };
        for (final String value : valid) {
            final u.Optional<Number> result = assertDoesNotThrow(() -> Numbers.tryCreateNumber(value), value);
            assertTrue(result.isPresent(), value);
            assertEquals(Numbers.createNumber(value), result.get(), value);
            assertTrue(Numbers.isCreatable(value), value);
        }
        for (final String value : new String[] { null, "", " ", "abc", "09", "0xFFL", "NaN", "١٢٣" }) {
            assertTrue(Numbers.tryCreateNumber(value).isEmpty(), String.valueOf(value));
        }
    }

    @Test
    public void testTryParseDouble() {
        for (final String value : new String[] { "0", "123.45", "NaN", "-Infinity", "0x1.0p2", "1e309" }) {
            final u.OptionalDouble result = assertDoesNotThrow(() -> Numbers.tryParseDouble(value), value);
            assertTrue(result.isPresent(), value);
            assertEquals(Double.parseDouble(value), result.getAsDouble(), value);
        }
        for (final String value : new String[] { null, "", "abc", "123L", "0xFF" }) {
            assertTrue(Numbers.tryParseDouble(value).isEmpty(), String.valueOf(value));
        }
    }

    // D4: emptiness test unified
    @Test
    @DisplayName("D4: the try* entry points treat null and empty exactly as the throwing ones do")
    public void test_D4_tryEntryPointsEmptiness() {
        assertTrue(Numbers.tryParseFloat(null).isEmpty());
        assertTrue(Numbers.tryParseFloat("").isEmpty());
        assertTrue(Numbers.tryParseDouble(null).isEmpty());
        assertTrue(Numbers.tryParseDouble("").isEmpty());
        assertTrue(Numbers.tryCreateNumber(null).isEmpty());
        assertTrue(Numbers.tryCreateNumber("").isEmpty());

        assertEquals(1.5f, Numbers.tryParseFloat("1.5").get());
        assertEquals(1.5d, Numbers.tryParseDouble("1.5").get());
        assertEquals(Integer.valueOf(15), Numbers.tryCreateNumber("15").get());
    }

    @Test
    public void testTryParseFloat() {
        for (final String value : new String[] { "0", "123.45", "NaN", "-Infinity", "0x1.0p2", "1e39" }) {
            final u.OptionalFloat result = assertDoesNotThrow(() -> Numbers.tryParseFloat(value), value);
            assertTrue(result.isPresent(), value);
            assertEquals(Float.parseFloat(value), result.getAsFloat(), value);
        }
        for (final String value : new String[] { null, "", " ", "abc", "123L", "0xFF", "١٢٣" }) {
            assertTrue(Numbers.tryParseFloat(value).isEmpty(), String.valueOf(value));
        }
    }

    @Test
    public void testTryParseInt() {
        assertEquals(123, Numbers.tryParseInt("123").getAsInt());
        assertEquals(10, Numbers.tryParseInt("010").getAsInt());
        assertEquals(16, Numbers.tryParseInt("0x10").getAsInt());
        assertEquals(123, Numbers.tryParseInt("123L").getAsInt());
        assertTrue(Numbers.tryParseInt("2147483648").isEmpty());
        assertTrue(Numbers.tryParseInt("abc").isEmpty());
        assertTrue(Numbers.tryParseInt("").isEmpty());
        assertTrue(Numbers.tryParseInt(null).isEmpty());
        assertTrue(Numbers.tryParseInt("١٢٣").isEmpty());
    }

    @Test
    public void testTryParseLong() {
        assertEquals(123L, Numbers.tryParseLong("123").getAsLong());
        assertEquals(255L, Numbers.tryParseLong("0xFFL").getAsLong());
        assertEquals(Long.MIN_VALUE, Numbers.tryParseLong("-9223372036854775808").getAsLong());
        assertEquals(Long.MIN_VALUE, Numbers.tryParseLong("-0x8000000000000000").getAsLong());
        assertTrue(Numbers.tryParseLong("9223372036854775808").isEmpty());
        assertTrue(Numbers.tryParseLong("abc").isEmpty());
        assertTrue(Numbers.tryParseLong(null).isEmpty());
    }
}
