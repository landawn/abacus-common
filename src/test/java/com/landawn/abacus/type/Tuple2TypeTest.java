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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;

public class Tuple2TypeTest extends TestBase {

    private final Tuple2Type<String, String> type = new Tuple2Type("String", "String");

    @Test
    public void test_declaringName() {
        String dn = type.declaringName();
        assertNotNull(dn);
        assertTrue(dn.contains("Tuple2"));
    }

    @Test
    public void test_javaType() {
        assertEquals(Tuple2.class, type.javaType());
    }

    @Test
    public void test_parameterTypes() {
        List<Type<?>> params = type.parameterTypes();
        assertNotNull(params);
        assertEquals(2, params.size());
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
        Tuple2<String, String> t = Tuple.of("hello", "world");
        String result = type.stringOf(t);
        assertNotNull(result);
        assertTrue(result.contains("hello"));
        assertTrue(result.contains("world"));
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
    public void test_valueOf_RejectsWrongArity() {
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("[\"only-one\"]"));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("[\"one\",\"two\",\"unexpected\"]"));
    }

    @Test
    public void test_valueOf_ValidJson() {
        Tuple2<String, String> t = Tuple.of("hello", "world");
        String json = type.stringOf(t);
        @SuppressWarnings("unchecked")
        Tuple2<String, String> result = type.valueOf(json);
        assertNotNull(result);
        assertEquals("hello", result._1);
        assertEquals("world", result._2);
    }

    @Test
    public void test_valueOf_IntegerTypes() {
        Tuple2Type<Integer, Integer> intType = new Tuple2Type("Integer", "Integer");
        @SuppressWarnings("unchecked")
        Tuple2<Integer, Integer> result = intType.valueOf("[1, 2]");
        assertNotNull(result);
        assertEquals(1, result._1);
        assertEquals(2, result._2);
    }

    @Test
    public void test_appendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void test_appendTo_WithStringBuilder() throws IOException {
        Tuple2<String, String> t = Tuple.of("a", "b");
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, t);
        String result = sb.toString();
        assertNotNull(result);
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
    }

    @Test
    public void test_appendTo_WithWriter() throws IOException {
        Tuple2<String, String> t = Tuple.of("x", "y");
        StringWriter sw = new StringWriter();
        type.appendTo(sw, t);
        String result = sw.toString();
        assertNotNull(result);
        assertTrue(result.startsWith("["));
        assertTrue(result.contains("x"));
    }

    @Test
    public void test_appendTo_PropagatesWriterIOException() {
        final IOException failure = new IOException("write failure");
        final Writer writer = new Writer() {
            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                throw failure;
            }

            @Override
            public void flush() {
                // no-op
            }

            @Override
            public void close() {
                // no-op
            }
        };

        assertSame(failure, assertThrows(IOException.class, () -> type.appendTo(writer, Tuple.of("x", "y"))));
    }

    @Test
    public void test_serializeTo_Null() throws IOException {
        var writer = Objectory.createBufferedJsonWriter();
        assertDoesNotThrow(() -> type.serializeTo(writer, null, null));
    }

    @Test
    public void test_serializeTo_NonNull() throws IOException {
        Tuple2<String, String> t = Tuple.of("a", "b");
        var writer = Objectory.createBufferedJsonWriter();
        assertDoesNotThrow(() -> type.serializeTo(writer, t, null));
    }

    @Test
    public void test_name() {
        assertNotNull(type.name());
        assertFalse(type.name().isEmpty());
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        assertDoesNotThrow(() -> type.get(rs, "col"));
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        assertDoesNotThrow(() -> type.set(stmt, "param", null));
    }

    @SuppressWarnings("unchecked")
    private static String reviewFixes20260906_ser(final Type<?> type, final Object value, final com.landawn.abacus.parser.JsonXmlSerConfig<?> config) throws java.io.IOException {
        final com.landawn.abacus.util.BufferedJsonWriter jsonWriter = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();

        try {
            ((Type<Object>) type).serializeTo(jsonWriter, value, config);
            return jsonWriter.toString();
        } finally {
            com.landawn.abacus.util.Objectory.recycle(jsonWriter);
        }
    }

    // T6-01 (2026-09-06): a DECLARED bean/map slot (non-serializable handler) is written as embedded JSON, not a quoted string.
    @Test
    public void reviewFixes20260906_declaredBeanSlotWritesEmbeddedJson() throws IOException {
        final Type<?> beanTuple = Type.of("Tuple2<" + ReviewFixesBean.class.getName() + ", Integer>");
        final com.landawn.abacus.parser.JsonSerConfig jsc = com.landawn.abacus.parser.JsonSerConfig.create();
        final com.landawn.abacus.parser.JsonSerConfig zero = com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullNumberAsZero(true);

        assertEquals("[{\"a\": 1, \"s\": \"x\"}, 2]", reviewFixes20260906_ser(beanTuple, Tuple.of(new ReviewFixesBean(), 2), jsc));
        assertEquals("[{\"a\": 1, \"s\": \"x\"}, 0]", reviewFixes20260906_ser(beanTuple, Tuple.of(new ReviewFixesBean(), null), zero));
        assertEquals("[null, 2]", reviewFixes20260906_ser(beanTuple, Tuple.of(null, 2), jsc));
        assertEquals("[1, \"a\"]", reviewFixes20260906_ser(Type.of("Tuple2<Object, Object>"), Tuple.of(1, "a"), jsc));
        assertEquals("[[1, \"a\"]]", com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asList(Tuple.of(1, "a"))));

        // bean round trip through the real parser (a map value is resolved by its runtime class -> Tuple2<Object, Object>)
        final ReviewFixesHolder holder = new ReviewFixesHolder();
        final String json = com.landawn.abacus.util.N.toJson(holder);
        assertEquals("{\"tb\": [{\"a\": 1, \"s\": \"x\"}, 2], \"mt\": {\"k\": [5, \"v\"]}}", json);
        final ReviewFixesHolder back = com.landawn.abacus.util.N.fromJson(json, ReviewFixesHolder.class);
        assertEquals(Integer.valueOf(2), back.tb._2);
        assertEquals(1, back.tb._1.a);
        assertEquals("x", back.tb._1.s);
        assertEquals(Integer.valueOf(5), back.mt.get("k")._1);
        assertEquals("v", back.mt.get("k")._2);
    }

    public static class ReviewFixesBean {
        public int a = 1;
        public String s = "x";
    }

    public static class ReviewFixesHolder {
        public Tuple2<ReviewFixesBean, Integer> tb = Tuple.of(new ReviewFixesBean(), 2);
        public java.util.Map<String, Tuple2<Integer, String>> mt = com.landawn.abacus.util.N.asMap("k", Tuple.of(5, "v"));
    }
}
