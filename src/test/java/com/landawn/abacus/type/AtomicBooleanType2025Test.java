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
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AtomicBooleanType2025Test extends TestBase {

    private final AtomicBooleanType type = new AtomicBooleanType();

    @Test
    public void test_clazz() {
        assertEquals(AtomicBoolean.class, type.clazz());
    }

    @Test
    public void test_name() {
        assertEquals("AtomicBoolean", type.name());
    }

    @Test
    public void test_stringOf() {
        assertEquals("true", type.stringOf(new AtomicBoolean(true)));
        assertEquals("false", type.stringOf(new AtomicBoolean(false)));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(true, type.valueOf("true").get());
        assertEquals(false, type.valueOf("false").get());
        assertEquals(false, type.valueOf("anything").get());
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with true
        when(rs.getBoolean(1)).thenReturn(true);
        assertEquals(true, type.get(rs, 1).get());

        // Test with false
        when(rs.getBoolean(2)).thenReturn(false);
        assertEquals(false, type.get(rs, 2).get());
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with true
        when(rs.getBoolean("boolCol")).thenReturn(true);
        assertEquals(true, type.get(rs, "boolCol").get());

        // Test with false
        when(rs.getBoolean("falseCol")).thenReturn(false);
        assertEquals(false, type.get(rs, "falseCol").get());
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        // Test with true
        type.set(stmt, 1, new AtomicBoolean(true));
        verify(stmt).setBoolean(1, true);

        // Test with false
        type.set(stmt, 2, new AtomicBoolean(false));
        verify(stmt).setBoolean(2, false);

        // Test with null
        type.set(stmt, 3, null);
        verify(stmt).setBoolean(3, false);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        // Test with true
        type.set(stmt, "param1", new AtomicBoolean(true));
        verify(stmt).setBoolean("param1", true);

        // Test with null
        type.set(stmt, "param2", null);
        verify(stmt).setBoolean("param2", false);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        // Test true
        type.appendTo(sw, new AtomicBoolean(true));
        assertEquals("true", sw.toString());

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
