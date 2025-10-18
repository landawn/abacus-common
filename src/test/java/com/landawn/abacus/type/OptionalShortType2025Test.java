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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalShort;

@Tag("2025")
public class OptionalShortType2025Test extends TestBase {

    private final OptionalShortType type = new OptionalShortType();

    @Test
    public void test_clazz() {
        assertEquals(OptionalShort.class, type.clazz());
    }

    @Test
    public void test_name() {
        assertEquals("OptionalShort", type.name());
    }

    @Test
    public void test_stringOf() {
        assertEquals("1234", type.stringOf(OptionalShort.of((short) 1234)));
        assertNull(type.stringOf(OptionalShort.empty()));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(OptionalShort.of((short) 1234), type.valueOf("1234"));
        assertEquals(OptionalShort.empty(), type.valueOf(""));
        assertEquals(OptionalShort.empty(), type.valueOf(null));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getObject(1)).thenReturn((short) 999);
        assertEquals(OptionalShort.of((short) 999), type.get(rs, 1));

        when(rs.getObject(2)).thenReturn(null);
        assertEquals(OptionalShort.empty(), type.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getObject("shortCol")).thenReturn((short) 777);
        assertEquals(OptionalShort.of((short) 777), type.get(rs, "shortCol"));

        when(rs.getObject("nullCol")).thenReturn(null);
        assertEquals(OptionalShort.empty(), type.get(rs, "nullCol"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, OptionalShort.of((short) 888));
        verify(stmt).setShort(1, (short) 888);

        type.set(stmt, 2, OptionalShort.empty());
        verify(stmt).setNull(2, java.sql.Types.SMALLINT);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "param1", OptionalShort.of((short) 555));
        verify(stmt).setShort("param1", (short) 555);

        type.set(stmt, "param2", OptionalShort.empty());
        verify(stmt).setNull("param2", java.sql.Types.SMALLINT);
    }

    @Test
    public void test_defaultValue() {
        assertEquals(OptionalShort.empty(), type.defaultValue());
    }
}
