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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class IntMapMultiConsumer2025Test extends TestBase {

    @Test
    public void test_accept() {
        final List<Integer> results = new ArrayList<>();

        IntMapMultiConsumer consumer = (value, action) -> {
            action.accept(value);
            action.accept(value * 2);
        };

        consumer.accept(5, results::add);

        assertEquals(2, results.size());
        assertEquals(5, results.get(0));
        assertEquals(10, results.get(1));
    }

    @Test
    public void test_accept_lambda() {
        final List<Integer> results = new ArrayList<>();

        IntMapMultiConsumer consumer = (value, action) -> {
            action.accept(value);
            action.accept(value + 1);
        };

        consumer.accept(10, results::add);

        assertEquals(2, results.size());
        assertEquals(10, results.get(0));
        assertEquals(11, results.get(1));
    }

    @Test
    public void test_accept_noOutput() {
        final List<Integer> results = new ArrayList<>();

        IntMapMultiConsumer consumer = (value, action) -> {
            // Don't call action
        };

        consumer.accept(5, results::add);

        assertEquals(0, results.size());
    }

    @Test
    public void test_accept_multipleOutputs() {
        final List<Integer> results = new ArrayList<>();

        IntMapMultiConsumer consumer = (value, action) -> {
            for (int i = 0; i < value; i++) {
                action.accept(i);
            }
        };

        consumer.accept(3, results::add);

        assertEquals(3, results.size());
        assertEquals(0, results.get(0));
        assertEquals(1, results.get(1));
        assertEquals(2, results.get(2));
    }
}
