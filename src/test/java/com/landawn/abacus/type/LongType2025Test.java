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
public class LongType2025Test extends TestBase {

    private final LongType type = new LongType();

    @Test
    public void test_clazz() {
        assertEquals(Long.class, type.clazz());
    }

    @Test
    public void test_isPrimitiveWrapper() {
        assertTrue(type.isPrimitiveWrapper());
    }

    @Test
    public void test_name() {
        assertEquals("Long", type.name());
    }

    @Test
    public void test_stringOf() {
        assertEquals("100000", type.stringOf(100000L));
        assertEquals("-50000", type.stringOf(-50000L));
        assertEquals("0", type.stringOf(0L));
        assertEquals("9223372036854775807", type.stringOf(Long.MAX_VALUE));
        assertEquals("-9223372036854775808", type.stringOf(Long.MIN_VALUE));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(100000L, type.valueOf("100000"));
        assertEquals(-50000L, type.valueOf("-50000"));
        assertEquals(0L, type.valueOf("0"));
        assertEquals(Long.MAX_VALUE, type.valueOf("9223372036854775807"));
        assertEquals(Long.MIN_VALUE, type.valueOf("-9223372036854775808"));
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_Object() {
        // Long input
        assertEquals(200000L, type.valueOf(Long.valueOf(200000L)));

        // Number inputs
        assertEquals(50L, type.valueOf(Integer.valueOf(50)));
        assertEquals(300L, type.valueOf(Short.valueOf((short) 300)));

        // String input
        assertEquals(150000L, type.valueOf("150000"));

        // Null input
        assertNull(type.valueOf((Object) null));
    }

    @Test
    public void test_valueOf_charArray() {
        char[] chars = "123456789".toCharArray();
        assertEquals(123456789L, type.valueOf(chars, 0, 9));

        char[] negChars = "-987654321".toCharArray();
        assertEquals(-987654321L, type.valueOf(negChars, 0, 10));

        assertNull(type.valueOf((char[]) null, 0, 0));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with null
        when(rs.getObject(1)).thenReturn(null);
        assertNull(type.get(rs, 1));

        // Test with Long
        when(rs.getObject(2)).thenReturn(999999999L);
        assertEquals(999999999L, type.get(rs, 2));

        // Test with Number (Integer)
        when(rs.getObject(3)).thenReturn(100000);
        assertEquals(100000L, type.get(rs, 3));

        // Test with String
        when(rs.getObject(4)).thenReturn("5000000");
        assertEquals(5000000L, type.get(rs, 4));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with null
        when(rs.getObject("nullCol")).thenReturn(null);
        assertNull(type.get(rs, "nullCol"));

        // Test with Long
        when(rs.getObject("longCol")).thenReturn(888888888L);
        assertEquals(888888888L, type.get(rs, "longCol"));

        // Test with Number
        when(rs.getObject("intCol")).thenReturn(777777);
        assertEquals(777777L, type.get(rs, "intCol"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        // Test with value
        type.set(stmt, 1, 123456789L);
        verify(stmt).setLong(1, 123456789L);

        // Test with null
        type.set(stmt, 2, null);
        verify(stmt).setNull(2, java.sql.Types.BIGINT);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        // Test with value
        type.set(stmt, "param1", 987654321L);
        verify(stmt).setLong("param1", 987654321L);

        // Test with null
        type.set(stmt, "param2", null);
        verify(stmt).setNull("param2", java.sql.Types.BIGINT);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        // Test value
        type.appendTo(sw, 555555555L);
        assertEquals("555555555", sw.toString());

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
