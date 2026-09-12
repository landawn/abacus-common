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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class NumbersExtractTest extends NumbersTestSupport {

    // ===== extractFirstInt / extractFirstIntOrElse =====

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

    @Test
    public void testExtractFirstIntOrElse() {
        assertEquals(123, Numbers.extractFirstIntOrElse("abc123def", 99));
        assertEquals(99, Numbers.extractFirstIntOrElse("abcdef", 99));
        assertEquals(99, Numbers.extractFirstIntOrElse("", 99));
        assertEquals(99, Numbers.extractFirstIntOrElse(null, 99));
    }

    // ===== extractFirstLong / extractFirstLongOrElse =====

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
    public void testExtractFirstLongOrElse() {
        assertEquals(123L, Numbers.extractFirstLongOrElse("abc123def", 99L));
        assertEquals(999L, Numbers.extractFirstLongOrElse("abc def", 999L));
        assertEquals(99L, Numbers.extractFirstLongOrElse("", 99L));
        assertEquals(99L, Numbers.extractFirstLongOrElse(null, 99L));
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

    // ===== extractFirstDouble / extractFirstDoubleOrElse =====

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
}
