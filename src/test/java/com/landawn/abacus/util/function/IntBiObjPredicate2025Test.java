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
public class IntBiObjPredicate2025Test extends TestBase {

    @Test
    public void test_test() {
        IntBiObjPredicate<String, String> predicate = (t, u, v) -> t == (u.length() + v.length());

        assertTrue(predicate.test(5, "ab", "cde"));
        assertFalse(predicate.test(3, "ab", "cde"));
    }

    @Test
    public void test_test_lambda() {
        IntBiObjPredicate<Integer, Integer> predicate = (t, u, v) -> (t + u + v) > 10;

        assertTrue(predicate.test(5, 3, 4));
        assertFalse(predicate.test(1, 2, 3));
    }

    @Test
    public void test_negate() {
        IntBiObjPredicate<String, String> predicate = (t, u, v) -> t > 0;
        IntBiObjPredicate<String, String> negated = predicate.negate();

        assertTrue(predicate.test(1, "A", "B"));
        assertFalse(negated.test(1, "A", "B"));
    }

    @Test
    public void test_and() {
        IntBiObjPredicate<String, String> pred1 = (t, u, v) -> t > 0;
        IntBiObjPredicate<String, String> pred2 = (t, u, v) -> u != null && v != null;
        IntBiObjPredicate<String, String> combined = pred1.and(pred2);

        assertTrue(combined.test(1, "A", "B"));
        assertFalse(combined.test(0, "A", "B"));
        assertFalse(combined.test(1, null, "B"));
    }

    @Test
    public void test_or() {
        IntBiObjPredicate<String, String> pred1 = (t, u, v) -> t == 0;
        IntBiObjPredicate<String, String> pred2 = (t, u, v) -> u == null;
        IntBiObjPredicate<String, String> combined = pred1.or(pred2);

        assertTrue(combined.test(0, "A", "B"));
        assertTrue(combined.test(1, null, "B"));
        assertFalse(combined.test(1, "A", "B"));
    }
}
