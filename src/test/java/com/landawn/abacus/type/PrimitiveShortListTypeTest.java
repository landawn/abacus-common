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
import com.landawn.abacus.util.ShortList;

public class PrimitiveShortListTypeTest extends TestBase {

    private final PrimitiveShortListType type = new PrimitiveShortListType();

    @Test
    public void testClazz() {
        assertEquals(ShortList.class, type.javaType());
    }

    @Test
    public void testStringOfEmptyList() {
        ShortList list = ShortList.of(new short[0]);
        assertEquals("[]", type.stringOf(list));
    }

    @Test
    public void testStringOfNonEmptyList() {
        ShortList list = ShortList.of(new short[] { 1, 2, 3 });
        assertEquals("[1, 2, 3]", type.stringOf(list));
    }

    @Test
    public void testValueOfEmptyString() {
        assertNull(type.valueOf(""));
    }

    @Test
    public void testValueOfEmptyArray() {
        ShortList result = type.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testValueOfNonEmptyArray() {
        ShortList result = type.valueOf("[1, 2, 3]");
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals((short) 1, result.get(0));
        assertEquals((short) 2, result.get(1));
        assertEquals((short) 3, result.get(2));
    }

    @Test
    public void testAppendToEmptyList() throws IOException {
        StringBuilder sb = new StringBuilder();
        ShortList list = ShortList.of(new short[0]);
        type.appendTo(sb, list);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToNonEmptyList() throws IOException {
        StringBuilder sb = new StringBuilder();
        ShortList list = ShortList.of(new short[] { 1, 2, 3 });
        type.appendTo(sb, list);
        assertEquals("[1, 2, 3]", sb.toString());
    }

    @Test
    public void testSerializeToEmptyList() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        ShortList list = ShortList.of(new short[0]);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        type.serializeTo(writer, list, config);
        verify(writer).write('[');
        verify(writer).write(']');
    }

    @Test
    public void testSerializeToNonEmptyList() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        ShortList list = ShortList.of(new short[] { 1, 2 });
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        type.serializeTo(writer, list, config);
        verify(writer).write('[');
        verify(writer).write((short) 1);
        verify(writer).write(", ");
        verify(writer).write((short) 2);
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

    // T8-09 / T8-13 (documented): overflow is an ArithmeticException (not NumberFormatException); blank input is null.
    @Test
    public void reviewFixes20260906_valueOfDocumentedFailureModesAndBlankInput() {
        org.junit.jupiter.api.Assertions.assertThrows(ArithmeticException.class, () -> type.valueOf("[32768]"));
        org.junit.jupiter.api.Assertions.assertThrows(ArithmeticException.class, () -> type.valueOf("[-32769]"));
        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> type.valueOf("[abc]"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> type.valueOf("[1,,2]"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> type.valueOf("[1, ]"));
        assertEquals(ShortList.of((short) 32767, (short) -32768), type.valueOf("[32767, -32768]"));

        assertNull(type.valueOf("   "));
        assertNull(type.valueOf(""));
        org.junit.jupiter.api.Assertions.assertTrue(type.valueOf("[ ]").isEmpty());
    }

}
