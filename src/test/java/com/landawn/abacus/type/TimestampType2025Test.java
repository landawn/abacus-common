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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class TimestampType2025Test extends TestBase {

    private final TimestampType type = new TimestampType();

    @Test
    public void test_clazz() {
        assertEquals(Timestamp.class, type.clazz());
    }

    @Test
    public void test_name() {
        assertEquals("Timestamp", type.name());
    }

    @Test
    public void test_valueOf_Object_Number() {
        long millis = System.currentTimeMillis();
        Timestamp result = type.valueOf(millis);
        assertEquals(millis, result.getTime());
    }

    @Test
    public void test_valueOf_Object_JavaUtilDate() {
        java.util.Date utilDate = new java.util.Date();
        Timestamp result = type.valueOf(utilDate);
        assertEquals(utilDate.getTime(), result.getTime());
    }

    @Test
    public void test_valueOf_Object_Null() {
        assertNull(type.valueOf((Object) null));
    }

    @Test
    public void test_valueOf_String_SysTime() {
        Timestamp result = type.valueOf("sysTime");
        assertNotNull(result);
        assertTrue(Math.abs(System.currentTimeMillis() - result.getTime()) < 1000);
    }

    @Test
    public void test_valueOf_String_Null() {
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_charArray() {
        long millis = System.currentTimeMillis();
        char[] chars = String.valueOf(millis).toCharArray();
        Timestamp result = type.valueOf(chars, 0, chars.length);
        assertNotNull(result);

        assertNull(type.valueOf((char[]) null, 0, 0));
        assertNull(type.valueOf(chars, 0, 0));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        when(rs.getTimestamp(1)).thenReturn(timestamp);
        assertEquals(timestamp, type.get(rs, 1));

        when(rs.getTimestamp(2)).thenReturn(null);
        assertNull(type.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        when(rs.getTimestamp("tsCol")).thenReturn(timestamp);
        assertEquals(timestamp, type.get(rs, "tsCol"));

        when(rs.getTimestamp("nullCol")).thenReturn(null);
        assertNull(type.get(rs, "nullCol"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        type.set(stmt, 1, timestamp);
        verify(stmt).setTimestamp(1, timestamp);

        type.set(stmt, 2, null);
        verify(stmt).setTimestamp(2, null);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        type.set(stmt, "param1", timestamp);
        verify(stmt).setTimestamp("param1", timestamp);

        type.set(stmt, "param2", null);
        verify(stmt).setTimestamp("param2", null);
    }

    @Test
    public void test_defaultValue() {
        assertNull(type.defaultValue());
    }
}
