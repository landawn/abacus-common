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
public class IntToByteFunction2025Test extends TestBase {

    @Test
    public void test_DEFAULT() {
        assertEquals((byte) 100, IntToByteFunction.DEFAULT.applyAsByte(100));
        assertEquals((byte) 0, IntToByteFunction.DEFAULT.applyAsByte(0));
        assertEquals((byte) -50, IntToByteFunction.DEFAULT.applyAsByte(-50));
    }

    @Test
    public void test_applyAsByte() {
        IntToByteFunction function = value -> (byte) (value * 2);

        assertEquals((byte) 20, function.applyAsByte(10));
        assertEquals((byte) 0, function.applyAsByte(0));
    }

    @Test
    public void test_applyAsByte_lambda() {
        IntToByteFunction function = value -> (byte) value;

        assertEquals((byte) 42, function.applyAsByte(42));
        assertEquals((byte) 127, function.applyAsByte(127));
    }
}
