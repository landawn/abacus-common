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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BigIntegerType2025Test extends TestBase {

    private final BigIntegerType type = new BigIntegerType();

    @Test
    public void test_clazz() {
        assertEquals(BigInteger.class, type.clazz());
    }

    @Test
    public void test_name() {
        assertEquals("BigInteger", type.name());
    }

    @Test
    public void test_stringOf() {
        assertEquals("123456789", type.stringOf(new BigInteger("123456789")));
        assertEquals("-987654321", type.stringOf(new BigInteger("-987654321")));
        assertEquals("0", type.stringOf(BigInteger.ZERO));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(new BigInteger("123456789"), type.valueOf("123456789"));
        assertEquals(new BigInteger("-987654321"), type.valueOf("-987654321"));
        assertEquals(BigInteger.ZERO, type.valueOf("0"));
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with BigInteger
        when(rs.getString(1)).thenReturn("999999999999");
        assertEquals(new BigInteger("999999999999"), type.get(rs, 1));

        // Test with null
        when(rs.getString(2)).thenReturn(null);
        assertNull(type.get(rs, 2));

        // Test with empty string
        when(rs.getString(3)).thenReturn("");
        assertNull(type.get(rs, 3));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with BigInteger
        when(rs.getString("bigIntCol")).thenReturn("888888888888");
        assertEquals(new BigInteger("888888888888"), type.get(rs, "bigIntCol"));

        // Test with null
        when(rs.getString("nullCol")).thenReturn(null);
        assertNull(type.get(rs, "nullCol"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        // Test with value
        BigInteger value = new BigInteger("777777777777");
        type.set(stmt, 1, value);
        verify(stmt).setString(1, "777777777777");

        // Test with null
        type.set(stmt, 2, null);
        verify(stmt).setString(2, null);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        // Test with value
        BigInteger value = new BigInteger("333333333333");
        type.set(stmt, "param1", value);
        verify(stmt).setString("param1", "333333333333");

        // Test with null
        type.set(stmt, "param2", null);
        verify(stmt).setString("param2", null);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        // Test value
        BigInteger value = new BigInteger("111111111111");
        type.appendTo(sw, value);
        assertEquals("111111111111", sw.toString());

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
