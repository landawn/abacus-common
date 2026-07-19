package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Triple;

public class TripleTypeTest extends TestBase {

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
        Class<?> clazz = tripleType.javaType();
        assertNotNull(clazz);
        assertEquals(Triple.class, clazz);
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = tripleType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(3, paramTypes.size());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(tripleType.isParameterizedType());
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
    public void testValueOfRejectsWrongElementCount() {
        assertThrows(IllegalArgumentException.class, () -> tripleType.valueOf("[\"test\",123]"));
        assertThrows(IllegalArgumentException.class, () -> tripleType.valueOf("[\"test\",123,true,\"unexpected\"]"));
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
    public void testAppendToWriterPropagatesCheckedIOException() {
        final IOException failure = new IOException("write failure");
        final Writer writer = new Writer() {
            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                throw failure;
            }

            @Override
            public void flush() {
                // no-op
            }

            @Override
            public void close() {
                // no-op
            }
        };

        assertSame(failure, assertThrows(IOException.class, () -> tripleType.appendTo(writer, testTriple)));
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
    public void testSerializeTo() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerConfig<?> config = null;

        tripleType.serializeTo(writer, testTriple, config);

        verify(writer, atLeastOnce()).write(anyChar());
    }

    @Test
    public void testSerializeToNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerConfig<?> config = null;

        tripleType.serializeTo(writer, null, config);

        verify(writer).write(any(char[].class));
    }

    @Test
    public void testSerializeToPropagatesCheckedIOException() throws IOException {
        final IOException failure = new IOException("write failure");
        final CharacterWriter writer = createCharacterWriter();
        doThrow(failure).when(writer).write(anyChar());

        assertSame(failure, assertThrows(IOException.class, () -> tripleType.serializeTo(writer, testTriple, null)));
    }

    @Test
    public void testAppendTo_unquotedToStringForm() throws IOException {
        StringBuilder sb = new StringBuilder();
        tripleType.appendTo(sb, Triple.of("a", 1, true));
        // appendTo emits the plain, toString()-style form: the String element is NOT quoted
        assertEquals("[a, 1, true]", sb.toString());
    }

    @Test
    public void testSerializeTo_jsonQuotedForm() throws IOException {
        Triple<String, Integer, Boolean> triple = Triple.of("a", 1, true);
        com.landawn.abacus.util.BufferedJsonWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();
        tripleType.serializeTo(writer, triple, com.landawn.abacus.parser.JsonSerConfig.create());
        String json = writer.toString();
        com.landawn.abacus.util.Objectory.recycle(writer);

        // serializeTo emits JSON: the String element IS quoted; equals stringOf and differs from appendTo
        assertEquals("[\"a\", 1, true]", json);
        assertEquals(tripleType.stringOf(triple), json);

        StringBuilder sb = new StringBuilder();
        tripleType.appendTo(sb, triple);
        org.junit.jupiter.api.Assertions.assertNotEquals(sb.toString(), json);
    }
}
