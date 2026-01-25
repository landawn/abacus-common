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
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple3;

@Tag("new-test")
public class TupleType102Test extends TestBase {

    private Tuple3Type<String, Integer, Boolean> tuple3Type;
    private Tuple3<String, Integer, Boolean> testTuple3;

    @BeforeEach
    public void setUp() {
        tuple3Type = (Tuple3Type<String, Integer, Boolean>) createType("Tuple3<String, Integer, Boolean>");
        testTuple3 = Tuple.of("test", 123, true);
    }

    @Test
    public void testDeclaringName() {
        String declaringName = tuple3Type.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Tuple3"));
    }

    @Test
    public void testClazz() {
        Class<?> clazz = tuple3Type.clazz();
        assertNotNull(clazz);
        assertEquals(Tuple3.class, clazz);
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = tuple3Type.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(3, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(tuple3Type.isGenericType());
    }

    @Test
    public void testStringOf() {
        String result = tuple3Type.stringOf(testTuple3);
        assertNotNull(result);
        assertTrue(result.contains("test"));
        assertTrue(result.contains("123"));
        assertTrue(result.contains("true"));
    }

    @Test
    public void testStringOfNull() {
        String result = tuple3Type.stringOf(null);
        assertNull(result);
    }

    @Test
    public void testValueOf() {
        String json = "[\"test\",123,true]";
        Tuple3<String, Integer, Boolean> result = tuple3Type.valueOf(json);
        assertNotNull(result);
        assertEquals("test", result._1);
        assertEquals(Integer.valueOf(123), result._2);
        assertEquals(Boolean.TRUE, result._3);
    }

    @Test
    public void testValueOfEmptyString() {
        Tuple3<String, Integer, Boolean> result = tuple3Type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfNull() {
        Tuple3<String, Integer, Boolean> result = tuple3Type.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testAppendToWriter() throws IOException {
        Writer writer = new StringWriter();
        tuple3Type.appendTo(writer, testTuple3);
        String result = writer.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testAppendToWriterNull() throws IOException {
        Writer writer = new StringWriter();
        tuple3Type.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        tuple3Type.appendTo(sb, testTuple3);
        String result = sb.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerializationConfig<?> config = null;

        tuple3Type.writeCharacter(writer, testTuple3, config);

        verify(writer, atLeastOnce()).write(anyChar());
    }

    @Test
    public void testWriteCharacterNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerializationConfig<?> config = null;

        tuple3Type.writeCharacter(writer, null, config);

        verify(writer).write(any(char[].class));
    }
}
