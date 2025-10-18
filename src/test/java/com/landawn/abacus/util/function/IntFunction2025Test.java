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
public class IntFunction2025Test extends TestBase {

    @Test
    public void test_BOX() {
        Integer result = IntFunction.BOX.apply(42);
        assertEquals(Integer.valueOf(42), result);

        result = IntFunction.BOX.apply(0);
        assertEquals(Integer.valueOf(0), result);

        result = IntFunction.BOX.apply(Integer.MAX_VALUE);
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), result);
    }

    @Test
    public void test_apply() {
        IntFunction<String> toString = value -> String.valueOf(value);

        assertEquals("100", toString.apply(100));
        assertEquals("-50", toString.apply(-50));
        assertEquals("0", toString.apply(0));
    }

    @Test
    public void test_apply_lambda() {
        IntFunction<Long> doubleLong = value -> (long) value * 2;

        assertEquals(20L, doubleLong.apply(10));
        assertEquals(-20L, doubleLong.apply(-10));
        assertEquals(0L, doubleLong.apply(0));
    }

    @Test
    public void test_apply_anonymousClass() {
        IntFunction<Boolean> isPositive = new IntFunction<Boolean>() {
            @Override
            public Boolean apply(int value) {
                return value > 0;
            }
        };

        assertTrue(isPositive.apply(5));
        assertFalse(isPositive.apply(-5));
        assertFalse(isPositive.apply(0));
    }

    @Test
    public void test_andThen() {
        IntFunction<Integer> square = value -> value * value;
        java.util.function.Function<Integer, String> intToString = i -> "Value: " + i;

        IntFunction<String> combined = square.andThen(intToString);

        assertEquals("Value: 100", combined.apply(10));
        assertEquals("Value: 25", combined.apply(-5));
    }

    @Test
    public void test_andThen_multipleChaining() {
        IntFunction<Integer> square = value -> value * value;
        java.util.function.Function<Integer, Double> intToDouble = i -> i * 1.5;
        java.util.function.Function<Double, String> doubleToString = d -> String.format("%.1f", d);

        IntFunction<String> combined = square.andThen(intToDouble).andThen(doubleToString);

        assertEquals("150.0", combined.apply(10));
        assertEquals("600.0", combined.apply(20));
    }

    @Test
    public void test_identity() {
        IntFunction<Integer> identity = IntFunction.identity();

        Integer result = identity.apply(42);
        assertEquals(Integer.valueOf(42), result);

        result = identity.apply(-100);
        assertEquals(Integer.valueOf(-100), result);

        result = identity.apply(0);
        assertEquals(Integer.valueOf(0), result);
    }

    @Test
    public void test_identity_boxingBehavior() {
        IntFunction<Integer> identity = IntFunction.identity();

        // For values in the cache range (-128 to 127), should be same object
        Integer result1 = identity.apply(127);
        Integer result2 = identity.apply(127);
        assertSame(result1, result2);
    }

    @Test
    public void test_apply_withMaxValue() {
        IntFunction<Long> toLong = value -> (long) value;

        assertEquals((long) Integer.MAX_VALUE, toLong.apply(Integer.MAX_VALUE));
    }

    @Test
    public void test_apply_withMinValue() {
        IntFunction<Long> toLong = value -> (long) value;

        assertEquals((long) Integer.MIN_VALUE, toLong.apply(Integer.MIN_VALUE));
    }

    @Test
    public void test_returnsNull() {
        IntFunction<String> alwaysNull = value -> null;

        assertNull(alwaysNull.apply(100));
        assertNull(alwaysNull.apply(0));
    }
}
