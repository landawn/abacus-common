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
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Triple;

@Tag("new-test")
public class TripleType100Test extends TestBase {

    private TripleType<String, Integer, Boolean> tripleType;
    private Triple<String, Integer, Boolean> testTriple;

    @BeforeEach
    public void setUp() {
        tripleType = (TripleType<String, Integer, Boolean>) createType("Triple<String, Integer, Boolean>");
        testTriple = Triple.of("test", 123, true);
    }

    @Test
    public void testDeclaringName() {
        String declaringName = tripleType.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Triple"));
    }

    @Test
    public void testClazz() {
        Class<?> clazz = tripleType.clazz();
        assertNotNull(clazz);
        assertEquals(Triple.class, clazz);
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = tripleType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(3, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(tripleType.isGenericType());
    }

    @Test
    public void testStringOf() {
        String result = tripleType.stringOf(testTriple);
        assertNotNull(result);
        assertTrue(result.contains("test"));
        assertTrue(result.contains("123"));
        assertTrue(result.contains("true"));
    }

    @Test
    public void testStringOfNull() {
        String result = tripleType.stringOf(null);
        assertNull(result);
    }

    @Test
    public void testValueOf() {
        String json = "[\"test\",123,true]";
        Triple<String, Integer, Boolean> result = tripleType.valueOf(json);
        assertNotNull(result);
        assertEquals("test", result.left());
        assertEquals(Integer.valueOf(123), result.middle());
        assertEquals(Boolean.TRUE, result.right());
    }

    @Test
    public void testValueOfEmptyString() {
        Triple<String, Integer, Boolean> result = tripleType.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfNull() {
        Triple<String, Integer, Boolean> result = tripleType.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testAppendToWriter() throws IOException {
        Writer writer = new StringWriter();
        tripleType.appendTo(writer, testTriple);
        String result = writer.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testAppendToWriterNull() throws IOException {
        Writer writer = new StringWriter();
        tripleType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        tripleType.appendTo(sb, testTriple);
        String result = sb.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerializationConfig<?> config = null;

        tripleType.writeCharacter(writer, testTriple, config);

        verify(writer, atLeastOnce()).write(anyChar());
    }

    @Test
    public void testWriteCharacterNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerializationConfig<?> config = null;

        tripleType.writeCharacter(writer, null, config);

        verify(writer).write(any(char[].class));
    }
}
