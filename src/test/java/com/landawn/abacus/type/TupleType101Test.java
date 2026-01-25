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
import com.landawn.abacus.util.Tuple.Tuple2;

@Tag("new-test")
public class TupleType101Test extends TestBase {

    private Tuple2Type<String, Integer> tuple2Type;
    private Tuple2<String, Integer> testTuple2;

    @BeforeEach
    public void setUp() {
        tuple2Type = (Tuple2Type<String, Integer>) createType("Tuple2<String, Integer>");
        testTuple2 = Tuple.of("test", 123);
    }

    @Test
    public void testDeclaringName() {
        String declaringName = tuple2Type.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Tuple2"));
    }

    @Test
    public void testClazz() {
        Class<?> clazz = tuple2Type.clazz();
        assertNotNull(clazz);
        assertEquals(Tuple2.class, clazz);
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = tuple2Type.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(2, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(tuple2Type.isGenericType());
    }

    @Test
    public void testStringOf() {
        String result = tuple2Type.stringOf(testTuple2);
        assertNotNull(result);
        assertTrue(result.contains("test"));
        assertTrue(result.contains("123"));
    }

    @Test
    public void testStringOfNull() {
        String result = tuple2Type.stringOf(null);
        assertNull(result);
    }

    @Test
    public void testValueOf() {
        String json = "[\"test\",123]";
        Tuple2<String, Integer> result = tuple2Type.valueOf(json);
        assertNotNull(result);
        assertEquals("test", result._1);
        assertEquals(Integer.valueOf(123), result._2);
    }

    @Test
    public void testValueOfEmptyString() {
        Tuple2<String, Integer> result = tuple2Type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfNull() {
        Tuple2<String, Integer> result = tuple2Type.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testAppendToWriter() throws IOException {
        Writer writer = new StringWriter();
        tuple2Type.appendTo(writer, testTuple2);
        String result = writer.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testAppendToWriterNull() throws IOException {
        Writer writer = new StringWriter();
        tuple2Type.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        tuple2Type.appendTo(sb, testTuple2);
        String result = sb.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerializationConfig<?> config = null;

        tuple2Type.writeCharacter(writer, testTuple2, config);

        verify(writer, atLeastOnce()).write(anyChar());
    }

    @Test
    public void testWriteCharacterNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerializationConfig<?> config = null;

        tuple2Type.writeCharacter(writer, null, config);

        verify(writer).write(any(char[].class));
    }
}
