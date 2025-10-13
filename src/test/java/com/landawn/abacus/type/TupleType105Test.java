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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple6;

@Tag("new-test")
public class TupleType105Test extends TestBase {

    private Tuple6Type<String, Integer, Boolean, Double, Long, Float> tuple6Type;
    private Tuple6<String, Integer, Boolean, Double, Long, Float> testTuple6;

    @BeforeEach
    public void setUp() {
        tuple6Type = (Tuple6Type<String, Integer, Boolean, Double, Long, Float>) createType("Tuple6<String, Integer, Boolean, Double, Long, Float>");
        testTuple6 = Tuple.of("test", 123, true, 3.14, 999L, 2.5f);
    }

    @Test
    public void testDeclaringName() {
        String declaringName = tuple6Type.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Tuple6"));
    }

    @Test
    public void testClazz() {
        Class<?> clazz = tuple6Type.clazz();
        assertNotNull(clazz);
        assertEquals(Tuple6.class, clazz);
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = tuple6Type.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(6, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(tuple6Type.isGenericType());
    }

    @Test
    public void testStringOf() {
        String result = tuple6Type.stringOf(testTuple6);
        assertNotNull(result);
        assertTrue(result.contains("test"));
        assertTrue(result.contains("123"));
        assertTrue(result.contains("true"));
        assertTrue(result.contains("3.14"));
        assertTrue(result.contains("999"));
        assertTrue(result.contains("2.5"));
    }

    @Test
    public void testStringOfNull() {
        String result = tuple6Type.stringOf(null);
        assertNull(result);
    }

    @Test
    public void testValueOf() {
        String json = "[\"test\",123,true,3.14,999,2.5]";
        Tuple6<String, Integer, Boolean, Double, Long, Float> result = tuple6Type.valueOf(json);
        assertNotNull(result);
        assertEquals("test", result._1);
        assertEquals(Integer.valueOf(123), result._2);
        assertEquals(Boolean.TRUE, result._3);
        assertEquals(Double.valueOf(3.14), result._4);
        assertEquals(Long.valueOf(999), result._5);
        assertEquals(Float.valueOf(2.5f), result._6);
    }

    @Test
    public void testValueOfEmptyString() {
        Tuple6<String, Integer, Boolean, Double, Long, Float> result = tuple6Type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfNull() {
        Tuple6<String, Integer, Boolean, Double, Long, Float> result = tuple6Type.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testAppendToWriter() throws IOException {
        Writer writer = new StringWriter();
        tuple6Type.appendTo(writer, testTuple6);
        String result = writer.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testAppendToWriterNull() throws IOException {
        Writer writer = new StringWriter();
        tuple6Type.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        tuple6Type.appendTo(sb, testTuple6);
        String result = sb.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = null;

        tuple6Type.writeCharacter(writer, testTuple6, config);

        verify(writer, atLeastOnce()).write(anyChar());
    }

    @Test
    public void testWriteCharacterNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = null;

        tuple6Type.writeCharacter(writer, null, config);

        verify(writer).write(any(char[].class));
    }
}
