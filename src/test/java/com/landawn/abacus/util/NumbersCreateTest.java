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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class NumbersCreateTest extends NumbersTestSupport {

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
}
