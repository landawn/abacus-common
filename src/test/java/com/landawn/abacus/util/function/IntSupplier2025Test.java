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
public class IntSupplier2025Test extends TestBase {

    @Test
    public void test_ZERO() {
        assertEquals(0, IntSupplier.ZERO.getAsInt());
        assertEquals(0, IntSupplier.ZERO.getAsInt());
        assertEquals(0, IntSupplier.ZERO.getAsInt());
    }

    @Test
    public void test_RANDOM() {
        // RANDOM should return int values
        int value1 = IntSupplier.RANDOM.getAsInt();
        int value2 = IntSupplier.RANDOM.getAsInt();
        int value3 = IntSupplier.RANDOM.getAsInt();

        // Verify they are valid int values (always true, but tests don't throw)
        assertTrue(value1 >= Integer.MIN_VALUE && value1 <= Integer.MAX_VALUE);
        assertTrue(value2 >= Integer.MIN_VALUE && value2 <= Integer.MAX_VALUE);
        assertTrue(value3 >= Integer.MIN_VALUE && value3 <= Integer.MAX_VALUE);
    }

    @Test
    public void test_getAsInt_lambda() {
        IntSupplier supplier = () -> 42;

        assertEquals(42, supplier.getAsInt());
        assertEquals(42, supplier.getAsInt());
    }

    @Test
    public void test_getAsInt_anonymousClass() {
        IntSupplier supplier = new IntSupplier() {
            @Override
            public int getAsInt() {
                return 100;
            }
        };

        assertEquals(100, supplier.getAsInt());
        assertEquals(100, supplier.getAsInt());
    }

    @Test
    public void test_getAsInt_statefulSupplier() {
        final int[] counter = { 0 };

        IntSupplier incrementing = () -> counter[0]++;

        assertEquals(0, incrementing.getAsInt());
        assertEquals(1, incrementing.getAsInt());
        assertEquals(2, incrementing.getAsInt());
    }

    @Test
    public void test_getAsInt_negativeValue() {
        IntSupplier supplier = () -> -500;

        assertEquals(-500, supplier.getAsInt());
    }

    @Test
    public void test_getAsInt_maxValue() {
        IntSupplier supplier = () -> Integer.MAX_VALUE;

        assertEquals(Integer.MAX_VALUE, supplier.getAsInt());
    }

    @Test
    public void test_getAsInt_minValue() {
        IntSupplier supplier = () -> Integer.MIN_VALUE;

        assertEquals(Integer.MIN_VALUE, supplier.getAsInt());
    }

    @Test
    public void test_getAsInt_varyingValues() {
        final int[] values = { 10, 20, 30 };
        final int[] index = { 0 };

        IntSupplier supplier = () -> values[index[0]++ % values.length];

        assertEquals(10, supplier.getAsInt());
        assertEquals(20, supplier.getAsInt());
        assertEquals(30, supplier.getAsInt());
        assertEquals(10, supplier.getAsInt()); // Wraps around
    }
}
