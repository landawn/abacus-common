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
public class IntObjPredicate2025Test extends TestBase {

    @Test
    public void test_test() {
        IntObjPredicate<String> predicate = (t, u) -> t == u.length();

        assertTrue(predicate.test(5, "hello"));
        assertFalse(predicate.test(3, "hello"));
    }

    @Test
    public void test_test_lambda() {
        IntObjPredicate<String> predicate = (index, value) -> index > 0 && value != null;

        assertTrue(predicate.test(1, "test"));
        assertFalse(predicate.test(0, "test"));
        assertFalse(predicate.test(1, null));
    }

    @Test
    public void test_negate() {
        IntObjPredicate<String> predicate = (t, u) -> t == u.length();
        IntObjPredicate<String> negated = predicate.negate();

        assertTrue(predicate.test(5, "hello"));
        assertFalse(negated.test(5, "hello"));
    }

    @Test
    public void test_and() {
        IntObjPredicate<String> pred1 = (t, u) -> t > 0;
        IntObjPredicate<String> pred2 = (t, u) -> u != null;
        IntObjPredicate<String> combined = pred1.and(pred2);

        assertTrue(combined.test(1, "test"));
        assertFalse(combined.test(0, "test"));
        assertFalse(combined.test(1, null));
    }

    @Test
    public void test_or() {
        IntObjPredicate<String> pred1 = (t, u) -> t == 0;
        IntObjPredicate<String> pred2 = (t, u) -> u == null;
        IntObjPredicate<String> combined = pred1.or(pred2);

        assertTrue(combined.test(0, "test"));
        assertTrue(combined.test(1, null));
        assertFalse(combined.test(1, "test"));
    }
}
