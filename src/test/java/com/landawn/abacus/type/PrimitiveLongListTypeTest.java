package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.LongList;

public class PrimitiveLongListTypeTest extends TestBase {

    private final PrimitiveLongListType type = new PrimitiveLongListType();

    @Test
    public void testClazz() {
        assertEquals(LongList.class, type.javaType());
    }

    @Test
    public void testStringOfEmptyList() {
        LongList list = LongList.of(new long[0]);
        assertEquals("[]", type.stringOf(list));
    }

    @Test
    public void testStringOfNonEmptyList() {
        LongList list = LongList.of(new long[] { 1L, 2L, 3L });
        assertEquals("[1, 2, 3]", type.stringOf(list));
    }

    @Test
    public void testValueOfEmptyString() {
        assertNull(type.valueOf(""));
    }

    @Test
    public void testValueOfEmptyArray() {
        LongList result = type.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testValueOfNonEmptyArray() {
        LongList result = type.valueOf("[1, 2, 3]");
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
        assertEquals(2L, result.get(1));
        assertEquals(3L, result.get(2));
    }

    @Test
    public void testAppendToEmptyList() throws IOException {
        StringBuilder sb = new StringBuilder();
        LongList list = LongList.of(new long[0]);
        type.appendTo(sb, list);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToNonEmptyList() throws IOException {
        StringBuilder sb = new StringBuilder();
        LongList list = LongList.of(new long[] { 1L, 2L, 3L });
        type.appendTo(sb, list);
        assertEquals("[1, 2, 3]", sb.toString());
    }

    @Test
    public void testWriteCharacterEmptyList() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        LongList list = LongList.of(new long[0]);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        type.writeCharacter(writer, list, config);
        verify(writer).write('[');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterNonEmptyList() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        LongList list = LongList.of(new long[] { 1L, 2L });
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        type.writeCharacter(writer, list, config);
        verify(writer).write('[');
        verify(writer).write(1L);
        verify(writer).write(", ");
        verify(writer).write(2L);
        verify(writer).write(']');
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
