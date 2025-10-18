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
public class IntObjFunction2025Test extends TestBase {

    @Test
    public void test_apply() {
        IntObjFunction<String, String> function = (t, u) -> t + ":" + u;

        assertEquals("10:test", function.apply(10, "test"));
        assertEquals("0:value", function.apply(0, "value"));
    }

    @Test
    public void test_apply_lambda() {
        IntObjFunction<String, Integer> function = (index, value) -> index + value.length();

        assertEquals(15, function.apply(10, "hello"));
        assertEquals(10, function.apply(10, ""));
    }

    @Test
    public void test_andThen() {
        IntObjFunction<String, String> concat = (t, u) -> t + ":" + u;
        java.util.function.Function<String, String> addPrefix = s -> "Result: " + s;

        IntObjFunction<String, String> combined = concat.andThen(addPrefix);

        assertEquals("Result: 5:test", combined.apply(5, "test"));
    }
}
