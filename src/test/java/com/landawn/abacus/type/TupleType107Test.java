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

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple8;

public class TupleType107Test extends TestBase {

    private Tuple8Type<String, Integer, Boolean, Double, Long, Float, Byte, Short> tuple8Type;
    private Tuple8<String, Integer, Boolean, Double, Long, Float, Byte, Short> testTuple8;

    @BeforeEach
    public void setUp() {
        tuple8Type = (Tuple8Type<String, Integer, Boolean, Double, Long, Float, Byte, Short>) createType(
                "Tuple8<String, Integer, Boolean, Double, Long, Float, Byte, Short>");
        testTuple8 = Tuple.of("test", 123, true, 3.14, 999L, 2.5f, (byte) 7, (short) 88);
    }

    @Test
    public void testDeclaringName() {
        String declaringName = tuple8Type.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Tuple8"));
    }

    @Test
    public void testClazz() {
        Class<?> clazz = tuple8Type.clazz();
        assertNotNull(clazz);
        assertEquals(Tuple8.class, clazz);
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = tuple8Type.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(8, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(tuple8Type.isGenericType());
    }

    @Test
    public void testStringOf() {
        String result = tuple8Type.stringOf(testTuple8);
        assertNotNull(result);
        assertTrue(result.contains("test"));
        assertTrue(result.contains("123"));
        assertTrue(result.contains("true"));
        assertTrue(result.contains("3.14"));
        assertTrue(result.contains("999"));
        assertTrue(result.contains("2.5"));
        assertTrue(result.contains("7"));
        assertTrue(result.contains("88"));
    }

    @Test
    public void testStringOfNull() {
        String result = tuple8Type.stringOf(null);
        assertNull(result);
    }

    @Test
    public void testValueOf() {
        String json = "[\"test\",123,true,3.14,999,2.5,7,88]";
        Tuple8<String, Integer, Boolean, Double, Long, Float, Byte, Short> result = tuple8Type.valueOf(json);
        assertNotNull(result);
        assertEquals("test", result._1);
        assertEquals(Integer.valueOf(123), result._2);
        assertEquals(Boolean.TRUE, result._3);
        assertEquals(Double.valueOf(3.14), result._4);
        assertEquals(Long.valueOf(999), result._5);
        assertEquals(Float.valueOf(2.5f), result._6);
        assertEquals(Byte.valueOf((byte) 7), result._7);
        assertEquals(Short.valueOf((short) 88), result._8);
    }

    @Test
    public void testValueOfEmptyString() {
        Tuple8<String, Integer, Boolean, Double, Long, Float, Byte, Short> result = tuple8Type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfNull() {
        Tuple8<String, Integer, Boolean, Double, Long, Float, Byte, Short> result = tuple8Type.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testAppendToWriter() throws IOException {
        Writer writer = new StringWriter();
        tuple8Type.appendTo(writer, testTuple8);
        String result = writer.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testAppendToWriterNull() throws IOException {
        Writer writer = new StringWriter();
        tuple8Type.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        tuple8Type.appendTo(sb, testTuple8);
        String result = sb.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = null;

        tuple8Type.writeCharacter(writer, testTuple8, config);

        verify(writer, atLeastOnce()).write(anyChar());
    }

    @Test
    public void testWriteCharacterNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = null;

        tuple8Type.writeCharacter(writer, null, config);

        verify(writer).write(any(char[].class));
    }
}
