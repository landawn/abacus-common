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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;

@Tag("2025")
public class BooleanArrayType2025Test extends TestBase {

    private final BooleanArrayType type = new BooleanArrayType();

    @Test
    public void test_clazz() {
        assertEquals(Boolean[].class, type.clazz());
    }

    @Test
    public void test_name() {
        assertEquals("Boolean[]", type.name());
    }

    @Test
    public void test_stringOf() {
        Boolean[] arr = { true, false, null, true };
        String result = type.stringOf(arr);
        assertEquals("[true, false, null, true]", result);

        assertNull(type.stringOf(null));
        assertEquals("[]", type.stringOf(new Boolean[0]));
    }

    @Test
    public void test_valueOf_String() {
        Boolean[] result = type.valueOf("[true, false, null]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertTrue(result[0]);
        assertFalse(result[1]);
        assertNull(result[2]);

        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
        assertNotNull(type.valueOf("[]"));
        assertEquals(0, type.valueOf("[]").length);
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        Boolean[] arr = { true, false, null };
        type.appendTo(sw, arr);
        assertEquals("[true, false, null]", sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, new Boolean[0]);
        assertEquals("[]", sw.toString());
    }

    @Test
    public void test_writeCharacter() throws IOException {
        CharacterWriter writer = mock(BufferedJsonWriter.class);
        JsonXmlSerializationConfig<?> config = mock(JsonXmlSerializationConfig.class);

        Boolean[] arr = { true, false };
        type.writeCharacter(writer, arr, config);
        verify(writer, atLeast(2)).write(anyChar());

        reset(writer);
        type.writeCharacter(writer, null, config);
        verify(writer).write(NULL_CHAR_ARRAY);
    }

    @Test
    public void test_isObjectArray() {
        assertTrue(type.isObjectArray());
    }
}
