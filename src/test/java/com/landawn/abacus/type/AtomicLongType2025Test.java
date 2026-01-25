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
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;

@Tag("2025")
public class AtomicLongType2025Test extends TestBase {

    private final AtomicLongType type = new AtomicLongType();

    @Test
    public void test_clazz() {
        assertEquals(AtomicLong.class, type.clazz());
    }

    @Test
    public void test_name() {
        assertEquals("AtomicLong", type.name());
    }

    @Test
    public void test_stringOf() {
        assertEquals("123456789", type.stringOf(new AtomicLong(123456789L)));
        assertEquals("-987654321", type.stringOf(new AtomicLong(-987654321L)));
        assertEquals("0", type.stringOf(new AtomicLong(0L)));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(123456789L, type.valueOf("123456789").get());
        assertEquals(-987654321L, type.valueOf("-987654321").get());
        assertEquals(0L, type.valueOf("0").get());
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with value
        when(rs.getLong(1)).thenReturn(999999999L);
        assertEquals(999999999L, type.get(rs, 1).get());

        // Test with zero
        when(rs.getLong(2)).thenReturn(0L);
        assertEquals(0L, type.get(rs, 2).get());
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with value
        when(rs.getLong("longCol")).thenReturn(777777777L);
        assertEquals(777777777L, type.get(rs, "longCol").get());

        // Test with negative
        when(rs.getLong("negCol")).thenReturn(-555555555L);
        assertEquals(-555555555L, type.get(rs, "negCol").get());
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        // Test with value
        type.set(stmt, 1, new AtomicLong(888888888L));
        verify(stmt).setLong(1, 888888888L);

        // Test with null (defaults to 0)
        type.set(stmt, 2, null);
        verify(stmt).setLong(2, 0L);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        // Test with value
        type.set(stmt, "param1", new AtomicLong(333333333L));
        verify(stmt).setLong("param1", 333333333L);

        // Test with null
        type.set(stmt, "param2", null);
        verify(stmt).setLong("param2", 0L);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        // Test value
        type.appendTo(sw, new AtomicLong(123456L));
        assertEquals("123456", sw.toString());

        // Test null
        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_writeCharacter() throws Exception {
        CharacterWriter writer = mock(BufferedJsonWriter.class);

        // Test value
        type.writeCharacter(writer, new AtomicLong(456789L), null);
        verify(writer).write(456789L);

        // Test null
        type.writeCharacter(writer, null, null);
        verify(writer).write("null".toCharArray());
    }

    @Test
    public void test_defaultValue() {
        assertNull(type.defaultValue());
    }
}
