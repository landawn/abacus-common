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
public class IntBiObjConsumer2025Test extends TestBase {

    @Test
    public void test_accept() {
        final StringBuilder result = new StringBuilder();

        IntBiObjConsumer<String, String> consumer = (t, u, v) -> {
            result.append(t).append(":").append(u).append(":").append(v);
        };

        consumer.accept(1, "A", "B");
        assertEquals("1:A:B", result.toString());
    }

    @Test
    public void test_accept_lambda() {
        final int[] sum = { 0 };

        IntBiObjConsumer<Integer, Integer> consumer = (t, u, v) -> sum[0] = t + u + v;

        consumer.accept(10, 20, 30);
        assertEquals(60, sum[0]);
    }

    @Test
    public void test_andThen() {
        final StringBuilder result = new StringBuilder();

        IntBiObjConsumer<String, String> first = (t, u, v) -> result.append(t);
        IntBiObjConsumer<String, String> second = (t, u, v) -> result.append(":").append(u).append(":").append(v);

        IntBiObjConsumer<String, String> combined = first.andThen(second);
        combined.accept(1, "X", "Y");

        assertEquals("1:X:Y", result.toString());
    }
}
