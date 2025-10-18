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

package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ZonedDateTimeType2025Test extends TestBase {

    private final ZonedDateTimeType type = new ZonedDateTimeType();

    @Test
    public void test_clazz() {
        assertNotNull(type.clazz());
    }

    @Test
    public void test_name() {
        assertNotNull(type.name());
        assertFalse(type.name().isEmpty());
    }

    @Test
    public void test_stringOf() {
        // Test with null
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        // Test with null
        Object result = type.valueOf((String) null);
        // Result may be null or default value depending on type
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();
        type.appendTo(sw, null);
        assertNotNull(sw.toString());
    }
}
