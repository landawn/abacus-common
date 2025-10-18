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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.BufferedJSONWriter;
import com.landawn.abacus.util.CharacterWriter;

@Tag("2025")
public class StringType2025Test extends TestBase {

    private final StringType type = new StringType();

    @Test
    public void test_clazz() {
        assertEquals(String.class, type.clazz());
    }

    @Test
    public void test_isString() {
        assertTrue(type.isString());
    }

    @Test
    public void test_name() {
        assertEquals("String", type.name());
    }

    @Test
    public void test_stringOf() {
        assertEquals("Hello", type.stringOf("Hello"));
        assertEquals("", type.stringOf(""));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals("Test", type.valueOf("Test"));
        assertEquals("", type.valueOf(""));
        assertNull(type.valueOf((String) null));
    }

    @Test
    public void test_valueOf_charArray() {
        char[] chars = "Hello World".toCharArray();
        assertEquals("Hello World", type.valueOf(chars, 0, 11));

        char[] offsetChars = "xxTestyy".toCharArray();
        assertEquals("Test", type.valueOf(offsetChars, 2, 4));

        assertEquals("", type.valueOf(chars, 0, 0));
        assertNull(type.valueOf((char[]) null, 0, 0));
    }

    @Test
    public void test_valueOf_Object_String() {
        assertEquals("Test", type.valueOf((Object) "Test"));
        assertNull(type.valueOf((Object) null));
    }

    @Test
    public void test_valueOf_Object_Reader() {
        StringReader reader = new StringReader("Reader Content");
        String result = type.valueOf(reader);
        assertEquals("Reader Content", result);
    }

    @Test
    public void test_valueOf_Object_Clob() throws SQLException {
        Clob clob = mock(Clob.class);
        when(clob.length()).thenReturn(10L);
        when(clob.getSubString(1, 10)).thenReturn("Clob Value");

        String result = type.valueOf(clob);
        assertEquals("Clob Value", result);

        verify(clob).free();
    }

    @Test
    public void test_valueOf_Object_Other() {
        // For other types, it should use their type's stringOf
        assertEquals("123", type.valueOf(123));
        assertEquals("true", type.valueOf(true));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with string
        when(rs.getString(1)).thenReturn("Value1");
        assertEquals("Value1", type.get(rs, 1));

        // Test with null
        when(rs.getString(2)).thenReturn(null);
        assertNull(type.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with string
        when(rs.getString("column1")).thenReturn("Value2");
        assertEquals("Value2", type.get(rs, "column1"));

        // Test with null
        when(rs.getString("nullCol")).thenReturn(null);
        assertNull(type.get(rs, "nullCol"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        // Test with value
        type.set(stmt, 1, "TestValue");
        verify(stmt).setString(1, "TestValue");

        // Test with null
        type.set(stmt, 2, null);
        verify(stmt).setString(2, null);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        // Test with value
        type.set(stmt, "param1", "TestValue");
        verify(stmt).setString("param1", "TestValue");

        // Test with null
        type.set(stmt, "param2", null);
        verify(stmt).setString("param2", null);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        // Test value
        type.appendTo(sw, "Hello");
        assertEquals("Hello", sw.toString());

        // Test null
        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_writeCharacter_withoutConfig() throws Exception {
        CharacterWriter writer = mock(BufferedJSONWriter.class);

        // Test value
        type.writeCharacter(writer, "Test", null);
        verify(writer).writeCharacter("Test");
    }

    @Test
    public void test_writeCharacter_withConfig_noQuotation() throws Exception {
        CharacterWriter writer = mock(BufferedJSONWriter.class);
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getStringQuotation()).thenReturn((char) 0);

        type.writeCharacter(writer, "Test", config);
        verify(writer).writeCharacter("Test");
    }

    @Test
    public void test_writeCharacter_null_withConfig_writeNullAsEmpty() throws Exception {
        CharacterWriter writer = mock(BufferedJSONWriter.class);
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.writeNullStringAsEmpty()).thenReturn(true);
        when(config.getStringQuotation()).thenReturn((char) 0);

        type.writeCharacter(writer, null, config);
        verify(writer).writeCharacter("");
    }

    @Test
    public void test_writeCharacter_null_withoutConfig() throws Exception {
        CharacterWriter writer = mock(BufferedJSONWriter.class);

        type.writeCharacter(writer, null, null);
        verify(writer).write("null".toCharArray());
    }

    @Test
    public void test_defaultValue() {
        assertNull(type.defaultValue());
    }
}
