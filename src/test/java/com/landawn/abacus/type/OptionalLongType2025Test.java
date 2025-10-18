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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalLong;

@Tag("2025")
public class OptionalLongType2025Test extends TestBase {

    private final OptionalLongType type = new OptionalLongType();

    @Test
    public void test_clazz() {
        assertEquals(OptionalLong.class, type.clazz());
    }

    @Test
    public void test_name() {
        assertEquals("OptionalLong", type.name());
    }

    @Test
    public void test_stringOf() {
        assertEquals("123456", type.stringOf(OptionalLong.of(123456L)));
        assertNull(type.stringOf(OptionalLong.empty()));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(OptionalLong.of(123456L), type.valueOf("123456"));
        assertEquals(OptionalLong.empty(), type.valueOf(""));
        assertEquals(OptionalLong.empty(), type.valueOf(null));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getObject(1)).thenReturn(999999L);
        assertEquals(OptionalLong.of(999999L), type.get(rs, 1));

        when(rs.getObject(2)).thenReturn(null);
        assertEquals(OptionalLong.empty(), type.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getObject("longCol")).thenReturn(777777L);
        assertEquals(OptionalLong.of(777777L), type.get(rs, "longCol"));

        when(rs.getObject("nullCol")).thenReturn(null);
        assertEquals(OptionalLong.empty(), type.get(rs, "nullCol"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, OptionalLong.of(888888L));
        verify(stmt).setLong(1, 888888L);

        type.set(stmt, 2, OptionalLong.empty());
        verify(stmt).setNull(2, java.sql.Types.BIGINT);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "param1", OptionalLong.of(555555L));
        verify(stmt).setLong("param1", 555555L);

        type.set(stmt, "param2", OptionalLong.empty());
        verify(stmt).setNull("param2", java.sql.Types.BIGINT);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        type.appendTo(sw, OptionalLong.of(12345L));
        assertEquals("12345", sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, OptionalLong.empty());
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_defaultValue() {
        assertEquals(OptionalLong.empty(), type.defaultValue());
    }
}
