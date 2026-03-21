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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.landawn.abacus.util.Tuple.Tuple9;

public class Tuple9TypeTest extends TestBase {

    private final Tuple9Type type = new Tuple9Type("String", "String", "String", "String", "String", "String", "String", "String", "String");

    @Test
    public void test_declaringName() {
        String dn = type.declaringName();
        assertNotNull(dn);
        assertTrue(dn.contains("Tuple9"));
    }

    @Test
    public void test_javaType() {
        assertEquals(Tuple9.class, type.javaType());
    }

    @Test
    public void test_parameterTypes() {
        Type<?>[] params = type.parameterTypes();
        assertNotNull(params);
        assertEquals(9, params.length);
    }

    @Test
    public void test_isParameterizedType() {
        assertTrue(type.isParameterizedType());
    }

    @Test
    public void test_stringOf_Null() {
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_stringOf_NonNull() {
        Tuple9<String, String, String, String, String, String, String, String, String> t = Tuple.of("a", "b", "c", "d", "e", "f", "g", "h", "i");
        String result = type.stringOf(t);
        assertNotNull(result);
        assertTrue(result.contains("a"));
        assertTrue(result.contains("i"));
    }

    @Test
    public void test_valueOf_Null() {
        assertNull(type.valueOf((String) null));
    }

    @Test
    public void test_valueOf_Empty() {
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_ValidJson() {
        Tuple9<String, String, String, String, String, String, String, String, String> t = Tuple.of("a", "b", "c", "d", "e", "f", "g", "h", "i");
        String json = type.stringOf(t);
        @SuppressWarnings("unchecked")
        Tuple9<String, String, String, String, String, String, String, String, String> result = (Tuple9<String, String, String, String, String, String, String, String, String>) type
                .valueOf(json);
        assertNotNull(result);
        assertEquals("a", result._1);
        assertEquals("i", result._9);
    }

    // appendTo with null tuple should write "null"
    @Test
    public void test_appendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void test_appendTo_WithAppendable() throws IOException {
        Tuple9<String, String, String, String, String, String, String, String, String> t = Tuple.of("a", "b", "c", "d", "e", "f", "g", "h", "i");
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, t);
        String result = sb.toString();
        assertNotNull(result);
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
    }

    @Test
    public void test_appendTo_WithWriter() throws IOException {
        Tuple9<String, String, String, String, String, String, String, String, String> t = Tuple.of("a", "b", "c", "d", "e", "f", "g", "h", "i");
        StringWriter sw = new StringWriter();
        type.appendTo(sw, t);
        String result = sw.toString();
        assertNotNull(result);
        assertTrue(result.startsWith("["));
    }

    // writeCharacter with null writes "null"
    @Test
    public void test_writeCharacter_Null() throws IOException {
        var writer = Objectory.createBufferedJsonWriter();
        assertDoesNotThrow(() -> type.writeCharacter(writer, null, null));
    }

    @Test
    public void test_writeCharacter_NonNull() throws IOException {
        Tuple9<String, String, String, String, String, String, String, String, String> t = Tuple.of("a", "b", "c", "d", "e", "f", "g", "h", "i");
        var writer = Objectory.createBufferedJsonWriter();
        assertDoesNotThrow(() -> type.writeCharacter(writer, t, null));
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
