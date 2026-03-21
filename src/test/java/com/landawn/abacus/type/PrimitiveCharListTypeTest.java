package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharacterWriter;

public class PrimitiveCharListTypeTest extends TestBase {

    private final PrimitiveCharListType type = new PrimitiveCharListType();

    @Test
    public void testClazz() {
        assertEquals(CharList.class, type.javaType());
    }

    @Test
    public void testStringOfEmptyList() {
        CharList list = CharList.of(new char[0]);
        assertEquals("[]", type.stringOf(list));
    }

    @Test
    public void testStringOfNonEmptyList() {
        CharList list = CharList.of(new char[] { 'a', 'b', 'c' });
        assertEquals("['a', 'b', 'c']", type.stringOf(list));
    }

    @Test
    public void test_valueOf_String() {
        // Test with null
        Object result = type.valueOf((String) null);
        // Result may be null or default value depending on type
        assertNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        assertNull(type.valueOf(""));
    }

    @Test
    public void testValueOfEmptyArray() {
        CharList result = type.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testValueOfNonEmptyArray() {
        CharList result = type.valueOf("['a', 'b', 'c']");
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals('a', result.get(0));
        assertEquals('b', result.get(1));
        assertEquals('c', result.get(2));
    }

    @Test
    public void testAppendToEmptyList() throws IOException {
        StringBuilder sb = new StringBuilder();
        CharList list = CharList.of(new char[0]);
        type.appendTo(sb, list);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToNonEmptyList() throws IOException {
        StringBuilder sb = new StringBuilder();
        CharList list = CharList.of(new char[] { 'a', 'b', 'c' });
        type.appendTo(sb, list);
        assertEquals("[a, b, c]", sb.toString());
    }

    @Test
    public void testWriteCharacterEmptyList() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        CharList list = CharList.of(new char[0]);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        type.writeCharacter(writer, list, config);
        verify(writer).write('[');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterNonEmptyList() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        CharList list = CharList.of(new char[] { 'a', 'b' });
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        type.writeCharacter(writer, list, config);
        verify(writer).write('[');
        verify(writer).writeCharacter('a');
        verify(writer).write(", ");
        verify(writer).writeCharacter('b');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterWithQuotation() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        CharList list = CharList.of(new char[] { 'a' });
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.getCharQuotation()).thenReturn('\'');

        type.writeCharacter(writer, list, config);
        verify(writer).write('[');
        verify(writer, times(2)).write('\'');
        verify(writer).writeCharacter('a');
        verify(writer).write(']');
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
