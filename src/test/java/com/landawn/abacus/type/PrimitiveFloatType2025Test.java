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

import java.io.IOException;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class PrimitiveFloatType2025Test extends TestBase {

    private final PrimitiveFloatType type = new PrimitiveFloatType();

    @Test
    public void test_clazz() {
        assertEquals(float.class, type.clazz());
    }

    @Test
    public void test_isPrimitiveType() {
        assertTrue(type.isPrimitiveType());
    }

    @Test
    public void test_name() {
        assertEquals("float", type.name());
    }

    @Test
    public void test_defaultValue() {
        assertEquals(Float.valueOf(0.0f), type.defaultValue());
    }

    @Test
    public void test_stringOf() {
        assertEquals("123.45", type.stringOf(123.45f));
        assertEquals("-123.45", type.stringOf(-123.45f));
        assertEquals("0.0", type.stringOf(0.0f));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(Float.valueOf(123.45f), type.valueOf("123.45"));
        assertEquals(Float.valueOf(-123.45f), type.valueOf("-123.45"));
        assertEquals(Float.valueOf(0.0f), type.valueOf("0.0"));

        assertEquals(0.0f, type.valueOf((String) null));
        assertEquals(0.0f, type.valueOf(""));
    }

    @Test
    public void test_valueOf_charArray() {
        char[] chars = "123.45".toCharArray();
        assertEquals(Float.valueOf(123.45f), type.valueOf(chars, 0, 6));

        char[] negChars = "-99.9".toCharArray();
        assertEquals(Float.valueOf(-99.9f), type.valueOf(negChars, 0, 5));

        assertEquals(0.0f, type.valueOf((char[]) null, 0, 0));
        assertEquals(0.0f, type.valueOf(chars, 0, 0));
    }

    @Test
    public void test_valueOf_Object_Float() {
        assertEquals(Float.valueOf(99.9f), type.valueOf(Float.valueOf(99.9f)));
        assertEquals(0.0f, type.valueOf((Object) null));
    }

    @Test
    public void test_valueOf_Object_Number() {
        assertEquals(Float.valueOf(100.0f), type.valueOf(100));
        assertEquals(Float.valueOf(42.5f), type.valueOf(42.5));
    }

    @Test
    public void test_valueOf_Object_String() {
        assertEquals(Float.valueOf(77.7f), type.valueOf((Object) "77.7"));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getFloat(1)).thenReturn(123.45f);
        assertEquals(Float.valueOf(123.45f), type.get(rs, 1));

        when(rs.getFloat(2)).thenReturn(0.0f);
        assertEquals(Float.valueOf(0.0f), type.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getFloat("price")).thenReturn(99.99f);
        assertEquals(Float.valueOf(99.99f), type.get(rs, "price"));

        when(rs.getFloat("rate")).thenReturn(-50.5f);
        assertEquals(Float.valueOf(-50.5f), type.get(rs, "rate"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, 123.45f);
        verify(stmt).setFloat(1, 123.45f);

        type.set(stmt, 2, null);
        verify(stmt).setNull(2, java.sql.Types.FLOAT);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "param1", 99.99f);
        verify(stmt).setFloat("param1", 99.99f);

        type.set(stmt, "param2", null);
        verify(stmt).setNull("param2", java.sql.Types.FLOAT);
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        type.appendTo(sw, 123.45f);
        assertEquals("123.45", sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
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
