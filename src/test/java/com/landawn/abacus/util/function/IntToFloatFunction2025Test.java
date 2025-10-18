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
public class IntToFloatFunction2025Test extends TestBase {

    @Test
    public void test_DEFAULT() {
        assertEquals(100f, IntToFloatFunction.DEFAULT.applyAsFloat(100), 0.001f);
        assertEquals(0f, IntToFloatFunction.DEFAULT.applyAsFloat(0), 0.001f);
        assertEquals(-50f, IntToFloatFunction.DEFAULT.applyAsFloat(-50), 0.001f);
    }

    @Test
    public void test_applyAsFloat() {
        IntToFloatFunction function = value -> value * 1.5f;

        assertEquals(150f, function.applyAsFloat(100), 0.001f);
        assertEquals(0f, function.applyAsFloat(0), 0.001f);
    }

    @Test
    public void test_applyAsFloat_lambda() {
        IntToFloatFunction function = value -> (float) value;

        assertEquals(42.0f, function.applyAsFloat(42), 0.001f);
    }
}
