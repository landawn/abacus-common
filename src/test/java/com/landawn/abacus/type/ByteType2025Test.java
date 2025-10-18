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

import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ByteType2025Test extends TestBase {

    private final ByteType type = new ByteType();

    @Test
    public void test_clazz() {
        assertEquals(Byte.class, type.clazz());
    }

    @Test
    public void test_isPrimitiveWrapper() {
        assertTrue(type.isPrimitiveWrapper());
    }

    @Test
    public void test_name() {
        assertEquals("Byte", type.name());
    }

    @Test
    public void test_stringOf() {
        assertEquals("10", type.stringOf((byte) 10));
        assertEquals("-5", type.stringOf((byte) -5));
        assertEquals("0", type.stringOf((byte) 0));
        assertEquals("127", type.stringOf(Byte.MAX_VALUE));
        assertEquals("-128", type.stringOf(Byte.MIN_VALUE));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals((byte) 10, type.valueOf("10"));
        assertEquals((byte) -5, type.valueOf("-5"));
        assertEquals((byte) 0, type.valueOf("0"));
        assertEquals(Byte.MAX_VALUE, type.valueOf("127"));
        assertEquals(Byte.MIN_VALUE, type.valueOf("-128"));
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_Object() {
        // Byte input
        assertEquals((byte) 10, type.valueOf(Byte.valueOf((byte) 10)));

        // Number inputs
        assertEquals((byte) 5, type.valueOf(Integer.valueOf(5)));
        assertEquals((byte) 20, type.valueOf(Long.valueOf(20L)));

        // String input
        assertEquals((byte) 15, type.valueOf("15"));

        // Null input
        assertNull(type.valueOf((Object) null));
    }

    @Test
    public void test_valueOf_charArray() {
        char[] chars = "42".toCharArray();
        assertEquals((byte) 42, type.valueOf(chars, 0, 2));

        char[] negChars = "-10".toCharArray();
        assertEquals((byte) -10, type.valueOf(negChars, 0, 3));

        // With offset
        char[] offsetChars = "xx25yy".toCharArray();
        assertEquals((byte) 25, type.valueOf(offsetChars, 2, 2));

        assertNull(type.valueOf((char[]) null, 0, 0));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with null
        when(rs.getObject(1)).thenReturn(null);
        assertNull(type.get(rs, 1));

        // Test with Byte
        when(rs.getObject(2)).thenReturn((byte) 42);
        assertEquals((byte) 42, type.get(rs, 2));

        // Test with Number (Integer)
        when(rs.getObject(3)).thenReturn(100);
        assertEquals((byte) 100, type.get(rs, 3));

        // Test with String
        when(rs.getObject(4)).thenReturn("50");
        assertEquals((byte) 50, type.get(rs, 4));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with null
        when(rs.getObject("nullCol")).thenReturn(null);
        assertNull(type.get(rs, "nullCol"));

        // Test with Byte
        when(rs.getObject("byteCol")).thenReturn((byte) 15);
        assertEquals((byte) 15, type.get(rs, "byteCol"));

        // Test with Number
        when(rs.getObject("intCol")).thenReturn(75);
        assertEquals((byte) 75, type.get(rs, "intCol"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        // Test with value
        type.set(stmt, 1, (byte) 20);
        verify(stmt).setByte(1, (byte) 20);

        // Test with null
        type.set(stmt, 2, null);
        verify(stmt).setNull(2, java.sql.Types.TINYINT);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        // Test with value
        type.set(stmt, "param1", (byte) 30);
        verify(stmt).setByte("param1", (byte) 30);

        // Test with null
        type.set(stmt, "param2", null);
        verify(stmt).setNull("param2", java.sql.Types.TINYINT);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        // Test value
        type.appendTo(sw, (byte) 25);
        assertEquals("25", sw.toString());

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
