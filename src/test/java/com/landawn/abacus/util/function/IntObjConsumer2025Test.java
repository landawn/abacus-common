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
import com.landawn.abacus.util.Pair;

@Tag("2025")
public class IntObjConsumer2025Test extends TestBase {

    @Test
    public void test_accept() {
        final Pair<Integer, String> result = Pair.of(0, "");

        IntObjConsumer<String> consumer = (t, u) -> {
            result.setLeft(t);
            result.setRight(u);
        };

        consumer.accept(42, "test");
        assertEquals(42, result.left().intValue());
        assertEquals("test", result.right());
    }

    @Test
    public void test_accept_lambda() {
        final StringBuilder sb = new StringBuilder();

        IntObjConsumer<String> consumer = (index, value) -> sb.append(index).append(":").append(value);

        consumer.accept(1, "A");
        assertEquals("1:A", sb.toString());
    }

    @Test
    public void test_andThen() {
        final Pair<Integer, String> result = Pair.of(0, "");

        IntObjConsumer<String> first = (t, u) -> result.setLeft(t);
        IntObjConsumer<String> second = (t, u) -> result.setRight(u);

        IntObjConsumer<String> combined = first.andThen(second);
        combined.accept(10, "value");

        assertEquals(10, result.left().intValue());
        assertEquals("value", result.right());
    }
}
