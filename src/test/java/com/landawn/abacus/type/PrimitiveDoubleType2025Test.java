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
public class PrimitiveDoubleType2025Test extends TestBase {

    private final PrimitiveDoubleType type = new PrimitiveDoubleType();

    @Test
    public void test_clazz() {
        assertEquals(double.class, type.clazz());
    }

    @Test
    public void test_isPrimitiveType() {
        assertTrue(type.isPrimitiveType());
    }

    @Test
    public void test_name() {
        assertEquals("double", type.name());
    }

    @Test
    public void test_defaultValue() {
        assertEquals(Double.valueOf(0.0), type.defaultValue());
    }

    @Test
    public void test_stringOf() {
        assertEquals("123.456", type.stringOf(123.456));
        assertEquals("-123.456", type.stringOf(-123.456));
        assertEquals("0.0", type.stringOf(0.0));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(Double.valueOf(123.456), type.valueOf("123.456"));
        assertEquals(Double.valueOf(-123.456), type.valueOf("-123.456"));
        assertEquals(Double.valueOf(0.0), type.valueOf("0.0"));

        assertEquals(0.0, type.valueOf((String) null));
        assertEquals(0.0, type.valueOf(""));
    }

    @Test
    public void test_valueOf_charArray() {
        char[] chars = "123.456".toCharArray();
        assertEquals(Double.valueOf(123.456), type.valueOf(chars, 0, 7));

        char[] negChars = "-99.99".toCharArray();
        assertEquals(Double.valueOf(-99.99), type.valueOf(negChars, 0, 6));

        assertEquals(0.0, type.valueOf((char[]) null, 0, 0));
        assertEquals(0.0, type.valueOf(chars, 0, 0));
    }

    @Test
    public void test_valueOf_Object_Double() {
        assertEquals(Double.valueOf(99.99), type.valueOf(Double.valueOf(99.99)));
        assertEquals(0.0, type.valueOf((Object) null));
    }

    @Test
    public void test_valueOf_Object_Number() {
        assertEquals(Double.valueOf(100.0), type.valueOf(100));
        assertEquals(Double.valueOf(42.5), type.valueOf(42.5f));
    }

    @Test
    public void test_valueOf_Object_String() {
        assertEquals(Double.valueOf(77.77), type.valueOf((Object) "77.77"));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getDouble(1)).thenReturn(123.456);
        assertEquals(Double.valueOf(123.456), type.get(rs, 1));

        when(rs.getDouble(2)).thenReturn(0.0);
        assertEquals(Double.valueOf(0.0), type.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getDouble("price")).thenReturn(99.99);
        assertEquals(Double.valueOf(99.99), type.get(rs, "price"));

        when(rs.getDouble("rate")).thenReturn(-50.5);
        assertEquals(Double.valueOf(-50.5), type.get(rs, "rate"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, 123.456);
        verify(stmt).setDouble(1, 123.456);

        type.set(stmt, 2, null);
        verify(stmt).setNull(2, java.sql.Types.DOUBLE);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "param1", 99.99);
        verify(stmt).setDouble("param1", 99.99);

        type.set(stmt, "param2", null);
        verify(stmt).setNull("param2", java.sql.Types.DOUBLE);
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        type.appendTo(sw, 123.456);
        assertEquals("123.456", sw.toString());

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
