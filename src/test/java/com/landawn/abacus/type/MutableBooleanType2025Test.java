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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
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
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.BufferedJSONWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.MutableBoolean;

@Tag("2025")
public class MutableBooleanType2025Test extends TestBase {

    private final MutableBooleanType type = new MutableBooleanType();

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getBoolean(1)).thenReturn(true);
        MutableBoolean result = type.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.value());

        when(rs.getBoolean(2)).thenReturn(false);
        result = type.get(rs, 2);
        assertNotNull(result);
        assertFalse(result.value());
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getBoolean("active")).thenReturn(true);
        MutableBoolean result = type.get(rs, "active");
        assertNotNull(result);
        assertTrue(result.value());

        when(rs.getBoolean("deleted")).thenReturn(false);
        result = type.get(rs, "deleted");
        assertNotNull(result);
        assertFalse(result.value());
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, MutableBoolean.of(true));
        verify(stmt).setBoolean(1, true);

        type.set(stmt, 2, MutableBoolean.of(false));
        verify(stmt).setBoolean(2, false);

        type.set(stmt, 3, null);
        verify(stmt).setBoolean(3, false);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "param1", MutableBoolean.of(true));
        verify(stmt).setBoolean("param1", true);

        type.set(stmt, "param2", MutableBoolean.of(false));
        verify(stmt).setBoolean("param2", false);

        type.set(stmt, "param3", null);
        verify(stmt).setBoolean("param3", false);
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        type.appendTo(sw, MutableBoolean.of(true));
        assertEquals("true", sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, MutableBoolean.of(false));
        assertEquals("false", sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_writeCharacter() throws IOException {
        CharacterWriter writer = mock(BufferedJSONWriter.class);
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        type.writeCharacter(writer, MutableBoolean.of(true), config);
        verify(writer).write(TRUE_CHAR_ARRAY);

        reset(writer);
        type.writeCharacter(writer, MutableBoolean.of(false), config);
        verify(writer).write(FALSE_CHAR_ARRAY);

        reset(writer);
        type.writeCharacter(writer, null, config);
        verify(writer).write(NULL_CHAR_ARRAY);
    }
}
