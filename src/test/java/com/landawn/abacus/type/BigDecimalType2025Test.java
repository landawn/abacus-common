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

import java.io.StringWriter;
import java.math.BigDecimal;
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

@Tag("2025")
public class BigDecimalType2025Test extends TestBase {

    private final BigDecimalType type = new BigDecimalType();

    @Test
    public void test_clazz() {
        assertEquals(BigDecimal.class, type.clazz());
    }

    @Test
    public void test_name() {
        assertEquals("BigDecimal", type.name());
    }

    @Test
    public void test_stringOf() {
        assertEquals("123.456", new BigDecimal("123.456").toString());
        assertEquals("123.456", type.stringOf(new BigDecimal("123.456")));
        assertEquals("0", type.stringOf(BigDecimal.ZERO));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(new BigDecimal("123.456"), type.valueOf("123.456"));
        assertEquals(new BigDecimal("-789.012"), type.valueOf("-789.012"));
        assertEquals(BigDecimal.ZERO, type.valueOf("0"));
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_charArray() {
        char[] chars = "999.999".toCharArray();
        assertEquals(new BigDecimal("999.999"), type.valueOf(chars, 0, 7));

        char[] negChars = "-555.555".toCharArray();
        assertEquals(new BigDecimal("-555.555"), type.valueOf(negChars, 0, 8));

        assertNull(type.valueOf(chars, 0, 0));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with BigDecimal
        BigDecimal value = new BigDecimal("12345.6789");
        when(rs.getBigDecimal(1)).thenReturn(value);
        assertEquals(value, type.get(rs, 1));

        // Test with null
        when(rs.getBigDecimal(2)).thenReturn(null);
        assertNull(type.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with BigDecimal
        BigDecimal value = new BigDecimal("98765.4321");
        when(rs.getBigDecimal("decimalCol")).thenReturn(value);
        assertEquals(value, type.get(rs, "decimalCol"));

        // Test with null
        when(rs.getBigDecimal("nullCol")).thenReturn(null);
        assertNull(type.get(rs, "nullCol"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        // Test with value
        BigDecimal value = new BigDecimal("777.888");
        type.set(stmt, 1, value);
        verify(stmt).setBigDecimal(1, value);

        // Test with null
        type.set(stmt, 2, null);
        verify(stmt).setBigDecimal(2, null);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        // Test with value
        BigDecimal value = new BigDecimal("333.444");
        type.set(stmt, "param1", value);
        verify(stmt).setBigDecimal("param1", value);

        // Test with null
        type.set(stmt, "param2", null);
        verify(stmt).setBigDecimal("param2", null);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        // Test value
        BigDecimal value = new BigDecimal("111.222");
        type.appendTo(sw, value);
        assertEquals("111.222", sw.toString());

        // Test null
        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_writeCharacter_withoutConfig() throws Exception {
        CharacterWriter writer = mock(BufferedJSONWriter.class);
        BigDecimal value = new BigDecimal("123.456");

        type.writeCharacter(writer, value, null);
        verify(writer).writeCharacter(value.toString());
    }

    @Test
    public void test_writeCharacter_withConfig_asPlain() throws Exception {
        CharacterWriter writer = mock(BufferedJSONWriter.class);
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.writeBigDecimalAsPlain()).thenReturn(true);

        BigDecimal value = new BigDecimal("1.23E+3");
        type.writeCharacter(writer, value, config);
        verify(writer).writeCharacter(value.toPlainString());
    }

    @Test
    public void test_writeCharacter_null() throws Exception {
        CharacterWriter writer = mock(BufferedJSONWriter.class);

        type.writeCharacter(writer, null, null);
        verify(writer).write("null".toCharArray());
    }

    @Test
    public void test_defaultValue() {
        assertNull(type.defaultValue());
    }
}
