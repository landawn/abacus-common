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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.net.URI;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.BufferedJSONWriter;
import com.landawn.abacus.util.CharacterWriter;

@Tag("2025")
public class URIType2025Test extends TestBase {

    private final URIType type = new URIType();

    @Test
    public void test_stringOf() throws Exception {
        URI uri = new URI("https://example.com");
        assertEquals("https://example.com", type.stringOf(uri));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() throws Exception {
        URI result = type.valueOf("https://example.com");
        assertNotNull(result);
        assertEquals("https://example.com", result.toString());

        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("https://example.com");

        URI result = type.get(rs, 1);
        assertNotNull(result);
        assertEquals("https://example.com", result.toString());

        when(rs.getString(2)).thenReturn(null);
        assertNull(type.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("uri")).thenReturn("https://example.com");

        URI result = type.get(rs, "uri");
        assertNotNull(result);
        assertEquals("https://example.com", result.toString());
    }

    @Test
    public void test_set_PreparedStatement() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);

        URI uri = new URI("https://example.com");
        type.set(stmt, 1, uri);
        verify(stmt).setString(1, "https://example.com");

        type.set(stmt, 2, null);
        verify(stmt).setString(2, null);
    }

    @Test
    public void test_set_CallableStatement() throws Exception {
        CallableStatement stmt = mock(CallableStatement.class);

        URI uri = new URI("https://example.com");
        type.set(stmt, "param", uri);
        verify(stmt).setString("param", "https://example.com");

        type.set(stmt, "param2", null);
        verify(stmt).setString("param2", null);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        URI uri = new URI("https://example.com");
        type.appendTo(sw, uri);
        assertEquals("https://example.com", sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_writeCharacter() throws Exception {
        CharacterWriter writer = mock(BufferedJSONWriter.class);
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        URI uri = new URI("https://example.com");
        type.writeCharacter(writer, uri, config);
        verify(writer).writeCharacter("https://example.com");

        reset(writer);
        type.writeCharacter(writer, null, config);
        verify(writer).write(NULL_CHAR_ARRAY);
    }
}
