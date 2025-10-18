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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class URLType2025Test extends TestBase {

    private final URLType type = new URLType();

    @Test
    public void test_clazz() {
        assertEquals(URL.class, type.clazz());
    }

    @Test
    public void test_name() {
        assertEquals("URL", type.name());
    }

    @Test
    public void test_stringOf() throws Exception {
        URL url = new URL("https://example.com/path");
        assertEquals("https://example.com/path", type.stringOf(url));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() throws Exception {
        URL result = type.valueOf("https://example.com");
        assertNotNull(result);
        assertEquals("https://example.com", result.toString());

        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_String_InvalidURL() {
        assertThrows(RuntimeException.class, () -> {
            type.valueOf("not a valid url");
        });
    }

    @Test
    public void test_get_ResultSet_byIndex() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        URL url = new URL("https://example.com");

        when(rs.getURL(1)).thenReturn(url);
        assertEquals(url, type.get(rs, 1));

        when(rs.getURL(2)).thenReturn(null);
        assertNull(type.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        URL url = new URL("https://test.com");

        when(rs.getURL("urlCol")).thenReturn(url);
        assertEquals(url, type.get(rs, "urlCol"));

        when(rs.getURL("nullCol")).thenReturn(null);
        assertNull(type.get(rs, "nullCol"));
    }

    @Test
    public void test_set_PreparedStatement() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);
        URL url = new URL("https://test.com");

        type.set(stmt, 1, url);
        verify(stmt).setURL(1, url);

        type.set(stmt, 2, null);
        verify(stmt).setURL(2, null);
    }

    @Test
    public void test_set_CallableStatement() throws Exception {
        CallableStatement stmt = mock(CallableStatement.class);
        URL url = new URL("https://test.com");

        type.set(stmt, "param1", url);
        verify(stmt).setURL("param1", url);

        type.set(stmt, "param2", null);
        verify(stmt).setURL("param2", null);
    }

    @Test
    public void test_defaultValue() {
        assertNull(type.defaultValue());
    }
}
