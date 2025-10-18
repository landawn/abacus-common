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
public class IntToCharFunction2025Test extends TestBase {

    @Test
    public void test_DEFAULT() {
        assertEquals((char) 65, IntToCharFunction.DEFAULT.applyAsChar(65));
        assertEquals((char) 0, IntToCharFunction.DEFAULT.applyAsChar(0));
    }

    @Test
    public void test_applyAsChar() {
        IntToCharFunction function = value -> (char) value;

        assertEquals('A', function.applyAsChar(65));
        assertEquals('Z', function.applyAsChar(90));
    }

    @Test
    public void test_applyAsChar_lambda() {
        IntToCharFunction function = value -> (char) (value + 32);

        assertEquals('a', function.applyAsChar(65)); // 'A' + 32 = 'a'
    }
}
