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

import static org.junit.Assert.assertThrows;
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
public class PrimitiveByteType2025Test extends TestBase {

    private final PrimitiveByteType type = new PrimitiveByteType();

    @Test
    public void test_clazz() {
        assertEquals(byte.class, type.clazz());
    }

    @Test
    public void test_isPrimitiveType() {
        assertTrue(type.isPrimitiveType());
    }

    @Test
    public void test_name() {
        assertEquals("byte", type.name());
    }

    @Test
    public void test_defaultValue() {
        assertEquals(Byte.valueOf((byte) 0), type.defaultValue());
    }

    @Test
    public void test_stringOf() {
        assertEquals("123", type.stringOf((byte) 123));
        assertEquals("-128", type.stringOf(Byte.MIN_VALUE));
        assertEquals("127", type.stringOf(Byte.MAX_VALUE));
        assertEquals("0", type.stringOf((byte) 0));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(Byte.valueOf((byte) 123), type.valueOf("123"));
        assertEquals(Byte.valueOf((byte) -128), type.valueOf("-128"));
        assertEquals(Byte.valueOf((byte) 127), type.valueOf("127"));
        assertEquals(Byte.valueOf((byte) 0), type.valueOf("0"));

        assertEquals((byte) 0, type.valueOf((String) null));
        assertEquals((byte) 0, type.valueOf(""));
    }

    @Test
    public void test_valueOf_charArray() {
        char[] chars = "123".toCharArray();
        assertEquals(Byte.valueOf((byte) 123), type.valueOf(chars, 0, 3));

        char[] negChars = "-128".toCharArray();
        assertEquals(Byte.valueOf((byte) -128), type.valueOf(negChars, 0, 4));

        assertEquals((byte) 0, type.valueOf((char[]) null, 0, 0));
        assertEquals((byte) 0, type.valueOf(chars, 0, 0));
    }

    @Test
    public void test_valueOf_Object_Byte() {
        assertEquals(Byte.valueOf((byte) 50), type.valueOf(Byte.valueOf((byte) 50)));
        assertEquals((byte) 0, type.valueOf((Object) null));
    }

    @Test
    public void test_valueOf_Object_Number() {
        assertEquals(Byte.valueOf((byte) 100), type.valueOf(100));
        assertEquals(Byte.valueOf((byte) 42), type.valueOf(42L));
        assertThrows(NumberFormatException.class, () -> type.valueOf(10.5));
    }

    @Test
    public void test_valueOf_Object_String() {
        assertEquals(Byte.valueOf((byte) 99), type.valueOf((Object) "99"));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getByte(1)).thenReturn((byte) 100);
        assertEquals(Byte.valueOf((byte) 100), type.get(rs, 1));

        when(rs.getByte(2)).thenReturn((byte) 0);
        when(rs.wasNull()).thenReturn(false);
        assertEquals(Byte.valueOf((byte) 0), type.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getByte("age")).thenReturn((byte) 25);
        assertEquals(Byte.valueOf((byte) 25), type.get(rs, "age"));

        when(rs.getByte("status")).thenReturn((byte) 1);
        assertEquals(Byte.valueOf((byte) 1), type.get(rs, "status"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, (byte) 100);
        verify(stmt).setByte(1, (byte) 100);

        type.set(stmt, 2, null);
        verify(stmt).setNull(2, java.sql.Types.TINYINT);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "param1", (byte) 50);
        verify(stmt).setByte("param1", (byte) 50);

        type.set(stmt, "param2", null);
        verify(stmt).setNull("param2", java.sql.Types.TINYINT);
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        type.appendTo(sw, (byte) 123);
        assertEquals("123", sw.toString());

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
