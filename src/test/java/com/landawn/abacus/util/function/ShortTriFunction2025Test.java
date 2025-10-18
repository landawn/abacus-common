/*
 * Copyright (C) 2025 HaiYang Li
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

package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ShortTriFunction2025Test extends TestBase {

    @Test
    public void test_apply() {
        ShortTriFunction<Integer> sum = (a, b, c) -> (int) (a + b + c);

        assertEquals(15, sum.apply((short) 3, (short) 5, (short) 7));
        assertEquals(0, sum.apply((short) 0, (short) 0, (short) 0));
        assertEquals(-15, sum.apply((short) -3, (short) -5, (short) -7));
    }

    @Test
    public void test_apply_lambda() {
        ShortTriFunction<String> formatter = (a, b, c) -> String.format("(%d, %d, %d)", a, b, c);

        assertEquals("(1, 2, 3)", formatter.apply((short) 1, (short) 2, (short) 3));
        assertEquals("(0, 0, 0)", formatter.apply((short) 0, (short) 0, (short) 0));
    }

    @Test
    public void test_apply_anonymousClass() {
        ShortTriFunction<Boolean> allPositive = new ShortTriFunction<Boolean>() {
            @Override
            public Boolean apply(short a, short b, short c) {
                return a > 0 && b > 0 && c > 0;
            }
        };

        assertTrue(allPositive.apply((short) 1, (short) 2, (short) 3));
        assertFalse(allPositive.apply((short) 1, (short) -2, (short) 3));
        assertFalse(allPositive.apply((short) 0, (short) 1, (short) 2));
    }

    @Test
    public void test_apply_returningDouble() {
        ShortTriFunction<Double> average = (a, b, c) -> (a + b + c) / 3.0;

        assertEquals(5.0, average.apply((short) 3, (short) 5, (short) 7), 0.001);
        assertEquals(0.0, average.apply((short) 0, (short) 0, (short) 0), 0.001);
    }

    @Test
    public void test_andThen() {
        ShortTriFunction<Integer> product = (a, b, c) -> (int) (a * b * c);
        java.util.function.Function<Integer, String> addPrefix = result -> "Result: " + result;

        ShortTriFunction<String> combined = product.andThen(addPrefix);

        assertEquals("Result: 24", combined.apply((short) 2, (short) 3, (short) 4));
        assertEquals("Result: 0", combined.apply((short) 0, (short) 5, (short) 10));
    }

    @Test
    public void test_andThen_multipleChaining() {
        ShortTriFunction<Integer> sum = (a, b, c) -> (int) (a + b + c);
        java.util.function.Function<Integer, Double> toDouble = i -> i * 1.5;
        java.util.function.Function<Double, String> format = d -> String.format("%.1f", d);

        ShortTriFunction<String> combined = sum.andThen(toDouble).andThen(format);

        assertEquals("15.0", combined.apply((short) 2, (short) 3, (short) 5));
        assertEquals("0.0", combined.apply((short) 0, (short) 0, (short) 0));
    }

    @Test
    public void test_apply_returningNull() {
        ShortTriFunction<String> alwaysNull = (a, b, c) -> null;

        assertNull(alwaysNull.apply((short) 1, (short) 2, (short) 3));
    }

    @Test
    public void test_apply_complexCalculation() {
        ShortTriFunction<Long> volumeCalculation = (length, width, height) -> (long) length * width * height;

        assertEquals(60, volumeCalculation.apply((short) 3, (short) 4, (short) 5));
        assertEquals(0, volumeCalculation.apply((short) 0, (short) 4, (short) 5));
    }
}
