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
public class IntToLongFunction2025Test extends TestBase {

    @Test
    public void test_DEFAULT() {
        assertEquals(100L, IntToLongFunction.DEFAULT.applyAsLong(100));
        assertEquals(0L, IntToLongFunction.DEFAULT.applyAsLong(0));
        assertEquals(-50L, IntToLongFunction.DEFAULT.applyAsLong(-50));
        assertEquals(Integer.MAX_VALUE, IntToLongFunction.DEFAULT.applyAsLong(Integer.MAX_VALUE));
    }

    @Test
    public void test_applyAsLong() {
        IntToLongFunction function = value -> (long) value * 2;

        assertEquals(200L, function.applyAsLong(100));
        assertEquals(0L, function.applyAsLong(0));
    }

    @Test
    public void test_applyAsLong_lambda() {
        IntToLongFunction function = value -> value + 1000L;

        assertEquals(1100L, function.applyAsLong(100));
    }
}
