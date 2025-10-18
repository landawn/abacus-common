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
public class IntBiObjFunction2025Test extends TestBase {

    @Test
    public void test_apply() {
        IntBiObjFunction<String, String, String> function = (t, u, v) -> t + ":" + u + ":" + v;

        assertEquals("1:A:B", function.apply(1, "A", "B"));
        assertEquals("0:X:Y", function.apply(0, "X", "Y"));
    }

    @Test
    public void test_apply_lambda() {
        IntBiObjFunction<Integer, Integer, Integer> function = (t, u, v) -> t + u + v;

        assertEquals(60, function.apply(10, 20, 30));
        assertEquals(0, function.apply(0, 0, 0));
    }

    @Test
    public void test_andThen() {
        IntBiObjFunction<String, String, String> concat = (t, u, v) -> t + ":" + u + ":" + v;
        java.util.function.Function<String, String> addPrefix = s -> "Result: " + s;

        IntBiObjFunction<String, String, String> combined = concat.andThen(addPrefix);

        assertEquals("Result: 5:A:B", combined.apply(5, "A", "B"));
    }
}
