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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Duration;

@Tag("2025")
public class DurationType2025Test extends TestBase {

    private final DurationType type = new DurationType();

    @Test
    public void test_clazz() {
        assertEquals(Duration.class, type.clazz());
    }

    @Test
    public void test_name() {
        assertEquals("Duration", type.name());
    }

    @Test
    public void test_isComparable() {
        assertTrue(type.isComparable());
    }

    @Test
    public void test_stringOf() {
        Duration duration = Duration.ofMillis(5000);
        assertNotNull(type.stringOf(duration));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong(1)).thenReturn(5000L);

        Duration result = type.get(rs, 1);
        assertNotNull(result);
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("duration")).thenReturn(10000L);

        Duration result = type.get(rs, "duration");
        assertNotNull(result);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        Duration duration = Duration.ofMillis(5000);
        type.set(stmt, "param", duration);
        verify(stmt).setLong(eq("param"), anyLong());

        type.set(stmt, "param2", null);
        verify(stmt).setLong(eq("param2"), anyLong());
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        Duration duration = Duration.ofMillis(5000);
        type.appendTo(sw, duration);
        assertNotNull(sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

}
