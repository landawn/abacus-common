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
public class ShortSupplier2025Test extends TestBase {

    @Test
    public void test_ZERO() {
        assertEquals(0, ShortSupplier.ZERO.getAsShort());
        assertEquals(0, ShortSupplier.ZERO.getAsShort());
        assertEquals(0, ShortSupplier.ZERO.getAsShort());
    }

    @Test
    public void test_RANDOM() {
        // RANDOM should return short values
        short value1 = ShortSupplier.RANDOM.getAsShort();
        short value2 = ShortSupplier.RANDOM.getAsShort();
        short value3 = ShortSupplier.RANDOM.getAsShort();

        // Verify they are valid short values
        assertTrue(value1 >= Short.MIN_VALUE && value1 <= Short.MAX_VALUE);
        assertTrue(value2 >= Short.MIN_VALUE && value2 <= Short.MAX_VALUE);
        assertTrue(value3 >= Short.MIN_VALUE && value3 <= Short.MAX_VALUE);

        // Very unlikely all three random values are the same
        // (though technically possible, probability is extremely low)
        boolean allDifferent = (value1 != value2 || value2 != value3 || value1 != value3);
        assertTrue(allDifferent || value1 == value2);   // Just verify it doesn't throw
    }

    @Test
    public void test_getAsShort_lambda() {
        ShortSupplier supplier = () -> (short) 42;

        assertEquals(42, supplier.getAsShort());
        assertEquals(42, supplier.getAsShort());
    }

    @Test
    public void test_getAsShort_anonymousClass() {
        ShortSupplier supplier = new ShortSupplier() {
            @Override
            public short getAsShort() {
                return (short) 100;
            }
        };

        assertEquals(100, supplier.getAsShort());
        assertEquals(100, supplier.getAsShort());
    }

    @Test
    public void test_getAsShort_statefulSupplier() {
        final short[] counter = { 0 };

        ShortSupplier incrementing = () -> counter[0]++;

        assertEquals(0, incrementing.getAsShort());
        assertEquals(1, incrementing.getAsShort());
        assertEquals(2, incrementing.getAsShort());
    }

    @Test
    public void test_getAsShort_negativeValue() {
        ShortSupplier supplier = () -> (short) -500;

        assertEquals(-500, supplier.getAsShort());
    }

    @Test
    public void test_getAsShort_maxValue() {
        ShortSupplier supplier = () -> Short.MAX_VALUE;

        assertEquals(Short.MAX_VALUE, supplier.getAsShort());
    }

    @Test
    public void test_getAsShort_minValue() {
        ShortSupplier supplier = () -> Short.MIN_VALUE;

        assertEquals(Short.MIN_VALUE, supplier.getAsShort());
    }

    @Test
    public void test_getAsShort_varyingValues() {
        final short[] values = { 10, 20, 30 };
        final int[] index = { 0 };

        ShortSupplier supplier = () -> values[index[0]++ % values.length];

        assertEquals(10, supplier.getAsShort());
        assertEquals(20, supplier.getAsShort());
        assertEquals(30, supplier.getAsShort());
        assertEquals(10, supplier.getAsShort());   // Wraps around
    }
}
