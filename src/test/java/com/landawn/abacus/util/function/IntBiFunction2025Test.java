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
public class IntBiFunction2025Test extends TestBase {

    @Test
    public void test_apply() {
        IntBiFunction<Long> sum = (t, u) -> (long) (t + u);

        assertEquals(15L, sum.apply(5, 10));
        assertEquals(0L, sum.apply(5, -5));
        assertEquals(-15L, sum.apply(-5, -10));
    }

    @Test
    public void test_apply_lambda() {
        IntBiFunction<String> formatter = (t, u) -> String.format("(%d, %d)", t, u);

        assertEquals("(10, 20)", formatter.apply(10, 20));
        assertEquals("(0, 0)", formatter.apply(0, 0));
        assertEquals("(-5, 15)", formatter.apply(-5, 15));
    }

    @Test
    public void test_apply_anonymousClass() {
        IntBiFunction<Boolean> inRange = new IntBiFunction<Boolean>() {
            @Override
            public Boolean apply(int value, int max) {
                return value >= 0 && value <= max;
            }
        };

        assertTrue(inRange.apply(50, 100));
        assertTrue(inRange.apply(0, 100));
        assertFalse(inRange.apply(150, 100));
        assertFalse(inRange.apply(-10, 100));
    }

    @Test
    public void test_andThen() {
        IntBiFunction<Long> multiply = (t, u) -> (long) t * u;
        java.util.function.Function<Long, String> addPrefix = result -> "Result: " + result;

        IntBiFunction<String> combined = multiply.andThen(addPrefix);

        assertEquals("Result: 30", combined.apply(5, 6));
        assertEquals("Result: 0", combined.apply(0, 10));
    }

    @Test
    public void test_apply_returningNull() {
        IntBiFunction<String> alwaysNull = (t, u) -> null;

        assertNull(alwaysNull.apply(10, 20));
    }
}
