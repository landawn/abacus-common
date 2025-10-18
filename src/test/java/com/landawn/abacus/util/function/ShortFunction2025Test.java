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
public class ShortFunction2025Test extends TestBase {

    @Test
    public void test_BOX() {
        Short result = ShortFunction.BOX.apply((short) 42);
        assertEquals(Short.valueOf((short) 42), result);

        result = ShortFunction.BOX.apply((short) 0);
        assertEquals(Short.valueOf((short) 0), result);

        result = ShortFunction.BOX.apply(Short.MAX_VALUE);
        assertEquals(Short.valueOf(Short.MAX_VALUE), result);
    }

    @Test
    public void test_apply() {
        ShortFunction<String> toString = value -> String.valueOf(value);

        assertEquals("100", toString.apply((short) 100));
        assertEquals("-50", toString.apply((short) -50));
        assertEquals("0", toString.apply((short) 0));
    }

    @Test
    public void test_apply_lambda() {
        ShortFunction<Integer> doubleValue = value -> (int) (value * 2);

        assertEquals(20, doubleValue.apply((short) 10));
        assertEquals(-20, doubleValue.apply((short) -10));
        assertEquals(0, doubleValue.apply((short) 0));
    }

    @Test
    public void test_apply_methodReference() {
        ShortFunction<String> toString = String::valueOf;

        assertEquals("42", toString.apply((short) 42));
        assertEquals("-7", toString.apply((short) -7));
    }

    @Test
    public void test_apply_anonymousClass() {
        ShortFunction<Boolean> isPositive = new ShortFunction<Boolean>() {
            @Override
            public Boolean apply(short value) {
                return value > 0;
            }
        };

        assertTrue(isPositive.apply((short) 5));
        assertFalse(isPositive.apply((short) -5));
        assertFalse(isPositive.apply((short) 0));
    }

    @Test
    public void test_andThen() {
        ShortFunction<Integer> toInt = value -> (int) value;
        java.util.function.Function<Integer, String> intToString = i -> "Value: " + i;

        ShortFunction<String> combined = toInt.andThen(intToString);

        assertEquals("Value: 10", combined.apply((short) 10));
        assertEquals("Value: -5", combined.apply((short) -5));
    }

    @Test
    public void test_andThen_multipleChaining() {
        ShortFunction<Integer> toInt = value -> (int) value;
        java.util.function.Function<Integer, Double> intToDouble = i -> i * 1.5;
        java.util.function.Function<Double, String> doubleToString = d -> String.format("%.1f", d);

        ShortFunction<String> combined = toInt.andThen(intToDouble).andThen(doubleToString);

        assertEquals("15.0", combined.apply((short) 10));
        assertEquals("30.0", combined.apply((short) 20));
    }

    @Test
    public void test_identity() {
        ShortFunction<Short> identity = ShortFunction.identity();

        Short result = identity.apply((short) 42);
        assertEquals(Short.valueOf((short) 42), result);

        result = identity.apply((short) -100);
        assertEquals(Short.valueOf((short) -100), result);

        result = identity.apply((short) 0);
        assertEquals(Short.valueOf((short) 0), result);
    }

    @Test
    public void test_identity_boxingBehavior() {
        ShortFunction<Short> identity = ShortFunction.identity();

        // Test that it properly boxes the value
        Short result1 = identity.apply((short) 127);
        Short result2 = identity.apply((short) 127);

        // For values in the cache range, should be same object
        assertSame(result1, result2);
    }

    @Test
    public void test_apply_withMaxValue() {
        ShortFunction<Long> toLong = value -> (long) value;

        assertEquals((long) Short.MAX_VALUE, toLong.apply(Short.MAX_VALUE));
    }

    @Test
    public void test_apply_withMinValue() {
        ShortFunction<Long> toLong = value -> (long) value;

        assertEquals((long) Short.MIN_VALUE, toLong.apply(Short.MIN_VALUE));
    }

    @Test
    public void test_returnsNull() {
        ShortFunction<String> alwaysNull = value -> null;

        assertNull(alwaysNull.apply((short) 100));
        assertNull(alwaysNull.apply((short) 0));
    }
}
