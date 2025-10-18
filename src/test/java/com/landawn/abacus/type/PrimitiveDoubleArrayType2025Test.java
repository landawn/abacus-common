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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.BufferedJSONWriter;
import com.landawn.abacus.util.CharacterWriter;

@Tag("2025")
public class PrimitiveDoubleArrayType2025Test extends TestBase {

    private final PrimitiveDoubleArrayType type = new PrimitiveDoubleArrayType();

    @Test
    public void test_clazz() {
        assertEquals(double[].class, type.clazz());
    }

    @Test
    public void test_name() {
        assertNotNull(type.name());
    }

    @Test
    public void test_isPrimitiveArray() {
        assertTrue(type.isPrimitiveArray());
    }

    @Test
    public void test_stringOf() {
        double[] arr = new double[] { 1.1, 2.2 };
        String result = type.stringOf(arr);
        assertNotNull(result);

        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        double[] result = type.valueOf("[1.1, 2.2]");
        assertNotNull(result);

        assertNull(type.valueOf((String) null));
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        double[] arr = new double[] { 1.1, 2.2 };
        type.appendTo(sw, arr);
        assertNotNull(sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_writeCharacter() throws IOException {
        CharacterWriter writer = mock(BufferedJSONWriter.class);
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        double[] arr = new double[] { 1.1, 2.2 };
        type.writeCharacter(writer, arr, config);
        verify(writer, atLeastOnce()).write(any(String.class));

        reset(writer);
        type.writeCharacter(writer, null, config);
        verify(writer).write(NULL_CHAR_ARRAY);
    }

    @Test
    public void test_defaultValue() {
        assertNull(type.defaultValue());
    }
}
