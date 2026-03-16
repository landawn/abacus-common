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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.FloatList;

@Tag("2025")
public class PrimitiveFloatListTypeTest extends TestBase {

    private final PrimitiveFloatListType type = new PrimitiveFloatListType();

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

    @Test
    public void testClazz() {
        assertEquals(FloatList.class, type.javaType());
    }

    @Test
    public void testStringOfEmptyList() {
        FloatList list = FloatList.of(new float[0]);
        assertEquals("[]", type.stringOf(list));
    }

    @Test
    public void testStringOfNonEmptyList() {
        FloatList list = FloatList.of(new float[] { 1.5f, 2.7f, 3.14f });
        assertEquals("[1.5, 2.7, 3.14]", type.stringOf(list));
    }

    @Test
    public void testValueOfEmptyString() {
        assertNull(type.valueOf(""));
    }

    @Test
    public void testValueOfEmptyArray() {
        FloatList result = type.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testValueOfNonEmptyArray() {
        FloatList result = type.valueOf("[1.5, 2.7, 3.14]");
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(1.5f, result.get(0));
        assertEquals(2.7f, result.get(1));
        assertEquals(3.14f, result.get(2));
    }

    @Test
    public void testAppendToEmptyList() throws IOException {
        StringBuilder sb = new StringBuilder();
        FloatList list = FloatList.of(new float[0]);
        type.appendTo(sb, list);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToNonEmptyList() throws IOException {
        StringBuilder sb = new StringBuilder();
        FloatList list = FloatList.of(new float[] { 1.5f, 2.7f, 3.14f });
        type.appendTo(sb, list);
        assertEquals("[1.5, 2.7, 3.14]", sb.toString());
    }

    @Test
    public void testWriteCharacterEmptyList() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        FloatList list = FloatList.of(new float[0]);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        type.writeCharacter(writer, list, config);
        verify(writer).write('[');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterNonEmptyList() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        FloatList list = FloatList.of(new float[] { 1.5f, 2.7f });
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        type.writeCharacter(writer, list, config);
        verify(writer).write('[');
        verify(writer).write(1.5f);
        verify(writer).write(", ");
        verify(writer).write(2.7f);
        verify(writer).write(']');
    }

}
