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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;

public class PrimitiveBooleanArrayTypeTest extends TestBase {

    private final PrimitiveBooleanArrayType type = new PrimitiveBooleanArrayType();

    @Test
    public void test_clazz() {
        assertEquals(boolean[].class, type.javaType());
    }

    @Test
    public void test_stringOf() {
        boolean[] arr = new boolean[] { true, false };
        String result = type.stringOf(arr);
        assertNotNull(result);

        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        boolean[] result = type.valueOf("[true, false]");
        assertNotNull(result);

        assertNull(type.valueOf((String) null));
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        boolean[] arr = new boolean[] { true, false };
        type.appendTo(sw, arr);
        assertNotNull(sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_serializeTo() throws IOException {
        CharacterWriter writer = mock(BufferedJsonWriter.class);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        boolean[] arr = new boolean[] { true, false };
        type.serializeTo(writer, arr, config);
        verify(writer, atLeastOnce()).write(any(String.class));

        reset(writer);
        type.serializeTo(writer, null, config);
        verify(writer).write(NULL_CHAR_ARRAY);
    }

    @Test
    public void test_isPrimitiveArray() {
        assertTrue(type.isPrimitiveArray());
    }

    @Test
    public void reviewFixes20260906_valueOfMapsNullToFalseAndRejectsUnknownTokens() {
        // documented contract (T7-05): a primitive array cannot hold null, so the literal null is false; any other token
        // that is not boolean text is rejected (code review 2026-10-05; it used to be false)
        assertArrayEquals(new boolean[] { false }, type.valueOf("[null]"));
        assertArrayEquals(new boolean[] { true, false }, type.valueOf("[1, null]"));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("[NULL]"));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("[yes]"));
        assertArrayEquals(new boolean[] { true, false, false, true, true }, type.valueOf("[Y, n, -1, y, TRUE]"));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("[Y, n, T, y, TRUE]"));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("[1, null, yes]"));
        assertArrayEquals(new boolean[] { true, false }, type.valueOf(new Boolean[] { true, null }));
        assertArrayEquals(new boolean[] { true, false, true }, type.valueOf(type.stringOf(new boolean[] { true, false, true })));
        assertEquals(0, type.valueOf("[]").length);
        assertNull(type.valueOf((String) null));
    }

    @Test
    public void reviewFixes20260906_collectionToArrayRejectsNullAndForeignElementsAndHashCodeOfNullIsZero() {
        assertThrows(NullPointerException.class, () -> type.collectionToArray(Arrays.asList((Boolean) null)));
        assertThrows(NullPointerException.class, () -> type.collectionToArray(Arrays.asList(true, null)));
        assertThrows(ClassCastException.class, () -> type.collectionToArray(Arrays.asList("zzz")));
        assertThrows(ClassCastException.class, () -> type.collectionToArray(Arrays.asList(true, 1)));
        assertArrayEquals(new boolean[] { true, false }, type.collectionToArray(Arrays.asList(true, false)));
        assertEquals(0, type.collectionToArray(Arrays.asList()).length);
        assertNull(type.collectionToArray(null));

        assertEquals(0, type.hashCode(null));
        assertEquals(Arrays.hashCode(new boolean[] { true, false }), type.hashCode(new boolean[] { true, false }));
    }

    // 2026-10-05: the compact JSON text form. Quoted content is decoded exactly once by the shared
    // AbstractArrayType.jsonElementContent; a bare null and an empty or blank quoted element are the default, false.
    @Test
    public void compactJsonTextForm20261005() {
        assertArrayEquals(new boolean[] { true, true, false, true, false, true }, type.valueOf("[\"true\", 'Y', \"0\", \"\\u0031\", null, '\\t1']"));
        assertArrayEquals(new boolean[] { false, false, false }, type.valueOf("[\"\", '', \" \"]"));
        assertArrayEquals(new boolean[] { true, false }, type.valueOf("[\"1\", '-1']"));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("[\"null\"]"));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("[\"yes\"]"));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("[\"\\\\u0031\"]"));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("[\"1']"));
    }
}
