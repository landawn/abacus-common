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
public class ShortToIntFunction2025Test extends TestBase {

    @Test
    public void test_DEFAULT() {
        assertEquals(100, ShortToIntFunction.DEFAULT.applyAsInt((short) 100));
        assertEquals(0, ShortToIntFunction.DEFAULT.applyAsInt((short) 0));
        assertEquals(-50, ShortToIntFunction.DEFAULT.applyAsInt((short) -50));
        assertEquals(Short.MAX_VALUE, ShortToIntFunction.DEFAULT.applyAsInt(Short.MAX_VALUE));
        assertEquals(Short.MIN_VALUE, ShortToIntFunction.DEFAULT.applyAsInt(Short.MIN_VALUE));
    }

    @Test
    public void test_applyAsInt() {
        ShortToIntFunction function = value -> value * 2;

        assertEquals(200, function.applyAsInt((short) 100));
        assertEquals(0, function.applyAsInt((short) 0));
        assertEquals(-100, function.applyAsInt((short) -50));
    }

    @Test
    public void test_applyAsInt_lambda() {
        ShortToIntFunction function = value -> value + 1000;

        assertEquals(1100, function.applyAsInt((short) 100));
        assertEquals(1000, function.applyAsInt((short) 0));
        assertEquals(950, function.applyAsInt((short) -50));
    }

    @Test
    public void test_applyAsInt_anonymousClass() {
        ShortToIntFunction function = new ShortToIntFunction() {
            @Override
            public int applyAsInt(short value) {
                return value * value;
            }
        };

        assertEquals(100, function.applyAsInt((short) 10));
        assertEquals(0, function.applyAsInt((short) 0));
        assertEquals(25, function.applyAsInt((short) -5));
    }

    @Test
    public void test_applyAsInt_wideningConversion() {
        ShortToIntFunction function = value -> value;

        // Test widening conversion preserves value
        assertEquals(32767, function.applyAsInt(Short.MAX_VALUE));
        assertEquals(-32768, function.applyAsInt(Short.MIN_VALUE));
        assertEquals(0, function.applyAsInt((short) 0));
    }

    @Test
    public void test_applyAsInt_withMaxValue() {
        ShortToIntFunction doubler = value -> value * 2;

        assertEquals(65534, doubler.applyAsInt(Short.MAX_VALUE));
    }

    @Test
    public void test_applyAsInt_withMinValue() {
        ShortToIntFunction doubler = value -> value * 2;

        assertEquals(-65536, doubler.applyAsInt(Short.MIN_VALUE));
    }

    @Test
    public void test_applyAsInt_withZero() {
        ShortToIntFunction multiplier = value -> value * 100;

        assertEquals(0, multiplier.applyAsInt((short) 0));
    }

    @Test
    public void test_applyAsInt_complexCalculation() {
        ShortToIntFunction complexFunction = value -> (value * 3 + 10) / 2;

        assertEquals(20, complexFunction.applyAsInt((short) 10)); // (10 * 3 + 10) / 2 = 40 / 2 = 20
        assertEquals(5, complexFunction.applyAsInt((short) 0)); // (0 * 3 + 10) / 2 = 10 / 2 = 5
    }

    @Test
    public void test_applyAsInt_absoluteValue() {
        ShortToIntFunction abs = value -> Math.abs(value);

        assertEquals(100, abs.applyAsInt((short) 100));
        assertEquals(100, abs.applyAsInt((short) -100));
        assertEquals(0, abs.applyAsInt((short) 0));
    }
}
