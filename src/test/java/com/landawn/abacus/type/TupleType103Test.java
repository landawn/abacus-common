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
import com.landawn.abacus.util.Tuple.Tuple4;

@Tag("new-test")
public class TupleType103Test extends TestBase {

    private Tuple4Type<String, Integer, Boolean, Double> tuple4Type;
    private Tuple4<String, Integer, Boolean, Double> testTuple4;

    @BeforeEach
    public void setUp() {
        tuple4Type = (Tuple4Type<String, Integer, Boolean, Double>) createType("Tuple4<String, Integer, Boolean, Double>");
        testTuple4 = Tuple.of("test", 123, true, 3.14);
    }

    @Test
    public void testDeclaringName() {
        String declaringName = tuple4Type.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Tuple4"));
    }

    @Test
    public void testClazz() {
        Class<?> clazz = tuple4Type.clazz();
        assertNotNull(clazz);
        assertEquals(Tuple4.class, clazz);
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = tuple4Type.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(4, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(tuple4Type.isGenericType());
    }

    @Test
    public void testStringOf() {
        String result = tuple4Type.stringOf(testTuple4);
        assertNotNull(result);
        if (!result.contains("test")) {
            System.out.println("Result: " + result);
        }
        assertTrue(result.contains("test"));
        assertTrue(result.contains("123"));
        assertTrue(result.contains("true"));
        assertTrue(result.contains("3.14"));
    }

    @Test
    public void testStringOfNull() {
        String result = tuple4Type.stringOf(null);
        assertNull(result);
    }

    @Test
    public void testValueOf() {
        String json = "[\"test\",123,true,3.14]";
        Tuple4<String, Integer, Boolean, Double> result = tuple4Type.valueOf(json);
        assertNotNull(result);
        assertEquals("test", result._1);
        assertEquals(Integer.valueOf(123), result._2);
        assertEquals(Boolean.TRUE, result._3);
        assertEquals(Double.valueOf(3.14), result._4);
    }

    @Test
    public void testValueOfEmptyString() {
        Tuple4<String, Integer, Boolean, Double> result = tuple4Type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfNull() {
        Tuple4<String, Integer, Boolean, Double> result = tuple4Type.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testAppendToWriter() throws IOException {
        Writer writer = new StringWriter();
        tuple4Type.appendTo(writer, testTuple4);
        String result = writer.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testAppendToWriterNull() throws IOException {
        Writer writer = new StringWriter();
        tuple4Type.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        tuple4Type.appendTo(sb, testTuple4);
        String result = sb.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = null;

        tuple4Type.writeCharacter(writer, testTuple4, config);

        verify(writer, atLeastOnce()).write(anyChar());
    }

    @Test
    public void testWriteCharacterNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = null;

        tuple4Type.writeCharacter(writer, null, config);

        verify(writer).write(any(char[].class));
    }
}
