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
public class IntToBooleanFunction2025Test extends TestBase {

    @Test
    public void test_DEFAULT() {
        // DEFAULT returns true if value > 0
        assertTrue(IntToBooleanFunction.DEFAULT.applyAsBoolean(1));
        assertTrue(IntToBooleanFunction.DEFAULT.applyAsBoolean(100));
        assertFalse(IntToBooleanFunction.DEFAULT.applyAsBoolean(0));
        assertFalse(IntToBooleanFunction.DEFAULT.applyAsBoolean(-1));
    }

    @Test
    public void test_applyAsBoolean() {
        IntToBooleanFunction function = value -> value > 0;

        assertTrue(function.applyAsBoolean(5));
        assertFalse(function.applyAsBoolean(-5));
        assertFalse(function.applyAsBoolean(0));
    }

    @Test
    public void test_applyAsBoolean_lambda() {
        IntToBooleanFunction isEven = value -> value % 2 == 0;

        assertTrue(isEven.applyAsBoolean(10));
        assertFalse(isEven.applyAsBoolean(11));
    }
}
