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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;

@Tag("2025")
public class PrimitiveCharType2025Test extends TestBase {

    private final PrimitiveCharType type = new PrimitiveCharType();

    @Test
    public void test_clazz() {
        assertEquals(char.class, type.clazz());
    }

    @Test
    public void test_isPrimitiveType() {
        assertTrue(type.isPrimitiveType());
    }

    @Test
    public void test_name() {
        assertEquals("char", type.name());
    }

    @Test
    public void test_defaultValue() {
        assertEquals(Character.valueOf((char) 0), type.defaultValue());
    }

    @Test
    public void test_stringOf() {
        assertEquals("A", type.stringOf('A'));
        assertEquals("z", type.stringOf('z'));
        assertEquals("0", type.stringOf('0'));
        assertEquals(" ", type.stringOf(' '));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(Character.valueOf('A'), type.valueOf("A"));
        assertEquals(Character.valueOf('B'), type.valueOf("B"));
        assertEquals(Character.valueOf('9'), type.valueOf("9"));

        assertEquals((char) 0, type.valueOf((String) null));
        assertEquals((char) 0, type.valueOf(""));
    }

    @Test
    public void test_valueOf_charArray() {
        char[] chars = "ABC".toCharArray();
        assertEquals(Character.valueOf('A'), type.valueOf(chars, 0, 1));
        assertEquals(Character.valueOf('B'), type.valueOf(chars, 1, 1));

        assertEquals((char) 0, type.valueOf((char[]) null, 0, 0));
        assertEquals((char) 0, type.valueOf(chars, 0, 0));
    }

    @Test
    public void test_valueOf_Object_Character() {
        assertEquals(Character.valueOf('X'), type.valueOf(Character.valueOf('X')));
        assertEquals((char) 0, type.valueOf((Object) null));
    }

    @Test
    public void test_valueOf_Object_String() {
        assertEquals(Character.valueOf('Z'), type.valueOf((Object) "Z"));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getString(1)).thenReturn("A");
        assertEquals(Character.valueOf('A'), type.get(rs, 1));

        when(rs.getString(2)).thenReturn("X");
        assertEquals(Character.valueOf('X'), type.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getString("code")).thenReturn("C");
        assertEquals(Character.valueOf('C'), type.get(rs, "code"));

        when(rs.getString("status")).thenReturn("Y");
        assertEquals(Character.valueOf('Y'), type.get(rs, "status"));
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        type.appendTo(sw, 'A');
        assertEquals("A", sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_writeCharacter() throws IOException {
        CharacterWriter writer = mock(BufferedJsonWriter.class);
        JsonXmlSerializationConfig<?> config = mock(JsonXmlSerializationConfig.class);

        type.writeCharacter(writer, 'A', config);
        verify(writer).writeCharacter('A');

        reset(writer);
        type.writeCharacter(writer, null, config);
        verify(writer).write(NULL_CHAR_ARRAY);
    }

    @Test
    public void test_isComparable() {
        assertTrue(type.isComparable());
    }
}
