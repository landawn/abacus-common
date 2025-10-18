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
public class ShortBiFunction2025Test extends TestBase {

    @Test
    public void test_apply() {
        ShortBiFunction<Integer> sum = (t, u) -> (int) (t + u);

        assertEquals(15, sum.apply((short) 5, (short) 10));
        assertEquals(0, sum.apply((short) 5, (short) -5));
        assertEquals(-15, sum.apply((short) -5, (short) -10));
    }

    @Test
    public void test_apply_lambda() {
        ShortBiFunction<String> formatter = (t, u) -> String.format("(%d, %d)", t, u);

        assertEquals("(10, 20)", formatter.apply((short) 10, (short) 20));
        assertEquals("(0, 0)", formatter.apply((short) 0, (short) 0));
        assertEquals("(-5, 15)", formatter.apply((short) -5, (short) 15));
    }

    @Test
    public void test_apply_anonymousClass() {
        ShortBiFunction<Boolean> inRange = new ShortBiFunction<Boolean>() {
            @Override
            public Boolean apply(short value, short max) {
                return value >= 0 && value <= max;
            }
        };

        assertTrue(inRange.apply((short) 50, (short) 100));
        assertTrue(inRange.apply((short) 0, (short) 100));
        assertFalse(inRange.apply((short) 150, (short) 100));
        assertFalse(inRange.apply((short) -10, (short) 100));
    }

    @Test
    public void test_apply_returningString() {
        ShortBiFunction<String> concatenate = (t, u) -> t + ":" + u;

        assertEquals("5:10", concatenate.apply((short) 5, (short) 10));
        assertEquals("-5:10", concatenate.apply((short) -5, (short) 10));
    }

    @Test
    public void test_apply_returningObject() {
        ShortBiFunction<java.awt.Point> createPoint = (x, y) -> new java.awt.Point(x, y);

        java.awt.Point point = createPoint.apply((short) 10, (short) 20);
        assertEquals(10, point.x);
        assertEquals(20, point.y);
    }

    @Test
    public void test_andThen() {
        ShortBiFunction<Integer> multiply = (t, u) -> (int) (t * u);
        java.util.function.Function<Integer, String> addPrefix = result -> "Result: " + result;

        ShortBiFunction<String> combined = multiply.andThen(addPrefix);

        assertEquals("Result: 30", combined.apply((short) 5, (short) 6));
        assertEquals("Result: -30", combined.apply((short) 5, (short) -6));
        assertEquals("Result: 0", combined.apply((short) 0, (short) 10));
    }

    @Test
    public void test_andThen_multipleChaining() {
        ShortBiFunction<Integer> add = (t, u) -> (int) (t + u);
        java.util.function.Function<Integer, Double> toDouble = i -> i * 1.5;
        java.util.function.Function<Double, String> format = d -> String.format("%.1f", d);

        ShortBiFunction<String> combined = add.andThen(toDouble).andThen(format);

        assertEquals("15.0", combined.apply((short) 5, (short) 5));
        assertEquals("22.5", combined.apply((short) 10, (short) 5));
    }

    @Test
    public void test_apply_withMaxValues() {
        ShortBiFunction<Long> multiplyToLong = (t, u) -> (long) t * u;

        long result = multiplyToLong.apply(Short.MAX_VALUE, (short) 2);
        assertEquals((long) Short.MAX_VALUE * 2, result);
    }

    @Test
    public void test_apply_withMinValues() {
        ShortBiFunction<Integer> add = (t, u) -> (int) (t + u);

        int result = add.apply(Short.MIN_VALUE, (short) 1);
        assertEquals((int) Short.MIN_VALUE + 1, result);
    }

    @Test
    public void test_apply_returningNull() {
        ShortBiFunction<String> alwaysNull = (t, u) -> null;

        assertNull(alwaysNull.apply((short) 10, (short) 20));
        assertNull(alwaysNull.apply((short) 0, (short) 0));
    }

    @Test
    public void test_apply_complexCalculation() {
        ShortBiFunction<Double> distance = (x, y) -> Math.sqrt(x * x + y * y);

        assertEquals(5.0, distance.apply((short) 3, (short) 4), 0.001);
        assertEquals(0.0, distance.apply((short) 0, (short) 0), 0.001);
    }
}
