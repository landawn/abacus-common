package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple1;

@Tag("new-test")
public class TupleType100Test extends TestBase {

    private Tuple1Type<String> tuple1Type;
    private Tuple1<String> testTuple1;

    @BeforeEach
    public void setUp() {
        tuple1Type = (Tuple1Type<String>) createType("Tuple1<String>");
        testTuple1 = Tuple.of("test");
    }

    @Test
    public void testDeclaringName() {
        String declaringName = tuple1Type.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Tuple1"));
    }

    @Test
    public void testClazz() {
        Class<?> clazz = tuple1Type.clazz();
        assertNotNull(clazz);
        assertEquals(Tuple1.class, clazz);
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = tuple1Type.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(tuple1Type.isGenericType());
    }

    @Test
    public void testStringOf() {
        String result = tuple1Type.stringOf(testTuple1);
        assertNotNull(result);
        assertTrue(result.contains("test"));
    }

    @Test
    public void testStringOfNull() {
        String result = tuple1Type.stringOf(null);
        assertNull(result);
    }

    @Test
    public void testValueOf() {
        String json = "[\"test\"]";
        Tuple1<String> result = tuple1Type.valueOf(json);
        assertNotNull(result);
        assertEquals("test", result._1);
    }

    @Test
    public void testValueOfEmptyString() {
        Tuple1<String> result = tuple1Type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfNull() {
        Tuple1<String> result = tuple1Type.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testAppendToWriter() throws IOException {
        Writer writer = new StringWriter();
        tuple1Type.appendTo(writer, testTuple1);
        String result = writer.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testAppendToWriterNull() throws IOException {
        Writer writer = new StringWriter();
        tuple1Type.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        tuple1Type.appendTo(sb, testTuple1);
        String result = sb.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = null;

        tuple1Type.writeCharacter(writer, testTuple1, config);

        verify(writer, atLeastOnce()).write(anyChar());
    }

    @Test
    public void testWriteCharacterNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = null;

        tuple1Type.writeCharacter(writer, null, config);

        verify(writer).write(any(char[].class));
    }
}
