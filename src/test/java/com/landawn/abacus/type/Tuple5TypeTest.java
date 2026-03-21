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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple5;

public class Tuple5TypeTest extends TestBase {

    private final Tuple5Type type = new Tuple5Type("String", "String", "String", "String", "String");

    @Test
    public void test_valueOf_String() {
        // Test with null
        Object result = type.valueOf((String) null);
        // Result may be null or default value depending on type
        assertNull(result);
    }

    @Test
    public void test_valueOf_Empty() {
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_ValidJson() {
        Tuple5<String, String, String, String, String> t = Tuple.of("a", "b", "c", "d", "e");
        String json = type.stringOf(t);
        @SuppressWarnings("unchecked")
        Tuple5<String, String, String, String, String> result = (Tuple5<String, String, String, String, String>) type.valueOf(json);
        assertNotNull(result);
        assertEquals("a", result._1);
        assertEquals("e", result._5);
    }

    @Test
    public void test_appendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void test_appendTo_WithStringBuilder() throws IOException {
        Tuple5<String, String, String, String, String> t = Tuple.of("a", "b", "c", "d", "e");
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, t);
        String result = sb.toString();
        assertNotNull(result);
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
    }

    @Test
    public void test_appendTo_WithWriter() throws IOException {
        Tuple5<String, String, String, String, String> t = Tuple.of("a", "b", "c", "d", "e");
        StringWriter sw = new StringWriter();
        type.appendTo(sw, t);
        String result = sw.toString();
        assertNotNull(result);
        assertTrue(result.startsWith("["));
    }

    @Test
    public void test_writeCharacter_Null() throws IOException {
        var writer = Objectory.createBufferedJsonWriter();
        assertDoesNotThrow(() -> type.writeCharacter(writer, null, null));
    }

    @Test
    public void test_writeCharacter_NonNull() throws IOException {
        Tuple5<String, String, String, String, String> t = Tuple.of("a", "b", "c", "d", "e");
        var writer = Objectory.createBufferedJsonWriter();
        assertDoesNotThrow(() -> type.writeCharacter(writer, t, null));
    }

    @Test
    public void test_name() {
        assertNotNull(type.name());
        assertFalse(type.name().isEmpty());
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.get(rs, "col"));
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        // Basic set test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.set(stmt, "param", null));
    }
}
