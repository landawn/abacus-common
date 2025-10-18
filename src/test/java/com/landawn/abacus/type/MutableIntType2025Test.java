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
import com.landawn.abacus.util.MutableInt;

@Tag("2025")
public class MutableIntType2025Test extends TestBase {

    private final MutableIntType type = new MutableIntType();

    @Test
    public void test_clazz() {
        assertEquals(MutableInt.class, type.clazz());
    }

    @Test
    public void test_name() {
        assertEquals("MutableInt", type.name());
    }

    @Test
    public void test_stringOf() {
        assertNotNull(type.stringOf(MutableInt.of(123)));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        MutableInt result = type.valueOf("123");
        assertNotNull(result);
        assertEquals(123, result.value());

        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getInt(1)).thenReturn(123);
        MutableInt result = type.get(rs, 1);
        assertNotNull(result);
        assertEquals(123, result.value());
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getInt("col")).thenReturn(123);
        MutableInt result = type.get(rs, "col");
        assertNotNull(result);
        assertEquals(123, result.value());
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, MutableInt.of(123));
        verify(stmt).setInt(1, 123);

        type.set(stmt, 2, null);
        verify(stmt).setInt(2, 0);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "param", MutableInt.of(123));
        verify(stmt).setInt("param", 123);

        type.set(stmt, "param2", null);
        verify(stmt).setInt("param2", 0);
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        type.appendTo(sw, MutableInt.of(123));
        assertNotNull(sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

}
