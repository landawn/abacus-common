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

import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class IntegerType2025Test extends TestBase {

    private final IntegerType type = new IntegerType();

    @Test
    public void test_clazz() {
        assertEquals(Integer.class, type.clazz());
    }

    @Test
    public void test_isPrimitiveWrapper() {
        assertTrue(type.isPrimitiveWrapper());
    }

    @Test
    public void test_name() {
        assertEquals("Integer", type.name());
    }

    @Test
    public void test_stringOf() {
        assertEquals("1000", type.stringOf(1000));
        assertEquals("-500", type.stringOf(-500));
        assertEquals("0", type.stringOf(0));
        assertEquals("2147483647", type.stringOf(Integer.MAX_VALUE));
        assertEquals("-2147483648", type.stringOf(Integer.MIN_VALUE));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(1000, type.valueOf("1000"));
        assertEquals(-500, type.valueOf("-500"));
        assertEquals(0, type.valueOf("0"));
        assertEquals(Integer.MAX_VALUE, type.valueOf("2147483647"));
        assertEquals(Integer.MIN_VALUE, type.valueOf("-2147483648"));
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_Object() {
        // Integer input
        assertEquals(2000, type.valueOf(Integer.valueOf(2000)));

        // Number inputs
        assertEquals(50, type.valueOf(Byte.valueOf((byte) 50)));
        assertEquals(300, type.valueOf(Long.valueOf(300L)));
        assertThrows(NumberFormatException.class, () -> type.valueOf(Double.valueOf(100.7)));

        // String input
        assertEquals(1500, type.valueOf("1500"));

        // Null input
        assertNull(type.valueOf((Object) null));
    }

    @Test
    public void test_valueOf_charArray() {
        char[] chars = "12345".toCharArray();
        assertEquals(12345, type.valueOf(chars, 0, 5));

        char[] negChars = "-9999".toCharArray();
        assertEquals(-9999, type.valueOf(negChars, 0, 5));

        assertNull(type.valueOf((char[]) null, 0, 0));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with null
        when(rs.getObject(1)).thenReturn(null);
        assertNull(type.get(rs, 1));

        // Test with Integer
        when(rs.getObject(2)).thenReturn(42000);
        assertEquals(42000, type.get(rs, 2));

        // Test with Number (Long)
        when(rs.getObject(3)).thenReturn(100000L);
        assertEquals(100000, type.get(rs, 3));

        // Test with String
        when(rs.getObject(4)).thenReturn("50000");
        assertEquals(50000, type.get(rs, 4));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with null
        when(rs.getObject("nullCol")).thenReturn(null);
        assertNull(type.get(rs, "nullCol"));

        // Test with Integer
        when(rs.getObject("intCol")).thenReturn(75000);
        assertEquals(75000, type.get(rs, "intCol"));

        // Test with Number
        when(rs.getObject("longCol")).thenReturn(999999L);
        assertEquals(999999, type.get(rs, "longCol"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        // Test with value
        type.set(stmt, 1, 88888);
        verify(stmt).setInt(1, 88888);

        // Test with null
        type.set(stmt, 2, null);
        verify(stmt).setNull(2, java.sql.Types.INTEGER);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        // Test with value
        type.set(stmt, "param1", 77777);
        verify(stmt).setInt("param1", 77777);

        // Test with null
        type.set(stmt, "param2", null);
        verify(stmt).setNull("param2", java.sql.Types.INTEGER);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        // Test value
        type.appendTo(sw, 66666);
        assertEquals("66666", sw.toString());

        // Test null
        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_defaultValue() {
        assertNull(type.defaultValue());
    }
}
