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
public class IntToShortFunction2025Test extends TestBase {

    @Test
    public void test_DEFAULT() {
        assertEquals((short) 100, IntToShortFunction.DEFAULT.applyAsShort(100));
        assertEquals((short) 0, IntToShortFunction.DEFAULT.applyAsShort(0));
        assertEquals((short) -50, IntToShortFunction.DEFAULT.applyAsShort(-50));
    }

    @Test
    public void test_applyAsShort() {
        IntToShortFunction function = value -> (short) (value * 2);

        assertEquals((short) 200, function.applyAsShort(100));
        assertEquals((short) 0, function.applyAsShort(0));
    }

    @Test
    public void test_applyAsShort_lambda() {
        IntToShortFunction function = value -> (short) value;

        assertEquals((short) 1000, function.applyAsShort(1000));
    }
}
