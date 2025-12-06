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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class InstantType2025Test extends TestBase {

    private final InstantType type = new InstantType();

    @Test
    public void test_clazz() {
        assertEquals(Instant.class, type.clazz());
    }

    @Test
    public void test_name() {
        assertEquals("Instant", type.name());
    }

    @Test
    public void test_stringOf() {
        Instant instant = Instant.ofEpochMilli(1609459200000L);   // 2021-01-01T00:00:00Z
        String result = type.stringOf(instant);
        assertNotNull(result);
        assertTrue(result.contains("2021-01-01"));

        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_Object_Number() {
        long millis = 1609459200000L;
        Instant result = type.valueOf(millis);
        assertNotNull(result);
        assertEquals(millis, result.toEpochMilli());
    }

    @Test
    public void test_valueOf_Object_Null() {
        assertNull(type.valueOf((Object) null));
    }

    @Test
    public void test_valueOf_String_SysTime() {
        Instant result = type.valueOf("sysTime");
        assertNotNull(result);
        assertTrue(Math.abs(Instant.now().toEpochMilli() - result.toEpochMilli()) < 1000);
    }

    @Test
    public void test_valueOf_String_Null() {
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_String_NumericMillis() {
        long millis = System.currentTimeMillis();
        Instant result = type.valueOf(String.valueOf(millis));
        assertNotNull(result);
        assertEquals(millis, result.toEpochMilli());
    }

    @Test
    public void test_valueOf_charArray() {
        long millis = System.currentTimeMillis();
        char[] chars = String.valueOf(millis).toCharArray();
        Instant result = type.valueOf(chars, 0, chars.length);
        assertNotNull(result);

        assertNull(type.valueOf((char[]) null, 0, 0));
        assertNull(type.valueOf(chars, 0, 0));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Instant instant = Instant.now();
        Timestamp timestamp = Timestamp.from(instant);

        when(rs.getTimestamp(1)).thenReturn(timestamp);
        Instant result = type.get(rs, 1);
        assertNotNull(result);
        assertEquals(instant.toEpochMilli(), result.toEpochMilli());

        when(rs.getTimestamp(2)).thenReturn(null);
        assertNull(type.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Instant instant = Instant.now();
        Timestamp timestamp = Timestamp.from(instant);

        when(rs.getTimestamp("instantCol")).thenReturn(timestamp);
        Instant result = type.get(rs, "instantCol");
        assertNotNull(result);
        assertEquals(instant.toEpochMilli(), result.toEpochMilli());

        when(rs.getTimestamp("nullCol")).thenReturn(null);
        assertNull(type.get(rs, "nullCol"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Instant instant = Instant.now();

        type.set(stmt, 1, instant);
        verify(stmt).setTimestamp(eq(1), any(Timestamp.class));

        type.set(stmt, 2, null);
        verify(stmt).setTimestamp(2, null);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Instant instant = Instant.now();

        type.set(stmt, "param1", instant);
        verify(stmt).setTimestamp(eq("param1"), any(Timestamp.class));

        type.set(stmt, "param2", null);
        verify(stmt).setTimestamp("param2", null);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringBuilder sb = new StringBuilder();
        Instant instant = Instant.ofEpochMilli(1609459200000L);

        type.appendTo(sb, instant);
        assertFalse(sb.toString().isEmpty());

        sb = new StringBuilder();
        type.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void test_defaultValue() {
        assertNull(type.defaultValue());
    }
}
