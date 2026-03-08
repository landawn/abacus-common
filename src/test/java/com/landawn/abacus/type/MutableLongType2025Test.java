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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.MutableLong;

@Tag("2025")
public class MutableLongType2025Test extends TestBase {

    private final MutableLongType type = new MutableLongType();

    @Test
    public void test_get_PreservesZeroValue() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getLong(1)).thenReturn(0L);
        when(rs.wasNull()).thenReturn(false);

        MutableLong result = type.get(rs, 1);
        assertNotNull(result);
        assertEquals(0L, result.value());
    }

    @Test
    public void test_get_ReturnsNullWhenJdbcValueIsNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getLong("col")).thenReturn(0L);
        when(rs.wasNull()).thenReturn(true);

        assertNull(type.get(rs, "col"));
    }

    @Test
    public void test_set_PreparedStatement_NullUsesSqlNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, null);

        verify(stmt).setNull(1, Types.BIGINT);
    }

    @Test
    public void test_set_CallableStatement_NullUsesSqlNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "param", null);

        verify(stmt).setNull("param", Types.BIGINT);
    }
}
