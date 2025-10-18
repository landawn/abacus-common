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
public class IntToDoubleFunction2025Test extends TestBase {

    @Test
    public void test_DEFAULT() {
        assertEquals(100.0, IntToDoubleFunction.DEFAULT.applyAsDouble(100), 0.001);
        assertEquals(0.0, IntToDoubleFunction.DEFAULT.applyAsDouble(0), 0.001);
        assertEquals(-50.0, IntToDoubleFunction.DEFAULT.applyAsDouble(-50), 0.001);
    }

    @Test
    public void test_applyAsDouble() {
        IntToDoubleFunction function = value -> value * 1.5;

        assertEquals(150.0, function.applyAsDouble(100), 0.001);
        assertEquals(0.0, function.applyAsDouble(0), 0.001);
    }

    @Test
    public void test_applyAsDouble_lambda() {
        IntToDoubleFunction function = value -> (double) value;

        assertEquals(42.0, function.applyAsDouble(42), 0.001);
    }
}
