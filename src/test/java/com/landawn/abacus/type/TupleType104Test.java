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
import com.landawn.abacus.util.Tuple.Tuple5;

@Tag("new-test")
public class TupleType104Test extends TestBase {

    private Tuple5Type<String, Integer, Boolean, Double, Long> tuple5Type;
    private Tuple5<String, Integer, Boolean, Double, Long> testTuple5;

    @BeforeEach
    public void setUp() {
        tuple5Type = (Tuple5Type<String, Integer, Boolean, Double, Long>) createType("Tuple5<String, Integer, Boolean, Double, Long>");
        testTuple5 = Tuple.of("test", 123, true, 3.14, 999L);
    }

    @Test
    public void testDeclaringName() {
        String declaringName = tuple5Type.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Tuple5"));
    }

    @Test
    public void testClazz() {
        Class<?> clazz = tuple5Type.clazz();
        assertNotNull(clazz);
        assertEquals(Tuple5.class, clazz);
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = tuple5Type.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(5, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(tuple5Type.isGenericType());
    }

    @Test
    public void testStringOf() {
        String result = tuple5Type.stringOf(testTuple5);
        assertNotNull(result);
        assertTrue(result.contains("test"));
        assertTrue(result.contains("123"));
        assertTrue(result.contains("true"));
        assertTrue(result.contains("3.14"));
        assertTrue(result.contains("999"));
    }

    @Test
    public void testStringOfNull() {
        String result = tuple5Type.stringOf(null);
        assertNull(result);
    }

    @Test
    public void testValueOf() {
        String json = "[\"test\",123,true,3.14,999]";
        Tuple5<String, Integer, Boolean, Double, Long> result = tuple5Type.valueOf(json);
        assertNotNull(result);
        assertEquals("test", result._1);
        assertEquals(Integer.valueOf(123), result._2);
        assertEquals(Boolean.TRUE, result._3);
        assertEquals(Double.valueOf(3.14), result._4);
        assertEquals(Long.valueOf(999), result._5);
    }

    @Test
    public void testValueOfEmptyString() {
        Tuple5<String, Integer, Boolean, Double, Long> result = tuple5Type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfNull() {
        Tuple5<String, Integer, Boolean, Double, Long> result = tuple5Type.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testAppendToWriter() throws IOException {
        Writer writer = new StringWriter();
        tuple5Type.appendTo(writer, testTuple5);
        String result = writer.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testAppendToWriterNull() throws IOException {
        Writer writer = new StringWriter();
        tuple5Type.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        tuple5Type.appendTo(sb, testTuple5);
        String result = sb.toString();
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = null;

        tuple5Type.writeCharacter(writer, testTuple5, config);

        verify(writer, atLeastOnce()).write(anyChar());
    }

    @Test
    public void testWriteCharacterNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = null;

        tuple5Type.writeCharacter(writer, null, config);

        verify(writer).write(any(char[].class));
    }
}
