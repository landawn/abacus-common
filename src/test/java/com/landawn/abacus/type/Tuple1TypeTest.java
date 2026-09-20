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
import static org.mockito.Mockito.mock;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Tuple1TypeTest extends TestBase {

    private final Tuple1Type type = new Tuple1Type("String");

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

    @SuppressWarnings("unchecked")
    private static String reviewFixes20260906_ser(final Type<?> type, final Object value, final com.landawn.abacus.parser.JsonXmlSerConfig<?> config)
            throws java.io.IOException {
        final com.landawn.abacus.util.BufferedJsonWriter jsonWriter = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();

        try {
            ((Type<Object>) type).serializeTo(jsonWriter, value, config);
            return jsonWriter.toString();
        } finally {
            com.landawn.abacus.util.Objectory.recycle(jsonWriter);
        }
    }

    // T6-01 (2026-09-06): an Object slot dispatches on the runtime class; a non-serializable handler writes embedded JSON.
    @SuppressWarnings("unchecked")
    @Test
    public void reviewFixes20260906_objectSlotUsesRuntimeTypeAndEmbeddedJson() throws java.io.IOException {
        final Type<Object> objType = (Type<Object>) Type.of("Tuple1<Object>");
        final com.landawn.abacus.parser.JsonSerConfig jsc = com.landawn.abacus.parser.JsonSerConfig.create();

        org.junit.jupiter.api.Assertions.assertEquals("[1]", reviewFixes20260906_ser(objType, com.landawn.abacus.util.Tuple.of(1), jsc));
        org.junit.jupiter.api.Assertions.assertEquals("[[1]]",
                reviewFixes20260906_ser(objType, com.landawn.abacus.util.Tuple.of(com.landawn.abacus.util.N.asList(1)), jsc));
        org.junit.jupiter.api.Assertions.assertEquals("[{\"k\": 1}]",
                reviewFixes20260906_ser(objType, com.landawn.abacus.util.Tuple.of(com.landawn.abacus.util.N.asMap("k", 1)), jsc));
        org.junit.jupiter.api.Assertions.assertEquals("[\"s\"]", reviewFixes20260906_ser(objType, com.landawn.abacus.util.Tuple.of("s"), jsc));
        org.junit.jupiter.api.Assertions.assertEquals("[null]", reviewFixes20260906_ser(objType, com.landawn.abacus.util.Tuple.of((Object) null), jsc));
        org.junit.jupiter.api.Assertions.assertEquals("[1]", reviewFixes20260906_ser(objType, com.landawn.abacus.util.Tuple.of(1), null));
        org.junit.jupiter.api.Assertions.assertEquals(objType.stringOf(com.landawn.abacus.util.Tuple.of(com.landawn.abacus.util.N.asList(1))),
                reviewFixes20260906_ser(objType, com.landawn.abacus.util.Tuple.of(com.landawn.abacus.util.N.asList(1)), jsc));
        org.junit.jupiter.api.Assertions.assertEquals("[[1]]",
                com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asList(com.landawn.abacus.util.Tuple.of(1))));
        // declared slots keep the declared handler; a declared null Integer slot honours writeNullNumberAsZero
        org.junit.jupiter.api.Assertions.assertEquals("[\"s\"]", reviewFixes20260906_ser(type, com.landawn.abacus.util.Tuple.of("s"), jsc));
        org.junit.jupiter.api.Assertions.assertEquals("[0]", reviewFixes20260906_ser(Type.of("Tuple1<Integer>"),
                com.landawn.abacus.util.Tuple.of((Integer) null), com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullNumberAsZero(true)));
        org.junit.jupiter.api.Assertions.assertEquals("[null]",
                reviewFixes20260906_ser(Type.of("Tuple1<Integer>"), com.landawn.abacus.util.Tuple.of((Integer) null), jsc));
    }
}
