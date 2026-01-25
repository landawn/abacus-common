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
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;

@Tag("2025")
public class PrimitiveBooleanType2025Test extends TestBase {

    private final PrimitiveBooleanType type = new PrimitiveBooleanType();

    @Test
    public void test_clazz() {
        assertEquals(boolean.class, type.clazz());
    }

    @Test
    public void test_isPrimitiveType() {
        assertTrue(type.isPrimitiveType());
    }

    @Test
    public void test_isBoolean() {
        assertTrue(type.isBoolean());
    }

    @Test
    public void test_name() {
        assertEquals("boolean", type.name());
    }

    @Test
    public void test_defaultValue() {
        assertEquals(Boolean.FALSE, type.defaultValue());
    }

    @Test
    public void test_stringOf() {
        assertEquals("true", type.stringOf(true));
        assertEquals("false", type.stringOf(false));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(Boolean.TRUE, type.valueOf("true"));
        assertEquals(Boolean.TRUE, type.valueOf("TRUE"));
        assertEquals(Boolean.TRUE, type.valueOf("True"));
        assertEquals(Boolean.TRUE, type.valueOf("Y"));
        assertEquals(Boolean.TRUE, type.valueOf("y"));
        assertEquals(Boolean.TRUE, type.valueOf("1"));

        assertEquals(Boolean.FALSE, type.valueOf("false"));
        assertEquals(Boolean.FALSE, type.valueOf("FALSE"));
        assertEquals(Boolean.FALSE, type.valueOf("False"));
        assertEquals(Boolean.FALSE, type.valueOf("N"));
        assertEquals(Boolean.FALSE, type.valueOf("0"));
        assertEquals(Boolean.FALSE, type.valueOf("anything"));

        assertEquals(Boolean.FALSE, type.valueOf((String) null));
        assertEquals(Boolean.FALSE, type.valueOf(""));
    }

    @Test
    public void test_valueOf_charArray() {
        char[] trueChars = "true".toCharArray();
        assertEquals(Boolean.TRUE, type.valueOf(trueChars, 0, 4));

        char[] falseChars = "false".toCharArray();
        assertEquals(Boolean.FALSE, type.valueOf(falseChars, 0, 5));

        char[] TrueChars = "TRUE".toCharArray();
        assertEquals(Boolean.TRUE, type.valueOf(TrueChars, 0, 4));

        assertEquals(Boolean.FALSE, type.valueOf((char[]) null, 0, 0));
        assertEquals(Boolean.FALSE, type.valueOf(trueChars, 0, 0));
    }

    @Test
    public void test_valueOf_Object_Boolean() {
        assertEquals(Boolean.TRUE, type.valueOf(Boolean.TRUE));
        assertEquals(Boolean.FALSE, type.valueOf(Boolean.FALSE));
        assertEquals(Boolean.FALSE, type.valueOf((Object) null));
    }

    @Test
    public void test_valueOf_Object_Number() {
        assertEquals(Boolean.TRUE, type.valueOf(1));
        assertEquals(Boolean.TRUE, type.valueOf(100));
        assertEquals(Boolean.TRUE, type.valueOf(1L));
        assertEquals(Boolean.FALSE, type.valueOf(0));
        assertEquals(Boolean.FALSE, type.valueOf(-1));
    }

    @Test
    public void test_valueOf_Object_String() {
        assertEquals(Boolean.TRUE, type.valueOf((Object) "true"));
        assertEquals(Boolean.TRUE, type.valueOf((Object) "Y"));
        assertEquals(Boolean.FALSE, type.valueOf((Object) "false"));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getBoolean(1)).thenReturn(true);
        assertEquals(Boolean.TRUE, type.get(rs, 1));

        when(rs.getBoolean(2)).thenReturn(false);
        assertEquals(Boolean.FALSE, type.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getBoolean("active")).thenReturn(true);
        assertEquals(Boolean.TRUE, type.get(rs, "active"));

        when(rs.getBoolean("deleted")).thenReturn(false);
        assertEquals(Boolean.FALSE, type.get(rs, "deleted"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, true);
        verify(stmt).setBoolean(1, true);

        type.set(stmt, 2, false);
        verify(stmt).setBoolean(2, false);

        type.set(stmt, 3, null);
        verify(stmt).setNull(3, java.sql.Types.BOOLEAN);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "param1", true);
        verify(stmt).setBoolean("param1", true);

        type.set(stmt, "param2", false);
        verify(stmt).setBoolean("param2", false);

        type.set(stmt, "param3", null);
        verify(stmt).setNull("param3", java.sql.Types.BOOLEAN);
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        type.appendTo(sw, true);
        assertEquals("true", sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, false);
        assertEquals("false", sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_writeCharacter_withoutConfig() throws IOException {
        CharacterWriter writer = mock(BufferedJsonWriter.class);

        type.writeCharacter(writer, true, null);
        verify(writer).write(TRUE_CHAR_ARRAY);

        reset(writer);
        type.writeCharacter(writer, false, null);
        verify(writer).write(FALSE_CHAR_ARRAY);

        reset(writer);
        type.writeCharacter(writer, null, null);
        verify(writer).write(NULL_CHAR_ARRAY);
    }

    @Test
    public void test_writeCharacter_withConfig_writeNullBooleanAsFalse() throws IOException {
        CharacterWriter writer = mock(BufferedJsonWriter.class);
        JsonXmlSerializationConfig<?> config = mock(JsonXmlSerializationConfig.class);
        when(config.writeNullBooleanAsFalse()).thenReturn(true);

        type.writeCharacter(writer, null, config);
        verify(writer).write(FALSE_CHAR_ARRAY);
    }

    @Test
    public void test_isNonQuotableCsvType() {
        assertTrue(type.isNonQuotableCsvType());
    }

    @Test
    public void test_isComparable() {
        assertTrue(type.isComparable());
    }
}
